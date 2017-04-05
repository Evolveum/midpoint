/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.policy;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.impl.AbstractModelImplementationIntegrationTest;
import com.evolveum.midpoint.model.impl.controller.ModelOperationTaskHandler;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.WfTestUtil;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.WorkflowResult;
import com.evolveum.midpoint.wf.impl.processors.general.GeneralChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createRetrieve;
import static com.evolveum.midpoint.schema.GetOperationOptions.resolveItemsNamed;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_OBJECT_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.F_TARGET_REF;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractWfTestPolicy extends AbstractModelImplementationIntegrationTest {

	protected static final File TEST_RESOURCE_DIR = new File("src/test/resources/policy");
	private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_RESOURCE_DIR, "system-configuration.xml");
	public static final File ROLE_SUPERUSER_FILE = new File(TEST_RESOURCE_DIR, "role-superuser.xml");
	public static final File USER_ADMINISTRATOR_FILE = new File(TEST_RESOURCE_DIR, "user-administrator.xml");

	protected static final File USER_JACK_FILE = new File(TEST_RESOURCE_DIR, "user-jack.xml");
	protected static final File USER_LEAD1_FILE = new File(TEST_RESOURCE_DIR, "user-lead1.xml");
	protected static final File USER_LEAD1_DEPUTY_1_FILE = new File(TEST_RESOURCE_DIR, "user-lead1-deputy1.xml");
	protected static final File USER_LEAD1_DEPUTY_2_FILE = new File(TEST_RESOURCE_DIR, "user-lead1-deputy2.xml");
	protected static final File USER_LEAD2_FILE = new File(TEST_RESOURCE_DIR, "user-lead2.xml");
	protected static final File USER_LEAD3_FILE = new File(TEST_RESOURCE_DIR, "user-lead3.xml");
	protected static final File USER_LEAD10_FILE = new File(TEST_RESOURCE_DIR, "user-lead10.xml");
	protected static final File USER_SECURITY_APPROVER_FILE = new File(TEST_RESOURCE_DIR, "user-security-approver.xml");
	protected static final File USER_SECURITY_APPROVER_DEPUTY_FILE = new File(TEST_RESOURCE_DIR, "user-security-approver-deputy.xml");

	protected static final File ROLE_APPROVER_FILE = new File(TEST_RESOURCE_DIR, "041-role-approver.xml");
	protected static final File METAROLE_DEFAULT_FILE = new File(TEST_RESOURCE_DIR, "metarole-default.xml");
	protected static final File METAROLE_SECURITY_FILE = new File(TEST_RESOURCE_DIR, "metarole-security.xml");
	// following 2 are not used by default (assigned when necessary)
	protected static final File METAROLE_PRUNE_TEST2X_ROLES_FILE = new File(TEST_RESOURCE_DIR, "metarole-prune-test2x-roles.xml");
	protected static final File METAROLE_APPROVE_UNASSIGN_FILE = new File(TEST_RESOURCE_DIR, "metarole-approve-unassign.xml");
	protected static final File ROLE_ROLE1_FILE = new File(TEST_RESOURCE_DIR, "role-role1.xml");
	protected static final File ROLE_ROLE1A_FILE = new File(TEST_RESOURCE_DIR, "role-role1a.xml");
	protected static final File ROLE_ROLE1B_FILE = new File(TEST_RESOURCE_DIR, "role-role1b.xml");
	protected static final File ROLE_ROLE2_FILE = new File(TEST_RESOURCE_DIR, "role-role2.xml");
	protected static final File ROLE_ROLE2A_FILE = new File(TEST_RESOURCE_DIR, "role-role2a.xml");
	protected static final File ROLE_ROLE2B_FILE = new File(TEST_RESOURCE_DIR, "role-role2b.xml");
	protected static final File ROLE_ROLE3_FILE = new File(TEST_RESOURCE_DIR, "role-role3.xml");
	protected static final File ROLE_ROLE3A_FILE = new File(TEST_RESOURCE_DIR, "role-role3a.xml");
	protected static final File ROLE_ROLE3B_FILE = new File(TEST_RESOURCE_DIR, "role-role3b.xml");
	protected static final File ROLE_ROLE4_FILE = new File(TEST_RESOURCE_DIR, "role-role4.xml");
	protected static final File ROLE_ROLE4A_FILE = new File(TEST_RESOURCE_DIR, "role-role4a.xml");
	protected static final File ROLE_ROLE4B_FILE = new File(TEST_RESOURCE_DIR, "role-role4b.xml");
	protected static final File ROLE_ROLE10_FILE = new File(TEST_RESOURCE_DIR, "role-role10.xml");
	protected static final File ROLE_ROLE10A_FILE = new File(TEST_RESOURCE_DIR, "role-role10a.xml");
	protected static final File ROLE_ROLE10B_FILE = new File(TEST_RESOURCE_DIR, "role-role10b.xml");
	protected static final File ROLE_FOCUS_ASSIGNMENT_MAPPING = new File(TEST_RESOURCE_DIR, "role-focus-assignment-mapping.xml");

	protected static final File USER_TEMPLATE_ASSIGNING_ROLE_1A = new File(TEST_RESOURCE_DIR, "user-template-assigning-role1a.xml");
	protected static final File USER_TEMPLATE_ASSIGNING_ROLE_1A_AFTER = new File(TEST_RESOURCE_DIR, "user-template-assigning-role1a-after.xml");

	protected static final String USER_ADMINISTRATOR_OID = SystemObjectsType.USER_ADMINISTRATOR.value();

	protected String userJackOid;
	protected String userLead1Oid;
	protected String userLead1Deputy1Oid;
	protected String userLead1Deputy2Oid;
	protected String userLead2Oid;
	protected String userLead3Oid;
	protected String userLead10Oid;
	protected String userSecurityApproverOid;
	protected String userSecurityApproverDeputyOid;

	protected String roleApproverOid;
	protected String metaroleDefaultOid;
	protected String metaroleSecurityOid;
	protected String metarolePruneTest2xRolesOid;
	protected String metaroleApproveUnassign;
	protected String roleRole1Oid;
	protected String roleRole1aOid;
	protected String roleRole1bOid;
	protected String roleRole2Oid;
	protected String roleRole2aOid;
	protected String roleRole2bOid;
	protected String roleRole3Oid;
	protected String roleRole3aOid;
	protected String roleRole3bOid;
	protected String roleRole4Oid;
	protected String roleRole4aOid;
	protected String roleRole4bOid;
	protected String roleRole10Oid;
	protected String roleRole10aOid;
	protected String roleRole10bOid;
	protected String roleFocusAssignmentMapping;

	protected String userTemplateAssigningRole1aOid;
	protected String userTemplateAssigningRole1aOidAfter;

	@Autowired
	protected Clockwork clockwork;

	@Autowired
	protected TaskManager taskManager;

	@Autowired
	protected WorkflowManager workflowManager;

	@Autowired
	protected WfTaskUtil wfTaskUtil;

	@Autowired
	protected ActivitiEngine activitiEngine;

	@Autowired
	protected MiscDataUtil miscDataUtil;

	@Autowired
	protected PrimaryChangeProcessor primaryChangeProcessor;

	@Autowired
	protected GeneralChangeProcessor generalChangeProcessor;

	protected PrismObject<UserType> userAdministrator;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		modelService.postInit(initResult);

		repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
		login(userAdministrator);

		roleApproverOid = repoAddObjectFromFile(ROLE_APPROVER_FILE, initResult).getOid();
		metaroleDefaultOid = repoAddObjectFromFile(METAROLE_DEFAULT_FILE, initResult).getOid();
		metaroleSecurityOid = repoAddObjectFromFile(METAROLE_SECURITY_FILE, initResult).getOid();
		metarolePruneTest2xRolesOid = repoAddObjectFromFile(METAROLE_PRUNE_TEST2X_ROLES_FILE, initResult).getOid();
		metaroleApproveUnassign = repoAddObjectFromFile(METAROLE_APPROVE_UNASSIGN_FILE, initResult).getOid();

		userJackOid = repoAddObjectFromFile(USER_JACK_FILE, initResult).getOid();
		roleRole1Oid = repoAddObjectFromFile(ROLE_ROLE1_FILE, initResult).getOid();
		roleRole1aOid = repoAddObjectFromFile(ROLE_ROLE1A_FILE, initResult).getOid();
		roleRole1bOid = repoAddObjectFromFile(ROLE_ROLE1B_FILE, initResult).getOid();
		roleRole2Oid = repoAddObjectFromFile(ROLE_ROLE2_FILE, initResult).getOid();
		roleRole2aOid = repoAddObjectFromFile(ROLE_ROLE2A_FILE, initResult).getOid();
		roleRole2bOid = repoAddObjectFromFile(ROLE_ROLE2B_FILE, initResult).getOid();
		roleRole3Oid = repoAddObjectFromFile(ROLE_ROLE3_FILE, initResult).getOid();
		roleRole3aOid = repoAddObjectFromFile(ROLE_ROLE3A_FILE, initResult).getOid();
		roleRole3bOid = repoAddObjectFromFile(ROLE_ROLE3B_FILE, initResult).getOid();
		roleRole4Oid = repoAddObjectFromFile(ROLE_ROLE4_FILE, initResult).getOid();
		roleRole4aOid = repoAddObjectFromFile(ROLE_ROLE4A_FILE, initResult).getOid();
		roleRole4bOid = repoAddObjectFromFile(ROLE_ROLE4B_FILE, initResult).getOid();
		roleRole10Oid = repoAddObjectFromFile(ROLE_ROLE10_FILE, initResult).getOid();
		roleRole10aOid = repoAddObjectFromFile(ROLE_ROLE10A_FILE, initResult).getOid();
		roleRole10bOid = repoAddObjectFromFile(ROLE_ROLE10B_FILE, initResult).getOid();
		roleFocusAssignmentMapping = repoAddObjectFromFile(ROLE_FOCUS_ASSIGNMENT_MAPPING, initResult).getOid();

		userLead1Oid = addAndRecomputeUser(USER_LEAD1_FILE, initTask, initResult);
		userLead2Oid = addAndRecomputeUser(USER_LEAD2_FILE, initTask, initResult);
		userLead3Oid = addAndRecomputeUser(USER_LEAD3_FILE, initTask, initResult);
		// LEAD10 will be imported later!
		userSecurityApproverOid = addAndRecomputeUser(USER_SECURITY_APPROVER_FILE, initTask, initResult);
		userSecurityApproverDeputyOid = addAndRecomputeUser(USER_SECURITY_APPROVER_DEPUTY_FILE, initTask, initResult);

		userTemplateAssigningRole1aOid = repoAddObjectFromFile(USER_TEMPLATE_ASSIGNING_ROLE_1A, initResult).getOid();
		userTemplateAssigningRole1aOidAfter = repoAddObjectFromFile(USER_TEMPLATE_ASSIGNING_ROLE_1A_AFTER, initResult).getOid();
	}

	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	protected void importLead10(Task task, OperationResult result) throws Exception {
		userLead10Oid = addAndRecomputeUser(USER_LEAD10_FILE, task, result);
	}

	protected void importLead1Deputies(Task task, OperationResult result) throws Exception {
		userLead1Deputy1Oid = addAndRecomputeUser(USER_LEAD1_DEPUTY_1_FILE, task, result);
		userLead1Deputy2Oid = addAndRecomputeUser(USER_LEAD1_DEPUTY_2_FILE, task, result);
	}

	protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult result) {
		Map<String, WorkflowResult> retval = new HashMap<>();
		retval.put(oid, result);
		return retval;
	}

	protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2,
			WorkflowResult approved2) {
		Map<String, WorkflowResult> retval = new HashMap<String, WorkflowResult>();
		retval.put(oid, approved);
		retval.put(oid2, approved2);
		return retval;
	}

	protected Map<String, WorkflowResult> createResultMap(String oid, WorkflowResult approved, String oid2,
			WorkflowResult approved2, String oid3, WorkflowResult approved3) {
		Map<String, WorkflowResult> retval = new HashMap<String, WorkflowResult>();
		retval.put(oid, approved);
		retval.put(oid2, approved2);
		retval.put(oid3, approved3);
		return retval;
	}

	protected void checkAuditRecords(Map<String, WorkflowResult> expectedResults) {
		checkWorkItemAuditRecords(expectedResults);
		checkWfProcessAuditRecords(expectedResults);
	}

	protected void checkWorkItemAuditRecords(Map<String, WorkflowResult> expectedResults) {
		WfTestUtil.checkWorkItemAuditRecords(expectedResults, dummyAuditService);
	}

	protected void checkWfProcessAuditRecords(Map<String, WorkflowResult> expectedResults) {
		WfTestUtil.checkWfProcessAuditRecords(expectedResults, dummyAuditService);
	}

	protected void removeAllAssignments(String oid, OperationResult result) throws Exception {
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, null, result);
		for (AssignmentType at : user.asObjectable().getAssignment()) {
			ObjectDelta delta = ObjectDelta
					.createModificationDeleteContainer(UserType.class, oid, UserType.F_ASSIGNMENT, prismContext,
							at.asPrismContainerValue().clone());
			repositoryService.modifyObject(UserType.class, oid, delta.getModifications(), result);
			display("Removed assignment " + at + " from " + user);
		}
	}

	public void createObject(final String TEST_NAME, ObjectType object, boolean immediate, boolean approve,
	                         String assigneeOid) throws Exception {
		ObjectDelta<RoleType> addObjectDelta = ObjectDelta.createAddDelta((PrismObject) object.asPrismObject());

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<RoleType> lensContext = createLensContext((Class) object.getClass());
				addFocusDeltaToContext(lensContext, addObjectDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems, OperationResult result) throws Exception {
				if (immediate) {
					assertFalse("There is model context in the root task (it should not be there)",
							wfTaskUtil.hasModelContext(rootTask));
				} else {
					ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
					ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
					assertTrue("Non-empty primary focus delta: " + realDelta0.debugDump(), realDelta0.isEmpty());
					assertNoObject(object);
					ExpectedTask expectedTask = new ExpectedTask(null, "Addition of " + object.getName().getOrig());
					ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(assigneeOid, null, expectedTask);
					assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
							null,
							Collections.singletonList(expectedTask),
							Collections.singletonList(expectedWorkItem));
				}
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertNoObject(object);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				if (approve) {
					assertObject(object);
				} else {
					assertNoObject(object);
				}
			}

			@Override
			protected boolean executeImmediately() {
				return immediate;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(getUser(userLead1Oid));
				return approve;
			}
		}, 1);
	}

	public <T extends ObjectType> void modifyObject(final String TEST_NAME, ObjectDelta<T> objectDelta,
			ObjectDelta<T> expectedDelta0, ObjectDelta<T> expectedDelta1,
			boolean immediate, boolean approve,
			String assigneeOid,
			List<ExpectedTask> expectedTasks, List<ExpectedWorkItem> expectedWorkItems,
			Runnable assertDelta0Executed,
			Runnable assertDelta1NotExecuted, Runnable assertDelta1Executed) throws Exception {

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				Class<T> clazz = objectDelta.getObjectTypeClass();
				//PrismObject<T> object = getObject(clazz, objectDelta.getOid());
				LensContext<T> lensContext = createLensContext(clazz);
				addFocusDeltaToContext(lensContext, objectDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems, OperationResult result) throws Exception {
				if (immediate) {
					assertFalse("There is model context in the root task (it should not be there)",
							wfTaskUtil.hasModelContext(rootTask));
				} else {
					ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
					ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
					assertDeltasEqual("Wrong delta left as primary focus delta.", expectedDelta0, realDelta0);
					assertDelta1NotExecuted.run();
					assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
							objectDelta.getOid(), expectedTasks, expectedWorkItems);
				}
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertDelta0Executed.run();
				assertDelta1NotExecuted.run();
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				assertDelta0Executed.run();
				if (approve) {
					assertDelta1Executed.run();
				} else {
					assertDelta1NotExecuted.run();
				}
			}

			@Override
			protected boolean executeImmediately() {
				return immediate;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				login(getUser(assigneeOid));
				return approve;
			}
		}, 1);
	}

	public <T extends ObjectType> void deleteObject(final String TEST_NAME, Class<T> clazz, String objectOid,
			boolean immediate, boolean approve,
			String assigneeOid,
			List<ExpectedTask> expectedTasks, List<ExpectedWorkItem> expectedWorkItems) throws Exception {

		executeTest(TEST_NAME, new TestDetails() {
			@Override
			protected LensContext createModelContext(OperationResult result) throws Exception {
				LensContext<T> lensContext = createLensContext(clazz);
				ObjectDelta<T> deleteDelta = ObjectDelta.createDeleteDelta(clazz, objectOid, prismContext);
				addFocusDeltaToContext(lensContext, deleteDelta);
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems, OperationResult result) throws Exception {
				if (immediate) {
					assertFalse("There is model context in the root task (it should not be there)",
							wfTaskUtil.hasModelContext(rootTask));
				} else {
					ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
					ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
					assertTrue("Delta0 is not empty: " + realDelta0.debugDump(), realDelta0.isEmpty());
					assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
							objectOid, expectedTasks, expectedWorkItems);
				}
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				assertObjectExists(clazz, objectOid);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				if (approve) {
					assertObjectDoesntExist(clazz, objectOid);
				} else {
					assertObjectExists(clazz, objectOid);
				}
			}

			@Override
			protected boolean executeImmediately() {
				return immediate;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				return approve;
			}
		}, 1);
	}

	protected WorkItemType getWorkItem(Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
		//Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.resolveItemsNamed(WorkItemType.F_TASK_REF);
		SearchResultList<WorkItemType> itemsAll = modelService.searchContainers(WorkItemType.class, null, null, task, result);
		if (itemsAll.size() != 1) {
			System.out.println("Unexpected # of work items: " + itemsAll.size());
			for (WorkItemType workItem : itemsAll) {
				System.out.println(PrismUtil.serializeQuietly(prismContext, workItem));
			}
		}
		assertEquals("Wrong # of total work items", 1, itemsAll.size());
		return itemsAll.get(0);
	}

	protected SearchResultList<WorkItemType> getWorkItems(Task task, OperationResult result) throws Exception {
		return modelService.searchContainers(WorkItemType.class, null, null, task, result);
	}

	protected void displayWorkItems(String title, List<WorkItemType> workItems) {
		workItems.forEach(wi -> display(title, wi));
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

	protected abstract class TestDetails {
		protected LensContext createModelContext(OperationResult result) throws Exception {
			return null;
		}

		protected void afterFirstClockworkRun(Task rootTask, List<Task> wfSubtasks, List<WorkItemType> workItems,
				OperationResult result) throws Exception {
		}

		protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
		}

		protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
		}

		protected boolean executeImmediately() {
			return false;
		}

		protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
			return true;
		}

		public List<ApprovalInstruction> getApprovalSequence() {
			return null;
		}
	}

	protected <F extends FocusType> void executeTest(String testName, TestDetails testDetails, int expectedSubTaskCount)
			throws Exception {

		// GIVEN
		prepareNotifications();
		dummyAuditService.clear();

		Task modelTask = taskManager.createTaskInstance(AbstractWfTestPolicy.class.getName() + "." + testName);
		modelTask.setOwner(userAdministrator);
		OperationResult result = new OperationResult("execution");

		LensContext<F> modelContext = testDetails.createModelContext(result);
		display("Model context at test start", modelContext);

		// this has problems with deleting assignments by ID
		//assertFocusModificationSanity(modelContext);

		// WHEN

		HookOperationMode mode = clockwork.run(modelContext, modelTask, result);

		// THEN

		display("Model context after first clockwork.run", modelContext);
		assertEquals("Unexpected state of the context", ModelState.PRIMARY, modelContext.getState());
		assertEquals("Wrong mode after clockwork.run in " + modelContext.getState(), HookOperationMode.BACKGROUND, mode);
		modelTask.refresh(result);
		display("Model task after first clockwork.run", modelTask);

		String rootTaskOid = wfTaskUtil.getRootTaskOid(modelTask);
		assertNotNull("Root task OID is not set in model task", rootTaskOid);

		Task rootTask = taskManager.getTask(rootTaskOid, result);
		assertTrue("Root task is not persistent", rootTask.isPersistent());

		UriStack uriStack = rootTask.getOtherHandlersUriStack();
		if (!testDetails.executeImmediately()) {
			assertEquals("Invalid handler at stack position 0", ModelOperationTaskHandler.MODEL_OPERATION_TASK_URI,
					uriStack.getUriStackEntry().get(0).getHandlerUri());
		} else {
			assertTrue("There should be no handlers for root tasks with immediate execution mode",
					uriStack == null || uriStack.getUriStackEntry().isEmpty());
		}

		ModelContext rootModelContext = testDetails.executeImmediately() ? null : wfTaskUtil.getModelContext(rootTask, result);
		if (!testDetails.executeImmediately()) {
			assertNotNull("Model context is not present in root task", rootModelContext);
		} else {
			assertNull("Model context is present in root task (execution mode = immediate)", rootModelContext);
		}

		List<Task> subtasks = rootTask.listSubtasks(result);
		Task task0 = findAndRemoveTask0(subtasks, testDetails);

		assertEquals("Incorrect number of subtasks", expectedSubTaskCount, subtasks.size());

		final Collection<SelectorOptions<GetOperationOptions>> options1 = resolveItemsNamed(
				F_OBJECT_REF,
				F_TARGET_REF,
				F_ASSIGNEE_REF,
				F_ORIGINAL_ASSIGNEE_REF,
				new ItemPath(F_TASK_REF, F_WORKFLOW_CONTEXT, F_REQUESTER_REF));

		List<WorkItemType> workItems = modelService.searchContainers(WorkItemType.class, null, options1, modelTask, result);

		testDetails.afterFirstClockworkRun(rootTask, subtasks, workItems, result);

		if (testDetails.executeImmediately()) {
			if (task0 != null) {
				waitForTaskClose(task0, 20000);
			}
			testDetails.afterTask0Finishes(rootTask, result);
		}

		for (int i = 0; i < subtasks.size(); i++) {
			Task subtask = subtasks.get(i);
			PrismProperty<ObjectTreeDeltasType> deltas = subtask.getTaskPrismObject()
					.findProperty(new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE, F_DELTAS_TO_PROCESS));
			assertNotNull("There are no modifications in subtask #" + i + ": " + subtask, deltas);
			assertEquals("Incorrect number of modifications in subtask #" + i + ": " + subtask, 1, deltas.getRealValues().size());
			// todo check correctness of the modification?

			// now check the workflow state
			String pid = wfTaskUtil.getProcessId(subtask);
			assertNotNull("Workflow process instance id not present in subtask " + subtask, pid);

			List<org.activiti.engine.task.Task> tasks = activitiEngine.getTaskService().createTaskQuery().processInstanceId(pid).list();
			assertFalse("activiti task not found", tasks.isEmpty());

			for (org.activiti.engine.task.Task task : tasks) {
				String executionId = task.getExecutionId();
				display("Execution id = " + executionId);
				Boolean approve = testDetails.decideOnApproval(executionId, task);
				if (approve != null) {
					workflowManager.completeWorkItem(task.getId(), approve, null, null, null, result);
					login(userAdministrator);
					break;
				}
			}
		}

		// alternative way of approvals executions
		if (CollectionUtils.isNotEmpty(testDetails.getApprovalSequence())) {
			List<ApprovalInstruction> instructions = new ArrayList<>(testDetails.getApprovalSequence());
			while (!instructions.isEmpty()) {
				List<WorkItemType> currentWorkItems = modelService
						.searchContainers(WorkItemType.class, null, options1, modelTask, result);
				boolean matched = false;
				main:
				for (ApprovalInstruction approvalInstruction : instructions) {
					for (WorkItemType workItem : currentWorkItems) {
						if (approvalInstruction.matches(workItem)) {
							login(getUser(approvalInstruction.approverOid));
							workflowManager.completeWorkItem(workItem.getWorkItemId(), approvalInstruction.approval, null,
									null, null, result);
							login(userAdministrator);
							matched = true;
							instructions.remove(approvalInstruction);
							break main;
						}
					}
				}
				if (!matched) {
					fail("None of approval instructions " + instructions + " matched any of current work items: "
							+ currentWorkItems);
				}
			}
		}

		waitForTaskClose(rootTask, 60000);

		subtasks = rootTask.listSubtasks(result);
		findAndRemoveTask0(subtasks, testDetails);
		testDetails.afterRootTaskFinishes(rootTask, subtasks, result);

		notificationManager.setDisabled(true);

		// Check audit
		display("Audit", dummyAuditService);
		display("Output context", modelContext);
	}

	private Task findAndRemoveTask0(List<Task> subtasks, TestDetails testDetails) {
		Task task0 = null;

		for (Task subtask : subtasks) {
			if (subtask.getTaskPrismObject().asObjectable().getWorkflowContext() == null
					|| subtask.getTaskPrismObject().asObjectable().getWorkflowContext().getProcessInstanceId() == null) {
				assertNull("More than one non-wf-monitoring subtask", task0);
				task0 = subtask;
			}
		}

		if (testDetails.executeImmediately()) {
			if (task0 != null) {
				subtasks.remove(task0);
			}
		} else {
			assertNull("Subtask for immediate execution was found even if it shouldn't be there", task0);
		}
		return task0;
	}

	protected void assertObjectInTaskTree(Task rootTask, String oid, boolean checkObjectOnSubtasks, OperationResult result)
			throws SchemaException {
		assertObjectInTask(rootTask, oid);
		if (checkObjectOnSubtasks) {
			for (Task task : rootTask.listSubtasks(result)) {
				assertObjectInTask(task, oid);
			}
		}
	}

	protected void assertObjectInTask(Task task, String oid) {
		assertEquals("Missing or wrong object OID in task " + task, oid, task.getObjectOid());
	}

	protected void waitForTaskClose(final Task task, final int timeout) throws Exception {
		final OperationResult waitResult = new OperationResult(AbstractIntegrationTest.class + ".waitForTaskClose");
		Checker checker = new Checker() {
			@Override
			public boolean check() throws CommonException {
				task.refresh(waitResult);
				OperationResult result = task.getResult();
				if (verbose)
					display("Check result", result);
				return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
			}

			@Override
			public void timeout() {
				try {
					task.refresh(waitResult);
				} catch (Throwable e) {
					display("Exception during task refresh", e);
				}
				OperationResult result = task.getResult();
				display("Result of timed-out task", result);
				assert false : "Timeout (" + timeout + ") while waiting for " + task + " to finish. Last result " + result;
			}
		};
		IntegrationTestTools.waitFor("Waiting for " + task + " finish", checker, timeout, 1000);
	}

	protected void assertWfContextAfterClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
			OperationResult result,
			String objectOid,
			List<ExpectedTask> expectedTasks,
			List<ExpectedWorkItem> expectedWorkItems) throws Exception {

		final Collection<SelectorOptions<GetOperationOptions>> options =
				SelectorOptions.createCollection(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM), createRetrieve());

		Task opTask = taskManager.createTaskInstance();
		TaskType rootTaskType = modelService.getObject(TaskType.class, rootTask.getOid(), options, opTask, result).asObjectable();
		display("rootTask", rootTaskType);
		assertTrue("unexpected process instance id in root task",
				rootTaskType.getWorkflowContext() == null || rootTaskType.getWorkflowContext().getProcessInstanceId() == null);

		assertEquals("Wrong # of wf subtasks (" + expectedTasks + ")", expectedTasks.size(), subtasks.size());
		int i = 0;
		for (Task subtask : subtasks) {
			TaskType subtaskType = modelService.getObject(TaskType.class, subtask.getOid(), options, opTask, result).asObjectable();
			display("Subtask #" + (i + 1) + ": ", subtaskType);
			checkTask(subtaskType, subtask.toString(), expectedTasks.get(i));
			WfTestUtil
					.assertRef("requester ref", subtaskType.getWorkflowContext().getRequesterRef(), USER_ADMINISTRATOR_OID, false, false);
			i++;
		}

		assertEquals("Wrong # of work items", expectedWorkItems.size(), workItems.size());
		i = 0;
		for (WorkItemType workItem : workItems) {
			display("Work item #" + (i + 1) + ": ", workItem);
			display("Task ref",
					workItem.getTaskRef() != null ? workItem.getTaskRef().asReferenceValue().debugDump(0, true) : null);
			if (objectOid != null) {
				WfTestUtil.assertRef("object reference", workItem.getObjectRef(), objectOid, true, true);
			}

			String targetOid = expectedWorkItems.get(i).targetOid;
			if (targetOid != null) {
				WfTestUtil.assertRef("target reference", workItem.getTargetRef(), targetOid, true, true);
			}
			WfTestUtil
					.assertRef("assignee reference", workItem.getOriginalAssigneeRef(), expectedWorkItems.get(i).assigneeOid, false, true);
			// name is not known, as it is not stored in activiti (only OID is)
			WfTestUtil.assertRef("task reference", workItem.getTaskRef(), null, false, true);
			final TaskType subtaskType = (TaskType) ObjectTypeUtil.getObjectFromReference(workItem.getTaskRef());
			checkTask(subtaskType, "task in workItem", expectedWorkItems.get(i).task);
			WfTestUtil
					.assertRef("requester ref", subtaskType.getWorkflowContext().getRequesterRef(), USER_ADMINISTRATOR_OID, false, true);

			i++;
		}
	}

	private void checkTask(TaskType subtaskType, String subtaskName, ExpectedTask expectedTask) {
		assertNull("Unexpected fetch result in wf subtask: " + subtaskName, subtaskType.getFetchResult());
		WfContextType wfc = subtaskType.getWorkflowContext();
		assertNotNull("Missing workflow context in wf subtask: " + subtaskName, wfc);
		assertNotNull("No process ID in wf subtask: " + subtaskName, wfc.getProcessInstanceId());
		assertEquals("Wrong process ID name in subtask: " + subtaskName, expectedTask.processName, wfc.getProcessInstanceName());
		if (expectedTask.targetOid != null) {
			assertEquals("Wrong target OID in subtask: " + subtaskName, expectedTask.targetOid, wfc.getTargetRef().getOid());
		} else {
			assertNull("TargetRef in subtask: " + subtaskName + " present even if it shouldn't", wfc.getTargetRef());
		}
		assertNotNull("Missing process start time in subtask: " + subtaskName, wfc.getStartTimestamp());
		assertNull("Unexpected process end time in subtask: " + subtaskName, wfc.getEndTimestamp());
		assertEquals("Wrong outcome", null, wfc.getOutcome());
		//assertEquals("Wrong state", null, wfc.getState());
	}

	protected String getTargetOid(String executionId)
			throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
			SecurityViolationException {
		LightweightObjectRef targetRef = (LightweightObjectRef) activitiEngine.getRuntimeService()
				.getVariable(executionId, CommonProcessVariableNames.VARIABLE_TARGET_REF);
		assertNotNull("targetRef not found", targetRef);
		String roleOid = targetRef.getOid();
		assertNotNull("requested role OID not found", roleOid);
		return roleOid;
	}

	protected void checkTargetOid(String executionId, String expectedOid)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
			SecurityViolationException {
		String realOid = getTargetOid(executionId);
		assertEquals("Unexpected target OID", expectedOid, realOid);
	}

	protected abstract class TestDetails2<F extends FocusType> {
		protected PrismObject<F> getFocus(OperationResult result) throws Exception { return null; }
		protected ObjectDelta<F> getFocusDelta() throws Exception { return null; }
		protected int getNumberOfDeltasToApprove() { return 0; }
		protected List<Boolean> getApprovals() { return null; }
		protected List<ObjectDelta<F>> getExpectedDeltasToApprove() {
			return null;
		}
		protected ObjectDelta<F> getExpectedDelta0() {
			return null;
		}
		protected String getObjectOid() {
			return null;
		}
		protected List<ExpectedTask> getExpectedTasks() { return null; }
		protected List<ExpectedWorkItem> getExpectedWorkItems() { return null; }

		protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception { }
		protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception { return true; }

		protected void sortSubtasks(List<Task> subtasks) {
			Collections.sort(subtasks, Comparator.comparing(this::getCompareKey));
		}

		protected void sortWorkItems(List<WorkItemType> workItems) {
			Collections.sort(workItems, Comparator.comparing(this::getCompareKey));
		}

		protected String getCompareKey(Task task) {
			return task.getTaskPrismObject().asObjectable().getWorkflowContext().getTargetRef().getOid();
		}

		protected String getCompareKey(WorkItemType workItem) {
			return workItem.getOriginalAssigneeRef().getOid();
		}

		public List<ApprovalInstruction> getApprovalSequence() {
			return null;
		}
	}

	protected <F extends FocusType> void executeTest2(String testName, TestDetails2<F> testDetails2, int expectedSubTaskCount,
			boolean immediate) throws Exception {
		executeTest(testName, new TestDetails() {
			@Override
			protected LensContext<F> createModelContext(OperationResult result) throws Exception {
				PrismObject<F> focus = testDetails2.getFocus(result);
				// TODO "object create" context
				LensContext<F> lensContext = createLensContext(focus.getCompileTimeClass());
				fillContextWithFocus(lensContext, focus);
				addFocusDeltaToContext(lensContext, testDetails2.getFocusDelta());
				if (immediate) {
					lensContext.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
				}
				return lensContext;
			}

			@Override
			protected void afterFirstClockworkRun(Task rootTask, List<Task> subtasks, List<WorkItemType> workItems,
					OperationResult result) throws Exception {
				if (immediate) {
					assertFalse("There is model context in the root task (it should not be there)",
							wfTaskUtil.hasModelContext(rootTask));
				} else {
					ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
					ObjectDelta expectedDelta0 = testDetails2.getExpectedDelta0();
					ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
					assertDeltasEqual("Wrong delta left as primary focus delta. ", expectedDelta0, realDelta0);
					for (int i = 0; i <= testDetails2.getNumberOfDeltasToApprove(); i++) {
						testDetails2.assertDeltaExecuted(i, false, rootTask, result);
					}
					testDetails2.sortSubtasks(subtasks);
					testDetails2.sortWorkItems(workItems);
					assertWfContextAfterClockworkRun(rootTask, subtasks, workItems, result,
							testDetails2.getObjectOid(),
							testDetails2.getExpectedTasks(), testDetails2.getExpectedWorkItems());
				}
			}

			@Override
			protected void afterTask0Finishes(Task task, OperationResult result) throws Exception {
				if (!immediate) {
					return;
				}
				for (int i = 1; i <= testDetails2.getNumberOfDeltasToApprove(); i++) {
					testDetails2.assertDeltaExecuted(i, false, task, result);
				}
				testDetails2.assertDeltaExecuted(0, true, task, result);
			}

			@Override
			protected void afterRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
				for (int i = 0; i <= testDetails2.getNumberOfDeltasToApprove(); i++) {
					testDetails2.assertDeltaExecuted(i, i == 0 || testDetails2.getApprovals().get(i-1), task, result);
				}
			}

			@Override
			protected boolean executeImmediately() {
				return immediate;
			}

			@Override
			protected Boolean decideOnApproval(String executionId, org.activiti.engine.task.Task task) throws Exception {
				return testDetails2.decideOnApproval(executionId, task);
			}

			@Override
			public List<ApprovalInstruction> getApprovalSequence() {
				return testDetails2.getApprovalSequence();
			}
		}, expectedSubTaskCount);
	}

	protected void assertDeltasEqual(String message, ObjectDelta expectedDelta, ObjectDelta realDelta) {
//		removeOldValues(expectedDelta);
//		removeOldValues(realDelta);
		if (!expectedDelta.equivalent(realDelta)) {
			fail(message + "\nExpected:\n" + expectedDelta.debugDump() + "\nReal:\n" + realDelta.debugDump());
		}
	}

//	private void removeOldValues(ObjectDelta<?> delta) {
//		if (delta.isModify()) {
//			delta.getModifications().forEach(mod -> mod.setEstimatedOldValues(null));
//		}
//	}

	protected void assertNoObject(ObjectType object) throws SchemaException, com.evolveum.midpoint.util.exception.ObjectNotFoundException, com.evolveum.midpoint.util.exception.SecurityViolationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException {
		assertNull("Object was created but it shouldn't be",
				searchObjectByName(object.getClass(), object.getName().getOrig()));
	}

	protected <T extends ObjectType> void assertObject(T object) throws SchemaException, com.evolveum.midpoint.util.exception.ObjectNotFoundException, com.evolveum.midpoint.util.exception.SecurityViolationException, com.evolveum.midpoint.util.exception.CommunicationException, com.evolveum.midpoint.util.exception.ConfigurationException {
		PrismObject<T> objectFromRepo = searchObjectByName((Class<T>) object.getClass(), object.getName().getOrig());
		assertNotNull("Object " + object + " was not created", object);
		objectFromRepo.removeItem(new ItemPath(ObjectType.F_METADATA), Item.class);
		assertEquals("Object is different from the one that was expected", object, objectFromRepo.asObjectable());
	}

}