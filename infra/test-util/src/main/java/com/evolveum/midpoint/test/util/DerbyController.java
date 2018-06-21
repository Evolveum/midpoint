/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.derby.drda.NetworkServerControl;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *
 * @author Radovan Semancik
 *
 */
public class DerbyController {

	public static final int DEFAULT_LISTEN_PORT = 11527;

	public static String COLUMN_LOGIN = "login";
	public static String COLUMN_PASSWORD = "password";
	public static String COLUMN_FULL_NAME = "full_name";
	public static String COLUMN_CHANGELOG = "changelog";

	private NetworkServerControl server;
	private String listenHostname;
	private InetAddress listenAddress;
	private int listentPort;
	private String jdbcUrl;
	private String dbName;
	private String username = "midpoint";
	private String password = "secret";

	private static final Trace LOGGER = TraceManager.getTrace(DerbyController.class);

	public DerbyController() {
		super();
		listenHostname = "localhost";
		this.listentPort = DEFAULT_LISTEN_PORT;
		dbName = "target/derbyMidPointTest";
	}

	public DerbyController(String dbName, String listenHostname, int listentPort) {
		super();
		this.listenHostname = listenHostname;
		this.listentPort = listentPort;
		this.dbName = dbName;
	}

	public String getListenHostname() {
		return listenHostname;
	}

	public int getListentPort() {
		return listentPort;
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
		return DriverManager.getConnection(jdbcUrl, "", "");
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
				COLUMN_LOGIN + " varchar(50),"+
				COLUMN_PASSWORD + " varchar(50), "+
				COLUMN_FULL_NAME + " varchar(51), "+
				COLUMN_CHANGELOG + " int)");
        //stmt.execute("insert into account values ('1','1','value1',3)");
        conn.commit();
	}

	public void start() throws Exception {
		LOGGER.info("Starting Derby embedded network server "+listenHostname+":"+listentPort+", database "+dbName);
		listenAddress = InetAddress.getByName(listenHostname);
		jdbcUrl = "jdbc:derby:"+dbName+";create=true;user="+username+";password="+password;
		server = new NetworkServerControl(listenAddress,listentPort);
		System.setProperty("derby.stream.error.file", "target/derby.log");
		server.start(null);
	}

	public void stop() throws Exception {
		LOGGER.info("Stopping Derby embedded network server");
		server.shutdown();
	}

	public Statement getExecutedStatementWhereLoginName(String loginName) throws SQLException {
		Connection conn = getConnection();
		// Check if it empty
		Statement stmt = conn.createStatement();
		stmt.execute("select * from users where "+COLUMN_LOGIN+"='"+loginName+"'");
		return stmt;
	}

}
