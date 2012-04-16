/**
 * Copyright (c) 2011 Evolveum
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 *
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;

import java.util.Properties;

/**
 * @author Pavol Mederly
 */
public class TaskManagerConfiguration {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskManagerConfiguration.class);

    private static final String TASK_MANAGER_CONFIGURATION = "midpoint.taskManager";
    private static final String CONFIG_THREADS = "threads";
    private static final String CONFIG_CLUSTERED = "clustered";
    private static final String CONFIG_JDBC_JOB_STORE = "jdbcJobStore";

    private static final String MIDPOINT_NODE_ID_PROPERTY = "midpoint.nodeId";
    private static final String JMX_PORT_PROPERTY = "com.sun.management.jmxremote.port";
    private static final String SUREFIRE_PRESENCE_PROPERTY = "surefire.real.class.path";

    private static final int DEFAULT_THREADS = 10;
    private static final boolean DEFAULT_CLUSTERED = false;             // do not change this value!
    private static final String DEFAULT_NODE_ID = "DefaultNode";
    private static final int DEFAULT_JMX_PORT = 20001;

    private int threads;
    private boolean jdbcJobStore;
    private boolean clustered;
    private String nodeId;
    private int jmxPort;

    /*
      * Whether to allow reusing quartz scheduler after task manager shutdown.
      *
      * Concretely, if it is set to 'true', quartz scheduler will not be shut down, only paused.
      * This allows for restarting it (scheduler cannot be started, if it was shut down:
      * http://quartz-scheduler.org/api/2.1.0/org/quartz/Scheduler.html#shutdown())
      *
      * By default, if run within TestNG (determined by seeing SUREFIRE_PRESENCE_PROPERTY set), we allow the reuse.
      * If run within Tomcat, we do not, because pausing the scheduler does NOT stop the execution threads.
      */
    private boolean reusableQuartzScheduler = false;

    // quartz jdbc job store specific information
    private String sqlSchemaFile;
    private String sqlDriverDelegateClass;
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    TaskManagerConfiguration(MidpointConfiguration masterConfig) {
        Configuration c = masterConfig.getConfiguration(TASK_MANAGER_CONFIGURATION);

        threads = c.getInt(CONFIG_THREADS, DEFAULT_THREADS);
        clustered = c.getBoolean(CONFIG_CLUSTERED, DEFAULT_CLUSTERED);
        jdbcJobStore = c.getBoolean(CONFIG_JDBC_JOB_STORE, clustered);

        nodeId = System.getProperty(MIDPOINT_NODE_ID_PROPERTY);
        if (nodeId == null)
            nodeId = DEFAULT_NODE_ID;

        String portString = System.getProperty(JMX_PORT_PROPERTY);
        if (portString == null) {
            jmxPort = DEFAULT_JMX_PORT;
        } else {
            try {
                jmxPort = Integer.parseInt(portString);
            } catch(NumberFormatException nfe) {
                throw new SystemException("Cannot get JMX management port - invalid integer value of " + portString, nfe);
            }
        }

        Properties sp = System.getProperties();
        if (sp.containsKey(SUREFIRE_PRESENCE_PROPERTY)) {
            LOGGER.info("Determined to run in a test environment, setting reusableQuartzScheduler to 'true'.");
            reusableQuartzScheduler = true;
        }

    }

    void setJdbcJobStoreInformation(SqlRepositoryConfiguration sqlConfig) {

        jdbcDriver = sqlConfig.getDriverClassName();
        jdbcUrl = sqlConfig.getJdbcUrl();
        jdbcUser = sqlConfig.getJdbcUsername();
        jdbcPassword = sqlConfig.getJdbcPassword();

        String dialect = sqlConfig.getHibernateDialect();

        if ("org.hibernate.dialect.H2Dialect".equals(dialect)) {
            sqlSchemaFile = "tables_h2.sql";
            sqlDriverDelegateClass = "org.quartz.impl.jdbcjobstore.StdJDBCDelegate";
        } else {
            // TODO: include other databases
            throw new SystemException("The database with dialect " + dialect + " is not supported for the use as scheduler JDBC job store.");
        }
    }

    void validate(boolean repoIsSql) {
        if (threads < 1) {
            LOGGER.warn("The configured number of threads is too low, setting it to 5.");
            threads = 5;
        }
        if (jdbcJobStore && !repoIsSql) {
            throw new SystemException("It is not possible to use JDBC Quartz job store without SQL repository.");
        }

        if (clustered && !jdbcJobStore) {
            throw new SystemException("Clustered task manager requires JDBC Quartz job store.");
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

    public String getSqlDriverDelegateClass() {
        return sqlDriverDelegateClass;
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

    public boolean isReusableQuartzScheduler() {
        return reusableQuartzScheduler;
    }

}
