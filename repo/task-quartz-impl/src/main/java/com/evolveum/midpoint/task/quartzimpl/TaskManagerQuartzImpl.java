/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.Response;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.Holder;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.configuration.api.ProfilingMode;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.task.quartzimpl.execution.ExecutionManager;
import com.evolveum.midpoint.task.quartzimpl.execution.HandlerExecutor;
import com.evolveum.midpoint.task.quartzimpl.execution.StalledTasksWatcher;
import com.evolveum.midpoint.task.quartzimpl.handlers.PartitioningTaskHandler;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.task.quartzimpl.work.workers.WorkersManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Task Manager implementation using Quartz scheduler.
 * <p>
 * Main classes:
 * - TaskManagerQuartzImpl
 * - TaskQuartzImpl
 * <p>
 * Helper classes:
 * - ExecutionManager: node-related functions (start, stop, query status), task-related functions (stop, query status)
 * - LocalNodeManager and RemoteExecutionManager (specific methods for local node and remote nodes)
 * <p>
 * - TaskManagerConfiguration: access to config gathered from various places (midpoint config, sql repo config, system properties)
 * - ClusterManager: keeps cluster nodes synchronized and verifies cluster configuration sanity
 * - JmxClient: used to invoke remote JMX agents
 * - JmxServer: provides a JMX agent for midPoint
 * - TaskSynchronizer: synchronizes information about tasks between midPoint repo and Quartz Job Store
 * - Initializer: contains TaskManager initialization code (quite complex)
 *
 * @author Pavol Mederly
 */
@Service(value = "taskManager")
@DependsOn(value = "repositoryService")
public class TaskManagerQuartzImpl implements TaskManager, BeanFactoryAware, SystemConfigurationChangeListener {

    private static final String DOT_INTERFACE = TaskManager.class.getName() + ".";
    private static final String DOT_IMPL_CLASS = TaskManagerQuartzImpl.class.getName() + ".";
    private static final String CLEANUP_TASKS = DOT_INTERFACE + "cleanupTasks";
    private static final String CLEANUP_NODES = DOT_INTERFACE + "cleanupNodes";
    public static final String CONTENTION_LOG_NAME = TaskManagerQuartzImpl.class.getName() + ".contention";

    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private LocalizationService localizationService;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private ClusterExecutionHelper clusterExecutionHelper;
    @Autowired private Protector protector;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private Tracer tracer;
    @Autowired private CacheDispatcher cacheDispatcher;
    @Autowired private CacheRegistry cacheRegistry;
    @Autowired private CounterManager counterManager;

    private GlobalTracingOverride globalTracingOverride;

    private InfrastructureConfigurationType infrastructureConfiguration;
    private String webContextPath;

    // instances of all the helper classes (see their definitions for their description)
    // TODO Convert these to regular spring beans!
    private final ExecutionManager executionManager = new ExecutionManager(this);
    private final ClusterManager clusterManager = new ClusterManager(this);
    private final StalledTasksWatcher stalledTasksWatcher = new StalledTasksWatcher(this);

    /** Task handlers mapped from their URIs. */
    private final Map<String, TaskHandler> handlers = new HashMap<>();

    /**
     * Primary handlers URIs.
     * These will be taken into account when searching for handler matching a given task category.
     */
    private final Map<String, TaskHandler> primaryHandlersUris = new HashMap<>();

    /** All non-deprecated handlers URIs. */
    private final Map<String, TaskHandler> nonDeprecatedHandlersUris = new HashMap<>();

    private final Set<TaskDeletionListener> taskDeletionListeners = new HashSet<>();

    /** Cached task prism definition. */
    private PrismObjectDefinition<TaskType> taskPrismDefinition;

    /**
     * Error status for this node.
     * Local Quartz scheduler is not allowed to be started if this status is not "OK".
     */
    private NodeErrorStatusType nodeErrorStatus = NodeErrorStatusType.OK;

    private final Set<TaskListener> taskListeners = new HashSet<>();

    /**
     * Locally running task instances - here are EXACT instances of {@link TaskQuartzImpl}
     * that are used to execute handlers.
     * Use ONLY for those actions that need to work with these instances,
     * e.g. when calling heartbeat() methods on them.
     * For any other business please use LocalNodeManager.getLocallyRunningTasks(...).
     * Maps task id to the task instance.
     */
    private final Map<String, RunningTaskQuartzImpl> locallyRunningTaskInstancesMap = new ConcurrentHashMap<>();

    private final ExecutorService lightweightHandlersExecutor = Executors.newCachedThreadPool();

    private BeanFactory beanFactory;

    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private RepositoryService repositoryService;
    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaHelper schemaHelper;
    @Autowired private WorkStateManager workStateManager;
    @Autowired private WorkersManager workersManager;
    @Autowired private RelationRegistry relationRegistry;

    @Autowired
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;

    // fixme " how to properly initialize handler executer for jobexecutor?
    @Autowired private HandlerExecutor handlerExecutor;

    private static final Trace LOGGER = TraceManager.getTrace(TaskManagerQuartzImpl.class);

    /**
     * How long to wait after TaskManager shutdown, if using JDBC Job Store.
     * This gives the JDBC thread pool a chance to close, before embedded H2 database server
     * would be closed by the SQL repo shutdown procedure.
     * The fact that H2 database is embedded is determined by {@link TaskManagerConfiguration#isDatabaseIsEmbedded()},
     * used in {@link #shutdown()} method.
     */
    private static final long WAIT_ON_SHUTDOWN = 2000;

    private static final List<String> PURGE_SUCCESSFUL_RESULT_FOR = Collections.singletonList(TaskCategory.WORKFLOW);

    //region Initialization and shutdown
    // ********************* INITIALIZATION AND SHUTDOWN *********************

    /**
     * Initialization.
     * <p>
     * TaskManager can work in two modes:
     * - "stop on initialization failure" - it means that if TaskManager initialization fails, the midPoint will
     * not be started (implemented by throwing SystemException). This is a safe approach, however, midPoint could
     * be used also without Task Manager, so it is perhaps too harsh to do it this way.
     * - "continue on initialization failure" - after such a failure midPoint initialization simply continues;
     * however, task manager is switched to "Error" state, in which the scheduler cannot be started;
     * Moreover, actually almost none Task Manager methods can be invoked, to prevent a damage.
     * <p>
     * This second mode is EXPERIMENTAL, should not be used in production for now.
     * <p>
     * ---
     * So,
     * <p>
     * (A) Generally, when not initialized, we refuse to execute almost all operations (knowing for sure that
     * the scheduler is not running).
     * <p>
     * (B) When initialized, but in error state (typically because of cluster misconfiguration not related to this node),
     * we refuse to start the scheduler on this node. Other methods are OK.
     */

    @PostConstruct
    public void init() {
        OperationResult result = createOperationResult(DOT_IMPL_CLASS + "init");

        try {
            new Initializer(this).init(result);
        } catch (TaskManagerInitializationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot initialize TaskManager due to the following exception: ", e);
            throw new SystemException("Cannot initialize TaskManager", e);
        }

        systemConfigurationChangeDispatcher.registerListener(this);

        // if running in test mode, the postInit will not be executed... so we have to start scheduler here
        if (configuration.isTestMode()) {
            startSchedulerIfNeeded(result);
        }

        clusterManager.postConstruct();
    }

