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
 * AD multi-domain test for AD 2019 hosted in Evolveum private cloud.
 * This test has some strange configuration:
 *
 * * SPR is used instead of VLV for paging.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapAd2019Strange extends TestAdLdapAd2019 {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-ad2019-strange.xml");
    }
}
