/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.*;

/**
 * This repository factory should be used for testing purposes only. It behaves like {@link com.evolveum.midpoint.repo.sql.SqlRepositoryFactory},
 * but during configuration initialization it checks system properties and overrides loaded configuration
 * ({@link com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration}).
 *
 * @author lazyman
 */
public class TestSqlRepositoryFactory extends SqlRepositoryFactory {

    private static final Trace LOGGER = TraceManager.getTrace(TestSqlRepositoryFactory.class);

    public static final String PROPERTY_CONFIG = "config";

    @Override
    public synchronized void init(Configuration configuration) throws RepositoryServiceFactoryException {
        String configFile = System.getProperty(PROPERTY_CONFIG);
        if (StringUtils.isNotEmpty(configFile)) {
            LOGGER.info("Overriding loaded configuration with values from '{}'", new Object[]{configFile});
            updateConfigurationFromFile(configuration, configFile);
        }

        updateConfigurationFromProperties(configuration, null);

        super.init(configuration);
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
        return new TestSqlRepositoryServiceImpl(this);
    }

    private void updateConfigurationFromFile(Configuration configuration, String filePath) throws RepositoryServiceFactoryException {
        Properties properties = new Properties();
        try {
            File file = new File(filePath);
            LOGGER.debug("Config file absolute path '{}'.", new Object[]{file.getAbsolutePath()});
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                throw new RepositoryServiceFactoryException("Config file '" + filePath + "' doesn't exist or can't be read.");
            }

            Reader reader = new InputStreamReader(new FileInputStream(file), "utf-8");
            properties.load(reader);
        } catch (RepositoryServiceFactoryException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException(ex.getMessage(), ex);
        }

        //override loaded configuration based on properties file...
        updateConfigurationFromProperties(configuration, properties);
    }

    private void updateConfigurationFromProperties(Configuration configuration, Properties properties) {
        updateConfigurationStringProperty(configuration, properties, PROPERTY_DATABASE, "h2");

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_EMBEDDED);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_DROP_IF_EXISTS);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_AS_SERVER);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_TCP_SSL);

        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_PORT);

        updateConfigurationStringProperty(configuration, properties, PROPERTY_BASE_DIR);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_FILE_NAME);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_DRIVER_CLASS_NAME);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_HIBERNATE_DIALECT);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_HIBERNATE_HBM2DDL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_PASSWORD);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_URL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_USERNAME);

        updateConfigurationStringProperty(configuration, properties, PROPERTY_TRANSACTION_ISOLATION);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_LOCK_FOR_UPDATE_VIA_SQL);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_USE_READ_ONLY_TRANSACTIONS);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_PERFORMANCE_STATISTICS_FILE);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_PERFORMANCE_STATISTICS_LEVEL);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_ITERATIVE_SEARCH_BY_PAGING);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_USE_ZIP);
        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_MIN_POOL_SIZE);
        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_MAX_POOL_SIZE);
    }

    private void updateConfigurationIntegerProperty(Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null || !value.matches("[1-9]{1}[0-9]*")) {
            return;
        }
        int val = Integer.parseInt(value);
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, val);
        configuration.setProperty(propertyName, val);
    }

    private void updateConfigurationBooleanProperty(Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null) {
            return;
        }
        boolean val = new Boolean(value).booleanValue();
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, val);
        configuration.setProperty(propertyName, val);
    }

    private void updateConfigurationStringProperty(Configuration configuration, Properties properties, String propertyName) {
        updateConfigurationStringProperty(configuration, properties, propertyName, null);
    }

    private void updateConfigurationStringProperty(Configuration configuration, Properties properties, String propertyName, String defaultValue) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null) {
            value = defaultValue;
        }

        if (value == null) {
            return;
        }
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, value);
        configuration.setProperty(propertyName, value);
    }
}
