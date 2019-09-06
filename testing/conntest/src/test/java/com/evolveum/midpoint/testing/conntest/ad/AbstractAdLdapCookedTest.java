/**
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Test for Active Directory LDAP-based access. This test is NOT using any raw settings.
 *
 * @author semancik
 */
public abstract class AbstractAdLdapCookedTest extends AbstractAdLdapTest {

	@Test
    public void test050Capabilities() throws Exception {
		final String TEST_NAME = "test050Capabilities";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Collection<Object> nativeCapabilitiesCollection = ResourceTypeUtil.getNativeCapabilitiesCollection(resourceType);
        display("Native capabilities", nativeCapabilitiesCollection);

        assertTrue("No native activation capability", ResourceTypeUtil.hasResourceNativeActivationCapability(resourceType));
        assertTrue("No native activation status capability", ResourceTypeUtil.hasResourceNativeActivationStatusCapability(resourceType));
//        assertTrue("No native lockout capability", ResourceTypeUtil.hasResourceNativeActivationLockoutCapability(resourceType));
        assertTrue("No native credentias capability", ResourceTypeUtil.isCredentialsCapabilityEnabled(resourceType, null));
	}

	protected void assertAccountDisabled(PrismObject<ShadowType> shadow) {
		assertAdministrativeStatus(shadow, ActivationStatusType.DISABLED);
	}

	protected void assertAccountEnabled(PrismObject<ShadowType> shadow) {
		assertAdministrativeStatus(shadow, ActivationStatusType.ENABLED);
	}

}
