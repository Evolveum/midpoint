package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.ninja.opts.SetupDatabaseSchemaOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class SetupDatabaseSchemaAction extends RepositoryAction<SetupDatabaseSchemaOptions> {

    @Override
    public void execute() throws Exception {
        final ApplicationContext applicationContext = context.getApplicationContext();

        final File scriptsDirectory = options.getScriptsDirectory();

        // upgrade midpoint repository
        final DataSource dataSource = applicationContext.getBean(DataSource.class);
        executeScripts(dataSource, scriptsDirectory, options.getScripts());

        // upgrade audit database
        if (!options.isNoAudit()) {
            final DataSource auditDataSource = NinjaUtils.getAuditDataSourceBean(applicationContext);
            executeScripts(auditDataSource, scriptsDirectory, options.getAuditScripts());
        }
    }

    private void executeScripts(DataSource dataSource, File scriptsDirectory, List<File> scripts) throws SQLException {
        List<File> files = scripts.stream()
                .map(script -> scriptsDirectory != null ? new File(scriptsDirectory, script.getPath()) : script)
                .toList();

        NinjaUtils.executeSqlScripts(dataSource, files);
    }
}
