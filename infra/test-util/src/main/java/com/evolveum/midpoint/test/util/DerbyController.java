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

/**
 * 
 * @author Radovan Semancik
 *
 */
public class DerbyController {
	private NetworkServerControl server;
	private String listenHostname;
	private InetAddress listenAddress;
	private int listentPort;
	private String jdbcUrl;
	private String dbName;
	private String username = "midpoint";
	private String password = "secret";
	
	public DerbyController() {
		super();
		listenHostname = "localhost";
		this.listentPort = 1527;
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
		stmt.execute("create table users(login varchar(50),password varchar(50),full_name varchar(51),changelog int)");
        //stmt.execute("insert into account values ('1','1','value1',3)");
        conn.commit();
	}

	public void start() throws Exception {
		listenAddress = InetAddress.getByName(listenHostname);
		jdbcUrl = "jdbc:derby:"+dbName+";create=true;user="+username+";password="+password;
		server = new NetworkServerControl(listenAddress,listentPort);
		System.setProperty("derby.stream.error.file", "target/derby.log");
		server.start(null);
	}
	
	public void stop() throws Exception {
		server.shutdown();
	}
	
}
