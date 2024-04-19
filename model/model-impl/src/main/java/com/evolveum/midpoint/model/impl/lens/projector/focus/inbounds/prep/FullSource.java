/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.config.AbstractMappingConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 * Inbound mapping source ({@link MSource}) that is used in clockwork-based inbound mapping evaluation.
 * This is the standard situation. The other one is e.g. pre-inbounds (correlation-time) evaluation.
 */
class FullSource extends MSource {

    private static final Trace LOGGER = TraceManager.getTrace(FullSource.class);

    private static final String OP_RESOLVE_ENTITLEMENT = FullSource.class.getName() + ".resolveEntitlement";

    @NotNull private final LensProjectionContext projectionContext;

    @NotNull private final ModelBeans beans = ModelBeans.get();

    @NotNull private final Context context;

    @NotNull private final IdentityManagementConfiguration identityManagementConfiguration;

    FullSource(
            PrismObject<ShadowType> currentShadow,
            @Nullable ObjectDelta<ShadowType> aPrioriDelta,
            @NotNull ResourceObjectDefinition resourceObjectDefinition,
            @NotNull ResourceObjectInboundDefinition inboundDefinition,
            @NotNull LensProjectionContext projectionContext,
            @NotNull Context context,
            @Nullable ShadowAssociationDefinition owningAssociationDefinition) throws ConfigurationException {
        super(
                asObjectable(currentShadow),
                aPrioriDelta,
                resourceObjectDefinition,
                inboundDefinition,
                owningAssociationDefinition);
        this.projectionContext = projectionContext;
        this.context = context;
        this.identityManagementConfiguration = getFocusContext().getIdentityManagementConfiguration();
    }

    @Override
    protected String getProjectionHumanReadableName() {
        return projectionContext.getHumanReadableName();
    }

    @Override
    boolean isClockwork() {
        return true;
    }

    @Override
    @NotNull ResourceType getResource() {
        return projectionContext.getResourceRequired();
    }

    @Override
    Object getContextDump() {
        return projectionContext.getLensContext().debugDumpLazily();
    }

    @Override
    boolean isEligibleForInboundProcessing(OperationResult result) throws SchemaException, ConfigurationException {
        LOGGER.trace("Starting determination if we should process inbound mappings. Full shadow: {}. A priori delta present: {}.",
                projectionContext.isFullShadow(), aPrioriDelta != null);

        if (projectionContext.isInboundSyncDisabled(result)) {
            LOGGER.trace("Skipping processing of inbound mappings because shadow policy marked shadow inbound disabled.");
            return false;
        }

        if (projectionContext.isBroken()) {
            LOGGER.trace("Skipping processing of inbound mappings because the context is broken");
            return false;
        }
        if (aPrioriDelta != null) {
            LOGGER.trace("A priori delta present, we'll do the inbound processing");
            return true;
        }
        if (projectionContext.getObjectCurrent() == null) {
            LOGGER.trace("No current projection object and no apriori delta: skipping the inbounds (there's nothing to process)");
            return false;
        }
        if (projectionContext.isFullShadow()) {
            LOGGER.trace("Full shadow is present, we'll do the inbound processing (it should be cheap)");
            return true;
        }
        if (projectionContext.isDoReconciliation()) {
            LOGGER.trace("We'll do the inbounds even we have no apriori delta nor full shadow, because the"
                    + " projection reconciliation is requested");
            return true;
        }
        if (projectionContext.hasDependentContext()) {
            LOGGER.trace("We'll do the inbounds even we have no apriori delta nor full shadow, because the"
                    + " projection has a dependent projection context");
            return true;
        }
        if (projectionContext.isDelete()) {
            // TODO what's the exact reason for this behavior?
            LOGGER.trace("We'll do the inbounds even we have no apriori delta nor full shadow, because the"
                    + " projection is being deleted");
            return true;
        }
        LOGGER.trace("Skipping processing of inbound mappings: no a priori delta, no full shadow,"
                        + " no reconciliation, no dependent context, and it's not a delete operation:\n{}",
                projectionContext.debugDumpLazily());
        return false;
    }

