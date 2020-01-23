/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common;

import java.io.File;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.TestUtil;

/**
 * @author semancik
 *
 */
public class AbstractModelCommonTest {

    protected static final File COMMON_DIR = new File("src/test/resources/common");

    protected static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    protected static final String EXPRESSION_PROFILE_SAFE_NAME = "safe";

    protected void displayTestTitle(final String TEST_NAME) {
        TestUtil.displayTestTitle(this, TEST_NAME);
    }

    protected void displayWhen(final String TEST_NAME) {
        TestUtil.displayWhen(TEST_NAME);
    }

    protected void displayThen(final String TEST_NAME) {
        TestUtil.displayThen(TEST_NAME);
    }

    protected Task createTask() {
        return new NullTaskImpl();
    }
}
