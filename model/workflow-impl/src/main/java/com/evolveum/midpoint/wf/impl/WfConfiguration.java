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
package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.RepositoryFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 *
 * @author Pavol Mederly
 */
@Component
@DependsOn({ "midpointConfiguration" })

public class WfConfiguration implements BeanFactoryAware {

    private static final transient Trace LOGGER = TraceManager.getTrace(WfConfiguration.class);

    private static final String WF_CONFIG_SECTION = "midpoint.workflow";
    private static final String CHANGE_PROCESSORS_SECTION = "changeProcessors";         // deprecated

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_JDBC_DRIVER = "jdbcDriver";
    public static final String KEY_JDBC_URL = "jdbcUrl";
    public static final String KEY_JDBC_USERNAME = "jdbcUsername";
    public static final String KEY_JDBC_PASSWORD = "jdbcPassword";
    public static final String KEY_DATA_SOURCE = "dataSource";
    public static final String KEY_ACTIVITI_SCHEMA_UPDATE = "activitiSchemaUpdate";
    public static final String KEY_PROCESS_CHECK_INTERVAL = "processCheckInterval";
    public static final String KEY_AUTO_DEPLOYMENT_FROM = "autoDeploymentFrom";
    public static final String KEY_ALLOW_APPROVE_OTHERS_ITEMS = "allowApproveOthersItems";

    public static final List<String> KNOWN_KEYS = Arrays.asList("midpoint.home", KEY_ENABLED, KEY_JDBC_DRIVER, KEY_JDBC_URL,
            KEY_JDBC_USERNAME, KEY_JDBC_PASSWORD, KEY_DATA_SOURCE, KEY_ACTIVITI_SCHEMA_UPDATE, KEY_AUTO_DEPLOYMENT_FROM);

    public static final List<String> DEPRECATED_KEYS = Arrays.asList(CHANGE_PROCESSORS_SECTION, KEY_PROCESS_CHECK_INTERVAL, KEY_ALLOW_APPROVE_OTHERS_ITEMS);

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private static final String AUTO_DEPLOYMENT_FROM_DEFAULT = "classpath*:processes/*.bpmn20.xml";

    private boolean enabled;

    private boolean activitiSchemaUpdate;

    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private String dataSource;

    private List<ChangeProcessor> changeProcessors = new ArrayList<>();

    private String[] autoDeploymentFrom;

    boolean dropDatabase;

    @PostConstruct
    void initialize() {

        Configuration c = midpointConfiguration.getConfiguration(WF_CONFIG_SECTION);

        checkAllowedKeys(c, KNOWN_KEYS, DEPRECATED_KEYS);

        enabled = c.getBoolean(KEY_ENABLED, true);
        if (!enabled) {
            LOGGER.info("Workflows are disabled.");
            return;
        }

        // activiti properties related to database connection will be taken from SQL repository
        SqlRepositoryConfiguration sqlConfig = null;
        String defaultJdbcUrlPrefix = null;
        dropDatabase = false;
        try {
            RepositoryFactory repositoryFactory = (RepositoryFactory) beanFactory.getBean("repositoryFactory");
            if (!(repositoryFactory.getFactory() instanceof SqlRepositoryFactory)) {    // it may be null as well
                LOGGER.debug("SQL configuration cannot be found; Activiti database configuration (if any) will be taken from 'workflow' configuration section only");
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("repositoryFactory.getFactory() = " + repositoryFactory);
                }
            } else {
                SqlRepositoryFactory sqlRepositoryFactory = (SqlRepositoryFactory) repositoryFactory.getFactory();
                sqlConfig = sqlRepositoryFactory.getSqlConfiguration();
                if (sqlConfig.isEmbedded()) {
                    defaultJdbcUrlPrefix = sqlRepositoryFactory.prepareJdbcUrlPrefix(sqlConfig);
                    dropDatabase = sqlConfig.isDropIfExists();
                }
            }
        } catch(NoSuchBeanDefinitionException e) {
            LOGGER.debug("SqlRepositoryFactory is not available, Activiti database configuration (if any) will be taken from 'workflow' configuration section only.");
            LOGGER.trace("Reason is", e);
        } catch (RepositoryServiceFactoryException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot determine default JDBC URL for embedded database", e);
        }

        String explicitJdbcUrl = c.getString(KEY_JDBC_URL, null);
        if (explicitJdbcUrl == null) {
            if (sqlConfig == null || sqlConfig.isEmbedded()) {
                jdbcUrl = defaultJdbcUrlPrefix + "-activiti;DB_CLOSE_ON_EXIT=FALSE;MVCC=FALSE";
            } else {
                jdbcUrl = sqlConfig.getJdbcUrl();
            }
        } else {
            jdbcUrl = explicitJdbcUrl;
        }

        dataSource = c.getString(KEY_DATA_SOURCE, null);
        if (dataSource == null && explicitJdbcUrl == null && sqlConfig != null) {
            dataSource = sqlConfig.getDataSource();             // we want to use wf-specific JDBC if there is one (i.e. we do not want to inherit data source from repo in such a case)
        }

