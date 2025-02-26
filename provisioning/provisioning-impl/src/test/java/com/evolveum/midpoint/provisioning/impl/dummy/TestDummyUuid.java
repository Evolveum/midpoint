/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Almost the same as TestDummy but this is using a UUID as ICF UID.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyUuid extends TestDummy {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-uuid");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected boolean isIcfNameUidSame() {
        return false;
    }

    @Override
    protected boolean supportsMemberOf() {
        return true;
    }

    @Override
    @Test
    public void test234EntitleAccountWillPiratesIdentifiersUid() throws Exception {
        // Disabled test here.
        // The resource requires name hint, but it is not provided in the delta.
        // Maybe one day midPoint will be smarter and can add the missing hint.
        // But it is not this day.
    }

    @Override
    @Test
    public void test235DetitleAccountWillPiratesIdentifiersUid() throws Exception {
        // Disabled test here.
        // The resource requires name hint, but it is not provided in the delta.
        // Maybe one day midPoint will be smarter and can add the missing hint.
        // But it is not this day.
    }
}
