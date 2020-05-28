/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.testng.annotations.Test;

/**
 * @author skublik
 */

public class TestCsvReport extends BasicNewReportTest {

    int expectedColumns;
    int expectedRow;

    @Override
    public void test001CreateDashboardReportWithDefaultColumn() throws Exception {
        setExpectedValueForDashboardReport();
        super.test001CreateDashboardReportWithDefaultColumn();
    }

    @Override
    public void test002CreateDashboardReportWithView() throws Exception {
        setExpectedValueForDashboardReport();
        super.test002CreateDashboardReportWithView();
    }

    @Override
    public void test003CreateDashboardReportWithTripleView() throws Exception {
        setExpectedValueForDashboardReport();
        super.test003CreateDashboardReportWithTripleView();
    }

    @Override
    public void test010CreateObjectCollectionReportWithDefaultColumn() throws Exception {
        expectedColumns = 6;
        expectedRow = 4;
        super.test010CreateObjectCollectionReportWithDefaultColumn();
    }

    @Override
    public void test011CreateObjectCollectionReportWithView() throws Exception {
        expectedColumns = 2;
        expectedRow = 2;
        super.test011CreateObjectCollectionReportWithView();
    }

    @Override
    public void test012CreateObjectCollectionReportWithDoubleView() throws Exception {
        expectedColumns = 3;
        expectedRow = 4;
        super.test012CreateObjectCollectionReportWithDoubleView();
    }

    @Override
    public void test013CreateAuditCollectionReportWithDefaultColumn() throws Exception {
        expectedColumns = 8;
        expectedRow = 46;
        super.test013CreateAuditCollectionReportWithDefaultColumn();
    }

    @Override
    public void test014CreateAuditCollectionReportWithView() throws Exception {
        expectedColumns = 2;
        expectedRow = 48;
        super.test014CreateAuditCollectionReportWithView();
    }

    @Override
    public void test015CreateAuditCollectionReportWithDoubleView() throws Exception {
        expectedColumns = 3;
        expectedRow = 50;
        super.test015CreateAuditCollectionReportWithDoubleView();
    }

    @Override
    public void test016CreateObjectCollectionReportWithFilter() throws Exception {
        expectedColumns = 2;
        expectedRow = 3;
        super.test016CreateObjectCollectionReportWithFilter();
    }

    @Override
    public void test017CreateObjectCollectionReportWithFilterAndBasicCollection() throws Exception {
        expectedColumns = 2;
        expectedRow = 2;
        super.test017CreateObjectCollectionReportWithFilterAndBasicCollection();
    }

    private void setExpectedValueForDashboardReport() {
        expectedColumns = 3;
        expectedRow = 8;
    }

    @Override
    protected ExportConfigurationType getExportConfiguration() {
        ExportConfigurationType config = new ExportConfigurationType();
        config.setType(ExportType.CSV);
        return config;
    }

    protected List<String> basicCheckOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<String> lines = super.basicCheckOutputFile(report);

        if (lines.size() != expectedRow) {
            fail("Unexpected count of rows of csv report. Expected: " + expectedRow + ", Actual: " + lines.size());
        }

        int actualColumns = getNumberOfColumns(lines);

        if (actualColumns != expectedColumns) {
            fail("Unexpected count of columns of csv report. Expected: " + expectedColumns + ", Actual: " + actualColumns);
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
