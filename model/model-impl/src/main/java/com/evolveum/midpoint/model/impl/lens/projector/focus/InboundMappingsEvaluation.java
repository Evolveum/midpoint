/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluatorParams;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingInitializer;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingOutputProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingTimeEval;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

/**
 * Evaluation of inbound mappings.
 *
 * Responsibility of this class:
 * 1) collects inbound mappings to be evaluated
 * 2) evaluates them
 * 3) consolidates the results into deltas
 */
@Experimental
class InboundMappingsEvaluation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(InboundMappingsEvaluation.class);

    private final LensContext<F> context;
    private final ModelBeans beans;
    private final MappingEvaluationEnvironment env;
    private final OperationResult result;

    /**
     * Key: target item path, value: InboundMappingStruct(mapping, projectionCtx)
     */
    private final PathKeyedMap<List<InboundMappingStruct<?, ?>>> mappingsToTarget = new PathKeyedMap<>();

    /**
     * Lazily evaluated.
     */
    private PrismContainerDefinition<ResourceObjectAssociationType> associationContainerDefinition;

    InboundMappingsEvaluation(LensContext<F> context, ModelBeans beans, MappingEvaluationEnvironment env, OperationResult result) {
        this.context = context;
        this.beans = beans;
        this.env = env;
        this.result = result;

    }

    void collectAndEvaluateMappings() throws SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        collectMappings();
        evaluateMappings();
    }

    /**
     * Used to collect all the mappings from all the projects, sorted by target property.
     *
     * Motivation: we need to evaluate them together, e.g. in case that there are several mappings
     * from several projections targeting the same property.
     */
    private void collectMappings()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {

            // Preliminary checks. (Before computing apriori delta and other things.)

            if (projectionContext.isTombstone()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {} because is is tombstone", lazy(projectionContext::getHumanReadableName));
                continue;
            }
            if (!projectionContext.isCanProject()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {}: there is a limit to propagate changes only from resource {}",
                        lazy(projectionContext::getHumanReadableName), context.getTriggeredResourceOid());
                continue;
            }

            new ProjectionMappingsCollector(projectionContext).collect();
        }
    }

    /**
     * Collects inbound mappings from specified projection context.
     */
    private class ProjectionMappingsCollector {

        private final LensProjectionContext projectionContext;

        private PrismObject<ShadowType> projectionCurrent;

        private final ObjectDelta<ShadowType> aPrioriDelta;

        private final RefinedObjectClassDefinition projectionDefinition;

        private boolean stop;

        private ProjectionMappingsCollector(LensProjectionContext projectionContext) throws SchemaException {
            this.projectionContext = projectionContext;
            this.projectionCurrent = projectionContext.getObjectCurrent();
            this.aPrioriDelta = getAPrioriDelta(projectionContext);
            this.projectionDefinition = projectionContext.getCompositeObjectClassDefinition();
        }

        public void collect() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

            if (!projectionContext.isDoReconciliation() && aPrioriDelta == null &&
                    !LensUtil.hasDependentContext(context, projectionContext) && !projectionContext.isFullShadow() &&
                    !projectionContext.isDelete()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {}: no full shadow, no reconciliation, "
                                + "no a priori delta and no dependent context and it's not delete operation:\n{}",
                        lazy(projectionContext::getHumanReadableName), projectionContext.debugDumpLazily());
                return;
            }

            if (aPrioriDelta == null && projectionContext.getObjectCurrent() == null) {
                LOGGER.trace("Nothing to process in inbound, both a priori delta and current account were null.");
                return;
            }

            checkObjectClassDefinitionPresent();

            collectProjectionMappings();
        }

        private void checkObjectClassDefinitionPresent() {
            if (projectionDefinition == null) {
                LOGGER.error("Definition for projection {} not found in the context, but it " +
                        "should be there, dumping context:\n{}", projectionContext.getHumanReadableName(), context.debugDump());
                throw new IllegalStateException("Definition for projection " + projectionContext.getHumanReadableName()
                        + " not found in the context, but it should be there");
            }
        }

        private void collectProjectionMappings()
                throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException, CommunicationException, SecurityViolationException {

            loadIfAnyStrong();
            if (shouldStop()) {
                return;
            }

            for (QName attributeName : projectionDefinition.getNamesOfAttributesWithInboundExpressions()) {
                collectAttributeInbounds(attributeName);
                if (shouldStop()) {
                    return;
                }
            }

            for (QName associationName : projectionDefinition.getNamesOfAssociationsWithInboundExpressions()) {
                collectAssociationInbounds(associationName);
                if (shouldStop()) {
                    return;
                }
            }

            if (isDeleteProjectionDelta()) {
                LOGGER.trace("Skipping application of special properties because of projection DELETE delta");
                return;
            }

            evaluateSpecialPropertyInbound(projectionDefinition.getPasswordInbound(), SchemaConstants.PATH_PASSWORD_VALUE, SchemaConstants.PATH_PASSWORD_VALUE,
                    context.getFocusContext().getObjectNew(), projectionContext);

            evaluateSpecialPropertyInbound(projectionDefinition.getActivationBidirectionalMappingType(ActivationType.F_ADMINISTRATIVE_STATUS), SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    context.getFocusContext().getObjectNew(), projectionContext);
            evaluateSpecialPropertyInbound(projectionDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_FROM), SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                    context.getFocusContext().getObjectNew(), projectionContext);
            evaluateSpecialPropertyInbound(projectionDefinition.getActivationBidirectionalMappingType(ActivationType.F_VALID_TO), SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    context.getFocusContext().getObjectNew(), projectionContext);

            collectAuxiliaryObjectClassInbounds();
        }


        private boolean isDeleteProjectionDelta() throws SchemaException {
            return ObjectDelta.isDelete(projectionContext.getSyncDelta()) || ObjectDelta.isDelete(projectionContext.getDelta());
        }

        private <V extends PrismValue, D extends ItemDefinition> void collectAttributeInbounds(QName attributeName)
                throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException,
                SecurityViolationException, CommunicationException {

            final ItemDelta<V, D> attributeAPrioriDelta;
            if (aPrioriDelta != null) {
                attributeAPrioriDelta = aPrioriDelta.findItemDelta(ItemPath.create(SchemaConstants.C_ATTRIBUTES, attributeName));
                if (attributeAPrioriDelta == null && !projectionContext.isFullShadow() && !LensUtil.hasDependentContext(context, projectionContext)) {
                    LOGGER.trace("Skipping inbound for {} in {}: Not a full shadow and account a priori delta exists, but doesn't have change for the attribute.",
                            attributeName, projectionContext.getResourceShadowDiscriminator());
                    return;
                }
            } else {
                attributeAPrioriDelta = null;
            }

            RefinedAttributeDefinition<?> attributeDef = java.util.Objects.requireNonNull(
                    projectionDefinition.findAttributeDefinition(attributeName),
                    () -> "No definition for attribute " + attributeName);

            if (attributeDef.isIgnored(LayerType.MODEL)) {
                LOGGER.trace("Skipping inbound for attribute {} in {} because the attribute is ignored",
                        attributeName, projectionContext.getResourceShadowDiscriminator());
                return;
            }

            List<MappingType> inboundMappingBeans = attributeDef.getInboundMappingTypes();
            LOGGER.trace("Processing inbounds for {} in {}; ({} mappings)", lazy(() -> PrettyPrinter.prettyPrint(attributeName)),
                    projectionContext.getResourceShadowDiscriminator(), inboundMappingBeans.size());

            if (isNotReadable(attributeDef.getLimitations(LayerType.MODEL))) {
                LOGGER.warn("Inbound mapping for non-readable attribute {} in {}, skipping",
                        attributeName, projectionContext.getHumanReadableName());
                return;
            }

            for (MappingType inboundMappingBean : inboundMappingBeans) {

                // There are two processing options:
                //
                //  * If we have a delta as an input we will proceed in relative mode, applying mappings on the delta.
                //    This usually happens when a delta comes from a sync notification or if there is a primary projection delta.
                //
                //  * if we do NOT have a delta then we will proceed in absolute mode. In that mode we will apply the
                //    mappings to the absolute projection state that we got from provisioning. This is a kind of "inbound reconciliation".
                //
                // TODO what if there is a priori delta for a given attribute (e.g. ADD one) and
                //  we want to reconcile also the existing attribute value? This probably would not work.

                loadIfStrong(inboundMappingBean);
                if (shouldStop()) {
                    return;
                }

                if (attributeAPrioriDelta == null && !projectionContext.isFullShadow() && !LensUtil.hasDependentContext(context, projectionContext)) {
                    LOGGER.trace("Skipping inbound for {} in {}: Not a full shadow and no a priori delta for processed property.",
                            attributeName, projectionContext.getResourceShadowDiscriminator());
                    return;
                }

                if (attributeAPrioriDelta != null) {
                    LOGGER.trace("Processing inbound from a priori delta");
                } else if (projectionCurrent != null) {
                    loadRequired();
                    if (shouldStop()) {
                        return;
                    }
                    LOGGER.trace("Processing inbound from account sync absolute state");
                } else {
                    LOGGER.trace("Both attribute apriori delta and current projection is null. Not collecting the mapping.");
                    return;
                }

                PrismProperty currentAttribute = getCurrentAttribute(attributeName);
                LOGGER.trace("Collecting attribute inbound mapping for {} from:\n"
                        + "- a priori delta:\n{}\n"
                        + "- a priori attribute delta:\n{}\n"
                        + "- current attribute:\n{}", attributeName,
                        DebugUtil.debugDumpLazily(aPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(attributeAPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(currentAttribute, 1));

                //noinspection unchecked
                collectMappingsForItem(inboundMappingBean, attributeName, currentAttribute, attributeAPrioriDelta, (D)attributeDef, null);
            }
        }

        @Nullable
        private PrismProperty getCurrentAttribute(QName attributeName) {
            if (projectionCurrent != null) {
                return projectionCurrent.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName));
            } else {
                return null;
            }
        }

        private <V extends PrismValue, D extends ItemDefinition> void collectAssociationInbounds(QName associationName)
                throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException,
                SecurityViolationException, CommunicationException {

            ItemDelta<V, D> associationAPrioriDelta;
            if (aPrioriDelta != null) {
                associationAPrioriDelta = aPrioriDelta.findItemDelta(ShadowType.F_ASSOCIATION);
                // Shouldn't we check delta for specific association? (Instead of any association delta?)
                if (associationAPrioriDelta == null && !projectionContext.isFullShadow() && !LensUtil.hasDependentContext(context, projectionContext)) {
                    LOGGER.trace("Skipping inbound for {} in {}: Not a full shadow and account a priori delta exists, but doesn't have a change for the associations.",
                            associationName, projectionContext.getResourceShadowDiscriminator());
                    return;
                }
            } else {
                associationAPrioriDelta = null;
            }

            RefinedAssociationDefinition associationDef = java.util.Objects.requireNonNull(
                    projectionDefinition.findAssociationDefinition(associationName),
                    () -> "No definition for association " + associationName);

            if (associationDef.isIgnored(LayerType.MODEL)) {
                LOGGER.trace("Skipping inbound for association {} in {} because the association is ignored",
                        associationName, projectionContext.getResourceShadowDiscriminator());
                return;
            }

            List<MappingType> inboundMappingBeans = associationDef.getInboundMappingTypes();
            LOGGER.trace("Processing inbound for {} in {}; ({} mappings)", lazy(() -> PrettyPrinter.prettyPrint(associationName)),
                    projectionContext.getResourceShadowDiscriminator(), inboundMappingBeans.size());

            if (isNotReadable(associationDef.getLimitations(LayerType.MODEL))) {
                LOGGER.warn("Inbound mapping for non-readable association {} in {}, skipping",
                        associationName, projectionContext.getHumanReadableName());
                return;
            }

            for (MappingType inboundMappingBean : inboundMappingBeans) {

                loadIfStrong(inboundMappingBean);
                if (shouldStop()) {
                    return;
                }

                if (associationAPrioriDelta == null && !projectionContext.isFullShadow() && !LensUtil.hasDependentContext(context, projectionContext)) {
                    LOGGER.trace("Skipping association inbound for {} in {}: Not a full shadow and no a priori delta for processed property.",
                            associationName, projectionContext.getResourceShadowDiscriminator());
                    return;
                }

                if (associationAPrioriDelta != null) {
                    LOGGER.trace("Processing inbound from a priori delta");
                } else if (projectionCurrent != null) {
                    loadRequired();
                    if (shouldStop()) {
                        return;
                    }
                    LOGGER.trace("Processing inbound from account sync absolute state");

                    PrismContainer<ShadowAssociationType> currentAssociation = projectionCurrent.findContainer(ShadowType.F_ASSOCIATION);
                    if (currentAssociation == null) {
                        LOGGER.trace("No shadow association value");
                        return;
                    }
                } else {
                    LOGGER.trace("Both association apriori delta and current projection is null. Not collecting the mapping.");
                    return;
                }

                PrismContainer<ShadowAssociationType> filteredAssociations = getFilteredAssociations(associationName);
                resolveEntitlementsIfNeeded((ContainerDelta<ShadowAssociationType>) associationAPrioriDelta, filteredAssociations);

                LOGGER.trace("Collecting association inbound mapping for {} with:\n"
                        + "- a priori delta:\n{}\n"
                        + "- association a priori delta:\n{}\n"
                        + "- current state (filtered associations):\n{}", associationName,
                        DebugUtil.debugDumpLazily(aPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(associationAPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(filteredAssociations, 1));

                PrismContainerDefinition<ResourceObjectAssociationType> associationContainerDef = getAssociationContainerDefinition();
                //noinspection unchecked
                collectMappingsForItem(inboundMappingBean, associationName, (Item<V,D>)filteredAssociations,
                        associationAPrioriDelta, (D)associationContainerDef,
                        (VariableProducer<V>) (VariableProducer<PrismContainerValue<ShadowAssociationType>>) this::resolveEntitlement);
            }
        }

        private void resolveEntitlementsIfNeeded(ContainerDelta<ShadowAssociationType> associationAPrioriDelta,
                PrismContainer<ShadowAssociationType> currentAssociation) {
            Collection<PrismContainerValue<ShadowAssociationType>> associationsToResolve = new ArrayList<>();
            if (currentAssociation != null) {
                associationsToResolve.addAll(currentAssociation.getValues());
            }
            if (associationAPrioriDelta != null) {
                associationsToResolve.addAll(associationAPrioriDelta.getValues(ShadowAssociationType.class));
            }

            for (PrismContainerValue<ShadowAssociationType> associationToResolve : associationsToResolve) {
                PrismReference shadowRef = associationToResolve.findReference(ShadowAssociationType.F_SHADOW_REF);
                if (shadowRef == null) {
                    continue;
                }

                if (projectionContext.getEntitlementMap().containsKey(shadowRef.getOid())) {
                    shadowRef.getValue().setObject(projectionContext.getEntitlementMap().get(shadowRef.getOid()));
                } else {
                    try {
                        PrismObject<ShadowType> entitlement = beans.provisioningService.getObject(ShadowType.class, shadowRef.getOid(),
                                null, env.task, result);
                        projectionContext.getEntitlementMap().put(entitlement.getOid(), entitlement);
                    } catch (ObjectNotFoundException | CommunicationException | SchemaException | ConfigurationException
                            | SecurityViolationException | ExpressionEvaluationException e) {
                        LOGGER.error("failed to load entitlement.");
                        // TODO: can we just ignore and continue?
                        continue;
                    }
                }
            }
        }


        @Nullable
        private PrismContainer<ShadowAssociationType> getFilteredAssociations(QName associationName)
                throws SchemaException {
            LOGGER.trace("Getting filtered associations for {}", associationName);
            PrismContainer<ShadowAssociationType> currentAssociation = projectionCurrent != null ?
                    projectionCurrent.findContainer(ShadowType.F_ASSOCIATION) : null;

            if (currentAssociation != null) {
                PrismContainer<ShadowAssociationType> filteredAssociations = currentAssociation.getDefinition().instantiate();
                Collection<PrismContainerValue<ShadowAssociationType>> filteredAssociationValues = currentAssociation.getValues().stream()
                        .filter(rVal -> associationName.equals(rVal.asContainerable().getName()))
                        .map(PrismContainerValue::clone)
                        .collect(Collectors.toList());
                beans.prismContext.adopt(filteredAssociations);
                // Make sure all the modified/cloned associations have proper definition
                filteredAssociations.applyDefinition(currentAssociation.getDefinition(), false);
                filteredAssociations.addAll(filteredAssociationValues);
                return filteredAssociations;
            } else {
                return null;
            }
        }

        private void resolveEntitlement(PrismContainerValue<ShadowAssociationType> value, ExpressionVariables variables) {
            LOGGER.trace("Producing value {} ", value);
            PrismReference entitlementRef = value.findReference(ShadowAssociationType.F_SHADOW_REF);
            if (entitlementRef == null) {
                LOGGER.trace("No shadow ref for {}. Skipping resolving entitlement", value);
                return;
            }
            PrismObject<ShadowType> entitlement = projectionContext.getEntitlementMap().get(entitlementRef.getOid());
            LOGGER.trace("Resolved entitlement {}", entitlement);
            variables.put(ExpressionConstants.VAR_ENTITLEMENT, entitlement, entitlement.getDefinition());
        }

        private boolean isNotReadable(PropertyLimitations limitations) {
            if (limitations != null) {
                PropertyAccessType access = limitations.getAccess();
                if (access != null) {
                    return access.isRead() == null || !access.isRead();
                }
            }
            return false;
        }

        private boolean shouldStop() {
            return stop || isBroken();
        }

        private boolean isBroken() {
            return projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN;
        }

        private void collectAuxiliaryObjectClassInbounds() throws SchemaException, ExpressionEvaluationException,
                ObjectNotFoundException, ConfigurationException, SecurityViolationException, CommunicationException {

            ResourceBidirectionalMappingAndDefinitionType auxiliaryObjectClassMappings = projectionDefinition.getAuxiliaryObjectClassMappings();
            if (auxiliaryObjectClassMappings == null) {
                return;
            }
            List<MappingType> inboundMappingBeans = auxiliaryObjectClassMappings.getInbound();
            LOGGER.trace("Processing inbound for auxiliary object class in {}; ({} mappings)",
                    projectionContext.getResourceShadowDiscriminator(), inboundMappingBeans.size());
            if (inboundMappingBeans.isEmpty()) {
                return;
            }

            if (aPrioriDelta != null) {
                PropertyDelta<QName> attributeAPrioriDelta = aPrioriDelta.findPropertyDelta(ShadowType.F_AUXILIARY_OBJECT_CLASS);
                if (attributeAPrioriDelta == null && !projectionContext.isFullShadow() && !LensUtil.hasDependentContext(context, projectionContext)) {
                    LOGGER.trace("Skipping inbound for auxiliary object class in {}: Not a full shadow and account a priori delta exists, but doesn't have change for processed property.",
                            projectionContext.getResourceShadowDiscriminator());
                    return;
                }
            }

            // Make we always have full shadow when dealing with auxiliary object classes.
            // Unlike structural object class the auxiliary object classes may have changed
            // on the resource
            loadRequired();
            if (shouldStop()) {
                return;
            }

            PrismPropertyDefinition<QName> auxiliaryObjectClassPropertyDefinition = beans.prismContext.getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(ShadowType.class)
                    .findPropertyDefinition(ShadowType.F_AUXILIARY_OBJECT_CLASS);

            for (MappingType inboundMappingBean : inboundMappingBeans) {
                PrismProperty<QName> oldAccountProperty = projectionCurrent.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
                LOGGER.trace("Processing inbound from account sync absolute state (currentAccount): {}", oldAccountProperty);
                collectMappingsForItem(inboundMappingBean, ShadowType.F_AUXILIARY_OBJECT_CLASS,
                        oldAccountProperty, null, auxiliaryObjectClassPropertyDefinition,
                        null);
            }
        }

        private <V extends PrismValue, D extends ItemDefinition> void collectMappingsForItem(MappingType inboundMappingBean,
                QName projectionItemName, Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta,
                D itemDefinition, VariableProducer<V> variableProducer) throws ObjectNotFoundException, SchemaException,
                ConfigurationException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

            if (currentProjectionItem != null && currentProjectionItem.hasRaw()) {
                throw new SystemException("Property "+currentProjectionItem+" has raw parsing state, such property cannot be used in inbound expressions");
            }

            ResourceType resource = projectionContext.getResource();

            if (!MappingImpl.isApplicableToChannel(inboundMappingBean, context.getChannel())) {
                LOGGER.trace("Mapping is not applicable to channel {}", context.getChannel());
                return;
            }

            PrismObject<ShadowType> shadowNew = projectionContext.getObjectNew();
            PrismObjectDefinition<ShadowType> shadowNewDef;
            if (shadowNew != null) {
                shadowNewDef = shadowNew.getDefinition();
            } else {
                shadowNewDef = beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
            }

            PrismObject<F> focus = getCurrentFocus();
            PrismObjectDefinition<F> focusNewDef;
            if (focus != null) {
                focusNewDef = focus.getDefinition();
            } else {
                focusNewDef = context.getFocusContextRequired().getObjectDefinition();
            }

            if (currentProjectionItem != null) {
                beans.projectionValueMetadataCreator.setValueMetadata(currentProjectionItem, projectionContext);
            }
            if (itemAPrioriDelta != null) {
                beans.projectionValueMetadataCreator.setValueMetadata(itemAPrioriDelta, projectionContext);
            }

            Source<V,D> defaultSource = new Source<>(currentProjectionItem, itemAPrioriDelta, null, ExpressionConstants.VAR_INPUT_QNAME, itemDefinition);
            defaultSource.recompute();

            MappingBuilder<V,D> builder = beans.mappingFactory.<V,D>createMappingBuilder()
                    .mappingBean(inboundMappingBean)
                    .mappingKind(MappingKindType.INBOUND)
                    .implicitSourcePath(ShadowType.F_ATTRIBUTES.append(projectionItemName))
                    .contextDescription("inbound expression for "+projectionItemName+" in "+resource)
                    .defaultSource(defaultSource)
                    .targetContext(LensUtil.getFocusDefinition(context))
                    .addVariableDefinition(ExpressionConstants.VAR_USER, focus, focusNewDef)
                    .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus, focusNewDef)
                    .addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS)
                    .addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadowNew, shadowNewDef)
                    .addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadowNew, shadowNewDef)
                    .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadowNew, shadowNewDef)
                    .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                    .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                    .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource, resource.asPrismObject().getDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, context.getSystemConfiguration(), context.getSystemConfiguration().getDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_OPERATION, context.getFocusContext().getOperation().getValue(), String.class)
                    .variableResolver(variableProducer)
                    .valuePolicySupplier(createStringPolicyResolver())
                    .originType(OriginType.INBOUND)
                    .originObject(resource);

            if (!context.getFocusContext().isDelete()) {
                assert focus != null;
                TypedValue<PrismObject<F>> targetContext = new TypedValue<>(focus);
                ExpressionVariables variables = builder.getVariables();
                Collection<V> originalValues = ExpressionUtil.computeTargetValues(inboundMappingBean.getTarget(), targetContext, variables, beans.mappingFactory.getObjectResolver() , "resolving target values",
                        beans.prismContext, env.task, result);
                builder.originalTargetValues(originalValues);
            }

            MappingImpl<V, D> mapping = builder.build();

            if (checkWeakSkip(mapping, focus)) {
                LOGGER.trace("Skipping because of mapping is weak and focus property has already a value");
                return;
            }

            InboundMappingStruct<V,D> mappingStruct = new InboundMappingStruct<>(mapping, projectionContext);

            ItemPath targetFocusItemPath = mapping.getOutputPath();
            if (ItemPath.isEmpty(targetFocusItemPath)) {
                throw new ConfigurationException("Empty target path in "+mapping.getContextDescription());
            }
            PrismObjectDefinition<F> focusDefinition = context.getFocusContext().getObjectDefinition();
            ItemDefinition targetItemDef = focusDefinition.findItemDefinition(targetFocusItemPath);
            if (targetItemDef == null) {
                throw new SchemaException("No definition for focus property "+targetFocusItemPath+", cannot process inbound expression in "+resource);
            }

            addToMappingsMap(mappingsToTarget, targetFocusItemPath, mappingStruct);
        }

        private void loadIfAnyStrong() throws SchemaException {
            if (!projectionContext.isFullShadow() && !projectionContext.isTombstone() && hasAnyStrongMapping()) {
                LOGGER.trace("There are strong inbound mappings, but the shadow hasn't be fully loaded yet. Trying to load full shadow now.");
                load();
            }
        }

        private void loadIfStrong(MappingType inboundMappingBean) throws SchemaException {
            if (!projectionContext.isFullShadow() && !projectionContext.isDelete() && inboundMappingBean.getStrength() == MappingStrengthType.STRONG) {
                LOGGER.trace("There is an inbound mapping with strength == STRONG. Trying to load full account now.");
                load();
            }
        }

        /**
         * This is the "ultimate load". If not successful we stop collecting from this projection context.
         */
        private void loadRequired() throws SchemaException {
            if (!projectionContext.isFullShadow()) {
                LOGGER.warn("Attempted to execute inbound expression on account shadow {} WITHOUT full account. Trying to load the account now.", projectionContext.getOid());      // todo change to trace level eventually
                load();
                if (!isBroken() && !projectionContext.isFullShadow()) {
                    if (projectionContext.getResourceShadowDiscriminator().getOrder() > 0) {
                        // higher-order context. It is OK not to load this
                        LOGGER.trace("Skipped load of higher-order account with shadow OID {} skipping inbound processing on it", projectionContext.getOid());
                    }
                    stop = true;
                }
            }
        }

        private void load() throws SchemaException {
            projectionCurrent = loadProjection(projectionContext, projectionCurrent);
        }

        private boolean hasAnyStrongMapping() {
            for (QName attributeName : projectionDefinition.getNamesOfAttributesWithInboundExpressions()) {
                RefinedAttributeDefinition<?> definition = projectionDefinition.findAttributeDefinition(attributeName);
                for (MappingType inboundMapping : definition.getInboundMappingTypes()) {
                    if (inboundMapping.getStrength() == MappingStrengthType.STRONG) {
                        return true;
                    }
                }
            }
            for (QName associationName : projectionDefinition.getNamesOfAssociationsWithInboundExpressions()) {
                RefinedAssociationDefinition definition = projectionDefinition.findAssociationDefinition(associationName);
                for (MappingType inboundMapping : definition.getInboundMappingTypes()) {
                    if (inboundMapping.getStrength() == MappingStrengthType.STRONG) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private PrismObject<ShadowType> loadProjection(LensProjectionContext projContext, PrismObject<ShadowType> accountCurrent)
            throws SchemaException {
        try {
            beans.contextLoader.loadFullShadow(context, projContext, "inbound", env.task, result);
            accountCurrent = projContext.getObjectCurrent();
        } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.warn("Couldn't load account with shadow OID {} because of {}, setting context as broken and skipping inbound processing on it", projContext.getOid(), e.getMessage());
            projContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
        }
        return accountCurrent;
    }


    /**
     * A priori delta is a delta that was executed in a previous "step". That means it is either delta from a previous
     * wave or a sync delta (in wave 0).
     */
    private ObjectDelta<ShadowType> getAPrioriDelta(
            LensProjectionContext accountContext) throws SchemaException {
        int wave = context.getProjectionWave();
        if (wave == 0) {
            return accountContext.getSyncDelta();
        }
        if (wave == accountContext.getWave() + 1) {
            // If this resource was processed in a previous wave ....
            // Normally, we take executed delta. However, there are situations (like preview changes - i.e. projector without execution),
            // when there is no executed delta. In that case we take standard primary + secondary delta.
            // TODO is this really correct? Think if the following can happen:
            // - NOT previewing
            // - no executed deltas but
            // - existing primary/secondary delta.
            List<LensObjectDeltaOperation<ShadowType>> executed = accountContext.getExecutedDeltas();
            if (!executed.isEmpty()) {
                return executed.get(executed.size()-1).getObjectDelta();
            } else {
                return accountContext.getDelta();
            }
        }
        return null;
    }

    private <F extends ObjectType> boolean checkWeakSkip(MappingImpl<?,?> inbound, PrismObject<F> newUser) throws SchemaException {
        if (inbound.getStrength() != MappingStrengthType.WEAK) {
            return false;
        }
        if (newUser == null) {
            return false;
        }
        PrismProperty<?> property = newUser.findProperty(inbound.getOutputPath());
        if (property != null && !property.isEmpty()) {
            return true;
        }
        return false;
    }


    private void addToMappingsMap(Map<ItemPath, List<InboundMappingStruct<?, ?>>> mappingsToTarget,
            ItemPath itemPath, InboundMappingStruct<?, ?> mappingStruct) {
        mappingsToTarget
                .computeIfAbsent(itemPath, k -> new ArrayList<>())
                .add(mappingStruct);
    }

    /**
     * Evaluate mappings consolidated from all the projections. There may be mappings from different projections to the same target.
     * We want to merge their values. Otherwise those mappings will overwrite each other.
     */
    private void evaluateMappings()
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, ConfigurationException, SecurityViolationException, CommunicationException {

        PrismObject<F> focusNew = context.getFocusContext().getObjectCurrent();
        if (focusNew == null) {
            focusNew = context.getFocusContext().getObjectNew();
        }

        for (Map.Entry<ItemPath, List<InboundMappingStruct<?, ?>>> mappingsToTargetEntry : mappingsToTarget.entrySet()) {
            evaluateMappingsForTargetItem(context, env.task, result, focusNew, mappingsToTargetEntry);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition> void evaluateMappingsForTargetItem(LensContext<F> context,
            Task task, OperationResult result, PrismObject<F> focusNew,
            Map.Entry<ItemPath, List<InboundMappingStruct<?, ?>>> mappingsToTargetEntry)
            throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException,
            SecurityViolationException,
            ConfigurationException,
            CommunicationException {
        D outputDefinition = null;
        boolean rangeCompletelyDefined = true;
        DeltaSetTriple<ItemValueWithOrigin<V, D>> allTriples = beans.prismContext.deltaFactory().createDeltaSetTriple();
        for (InboundMappingStruct<?, ?> mappingsToTargetStruct : mappingsToTargetEntry.getValue()) {
            //noinspection unchecked
            MappingImpl<V, D> mapping = (MappingImpl<V, D>) mappingsToTargetStruct.getMapping();
            LensProjectionContext projectionCtx = mappingsToTargetStruct.getProjectionContext();
            outputDefinition = mapping.getOutputDefinition();
            if (!mapping.hasTargetRange()) {
                rangeCompletelyDefined = false;
            }

            beans.mappingEvaluator.evaluateMapping(mapping, context, projectionCtx, task, result);

            DeltaSetTriple<ItemValueWithOrigin<V, D>> iwwoTriple = ItemValueWithOrigin.createOutputTriple(mapping,
                    beans.prismContext);
            LOGGER.trace("Inbound mapping for {}\nreturned triple:\n{}",
                    DebugUtil.shortDumpLazily(mapping.getDefaultSource()), DebugUtil.debugDumpLazily(iwwoTriple, 1));
            if (iwwoTriple == null) {
                continue;
            }
            if (projectionCtx.isDelete()) {
                // Projection is going to be deleted. All the values that this projection was giving should be removed now.
                LOGGER.trace("Projection is going to be deleted, setting values from this projection to minus set");
                allTriples.addAllToMinusSet(iwwoTriple.getPlusSet());
                allTriples.addAllToMinusSet(iwwoTriple.getZeroSet());
                allTriples.addAllToMinusSet(iwwoTriple.getMinusSet());
            } else {
                allTriples.merge(iwwoTriple);
            }

        }
        DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidatedTriples = consolidateTriples(allTriples);

        LOGGER.trace("Consolidated triples for mapping for item {}\n{}", mappingsToTargetEntry.getKey(), consolidatedTriples.debugDumpLazily(1));

        ItemDelta<V, D> focusItemDelta = collectOutputDelta(outputDefinition, mappingsToTargetEntry.getKey(), focusNew,
                consolidatedTriples, rangeCompletelyDefined);

        if (focusItemDelta != null && !focusItemDelta.isEmpty()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Created delta (from inbound expressions for {})\n{}", focusItemDelta.getElementName(), focusItemDelta.debugDump(1));
            }
            context.getFocusContext().swallowToProjectionWaveSecondaryDelta(focusItemDelta);
            context.recomputeFocus();
        } else {
            LOGGER.trace("Created delta (from inbound expressions for {}) was null or empty.", focusItemDelta.getElementName());
        }
    }

    private <V extends PrismValue, D extends ItemDefinition> DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidateTriples(DeltaSetTriple<ItemValueWithOrigin<V, D>> originTriples) {
        // Meaning of the resulting triple:
        // values in PLUS set will be added (valuesToAdd in delta)
        // values in MINUS set will be removed (valuesToDelete in delta)
        // values in ZERO set will be compared with existing values in
        // user property
        // the differences will be added to delta

        Collection<ItemValueWithOrigin<V, D>> consolidatedZeroSet = new HashSet<>();
        Collection<ItemValueWithOrigin<V, D>> consolidatedPlusSet = new HashSet<>();
        Collection<ItemValueWithOrigin<V, D>> consolidatedMinusSet = new HashSet<>();

        if (originTriples != null) {
            if (originTriples.hasPlusSet()) {
                Collection<ItemValueWithOrigin<V, D>> plusSet = originTriples.getPlusSet();
                LOGGER.trace("Consolidating plusSet from origin:\n {}", DebugUtil.debugDumpLazily(plusSet));


                for (ItemValueWithOrigin<V, D> plusValue : plusSet) {
                    boolean consolidated = false;
                    if (originTriples.hasMinusSet()) {
                        for (ItemValueWithOrigin<V, D> minusValue: originTriples.getMinusSet()) {
                            if (minusValue.getItemValue().equals(plusValue.getItemValue(), EquivalenceStrategy.REAL_VALUE)) {
                                LOGGER.trace(
                                        "Removing value {} from minus set -> moved to the zero, because the same value present in plus and minus set at the same time",
                                        minusValue.debugDumpLazily());
                                consolidatedMinusSet.remove(minusValue);
                                consolidatedPlusSet.remove(plusValue);
                                consolidatedZeroSet.add(minusValue);
                                consolidated = true;
                            }
                        }
                    }

                    if (originTriples.hasZeroSet()) {
                        for (ItemValueWithOrigin<V, D> zeroValue: originTriples.getZeroSet()) {
                            if (zeroValue.getItemValue().equals(plusValue.getItemValue(), EquivalenceStrategy.REAL_VALUE)) {
                                LOGGER.trace(
                                        "Removing value {} from plus set -> moved to the zero, because the same value present in plus and minus set at the same time",
                                        zeroValue.debugDumpLazily());
                                consolidatedPlusSet.remove(plusValue);
                                consolidated = true;
                            }
                        }
                    }
                    if (!consolidated) { //&& !PrismValue.containsRealValue(consolidatedPlusSet, plusValue)) {
                        consolidatedPlusSet.add(plusValue);
                    }
                }
            }


            if (originTriples.hasZeroSet()) {
                Collection<ItemValueWithOrigin<V, D>> zeroSet = originTriples.getZeroSet();
                LOGGER.trace("Consolidating zero set from origin:\n {}", DebugUtil.debugDumpLazily(zeroSet));

                for (ItemValueWithOrigin<V, D> zeroValue : zeroSet) {
                    boolean consolidated = false;
                    if (originTriples.hasMinusSet()) {
                        for (ItemValueWithOrigin<V, D> minusValue : originTriples.getMinusSet()) {
                            if (minusValue.getItemValue().equals(zeroValue.getItemValue(), EquivalenceStrategy.REAL_VALUE)) {
                                LOGGER.trace(
                                        "Removing value {} from minus set -> moved to the zero, because the same value present in zero and minus set at the same time",
                                        minusValue.debugDumpLazily());
                                consolidatedMinusSet.remove(minusValue);
                                consolidatedZeroSet.add(minusValue);
                                consolidated = true;
                            }
                        }

                    }

                    if (originTriples.hasPlusSet()) {
                        for (ItemValueWithOrigin<V, D> plusValue : originTriples.getPlusSet()) {
                            if (plusValue.getItemValue().equals(zeroValue.getItemValue(), EquivalenceStrategy.REAL_VALUE)) {
                                LOGGER.trace(
                                        "Removing value {} from plus set -> moved to the zero, because the same value present in zero and plus set at the same time",
                                        plusValue.debugDumpLazily());
                                consolidatedPlusSet.remove(plusValue);
                                consolidatedZeroSet.add(plusValue);
                                consolidated = true;
                            }
                        }

                    }
                    if (!consolidated) {// && !PrismValue.containsRealValue(consolidatedZeroSet, zeroValue)) {
                        consolidatedZeroSet.add(zeroValue);
                    }
                }


            }



            if (originTriples.hasMinusSet()) {
                Collection<ItemValueWithOrigin<V, D>> minusSet = originTriples.getMinusSet();
                LOGGER.trace("Consolidating minus set from origin:\n {}", DebugUtil.debugDumpLazily(minusSet));

                for (ItemValueWithOrigin<V, D> minusValue : minusSet){
                    boolean consolidated = false;
                    if (originTriples.hasPlusSet()) {
                        for (ItemValueWithOrigin<V, D> plusValue : originTriples.getPlusSet()) {
                            if (plusValue.getItemValue().equals(minusValue.getItemValue(), EquivalenceStrategy.REAL_VALUE)) {
                                LOGGER.trace(
                                        "Removing value {} from minus set -> moved to the zero, because the same value present in plus and minus set at the same time",
                                        plusValue.debugDumpLazily());
                                consolidatedPlusSet.remove(plusValue);
                                consolidatedMinusSet.remove(minusValue);
                                consolidatedZeroSet.add(minusValue);
                                consolidated = true;
                            }
                        }
                    }


                    if (originTriples.hasZeroSet()) {
                        for (ItemValueWithOrigin<V, D> zeroValue : originTriples.getZeroSet()) {
                            if (zeroValue.getItemValue().equals(minusValue.getItemValue(), EquivalenceStrategy.REAL_VALUE)) {
                                LOGGER.trace(
                                        "Removing value {} from minus set -> moved to the zero, because the same value present in plus and minus set at the same time",
                                        zeroValue.debugDumpLazily());
                                consolidatedMinusSet.remove(minusValue);
                                consolidatedZeroSet.add(zeroValue);
                                consolidated = true;
                            }
                        }
                    }

                    if (!consolidated) { // && !PrismValue.containsRealValue(consolidatedMinusSet, minusValue)) {
                        consolidatedMinusSet.add(minusValue);
                    }

                }

            }
        }

        DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidatedTriples = beans.prismContext.deltaFactory().createDeltaSetTriple();
        consolidatedTriples.addAllToMinusSet(consolidatedMinusSet);
        consolidatedTriples.addAllToPlusSet(consolidatedPlusSet);
        consolidatedTriples.addAllToZeroSet(consolidatedZeroSet);
        return consolidatedTriples;
    }


    private <V extends PrismValue, D extends ItemDefinition> ItemDelta<V, D> collectOutputDelta(
            D outputDefinition, ItemPath outputPath, PrismObject<F> focusNew,
            DeltaSetTriple<ItemValueWithOrigin<V, D>> consolidatedTriples, boolean rangeCompletelyDefined) throws SchemaException {

        ItemDelta outputFocusItemDelta = outputDefinition.createEmptyDelta(outputPath);
        Item<V, D> targetFocusItem = null;
        if (focusNew != null) {
            targetFocusItem = focusNew.findItem(outputPath);
        }
        boolean isAssignment = FocusType.F_ASSIGNMENT.equivalent(outputPath);

        Item<V,D> shouldBeItem = outputDefinition.instantiate();
        if (consolidatedTriples != null) {

            Collection<ItemValueWithOrigin<V, D>> shouldBeItemValues = consolidatedTriples.getNonNegativeValues();
            for (ItemValueWithOrigin<V, D> itemWithOrigin : shouldBeItemValues) {
                V clonedValue = LensUtil.cloneAndApplyAssignmentOrigin(itemWithOrigin.getItemValue(),
                        isAssignment,
                        shouldBeItemValues);
                shouldBeItem.add(clonedValue, EquivalenceStrategy.REAL_VALUE);
            }

            if (consolidatedTriples.hasPlusSet()) {

                boolean alreadyReplaced = false;
                for (ItemValueWithOrigin<V, D> valueWithOrigin : consolidatedTriples.getPlusSet()) {
                    MappingImpl<V,D> originMapping = (MappingImpl) valueWithOrigin.getMapping();
                    if (targetFocusItem == null) {
                        targetFocusItem = focusNew.findItem(originMapping.getOutputPath());
                    }
                    V value = valueWithOrigin.getItemValue();
                    if (targetFocusItem != null && targetFocusItem.contains(value, EquivalenceStrategy.REAL_VALUE)) {
                        continue;
                    }

                    if (outputFocusItemDelta == null) {
                        outputFocusItemDelta = outputDefinition.createEmptyDelta(originMapping.getOutputPath());
                    }

                    // if property is not multi value replace existing
                    // attribute
                    if (targetFocusItem != null && !targetFocusItem.getDefinition().isMultiValue()
                            && !targetFocusItem.isEmpty()) {
                        Collection<V> replace = new ArrayList<>();
                        replace.add(LensUtil.cloneAndApplyAssignmentOrigin(value, isAssignment, originMapping.getMappingBean()));
                        outputFocusItemDelta.setValuesToReplace(replace);


                        if (alreadyReplaced) {
                            LOGGER.warn("Multiple values for a single-valued property {}; duplicate value = {}", targetFocusItem,
                                    value);
                        } else {
                            alreadyReplaced = true;
                        }
                    } else {
                        outputFocusItemDelta.addValueToAdd(LensUtil.cloneAndApplyAssignmentOrigin(value, isAssignment, originMapping.getMappingBean()));
                    }
                }
            }

            if (consolidatedTriples.hasMinusSet()) {
                LOGGER.trace("Checking account sync property delta values to delete");
                for (ItemValueWithOrigin<V, D> valueWithOrigin : consolidatedTriples.getMinusSet()) {
                    V value = valueWithOrigin.getItemValue();

                    if (targetFocusItem == null || targetFocusItem.contains(value, EquivalenceStrategy.REAL_VALUE)) {
                        if (!outputFocusItemDelta.isReplace()) {
                            // This is not needed if we are going to
                            // replace. In fact it might cause an error.
                            outputFocusItemDelta.addValueToDelete(value);
                        }
                    }
                }
            }

        } else {
            // triple == null
            // the mapping is not applicable. Nothing to do.
        }

        // We want to diff the values that should be in the target property and the values that are already there.
        // We want to add any values that should be there but are not there yet. Inbound mappings are not completely
        // relative. E.g. we often get new state of an account, but we do not know what changes have happened there.
        // Therefore it can easily happen that mapping will produce a value in a zero set, but that value is not
        // present in the target property.
        //
        // This is some kind of "inbound reconciliation".
        //
        // Adding values that are not present is quite easy and reliable. But the real problem is when to remove a value.
        // Default behavior in midPoint is to be tolerant. In case that non-tolerant behavior is needed, range can be used.
        // However, inbound mappings are slightly special. We do not have the usual relativistic behavior here.
        // Non-tolerant behavior would work fine for multivalue properties, as for that range definition is usually
        // needed anyway. But for single-value properties the behavior may not be intuitive. As inbound mappings are
        // almost always non-relativistic, there is no explicit delta that would remove a value. Therefore tolerant
        // inbound mapping would not remove any values.
        // The decision is that default behavior for single-value properties is to be non-tolerant. This is intuitive behavior.
        // And as single-value properties cannot have more than one value anyway, it does not really has a big potential for any harm.

        boolean tolerateTargetValues = !outputDefinition.isSingleValue() || rangeCompletelyDefined;

        if (targetFocusItem != null) {
            LOGGER.trace("Comparing focus item:\n{}\nto should be item:\n{}",
                    DebugUtil.debugDumpLazily(targetFocusItem, 1), DebugUtil.debugDumpLazily(shouldBeItem, 1));
            ItemDelta diffDelta = targetFocusItem.diff(shouldBeItem);
            LOGGER.trace("The difference is:\n{}", DebugUtil.debugDumpLazily(diffDelta, 1));

            if (diffDelta != null) {
                // this is probably not correct, as the default for
                // inbounds should be TRUE
                if (tolerateTargetValues) {
                    if (diffDelta.isReplace()) {
                        if (diffDelta.getValuesToReplace().isEmpty()) {
                            diffDelta.resetValuesToReplace();
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Removing empty replace part of the diff delta because mapping is tolerant:\n{}",
                                        diffDelta.debugDump());
                            }
                        } else {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace(
                                        "Making sure that the replace part of the diff contains old values delta because mapping is tolerant:\n{}",
                                        diffDelta.debugDump());
                            }
                            for (Object shouldBeValueObj : shouldBeItem.getValues()) {
                                PrismValue shouldBeValue = (PrismValue) shouldBeValueObj;
                                if (!PrismValueCollectionsUtil.containsRealValue(diffDelta.getValuesToReplace(), shouldBeValue)) {
                                    diffDelta.addValueToReplace(shouldBeValue.clone());
                                }
                            }
                        }
                    } else {
                        diffDelta.resetValuesToDelete();
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Removing delete part of the diff delta because mapping settings are tolerateTargetValues={}:\n{}",
                                    tolerateTargetValues, diffDelta.debugDump(1));
                        }
                    }
                }

                diffDelta.setElementName(ItemPath.toName(outputPath.last()));
                diffDelta.setParentPath(outputPath.allExceptLast());
                outputFocusItemDelta.merge(diffDelta);
            }

        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Adding user property because inbound say so (account doesn't contain that value):\n{}",
                        shouldBeItem.getValues());
            }
            // if user property doesn't exist we have to add it (as delta), because inbound say so
            outputFocusItemDelta.addValuesToAdd(shouldBeItem.getClonedValues());
        }


        return outputFocusItemDelta;
    }

    private <V extends PrismValue, D extends ItemDefinition> boolean hasRange(List<MappingImpl<V, D>> mappings) {
        for (MappingImpl<V, D> mapping : mappings) {
            if (mapping.hasTargetRange()) {
                return true;
            }
        }
        return false;
    }


    private ConfigurableValuePolicySupplier createStringPolicyResolver() {
        return new ConfigurableValuePolicySupplier() {
            private ItemDefinition outputDefinition;

            @Override
            public void setOutputDefinition(ItemDefinition outputDefinition) {
                this.outputDefinition = outputDefinition;
            }

            @Override
            public ValuePolicyType get(OperationResult result) {
                if (outputDefinition.getItemName().equals(PasswordType.F_VALUE)) {
                    return beans.credentialsProcessor.determinePasswordPolicy(context.getFocusContext());
                } else {
                    return null;
                }
            }
        };
    }

    /**
     * Processing for special (fixed-schema) properties such as credentials and activation.
     */
    private void evaluateSpecialPropertyInbound(ResourceBidirectionalMappingType biMapping, ItemPath sourceTargetPath,
            PrismObject<F> newUser, LensProjectionContext projCtx) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (biMapping != null) {
            evaluateSpecialPropertyInbound(biMapping.getInbound(), sourceTargetPath, sourceTargetPath, newUser, projCtx);
        }
    }

    /**
     * Processing for special (fixed-schema) properties such as credentials and activation.
     */
    private void evaluateSpecialPropertyInbound(Collection<MappingType> inboundMappingBeans,
            ItemPath sourcePath, ItemPath targetPath, PrismObject<F> newUser, LensProjectionContext projContext) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {

        if (inboundMappingBeans == null || inboundMappingBeans.isEmpty() || newUser == null || !projContext.isFullShadow()) {
            return;
        }

        ObjectDelta<F> userPrimaryDelta = context.getFocusContext().getPrimaryDelta();
        PropertyDelta primaryPropDelta;
        if (userPrimaryDelta != null) {
            primaryPropDelta = userPrimaryDelta.findPropertyDelta(targetPath);
            if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
                // Replace primary delta overrides any inbound
                return;
            }
        }

        ObjectDelta<F> userSecondaryDelta = context.getFocusContext().getProjectionWaveSecondaryDelta();
        if (userSecondaryDelta != null) {
            PropertyDelta<?> delta = userSecondaryDelta.findPropertyDelta(targetPath);
            if (delta != null) {
                //remove delta if exists, it will be handled by inbound
                userSecondaryDelta.getModifications().remove(delta);
            }
        }

        MappingInitializer<PrismValue, ItemDefinition> initializer =
                (builder) -> {
                    if (projContext.getObjectNew() == null) {
                        projContext.recompute();
                        if (projContext.getObjectNew() == null) {
                            // Still null? something must be really wrong here.
                            String message = "Recomputing account " + projContext.getResourceShadowDiscriminator()
                                    + " results in null new account. Something must be really broken.";
                            LOGGER.error(message);
                            LOGGER.trace("Account context:\n{}", projContext.debugDumpLazily());
                            throw new SystemException(message);
                        }
                    }

                    ObjectDelta<ShadowType> aPrioriShadowDelta = getAPrioriDelta(projContext);
                    ItemDelta<PrismPropertyValue<?>,PrismPropertyDefinition<?>> specialAttributeDelta = null;
                    if (aPrioriShadowDelta != null){
                        specialAttributeDelta = aPrioriShadowDelta.findItemDelta(sourcePath);
                    }
                    ItemDeltaItem<PrismPropertyValue<?>,PrismPropertyDefinition<?>> sourceIdi = projContext.getObjectDeltaObject().findIdi(sourcePath);
                    if (specialAttributeDelta == null){
                        specialAttributeDelta = sourceIdi.getDelta();
                    }
                    Source<PrismPropertyValue<?>,PrismPropertyDefinition<?>> source = new Source<>(
                            sourceIdi.getItemOld(), specialAttributeDelta, sourceIdi.getItemOld(),
                            ExpressionConstants.VAR_INPUT_QNAME,
                            sourceIdi.getDefinition());
                    builder.defaultSource(source)
                            .addVariableDefinition(ExpressionConstants.VAR_USER, newUser, UserType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_FOCUS, newUser, FocusType.class)
                            .addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);

                    PrismObject<ShadowType> accountNew = projContext.getObjectNew();
                    builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountNew, ShadowType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_SHADOW, accountNew, ShadowType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, accountNew, ShadowType.class)
                            .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                            .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                            .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projContext.getResource(), ResourceType.class)
                            .valuePolicySupplier(createStringPolicyResolver())
                            .mappingKind(MappingKindType.INBOUND)
                            .implicitSourcePath(sourcePath)
                            .implicitTargetPath(targetPath)
                            .originType(OriginType.INBOUND)
                            .originObject(projContext.getResource());

                    return builder;
                };

        MappingOutputProcessor<PrismValue> processor =
                (mappingOutputPath, outputStruct) -> {
                    PrismValueDeltaSetTriple<PrismValue> outputTriple = outputStruct.getOutputTriple();
                    if (outputTriple == null){
                        LOGGER.trace("Mapping for property {} evaluated to null. Skipping inbound processing for that property.", sourcePath);
                        return false;
                    }

                    ObjectDelta<F> userSecondaryDeltaInt = context.getFocusContext().getProjectionWaveSecondaryDelta();
                    if (userSecondaryDeltaInt != null) {
                        PropertyDelta<?> delta = userSecondaryDeltaInt.findPropertyDelta(targetPath);
                        if (delta != null) {
                            //remove delta if exists, it will be handled by inbound
                            userSecondaryDeltaInt.getModifications().remove(delta);
                        }
                    }

                    PrismObjectDefinition<F> focusDefinition = context.getFocusContext().getObjectDefinition();
                    PrismProperty result = focusDefinition.findPropertyDefinition(targetPath).instantiate();
                    result.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));

                    PrismProperty targetPropertyNew = newUser.findOrCreateProperty(targetPath);
                    PropertyDelta<?> delta;
                    if (ProtectedStringType.COMPLEX_TYPE.equals(targetPropertyNew.getDefinition().getTypeName())) {
                        // We have to compare this in a special way. The cipherdata may be different due to a different
                        // IV, but the value may still be the same
                        ProtectedStringType resultValue = (ProtectedStringType) result.getRealValue();
                        ProtectedStringType targetPropertyNewValue = (ProtectedStringType) targetPropertyNew.getRealValue();
                        try {
                            if (beans.protector.compareCleartext(resultValue, targetPropertyNewValue)) {
                                delta = null;
                            } else {
                                delta = targetPropertyNew.diff(result);
                            }
                        } catch (EncryptionException e) {
                            throw new SystemException(e.getMessage(), e);
                        }
                    } else {
                        delta = targetPropertyNew.diff(result);
                    }
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("targetPropertyNew:\n{}\ndelta:\n{}", targetPropertyNew.debugDump(1), DebugUtil.debugDump(delta, 1));
                    }
                    if (delta != null && !delta.isEmpty()) {
                        delta.setParentPath(targetPath.allExceptLast());
                        if (!context.getFocusContext().alreadyHasDelta(delta)){
                            context.getFocusContext().swallowToProjectionWaveSecondaryDelta(delta);
                        }
                    }
                    return false;
                };

        MappingEvaluatorParams<PrismValue, ItemDefinition, F, F> params = new MappingEvaluatorParams<>();
        params.setMappingTypes(inboundMappingBeans);
        params.setMappingDesc("inbound mapping for " + sourcePath + " in " + projContext.getResource());
        params.setNow(env.now);
        params.setInitializer(initializer);
        params.setProcessor(processor);
        params.setAPrioriTargetObject(newUser);
        params.setAPrioriTargetDelta(userPrimaryDelta);
        params.setTargetContext(context.getFocusContext());
        params.setDefaultTargetItemPath(targetPath);
        params.setEvaluateCurrent(MappingTimeEval.CURRENT);
        params.setContext(context);
        params.setHasFullTargetObject(true);
        beans.mappingEvaluator.evaluateMappingSetProjection(params, env.task, result);
    }

    private PrismContainerDefinition<ResourceObjectAssociationType> getAssociationContainerDefinition() {
        if (associationContainerDefinition == null) {
            associationContainerDefinition = beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class).findContainerDefinition(ShadowType.F_ASSOCIATION);
        }
        return associationContainerDefinition;
    }

    static class InboundMappingStruct<V extends PrismValue, D extends ItemDefinition> {
        private final MappingImpl<V,D> mapping;
        private final LensProjectionContext projectionContext;

        private InboundMappingStruct(MappingImpl<V, D> mapping, LensProjectionContext projectionContext) {
            this.mapping = mapping;
            this.projectionContext = projectionContext;
        }

        public MappingImpl<V,D> getMapping() {
            return mapping;
        }

        public LensProjectionContext getProjectionContext() {
            return projectionContext;
        }
    }

    private PrismObject<F> getCurrentFocus() {
        if (context.getFocusContext().getObjectCurrent() != null) {
            return context.getFocusContext().getObjectCurrent();
        } else {
            return context.getFocusContext().getObjectNew();
        }
    }
}
