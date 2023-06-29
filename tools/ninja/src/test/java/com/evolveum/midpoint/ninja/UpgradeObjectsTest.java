package com.evolveum.midpoint.ninja;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class UpgradeObjectsTest extends NinjaSpringTest {

    @Test
    public void test100UpgradeObjects() {

    }
}
