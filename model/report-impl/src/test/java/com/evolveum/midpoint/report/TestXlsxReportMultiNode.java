/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_APPLICATION_VND_MSEXCEL_2007;

import java.io.File;
import java.io.FileInputStream;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests distributed (bucketed, multi-node) report export to XLSX, counterpart of {@link TestCsvReportMultiNode}.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestXlsxReportMultiNode extends EmptyReportIntegrationTest {

    private static final TestObject<TaskType> TASK_DISTRIBUTED_EXPORT_USERS = TestObject.file(TEST_DIR_REPORTS,
            "task-distributed-export-users.xml", "5ab8f8c6-df1a-4580-af8b-a899f240b44f");
    private static final TestObject<TaskType> TASK_DISTRIBUTED_EXPORT_AUDIT = TestObject.file(TEST_DIR_REPORTS,
            "task-distributed-export-audit.xml", "466c5ddd-7739-437f-b049-b270da5ff828");
    private static final TestObject<ReportType> REPORT_OBJECT_COLLECTION_USERS = TestObject.file(TEST_DIR_REPORTS,
            "report-object-collection-users.xml", "64e13165-21e5-419a-8d8b-732895109f84");

    private static final int USERS = 1000;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(USER_WILL, initTask, initResult);
        addObject(USER_JACK, initTask, initResult);
        repoAdd(TASK_DISTRIBUTED_EXPORT_USERS, initResult);
        repoAdd(TASK_DISTRIBUTED_EXPORT_AUDIT, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_AUDIT_RECORDS, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_USERS, initResult);
        repoAdd(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN, initResult);
        createUsers(USERS, initTask, initResult);
    }

    @Override
    protected FileFormatConfigurationType getFileFormatConfiguration() {
        return new FileFormatConfigurationType().type(FileFormatTypeType.XLSX);
    }

    @Override
    protected ItemName getWorkDefinitionType() {
        return WorkDefinitionsType.F_DISTRIBUTED_REPORT_EXPORT;
    }

    @Test
    public void test100ExportUsers() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();
        int reportDataCountBefore = repositoryService.countObjects(ReportDataType.class, null, null, result);

        when();
        runExportTask(TASK_DISTRIBUTED_EXPORT_USERS, REPORT_OBJECT_COLLECTION_USERS, result);
        waitForTaskCloseOrSuspend(TASK_DISTRIBUTED_EXPORT_USERS.oid);

        then();
        assertTask(TASK_DISTRIBUTED_EXPORT_USERS.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_DISTRIBUTED_TASK.value());

        Sheet sheet = readSingleSheet(TASK_DISTRIBUTED_EXPORT_USERS);
        // header + 1000 generated users + administrator, jack, will + subscription footer
        assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(USERS + 5);
        assertThat(sheet.getRow(0).getPhysicalNumberOfCells()).isEqualTo(2);
        assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");

        assertThat(repositoryService.countObjects(ReportDataType.class, null, null, result))
                .as("partial report data objects must be consumed")
                .isEqualTo(reportDataCountBefore + 1);
        assertNotificationMessage(REPORT_OBJECT_COLLECTION_USERS.getObjectable(), MIME_APPLICATION_VND_MSEXCEL_2007);
    }

    @Test
    public void test101ExportAuditRecords() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        when();
        runExportTask(TASK_DISTRIBUTED_EXPORT_AUDIT, REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN, result);
        waitForTaskCloseOrSuspend(TASK_DISTRIBUTED_EXPORT_AUDIT.oid);

        then();
        assertTask(TASK_DISTRIBUTED_EXPORT_AUDIT.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_DISTRIBUTED_TASK.value());

        Sheet sheet = readSingleSheet(TASK_DISTRIBUTED_EXPORT_AUDIT);
        int rows = sheet.getPhysicalNumberOfRows();
        assertThat(rows).as("rows (header + records + footer)").isBetween(1001, 1013);
        assertThat(sheet.getRow(0).getPhysicalNumberOfCells()).isEqualTo(8);
        assertNotificationMessage(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN.getObjectable(), MIME_APPLICATION_VND_MSEXCEL_2007);
    }

    /** Reads the only sheet of the aggregated workbook produced by the given task. */
    private Sheet readSingleSheet(TestObject<TaskType> testTask) throws Exception {
        PrismObject<TaskType> reportTask = getObject(TaskType.class, testTask.oid);
        File outputFile = findReportOutputFile(reportTask, getTestOperationResult());
        assertThat(outputFile).as("aggregated report output file").isNotNull();
        assertThat(outputFile.getName()).endsWith(".xlsx");
        try (var inputStream = new FileInputStream(outputFile); var workbook = new XSSFWorkbook(inputStream)) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            return workbook.getSheetAt(0);
        }
    }
}
