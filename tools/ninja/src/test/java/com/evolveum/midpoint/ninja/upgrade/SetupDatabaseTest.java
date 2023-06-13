package com.evolveum.midpoint.ninja.upgrade;

import com.evolveum.midpoint.ninja.action.SetupDatabaseAction;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.SetupDatabaseOptions;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class SetupDatabaseTest extends BaseUpgradeTest{

    @Test
    public void test100InitializeDatabase() throws Exception {
        Assertions.assertThat(countTablesInPublicSchema()).isZero();

        BaseOptions baseOptions = new BaseOptions();

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome("./target/midpoint-home");

        Assertions.assertThat(ninjaTestsJdbcUrl).isNotNull();
        connectionOptions.setUrl(ninjaTestsJdbcUrl);

        SetupDatabaseOptions setupDatabaseOptions = new SetupDatabaseOptions();
        setupDatabaseOptions.setScriptsDirectory(new File("../../config/sql/native-new"));

        try (NinjaContext context = new NinjaContext(List.of(baseOptions, connectionOptions, setupDatabaseOptions))) {

            SetupDatabaseAction action = new SetupDatabaseAction();
            action.init(context, setupDatabaseOptions);

            action.execute();
        }

        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();

        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber"))
                .isEqualTo("15");
        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber"))
                .isEqualTo("3");
    }
}
