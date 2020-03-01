/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.assignments;

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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.ExpectedTask;
import com.evolveum.midpoint.wf.impl.ExpectedWorkItem;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Testing approvals of role assignments: create/delete assignment, potentially for more roles and combined with other operations.
 * Testing also with deputies specified.
 *
 * Subclasses provide specializations regarding ways how rules and/or approvers are attached to roles.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractTestAssignmentApproval extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(AbstractTestAssignmentApproval.class);

    static final File TEST_RESOURCE_DIR = new File("src/test/resources/assignments");

    private static final File METAROLE_DEFAULT_FILE = new File(TEST_RESOURCE_DIR, "metarole-default.xml");

    private static final File ROLE_ROLE1_FILE = new File(TEST_RESOURCE_DIR, "role-role1.xml");
    private static final File ROLE_ROLE1B_FILE = new File(TEST_RESOURCE_DIR, "role-role1b.xml");
    private static final File ROLE_ROLE2_FILE = new File(TEST_RESOURCE_DIR, "role-role2.xml");
    private static final File ROLE_ROLE2B_FILE = new File(TEST_RESOURCE_DIR, "role-role2b.xml");
    private static final File ROLE_ROLE3_FILE = new File(TEST_RESOURCE_DIR, "role-role3.xml");
    private static final File ROLE_ROLE3B_FILE = new File(TEST_RESOURCE_DIR, "role-role3b.xml");
    private static final File ROLE_ROLE4_FILE = new File(TEST_RESOURCE_DIR, "role-role4.xml");
    private static final File ROLE_ROLE4B_FILE = new File(TEST_RESOURCE_DIR, "role-role4b.xml");
    private static final File ROLE_ROLE10_FILE = new File(TEST_RESOURCE_DIR, "role-role10.xml");
    private static final File ROLE_ROLE10B_FILE = new File(TEST_RESOURCE_DIR, "role-role10b.xml");
    private static final File ROLE_ROLE15_FILE = new File(TEST_RESOURCE_DIR, "role-role15.xml");

    private static final File USER_JACK_DEPUTY_FILE = new File(TEST_RESOURCE_DIR, "user-jack-deputy.xml");        // delegation is created only when needed
    private static final File USER_LEAD1_FILE = new File(TEST_RESOURCE_DIR, "user-lead1.xml");
    private static final File USER_LEAD1_DEPUTY_1_FILE = new File(TEST_RESOURCE_DIR, "user-lead1-deputy1.xml");
    private static final File USER_LEAD1_DEPUTY_2_FILE = new File(TEST_RESOURCE_DIR, "user-lead1-deputy2.xml");
    private static final File USER_LEAD2_FILE = new File(TEST_RESOURCE_DIR, "user-lead2.xml");
    private static final File USER_LEAD3_FILE = new File(TEST_RESOURCE_DIR, "user-lead3.xml");
    private static final File USER_LEAD10_FILE = new File(TEST_RESOURCE_DIR, "user-lead10.xml");
    private static final File USER_LEAD15_FILE = new File(TEST_RESOURCE_DIR, "user-lead15.xml");

    String roleRole1Oid;
    String roleRole1bOid;
    String roleRole2Oid;
    String roleRole2bOid;
    String roleRole3Oid;
    String roleRole3bOid;
    String roleRole4Oid;
    String roleRole4bOid;
    String roleRole10Oid;
    String roleRole10bOid;
    String roleRole15Oid;

    private String userJackDeputyOid;
    private String userLead1Oid;
    private String userLead1Deputy1Oid;
    private String userLead1Deputy2Oid;
    private String userLead2Oid;
    private String userLead3Oid;

    protected abstract String getRoleOid(int number);
    protected abstract String getRoleName(int number);

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(METAROLE_DEFAULT_FILE, initResult);

        roleRole1Oid = repoAddObjectFromFile(ROLE_ROLE1_FILE, initResult).getOid();
        roleRole1bOid = repoAddObjectFromFile(ROLE_ROLE1B_FILE, initResult).getOid();
        roleRole2Oid = repoAddObjectFromFile(ROLE_ROLE2_FILE, initResult).getOid();
        roleRole2bOid = repoAddObjectFromFile(ROLE_ROLE2B_FILE, initResult).getOid();
        roleRole3Oid = repoAddObjectFromFile(ROLE_ROLE3_FILE, initResult).getOid();
        roleRole3bOid = repoAddObjectFromFile(ROLE_ROLE3B_FILE, initResult).getOid();
        roleRole4Oid = repoAddObjectFromFile(ROLE_ROLE4_FILE, initResult).getOid();
        roleRole4bOid = repoAddObjectFromFile(ROLE_ROLE4B_FILE, initResult).getOid();
        roleRole10Oid = repoAddObjectFromFile(ROLE_ROLE10_FILE, initResult).getOid();
        roleRole10bOid = repoAddObjectFromFile(ROLE_ROLE10B_FILE, initResult).getOid();
        roleRole15Oid = repoAddObjectFromFile(ROLE_ROLE15_FILE, initResult).getOid();

        userJackDeputyOid = repoAddObjectFromFile(USER_JACK_DEPUTY_FILE, initResult).getOid();
        userLead1Oid = addAndRecomputeUser(USER_LEAD1_FILE, initTask, initResult);
        userLead2Oid = addAndRecomputeUser(USER_LEAD2_FILE, initTask, initResult);
        userLead3Oid = addAndRecomputeUser(USER_LEAD3_FILE, initTask, initResult);
        addAndRecomputeUser(USER_LEAD15_FILE, initTask, initResult);
        // LEAD10 will be imported later!
    }

    /**
     * The simplest case: addition of an assignment of single security-sensitive role (Role1).
     * Although it induces Role10 membership, it is not a problem, as Role10 approver (Lead10) is not imported yet.
     * (Even if it was, Role10 assignment would not be approved, see test030.)
     */
    @Test
    public void test010AddRole1Assignment() throws Exception {
        login(userAdministrator);

        OperationResult result = executeAssignRole1ToJack(false, false, null, null, true);

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
        OperationResult result = getTestResult();

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
        assertNotAssignedRole(getUser(userJackOid), getRoleOid(1), task, result);
    }

    /**
     * Repeating test010; this time with Lead10 present. So we are approving an assignment of single security-sensitive role (Role1),
     * that induces another security-sensitive role (Role10). Because of current implementation constraints, only the first assignment
     * should be brought to approval.
     */
    @Test
    public void test030AddRole1AssignmentAgain() throws Exception {
        login(userAdministrator);

        importLead10(getTestTask(), getTestResult());

        executeAssignRole1ToJack(false, false, null, null, false);
    }

    /**
     * The same as above, but with immediate execution.
     */
    @Test
    public void test040AddRole1AssignmentImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRole1ToJack(true, false, null, null, false);
    }

    /**
     * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
     * are not to be approved.
     *
     * Decisions for roles 1-3 are rejected.
     */
    @Test
    public void test050AddRoles123AssignmentNNN() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(null, false, false, false, false);
    }

    /**
     * The same as above, but with immediate execution.
     */
    @Test
     public void test052AddRoles123AssignmentNNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(null, true, false, false, false);
    }

    /**
     * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
     * are not to be approved.
     *
     * Decision for role 1 is accepted.
     */
    @Test
    public void test060AddRoles123AssignmentYNN() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(null, false, true, false, false);
    }

    @Test
    public void test062AddRoles123AssignmentYNNImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(null, true, true, false, false);
    }

    /**
     * Attempt to assign roles 1, 2, 3, 4 along with changing description. Assignment of role 4 and description change
     * are not to be approved.
     *
     * Decisions for roles 1-3 are accepted.
     */
    @Test
    public void test070AddRoles123AssignmentYYY() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(null, false, true, true, true);
    }

    @Test
    public void test072AddRoles123AssignmentYYYImmediate() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRoles123ToJack(null, true, true, true, true);
    }

    @Test   // MID-4355
    public void test100AddCreateDelegation() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        task.setOwner(userAdministrator);
        OperationResult result = getTestResult();

        // WHEN
        assignDeputy(userJackDeputyOid, userJackOid, a -> {
            //a.beginLimitTargetContent().allowTransitive(true);
        }, task, result);

        // THEN
        PrismObject<UserType> deputy = getUser(userJackDeputyOid);
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
        importLead1Deputies(task, getTestResult());

        unassignAllRoles(userJackOid);
        executeAssignRole1ToJack(false, true, null, null, false);
    }

    /**
     * Assigning Role1 with two deputies present. (Approved by one of the deputies.)
     */
    @Test
    public void test132AddRole1aAssignmentWithDeputyApprovedByDeputy1() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRole1ToJack(false, true, userLead1Deputy1Oid, null, false);
    }

    @Test(enabled = false)
    public void test150AddRole1ApproverAssignment() throws Exception {
        login(userAdministrator);

        unassignAllRoles(userJackOid);
        executeAssignRole1ToJack(false, true, null, SchemaConstants.ORG_APPROVER, false);
    }

    // in memory tracing is required to check for MID-5814
    private OperationResult executeAssignRole1ToJack(boolean immediate, boolean deputy, String approverOid, QName relation,
            boolean useInMemoryTracing) throws Exception {
        String testName = getTestNameShort();
        PrismObject<UserType> jack = getUser(userJackOid);
        AssignmentType assignment = createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext);
        assignment.getTargetRef().setRelation(relation);
        ObjectDelta<UserType> addRole1Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(assignment)
                .asObjectDelta(userJackOid);
        String realApproverOid = approverOid != null ? approverOid : userLead1Oid;
        return executeTest2(testName, new TestDetails2<UserType>() {
            @Override
            protected PrismObject<UserType> getFocus(OperationResult result) {
                return jack.clone();
            }

            @Override
            protected ObjectDelta<UserType> getFocusDelta() {
                return addRole1Delta.clone();
            }

            @Override
            protected int getNumberOfDeltasToApprove() {
                return 1;
            }

            @Override
            protected List<Boolean> getApprovals() {
                return Collections.singletonList(true);
            }

            @Override
            protected List<ObjectDelta<UserType>> getExpectedDeltasToApprove() {
                return Collections.singletonList(addRole1Delta.clone());
            }

            @Override
            protected ObjectDelta<UserType> getExpectedDelta0() {
                return prismContext.deltaFactory().object()
                        .createModifyDelta(jack.getOid(), Collections.emptyList(), UserType.class);
            }

            @Override
            protected String getObjectOid() {
                return jack.getOid();
            }

            @Override
            protected List<ExpectedTask> getExpectedTasks() {
                return Collections.singletonList(new ExpectedTask(getRoleOid(1), "Assigning role \"" + getRoleName(1) + "\" to user \"jack\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                ExpectedTask expTask = getExpectedTasks().get(0);
                return Collections.singletonList(new ExpectedWorkItem(userLead1Oid, getRoleOid(1), expTask));
            }

            @Override
            protected void assertDeltaExecuted(int number, boolean yes, Task opTask, OperationResult result) throws Exception {
                if (number == 1) {
                    if (yes) {
                        assertAssignedRole(userJackOid, getRoleOid(1), result);
                        checkAuditRecords(createResultMap(getRoleOid(1), WorkflowResult.APPROVED));
                        checkUserApprovers(userJackOid, Collections.singletonList(realApproverOid), result);
                    } else {
                        assertNotAssignedRole(userJackOid, getRoleOid(1), opTask, result);
                    }
                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                assertActiveWorkItems(userLead1Oid, 1);
                assertActiveWorkItems(userLead1Deputy1Oid, deputy ? 1 : 0);
                assertActiveWorkItems(userLead1Deputy2Oid, deputy ? 1 : 0);
                checkTargetOid(caseWorkItem, getRoleOid(1));
                login(getUser(realApproverOid));
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

    private void executeAssignRoles123ToJack(String testNameExplicit, boolean immediate, boolean approve1, boolean approve2, boolean approve3) throws Exception {
        String testName = testNameExplicit != null ? testNameExplicit : getTestNameShort();
        PrismObject<UserType> jack = getUser(userJackOid);
        ObjectDelta<UserType> addRole1Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(1), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> addRole2Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(2), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> addRole3Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(3), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> addRole4Delta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(createAssignmentTo(getRoleOid(4), ObjectTypes.ROLE, prismContext))
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> changeDescriptionDelta = prismContext
                .deltaFor(UserType.class)
                .item(UserType.F_DESCRIPTION).replace(testName)
                .asObjectDelta(userJackOid);
        ObjectDelta<UserType> primaryDelta = ObjectDeltaCollectionsUtil
                .summarize(addRole1Delta, addRole2Delta, addRole3Delta, addRole4Delta, changeDescriptionDelta);
        ObjectDelta<UserType> delta0 = ObjectDeltaCollectionsUtil.summarize(addRole4Delta, changeDescriptionDelta);
        String originalDescription = getUser(userJackOid).asObjectable().getDescription();
        executeTest2(testName, new TestDetails2<UserType>() {
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
                        new ExpectedTask(getRoleOid(1), "Assigning role \""+getRoleName(1)+"\" to user \"jack\""),
                        new ExpectedTask(getRoleOid(2), "Assigning role \""+getRoleName(2)+"\" to user \"jack\""),
                        new ExpectedTask(getRoleOid(3), "Assigning role \""+getRoleName(3)+"\" to user \"jack\""));
            }

            @Override
            protected List<ExpectedWorkItem> getExpectedWorkItems() {
                List<ExpectedTask> expTasks = getExpectedTasks();
                return Arrays.asList(
                        new ExpectedWorkItem(userLead1Oid, getRoleOid(1), expTasks.get(0)),
                        new ExpectedWorkItem(userLead2Oid, getRoleOid(2), expTasks.get(1)),
                        new ExpectedWorkItem(userLead3Oid, getRoleOid(3), expTasks.get(2))
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
                            assertNotAssignedRole(userJackOid, getRoleOid(4), opTask, result);
                        }
                        break;
                    case 1:
                    case 2:
                    case 3:
                    if (yes) {
                        assertAssignedRole(userJackOid, getRoleOid(number), result);
                    } else {
                        assertNotAssignedRole(userJackOid, getRoleOid(number), opTask, result);
                    }
                    break;

                }
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                String targetOid = getTargetOid(caseWorkItem);
                if (getRoleOid(1).equals(targetOid)) {
                    login(getUser(userLead1Oid));
                    return approve1;
                } else if (getRoleOid(2).equals(targetOid)) {
                    login(getUser(userLead2Oid));
                    return approve2;
                } else if (getRoleOid(3).equals(targetOid)) {
                    login(getUser(userLead3Oid));
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
        addAndRecomputeUser(USER_LEAD10_FILE, task, result);
    }

    private void importLead1Deputies(Task task, OperationResult result) throws Exception {
        userLead1Deputy1Oid = addAndRecomputeUser(USER_LEAD1_DEPUTY_1_FILE, task, result);
        userLead1Deputy2Oid = addAndRecomputeUser(USER_LEAD1_DEPUTY_2_FILE, task, result);
    }
}
