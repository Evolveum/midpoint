/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Manages task threads on the local node. Concerned mainly with stopping threads and querying their state.
 *
 * @author Pavol Mederly
 */
public class LocalExecutionManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(LocalExecutionManager.class);

    private TaskManagerQuartzImpl taskManager;

    public LocalExecutionManager(TaskManagerQuartzImpl taskManager) {
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
        if (configuration.isReusableQuartzScheduler()) {
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

        if (configuration.isReusableQuartzScheduler()) {
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
            try {
                connection.prepareStatement("SELECT count(*) FROM qrtz_job_details").executeQuery().close();
            } catch (SQLException ignored) {
                try {
                    connection.prepareStatement(getResource(configuration.getSqlSchemaFile())).executeUpdate();
                } catch (IOException ex) {
                    throw new TaskManagerInitializationException("Could not read Quartz database schema file: " + configuration.getSqlSchemaFile(), ex);
                } catch (SQLException e) {
                    throw new TaskManagerInitializationException("Could not create Quartz JDBC Job Store tables from " + configuration.getSqlSchemaFile(), e);
                }
            }
        } finally {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
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

    void startScheduler() throws TaskManagerException {

        try {
            LOGGER.info("Starting the Quartz scheduler");
            getQuartzScheduler().start();
        } catch (SchedulerException e) {
            throw new TaskManagerException("Cannot start the Quartz scheduler", e);
        }
    }

    void pauseScheduler() throws TaskManagerException {

        LOGGER.info("Putting Quartz scheduler into standby mode");
        try {
            getQuartzScheduler().standby();
        } catch (SchedulerException e1) {
            throw new TaskManagerException("Cannot put Quartz scheduler into standby mode", e1);
        }
    }

    void shutdownScheduler() throws TaskManagerException {

        LOGGER.info("Shutting down Quartz scheduler");
        try {
            getQuartzScheduler().shutdown(true);
            LOGGER.info("Quartz scheduler was shut down");
        } catch (SchedulerException e) {
            throw new TaskManagerException("Cannot shutdown Quartz scheduler", e);
        }
    }

    /**
     *  Robust version of 'shutdownScheduler', ignores exceptions, shuts down the scheduler only if not shutdown already.
     *  Used for emergency situations, e.g. node error.
     */
    void shutdownSchedulerChecked() {

        try {
            if (getQuartzScheduler() != null && !getQuartzScheduler().isShutdown()) {
                LOGGER.info("Shutting down Quartz scheduler");
                getQuartzScheduler().shutdown(true);
            }
        } catch (SchedulerException e) {
            LoggingUtils.logException(LOGGER, "Cannot shutdown scheduler.", e);
        }
    }


    private Scheduler getQuartzScheduler() {
        return getGlobalExecutionManager().getQuartzScheduler();
    }

    /*
     *  The following methods are used by external clients, e.g. the admin GUI.
     *  They do not throw exceptions.
     */

    boolean stopSchedulerAndTasksLocally(long timeToWait, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(LocalExecutionManager.class.getName() + ".stopSchedulerAndTasksLocally");
        result.addParam("timeToWait", timeToWait);

        try {
            pauseScheduler();
        } catch (TaskManagerException e) {
            LoggingUtils.logException(LOGGER, "An exception occurred while stopping the scheduler, continuing with stopping tasks", e);
            result.createSubresult("pauseScheduler").recordWarning("An exception occurred while stopping the scheduler, continuing with stopping tasks", e);
        }
        try {
            boolean tasksStopped = getGlobalExecutionManager().stopAllTasksOnThisNodeAndWait(timeToWait, result);
            LOGGER.info("Scheduler stopped; " + (tasksStopped ? "all task threads have been stopped as well." : "some task threads may still run."));
            result.recordSuccessIfUnknown();
            return tasksStopped;
        } catch(TaskManagerException e) {
            LoggingUtils.logException(LOGGER, "It is not possible to stop locally running tasks", e);
            result.recordPartialError("It is not possible to stop locally running tasks", e);
            return false;
        }
    }

    void stopSchedulerLocally(OperationResult result) {
        try {
            pauseScheduler();
        } catch (TaskManagerException e) {
            LoggingUtils.logException(LOGGER, "An exception occurred while trying to pause the scheduler", e);
            result.recordFatalError("Cannot pause local scheduler", e);
        }
    }

    void startSchedulerLocally(OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(LocalExecutionManager.class.getName() + ".startSchedulerLocally");

        if (taskManager.isInErrorState()) {
            String message = "Cannot start the scheduler, because Task Manager is in error state (" + taskManager.getLocalNodeErrorStatus() + ")";
            LOGGER.error(message);
            result.recordFatalError(message);
            return;
        }

        try {
            startScheduler();
            LOGGER.debug("Quartz scheduler started.");
            result.recordSuccess();
        } catch (TaskManagerException e) {
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

    void stopLocalTask(String oid, OperationResult result) {
        try {
            getQuartzScheduler().interrupt(TaskQuartzImplUtil.createJobKeyForTaskOid(oid));
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
    Set<Task> getLocallyRunningTasks(OperationResult parentResult) throws TaskManagerException {

        OperationResult result = parentResult.createSubresult(LocalExecutionManager.class.getName() + ".getLocallyRunningTasks");

        Set<Task> retval = new HashSet<Task>();

        List<JobExecutionContext> jecs;
        try {
            jecs = getQuartzScheduler().getCurrentlyExecutingJobs();
        } catch (SchedulerException e1) {
            String message = "Cannot get the list of currently executing jobs on local node.";
            result.recordFatalError(message, e1);
            throw new TaskManagerException(message, e1);
        }

        for (JobExecutionContext jec : jecs) {
            String oid = jec.getJobDetail().getKey().getName();
            try {
                retval.add(taskManager.getTask(oid, result));
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Cannot get the task with OID {} as it no longer exists", e, oid);
            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Cannot get the task with OID {} due to schema problems", e, oid);
            }
        }

        result.recordSuccessIfUnknown();

        return retval;
    }

    /*
     * Various auxiliary methods
     */

    private OperationResult createOperationResult(String methodName) {
        return new OperationResult(LocalExecutionManager.class.getName() + "." + methodName);
    }

    public GlobalExecutionManager getGlobalExecutionManager() {
        return taskManager.getGlobalExecutionManager();
    }

}

