/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.evolveum.midpoint.util.annotation.Experimental;
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
@Experimental
class PendingOperationsHelper {

    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;

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

    void addPendingOperationForExistingShadow(
            Collection<ItemDelta<?, ?>> shadowChanges,
            ProvisioningOperationState<?> opState,
            ObjectDelta<ShadowType> requestDelta,
            XMLGregorianCalendar now) throws SchemaException {
            shadowChanges.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PENDING_OPERATION)
                            .add(opState.toPendingOperation(
                                    requestDelta, null, now))
                            .asItemDelta());
    }

    void collectCurrentPendingOperationUpdates(
            Collection<ItemDelta<?, ?>> shadowChanges,
            ProvisioningOperationState<?> opState,
            XMLGregorianCalendar now) throws SchemaException {
        PendingOperationType shadowPendingOp = opState.getCurrentPendingOperation();
        if (shadowPendingOp != null) {
            PrismContainerValue<?> pendingOpValue = shadowPendingOp.asPrismContainerValue();
            assert pendingOpValue.getId() != null;
            ItemPath pendingOpValuePath = pendingOpValue.getPath();

            PendingOperationExecutionStatusType realExecStatus = opState.getExecutionStatus();
            if (shadowPendingOp.getExecutionStatus() != realExecStatus) {
                shadowChanges.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_EXECUTION_STATUS, realExecStatus));

                if (realExecStatus == EXECUTING
                        && shadowPendingOp.getOperationStartTimestamp() == null) {
                    shadowChanges.add(
                            createPendingOperationDelta(
                                    pendingOpValuePath, PendingOperationType.F_OPERATION_START_TIMESTAMP, now));
                }

                if (realExecStatus == COMPLETED
                        && shadowPendingOp.getCompletionTimestamp() == null) {
                    shadowChanges.add(
                            createPendingOperationDelta(
                                    pendingOpValuePath, PendingOperationType.F_COMPLETION_TIMESTAMP, now));
                }
            }

            if (shadowPendingOp.getRequestTimestamp() == null) {
                // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                // Therefore imprecise timestamp is better than no timestamp.
                shadowChanges.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_REQUEST_TIMESTAMP, now));
            }

            OperationResultStatusType realResultStatus = opState.getResultStatusTypeOrDefault();
            if (shadowPendingOp.getResultStatus() != realResultStatus) {
                shadowChanges.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_RESULT_STATUS, realResultStatus));
            }

            String realAsyncOpRef = opState.getAsynchronousOperationReference();
            if (realAsyncOpRef == null) {
                // Not sure about the reason for this. Can the operation reference be cleared?
            } else if (!Objects.equals(shadowPendingOp.getAsynchronousOperationReference(), realAsyncOpRef)) {
                shadowChanges.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, realAsyncOpRef));
            }

            PendingOperationTypeType realOpType = opState.getOperationType();
            if (realOpType == null) {
                // We will not push the null value to the shadow. The reason is that sometimes the original value of `RETRY`
                // is cleared by the successful execution of the operation.
            } else if (realOpType != shadowPendingOp.getType()) {
                shadowChanges.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_TYPE, realOpType));
            }

            Integer realAttemptNumber = opState.getAttemptNumber();
            if (!Objects.equals(shadowPendingOp.getAttemptNumber(), realAttemptNumber)) {
                shadowChanges.add(
                        createPendingOperationDelta(
                                pendingOpValuePath, PendingOperationType.F_ATTEMPT_NUMBER, realAttemptNumber));
            }

            XMLGregorianCalendar realLastAttemptTimestamp = opState.getLastAttemptTimestamp();
            if (realLastAttemptTimestamp == null) {
                // null means "do not change"
            } else if (!Objects.equals(shadowPendingOp.getLastAttemptTimestamp(), realLastAttemptTimestamp)) {
                shadowChanges.add(
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

    PendingOperationType findPendingAddOperation(ShadowType shadow) {
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

    void collectPendingOperationUpdates(
            List<ItemDelta<?, ?>> shadowDeltas, ProvisioningOperationState<?> opState, XMLGregorianCalendar now)
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
            shadowDeltas.add(
                    createPendingOperationDelta(containerPath, PendingOperationType.F_EXECUTION_STATUS, executionStatus));
            shadowDeltas.add(
                    createPendingOperationDelta(containerPath, PendingOperationType.F_RESULT_STATUS, resultStatus));
            shadowDeltas.add(
                    createPendingOperationDelta(
                            containerPath, PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, asynchronousOpReference));
            if (existingPendingOperation.getRequestTimestamp() == null) {
                // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                // Therefore imprecise timestamp is better than no timestamp.
                shadowDeltas.add(
                        createPendingOperationDelta(containerPath, PendingOperationType.F_REQUEST_TIMESTAMP, now));
            }
            if (executionStatus == COMPLETED && existingPendingOperation.getCompletionTimestamp() == null) {
                shadowDeltas.add(
                        createPendingOperationDelta(containerPath, PendingOperationType.F_COMPLETION_TIMESTAMP, now));
            }
            if (executionStatus == EXECUTING && existingPendingOperation.getOperationStartTimestamp() == null) {
                shadowDeltas.add(
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
}
