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

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

/**
 * This class is used for SQL repository configuration. It reads values from Apache configuration object (xml).
 *
 * @author lazyman
 */
public class SqlRepositoryConfiguration {

    //embedded configuration
    private boolean embedded = true;
    private boolean asServer = false;
    private String baseDir;
    private String fileName;
    private boolean tcpSSL = false;
    private int port = 5437;
    //embedded jdbc url configuration properties
    //CACHE_SIZE
    private int cacheSize;        //todo use in initialization

    //connection for hibernate
    private String driverClassName;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private String hibernateDialect;
    private String hibernateHbm2ddl;

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
        setFileName(configuration.getString("fileName", fileName));
    }

    public void validate() throws RepositoryServiceFactoryException {
        notEmpty(getJdbcUrl(), "JDBC Url is empty or not defined.");
        notEmpty(getJdbcUsername(), "JDBC user name is empty or not defined.");
        notNull(getJdbcPassword(), "JDBC password is not defined.");

        notEmpty(getDriverClassName(), "Driver class name is empty or not defined.");

        notEmpty(getHibernateDialect(), "Hibernate dialect is empty or not defined.");
        notEmpty(getHibernateHbm2ddl(), "Hibernate hbm2ddl option is empty or not defined.");

        if (isEmbedded()) {
            notEmpty(getBaseDir(), "Base dir is empty or not defined.");
            if (isAsServer()) {
                if (getPort() < 0 || getPort() > 65535) {
                    throw new RepositoryServiceFactoryException("Port must be in interval (0-65535)");
                }
            }
        }
    }

    private void notNull(String value, String message) throws RepositoryServiceFactoryException {
        if (value == null) {
            throw new RepositoryServiceFactoryException(message);
        }
    }

    private void notEmpty(String value, String message) throws RepositoryServiceFactoryException {
        if (StringUtils.isEmpty(value)) {
            throw new RepositoryServiceFactoryException(message);
        }
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

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    /**
     * Used in embedded mode to define h2 database file name. Default will be "midpoint".
     *
     * @return name of DB file
     */
    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
