/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.testng.SkipException;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;

import static org.testng.AssertJUnit.assertTrue;

public class TestCsvReportMultiNode extends TestCsvReport {

    private static final File TEST_DIR = new File("src/test/resources/reports");

    private static final TestResource<TaskType> TASK_DISTRIBUTED_EXPORT_USERS = new TestResource<>(TEST_DIR_REPORTS,
            "task-distributed-export-users.xml", "5ab8f8c6-df1a-4580-af8b-a899f240b44f");

    private static final TestResource<TaskType> TASK_DISTRIBUTED_EXPORT_AUDIT = new TestResource<>(TEST_DIR_REPORTS,
            "task-distributed-export-audit.xml", "466c5ddd-7739-437f-b049-b270da5ff828");

    private static final TestResource<ReportType> REPORT_OBJECT_COLLECTION_USERS = new TestResource<>(TEST_DIR,
            "report-object-collection-users.xml", "64e13165-21e5-419a-8d8b-732895109f84");

    private static final int USERS = 1000;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        commonInitialization(initResult);

        repoAdd(TASK_DISTRIBUTED_EXPORT_USERS, initResult);
        repoAdd(TASK_DISTRIBUTED_EXPORT_AUDIT, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_AUDIT_RECORDS, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_USERS, initResult);
        repoAdd(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN, initResult);

        createUsers(USERS, initTask, initResult);
    }

    @Test
    public void test100ExportUsers() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        runExportTask(TASK_DISTRIBUTED_EXPORT_USERS, REPORT_OBJECT_COLLECTION_USERS, result);

        when();

        waitForTaskCloseOrSuspend(TASK_DISTRIBUTED_EXPORT_USERS.oid);

        then();

        assertTask(TASK_DISTRIBUTED_EXPORT_USERS.oid, "after")
                .assertSuccess()
                .display();

        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_USERS.oid);
        basicCheckOutputFile(report, 1004, 2, null);
    }

    @Test
    public void test101ExportAuditRecords() throws Exception {
        checkSqaleRepo();
        auditTest();

        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN.oid);
        List<String> rows = basicCheckOutputFile(report, -1, 8, null);
        assertTrue("Unexpected number of rows in report. Expected:1000-1010, Actual:" + rows.size(), rows.size() > 1000 && rows.size() <= 10010);
    }

    @Test
    public void test102ExportAuditRecordsInsideTwoTimestamps() throws Exception {
        checkSqaleRepo();

        List<AuditEventRecordType> auditRecords = getAllAuditRecords(getTestTask(), getTestTask().getResult());
        SearchFilterType filter = PrismContext.get().getQueryConverter().createSearchFilterType(
                PrismContext.get().queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_TIMESTAMP).ge(auditRecords.get(500).getTimestamp()).and()
                        .item(AuditEventRecordType.F_TIMESTAMP).le(auditRecords.get(1300).getTimestamp()).buildFilter()
        );
        modifyObjectReplaceProperty(
                ObjectCollectionType.class,
                OBJECT_COLLECTION_ALL_AUDIT_RECORDS.oid,
                ObjectCollectionType.F_FILTER,
                getTestTask(),
                getTestTask().getResult(),
                filter
                );

        auditTest();

        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN.oid);
        List<String> rows = basicCheckOutputFile(report, -1, 8, null);
        assertTrue("Unexpected number of rows in report. Expected:800-810, Actual:" + rows.size(), rows.size() > 800 && rows.size() <= 810);
    }

    @Test
    public void test103ExportAuditRecordsOutsideTwoTimestamps() throws Exception {
        checkSqaleRepo();

        List<AuditEventRecordType> auditRecords = getAllAuditRecords(getTestTask(), getTestTask().getResult());
        SearchFilterType filter = PrismContext.get().getQueryConverter().createSearchFilterType(
                PrismContext.get().queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_TIMESTAMP).ge(auditRecords.get(1300).getTimestamp()).or()
                        .item(AuditEventRecordType.F_TIMESTAMP).le(auditRecords.get(500).getTimestamp()).buildFilter()
        );
        modifyObjectReplaceProperty(
                ObjectCollectionType.class,
                OBJECT_COLLECTION_ALL_AUDIT_RECORDS.oid,
                ObjectCollectionType.F_FILTER,
                getTestTask(),
                getTestTask().getResult(),
                filter
        );


        auditTest();

        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN.oid);
        List<String> rows = basicCheckOutputFile(report, -1, 8, null);
        assertTrue("Unexpected number of rows in report. Expected:1200-1215, Actual:" + rows.size(), rows.size() > 1200 && rows.size() <= 1215);
    }

    private void checkSqaleRepo() {
        if (!(plainRepositoryService instanceof SqaleRepositoryService)) {
            throw new SkipException("Skipping test before it is relevant only for sqale repo");
        }
    }

    private void auditTest() throws Exception{
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        runExportTask(TASK_DISTRIBUTED_EXPORT_AUDIT, REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN, result);

        when();

        waitForTaskCloseOrSuspend(TASK_DISTRIBUTED_EXPORT_AUDIT.oid);

        then();

        assertTask(TASK_DISTRIBUTED_EXPORT_AUDIT.oid, "after")
                .assertSuccess()
                .display();
    }

    @Override
    protected ItemName getWorkDefinitionType() {
        return WorkDefinitionsType.F_DISTRIBUTED_REPORT_EXPORT;
    }
}
