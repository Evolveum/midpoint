package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.ninja.BaseUpgradeTest;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.RunSqlAction;
import com.evolveum.midpoint.ninja.action.RunSqlOptions;

public abstract class UpgradeTest extends BaseUpgradeTest {

    protected abstract File getScriptsDirectory();

    protected abstract String getOldSchemaChangeNumber();

    protected abstract String getOldSchemaAuditChangeNumber();

    @Test(enabled = false)
    public void test100UpgradeDatabase() throws Exception {
        given();

        recreateSchema(getScriptsDirectory());

        Assertions.assertThat(getSchemaChangeNumber(ninjaTestDatabase)).isEqualTo(getOldSchemaChangeNumber());
        Assertions.assertThat(getAuditSchemaChangeNumber(ninjaTestDatabase)).isEqualTo(getOldSchemaAuditChangeNumber());

        when();

        BaseOptions baseOptions = new BaseOptions();

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());

        connectionOptions.setUrl(getJdbcUrlForNinjaTestsUser());

        RunSqlOptions upgradeDatabaseOptions = new RunSqlOptions();
        upgradeDatabaseOptions.setUpgrade(true);
        upgradeDatabaseOptions.setMode(RunSqlOptions.Mode.REPOSITORY);

        // we can't use setCreate(true) to autocomplete scripts, since real ninja is packaged differently
        // later on and scripts files are not yet available in default location
        upgradeDatabaseOptions.setScripts(getUpgradeScripts(new File("../../config/sql/native")));

        List<Object> options = List.of(baseOptions, connectionOptions, upgradeDatabaseOptions);

        executeAction(RunSqlAction.class, upgradeDatabaseOptions, options);

        then();

        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();

        Assertions.assertThat(getSchemaChangeNumber(ninjaTestDatabase)).isEqualTo(CURRENT_SCHEMA_CHANGE_NUMBER);
        Assertions.assertThat(getAuditSchemaChangeNumber(ninjaTestDatabase)).isEqualTo(CURRENT_AUDIT_SCHEMA_CHANGE_NUMBER);
    }
}
