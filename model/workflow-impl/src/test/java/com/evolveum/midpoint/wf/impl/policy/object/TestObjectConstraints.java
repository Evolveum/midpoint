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
package com.evolveum.midpoint.wf.impl.policy.object;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.RecordingProgressListener;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.policy.ExpectedTask;
import com.evolveum.midpoint.wf.impl.policy.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieve;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_WORK_ITEM;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Testing approvals of various triggered object-level constraints.
 * In a way it's an extension of role lifecycle tests.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestObjectConstraints extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestObjectConstraints.class);

	protected static final File TEST_OBJECT_RESOURCE_DIR = new File("src/test/resources/policy/object");

	protected static final File METAROLE_CONSTRAINTS_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "metarole-constraints.xml");
	protected static final File ROLE_EMPLOYEE_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "role-employee.xml");
	protected static final File USER_EMPLOYEE_OWNER_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "user-employee-owner.xml");
	protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "system-configuration.xml");

	protected String metaroleConstraintsOid;
	protected String userEmployeeOwnerOid;

	String roleEmployeeOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		metaroleConstraintsOid = addAndRecompute(METAROLE_CONSTRAINTS_FILE, initTask, initResult);
		userEmployeeOwnerOid = addAndRecomputeUser(USER_EMPLOYEE_OWNER_FILE, initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Test
	public void test010CreateRoleEmployee() throws Exception {
		final String TEST_NAME = "test010CreateRoleEmployee";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<RoleType> employee = prismContext.parseObject(ROLE_EMPLOYEE_FILE);
		executeTest(TEST_NAME, new TestDetails() {
					@Override
					protected LensContext createModelContext(OperationResult result) throws Exception {
						LensContext<RoleType> lensContext = createLensContext(RoleType.class);
						addFocusDeltaToContext(lensContext, ObjectDelta.createAddDelta(employee));
						return lensContext;
					}

					@Override
					protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
							OperationResult result) throws Exception {
						ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
						ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
						assertTrue("Non-empty primary focus delta: " + realDelta0.debugDump(), realDelta0.isEmpty());
						assertNoObject(employee);
						ExpectedTask expectedTask = new ExpectedTask(null, "Addition of " + employee.asObjectable().getName().getOrig());
						ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userEmployeeOwnerOid, null, expectedTask);
						assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
								null,
								Collections.singletonList(expectedTask),
								Collections.singletonList(expectedWorkItem));

						Collection<SelectorOptions<GetOperationOptions>> options =
								SelectorOptions.createCollection(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM), createRetrieve());
						Task opTask = taskManager.createTaskInstance();
						TaskType subtask = modelService.getObject(TaskType.class, subtasks.get(0).getOid(), options, opTask, result).asObjectable();

						WfContextType wfc = subtask.getWorkflowContext();
						ItemApprovalProcessStateType processState = WfContextUtil.getItemApprovalProcessInfo(wfc);
						assertEquals("Wrong # of attached policy rules entries", 1, processState.getPolicyRules().getEntry().size());
						SchemaAttachedPolicyRuleType attachedRule = processState.getPolicyRules().getEntry().get(0);
						assertEquals(1, attachedRule.getStageMin().intValue());
						assertEquals(1, attachedRule.getStageMax().intValue());
						assertEquals("Wrong # of attached triggers", 1, attachedRule.getRule().getTrigger().size());
						EvaluatedPolicyRuleTriggerType trigger = attachedRule.getRule().getTrigger().get(0);
						assertEquals("Wrong constraintKind in trigger", PolicyConstraintKindType.OBJECT_MODIFICATION, trigger.getConstraintKind());

						WorkItemType workItem = wfc.getWorkItem().get(0);
						assertEquals("Wrong # of additional information", 0, workItem.getAdditionalInformation().size());
					}

					@Override
					protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
						assertNoObject(employee);
					}

					@Override
					protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
						assertObject(employee);
					}

					@Override
					protected boolean executeImmediately() {
						return false;
					}

					@Override
					protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
						login(getUser(userEmployeeOwnerOid));
						return true;
					}
				}, 1);


		roleEmployeeOid = searchObjectByName(RoleType.class, "employee").getOid();

		PrismReferenceValue employeeOwner = new PrismReferenceValue(roleEmployeeOid, RoleType.COMPLEX_TYPE).relation(SchemaConstants.ORG_OWNER);
		executeChanges((ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(employeeOwner, prismContext))
				.asObjectDelta(userEmployeeOwnerOid),
				null, task, result);
		display("Employee role", getRole(roleEmployeeOid));
		display("Employee owner", getUser(userEmployeeOwnerOid));
	}

	@Test
	public void test020ActivateIncompleteRole() throws Exception {
		final String TEST_NAME = "test020ActivateIncompleteRole";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		@SuppressWarnings({"unchecked", "raw"})
		ObjectDelta<RoleType> activateRoleDelta = (ObjectDelta<RoleType>) DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ACTIVE)
				.asObjectDelta(roleEmployeeOid);

		RecordingProgressListener recordingListener = new RecordingProgressListener();
		try {
			modelService.executeChanges(Collections.singleton(activateRoleDelta), null, task,
					Collections.singleton(recordingListener), result);
			fail("unexpected success");
		} catch (PolicyViolationException e) {
			System.out.println("Got expected exception: " + e.getMessage());
		}

		LensContext<RoleType> context = (LensContext<RoleType>) recordingListener.getModelContext();
		System.out.println(context.dumpFocusPolicyRules(0));
		EvaluatedPolicyRule incompleteActivationRule = context.getFocusContext().getPolicyRules().stream()
				.filter(rule -> "disallow-incomplete-role-activation".equals(rule.getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("rule not found"));
		assertEquals("Wrong # of triggers in incompleteActivationRule", 2, incompleteActivationRule.getTriggers().size());  // objectState + or
	}

	/**
	 * This time let's fill-in the description as well.
	 */
	@Test
	public void test030ActivateIncompleteRoleAgain() throws Exception {
		final String TEST_NAME = "test030ActivateIncompleteRoleAgain";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		@SuppressWarnings({"unchecked", "raw"})
		ObjectDelta<RoleType> activateRoleDelta = (ObjectDelta<RoleType>) DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ACTIVE)
				.item(RoleType.F_DESCRIPTION).replace("hi")
				.asObjectDelta(roleEmployeeOid);

		RecordingProgressListener recordingListener = new RecordingProgressListener();
		try {
			modelService.executeChanges(Collections.singleton(activateRoleDelta), null, task,
					Collections.singleton(recordingListener), result);
			fail("unexpected success");
		} catch (PolicyViolationException e) {
			System.out.println("Got expected exception: " + e.getMessage());
		}

		LensContext<RoleType> context = (LensContext<RoleType>) recordingListener.getModelContext();
		System.out.println(context.dumpFocusPolicyRules(0));
		EvaluatedPolicyRule incompleteActivationRule = context.getFocusContext().getPolicyRules().stream()
				.filter(rule -> "disallow-incomplete-role-activation".equals(rule.getName()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("rule not found"));
		assertEquals("Wrong # of triggers in incompleteActivationRule", 2, incompleteActivationRule.getTriggers().size());
	}

	@Test
	public void test040AddApprover() throws Exception {
		final String TEST_NAME = "test040AddApprover";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		assignRole(userEmployeeOwnerOid, roleEmployeeOid, SchemaConstants.ORG_APPROVER, task, result);
		result.computeStatus();
		assertSuccess(result);
	}


	@Test
	public void test045ActivateCompleteRole() throws Exception {
		final String TEST_NAME = "test045ActivateCompleteRole";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		@SuppressWarnings({"unchecked", "raw"})
		ObjectDelta<RoleType> activateRoleDelta = (ObjectDelta<RoleType>) DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ACTIVE)
				.item(RoleType.F_DESCRIPTION).replace("hi")
				.asObjectDelta(roleEmployeeOid);

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext(RoleType.class);
				addFocusDeltaToContext(lensContext, activateRoleDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
				ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
				assertTrue("Non-empty primary focus delta: " + realDelta0.debugDump(), realDelta0.isEmpty());
				ExpectedTask expectedTask = new ExpectedTask(null, "Approve modification of employee");
//				ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userEmployeeOwnerOid, null, expectedTask);
//				assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
//						null,
//						Collections.singletonList(expectedTask),
//						Collections.singletonList(expectedWorkItem));
//
//				Collection<SelectorOptions<GetOperationOptions>> options =
//						SelectorOptions.createCollection(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM), createRetrieve());
//				Task opTask = taskManager.createTaskInstance();
//				TaskType subtask = modelService.getObject(TaskType.class, subtasks.get(0).getOid(), options, opTask, result).asObjectable();
//
//				WfContextType wfc = subtask.getWorkflowContext();
//				ItemApprovalProcessStateType processState = WfContextUtil.getItemApprovalProcessInfo(wfc);
//				assertEquals("Wrong # of attached policy rules entries", 1, processState.getPolicyRules().getEntry().size());
//				SchemaAttachedPolicyRuleType attachedRule = processState.getPolicyRules().getEntry().get(0);
//				assertEquals(1, attachedRule.getStageMin().intValue());
//				assertEquals(1, attachedRule.getStageMax().intValue());
//				assertEquals("Wrong # of attached triggers", 1, attachedRule.getRule().getTrigger().size());
//				EvaluatedPolicyRuleTriggerType trigger = attachedRule.getRule().getTrigger().get(0);
//				assertEquals("Wrong constraintKind in trigger", PolicyConstraintKindType.OBJECT_MODIFICATION, trigger.getConstraintKind());
//
//				WorkItemType workItem = wfc.getWorkItem().get(0);
//				assertEquals("Wrong # of additional information", 0, workItem.getAdditionalInformation().size());
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				//assertNoObject(employee);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				//assertObject(employee);
			}

			@Override
			protected boolean executeImmediately() {
				return false;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(getUser(userEmployeeOwnerOid));
				return true;
			}
		}, 1);
	}


