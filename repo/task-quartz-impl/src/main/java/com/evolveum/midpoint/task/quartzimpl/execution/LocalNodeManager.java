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
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

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
            quartzProperties.put("org.quartz.jobStore.dataSource", "myDS");

            createQuartzDbSchema();

            quartzProperties.put("org.quartz.dataSource.myDS.driver", configuration.getJdbcDriver());
            quartzProperties.put("org.quartz.dataSource.myDS.URL", configuration.getJdbcUrl());
            quartzProperties.put("org.quartz.dataSource.myDS.user", configuration.getJdbcUser());
            quartzProperties.put("org.quartz.dataSource.myDS.password", configuration.getJdbcPassword());

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
            LOGGER.info("ReusableQuartzScheduler is set: the task manager threads will NOT be stopped on shutdown. Also, scheduler threads will run as daemon ones.");
            quartzProperties.put("org.quartz.scheduler.makeSchedulerThreadDaemon", "true");
            quartzProperties.put("org.quartz.threadPool.makeThreadsDaemons", "true");
        }

        // initialize the scheduler (without starting it)
        try {
            LOGGER.trace("Quartz scheduler properties: {}", quartzProperties);
            StdSchedulerFactory sf = new StdSchedulerFactory();
            sf.initialize(quartzProperties);
            getGlobalExecutionManager().setQuartzScheduler(sf.getScheduler());
        } catch (SchedulerException e) {
            throw new TaskManagerInitializationException("Cannot initialize the Quartz scheduler", e);
        }
    }

    /**
     * Creates Quartz database schema, if it does not exist.
     *
     * @throws TaskManagerInitializationException
     */
    private void createQuartzDbSchema() throws TaskManagerInitializationException {
        TaskManagerConfiguration configuration = taskManager.getConfiguration();
        try {
            Class.forName(configuration.getJdbcDriver());
        } catch (ClassNotFoundException e) {
            throw new TaskManagerInitializationException("Could not locate database driver class " + configuration.getJdbcDriver(), e);
        }
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(configuration.getJdbcUrl(), configuration.getJdbcUser(), configuration.getJdbcPassword());
        } catch (SQLException e) {
            throw new TaskManagerInitializationException("Cannot create JDBC connection to Quartz Job Store", e);
        }
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

    private boolean doQuartzTablesExist(Connection connection) {
        try {
            connection.prepareStatement("SELECT count(*) FROM qrtz_job_details").executeQuery().close();
            LOGGER.trace("Quartz tables seem to exist (at least QRTZ_JOB_DETAILS does).");
            return true;
        } catch (SQLException ignored) {
            LOGGER.trace("Quartz tables seem not to exist (at least QRTZ_JOB_DETAILS does not), we got an exception when trying to access it", ignored);
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
            LoggingUtils.logException(LOGGER, "Couldn't put local Quartz scheduler into standby mode", e1);
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
            LoggingUtils.logException(LOGGER, "Cannot (re)start Quartz scheduler.", e);
            result.recordFatalError("Cannot (re)start Quartz scheduler.", e);
        }
    }



    public NodeExecutionStatus getLocalNodeExecutionStatus() {

        if (taskManager.getLocalNodeErrorStatus() != NodeErrorStatus.OK) {
            return NodeExecutionStatus.ERROR;
        } else {
            Scheduler quartzScheduler = getGlobalExecutionManager().getQuartzScheduler();

            if (quartzScheduler == null) {      // this should not occur if error status is OK
                return NodeExecutionStatus.COMMUNICATION_ERROR;
            }

            boolean quartzRunning;
            try {
                quartzRunning = quartzScheduler.isStarted() && !quartzScheduler.isInStandbyMode() && !quartzScheduler.isShutdown();
            } catch (SchedulerException e) {
                LoggingUtils.logException(LOGGER, "Cannot determine Quartz scheduler state", e);
                return NodeExecutionStatus.COMMUNICATION_ERROR;
            }
            return quartzRunning ? NodeExecutionStatus.RUNNING : NodeExecutionStatus.PAUSED;
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
            LoggingUtils.logException(LOGGER, message, e);			// however, we continue (e.g. to suspend the task)
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
            LoggingUtils.logException(LOGGER, "Cannot find the currently executing job for the task {}", e1, oid);
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
            LoggingUtils.logException(LOGGER, "Cannot get the list of currently executing jobs", e);
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
            LoggingUtils.logException(LOGGER, message, e1);
            return retval;
        }

        for (JobExecutionContext jec : jecs) {
            String oid = jec.getJobDetail().getKey().getName();
            try {
                retval.add(taskManager.getTask(oid, result));
            } catch (ObjectNotFoundException e) {
                String m = "Cannot get the task with OID " + oid + " as it no longer exists";
                LoggingUtils.logException(LOGGER, m, e);
                result.createSubresult(LocalNodeManager.class.getName() + ".getLocallyRunningTask").recordFatalError(m, e);
            } catch (SchemaException e) {
                String m = "Cannot get the task with OID " + oid + " due to schema problems";
                LoggingUtils.logException(LOGGER, m, e);
                result.createSubresult(LocalNodeManager.class.getName() + ".getLocallyRunningTask").recordFatalError(m, e);
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

