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
package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.RepositoryFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
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
    private static final String CHANGE_PROCESSORS_SECTION = "changeProcessors";

    public static final String KEY_ENABLED = "enabled";
    public static final String KEY_JDBC_DRIVER = "jdbcDriver";
    public static final String KEY_JDBC_URL = "jdbcUrl";
    public static final String KEY_JDBC_USERNAME = "jdbcUsername";
    public static final String KEY_JDBC_PASSWORD = "jdbcPassword";
    public static final String KEY_ACTIVITI_SCHEMA_UPDATE = "activitiSchemaUpdate";
    public static final String KEY_PROCESS_CHECK_INTERVAL = "processCheckInterval";
    public static final String KEY_AUTO_DEPLOYMENT_FROM = "autoDeploymentFrom";
    public static final String KEY_ALLOW_APPROVE_OTHERS_ITEMS = "allowApproveOthersItems";

    public static final String[] KNOWN_KEYS = { "midpoint.home", KEY_ENABLED, KEY_JDBC_DRIVER, KEY_JDBC_URL,
            KEY_JDBC_USERNAME, KEY_JDBC_PASSWORD, KEY_ACTIVITI_SCHEMA_UPDATE, KEY_PROCESS_CHECK_INTERVAL,
            KEY_AUTO_DEPLOYMENT_FROM, KEY_ALLOW_APPROVE_OTHERS_ITEMS, CHANGE_PROCESSORS_SECTION };

    @Autowired(required = true)
    private MidpointConfiguration midpointConfiguration;

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private static final String AUTO_DEPLOYMENT_FROM_DEFAULT = "classpath*:processes/*.bpmn20.xml";

    private Boolean enabled = null;

    private boolean activitiSchemaUpdate;

    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private boolean allowApproveOthersItems;

    private List<ChangeProcessor> changeProcessors = null;

    private int processCheckInterval;
    private String autoDeploymentFrom;

    @PostConstruct
    void initialize() {

        Configuration c = midpointConfiguration.getConfiguration(WF_CONFIG_SECTION);

        checkAllowedKeys(c, KNOWN_KEYS);

        enabled = c.getBoolean(KEY_ENABLED, true);
        if (!enabled) {
            LOGGER.info("Workflows are disabled.");
            return;
        }

        // activiti properties related to database connection will be taken from SQL repository
        SqlRepositoryConfiguration sqlConfig = null;
        String defaultJdbcUrlPrefix = null;
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
                }
            }
        } catch(NoSuchBeanDefinitionException e) {
            LOGGER.debug("SqlRepositoryFactory is not available, Activiti database configuration (if any) will be taken from 'workflow' configuration section only.");
            LOGGER.trace("Reason is", e);
        } catch (RepositoryServiceFactoryException e) {
            LoggingUtils.logException(LOGGER, "Cannot determine default JDBC URL for embedded database", e);
        }

        jdbcUrl = c.getString(KEY_JDBC_URL, null);
        if (jdbcUrl == null) {
            if (sqlConfig.isEmbedded()) {
                jdbcUrl = defaultJdbcUrlPrefix + "-activiti;DB_CLOSE_ON_EXIT=FALSE";
            } else {
                jdbcUrl = sqlConfig.getJdbcUrl();
            }
        }
        LOGGER.info("Activiti database is at " + jdbcUrl);

        activitiSchemaUpdate = c.getBoolean(KEY_ACTIVITI_SCHEMA_UPDATE, true);
        jdbcDriver = c.getString(KEY_JDBC_DRIVER, sqlConfig != null ? sqlConfig.getDriverClassName() : null);
        jdbcUser = c.getString(KEY_JDBC_USERNAME, sqlConfig != null ? sqlConfig.getJdbcUsername() : null);
        jdbcPassword = c.getString(KEY_JDBC_PASSWORD, sqlConfig != null ? sqlConfig.getJdbcPassword() : null);

        processCheckInterval = c.getInt(KEY_PROCESS_CHECK_INTERVAL, 10);    // todo set to bigger default for production use
        autoDeploymentFrom = c.getString(KEY_AUTO_DEPLOYMENT_FROM, AUTO_DEPLOYMENT_FROM_DEFAULT);
        allowApproveOthersItems = c.getBoolean(KEY_ALLOW_APPROVE_OTHERS_ITEMS, false);

        if (allowApproveOthersItems) {
            LOGGER.info("allowApproveOthersItems parameter is set to true, therefore authorized users CAN approve/reject work items assigned to other users.");
        }

