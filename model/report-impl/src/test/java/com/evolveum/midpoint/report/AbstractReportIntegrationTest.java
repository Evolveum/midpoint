/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.report.api.ReportConstants;

import com.evolveum.midpoint.report.api.ReportManager;

import com.evolveum.midpoint.report.impl.ReportTaskHandler;

import com.evolveum.midpoint.test.util.MidPointTestConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractReportIntegrationTest extends AbstractModelIntegrationTest {

    protected static final File TEST_DIR_COMMON = new File("src/test/resources/common");
    protected static final File EXPORT_DIR = new File("target/midpoint-home/export");

    protected static final File TEST_REPORTS_DIR = new File("src/test/resources/reports");

    protected static final File USER_WILL_FILE = new File(TEST_DIR_COMMON, "user-will.xml");
    protected static final File USER_JACK_FILE = new File(TEST_DIR_COMMON, "user-jack.xml");
    protected static final File ROLE_SUPERUSER_FILE = new File(TEST_DIR_COMMON, "role-superuser.xml");
    protected static final File USER_ADMINISTRATOR_FILE = new File(TEST_DIR_COMMON, "user-administrator.xml");
    protected static final File ARCHETYPE_TASK_FILE = new File(COMMON_DIR, "archetype-task-report.xml");
    protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR_COMMON, "system-configuration.xml");
    protected static final File SYSTEM_CONFIGURATION_SAFE_FILE = new File(TEST_DIR_COMMON, "system-configuration-safe.xml");

    protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

    protected static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
    protected static final File COLLECTION_ROLE_FILE = new File(COMMON_DIR, "object-collection-all-role.xml");
    protected static final File COLLECTION_RESOURCE_FILE = new File(COMMON_DIR, "object-collection-all-resource.xml");
    protected static final File COLLECTION_TASK_FILE = new File(COMMON_DIR, "object-collection-all-task.xml");
    protected static final File COLLECTION_USER_FILE = new File(COMMON_DIR, "object-collection-all-user.xml");
    protected static final File COLLECTION_AUDIT_FILE = new File(COMMON_DIR, "object-collection-all-audit-records.xml");
    protected static final File COLLECTION_ASSIGNMENT_HOLDER_FILE = new File(COMMON_DIR, "object-collection-all-assignment-holder.xml");
    protected static final File COLLECTION_SHADOW_FILE = new File(COMMON_DIR, "object-collection-shadow-of-resource.xml");
    protected static final File DASHBOARD_DEFAULT_COLUMNS_FILE = new File(COMMON_DIR, "dashboard-default-columns.xml");
    protected static final File DASHBOARD_VIEW_FILE = new File(COMMON_DIR, "dashboard-with-view.xml");
    protected static final File COLLECTION_ROLE_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-role-with-view.xml");
    protected static final File COLLECTION_RESOURCE_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-resource-with-view.xml");
    protected static final File COLLECTION_TASK_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-task-with-view.xml");
    protected static final File COLLECTION_USER_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-user-with-view.xml");
    protected static final File COLLECTION_AUDIT_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-audit-records-with-view.xml");
    protected static final File COLLECTION_AUDIT_EMPTY_FILE = new File(COMMON_DIR, "object-collection-audit-empty.xml");
    protected static final File COLLECTION_EMPTY_FILE = new File(COMMON_DIR, "object-collection-empty.xml");
    protected static final File COLLECTION_ASSIGNMENT_HOLDER_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-all-assignment-holder-with-view.xml");
    protected static final File COLLECTION_SHADOW_WITH_VIEW_FILE = new File(COMMON_DIR, "object-collection-shadow-of-resource-with-view.xml");
    protected static final File DASHBOARD_TRIPLE_VIEW_FILE = new File(COMMON_DIR, "dashboard-with-triple-view.xml");
    protected static final File DASHBOARD_EMPTY_FILE = new File(COMMON_DIR, "dashboard-empty.xml");
    protected static final File COLLECTION_BASIC = new File(COMMON_DIR, "object-collection-basic-filter.xml");

    protected static final File REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_DIR, "report-dashboard-with-default-column.xml");
    protected static final File REPORT_DASHBOARD_WITH_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-dashboard-with-view.xml");
    protected static final File REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-dashboard-with-triple-view.xml");
    protected static final File REPORT_DASHBOARD_EMPTY_FILE = new File(TEST_REPORTS_DIR, "report-dashboard-empty.xml");

    protected static final File REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-default-column.xml");
    protected static final File REPORT_AUDIT_COLLECTION_WITH_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-view.xml");
    protected static final File REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-double-view.xml");
    protected static final File REPORT_AUDIT_COLLECTION_WITH_CONDITION_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-with-condition.xml");
    protected static final File REPORT_AUDIT_COLLECTION_EMPTY_FILE = new File(TEST_REPORTS_DIR, "report-audit-collection-empty.xml");

    protected static final File REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-default-column.xml");
    protected static final File REPORT_OBJECT_COLLECTION_WITH_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-view.xml");
    protected static final File REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-double-view.xml");
    protected static final File REPORT_OBJECT_COLLECTION_WITH_FILTER_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-filter.xml");
    protected static final File REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-filter-and-basic-collection.xml");
    protected static final File REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-filter-and-basic-collection-without-view.xml");
    protected static final File REPORT_OBJECT_COLLECTION_WITH_CONDITION_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-condition.xml");
    protected static final File REPORT_OBJECT_COLLECTION_EMPTY_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-empty.xml");
    protected static final File REPORT_OBJECT_COLLECTION_WITH_PARAM_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-param.xml");
    protected static final File REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM_FILE = new File(TEST_REPORTS_DIR, "report-object-collection-with-subreport-param.xml");
    protected static final File REPORT_WITH_IMPORT_SCRIPT_FILE = new File(TEST_REPORTS_DIR, "report-with-import-script.xml");

    protected static final File REPORT_USER_LIST_FILE = new File(TEST_REPORTS_DIR, "report-user-list.xml");
    protected static final File REPORT_USER_LIST_SCRIPT_FILE = new File(TEST_REPORTS_DIR, "report-user-list-script.xml");

    protected static final String REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8582";
    protected static final String REPORT_DASHBOARD_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8533";
    protected static final String REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID = "2b87aa2e-dd86-4842-bcf5-76200a9a8533";
    protected static final String REPORT_DASHBOARD_EMPTY_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a8eq2";

    protected static final String REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85bc";
    protected static final String REPORT_AUDIT_COLLECTION_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85cd";
    protected static final String REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85fg";
    protected static final String REPORT_AUDIT_COLLECTION_WITH_CONDITION_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85rr";
    protected static final String REPORT_AUDIT_COLLECTION_EMPTY_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85qf";

    protected static final String REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85ab";
    protected static final String REPORT_OBJECT_COLLECTION_WITH_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85de";
    protected static final String REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85ef";
    protected static final String REPORT_OBJECT_COLLECTION_WITH_FILTER_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85gh";
    protected static final String REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85hi";
    protected static final String REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85r7";
    protected static final String REPORT_OBJECT_COLLECTION_WITH_CONDITION_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a851a";
    protected static final String REPORT_OBJECT_COLLECTION_EMPTY_LIST_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85sq";
    protected static final String REPORT_OBJECT_COLLECTION_WITH_PARAM_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85ew";
    protected static final String REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM_OID = "2b44aa2e-dd86-4842-bcf5-762c8a9a85qq";
    protected static final String REPORT_WITH_IMPORT_SCRIPT_OID = "2b44aa2e-dd86-4842-bcf5-762c8c4a851a";

    protected static final String REPORT_USER_LIST_OID = "00000000-0000-0000-0000-000000000110";
    protected static final String REPORT_USER_LIST_SCRIPT_OID = "222bf2b8-c89b-11e7-bf36-ebd4e4d45a80";

    protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";

    protected static final String OP_CREATE_REPORT = ReportTaskHandler.class.getName() + "createReport";
    protected static final String OP_IMPORT_REPORT = ReportTaskHandler.class.getName() + "importReport";

    @Autowired
    protected ReportManager reportManager;

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        // System Configuration
        modelService.postInit(initResult);
        try {
            repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        // User administrator
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);

        login(userAdministrator);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        importObjectFromFile(ARCHETYPE_TASK_FILE, initResult);

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
        importObjectFromFile(COLLECTION_AUDIT_EMPTY_FILE, initResult);
        importObjectFromFile(COLLECTION_ASSIGNMENT_HOLDER_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_SHADOW_WITH_VIEW_FILE, initResult);
        importObjectFromFile(COLLECTION_EMPTY_FILE, initResult);
        importObjectFromFile(DASHBOARD_TRIPLE_VIEW_FILE, initResult);
        importObjectFromFile(DASHBOARD_EMPTY_FILE, initResult);

        importObjectFromFile(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_WITH_TRIPLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_DASHBOARD_EMPTY_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_WITH_CONDITION_FILE, initResult);
        importObjectFromFile(REPORT_AUDIT_COLLECTION_EMPTY_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_FILTER_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_CONDITION_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_EMPTY_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_PARAM_FILE, initResult);
        importObjectFromFile(REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM_FILE, initResult);
        importObjectFromFile(REPORT_WITH_IMPORT_SCRIPT_FILE, initResult);

        importObjectFromFile(REPORT_USER_LIST_FILE, initResult);
        importObjectFromFile(REPORT_USER_LIST_SCRIPT_FILE, initResult);
    }

    @Test(priority=1)
    public void test001CreateDashboardReportWithDefaultColumn() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_DASHBOARD_WITH_DEFAULT_COLUMN_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=2)
    public void test002CreateDashboardReportWithView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_DASHBOARD_WITH_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=3)
    public void test003CreateDashboardReportWithTripleView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_DASHBOARD_WITH_TRIPLE_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=4)
    public void test004CreateDashboardReportEmpty() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_DASHBOARD_EMPTY_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=101)
    public void test101CreateAuditCollectionReportWithDefaultColumn() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=102)
    public void test102CreateAuditCollectionReportWithView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=103)
    public void test103CreateAuditCollectionReportWithDoubleView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=104)
    public void test104CreateAuditCollectionReportWithCondition() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_WITH_CONDITION_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=105)
    public void test105CreateAuditCollectionReportEmpty() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_AUDIT_COLLECTION_EMPTY_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=110)
    public void test110CreateObjectCollectionReportWithDefaultColumn() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=111)
    public void test111CreateObjectCollectionReportWithView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=112)
    public void test112CreateObjectCollectionReportWithDoubleView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=113)
    public void test113CreateObjectCollectionReportWithFilter() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_FILTER_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=114)
    public void test114CreateObjectCollectionReportWithFilterAndBasicCollection() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=115)
    public void test115CreateObjectCollectionReportWithCondition() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_CONDITION_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=116)
    public void test116CreateObjectCollectionEmptyReport() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_EMPTY_LIST_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=117)
    public void test117CreateObjectCollectionReportWithFilterAndBasicCollectionWithoutView() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=118)
    public void test118CreateObjectCollectionWithParamReport() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_PARAM_OID);
        runReport(report, getParameters("givenName", String.class, "Will"), false);
        basicCheckOutputFile(report);
    }

    @Test(priority=119)
    public void test119CreateObjectCollectionWithSubreportParamReport() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=120)
    public void test120RunMidpointUsers() throws Exception {
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_USER_LIST_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
    }

    @Test(priority=121)
    public void test121RunMidpointUsersScript() throws Exception {
        if (!isOsUnix()) {
            displaySkip();
            return;
        }
        PrismObject<ReportType> report = getObject(ReportType.class, REPORT_USER_LIST_SCRIPT_OID);
        runReport(report, false);
        basicCheckOutputFile(report);
        File targetFile = new File(MidPointTestConstants.TARGET_DIR_PATH, "report-users");
        assertTrue("Target file is not there", targetFile.exists());
    }

    private PrismContainer<ReportParameterType> getParameters(String name, Class<String> type, Object realValue) throws SchemaException {
        PrismContainerDefinition<ReportParameterType> paramContainerDef = prismContext.getSchemaRegistry().findContainerDefinitionByElementName(ReportConstants.REPORT_PARAMS_PROPERTY_NAME);
        PrismContainer<ReportParameterType> paramContainer;
        paramContainer = paramContainerDef.instantiate();
        ReportParameterType reportParam = new ReportParameterType();
        PrismContainerValue<ReportParameterType> reportParamValue = reportParam.asPrismContainerValue();
        reportParamValue.revive(prismContext);
        paramContainer.add(reportParamValue);

        QName typeName = prismContext.getSchemaRegistry().determineTypeForClass(type);
        MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                new QName(ReportConstants.NS_EXTENSION, name), typeName);
        def.setDynamic(true);
        def.setRuntimeSchema(true);
        def.toMutable().setMaxOccurs(1);

        PrismProperty prop = def.instantiate();
        prop.addRealValue(realValue);
        reportParamValue.add(prop);

        return paramContainer;
    }

    protected PrismObject<TaskType> runReport(PrismObject<ReportType> report, boolean errorOk) throws Exception {
        return runReport(report, null, errorOk);
    }

    protected PrismObject<TaskType> runReport(PrismObject<ReportType> report, PrismContainer<ReportParameterType> params, boolean errorOk) throws Exception {
        Task task = createTask(OP_CREATE_REPORT);
        OperationResult result = task.getResult();
        PrismObject<ReportType> reportBefore = report.clone();
        report.asObjectable().setFileFormat(getFileFormatConfiguration());
        ObjectDelta<ReportType> diffDelta = reportBefore.diff(report, EquivalenceStrategy.REAL_VALUE_CONSIDER_DIFFERENT_IDS);
        executeChanges(diffDelta, ModelExecuteOptions.createRaw(), task, result);

        // WHEN
        when();
        reportManager.runReport(report, params, task, result);

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

    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }
}
