/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task;

import java.lang.reflect.Constructor;
import java.util.*;

import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.common.util.OperationExecutionRecorderForTasks;
import com.evolveum.midpoint.schema.SchemaHelper;

import com.evolveum.midpoint.util.exception.SystemException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.StatisticsCollectionStrategy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.task.api.WorkBucketAwareTaskHandler;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import static com.evolveum.midpoint.task.api.util.TaskExceptionHandlingUtil.processException;
import static com.evolveum.midpoint.task.api.util.TaskExceptionHandlingUtil.processFinish;

/**
 * <p>Task handler for "search-iterative" tasks.</p>
 *
 * <p>This class fulfills rudimentary duties only:</p>
 * <ol>
 *  <li>holds autowired beans (as it is a Spring component, unlike related classes),</li>
 *  <li>provides {@link WorkBucketAwareTaskHandler} interface to the task manager.</li>
 * </ol>
 *
 * <p> <b>WARNING!</b> The task handler is efficiently singleton! It is a Spring bean and it is
 * supposed to handle all search task instances. Therefore it must not have task-specific fields.
 * It can only contain fields specific to all tasks of a specified type.</p>
 *
 * <p>All of the work is delegated to {@link AbstractSearchIterativeTaskExecution} which in turn
 * uses other classes to do the work.</p>
 *
 * <p>
 * The whole structure then looks like this:
 * </p>
 *
 * <ol>
 *     <li><{@link AbstractSearchIterativeTaskHandler} is the main entry point. It instantiates {@link AbstractSearchIterativeTaskExecution}
 *  that is responsible for the execution of the search iterative task./li>
 *     <li>Then, {@link AbstractSearchIterativeTaskExecution} represents the specific execution of the task. It should contain all the
 *  fields that are specific to given task instance, like fetched resource definition object (for synchronization tasks),
 *  timestamps (for scanner tasks), and so on. Also data provided by the {@link TaskManager} (like current task part definition,
 *  current bucket, and so on) are kept there.</li>
 *     <li>The task execution object then instantiates - via {@link AbstractSearchIterativeTaskExecution#createPartExecutions()}</li>
 *  method - objects that are responsible for execution of individual <i>task parts</i>. For example, a reconciliation task consists
 *  of three such parts: processing unfinished shadows, resource objects reconciliation, and (remaining) shadows reconciliation.
 *  Each part execution class contains code to construct a query, search options, and instantiates <i>resulting objects
 *  handler</i>: a subclass of {@link AbstractSearchIterativeResultHandler}.
 * </ol>
 *
 * <b>
 *     TODO Specify responsibilities of individual components w.r.t. multithreading, error handling,
 *      progress and error reporting, and so on.
 * </b>
 *
 * <p>This approach may look like an overkill for simple tasks (like e.g. recompute or propagation tasks), but it enables
 * code deduplication and simplification for really complex tasks, like the reconciliation. It is experimental and probably will
 * evolve in the future. There is also a possibility of introducing the <i>task execution</i> concept at the level of
 * the task manager.</p>
 *
 * <p>For the simplest tasks please use SimpleIterativeTaskHandler (in model-impl) that hides all the complexity
 * in exchange for some task limitations, like having only a single part, and so on.</p>
 *
 * @author semancik
 */
