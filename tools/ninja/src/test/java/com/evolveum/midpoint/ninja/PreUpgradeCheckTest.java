package com.evolveum.midpoint.ninja;

import java.sql.Connection;

import org.assertj.core.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.SqaleUtils;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class PreUpgradeCheckTest extends NinjaSpringTest {

    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        if (!repository.isNative()) {
            throw new SkipException("Skipping test because repository not using native PostgreSQL implementation.");
        }

        super.beforeClass();
    }

    @Test
    public void test100TestNoNodes() throws Exception {
        given();

        when();

        MainResult<Boolean> result = executeTest(
                list -> {
                    boolean found = list.stream().anyMatch(
                            s -> s.contains("There are zero nodes in cluster to validate current midPoint version"));
                    Assertions.assertThat(found).isTrue();
                },
                EMPTY_STREAM_VALIDATOR,
                "-v", "-m", getMidpointHome(), "pre-upgrade-check"
        );

        then();

        Boolean shouldContinue = result.result();
        Assertions.assertThat(result.exitCode())
                .isZero()
                .withFailMessage("Upgrade pre-check - error code should be zero.");
        Assertions.assertThat(shouldContinue)
                .isTrue()
                .withFailMessage("Upgrade pre-check - should continue (true).");
    }

    // todo enable this test, however it shouldn't modify main test database structure, whole test class should prepare
    //  another "upgrade specific" database where schema/tables can be dropped and recreated
    @Test(enabled = false)
    public void test200TestWrongSchemaVersion() throws Exception {
        given();

        try (Connection connection = repositoryDataSource.getConnection()) {
            JdbcTemplate template = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            template.update("UPDATE m_global_metadata SET value=? WHERE name = ?", "123456", SqaleUtils.SCHEMA_CHANGE_NUMBER);
            template.execute("COMMIT");
        }

        when();

        MainResult<Boolean> result = executeTest(
                list -> {
                    boolean found = list.stream().anyMatch(
                            s -> s.contains("There are zero nodes in cluster to validate current midPoint version"));
                    Assertions.assertThat(found).isTrue();
                },
                EMPTY_STREAM_VALIDATOR,
                "-v", "-m", getMidpointHome(), "pre-upgrade-check"
        );

        then();

        Boolean shouldContinue = result.result();
        Assertions.assertThat(result.exitCode())
                .isZero()
                .withFailMessage("Upgrade pre-check - error code should be zero.");
        Assertions.assertThat(shouldContinue)
                .isFalse()
                .withFailMessage("Upgrade pre-check - DB schema version doesn't match.");
    }
}
