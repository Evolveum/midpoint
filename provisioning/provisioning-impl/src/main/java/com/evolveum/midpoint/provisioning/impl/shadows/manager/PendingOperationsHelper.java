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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowProvisioningOperation;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Helps with the management of pending operations.
 *
 * TODO clean up
 */
@Component
class PendingOperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(PendingOperationsHelper.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;

    void computePendingOperationsDeltas(List<ItemDelta<?, ?>> shadowModifications, ShadowProvisioningOperation<?> operation)
            throws SchemaException {

        ProvisioningContext ctx = operation.getCtx();
        ProvisioningOperationState<?> opState = operation.getOpState();

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
    }

    void addPendingOperationIntoNewShadow(
            ShadowType repoShadow, ShadowType resourceShadow, AddOperationState opState, String asyncOperationReference)
            throws SchemaException {

        PendingOperationType pendingOperation =
                opState.toPendingOperation(
                        resourceShadow.asPrismObject().createAddDelta(),
                        asyncOperationReference,
                        clock.currentTimeXMLGregorianCalendar());
        repoShadow.getPendingOperation().add(pendingOperation);

        repoShadow.setExists(false); // TODO why here?!
    }

    private void addPendingOperationForExistingShadow(
            Collection<ItemDelta<?, ?>> shadowModifications,
            ProvisioningOperationState<?> opState,
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
            ProvisioningOperationState<?> opState,
            XMLGregorianCalendar now) throws SchemaException {
        PendingOperationType shadowPendingOp = opState.getCurrentPendingOperation();
        if (shadowPendingOp != null) {
            PrismContainerValue<?> pendingOpValue = shadowPendingOp.asPrismContainerValue();
            assert pendingOpValue.getId() != null;
            ItemPath pendingOpValuePath = pendingOpValue.getPath();

            PendingOperationExecutionStatusType realExecStatus = opState.getExecutionStatus();
            if (shadowPendingOp.getExecutionStatus() != realExecStatus) {
                shadowModifications.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_EXECUTION_STATUS, realExecStatus));

                if (realExecStatus == EXECUTING
                        && shadowPendingOp.getOperationStartTimestamp() == null) {
                    shadowModifications.add(
                            createPendingOperationDelta(
                                    pendingOpValuePath, PendingOperationType.F_OPERATION_START_TIMESTAMP, now));
                }

                if (realExecStatus == COMPLETED
                        && shadowPendingOp.getCompletionTimestamp() == null) {
                    shadowModifications.add(
                            createPendingOperationDelta(
                                    pendingOpValuePath, PendingOperationType.F_COMPLETION_TIMESTAMP, now));
                }
            }

            if (shadowPendingOp.getRequestTimestamp() == null) {
                // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                // Therefore imprecise timestamp is better than no timestamp.
                shadowModifications.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_REQUEST_TIMESTAMP, now));
            }

            OperationResultStatusType realResultStatus = opState.getResultStatusTypeOrDefault();
            if (shadowPendingOp.getResultStatus() != realResultStatus) {
                shadowModifications.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_RESULT_STATUS, realResultStatus));
            }

            String realAsyncOpRef = opState.getAsynchronousOperationReference();
            if (realAsyncOpRef == null) {
                // Not sure about the reason for this. Can the operation reference be cleared?
            } else if (!Objects.equals(shadowPendingOp.getAsynchronousOperationReference(), realAsyncOpRef)) {
                shadowModifications.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, realAsyncOpRef));
            }

            PendingOperationTypeType realOpType = opState.getOperationType();
            if (realOpType == null) {
                // We will not push the null value to the shadow. The reason is that sometimes the original value of `RETRY`
                // is cleared by the successful execution of the operation.
            } else if (realOpType != shadowPendingOp.getType()) {
                shadowModifications.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_TYPE, realOpType));
            }

            Integer realAttemptNumber = opState.getAttemptNumber();
            if (!Objects.equals(shadowPendingOp.getAttemptNumber(), realAttemptNumber)) {
                shadowModifications.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_ATTEMPT_NUMBER, realAttemptNumber));
            }

            XMLGregorianCalendar realLastAttemptTimestamp = opState.getLastAttemptTimestamp();
            if (realLastAttemptTimestamp == null) {
                // null means "do not change"
            } else if (!Objects.equals(shadowPendingOp.getLastAttemptTimestamp(), realLastAttemptTimestamp)) {
                shadowModifications.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_LAST_ATTEMPT_TIMESTAMP, realLastAttemptTimestamp));
            }
        }
    }

    private ItemDelta<?, ?> createPendingOperationDelta(
            ItemPath containerPath,
            QName propName,
            Object valueToReplace) throws SchemaException {
        return prismContext.deltaFor(ShadowType.class)
                .item(containerPath.append(propName))
                .replace(valueToReplace)
                .asItemDelta();
    }

    PendingOperationType findEquivalentPendingOperation(ShadowType currentShadow, ObjectDelta<ShadowType> proposedDelta)
            throws SchemaException {
        for (PendingOperationType pendingOperation : currentShadow.getPendingOperation()) {
            OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
            if (resultStatus != null && resultStatus != OperationResultStatusType.IN_PROGRESS) {
                continue;
            }
            ObjectDeltaType deltaBean = pendingOperation.getDelta();
            if (deltaBean == null) {
                continue;
            }
            ObjectDelta<Objectable> delta = DeltaConvertor.createObjectDelta(deltaBean);
            if (!delta.equivalent(proposedDelta)) {
                continue;
            }
            return pendingOperation;
        }
        return null;
    }

    static PendingOperationType findPendingAddOperation(ShadowType shadow) {
        for (PendingOperationType pendingOperation : shadow.getPendingOperation()) {
            OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
            if (resultStatus != null && resultStatus != OperationResultStatusType.IN_PROGRESS) {
                continue;
            }
            ObjectDeltaType deltaBean = pendingOperation.getDelta();
            if (deltaBean == null) {
                continue;
            }
            if (deltaBean.getChangeType() != ChangeTypeType.ADD) {
                continue;
            }
            return pendingOperation;
        }
        return null;
    }

    private void collectPendingOperationUpdates(
            List<ItemDelta<?, ?>> shadowModifications, ProvisioningOperationState<?> opState, XMLGregorianCalendar now)
            throws SchemaException {
        OperationResultStatusType resultStatus = opState.getResultStatusTypeOrDefault();
        String asynchronousOpReference = opState.getAsynchronousOperationReference();
        PendingOperationExecutionStatusType executionStatus = opState.getExecutionStatus();

        List<PendingOperationType> pendingOperations = opState.getPropagatedPendingOperations();
        // We bravely expect these have been set before ;)

        // TODO what about retries in the case of e.g. communication failures?
        // Compare with collectCurrentPendingOperationUpdates method
        for (PendingOperationType existingPendingOperation : pendingOperations) {
            ItemPath containerPath = existingPendingOperation.asPrismContainerValue().getPath();
            shadowModifications.add(
                    createPendingOperationDelta(containerPath, PendingOperationType.F_EXECUTION_STATUS, executionStatus));
            shadowModifications.add(
                    createPendingOperationDelta(containerPath, PendingOperationType.F_RESULT_STATUS, resultStatus));
            shadowModifications.add(
                    createPendingOperationDelta(
                            containerPath, PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, asynchronousOpReference));
            if (existingPendingOperation.getRequestTimestamp() == null) {
                // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                // Therefore imprecise timestamp is better than no timestamp.
                shadowModifications.add(
                        createPendingOperationDelta(containerPath, PendingOperationType.F_REQUEST_TIMESTAMP, now));
            }
            if (executionStatus == COMPLETED && existingPendingOperation.getCompletionTimestamp() == null) {
                shadowModifications.add(
                        createPendingOperationDelta(containerPath, PendingOperationType.F_COMPLETION_TIMESTAMP, now));
            }
            if (executionStatus == EXECUTING && existingPendingOperation.getOperationStartTimestamp() == null) {
                shadowModifications.add(
                        createPendingOperationDelta(containerPath, PendingOperationType.F_OPERATION_START_TIMESTAMP, now));
            }
        }
    }

    /**
     * Creates deltas that marks all "RETRY"-typed pending operations as completed.
     *
     * This is very simple code that essentially works only for postponed operations (retries).
     * TODO: better support for async and manual operations
     */
    List<ItemDelta<?, ?>> cancelAllPendingOperations(ShadowType repoShadow) throws SchemaException {
        List<ItemDelta<?, ?>> shadowDeltas = new ArrayList<>();
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        for (PendingOperationType pendingOperation : repoShadow.getPendingOperation()) {
            if (pendingOperation.getExecutionStatus() == COMPLETED) {
                continue;
            }
            if (pendingOperation.getType() != PendingOperationTypeType.RETRY) {
                // Other operations are not cancellable now
                continue;
            }
            ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();
            shadowDeltas.addAll(
                    prismContext.deltaFor(ShadowType.class)
                            .item(containerPath.append(PendingOperationType.F_EXECUTION_STATUS))
                            .replace(COMPLETED)
                            .item(containerPath.append(PendingOperationType.F_COMPLETION_TIMESTAMP))
                            .replace(now)
                            .item(containerPath.append(PendingOperationType.F_RESULT_STATUS))
                            .replace(OperationResultStatusType.NOT_APPLICABLE)
                            .asItemDeltas());
        }
        return shadowDeltas;
    }

    /**
     * The goal of this operation is to _atomically_ store the pending operation into the shadow.
     *
     * If there is a conflicting pending operation there, we may return it: depending on the situation (see the code).
     * The repo shadow in opState is updated.
     *
     * BEWARE: updated repo shadow is raw. ApplyDefinitions must be called on it before any serious use.
     *
     * In the future we may perhaps use the newer {@link RepositoryService#modifyObjectDynamically(Class, String, Collection,
     * RepositoryService.ModificationsSupplier, RepoModifyOptions, OperationResult)} instead of the optimistic locking runner.
     */
    PendingOperationType checkAndRecordPendingOperationBeforeExecution(
            @NotNull ProvisioningContext ctx,
            @NotNull ObjectDelta<ShadowType> proposedDelta,
            @NotNull ProvisioningOperationState<?> opState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException {
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

        OptimisticLockingRunner<ShadowType, PendingOperationType> runner =
                new OptimisticLockingRunner.Builder<ShadowType, PendingOperationType>()
                        .object(opState.getRepoShadow().asPrismObject())
                        .result(result)
                        .repositoryService(repositoryService)
                        .maxNumberOfAttempts(10)
                        .delayRange(20)
                        .build();

        try {

            return runner.run(
                    (object) -> {

                        // The runner itself could have updated the shadow (in case of precondition violation).
                        opState.setRepoShadow(
                                runner.getObject().asObjectable());

                        if (avoidDuplicateOperations) {
                            PendingOperationType existingPendingOperation =
                                    findEquivalentPendingOperation(object.asObjectable(), proposedDelta);
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

                        PendingOperationType currentPendingOperation;
                        try {
                            currentPendingOperation =
                                    recordRequestedPendingOperationDelta(
                                            object, proposedDelta, opState, object.getVersion(), result);
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

    private @NotNull PendingOperationType recordRequestedPendingOperationDelta(
            PrismObject<ShadowType> shadow,
            ObjectDelta<ShadowType> pendingDelta,
            @NotNull ProvisioningOperationState<?> opState,
            String currentObjectVersion,
            OperationResult result) throws SchemaException, ObjectNotFoundException, PreconditionViolationException {

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
            repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, precondition, null, result);
        } catch (ObjectAlreadyExistsException e) {
            // should not happen
            throw new SystemException(e);
        }

        // We have to re-read shadow here. We need to get the pending operation in a form as it was stored.
        // We need id in the operation. Otherwise we won't be able to update it.
        ShadowType updatedShadow = repositoryService
                .getObject(ShadowType.class, shadow.getOid(), null, result)
                .asObjectable();
        opState.setRepoShadow(updatedShadow);
        return requireNonNull(
                findEquivalentPendingOperation(updatedShadow, pendingDelta),
                "Cannot find my own operation " + pendingOperation + " in " + updatedShadow);
    }
}
