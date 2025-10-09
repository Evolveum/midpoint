/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common;

import java.io.File;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DebugDumpable;

/**
 * @author semancik
 */
public class AbstractModelCommonTest extends AbstractUnitTest
        implements InfraTestMixin {

    protected static final File COMMON_DIR = new File("src/test/resources/common");

    protected static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    protected static final String EXPRESSION_PROFILE_SAFE_NAME = "safe";

    protected Task createTask() {
        return new NullTaskImpl();
    }

    public void displayValue(String title, DebugDumpable value) {
        displayDumpable(title, value);
    }

    public void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }
}