    @Override
    boolean isProjectionBeingDeleted() {
        return ObjectDelta.isDelete(projectionContext.getSyncDelta())
                || ObjectDelta.isDelete(projectionContext.getPrimaryDelta());
    }

    @Override
    boolean isAbsoluteStateAvailable() {
        return projectionContext.isFullShadow();
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> void setValueMetadata(
            Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (currentProjectionItem != null) {
            LOGGER.trace("Setting value metadata for current projection item");
            beans.projectionValueMetadataCreator.setValueMetadata(
                    currentProjectionItem, projectionContext, context.env, result);
        }
        if (itemAPrioriDelta != null) {
            LOGGER.trace("Setting value metadata for item a priori delta");
            beans.projectionValueMetadataCreator.setValueMetadata(
                    itemAPrioriDelta, projectionContext, context.env, result);
        }
    }

    @Override
    PrismObject<ShadowType> getResourceObjectNew() {
        return currentShadow;
    }

    @Override
    String getChannel() {
        return projectionContext.getLensContext().getChannel();
    }

    @Override
    @NotNull ProcessingMode getItemProcessingMode(
            String itemDescription,
            ItemDelta<?, ?> itemAPrioriDelta,
            List<? extends AbstractMappingConfigItem<?>> mappings,
            boolean executionModeVisible,
            boolean ignored,
            PropertyLimitations limitations) throws SchemaException, ConfigurationException {

        if (shouldBeMappingSkipped(itemDescription, executionModeVisible, ignored, limitations)) {
            return ProcessingMode.NONE;
        }

        if (itemAPrioriDelta != null) {
            LOGGER.trace("Mapping(s) for {}: Item a priori delta exists, we'll use it for the evaluation", itemDescription);
            return ProcessingMode.A_PRIORI_DELTA;
        }

        if (currentShadow == null) {
            // We have no chance of loading the shadow - we have no information about it.
            // Actually, this shouldn't occur (see shouldProcessMappings).
            LOGGER.trace("Mapping(s) for {}: No item a priori delta, and no shadow (not even repo version) -> skipping them",
                    itemDescription);
            return ProcessingMode.NONE;
        }

        if (projectionContext.isFullShadow()) {
            LOGGER.trace("Mapping(s) for {}: No item a priori delta present, but we have the full shadow."
                    + " We'll use it for the evaluation.", itemDescription);
            return ProcessingMode.ABSOLUTE_STATE;
        }

        if (projectionContext.hasDependentContext()) {
            LOGGER.trace("Mapping(s) for {}: A dependent context is present. We'll load the shadow.", itemDescription);
            return ProcessingMode.ABSOLUTE_STATE;
        }

        if (mappings.stream().anyMatch(mapping -> mapping.isStrong())) {
            LOGGER.trace("Mapping(s) for {}: A strong mapping is present. We'll load the shadow.", itemDescription);
            return ProcessingMode.ABSOLUTE_STATE;
        }

        LOGGER.trace("Mapping(s) for {}: There is no special reason for loading the shadow. We'll apply them if the shadow"
                + " is loaded for another reason.", itemDescription);
        return ProcessingMode.ABSOLUTE_STATE_IF_KNOWN;
    }

    @Override
    void loadFullShadowIfNeeded(boolean fullStateRequired, @NotNull Context context, OperationResult result)
            throws SchemaException, StopProcessingProjectionException {
        if (projectionContext.isFullShadow()) {
            return;
        }
        if (projectionContext.isGone()) {
            LOGGER.trace("Not loading {} because the resource object is gone", getProjectionHumanReadableNameLazy());
        }

        if (fullStateRequired) {
            LOGGER.trace("Loading {} because full state is required", getProjectionHumanReadableNameLazy());
            doLoad(context, result);
        }
    }

    private void doLoad(@NotNull Context context, OperationResult result)
            throws SchemaException, StopProcessingProjectionException {
        try {
            beans.contextLoader.loadFullShadow(projectionContext, "inbound", context.env.task, result);
            currentShadow = projectionContext.getObjectCurrent();
            if (projectionContext.isBroken()) { // just in case the load does not return an exception
                throw new StopProcessingProjectionException();
            }
            if (!projectionContext.isFullShadow()) {
                LOGGER.trace("Projection {} couldn't be loaded - it is not a full shadow even after load operation",
                        projectionContext);
                if (aPrioriDelta != null) {
                    LOGGER.trace("There's a priori delta. We'll try to process inbounds in relative mode.");
                } else {
                    LOGGER.trace("There's no a priori delta. We stop processing the inbounds for this projection.");
                    throw new StopProcessingProjectionException();
                }
            }
        } catch (ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException |
                ExpressionEvaluationException e) {
            LOGGER.warn("Couldn't load account with shadow OID {} because of {}, setting context as broken and"
                    + " skipping inbound processing on it", projectionContext.getOid(), e.getMessage());
            projectionContext.setBroken();
            throw new StopProcessingProjectionException();
        }
    }

    @Override
    void resolveInputEntitlements(
            ContainerDelta<ShadowAssociationValueType> associationAPrioriDelta,
            ShadowAssociation currentAssociation) {

        // FIXME rework this!

        Collection<PrismContainerValue<ShadowAssociationValueType>> associationsToResolve = new ArrayList<>();
        if (currentAssociation != null) {
            associationsToResolve.addAll(currentAssociation.getValues());
        }
        if (associationAPrioriDelta != null) {
            // TODO Shouldn't we filter also these?
            associationsToResolve.addAll(
                    associationAPrioriDelta.getValues(ShadowAssociationValueType.class));
        }

        for (PrismContainerValue<ShadowAssociationValueType> associationToResolve : associationsToResolve) {
            resolveEntitlementFromResource(associationToResolve);
        }
    }

    private void resolveEntitlementFromResource(PrismContainerValue<ShadowAssociationValueType> associationToResolve) {
        var associationValue = associationToResolve.asContainerable();

        ObjectReferenceType shadowRef = associationValue.getShadowRef();
        if (shadowRef == null) {
            return;
        }

        Map<String, PrismObject<ShadowType>> entitlementMap = projectionContext.getEntitlementMap();

        if (!Boolean.TRUE.equals(associationValue.isIdentifiersOnly())) {
            // If there's a full object (not ID only), we can use the embedded object directly
            PrismObject<Objectable> existingObject = shadowRef.getObject();
            if (existingObject != null && existingObject.getOid() != null) {
                entitlementMap.put(existingObject.getOid(), PrismObject.cast(existingObject, ShadowType.class));
                return;
            }
        }

        PrismObject<ShadowType> object;
        String oid = shadowRef.getOid();
        if (entitlementMap.containsKey(oid)) {
            object = entitlementMap.get(oid);
        } else {
            // FIXME improve error handling here
            OperationResult subResult = new OperationResult(OP_RESOLVE_ENTITLEMENT); // FIXME!!!!!!!!!
            try {
                object = beans.provisioningService.getObject(
                        ShadowType.class, oid, createReadOnlyCollection(), context.env.task, subResult);
                entitlementMap.put(object.getOid(), object); // The OID may be different -- is that OK?
                entitlementMap.put(oid, object);
                subResult.close();
                subResult.muteError(); // We don't want to propagate e.g. maintenance errors to upper layers (for now)
            } catch (CommonException e) {
                LoggingUtils.logExceptionAsWarning(LOGGER, "failed to load entitlement: {}", e, shadowRef);
                entitlementMap.put(oid, null); // To avoid repeated attempts
                object = null;
                // TODO: can we just ignore and continue?
            } finally {
                subResult.close();
            }
        }
        shadowRef.asReferenceValue().setObject(object);
    }

    @Override
    void getEntitlementVariableProducer(
            @NotNull Source<?, ?> source, @Nullable PrismValue value, @NotNull VariablesMap variables) {

        LOGGER.trace("getEntitlementVariableProducer: processing value {} in {}", value, source);

        // We act on the default source that should contain the association value. So some safety checks first.
        if (!ExpressionConstants.VAR_INPUT_QNAME.matches(source.getName())) {
            LOGGER.trace("Source other than 'input', exiting");
            return;
        }

        // FIXME rework this!

        PrismReference entitlementRef;

        LOGGER.trace("Trying to resolve the entitlement object from association value {}", value);
        PrismObject<ShadowType> entitlement;
        if (!(value instanceof PrismContainerValue<?> pcv)) {
            LOGGER.trace("No value or not a PCV -> no entitlement object");
            entitlementRef = null;
            entitlement = null;
        } else {
            entitlementRef = pcv.findReference(ShadowAssociationValueType.F_SHADOW_REF);
            if (entitlementRef == null) {
                LOGGER.trace("No shadow reference found -> no entitlement object");
                entitlement = null;
            } else {
                // This is the old style of obtaining the entitlement; to be deleted
                entitlement = projectionContext.getEntitlementMap().get(entitlementRef.getOid());
                LOGGER.trace("Resolved entitlement object: {}", entitlement);
            }
        }

        PrismObjectDefinition<ShadowType> entitlementDef =
                entitlement != null && entitlement.getDefinition() != null ?
                        entitlement.getDefinition() :
                        beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);

        variables.put(ExpressionConstants.VAR_ENTITLEMENT, entitlement, entitlementDef);

        // The new way
        var entitlementRefValue = entitlementRef != null ? entitlementRef.getValue() : null;
        var entitlementNew = entitlementRefValue != null ? entitlementRefValue.getObject() : null;
        variables.put(ExpressionConstants.VAR_ASSOCIATED_SHADOW, entitlementNew, entitlementDef);
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> InboundMappingEvaluationRequest<V, D> createMappingRequest(MappingImpl<V, D> mapping) {
        return new InboundMappingEvaluationRequest<>(mapping, projectionContext.isDelete(), projectionContext);
    }

    @Override
    @NotNull InboundMappingEvaluationPhaseType getCurrentEvaluationPhase() {
        return InboundMappingEvaluationPhaseType.CLOCKWORK;
    }

    @Override
    @Nullable FocusIdentitySourceType getFocusIdentitySource() {
        return projectionContext.getFocusIdentitySource();
    }

    @Override
    @Nullable IdentityItemConfiguration getIdentityItemConfiguration(@NotNull ItemPath itemPath) {
        return identityManagementConfiguration.getForPath(itemPath);
    }

    private @NotNull LensFocusContext<? extends ObjectType> getFocusContext() {
        return projectionContext.getLensContext().getFocusContextRequired();
    }

    @Override
    ItemPath determineTargetPathExecutionOverride(ItemPath targetItemPath) throws SchemaException {

        LensFocusContext<?> focusContext = getFocusContext();
        ObjectType objectNew = asObjectable(focusContext.getObjectNew());
        if (!(objectNew instanceof FocusType focusNew)) {
            LOGGER.trace("Focus is not a FocusType (or a 'new' object does not exist)");
            return null;
        }

        IdentityItemConfiguration identityItemConfiguration = getIdentityItemConfiguration(targetItemPath);
        if (identityItemConfiguration == null) {
            LOGGER.trace("No identity item configuration for '{}' (target path will not be overridden)", targetItemPath);
            return null;
        }

        FocusIdentitySourceType identitySource = getFocusIdentitySource();
        if (identitySource == null) {
            return null; // Means that we are not in the clockwork. We will write right to the pre-focus object.
        }
        FocusIdentityType identity = FocusTypeUtil.getMatchingIdentity(focusNew, identitySource);
        long id;
        if (identity != null) {
            id = Objects.requireNonNull(
                    identity.getId(),
                    () -> "Identity container without an ID: " + identity);

        } else {
            id = focusContext.getTemporaryContainerId(SchemaConstants.PATH_FOCUS_IDENTITY);
            FocusIdentityType newIdentity = new FocusIdentityType()
                    .id(id)
                    .source(identitySource)
                    .data(createNewFocus());
            focusContext.swallowToSecondaryDelta(
                    PrismContext.get().deltaFor(FocusType.class)
                            .item(SchemaConstants.PATH_FOCUS_IDENTITY)
                            .add(newIdentity)
                            .asItemDelta());
        }
        return ItemPath.create(
                FocusType.F_IDENTITIES,
                FocusIdentitiesType.F_IDENTITY,
                id,
                FocusIdentityType.F_DATA,
                targetItemPath);
    }

    private FocusType createNewFocus() throws SchemaException {
        return (FocusType) PrismContext.get().createObjectable(
                getFocusContext().getObjectTypeClass());
    }
}
