/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;

public class TestDummyCachingPartial extends TestDummyCaching {

    public static final File RESOURCE_DUMMY_FILE = new File(TestDummyCaching.TEST_DIR, "resource-dummy-partial.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected @NotNull Collection<? extends QName> getCachedAccountAttributes() throws SchemaException, ConfigurationException {
        List<QName> rv = new ArrayList<>(
                getAccountDefaultDefinition().getAllIdentifiersNames());
        rv.add(DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME);
        rv.add(DUMMY_ACCOUNT_ATTRIBUTE_LOOT_QNAME);
        rv.add(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME);
        rv.add(DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME);
        return rv;
    }

    // TEMPORARY
    @Override
    boolean isAttrCached(String attrName) {
        try {
            return QNameUtil.matchAny(
                    new QName(NS_RI, attrName),
                    getCachedAccountAttributes());
        } catch (CommonException e) {
            throw new SystemException(e);
        }
    }
}
