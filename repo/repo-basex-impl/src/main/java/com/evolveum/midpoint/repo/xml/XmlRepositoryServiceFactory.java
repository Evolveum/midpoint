/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms 
 * of the Common Development and Distribution License 
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or 
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, 
 * with the fields enclosed by brackets [] replaced by 
 * your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner] 
 * Portions Copyrighted 2011 Igor Farinic
 */
package com.evolveum.midpoint.repo.xml;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.basex.BaseXServer;
import org.basex.core.BaseXException;
import org.basex.server.ClientQuery;
import org.basex.server.ClientSession;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class XmlRepositoryServiceFactory implements RepositoryServiceFactory {

	private static final Trace LOGGER = TraceManager.getTrace(XmlRepositoryServiceFactory.class);

	private boolean dropDatabase = false;
	private boolean runServer = true;
	private boolean embedded = true;
	private boolean shutdown = false;
	private boolean debug = false;
	private String initialDataPath = "";
	private String host = "localhost";
	private int port = 1984;
	private int eventPort = 1985;
	private String username = "admin";
	private String password = "admin";
	private String databaseName = "midPoint";
	private String serverPath;
	private BaseXServer server;

	@Override
	public void init(Configuration config) throws RepositoryServiceFactoryException {
		// TODO: if no configuration is set then generate default configuration
		applyConfiguration(config);

		startServer();

		recreateDatabase();
	}

	/**
	 * Starts BaseX server. Based on configuration it will start BaseX server in
	 * daemon or standalone mode or won't start server at all.
	 *
	 * @throws RepositoryServiceFactoryException
	 */
	private void startServer() throws RepositoryServiceFactoryException {
		if (runServer) {
			// start BaseX server, it registers its own shutdown hook, therefore
			// no cleanup is required
			LOGGER.trace("Starting BaseX Server on {}:{}", host, port);

			if (StringUtils.isNotEmpty(serverPath)) {
				LOGGER.debug("BaseX Server base path: {}", serverPath);
			} else {
				LOGGER.debug("BaseX Server base not set, using default value");
			}
			StringBuffer commands = new StringBuffer();
			if (StringUtils.isNotEmpty(serverPath)) {
				if (StringUtils.equals("memory", serverPath)) {
					commands.append("-cset mainmem true;info");
				} else {
					commands.append("-cset dbpath ").append(serverPath).append(";info");
				}
			}

			String debugging = "";
			if (debug) {
				debugging = "-d";
			}

			// args ordering is important!
			if (embedded) {
				this.checkPort(port);
				this.checkPort(eventPort);
				// set debug mode and run it in the same process
				server = new BaseXServer("-p" + port, "-e" + eventPort, "-D", "-s", debugging, commands.toString());
			} else {
				// set debug mode and run it as a Daemon process
				server = new BaseXServer("-p" + port, "-e" + eventPort, "-s", debugging, commands.toString());
			}
			LOGGER.trace("BaseX Server started");
		}
	}

	/**
	 * (re)create of the database. It will drop old database only if Factory's
	 * property dropDatabase is set to true.
	 *
	 * @throws RepositoryServiceFactoryException
	 */
	private void recreateDatabase() throws RepositoryServiceFactoryException {
		boolean newDb = false;

		ClientSession session = null;
		try {
			LOGGER.trace("Creating BaseX client Session");
			session = new ClientSession(host, port, username, password);
			LOGGER.trace("BaseX client Session created");

			if (dropDatabase) {
				session.execute("DROP DATABASE " + databaseName);
			}

			try {
				session.execute("OPEN " + databaseName);
			} catch (BaseXException ex) {
				if (("Database '" + databaseName + "' was not found.").equals(ex.getMessage())) {
					newDb = true;
				}
			}
			if (newDb) {
				// create and initialize DB
				session.execute("CREATE DB " + databaseName + " " + initialDataPath);
				if (StringUtils.isEmpty(initialDataPath)) {
					// FIXME: remove hardcoded values
					String serializedObject = "<c:objects xmlns:c=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"/>";
					StringBuilder query = new StringBuilder()
							.append("declare namespace c='" + SchemaConstants.NS_C + "';\n").append("let $x := ")
							.append(serializedObject).append("\n").append("return insert node $x into doc(\"")
							.append(databaseName).append("\")");
					ClientQuery cq;
					cq = session.query(query.toString());
					cq.execute();
				}
			}
		} catch (BaseXException e) {
			throw new RepositoryServiceFactoryException("XML DB Exception during DB initialization", e);
		} catch (IOException e) {
			throw new RepositoryServiceFactoryException("XML DB IO Exception during DB initialization", e);
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (IOException e) {
					throw new RepositoryServiceFactoryException(
							"BaseX Client Session IO Exception during DB initialization", e);
				}
			}
		}
	}

	private void applyConfiguration(Configuration config) {
		if (config != null) {
			// apply default values, if property not found in configuration
			setDatabaseName(config.getString("databaseName", databaseName));
			setDropDatabase(config.getBoolean("dropDatabase", dropDatabase));
			setEmbedded(config.getBoolean("embedded", embedded));
			setHost(config.getString("host", host));
			setInitialDataPath(config.getString("initialDataPath", initialDataPath));
			setPassword(config.getString("password", password));
			setPort(config.getInt("port", port));
			setEventPort(config.getInt("eventPort", eventPort));
			setRunServer(config.getBoolean("runServer", runServer));
			setServerPath(config.getString("serverPath", serverPath));
			setShutdown(config.getBoolean("shutdown", shutdown));
			setUsername(config.getString("username", username));
			setDebug(config.getBoolean("debug", debug));
		} else {
			throw new IllegalStateException("Configuration has to be injected prior the initialization.");
		}
	}

	@Override
	public void destroy() {
		if (shutdown) {
			if (server != null) {
				LOGGER.info("Basex server committing to shutdown.");
				server.stop();
				LOGGER.info("Basex server is down.");
			}
		}
		LOGGER.info("Destroying BaseX server service.");
	}

	@Override
	public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
		RepositoryService repositoryService = new XmlRepositoryService(host, port, username, password, databaseName);
		return repositoryService;
	}

	public boolean isRunServer() {
		return runServer;
	}

	public void setRunServer(boolean runServer) {
		this.runServer = runServer;
	}

	public boolean isEmbedded() {
		return embedded;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public String getInitialDataPath() {
		return initialDataPath;
	}

	public void setInitialDataPath(String initialDataPath) {
		this.initialDataPath = initialDataPath;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getEventPort() {
		return eventPort;
	}

	public void setEventPort(int eventPort) {
		this.eventPort = eventPort;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public boolean isDropDatabase() {
		return dropDatabase;
	}

	public void setDropDatabase(boolean dropDatabase) {
		this.dropDatabase = dropDatabase;
	}

	public boolean isShutdown() {
		return shutdown;
	}

	public void setShutdown(boolean shutdown) {
		this.shutdown = shutdown;
	}

	public String getServerPath() {
		return serverPath;
	}

	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	private void checkPort(int port) throws RepositoryServiceFactoryException {
		ServerSocket ss = null;
		try {
			ss = new ServerSocket();
			ss.setReuseAddress(true);
			SocketAddress endpoint = new InetSocketAddress(port);
			ss.bind(endpoint);
		} catch (IOException e) {
			throw new RepositoryServiceFactoryException("BaseX port (" + port + ") already in use.", e);
		} finally {
			try {
				if (ss != null) {
					ss.close();
				}
			} catch (IOException e) {
				LOGGER.error(
						"Reported IO error, while closing ServerSocket used to test availability of port for BaseX Server",
						e);
			}
		}
	}
}
