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
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
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
    private static final String CONFIG_JDBC_DRIVER = "jdbcDriver";
    private static final String CONFIG_JDBC_URL = "jdbcUrl";
    private static final String CONFIG_JDBC_USER = "jdbcUser";
    private static final String CONFIG_JDBC_PASSWORD = "jdbcPassword";
    private static final String CONFIG_SQL_SCHEMA_FILE = "sqlSchemaFile";
    private static final String CONFIG_JDBC_DRIVER_DELEGATE_CLASS = "jdbcDriverDelegateClass";
    private static final String CONFIG_USE_THREAD_INTERRUPT = "useThreadInterrupt";

    private static final String MIDPOINT_NODE_ID_PROPERTY = "midpoint.nodeId";
    private static final String JMX_PORT_PROPERTY = "com.sun.management.jmxremote.port";
    private static final String SUREFIRE_PRESENCE_PROPERTY = "surefire.real.class.path";

    private static final int DEFAULT_THREADS = 10;
    private static final boolean DEFAULT_CLUSTERED = false;             // do not change this value!
    private static final String DEFAULT_NODE_ID = "DefaultNode";
    private static final int DEFAULT_JMX_PORT = 20001;
    private static final String USE_THREAD_INTERRUPT_DEFAULT = "whenNecessary";

    private int threads;
    private boolean jdbcJobStore;
    private boolean clustered;
    private String nodeId;
    private int jmxPort;

    private UseThreadInterrupt useThreadInterrupt;

    // quartz jdbc job store specific information
    private String sqlSchemaFile;
    private String jdbcDriverDelegateClass;
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private String hibernateDialect;

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

    TaskManagerConfiguration(MidpointConfiguration masterConfig) {
        Configuration c = masterConfig.getConfiguration(TASK_MANAGER_CONFIGURATION);

        threads = c.getInt(CONFIG_THREADS, DEFAULT_THREADS);
        clustered = c.getBoolean(CONFIG_CLUSTERED, DEFAULT_CLUSTERED);
        jdbcJobStore = c.getBoolean(CONFIG_JDBC_JOB_STORE, clustered);

        nodeId = System.getProperty(MIDPOINT_NODE_ID_PROPERTY);
        if (StringUtils.isEmpty(nodeId))
            nodeId = DEFAULT_NODE_ID;

        String portString = System.getProperty(JMX_PORT_PROPERTY);
        if (StringUtils.isEmpty(portString)) {
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

        String useTI = c.getString(CONFIG_USE_THREAD_INTERRUPT, USE_THREAD_INTERRUPT_DEFAULT);
        try {
            useThreadInterrupt = UseThreadInterrupt.fromValue(useTI);
        } catch(IllegalArgumentException e) {
            throw new SystemException("Illegal value for " + CONFIG_USE_THREAD_INTERRUPT + ": " + useTI, e);
        }

    }

    private static final Map<String,String> schemas = new HashMap<String,String>();
    private static final Map<String,String> delegates = new HashMap<String,String>();

    static void addDbInfo(String dialect, String schema, String delegate) {
        schemas.put(dialect, schema);
        delegates.put(dialect, delegate);
    }

    static {
        addDbInfo("org.hibernate.dialect.H2Dialect", "tables_h2.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.PostgreSQLDialect", "tables_postgres.sql", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        addDbInfo("org.hibernate.dialect.MySQLDialect", "tables_mysql.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.MySQLInnoDBDialect", "tables_mysql.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.OracleDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle9Dialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle8iDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle9iDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.Oracle10gDialect", "tables_oracle.sql", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        addDbInfo("org.hibernate.dialect.SQLServerDialect", "tables_sqlServer.sql", "org.quartz.impl.jdbcjobstore.MSSQLDelegate");
    }

    void setJdbcJobStoreInformation(MidpointConfiguration masterConfig, SqlRepositoryConfiguration sqlConfig) {

        Configuration c = masterConfig.getConfiguration(TASK_MANAGER_CONFIGURATION);

        jdbcDriver = c.getString(CONFIG_JDBC_DRIVER, sqlConfig.getDriverClassName());
        jdbcUrl = c.getString(CONFIG_JDBC_URL, sqlConfig.getJdbcUrl());
        jdbcUser = c.getString(CONFIG_JDBC_USER, sqlConfig.getJdbcUsername());
        jdbcPassword = c.getString(CONFIG_JDBC_PASSWORD, sqlConfig.getJdbcPassword());

        hibernateDialect = sqlConfig.getHibernateDialect();

        String defaultSqlSchemaFile = schemas.get(hibernateDialect);
        String defaultDriverDelegate = delegates.get(hibernateDialect);

        sqlSchemaFile = c.getString(CONFIG_SQL_SCHEMA_FILE, defaultSqlSchemaFile);
        jdbcDriverDelegateClass = c.getString(CONFIG_JDBC_DRIVER_DELEGATE_CLASS, defaultDriverDelegate);
    }

    void validate() {

        if (threads < 1) {
            LOGGER.warn("The configured number of threads is too low, setting it to 5.");
            threads = 5;
        }

        if (clustered) {
            mustBeTrue(jdbcJobStore, "Clustered task manager requires JDBC Quartz job store.");
        }

        notEmpty(nodeId, "Node identifier must be known.");     // only a safeguard, because it gets a default value if not set
        mustBeFalse(clustered && jmxPort == 0, "JMX port number must be known.");    // the same

    }

    void validateJdbcConfig() {

        notEmpty(jdbcDriver, "JDBC driver must be specified (either explicitly or in SQL repository configuration)");
        notEmpty(jdbcUrl, "JDBC URL must be specified (either explicitly or in SQL repository configuration).");
        notNull(jdbcUser, "JDBC user name must be specified (either explicitly or in SQL repository configuration).");
        notNull(jdbcPassword, "JDBC password must be specified (either explicitly or in SQL repository configuration).");
        notEmpty(jdbcDriverDelegateClass, "JDBC driver delegate class must be specified (either explicitly or through one of supported Hibernate dialects).");
        notEmpty(sqlSchemaFile, "SQL schema file must be specified (either explicitly or through one of supported Hibernate dialects).");
    }

    // TODO: change SystemException to something more reasonable

    private void notEmpty(String value, String message) {
        if (StringUtils.isEmpty(value)) {
            throw new SystemException(message);
        }
    }

    private void notNull(String value, String message) {
        if (value == null) {
            throw new SystemException(message);
        }
    }

    private void mustBeTrue(boolean condition, String message) {
        if (!condition) {
            throw new SystemException(message);
        }
    }

    private void mustBeFalse(boolean condition, String message) {
        if (condition) {
            throw new SystemException(message);
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

    public boolean isReusableQuartzScheduler() {
        return reusableQuartzScheduler;
    }

    public UseThreadInterrupt getUseThreadInterrupt() {
        return useThreadInterrupt;
    }
}
