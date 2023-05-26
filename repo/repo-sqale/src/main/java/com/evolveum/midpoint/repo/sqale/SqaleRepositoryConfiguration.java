/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.annotation.PostConstruct;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.repo.sqlbase.TransactionIsolation;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Common part of the SQL-based repository configuration.
 * Contains JDBC/datasource setup, connection pool configuration, but no framework/ORM stuff.
 */
public class SqaleRepositoryConfiguration implements JdbcRepositoryConfiguration {

    private static final String PROPERTY_SQL_DURATION_WARNING_MS = "sqlDurationWarningMs";

    private static final String DEFAULT_DRIVER = "org.postgresql.Driver";
    private static final SupportedDatabase DEFAULT_DATABASE = SupportedDatabase.POSTGRESQL;
    private static final String DEFAULT_JDBC_URL = "jdbc:postgresql://localhost:5432/midpoint";
    private static final String DEFAULT_FULL_OBJECT_FORMAT = PrismContext.LANG_JSON;

    /**
     * We need at least two connections, because ext item/URI cache can start nested transaction
     * using separate connection to add missing entry into the database.
     * For audit 1 is probably possible, but we just use the same floor for simplicity.
     */
    private static final int MIN_POOL_SIZE_FLOOR = 2;
    private static final int DEFAULT_MIN_POOL_SIZE = 8;

    /**
     * 40 is more than plentiful for any single-node setup.
     * With 2 nodes, this means 80 connections + separate pool for scheduler, which is 10 for each node.
     * That is already up to the limit of 100 default PG connections.
     * Nobody should go above 2 nodes without further tweaking.
     * Adding more connections to the database may not be the best course of action either, as it may
     * use more resources on the DB server and be slower than using less connections.
     * Total number of midPoint connections for the whole cluster should always be under PG connection limit.
     * If pool doesn't have a connection, the thread will wait a bit and work as a natural throttling.
     * If DB doesn't have another connection it reports error which will be much uglier.
     */
    private static final int DEFAULT_MAX_POOL_SIZE = 40;

    private static final int DEFAULT_ITERATIVE_SEARCH_PAGE_SIZE = 100;

    private static final int DEFAULT_SQL_DURATION_WARNING_MS = 0; // 0 or less means no warning

    @NotNull private final Configuration configuration;

    // either dataSource or JDBC URL must be set
    private String dataSource;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;

    private String driverClassName;

    private long initializationFailTimeout;
    private int minPoolSize;
    private int maxPoolSize;
    private Long maxLifetime;
    private Long idleTimeout;
    private Long keepaliveTime;
    private Long leakDetectionThreshold;

    private String fullObjectFormat;

    private String performanceStatisticsFile;
    private int performanceStatisticsLevel;

    private int iterativeSearchByPagingBatchSize;
    private boolean createMissingCustomColumns;

    private long sqlDurationWarningMs; // 0 or less means no warning

    // Provided with configuration node "midpoint.repository".
    public SqaleRepositoryConfiguration(@NotNull Configuration configuration) {
        this.configuration = configuration;
    }

    @PostConstruct
    public void init() throws RepositoryServiceFactoryException {
        dataSource = configuration.getString(PROPERTY_DATASOURCE);

        jdbcUrl = configuration.getString(PROPERTY_JDBC_URL, DEFAULT_JDBC_URL);
        jdbcUsername = configuration.getString(PROPERTY_JDBC_USERNAME, null);

        driverClassName = DEFAULT_DRIVER;

        String jdbcPasswordFile = configuration.getString(PROPERTY_JDBC_PASSWORD_FILE);
        if (jdbcPasswordFile != null) {
            try {
                jdbcPassword = Files.readString(Path.of(jdbcPasswordFile));
            } catch (IOException e) {
                throw new SystemException("Couldn't read JDBC password from specified file '"
                        + jdbcPasswordFile + "': " + e.getMessage(), e);
            }
        } else {
            jdbcPassword = configuration.getString(PROPERTY_JDBC_PASSWORD, null);
        }

        // maxPoolSize can't be smaller than MIN_POOL_SIZE_FLOOR
        maxPoolSize = Math.max(
                configuration.getInt(PROPERTY_MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE),
                MIN_POOL_SIZE_FLOOR);
        minPoolSize = configuration.getInt(PROPERTY_MIN_POOL_SIZE, Math.min(DEFAULT_MIN_POOL_SIZE, maxPoolSize));
        maxLifetime = configuration.getLong(PROPERTY_MAX_LIFETIME, null);
        idleTimeout = configuration.getLong(PROPERTY_IDLE_TIMEOUT, null);
        keepaliveTime = configuration.getLong(PROPERTY_KEEPALIVE_TIME, null);
        // 0 to disable, which is also HikariCP default
        leakDetectionThreshold = configuration.getLong(PROPERTY_LEAK_DETECTION_THRESHOLD, null);
        // 1ms is also HikariCP default, we use "long" for it so it must be set
        initializationFailTimeout = configuration.getLong(PROPERTY_INITIALIZATION_FAIL_TIMEOUT, 1L);

        fullObjectFormat = configuration.getString(PROPERTY_FULL_OBJECT_FORMAT, DEFAULT_FULL_OBJECT_FORMAT)
                .toLowerCase(); // all language string constants are lower-cases

        performanceStatisticsFile = configuration.getString(PROPERTY_PERFORMANCE_STATISTICS_FILE);
        performanceStatisticsLevel = configuration.getInt(PROPERTY_PERFORMANCE_STATISTICS_LEVEL,
                SqlPerformanceMonitorImpl.LEVEL_LOCAL_STATISTICS);

        iterativeSearchByPagingBatchSize = configuration.getInt(
                PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE, DEFAULT_ITERATIVE_SEARCH_PAGE_SIZE);
        createMissingCustomColumns =
                configuration.getBoolean(PROPERTY_CREATE_MISSING_CUSTOM_COLUMNS, false);

        sqlDurationWarningMs = configuration.getLong(
                PROPERTY_SQL_DURATION_WARNING_MS, DEFAULT_SQL_DURATION_WARNING_MS);

        validateConfiguration();
    }

