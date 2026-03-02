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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * AD multi-domain test for AD 2025 hosted in Evolveum private cloud.
 *
 * This test is running on ad09/ad10 servers in ad2025.lab.evolveum.com domain.
 *
 * These servers do not have Exchange installed, therefore exchange-specific aspects are skipped.
 *
 * There is also a problem with CredSSP configuration on those servers.
 * Therefore "second-hop" CredSSP tests are skipped here.
 * There is still CreddSSP configured in ad09 server (top-level domain), therefore CredSSP is still tested in a way.
 * In case of need the old Chimera/Hydra environment is archived, therefore it can be restored and used for full CredSSP tests.
 *
 * @see AbstractAdLdapMultidomainTest
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapAd2025 extends AbstractAdLdapMultidomainTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-ad2025.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "ad09.ad2025.lab.evolveum.com";
    }

    @Override
    protected String getLdapSuffix() {
        return "DC=ad2025,DC=lab,DC=evolveum,DC=com";
    }

    @Override
    protected String getLdapSubServerHost() {
        return "ad10.ad2025.lab.evolveum.com";
    }

    @Override
    protected String getLdapSubSuffix() {
        return "DC=sub2025,DC=ad2025,DC=lab,DC=evolveum,DC=com";
    }

    @Override
    protected File getReconciliationTaskFile() {
        return new File(getBaseDir(), "task-reconcile-ad2025-users.xml");
    }

    @Override
    protected String getReconciliationTaskOid() {
        return "6dabfa58-d635-11ea-ae7a-5b48b3057a69";
    }

    @Override
    protected File getShadowGhostFile() {
        return new File(TEST_DIR, "shadow-ghost-2025.xml");
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
        return "S-1-5-21-633336390-1514156346-1603286900-1102";
    }

    @Override
    protected int getNumberOfAllAccounts() {
        //Namely: Administrator, Guest, AD07, krbtgt
        // Jack Sparrow, MidPoint, SSH Test, sub2025$
        return 8;
    }

    @Override
    protected boolean hasExchange() {
        return false;
    }

    @Override
    protected String getExpected182FirstShadow() {
        return "CN=Administrator," + getPeopleLdapSuffix();
    }
}
