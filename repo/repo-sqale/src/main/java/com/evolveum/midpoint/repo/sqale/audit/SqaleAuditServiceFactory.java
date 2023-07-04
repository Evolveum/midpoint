/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit;

import static com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration.*;

import java.sql.Types;
import java.util.List;
import javax.sql.DataSource;

import com.evolveum.midpoint.repo.sqale.SqaleUtils;

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
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryBeanConfig;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditDeltaMapping;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecordMapping;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditRefValueMapping;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTableMetadata;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.querydsl.SqlLogger;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * {@link AuditServiceFactory} for {@link SqaleAuditService}, that is DB-based auditing.
 * <p>
 * This works only with new (`sqale`) repository, which is handled by condition in {@link SqaleRepositoryBeanConfig}.
 * If this class is specified in `config.xml` as audit factory without new repository,
 * it fails because it will not be found as a bean.
 */
public class SqaleAuditServiceFactory implements AuditServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqaleAuditServiceFactory.class);

    private static final String CONF_AUDIT_SERVICE_COLUMNS = "customColumn";
    private static final String CONF_AUDIT_SERVICE_COLUMN_NAME = "columnName";
    private static final String CONF_AUDIT_SERVICE_EVENT_RECORD_PROPERTY_NAME = "eventRecordPropertyName";

    private final SqaleRepositoryConfiguration sqaleRepositoryConfiguration;
    private final SchemaService schemaService;
    private final DataSource repositoryDataSource;
    private final SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection;

    private SqaleAuditService auditService;

    public SqaleAuditServiceFactory(
            SqaleRepositoryConfiguration sqaleRepositoryConfiguration,
            SchemaService schemaService,
            DataSource repositoryDataSource,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        this.sqaleRepositoryConfiguration = sqaleRepositoryConfiguration;
        this.schemaService = schemaService;
        this.repositoryDataSource = repositoryDataSource;
        this.sqlPerformanceMonitorsCollection = sqlPerformanceMonitorsCollection;
    }

    @Override
    public synchronized void init(@NotNull Configuration configuration) throws AuditServiceFactoryException {
        LOGGER.info("Initializing SQL audit service factory.");
        try {
            SqaleRepoContext sqlRepoContext = createSqaleRepoContext(configuration);
            auditService = new SqaleAuditService(sqlRepoContext, sqlPerformanceMonitorsCollection);
            initCustomColumns(configuration, sqlRepoContext);
        } catch (RepositoryServiceFactoryException ex) {
            throw new AuditServiceFactoryException(ex.getMessage(), ex);
        }
        LOGGER.info("SQL audit service factory initialization complete.");
    }

    private SqaleRepoContext createSqaleRepoContext(Configuration configuration)
            throws RepositoryServiceFactoryException {
        // one of these properties must be present to trigger separate audit datasource config
        if (configuration.getString(PROPERTY_JDBC_URL) == null
                && configuration.getString(PROPERTY_DATASOURCE) == null) {
            LOGGER.info("SQL audit service will use default repository configuration.");
            return createSqaleRepoContext(
                    SqaleRepositoryConfiguration.initForAudit(sqaleRepositoryConfiguration, configuration),
                    repositoryDataSource,
                    schemaService);
        }

        LOGGER.info("Configuring SQL audit service to use a different datasource");
        SqaleRepositoryConfiguration config = new SqaleRepositoryConfiguration(configuration);
        config.init(); // normally Spring calls this, but for audit this is unmanaged bean

        DataSourceFactory dataSourceFactory = new DataSourceFactory(config);
        DataSource dataSource = dataSourceFactory.createDataSource("mp-audit");
        return createSqaleRepoContext(config, dataSource, schemaService);
    }

    private SqaleRepoContext createSqaleRepoContext(
            SqaleRepositoryConfiguration config,
            DataSource dataSource,
            SchemaService schemaService) {
        QueryModelMappingRegistry mappingRegistry = new QueryModelMappingRegistry();
        SqaleRepoContext repositoryContext =
                new SqaleRepoContext(config, dataSource, schemaService, mappingRegistry,
                        SqaleUtils.SCHEMA_AUDIT_CHANGE_NUMBER, SqaleUtils.CURRENT_SCHEMA_AUDIT_CHANGE_NUMBER);
        repositoryContext.setQuerydslSqlListener(new SqlLogger(config.getSqlDurationWarningMs()));

        // Registered mapping needs repository context which needs registry - now we have both:
        mappingRegistry
                .register(AuditEventRecordType.COMPLEX_TYPE,
                        QAuditEventRecordMapping.init(repositoryContext))
                .register(QAuditRefValueMapping.init(repositoryContext))
                .register(QAuditDeltaMapping.init(repositoryContext))
                .seal();

        return repositoryContext;
    }

    private void initCustomColumns(
            @NotNull Configuration configuration, SqaleRepoContext sqlRepoContext) {
        List<HierarchicalConfiguration<ImmutableNode>> subConfigColumns =
                ((BaseHierarchicalConfiguration) configuration)
                        .configurationsAt(CONF_AUDIT_SERVICE_COLUMNS);

        // here we use config from context, it can be main repository configuration
        SqaleRepositoryConfiguration repoConfig =
                (SqaleRepositoryConfiguration) sqlRepoContext.getJdbcRepositoryConfiguration();
        boolean createMissing = repoConfig.isCreateMissingCustomColumns()
                // but we'll consider the flag also on audit configuration, just in case
                || configuration.getBoolean(PROPERTY_CREATE_MISSING_CUSTOM_COLUMNS, false);
        SqlTableMetadata tableMetadata = null;
        if (createMissing) {
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

            ColumnMetadata columnMetadata = ColumnMetadata.named(columnName).ofType(Types.VARCHAR);
            QAuditEventRecordMapping.get().addExtensionColumn(propertyName, columnMetadata);
            if (tableMetadata != null && tableMetadata.get(columnName) == null) {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
                    jdbcSession.addColumn(QAuditEventRecord.TABLE_NAME, columnMetadata);
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
    public SqaleAuditService createAuditService() {
        // Just returns pre-created instance from init, it's not such a sin.
        // Still the method is named "create*" because it's a factory method on a factory bean.
        return auditService;
    }

    @Override
    public synchronized void destroy() {
        LOGGER.info("Destroying SQL audit service factory.");
        auditService.destroy();
        LOGGER.info("SQL audit service factory destroy complete.");
    }
}
