package com.evolveum.midpoint.ninja.action;

import static com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration.PROPERTY_DATASOURCE;
import static com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration.PROPERTY_JDBC_URL;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.AuditFactory;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditServiceFactory;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;

// todo proper logging
public class RunSqlAction extends Action<RunSqlOptions, Void> {

    @Override
    public @NotNull NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        ConnectionOptions opts = NinjaUtils.getOptions(allOptions, ConnectionOptions.class);
        if (opts == null || StringUtils.isEmpty(opts.getMidpointHome())) {
            // no midpoint-home option defined, custom jdbc url/username/password will be used
            return NinjaApplicationContextLevel.NONE;
        }

        return NinjaApplicationContextLevel.STARTUP_CONFIGURATION;
    }

    @Override
    public Void execute() throws Exception {
        final RunSqlOptions.Mode mode = options.getMode() != null ? options.getMode() : RunSqlOptions.Mode.REPOSITORY;

        setScriptsDefaults(mode, options);

        File scriptsDirectory = options.getScriptsDirectory();

        ConnectionOptions opts = context.getOptions(ConnectionOptions.class);
        if (mode == RunSqlOptions.Mode.RAW || opts == null || StringUtils.isEmpty(opts.getMidpointHome())) {
            log.info("Running scripts in raw mode using custom JDBC url/username/password options.");

            if (options.getScripts().isEmpty()) {
                return null;
            }

            // setup custom datasource
            try (HikariDataSource dataSource = setupCustomDataSource()) {
                executeScripts(dataSource, scriptsDirectory, options.getScripts());
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
                executeScripts(repositoryDataSource, scriptsDirectory, options.getScripts());

                return null;
            }

            if (mode == RunSqlOptions.Mode.AUDIT) {
                auditDataSource = createAuditDataSource(repositoryDataSource, midpointConfiguration);
                if (auditDataSource != null) {
                    executeScripts(auditDataSource, scriptsDirectory, options.getScripts());
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
        if (mode != RunSqlOptions.Mode.REPOSITORY && mode != RunSqlOptions.Mode.AUDIT) {
            return;
        }

        if (options.getScriptsDirectory() == null) {
            options.setScriptsDirectory(new File("./doc/config/sql/native-new"));
        }

        if (options.getScripts().isEmpty()) {
            if (mode == RunSqlOptions.Mode.REPOSITORY) {
                if (BooleanUtils.isTrue(options.getUpgrade())) {
                    options.setScripts(List.of(
                            new File("postgres-new-upgrade.sql")
                    ));
                } else {
                    options.setScripts(List.of(
                            new File("postgres-new.sql"),
                            new File("postgres-new-quartz.sql")
                    ));
                }
            } else if (mode == RunSqlOptions.Mode.AUDIT) {
                if (BooleanUtils.isTrue(options.getUpgrade())) {
                    options.setScripts(List.of(
                            new File("postgres-new-upgrade-audit.sql")
                    ));
                } else {
                    options.setScripts(List.of(
                            new File("postgres-new-audit.sql")
                    ));
                }
            }
        }
    }

    private HikariDataSource setupCustomDataSource() {
        if (StringUtils.isEmpty(options.getJdbcUrl())) {
            throw new IllegalStateException("JDBC url parameter not defined");
        }

        if (StringUtils.isEmpty(options.getJdbcUsername())) {
            throw new IllegalStateException("JDBC username parameter not defined");
        }

        if (StringUtils.isEmpty(options.getPassword())) {
            throw new IllegalStateException("JDBC password parameter not defined");
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

    private void executeScripts(DataSource dataSource, File scriptsDirectory, List<File> scripts) throws IOException, SQLException {
        List<File> files = scripts.stream()
                .map(script -> scriptsDirectory != null ? new File(scriptsDirectory, script.getPath()) : script)
                .toList();

        executeSqlScripts(dataSource, files);
    }

    private void executeSqlScripts(@NotNull DataSource dataSource, @NotNull List<File> scripts) throws IOException, SQLException {
        try (Connection connection = dataSource.getConnection()) {
            boolean autocommit = connection.getAutoCommit();
            connection.setAutoCommit(true);

            try {
                for (File script : scripts) {
                    log.info("Executing script {}", script.getPath());

                    Statement stmt = connection.createStatement();

                    String sql = FileUtils.readFileToString(script, StandardCharsets.UTF_8);
                    stmt.execute(sql);  // todo print results

                    stmt.close();
                }
            } finally {
                connection.setAutoCommit(autocommit);
            }
        }
    }

    private void closeQuietly(DataSource dataSource) {
        if (dataSource instanceof Closeable) {
            IOUtils.closeQuietly((Closeable) dataSource);
        }
    }
}
