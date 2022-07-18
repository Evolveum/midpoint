/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
 * AD multi-domain test for AD 2102R2 hosted in Evolveum private cloud.
 *
 * This test is running on ad01/ad02 servers in ad2012.lab.evolveum.com domain.
 *
 * These servers ho not have Exchange installed, therefore exchange-specific aspects are skipped.
 *
 * There is also a problem with CredSSP configuration on those servers.
 * Therefore "second-hop" CredSSP tests are skipped here.
 * There is still CreddSSP configured in ad01 server (top-level domain), therefore CredSSP is still tested in a way.
 * In case of need the old Chimera/Hydra environment is archived, therefore it can be restored and used for full CredSSP tests.
 *
 * @see AbstractAdLdapMultidomainTest
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapAd2022 extends AbstractAdLdapMultidomainTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-ad2022.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "ad07.ad2022.lab.evolveum.com";
    }

    @Override
    protected String getLdapSuffix() {
        return "DC=ad2022,DC=lab,DC=evolveum,DC=com";
    }

    @Override
    protected String getLdapSubServerHost() {
        return "ad08.ad2022.lab.evolveum.com";
    }

    @Override
    protected String getLdapSubSuffix() {
        return "DC=sub2022,DC=ad2022,DC=lab,DC=evolveum,DC=com";
    }

    @Override
    protected File getReconciliationTaskFile() {
        return new File(getBaseDir(), "task-reconcile-ad2022-users.xml");
    }

    @Override
    protected String getReconciliationTaskOid() {
        return "6dabfa58-d635-11ea-ae7a-5b48b3057a69";
    }

    @Override
    protected File getShadowGhostFile() {
        return new File(TEST_DIR, "shadow-ghost-2022.xml");
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
        return "S-1-5-21-1409450148-683378572-3960173012-1105";
    }

    @Override
    protected int getNumberOfAllAccounts() {
        return 9;
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
