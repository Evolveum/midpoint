/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class DatabaseSchemaStep implements UpgradeStep<StepResult> {

    public static final int SUPPORTED_VERSION_LTS = 1;  // for 4.4.4

    public static final int SUPPORTED_VERSION_FEATURE = 15; // for 4.7.1

    private static final String MIDPOINT_DB_SCRIPTS_DIRECTORY = "/doc/config/sql/native-new";

    private static final String MIDPOINT_DB_UPGRADE_FILE = MIDPOINT_DB_SCRIPTS_DIRECTORY + "/postgres-new-upgrade.sql";

    private static final String AUDIT_DB_UPGRADE_FILE = MIDPOINT_DB_SCRIPTS_DIRECTORY + "/postgres-new-upgrade-audit.sql";

    private final UpgradeStepsContext context;

    public DatabaseSchemaStep(UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public String getIdentifier() {
        return "databaseSchema";
    }

    @Override
    public StepResult execute() throws Exception {
        final DownloadDistributionResult distribution = context.getResult(DownloadDistributionResult.class);
        final File distributionDirectory = distribution.getDistributionDirectory();

        final ApplicationContext applicationContext = context.getContext().getApplicationContext();

        // upgrade midpoint repository
        final DataSource dataSource = applicationContext.getBean(DataSource.class);
        NinjaUtils.executeSqlScripts(dataSource, List.of(new File(distributionDirectory, MIDPOINT_DB_UPGRADE_FILE)), ";;");

        // upgrade audit database
        final DataSource auditDataSource = NinjaUtils.getAuditDataSourceBean(applicationContext);
        NinjaUtils.executeSqlScripts(auditDataSource, List.of(new File(distributionDirectory, AUDIT_DB_UPGRADE_FILE)), ";;");

        return new StepResult() {

        };
    }

    private void executeUpgradeScript(File upgradeScript, DataSource dataSource) throws SQLException {
        FileSystemResourceLoader loader = new FileSystemResourceLoader();
        Resource script = loader.getResource("file:" + upgradeScript.getAbsolutePath());

        try (Connection connection = dataSource.getConnection()) {
            boolean autocommit = connection.getAutoCommit();
            connection.setAutoCommit(true);

            try {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.setSeparator(";;");
                populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
                populator.addScript(script);
                populator.populate(connection);
            } finally {
                connection.setAutoCommit(autocommit);
            }
        }
    }
}

//1685390031006-midpoint-latest-dist.zip
