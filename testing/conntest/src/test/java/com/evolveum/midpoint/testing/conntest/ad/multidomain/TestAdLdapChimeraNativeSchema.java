/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.conntest.ad.multidomain;

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
