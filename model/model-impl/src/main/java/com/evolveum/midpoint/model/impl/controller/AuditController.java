/*
 * Copyright (C) 2016-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.audit.api.AuditResultHandler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.common.util.AuditHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * @author semancik
 */
@Component
public class AuditController implements ModelAuditService {

    private static final Trace LOGGER = TraceManager.getTrace(AuditController.class);

    @Autowired private AuditService auditService;
    @Autowired private AuditHelper auditHelper;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private PrismContext prismContext;

    @Override
    public void audit(AuditEventRecord record, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorize(ModelAuthorizationAction.AUDIT_RECORD, task, result);
        auditHelper.audit(record, null, task, result);
    }

    public @NotNull SearchResultList<AuditEventRecordType> searchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorize(ModelAuthorizationAction.AUDIT_READ, task, parentResult);
        return auditService.searchObjects(query, options, parentResult);
    }

    @Override
    public void searchObjectsIterative(@Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull AuditResultHandler handler,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorize(ModelAuthorizationAction.AUDIT_READ, task, parentResult);
        auditService.searchObjectsIterative(query, handler, options, parentResult);
    }

    @Override
    public int countObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException,
            CommunicationException, ConfigurationException, ObjectNotFoundException {
        authorize(ModelAuthorizationAction.AUDIT_READ, task, parentResult);
        return auditService.countObjects(query, options, parentResult);
    }

    @Override
    public void cleanupAudit(CleanupPolicyType policy, Task task, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        authorize(ModelAuthorizationAction.AUDIT_MANAGE, task, parentResult);
        auditService.cleanupAudit(policy, parentResult);
    }

    @Override
    public boolean supportsRetrieval() {
        return auditService.supportsRetrieval();
    }

    @Override
    public <O extends ObjectType> PrismObject<O> reconstructObject(
            Class<O> type, String oid, String eventIdentifier, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        // TODO: authorizations

        O currentObjectType = objectResolver.getObjectSimple(type, oid, null, task, result);
        //noinspection unchecked
        PrismObject<O> currentObject = (PrismObject<O>) currentObjectType.asPrismObject();

        List<AuditEventRecordType> changeTrail =
                new ArrayList<>( // we later modify this list, so we need to make it mutable here
                        getChangeTrail(oid, eventIdentifier, result));

        LOGGER.trace("Found change trail for {} containing {} events", oid, changeTrail.size());

        LOGGER.debug("TRAIL:\n{}", DebugUtil.debugDumpLazily(changeTrail, 1));

        PrismObject<O> objectFromLastEvent =
                getObjectFromLastEvent(currentObject, changeTrail, eventIdentifier);
        if (objectFromLastEvent != null) {
            return objectFromLastEvent;
        }

        return rollBackTime(currentObject.clone(), changeTrail);
    }

    private List<AuditEventRecordType> getChangeTrail(
            String targetOid, String finalEventIdentifier, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        AuditEventRecordType finalEvent = findEvent(finalEventIdentifier, result);
        if (finalEvent == null) {
            throw new ObjectNotFoundException(
                    "Audit event ID " + finalEventIdentifier + " was not found",
                    AuditEventRecordType.class,
                    finalEventIdentifier);
        }

        LOGGER.trace("Final event:\n{}", finalEvent.debugDumpLazily(1));

        return auditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_REPO_ID).ge(finalEvent.getRepoId())
                        .and()
                        .item(AuditEventRecordType.F_TARGET_REF).ref(targetOid)
                        .and()
                        .item(AuditEventRecordType.F_EVENT_STAGE).eq(AuditEventStageType.EXECUTION)
                        .desc(AuditEventRecordType.F_REPO_ID)
                        .build(),
                null, result);
    }

    private AuditEventRecordType findEvent(String eventIdentifier, OperationResult result)
            throws SchemaException {
        SearchResultList<AuditEventRecordType> listRecords = auditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_EVENT_IDENTIFIER).eq(eventIdentifier)
                        .build(),
                null, result);
        if (listRecords.isEmpty()) {
            return null;
        }
        if (listRecords.size() > 1) {
            LOGGER.error("Found " + listRecords.size() + " audit records for event ID "
                    + eventIdentifier + " (expecting just one)");
        }
        return listRecords.get(0);
    }

    /**
     * Side effect: removes the last event.
     *
     * The reason is that we don't want to use it when rolling back the time later in
     * {@link #rollBackTime(PrismObject, List)}. It is the reference event that we want to
     * know the object state on.
     */
    private <O extends ObjectType> PrismObject<O> getObjectFromLastEvent(
            PrismObject<O> object, List<AuditEventRecordType> changeTrail, String eventIdentifier) {
        if (changeTrail.isEmpty()) {
            return object;
        }
        AuditEventRecordType lastEvent = changeTrail.remove(changeTrail.size() - 1);
        if (!eventIdentifier.equals(lastEvent.getEventIdentifier())) {
            throw new IllegalStateException("Wrong last event identifier, expected "
                    + eventIdentifier + " but was " + lastEvent.getEventIdentifier());
        }

        List<ObjectDeltaOperationType> lastEventDeltasOperations = lastEvent.getDelta();
        for (ObjectDeltaOperationType lastEventDeltasOperation : lastEventDeltasOperations) {
            if (!isApplicable(lastEventDeltasOperation, object, lastEvent)) {
                continue;
            }

            ObjectDeltaType objectDelta = lastEventDeltasOperation.getObjectDelta();
            if (objectDelta.getChangeType() == ChangeTypeType.ADD) {
                // We are lucky. This is object add, so we have complete object there.
                // No need to roll back the operations.
                //noinspection unchecked
                PrismObject<O> objectToAdd = objectDelta.getObjectToAdd().asPrismObject();
                LOGGER.trace("Taking object from add delta in last event {}:\n{}",
                        lastEvent.getEventIdentifier(), objectToAdd.debugDumpLazily(1));
                return objectToAdd;
            }
        }
        return null;
    }

    private <O extends ObjectType> PrismObject<O> rollBackTime(
            PrismObject<O> object, List<AuditEventRecordType> changeTrail)
            throws SchemaException {
        for (AuditEventRecordType event : changeTrail) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Applying event {} ({})", event.getEventIdentifier(), event.getTimestamp());
            }
            List<ObjectDeltaOperationType> deltaOperations = event.getDelta();
            for (ObjectDeltaOperationType deltaOperation : deltaOperations) {
                if (!isApplicable(deltaOperation, object, event)) {
                    continue;
                }

                ObjectDelta<O> objectDelta = DeltaConvertor.createObjectDelta(
                        deltaOperation.getObjectDelta(), prismContext);
                if (objectDelta.isDelete()) {
                    throw new SchemaException("Delete delta found in the audit trail."
                            + " Object history cannot be reconstructed.");
                }
                if (objectDelta.isAdd()) {
                    throw new SchemaException("Add delta found in the audit trail."
                            + " Object history cannot be reconstructed.");
                }
                ObjectDelta<O> reverseDelta = objectDelta.createReverseDelta();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Applying delta (reverse):\n{}", reverseDelta.debugDump(1));
                }
                reverseDelta.applyTo(object);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object after application of event {} ({}):\n{}",
                        event.getEventIdentifier(), event.getTimestamp(), object.debugDump(1));
            }
        }
        return object;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private <O extends ObjectType> boolean isApplicable(
            ObjectDeltaOperationType lastEventDeltasOperation,
            PrismObject<O> object, AuditEventRecordType lastEvent) {
        OperationResultType executionResult = lastEventDeltasOperation.getExecutionResult();
        ObjectDeltaType objectDelta = lastEventDeltasOperation.getObjectDelta();
        if (executionResult != null
                && executionResult.getStatus() == OperationResultStatusType.FATAL_ERROR) {
            LOGGER.trace("Skipping delta {} in event {} because it is {}",
                    objectDelta, lastEvent.getEventIdentifier(), executionResult.getStatus());
            return false;
        }
        if (!object.getOid().equals(objectDelta.getOid())) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Skipping delta {} in event {} because OID does not match ({} vs {})",
                        objectDelta, lastEvent.getEventIdentifier(), object.getOid(), objectDelta.getOid());
            }
            return false;
        }
        return true;
    }

    private void authorize(ModelAuthorizationAction action, Task task, OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        securityEnforcer.authorize(action.getUrl(), AuthorizationPhaseType.REQUEST,
                AuthorizationParameters.EMPTY, null, task, result);
        securityEnforcer.authorize(action.getUrl(), AuthorizationPhaseType.EXECUTION,
                AuthorizationParameters.EMPTY, null, task, result);
    }
}