//        hibernateDialect = sqlConfig != null ? sqlConfig.getHibernateDialect() : "";

        validate();
    }

    public void checkAllowedKeys(Configuration c, String[] knownKeys) {
        Set<String> knownKeysSet = new HashSet<String>(knownKeys.length);
        for (String key : knownKeys) {
            knownKeysSet.add(key);
        }

        Iterator<String> keyIterator = c.getKeys();
        while (keyIterator.hasNext()) {
            String keyName = keyIterator.next();
            if (!knownKeysSet.contains(keyName) && !knownKeysSet.contains(StringUtils.substringBefore(keyName, "."))) {
                throw new SystemException("Unknown key " + keyName + " in workflow configuration");
            }
        }
    }

    public Configuration getChangeProcessorsConfig() {
        Validate.notNull(midpointConfiguration, "midpointConfiguration was not initialized correctly (check spring beans initialization order)");
        return midpointConfiguration.getConfiguration(WF_CONFIG_SECTION).subset(CHANGE_PROCESSORS_SECTION); // via subset, because getConfiguration puts 'midpoint.home' property to the result
    }

    void validate() {

        notEmpty(jdbcDriver, "JDBC driver must be specified (either explicitly or in SQL repository configuration)");
        notEmpty(jdbcUrl, "JDBC URL must be specified (either explicitly or in SQL repository configuration).");
        notNull(jdbcUser, "JDBC user name must be specified (either explicitly or in SQL repository configuration).");
        notNull(jdbcPassword, "JDBC password must be specified (either explicitly or in SQL repository configuration).");
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

    public Boolean isEnabled() {
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

    public int getProcessCheckInterval() {
        return processCheckInterval;
    }

    public String getAutoDeploymentFrom() {
        return autoDeploymentFrom;
    }

    public boolean isAllowApproveOthersItems() {
        return allowApproveOthersItems;
    }

    public synchronized List<ChangeProcessor> getChangeProcessors() {
        if (changeProcessors != null) {
            return changeProcessors;
        }

        changeProcessors = new ArrayList<ChangeProcessor>();

        if (!enabled) {
            return changeProcessors;
        }

        List<String> changeProcessorNames = new ArrayList<String>();            // list - to preserve order

        Iterator<String> cpIterator = getChangeProcessorsConfig().getKeys();         // returns 'processor-name' but also 'other-processor-name.wrapper-name' etc.
        while (cpIterator.hasNext()) {
            String keyName = cpIterator.next();
            String processorName = StringUtils.substringBefore(keyName, ".");
            if (!changeProcessorNames.contains(processorName)) {
                changeProcessorNames.add(processorName);
            }
        }

        LOGGER.trace("Resolving change processors: {}", changeProcessorNames);

        for (String processorName : changeProcessorNames) {
            LOGGER.trace("Searching for change processor {}", processorName);
            try {
                ChangeProcessor processor = (ChangeProcessor) beanFactory.getBean(processorName);
                changeProcessors.add(processor);
            } catch(BeansException e) {
                throw new SystemException("Change processor " + processorName + " could not be found.", e);
            }
        }

        LOGGER.debug("Resolved " + changeProcessors.size() + " change processors.");
        return changeProcessors;
    }

    public ChangeProcessor findChangeProcessor(String processorClassName) {
        for (ChangeProcessor cp : getChangeProcessors()) {
            if (processorClassName.equals(cp.getClass().getName())) {
                return cp;
            }
        }

        throw new IllegalStateException("Change processor " + processorClassName + " is not registered.");
    }
}
