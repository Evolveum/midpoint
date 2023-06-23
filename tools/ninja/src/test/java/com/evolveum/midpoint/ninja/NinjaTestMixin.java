package com.evolveum.midpoint.ninja;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.helpers.BaseHelper;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public interface NinjaTestMixin {

    StreamValidator EMPTY_STREAM_VALIDATOR = list -> Assertions.assertThat(list).isEmpty();

    Trace LOGGER = TraceManager.getTrace(NinjaTestMixin.class);

    File TARGET_HOME = new File("./target/midpoint-home");

    File RESOURCES_DIRECTORY = new File("./src/test/resources");

    @FunctionalInterface
    interface BiFunctionThrowable<T, U, R> {

        R apply(T t, U u) throws Exception;
    }

    @FunctionalInterface
    interface StreamValidator {

        void validate(List<String> stream) throws Exception;
    }

    /**
     * Add @BeforeMethod calling this into test classes that need this.
     * This also removes H2 DB files, effectively cleaning the DB between test methods.
     * This is not enough to support tests on other DB (it doesn't run dbtest profile properly)
     * or for Native repository, but {@link #clearMidpointTestDatabase(ApplicationContext)} can be used in the preExecute block.
     */
    default void setupMidpointHome() throws IOException {
        FileUtils.deleteDirectory(TARGET_HOME);

        File baseHome = new File(RESOURCES_DIRECTORY, "midpoint-home");

        FileUtils.copyDirectory(baseHome, TARGET_HOME);

        // This tells Ninja to use the right config XML for Native repo.
        // Ninja tests don't support test.config.file property as other midPoint tests.
        String testConfigFile = System.getProperty("test.config.file");
        if (testConfigFile != null) {
            System.setProperty(MidpointConfiguration.MIDPOINT_CONFIG_FILE_PROPERTY, testConfigFile);
        }
    }

    default void clearMidpointTestDatabase(ApplicationContext context) {
        LOGGER.info("Cleaning up the database");

        RepositoryService repository = context.getBean(RepositoryService.class);
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
            BaseHelper baseHelper = context.getBean(BaseHelper.class);
            SchemaService schemaService = context.getBean(SchemaService.class);

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

    default String getMidpointHome() {
        return System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
    }

    default <O, R, A extends Action<O, R>> R executeAction(
            @NotNull Class<A> actionClass, @NotNull O actionOptions, @NotNull List<Object> allOptions)
            throws Exception {

        return executeAction(actionClass, actionOptions, allOptions, null, null);
    }

    default <O, R, A extends Action<O, R>> R executeAction(
            @NotNull Class<A> actionClass, @NotNull O actionOptions, @NotNull List<Object> allOptions,
            @Nullable StreamValidator validateOut, @Nullable StreamValidator validateErr)
            throws Exception {

        return executeWithWrappedPrintStreams((out, err) -> {

            A action = actionClass.getConstructor().newInstance();
            try (NinjaContext context = new NinjaContext(out, err, allOptions, action.getApplicationContextLevel(allOptions))) {
                action.init(context, actionOptions);

                return action.execute();
            }
        }, validateOut, validateErr);
    }

    private <R> R executeWithWrappedPrintStreams(
            @NotNull BiFunctionThrowable<PrintStream, PrintStream, R> function, @Nullable StreamValidator validateOut,
            @Nullable StreamValidator validateErr)
            throws Exception {

        ByteArrayOutputStream bosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bosErr = new ByteArrayOutputStream();
        try (
                PrintStream out = new PrintStream(bosOut);
                PrintStream err = new PrintStream(bosErr)
        ) {

            return function.apply(out, err);
        } finally {
            List<String> outLines = processTestOutputStream(bosOut, "OUT");
            List<String> errLines = processTestOutputStream(bosErr, "ERR");

            if (validateOut != null) {
                validateOut.validate(outLines);
            }

            if (validateErr != null) {
                validateErr.validate(errLines);
            }
        }
    }

    default void executeTest(
            @Nullable StreamValidator validateOut, @Nullable StreamValidator validateErr, @NotNull String... args)
            throws Exception {

        executeWithWrappedPrintStreams((out, err) -> {

            Main main = new Main();
            main.setOut(out);
            main.setErr(err);

            main.run(args);

            return null;
        }, validateOut, validateErr);
    }

    private List<String> processTestOutputStream(ByteArrayOutputStream bos, String prefix) throws IOException {
        final Trace logger = TraceManager.getTrace(getClass());

        List<String> lines = IOUtils.readLines(new ByteArrayInputStream(bos.toByteArray()), StandardCharsets.UTF_8);
        if (logger.isDebugEnabled()) {
            lines.forEach(line -> logger.debug("{}: {}", prefix, line));
        }

        return lines;
    }
}
