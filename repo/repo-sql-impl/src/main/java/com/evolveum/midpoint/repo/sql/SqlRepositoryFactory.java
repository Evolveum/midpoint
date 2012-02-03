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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.h2.tools.Server;

/**
 * @author lazyman
 */
public class SqlRepositoryFactory implements RepositoryServiceFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryFactory.class);

    private Configuration configuration;

    //embedded configuration
    private boolean embedded = true;
    private boolean asServer = false;
    private String baseDir = "~/h2";
    private boolean tcpSSL = false;
    private int port = 5435;
    //connection for hibernate
    private String driverClassName = "org.h2.Driver";
    private String jdbcUrl = "jdbc:h2:file:~/h2/midpoint";
    private String jdbcUsername = "midpoint";
    private String jdbcPassword = "midpoint";
    private String hibernateDialect = "org.hibernate.dialect.H2Dialect";
    private String hibernateHbm2ddl = "update";

    //embedded h2
    private Server server;

    public boolean isAsServer() {
        return asServer;
    }

    public void setAsServer(boolean asServer) {
        this.asServer = asServer;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    public boolean isTcpSSL() {
        return tcpSSL;
    }

    public void setTcpSSL(boolean tcpSSL) {
        this.tcpSSL = tcpSSL;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    public String getHibernateDialect() {
        return hibernateDialect;
    }

    public void setHibernateDialect(String hibernateDialect) {
        this.hibernateDialect = hibernateDialect;
    }

    public String getHibernateHbm2ddl() {
        return hibernateHbm2ddl;
    }

    public void setHibernateHbm2ddl(String hibernateHbm2ddl) {
        this.hibernateHbm2ddl = hibernateHbm2ddl;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    private void applyConfiguration() {
        if (configuration == null) {
            throw new IllegalStateException("Configuration has to be injected prior the initialization.");
        }

        setAsServer(configuration.getBoolean("asServer", asServer));
        setBaseDir(configuration.getString("baseDir", baseDir));
        setDriverClassName(configuration.getString("driverClassName", driverClassName));
        setEmbedded(configuration.getBoolean("embedded", embedded));
        setHibernateDialect(configuration.getString("hibernateDialect", hibernateDialect));
        setHibernateHbm2ddl(configuration.getString("hibernateHbm2ddl", hibernateHbm2ddl));
        setJdbcPassword(configuration.getString("jdbcPassword", jdbcPassword));
        setJdbcUrl(configuration.getString("jdbcUrl", jdbcUrl));
        setJdbcUsername(configuration.getString("jdbcUsername", jdbcUsername));
        setPort(configuration.getInt("port", port));
        setTcpSSL(configuration.getBoolean("tcpSSL", tcpSSL));
    }


    @Override
    public void destroy() throws RepositoryServiceFactoryException {
        if (!isEmbedded() || !isAsServer()) {
            LOGGER.info("Repository is not running in embedded mode, shutdown complete.");
            return;
        }

        LOGGER.info("Shutting down embedded h2");
        if (server != null && server.isRunning(true)) {
            server.stop();
        }
        LOGGER.info("Shutdown complete.");
    }

    private void startServer() throws RepositoryServiceFactoryException {
        StringBuilder args = new StringBuilder();
        args.append("-baseDir");
        args.append(getBaseDir());
        args.append(" ");
        if (isTcpSSL()) {
            args.append("-tcpSSL ");
        }
        args.append("-ifExists ");
        if (getPort() > 0) {
            args.append("-tcpPort");
            args.append(getPort());
            args.append(" ");
        }

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

        try {
            server = Server.createTcpServer(args.toString()).start();
            server.start();
        } catch (Exception ex) {
            throw new RepositoryServiceFactoryException(ex.getMessage(), ex);
        }
    }

    @Override
    public void init() throws RepositoryServiceFactoryException {
        LOGGER.info("Applying configuration");
        applyConfiguration();

        if (isEmbedded() && isAsServer()) {
            LOGGER.info("Starting h2 in server mode.");
            startServer();
        } else {
            LOGGER.info("Repository is not running in embedded mode, initialization complete.");
        }

        LOGGER.info("Running init script.");
        //todo init script

        LOGGER.info("Repository initialization finished.");

        //todo fix spring configuration somehow :)


    }

    @Override
    public RepositoryService getRepositoryService() throws RepositoryServiceFactoryException {
        return new SqlRepositoryServiceImpl();
    }

    @Override
    public void setConfiguration(Configuration config) {
        Validate.notNull(config, "Configuration must not be null.");
        this.configuration = config;
    }

    @Override
    public String getComponentId() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Configuration getCurrentConfiguration() {
        return configuration;
    }
}
