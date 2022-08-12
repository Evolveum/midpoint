/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.testng.annotations.Test;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * @author Radovan Semancik
 */
public class DerbyControllerTest extends AbstractUnitTest {

    @Test
    public void testStartStopDerby() throws Exception {
        DerbyController controller = new DerbyController();

        // Start Derby
        controller.startCleanServer();

        Connection conn = controller.getConnection();

        // Check if it is empty
        Statement stmt = conn.createStatement();
        stmt.execute("select * from users");
        ResultSet rs = stmt.getResultSet();
        assertFalse("The \"users\" table is not empty", rs.next());
        rs.close();
        stmt.close();

        // Try insert
        stmt = conn.createStatement();
        stmt.execute("insert into users values ('jack','d3adm3nt3lln0t4l3s','cpt. Jack Sparrow',1)");
        conn.commit();

        // Try to connect over the "network" (localhost)
        Class.forName("org.apache.derby.jdbc.ClientDriver").getDeclaredConstructor().newInstance();
        String networkJdbcUrl = "jdbc:derby://" + controller.getListenHostname() + ":" + controller.getListenPort() + "/" + controller.getDbName();
        Properties props = new Properties();
        props.setProperty("user", controller.getUsername());
        props.setProperty("password", controller.getPassword());
        System.out.println("JDBC Connecting to " + networkJdbcUrl + " as " + controller.getUsername());
        Connection networkConn = DriverManager.getConnection(networkJdbcUrl, props);

        // Check if it empty
        stmt = networkConn.createStatement();
        stmt.execute("select * from users");
        rs = stmt.getResultSet();
        assertTrue("The \"users\" empty after insert", rs.next());
        rs.close();
        stmt.close();

        // stop derby
        controller.stop();
    }
}
