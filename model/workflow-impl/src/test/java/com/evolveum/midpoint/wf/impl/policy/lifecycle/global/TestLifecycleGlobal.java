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

package com.evolveum.midpoint.wf.impl.policy.lifecycle.global;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.impl.policy.lifecycle.AbstractTestLifecycle;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

/**
 * Tests role lifecycle with global policy rules.
 *
 * @author mederly
 */
public class TestLifecycleGlobal extends AbstractTestLifecycle {

	@Override
	protected boolean approveObjectAdd() {
		return true;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

		// couldn't use updateSystemConfiguration because users' OIDs are not known yet at that time
		GlobalPolicyRuleType ruleAll =
				new GlobalPolicyRuleType(prismContext)
					.name("all-modifications")
					.beginPolicyConstraints()
						.beginModification()
						.<PolicyConstraintsType>end()
					.<GlobalPolicyRuleType>end()
					.beginPolicyActions()
						.beginApproval()
							.beginApprovalSchema()
								.beginLevel()
									.approverRelation(new QName("owner"))		// intentionally no namespace
									.outcomeIfNoApprovers(ApprovalLevelOutcomeType.APPROVE)
								.<ApprovalSchemaType>end()
							.<ApprovalPolicyActionType>end()
						.<PolicyActionsType>end()
					.<GlobalPolicyRuleType>end()
					.beginFocusSelector()
						.type(RoleType.COMPLEX_TYPE)
					.end();

		GlobalPolicyRuleType ruleAllAdditions =
				new GlobalPolicyRuleType(prismContext)
					.name("all-additions")
					.beginPolicyConstraints()
						.beginModification()
							.operation(ChangeTypeType.ADD)
						.<PolicyConstraintsType>end()
					.<GlobalPolicyRuleType>end()
					.beginPolicyActions()
						.beginApproval()
							.approverRef(userLead1Oid, UserType.COMPLEX_TYPE)
						.<PolicyActionsType>end()
					.<GlobalPolicyRuleType>end()
					.beginFocusSelector()
						.type(RoleType.COMPLEX_TYPE)
					.end();

		GlobalPolicyRuleType ruleModificationOfRiskLevel =
				new GlobalPolicyRuleType(prismContext)
					.name("modification-of-risk-level")
					.beginPolicyConstraints()
						.beginModification()
							.item(new ItemPath(RoleType.F_RISK_LEVEL).asItemPathType())
						.<PolicyConstraintsType>end()
					.<GlobalPolicyRuleType>end()
					.beginPolicyActions()
						.beginApproval()
							.beginProcessSpecification()
								.beginDeltaFrom()
									.item(new ItemPath(RoleType.F_RISK_LEVEL).asItemPathType())
								.<WfProcessSpecificationType>end()
							.<ApprovalPolicyActionType>end()
							.approverRef(userLead2Oid, UserType.COMPLEX_TYPE)
						.<PolicyActionsType>end()
					.<GlobalPolicyRuleType>end()
					.beginFocusSelector()
						.type(RoleType.COMPLEX_TYPE)
					.end();

		GlobalPolicyRuleType ruleModificationOfApproverRef =
				new GlobalPolicyRuleType(prismContext)
					.name("modification-of-approverRef")        // to be multivalued
					.beginPolicyConstraints()
						.beginModification()
							.item(new ItemPath(RoleType.F_APPROVER_REF).asItemPathType())
						.<PolicyConstraintsType>end()
					.<GlobalPolicyRuleType>end()
					.beginPolicyActions()
						.beginApproval()
							.beginProcessSpecification()
								.beginDeltaFrom()
									.itemValue(new ItemPath(RoleType.F_APPROVER_REF).asItemPathType())
								.<WfProcessSpecificationType>end()
							.<ApprovalPolicyActionType>end()
							.approverRef(userLead3Oid, UserType.COMPLEX_TYPE)
						.<PolicyActionsType>end()
					.<GlobalPolicyRuleType>end()
					.beginFocusSelector()
						.type(RoleType.COMPLEX_TYPE)
					.end();

		List<ItemDelta<?, ?>> deltas =
				DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
						.item(SystemConfigurationType.F_GLOBAL_POLICY_RULE)
						.replace(ruleAll, ruleAllAdditions, ruleModificationOfRiskLevel, ruleModificationOfApproverRef)
						.asItemDeltas();
		repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), deltas, initResult);

		systemObjectCache.invalidateCaches();

		IntegrationTestTools.display("System configuration",
				getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value()));
	}

	private String roleJudgeOid;
	private String roleCaptainOid;
	private String roleThiefOid;

	@Test
	public void test500CreateRoleJudge() throws Exception {
		final String TEST_NAME = "test500CreateRoleJudge";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		RoleType judge = new RoleType(prismContext)
				.name("judge")
				.riskLevel("high");

		ObjectDelta<RoleType> addObjectDelta = ObjectDelta.createAddDelta(judge.asPrismObject());

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext(RoleType.class);
				addFocusDeltaToContext(lensContext, addObjectDelta);
				lensContext.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				assertFalse("There is model context in the root task (it should not be there)",
						wfTaskUtil.hasModelContext(rootTask));
				display("subtasks", subtasks);
				display("work items", workItems);
				// todo some asserts here
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertNoObject(judge);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				assertObject(judge);
			}

			@Override
			protected boolean executeImmediately() {
				return true;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(userAdministrator);
				return true;
			}
		}, 2);

		// TODO some more asserts

		PrismObject<RoleType> judgeAfter = searchObjectByName(RoleType.class, "judge");
		roleJudgeOid = judgeAfter.getOid();

		PrismReferenceValue judgeOwner = new PrismReferenceValue(roleJudgeOid, RoleType.COMPLEX_TYPE);
		judgeOwner.setRelation(SchemaConstants.ORG_OWNER);
		executeChanges(DeltaBuilder.deltaFor(UserType.class, prismContext)
						.item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(judgeAfter, SchemaConstants.ORG_OWNER))
						.asObjectDeltaCast(userJudgeOwnerOid),
				null, task, result);

		display("Judge role", judgeAfter);
		display("Judge owner", getUser(userJudgeOwnerOid));

		assertEquals("Wrong risk level", "high", judgeAfter.asObjectable().getRiskLevel());
	}

	@Test
	public void test510AddApproversToJudge() throws Exception {
		final String TEST_NAME = "test510AddApproversToJudge";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ObjectDelta<RoleType> judgeDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_APPROVER_REF)
						.add(new ObjectReferenceType().oid("oid1").type(RoleType.COMPLEX_TYPE),
								new ObjectReferenceType().oid("oid2").type(RoleType.COMPLEX_TYPE))
				.item(RoleType.F_DESCRIPTION)
						.replace("hi")
				.asObjectDeltaCast(roleJudgeOid);

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext(RoleType.class);
				addFocusDeltaToContext(lensContext, judgeDelta);
				lensContext.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				assertFalse("There is model context in the root task (it should not be there)",
						wfTaskUtil.hasModelContext(rootTask));
				display("subtasks", subtasks);
				display("work items", workItems);
				// todo some asserts here
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				// nothing here
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				// nothing here
			}

			@Override
			protected boolean executeImmediately() {
				return true;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(userAdministrator);
				return true;
			}
		}, 3);

		// TODO some more asserts

		PrismObject<RoleType> judgeAfter = searchObjectByName(RoleType.class, "judge");

		display("Judge role", judgeAfter);

		assertEquals("Wrong risk level", "high", judgeAfter.asObjectable().getRiskLevel());
		assertEquals("Wrong description", "hi", judgeAfter.asObjectable().getDescription());
		PrismAsserts.assertReferenceValues(judgeAfter.findReference(RoleType.F_APPROVER_REF), "oid1", "oid2");
	}

	@Test
	public void test600CreateRoleCaptain() throws Exception {
		final String TEST_NAME = "test600CreateRoleCaptain";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		RoleType captain = new RoleType(prismContext)
				.name("captain")
				.description("something")
				.riskLevel("high")
				.approverRef(new ObjectReferenceType().oid("oid1").type(UserType.COMPLEX_TYPE))
				.approverRef(new ObjectReferenceType().oid("oid2").type(UserType.COMPLEX_TYPE));

		ObjectDelta<RoleType> addObjectDelta = ObjectDelta.createAddDelta(captain.asPrismObject());

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext(RoleType.class);
				addFocusDeltaToContext(lensContext, addObjectDelta);
				lensContext.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				assertFalse("There is model context in the root task (it should not be there)",
						wfTaskUtil.hasModelContext(rootTask));
				display("subtasks", subtasks);
				display("work items", workItems);
				// todo some asserts here
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertNoObject(captain);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				assertObject(captain);
			}

			@Override
			protected boolean executeImmediately() {
				return true;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(userAdministrator);
				return true;
			}
		}, 4);

		// TODO some more asserts

		PrismObject<RoleType> captainAfter = searchObjectByName(RoleType.class, "captain");
		roleCaptainOid = captainAfter.getOid();
		display("Captain role", captainAfter);

		assertEquals("Wrong risk level", "high", captainAfter.asObjectable().getRiskLevel());
		PrismAsserts.assertReferenceValues(captainAfter.findReference(RoleType.F_APPROVER_REF), "oid1", "oid2");
	}

	@Test
	public void test610DeleteApproversFromCaptain() throws Exception {
		final String TEST_NAME = "test610DeleteApproversFromCaptain";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<RoleType> captainBefore = getRole(roleCaptainOid);

		ObjectDelta<RoleType> captainDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_APPROVER_REF)
						.delete(CloneUtil.cloneCollectionMembers(captainBefore.findReference(RoleType.F_APPROVER_REF).getValues()))
				.asObjectDeltaCast(roleCaptainOid);

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext(RoleType.class);
				addFocusDeltaToContext(lensContext, captainDelta);
				lensContext.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				assertFalse("There is model context in the root task (it should not be there)",
						wfTaskUtil.hasModelContext(rootTask));
				display("subtasks", subtasks);
				display("work items", workItems);
				// todo some asserts here
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				// nothing here
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				// nothing here
			}

			@Override
			protected boolean executeImmediately() {
				return true;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(userAdministrator);
				return true;
			}
		}, 2);

		// TODO some more asserts

		PrismObject<RoleType> captainAfter = getRole(roleCaptainOid);

		display("Captain role", captainAfter);

		assertEquals("Wrong risk level", "high", captainAfter.asObjectable().getRiskLevel());
		PrismAsserts.assertReferenceValues(captainAfter.findOrCreateReference(RoleType.F_APPROVER_REF));
	}

	@Test
	public void test700CreateRoleThief() throws Exception {
		final String TEST_NAME = "test700CreateRoleThief";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		RoleType thief = new RoleType(prismContext)
				.name("thief")
				.description("something")
				.riskLevel("high")
				.approverRef(new ObjectReferenceType().oid("oid1").type(UserType.COMPLEX_TYPE))
				.approverRef(new ObjectReferenceType().oid("oid2").type(UserType.COMPLEX_TYPE));

		ObjectDelta<RoleType> addObjectDelta = ObjectDelta.createAddDelta(thief.asPrismObject());

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext(RoleType.class);
				addFocusDeltaToContext(lensContext, addObjectDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				display("subtasks", subtasks);
				display("work items", workItems);
				// todo some asserts here
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertNoObject(thief);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				assertObject(thief);
			}

			@Override
			protected boolean executeImmediately() {
				return false;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(userAdministrator);
				return true;
			}
		}, 4);

		// TODO some more asserts

		PrismObject<RoleType> thiefAfter = searchObjectByName(RoleType.class, "thief");
		roleThiefOid = thiefAfter.getOid();
		display("Thief role", thiefAfter);

		assertEquals("Wrong risk level", "high", thiefAfter.asObjectable().getRiskLevel());
		PrismAsserts.assertReferenceValues(thiefAfter.findReference(RoleType.F_APPROVER_REF), "oid1", "oid2");
	}

	@Test
	public void test710DeleteApproversFromThief() throws Exception {
		final String TEST_NAME = "test710DeleteApproversFromThief";
		TestUtil.displayTestTitle(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<RoleType> thiefBefore = getRole(roleThiefOid);

		ObjectDelta<RoleType> captainDelta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_APPROVER_REF)
				.delete(CloneUtil.cloneCollectionMembers(thiefBefore.findReference(RoleType.F_APPROVER_REF).getValues()))
				.asObjectDeltaCast(roleThiefOid);

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext(RoleType.class);
				addFocusDeltaToContext(lensContext, captainDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				display("subtasks", subtasks);
				display("work items", workItems);
				// todo some asserts here
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				// nothing here
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				// nothing here
			}

			@Override
			protected boolean executeImmediately() {
				return false;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(userAdministrator);
				return true;
			}
		}, 2);

		// TODO some more asserts

		PrismObject<RoleType> thiefAfter = getRole(roleThiefOid);

		display("Thief role", thiefAfter);

		assertEquals("Wrong risk level", "high", thiefAfter.asObjectable().getRiskLevel());
		PrismAsserts.assertReferenceValues(thiefAfter.findOrCreateReference(RoleType.F_APPROVER_REF));
	}

	// TODO test that contains task0 that adds an object (i.e. rule for 'add' is not applied)

	@Test
	public void zzzMarkAsNotInitialized() {
		display("Setting class as not initialized");
		unsetSystemInitialized();
	}
}
