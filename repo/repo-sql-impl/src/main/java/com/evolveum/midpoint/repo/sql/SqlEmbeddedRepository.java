/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.h2.tools.Server;

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Component managing embedded H2, starting server if necessary.
 * Doing nothing for non-H2 databases.
 */
public class SqlEmbeddedRepository {

    private static final Trace LOGGER = TraceManager.getTrace(SqlEmbeddedRepository.class);

    private static final String H2_IMPLICIT_RELATIVE_PATH = "h2.implicitRelativePath";

    private static final long POOL_CLOSE_WAIT = 500L;
    private static final long H2_CLOSE_WAIT = 2000L;

    private final SqlRepositoryConfiguration sqlConfiguration;
    private Server server;

    public SqlEmbeddedRepository(SqlRepositoryConfiguration sqlConfiguration) {
        this.sqlConfiguration = sqlConfiguration;
    }

    @PostConstruct
    public void init() throws RepositoryServiceFactoryException {
        if (!sqlConfiguration.isUsingH2()) {
            return;
        }

        if (System.getProperty(H2_IMPLICIT_RELATIVE_PATH) == null) {
            // Allows implicitly relative paths to H2 database file.
            // Our paths were changed to ./midpoint in Dec 2020, so it's here only for users.
            // TODO: consider removal, if someone wants it, let them comply with H2 1.4.
            System.setProperty(H2_IMPLICIT_RELATIVE_PATH, "true");
        }

        if (sqlConfiguration.isEmbedded()) {
            dropDatabaseIfExists(sqlConfiguration);
            if (sqlConfiguration.isAsServer()) {

                try {
                    // create DB file if necessary, not doable in remote/server configuration without creating
                    // security hole by allowing some sort of remote access
                    // http://h2database.com/html/tutorial.html?highlight=ifNotExists&search=ifnote#firstFound
                    String baseDir = sqlConfiguration.getBaseDir();
                    String fileName = sqlConfiguration.getFileName();
                    File databaseFile = new File(baseDir, fileName);
                    String jdbcUrl = "jdbc:h2:file:" + databaseFile.getAbsolutePath();

                    Connection connection = DriverManager.getConnection(
                            jdbcUrl, sqlConfiguration.getJdbcUsername(), sqlConfiguration.getJdbcPassword());
                    connection.close();
                } catch (Exception ex) {
                    throw new RepositoryServiceFactoryException(
                            "Couldn't pre-create database, reason: " + ex.getMessage(), ex);
                }

                LOGGER.info("Starting h2 in server mode.");
                startServer();
            } else {
                LOGGER.info("H2 prepared to run in local mode (from file).");
            }
            LOGGER.info("H2 files are in '{}'.", new File(sqlConfiguration.getBaseDir()).getAbsolutePath());
        } else {
            LOGGER.info("Repository is not running in embedded mode.");
        }
    }

    private void dropDatabaseIfExists(SqlRepositoryConfiguration config) throws RepositoryServiceFactoryException {
        if (!config.isDropIfExists()) {
            LOGGER.info("Database wont be deleted, dropIfExists=false.");
            return;
        }
        LOGGER.info("Deleting database.");

        File file = new File(config.getBaseDir());
        final String fileName = config.getFileName();
        try {
            //removing files based on http://www.h2database.com/html/features.html#database_file_layout
            File dbFileOld = new File(file, fileName + ".h2.db");
            removeFile(dbFileOld);
            File dbFile = new File(file, fileName + ".mv.db");
            removeFile(dbFile);
            File lockFile = new File(file, fileName + ".lock.db");
            removeFile(lockFile);
            File traceFile = new File(file, fileName + ".trace.db");
            removeFile(traceFile);

            File[] tempFiles = file.listFiles(
                    (parent, name) -> name.matches("^" + fileName + "\\.[0-9]*\\.temp\\.db$"));
            if (tempFiles != null) {
                for (File temp : tempFiles) {
                    removeFile(temp);
                }
            }

            File lobDir = new File(file, fileName + ".lobs.db");
            if (lobDir.exists() && lobDir.isDirectory()) {
                LOGGER.info("Deleting directory '{}'", lobDir.getAbsolutePath());
                FileUtils.deleteDirectory(lobDir);
            }
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException("Couldn't drop existing database files, reason: "
                    + ex.getMessage(), ex);
        }
    }

    private void removeFile(File file) {
        if (file.exists()) {
            LOGGER.info("Deleting file '{}', result: {}", file.getAbsolutePath(), file.delete());
        } else {
            LOGGER.info("File '{}' doesn't exist: delete status {}", file.getAbsolutePath(), file.delete());
        }
    }

