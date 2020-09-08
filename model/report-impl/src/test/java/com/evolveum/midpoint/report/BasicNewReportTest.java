/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author skublik
 */

public abstract class BasicNewReportTest extends AbstractReportIntegrationTest {

    protected static final File USER_WILL_FILE = new File(TEST_DIR_COMMON, "user-will.xml");
    protected static final String USER_WILL_OID = "c0c010c0-d34d-b33f-f00d-111111111122";

    public static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
    public static final File COLLECTION_ROLE_FILE = new File(COMMON_DIR, "object-collection-all-role.xml");
    public static final File COLLECTION_RESOURCE_FILE = new File(COMMON_DIR, "object-collection-all-resource.xml");
    public static final File COLLECTION_TASK_FILE = new File(COMMON_DIR, "object-collection-all-task.xml");
    public static final File COLLECTION_USER_FILE = new File(COMMON_DIR, "object-collection-all-user.xml");
    public static final File COLLECTION_AUDIT_FILE = new File(COMMON_DIR, "object-collection-all-audit-records.xml");
    public static final File COLLECTION_ASSIGNMENT_HOLDER_FILE = new File(COMMON_DIR, "object-collection-all-assignment-holder.xml");
    public static final File COLLECTION_SHADOW_FILE = new File(COMMON_DIR, "object-collection-shadow-of-resource.xml");
    public static final File DASHBOARD_DEFAULT_COLUMNS_FILE = new File(COMMON_DIR, "dashboard-default-columns.xml");
    public static final File DASHBOARD_VIEW_FILE = new File(COMMON_DIR, "dashboard-with-view.xml");
    public static final File COLLECTION_ROLE_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-role-with-view.xml");
    public static final File COLLECTION_RESOURCE_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-resource-with-view.xml");
    public static final File COLLECTION_TASK_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-task-with-view.xml");
    public static final File COLLECTION_USER_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-user-with-view.xml");
    public static final File COLLECTION_AUDIT_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-audit-records-with-view.xml");
    public static final File COLLECTION_ASSIGNMENT_HOLDER_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-assignment-holder-with-view.xml");
    public static final File COLLECTION_SHADOW_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-shadow-of-resource-with-view.xml");
    public static final File DASHBOARD_TRIPLE_VIEW_FILE = new File(COMMON_DIR, "dashboard-with-triple-view.xml");
    public static final File COLLECTION_BASIC = new File(COMMON_DIR, "object-collection-basic-filter.xml");

