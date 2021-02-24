/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluatorParams;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingInitializer;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingOutputProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingTimeEval;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ConfigurableValuePolicySupplier;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.VariableProducer;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
    private final PathKeyedMap<List<InboundMappingStruct<?, ?>>> mappingsMap = new PathKeyedMap<>();

    /**
     * Output triples for individual target paths.
     */
    private final PathKeyedMap<DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap = new PathKeyedMap<>();

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
            CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException {
        collectMappings();
        evaluateMappings();
        consolidateTriples();
    }

    //region Collecting mappings (evaluation in some cases)

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

            new ProjectionMappingsCollector(projectionContext).collectOrEvaluate();
        }
    }

    /**
     * Collects inbound mappings from specified projection context.
     *
     * (Special mappings i.e. password and activation ones, are evaluated immediately. This is to be revised.)
     */
    private class ProjectionMappingsCollector {

        private final LensProjectionContext projectionContext;

        private PrismObject<ShadowType> currentProjection;

        private final ObjectDelta<ShadowType> aPrioriDelta;

        private final RefinedObjectClassDefinition projectionDefinition;

        private boolean stop;

        private ProjectionMappingsCollector(LensProjectionContext projectionContext) throws SchemaException {
            this.projectionContext = projectionContext;
            this.currentProjection = projectionContext.getObjectCurrent();
            this.aPrioriDelta = getAPrioriDelta(projectionContext);
            this.projectionDefinition = projectionContext.getCompositeObjectClassDefinition();
        }

        private void collectOrEvaluate() throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

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

            collectOrEvaluateProjectionMappings();
        }

        private void checkObjectClassDefinitionPresent() {
            if (projectionDefinition == null) {
                LOGGER.error("Definition for projection {} not found in the context, but it " +
                        "should be there, dumping context:\n{}", projectionContext.getHumanReadableName(), context.debugDump());
                throw new IllegalStateException("Definition for projection " + projectionContext.getHumanReadableName()
                        + " not found in the context, but it should be there");
            }
        }

        private void collectOrEvaluateProjectionMappings()
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
                // TODO why we are skipping evaluation of these special properties?
                LOGGER.trace("Skipping application of special properties because of projection DELETE delta");
                return;
            }

            evaluateSpecialInbounds(projectionDefinition.getPasswordInbound(),
                    SchemaConstants.PATH_PASSWORD_VALUE, SchemaConstants.PATH_PASSWORD_VALUE);
            evaluateSpecialInbounds(getActivationInbound(ActivationType.F_ADMINISTRATIVE_STATUS),
                    SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
            evaluateSpecialInbounds(getActivationInbound(ActivationType.F_VALID_FROM),
                    SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM);
            evaluateSpecialInbounds(getActivationInbound(ActivationType.F_VALID_TO),
                    SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO);

            collectAuxiliaryObjectClassInbounds();
        }

        private List<MappingType> getActivationInbound(ItemName itemName) {
            ResourceBidirectionalMappingType biDirectionalMapping = projectionDefinition.getActivationBidirectionalMappingType(itemName);
            return biDirectionalMapping != null ? biDirectionalMapping.getInbound() : Collections.emptyList();
        }

        private boolean isDeleteProjectionDelta() {
            return ObjectDelta.isDelete(projectionContext.getSyncDelta()) || ObjectDelta.isDelete(projectionContext.getPrimaryDelta());
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

            RefinedAttributeDefinition<?> attributeDef = findAttributeDefinition(attributeName);

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
                } else if (currentProjection != null) {
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
                LOGGER.trace("Collecting attribute inbound mapping for {}. Relevant values are:\n"
                                + "- a priori delta:\n{}\n"
                                + "- a priori attribute delta:\n{}\n"
                                + "- current attribute:\n{}", attributeName,
                        DebugUtil.debugDumpLazily(aPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(attributeAPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(currentAttribute, 1));

                //noinspection unchecked
                collectMapping(inboundMappingBean, attributeName, currentAttribute, attributeAPrioriDelta, (D) attributeDef, null);
            }
        }

        @NotNull
        private RefinedAttributeDefinition<Object> findAttributeDefinition(QName attributeName) {
            return java.util.Objects.requireNonNull(
                    projectionDefinition.findAttributeDefinition(attributeName),
                    () -> "No definition for attribute " + attributeName);
        }

        @NotNull
        private RefinedAssociationDefinition findAssociationDefinition(QName associationName) {
            return java.util.Objects.requireNonNull(
                    projectionDefinition.findAssociationDefinition(associationName),
                    () -> "No definition for association " + associationName);
        }

        @Nullable
        private PrismProperty getCurrentAttribute(QName attributeName) {
            if (currentProjection != null) {
                return currentProjection.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attributeName));
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

            RefinedAssociationDefinition associationDef = findAssociationDefinition(associationName);

            if (associationDef.isIgnored(LayerType.MODEL)) {
                LOGGER.trace("Skipping inbounds for association {} in {} because the association is ignored",
                        associationName, projectionContext.getResourceShadowDiscriminator());
                return;
            }

            List<MappingType> inboundMappingBeans = associationDef.getInboundMappingTypes();
            LOGGER.trace("Processing inbounds for {} in {}; ({} mappings)", lazy(() -> PrettyPrinter.prettyPrint(associationName)),
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
                } else if (currentProjection != null) {
                    loadRequired();
                    if (shouldStop()) {
                        return;
                    }
                    LOGGER.trace("Processing inbound from account sync absolute state");

                    PrismContainer<ShadowAssociationType> currentAssociation = currentProjection.findContainer(ShadowType.F_ASSOCIATION);
                    if (currentAssociation == null) {
                        // Todo is this OK?
                        LOGGER.trace("No shadow association value");
                        return;
                    }
                } else {
                    LOGGER.trace("Both association apriori delta and current projection is null. Not collecting the mapping.");
                    return;
                }

                PrismContainer<ShadowAssociationType> filteredAssociations = getFilteredAssociations(associationName);
                resolveEntitlementsIfNeeded((ContainerDelta<ShadowAssociationType>) associationAPrioriDelta, filteredAssociations);

                LOGGER.trace("Collecting association inbound mapping for {}. Relevant values are:\n"
                                + "- a priori delta:\n{}\n"
                                + "- association a priori delta:\n{}\n"
                                + "- current state (filtered associations):\n{}", associationName,
                        DebugUtil.debugDumpLazily(aPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(associationAPrioriDelta, 1),
                        DebugUtil.debugDumpLazily(filteredAssociations, 1));

                PrismContainerDefinition<ResourceObjectAssociationType> associationContainerDef = getAssociationContainerDefinition();
                //noinspection unchecked
                collectMapping(inboundMappingBean, associationName, (Item<V, D>) filteredAssociations,
                        associationAPrioriDelta, (D) associationContainerDef,
                        (VariableProducer<V>) (VariableProducer<PrismContainerValue<ShadowAssociationType>>) this::resolveEntitlementFromMap);
            }
        }

        private PrismContainerDefinition<ResourceObjectAssociationType> getAssociationContainerDefinition() {
            if (associationContainerDefinition == null) {
                associationContainerDefinition = beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class).findContainerDefinition(ShadowType.F_ASSOCIATION);
            }
            return associationContainerDefinition;
        }

        private void resolveEntitlementsIfNeeded(ContainerDelta<ShadowAssociationType> associationAPrioriDelta,
                PrismContainer<ShadowAssociationType> currentAssociation) {
            Collection<PrismContainerValue<ShadowAssociationType>> associationsToResolve = new ArrayList<>();
            if (currentAssociation != null) {
                associationsToResolve.addAll(currentAssociation.getValues());
            }
            if (associationAPrioriDelta != null) {
                // TODO Shouldn't we filter also these?
                associationsToResolve.addAll(associationAPrioriDelta.getValues(ShadowAssociationType.class));
            }

            for (PrismContainerValue<ShadowAssociationType> associationToResolve : associationsToResolve) {
                PrismReference shadowRef = associationToResolve.findReference(ShadowAssociationType.F_SHADOW_REF);
                if (shadowRef != null) {
                    resolveEntitlementFromResource(shadowRef);
                }
            }
        }

        private void resolveEntitlementFromResource(PrismReference shadowRef) {
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
                }
            }
        }

        @Nullable
        private PrismContainer<ShadowAssociationType> getFilteredAssociations(QName associationName)
                throws SchemaException {
            LOGGER.trace("Getting filtered associations for {}", associationName);
            PrismContainer<ShadowAssociationType> currentAssociation = currentProjection != null ?
                    currentProjection.findContainer(ShadowType.F_ASSOCIATION) : null;

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

        private void resolveEntitlementFromMap(PrismContainerValue<ShadowAssociationType> value, VariablesMap variables) {
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
                PrismProperty<QName> oldAccountProperty = currentProjection.findProperty(ShadowType.F_AUXILIARY_OBJECT_CLASS);
                LOGGER.trace("Processing inbound from account sync absolute state (currentAccount): {}", oldAccountProperty);
                // TODO why we ignore auxiliary object class apriori delta?
                collectMapping(inboundMappingBean, ShadowType.F_AUXILIARY_OBJECT_CLASS,
                        oldAccountProperty, null, auxiliaryObjectClassPropertyDefinition,
                        null);
            }
        }

        /**
         * Processing for special (fixed-schema) properties such as credentials and activation.
         *
         * The code is rather strange. TODO revisit and clean up
         *
         * Also it is not clear why these mappings are not collected to the map for later execution,
         * just like regular mappings are.
         */
        private void evaluateSpecialInbounds(Collection<MappingType> inboundMappingBeans,
                ItemPath sourcePath, ItemPath targetPath) throws SchemaException,
                ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
                SecurityViolationException {

            if (inboundMappingBeans == null || inboundMappingBeans.isEmpty()) {
                return;
            }

            LOGGER.trace("Collecting {} inbounds for special property {}", inboundMappingBeans.size(), sourcePath);

            PrismObject<F> focus = getCurrentFocus(); // TODO check if we should really use current object here
            if (focus == null) {
                LOGGER.trace("No current/new focus, skipping.");
                return;
            }
            if (!projectionContext.isFullShadow()) {
                // TODO - is this ok?
                LOGGER.trace("Full shadow not loaded, skipping.");
                return;
            }

            ObjectDelta<F> userPrimaryDelta = context.getFocusContext().getPrimaryDelta();
            if (userPrimaryDelta != null) {
                PropertyDelta primaryPropDelta = userPrimaryDelta.findPropertyDelta(targetPath);
                if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
                    LOGGER.trace("Primary delta of 'replace' overrides any inbounds, skipping. Delta: {}", primaryPropDelta);
                    return;
                }
            }

            // TODO
