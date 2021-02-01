/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private static final String DEFAULT_DRIVER = "org.postgresql.Driver";
    private static final SupportedDatabase DEFAULT_DATABASE = SupportedDatabase.POSTGRESQL;

    private static final int DEFAULT_MIN_POOL_SIZE = 8;
    private static final int DEFAULT_MAX_POOL_SIZE = 20;

    /**
     * Database kind - either explicitly configured or derived from other options .
     */
    @NotNull
    private final SupportedDatabase databaseType;

    // either dataSource or JDBC URL must be set
    @Nullable private final String dataSource;
    @Nullable private final String jdbcUrl;
    @Nullable private final String jdbcUsername;
    @Nullable private final String jdbcPassword;

    private final String driverClassName;

    private final int minPoolSize;
    private final int maxPoolSize;

    private final boolean useZip;
    private final boolean useZipAudit;
    private final String fullObjectFormat;

    private final String performanceStatisticsFile;
    private final int performanceStatisticsLevel;

    public SqaleRepositoryConfiguration(Configuration configuration) {
        dataSource = configuration.getString(PROPERTY_DATASOURCE);

        jdbcUrl = configuration.getString(PROPERTY_JDBC_URL, defaultJdbcUrl());
        jdbcUsername = configuration.getString(PROPERTY_JDBC_USERNAME);

        // TODO perhaps add warnings that other values are ignored anyway?
        databaseType = DEFAULT_DATABASE;
//        databaseType = Optional.ofNullable(configuration.getString(PROPERTY_DATABASE))
//                .map(s -> SupportedDatabase.valueOf(s.toUpperCase()))
//                .orElse(guessDatabaseType(configuration));

        driverClassName = DEFAULT_DRIVER; //configuration.getString(PROPERTY_DRIVER_CLASS_NAME);

        String jdbcPasswordFile = configuration.getString(PROPERTY_JDBC_PASSWORD_FILE);
        if (jdbcPasswordFile != null) {
            try {
                jdbcPassword = Files.readString(Path.of(jdbcPasswordFile));
            } catch (IOException e) {
                throw new SystemException("Couldn't read JDBC password from specified file '"
                        + jdbcPasswordFile + "': " + e.getMessage(), e);
            }
        } else {
            jdbcPassword = System.getProperty(PROPERTY_JDBC_PASSWORD,
                    configuration.getString(PROPERTY_JDBC_PASSWORD));
        }

        minPoolSize = configuration.getInt(PROPERTY_MIN_POOL_SIZE, DEFAULT_MIN_POOL_SIZE);
        maxPoolSize = configuration.getInt(PROPERTY_MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);

        useZip = configuration.getBoolean(PROPERTY_USE_ZIP, false);
        useZipAudit = configuration.getBoolean(PROPERTY_USE_ZIP_AUDIT, true);
        fullObjectFormat = System.getProperty(PROPERTY_FULL_OBJECT_FORMAT,
                configuration.getString(PROPERTY_FULL_OBJECT_FORMAT, PrismContext.LANG_XML));

        performanceStatisticsFile = configuration.getString(PROPERTY_PERFORMANCE_STATISTICS_FILE);
        performanceStatisticsLevel = configuration.getInt(PROPERTY_PERFORMANCE_STATISTICS_LEVEL,
                SqlPerformanceMonitorImpl.LEVEL_LOCAL_STATISTICS);
    }

    protected String defaultJdbcUrl() {
        return null;
    }

    public SupportedDatabase getDatabaseType() {
        return databaseType;
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

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public boolean isUseZip() {
        return useZip;
    }

    public boolean isUseZipAudit() {
        return useZipAudit;
    }

    /**
     * Returns serialization format (language) for writing fullObject.
     * Also see {@link #PROPERTY_FULL_OBJECT_FORMAT}.
     */
    public String getFullObjectFormat() {
        return fullObjectFormat;
    }

    public SqaleRepositoryConfiguration validate() throws RepositoryServiceFactoryException {
        if (dataSource == null) {
            notEmpty(jdbcUrl, "JDBC Url is empty or not defined.");
            // We don't check username and password, they can be null (MID-5342)
            // In case of configuration mismatch we let the JDBC driver to fail.
            notEmpty(driverClassName, "Driver class name is empty or not defined.");
        }

        // TODO the rest from SqlRepoConf#validate except for Hibernate of course

        return this;
    }

    public String getDefaultEmbeddedJdbcUrlPrefix() {
        throw new UnsupportedOperationException(
                "This configuration (repository factory) does not support embedded database.");
    }

    @Override
    public boolean isEmbedded() {
        throw new UnsupportedOperationException(
                "This configuration (repository factory) does not support embedded database.");
    }

    @Override
    public boolean isUsing(SupportedDatabase db) {
        return databaseType == db;
    }

    // TODO - IMPLEMENT EVERYTHING BELOW
    @Override
    public TransactionIsolation getTransactionIsolation() {
        return null;
    }

    @Override
    public String getReadOnlyTransactionStatement() {
        return null;
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
        return null;
    }

    @Override
    public Long getIdleTimeout() {
        return null;
    }

    @Override
    public long getInitializationFailTimeout() {
        return 0;
    }

    @Override
    public boolean isFatalException(Throwable ex) {
        return false;
    }

    @Override
    public String getPerformanceStatisticsFile() {
        return performanceStatisticsFile;
    }

    @Override
    public int getPerformanceStatisticsLevel() {
        return performanceStatisticsLevel;
    }
}
