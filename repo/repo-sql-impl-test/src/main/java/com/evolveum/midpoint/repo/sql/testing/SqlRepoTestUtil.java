/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.tools.testng.TestMonitor;
import com.evolveum.midpoint.tools.testng.TestReportSection;

public class SqlRepoTestUtil {

    public static void assertVersionProgress(String prevVersion, String nextVersion) {
        String error = checkVersionProgress(prevVersion, nextVersion);
        if (error != null) {
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
            // anythig is OK
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
     * Returns report callback effectively wrapping around "this" at the moment the callback is created.
     * This is handy, because the field queryListener may be null at the moment when the results
     * are to be processed because of {@link AbstractSpringTest#clearClassFields()}
     * and ordering of @After... methods.
     * <p>
     * Note that the section is NOT added if the count of queries is 0.
     */
    public static TestMonitor.ReportCallback createReportCallback(TestQueryListener testQueryListener) {
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
}
