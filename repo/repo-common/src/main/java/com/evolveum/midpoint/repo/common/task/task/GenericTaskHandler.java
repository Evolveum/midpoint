/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;

import com.evolveum.midpoint.repo.common.task.work.BucketingManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.*;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityExecutionRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskActivityStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;

/**
 * Handler for generic tasks, i.e. tasks that are driven by definition of their activities.
 *
 * TODO Consider renaming to `ActivityTaskHandler`
 */
@Component
public class GenericTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(GenericTaskHandler.class);

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/generic/handler-3";

    /**
     * Executions (instances) of the current task handler. Used to delegate {@link #heartbeat(Task)} method calls.
     * Note: the future of this method is unclear.
     */
    @NotNull private final Map<String, GenericTaskExecution> currentTaskExecutions = Collections.synchronizedMap(new HashMap<>());

    /**
     * Should we avoid auto-assigning task archetypes based on activity handler?
     * This is useful for restricted environments (like in tests) when there are no archetypes present.
     */
    private boolean avoidAutoAssigningArchetypes;

    /** Common beans */
    @Autowired private CommonTaskBeans beans;
    @Autowired private TaskManager taskManager;
    @Autowired private BucketingManager bucketingManager;

    @PostConstruct
    public void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
        taskManager.setDefaultHandlerUri(HANDLER_URI);
    }

    @PreDestroy
    public void destroy() {
        taskManager.unregisterHandler(HANDLER_URI);
        taskManager.setDefaultHandlerUri(null);
    }

    public CommonTaskBeans getBeans() {
        return beans;
    }

    @Override
    public @Nullable String getArchetypeOid(@Nullable String handlerUri) {
        if (handlerUri == null) {
            return null;
        } else {
            return beans.activityHandlerRegistry.getArchetypeOid(handlerUri);
        }
    }

    /**
     * Main entry point.
     *
     * We basically delegate all the processing to a TaskExecution object.
     */
    @Override
    public TaskRunResult run(@NotNull RunningTask localCoordinatorTask)
            throws TaskException {
        GenericTaskExecution taskExecution = new GenericTaskExecution(localCoordinatorTask, this);
        try {
            registerExecution(localCoordinatorTask, taskExecution);
            return taskExecution.run(localCoordinatorTask.getResult());
        } finally {
            unregisterExecution(localCoordinatorTask);
        }
    }

    /** TODO decide what to do with this method. */
    private GenericTaskExecution getCurrentTaskExecution(Task task) {
        return currentTaskExecutions.get(task.getOid());
    }

    /** TODO decide what to do with this method. */
    @Override
    public Long heartbeat(Task task) {
        // Delegate heartbeat to the result handler
        TaskExecution execution = getCurrentTaskExecution(task);
        if (execution != null) {
            return execution.heartbeat();
        } else {
            // most likely a race condition.
            return null;
        }
    }

    /** TODO decide what to do with this method. */
    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

    /** TODO decide what to do with this method. */
    private void registerExecution(RunningTask localCoordinatorTask, GenericTaskExecution execution) {
        currentTaskExecutions.put(localCoordinatorTask.getOid(), execution);
    }

    /** TODO decide what to do with this method. */
    private void unregisterExecution(RunningTask localCoordinatorTask) {
        currentTaskExecutions.remove(localCoordinatorTask.getOid());
    }

    public void registerLegacyHandlerUri(String handlerUri) {
        beans.taskManager.registerHandler(handlerUri, this);
    }

    public void unregisterLegacyHandlerUri(String handlerUri) {
        beans.taskManager.unregisterHandler(handlerUri);
    }

    boolean isAvoidAutoAssigningArchetypes() {
        return avoidAutoAssigningArchetypes;
    }

    public void setAvoidAutoAssigningArchetypes(boolean avoidAutoAssigningArchetypes) {
        this.avoidAutoAssigningArchetypes = avoidAutoAssigningArchetypes;
    }

    @Override
    public void cleanupOnNodeDown(@NotNull TaskType taskBean, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        TaskActivityStateType state = taskBean.getActivityState();
        if (state == null || state.getLocalRootActivityExecutionRole() != ActivityExecutionRoleType.WORKER) {
            return;
        }

        Task task = taskManager.createTaskInstance(taskBean.asPrismObject(), result);
        Task parentTask = Objects.requireNonNull(
                task.getParentTask(result), () -> "No parent for worker task " + task);
        ActivityPath activityPath =
                ActivityPath.fromBean(
                        Objects.requireNonNull(
                                state.getLocalRoot(), "No local root in " + task));

        LOGGER.info("Returning all buckets from {} (coordinator {})", task, parentTask);
        bucketingManager.releaseAllWorkBucketsFromWorker(parentTask.getOid(), task.getOid(), activityPath, null, result);
    }
}
