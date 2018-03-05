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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.h2.tools.Server;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SqlRepositoryFactory implements RepositoryServiceFactory {

	private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryFactory.class);
    private static final long C3P0_CLOSE_WAIT = 500L;
    private static final long H2_CLOSE_WAIT = 2000L;
	private static final String H2_IMPLICIT_RELATIVE_PATH = "h2.implicitRelativePath";
	private boolean initialized;
    private SqlRepositoryConfiguration sqlConfiguration;
    private Server server;

    private SqlPerformanceMonitor performanceMonitor;

    public SqlRepositoryConfiguration getSqlConfiguration() {
        Validate.notNull(sqlConfiguration, "Sql repository configuration not available (null).");
        return sqlConfiguration;
    }

    @Override
    public synchronized void destroy() throws RepositoryServiceFactoryException {
        if (!initialized) {
            LOGGER.info("SQL repository was not initialized, nothing to destroy.");
            return;
        }

        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
        }

        if (!getSqlConfiguration().isEmbedded()) {
            LOGGER.info("Repository is not running in embedded mode, shutdown complete.");
            return;
        }

        LOGGER.info("Waiting " + C3P0_CLOSE_WAIT + " ms for the connection pool to be closed.");
        try {
            Thread.sleep(C3P0_CLOSE_WAIT);
        } catch (InterruptedException e) {
            // just ignore
        }

        if (getSqlConfiguration().isAsServer()) {
            LOGGER.info("Shutting down embedded H2");
            if (server != null && server.isRunning(true))
                server.stop();
        } else {
            LOGGER.info("H2 running as local instance (from file); waiting " + H2_CLOSE_WAIT + " ms for the DB to be closed.");
            try {
                Thread.sleep(H2_CLOSE_WAIT);
            } catch (InterruptedException e) {
                // just ignore
            }

        }

        LOGGER.info("Shutdown complete.");

        initialized = false;
    }

    @Override
    public void destroyService(RepositoryService service) throws RepositoryServiceFactoryException {
        //we don't need destroying service objects, they will be GC correctly
    }

    @Override
    public synchronized void init(Configuration configuration) throws RepositoryServiceFactoryException {
        if (initialized) {
            LOGGER.info("SQL repository already initialized.");
            return;
        }
        Validate.notNull(configuration, "Configuration must not be null.");

        LOGGER.info("Initializing SQL repository factory");
        SqlRepositoryConfiguration config = new SqlRepositoryConfiguration(configuration);
        config.validate();
        sqlConfiguration = config;

        if (config.isUsingH2()) {
            if (System.getProperty(H2_IMPLICIT_RELATIVE_PATH) == null) {
                System.setProperty(H2_IMPLICIT_RELATIVE_PATH, "true");        // to ensure backwards compatibility (H2 1.3.x)
            }
        }

        if (config.isEmbedded()) {
            dropDatabaseIfExists(config);
            if (config.isAsServer()) {
                LOGGER.info("Starting h2 in server mode.");
                startServer();
            } else {
                LOGGER.info("H2 prepared to run in local mode (from file).");
            }
            LOGGER.info("H2 files are in '{}'.", new File(config.getBaseDir()).getAbsolutePath());
        } else {
            LOGGER.info("Repository is not running in embedded mode.");
        }

        performanceMonitor = new SqlPerformanceMonitor();
        performanceMonitor.initialize(this);

        ExtItemDictionary.getInstance().initialize();

        LOGGER.info("Repository initialization finished.");

        initialized = true;
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
        return new SqlRepositoryServiceImpl(this);
    }

    private String getRelativeBaseDirPath(String baseDir) {
        String path = new File(".").toURI().relativize(new File(baseDir).toURI()).getPath();
        if (StringUtils.isEmpty(path)) {
            path = ".";
        }
        return path;
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
        	LOGGER.error("Reported IO error, while binding ServerSocket to port "+port+" used to test availability " +
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

    private void startServer() throws RepositoryServiceFactoryException {
        SqlRepositoryConfiguration config = getSqlConfiguration();
        checkPort(config.getPort());

        try {
        	String[] serverArguments = createArguments(config);
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

        List<String> args = new ArrayList<String>();
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

        return args.toArray(new String[args.size()]);
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

            File[] tempFiles = file.listFiles((parent, name) -> {
				if (name.matches("^" + fileName + "\\.[0-9]*\\.temp\\.db$")) {
					return true;
				}
				return false;
			});
            if (tempFiles != null) {
                for (File temp : tempFiles) {
                    removeFile(temp);
                }
            }

            File lobDir = new File(file, fileName + ".lobs.db");
            if (lobDir.exists() && lobDir.isDirectory()) {
                LOGGER.info("Deleting directory '{}'", new Object[]{lobDir.getAbsolutePath()});
                FileUtils.deleteDirectory(lobDir);
            }
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException("Couldn't drop existing database files, reason: "
                    + ex.getMessage(), ex);
        }
    }

    private void removeFile(File file) throws IOException {
        if (file.exists()) {
            LOGGER.info("Deleting file '{}', result: {}", new Object[]{file.getAbsolutePath(), file.delete()});
        } else {
            LOGGER.info("File '{}' doesn't exist: delete status {}", new Object[]{file.getAbsolutePath(), file.delete()});
        }
    }

    public SqlPerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }
}
