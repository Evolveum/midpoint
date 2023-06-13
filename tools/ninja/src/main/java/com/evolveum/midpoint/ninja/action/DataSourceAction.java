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

import com.evolveum.midpoint.ninja.opts.ConnectionOptions;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.AuditFactory;
import com.evolveum.midpoint.ninja.opts.DataSourceOptions;
import com.evolveum.midpoint.repo.api.RepositoryServiceFactoryException;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryConfiguration;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditServiceFactory;
import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;

public abstract class DataSourceAction<O extends DataSourceOptions> extends Action<O, Void> {

    @Override
    public Void execute() throws Exception {
        // this is manual setup of datasource for midpoint, can't be done via spring application context initialization with repository
        // because sqale repository during initialization loads data from m_uri and m_ext_item (not yet existing)
        log.info("Initializing application context");

        ConnectionOptions connectionOptions = context.getOptions(ConnectionOptions.class);
        // force offline mode - when application context is initialized we don't want to have full initialization of sqale repository/audit service
        connectionOptions.setOffline(true);

        final ApplicationContext applicationContext = context.getApplicationContext();
        final MidpointConfiguration midpointConfiguration = applicationContext.getBean(MidpointConfiguration.class);

        DataSource repositoryDataSource = null;
        DataSource auditDataSource = null;
        try {
            File scriptsDirectory = options.getScriptsDirectory();

            // upgrade midpoint repository
            Configuration configuration = midpointConfiguration.getConfiguration(MidpointConfiguration.REPOSITORY_CONFIGURATION);
            repositoryDataSource = createDataSource(configuration, "ninja-repository");
            if (!options.isAuditOnly()) {
                executeScripts(repositoryDataSource, scriptsDirectory, options.getScripts());
            }

            // upgrade audit database
            if (!options.isNoAudit()) {
                auditDataSource = createAuditDataSource(repositoryDataSource, midpointConfiguration);
                if (auditDataSource != null) {
                    executeScripts(auditDataSource, scriptsDirectory, options.getAuditScripts());
                } else {
                    log.error("Audit configuration not found in <midpoint-home>/config.xml");
                }
            }
        } finally {
            closeQuietly(repositoryDataSource);
            closeQuietly(auditDataSource);
        }

        return null;
    }

    private void executeScripts(DataSource dataSource, File scriptsDirectory, List<File> scripts) throws IOException, SQLException {
        List<File> files = scripts.stream()
                .map(script -> scriptsDirectory != null ? new File(scriptsDirectory, script.getPath()) : script)
                .toList();

        executeSqlScripts(dataSource, files);
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

    private DataSource createDataSource(Configuration configuration, String name) throws RepositoryServiceFactoryException {
        log.info("Creating connection for " + name);
        SqaleRepositoryConfiguration repositoryConfiguration = new SqaleRepositoryConfiguration(configuration);
        repositoryConfiguration.init();
        DataSourceFactory dataSourceFactory = new DataSourceFactory(repositoryConfiguration);

        return dataSourceFactory.createDataSource(name);
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
                    stmt.execute(sql);

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
