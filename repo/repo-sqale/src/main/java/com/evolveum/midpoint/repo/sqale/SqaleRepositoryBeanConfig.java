/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import javax.sql.DataSource;

import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorDevelopmentMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster.QClusterDetectedPatternMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier.QOutlierMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.outlier.QOutlierPartitionMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.*;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditServiceFactory;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.QAccessCertificationCampaignMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.QAccessCertificationCaseMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.QAccessCertificationDefinitionMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.accesscert.QAccessCertificationWorkItemMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignmentMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.QCaseMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem.QCaseWorkItemMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorHostMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusIdentityMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QGenericObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTableRowMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster.QClusterObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.mining.session.QSessionObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.node.QNodeMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.notification.QMessageTemplateMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QOperationExecutionMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QTriggerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.other.*;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReportDataMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.report.QReportMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadowMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.simulation.QProcessedObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.simulation.QSimulationResultMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSecurityPolicyMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QSystemConfigurationMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.system.QValuePolicyMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.tag.QMarkMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QAffectedObjectsMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTaskMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.schema.QSchemaMapping;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.repo.sqlbase.SystemConfigurationChangeDispatcherImpl;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryModelMappingRegistry;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorsCollectionImpl;
import com.evolveum.midpoint.repo.sqlbase.querydsl.SqlLogger;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * New SQL repository related configuration.
 * {@link ConditionalOnMissingBean} annotations are used to avoid duplicate bean acquirement that
 * would happen when combined with alternative configurations (e.g. context XMLs for test).
 * {@link ConditionalOnExpression} class annotation activates this configuration only if midpoint
 * {@code config.xml} specifies the repository factory class from SQL package.
 *
 * To choose this "new SQL" repository set `type` element under `repository` to contain either
 * `sqale`, `scale` or `native` (name used in docs).
 * All values are case-insensitive.
 */
@Configuration
@ConditionalOnExpression(
        "#{midpointConfiguration.keyMatches('midpoint.repository.type', '(?i)s[qc]ale|native')}")
@ComponentScan
public class SqaleRepositoryBeanConfig {

    @Bean
    public SqaleRepositoryConfiguration sqaleRepositoryConfiguration(
            MidpointConfiguration midpointConfiguration) {
        return new SqaleRepositoryConfiguration(
                midpointConfiguration.getConfiguration(
                        MidpointConfiguration.REPOSITORY_CONFIGURATION));
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
        return dataSourceFactory.createDataSource("mp-repo");
    }

