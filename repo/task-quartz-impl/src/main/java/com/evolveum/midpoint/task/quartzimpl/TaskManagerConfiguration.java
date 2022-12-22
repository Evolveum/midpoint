/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Map.entry;

import java.util.*;

import com.google.common.base.Strings;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManagerConfigurationException;
import com.evolveum.midpoint.task.api.UseThreadInterrupt;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionLimitationsType;

/**
 * Task Manager configuration, derived from "taskManager" section of midPoint config,
 * SQL repository configuration (if present), and some system properties.
 *
 * See also the description in midPoint wiki (TODO URL).
 *
 * On configuration failures, it throws TaskManagerConfigurationException.
 *
 * TODO finish review of this class
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Component
public class TaskManagerConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(TaskManagerConfiguration.class);

    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    private static final String STOP_ON_INITIALIZATION_FAILURE_CONFIG_ENTRY = "stopOnInitializationFailure";
    private static final String THREADS_CONFIG_ENTRY = "threads";
    private static final String CLUSTERED_CONFIG_ENTRY = "clustered";
    private static final String JDBC_JOB_STORE_CONFIG_ENTRY = "jdbcJobStore";
    private static final String JDBC_DRIVER_CONFIG_ENTRY = "jdbcDriver";
    private static final String JDBC_URL_CONFIG_ENTRY = "jdbcUrl";
    private static final String JDBC_USER_CONFIG_ENTRY = "jdbcUser";
    private static final String JDBC_PASSWORD_CONFIG_ENTRY = "jdbcPassword";
    private static final String DATA_SOURCE_CONFIG_ENTRY = "dataSource";
    private static final String USE_REPOSITORY_CONNECTION_PROVIDER_CONFIG_ENTRY = "useRepositoryConnectionProvider";

    private static final String SQL_SCHEMA_FILE_CONFIG_ENTRY = "sqlSchemaFile";
    private static final String CREATE_QUARTZ_TABLES_CONFIG_ENTRY = "createQuartzTables";
    private static final String JDBC_DRIVER_DELEGATE_CLASS_CONFIG_ENTRY = "jdbcDriverDelegateClass";
    private static final String USE_THREAD_INTERRUPT_CONFIG_ENTRY = "useThreadInterrupt";
    @Deprecated private static final String JMX_CONNECT_TIMEOUT_CONFIG_ENTRY = "jmxConnectTimeout";
    @Deprecated private static final String QUARTZ_NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY = "quartzNodeRegistrationInterval";
    private static final String QUARTZ_CLUSTER_CHECKIN_INTERVAL_CONFIG_ENTRY = "quartzClusterCheckinInterval";
    private static final String QUARTZ_CLUSTER_CHECKIN_GRACE_PERIOD_CONFIG_ENTRY = "quartzClusterCheckinGracePeriod";
    private static final String NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY = "nodeRegistrationInterval";
    private static final String NODE_ALIVENESS_CHECK_INTERVAL_CONFIG_ENTRY = "nodeAlivenessCheckInterval";
    private static final String NODE_ALIVENESS_TIMEOUT_CONFIG_ENTRY = "nodeAlivenessTimeout";
    private static final String NODE_STARTUP_TIMEOUT_CONFIG_ENTRY = "nodeStartupTimeout";
    private static final String NODE_TIMEOUT_CONFIG_ENTRY = "nodeTimeout";
    private static final String NODE_STARTUP_DELAY_CONFIG_ENTRY = "nodeStartupDelay";

    private static final String CHECK_FOR_TASK_CONCURRENT_EXECUTION_CONFIG_ENTRY = "checkForTaskConcurrentExecution";
    private static final String TEST_MODE_CONFIG_ENTRY = "testMode";
    private static final String WAITING_TASKS_CHECK_INTERVAL_CONFIG_ENTRY = "waitingTasksCheckInterval";
    private static final String STALLED_TASKS_CHECK_INTERVAL_CONFIG_ENTRY = "stalledTasksCheckInterval";
    private static final String STALLED_TASKS_THRESHOLD_CONFIG_ENTRY = "stalledTasksThreshold";
    private static final String STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_CONFIG_ENTRY = "stalledTasksRepeatedNotificationInterval";
    private static final String RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_CONFIG_ENTRY = "runNowKeepsOriginalSchedule";
    private static final String SCHEDULER_INITIALLY_STOPPED_CONFIG_ENTRY = "schedulerInitiallyStopped";

    private static final String LOCAL_NODE_CLUSTERING_ENABLED_CONFIG_ENTRY = "localNodeClusteringEnabled";

    // The following are deprecated.
    private static final String WORK_ALLOCATION_MAX_RETRIES_ENTRY = "workAllocationMaxRetries";
    private static final String WORK_ALLOCATION_RETRY_INTERVAL_BASE_ENTRY = "workAllocationRetryIntervalBase";
    private static final String WORK_ALLOCATION_RETRY_INTERVAL_LIMIT_ENTRY = "workAllocationRetryIntervalLimit";
    private static final String WORK_ALLOCATION_RETRY_EXPONENTIAL_THRESHOLD_ENTRY = "workAllocationRetryExponentialThreshold";
    private static final String WORK_ALLOCATION_INITIAL_DELAY_ENTRY = "workAllocationInitialDelay";
    private static final String WORK_ALLOCATION_DEFAULT_FREE_BUCKET_WAIT_INTERVAL_ENTRY = "workAllocationDefaultFreeBucketWaitInterval";

    private static final String TASK_EXECUTION_LIMITATIONS_CONFIG_ENTRY = "taskExecutionLimitations";

    private static final String SUREFIRE_PRESENCE_PROPERTY = "surefire.real.class.path";

    private static final boolean STOP_ON_INITIALIZATION_FAILURE_DEFAULT = true;
    private static final int THREADS_DEFAULT = 10;
    private static final boolean CLUSTERED_DEFAULT = false;             // do not change this value!
    private static final boolean CREATE_QUARTZ_TABLES_DEFAULT = true;
    private static final String USE_THREAD_INTERRUPT_DEFAULT = "whenNecessary";
    private static final boolean CHECK_FOR_TASK_CONCURRENT_EXECUTION_DEFAULT = false;
    private static final int WAITING_TASKS_CHECK_INTERVAL_DEFAULT = 600;
    private static final int STALLED_TASKS_CHECK_INTERVAL_DEFAULT = 600;
    private static final int STALLED_TASKS_THRESHOLD_DEFAULT = 600;             // if a task does not advance its progress for 10 minutes, it is considered stalled
    private static final int STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_DEFAULT = 3600;
    private static final boolean RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_DEFAULT = false;

    private boolean stopOnInitializationFailure;
    private int threads;
    private boolean jdbcJobStore;
    private boolean clustered;
    private String nodeId;
    private String url;
    private String hostName;
    private Integer httpPort;

    /**
     * How often should node register itself in repository. In seconds.
     */
    private int nodeRegistrationCycleTime;

    private static final int NODE_REGISTRATION_CYCLE_TIME_DEFAULT = 10;

    /**
     * After what time should be node considered (temporarily) down. In seconds.
     */
    private int nodeTimeout;

    private static final int NODE_TIMEOUT_DEFAULT = 30;

    /**
     * After what time (of not checking in) should be node considered permanently down
     * and recorded as such in the repository. In seconds.
     */
    private int nodeAlivenessTimeout;

    private static final int NODE_ALIVENESS_TIMEOUT_DEFAULT = 900;

    /**
     * After what time (of not checking in) the node start-up is considered to be "too long". In seconds.
     *
     * Note that we currently do nothing after this timeout occurs, except for logging a warning message.
     */
    private int nodeStartupTimeout;

    private static final int NODE_STARTUP_TIMEOUT_DEFAULT = 900;

    /**
     * How often this node checks for other nodes being down (in seconds).
     *
     * The check is based on last check in time and {@link #nodeAlivenessTimeout}.
     * See {@link ClusterManager#checkNodeAliveness(OperationResult)}.
     */
    private int nodeAlivenessCheckInterval;

    private static final int NODE_ALIVENESS_CHECK_INTERVAL_DEFAULT = 120;

    /**
     * Number of seconds after which we declare the node as started and announce it as a part of the cluster.
     */
    private int nodeStartupDelay;

    private static final int NODE_STARTUP_DELAY_DEFAULT = 0;

    /**
     * How often Quartz node registers itself in Quartz database (in milliseconds).
     *
     * This has nothing to do with midPoint cluster state determination. It is a parameter internal to Quartz scheduler.
     */
    private long quartzClusterCheckinInterval;

    private static final long QUARTZ_CLUSTER_CHECKIN_INTERVAL_DEFAULT = 7500;

    /**
     * How long can be Quartz node "unresponsive" (not checking in) (in milliseconds).
     *
     * This has nothing to do with midPoint cluster state determination. It is a parameter internal to Quartz scheduler.
     */
    private long quartzClusterCheckinGracePeriod;

    private static final long QUARTZ_CLUSTER_CHECKIN_GRACE_PERIOD_DEFAULT = 7500;

    private boolean checkForTaskConcurrentExecution;
    private UseThreadInterrupt useThreadInterrupt;
    private int waitingTasksCheckInterval;
    private int stalledTasksCheckInterval;
    private int stalledTasksThreshold;
    private int stalledTasksRepeatedNotificationInterval;
    private boolean runNowKeepsOriginalSchedule;
    private boolean schedulerInitiallyStopped;
    private boolean localNodeClusteringEnabled;

    private TaskExecutionLimitationsType taskExecutionLimitations;

    // quartz jdbc job store specific information
    private String sqlSchemaFile;
    private String jdbcDriverDelegateClass;
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private String dataSource;
    // This saves 10 connections that are mostly idle and only usable for scheduler.
    // Not good for old PG where it collides with strong trn serialization, but otherwise good.
    private boolean useRepositoryConnectionProvider;
    private boolean createQuartzTables;

    private SupportedDatabase database;
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

    private static final List<String> KNOWN_KEYS = List.of(
            STOP_ON_INITIALIZATION_FAILURE_CONFIG_ENTRY,
            THREADS_CONFIG_ENTRY,
            CLUSTERED_CONFIG_ENTRY,
            JDBC_JOB_STORE_CONFIG_ENTRY,
            JDBC_DRIVER_CONFIG_ENTRY,
            JDBC_URL_CONFIG_ENTRY,
            JDBC_USER_CONFIG_ENTRY,
            JDBC_PASSWORD_CONFIG_ENTRY,
            DATA_SOURCE_CONFIG_ENTRY,
            USE_REPOSITORY_CONNECTION_PROVIDER_CONFIG_ENTRY,
            SQL_SCHEMA_FILE_CONFIG_ENTRY,
            CREATE_QUARTZ_TABLES_CONFIG_ENTRY,
            JDBC_DRIVER_DELEGATE_CLASS_CONFIG_ENTRY,
            USE_THREAD_INTERRUPT_CONFIG_ENTRY,
            QUARTZ_CLUSTER_CHECKIN_INTERVAL_CONFIG_ENTRY,
            QUARTZ_CLUSTER_CHECKIN_GRACE_PERIOD_CONFIG_ENTRY,
            NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY,
            NODE_TIMEOUT_CONFIG_ENTRY,
            TEST_MODE_CONFIG_ENTRY,
            WAITING_TASKS_CHECK_INTERVAL_CONFIG_ENTRY,
            STALLED_TASKS_CHECK_INTERVAL_CONFIG_ENTRY,
            STALLED_TASKS_THRESHOLD_CONFIG_ENTRY,
            STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_CONFIG_ENTRY,
            RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_CONFIG_ENTRY,
            SCHEDULER_INITIALLY_STOPPED_CONFIG_ENTRY,
            LOCAL_NODE_CLUSTERING_ENABLED_CONFIG_ENTRY,
            TASK_EXECUTION_LIMITATIONS_CONFIG_ENTRY,
            CHECK_FOR_TASK_CONCURRENT_EXECUTION_CONFIG_ENTRY,
            NODE_ALIVENESS_TIMEOUT_CONFIG_ENTRY,
            NODE_STARTUP_TIMEOUT_CONFIG_ENTRY,
            NODE_STARTUP_DELAY_CONFIG_ENTRY,
            NODE_ALIVENESS_CHECK_INTERVAL_CONFIG_ENTRY
    );

    private static final List<String> DEPRECATED_KEYS = List.of(
            MidpointConfiguration.MIDPOINT_HOME_PROPERTY,
            JMX_CONNECT_TIMEOUT_CONFIG_ENTRY,
            QUARTZ_NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY,
            WORK_ALLOCATION_MAX_RETRIES_ENTRY,
            WORK_ALLOCATION_RETRY_INTERVAL_BASE_ENTRY,
            WORK_ALLOCATION_RETRY_INTERVAL_LIMIT_ENTRY,
            WORK_ALLOCATION_RETRY_EXPONENTIAL_THRESHOLD_ENTRY,
            WORK_ALLOCATION_INITIAL_DELAY_ENTRY,
            WORK_ALLOCATION_DEFAULT_FREE_BUCKET_WAIT_INTERVAL_ENTRY
    );

    void checkAllowedKeys(MidpointConfiguration masterConfig) throws TaskManagerConfigurationException {
        Configuration c = masterConfig.getConfiguration(MidpointConfiguration.TASK_MANAGER_CONFIGURATION);
        checkAllowedKeys(c);
    }

    // todo copied from WfConfiguration -- refactor
    private void checkAllowedKeys(Configuration c) throws TaskManagerConfigurationException {
        Set<String> knownKeysSet = new HashSet<>(TaskManagerConfiguration.KNOWN_KEYS);
        Set<String> deprecatedKeysSet = new HashSet<>(TaskManagerConfiguration.DEPRECATED_KEYS);

        Iterator<String> keyIterator = c.getKeys();
        while (keyIterator.hasNext()) {
            String keyName = keyIterator.next();
            if (Strings.isNullOrEmpty(keyName)) {
                continue; // happens if <taskManager> element is empty
            }

            String normalizedKeyName = StringUtils.substringBefore(keyName, "."); // because of subkeys
            normalizedKeyName = StringUtils.substringBefore(normalizedKeyName, "["); // because of [@xmlns:c]
            int colon = normalizedKeyName.indexOf(':'); // because of c:generalChangeProcessorConfiguration
            if (colon != -1) {
                normalizedKeyName = normalizedKeyName.substring(colon + 1);
            }
            if (deprecatedKeysSet.contains(keyName) || deprecatedKeysSet.contains(normalizedKeyName)) {
                LOGGER.warn("Key {} in task manager configuration is deprecated and has no effect.", keyName);
            } else if (!knownKeysSet.contains(keyName) && !knownKeysSet.contains(normalizedKeyName)) {
                // ...we need to test both because of keys like 'midpoint.home'
                throw new TaskManagerConfigurationException("Unknown key " + keyName + " in task manager configuration");
            }
        }
    }

    void setBasicInformation(MidpointConfiguration masterConfig, OperationResult result) throws TaskManagerConfigurationException {
        Configuration root = masterConfig.getConfiguration();
        Configuration c = masterConfig.getConfiguration(MidpointConfiguration.TASK_MANAGER_CONFIGURATION);

        stopOnInitializationFailure = c.getBoolean(STOP_ON_INITIALIZATION_FAILURE_CONFIG_ENTRY, STOP_ON_INITIALIZATION_FAILURE_DEFAULT);

        threads = c.getInt(THREADS_CONFIG_ENTRY, THREADS_DEFAULT);
        clustered = c.getBoolean(CLUSTERED_CONFIG_ENTRY, CLUSTERED_DEFAULT);
        jdbcJobStore = c.getBoolean(JDBC_JOB_STORE_CONFIG_ENTRY, clustered);

        nodeId =
                new NodeIdComputer(prismContext, repositoryService)
                        .determineNodeId(root, clustered, result);

        hostName = root.getString(MidpointConfiguration.MIDPOINT_HOST_NAME_PROPERTY, null);

        httpPort = root.getInteger(MidpointConfiguration.MIDPOINT_HTTP_PORT_PROPERTY, null);
        url = root.getString(MidpointConfiguration.MIDPOINT_URL_PROPERTY, null);

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
        } catch (IllegalArgumentException e) {
            throw new TaskManagerConfigurationException("Illegal value for " + USE_THREAD_INTERRUPT_CONFIG_ENTRY + ": " + useTI, e);
        }

        nodeRegistrationCycleTime = c.getInt(NODE_REGISTRATION_INTERVAL_CONFIG_ENTRY, NODE_REGISTRATION_CYCLE_TIME_DEFAULT);
        nodeAlivenessCheckInterval = c.getInt(NODE_ALIVENESS_CHECK_INTERVAL_CONFIG_ENTRY, NODE_ALIVENESS_CHECK_INTERVAL_DEFAULT);
        nodeAlivenessTimeout = c.getInt(NODE_ALIVENESS_TIMEOUT_CONFIG_ENTRY, NODE_ALIVENESS_TIMEOUT_DEFAULT);
        nodeStartupTimeout = c.getInt(NODE_STARTUP_TIMEOUT_CONFIG_ENTRY, NODE_STARTUP_TIMEOUT_DEFAULT);
        nodeTimeout = c.getInt(NODE_TIMEOUT_CONFIG_ENTRY, NODE_TIMEOUT_DEFAULT);
        nodeStartupDelay = c.getInt(NODE_STARTUP_DELAY_CONFIG_ENTRY, NODE_STARTUP_DELAY_DEFAULT);

        quartzClusterCheckinInterval = c.getLong(QUARTZ_CLUSTER_CHECKIN_INTERVAL_CONFIG_ENTRY, QUARTZ_CLUSTER_CHECKIN_INTERVAL_DEFAULT);
        quartzClusterCheckinGracePeriod = c.getLong(QUARTZ_CLUSTER_CHECKIN_GRACE_PERIOD_CONFIG_ENTRY, QUARTZ_CLUSTER_CHECKIN_GRACE_PERIOD_DEFAULT);

        checkForTaskConcurrentExecution = c.getBoolean(CHECK_FOR_TASK_CONCURRENT_EXECUTION_CONFIG_ENTRY, CHECK_FOR_TASK_CONCURRENT_EXECUTION_DEFAULT);

        waitingTasksCheckInterval = c.getInt(WAITING_TASKS_CHECK_INTERVAL_CONFIG_ENTRY, WAITING_TASKS_CHECK_INTERVAL_DEFAULT);
        stalledTasksCheckInterval = c.getInt(STALLED_TASKS_CHECK_INTERVAL_CONFIG_ENTRY, STALLED_TASKS_CHECK_INTERVAL_DEFAULT);
        stalledTasksThreshold = c.getInt(STALLED_TASKS_THRESHOLD_CONFIG_ENTRY, STALLED_TASKS_THRESHOLD_DEFAULT);
        stalledTasksRepeatedNotificationInterval = c.getInt(STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_CONFIG_ENTRY, STALLED_TASKS_REPEATED_NOTIFICATION_INTERVAL_DEFAULT);
        runNowKeepsOriginalSchedule = c.getBoolean(RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_CONFIG_ENTRY, RUN_NOW_KEEPS_ORIGINAL_SCHEDULE_DEFAULT);
        schedulerInitiallyStopped = c.getBoolean(SCHEDULER_INITIALLY_STOPPED_CONFIG_ENTRY, false);
        localNodeClusteringEnabled = c.getBoolean(LOCAL_NODE_CLUSTERING_ENABLED_CONFIG_ENTRY, false);

        if (c.containsKey(TASK_EXECUTION_LIMITATIONS_CONFIG_ENTRY)) {
            taskExecutionLimitations = parseExecutionLimitations(c.getString(TASK_EXECUTION_LIMITATIONS_CONFIG_ENTRY));
        }
    }

    // Examples:
    //  - admin-node:2,sync-jobs:4      (2 threads for admin-node, 4 threads for sync-jobs, and the usual default settings)
    //  - admin-node,sync-jobs          (unlimited threads for admin-node and sync-jobs; and the usual default settings)
    //  - #,_,*:0                       (these are default settings: unlimited for the current node (marked as #) and for unlabeled tasks (marked as _), 0 for other tasks)
    //  - admin-node:2,sync-jobs:4,#:0,_:0 (2 for admin-node, 4 for sync-jobs, and default settings are overridden - nothing for current node name, nothing for unlabeled tasks)
    //
    // We assume that no group name contains ':' or ',' characters.
    //
    // package-visible and static because of testing needs
    static TaskExecutionLimitationsType parseExecutionLimitations(String limitations) throws TaskManagerConfigurationException {
        if (limitations == null) {
            return null;
        } else {
            TaskExecutionLimitationsType rv = new TaskExecutionLimitationsType();
            for (String limitation : StringUtils.splitPreserveAllTokens(limitations.trim(), ',')) {
                String[] limitationParts = limitation.trim().split(":");
                String groupName;
                Integer groupLimit;
                if (limitationParts.length == 1) {
                    groupName = limitationParts[0].trim();
                    groupLimit = null;
                } else if (limitationParts.length == 2) {
                    groupName = limitationParts[0].trim();
                    try {
                        String limitValue = limitationParts[1].trim();
                        groupLimit = "*".equals(limitValue) ? null : Integer.parseInt(limitValue);
                    } catch (NumberFormatException e) {
                        throw new TaskManagerConfigurationException("Couldn't parse limitation '" + limitation + "' in limitations specification '" + limitations, e);
                    }
                } else {
                    throw new TaskManagerConfigurationException("Couldn't parse limitation '" + limitation + "' in limitations specification '" + limitations);
                }
                rv.beginGroupLimitation()
                        .groupName(groupName)
                        .limit(groupLimit);
            }
            return rv;
        }
    }

    private static final Map<SupportedDatabase, String> SCHEMAS = Map.ofEntries(
            entry(SupportedDatabase.H2, "tables_h2.sql"),
            entry(SupportedDatabase.POSTGRESQL, "tables_postgres.sql"),
            entry(SupportedDatabase.ORACLE, "tables_oracle.sql"),
            entry(SupportedDatabase.SQLSERVER, "tables_sqlServer.sql"));

    private static final Map<SupportedDatabase, String> DELEGATES = Map.ofEntries(
            entry(SupportedDatabase.H2, "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
            entry(SupportedDatabase.POSTGRESQL, "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate"),
            // TODO shouldn't we use OracleDelegate?
            entry(SupportedDatabase.ORACLE, "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"),
            entry(SupportedDatabase.SQLSERVER, "org.quartz.impl.jdbcjobstore.MSSQLDelegate"));

    void setJdbcJobStoreInformation(
            MidpointConfiguration masterConfig, JdbcRepositoryConfiguration jdbcConfig) {

        Configuration taskManagerConf = masterConfig.getConfiguration(MidpointConfiguration.TASK_MANAGER_CONFIGURATION);

        database = jdbcConfig != null ? jdbcConfig.getDatabaseType() : null;

        String defaultSqlSchemaFile = SCHEMAS.get(database);
        String defaultDriverDelegate = DELEGATES.get(database);

        sqlSchemaFile = taskManagerConf.getString(SQL_SCHEMA_FILE_CONFIG_ENTRY, defaultSqlSchemaFile);
        jdbcDriverDelegateClass = taskManagerConf.getString(JDBC_DRIVER_DELEGATE_CLASS_CONFIG_ENTRY, defaultDriverDelegate);

        createQuartzTables = taskManagerConf.getBoolean(CREATE_QUARTZ_TABLES_CONFIG_ENTRY, CREATE_QUARTZ_TABLES_DEFAULT);
        databaseIsEmbedded = jdbcConfig != null && jdbcConfig.isEmbedded();

        String explicitJdbcUrl = taskManagerConf.getString(JDBC_URL_CONFIG_ENTRY, null);
        useRepositoryConnectionProvider = taskManagerConf.getBoolean(
                USE_REPOSITORY_CONNECTION_PROVIDER_CONFIG_ENTRY,
                repositoryService.isNative() && explicitJdbcUrl == null);
        if (useRepositoryConnectionProvider) {
            LOGGER.info("Using connection provider from repository (ignoring all the other database-related configuration)");
            if (jdbcConfig != null && jdbcConfig.isUsingH2()) {
                LOGGER.warn("This option is not supported for H2! Please change the task manager configuration.");
            }
        } else {
            jdbcDriver = taskManagerConf.getString(JDBC_DRIVER_CONFIG_ENTRY,
                    jdbcConfig != null ? jdbcConfig.getDriverClassName() : null);

            if (explicitJdbcUrl == null) {
                if (jdbcConfig != null) {
                    if (jdbcConfig.isEmbedded()) {
                        jdbcUrl = jdbcConfig.getDefaultEmbeddedJdbcUrlPrefix() + "-quartz;MVCC=TRUE;DB_CLOSE_ON_EXIT=FALSE";
                    } else {
                        jdbcUrl = jdbcConfig.getJdbcUrl("mp-scheduler");
                    }
                } else {
                    jdbcUrl = null;
                }
            } else {
                jdbcUrl = explicitJdbcUrl;
            }
            dataSource = taskManagerConf.getString(DATA_SOURCE_CONFIG_ENTRY, null);
            if (dataSource == null && explicitJdbcUrl == null && jdbcConfig != null) {
                dataSource = jdbcConfig.getDataSource();             // we want to use quartz-specific JDBC if there is one (i.e. we do not want to inherit data source from repo in such a case)
            }

            if (dataSource != null) {
                LOGGER.info("Quartz database is at {} (a data source)", dataSource);
            } else {
                LOGGER.info("Quartz database is at {} (a JDBC URL)", jdbcUrl);
            }

            jdbcUser = taskManagerConf.getString(JDBC_USER_CONFIG_ENTRY,
                    jdbcConfig != null ? jdbcConfig.getJdbcUsername() : null);
            jdbcPassword = taskManagerConf.getString(JDBC_PASSWORD_CONFIG_ENTRY,
                    jdbcConfig != null ? jdbcConfig.getJdbcPassword() : null);
        }
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

        notEmpty(nodeId, "Node identifier must be set.");

        mustBeTrue(nodeRegistrationCycleTime > 1 && nodeRegistrationCycleTime <= 600, "Node registration cycle time must be between 1 and 600 seconds");
        mustBeTrue(nodeTimeout > 5, "Node timeout must be at least 5 seconds");
    }

    void validateJdbcJobStoreInformation() throws TaskManagerConfigurationException {
        if (!useRepositoryConnectionProvider && StringUtils.isEmpty(dataSource)) {
            notEmpty(jdbcDriver, "JDBC driver must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
            notEmpty(jdbcUrl, "JDBC URL must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
            notNull(jdbcUser, "JDBC user name must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
            notNull(jdbcPassword, "JDBC password must be specified (either explicitly or via data source; in task manager or in SQL repository configuration)");
        }
        if (StringUtils.isEmpty(jdbcDriverDelegateClass)) {
            throw new TaskManagerConfigurationException("JDBC driver delegate class must be specified (either explicitly or "
                    + "through specifying a database type). It seems that the currently specified database ("
                    + database + ") is not among supported ones (" + DELEGATES.keySet() + "). "
                    + "Please check your repository configuration or specify driver delegate explicitly.");
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

    public int getThreads() {
        return threads;
    }

    public boolean isJdbcJobStore() {
        return jdbcJobStore;
    }

    public boolean isClustered() {
        return clustered;
    }

    public @NotNull String getNodeId() {
        return Objects.requireNonNull(nodeId, "No node ID");
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

    @SuppressWarnings("unused")
    public boolean isStopOnInitializationFailure() {
        return stopOnInitializationFailure;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isDatabaseIsEmbedded() {
        return databaseIsEmbedded;
    }

    public int getNodeTimeout() {
        return nodeTimeout;
    }

    public int getNodeAlivenessTimeout() {
        return nodeAlivenessTimeout;
    }

    public int getNodeStartupTimeout() {
        return nodeStartupTimeout;
    }

    public int getNodeRegistrationCycleTime() {
        return nodeRegistrationCycleTime;
    }

    int getNodeStartupDelay() {
        return nodeStartupDelay;
    }

    public long getQuartzClusterCheckinInterval() {
        return quartzClusterCheckinInterval;
    }

    public long getQuartzClusterCheckinGracePeriod() {
        return quartzClusterCheckinGracePeriod;
    }

    public int getNodeAlivenessCheckInterval() {
        return nodeAlivenessCheckInterval;
    }

    public boolean isCheckForTaskConcurrentExecution() {
        return checkForTaskConcurrentExecution;
    }

    public String getUrl() {
        return url;
    }

    public Integer getHttpPort() {
        return httpPort;
    }

    public String getHostName() {
        return hostName;
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

    @SuppressWarnings("unused")
    public void setCreateQuartzTables(boolean createQuartzTables) {
        this.createQuartzTables = createQuartzTables;
    }

    public boolean isUseRepositoryConnectionProvider() {
        return useRepositoryConnectionProvider;
    }

    public String getDataSource() {
        return dataSource;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isSchedulerInitiallyStopped() {
        return schedulerInitiallyStopped;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isLocalNodeClusteringEnabled() {
        return localNodeClusteringEnabled;
    }

    public TaskExecutionLimitationsType getTaskExecutionLimitations() {
        return taskExecutionLimitations;
    }
}
