package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryConfiguration;

import com.evolveum.midpoint.repo.sqlbase.DataSourceFactory;

import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.ninja.opts.SetupDatabaseSchemaOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class SetupDatabaseSchemaAction extends RepositoryAction<SetupDatabaseSchemaOptions> {

    @Override
    public void execute() throws Exception {
        // this is manual setup of datasource for midpoint, can't be done via spring application context initialization with repository
        // because sqale repository during initialization loads data from m_uri and m_ext_item (not yet existing)
        final ApplicationContext applicationContext = context.getApplicationContext();

        final MidpointConfiguration midpointConfiguration = applicationContext.getBean(MidpointConfiguration.class);

        SqaleRepositoryConfiguration repositoryConfiguration = new SqaleRepositoryConfiguration(
                midpointConfiguration.getConfiguration(
                        MidpointConfiguration.REPOSITORY_CONFIGURATION));
        repositoryConfiguration.init();
        DataSourceFactory dataSourceFactory = new DataSourceFactory(repositoryConfiguration);
        try {

            final File scriptsDirectory = options.getScriptsDirectory();

            // upgrade midpoint repository
            final DataSource dataSource = dataSourceFactory.createDataSource("ninja-repository");
            executeScripts(dataSource, scriptsDirectory, options.getScripts());

            // upgrade audit database
            if (!options.isNoAudit()) {
                // todo figure out how to initialize datasource for audit, it's a mess

                final DataSource auditDataSource = NinjaUtils.getAuditDataSourceBean(applicationContext);
                executeScripts(auditDataSource, scriptsDirectory, options.getAuditScripts());
            }
        } finally {
            dataSourceFactory.destroy();
        }
    }

    private void executeScripts(DataSource dataSource, File scriptsDirectory, List<File> scripts) throws IOException, SQLException {
        List<File> files = scripts.stream()
                .map(script -> scriptsDirectory != null ? new File(scriptsDirectory, script.getPath()) : script)
                .toList();

        NinjaUtils.executeSqlScripts(dataSource, files);
    }
}
