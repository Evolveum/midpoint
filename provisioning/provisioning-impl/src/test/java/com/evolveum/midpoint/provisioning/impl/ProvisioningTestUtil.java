/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ProvisioningTestUtil {

    public static final File COMMON_TEST_DIR_FILE = new File("src/test/resources/common/");
    public static final File TEST_DIR_IMPL_FILE = new File("src/test/resources/impl/");

    public static final String RESOURCE_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-9999dddddddd";

    public static final String DOT_JPG_FILENAME = "src/test/resources/common/dot.jpg";

    public static final File USER_ADMIN_FILE = new File(COMMON_TEST_DIR_FILE, "admin.xml");

    public static final String CONNID_CONNECTOR_FACADE_CLASS_NAME = "org.identityconnectors.framework.api.ConnectorFacade";
    public static final String CONNID_UID_NAME = "__UID__";
    public static final String CONNID_NAME_NAME = "__NAME__";
    public static final String CONNID_DESCRIPTION_NAME = "__DESCRIPTION__";

    public static void checkRepoAccountShadow(PrismObject<ShadowType> repoShadow) {
        checkRepoShadow(repoShadow, ShadowKindType.ACCOUNT);
    }

    public static void checkRepoEntitlementShadow(PrismObject<ShadowType> repoShadow) {
        checkRepoShadow(repoShadow, ShadowKindType.ENTITLEMENT);
    }

    public static void checkRepoShadow(PrismObject<ShadowType> repoShadow, ShadowKindType kind) {
        checkRepoShadow(repoShadow, kind, 2);
    }

    public static void checkRepoShadow(PrismObject<ShadowType> repoShadow, ShadowKindType kind, Integer expectedNumberOfAttributes) {
        ShadowType repoShadowType = repoShadow.asObjectable();
        assertNotNull("No OID in repo shadow "+repoShadow, repoShadowType.getOid());
        assertNotNull("No name in repo shadow "+repoShadow, repoShadowType.getName());
        assertNotNull("No objectClass in repo shadow "+repoShadow, repoShadowType.getObjectClass());
        assertEquals("Wrong kind in repo shadow "+repoShadow, kind, repoShadowType.getKind());
        PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
        assertNotNull("No attributes in repo shadow "+repoShadow, attributesContainer);
        Collection<Item<?,?>> attributes = attributesContainer.getValue().getItems();
        assertFalse("Empty attributes in repo shadow "+repoShadow, attributes.isEmpty());
        if (expectedNumberOfAttributes != null) {
            assertEquals("Unexpected number of attributes in repo shadow "+repoShadow, (int)expectedNumberOfAttributes, attributes.size());
        }
    }

    public static QName getDefaultAccountObjectClass(ResourceType resourceType) {
        String namespace = ResourceTypeUtil.getResourceNamespace(resourceType);
        return new QName(namespace, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
    }

}
