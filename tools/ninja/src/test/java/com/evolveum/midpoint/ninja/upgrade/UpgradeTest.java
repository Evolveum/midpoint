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
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.SetupDatabaseAction;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.opts.SetupDatabaseOptions;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.test.AbstractIntegrationTest;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class UpgradeTest extends AbstractIntegrationTest {

    private static final String NINJA_TESTS_USER = "ninja_upgrade_tests";

    @Autowired
    private DataSourceFactory dataSourceFactory;

    @Autowired
    @Qualifier("dataSource")
    private DataSource midpointDatasource;

    private HikariDataSource ninjaTestDatabase;

    private String ninjaTestsJdbcUrl;

    @BeforeClass
    public void beforeClass() throws SQLException {
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

    private void recreateEmptyTestDatabase() throws SQLException {
        runWith(midpointDatasource, jdbcTemplate -> {
            jdbcTemplate.execute("DROP DATABASE IF EXISTS " + NINJA_TESTS_USER + ";");
            jdbcTemplate.execute("DROP USER IF EXISTS " + NINJA_TESTS_USER + ";");

            jdbcTemplate.execute("CREATE USER " + NINJA_TESTS_USER + " WITH PASSWORD '" + NINJA_TESTS_USER + "' LOGIN SUPERUSER;");
            jdbcTemplate.execute("CREATE DATABASE " + NINJA_TESTS_USER + " WITH OWNER = " + NINJA_TESTS_USER
                    + " ENCODING = 'UTF8' TABLESPACE = pg_default LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' CONNECTION LIMIT = -1 TEMPLATE = template0");
        });
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

    private void runWith(DataSource dataSource, Consumer<JdbcTemplate> consumer) throws SQLException {
        runWithResult(dataSource, (Function<JdbcTemplate, Void>) jdbcTemplate -> {
            consumer.accept(jdbcTemplate);
            return null;
        });
    }

    private <R> R runWithResult(DataSource dataSource, Function<JdbcTemplate, R> function) throws SQLException {
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

    private Long countTablesInPublicSchema() throws SQLException {
        return runWithResult(ninjaTestDatabase, jdbcTemplate ->
                jdbcTemplate.queryForObject("SELECT count(*) as count FROM information_schema.tables WHERE table_schema = 'public'", Long.class));
    }

    private String getGlobalMetadataValue(DataSource dataSource, String key) throws SQLException {
        return runWithResult(dataSource, jdbcTemplate ->
                jdbcTemplate.queryForObject("select value from m_global_metadata where name=?", new Object[] { key }, String.class));
    }

    @Test
    public void test100InitializeDatabase() throws Exception {
        Assertions.assertThat(countTablesInPublicSchema()).isZero();

        BaseOptions baseOptions = new BaseOptions();

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome("./target/midpoint-home");

        Assertions.assertThat(ninjaTestsJdbcUrl).isNotNull();
        connectionOptions.setUrl(ninjaTestsJdbcUrl);

        SetupDatabaseOptions setupDatabaseOptions = new SetupDatabaseOptions();
        setupDatabaseOptions.setScriptsDirectory(new File("../../config/sql/native-new"));

        NinjaContext context = new NinjaContext(List.of(baseOptions, connectionOptions, setupDatabaseOptions));

        SetupDatabaseAction action = new SetupDatabaseAction();
        action.init(context, setupDatabaseOptions);

        action.execute();

        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();

        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber"))
                .isEqualTo("15");
        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber"))
                .isEqualTo("3");
    }
}
