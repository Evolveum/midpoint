/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.testng.AssertJUnit;

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
public class BaseTest extends AbstractUnitTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    private static final File TARGET_HOME = new File("./target/home");

    public static final String RESOURCES_FOLDER = "./target/test-classes";

    private List<String> systemOut;
    private List<String> systemErr;

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

    protected void executeTest(String... args) {
        executeTest(null, null, args);
    }

    protected void executeTest(ExecutionValidator preExecutionValidator,
            ExecutionValidator postExecutionValidator, String... args) {
        executeTest(null, preExecutionValidator, postExecutionValidator, false, false, args);
    }

    /**
     * TODO: Messing with stdout/err is not ideal, Maven also complains:
     * [WARNING] Corrupted STDOUT by directly writing to native stream in forked JVM 1. See FAQ web page and the dump file ...
     * It would be better to use PrintStream variables in Ninja directly, by default these would be System.out/err,
     * but the can be provided from the outside too (for tests).
     * Also, the test stream could do double duty - add the output to the list for asserts (or even work as asserter!),
     * and still print it to the original stream as well for better log output from the maven (test.log).
     */
    protected void executeTest(ExecutionValidator preInit,
            ExecutionValidator preExecution,
            ExecutionValidator postExecution,
            boolean saveOut, boolean saveErr, String... args) {

        systemOut = new ArrayList<>();
        systemErr = new ArrayList<>();

        ByteArrayOutputStream bosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bosErr = new ByteArrayOutputStream();

        if (saveOut) {
            System.setOut(new PrintStream(bosOut));
        }

        if (saveErr) {
            System.setErr(new PrintStream(bosErr));
        }

        try {
            Main main = new Main() {

                // todo fix [viliam]
//                @Override
//                protected void preInit(NinjaContext context) {
//                    validate(preInit, context, "pre init");
//                }
//
//                @Override
//                protected void preExecute(NinjaContext context) {
//                    validate(preExecution, context, "pre execution");
//                }
//
//                @Override
//                protected void postExecute(NinjaContext context) {
//                    validate(postExecution, context, "post execution");
//                }
            };

            main.run(args);
        } finally {
            try {
                if (saveOut) {
                    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
                    systemOut = IOUtils.readLines(new ByteArrayInputStream(bosOut.toByteArray()), StandardCharsets.UTF_8);
                }

                if (saveErr) {
                    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
                    systemErr = IOUtils.readLines(new ByteArrayInputStream(bosErr.toByteArray()), StandardCharsets.UTF_8);
                }
            } catch (IOException ex) {
                // ignored
            }
            systemOut.forEach(s -> System.out.println(s));
            systemErr.forEach(s -> System.err.println(s));
        }
    }

    protected List<String> getSystemOut() {
        return systemOut;
    }

    protected List<String> getSystemErr() {
        return systemErr;
    }

    private void validate(ExecutionValidator validator, NinjaContext context, String message) {
        if (validator == null) {
            return;
        }

        try {
            LOG.info("Starting {}", message);
            validator.validate(context);
        } catch (Exception ex) {
            logTestExecutionFail("Validation '" + message + "' failed with exception", ex);
        }
    }

    private void logTestExecutionFail(String message, Exception ex) {
        LOG.error(message, ex);

        AssertJUnit.fail(message + ": " + ex.getMessage());
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
