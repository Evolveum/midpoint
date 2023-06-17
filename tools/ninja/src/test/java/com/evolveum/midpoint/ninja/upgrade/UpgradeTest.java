package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;

import org.testng.annotations.Test;

public abstract class UpgradeTest extends BaseUpgradeTest {

    protected abstract File getScriptsDirectory();

    protected abstract String getOldSchemaChangeNumber();

    protected abstract String getOldSchemaAuditChangeNumber();

    @Test
    public void test100UpgradeDatabase() throws Exception {
//        given();
//
//        recreateSchema(getScriptsDirectory());
//
//        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber")).isEqualTo(getOldSchemaChangeNumber());
//        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber")).isEqualTo(getOldSchemaAuditChangeNumber());
//
//        when();
//
//        BaseOptions baseOptions = new BaseOptions();
//
//        ConnectionOptions connectionOptions = new ConnectionOptions();
//        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());
//
//        connectionOptions.setUrl(ninjaTestsJdbcUrl);
//
//        UpgradeDatabaseOptions upgradeDatabaseOptions = new UpgradeDatabaseOptions();
//        upgradeDatabaseOptions.setScriptsDirectory(new File("../../config/sql/native-new"));
//
//        List<Object> options = List.of(baseOptions, connectionOptions, upgradeDatabaseOptions);
//
//        UpgradeDatabaseAction action = new UpgradeDatabaseAction();
//        try (NinjaContext context = new NinjaContext(options, action.getApplicationContextLevel(options))) {
//            action.init(context, upgradeDatabaseOptions);
//
//            action.execute();
//        }
//
//        then();
//
//        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();
//
//        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber"))
//                .isEqualTo("15");
//        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber"))
//                .isEqualTo("3");
    }
}
