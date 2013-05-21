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

import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sql.util.MidPointConnectionCustomizer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

/**
 * This class is used for SQL repository configuration. It reads values from Apache configuration object (xml).
 *
 * @author lazyman
 */
public class SqlRepositoryConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryConfiguration.class);

    public static final String PROPERTY_BASE_DIR = "baseDir";
    public static final String PROPERTY_DROP_IF_EXISTS = "dropIfExists";
    public static final String PROPERTY_AS_SERVER = "asServer";
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_FILE_NAME = "fileName";
    public static final String PROPERTY_TCP_SSL = "tcpSSL";
    public static final String PROPERTY_EMBEDDED = "embedded";
    public static final String PROPERTY_DRIVER_CLASS_NAME = "driverClassName";
    public static final String PROPERTY_HIBERNATE_HBM2DDL = "hibernateHbm2ddl";
    public static final String PROPERTY_HIBERNATE_DIALECT = "hibernateDialect";
    public static final String PROPERTY_JDBC_PASSWORD = "jdbcPassword";
    public static final String PROPERTY_JDBC_USERNAME = "jdbcUsername";
    public static final String PROPERTY_JDBC_URL = "jdbcUrl";
    public static final String PROPERTY_DATASOURCE = "dataSource";

    // concurrency properties
    public static final String PROPERTY_TRANSACTION_ISOLATION = "transactionIsolation";
    public static final String PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE = "lockForUpdateViaHibernate";
    public static final String PROPERTY_LOCK_FOR_UPDATE_VIA_SQL = "lockForUpdateViaSql";
    public static final String PROPERTY_USE_READ_ONLY_TRANSACTIONS = "useReadOnlyTransactions";
    public static final String PROPERTY_PERFORMANCE_STATISTICS_FILE = "performanceStatisticsFile";
    public static final String PROPERTY_PERFORMANCE_STATISTICS_LEVEL = "performanceStatisticsLevel";

    //other
    public static final String PROPERTY_ITERATIVE_SEARCH_BY_PAGING = "iterativeSearchByPaging";
    public static final String PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE = "iterativeSearchByPagingBatchSize";

    //embedded configuration
    private boolean embedded = true;
    private boolean asServer;
    private String baseDir;
    private String fileName;
    private boolean tcpSSL;
    private int port = 5437;
    private boolean dropIfExists;
    //connection for hibernate
    private String driverClassName;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;
    private String hibernateDialect;
    private String hibernateHbm2ddl;
    private String dataSource;

    private TransactionIsolation transactionIsolation;
    private boolean lockForUpdateViaHibernate;
    private boolean lockForUpdateViaSql;
    private boolean useReadOnlyTransactions;
    private String performanceStatisticsFile;
    private int performanceStatisticsLevel;

    private boolean iterativeSearchByPaging;
    private int iterativeSearchByPagingBatchSize;

    public SqlRepositoryConfiguration(Configuration configuration) {
        setAsServer(configuration.getBoolean(PROPERTY_AS_SERVER, asServer));
        setBaseDir(configuration.getString(PROPERTY_BASE_DIR, baseDir));
        setDriverClassName(configuration.getString(PROPERTY_DRIVER_CLASS_NAME, driverClassName));
        setEmbedded(configuration.getBoolean(PROPERTY_EMBEDDED, embedded));
        setHibernateDialect(configuration.getString(PROPERTY_HIBERNATE_DIALECT, hibernateDialect));
        setHibernateHbm2ddl(configuration.getString(PROPERTY_HIBERNATE_HBM2DDL, hibernateHbm2ddl));
        setJdbcPassword(configuration.getString(PROPERTY_JDBC_PASSWORD, jdbcPassword));
        setJdbcUrl(configuration.getString(PROPERTY_JDBC_URL, jdbcUrl));
        setJdbcUsername(configuration.getString(PROPERTY_JDBC_USERNAME, jdbcUsername));
        setPort(configuration.getInt(PROPERTY_PORT, port));
        setTcpSSL(configuration.getBoolean(PROPERTY_TCP_SSL, tcpSSL));
        setFileName(configuration.getString(PROPERTY_FILE_NAME, fileName));
        setDropIfExists(configuration.getBoolean(PROPERTY_DROP_IF_EXISTS, dropIfExists));
        setDataSource(configuration.getString(PROPERTY_DATASOURCE, null));

        computeDefaultConcurrencyParameters();

        setTransactionIsolation(configuration.getString(PROPERTY_TRANSACTION_ISOLATION, transactionIsolation.value()));
        setLockForUpdateViaHibernate(configuration.getBoolean(PROPERTY_LOCK_FOR_UPDATE_VIA_HIBERNATE, lockForUpdateViaHibernate));
        setLockForUpdateViaSql(configuration.getBoolean(PROPERTY_LOCK_FOR_UPDATE_VIA_SQL, lockForUpdateViaSql));
        setUseReadOnlyTransactions(configuration.getBoolean(PROPERTY_USE_READ_ONLY_TRANSACTIONS, useReadOnlyTransactions));
        setPerformanceStatisticsFile(configuration.getString(PROPERTY_PERFORMANCE_STATISTICS_FILE, performanceStatisticsFile));
        setPerformanceStatisticsLevel(configuration.getInt(PROPERTY_PERFORMANCE_STATISTICS_LEVEL, performanceStatisticsLevel));

        computeDefaultIterativeSearchParameters();

        setIterativeSearchByPaging(configuration.getBoolean(PROPERTY_ITERATIVE_SEARCH_BY_PAGING, iterativeSearchByPaging));
        setIterativeSearchByPagingBatchSize(configuration.getInt(PROPERTY_ITERATIVE_SEARCH_BY_PAGING_BATCH_SIZE, iterativeSearchByPagingBatchSize));
    }

    private void computeDefaultConcurrencyParameters() {
        if (hibernateDialect == null || hibernateDialect.equals("org.hibernate.dialect.H2Dialect")) {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = false;        // h2 does not support "SET TRANSACTION READ ONLY" command
        } else if (hibernateDialect.equals("org.hibernate.dialect.MySQL5InnoDBDialect")) {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = true;
        } else if (hibernateDialect.equals("org.hibernate.dialect.Oracle10gDialect")) {
            transactionIsolation = TransactionIsolation.READ_COMMITTED;
            lockForUpdateViaHibernate = true;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = true;
        } else if (hibernateDialect.equals("org.hibernate.dialect.PostgreSQLDialect")
                || hibernateDialect.equals("org.hibernate.dialect.PostgresPlusDialect")) {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = true;
        } else if (hibernateDialect.equals("com.evolveum.midpoint.repo.sql.util.UnicodeSQLServer2008Dialect")) {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = false;
        } else {
            transactionIsolation = TransactionIsolation.SERIALIZABLE;
            lockForUpdateViaHibernate = false;
            lockForUpdateViaSql = false;
            useReadOnlyTransactions = true;
            LOGGER.warn("Fine-tuned concurrency parameters defaults for hibernate dialect " + hibernateDialect
                    + " not found; using the following defaults: transactionIsolation = " + transactionIsolation
                    + ", lockForUpdateViaHibernate = " + lockForUpdateViaHibernate
                    + ", lockForUpdateViaSql = " + lockForUpdateViaSql
                    + ", useReadOnlyTransactions = " + useReadOnlyTransactions
                    + ". Please override them if necessary.");
        }
    }

    private void computeDefaultIterativeSearchParameters() {
        if (hibernateDialect == null || hibernateDialect.equals("org.hibernate.dialect.H2Dialect")) {
            iterativeSearchByPaging = true;
            iterativeSearchByPagingBatchSize = 100;
        } else {
            iterativeSearchByPaging = false;
        }
    }

    /**
     * Configuration validation.
     *
     * @throws RepositoryServiceFactoryException
     *          if configuration is invalid.
     */
    public void validate() throws RepositoryServiceFactoryException {
        if (StringUtils.isEmpty(getDataSource())) {
            notEmpty(getJdbcUrl(), "JDBC Url is empty or not defined.");
            notEmpty(getJdbcUsername(), "JDBC user name is empty or not defined.");
            notNull(getJdbcPassword(), "JDBC password is not defined.");
            notEmpty(getDriverClassName(), "Driver class name is empty or not defined.");
        }

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

    /**
     * @return Returns true if repository is running in embedded server mode, otherwise false. Default is false.
     */
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

    /**
     * @return Password for JDBC connection. (Optional)
     */
    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * @return JDBC URL connection string for hibernate data source. (for embedded mode it's created automatically).
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * @return Username for JDBC connection. (Optional)
     */
    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    /**
     * @return Port number if repository is running in embedded server mode. Default is 5437.
     */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Value represents repository running in embedded server mode with SSL turned on/off. Default value is false.
     *
     * @return Returns true if repository is running in embedded server mode and SSL turned on.
     */
    public boolean isTcpSSL() {
        return tcpSSL;
    }

    public void setTcpSSL(boolean tcpSSL) {
        this.tcpSSL = tcpSSL;
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

    public boolean isDropIfExists() {
        return dropIfExists;
    }

    public void setDropIfExists(boolean dropIfExists) {
        this.dropIfExists = dropIfExists;
    }

    public TransactionIsolation getTransactionIsolation() {
        return transactionIsolation;
    }

    public void setTransactionIsolation(TransactionIsolation transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }

    public void setTransactionIsolation(String transactionIsolation) {
        this.transactionIsolation = TransactionIsolation.fromValue(transactionIsolation);

        // ugly hack, but I know of no way to work around
        MidPointConnectionCustomizer.setTransactionIsolation(this.transactionIsolation);
    }

    public boolean isLockForUpdateViaHibernate() {
        return lockForUpdateViaHibernate;
    }

    public void setLockForUpdateViaHibernate(boolean lockForUpdateViaHibernate) {
        this.lockForUpdateViaHibernate = lockForUpdateViaHibernate;
    }

    public boolean isLockForUpdateViaSql() {
        return lockForUpdateViaSql;
    }

    public void setLockForUpdateViaSql(boolean lockForUpdateViaSql) {
        this.lockForUpdateViaSql = lockForUpdateViaSql;
    }

    public boolean isUseReadOnlyTransactions() {
        return useReadOnlyTransactions;
    }

    public void setUseReadOnlyTransactions(boolean useReadOnlyTransactions) {
        this.useReadOnlyTransactions = useReadOnlyTransactions;
    }

    public String getPerformanceStatisticsFile() {
        return performanceStatisticsFile;
    }

    public void setPerformanceStatisticsFile(String performanceStatisticsFile) {
        this.performanceStatisticsFile = performanceStatisticsFile;
    }

    public int getPerformanceStatisticsLevel() {
        return performanceStatisticsLevel;
    }

    public void setPerformanceStatisticsLevel(int performanceStatisticsLevel) {
        this.performanceStatisticsLevel = performanceStatisticsLevel;
    }

    public boolean isIterativeSearchByPaging() {
        return iterativeSearchByPaging;
    }

    public void setIterativeSearchByPaging(boolean iterativeSearchByPaging) {
        this.iterativeSearchByPaging = iterativeSearchByPaging;
    }

    public int getIterativeSearchByPagingBatchSize() {
        return iterativeSearchByPagingBatchSize;
    }

    public void setIterativeSearchByPagingBatchSize(int iterativeSearchByPagingBatchSize) {
        this.iterativeSearchByPagingBatchSize = iterativeSearchByPagingBatchSize;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
}
