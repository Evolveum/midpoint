/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.getGroupingInterval;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTION_PENDING;

import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.util.ShadowUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
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
class ShadowOperationPropagationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowOperationPropagationHelper.class);

    @Autowired private Clock clock;
    @Autowired private ProvisioningContextFactory ctxFactory;

    void propagateOperations(
            @NotNull ResourceType resource,
            @NotNull ShadowType rawRepoShadow,
            @NotNull Task task,
            @NotNull OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
            SecurityViolationException, PolicyViolationException, EncryptionException {

        Duration operationGroupingInterval = getGroupingInterval(resource);
        if (operationGroupingInterval == null) {
            LOGGER.warn("Skipping propagation of {} because no there is no operationGroupingInterval defined in resource",
                    rawRepoShadow);
            return;
        }

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        List<PendingOperationType> execPendingOperations = rawRepoShadow.getPendingOperation().stream()
                .filter(op -> op.getExecutionStatus() == EXECUTION_PENDING)
                .collect(Collectors.toList());

        if (execPendingOperations.isEmpty()) {
            LOGGER.debug("Skipping propagation of {} because there are no pending executions", rawRepoShadow);
            return;
        }
        if (!isPropagationTriggered(execPendingOperations, operationGroupingInterval, now)) {
            LOGGER.debug("Skipping propagation of {} because no pending operation triggered propagation", rawRepoShadow);
            return;
        }
        LOGGER.debug("Propagating {} pending operations in {}", execPendingOperations.size(), rawRepoShadow);

        List<PendingOperationType> sortedOperations = ShadowUtil.sortPendingOperations(execPendingOperations);

        ProvisioningContext ctx = ctxFactory.createForShadow(rawRepoShadow, task, result);
        ctx.setPropagation(true);
        RepoShadow repoShadow = ctx.adoptRepoShadow(rawRepoShadow);
        ObjectDelta<ShadowType> aggregateDelta = computeAggregatedDelta(ctx, sortedOperations);

        LOGGER.trace("Merged operation for {}:\n{} ", repoShadow, aggregateDelta.debugDumpLazily(1));

        if (aggregateDelta.isAdd()) {
            ShadowType shadowToAdd = aggregateDelta.getObjectToAdd().asObjectable();
            ResourceObject objectToAdd = ResourceObject.fromBean(shadowToAdd, false, ctx.getObjectDefinitionRequired());
            ShadowAddOperation.executeInPropagation(ctx, repoShadow, objectToAdd, sortedOperations, result);
        } else if (aggregateDelta.isModify()) {
            ShadowModifyOperation.executeInPropagation(ctx, repoShadow, aggregateDelta.getModifications(), sortedOperations, result);
        } else if (aggregateDelta.isDelete()) {
            ShadowDeleteOperation.executeInPropagation(ctx, repoShadow, sortedOperations, result);
        } else {
            throw new IllegalStateException("Delta from outer space: " + aggregateDelta);
        }
        // do we need to modify exists/dead flags?
    }

    private @NotNull ObjectDelta<ShadowType> computeAggregatedDelta(
            @NotNull ProvisioningContext ctx,
            @NotNull List<PendingOperationType> sortedOperations)
            throws SchemaException, ConfigurationException {
        ObjectDelta<ShadowType> aggregateDelta = null;
        for (PendingOperationType pendingOperation : sortedOperations) {
            ObjectDelta<ShadowType> pendingDelta = DeltaConvertor.createObjectDelta(pendingOperation.getDelta());
            ctx.applyAttributesDefinition(pendingDelta);
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
