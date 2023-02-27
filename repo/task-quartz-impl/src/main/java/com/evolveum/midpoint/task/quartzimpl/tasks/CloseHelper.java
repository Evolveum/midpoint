/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.tasks;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;

@Component
class CloseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SuspendAndDeleteHelper.class);

    @Autowired private LocalScheduler localScheduler;
    @Autowired private PrismContext prismContext;
    @Autowired private UnpauseHelper unpauseHelper;

    public void closeTask(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        try {
            closeInTask((TaskQuartzImpl) task, result);
        } finally {
            if (task.isPersistent()) {
                localScheduler.deleteTaskFromQuartz(task.getOid(), false, result);
                unpauseHelper.unpauseDependentsIfPossible((TaskQuartzImpl) task, result);
            }
        }
    }

    private void updateTaskResult(Task task) {
        OperationResult taskResult = task.getResult();
        if (taskResult == null) {
            return; // shouldn't occur
        }

        // this is a bit of magic to ensure closed tasks will not stay with IN_PROGRESS result
        // (and, if possible, also not with UNKNOWN)
        if (taskResult.getStatus() == IN_PROGRESS || taskResult.getStatus() == UNKNOWN) {
            taskResult.computeStatus();
            if (taskResult.getStatus() == IN_PROGRESS) {
                taskResult.setStatus(SUCCESS);
            }
        }

        // TODO Clean up the result before updating (summarize, remove minor operations - maybe deeply?) - see e.g. MID-7830
        // Update result and result status in prism.
        task.setResult(taskResult);
    }

    /** Executes close operation in the repository task object, i.e. with no regards of Quartz. */
    private void closeInTask(TaskQuartzImpl task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER.trace("Closing task in repository: {}", task);
        updateTaskResult(task);
        task.setExecutionState(TaskExecutionStateType.CLOSED);
        task.setSchedulingState(TaskSchedulingStateType.CLOSED);
        task.setCompletionTimestamp(System.currentTimeMillis());
        Duration cleanupAfterCompletion = task.getCleanupAfterCompletion();
        if (cleanupAfterCompletion != null) {
            TriggerType trigger = new TriggerType()
                    .timestamp(XmlTypeConverter.fromNow(cleanupAfterCompletion))
                    .handlerUri(SchemaConstants.COMPLETED_TASK_CLEANUP_TRIGGER_HANDLER_URI);
            task.addTrigger(trigger); // we just ignore any other triggers (they will do nothing if launched too early)
        }
        try {
            task.flushPendingModifications(result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException(e);
        }
    }
}
