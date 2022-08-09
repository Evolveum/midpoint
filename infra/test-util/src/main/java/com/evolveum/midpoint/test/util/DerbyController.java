/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import java.net.InetAddress;
import java.sql.*;

import org.apache.derby.drda.NetworkServerControl;
import org.testng.TestException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 */
public class DerbyController {

    public static final int DEFAULT_LISTEN_PORT = 11527;

    public static final String COLUMN_LOGIN = "login";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_FULL_NAME = "full_name";
    public static final String COLUMN_CHANGELOG = "changelog";

    private NetworkServerControl server;
    private final String listenHostname;
    private final int listenPort;
    private String jdbcUrl;
    private final String dbName;
    private final String username = "midpoint";
    private final String password = "secret";

    private static final Trace LOGGER = TraceManager.getTrace(DerbyController.class);

    public DerbyController() {
        this("target/derbyMidPointTest", "localhost", DEFAULT_LISTEN_PORT);
    }

    public DerbyController(String dbName, String listenHostname, int listenPort) {
        this.listenHostname = listenHostname;
        this.listenPort = listenPort;
        this.dbName = dbName;
    }

    public String getListenHostname() {
        return listenHostname;
    }

    public int getListenPort() {
        return listenPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(jdbcUrl, "", "");
        } catch (SQLTransientException e) {
            try {
                // Let's try one more time after a second.
                Thread.sleep(1000L);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            return DriverManager.getConnection(jdbcUrl, "", "");
        }
    }

    public void startCleanServer() throws Exception {
        start();
        cleanup();
    }

    private void cleanup() throws SQLException {
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        try {
            stmt.execute("drop table users");
        } catch (SQLException ex) {
            // Ignore. The table may not exist
        }
        stmt.execute("create table users(" +
                COLUMN_LOGIN + " varchar(50)," +
                COLUMN_PASSWORD + " varchar(50), " +
                COLUMN_FULL_NAME + " varchar(51), " +
                COLUMN_CHANGELOG + " int)");
        //stmt.execute("insert into account values ('1','1','value1',3)");
        conn.commit();
    }

    public void start() throws Exception {
        LOGGER.info("Starting Derby embedded network server " + listenHostname + ":" + listenPort + ", database " + dbName);
        InetAddress listenAddress = InetAddress.getByName(listenHostname);
        jdbcUrl = "jdbc:derby:" + dbName + ";create=true;user=" + username + ";password=" + password;
        server = new NetworkServerControl(listenAddress, listenPort);
        System.setProperty("derby.stream.error.file", "target/derby.log");
        server.start(null);
        boolean dbServerOk = false;
        int maxPings = 600; // times 100 ms lower => 1 min max
        do {
            try {
                Thread.sleep(100);
                server.ping();
                dbServerOk = true;
                // There is slight possibility that getConnection() still times out if called immediately.
                Thread.sleep(50);
                return; // OK, started, let's go out of the method
            } catch (Exception e) {
                LOGGER.debug("Derby embedded network server not ready yet...");
            }

            maxPings -= 1;
        } while (!dbServerOk && maxPings > 0);

        // Negative scenario, we ran out of maxPings attempts.
        LOGGER.warn("Derby DB did not start on time, trying to shut it down.");
        stop();
        throw new TestException("Derby DB did not start on time");
    }

    public void stop() throws Exception {
        LOGGER.info("Stopping Derby embedded network server");
        server.shutdown();
    }
}
