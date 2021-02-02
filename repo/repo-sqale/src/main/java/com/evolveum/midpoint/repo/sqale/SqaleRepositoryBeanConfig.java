/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.sql.DataSource;

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.sqale.qmodel.QDashboardMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.QObjectCollectionMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorHostMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTableRowMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.node.QNodeMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReportMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReportOutputMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRoleMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QArchetypeMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QRoleMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QServiceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSecurityPolicyMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSystemConfigurationMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QValuePolicyMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTaskMapping;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SystemConfigurationChangeDispatcherImpl;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorsCollectionImpl;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * New SQL repository related configuration.
 * {@link ConditionalOnMissingBean} annotations are used to avoid duplicate bean acquirement that
 * would happen when combined with alternative configurations (e.g. context XMLs for test).
 * {@link ConditionalOnExpression} class annotation activates this configuration only if midpoint
 * {@code config.xml} specifies the repository factory class from SQL package.
 * <p>
 * To choose this "new SQL" repository set {@code repositoryServiceFactoryClass} to a value starting
 * with (or equal to) {@code com.evolveum.midpoint.repo.sqale.} (including the dot at the end).
 * Alternatively simple {@code sqale} or {@code scale} will work too.
 * All values are case-insensitive.
 */
@Configuration
@ConditionalOnExpression("#{midpointConfiguration.keyMatches("
        + "'midpoint.repository.repositoryServiceFactoryClass',"
        + " '(?i)com\\.evolveum\\.midpoint\\.repo\\.sqale\\..*', '(?i)s[qc]ale')}")
@ComponentScan
public class SqaleRepositoryBeanConfig {

    @Bean
    public SqaleRepositoryConfiguration sqaleRepositoryConfiguration(
            MidpointConfiguration midpointConfiguration) throws RepositoryServiceFactoryException {
        // TODO remove logging change, when better way to do it for initial start is found
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.querydsl.sql")).setLevel(Level.DEBUG);
        // PG logs too much on TRACE or not enough on DEBUG, not useful in the main log
//        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.postgresql")).setLevel(Level.TRACE);

        return new SqaleRepositoryConfiguration(
                midpointConfiguration.getConfiguration(
                        MidpointConfiguration.REPOSITORY_CONFIGURATION))
                .validate();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceFactory dataSourceFactory(
            SqaleRepositoryConfiguration repositoryConfiguration) {
        return new DataSourceFactory(repositoryConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceFactory dataSourceFactory)
            throws RepositoryServiceFactoryException {
        return dataSourceFactory.createDataSource();
    }

    @Bean
    public SqaleRepoContext sqlRepoContext(
            SqaleRepositoryConfiguration repositoryConfiguration,
            DataSource dataSource) {
        QueryModelMappingRegistry mappingRegistry = new QueryModelMappingRegistry()
                // ordered alphabetically here
                .register(AbstractRoleType.COMPLEX_TYPE, QAbstractRoleMapping.INSTANCE)
                .register(ArchetypeType.COMPLEX_TYPE, QArchetypeMapping.INSTANCE)
                .register(DashboardType.COMPLEX_TYPE, QDashboardMapping.INSTANCE)
                .register(FocusType.COMPLEX_TYPE, QFocusMapping.INSTANCE)
                .register(ConnectorType.COMPLEX_TYPE, QConnectorMapping.INSTANCE)
                .register(ConnectorHostType.COMPLEX_TYPE, QConnectorHostMapping.INSTANCE)
                .register(LookupTableType.COMPLEX_TYPE, QLookupTableMapping.INSTANCE)
                .register(LookupTableRowType.COMPLEX_TYPE, QLookupTableRowMapping.INSTANCE)
                .register(NodeType.COMPLEX_TYPE, QNodeMapping.INSTANCE)
                .register(ObjectType.COMPLEX_TYPE, QObjectMapping.INSTANCE)
                .register(ObjectCollectionType.COMPLEX_TYPE, QObjectCollectionMapping.INSTANCE)
                .register(ReportType.COMPLEX_TYPE, QReportMapping.INSTANCE)
                .register(ReportDataType.COMPLEX_TYPE, QReportOutputMapping.INSTANCE)
                .register(RoleType.COMPLEX_TYPE, QRoleMapping.INSTANCE)
                .register(SecurityPolicyType.COMPLEX_TYPE, QSecurityPolicyMapping.INSTANCE)
                .register(ServiceType.COMPLEX_TYPE, QServiceMapping.INSTANCE)
                .register(SystemConfigurationType.COMPLEX_TYPE, QSystemConfigurationMapping.INSTANCE)
                .register(TaskType.COMPLEX_TYPE, QTaskMapping.INSTANCE)
                .register(UserType.COMPLEX_TYPE, QUserMapping.INSTANCE)
                .register(ValuePolicyType.COMPLEX_TYPE, QValuePolicyMapping.INSTANCE)
                .seal();

        return new SqaleRepoContext(repositoryConfiguration, dataSource, mappingRegistry);
    }

    @Bean
    public SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection() {
        return new SqlPerformanceMonitorsCollectionImpl();
    }

    @Bean
    public SqaleRepositoryService repositoryService(
            SqlRepoContext sqlRepoContext,
            SchemaHelper schemaService,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        return new SqaleRepositoryService(
                sqlRepoContext,
                schemaService,
                sqlPerformanceMonitorsCollection);
    }

    // TODO @Bean for AuditServiceFactory later

    // TODO rethink? using Spring events
    @Bean
    public SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher() {
        return new SystemConfigurationChangeDispatcherImpl();
    }
}
