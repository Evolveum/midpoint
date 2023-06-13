/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieveCollection;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ORG_DEFAULT;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AccessCertificationCaseId;
import com.evolveum.midpoint.schema.util.AccessCertificationWorkItemId;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    private static final TestObject<RoleType> ROLE_SUPER_APPROVER_LEGACY = TestObject.file(TEST_DIR, "role-super-approver-legacy.xml", "89e19cff-183c-4890-be65-ad404aeac405");
    private static final TestObject<RoleType> ROLE_APPROVER_STANDARD_WITH_CANDIDATES = TestObject.file(TEST_DIR, "role-approver-standard-with-candidates.xml", "509ce515-35ff-45f6-bf49-0d5b81af515e");

    private static final TestObject<RoleType> ROLE_REVIEWER_COMMON_PARTS = TestObject.file(TEST_DIR, "role-reviewer-common-parts.xml", "26224a3a-3b78-4cc0-8c4c-368193eb575c");
    private static final TestObject<RoleType> ROLE_REVIEWER_STANDARD_NEW = TestObject.file(TEST_DIR, "role-reviewer-standard-new.xml", "fd4b11ca-8bf4-4ca2-8c4f-654f734de032");
    private static final TestObject<RoleType> ROLE_REVIEWER_STANDARD_LEGACY = TestObject.file(TEST_DIR, "role-reviewer-standard-legacy.xml", "56e5008f-d843-4bff-8c33-1b3d94bc2235");

    private static final TestObject<RoleType> ORG_WHEEL = TestObject.file(TEST_DIR, "org-wheel.xml", "2c76f1d8-34a9-4063-bffb-02bde8ee5569");

    private static final TestObject<RoleType> ROLE_1 = TestObject.file(TEST_DIR, "role-1.xml", "a023a613-2d48-4fc8-9958-c56fd9b2592a");
    private static final TestObject<RoleType> ROLE_2 = TestObject.file(TEST_DIR, "role-2.xml", "0e1899bc-88d4-49cd-8a88-a290928a7510");
    private static final TestObject<RoleType> ROLE_3 = TestObject.file(TEST_DIR, "role-3.xml", "f2ce1520-cf71-42c5-962f-394217aa531e");

    private static final TestObject<UserType> USER_1 = TestObject.file(TEST_DIR, "user-1.xml", "96dc9b60-6e02-4c30-a032-cb435d13a7f5");
    private static final TestObject<UserType> USER_MANAGER1 = TestObject.file(TEST_DIR, "user-manager1.xml", "28e36fe8-ab06-40e3-a97a-ee5beaa9d7d1");
    private static final TestObject<UserType> USER_MANAGER2 = TestObject.file(TEST_DIR, "user-manager2.xml", "c8579e49-8635-404b-b41d-453938253f56");
    private static final TestObject<UserType> USER_MANAGER3 = TestObject.file(TEST_DIR, "user-manager3.xml", "83cb3fa9-d7bd-447f-a2e8-339a0fc9eff3");
    private static final TestObject<UserType> USER_MANAGER4 = TestObject.file(TEST_DIR, "user-manager4.xml", "cefb4698-7e4b-4a9d-a265-0835c0ccef05");
    private static final TestObject<UserType> USER_WHEEL_MEMBER1 = TestObject.file(TEST_DIR, "user-wheel-member1.xml", "6db795fc-bc78-4286-bff3-77333f20a7c4");
    private static final TestObject<UserType> USER_DEPUTY1_1 = TestObject.file(TEST_DIR, "user-deputy1-1.xml", "d8fe1026-6c46-4fbe-986b-b2e486630b97");
    private static final TestObject<UserType> USER_DEPUTY1_2 = TestObject.file(TEST_DIR, "user-deputy1-2.xml", "a4519780-6f2e-47aa-a70e-49bfc531e7ee");
    private static final TestObject<UserType> USER_DEPUTY1_2_1 = TestObject.file(TEST_DIR, "user-deputy1-2-1.xml", "30613f66-9e4c-475b-bb78-cf1bd56cbaab");

    private static final TestObject<CaseType> CASE_REQUEST_1 = TestObject.file(TEST_DIR, "case-request-1.xml", "578e1d81-7865-4e05-9975-0d144fb6e488");
    private static final TestObject<CaseType> CASE_REQUEST_1_ROLE_1 = TestObject.file(TEST_DIR, "case-request-1-role-1.xml", "c73df3a2-18ca-49f9-80de-81a0d6f2d1a1");
    private static final TestObject<CaseType> CASE_REQUEST_1_ROLE_2 = TestObject.file(TEST_DIR, "case-request-1-role-2.xml", "d726fbcc-c00e-4bf8-aca3-3b8e92bd0903");

    private static final TestObject<CaseType> CASE_REQUEST_2 = TestObject.file(TEST_DIR, "case-request-2.xml", "d0a72b38-3d12-4c98-b35b-c2d5bde1106f");
    private static final TestObject<CaseType> CASE_REQUEST_2_ROLE_3 = TestObject.file(TEST_DIR, "case-request-2-role-3.xml", "5fece299-5463-4e33-b8ba-e299e8bf2b52");

    private static final TestObject<AccessCertificationCampaignType> CAMPAIGN_ASSIGNMENTS_1 = TestObject.file(TEST_DIR, "campaign-assignments-1.xml", "5ce93ee5-c832-4d60-8cd0-82c1ac8de1a4");

    private static final List<TestObject<CaseType>> ALL_CASES = List.of(
            CASE_REQUEST_1,
            CASE_REQUEST_1_ROLE_1,
            CASE_REQUEST_1_ROLE_2,
            CASE_REQUEST_2,
            CASE_REQUEST_2_ROLE_3);

    private static final WorkItemId WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1 = WorkItemId.of(CASE_REQUEST_1_ROLE_1.oid, 5L);
    private static final WorkItemId WORK_ITEM_REQUEST_1_ROLE_1_MANAGER2 = WorkItemId.of(CASE_REQUEST_1_ROLE_1.oid, 4L);
    private static final WorkItemId WORK_ITEM_REQUEST_1_ROLE_2_MANAGER3 = WorkItemId.of(CASE_REQUEST_1_ROLE_2.oid, 4L);
    private static final WorkItemId WORK_ITEM_REQUEST_2_ROLE_3_WHEEL_MEMBER1 = WorkItemId.of(CASE_REQUEST_2_ROLE_3.oid, 4L);

    private static final Set<WorkItemId> ALL_WORK_ITEMS = Set.of(
            WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1,
            WORK_ITEM_REQUEST_1_ROLE_1_MANAGER2,
            WORK_ITEM_REQUEST_1_ROLE_2_MANAGER3,
            WORK_ITEM_REQUEST_2_ROLE_3_WHEEL_MEMBER1
    );

    private static final AccessCertificationCaseId CERT_CASE_ROLE_1 = new AccessCertificationCaseId(CAMPAIGN_ASSIGNMENTS_1.oid, 2);
    private static final AccessCertificationCaseId CERT_CASE_ROLE_2 = new AccessCertificationCaseId(CAMPAIGN_ASSIGNMENTS_1.oid, 4);
    //private static final Set<AccessCertificationCaseId> ALL_CERT_CASES = Set.of(CERT_CASE_ROLE_1, CERT_CASE_ROLE_2);

    private static final AccessCertificationWorkItemId CERT_WORK_ITEM_ROLE_1_MANAGER1 = new AccessCertificationWorkItemId(CERT_CASE_ROLE_1, 1);
    private static final AccessCertificationWorkItemId CERT_WORK_ITEM_ROLE_1_MANAGER2 = new AccessCertificationWorkItemId(CERT_CASE_ROLE_1, 2);
    private static final AccessCertificationWorkItemId CERT_WORK_ITEM_ROLE_2_MANAGER3 = new AccessCertificationWorkItemId(CERT_CASE_ROLE_2, 1);
    private static final Set<AccessCertificationWorkItemId> ALL_CERT_WORK_ITEMS = Set.of(
            CERT_WORK_ITEM_ROLE_1_MANAGER1,
            CERT_WORK_ITEM_ROLE_1_MANAGER2,
            CERT_WORK_ITEM_ROLE_2_MANAGER3
    );

    /**
     * Structure of cases and their work items with assignees:
     *
     *    case-request-1
     *         |
     *         +--> case-request-1-role-1
     *         |       |
     *         |       +------> manager1 (pcv #5)
     *         |       \------> manager2 (pcv #4)
     *         |
     *         \--> case-request-1-role-2
     *                 \------> manager3 (pcv #4)
     *
     *    case-request-2
     *         |
     *         \--> case-request-2-role-3 (pcv #4)
     *                 \-----> candidate: wheel (org) ---> wheel-member1 (member)
     *
     *
     * Structure of certification cases and their work items with assignees:
     *
     *    case-role-1 (#2)
     *         |
     *         +------> manager1 (#1)
     *         \------> manager2 (#2)
     *
     *    case-role-2 (#4)
     *         \------> manager3 (#1)
     *
     *
     * Structure of deputies:
     *
     *    manager1
     *       |
     *       +-- (cases) ---> deputy1-1
     *       \-- (cert) ----> deputy1-2
     *                           |
     *                           \-- (cases) ---> deputy1-2-1
     *
     */

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                ROLE_APPROVER_COMMON_PARTS,
                ROLE_APPROVER_STANDARD_NEW,
                ROLE_APPROVER_STANDARD_LEGACY,
                ROLE_SUPER_APPROVER_LEGACY,
                ROLE_APPROVER_STANDARD_WITH_CANDIDATES,
                ROLE_REVIEWER_COMMON_PARTS,
                ROLE_REVIEWER_STANDARD_NEW,
                ROLE_REVIEWER_STANDARD_LEGACY,
                ORG_WHEEL,
                ROLE_1,
                ROLE_2,
                ROLE_3,
                USER_1,
                USER_MANAGER1,
                USER_MANAGER2,
                USER_MANAGER3,
                USER_MANAGER4,
                USER_WHEEL_MEMBER1,
                USER_DEPUTY1_1,
                USER_DEPUTY1_2,
                USER_DEPUTY1_2_1
        );

        initTestObjectsRaw(initResult, ALL_CASES.toArray(new TestObject[0]));
        initTestObjectsRaw(initResult, CAMPAIGN_ASSIGNMENTS_1);
    }

    @Test
    public void test100StandardApproverLegacyRole() throws Exception {
        given();
        doStandardSetup(USER_MANAGER1, ROLE_APPROVER_STANDARD_LEGACY);

        then("relevant cases are visible");
        // because all cases are visible for legacy
        //noinspection unchecked
        assertCasesVisible(ALL_CASES.toArray(new TestObject[0]));

        and("relevant work items are visible");
        assertWorkItemsVisible(WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1);

        and("relevant work items can be completed and delegated");
        assertWorkItemsCompletableAndDelegable(WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1);
    }

    @Test
    public void test110StandardApproverNewRole() throws Exception {
        given();
        doStandardSetup(USER_MANAGER1, ROLE_APPROVER_STANDARD_NEW);

        then("relevant cases are visible");
        // currently only "assigned to me" are visible, so request-level ones are not
        assertCasesVisible(CASE_REQUEST_1_ROLE_1);

        and("relevant work items are visible");
        assertWorkItemsVisible(WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1);

        and("relevant work items can be completed and delegated");
        assertWorkItemsCompletableAndDelegable(WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1);
    }

    @Test
    public void test120WheelMemberStandard() throws Exception {
        given();
        doStandardSetup(USER_WHEEL_MEMBER1, ORG_WHEEL, ROLE_APPROVER_STANDARD_NEW);

        then("no relevant cases are visible");
        assertCasesVisible(); // candidates are not covered by this role

        and("no work items are visible");
        assertWorkItemsVisible();

        and("no work items can be completed nor delegated");
        assertWorkItemsCompletableAndDelegable();
    }

    @Test
    public void test130WheelMemberStandardWithCandidates() throws Exception {
        given();
        doStandardSetup(USER_WHEEL_MEMBER1, ORG_WHEEL, ROLE_APPROVER_STANDARD_WITH_CANDIDATES);

        then("relevant cases are visible");
        assertCasesVisible(CASE_REQUEST_2_ROLE_3);

        and("relevant work items are visible");
        assertWorkItemsVisible(WORK_ITEM_REQUEST_2_ROLE_3_WHEEL_MEMBER1);

        and("relevant work items can be completed and delegated");
        assertWorkItemsCompletableAndDelegable(WORK_ITEM_REQUEST_2_ROLE_3_WHEEL_MEMBER1);
    }

    // TODO disallow completing/delegating work items claimed to someone else
    // TODO special "with candidates" approver that cannot immediately complete/delegate work items he has not claimed

    @Test
    public void test140SuperApproverLegacy() throws Exception {
        given();
        doStandardSetup(USER_MANAGER4, ROLE_SUPER_APPROVER_LEGACY);

        then("approval cases are visible (request-level ones are not)");
        assertCasesVisible(CASE_REQUEST_1_ROLE_1, CASE_REQUEST_1_ROLE_2, CASE_REQUEST_2_ROLE_3);

        and("all work items are visible");
        assertWorkItemsVisible(ALL_WORK_ITEMS.toArray(new WorkItemId[0]));

        and("all work items can be completed and delegated");
        assertWorkItemsCompletableAndDelegable(ALL_WORK_ITEMS.toArray(new WorkItemId[0]));
    }

    @Test
    public void test150StandardApproversAsDeputies() throws Exception {
        given();
        login(userAdministrator);
        setRoles(USER_MANAGER1); // roles of this user are not important, as they are not delegated

        when("deputy1-1 that obtains case management privileges is logged in");
        login(USER_DEPUTY1_1);

        then("relevant cases and work items are visible + relevant work items can be completed/delegated");
        assertCasesVisible(CASE_REQUEST_1_ROLE_1);
        assertWorkItemsVisible(WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1);
        assertWorkItemsCompletableAndDelegable(WORK_ITEM_REQUEST_1_ROLE_1_MANAGER1);

        when("deputy1-2 that does not obtain case management privileges is logged in");
        login(USER_DEPUTY1_2);

        then("no cases and work items are visible + no work items can be completed/delegated");
        assertCasesVisible();
        assertWorkItemsVisible();
        assertWorkItemsCompletableAndDelegable();

        when("deputy1-2-1 that does not obtain case management privileges is logged in");
        login(USER_DEPUTY1_2_1);

        then("no cases and work items are visible + no work items can be completed/delegated");
        assertCasesVisible();
        assertWorkItemsVisible();
        assertWorkItemsCompletableAndDelegable();
    }

    @Test
    public void test200StandardReviewerLegacyRole() throws Exception {
        if (isNativeRepository()) {
            throw new SkipException("FIXME skipped because of MID-8894");
        }

        given();
        doStandardSetup(USER_MANAGER1, ROLE_REVIEWER_STANDARD_LEGACY);

        then("relevant cases and work items are visible, completable and delegable");
        assertCertCasesVisible(CERT_CASE_ROLE_1);
        assertCertWorkItemsVisible(CERT_WORK_ITEM_ROLE_1_MANAGER1);
        assertCertWorkItemsCompletable(CERT_WORK_ITEM_ROLE_1_MANAGER1);
        assertCertWorkItemsDelegable();// legacy autz does not provide delegation
    }

    @Test
    public void test210StandardReviewerNewRole() throws Exception {
        if (isNativeRepository()) {
            throw new SkipException("FIXME skipped because of MID-8894");
        }

        given();
        doStandardSetup(USER_MANAGER1, ROLE_REVIEWER_STANDARD_NEW);

        then("relevant cases and work items are visible, completable and delegable");
        assertCertCasesVisible(CERT_CASE_ROLE_1);
        assertCertWorkItemsVisible(CERT_WORK_ITEM_ROLE_1_MANAGER1);
        assertCertWorkItemsCompletableAndDelegable(CERT_WORK_ITEM_ROLE_1_MANAGER1);
    }

    @SafeVarargs
    private void assertCasesVisible(TestObject<CaseType>... cases) throws Exception {
        var casesList = List.of(cases);
        for (TestObject<CaseType> aCase : ALL_CASES) {
            if (casesList.contains(aCase)) {
                assertGetAllow(CaseType.class, aCase.oid);
            } else {
                assertGetDeny(CaseType.class, aCase.oid);
            }
        }
        assertSearchCases(
                casesList.stream()
                        .map(o -> o.oid)
                        .toArray(String[]::new));
    }

    private void assertWorkItemsVisible(WorkItemId... ids) throws CommonException {
        // When directly searching for them
        assertSearchWorkItems(ids);

        // From cases
        var real = WorkItemId.of(getAllWorkItemsFromCases());
        assertThat(real).as("visible work items").containsExactlyInAnyOrder(ids);
    }

    private Collection<CaseWorkItemType> getAllWorkItemsFromCases() throws CommonException {
        return modelService
                .searchObjects(CaseType.class, null, null, getTestTask(), getTestOperationResult()).stream()
                .flatMap(aCase -> aCase.asObjectable().getWorkItem().stream())
                .collect(Collectors.toList());
    }

    private Collection<AccessCertificationCaseType> getAllCertCasesFromCampaigns() throws CommonException {
        return modelService
                .searchObjects(
                        AccessCertificationCampaignType.class,
                        null,
                        createRetrieveCollection(),
                        getTestTask(),
                        getTestOperationResult())
                .stream()
                .flatMap(campaign -> campaign.asObjectable().getCase().stream())
                .collect(Collectors.toList());
    }

    private Collection<AccessCertificationWorkItemType> getAllCertWorkItemsFromCases() throws CommonException {
        return modelService
                .searchContainers(
                        AccessCertificationCaseType.class,
                        null,
                        null,
                        getTestTask(),
                        getTestOperationResult())
                .stream()
                .flatMap(aCase -> aCase.getWorkItem().stream())
                .collect(Collectors.toList());
    }

    private Collection<AccessCertificationWorkItemType> getAllCertWorkItemsFromCampaigns() throws CommonException {
        return getAllCertCasesFromCampaigns().stream()
                .flatMap(aCase -> aCase.getWorkItem().stream())
                .collect(Collectors.toList());
    }

    private void assertCertCasesVisible(AccessCertificationCaseId... ids) throws Exception {
        // When directly searching for them
        assertCertCasesSearch(ids);

        // From campaigns
        var fromCampaigns = AccessCertificationCaseId.of(getAllCertCasesFromCampaigns());
        assertThat(fromCampaigns).as("visible cert cases from campaigns").containsExactlyInAnyOrder(ids);
    }

    private void assertCertWorkItemsVisible(AccessCertificationWorkItemId... ids) throws Exception {
        // When directly searching for them
        assertCertWorkItemsSearch(ids);

        // From cases
        var fromCases = AccessCertificationWorkItemId.of(getAllCertWorkItemsFromCases());
        assertThat(fromCases).as("visible work items from cert cases").containsExactlyInAnyOrder(ids);

        // From campaigns
        var fromCampaigns = AccessCertificationWorkItemId.of(getAllCertWorkItemsFromCampaigns());
        assertThat(fromCampaigns).as("visible work items from cert campaigns").containsExactlyInAnyOrder(ids);
    }

    private void assertWorkItemsCompletableAndDelegable(WorkItemId... ids) throws CommonException {
        var idList = List.of(ids);
        for (var id : ALL_WORK_ITEMS) {
            if (idList.contains(id)) {
                assertCompleteAndDelegateAllow(id);
            } else {
                assertCompleteAndDelegateDeny(id);
            }
        }
    }

    private void assertCertWorkItemsCompletableAndDelegable(AccessCertificationWorkItemId... ids) throws CommonException {
        var idList = List.of(ids);
        for (var id : ALL_CERT_WORK_ITEMS) {
            if (idList.contains(id)) {
                assertCompleteAndDelegateAllow(id);
            } else {
                assertCompleteAndDelegateDeny(id);
            }
        }
    }

    private void assertCertWorkItemsCompletable(AccessCertificationWorkItemId... ids) throws CommonException {
        var idList = List.of(ids);
        for (var id : ALL_CERT_WORK_ITEMS) {
            if (idList.contains(id)) {
                assertCompleteAllow(id);
            } else {
                assertCompleteDeny(id);
            }
        }
    }

    private void assertCertWorkItemsDelegable(AccessCertificationWorkItemId... ids) throws CommonException {
        var idList = List.of(ids);
        for (var id : ALL_CERT_WORK_ITEMS) {
            if (idList.contains(id)) {
                assertDelegateAllow(id);
            } else {
                assertDelegateDeny(id);
            }
        }
    }

    @SafeVarargs
    private void doStandardSetup(TestObject<UserType> user, TestObject<RoleType>... roles) throws CommonException {
        loginAdministrator();
        setRoles(user, roles);
        login(user);
    }

    @SafeVarargs
    private void setRoles(TestObject<UserType> user, TestObject<RoleType>... roles) throws CommonException {
        unassignAllRoles(user.oid);
        for (TestObject<RoleType> role : roles) {
            assign(user, role, ORG_DEFAULT, null, getTestTask(), getTestOperationResult());
        }
    }
}
