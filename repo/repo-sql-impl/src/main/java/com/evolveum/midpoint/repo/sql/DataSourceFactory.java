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
import com.evolveum.midpoint.repo.sql.util.MidPointConnectionCustomizer;
import com.evolveum.midpoint.repo.sql.util.MidPointConnectionTester;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.lang.StringUtils;
import org.springframework.jndi.JndiObjectFactoryBean;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;

/**
 * @author Viliam Repan (lazyman)
 */
public class DataSourceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(DataSourceFactory.class);

    private SqlRepositoryConfiguration configuration;

    private DataSource dataSource;

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

            LOGGER.info("Constructing default C3P0 datasource with connection pooling; JDBC URL: {}", configuration.getJdbcUrl());
            dataSource = createC3P0DataSource();
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

    private DataSource createC3P0DataSource() throws PropertyVetoException {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setDriverClass(configuration.getDriverClassName());
        ds.setJdbcUrl(configuration.getJdbcUrl());
        ds.setUser(configuration.getJdbcUsername());
        ds.setPassword(configuration.getJdbcPassword());

        ds.setAcquireIncrement(3);
        ds.setMinPoolSize(configuration.getMinPoolSize());
        ds.setMaxPoolSize(configuration.getMaxPoolSize());
        ds.setIdleConnectionTestPeriod(1800);
        ds.setConnectionTesterClassName(MidPointConnectionTester.class.getName());
        ds.setConnectionCustomizerClassName(MidPointConnectionCustomizer.class.getName());

        return ds;
    }

    public void destroy() {
        if (dataSource instanceof ComboPooledDataSource) {
            ComboPooledDataSource ds = (ComboPooledDataSource) dataSource;
            ds.close();
        }
    }
}
