/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad.big;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;

import java.io.File;

/**
 * Big AD test for AD 2106 hosted in Evolveum private cloud.
 *
 * This test is running on ad05/ad06 servers in ad2016.lab.evolveum.com domain.
 *
 * The tests are working with big user population and big groups.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapBigAd2016 extends AbstractAdLdapBigTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-ad2016.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "ad05.ad2016.lab.evolveum.com";
    }

    @Override
    protected String getLdapSuffix() {
        return "DC=ad2016,DC=lab,DC=evolveum,DC=com";
    }

    @Override
    protected int getNumberOfAllAccounts() {
        return 11;
    }

    @Override
    protected boolean hasExchange() {
        return false;
    }
}
