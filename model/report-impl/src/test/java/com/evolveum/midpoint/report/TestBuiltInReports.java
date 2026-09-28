/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;
import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.evolveum.midpoint.test.TestObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * This is to test built-in midPoint reports (mentioned in {@link CommonInitialObjects}) and their export to CSV.
 *
 * . `test10x` - {@link CommonInitialObjects#REPORT_RECONCILIATION}
 * . `test110` - {@link CommonInitialObjects#REPORT_USER_LIST}
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestBuiltInReports extends TestCsvReport {

    private static final int USERS = 10;

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "built-in");

    private static final String SAFE_SCRIPTS_ONLY = "safe-scripts-only";

    private static final TestObject<?> ORG_1 = TestObject.file(TEST_DIR, "org-1.xml", "daddd826-0cb1-4df7-9ee3-f480933e6ac3");
    private static final TestObject<?> ORG_2 = TestObject.file(TEST_DIR, "org-2.xml", "6989479c-d729-4ea8-8f3b-1a9c22510576");

    private static final DummyTestResource RESOURCE_DUMMY_TARGET = new DummyTestResource(
            TEST_DIR, "resource-dummy-target.xml", "20797685-10d3-4250-9648-8a004a7ee624", "target");

    // data for reconciliation report
    private static final int RECONCILIATION_COLUMNS = 5; // shadow name
    private static final int C_RECONCILIATION_NAME = 0; // shadow name
    private static final int C_RECONCILIATION_SITUATION = 2;
    private static final int C_RECONCILIATION_OWNER = 3;

    // data for user list report
    private static final int USER_LIST_COLUMNS = 7;
    private static final int C_USER_LIST_NAME = 0;
    private static final int C_USER_LIST_ROLE = 3;
    private static final int C_USER_LIST_ORG = 4;
    private static final int C_USER_LIST_ACCOUNT = 5;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        setDefaultExpressionProfile(SAFE_SCRIPTS_ONLY, initTask, initResult);

        // TODO consider moving this initialization to the superclass
        initTestObjects(initTask, initResult,
                ORG_1, ORG_2,
                ARCHETYPE_REPORT,
                ARCHETYPE_COLLECTION_REPORT,
                OBJECT_COLLECTION_SHADOW_ALL,
                REPORT_RECONCILIATION,
                CommonInitialObjects.REPORT_USER_LIST);

        RESOURCE_DUMMY_TARGET.initAndTest(this, initTask, initResult);

        modelObjectCreatorFor(UserType.class)
                .withObjectCount(USERS)
                .withNamePattern("user-%04d")
                .withCustomizer((u, number) -> u.getAssignment().add(RESOURCE_DUMMY_TARGET.assignmentTo(ACCOUNT_DEFAULT)))
                .execute(initResult);

        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(ORG_1.assignmentTo(),
                                ORG_2.assignmentTo())
                        .asObjectDelta(USER_JACK.oid),
                null, initTask, initResult);

        RESOURCE_DUMMY_TARGET.addAccount("user-extra");
        importAccountsRequest() // we need to see this shadow with UNMATCHED situation
                .withResourceOid(RESOURCE_DUMMY_TARGET.oid)
                .withProcessingAllAccounts()
                .executeOnForeground(initResult);
    }

    @Test
    public void test100ReconciliationReportDefaultParameters() throws CommonException, IOException {
        var task = getTestTask();
        var result = task.getResult();

        when("reconciliation report is created");
        var lines = REPORT_RECONCILIATION.export()
                .withParameter(PARAM_RESOURCE_REF, RESOURCE_DUMMY_TARGET.ref())
                .execute(result);

        then("it contains all the data");
        assertReconciliationFull(lines);
    }

    @Test
    public void test102ReconciliationReportWithResourceAndObjectClass() throws CommonException, IOException {
        var task = getTestTask();
        var result = task.getResult();

        when("reconciliation report is created");
        var lines = REPORT_RECONCILIATION.export()
                .withParameter(PARAM_RESOURCE_REF, RESOURCE_DUMMY_TARGET.ref())
                .withParameter(PARAM_OBJECT_CLASS, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME)
                .execute(result);

        then("it contains all the data");
        assertReconciliationFull(lines);
    }

    private void assertReconciliationFull(List<String> lines) throws IOException {
        assertCsv(lines, "after")
                .sortBy(C_RECONCILIATION_NAME)
                .display()
                .assertRecords(USERS + 1) // +1 for orphaned user-extra
                .assertColumns(RECONCILIATION_COLUMNS)
                .record(0)
                .assertValue(C_RECONCILIATION_NAME, "user-0000")
                .assertValue(C_RECONCILIATION_SITUATION, "Linked")
                .assertValue(C_RECONCILIATION_OWNER, "user-0000")
                .end()
                .record(USERS)
                .assertValue(C_RECONCILIATION_NAME, "user-extra")
                .assertValue(C_RECONCILIATION_SITUATION, "Unmatched")
                .assertValue(C_RECONCILIATION_OWNER, "");
    }

    @Test
    public void test104ReconciliationReportWrongResource() throws CommonException, IOException {
        var task = getTestTask();
        var result = task.getResult();

        when("reconciliation report is created");
        var lines = REPORT_RECONCILIATION.export()
                .withParameter(PARAM_RESOURCE_REF,
                        new ObjectReferenceType()
                                .oid(UUID.randomUUID().toString())
                                .type(ResourceType.COMPLEX_TYPE))
                .execute(result);

        then("it contains no data");
        assertReconciliationEmpty(lines);
    }

    @Test
    public void test106ReconciliationReportWrongObjectClass() throws CommonException, IOException {
        var task = getTestTask();
        var result = task.getResult();

        when("reconciliation report is created");
        var lines = REPORT_RECONCILIATION.export()
                .withParameter(PARAM_OBJECT_CLASS, "nothing")
                .execute(result);

        then("it contains no data");
        assertReconciliationEmpty(lines);
    }

    private void assertReconciliationEmpty(List<String> lines) throws IOException {
        assertCsv(lines, "after")
                .sortBy(C_RECONCILIATION_NAME)
                .display()
                .assertRecords(0)
                .assertColumns(RECONCILIATION_COLUMNS);
    }

    @Test
    public void test110UserListReportDefaultParameters() throws CommonException, IOException {
        var task = getTestTask();
        var result = task.getResult();

        setDefaultExpressionProfile(null, task, result);

        when("user list report is created");
        var lines = CommonInitialObjects.REPORT_USER_LIST.export()
                .execute(result);

        then("it contains all the data");
        assertCsv(lines, "after")
                .sortBy(C_USER_LIST_NAME)
                .display()
                .assertRecords(USERS + 3) // +3 for administrator, jack and will
                .assertColumns(USER_LIST_COLUMNS)
                .forRecord(C_USER_LIST_NAME, "user-0000",
                        a -> a.assertValue(C_USER_LIST_ACCOUNT, "user-0000 (Resource: resource-target)")
                                .assertValue(C_USER_LIST_ROLE, "")
                                .assertValue(C_USER_LIST_ORG, ""))
                .forRecord(C_USER_LIST_NAME, "jack", // he has no accounts
                        a -> a.assertValue(C_USER_LIST_ACCOUNT, "")
                                .assertValue(C_USER_LIST_ROLE, "Superuser")
                                .assertValue(C_USER_LIST_ORG,
                                        s -> s.isIn("org-1,org-2", "org-2,org-1")));
    }
}
