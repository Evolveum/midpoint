/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import com.google.common.base.Joiner;
import org.javasimon.Stopwatch;

/**
 * Common mixin supporting work with {@link TestMonitor}, use one of the sub-interfaces based
 * on the required scope measured by the test monitor (class or method).
 * Test class (perhaps abstract superclass) must implement create/destroy/"get" methods,
 * the rest should be taken care of by the lifecycle methods in sub-interfaces.
 */
public interface PerformanceTestCommonMixin extends MidpointTestMixin {

    Joiner MONITOR_JOINER = Joiner.on(".");

    /** Used for reporting at the end of the scope, but also for adding monitors by the test code. */
    TestMonitor testMonitor();

    /** Called after method or class, see sub-interfaces. */
    void createTestMonitor();

    /** Implement, don't call, used by {@link #dumpReport}. */
    void destroyTestMonitor();

    /** Override to enrich test monitor with more data. */
    default void beforeDumpReport(TestMonitor testMonitor) {
    }

    /**
     * Dumps the report and <b>"destroys" the test monitor</b> to allow its garbage collection.
     * Otherwise if many test objects are part of the test runtime it can take a lot of memory.
     * Called after method or class, see sub-interfaces.
     *
     * @param reportedTestName reported test name can be a class name or class+method name
     * combination depending on the test monitor scope
     */
    default void dumpReport(String reportedTestName) {
        TestMonitor testMonitor = testMonitor();
        beforeDumpReport(testMonitor);
        testMonitor.dumpReport(reportedTestName);
        destroyTestMonitor();
    }

    default Stopwatch stopwatch(String name, String description) {
        return testMonitor().stopwatch(name, description);
    }

    /**
     * Returns final name from name parts, joined with predefined character (yes, it's a dot).
     */
    default String monitorName(String... nameParts) {
        return MONITOR_JOINER.join(nameParts);
    }
}
