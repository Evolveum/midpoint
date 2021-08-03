/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.testing;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.tools.testng.TestMonitor;
import com.evolveum.midpoint.tools.testng.TestReportSection;

public class SqlRepoTestUtil {

    public static void assertVersionProgress(String prevVersion, String nextVersion) {
        String error = checkVersionProgress(prevVersion, nextVersion);
        if (error != null) {
            AssertJUnit.fail(error);
        }
    }

    /** If modifications are narrowed to none, it's possible that the version stays the same. */
    public static void assertVersionProgressOptional(String prevVersion, String nextVersion) {
        String error = checkVersionProgress(prevVersion, nextVersion);
        if (error != null && !prevVersion.equals(nextVersion)) {
            AssertJUnit.fail(error);
        }
    }

    public static String checkVersionProgress(String prevVersion, String nextVersion) {
        String error = checkVersionProgressInternal(prevVersion, nextVersion);
        if (error == null) {
            return null;
        }
        return "Invalid version progress from '" + prevVersion + "' to '" + nextVersion + "': " + error;
    }

    private static String checkVersionProgressInternal(String prevVersion, String nextVersion) {
        if (nextVersion == null) {
            return "null next version";
        }
        if (prevVersion == null) {
            // anything is OK
            return null;
        }
        if (prevVersion.equals(nextVersion)) {
            return "version are same";
        }
        int prevInt;
        try {
            prevInt = Integer.parseInt(prevVersion);
        } catch (NumberFormatException e) {
            return "previous version is not numeric";
        }
        int nextInt;
        try {
            nextInt = Integer.parseInt(nextVersion);
        } catch (NumberFormatException e) {
            return "next version is not numeric";
        }
        if (nextInt <= prevInt) {
            return "wrong numeric order";
        }
        return null;
    }

    /**
     * Returns report callback adding query section to the performance test report.
     * Note that the section is NOT added if the count of queries is 0.
     */
    public static TestMonitor.ReportCallback reportCallbackQuerySummary(
            TestQueryListener testQueryListener) {
        return testMonitor -> {
            if (testQueryListener.hasNoEntries()) {
                return;
            }

            TestReportSection section = testMonitor.addReportSection("query")
                    .withColumns("metric", "count");
            section.addRow("query-count", testQueryListener.getQueryCount());
            section.addRow("execution-count", testQueryListener.getExecutionCount());
        };
    }

    /**
     * Returns report callback adding detailed query dump section to the performance test report.
     * Note that the section is NOT added if the count of queries is 0.
     * This section is more for visual comparison/interpretation than for graphing.
     */
    public static TestMonitor.ReportCallback reportCallbackQueryList(
            TestQueryListener testQueryListener) {
        return testMonitor -> {
            if (testQueryListener.hasNoEntries()) {
                return;
            }

            TestReportSection section = testMonitor.addRawReportSection("query-list");
            testQueryListener.getEntries().forEach(e -> section.addRow(e.query));
        };
    }
}
