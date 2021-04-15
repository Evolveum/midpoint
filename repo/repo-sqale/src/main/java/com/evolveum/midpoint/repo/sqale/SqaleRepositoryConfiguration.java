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
import javax.annotation.PostConstruct;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
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

    @NotNull private final Environment env; // for better Spring properties/override integration
    @NotNull private final Configuration configuration;

    /** Database kind - either explicitly configured or derived from other options. */
    private SupportedDatabase databaseType;

    // either dataSource or JDBC URL must be set
    private String dataSource;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;

    private String driverClassName;

    private int minPoolSize;
    private int maxPoolSize;

    private boolean useZip;
    private boolean useZipAudit;
    private String fullObjectFormat;

    private String performanceStatisticsFile;
    private int performanceStatisticsLevel;

    public SqaleRepositoryConfiguration(
            @NotNull Environment env,
            @NotNull Configuration configuration) {
        this.env = env;
        this.configuration = configuration;
    }

    @PostConstruct
    public void init() throws RepositoryServiceFactoryException {
        // TODO the rest from SqlRepoConf#validate except for Hibernate of course
        dataSource = getString(PROPERTY_DATASOURCE);

        jdbcUrl = configuration.getString(PROPERTY_JDBC_URL, defaultJdbcUrl());
        jdbcUsername = getString(PROPERTY_JDBC_USERNAME);

        // TODO perhaps add warnings that other values are ignored anyway?
        databaseType = DEFAULT_DATABASE;
        driverClassName = DEFAULT_DRIVER;

        String jdbcPasswordFile = getString(PROPERTY_JDBC_PASSWORD_FILE);
        if (jdbcPasswordFile != null) {
            try {
                jdbcPassword = Files.readString(Path.of(jdbcPasswordFile));
            } catch (IOException e) {
                throw new SystemException("Couldn't read JDBC password from specified file '"
                        + jdbcPasswordFile + "': " + e.getMessage(), e);
            }
        } else {
            jdbcPassword = System.getProperty(PROPERTY_JDBC_PASSWORD,
                    getString(PROPERTY_JDBC_PASSWORD));
        }

        minPoolSize = configuration.getInt(PROPERTY_MIN_POOL_SIZE, DEFAULT_MIN_POOL_SIZE);
        maxPoolSize = configuration.getInt(PROPERTY_MAX_POOL_SIZE, DEFAULT_MAX_POOL_SIZE);

        useZip = configuration.getBoolean(PROPERTY_USE_ZIP, false);
        useZipAudit = configuration.getBoolean(PROPERTY_USE_ZIP_AUDIT, true);
        fullObjectFormat = getString(PROPERTY_FULL_OBJECT_FORMAT, PrismContext.LANG_XML)
                .toLowerCase(); // all language string constants are lower-cases

        performanceStatisticsFile = getString(PROPERTY_PERFORMANCE_STATISTICS_FILE);
        performanceStatisticsLevel = configuration.getInt(PROPERTY_PERFORMANCE_STATISTICS_LEVEL,
                SqlPerformanceMonitorImpl.LEVEL_LOCAL_STATISTICS);

        validateConfiguration();
    }

    private String getString(String property) {
        return getString(property, null);
    }

    /**
     * Returns property value as string in this precedence:
     *
     * * using just property name (local, short name) against Spring environment, see
     * https://docs.spring.io/spring-boot/docs/current/reference/html/spring-boot-features.html#boot-features-external-config[this documentation]
     * for particular order of value resolution;
     * * using property name with repository configuration prefix (`midpoint.repository.*`) which is
     * future proof if we want to migrate to native Spring properties configuration;
     * * finally, {@link Configuration} object representing content of `midpoint/repository`
     * elements is used.
     */
    private String getString(String property, String defaultValue) {
        return env.getProperty(property,
                env.getProperty(MidpointConfiguration.REPOSITORY_CONFIGURATION + '.' + property,
                        configuration.getString(property, defaultValue)));
    }

    private void validateConfiguration() throws RepositoryServiceFactoryException {
        if (dataSource == null) {
            notEmpty(jdbcUrl, "JDBC Url is empty or not defined.");
            // We don't check username and password, they can be null (MID-5342)
            // In case of configuration mismatch we let the JDBC driver to fail.
            notEmpty(driverClassName, "Driver class name is empty or not defined.");
        }
    }

    protected String defaultJdbcUrl() {
        return null;
    }

    public @NotNull SupportedDatabase getDatabaseType() {
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
        // by default, any exception is fatal, unless specified otherwise (not yet implemented)
        return true;
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
