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

import org.apache.commons.configuration.Configuration;

/**
 * This class is used for SQL repository configuration. It reads values from Apache configuration object (xml).
 *
 * @author lazyman
 */
public class SqlRepositoryConfiguration {

    //embedded configuration
    private boolean embedded = true;
    private boolean asServer = false;
    private String baseDir = "~/";
    private boolean tcpSSL = false;
    private int port = 5437;
    //connection for hibernate
    private String driverClassName = "org.h2.Driver";
    private String jdbcUrl = "jdbc:h2:file:~/midpoint";
    private String jdbcUsername = "sa";
    private String jdbcPassword = "";
    private String hibernateDialect = "org.hibernate.dialect.H2Dialect";
    private String hibernateHbm2ddl = "update";

    public SqlRepositoryConfiguration(Configuration configuration) {
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

    /**
     * Value represents hibernate dialect used to communicate with database. You can choose from
     * <a href="http://docs.jboss.org/hibernate/core/4.0/manual/en-US/html/session-configuration.html#configuration-optional-dialects">dialects</a>
     * <p/>
     * It's used in "hibernate.dialect" property
     *
     * @return hibernate dialect
     */
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isTcpSSL() {
        return tcpSSL;
    }

    public void setTcpSSL(boolean tcpSSL) {
        this.tcpSSL = tcpSSL;
    }
}
