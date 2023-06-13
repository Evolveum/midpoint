package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.upgrade.UpgradeDatabaseAction;
import com.evolveum.midpoint.ninja.action.upgrade.UpgradeDatabaseOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class UpgradeFromLtsToLtsTest extends BaseUpgradeTest {

    @Test
    public void test100UpgradeDatabase() throws Exception {
        given();

        recreateSchema(new File("./src/test/resources/upgrade/sql-scripts/4.4.5"));

        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber")).isEqualTo("1");
        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber")).isEqualTo("1");

        when();

        BaseOptions baseOptions = new BaseOptions();

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome("./target/midpoint-home");

        connectionOptions.setUrl(ninjaTestsJdbcUrl);

        UpgradeDatabaseOptions upgradeDatabaseOptions = new UpgradeDatabaseOptions();
        upgradeDatabaseOptions.setScriptsDirectory(new File("../../config/sql/native-new"));

        NinjaContext context = new NinjaContext(List.of(baseOptions, connectionOptions, upgradeDatabaseOptions));

        UpgradeDatabaseAction action = new UpgradeDatabaseAction();
        action.init(context, upgradeDatabaseOptions);

        action.execute();

        then();

        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();

        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber"))
                .isEqualTo("15");
        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber"))
                .isEqualTo("3");
    }
}
