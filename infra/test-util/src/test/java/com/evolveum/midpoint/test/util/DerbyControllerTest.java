/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 *
 */
package com.evolveum.midpoint.test.util;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import org.testng.annotations.Test;

/**
 * @author Radovan Semancik
 *
 */
public class DerbyControllerTest {

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
		assertFalse("The \"users\" table is not empty",rs.next());
		rs.close();
		stmt.close();

		// Try insert
		stmt = conn.createStatement();
        stmt.execute("insert into users values ('jack','d3adm3nt3lln0t4l3s','cpt. Jack Sparrow',1)");
        conn.commit();

		// Try to connect over the "network" (localhost)
        Class.forName("org.apache.derby.jdbc.ClientDriver").newInstance();
		String networkJdbcUrl = "jdbc:derby://"+controller.getListenHostname()+":"+controller.getListentPort()+"/"+controller.getDbName();
		Properties props = new Properties();
		props.setProperty("user",controller.getUsername());
		props.setProperty("password",controller.getPassword());
		System.out.println("JDBC Connecting to "+networkJdbcUrl+" as "+controller.getUsername());
		Connection networkConn = DriverManager.getConnection(networkJdbcUrl,props);

		// Check if it empty
		stmt = networkConn.createStatement();
		stmt.execute("select * from users");
		rs = stmt.getResultSet();
		assertTrue("The \"users\" empty after insert",rs.next());
		rs.close();
		stmt.close();

		// stop derby
		controller.stop();
	}
}
