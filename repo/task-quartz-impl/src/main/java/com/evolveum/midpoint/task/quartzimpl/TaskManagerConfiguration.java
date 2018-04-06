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
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.task.api.TaskManagerConfigurationException;
import com.evolveum.midpoint.task.api.UseThreadInterrupt;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Task Manager configuration, derived from "taskManager" section of midPoint config,
 * SQL repository configuration (if present), and some system properties.
 *
 * See also the description in midPoint wiki (TODO URL).
 *
 * On configuration failures, it throws TaskManagerConfigurationException.
 *
 * @author Pavol Mederly
 */
public class TaskManagerConfiguration {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskManagerConfiguration.class);

    private static final String TASK_MANAGER_CONFIG_SECTION = "midpoint.taskManager";
    private static final String STOP_ON_INITIALIZATION_FAILURE_CONFIG_ENTRY = "stopOnInitializationFailure";
    private static final String THREADS_CONFIG_ENTRY = "threads";
    private static final String CLUSTERED_CONFIG_ENTRY = "clustered";
    private static final String JDBC_JOB_STORE_CONFIG_ENTRY = "jdbcJobStore";
    private static final String JDBC_DRIVER_CONFIG_ENTRY = "jdbcDriver";
    private static final String JDBC_URL_CONFIG_ENTRY = "jdbcUrl";
    private static final String JDBC_USER_CONFIG_ENTRY = "jdbcUser";
    private static final String JDBC_PASSWORD_CONFIG_ENTRY = "jdbcPassword";
    private static final String DATA_SOURCE_CONFIG_ENTRY = "dataSource";
    private static final String SQL_SCHEMA_FILE_CONFIG_ENTRY = "sqlSchemaFile";
    private static final String CREATE_QUARTZ_TABLES_CONFIG_ENTRY = "createQuartzTables";
    private static final String JDBC_DRIVER_DELEGATE_CLASS_CONFIG_ENTRY = "jdbcDriverDelegateClass";
    private static final String USE_THREAD_INTERRUPT_CONFIG_ENTRY = "useThreadInterrupt";
    private static final String JMX_CONNECT_TIMEOUT_CONFIG_ENTRY = "jmxConnectTimeout";
    private static final String QUARTZ_NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY = "quartzNodeRegistrationInterval";
    private static final String NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY = "nodeRegistrationInterval";
    private static final String NODE_TIMEOUT_CONFIG_ENTRY = "nodeTimeout";
    private static final String JMX_USERNAME_CONFIG_ENTRY = "jmxUsername";
    private static final String JMX_PASSWORD_CONFIG_ENTRY = "jmxPassword";
    private static final String TEST_MODE_CONFIG_ENTRY = "testMode";
    private static final String WAITING_TASKS_CHECK_INTERVAL_CONFIG_ENTRY = "waitingTasksCheckInterval";
    private static final String STALLED_TASKS_CHECK_INTERVAL_CONFIG_ENTRY = "stalledTasksCheckInterval";
    private static final String STALLED_TASKS_THRESHOLD_CONFIG_ENTRY = "stalledTasksThreshold";
    private static final String STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_CONFIG_ENTRY = "stalledTasksRepeatedNotificationInterval";
    private static final String RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_CONFIG_ENTRY = "runNowKeepsOriginalSchedule";
    private static final String SCHEDULER_INITIALLY_STOPPED_CONFIG_ENTRY = "schedulerInitiallyStopped";

    private static final String WORK_ALLOCATION_MAX_ATTEMPTS_ENTRY = "workAllocationMaxAttempts";
    private static final String WORK_ALLOCATION_RETRY_INTERVAL_ENTRY = "workAllocationRetryInterval";
    private static final String WORK_ALLOCATION_INITIAL_DELAY_ENTRY = "workAllocationInitialDelay";
    private static final String WORK_ALLOCATION_DEFAULT_FREE_BUCKET_WAIT_INTERVAL_ENTRY = "workAllocationDefaultFreeBucketWaitInterval";

    private static final String MIDPOINT_NODE_ID_PROPERTY = "midpoint.nodeId";
    private static final String MIDPOINT_JMX_HOST_NAME_PROPERTY = "midpoint.jmxHostName";
    private static final String JMX_PORT_PROPERTY = "com.sun.management.jmxremote.port";
    private static final String SUREFIRE_PRESENCE_PROPERTY = "surefire.real.class.path";

    private static final boolean STOP_ON_INITIALIZATION_FAILURE_DEFAULT = true;
    private static final int THREADS_DEFAULT = 10;
    private static final boolean CLUSTERED_DEFAULT = false;             // do not change this value!
    private static final boolean CREATE_QUARTZ_TABLES_DEFAULT = true;
    private static final String NODE_ID_DEFAULT = "DefaultNode";
    private static final int JMX_PORT_DEFAULT = 20001;
    private static final int JMX_CONNECT_TIMEOUT_DEFAULT = 5;
    private static final String USE_THREAD_INTERRUPT_DEFAULT = "whenNecessary";
    private static final int QUARTZ_NODE_REGISTRATION_CYCLE_TIME_DEFAULT = 10;
    private static final int NODE_REGISTRATION_CYCLE_TIME_DEFAULT = 10;
    private static final int NODE_TIMEOUT_DEFAULT = 30;
    private static final String JMX_USERNAME_DEFAULT = "midpoint";
    private static final String JMX_PASSWORD_DEFAULT = "secret";
    private static final int WAITING_TASKS_CHECK_INTERVAL_DEFAULT = 600;
    private static final int STALLED_TASKS_CHECK_INTERVAL_DEFAULT = 600;
    private static final int STALLED_TASKS_THRESHOLD_DEFAULT = 600;             // if a task does not advance its progress for 10 minutes, it is considered stalled
    private static final int STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_DEFAULT = 3600;
    private static final boolean RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_DEFAULT = false;

    private static final int WORK_ALLOCATION_MAX_ATTEMPTS_DEFAULT = 40;
    private static final long WORK_ALLOCATION_RETRY_INTERVAL_DEFAULT = 5000L;
    private static final long WORK_ALLOCATION_INITIAL_DELAY_DEFAULT = 5000L;
    private static final long WORK_ALLOCATION_DEFAULT_FREE_BUCKET_WAIT_INTERVAL_DEFAULT = 20000L;

    private boolean stopOnInitializationFailure;
    private int threads;
    private boolean jdbcJobStore;
    private boolean clustered;
    private String nodeId;
    private String jmxHostName;
    private int jmxPort;
    private int jmxConnectTimeout;
    private int quartzNodeRegistrationCycleTime;            // UNUSED (currently) !
    private int nodeRegistrationCycleTime, nodeTimeout;
    private UseThreadInterrupt useThreadInterrupt;
    private int waitingTasksCheckInterval;
    private int stalledTasksCheckInterval;
    private int stalledTasksThreshold;
    private int stalledTasksRepeatedNotificationInterval;
    private boolean runNowKeepsOriginalSchedule;
    private boolean schedulerInitiallyStopped;

    private int workAllocationMaxAttempts;
    private long workAllocationRetryInterval;
    private long workAllocationInitialDelay;
    private long workAllocationDefaultFreeBucketWaitInterval;

    // JMX credentials for connecting to remote nodes
    private String jmxUsername;
    private String jmxPassword;

    // quartz jdbc job store specific information
    private String sqlSchemaFile;
    private String jdbcDriverDelegateClass;
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private String dataSource;
    private boolean createQuartzTables;

    private String hibernateDialect;
    private boolean databaseIsEmbedded;

    /*
      * Are we in the test mode?
      *
      * It affects e.g. whether to allow reusing quartz scheduler after task manager shutdown.
      *
      * Concretely, if in test mode, quartz scheduler will not be shut down, only paused.
      * This allows for restarting it (scheduler cannot be started, if it was shut down:
      * http://quartz-scheduler.org/api/2.1.0/org/quartz/Scheduler.html#shutdown())
      *
      * If not run in test mode (i.e. within Tomcat), we do not, because pausing
      * the scheduler does NOT stop the execution threads.
      *
      * We determine whether in test mode by examining testMode property and, if not present,
      * by looking for SUREFIRE_PRESENCE_PROPERTY.
      */
    private boolean midPointTestMode = false;

    public static final List<String> KNOWN_KEYS = Arrays.asList(
            "midpoint.home",
            STOP_ON_INITIALIZATION_FAILURE_CONFIG_ENTRY,
            THREADS_CONFIG_ENTRY,
            CLUSTERED_CONFIG_ENTRY,
            JDBC_JOB_STORE_CONFIG_ENTRY,
            JDBC_DRIVER_CONFIG_ENTRY,
            JDBC_URL_CONFIG_ENTRY,
            JDBC_USER_CONFIG_ENTRY,
            JDBC_PASSWORD_CONFIG_ENTRY,
            DATA_SOURCE_CONFIG_ENTRY,
            SQL_SCHEMA_FILE_CONFIG_ENTRY,
            CREATE_QUARTZ_TABLES_CONFIG_ENTRY,
            JDBC_DRIVER_DELEGATE_CLASS_CONFIG_ENTRY,
            USE_THREAD_INTERRUPT_CONFIG_ENTRY,
            JMX_CONNECT_TIMEOUT_CONFIG_ENTRY,
            QUARTZ_NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY,
            NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY,
            NODE_TIMEOUT_CONFIG_ENTRY,
            JMX_USERNAME_CONFIG_ENTRY,
            JMX_PASSWORD_CONFIG_ENTRY,
            TEST_MODE_CONFIG_ENTRY,
            WAITING_TASKS_CHECK_INTERVAL_CONFIG_ENTRY,
            STALLED_TASKS_CHECK_INTERVAL_CONFIG_ENTRY,
            STALLED_TASKS_THRESHOLD_CONFIG_ENTRY,
            STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_CONFIG_ENTRY,
            RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_CONFIG_ENTRY,
			SCHEDULER_INITIALLY_STOPPED_CONFIG_ENTRY,
            WORK_ALLOCATION_MAX_ATTEMPTS_ENTRY,
            WORK_ALLOCATION_RETRY_INTERVAL_ENTRY,
            WORK_ALLOCATION_INITIAL_DELAY_ENTRY,
            WORK_ALLOCATION_DEFAULT_FREE_BUCKET_WAIT_INTERVAL_ENTRY
    );

    void checkAllowedKeys(MidpointConfiguration masterConfig) throws TaskManagerConfigurationException {
        Configuration c = masterConfig.getConfiguration(TASK_MANAGER_CONFIG_SECTION);
        checkAllowedKeys(c, KNOWN_KEYS);
    }

    // todo copied from WfConfiguration -- refactor
    private void checkAllowedKeys(Configuration c, List<String> knownKeys) throws TaskManagerConfigurationException {
        Set<String> knownKeysSet = new HashSet<>(knownKeys);

        Iterator<String> keyIterator = c.getKeys();
        while (keyIterator.hasNext())  {
            String keyName = keyIterator.next();
            String normalizedKeyName = StringUtils.substringBefore(keyName, ".");                       // because of subkeys
            normalizedKeyName = StringUtils.substringBefore(normalizedKeyName, "[");                    // because of [@xmlns:c]
            int colon = normalizedKeyName.indexOf(':');                                                 // because of c:generalChangeProcessorConfiguration
            if (colon != -1) {
                normalizedKeyName = normalizedKeyName.substring(colon + 1);
            }
            if (!knownKeysSet.contains(keyName) && !knownKeysSet.contains(normalizedKeyName)) {         // ...we need to test both because of keys like 'midpoint.home'
                throw new TaskManagerConfigurationException("Unknown key " + keyName + " in task manager configuration");
            }
        }
    }

    void setBasicInformation(MidpointConfiguration masterConfig) throws TaskManagerConfigurationException {
        Configuration c = masterConfig.getConfiguration(TASK_MANAGER_CONFIG_SECTION);

        stopOnInitializationFailure = c.getBoolean(STOP_ON_INITIALIZATION_FAILURE_CONFIG_ENTRY, STOP_ON_INITIALIZATION_FAILURE_DEFAULT);

        threads = c.getInt(THREADS_CONFIG_ENTRY, THREADS_DEFAULT);
        clustered = c.getBoolean(CLUSTERED_CONFIG_ENTRY, CLUSTERED_DEFAULT);
        jdbcJobStore = c.getBoolean(JDBC_JOB_STORE_CONFIG_ENTRY, clustered);

        nodeId = System.getProperty(MIDPOINT_NODE_ID_PROPERTY);
        if (StringUtils.isEmpty(nodeId) && !clustered) {
            nodeId = NODE_ID_DEFAULT;
        }

        jmxHostName = System.getProperty(MIDPOINT_JMX_HOST_NAME_PROPERTY);

        String portString = System.getProperty(JMX_PORT_PROPERTY);
        if (StringUtils.isEmpty(portString)) {
            jmxPort = JMX_PORT_DEFAULT;
        } else {
            try {
                jmxPort = Integer.parseInt(portString);
            } catch(NumberFormatException nfe) {
                throw new TaskManagerConfigurationException("Cannot get JMX management port - invalid integer value of " + portString, nfe);
            }
        }

        jmxConnectTimeout = c.getInt(JMX_CONNECT_TIMEOUT_CONFIG_ENTRY, JMX_CONNECT_TIMEOUT_DEFAULT);

        if (c.containsKey(TEST_MODE_CONFIG_ENTRY)) {
            midPointTestMode = c.getBoolean(TEST_MODE_CONFIG_ENTRY);
            LOGGER.trace(TEST_MODE_CONFIG_ENTRY + " present, its value = " + midPointTestMode);
        } else {
            LOGGER.trace(TEST_MODE_CONFIG_ENTRY + " NOT present");
            Properties sp = System.getProperties();
            if (sp.containsKey(SUREFIRE_PRESENCE_PROPERTY)) {
                LOGGER.info("Determined to run in a test environment, setting midPointTestMode to 'true'.");
                midPointTestMode = true;
            } else {
                midPointTestMode = false;
            }
        }
        LOGGER.trace("midPointTestMode = " + midPointTestMode);

        String useTI = c.getString(USE_THREAD_INTERRUPT_CONFIG_ENTRY, USE_THREAD_INTERRUPT_DEFAULT);
        try {
            useThreadInterrupt = UseThreadInterrupt.fromValue(useTI);
        } catch(IllegalArgumentException e) {
            throw new TaskManagerConfigurationException("Illegal value for " + USE_THREAD_INTERRUPT_CONFIG_ENTRY + ": " + useTI, e);
        }

        quartzNodeRegistrationCycleTime = c.getInt(QUARTZ_NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY, QUARTZ_NODE_REGISTRATION_CYCLE_TIME_DEFAULT);
        nodeRegistrationCycleTime = c.getInt(NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY, NODE_REGISTRATION_CYCLE_TIME_DEFAULT);
        nodeTimeout = c.getInt(NODE_TIMEOUT_CONFIG_ENTRY, NODE_TIMEOUT_DEFAULT);

        jmxUsername = c.getString(JMX_USERNAME_CONFIG_ENTRY, JMX_USERNAME_DEFAULT);
        jmxPassword = c.getString(JMX_PASSWORD_CONFIG_ENTRY, JMX_PASSWORD_DEFAULT);

        waitingTasksCheckInterval = c.getInt(WAITING_TASKS_CHECK_INTERVAL_CONFIG_ENTRY, WAITING_TASKS_CHECK_INTERVAL_DEFAULT);
        stalledTasksCheckInterval = c.getInt(STALLED_TASKS_CHECK_INTERVAL_CONFIG_ENTRY, STALLED_TASKS_CHECK_INTERVAL_DEFAULT);
        stalledTasksThreshold = c.getInt(STALLED_TASKS_THRESHOLD_CONFIG_ENTRY, STALLED_TASKS_THRESHOLD_DEFAULT);
        stalledTasksRepeatedNotificationInterval = c.getInt(STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_CONFIG_ENTRY, STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_DEFAULT);
        runNowKeepsOriginalSchedule = c.getBoolean(RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_CONFIG_ENTRY, RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_DEFAULT);
        schedulerInitiallyStopped = c.getBoolean(SCHEDULER_INITIALLY_STOPPED_CONFIG_ENTRY, false);

        workAllocationMaxAttempts = c.getInt(WORK_ALLOCATION_MAX_ATTEMPTS_ENTRY, WORK_ALLOCATION_MAX_ATTEMPTS_DEFAULT);
        workAllocationRetryInterval = c.getLong(WORK_ALLOCATION_RETRY_INTERVAL_ENTRY, WORK_ALLOCATION_RETRY_INTERVAL_DEFAULT);
        workAllocationInitialDelay = c.getLong(WORK_ALLOCATION_INITIAL_DELAY_ENTRY, WORK_ALLOCATION_INITIAL_DELAY_DEFAULT);
        workAllocationDefaultFreeBucketWaitInterval = c.getLong(WORK_ALLOCATION_DEFAULT_FREE_BUCKET_WAIT_INTERVAL_ENTRY,
                WORK_ALLOCATION_DEFAULT_FREE_BUCKET_WAIT_INTERVAL_DEFAULT);
    }

    private static final Map<String,String> schemas = new HashMap<>();
    private static final Map<String,String> delegates = new HashMap<>();

    static void addDbInfo(String dialect, String schema, String delegate) {
        schemas.put(dialect, schema);
        delegates.put(dialect, delegate);
    }

    static {
        addDbInfo("org.hibernate.dialect.H2Dialect", "tables_h2.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.PostgreSQLDialect", "tables_postgres.sql", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        addDbInfo("org.hibernate.dialect.PostgresPlusDialect", "tables_postgres.sql", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        addDbInfo("com.evolveum.midpoint.repo.sql.util.MidPointPostgreSQLDialect", "tables_postgres.sql", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        addDbInfo("org.hibernate.dialect.MySQLDialect", "tables_mysql.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.MySQLInnoDBDialect", "tables_mysql_innodb.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("com.evolveum.midpoint.repo.sql.util.MidPointMySQLDialect", "tables_mysql_innodb.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.OracleDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle9Dialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle8iDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle9iDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle10gDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.SQLServerDialect", "tables_sqlServer.sql", "org.quartz.impl.jdbcjobstore.MSSQLDelegate");
        addDbInfo("com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect", "tables_sqlServer.sql", "org.quartz.impl.jdbcjobstore.MSSQLDelegate");
    }

    void setJdbcJobStoreInformation(MidpointConfiguration masterConfig, SqlRepositoryConfiguration sqlConfig, String defaultJdbcUrlPrefix) {

        Configuration c = masterConfig.getConfiguration(TASK_MANAGER_CONFIG_SECTION);

        jdbcDriver = c.getString(JDBC_DRIVER_CONFIG_ENTRY, sqlConfig != null ? sqlConfig.getDriverClassName() : null);

        String explicitJdbcUrl = c.getString(JDBC_URL_CONFIG_ENTRY, null);
        if (explicitJdbcUrl == null) {
            if (sqlConfig.isEmbedded()) {
                jdbcUrl = defaultJdbcUrlPrefix + "-quartz;MVCC=TRUE;DB_CLOSE_ON_EXIT=FALSE";
            } else {
                jdbcUrl = sqlConfig.getJdbcUrl();
            }
        } else {
            jdbcUrl = explicitJdbcUrl;
        }
        dataSource = c.getString(DATA_SOURCE_CONFIG_ENTRY, null);
        if (dataSource == null && explicitJdbcUrl == null && sqlConfig != null) {
            dataSource = sqlConfig.getDataSource();             // we want to use quartz-specific JDBC if there is one (i.e. we do not want to inherit data source from repo in such a case)
        }

        if (dataSource != null) {
            LOGGER.info("Quartz database is at {} (a data source)", dataSource);
        } else {
            LOGGER.info("Quartz database is at {} (a JDBC URL)", jdbcUrl);
        }

        jdbcUser = c.getString(JDBC_USER_CONFIG_ENTRY, sqlConfig != null ? sqlConfig.getJdbcUsername() : null);
        jdbcPassword = c.getString(JDBC_PASSWORD_CONFIG_ENTRY, sqlConfig != null ? sqlConfig.getJdbcPassword() : null);

        hibernateDialect = sqlConfig != null ? sqlConfig.getHibernateDialect() : "";

        String defaultSqlSchemaFile = schemas.get(hibernateDialect);
        String defaultDriverDelegate = delegates.get(hibernateDialect);

        sqlSchemaFile = c.getString(SQL_SCHEMA_FILE_CONFIG_ENTRY, defaultSqlSchemaFile);
        jdbcDriverDelegateClass = c.getString(JDBC_DRIVER_DELEGATE_CLASS_CONFIG_ENTRY, defaultDriverDelegate);

        createQuartzTables = c.getBoolean(CREATE_QUARTZ_TABLES_CONFIG_ENTRY, CREATE_QUARTZ_TABLES_DEFAULT);
    }

    /**
     * Check configuration, except for JDBC JobStore-specific parts.
     */
    void validateBasicInformation() throws TaskManagerConfigurationException {

        if (threads < 1) {
            LOGGER.warn("The configured number of threads is too low, setting it to 5.");
            threads = 5;
        }

        if (clustered) {
            mustBeTrue(jdbcJobStore, "Clustered task manager requires JDBC Quartz job store.");
        }

        notEmpty(nodeId, "Node identifier must be set when run in clustered mode.");
        mustBeFalse(clustered && jmxPort == 0, "JMX port number must be known when run in clustered mode.");

        mustBeTrue(quartzNodeRegistrationCycleTime > 1 && quartzNodeRegistrationCycleTime <= 600, "Quartz node registration cycle time must be between 1 and 600 seconds");
        mustBeTrue(nodeRegistrationCycleTime > 1 && nodeRegistrationCycleTime <= 600, "Node registration cycle time must be between 1 and 600 seconds");
        mustBeTrue(nodeTimeout > 5 && nodeTimeout <= 3600, "Node timeout must be between 5 and 3600 seconds");
    }

    void validateJdbcJobStoreInformation() throws TaskManagerConfigurationException {

        if (StringUtils.isEmpty(dataSource)) {
            notEmpty(jdbcDriver, "JDBC driver must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
            notEmpty(jdbcUrl, "JDBC URL must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
            notNull(jdbcUser, "JDBC user name must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
            notNull(jdbcPassword, "JDBC password must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
        }
        if (StringUtils.isEmpty(jdbcDriverDelegateClass)) {
            throw new TaskManagerConfigurationException("JDBC driver delegate class must be specified (either explicitly or "
                    + "through one of supported Hibernate dialects). It seems that the currently specified dialect ("
                    + hibernateDialect + ") is not among supported dialects (" + delegates.keySet() + "). "
                    + "Please check " + TaskManagerConfiguration.class.getName() + " class or specify driver delegate explicitly.");
        }
        notEmpty(sqlSchemaFile, "SQL schema file must be specified (either explicitly or through one of supported Hibernate dialects).");
    }

    private void notEmpty(String value, String message) throws TaskManagerConfigurationException {
        if (StringUtils.isEmpty(value)) {
            throw new TaskManagerConfigurationException(message);
        }
    }

    private void notNull(String value, String message) throws TaskManagerConfigurationException {
        if (value == null) {
            throw new TaskManagerConfigurationException(message);
        }
    }

    private void mustBeTrue(boolean condition, String message) throws TaskManagerConfigurationException {
        if (!condition) {
            throw new TaskManagerConfigurationException(message);
        }
    }

    private void mustBeFalse(boolean condition, String message) throws TaskManagerConfigurationException {
        if (condition) {
            throw new TaskManagerConfigurationException(message);
        }
    }

    public int getThreads() {
        return threads;
    }

    public boolean isJdbcJobStore() {
        return jdbcJobStore;
    }

    public boolean isClustered() {
        return clustered;
    }

    public String getNodeId() {
        return nodeId;
    }

    public int getJmxPort() {
        return jmxPort;
    }

    public String getSqlSchemaFile() {
        return sqlSchemaFile;
    }

    public String getJdbcDriverDelegateClass() {
        return jdbcDriverDelegateClass;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getJdbcUser() {
        return jdbcUser;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public boolean isTestMode() {
        return midPointTestMode;
    }

    public UseThreadInterrupt getUseThreadInterrupt() {
        return useThreadInterrupt;
    }

    public int getJmxConnectTimeout() {
        return jmxConnectTimeout;
    }

    public boolean isStopOnInitializationFailure() {
        return stopOnInitializationFailure;
    }

    public boolean isDatabaseIsEmbedded() {
        return databaseIsEmbedded;
    }

    public int getNodeTimeout() {
        return nodeTimeout;
    }

    public int getNodeRegistrationCycleTime() {
        return nodeRegistrationCycleTime;
    }

    public int getQuartzNodeRegistrationCycleTime() {
        return quartzNodeRegistrationCycleTime;
    }

    public String getJmxUsername() {
        return jmxUsername;
    }

    public String getJmxPassword() {
        return jmxPassword;
    }

    public String getJmxHostName() {
        return jmxHostName;
    }

    public int getWaitingTasksCheckInterval() {
        return waitingTasksCheckInterval;
    }

    public int getStalledTasksCheckInterval() {
        return stalledTasksCheckInterval;
    }

    public int getStalledTasksThreshold() {
        return stalledTasksThreshold;
    }

    public int getStalledTasksRepeatedNotificationInterval() {
        return stalledTasksRepeatedNotificationInterval;
    }

    public boolean isRunNowKeepsOriginalSchedule() {
        return runNowKeepsOriginalSchedule;
    }

    public boolean isCreateQuartzTables() {
        return createQuartzTables;
    }

    public void setCreateQuartzTables(boolean createQuartzTables) {
        this.createQuartzTables = createQuartzTables;
    }

    public String getDataSource() {
        return dataSource;
    }

	public boolean isSchedulerInitiallyStopped() {
		return schedulerInitiallyStopped;
	}

    public int getWorkAllocationMaxAttempts() {
        return workAllocationMaxAttempts;
    }

    public long getWorkAllocationRetryInterval() {
        return workAllocationRetryInterval;
    }

    public long getWorkAllocationInitialDelay() {
        return workAllocationInitialDelay;
    }

    public long getWorkAllocationDefaultFreeBucketWaitInterval() {
        return workAllocationDefaultFreeBucketWaitInterval;
    }
}
