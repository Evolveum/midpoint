/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import org.javasimon.Stopwatch;
import org.testng.annotations.AfterClass;

/**
 * Mixin supporting work with {@link TestMonitor}.
 * Setting of {@link TestMonitor} is up to the class, adding before-class method here that
 * uses setter you would have to provide is not helpful.
 */
public interface PerformanceTestMixin extends MidpointTestMixin {

    TestMonitor getTestMonitor();

    @AfterClass
    default void dumpReport() {
        // TODO output to files, how?
        TestMonitor testMonitor = getTestMonitor();
        testMonitor.dumpReport(getClass().getSimpleName());
        // TODO more output types
    }

    default Stopwatch stopwatch(String name) {
        return getTestMonitor().stopwatch(name);
    }
}
