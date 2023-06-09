/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.assignments;

import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Testing approvals of role assignments: create/delete assignment, potentially for more roles and combined with other operations.
 * Testing also with deputies specified.
 * <p>
 * Subclasses provide specializations regarding ways how rules and/or approvers are attached to roles.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractTestAssignmentApproval extends AbstractWfTestPolicy {

    static final File TEST_RESOURCE_DIR = new File("src/test/resources/assignments");

    private static final TestObject<RoleType> METAROLE_DEFAULT = TestObject.file(
            TEST_RESOURCE_DIR, "metarole-default.xml", "00000001-d34d-b33f-f00d-b00000000001");

    // Roles 1-3 are approved using implicit or global policy rule -- they have no metarole causing approval
    // The approval is triggered because Lead 1-3 are set as approvers for these roles.
    // There is no approver for role 4 so it does not undertake aby approval.
    static final TestObject<RoleType> ROLE1 = TestObject.file(
            TEST_RESOURCE_DIR, "role-role1.xml", "00000001-d34d-b33f-f00d-000000000001");
    static final TestObject<RoleType> ROLE2 = TestObject.file(
            TEST_RESOURCE_DIR, "role-role2.xml", "00000001-d34d-b33f-f00d-000000000002");
    static final TestObject<RoleType> ROLE3 = TestObject.file(
            TEST_RESOURCE_DIR, "role-role3.xml", "00000001-d34d-b33f-f00d-000000000003");
    static final TestObject<RoleType> ROLE4 = TestObject.file(
            TEST_RESOURCE_DIR, "role-role4.xml", "00000001-d34d-b33f-f00d-000000000004");

    // Roles 1b-3b are approved using metarole holding a policy rule that engages users with "special-approver" relation.
    // The approval is triggered because Lead 1-3 are set as "special approvers" for these roles.
    // There is no approver for role 4 so it does not undertake aby approval.
    static final TestObject<RoleType> ROLE1B = TestObject.file(
            TEST_RESOURCE_DIR, "role-role1b.xml", "00000001-d34d-b33f-f00d-00000000001b");
    static final TestObject<RoleType> ROLE2B = TestObject.file(
            TEST_RESOURCE_DIR, "role-role2b.xml", "00000001-d34d-b33f-f00d-00000000002b");
    static final TestObject<RoleType> ROLE3B = TestObject.file(
            TEST_RESOURCE_DIR, "role-role3b.xml", "00000001-d34d-b33f-f00d-00000000003b");
    static final TestObject<RoleType> ROLE4B = TestObject.file(
            TEST_RESOURCE_DIR, "role-role4b.xml", "00000001-d34d-b33f-f00d-00000000004b");

    // Note: Role10/10b is induced so it is _not_ being approved. Only direct assignments are covered by approvals.
    static final TestObject<RoleType> ROLE10 = TestObject.file(
            TEST_RESOURCE_DIR, "role-role10.xml", "00000001-d34d-b33f-f00d-000000000010");
    static final TestObject<RoleType> ROLE10B = TestObject.file(
            TEST_RESOURCE_DIR, "role-role10b.xml", "00000001-d34d-b33f-f00d-00000000010b");

    // delegation for jack-deputy is created only when needed
    private static final TestObject<UserType> USER_JACK_DEPUTY = TestObject.file(
            TEST_RESOURCE_DIR, "user-jack-deputy.xml", "e44769f2-030b-4e9c-9ddf-76bb3a348f9c");
    private static final TestObject<UserType> USER_LEAD1 = TestObject.file(
            TEST_RESOURCE_DIR, "user-lead1.xml", "00000001-d34d-b33f-f00d-a00000000001");
    private static final TestObject<UserType> USER_LEAD1_DEPUTY_1 = TestObject.file(
            TEST_RESOURCE_DIR, "user-lead1-deputy1.xml", "00000001-d34d-b33f-f00d-ad1000000001");
    private static final TestObject<UserType> USER_LEAD1_DEPUTY_2 = TestObject.file(
            TEST_RESOURCE_DIR, "user-lead1-deputy2.xml", "00000001-d34d-b33f-f00d-ad1000000002");
    private static final TestObject<UserType> USER_LEAD2 = TestObject.file(
            TEST_RESOURCE_DIR, "user-lead2.xml", "00000001-d34d-b33f-f00d-a00000000002");
    private static final TestObject<UserType> USER_LEAD3 = TestObject.file(
            TEST_RESOURCE_DIR, "user-lead3.xml", "00000001-d34d-b33f-f00d-a00000000003");
    private static final TestObject<UserType> USER_LEAD10 = TestObject.file(
            TEST_RESOURCE_DIR, "user-lead10.xml", "00000001-d34d-b33f-f00d-a00000000010");

    // Draft user. His assignments should undertake approvals just like other users' assignments (MID-6113).
    private static final TestObject<UserType> USER_DRAFT =
            TestObject.file(TEST_RESOURCE_DIR, "user-draft.xml", "e3c00bba-8ce0-4727-a294-91842264c2de");

    protected abstract TestObject<RoleType> getRole(int number);

    private boolean lead1DeputiesLoaded;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                METAROLE_DEFAULT,
                ROLE1, ROLE1B, ROLE2, ROLE2B, ROLE3, ROLE3B, ROLE4, ROLE4B, ROLE10, ROLE10B,
                USER_JACK_DEPUTY, USER_LEAD1, USER_LEAD2, USER_LEAD3);

        // LEAD10 will be imported later

        repoAdd(USER_DRAFT, initResult); // intentionally not via model
    }

    /**
     * The simplest case: addition of an assignment of single security-sensitive role (Role1).
     * Although it induces Role10 membership, it is not a problem, as Role10 approver (Lead10) is not imported yet.
     * (Even if it was, Role10 assignment would not be approved, see test030.)
     */
    @Test
    public void test010AddRole1Assignment() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        task.addTracingRequest(TracingRootType.CLOCKWORK_RUN);
        task.addTracingRequest(TracingRootType.WORKFLOW_OPERATION);
        task.setTracingProfile(
                new TracingProfileType()
                        .createTraceFile(false)
                        .collectLogEntries(true)
        );

        executeAssignRole1(USER_JACK, false, false, USER_LEAD1.oid, null, result);

        then("no unresolved object reference in audited deltas (MID-5814)");
        assertThatOperationResult(result)
                .isTracedSomewhere()
                .noneLogEntryMatches(text ->
                        text.contains("Unresolved object reference in delta being audited"));

        and("the execution task has correct archetype (MID-6611)");
        String caseOid = getReferencedCaseOidRequired(result);

        ObjectQuery taskQuery = queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(caseOid)
                .build();
        PrismObject<TaskType> executionTask = MiscUtil.extractSingletonRequired(
                modelService.searchObjects(TaskType.class, taskQuery, null, task, result),
                () -> new IllegalStateException("More execution tasks for case " + caseOid),
                () -> new IllegalStateException("No execution task for case " + caseOid));
        assertTask(executionTask.asObjectable(), "execution task after")
                .display()
                .assertArchetypeRef(SystemObjectsType.ARCHETYPE_APPROVAL_TASK.value());
    }

    /**
     * Removing recently added assignment of single security-sensitive role. Should execute without approval (for now).
     */
    @Test
    public void test020DeleteRole1Assignment() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        when("role1 unassign is requested");
        String roleOid = getRole(1).oid;
        unassignRole(USER_JACK.oid, roleOid, task, result);

        then("it is unassigned");
        assertSuccess(result);
        assertNotAssignedRole(getUser(USER_JACK.oid), roleOid);
    }

    /**
     * Repeating {@link #test010AddRole1Assignment()}; this time with Lead10 present.
     *
     * So we are approving an assignment of single security-sensitive role (`Role1`), that induces another security-sensitive role
     * (`Role10`). Because of current implementation constraints, only the first assignment should be brought to approval.
     */
    @Test
    public void test030AddRole1AssignmentAgain() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        importLead10(task, result);
        executeAssignRole1(USER_JACK, false, false, USER_LEAD1.oid, null, result);
    }

    /**
     * The same as above, but with immediate execution.
     */
    @Test
    public void test040AddRole1AssignmentImmediate() throws Exception {
        login(userAdministrator);
        OperationResult result = getTestOperationResult();

        unassignAllRoles(USER_JACK.oid);
        executeAssignRole1(USER_JACK, true, false, USER_LEAD1.oid, null, result);
    }

    /**
     * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
     * are not to be approved.
     * <p>
     * Decisions for roles 1-3 are rejected.
     */
    @Test
    public void test050AddRoles123AssignmentNNN() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRoles123ToJack(false, false, false, false);
    }

    /**
     * The same as above, but with immediate execution.
     */
    @Test
    public void test052AddRoles123AssignmentNNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRoles123ToJack(true, false, false, false);
    }

    /**
     * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
     * are not to be approved.
     * <p>
     * Decision for role 1 is accepted.
     */
    @Test
    public void test060AddRoles123AssignmentYNN() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRoles123ToJack(false, true, false, false);
    }

    @Test
    public void test062AddRoles123AssignmentYNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRoles123ToJack(true, true, false, false);
    }

    /**
     * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
     * are not to be approved.
     * <p>
     * Decisions for roles 1-3 are accepted.
     */
    @Test
    public void test070AddRoles123AssignmentYYY() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRoles123ToJack(false, true, true, true);
    }

    @Test
    public void test072AddRoles123AssignmentYYYImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRoles123ToJack(true, true, true, true);
    }

    @Test // MID-4355
    public void test100AddCreateDelegation() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        task.setOwner(userAdministrator);
        OperationResult result = getTestOperationResult();

        when();
        assignDeputy(USER_JACK_DEPUTY.oid, USER_JACK.oid, task, result);

        then();
        PrismObject<UserType> deputy = getUser(USER_JACK_DEPUTY.oid);
        display("deputy after", deputy);

        result.computeStatus();
        assertSuccess(result);
        assertAssignedDeputy(deputy, USER_JACK.oid);
    }

    /**
     * Assigning Role1 with two deputies present. (But approved by the delegator.)
     */
    @Test
    public void test130AddRole1aAssignmentWithDeputy() throws Exception {
        login(userAdministrator);
        Task task = getTestTask();
        OperationResult result = task.getResult();

        importLead1Deputies(task, result);

        unassignAllRoles(USER_JACK.oid);
        executeAssignRole1(USER_JACK, false, true, USER_LEAD1.oid, null, result);
    }

    /**
     * Assigning Role1 with two deputies present. (Approved by one of the deputies.)
     */
    @Test
    public void test132AddRole1aAssignmentWithDeputyApprovedByDeputy1() throws Exception {
        login(userAdministrator);
        OperationResult result = getTestOperationResult();

        unassignAllRoles(USER_JACK.oid);
        executeAssignRole1(
                USER_JACK, false, true, USER_LEAD1_DEPUTY_1.oid, null, result);
    }

    @Test(enabled = false)
    public void test150AddRole1ApproverAssignment() throws Exception {
        login(userAdministrator);
        OperationResult result = getTestOperationResult();

        unassignAllRoles(USER_JACK.oid);
        executeAssignRole1(
                USER_JACK, false, true, USER_LEAD1.oid, SchemaConstants.ORG_APPROVER, result);
    }

    @Test
    public void test200AddRole1AssignmentToDraftUser() throws Exception {
        login(userAdministrator);
        OperationResult result = getTestOperationResult();

        executeAssignRole1(USER_DRAFT, false, true, USER_LEAD1.oid, null, result);
    }

    private void executeAssignRole1(
            TestObject<UserType> userObject,
            boolean immediate,
            boolean deputiesOfLeadOneSeeItems,
            String approverOid,
            QName relation,
            OperationResult result) throws Exception {
        Task task = getTestTask();

        String userOid = userObject.oid;
        PrismObject<UserType> user = getUser(userOid);
        String userDisplayName = user.asObjectable().getFullName() + " (" + user.asObjectable().getName() + ")";

        TestObject<RoleType> roleObject = getRole(1);

        ObjectDelta<UserType> delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType()
                        .targetRef(roleObject.oid, RoleType.COMPLEX_TYPE, relation))
                .asObjectDelta(userOid);

        when("role1 is requested");
        executeChanges(delta, getOptions(immediate), task, result);

        // @formatter:off
        var workItem = assertReferencedCase(result)
                .subcases()
                .singleWithApprovalSchema()
                    .display()
                    .assertObjectRef(userObject.ref())
                    .assertTargetRef(roleObject.ref())
                    .assertOpenApproval(String.format("Assigning role \"%s\" to user \"%s\"",
                            roleObject.getObjectable().getName(), userDisplayName))
                    .assertDeltasToApprove(delta)
                    .workItems()
                        .single()
                            .assertAssignees(USER_LEAD1.oid)
                            .getRealValue();
        // @formatter:on

        then("role is not assigned yet");
        assertUser(userOid, "before approval")
                .assignments()
                .assertNoRole(roleObject.oid);

        when("work item is approved");
        assertActiveWorkItems(USER_LEAD1.oid, 1);
        if (lead1DeputiesLoaded || deputiesOfLeadOneSeeItems) {
            assertActiveWorkItems(USER_LEAD1_DEPUTY_1.oid, deputiesOfLeadOneSeeItems ? 1 : 0);
            assertActiveWorkItems(USER_LEAD1_DEPUTY_2.oid, deputiesOfLeadOneSeeItems ? 1 : 0);
        }
        login(getUser(approverOid));
        approveWorkItem(workItem, task, result);
        login(userAdministrator);
        waitForCaseClose(getReferencedCaseOidRequired(result));

        then("role is assigned, audit and metadata are OK");
        assertUserAfter(userOid)
                .assignments()
                .assertRole(roleObject.oid);

        checkAuditRecords(createResultMap(roleObject.oid, WorkflowResult.APPROVED));
        checkUserApprovers(userOid, singletonList(approverOid), result);
    }

    private List<PrismReferenceValue> getPotentialAssignees(PrismObject<UserType> user) {
        List<PrismReferenceValue> rv = new ArrayList<>();
        rv.add(ObjectTypeUtil.createObjectRef(user).asReferenceValue());
        for (PrismReferenceValue delegatorReference : DeputyUtils.getDelegatorReferences(user.asObjectable(), relationRegistry)) {
            rv.add(new ObjectReferenceType().oid(delegatorReference.getOid()).asReferenceValue());
        }
        return rv;
    }

    private void assertActiveWorkItems(String approverOid, int expectedCount) throws Exception {
        if (approverOid == null && expectedCount == 0) {
            return;
        }
        Task task = getTestTask();
        ObjectQuery query = prismContext.queryFor(CaseWorkItemType.class)
                .item(CaseWorkItemType.F_ASSIGNEE_REF).ref(getPotentialAssignees(getUser(approverOid)))
                .and().item(CaseWorkItemType.F_CLOSE_TIMESTAMP).isNull()
                .build();
        List<CaseWorkItemType> items = modelService.searchContainers(CaseWorkItemType.class, query, null, task, task.getResult());
        assertEquals("Wrong active work items for " + approverOid, expectedCount, items.size());
    }

    private void executeAssignRoles123ToJack(boolean immediate, boolean approve1, boolean approve2, boolean approve3)
            throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String testName = getTestNameShort();
        TestObject<RoleType> role1 = getRole(1);
        TestObject<RoleType> role2 = getRole(2);
        TestObject<RoleType> role3 = getRole(3);
        TestObject<RoleType> role4 = getRole(4);

        ObjectDelta<UserType> addRole1Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(role1.assignmentTo())
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> addRole2Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(role2.assignmentTo())
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> addRole3Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(role3.assignmentTo())
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> addRole4Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(getRole(4).assignmentTo())
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> changeDescriptionDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(testName)
                .asObjectDelta(USER_JACK.oid);
        ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil
                .summarize(addRole1Delta, addRole2Delta, addRole3Delta, addRole4Delta, changeDescriptionDelta);
        ObjectDelta<UserType> delta0 = ObjectDeltaCollectionsUtil.summarize(addRole4Delta, changeDescriptionDelta);
        String originalDescription = getUser(USER_JACK.oid).asObjectable().getDescription();
        String jackDisplayName = "Jack Sparrow (jack)";

        when("roles are requested");
        executeChanges(primaryDelta, getOptions(immediate), task, result);

        then("request case with subcases is created");
        IntFunction<String> caseNameFormatter =
                i -> String.format("Assigning role \"%s\" to user \"%s\"", getRoleName(i), jackDisplayName);

        // @formatter:off
        var aCase = getObject(CaseType.class, getReferencedCaseOidRequired(result)).asObjectable();
        var workItem1 = assertCase(aCase, "")
                .subcases()
                .singleForTarget(role1.oid)
                    .display()
                    .assertObjectRef(USER_JACK.ref())
                    .assertOpenApproval(caseNameFormatter.apply(1))
                    .assertDeltasToApprove(addRole1Delta)
                    .workItems()
                        .single()
                            .assertAssignees(USER_LEAD1.oid)
                            .getRealValue();
        var workItem2 = assertCase(aCase, "")
                .subcases()
                .singleForTarget(role2.oid)
                    .display()
                    .assertObjectRef(USER_JACK.ref())
                    .assertOpenApproval(caseNameFormatter.apply(2))
                    .assertDeltasToApprove(addRole2Delta)
                    .workItems()
                        .single()
                            .assertAssignees(USER_LEAD2.oid)
                            .getRealValue();
        var workItem3 = assertCase(aCase, "")
                .subcases()
                .singleForTarget(role3.oid)
                    .display()
                    .assertObjectRef(USER_JACK.ref())
                    .assertOpenApproval(caseNameFormatter.apply(3))
                    .assertDeltasToApprove(addRole3Delta)
                    .workItems()
                        .single()
                            .assertAssignees(USER_LEAD3.oid)
                            .getRealValue();
        var case0Oid = assertCase(aCase, "")
                .subcases()
                .assertSubcases(4)
                .singleWithoutApprovalSchema()
                    .assertDeltasToApprove(delta0)
                    .getOid();
        // @formatter:on

        and("changes are not applied yet");
        assertUser(USER_JACK.oid, "before approval")
                .assignments()
                .assertNoRole(role1.oid)
                .assertNoRole(role2.oid)
                .assertNoRole(role3.oid);

        if (immediate) {
            waitForCaseClose(case0Oid);
            assertUser(USER_JACK.oid, "before approval")
                    .assertDescription(testName)
                    .assignments()
                    .assertRole(role4.oid);
        } else {
            assertUser(USER_JACK.oid, "before approval")
                    .assertDescription(originalDescription)
                    .assignments()
                    .assertNoRole(role4.oid);
        }

        when("work items are completed");
        login(USER_LEAD1.get());
        approveOrRejectWorkItem(workItem1, approve1, task, result);
        login(USER_LEAD2.get());
        approveOrRejectWorkItem(workItem2, approve2, task, result);
        login(USER_LEAD3.get());
        approveOrRejectWorkItem(workItem3, approve3, task, result);
        login(userAdministrator);
        waitForCaseClose(getReferencedCaseOidRequired(result));

        then("roles are assigned (or not)");
        assertUserAfter(USER_JACK.oid)
                .assignments()
                .assertRolePresence(role1.oid, approve1)
                .assertRolePresence(role2.oid, approve2)
                .assertRolePresence(role3.oid, approve3);
    }

    @Test
    public void test300ApprovalAndEnforce() throws Exception {
        // ignored by default (put here so that zzzMarkAsNotInitialized() will be executed after this one!)
    }

    private void importLead10(Task task, OperationResult result) throws Exception {
        task.setChannel(SchemaConstants.CHANNEL_INIT_URI); // to disable approvals
        USER_LEAD10.init(this, task, result);
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
    }

    private void importLead1Deputies(Task task, OperationResult result) throws Exception {
        var origChannel = task.getChannel();
        task.setChannel(SchemaConstants.CHANNEL_INIT_URI); // To avoid approvals
        USER_LEAD1_DEPUTY_1.init(this, task, result);
        USER_LEAD1_DEPUTY_2.init(this, task, result);
        task.setChannel(origChannel);

        lead1DeputiesLoaded = true;
    }

    private String getRoleName(int number) {
        return getRole(number).getObjectable().getName().getOrig();
    }
}