        if (dataSource != null) {
            LOGGER.info("Activiti database is at " + dataSource + " (a data source)");
        } else {
            LOGGER.info("Activiti database is at " + jdbcUrl + " (a JDBC URL)");
        }

		boolean defaultSchemaUpdate = sqlConfig == null || "update".equals(sqlConfig.getHibernateHbm2ddl());
        activitiSchemaUpdate = c.getBoolean(KEY_ACTIVITI_SCHEMA_UPDATE, defaultSchemaUpdate);
		LOGGER.info("Activiti automatic schema update: {}", activitiSchemaUpdate);

        jdbcDriver = c.getString(KEY_JDBC_DRIVER, sqlConfig != null ? sqlConfig.getDriverClassName() : null);
        jdbcUser = c.getString(KEY_JDBC_USERNAME, sqlConfig != null ? sqlConfig.getJdbcUsername() : null);
        jdbcPassword = c.getString(KEY_JDBC_PASSWORD, sqlConfig != null ? sqlConfig.getJdbcPassword() : null);

        autoDeploymentFrom = c.getStringArray(KEY_AUTO_DEPLOYMENT_FROM);
        if (autoDeploymentFrom.length == 0) {
            autoDeploymentFrom = new String[] { AUTO_DEPLOYMENT_FROM_DEFAULT };
        }

//        hibernateDialect = sqlConfig != null ? sqlConfig.getHibernateDialect() : "";

        validate();
    }

    public void checkAllowedKeys(Configuration c, List<String> knownKeys, List<String> deprecatedKeys) {
        Set<String> knownKeysSet = new HashSet<>(knownKeys);
        Set<String> deprecatedKeysSet = new HashSet<>(deprecatedKeys);

        Iterator<String> keyIterator = c.getKeys();
        while (keyIterator.hasNext())  {
            String keyName = keyIterator.next();
            String normalizedKeyName = StringUtils.substringBefore(keyName, ".");                       // because of subkeys
            normalizedKeyName = StringUtils.substringBefore(normalizedKeyName, "[");                    // because of [@xmlns:c]
            int colon = normalizedKeyName.indexOf(':');                                                 // because of c:generalChangeProcessorConfiguration
            if (colon != -1) {
                normalizedKeyName = normalizedKeyName.substring(colon + 1);
            }
            if (deprecatedKeysSet.contains(keyName) || deprecatedKeysSet.contains(normalizedKeyName)) {
                throw new SystemException("Deprecated key " + keyName + " in workflow configuration. Please see https://wiki.evolveum.com/display/midPoint/Workflow+configuration.");
            }
            if (!knownKeysSet.contains(keyName) && !knownKeysSet.contains(normalizedKeyName)) {         // ...we need to test both because of keys like 'midpoint.home'
                throw new SystemException("Unknown key " + keyName + " in workflow configuration");
            }
        }
    }

//    public Configuration getChangeProcessorsConfig() {
//        Validate.notNull(midpointConfiguration, "midpointConfiguration was not initialized correctly (check spring beans initialization order)");
//        return midpointConfiguration.getConfiguration(WF_CONFIG_SECTION).subset(CHANGE_PROCESSORS_SECTION); // via subset, because getConfiguration puts 'midpoint.home' property to the result
//    }

    void validate() {

        if (dataSource != null) {
            notEmpty(dataSource, "Data source (if specified) must not be an empty string");
        } else {
            notEmpty(jdbcDriver, "JDBC driver or a data source must be specified (either explicitly or in SQL repository configuration)");
            notEmpty(jdbcUrl, "JDBC URL or a data source must be specified (either explicitly or in SQL repository configuration).");
            notNull(jdbcUser, "JDBC user name or a data source must be specified (either explicitly or in SQL repository configuration).");
            notNull(jdbcPassword, "JDBC password or a data source must be specified (either explicitly or in SQL repository configuration).");
        }
    }

    private void notEmpty(String value, String message) {
        if (StringUtils.isEmpty(value)) {
            throw new SystemException(message);
        }
    }

    private void notNull(String value, String message) {
        if (value == null) {
            throw new SystemException(message);
        }
    }

    public boolean isActivitiSchemaUpdate() {
        return activitiSchemaUpdate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getJdbcUser() {
        return jdbcUser;
    }

    public String getDataSource() {
        return dataSource;
    }

    public String[] getAutoDeploymentFrom() {
        return autoDeploymentFrom;
    }

    public boolean isDropDatabase() {
        return dropDatabase;
    }

    public ChangeProcessor findChangeProcessor(String processorClassName) {
        for (ChangeProcessor cp : changeProcessors) {
            if (cp.getClass().getName().equals(processorClassName)) {
                return cp;
            }
        }

        throw new IllegalStateException("Change processor " + processorClassName + " is not registered.");
    }

    public void registerProcessor(ChangeProcessor changeProcessor) {
        changeProcessors.add(changeProcessor);
    }

    public List<ChangeProcessor> getChangeProcessors() {
        return changeProcessors;
    }
}
