/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.repo.api.CacheDispatcher;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.cluster.NodeRegistrar;
import com.evolveum.midpoint.task.quartzimpl.execution.*;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.task.quartzimpl.run.JobExecutor;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeErrorStateType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;

/**
 * Initializes the task manager and brings it down.
 */
@DependsOn("midpointConfiguration")
@Component
public class UpAndDown implements BeanFactoryAware {

    private static final Trace LOGGER = TraceManager.getTrace(UpAndDown.class);

    @Autowired private TaskManagerQuartzImpl taskManager;
    @Autowired private ClusterManager clusterManager;
    @Autowired private NodeRegistrar nodeRegistrar;
    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private LocalScheduler localScheduler;
    @Autowired private LocalNodeState localNodeState;
    @Autowired private Schedulers schedulers;
    @Autowired private LocalExecutionManager localExecutionManager;
    @Autowired private TaskSynchronizer taskSynchronizer;
    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private CacheDispatcher cacheDispatcher;
    private BeanFactory beanFactory;

    /**
     * How long to wait after TaskManager shutdown, if using JDBC Job Store.
     * This gives the JDBC thread pool a chance to close, before embedded H2 database server
     * would be closed by the SQL repo shutdown procedure.
     * The fact that H2 database is embedded is determined by {@link TaskManagerConfiguration#isDatabaseIsEmbedded()},
     * used in {@link #shutdown(OperationResult)} method.
     */
    private static final long WAIT_ON_SHUTDOWN = 2000;

    /**
     * Initialization.
     *
     * TaskManager can work in two modes:
     * - "stop on initialization failure" - it means that if TaskManager initialization fails, the midPoint will
     * not be started (implemented by throwing SystemException). This is a safe approach, however, midPoint could
     * be used also without Task Manager, so it is perhaps too harsh to do it this way.
     * - "continue on initialization failure" - after such a failure midPoint initialization simply continues;
     * however, task manager is switched to "Error" state, in which the scheduler cannot be started;
     * Moreover, actually almost none Task Manager methods can be invoked, to prevent a damage.
     *
     * This second mode is EXPERIMENTAL, should not be used in production for now.
     *
     * So,
     *
     * 1. Generally, when not initialized, we refuse to execute almost all operations (knowing for sure that
     * the scheduler is not running).
     *
     * 2. When initialized, but in error state (typically because of cluster misconfiguration not related to this node),
     * we refuse to start the scheduler on this node. Other methods are OK.
     */
    public void init(OperationResult result) {
        try {
            initInternal(result);
        } catch (TaskManagerInitializationException e) {
            throw new SystemException(e); // Mode 1: stop on initialization failure
        }
    }

