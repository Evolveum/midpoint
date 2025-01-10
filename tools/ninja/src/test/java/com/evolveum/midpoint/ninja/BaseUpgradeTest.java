/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.RunSqlAction;
import com.evolveum.midpoint.ninja.action.RunSqlOptions;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;

public abstract class BaseUpgradeTest extends AbstractSpringTest implements InfraTestMixin, NinjaTestMixin {

    public static final String NINJA_TESTS_USER = "ninja_upgrade_tests";

    public static final File UPGRADE_MIDPOINT_HOME = new File("./target/midpoint-home-upgrade");

    public static final String CURRENT_SCHEMA_CHANGE_NUMBER = "15";

    public static final String CURRENT_AUDIT_SCHEMA_CHANGE_NUMBER = "4";

    @Autowired
    private MidpointConfiguration midpointConfiguration;

    protected DataSource midpointDatasource;

    protected HikariDataSource ninjaTestDatabase;

    private String jdbcUrlPrefix;

    @BeforeClass
    public void beforeClass() throws Exception {
        FileUtils.forceMkdir(UPGRADE_MIDPOINT_HOME);
        FileUtils.copyFileToDirectory(new File("./src/test/resources/upgrade/midpoint-home/config.xml"), UPGRADE_MIDPOINT_HOME);

        Configuration configuration = midpointConfiguration.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION);
        SqaleRepositoryConfiguration repositoryConfiguration = new SqaleRepositoryConfiguration(configuration);
        repositoryConfiguration.init();
        DataSourceFactory dataSourceFactory = new DataSourceFactory(repositoryConfiguration);
        midpointDatasource = dataSourceFactory.createDataSource("midpoint");

        String jdbcUrl = dataSourceFactory.configuration().getJdbcUrl().replaceFirst("jdbc:", "");
        URI jdbcURI = URI.create(jdbcUrl);

        jdbcUrlPrefix = "jdbc:postgresql://" + jdbcURI.getHost() + ":" + jdbcURI.getPort();

        recreateEmptyNinjaTestsDatabase();
    }

    protected HikariDataSource createHikariDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(Driver.class.getName());
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setAutoCommit(true);
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(3);

        return new HikariDataSource(config);
    }

    protected String getJdbcUrlForNinjaTestsUser() {
        return getJdbcUrlForDatabase(NINJA_TESTS_USER);
    }

    protected String getJdbcUrlForDatabase(String database) {
        return jdbcUrlPrefix + "/" + database;
    }

    protected void recreateEmptyNinjaTestsDatabase() throws SQLException {
        runWith(midpointDatasource, jdbcTemplate -> {
            if (ninjaTestDatabase != null) {
                ninjaTestDatabase.close();
                ninjaTestDatabase = null;
            }

            jdbcTemplate.execute("DROP DATABASE IF EXISTS " + NINJA_TESTS_USER + ";");
            jdbcTemplate.execute("DROP USER IF EXISTS " + NINJA_TESTS_USER + ";");

            jdbcTemplate.execute("CREATE USER " + NINJA_TESTS_USER + " WITH PASSWORD '" + NINJA_TESTS_USER + "' LOGIN SUPERUSER;");
            jdbcTemplate.execute("CREATE DATABASE " + NINJA_TESTS_USER + " WITH OWNER = " + NINJA_TESTS_USER
                    + " ENCODING = 'UTF8' TABLESPACE = pg_default LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' CONNECTION LIMIT = -1 TEMPLATE = template0");

            if (ninjaTestDatabase == null) {
                ninjaTestDatabase = createHikariDataSource(getJdbcUrlForNinjaTestsUser(), NINJA_TESTS_USER, NINJA_TESTS_USER);
            }
        });
    }

    protected List<File> getUpgradeScripts(File scriptsDirectory) {
        return getScripts(scriptsDirectory, RunSqlOptions.Mode.REPOSITORY.updateScripts, RunSqlOptions.Mode.AUDIT.updateScripts);
    }

    protected List<File> getCreateScripts(File scriptsDirectory) {
        return getScripts(scriptsDirectory, RunSqlOptions.Mode.REPOSITORY.createScripts, RunSqlOptions.Mode.AUDIT.createScripts);
    }

    private List<File> getScripts(File scriptsDirectory, List<File>... scripts) {
        List<File> result = new ArrayList<>();
        for (List<File> list : scripts) {
            list.forEach(f -> result.add(new File(scriptsDirectory, f.getName())));
        }
        return result;
    }

    protected void recreateSchema(@NotNull File scriptsDirectory) throws Exception {
        runWith(ninjaTestDatabase, jdbcTemplate -> jdbcTemplate.execute("DROP SCHEMA IF EXISTS public;"));

        BaseOptions baseOptions = new BaseOptions();

        ConnectionOptions connectionOptions = new ConnectionOptions();

        RunSqlOptions runSqlOptions = new RunSqlOptions();
        runSqlOptions.setJdbcUrl(getJdbcUrlForNinjaTestsUser());
        runSqlOptions.setJdbcUsername(NINJA_TESTS_USER);
        runSqlOptions.setJdbcPassword(NINJA_TESTS_USER);
        runSqlOptions.setScripts(getCreateScripts(scriptsDirectory));

        List<Object> options = List.of(baseOptions, connectionOptions, runSqlOptions);

        executeAction(RunSqlAction.class, runSqlOptions, options);

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

    protected String getAuditSchemaChangeNumber(@NotNull DataSource dataSource) throws SQLException {
        return getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber");
    }

    protected String getSchemaChangeNumber(@NotNull DataSource dataSource) throws SQLException {
        return getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber");
    }

    protected String getGlobalMetadataValue(@NotNull DataSource dataSource, @NotNull String key) throws SQLException {
        return runWithResult(dataSource, jdbcTemplate ->
                jdbcTemplate.queryForObject("SELECT value FROM m_global_metadata WHERE name=?", new Object[] { key }, String.class));
    }
}
