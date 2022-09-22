/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.Collection;

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

    void addPendingOperationAdd(
            PrismObject<ShadowType> repoShadow,
            PrismObject<ShadowType> resourceShadow,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState, String asyncOperationReference)
            throws SchemaException {

        ShadowType repoShadowType = repoShadow.asObjectable();
        PendingOperationType pendingOperation = createPendingOperation(resourceShadow.createAddDelta(), opState, asyncOperationReference);
        repoShadowType.getPendingOperation().add(pendingOperation);
        opState.addPendingOperation(pendingOperation);
        repoShadowType.setExists(false);
    }

    PendingOperationType createPendingOperation(
            ObjectDelta<ShadowType> requestDelta,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            String asyncOperationReference) throws SchemaException {
        ObjectDeltaType deltaType = DeltaConvertor.toObjectDeltaType(requestDelta);
        PendingOperationType pendingOperation = new PendingOperationType();
        pendingOperation.setType(opState.getOperationType());
        pendingOperation.setDelta(deltaType);
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        pendingOperation.setRequestTimestamp(now);
        if (PendingOperationExecutionStatusType.EXECUTING.equals(opState.getExecutionStatus())) {
            pendingOperation.setOperationStartTimestamp(now);
        }
        pendingOperation.setExecutionStatus(opState.getExecutionStatus());
        pendingOperation.setResultStatus(opState.getResultStatusType());
        if (opState.getAttemptNumber() != null) {
            pendingOperation.setAttemptNumber(opState.getAttemptNumber());
            pendingOperation.setLastAttemptTimestamp(now);
        }
        if (asyncOperationReference != null) {
            pendingOperation.setAsynchronousOperationReference(asyncOperationReference);
        } else {
            pendingOperation.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
        }
        return pendingOperation;
    }

    void collectPendingOperationUpdates(Collection<ItemDelta<?, ?>> shadowChanges,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            OperationResultStatus implicitStatus,
            XMLGregorianCalendar now) {

        PrismContainerDefinition<PendingOperationType> containerDefinition =
                opState.getRepoShadow().getDefinition().findContainerDefinition(ShadowType.F_PENDING_OPERATION);

        OperationResultStatus opStateResultStatus = MiscUtil.getFirstNonNull(opState.getResultStatus(), implicitStatus);
        OperationResultStatusType opStateResultStatusType =
                opStateResultStatus != null ? opStateResultStatus.createStatusType() : null;
        String asynchronousOperationReference = opState.getAsynchronousOperationReference();

        for (PendingOperationType pendingOperation : opState.getPendingOperations()) {
            if (pendingOperation.asPrismContainerValue().getId() == null) {
                // This must be a new operation
                ContainerDelta<PendingOperationType> cdelta = prismContext.deltaFactory().container().create(
                        ShadowType.F_PENDING_OPERATION, containerDefinition);
                cdelta.addValuesToAdd(pendingOperation.asPrismContainerValue().clone());
                shadowChanges.add(cdelta);
            } else {
                ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

                if (!opState.getExecutionStatus().equals(pendingOperation.getExecutionStatus())) {
                    PropertyDelta<PendingOperationExecutionStatusType> executionStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
                            PendingOperationType.F_EXECUTION_STATUS, opState.getExecutionStatus());
                    shadowChanges.add(executionStatusDelta);

                    if (opState.getExecutionStatus().equals(PendingOperationExecutionStatusType.EXECUTING) && pendingOperation.getOperationStartTimestamp() == null) {
                        PropertyDelta<XMLGregorianCalendar> timestampDelta = createPendingOperationDelta(containerDefinition, containerPath,
                                PendingOperationType.F_OPERATION_START_TIMESTAMP, now);
                        shadowChanges.add(timestampDelta);
                    }

                    if (opState.getExecutionStatus().equals(PendingOperationExecutionStatusType.COMPLETED) && pendingOperation.getCompletionTimestamp() == null) {
                        PropertyDelta<XMLGregorianCalendar> completionTimestampDelta = createPendingOperationDelta(containerDefinition, containerPath,
                                PendingOperationType.F_COMPLETION_TIMESTAMP, now);
                        shadowChanges.add(completionTimestampDelta);
                    }
                }

                if (pendingOperation.getRequestTimestamp() == null) {
                    // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                    // Therefore imprecise timestamp is better than no timestamp.
                    PropertyDelta<XMLGregorianCalendar> timestampDelta = createPendingOperationDelta(containerDefinition, containerPath,
                            PendingOperationType.F_REQUEST_TIMESTAMP, now);
                    shadowChanges.add(timestampDelta);
                }

                if (opStateResultStatusType == null) {
                    if (pendingOperation.getResultStatus() != null) {
                        PropertyDelta<OperationResultStatusType> resultStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
                                PendingOperationType.F_RESULT_STATUS, null);
                        shadowChanges.add(resultStatusDelta);
                    }
                } else {
                    if (!opStateResultStatusType.equals(pendingOperation.getResultStatus())) {
                        PropertyDelta<OperationResultStatusType> resultStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
                                PendingOperationType.F_RESULT_STATUS, opStateResultStatusType);
                        shadowChanges.add(resultStatusDelta);
                    }
                }

                if (asynchronousOperationReference != null && !asynchronousOperationReference.equals(pendingOperation.getAsynchronousOperationReference())) {
                    PropertyDelta<String> executionStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
                            PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, asynchronousOperationReference);
                    shadowChanges.add(executionStatusDelta);
                }

                if (opState.getOperationType() != null && !opState.getOperationType().equals(pendingOperation.getType())) {
                    PropertyDelta<PendingOperationTypeType> executionStatusDelta = createPendingOperationDelta(containerDefinition, containerPath,
                            PendingOperationType.F_TYPE, opState.getOperationType());
                    shadowChanges.add(executionStatusDelta);
                }
            }
        }

    }

    private <T> PropertyDelta<T> createPendingOperationDelta(PrismContainerDefinition<PendingOperationType> containerDefinition, ItemPath containerPath, QName propName, T valueToReplace) {
        PrismPropertyDefinition<T> propDef = containerDefinition.findPropertyDefinition(ItemName.fromQName(propName));
        PropertyDelta<T> propDelta = prismContext.deltaFactory().property().create(containerPath.append(propName), propDef);
        if (valueToReplace == null) {
            propDelta.setValueToReplace();
        } else {
            propDelta.setRealValuesToReplace(valueToReplace);
        }
        return propDelta;
    }

    PendingOperationType findExistingPendingOperation(PrismObject<ShadowType> currentShadow, ObjectDelta<ShadowType> proposedDelta, boolean processInProgress) throws SchemaException {
        for (PendingOperationType pendingOperation : currentShadow.asObjectable().getPendingOperation()) {
            OperationResultStatusType resultStatus = pendingOperation.getResultStatus();
            if (!isInProgressOrRequested(resultStatus, processInProgress)) {
                continue;
            }
            ObjectDeltaType deltaType = pendingOperation.getDelta();
            if (deltaType == null) {
                continue;
            }
            ObjectDelta<Objectable> delta = DeltaConvertor.createObjectDelta(deltaType, prismContext);
            if (!matchPendingDelta(delta, proposedDelta)) {
                continue;
            }
            return pendingOperation;
        }
        return null;
    }

    private boolean isInProgressOrRequested(OperationResultStatusType resultStatus, boolean processInProgress) {
        if (resultStatus == null) {
            return true;
        }
        if (processInProgress && resultStatus == OperationResultStatusType.IN_PROGRESS) {
            return true;
        }
        return false;
    }

    private boolean matchPendingDelta(ObjectDelta<Objectable> pendingDelta, ObjectDelta<ShadowType> proposedDelta) {
        return pendingDelta.equivalent(proposedDelta);
    }

}
