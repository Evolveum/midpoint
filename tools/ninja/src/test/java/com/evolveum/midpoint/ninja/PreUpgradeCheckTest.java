package com.evolveum.midpoint.ninja;

import java.sql.Connection;

import org.assertj.core.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.SqaleUtils;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class PreUpgradeCheckTest extends NinjaSpringTest {

    @Test
    public void test100TestNoNodes() throws Exception {
        given();

        when();

        Boolean result = (Boolean) executeTest(
                list -> {
                    boolean found = list.stream().anyMatch(
                            s -> s.contains("There are zero nodes in cluster to validate current midPoint version"));
                    Assertions.assertThat(found).isTrue();
                },
                EMPTY_STREAM_VALIDATOR,
                "-v", "-m", getMidpointHome(), "pre-upgrade-check"
        );

        then();

        Assertions.assertThat(result)
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

        Boolean result = (Boolean) executeTest(
                list -> {
                    boolean found = list.stream().anyMatch(
                            s -> s.contains("There are zero nodes in cluster to validate current midPoint version"));
                    Assertions.assertThat(found).isTrue();
                },
                EMPTY_STREAM_VALIDATOR,
                "-v", "-m", getMidpointHome(), "pre-upgrade-check"
        );

        then();

        Assertions.assertThat(result)
                .isFalse()
                .withFailMessage("Upgrade pre-check - DB schema version doesn't match.");
    }
}
