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
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.ArrayList;
import javax.xml.datatype.Duration;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;
import com.evolveum.midpoint.provisioning.impl.shadows.*;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InternalsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataRecordingStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * A bridge between {@link ShadowProvisioningOperation} and the rest of shadow manager package:
 * records the result of the operation into the corresponding shadows.
 *
 * This is one of public classes of the shadow manager package.
 */
@Component
public class OperationResultRecorder {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ShadowUpdater shadowUpdater;
    @Autowired private ShadowObjectComputer shadowObjectComputer;
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
        AbstractShadow shadow = operation.getShadowAddedOrToAdd();

        RawRepoShadow rawRepoShadowToAdd = shadowObjectComputer.createShadowForRepoStorage(ctx, shadow, result);
        var repoShadowBeanToAdd = rawRepoShadowToAdd.getBean(); // to be updated below

        if (opState.isCompleted()) {
            repoShadowBeanToAdd.setExists(true);
        } else {
            repoShadowBeanToAdd.setExists(false);
            pendingOperationsHelper.addPendingOperationIntoNewShadow(
                    repoShadowBeanToAdd, shadow.getBean(), opState, null);
        }

        addCreationMetadata(repoShadowBeanToAdd);

        LOGGER.trace("Adding repository shadow\n{}", repoShadowBeanToAdd.debugDumpLazily(1));
        String oid;

        try {

            ConstraintsChecker.onShadowAddOperation(repoShadowBeanToAdd); // TODO migrate to repo cache invalidation
            oid = repositoryService.addObject(repoShadowBeanToAdd.asPrismObject(), null, result);

        } catch (ObjectAlreadyExistsException ex) {
            // This should not happen. The OID is not supplied and it is generated by the repo.
            // If it happens, it must be a repo bug.
            // TODO what about conflicting primaryIdentifierValue?
            throw new ObjectAlreadyExistsException(
                    "Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
        }
        repoShadowBeanToAdd.setOid(oid);
        opState.setRepoShadow(
                ctx.adoptRawRepoShadow(repoShadowBeanToAdd));

        LOGGER.trace("Active shadow added to the repository: {}", repoShadowBeanToAdd);
    }

