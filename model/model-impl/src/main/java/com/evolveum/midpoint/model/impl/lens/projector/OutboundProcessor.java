/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ValuePolicyResolver;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.lens.Construction;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Processor that evaluates values of the outbound mappings. It does not create the deltas yet. It just collects the
 * evaluated mappings in account context.
 *
 * @author Radovan Semancik
 */
@Component
public class OutboundProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundProcessor.class);

    private PrismContainerDefinition<ShadowAssociationType> associationContainerDefinition;

    @Autowired private PrismContext prismContext;
    @Autowired private MappingFactory mappingFactory;
    @Autowired private MappingEvaluator mappingEvaluator;
    @Autowired private ContextLoader contextLoader;
    @Autowired private Clock clock;

    <F extends FocusType> void processOutbound(LensContext<F> context, LensProjectionContext projCtx, Task task, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        ResourceShadowDiscriminator discr = projCtx.getResourceShadowDiscriminator();
        ObjectDelta<ShadowType> projectionDelta = projCtx.getDelta();

        if (projectionDelta != null && projectionDelta.getChangeType() == ChangeType.DELETE) {
            LOGGER.trace("Processing outbound expressions for {} skipped, DELETE account delta", discr);
            // No point in evaluating outbound
            return;
        }

        LOGGER.trace("Processing outbound expressions for {} starting", discr);

        RefinedObjectClassDefinition rOcDef = projCtx.getStructuralObjectClassDefinition();
        if (rOcDef == null) {
            LOGGER.error("Definition for {} not found in the context, but it should be there, dumping context:\n{}", discr, context.debugDump());
            throw new IllegalStateException("Definition for " + discr + " not found in the context, but it should be there");
        }

        ObjectDeltaObject<F> focusOdo = context.getFocusContext().getObjectDeltaObject();
        ObjectDeltaObject<ShadowType> projectionOdo = projCtx.getObjectDeltaObject();

        Construction<F> outboundConstruction = new Construction<>(null, projCtx.getResource());
        outboundConstruction.setRefinedObjectClassDefinition(rOcDef);

        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = rOcDef.getAuxiliaryObjectClassDefinitions();
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
            outboundConstruction.addAuxiliaryObjectClassDefinition(auxiliaryObjectClassDefinition);
        }

        String operation = projCtx.getOperation().getValue();

        NextRecompute nextRecompute = null;

        for (QName attributeName : rOcDef.getNamesOfAttributesWithOutboundExpressions()) {
            RefinedAttributeDefinition<?> refinedAttributeDefinition = rOcDef.findAttributeDefinition(attributeName);
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
            if (!projCtx.isDelete() && !projCtx.isFullShadow() && (strength == MappingStrengthType.STRONG || strength == MappingStrengthType.WEAK)) {
                contextLoader.loadFullShadow(context, projCtx, "strong/weak outbound mapping", task, result);
                projectionOdo = projCtx.getObjectDeltaObject();
            }

            String mappingShortDesc = "outbound mapping for " +
                    PrettyPrinter.prettyPrint(refinedAttributeDefinition.getItemName()) + " in " + projCtx.getResource();
            MappingImpl.Builder<PrismPropertyValue<?>, RefinedAttributeDefinition<?>> builder =
                    mappingFactory.createMappingBuilder(outboundMappingType, mappingShortDesc);
            //noinspection ConstantConditions
            builder = builder.originObject(projCtx.getResource())
                    .originType(OriginType.OUTBOUND);
            MappingImpl<PrismPropertyValue<?>,RefinedAttributeDefinition<?>> evaluatedMapping = evaluateMapping(builder, attributeName, refinedAttributeDefinition,
                    focusOdo, projectionOdo, operation, rOcDef, null, context, projCtx, task, result);

            if (evaluatedMapping != null) {
                outboundConstruction.addAttributeMapping(evaluatedMapping);
                nextRecompute = NextRecompute.update(evaluatedMapping, nextRecompute);
            }
        }

        for (QName assocName : rOcDef.getNamesOfAssociationsWithOutboundExpressions()) {
            RefinedAssociationDefinition associationDefinition = rOcDef.findAssociationDefinition(assocName);

            final MappingType outboundMappingType = associationDefinition.getOutboundMappingType();
            if (outboundMappingType == null) {
                continue;
            }

//            if (associationDefinition.isIgnored(LayerType.MODEL)) {
//                LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", assocName);
//                continue;
//            }

            MappingImpl.Builder<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> mappingBuilder = mappingFactory.createMappingBuilder(outboundMappingType,
                    "outbound mapping for " + PrettyPrinter.prettyPrint(associationDefinition.getName())
                    + " in " + projCtx.getResource());

            PrismContainerDefinition<ShadowAssociationType> outputDefinition = getAssociationContainerDefinition();
            MappingImpl<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> evaluatedMapping = evaluateMapping(mappingBuilder,
                    assocName, outputDefinition, focusOdo, projectionOdo, operation, rOcDef,
                    associationDefinition.getAssociationTarget(), context, projCtx, task, result);

            if (evaluatedMapping != null) {
                outboundConstruction.addAssociationMapping(evaluatedMapping);
                nextRecompute = NextRecompute.update(evaluatedMapping, nextRecompute);
            }
        }

        projCtx.setOutboundConstruction(outboundConstruction);
        if (nextRecompute != null) {
            nextRecompute.createTrigger(context.getFocusContext());
        }
    }

    // TODO: unify with MappingEvaluator.evaluateOutboundMapping(...)
    private <F extends FocusType, V extends PrismValue, D extends ItemDefinition> MappingImpl<V, D> evaluateMapping(final MappingImpl.Builder<V,D> mappingBuilder, QName attributeQName,
            D targetDefinition, ObjectDeltaObject<F> focusOdo, ObjectDeltaObject<ShadowType> projectionOdo,
            String operation, RefinedObjectClassDefinition rOcDef, RefinedObjectClassDefinition assocTargetObjectClassDefinition,
            LensContext<F> context, LensProjectionContext projCtx, final Task task, OperationResult result)
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
        mappingBuilder.now(clock.currentTimeXMLGregorianCalendar());

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

        ValuePolicyResolver stringPolicyResolver = new ValuePolicyResolver() {
            private ItemDefinition outputDefinition;

            @Override
            public void setOutputPath(ItemPath outputPath) {
            }

            @Override
            public void setOutputDefinition(ItemDefinition outputDefinition) {
                this.outputDefinition = outputDefinition;
            }

            @Override
            public ValuePolicyType resolve() {

                if (mappingBuilder.getMappingType().getExpression() != null) {
                    List<JAXBElement<?>> evaluators = mappingBuilder.getMappingType().getExpression().getExpressionEvaluator();
                    for (JAXBElement jaxbEvaluator : evaluators) {
                        Object object = jaxbEvaluator.getValue();
                        if (object instanceof GenerateExpressionEvaluatorType && ((GenerateExpressionEvaluatorType) object).getValuePolicyRef() != null) {
                            ObjectReferenceType ref = ((GenerateExpressionEvaluatorType) object).getValuePolicyRef();
                            try {
                                ValuePolicyType valuePolicyType = mappingBuilder.getObjectResolver().resolve(ref, ValuePolicyType.class,
                                        null, "resolving value policy for generate attribute "+ outputDefinition.getItemName()+"value", task, new OperationResult("Resolving value policy"));
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
        mappingBuilder.valuePolicyResolver(stringPolicyResolver);
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
        mappingEvaluator.evaluateMapping(mapping, context, projCtx, task, result);

        return mapping;
    }

    private PrismContainerDefinition<ShadowAssociationType> getAssociationContainerDefinition() {
        if (associationContainerDefinition == null) {
            PrismObjectDefinition<ShadowType> shadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
            associationContainerDefinition = shadowDefinition.findContainerDefinition(ShadowType.F_ASSOCIATION);
        }
        return associationContainerDefinition;
    }
}
