package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskDeletionListener;
import com.evolveum.midpoint.task.api.TaskListener;

import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Maintains task listeners.
 *
 * TODO finish review of this class
 */
@Component
public class TaskListenerRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(TaskListenerRegistry.class);

    private final Set<TaskListener> taskListeners = new HashSet<>();

    private final Set<TaskDeletionListener> taskDeletionListeners = new HashSet<>();

    public void registerTaskListener(TaskListener taskListener) {
        taskListeners.add(taskListener);
    }

    public void unregisterTaskListener(TaskListener taskListener) {
        taskListeners.remove(taskListener);
    }

    public void notifyTaskStart(Task task) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskStart(task);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    private void logListenerException(RuntimeException e) {
        LoggingUtils.logUnexpectedException(LOGGER, "Task listener returned an unexpected exception", e);
    }

    public void notifyTaskFinish(Task task, TaskRunResult runResult) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskFinish(task, runResult);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    public void notifyTaskThreadStart(Task task, boolean isRecovering) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskThreadStart(task, isRecovering);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    public void notifyTaskThreadFinish(Task task) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskThreadFinish(task);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    public void registerTaskDeletionListener(TaskDeletionListener listener) {
        Validate.notNull(listener, "Task deletion listener is null");
        taskDeletionListeners.add(listener);
    }

    public void notifyTaskDeleted(Task task, OperationResult result) {
        for (TaskDeletionListener listener : taskDeletionListeners) {
            try {
                listener.onTaskDelete(task, result);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }
}
