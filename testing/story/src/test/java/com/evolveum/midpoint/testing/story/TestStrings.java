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

package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertReferenceValues;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * 
 * @author mederly
 *
 */

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestStrings extends AbstractStoryTest {

	@Autowired private WorkflowService workflowService;
	
	private static final String TEST_DIR = "src/test/resources/strings";
	private static final String ORG_DIR = TEST_DIR + "/orgs";
	private static final String ROLES_DIR = TEST_DIR + "/roles";
	private static final String ROLES_SPECIFIC_DIR = TEST_DIR + "/roles-specific";
	private static final String USERS_DIR = TEST_DIR + "/users";

	private static final File ORG_MONKEY_ISLAND_FILE = new File(ORG_DIR, "0-org-monkey-island-modified.xml");
	private static final File ORG_TEAMS_FILE = new File(ORG_DIR, "1-teams.xml");
	private static String orgTeamsOid;
	private static final File ORG_ROLE_CATALOG_FILE = new File(ORG_DIR, "2-role-catalog.xml");
	private static String orgRoleCatalogOid;
	private static final File ORG_SECURITY_APPROVERS_FILE = new File(ORG_DIR, "security-approvers.xml");
	private static String orgSecurityApproversOid;
	private static final File ORG_SOD_APPROVERS_FILE = new File(ORG_DIR, "sod-approvers.xml");
	private static String orgSodApproversOid;

	private static final File FORM_USER_DETAILS_FILE = new File(ROLES_DIR, "form-user-details.xml");
	private static String formUserDetailsOid;
	private static final File METAROLE_APPROVAL_LINE_MANAGERS_FILE = new File(ROLES_DIR, "metarole-approval-line-managers.xml");
	private static String metaroleApprovalLineManagersOid;
	private static final File METAROLE_APPROVAL_ROLE_APPROVERS_ALL_FILE = new File(ROLES_DIR, "metarole-approval-role-approvers-all.xml");
	private static String metaroleApprovalRoleApproversAllOid;
	private static final File METAROLE_APPROVAL_ROLE_APPROVERS_FIRST_FILE = new File(ROLES_DIR, "metarole-approval-role-approvers-first.xml");
	private static String metaroleApprovalRoleApproversFirstOid;
	private static final File METAROLE_APPROVAL_ROLE_APPROVERS_FORM_FILE = new File(ROLES_DIR, "metarole-approval-role-approvers-form.xml");
	private static String metaroleApprovalRoleApproversFormOid;
	private static final File METAROLE_APPROVAL_SECURITY_FILE = new File(ROLES_DIR, "metarole-approval-security.xml");
	private static String metaroleApprovalSecurityOid;
	private static final File METAROLE_APPROVAL_SOD_FILE = new File(ROLES_DIR, "metarole-approval-sod.xml");
	private static String metaroleApprovalSodOid;
	private static final File METAROLE_ESCALATION_THEN_REJECTION_FILE = new File(ROLES_DIR, "metarole-approval-escalation-then-rejection.xml");
	private static String metaroleEscalationThenRejectionOid;
	private static final File METAROLE_REJECTION_FILE = new File(ROLES_DIR, "metarole-approval-rejection.xml");
	private static String metaroleRejectionOid;

	private static final File ROLE_A_TEST_1 = new File(ROLES_SPECIFIC_DIR, "a-test-1.xml");
	private static String roleATest1Oid;
	private static final File ROLE_A_TEST_2A = new File(ROLES_SPECIFIC_DIR, "a-test-2a.xml");
	private static String roleATest2aOid;
	private static final File ROLE_A_TEST_2B = new File(ROLES_SPECIFIC_DIR, "a-test-2b.xml");
	private static String roleATest2bOid;
	private static final File ROLE_A_TEST_3A = new File(ROLES_SPECIFIC_DIR, "a-test-3a.xml");
	private static String roleATest3aOid;
	private static final File ROLE_A_TEST_3B = new File(ROLES_SPECIFIC_DIR, "a-test-3b.xml");
	private static String roleATest3bOid;
	private static final File ROLE_A_TEST_3X = new File(ROLES_SPECIFIC_DIR, "a-test-3x.xml");
	private static String roleATest3xOid;
	private static final File ROLE_A_TEST_3Y = new File(ROLES_SPECIFIC_DIR, "a-test-3y.xml");
	private static String roleATest3yOid;
	private static final File ROLE_A_TEST_4 = new File(ROLES_SPECIFIC_DIR, "a-test-4.xml");
	private static String roleATest4Oid;

	private static final File USER_BARKEEPER_FILE = new File(USERS_DIR, "barkeeper.xml");
	private static String userBarkeeperOid;
	private static final File USER_BOB_FILE = new File(USERS_DIR, "bob.xml");
	private static String userBobOid;
	private static final File USER_CARLA_FILE = new File(USERS_DIR, "carla.xml");
	private static String userCarlaOid;
	private static final File USER_CHEESE_FILE = new File(USERS_DIR, "cheese.xml");
	private static String userCheeseOid;
	private static final File USER_CHEF_FILE = new File(USERS_DIR, "chef.xml");
	private static String userChefOid;
	private static final File USER_ELAINE_FILE = new File(USERS_DIR, "elaine.xml");
	private static String userElaineOid;
	private static final File USER_GUYBRUSH_FILE = new File(USERS_DIR, "guybrush.xml");
	private static String userGuybrushOid;
	private static final File USER_LECHUCK_FILE = new File(USERS_DIR, "lechuck.xml");
	private static String userLechuckOid;

	public static final String NS_STRINGS_EXT = "http://midpoint.evolveum.com/xml/ns/strings";


	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// we prefer running trigger scanner by hand
		// and we don't need validity scanner
		taskManager.suspendTasks(Arrays.asList(TASK_TRIGGER_SCANNER_OID, TASK_VALIDITY_SCANNER_OID), 60000L, initResult);
		modifySystemObjectInRepo(TaskType.class, TASK_TRIGGER_SCANNER_OID,
				DeltaBuilder.deltaFor(TaskType.class, prismContext)
						.item(TaskType.F_SCHEDULE).replace()
						.asItemDeltas(),
				initResult);
		taskManager.resumeTasks(Collections.singleton(TASK_TRIGGER_SCANNER_OID), initResult);

		Task triggerScanner = taskManager.getTask(TASK_TRIGGER_SCANNER_OID, initResult);
		display("triggerScanner", triggerScanner);

		// import of story objects
		repoAddObjectsFromFile(ORG_MONKEY_ISLAND_FILE, OrgType.class, initResult);
		orgTeamsOid = repoAddObjectFromFile(ORG_TEAMS_FILE, initResult).getOid();
		orgRoleCatalogOid = repoAddObjectFromFile(ORG_ROLE_CATALOG_FILE, initResult).getOid();
		orgSecurityApproversOid = repoAddObjectFromFile(ORG_SECURITY_APPROVERS_FILE, initResult).getOid();
		orgSodApproversOid = repoAddObjectFromFile(ORG_SOD_APPROVERS_FILE, initResult).getOid();

		formUserDetailsOid = repoAddObjectFromFile(FORM_USER_DETAILS_FILE, initResult).getOid();
		metaroleApprovalLineManagersOid = repoAddObjectFromFile(METAROLE_APPROVAL_LINE_MANAGERS_FILE, initResult).getOid();
		metaroleApprovalRoleApproversAllOid = repoAddObjectFromFile(METAROLE_APPROVAL_ROLE_APPROVERS_ALL_FILE, initResult).getOid();
		metaroleApprovalRoleApproversFirstOid = repoAddObjectFromFile(METAROLE_APPROVAL_ROLE_APPROVERS_FIRST_FILE, initResult).getOid();
		metaroleApprovalRoleApproversFormOid = repoAddObjectFromFile(METAROLE_APPROVAL_ROLE_APPROVERS_FORM_FILE, initResult).getOid();
		metaroleApprovalSecurityOid = repoAddObjectFromFile(METAROLE_APPROVAL_SECURITY_FILE, initResult).getOid();
		metaroleApprovalSodOid = repoAddObjectFromFile(METAROLE_APPROVAL_SOD_FILE, initResult).getOid();
		metaroleEscalationThenRejectionOid = repoAddObjectFromFile(METAROLE_ESCALATION_THEN_REJECTION_FILE, initResult).getOid();
		metaroleRejectionOid = repoAddObjectFromFile(METAROLE_REJECTION_FILE, initResult).getOid();

		roleATest1Oid = addAndRecompute(ROLE_A_TEST_1, initTask, initResult);
		roleATest2aOid = addAndRecompute(ROLE_A_TEST_2A, initTask, initResult);
		roleATest2bOid = addAndRecompute(ROLE_A_TEST_2B, initTask, initResult);
		roleATest3aOid = addAndRecompute(ROLE_A_TEST_3A, initTask, initResult);
		roleATest3bOid = addAndRecompute(ROLE_A_TEST_3B, initTask, initResult);
		roleATest3xOid = addAndRecompute(ROLE_A_TEST_3X, initTask, initResult);
		roleATest3yOid = addAndRecompute(ROLE_A_TEST_3Y, initTask, initResult);
		roleATest4Oid = addAndRecompute(ROLE_A_TEST_4, initTask, initResult);

		userBarkeeperOid = addAndRecomputeUser(USER_BARKEEPER_FILE, initTask, initResult);
		userBobOid = addAndRecomputeUser(USER_BOB_FILE, initTask, initResult);
		userCarlaOid = addAndRecomputeUser(USER_CARLA_FILE, initTask, initResult);
		userCheeseOid = addAndRecomputeUser(USER_CHEESE_FILE, initTask, initResult);
		userChefOid = addAndRecomputeUser(USER_CHEF_FILE, initTask, initResult);
		userElaineOid = addAndRecomputeUser(USER_ELAINE_FILE, initTask, initResult);
		userGuybrushOid = addAndRecomputeUser(USER_GUYBRUSH_FILE, initTask, initResult);
		userLechuckOid = addAndRecomputeUser(USER_LECHUCK_FILE, initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

	@Override
	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
		Task task = createTask(TestStrings.class.getName() + "." + TEST_NAME);

        // TODO
	}

	@Test
    public void test100SimpleAssignmentStart() throws Exception {
		final String TEST_NAME = "test100SimpleAssignmentStart";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = createTask(TestStrings.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		assignRole(userBobOid, roleATest1Oid, task, task.getResult());
		assertNotAssignedRole(getUser(userBobOid), roleATest1Oid);

		WorkItemType workItem = getWorkItem(task, result);
		display("Work item", workItem);
		PrismObject<TaskType> wfTask = getTask(workItem.getTaskRef().getOid());
		display("wfTask", wfTask);

		assertTriggers(wfTask, 1);

		ItemApprovalProcessStateType info = WfContextUtil.getItemApprovalProcessInfo(wfTask.asObjectable().getWorkflowContext());
		ApprovalSchemaType schema = info.getApprovalSchema();
		assertEquals("Wrong # of approval levels", 3, schema.getLevel().size());
		assertApprovalLevel(schema, 1, "Line managers", "P5D", 2);
		assertApprovalLevel(schema, 2, "Security", "P7D", 1);
		assertApprovalLevel(schema, 3, "Role approvers (all)", "P5D", 2);
		assertStage(wfTask, 1, 3, "Line managers", null);
		assertAssignee(workItem, userLechuckOid, userLechuckOid);
	}

	@Test
	public void test102SimpleAssignmentApproveByLechuck() throws Exception {
		final String TEST_NAME = "test102SimpleAssignmentApproveByLechuck";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = createTask(TestStrings.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);
		WorkItemType workItem = getWorkItem(task, result);

		PrismObject<UserType> lechuck = getUserFromRepo(userLechuckOid);
		login(lechuck);

		workflowService.completeWorkItem(workItem.getWorkItemId(), true, "OK. LeChuck", null, result);

		login(userAdministrator);

		List<WorkItemType> workItems = getWorkItems(task, result);
		assertEquals("Wrong # of work items on level 2", 2, workItems.size());
		workItems.forEach(wi -> display("Work item after 1st approval", wi));
		PrismObject<TaskType> wfTask = getTask(workItem.getTaskRef().getOid());
		display("wfTask after 1st approval", wfTask);

		assertStage(wfTask, 2, 3, "Security", null);
		assertTriggers(wfTask, 2);

		// TODO check events
	}

	@Test
	public void test104SimpleAssignmentApproveByAdministrator() throws Exception {
		final String TEST_NAME = "test104SimpleAssignmentApproveByAdministrator";
		TestUtil.displayTestTile(this, TEST_NAME);
		Task task = createTask(TestStrings.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		login(userAdministrator);
		List<WorkItemType> workItems = getWorkItems(task, result);
		WorkItemType firstWorkItem = workItems.get(0);

		// Second approval
		workflowService.completeWorkItem(firstWorkItem.getWorkItemId(), true, "OK. Security.", null, result);

		workItems = getWorkItems(task, result);
		workItems.forEach(wi -> display("Work item after 2nd approval", wi));
		assertEquals("Wrong # of work items on level 3", 2, workItems.size());
		PrismObject<TaskType> wfTask = getTask(workItems.get(0).getTaskRef().getOid());
		display("wfTask after 2nd approval", wfTask);

		assertStage(wfTask, 3, 3, "Role approvers (all)", null);
		assertTriggers(wfTask, 2);

		Map<String, WorkItemType> workItemsMap = createWorkItemsMap(workItems);
		assertNotNull("chef is not an approver", workItemsMap.get(userChefOid));
		assertNotNull("cheese is not an approver", workItemsMap.get(userCheeseOid));
	}

	private void assertTriggers(PrismObject<TaskType> wfTask, int count) {
		assertEquals("Wrong # of triggers", count, wfTask.asObjectable().getTrigger().size());
	}

	private void assertAssignee(WorkItemType workItem, String originalAssignee, String... currentAssignee) {
		assertRefEquals("Wrong original assignee", ObjectTypeUtil.createObjectRef(originalAssignee, ObjectTypes.USER), workItem.getOriginalAssigneeRef());
		assertReferenceValues(ref(workItem.getAssigneeRef()), currentAssignee);
	}


	private void assertStage(PrismObject<TaskType> wfTask, Integer stageNumber, Integer stageCount, String stageName, String stageDisplayName) {
		WfContextType wfc = wfTask.asObjectable().getWorkflowContext();
		assertEquals("Wrong stage number", stageNumber, wfc.getStageNumber());
		assertEquals("Wrong stage count", stageCount, wfc.getStageCount());
		assertEquals("Wrong stage name", stageName, wfc.getStageName());
		assertEquals("Wrong stage name", stageDisplayName, wfc.getStageDisplayName());
	}

	private void assertApprovalLevel(ApprovalSchemaType schema, int number, String name, String duration, int timedActions) {
		ApprovalLevelType level = schema.getLevel().get(number-1);
		assertEquals("Wrong level number", number, (int) level.getOrder());
		assertEquals("Wrong level name", name, level.getName());
		assertEquals("Wrong level duration", XmlTypeConverter.createDuration(duration), level.getDuration());
		assertEquals("Wrong # of timed actions", timedActions, level.getTimedActions().size());
	}

	//region TODO deduplicate with AbstractWfTestPolicy
	protected WorkItemType getWorkItem(Task task, OperationResult result) throws Exception {
		SearchResultList<WorkItemType> itemsAll = getWorkItems(task, result);
		if (itemsAll.size() != 1) {
			System.out.println("Unexpected # of work items: " + itemsAll.size());
			for (WorkItemType workItem : itemsAll) {
				System.out.println(PrismUtil.serializeQuietly(prismContext, workItem));
			}
		}
		assertEquals("Wrong # of total work items", 1, itemsAll.size());
		return itemsAll.get(0);
	}

	private SearchResultList<WorkItemType> getWorkItems(Task task, OperationResult result) throws Exception {
		return modelService.searchContainers(WorkItemType.class, null, null, task, result);
	}

	protected ObjectReferenceType ort(String oid) {
		return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.USER);
	}

	protected PrismReferenceValue prv(String oid) {
		return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.USER).asReferenceValue();
	}

	protected PrismReference ref(List<ObjectReferenceType> orts) {
		PrismReference rv = new PrismReference(new QName("dummy"));
		orts.forEach(ort -> rv.add(ort.asReferenceValue().clone()));
		return rv;
	}

	protected PrismReference ref(ObjectReferenceType ort) {
		return ref(Collections.singletonList(ort));
	}

	protected Map<String, WorkItemType> createWorkItemsMap(Collection<WorkItemType> workItems) {
		Map<String, WorkItemType> rv = new HashMap<>();
		workItems.forEach(wi -> rv.put(wi.getOriginalAssigneeRef().getOid(), wi));
		return rv;
	}
	//endregion

}
