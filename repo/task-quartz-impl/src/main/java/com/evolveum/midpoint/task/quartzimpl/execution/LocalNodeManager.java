/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.repo.sql.DataSourceFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImplUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.sql.ScriptRunner;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeErrorStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStatusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.SchedulerListenerSupport;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages task threads on the local node. Concerned mainly with stopping threads and querying their state.
 *
 * @author Pavol Mederly
 */
public class LocalNodeManager {

    private static final Trace LOGGER = TraceManager.getTrace(LocalNodeManager.class);

    private TaskManagerQuartzImpl taskManager;

    LocalNodeManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }

    /*
    * =============== SCHEDULER-LEVEL ACTIONS ===============
    *
    * (used internally by TaskManager)
    */

    /**
     * Prepares Quartz scheduler. Configures its properties (based on Task Manager configuration) and creates the instance.
     * Does not start the scheduler, because this is done during post initialization.
     */
    void initializeScheduler() throws TaskManagerInitializationException {

        TaskManagerConfiguration configuration = taskManager.getConfiguration();
        Properties quartzProperties = new Properties();

        if (configuration.isJdbcJobStore()) {
            quartzProperties.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
            quartzProperties.put("org.quartz.jobStore.driverDelegateClass", configuration.getJdbcDriverDelegateClass());
            quartzProperties.put("org.quartz.jobStore.clusterCheckinInterval", String.valueOf(configuration.getQuartzClusterCheckinInterval()));
            quartzProperties.put("org.quartz.jobStore.clusterCheckinGracePeriod", String.valueOf(configuration.getQuartzClusterCheckinGracePeriod()));

            createQuartzDbSchema(configuration);

            final String myDs = "myDS";
            quartzProperties.put("org.quartz.jobStore.dataSource", myDs);
            if (configuration.isUseRepositoryConnectionProvider()) {
                DataSourceFactory dataSourceFactory = (DataSourceFactory) taskManager.getBeanFactory().getBean("dataSourceFactory");
                int index = (int) (Math.random() * Integer.MAX_VALUE);
                RepositoryConnectionProvider.DATA_SOURCES.put(index, dataSourceFactory.getDataSource());
                quartzProperties.put("org.quartz.dataSource."+myDs+".connectionProvider.class", RepositoryConnectionProvider.class.getName());
                quartzProperties.put("org.quartz.dataSource."+myDs+".dataSourceIndex", String.valueOf(index));
            } else if (configuration.getDataSource() != null) {
                quartzProperties.put("org.quartz.dataSource."+myDs+".jndiURL", configuration.getDataSource());
            } else {
                quartzProperties.put("org.quartz.dataSource."+myDs+".provider", "hikaricp");
                quartzProperties.put("org.quartz.dataSource."+myDs+".driver", configuration.getJdbcDriver());
                quartzProperties.put("org.quartz.dataSource."+myDs+".URL", configuration.getJdbcUrl());
                quartzProperties.put("org.quartz.dataSource."+myDs+".user", configuration.getJdbcUser());
                quartzProperties.put("org.quartz.dataSource."+myDs+".password", configuration.getJdbcPassword());
            }

            quartzProperties.put("org.quartz.jobStore.isClustered", configuration.isClustered() ? "true" : "false");

        } else {
            quartzProperties.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        }
        quartzProperties.put("org.quartz.scheduler.instanceName", "midPointScheduler");

        quartzProperties.put("org.quartz.scheduler.instanceId", taskManager.getNodeId());

        quartzProperties.put("org.quartz.scheduler.skipUpdateCheck", "true");
        quartzProperties.put("org.quartz.threadPool.threadCount", Integer.toString(configuration.getThreads()));

        // in test mode we set idleWaitTime to a lower value, because on some occasions
        // the Quartz scheduler "forgot" to fire a trigger immediately after creation,
        // and the default delay of 10s is too much for most of the tests.
        int schedulerLoopTime;
        if (configuration.isTestMode()) {
            if (configuration.isJdbcJobStore()) {
                schedulerLoopTime = 5000;
            } else {
                schedulerLoopTime = 2000;
            }
        } else {
            schedulerLoopTime = 10000;
        }

        quartzProperties.put("org.quartz.scheduler.idleWaitTime", Integer.toString(schedulerLoopTime));

        quartzProperties.put("org.quartz.scheduler.jmx.export", "true");

        if (configuration.isTestMode()) {
            LOGGER.info("QuartzScheduler is set to be reusable: the task manager threads will NOT be stopped on shutdown. Also, scheduler threads will run as daemon ones.");
            quartzProperties.put("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
            quartzProperties.put("org.quartz.threadPool.makeThreadsDaemons", "true");
        }

        LOGGER.info("Initializing Quartz scheduler (but not starting it yet).");
        // initialize the scheduler (without starting it)
        try {
            LOGGER.trace("Quartz scheduler properties: {}", quartzProperties);
            StdSchedulerFactory sf = new StdSchedulerFactory();
            sf.initialize(quartzProperties);
            Scheduler scheduler = sf.getScheduler();
            setMySchedulerListener(scheduler);
            getGlobalExecutionManager().setQuartzScheduler(scheduler);
            LOGGER.info("... Quartz scheduler initialized.");
        } catch (SchedulerException e) {
            throw new TaskManagerInitializationException("Cannot initialize the Quartz scheduler", e);
        }
    }

    private void setMySchedulerListener(Scheduler scheduler) throws SchedulerException {
        for (SchedulerListener listener : scheduler.getListenerManager().getSchedulerListeners()) {
            if (listener instanceof MySchedulerListener) {
                scheduler.getListenerManager().removeSchedulerListener(listener);
            }
        }
        scheduler.getListenerManager().addSchedulerListener(new MySchedulerListener());
    }

    private class MySchedulerListener extends SchedulerListenerSupport {
        @Override
        public void schedulerStarting() {
            OperationResult result = new OperationResult(LocalNodeManager.class.getName() + ".schedulerStarting");
            NodeType node = taskManager.getClusterManager().getFreshVerifiedLocalNodeObject(result);
            if (node != null) {
                Scheduler quartzScheduler = taskManager.getExecutionManager().getQuartzScheduler();
                if (quartzScheduler != null) {
                    getGlobalExecutionManager().setLocalExecutionLimitations(quartzScheduler, node.getTaskExecutionLimitations());
                }
            } else {
                LOGGER.warn("Couldn't set Quartz scheduler execution capabilities, because local node object couldn't be correctly read.");
            }
        }
    }

    /**
     * Creates Quartz database schema, if it does not exist.
     */
    private void createQuartzDbSchema(TaskManagerConfiguration configuration) throws TaskManagerInitializationException {
        Connection connection = getConnection(configuration);
        LOGGER.debug("createQuartzDbSchema: trying JDBC connection {}", connection);
        try {
            if (areQuartzTablesMissing(connection)) {

                if (configuration.isCreateQuartzTables()) {
                    try {
                        Reader scriptReader = getResourceReader(configuration.getSqlSchemaFile());

                        ScriptRunner runner = new ScriptRunner(connection, false, true);
                        runner.runScript(scriptReader);

                    } catch (IOException ex) {
                        throw new TaskManagerInitializationException("Could not read Quartz database schema file: " + configuration.getSqlSchemaFile(), ex);
                    } catch (SQLException e) {
                        throw new TaskManagerInitializationException("Could not create Quartz JDBC Job Store tables from " + configuration.getSqlSchemaFile(), e);
                    }

                    if (areQuartzTablesMissing(connection)) {
                        throw new TaskManagerInitializationException("Quartz tables seem not to exist even after running creation script.");
                    }
                } else {
                    throw new TaskManagerInitializationException("Quartz tables seem not to exist and their automatic creation is disabled (createQuartzTables is set to false).");
                }
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private Connection getConnection(TaskManagerConfiguration configuration) throws TaskManagerInitializationException {
        Connection connection;
        try {
            if (configuration.isUseRepositoryConnectionProvider()) {
                DataSourceFactory dataSourceFactory = (DataSourceFactory) taskManager.getBeanFactory().getBean("dataSourceFactory");
                connection = dataSourceFactory.getDataSource().getConnection();
            } else if (configuration.getDataSource() != null) {
                DataSource dataSource;
                try {
                    InitialContext context = new InitialContext();
                    dataSource = (DataSource) context.lookup(configuration.getDataSource());
                } catch (NamingException e) {
                    throw new TaskManagerInitializationException("Cannot find a data source '"+configuration.getDataSource()+"': " + e.getMessage(), e);
                }
                connection = dataSource.getConnection();
            } else {
                try {
                    Class.forName(configuration.getJdbcDriver());
                } catch (ClassNotFoundException e) {
                    throw new TaskManagerInitializationException("Could not locate database driver class " + configuration.getJdbcDriver(), e);
                }
                connection = DriverManager.getConnection(configuration.getJdbcUrl(), configuration.getJdbcUser(), configuration.getJdbcPassword());
            }
        } catch (SQLException e) {
            throw new TaskManagerInitializationException("Cannot create JDBC connection to Quartz Job Store", e);
        }
        return connection;
    }

    private boolean areQuartzTablesMissing(Connection connection) {
        //noinspection CatchMayIgnoreException
        try {
            connection.prepareStatement("SELECT count(*) FROM QRTZ_JOB_DETAILS").executeQuery().close();
            LOGGER.debug("Quartz tables seem to exist (at least QRTZ_JOB_DETAILS does).");
            return false;
        } catch (SQLException ignored) {
            LOGGER.debug("Quartz tables seem not to exist (at least QRTZ_JOB_DETAILS does not), we got an exception when trying to access it", ignored);
            return true;
        }
    }

    private Reader getResourceReader(String name) throws TaskManagerInitializationException {
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            throw new TaskManagerInitializationException("Quartz DB schema (" + name + ") cannot be found.");
        }
        return new BufferedReader(new InputStreamReader(stream));
    }

    private void pauseScheduler(OperationResult result) {

        LOGGER.info("Putting Quartz scheduler into standby mode");
        try {
            getQuartzScheduler().standby();
            result.recordSuccess();
        } catch (SchedulerException e1) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't put local Quartz scheduler into standby mode", e1);
            result.recordFatalError("Couldn't put local Quartz scheduler into standby mode", e1);
        }
    }

    void shutdownScheduler() throws TaskManagerException {

        LOGGER.info("Shutting down Quartz scheduler");
        try {
            if (getQuartzScheduler() != null && !getQuartzScheduler().isShutdown()) {
                getQuartzScheduler().shutdown(true);
            }
            LOGGER.info("Quartz scheduler was shut down");
        } catch (SchedulerException e) {
            throw new TaskManagerException("Cannot shutdown Quartz scheduler", e);
        }
    }

    private Scheduler getQuartzScheduler() {
        return getGlobalExecutionManager().getQuartzScheduler();
    }

    /*
     *  The following methods are used by external clients, e.g. the admin GUI.
     *  They do not throw exceptions.
     */

    boolean stopSchedulerAndTasks(long timeToWait, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(LocalNodeManager.class.getName() + ".stopSchedulerAndTasks");
        result.addParam("timeToWait", timeToWait);

        pauseScheduler(result);
        boolean tasksStopped = getGlobalExecutionManager().stopAllTasksOnThisNodeAndWait(timeToWait, result);
        LOGGER.info("Scheduler stopped; " + (tasksStopped ? "all task threads have been stopped as well." : "some task threads may still run."));
        result.recordSuccessIfUnknown();
        return tasksStopped;
    }

    void stopScheduler(OperationResult result) {
        pauseScheduler(result);
    }

    void startScheduler(OperationResult result) {

        if (taskManager.isInErrorState()) {
            String message = "Cannot start the scheduler, because Task Manager is in error state (" + taskManager.getLocalNodeErrorStatus() + ")";
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }

        try {
            LOGGER.info("Starting the Quartz scheduler");
            getQuartzScheduler().start();
            LOGGER.debug("Quartz scheduler started.");
            result.recordSuccess();
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot (re)start Quartz scheduler.", e);
            result.recordFatalError("Cannot (re)start Quartz scheduler.", e);
        }
    }

    NodeExecutionStatusType getLocalNodeExecutionStatus() {

        if (taskManager.getLocalNodeErrorStatus() != NodeErrorStatusType.OK) {
            return NodeExecutionStatusType.ERROR;
        } else {
            Boolean quartzRunning = isQuartzSchedulerRunning();
            if (quartzRunning == null) {      // this should not occur if error status is OK
                return NodeExecutionStatusType.COMMUNICATION_ERROR;
            } else {
                return quartzRunning ? NodeExecutionStatusType.RUNNING : NodeExecutionStatusType.PAUSED;
            }
        }
    }

    private Boolean isQuartzSchedulerRunning() {
        Scheduler quartzScheduler = getGlobalExecutionManager().getQuartzScheduler();
        if (quartzScheduler == null) {
            return null;
        }
        try {
            return quartzScheduler.isStarted() && !quartzScheduler.isInStandbyMode() && !quartzScheduler.isShutdown();
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot determine Quartz scheduler state", e);
            return null;
        }
    }

    public boolean isRunning() {
        Boolean retval = isQuartzSchedulerRunning();
        return retval != null && retval;
    }

    /*
    * ==================== STOP TASK METHODS: "soft" and "hard" ====================
    *
    * soft = set 'canRun' to false
    * hard = call Thread.interrupt
    */

    // local task should be running
    void stopLocalTaskRun(String oid, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(LocalNodeManager.class.getName() + ".stopLocalTaskRun");
        result.addParam("task", oid);

        LOGGER.info("Stopping local task {} run", oid);

        try {
            getQuartzScheduler().interrupt(TaskQuartzImplUtil.createJobKeyForTaskOid(oid));
            result.recordSuccess();
        } catch (UnableToInterruptJobException e) {
            String message = "Unable to interrupt the task " + oid;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);            // however, we continue (e.g. to suspend the task)
            result.recordFatalError(message, e);
        }
    }

    /**
     * Calls Thread.interrupt() on a local thread hosting task with a given OID.
     */
    void interruptLocalTaskThread(String oid) {

        LOGGER.trace("Trying to find and interrupt a local execution thread for task {} (if it exists).", oid);
        try {
            List<JobExecutionContext> jecs = getQuartzScheduler().getCurrentlyExecutingJobs();
            for (JobExecutionContext jec : jecs) {
                String oid1 = jec.getJobDetail().getKey().getName();
                if (oid.equals(oid1)) {
                    Job job = jec.getJobInstance();
                    if (job instanceof JobExecutor) {
                        JobExecutor jobExecutor = (JobExecutor) job;
                        jobExecutor.sendThreadInterrupt();
                    }
                    break;
                }
            }

        } catch (SchedulerException e1) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot find the currently executing job for the task {}", e1, oid);
            // ...and ignore it.
        }
    }

    /*
    * ==================== THREAD QUERY METHODS ====================
    */

    boolean isTaskThreadActiveLocally(String oid) {

        try {
            for (JobExecutionContext jec : getQuartzScheduler().getCurrentlyExecutingJobs()) {
                if (oid.equals(jec.getJobDetail().getKey().getName())) {
                    return true;
                }
            }
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get the list of currently executing jobs", e);
            return false;
        }
        return false;
    }

    Set<String> getLocallyRunningTasksOids(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(LocalNodeManager.class.getName() + ".getLocallyRunningTasksOids");
        try {
            List<JobExecutionContext> jobs = getQuartzScheduler().getCurrentlyExecutingJobs();
            Set<String> oids = jobs.stream().map(ec -> ec.getJobDetail().getKey().getName()).collect(Collectors.toSet());
            result.recordSuccess();
            return oids;
        } catch (Throwable t) {
            String message = "Cannot get the list of currently executing jobs on local node.";
            result.recordFatalError(message, t);
            LoggingUtils.logUnexpectedException(LOGGER, message, t);
            return Collections.emptySet();      // todo or throw an exception?
        }
    }

    @Nullable
    Thread getLocalTaskThread(@NotNull String oid) {
        try {
            for (JobExecutionContext jec : getQuartzScheduler().getCurrentlyExecutingJobs()) {
                if (oid.equals(jec.getJobDetail().getKey().getName())) {
                    Job job = jec.getJobInstance();
                    if (job instanceof JobExecutor) {
                        return ((JobExecutor) job).getExecutingThread();
                    }
                }
            }
        } catch (SchedulerException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get the list of currently executing jobs", e);
        }
        return null;
    }

    /**
     * Returns all the currently executing tasks.
     */
    Collection<Task> getLocallyRunningTasks(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(LocalNodeManager.class.getName() + ".getLocallyRunningTasks");

        List<Task> retval = new ArrayList<>();

        for (String oid : getLocallyRunningTasksOids(result)) {
            OperationResult result1 = result.createSubresult(LocalNodeManager.class.getName() + ".getLocallyRunningTask");
            try {
                retval.add(taskManager.getTaskPlain(oid, result1));
                result1.recordSuccess();
            } catch (ObjectNotFoundException e) {
                String m = "Cannot get the task with OID " + oid + " as it no longer exists";
                LoggingUtils.logException(LOGGER, m, e);
                result1.recordHandledError(m, e);               // it's OK, the task could disappear in the meantime
            } catch (SchemaException e) {
                String m = "Cannot get the task with OID " + oid + " due to schema problems";
                LoggingUtils.logUnexpectedException(LOGGER, m, e);
                result1.recordFatalError(m, e);
            }
        }

        result.computeStatus();

        return retval;
    }

    /*
     * Various auxiliary methods
     */

    private ExecutionManager getGlobalExecutionManager() {
        return taskManager.getExecutionManager();
    }

}

