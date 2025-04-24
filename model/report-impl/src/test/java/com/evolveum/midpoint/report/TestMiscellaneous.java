/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;
import java.util.Objects;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestReport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Tests e.g. {@link ReportManager#runReport(PrismObject, PrismContainer, Task, OperationResult)} method.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends EmptyReportIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "misc");

    private static final TestReport REPORT_EXPORT_USERS = TestReport.file(
            TEST_DIR, "report-export-users.xml", "63665d73-9829-4064-a8ec-0a04c554ebcc");
    private static final TestReport REPORT_IMPORT_USERS = TestReport.file(
            TEST_DIR, "report-import-users.xml", "54319c28-cc40-4005-bac3-a5a9dbabd91b");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                REPORT_EXPORT_USERS,
                REPORT_IMPORT_USERS);
    }

    /**
     * Checking that {@link ReportManager#runReport(PrismObject, PrismContainer, Task, OperationResult)} correctly
     * applies the archetypes, e.g. their mappings.
     *
     * MID-8364
     */
    @Test
    public void test100RunExport() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("report export is run");
        reportManager.runReport(REPORT_EXPORT_USERS.get(), null, task, result);

        then("the task has archetype mappings applied");
        var taskOid = Objects.requireNonNull(result.findTaskOid(), "no task OID");
        waitForTaskCloseOrSuspend(taskOid);
        assertTask(taskOid, "after")
                .display()
                .assertArchetypeRef(ARCHETYPE_TASK_REPORT_EXPORT_CLASSIC.oid)
                .assertDescription("export")
                .assertClosed()
                .assertSuccess();
    }

    /**
     * As {@link #test100RunExport()} but for import method {@link ReportManager#importReport(PrismObject, PrismObject,
     * Task, OperationResult)}.
     *
     * MID-8364
     */
    @Test
    public void test110RunImport() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("user data file");
        ReportDataType data = new ReportDataType()
                .name("users-to-import")
                .fileFormat(FileFormatTypeType.CSV)
                .filePath(new File(TEST_DIR, "users-to-import.csv").getAbsolutePath());
        addObject(data, task, result);

        when("report import is run");
        reportManager.importReport(
                REPORT_IMPORT_USERS.get(),
                data.asPrismObject(),
                task, result);

        then("the task has archetype mappings applied");
        var taskOid = Objects.requireNonNull(result.findTaskOid(), "no task OID");
        waitForTaskCloseOrSuspend(taskOid);
        assertTask(taskOid, "after")
                .display()
                .assertArchetypeRef(ARCHETYPE_TASK_REPORT_IMPORT_CLASSIC.oid)
                .assertDescription("import")
                .assertClosed()
                .assertSuccess();

        and("user is imported");
        assertUserAfterByUsername("jim")
                .assertFullName("Jim Hacker");
    }

    @Override
    protected FileFormatConfigurationType getFileFormatConfiguration() {
        return null; // unused
    }

    /**
     * MID-10659
     *
     * Chained subreports variables, different code path via {@link ReportManager#evaluateSubreportParameters} used in GUI
     */
    @Test
    public void testSubreportsVariables() {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_OBJECT, new AuditEventRecordType(), AuditEventRecordType.class);

        reportManager.evaluateSubreportParameters(REPORT_SUBREPORT_AUDIT.get(),variables, task, result);

        assertSuccess(result);
    }
}
