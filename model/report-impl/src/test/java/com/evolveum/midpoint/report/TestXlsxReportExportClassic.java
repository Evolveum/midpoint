/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.common.MimeTypeUtil.MIME_APPLICATION_VND_MSEXCEL_2007;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests XLSX export for classic report tasks.
 *
 * Verifies that object collection and dashboard reports are exported as valid XLSX workbooks,
 * that XLSX notifications use the correct content type.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestXlsxReportExportClassic extends EmptyReportIntegrationTest {

    private static final TestObject<TaskType> TASK_DISTRIBUTED_EXPORT = TestObject.file(
            TEST_DIR_REPORTS,
            "task-distributed-export-users.xml",
            "5ab8f8c6-df1a-4580-af8b-a899f240b44f");

    private static final TestObject<ReportType> REPORT_DISTRIBUTED_EXPORT = TestObject.file(
            TEST_DIR_REPORTS,
            "report-object-collection-users.xml",
            "64e13165-21e5-419a-8d8b-732895109f84");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        addObject(USER_WILL, initTask, initResult);
        addObject(USER_JACK, initTask, initResult);
        repoAdd(TASK_EXPORT_CLASSIC, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN, initResult);
        repoAdd(REPORT_DISTRIBUTED_EXPORT, initResult);
        repoAdd(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_USERS, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_ROLES, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_RESOURCE, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_ASSIGNMENT_HOLDER, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_TASK, initResult);
        repoAdd(OBJECT_COLLECTION_SHADOW_OF_RESOURCE, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_AUDIT_RECORDS, initResult);
        repoAdd(DASHBOARD_DEFAULT_COLUMNS, initResult);
    }

    @Override
    protected FileFormatConfigurationType getFileFormatConfiguration() {
        return new FileFormatConfigurationType().type(FileFormatTypeType.XLSX);
    }

    @Test
    public void test100ExportObjectCollection() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        when();
        runExportTaskClassic(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN, result);
        waitForTaskCloseOrSuspend(TASK_EXPORT_CLASSIC.oid);

        then();
        assertTask(TASK_EXPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK.value());

        PrismObject<TaskType> reportTask = getObject(TaskType.class, TASK_EXPORT_CLASSIC.oid);
        File outputFile = findReportOutputFile(reportTask, result);
        assertThat(outputFile).as("report output file").isNotNull();
        assertThat(outputFile.getName()).endsWith(".xlsx");

        try (var inputStream = new FileInputStream(outputFile);
                var workbook = new XSSFWorkbook(inputStream)) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getPhysicalNumberOfCells()).isEqualTo(6);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(5);
            assertThat(IntStream.range(1, sheet.getLastRowNum())
                    .mapToObj(sheet::getRow)
                    .filter(java.util.Objects::nonNull)
                    .flatMap(row -> IntStream.range(0, row.getPhysicalNumberOfCells())
                            .mapToObj(row::getCell)
                            .filter(java.util.Objects::nonNull)
                            .map(Cell::getStringCellValue))
                    .toList())
                    .contains("jack", "will");
        }

        assertNotificationMessage(
                REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN.getObjectable(),
                MIME_APPLICATION_VND_MSEXCEL_2007);
    }

    @Test
    public void test110ExportDashboard() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyTransport.clearMessages();

        modifyObjectReplaceProperty(
                ReportType.class,
                REPORT_DASHBOARD_WITH_DEFAULT_COLUMN.oid,
                ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_STORE_EXPORTED_WIDGET_DATA),
                task,
                result,
                StoreExportedWidgetDataType.WIDGET_AND_FILE);

        when();
        runExportTaskClassic(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN, result);
        waitForTaskCloseOrSuspend(TASK_EXPORT_CLASSIC.oid);

        then();
        assertTask(TASK_EXPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK.value());

        PrismObject<TaskType> reportTask = getObject(TaskType.class, TASK_EXPORT_CLASSIC.oid);
        File outputFile = findReportOutputFile(reportTask, result);
        assertThat(outputFile).as("dashboard report output file").isNotNull();
        assertThat(outputFile.getName()).endsWith(".xlsx");

        try (var inputStream = new FileInputStream(outputFile); var workbook = new XSSFWorkbook(inputStream)) {
            // summary sheet + one sheet per widget with table data
            assertThat(workbook.getNumberOfSheets()).isGreaterThan(1);

            Sheet summarySheet = workbook.getSheetAt(0);
            assertThat(cellValues(summarySheet))
                    .contains("Resources all", "User all")
                    .doesNotContain("jack", "will");
            List<String> widgetSheetValues = new ArrayList<>();
            for (int i = 1; i < workbook.getNumberOfSheets(); i++) {
                widgetSheetValues.addAll(cellValues(workbook.getSheetAt(i)));
            }
            assertThat(widgetSheetValues).contains("jack", "will");
        }

        PrismObject<DashboardType> dashboard = getObject(DashboardType.class, DASHBOARD_DEFAULT_COLUMNS.oid);
        assertThat(dashboard.asObjectable().getWidget())
                .allSatisfy(widget -> assertThat(widget.getData().getStoredData()).isNotNull());

        assertNotificationMessage(
                REPORT_DASHBOARD_WITH_DEFAULT_COLUMN.getObjectable(),
                MIME_APPLICATION_VND_MSEXCEL_2007);
    }

    private static List<Cell> cells(Sheet sheet) {
        return IntStream.rangeClosed(sheet.getFirstRowNum(), sheet.getLastRowNum())
                .mapToObj(sheet::getRow)
                .filter(Objects::nonNull)
                .flatMap(row -> IntStream.range(0, row.getLastCellNum())
                        .mapToObj(row::getCell)
                        .filter(Objects::nonNull))
                .toList();
    }

    private static List<String> cellValues(Sheet sheet) {
        return cells(sheet).stream().map(Cell::getStringCellValue).toList();
    }

    @Test
    public void test200DistributedExport() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();
        int reportDataCountBefore = repositoryService.countObjects(ReportDataType.class, null, null, result);

        modifyObjectReplaceContainer(
                ReportType.class,
                REPORT_DISTRIBUTED_EXPORT.oid,
                ReportType.F_FILE_FORMAT,
                task,
                result,
                getFileFormatConfiguration());
        repoAdd(TASK_DISTRIBUTED_EXPORT, result);
        changeTaskReport(
                REPORT_DISTRIBUTED_EXPORT,
                ItemPath.create(
                        TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_DISTRIBUTED_REPORT_EXPORT,
                        DistributedReportExportWorkDefinitionType.F_REPORT_REF),
                TASK_DISTRIBUTED_EXPORT);

        when();
        rerunTask(TASK_DISTRIBUTED_EXPORT.oid, result);
        waitForTaskCloseOrSuspend(TASK_DISTRIBUTED_EXPORT.oid);

        then();
        assertTask(TASK_DISTRIBUTED_EXPORT.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_DISTRIBUTED_TASK.value());

        PrismObject<TaskType> reportTask = getObject(TaskType.class, TASK_DISTRIBUTED_EXPORT.oid);
        File outputFile = findReportOutputFile(reportTask, result);
        assertThat(outputFile).as("aggregated report output file").isNotNull();
        assertThat(outputFile.getName()).endsWith(".xlsx");

        try (var inputStream = new FileInputStream(outputFile); var workbook = new XSSFWorkbook(inputStream)) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            Sheet sheet = workbook.getSheetAt(0);
            // header + administrator, jack, will + subscription footer
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(5);
            assertThat(cellValues(sheet)).contains("jack", "will", "administrator");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");
        }

        // only the aggregated object remains; partial ones were consumed
        assertThat(repositoryService.countObjects(ReportDataType.class, null, null, result))
                .isEqualTo(reportDataCountBefore + 1);
    }
}
