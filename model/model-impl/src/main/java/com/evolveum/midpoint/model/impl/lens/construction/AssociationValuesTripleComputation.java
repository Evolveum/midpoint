/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.forProjectionContext;
import static com.evolveum.midpoint.schema.util.ObjectOperationPolicyTypeUtil.isMembershipSyncOutboundDisabled;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;

import com.evolveum.midpoint.prism.delta.PlusMinusZero;

import com.evolveum.midpoint.schema.config.AbstractAttributeMappingsDefinitionConfigItem;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;

import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentTargetImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.DeltaSetTripleIvwoMap;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.ItemDefinitionProvider;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * TEMPORARY/EXPERIMENTAL class that provides a full computation of an association values triple,
 * based on "modern" configuration style of association type.
 */
public class AssociationValuesTripleComputation {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationValuesTripleComputation.class);

    private final boolean complexAssociation;
    @NotNull private final ShadowAssociationDefinition associationDefinition;
    @NotNull private final AssociationOutboundMappingType outboundBean;
    @NotNull private final LensProjectionContext projectionContext;
    @NotNull private final MappingEvaluationEnvironment env;
    @NotNull private final OperationResult result;
    @NotNull private final PrismValueDeltaSetTriple<ShadowAssociationValue> triple;
    @NotNull private final ModelBeans b = ModelBeans.get();

    private AssociationValuesTripleComputation(
            @NotNull ShadowAssociationDefinition associationDefinition,
            @NotNull AssociationOutboundMappingType outboundBean,
            @NotNull LensProjectionContext projectionContext,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result) {
        this.complexAssociation = associationDefinition.isComplex();
        this.associationDefinition = associationDefinition;
        this.outboundBean = outboundBean;
        this.projectionContext = projectionContext;
        this.env = env;
        this.result = result;
        this.triple = PrismContext.get().deltaFactory().createPrismValueDeltaSetTriple();
    }

    /** Assumes the existence of the projection context and association definition with a bean. */
    public static PrismValueDeltaSetTriple<ShadowAssociationValue> compute(
            @NotNull ShadowAssociationDefinition associationDefinition,
            @NotNull AssociationOutboundMappingType outboundBean,
            @NotNull LensProjectionContext projectionContext,
            @NotNull XMLGregorianCalendar now,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        var avc = new AssociationValuesTripleComputation(
                associationDefinition,
                outboundBean,
                projectionContext,
                new MappingEvaluationEnvironment("association computation", now, task),
                result);
        return avc.compute();
    }

    private PrismValueDeltaSetTriple<ShadowAssociationValue> compute()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        if (outboundBean.getExpression() != null) {
            throw new UnsupportedOperationException("This should be treated elsewhere"); // FIXME document or implement
        } else {
            try {
                // We need to gather all relevant "magic assignments" here.
                var lensContext = projectionContext.getLensContext();
                lensContext.getEvaluatedAssignmentTriple().foreach(
                        (eaSet, ea) ->
                                ea.getRoles().foreach(
                                        (targetSet, target) -> {
                                            try {
                                                if (target.getAssignmentPath().last().isMatchingOrder()) {
                                                    var mode = PlusMinusZero.compute(eaSet, targetSet);
                                                    if (mode != null) {
                                                        // TODO consider validity as well
                                                        processAssignmentTarget(mode, target);
                                                    }
                                                }
                                            } catch (CommonException e) {
                                                throw new LocalTunnelException(e);
                                            }
                                        }
                                )
                );
            } catch (LocalTunnelException e) {
                e.unwrapAndRethrow();
                throw new NotHereAssertionError();
            }
        }
        return triple;
    }

    /** @see LocalTunnelException#unwrapAndRethrow() */
    private void processAssignmentTarget(@NotNull PlusMinusZero mode, EvaluatedAssignmentTargetImpl target)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        LOGGER.trace("Processing assignment target {}", target);
        var eligibility = getEligibility(target);
        if (!eligibility.eligible) {
            LOGGER.trace(" -> not eligible");
            return;
        }
        var tripleForTarget = new ValueComputation(target).compute(mode);
        LOGGER.trace(" -> resulting triple for this target: {}", tripleForTarget);
        if (tripleForTarget != null) {
            triple.merge(tripleForTarget);
        }
    }

    private AssignmentTargetEligibility getEligibility(EvaluatedAssignmentTargetImpl target)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var targetObject = target.getTarget().asObjectable();
        if (!(targetObject instanceof FocusType focus)) {
            return new AssignmentTargetEligibility(false, List.of());
        }
        var relevantShadows = findRelevantShadows(focus);
        return new AssignmentTargetEligibility(!relevantShadows.isEmpty(), relevantShadows);
    }

    private Collection<AbstractShadow> findRelevantShadows(FocusType focus)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        var objectParticipants = associationDefinition.getObjectParticipants();
        LOGGER.trace("Trying to find relevant shadows for focus {} having {} linkRefs (object types: {})",
                focus, focus.getLinkRef().size(), objectParticipants);

        stateCheck(!objectParticipants.isEmpty(), "No object participants in %s", associationDefinition);

        var relevantShadows = new ArrayList<AbstractShadow>();
        for (var linkRef : focus.getLinkRef()) {
            var shadow = AbstractShadow.of(
                    b.provisioningService.getObject(
                            ShadowType.class, linkRef.getOid(),
                            GetOperationOptionsBuilder.create().noFetch().build(),
                            env.task, result));
            if (shadow.isDead()) {
                LOGGER.trace("Ignoring dead shadow {}", shadow);
                continue;
            }
            if (isMembershipSyncOutboundDisabled(shadow.getEffectiveOperationPolicyRequired())) {
                // This check is supported for simple associations right now. But it does not hurt to evaluate it
                // for complex ones as well.
                LOGGER.trace("Ignoring shadow {} because of membership sync outbound policy", shadow);
                continue;
            }
            // TODO we should distinguish between types of objects in this associations
            if (objectParticipants.values().stream().anyMatch(participant -> participant.matches(shadow.getBean()))) {
                LOGGER.trace("{} is relevant", shadow);
                relevantShadows.add(shadow);
            } else {
                LOGGER.trace("{} is not relevant", shadow);
            }
        }
        return relevantShadows;
    }

    private class ValueComputation {

        @NotNull private final AssignmentHolderType assignmentTarget;

        @NotNull private final AssignmentPathVariables assignmentPathVariables;

        /** Values of individual items within the association. */
        @NotNull private final DeltaSetTripleIvwoMap tripleMap = new DeltaSetTripleIvwoMap();

        ValueComputation(@NotNull EvaluatedAssignmentTargetImpl target) throws SchemaException {
            this.assignmentTarget = target.getTarget().asObjectable();
            this.assignmentPathVariables = stateNonNull(
                    target.getAssignmentPath().computePathVariables(),
                    "No path variables for %s", target);
        }

        private @Nullable PrismValueDeltaSetTriple<ShadowAssociationValue> compute(@NotNull PlusMinusZero mode)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, ObjectNotFoundException {
            for (var attrDefBean : outboundBean.getAttribute()) {
                evaluateAttribute(attrDefBean, false);
            }
            for (var attrDefBean : outboundBean.getObjectRef()) {
                evaluateAttribute(attrDefBean, true);
            }
            var associationDataObject = consolidate();
            ShadowAssociationValue associationValue;
            if (complexAssociation) {
                associationValue = ShadowAssociationValue.fromAssociationDataObject(
                        AbstractShadow.of(associationDataObject),
                        associationDefinition);
                if (associationValue.isEmpty()) {
                    LOGGER.trace("Empty complex association value -> no output");
                    return null;
                }
            } else {
                var referenceAttributes = ShadowUtil.getAttributesContainer(associationDataObject).getReferenceAttributes();
                if (referenceAttributes.isEmpty()) {
                    LOGGER.trace("No reference attribute in the simple association value -> no output");
                    return null;
                }
                var referenceAttribute = MiscUtil.extractSingletonRequired(referenceAttributes);
                associationValue = ShadowAssociationValue.empty(associationDefinition);
                associationValue.getOrCreateObjectsContainer().addAttribute(referenceAttribute.clone());
            }
            var resultingTriple = PrismContext.get().deltaFactory().<ShadowAssociationValue>createPrismValueDeltaSetTriple();
            resultingTriple.addAllToSet(mode, List.of(associationValue));
            return resultingTriple;
        }

        private void evaluateAttribute(AttributeOutboundMappingsDefinitionType attrDefBean, boolean isObjectRef)
                throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
                ConfigurationException, ObjectNotFoundException {

            var origin = ConfigurationItemOrigin.inResourceOrAncestor(projectionContext.getResourceRequired());
            var mappingsCI = AbstractAttributeMappingsDefinitionConfigItem.of(attrDefBean, origin);
            var targetItemName = isObjectRef ? mappingsCI.getObjectRefOrDefault(associationDefinition) : mappingsCI.getRef();
            var targetItemPath = ShadowType.F_ATTRIBUTES.append(targetItemName);
            for (var outboundBean : attrDefBean.getMapping()) {
                var mappingConfigItem = MappingConfigItem.of(outboundBean, origin);

                MappingBuilder<PrismValue, ItemDefinition<?>> builder =
                        b.mappingFactory.createMappingBuilder(mappingConfigItem, "association value computation");

                var lensContext = projectionContext.getLensContext();
                if (!builder.isApplicableToChannel(lensContext.getChannel())) {
                    LOGGER.trace("Skipping outbound mapping for {} because the channel does not match", associationDefinition);
                    continue;
                }
                if (!builder.isApplicableToExecutionMode(env.task.getExecutionMode())) {
                    LOGGER.trace("Skipping outbound mapping for {} because the execution mode does not match", associationDefinition);
                    continue;
                }

                var focusContext = lensContext.getFocusContextRequired();
                ObjectDeltaObject<?> focusOdoAbsolute = focusContext.getObjectDeltaObjectAbsolute();

                var magicAssignmentIdi = assignmentPathVariables.getMagicAssignment();

                var outputDefinition =
                        associationDefinition.isComplex() ?
                                associationDefinition
                                        .getAssociationDataObjectDefinition()
                                        .findAttributeDefinitionRequired(targetItemName) :
                                projectionContext
                                        .getCompositeObjectDefinition()
                                        .findAttributeDefinitionRequired(targetItemName);

                builder = builder
                        .targetItemName(targetItemName)
                        .implicitTargetPath(targetItemPath)
                        .defaultTargetPath(targetItemPath)
                        .defaultTargetDefinition((ItemDefinition<?>) outputDefinition)
                        .mappingKind(MappingKindType.OUTBOUND)
                        .defaultSourceContextIdi(magicAssignmentIdi)
                        .addRootVariableDefinition(magicAssignmentIdi)
                        .addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT, magicAssignmentIdi)
                        .addVariableDefinition(ExpressionConstants.VAR_USER, focusOdoAbsolute)
                        .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdoAbsolute)
                        .addAliasRegistration(ExpressionConstants.VAR_ASSIGNMENT, null)
                        .addVariableDefinition(
                                ExpressionConstants.VAR_ASSOCIATION_DEFINITION,
                                associationDefinition, ShadowReferenceAttributeDefinition.class)
                        .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projectionContext.getResource(), ResourceType.class)
                        .addVariableDefinition(ExpressionConstants.VAR_THIS_OBJECT, assignmentTarget, ObjectType.class);

                builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables);
                builder = builder.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, lensContext.getSystemConfiguration(), SystemConfigurationType.class);
                builder.now(env.now);

                var mapping = builder.build();

                b.mappingEvaluator.evaluateMapping(mapping, forProjectionContext(projectionContext), env.task, result);

                tripleMap.putOrMerge(targetItemPath, ItemValueWithOrigin.createOutputTriple(mapping));
            }
        }

        private @NotNull ShadowType consolidate()
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            var shadowDef = associationDefinition.isComplex() ?
                    associationDefinition.getAssociationDataObjectDefinition() :
                    projectionContext.getCompositeObjectDefinitionRequired();
            var shadow = shadowDef.createBlankShadow().getBean();
            //noinspection unchecked
            var consolidation = new DeltaSetTripleMapConsolidation<>(
                    tripleMap,
                    (PrismContainerValue<ShadowType>) shadow.asPrismContainerValue(),
                    DeltaSetTripleMapConsolidation.APrioriDeltaProvider.none(),
                    (a) -> false,
                    true,
                    null,
                    ItemDefinitionProvider.forObjectDefinition(shadow.asPrismObject().getDefinition()),
                    env,
                    projectionContext.getLensContext(),
                    result);
            consolidation.computeItemDeltas();
            ItemDeltaCollectionsUtil.applyTo(
                    consolidation.getItemDeltas(),
                    shadow.asPrismContainerValue());
            return shadow;
        }
    }

    private record AssignmentTargetEligibility(
            boolean eligible,
            @NotNull Collection<AbstractShadow> relevantShadows) {
    }

    static class LocalTunnelException extends RuntimeException {
        CommonException exception;
        LocalTunnelException(CommonException cause) {
            super(cause);
            this.exception = cause;
        }

        /**
         * Update this method when exceptions in
         * {@link AssociationValuesTripleComputation#processAssignmentTarget(PlusMinusZero, EvaluatedAssignmentTargetImpl)} are updated.
         */
        void unwrapAndRethrow() throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            if (exception instanceof SchemaException schemaException) {
                throw schemaException;
            } else if (exception instanceof ExpressionEvaluationException expressionEvaluationException) {
                throw expressionEvaluationException;
            } else if (exception instanceof CommunicationException communicationException) {
                throw communicationException;
            } else if (exception instanceof SecurityViolationException securityViolationException) {
                throw securityViolationException;
            } else if (exception instanceof ConfigurationException configurationException) {
                throw configurationException;
            } else if (exception instanceof ObjectNotFoundException objectNotFoundException) {
                throw objectNotFoundException;
            } else {
                throw SystemException.unexpected(exception);
            }
        }
    }
}
