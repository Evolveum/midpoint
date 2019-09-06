/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.util.Properties;

import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.util.EntityStateInterceptor;
import com.evolveum.midpoint.repo.sql.util.MidPointImplicitNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.MidPointPhysicalNamingStrategy;

/**
 * Created by Viliam Repan (lazyman).
 */
@Configuration
public class SqlRepositoryBeanConfig {

    @Autowired
    private SqlRepositoryFactory sqlRepositoryFactory;

    @Bean
    public ExtItemDictionary extItemDictionary() {
        return new ExtItemDictionary();
    }

    @Bean
    public DataSourceFactory dataSourceFactory() {
        DataSourceFactory df = new DataSourceFactory();
        df.setConfiguration(sqlRepositoryFactory.getSqlConfiguration());

        return df;
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
    public LocalSessionFactoryBean sessionFactory() throws RepositoryServiceFactoryException {
        LocalSessionFactoryBean bean = new LocalSessionFactoryBean();

        DataSourceFactory dataSourceFactory = dataSourceFactory();
        SqlRepositoryConfiguration configuration = sqlRepositoryFactory.getSqlConfiguration();

        bean.setDataSource(dataSourceFactory.createDataSource());

        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.dialect", configuration.getHibernateDialect());
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", configuration.getHibernateHbm2ddl());
        hibernateProperties.setProperty("hibernate.id.new_generator_mappings", "true");
        hibernateProperties.setProperty("hibernate.jdbc.batch_size", "20");
        hibernateProperties.setProperty("javax.persistence.validation.mode", "none");
        hibernateProperties.setProperty("hibernate.transaction.coordinator_class", "jdbc");
        hibernateProperties.setProperty("hibernate.hql.bulk_id_strategy", "org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy");

        bean.setHibernateProperties(hibernateProperties);
        bean.setImplicitNamingStrategy(midPointImplicitNamingStrategy());
        bean.setPhysicalNamingStrategy(midPointPhysicalNamingStrategy());
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
        bean.setEntityInterceptor(entityStateInterceptor());

        return bean;
    }

    @Bean
    public HibernateTransactionManager transactionManager() throws RepositoryServiceFactoryException {
        HibernateTransactionManager htm = new HibernateTransactionManager();
        htm.setSessionFactory(sessionFactory().getObject());

        return htm;
    }
}
