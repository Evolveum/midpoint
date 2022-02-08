/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

/**
 * Common contract for SQL-based repository configuration.
 */
public interface JdbcRepositoryConfiguration {

    String PROPERTY_DATABASE = "database";

    String PROPERTY_DATASOURCE = "dataSource";

    String PROPERTY_DRIVER_CLASS_NAME = "driverClassName";
    String PROPERTY_JDBC_PASSWORD = "jdbcPassword";
    String PROPERTY_JDBC_PASSWORD_FILE = "jdbcPasswordFile";
    String PROPERTY_JDBC_USERNAME = "jdbcUsername";
    String PROPERTY_JDBC_URL = "jdbcUrl";

    String PROPERTY_MIN_POOL_SIZE = "minPoolSize";
    String PROPERTY_MAX_POOL_SIZE = "maxPoolSize";
    String PROPERTY_MAX_LIFETIME = "maxLifetime";
    String PROPERTY_IDLE_TIMEOUT = "idleTimeout";
    String PROPERTY_KEEPALIVE_TIME = "keepaliveTime";
    String PROPERTY_LEAK_DETECTION_THRESHOLD = "leakDetectionThreshold";
    String PROPERTY_INITIALIZATION_FAIL_TIMEOUT = "initializationFailTimeout";

    String PROPERTY_USE_ZIP = "useZip";
    String PROPERTY_USE_ZIP_AUDIT = "useZipAudit";

    /**
     * Specifies language used for writing fullObject attribute.
     * See LANG constants in {@link com.evolveum.midpoint.prism.PrismContext} for supported values.
     */
    String PROPERTY_FULL_OBJECT_FORMAT = "fullObjectFormat";

    String PROPERTY_PERFORMANCE_STATISTICS_FILE = "performanceStatisticsFile";
    String PROPERTY_PERFORMANCE_STATISTICS_LEVEL = "performanceStatisticsLevel";

    String PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE = "iterativeSearchByPagingBatchSize";
    String PROPERTY_CREATE_MISSING_CUSTOM_COLUMNS = "createMissingCustomColumns";

    SupportedDatabase getDatabaseType();
    String getDataSource();
    String getDriverClassName();
    String getJdbcUrl();

    /**
     * Allows distinguishing connections for various usages like repository, audit, scheduler.
     * By default, this doesn't do anything, but works for new repository.
     */
    default String getJdbcUrl(String applicationName) {
        return getJdbcUrl();
    }

    String getJdbcUsername();
    boolean isEmbedded();
    String getJdbcPassword();

    TransactionIsolation getTransactionIsolation();

    /**
     * If true, then {@link java.sql.Connection#setReadOnly(boolean)} is used for read only
     * transactions and {@link #getReadOnlyTransactionStatement()} is ignored.
     */
    default boolean useSetReadOnlyOnConnection() {
        return false;
    }

    /**
     * Statement to start or set read only transaction, null if not supported.
     */
    default String getReadOnlyTransactionStatement() {
        return null;
    }

    // For HikariCP config see: https://github.com/brettwooldridge/HikariCP#gear-configuration-knobs-baby
    // All times are in ms.
    long getInitializationFailTimeout();
    int getMinPoolSize();
    int getMaxPoolSize();
    Long getMaxLifetime();
    Long getIdleTimeout();
    default Long getKeepaliveTime() {
        return null;
    }
    default Long getLeakDetectionThreshold() {
        return null;
    }

    boolean isUseZip();
    boolean isUseZipAudit();
    boolean isUsing(SupportedDatabase db);

    /**
     * Returns serialization format (language) for writing fullObject.
     * Also see {@link #PROPERTY_FULL_OBJECT_FORMAT}.
     */
    String getFullObjectFormat();
    String getDefaultEmbeddedJdbcUrlPrefix();

    String getPerformanceStatisticsFile();
    int getPerformanceStatisticsLevel();

    default boolean isUsingH2() {
        return isUsing(SupportedDatabase.H2);
    }

    default boolean isUsingOracle() {
        return isUsing(SupportedDatabase.ORACLE);
    }

    default boolean isUsingPostgreSQL() {
        return isUsing(SupportedDatabase.POSTGRESQL);
    }

    default boolean isUsingSQLServer() {
        return isUsing(SupportedDatabase.SQLSERVER);
    }

    int getIterativeSearchByPagingBatchSize();

    boolean isCreateMissingCustomColumns();
}