    @PreDestroy
    public void destroy() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
        clusterManager.preDestroy();
    }

    @Override
    public void postInit(OperationResult parentResult) {
        // nothing to do here
    }

    @Override
    @EventListener(ApplicationReadyEvent.class)
    public void onSystemStarted() {
        int delay = configuration.getNodeStartupDelay();
        if (delay > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(delay * 1000L);
                    switchFromStartingToUpState();
                } catch (InterruptedException e) {
                    LOGGER.warn("Got InterruptedException while waiting to switch to UP state; skipping the switch.");
                }
            }, "Delayed Starting to Up state transition").start();
        } else {
            switchFromStartingToUpState();
        }
    }

    private void switchFromStartingToUpState() {
        OperationResult result = new OperationResult(DOT_IMPL_CLASS + "switchFromStartingToUpState");

        clusterManager.registerNodeUp(result);
        cacheDispatcher.dispatchInvalidation(null, null, false, null);

        clusterManager.startClusterManagerThread();
        startSchedulerIfNeeded(result);
    }

    private void startSchedulerIfNeeded(OperationResult result) {
        if (configuration.isSchedulerInitiallyStopped()) {
            LOGGER.info("Scheduler was not started because of system configuration 'schedulerInitiallyStopped' setting. You can start it manually if needed.");
        } else if (midpointConfiguration.isSafeMode()) {
            LOGGER.info("Scheduler was not started because the safe mode is ON. You can start it manually if needed.");
        } else {
            executionManager.startScheduler(getNodeId(), result);
            if (result.getLastSubresultStatus() != SUCCESS) {
                throw new SystemException("Quartz task scheduler couldn't be started.");
            }
        }
    }

    @PreDestroy
    public void shutdown() {

        OperationResult result = createOperationResult("shutdown");

        LOGGER.info("Task Manager shutdown starting");

        if (executionManager.getQuartzScheduler() != null) {

            executionManager.stopScheduler(getNodeId(), result);
            executionManager.stopAllTasksOnThisNodeAndWait(0L, result);

            if (configuration.isTestMode()) {
                LOGGER.info("Quartz scheduler will NOT be shutdown. It stays in paused mode.");
            } else {
                try {
                    executionManager.shutdownLocalScheduler();
                } catch (TaskManagerException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot shutdown Quartz scheduler, continuing with node shutdown", e);
                }
            }
        }

        clusterManager.stopClusterManagerThread(0L, result);
        clusterManager.recordNodeShutdown(result);

        if (configuration.isJdbcJobStore() && configuration.isDatabaseIsEmbedded()) {
            LOGGER.trace("Waiting {} msecs to give Quartz thread pool a chance to shutdown.", WAIT_ON_SHUTDOWN);
            try {
                Thread.sleep(WAIT_ON_SHUTDOWN);
            } catch (InterruptedException e) {
                // safe to ignore
            }
        }
        LOGGER.info("Task Manager shutdown finished");
    }

    //endregion

    //region Node state management (???)
    /*
     *  ********************* STATE MANAGEMENT *********************
     */

    public boolean isRunning() {
        return executionManager.isLocalNodeRunning();
    }

    public boolean isInErrorState() {
        return nodeErrorStatus != NodeErrorStatusType.OK;
    }
    //endregion

    //region Suspend, resume, pause, unpause
    /*
     * First here are TaskManager API methods implemented in this class,
     * then those, which are delegated to helper classes.
     */

    @Override
    public boolean deactivateServiceThreads(long timeToWait, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "deactivateServiceThreads");
        result.addParam("timeToWait", timeToWait);

        LOGGER.info("Deactivating Task Manager service threads (waiting time = " + timeToWait + ")");
        clusterManager.stopClusterManagerThread(timeToWait, result);
        boolean retval = executionManager.stopSchedulerAndTasksLocally(timeToWait, result);

        result.computeStatus();
        return retval;
    }

    @Override
    public void reactivateServiceThreads(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "reactivateServiceThreads");

        LOGGER.info("Reactivating Task Manager service threads.");
        clusterManager.startClusterManagerThread();
        executionManager.startScheduler(getNodeId(), result);

        result.computeStatus();
    }

    @Override
    public boolean getServiceThreadsActivationState() {

        try {
            Scheduler scheduler = executionManager.getQuartzScheduler();
            return scheduler != null && scheduler.isStarted() &&
                    !scheduler.isInStandbyMode() &&
                    !scheduler.isShutdown() &&
                    clusterManager.isClusterManagerThreadActive();
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot determine the state of the Quartz scheduler", e);
            return false;
        }
    }

    @Override
    public boolean suspendTask(String taskOid, long waitTime, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        return suspendTask(getTaskPlain(taskOid, parentResult), waitTime, parentResult);
    }

    @Override
    public boolean suspendTask(Task task, long waitTime, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        suspendTaskInternal(task, parentResult);
        return waitForTasksToStopInternal(singleton(task), waitTime, parentResult);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean suspendTaskQuietly(Task task, long waitTime, OperationResult parentResult) {
        return suspendTasksInternalQuietly(singleton(task), waitTime, false, parentResult);
    }

    @Override
    public boolean suspendAndCloseTaskQuietly(Task task, long waitTime, OperationResult parentResult) {
        return suspendTasksInternalQuietly(singleton(task), waitTime, true, parentResult);
    }

    @Override
    public boolean suspendTasks(Collection<String> taskOids, long waitForStop, OperationResult parentResult) {
        return suspendTasksInternalQuietly(resolveTaskOids(taskOids, parentResult), waitForStop, false, parentResult);
    }

    @Override
    public boolean suspendTaskTree(String rootTaskOid, long waitTime, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "suspendTaskTree");
        result.addParam("rootTaskOid", rootTaskOid);
        result.addParam("waitTime", waitTime);

        try {
            TaskQuartzImpl root = getTaskPlain(rootTaskOid, result);
            List<Task> subtasks = root.listSubtasksDeeply(true, parentResult);
            List<String> oidsToSuspend = new ArrayList<>(subtasks.size() + 1);
            oidsToSuspend.add(rootTaskOid);
            for (Task subtask : subtasks) {
                oidsToSuspend.add(subtask.getOid());
            }
            return suspendTasks(oidsToSuspend, waitTime, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't suspend task tree", t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void resumeTaskTree(String rootTaskOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "resumeTaskTree");
        result.addParam("rootTaskOid", rootTaskOid);

        try {
            TaskQuartzImpl root = getTaskPlain(rootTaskOid, result);
            List<Task> subtasks = root.listSubtasksDeeply(true, parentResult);
            List<String> oidsToResume = new ArrayList<>(subtasks.size() + 1);
            if (root.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
                oidsToResume.add(rootTaskOid);
            }
            for (Task subtask : subtasks) {
                if (subtask.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
                    oidsToResume.add(subtask.getOid());
                }
            }
            resumeTasks(oidsToResume, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't resume task tree", t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void reconcileWorkers(String coordinatorTaskOid, WorkersReconciliationOptions options, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "reconcileWorkers");
        result.addParam("coordinatorTaskOid", coordinatorTaskOid);

        try {
            workersManager.reconcileWorkers(coordinatorTaskOid, options, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't reconcile workers", t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void deleteWorkersAndWorkState(String rootTaskOid, boolean deleteWorkers, long subtasksWaitTime,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "deleteWorkersAndWorkState");
        result.addParam("rootTaskOid", rootTaskOid);
        result.addParam("deleteWorkers", deleteWorkers);
        result.addParam("subtasksWaitTime", subtasksWaitTime);
        try {
            workersManager.deleteWorkersAndWorkState(rootTaskOid, deleteWorkers, subtasksWaitTime, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't delete workers and work state", t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void scheduleCoordinatorAndWorkersNow(String coordinatorOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "scheduleCoordinatorAndWorkersNow");
        result.addParam("coordinatorOid", coordinatorOid);
        try {
            TaskQuartzImpl coordinatorTask = getTaskPlain(coordinatorOid, result);
            TaskExecutionStatus status = coordinatorTask.getExecutionStatus();
            switch (status) {
                case CLOSED:
                case RUNNABLE:
                    // hoping that the task handler will do what is needed (i.e. recreate or restart workers)
                    scheduleTaskNow(coordinatorTask, result);
                    break;
                case WAITING:
                    // this means that workers are either busy (runnable) or are suspended; administrator should do something with that
                    String msg1 =
                            "Coordinator " + coordinatorTask + " cannot be run now, because it is in WAITING state. " +
                                    "Please check and resolve state of its worker tasks.";
                    LOGGER.error(msg1);
                    result.recordFatalError(msg1);
                    break;
                case SUSPENDED:
                    String msg2 =
                            "Coordinator " + coordinatorTask + " cannot be run now, because it is in SUSPENDED state. " +
                                    "Please use appropriate method to schedule its execution.";
                    LOGGER.error(msg2);
                    result.recordFatalError(msg2);
                    break;
                default:
                    throw new IllegalStateException("Coordinator " + coordinatorTask + " is in unsupported state: " + status);
            }
        } catch (Throwable t) {
            result.recordFatalError("Couldn't resume coordinator and its workers", t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private boolean suspendTasksInternalQuietly(Collection<Task> tasks, long waitForStop, boolean closeTasks, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "suspendTasks");
        result.addArbitraryObjectCollectionAsParam("tasks", tasks);
        result.addParam("waitForStop", waitingInfo(waitForStop));
        result.addParam("closeTasks", closeTasks);

        LOGGER.info("{} tasks {}; {}.", closeTasks ? "Closing" : "Suspending", tasks, waitingInfo(waitForStop));

        for (Task task : tasks) {
            try {
                suspendTaskInternal(task, result);
            } catch (ObjectNotFoundException e) {
                String message = "Cannot suspend task because it does not exist; task = " + task;
                LoggingUtils.logException(LOGGER, message, e);
            } catch (SchemaException | RuntimeException e) {
                String message = "Cannot suspend task because of an unexpected exception; task = " + task;
                LoggingUtils.logUnexpectedException(LOGGER, message, e);
            }
        }
        boolean stopped = waitForTasksToStopInternal(tasks, waitForStop, result);
        if (closeTasks) {
            for (Task task : tasks) {
                try {
                    closeTask(task, result);
                    ((InternalTaskInterface) task).checkDependentTasksOnClose(parentResult);
                } catch (ObjectNotFoundException | SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot close suspended task {}", e, task);
                }
            }
        }
        result.computeStatus();
        return stopped;
    }

    private boolean waitForTasksToStopInternal(Collection<Task> tasks, long waitForStop, OperationResult result) {
        //noinspection SimplifiableIfStatement
        if (waitForStop != DO_NOT_STOP) {
            return executionManager.stopTasksRunAndWait(tasks, null, waitForStop, true, result);
        } else {
            return false;
        }
    }

    private void suspendTaskInternal(Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (task.getOid() == null) {
            // this should not occur; so we can treat it in such a brutal way
            throw new IllegalArgumentException("Only persistent tasks can be suspended/closed (for now); task " + task + " is transient.");
        } else {
            if (task.getExecutionStatus() == TaskExecutionStatus.WAITING || task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
                try {
                    List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                            .item(TaskType.F_EXECUTION_STATUS).replace(TaskExecutionStatusType.SUSPENDED)
                            .item(TaskType.F_STATE_BEFORE_SUSPEND).replace(task.getExecutionStatus().toTaskType())
                            .asItemDeltas();
                    ((InternalTaskInterface) task).applyDeltasImmediate(itemDeltas, result);
                } catch (ObjectAlreadyExistsException e) {
                    throw new SystemException("Unexpected ObjectAlreadyExistsException while suspending a task: " + e.getMessage(), e);
                }
            }
            executionManager.pauseTaskJob(task, result);
            // even if this will not succeed, by setting the execution status to SUSPENDED we hope the task
            // thread will exit on next iteration (does not apply to single-run tasks, of course)
        }
    }

    private String waitingInfo(long waitForStop) {
        if (waitForStop == WAIT_INDEFINITELY) {
            return "stop tasks, and wait for their completion (if necessary)";
        } else if (waitForStop == DO_NOT_WAIT) {
            return "stop tasks, but do not wait";
        } else if (waitForStop == DO_NOT_STOP) {
            return "do not stop tasks";
        } else {
            return "stop tasks and wait " + waitForStop + " ms for their completion (if necessary)";
        }
    }

    // todo: better name for this method

    @Override
    public void pauseTask(Task task, TaskWaitingReason reason, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "pauseTask");
        result.addArbitraryObjectAsParam("task", task);

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            String message = "Attempted to pause a task that is not in the RUNNABLE state (task = " + task + ", state = " + task.getExecutionStatus();
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }
        try {
            ((InternalTaskInterface) task).setExecutionStatusImmediate(TaskExecutionStatus.WAITING, result);
            ((InternalTaskInterface) task).setWaitingReasonImmediate(reason, result);
        } catch (ObjectNotFoundException e) {
            String message = "A task cannot be paused, because it does not exist; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            throw e;
        } catch (SchemaException e) {
            String message = "A task cannot be paused due to schema exception; task = " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            throw e;
        }

        // make the trigger as it should be
        executionManager.synchronizeTask(task, result);

        if (result.isUnknown()) {
            result.computeStatus();
        }
    }

    // todo: better name for this method

    @Override
    public void unpauseTask(Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "unpauseTask");
        result.addArbitraryObjectAsParam("task", task);

        // Here can a race condition occur. If the parent was WAITING but has become SUSPENDED in the meanwhile,
        // this test could pass (seeing WAITING status) but the following unpause action is mistakenly executed
        // on suspended task, overwriting SUSPENDED status!
        //
        // Therefore scheduleWaitingTaskNow and makeWaitingTaskRunnable must make sure the task is (still) waiting.
        // The closeTask method is OK even if the task has become suspended in the meanwhile.
        if (task.getExecutionStatus() != TaskExecutionStatus.WAITING) {
            String message = "Attempted to unpause a task that is not in the WAITING state (task = " + task + ", state = " + task.getExecutionStatus();
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }
        if (task.getHandlerUri() == null) {
            LOGGER.trace("No handler in task being unpaused - closing it: {}", task);
            closeTask(task, result);
            ((InternalTaskInterface) task).checkDependentTasksOnClose(parentResult);       // TODO
            result.computeStatusIfUnknown();
            return;
        }

        TaskUnpauseActionType action = getUnpauseAction(task);
        switch (action) {
            case EXECUTE_IMMEDIATELY:
                LOGGER.trace("Unpausing task using 'executeImmediately' action (scheduling it now): {}", task);
                scheduleWaitingTaskNow(task, result);
                break;
            case RESCHEDULE:
                if (task.isRecurring()) {
                    LOGGER.trace("Unpausing recurring task using 'reschedule' action (making it runnable): {}", task);
                    makeWaitingTaskRunnable(task, result);
                } else {
                    LOGGER.trace("Unpausing task using 'reschedule' action (closing it, because the task is single-run): {}", task);
                    closeTask(task, result);
                    ((InternalTaskInterface) task).checkDependentTasksOnClose(parentResult);       // TODO
                }
                break;
            case CLOSE:
                LOGGER.trace("Unpausing task using 'close' action: {}", task);
                closeTask(task, result);
                ((InternalTaskInterface) task).checkDependentTasksOnClose(parentResult);       // TODO
                break;
            default:
                throw new IllegalStateException("Unsupported unpause action: " + action);
        }
        result.computeStatusIfUnknown();
    }

    @NotNull
    private TaskUnpauseActionType getUnpauseAction(Task task) {
        if (task.getUnpauseAction() != null) {
            return task.getUnpauseAction();
        } else if (task.isSingle()) {
            return TaskUnpauseActionType.EXECUTE_IMMEDIATELY;
        } else {
            return TaskUnpauseActionType.RESCHEDULE;
        }
    }

    @Override
    public void resumeTasks(Collection<String> taskOids, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "resumeTasks");
        for (String oid : taskOids) {
            try {
                resumeTask(getTaskPlain(oid, result), result);
            } catch (ObjectNotFoundException e) {           // result is already updated
                LoggingUtils.logException(LOGGER, "Couldn't resume task with OID {}", e, oid);
            } catch (SchemaException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resume task with OID {}", e, oid);
            }
        }
        result.computeStatus();
    }

    @Override
    public void resumeTask(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        resumeTask(getTaskPlain(taskOid, parentResult), parentResult);
    }

    @Override
    public void resumeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "resumeTask");
        result.addArbitraryObjectAsParam("task", task);
        try {

            if (task.getExecutionStatus() != TaskExecutionStatus.SUSPENDED &&
                    !(task.getExecutionStatus() == TaskExecutionStatus.CLOSED && task.isRecurring())) {
                String message =
                        "Attempted to resume a task that is not in the SUSPENDED state (or CLOSED for recurring tasks) (task = "
                                + task + ", state = " + task.getExecutionStatus();
                LOGGER.error(message);
                result.recordFatalError(message);
                return;
            }
            clearTaskOperationResult(task, parentResult);           // see a note on scheduleTaskNow
            if (task.getStateBeforeSuspend() == TaskExecutionStatusType.WAITING) {
                List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_EXECUTION_STATUS).replace(TaskExecutionStatusType.WAITING)
                        .item(TaskType.F_STATE_BEFORE_SUSPEND).replace()
                        .asItemDeltas();
                ((InternalTaskInterface) task).applyDeltasImmediate(itemDeltas, result);
            } else {
                List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(TaskType.class)
                        .item(TaskType.F_EXECUTION_STATUS).replace(TaskExecutionStatusType.RUNNABLE)
                        .item(TaskType.F_STATE_BEFORE_SUSPEND).replace()
                        .asItemDeltas();
                ((InternalTaskInterface) task).applyDeltasImmediate(itemDeltas, result);
                executionManager.synchronizeTask(task, result);
            }
        } catch (ObjectAlreadyExistsException t) {
            result.recordFatalError("Couldn't resume task: " + t.getMessage(), t);
            throw new IllegalStateException("Unexpected exception while resuming task " + task + ": " + t.getMessage(), t);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't resume task: " + t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void makeWaitingTaskRunnable(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, PreconditionViolationException {

        try {
            ((InternalTaskInterface) task).setExecutionStatusImmediate(TaskExecutionStatus.RUNNABLE, TaskExecutionStatusType.WAITING, result);
        } catch (ObjectNotFoundException e) {
            String message = "A task cannot be made runnable, because it does not exist; task = " + task;
            LoggingUtils.logException(LOGGER, message, e);
            throw e;
        } catch (SchemaException | PreconditionViolationException e) {
            String message = "A task cannot be made runnable; task = " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            throw e;
        }

        // make the trigger as it should be
        executionManager.synchronizeTask(task, result);
    }
    //endregion

    //region Working with task instances (other than suspend/resume)
    /*
     *  ********************* WORKING WITH TASK INSTANCES *********************
     */

    @Override
    public TaskQuartzImpl createTaskInstance() {
        return createTaskInstance(null);
    }

    @Override
    public TaskQuartzImpl createTaskInstance(String operationName) {
        return TaskQuartzImpl.createNew(this, operationName);
    }

    LightweightIdentifier generateTaskIdentifier() {
        return lightweightIdentifierGenerator.generate();
    }

    @Override
    @NotNull
    public TaskQuartzImpl createTaskInstance(PrismObject<TaskType> taskPrism, OperationResult parentResult) throws SchemaException {
        return createTaskInstance(taskPrism, null, parentResult);
    }

    @Override
    @NotNull
    public TaskQuartzImpl createTaskInstance(PrismObject<TaskType> taskPrism, @Deprecated String operationName, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "createTaskInstance");
        result.addParam("taskPrism", taskPrism);

        TaskQuartzImpl task = TaskQuartzImpl.createFromPrismObject(this, taskPrism);
        task.resolveOwnerRef(result);
        result.recordSuccessIfUnknown();
        return task;
    }

    @Override
    @NotNull
    public TaskQuartzImpl getTaskPlain(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        return getTaskPlain(taskOid, null, parentResult);
    }

    @Override
    @NotNull
    public TaskQuartzImpl getTaskWithResult(String taskOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                .item(TaskType.F_RESULT).retrieve()
                .build();
        return getTaskPlain(taskOid, options, parentResult);
    }

    @Override
    public void switchToBackground(final Task task, OperationResult parentResult) {

        parentResult.recordStatus(OperationResultStatus.IN_PROGRESS, "Task switched to background");
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "switchToBackground");

        // if the task result was unknown, we change it to 'in-progress'
        // (and roll back this change if storing into repo fails...)
        boolean wasUnknown = false;
        try {
            if (task.getResult().isUnknown()) {
                wasUnknown = true;
                task.getResult().recordInProgress();
            }
            persist(task, result);
            result.recordSuccess();
        } catch (RuntimeException ex) {
            if (wasUnknown) {
                task.getResult().recordUnknown();
            }
            result.recordFatalError("Unexpected problem: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    private void persist(Task task, OperationResult parentResult) {
        if (task.getPersistenceStatus() == TaskPersistenceStatus.PERSISTENT) {
            // Task already persistent. Nothing to do.
            return;
        }

        if (task instanceof RunningTask) {
            throw new UnsupportedOperationException("Running task cannot be made persistent");
        }

        TaskQuartzImpl taskImpl = (TaskQuartzImpl) task;

        if (task.getName() == null) {
            PolyStringType polyStringName = new PolyStringType("Task " + task.getTaskIdentifier());
            taskImpl.setNameTransient(polyStringName);
        }

        if (taskImpl.getOid() != null) {
            // We don't support user-specified OIDs
            throw new IllegalArgumentException("Transient task must not have OID (task:" + task + ")");
        }

        // hack: set Category if it is not set yet
        if (taskImpl.getCategory() == null) {
            taskImpl.setCategoryTransient(taskImpl.getCategoryFromHandler());
        }

        try {
            CryptoUtil.encryptValues(protector, taskImpl.getLiveTaskObjectForNotRunningTasks());
            addTaskToRepositoryAndQuartz(taskImpl, null, parentResult);
        } catch (ObjectAlreadyExistsException ex) {
            // This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
            throw new IllegalStateException("Got ObjectAlreadyExistsException while not expecting it (task:" + task + ")", ex);
        } catch (SchemaException ex) {
            // This should not happen. If it does, it is a bug. It is OK to convert to a runtime exception
            throw new IllegalStateException("Got SchemaException while not expecting it (task:" + task + ")", ex);
        } catch (EncryptionException e) {
            // TODO handle this better
            throw new SystemException("Couldn't encrypt plain text values in " + task + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String addTask(PrismObject<TaskType> taskPrism, RepoAddOptions options, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "addTask");
        if (taskPrism.asObjectable().getOwnerRef() == null) {
            try {
                MidPointPrincipal principal = SecurityUtil.getPrincipal();
                if (principal != null) {
                    ObjectReferenceType newOwnerRef = ObjectTypeUtil.createObjectRef(principal.getFocus(), prismContext);
                    taskPrism.asObjectable().setOwnerRef(newOwnerRef);
                }
            } catch (SecurityViolationException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine logged-in user. Task owner was not set.", e);
            }
        }
        TaskQuartzImpl task = createTaskInstance(taskPrism, result);            // perhaps redundant, but it's more convenient to work with Task than with Task prism
        if (task.getTaskIdentifier() == null) {
            task.setTaskIdentifier(generateTaskIdentifier().toString());
        }
        String oid = addTaskToRepositoryAndQuartz(task, options, result);
        result.computeStatus();
        return oid;
    }

    private String addTaskToRepositoryAndQuartz(Task task, RepoAddOptions options,
            OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {

        if (task instanceof RunningTask && ((RunningTask) task).isLightweightAsynchronousTask()) {
            throw new IllegalStateException("A task with lightweight task handler cannot be made persistent; task = " + task);
            // otherwise, there would be complications on task restart: the task handler is not stored in the repository,
            // so it is just not possible to restart such a task
        }

        OperationResult result = parentResult.createSubresult(DOT_IMPL_CLASS + "addTaskToRepositoryAndQuartz");
        result.addArbitraryObjectAsParam("task", task);

        String oid;
        try {
            oid = repositoryService.addObject(task.getUpdatedTaskObject(), options, result);
        } catch (ObjectAlreadyExistsException | SchemaException e) {
            result.recordFatalError("Couldn't add task to repository: " + e.getMessage(), e);
            throw e;
        }

        ((InternalTaskInterface) task).setOid(oid);

        synchronizeTaskWithQuartz(task, result);

        result.computeStatus();
        return oid;
    }

    @Override
    public void modifyTask(String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "modifyTask");
        try {
            repositoryService.modifyObject(TaskType.class, oid, modifications, result);
            TaskQuartzImpl task = getTaskPlain(oid, result);
            task.setRecreateQuartzTrigger(true);
            synchronizeTaskWithQuartz(task, result);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void suspendAndDeleteTasks(Collection<String> taskOids, long suspendTimeout, boolean alsoSubtasks, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "suspendAndDeleteTasks");
        result.addArbitraryObjectCollectionAsParam("taskOids", taskOids);

        List<Task> tasksToBeDeleted = new ArrayList<>();
        for (String oid : taskOids) {
            try {
                Task task = getTaskPlain(oid, result);
                tasksToBeDeleted.add(task);
                if (alsoSubtasks) {
                    tasksToBeDeleted.addAll(task.listSubtasksDeeply(true, result));
                }
            } catch (ObjectNotFoundException e) {
                // just skip suspending/deleting this task. As for the error, it should be already put into result.
                LoggingUtils.logException(LOGGER, "Error when retrieving task {} or its subtasks before the deletion. Skipping the deletion for this task.", e, oid);
            } catch (SchemaException e) {
                // same as above
                LoggingUtils.logUnexpectedException(LOGGER, "Error when retrieving task {} or its subtasks before the deletion. Skipping the deletion for this task.", e, oid);
            } catch (RuntimeException e) {
                result.createSubresult(DOT_IMPL_CLASS + "getTaskTree").recordPartialError("Unexpected error when retrieving task tree for " + oid + " before deletion", e);
                LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error when retrieving task {} or its subtasks before the deletion. Skipping the deletion for this task.", e, oid);
            }
        }

        List<Task> tasksToBeSuspended = new ArrayList<>();
        for (Task task : tasksToBeDeleted) {
            if (task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
                tasksToBeSuspended.add(task);
            }
        }

        // now suspend the tasks before deletion
        if (!tasksToBeSuspended.isEmpty()) {
            suspendTasksInternalQuietly(tasksToBeSuspended, suspendTimeout, false, result);
        }

        // delete them
        for (Task task : tasksToBeDeleted) {
            try {
                deleteTask(task.getOid(), result);
            } catch (ObjectNotFoundException e) {   // in all cases (even RuntimeException) the error is already put into result
                LoggingUtils.logException(LOGGER, "Error when deleting task {}", e, task);
            } catch (SchemaException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Error when deleting task {}", e, task);
            }
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }
    }

    @Override
    public void suspendAndDeleteTask(String taskOid, long suspendTimeout, boolean alsoSubtasks, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "suspendAndDeleteTask");
        result.addParam("taskOid", taskOid);

        List<Task> tasksToBeDeleted = new ArrayList<>();
        Task thisTask = getTaskPlain(taskOid, result);
        tasksToBeDeleted.add(thisTask);
        if (alsoSubtasks) {
            tasksToBeDeleted.addAll(thisTask.listSubtasksDeeply(true, result));
        }

        List<Task> tasksToBeSuspended = new ArrayList<>();
        for (Task task : tasksToBeDeleted) {
            if (task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
                tasksToBeSuspended.add(task);
            }
        }

        // now suspend the tasks before deletion
        if (!tasksToBeSuspended.isEmpty()) {
            suspendTasksInternalQuietly(tasksToBeSuspended, suspendTimeout, false, result);
        }

        // delete them, using delayed exception throwing
        SchemaException schemaException = null;
        ObjectNotFoundException objectNotFoundException = null;
        RuntimeException runtimeException = null;
        for (Task task : tasksToBeDeleted) {
            try {
                deleteTask(task.getOid(), result);
            } catch (ObjectNotFoundException e) {   // in all cases (even RuntimeException) the error is already put into result
                LoggingUtils.logException(LOGGER, "Error when deleting task {}", e, task);
                objectNotFoundException = e;
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Error when deleting task {}", e, task);
                schemaException = e;
            } catch (RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Error when deleting task {}", e, task);
                runtimeException = e;
            }
        }

        if (schemaException != null) {
            throw schemaException;
        } else if (objectNotFoundException != null) {
            throw objectNotFoundException;
        } else if (runtimeException != null) {
            throw runtimeException;
        }

        if (result.isUnknown()) {
            result.computeStatus();
        }
    }

    @Override
    public void deleteTask(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "deleteTask");
        result.addParam("oid", oid);
        try {
            Task task = getTaskPlain(oid, result);
            if (task.getNode() != null) {
                result.recordWarning("Deleting a task that seems to be currently executing on node " + task.getNode());
            }
            for (TaskDeletionListener listener : taskDeletionListeners) {
                listener.onTaskDelete(task, result);
            }
            repositoryService.deleteObject(TaskType.class, oid, result);
            executionManager.removeTaskFromQuartz(oid, result);
            result.computeStatusIfUnknown();
        } catch (ObjectNotFoundException e) {
            result.recordFatalError("Cannot delete the task because it does not exist.", e);
            throw e;
        } catch (SchemaException e) {
            result.recordFatalError("Cannot delete the task because of schema exception.", e);
            throw e;
        } catch (RuntimeException e) {
            result.recordFatalError("Cannot delete the task because of a runtime exception.", e);
            throw e;
        }
    }

    public void registerRunningTask(RunningTaskQuartzImpl task) {
        synchronized (locallyRunningTaskInstancesMap) {
            locallyRunningTaskInstancesMap.put(task.getTaskIdentifier(), task);
            LOGGER.trace("Registered task {}, locally running instances = {}", task, locallyRunningTaskInstancesMap);
        }
    }

    public void unregisterRunningTask(RunningTaskQuartzImpl task) {
        synchronized (locallyRunningTaskInstancesMap) {
            locallyRunningTaskInstancesMap.remove(task.getTaskIdentifier());
            LOGGER.trace("Unregistered task {}, locally running instances = {}", task, locallyRunningTaskInstancesMap);
        }
    }

    //endregion

    //region Transient and lightweight tasks
//    public void registerTransientSubtask(TaskQuartzImpl subtask, TaskQuartzImpl parent) {
//        Validate.notNull(subtask, "Subtask is null");
//        Validate.notNull(parent, "Parent task is null");
//        if (parent.isTransient()) {
//            registerTransientTask(parent);
//        }
//        registerTransientTask(subtask);
//    }
//
//    public void registerTransientTask(TaskQuartzImpl task) {
//        Validate.notNull(task, "Task is null");
//        Validate.notNull(task.getTaskIdentifier(), "Task identifier is null");
//        registeredTransientTasks.put(task.getTaskIdentifier(), task);
//    }

    void startLightweightTask(final RunningTaskQuartzImpl task) {
        if (task.isPersistent()) {
            throw new IllegalStateException("An attempt to start LightweightTaskHandler in a persistent task; task = " + task);
        }

        final LightweightTaskHandler lightweightTaskHandler = task.getLightweightTaskHandler();
        if (lightweightTaskHandler == null) {
            // nothing to do
            return;
        }

        synchronized (task) {
            if (task.lightweightHandlerStartRequested()) {
                throw new IllegalStateException("Handler for the lightweight task " + task + " has already been started.");
            }
            if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                throw new IllegalStateException("Handler for lightweight task " + task + " couldn't be started because the task's state is " + task.getExecutionStatus());
            }

            Runnable r = () -> {
                LOGGER.debug("Lightweight task handler shell starting execution; task = {}", task);

                try {
                    // Task owner is cloned because otherwise we get CME's when recomputing the owner user during login process
                    securityContextManager.setupPreAuthenticatedSecurityContext(CloneUtil.clone(task.getOwner()));
                } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't set up task security context {}", e, task);
                    throw new SystemException(e.getMessage(), e);
                }

                try {
                    task.setLightweightHandlerExecuting(true);
                    task.setExecutingThread(Thread.currentThread());
                    task.startCollectingLowLevelStatistics();
                    cacheConfigurationManager.setThreadLocalProfiles(task.getCachingProfiles());
                    OperationResult.setThreadLocalHandlingStrategy(task.getOperationResultHandlingStrategyName());
                    lightweightTaskHandler.run(task);
                    cacheConfigurationManager.unsetThreadLocalProfiles();
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Lightweight task handler has thrown an exception; task = {}", t, task);
                } finally {
                    task.setExecutingThread(null);
                    task.setLightweightHandlerExecuting(false);
                }
                LOGGER.debug("Lightweight task handler shell finishing; task = {}", task);
                try {
                    // TODO what about concurrency here??!
                    closeTask(task, task.getResult());
                    // commented out, as currently LATs cannot participate in dependency relationships
                    //task.checkDependentTasksOnClose(task.getResult());
                    // task.updateStoredTaskResult();   // has perhaps no meaning for transient tasks
                } catch (Exception e) {     // todo
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't correctly close task {}", e, task);
                }
            };

            Future future = lightweightHandlersExecutor.submit(r);
            task.setLightweightHandlerFuture(future);
            LOGGER.debug("Lightweight task handler submitted to start; task = {}", task);
        }
    }

    @Override
    public void waitForTransientChildren(RunningTask task, OperationResult result) {
        for (RunningTaskQuartzImpl subtask : ((RunningTaskQuartzImpl) task).getRunningLightweightAsynchronousSubtasks()) {
            Future future = subtask.getLightweightHandlerFuture();
            if (future != null) {       // should always be
                LOGGER.debug("Waiting for subtask {} to complete.", subtask);
                try {
                    future.get();
                } catch (CancellationException e) {
                    // the Future was cancelled; however, the run() method may be still executing
                    // we want to be sure it is already done
                    while (subtask.isLightweightHandlerExecuting()) {
                        LOGGER.debug("Subtask {} was cancelled, waiting for its real completion.", subtask);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {
                            LOGGER.warn("Waiting for subtask {} completion interrupted.", subtask);
                            break;
                        }
                    }
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Exception while waiting for subtask {} to complete.", t, subtask);
                    result.recordWarning("Got exception while waiting for subtask " + subtask + " to complete: " + t.getMessage(), t);
                }
                LOGGER.debug("Waiting for subtask {} done.", subtask);
            }
        }
    }

    //endregion

    //region Getting and searching for tasks and nodes

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type,
            String oid,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + ".getObject");
        result.addParam("objectType", type);
        result.addParam("oid", oid);
        result.addArbitraryObjectCollectionAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        try {
            if (TaskType.class.isAssignableFrom(type)) {
                GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
                if (GetOperationOptions.isRaw(rootOptions)) {
                    //noinspection unchecked
                    return (PrismObject<T>) repositoryService.getObject(TaskType.class, oid, options, result);
                } else {
                    Task task = getTask(oid, options, result);
                    //noinspection unchecked
                    return (PrismObject<T>) task.getUpdatedTaskObject();
                }
            } else if (NodeType.class.isAssignableFrom(type)) {
                //noinspection unchecked
                return (PrismObject<T>) repositoryService.getObject(NodeType.class, oid, options, result);      // TODO add transient attributes just like in searchObject
            } else {
                throw new IllegalArgumentException("Unsupported object type: " + type);
            }
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    @NotNull
    public TaskQuartzImpl getTaskPlain(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(DOT_IMPL_CLASS + "getTaskPlain");
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addArbitraryObjectCollectionAsParam(OperationResult.PARAM_OPTIONS, options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
        try {
            PrismObject<TaskType> taskPrism = repositoryService.getObject(TaskType.class, oid, options, result);
            return createTaskInstance(taskPrism, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    public TaskQuartzImpl getTask(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(DOT_IMPL_CLASS + "getTask");
        result.addParam(OperationResult.PARAM_OID, oid);
        result.addArbitraryObjectCollectionAsParam(OperationResult.PARAM_OPTIONS, options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);
        try {
            ClusterStatusInformation clusterStatusInformation = getClusterStatusInformation(options, TaskType.class,
                    true, result); // returns null if noFetch is set

            PrismObject<TaskType> taskPrism = getTaskInCluster(oid, options, clusterStatusInformation, result);
            if (taskPrism == null) {
                taskPrism = repositoryService.getObject(TaskType.class, oid, options, result);
            }

            TaskQuartzImpl task = createTaskInstance(taskPrism, result);

            addTransientTaskInformation(task,
                    clusterStatusInformation,
                    SelectorOptions.hasToLoadPath(TaskType.F_NEXT_RUN_START_TIMESTAMP, options),
                    SelectorOptions.hasToLoadPath(TaskType.F_NEXT_RETRY_TIMESTAMP, options),
                    SelectorOptions.hasToLoadPath(TaskType.F_NODE_AS_OBSERVED, options),
                    result);

            if (SelectorOptions.hasToLoadPath(TaskType.F_SUBTASK_REF, options)) {
                fillInSubtasks(task, clusterStatusInformation, options, result);
            }
            fillOperationExecutionState(task);
            return task;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private PrismObject<TaskType> getTaskInCluster(String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            ClusterStatusInformation clusterStatusInformation, OperationResult parentResult) throws SchemaException {

        NodeType runsAt = clusterStatusInformation.findNodeInfoForTask(oid);
        if (runsAt == null || isCurrentNode(runsAt.asPrismObject())) {
            return null;
        }

        final Holder<PrismObject<TaskType>> taskPrism = new Holder<>();
        clusterExecutionHelper.execute((client, node, opResult) -> {
                    Response response = client.path(TaskConstants.GET_TASK_REST_PATH + oid)
                            .query("include", GetOperationOptions.toRestIncludeOption(options))
                            .get();
                    Response.StatusType statusType = response.getStatusInfo();
                    if (statusType.getFamily() == Response.Status.Family.SUCCESSFUL) {
                        TaskType taskType = response.readEntity(TaskType.class);
                        taskPrism.setValue(taskType.asPrismObject());
                    } else {
                        LOGGER.warn("Cannot get task from {}", node);
                    }
            }, new ClusterExecutionOptions().tryAllNodes(), "load task (cluster)", parentResult);

        return taskPrism.getValue();
    }

    private void fillOperationExecutionState(Task task0) {
        TaskQuartzImpl task = (TaskQuartzImpl) task0;

        if (task.getTaskIdentifier() == null) {
            return;     // shouldn't really occur
        }

        RunningTask taskInMemory = getLocallyRunningTaskByIdentifier(task.getTaskIdentifier());
        if (taskInMemory == null) {
            return;
        }

        OperationStatsType operationStats = taskInMemory.getAggregatedLiveOperationStats();
        if (operationStats != null) {
            operationStats.setLiveInformation(true);
        }
        task.setOperationStatsTransient(operationStats);
        task.setProgressTransient(taskInMemory.getProgress());

        OperationResult result = taskInMemory.getResult();
        if (result != null) {
            try {
                task.setResultTransient(taskInMemory.getResult().clone());
            } catch (ConcurrentModificationException e) {
                // This can occur, see MID-3954/MID-4088. We will use operation result that was fetched from the repository
                // (it might be a bit outdated).
                LOGGER.warn("Concurrent access to operation result denied; using data from the repository (see MID-3954/MID-4088): {}", task, e);
            }
        } else {
            task.setResultTransient(null);
        }
    }

    // task is Task or TaskType
    // returns List<Task> or List<PrismObject<TaskType>>
    private List<?> getSubtasks(Object task, OperationResult result) throws SchemaException {
        if (task instanceof Task) {
            return ((Task) task).listSubtasks(result);
        } else if (task instanceof TaskType) {
            return listPersistentSubtasksForTask(((TaskType) task).getTaskIdentifier(), result);
        } else if (task instanceof PrismObject<?>) {
            //noinspection unchecked
            return listPersistentSubtasksForTask(((PrismObject<TaskType>) task).asObjectable().getTaskIdentifier(), result);
        } else {
            throw new IllegalArgumentException("task: " + task + " (of class " + (task != null ? task.getClass() : "null") + ")");
        }
    }

    // task is Task, TaskType or PrismObject<TaskType>
    // subtask is Task or PrismObject<TaskType>
    private void addSubtask(Object task, Object subtask) {
        TaskType subtaskBean;
        if (subtask instanceof TaskQuartzImpl) {
            subtaskBean = ((TaskQuartzImpl) subtask).getLiveTaskObject().asObjectable();
        } else if (subtask instanceof PrismObject<?>) {
            //noinspection unchecked
            subtaskBean = ((PrismObject<TaskType>) subtask).asObjectable();
        } else {
            throw new IllegalArgumentException("subtask: " + task);
        }

        if (task instanceof Task) {
            ((InternalTaskInterface) task).addSubtask(subtaskBean);
        } else if (task instanceof TaskType) {
            TaskTypeUtil.addSubtask((TaskType) task, subtaskBean, prismContext);
        } else if (task instanceof PrismObject<?>) {
            //noinspection unchecked
            TaskTypeUtil.addSubtask(((PrismObject<TaskType>) task).asObjectable(), subtaskBean, prismContext);
        } else {
            throw new IllegalArgumentException("task: " + task);
        }
    }

    private boolean isPersistent(Object task) {
        if (task instanceof Task) {
            return ((Task) task).isPersistent();
        } else if (task instanceof PrismObject<?>) {
            return ((PrismObject<?>) task).getOid() != null;
        } else {
            throw new IllegalArgumentException("task: " + task);
        }
    }

    // task is Task or TaskType
    private void fillInSubtasks(Object task, ClusterStatusInformation clusterStatusInformation, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        boolean retrieveNextRunStartTime = SelectorOptions.hasToLoadPath(TaskType.F_NEXT_RUN_START_TIMESTAMP, options);
        boolean retrieveRetryTime = SelectorOptions.hasToLoadPath(TaskType.F_NEXT_RETRY_TIMESTAMP, options);
        boolean retrieveNodeAsObserved = SelectorOptions.hasToLoadPath(TaskType.F_NODE_AS_OBSERVED, options);

        for (Object subtask : getSubtasks(task, result)) {
            if (isPersistent(subtask)) {
                addTransientTaskInformation(subtask, clusterStatusInformation, retrieveNextRunStartTime, retrieveRetryTime,
                        retrieveNodeAsObserved, result);
                fillInSubtasks(subtask, clusterStatusInformation, options, result);
            }
            addSubtask(task, subtask);
        }
    }

    List<PrismObject<TaskType>> listPersistentSubtasksForTask(String taskIdentifier, OperationResult result) throws SchemaException {
        if (StringUtils.isEmpty(taskIdentifier)) {
            return new ArrayList<>();
        }
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_PARENT).eq(taskIdentifier)
                .build();

        List<PrismObject<TaskType>> list;
        try {
            list = repositoryService.searchObjects(TaskType.class, query, null, result);
            result.recordSuccessIfUnknown();
        } catch (SchemaException | RuntimeException e) {
            result.recordFatalError(e);
            throw e;
        }
        return list;
    }

    @Override
    @NotNull
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "searchObjects");
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addArbitraryObjectCollectionAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        if (TaskType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            return (SearchResultList<PrismObject<T>>) (SearchResultList) searchTasks(query, options, result);
        } else if (NodeType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            return (SearchResultList<PrismObject<T>>) (SearchResultList) searchNodes(query, options, result);
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + type);
        }
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            ResultHandler<T> handler, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "searchObjects");
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addArbitraryObjectCollectionAsParam("options", options);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        SearchResultList<PrismObject<T>> objects;
        if (TaskType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            objects = (SearchResultList<PrismObject<T>>) (SearchResultList) searchTasks(query, options, result);
        } else if (NodeType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            objects = (SearchResultList<PrismObject<T>>) (SearchResultList) searchNodes(query, options, result);
        } else {
            throw new IllegalArgumentException("Unsupported object type: " + type);
        }

        for (PrismObject<T> object : objects) {
            handler.handle(object, result);
        }

        result.computeStatus();
        return objects.getMetadata();
    }

    @Override
    public <T extends ObjectType> int countObjects(
            Class<T> type, ObjectQuery query, OperationResult parentResult)
            throws SchemaException {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + ".countObjects");
        result.addParam("objectType", type);
        result.addParam("query", query);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        try {
            return repositoryService.countObjects(type, query, null, parentResult);
        } finally {
            result.computeStatus();
        }
    }



    /*
     * Gets nodes from repository and adds runtime information to them (taken from ClusterStatusInformation).
     */
//    @Override
//    public int countNodes(ObjectQuery query, OperationResult result) throws SchemaException {
//        return repositoryService.countObjects(NodeType.class, query, result);
//    }

    @NotNull
    private SearchResultList<PrismObject<NodeType>> searchNodes(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {

        ClusterStatusInformation clusterStatusInformation = getClusterStatusInformation(options, NodeType.class, true, result);

        List<PrismObject<NodeType>> nodesInRepository;
        try {
            nodesInRepository = repositoryService.searchObjects(NodeType.class, query, options, result);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't get nodes from repository: " + e.getMessage());
            throw e;
        }

        List<PrismObject<NodeType>> list = new ArrayList<>();

        if (clusterStatusInformation != null) {
            for (PrismObject<NodeType> nodeInRepositoryPrism : nodesInRepository) {
                NodeType returnedNode = nodeInRepositoryPrism.asObjectable();

                NodeType nodeRuntimeInfo = clusterStatusInformation.findNodeById(returnedNode.getNodeIdentifier());
                if (nodeRuntimeInfo != null) {
                    returnedNode.setExecutionStatus(nodeRuntimeInfo.getExecutionStatus());
                    returnedNode.setErrorStatus(nodeRuntimeInfo.getErrorStatus());
                    returnedNode.setConnectionResult(nodeRuntimeInfo.getConnectionResult());
                } else {
                    // node is in repo, but no information on it is present in CSI
                    // (should not occur except for some temporary conditions, because CSI contains info on all nodes from repo)
                    returnedNode.setExecutionStatus(NodeExecutionStatusType.COMMUNICATION_ERROR);
                    OperationResult r = new OperationResult("connect");
                    r.recordFatalError("Node not known at this moment");
                    returnedNode.setConnectionResult(r.createOperationResultType());
                }
                list.add(returnedNode.asPrismObject());
            }
        } else {
            list = nodesInRepository;
        }
        LOGGER.trace("searchNodes returning {}", list);
        result.computeStatus();
        return new SearchResultList<>(list);
    }

    @SuppressWarnings("SameParameterValue")
    private ClusterStatusInformation getClusterStatusInformation(Collection<SelectorOptions<GetOperationOptions>> options, Class<? extends ObjectType> objectClass, boolean allowCached, OperationResult result) {
        boolean noFetch = GetOperationOptions.isNoFetch(SelectorOptions.findRootOptions(options));
        boolean retrieveStatus;

        if (noFetch) {
            retrieveStatus = false;
        } else {
            if (objectClass.equals(TaskType.class)) {
                retrieveStatus = SelectorOptions.hasToLoadPath(TaskType.F_NODE_AS_OBSERVED, options);
            } else if (objectClass.equals(NodeType.class)) {
                retrieveStatus = true;                          // implement some determination algorithm if needed
            } else {
                throw new IllegalArgumentException("object class: " + objectClass);
            }
        }

        if (retrieveStatus) {
            return executionManager.getClusterStatusInformation(true, allowCached, result);
        } else {
            return null;
        }
    }

    private @NotNull SearchResultList<PrismObject<TaskType>> searchTasks(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {

        ClusterStatusInformation clusterStatusInformation = getClusterStatusInformation(options, TaskType.class, true, result); // returns null if noFetch is set

        List<PrismObject<TaskType>> tasksInRepository;
        try {
            tasksInRepository = repositoryService.searchObjects(TaskType.class, query, options, result);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't get tasks from repository: " + e.getMessage(), e);
            throw e;
        }

        boolean retrieveNextRunStartTime = SelectorOptions.hasToLoadPath(TaskType.F_NEXT_RUN_START_TIMESTAMP, options);
        boolean retrieveRetryTime = SelectorOptions.hasToLoadPath(TaskType.F_NEXT_RETRY_TIMESTAMP, options);
        boolean retrieveNodeAsObserved = SelectorOptions.hasToLoadPath(TaskType.F_NODE_AS_OBSERVED, options);
        boolean loadSubtasks = SelectorOptions.hasToLoadPath(TaskType.F_SUBTASK_REF, options);

        List<PrismObject<TaskType>> retval = new ArrayList<>();
        for (PrismObject<TaskType> taskInRepository : tasksInRepository) {
            addTransientTaskInformation(taskInRepository, clusterStatusInformation,
                    retrieveNextRunStartTime, retrieveRetryTime, retrieveNodeAsObserved, result);
            if (loadSubtasks) {
                fillInSubtasks(taskInRepository, clusterStatusInformation, options, result);
            }
            retval.add(taskInRepository);
        }
        result.computeStatus();
        return new SearchResultList<>(retval);
    }

    // task is Task or PrismObject<TaskType>
    private void addTransientTaskInformation(Object task, ClusterStatusInformation clusterStatusInformation,
            boolean retrieveNextRunStartTime, boolean retrieveRetryTime, boolean retrieveNodeAsObserved, OperationResult result) {

        if (!isPersistent(task)) {
            throw new IllegalStateException("Task " + task + " is not persistent");
        }
        if (task instanceof RunningTask) {
            throw new UnsupportedOperationException("addTransientTaskInformation is not available for running tasks");
        }
        TaskType taskBean;
        if (task instanceof TaskQuartzImpl) {
            taskBean = ((TaskQuartzImpl) task).getLiveTaskObjectForNotRunningTasks().asObjectable();
        } else if (task instanceof PrismObject<?>) {
            //noinspection unchecked
            taskBean = ((PrismObject<TaskType>) task).asObjectable();
        } else {
            throw new IllegalArgumentException("task: " + task);
        }

        if (clusterStatusInformation != null && retrieveNodeAsObserved) {
            NodeType runsAt = clusterStatusInformation.findNodeInfoForTask(taskBean.getOid());
            if (runsAt != null) {
                taskBean.setNodeAsObserved(runsAt.getNodeIdentifier());
            }
        }
        if (retrieveNextRunStartTime || retrieveRetryTime) {
            NextStartTimes times = getNextStartTimes(taskBean.getOid(), retrieveNextRunStartTime, retrieveRetryTime, result);
            if (retrieveNextRunStartTime && times.nextScheduledRun != null) {
                taskBean.setNextRunStartTimestamp(XmlTypeConverter.createXMLGregorianCalendar(times.nextScheduledRun));
            }
            if (retrieveRetryTime && times.nextRetry != null) {
                taskBean.setNextRetryTimestamp(XmlTypeConverter.createXMLGregorianCalendar(times.nextRetry));
            }
        }
        Long stalledSince = stalledTasksWatcher.getStalledSinceForTask(taskBean);
        if (stalledSince != null) {
            taskBean.setStalledSince(XmlTypeConverter.createXMLGregorianCalendar(stalledSince));
        }
    }

//    @Override
//    public int countTasks(ObjectQuery query, OperationResult result) throws SchemaException {
//        return repositoryService.countObjects(TaskType.class, query, result);
//    }
    //endregion

    //region Deletion listeners

    @Override
    public void registerTaskDeletionListener(TaskDeletionListener listener) {
        Validate.notNull(listener, "Task deletion listener is null");
        taskDeletionListeners.add(listener);
    }

    //endregion

    //region Managing handlers and task categories
    /*
     *  ********************* MANAGING HANDLERS AND TASK CATEGORIES *********************
     */

    @Override
    public void registerHandler(String uri, TaskHandler handler) {
        LOGGER.trace("Registering task handler for URI {}", uri);
        handlers.put(uri, handler);
        nonDeprecatedHandlersUris.put(uri, handler);
        primaryHandlersUris.put(uri, handler);
    }

    @Override
    public void registerAdditionalHandlerUri(String uri, TaskHandler handler) {
        LOGGER.trace("Registering additional URI for a task handler: {}", uri);
        nonDeprecatedHandlersUris.put(uri, handler);
        handlers.put(uri, handler);
    }

    @Override
    public void registerDeprecatedHandlerUri(String uri, TaskHandler handler) {
        LOGGER.trace("Registering additional (deprecated) URI for a task handler: {}", uri);
        handlers.put(uri, handler);
    }

    public TaskHandler getHandler(String uri) {
        if (uri != null) { return handlers.get(uri); } else { return null; }
    }

    @Deprecated // Remove in 4.2
    @Override
    public List<String> getAllTaskCategories() {
        Set<String> categories = new HashSet<>();
        for (TaskHandler h : primaryHandlersUris.values()) {
            List<String> cat = h.getCategoryNames();
            if (cat != null) {
                categories.addAll(cat);
            } else {
                String catName = h.getCategoryName(null);
                if (catName != null) {
                    categories.add(catName);
                }
            }
        }
        return new ArrayList<>(categories);
    }

    @Deprecated // Remove in 4.2
    @Override
    public String getHandlerUriForCategory(String category) {
        Set<String> found = new HashSet<>();
        for (Map.Entry<String, TaskHandler> h : primaryHandlersUris.entrySet()) {
            List<String> cats = h.getValue().getCategoryNames();
            if (cats != null) {
                if (cats.contains(category)) {
                    found.add(h.getKey());
                }
            } else {
                String cat = h.getValue().getCategoryName(null);
                if (category.equals(cat)) {
                    found.add(h.getKey());
                }
            }
        }
        if (found.isEmpty()) {
            return null;
        } else if (found.size() == 1) {
            return found.iterator().next();
        } else {
            LOGGER.warn("More task handlers found for category {}; returning none.", category);
            return null;
        }
    }

    @Override
    public Collection<String> getAllHandlerUris(boolean nonDeprecatedOnly) {
        return Collections.unmodifiableSet(getHandlerUriMap(nonDeprecatedOnly).keySet());
    }

    private Map<String, TaskHandler> getHandlerUriMap(boolean nonDeprecatedOnly) {
        return nonDeprecatedOnly ? nonDeprecatedHandlersUris : handlers;
    }

    @Override
    public Collection<String> getHandlerUrisForArchetype(String archetypeOid, boolean nonDeprecatedOnly) {
        return getHandlerUriMap(nonDeprecatedOnly).entrySet().stream()
                .filter(entry -> archetypeOid.equals(entry.getValue().getArchetypeOid()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    //endregion

    //region Task creation/removal listeners
    /*
     *  ********************* TASK CREATION/REMOVAL LISTENERS *********************
     */

    @Override
    public void onTaskCreate(String oid, OperationResult parentResult) {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "onTaskCreate");
        result.addParam("oid", oid);

        LOGGER.trace("onTaskCreate called for oid = " + oid);

        TaskQuartzImpl task;
        try {
            task = getTaskPlain(oid, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Quartz shadow job cannot be created, because task in repository was not found; oid = {}", e, oid);
            result.computeStatus();
            return;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Quartz shadow job cannot be created, because task from repository could not be retrieved; oid = {}", e, oid);
            result.computeStatus();
            return;
        }

        task.synchronizeWithQuartz(result);
        result.computeStatus();
    }

    @Override
    public void onTaskDelete(String oid, OperationResult parentResult) {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "onTaskDelete");
        result.addParam("oid", oid);

        LOGGER.trace("onTaskDelete called for oid = " + oid);

        JobKey jobKey = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);

        try {
            if (executionManager.getQuartzScheduler().checkExists(jobKey)) {
                executionManager.getQuartzScheduler().deleteJob(jobKey);            // removes triggers as well
            }
        } catch (SchedulerException e) {
            String message = "Quartz shadow job cannot be removed; oid = " + oid;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message);
        }

        result.recordSuccessIfUnknown();

    }
    //endregion

    //region Notifications
    @Override
    public void registerTaskListener(TaskListener taskListener) {
        taskListeners.add(taskListener);
    }

    @Override
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

    //endregion

    //region Other methods + getters and setters (CLEAN THIS UP)
    /*
     *  ********************* OTHER METHODS + GETTERS AND SETTERS *********************
     */

    PrismObjectDefinition<TaskType> getTaskObjectDefinition() {
        if (taskPrismDefinition == null) {
            taskPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class);
        }
        return taskPrismDefinition;
    }

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(TaskManagerQuartzImpl.class.getName() + "." + methodName);
    }

    @NotNull
    public TaskManagerConfiguration getConfiguration() {
        return configuration;
    }

    @NotNull
    public PrismContext getPrismContext() {
        return prismContext;
    }

    public SchemaHelper getSchemaHelper() {
        return schemaHelper;
    }

    public NodeErrorStatusType getLocalNodeErrorStatus() {
        return nodeErrorStatus;
    }

    public void setNodeErrorStatus(NodeErrorStatusType nodeErrorStatus) {
        this.nodeErrorStatus = nodeErrorStatus;
    }

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    MidpointConfiguration getMidpointConfiguration() {
        return midpointConfiguration;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public RelationRegistry getRelationRegistry() {
        return relationRegistry;
    }

    public ExecutionManager getExecutionManager() {
        return executionManager;
    }

    //endregion

    //region Delegation (CLEAN THIS UP)
    /*
     *  ********************* DELEGATIONS *********************
     */

    void synchronizeTaskWithQuartz(Task task, OperationResult parentResult) {
        executionManager.synchronizeTask(task, parentResult);
    }

    @Override
    public void synchronizeTasks(OperationResult result) {
        executionManager.synchronizeJobStores(result);
    }

    @Override
    public String getNodeId() {
        return configuration.getNodeId();
    }

    @Override
    public Collection<Task> getLocallyRunningTasks(OperationResult parentResult) {
        return executionManager.getLocallyRunningTasks(parentResult);
    }

    @Override
    public SchedulerInformationType getLocalSchedulerInformation(OperationResult parentResult) {
        return executionManager.getLocalSchedulerInformation(parentResult);
    }

    @Override
    public void stopLocalScheduler(OperationResult parentResult) {
        executionManager.stopLocalScheduler(parentResult);
    }

    @Override
    public void startLocalScheduler(OperationResult parentResult) {
        executionManager.startLocalScheduler(parentResult);
    }

    @Override
    public void stopLocalTask(String oid, OperationResult parentResult) {
        executionManager.stopLocalTask(oid, parentResult);
    }

    @Override
    public RunningTask getLocallyRunningTaskByIdentifier(String lightweightIdentifier) {
        synchronized (locallyRunningTaskInstancesMap) {
            return locallyRunningTaskInstancesMap.get(lightweightIdentifier);
        }
    }

    @Override
    public void stopScheduler(String nodeIdentifier, OperationResult parentResult) {
        executionManager.stopScheduler(nodeIdentifier, parentResult);
    }

    @Override
    public void stopSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
        OperationResult result = new OperationResult(DOT_INTERFACE + "stopSchedulers");
        for (String nodeIdentifier : nodeIdentifiers) {
            stopScheduler(nodeIdentifier, result);
        }
        result.computeStatus();
    }

    @Override
    public void startScheduler(String nodeIdentifier, OperationResult parentResult) {
        executionManager.startScheduler(nodeIdentifier, parentResult);
    }

    @Override
    public void startSchedulers(Collection<String> nodeIdentifiers, OperationResult parentResult) {
        OperationResult result = new OperationResult(DOT_INTERFACE + "startSchedulers");
        for (String nodeIdentifier : nodeIdentifiers) {
            startScheduler(nodeIdentifier, result);
        }
        result.computeStatus();
    }

    @Override
    public boolean stopSchedulersAndTasks(Collection<String> nodeIdentifiers, long timeToWait, OperationResult result) {
        return executionManager.stopSchedulersAndTasks(nodeIdentifiers, timeToWait, result);
    }

    @Override
    public boolean isCurrentNode(PrismObject<NodeType> node) {
        return clusterManager.isCurrentNode(node);
    }

    @Override
    public void deleteNode(String nodeOid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        clusterManager.deleteNode(nodeOid, result);
    }

    @Override
    public void scheduleTaskNow(String taskOid, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        scheduleTaskNow(getTaskPlain(taskOid, parentResult), parentResult);
    }

    @Override
    public void scheduleTaskNow(Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        /*
         *  Note: we clear task operation result because this is what a user would generally expect when re-running a task
         *  (MID-1920). We do NOT do that on each task run e.g. to have an ability to see last task execution status
         *  during a next task run. (When the interval between task runs is too short, e.g. for live sync tasks.)
         */
        if (task.isClosed()) {
            clearTaskOperationResult(task, parentResult);
            executionManager.reRunClosedTask(task, parentResult);
        } else if (task.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
            clearTaskOperationResult(task, parentResult);
            scheduleRunnableTaskNow(task, parentResult);
        } else if (task.getExecutionStatus() == TaskExecutionStatus.WAITING) {
            clearTaskOperationResult(task, parentResult);
            scheduleWaitingTaskNow(task, parentResult);
        } else {
            String message = "Task " + task + " cannot be run now, because it is not in RUNNABLE nor CLOSED state. State is " + task.getExecutionStatus();
            parentResult.createSubresult(DOT_INTERFACE + "scheduleTaskNow").recordFatalError(message);
            LOGGER.error(message);
        }
    }

    private void clearTaskOperationResult(Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult emptyTaskResult = new OperationResult("run");
        emptyTaskResult.setStatus(OperationResultStatus.IN_PROGRESS);
        task.setResultImmediate(emptyTaskResult, parentResult);
    }

    public void scheduleRunnableTaskNow(Task task, OperationResult parentResult) {
        executionManager.scheduleRunnableTaskNow(task, parentResult);
    }

    private void scheduleWaitingTaskNow(Task task, OperationResult parentResult) {
        executionManager.scheduleWaitingTaskNow(task, parentResult);
    }

    @Override
    public void scheduleTasksNow(Collection<String> taskOids, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "scheduleTasksNow");
        result.addArbitraryObjectCollectionAsParam("taskOids", taskOids);
        for (String oid : taskOids) {
            try {
                scheduleTaskNow(getTaskPlain(oid, result), result);
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't schedule task with OID {}", e, oid);
            } catch (SchemaException | RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't schedule task with OID {}", e, oid);
            }
        }
        result.computeStatus();
    }

    // use with care (e.g. w.r.t. dependent tasks)
    public void closeTask(Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        try {
            OperationResult taskResult = updateTaskResult(task, parentResult);
            task.close(taskResult, true, parentResult);
        } finally {
            if (task.isPersistent()) {
                executionManager.removeTaskFromQuartz(task.getOid(), parentResult);
            }
        }
    }

    private boolean shouldPurgeResult(Task task) {
        return PURGE_SUCCESSFUL_RESULT_FOR.contains(task.getCategory()) &&
                task.getResultStatus() == OperationResultStatusType.SUCCESS || task.getResultStatus() == OperationResultStatusType.IN_PROGRESS;
    }

    // do not forget to kick dependent tasks when closing this one (currently only done in finishHandler)
    void closeTaskWithoutSavingState(Task task, OperationResult parentResult) {
        try {
            OperationResult taskResult = updateTaskResult(task, parentResult);
            task.close(taskResult, false, parentResult);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new SystemException(e);       // shouldn't occur
        }
        executionManager.removeTaskFromQuartz(task.getOid(), parentResult);
    }

    // returns null if no change is needed in the task
    @Nullable
    private OperationResult updateTaskResult(Task task, OperationResult parentResult) throws SchemaException {
        OperationResult taskResult = task.getResult();
        if (taskResult == null) {
            try {
                task.refresh(parentResult);     // expecting to get the result
            } catch (ObjectNotFoundException e) {
                LOGGER.warn("Task result cannot be updated because the task is gone: {}", task, e);
                return null;
            }
            taskResult = task.getResult();
            if (taskResult == null) {
                LOGGER.warn("Null task result in {}", task);
                return null;
            }
        }
        boolean resultChanged = false;
        // this is a bit of magic to ensure closed tasks will not stay with IN_PROGRESS result (and, if possible, also not with UNKNOWN)
        if (taskResult.getStatus() == IN_PROGRESS || taskResult.getStatus() == UNKNOWN) {
            taskResult.computeStatus();
            if (taskResult.getStatus() == IN_PROGRESS) {
                taskResult.setStatus(SUCCESS);
            }
            resultChanged = true;
        }
        if (shouldPurgeResult(task)) {
            taskResult = OperationResult.keepRootOnly(taskResult);
            resultChanged = true;
        }
        return resultChanged ? taskResult : null;
    }

    @Override
    public ParseException validateCronExpression(String cron) {
        return TaskQuartzImplUtil.validateCronExpression(cron);
    }

    // currently finds only persistent tasks
    @Override
    @NotNull
    public Task getTaskByIdentifier(String identifier, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "getTaskByIdentifier");
        result.addParam("identifier", identifier);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        Task task = createTaskInstance(getTaskTypeByIdentifier(identifier, null, result), result);
        result.computeStatus();
        return task;
    }

    @Override
    @NotNull
    public PrismObject<TaskType> getTaskTypeByIdentifier(String identifier, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createMinorSubresult(DOT_IMPL_CLASS + "getTaskTypeByIdentifier");
        result.addParam("identifier", identifier);
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, TaskManagerQuartzImpl.class);

        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_TASK_IDENTIFIER).eq(identifier)
                .build();

        List<PrismObject<TaskType>> list = repositoryService.searchObjects(TaskType.class, query, options, result);
        if (list.isEmpty()) {
            throw new ObjectNotFoundException("Task with identifier " + identifier + " could not be found");
        } else if (list.size() > 1) {
            throw new IllegalStateException("Found more than one task with identifier " + identifier + " (" + list.size() + " of them)");
        }
        PrismObject<TaskType> retval = list.get(0);
        if (SelectorOptions.hasToLoadPath(TaskType.F_SUBTASK_REF, options)) {
            ClusterStatusInformation clusterStatusInformation = getClusterStatusInformation(options, TaskType.class, true, result); // returns null if noFetch is set
            fillInSubtasks(retval.asObjectable(), clusterStatusInformation, options, result);
        }
        result.computeStatusIfUnknown();
        return retval;
    }

    List<Task> resolveTasksFromTaskTypes(List<PrismObject<TaskType>> taskPrisms, OperationResult result) throws SchemaException {
        List<Task> tasks = new ArrayList<>(taskPrisms.size());
        for (PrismObject<TaskType> taskPrism : taskPrisms) {
            tasks.add(createTaskInstance(taskPrism, result));
        }
        result.recordSuccessIfUnknown();
        return tasks;
    }

    @Override
    public void cleanupTasks(CleanupPolicyType policy, RunningTask executionTask, OperationResult parentResult) throws SchemaException {
        if (policy.getMaxAge() == null) {
            return;
        }

        OperationResult result = parentResult.createSubresult(CLEANUP_TASKS);
        try {
            TimeBoundary timeBoundary = TimeBoundary.compute(policy.getMaxAge());
            XMLGregorianCalendar deleteTasksClosedUpTo = timeBoundary.boundary;

            LOGGER.info("Starting cleanup for closed tasks deleting up to {} (duration '{}').", deleteTasksClosedUpTo,
                    timeBoundary.positiveDuration);

            ObjectQuery obsoleteTasksQuery = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.CLOSED)
                    .and().item(TaskType.F_COMPLETION_TIMESTAMP).le(deleteTasksClosedUpTo)
                    .and().item(TaskType.F_PARENT).isNull()
                    .build();
            List<PrismObject<TaskType>> obsoleteTasks = repositoryService.searchObjects(TaskType.class, obsoleteTasksQuery, null, result);

            // enable when having enough time
            //        result.createSubresult(result.getOperation()+".count").recordStatus(SUCCESS, "Task tree(s) to be deleted: " + obsoleteTasks.size());
            //        // show the result immediately
            //        try {
            //            executionTask.setResultImmediate(executionTask.getResult(), new OperationResult("dummy"));
            //        } catch (ObjectNotFoundException e) {
            //            LoggingUtils.logUnexpectedException(LOGGER, "Task {} does not exist", e, executionTask);
            //        }

            LOGGER.debug("Found {} task tree(s) to be cleaned up", obsoleteTasks.size());

            boolean interrupted = false;
            int deleted = 0;
            int problems = 0;
            int bigProblems = 0;
            for (PrismObject<TaskType> rootTaskPrism : obsoleteTasks) {

                if (!executionTask.canRun()) {
                    result.recordWarning("Interrupted");
                    LOGGER.warn("Task cleanup was interrupted.");
                    interrupted = true;
                    break;
                }

                final String taskName = PolyString.getOrig(rootTaskPrism.getName());
                final String taskOid = rootTaskPrism.getOid();
                final long started = System.currentTimeMillis();
                executionTask.recordIterativeOperationStart(taskName, null, TaskType.COMPLEX_TYPE, taskOid);
                try {
                    // get whole tree
                    Task rootTask = createTaskInstance(rootTaskPrism, result);
                    List<Task> taskTreeMembers = rootTask.listSubtasksDeeply(true, result);
                    taskTreeMembers.add(rootTask);

                    LOGGER.trace("Removing task {} along with its {} children.", rootTask, taskTreeMembers.size() - 1);

                    Throwable lastProblem = null;
                    for (Task task : taskTreeMembers) {
                        try {
                            deleteTask(task.getOid(), result);
                            deleted++;
                        } catch (SchemaException | ObjectNotFoundException | RuntimeException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete obsolete task {}", e, task);
                            lastProblem = e;
                            problems++;
                            if (!task.getTaskIdentifier().equals(rootTask.getTaskIdentifier())) {
                                bigProblems++;
                            }
                        }
                    }
                    // approximate solution (as the problem might be connected to a subtask)
                    executionTask
                            .recordIterativeOperationEnd(taskName, null, TaskType.COMPLEX_TYPE, taskOid, started, lastProblem);
                } catch (Throwable t) {
                    executionTask.recordIterativeOperationEnd(taskName, null, TaskType.COMPLEX_TYPE, taskOid, started, t);
                    throw t;
                }
                executionTask.incrementProgressAndStoreStatsIfNeeded();
            }

            LOGGER.info("Task cleanup procedure " + (interrupted ? "was interrupted" : "finished")
                    + ". Successfully deleted {} tasks; there were problems with deleting {} tasks.", deleted, problems);
            if (bigProblems > 0) {
                LOGGER.error(
                        "{} subtask(s) couldn't be deleted. Inspect that manually, otherwise they might reside in repo forever.",
                        bigProblems);
            }
            String suffix = interrupted ? " Interrupted." : "";
            if (problems == 0) {
                parentResult.createSubresult(CLEANUP_TASKS + ".statistics")
                        .recordStatus(SUCCESS, "Successfully deleted " + deleted + " task(s)." + suffix);
            } else {
                parentResult.createSubresult(CLEANUP_TASKS + ".statistics")
                        .recordPartialError("Successfully deleted " + deleted + " task(s), "
                                + "there was problems with deleting " + problems + " tasks." + suffix
                                + (bigProblems > 0 ?
                                (" " + bigProblems + " subtask(s) couldn't be deleted, please see the log.") :
                                ""));
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public void cleanupNodes(DeadNodeCleanupPolicyType policy, RunningTask task, OperationResult parentResult) {
        if (policy.getMaxAge() == null) {
            return;
        }

        OperationResult result = parentResult.createSubresult(CLEANUP_NODES);
        try {
            TimeBoundary timeBoundary = TimeBoundary.compute(policy.getMaxAge());
            XMLGregorianCalendar deleteNodesNotCheckedInAfter = timeBoundary.boundary;

            LOGGER.info("Starting cleanup for stopped nodes not checked in after {} (duration '{}').",
                    deleteNodesNotCheckedInAfter, timeBoundary.positiveDuration);
            for (PrismObject<NodeType> node : clusterManager.getAllNodes(result)) {
                if (!task.canRun()) {
                    result.recordWarning("Interrupted");
                    LOGGER.warn("Node cleanup was interrupted.");
                    break;
                }
                if (!clusterManager.isCurrentNode(node) &&
                        !clusterManager.isCheckingIn(node.asObjectable()) &&
                        XmlTypeConverter.compareMillis(node.asObjectable().getLastCheckInTime(), deleteNodesNotCheckedInAfter) <= 0) {
                    // This includes last check in time == null
                    LOGGER.info("Deleting dead node {}; last check in time = {}", node, node.asObjectable().getLastCheckInTime());
                    String nodeName = PolyString.getOrig(node.getName());
                    long started = System.currentTimeMillis();
                    try {
                        task.recordIterativeOperationStart(nodeName, null, NodeType.COMPLEX_TYPE, node.getOid());
                        repositoryService.deleteObject(NodeType.class, node.getOid(), result);
                        task.recordIterativeOperationEnd(nodeName, null, NodeType.COMPLEX_TYPE, node.getOid(), started, null);
                    } catch (Throwable t) {
                        task.recordIterativeOperationEnd(nodeName, null, NodeType.COMPLEX_TYPE, node.getOid(), started, t);
                        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete dead node {}", t, node);
                    }
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // if there are problems with retrieving a task, we just log exception and put into operation result
    private List<Task> resolveTaskOids(Collection<String> oids, OperationResult parentResult) {
        List<Task> retval = new ArrayList<>();
        OperationResult result = parentResult.createMinorSubresult(DOT_IMPL_CLASS + ".resolveTaskOids");
        for (String oid : oids) {
            try {
                retval.add(getTaskPlain(oid, result));
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't retrieve task with OID {}", e, oid); // result is updated in getTask
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve task with OID {}", e, oid);
            }
        }
        result.computeStatus();
        return retval;
    }

    @Override
    public Long getNextRunStartTime(String oid, OperationResult parentResult) {
        return getNextStartTimes(oid, true, false, parentResult).nextScheduledRun;
    }

    public String getIntraClusterHttpUrlPattern() {
        return infrastructureConfiguration != null
                ? infrastructureConfiguration.getIntraClusterHttpUrlPattern()
                : null;
    }

    public Thread getTaskThread(String oid) {
        return executionManager.getTaskThread(oid);
    }

    public static class NextStartTimes {
        private final Long nextScheduledRun;
        private final Long nextRetry;

        public NextStartTimes(Trigger standardTrigger, Trigger nextRetryTrigger) {
            this.nextScheduledRun = getTime(standardTrigger);
            this.nextRetry = getTime(nextRetryTrigger);
        }

        private Long getTime(Trigger t) {
            return t != null && t.getNextFireTime() != null ? t.getNextFireTime().getTime() : null;
        }
    }

    @NotNull
    private NextStartTimes getNextStartTimes(
            String oid, boolean retrieveNextRunStartTime,
            boolean retrieveRetryTime, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(DOT_INTERFACE + "getNextStartTimes");
        result.addParam("oid", oid);
        result.addParam("retrieveNextRunStartTime", retrieveNextRunStartTime);
        result.addParam("retrieveRetryTime", retrieveRetryTime);
        return executionManager.getNextStartTimes(oid, retrieveNextRunStartTime, retrieveRetryTime, result);
    }

    public void checkStalledTasks(OperationResult result) {
        stalledTasksWatcher.checkStalledTasks(result);
    }

    //endregion

    //region Task housekeeping
    public void checkWaitingTasks(OperationResult result) throws SchemaException {
        int count = 0;
        List<Task> tasks = listWaitingTasks(TaskWaitingReason.OTHER_TASKS, result);
        for (Task task : tasks) {
            try {
                ((InternalTaskInterface) task).checkDependencies(result);
                count++;
            } catch (SchemaException | ObjectNotFoundException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't check dependencies for task {}", e, task);
            }
        }
        LOGGER.trace("Check waiting tasks completed; {} tasks checked.", count);
    }

    @SuppressWarnings("SameParameterValue")
    private List<Task> listWaitingTasks(TaskWaitingReason reason, OperationResult result) throws SchemaException {
        S_AtomicFilterEntry q = prismContext.queryFor(TaskType.class);
        q = q.item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING).and();
        if (reason != null) {
            q = q.item(TaskType.F_WAITING_REASON).eq(reason.toTaskType()).and();
        }
        ObjectQuery query = q.all().build();
        List<PrismObject<TaskType>> prisms = repositoryService.searchObjects(TaskType.class, query, null, result);
        List<Task> tasks = resolveTasksFromTaskTypes(prisms, result);

        result.recordSuccessIfUnknown();
        return tasks;
    }

    // returns map task lightweight id -> task
    public Map<String, RunningTaskQuartzImpl> getLocallyRunningTaskInstances() {
        synchronized (locallyRunningTaskInstancesMap) {    // must be synchronized while iterating over it (addAll)
            return new HashMap<>(locallyRunningTaskInstancesMap);
        }
    }

    Collection<Task> getTransientSubtasks(String identifier) {
        List<Task> retval = new ArrayList<>();
        RunningTaskQuartzImpl runningInstance = locallyRunningTaskInstancesMap.get(identifier);
        if (runningInstance != null) {
            for (RunningTaskQuartzImpl subtask : runningInstance.getLightweightAsynchronousSubtasks()) {
                retval.add(subtask.cloneAsStaticTask());
            }
        }
        return retval;
    }

    //endregion

    public SecurityContextManager getSecurityContextManager() {
        return securityContextManager;
    }

    public WorkStateManager getWorkStateManager() {
        return workStateManager;
    }

    /**
     * @return the handlerExecutor
     */
    public HandlerExecutor getHandlerExecutor() {
        return handlerExecutor;
    }

    @Override
    public ObjectQuery narrowQueryForWorkBucket(ObjectQuery query, Class<? extends ObjectType> type,
            Function<ItemPath, ItemDefinition<?>> itemDefinitionProvider, Task workerTask,
            WorkBucketType workBucket, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException {
        return workStateManager.narrowQueryForWorkBucket(workerTask, query, type, itemDefinitionProvider, workBucket, opResult);
    }

    @Override
    public TaskHandler createAndRegisterPartitioningTaskHandler(String handlerUri, Function<Task, TaskPartitionsDefinition> partitioningStrategy) {
        PartitioningTaskHandler handler = new PartitioningTaskHandler(this, partitioningStrategy);
        registerHandler(handlerUri, handler);
        return handler;
    }

    @Override
    public void setFreeBucketWaitInterval(long value) {
        workStateManager.setFreeBucketWaitIntervalOverride(value);
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    @Override
    public boolean isLocalNodeClusteringEnabled() {
        return configuration.isLocalNodeClusteringEnabled();
    }

    @Override
    public boolean isClustered() {
        return configuration.isClustered();
    }

    public SystemConfigurationChangeDispatcher getSystemConfigurationChangeDispatcher() {
        return systemConfigurationChangeDispatcher;
    }

    public ClusterExecutionHelper getClusterExecutionHelper() {
        return clusterExecutionHelper;
    }

    public Protector getProtector() {
        return protector;
    }

    @Override
    public void setWebContextPath(String path) {
        LOGGER.debug("setting webContextPath to '{}'", path);
        webContextPath = path;
    }

    public String getWebContextPath() {
        return webContextPath;
    }

    @Override
    public boolean update(@Nullable SystemConfigurationType value) {
        infrastructureConfiguration = value != null ? value.getInfrastructure() : null;
        return true;
    }

    @Override
    public String getRunningTasksThreadsDump(OperationResult parentResult) {
        return executionManager.getRunningTasksThreadsDump(parentResult);
    }

    @Override
    public String recordRunningTasksThreadsDump(String cause, OperationResult parentResult) throws ObjectAlreadyExistsException {
        return executionManager.recordRunningTasksThreadsDump(cause, parentResult);
    }

    @Override
    public String getTaskThreadsDump(String taskOid, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {
        return executionManager.getTaskThreadsDump(taskOid, parentResult);
    }

    @Override
    public String recordTaskThreadsDump(String taskOid, String cause, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        return executionManager.recordTaskThreadsDump(taskOid, cause, parentResult);
    }

    /**
     * Creates a running task from a regular one.
     * Should be used ONLY when creating running task instance for handler (standard or lightweight) run.
     */
    public RunningTaskQuartzImpl createRunningTask(Task task) {
        if (task instanceof RunningTask) {
            LOGGER.warn("Task {} is already a RunningTask", task);
            return ((RunningTaskQuartzImpl) task);
        } else {
            PrismObject<TaskType> taskPrismObject = task.getUpdatedTaskObject();
            return new RunningTaskQuartzImpl(this, taskPrismObject);
        }
    }

    @Override
    public RunningTaskQuartzImpl createFakeRunningTask(Task task) {
        RunningTaskQuartzImpl runningTask = createRunningTask(task);
        runningTask.setExecutingThread(Thread.currentThread());
        return runningTask;
    }

    @Override
    public NodeType getLocalNode() {
        return ObjectTypeUtil.asObjectable(clusterManager.getLocalNodeObject());
    }

    public CacheConfigurationManager getCacheConfigurationManager() {
        return cacheConfigurationManager;
    }

    @Override
    public boolean isDynamicProfilingEnabled() {
        return midpointConfiguration.getProfilingMode() == ProfilingMode.DYNAMIC;
    }

    @Override
    public Tracer getTracer() {
        return tracer;
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

    private static class TimeBoundary {
        private final Duration positiveDuration;
        private final XMLGregorianCalendar boundary;

        private TimeBoundary(Duration positiveDuration, XMLGregorianCalendar boundary) {
            this.positiveDuration = positiveDuration;
            this.boundary = boundary;
        }

        private static TimeBoundary compute(Duration rawDuration) {
            Duration positiveDuration = rawDuration.getSign() > 0 ? rawDuration : rawDuration.negate();
            XMLGregorianCalendar boundary = XmlTypeConverter.createXMLGregorianCalendar();
            boundary.add(positiveDuration.negate());
            return new TimeBoundary(positiveDuration, boundary);
        }
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
        NodeType localNode = getLocalNode();
        if (localNode == null) {
            // should not occur during regular operation
            return emptySet();
        } else {
            return Collections.unmodifiableCollection(localNode.getArchetypeRef());
        }
    }

    public CacheRegistry getCacheRegistry() {
        return cacheRegistry;
    }

    public CounterManager getCounterManager() {
        return counterManager;
    }
}
