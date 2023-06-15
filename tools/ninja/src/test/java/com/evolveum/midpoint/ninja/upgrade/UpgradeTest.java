package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeDatabaseAction;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeDatabaseOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;

public abstract class UpgradeTest extends BaseUpgradeTest {

    protected abstract File getScriptsDirectory();

    protected abstract String getOldSchemaChangeNumber();

    protected abstract String getOldSchemaAuditChangeNumber();

    @Test
    public void test100UpgradeDatabase() throws Exception {
        given();

        recreateSchema(getScriptsDirectory());

        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber")).isEqualTo(getOldSchemaChangeNumber());
        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber")).isEqualTo(getOldSchemaAuditChangeNumber());

        when();

        BaseOptions baseOptions = new BaseOptions();

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());

        connectionOptions.setUrl(ninjaTestsJdbcUrl);

        UpgradeDatabaseOptions upgradeDatabaseOptions = new UpgradeDatabaseOptions();
        upgradeDatabaseOptions.setScriptsDirectory(new File("../../config/sql/native-new"));

        try (NinjaContext context = new NinjaContext(List.of(baseOptions, connectionOptions, upgradeDatabaseOptions))) {
            UpgradeDatabaseAction action = new UpgradeDatabaseAction();
            action.init(context, upgradeDatabaseOptions);

            action.execute();
        }

        then();

        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();

        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber"))
                .isEqualTo("15");
        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber"))
                .isEqualTo("3");
    }
}
