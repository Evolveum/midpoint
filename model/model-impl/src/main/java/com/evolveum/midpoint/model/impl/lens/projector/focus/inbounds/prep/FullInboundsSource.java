/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundSourceData;
import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowAssociationsUtil;
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
 * Inbound mapping source ({@link InboundsSource}) that is used in clockwork-based inbound mapping evaluation.
 * This is the standard situation. The other one is e.g. pre-inbounds (correlation-time) evaluation.
 */
public class FullInboundsSource extends InboundsSource {

    private static final Trace LOGGER = TraceManager.getTrace(FullInboundsSource.class);

    private static final String OP_RESOLVE_ENTITLEMENT = FullInboundsSource.class.getName() + ".resolveEntitlement";

    @NotNull private final LensProjectionContext projectionContext;

    @NotNull private final ModelBeans beans = ModelBeans.get();

    @NotNull private final InboundsContext context;

    @NotNull private final IdentityManagementConfiguration identityManagementConfiguration;

    public FullInboundsSource(
            @NotNull InboundSourceData sourceData,
            @NotNull ResourceObjectInboundProcessingDefinition inboundDefinition,
            @NotNull LensProjectionContext projectionContext,
            @NotNull InboundsContext context) throws ConfigurationException {
        super(
                sourceData,
                inboundDefinition,
                projectionContext.getResourceRequired(),
                new InboundMappingContextSpecification(
                        projectionContext.getKey().getTypeIdentification(),
                        null,
                        projectionContext.getTag()),
                projectionContext.getHumanReadableName());
        this.projectionContext = projectionContext;
        this.context = context;
        this.identityManagementConfiguration = getFocusContext().getIdentityManagementConfiguration();
    }

    @Override
    boolean isClockwork() {
        return true;
    }

