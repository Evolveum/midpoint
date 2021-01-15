/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.TransactionManager;

import com.evolveum.midpoint.audit.api.AuditServiceFactory;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sql.util.EntityStateInterceptor;
import com.evolveum.midpoint.repo.sql.util.MidPointImplicitNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.MidPointPhysicalNamingStrategy;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.repo.sqlbase.SystemConfigurationChangeDispatcherImpl;

/**
 * SQL repository related configuration from {@link DataSourceFactory} through ORM all the way to
 * {@link TransactionManager}.
 * {@link ConditionalOnMissingBean} annotations are used to avoid duplicate bean acquirement that
 * would happen when combined with alternative configurations (e.g. context XMLs for test).
 * {@link ConditionalOnExpression} class annotation activates this configuration only if midpoint
 * {@code config.xml} specifies the repository factory class from SQL package.
 * <p>
 * Spring configuration note - ConditionalOnExpression is ugly, but the following does NOT work:
 * <ul>
 * <li>{@code @ConditionalOnBean(SqlRepositoryFactory.class)} - with {@code RepositoryServiceFactory}
 * it does, but that does not help.</li>
 * <li>{@code @ConditionalOnExpression("#{repositoryFactory...} - because {@code RepositoryFactory}
 * is not initialized yet and all injected stuff is still {@code null}.</li>
 * </ul>
 */
@Configuration
@ConditionalOnExpression("#{midpointConfiguration.getConfiguration('midpoint.repository')"
        + ".getString('repositoryServiceFactoryClass').startsWith('com.evolveum.midpoint.repo.sql.')}")
@ComponentScan
public class SqlRepositoryBeanConfig {

    @Bean
    public ExtItemDictionary extItemDictionary() {
        return new ExtItemDictionary();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceFactory dataSourceFactory(SqlRepositoryFactory sqlRepositoryFactory) {
        return new DataSourceFactory(sqlRepositoryFactory.getConfiguration());
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceFactory dataSourceFactory)
            throws RepositoryServiceFactoryException {
        return dataSourceFactory.createDataSource();
    }

    @Bean
    public MidPointImplicitNamingStrategy midPointImplicitNamingStrategy() {
        return new MidPointImplicitNamingStrategy();
    }

    @Bean
    public MidPointPhysicalNamingStrategy midPointPhysicalNamingStrategy() {
        return new MidPointPhysicalNamingStrategy();
    }

    @Bean
    public EntityStateInterceptor entityStateInterceptor() {
        return new EntityStateInterceptor();
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(
            DataSource dataSource,
            SqlRepositoryFactory sqlRepositoryFactory,
            MidPointImplicitNamingStrategy midPointImplicitNamingStrategy,
            MidPointPhysicalNamingStrategy midPointPhysicalNamingStrategy,
            EntityStateInterceptor entityStateInterceptor) {

        SqlRepositoryConfiguration configuration = sqlRepositoryFactory.getConfiguration();

        return sessionFactory(dataSource, configuration, midPointImplicitNamingStrategy,
                midPointPhysicalNamingStrategy, entityStateInterceptor);
    }

    // Used by programmatic audit initialization
    @NotNull
    public LocalSessionFactoryBean sessionFactory(
            DataSource dataSource,
            SqlRepositoryConfiguration configuration,
            MidPointImplicitNamingStrategy midPointImplicitNamingStrategy,
            MidPointPhysicalNamingStrategy midPointPhysicalNamingStrategy,
            EntityStateInterceptor entityStateInterceptor) {
        LocalSessionFactoryBean bean = new LocalSessionFactoryBean();

        // While dataSource == dataSourceFactory.getDataSource(), we're using dataSource as
        // parameter to assure, that Spring already called the factory method. Explicit is good.
        bean.setDataSource(dataSource);

        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", configuration.getHibernateDialect());
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", configuration.getHibernateHbm2ddl());
        hibernateProperties.setProperty("hibernate.id.new_generator_mappings", "true");
        hibernateProperties.setProperty("hibernate.jdbc.batch_size", "20");
        hibernateProperties.setProperty("javax.persistence.validation.mode", "none");
        hibernateProperties.setProperty("hibernate.transaction.coordinator_class", "jdbc");
        hibernateProperties.setProperty("hibernate.hql.bulk_id_strategy",
                "org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy");

        bean.setHibernateProperties(hibernateProperties);
        bean.setImplicitNamingStrategy(midPointImplicitNamingStrategy);
        bean.setPhysicalNamingStrategy(midPointPhysicalNamingStrategy);
        bean.setAnnotatedPackages("com.evolveum.midpoint.repo.sql.type");
        bean.setPackagesToScan(
                "com.evolveum.midpoint.repo.sql.data.common",
                "com.evolveum.midpoint.repo.sql.data.common.any",
                "com.evolveum.midpoint.repo.sql.data.common.container",
                "com.evolveum.midpoint.repo.sql.data.common.embedded",
                "com.evolveum.midpoint.repo.sql.data.common.enums",
                "com.evolveum.midpoint.repo.sql.data.common.id",
                "com.evolveum.midpoint.repo.sql.data.common.other",
                "com.evolveum.midpoint.repo.sql.data.common.type",
                "com.evolveum.midpoint.repo.sql.data.audit");
        bean.setEntityInterceptor(entityStateInterceptor);

        return bean;
    }

    @Bean
    public TransactionManager transactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager htm = new HibernateTransactionManager();
        htm.setSessionFactory(sessionFactory);

        return htm;
    }

    @Bean
    public AuditServiceFactory sqlAuditServiceFactory(
            BaseHelper defaultBaseHelper,
            PrismContext prismContext) {
        return new SqlAuditServiceFactory(defaultBaseHelper, prismContext);
    }

    // TODO it would be better to have dependencies explicit here, but there is cyclic one
    //  in SystemConfigurationChangeDispatcherImpl.
    @Bean
    public SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher() {
        return new SystemConfigurationChangeDispatcherImpl();
    }
}
