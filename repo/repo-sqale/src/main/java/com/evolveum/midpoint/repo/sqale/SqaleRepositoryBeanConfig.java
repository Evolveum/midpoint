/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;

/**
 * New SQL repository related configuration.
 * {@link ConditionalOnMissingBean} annotations are used to avoid duplicate bean acquirement that
 * would happen when combined with alternative configurations (e.g. context XMLs for test).
 * {@link ConditionalOnExpression} class annotation activates this configuration only if midpoint
 * {@code config.xml} specifies the repository factory class from SQL package.
 */
@Configuration
@ConditionalOnExpression("#{midpointConfiguration.getConfiguration('midpoint.repository')"
        + ".getString('repositoryServiceFactoryClass').startsWith('com.evolveum.midpoint.repo.sqale.')}")
@ComponentScan
public class SqaleRepositoryBeanConfig {

    @Bean
    public SqlRepoContext sqlRepoContext() {
        // TODO fill in
        return new SqlRepoContext(null, null);
    }

    /*
    @Bean
    @ConditionalOnMissingBean
    public DataSourceFactory dataSourceFactory(SqlRepositoryFactory sqlRepositoryFactory) {
        return new DataSourceFactory(sqlRepositoryFactory.getSqlConfiguration());
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
            DataSourceFactory dataSourceFactory,
            MidPointImplicitNamingStrategy midPointImplicitNamingStrategy,
            MidPointPhysicalNamingStrategy midPointPhysicalNamingStrategy,
            EntityStateInterceptor entityStateInterceptor) {
        LocalSessionFactoryBean bean = new LocalSessionFactoryBean();

        SqlRepositoryConfiguration configuration = dataSourceFactory.configuration();

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
    */

    // TODO @Bean for AuditServiceFactory
}
