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
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-conntest-test-main.xml" })
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapChimeraRunAs extends AbstractAdLdapMultidomainRunAsTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-chimera-runas.xml");
    }

    protected String getResourceOid() {
        return "eced6d24-73e3-11e5-8457-93eff15a6b85";
    }

    @Override
    protected String getLdapServerHost() {
        return "chimera.ad.evolveum.com";
    }

    @Override
    protected int getLdapServerPort() {
        return 636;
    }

    @Override
    protected String getLdapBindDn() {
        return null;
    }

    @Override
    protected String getLdapSubServerHost() {
        return null;
    }

    @Override
    protected String getLdapSubSuffix() {
        return null;
    }

    @Override
    protected String getLdapSubBindDn() {
        return null;
    }

    @Override
    protected String getAccountJackSid() {
        return null;
    }

    @Override
    protected File getReconciliationTaskFile() {
        return new File(getBaseDir(), "task-reconcile-chimera-users.xml");
    }

    @Override
    protected String getReconciliationTaskOid() {
        return "6e2689dc-88fa-11e9-a382-0baf927677fd";
    }

    @Override
    protected File getShadowGhostFile() {
        return null;
    }

    @Override
    protected int getNumberOfAllAccounts() {
        return 0;
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
    protected boolean hasExchange() {
        return true;
    }
}