    private void recordAddResultInExistingShadow(ShadowAddOperation addOperation, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, EncryptionException {

        var shadowModifications = computePendingOperationsAndLifecycleStateModifications(addOperation);

        ProvisioningContext ctx = addOperation.getCtx();

        var repoShadow = addOperation.getOpState().getRepoShadow();
        ctx.updateShadowState(repoShadow); // TODO is this needed?

        var resourceObject = addOperation.getShadowAddedOrToAdd();

        // Here we compute the deltas that would align the repository shadow with the "full" version of the resource object.
        // We use the same mechanism (shadow delta computer) as is used for shadows coming from the resource.
        shadowModifications.addAll(
                ShadowDeltaComputerAbsolute.computeShadowModifications(
                        ctx, repoShadow, resourceObject, null,
                        addOperation.getEffectiveMarksAndPoliciesRequired(), false, result));

        addModificationMetadataDeltas(shadowModifications, repoShadow);

        shadowUpdater.executeRepoShadowModifications(ctx, repoShadow, shadowModifications, result);
    }

    public void recordModifyResult(ShadowModifyOperation operation, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        var ctx = operation.getCtx();
        var opState = operation.getOpState();
        var repoShadow = opState.getRepoShadowRequired();

        var allModifications = new RepoShadowModifications();
        if (opState.isCompleted()) {
            allModifications.addAll(operation.getRequestedNonResourceModifications());
            allModifications.addAll(
                    emptyIfNull(operation.getEffectiveResourceLevelModifications()),
                    ctx.getObjectDefinitionRequired());
        }
        allModifications.addAll(computePendingOperationsAndLifecycleStateModifications(operation));
        if (shouldApplyModifyMetadata()) {
            addModificationMetadataDeltas(allModifications, opState.getRepoShadow());
        }

        LOGGER.trace("Updating repository {} after MODIFY operation {}, {}/{} modifications",
                repoShadow, opState, allModifications.size(), allModifications.sizeRaw());

        shadowUpdater.modifyRepoShadow(ctx, repoShadow, allModifications.getItemDeltas(), result);
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
    public RepoShadow recordDeleteResult(ShadowDeleteOperation operation, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        ProvisioningContext ctx = operation.getCtx();
        ProvisioningOperationState<AsynchronousOperationResult> opState = operation.getOpState();
        ProvisioningOperationOptions options = operation.getOptions();
        var repoShadow = opState.getRepoShadow();

        if (ProvisioningOperationOptions.isForce(options)) {
            LOGGER.trace("Deleting repository {} (forced deletion): {}", repoShadow, opState);
            shadowUpdater.executeRepoShadowDeletion(repoShadow, ctx.getTask(), result);
            // TODO why not setting repo shadow null in opState, as it is done a few lines later?
            return null;
        }

        if (!opState.hasCurrentPendingOperation() && opState.isCompleted()) {
            if (!repoShadow.hasPendingOperations() && opState.isSuccess()) {
                LOGGER.trace("Deleting repository {}: {}", repoShadow, opState);
                shadowUpdater.executeRepoShadowDeletion(repoShadow, ctx.getTask(), result);
                opState.setRepoShadow(null);
                return null;
            } else {
                // There are unexpired pending operations in the shadow. We cannot delete the shadow yet.
                // Therefore just mark shadow as dead.
                LOGGER.trace("Keeping dead {} because of pending operations or operation result", repoShadow);
                RepoShadow updatedShadow = shadowUpdater.markShadowTombstone(repoShadow, ctx.getTask(), result);
                opState.setRepoShadow(updatedShadow);
                return updatedShadow;
            }
        }
        LOGGER.trace("Recording the result of pending delete operation in repository {}: {}", repoShadow, opState);
        var shadowModifications = computePendingOperationsAndLifecycleStateModifications(operation);
        addModificationMetadataDeltas(shadowModifications, opState.getRepoShadow());

        var logicalModifications = new ArrayList<>(shadowModifications.getItemDeltas());
        if (repoShadow.getBean().getPrimaryIdentifierValue() != null) {
            // State goes to reaping or corpse or tombstone -> primaryIdentifierValue must be freed (if not done so yet)
            ItemDeltaCollectionsUtil.addNotEquivalent(
                    logicalModifications,
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDeltas());
        }

        LOGGER.trace("Updating repository {} after DELETE operation {}, {} repository shadow modifications",
                repoShadow, opState, logicalModifications.size());
        shadowUpdater.modifyRepoShadow(ctx, repoShadow, logicalModifications, result); // May be also directly executed
        return repoShadow; // The shadow is obviously updated also in opState (it is the same Java object).
    }

    private @NotNull RepoShadowModifications computePendingOperationsAndLifecycleStateModifications(
            ShadowProvisioningOperation<?> operation)
            throws SchemaException {

        var shadowModifications = new RepoShadowModifications();
        shadowModifications.addAll(
                pendingOperationsHelper.computePendingOperationsDeltas(operation));

        ProvisioningOperationState<?> opState = operation.getOpState();
        RepoShadow repoShadow = opState.getRepoShadowRequired();
        if (opState.isCompleted() && opState.isSuccess()) {
            if (operation instanceof ShadowDeleteOperation) {
                shadowModifications.addAll(
                        shadowUpdater.createTombstoneDeltas(repoShadow));
            } else if (!repoShadow.doesExist()) {
                shadowModifications.add(
                        prismContext.deltaFor(ShadowType.class)
                                .item(ShadowType.F_EXISTS)
                                .replace(true)
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
        RepoShadow repoShadow = opState.getRepoShadow();
        if (repoShadow == null) {
            // Shadow does not exist. As this operation immediately ends up with an error then
            // we not even bother to create a shadow.
            return;
        }

        var shadowModifications = new RepoShadowModifications();

        opState.setDefaultResultStatus(OperationResultStatus.FATAL_ERROR);
        shadowModifications.addAll(
                pendingOperationsHelper.computePendingOperationsDeltas(operation));

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

            if (!repoShadow.isDead()) {
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
