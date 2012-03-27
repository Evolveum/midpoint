/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactory;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.query.QueryRegistry;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.h2.tools.Server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 */
public class SqlRepositoryFactory implements RepositoryServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryFactory.class);
    private SqlRepositoryConfiguration sqlConfiguration;
    private Server server;

    public SqlRepositoryConfiguration getSqlConfiguration() {
        Validate.notNull(sqlConfiguration, "Sql repository configuration not available (null).");
        return sqlConfiguration;
    }

    @Override
    public void destroy() throws RepositoryServiceFactoryException {
        if (!getSqlConfiguration().isEmbedded()) {
            LOGGER.info("Repository is not running in embedded mode, shutdown complete.");
        }

        if (getSqlConfiguration().isAsServer()) {
            LOGGER.info("Shutting down embedded H2");
            if (server != null && server.isRunning(true))
                server.stop();
        } else {
            LOGGER.info("H2 running as local instance (from file).");
        }
        LOGGER.info("Shutdown complete.");
    }

    @Override
    public void destroyService(RepositoryService service) throws RepositoryServiceFactoryException {
        //we don't need destroying service objects, they will be GC correctly
    }

    @Override
    public void init(Configuration configuration) throws RepositoryServiceFactoryException {
        Validate.notNull(configuration, "Configuration must not be null.");

        LOGGER.info("Initializing SQL repository factory");
        SqlRepositoryConfiguration config = new SqlRepositoryConfiguration(configuration);
        normalizeConfiguration(config);
        config.validate();
        sqlConfiguration = config;

        if (getSqlConfiguration().isEmbedded()) {
            if (getSqlConfiguration().isAsServer()) {
                LOGGER.info("Starting h2 in server mode.");
                startServer();
            } else {
                LOGGER.info("H2 prepared to run in local mode (from file).");
            }
            LOGGER.info("H2 files are in '{}'.",
                    new Object[]{new File(sqlConfiguration.getBaseDir()).getAbsolutePath()});
        } else {
            LOGGER.info("Repository is not running in embedded mode.");
        }

        try {
            QueryRegistry.getInstance();
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException("Couldn't initialize query registry, reason: "
                    + ex.getMessage(), ex);
        }
        LOGGER.info("Repository initialization finished.");
    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
        return new SqlRepositoryServiceImpl();
    }

    /**
     * This method checks actual configuration and updates if it's in embedded mode. (build correct
     * jdbc url, sets default username and password, driver class and hibernate properties)
     *
     * @param config
     * @throws RepositoryServiceFactoryException
     *          this exception is thrown if baseDir defined in configuration xml doesn't exist or it's not a directory
     */
    private void normalizeConfiguration(SqlRepositoryConfiguration config) throws RepositoryServiceFactoryException {
        if (!config.isEmbedded()) {
            return;
        }

        if (StringUtils.isEmpty(config.getFileName())) {
            config.setFileName("midpoint");
        }

        File baseDir = new File(config.getBaseDir());
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new RepositoryServiceFactoryException("File '" + config.getBaseDir()
                    + "' defined as baseDir doesn't exist or is not directory.");
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:h2:");
        if (config.isAsServer()) {
            //jdbc:h2:tcp://<server>[:<port>]/[<path>]<databaseName>
            jdbcUrl.append("tcp://127.0.0.1:");
            jdbcUrl.append(config.getPort());
            jdbcUrl.append("/");
            jdbcUrl.append(config.getFileName());
        } else {
            //jdbc:h2:[file:][<path>]<databaseName>
            jdbcUrl.append("file:");

            File databaseFile = new File(config.getBaseDir(), config.getFileName());
            jdbcUrl.append(databaseFile.getAbsolutePath());
        }
        config.setJdbcUrl(jdbcUrl.toString());
        LOGGER.trace("JDBC url created: {}", new Object[]{config.getJdbcUrl()});

        config.setJdbcUsername("sa");
        config.setJdbcPassword("");

        config.setDriverClassName("org.h2.Driver");
        config.setHibernateDialect("org.hibernate.dialect.H2Dialect");
        config.setHibernateHbm2ddl("update");
    }
    
    private String getRelativeBaseDirPath(String baseDir) {
        return new File(".").toURI().relativize(new File(baseDir).toURI()).getPath();
    }

    private void checkPort(int port) throws RepositoryServiceFactoryException {
        if (port >= 65635 || port < 0) {
            throw new RepositoryServiceFactoryException("Port must be in range 0-65634, not '" + port + "'.");
        }

        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.setReuseAddress(true);
            SocketAddress endpoint = new InetSocketAddress(port);
            ss.bind(endpoint);
        } catch (IOException e) {
            throw new RepositoryServiceFactoryException("Configured port (" + port + ") for H2 already in use.", e);
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
            server = Server.createTcpServer(createArguments(config));
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
        // todo with this we can't create database on first start..
        // args.add("-ifExists");

        return args.toArray(new String[args.size()]);
    }
}
