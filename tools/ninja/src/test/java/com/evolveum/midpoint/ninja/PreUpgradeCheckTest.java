package com.evolveum.midpoint.ninja;

import org.assertj.core.api.Assertions;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Task spring context is needed to have the task manager initialized - so that node object is available for check.
 */
@ContextConfiguration(locations = { "classpath:ctx-ninja-test.xml", "classpath:ctx-task.xml" })
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

    @Override
    public void clearMidpointTestDatabase(ApplicationContext context) {
        // do nothing, we want to keep the data
    }

    @Test
    public void test100TestNoNodes() throws Exception {
        when();

        MainResult<Boolean> result = executeTest(
                list -> {
                    boolean found = list.stream().anyMatch(
                            s -> s.contains("Found 1 nodes in cluster"));
                    Assertions.assertThat(found).isTrue();
                },
                EMPTY_STREAM_VALIDATOR,
                "-v", "-m", getMidpointHome(), "pre-upgrade-check"
        );

        then();

        Boolean shouldContinue = result.result();
        Assertions.assertThat(result.exitCode())
                .withFailMessage("Upgrade pre-check - error code should be zero.")
                .isZero();
        Assertions.assertThat(shouldContinue)
                .withFailMessage("Upgrade pre-check - should continue (true).")
                .isTrue();
    }
}
