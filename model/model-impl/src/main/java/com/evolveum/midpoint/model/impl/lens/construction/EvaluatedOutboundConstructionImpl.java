/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;

import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;

import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Radovan Semancik
 */
public class EvaluatedOutboundConstructionImpl<AH extends AssignmentHolderType> extends EvaluatedConstructionImpl<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedOutboundConstructionImpl.class);

    /**
     * @pre construction is already evaluated and not ignored (has resource)
     */
    EvaluatedOutboundConstructionImpl(@NotNull final OutboundConstruction<AH> construction, @NotNull LensProjectionContext projectionContext) {
        super(construction, projectionContext.getResourceShadowDiscriminator());
        setProjectionContext(projectionContext);
    }

    @Override
    public NextRecompute evaluate(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        initializeProjectionContext();
        NextRecompute nextRecompute = evaluateAttributes(task, result);
        nextRecompute = evaluateAssociations(nextRecompute, task, result);

        return nextRecompute;
    }

    private NextRecompute evaluateAttributes(Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        ObjectDeltaObject<ShadowType> projectionOdo = getProjectionContext().getObjectDeltaObject();
        String operation = getProjectionContext().getOperation().getValue();
        NextRecompute nextRecompute = null;

        for (QName attributeName : getConstruction().getRefinedObjectClassDefinition().getNamesOfAttributesWithOutboundExpressions()) {
            RefinedAttributeDefinition<?> refinedAttributeDefinition = getConstruction().getRefinedObjectClassDefinition().findAttributeDefinition(attributeName);
            if (refinedAttributeDefinition == null) {
                throw new IllegalStateException("No definition for " + attributeName);
            }

            final MappingType outboundMappingType = refinedAttributeDefinition.getOutboundMappingType();
            if (outboundMappingType == null) {
                continue;
            }

            if (refinedAttributeDefinition.getProcessing(LayerType.MODEL) == ItemProcessing.IGNORE) {
                LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", attributeName);
                continue;
            }

            MappingStrengthType strength = outboundMappingType.getStrength();
            if (!getProjectionContext().isDelete() && !getProjectionContext().isFullShadow() && (strength == MappingStrengthType.STRONG || strength == MappingStrengthType.WEAK)) {
                getConstruction().loadFullShadow(getProjectionContext(), "strong/weak outbound mapping", task, result);
                projectionOdo = getProjectionContext().getObjectDeltaObject();
            }

            String mappingShortDesc = "outbound mapping for " +
                    PrettyPrinter.prettyPrint(refinedAttributeDefinition.getItemName()) + " in " + getProjectionContext().getResource();
            MappingBuilder<PrismPropertyValue<?>, RefinedAttributeDefinition<?>> builder =
                    getConstruction().getMappingFactory().createMappingBuilder(outboundMappingType, mappingShortDesc);
            //noinspection ConstantConditions
            builder = builder.originObject(getProjectionContext().getResource())
                    .originType(OriginType.OUTBOUND);
            MappingImpl<PrismPropertyValue<?>, RefinedAttributeDefinition<?>> evaluatedMapping = evaluateMapping(builder, attributeName, refinedAttributeDefinition,
                    getConstruction().getFocusOdo(), projectionOdo, operation, getConstruction().getRefinedObjectClassDefinition(), null, getConstruction().getLensContext(), getProjectionContext(), task, result);

            if (evaluatedMapping != null) {
                addAttributeMapping(evaluatedMapping);
                nextRecompute = NextRecompute.update(evaluatedMapping, nextRecompute);
            }
        }

        return nextRecompute;
    }

    private NextRecompute evaluateAssociations(NextRecompute nextRecompute, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        ObjectDeltaObject<ShadowType> projectionOdo = getProjectionContext().getObjectDeltaObject();
        String operation = getProjectionContext().getOperation().getValue();

        for (QName assocName : getConstruction().getRefinedObjectClassDefinition().getNamesOfAssociationsWithOutboundExpressions()) {
            RefinedAssociationDefinition associationDefinition = getConstruction().getRefinedObjectClassDefinition().findAssociationDefinition(assocName);

            final MappingType outboundMappingType = associationDefinition.getOutboundMappingType();
            if (outboundMappingType == null) {
                continue;
            }

//            if (associationDefinition.isIgnored(LayerType.MODEL)) {
//                LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", assocName);
//                continue;
//            }

            MappingBuilder<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> mappingBuilder = getConstruction().getMappingFactory().createMappingBuilder(outboundMappingType,
                    "outbound mapping for " + PrettyPrinter.prettyPrint(associationDefinition.getName())
                            + " in " + getProjectionContext().getResource());

            PrismContainerDefinition<ShadowAssociationType> outputDefinition = getConstruction().getAssociationContainerDefinition();
            MappingImpl<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> evaluatedMapping = evaluateMapping(mappingBuilder,
                    assocName, outputDefinition, getConstruction().getFocusOdo(), projectionOdo, operation, getConstruction().getRefinedObjectClassDefinition(),
                    associationDefinition.getAssociationTarget(), getConstruction().getLensContext(), getProjectionContext(), task, result);

            if (evaluatedMapping != null) {
                addAssociationMapping(evaluatedMapping);
                nextRecompute = NextRecompute.update(evaluatedMapping, nextRecompute);
            }
        }

        return nextRecompute;
    }

    // TODO: unify with MappingEvaluator.evaluateOutboundMapping(...)
    private <V extends PrismValue, D extends ItemDefinition> MappingImpl<V, D> evaluateMapping(final MappingBuilder<V,D> mappingBuilder, QName attributeQName,
            D targetDefinition, ObjectDeltaObject<AH> focusOdo, ObjectDeltaObject<ShadowType> projectionOdo,
            String operation, RefinedObjectClassDefinition rOcDef, RefinedObjectClassDefinition assocTargetObjectClassDefinition,
            LensContext<AH> context, LensProjectionContext projCtx, final Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (!mappingBuilder.isApplicableToChannel(context.getChannel())) {
            LOGGER.trace("Skipping outbound mapping for {} because the channel does not match", attributeQName);
            return null;
        }

        // TODO: check access

        // This is just supposed to be an optimization. The consolidation should deal with the weak mapping
        // even if it is there. But in that case we do not need to evaluate it at all.

        // Edit 2017-02-16 pmed: It's not quite true. If the attribute is non-tolerant, it will get removed if we would
        // skip evaluation of this mapping. So we really need to do this.
//        if (mappingBuilder.getStrength() == MappingStrengthType.WEAK && projCtx.hasValueForAttribute(mappingQName)) {
//            LOGGER.trace("Skipping outbound mapping for {} because it is weak", mappingQName);
//            return null;
//        }

        ItemPath targetPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attributeQName);
        mappingBuilder.defaultTargetPath(targetPath);
        mappingBuilder.defaultTargetDefinition(targetDefinition);
        mappingBuilder.sourceContext(focusOdo);
        mappingBuilder.mappingQName(attributeQName);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
        mappingBuilder.addAliasRegistration(ExpressionConstants.VAR_USER, null);
        mappingBuilder.addAliasRegistration(ExpressionConstants.VAR_FOCUS, null);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, projectionOdo);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_SHADOW, projectionOdo);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_PROJECTION, projectionOdo);
        mappingBuilder.addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION);
        mappingBuilder.addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, context.getSystemConfiguration(), SystemConfigurationType.class);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ITERATION,
                LensUtil.getIterationVariableValue(projCtx), Integer.class);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ITERATION_TOKEN,
                LensUtil.getIterationTokenVariableValue(projCtx), String.class);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projCtx.getResource(), ResourceType.class);
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_OPERATION, operation);

        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_LEGAL, projCtx.isLegal());
        mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ASSIGNED, projCtx.isAssigned());
        mappingBuilder.now(getConstruction().getNow());

        if (assocTargetObjectClassDefinition != null) {
            mappingBuilder.addVariableDefinition(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION,
                    assocTargetObjectClassDefinition, RefinedObjectClassDefinition.class);
        }
        mappingBuilder.rootNode(focusOdo);
        mappingBuilder.originType(OriginType.OUTBOUND);
        mappingBuilder.mappingKind(MappingKindType.OUTBOUND);
        mappingBuilder.implicitTargetPath(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeQName));
        mappingBuilder.refinedObjectClassDefinition(rOcDef);

        if (projCtx.isDelete()) {
            mappingBuilder.originalTargetValues(Collections.emptyList());
        } else if (projCtx.isAdd()) {
            mappingBuilder.originalTargetValues(Collections.emptyList());
        } else {
            PrismObject<ShadowType> oldObject = projectionOdo.getOldObject();
            if (oldObject != null) {
                PrismProperty<Object> attributeOld = oldObject.findProperty(targetPath);
                if (attributeOld == null) {
                    if (projCtx.hasFullShadow()) {
                        // We know that the attribute has no values
                        mappingBuilder.originalTargetValues(Collections.emptyList());
                    } else {
                        // We do not have full shadow. Therefore we know nothing about attribute values.
                        // We cannot set originalTargetValues here.
                    }
                } else {
                    //noinspection unchecked
                    mappingBuilder.originalTargetValues((Collection<V>) attributeOld.getValues());
                }
            }
        }

        ConfigurableValuePolicySupplier valuePolicySupplier = new ConfigurableValuePolicySupplier() {
            private ItemDefinition outputDefinition;

            @Override
            public void setOutputDefinition(ItemDefinition outputDefinition) {
                this.outputDefinition = outputDefinition;
            }

            @Override
            public ValuePolicyType get(OperationResult result) {

                if (mappingBuilder.getMappingBean().getExpression() != null) {
                    List<JAXBElement<?>> evaluators = mappingBuilder.getMappingBean().getExpression().getExpressionEvaluator();
                    for (JAXBElement jaxbEvaluator : evaluators) {
                        Object object = jaxbEvaluator.getValue();
                        if (object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null) {
                            ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
                            try {
                                ValuePolicyType valuePolicyType = mappingBuilder.getBeans().objectResolver.resolve(ref, ValuePolicyType.class,
                                        null, "resolving value policy for generate attribute "+ outputDefinition.getItemName()+"value", task, result);
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
        mappingBuilder.valuePolicySupplier(valuePolicySupplier);
        // TODO: other variables?

        // Set condition masks. There are used as a brakes to avoid evaluating to nonsense values in case user is not present
        // (e.g. in old values in ADD situations and new values in DELETE situations).
        if (focusOdo.getOldObject() == null) {
            mappingBuilder.conditionMaskOld(false);
        }
        if (focusOdo.getNewObject() == null) {
            mappingBuilder.conditionMaskNew(false);
        }

        MappingImpl<V,D> mapping = mappingBuilder.build();
        getConstruction().getMappingEvaluator().evaluateMapping(mapping, context, projCtx, task, result);

        return mapping;
    }


    private String getHumanReadableConstructionDescription() {
        return "outbound construction for (" + getResource() + "/" + getKind() + "/" + getIntent() + "/" + getTag() + ")";
    }


    @Override
    public String toString() {
        return "EvaluatedOutboundConstructionImpl(" +
                "discriminator=" + getResourceShadowDiscriminator() +
                ", construction=" + getConstruction() +
                ", projectionContext='" + getProjectionContext() +
                ')';
    }
}