public abstract class AbstractSearchIterativeTaskHandler<
        TH extends AbstractSearchIterativeTaskHandler<TH, TE>,
        TE extends AbstractSearchIterativeTaskExecution<TH, TE>>
        implements WorkBucketAwareTaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeTaskHandler.class);

    /**
     * Human-understandable name of the task type, like "Recompute", "Import from resource", and so on.
     * Used for logging and similar purposes.
     */
    @NotNull public final String taskTypeName;

    /**
     * Prefix for the task's operation result operation name.
     * E.g. "com.evolveum.midpoint.common.operation.reconciliation"
     */
    @NotNull public final String taskOperationPrefix;

    @NotNull protected final TaskReportingOptions reportingOptions;

    // If you need to store fields specific to task instance or task run the ResultHandler is a good place to do that.

    /**
     * Executions (instances) of the current task handler. Used to delegate {@link #heartbeat(Task)} method calls.
     * Note: the future of this method is unclear.
     */
    private final Map<String, TE> currentTaskExecutions = Collections.synchronizedMap(new HashMap<>());

    @Autowired protected TaskManager taskManager;
    @Autowired @Qualifier("cacheRepositoryService") protected RepositoryService repositoryService;
    @Autowired protected PrismContext prismContext;
    @Autowired protected SchemaHelper schemaHelper;
    @Autowired protected MatchingRuleRegistry matchingRuleRegistry;
    @Autowired protected OperationExecutionRecorderForTasks operationExecutionRecorder;

    protected AbstractSearchIterativeTaskHandler(@NotNull String taskTypeName, @NotNull String taskOperationPrefix) {
        this.taskTypeName = taskTypeName;
        this.taskOperationPrefix = taskOperationPrefix;
        this.reportingOptions = new TaskReportingOptions();
    }

    protected @NotNull String getTaskTypeName() {
        return taskTypeName;
    }

    public @NotNull String getTaskOperationPrefix() {
        return taskOperationPrefix;
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
        return reportingOptions.getStatisticsCollectionStrategy();
    }

    @Override
    public TaskWorkBucketProcessingResult run(RunningTask localCoordinatorTask, WorkBucketType workBucket,
            TaskPartitionDefinitionType partition, TaskWorkBucketProcessingResult previousRunResult) {
        TE taskExecution = createTaskExecution(localCoordinatorTask, workBucket, partition, previousRunResult);
        try {
            taskExecution.run();
            return processFinish(taskExecution.getCurrentRunResult());
        } catch (Throwable t) {
            return processException(t, LOGGER, partition, taskTypeName, taskExecution.getCurrentRunResult());
        }
    }

    @NotNull
    protected TE createTaskExecution(RunningTask localCoordinatorTask, WorkBucketType workBucket,
            TaskPartitionDefinitionType partition, TaskWorkBucketProcessingResult previousRunResult) {
        try {
            TaskExecutionClass annotation = this.getClass().getAnnotation(TaskExecutionClass.class);
            Class<? extends AbstractSearchIterativeTaskExecution> executionClass =
                    annotation != null ? annotation.value() : AbstractSearchIterativeTaskExecution.class;
            Constructor<?> constructor = executionClass.getDeclaredConstructor(this.getClass(), RunningTask.class,
                    WorkBucketType.class, TaskPartitionDefinitionType.class, TaskWorkBucketProcessingResult.class);
            //noinspection unchecked
            return (TE) constructor.newInstance(this, localCoordinatorTask, workBucket, partition, previousRunResult);
        } catch (Throwable t) {
            throw new SystemException("Cannot create task execution instance for " + this.getClass() + ": " + t.getMessage(), t);
        }
    }

    private TE getCurrentTaskExecution(Task task) {
        return currentTaskExecutions.get(task.getOid());
    }

    @Override
    public Long heartbeat(Task task) {
        // Delegate heartbeat to the result handler
        TE execution = getCurrentTaskExecution(task);
        if (execution != null) {
            return execution.heartbeat();
        } else {
            // most likely a race condition.
            return null;
        }
    }

    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

    public @NotNull TaskReportingOptions getReportingOptions() {
        return reportingOptions;
    }

    void registerExecution(RunningTask localCoordinatorTask, TE execution) {
        currentTaskExecutions.put(localCoordinatorTask.getOid(), execution);
    }

    void unregisterExecution(RunningTask localCoordinatorTask) {
        currentTaskExecutions.remove(localCoordinatorTask.getOid());
    }

    public MatchingRuleRegistry getMatchingRuleRegistry() {
        return matchingRuleRegistry;
    }

    public OperationExecutionRecorderForTasks getOperationExecutionRecorder() {
        return operationExecutionRecorder;
    }
}
