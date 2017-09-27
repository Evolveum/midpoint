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

package com.evolveum.midpoint.certification.test.complex;

import com.evolveum.midpoint.certification.test.AbstractUninitializedCertificationTest;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.RecordingProgressListener;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.createPartialProcessing;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;

/**
 * A complex policy-drive role lifecycle scenario (see https://wiki.evolveum.com/display/midPoint/Sample+scenario).
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-certification-test-with-workflows.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPolicyDrivenRoleLifecycle extends AbstractUninitializedCertificationTest {

	protected static final File TEST_DIR = new File("src/test/resources/complex");
	public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

	protected static final File ROLE_EMPTY_FILE = new File(TEST_DIR, "role-empty.xml");
	protected static final File ROLE_HIGH_RISK_EMPTY_FILE = new File(TEST_DIR, "role-high-risk-empty.xml");
	protected static final File ROLE_CORRECT_FILE = new File(TEST_DIR, "role-correct.xml");
	protected static final File ROLE_CORRECT_HIGH_RISK_FILE = new File(TEST_DIR, "role-correct-high-risk.xml");

	protected static final String SITUATION_INCOMPLETE_ROLE = "http://sample.org/situations#incomplete-role-c1-to-c4";
	protected static final String SITUATION_ACTIVE_ROLE_WITH_NO_IDENTIFIER = "http://sample.org/situations#active-role-with-no-identifier";

	protected static String roleEmptyOid;
	protected static String roleHighRiskEmptyOid;
	protected static String roleCorrectOid;
	protected static String roleCorrectHighRiskOid;
	protected static String userJackOid;

	protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");

	protected static final File ASSIGNMENT_CERT_DEF_FILE = new File(TEST_DIR, "adhoc-certification-assignment.xml");
    protected static final String ASSIGNMENT_CERT_DEF_OID = "540940e9-4ac5-4340-ba85-fd7e8b5e6686";

	protected static final File MODIFICATION_CERT_DEF_FILE = new File(TEST_DIR, "adhoc-certification-modification.xml");
    protected static final String MODIFICATION_CERT_DEF_OID = "83a16584-bb2a-448c-aee1-82fc6d577bcb";

    protected static final File ORG_LABORATORY_FILE = new File(TEST_DIR, "org-laboratory.xml");
    protected static final String ORG_LABORATORY_OID = "027faec7-7763-4b26-ab92-c5c0acbb1173";

    protected static final File USER_INDIGO_FILE = new File(TEST_DIR, "user-indigo.xml");
    protected static final String USER_INDIGO_OID = "11b35bd2-9b2f-4a00-94fa-7ed0079a7500";

    protected AccessCertificationDefinitionType assignmentCertificationDefinition;
    protected AccessCertificationDefinitionType modificationCertificationDefinition;

    @Autowired private WorkflowService workflowService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        userJackOid = addAndRecompute(USER_JACK_FILE, initTask, initResult);
	    roleEmptyOid = addAndRecompute(ROLE_EMPTY_FILE, initTask, initResult);
	    roleHighRiskEmptyOid = addAndRecompute(ROLE_HIGH_RISK_EMPTY_FILE, initTask, initResult);
	    roleCorrectOid = addAndRecompute(ROLE_CORRECT_FILE, initTask, initResult);
	    roleCorrectHighRiskOid = addAndRecompute(ROLE_CORRECT_HIGH_RISK_FILE, initTask, initResult);
    }

	@NotNull
	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Test
	public void test010AttemptToActivateIncompleteRoleC1345() throws Exception {
		final String TEST_NAME = "test010AttemptToActivateIncompleteRoleC1345";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
		task.setOwner(userAdministrator.asPrismObject());
		OperationResult result = task.getResult();

		// WHEN+THEN
		TestUtil.displayWhen(TEST_NAME);
		TestUtil.displayThen(TEST_NAME);
		Holder<LensContext<?>> contextHolder = new Holder<>();
		activateRoleAssertFailure(roleEmptyOid, contextHolder, result, task);

		PrismObject<RoleType> role = getRole(roleEmptyOid);
		display("role after", role);

		dumpRules(contextHolder);
		assertEquals("Wrong policy situation", singletonList(SITUATION_INCOMPLETE_ROLE), role.asObjectable().getPolicySituation());
	}

	private void activateRoleAssertFailure(String roleOid, Holder<LensContext<?>> contextHolder, OperationResult result, Task task)
			throws SchemaException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException,
			SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		try {
			activateRole(roleOid, contextHolder, task, result);
			fail("unexpected success");
		} catch (PolicyViolationException e) {
			System.out.println("Got expected exception:");
			e.printStackTrace(System.out);
		}
	}

	@Test
	public void test020AttemptToActivateIncompleteRoleC234() throws Exception {
		final String TEST_NAME = "test020AttemptToActivateIncompleteRoleC234";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
		task.setOwner(userAdministrator.asPrismObject());
		OperationResult result = task.getResult();

		// WHEN+THEN
		TestUtil.displayWhen(TEST_NAME);
		TestUtil.displayThen(TEST_NAME);
		Holder<LensContext<?>> contextHolder = new Holder<>();
		activateRoleAssertFailure(roleHighRiskEmptyOid, contextHolder, result, task);

		PrismObject<RoleType> role = getRole(roleHighRiskEmptyOid);
		display("role after", role);

		dumpRules(contextHolder);
		assertEquals("Wrong policy situation", singletonList(SITUATION_INCOMPLETE_ROLE), role.asObjectable().getPolicySituation());
	}

	@Test
	public void test030AttemptToActivateCorrectRoleC34() throws Exception {
		final String TEST_NAME = "test030AttemptToActivateCorrectRoleC34";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
		task.setOwner(userAdministrator.asPrismObject());
		OperationResult result = task.getResult();

		// WHEN+THEN
		TestUtil.displayWhen(TEST_NAME);
		TestUtil.displayThen(TEST_NAME);
		Holder<LensContext<?>> contextHolder = new Holder<>();
		activateRoleAssertFailure(roleCorrectOid, contextHolder, result, task);

		PrismObject<RoleType> role = getRole(roleCorrectOid);
		display("role after", role);

		dumpRules(contextHolder);
		assertEquals("Wrong policy situation", singletonList(SITUATION_INCOMPLETE_ROLE), role.asObjectable().getPolicySituation());
	}

	private void dumpRules(Holder<LensContext<?>> contextHolder) {
		System.out.println(contextHolder.getValue().dumpFocusPolicyRules(0, true));
	}

	@Test
	public void test040AssignOwnerAndApproverToCorrectRole() throws Exception {
		final String TEST_NAME = "test040AssignOwnerAndApproverToCorrectRole";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
		task.setOwner(userAdministrator.asPrismObject());
		OperationResult result = task.getResult();

		// WHEN+THEN
		TestUtil.displayWhen(TEST_NAME);
		TestUtil.displayThen(TEST_NAME);
		ModelExecuteOptions noApprovals = createPartialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
		assignRole(USER_ADMINISTRATOR_OID, roleCorrectOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
		assignRole(USER_ADMINISTRATOR_OID, roleCorrectOid, SchemaConstants.ORG_OWNER, noApprovals, task, result);

		// recompute the role to set correct policy situation
		recomputeFocus(RoleType.class, roleCorrectOid, task, result);
	}

	@Test
	public void test050ActivateCorrectRole() throws Exception {
		final String TEST_NAME = "test050ActivateCorrectRole";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
		task.setOwner(userAdministrator.asPrismObject());
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Holder<LensContext<?>> contextHolder = new Holder<>();
		activateRole(roleCorrectOid, contextHolder, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		PrismObject<RoleType> roleAfter = getRole(roleCorrectOid);
		display("role after", roleAfter);
		assertEquals("Wrong (changed) lifecycle state", SchemaConstants.LIFECYCLE_DRAFT, roleAfter.asObjectable().getLifecycleState());

		dumpRules(contextHolder);
		assertEquals("Wrong policy situation", emptyList(), roleAfter.asObjectable().getPolicySituation());
		assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.asObjectable().getTriggeredPolicyRule());

		Collection<SelectorOptions<GetOperationOptions>> options =
				GetOperationOptions.retrieveItemsNamed(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM);
		List<PrismObject<TaskType>> tasks = getTasksForObject(roleCorrectOid, RoleType.COMPLEX_TYPE, options, task, result);
		display("tasks for role", tasks);
		assertEquals("Wrong # of approval tasks for role", 2, tasks.size());

		TaskType approvalTask = getApprovalTask(tasks);
		TaskType rootTask = getRootTask(tasks);
		WfContextType wfc = approvalTask.getWorkflowContext();
		assertEquals("Modification of correct", wfc.getProcessInstanceName());
		assertEquals("wrong # of work items", 1, wfc.getWorkItem().size());
		WorkItemType workItem = wfc.getWorkItem().get(0);
		ItemApprovalProcessStateType info = WfContextUtil.getItemApprovalProcessInfo(wfc);
		assertEquals("wrong # of approval stages", 1, info.getApprovalSchema().getStage().size());
		assertEquals("wrong # of attached policy rules", 1, info.getPolicyRules().getEntry().size());
		EvaluatedPolicyRuleType rule = info.getPolicyRules().getEntry().get(0).getRule();
		List<EvaluatedPolicyRuleTriggerType> triggers = rule.getTrigger();

		// TODO check trigger

		workflowService.completeWorkItem(workItem.getExternalId(), true, null, null, result);
		waitForTaskFinish(rootTask.getOid(), false);

		PrismObject<RoleType> roleAfterApproval = getRole(roleCorrectOid);
		display("role after approval", roleAfterApproval);
		assertEquals("Wrong (unchanged) lifecycle state", SchemaConstants.LIFECYCLE_ACTIVE, roleAfterApproval.asObjectable().getLifecycleState());

		assertEquals("Wrong policy situation", emptyList(), roleAfter.asObjectable().getPolicySituation());
		assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.asObjectable().getTriggeredPolicyRule());
	}

	@Test
	public void test060AssignOwnerAndApproverToCorrectHighRiskRole() throws Exception {
		final String TEST_NAME = "test060AssignOwnerAndApproverToCorrectHighRiskRole";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
		task.setOwner(userAdministrator.asPrismObject());
		OperationResult result = task.getResult();

		// WHEN+THEN
		TestUtil.displayWhen(TEST_NAME);
		TestUtil.displayThen(TEST_NAME);
		ModelExecuteOptions noApprovals = createPartialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
		assignRole(USER_ADMINISTRATOR_OID, roleCorrectHighRiskOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
		assignRole(userJackOid, roleCorrectHighRiskOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
		assignRole(USER_ADMINISTRATOR_OID, roleCorrectHighRiskOid, SchemaConstants.ORG_OWNER, noApprovals, task, result);

		// recompute the role to set correct policy situation
		recomputeFocus(RoleType.class, roleCorrectHighRiskOid, task, result);
	}

	@Test
	public void test070ActivateCorrectHighRiskRole() throws Exception {
		final String TEST_NAME = "test070ActivateCorrectHighRiskRole";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
		task.setOwner(userAdministrator.asPrismObject());
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		Holder<LensContext<?>> contextHolder = new Holder<>();
		activateRole(roleCorrectHighRiskOid, contextHolder, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		PrismObject<RoleType> roleAfter = getRole(roleCorrectHighRiskOid);
		display("role after", roleAfter);
		assertEquals("Wrong (changed) lifecycle state", SchemaConstants.LIFECYCLE_DRAFT, roleAfter.asObjectable().getLifecycleState());

		dumpRules(contextHolder);

		assertEquals("Wrong policy situation", emptyList(), roleAfter.asObjectable().getPolicySituation());
		assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.asObjectable().getTriggeredPolicyRule());

		Collection<SelectorOptions<GetOperationOptions>> options =
				GetOperationOptions.retrieveItemsNamed(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM);
		List<PrismObject<TaskType>> tasks = getTasksForObject(roleCorrectHighRiskOid, RoleType.COMPLEX_TYPE, options, task, result);
		display("tasks for role", tasks);
		assertEquals("Wrong # of approval tasks for role", 2, tasks.size());

		TaskType approvalTask = getApprovalTask(tasks);
		WfContextType wfc = approvalTask.getWorkflowContext();
		assertEquals("Modification of correct-high-risk", wfc.getProcessInstanceName());
		assertEquals("wrong # of work items", 1, wfc.getWorkItem().size());
		WorkItemType workItem = wfc.getWorkItem().get(0);
		ItemApprovalProcessStateType info = WfContextUtil.getItemApprovalProcessInfo(wfc);
		assertEquals("wrong # of approval stages", 2, info.getApprovalSchema().getStage().size());
		assertEquals("wrong # of attached policy rules", 2, info.getPolicyRules().getEntry().size());

		workflowService.completeWorkItem(workItem.getExternalId(), true, null, null, result);

		approvalTask = modelService.getObject(TaskType.class, approvalTask.getOid(), options, task, result).asObjectable();
		wfc = approvalTask.getWorkflowContext();
		assertEquals("wrong # of work items", 1, wfc.getWorkItem().size());
		workItem = wfc.getWorkItem().get(0);
		workflowService.completeWorkItem(workItem.getExternalId(), true, null, null, result);

		TaskType rootTask = getRootTask(tasks);
		waitForTaskFinish(rootTask.getOid(), false);

		PrismObject<RoleType> roleAfterApproval = getRole(roleCorrectHighRiskOid);
		display("role after approval", roleAfterApproval);
		assertEquals("Wrong (unchanged) lifecycle state", SchemaConstants.LIFECYCLE_ACTIVE, roleAfterApproval.asObjectable().getLifecycleState());

		assertEquals("Wrong policy situation", singletonList(SITUATION_ACTIVE_ROLE_WITH_NO_IDENTIFIER), roleAfterApproval.asObjectable().getPolicySituation());
		assertEquals("Wrong triggered policy rules", emptyList(), roleAfterApproval.asObjectable().getTriggeredPolicyRule());   // recording rules = none
	}

	private void activateRole(String oid, Holder<LensContext<?>> contextHolder, Task task, OperationResult result)
			throws SchemaException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException,
			PolicyViolationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		ObjectDelta<RoleType> delta = DeltaBuilder.deltaFor(RoleType.class, prismContext)
				.item(RoleType.F_LIFECYCLE_STATE)
				.replace(SchemaConstants.LIFECYCLE_ACTIVE)
				.asObjectDeltaCast(oid);
		RecordingProgressListener listener = new RecordingProgressListener();
		try {
			modelService.executeChanges(singleton(delta), null, task, singleton(listener), result);
		} finally {
			if (contextHolder != null) {
				contextHolder.setValue((LensContext<?>) listener.getModelContext());
			}
		}
	}

//	@Test
//    public void test010HireIndigo() throws Exception {
//        final String TEST_NAME = "test010HireIndigo";
//        TestUtil.displayTestTitle(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
//        task.setOwner(userAdministrator.asPrismObject());
//        OperationResult result = task.getResult();
//
//        // WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        assignOrg(USER_INDIGO_OID, ORG_LABORATORY_OID, task, result);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//        result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns = repositoryService
//				.searchObjects(AccessCertificationCampaignType.class, null, null, result);
//		assertEquals("Wrong # of campaigns", 1, campaigns.size());
//		AccessCertificationCampaignType campaign = campaigns.get(0).asObjectable();
//
//		campaign = getCampaignWithCases(campaign.getOid());
//        display("campaign", campaign);
//        assertAfterCampaignStart(campaign, assignmentCertificationDefinition, 1);		// beware, maybe not all details would match (in the future) - then adapt this test
//        assertPercentComplete(campaign, 0, 0, 0);      // no cases, no problems
//		assertCases(campaign.getOid(), 1);
//	}
//
//    @Test
//    public void test020ModifyIndigo() throws Exception {
//        final String TEST_NAME = "test020ModifyIndigo";
//        TestUtil.displayTestTitle(this, TEST_NAME);
//
//        // GIVEN
//        Task task = taskManager.createTaskInstance(TestPolicyDrivenRoleLifecycle.class.getName() + "." + TEST_NAME);
//        task.setOwner(userAdministrator.asPrismObject());
//        OperationResult result = task.getResult();
//
//        // WHEN
//        TestUtil.displayWhen(TEST_NAME);
//        @SuppressWarnings({ "unchecked", "raw" })
//		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
//				.item(UserType.F_DESCRIPTION).replace("new description")
//				.item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
//				.asObjectDelta(USER_INDIGO_OID);
//        executeChanges(delta, null, task, result);
//
//        // THEN
//        TestUtil.displayThen(TEST_NAME);
//        result.computeStatus();
//        TestUtil.assertSuccess(result);
//
//		SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns = repositoryService
//				.searchObjects(AccessCertificationCampaignType.class, null, null, result);
//		assertEquals("Wrong # of campaigns", 2, campaigns.size());
//		AccessCertificationCampaignType campaign = campaigns.stream()
//				.filter(c -> MODIFICATION_CERT_DEF_OID.equals(c.asObjectable().getDefinitionRef().getOid()))
//				.findFirst()
//				.orElseThrow(() -> new AssertionError("No modification-triggered campaign")).asObjectable();
//
//		campaign = getCampaignWithCases(campaign.getOid());
//        display("campaign", campaign);
//        assertAfterCampaignStart(campaign, modificationCertificationDefinition, 1);		// beware, maybe not all details would match (in the future) - then adapt this test
//        assertPercentComplete(campaign, 0, 0, 0);      // no cases, no problems
//	}
}
