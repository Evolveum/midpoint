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
    public void test101CreateAuditCollectionReportWithDefaultColumn() throws Exception {
        expectedColumns = 8;
        expectedRow = -1;
        super.test101CreateAuditCollectionReportWithDefaultColumn();
    }

    @Override
    public void test102CreateAuditCollectionReportWithView() throws Exception {
        expectedColumns = 2;
        expectedRow = -1;
        super.test102CreateAuditCollectionReportWithView();
    }

    @Override
    public void test103CreateAuditCollectionReportWithDoubleView() throws Exception {
        expectedColumns = 3;
        expectedRow = -1;
        super.test103CreateAuditCollectionReportWithDoubleView();
    }

    @Test
    public void test104CreateAuditCollectionReportWithCondition() throws Exception {
        expectedColumns = 8;
        expectedRow = 2;
        super.test104CreateAuditCollectionReportWithCondition();
    }

    @Override
    public void test110CreateObjectCollectionReportWithDefaultColumn() throws Exception {
        expectedColumns = 6;
        expectedRow = 4;
        super.test110CreateObjectCollectionReportWithDefaultColumn();
    }

    @Override
    public void test111CreateObjectCollectionReportWithView() throws Exception {
        expectedColumns = 2;
        expectedRow = 2;
        super.test111CreateObjectCollectionReportWithView();
    }

    @Override
    public void test112CreateObjectCollectionReportWithDoubleView() throws Exception {
        expectedColumns = 3;
        expectedRow = 4;
        super.test112CreateObjectCollectionReportWithDoubleView();
    }

    @Override
    public void test113CreateObjectCollectionReportWithFilter() throws Exception {
        expectedColumns = 2;
        expectedRow = 3;
        super.test113CreateObjectCollectionReportWithFilter();
    }

    @Override
    public void test114CreateObjectCollectionReportWithFilterAndBasicCollection() throws Exception {
        expectedColumns = 2;
        expectedRow = 2;
        super.test114CreateObjectCollectionReportWithFilterAndBasicCollection();
    }

    @Test
    public void test115CreateObjectCollectionReportWithCondition() throws Exception {
        expectedColumns = 6;
        expectedRow = 2;
        super.test115CreateObjectCollectionReportWithCondition();
    }

    private void setExpectedValueForDashboardReport() {
        expectedColumns = 3;
        expectedRow = 7;
    }

    @Override
    protected ExportConfigurationType getExportConfiguration() {
        ExportConfigurationType config = new ExportConfigurationType();
        config.setType(ExportType.CSV);
        return config;
    }

    protected List<String> basicCheckOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        List<String> lines = super.basicCheckOutputFile(report);

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
        return lines;
    }

    private int getNumberOfColumns(List<String> lines) {
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Couldn't find line of report");
        }
        return lines.get(0).split(";").length;
    }
}
