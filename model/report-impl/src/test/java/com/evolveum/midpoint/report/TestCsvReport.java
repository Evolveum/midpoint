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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author skublik
 */

public class TestCsvReport extends BasicNewReportTest {

    public static final File REPORT_IMPORT_OBJECT_COLLECTION_WITH_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-import-object-collection-with-view.xml");

    public static final String IMPORT_USERS_FILE_PATH = MidPointTestConstants.TEST_RESOURCES_PATH + "/import/import-users.csv";

    public static final String REPORT_IMPORT_OBJECT_COLLECTION_WITH_CONDITION_OID = "2b77aa2e-dd86-4842-bcf5-762c8a9a85de";

    int expectedColumns;
    int expectedRow;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        importObjectFromFile(REPORT_IMPORT_OBJECT_COLLECTION_WITH_VIEW_FILE, initResult);
    }

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

    @Test
    public void test200ImportReportForUser() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_IMPORT_OBJECT_COLLECTION_WITH_CONDITION_OID);
        importReport(report, IMPORT_USERS_FILE_PATH, false);
        PrismObject<UserType> user = searchObjectByName(UserType.class, "testUser01");
        assertNotNull("User testUser01 was not created", user);
    }

    private void setExpectedValueForDashboardReport() {
        expectedColumns = 3;
        expectedRow = 7;
    }

    @Override
    protected FileFormatConfigurationType getFileFormatConfiguration() {
        FileFormatConfigurationType config = new FileFormatConfigurationType();
        config.setType(FileFormatTypeType.CSV);
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
