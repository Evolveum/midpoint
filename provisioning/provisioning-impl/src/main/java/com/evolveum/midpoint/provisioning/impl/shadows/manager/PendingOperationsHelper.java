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
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
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

import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;

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
            ShadowType repoShadow,
            ShadowType resourceShadow,
            ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> opState,
            String asyncOperationReference)
            throws SchemaException {

        PendingOperationType pendingOperation =
                opState.toPendingOperation(
                        resourceShadow.asPrismObject().createAddDelta(),
                        asyncOperationReference,
                        clock.currentTimeXMLGregorianCalendar());
        repoShadow.getPendingOperation().add(pendingOperation);
        opState.addPendingOperation(pendingOperation);
        repoShadow.setExists(false);
    }

    void collectPendingOperationUpdates(
            Collection<ItemDelta<?, ?>> shadowChanges,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            OperationResultStatus implicitStatus,
            XMLGregorianCalendar now) {

        PrismContainerDefinition<PendingOperationType> containerDef =
                opState.getRepoShadow().asPrismObject().getDefinition().findContainerDefinition(ShadowType.F_PENDING_OPERATION);

        OperationResultStatusType opStateResultStatusType =
                createStatusType(
                        MiscUtil.getFirstNonNull(opState.getResultStatus(), implicitStatus));

        // TODO is this correct? What if there are multiple pending operations - will not they get overwritten by the current
        //  opState properties?
        for (PendingOperationType pendingOperation : opState.getPendingOperations()) {
            if (pendingOperation.asPrismContainerValue().getId() == null) {
                // This must be a new operation
                ContainerDelta<PendingOperationType> delta =
                        prismContext.deltaFactory().container().create(ShadowType.F_PENDING_OPERATION, containerDef);
                delta.addValuesToAdd(pendingOperation.asPrismContainerValue().clone());
                shadowChanges.add(delta);
            } else {
                ItemPath containerPath = pendingOperation.asPrismContainerValue().getPath();

                if (opState.getExecutionStatus() != pendingOperation.getExecutionStatus()) {
                    shadowChanges.add(
                            createPendingOperationDelta(
                                    containerDef,
                                    containerPath,
                                    PendingOperationType.F_EXECUTION_STATUS,
                                    opState.getExecutionStatus()));

                    if (opState.getExecutionStatus() == EXECUTING
                            && pendingOperation.getOperationStartTimestamp() == null) {
                        shadowChanges.add(
                                createPendingOperationDelta(
                                        containerDef, containerPath, PendingOperationType.F_OPERATION_START_TIMESTAMP, now));
                    }

                    if (opState.getExecutionStatus() == COMPLETED
                            && pendingOperation.getCompletionTimestamp() == null) {
                        shadowChanges.add(
                                createPendingOperationDelta(
                                        containerDef, containerPath, PendingOperationType.F_COMPLETION_TIMESTAMP, now));
                    }
                }

                if (pendingOperation.getRequestTimestamp() == null) {
                    // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                    // Therefore imprecise timestamp is better than no timestamp.
                    shadowChanges.add(
                            createPendingOperationDelta(
                                    containerDef, containerPath, PendingOperationType.F_REQUEST_TIMESTAMP, now));
                }

                if (pendingOperation.getResultStatus() != opStateResultStatusType) {
                    shadowChanges.add(
                            createPendingOperationDelta(
                                    containerDef, containerPath, PendingOperationType.F_RESULT_STATUS, opStateResultStatusType));
                }

                String asynchronousOperationReference = opState.getAsynchronousOperationReference();
                if (asynchronousOperationReference != null
                        && !asynchronousOperationReference.equals(pendingOperation.getAsynchronousOperationReference())) {
                    shadowChanges.add(
                            createPendingOperationDelta(
                                    containerDef, containerPath, PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, asynchronousOperationReference));
                }

                if (opState.getOperationType() != null && opState.getOperationType() != pendingOperation.getType()) {
                    shadowChanges.add(
                            createPendingOperationDelta(
                                    containerDef, containerPath, PendingOperationType.F_TYPE, opState.getOperationType()));
                }
            }
        }
    }

    private <T> PropertyDelta<T> createPendingOperationDelta(
            PrismContainerDefinition<PendingOperationType> containerDefinition,
            ItemPath containerPath,
            QName propName,
            T valueToReplace) {
        PrismPropertyDefinition<T> propDef = containerDefinition.findPropertyDefinition(ItemName.fromQName(propName));
        PropertyDelta<T> propDelta = prismContext.deltaFactory().property().create(containerPath.append(propName), propDef);
        if (valueToReplace == null) {
            propDelta.setValueToReplace();
        } else {
            //noinspection unchecked
            propDelta.setRealValuesToReplace(valueToReplace);
        }
        return propDelta;
    }

    PendingOperationType findExistingPendingOperation(ShadowType currentShadow, ObjectDelta<ShadowType> proposedDelta)
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
}
