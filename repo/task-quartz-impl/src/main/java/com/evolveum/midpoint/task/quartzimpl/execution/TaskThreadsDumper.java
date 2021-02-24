/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.execution;

import java.util.*;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.*;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskRetriever;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Dumps task threads.
 */
@Component
public class TaskThreadsDumper {

    private static final Trace LOGGER = TraceManager.getTrace(TaskThreadsDumper.class);

    private static final String DOT_CLASS = TaskThreadsDumper.class.getName() + ".";
    public static final String OP_GET_RUNNING_TASKS_THREADS_DUMP = DOT_CLASS + "getRunningTasksThreadsDump";
    public static final String OP_GET_TASK_THREADS_DUMP = DOT_CLASS + "getTaskThreadsDump";
    public static final String OP_RECORD_RUNNING_TASKS_THREADS_DUMP = DOT_CLASS + "recordRunningTasksThreadsDump";
    public static final String OP_RECORD_TASK_THREADS_DUMP = DOT_CLASS + "recordTaskThreadsDump";

    @Autowired private TaskRetriever taskRetriever;
    @Autowired private LocalScheduler localScheduler;
    @Autowired private LocalNodeState localNodeState;
    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private RepositoryService repositoryService;

    public String recordRunningTasksThreadsDump(String cause, OperationResult parentResult) throws ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(OP_RECORD_RUNNING_TASKS_THREADS_DUMP);
        try {
            Collection<String> locallyRunningTasksOids = localScheduler.getLocallyRunningTasksOids(result);
            StringBuilder output = new StringBuilder();
            for (String oid : locallyRunningTasksOids) {
                try {
                    output.append(recordTaskThreadsDump(oid, cause, result));
                } catch (SchemaException | ObjectNotFoundException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get task thread dump for {}", e, oid);
                    output.append("Couldn't get task thread dump for ").append(oid).append("\n\n");
                }
            }
            return output.toString();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't record thread dump for running tasks: " + t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public String recordTaskThreadsDump(String taskOid, String cause, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createSubresult(OP_RECORD_TASK_THREADS_DUMP);
        result.addParam("taskOid", taskOid);
        result.addParam("cause", cause);
        try {
            StringBuilder output = new StringBuilder();
            String dump = getTaskThreadsDump(taskOid, result);
            if (dump != null) {
                LOGGER.debug("Thread dump for task {}:\n{}", taskOid, dump);
                DiagnosticInformationType event = new DiagnosticInformationType()
                        .timestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                        .type(SchemaConstants.TASK_THREAD_DUMP_URI)
                        .cause(cause)
                        .nodeIdentifier(configuration.getNodeId())
                        .content(dump);
                repositoryService.addDiagnosticInformation(TaskType.class, taskOid, event, result);
                output.append("Thread dump for task ").append(taskOid).append(" was recorded.\n");
            } else {
                output.append("Thread dump for task ").append(taskOid).append(" was NOT recorded as it couldn't be obtained.\n");
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Unable to get threads dump for task " +
                        taskOid + "; it is probably not running locally.");
            }
            return output.toString();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't take thread dump: " + t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public String getRunningTasksThreadsDump(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_GET_RUNNING_TASKS_THREADS_DUMP);
        try {
            Collection<String> locallyRunningTasksOids = localScheduler.getLocallyRunningTasksOids(result);
            StringBuilder output = new StringBuilder();
            for (String taskOid : locallyRunningTasksOids) {
                try {
                    output.append(getTaskThreadsDump(taskOid, result));
                } catch (SchemaException | ObjectNotFoundException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get task thread dump for {}", e, taskOid);
                    output.append("Couldn't get task thread dump for ").append(taskOid).append("\n\n");
                }
            }
            return output.toString();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't get thread dump for running tasks: " + t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public String getTaskThreadsDump(String taskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_GET_TASK_THREADS_DUMP);
        try {
            StringBuilder output = new StringBuilder();
            TaskQuartzImpl task = taskRetriever.getTaskPlain(taskOid, parentResult);
            RunningTask localTask = localNodeState.getLocallyRunningTaskByIdentifier(task.getTaskIdentifier());
            Thread rootThread = localScheduler.getLocalTaskThread(taskOid);
            if (localTask == null || rootThread == null) {
                result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Task " + task + " is not running locally");
                return null;
            }
            output.append("*** Root thread for task ").append(task).append(":\n\n");
            addTaskInfo(output, localTask, rootThread);
            for (RunningTask subtask : localTask.getLightweightAsynchronousSubtasks()) {
                Thread thread = ((RunningTaskQuartzImpl) subtask).getExecutingThread();
                output.append("** Information for lightweight asynchronous subtask ").append(subtask).append(":\n\n");
                addTaskInfo(output, subtask, thread);
            }
            return output.toString();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't get task threads dump: " + t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void addTaskInfo(StringBuilder output, RunningTask localTask, Thread thread) {
        output.append("Execution state: ").append(localTask.getExecutionState()).append("\n");
        output.append("Progress: ").append(localTask.getProgress());
        if (localTask.getExpectedTotal() != null) {
            output.append(" of ").append(localTask.getExpectedTotal());
        }
        output.append("\n");
        OperationStatsType stats = localTask.getAggregatedLiveOperationStats();
        IterativeTaskInformationType info = stats != null ? stats.getIterativeTaskInformation() : null;
        if (info != null) {
            output.append(IterativeTaskInformation.format(info));
        }
        output.append("\n");
        if (thread != null) {
            output.append(MiscUtil.takeThreadDump(thread));
        } else {
            output.append("(no thread for this task)");
        }
        output.append("\n\n");
    }
}
