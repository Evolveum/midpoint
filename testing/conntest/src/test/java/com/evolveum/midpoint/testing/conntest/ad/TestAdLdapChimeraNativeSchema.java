/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;

/**
 * Test for multi-domain AD (chimera-hydra) with native AD schema support and automatic objectCategory management.
 *
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-conntest-test-main.xml" })
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapChimeraNativeSchema extends TestAdLdapChimera {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-chimera-native-schema.xml");
    }

}