    private void validateConfiguration() throws RepositoryServiceFactoryException {
        if (dataSource == null) {
            notEmpty(jdbcUrl, "JDBC URL is empty or not defined.");
            // We don't check username and password, they can be null (MID-5342)
            // In case of configuration mismatch we let the JDBC driver to fail.
            notEmpty(driverClassName, "Driver class name is empty or not defined.");
        }
    }

    public @NotNull SupportedDatabase getDatabaseType() {
        return DEFAULT_DATABASE;
    }

    public String getDataSource() {
        return dataSource;
    }

    protected void notEmpty(String value, String message) throws RepositoryServiceFactoryException {
        if (StringUtils.isEmpty(value)) {
            throw new RepositoryServiceFactoryException(message);
        }
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public static final String APPLICATION_NAME_JDBC_PARAM = "ApplicationName=";

    @Override
    public String getJdbcUrl(@NotNull String applicationName) {
        if (jdbcUrl != null && !jdbcUrl.contains(APPLICATION_NAME_JDBC_PARAM)) {
            return jdbcUrl + (jdbcUrl.contains("?") ? '&' : '?') + APPLICATION_NAME_JDBC_PARAM + applicationName;
        } else {
            return jdbcUrl;
        }
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    /** We leave potential compression to PG, for us the values are plain. */
    public boolean isUseZip() {
        throw new UnsupportedOperationException();
    }

    /** We leave potential compression to PG, for us the values are plain. */
    public boolean isUseZipAudit() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns serialization format (language) for writing fullObject.
     * Also see {@link #PROPERTY_FULL_OBJECT_FORMAT}.
     */
    public String getFullObjectFormat() {
        return fullObjectFormat;
    }

    @Override
    public boolean isEmbedded() {
        return false;
    }

    @Override
    public String getDefaultEmbeddedJdbcUrlPrefix() {
        throw new UnsupportedOperationException(
                "This configuration (repository factory) does not support embedded database.");
    }

    @Override
    public boolean isUsing(SupportedDatabase db) {
        return DEFAULT_DATABASE == db;
    }

    @Override
    public TransactionIsolation getTransactionIsolation() {
        // Not set explicitly, we leave it to PG, defaults to Connection.TRANSACTION_READ_COMMITTED
        return null;
    }

    @Override
    public boolean useSetReadOnlyOnConnection() {
        return true;
    }

    @Override
    public int getMinPoolSize() {
        return minPoolSize;
    }

    @Override
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    @Override
    public Long getMaxLifetime() {
        return maxLifetime;
    }

    @Override
    public Long getIdleTimeout() {
        return idleTimeout;
    }

    @Override
    public long getInitializationFailTimeout() {
        return initializationFailTimeout;
    }

    @Override
    public Long getKeepaliveTime() {
        return keepaliveTime;
    }

    @Override
    public Long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }

    @Override
    public String getPerformanceStatisticsFile() {
        return performanceStatisticsFile;
    }

    @Override
    public int getPerformanceStatisticsLevel() {
        return performanceStatisticsLevel;
    }

    @Override
    public int getIterativeSearchByPagingBatchSize() {
        return iterativeSearchByPagingBatchSize;
    }

    // exists because of testing
    public void setIterativeSearchByPagingBatchSize(int iterativeSearchByPagingBatchSize) {
        this.iterativeSearchByPagingBatchSize = iterativeSearchByPagingBatchSize;
    }

    @Override
    public boolean isCreateMissingCustomColumns() {
        return createMissingCustomColumns;
    }

    /**
     * Returns threshold duration for SQL, after which it should be logged on warning level.
     * Value of 0 or less means that this warning is disabled.
     */
    public long getSqlDurationWarningMs() {
        return sqlDurationWarningMs;
    }

    /**
     * Creates a copy of provided configuration for audit and applies override from config.xml.
     * This is used when the same data source is used by audit and repository.
     */
    public static SqaleRepositoryConfiguration initForAudit(
            @NotNull SqaleRepositoryConfiguration mainRepoConfig, Configuration auditConfig) {
        SqaleRepositoryConfiguration config = new SqaleRepositoryConfiguration(auditConfig);
        config.fullObjectFormat =
                auditConfig.getString(PROPERTY_FULL_OBJECT_FORMAT, mainRepoConfig.fullObjectFormat)
                        .toLowerCase();
        config.iterativeSearchByPagingBatchSize = auditConfig.getInt(
                PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE, mainRepoConfig.iterativeSearchByPagingBatchSize);
        config.createMissingCustomColumns = auditConfig.getBoolean(
                PROPERTY_CREATE_MISSING_CUSTOM_COLUMNS, mainRepoConfig.createMissingCustomColumns);

        // perf stats settings must be copied to allow proper perf monitoring of audit
        config.performanceStatisticsFile = mainRepoConfig.performanceStatisticsFile;
        config.performanceStatisticsLevel = mainRepoConfig.performanceStatisticsLevel;
        return config;
    }
}
