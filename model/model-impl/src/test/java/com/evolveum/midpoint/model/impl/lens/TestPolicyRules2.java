/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Tests some of the "new" policy rules (state, hasAssignment).
 * Moved out of TestPolicyRules to keep the tests of reasonable size.
 *
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules2 extends AbstractLensTest {

	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens/policy");

	protected static final File ROLE_PERSON_FILE = new File(TEST_DIR, "role-person.xml");
	protected static final File ROLE_TEMPORARY_FILE = new File(TEST_DIR, "role-temporary.xml");
	protected static final File ROLE_STUDENT_FILE = new File(TEST_DIR, "role-student.xml");
	protected static final File USER_JOE_FILE = new File(TEST_DIR, "user-joe.xml");
	protected static final File USER_FRANK_FILE = new File(TEST_DIR, "user-frank.xml");
	protected static final File USER_PETER_FILE = new File(TEST_DIR, "user-peter.xml");

	protected static final File ROLE_CHAINED_REFERENCES_FILE = new File(TEST_DIR, "role-chained-references.xml");
	protected static final File ROLE_CYCLIC_REFERENCES_FILE = new File(TEST_DIR, "role-cyclic-references.xml");
	protected static final File ROLE_UNRESOLVABLE_REFERENCES_FILE = new File(TEST_DIR, "role-unresolvable-references.xml");
	protected static final File ROLE_AMBIGUOUS_REFERENCE_FILE = new File(TEST_DIR, "role-ambiguous-reference.xml");

	private static final int STUDENT_TARGET_RULES = 5;          // one is global
	private static final int STUDENT_FOCUS_RULES = 21;

	private static final String ACTIVITY_DESCRIPTION = "PROJECTOR (test)";

	private String rolePersonOid;
	private String roleTemporaryOid;
	private String roleStudentOid;
	private String userJoeOid;
	private String userFrankOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		setDefaultUserTemplate(USER_TEMPLATE_OID);

		rolePersonOid = addAndRecompute(ROLE_PERSON_FILE, initTask, initResult);
		roleTemporaryOid = addAndRecompute(ROLE_TEMPORARY_FILE, initTask, initResult);
		roleStudentOid = addAndRecompute(ROLE_STUDENT_FILE, initTask, initResult);
		userJoeOid = addAndRecompute(USER_JOE_FILE, initTask, initResult);
		userFrankOid = addAndRecompute(USER_FRANK_FILE, initTask, initResult);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

		ObjectFilter studentFilter = QueryBuilder.queryFor(RoleType.class, prismContext).id(roleStudentOid).buildFilter();

		GlobalPolicyRuleType hasStudentDisabled = new GlobalPolicyRuleType(prismContext)
				.name("has-student-assignment-disabled")
				.focusSelector(new ObjectSelectorType(prismContext))
				.targetSelector(new ObjectSelectorType(prismContext)
						.type(RoleType.COMPLEX_TYPE)
						.filter(QueryConvertor.createSearchFilterType(studentFilter, prismContext)))
				.beginPolicyConstraints()
					.beginHasAssignment()
						.name("student-assignment-disabled")
						.targetRef(roleStudentOid, RoleType.COMPLEX_TYPE)
						.enabled(false)
					.<PolicyConstraintsType>end()
				.<GlobalPolicyRuleType>end()
				.evaluationTarget(PolicyRuleEvaluationTargetType.ASSIGNMENT)
				.policyActions(new PolicyActionsType(prismContext));

		repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
						.item(SystemConfigurationType.F_GLOBAL_POLICY_RULE).add(hasStudentDisabled)
						.asItemDeltas(), initResult);

		InternalMonitor.reset();
		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	/**
	 * Jack's cost center does not match 1900. So these constraints should not trigger.
	 */
	@Test
	public void test100JackAttemptAssignRoleStudent() throws Exception {
		final String TEST_NAME = "test100JackAttemptAssignRoleStudent";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addModificationToContextAssignRole(context, USER_JACK_OID, roleStudentOid);

		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

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
	}

	/**
	 * Joe's cost center is 1900. So all 1900-related constraints (old/current/new) should trigger.
	 */
	@Test
	public void test110JoeAttemptAssignRoleStudent() throws Exception {
		final String TEST_NAME = "test110JoeAttemptAssignRoleStudent";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, userJoeOid, result);
		addModificationToContextAssignRole(context, userJoeOid, roleStudentOid);

		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
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
	}

	/**
	 * Jacks's cost center is set to be 1900. So 1900/new constraint should trigger. But not 1900/current nor 1900/old.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void test120JackAttemptToMoveTo1900AndAssignRoleStudent() throws Exception {
		final String TEST_NAME = "test120JackAttemptToMoveTo1900AndAssignRoleStudent";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		context.getFocusContext().addPrimaryDelta((ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(roleStudentOid, ObjectTypes.ROLE, prismContext))
				.item(UserType.F_COST_CENTER).replace("1900")
				.asObjectDelta(USER_JACK_OID));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);
		//dumpPolicySituations(context);

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
	}

	/**
	 * This time we really execute the operation. Clockwork will be run iteratively.
	 * Because of these iterations, both 1900/current and 1900/new (but not 1900/old) constraints will trigger.
	 * However, 1900/current will trigger only in secondary phase (wave 1). This is important because of approvals:
	 * such triggered actions will not be recognized.
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void test130JackMoveTo1900AndAssignRoleStudent() throws Exception {
		final String TEST_NAME = "test130JackMoveTo1900AndAssignRoleStudent";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		context.getFocusContext().addPrimaryDelta((ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(roleStudentOid, ObjectTypes.ROLE, prismContext))
				.item(UserType.F_COST_CENTER).replace("1900")
				.asObjectDelta(USER_JACK_OID));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		clockwork.run(context, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
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

		assertAssignmentPolicySituation(context, roleStudentOid,
				SchemaConstants.MODEL_POLICY_SITUATION_ASSIGNMENT_MODIFIED,
				SchemaConstants.MODEL_POLICY_SITUATION_OBJECT_STATE);
		assertFocusPolicySituation(context,
				SchemaConstants.MODEL_POLICY_SITUATION_OBJECT_STATE,
				SchemaConstants.MODEL_POLICY_SITUATION_HAS_ASSIGNMENT,
				SchemaConstants.MODEL_POLICY_SITUATION_HAS_NO_ASSIGNMENT);
	}

	@Test
	public void test140JackNoChange() throws Exception {
		final String TEST_NAME = "test140JackNoChange";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
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
	}

	@Test
	public void test142JackNoChangeButTaskExists() throws Exception {
		final String TEST_NAME = "test142JackNoChangeButTaskExists";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		TaskType approvalTask = prismContext.createObjectable(TaskType.class)
				.name("approval task")
				.category(TaskCategory.WORKFLOW)
				.executionStatus(TaskExecutionStatusType.WAITING)
				.ownerRef(userAdministrator.getOid(), UserType.COMPLEX_TYPE)
				.objectRef(USER_JACK_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT);
		String approvalTaskOid = taskManager.addTask(approvalTask.asPrismObject(), result);
		System.out.println("Approval task OID = " + approvalTaskOid);

		ObjectQuery query = QueryBuilder.queryFor(TaskType.class, prismContext)
				.item(TaskType.F_OBJECT_REF).ref(USER_JACK_OID)
				.and().item(TaskType.F_CATEGORY).eq(TaskCategory.WORKFLOW)
				.and().item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING)
				.build();
		SearchResultList<PrismObject<TaskType>> tasks = modelService
				.searchObjects(TaskType.class, query, null, task, result);
		display("Tasks for jack", tasks);

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
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
	}


	@SuppressWarnings("unchecked")
	@Test
	public void test150FrankAttemptToAssignRoleStudentButDisabled() throws Exception {
		final String TEST_NAME = "test150FrankAttemptToAssignRoleStudentButDisabled";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithUser(context, userFrankOid, result);
		context.getFocusContext().addPrimaryDelta((ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(
						ObjectTypeUtil.createAssignmentTo(roleStudentOid, ObjectTypes.ROLE, prismContext)
								.beginActivation()
								.administrativeStatus(ActivationStatusType.DISABLED)
								.<AssignmentType>end())
				.asObjectDelta(userFrankOid));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
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
	}

	@Test
	public void test160AttemptToAddPeter() throws Exception {
		final String TEST_NAME = "test160AttemptToAddPeter";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithAddUserDelta(context, prismContext.parseObject(USER_PETER_FILE));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
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
	}

	@Test
	public void test170AddPeter() throws Exception {
		final String TEST_NAME = "test170AddPeter";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserLensContext();
		fillContextWithAddUserDelta(context, prismContext.parseObject(USER_PETER_FILE));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		clockwork.run(context, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);
		dumpPolicySituations(context);

		assertEvaluatedTargetPolicyRules(context, STUDENT_TARGET_RULES);
		assertTargetTriggers(context, null, 1);
		// Assignment situation is already gone (in second iteration)!
		// This is different from test130, where an assignment is being added (instead of whole user being added).
		// The difference is that when adding a user, its assignments (in wave 1) are in the zero set.
		// Whereas when modifying a user, its new assignments (in wave 1) are in the plus set.
		// This is to be solved somehow.
		// See MID-4126.
		assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 8);
		assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 1);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
		assertFocusTriggers(context, PolicyConstraintKindType.TRANSITION, 2);
		// for the same reason as in target rules, student assignment is caught in "true-true" transition situation
		// instead of "false-true" and "false-any" situations!

		// adapt the test after fixing MID-4126
		assertAssignmentPolicySituation(context, roleStudentOid,
				SchemaConstants.MODEL_POLICY_SITUATION_OBJECT_STATE);
		assertFocusPolicySituation(context,
				SchemaConstants.MODEL_POLICY_SITUATION_OBJECT_STATE,
				SchemaConstants.MODEL_POLICY_SITUATION_HAS_ASSIGNMENT,
				SchemaConstants.MODEL_POLICY_SITUATION_HAS_NO_ASSIGNMENT);
	}

	/**
	 * Test whether policy rules intended for users are not evaluated on the role itself.
	 */
	@Test
	public void test180StudentRecompute() throws Exception {
		final String TEST_NAME = "test180StudentRecompute";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<RoleType> context = createLensContext(RoleType.class);
		fillContextWithFocus(context, RoleType.class, roleStudentOid, result);
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		projector.project(context, ACTIVITY_DESCRIPTION, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		dumpPolicyRules(context);
		dumpPolicySituations(context);

		assertEvaluatedTargetPolicyRules(context, 0);
		assertTargetTriggers(context, null, 0);

		assertEvaluatedFocusPolicyRules(context, 4);
		assertFocusTriggers(context, null, 2);
		assertFocusTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 2);
	}

	@Test
	public void test200AddUnresolvable() throws Exception {
		final String TEST_NAME = "test200AddUnresolvable";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<RoleType> context = createLensContext(RoleType.class);
		fillContextWithAddDelta(context, prismContext.parseObject(ROLE_UNRESOLVABLE_REFERENCES_FILE));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			clockwork.run(context, task, result);
			TestUtil.displayThen(TEST_NAME);
			fail("unexpected success");
		} catch (ObjectNotFoundException e) {
			TestUtil.displayThen(TEST_NAME);
			System.out.println("Expected exception: " + e);
			e.printStackTrace(System.out);
			if (!e.getMessage().contains("No policy constraint named 'unresolvable' could be found")) {
				fail("Exception message was not as expected: " + e.getMessage());
			}
		}
	}

	@Test
	public void test210AddCyclic() throws Exception {
		final String TEST_NAME = "test210AddCyclic";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<RoleType> context = createLensContext(RoleType.class);
		fillContextWithAddDelta(context, prismContext.parseObject(ROLE_CYCLIC_REFERENCES_FILE));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			clockwork.run(context, task, result);
			TestUtil.displayThen(TEST_NAME);
			fail("unexpected success");
		} catch (SchemaException e) {
			TestUtil.displayThen(TEST_NAME);
			System.out.println("Expected exception: " + e);
			e.printStackTrace(System.out);
			if (!e.getMessage().contains("Trying to resolve cyclic reference to constraint")) {
				fail("Exception message was not as expected: " + e.getMessage());
			}
		}
	}

	@Test
	public void test220AddChained() throws Exception {
		final String TEST_NAME = "test220AddChained";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<RoleType> context = createLensContext(RoleType.class);
		fillContextWithAddDelta(context, prismContext.parseObject(ROLE_CHAINED_REFERENCES_FILE));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		clockwork.run(context, task, result);

		TestUtil.displayThen(TEST_NAME);
		display("Output context", context);

		Map<String,EvaluatedPolicyRule> rules = new HashMap<>();
		forEvaluatedFocusPolicyRule(context, (r) -> {
			display("rule", r);
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
	}

	@Test
	public void test230AddAmbiguous() throws Exception {
		final String TEST_NAME = "test230AddAmbiguous";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyRules2.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<RoleType> context = createLensContext(RoleType.class);
		fillContextWithAddDelta(context, prismContext.parseObject(ROLE_AMBIGUOUS_REFERENCE_FILE));
		display("Input context", context);

		assertFocusModificationSanity(context);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		try {
			clockwork.run(context, task, result);
			TestUtil.displayThen(TEST_NAME);
			fail("unexpected success");
		} catch (SchemaException e) {
			TestUtil.displayThen(TEST_NAME);
			System.out.println("Expected exception: " + e);
			e.printStackTrace(System.out);
			if (!e.getMessage().contains("Conflicting definitions of 'constraint-B'")) {
				fail("Exception message was not as expected: " + e.getMessage());
			}
		}
	}


}
