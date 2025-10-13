/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.testing;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * TODO: These can go away if StartupConfiguration can also override config properties from file.
 *  Currently in applyEnvironmentProperties it only knows how to do it from -D options.
 *  Another difference is that this reads properties like "database" which would have to be
 *  changed to "midpoint.repository.database".
 *  Still, it would be cool to get rid off this class and do more with production setup.
 *
 * This {@link SqlRepositoryConfiguration} factory should be used for testing purposes only.
 * During configuration initialization it checks system properties and overrides loaded
 * in {@link com.evolveum.midpoint.common.configuration.api.MidpointConfiguration} before
 * {@link SqlRepositoryConfiguration} used it.
 */
public class TestSqlRepositoryConfigurationFactory {

    private static final Trace LOGGER = TraceManager.getTrace(TestSqlRepositoryConfigurationFactory.class);

    public static final String PROPERTY_CONFIG = "config";

    private final MidpointConfiguration midpointConfiguration;

    public TestSqlRepositoryConfigurationFactory(MidpointConfiguration midpointConfiguration) {
        this.midpointConfiguration = midpointConfiguration;
    }

    public SqlRepositoryConfiguration createSqlRepositoryConfiguration()
            throws RepositoryServiceFactoryException {
        Configuration rawRepoConfig = midpointConfiguration.getConfiguration(
                MidpointConfiguration.REPOSITORY_CONFIGURATION);

        String configFile = System.getProperty(PROPERTY_CONFIG);
        if (StringUtils.isNotEmpty(configFile)) {
            LOGGER.info("Overriding loaded configuration with values from '{}'", configFile);
            updateConfigurationFromFile(rawRepoConfig, configFile);
        }

        updateConfigurationFromProperties(rawRepoConfig, null);

        SqlRepositoryConfiguration config = new SqlRepositoryConfiguration(rawRepoConfig);
        config.validate();
        return config;
    }

    private void updateConfigurationFromFile(Configuration configuration, String filePath)
            throws RepositoryServiceFactoryException {
        Properties properties = new Properties();
        try {
            File file = new File(filePath);
            LOGGER.debug("Config file absolute path '{}'.", file.getAbsolutePath());
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                throw new RepositoryServiceFactoryException("Config file '" + filePath + "' doesn't exist or can't be read.");
            }

            Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
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
        updateConfigurationStringProperty(configuration, properties, PROPERTY_DATABASE);

        updateConfigurationStringProperty(configuration, properties, PROPERTY_DRIVER_CLASS_NAME);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_HIBERNATE_DIALECT);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_HIBERNATE_HBM2DDL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_PASSWORD);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_URL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_JDBC_USERNAME);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_SKIP_EXPLICIT_SCHEMA_VALIDATION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_MISSING_SCHEMA_ACTION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_UPGRADEABLE_SCHEMA_ACTION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_INCOMPATIBLE_SCHEMA_ACTION);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_SCHEMA_VERSION_IF_MISSING);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_SCHEMA_VERSION_OVERRIDE);

        updateConfigurationStringProperty(configuration, properties, PROPERTY_TRANSACTION_ISOLATION);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE);
        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_LOCK_FOR_UPDATE_VIA_SQL);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_READ_ONLY_TRANSACTIONS_STATEMENT);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_PERFORMANCE_STATISTICS_FILE);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_PERFORMANCE_STATISTICS_LEVEL);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_ITERATIVE_SEARCH_BY_PAGING);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_MAX_OBJECTS_FOR_IMPLICIT_FETCH_ALL_ITERATION_METHOD);

        updateConfigurationBooleanProperty(configuration, properties, PROPERTY_USE_ZIP);
        updateConfigurationStringProperty(configuration, properties, PROPERTY_FULL_OBJECT_FORMAT);
        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_MIN_POOL_SIZE);
        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_MAX_POOL_SIZE);

        updateConfigurationIntegerProperty(configuration, properties, PROPERTY_TEXT_INFO_COLUMN_SIZE);
    }

    private void updateConfigurationIntegerProperty(
            Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null || !value.matches("[1-9][0-9]*")) {
            return;
        }
        int val = Integer.parseInt(value);
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, val);
        configuration.setProperty(propertyName, val);
    }

    private void updateConfigurationBooleanProperty(
            Configuration configuration, Properties properties, String propertyName) {
        String value = properties != null ? properties.getProperty(propertyName) : System.getProperty(propertyName);
        if (value == null) {
            return;
        }
        boolean val = Boolean.parseBoolean(value);
        LOGGER.info("Overriding loaded configuration with value read from system properties: {}={}", propertyName, val);
        configuration.setProperty(propertyName, val);
    }

    private void updateConfigurationStringProperty(
            Configuration configuration, Properties properties, String propertyName) {
        updateConfigurationStringProperty(configuration, properties, propertyName, null);
    }

    private void updateConfigurationStringProperty(
            Configuration configuration, Properties properties, String propertyName,
            @SuppressWarnings("SameParameterValue") String defaultValue) {
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
