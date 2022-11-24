/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskDeletionListener;
import com.evolveum.midpoint.task.api.TaskListener;

import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskUpdatedListener;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains task listeners.
 *
 * TODO finish review of this class
 */
@Component
public class TaskListenerRegistry {

    private static final Trace LOGGER = TraceManager.getTrace(TaskListenerRegistry.class);

    private final Set<TaskListener> taskListeners = ConcurrentHashMap.newKeySet();

    private final Set<TaskUpdatedListener> taskUpdatedListeners = ConcurrentHashMap.newKeySet();

    private final Set<TaskDeletionListener> taskDeletionListeners = ConcurrentHashMap.newKeySet();

    void registerTaskListener(TaskListener taskListener) {
        taskListeners.add(taskListener);
    }

    void unregisterTaskListener(TaskListener taskListener) {
        taskListeners.remove(taskListener);
    }

    void registerTaskUpdatedListener(TaskUpdatedListener listener) {
        taskUpdatedListeners.add(listener);
    }

    void unregisterTaskUpdatedListener(TaskUpdatedListener listener) {
        taskUpdatedListeners.remove(listener);
    }

    public void notifyTaskStart(Task task, OperationResult result) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskStart(task, result);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    private void logListenerException(RuntimeException e) {
        LoggingUtils.logUnexpectedException(LOGGER, "Task listener returned an unexpected exception", e);
    }

    public void notifyTaskFinish(Task task, TaskRunResult runResult, OperationResult result) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskFinish(task, runResult, result);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    public void notifyTaskThreadStart(Task task, boolean isRecovering, OperationResult result) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskThreadStart(task, isRecovering, result);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    public void notifyTaskThreadFinish(Task task, OperationResult result) {
        for (TaskListener taskListener : taskListeners) {
            try {
                taskListener.onTaskThreadFinish(task, result);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }

    void registerTaskDeletionListener(TaskDeletionListener listener) {
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

    public void notifyTaskUpdated(TaskQuartzImpl task, OperationResult result) {
        for (TaskUpdatedListener listener : taskUpdatedListeners) {
            try {
                listener.onTaskUpdated(task, result);
            } catch (RuntimeException e) {
                logListenerException(e);
            }
        }
    }
}
