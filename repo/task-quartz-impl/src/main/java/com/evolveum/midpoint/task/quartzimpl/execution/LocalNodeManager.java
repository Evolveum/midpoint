/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeErrorStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeExecutionStatusType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
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

/**
 * Manages task threads on the local node. Concerned mainly with stopping threads and querying their state.
 *
 * @author Pavol Mederly
 */
public class LocalNodeManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(LocalNodeManager.class);

    private TaskManagerQuartzImpl taskManager;

    public LocalNodeManager(TaskManagerQuartzImpl taskManager) {
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
     *
     * @throws com.evolveum.midpoint.task.api.TaskManagerInitializationException
     */
    void initializeScheduler() throws TaskManagerInitializationException {

        TaskManagerConfiguration configuration = taskManager.getConfiguration();
        Properties quartzProperties = new Properties();

        if (configuration.isJdbcJobStore()) {
            quartzProperties.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
            quartzProperties.put("org.quartz.jobStore.driverDelegateClass", configuration.getJdbcDriverDelegateClass());

            createQuartzDbSchema(configuration);

            String MY_DS = "myDS";
            quartzProperties.put("org.quartz.jobStore.dataSource", MY_DS);
            if (configuration.getDataSource() != null) {
                quartzProperties.put("org.quartz.dataSource."+MY_DS+".jndiURL", configuration.getDataSource());
            } else {
                quartzProperties.put("org.quartz.dataSource."+MY_DS+".provider", "hikaricp");
                quartzProperties.put("org.quartz.dataSource."+MY_DS+".driver", configuration.getJdbcDriver());
                quartzProperties.put("org.quartz.dataSource."+MY_DS+".URL", configuration.getJdbcUrl());
                quartzProperties.put("org.quartz.dataSource."+MY_DS+".user", configuration.getJdbcUser());
                quartzProperties.put("org.quartz.dataSource."+MY_DS+".password", configuration.getJdbcPassword());
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
        // the Quartz scheduler "forgots" to fire a trigger immediately after creation,
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
     *
     * @throws TaskManagerInitializationException
     * @param configuration
     */
    private void createQuartzDbSchema(TaskManagerConfiguration configuration) throws TaskManagerInitializationException {
        Connection connection = getConnection(configuration);
        LOGGER.debug("createQuartzDbSchema: trying JDBC connection {}", connection);
        try {
            if (!doQuartzTablesExist(connection)) {

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

                    if (!doQuartzTablesExist(connection)) {
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
        Connection connection = null;
        try {
            if (configuration.getDataSource() != null) {
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

    private boolean doQuartzTablesExist(Connection connection) {
        try {
            connection.prepareStatement("SELECT count(*) FROM QRTZ_JOB_DETAILS").executeQuery().close();
            LOGGER.debug("Quartz tables seem to exist (at least QRTZ_JOB_DETAILS does).");
            return true;
        } catch (SQLException ignored) {
            LOGGER.debug("Quartz tables seem not to exist (at least QRTZ_JOB_DETAILS does not), we got an exception when trying to access it", ignored);
            return false;
        }
    }

    private Reader getResourceReader(String name) throws IOException, TaskManagerInitializationException {
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            throw new TaskManagerInitializationException("Quartz DB schema (" + name + ") cannot be found.");
        }
        return new BufferedReader(new InputStreamReader(stream));
    }

    private String getResource(String name) throws IOException, TaskManagerInitializationException {
        InputStream stream = getClass().getResourceAsStream(name);
        if (stream == null) {
            throw new TaskManagerInitializationException("Quartz DB schema (" + name + ") cannot be found.");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuffer sb = new StringBuffer();
        int i;
        while ((i = br.read()) != -1) {
            sb.append((char) i);
        }
        return sb.toString();
    }

    void pauseScheduler(OperationResult result) {

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

    public NodeExecutionStatusType getLocalNodeExecutionStatus() {

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
        if (retval == null) {
            return false;           // should not occur anyway
        } else {
            return retval;
        }
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

        LOGGER.info("Stopping local task " + oid + " run");

        try {
            getQuartzScheduler().interrupt(TaskQuartzImplUtil.createJobKeyForTaskOid(oid));
            result.recordSuccess();
        } catch (UnableToInterruptJobException e) {
            String message = "Unable to interrupt the task " + oid;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);			// however, we continue (e.g. to suspend the task)
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

    /**
     * Returns all the currently executing tasks.
     *
     * @return
     */
    Set<Task> getLocallyRunningTasks(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(LocalNodeManager.class.getName() + ".getLocallyRunningTasks");

        Set<Task> retval = new HashSet<Task>();

        List<JobExecutionContext> jecs;
        try {
            jecs = getQuartzScheduler().getCurrentlyExecutingJobs();
        } catch (SchedulerException e1) {
            String message = "Cannot get the list of currently executing jobs on local node.";
            result.recordFatalError(message, e1);
            LoggingUtils.logUnexpectedException(LOGGER, message, e1);
            return retval;
        }

        for (JobExecutionContext jec : jecs) {
            String oid = jec.getJobDetail().getKey().getName();
            OperationResult result1 = result.createSubresult(LocalNodeManager.class.getName() + ".getLocallyRunningTask");
            try {
                retval.add(taskManager.getTask(oid, result1));
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

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(LocalNodeManager.class.getName() + "." + methodName);
    }

    public ExecutionManager getGlobalExecutionManager() {
        return taskManager.getExecutionManager();
    }

}