    public static final File REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_DIR, "report-dashboard-with-default-column.xml");
    public static final File REPORT_DASHBOARD_WITH_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-dashboard-with-view.xml");
    public static final File REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-dashboard-with-triple-view.xml");

    public static final File REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-default-column.xml");
    public static final File REPORT_AUDIT_COLLECTION_WITH_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-view.xml");
    public static final File REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-double-view.xml");
    public static final File REPORT_AUDIT_COLLECTION_WITH_CONDITION_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-condition.xml");

    public static final File REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-default-column.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-view.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-double-view.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_FILTER_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-filter.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-filter-and-basic-collection.xml");
    public static final File REPORT_OBJECT_COLLECTION_WITH_CONDITION_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-condition.xml");
    public static final File REPORT_WITH_IMPORT_SCRIPT = new File(TEST_REPORTS_DIR, "report-with-import-script.xml");

    public static final String REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8582";
    public static final String REPORT_DASHBOARD_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8533";
    public static final String REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID = "2b87aa2e-dd86-4842-bcf5-76200a9a8533";

    public static final String REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85bc";
    public static final String REPORT_AUDIT_COLLECTION_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85cd";
    public static final String REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85fg";
    public static final String REPORT_AUDIT_COLLECTION_WITH_CONDITION_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85rr";

    public static final String REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85ab";
    public static final String REPORT_OBJECT_COLLECTION_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85de";
    public static final String REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85ef";
    public static final String REPORT_OBJECT_COLLECTION_WITH_FILTER_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85gh";
    public static final String REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85hi";
    public static final String REPORT_OBJECT_COLLECTION_WITH_CONDITION_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a851a";
    public static final String REPORT_WITH_IMPORT_SCRIPT_OID = "2b44aa2e-dd86-4842-bcf5-762c8c4a851a";

    public static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CleanupPolicyType policy = new CleanupPolicyType();
        policy.setMaxRecords(0);
        modelAuditService.cleanupAudit(policy, initTask, initResult);

        DummyResourceContoller dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.extendSchemaPirate();
        PrismObject<ResourceType> resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
        dummyResourceCtl.setResource(resourceDummy);
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null, initTask, initResult);
        importObjectFromFile(COLLECTION_BASIC, initResult);
        importObjectFromFile(USER_WILL_FILE, initResult);
        importObjectFromFile(COLLECTION_ROLE_FILE, initResult);
        importObjectFromFile(COLLECTION_USER_FILE, initResult);
        importObjectFromFile(COLLECTION_RESOURCE_FILE, initResult);
        importObjectFromFile(COLLECTION_TASK_FILE, initResult);
        importObjectFromFile(COLLECTION_AUDIT_FILE, initResult);
        importObjectFromFile(COLLECTION_ASSIGNMENT_HOLDER_FILE, initResult);
        importObjectFromFile(COLLECTION_SHADOW_FILE, initResult);
        importObjectFromFile(DASHBOARD_DEFAULT_COLUMNS_FILE, initResult);
        importObjectFromFile(DASHBOARD_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_ROLE_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_USER_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_RESOURCE_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_TASK_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_AUDIT_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_ASSIGNMENT_HOLDER_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_SHADOW_WITH_VIEW_FILE, initResult);
        importObjectFromFile(DASHBOARD_TRIPLE_VIEW_FILE, initResult);

        importObjectFromFile(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_CONDITION_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_FILTER_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_CONDITION_FILE, initResult);
        importObjectFromFile(REPORT_WITH_IMPORT_SCRIPT, initResult);
    }

    @Test
    public void test001CreateDashboardReportWithDefaultColumn() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test002CreateDashboardReportWithView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_DASHBOARD_WITH_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test003CreateDashboardReportWithTripleView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test101CreateAuditCollectionReportWithDefaultColumn() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test102CreateAuditCollectionReportWithView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test103CreateAuditCollectionReportWithDoubleView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test104CreateAuditCollectionReportWithCondition() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_CONDITION_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test110CreateObjectCollectionReportWithDefaultColumn() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test111CreateObjectCollectionReportWithView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test112CreateObjectCollectionReportWithDoubleView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test113CreateObjectCollectionReportWithFilter() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_FILTER_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test114CreateObjectCollectionReportWithFilterAndBasicCollection() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test
    public void test115CreateObjectCollectionReportWithCondition() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_CONDITION_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    protected PrismObject<TaskType> runReport(PrismObject<ReportType> report, boolean errorOk) throws Exception {
        Task task = createTask(OP_CREATE_REPORT);
        OperationResult result = task.getResult();
        PrismObject<ReportType> reportBefore = report.clone();
        report.asObjectable().setFileFormat(getFileFormatConfiguration());
        ObjectDelta<ReportType> diffDelta = reportBefore.diff(report, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
        executeChanges(diffDelta, ModelExecuteOptions.createRaw(), task, result);

        // WHEN
        when();
        reportManager.runReport(report, null, task, result);

        assertInProgress(result);

        display("Background task (running)", task);

        waitForTaskFinish(task.getOid(), false, DEFAULT_TASK_WAIT_TIMEOUT, errorOk);

        // THEN
        then();
        PrismObject<TaskType> finishedTask = getTask(task.getOid());
        display("Background task (finished)", finishedTask);

        return finishedTask;
    }

    protected PrismObject<TaskType> importReport(PrismObject<ReportType> report, String pathToImportFile, boolean errorOk) throws Exception {
        Task task = createTask(OP_IMPORT_REPORT);
        OperationResult result = task.getResult();
        PrismObject<ReportType> reportBefore = report.clone();
        report.asObjectable().setFileFormat(getFileFormatConfiguration());
        ObjectDelta<ReportType> diffDelta = reportBefore.diff(report, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
        executeChanges(diffDelta, ModelExecuteOptions.createRaw(), task, result);

        PrismObject<ReportDataType> reportData = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(ReportDataType.class).instantiate();
        reportData.asObjectable().setFileFormat(getFileFormatConfiguration().getType());
        reportData.asObjectable().setFilePath(new File(pathToImportFile).getAbsolutePath());
        reportData.asObjectable().setName(new PolyStringType(report.getName()));
        String reportDataOid = addObject(reportData, task, result);
        reportData.setOid(reportDataOid);

        // WHEN
        when();
        reportManager.importReport(report, reportData, task, result);

        assertInProgress(result);

        display("Background task (running)", task);

        waitForTaskFinish(task.getOid(), false, DEFAULT_TASK_WAIT_TIMEOUT, errorOk);

        // THEN
        then();
        PrismObject<TaskType> finishedTask = getTask(task.getOid());
        display("Background task (finished)", finishedTask);

        return finishedTask;
    }

    protected abstract FileFormatConfigurationType getFileFormatConfiguration();

    protected List<String> basicCheckOutputFile(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        File outputFile = findOutputFile(report);
        displayValue("Found report file", outputFile);
        assertNotNull("No output file for " + report, outputFile);
        List<String> lines = Files.readAllLines(Paths.get(outputFile.getPath()));
        displayValue("Report content (" + lines.size() + " lines)", String.join("\n", lines));
        outputFile.renameTo(new File(outputFile.getParentFile(), "processed-" + outputFile.getName()));
        return lines;
    }

    protected File findOutputFile(PrismObject<ReportType> report) {
        String filePrefix = report.getName().getOrig();
        File[] matchingFiles = EXPORT_DIR.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(filePrefix);
            }
        });
        if (matchingFiles.length == 0) {
            return null;
        }
        if (matchingFiles.length > 1) {
            throw new IllegalStateException("Found more than one output files for " + report + ": " + Arrays.toString(matchingFiles));
        }
        return matchingFiles[0];
    }
}
