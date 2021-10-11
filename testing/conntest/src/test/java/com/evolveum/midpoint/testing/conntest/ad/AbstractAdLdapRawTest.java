/*
 * Copyright (c) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import static com.evolveum.midpoint.testing.conntest.ad.AdTestMixin.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collection;

import com.evolveum.midpoint.prism.path.ItemPath;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Test for Active Directory LDAP-based access. This test is using raw userAccountControl.
 *
 * @author semancik
 */
public abstract class AbstractAdLdapRawTest extends AbstractAdLdapTest {

    @Test
    public void test050Capabilities() {
        Collection<Object> nativeCapabilitiesCollection = ResourceTypeUtil.getNativeCapabilitiesCollection(resourceType);
        display("Native capabilities", nativeCapabilitiesCollection);

        assertFalse("No native activation capability", ResourceTypeUtil.hasResourceNativeActivationCapability(resourceType));
        assertFalse("No native activation status capability", ResourceTypeUtil.hasResourceNativeActivationStatusCapability(resourceType));
        assertFalse("No native lockout capability", ResourceTypeUtil.hasResourceNativeActivationLockoutCapability(resourceType));
        assertTrue("No native credentias capability", ResourceTypeUtil.isCredentialsCapabilityEnabled(resourceType, null));
    }


    protected void assertAccountDisabled(PrismObject<ShadowType> shadow) {
        PrismAsserts.assertPropertyValue(shadow, ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_USER_ACCOUNT_CONTROL_QNAME), 514);
    }

    protected void assertAccountEnabled(PrismObject<ShadowType> shadow) {
        PrismAsserts.assertPropertyValue(shadow, ItemPath.create(ShadowType.F_ATTRIBUTES, ATTRIBUTE_USER_ACCOUNT_CONTROL_QNAME), 512);
    }

}
