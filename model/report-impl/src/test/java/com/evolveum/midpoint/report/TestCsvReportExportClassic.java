/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report;

import static com.evolveum.midpoint.schema.util.ReportParameterTypeUtil.createParameters;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
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
        REPORT_ASSIGNMENTS_LEFT_JOIN.init(this, initTask, initResult);
        REPORT_ASSIGNMENTS_INNER_JOIN.init(this, initTask, initResult);
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
        repoAdd(REPORT_OBJECT_COLLECTION_WITH_VALUE_METADATA, initResult);

        repoAdd(DASHBOARD_DEFAULT_COLUMNS, initResult);
        repoAdd(DASHBOARD_EMPTY, initResult);

        createUsers(USERS, initTask, initResult);
    }

    @Test
    public void test001DashboardReportWithDefaultColumn() throws Exception {
        testClassicExport(REPORT_DASHBOARD_WITH_DEFAULT_COLUMN, 8, 3, null);
    }

    @Test
    public void test004DashboardReportEmpty() throws Exception {
        testClassicExport(REPORT_DASHBOARD_EMPTY, 3, 3, null);
    }

    @Test
    public void test100AuditCollectionReportWithDefaultColumn() throws Exception {
        testClassicExport(REPORT_AUDIT_COLLECTION_WITH_DEFAULT_COLUMN, DONT_COUNT_ROWS, 8, null);
    }

    @Test
    public void test101AuditCollectionReportWithView() throws Exception {
        testClassicExport(REPORT_AUDIT_COLLECTION_WITH_VIEW, DONT_COUNT_ROWS, 2, null);
    }

    @Test
    public void test102AuditCollectionReportWithDoubleView() throws Exception {
        testClassicExport(REPORT_AUDIT_COLLECTION_WITH_DOUBLE_VIEW, DONT_COUNT_ROWS, 3, null);
    }

    @Test
    public void test103AuditCollectionReportWithCondition() throws Exception {
        testClassicExport(REPORT_AUDIT_COLLECTION_WITH_CONDITION, 2, 8, null);
    }

    @Test
    public void test104AuditCollectionReportEmpty() throws Exception {
        testClassicExport(REPORT_AUDIT_COLLECTION_EMPTY, 1, 8, null);
    }

    @Test
    public void test110ObjectCollectionReportWithDefaultColumn() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_DEFAULT_COLUMN, 54, 5, null);
    }

    @Test
    public void test111ObjectCollectionReportWithView() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_VIEW, 2, 2, null);
    }

    @Test
    public void test112ObjectCollectionReportWithDoubleView() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_DOUBLE_VIEW, 54, 3, null);
    }

    @Test
    public void test113ObjectCollectionReportWithFilter() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_FILTER, 3, 2, null);
    }

    @Test
    public void test114ObjectCollectionReportWithFilterAndBasicCollection() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_FILTER_AND_BASIC_COLLECTION, 2, 2, null);
    }



    @Test
    public void test115ObjectCollectionReportWithCondition() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_CONDITION, 2, 5, null);
    }

    @Test
    public void test116ObjectCollectionEmptyReport() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_EMPTY, 1, 5, null);
    }

    @Test
    public void test117ObjectCollectionReportWithFilterAndBasicCollectionWithoutView() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_FILTER_BASIC_COLLECTION_WITHOUT_VIEW, 2, 1, null);
    }

    @Test
    public void test118ObjectCollectionWithParamReport() throws Exception {
        ReportParameterType parameters = createParameters("givenName", "Will");
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_PARAM, 2, 2, null, parameters);
    }

    @Test
    public void test119ObjectCollectionWithSubreportParamReport() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_SUBREPORT_PARAM, 2, 2, "\"will\";\"TestRole1230,TestRole123010\"");
    }

    @Test
    public void test120RunMidpointUsers() throws Exception {
        testClassicExport(REPORT_USER_LIST, 54, 5, null);
    }

    @Test
    public void test121RunMidpointUsersScript() throws Exception {
        if (!isOsUnix()) {
            displaySkip();
            return;
        }
        testClassicExport(REPORT_USER_LIST_SCRIPT, 54, 5, null);
        File targetFile = new File(MidPointTestConstants.TARGET_DIR_PATH, "report-users");
        assertTrue("Target file is not there", targetFile.exists());
    }

    @Test
    public void test125ObjectCollectionReportWithValueMetadata() throws Exception {
        testClassicExport(REPORT_OBJECT_COLLECTION_WITH_VALUE_METADATA, 54, 3, null);
    }

    @Test
    public void test130ExportUsersWithAssignments() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user = new UserType()
                .name("moreassignments")
                .assignment(new AssignmentType().targetRef(SystemObjectsType.ROLE_END_USER.value(), RoleType.COMPLEX_TYPE))
                .assignment(new AssignmentType().targetRef(SystemObjectsType.ROLE_APPROVER.value(), RoleType.COMPLEX_TYPE));
        addObject(user.asPrismObject(), getTestTask(), getTestTask().getResult());

        List<String> linesLeft = REPORT_ASSIGNMENTS_LEFT_JOIN.export().execute(result);
        assertCsv(linesLeft, "left join")
                .assertRecords(55)
                .assertColumns(3);

        List<String> linesInner = REPORT_ASSIGNMENTS_INNER_JOIN.export().execute(result);
        assertCsv(linesInner, "inner join")
                .assertRecords(4)
                .assertColumns(3);
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

        ReportParameterType parameters = createParameters("targetName", name);

        testClassicExport(REPORT_SUBREPORT_AUDIT, 5, 4, null, parameters);
    }


}
