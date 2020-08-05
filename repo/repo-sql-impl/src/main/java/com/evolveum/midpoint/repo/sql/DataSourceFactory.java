/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.io.Closeable;
import java.io.IOException;
import javax.annotation.PreDestroy;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jndi.JndiObjectFactoryBean;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Viliam Repan (lazyman)
 */
public class DataSourceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(DataSourceFactory.class);

    private final SqlRepositoryConfiguration configuration;

    private DataSource internalDataSource;
    private DataSource dataSource;

    public DataSourceFactory(SqlRepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    public SqlRepositoryConfiguration configuration() {
        return configuration;
    }

    public DataSource createDataSource() throws RepositoryServiceFactoryException {
        LOGGER.info("Loading datasource.");
        if (configuration == null) {
            throw new RepositoryServiceFactoryException("SQL configuration is null, couldn't create datasource.");
        }

        try {
            if (StringUtils.isNotEmpty(configuration.getDataSource())) {
                LOGGER.info("JNDI datasource present in configuration, looking for '{}'.", configuration.getDataSource());
                dataSource = createJNDIDataSource();
            } else {
                LOGGER.info("Constructing default datasource with connection pooling; JDBC URL: {}", configuration.getJdbcUrl());
                internalDataSource = createDataSourceInternal();
                dataSource = internalDataSource;
            }
            return dataSource;
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException("Couldn't initialize datasource, reason: " + ex.getMessage(), ex);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    private DataSource createJNDIDataSource() throws IllegalArgumentException, NamingException {
        JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
        factory.setJndiName(configuration.getDataSource());
        factory.afterPropertiesSet();
        return (DataSource) factory.getObject();
    }

    private HikariConfig createConfig() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(configuration.getDriverClassName());
        config.setJdbcUrl(configuration.getJdbcUrl());
        config.setUsername(configuration.getJdbcUsername());
        config.setPassword(configuration.getJdbcPassword());

        config.setRegisterMbeans(true);

        config.setMinimumIdle(configuration.getMinPoolSize());
        config.setMaximumPoolSize(configuration.getMaxPoolSize());

        if (configuration.getMaxLifetime() != null) {
            config.setMaxLifetime(configuration.getMaxLifetime());
        }

        if (configuration.getIdleTimeout() != null) {
            config.setIdleTimeout(configuration.getIdleTimeout());
        }

        config.setIsolateInternalQueries(true);
//        config.setAutoCommit(false);

        TransactionIsolation ti = configuration.getTransactionIsolation();
        if (ti != null && TransactionIsolation.SNAPSHOT != ti) {
            config.setTransactionIsolation("TRANSACTION_" + ti.name());
        }

        if (configuration.isUsingMySqlCompatible()) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

//            config.addDataSourceProperty("useServerPrepStmts", "true");
//            config.addDataSourceProperty("useLocalSessionState", "true");
//            config.addDataSourceProperty("useLocalTransactionState", "true");
//            config.addDataSourceProperty("rewriteBatchedStatements", "true");
//            config.addDataSourceProperty("cacheResultSetMetadata", "true");
//            config.addDataSourceProperty("cacheServerConfiguration", "true");
//            config.addDataSourceProperty("elideSetAutoCommits", "true");
//            config.addDataSourceProperty("maintainTimeStats", "false");
        }

        config.setInitializationFailTimeout(configuration.getInitializationFailTimeout());

        return config;
    }

    private DataSource createDataSourceInternal() {
        HikariConfig config = createConfig();

        return new HikariDataSource(config);
    }

    @PreDestroy
    public void destroy() throws IOException {
        if (internalDataSource instanceof Closeable) {
            ((Closeable) internalDataSource).close();
        }
    }
}
