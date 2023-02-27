/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.testing;

import javax.sql.DataSource;

import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.support.ProxyConfigSpringXmlSupport;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.*;

import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryBeanConfig;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Test configuration for new repository, adding test query listener and database cleanup.
 */
@Configuration
@ConditionalOnExpression(
        "#{midpointConfiguration.keyMatches('midpoint.repository.type', '(?i)s[qc]ale|native')}")
@ComponentScan
@Import(SqaleRepositoryBeanConfig.class)
public class TestSqaleRepositoryBeanConfig {

    private static final Trace LOGGER = TraceManager.getTrace(TestSqaleRepositoryBeanConfig.class);

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
        //noinspection DuplicatedCode
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

    @Primary
    @Bean
    public SqaleRepositoryService repositoryService(
            SqaleRepoContext sqlRepoContext,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        clearDatabase(sqlRepoContext);

        return new SqaleRepositoryService(
                sqlRepoContext,
                sqlPerformanceMonitorsCollection);
    }

    /**
     * Taken from `SqaleRepoBaseTest#clearDatabase()` - this is the new repo version of
     * TestSqlRepositoryBeanPostProcessor.
     * No need to clean up the cache tables, only the main and audit tables are cleared.
     */
    public void clearDatabase(SqaleRepoContext sqlRepoContext) {
        LOGGER.info("Clearing the testing database!");
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            // Truncate cascades to sub-rows of the "object aggregate" - if FK points to m_object table hierarchy.
            jdbcSession.executeStatement("TRUNCATE m_object CASCADE;");

            // But truncate does not run ON DELETE trigger, many refs/container tables are not cleaned,
            // because their FK references OID pool table. After truncating m_object_oid it cleans all the tables.
            jdbcSession.executeStatement("TRUNCATE m_object_oid CASCADE;");

            jdbcSession.executeStatement("TRUNCATE ma_audit_event CASCADE;");
            jdbcSession.commit();
        }
    }
}
