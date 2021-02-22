package com.evolveum.midpoint.task.quartzimpl.quartz;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jetbrains.annotations.NotNull;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.SchedulerListenerSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManagerInitializationException;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerConfiguration;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.sql.ScriptRunner;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;

/**
 * Helps with Quartz starting and stopping
 */
@Component
class QuartzInitializationHelper implements BeanFactoryAware {

    private static final Trace LOGGER = TraceManager.getTrace(QuartzInitializationHelper.class);

    /**
     * In test mode we set idleWaitTime to a lower value, because on some occasions
     * the Quartz scheduler "forgot" to fire a trigger immediately after creation,
     * and the default delay of 10s is too much for most of the tests.
     */
    public static final int IDLE_WAIT_TIME_FOR_TEST_MODE_JDBC = 5000;
    public static final int IDLE_WAIT_TIME_TEST_MODE_MEMORY = 2000;
    public static final int IDLE_WAIT_TIME = 10000;

    @Autowired private TaskManagerConfiguration configuration;
    @Autowired private ClusterManager clusterManager;
    @Autowired private LocalScheduler localScheduler;

    private BeanFactory beanFactory;

    /**
     * Prepares Quartz scheduler. Configures its properties (based on Task Manager configuration) and creates the instance.
     * Does not start the scheduler, because this is done during post initialization.
     */
    public void initializeScheduler() throws TaskManagerInitializationException {

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
                DataSourceFactory dataSourceFactory = (DataSourceFactory) beanFactory.getBean("dataSourceFactory");
                int index = (int) (Math.random() * Integer.MAX_VALUE);
                RepositoryConnectionProvider.DATA_SOURCES.put(index, dataSourceFactory.getDataSource());
                quartzProperties.put("org.quartz.dataSource." + myDs + ".connectionProvider.class", RepositoryConnectionProvider.class.getName());
                quartzProperties.put("org.quartz.dataSource." + myDs + ".dataSourceIndex", String.valueOf(index));
            } else if (configuration.getDataSource() != null) {
                quartzProperties.put("org.quartz.dataSource." + myDs + ".jndiURL", configuration.getDataSource());
            } else {
                quartzProperties.put("org.quartz.dataSource." + myDs + ".provider", "hikaricp");
                quartzProperties.put("org.quartz.dataSource." + myDs + ".driver", configuration.getJdbcDriver());
                quartzProperties.put("org.quartz.dataSource." + myDs + ".URL", configuration.getJdbcUrl());
                quartzProperties.put("org.quartz.dataSource." + myDs + ".user", configuration.getJdbcUser());
                quartzProperties.put("org.quartz.dataSource." + myDs + ".password", configuration.getJdbcPassword());
            }

            quartzProperties.put("org.quartz.jobStore.isClustered", configuration.isClustered() ? "true" : "false");

        } else {
            quartzProperties.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        }
        quartzProperties.put("org.quartz.scheduler.instanceName", "midPointScheduler");

        quartzProperties.put("org.quartz.scheduler.instanceId", configuration.getNodeId());

        quartzProperties.put("org.quartz.scheduler.skipUpdateCheck", "true");
        quartzProperties.put("org.quartz.threadPool.threadCount", Integer.toString(configuration.getThreads()));
        quartzProperties.put("org.quartz.scheduler.idleWaitTime", Integer.toString(determineIdleWaitTime()));

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
            localScheduler.setQuartzScheduler(scheduler);
            LOGGER.info("... Quartz scheduler initialized.");
        } catch (SchedulerException e) {
            throw new TaskManagerInitializationException("Cannot initialize the Quartz scheduler", e);
        }
    }

    private int determineIdleWaitTime() {
        int idleWaitTime;
        if (configuration.isTestMode()) {
            if (configuration.isJdbcJobStore()) {
                idleWaitTime = IDLE_WAIT_TIME_FOR_TEST_MODE_JDBC;
            } else {
                idleWaitTime = IDLE_WAIT_TIME_TEST_MODE_MEMORY;
            }
        } else {
            idleWaitTime = IDLE_WAIT_TIME;
        }
        return idleWaitTime;
    }

    private void setMySchedulerListener(Scheduler scheduler) throws SchedulerException {
        for (SchedulerListener listener : scheduler.getListenerManager().getSchedulerListeners()) {
            if (listener instanceof MySchedulerListener) {
                scheduler.getListenerManager().removeSchedulerListener(listener);
            }
        }
        scheduler.getListenerManager().addSchedulerListener(new MySchedulerListener());
    }

    /**
     * Creates Quartz database schema, if it does not exist.
     */
    private void createQuartzDbSchema(TaskManagerConfiguration configuration) throws TaskManagerInitializationException {
        Connection connection = getConnection(configuration);
        LOGGER.debug("createQuartzDbSchema: trying JDBC connection {}", connection);
        //noinspection TryFinallyCanBeTryWithResources
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
                DataSourceFactory dataSourceFactory = (DataSourceFactory) beanFactory.getBean("dataSourceFactory");
                connection = dataSourceFactory.getDataSource().getConnection();
            } else if (configuration.getDataSource() != null) {
                DataSource dataSource;
                try {
                    InitialContext context = new InitialContext();
                    dataSource = (DataSource) context.lookup(configuration.getDataSource());
                } catch (NamingException e) {
                    throw new TaskManagerInitializationException("Cannot find a data source '" + configuration.getDataSource() + "': " + e.getMessage(), e);
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

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private class MySchedulerListener extends SchedulerListenerSupport {
        @Override
        public void schedulerStarting() {
            OperationResult result = new OperationResult(LocalScheduler.class.getName() + ".schedulerStarting");
            NodeType node = clusterManager.getFreshVerifiedLocalNodeObject(result);
            if (node != null) {
                localScheduler.setLocalExecutionLimitations(node.getTaskExecutionLimitations());
            } else {
                LOGGER.warn("Couldn't set Quartz scheduler execution capabilities, because local node object couldn't be correctly read.");
            }
        }
    }

}
