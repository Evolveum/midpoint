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
import com.evolveum.midpoint.repo.sql.util.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.h2.Driver;
import org.hibernate.dialect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * This class is used for SQL repository configuration. It reads values from Apache configuration object (xml).
 *
 * @author lazyman
 */
public class SqlRepositoryConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryConfiguration.class);

	public enum Database {

    	// we might include other dialects if needed (but the ones listed here are the recommended ones)
        H2(DRIVER_H2, H2Dialect.class.getName()),
		MYSQL(DRIVER_MYSQL, MidPointMySQLDialect.class.getName()),
		POSTGRESQL(DRIVER_POSTGRESQL, MidPointPostgreSQLDialect.class.getName()),
		SQLSERVER(DRIVER_SQLSERVER, UnicodeSQLServer2008Dialect.class.getName()),
		ORACLE(DRIVER_ORACLE, MidPointOracleDialect.class.getName()),
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

		@Nullable
	    public static Database findByDriverClassName(String driverClassName) {
			if (driverClassName != null) {
				return Arrays.stream(values())
						.filter(db -> db.containsDriver(driverClassName))
						.findFirst().orElse(null);
			} else {
				return null;
			}
	    }

	    public static Database findByHibernateDialect(String hibernateDialect) {
			if (hibernateDialect != null) {
				return Arrays.stream(values())
						.filter(db -> db.containsDialect(hibernateDialect))
						.findFirst().orElse(null);
			} else {
				return null;
			}
	    }
    }

	private static final String DEFAULT_FILE_NAME = "midpoint";
	private static final String DEFAULT_EMBEDDED_H2_JDBC_USERNAME = "sa";
	private static final String DEFAULT_EMBEDDED_H2_JDBC_PASSWORD = "";
	private static final int DEFAULT_EMBEDDED_H2_PORT = 5437;
	private static final int DEFAULT_MIN_POOL_SIZE = 8;
	private static final int DEFAULT_MAX_POOL_SIZE = 20;

	private static final String USER_HOME_VARIABLE = "user.home";
	private static final String MIDPOINT_HOME_VARIABLE = "midpoint.home";

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

    /*
     * Most of the properties below is final to make the code clean and readable.
     * Exceptions (mainly due to testing facilitation) are marked.
     */

	/**
	 * Database kind - either explicitly configured or derived from other options (driver name, hibernate dialect, embedded).
	 * May be null if couldn't be derived in any reasonable way.
	 */
	private final Database database;

    //embedded configuration
    private final boolean embedded;
    private final boolean asServer;
    private final String baseDir;
    private final String fileName;
    private final boolean tcpSSL;
    private final int port;
    private final boolean dropIfExists;
    //connection for hibernate
    private final String driverClassName;
    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final String hibernateDialect;
    private String hibernateHbm2ddl;                            // not final only because of testing
    private final String dataSource;
    private final int minPoolSize;
    private final int maxPoolSize;
    private final boolean useZip;

    private TransactionIsolation defaultTransactionIsolation;
    private boolean defaultLockForUpdateViaHibernate;
    private boolean defaultLockForUpdateViaSql;
    private boolean defaultUseReadOnlyTransactions;

    private final TransactionIsolation transactionIsolation;
    private final boolean lockForUpdateViaHibernate;
    private final boolean lockForUpdateViaSql;
    private final boolean useReadOnlyTransactions;

    private final String performanceStatisticsFile;
    private final int performanceStatisticsLevel;

    private boolean defaultIterativeSearchByPaging;
    private int defaultIterativeSearchByPagingBatchSize;

    private final boolean iterativeSearchByPaging;
    private int iterativeSearchByPagingBatchSize;               // not final only because of testing

    private final boolean ignoreOrgClosure;
    private final OrgClosureManager.StartupAction orgClosureStartupAction;
    private final boolean skipOrgClosureStructureCheck;
    private final boolean stopOnOrgClosureStartupFailure;

	/*
	 * Notes:
	 * - In testing mode, the configuration is already updated from .properties file.
	 * - Many options have database-specific defaults. The kind of database is derived from these options (in this order):
	 *    1. database
	 *    2. driverClassName
	 *    3. hibernateDialect
	 *    4. embedded (if true, H2 is used)
	 */
    public SqlRepositoryConfiguration(Configuration configuration) {
    	dataSource = MiscUtil.nullIfEmpty(configuration.getString(PROPERTY_DATASOURCE));

    	// guessing the database + setting related basic properties

        Database configuredDatabase = Database.findDatabase(configuration.getString(PROPERTY_DATABASE));
        String configuredDriverClassName = configuration.getString(PROPERTY_DRIVER_CLASS_NAME);
        String configuredHibernateDialect = configuration.getString(PROPERTY_HIBERNATE_DIALECT);
        Boolean configuredEmbedded = configuration.getBoolean(PROPERTY_EMBEDDED, null);

        if (configuredDatabase != null) {
        	database = configuredDatabase;
        } else {
	        Database guessedDatabase = null;
	        if (configuredDriverClassName != null) {
		        guessedDatabase = Database
				        .findByDriverClassName(configuredDriverClassName);     // may be still null for unknown drivers
	        }
	        if (guessedDatabase == null && configuredHibernateDialect != null) {
		        guessedDatabase = Database
				        .findByHibernateDialect(configuredHibernateDialect);   // may be still null for unknown dialects
	        }
	        if (guessedDatabase == null && Boolean.TRUE.equals(configuredEmbedded)) {
		        guessedDatabase = H2;
	        }
	        if (guessedDatabase == null && dataSource == null && configuredDriverClassName == null
			        && configuredHibernateDialect == null && configuredEmbedded == null) {
		        guessedDatabase = H2;
	        }
	        database = guessedDatabase;
        }
        driverClassName = defaultIfNull(configuredDriverClassName, getDefaultDriverClassName(dataSource, database));
	    hibernateDialect = defaultIfNull(configuredHibernateDialect, getDefaultHibernateDialect(database));
	    embedded = defaultIfNull(configuredEmbedded, getDefaultEmbedded(dataSource, database));

	    // other properties

	    asServer = configuration.getBoolean(PROPERTY_AS_SERVER, embedded);
	    String baseDirOption = configuration.getString(PROPERTY_BASE_DIR);
	    baseDir = baseDirOption != null ? baseDirOption : getDerivedBaseDir();      // there's logging there so we call it only if necessary
	    fileName = configuration.getString(PROPERTY_FILE_NAME, DEFAULT_FILE_NAME);

        hibernateHbm2ddl = configuration.getString(PROPERTY_HIBERNATE_HBM2DDL, getDefaultHibernateHbm2ddl(database));
        jdbcUsername = configuration.getString(PROPERTY_JDBC_USERNAME, embedded ? DEFAULT_EMBEDDED_H2_JDBC_USERNAME : null);
	    jdbcPassword = configuration.getString(PROPERTY_JDBC_PASSWORD, embedded ? DEFAULT_EMBEDDED_H2_JDBC_PASSWORD : null);
        port = configuration.getInt(PROPERTY_PORT, DEFAULT_EMBEDDED_H2_PORT);
        tcpSSL = configuration.getBoolean(PROPERTY_TCP_SSL, false);
        dropIfExists = configuration.getBoolean(PROPERTY_DROP_IF_EXISTS, false);
        minPoolSize = configuration.getInt(PROPERTY_MIN_POOL_SIZE, DEFAULT_MIN_POOL_SIZE);
        maxPoolSize = configuration.getInt(PROPERTY_MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);
        useZip = configuration.getBoolean(PROPERTY_USE_ZIP, false);

        // requires asServer, baseDir, fileName, port
	    jdbcUrl = configuration.getString(PROPERTY_JDBC_URL, embedded ? getDefaultEmbeddedJdbcUrl() : null);

	    computeDefaultConcurrencyParameters();
	    transactionIsolation = TransactionIsolation.fromValue(
	    		configuration.getString(PROPERTY_TRANSACTION_ISOLATION, defaultTransactionIsolation.value()));
		applyTransactionIsolation();

        lockForUpdateViaHibernate = configuration.getBoolean(PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE, defaultLockForUpdateViaHibernate);
        lockForUpdateViaSql = configuration.getBoolean(PROPERTY_LOCK_FOR_UPDATE_VIA_SQL, defaultLockForUpdateViaSql);
        useReadOnlyTransactions = configuration.getBoolean(PROPERTY_USE_READ_ONLY_TRANSACTIONS, defaultUseReadOnlyTransactions);

        performanceStatisticsFile = configuration.getString(PROPERTY_PERFORMANCE_STATISTICS_FILE);
        performanceStatisticsLevel = configuration.getInt(PROPERTY_PERFORMANCE_STATISTICS_LEVEL, 0);

        computeDefaultIterativeSearchParameters();
        iterativeSearchByPaging = configuration.getBoolean(PROPERTY_ITERATIVE_SEARCH_BY_PAGING, defaultIterativeSearchByPaging);
        iterativeSearchByPagingBatchSize = configuration.getInt(PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE, defaultIterativeSearchByPagingBatchSize);

        ignoreOrgClosure = configuration.getBoolean(PROPERTY_IGNORE_ORG_CLOSURE, false);
        orgClosureStartupAction = OrgClosureManager.StartupAction.fromValue(
        		configuration.getString(PROPERTY_ORG_CLOSURE_STARTUP_ACTION,
				        OrgClosureManager.StartupAction.REBUILD_IF_NEEDED.toString()));
        skipOrgClosureStructureCheck = configuration.getBoolean(PROPERTY_SKIP_ORG_CLOSURE_STRUCTURE_CHECK, false);
        stopOnOrgClosureStartupFailure = configuration.getBoolean(PROPERTY_STOP_ON_ORG_CLOSURE_STARTUP_FAILURE, true);
    }

	private String getDefaultEmbeddedJdbcUrl() {
		return getDefaultEmbeddedJdbcUrlPrefix()
				+ ";MVCC=FALSE"                // Turn off MVCC, revert to table locking.
				+ ";DB_CLOSE_ON_EXIT=FALSE"    // Disable database closing on exit. By default, a database is closed when the last connection is closed.
				+ ";LOCK_MODE=1"               // Both read locks and write locks are kept until the transaction commits.
				+ ";LOCK_TIMEOUT=100"          // This is experimental setting - let's resolve locking conflicts by midPoint itself
				+ ";MAX_LENGTH_INPLACE_LOB=10240"; // We want to store blob datas i.e. full xml object right in table (it's often only a few kb)
	}

	private String getDerivedBaseDir() {
		LOGGER.debug("Base dir path in configuration was not defined.");
		String rv;
		if (StringUtils.isNotEmpty(System.getProperty(MIDPOINT_HOME_VARIABLE))) {
			rv = System.getProperty(MIDPOINT_HOME_VARIABLE);
			LOGGER.info("Using {} with value {} as base dir for configuration.", MIDPOINT_HOME_VARIABLE, rv);
		} else if (StringUtils.isNotEmpty(System.getProperty(USER_HOME_VARIABLE))) {
			rv = System.getProperty(USER_HOME_VARIABLE);
			LOGGER.info("Using {} with value {} as base dir for configuration.", USER_HOME_VARIABLE, rv);
		} else {
			rv = ".";
			LOGGER.info("Using '.' as base dir for configuration (neither {} nor {} was defined).", MIDPOINT_HOME_VARIABLE, USER_HOME_VARIABLE);
		}
		return rv;
	}

	/**
	 * Prepares a prefix (first part) of JDBC URL for embedded database. Used also by configurator of tasks (quartz)
	 * and workflow (activiti) modules; they add their own db names and parameters to this string.
	 *
	 * @return prefix of JDBC URL like jdbc:h2:file:d:\midpoint\midpoint
	 */
	public String getDefaultEmbeddedJdbcUrlPrefix() {
		File baseDirFile = new File(baseDir);
		if (!baseDirFile.exists() || !baseDirFile.isDirectory()) {
			throw new SystemException("File '" + baseDir + "' defined as baseDir doesn't exist or is not a directory.");
		}
		StringBuilder jdbcUrl = new StringBuilder("jdbc:h2:");
		if (asServer) {
			//jdbc:h2:tcp://<server>[:<port>]/[<path>]<databaseName>
			jdbcUrl.append("tcp://127.0.0.1:");
			jdbcUrl.append(port);
			jdbcUrl.append("/");
			jdbcUrl.append(fileName);
		} else {
			//jdbc:h2:[file:][<path>]<databaseName>
			jdbcUrl.append("file:");

			File databaseFile = new File(baseDir, fileName);
			jdbcUrl.append(databaseFile.getAbsolutePath());
		}
		return jdbcUrl.toString();
	}


	// The methods below are static to highlight their data dependencies and to avoid using properties
	// that were not yet initialized.
	private static String getDefaultDriverClassName(String dataSource, Database database) {
    	if (dataSource != null) {
    	    if (database != null) {
    	        return database.getDefaultDriverClassName();
            }
    		return null;                // driver is not needed here
	    } else if (database != null) {
			return database.getDefaultDriverClassName();
		} else {
			return null;
		}
	}

	private static String getDefaultHibernateDialect(Database database) {
		if (database != null) {
			return database.getDefaultHibernateDialect();
		} else {
			return null;
		}
	}

	private static Boolean getDefaultEmbedded(String dataSource, Database database) {
		// Embedded means we want to start the database ourselves i.e. from midPoint.
		// This option is obviously supported only for H2; and for H2, it is the default.
		// Note that when using dataSource, we assume the database was started elsewhere.
		// (However, it can be hardly expected anyone would use H2 with the data source,
		// except for DataSourceTest.)
    	return dataSource == null && database == H2;
	}

	private static String getDefaultHibernateHbm2ddl(Database database) {
		return database == H2 ? "update" : "validate";
	}

    private void computeDefaultConcurrencyParameters() {
        if (isUsingH2()) {
            defaultTransactionIsolation = TransactionIsolation.SERIALIZABLE;
            defaultLockForUpdateViaHibernate = false;
            defaultLockForUpdateViaSql = false;
            defaultUseReadOnlyTransactions = false;        // h2 does not support "SET TRANSACTION READ ONLY" command
        } else if (isUsingMySqlCompatible()) {
	        defaultTransactionIsolation = TransactionIsolation.SERIALIZABLE;
	        defaultLockForUpdateViaHibernate = false;
	        defaultLockForUpdateViaSql = true;
	        defaultUseReadOnlyTransactions = true;
        } else if (isUsingOracle()) {
	        defaultTransactionIsolation = TransactionIsolation.SERIALIZABLE;
	        defaultLockForUpdateViaHibernate = false;
	        defaultLockForUpdateViaSql = true;
	        defaultUseReadOnlyTransactions = true;
        } else if (isUsingPostgreSQL()) {
	        defaultTransactionIsolation = TransactionIsolation.SERIALIZABLE;
	        defaultLockForUpdateViaHibernate = false;
	        defaultLockForUpdateViaSql = false;
	        defaultUseReadOnlyTransactions = true;
        } else if (isUsingSQLServer()) {
	        defaultTransactionIsolation = TransactionIsolation.SNAPSHOT;
	        defaultLockForUpdateViaHibernate = false;
	        defaultLockForUpdateViaSql = false;
	        defaultUseReadOnlyTransactions = false;
        } else {
	        defaultTransactionIsolation = TransactionIsolation.SERIALIZABLE;
	        defaultLockForUpdateViaHibernate = false;
	        defaultLockForUpdateViaSql = false;
	        defaultUseReadOnlyTransactions = true;
	        //noinspection ConstantConditions
	        LOGGER.warn("Fine-tuned concurrency parameters defaults for hibernate dialect " + hibernateDialect
                    + " not found; using the following defaults: transactionIsolation = " + defaultTransactionIsolation
                    + ", lockForUpdateViaHibernate = " + defaultLockForUpdateViaHibernate
                    + ", lockForUpdateViaSql = " + defaultLockForUpdateViaSql
                    + ", useReadOnlyTransactions = " + defaultUseReadOnlyTransactions
                    + ". Please override them if necessary.");
        }
    }

    private void computeDefaultIterativeSearchParameters() {
        if (isUsingH2()) {
            defaultIterativeSearchByPaging = true;
            defaultIterativeSearchByPagingBatchSize = 50;
        } else if (isUsingMySqlCompatible()) {
	        defaultIterativeSearchByPaging = true;
	        defaultIterativeSearchByPagingBatchSize = 50;
        } else {
	        defaultIterativeSearchByPaging = false;
        }
    }

    /**
     * Configuration validation.
     *
     * @throws RepositoryServiceFactoryException if configuration is invalid.
     */
    public void validate() throws RepositoryServiceFactoryException {
        if (dataSource == null) {
            notEmpty(jdbcUrl, "JDBC Url is empty or not defined.");
            notEmpty(jdbcUsername, "JDBC user name is empty or not defined.");
            notNull(jdbcPassword, "JDBC password is not defined.");
            notEmpty(driverClassName, "Driver class name is empty or not defined.");
        }

        notEmpty(hibernateDialect, "Hibernate dialect is empty or not defined.");
        notEmpty(hibernateHbm2ddl, "Hibernate hbm2ddl option is empty or not defined.");

        if (embedded) {
            notEmpty(baseDir, "Base dir is empty or not defined.");
            if (asServer) {
                if (port < 0 || port > 65535) {
                    throw new RepositoryServiceFactoryException("Port must be in interval (0-65534)");
                }
            }
        }

        if (minPoolSize <= 0) {
            throw new RepositoryServiceFactoryException("Min. pool size must be greater than zero.");
        }

        if (maxPoolSize <= 0) {
            throw new RepositoryServiceFactoryException("Max. pool size must be greater than zero.");
        }

        if (minPoolSize > maxPoolSize) {
            throw new RepositoryServiceFactoryException("Max. pool size must be greater than min. pool size.");
        }
    }

    @SuppressWarnings("SameParameterValue")
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

    public String getBaseDir() {
        return baseDir;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public boolean isEmbedded() {
        return embedded;
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

    public String getHibernateHbm2ddl() {
        return hibernateHbm2ddl;
    }

    // use only for testing
    public void setHibernateHbm2ddl(String hibernateHbm2ddl) {
        this.hibernateHbm2ddl = hibernateHbm2ddl;
    }

    /**
     * @return Password for JDBC connection. (Optional)
     */
    public String getJdbcPassword() {
        return jdbcPassword;
    }

    /**
     * @return JDBC URL connection string for hibernate data source. (for embedded mode it's created automatically).
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * @return Username for JDBC connection. (Optional)
     */
    public String getJdbcUsername() {
        return jdbcUsername;
    }

    /**
     * @return Port number if repository is running in embedded server mode. Default is 5437.
     */
    public int getPort() {
        return port;
    }

    /**
     * Value represents repository running in embedded server mode with SSL turned on/off. Default value is false.
     *
     * @return Returns true if repository is running in embedded server mode and SSL turned on.
     */
    public boolean isTcpSSL() {
        return tcpSSL;
    }

    /**
     * Used in embedded mode to define h2 database file name. Default will be "midpoint".
     *
     * @return name of DB file
     */
    public String getFileName() {
        return fileName;
    }

    public boolean isDropIfExists() {
        return dropIfExists;
    }

    public TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    private void applyTransactionIsolation() {
        // ugly hack, but I know of no way to work around
//        MidPointConnectionCustomizer.setTransactionIsolation(transactionIsolation);
    }

    public boolean isLockForUpdateViaHibernate() {
        return lockForUpdateViaHibernate;
    }

    public boolean isLockForUpdateViaSql() {
        return lockForUpdateViaSql;
    }

    public boolean isUseReadOnlyTransactions() {
        return useReadOnlyTransactions;
    }

    public String getPerformanceStatisticsFile() {
        return performanceStatisticsFile;
    }

    public int getPerformanceStatisticsLevel() {
        return performanceStatisticsLevel;
    }

    public boolean isIterativeSearchByPaging() {
        return iterativeSearchByPaging;
    }

    public int getIterativeSearchByPagingBatchSize() {
        return iterativeSearchByPagingBatchSize;
    }

    // exists because of testing
    public void setIterativeSearchByPagingBatchSize(int iterativeSearchByPagingBatchSize) {
        this.iterativeSearchByPagingBatchSize = iterativeSearchByPagingBatchSize;
    }

    public String getDataSource() {
        return dataSource;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public boolean isUseZip() {
        return useZip;
    }

    public boolean isIgnoreOrgClosure() {
        return ignoreOrgClosure;
    }

    public OrgClosureManager.StartupAction getOrgClosureStartupAction() {
        return orgClosureStartupAction;
    }

    public boolean isUsingH2() {
        return isUsing(H2);
    }

	private boolean isUsing(Database db) {
		// Originally we checked here also the driver/dialect; but this is no longer necessary as the database
		// guesswork is done at initialization time.
		return database == db;
	}

	public boolean isUsingOracle() {
        return isUsing(ORACLE);
    }

    public boolean isUsingMySqlCompatible() {
        return isUsing(MYSQL) || isUsing(MARIADB);
    }

    @SuppressWarnings("unused")
    public boolean isUsingMySql() {
        return isUsing(MYSQL);
    }

    @SuppressWarnings("unused")
    public boolean isUsingMariaDB() {
        return isUsing(MARIADB);
    }

    public boolean isUsingPostgreSQL() {
        return isUsing(POSTGRESQL);
    }

    public boolean isUsingSQLServer() {
        return isUsing(SQLSERVER);
    }

    public boolean isStopOnOrgClosureStartupFailure() {
        return stopOnOrgClosureStartupFailure;
    }

    public boolean isSkipOrgClosureStructureCheck() {
        return skipOrgClosureStructureCheck;
    }

    public Database getDatabase() {
        return database;
    }
}