    @Bean
    public SqaleRepoContext sqlRepoContext(
            SqaleRepositoryConfiguration repositoryConfiguration,
            SchemaService schemaService,
            DataSource dataSource) {
        QueryModelMappingRegistry mappingRegistry = new QueryModelMappingRegistry();
        SqaleRepoContext repositoryContext = new SqaleRepoContext(
                repositoryConfiguration, dataSource, schemaService, mappingRegistry,
                SqaleUtils.SCHEMA_CHANGE_NUMBER, SqaleUtils.CURRENT_SCHEMA_CHANGE_NUMBER);

        // logger on com.evolveum.midpoint.repo.sqlbase.querydsl.SqlLogger
        // DEBUG = show query, TRACE = add parameter values too (bindings)
        repositoryContext.setQuerydslSqlListener(
                new SqlLogger(repositoryConfiguration.getSqlDurationWarningMs()));

        // Registered mapping needs repository context which needs registry. Now we can fill it.
        // Mappings are ordered alphabetically here, mappings without schema type are at the end.

        mappingRegistry
                .register(AbstractRoleType.COMPLEX_TYPE,
                        QAbstractRoleMapping.initAbstractRoleMapping(repositoryContext))
                .register(AccessCertificationCampaignType.COMPLEX_TYPE,
                        QAccessCertificationCampaignMapping
                                .initAccessCertificationCampaignMapping(repositoryContext))
                .register(AccessCertificationCaseType.COMPLEX_TYPE,
                        QAccessCertificationCaseMapping
                                .initAccessCertificationCaseMapping(repositoryContext))
                .register(AccessCertificationDefinitionType.COMPLEX_TYPE,
                        QAccessCertificationDefinitionMapping.init(repositoryContext))
                .register(AccessCertificationWorkItemType.COMPLEX_TYPE,
                        QAccessCertificationWorkItemMapping.init(repositoryContext))
                .register(ArchetypeType.COMPLEX_TYPE,
                        QArchetypeMapping.initArchetypeMapping(repositoryContext))
                .register(AssignmentHolderType.COMPLEX_TYPE,
                        QAssignmentHolderMapping.initAssignmentHolderMapping(repositoryContext))
                .register(AssignmentType.COMPLEX_TYPE,
                        QAssignmentMapping.initAssignmentMapping(repositoryContext))
                .register(CaseType.COMPLEX_TYPE, QCaseMapping.initCaseMapping(repositoryContext))
                .register(CaseWorkItemType.COMPLEX_TYPE,
                        QCaseWorkItemMapping.initCaseWorkItemMapping(repositoryContext))
                .register(DashboardType.COMPLEX_TYPE, QDashboardMapping.init(repositoryContext))
                .register(FocusType.COMPLEX_TYPE, QFocusMapping.initFocusMapping(repositoryContext))
                .register(FocusIdentityType.COMPLEX_TYPE, QFocusIdentityMapping.init(repositoryContext))
                .register(FormType.COMPLEX_TYPE, QFormMapping.init(repositoryContext))
                .register(FunctionLibraryType.COMPLEX_TYPE,
                        QFunctionLibraryMapping.init(repositoryContext))
                .register(ConnectorType.COMPLEX_TYPE, QConnectorMapping.init(repositoryContext))
                .register(ConnectorHostType.COMPLEX_TYPE,
                        QConnectorHostMapping.init(repositoryContext))
                .register(GenericObjectType.COMPLEX_TYPE,
                        QGenericObjectMapping.init(repositoryContext))
                .register(LookupTableType.COMPLEX_TYPE, QLookupTableMapping.init(repositoryContext))
                .register(LookupTableRowType.COMPLEX_TYPE,
                        QLookupTableRowMapping.init(repositoryContext))
                .register(MessageTemplateType.COMPLEX_TYPE, QMessageTemplateMapping.init(repositoryContext))
                .register(NodeType.COMPLEX_TYPE, QNodeMapping.init(repositoryContext))
                .register(ObjectType.COMPLEX_TYPE,
                        QObjectMapping.initObjectMapping(repositoryContext))
                .register(ObjectCollectionType.COMPLEX_TYPE,
                        QObjectCollectionMapping.init(repositoryContext))
                .register(ObjectTemplateType.COMPLEX_TYPE,
                        QObjectTemplateMapping.initObjectTemplateMapping(repositoryContext))
                .register(OperationExecutionType.COMPLEX_TYPE,
                        QOperationExecutionMapping.init(repositoryContext))
                .register(OrgType.COMPLEX_TYPE, QOrgMapping.initOrgMapping(repositoryContext))
                .register(ReportType.COMPLEX_TYPE, QReportMapping.init(repositoryContext))
                .register(ReportDataType.COMPLEX_TYPE, QReportDataMapping.init(repositoryContext))
                .register(RoleAnalysisClusterType.COMPLEX_TYPE, QClusterObjectMapping.init(repositoryContext))
                .register(RoleAnalysisSessionType.COMPLEX_TYPE, QSessionObjectMapping.init(repositoryContext))
                .register(RoleAnalysisOutlierType.COMPLEX_TYPE, QOutlierMapping.init(repositoryContext))
                .register(ResourceType.COMPLEX_TYPE, QResourceMapping.init(repositoryContext))
                .register(RoleType.COMPLEX_TYPE, QRoleMapping.init(repositoryContext))
                .register(SecurityPolicyType.COMPLEX_TYPE,
                        QSecurityPolicyMapping.init(repositoryContext))
                .register(SequenceType.COMPLEX_TYPE, QSequenceMapping.init(repositoryContext))
                .register(ServiceType.COMPLEX_TYPE, QServiceMapping.init(repositoryContext))
                .register(ShadowType.COMPLEX_TYPE,
                        QShadowMapping.initShadowMapping(repositoryContext))
                .register(SystemConfigurationType.COMPLEX_TYPE,
                        QSystemConfigurationMapping.init(repositoryContext))
                .register(TaskType.COMPLEX_TYPE, QTaskMapping.init(repositoryContext))
                .register(TriggerType.COMPLEX_TYPE, QTriggerMapping.init(repositoryContext))
                .register(UserType.COMPLEX_TYPE, QUserMapping.initUserMapping(repositoryContext))
                .register(ValuePolicyType.COMPLEX_TYPE, QValuePolicyMapping.init(repositoryContext))
                .register(QContainerMapping.initContainerMapping(repositoryContext))
                .register(QReferenceMapping.init(repositoryContext))
                .register(SimulationResultType.COMPLEX_TYPE, QSimulationResultMapping.initSimulationResultMapping(repositoryContext))
                .register(SimulationResultProcessedObjectType.COMPLEX_TYPE, QProcessedObjectMapping.initProcessedResultMapping(repositoryContext))
                .register(MarkType.COMPLEX_TYPE, QMarkMapping.init(repositoryContext))
                .register(SchemaType.COMPLEX_TYPE, QSchemaMapping.init(repositoryContext))
                .register(QAffectedObjectsMapping.init(repositoryContext))
                .register(RoleAnalysisOutlierPartitionType.COMPLEX_TYPE, QOutlierPartitionMapping.initMapping(repositoryContext))
                .register(RoleAnalysisDetectionPatternType.COMPLEX_TYPE, QClusterDetectedPatternMapping.initMapping(repositoryContext))
                .register(PolicyType.COMPLEX_TYPE, QPolicyMapping.init(repositoryContext))
                .register(ApplicationType.COMPLEX_TYPE, QApplicationMapping.init(repositoryContext))
                .register(ConnectorDevelopmentType.COMPLEX_TYPE, QConnectorDevelopmentMapping.init(repositoryContext))
                .seal();

        return repositoryContext;
    }

    @Bean
    public SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection() {
        return new SqlPerformanceMonitorsCollectionImpl();
    }

    @Bean
    public SqaleRepositoryService repositoryService(
            SqaleRepoContext sqlRepoContext,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        return new SqaleRepositoryService(
                sqlRepoContext,
                sqlPerformanceMonitorsCollection);
    }

    @Bean
    public AuditServiceFactory sqlAuditServiceFactory(
            SqaleRepositoryConfiguration sqaleRepositoryConfiguration,
            SchemaService schemaService,
            DataSource dataSource,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        return new SqaleAuditServiceFactory(
                sqaleRepositoryConfiguration,
                schemaService,
                dataSource,
                sqlPerformanceMonitorsCollection);
    }

    // TODO rethink? using Spring events
    @Bean
    public SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher() {
        return new SystemConfigurationChangeDispatcherImpl();
    }
}
