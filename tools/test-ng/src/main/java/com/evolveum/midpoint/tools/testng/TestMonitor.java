/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.tools.testng;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
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
     * Name of a system property that specifies file name prefix for report.
     * If system property is null, report is dumped to standard output.
     * Specified file name prefix can be absolute or relative from working directory,
     * e.g. {@code target/perf-report}.
     */
    public static final String PERF_REPORT_PREFIX_PROPERTY_NAME = "mp.perf.report.prefix";

    /**
     * Order of creation/addition is the order in the final report.
     * Stopwatches are stored under specific names, but their names are null, always use the key.
     */
    private final Map<String, Stopwatch> stopwatches = new LinkedHashMap<>();

    /**
     * Collection of report sections that will be formatted using {@link #dumpReport(String)}.
     * which can be extended in two ways:
     * <ul>
     * <li>calling {@link #addReportSection(String)} and then filling the </li>
     * </ul>
     */
    private final List<TestReportSection> reportSections = new ArrayList<>();

    /**
     *
     */
    private final List<ReportCallback> reportCallbacks = new ArrayList<>();

    /** Simon manager used for monitor creations, otherwise ignored. */
    private final Manager simonManager = new EnabledManager();

    /**
     * If you want to register already existing Stopwatch, e.g. from production code.
     * Note is taken from the {@link Stopwatch#getNote()}.
     */
    public synchronized void register(Stopwatch stopwatch) {
        stopwatches.put(stopwatch.getName(), stopwatch);
    }

    /**
     * Returns {@link Stopwatch} for specified name, registers a new one if needed.
     */
    public synchronized Stopwatch stopwatch(String name, String description) {
        Stopwatch stopwatch = stopwatches.get(name);
        if (stopwatch == null) {
            // internally the stopwatch is not named to avoid registration with Simon Manager
            stopwatch = simonManager.getStopwatch(null);
            stopwatches.put(name, stopwatch);
            stopwatch.setNote(description);
        }
        return stopwatch;
    }

    /**
     * Starts measurement (represented by {@link Split}) on a stopwatch with specified name.
     * Stopwatch is created and registered with this object if needed (no Simon manager is used).
     * Returned {@link Split} is {@link AutoCloseable}, so it can be used in try-with-resource
     * and the actual variable can be completely ignored.
     */
    public Split stopwatchStart(String name, String description) {
        return stopwatch(name, description).start();
    }

    public TestMonitor addReportCallback(ReportCallback reportCallback) {
        reportCallbacks.add(reportCallback);
        return this;
    }

    public TestReportSection addReportSection(String sectionName) {
        TestReportSection reportSection = new TestReportSection(sectionName);
        reportSections.add(reportSection);
        return reportSection;
    }

    public void dumpReport(String testName) {
        ReportMetadata reportMetadata = new ReportMetadata(testName);
        String perfReportPrefix = System.getProperty(PERF_REPORT_PREFIX_PROPERTY_NAME);
        if (perfReportPrefix == null) {
            dumpReportToStdout(reportMetadata);
            return;
        }

        // we want to report to a file
        String filename = String.format("%s-%s-%s-%s.txt",
                perfReportPrefix, testName, reportMetadata.commitIdShort, reportMetadata.timestamp);
        try (PrintStream out = new PrintStream(
                new BufferedOutputStream(
                        new FileOutputStream(filename)))) {
            dumpReport(reportMetadata, out);
        } catch (FileNotFoundException e) {
            System.out.println("Creating report file failed with: " + e.toString());
            System.out.println("Falling back to stdout dump:");
            dumpReportToStdout(reportMetadata);
        }
    }

    private void dumpReportToStdout(ReportMetadata reportMetadata) {
        System.out.println(">>>> PERF REPORT");
        dumpReport(reportMetadata, System.out);
        System.out.println("<<<<");
    }

    public void dumpReport(String testName, PrintStream out) {
        dumpReport(new ReportMetadata(testName), out);
    }

    private void dumpReport(ReportMetadata reportMetadata, PrintStream out) {
        out.println("Commit: " + reportMetadata.commitId);
        out.println("Timestamp: " + reportMetadata.timestamp);
        out.println("Branch: " + reportMetadata.branch);
        out.println("Test: " + reportMetadata.testName);

        TestReportSection section = addReportSection("stopwatch")
                // millis are more practical, but sometimes too big for avg and min and we don't wanna mix ms/us
                .withColumns("monitor", "count", "total(us)", "avg(us)", "min(us)", "max(us)", "note");
        for (Map.Entry<String, Stopwatch> stopwatchEntry : stopwatches.entrySet()) {
            Stopwatch stopwatch = stopwatchEntry.getValue();
            section.addRow(
                    stopwatchEntry.getKey(),
                    stopwatch.getCounter(),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getTotal()),
                    TimeUnit.NANOSECONDS.toMicros((long) stopwatch.getMean()),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getMin()),
                    TimeUnit.NANOSECONDS.toMicros(stopwatch.getMax()),
                    stopwatch.getNote());
        }

        // executing callback to get other report sections for higher level metrics
        for (ReportCallback reportCallback : reportCallbacks) {
            reportCallback.execute(this);
        }

        for (TestReportSection reportSection : reportSections) {
            reportSection.dump(reportMetadata.testName, out);
        }
    }

    private static class ReportMetadata {

        public final String testName;
        public final String buildNumber;
        public final String branch;
        public final String commitId;
        public final String commitIdShort;
        public final String date;
        public final String timestamp;

        public ReportMetadata(String testName) {
            this.testName = testName;
            /*
             * Various Jenkins related environment variables are available:
             * Git related: BRANCH (local), GIT_BRANCH, GIT_COMMIT (full), GIT_PREVIOUS_COMMIT,
             * GIT_PREVIOUS_SUCCESSFUL_COMMIT, GIT_URL
             * Job related: BUILD_DISPLAY_NAME, BUILD_ID, BUILD_NUMBER, BUILD_TAG, BUILD_URL,
             * JOB_BASE_NAME, JOB_DISPLAY_URL, JOB_NAME, JOB_URL
             * Other Jenkins: JENKINS_HOME, JENKINS_SERVER_COOKIE, JENKINS_URL, NODE_LABELS, NODE_NAME
             * See also: https://www.jenkins.io/doc/book/pipeline/jenkinsfile/#using-environment-variables
             */
            Map<String, String> env = System.getenv();
            buildNumber = env.getOrDefault("BUILD_NUMBER", "unknown");
            branch = env.getOrDefault("BRANCH", "unknown");
            commitId = env.getOrDefault("GIT_COMMIT", "unknown");
            commitIdShort = StringUtils.truncate(commitId, 8);

            date = LocalDate.now().toString();
            timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
        }

        @Override
        public String toString() {
            return "ReportMetadata{" +
                    "testName='" + testName + '\'' +
                    ", buildNumber='" + buildNumber + '\'' +
                    ", commitId='" + commitId + '\'' +
                    ", commitIdShort='" + commitIdShort + '\'' +
                    ", date='" + date + '\'' +
                    ", timestamp='" + timestamp + '\'' +
                    '}';
        }
    }

    public interface ReportCallback {
        /**
         * Called during report dump - allows interacting with the monitor,
         * typically to add additional report section.
         */
        void execute(TestMonitor testMonitor);
    }
}
