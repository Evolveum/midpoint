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
import java.util.Optional;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.SupportedDatabase;
import com.evolveum.midpoint.repo.sqlbase.TransactionIsolation;
import com.evolveum.midpoint.util.exception.SystemException;

/**
 * Common part of the SQL-based repository configuration.
 * Contains JDBC/datasource setup, connection pool configuration, but no framework/ORM stuff.
 */
public class SqaleRepositoryConfiguration implements JdbcRepositoryConfiguration {

    public static final String PROPERTY_DATABASE = "database";

    public static final String PROPERTY_DATASOURCE = "dataSource";

    public static final String PROPERTY_DRIVER_CLASS_NAME = "driverClassName";
    public static final String PROPERTY_JDBC_PASSWORD = "jdbcPassword";
    public static final String PROPERTY_JDBC_PASSWORD_FILE = "jdbcPasswordFile";
    public static final String PROPERTY_JDBC_USERNAME = "jdbcUsername";
    public static final String PROPERTY_JDBC_URL = "jdbcUrl";

    public static final String PROPERTY_USE_ZIP = "useZip";
    public static final String PROPERTY_USE_ZIP_AUDIT = "useZipAudit";

    /**
     * Specifies language used for writing fullObject attribute.
     * See LANG constants in {@link PrismContext} for supported values.
     */
    public static final String PROPERTY_FULL_OBJECT_FORMAT = "fullObjectFormat";

    /**
     * Database kind - either explicitly configured or derived from other options .
     */
//    @NotNull // TODO we want this not null eventually, see guessDatabaseType()
    private final SupportedDatabase databaseType;

    // either dataSource or JDBC URL must be set
    @Nullable private final String dataSource;
    @Nullable private final String jdbcUrl;
    @Nullable private final String jdbcUsername;
    @Nullable private final String jdbcPassword;

    private final String driverClassName;

    private final boolean useZip;
    private final boolean useZipAudit;
    private final String fullObjectFormat;

    public SqaleRepositoryConfiguration(Configuration configuration) {
        dataSource = configuration.getString(PROPERTY_DATASOURCE);

        jdbcUrl = configuration.getString(PROPERTY_JDBC_URL, defaultJdbcUrl());
        jdbcUsername = configuration.getString(PROPERTY_JDBC_USERNAME);

        driverClassName = configuration.getString(PROPERTY_DRIVER_CLASS_NAME);

        databaseType = Optional.ofNullable(configuration.getString(PROPERTY_DATABASE))
                .map(s -> SupportedDatabase.valueOf(s.toUpperCase()))
                .orElse(guessDatabaseType(configuration));

        String jdbcPasswordFile = configuration.getString(PROPERTY_JDBC_PASSWORD_FILE);
        if (jdbcPasswordFile != null) {
            try {
                jdbcPassword = Files.readString(Path.of(jdbcPasswordFile));
            } catch (IOException e) {
                throw new SystemException("Couldn't read JDBC password from specified file '" + jdbcPasswordFile + "': " + e.getMessage(), e);
            }
        } else {
            jdbcPassword = System.getProperty(PROPERTY_JDBC_PASSWORD,
                    configuration.getString(PROPERTY_JDBC_PASSWORD));
        }

        useZip = configuration.getBoolean(PROPERTY_USE_ZIP, false);
        useZipAudit = configuration.getBoolean(PROPERTY_USE_ZIP_AUDIT, true);
        fullObjectFormat = System.getProperty(PROPERTY_FULL_OBJECT_FORMAT,
                configuration.getString(PROPERTY_FULL_OBJECT_FORMAT, PrismContext.LANG_XML));
    }

    protected String defaultJdbcUrl() {
        return null;
    }

    // TODO: we want this not null eventually, but if it is not null in this base class,
    //  how can subclass add any information (e.g. using Hibernate dialect)?
    protected SupportedDatabase guessDatabaseType(Configuration configuration) {
        // TODO
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

    public void validate() throws RepositoryServiceFactoryException {
        if (dataSource == null) {
            notEmpty(jdbcUrl, "JDBC Url is empty or not defined.");
            // We don't check username and password, they can be null (MID-5342)
            // In case of configuration mismatch we let the JDBC driver to fail.
            notEmpty(driverClassName, "Driver class name is empty or not defined.");
        }
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
        return 0;
    }

    @Override
    public int getMaxPoolSize() {
        return 0;
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
    public boolean shouldRollback(Throwable ex) {
        return false;
    }
}
