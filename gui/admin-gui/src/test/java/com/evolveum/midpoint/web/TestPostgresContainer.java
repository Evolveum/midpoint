/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.web;

import java.io.File;
import java.io.IOException;

import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Starts a PostgreSQL container for the native (Sqale) repository used by the GUI tests.
 *
 * The container is started once per JVM (on first use) and stopped by Testcontainers' Ryuk at JVM exit.
 * It listens on the fixed port 5432 and uses the credentials from {@code test-config-new-repo.xml}
 * ({@code midtest}/{@code password}, database {@code midtest}). The schema is created from the repository's
 * own scripts in {@code config/sql/native}.
 */
public final class TestPostgresContainer {

    private static final String IMAGE = "postgres:16-alpine";
    private static final int PORT = 5432;
    private static final String SQL_DIR = "../../config/sql/native";

    private static FixedHostPortGenericContainer<?> container;

    private TestPostgresContainer() {
    }

    public static synchronized void ensureStarted() {
        if (container != null) {
            return;
        }
        try {
            container = new FixedHostPortGenericContainer<>(IMAGE)
                    .withFixedExposedPort(PORT, PORT)
                    .withEnv("POSTGRES_DB", "midtest")
                    .withEnv("POSTGRES_USER", "midtest")
                    .withEnv("POSTGRES_PASSWORD", "password")
                    .withCopyFileToContainer(script("postgres.sql"), "/docker-entrypoint-initdb.d/01-postgres.sql")
                    .withCopyFileToContainer(script("postgres-audit.sql"), "/docker-entrypoint-initdb.d/02-postgres-audit.sql")
                    .withCopyFileToContainer(script("postgres-quartz.sql"), "/docker-entrypoint-initdb.d/03-postgres-quartz.sql")
                    // the entrypoint restarts the server after running init scripts, so the message appears twice
                    .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 2));
            container.start();
        } catch (RuntimeException e) {
            container = null;
            throw new IllegalStateException("Cannot start PostgreSQL test container on port " + PORT
                    + " (is the port free and is Docker running?)", e);
        }
    }

    private static MountableFile script(String name) {
        File file = new File(SQL_DIR, name);
        try {
            return MountableFile.forHostPath(file.getCanonicalPath());
        } catch (IOException e) {
            throw new IllegalStateException("Missing SQL script " + file.getAbsolutePath(), e);
        }
    }
}