    private void initInternal(OperationResult result) throws TaskManagerInitializationException {

        LOGGER.info("Task Manager initialization.");

        // get the configuration (general section + JDBC section as well)
        configuration.checkAllowedKeys(midpointConfiguration);
        configuration.setBasicInformation(midpointConfiguration, result);
        configuration.validateBasicInformation();

        LOGGER.info("Task Manager: Quartz Job Store: "
                + (configuration.isJdbcJobStore() ? "JDBC" : "in-memory") + ", "
                + (configuration.isClustered() ? "" : "NOT ") + "clustered. Threads: "
                + configuration.getThreads());

        if (configuration.isJdbcJobStore()) {
            // Let's find Quartz JDBC setup fallback (which will be used very likely)
            JdbcRepositoryConfiguration jdbcConfig = null;
            try {
                jdbcConfig = beanFactory.getBean(JdbcRepositoryConfiguration.class);
            } catch (NoSuchBeanDefinitionException e) {
                LOGGER.info("JdbcRepositoryConfiguration is not available, JDBC Job Store"
                        + " configuration will be taken from taskManager section only.");
                LOGGER.trace("Reason is", e);
            }

            configuration.setJdbcJobStoreInformation(midpointConfiguration, jdbcConfig);
            configuration.validateJdbcJobStoreInformation();
        }

        // register node
        NodeType node = nodeRegistrar.initializeNode(result); // may throw initialization exception
        if (!configuration.isTestMode()) { // in test mode do not start cluster manager thread nor verify cluster config
            clusterManager.checkClusterConfiguration(result); // Does not throw exceptions. Sets the ERROR state if necessary, however.
        }

        JobExecutor.setTaskManagerQuartzImpl(taskManager); // unfortunately, there seems to be no clean way of letting jobs know the taskManager

        localScheduler.initializeScheduler();
        if (localNodeState.getErrorState() == NodeErrorStateType.OK) {
            localScheduler.setLocalExecutionLimitations(node.getTaskExecutionLimitations());
        } else {
            localScheduler.shutdownScheduler();
        }

        // populate the scheduler with jobs (if RAM-based), or synchronize with midPoint repo
        if (!taskSynchronizer.synchronizeJobStores(result)) {
            if (!configuration.isJdbcJobStore()) {
                LOGGER.error("Some or all tasks could not be imported from midPoint repository to Quartz job store. They will therefore not be executed.");
            } else {
                LOGGER.warn("Some or all tasks could not be synchronized between midPoint repository and Quartz job store. They may not function correctly.");
            }
        }

        LOGGER.trace("Quartz scheduler initialized (not yet started, however)");
        LOGGER.info("Task Manager initialized");

        // if running in test mode, the postInit will not be executed... so we have to start scheduler here
        if (configuration.isTestMode()) {
            startSchedulerIfNeeded(result);
        }
    }

    private void startSchedulerIfNeeded(OperationResult result) {
        if (configuration.isSchedulerInitiallyStopped()) {
            LOGGER.info("Scheduler was not started because of system configuration 'schedulerInitiallyStopped' setting. You can start it manually if needed.");
        } else if (midpointConfiguration.isSafeMode()) {
            LOGGER.info("Scheduler was not started because the safe mode is ON. You can start it manually if needed.");
        } else {
            schedulers.startScheduler(taskManager.getNodeId(), result);
            if (result.getLastSubresultStatus() != SUCCESS) {
                throw new SystemException("Quartz task scheduler couldn't be started.");
            }
        }
    }

    void onSystemStarted(OperationResult result) {
        int delay = configuration.getNodeStartupDelay();
        if (delay > 0) {
            new Thread(() -> {
                try {
                    Thread.sleep(delay * 1000L);
                    switchFromStartingToUpState(result);
                } catch (InterruptedException e) {
                    LOGGER.warn("Got InterruptedException while waiting to switch to UP state; skipping the switch.");
                }
            }, "Delayed Starting to Up state transition").start();
        } else {
            switchFromStartingToUpState(result);
        }
    }

    private void switchFromStartingToUpState(OperationResult result) {
        clusterManager.registerNodeUp(result);
        cacheDispatcher.dispatchInvalidation(null, null, false, null);

        clusterManager.startClusterManagerThread();
        startSchedulerIfNeeded(result);
    }

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void shutdown(OperationResult result) {
        try {
            shutdownInternal(result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't bring TaskManager down, continuing", e);
            // intentionally not re-throwing the exception
        }
    }

    private void shutdownInternal(OperationResult result) {
        LOGGER.info("Task Manager shutdown starting");

        if (localScheduler.getQuartzScheduler() != null) {

            // This method has been called already. But better twice than not at all.
            // (We may remove this call if really not needed - some day.)
            stopLocalSchedulerAndTasks(result);

            if (configuration.isTestMode()) {
                LOGGER.info("Quartz scheduler will NOT be shutdown. It stays in paused mode.");
            } else {
                localScheduler.shutdownScheduler();
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

    void stopLocalSchedulerAndTasks(OperationResult result) {
        try {
            localExecutionManager.stopSchedulerAndTasks(TaskManager.WAIT_INDEFINITELY, result);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop local scheduler and tasks, continuing with the shutdown",
                    t);
        }
    }
}
