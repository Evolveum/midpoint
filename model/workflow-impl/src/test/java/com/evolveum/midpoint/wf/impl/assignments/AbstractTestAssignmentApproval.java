/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.assignments;

import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.TestResource;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.OperationResultAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.ExpectedTask;
import com.evolveum.midpoint.wf.impl.ExpectedWorkItem;
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

    private static final File METAROLE_DEFAULT_FILE = new File(TEST_RESOURCE_DIR, "metarole-default.xml");

    // Roles 1-3 are approved using implicit or global policy rule -- they have no metarole causing approval
    // The approval is triggered because Lead 1-3 are set as approvers for these roles.
    // There is no approver for role 4 so it does not undertake aby approval.
    static final TestResource ROLE1 = new TestResource(TEST_RESOURCE_DIR, "role-role1.xml", "00000001-d34d-b33f-f00d-000000000001");
    static final TestResource ROLE2 = new TestResource(TEST_RESOURCE_DIR, "role-role2.xml", "00000001-d34d-b33f-f00d-000000000002");
    static final TestResource ROLE3 = new TestResource(TEST_RESOURCE_DIR, "role-role3.xml", "00000001-d34d-b33f-f00d-000000000003");
    static final TestResource ROLE4 = new TestResource(TEST_RESOURCE_DIR, "role-role4.xml", "00000001-d34d-b33f-f00d-000000000004");

    // Roles 1b-3b are approved using metarole holding a policy rule that engages users with "special-approver" relation.
    // The approval is triggered because Lead 1-3 are set as "special approvers" for these roles.
    // There is no approver for role 4 so it does not undertake aby approval.
    static final TestResource ROLE1B = new TestResource(TEST_RESOURCE_DIR, "role-role1b.xml", "00000001-d34d-b33f-f00d-00000000001b");
    static final TestResource ROLE2B = new TestResource(TEST_RESOURCE_DIR, "role-role2b.xml", "00000001-d34d-b33f-f00d-00000000002b");
    static final TestResource ROLE3B = new TestResource(TEST_RESOURCE_DIR, "role-role3b.xml", "00000001-d34d-b33f-f00d-00000000003b");
    static final TestResource ROLE4B = new TestResource(TEST_RESOURCE_DIR, "role-role4b.xml", "00000001-d34d-b33f-f00d-00000000004b");

    // Note: Role10/10b is induced so it is _not_ being approved. Only direct assignments are covered by approvals.
    static final TestResource ROLE10 = new TestResource(TEST_RESOURCE_DIR, "role-role10.xml", "00000001-d34d-b33f-f00d-000000000010");
    static final TestResource ROLE10B = new TestResource(TEST_RESOURCE_DIR, "role-role10b.xml", "00000001-d34d-b33f-f00d-00000000010b");

    // delegation for jack-deputy is created only when needed
    private static final TestResource USER_JACK_DEPUTY = new TestResource(TEST_RESOURCE_DIR, "user-jack-deputy.xml", "e44769f2-030b-4e9c-9ddf-76bb3a348f9c");
    private static final TestResource USER_LEAD1 = new TestResource(TEST_RESOURCE_DIR, "user-lead1.xml", "00000001-d34d-b33f-f00d-L00000000001");
    private static final TestResource USER_LEAD1_DEPUTY_1 = new TestResource(TEST_RESOURCE_DIR, "user-lead1-deputy1.xml", "00000001-d34d-b33f-f00d-LD1000000001");
    private static final TestResource USER_LEAD1_DEPUTY_2 = new TestResource(TEST_RESOURCE_DIR, "user-lead1-deputy2.xml", "00000001-d34d-b33f-f00d-LD1000000002");
    private static final TestResource USER_LEAD2 = new TestResource(TEST_RESOURCE_DIR, "user-lead2.xml", "00000001-d34d-b33f-f00d-L00000000002");
    private static final TestResource USER_LEAD3 = new TestResource(TEST_RESOURCE_DIR, "user-lead3.xml", "00000001-d34d-b33f-f00d-L00000000003");
    private static final TestResource USER_LEAD10 = new TestResource(TEST_RESOURCE_DIR, "user-lead10.xml", "00000001-d34d-b33f-f00d-L00000000010");

    // Draft user. His assignments should undertake approvals just like other users' assignments (MID-6113).
    private static final TestResource USER_DRAFT = new TestResource(TEST_RESOURCE_DIR, "user-draft.xml","e3c00bba-8ce0-4727-a294-91842264c2de");

    protected abstract String getRoleOid(int number);
    protected abstract String getRoleName(int number);

    private boolean lead1DeputiesLoaded;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(METAROLE_DEFAULT_FILE, initResult);

        repoAdd(ROLE1, initResult);
        repoAdd(ROLE1B, initResult);
        repoAdd(ROLE2, initResult);
        repoAdd(ROLE2B, initResult);
        repoAdd(ROLE3, initResult);
        repoAdd(ROLE3B, initResult);
        repoAdd(ROLE4, initResult);
        repoAdd(ROLE4B, initResult);
        repoAdd(ROLE10, initResult);
        repoAdd(ROLE10B, initResult);

        repoAdd(USER_JACK_DEPUTY, initResult);
        addAndRecomputeUser(USER_LEAD1.file, initTask, initResult);
        addAndRecomputeUser(USER_LEAD2.file, initTask, initResult);
        addAndRecomputeUser(USER_LEAD3.file, initTask, initResult);
        // LEAD10 will be imported later!

        repoAdd(USER_DRAFT, initResult);
    }

    /**
     * The simplest case: addition of an assignment of single security-sensitive role (Role1).
     * Although it induces Role10 membership, it is not a problem, as Role10 approver (Lead10) is not imported yet.
     * (Even if it was, Role10 assignment would not be approved, see test030.)
     */
    @Test
    public void test010AddRole1Assignment() throws Exception {
        login(userAdministrator);

        OperationResult result = executeAssignRole1(userJackOid, false, false, USER_LEAD1.oid, null, true);

        // MID-5814
        OperationResultAsserter.forResult(result)
                .assertTracedSomewhere()                // just checking that tracing works
                .assertNoLogEntry(text -> text.contains("Unresolved object reference in delta being audited"));
    }

    /**
     * Removing recently added assignment of single security-sensitive role. Should execute without approval (for now).
     */
    @Test
    public void test020DeleteRole1Assignment() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, userJackOid, result);
        addFocusDeltaToContext(context,
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).delete(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
                        .asObjectDelta(userJackOid));
        clockwork.run(context, task, result);

        assertEquals("Wrong context state", ModelState.FINAL, context.getState());
        result.computeStatusIfUnknown();
        TestUtil.assertSuccess(result);
        assertNotAssignedRole(getUser(userJackOid), getRoleOid(1));
    }

    /**
     * Repeating test010; this time with Lead10 present. So we are approving an assignment of single security-sensitive role (Role1),
     * that induces another security-sensitive role (Role10). Because of current implementation constraints, only the first assignment
     * should be brought to approval.
     */
    @Test
    public void test030AddRole1AssignmentAgain() throws Exception {
        login(userAdministrator);

        importLead10(getTestTask(), getTestOperationResult());

        executeAssignRole1(userJackOid, false, false, USER_LEAD1.oid, null, false);
    }

    /**
     * The same as above, but with immediate execution.
     */
    @Test
    public void test040AddRole1AssignmentImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRole1(userJackOid, true, false, USER_LEAD1.oid, null, false);
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

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(false, false, false, false);
    }

    /**
     * The same as above, but with immediate execution.
     */
    @Test
    public void test052AddRoles123AssignmentNNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
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

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(false, true, false, false);
    }

    @Test
    public void test062AddRoles123AssignmentYNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
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

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(false, true, true, true);
    }

    @Test
    public void test072AddRoles123AssignmentYYYImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(true, true, true, true);
    }

    @Test   // MID-4355
    public void test100AddCreateDelegation() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        task.setOwner(userAdministrator);
        OperationResult result = getTestOperationResult();

        // WHEN
        assignDeputy(USER_JACK_DEPUTY.oid, userJackOid, a -> {
            //a.beginLimitTargetContent().allowTransitive(true);
        }, task, result);

        // THEN
        PrismObject<UserType> deputy = getUser(USER_JACK_DEPUTY.oid);
        display("deputy after", deputy);

        result.computeStatus();
        assertSuccess(result);
        assertAssignedDeputy(deputy, userJackOid);
    }

    /**
     * Assigning Role1 with two deputies present. (But approved by the delegator.)
     */
    @Test
    public void test130AddRole1aAssignmentWithDeputy() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        importLead1Deputies(task, getTestOperationResult());

        unassignAllRoles(userJackOid);
        executeAssignRole1(userJackOid, false, true, USER_LEAD1.oid, null, false);
    }

    /**
     * Assigning Role1 with two deputies present. (Approved by one of the deputies.)
     */
    @Test
    public void test132AddRole1aAssignmentWithDeputyApprovedByDeputy1() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRole1(userJackOid, false, true, USER_LEAD1_DEPUTY_1.oid, null, false);
    }

    @Test(enabled = false)
    public void test150AddRole1ApproverAssignment() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRole1(userJackOid, false, true, USER_LEAD1.oid, SchemaConstants.ORG_APPROVER, false);
    }

    @Test
    public void test200AddRole1AssignmentToDraftUser() throws Exception {
        login(userAdministrator);

        executeAssignRole1(USER_DRAFT.oid, false, true, USER_LEAD1.oid, null, false);
    }

    // in memory tracing is required to check for MID-5814
    private OperationResult executeAssignRole1(String userOid, boolean immediate, boolean deputiesOfLeadOneSeeItems, String approverOid, QName relation,
            boolean useInMemoryTracing) throws Exception {
        PrismObject<UserType> user = getUser(userOid);
        String userName = user.getName().getOrig();
        // @formatter:off
        ObjectDelta<UserType> delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(new AssignmentType(prismContext)
                        .targetRef(getRoleOid(1), RoleType.COMPLEX_TYPE, relation))
                .asObjectDelta(userOid);
        // @formatter:on
        return executeTest2(new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return user.clone();
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() {
                return delta.clone();
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
                return singletonList(delta.clone());
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return prismContext.deltaFactory().object()
                        .createModifyDelta(user.getOid(), Collections.emptyList(), UserType.class);
            }

            @Override
            protected String getObjectOid() {
                return user.getOid();
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return singletonList(new ExpectedTask(getRoleOid(1), "Assigning role \"" +
                        getRoleName(1) + "\" to user \"" + userName + "\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                ExpectedTask expTask = getExpectedTasks().get(0);
                return singletonList(new ExpectedWorkItem(USER_LEAD1.oid, getRoleOid(1), expTask));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                if (number == 1) {
                    if (yes) {
                        assertAssignedRole(userOid, getRoleOid(1), result);
                        checkAuditRecords(createResultMap(getRoleOid(1), WorkflowResult.APPROVED));
                        checkUserApprovers(userOid, singletonList(approverOid), result);
                    } else {
                        assertNotAssignedRole(userOid, getRoleOid(1), result);
                    }
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                assertActiveWorkItems(USER_LEAD1.oid, 1);
                if (lead1DeputiesLoaded || deputiesOfLeadOneSeeItems) {
                    assertActiveWorkItems(USER_LEAD1_DEPUTY_1.oid, deputiesOfLeadOneSeeItems ? 1 : 0);
                    assertActiveWorkItems(USER_LEAD1_DEPUTY_2.oid, deputiesOfLeadOneSeeItems ? 1 : 0);
                }
                checkTargetOid(caseWorkItem, getRoleOid(1));
                login(getUser(approverOid));
                return true;
            }

            @Override
            public void setTracing(Task opTask) {
                if (useInMemoryTracing) {
                    opTask.addTracingRequest(TracingRootType.CLOCKWORK_RUN);
                    opTask.addTracingRequest(TracingRootType.WORKFLOW_OPERATION);
                    opTask.setTracingProfile(
                            new TracingProfileType(prismContext)
                                    .createTraceFile(false)
                                    .collectLogEntries(true)
                    );
                }
            }
        }, 1, immediate);
    }

    private List<PrismReferenceValue> getPotentialAssignees(PrismObject<UserType> user) {
        List<PrismReferenceValue> rv = new ArrayList<>();
        rv.add(ObjectTypeUtil.createObjectRef(user, prismContext).asReferenceValue());
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

    private void executeAssignRoles123ToJack(boolean immediate, boolean approve1, boolean approve2, boolean approve3) throws Exception {
        String testName = getTestNameShort();
        PrismObject<UserType> jack = getUser(userJackOid);
        ObjectDelta<UserType> addRole1Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> addRole2Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(2), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> addRole3Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(3), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> addRole4Delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(4), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> changeDescriptionDelta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(testName)
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil
                .summarize(addRole1Delta, addRole2Delta, addRole3Delta, addRole4Delta, changeDescriptionDelta);
        ObjectDelta<UserType> delta0 = ObjectDeltaCollectionsUtil.summarize(addRole4Delta, changeDescriptionDelta);
        String originalDescription = getUser(userJackOid).asObjectable().getDescription();
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
                return Arrays.asList(approve1, approve2, approve3);
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
                        new ExpectedTask(getRoleOid(1), "Assigning role \"" + getRoleName(1) + "\" to user \"jack\""),
                        new ExpectedTask(getRoleOid(2), "Assigning role \"" + getRoleName(2) + "\" to user \"jack\""),
                        new ExpectedTask(getRoleOid(3), "Assigning role \"" + getRoleName(3) + "\" to user \"jack\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                List<ExpectedTask> expTasks = getExpectedTasks();
                return Arrays.asList(
                        new ExpectedWorkItem(USER_LEAD1.oid, getRoleOid(1), expTasks.get(0)),
                        new ExpectedWorkItem(USER_LEAD2.oid, getRoleOid(2), expTasks.get(1)),
                        new ExpectedWorkItem(USER_LEAD3.oid, getRoleOid(3), expTasks.get(2))
                );
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                switch (number) {
                    case 0:
                        if (yes) {
                            assertUserProperty(userJackOid, UserType.F_DESCRIPTION, testName);
                            assertAssignedRole(userJackOid, getRoleOid(4), result);
                        } else {
                            if (originalDescription != null) {
                                assertUserProperty(userJackOid, UserType.F_DESCRIPTION, originalDescription);
                            } else {
                                assertUserNoProperty(userJackOid, UserType.F_DESCRIPTION);
                            }
                            assertNotAssignedRole(userJackOid, getRoleOid(4), result);
                        }
                        break;
                    case 1:
                    case 2:
                    case 3:
                        if (yes) {
                            assertAssignedRole(userJackOid, getRoleOid(number), result);
                        } else {
                            assertNotAssignedRole(userJackOid, getRoleOid(number), result);
                        }
                        break;

                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                String targetOid = getTargetOid(caseWorkItem);
                if (getRoleOid(1).equals(targetOid)) {
                    login(getUser(USER_LEAD1.oid));
                    return approve1;
                } else if (getRoleOid(2).equals(targetOid)) {
                    login(getUser(USER_LEAD2.oid));
                    return approve2;
                } else if (getRoleOid(3).equals(targetOid)) {
                    login(getUser(USER_LEAD3.oid));
                    return approve3;
                } else {
                    throw new IllegalStateException("Unexpected approval request for " + targetOid);
                }
            }
        }, 3, immediate);
    }

    @Test
    public void test300ApprovalAndEnforce() throws Exception {
        // ignored by default (put here so that zzzMarkAsNotInitialized() will be executed after this one!)
    }

    private void importLead10(Task task, OperationResult result) throws Exception {
        addAndRecomputeUser(USER_LEAD10.file, task, result);
    }

    private void importLead1Deputies(Task task, OperationResult result) throws Exception {
        addAndRecomputeUser(USER_LEAD1_DEPUTY_1.file, task, result);
        addAndRecomputeUser(USER_LEAD1_DEPUTY_2.file, task, result);
        lead1DeputiesLoaded = true;
    }
}
