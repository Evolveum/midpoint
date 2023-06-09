/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;

/**
 * Checks the security of cases and certification campaigns and their constituents (work items, certification cases).
 * Features tested are delegations, candidate assignees, and so on.
 *
 * Intentionally *not* a child of {@link AbstractInitializedSecurityTest}, in order to have a simple environment.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurityGovernance extends AbstractEmptySecurityTest {

    public static final File TEST_DIR = new File("src/test/resources/security/governance");

    private static final TestObject<RoleType> ROLE_APPROVER_COMMON_PARTS = TestObject.file(TEST_DIR, "role-approver-common-parts.xml", "9d06e3c7-a182-4b61-b3b0-5c181eaf6955");
    private static final TestObject<RoleType> ROLE_APPROVER_STANDARD_NEW = TestObject.file(TEST_DIR, "role-approver-standard-new.xml", "3bd0f683-691e-4773-b96b-50cad6960d88");
    private static final TestObject<RoleType> ROLE_APPROVER_STANDARD_LEGACY = TestObject.file(TEST_DIR, "role-approver-standard-legacy.xml", "c548f9ee-4986-4ddd-b1c9-19d1a07ae567");
    private static final TestObject<RoleType> ROLE_APPROVER_STANDARD_WITH_CANDIDATES = TestObject.file(TEST_DIR, "role-approver-standard-with-candidates.xml", "509ce515-35ff-45f6-bf49-0d5b81af515e");

    private static final TestObject<RoleType> ORG_WHEEL = TestObject.file(TEST_DIR, "org-wheel.xml", "2c76f1d8-34a9-4063-bffb-02bde8ee5569");

    private static final TestObject<RoleType> ROLE_1 = TestObject.file(TEST_DIR, "role-1.xml", "a023a613-2d48-4fc8-9958-c56fd9b2592a");
    private static final TestObject<RoleType> ROLE_2 = TestObject.file(TEST_DIR, "role-2.xml", "0e1899bc-88d4-49cd-8a88-a290928a7510");
    private static final TestObject<RoleType> ROLE_3 = TestObject.file(TEST_DIR, "role-3.xml", "f2ce1520-cf71-42c5-962f-394217aa531e");

    private static final TestObject<UserType> USER_1 = TestObject.file(TEST_DIR, "user-1.xml", "96dc9b60-6e02-4c30-a032-cb435d13a7f5");
    private static final TestObject<UserType> USER_APPROVER1 = TestObject.file(TEST_DIR, "user-approver1.xml", "28e36fe8-ab06-40e3-a97a-ee5beaa9d7d1");
    private static final TestObject<UserType> USER_APPROVER2 = TestObject.file(TEST_DIR, "user-approver2.xml", "c8579e49-8635-404b-b41d-453938253f56");
    private static final TestObject<UserType> USER_APPROVER3 = TestObject.file(TEST_DIR, "user-approver3.xml", "83cb3fa9-d7bd-447f-a2e8-339a0fc9eff3");
    private static final TestObject<UserType> USER_WHEEL_MEMBER1 = TestObject.file(TEST_DIR, "user-wheel-member1.xml", "6db795fc-bc78-4286-bff3-77333f20a7c4");

    private static final TestObject<CaseType> CASE_REQUEST_1 = TestObject.file(TEST_DIR, "case-request-1.xml", "578e1d81-7865-4e05-9975-0d144fb6e488");
    private static final TestObject<CaseType> CASE_REQUEST_1_ROLE_1 = TestObject.file(TEST_DIR, "case-request-1-role-1.xml", "c73df3a2-18ca-49f9-80de-81a0d6f2d1a1");
    private static final TestObject<CaseType> CASE_REQUEST_1_ROLE_2 = TestObject.file(TEST_DIR, "case-request-1-role-2.xml", "d726fbcc-c00e-4bf8-aca3-3b8e92bd0903");

    private static final TestObject<CaseType> CASE_REQUEST_2 = TestObject.file(TEST_DIR, "case-request-2.xml", "d0a72b38-3d12-4c98-b35b-c2d5bde1106f");
    private static final TestObject<CaseType> CASE_REQUEST_2_ROLE_3 = TestObject.file(TEST_DIR, "case-request-2-role-3.xml", "5fece299-5463-4e33-b8ba-e299e8bf2b52");

    private static final WorkItemId WORK_ITEM_REQUEST_1_ROLE_1_APPROVER1 = WorkItemId.of(CASE_REQUEST_1_ROLE_1.oid, 5L);
    private static final WorkItemId WORK_ITEM_REQUEST_2_ROLE_3_WHEEL_MEMBER1 = WorkItemId.of(CASE_REQUEST_2_ROLE_3.oid, 4L);

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                ROLE_APPROVER_COMMON_PARTS,
                ROLE_APPROVER_STANDARD_NEW,
                ROLE_APPROVER_STANDARD_LEGACY,
                ROLE_APPROVER_STANDARD_WITH_CANDIDATES,
                ORG_WHEEL,
                ROLE_1,
                ROLE_2,
                ROLE_3,
                USER_1,
                USER_APPROVER1,
                USER_APPROVER2,
                USER_APPROVER3,
                USER_WHEEL_MEMBER1);

        initTestObjectsRaw(initResult,
                CASE_REQUEST_1,
                CASE_REQUEST_1_ROLE_1,
                CASE_REQUEST_1_ROLE_2,
                CASE_REQUEST_2,
                CASE_REQUEST_2_ROLE_3);

    }

    @Test
    public void test100StandardApproverLegacyRole() throws Exception {
        given();
        doStandardSetup(USER_APPROVER1, ROLE_APPROVER_STANDARD_LEGACY);

        then("relevant cases are visible");
        assertGetAllow(CaseType.class, CASE_REQUEST_1.oid); // because all cases are visible for legacy
        assertGetAllow(CaseType.class, CASE_REQUEST_1_ROLE_1.oid);
        assertGetAllow(CaseType.class, CASE_REQUEST_1_ROLE_2.oid); // because all cases are visible for legacy
        assertGetAllow(CaseType.class, CASE_REQUEST_2.oid); // because all cases are visible for legacy
        assertGetAllow(CaseType.class, CASE_REQUEST_2_ROLE_3.oid); // because all cases are visible for legacy
        assertSearchCases(
                CASE_REQUEST_1.oid,
                CASE_REQUEST_1_ROLE_1.oid,
                CASE_REQUEST_1_ROLE_2.oid,
                CASE_REQUEST_2.oid,
                CASE_REQUEST_2_ROLE_3.oid);

        and("relevant work items are visible");
        assertSearchWorkItems(WORK_ITEM_REQUEST_1_ROLE_1_APPROVER1);
    }

    @Test
    public void test110StandardApproverNewRole() throws Exception {
        given();
        doStandardSetup(USER_APPROVER1, ROLE_APPROVER_STANDARD_NEW);

        then("relevant cases are visible");
        assertGetDeny(CaseType.class, CASE_REQUEST_1.oid); // currently only "assigned to me" are visible
        assertGetAllow(CaseType.class, CASE_REQUEST_1_ROLE_1.oid);
        assertGetDeny(CaseType.class, CASE_REQUEST_1_ROLE_2.oid);
        assertGetDeny(CaseType.class, CASE_REQUEST_2.oid);
        assertGetDeny(CaseType.class, CASE_REQUEST_2_ROLE_3.oid);
        assertSearchCases(CASE_REQUEST_1_ROLE_1.oid);

        and("relevant work items are visible");
        assertSearchWorkItems(WORK_ITEM_REQUEST_1_ROLE_1_APPROVER1);
    }

    @Test
    public void test120WheelMemberStandard() throws Exception {
        given();
        doStandardSetup(USER_WHEEL_MEMBER1, ORG_WHEEL, ROLE_APPROVER_STANDARD_NEW);

        then("no relevant cases are visible");
        assertGetDeny(CaseType.class, CASE_REQUEST_1.oid); // irrelevant
        assertGetDeny(CaseType.class, CASE_REQUEST_1_ROLE_1.oid); // irrelevant
        assertGetDeny(CaseType.class, CASE_REQUEST_1_ROLE_2.oid); // irrelevant
        assertGetDeny(CaseType.class, CASE_REQUEST_2.oid); // candidates are not covered by this role
        assertGetDeny(CaseType.class, CASE_REQUEST_2_ROLE_3.oid); // candidates are not covered by this role
        assertSearchCases();

        and("no work items are visible");
        assertSearchWorkItems();
    }

    @Test
    public void test130WheelMemberStandardWithCandidates() throws Exception {
        given();
        doStandardSetup(USER_WHEEL_MEMBER1, ORG_WHEEL, ROLE_APPROVER_STANDARD_WITH_CANDIDATES);

        then("no relevant cases are visible");
        assertGetDeny(CaseType.class, CASE_REQUEST_1.oid); // irrelevant
        assertGetDeny(CaseType.class, CASE_REQUEST_1_ROLE_1.oid); // irrelevant
        assertGetDeny(CaseType.class, CASE_REQUEST_1_ROLE_2.oid); // irrelevant
        assertGetDeny(CaseType.class, CASE_REQUEST_2.oid); // currently not covered
        assertGetAllow(CaseType.class, CASE_REQUEST_2_ROLE_3.oid);
        assertSearchCases(CASE_REQUEST_2_ROLE_3.oid);

        and("no work items are visible");
        assertSearchWorkItems(WORK_ITEM_REQUEST_2_ROLE_3_WHEEL_MEMBER1);
    }

    @SafeVarargs
    private void doStandardSetup(TestObject<UserType> user, TestObject<RoleType>... roles) throws CommonException {
        loginAdministrator();
        setRoles(user, roles);
        login(user.getNameOrig());
    }

    @SafeVarargs
    private void setRoles(TestObject<UserType> user, TestObject<RoleType>... roles) throws CommonException {
        unassignAllRoles(user.oid);
        for (TestObject<RoleType> role : roles) {
            assign(user, role, ORG_DEFAULT, null, getTestTask(), getTestOperationResult());
        }
    }
}
