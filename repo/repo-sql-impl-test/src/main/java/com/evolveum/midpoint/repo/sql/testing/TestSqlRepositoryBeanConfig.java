/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.testing;

import java.util.Properties;
import javax.sql.DataSource;

import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.support.ProxyConfigSpringXmlSupport;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.SqlRepositoryBeanConfig;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.util.EntityStateInterceptor;
import com.evolveum.midpoint.repo.sql.util.MidPointImplicitNamingStrategy;
import com.evolveum.midpoint.repo.sql.util.MidPointPhysicalNamingStrategy;

/**
 * Test configuration for repository, adding test query listener and related interceptors.
 */
@Configuration
@ConditionalOnExpression("#{midpointConfiguration.keyMatches("
        + "'midpoint.repository.repositoryServiceFactoryClass',"
        + " '(?i)com\\.evolveum\\.midpoint\\.repo\\.sql\\..*', '(?i)sql')"
        + "|| midpointConfiguration.keyMatches("
        + "'midpoint.repository.type',"
        + " '(?i)com\\.evolveum\\.midpoint\\.repo\\.sql\\..*', '(?i)sql')"
        + "}")
@ComponentScan
@Import(SqlRepositoryBeanConfig.class)
public class TestSqlRepositoryBeanConfig {

    @Bean
    public TestInterceptor testInterceptor() {
        return new TestInterceptor();
    }

    /**
     * This conditional on missing bean "ninja" is just nasty hack to fix initialization of ninja spring context in tests.
     * Currently, when tests for ninja are initialized - spring context for test class alone is initialized correctly.
     * However, when ninja is started in test via {@link NinjaTestMixin#executeTest}, ninja starts loading it's own spring
     * context internally to initialize repository service - and if generic repository (e.g. for mssql) is being initialized,
     * class path scan also finds this {@link TestSqlRepositoryBeanConfig} and initializes it - meaning, underlying database is
     * cleaned because of this bean post processor.
     *
     * @see ctx-ninja.xml (bean "ninja")
     */
    @ConditionalOnMissingBean(name = "ninja")
    @Bean
    public TestSqlRepositoryBeanPostProcessor testSqlRepositoryBeanPostProcessor() {
        return new TestSqlRepositoryBeanPostProcessor();
    }

    @Bean
    public TestQueryListener testQueryListener() {
        return new TestQueryListener();
    }

    /** Proxied data source used to collect SQL statements. */
    @Primary
    @Bean
    public DataSource proxiedTestDataSource(
            DataSource dataSource,
            TestQueryListener testQueryListener) {
        ChainListener chainListener = new ChainListener();
        chainListener.addListener(testQueryListener);

        ProxyConfigSpringXmlSupport proxyConfigSupport = new ProxyConfigSpringXmlSupport();
        proxyConfigSupport.setDataSourceName("my-ds");
        proxyConfigSupport.setQueryListener(chainListener);

        ProxyDataSource bean = new ProxyDataSource();
        bean.setDataSource(dataSource);
        bean.setProxyConfig(proxyConfigSupport.create());
        return bean;
    }

    @Bean
    public SqlRepositoryConfiguration sqlRepositoryConfiguration(
            MidpointConfiguration midpointConfiguration) throws RepositoryServiceFactoryException {
        // See TODO in TestSqlRepositoryConfigurationFactory about how this can go away.
        return new TestSqlRepositoryConfigurationFactory(midpointConfiguration)
                .createSqlRepositoryConfiguration();
    }

    /*
     * This is working alternative to @Autowired private LocalSessionFactoryBean sessionFactory
     * with @PostConstruct init method that adds the properties. That solution didn't work, because
     * the properties are probably used to initialize the session before added.
     * This nicely reuses existing production config method and adds the properties before they
     * are read and reflected in the final setup.
     * (There is actually just one test that using the inspector - ObjectDeltaUpdaterTest.)
     */
    @Bean
    @Primary
    public LocalSessionFactoryBean sessionFactory(
            DataSource dataSource,
            SqlRepositoryConfiguration configuration,
            MidPointImplicitNamingStrategy midPointImplicitNamingStrategy,
            MidPointPhysicalNamingStrategy midPointPhysicalNamingStrategy,
            EntityStateInterceptor entityStateInterceptor) {
        LocalSessionFactoryBean sessionFactory = new SqlRepositoryBeanConfig().sessionFactory(
                dataSource, configuration, midPointImplicitNamingStrategy,
                midPointPhysicalNamingStrategy, entityStateInterceptor);

        // These are only test-related changes regarding the session factory.
        Properties hibernateProperties = sessionFactory.getHibernateProperties();
        hibernateProperties.setProperty("hibernate.show_sql", "false");
        hibernateProperties.setProperty("hibernate.session_factory.statement_inspector",
                "com.evolveum.midpoint.repo.sql.testing.TestStatementInspector");

        return sessionFactory;
    }
}
