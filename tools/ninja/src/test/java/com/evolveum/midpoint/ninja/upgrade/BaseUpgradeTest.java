package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.SetupDatabaseAction;
import com.evolveum.midpoint.ninja.action.SetupDatabaseOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.test.AbstractIntegrationTest;

public abstract class BaseUpgradeTest extends AbstractIntegrationTest {

    protected static final String NINJA_TESTS_USER = "ninja_upgrade_tests";

    protected static final File UPGRADE_MIDPOINT_HOME = new File("./target/midpoint-home-upgrade");

    @Autowired
    protected DataSourceFactory dataSourceFactory;

    @Autowired
    @Qualifier("dataSource")
    protected DataSource midpointDatasource;

    protected HikariDataSource ninjaTestDatabase;

    protected String ninjaTestsJdbcUrl;

    @BeforeClass
    public void beforeClass() throws Exception {
        FileUtils.forceMkdir(UPGRADE_MIDPOINT_HOME);
        FileUtils.copyFileToDirectory(new File("./src/test/resources/upgrade/midpoint-home/config.xml"), UPGRADE_MIDPOINT_HOME);

        recreateEmptyTestDatabase();

        HikariConfig config = new HikariConfig();
        config.setUsername(NINJA_TESTS_USER);
        config.setPassword(NINJA_TESTS_USER);
        config.setAutoCommit(true);

        String jdbcUrl = dataSourceFactory.configuration().getJdbcUrl().replaceFirst("jdbc:", "");
        URI jdbcURI = URI.create(jdbcUrl);

        ninjaTestsJdbcUrl = "jdbc:postgresql://" + jdbcURI.getHost() + ":" + jdbcURI.getPort() + "/" + NINJA_TESTS_USER;
        config.setJdbcUrl(ninjaTestsJdbcUrl);

        ninjaTestDatabase = new HikariDataSource(config);

        runWith(ninjaTestDatabase, jdbcTemplate -> jdbcTemplate.execute("DROP SCHEMA IF EXISTS public CASCADE;"));
    }

    protected void recreateEmptyTestDatabase() throws SQLException {
        runWith(midpointDatasource, jdbcTemplate -> {
            jdbcTemplate.execute("DROP DATABASE IF EXISTS " + NINJA_TESTS_USER + ";");
            jdbcTemplate.execute("DROP USER IF EXISTS " + NINJA_TESTS_USER + ";");

            jdbcTemplate.execute("CREATE USER " + NINJA_TESTS_USER + " WITH PASSWORD '" + NINJA_TESTS_USER + "' LOGIN SUPERUSER;");
            jdbcTemplate.execute("CREATE DATABASE " + NINJA_TESTS_USER + " WITH OWNER = " + NINJA_TESTS_USER
                    + " ENCODING = 'UTF8' TABLESPACE = pg_default LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' CONNECTION LIMIT = -1 TEMPLATE = template0");
        });
    }

    protected void recreateSchema(@NotNull File scriptsDirectory) throws Exception {
        runWith(ninjaTestDatabase, jdbcTemplate -> jdbcTemplate.execute("DROP SCHEMA IF EXISTS public;"));

        BaseOptions baseOptions = new BaseOptions();

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());

        Assertions.assertThat(ninjaTestsJdbcUrl).isNotNull();
        connectionOptions.setUrl(ninjaTestsJdbcUrl);

        SetupDatabaseOptions setupDatabaseOptions = new SetupDatabaseOptions();
        setupDatabaseOptions.setScriptsDirectory(scriptsDirectory);

        List<Object> options = List.of(baseOptions, connectionOptions, setupDatabaseOptions);

        SetupDatabaseAction action = new SetupDatabaseAction();

        try (NinjaContext context = new NinjaContext(options, action.getApplicationContextLevel(options))) {
            action.init(context, setupDatabaseOptions);

            action.execute();
        }

        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() throws SQLException {
        if (ninjaTestDatabase != null) {
            ninjaTestDatabase.close();
        }

        runWith(midpointDatasource, jdbcTemplate -> {
            jdbcTemplate.execute("DROP DATABASE IF EXISTS " + NINJA_TESTS_USER + ";");
            jdbcTemplate.execute("DROP USER IF EXISTS " + NINJA_TESTS_USER + ";");
        });
    }

    protected void runWith(@NotNull DataSource dataSource, @NotNull Consumer<JdbcTemplate> consumer) throws SQLException {
        runWithResult(dataSource, (Function<JdbcTemplate, Void>) jdbcTemplate -> {
            consumer.accept(jdbcTemplate);
            return null;
        });
    }

    protected <R> R runWithResult(@NotNull DataSource dataSource, @NotNull Function<JdbcTemplate, R> function) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean autocommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(true);

                JdbcTemplate template = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                return function.apply(template);
            } finally {
                connection.setAutoCommit(autocommit);
            }
        }
    }

    protected Long countTablesInPublicSchema() throws SQLException {
        return runWithResult(ninjaTestDatabase, jdbcTemplate ->
                jdbcTemplate.queryForObject("SELECT COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'public'", Long.class));
    }

    protected String getGlobalMetadataValue(@NotNull DataSource dataSource, @NotNull String key) throws SQLException {
        return runWithResult(dataSource, jdbcTemplate ->
                jdbcTemplate.queryForObject("SELECT value FROM m_global_metadata WHERE name=?", new Object[] { key }, String.class));
    }
}
