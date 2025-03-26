/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * Very basic update capability: no deltas, no add/delete attribute values.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyUltraLegacyUpdate extends TestDummy {

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-ultra-legacy-update.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected boolean isUltraLegacyUpdate() {
        return true;
    }

    @Override
    protected void assertEffectiveUpdateCapability(UpdateCapabilityType capUpdate) {
        assertNotNull("native update capability not present", capUpdate);
        assertNull("native update capability is manual", capUpdate.isManual());
        assertThat(capUpdate.isDelta())
                .as("'delta' update capability")
                .isNotEqualTo(true);
        assertThat(capUpdate.isAddRemoveAttributeValues())
                .as("'addRemoveAttributeValues' update capability")
                .isNotEqualTo(true);
    }
}
