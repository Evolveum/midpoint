/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;
import java.util.List;

import static com.evolveum.midpoint.test.DummyResourceContoller.*;

public class TestDummyCachingPartial extends TestDummyCaching {

    public static final File RESOURCE_DUMMY_FILE = new File(TestDummyCaching.TEST_DIR, "resource-dummy-partial.xml");

    private static final List<String> CACHED = List.of(
            // icfs:name is cached but not checked via methods using this list
            DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME,
            DUMMY_ACCOUNT_ATTRIBUTE_LOOT_NAME,
            DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
            DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME);

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    boolean isAttrCached(String attrName) {
        return CACHED.contains(attrName);
    }
}
