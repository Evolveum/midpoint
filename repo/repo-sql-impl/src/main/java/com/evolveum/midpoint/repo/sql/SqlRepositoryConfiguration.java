/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.Database.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.h2.Driver;
import org.hibernate.dialect.H2Dialect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.helpers.OrgClosureManager;
import com.evolveum.midpoint.repo.sql.perf.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.repo.sql.util.MidPointMySQLDialect;
import com.evolveum.midpoint.repo.sql.util.MidPointOracleDialect;
import com.evolveum.midpoint.repo.sql.util.MidPointPostgreSQLDialect;
import com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This class is used for SQL repository configuration. It reads values from Apache configuration object (xml).
 *
 * @author lazyman
 */
public class SqlRepositoryConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryConfiguration.class);

    private static final String HBM2DDL_CREATE_DROP = "create-drop";
    private static final String HBM2DDL_CREATE = "create";
    private static final String HBM2DDL_UPDATE = "update";
    private static final String HBM2DDL_VALIDATE = "validate";
    private static final String HBM2DDL_NONE = "none";

    public enum Database {

        // order is important! (the first value is the default)
        H2(DRIVER_H2,
                H2Dialect.class.getName()),
        MYSQL(DRIVER_MYSQL,
                MidPointMySQLDialect.class.getName(),
                org.hibernate.dialect.MySQLDialect.class.getName(),
                org.hibernate.dialect.MySQLInnoDBDialect.class.getName()),
        POSTGRESQL(DRIVER_POSTGRESQL,
                MidPointPostgreSQLDialect.class.getName(),
                org.hibernate.dialect.PostgreSQLDialect.class.getName(),
                org.hibernate.dialect.PostgresPlusDialect.class.getName()),
        SQLSERVER(DRIVER_SQLSERVER,
                UnicodeSQLServer2008Dialect.class.getName(),
                org.hibernate.dialect.SQLServerDialect.class.getName()),
        ORACLE(DRIVER_ORACLE,
                MidPointOracleDialect.class.getName(),
                org.hibernate.dialect.OracleDialect.class.getName(),
                org.hibernate.dialect.Oracle9Dialect.class.getName(),
                org.hibernate.dialect.Oracle8iDialect.class.getName(),
                org.hibernate.dialect.Oracle9iDialect.class.getName(),
                org.hibernate.dialect.Oracle10gDialect.class.getName()),
        MARIADB(DRIVER_MARIADB,
                MidPointMySQLDialect.class.getName());

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

    /**
     * What to do if the DB schema is missing.
     */
    public enum MissingSchemaAction {
        /**
         * The problem is reported and midPoint startup is cancelled. This is the default.
         */
        STOP("stop"),
        /**
         * The problem is reported but startup continues. Not recommended.
         */
        WARN("warn"),
        /**
         * MidPoint will attempt to create the schema using standard DB scripts. Then it will validate the schema again.
         */
        CREATE("create");

        private final String value;

        MissingSchemaAction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MissingSchemaAction fromValue(String text) {
            if (StringUtils.isEmpty(text)) {
                return null;
            }
            for (MissingSchemaAction a : values()) {
                if (text.equals(a.value)) {
                    return a;
                }
            }
            throw new IllegalArgumentException("Unknown MissingSchemaAction: " + text);
        }
    }

    /**
     * What to do if the DB schema is outdated and is upgradeable (either automatically or manually).
     */
    public enum UpgradeableSchemaAction {
        /**
         * The problem is reported and midPoint startup is cancelled. This is the default.
         */
        STOP("stop"),
        /**
         * The problem is reported. Not recommended.
         */
        WARN("warn"),
        /**
         * An automatic upgrade is attempted, if possible. (If not possible, the startup is cancelled.) NOT SUPPORTED YET.
         */
        UPGRADE("upgrade");

        private final String value;

        UpgradeableSchemaAction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static UpgradeableSchemaAction fromValue(String text) {
            if (StringUtils.isEmpty(text)) {
                return null;
            }
            for (UpgradeableSchemaAction a : values()) {
                if (text.equals(a.value)) {
                    return a;
                }
            }
            throw new IllegalArgumentException("Unknown UpgradeableSchemaAction: " + text);
        }
    }

    /**
     * What to do if the DB schema is incompatible (e.g. newer schema with older midPoint).
     */
    public enum IncompatibleSchemaAction {
        /**
         * The problem is reported and midPoint startup is cancelled. This is the default.
         */
        STOP("stop"),
        /**
         * The problem is reported. Not recommended.
         */
        WARN("warn");

        private final String value;

        IncompatibleSchemaAction(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static IncompatibleSchemaAction fromValue(String text) {
            if (StringUtils.isEmpty(text)) {
                return null;
            }
            for (IncompatibleSchemaAction a : values()) {
                if (text.equals(a.value)) {
                    return a;
                }
            }
            throw new IllegalArgumentException("Unknown IncompatibleSchemaAction: " + text);
        }
    }

    private static final String DEFAULT_FILE_NAME = "midpoint";
    private static final String DEFAULT_EMBEDDED_H2_JDBC_USERNAME = "sa";
    private static final String DEFAULT_EMBEDDED_H2_JDBC_PASSWORD = "";
    private static final int DEFAULT_EMBEDDED_H2_PORT = 5437;
    private static final int DEFAULT_MIN_POOL_SIZE = 8;
    private static final int DEFAULT_MAX_POOL_SIZE = 20;
    private static final int DEFAULT_MAX_OBJECTS_FOR_IMPLICIT_FETCH_ALL_ITERATION_METHOD = 500;

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
    public static final String PROPERTY_JDBC_PASSWORD_FILE = "jdbcPasswordFile";
    public static final String PROPERTY_JDBC_USERNAME = "jdbcUsername";
    public static final String PROPERTY_JDBC_URL = "jdbcUrl";
    public static final String PROPERTY_DATASOURCE = "dataSource";
    public static final String PROPERTY_USE_ZIP = "useZip";

    /**
     * Specifies language used for writing fullObject attribute.
     * See LANG constants in {@link com.evolveum.midpoint.prism.PrismContext} for supported values.
     */
    public static final String PROPERTY_FULL_OBJECT_FORMAT = "fullObjectFormat";
    public static final String PROPERTY_MIN_POOL_SIZE = "minPoolSize";
    public static final String PROPERTY_MAX_POOL_SIZE = "maxPoolSize";
    public static final String PROPERTY_MAX_LIFETIME = "maxLifetime";
    public static final String PROPERTY_IDLE_TIMEOUT = "idleTimeout";

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
    public static final String PROPERTY_MAX_OBJECTS_FOR_IMPLICIT_FETCH_ALL_ITERATION_METHOD = "maxObjectsForImplicitFetchAllIterationMethod";

    //closure
    public static final String PROPERTY_IGNORE_ORG_CLOSURE = "ignoreOrgClosure";
    public static final String PROPERTY_ORG_CLOSURE_STARTUP_ACTION = "orgClosureStartupAction";
    public static final String PROPERTY_SKIP_ORG_CLOSURE_STRUCTURE_CHECK = "skipOrgClosureStructureCheck";
    public static final String PROPERTY_STOP_ON_ORG_CLOSURE_STARTUP_FAILURE = "stopOnOrgClosureStartupFailure";

    public static final String PROPERTY_SKIP_EXPLICIT_SCHEMA_VALIDATION = "skipExplicitSchemaValidation";
    public static final String PROPERTY_MISSING_SCHEMA_ACTION = "missingSchemaAction";
    public static final String PROPERTY_UPGRADEABLE_SCHEMA_ACTION = "upgradeableSchemaAction";
    public static final String PROPERTY_INCOMPATIBLE_SCHEMA_ACTION = "incompatibleSchemaAction";
    public static final String PROPERTY_SCHEMA_VERSION_IF_MISSING = "schemaVersionIfMissing";
    public static final String PROPERTY_SCHEMA_VERSION_OVERRIDE = "schemaVersionOverride";
    public static final String PROPERTY_SCHEMA_VARIANT = "schemaVariant";

    public static final String PROPERTY_INITIALIZATION_FAIL_TIMEOUT = "initializationFailTimeout";

    public static final String PROPERTY_ENABLE_NO_FETCH_EXTENSION_VALUES_INSERTION = "enableNoFetchExtensionValuesInsertion";
    public static final String PROPERTY_ENABLE_NO_FETCH_EXTENSION_VALUES_DELETION = "enableNoFetchExtensionValuesDeletion";
    public static final String PROPERTY_ENABLE_INDEX_ONLY_ITEMS = "enableIndexOnlyItems";

    public static final String PROPERTY_TEXT_INFO_COLUMN_SIZE = "textInfoColumnSize";

    private static final String DRIVER_H2 = Driver.class.getName();
    private static final String DRIVER_MYSQL = "com.mysql.cj.jdbc.Driver";
    private static final String DRIVER_MARIADB = "org.mariadb.jdbc.Driver";
    private static final String DRIVER_SQLSERVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String DRIVER_POSTGRESQL = "org.postgresql.Driver";
    private static final String DRIVER_ORACLE = "oracle.jdbc.OracleDriver";

    private static final String UTF8MB4 = "utf8mb4";

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
    private final Long maxLifetime;
    private final Long idleTimeout;
    private final boolean useZip;
    private String fullObjectFormat; // non-final for testing

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
    private final int maxObjectsForImplicitFetchAllIterationMethod;

    private final boolean iterativeSearchByPaging;
    private int iterativeSearchByPagingBatchSize;               // not final only because of testing

    private final boolean ignoreOrgClosure;
    private final OrgClosureManager.StartupAction orgClosureStartupAction;
    private final boolean skipOrgClosureStructureCheck;
    private final boolean stopOnOrgClosureStartupFailure;

    private final long initializationFailTimeout;

    private boolean skipExplicitSchemaValidation;
    @NotNull private final MissingSchemaAction missingSchemaAction;
    @NotNull private final UpgradeableSchemaAction upgradeableSchemaAction;
    @NotNull private final IncompatibleSchemaAction incompatibleSchemaAction;
    private String schemaVersionIfMissing;
    private String schemaVersionOverride;
    private String schemaVariant;           // e.g. "utf8mb4" for MySQL/MariaDB

    private boolean enableNoFetchExtensionValuesInsertion;
    private boolean enableNoFetchExtensionValuesDeletion;
    private boolean enableIndexOnlyItems;

    private int textInfoColumnSize;

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

        String jdbcPasswordFile = configuration.getString(PROPERTY_JDBC_PASSWORD_FILE);
        if (jdbcPasswordFile != null) {
            try {
                jdbcPassword = readFile(jdbcPasswordFile);
            } catch (IOException e) {
                throw new SystemException("Couldn't read JDBC password from specified file '" + jdbcPasswordFile + "': " + e.getMessage(), e);
            }
        } else {
            jdbcPassword = configuration.getString(PROPERTY_JDBC_PASSWORD, embedded ? DEFAULT_EMBEDDED_H2_JDBC_PASSWORD : null);
        }
        port = configuration.getInt(PROPERTY_PORT, DEFAULT_EMBEDDED_H2_PORT);
        tcpSSL = configuration.getBoolean(PROPERTY_TCP_SSL, false);
        dropIfExists = configuration.getBoolean(PROPERTY_DROP_IF_EXISTS, false);
        minPoolSize = configuration.getInt(PROPERTY_MIN_POOL_SIZE, DEFAULT_MIN_POOL_SIZE);
        maxPoolSize = configuration.getInt(PROPERTY_MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);
        maxLifetime = configuration.getLong(PROPERTY_MAX_LIFETIME, null);
        idleTimeout = configuration.getLong(PROPERTY_IDLE_TIMEOUT, null);

        useZip = configuration.getBoolean(PROPERTY_USE_ZIP, false);
        fullObjectFormat = configuration.getString(
                PROPERTY_FULL_OBJECT_FORMAT,
                System.getProperty(PROPERTY_FULL_OBJECT_FORMAT, PrismContext.LANG_XML));

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
        performanceStatisticsLevel = configuration.getInt(PROPERTY_PERFORMANCE_STATISTICS_LEVEL, SqlPerformanceMonitorImpl.LEVEL_LOCAL_STATISTICS);

        computeDefaultIterativeSearchParameters();
        iterativeSearchByPaging = configuration.getBoolean(PROPERTY_ITERATIVE_SEARCH_BY_PAGING, defaultIterativeSearchByPaging);
        iterativeSearchByPagingBatchSize = configuration.getInt(PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE, defaultIterativeSearchByPagingBatchSize);
        maxObjectsForImplicitFetchAllIterationMethod = configuration.getInt(PROPERTY_MAX_OBJECTS_FOR_IMPLICIT_FETCH_ALL_ITERATION_METHOD,
                DEFAULT_MAX_OBJECTS_FOR_IMPLICIT_FETCH_ALL_ITERATION_METHOD);

        ignoreOrgClosure = configuration.getBoolean(PROPERTY_IGNORE_ORG_CLOSURE, false);
        orgClosureStartupAction = OrgClosureManager.StartupAction.fromValue(
                configuration.getString(PROPERTY_ORG_CLOSURE_STARTUP_ACTION,
                        OrgClosureManager.StartupAction.REBUILD_IF_NEEDED.toString()));
        skipOrgClosureStructureCheck = configuration.getBoolean(PROPERTY_SKIP_ORG_CLOSURE_STRUCTURE_CHECK, false);
        stopOnOrgClosureStartupFailure = configuration.getBoolean(PROPERTY_STOP_ON_ORG_CLOSURE_STARTUP_FAILURE, true);

        skipExplicitSchemaValidation = configuration.getBoolean(PROPERTY_SKIP_EXPLICIT_SCHEMA_VALIDATION,
                isAutoUpdate(hibernateHbm2ddl) || isValidate(hibernateHbm2ddl));

        missingSchemaAction = defaultIfNull(MissingSchemaAction.fromValue(configuration.getString(PROPERTY_MISSING_SCHEMA_ACTION)),
                MissingSchemaAction.STOP);
        upgradeableSchemaAction = defaultIfNull(UpgradeableSchemaAction.fromValue(
                configuration.getString(PROPERTY_UPGRADEABLE_SCHEMA_ACTION)), UpgradeableSchemaAction.STOP);
        incompatibleSchemaAction = defaultIfNull(IncompatibleSchemaAction
                .fromValue(configuration.getString(PROPERTY_INCOMPATIBLE_SCHEMA_ACTION)), IncompatibleSchemaAction.STOP);

        schemaVersionIfMissing = configuration.getString(PROPERTY_SCHEMA_VERSION_IF_MISSING);
        schemaVersionOverride = configuration.getString(PROPERTY_SCHEMA_VERSION_OVERRIDE);
        schemaVariant = configuration.getString(PROPERTY_SCHEMA_VARIANT);

        initializationFailTimeout = configuration.getLong(PROPERTY_INITIALIZATION_FAIL_TIMEOUT, 1L);

        enableNoFetchExtensionValuesInsertion = configuration.getBoolean(PROPERTY_ENABLE_NO_FETCH_EXTENSION_VALUES_INSERTION, true);
        enableNoFetchExtensionValuesDeletion = configuration.getBoolean(PROPERTY_ENABLE_NO_FETCH_EXTENSION_VALUES_DELETION, false);
        enableIndexOnlyItems = configuration.getBoolean(PROPERTY_ENABLE_INDEX_ONLY_ITEMS, false);

        int maxTextSize = (database == MYSQL || database == MARIADB) && UTF8MB4.equalsIgnoreCase(schemaVariant) ? 191 : 255;
        textInfoColumnSize = configuration.getInt(PROPERTY_TEXT_INFO_COLUMN_SIZE, maxTextSize);
    }

    private boolean isAutoUpdate(String hbm2ddl) {
        assert hbm2ddl != null;
        return HBM2DDL_UPDATE.equals(hbm2ddl) || HBM2DDL_CREATE.equals(hbm2ddl) || HBM2DDL_CREATE_DROP.equals(hbm2ddl);
    }

    private boolean isValidate(String hbm2ddl) {
        assert hbm2ddl != null;
        return HBM2DDL_VALIDATE.equals(hbm2ddl);
    }

    private String readFile(String filename) throws IOException {
        try (FileReader reader = new FileReader(filename)) {
            List<String> lines = IOUtils.readLines(reader);
            return String.join("\n", lines);
        }
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
        if (StringUtils.isNotEmpty(System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY))) {
            rv = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
            LOGGER.info("Using {} with value {} as base dir for configuration.", MidpointConfiguration.MIDPOINT_HOME_PROPERTY, rv);
        } else if (StringUtils.isNotEmpty(System.getProperty(MidpointConfiguration.USER_HOME_PROPERTY))) {
            rv = System.getProperty(MidpointConfiguration.USER_HOME_PROPERTY);
            LOGGER.info("Using {} with value {} as base dir for configuration.", MidpointConfiguration.USER_HOME_PROPERTY, rv);
        } else {
            rv = ".";
            LOGGER.info("Using '.' as base dir for configuration (neither {} nor {} was defined).",
                    MidpointConfiguration.MIDPOINT_HOME_PROPERTY, MidpointConfiguration.USER_HOME_PROPERTY);
        }
        return rv;
    }

    /**
     * Prepares a prefix (first part) of JDBC URL for embedded database. Used also by configurator of tasks (quartz)
     * module; it adds its own db names and parameters to this string.
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
        return database == H2 ? HBM2DDL_UPDATE : HBM2DDL_NONE;
    }

    private void computeDefaultConcurrencyParameters() {
        if (isUsingH2()) {
            defaultTransactionIsolation = TransactionIsolation.SERIALIZABLE;
            defaultLockForUpdateViaHibernate = false;
            defaultLockForUpdateViaSql = true;
            defaultUseReadOnlyTransactions = false;        // h2 does not support "SET TRANSACTION READ ONLY" command
        } else if (isUsingMySqlCompatible()) {
            defaultTransactionIsolation = TransactionIsolation.SERIALIZABLE;
            defaultLockForUpdateViaHibernate = false;
            defaultLockForUpdateViaSql = true;
            defaultUseReadOnlyTransactions = true;
        } else if (isUsingOracle()) {
            /*
             * Isolation of SERIALIZABLE causes false ORA-8177 (serialization) exceptions even for single-thread scenarios
             * since midPoint 3.8 and/or Oracle 12c (to be checked more precisely).
             *
             * READ_COMMITTED is currently a problem for MySQL and PostgreSQL because of org closure conflicts. However,
             * in case of Oracle (and SQL Server and H2) we explicitly lock the whole M_ORG_CLOSURE_TABLE during closure
             * updates. Therefore we can use READ_COMMITTED isolation for Oracle.
             *
             * (This is maybe the optimal solution also for other databases - to be researched later.)
             */
            defaultTransactionIsolation = TransactionIsolation.READ_COMMITTED;
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
        defaultIterativeSearchByPaging = true;
        defaultIterativeSearchByPagingBatchSize = 50;
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

    public int getMaxObjectsForImplicitFetchAllIterationMethod() {
        return maxObjectsForImplicitFetchAllIterationMethod;
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

    public Long getMaxLifetime() {
        return maxLifetime;
    }

    public Long getIdleTimeout() {
        return idleTimeout;
    }

    public boolean isUseZip() {
        return useZip;
    }

    /**
     * This is normally not used outside of tests, but should be safe to change any time.
     */
    public void setFullObjectFormat(String fullObjectFormat) {
        this.fullObjectFormat = fullObjectFormat;
    }

    /**
     * Returns serialization format (language) for writing fullObject.
     * Also see {@link #PROPERTY_FULL_OBJECT_FORMAT}.
     */
    public String getFullObjectFormat() {
        return fullObjectFormat;
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

    @NotNull
    public MissingSchemaAction getMissingSchemaAction() {
        return missingSchemaAction;
    }

    @NotNull
    public UpgradeableSchemaAction getUpgradeableSchemaAction() {
        return upgradeableSchemaAction;
    }

    @NotNull
    public IncompatibleSchemaAction getIncompatibleSchemaAction() {
        return incompatibleSchemaAction;
    }

    public boolean isSkipExplicitSchemaValidation() {
        return skipExplicitSchemaValidation;
    }

    public String getSchemaVersionIfMissing() {
        return schemaVersionIfMissing;
    }

    public String getSchemaVersionOverride() {
        return schemaVersionOverride;
    }

    public String getSchemaVariant() {
        return schemaVariant;
    }

    public long getInitializationFailTimeout() {
        return initializationFailTimeout;
    }

    public boolean isEnableNoFetchExtensionValuesInsertion() {
        return enableNoFetchExtensionValuesInsertion;
    }

    public boolean isEnableNoFetchExtensionValuesDeletion() {
        return enableNoFetchExtensionValuesDeletion;
    }

    public boolean isEnableIndexOnlyItems() {
        return enableIndexOnlyItems;
    }

    public int getTextInfoColumnSize() {
        return textInfoColumnSize;
    }

    // for testing only
    @SuppressWarnings("SameParameterValue")
    public void setEnableNoFetchExtensionValuesInsertion(boolean enableNoFetchExtensionValuesInsertion) {
        this.enableNoFetchExtensionValuesInsertion = enableNoFetchExtensionValuesInsertion;
    }

    // for testing only
    @SuppressWarnings("SameParameterValue")
    public void setEnableNoFetchExtensionValuesDeletion(boolean enableNoFetchExtensionValuesDeletion) {
        this.enableNoFetchExtensionValuesDeletion = enableNoFetchExtensionValuesDeletion;
    }

    // for testing only
    @SuppressWarnings("SameParameterValue")
    public void setEnableIndexOnlyItems(boolean enableIndexOnlyItems) {
        this.enableIndexOnlyItems = enableIndexOnlyItems;
    }
}
