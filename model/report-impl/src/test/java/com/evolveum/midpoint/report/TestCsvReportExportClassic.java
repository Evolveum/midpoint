/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportExportClassic extends TestCsvReport {

    private static final int USERS = 50;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(TASK_EXPORT_CLASSIC, initResult);

        repoAdd(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN, initResult);
        repoAdd(REPORT_AUDIT_COLLECTION_WITH_VIEW, initResult);
        repoAdd(REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW, initResult);
        repoAdd(REPORT_AUDIT_COLLECTION_WITH_CONDITION, initResult);
        repoAdd(REPORT_AUDIT_COLLECTION_EMPTY, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_VIEW, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_FILTER, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_CONDITION, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_EMPTY, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_PARAM, initResult);
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM, initResult);
        repoAdd(REPORT_SUBREPORT_AS_ROW_USERS, initResult);
        repoAdd(REPORT_SUBREPORT_AUDIT, initResult);
        repoAdd(REPORT_USER_LIST, initResult);
        repoAdd(REPORT_USER_LIST_SCRIPT, initResult);
        repoAdd(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN, initResult);
        repoAdd(REPORT_DASHBOARD_EMPTY, initResult);

        repoAdd(OBJECT_COLLECTION_ALL_AUDIT_RECORDS, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_AUDIT_RECORDS_WITH_VIEW, initResult);
        repoAdd(OBJECT_COLLECTION_AUDIT_EMPTY, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_USERS, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_ROLES, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_USERS_WITH_VIEW, initResult);
        repoAdd(OBJECT_COLLECTION_BASIC_FILTER, initResult);
        repoAdd(OBJECT_COLLECTION_EMPTY, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_RESOURCE, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_ASSIGNMENT_HOLDER, initResult);
        repoAdd(OBJECT_COLLECTION_ALL_TASK, initResult);
        repoAdd(OBJECT_COLLECTION_SHADOW_OF_RESOURCE, initResult);

        repoAdd(DASHBOARD_DEFAULT_COLUMNS, initResult);
        repoAdd(DASHBOARD_EMPTY, initResult);

        createUsers(USERS, initTask, initResult);
    }

    @Test
    public void test001DashboardReportWithDefaultColumn() throws Exception {
        runTest(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN, 8, 3, null);
    }

    @Test
    public void test004DashboardReportEmpty() throws Exception {
        runTest(REPORT_DASHBOARD_EMPTY, 3, 3, null);
    }

    @Test
    public void test100AuditCollectionReportWithDefaultColumn() throws Exception {
        runTest(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN, DONT_COUNT_ROWS, 8, null);
    }

    @Test
    public void test101AuditCollectionReportWithView() throws Exception {
        runTest(REPORT_AUDIT_COLLECTION_WITH_VIEW, DONT_COUNT_ROWS, 2, null);
    }

    @Test
    public void test102AuditCollectionReportWithDoubleView() throws Exception {
        runTest(REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW, DONT_COUNT_ROWS, 3, null);
    }

    @Test
    public void test103AuditCollectionReportWithCondition() throws Exception {
        runTest(REPORT_AUDIT_COLLECTION_WITH_CONDITION, 2, 8, null);
    }

    @Test
    public void test104AuditCollectionReportEmpty() throws Exception {
        runTest(REPORT_AUDIT_COLLECTION_EMPTY, 1, 8, null);
    }

    @Test
    public void test110ObjectCollectionReportWithDefaultColumn() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN, 54, 6, null);
    }

    @Test
    public void test111ObjectCollectionReportWithView() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_WITH_VIEW, 2, 2, null);
    }

    @Test
    public void test112ObjectCollectionReportWithDoubleView() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW, 54, 3, null);
    }

    @Test
    public void test113ObjectCollectionReportWithFilter() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_WITH_FILTER, 3, 2, null);
    }

    @Test
    public void test114ObjectCollectionReportWithFilterAndBasicCollection() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION, 2, 2, null);
    }

    @Test
    public void test115ObjectCollectionReportWithCondition() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_WITH_CONDITION, 2, 6, null);
    }

    @Test
    public void test116ObjectCollectionEmptyReport() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_EMPTY, 1, 6, null);
    }

    @Test
    public void test117ObjectCollectionReportWithFilterAndBasicCollectionWithoutView() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW, 2, 1, null);
    }

    @Test
    public void test118ObjectCollectionWithParamReport() throws Exception {
        ReportParameterType parameters = getParameters("givenName", String.class, "Will");
        runTest(REPORT_OBJECT_COLLECTION_WITH_PARAM, 2, 2, null, parameters);
    }

    @Test
    public void test119ObjectCollectionWithSubreportParamReport() throws Exception {
        runTest(REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM, 2, 2, "\"will\";\"TestRole1230,TestRole123010\"");
    }

    @Test
    public void test120RunMidpointUsers() throws Exception {
        runTest(REPORT_USER_LIST, 54, 6, null);
    }

    @Test
    public void test121RunMidpointUsersScript() throws Exception {
        if (!isOsUnix()) {
            displaySkip();
            return;
        }
        runTest(REPORT_USER_LIST_SCRIPT, 54, 6, null);
        File targetFile = new File(MidPointTestConstants.TARGET_DIR_PATH, "report-users");
        assertTrue("Target file is not there", targetFile.exists());
    }

    @Test
    public void test130ExportUsersWithAssignments() throws Exception {
        UserType user = new UserType()
                .name("moreassignments")
                .assignment(new AssignmentType().targetRef(SystemObjectsType.ROLE_END_USER.value(), RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType().targetRef(SystemObjectsType.ROLE_APPROVER.value(), RoleType.COMPLEX_TYPE));
        addObject(user.asPrismObject(), getTestTask(), getTestTask().getResult());

        runTest(REPORT_SUBREPORT_AS_ROW_USERS, 56, 3, null);
    }

    @Test
    public void test140ExportAuditRecords() throws Exception {
        final Task task = getTestTask();
        final OperationResult result = task.getResult();

        final String name = "test140ExportAuditRecords";
        final UserType user = new UserType()
                .name(name)
                .assignment(new AssignmentType().targetRef(SystemObjectsType.ROLE_END_USER.value(), RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType().targetRef(SystemObjectsType.ROLE_DELEGATOR.value(), RoleType.COMPLEX_TYPE));
        final String oid = addObject(user.asPrismObject(), task, result);

        final PrismObject<UserType> testedUserObject = getObject(UserType.class, oid);
        final UserType testedUser = testedUserObject.asObjectable();

        ObjectDelta delta = prismContext.deltaFactory().object().create(UserType.class, ChangeType.MODIFY);
        delta.setOid(oid);
        delta.setObjectTypeClass(UserType.class);
        delta.addModification(
                createAssignmentModification(
                        new AssignmentType().targetRef(SystemObjectsType.ROLE_APPROVER.value(), RoleType.COMPLEX_TYPE), true));
        delta.addModification(
                createAssignmentModification(
                        testedUser.getAssignment().stream()
                                .filter(a -> SystemObjectsType.ROLE_END_USER.value().equals(a.getTargetRef().getOid()))
                                .map(a -> a.clone())
                                .findFirst().orElseThrow(), false)
        );

        executeChanges(delta, ModelExecuteOptions.create(), task, result);

        ReportParameterType parameters = getParameters("targetName", String.class, name);

        runTest(REPORT_SUBREPORT_AUDIT, 5, 4, null, parameters);
    }

    private void runTest(TestResource<ReportType> reportResource, int expectedRows, int expectedColumns,
            String lastLine, ReportParameterType parameters) throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        if (parameters != null) {
            modifyObjectReplaceContainer(TaskType.class,
                    TASK_EXPORT_CLASSIC.oid,
                    ItemPath.create(TaskType.F_ACTIVITY,
                            ActivityDefinitionType.F_WORK,
                            WorkDefinitionsType.F_REPORT_EXPORT,
                            ClassicReportImportWorkDefinitionType.F_REPORT_PARAM),
                    task,
                    result,
                    parameters);
        }

        dummyTransport.clearMessages();

        runExportTaskClassic(reportResource, result);

        when();

        waitForTaskCloseOrSuspend(TASK_EXPORT_CLASSIC.oid);

        then();

        assertTask(TASK_EXPORT_CLASSIC.oid, "after")
                .assertSuccess()
                .display()
                .assertHasArchetype(SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK.value());

        PrismObject<ReportType> report = getObject(ReportType.class, reportResource.oid);
        basicCheckOutputFile(report, expectedRows, expectedColumns, lastLine);

        assertNotificationMessage(reportResource);
    }

    private void runTest(TestResource<ReportType> reportResource, int expectedRows, int expectedColumns, String lastLine)
            throws Exception {
        runTest(reportResource, expectedRows, expectedColumns, lastLine, null);
    }

    private ReportParameterType getParameters(String name, Class<String> type, Object realValue) throws SchemaException {
        ReportParameterType reportParam = new ReportParameterType();
        PrismContainerValue<ReportParameterType> reportParamValue = reportParam.asPrismContainerValue();

        QName typeName = prismContext.getSchemaRegistry().determineTypeForClass(type);
        MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(
                new QName(ReportConstants.NS_EXTENSION, name), typeName);
        def.setDynamic(true);
        def.setRuntimeSchema(true);
        def.toMutable().setMaxOccurs(1);

        PrismProperty<Object> prop = def.instantiate();
        prop.addRealValue(realValue);
        reportParamValue.add(prop);

        return reportParam;
    }
}
