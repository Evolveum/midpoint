/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;

import com.google.common.annotations.VisibleForTesting;
import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.ProfilingMode;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.NodeRegistrar;
import com.evolveum.midpoint.task.quartzimpl.execution.LocalExecutionManager;
import com.evolveum.midpoint.task.quartzimpl.execution.Schedulers;
import com.evolveum.midpoint.task.quartzimpl.execution.TaskStopper;
import com.evolveum.midpoint.task.quartzimpl.execution.TaskThreadsDumper;
import com.evolveum.midpoint.task.quartzimpl.nodes.NodeCleaner;
import com.evolveum.midpoint.task.quartzimpl.nodes.NodeRetriever;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.task.quartzimpl.run.HandlerExecutor;
import com.evolveum.midpoint.task.quartzimpl.tasks.*;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Task Manager implementation using Quartz scheduler.
 *
 * This is just a facade. All the work is distributed to individual classes.
 *
 * TODO finish review of this class
 */
@Service(value = "taskManager")
@DependsOn(value = "repositoryService")
public class TaskManagerQuartzImpl implements TaskManager, SystemConfigurationChangeListener {

    private static final String DOT_INTERFACE = TaskManager.class.getName() + ".";
    private static final String OP_COUNT_OBJECTS = DOT_INTERFACE + "countObjects";
    private static final String OP_SEARCH_OBJECTS_ITERATIVE = DOT_INTERFACE + "searchObjectsIterative";
    private static final String OP_SEARCH_OBJECTS = DOT_INTERFACE + "searchObjects";
    private static final String OP_GET_OBJECT = DOT_INTERFACE + "getObject";
    private static final String OP_DELETE_TASK_TREE = DOT_INTERFACE + "deleteTaskTree";
    private static final String OP_DELETE_TASK = DOT_INTERFACE + "deleteTask";
    private static final String OP_GET_NEXT_RUN_START_TIME = DOT_INTERFACE + "getNextStartTimes";
    private static final String OP_GET_TASK_BY_IDENTIFIER = DOT_INTERFACE + "getTaskByIdentifier";
    private static final String OP_STOP_LOCAL_SCHEDULER = DOT_INTERFACE + "stopLocalScheduler";
    private static final String OP_SCHEDULE_TASKS_NOW = DOT_INTERFACE + "scheduleTasksNow";
    private static final String OP_DELETE_NODE = DOT_INTERFACE + "deleteNode";
    private static final String OP_SCHEDULE_TASK_NOW = DOT_INTERFACE + "scheduleTaskNow";
    private static final String OP_SUSPEND_AND_DELETE_TASK = DOT_INTERFACE + "suspendAndDeleteTask";
    private static final String OP_SUSPEND_AND_DELETE_TASKS = DOT_INTERFACE + "suspendAndDeleteTasks";
    private static final String OP_SUSPEND_AND_CLOSE_TASK_NO_EXCEPTION = DOT_INTERFACE + "suspendAndCloseTaskNoException";
    private static final String OP_MODIFY_TASK = DOT_INTERFACE + "modifyTask";
    private static final String OP_ADD_TASK = DOT_INTERFACE + "addTask";
    private static final String OP_RESUME_TASKS = DOT_INTERFACE + "resumeTasks";
    private static final String OP_RESUME_TASK = DOT_INTERFACE + "resumeTask";
    private static final String OP_UNPAUSE_TASK = DOT_INTERFACE + "unpauseTask";
    private static final String OP_RESUME_TASK_TREE = DOT_INTERFACE + "resumeTaskTree";
    private static final String OP_SUSPEND_TASK_TREE = DOT_INTERFACE + "suspendTaskTree";
    private static final String OP_DEACTIVATE_SERVICE_THREADS = DOT_INTERFACE + "deactivateServiceThreads";
    private static final String OP_GET_LOCAL_SCHEDULER_INFORMATION = DOT_INTERFACE + "getLocalSchedulerInformation";
    private static final String OP_REACTIVATE_SERVICE_THREADS = DOT_INTERFACE + "reactivateServiceThreads";
    private static final String OP_START_LOCAL_SCHEDULER = DOT_INTERFACE + "startLocalScheduler";
    private static final String OP_START_SCHEDULER = DOT_INTERFACE + "startScheduler";
    private static final String OP_STOP_SCHEDULER = DOT_INTERFACE + "stopScheduler";
    private static final String OP_STOP_SCHEDULERS_AND_TASKS = DOT_INTERFACE + "stopSchedulersAndTasks";
    private static final String OP_SUSPEND_TASK = DOT_INTERFACE + "suspendTask";
    private static final String OP_MARK_CLOSED_TASK_SUSPENDED = DOT_INTERFACE + "markClosedTaskSuspended";
    private static final String OP_SUSPEND_TASKS = DOT_INTERFACE + "suspendTasks";

