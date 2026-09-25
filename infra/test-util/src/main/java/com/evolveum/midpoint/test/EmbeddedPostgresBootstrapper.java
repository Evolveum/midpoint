/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bootstraps an embedded PostgreSQL (https://github.com/zonkyio/embedded-postgres) for tests,
 * used by the "embedded-postgres" Maven profile (active by default).
 * <p>
 * Must be instantiated (as a bean declared before the "midpointConfiguration" bean) before
 * {@link com.evolveum.midpoint.init.StartupConfiguration} reads the configuration:
 * it starts the embedded server, prepares the "midtest" database with the midPoint schema
 * (config/sql/native/*.sql) and overrides the "midpoint.repository.jdbcUrl/jdbcUsername/jdbcPassword"
 * system properties, which then take effect via the standard "midpoint.*" property override mechanism.
 * <p>
 * Starting is a no-op when:
 * <ul>
 * <li>the {@link #ENABLED_PROPERTY} system property is not "true" (e.g. "-Psqale" profile
 * pointing tests at an externally managed database), or</li>
 * <li>{@link #JDBC_URL_PROPERTY} is already set (explicit connection always wins, e.g. in CI).</li>
 * </ul>
 * Instances are reused: the embedded server is started at most once per JVM.
 */
public class EmbeddedPostgresBootstrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedPostgresBootstrapper.class);

    /** System property (JVM option) enabling the embedded PostgreSQL bootstrap. */
    public static final String ENABLED_PROPERTY = "midpoint.embedded.postgres";

    /** Standard midPoint property for the repository JDBC URL, overridden to point at the embedded server. */
    public static final String JDBC_URL_PROPERTY = "midpoint.repository.jdbcUrl";
    public static final String JDBC_USERNAME_PROPERTY = "midpoint.repository.jdbcUsername";
    public static final String JDBC_PASSWORD_PROPERTY = "midpoint.repository.jdbcPassword";

    /** Name of the prepared database (same as the one used with external test databases). */
    public static final String DB_NAME = "midtest";
    /** Embedded PostgreSQL runs "initdb -A trust", so the superuser has no password. */
    public static final String DB_USER = "postgres";

    /** Schema scripts, in the order used by CI (Jenkins) and ninja native tests. */
    private static final String[] SCHEMA_SCRIPTS = {
            "postgres.sql",
            "postgres-quartz.sql",
            "postgres-audit.sql",
    };

    private static EmbeddedPostgres embeddedPostgres;

    /**
     * Starts the embedded PostgreSQL (if enabled and not yet started) and overrides
     * the "midpoint.repository.jdbc*" system properties to point at it.
     */
    public void start() {
        if (!Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"))) {
            LOGGER.debug("Embedded PostgreSQL not enabled ({} not set to true), skipping.", ENABLED_PROPERTY);
            return;
        }
        String explicitJdbcUrl = System.getProperty(JDBC_URL_PROPERTY);
        if (explicitJdbcUrl != null && !explicitJdbcUrl.isEmpty()) {
            LOGGER.info("Explicit {} is set ('{}'), not starting the embedded PostgreSQL.",
                    JDBC_URL_PROPERTY, explicitJdbcUrl);
            return;
        }
        if (embeddedPostgres != null) {
            LOGGER.debug("Embedded PostgreSQL already running, reusing it.");
            return;
        }

        LOGGER.info("Starting the embedded PostgreSQL for testing...");
        try {
            EmbeddedPostgres postgres = EmbeddedPostgres.builder()
                    .setPGStartupWait(Duration.ofSeconds(30))
                    .start();
            int port = postgres.getPort();

            String jdbcUrlBase = "jdbc:postgresql://localhost:" + port;
            createDatabase(jdbcUrlBase);
            executeSchemaScripts(jdbcUrlBase);

            String jdbcUrl = jdbcUrlBase + "/" + DB_NAME;
            System.setProperty(JDBC_URL_PROPERTY, jdbcUrl);
            System.setProperty(JDBC_USERNAME_PROPERTY, DB_USER);
            System.setProperty(JDBC_PASSWORD_PROPERTY, "");

            embeddedPostgres = postgres;
            LOGGER.info("Embedded PostgreSQL ready, tests will use JDBC URL: {}", jdbcUrl);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start the embedded PostgreSQL for testing", e);
        }
    }

    private static void createDatabase(String jdbcUrlBase) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrlBase + "/postgres", DB_USER, null);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + DB_NAME);
            LOGGER.info("Database '{}' created.", DB_NAME);
        }
    }

    private static void executeSchemaScripts(String jdbcUrlBase) throws Exception {
        File sqlDirectory = resolveSqlDirectory();
        try (Connection connection = DriverManager.getConnection(jdbcUrlBase + "/" + DB_NAME, DB_USER, null)) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                for (String script : SCHEMA_SCRIPTS) {
                    File scriptFile = new File(sqlDirectory, script);
                    LOGGER.info("Executing schema script: {}", scriptFile.getAbsolutePath());
                    statement.execute(Files.readString(scriptFile.toPath(), StandardCharsets.UTF_8));
                }
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
        LOGGER.info("Schema scripts executed successfully.");
    }

    /**
     * Locates the "config/sql/native" directory in the repository root.
     * Uses "maven.multiModuleProjectDirectory" (passed to the forked test JVM by the build),
     * falling back to walking up from the working directory (tests run from module directories).
     */
    private static File resolveSqlDirectory() {
        String mavenRoot = System.getProperty("maven.multiModuleProjectDirectory");
        if (mavenRoot != null && !mavenRoot.isEmpty()) {
            File sqlDirectory = new File(mavenRoot, "config/sql/native");
            if (sqlDirectory.isDirectory()) {
                return sqlDirectory;
            }
        }
        File directory = new File(System.getProperty("user.dir"));
        for (int levels = 0; levels < 5 && directory != null; levels++) {
            File sqlDirectory = new File(directory, "config/sql/native");
            if (sqlDirectory.isDirectory()) {
                return sqlDirectory;
            }
            directory = directory.getParentFile();
        }
        throw new IllegalStateException("Cannot locate the 'config/sql/native' directory "
                + "(set the 'maven.multiModuleProjectDirectory' system property to the repository root)");
    }
}
