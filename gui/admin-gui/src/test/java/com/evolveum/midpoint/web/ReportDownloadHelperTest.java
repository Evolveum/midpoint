/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import static org.testng.Assert.assertEquals;

import com.evolveum.midpoint.web.page.admin.reports.ReportDownloadHelper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Tests download filename handling for report outputs.
 *
 * In particular, verifies that ZIP-backed tracing report files are downloaded
 * with the {@code .zip} suffix while preserving existing names and filename
 * sanitization behavior.
 */
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReportDownloadHelperTest extends AbstractGuiIntegrationTest {

    @Test
    public void zipReportFileNameHasZipExtension() {
        ReportDataType report = report("trace-2026", "C:\\midpoint\\trace\\trace-2026.zip");

        Assert.assertEquals(ReportDownloadHelper.getReportFileName(report), "trace-2026.zip");
    }

    @Test
    public void zipReportFileNameDoesNotDuplicateZipExtension() {
        ReportDataType report = report("trace-2026.zip", "C:\\midpoint\\trace\\trace-2026.zip");

        assertEquals(ReportDownloadHelper.getReportFileName(report), "trace-2026.zip");
    }

    @Test
    public void nonZipReportFileNameIsNotChanged() {
        ReportDataType report = report("report-output", "C:\\midpoint\\export\\report-output.csv");

        assertEquals(ReportDownloadHelper.getReportFileName(report), "report-output.csv");
    }

    @Test
    public void pathComponentsAreRemovedBeforeAppendingZipExtension() {
        ReportDataType report = report("C:\\tmp\\trace-2026", "C:\\midpoint\\trace\\trace-2026.zip");

        assertEquals(ReportDownloadHelper.getReportFileName(report), "trace-2026.zip");
    }

    @Test
    public void reportWithoutFilePathDoesNotGetZipExtension() {
        ReportDataType report = report("trace-2026", null);

        assertEquals(ReportDownloadHelper.getReportFileName(report), "trace-2026");
    }

    @Test
    public void emptyZipReportFileNameUsesFallbackWithZipExtension() {
        ReportDataType report = report(null, "C:\\midpoint\\trace\\trace-2026.zip");

        assertEquals(ReportDownloadHelper.getReportFileName(report), "report.zip");
    }

    private static ReportDataType report(String name, String filePath) {
        ReportDataType report = new ReportDataType()
                .filePath(filePath);
        if (name != null) {
            report.name(new PolyStringType(name));
        }
        return report;
    }
}
