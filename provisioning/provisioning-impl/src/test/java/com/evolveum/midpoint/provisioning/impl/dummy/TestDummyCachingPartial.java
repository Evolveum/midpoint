/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;

import java.io.File;
import java.util.Collection;
import java.util.stream.Stream;
import javax.xml.namespace.QName;

import com.google.common.collect.Streams;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

public class TestDummyCachingPartial extends TestDummyCaching {

    public static final File RESOURCE_DUMMY_FILE = new File(TestDummyCaching.TEST_DIR, "resource-dummy-partial.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected @NotNull Collection<? extends QName> getCachedAccountAttributes() throws SchemaException, ConfigurationException {
        var accountDefaultDef = getAccountDefaultDefinition();
        return Streams.concat(
                        accountDefaultDef.getAllIdentifiersNames().stream(),
                        Stream.of(
                                DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME,
                                DUMMY_ACCOUNT_ATTRIBUTE_LOOT_QNAME,
                                DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME,
                                DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME,
                                DUMMY_ENTITLEMENT_GROUP_QNAME)) // priv is not cached, only group is
                .toList();
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