//            ObjectDelta<F> userSecondaryDelta = context.getFocusContext().getProjectionWaveSecondaryDelta();
//            if (userSecondaryDelta != null) {
//                PropertyDelta<?> secondaryPropDelta = userSecondaryDelta.findPropertyDelta(targetPath);
//                if (secondaryPropDelta != null) {
//                    LOGGER.trace("There is a secondary delta in the current wave. Removing it. New value will be handled by inbounds: {}", secondaryPropDelta);
//                    userSecondaryDelta.getModifications().remove(secondaryPropDelta);
//                }
//            }

            MappingInitializer<PrismValue, ItemDefinition> initializer =
                    (builder) -> {
                        if (projectionContext.getObjectNew() == null) {
                            projectionContext.recompute();
                            if (projectionContext.getObjectNew() == null) {
                                // Still null? something must be really wrong here.
                                String message = "Recomputing account " + projectionContext.getResourceShadowDiscriminator()
                                        + " results in null new account. Something must be really broken.";
                                LOGGER.error(message);
                                LOGGER.trace("Account context:\n{}", projectionContext.debugDumpLazily());
                                throw new SystemException(message);
                            }
                        }

                        ItemDelta<PrismPropertyValue<?>, PrismPropertyDefinition<?>> specialAttributeDelta;
                        if (aPrioriDelta != null) {
                            specialAttributeDelta = aPrioriDelta.findItemDelta(sourcePath);
                        } else {
                            specialAttributeDelta = null;
                        }
                        ItemDeltaItem<PrismPropertyValue<?>, PrismPropertyDefinition<?>> sourceIdi = projectionContext.getObjectDeltaObject().findIdi(sourcePath);
                        if (specialAttributeDelta == null) {
                            specialAttributeDelta = sourceIdi.getDelta();
                        }
                        Source<PrismPropertyValue<?>, PrismPropertyDefinition<?>> source = new Source<>(
                                sourceIdi.getItemOld(), specialAttributeDelta, sourceIdi.getItemOld(),
                                ExpressionConstants.VAR_INPUT_QNAME,
                                sourceIdi.getDefinition());
                        builder.defaultSource(source)
                                .addVariableDefinition(ExpressionConstants.VAR_USER, focus, UserType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus, FocusType.class)
                                .addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);

                        PrismObject<ShadowType> accountNew = projectionContext.getObjectNew();
                        builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountNew, ShadowType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_SHADOW, accountNew, ShadowType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, accountNew, ShadowType.class)
                                .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                                .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                                .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projectionContext.getResource(), ResourceType.class)
                                .valuePolicySupplier(createValuePolicySupplier())
                                .mappingKind(MappingKindType.INBOUND)
                                .implicitSourcePath(sourcePath)
                                .implicitTargetPath(targetPath)
                                .originType(OriginType.INBOUND)
                                .originObject(projectionContext.getResource());

                        return builder;
                    };

            MappingOutputProcessor<PrismValue> processor =
                    (mappingOutputPath, outputStruct) -> {
                        PrismValueDeltaSetTriple<PrismValue> outputTriple = outputStruct.getOutputTriple();
                        if (outputTriple == null) {
                            LOGGER.trace("Mapping for property {} evaluated to null. Skipping inbound processing for that property.", sourcePath);
                            return false;
                        }

                        // TODO
//                        ObjectDelta<F> userSecondaryDeltaInt = context.getFocusContext().getProjectionWaveSecondaryDelta();
//                        if (userSecondaryDeltaInt != null) {
//                            PropertyDelta<?> delta = userSecondaryDeltaInt.findPropertyDelta(targetPath);
//                            if (delta != null) {
//                                //remove delta if exists, it will be handled by inbound
//                                userSecondaryDeltaInt.getModifications().remove(delta);
//                            }
//                        }

                        PrismObjectDefinition<F> focusDefinition = context.getFocusContext().getObjectDefinition();
                        PrismProperty result = focusDefinition.findPropertyDefinition(targetPath).instantiate();
                        //noinspection unchecked
                        result.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));

                        PrismProperty targetPropertyNew = focus.findOrCreateProperty(targetPath);
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
                                    //noinspection unchecked
                                    delta = targetPropertyNew.diff(result);
                                }
                            } catch (EncryptionException e) {
                                throw new SystemException(e.getMessage(), e);
                            }
                        } else {
                            //noinspection unchecked
                            delta = targetPropertyNew.diff(result);
                        }
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("targetPropertyNew:\n{}\ndelta:\n{}", targetPropertyNew.debugDump(1), DebugUtil.debugDump(delta, 1));
                        }
                        if (delta != null && !delta.isEmpty()) {
                            delta.setParentPath(targetPath.allExceptLast());
                            context.getFocusContext().swallowToSecondaryDelta(delta);
                        }
                        return false;
                    };

            MappingEvaluatorParams<PrismValue, ItemDefinition, F, F> params = new MappingEvaluatorParams<>();
            params.setMappingTypes(inboundMappingBeans);
            params.setMappingDesc("inbound mapping for " + sourcePath + " in " + projectionContext.getResource());
            params.setNow(env.now);
            params.setInitializer(initializer);
            params.setProcessor(processor);
            params.setAPrioriTargetObject(focus);
            params.setAPrioriTargetDelta(userPrimaryDelta);
            params.setTargetContext(context.getFocusContext());
            params.setDefaultTargetItemPath(targetPath);
            params.setEvaluateCurrent(MappingTimeEval.CURRENT);
            params.setContext(context);
            params.setHasFullTargetObject(true);
            beans.mappingEvaluator.evaluateMappingSetProjection(params, env.task, result);
        }

        private <V extends PrismValue, D extends ItemDefinition> void collectMapping(MappingType inboundMappingBean,
                QName projectionItemName, Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta,
                D itemDefinition, VariableProducer<V> variableProducer) throws ObjectNotFoundException, SchemaException,
                ConfigurationException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

            if (currentProjectionItem != null && currentProjectionItem.hasRaw()) {
                throw new SystemException("Property " + currentProjectionItem + " has raw parsing state, such property cannot be used in inbound expressions");
            }

            ResourceType resource = projectionContext.getResource();

            if (!MappingImpl.isApplicableToChannel(inboundMappingBean, context.getChannel())) {
                LOGGER.trace("Mapping is not applicable to channel {}", context.getChannel());
                return;
            }

            PrismObject<ShadowType> shadowNew = projectionContext.getObjectNew();
            PrismObjectDefinition<ShadowType> shadowNewDef = getShadowDefinition(shadowNew);

            PrismObject<F> focus = getCurrentFocus(); // TODO check if we should really use current object here
            PrismObjectDefinition<F> focusDef = getFocusDefinition(focus);

            if (currentProjectionItem != null) {
                beans.projectionValueMetadataCreator.setValueMetadata(currentProjectionItem, projectionContext, env, result);
            }
            if (itemAPrioriDelta != null) {
                beans.projectionValueMetadataCreator.setValueMetadata(itemAPrioriDelta, projectionContext, env, result);
            }

            Source<V, D> defaultSource = new Source<>(currentProjectionItem, itemAPrioriDelta, null, ExpressionConstants.VAR_INPUT_QNAME, itemDefinition);
            defaultSource.recompute();

            MappingBuilder<V, D> builder = beans.mappingFactory.<V, D>createMappingBuilder()
                    .mappingBean(inboundMappingBean)
                    .mappingKind(MappingKindType.INBOUND)
                    .implicitSourcePath(ShadowType.F_ATTRIBUTES.append(projectionItemName))
                    .contextDescription("inbound expression for " + projectionItemName + " in " + resource)
                    .defaultSource(defaultSource)
                    .targetContext(LensUtil.getFocusDefinition(context))
                    .addVariableDefinition(ExpressionConstants.VAR_USER, focus, focusDef)
                    .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus, focusDef)
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
                    .valuePolicySupplier(createValuePolicySupplier())
                    .originType(OriginType.INBOUND)
                    .originObject(resource)
                    .now(env.now);

            if (!context.getFocusContext().isDelete()) {
                assert focus != null;
                TypedValue<PrismObject<F>> targetContext = new TypedValue<>(focus);
                VariablesMap variables = builder.getVariables();
                Collection<V> originalValues = ExpressionUtil.computeTargetValues(inboundMappingBean.getTarget(), targetContext,
                        variables, beans.mappingFactory.getObjectResolver(), "resolving target values",
                        beans.prismContext, env.task, result);
                builder.originalTargetValues(originalValues);
            }

            MappingImpl<V, D> mapping = builder.build();

            if (checkWeakSkip(mapping, focus)) {
                LOGGER.trace("Skipping because of mapping is weak and focus property has already a value");
                return;
            }

            InboundMappingStruct<V, D> mappingStruct = new InboundMappingStruct<>(mapping, projectionContext);

            ItemPath targetFocusItemPath = mapping.getOutputPath();
            if (ItemPath.isEmpty(targetFocusItemPath)) {
                throw new ConfigurationException("Empty target path in " + mapping.getContextDescription());
            }
            ItemDefinition targetItemDef = focusDef.findItemDefinition(targetFocusItemPath);
            if (targetItemDef == null) {
                throw new SchemaException("No definition for focus property " + targetFocusItemPath + ", cannot process inbound expression in " + resource);
            }

            addToMappingsMap(targetFocusItemPath, mappingStruct);
        }

        private void addToMappingsMap(ItemPath itemPath, InboundMappingStruct<?, ?> mappingStruct) {
            mappingsMap
                    .computeIfAbsent(itemPath, k -> new ArrayList<>())
                    .add(mappingStruct);
        }

        private PrismObjectDefinition<ShadowType> getShadowDefinition(PrismObject<ShadowType> shadowNew) {
            if (shadowNew != null && shadowNew.getDefinition() != null) {
                return shadowNew.getDefinition();
            } else {
                return beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
            }
        }

        private void loadIfAnyStrong() throws SchemaException {
            if (!projectionContext.isFullShadow() && !projectionContext.isTombstone() && hasAnyStrongMapping()) {
                LOGGER.trace("There are strong inbound mappings, but the shadow hasn't be fully loaded yet. Trying to load full shadow now.");
                doLoad();
            }
        }

        private void loadIfStrong(MappingType inboundMappingBean) throws SchemaException {
            if (!projectionContext.isFullShadow() && !projectionContext.isDelete() && inboundMappingBean.getStrength() == MappingStrengthType.STRONG) {
                LOGGER.trace("There is an inbound mapping with strength == STRONG. Trying to load full account now.");
                doLoad();
            }
        }

        /**
         * This is the "ultimate load". If not successful we stop collecting from this projection context.
         */
        private void loadRequired() throws SchemaException {
            if (!projectionContext.isFullShadow()) {
                // todo change to trace level eventually
                LOGGER.warn("Attempted to execute inbound expression on account shadow {} WITHOUT full account. Trying to load the account now.", projectionContext.getOid());
                doLoad();
                if (!isBroken() && !projectionContext.isFullShadow()) {
                    if (projectionContext.getResourceShadowDiscriminator().getOrder() > 0) {
                        // higher-order context. It is OK not to load this
                        LOGGER.trace("Skipped load of higher-order account with shadow OID {} skipping inbound processing on it", projectionContext.getOid());
                    }
                    stop = true;
                }
            }
        }

        private void doLoad() throws SchemaException {
            try {
                beans.contextLoader.loadFullShadow(context, projectionContext, "inbound", env.task, result);
                currentProjection = projectionContext.getObjectCurrent();
            } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                LOGGER.warn("Couldn't load account with shadow OID {} because of {}, setting context as broken and skipping inbound processing on it", projectionContext.getOid(), e.getMessage());
                projectionContext.setSynchronizationPolicyDecision(SynchronizationPolicyDecision.BROKEN);
            }
        }

        private boolean hasAnyStrongMapping() {
            for (QName attributeName : projectionDefinition.getNamesOfAttributesWithInboundExpressions()) {
                RefinedAttributeDefinition<?> definition = findAttributeDefinition(attributeName);
                for (MappingType inboundMapping : definition.getInboundMappingTypes()) {
                    if (inboundMapping.getStrength() == MappingStrengthType.STRONG) {
                        return true;
                    }
                }
            }
            for (QName associationName : projectionDefinition.getNamesOfAssociationsWithInboundExpressions()) {
                RefinedAssociationDefinition definition = findAssociationDefinition(associationName);
                for (MappingType inboundMapping : definition.getInboundMappingTypes()) {
                    if (inboundMapping.getStrength() == MappingStrengthType.STRONG) {
                        return true;
                    }
                }
            }
            return false;
        }
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
                return executed.get(executed.size() - 1).getObjectDelta();
            } else {
                return accountContext.getSummaryDelta(); // TODO check this
            }
        }
        return null;
    }

    private boolean checkWeakSkip(MappingImpl<?, ?> inbound, PrismObject<F> focus) {
        if (inbound.getStrength() != MappingStrengthType.WEAK) {
            return false;
        }
        if (focus != null) {
            Item<?, ?> item = focus.findItem(inbound.getOutputPath());
            return item != null && !item.isEmpty();
        } else {
            return false;
        }
    }
    //endregion

    //region Evaluating collected mappings

    /**
     * Evaluate mappings consolidated from all the projections. There may be mappings from different projections to the same target.
     * We want to merge their values. Otherwise those mappings will overwrite each other.
     */
    private void evaluateMappings() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, CommunicationException {
        for (Map.Entry<ItemPath, List<InboundMappingStruct<?, ?>>> entry : mappingsMap.entrySet()) {
            evaluateMappingsForTargetItem(entry.getKey(), entry.getValue());
        }
    }

    private void evaluateMappingsForTargetItem(ItemPath targetPath, List<InboundMappingStruct<?, ?>> mappingStructList)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        assert !mappingStructList.isEmpty();
        for (InboundMappingStruct<?, ?> mappingStruct : mappingStructList) {
            MappingImpl<?, ?> mapping = mappingStruct.getMapping();
            LensProjectionContext projectionCtx = mappingStruct.getProjectionContext();

            beans.mappingEvaluator.evaluateMapping(mapping, context, projectionCtx, env.task, result);
            mergeMappingOutput(mapping, targetPath, projectionCtx.isDelete());
        }
    }

    private <V extends PrismValue, D extends ItemDefinition> void mergeMappingOutput(MappingImpl<V, D> mapping,
            ItemPath targetPath, boolean allToDelete) {

        DeltaSetTriple<ItemValueWithOrigin<V, D>> ivwoTriple = ItemValueWithOrigin.createOutputTriple(mapping, beans.prismContext);
        LOGGER.trace("Inbound mapping for {}\nreturned triple:\n{}",
                DebugUtil.shortDumpLazily(mapping.getDefaultSource()), DebugUtil.debugDumpLazily(ivwoTriple, 1));

        if (ivwoTriple != null) {
            if (allToDelete) {
                LOGGER.trace("Projection is going to be deleted, setting values from this projection to minus set");
                DeltaSetTriple<ItemValueWithOrigin<V, D>> convertedTriple = beans.prismContext.deltaFactory().createDeltaSetTriple();
                convertedTriple.addAllToMinusSet(ivwoTriple.getPlusSet());
                convertedTriple.addAllToMinusSet(ivwoTriple.getZeroSet());
                convertedTriple.addAllToMinusSet(ivwoTriple.getMinusSet());
                //noinspection unchecked
                DeltaSetTripleUtil.putIntoOutputTripleMap((PathKeyedMap) outputTripleMap, targetPath, convertedTriple);
            } else {
                //noinspection unchecked
                DeltaSetTripleUtil.putIntoOutputTripleMap((PathKeyedMap) outputTripleMap, targetPath, ivwoTriple);
            }
        }
    }
    //endregion

    //region Consolidating triples to deltas

    private void consolidateTriples() throws CommunicationException, ObjectNotFoundException, ConfigurationException,
            SchemaException, SecurityViolationException, ExpressionEvaluationException {

        PrismObject<F> focusNew = context.getFocusContext().getObjectNew();
        PrismObjectDefinition<F> focusDefinition = getFocusDefinition(focusNew);
        LensFocusContext<F> focusContext = context.getFocusContextRequired();
        ObjectDelta<F> focusAPrioriDelta = focusContext.getCurrentDelta();

        Consumer<IvwoConsolidatorBuilder> customizer = builder ->
                builder
                        .deleteExistingValues(builder.getItemDefinition().isSingleValue() &&
                                !rangeIsCompletelyDefined(builder.getItemPath()))
                        .skipNormalMappingAPrioriDeltaCheck(true);

        DeltaSetTripleMapConsolidation<F> consolidation = new DeltaSetTripleMapConsolidation<>(
                outputTripleMap, focusNew, focusAPrioriDelta, context::primaryFocusItemDeltaExists,
                true, customizer, focusDefinition,
                env, beans, context, result);
        consolidation.computeItemDeltas();

        focusContext.swallowToSecondaryDelta(consolidation.getItemDeltas());
    }

    private boolean rangeIsCompletelyDefined(ItemPath itemPath) {
        return mappingsMap.get(itemPath).stream()
                .allMatch(m -> m.getMapping().hasTargetRange());
    }

    private ConfigurableValuePolicySupplier createValuePolicySupplier() {
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
    //endregion

    //region Miscellaneous

    // TODO Check relevance of this method. Should we really use current object?
    //  Fortunately, for inbounds processing it seems that current is usually the same as new,
    //  because there are no secondary deltas yet. But are we sure?
    private PrismObject<F> getCurrentFocus() {
        if (context.getFocusContext().getObjectCurrent() != null) {
            return context.getFocusContext().getObjectCurrent();
        } else {
            return context.getFocusContext().getObjectNew();
        }
    }

    private PrismObjectDefinition<F> getFocusDefinition(PrismObject<F> focus) {
        if (focus != null && focus.getDefinition() != null) {
            return focus.getDefinition();
        } else {
            return context.getFocusContextRequired().getObjectDefinition();
        }
    }

    static class InboundMappingStruct<V extends PrismValue, D extends ItemDefinition> {
        private final MappingImpl<V, D> mapping;
        private final LensProjectionContext projectionContext;

        private InboundMappingStruct(MappingImpl<V, D> mapping, LensProjectionContext projectionContext) {
            this.mapping = mapping;
            this.projectionContext = projectionContext;
        }

        public MappingImpl<V, D> getMapping() {
            return mapping;
        }

        public LensProjectionContext getProjectionContext() {
            return projectionContext;
        }
    }
    //endregion
}
