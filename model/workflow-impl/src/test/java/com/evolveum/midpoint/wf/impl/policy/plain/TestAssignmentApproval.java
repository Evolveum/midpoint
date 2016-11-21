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
package com.evolveum.midpoint.wf.impl.policy.plain;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.policy.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.processes.common.WorkflowResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-workflow-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentApproval extends AbstractWfTestPolicy {

    protected static final Trace LOGGER = TraceManager.getTrace(TestAssignmentApproval.class);

    /**
     * The simplest case: addition of an assignment of single security-sensitive role (Role1).
	 * Although it induces Role10 membership, it is not a problem, as Role10 approver (Lead10) is not imported yet.
     */
	@Test
    public void test010AddRole1Assignment() throws Exception {
        final String TEST_NAME = "test010AddRole1Assignment";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(userAdministrator);

		executeAssignRole1ToJack(TEST_NAME, false, false, null);
	}

	/**
	 * Removing recently added assignment of single security-sensitive role. Should execute without approval (for now).
	 */
	@Test
	public void test020DeleteRole1Assignment() throws Exception {
		final String TEST_NAME = "test020DeleteRole1Assignment";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		LensContext<UserType> context = createUserAccountContext();
		fillContextWithUser(context, USER_JACK_OID, result);
		addFocusDeltaToContext(context,
				(ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
						.item(UserType.F_ASSIGNMENT).delete(createAssignmentTo(ROLE_ROLE1_OID, ObjectTypes.ROLE, prismContext))
						.asObjectDelta(USER_JACK_OID));
		clockwork.run(context, task, result);

		assertEquals("Wrong context state", ModelState.FINAL, context.getState());
		TestUtil.assertSuccess(result);
		assertNotAssignedRole(getUser(USER_JACK_OID), ROLE_ROLE1_OID, task, result);
	}

	/**
	 * Repeating test010; this time with Lead10 present. So we are approving an assignment of single security-sensitive role (Role1),
	 * that induces another security-sensitive role (Role10). Because of current implementation constraints, only the first assignment
	 * should be brought to approval.
	 */
	@Test
	public void test030AddRole1AssignmentAgain() throws Exception {
		final String TEST_NAME = "test030AddRole1AssignmentAgain";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		importLead10(task, task.getResult());

		executeAssignRole1ToJack(TEST_NAME, false, false, null);
	}

	@Test
	public void test040AddRole1AssignmentImmediate() throws Exception {
		final String TEST_NAME = "test040AddRole1AssignmentImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRole1ToJack(TEST_NAME, true, false, null);
	}

	@Test
	public void test050AddRoles123AssignmentNNN() throws Exception {
		final String TEST_NAME = "test050AddRoles123AssignmentNNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, false, false, false, false);
	}

	@Test
 	public void test052AddRoles123AssignmentNNNImmediate() throws Exception {
		final String TEST_NAME = "test052AddRoles123AssignmentNNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, true, false, false, false);
	}

	@Test
	public void test060AddRoles123AssignmentYNN() throws Exception {
		final String TEST_NAME = "test060AddRoles123AssignmentYNN";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, false, true, false, false);
	}

	@Test
	public void test062AddRoles123AssignmentYNNImmediate() throws Exception {
		final String TEST_NAME = "test062AddRoles123AssignmentYNNImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, true, true, false, false);
	}

	@Test
	public void test070AddRoles123AssignmentYYY() throws Exception {
		final String TEST_NAME = "test070AddRoles123AssignmentYYY";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, false, true, true, true);
	}

	@Test
	public void test072AddRoles123AssignmentYYYImmediate() throws Exception {
		final String TEST_NAME = "test072AddRoles123AssignmentYYYImmediate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		unassignAllRoles(USER_JACK_OID);
		executeAssignRoles123ToJack(TEST_NAME, true, true, true, true);
	}

	/**
	 * Repeating test010; this time with Lead10 present. So we are approving an assignment of single security-sensitive role (Role1),
	 * that induces another security-sensitive role (Role10). Because of current implementation constraints, only the first assignment
	 * should be brought to approval.
	 */
	@Test
	public void test130AddRole1AssignmentWithDeputy() throws Exception {
		final String TEST_NAME = "test130AddRole1AssignmentWithDeputy";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		Task task = createTask(TEST_NAME);
		importLead1Deputies(task, task.getResult());

		executeAssignRole1ToJack(TEST_NAME, false, true, null);
	}

	@Test
	public void test132AddRole1AssignmentWithDeputyApprovedByDeputy1() throws Exception {
		final String TEST_NAME = "test132AddRole1AssignmentWithDeputyApprovedByDeputy1";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(userAdministrator);

		executeAssignRole1ToJack(TEST_NAME, false, true, USER_LEAD1_DEPUTY_1_OID);
	}


	private void executeAssignRole1ToJack(String TEST_NAME, boolean immediate, boolean deputy, String approverOid) throws Exception {
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		ObjectDelta<UserType> addRole1Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE1_OID, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		String realApproverOid = approverOid != null ? approverOid : USER_LEAD1_OID;
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) throws Exception {
				return jack.clone();
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws SchemaException {
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
				return ObjectDelta.createModifyDelta(jack.getOid(), Collections.emptyList(), UserType.class, prismContext);
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<String> getExpectedTargetOids() {
				return Collections.singletonList(ROLE_ROLE1_OID);
			}

			@Override
			protected List<String> getExpectedAssigneeOids() {
				return Arrays.asList(USER_LEAD1_OID, USER_LEAD1_DEPUTY_1_OID, USER_LEAD1_DEPUTY_2_OID);
			}

			@Override
			protected List<String> getExpectedProcessNames() {
				return Collections.singletonList("Assigning Role1 to jack");
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				if (number == 1) {
					if (yes) {
						assertAssignedRole(USER_JACK_OID, ROLE_ROLE1_OID, rootTask, result);
						checkWorkItemAuditRecords(createResultMap(ROLE_ROLE1_OID, WorkflowResult.APPROVED));
						checkUserApprovers(USER_JACK_OID, Collections.singletonList(realApproverOid), result);
					} else {
						assertNotAssignedRole(USER_JACK_OID, ROLE_ROLE1_OID, rootTask, result);
					}
				}
			}

			@Override
			protected boolean decideOnApproval(String executionId) throws Exception {
				checkTargetOid(executionId, ROLE_ROLE1_OID);
				login(getUser(realApproverOid));
				return true;
			}
		}, 1, immediate);
	}

	private void executeAssignRoles123ToJack(String TEST_NAME, boolean immediate, boolean approve1, boolean approve2, boolean approve3) throws Exception {
		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		ObjectDelta<UserType> addRole1Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE1_OID, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		ObjectDelta<UserType> addRole2Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE2_OID, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		ObjectDelta<UserType> addRole3Delta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(createAssignmentTo(ROLE_ROLE3_OID, ObjectTypes.ROLE, prismContext))
				.asObjectDelta(USER_JACK_OID);
		ObjectDelta<UserType> changeDescriptionDelta = (ObjectDelta<UserType>) DeltaBuilder
				.deltaFor(UserType.class, prismContext)
				.item(UserType.F_DESCRIPTION).replace(TEST_NAME)
				.asObjectDelta(USER_JACK_OID);
		ObjectDelta<UserType> primaryDelta = ObjectDelta.summarize(addRole1Delta, addRole2Delta, addRole3Delta, changeDescriptionDelta);
		String originalDescription = getUser(USER_JACK_OID).asObjectable().getDescription();
		executeTest2(TEST_NAME, new TestDetails2<UserType>() {
			@Override
			protected PrismObject<UserType> getFocus(OperationResult result) throws Exception {
				return jack.clone();
			}

			@Override
			protected ObjectDelta<UserType> getFocusDelta() throws SchemaException {
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
				return changeDescriptionDelta.clone();
			}

			@Override
			protected String getObjectOid() {
				return jack.getOid();
			}

			@Override
			protected List<String> getExpectedTargetOids() {
				return Arrays.asList(ROLE_ROLE1_OID, ROLE_ROLE2_OID, ROLE_ROLE3_OID);
			}

			@Override
			protected List<String> getExpectedAssigneeOids() {
				return Arrays.asList(USER_LEAD1_OID, USER_LEAD2_OID, USER_LEAD3_OID);
			}

			@Override
			protected List<String> getExpectedProcessNames() {
				return Arrays.asList("Assigning Role1 to jack", "Assigning Role2 to jack", "Assigning Role3 to jack");
			}

			@Override
			protected void assertDeltaExecuted(int number, boolean yes, Task rootTask, OperationResult result) throws Exception {
				String rolesOids[] = { ROLE_ROLE1_OID, ROLE_ROLE2_OID, ROLE_ROLE3_OID };
				switch(number) {
					case 0:
						if (yes) {
							assertUserProperty(USER_JACK_OID, UserType.F_DESCRIPTION, TEST_NAME);
						} else {
							if (originalDescription != null) {
								assertUserProperty(USER_JACK_OID, UserType.F_DESCRIPTION, originalDescription);
							} else {
								assertUserNoProperty(USER_JACK_OID, UserType.F_DESCRIPTION);
							}
						}
						break;
					case 1:
					case 2:
					case 3:
					if (yes) {
						assertAssignedRole(USER_JACK_OID, rolesOids[number-1], rootTask, result);
					} else {
						assertNotAssignedRole(USER_JACK_OID, rolesOids[number-1], rootTask, result);
					}
					break;

				}
			}

			@Override
			protected boolean decideOnApproval(String executionId) throws Exception {
				String targetOid = getTargetOid(executionId);
				if (ROLE_ROLE1_OID.equals(targetOid)) {
					login(getUser(USER_LEAD1_OID));
					return approve1;
				} else if (ROLE_ROLE2_OID.equals(targetOid)) {
					login(getUser(USER_LEAD2_OID));
					return approve2;
				} else if (ROLE_ROLE3_OID.equals(targetOid)) {
					login(getUser(USER_LEAD3_OID));
					return approve3;
				} else {
					throw new IllegalStateException("Unexpected approval request for " + targetOid);
				}
			}
		}, 3, immediate);
	}

	//
