/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TARGET_DIR;
import static com.evolveum.midpoint.testing.story.sysperf.TestSystemPerformance.TEST_DIR;

class ReconciliationWithSourceConfiguration {

    private static final String PROP = "reconciliation"; // eventually we change this to reconciliation-with-source
    private static final String PROP_THREADS = PROP + ".threads";
    private static final String PROP_RUNS = PROP + ".runs";

    private static final File TASK_TEMPLATE_FILE = new File(TEST_DIR, "task-reconciliation-with-source.vm.xml");

    private final int threads;
    private final int runs;

    private final List<TestObject<TaskType>> generatedTasks;

    private ReconciliationWithSourceConfiguration() {
        threads = Integer.parseInt(System.getProperty(PROP_THREADS, "0"));
        runs = Integer.parseInt(System.getProperty(PROP_RUNS, "1"));

        generatedTasks = generateTasks();
    }

    int getThreads() {
        return threads;
    }

    int getRuns() {
        return runs;
    }

    List<TestObject<TaskType>> getGeneratedTasks() {
        return generatedTasks;
    }

    @Override
    public String toString() {
        return "ReconciliationConfiguration{" +
                "threads=" + threads +
                ", runs=" + runs +
                '}';
    }

    public static ReconciliationWithSourceConfiguration setup() {
        ReconciliationWithSourceConfiguration configuration = new ReconciliationWithSourceConfiguration();
        System.out.println("Reconciliation with sources: " + configuration);
        return configuration;
    }

    private List<TestObject<TaskType>> generateTasks() {
        List<TestObject<TaskType>> tasks = new ArrayList<>();
        List<DummyTestResource> sourceResources = TestSystemPerformance.SOURCES_CONFIGURATION.getGeneratedResources();
        for (int i = 0; i < sourceResources.size(); i++) {
            String taskOid = RandomSource.randomUUID().toString();
            tasks.add(TestObject.file(TARGET_DIR, createFile(i, sourceResources.get(i), taskOid), taskOid));
        }
        return tasks;
    }

    private String createFile(int index, DummyTestResource resource, String taskOid) {
        String generatedFileName = String.format("generated-task-reconciliation-with-source-%03d.xml", index);

        File generated = new File(TARGET_DIR, generatedFileName);
        VelocityGenerator.generate(TASK_TEMPLATE_FILE, generated,
                Map.of("taskOid", taskOid,
                        "index", String.format("%03d", index),
                        "resourceOid", resource.oid,
                        "workerThreads", threads));

        return generatedFileName;
    }
}
