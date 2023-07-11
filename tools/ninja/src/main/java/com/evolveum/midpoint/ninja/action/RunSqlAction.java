package com.evolveum.midpoint.ninja.action;

import static com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration.PROPERTY_DATASOURCE;
import static com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration.PROPERTY_JDBC_URL;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.AuditFactory;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.InputParameterException;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditServiceFactory;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;

public class RunSqlAction extends Action<RunSqlOptions, Void> {

    @Override
    public String getOperationName() {
        return "run sql scripts";
    }

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        if (preferCustomJdbcConnection(allOptions)) {
            return NinjaApplicationContextLevel.NONE;
        }

        return NinjaApplicationContextLevel.STARTUP_CONFIGURATION;
    }

    private boolean preferCustomJdbcConnection(List<Object> allOptions) {
        RunSqlOptions opts = NinjaUtils.getOptions(allOptions, RunSqlOptions.class);
        if (opts != null && StringUtils.isNotEmpty(opts.getJdbcUrl()) && StringUtils.isNotEmpty(opts.getJdbcUsername())) {
            return true;
        }

        ConnectionOptions connectionOpts = NinjaUtils.getOptions(allOptions, ConnectionOptions.class);
        return connectionOpts == null || StringUtils.isEmpty(connectionOpts.getMidpointHome());
    }

    @Override
    public Void execute() throws Exception {
        RunSqlOptions.Mode mode = options.getMode();
        if (mode == null) {
            mode = preferCustomJdbcConnection(context.getAllOptions()) ? RunSqlOptions.Mode.RAW : RunSqlOptions.Mode.REPOSITORY;
        }

        setScriptsDefaults(mode, options);

        if (mode == RunSqlOptions.Mode.RAW || preferCustomJdbcConnection(context.getAllOptions())) {
            log.info("Running scripts in raw mode using custom JDBC url/username/password options.");

            if (options.getScripts().isEmpty()) {
                return null;
            }

            // setup custom datasource
            try (HikariDataSource dataSource = setupCustomDataSource()) {
                executeScripts(dataSource, options.getScripts());
            }

            return null;
        }

        log.info("Running scripts against midpoint {}.", mode.name().toLowerCase());

        final ApplicationContext applicationContext = context.getApplicationContext();
        final MidpointConfiguration midpointConfiguration = applicationContext.getBean(MidpointConfiguration.class);

        DataSource repositoryDataSource = null;
        DataSource auditDataSource = null;
        try {
            Configuration configuration = midpointConfiguration.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION);
            repositoryDataSource = createDataSource(configuration, "ninja-repository");
            if (mode == RunSqlOptions.Mode.REPOSITORY) {
                executeScripts(repositoryDataSource, options.getScripts());

                return null;
            }

            if (mode == RunSqlOptions.Mode.AUDIT) {
                auditDataSource = createAuditDataSource(repositoryDataSource, midpointConfiguration);
                if (auditDataSource != null) {
                    executeScripts(auditDataSource, options.getScripts());
                } else {
                    log.error("Audit configuration not found in " + midpointConfiguration.getMidpointHome() + "/config.xml");
                }
            }
        } finally {
            closeQuietly(repositoryDataSource);
            closeQuietly(auditDataSource);
        }

        return null;
    }

    private void setScriptsDefaults(RunSqlOptions.Mode mode, RunSqlOptions options) {
        if (!options.getScripts().isEmpty()) {
            return;
        }

        if (options.getCreate()) {
            options.setScripts(mode.createScripts);
        } else if (options.getUpgrade()) {
            options.setScripts(mode.updateScripts);
        }
    }

    private HikariDataSource setupCustomDataSource() {
        if (StringUtils.isEmpty(options.getJdbcUrl())) {
            throw new InputParameterException("JDBC url parameter not defined");
        }

        if (StringUtils.isEmpty(options.getJdbcUsername())) {
            throw new InputParameterException("JDBC username parameter not defined");
        }

        if (StringUtils.isEmpty(options.getPassword())) {
            throw new InputParameterException("JDBC password parameter not defined");
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(options.getJdbcUrl());
        config.setUsername(options.getJdbcUsername());
        config.setPassword(options.getPassword());

        config.setDriverClassName("org.postgresql.Driver");
        config.setAutoCommit(true);
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(5);
        config.setPoolName("ninja-custom-jdbc");

        return new HikariDataSource(config);
    }

    private DataSource createDataSource(Configuration configuration, String name) throws RepositoryServiceFactoryException {
        log.info("Creating connection for " + name);
        SqaleRepositoryConfiguration repositoryConfiguration = new SqaleRepositoryConfiguration(configuration);
        repositoryConfiguration.init();
        DataSourceFactory dataSourceFactory = new DataSourceFactory(repositoryConfiguration);

        return dataSourceFactory.createDataSource(name);
    }

    private DataSource createAuditDataSource(DataSource repositoryDataSource, MidpointConfiguration midpointConfiguration)
            throws RepositoryServiceFactoryException {

        Configuration config = midpointConfiguration.getConfiguration(MidpointConfiguration.AUDIT_CONFIGURATION);
        List<HierarchicalConfiguration<ImmutableNode>> auditServices =
                ((BaseHierarchicalConfiguration) config).configurationsAt(AuditFactory.CONF_AUDIT_SERVICE);

        Configuration auditServiceConfig = null;
        for (Configuration serviceConfig : auditServices) {
            String className = serviceConfig.getString(AuditFactory.CONF_AUDIT_SERVICE_FACTORY);
            if (SqaleAuditServiceFactory.class.getName().equals(className)) {
                auditServiceConfig = serviceConfig;
                break;
            }
        }

        if (auditServiceConfig == null) {
            return null;
        }

        if (auditServiceConfig.getString(PROPERTY_JDBC_URL) == null
                && auditServiceConfig.getString(PROPERTY_DATASOURCE) == null) {
            return repositoryDataSource;
        }

        return createDataSource(auditServiceConfig, "ninja-audit");
    }

    private void executeScripts(@NotNull DataSource dataSource, @NotNull List<File> scripts) throws IOException, SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean autocommit = connection.getAutoCommit();
            connection.setAutoCommit(true);

            try {
                for (File script : scripts) {
                    log.info("Executing script {}", script.getPath());

                    Statement stmt = connection.createStatement();

                    String sql = FileUtils.readFileToString(script, StandardCharsets.UTF_8);
                    boolean hasResult = stmt.execute(sql);
                    if (options.getResult()) {
                        int index = 0;
                        printStatementResults(stmt, hasResult, index);

                        while (true) {
                            hasResult = stmt.getMoreResults();
                            index++;

                            printStatementResults(stmt, hasResult, index);

                            if (!hasResult && stmt.getUpdateCount() == -1) {
                                break;
                            }
                        }
                    }

                    stmt.close();
                }
            } finally {
                connection.setAutoCommit(autocommit);
            }
        }
    }

    private void printStatementResults(Statement stmt, boolean hasResult, int index) throws SQLException {
        String resultHeader = ConsoleFormat.formatInfoMessageWithParameter("Result #", index) + ": ";
        if (!hasResult) {
            int updateCount = stmt.getUpdateCount();
            if (updateCount != -1) {
                context.out.println(resultHeader + updateCount + " updated rows ");
            }
            return;
        }

        context.out.println(resultHeader);
        try (ResultSet set = stmt.getResultSet()) {
            printResultSet(set);
        }
    }

    private void printResultSet(ResultSet set) throws SQLException {
        ResultSetMetaData metaData = set.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<String> row = new ArrayList<>();

        for (int i = 1; i <= columnCount; i++) {
            row.add(metaData.getColumnLabel(i));
        }

        context.out.println(StringUtils.join(row, "|"));
        row.clear();

        while (set.next()) {
            for (int i = 1; i <= columnCount; i++) {
                Object obj = set.getObject(i);
                row.add(obj != null ? obj.toString() : "");
            }

            context.out.println(StringUtils.join(row, "|"));
            row.clear();
        }
    }

    private void closeQuietly(DataSource dataSource) {
        if (dataSource instanceof Closeable) {
            IOUtils.closeQuietly((Closeable) dataSource);
        }
    }
}
