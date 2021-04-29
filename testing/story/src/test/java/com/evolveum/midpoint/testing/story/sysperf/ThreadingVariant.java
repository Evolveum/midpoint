/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TEST_DIR;

enum ThreadingVariant {

    T1("1t"),
    T4("4t"),
    T16("16t"),
    T40_N4("40t-4n");

    private static final String PROP_THREADING = "threading";

    private static final String IMPORT_TASK_OID = "c32d4da6-bdd3-481d-931a-60ca8a5a01ba";
    private static final String TASK_FILE_NAME_PATTERN = "task-import-%s.xml";

    private final String name;

    ThreadingVariant(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static ThreadingVariant setup() {
        String configuredName = System.getProperty(PROP_THREADING, T1.name);
        ThreadingVariant variant = fromName(configuredName);
        System.out.println("Threading variant: " + variant);
        return variant;
    }

    public TestResource<TaskType> getImportTaskResource() {
        String fileName = String.format(TASK_FILE_NAME_PATTERN, name);
        return new TestResource<>(TEST_DIR, fileName, IMPORT_TASK_OID);
    }

    private static ThreadingVariant fromName(String name) {
        for (ThreadingVariant value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown threading variant: " + name);
    }
}
