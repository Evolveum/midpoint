package com.evolveum.midpoint.ninja.upgrade;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.NinjaSpringTest;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class PreUpgradeCheckTest extends NinjaSpringTest {

    @Test
    public void test100TestNoNodes() throws Exception {
        given();

        when();

        executeTest(
                list -> {
                    boolean found = list.stream().anyMatch(
                            s -> s.contains("There are zero nodes in cluster to validate current midPoint version"));
                    Assertions.assertThat(found).isTrue();
                },
                EMPTY_STREAM_VALIDATOR,
                "-v", "-m", getMidpointHome(), "pre-upgrade-check"
        );

        then();
    }
}
