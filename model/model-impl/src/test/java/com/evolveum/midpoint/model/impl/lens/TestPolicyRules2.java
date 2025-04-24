/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.test.util.MidPointAsserts.assertSerializable;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.test.TestObject;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests some of the "new" policy rules (state, hasAssignment).
 * Moved out of TestPolicyRules to keep the tests of reasonable size.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules2 extends AbstractLensTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens/policy");

    private static final File ROLE_PERSON_FILE = new File(TEST_DIR, "role-person.xml");
    private static final File ROLE_TEMPORARY_FILE = new File(TEST_DIR, "role-temporary.xml");
    private static final File ROLE_STUDENT_FILE = new File(TEST_DIR, "role-student.xml");

    static final TestObject<RoleType> ROLE_RUM_DRINKER = TestObject.file(
            TEST_DIR, "role-rum-drinker.xml", "d9904984-2119-11f0-8913-bb00c5bbe9a2");

    static final TestObject<RoleType> ROLE_RUM_SPIRIT = TestObject.file(
            TEST_DIR, "role-rum-spirit.xml", "a6a9a550-211a-11f0-9706-b3947043966d");

    static final TestObject<RoleType> ROLE_RUM_MANAGER = TestObject.file(
            TEST_DIR, "role-rum-manager.xml", "9d18d638-2119-11f0-b735-43f4b26a370d");

    private static final File USER_JOE_FILE = new File(TEST_DIR, "user-joe.xml");
    private static final File USER_FRANK_FILE = new File(TEST_DIR, "user-frank.xml");
    private static final File USER_PETER_FILE = new File(TEST_DIR, "user-peter.xml");

    private static final File ROLE_CHAINED_REFERENCES_FILE = new File(TEST_DIR, "role-chained-references.xml");
    private static final File ROLE_CYCLIC_REFERENCES_FILE = new File(TEST_DIR, "role-cyclic-references.xml");
    private static final File ROLE_UNRESOLVABLE_REFERENCES_FILE = new File(TEST_DIR, "role-unresolvable-references.xml");
    private static final File ROLE_AMBIGUOUS_REFERENCE_FILE = new File(TEST_DIR, "role-ambiguous-reference.xml");
    private static final File ROLE_IMMUTABLE_INDUCEMENTS_FILE = new File(TEST_DIR, "role-immutable-inducements.xml");
    private static final File ROLE_NO_INDUCEMENTS_ADD_DELETE_FILE = new File(TEST_DIR, "role-no-inducements-add-delete.xml");
    private static final File ROLE_NO_INDUCEMENTS_ADD_DELETE_VIA_EXPRESSION_FILE = new File(TEST_DIR, "role-no-inducements-add-delete-via-expression.xml");

    private static final int STUDENT_TARGET_RULES = 6; // one is global
    private static final int STUDENT_FOCUS_RULES = 21;

    private static final String ACTIVITY_DESCRIPTION = "PROJECTOR (test)";

    private String roleImmutableInducementsOid;
    private String roleNoInducementsAddDeleteOid;
    private String roleNoInducementsAddDeleteViaExpressionOid;
    private String roleStudentOid;
    private String userJoeOid;
    private String userFrankOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        setDefaultUserTemplate(USER_TEMPLATE_OID);

        roleImmutableInducementsOid = repoAddObjectFromFile(ROLE_IMMUTABLE_INDUCEMENTS_FILE, initResult).getOid();  // using repo because the inducement is present in the object

        roleNoInducementsAddDeleteOid = addAndRecompute(ROLE_NO_INDUCEMENTS_ADD_DELETE_FILE, initTask, initResult);
        roleNoInducementsAddDeleteViaExpressionOid = addAndRecompute(ROLE_NO_INDUCEMENTS_ADD_DELETE_VIA_EXPRESSION_FILE, initTask, initResult);
        addAndRecompute(ROLE_PERSON_FILE, initTask, initResult);
        addAndRecompute(ROLE_TEMPORARY_FILE, initTask, initResult);
        roleStudentOid = addAndRecompute(ROLE_STUDENT_FILE, initTask, initResult);
        userJoeOid = addAndRecompute(USER_JOE_FILE, initTask, initResult);
        userFrankOid = addAndRecompute(USER_FRANK_FILE, initTask, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        ObjectFilter studentFilter = prismContext.queryFor(RoleType.class).id(roleStudentOid).buildFilter();

        GlobalPolicyRuleType hasStudentDisabled = new GlobalPolicyRuleType()
                .name("has-student-assignment-disabled")
                .focusSelector(new ObjectSelectorType())
                .targetSelector(new ObjectSelectorType()
                        .type(RoleType.COMPLEX_TYPE)
                        .filter(prismContext.getQueryConverter().createSearchFilterType(studentFilter)))
                .beginPolicyConstraints()
                .beginHasAssignment()
                .name("student-assignment-disabled")
                .targetRef(roleStudentOid, RoleType.COMPLEX_TYPE)
                .enabled(false)
                .<PolicyConstraintsType>end()
                .<GlobalPolicyRuleType>end()
                .evaluationTarget(PolicyRuleEvaluationTargetType.ASSIGNMENT)
                .policyActions(new PolicyActionsType());

        repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                prismContext.deltaFor(SystemConfigurationType.class)
                        .item(SystemConfigurationType.F_GLOBAL_POLICY_RULE).add(hasStudentDisabled)
                        .asItemDeltas(), initResult);

        initTestObjects(
                initTask, initResult,
                ROLE_RUM_DRINKER,
                ROLE_RUM_SPIRIT,
                ROLE_RUM_MANAGER
                );

        InternalMonitor.reset();
        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    /**
     * Jack's cost center does not match 1900. So these constraints should not trigger.
     */
    @Test
    public void test100JackAttemptAssignRoleStudent() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, roleStudentOid);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpPolicyRules(context);
        //dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 7);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 2);

        assertSerializable(context);
    }

    /**
     * Joe's cost center is 1900. So all 1900-related constraints (old/current/new) should trigger.
     */
    @Test
    public void test110JoeAttemptAssignRoleStudent() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, userJoeOid, result);
        addModificationToContextAssignRole(context, userJoeOid, roleStudentOid);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        //dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 10);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 4);

        assertSerializable(context);
    }

    /**
     * Jacks's cost center is set to be 1900. So 1900/new constraint should trigger. But not 1900/current nor 1900/old.
     * Also validTo constraint should trigger.
     */
    @Test
    public void test120JackAttemptToMoveTo1900AndAssignRoleStudent() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        AssignmentType assignment = ObjectTypeUtil.createAssignmentTo(roleStudentOid, ObjectTypes.ROLE);
        assignment.beginActivation().validTo("2099-01-01T00:00:00");
        context.getFocusContext().addToPrimaryDelta(prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(assignment)
                .item(UserType.F_COST_CENTER).replace("1900")
                .asObjectDelta(USER_JACK_OID));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        //dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 3);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 9);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 3);

        assertSerializable(context);
    }

    /**
     * Assign Rum drinker role to Jack.
     * This role has assignment rule that should always trigger, even now.
     * #10663
     */
    @Test
    public void test121JackAttemptAssignRoleRumDrinker() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_RUM_DRINKER.oid);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpPolicyRules(context);
        //dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, 1);
        assertTargetTriggers(context, null, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 0);

        assertEvaluatedFocusPolicyRules(context, 0);
        assertFocusTriggers(context, null, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 0);

        assertSerializable(context);
    }

    /**
     * Assign Rum manager role to Jack.
     * Both induced roles has assignment rules that should trigger.
     * #10663
     */
    @Test
    public void test122JackAttemptAssignRoleRumManager() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_RUM_MANAGER.oid);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpPolicyRules(context);
        //dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, 2);
        assertTargetTriggers(context, null, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 0);

        assertEvaluatedFocusPolicyRules(context, 0);
        assertFocusTriggers(context, null, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 0);

        assertSerializable(context);
    }

    /**
     * Assign Rum spirit role to Jack.
     * This roles has assignment rule that should trigger only when assigned indirectly.
     * It should not trigger in this case.
     * #10663
     */
    @Test
    public void test123JackAttemptAssignRoleRumSpirit() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_RUM_SPIRIT.oid);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        assertSuccess(result);

        dumpPolicyRules(context);
        //dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, 1);
        assertTargetTriggers(context, null, 0);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 0);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 0);

        assertEvaluatedFocusPolicyRules(context, 0);
        assertFocusTriggers(context, null, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 0);

        assertSerializable(context);
    }

    /**
     * This time we really execute the operation. Clockwork will be run iteratively.
     * Because of these iterations, both 1900/current and 1900/new (but not 1900/old) constraints will trigger.
     * However, 1900/current will trigger only in secondary phase (wave 1). This is important because of approvals:
     * such triggered actions will not be recognized.
     */
    @Test
    public void test130JackMoveTo1900AndAssignRoleStudent() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        AssignmentType assignment = ObjectTypeUtil.createAssignmentTo(roleStudentOid, ObjectTypes.ROLE);
        assignment.beginActivation().validTo("2099-01-01T00:00:00");
        context.getFocusContext().addToPrimaryDelta(prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(assignment)
                .item(UserType.F_COST_CENTER).replace("1900")
                .asObjectDelta(USER_JACK_OID));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();

        clockwork.run(context, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 3);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 9);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 3);

        assertSerializable(context);
    }

    @Test
    public void test135JackChangeValidTo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Long assignmentId = getUser(USER_JACK_OID).asObjectable().getAssignment().get(0).getId();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        context.getFocusContext().addToPrimaryDelta(prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO).replace()
                .asObjectDelta(USER_JACK_OID));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();

        // cannot run the clockwork as in the secondary state the deltas are no longer considered (!)
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 3);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 9);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 3);

        assertSerializable(context);
    }

    @Test
    public void test140JackNoChange() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);        // no assignment change => no triggering of ASSIGNMENT constraint (is this correct?)
        assertTargetTriggers(context, null, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 0);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 9);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 3);

        assertSerializable(context);
    }

    @Test
    public void test142JackNoChangeButTaskExists() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        TaskType approvalTask = prismContext.createObjectable(TaskType.class)
                .name("approval task")
                .executionState(TaskExecutionStateType.WAITING)
                .schedulingState(TaskSchedulingStateType.WAITING)
                .ownerRef(userAdministrator.getOid(), UserType.COMPLEX_TYPE)
                .objectRef(USER_JACK_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT);
        String approvalTaskOid = taskManager.addTask(approvalTask.asPrismObject(), result);
        System.out.println("Approval task OID = " + approvalTaskOid);

        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(USER_JACK_OID)
                .and().item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.WAITING)
                .build();
        SearchResultList<PrismObject<TaskType>> tasks = modelService
                .searchObjects(TaskType.class, query, null, task, result);
        display("Tasks for jack", tasks);

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 0);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 10);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 2);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 3);

        assertSerializable(context);
    }

    @Test
    public void test150FrankAttemptToAssignRoleStudentButDisabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, userFrankOid, result);
        context.getFocusContext().addToPrimaryDelta(prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(
                        ObjectTypeUtil.createAssignmentTo(roleStudentOid, ObjectTypes.ROLE)
                                .beginActivation()
                                .administrativeStatus(ActivationStatusType.DISABLED)
                                .<AssignmentType>end())
                .asObjectDelta(userFrankOid));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        //dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 3);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 6);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 0);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 3);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 2);

        assertSerializable(context);
    }

    @Test
    public void test160AttemptToAddPeter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithAddUserDelta(context, prismContext.parseObject(USER_PETER_FILE));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 9);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 3);

        assertSerializable(context);
    }

    @Test
    public void test170AddPeter() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<UserType> context = createUserLensContext();
        fillContextWithAddUserDelta(context, prismContext.parseObject(USER_PETER_FILE));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        clockwork.run(context, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
        assertTargetTriggers(context, null, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, "assignment-of-student");
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, "always-true");

        assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
        assertFocusTriggers(context, null, 9);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, "cc-1900");
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT,
                "has-person-assignment-indirect", "has-person-assignment-enabled",
                "has-student-assignment-enabled", "has-student-assignment");
        assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, "has-no-assignment-for-aaa");
        assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION,
                "cc-from-1900-false-true", "has-student-assignment-false-true",
                "has-student-assignment-false-any");

        assertSerializable(context);
    }

    /**
     * Test whether policy rules intended for users are not evaluated on the role itself.
     */
    @Test
    public void test180StudentRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithFocus(context, RoleType.class, roleStudentOid, result);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, ACTIVITY_DESCRIPTION, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, 0);
        assertTargetTriggers(context, null, 0);

        assertEvaluatedFocusPolicyRules(context, 5);
        assertFocusTriggers(context, null, 2);
        assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 2);

        assertSerializable(context);
    }

    @Test
    public void test200AddUnresolvable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithAddDelta(context, prismContext.parseObject(ROLE_UNRESOLVABLE_REFERENCES_FILE));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        when();
        try {
            clockwork.run(context, task, result);
            then();
            fail("unexpected success");
        } catch (ObjectNotFoundException e) {
            then();
            displayExpectedException(e);
            if (!e.getMessage().contains("No policy constraint named 'unresolvable' could be found")) {
                fail("Exception message was not as expected: " + e.getMessage());
            }
        }

        assertSerializable(context);
    }

    @Test
    public void test210AddCyclic() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithAddDelta(context, prismContext.parseObject(ROLE_CYCLIC_REFERENCES_FILE));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        try {
            clockwork.run(context, task, result);
            then();
            fail("unexpected success");
        } catch (SchemaException e) {
            then();
            displayExpectedException(e);
            if (!e.getMessage().contains("Trying to resolve cyclic reference to constraint")) {
                fail("Exception message was not as expected: " + e.getMessage());
            }
        }

        assertSerializable(context);
    }

    @Test
    public void test220AddChained() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithAddDelta(context, prismContext.parseObject(ROLE_CHAINED_REFERENCES_FILE));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        clockwork.run(context, task, result);

        then();
        displayDumpable("Output context", context);

        Map<String, EvaluatedPolicyRule> rules = new HashMap<>();
        forEvaluatedFocusPolicyRule(context, (r) -> {
            displayDumpable("rule", r);
            rules.put(r.getName(), r);
        });

        assertEquals("Wrong # of policy rules", 5, rules.size());
        EvaluatedPolicyRule rule1 = rules.get("rule1");
        assertNotNull("no rule1", rule1);
        PolicyConstraintsType pc1 = rule1.getPolicyConstraints();
        assertEquals(1, pc1.getAnd().size());
        PolicyConstraintsType pc1inner = pc1.getAnd().get(0);
        assertEquals("mod-description-and-riskLevel-and-inducement", pc1inner.getName());
        assertEquals("mod-riskLevel-and-inducement", pc1inner.getAnd().get(0).getName());
        assertEquals(2, pc1inner.getAnd().get(0).getModification().size());

        EvaluatedPolicyRule rule2 = rules.get("rule2");
        assertNotNull("no rule2", rule2);
        PolicyConstraintsType pc2 = rule2.getPolicyConstraints();
        assertEquals("mod-description-and-riskLevel-and-inducement", pc2.getName());
        assertEquals("mod-riskLevel-and-inducement", pc2.getAnd().get(0).getName());
        assertEquals(2, pc2.getAnd().get(0).getModification().size());
        assertEquals("Constraints in rule1, rule2 are different", pc1inner, pc2);

        EvaluatedPolicyRule rule3 = rules.get("rule3");
        assertNotNull("no rule3", rule3);
        PolicyConstraintsType pc3 = rule3.getPolicyConstraints();
        assertEquals("mod-riskLevel-and-inducement", pc3.getName());
        assertEquals(2, pc3.getModification().size());
        assertEquals("Constraints in rule2 and rule3 are different", pc2.getAnd().get(0), pc3);

        EvaluatedPolicyRule rule4 = rules.get("rule4");
        assertNotNull("no rule4", rule4);
        PolicyConstraintsType pc4 = rule4.getPolicyConstraints();
        assertEquals(1, pc4.getModification().size());
        assertEquals("mod-inducement", pc4.getModification().get(0).getName());

        assertSerializable(context);
    }

    @Test
    public void test230AddAmbiguous() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        LensContext<RoleType> context = createLensContext(RoleType.class);
        fillContextWithAddDelta(context, prismContext.parseObject(ROLE_AMBIGUOUS_REFERENCE_FILE));
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        try {
            clockwork.run(context, task, result);
            then();
            fail("unexpected success");
        } catch (SchemaException e) {
            then();
            displayExpectedException(e);
            if (!e.getMessage().contains("Conflicting definitions of 'constraint-B'")) {
                fail("Exception message was not as expected: " + e.getMessage());
            }
        }

        assertSerializable(context);
    }

    // MID-4270
    @Test
    public void test300ModifyInducement() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<RoleType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT, 1L, AssignmentType.F_DESCRIPTION).replace("hi")
                .asObjectDelta(roleImmutableInducementsOid);
        LensContext<RoleType> context = createLensContext(RoleType.class);
        context.createFocusContext().setPrimaryDelta(delta);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        try {
            clockwork.run(context, task, result);
            then();
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            then();
            displayExpectedException(e);
            if (!getTranslatedMessage(e).contains("Role \"Immutable inducements\" is to be modified")) {
                fail("Exception message was not as expected: " + getTranslatedMessage(e));
            }
        }

        assertSerializable(context);
    }

    // MID-4270
    @Test
    public void test310ModifyInducementPass() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<RoleType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT, 1L, AssignmentType.F_DESCRIPTION).replace("hi")
                .asObjectDelta(roleNoInducementsAddDeleteOid);
        LensContext<RoleType> context = createLensContext(RoleType.class);
        context.createFocusContext().setPrimaryDelta(delta);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        clockwork.run(context, task, result);
        then();
        result.computeStatus();
        assertSuccess("unexpected failure", result);

        assertSerializable(context);
    }

    // MID-4270
    @Test
    public void test320ModifyInducementPass2() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<RoleType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT, 1L, AssignmentType.F_DESCRIPTION).replace("hi")
                .asObjectDelta(roleNoInducementsAddDeleteViaExpressionOid);
        LensContext<RoleType> context = createLensContext(RoleType.class);
        context.createFocusContext().setPrimaryDelta(delta);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        clockwork.run(context, task, result);
        then();
        result.computeStatus();
        assertSuccess("unexpected failure", result);

        assertSerializable(context);
    }

    // MID-4270
    @Test
    public void test330AddInducement() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<RoleType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT).add(new AssignmentType().targetRef("1", OrgType.COMPLEX_TYPE))
                .asObjectDelta(roleNoInducementsAddDeleteOid);
        LensContext<RoleType> context = createLensContext(RoleType.class);
        context.createFocusContext().setPrimaryDelta(delta);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        try {
            clockwork.run(context, task, result);
            then();
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            then();
            displayExpectedException(e);
            if (!getTranslatedMessage(e).contains("Role \"No inducements add or delete\" is to be modified")) {
                fail("Exception message was not as expected: " + getTranslatedMessage(e));
            }
        }

        assertSerializable(context);
    }

    // MID-4270
    @Test
    public void test340AddInducementViaExpression() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<RoleType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT).replace(new AssignmentType().targetRef("1", OrgType.COMPLEX_TYPE))
                .asObjectDelta(roleNoInducementsAddDeleteViaExpressionOid);
        LensContext<RoleType> context = createLensContext(RoleType.class);
        context.createFocusContext().setPrimaryDelta(delta);
        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        try {
            clockwork.run(context, task, result);
            then();
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            then();
            displayExpectedException(e);
            if (!getTranslatedMessage(e).contains("Role \"No inducements add or delete (expression)\" is to be modified")) {
                fail("Exception message was not as expected: " + getTranslatedMessage(e));
            }
        }

        assertSerializable(context);
    }
}
