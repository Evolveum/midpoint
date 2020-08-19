/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.PROPERTY_DATASOURCE;
import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.PROPERTY_JDBC_URL;

import java.io.IOException;
import java.sql.Types;
import java.util.List;
import javax.sql.DataSource;

import com.google.common.base.Strings;
import com.querydsl.sql.ColumnMetadata;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.audit.api.AuditServiceFactoryException;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.helpers.JdbcSession;
import com.evolveum.midpoint.repo.sql.pure.SqlTableMetadata;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sql.util.EntityStateInterceptor;
import com.evolveum.midpoint.repo.sql.util.MidPointImplicitNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.MidPointPhysicalNamingStrategy;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * {@link AuditServiceFactory} for {@link SqlAuditServiceImpl}, that is DB-based auditing.
 */
public class SqlAuditServiceFactory implements AuditServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceFactory.class);

    private static final String CONF_AUDIT_SERVICE_COLUMNS = "customColumn";
    private static final String CONF_AUDIT_SERVICE_COLUMN_NAME = "columnName";
    private static final String CONF_AUDIT_SERVICE_EVENT_RECORD_PROPERTY_NAME = "eventRecordPropertyName";

    @Autowired private BaseHelper defaultBaseHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private MidPointImplicitNamingStrategy midPointImplicitNamingStrategy;
    @Autowired private MidPointPhysicalNamingStrategy midPointPhysicalNamingStrategy;
    @Autowired private EntityStateInterceptor entityStateInterceptor;

    private SqlAuditServiceImpl auditService;

    @Override
    public synchronized void init(@NotNull Configuration configuration) throws AuditServiceFactoryException {
        LOGGER.info("Initializing Sql audit service factory.");
        try {
            BaseHelper baseHelper = configureBaseHelper(configuration);

            auditService = new SqlAuditServiceImpl(baseHelper, prismContext);
            initCustomColumns(configuration, baseHelper);
        } catch (RepositoryServiceFactoryException ex) {
            throw new AuditServiceFactoryException(ex.getMessage(), ex);
        }
        LOGGER.info("SQL audit service factory initialization complete.");
    }

    private void initCustomColumns(
            @NotNull Configuration configuration, @NotNull BaseHelper baseHelper) {
        List<HierarchicalConfiguration<ImmutableNode>> subConfigColumns =
                ((BaseHierarchicalConfiguration) configuration)
                        .configurationsAt(CONF_AUDIT_SERVICE_COLUMNS);

        boolean createMissing = baseHelper.getConfiguration().isCreateMissingCustomColumns();
        SqlTableMetadata tableMetadata = null;
        if (createMissing) {
            try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startReadOnlyTransaction()) {
                tableMetadata = SqlTableMetadata.create(
                        jdbcSession.connection(), QAuditEventRecord.TABLE_NAME);
            }
        }

        for (Configuration subConfigColumn : subConfigColumns) {
            String columnName = getStringFromConfig(
                    subConfigColumn, CONF_AUDIT_SERVICE_COLUMN_NAME);
            String eventRecordPropertyName = getStringFromConfig(
                    subConfigColumn, CONF_AUDIT_SERVICE_EVENT_RECORD_PROPERTY_NAME);
            // No type definition for now, it's all String or String implicit conversion.

            auditService.addCustomColumn(eventRecordPropertyName, columnName);
            if (tableMetadata != null && tableMetadata.get(columnName) == null) {
                try (JdbcSession jdbcSession = baseHelper.newJdbcSession().startTransaction()) {
                    jdbcSession.addColumn(QAuditEventRecord.TABLE_NAME,
                            ColumnMetadata.named(columnName).ofType(Types.VARCHAR).withSize(255));
                }
            }
        }
    }

    private BaseHelper configureBaseHelper(Configuration configuration)
            throws RepositoryServiceFactoryException {
        // one of these properties must be present to trigger separate audit datasource config
        if (configuration.getString(PROPERTY_JDBC_URL) == null
                && configuration.getString(PROPERTY_DATASOURCE) == null) {
            LOGGER.info("SQL audit service will use default repository configuration.");
            // NOTE: If default BaseHelper is used, it's used to configure PerformanceMonitor
            // in SqlBaseService. Perhaps the base class is useless and these factories can provide
            // PerformanceMonitor for the services.
            return defaultBaseHelper;
        }

        LOGGER.info("Configuring SQL audit service to use a different datasource");
        // SqlRepositoryConfiguration is used as it supports everything we need, BUT...
        // it also contains Hibernate dependencies that we DON'T want to use here.
        // We accept this "partial reuse" problem for the benefit of not needing another class.
        SqlRepositoryConfiguration config = new SqlRepositoryConfiguration(configuration);
        config.validate();

        DataSourceFactory dataSourceFactory = new DataSourceFactory(config);
        DataSource dataSource = dataSourceFactory.createDataSource();
        // Spring config class is used with all its configuration code, but this time, we're NOT
        // creating managed beans - we want to create alternative non-managed BaseHelper.
        SqlRepositoryBeanConfig beanConfig = new SqlRepositoryBeanConfig();
        LocalSessionFactoryBean sessionFactoryBean = beanConfig.sessionFactory(
                dataSource, dataSourceFactory, midPointImplicitNamingStrategy,
                midPointPhysicalNamingStrategy, entityStateInterceptor);
        // we don't want to check all the entities, only audit ones
        sessionFactoryBean.setPackagesToScan("com.evolveum.midpoint.repo.sql.data.audit");
        try {
            sessionFactoryBean.afterPropertiesSet();
        } catch (IOException e) {
            throw new RepositoryServiceFactoryException(e);
        }
        SessionFactory sessionFactory = sessionFactoryBean.getObject();
        return new BaseHelper(config, sessionFactory, dataSource);
    }

    private String getStringFromConfig(Configuration config, String key) {
        String value = config.getString(key);
        if (Strings.isNullOrEmpty(value)) {
            LOGGER.error("Property with key ({}) not found in configuration. " +
                    "Provided configuration:\n{}", key, config);
            throw new SystemException("Property with key (" + key
                    + ") not found in configuration. Provided configuration:\n"
                    + config);
        }

        return value;
    }

    @Override
    public AuditService createAuditService() {
        // Just returns pre-created instance from init, it's not such a sin.
        // Still the method is named "create*" because it's a factory method on a factory bean.
        return auditService;
    }

    @Override
    public synchronized void destroy() {
        LOGGER.info("Destroying Sql audit service factory.");
        auditService.destroy();
        LOGGER.info("Sql audit service factory destroy complete.");
    }
}
