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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.helpers.OrgClosureManager;
import com.evolveum.midpoint.repo.sql.util.MidPointConnectionCustomizer;
import com.evolveum.midpoint.repo.sql.util.MidPointMySQLDialect;
import com.evolveum.midpoint.repo.sql.util.MidPointPostgreSQLDialect;
import com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.h2.Driver;
import org.hibernate.dialect.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database.*;

/**
 * This class is used for SQL repository configuration. It reads values from Apache configuration object (xml).
 *
 * @author lazyman
 */
public class SqlRepositoryConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryConfiguration.class);

    enum Database {

    	// we might include other dialects if needed (but the ones listed here are the recommended ones)
        H2(DRIVER_H2, H2Dialect.class.getName()),
		MYSQL(DRIVER_MYSQL, MidPointMySQLDialect.class.getName()),
		POSTGRESQL(DRIVER_POSTGRESQL, MidPointPostgreSQLDialect.class.getName()),
		SQLSERVER(DRIVER_SQLSERVER, UnicodeSQLServer2008Dialect.class.getName()),
		ORACLE(DRIVER_ORACLE, Oracle12cDialect.class.getName()),
		MARIADB(DRIVER_MARIADB, MidPointMySQLDialect.class.getName());

        // order is important! (the first value is the default)
		@NotNull List<String> drivers;
		@NotNull List<String> dialects;

		Database(String driver, String... dialects) {
			this.drivers = Collections.singletonList(driver);
			this.dialects = Arrays.asList(dialects);
		}

		public static Database findDatabase(String databaseName) {
			if (StringUtils.isBlank(databaseName)) {
				return null;
			}
			for (Database database : values()) {
				if (database.name().equalsIgnoreCase(databaseName)) {
					return database;
				}
			}
			throw new IllegalArgumentException("Unsupported database type: " + databaseName);
		}

		public String getDefaultHibernateDialect() {
			return dialects.get(0);
		}

		public String getDefaultDriverClassName() {
			return drivers.get(0);
		}

		public boolean containsDriver(String driverClassName) {
			return drivers.contains(driverClassName);
		}

		public boolean containsDialect(String hibernateDialect) {
			return dialects.contains(hibernateDialect);
		}
	}

    public static final String PROPERTY_DATABASE = "database";
    public static final String PROPERTY_BASE_DIR = "baseDir";
    public static final String PROPERTY_DROP_IF_EXISTS = "dropIfExists";
    public static final String PROPERTY_AS_SERVER = "asServer";
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_FILE_NAME = "fileName";
    public static final String PROPERTY_TCP_SSL = "tcpSSL";
    public static final String PROPERTY_EMBEDDED = "embedded";
    public static final String PROPERTY_DRIVER_CLASS_NAME = "driverClassName";
    public static final String PROPERTY_HIBERNATE_HBM2DDL = "hibernateHbm2ddl";
    public static final String PROPERTY_HIBERNATE_DIALECT = "hibernateDialect";
    public static final String PROPERTY_JDBC_PASSWORD = "jdbcPassword";
    public static final String PROPERTY_JDBC_USERNAME = "jdbcUsername";
    public static final String PROPERTY_JDBC_URL = "jdbcUrl";
    public static final String PROPERTY_DATASOURCE = "dataSource";
    public static final String PROPERTY_USE_ZIP = "useZip";
    public static final String PROPERTY_MIN_POOL_SIZE = "minPoolSize";
    public static final String PROPERTY_MAX_POOL_SIZE = "maxPoolSize";

    // concurrency properties
    public static final String PROPERTY_TRANSACTION_ISOLATION = "transactionIsolation";
    public static final String PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE = "lockForUpdateViaHibernate";
    public static final String PROPERTY_LOCK_FOR_UPDATE_VIA_SQL = "lockForUpdateViaSql";
    public static final String PROPERTY_USE_READ_ONLY_TRANSACTIONS = "useReadOnlyTransactions";
    public static final String PROPERTY_PERFORMANCE_STATISTICS_FILE = "performanceStatisticsFile";
    public static final String PROPERTY_PERFORMANCE_STATISTICS_LEVEL = "performanceStatisticsLevel";

    //other
    public static final String PROPERTY_ITERATIVE_SEARCH_BY_PAGING = "iterativeSearchByPaging";
    public static final String PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE = "iterativeSearchByPagingBatchSize";

    //closure
    public static final String PROPERTY_IGNORE_ORG_CLOSURE = "ignoreOrgClosure";
    public static final String PROPERTY_ORG_CLOSURE_STARTUP_ACTION = "orgClosureStartupAction";
    public static final String PROPERTY_SKIP_ORG_CLOSURE_STRUCTURE_CHECK = "skipOrgClosureStructureCheck";
    public static final String PROPERTY_STOP_ON_ORG_CLOSURE_STARTUP_FAILURE = "stopOnOrgClosureStartupFailure";

    private static final String DRIVER_H2 = Driver.class.getName();
    private static final String DRIVER_MYSQL = "com.mysql.cj.jdbc.Driver";
    private static final String DRIVER_MARIADB = "org.mariadb.jdbc.Driver";
    private static final String DRIVER_SQLSERVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    private static final String DRIVER_ORACLE = "oracle.jdbc.OracleDriver";

    private String database = null;

    //embedded configuration
    private boolean embedded = true;
    private boolean asServer = false;
    private String baseDir;
    private String fileName;
    private boolean tcpSSL;
    private int port = 5437;
    private boolean dropIfExists;
    //connection for hibernate
    private String driverClassName;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private String hibernateDialect;
    private String hibernateHbm2ddl;
    private String dataSource;
    private int minPoolSize = 8;
    private int maxPoolSize = 20;
    private boolean useZip;

    private TransactionIsolation transactionIsolation;
    private boolean lockForUpdateViaHibernate;
    private boolean lockForUpdateViaSql;
    private boolean useReadOnlyTransactions;
    private String performanceStatisticsFile;
    private int performanceStatisticsLevel;

    private boolean iterativeSearchByPaging;
    private int iterativeSearchByPagingBatchSize;

    private boolean ignoreOrgClosure;
    private OrgClosureManager.StartupAction orgClosureStartupAction;
    private boolean skipOrgClosureStructureCheck;
    private boolean stopOnOrgClosureStartupFailure;

    public SqlRepositoryConfiguration(Configuration configuration) {
        setDatabase(configuration.getString(PROPERTY_DATABASE, database));
		setDataSource(configuration.getString(PROPERTY_DATASOURCE, null));

		computeDefaultDatabaseParameters();

        setAsServer(configuration.getBoolean(PROPERTY_AS_SERVER, embedded));
        setBaseDir(configuration.getString(PROPERTY_BASE_DIR, baseDir));
        setDriverClassName(configuration.getString(PROPERTY_DRIVER_CLASS_NAME, driverClassName));
        setEmbedded(configuration.getBoolean(PROPERTY_EMBEDDED, embedded));
        setHibernateDialect(configuration.getString(PROPERTY_HIBERNATE_DIALECT, hibernateDialect));
        setHibernateHbm2ddl(configuration.getString(PROPERTY_HIBERNATE_HBM2DDL, hibernateHbm2ddl));
        setJdbcPassword(configuration.getString(PROPERTY_JDBC_PASSWORD, jdbcPassword));
        setJdbcUrl(configuration.getString(PROPERTY_JDBC_URL, jdbcUrl));
        setJdbcUsername(configuration.getString(PROPERTY_JDBC_USERNAME, jdbcUsername));
        setPort(configuration.getInt(PROPERTY_PORT, port));
        setTcpSSL(configuration.getBoolean(PROPERTY_TCP_SSL, tcpSSL));
        setFileName(configuration.getString(PROPERTY_FILE_NAME, fileName));
        setDropIfExists(configuration.getBoolean(PROPERTY_DROP_IF_EXISTS, dropIfExists));
        setMinPoolSize(configuration.getInt(PROPERTY_MIN_POOL_SIZE, minPoolSize));
        setMaxPoolSize(configuration.getInt(PROPERTY_MAX_POOL_SIZE, maxPoolSize));
        setUseZip(configuration.getBoolean(PROPERTY_USE_ZIP, useZip));

        computeDefaultConcurrencyParameters();

        setTransactionIsolation(configuration.getString(PROPERTY_TRANSACTION_ISOLATION, transactionIsolation.value()));
        setLockForUpdateViaHibernate(configuration.getBoolean(PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE, lockForUpdateViaHibernate));
        setLockForUpdateViaSql(configuration.getBoolean(PROPERTY_LOCK_FOR_UPDATE_VIA_SQL, lockForUpdateViaSql));
        setUseReadOnlyTransactions(configuration.getBoolean(PROPERTY_USE_READ_ONLY_TRANSACTIONS, useReadOnlyTransactions));
        setPerformanceStatisticsFile(configuration.getString(PROPERTY_PERFORMANCE_STATISTICS_FILE, performanceStatisticsFile));
        setPerformanceStatisticsLevel(configuration.getInt(PROPERTY_PERFORMANCE_STATISTICS_LEVEL, performanceStatisticsLevel));

        computeDefaultIterativeSearchParameters();

        setIterativeSearchByPaging(configuration.getBoolean(PROPERTY_ITERATIVE_SEARCH_BY_PAGING, iterativeSearchByPaging));
        setIterativeSearchByPagingBatchSize(configuration.getInt(PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE, iterativeSearchByPagingBatchSize));

        setIgnoreOrgClosure(configuration.getBoolean(PROPERTY_IGNORE_ORG_CLOSURE, false));
        setOrgClosureStartupAction(configuration.getString(PROPERTY_ORG_CLOSURE_STARTUP_ACTION, OrgClosureManager.StartupAction.REBUILD_IF_NEEDED.toString()));
        setSkipOrgClosureStructureCheck(configuration.getBoolean(PROPERTY_SKIP_ORG_CLOSURE_STRUCTURE_CHECK, false));
        setStopOnOrgClosureStartupFailure(configuration.getBoolean(PROPERTY_STOP_ON_ORG_CLOSURE_STARTUP_FAILURE, true));
    }

    private void computeDefaultDatabaseParameters() {
		Database db = Database.findDatabase(getDatabase());
		if (db == null) {
			if (dataSource != null) {
				return;		// no defaults in this case
			} else {
				db = H2;
			}
		}
		if (db == H2) {
			embedded = true;
			hibernateHbm2ddl = "update";
		} else {
			embedded = false;
			hibernateHbm2ddl = "validate";
		}
		hibernateDialect = db.getDefaultHibernateDialect();
		driverClassName = db.getDefaultDriverClassName();
    }

    private void computeDefaultConcurrencyParameters() {
        if (isUsingH2()) {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = false;        // h2 does not support "SET TRANSACTION READ ONLY" command
        } else if (isUsingMySQL() || isUsingMariaDB()) {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = true;
        } else if (isUsingOracle()) {
            transactionIsolation = TransactionIsolation.READ_COMMITTED;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = true;
            useReadOnlyTransactions = true;
        } else if (isUsingPostgreSQL()) {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = true;
        } else if (isUsingSQLServer()) {
            transactionIsolation = TransactionIsolation.SNAPSHOT;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = false;
        } else {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = true;
            LOGGER.warn("Fine-tuned concurrency parameters defaults for hibernate dialect " + hibernateDialect
                    + " not found; using the following defaults: transactionIsolation = " + transactionIsolation
                    + ", lockForUpdateViaHibernate = " + lockForUpdateViaHibernate
                    + ", lockForUpdateViaSql = " + lockForUpdateViaSql
                    + ", useReadOnlyTransactions = " + useReadOnlyTransactions
                    + ". Please override them if necessary.");
        }
    }

    private void computeDefaultIterativeSearchParameters() {
        if (isUsingH2()) {
            iterativeSearchByPaging = true;
            iterativeSearchByPagingBatchSize = 50;
        } else if (isUsingMySQL() || isUsingMariaDB()) {
            iterativeSearchByPaging = true;
            iterativeSearchByPagingBatchSize = 50;
        } else {
            iterativeSearchByPaging = false;
        }
    }

    /**
     * Configuration validation.
     *
     * @throws RepositoryServiceFactoryException if configuration is invalid.
     */
    public void validate() throws RepositoryServiceFactoryException {
        if (StringUtils.isEmpty(getDataSource())) {
            notEmpty(getJdbcUrl(), "JDBC Url is empty or not defined.");
            notEmpty(getJdbcUsername(), "JDBC user name is empty or not defined.");
            notNull(getJdbcPassword(), "JDBC password is not defined.");
            notEmpty(getDriverClassName(), "Driver class name is empty or not defined.");
        }

        notEmpty(getHibernateDialect(), "Hibernate dialect is empty or not defined.");
        notEmpty(getHibernateHbm2ddl(), "Hibernate hbm2ddl option is empty or not defined.");

        if (isEmbedded()) {
            notEmpty(getBaseDir(), "Base dir is empty or not defined.");
            if (isAsServer()) {
                if (getPort() < 0 || getPort() > 65535) {
                    throw new RepositoryServiceFactoryException("Port must be in interval (0-65534)");
                }
            }
        }

        if (getMinPoolSize() <= 0) {
            throw new RepositoryServiceFactoryException("Min. pool size must be greater than zero.");
        }

        if (getMaxPoolSize() <= 0) {
            throw new RepositoryServiceFactoryException("Max. pool size must be greater than zero.");
        }

        if (getMinPoolSize() > getMaxPoolSize()) {
            throw new RepositoryServiceFactoryException("Max. pool size must be greater than min. pool size.");
        }
    }

    private void notNull(String value, String message) throws RepositoryServiceFactoryException {
        if (value == null) {
            throw new RepositoryServiceFactoryException(message);
        }
    }

    private void notEmpty(String value, String message) throws RepositoryServiceFactoryException {
        if (StringUtils.isEmpty(value)) {
            throw new RepositoryServiceFactoryException(message);
        }
    }

    /**
     * @return Returns true if repository is running in embedded server mode, otherwise false. Default is false.
     */
    public boolean isAsServer() {
        return asServer;
    }

    public void setAsServer(boolean asServer) {
        this.asServer = asServer;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    /**
     * Value represents hibernate dialect used to communicate with database. You can choose from
     * <a href="http://docs.jboss.org/hibernate/core/4.0/manual/en-US/html/session-configuration.html#configuration-optional-dialects">dialects</a>
     * <p/>
     * It's used in "hibernate.dialect" property
     *
     * @return hibernate dialect
     */
    public String getHibernateDialect() {
        return hibernateDialect;
    }

    public void setHibernateDialect(String hibernateDialect) {
        this.hibernateDialect = hibernateDialect;
    }

    public String getHibernateHbm2ddl() {
        return hibernateHbm2ddl;
    }

    public void setHibernateHbm2ddl(String hibernateHbm2ddl) {
        this.hibernateHbm2ddl = hibernateHbm2ddl;
    }

    /**
     * @return Password for JDBC connection. (Optional)
     */
    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * @return JDBC URL connection string for hibernate data source. (for embedded mode it's created automatically).
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * @return Username for JDBC connection. (Optional)
     */
    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    /**
     * @return Port number if repository is running in embedded server mode. Default is 5437.
     */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Value represents repository running in embedded server mode with SSL turned on/off. Default value is false.
     *
     * @return Returns true if repository is running in embedded server mode and SSL turned on.
     */
    public boolean isTcpSSL() {
        return tcpSSL;
    }

    public void setTcpSSL(boolean tcpSSL) {
        this.tcpSSL = tcpSSL;
    }

    /**
     * Used in embedded mode to define h2 database file name. Default will be "midpoint".
     *
     * @return name of DB file
     */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isDropIfExists() {
        return dropIfExists;
    }

    public void setDropIfExists(boolean dropIfExists) {
        this.dropIfExists = dropIfExists;
    }

    public TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(TransactionIsolation transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }

    public void setTransactionIsolation(String transactionIsolation) {
        this.transactionIsolation = TransactionIsolation.fromValue(transactionIsolation);

        // ugly hack, but I know of no way to work around
        MidPointConnectionCustomizer.setTransactionIsolation(this.transactionIsolation);
    }

    public boolean isLockForUpdateViaHibernate() {
        return lockForUpdateViaHibernate;
    }

    public void setLockForUpdateViaHibernate(boolean lockForUpdateViaHibernate) {
        this.lockForUpdateViaHibernate = lockForUpdateViaHibernate;
    }

    public boolean isLockForUpdateViaSql() {
        return lockForUpdateViaSql;
    }

    public void setLockForUpdateViaSql(boolean lockForUpdateViaSql) {
        this.lockForUpdateViaSql = lockForUpdateViaSql;
    }

    public boolean isUseReadOnlyTransactions() {
        return useReadOnlyTransactions;
    }

    public void setUseReadOnlyTransactions(boolean useReadOnlyTransactions) {
        this.useReadOnlyTransactions = useReadOnlyTransactions;
    }

    public String getPerformanceStatisticsFile() {
        return performanceStatisticsFile;
    }

    public void setPerformanceStatisticsFile(String performanceStatisticsFile) {
        this.performanceStatisticsFile = performanceStatisticsFile;
    }

    public int getPerformanceStatisticsLevel() {
        return performanceStatisticsLevel;
    }

    public void setPerformanceStatisticsLevel(int performanceStatisticsLevel) {
        this.performanceStatisticsLevel = performanceStatisticsLevel;
    }

    public boolean isIterativeSearchByPaging() {
        return iterativeSearchByPaging;
    }

    public void setIterativeSearchByPaging(boolean iterativeSearchByPaging) {
        this.iterativeSearchByPaging = iterativeSearchByPaging;
    }

    public int getIterativeSearchByPagingBatchSize() {
        return iterativeSearchByPagingBatchSize;
    }

    public void setIterativeSearchByPagingBatchSize(int iterativeSearchByPagingBatchSize) {
        this.iterativeSearchByPagingBatchSize = iterativeSearchByPagingBatchSize;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public boolean isUseZip() {
        return useZip;
    }

    public void setUseZip(boolean useZip) {
        this.useZip = useZip;
    }

    public boolean isIgnoreOrgClosure() {
        return ignoreOrgClosure;
    }

    public void setIgnoreOrgClosure(boolean value) {
        this.ignoreOrgClosure = value;
    }

    public OrgClosureManager.StartupAction getOrgClosureStartupAction() {
        return orgClosureStartupAction;
    }

    public void setOrgClosureStartupAction(String orgClosureStartupAction) {
        this.orgClosureStartupAction = OrgClosureManager.StartupAction.fromValue(orgClosureStartupAction);
    }

    public boolean isUsingH2() {
        return isUsing(H2);
    }

	private boolean isUsing(Database db) {
		return db.name().equalsIgnoreCase(database)
				|| db.containsDriver(driverClassName)
				|| db.containsDialect(hibernateDialect);
	}

	public boolean isUsingOracle() {
        return isUsing(ORACLE);
    }

    public boolean isUsingMySQL() {
        return isUsing(MYSQL);
    }

    public boolean isUsingMariaDB() {
		// Note that MySQL and MariaDB share the same hibernateDialect.
		// So if this is the only information (e.g. when using tomcat datasource configuration),
		// we want only isUsingMySQL to return 'true'.
        return isUsing(MARIADB) && !isUsing(MYSQL);
    }

    public boolean isUsingPostgreSQL() {
        return isUsing(POSTGRESQL);
    }

    public boolean isUsingSQLServer() {
        return isUsing(SQLSERVER);
    }

    public void setStopOnOrgClosureStartupFailure(boolean stopOnOrgClosureStartupFailure) {
        this.stopOnOrgClosureStartupFailure = stopOnOrgClosureStartupFailure;
    }

    public boolean isStopOnOrgClosureStartupFailure() {
        return stopOnOrgClosureStartupFailure;
    }

    public boolean isSkipOrgClosureStructureCheck() {
        return skipOrgClosureStructureCheck;
    }

    public void setSkipOrgClosureStructureCheck(boolean skipOrgClosureStructureCheck) {
        this.skipOrgClosureStructureCheck = skipOrgClosureStructureCheck;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }
}
