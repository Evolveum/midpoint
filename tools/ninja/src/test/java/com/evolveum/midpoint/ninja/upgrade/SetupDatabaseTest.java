package com.evolveum.midpoint.ninja.upgrade;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class SetupDatabaseTest extends BaseUpgradeTest {

    @Test
    public void test100InitializeDatabase() throws Exception {
//        Assertions.assertThat(countTablesInPublicSchema()).isZero();
//
//        BaseOptions baseOptions = new BaseOptions();
//
//        ConnectionOptions connectionOptions = new ConnectionOptions();
//        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());
//
//        Assertions.assertThat(ninjaTestsJdbcUrl).isNotNull();
//        connectionOptions.setUrl(ninjaTestsJdbcUrl);
//
//        SetupDatabaseOptions setupDatabaseOptions = new SetupDatabaseOptions();
//        setupDatabaseOptions.setScriptsDirectory(new File("../../config/sql/native-new"));
//
//        List<Object> options = List.of(baseOptions, connectionOptions, setupDatabaseOptions);
//
//        SetupDatabaseAction action = new SetupDatabaseAction();
//
//        try (NinjaContext context = new NinjaContext(options, action.getApplicationContextLevel(options))) {
//            action.init(context, setupDatabaseOptions);
//
//            action.execute();
//        }
//
//        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();
//
//        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber"))
//                .isEqualTo("15");
//        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber"))
//                .isEqualTo("4");
    }
}
