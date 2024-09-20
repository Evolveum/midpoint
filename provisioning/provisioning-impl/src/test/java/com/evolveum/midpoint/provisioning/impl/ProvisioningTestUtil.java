/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.io.File;
import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
public class ProvisioningTestUtil {

    static final File COMMON_TEST_DIR_FILE = new File("src/test/resources/common/");

    public static final String DOT_JPG_FILENAME = "src/test/resources/common/dot.jpg";

    public static final File USER_ADMIN_FILE = new File(COMMON_TEST_DIR_FILE, "admin.xml");

    public static final String CONNID_CONNECTOR_FACADE_CLASS_NAME = "org.identityconnectors.framework.api.ConnectorFacade";
    public static final String CONNID_UID_NAME = "__UID__";
    public static final String CONNID_NAME_NAME = "__NAME__";
    public static final String CONNID_DESCRIPTION_NAME = "__DESCRIPTION__";

    public static void checkRepoAccountShadow(RawRepoShadow repoShadow) {
        checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT);
    }

    public static void checkRepoEntitlementShadow(RawRepoShadow repoShadow) {
        checkRepoShadow(repoShadow, ShadowKindType.ENTITLEMENT);
    }

    public static void checkRepoShadow(RawRepoShadow repoShadow, ShadowKindType kind) {
        checkRepoShadow(repoShadow, kind, 2);
    }

    public static void checkRepoShadow(RawRepoShadow repoShadow, ShadowKindType kind, Integer expectedNumberOfAttributes) {
        ShadowType bean = repoShadow.getBean();
        assertNotNull("No OID in repo shadow "+repoShadow, bean.getOid());
        assertNotNull("No name in repo shadow "+repoShadow, bean.getName());
        assertNotNull("No objectClass in repo shadow "+repoShadow, bean.getObjectClass());
        assertEquals("Wrong kind in repo shadow "+repoShadow, kind, bean.getKind());
        PrismContainer<Containerable> attributesContainer = repoShadow.getPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        assertNotNull("No attributes in repo shadow "+repoShadow, attributesContainer);
        Collection<Item<?,?>> attributes = attributesContainer.getValue().getItems();
        assertFalse("Empty attributes in repo shadow "+repoShadow, attributes.isEmpty());
        if (expectedNumberOfAttributes != null) {
            if (InternalsConfig.isShadowCachingOnByDefault()) {
                assertThat(attributes)
                        .as("attributes in repo shadow " + repoShadow)
                        .hasSizeGreaterThanOrEqualTo(expectedNumberOfAttributes);
            } else {
                assertEquals("Unexpected number of attributes in repo shadow " + repoShadow, (int) expectedNumberOfAttributes, attributes.size());
            }
        }
        assertThat(bean.getShadowLifecycleState())
                .as("shadow lifecycle of " + repoShadow)
                .isNull();
    }
}
