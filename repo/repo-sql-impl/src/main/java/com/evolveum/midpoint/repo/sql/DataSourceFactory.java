/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.jndi.JndiObjectFactoryBean;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;

/**
 * @author Viliam Repan (lazyman)
 */
public class DataSourceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(DataSourceFactory.class);

    private SqlRepositoryConfiguration configuration;

    private DataSource dataSource;

    public SqlRepositoryConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(SqlRepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    public DataSource createDataSource() throws RepositoryServiceFactoryException {
        LOGGER.info("Loading datasource.");
        if (configuration == null) {
            throw new RepositoryServiceFactoryException("SQL configuration is null, couldn't create datasource.");
        }

        try {
            if (StringUtils.isNotEmpty(configuration.getDataSource())) {
                LOGGER.info("JDNI datasource present in configuration, looking for '{}'.",
                        new Object[]{configuration.getDataSource()});
                return createJNDIDataSource();
            }

            LOGGER.info("Constructing default datasource with connection pooling; JDBC URL: {}", configuration.getJdbcUrl());
            dataSource = createDataSourceInternal();
            return dataSource;
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException("Couldn't initialize datasource, reason: " + ex.getMessage(), ex);
        }
    }

    private DataSource createJNDIDataSource() throws IllegalArgumentException, NamingException {
        JndiObjectFactoryBean factory = new JndiObjectFactoryBean();
        factory.setJndiName(configuration.getDataSource());
        factory.afterPropertiesSet();

        return (DataSource) factory.getObject();
    }

    public HikariConfig createConfig() {
        HikariConfig config = new HikariConfig();

        config.setDriverClassName(configuration.getDriverClassName());
        config.setJdbcUrl(configuration.getJdbcUrl());
        config.setUsername(configuration.getJdbcUsername());
        config.setPassword(configuration.getJdbcPassword());

        config.setRegisterMbeans(true);

        config.setMinimumIdle(configuration.getMinPoolSize());
        config.setMaximumPoolSize(configuration.getMaxPoolSize());

        TransactionIsolation ti = configuration.getTransactionIsolation();
        if (ti != null) {
            config.setTransactionIsolation("TRANSACTION_" + ti.name());
        }

        // todo fix this !!! and the same in ctx-test-datasource.xml
        //        config.setConnectionTesterClassName(MidPointConnectionTester.class.getName());

        if (configuration.isUsingMySqlCompatible()) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("useLocalTransactionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");
        }

        return config;
    }

    private DataSource createDataSourceInternal() {
        HikariConfig config = createConfig();

        return new HikariDataSource(config);
    }

    public void destroy() throws IOException {
        if (dataSource instanceof Closeable) {
            ((Closeable) dataSource).close();
        }
    }
}
