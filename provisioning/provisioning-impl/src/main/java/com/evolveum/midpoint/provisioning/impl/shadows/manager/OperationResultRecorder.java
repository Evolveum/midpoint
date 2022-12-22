/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.manager.MetadataUtil.addCreationMetadata;
import static com.evolveum.midpoint.provisioning.impl.shadows.manager.MetadataUtil.addModificationMetadataDeltas;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;

import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResultStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.Duration;

/**
 * A bridge between {@link ShadowProvisioningOperation} and the rest of shadow manager package: records the result of the operation
 * into the corresponding shadows.
 *
 * This is one of public classes of the shadow manager package.
 */
@Component
public class OperationResultRecorder {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private PrismContext prismContext;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ShadowDeltaComputerAbsolute shadowDeltaComputerAbsolute;
    @Autowired private ShadowUpdater shadowUpdater;
    @Autowired private ShadowCreator shadowCreator;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;
    @Autowired private Clock clock;

    private static final Trace LOGGER = TraceManager.getTrace(OperationResultRecorder.class);

    /**
     * Record results of ADD operation to the shadow: creates a shadow or updates an existing one.
     */
    public void recordAddResult(ShadowAddOperation operation, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            EncryptionException {

        if (operation.getOpState().getRepoShadow() == null) {
            recordAddResultInNewShadow(operation, result);
        } else {
            // We know that we have existing shadow. This may be proposed shadow,
            // or a shadow with failed add operation that was just re-tried
            recordAddResultInExistingShadow(operation, result);
        }
    }

    /**
     * Add new active shadow to repository. It is executed after ADD operation on resource.
     * There are several scenarios. The operation may have been executed (synchronous operation),
     * it may be executing (asynchronous operation) or the operation may be delayed due to grouping.
     * This is indicated by the execution status in the opState parameter.
     */
    private void recordAddResultInNewShadow(ShadowAddOperation operation, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectAlreadyExistsException, EncryptionException {

        // TODO: check for proposed Shadow. There may be a proposed shadow even if we do not have explicit proposed shadow OID
        //  (e.g. in case that the add operation failed). If proposed shadow is present do modify instead of add.

        ProvisioningContext ctx = operation.getCtx();
        AddOperationState opState = operation.getOpState();
        ShadowType resourceObject = operation.getResourceObjectAddedOrToAdd();

        ShadowType repoShadowToAdd = shadowCreator.createShadowForRepoStorage(ctx, resourceObject);
        opState.setRepoShadow(repoShadowToAdd);

        if (!opState.isCompleted()) {
            pendingOperationsHelper.addPendingOperationIntoNewShadow(
                    repoShadowToAdd, resourceObject, opState, null);
        }

        addCreationMetadata(repoShadowToAdd);

        LOGGER.trace("Adding repository shadow\n{}", repoShadowToAdd.debugDumpLazily(1));
        String oid;

        try {

            ConstraintsChecker.onShadowAddOperation(repoShadowToAdd); // TODO migrate to repo cache invalidation
            oid = repositoryService.addObject(repoShadowToAdd.asPrismObject(), null, result);

        } catch (ObjectAlreadyExistsException ex) {
            // This should not happen. The OID is not supplied and it is generated by the repo.
            // If it happens, it must be a repo bug.
            // TODO what about conflicting primaryIdentifierValue?
            throw new ObjectAlreadyExistsException(
                    "Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
        }
        repoShadowToAdd.setOid(oid);

        LOGGER.trace("Active shadow added to the repository: {}", repoShadowToAdd);
    }

    private void recordAddResultInExistingShadow(ShadowAddOperation addOperation, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {

        ProvisioningContext ctx = addOperation.getCtx();
        ShadowType resourceShadow = addOperation.getResourceObjectAddedOrToAdd();
        AddOperationState opState = addOperation.getOpState();

        Collection<ItemDelta<?, ?>> shadowModifications = computePendingOperationsAndLifecycleStateModifications(addOperation);

        // Here we compute the deltas that would align the repository shadow with the "full" version of the resource object.
        // We use the same mechanism (shadow delta computer) as is used for shadows coming from the resource.
        ShadowType repoShadow = opState.getRepoShadow();
        ShadowLifecycleStateType shadowState = ctx.determineShadowState(repoShadow);
        shadowModifications.addAll(
                shadowDeltaComputerAbsolute
                        .computeShadowDelta(
                                ctx, repoShadow, resourceShadow, null, shadowState, false)
                        .getModifications());

        addModificationMetadataDeltas(shadowModifications, repoShadow);

        shadowUpdater.executeRepoShadowModifications(ctx, repoShadow, shadowModifications, result);
    }

    public void recordModifyResult(ShadowModifyOperation operation, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        ProvisioningContext ctx = operation.getCtx();
        ProvisioningOperationState.ModifyOperationState opState = operation.getOpState();
        ShadowType repoShadow = opState.getRepoShadowRequired();
        Collection<? extends ItemDelta<?, ?>> effectiveModifications = operation.getEffectiveModifications();

        List<ItemDelta<?, ?>> shadowModifications = computePendingOperationsAndLifecycleStateModifications(operation);

        List<ItemDelta<?, ?>> allModifications;
        if (opState.isCompleted()) {
            //noinspection unchecked,rawtypes
            allModifications = MiscUtil.join(effectiveModifications, (List) shadowModifications);
        } else {
            allModifications = shadowModifications;
        }
        if (shouldApplyModifyMetadata()) {
            addModificationMetadataDeltas(allModifications, opState.getRepoShadow());
        }
        LOGGER.trace("Updating repository {} after MODIFY operation {}, {} repository shadow modifications",
                repoShadow, opState, effectiveModifications.size());

        shadowUpdater.modifyRepoShadow(ctx, repoShadow, allModifications, result);
    }

    private boolean shouldApplyModifyMetadata() {
        SystemConfigurationType config = provisioningService.getSystemConfiguration();
        InternalsConfigurationType internals = config != null ? config.getInternals() : null;
        MetadataRecordingStrategyType shadowMetadataRecording = internals != null ? internals.getShadowMetadataRecording() : null;
        return shadowMetadataRecording == null || !Boolean.TRUE.equals(shadowMetadataRecording.isSkipOnModify());
    }

    /**
     * Returns updated repo shadow, or null if shadow is deleted from repository.
     */
    public ShadowType recordDeleteResult(ShadowDeleteOperation operation, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {

        ProvisioningContext ctx = operation.getCtx();
        ProvisioningOperationState<AsynchronousOperationResult> opState = operation.getOpState();
        ProvisioningOperationOptions options = operation.getOptions();
        ShadowType repoShadow = opState.getRepoShadow();

        if (ProvisioningOperationOptions.isForce(options)) {
            LOGGER.trace("Deleting repository {} (forced deletion): {}", repoShadow, opState);
            shadowUpdater.executeRepoShadowDeletion(repoShadow, ctx.getTask(), result);
            // TODO why not setting repo shadow null in opState, as it is done a few lines later?
            return null;
        }

        if (!opState.hasCurrentPendingOperation() && opState.isCompleted()) {
            if (repoShadow.getPendingOperation().isEmpty() && opState.isSuccess()) {
                LOGGER.trace("Deleting repository {}: {}", repoShadow, opState);
                shadowUpdater.executeRepoShadowDeletion(repoShadow, ctx.getTask(), result);
                opState.setRepoShadow(null);
                return null;
            } else {
                // There are unexpired pending operations in the shadow. We cannot delete the shadow yet.
                // Therefore just mark shadow as dead.
                LOGGER.trace("Keeping dead {} because of pending operations or operation result", repoShadow);
                ShadowType updatedShadow = shadowUpdater.markShadowTombstone(repoShadow, ctx.getTask(), result);
                opState.setRepoShadow(updatedShadow);
                return updatedShadow;
            }
        }
        LOGGER.trace("Recording the result of pending delete operation in repository {}: {}", repoShadow, opState);
        List<ItemDelta<?, ?>> shadowModifications = computePendingOperationsAndLifecycleStateModifications(operation);
        addModificationMetadataDeltas(shadowModifications, opState.getRepoShadow());

        if (repoShadow.getPrimaryIdentifierValue() != null) {
            // State goes to reaping or corpse or tombstone -> primaryIdentifierValue must be freed (if not done so yet)
            ItemDeltaCollectionsUtil.addNotEquivalent(
                    shadowModifications,
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDeltas());
        }

        LOGGER.trace("Updating repository {} after DELETE operation {}, {} repository shadow modifications",
                repoShadow, opState, shadowModifications.size());
        shadowUpdater.modifyRepoShadow(ctx, repoShadow, shadowModifications, result); // May be also directly executed
        return repoShadow; // The shadow is obviously updated also in opState (it is the same Java object).
    }

    private List<ItemDelta<?, ?>> computePendingOperationsAndLifecycleStateModifications(ShadowProvisioningOperation<?> operation)
            throws SchemaException {

        List<ItemDelta<?, ?>> shadowModifications = new ArrayList<>();
        pendingOperationsHelper.computePendingOperationsDeltas(shadowModifications, operation);

        ProvisioningOperationState<?> opState = operation.getOpState();
        ShadowType repoShadow = opState.getRepoShadowRequired();
        if (opState.isCompleted() && opState.isSuccess()) {
            if (operation instanceof ShadowDeleteOperation) {
                shadowUpdater.addTombstoneDeltas(repoShadow, shadowModifications);
            } else if (!ShadowUtil.isExists(repoShadow)) {
                shadowModifications.add(
                        prismContext.deltaFor(ShadowType.class)
                                .item(ShadowType.F_EXISTS)
                                .replace()
                                .asItemDelta());
            }
        }
        return shadowModifications;
    }

    /**
     * Record results of an operation that have thrown exception.
     * This happens after the error handler is processed - and only for those
     * cases when the handler has re-thrown the exception.
     */
    public void recordOperationException(ShadowProvisioningOperation<?> operation, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        ProvisioningContext ctx = operation.getCtx();
        ProvisioningOperationState<?> opState = operation.getOpState();
        ShadowType repoShadow = opState.getRepoShadow();
        if (repoShadow == null) {
            // Shadow does not exist. As this operation immediately ends up with an error then
            // we not even bother to create a shadow.
            return;
        }

        List<ItemDelta<?, ?>> shadowModifications = new ArrayList<>();

        opState.setDefaultResultStatus(OperationResultStatus.FATAL_ERROR);
        pendingOperationsHelper.computePendingOperationsDeltas(shadowModifications, operation);

        if (operation.isAdd()) {
            // This means we have failed add operation here. We tried to add object,
            // but we have failed. Which means that this shadow is now dead.
            Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
            if (XmlTypeConverter.isZero(deadRetentionPeriod)) {
                // Do not bother with marking the shadow as dead. It should be gone immediately.
                // Deleting it now saves one modify operation.
                LOGGER.trace("Deleting repository shadow (after error handling)\n{}",
                        debugDumpLazily(shadowModifications, 1));
                shadowUpdater.deleteShadow(opState.getRepoShadow(), ctx.getTask(), result);
                return;
            }

            if (!ShadowUtil.isDead(repoShadow)) {
                shadowModifications.addAll(
                        prismContext.deltaFor(ShadowType.class)
                                .item(ShadowType.F_DEAD).replace(true)
                                .item(ShadowType.F_DEATH_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
                                // We need to free the identifier for further use by live shadows that may come later
                                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                                .asItemDeltas());
            }
        }

        if (!shadowModifications.isEmpty()) {
            LOGGER.trace("Updating repository shadow (after error handling)\n{}", debugDumpLazily(shadowModifications, 1));
            shadowUpdater.executeRepoShadowModifications(ctx, opState.getRepoShadow(), shadowModifications, result);
        }
    }
}
