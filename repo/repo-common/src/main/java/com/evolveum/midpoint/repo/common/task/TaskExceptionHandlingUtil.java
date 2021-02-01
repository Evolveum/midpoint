/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType.FATAL;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType.PARTIAL;

/**
 * Preliminary (temporary) place for error handling code in tasks.
 */
@Experimental
public class TaskExceptionHandlingUtil {

    /**
     * General method that processes any exceptions from a task.
     */
    public static TaskException convertException(Throwable t, TaskPartitionDefinitionType partition) {
        if (t instanceof TaskException) {
            return (TaskException) t;
        } else if (t instanceof ObjectNotFoundException) {
            // This is bad. The resource or task or something like that does not exist. Permanent problem.
            String oid = ((ObjectNotFoundException) t).getOid();
            return new TaskException("A required object does not exist, OID: " + oid, FATAL_ERROR, getRunStatus(t, partition, FATAL), t);
        } else if (t instanceof MaintenanceException) {
            // Resource is in the maintenance, do not suspend the task
            return new TaskException("Maintenance exception", HANDLED_ERROR, TEMPORARY_ERROR);
        } else if (t instanceof CommunicationException) {
            // Error, but not critical. Just try later.
            return new TaskException("Communication error", PARTIAL_ERROR, getRunStatus(t, partition, PARTIAL), t);
        } else if (t instanceof SchemaException) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            return new TaskException("Error dealing with schema", FATAL_ERROR, getRunStatus(t, partition, PARTIAL), t);
        } else if (t instanceof PolicyViolationException) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            return new TaskException("Policy violation", FATAL_ERROR, getRunStatus(t, partition, PARTIAL), t);
        } else if (t instanceof PreconditionViolationException) {
            // Not sure about this.
            // It may be worth to retry. Error is fatal, but may not be permanent.
            return new TaskException("Internal error", FATAL_ERROR, getRunStatus(t, partition, PARTIAL), t);
        } else if (t instanceof ConfigurationException) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            return new TaskException("Configuration error", FATAL_ERROR, getRunStatus(t, partition, PARTIAL), t);
        } else if (t instanceof SecurityViolationException) {
            return new TaskException("Security violation", FATAL_ERROR, getRunStatus(t, partition, FATAL), t);
        } else if (t instanceof ExpressionEvaluationException) {
            return new TaskException("Expression error", FATAL_ERROR, getRunStatus(t, partition, FATAL), t);
        } else if (t instanceof SystemException) {
            return new TaskException("Unspecified error", FATAL_ERROR, getRunStatus(t, partition, FATAL), t);
        } else {
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            return new TaskException("Internal error", FATAL_ERROR, getRunStatus(t, partition, FATAL), t);
        }
    }

    private static TaskRunResult.TaskRunResultStatus getRunStatus(Throwable ex, TaskPartitionDefinitionType partition, CriticalityType defaultCriticality) {
        CriticalityType criticality;
        if (partition == null) {
            criticality = defaultCriticality;
        } else {
            criticality = ExceptionUtil.getCriticality(partition.getErrorCriticality(), ex, defaultCriticality);
        }

        switch (criticality) {
            case FATAL:
                return PERMANENT_ERROR;
            case PARTIAL:
            default:
                return TEMPORARY_ERROR;
        }
    }

    static <TRR extends TaskRunResult> TRR processFinish(Trace logger,
            TaskPartitionDefinitionType partition, String ctx, TRR runResult, ErrorState errorState) {

        if (errorState.isPermanentErrorEncountered()) {
            return processException(errorState.getPermanentErrorException(), logger, partition, ctx, runResult);
        }
        // TODO what in case of not permanent error?

        OperationResult opResult = runResult.getOperationResult();

        // Normally we compute status if unknown. But for tasks, the root status is IN_PROGRESS by default.
        // So we do the computation even for IN_PROGRESS status.
        if (opResult.isUnknown() || opResult.isInProgress()) {
            opResult.computeStatus();
        }

        runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        return runResult;
    }

    /**
     * TODO TODO TODO
     *
     * Here we do the error handling.
     */
    public static <TRR extends TaskRunResult> TRR processException(Throwable t, Trace logger,
            TaskPartitionDefinitionType partition, String ctx, TRR runResult) {
        TaskException taskException = convertException(t, partition);
        return processTaskException(taskException, logger, ctx, runResult);
    }

    private static <TRR extends TaskRunResult> TRR processTaskException(TaskException e, Trace logger, String ctx, TRR runResult) {
        Throwable cause = e.getCause();
        String causeMessageSuffix = cause != null ? ": " + cause.getMessage() : "";
        String message = e.getMessage() + causeMessageSuffix;
        logger.error("{}: {}", ctx, message, cause);
        runResult.getOperationResult().recordStatus(e.getOpResultStatus(), message, cause);
        runResult.setRunResultStatus(e.getRunResultStatus());
        return runResult;
    }

}
