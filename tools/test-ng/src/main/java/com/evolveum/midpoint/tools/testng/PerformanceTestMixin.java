/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import com.google.common.base.Joiner;
import org.javasimon.Stopwatch;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Mixin supporting work with {@link TestMonitor}.
 * Setting of {@link TestMonitor} is up to the class, adding before-class method here that
 * uses setter you would have to provide is not helpful.
 */
public interface PerformanceTestMixin extends MidpointTestMixin {

    Joiner MONITOR_JOINER = Joiner.on(".");

    TestMonitor testMonitor();

    void initializeTestMonitor();

    @BeforeClass
    default void initTestMonitor() {
        initializeTestMonitor();
    }

    @AfterClass
    default void dumpReport() {
        TestMonitor testMonitor = testMonitor();
        testMonitor.dumpReport(getClass().getSimpleName());
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
