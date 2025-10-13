/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Almost the same as TestDummy but uses READ+REPLACE mode for all account+group attributes.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyReadReplaceForAll extends TestDummy {

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-priorities-read-replace");
    public static final File RESOURCE_DUMMY_FILENAME = new File(TEST_DIR, "resource-dummy-all-read-replace.xml");

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }
}
