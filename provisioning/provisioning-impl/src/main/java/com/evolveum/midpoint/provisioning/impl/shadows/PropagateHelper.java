/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.getGroupingInterval;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTION_PENDING;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.DeleteOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.ModifyOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Implements operations propagation.
 */
@Component
@Experimental
class PropagateHelper {

    private static final Trace LOGGER = TraceManager.getTrace(PropagateHelper.class);

    @Autowired private Clock clock;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired protected ShadowManager shadowManager;
    @Autowired private ProvisioningContextFactory ctxFactory;
    @Autowired private DefinitionsHelper definitionsHelper;
    @Autowired private ShadowAddHelper addHelper;
    @Autowired private ShadowModifyHelper modifyHelper;
    @Autowired private ShadowDeleteHelper deleteHelper;

    void propagateOperations(
            @NotNull ResourceType resource,
            @NotNull ShadowType shadow,
            @NotNull Task task,
            @NotNull OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
            SecurityViolationException, PolicyViolationException, EncryptionException {

        Duration operationGroupingInterval = getGroupingInterval(resource);
        if (operationGroupingInterval == null) {
            LOGGER.warn("Skipping propagation of {} because no there is no operationGroupingInterval defined in resource", shadow);
            return;
        }

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        List<PendingOperationType> execPendingOperations = shadow.getPendingOperation().stream()
                .filter(op -> op.getExecutionStatus() == EXECUTION_PENDING)
                .collect(Collectors.toList());

        if (execPendingOperations.isEmpty()) {
            LOGGER.debug("Skipping propagation of {} because there are no pending executions", shadow);
            return;
        }
        if (!isPropagationTriggered(execPendingOperations, operationGroupingInterval, now)) {
            LOGGER.debug("Skipping propagation of {} because no pending operation triggered propagation", shadow);
            return;
        }
        LOGGER.debug("Propagating {} pending operations in {}", execPendingOperations.size(), shadow);

        List<PendingOperationType> sortedOperations = shadowCaretaker.sortPendingOperations(execPendingOperations);
        ObjectDelta<ShadowType> aggregateDelta = computeAggregatedDelta(sortedOperations, shadow, task, result);

        ProvisioningContext ctx = ctxFactory.createForShadow(shadow, task, result);
        ctx.setPropagation(true);
        ctx.applyAttributesDefinition(shadow);
        ctx.applyAttributesDefinition(aggregateDelta);
        LOGGER.trace("Merged operation for {}:\n{} ", shadow, aggregateDelta.debugDumpLazily(1));

        if (aggregateDelta.isAdd()) {
            ShadowType shadowToAdd = aggregateDelta.getObjectToAdd().asObjectable();
            shadowToAdd.setOid(shadow.getOid());
            AddOperationState opState = new AddOperationState(shadow);
            opState.setPropagatedPendingOperations(sortedOperations);
            addHelper.executeAddAttempt(ctx, shadowToAdd, null, null, opState, result);
        } else if (aggregateDelta.isModify()) {
            Collection<? extends ItemDelta<?,?>> modifications = aggregateDelta.getModifications();
            ModifyOperationState opState = new ModifyOperationState(shadow);
            opState.setPropagatedPendingOperations(sortedOperations);
            modifyHelper.executeModifyAttempt(ctx, modifications, null, null, opState, true, result);
        } else if (aggregateDelta.isDelete()) {
            DeleteOperationState opState = new DeleteOperationState(shadow);
            opState.setPropagatedPendingOperations(sortedOperations);
            deleteHelper.executeDeleteAttempt(ctx, null, null, opState, true, result);
        } else {
            throw new IllegalStateException("Delta from outer space: " + aggregateDelta);
        }
        // do we need to modify exists/dead flags?
    }

    @NotNull
    private ObjectDelta<ShadowType> computeAggregatedDelta(
            @NotNull List<PendingOperationType> sortedOperations,
            @NotNull ShadowType shadow,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ObjectDelta<ShadowType> aggregateDelta = null;
        for (PendingOperationType pendingOperation : sortedOperations) {
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingOperation.getDelta());
            // TODO Shadow is retrieved from the repository here!
            definitionsHelper.applyDefinition(pendingDelta, shadow, task, result);
            if (aggregateDelta == null) {
                aggregateDelta = pendingDelta;
            } else {
                aggregateDelta.merge(pendingDelta);
            }
        }
        assert aggregateDelta != null; // there is at least one pending operation
        return aggregateDelta;
    }

    private boolean isPropagationTriggered(
            List<PendingOperationType> operations, Duration operationGroupingInterval, XMLGregorianCalendar now) {
        return operations.stream()
                .map(PendingOperationType::getRequestTimestamp)
                .anyMatch(
                        timestamp -> timestamp != null
                                && XmlTypeConverter.isAfterInterval(timestamp, operationGroupingInterval, now));
    }
}
