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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * AD multi-domain test for AD 2102R2 hosted in Evolveum private cloud.
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapAd2012 extends AbstractAdLdapMultidomainTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-ad2012.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "ad01.ad2012.lab.evolveum.com";
    }

    @Override
    protected String getLdapSuffix() {
        return "DC=ad2012,DC=lab,DC=evolveum,DC=com";
    }

    @Override
    protected String getLdapSubServerHost() {
        return "ad02.ad2012.lab.evolveum.com";
    }

    @Override
    protected String getLdapSubSuffix() {
        return "DC=sub,DC=ad2012,DC=lab,DC=evolveum,DC=com";
    }

    @Override
    protected File getReconciliationTaskFile() {
        return new File(getBaseDir(), "task-reconcile-ad2012-users.xml");
    }

    @Override
    protected String getReconciliationTaskOid() {
        return "6dabfa58-d635-11ea-ae7a-5b48b3057a69";
    }

    @Override
    protected void assertAccountDisabled(PrismObject<ShadowType> shadow) {
        assertAdministrativeStatus(shadow, ActivationStatusType.DISABLED);
    }

    @Override
    protected void assertAccountEnabled(PrismObject<ShadowType> shadow) {
        assertAdministrativeStatus(shadow, ActivationStatusType.ENABLED);
    }

    @Override
    protected String getAccountJackSid() {
        return "S-1-5-21-2906460819-3509546030-3153952426-1109";
    }

    @Override
    protected int getNumberOfAllAccounts() {
        return 9;
    }

    @Override
    protected boolean hasExchange() {
        return false;
    }
}