    @Override
    boolean isEligibleForInboundProcessing(OperationResult result) throws SchemaException, ConfigurationException {

        LOGGER.trace("Starting determination if we should process inbound mappings.");

        if (projectionContext.areInboundSyncMappingsDisabled(result)) {
            LOGGER.trace("Skipping processing of inbound mappings because of the shadow operations policy.");
            return false;
        }
        if (projectionContext.isBroken()) {
            LOGGER.trace("Skipping processing of inbound mappings because the context is broken");
            return false;
        }
        if (sourceData.hasSyncOrEffectiveDelta()) {
            LOGGER.trace("Delta (sync/effective) present, we'll do the inbound processing");
            return true;
        }
        if (projectionContext.getObjectCurrent() == null) {
            LOGGER.trace("No current projection object and no apriori delta: skipping the inbounds (there's nothing to process)");
            return false;
        }
        // === TEMPORARY CODE BELOW ===
        // It is here to provide the same behavior (regarding whether to allow inbounds or not) as it was in 4.8.
        // After the tests pass with this code, we could try to gradually allow starting inbounds with cached data,
        // while blocking auto-loading shadows for the cases where (in 4.8) the inbounds would not be started in the first place.
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
            // Note that the condition isDelete() & isCompleted() was already treated - and the processing was skipped if so.
            assert !projectionContext.isCompleted() : "Projection is completed & it's being deleted: " + projectionContext;

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
    public boolean isItemLoaded(@NotNull ItemPath path) throws SchemaException, ConfigurationException {
        return projectionContext.isItemLoaded(path);
    }

    @Override
    public boolean isFullShadowLoaded() {
        return projectionContext.isFullShadow();
    }

    @Override
    public boolean isShadowGone() {
        return projectionContext.isGone();
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
    String getChannel() {
        return projectionContext.getLensContext().getChannel();
    }

    @Override
    void loadFullShadow(@NotNull InboundsContext context, OperationResult result)
            throws SchemaException, StopProcessingProjectionException {
        LOGGER.trace("Loading {} because full state is required", humanReadableName);
        try {
            beans.contextLoader.loadFullShadow(projectionContext, "inbound", context.env.task, result);
            if (projectionContext.isBroken()) { // just in case the load does not return an exception
                throw new StopProcessingProjectionException();
            }
            sourceData = sourceData.updateShadowAfterReload(projectionContext.getObjectCurrentRequired());
            if (!projectionContext.isFullShadow()) {
                LOGGER.trace("Projection couldn't or shouldn't be loaded - it is not a full shadow even after load operation: {}",
                        projectionContext);
                if (sourceData.hasEffectiveDelta()) {
                    LOGGER.trace("There's a delta. We'll try to process relevant inbounds in relative mode.");
                } else {
                    LOGGER.trace("There's no delta. We stop processing the inbounds for this projection.");
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
            ContainerDelta<ShadowAssociationValueType> associationDelta,
            ShadowAssociation currentAssociation,
            OperationResult result) {

        var associationValuesToResolve = new ArrayList<ShadowAssociationValue>();
        if (currentAssociation != null) {
            associationValuesToResolve.addAll(currentAssociation.getAssociationValues());
        }
        if (associationDelta != null) {
            // TODO Shouldn't we filter also these?
            for (var associationValue : associationDelta.getValues(ShadowAssociationValueType.class)) {
                associationValuesToResolve.add((ShadowAssociationValue) associationValue);
            }
        }

        for (var associationValueToResolve : associationValuesToResolve) {
            resolveEntitlementFromResource(associationValueToResolve, result);
        }
    }

    private void resolveEntitlementFromResource(
            ShadowAssociationValue associationValueToResolve,
            OperationResult result) {
        // FIXME ugly hack
        ObjectReferenceType shadowRef = associationValueToResolve.getSingleObjectRefRelaxed();
        if (shadowRef == null) {
            return;
        }

        Map<String, PrismObject<ShadowType>> entitlementMap = projectionContext.getEntitlementMap();

        PrismObject<Objectable> existingObject = shadowRef.getObject();
        if (existingObject != null && existingObject.getOid() != null) {
            entitlementMap.put(existingObject.getOid(), PrismObject.cast(existingObject, ShadowType.class));
            return;
        }

        PrismObject<ShadowType> object;
        String oid = shadowRef.getOid();
        if (entitlementMap.containsKey(oid)) {
            object = entitlementMap.get(oid);
        } else {
            // FIXME improve error handling here
            OperationResult subResult = result.createMinorSubresult(OP_RESOLVE_ENTITLEMENT);
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

        LOGGER.trace("Trying to resolve the entitlement object from association value {}", value);
        PrismObject<ShadowType> entitlement;
        if (!(value instanceof PrismContainerValue<?> pcv)) {
            LOGGER.trace("No value or not a PCV -> no entitlement object");
            entitlement = null;
        } else {
            var reference = ShadowAssociationsUtil.getSingleObjectRefRelaxed((ShadowAssociationValueType) pcv.asContainerable());
            if (reference == null) {
                LOGGER.trace("No shadow reference found -> no entitlement object");
                entitlement = null;
            } else {
                // This is the old style of obtaining the entitlement; to be deleted
                entitlement = projectionContext.getEntitlementMap().get(reference.getOid());
                LOGGER.trace("Resolved entitlement object: {}", entitlement);
            }
        }

        PrismObjectDefinition<ShadowType> entitlementDef =
                entitlement != null && entitlement.getDefinition() != null ?
                        entitlement.getDefinition() :
                        beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);

        variables.put(ExpressionConstants.VAR_ENTITLEMENT, entitlement, entitlementDef);

        // The new way
        variables.put(ExpressionConstants.VAR_OBJECT, entitlement, entitlementDef);
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> MappingEvaluationRequest<V, D> createMappingRequest(MappingImpl<V, D> mapping) {
        return new MappingEvaluationRequest<>(mapping, projectionContext.isDelete(), projectionContext);
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

    @Override
    boolean hasDependentContext() throws SchemaException, ConfigurationException {
        return projectionContext.hasDependentContext();
    }

    @Override
    @NotNull CachedShadowsUseType getCachedShadowsUse() throws SchemaException, ConfigurationException {
        return projectionContext.getCachedShadowsUse();
    }
}
