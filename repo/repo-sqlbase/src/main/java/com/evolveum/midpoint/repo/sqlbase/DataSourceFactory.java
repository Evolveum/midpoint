/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase;

import java.io.Closeable;
import java.io.IOException;
import jakarta.annotation.PreDestroy;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jndi.JndiObjectFactoryBean;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class DataSourceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(DataSourceFactory.class);

    private final JdbcRepositoryConfiguration configuration;

    private boolean internalDataSource = false;

    private DataSource dataSource;

    public DataSourceFactory(JdbcRepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    public JdbcRepositoryConfiguration configuration() {
        return configuration;
    }

    public DataSource createDataSource(String applicationName) throws RepositoryServiceFactoryException {
        LOGGER.info("Loading datasource.");
        if (configuration == null) {
            throw new RepositoryServiceFactoryException(
                    "SQL configuration is null, couldn't create datasource.");
        }

        if (StringUtils.isNotEmpty(configuration.getDataSource())) {
            try {
                LOGGER.info("JNDI datasource present in configuration, looking for '{}'.",
                        configuration.getDataSource());
                dataSource = createJndiDataSource();
            } catch (Exception ex) {
                throw new RepositoryServiceFactoryException(
                        "Couldn't initialize datasource using datasource " + configuration.getDataSource()
                                + ", reason: " + ex.getMessage(), ex);
            }
        } else {
            String jdbcUrl = configuration.getJdbcUrl(applicationName);
            try {
                LOGGER.info("Constructing datasource '{}' with connection pooling; JDBC URL: {}"
                                + "\n Using driver: {}",
                        applicationName, jdbcUrl, configuration.getDriverClassName());
                HikariConfig config = createHikariConfig(applicationName);
                dataSource = new HikariDataSource(config);
                internalDataSource = true;
            } catch (Exception ex) {
                throw new RepositoryServiceFactoryException(
                        "Couldn't initialize datasource using JDBC URL " + jdbcUrl
                                + ", reason: " + ex.getMessage(), ex);
            }
        }
        return dataSource;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    private DataSource createJndiDataSource() throws IllegalArgumentException, NamingException {
        JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
        factory.setJndiName(configuration.getDataSource());
        factory.afterPropertiesSet();
        return (DataSource) factory.getObject();
    }

    private HikariConfig createHikariConfig(String applicationName) {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(configuration.getDriverClassName());
        config.setJdbcUrl(configuration.getJdbcUrl(applicationName));
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
        if (configuration.getKeepaliveTime() != null) {
            config.setKeepaliveTime(configuration.getKeepaliveTime());
        }
        if (configuration.getLeakDetectionThreshold() != null) {
            config.setLeakDetectionThreshold(configuration.getLeakDetectionThreshold());
        }

        config.setIsolateInternalQueries(true);

        TransactionIsolation ti = configuration.getTransactionIsolation();
        if (ti != null && TransactionIsolation.SNAPSHOT != ti) {
            config.setTransactionIsolation("TRANSACTION_" + ti.name());
        }

        config.setInitializationFailTimeout(configuration.getInitializationFailTimeout());
        // We don't want auto commit to assure rollback behavior on close() if connection was not committed.
        config.setAutoCommit(false);

        return config;
    }

    @PreDestroy
    public void destroy() throws IOException {
        if (internalDataSource && dataSource instanceof Closeable) {
            ((Closeable) dataSource).close();
        }
    }
}