//	@Test
//	public void test100ModifyRolePirateDescription() throws Exception {
//		final String TEST_NAME = "test100ModifyRolePirateDescription";
//		TestUtil.displayTestTitle(this, TEST_NAME);
//		login(userAdministrator);
//
//		ObjectDelta<RoleType> descriptionDelta = (ObjectDelta<RoleType>) DeltaBuilder.deltaFor(RoleType.class, prismContext)
//				.item(RoleType.F_DESCRIPTION).replace("Bloody pirate")
//				.asObjectDelta(roleEmployeeOid);
//		ObjectDelta<RoleType> delta0 = ObjectDelta.createModifyDelta(roleEmployeeOid, Collections.emptyList(), RoleType.class, prismContext);
//		//noinspection UnnecessaryLocalVariable
//		ObjectDelta<RoleType> delta1 = descriptionDelta;
//		ExpectedTask expectedTask = new ExpectedTask(null, "Modification of pirate");
//		ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userEmployeeOwnerOid, null, expectedTask);
//		modifyObject(TEST_NAME, descriptionDelta, delta0, delta1, false, true, userEmployeeOwnerOid,
//				Collections.singletonList(expectedTask), Collections.singletonList(expectedWorkItem),
//				() -> {},
//				() -> assertNull("Description is modified", getRoleSimple(roleEmployeeOid).getDescription()),
//				() -> assertEquals("Description was NOT modified", "Bloody pirate", getRoleSimple(roleEmployeeOid).getDescription()));
//	}
//
//	@Test
//	public void test200DeleteRolePirate() throws Exception {
//		final String TEST_NAME = "test200DeleteRolePirate";
//		TestUtil.displayTestTitle(this, TEST_NAME);
//		login(userAdministrator);
//
//		ExpectedTask expectedTask = new ExpectedTask(null, "Deletion of pirate");
//		ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userEmployeeOwnerOid, null, expectedTask);
//		deleteObject(TEST_NAME, RoleType.class, roleEmployeeOid, false, true, userEmployeeOwnerOid,
//				Collections.singletonList(expectedTask), Collections.singletonList(expectedWorkItem));
//	}
//
//	@Test
//	public void zzzMarkAsNotInitialized() {
//		display("Setting class as not initialized");
//		unsetSystemInitialized();
//	}

}
