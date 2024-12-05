/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.RecordPendingOperationsType.ALL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;
import com.evolveum.midpoint.provisioning.impl.shadows.PendingOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowProvisioningOperation;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Helps with the management of pending operations.
 *
 * TODO clean up
 */
@Component
class PendingOperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(PendingOperationsHelper.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired ShadowFinder shadowFinder;
    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;

    List<ItemDelta<?, ?>> computePendingOperationsDeltas(ShadowProvisioningOperation operation)
            throws SchemaException {

        List<ItemDelta<?, ?>> shadowModifications = new ArrayList<>();

        ProvisioningContext ctx = operation.getCtx();
        ProvisioningOperationState opState = operation.getOpState();

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        if (ctx.isPropagation()) {
            LOGGER.trace("Collecting pending operation updates for propagation operation");
            collectPendingOperationUpdates(shadowModifications, opState, now);
        } else if (opState.hasCurrentPendingOperation()) {
            LOGGER.trace("Collecting pending operation updates for known current pending operation");
            collectCurrentPendingOperationUpdates(shadowModifications, opState, now);
        } else if (!opState.isCompleted()) {
            LOGGER.trace("Collecting pending operation updates for 'new' pending operation");
            addPendingOperationForExistingShadow(shadowModifications, opState, operation.getResourceDelta(), now);
        } else {
            LOGGER.trace("Operation is complete -> no pending operation updates");
        }
        return shadowModifications;
    }

    void addPendingOperationIntoNewShadow(
            ShadowType repoShadowBean, ShadowType resourceShadow, ProvisioningOperationState opState, String asyncOperationReference)
            throws SchemaException {

        repoShadowBean.getPendingOperation().add(
                opState.toPendingOperation(
                        resourceShadow.asPrismObject().createAddDelta(),
                        asyncOperationReference,
                        clock.currentTimeXMLGregorianCalendar()));
    }

    private void addPendingOperationForExistingShadow(
            Collection<ItemDelta<?, ?>> shadowModifications,
            ProvisioningOperationState opState,
            ObjectDelta<ShadowType> delta,
            XMLGregorianCalendar now) throws SchemaException {
            shadowModifications.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PENDING_OPERATION)
                            .add(opState.toPendingOperation(delta, null, now))
                            .asItemDelta());
    }

    private void collectCurrentPendingOperationUpdates(
            Collection<ItemDelta<?, ?>> shadowModifications,
            ProvisioningOperationState opState,
            XMLGregorianCalendar now) {
        var shadowPendingOp = opState.getCurrentPendingOperation();
        if (shadowPendingOp != null) {
            PendingOperationExecutionStatusType realExecStatus = opState.getExecutionStatus();
            if (shadowPendingOp.getExecutionStatus() != realExecStatus) {
                shadowModifications.add(
                        shadowPendingOp.createPropertyDelta(PendingOperationType.F_EXECUTION_STATUS, realExecStatus));

                if (realExecStatus == EXECUTING
                        && shadowPendingOp.getOperationStartTimestamp() == null) {
                    shadowModifications.add(
                            shadowPendingOp.createPropertyDelta(PendingOperationType.F_OPERATION_START_TIMESTAMP, now));
                }

                if (realExecStatus == COMPLETED
                        && shadowPendingOp.getCompletionTimestamp() == null) {
                    shadowModifications.add(
                            shadowPendingOp.createPropertyDelta(PendingOperationType.F_COMPLETION_TIMESTAMP, now));
                }
            }

            if (shadowPendingOp.getRequestTimestamp() == null) {
                // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                // Therefore imprecise timestamp is better than no timestamp.
                shadowModifications.add(
                        shadowPendingOp.createPropertyDelta(PendingOperationType.F_REQUEST_TIMESTAMP, now));
            }

            OperationResultStatusType realResultStatus = opState.getResultStatusTypeOrDefault();
            if (shadowPendingOp.getResultStatus() != realResultStatus) {
                shadowModifications.add(
                        shadowPendingOp.createPropertyDelta(PendingOperationType.F_RESULT_STATUS, realResultStatus));
            }

            String realAsyncOpRef = opState.getAsynchronousOperationReference();
            if (realAsyncOpRef == null) {
                // Not sure about the reason for this. Can the operation reference be cleared?
            } else if (!Objects.equals(shadowPendingOp.getAsynchronousOperationReference(), realAsyncOpRef)) {
                shadowModifications.add(
                        shadowPendingOp.createPropertyDelta(
                                PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, realAsyncOpRef));
            }

            PendingOperationTypeType realOpType = opState.getOperationType();
            if (realOpType == null) {
                // We will not push the null value to the shadow. The reason is that sometimes the original value of `RETRY`
                // is cleared by the successful execution of the operation.
            } else if (realOpType != shadowPendingOp.getType()) {
                shadowModifications.add(
                        shadowPendingOp.createPropertyDelta(PendingOperationType.F_TYPE, realOpType));
            }

            Integer realAttemptNumber = opState.getAttemptNumber();
            if (!Objects.equals(shadowPendingOp.getAttemptNumber(), realAttemptNumber)) {
                shadowModifications.add(
                        shadowPendingOp.createPropertyDelta(PendingOperationType.F_ATTEMPT_NUMBER, realAttemptNumber));
            }

            XMLGregorianCalendar realLastAttemptTimestamp = opState.getLastAttemptTimestamp();
            if (realLastAttemptTimestamp == null) {
                // null means "do not change"
            } else if (!Objects.equals(shadowPendingOp.getLastAttemptTimestamp(), realLastAttemptTimestamp)) {
                shadowModifications.add(
                        shadowPendingOp.createPropertyDelta(
                                PendingOperationType.F_LAST_ATTEMPT_TIMESTAMP, realLastAttemptTimestamp));
            }
        }
    }

    private PendingOperation findEquivalentPendingOperation(
            @NotNull RepoShadow currentShadow, @NotNull ObjectDelta<ShadowType> proposedDelta)
            throws SchemaException {
        for (var pendingOperation : currentShadow.getPendingOperations()) {
            if (pendingOperation.isInProgress()
                    && pendingOperation.hasDelta()
                    && pendingOperation.getDelta().equivalent(proposedDelta)) {
                return pendingOperation;
            }
        }
        return null;
    }

    private void collectPendingOperationUpdates(
            List<ItemDelta<?, ?>> shadowModifications, ProvisioningOperationState opState, XMLGregorianCalendar now) {
        var executionStatus = opState.getExecutionStatus();

        var pendingOperations = opState.getPropagatedPendingOperations();
        // We bravely expect these have been set before ;)

        // TODO what about retries in the case of e.g. communication failures?
        // Compare with collectCurrentPendingOperationUpdates method
        for (var pendingOperation : pendingOperations) {
            shadowModifications.add(
                    pendingOperation.createPropertyDelta(PendingOperationType.F_EXECUTION_STATUS, executionStatus));
            shadowModifications.add(
                    pendingOperation.createPropertyDelta(
                            PendingOperationType.F_RESULT_STATUS, opState.getResultStatusTypeOrDefault()));
            shadowModifications.add(
                    pendingOperation.createPropertyDelta(
                            PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, opState.getAsynchronousOperationReference()));
            if (pendingOperation.getRequestTimestamp() == null) {
                // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult
                // to cleanup. Therefore imprecise timestamp is better than no timestamp.
                shadowModifications.add(
                        pendingOperation.createPropertyDelta(PendingOperationType.F_REQUEST_TIMESTAMP, now));
            }
            if (executionStatus == COMPLETED && pendingOperation.getCompletionTimestamp() == null) {
                shadowModifications.add(
                        pendingOperation.createPropertyDelta(PendingOperationType.F_COMPLETION_TIMESTAMP, now));
            }
            if (executionStatus == EXECUTING && pendingOperation.getOperationStartTimestamp() == null) {
                shadowModifications.add(
                        pendingOperation.createPropertyDelta(PendingOperationType.F_OPERATION_START_TIMESTAMP, now));
            }
        }
    }

    /**
     * Creates deltas that marks all "RETRY"-typed pending operations as completed.
     *
     * This is very simple code that essentially works only for postponed operations (retries).
     * TODO: better support for async and manual operations
     */
    RepoShadowModifications cancelAllPendingOperations(RepoShadow repoShadow) {
        RepoShadowModifications shadowModifications = new RepoShadowModifications();
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        for (var pendingOperation : repoShadow.getPendingOperations()) {
            if (pendingOperation.getExecutionStatus() == COMPLETED) {
                continue;
            }
            if (pendingOperation.getType() != PendingOperationTypeType.RETRY) {
                // Other operations are not cancellable now
                continue;
            }
            shadowModifications.addAll(
                    pendingOperation.createCancellationDeltas(now));
        }
        return shadowModifications;
    }

    /**
     * The goal of this operation is to _atomically_ store the pending operation into the shadow.
     *
     * If there is a conflicting pending operation there, we may return it: depending on the situation (see the code).
     * The repo shadow in opState is updated.
     *
     * In the future we may perhaps use the newer {@link RepositoryService#modifyObjectDynamically(Class, String, Collection,
     * RepositoryService.ModificationsSupplier, RepoModifyOptions, OperationResult)} instead of the optimistic locking runner.
     */
    PendingOperation checkAndRecordPendingOperationBeforeExecution(
            @NotNull ProvisioningContext ctx,
            @NotNull ObjectDelta<ShadowType> proposedDelta,
            @NotNull ProvisioningOperationState opState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        ResourceType resource = ctx.getResource();
        ResourceConsistencyType consistency = resource.getConsistency();

        boolean avoidDuplicateOperations;
        if (ctx.isInMaintenance()) {
            LOGGER.trace("Maintenance mode => we always check for duplicate pending operations");
            avoidDuplicateOperations = true;
        } else if (consistency == null) {
            LOGGER.trace("No consistency section exists => we do not pre-record pending operations at all");
            return null;
        } else {
            avoidDuplicateOperations = Boolean.TRUE.equals(consistency.isAvoidDuplicateOperations());
            LOGGER.trace("Consistency section exists, we will pre-record pending operations; "
                    + "with the duplicate operations avoidance flag set to: {}", avoidDuplicateOperations);
        }

        assert opState.hasRepoShadow();

        OptimisticLockingRunner<ShadowType, PendingOperation> runner =
                new OptimisticLockingRunner.Builder<ShadowType, PendingOperation>()
                        .object(opState.getRepoShadow().getPrismObject())
                        .result(result)
                        .repositoryService(repositoryService)
                        .maxNumberOfAttempts(10)
                        .delayRange(20)
                        .build();

        try {

            return runner.run(
                    (object) -> {

                        var currentRepoShadow = ctx.adoptRawRepoShadow(object);

                        // The runner itself could have updated the shadow (in case of precondition violation).
                        opState.setRepoShadow(currentRepoShadow);

                        if (avoidDuplicateOperations) {
                            var existingPendingOperation = findEquivalentPendingOperation(currentRepoShadow, proposedDelta);
                            if (existingPendingOperation != null) {
                                LOGGER.debug("Found equivalent pending operation for {} of {}: {}",
                                        proposedDelta.getChangeType(), object, existingPendingOperation);
                                // Not storing into opState, as we won't execute it.
                                return existingPendingOperation;
                            }
                        }

                        if (ResourceTypeUtil.getRecordPendingOperations(resource) != ALL) {
                            return null;
                        }

                        LOGGER.trace("Storing pending operation for {} of {}", proposedDelta.getChangeType(), object);

                        PendingOperation currentPendingOperation;
                        try {
                            currentPendingOperation =
                                    recordRequestedPendingOperationDelta(
                                            ctx, object, proposedDelta, opState, object.getVersion(), result);
                        } catch (PreconditionViolationException e) {
                            LOGGER.trace("Couldn't store the requested operation as a pending one because of an update conflict"
                                    + " from another thread. Will try again, if the optimistic locking runner allows.");
                            throw e;
                        }

                        // If we are here, we were able to store the pending operation without conflict from another thread.
                        // So, we can return.
                        LOGGER.trace("Successfully stored pending operation for {} of {}", proposedDelta.getChangeType(), object);

                        opState.setCurrentPendingOperation(currentPendingOperation);

                        // Yes, really return null. We are supposed to return conflicting operation (if found).
                        // But in this case there is no conflict. This operation does not conflict with itself.
                        return null;
                    }
            );

        } catch (ObjectAlreadyExistsException e) {
            // should not happen
            throw new SystemException(e);
        }
    }

    private @NotNull PendingOperation recordRequestedPendingOperationDelta(
            @NotNull ProvisioningContext ctx,
            @NotNull PrismObject<ShadowType> shadow,
            @NotNull ObjectDelta<ShadowType> pendingDelta,
            @NotNull ProvisioningOperationState opState,
            String currentObjectVersion,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException, PreconditionViolationException,
            ConfigurationException {

        PendingOperationType pendingOperation = new PendingOperationType();
        pendingOperation.setDelta(DeltaConvertor.toObjectDeltaType(pendingDelta));
        pendingOperation.setRequestTimestamp(clock.currentTimeXMLGregorianCalendar());
        pendingOperation.setExecutionStatus(opState.getExecutionStatus());
        pendingOperation.setResultStatus(opState.getResultStatusTypeOrDefault());
        pendingOperation.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());

        var repoDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PENDING_OPERATION).add(pendingOperation)
                .asItemDeltas();

        ModificationPrecondition<ShadowType> precondition =
                currentObjectVersion != null ? new VersionPrecondition<>(currentObjectVersion) : null;

        try {
            // TODO shouldn't we use ShadowUpdater here?
            repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, precondition, null, result);
        } catch (ObjectAlreadyExistsException e) {
            // should not happen
            throw new SystemException(e);
        }

        // We have to re-read shadow here. We need to get the pending operation in a form as it was stored.
        // We need id in the operation. Otherwise we won't be able to update it.
        RepoShadow updatedShadow = shadowFinder.getRepoShadow(ctx, shadow.getOid(), result);
        opState.setRepoShadow(updatedShadow);
        return MiscUtil.stateNonNull(
                findEquivalentPendingOperation(updatedShadow, pendingDelta),
                "Cannot find my own operation %s in %s", pendingOperation, updatedShadow);
    }
}
