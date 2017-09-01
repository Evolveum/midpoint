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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

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

	private static final int STUDENT_TARGET_RULES = 2;          // one is global
	private static final int STUDENT_FOCUS_RULES = 17;

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
				.policyActions(new PolicyActionsType(prismContext));

		repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
						.item(SystemConfigurationType.F_GLOBAL_POLICY_RULE).add(hasStudentDisabled)
						.asItemDeltas(), initResult);

		InternalMonitor.reset();
		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	/**
	 * Jack's cost center does not match neither 1900 nor 1910. So these constraints should not trigger.
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
		assertTargetTriggers(context, null, 1);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT, 1);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 6);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 5);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
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
		assertTargetTriggers(context, null, 1);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT, 1);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 9);
		assertFocusTriggers(context, PolicyConstraintKindType.FOCUS_STATE, 3);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 5);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
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
		assertTargetTriggers(context, null, 1);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT, 1);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 7);
		assertFocusTriggers(context, PolicyConstraintKindType.FOCUS_STATE, 1);      // new
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 5);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
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
		assertTargetTriggers(context, null, 1);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT, 1);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 8);
		assertFocusTriggers(context, PolicyConstraintKindType.FOCUS_STATE, 2);      // new, current
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 5);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);

		assertAssignmentPolicySituation(context, roleStudentOid, SchemaConstants.MODEL_POLICY_SITUATION_ASSIGNED);
		assertFocusPolicySituation(context,
				SchemaConstants.MODEL_POLICY_SITUATION_FOCUS_STATE,
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
		assertTargetTriggers(context, null, 0);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT, 0);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 9);
		assertFocusTriggers(context, PolicyConstraintKindType.FOCUS_STATE, 3);      // old, current, new 1900 rules
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 5);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
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
		assertTargetTriggers(context, null, 2);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT, 1);
		assertTargetTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 1);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 5);
		assertFocusTriggers(context, PolicyConstraintKindType.FOCUS_STATE, 0);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 4);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
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
		assertTargetTriggers(context, null, 1);
		assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT, 1);

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 8);
		assertFocusTriggers(context, PolicyConstraintKindType.FOCUS_STATE, 2);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 2+2+1);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);
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
		assertTargetTriggers(context, null, 0);
		// Assignment situation is already gone (in second iteration)!
		// This is different from test130, where an assignment is being added (instead of whole user being added).
		// The difference is that when adding a user, its assignments (in wave 1) are in the zero set.
		// Whereas when modifying a user, its new assignments (in wave 1) are in the plus set.
		// This is to be solved somehow.
		// See MID-4126.

		assertEvaluatedFocusPolicyRules(context, STUDENT_FOCUS_RULES);
		assertFocusTriggers(context, null, 8);
		assertFocusTriggers(context, PolicyConstraintKindType.FOCUS_STATE, 2);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_ASSIGNMENT, 2+2+1);
		assertFocusTriggers(context, PolicyConstraintKindType.HAS_NO_ASSIGNMENT, 1);

		// adapt the test after fixing MID-4126
		assertAssignmentPolicySituation(context, roleStudentOid);
		assertFocusPolicySituation(context,
				SchemaConstants.MODEL_POLICY_SITUATION_FOCUS_STATE,
				SchemaConstants.MODEL_POLICY_SITUATION_HAS_ASSIGNMENT,
				SchemaConstants.MODEL_POLICY_SITUATION_HAS_NO_ASSIGNMENT);
	}

}
