/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql;

import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.PROPERTY_DATASOURCE;
import static com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration.PROPERTY_JDBC_URL;

import java.sql.Connection;
import java.sql.Types;
import java.util.List;
import javax.sql.DataSource;

import com.google.common.base.Strings;
import com.querydsl.sql.ColumnMetadata;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.audit.api.AuditServiceFactoryException;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.audit.mapping.*;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sqlbase.*;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.querydsl.SqlLogger;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * {@link AuditServiceFactory} for {@link SqlAuditServiceImpl}, that is DB-based auditing.
 * <p>
 * This works only if legacy repository is used, which is handled by ConditionalOnExpression.
 * If this class is specified in config.xml as audit factory, without old repository it fails
 * because it will not be found as a bean.
 */
public class SqlAuditServiceFactory implements AuditServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqlAuditServiceFactory.class);

    private static final String CONF_AUDIT_SERVICE_COLUMNS = "customColumn";
    private static final String CONF_AUDIT_SERVICE_COLUMN_NAME = "columnName";
    private static final String CONF_AUDIT_SERVICE_EVENT_RECORD_PROPERTY_NAME = "eventRecordPropertyName";

    private final BaseHelper defaultBaseHelper;
    private final SchemaService schemaService;

    private SqlAuditServiceImpl auditService;

    public SqlAuditServiceFactory(
            BaseHelper defaultBaseHelper,
            SchemaService schemaService) {
        this.defaultBaseHelper = defaultBaseHelper;
        this.schemaService = schemaService;
    }

    @Override
    public synchronized void init(@NotNull Configuration configuration) throws AuditServiceFactoryException {
        LOGGER.info("Initializing Sql audit service factory.");
        try {
            SqlRepoContext sqlRepoContext = createSqlRepoContext(configuration);
            // base helper is only used for logging/exception handling, so the default one is OK
            auditService = new SqlAuditServiceImpl(defaultBaseHelper, sqlRepoContext, schemaService);
            initCustomColumns(configuration, sqlRepoContext);
        } catch (RepositoryServiceFactoryException ex) {
            throw new AuditServiceFactoryException(ex.getMessage(), ex);
        }
        LOGGER.info("SQL audit service factory initialization complete.");
    }

    private SqlRepoContext createSqlRepoContext(Configuration configuration)
            throws RepositoryServiceFactoryException {
        // one of these properties must be present to trigger separate audit datasource config
        if (configuration.getString(PROPERTY_JDBC_URL) == null
                && configuration.getString(PROPERTY_DATASOURCE) == null) {
            LOGGER.info("SQL audit service will use default repository configuration.");
            // NOTE: If default BaseHelper is used, it's used to configure PerformanceMonitor
            // in SqlBaseService. Perhaps the base class is useless and these factories can provide
            // PerformanceMonitor for the services.
            return createSqlRepoContext(
                    defaultBaseHelper.getConfiguration(),
                    defaultBaseHelper.dataSource(),
                    schemaService);
        }

        LOGGER.info("Configuring SQL audit service to use a different datasource");
        // SqlRepositoryConfiguration is used as it supports everything we need, BUT...
        // it also contains Hibernate dependencies that we DON'T want to use here.
        // We accept this "partial reuse" problem for the benefit of not needing another class.
        SqlRepositoryConfiguration config = new SqlRepositoryConfiguration(configuration);
        config.validate();

        DataSourceFactory dataSourceFactory = new DataSourceFactory(config);
        DataSource dataSource = dataSourceFactory.createDataSource("mp-audit");
        return createSqlRepoContext(config, dataSource, schemaService);
    }

    private SqlRepoContext createSqlRepoContext(
            JdbcRepositoryConfiguration config,
            DataSource dataSource,
            SchemaService schemaService) {
        QueryModelMappingRegistry mappingRegistry = new QueryModelMappingRegistry();
        SqlRepoContext repositoryContext =
                new SqlRepoContext(config, dataSource, schemaService, mappingRegistry);
        repositoryContext.setQuerydslSqlListener(new SqlLogger());

        // Registered mapping needs repository context which needs registry - but we fill it now:
        mappingRegistry
                .register(AuditEventRecordType.COMPLEX_TYPE,
                        QAuditEventRecordMapping.init(repositoryContext))
                .register(QAuditItemMapping.init(repositoryContext))
                .register(QAuditPropertyValueMapping.init(repositoryContext))
                .register(QAuditRefValueMapping.init(repositoryContext))
                .register(QAuditResourceMapping.init(repositoryContext))
                .register(QAuditDeltaMapping.init(repositoryContext))
                .seal();

        return repositoryContext;
    }

    private void initCustomColumns(
            @NotNull Configuration configuration, SqlRepoContext sqlRepoContext) {
        List<HierarchicalConfiguration<ImmutableNode>> subConfigColumns =
                ((BaseHierarchicalConfiguration) configuration)
                        .configurationsAt(CONF_AUDIT_SERVICE_COLUMNS);

        // here we use config from context, it can be main repository configuration
        SqlRepositoryConfiguration repoConfig =
                (SqlRepositoryConfiguration) sqlRepoContext.getJdbcRepositoryConfiguration();
        SqlTableMetadata tableMetadata = null;
        if (repoConfig.isCreateMissingCustomColumns()) {
            try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
                tableMetadata = SqlTableMetadata.create(
                        jdbcSession.connection(), QAuditEventRecord.TABLE_NAME);
            }
        }

        for (Configuration subConfigColumn : subConfigColumns) {
            String columnName = getStringFromConfig(
                    subConfigColumn, CONF_AUDIT_SERVICE_COLUMN_NAME);
            String propertyName = getStringFromConfig(
                    subConfigColumn, CONF_AUDIT_SERVICE_EVENT_RECORD_PROPERTY_NAME);
            // No type definition for now, it's all String or String implicit conversion.

            ColumnMetadata columnMetadata =
                    ColumnMetadata.named(columnName).ofType(Types.NVARCHAR).withSize(255);
            QAuditEventRecordMapping.get().addExtensionColumn(propertyName, columnMetadata);
            if (tableMetadata != null && tableMetadata.get(columnName) == null) {
                // Fails on SQL Server with snapshot transaction, so different isolation is used.
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession()
                        .startTransaction(Connection.TRANSACTION_READ_COMMITTED)) {
                    jdbcSession.addColumn(QAuditEventRecord.TABLE_NAME,
                            ColumnMetadata.named(columnName).ofType(Types.VARCHAR).withSize(255));
                    jdbcSession.commit();
                }
            }
        }
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
    public SqlAuditServiceImpl createAuditService() {
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
