/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReport extends EmptyReportIntegrationTest {

    List<String> basicCheckOutputFile(PrismObject<ReportType> report, int expectedRow, int expectedColumns, CharSequence lastLine) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<String> lines = getLinesOfOutputFile(report);

        if (expectedRow != -1 && lines.size() != expectedRow) {
            fail("Unexpected count of rows of csv report. Expected: " + expectedRow + ", Actual: " + lines.size());
        }

        if (expectedRow == -1 && lines.size() < 2) {
            fail("Unexpected count of rows of csv report. Expected: more as one, Actual: " + lines.size());
        }

        int actualColumns = getNumberOfColumns(lines);
        if (actualColumns != expectedColumns) {
            fail("Unexpected count of columns of csv report. Expected: " + expectedColumns + ", Actual: " + actualColumns);
        }

        String lastRealLine = lines.get(lines.size() - 1);
        if (StringUtils.isNoneEmpty(lastLine) && !lastRealLine.equals(lastLine)) {
            fail("Unexpected last line of csv report. Expected: '" + lastLine + "', Actual: '" + lastRealLine + "'");
        }
        return lines;
    }

    private int getNumberOfColumns(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Couldn't find line of report");
        }
        return lines.get(0).split(";").length;
    }
}
