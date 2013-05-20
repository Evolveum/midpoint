/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.testing;

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

        LOGGER.info("Overriding loaded configuration with values read from system properties.");
        updateConfigurationBooleanProperty(configuration, PROPERTY_EMBEDDED);
        updateConfigurationBooleanProperty(configuration, PROPERTY_DROP_IF_EXISTS);
        updateConfigurationBooleanProperty(configuration, PROPERTY_AS_SERVER);
        updateConfigurationBooleanProperty(configuration, PROPERTY_TCP_SSL);

        updateConfigurationIntegerProperty(configuration, PROPERTY_PORT);

        updateConfigurationStringProperty(configuration, PROPERTY_BASE_DIR);
        updateConfigurationStringProperty(configuration, PROPERTY_FILE_NAME);
        updateConfigurationStringProperty(configuration, PROPERTY_DRIVER_CLASS_NAME);
        updateConfigurationStringProperty(configuration, PROPERTY_HIBERNATE_DIALECT);
        updateConfigurationStringProperty(configuration, PROPERTY_HIBERNATE_HBM2DDL);
        updateConfigurationStringProperty(configuration, PROPERTY_JDBC_PASSWORD);
        updateConfigurationStringProperty(configuration, PROPERTY_JDBC_URL);
        updateConfigurationStringProperty(configuration, PROPERTY_JDBC_USERNAME);

        updateConfigurationStringProperty(configuration, PROPERTY_TRANSACTION_ISOLATION);
        updateConfigurationBooleanProperty(configuration, PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE);
        updateConfigurationBooleanProperty(configuration, PROPERTY_LOCK_FOR_UPDATE_VIA_SQL);
        updateConfigurationBooleanProperty(configuration, PROPERTY_USE_READ_ONLY_TRANSACTIONS);
        updateConfigurationStringProperty(configuration, PROPERTY_PERFORMANCE_STATISTICS_FILE);
        updateConfigurationStringProperty(configuration, PROPERTY_PERFORMANCE_STATISTICS_LEVEL);

        updateConfigurationBooleanProperty(configuration, PROPERTY_ITERATIVE_SEARCH_BY_PAGING);
        updateConfigurationStringProperty(configuration, PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE);

        super.init(configuration);
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
    }

    private void updateConfigurationIntegerProperty(Configuration configuration, String propertyName) {
        updateConfigurationIntegerProperty(configuration, null, propertyName);
    }

    private void updateConfigurationIntegerProperty(Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null || !value.matches("[1-9]{1}[0-9]*")) {
            return;
        }

        configuration.setProperty(propertyName, Integer.parseInt(value));
    }

    private void updateConfigurationBooleanProperty(Configuration configuration, String propertyName) {
        updateConfigurationBooleanProperty(configuration, null, propertyName);
    }

    private void updateConfigurationBooleanProperty(Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null) {
            return;
        }

        configuration.setProperty(propertyName, new Boolean(value).booleanValue());
    }

    private void updateConfigurationStringProperty(Configuration configuration, String propertyName) {
        updateConfigurationStringProperty(configuration, null, propertyName);
    }

    private void updateConfigurationStringProperty(Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null) {
            return;
        }

        configuration.setProperty(propertyName, value);
    }
}