    private static final String DOT_IMPL_CLASS = TaskManagerQuartzImpl.class.getName() + ".";
    private static final String OP_IS_ORPHANED = DOT_IMPL_CLASS + "isOrphaned";
    private static final String OP_GET_TASK_TYPE_BY_IDENTIFIER = DOT_IMPL_CLASS + "getTaskTypeByIdentifier";
    private static final String OP_GET_TASK_PLAIN = DOT_IMPL_CLASS + "getTaskPlain";
    public static final String OP_CLEANUP_TASKS = DOT_INTERFACE + "cleanupTasks";
    private static final String OP_CLEANUP_NODES = DOT_INTERFACE + "cleanupNodes";

    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private Schedulers schedulers;
    @Autowired private TaskThreadsDumper taskThreadsDumper;
    @Autowired private TaskStopper taskStopper;
    @Autowired private LocalScheduler localScheduler;
    @Autowired private LocalExecutionManager localExecutionManager;
    @Autowired private ClusterManager clusterManager;
    @Autowired private TaskHandlerRegistry handlerRegistry;
    @Autowired private TaskListenerRegistry listenerRegistry;
    @Autowired private TaskStateManager taskStateManager;
    @Autowired private TaskRetriever taskRetriever;
    @Autowired private NodeRetriever nodeRetriever;
    @Autowired private TaskPersister taskPersister;
    @Autowired private TaskInstantiator taskInstantiator;
    @Autowired private LocalNodeState localNodeState;
    @Autowired private TaskCleaner taskCleaner;
    @Autowired private NodeCleaner nodeCleaner;
    @Autowired private NodeRegistrar nodeRegistrar;
    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private RepositoryService repositoryService;
    @Autowired(required = false) private SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection;
    @Autowired private PrismContext prismContext;
    @Autowired private LightweightTaskManager lightweightTaskManager;
    @Autowired private TaskSynchronizer taskSynchronizer;
    @Autowired private TaskBeans beans;
    @Autowired(required = false) private DataSource dataSource;

    @Autowired
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;

    // fixme How to properly initialize handler executor for job executor?
    @Autowired private HandlerExecutor handlerExecutor;

    private GlobalTracingOverride globalTracingOverride;

    private InfrastructureConfigurationType infrastructureConfiguration;

    /** Cached task prism definition. */
    private PrismObjectDefinition<TaskType> taskPrismDefinition;

    @NotNull private final Set<ClusteringAvailabilityProvider> clusteringAvailabilityProviders = ConcurrentHashMap.newKeySet();

    private static final Trace LOGGER = TraceManager.getTrace(TaskManagerQuartzImpl.class);