//    /**
//     * User modification with one security-sensitive role and other (unrelated) change - e.g. change of the given name.
//     * Aggregated execution.
//     */
//
//    @Test(enabled = true)
//    public void test011UserModifyAddRoleChangeGivenName() throws Exception {
//        TestUtil.displayTestTile(this, "test011UserModifyAddRoleChangeGivenName");
//        login(userAdministrator);
//
//        executeTest("test011UserModifyAddRoleChangeGivenName", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE2_CHANGE_GN);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There is wrong number of modifications left in primary focus delta", 1, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//                ItemDelta givenNameDelta = (ItemDelta) taskModelContext.getFocusContext().getPrimaryDelta().getModifications().iterator().next();
//
//                assertNotNull("givenName delta is incorrect (not a replace delta)", givenNameDelta.isReplace());
//                assertEquals("givenName delta is incorrect (wrong value)", "JACK", ((PrismPropertyValue<PolyString>) givenNameDelta.getValuesToReplace().iterator().next()).getValue().getOrig());
//
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertNotAssignedRole(jack, ROLE_R2_OID);
//                assertEquals("Wrong given name before change", "Jack", jack.asObjectable().getGivenName().getOrig());
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertNotAssignedRole(jack, ROLE_R2_OID);
//                assertEquals("Wrong given name after change", "JACK", jack.asObjectable().getGivenName().getOrig());
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//                checkWorkItemAuditRecords(createResultMap(ROLE_R2_OID, WorkflowResult.REJECTED));
//                checkUserApprovers(USER_JACK_OID, new ArrayList<String>(), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                return decideOnRoleApproval(executionId);
//            }
//
//        });
//    }
//
//    @Test(enabled = true)
//    public void test012UserModifyAddRoleChangeGivenNameImmediate() throws Exception {
//        TestUtil.displayTestTile(this, "test012UserModifyAddRoleChangeGivenNameImmediate");
//        login(userAdministrator);
//        executeTest("test012UserModifyAddRoleChangeGivenNameImmediate", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 2; }
//            @Override boolean immediate() { return true; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE3_CHANGE_GN2);
//                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                assertFalse("There is model context in the root task (it should not be there)", wfTaskUtil.hasModelContext(rootTask));
//            }
//
//            @Override
//            void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertNotAssignedRole(jack, ROLE_R3_OID);
//                assertEquals("Wrong given name after immediate execution", "J-A-C-K", jack.asObjectable().getGivenName().getOrig());
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertAssignedRole(jack, ROLE_R3_OID);
//
//                checkDummyTransportMessages("simpleUserNotifier", 2);
//                checkWorkItemAuditRecords(createResultMap(ROLE_R3_OID, WorkflowResult.APPROVED));
//                checkUserApprovers(USER_JACK_OID, Arrays.asList(R3BOSS_OID), result);         // given name is changed before role is added, so the approver should be recorded
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                return decideOnRoleApproval(executionId);
//            }
//
//        });
//    }
//
//
//    @Test(enabled = true)
//    public void test020UserModifyAddThreeRoles() throws Exception {
//        TestUtil.displayTestTile(this, "test020UserModifyAddThreeRoles");
//        login(userAdministrator);
//        executeTest("test020UserModifyAddThreeRoles", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 2; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ACTIVATION_DISABLE);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There is wrong number of modifications left in primary focus delta", 2, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//                Iterator<? extends ItemDelta> it = taskModelContext.getFocusContext().getPrimaryDelta().getModifications().iterator();
//                ItemDelta addRoleDelta = null, activationChange = null;
//                while (it.hasNext()) {
//                    ItemDelta mod = it.next();
//                    if (mod.isAdd()) {
//                        addRoleDelta = mod;
//                    } else if (mod.isReplace()) {
//                        activationChange = mod;
//                    }
//                }
//                assertNotNull("role add delta was not found", addRoleDelta);
//                assertEquals("role add delta contains wrong number of values", 1, addRoleDelta.getValuesToAdd().size());
//                assertNotNull("activation change delta was not found", activationChange);
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = getUserFromRepo(USER_JACK_OID, result);
//                assertNotAssignedRole(jack, ROLE_R1_OID);
//                assertNotAssignedRole(jack, ROLE_R2_OID);
//                assertAssignedRole(jack, ROLE_R3_OID);
//                assertAssignedRole(jack, ROLE_R4_OID);
//                assertEquals("activation has not been changed", ActivationStatusType.DISABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//                checkWorkItemAuditRecords(createResultMap(ROLE_R2_OID, WorkflowResult.REJECTED, ROLE_R3_OID, WorkflowResult.APPROVED));
//                checkUserApprovers(USER_JACK_OID, Arrays.asList(R3BOSS_OID), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                return decideOnRoleApproval(executionId);
//            }
//
//        });
//    }
//
//    @Test(enabled = true)
//    public void test021UserModifyAddThreeRolesImmediate() throws Exception {
//        TestUtil.displayTestTile(this, "test021UserModifyAddThreeRolesImmediate");
//        login(userAdministrator);
//        executeTest("test021UserModifyAddThreeRolesImmediate", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 3; }
//            @Override boolean immediate() { return true; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLES2_3_4);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ACTIVATION_ENABLE);
//                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                assertFalse("There is model context in the root task (it should not be there)", wfTaskUtil.hasModelContext(rootTask));
//            }
//
//            @Override
//            void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertNotAssignedRole(jack, ROLE_R1_OID);
//                assertNotAssignedRole(jack, ROLE_R2_OID);
//                assertNotAssignedRole(jack, ROLE_R3_OID);
//                assertAssignedRole(jack, ROLE_R4_OID);
//                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
//                checkUserApprovers(USER_JACK_OID, new ArrayList<String>(), result);
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = getUserFromRepo(USER_JACK_OID, result);
//                assertNotAssignedRole(jack, ROLE_R1_OID);
//                assertNotAssignedRole(jack, ROLE_R2_OID);
//                assertAssignedRole(jack, ROLE_R3_OID);
//                assertAssignedRole(jack, ROLE_R4_OID);
//                assertEquals("activation has not been changed", ActivationStatusType.ENABLED, jack.asObjectable().getActivation().getAdministrativeStatus());
//
//                checkDummyTransportMessages("simpleUserNotifier", 2);
//                checkWorkItemAuditRecords(createResultMap(ROLE_R2_OID, WorkflowResult.REJECTED, ROLE_R3_OID, WorkflowResult.APPROVED));
//                checkUserApprovers(USER_JACK_OID, Arrays.asList(R3BOSS_OID), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                return decideOnRoleApproval(executionId);
//            }
//
//        });
//    }
//
//    @Test(enabled = true)
//    public void test030UserAdd() throws Exception {
//        TestUtil.displayTestTile(this, "test030UserAdd");
//        login(userAdministrator);
//        executeTest("test030UserAdd", null, new TestDetails() {
//            @Override int subtaskCount() { return 2; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return false; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                PrismObject<UserType> bill = prismContext.parseObject(USER_BILL_FILE);
//                fillContextWithAddUserDelta(context, bill);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                PrismObject<UserType> objectToAdd = taskModelContext.getFocusContext().getPrimaryDelta().getObjectToAdd();
//                assertNotNull("There is no object to add left in primary focus delta", objectToAdd);
//                assertFalse("There is assignment of R1 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R1_OID));
//                assertFalse("There is assignment of R2 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R2_OID));
//                assertFalse("There is assignment of R3 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R3_OID));
//                assertTrue("There is no assignment of R4 in reduced primary focus delta", assignmentExists(objectToAdd.asObjectable().getAssignment(), ROLE_R4_OID));
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> bill = findUserInRepo("bill", result);
//                assertAssignedRole(bill, ROLE_R1_OID);
//                assertNotAssignedRole(bill, ROLE_R2_OID);
//                assertNotAssignedRole(bill, ROLE_R3_OID);
//                assertAssignedRole(bill, ROLE_R4_OID);
//                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignment().size());
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//                checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED, ROLE_R2_OID, WorkflowResult.REJECTED));
//                checkUserApproversForCreate(bill.getOid(), Arrays.asList(R1BOSS_OID), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                return decideOnRoleApproval(executionId);
//            }
//
//            @Override
//            String getObjectOid(Task task, OperationResult result) throws SchemaException {
//                //return findUserInRepo("bill", result).getOid();
//                return DONT_CHECK;        // don't check in this case
//            }
//        });
//    }
//
//    @Test(enabled = true)
//    public void test031UserAddImmediate() throws Exception {
//        TestUtil.displayTestTile(this, "test031UserAddImmediate");
//        login(userAdministrator);
//
//        deleteUserFromModel("bill");
//
//        executeTest("test031UserAddImmediate", null, new TestDetails() {
//            @Override int subtaskCount() { return 3; }
//            @Override boolean immediate() { return true; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                PrismObject<UserType> bill = prismContext.parseObject(USER_BILL_FILE);
//                fillContextWithAddUserDelta(context, bill);
//                context.setOptions(ModelExecuteOptions.createExecuteImmediatelyAfterApproval());
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                assertFalse("There is model context in the root task (it should not be there)", wfTaskUtil.hasModelContext(rootTask));
//            }
//
//            @Override
//            void assertsAfterImmediateExecutionFinished(Task task, OperationResult result) throws Exception {
//                PrismObject<UserType> bill = findUserInRepo("bill", result);
//                assertNotAssignedRole(bill, ROLE_R1_OID);
//                assertNotAssignedRole(bill, ROLE_R2_OID);
//                assertNotAssignedRole(bill, ROLE_R3_OID);
//                assertAssignedRole(bill, ROLE_R4_OID);
//                //assertEquals("Wrong number of assignments for bill", 3, bill.asObjectable().getAssignment().size());
//                checkUserApproversForCreate(USER_JACK_OID, new ArrayList<>(), result);
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> bill = findUserInRepo("bill", result);
//                assertAssignedRole(bill, ROLE_R1_OID);
//                assertNotAssignedRole(bill, ROLE_R2_OID);
//                assertNotAssignedRole(bill, ROLE_R3_OID);
//                assertAssignedRole(bill, ROLE_R4_OID);
//                //assertEquals("Wrong number of assignments for bill", 4, bill.asObjectable().getAssignment().size());
//
//                checkDummyTransportMessages("simpleUserNotifier", 2);
//                checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED, ROLE_R2_OID, WorkflowResult.REJECTED));
//                checkUserApprovers(bill.getOid(), Arrays.asList(R1BOSS_OID), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                return decideOnRoleApproval(executionId);
//            }
//
//            @Override
//            String getObjectOid(Task task, OperationResult result) throws SchemaException {
//                return findUserInRepo("bill", result).getOid();
//            }
//        });
//    }
//
//    @Test(enabled = true)
//    public void test040UserModifyPasswordChangeBlocked() throws Exception {
//        TestUtil.displayTestTile(this, "test040UserModifyPasswordChangeBlocked");
//        login(userAdministrator);
//
//        PrismObject<UserType> jack = getUser(USER_JACK_OID);
//        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
//        LOGGER.trace("password before test = " + originalPasswordValue);
//
//        executeTest("test040UserModifyPasswordChangeBlocked", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                encryptAndAddFocusModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD);
//                //context.setOptions(ModelExecuteOptions.createNoCrypt());
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = getUser(USER_JACK_OID);
//                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
//                LOGGER.trace("password after test = " + afterTestPasswordValue);
//
//                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
//                assertTrue("password was changed", originalPasswordValue.getEncryptedDataType().equals(afterTestPasswordValue.getEncryptedDataType()));
//
//                checkDummyTransportMessages("simpleUserNotifier", 0);
//                // we don't check for modifyApproverRef because in this test the value was not changed (no change was executed)
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                login(getUser(USER_ADMINISTRATOR_OID));
//                return false;
//            }
//        });
//    }
//
//    @Test(enabled = true)
//    public void test041UserModifyPasswordChange() throws Exception {
//        TestUtil.displayTestTile(this, "test041UserModifyPasswordChange");
//        login(userAdministrator);
//        PrismObject<UserType> jack = getUser(USER_JACK_OID);
//        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
//        LOGGER.trace("password before test = " + originalPasswordValue);
//
//        executeTest("test041UserModifyPasswordChange", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                encryptAndAddFocusModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD);
//                //context.setOptions(ModelExecuteOptions.createNoCrypt());
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = getUser(USER_JACK_OID);
//                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
//                LOGGER.trace("password after test = " + afterTestPasswordValue);
//
//                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
//                assertFalse("password was not changed", originalPasswordValue.getEncryptedDataType().equals(afterTestPasswordValue.getEncryptedDataType()));
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                login(getUser(USER_ADMINISTRATOR_OID));
//                return true;
//            }
//        });
//    }
//
//    @Test(enabled = true)
//    public void test050UserModifyAddRoleAndPasswordChange() throws Exception {
//        TestUtil.displayTestTile(this, "test050UserModifyAddRoleAndPasswordChange");
//        login(userAdministrator);
//        PrismObject<UserType> jack = getUser(USER_JACK_OID);
//        final ProtectedStringType originalPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
//        LOGGER.trace("password before test = " + originalPasswordValue);
//
//        executeTest("test050UserModifyAddRoleAndPasswordChange", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 2; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                encryptAndAddFocusModificationToContext(context, REQ_USER_JACK_MODIFY_CHANGE_PASSWORD_2);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE1);
//                //context.setOptions(ModelExecuteOptions.createNoCrypt());
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = getUser(USER_JACK_OID);
//                ProtectedStringType afterTestPasswordValue = jack.asObjectable().getCredentials().getPassword().getValue();
//                LOGGER.trace("password after test = " + afterTestPasswordValue);
//
//                // todo why is password value not set?
//                //assertNotNull("password was not set", afterTestPasswordValue.getEncryptedData());
//                //assertFalse("password was not changed", originalPasswordValue.getEncryptedData().equals(afterTestPasswordValue.getEncryptedData()));
//                assertAssignedRole(jack, ROLE_R1_OID);
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                LightweightObjectRef targetRef = (LightweightObjectRef) activitiEngine.getRuntimeService().getVariable(executionId, CommonProcessVariableNames.VARIABLE_TARGET_REF);
//                if (targetRef != null && RoleType.COMPLEX_TYPE.equals(targetRef.toObjectReferenceType().getType())) {
//                    return decideOnRoleApproval(executionId);
//                } else {
//                    login(getUser(USER_ADMINISTRATOR_OID));
//                    return true;
//                }
//            }
//
//        });
//    }
//
//    @Test(enabled = true)
//    public void test060UserModifyAddRoleAutoApproval() throws Exception {
//        TestUtil.displayTestTile(this, "test060UserModifyAddRoleAutoApproval");
//        login(userAdministrator);
//        executeTest("test060UserModifyAddRoleAutoApproval", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override boolean approvedAutomatically() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE10);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                // todo perhaps the role should be assigned even at this point?
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertAssignedRole(jack, ROLE_R10_OID);
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                throw new AssertionError("Decision should not be acquired in this scenario.");
//            }
//
//        });
//    }
//
//    @Test
//    public void test062UserModifyAddRoleAutoApprovalFirstDecides() throws Exception {
//        TestUtil.displayTestTile(this, "test062UserModifyAddRoleAutoApprovalFirstDecides");
//        login(userAdministrator);
//        executeTest("test062UserModifyAddRoleAutoApprovalFirstDecides", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override boolean approvedAutomatically() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusDeltaToContext(context,
//                        (ObjectDelta) DeltaBuilder.deltaFor(UserType.class, prismContext)
//                                .item(UserType.F_ASSIGNMENT).add(
//                                        ObjectTypeUtil.createAssignmentTo(ROLE_R11_OID, ObjectTypes.ROLE, prismContext).asPrismContainerValue())
//                            .asObjectDelta(USER_JACK_OID));
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                // todo perhaps the role should be assigned even at this point?
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertAssignedRole(jack, ROLE_R11_OID);
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                throw new AssertionError("Decision should not be acquired in this scenario.");
//            }
//
//        });
//    }
//
//    @Test
//    public void test064UserModifyAddRoleNoApproversAllMustAgree() throws Exception {
//        TestUtil.displayTestTile(this, "test064UserModifyAddRoleNoApproversAllMustAgree");
//        login(userAdministrator);
//        executeTest("test064UserModifyAddRoleNoApproversAllMustAgree", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override boolean approvedAutomatically() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusDeltaToContext(context,
//                        (ObjectDelta) DeltaBuilder.deltaFor(UserType.class, prismContext)
//                                .item(UserType.F_ASSIGNMENT).add(
//                                        ObjectTypeUtil.createAssignmentTo(ROLE_R12_OID, ObjectTypes.ROLE, prismContext).asPrismContainerValue())
//                                .asObjectDelta(USER_JACK_OID));
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                // todo perhaps the role should be assigned even at this point?
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertAssignedRole(jack, ROLE_R12_OID);
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                throw new AssertionError("Decision should not be acquired in this scenario.");
//            }
//
//        });
//    }
//
//    @Test
//    public void test065UserModifyAddRoleNoApproversFirstDecides() throws Exception {
//        TestUtil.displayTestTile(this, "test065UserModifyAddRoleNoApproversFirstDecides");
//        login(userAdministrator);
//        executeTest("test065UserModifyAddRoleNoApproversFirstDecides", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override boolean approvedAutomatically() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusDeltaToContext(context,
//                        (ObjectDelta) DeltaBuilder.deltaFor(UserType.class, prismContext)
//                                .item(UserType.F_ASSIGNMENT).add(
//                                        ObjectTypeUtil.createAssignmentTo(ROLE_R13_OID, ObjectTypes.ROLE, prismContext).asPrismContainerValue())
//                                .asObjectDelta(USER_JACK_OID));
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                // todo perhaps the role should be assigned even at this point?
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                PrismObject<UserType> jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result);
//                assertAssignedRole(jack, ROLE_R13_OID);
//
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                throw new AssertionError("Decision should not be acquired in this scenario.");
//            }
//
//        });
//    }
//
//
//    @Test(enabled = true)
//    public void test070UserModifyAssignment() throws Exception {
//        TestUtil.displayTestTile(this, "test070UserModifyAssignment");
//        login(userAdministrator);
//        removeAllAssignments(USER_JACK_OID, new OperationResult("dummy"));
//        assignRoleRaw(USER_JACK_OID, ROLE_R1_OID);
//
//        final XMLGregorianCalendar validFrom = XmlTypeConverter.createXMLGregorianCalendar(2015, 2, 25, 10, 0, 0);
//        final XMLGregorianCalendar validTo = XmlTypeConverter.createXMLGregorianCalendar(2015, 3, 25, 10, 0, 0);
//
//        executeTest("test070UserModifyAssignment", USER_JACK_OID, new TestDetails() {
//            @Override
//            int subtaskCount() {
//                return 1;
//            }
//
//            @Override
//            boolean immediate() {
//                return false;
//            }
//
//            @Override
//            boolean checkObjectOnSubtasks() {
//                return true;
//            }
//
//            @Override
//            boolean removeAssignmentsBeforeTest() {
//                return false;
//            }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                UserType jack = context.getFocusContext().getObjectOld().asObjectable();
//                modifyAssignmentValidity(context, jack, validFrom, validTo);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//                UserType jack = getUser(USER_JACK_OID).asObjectable();
//                checkNoAssignmentValidity(jack);
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                UserType jack = getUser(USER_JACK_OID).asObjectable();
//                checkAssignmentValidity(jack, validFrom, validTo);
//
//                // TODO
//                //checkDummyTransportMessages("simpleUserNotifier", 1);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                login(getUser(R1BOSS_OID));
//                return true;
//            }
//
//        });
//    }
//
//    private void checkAssignmentValidity(UserType jack, XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo) {
//        assertEquals("jack's assignments", 1, jack.getAssignment().size());
//        AssignmentType assignmentType = jack.getAssignment().get(0);
//        assertEquals("wrong validFrom", validFrom, assignmentType.getActivation().getValidFrom());
//        assertEquals("wrong validTo", validTo, assignmentType.getActivation().getValidTo());
//    }
//
//    protected void checkNoAssignmentValidity(UserType jack) {
//        assertEquals("jack's assignments", 1, jack.getAssignment().size());
//        AssignmentType assignmentType = jack.getAssignment().get(0);
//        if (assignmentType.getActivation() != null) {
//            assertNull("validFrom already set", assignmentType.getActivation().getValidFrom());
//            assertNull("validTo already set", assignmentType.getActivation().getValidTo());
//        }
//    }
//
//    protected void modifyAssignmentValidity(LensContext<UserType> context, UserType jack, XMLGregorianCalendar validFrom, XMLGregorianCalendar validTo) throws SchemaException {
//        assertEquals("jack's assignments", 1, jack.getAssignment().size());
//        PrismContainerDefinition<ActivationType> activationDef =
//                prismContext.getSchemaRegistry()
//                        .findObjectDefinitionByCompileTimeClass(UserType.class)
//                        .findContainerDefinition(new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION));
//        assertNotNull("no activationDef", activationDef);
//
//        Long assignmentId = jack.getAssignment().get(0).getId();
//        PrismPropertyDefinition<XMLGregorianCalendar> validFromDef = activationDef.findPropertyDefinition(ActivationType.F_VALID_FROM);
//        PropertyDelta<XMLGregorianCalendar> validFromDelta = new PropertyDelta<>(
//                new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT),
//                        new IdItemPathSegment(assignmentId),
//                        new NameItemPathSegment(AssignmentType.F_ACTIVATION),
//                        new NameItemPathSegment(ActivationType.F_VALID_FROM)),
//                        validFromDef, prismContext);
//        validFromDelta.setValueToReplace(new PrismPropertyValue<>(validFrom));
//        PrismPropertyDefinition<XMLGregorianCalendar> validToDef = activationDef.findPropertyDefinition(ActivationType.F_VALID_TO);
//        PropertyDelta<XMLGregorianCalendar> validToDelta = new PropertyDelta<>(
//                new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT),
//                        new IdItemPathSegment(assignmentId),
//                        new NameItemPathSegment(AssignmentType.F_ACTIVATION),
//                        new NameItemPathSegment(ActivationType.F_VALID_TO)),
//                        validToDef, prismContext);
//        validToDelta.setValueToReplace(new PrismPropertyValue<>(validTo));
//
//        ObjectDelta<UserType> userDelta = new ObjectDelta<>(UserType.class, ChangeType.MODIFY, prismContext);
//        userDelta.setOid(USER_JACK_OID);
//        userDelta.addModification(validFromDelta);
//        userDelta.addModification(validToDelta);
//        addFocusDeltaToContext(context, userDelta);
//    }
//
//    private void assignRoleRaw(String userOid, String roleOid) throws SchemaException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
//        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid, roleOid, RoleType.COMPLEX_TYPE, null, null, null, true);
//        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
//        repositoryService.modifyObject(UserType.class,
//                userOid, userDelta.getModifications(), new OperationResult("dummy"));
//    }
//
//    /**
//     * User modification: adding single security-sensitive resource assignment.
//     */
//    @Test(enabled = true)
//    public void test080UserModifyAddResource() throws Exception {
//        TestUtil.displayTestTile(this, "test080UserModifyAddResource");
//        login(userAdministrator);
//        executeTest("test080UserModifyAddResource", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                addFocusModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_DUMMY);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//                assertNotAssignedResource(USER_JACK_OID, RESOURCE_DUMMY_OID, rootTask, result);
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                assertAssignedResource(USER_JACK_OID, RESOURCE_DUMMY_OID, task, result);
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
//                checkUserApprovers(USER_JACK_OID, Arrays.asList(DUMMYBOSS_OID), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                login(getUser(DUMMYBOSS_OID));
//                return true;
//            }
//        });
//    }
//
//    /**
//     * User modification: modifying validity of single security-sensitive resource assignment.
//     */
//    @Test(enabled = true)
//    public void test090UserModifyModifyResourceAssignmentValidity() throws Exception {
//        TestUtil.displayTestTile(this, "test090UserModifyModifyResourceAssignmentValidity");
//        login(userAdministrator);
//
//        final XMLGregorianCalendar validFrom = XmlTypeConverter.createXMLGregorianCalendar(2015, 2, 25, 10, 0, 0);
//        final XMLGregorianCalendar validTo = XmlTypeConverter.createXMLGregorianCalendar(2015, 3, 25, 10, 0, 0);
//
//        executeTest("test090UserModifyModifyResourceAssignmentValidity", USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//            @Override boolean removeAssignmentsBeforeTest() { return false; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                UserType jack = context.getFocusContext().getObjectOld().asObjectable();
//                modifyAssignmentValidity(context, jack, validFrom, validTo);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//                UserType jack = getUser(USER_JACK_OID).asObjectable();
//                checkNoAssignmentValidity(jack);
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                assertAssignedResource(USER_JACK_OID, RESOURCE_DUMMY_OID, task, result);
//                UserType jack = getUser(USER_JACK_OID).asObjectable();
//                checkAssignmentValidity(jack, validFrom, validTo);
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
//                checkUserApprovers(USER_JACK_OID, Arrays.asList(DUMMYBOSS_OID), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                login(getUser(DUMMYBOSS_OID));
//                return true;
//            }
//        });
//    }
//
//    /**
//     * User modification: modifying attribute of single security-sensitive resource assignment.
//     *
//     * User primary delta:
//     *  ObjectDelta<UserType>(UserType:377205db-33d1-47e5-bf96-bbe2a7d1222e,MODIFY):
//     *  assignment/[1]/construction/attribute
//     *  ADD: ResourceAttributeDefinitionType(ref=ItemPathType{itemPath=lastname}...)
//     *  DELETE: ResourceAttributeDefinitionType(ref=ItemPathType{itemPath=lastname}...)
//     *
//     */
//    @Test(enabled = true)
//    public void test095UserModifyModifyResourceAssignmentConstruction() throws Exception {
//        final String TEST_NAME = "test095UserModifyModifyResourceAssignmentConstruction";
//        TestUtil.displayTestTile(this, TEST_NAME);
//        login(userAdministrator);
//
//        executeTest(TEST_NAME, USER_JACK_OID, new TestDetails() {
//            @Override int subtaskCount() { return 1; }
//            @Override boolean immediate() { return false; }
//            @Override boolean checkObjectOnSubtasks() { return true; }
//            @Override boolean removeAssignmentsBeforeTest() { return false; }
//
//            @Override
//            public LensContext createModelContext(OperationResult result) throws Exception {
//                LensContext<UserType> context = createUserAccountContext();
//                fillContextWithUser(context, USER_JACK_OID, result);
//                UserType jack = context.getFocusContext().getObjectOld().asObjectable();
//                modifyAssignmentConstruction(context, jack,
//                        DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "water", true);
//                return context;
//            }
//
//            @Override
//            public void assertsAfterClockworkRun(Task rootTask, List<Task> wfSubtasks, OperationResult result) throws Exception {
//                ModelContext taskModelContext = wfTaskUtil.getModelContext(rootTask, result);
//                assertEquals("There are modifications left in primary focus delta", 0, taskModelContext.getFocusContext().getPrimaryDelta().getModifications().size());
//                UserType jack = getUser(USER_JACK_OID).asObjectable();
//                checkNoAssignmentConstruction(jack, "drink");
//            }
//
//            @Override
//            void assertsRootTaskFinishes(Task task, List<Task> subtasks, OperationResult result) throws Exception {
//                assertAssignedResource(USER_JACK_OID, RESOURCE_DUMMY_OID, task, result);
//                UserType jack = getUser(USER_JACK_OID).asObjectable();
//                checkAssignmentConstruction(jack, "drink", "water");
//                checkDummyTransportMessages("simpleUserNotifier", 1);
//                //checkWorkItemAuditRecords(createResultMap(ROLE_R1_OID, WorkflowResult.APPROVED));
//                checkUserApprovers(USER_JACK_OID, Arrays.asList(DUMMYBOSS_OID), result);
//            }
//
//            @Override
//            boolean decideOnApproval(String executionId) throws Exception {
//                login(getUser(DUMMYBOSS_OID));
//                return true;
//            }
//        });
//    }
//
//    protected void modifyAssignmentConstruction(LensContext<UserType> context, UserType jack,
//                                                String attributeName, String value, boolean add) throws SchemaException {
//        assertEquals("jack's assignments", 1, jack.getAssignment().size());
//        PrismPropertyDefinition<ResourceAttributeDefinitionType> attributeDef =
//                prismContext.getSchemaRegistry()
//                        .findObjectDefinitionByCompileTimeClass(UserType.class)
//                        .findPropertyDefinition(new ItemPath(UserType.F_ASSIGNMENT,
//                                AssignmentType.F_CONSTRUCTION,
//                                ConstructionType.F_ATTRIBUTE));
//        assertNotNull("no attributeDef", attributeDef);
//
//        Long assignmentId = jack.getAssignment().get(0).getId();
//        PropertyDelta<ResourceAttributeDefinitionType> attributeDelta = new PropertyDelta<ResourceAttributeDefinitionType>(
//                new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT),
//                        new IdItemPathSegment(assignmentId),
//                        new NameItemPathSegment(AssignmentType.F_CONSTRUCTION),
//                        new NameItemPathSegment(ConstructionType.F_ATTRIBUTE)),
//                attributeDef, prismContext);
//        ResourceAttributeDefinitionType attributeDefinitionType = new ResourceAttributeDefinitionType();
//        attributeDefinitionType.setRef(new ItemPathType(new ItemPath(new QName(RESOURCE_DUMMY_NAMESPACE, attributeName))));
//        MappingType outbound = new MappingType();
//        outbound.setStrength(MappingStrengthType.STRONG);       // to see changes on the resource
//        ExpressionType expression = new ExpressionType();
//        expression.getExpressionEvaluator().add(new ObjectFactory().createValue(value));
//        outbound.setExpression(expression);
//        attributeDefinitionType.setOutbound(outbound);
//
//        if (add) {
//            attributeDelta.addValueToAdd(new PrismPropertyValue<>(attributeDefinitionType));
//        } else {
//            attributeDelta.addValueToDelete(new PrismPropertyValue<>(attributeDefinitionType));
//        }
//
//        ObjectDelta<UserType> userDelta = new ObjectDelta<>(UserType.class, ChangeType.MODIFY, prismContext);
//        userDelta.setOid(USER_JACK_OID);
//        userDelta.addModification(attributeDelta);
//        addFocusDeltaToContext(context, userDelta);
//    }
//
//    private void checkAssignmentConstruction(UserType jack, String attributeName, String value) throws SchemaException {
//        assertEquals("jack's assignments", 1, jack.getAssignment().size());
//        AssignmentType assignmentType = jack.getAssignment().get(0);
//        ConstructionType constructionType = assignmentType.getConstruction();
//        assertNotNull("construction is null", constructionType);
//        boolean found = false;
//        for (ResourceAttributeDefinitionType attributeDefinitionType : constructionType.getAttribute()) {
//            if (attributeDefinitionType.getRef().equivalent(new ItemPathType(new ItemPath(new QName(attributeName))))) {
//                ExpressionType expressionType = attributeDefinitionType.getOutbound().getExpression();
//                assertNotNull("no expression", expressionType);
//                assertEquals("wrong # of expression evaluators", 1, expressionType.getExpressionEvaluator().size());
//                JAXBElement<?> element = expressionType.getExpressionEvaluator().get(0);
//                PrimitiveXNode valueXNode = (PrimitiveXNode) (((RawType) element.getValue()).serializeToXNode());
//                assertEquals("wrong outbound value", value, valueXNode.getStringValue());
//                found = true;
//            }
//        }
//        assertTrue("attribute " + attributeName + " mapping not found", found);
//    }
//
//    private void checkNoAssignmentConstruction(UserType jack, String attributeName) {
//        assertEquals("jack's assignments", 1, jack.getAssignment().size());
//        AssignmentType assignmentType = jack.getAssignment().get(0);
//        ConstructionType constructionType = assignmentType.getConstruction();
//        assertNotNull("construction is null", constructionType);
//        for (ResourceAttributeDefinitionType attributeDefinitionType : constructionType.getAttribute()) {
//            if (attributeDefinitionType.getRef().equivalent(new ItemPathType(new ItemPath(new QName(attributeName))))) {
//                fail("Construction attribute " + attributeName + " present, although it shouldn't");
//            }
//        }
//    }

}
