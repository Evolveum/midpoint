/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.io.File;
import javax.sql.DataSource;

import com.evolveum.midpoint.init.AuditServiceProxy;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.audit.SqaleAuditService;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;

public class DatabaseSchemaStep implements UpgradeStep<StepResult> {

    public static final int SUPPORTED_VERSION_LTS = 1;  // for 4.4.4

    public static final int SUPPORTED_VERSION_FEATURE = 15; // for 4.7.1

    private static final String MIDPOINT_DB_UPGRADE_FILE = "/config/postgres-new-upgrade.sql";

    private static final String AUDIT_DB_UPGRADE_FILE = "/config/postgres-new-upgrade-audit.sql";

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

        executeUpgradeScript(new File(distributionDirectory, MIDPOINT_DB_UPGRADE_FILE), dataSource);

        // upgrade audit database
        AuditServiceProxy auditProxy = applicationContext.getBean(AuditServiceProxy.class);
        SqaleAuditService auditService = auditProxy.getImplementation(SqaleAuditService.class);
        SqaleRepoContext repoContext = auditService.sqlRepoContext();
        final DataSource auditDataSource = (DataSource) FieldUtils.readField(repoContext, "dataSource", true);

        executeUpgradeScript(new File(distributionDirectory, AUDIT_DB_UPGRADE_FILE), auditDataSource);

        return new StepResult() {

        };
    }

    private void executeUpgradeScript(File upgradeScript, DataSource dataSource) {
        FileSystemResourceLoader loader = new FileSystemResourceLoader();
        Resource script = loader.getResource(upgradeScript.getAbsolutePath());

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(script);
        populator.execute(dataSource);
    }
}
