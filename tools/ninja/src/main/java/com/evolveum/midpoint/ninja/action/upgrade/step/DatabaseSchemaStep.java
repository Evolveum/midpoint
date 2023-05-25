/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.step;

import java.io.Closeable;
import javax.sql.DataSource;

import com.evolveum.midpoint.ninja.action.upgrade.StepResult;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStep;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeStepsContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

public class DatabaseSchemaStep implements UpgradeStep<StepResult> {

    public static final int SUPPORTED_VERSION_LTS = 1;  // for 4.4.4

    public static final int SUPPORTED_VERSION_FEATURE = 15; // for 4.7.1

    private static final String MIDPOINT_DB_UPGRADE_FILE = "/config/postgres-new-upgrade.sql";

    private static final String AUDIT_DB_UPGRADE_FILE = "postgres-new-upgrade-audit.sql";

    private final UpgradeStepsContext context;

    private ApplicationContext applicationContext;

    public DatabaseSchemaStep(UpgradeStepsContext context) {
        this.context = context;
    }

    @Override
    public String getIdentifier() {
        return "databaseSchema";
    }

    @Override
    public StepResult execute() throws Exception {
        // 1/ initialize DB connection, using midpoint home?
        // 2/ check current state of DB. Is it previous feature release (4.6) or LTS (4.4)
        // 3/ pick proper scripts
        // 4/ execute upgrade scripts

        try {
            init();

            upgrade();
        } finally {
            destroy();
        }

        return new StepResult() {
        };
    }

    // todo fix, same code is also in
    private void init() {
        // todo application context should be initialized here ("at this time") not during initialization of ninja context
        applicationContext = context.getContext().getApplicationContext();
    }

    private void upgrade() {
        // todo implement audit upgrade
        DataSource midpointDS = applicationContext.getBean(DataSource.class);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

        DownloadDistributionResult distribution = context.getResult(DownloadDistributionResult.class);

        FileSystemResourceLoader loader = new FileSystemResourceLoader();
        Resource script = loader.getResource(distribution.getDistributionDirectory() + MIDPOINT_DB_UPGRADE_FILE);

        populator.addScript(script);
        populator.execute(midpointDS);
    }

    private void destroy() {
        try {
            if (applicationContext instanceof Closeable) {
                ((Closeable) applicationContext).close();
            }
        } catch (Exception ex) {
            // todo handle properly
            ex.printStackTrace();
        }
    }
}
