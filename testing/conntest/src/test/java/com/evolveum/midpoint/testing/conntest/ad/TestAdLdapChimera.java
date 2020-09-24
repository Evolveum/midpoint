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
 * Old "chimera/hydra" servers in Lozorno. DEPRECATED.
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAdLdapChimera extends AbstractAdLdapMultidomainTest {

    @Override
    protected File getResourceFile() {
        return new File(getBaseDir(), "resource-chimera.xml");
    }

    @Override
    protected String getLdapServerHost() {
        return "chimera.ad.evolveum.com";
    }

    @Override
    protected String getLdapSubServerHost() {
        return "hydra.ad.evolveum.com";
    }

    @Override
    protected String getLdapSuffix() {
        return "DC=ad,DC=evolveum,DC=com";
    }

    @Override
    protected String getLdapSubSuffix() {
        return "DC=sub,DC=ad,DC=evolveum,DC=com";
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
    protected int getNumberOfAllAccounts() {
        return 18;
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
        return "S-1-5-21-3305462238-3617280118-659738602-4878";
    }

    @Override
    protected boolean hasExchange() {
        return true;
    }

    @Override
    protected boolean isVagueTest() {
        return true;
    }

    @Override
    protected String getExpected182FirstShadow() {
        return "CN=Adolf Supperior,"+getPeopleLdapSuffix();
    }
}
