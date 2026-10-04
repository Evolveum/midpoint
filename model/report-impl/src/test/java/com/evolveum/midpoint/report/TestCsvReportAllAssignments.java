/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestReport;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestCsvReportAllAssignments extends TestCsvReport {

    private static final int USERS = 50;
    private static final int REPORT_COLUMN_COUNT = 10;
    private static final int C_USER = 0;
    private static final int C_NAME = 1;
    private static final int C_ARCHETYPE = 2;
    private static final int C_RELATION = 3;
    private static final int C_PATH = 4;
    private static final int C_PARENT = 5;
    private static final int C_ACTIVATION = 6;
    private static final int C_VALID_TO = 7;
    private static final int C_SINCE = 8;
    private static final int C_SOURCE = 9;
    private static final String SAFE_SCRIPTS_ONLY = "safe-scripts-only";

    private static final TestReport REPORT_INDIRECT_ASSIGNMENTS = TestReport.classPath(DIR_REPORTS,
            "report-indirect-assignments.xml", "7f1695f2-d826-4d78-a046-b8249b79d2b5");

    private static final TestTask TASK_EXPORT_CLASSIC_ROLE_CACHING = new TestTask(TEST_DIR_REPORTS,
            "task-export-role-caching.xml", "0ff414b6-76c6-4d38-95e8-d2d34c7a11cb");

    private String appArchetypeOid;
    private String appRoleOid;
    private XMLGregorianCalendar user3ValidTo;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // Only for Native repo, as Generic repo does not support reference search.
        if (!isNativeRepository()) {
            return;
        }
        super.initSystem(initTask, initResult);

        setDefaultExpressionProfile(SAFE_SCRIPTS_ONLY, initTask, initResult);

        CommonInitialObjects.ARCHETYPE_REPORT.init(this, initTask, initResult);
        REPORT_INDIRECT_ASSIGNMENTS.init(this, initTask, initResult); // new style (2023-02 and later)
        repoAdd(TASK_EXPORT_CLASSIC_ROLE_CACHING, initResult); // old style

        appArchetypeOid = addObject(new ArchetypeType().name("Application")
                .asPrismObject(), initTask, initResult);
        String orgOid = addObject(new OrgType().name("Org1")
                .asPrismObject(), initTask, initResult);

        // adding role chain: businessRole -> appRole -> appService
        String appServiceOid = addObject(new ServiceType().name("appService")
                .assignment(new AssignmentType().targetRef(appArchetypeOid, ArchetypeType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
        appRoleOid = addObject(new RoleType().name("appRole")
                .inducement(new AssignmentType().targetRef(appServiceOid, ServiceType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
        String businessRoleOid = addObject(new RoleType().name("businessRole")
                .inducement(new AssignmentType().targetRef(appRoleOid, RoleType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);

        // one user without metadata to check the report's robustness
        switchAccessesMetadata(false, initTask, initResult);
        addObject(new UserType().name("user-without-metadata")
                .assignment(new AssignmentType().targetRef(businessRoleOid, RoleType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
        switchAccessesMetadata(true, initTask, initResult);

        // Initialization of the bulk of the users
        for (int i = 1; i <= USERS; i++) {
            UserType user = new UserType()
                    .name(String.format("user-%05d", i))
                    .assignment(new AssignmentType().targetRef(businessRoleOid, RoleType.COMPLEX_TYPE));
            if (i % 3 == 0) {
                // To mix it up, every third user has also direct assignment to the service.
                XMLGregorianCalendar validTo = MiscUtil.asXMLGregorianCalendar(Instant.now().plus(1, ChronoUnit.DAYS));
                user.assignment(new AssignmentType()
                        .targetRef(appServiceOid, ServiceType.COMPLEX_TYPE)
                        .activation(new ActivationType()
                                // for some output variation
                                .validFrom(MiscUtil.asXMLGregorianCalendar(
                                        Instant.now().minus(REPORT_COLUMN_COUNT, ChronoUnit.DAYS)))
                                .validTo(validTo)));
                if (i == 3) {
                    user3ValidTo = validTo;
                }
                user.assignment(new AssignmentType()
                        .targetRef(orgOid, OrgType.COMPLEX_TYPE,
                                i == 3 ? SchemaConstants.ORG_MANAGER : SchemaConstants.ORG_DEFAULT));
            }
            addObject(user.asPrismObject(), initTask, initResult);
        }

        // One user with metadata pointing to deleted role
        String deletedRoleOid = addObject(new RoleType().name("deletedRole").asPrismObject(), initTask, initResult);
        addObject(new UserType().name("user-with-deleted-role")
                .assignment(new AssignmentType().targetRef(deletedRoleOid, RoleType.COMPLEX_TYPE))
                .asPrismObject(), initTask, initResult);
        deleteObject(RoleType.class, deletedRoleOid);
    }

    @Test
    public void test100RunReport() throws Exception {
        skipIfNotNativeRepository();

        when("report is run without any parameters");
        List<String> rows = REPORT_INDIRECT_ASSIGNMENTS.export()
                .execute(getTestOperationResult());

        then("only rows for that user are exported");
        assertCsv(rows, "after")
                .assertColumns(REPORT_COLUMN_COUNT)
                // 50 * 3 (normal) + 50 // 3 * 2 (direct assignments + orgs) + 3 (without metadata) + deleted + jack
                .assertRecords(187);
    }

    @Test
    public void test200RunReportWithUserParameter() throws Exception {
        skipIfNotNativeRepository();

        when("report is run with userName parameter set");
        List<String> rows = REPORT_INDIRECT_ASSIGNMENTS.export()
                .withParameter("userName", "user-00001")
                .execute(getTestOperationResult());

        then("only rows for that user are exported");
        assertCsv(rows, "after")
                .assertColumns(REPORT_COLUMN_COUNT)
                .assertRecords(3) // rows for user-00001
                .forRecord(C_NAME, "businessRole", record -> record
                        .assertValue(C_USER, "user-00001")
                        .assertValue(C_PATH, "businessRole")
                        .assertValue(C_PARENT, "Direct")
                        .assertValue(C_ACTIVATION, "Enabled")
                        .assertValue(C_VALID_TO, "")
                        .assertValueNotEmpty(C_SINCE)
                        .assertValue(C_SOURCE, ""))
                .forRecord(C_NAME, "appRole", record -> record
                        .assertValue(C_USER, "user-00001")
                        .assertValue(C_PATH, "businessRole -> appRole")
                        .assertValue(C_PARENT, "businessRole"))
                .forRecord(C_NAME, "appService", record -> record
                        .assertValue(C_USER, "user-00001")
                        .assertValue(C_ARCHETYPE, "Application")
                        .assertValue(C_PATH, "businessRole -> appRole -> appService")
                        .assertValue(C_PARENT, "appRole"));
    }

    @Test
    public void test201RunReportForDirectAssignment() throws Exception {
        skipIfNotNativeRepository();

        List<String> rows = REPORT_INDIRECT_ASSIGNMENTS.export()
                .withParameter("userName", "user-00003")
                .execute(getTestOperationResult());

        assertCsv(rows, "after")
                .assertColumns(REPORT_COLUMN_COUNT)
                .forRecords(1,
                        record -> "appService".equals(record.get(C_NAME))
                                && "appService".equals(record.get(C_PATH)),
                        record -> record
                                .assertValue(C_USER, "user-00003")
                                .assertValue(C_PARENT, "Direct")
                                .assertValue(C_ACTIVATION, "Enabled")
                                .assertValue(C_VALID_TO, ReportUtils.prettyPrintForReport(user3ValidTo)))
                .forRecord(C_NAME, "Org1", record -> record
                        .assertValue(C_USER, "user-00003")
                        .assertValue(C_PATH, "Org1")
                        .assertValue(C_RELATION, "manager"));
    }

    @Test
    public void test202RunReportWithoutMetadata() throws Exception {
        skipIfNotNativeRepository();

        List<String> rows = REPORT_INDIRECT_ASSIGNMENTS.export()
                .withParameter("userName", "user-without-metadata")
                .execute(getTestOperationResult());

        assertCsv(rows, "after")
                .assertColumns(REPORT_COLUMN_COUNT)
                .assertRecords(3)
                .allRecords(record -> record
                        .assertValue(C_USER, "Unknown owner")
                        .assertValue(C_PATH, "?")
                        .assertValue(C_PARENT, "?")
                        .assertValue(C_SINCE, "")
                        .assertValue(C_SOURCE, ""));
    }

    @Test
    public void test203RunReportWithDeletedTarget() throws Exception {
        skipIfNotNativeRepository();

        List<String> rows = REPORT_INDIRECT_ASSIGNMENTS.export()
                .withParameter("userName", "user-with-deleted-role")
                .execute(getTestOperationResult());

        assertCsv(rows, "after")
                .assertColumns(REPORT_COLUMN_COUNT)
                .assertRecords(1)
                .record(0, record -> record
                        .assertValue(C_USER, "user-with-deleted-role")
                        .assertValue(C_NAME, "")
                        .assertValue(C_PATH, "null")
                        .assertValue(C_PARENT, "Direct"));
    }

    @Test
    public void test210RunReportWithRoleParameter() throws Exception {
        skipIfNotNativeRepository();

        when("report is run with role parameter set");
        List<String> rows = REPORT_INDIRECT_ASSIGNMENTS.export()
                .withParameter("roleRef", new ObjectReferenceType().oid(appRoleOid).type(RoleType.COMPLEX_TYPE))
                .execute(getTestOperationResult());

        then("only rows with that role are exported");
        assertCsv(rows, "after")
                .assertColumns(REPORT_COLUMN_COUNT)
                // 50 normal + 1 without metadata, but still properly matched by ref search
                .assertRecords(51);
    }

    @Test(enabled = false) // TODO when roleArchetypeRef parameter is supported
    public void test220RunReportWithRoleParameter() throws Exception {
        skipIfNotNativeRepository();

        when("report is run with role archetype parameter set");
        List<String> rows = REPORT_INDIRECT_ASSIGNMENTS.export()
                .withParameter("roleArchetypeRef",
                        new ObjectReferenceType().oid(appArchetypeOid).type(ArchetypeType.COMPLEX_TYPE))
                .execute(getTestOperationResult());

        then("only rows with that role are exported");
        assertCsv(rows, "after")
                .assertColumns(REPORT_COLUMN_COUNT)
                // 50 normal + 16 direct + 1 without metadata, but still properly matched by ref search
                .assertRecords(67);
    }
}
