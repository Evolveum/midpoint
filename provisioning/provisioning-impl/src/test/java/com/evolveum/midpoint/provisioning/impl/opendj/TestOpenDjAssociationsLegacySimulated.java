/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.opendj;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjAssociationsLegacySimulated extends AbstractOpenDjSimulatedAssociationsTest {

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-legacy.xml");

    @Override
    protected File getResourceOpenDjFile() {
        return RESOURCE_OPENDJ_FILE;
    }
}