    private void startServer() throws RepositoryServiceFactoryException {
        checkPort(sqlConfiguration.getPort());

        try {
            String[] serverArguments = createArguments(sqlConfiguration);
            if (LOGGER.isTraceEnabled()) {
                String stringArgs = StringUtils.join(serverArguments, " ");
                LOGGER.trace("Starting H2 server with arguments: {}", stringArgs);
            }
            server = Server.createTcpServer(serverArguments);
            server.start();
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException(ex.getMessage(), ex);
        }
    }

    private void checkPort(int port) throws RepositoryServiceFactoryException {
        if (port >= 65635 || port < 0) {
            throw new RepositoryServiceFactoryException("Port must be in range 0-65634, not '" + port + "'.");
        }

        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
        } catch (BindException e) {
            throw new RepositoryServiceFactoryException("Configured port (" + port + ") for H2 already in use.", e);
        } catch (IOException e) {
            LOGGER.error("Reported IO error, while binding ServerSocket to port " + port + " used to test availability " +
                    "of port for H2 Server", e);
        } finally {
            try {
                if (ss != null) {
                    ss.close();
                }
            } catch (IOException ex) {
                LOGGER.error("Reported IO error, while closing ServerSocket used to test availability " +
                        "of port for H2 Server", ex);
            }
        }
    }

    private String[] createArguments(SqlRepositoryConfiguration config) {
//        [-help] or [-?]         Print the list of options
//        [-web]                  Start the web server with the H2 Console
//        [-webAllowOthers]       Allow other computers to connect - see below
//        [-webDaemon]            Use a daemon thread
//        [-webPort <port>]       The port (default: 8082)
//        [-webSSL]               Use encrypted (HTTPS) connections
//        [-browser]              Start a browser connecting to the web server
//        [-tcp]                  Start the TCP server
//        [-tcpAllowOthers]       Allow other computers to connect - see below
//        [-tcpDaemon]            Use a daemon thread
//        [-tcpPort <port>]       The port (default: 9092)
//        [-tcpSSL]               Use encrypted (SSL) connections
//        [-tcpPassword <pwd>]    The password for shutting down a TCP server
//        [-tcpShutdown "<url>"]  Stop the TCP server; example: tcp://localhost
//        [-tcpShutdownForce]     Do not wait until all connections are closed
//        [-pg]                   Start the PG server
//        [-pgAllowOthers]        Allow other computers to connect - see below
//        [-pgDaemon]             Use a daemon thread
//        [-pgPort <port>]        The port (default: 5435)
//        [-properties "<dir>"]   Server properties (default: ~, disable: null)
//        [-baseDir <dir>]        The base directory for H2 databases (all servers)
//        [-ifExists]             Only existing databases may be opened (all servers)
//        [-trace]                Print additional trace information (all servers)

        List<String> args = new ArrayList<>();
        if (StringUtils.isNotEmpty(config.getBaseDir())) {
            args.add("-baseDir");
            args.add(getRelativeBaseDirPath(config.getBaseDir()));
        }
        if (config.isTcpSSL()) {
            args.add("-tcpSSL");
        }
        if (config.getPort() > 0) {
            args.add("-tcpPort");
            args.add(Integer.toString(config.getPort()));
        }
        // Allows auto-creation of remote database, which is a security hole and was forbidden
        // from 1.4.198, see https://h2database.com/html/tutorial.html#creating_new_databases
//        args.add("-ifNotExists"); // we're using <198 version now

        return args.toArray(new String[0]);
    }

    private String getRelativeBaseDirPath(String baseDir) {
        String path = new File(".").toURI().relativize(new File(baseDir).toURI()).getPath();
        if (Strings.isNullOrEmpty(path)) {
            path = ".";
        }
        return path;
    }

    @PreDestroy
    public void destroy() {
        if (!sqlConfiguration.isUsingH2()) {
            return;
        }

        LOGGER.info("Waiting " + POOL_CLOSE_WAIT + " ms for the connection pool to be closed.");
        try {
            Thread.sleep(POOL_CLOSE_WAIT);
        } catch (InterruptedException e) {
            // just ignore
        }

        if (sqlConfiguration.isAsServer()) {
            LOGGER.info("Shutting down embedded H2");
            if (server != null && server.isRunning(true)) {
                server.stop();
            }
        } else {
            LOGGER.info("H2 running as local instance (from file); waiting " + H2_CLOSE_WAIT + " ms for the DB to be closed.");
            try {
                Thread.sleep(H2_CLOSE_WAIT);
            } catch (InterruptedException e) {
                // just ignore
            }
        }

        LOGGER.info("H2 database shutdown complete.");
    }
}
