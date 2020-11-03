/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.javasimon.EnabledManager;
import org.javasimon.Manager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;

/**
 * Object can hold various monitors (stopwatches, counters) and dump a report for them.
 * Use for a single report unit, e.g. a test class, then throw away.
 */
public class TestMonitor {

    /**
     * Order of creation/addition is the order in the final report.
     * Stopwatches are stored under specific names, but their names are null, always use the key.
     */
    private final Map<String, Stopwatch> stopwatches = new LinkedHashMap<>();

    // TODO other monitors later

    /** Simon manager used for monitor creations, otherwise ignored. */
    private final Manager simonManager = new EnabledManager();

    /** If you want to register already existing Stopwatch, e.g. from production code. */
    public synchronized void register(Stopwatch stopwatch) {
        stopwatches.put(stopwatch.getName(), stopwatch);
    }

    /**
     * Returns {@link Stopwatch} for specified name, registers a new one if needed.
     */
    public synchronized Stopwatch stopwatch(String name) {
        Stopwatch stopwatch = stopwatches.get(name);
        if (stopwatch == null) {
            // internally the stopwatch is not named to avoid registration with Simon Manager
            stopwatch = simonManager.getStopwatch(null);
            stopwatches.put(name, stopwatch);
        }
        return stopwatch;
    }

    /**
     * Starts measurement (represented by {@link Split}) on a stopwatch with specified name.
     * Stopwatch is created and registered with this object if needed (no Simon manager is used).
     * Returned {@link Split} is {@link AutoCloseable}, so it can be used in try-with-resource
     * and the actual variable can be completely ignored.
     */
    public Split stopwatchStart(String name) {
        return stopwatch(name).start();
    }

    // TODO not sure what "testName" is, it's some kind of "bundle of monitors".
    //  Currently it is tied to the dump call, so it can't be test method name unless it's called
    //  in each method. Scoping/grouping of monitors is to be determined yet.
    public void dumpReport(String testName) {
        dumpReport(testName, System.out);
    }

    public void dumpReport(String testName, PrintStream out) {
        // millis are more practical, but sometimes too big for avg and min and we don't wanna mix ms/us
        out.println("test|name|count|total(us)|avg(us)|min(us)|max(us)");
        for (Map.Entry<String, Stopwatch> stopwatchEntry : stopwatches.entrySet()) {
            String name = stopwatchEntry.getKey();
            Stopwatch stopwatch = stopwatchEntry.getValue();
            out.printf("%s|%s|%d|%d|%d|%d|%d\n", testName, name,
                    stopwatch.getCounter(),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getTotal()),
                    TimeUnit.NANOSECONDS.toMicros((long) stopwatch.getMean()),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getMin()),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getMax()));
        }
    }
}
