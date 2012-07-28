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
package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.task.api.TaskManagerConfigurationException;
import com.evolveum.midpoint.task.api.UseThreadInterrupt;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author Pavol Mederly
 */
public class WfConfiguration {

    private static final transient Trace LOGGER = TraceManager.getTrace(WfConfiguration.class);

    private static final String WF_CONFIG_SECTION = "midpoint.workflow";

    private boolean enabled;

    private boolean activitiSchemaUpdate;

    private String database;
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;

    private String hibernateDialect;
    private boolean databaseIsEmbedded;
    private int processCheckInterval;

    void initialize(MidpointConfiguration masterConfig, SqlRepositoryConfiguration sqlConfig) {

        Configuration c = masterConfig.getConfiguration(WF_CONFIG_SECTION);

        enabled = c.getBoolean("enabled", false);
        activitiSchemaUpdate = c.getBoolean("activitiSchemaUpdate", false);
        jdbcDriver = c.getString("jdbcDriver", sqlConfig != null ? sqlConfig.getDriverClassName() : null);
        jdbcUrl = c.getString("jdbcUrl", sqlConfig != null ? sqlConfig.getJdbcUrl() : null);
        jdbcUser = c.getString("jdbcUser", sqlConfig != null ? sqlConfig.getJdbcUsername() : null);
        jdbcPassword = c.getString("jdbcPassword", sqlConfig != null ? sqlConfig.getJdbcPassword() : null);

        processCheckInterval = c.getInt("processCheckInterval", 30);

        hibernateDialect = sqlConfig != null ? sqlConfig.getHibernateDialect() : "";

//        String defaultSqlSchemaFile = schemas.get(hibernateDialect);
//        String defaultDriverDelegate = delegates.get(hibernateDialect);

//        sqlSchemaFile = c.getString(SQL_SCHEMA_FILE_CONFIG_ENTRY, defaultSqlSchemaFile);
//        jdbcDriverDelegateClass = c.getString(JDBC_DRIVER_DELEGATE_CLASS_CONFIG_ENTRY, defaultDriverDelegate);

        validate();
    }

    void validate() {

        notEmpty(jdbcDriver, "JDBC driver must be specified (either explicitly or in SQL repository configuration)");
        notEmpty(jdbcUrl, "JDBC URL must be specified (either explicitly or in SQL repository configuration).");
        notNull(jdbcUser, "JDBC user name must be specified (either explicitly or in SQL repository configuration).");
        notNull(jdbcPassword, "JDBC password must be specified (either explicitly or in SQL repository configuration).");
//        notEmpty(jdbcDriverDelegateClass, "JDBC driver delegate class must be specified (either explicitly or through one of supported Hibernate dialects).");
//        notEmpty(sqlSchemaFile, "SQL schema file must be specified (either explicitly or through one of supported Hibernate dialects).");
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

    private void mustBeTrue(boolean condition, String message) {
        if (!condition) {
            throw new SystemException(message);
        }
    }

    private void mustBeFalse(boolean condition, String message) {
        if (condition) {
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

    public int getProcessCheckInterval() {
        return processCheckInterval;
    }
}
