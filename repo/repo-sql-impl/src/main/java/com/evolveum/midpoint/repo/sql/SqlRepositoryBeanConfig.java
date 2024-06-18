/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.util.Properties;
import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.TransactionManager;

import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.util.EntityStateInterceptor;
import com.evolveum.midpoint.repo.sql.util.MidPointImplicitNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.MidPointPhysicalNamingStrategy;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.repo.sqlbase.SystemConfigurationChangeDispatcherImpl;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorsCollectionImpl;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;

/**
 * SQL repository related configuration from {@link DataSourceFactory} through ORM with
 * {@link TransactionManager} all the way to {@link SqlRepositoryServiceImpl}.
 * {@link ConditionalOnMissingBean} annotations are used to avoid duplicate bean acquirement that
 * would happen when combined with alternative configurations (e.g. context XMLs for test).
 * {@link ConditionalOnExpression} class annotation activates this configuration only if midpoint
 * {@code config.xml} specifies the repository factory class from SQL package.
 *
 * With current initialization not relying on system-init directly anymore, there is in fact
 * no "repository service factory" class and value of {@code com.evolveum.midpoint.repo.sql.}
 * for it is enough to initialize this SQL repository implementation.
 * Alternatively just value `sql` or `generic` (named used in docs) can be used.
 * Both values are now case-insensitive.
 *
 * Any of the values also work with alternative key element {@code type}.
 * The shortest form then looks like {@code <type>sql</type>}.
 */
@Configuration
@ConditionalOnExpression("#{midpointConfiguration.keyMatches("
        + "'midpoint.repository.repositoryServiceFactoryClass',"
        + " '(?i)com\\.evolveum\\.midpoint\\.repo\\.sql\\..*', '(?i)sql|generic')"
        + "|| midpointConfiguration.keyMatches("
        + "'midpoint.repository.type',"
        + " '(?i)com\\.evolveum\\.midpoint\\.repo\\.sql\\..*', '(?i)sql|generic')"
        + "}")
@ComponentScan
public class SqlRepositoryBeanConfig {

    @Bean
    @ConditionalOnMissingBean
    public SqlRepositoryConfiguration sqlRepositoryConfiguration(
            MidpointConfiguration midpointConfiguration) throws RepositoryServiceFactoryException {
        return new SqlRepositoryConfiguration(
                midpointConfiguration.getConfiguration(
                        MidpointConfiguration.REPOSITORY_CONFIGURATION))
                .validate();
    }

    @Bean
    public SqlEmbeddedRepository sqlEmbeddedRepository(
            SqlRepositoryConfiguration repositoryConfiguration) {
        return new SqlEmbeddedRepository(repositoryConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceFactory dataSourceFactory(
            // dependency in case we need to start H2 server
            @SuppressWarnings("unused") SqlEmbeddedRepository sqlEmbeddedRepository,
            SqlRepositoryConfiguration repositoryConfiguration) {
        return new DataSourceFactory(repositoryConfiguration);
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceFactory dataSourceFactory)
            throws RepositoryServiceFactoryException {
        return dataSourceFactory.createDataSource("mp-repo");
    }

    @Bean
    @ConditionalOnMissingBean
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(
            DataSource dataSource,
            SqlRepositoryConfiguration configuration) {

        LocalContainerEntityManagerFactoryBean bean = new LocalContainerEntityManagerFactoryBean();

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        bean.setJpaVendorAdapter(vendorAdapter);

        // While dataSource == dataSourceFactory.getDataSource(), we're using dataSource as
        // parameter to assure, that Spring already called the factory method. Explicit is good.
        bean.setDataSource(dataSource);

        Properties jpaProperties = new Properties();
        jpaProperties.setProperty(JdbcSettings.DIALECT, configuration.getHibernateDialect());
        jpaProperties.setProperty(SchemaToolingSettings.HBM2DDL_AUTO, configuration.getHibernateHbm2ddl());
        jpaProperties.setProperty("hibernate.id.new_generator_mappings", "true");
        jpaProperties.setProperty(BatchSettings.STATEMENT_BATCH_SIZE, "20");
        jpaProperties.setProperty(ValidationSettings.JAKARTA_VALIDATION_MODE, "none");
        jpaProperties.setProperty(TransactionSettings.TRANSACTION_COORDINATOR_STRATEGY, "jdbc");
        jpaProperties.setProperty("hibernate.hql.bulk_id_strategy",
                "org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy");
        jpaProperties.setProperty(SessionEventSettings.INTERCEPTOR, EntityStateInterceptor.class.getName());

        jpaProperties.setProperty(MappingSettings.IMPLICIT_NAMING_STRATEGY, MidPointImplicitNamingStrategy.class.getName());
        jpaProperties.setProperty(MappingSettings.PHYSICAL_NAMING_STRATEGY, MidPointPhysicalNamingStrategy.class.getName());

        bean.setJpaProperties(jpaProperties);
        bean.setPackagesToScan(
                "com.evolveum.midpoint.repo.sql.type",
                "com.evolveum.midpoint.repo.sql.data.common",
                "com.evolveum.midpoint.repo.sql.data.common.any",
                "com.evolveum.midpoint.repo.sql.data.common.container",
                "com.evolveum.midpoint.repo.sql.data.common.embedded",
                "com.evolveum.midpoint.repo.sql.data.common.enums",
                "com.evolveum.midpoint.repo.sql.data.common.id",
                "com.evolveum.midpoint.repo.sql.data.common.other",
                "com.evolveum.midpoint.repo.sql.data.common.type",
                "com.evolveum.midpoint.repo.sql.data.audit");

        return bean;
    }

    @Bean
    public TransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager jtm = new JpaTransactionManager(emf);
        jtm.setEntityManagerFactory(emf);

        return jtm;
    }

    @Bean
    public SqlRepositoryServiceImpl repositoryService(
            BaseHelper baseHelper,
            MatchingRuleRegistry matchingRuleRegistry,
            PrismContext prismContext,
            RelationRegistry relationRegistry) {

        return new SqlRepositoryServiceImpl(baseHelper, matchingRuleRegistry, prismContext, relationRegistry);
    }

    @Bean
    public ExtItemDictionary extItemDictionary() {
        return new ExtItemDictionary();
    }

    @Bean
    public AuditServiceFactory sqlAuditServiceFactory(
            BaseHelper defaultBaseHelper,
            SchemaService schemaService) {
        return new SqlAuditServiceFactory(defaultBaseHelper, schemaService);
    }

    // TODO it would be better to have dependencies explicit here, but there is cyclic one
    //  in SystemConfigurationChangeDispatcherImpl.
    @Bean
    public SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher() {
        return new SystemConfigurationChangeDispatcherImpl();
    }

    @Bean
    public SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection() {
        return new SqlPerformanceMonitorsCollectionImpl();
    }
}
