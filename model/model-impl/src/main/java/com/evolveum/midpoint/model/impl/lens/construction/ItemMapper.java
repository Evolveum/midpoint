/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.forModelContext;
import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.forProjectionContext;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAttributeDefinition;

import com.evolveum.midpoint.util.DebugUtil;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * Attribute or association "mapper" and its evaluation:
 *
 * 1. Traditional style: just a single outbound mapping.
 * 2. Association-type style: a set of mappings that define the association value.
 *
 * The latter case delegates the computation to {@link AssociationValuesTripleComputation}.
 */
abstract class ItemMapper<AH extends AssignmentHolderType, V extends PrismValue, D extends ItemDefinition<?>> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemMapper.class);

    /** "Parent" object: the construction evaluation. */
    @NotNull private final ConstructionEvaluation<?, ?> constructionEvaluation;

    /** "Grand-grand-parent": the construction. */
    @NotNull private final ResourceObjectConstruction<AH, ?> construction;

    /** Name of the attribute or association. */
    @NotNull private final ItemName itemName;

    /** Path to the attribute or association. */
    @NotNull private final ItemPath itemPath;

    /** The definition of attribute/association. */
    @NotNull final D itemDefinition;

    /** The mapper itself. */
    @NotNull private final Mapper<V, D> mapper;

    /** Traditional/legacy simple/reference attribute evaluation. */
    ItemMapper(
            @NotNull ConstructionEvaluation<AH, ?> constructionEvaluation,
            @NotNull ItemName itemName,
            @NotNull ItemPath itemPath,
            @NotNull D itemDefinition,
            @NotNull MappingConfigItem mappingConfigItem, // [EP:M:OM] DONE 2/2
            @NotNull OriginType originType,
            @NotNull MappingKindType mappingKind) {
        this.constructionEvaluation = constructionEvaluation;
        this.construction = constructionEvaluation.construction;
        this.itemName = itemName;
        this.itemPath = itemPath;
        this.itemDefinition = itemDefinition;
        this.mapper = new MappingBasedMapper(mappingConfigItem, originType, mappingKind);
    }

    /** Association evaluation based on association type definition. */
    ItemMapper(
            @NotNull ConstructionEvaluation<AH, ?> constructionEvaluation,
            @NotNull ItemName itemName,
            @NotNull ItemPath itemPath,
            @NotNull D itemDefinition) {
        this.constructionEvaluation = constructionEvaluation;
        this.construction = constructionEvaluation.construction;
        this.itemName = itemName;
        this.itemPath = itemPath;
        this.itemDefinition = itemDefinition;
        this.mapper = new AssociationTypeBasedMapper();
    }

    PrismValueDeltaSetTripleProducer<V, D> getTripleProducer() {
        return mapper.getTripleProducer();
    }

    public PrismValueDeltaSetTripleProducer<V, D> evaluate() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        mapper.checkNotYetEvaluated();

        LOGGER.trace("Starting evaluation of (item-level) {}", this);

        constructionEvaluation.loadFullShadowIfNeeded(this);

        try {
            return mapper.evaluate();
        } catch (SchemaException e) {
            throw new SchemaException(getEvaluationErrorMessage(e), e);
        } catch (@SuppressWarnings("CaughtExceptionImmediatelyRethrown") ExpressionEvaluationException e) {
            // No need to specially handle this here. It was already handled in the expression-processing
            // code and it has proper description.
            throw e;
        } catch (ObjectNotFoundException e) {
            throw e.wrap(getEvaluationErrorMessagePrefix());
        } catch (SecurityViolationException e) {
            throw new SecurityViolationException(getEvaluationErrorMessage(e), e);
        } catch (ConfigurationException e) {
            throw new ConfigurationException(getEvaluationErrorMessage(e), e);
        } catch (CommunicationException e) {
            throw new CommunicationException(getEvaluationErrorMessage(e), e);
        }
    }

    boolean isEnabled() {
        return mapper.isEnabled();
    }

    abstract ShadowAssociationDefinition getAssociationDefinition();

    private @NotNull Collection<V> getOriginalTargetValues() {
        LensProjectionContext projCtx = constructionEvaluation.projectionContext;
        ObjectDeltaObject<ShadowType> projectionOdo = constructionEvaluation.getProjectionOdo();

        if (projCtx == null || projCtx.isDelete() || projCtx.isAdd() || projectionOdo == null) {
            return List.of();
        }

        PrismObject<ShadowType> oldObject = projectionOdo.getOldObject();
        if (oldObject == null) {
            return List.of();
        }

        Item<V, D> item = oldObject.findItem(itemPath);
        if (item == null) {
            // Either the projection is fully loaded and the attribute/association does not exist,
            // or the projection is not loaded (contrary to the fact that loading was requested).
            // In both cases the wisest approach is to return empty list, keeping mapping from failing,
            // and not removing anything. In the future we may consider issuing a warning, if we don't have
            // full shadow, and range specification is present.
            return List.of();
        }

        return item.getValues();
    }

    private Object getIteration() {
        return constructionEvaluation.projectionContext != null ?
                LensUtil.getIterationVariableValue(constructionEvaluation.projectionContext) : null;
    }

    private Object getIterationToken() {
        return constructionEvaluation.projectionContext != null ?
                LensUtil.getIterationTokenVariableValue(constructionEvaluation.projectionContext) : null;
    }

    private Boolean getLegal() {
        return constructionEvaluation.projectionContext != null ? constructionEvaluation.projectionContext.isLegal() : null;
    }

    private Boolean getAssigned() {
        return constructionEvaluation.projectionContext != null ? constructionEvaluation.projectionContext.isAssigned() : null;
    }

    private String getEvaluationErrorMessage(Exception e) {
        return getEvaluationErrorMessagePrefix() + ": " + e.getMessage();
    }

    private String getEvaluationErrorMessagePrefix() {
        return "Error evaluating mapping for " + getTypedItemName() + " in " +
                constructionEvaluation.evaluatedConstruction.getHumanReadableConstructionDescription();
    }

    @NotNull
    private String getShortDesc() {
        return "outbound mapping for " + getTypedItemName() + " in " + construction.source;
    }

    @NotNull
    private String getTypedItemName() {
        return getItemType() + " " + PrettyPrinter.prettyPrint(itemName);
    }

    /**
     * @return "attribute" or "association"
     */
    protected abstract String getItemType();

    public boolean isVisible(Task task) {
        var lifecycleState = ((ShadowAttributeDefinition<?, ?, ?, ?>) itemDefinition).getLifecycleState();
        return SimulationUtil.isVisible(lifecycleState, task.getExecutionMode());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "itemName=" + itemName +
                ", itemPath=" + itemPath +
                '}';
    }

    boolean hasRangeSpecified() {
        return mapper.hasRangeSpecified();
    }

    MappingStrengthType getMapperStrength() {
        return mapper.getStrength();
    }

    /** Abstract mapper. */
    private interface Mapper<V extends PrismValue, D extends ItemDefinition<?>> extends Serializable {

        boolean hasRangeSpecified();

        MappingStrengthType getStrength();

        PrismValueDeltaSetTripleProducer<V, D> evaluate()
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
                ConfigurationException, SecurityViolationException ;

        void checkNotYetEvaluated();

        boolean isEnabled();

        PrismValueDeltaSetTripleProducer<V, D> getTripleProducer();
    }

    /**
     * Maps
     * Assumes the existence of the projection context and association definition with a bean. */
    private class AssociationTypeBasedMapper implements Mapper<V, D> {

        private PrismValueDeltaSetTriple<V> computedTriple;

        @Override
        public boolean hasRangeSpecified() {
            return false;
        }

        @Override
        public MappingStrengthType getStrength() {
            return MappingStrengthType.STRONG;
        }

        @Override
        public PrismValueDeltaSetTripleProducer<V, D> evaluate()
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            //noinspection unchecked
            computedTriple = (PrismValueDeltaSetTriple<V>)
                    AssociationValuesTripleComputation.compute(getAssociationDefinition(), constructionEvaluation);

            return getTripleProducer();
        }

        @Override
        public void checkNotYetEvaluated() {
            stateCheck(computedTriple == null, "Association value was already computed");
        }

        @Override
        public boolean isEnabled() {
            return true; // TODO implement
        }

        @Override
        public PrismValueDeltaSetTripleProducer<V, D> getTripleProducer() {
            //noinspection MethodDoesntCallSuperMethod
            return new PrismValueDeltaSetTripleProducer<>() {
                @Override
                public QName getTargetItemName() {
                    return itemName;
                }

                @Override
                public PrismValueDeltaSetTriple<V> getOutputTriple() {
                    return computedTriple;
                }

                @Override
                public @NotNull MappingStrengthType getStrength() {
                    return MappingStrengthType.STRONG;
                }

                @Override
                public PrismValueDeltaSetTripleProducer<V, D> clone() {
                    return this;
                }

                @Override
                public boolean isExclusive() {
                    return false;
                }

                @Override
                public boolean isAuthoritative() {
                    return false;
                }

                @Override
                public boolean isSourceless() {
                    return false;
                }

                @Override
                public String getIdentifier() {
                    return null;
                }

                @Override
                public boolean isPushChanges() {
                    return false;
                }

                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public String toString() {
                    return "association value construction";
                }

                @Override
                public String debugDump(int indent) {
                    return DebugUtil.debugDump(toString(), indent);
                }

                @Override
                public String toHumanReadableDescription() {
                    return toString();
                }
            };
        }
    }

    private class MappingBasedMapper implements Mapper<V, D> {

        /**
         * Mapping definition including the origin.
         *
         * [EP:M:OM] DONE
         */
        @NotNull private final MappingConfigItem mappingConfigItem;

        /** Legacy mapping "origin". (Will be probably removed soon.) */
        @NotNull private final OriginType originType;

        /** Mapping kind. For reporting purposes. */
        @NotNull private final MappingKindType mappingKind;

        /** The prepared or evaluated mapping. */
        private MappingImpl<V, D> mapping;

        MappingBasedMapper(
                @NotNull MappingConfigItem mappingConfigItem,
                @NotNull OriginType originType,
                @NotNull MappingKindType mappingKind) {
            this.mappingConfigItem = mappingConfigItem;
            this.originType = originType;
            this.mappingKind = mappingKind;
        }

        @Override
        public boolean hasRangeSpecified() {
            return mappingConfigItem.hasRangeSpecified();
        }

        @Override
        public MappingStrengthType getStrength() {
            return mappingConfigItem.value().getStrength();
        }

        // TODO: unify with MappingEvaluator.evaluateOutboundMapping(...)
        public PrismValueDeltaSetTripleProducer<V, D> evaluate()
                throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            MappingBuilder<V, D> mappingBuilder = // [EP:M:OM] DONE
                    construction.getMappingFactory().createMappingBuilder(mappingConfigItem, getShortDesc());

            ObjectDeltaObject<ShadowType> projectionOdo = constructionEvaluation.getProjectionOdo();

            mappingBuilder = construction.initializeMappingBuilder(
                    mappingBuilder, itemPath, itemName, itemDefinition,
                    getAssociationDefinition(), constructionEvaluation.task);

            if (mappingBuilder == null) {
                return null;
            }

            // TODO: check access

            mappingBuilder.originType(originType);
            mappingBuilder.mappingKind(mappingKind);

            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, projectionOdo);
            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, projectionOdo);
            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projectionOdo);
            mappingBuilder.addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION);
            mappingBuilder.addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION);
            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_OPERATION, constructionEvaluation.operation);

            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ITERATION, getIteration(), Integer.class);
            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN, getIterationToken(), String.class);
            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_LEGAL, getLegal());
            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ASSIGNED, getAssigned());

            mappingBuilder.originalTargetValues(getOriginalTargetValues());
            mappingBuilder.valuePolicySupplier(createValuePolicySupplier());

            // TODO: other variables?

            mapping = mappingBuilder.build();

            LensProjectionContext projCtx = constructionEvaluation.projectionContext;
            construction.getMappingEvaluator().evaluateMapping(
                    mapping,
                    projCtx != null ? forProjectionContext(projCtx) : forModelContext(construction.lensContext),
                    constructionEvaluation.task,
                    constructionEvaluation.result);

            return mapping;
        }

        private ConfigurableValuePolicySupplier createValuePolicySupplier() {
            return new ConfigurableValuePolicySupplier() {
                private ItemDefinition<?> outputDefinition;

                @Override
                public void setOutputDefinition(ItemDefinition<?> outputDefinition) {
                    this.outputDefinition = outputDefinition;
                }

                @Override
                public ValuePolicyType get(OperationResult result) {

                    ExpressionType expressionBean = mappingConfigItem.value().getExpression();
                    if (expressionBean == null) {
                        return null;
                    }
                    List<JAXBElement<?>> evaluators = expressionBean.getExpressionEvaluator();
                    for (JAXBElement<?> jaxbEvaluator : evaluators) {
                        Object object = jaxbEvaluator.getValue();
                        if (object instanceof GenerateExpressionEvaluatorType genEvaluatorBean
                                && genEvaluatorBean.getValuePolicyRef() != null) {
                            ObjectReferenceType ref = genEvaluatorBean.getValuePolicyRef();
                            try {
                                return ModelBeans.get().modelObjectResolver.resolve(
                                        ref, ValuePolicyType.class,
                                        null,
                                        "resolving value policy for generate attribute " + outputDefinition.getItemName() + "value",
                                        constructionEvaluation.task, result);
                            } catch (CommonException ex) {
                                throw new SystemException(ex.getMessage(), ex);
                            }
                        }
                    }
                    return null;
                }
            };
        }

        @Override
        public void checkNotYetEvaluated() {
            stateCheck(mapping == null, "Mapping was already evaluated");
        }

        @Override
        public boolean isEnabled() {
            return mappingConfigItem.isEnabled();
        }

        @Override
        public PrismValueDeltaSetTripleProducer<V, D> getTripleProducer() {
            return mapping;
        }
    }
}
