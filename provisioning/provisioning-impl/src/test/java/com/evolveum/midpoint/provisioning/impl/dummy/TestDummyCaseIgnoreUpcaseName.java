/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Almost the same as TestDummyCaseIgnore but the resource is changing all names to upper case.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyCaseIgnoreUpcaseName extends TestDummyCaseIgnore {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-case-ignore-upcase-name");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    protected String transformNameToResource(String origName) {
        return origName.toUpperCase();
    }

    @Override
    protected void assertShadowName(PrismObject<ShadowType> shadow, String expectedName) {
        assertEquals("Shadow name is wrong in "+shadow, expectedName.toLowerCase(), shadow.asObjectable().getName().getOrig().toLowerCase());
    }

    @Override
    protected String[] getSortedUsernames18x() {
        // MORGAN, WILL, carla, daemon, meathook
        return new String[] { transformNameToResource("morgan"), getWillNameOnResource(), "carla", "daemon", "meathook" };
    }

}
