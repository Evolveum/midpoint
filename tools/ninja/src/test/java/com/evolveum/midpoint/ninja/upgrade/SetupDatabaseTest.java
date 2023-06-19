package com.evolveum.midpoint.ninja.upgrade;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;

import com.evolveum.midpoint.ninja.action.RunSqlAction;
import com.evolveum.midpoint.ninja.action.RunSqlOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;

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
public class SetupDatabaseTest extends BaseUpgradeTest {

    @Test
    public void test100InitializeDatabase() throws Exception {
        Assertions.assertThat(countTablesInPublicSchema()).isZero();

        BaseOptions baseOptions = new BaseOptions();
        baseOptions.setVerbose(true);

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());

        Assertions.assertThat(ninjaTestsJdbcUrl).isNotNull();
        connectionOptions.setUrl(ninjaTestsJdbcUrl);

        RunSqlOptions runSqlOptions = new RunSqlOptions();
//        runSqlOptions.setScriptsDirectory(new File("../../config/sql/native-new"));
        runSqlOptions.setCreate(true);
        runSqlOptions.setMode(RunSqlOptions.Mode.REPOSITORY);

        List<Object> options = List.of(baseOptions, connectionOptions, runSqlOptions);

        RunSqlAction action = new RunSqlAction();
        try (NinjaContext context = new NinjaContext(options, action.getApplicationContextLevel(options))) {
            action.init(context, runSqlOptions);

            action.execute();
        }


        runSqlOptions.setMode(RunSqlOptions.Mode.AUDIT);
        action = new RunSqlAction();
        try (NinjaContext context = new NinjaContext(options, action.getApplicationContextLevel(options))) {
            action.init(context, runSqlOptions);

            action.execute();
        }

        Assertions.assertThat(countTablesInPublicSchema()).isNotZero();

        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaChangeNumber"))
                .isEqualTo("15");
        Assertions.assertThat(getGlobalMetadataValue(ninjaTestDatabase, "schemaAuditChangeNumber"))
                .isEqualTo("4");
    }
}
