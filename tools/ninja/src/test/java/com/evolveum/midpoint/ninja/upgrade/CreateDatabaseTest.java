package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import com.evolveum.midpoint.ninja.BaseUpgradeTest;
import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.RunSqlAction;
import com.evolveum.midpoint.ninja.action.RunSqlOptions;

@ContextConfiguration(locations = "classpath:ctx-ninja-no-repository-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class CreateDatabaseTest extends BaseUpgradeTest {

    public static final File UPGRADE_RESOURCES = new File("./src/test/resources/upgrade");
    public static final String CREATE_MIDPOINT_SCHEMA_TEST = "create_midpoint_schema_test";

    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        super.beforeClass();

        dropUserAndDatabaseIfExists();
    }

    @Override
    public void afterClass() throws SQLException {
        dropUserAndDatabaseIfExists();

        super.afterClass();
    }

    private void dropUserAndDatabaseIfExists() throws SQLException {
        runWith(ninjaTestDatabase, jdbcTemplate -> jdbcTemplate.execute(
                "DROP DATABASE IF EXISTS " + CREATE_MIDPOINT_SCHEMA_TEST + ";\n"
                        + "DROP USER IF EXISTS " + CREATE_MIDPOINT_SCHEMA_TEST + ";"));
    }

    @Test(enabled = false)
    public void test100CreateUserAndDatabase() throws Exception {
        BaseOptions baseOptions = new BaseOptions();
        baseOptions.setVerbose(true);

        final File FAKE_MIDPOINT_HOME = new File("./target/midpoint-home-upgrade-non-existing");

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(FAKE_MIDPOINT_HOME.getPath());

        RunSqlOptions runSqlOptions = new RunSqlOptions();
        runSqlOptions.setJdbcUrl(getJdbcUrlForNinjaTestsUser());
        runSqlOptions.setJdbcUsername(NINJA_TESTS_USER);
        runSqlOptions.setJdbcPassword(NINJA_TESTS_USER);
        runSqlOptions.setScripts(List.of(new File(UPGRADE_RESOURCES, "sql/create-database.sql")));
        runSqlOptions.setResult(true);

        List<Object> options = List.of(baseOptions, connectionOptions, runSqlOptions);

        executeAction(RunSqlAction.class, runSqlOptions, options);

        Assertions.assertThat(FAKE_MIDPOINT_HOME.exists()).isFalse();

        try (HikariDataSource dataSource = createHikariDataSource(getJdbcUrlForDatabase(CREATE_MIDPOINT_SCHEMA_TEST), CREATE_MIDPOINT_SCHEMA_TEST, CREATE_MIDPOINT_SCHEMA_TEST)) {
            Object one = runWithResult(dataSource, jdbcTemplate -> jdbcTemplate.queryForObject("select 1", Long.class));
            Assertions.assertThat(one).isEqualTo(1L);
        }
    }

    @Test(enabled = false)
    public void test200CreateSchema() throws Exception {
        BaseOptions baseOptions = new BaseOptions();
        baseOptions.setVerbose(true);

        final File FAKE_MIDPOINT_HOME = new File("./target/midpoint-home-upgrade-non-existing");

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(FAKE_MIDPOINT_HOME.getPath());

        final String jdbcUrl = getJdbcUrlForDatabase(CREATE_MIDPOINT_SCHEMA_TEST);
        RunSqlOptions runSqlOptions = new RunSqlOptions();
        runSqlOptions.setJdbcUrl(jdbcUrl);
        runSqlOptions.setJdbcUsername(CREATE_MIDPOINT_SCHEMA_TEST);
        runSqlOptions.setJdbcPassword(CREATE_MIDPOINT_SCHEMA_TEST);
        runSqlOptions.setScripts(List.of(
                new File("../../config/sql/native/postgres.sql"),
                new File("../../config/sql/native/postgres-quartz.sql"),
                new File("../../config/sql/native/postgres-audit.sql")));

        List<Object> options = List.of(baseOptions, connectionOptions, runSqlOptions);

        executeAction(RunSqlAction.class, runSqlOptions, options);

        Assertions.assertThat(FAKE_MIDPOINT_HOME.exists()).isFalse();

        try (HikariDataSource dataSource = createHikariDataSource(jdbcUrl, CREATE_MIDPOINT_SCHEMA_TEST, CREATE_MIDPOINT_SCHEMA_TEST)) {
            Assertions.assertThat(getSchemaChangeNumber(dataSource)).isEqualTo(CURRENT_SCHEMA_CHANGE_NUMBER);
            Assertions.assertThat(getAuditSchemaChangeNumber(dataSource)).isEqualTo(CURRENT_AUDIT_SCHEMA_CHANGE_NUMBER);
        }
    }
}
