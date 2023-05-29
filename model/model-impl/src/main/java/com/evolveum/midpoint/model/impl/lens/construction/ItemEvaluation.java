/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.schema.util.SimulationUtil;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluation of an attribute or association.
 * (More specifically, evaluation of an outbound mapping for attribute/association.)
 *
 * @param <RD> Refined definition for attribute/association.
 */
abstract class ItemEvaluation<AH extends AssignmentHolderType, V extends PrismValue, D extends ItemDefinition<?>, RD> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemEvaluation.class);

    /**
     * "Parent" object: the construction evaluation.
     */
    @NotNull private final ConstructionEvaluation<?, ?> constructionEvaluation;

    /**
     * "Grand-grand-parent": the construction.
     */
    @NotNull private final ResourceObjectConstruction<AH, ?> construction;

    /**
     * Name of the attribute or association.
     */
    @NotNull final ItemName itemName;

    /**
     * Path to the attribute or association.
     * We know that for the association the path is imprecise: association/name.
     * But the mapping will need to live with it.
     */
    @NotNull final ItemPath itemPath;

    /**
     * Refined definition of attribute/association.
     */
    @NotNull final RD itemRefinedDefinition;

    /**
     * Prism item definition of attribute (the same as refined one) or association (different).
     */
    @NotNull private final D itemPrismDefinition;

    /**
     * Mapping definition.
     */
    @NotNull private final MappingType mappingBean;

    /**
     * Mapping origin.
     */
    @NotNull private final OriginType originType;

    /**
     * Mapping kind. For reporting purposes.
     */
    @NotNull private final MappingKindType mappingKind;

    /**
     * Evaluated mapping. The evaluation is carried out by this class.
     */
    private MappingImpl<V, D> evaluatedMapping;

    ItemEvaluation(ConstructionEvaluation<AH, ?> constructionEvaluation, @NotNull ItemName itemName,
            @NotNull ItemPath itemPath,
            @NotNull RD itemRefinedDefinition,
            @NotNull D itemPrismDefinition, @NotNull MappingType mappingBean, @NotNull OriginType originType,
            @NotNull MappingKindType mappingKind) {
        this.constructionEvaluation = constructionEvaluation;
        this.construction = constructionEvaluation.construction;
        this.itemName = itemName;
        this.itemPath = itemPath;
        this.itemRefinedDefinition = itemRefinedDefinition;
        this.itemPrismDefinition = itemPrismDefinition;
        this.mappingBean = mappingBean;
        this.originType = originType;
        this.mappingKind = mappingKind;
    }

    MappingImpl<V, D> getEvaluatedMapping() {
        return evaluatedMapping;
    }

    public void evaluate() throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        checkNotYetEvaluated();

        LOGGER.trace("Starting evaluation of (item-level) {}", this);

        constructionEvaluation.loadFullShadowIfNeeded(this);

        try {
            evaluatedMapping = evaluateMapping();
        } catch (SchemaException e) {
            throw new SchemaException(getEvaluationErrorMessage(e), e);
        } catch (@SuppressWarnings("CaughtExceptionImmediatelyRethrown") ExpressionEvaluationException e) {
            // No need to specially handle this here. It was already handled in the expression-processing
            // code and it has proper description.
            throw e;
        } catch (ObjectNotFoundException e) {
            throw e.wrap(getEvaluationErrorMessagePrefix(e));
        } catch (SecurityViolationException e) {
            throw new SecurityViolationException(getEvaluationErrorMessage(e), e);
        } catch (ConfigurationException e) {
            throw new ConfigurationException(getEvaluationErrorMessage(e), e);
        } catch (CommunicationException e) {
            throw new CommunicationException(getEvaluationErrorMessage(e), e);
        }
    }

    private void checkNotYetEvaluated() {
        if (evaluatedMapping != null) {
            throw new IllegalStateException();
        }
    }

    public @NotNull MappingType getMappingBean() {
        return mappingBean;
    }

    boolean hasEvaluatedMapping() {
        return evaluatedMapping != null && evaluatedMapping.isEnabled();
    }

    // TODO: unify with MappingEvaluator.evaluateOutboundMapping(...)
    private MappingImpl<V, D> evaluateMapping()
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        MappingBuilder<V, D> mappingBuilder = construction.getMappingFactory().createMappingBuilder(mappingBean, getShortDesc());

        LensProjectionContext projCtx = constructionEvaluation.projectionContext;
        ObjectDeltaObject<ShadowType> projectionOdo = constructionEvaluation.getProjectionOdo();

        LensContext<AH> context = construction.lensContext;

        mappingBuilder = construction.initializeMappingBuilder(
                mappingBuilder, itemPath, itemName, itemPrismDefinition,
                getAssociationTargetObjectClassDefinition(), constructionEvaluation.task);

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

        MappingImpl<V, D> mapping = mappingBuilder.build();
        construction.getMappingEvaluator().evaluateMapping(mapping, context, projCtx,
                constructionEvaluation.task, constructionEvaluation.result);

        return mapping;
    }

    abstract ResourceObjectTypeDefinition getAssociationTargetObjectClassDefinition();

    private ConfigurableValuePolicySupplier createValuePolicySupplier() {
        return new ConfigurableValuePolicySupplier() {
            private ItemDefinition<?> outputDefinition;

            @Override
            public void setOutputDefinition(ItemDefinition<?> outputDefinition) {
                this.outputDefinition = outputDefinition;
            }

            @Override
            public ValuePolicyType get(OperationResult result) {

                if (mappingBean.getExpression() != null) {
                    List<JAXBElement<?>> evaluators = mappingBean.getExpression().getExpressionEvaluator();
                    for (JAXBElement<?> jaxbEvaluator : evaluators) {
                        Object object = jaxbEvaluator.getValue();
                        if (object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null) {
                            ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
                            try {
                                ValuePolicyType valuePolicyType = ModelBeans.get().modelObjectResolver.resolve(ref, ValuePolicyType.class,
                                        null, "resolving value policy for generate attribute "+ outputDefinition.getItemName()+"value",
                                        constructionEvaluation.task, result);
                                if (valuePolicyType != null) {
                                    return valuePolicyType;
                                }
                            } catch (CommonException ex) {
                                throw new SystemException(ex.getMessage(), ex);
                            }
                        }
                    }
                }
                return null;
            }
        };
    }

    private Collection<V> getOriginalTargetValues() {
        LensProjectionContext projCtx = constructionEvaluation.projectionContext;
        ObjectDeltaObject<ShadowType> projectionOdo = constructionEvaluation.getProjectionOdo();

        if (projCtx == null || projCtx.isDelete() || projCtx.isAdd() || projectionOdo == null) {
            return Collections.emptyList();
        } else {
            PrismObject<ShadowType> oldObject = projectionOdo.getOldObject();
            if (oldObject != null) {
                return getOriginalTargetValuesFromShadow(oldObject);
            } else {
                return Collections.emptyList();
            }
        }
    }

    protected abstract Collection<V> getOriginalTargetValuesFromShadow(@NotNull PrismObject<ShadowType> shadow);

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
        return getEvaluationErrorMessagePrefix(e) + ": " + e.getMessage();
    }

    private String getEvaluationErrorMessagePrefix(Exception e) {
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
        return SimulationUtil.isVisible(getLifecycleState(), task.getExecutionMode());
    }

    abstract String getLifecycleState();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "itemName=" + itemName +
                ", itemPath=" + itemPath +
                '}';
    }
}
