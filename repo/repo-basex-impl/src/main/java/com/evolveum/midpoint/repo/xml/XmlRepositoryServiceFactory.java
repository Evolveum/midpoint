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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.basex.BaseXServer;
import org.basex.core.BaseXException;
import org.basex.server.ClientQuery;
import org.basex.server.ClientSession;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SystemException;

public class XmlRepositoryServiceFactory {

	private static final Trace TRACE = TraceManager.getTrace(XmlRepositoryServiceFactory.class);

	private boolean dropDatabase = false;
	private boolean runServer = true;
	private boolean embedded = true;
	// shutdown = true for standalone run, shutdown = false for test run
	private boolean shutdown = false;
	private String initialDataPath;
	private String host;
	private int port;
	private String username;
	private String password;
	private String databaseName;
	private String serverPath;
	
	private List<ClientSession> sessions = new ArrayList<ClientSession>();
	BaseXServer server;

	public void init() throws RepositoryServiceFactoryException {

		if (runServer) {
			// start BaseX server, it registers its own shutdown hook, therefore
			// no cleanup is required
			TRACE.trace("Starting BaseX Server on {}:{}", host, port);

			if (StringUtils.isNotEmpty(serverPath)) {
				TRACE.debug("BaseX Server base path: {}", serverPath);
	 	 	} else {
	 	 		TRACE.debug("BaseX Server base not set, using default value");
	 	 	}
	 	 	StringBuffer commands = new StringBuffer();
	 	 	if (StringUtils.isNotEmpty(serverPath)) {
	 	 		if (StringUtils.equals("memory", serverPath)) {
	 	 			commands.append("-cset mainmem true;info");
	 	 	 	} else {
	 	 	 		commands.append("-cset dbpath ").append(serverPath).append(";info");
	 	 	 	}
	 	 	}
	 	 	 	
			// args ordering is important!
			if (embedded) {
//				this.checkPort();
				// set debug mode and run it in the same process
				server = new BaseXServer("-p" + port, "-d", "-D", "-s", commands.toString());
			} else {
				// set debug mode and run it as a Daemon process
				server = new BaseXServer("-p" + port, "-d", "-s", commands.toString());
			}
			TRACE.trace("BaseX Server started");
		}

		boolean newDb = false;

		ClientSession session = null;
		try {
			TRACE.trace("Creating BaseX client Session");
			session = new ClientSession(host, port, username, password);
			TRACE.trace("BaseX client Session created");

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
							.append("declare namespace c='" + SchemaConstants.NS_C + "';\n")
							.append("let $x := ").append(serializedObject).append("\n")
							.append("return insert node $x into doc(\"").append(databaseName).append("\")");
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
							"XML DB Client Session IO Exception during DB initialization", e);
				}
			}
		}

	}

	public void destroy() {
		if (shutdown) {
			for (ClientSession session : sessions) {
				try {
					TRACE.trace("Closing XML DB Client session");
					session.close();
				} catch (IOException ex) {
					TRACE.error("Reported IO while closing session to XML Database", ex);
					throw new SystemException("Reported IO while closing session to XML Database", ex);
				}
			}

			if (server != null) {
				server.stop();
			}
		}
	}

	public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
		ClientSession session = null;
		try {
			session = new ClientSession(host, port, username, password);
			session.execute("OPEN " + databaseName);
			RepositoryService repositoryService = new XmlRepositoryService(session);
			return repositoryService;
		} catch (IOException e) {
			throw new RepositoryServiceFactoryException("XML DB IO Exception during client initialization", e);
		} catch (BaseXException e) {
			throw new RepositoryServiceFactoryException("XML DB Exception during client initialization", e);
		} finally {
			if (session != null) {
				sessions.add(session);
			}
		}
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
		
//	private void checkPort() throws RepositoryServiceFactoryException {
//		ServerSocket ss = null;
//		try {
//			ss = new ServerSocket(this.getPort());
//			ss.setReuseAddress(true);
//		} catch (IOException e) {
//			throw new RepositoryServiceFactoryException("BaseX port (" + this.getPort()
//					+ ") already in use.", e);
//		} finally {
//			try {
//				if (ss != null) {
//					ss.close();
//				}
//			} catch (IOException e) {
//				TRACE.error("Reported IO error, while closing ServerSocket used to test availability of port for BaseX Server", e);
//			}
//		}
//	}
}
