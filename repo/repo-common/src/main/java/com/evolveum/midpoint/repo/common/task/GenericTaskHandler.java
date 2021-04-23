/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartDefinitionType;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * TODO
 */
@Component
public class GenericTaskHandler implements TaskHandler {

    public static final String TEMPORARY_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/generic/handler-3";

    /**
     * Executions (instances) of the current task handler. Used to delegate {@link #heartbeat(Task)} method calls.
     * Note: the future of this method is unclear.
     */
    @NotNull private final Map<String, TaskExecution> currentTaskExecutions = Collections.synchronizedMap(new HashMap<>());

    // Various useful beans.

    @Autowired private TaskPartExecutionFactoryRegistry taskPartExecutionFactoryRegistry;
    @Autowired protected TaskManager taskManager;
    @Autowired private Tracer tracer;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired @Qualifier("cacheRepositoryService") protected RepositoryService repositoryService;
    @Autowired protected PrismContext prismContext;
    @Autowired protected SchemaService schemaService;
    @Autowired protected MatchingRuleRegistry matchingRuleRegistry;
    @Autowired protected OperationExecutionRecorderForTasks operationExecutionRecorder;
    @Autowired protected LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @PostConstruct
    public void initialize() {
        taskManager.registerHandler(TEMPORARY_HANDLER_URI, this);
    }

    public @NotNull TaskManager getTaskManager() {
        return taskManager;
    }

    public @NotNull RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public @NotNull PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public @NotNull StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy(); // FIXME
    }

    @Override
    public String getArchetypeOid() {
        return null; // TODO
    }

    /**
     * Main entry point.
     *
     * We basically delegate all the processing to a TaskExecution object.
     * Error handling is delegated to {@link TaskExceptionHandlingUtil#processException(Throwable, Trace, TaskPartDefinitionType, String, TaskRunResult)}
     * method.
     */
    @Override
    public TaskRunResult run(@NotNull RunningTask localCoordinatorTask) {
        TaskExecution taskExecution = new GenericTaskExecution(localCoordinatorTask, this);
        try {
            registerExecution(localCoordinatorTask, taskExecution);
            return taskExecution.run(localCoordinatorTask.getResult());
        } catch (Throwable t) {
            throw new IllegalStateException(t); // TODO
        } finally {
            unregisterExecution(localCoordinatorTask);
        }
    }

    /** TODO decide what to do with this method. */
    private TaskExecution getCurrentTaskExecution(Task task) {
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

    @Override
    public String getCategoryName(Task task) {
        return null;
    }

    /** TODO decide what to do with this method. */
    private void registerExecution(RunningTask localCoordinatorTask, TaskExecution execution) {
        currentTaskExecutions.put(localCoordinatorTask.getOid(), execution);
    }

    /** TODO decide what to do with this method. */
    private void unregisterExecution(RunningTask localCoordinatorTask) {
        currentTaskExecutions.remove(localCoordinatorTask.getOid());
    }

    public @NotNull MatchingRuleRegistry getMatchingRuleRegistry() {
        return matchingRuleRegistry;
    }

    public @NotNull OperationExecutionRecorderForTasks getOperationExecutionRecorder() {
        return operationExecutionRecorder;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public CacheConfigurationManager getCacheConfigurationManager() {
        return cacheConfigurationManager;
    }

    public TaskPartExecutionFactoryRegistry getTaskPartExecutionFactory() {
        return taskPartExecutionFactoryRegistry;
    }
}
