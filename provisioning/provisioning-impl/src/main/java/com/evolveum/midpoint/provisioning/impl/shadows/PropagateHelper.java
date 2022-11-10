/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Implements operations propagation.
 */
@Component
@Experimental
class PropagateHelper {

    private static final Trace LOGGER = TraceManager.getTrace(PropagateHelper.class);

    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private DefinitionsHelper definitionsHelper;
    @Autowired private ShadowAddHelper addHelper;
    @Autowired private ModifyHelper modifyHelper;
    @Autowired private DeleteHelper deleteHelper;

    void propagateOperations(
            ResourceType resource,
            ShadowType shadow,
            Task task,
            OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
            SecurityViolationException, PolicyViolationException, EncryptionException {

        ResourceConsistencyType resourceConsistencyType = resource.getConsistency();
        if (resourceConsistencyType == null) {
            LOGGER.warn("Skipping propagation of {} because no there is no consistency definition in resource", shadow);
            return;
        }
        Duration operationGroupingInterval = resourceConsistencyType.getOperationGroupingInterval();
        if (operationGroupingInterval == null) {
            LOGGER.warn("Skipping propagation of {} because no there is no operationGroupingInterval defined in resource", shadow);
            return;
        }
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        List<PendingOperationType> pendingExecutionOperations = new ArrayList<>();
        boolean triggered = false;
        for (PendingOperationType pendingOperation : shadow.getPendingOperation()) {
            PendingOperationExecutionStatusType executionStatus = pendingOperation.getExecutionStatus();
            if (executionStatus == PendingOperationExecutionStatusType.EXECUTION_PENDING) {
                pendingExecutionOperations.add(pendingOperation);
                if (isPropagationTriggered(pendingOperation, operationGroupingInterval, now)) {
                    triggered = true;
                }
            }
        }
        if (!triggered) {
            LOGGER.debug("Skipping propagation of {} because no pending operation triggered propagation", shadow);
            return;
        }
        if (pendingExecutionOperations.isEmpty()) {
            LOGGER.debug("Skipping propagation of {} because there are no pending executions", shadow);
            return;
        }
        LOGGER.debug("Propagating {} pending operations in {} ", pendingExecutionOperations.size(), shadow);

        ObjectDelta<ShadowType> operationDelta = null;
        List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(pendingExecutionOperations);
        for (PendingOperationType pendingOperation: sortedOperations) {
            ObjectDeltaType pendingDeltaType = pendingOperation.getDelta();
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingDeltaType, prismContext);
            definitionsHelper.applyDefinition(pendingDelta, shadow, task, result);
            if (operationDelta == null) {
                operationDelta = pendingDelta;
            } else {
                operationDelta.merge(pendingDelta);
            }
        }
        assert operationDelta != null; // there is at least one pending operation

        ProvisioningContext ctx = ctxFactory.createForShadow(shadow, task, result);
        ctx.setPropagation(true);
        ctx.applyAttributesDefinition(shadow);
        ctx.applyAttributesDefinition(operationDelta);
        LOGGER.trace("Merged operation for {}:\n{} ", shadow, operationDelta.debugDumpLazily(1));

        if (operationDelta.isAdd()) {
            ShadowType shadowToAdd = operationDelta.getObjectToAdd().asObjectable();
            ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> opState =
                    ProvisioningOperationState.fromPendingOperations(shadow, sortedOperations);
            shadowToAdd.setOid(shadow.getOid());
            addHelper.executeAddShadowAttempt(ctx, shadowToAdd, null, opState, null, result);
            opState.determineExecutionStatusFromResult();

            shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);

            addHelper.notifyAfterAdd(ctx, opState.getAsyncResult().getReturnValue(), opState, task, result);

        } else if (operationDelta.isModify()) {
            Collection<? extends ItemDelta<?,?>> modifications = operationDelta.getModifications();
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue<?>>>>> opState =
                    modifyHelper.executeResourceModify(ctx, shadow, modifications, null, null, now, result);
            opState.determineExecutionStatusFromResult();

            shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);

            modifyHelper.notifyAfterModify(ctx, shadow, modifications, opState, result);

        } else if (operationDelta.isDelete()) {
            ProvisioningOperationState<AsynchronousOperationResult> opState =
                    deleteHelper.executeResourceDelete(ctx, shadow, null, null, task, result);
            opState.determineExecutionStatusFromResult();

            shadowManager.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);

            deleteHelper.notifyAfterDelete(ctx, shadow, opState, result);

        } else {
            throw new IllegalStateException("Delta from outer space: "+operationDelta);
        }

        // do we need to modify exists/dead flags?

    }

    private boolean isPropagationTriggered(PendingOperationType pendingOperation, Duration operationGroupingInterval, XMLGregorianCalendar now) {
        XMLGregorianCalendar requestTimestamp = pendingOperation.getRequestTimestamp();
        if (requestTimestamp == null) {
            return false;
        }
        return XmlTypeConverter.isAfterInterval(requestTimestamp, operationGroupingInterval, now);
    }

}
