/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TARGET_DIR;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TEST_DIR;

import java.io.File;
import java.util.Map;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

class RecomputationConfiguration {

    private static final String PROP = "recomputation";
    private static final String PROP_THREADS = PROP + ".threads";

    private static final File TASK_TEMPLATE_FILE = new File(TEST_DIR, "task-recomputation.vm.xml");

    private static final String RECOMPUTE_TASK_OID = "f5920848-6c8f-4eda-ae26-2b961d6dae1b";

    private final int threads;

    private final TestObject<TaskType> generatedTask;

    private RecomputationConfiguration() {
        threads = Integer.parseInt(System.getProperty(PROP_THREADS, "0"));
        generatedTask = generateTask();
    }

    int getThreads() {
        return threads;
    }

    TestObject<TaskType> getGeneratedTask() {
        return generatedTask;
    }

    @Override
    public String toString() {
        return "RecomputationConfiguration{" +
                "threads=" + threads +
                '}';
    }

    public static RecomputationConfiguration setup() {
        RecomputationConfiguration configuration = new RecomputationConfiguration();
        System.out.println("Recompute: " + configuration);
        return configuration;
    }

    private TestObject<TaskType> generateTask() {
        return TestObject.file(TARGET_DIR, createFile(), RECOMPUTE_TASK_OID);
    }

    private String createFile() {
        String generatedFileName = "generated-task-recompute.xml";

        File generated = new File(TARGET_DIR, generatedFileName);
        VelocityGenerator.generate(TASK_TEMPLATE_FILE, generated,
                Map.of("workerThreads", threads));

        return generatedFileName;
    }
}
