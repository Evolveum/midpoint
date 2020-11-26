/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

@Component
public class AuditReindexTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(AuditReindexTaskHandler.class);

    public static final String HANDLER_URI = ModelPublicConstants.AUDIT_REINDEX_TASK_HANDLER_URI;

    private static final String TASK_NAME = "AuditReindex";

    private static final int BATCH_SIZE = 20;

    @Autowired protected AuditService auditService;
    @Autowired protected TaskManager taskManager;

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(RunningTask coordinatorTask, TaskPartitionDefinitionType partition) {
        OperationResult opResult = new OperationResult(OperationConstants.AUDIT_REINDEX + ".run");
        opResult.setStatus(OperationResultStatus.IN_PROGRESS);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        final long expectedTotal = auditService.countObjects(null, null, opResult);
        AuditResultHandler resultHandler = new AuditResultHandler() {
            private final AtomicInteger processedObjects = new AtomicInteger();

            @Override
            public boolean handle(AuditEventRecord auditRecord) {
                auditService.reindexEntry(auditRecord);
                processedObjects.incrementAndGet();

                return true;
            }

            @Override
            public boolean handle(AuditEventRecordType auditRecord) {
                return true;
            }

            @Override
            public int getProgress() {
                return processedObjects.get();
            }
        };

        try {
            LOGGER.trace("{}: expecting {} objects to be processed", TASK_NAME, expectedTotal);

            coordinatorTask.setProgress(0L);
            coordinatorTask.setExpectedTotal(expectedTotal);
            try {
                coordinatorTask.flushPendingModifications(opResult);
            } catch (ObjectAlreadyExistsException e) { // other exceptions are handled in the outer try block
                throw new IllegalStateException(
                        "Unexpected ObjectAlreadyExistsException when updating task progress/expectedTotal",
                        e);
            }
            Map<String, Object> params = new HashMap<>();
            int firstResult = 0;
            int maxResults = BATCH_SIZE;
            while (true) {
                params.put("setFirstResult", firstResult);
                params.put("setMaxResults", maxResults);
                // TODO MID-6319: to migrate this to searchObjects returning AERType?
                // BUT AERType does NOT contain repository identifier, which would make it
                // complicated or inefficient to work with persistent audit records.
                List<AuditEventRecord> records = auditService.listRecords(null, params, opResult);
                if (CollectionUtils.isNotEmpty(records)) {
                    for (AuditEventRecord record : records) {
                        resultHandler.handle(record);
                        // TODO increase task progress!
                        runResult.setProgress((long) resultHandler.getProgress());
                    }
                    firstResult += maxResults;
                    maxResults = (int) ((expectedTotal - firstResult) > maxResults ? maxResults : (expectedTotal - firstResult));
                } else {
                    break;
                }
            }
            opResult.recordSuccess();

        } catch (ObjectNotFoundException e) {
            // This is bad. The resource does not exist. Permanent problem.
            logErrorAndSetResult(runResult, resultHandler, "Object not found", e,
                    OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } catch (SchemaException e) {
            // Not sure about this. But most likely it is a misconfigured
            // resource or connector
            // It may be worth to retry. Error is fatal, but may not be
            // permanent.
            logErrorAndSetResult(runResult, resultHandler, "Error dealing with schema", e,
                    OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
            return runResult;
        } catch (RuntimeException e) {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense
            // to retry.
            logErrorAndSetResult(runResult, resultHandler, "Internal error", e,
                    OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        // TODO: check last handler status

        runResult.setProgress((long) resultHandler.getProgress());
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

        String finishMessage = "Finished " + TASK_NAME + " (" + coordinatorTask + "). ";
        String statistics = "Processed " + resultHandler.getProgress() + " objects";

        opResult.createSubresult(OperationConstants.AUDIT_REINDEX + ".statistics")
                .recordStatus(OperationResultStatus.SUCCESS, statistics);

        LOGGER.info(finishMessage + statistics);

        LOGGER.trace("{} run finished (task {}, run result {})", TASK_NAME, coordinatorTask, runResult);

        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return null;
    }

    @Override
    public void refreshStatus(Task task) {
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    // TODO: copied from abstract search iterative handler
    private void logErrorAndSetResult(TaskRunResult runResult, AuditResultHandler resultHandler,
            String message, Throwable e, OperationResultStatus opStatus, TaskRunResultStatus status) {
        LOGGER.error("{}: {}: {}", TASK_NAME, message, e.getMessage(), e);
        runResult.getOperationResult().recordStatus(opStatus, message + ": " + e.getMessage(), e);
        runResult.setRunResultStatus(status);
        runResult.setProgress((long) resultHandler.getProgress());
    }
}
