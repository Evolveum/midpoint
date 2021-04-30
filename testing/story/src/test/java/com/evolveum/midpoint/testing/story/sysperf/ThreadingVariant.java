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

    T1("1t", 1, 1),
    T4("4t", 4, 1),
    T16("16t", 16, 1),
    T40_N4("40t-4n", 40, 4);

    private static final String PROP_THREADING = "threading";

    private static final String IMPORT_TASK_OID = "c32d4da6-bdd3-481d-931a-60ca8a5a01ba";
    private static final String IMPORT_TASK_FILE_NAME_PATTERN = "task-import-%s.xml";

    private static final String RECOMPUTE_TASK_OID = "f5920848-6c8f-4eda-ae26-2b961d6dae1b";
    private static final String RECOMPUTE_TASK_FILE_NAME_PATTERN = "task-recompute-%s.xml";

    private final String name;
    private final int threads;
    private final int tasks;

    ThreadingVariant(String name, int threads, int tasks) {
        this.name = name;
        this.threads = threads;
        this.tasks = tasks;
    }

    public String getName() {
        return name;
    }

    public int getThreads() {
        return threads;
    }

    public int getTasks() {
        return tasks;
    }

    public static ThreadingVariant setup() {
        String configuredName = System.getProperty(PROP_THREADING, T1.name);
        ThreadingVariant variant = fromName(configuredName);
        System.out.println("Threading variant: " + variant);
        return variant;
    }

    public TestResource<TaskType> getImportTaskResource() {
        String fileName = String.format(IMPORT_TASK_FILE_NAME_PATTERN, name);
        return new TestResource<>(TEST_DIR, fileName, IMPORT_TASK_OID);
    }

    public TestResource<TaskType> getRecomputeTaskResource() {
        String fileName = String.format(RECOMPUTE_TASK_FILE_NAME_PATTERN, name);
        return new TestResource<>(TEST_DIR, fileName, RECOMPUTE_TASK_OID);
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
