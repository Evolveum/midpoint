/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.activity.run.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.ActivityTreeStateOverview;
import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Handler for tasks that are based on activity (activities) definition.
 */
@Component
public class ActivityBasedTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/activity-based/handler-3";

    /**
     * Runs (instances) of the current task handler. Used to delegate {@link #heartbeat(Task)} method calls.
     * Note: the future of this method is unclear.
     */
    @NotNull private final Map<String, ActivityBasedTaskRun> currentTaskRuns = Collections.synchronizedMap(new HashMap<>());

    /**
     * Should we avoid auto-assigning task archetypes based on activity handler?
     * This is useful for restricted environments (like in tests) when there are no archetypes present.
     */
    private boolean avoidAutoAssigningArchetypes;

    /** Common beans */
    @Autowired private CommonTaskBeans beans;
    @Autowired private TaskManager taskManager;

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
     * We basically delegate all the processing to a {@link TaskRun} object.
     */
    @Override
    public TaskRunResult run(@NotNull RunningTask localCoordinatorTask)
            throws TaskException {
        ActivityBasedTaskRun taskRun = new ActivityBasedTaskRun(localCoordinatorTask, this);
        try {
            registerTaskRun(localCoordinatorTask, taskRun);
            return taskRun.run(localCoordinatorTask.getResult());
        } finally {
            unregisterTaskRun(localCoordinatorTask);
        }
    }

    @Override
    public @NotNull StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromStoredValues(); // these are cleared in ActivityTreePurger
    }

    /** TODO decide what to do with this method. */
    private ActivityBasedTaskRun getCurrentTaskRun(Task task) {
        return currentTaskRuns.get(task.getOid());
    }

    /** TODO decide what to do with this method. */
    @Override
    public Long heartbeat(Task task) {
        // Delegate heartbeat to the result handler
        TaskRun taskRun = getCurrentTaskRun(task);
        if (taskRun != null) {
            return taskRun.heartbeat();
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
    private void registerTaskRun(RunningTask localCoordinatorTask, ActivityBasedTaskRun taskRun) {
        currentTaskRuns.put(localCoordinatorTask.getOid(), taskRun);
    }

    /** TODO decide what to do with this method. */
    private void unregisterTaskRun(RunningTask localCoordinatorTask) {
        currentTaskRuns.remove(localCoordinatorTask.getOid());
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
    public void onNodeDown(@NotNull TaskType taskBean, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        Task task = taskManager.createTaskInstance(taskBean.asPrismObject(), result);
        ParentAndRoot parentAndRoot = task.getParentAndRoot(result);
        new NodeDownCleaner(task, parentAndRoot.parent, parentAndRoot.root, beans)
                .execute(result);
    }

    @Override
    public void onTaskStalled(@NotNull RunningTask task, long stalledSince, @NotNull OperationResult result) throws CommonException {
        if (task.isPersistent()) {
            new ActivityTreeStateOverview(task.getRootTask(), beans)
                    .markTaskStalled(task.getOid(), stalledSince, result);
        }
    }
}
