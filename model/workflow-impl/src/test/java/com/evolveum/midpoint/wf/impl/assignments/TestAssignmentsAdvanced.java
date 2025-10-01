/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.assignments;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.AUTO_COMPLETION_CONDITION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType.NO_ASSIGNEES_FOUND;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.PROCESS;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.test.TestDir;
import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.wf.impl.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.CheckedRunnable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A special test dealing with assigning roles that have different metarole-induced approval policies.
 *
 * . `Role21` - uses default approval (org:approver)
 * . `Role22` - uses metarole 1 'default' induced approval (org:special-approver)
 * . `Role23` - uses both metarole 'default' and 'security' induced approval (org:special-approver and org:security-approver)
 * . `RoleExtensionPropertyModApproval` - requires an approval when extension property is to be modified
 *
 * TODO clean up this test, using "new" asserters
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentsAdvanced extends AbstractWfTestPolicy {

    private static final TestDir TEST_DIR = TestDir.of("src/test/resources/assignments-advanced");

    private static final TestObject<RoleType> METAROLE_DEFAULT = TEST_DIR.object("metarole-default.xml", "00000001-d34d-b33f-f00d-b00000000001");
    private static final TestObject<RoleType> METAROLE_SECURITY = TEST_DIR.object("metarole-security.xml", "00000001-d34d-b33f-f00d-f00000000002");
    private static final TestObject<RoleType> METAROLE_ADMINISTRATOR_APPROVAL = TEST_DIR.object("metarole-administrator-approval.xml", "715dc3b6-eb2c-4cc8-b2bf-0f7968bbc52a");
    private static final TestObject<RoleType> METAROLE_ADMINISTRATOR_APPROVAL_IDEMPOTENT = TEST_DIR.object("metarole-administrator-approval-idempotent.xml", "00586339-50f0-4aa8-aa0a-d600810f6577");

    private static final TestObject<RoleType> ROLE_ROLE21 = TEST_DIR.object("role-role21-standard.xml", "00000001-d34d-b33f-f00d-000000000021");
    private static final TestObject<RoleType> ROLE_ROLE22 = TEST_DIR.object("role-role22-special.xml", "00000001-d34d-b33f-f00d-000000000022");
    private static final TestObject<RoleType> ROLE_ROLE23 = TEST_DIR.object("role-role23-special-and-security.xml", "00000001-d34d-b33f-f00d-000000000023");
    private static final TestObject<RoleType> ROLE_ROLE24 = TEST_DIR.object("role-role24-approval-and-enforce.xml", "00000001-d34d-b33f-f00d-000000000024");
    private static final TestObject<RoleType> ROLE_ROLE25 = TEST_DIR.object("role-role25-very-complex-approval.xml", "00000001-d34d-b33f-f00d-000000000025");
    private static final TestObject<RoleType> ROLE_ROLE26 = TEST_DIR.object("role-role26-no-approvers.xml", "00000001-d34d-b33f-f00d-000000000026");
    private static final TestObject<RoleType> ROLE_ROLE27 = TEST_DIR.object("role-role27-modifications-and.xml", "00000001-d34d-b33f-f00d-000000000027");
    private static final TestObject<RoleType> ROLE_ROLE28 = TEST_DIR.object("role-role28-modifications-or.xml", "00000001-d34d-b33f-f00d-000000000028");
    private static final TestObject<RoleType> ROLE_ROLE29 = TEST_DIR.object("role-role29-modifications-no-items.xml", "00000001-d34d-b33f-f00d-000000000029");
    private static final TestObject<RoleType> ROLE_EXTENSION_PROPERTY_MOD_APPROVAL = TEST_DIR.object("role-extension-property-mod-approval.xml", "60459cec-7bdb-4872-99ae-65063c9c2e82");

    private static final TestObject<RoleType> ROLE_SKIPPED = TEST_DIR.object("role-skipped.xml", "66134203-f023-4986-bb5c-a350941909eb");
    private static final TestObject<RoleType> ROLE_AUTOCOMPLETE_FILE = TEST_DIR.object("role-autocomplete.xml", "9f4a3a87-2b9e-4e4c-9fcb-7bb2d1210a5e");
    private static final TestObject<RoleType> ROLE_APPROVE_UNASSIGN = TEST_DIR.object("role-approve-unassign.xml", "3746aa73-ae91-4326-8493-f5ac5b22f3b6");

    private static final TestObject<RoleType> ROLE_IDEMPOTENT = TEST_DIR.object("role-idempotent.xml", "e2f2d977-887b-4ea1-99d8-a6a030a1a6c0");
    private static final TestObject<RoleType> ROLE_WITH_IDEMPOTENT_METAROLE = TEST_DIR.object("role-with-idempotent-metarole.xml", "34855a80-3899-4ecf-bdb3-9fc008c4ff70");

    private static final TestObject<OrgType> ORG_LEADS2122 = TEST_DIR.object("org-leads2122.xml", "00000001-d34d-b33f-f00d-000000002122");

    private static final TestObject<RoleType> ROLE_BEING_ENABLED = TEST_DIR.object("role-being-enabled.xml", "4fcf187a-09b7-4d32-b998-9cd978195a82");
    private static final TestObject<UserType> USER_HOLDER_OF_ROLE_BEING_ENABLED = TEST_DIR.object("user-holder-of-role-being-enabled.xml", "c6f7ddbe-a596-4a53-8acf-e03282f3da33");
    private static final TestObject<UserType> USER_APPROVER_OF_ROLE_BEING_ENABLED_AND_DISABLED = TEST_DIR.object("user-approver-of-role-being-enabled-and-disabled.xml", "39764050-fbd8-4746-a8a6-cb9141ae31ae");

    private static final TestObject<RoleType> ROLE_BEING_DISABLED = TEST_DIR.object("role-being-disabled.xml", "4835efdc-6fee-438b-bb09-fd428a200dbb");
    private static final TestObject<UserType> USER_HOLDER_OF_ROLE_BEING_DISABLED = TEST_DIR.object("user-holder-of-role-being-disabled.xml", "586fc857-71d8-4906-a61d-46ba7941be00");

    private static final TestObject<RoleType> ROLE_BEING_DISABLED_WITH_APPROVAL = TEST_DIR.object("role-being-disabled-with-approval.xml", "04c8d15f-da0a-4553-9b43-af835a7c8b82");
    private static final TestObject<UserType> USER_HOLDER_OF_ROLE_BEING_DISABLED_WITH_APPROVAL = TEST_DIR.object("user-holder-of-role-being-disabled-with-approval.xml", "411258e6-191b-4957-95d3-86b6e25024d3");

    private static final TestObject<UserType> USER_LEAD21 = TEST_DIR.object("user-lead21.xml", "00000001-d34d-b33f-f00d-f00000000021");
    private static final TestObject<UserType> USER_LEAD22 = TEST_DIR.object("user-lead22.xml", "00000001-d34d-b33f-f00d-f00000000022");
    private static final TestObject<UserType> USER_LEAD23 = TEST_DIR.object("user-lead23.xml", "00000001-d34d-b33f-f00d-f00000000023");
    private static final TestObject<UserType> USER_LEAD24 = TEST_DIR.object("user-lead24.xml", "00000001-d34d-b33f-f00d-f00000000024");
    private static final TestObject<UserType> USER_SECURITY_APPROVER = TEST_DIR.object("user-security-approver.xml", "00000001-d34d-b33f-f00d-5eca99000001");
    private static final TestObject<UserType> USER_SECURITY_APPROVER_DEPUTY = TEST_DIR.object("user-security-approver-deputy.xml", "00000001-d34d-b33f-f00d-5eca990000D1");
    private static final TestObject<UserType> USER_SECURITY_APPROVER_DEPUTY_LIMITED = TEST_DIR.object("user-security-approver-deputy-limited.xml", "00000001-d34d-b33f-f00d-5eca990000d2");

    private static final TestObject<RoleType> METAROLE_SELECTIVE_APPROVAL = TEST_DIR.object("metarole-selective-approval.xml", "f3f31769-4b1e-401c-8db4-22914cee1550");
    private static final TestObject<RoleType> ROLE_SELECTIVE_A = TEST_DIR.object("role-selective-a.xml", "e67e973e-cc54-47da-9369-8b483b677aaa");
    private static final TestObject<RoleType> ROLE_SELECTIVE_B = TEST_DIR.object("role-selective-b.xml", "81f4f09c-263b-45fe-b2a7-e09f02de6348");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjectsRaw(initResult,
                METAROLE_DEFAULT,
                METAROLE_SECURITY,
                METAROLE_ADMINISTRATOR_APPROVAL,
                METAROLE_ADMINISTRATOR_APPROVAL_IDEMPOTENT);

        initTestObjectsRaw(initResult,
                ROLE_ROLE21, ROLE_ROLE22, ROLE_ROLE23, ROLE_ROLE24, ROLE_ROLE25,
                ROLE_ROLE26, ROLE_ROLE27, ROLE_ROLE28, ROLE_ROLE29,
                ROLE_IDEMPOTENT, ROLE_WITH_IDEMPOTENT_METAROLE, ROLE_SKIPPED, ROLE_APPROVE_UNASSIGN,
                ORG_LEADS2122);

        initTestObjectsRaw(initResult,
                USER_LEAD21, USER_LEAD22, USER_LEAD23, USER_LEAD24,
                USER_SECURITY_APPROVER, USER_SECURITY_APPROVER_DEPUTY, USER_SECURITY_APPROVER_DEPUTY_LIMITED);

        recomputeUser(USER_LEAD21.oid, initTask, initResult);
        recomputeUser(USER_LEAD22.oid, initTask, initResult);
        recomputeUser(USER_LEAD23.oid, initTask, initResult);
        recomputeUser(USER_LEAD24.oid, initTask, initResult);
        recomputeUser(USER_SECURITY_APPROVER.oid, initTask, initResult);
        recomputeUser(USER_SECURITY_APPROVER_DEPUTY.oid, initTask, initResult);
        recomputeUser(USER_SECURITY_APPROVER_DEPUTY_LIMITED.oid, initTask, initResult);

        repoAdd(ROLE_BEING_ENABLED, initResult);
        addObject(USER_HOLDER_OF_ROLE_BEING_ENABLED, initTask, initResult);
        repoAdd(ROLE_BEING_DISABLED, initResult);
        addObject(USER_HOLDER_OF_ROLE_BEING_DISABLED, initTask, initResult);
        repoAdd(ROLE_AUTOCOMPLETE_FILE, initResult);
        repoAdd(ROLE_BEING_DISABLED_WITH_APPROVAL, initResult);
        addObject(USER_HOLDER_OF_ROLE_BEING_DISABLED_WITH_APPROVAL, initTask, initResult);

        // import this one last to avoid approvals at this stage
        addObject(USER_APPROVER_OF_ROLE_BEING_ENABLED_AND_DISABLED, initTask, initResult);

        repoAdd(ROLE_EXTENSION_PROPERTY_MOD_APPROVAL, initResult);

        initTestObjectsRaw(initResult,
                METAROLE_SELECTIVE_APPROVAL,
                ROLE_SELECTIVE_A, ROLE_SELECTIVE_B);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_JSON);
    }

    @Test
    public void test102AddRoles123AssignmentYYYYDeputy() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(false, true, true, true, true, true);
    }

    @Test
    public void test105AddRoles123AssignmentYYYYImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(true, true, true, true, true, false);
    }

    @Test
    public void test110AddRoles123AssignmentNNNN() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(true, false, false, false, false, false);
    }

    @Test
    public void test115AddRoles123AssignmentNNNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(true, false, false, false, false, false);
    }

    @Test
    public void test120AddRoles123AssignmentYNNN() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(true, true, false, false, false, false);
    }

    @Test
    public void test125AddRoles123AssignmentYNNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(true, true, false, false, false, false);
    }

    @Test
    public void test130AddRoles123AssignmentYYYN() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(false, true, true, true, false, false);
    }

    @Test
    public void test132AddRoles123AssignmentYYYNDeputy() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(false, true, true, true, false, true);
    }

    @Test
    public void test135AddRoles123AssignmentYYYNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(true, true, true, true, false, false);
    }

    /**
     * Attempt to assign roles 21-23 along with changing description.
     */
    @Test
    public void test200AddRoles123AssignmentYYYY() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRoles123ToJack(false, true, true, true, true, false);
    }

    @Test
    public void test210DeleteRoles123AssignmentN() throws Exception {
        login(userAdministrator);

        executeUnassignRoles123ToJack(false, false, false, true);
    }

    @Test
    public void test212DeleteRoles123AssignmentNById() throws Exception {
        login(userAdministrator);

        executeUnassignRoles123ToJack(false, false, true, false);
    }

    @Test
    public void test218DeleteRoles123AssignmentY() throws Exception {
        login(userAdministrator);

        executeUnassignRoles123ToJack(false, true, false, false);
    }

    @Test
    public void test220AddRoles123AssignmentYYYY() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        executeAssignRoles123ToJack(false, true, true, true, true, false);
    }

    @Test
    public void test230DeleteRoles123AssignmentYById() throws Exception {
        login(userAdministrator);

        executeUnassignRoles123ToJack(false, true, true, true);
    }

    /**
     * MID-3836
     */
    @Test
    public void test300ApprovalAndEnforce() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        try {
            assignRole(USER_JACK.oid, ROLE_ROLE24.oid, task, result);
        } catch (PolicyViolationException e) {
            // ok
            System.out.println("Got expected exception: " + e);
        }
        List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, ObjectQueryUtil.openItemsQuery(), null, task, result);
        display("current work items", currentWorkItems);
        assertEquals("Wrong # of current work items", 0, currentWorkItems.size());
    }

    // preview-related tests

    @Test
    public void test400AddRoles123AssignmentPreview() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        previewAssignRolesToJack(false, false);
    }

    @Test
    public void test410AddRoles1234AssignmentPreview() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        previewAssignRolesToJack(false, true);
    }

    @Test
    public void test420AddRoles123AssignmentPreviewImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid, true);
        previewAssignRolesToJack(true, false);
    }

    /**
     * MID-4121
     */
    public static Exception exception;      // see system-configuration.xml

    @Test
    public void test500NoApprovers() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assignRole(USER_JACK.oid, ROLE_ROLE26.oid, task, result);
        String ref = result.findAsynchronousOperationReference(); // TODO use recompute + getAsync... when fixed
        assertNotNull("No asynchronous operation reference", ref);
        String caseOid = OperationResult.referenceToCaseOid(ref);

        List<CaseWorkItemType> currentWorkItems = modelService.searchContainers(CaseWorkItemType.class, ObjectQueryUtil.openItemsQuery(), null, task, result);
        display("current work items", currentWorkItems);
        assertEquals("Wrong # of current work items", 0, currentWorkItems.size());

        assertNotNull("Missing task OID", caseOid);
        waitForCaseClose(getCase(caseOid));

        if (exception != null) {
            System.err.println("Got unexpected exception");
            exception.printStackTrace();
            fail("Got unexpected exception: " + exception);
        }
    }

    // "assignment modification" (no specific items)
    @Test
    public void test600AssignRole29() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        assignRole(USER_JACK.oid, ROLE_ROLE29.oid, task, result);           // should proceed without approvals

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        assertAssignedRoles(jack, ROLE_ROLE29.oid);
    }

    @Test
    public void test610ModifyAssignmentOfRole29() throws Exception {
        login(userAdministrator);

        // WHEN
        when();
        PrismObject<UserType> jackBefore = getUser(USER_JACK.oid);
        AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, ROLE_ROLE29.oid);
        ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_DESCRIPTION)
                .replace("new description")
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("new full name"))
                .asObjectDelta(USER_JACK.oid);

        // +THEN
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jackBefore;
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() throws Exception {
                return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return singletonList(deltaToApprove);
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return delta0;
            }

            @Override
            protected String getObjectOid() {
                return USER_JACK.oid;
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return singletonList(new ExpectedTask(ROLE_ROLE29.oid, "Modifying assignment of role \"Role29\" on user \"new full name (jack)\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                ExpectedTask etask = getExpectedTasks().get(0);
                return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, ROLE_ROLE29.oid, etask));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
                // todo
                // e.g. check metadata
                PrismObject<UserType> jack = getUser(USER_JACK.oid);
                if (number == 0) {
                    assertEquals("wrong new full name", yes ? "new full name" : "Jack Sparrow", jack.asObjectable().getFullName().getOrig());
                } else {
                    AssignmentType assignment1 = findAssignmentByTargetRequired(jack, ROLE_ROLE29.oid);
                    assertEquals("wrong new assignment description", yes ? "new description" : null, assignment1.getDescription());
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return null;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment1"));
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
                    OperationResult result) {
                // todo
            }

        }, 1, false);

        // THEN
        then();
        PrismObject<UserType> jackAfter = getUser(USER_JACK.oid);
        display("jack after", jackAfter);
        assertAssignedRoles(jackAfter, ROLE_ROLE29.oid);
        assertAssignmentMetadata(jackAfter, ROLE_ROLE29.oid, emptySet(), emptySet(), singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment1"));
    }

    private void assertAssignmentMetadata(
            PrismObject<? extends FocusType> object, String targetOid, Set<String> createApproverOids,
            Set<String> createApprovalComments, Set<String> modifyApproverOids, Set<String> modifyApprovalComments) {
        AssignmentType assignment = findAssignmentByTargetRequired(object, targetOid);
        PrismAsserts.assertReferenceOids(
                "Wrong create approvers", createApproverOids, ValueMetadataTypeUtil.getCreateApproverRefs(assignment));
        PrismAsserts.assertEqualsCollectionUnordered(
                "Wrong create comments", createApprovalComments, ValueMetadataTypeUtil.getCreateApprovalComments(assignment));
        PrismAsserts.assertReferenceOids(
                "Wrong modify approvers", modifyApproverOids, ValueMetadataTypeUtil.getModifyApproverRefs(assignment));
        PrismAsserts.assertEqualsCollectionUnordered(
                "Wrong modify comments", modifyApprovalComments, ValueMetadataTypeUtil.getModifyApprovalComments(assignment));
    }

    @Test
    public void test620ModifyAssignmentOfRole29Immediate() throws Exception {
        login(userAdministrator);

        // WHEN
        when();
        PrismObject<UserType> jackBefore = getUser(USER_JACK.oid);
        AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, ROLE_ROLE29.oid);
        ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_DESCRIPTION)
                .replace("new description 2")
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("new full name 2"))
                .asObjectDelta(USER_JACK.oid);

        // +THEN
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jackBefore;
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() throws Exception {
                return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return singletonList(deltaToApprove);
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return delta0;
            }

            @Override
            protected String getObjectOid() {
                return USER_JACK.oid;
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return singletonList(new ExpectedTask(ROLE_ROLE29.oid, "Modifying assignment of role \"Role29\" on user \"Jack Sparrow (jack)\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                ExpectedTask etask = getExpectedTasks().get(0);
                return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, ROLE_ROLE29.oid, etask));
            }

            @SuppressWarnings("Duplicates")
            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
                // todo
                // e.g. check metadata
                PrismObject<UserType> jack = getUser(USER_JACK.oid);
                if (number == 0) {
                    assertEquals("wrong new full name", yes ? "new full name 2" : "new full name", jack.asObjectable().getFullName().getOrig());
                } else {
                    AssignmentType assignment1 = findAssignmentByTargetRequired(jack, ROLE_ROLE29.oid);
                    assertEquals("wrong new assignment description", yes ? "new description 2" : "new description", assignment1.getDescription());
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return null;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment2"));
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
                    OperationResult result) {
                // todo
            }

        }, 1, true);

        // THEN
        then();
        PrismObject<UserType> jackAfter = getUser(USER_JACK.oid);
        display("jack after", jackAfter);
        assertAssignedRoles(jackAfter, ROLE_ROLE29.oid);
        assertAssignmentMetadata(jackAfter, ROLE_ROLE29.oid, emptySet(), emptySet(), singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment2"));
    }

    @Test
    public void test630UnassignRole29() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        unassignRoleByAssignmentValue(getUser(USER_JACK.oid), ROLE_ROLE29.oid, task, result);           // should proceed without approvals

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        assertNotAssignedRole(jack, ROLE_ROLE29.oid);
    }

    // assignment modification ("or" of 2 items)
    @Test
    public void test700AssignRole28() throws Exception {
        login(userAdministrator);

        // WHEN/THEN
        ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(ObjectTypeUtil.createAssignmentTo(ROLE_ROLE28.oid, ObjectTypes.ROLE)
                        .description("description"))
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("new full name 3"))
                .asObjectDelta(USER_JACK.oid);

        PrismObject<UserType> jackBefore = getUser(USER_JACK.oid);

        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jackBefore;
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() throws Exception {
                return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return singletonList(deltaToApprove);
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return delta0;
            }

            @Override
            protected String getObjectOid() {
                return USER_JACK.oid;
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return singletonList(new ExpectedTask(ROLE_ROLE28.oid,
                        "Assigning role \"Role28\" to user \"new full name 3 (jack)\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                ExpectedTask etask = getExpectedTasks().get(0);
                return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, ROLE_ROLE28.oid, etask));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
                // todo
                // e.g. check metadata
                PrismObject<UserType> jack = getUser(USER_JACK.oid);
                if (number == 0) {
                    assertEquals("wrong new full name", yes ? "new full name 3" : "new full name 2", jack.asObjectable().getFullName().getOrig());
                } else {
                    if (yes) {
                        assertAssignedRole(jack, ROLE_ROLE28.oid);
                    } else {
                        assertNotAssignedRole(jack, ROLE_ROLE28.oid);
                    }
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return null;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment3"));
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
                    OperationResult result) {
                // todo
            }

        }, 1, false);

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        assertAssignedRoles(jack, ROLE_ROLE28.oid);
        assertAssignmentMetadata(jack, ROLE_ROLE28.oid, singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment3"), emptySet(), emptySet());
    }

    @Test
    public void test710ModifyAssignmentOfRole28() throws Exception {
        login(userAdministrator);

        // WHEN
        when();
        PrismObject<UserType> jackBefore = getUser(USER_JACK.oid);
        AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, ROLE_ROLE28.oid);
        ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_DESCRIPTION)
                .replace("new description")
                .item(UserType.F_ASSIGNMENT, assignment.getId(), AssignmentType.F_LIFECYCLE_STATE)      // this will be part of the delta to approve
                .replace("active")
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("new full name 4"))
                .asObjectDelta(USER_JACK.oid);

        // +THEN
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jackBefore;
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() throws Exception {
                return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return singletonList(deltaToApprove);
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return delta0;
            }

            @Override
            protected String getObjectOid() {
                return USER_JACK.oid;
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return singletonList(new ExpectedTask(ROLE_ROLE28.oid, "Modifying assignment of role \"Role28\" on user \"new full name 4 (jack)\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                ExpectedTask etask = getExpectedTasks().get(0);
                return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, ROLE_ROLE28.oid, etask));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
                // todo
                // e.g. check metadata
                PrismObject<UserType> jack = getUser(USER_JACK.oid);
                if (number == 0) {
                    assertEquals("wrong new full name", yes ? "new full name 4" : "new full name 3", jack.asObjectable().getFullName().getOrig());
                } else {
                    AssignmentType assignment1 = findAssignmentByTargetRequired(jack, ROLE_ROLE28.oid);
                    assertEquals("wrong new assignment description", yes ? "new description" : "description", assignment1.getDescription());
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return null;
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                return singletonList(new ApprovalInstruction(null, true, USER_ADMINISTRATOR_OID, "comment4"));
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
                    OperationResult result) {
                // todo
            }

        }, 1, false);

        // THEN
        then();
        PrismObject<UserType> jackAfter = getUser(USER_JACK.oid);
        display("jack after", jackAfter);
        assertAssignedRoles(jackAfter, ROLE_ROLE28.oid);
        assertAssignmentMetadata(jackAfter, ROLE_ROLE28.oid, singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment3"), singleton(USER_ADMINISTRATOR_OID), singleton("administrator :: comment4"));
    }

    @Test
    public void test720UnassignRole28() throws Exception {
        login(userAdministrator);

        // WHEN
        when();
        PrismObject<UserType> jackBefore = getUser(USER_JACK.oid);
        AssignmentType assignment = findAssignmentByTargetRequired(jackBefore, ROLE_ROLE28.oid);
        ObjectDelta<UserType> deltaToApprove = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType().id(assignment.getId()))        // id-only, to test the constraint
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> delta0 = prismContext.deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME)
                .replace(PolyString.fromOrig("new full name 5"))
                .asObjectDelta(USER_JACK.oid);

        // +THEN
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jackBefore;
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() throws Exception {
                return ObjectDeltaCollectionsUtil.summarize(deltaToApprove, delta0);
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return singletonList(deltaToApprove);
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return delta0;
            }

            @Override
            protected String getObjectOid() {
                return USER_JACK.oid;
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return singletonList(new ExpectedTask(ROLE_ROLE28.oid, "Unassigning role \"Role28\" from user \"new full name 5 (jack)\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                ExpectedTask etask = getExpectedTasks().get(0);
                return singletonList(new ExpectedWorkItem(USER_ADMINISTRATOR_OID, ROLE_ROLE28.oid, etask));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                System.out.println("assertDeltaExecuted for number = " + number + ", yes = " + yes);
                // todo
                // e.g. check metadata
                PrismObject<UserType> jack = getUser(USER_JACK.oid);
                if (number == 0) {
                    assertEquals("wrong new full name", yes ? "new full name 5" : "new full name 4", jack.asObjectable().getFullName().getOrig());
                } else {
                    if (yes) {
                        assertNotAssignedRole(jack, ROLE_ROLE28.oid);
                    } else {
                        assertAssignedRole(jack, ROLE_ROLE28.oid);
                    }
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return true;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase, List<CaseType> subcases, List<CaseWorkItemType> workItems,
                    OperationResult result) {
                // todo
            }

        }, 1, false);

        // THEN
        then();
        PrismObject<UserType> jackAfter = getUser(USER_JACK.oid);
        display("jack after", jackAfter);
        assertNotAssignedRole(jackAfter, ROLE_ROLE28.oid);
    }

    @Test
    public void test800AssignRole27() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(ObjectTypeUtil.createAssignmentTo(ROLE_ROLE27.oid, ObjectTypes.ROLE)
                        .description("description"))
                .asObjectDelta(USER_JACK.oid);
        executeChanges(delta, null, task, result); // should proceed without approvals (only 1 of the items is present)

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        assertAssignedRoles(jack, ROLE_ROLE27.oid);
    }

    @Test
    public void test810ModifyAssignmentOfRole27() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        PrismObject<UserType> jackBefore = getUser(USER_JACK.oid);
        AssignmentType assignmentBefore = findAssignmentByTargetRequired(jackBefore, ROLE_ROLE27.oid);
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignmentBefore.getId(), AssignmentType.F_DESCRIPTION)
                .replace("new description")
                .asObjectDelta(USER_JACK.oid);

        executeChanges(delta, null, task, result); // should proceed without approvals (only 1 of the items is present)

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        AssignmentType assignment = findAssignmentByTargetRequired(jack, ROLE_ROLE27.oid);
        assertEquals("Wrong description", "new description", assignment.getDescription());
    }

    @Test
    public void test820UnassignRole27() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        unassignRoleByAssignmentValue(getUser(USER_JACK.oid), ROLE_ROLE27.oid, task, result);           // should proceed without approvals

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        assertNotAssignedRole(jack, ROLE_ROLE27.oid);
    }

    /**
     * An assignment is enabled by changing validTo property from 1900 to null.
     * Such a change should *not* trigger any approvals (under the default rules).
     *
     * MID-7317
     */
    @Test
    public void test830EnableAssignment() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        UserType userBefore = getUserFromRepo(USER_HOLDER_OF_ROLE_BEING_ENABLED.oid).asObjectable();

        when();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT,
                        userBefore.getAssignment().get(0).getId(),
                        AssignmentType.F_ACTIVATION,
                        ActivationType.F_VALID_TO)
                .replace()
                .asObjectDelta(USER_HOLDER_OF_ROLE_BEING_ENABLED.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        List<PrismObject<CaseType>> cases =
                getCasesForObject(USER_HOLDER_OF_ROLE_BEING_ENABLED.oid, UserType.COMPLEX_TYPE, null, task, result);
        assertEquals("Wrong # of cases", 0, cases.size());

        assertUser(USER_HOLDER_OF_ROLE_BEING_ENABLED.oid, "after")
                .assignments()
                .single()
                .activation()
                .assertValidTo((XMLGregorianCalendar) null);
    }

    /**
     * An assignment is disabled by setting validTo property to an old value.
     * Such a change should *not* trigger any approvals under "assignment deletion" policy rule.
     *
     * MID-7317
     */
    @Test
    public void test840DisableAssignment() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        UserType userBefore = getUserFromRepo(USER_HOLDER_OF_ROLE_BEING_DISABLED.oid).asObjectable();

        when();
        XMLGregorianCalendar newValidTo = XmlTypeConverter.fromNow("-P1D");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT,
                        userBefore.getAssignment().get(0).getId(),
                        AssignmentType.F_ACTIVATION,
                        ActivationType.F_VALID_TO)
                .replace(newValidTo)
                .asObjectDelta(USER_HOLDER_OF_ROLE_BEING_DISABLED.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        List<PrismObject<CaseType>> cases =
                getCasesForObject(USER_HOLDER_OF_ROLE_BEING_DISABLED.oid, UserType.COMPLEX_TYPE, null, task, result);
        assertEquals("Wrong # of cases", 0, cases.size());

        assertUser(USER_HOLDER_OF_ROLE_BEING_DISABLED.oid, "after")
                .assignments()
                .single()
                .activation()
                .assertValidTo(newValidTo)
                .assertEffectiveStatus(ActivationStatusType.DISABLED);
    }

    /**
     * An assignment is disabled by setting validTo property to an old value.
     * This should trigger an approval because of `modify` operation in the policy rule.
     *
     * MID-7317
     */
    @Test
    public void test850DisableAssignmentWithApproval() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        UserType userBefore = getUserFromRepo(USER_HOLDER_OF_ROLE_BEING_DISABLED_WITH_APPROVAL.oid).asObjectable();

        when();
        XMLGregorianCalendar newValidTo = XmlTypeConverter.fromNow("-P1D");
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT,
                        userBefore.getAssignment().get(0).getId(),
                        AssignmentType.F_ACTIVATION,
                        ActivationType.F_VALID_TO)
                .replace(newValidTo)
                .asObjectDelta(USER_HOLDER_OF_ROLE_BEING_DISABLED_WITH_APPROVAL.oid);
        executeChanges(delta, null, task, result);

        then();
        assertInProgress(result);

        List<PrismObject<CaseType>> cases =
                getCasesForObject(USER_HOLDER_OF_ROLE_BEING_DISABLED_WITH_APPROVAL.oid, UserType.COMPLEX_TYPE, null, task, result);
        assertEquals("Wrong # of cases", 2, cases.size());
        CaseType approvalCase = getApprovalCase(cases);
        displayDumpable("Approval case", approvalCase);
        assertThat(approvalCase.getWorkItem()).as("work items").hasSize(1);

        assertUser(USER_HOLDER_OF_ROLE_BEING_DISABLED_WITH_APPROVAL.oid, "after")
                .assignments()
                .single()
                .activation()
                .assertValidTo((XMLGregorianCalendar) null)
                .assertEffectiveStatus(ActivationStatusType.ENABLED);

        when("approving the operation");
        approveWorkItem(approvalCase.getWorkItem().get(0), task, result);

        then("approving the operation");
        waitForCaseClose(getRootCase(cases));

        assertUser(USER_HOLDER_OF_ROLE_BEING_DISABLED_WITH_APPROVAL.oid, "after")
                .assignments()
                .single()
                .activation()
                .assertValidTo(newValidTo)
                .assertEffectiveStatus(ActivationStatusType.DISABLED);
    }

    /**
     * MID-5827
     */
    @Test
    public void test900AssignIdempotentRole() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        unassignAllRoles(USER_JACK.oid);

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(
                        new AssignmentType().targetRef(ROLE_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE),
                        new AssignmentType()
                                .targetRef(ROLE_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                                .beginActivation().validFrom("1990-01-01T00:00:00").end())
                .asObjectDelta(USER_JACK.oid);

        executeChanges(delta, null, task, result);

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        assertNotAssignedRole(jack, ROLE_IDEMPOTENT.oid);

        String ref = result.findAsynchronousOperationReference();
        assertNotNull("No async operation reference", ref);
        String rootCaseOid = OperationResult.referenceToCaseOid(ref);
        List<CaseType> subcases = getSubcases(rootCaseOid, null, result);
        assertEquals("Wrong # of subcases", 2, subcases.size());
        for (CaseType subcase : subcases) {
            assertTrue("Subcase is not an approval one: " + subcase, CaseTypeUtil.isApprovalCase(subcase));
            assertEquals("Wrong # of work items in " + subcase, 1, subcase.getWorkItem().size());
        }

        deleteCaseTree(rootCaseOid, result);
    }

    @Test
    public void test910AssignRoleWithIdempotentMetarole() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        when();
        unassignAllRoles(USER_JACK.oid);

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(
                        new AssignmentType().targetRef(ROLE_WITH_IDEMPOTENT_METAROLE.oid, RoleType.COMPLEX_TYPE),
                        new AssignmentType()
                                .targetRef(ROLE_WITH_IDEMPOTENT_METAROLE.oid, RoleType.COMPLEX_TYPE)
                                .beginActivation().validFrom("1990-01-01T00:00:00").end())
                .asObjectDelta(USER_JACK.oid);

        executeChanges(delta, null, task, result);

        // THEN
        then();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        display("jack", jack);
        assertNotAssignedRole(jack, ROLE_WITH_IDEMPOTENT_METAROLE.oid);

        String ref = result.findAsynchronousOperationReference();
        assertNotNull("No async operation reference", ref);
        String rootCaseOid = OperationResult.referenceToCaseOid(ref);
        List<CaseType> subcases = getSubcases(rootCaseOid, null, result);
        assertEquals("Wrong # of subcases", 2, subcases.size());
        for (CaseType subcase : subcases) {
            assertTrue("Subcase is not an approval one: " + subcase, CaseTypeUtil.isApprovalCase(subcase));
            assertEquals("Wrong # of work items in " + subcase, 1, subcase.getWorkItem().size());
        }

        deleteCaseTree(rootCaseOid, result);
    }

    /**
     * MID-5895
     */
    @Test
    public void test920AssignRoleWithComputedSkip() throws Exception {
        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignAllRoles(USER_JACK.oid);

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType().targetRef(ROLE_SKIPPED.oid, RoleType.COMPLEX_TYPE))
                .asObjectDelta(USER_JACK.oid);

        executeChanges(delta, null, task, result);

        then();
        String ref = result.findAsynchronousOperationReference();
        assertNull("Present async operation reference", ref);

        assertUser(USER_JACK.oid, "after")
                .assertAssignments(1);
    }

    @Test
    public void test930UnassignRoleWithApproval() throws Exception {
        given();
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        unassignAllRoles(USER_JACK.oid);
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(new AssignmentType().targetRef(ROLE_APPROVE_UNASSIGN.oid, RoleType.COMPLEX_TYPE))
                        .<UserType>asObjectDelta(USER_JACK.oid), null, task, result);
        assertUser(USER_JACK.oid, "before")
                .assertAssignments(1);

        when();
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(new AssignmentType().targetRef(ROLE_APPROVE_UNASSIGN.oid, RoleType.COMPLEX_TYPE))
                        .asObjectDelta(USER_JACK.oid), null, task, result);

        then();
        String ref = result.findAsynchronousOperationReference();
        assertNotNull("No async operation reference", ref);

        assertUser(USER_JACK.oid, "after")
                .assertAssignments(1);
    }

    /**
     * MID-7945
     */
    @Test
    public void test940AddSensitiveExtensionProperty() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("a role with sensitive extension property is modified (by adding the extension)");
        PrismContainerValue<?> extensionContainerValue =
                prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class)
                        .findContainerDefinition(RoleType.F_EXTENSION)
                        .instantiate()
                        .getValue();
        extensionContainerValue.createProperty(EXT_SEA)
                .setRealValue("Caribbean");
        executeChanges(
                prismContext.deltaFor(RoleType.class)
                        .item(RoleType.F_EXTENSION)
                        .add(extensionContainerValue.clone())
                        .asObjectDelta(ROLE_EXTENSION_PROPERTY_MOD_APPROVAL.oid),
                null, task, result);

        then("operation is in progress");
        assertInProgress(result);

        and("an approval case is created");
        assertCase(result, "after")
                .subcases()
                .assertSubcases(1);
    }

    /**
     * Assigning two roles: the first with auto-completion result of "skip", the second with standard approval.
     * After approval, both roles should be assigned.
     *
     * MID-9234
     */
    @Test
    public void test950AutocompleteAndApprovalCombined() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("a user");
        UserType user = new UserType()
                .name(userName);
        addObject(user, task, result);

        when("roles are assigned to the user");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(
                                ROLE_SELECTIVE_A.assignmentTo(),
                                ROLE_SELECTIVE_B.assignmentTo())
                        .asObjectDelta(user.getOid()),
                null, task, result);

        then("there are two subcases");
        String ref = result.findAsynchronousOperationReference();
        assertNotNull("No async operation reference", ref);
        String rootCaseOid = OperationResult.referenceToCaseOid(ref);
        List<CaseType> subcases = getSubcases(rootCaseOid, null, result);
        displayCollection("subcases", subcases);
        assertThat(subcases).as("subcases").hasSize(2);

        when("case is approved");
        var openCase = getOpenCaseRequired(subcases);
        approveCase(openCase, task, result);

        then("both roles are assigned");
        waitForCaseClose(rootCaseOid);
        assertUser(user.getOid(), "after")
                .assignments()
                .assertRole(ROLE_SELECTIVE_A.oid)
                .assertRole(ROLE_SELECTIVE_B.oid);
    }

    //connected to #10853 which is not replicated on master, still the test is added for debugging purpose,
    //may be will be removed in later, after resolving the ticket
    @Test
    public void test960CreateNewRoleWhileApprovalProcessWithAutoCompletion() throws Exception {
        given();
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("create new role with assignment to the approval process with auto-completion");
        String newRoleOid = "8fcb9b1c-3a6f-4e47-93c8-1c4d4379dfd0";
        String newRoleName = "addRoleWithAutocompleteApproval";
        RoleType newRole = new RoleType()
                .oid(newRoleOid)
                .name(newRoleName)
                .assignment(ROLE_AUTOCOMPLETE_FILE.assignmentTo());
        ObjectDelta<RoleType> addDelta = newRole.asPrismObject().createAddDelta();
        executeChanges(addDelta, null, task, result);
        CaseType caseObj = assertCase(result, "after")
                .getObject().asObjectable();
        waitForCaseClose(caseObj, 3000);

        // THEN
        then();
        PrismObject<RoleType> createdRole = repositoryService.getObject(RoleType.class, newRoleOid, null, result);
        String xml = getPrismContext().xmlSerializer().serialize(createdRole);
        assertThat(xml.startsWith("<role"))
                .withFailMessage("Expected that created role object xml starts with <role> tag.")
                .isEqualTo(true);
    }

    private void executeAssignRoles123ToJack(boolean immediate,
            boolean approve1, boolean approve2, boolean approve3a, boolean approve3b, boolean securityDeputy) throws Exception {
        Task task = getTestTask();
        String testName = getTestNameShort();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        ObjectDelta<UserType> addRole1Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE21.oid, ObjectTypes.ROLE))
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> addRole2Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE22.oid, ObjectTypes.ROLE))
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> addRole3Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE23.oid, ObjectTypes.ROLE))
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> changeDescriptionDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(testName)
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil
                .summarize(addRole1Delta, addRole2Delta, addRole3Delta, changeDescriptionDelta);
        ObjectDelta<UserType> delta0 = changeDescriptionDelta.clone();
        String originalDescription = getUser(USER_JACK.oid).asObjectable().getDescription();
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jack.clone();
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() {
                return primaryDelta.clone();
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 3;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return Arrays.asList(approve1, approve2, approve3a && approve3b);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return Arrays.asList(addRole1Delta.clone(), addRole2Delta.clone(), addRole3Delta.clone());
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return delta0.clone();
            }

            @Override
            protected String getObjectOid() {
                return jack.getOid();
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return Arrays.asList(
                        new ExpectedTask(ROLE_ROLE21.oid, "Assigning role \"Role21\" to user \"Jack Sparrow (jack)\""),
                        new ExpectedTask(ROLE_ROLE22.oid, "Assigning role \"Role22\" to user \"Jack Sparrow (jack)\""),
                        new ExpectedTask(ROLE_ROLE23.oid, "Assigning role \"Role23\" to user \"Jack Sparrow (jack)\""));
            }

            // after first step
            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                List<ExpectedTask> tasks = getExpectedTasks();
                return Arrays.asList(
                        new ExpectedWorkItem(USER_LEAD21.oid, ROLE_ROLE21.oid, tasks.get(0)),
                        new ExpectedWorkItem(USER_LEAD22.oid, ROLE_ROLE22.oid, tasks.get(1)),
                        new ExpectedWorkItem(USER_LEAD23.oid, ROLE_ROLE23.oid, tasks.get(2))
                );
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                switch (number) {
                    case 0:
                        if (yes) {
                            assertUserProperty(USER_JACK.oid, UserType.F_DESCRIPTION, testName);
                        } else {
                            if (originalDescription != null) {
                                assertUserProperty(USER_JACK.oid, UserType.F_DESCRIPTION, originalDescription);
                            } else {
                                assertUserNoProperty(USER_JACK.oid, UserType.F_DESCRIPTION);
                            }
                        }
                        break;
                    case 1:
                    case 2:
                    case 3:
                        String[] oids = { ROLE_ROLE21.oid, ROLE_ROLE22.oid, ROLE_ROLE23.oid };
                        if (yes) {
                            assertAssignedRole(USER_JACK.oid, oids[number - 1], result);
                        } else {
                            assertNotAssignedRole(USER_JACK.oid, oids[number - 1], result);
                        }
                        break;

                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return null;            // ignore this way of approving
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ExpectedTask> tasks = getExpectedTasks();
                List<ApprovalInstruction> instructions = new ArrayList<>();
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_LEAD21.oid, ROLE_ROLE21.oid, tasks.get(0)), approve1, USER_LEAD21.oid, null));
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_LEAD22.oid, ROLE_ROLE22.oid, tasks.get(1)), approve2, USER_LEAD22.oid, null));
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_LEAD23.oid, ROLE_ROLE23.oid, tasks.get(2)), approve3a, USER_LEAD23.oid, null));
                if (approve3a) {
                    ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(USER_SECURITY_APPROVER.oid, ROLE_ROLE23.oid, tasks.get(2));
                    CheckedRunnable before = () -> {
                        login(getUserFromRepo(USER_SECURITY_APPROVER.oid));
                        checkVisibleWorkItem(expectedWorkItem, 1, task, task.getResult());
                        login(getUserFromRepo(USER_SECURITY_APPROVER_DEPUTY.oid));
                        checkVisibleWorkItem(expectedWorkItem, 1, task, task.getResult());
                        login(getUserFromRepo(USER_SECURITY_APPROVER_DEPUTY_LIMITED.oid));
                        checkVisibleWorkItem(null, 0, task, task.getResult());
                    };
                    instructions.add(new ApprovalInstruction(expectedWorkItem, approve3b,
                            securityDeputy ? USER_SECURITY_APPROVER_DEPUTY.oid : USER_SECURITY_APPROVER.oid, null, before, null));
                }
                return instructions;
            }
        }, 3, immediate);
    }

    private void previewAssignRolesToJack(boolean immediate, boolean also24) throws Exception {
        String testName = getTestNameShort();
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        boolean doTrace = false;
        //noinspection ConstantConditions
        if (doTrace) {
            result.tracingProfile(tracer.compileProfile(addWorkflowLogging(createModelLoggingTracingProfile()), result));
        }

        List<AssignmentType> assignmentsToAdd = new ArrayList<>();
        assignmentsToAdd.add(createAssignmentTo(ROLE_ROLE21.oid, ObjectTypes.ROLE));
        assignmentsToAdd.add(createAssignmentTo(ROLE_ROLE22.oid, ObjectTypes.ROLE));
        assignmentsToAdd.add(createAssignmentTo(ROLE_ROLE23.oid, ObjectTypes.ROLE));
        assignmentsToAdd.add(createAssignmentTo(ROLE_ROLE25.oid, ObjectTypes.ROLE));
        if (also24) {
            assignmentsToAdd.add(createAssignmentTo(ROLE_ROLE24.oid, ObjectTypes.ROLE));
        }
        ObjectDelta<UserType> primaryDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).addRealValues(assignmentsToAdd)
                .item(UserType.F_DESCRIPTION).replace(testName)
                .asObjectDelta(USER_JACK.oid);

        ModelExecuteOptions options = executeOptions()
                .executeImmediatelyAfterApproval(immediate)
                .partialProcessing(new PartialProcessingOptionsType().approvals(PROCESS))
                .firstClickOnly()
                .previewPolicyRulesEnforcement();

        ModelContext<ObjectType> modelContext =
                modelInteractionService.previewChanges(List.of(primaryDelta), options, task, result);

        List<ApprovalSchemaExecutionInformationType> approvalInfo = modelContext.getHookPreviewResults(ApprovalSchemaExecutionInformationType.class);
        PolicyRuleEnforcerPreviewOutputType enforceInfo = modelContext.getPolicyRuleEnforcerPreviewOutput();
        displayContainerablesCollection("Approval infos", approvalInfo);
        display("Enforce info", enforceInfo);
        result.computeStatus();
        //noinspection ConstantConditions
        if (doTrace) {
            tracer.storeTrace(task, result, null);
        }

        // we do not assert success here, because there are (intentional) exceptions in some expressions

        assertEquals("Wrong # of schema execution information pieces", also24 ? 5 : 4, approvalInfo.size());
        assertNotNull("No enforcement preview output", enforceInfo);
        List<EvaluatedPolicyRuleType> enforcementRules = enforceInfo.getRule();
        assertEquals("Wrong # of enforcement rules", also24 ? 1 : 0, enforcementRules.size());

        // shortcuts
        final String l1 = USER_LEAD21.oid, l2 = USER_LEAD22.oid, l3 = USER_LEAD23.oid, l4 = USER_LEAD24.oid;

        assertApprovalInfo(approvalInfo, ROLE_ROLE21.oid,
                new ExpectedStagePreview(1, set(l1), set(l1)));
        assertApprovalInfo(approvalInfo, ROLE_ROLE22.oid,
                new ExpectedStagePreview(1, set(l2), set(l2)));
        assertApprovalInfo(approvalInfo, ROLE_ROLE23.oid,
                new ExpectedStagePreview(1, set(l3), set(l3)),
                new ExpectedStagePreview(2, set(USER_SECURITY_APPROVER.oid), set(USER_SECURITY_APPROVER.oid)));
        if (also24) {
            assertApprovalInfo(approvalInfo, ROLE_ROLE24.oid,
                    new ExpectedStagePreview(1, set(l4), set(l4)));
        }
        assertApprovalInfo(approvalInfo, ROLE_ROLE25.oid,
                new ExpectedStagePreview(1, set(l1, l2, l3, l4), set(l1, l2, l3, l4)),
                new ExpectedStagePreview(2, set(), set(l3)),
                new ExpectedStagePreview(3, set(ORG_LEADS2122.oid), set(ORG_LEADS2122.oid)),
                new ExpectedStagePreview(4, set(ORG_LEADS2122.oid), set(l1, l2)),
                new ExpectedStagePreview(5, set(l1, l2, l3, l4), set(), APPROVE, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(6, set(l1, l2, l3, l4), set(), APPROVE, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(7, set(l1, l2, l3, l4), set(), SKIP, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(8, set(l1, l2, l3, l4), set(), REJECT, AUTO_COMPLETION_CONDITION),
                new ExpectedStagePreview(9, set(l1, l2, l3, l4), set(l1, l2, l3, l4), true),
                new ExpectedStagePreview(10, set(), set(), REJECT, NO_ASSIGNEES_FOUND));

    }

    private Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private void assertApprovalInfo(List<ApprovalSchemaExecutionInformationType> infos, String targetOid,
            ExpectedStagePreview... expectedStagePreviews) {
        ApprovalSchemaExecutionInformationType found = null;
        for (ApprovalSchemaExecutionInformationType info : infos) {
            assertNotNull("No taskRef", info.getCaseRef());
            PrismObject<?> object = info.getCaseRef().asReferenceValue().getObject();
            assertNotNull("No case in caseRef", object);
            CaseType aCase = (CaseType) object.asObjectable();
            ApprovalContextType wfc = aCase.getApprovalContext();
            assertNotNull("No wf context in caseRef", wfc);
            assertNotNull("No targetRef in caseRef", aCase.getTargetRef());
            if (targetOid.equals(aCase.getTargetRef().getOid())) {
                found = info;
                break;
            }
        }
        assertNotNull("No approval info for target '" + targetOid + "' found", found);
        String taskName = getOrig(found.getCaseRef().getTargetName());
        assertEquals("Wrong # of stage info in " + taskName, expectedStagePreviews.length, found.getStage().size());
        for (int i = 0; i < expectedStagePreviews.length; i++) {
            ExpectedStagePreview expectedStagePreview = expectedStagePreviews[i];
            ApprovalStageExecutionInformationType stagePreview = found.getStage().get(i);
            String pos = taskName + "/" + (i + 1);
            assertNotNull("no stage definition at " + pos, stagePreview.getDefinition());
            assertNotNull("no execution preview at " + pos, stagePreview.getExecutionPreview());

            assertEquals("Wrong preview stage number at " + pos, (Integer) (i + 1), stagePreview.getNumber());
            assertEquals("Wrong definition stage number at " + pos, (Integer) (i + 1), stagePreview.getDefinition().getNumber());

            assertEquals("Stage definition approver ref info differs at " + pos, expectedStagePreview.definitionApproverOids, getOids(stagePreview.getDefinition().getApproverRef()));
            assertEquals("Stage expected approver ref info differs at " + pos, expectedStagePreview.expectedApproverOids, getOids(stagePreview.getExecutionPreview().getExpectedApproverRef()));
            assertEquals("Unexpected outcome at " + pos, expectedStagePreview.outcome, stagePreview.getExecutionPreview().getExpectedAutomatedOutcome());
            assertEquals("Unexpected completion reason at " + pos, expectedStagePreview.reason, stagePreview.getExecutionPreview().getExpectedAutomatedCompletionReason());
            if (expectedStagePreview.hasError) {
                assertNotNull("Error should be present at " + pos, stagePreview.getExecutionPreview().getErrorMessage());
            } else {
                //noinspection SimplifiedTestNGAssertion
                assertEquals("Error message differs at " + pos, null, stagePreview.getExecutionPreview().getErrorMessage());
            }
        }
    }

    private Set<String> getOids(List<ObjectReferenceType> refs) {
        return new HashSet<>(ObjectTypeUtil.objectReferenceListToOids(refs));
    }

    @SuppressWarnings("SameParameterValue")
    private void executeUnassignRoles123ToJack(boolean immediate, boolean approve, boolean byId,
            boolean has1and2) throws Exception {
        String testName = getTestNameShort();
        PrismObject<UserType> jack = getUser(USER_JACK.oid);
        AssignmentType a1 = has1and2 ? findAssignmentByTargetRequired(jack, ROLE_ROLE21.oid) : null;
        AssignmentType a2 = has1and2 ? findAssignmentByTargetRequired(jack, ROLE_ROLE22.oid) : null;
        AssignmentType a3 = findAssignmentByTargetRequired(jack, ROLE_ROLE23.oid);
        AssignmentType del1 = toDelete(a1, byId);
        AssignmentType del2 = toDelete(a2, byId);
        AssignmentType del3 = toDelete(a3, byId);
        ObjectDelta<UserType> deleteRole1Delta = has1and2 ? prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(del1)
                .asObjectDelta(USER_JACK.oid) : null;
        ObjectDelta<UserType> deleteRole2Delta = has1and2 ? prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(del2)
                .asObjectDelta(USER_JACK.oid) : null;
        ObjectDelta<UserType> deleteRole3Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(del3)
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> changeDescriptionDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(testName)
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil
                .summarize(changeDescriptionDelta, deleteRole1Delta, deleteRole2Delta, deleteRole3Delta);
        ObjectDelta<UserType> delta0 = ObjectDeltaCollectionsUtil
                .summarize(changeDescriptionDelta, deleteRole1Delta, deleteRole2Delta);
        String originalDescription = getUser(USER_JACK.oid).asObjectable().getDescription();
        executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jack.clone();
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() {
                return primaryDelta.clone();
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return singletonList(approve);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return singletonList(deleteRole3Delta.clone());
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return delta0.clone();
            }

            @Override
            protected String getObjectOid() {
                return jack.getOid();
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return singletonList(
                        new ExpectedTask(ROLE_ROLE23.oid, "Unassigning role \"Role23\" from user \"Jack Sparrow (jack)\""));
            }

            // after first step
            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                List<ExpectedTask> tasks = getExpectedTasks();
                return singletonList(
                        new ExpectedWorkItem(USER_SECURITY_APPROVER.oid, ROLE_ROLE23.oid, tasks.get(0))
                );
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                switch (number) {
                    case 0:
                        if (yes) {
                            assertUserProperty(USER_JACK.oid, UserType.F_DESCRIPTION, testName);
                        } else {
                            if (originalDescription != null) {
                                assertUserProperty(USER_JACK.oid, UserType.F_DESCRIPTION, originalDescription);
                            } else {
                                assertUserNoProperty(USER_JACK.oid, UserType.F_DESCRIPTION);
                            }
                        }
                        if (yes || !has1and2) {
                            assertNotAssignedRole(USER_JACK.oid, ROLE_ROLE21.oid, result);
                            assertNotAssignedRole(USER_JACK.oid, ROLE_ROLE22.oid, result);
                        } else {
                            assertAssignedRole(USER_JACK.oid, ROLE_ROLE21.oid, result);
                            assertAssignedRole(USER_JACK.oid, ROLE_ROLE22.oid, result);
                        }
                        break;
                    case 1:
                        if (yes) {
                            assertNotAssignedRole(USER_JACK.oid, ROLE_ROLE23.oid, result);
                        } else {
                            assertAssignedRole(USER_JACK.oid, ROLE_ROLE23.oid, result);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected delta number: " + number);
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) {
                return null;            // ignore this way of approving
            }

            @Override
            public List<ApprovalInstruction> getApprovalSequence() {
                List<ExpectedTask> tasks = getExpectedTasks();
                List<ApprovalInstruction> instructions = new ArrayList<>();
                instructions.add(new ApprovalInstruction(
                        new ExpectedWorkItem(USER_SECURITY_APPROVER.oid, ROLE_ROLE23.oid, tasks.get(0)), approve,
                        USER_SECURITY_APPROVER.oid, null));
                return instructions;
            }
        }, 1, immediate);
    }

    private AssignmentType toDelete(AssignmentType assignment, boolean byId) {
        if (assignment == null) {
            return null;
        }
        if (!byId) {
            return assignment.clone();
        } else {
            AssignmentType rv = new AssignmentType();
            rv.setId(assignment.getId());
            return rv;
        }
    }

    private static class ExpectedStagePreview {
        @SuppressWarnings({ "FieldCanBeLocal", "unused" }) private final int number;
        private final Set<String> definitionApproverOids;
        private final Set<String> expectedApproverOids;
        private final ApprovalLevelOutcomeType outcome;
        private final AutomatedCompletionReasonType reason;
        private final boolean hasError;

        private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids) {
            this(number, definitionApproverOids, expectedApproverOids, null, null, false);
        }

        private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids,
                ApprovalLevelOutcomeType outcome, AutomatedCompletionReasonType reason) {
            this(number, definitionApproverOids, expectedApproverOids, outcome, reason, false);
        }

        private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids,
                boolean hasError) {
            this(number, definitionApproverOids, expectedApproverOids, null, null, hasError);
        }

        private ExpectedStagePreview(int number, Set<String> definitionApproverOids, Set<String> expectedApproverOids,
                ApprovalLevelOutcomeType outcome, AutomatedCompletionReasonType reason, boolean hasError) {
            this.number = number;
            this.definitionApproverOids = definitionApproverOids;
            this.expectedApproverOids = expectedApproverOids;
            this.outcome = outcome;
            this.reason = reason;
            this.hasError = hasError;
        }
    }
}
