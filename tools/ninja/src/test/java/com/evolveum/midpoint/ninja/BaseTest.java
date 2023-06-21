/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BaseTest extends AbstractUnitTest implements NinjaTestMixin {

    private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    private static final File TARGET_HOME = new File("./target/home");

    public static final File RESOURCES_DIRECTORY = new File("./src/test/resources");

    public static final String RESOURCES_DIRECTORY_PATH = RESOURCES_DIRECTORY.getPath();

    /**
     * Add @BeforeMethod calling this into test classes that need this.
     * This also removes H2 DB files, effectively cleaning the DB between test methods.
     * This is not enough to support tests on other DB (it doesn't run dbtest profile properly)
     * or for Native repository, but {@link #clearDb} can be used in the preExecute block.
     */
    protected void setupMidpointHome() throws IOException {
        FileUtils.deleteDirectory(TARGET_HOME);

        File baseHome = new File("./src/test/resources/midpoint-home");

        FileUtils.copyDirectory(baseHome, TARGET_HOME);

        // This tells Ninja to use the right config XML for Native repo.
        // Ninja tests don't support test.config.file property as other midPoint tests.
        String testConfigFile = System.getProperty("test.config.file");
        if (testConfigFile != null) {
            System.setProperty(MidpointConfiguration.MIDPOINT_CONFIG_FILE_PROPERTY, testConfigFile);
        }
    }

    protected String getMidpointHome() {
        return TARGET_HOME.getAbsolutePath();
    }

    protected void clearDb(NinjaContext ninjaContext) {
        LOG.info("Cleaning up the database");

        RepositoryService repository = ninjaContext.getRepository();
        if (repository instanceof SqaleRepositoryService) {
            SqaleRepoContext repoCtx = ((SqaleRepositoryService) repository).sqlRepoContext();
            // Just like in model-intest TestSqaleRepositoryBeanConfig.clearDatabase()
            try (JdbcSession jdbcSession = repoCtx.newJdbcSession().startTransaction()) {
                jdbcSession.executeStatement("TRUNCATE m_object CASCADE;");
                jdbcSession.executeStatement("TRUNCATE m_object_oid CASCADE;");
                jdbcSession.executeStatement("TRUNCATE ma_audit_event CASCADE;");
                jdbcSession.commit();
            }
        } else {
            // Logic adapted from TestSqlRepositoryBeanPostProcessor, we just need to get all those beans
            ApplicationContext appContext = ninjaContext.getApplicationContext();
            BaseHelper baseHelper = appContext.getBean(BaseHelper.class);
            SchemaService schemaService = appContext.getBean(SchemaService.class);

            SqlRepositoryConfiguration repoConfig = baseHelper.getConfiguration();
            if (repoConfig.isEmbedded()) {
                return;
            }

            SqlRepoContext fakeRepoContext = new SqlRepoContext(
                    repoConfig, baseHelper.dataSource(), schemaService, null);
            try (JdbcSession jdbcSession = fakeRepoContext.newJdbcSession().startTransaction()) {
                jdbcSession.executeStatement(useProcedure(repoConfig)
                        ? "{ call cleanupTestDatabaseProc() }"
                        : "select cleanupTestDatabase();");
                jdbcSession.commit();
            }
        }
    }

    // Only for generic repo, eventually goes away...
    private boolean useProcedure(SqlRepositoryConfiguration config) {
        return StringUtils.containsIgnoreCase(config.getHibernateDialect(), "oracle")
                || StringUtils.containsIgnoreCase(config.getHibernateDialect(), "SQLServer");
    }
}