    //region Suspend, resume, pause, unpause
    /*
     * First here are TaskManager API methods implemented in this class,
     * then those, which are delegated to helper classes.
     */
    @Override
    public boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createSubresult(OP_DEACTIVATE_SERVICE_THREADS);
        result.addParam("timeToWait", timeToWait);
        try {
            LOGGER.info("Deactivating Task Manager service threads (waiting time = {})", timeToWait);
            clusterManager.stopClusterManagerThread(timeToWait, result);
            return localExecutionManager.stopSchedulerAndTasks(timeToWait, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void reactivateServiceThreads(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_REACTIVATE_SERVICE_THREADS);
        try {
            LOGGER.info("Reactivating Task Manager service threads.");
            clusterManager.startClusterManagerThread();
            schedulers.startScheduler(getNodeId(), result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public boolean getServiceThreadsActivationState() {
        return localScheduler.isRunningChecked() && clusterManager.isClusterManagerThreadActive();
    }

    @Override
    public boolean suspendTask(String taskOid, long waitTime, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_SUSPEND_TASK)
                .addParam("taskOid", taskOid)
                .addParam("waitTime", waitTime)
                .build();
        try {
            return taskStateManager.suspendTask(taskOid, waitTime, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void markClosedTaskSuspended(String taskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_MARK_CLOSED_TASK_SUSPENDED)
                .addParam("taskOid", taskOid)
                .build();
        try {
            taskStateManager.markClosedTaskSuspended(taskOid, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public boolean suspendTask(Task task, long waitTime, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.subresult(OP_SUSPEND_TASK)
                .addArbitraryObjectAsParam("task", task)
                .addParam("waitTime", waitTime)
                .build();
        try {
            return taskStateManager.suspendTask((TaskQuartzImpl) task, waitTime, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public boolean suspendTasks(Collection<String> taskOids, long waitForStop, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_SUSPEND_TASKS)
                .addArbitraryObjectCollectionAsParam("taskOids", taskOids)
                .addParam("waitForStop", waitForStop)
                .build();
        try {
            return taskStateManager.suspendTasks(taskOids, waitForStop, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public boolean suspendTaskTree(String rootTaskOid, long waitTime, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_SUSPEND_TASK_TREE);
        result.addParam("rootTaskOid", rootTaskOid);
        result.addParam("waitTime", waitTime);
        try {
            return taskStateManager.suspendTaskTree(rootTaskOid, waitTime, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't suspend task tree", t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void resumeTaskTree(String rootTaskOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_RESUME_TASK_TREE);
        result.addParam("rootTaskOid", rootTaskOid);
        try {
            taskStateManager.resumeTaskTree(rootTaskOid, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't resume task tree", t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void unpauseTask(Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {
        OperationResult result = parentResult.createSubresult(OP_UNPAUSE_TASK);
        result.addArbitraryObjectAsParam("task", task);
        try {
            taskStateManager.unpauseTask((TaskQuartzImpl) task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void resumeTasks(Collection<String> taskOids, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_RESUME_TASKS)
                .addArbitraryObjectCollectionAsParam("taskOids", taskOids)
                .build();
        try {
            for (String oid : taskOids) {
                try {
                    resumeTask(oid, result); // to provide result for each task resumed
                } catch (ObjectNotFoundException e) { // result is already updated
                    LoggingUtils.logException(LOGGER, "Couldn't resume task with OID {}", e, oid);
                } catch (SchemaException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resume task with OID {}", e, oid);
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void resumeTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.subresult(OP_RESUME_TASK)
                .addParam("taskOid", taskOid)
                .build();
        try {
            taskStateManager.resumeTask(taskOid, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void resumeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.subresult(OP_RESUME_TASK)
                .addArbitraryObjectAsParam("task", task)
                .build();
        try {
            taskStateManager.resumeTask((TaskQuartzImpl) task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }
    //endregion

    //region Working with task instances (other than suspend/resume)
    @Override
    public TaskQuartzImpl createTaskInstance(String operationName) {
        return taskInstantiator.createTaskInstance(operationName);
    }

    @Override
    @NotNull
    public TaskQuartzImpl createTaskInstance(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {
        return taskInstantiator.createTaskInstance(taskPrism, parentResult);
    }

    @Override
    @NotNull
    public TaskQuartzImpl createTaskInstance(PrismObject<TaskType> taskPrism, @Deprecated String operationName,
            OperationResult parentResult) throws SchemaException {
        return taskInstantiator.createTaskInstance(taskPrism, parentResult);
    }

    @Override
    @NotNull
    public TaskQuartzImpl getTaskPlain(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        return taskRetriever.getTaskPlain(taskOid, parentResult);
    }

    @Override
    @NotNull
    public TaskQuartzImpl getTaskWithResult(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        return taskRetriever.getTaskWithResult(taskOid, parentResult);
    }

    @Override
    public void switchToBackground(Task task, OperationResult parentResult) {
        taskPersister.switchToBackground((TaskQuartzImpl) task, parentResult);
    }

    @Override
    public String addTask(PrismObject<TaskType> taskPrism, RepoAddOptions options, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = parentResult.createSubresult(OP_ADD_TASK);
        try {
            return taskPersister.addTask(taskPrism, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void modifyTask(String oid,
            Collection<? extends ItemDelta<?, ?>> modifications, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(OP_MODIFY_TASK);
        try {
            taskPersister.modifyTask(oid, modifications, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void suspendAndDeleteTasks(Collection<String> taskOids, long suspendTimeout, boolean alsoSubtasks,
            OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_SUSPEND_AND_DELETE_TASKS);
        result.addArbitraryObjectCollectionAsParam("taskOids", taskOids);
        result.addParam("suspendTimeout", suspendTimeout);
        result.addParam("alsoSubtasks", alsoSubtasks);
        try {
            taskStateManager.suspendAndDeleteTasks(taskOids, suspendTimeout, alsoSubtasks, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void suspendAndDeleteTask(String taskOid, long suspendTimeout, boolean alsoSubtasks, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_SUSPEND_AND_DELETE_TASK);
        result.addParam("taskOid", taskOid);
        result.addParam("suspendTimeout", suspendTimeout);
        result.addParam("alsoSubtasks", alsoSubtasks);
        try {
            taskStateManager.suspendAndDeleteTask(taskOid, suspendTimeout, alsoSubtasks, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void suspendAndCloseTaskNoException(Task task, long suspendTimeout, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_SUSPEND_AND_CLOSE_TASK_NO_EXCEPTION);
        result.addArbitraryObjectAsParam("task", task);
        result.addParam("suspendTimeout", suspendTimeout);
        try {
            taskStateManager.suspendAndCloseTaskNoException((TaskQuartzImpl) task, suspendTimeout, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't suspend and close task {}", t, task);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createSubresult(OP_DELETE_TASK);
        result.addParam("oid", oid);
        try {
            taskStateManager.deleteTask(oid, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void deleteTaskTree(String rootTaskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_DELETE_TASK_TREE);
        result.addParam("rootTaskOid", rootTaskOid);
        try {
            taskStateManager.deleteTaskTree(rootTaskOid, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }
    //endregion

    //region Transient and lightweight tasks
    @Override
    public void waitForTransientChildrenAndCloseThem(RunningTask task, OperationResult result) {
        lightweightTaskManager.waitForTransientChildrenAndCloseThem(task, result);
    }
    //endregion

    //region Getting and searching for tasks and nodes

    @Override
    public @NotNull <T extends ObjectType> PrismObject<T> getObject(Class<T> type,
            String oid,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.createMinorSubresult(OP_GET_OBJECT);
        result.addParam("objectType", type);
        result.addParam("oid", oid);
        result.addArbitraryObjectCollectionAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        try {
            if (TaskType.class.isAssignableFrom(type)) {
                return taskRetriever.getTaskPrismObject(oid, options, result);
            } else if (NodeType.class.isAssignableFrom(type)) {
                //noinspection unchecked
                return (PrismObject<T>) repositoryService.getObject(NodeType.class, oid, options, result);
                // TODO add transient attributes just like in searchObject
            } else {
                throw new IllegalArgumentException("Unsupported object type: " + type);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    @NotNull
    public TaskQuartzImpl getTaskPlain(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(OP_GET_TASK_PLAIN);
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addArbitraryObjectCollectionAsParam(OperationResult.PARAM_OPTIONS, options);
        try {
            return taskRetriever.getTaskPlain(oid, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    @NotNull
    public TaskQuartzImpl getTask(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(DOT_IMPL_CLASS + "getTask");
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addArbitraryObjectCollectionAsParam(OperationResult.PARAM_OPTIONS, options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
        try {
            return taskRetriever.getTask(oid, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    @NotNull
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OP_SEARCH_OBJECTS);
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addArbitraryObjectCollectionAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
        try {
            if (TaskType.class.isAssignableFrom(type)) {
                //noinspection unchecked
                return (SearchResultList<PrismObject<T>>) (SearchResultList<?>) taskRetriever.searchTasks(query, options, result);
            } else if (NodeType.class.isAssignableFrom(type)) {
                //noinspection unchecked
                return (SearchResultList<PrismObject<T>>) (SearchResultList<?>) nodeRetriever.searchNodes(query, options, result);
            } else {
                throw new IllegalArgumentException("Unsupported object type: " + type);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            ResultHandler<T> handler, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(OP_SEARCH_OBJECTS_ITERATIVE);
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addArbitraryObjectCollectionAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
        try {
            SearchResultList<PrismObject<T>> objects;
            if (TaskType.class.isAssignableFrom(type)) {
                //noinspection unchecked
                objects = (SearchResultList<PrismObject<T>>) (SearchResultList<?>) taskRetriever.searchTasks(query, options, result);
            } else if (NodeType.class.isAssignableFrom(type)) {
                //noinspection unchecked
                objects = (SearchResultList<PrismObject<T>>) (SearchResultList<?>) nodeRetriever.searchNodes(query, options, result);
            } else {
                throw new IllegalArgumentException("Unsupported object type: " + type);
            }

            for (PrismObject<T> object : objects) {
                handler.handle(object, result);
            }
            return objects.getMetadata();

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public <T extends ObjectType> int countObjects(
            Class<T> type, ObjectQuery query, OperationResult parentResult)
            throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(OP_COUNT_OBJECTS);
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        try {
            return repositoryService.countObjects(type, query, null, parentResult);
        } finally {
            result.computeStatus();
        }
    }
    //endregion

    //region Managing handlers and task categories
    @Override
    public void registerHandler(@NotNull String uri, @NotNull TaskHandler handler) {
        handlerRegistry.registerHandler(uri, handler);
    }

    @Override
    public void unregisterHandler(String uri) {
        handlerRegistry.unregisterHandler(uri);
    }

    @Override
    public TaskHandler getHandler(String uri) {
        return handlerRegistry.getHandler(uri);
    }

    @Override
    public void setDefaultHandlerUri(String uri) {
        handlerRegistry.setDefaultHandlerUri(uri);
    }

    //endregion

    //region Notifications
    @Override
    public void registerTaskListener(TaskListener taskListener) {
        listenerRegistry.registerTaskListener(taskListener);
    }

    @Override
    public void unregisterTaskListener(TaskListener taskListener) {
        listenerRegistry.unregisterTaskListener(taskListener);
    }

    @Override
    public void registerTaskDeletionListener(TaskDeletionListener listener) {
        listenerRegistry.registerTaskDeletionListener(listener);
    }

    @Override
    public void registerTaskUpdatedListener(TaskUpdatedListener listener) {
        listenerRegistry.registerTaskUpdatedListener(listener);
    }

    @Override
    public void unregisterTaskUpdatedListener(TaskUpdatedListener listener) {
        listenerRegistry.unregisterTaskUpdatedListener(listener);
    }
    //endregion

    //region Other methods + getters and setters (CLEAN THIS UP)
    PrismObjectDefinition<TaskType> getTaskObjectDefinition() {
        if (taskPrismDefinition == null) {
            taskPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
        }
        return taskPrismDefinition;
    }

    @NotNull
    public TaskManagerConfiguration getConfiguration() {
        return configuration;
    }

    @NotNull
    public PrismContext getPrismContext() {
        return prismContext;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public SqlPerformanceMonitorsCollection getSqlPerformanceMonitorsCollection() {
        return sqlPerformanceMonitorsCollection;
    }

    public TaskThreadsDumper getExecutionManager() {
        return taskThreadsDumper;
    }
    //endregion

    //region TODO
    @Override
    public void synchronizeTasks(OperationResult result) {
        taskSynchronizer.synchronizeJobStores(result);
    }

    @Override
    public @NotNull String getNodeId() {
        return configuration.getNodeId();
    }

    @Override
    public SchedulerInformationType getLocalSchedulerInformation(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_GET_LOCAL_SCHEDULER_INFORMATION);
        try {
            return localExecutionManager.getLocalSchedulerInformation(result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void stopLocalScheduler(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_STOP_LOCAL_SCHEDULER);
        try {
            localScheduler.stopScheduler(result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't stop local scheduler: " + t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void startLocalScheduler(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_START_LOCAL_SCHEDULER);
        try {
            localScheduler.startScheduler();
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void stopScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_STOP_SCHEDULER)
                .addParam("nodeIdentifier", nodeIdentifier)
                .build();
        try {
            schedulers.stopScheduler(nodeIdentifier, parentResult);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult result) {
        for (String nodeIdentifier : nodeIdentifiers) {
            try {
                stopScheduler(nodeIdentifier, result);
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Couldn't stop scheduler on node {}", t, nodeIdentifier);
            }
        }
    }

    @Override
    public void startScheduler(String nodeIdentifier, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(OP_START_SCHEDULER)
                .addParam("nodeIdentifier", nodeIdentifier)
                .build();
        try {
            schedulers.startScheduler(nodeIdentifier, parentResult);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void startSchedulers(Collection<String> nodeIdentifiers, OperationResult result) {
        for (String nodeIdentifier : nodeIdentifiers) {
            try {
                startScheduler(nodeIdentifier, result);
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Couldn't start scheduler on node {}", t, nodeIdentifier);
            }
        }
    }

    @Override
    public boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long timeToWait, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_STOP_SCHEDULERS_AND_TASKS)
                .addArbitraryObjectCollectionAsParam("nodeIdentifiers", nodeIdentifiers)
                .addParam("timeToWait", timeToWait)
                .build();
        try {
            LOGGER.info("Stopping schedulers and tasks on nodes: {}, waiting {} ms for task(s) shutdown.", nodeIdentifiers, timeToWait);
            stopSchedulers(nodeIdentifiers, result);
            return taskStopper.stopAllTasksOnNodes(nodeIdentifiers, timeToWait, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void stopLocalTaskRunInStandardWay(String oid, OperationResult result) {
        localScheduler.stopLocalTaskRunInStandardWay(oid, result);
    }

    @Override
    public RunningTask getLocallyRunningTaskByIdentifier(String lightweightIdentifier) {
        return localNodeState.getLocallyRunningTaskByIdentifier(lightweightIdentifier);
    }

    @Override
    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return clusterManager.isCurrentNode(node);
    }

    @Override
    public void deleteNode(String nodeOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_DELETE_NODE)
                .addParam("nodeOid", nodeOid)
                .build();
        try {
            clusterManager.deleteNode(nodeOid, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void registerNodeUp(OperationResult result) {
        clusterManager.registerNodeUp(result);
    }

    @Override
    public @NotNull ClusterStateType determineClusterState(OperationResult result) throws SchemaException {
        return clusterManager.determineClusterState(result);
    }

    @Override
    public void scheduleTaskNow(String taskOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_SCHEDULE_TASK_NOW)
                .addParam("taskOid", taskOid)
                .build();
        try {
            taskStateManager.scheduleTaskNow(taskOid, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void scheduleTaskNow(Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_SCHEDULE_TASK_NOW)
                .addArbitraryObjectAsParam("task", task)
                .build();
        try {
            taskStateManager.scheduleTaskNow((TaskQuartzImpl) task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void scheduleTasksNow(Collection<String> taskOids, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_SCHEDULE_TASKS_NOW);
        result.addArbitraryObjectCollectionAsParam("taskOids", taskOids);
        try {
            for (String oid : taskOids) {
                try {
                    scheduleTaskNow(oid, result);
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't schedule task with OID {}", e, oid);
                } catch (SchemaException | RuntimeException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't schedule task with OID {}", e, oid);
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    @VisibleForTesting // TODO
    public void closeTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        Task task = taskRetriever.getTaskPlain(taskOid, parentResult);
        taskStateManager.closeTask(task, parentResult);
    }

    // currently finds only persistent tasks
    @Override
    @NotNull
    public TaskQuartzImpl getTaskByIdentifier(String identifier, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(OP_GET_TASK_BY_IDENTIFIER);
        result.addParam("identifier", identifier);
        try {
            return taskRetriever.getTaskByIdentifier(identifier, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    @NotNull
    public PrismObject<TaskType> getTaskTypeByIdentifier(String identifier, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(OP_GET_TASK_TYPE_BY_IDENTIFIER);
        result.addParam("identifier", identifier);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
        try {
            return taskRetriever.getTaskTypeByIdentifier(identifier, options, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public boolean isOrphaned(PrismObject<TaskType> task, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.subresult(OP_IS_ORPHANED)
                .setMinor()
                .addArbitraryObjectAsParam("task", task)
                .build();
        try {
            return taskRetriever.isOrphaned(task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void cleanupTasks(@NotNull CleanupPolicyType policy, @NotNull Predicate<TaskType> selector,
            @NotNull RunningTask executionTask, @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        if (policy.getMaxAge() == null) {
            return;
        }

        OperationResult result = parentResult.createSubresult(OP_CLEANUP_TASKS);
        try {
            taskCleaner.cleanupTasks(policy, selector, executionTask, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void cleanupNodes(@NotNull DeadNodeCleanupPolicyType policy, @NotNull Predicate<NodeType> selector,
            @NotNull RunningTask task, @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {

        if (policy.getMaxAge() == null) {
            return;
        }

        OperationResult result = parentResult.createSubresult(OP_CLEANUP_NODES);
        try {
            nodeCleaner.cleanupNodes(policy, selector, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public Long getNextRunStartTime(String oid, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(OP_GET_NEXT_RUN_START_TIME);
        result.addParam("oid", oid);
        result.addParam("retrieveNextRunStartTime", true);
        result.addParam("retrieveRetryTime", false);
        try {
            return localScheduler.getNextStartTimes(oid, true, false, result)
                    .getNextScheduledRun();
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public String getIntraClusterHttpUrlPattern() {
        return infrastructureConfiguration != null
                ? infrastructureConfiguration.getIntraClusterHttpUrlPattern()
                : null;
    }
    //endregion

    public SecurityContextManager getSecurityContextManager() {
        return securityContextManager;
    }

    public HandlerExecutor getHandlerExecutor() {
        return handlerExecutor;
    }

//    @Override
//    public TaskHandler createAndRegisterPartitioningTaskHandler(String handlerUri, Function<Task, TaskPartitionsDefinition> partitioningStrategy) {
//        PartitioningTaskHandler handler = new PartitioningTaskHandler(this, partitioningStrategy);
//        registerHandler(handlerUri, handler);
//        return handler;
//    }

    @Override
    public boolean isLocalNodeClusteringEnabled() {
        return configuration.isLocalNodeClusteringEnabled();
    }

    @Override
    public boolean isClustered() {
        return configuration.isClustered();
    }

    @Override
    public void setWebContextPath(String path) {
        nodeRegistrar.setWebContextPath(path);
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        infrastructureConfiguration = value != null ? value.getInfrastructure() : null;
    }

    @Override
    public String getRunningTasksThreadsDump(OperationResult parentResult) {
        return taskThreadsDumper.getRunningTasksThreadsDump(parentResult);
    }

    @Override
    public String recordRunningTasksThreadsDump(String cause, OperationResult parentResult) throws ObjectAlreadyExistsException {
        return taskThreadsDumper.recordRunningTasksThreadsDump(cause, parentResult);
    }

    @Override
    public String getTaskThreadsDump(String taskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        return taskThreadsDumper.getTaskThreadsDump(taskOid, parentResult);
    }

    @VisibleForTesting
    @Override
    public RunningTaskQuartzImpl createFakeRunningTask(Task task) {
        RunningTaskQuartzImpl runningTask = taskInstantiator.toRunningTaskInstance(task, task, null);
        runningTask.setExecutingThread(Thread.currentThread());
        return runningTask;
    }

    @Override
    public @NotNull NodeType getLocalNode() {
        return nodeRegistrar.getCachedLocalNodeObjectRequired().asObjectable();
    }

    @Override
    public @NotNull String getLocalNodeOid() {
        return nodeRegistrar.getCachedLocalNodeObjectOid();
    }

    @Override
    public CacheConfigurationManager getCacheConfigurationManager() {
        return cacheConfigurationManager;
    }

    @Override
    public boolean isDynamicProfilingEnabled() {
        return midpointConfiguration.getProfilingMode() == ProfilingMode.DYNAMIC;
    }

    @Override
    public boolean isTracingOverridden() {
        return globalTracingOverride != null;
    }

    @NotNull
    @Override
    public Collection<TracingRootType> getGlobalTracingRequestedFor() {
        return globalTracingOverride != null ? globalTracingOverride.roots : emptySet();
    }

    @Override
    public TracingProfileType getGlobalTracingProfile() {
        return globalTracingOverride != null ? globalTracingOverride.profile : null;
    }

    @Override
    public void setGlobalTracingOverride(@NotNull Collection<TracingRootType> roots, @NotNull TracingProfileType profile) {
        globalTracingOverride = new GlobalTracingOverride(roots, profile);
    }

    @Override
    public void unsetGlobalTracingOverride() {
        globalTracingOverride = null;
    }

    @Override
    public boolean isUpAndAlive(NodeType node) {
        return clusterManager.isUpAndAlive(node);
    }

    @Override
    public boolean isCheckingIn(NodeType node) {
        return clusterManager.isCheckingIn(node);
    }

    @Override
    public Collection<ObjectReferenceType> getLocalNodeGroups() {
        return Collections.unmodifiableCollection(
                getLocalNode().getArchetypeRef());
    }

    // TODO move to more appropriate place
    @Override
    public Number[] getDBPoolStats() {
        if (dataSource instanceof HikariDataSource) {
            HikariPoolMXBean pool = ((HikariDataSource) dataSource).getHikariPoolMXBean();
            HikariConfigMXBean config = ((HikariDataSource) dataSource).getHikariConfigMXBean();

            if (pool == null || config == null) {
                return null;
            }

            return new Number[] {
                    pool.getActiveConnections(),
                    pool.getIdleConnections(),
                    pool.getThreadsAwaitingConnection(),
                    pool.getTotalConnections(),
                    config.getMaximumPoolSize()
            };
        }
        return null;
    }

    @Override
    public void registerClusteringAvailabilityProvider(@NotNull ClusteringAvailabilityProvider provider) {
        clusteringAvailabilityProviders.add(provider);
    }

    @Override
    public void unregisterClusteringAvailabilityProvider(@NotNull ClusteringAvailabilityProvider provider) {
        clusteringAvailabilityProviders.remove(provider);
    }

    @Override
    public boolean isClusteringAvailable() {
        if (clusteringAvailabilityProviders.isEmpty()) {
            return true; // should not occur during the regular system operation; there should be exactly one such provider
        }
        return clusteringAvailabilityProviders.stream()
                .anyMatch(provider -> provider.isClusteringAvailable());
    }

    public TaskBeans getBeans() {
        return beans;
    }
}
