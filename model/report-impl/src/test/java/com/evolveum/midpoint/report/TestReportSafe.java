/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Basic report test, using "safe" expression profile.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReportSafe extends TestReport {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_SAFE_FILE;
    }

    /**
     * Reports with poisonous operations in the query. This should work with null profile.
     * But it should fail with safe profile.
     * Field operations are safe in this report, just the query is poisonous.
     */
    @Test
    @Override
    public void test112ReportUserListExpressionsPoisonousQueryCsv() throws Exception {
        final String TEST_NAME = "test110ReportUserListExpressionsCsv";
        testReportListUsersCsvFailure(TEST_NAME, REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_OID);
    }

    /**
     * Reports with poisonous operations in the field expression. This should work with null profile.
     * But it should fail with safe profile.
     * Query expression is safe in this report, just fields are poisonous.
     */
    @Test
    public void test114ReportUserListExpressionsPoisonousFieldCsv() throws Exception {
        final String TEST_NAME = "test114ReportUserListExpressionsPoisonousFieldCsv";
        testReportListUsersCsvFailure(TEST_NAME, REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_OID);
    }

    /**
     * Reports with report.searchAuditRecords() operation in the query expression. This should work with null profile.
     * But it should fail with safe profile.
     */
    @Test
    public void test300ReportAuditLegacy() throws Exception {
        final String TEST_NAME = "test300ReportAuditLegacy";
        testReportAuditCsvFailure(TEST_NAME, REPORT_AUDIT_CSV_LEGACY_OID);
    }

}
