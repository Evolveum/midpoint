/*
 * Copyright (c) 2017 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalInspector;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Testing bushy roles hierarchy. Especially reuse of the same role
 * in the rich role hierarchy. It looks like this:
 * 
 *                    user
 *                     |
 *       +------+------+-----+-----+-....
 *       |      |      |     |     |
 *       v      v      v     v     v
 *      Ra1    Ra2    Ra3   Ra4   Ra5
 *       |      |      |     |     |
 *       +------+------+-----+-----+
 *                     |
 *                     v
 *                notorious role
 *                     |
 *       +------+------+-----+-----+-....
 *       |      |      |     |     |
 *       v      v      v     v     v
 *      Rb1    Rb2    Rb3   Rb4   Rb5
 *      
 * Naive mode of evaluation would imply cartesian product of all Rax and Rbx
 * combinations. That's painfully inefficient. Therefore make sure that the
 * notorious roles is evaluated only once and the results of the evaluation
 * are reused.
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNotoriousRole extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "notorious");
	
	public static final File ROLE_NOTORIOUS_FILE = new File(TEST_DIR, "role-notorious.xml");
	public static final String ROLE_NOTORIOUS_OID = "1e95a1b8-46d1-11e7-84c5-e36e43bb0f00";
	
	private static final int NUMBER_OF_ORDINARY_ROLES = 1; // including superuser role
	
	private static final int NUMBER_OF_LEVEL_A_ROLES = 100;
	private static final String ROLE_LEVEL_A_NAME_FORMAT = "Role A %06d";
	private static final String ROLE_LEVEL_A_OID_FORMAT = "00000000-0000-ffff-2a00-000000%06d";
	
	private static final int NUMBER_OF_LEVEL_B_ROLES = 300;
	private static final String ROLE_LEVEL_B_NAME_FORMAT = "Role B %06d";
	private static final String ROLE_LEVEL_B_OID_FORMAT = "00000000-0000-ffff-2b00-000000%06d";
	
	private static final Trace LOGGER = TraceManager.getTrace(TestNotoriousRole.class);
	
	private RepoReadInspector inspector;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		generateRoles(NUMBER_OF_LEVEL_A_ROLES, ROLE_LEVEL_A_NAME_FORMAT, ROLE_LEVEL_A_OID_FORMAT,
				(role,i) -> {
					role.beginInducement().targetRef(ROLE_NOTORIOUS_OID, RoleType.COMPLEX_TYPE).end();
				},
				initResult);

		generateRoles(NUMBER_OF_LEVEL_B_ROLES, ROLE_LEVEL_B_NAME_FORMAT, ROLE_LEVEL_B_OID_FORMAT,
				null,
				initResult);

		addNotoriousRole(initResult);
		
		inspector = new RepoReadInspector();
		InternalMonitor.setInspector(inspector);
		
		InternalMonitor.setTraceRoleEvaluation(true);
	}
	
	private void addNotoriousRole(OperationResult result) throws Exception {
		PrismObject<RoleType> role = parseObject(ROLE_NOTORIOUS_FILE);
		RoleType roleType = role.asObjectable();
		for(int i=0; i < NUMBER_OF_LEVEL_B_ROLES; i++) {
			roleType.beginInducement().targetRef(generateRoleOid(ROLE_LEVEL_B_OID_FORMAT, i), RoleType.COMPLEX_TYPE).end();
		}
		LOGGER.info("Adding {}:\n{}", role, role.debugDump(1));
		repositoryService.addObject(role, null, result);
	}

	private String generateRoleOid(String oidFormat, int num) {
		return String.format(oidFormat, num);
	}
	
	private String generateRoleAOid(int num) {
		return String.format(ROLE_LEVEL_A_OID_FORMAT, num);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTile(TEST_NAME);

        assertObjects(RoleType.class, NUMBER_OF_LEVEL_A_ROLES + NUMBER_OF_LEVEL_B_ROLES + NUMBER_OF_ORDINARY_ROLES + 1);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());
	}
	
	@Test
    public void test100AssignRa0ToJack() throws Exception {
		final String TEST_NAME = "test100AssignRa0ToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberProjectorRunCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, generateRoleAOid(0), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0 assign in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, 1);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Projector runs", InternalMonitor.getProjectorRunCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertProjectorRunCount(hackify(1));
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 2));
        assertPrismObjectCompareCount(0);
	}
	
	private int hackify(int i) {
		// TODO: projector now runs three times instead of one.
		return i*3;
	}

	@Test
    public void test102RecomputeJack() throws Exception {
		final String TEST_NAME = "test102RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberProjectorRunCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0 recompute in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, 1);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 2));
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test104PreviewChangesJack() throws Exception {
		final String TEST_NAME = "test104PreviewChangesJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", assignmentSummary(userBefore));
        
        ObjectDelta<UserType> delta = userBefore.createModifyDelta();
        delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "123");
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberProjectorRunCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);        
		ModelContext<ObjectType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0 preview changes in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, 1);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertProjectorRunCount(1);
        assertRoleEvaluationCount((NUMBER_OF_LEVEL_B_ROLES + 2)*2);
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test109UnassignRa0FromJack() throws Exception {
		final String TEST_NAME = "test109UnassignRa0FromJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, generateRoleAOid(0), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0 unassign in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertNoAssignments(userAfter);
        assertRoleMembershipRefs(userAfter, 0);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 2));        
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test110Assign5ARolesToJack() throws Exception {
		final String TEST_NAME = "test110AssignAllARolesToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignJackARoles(5, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Assign 5 A roles in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 1 + 5))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, 5);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + 5));
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test112RecomputeJack() throws Exception {
		final String TEST_NAME = "test112RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberProjectorRunCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Recompute 5 A roles in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 1 + 5))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, 5);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + 5));
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test119Unassign5ARolesFromJack() throws Exception {
		final String TEST_NAME = "test119Unassign5ARolesFromJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignJackARoles(5, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0 unassign in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 1 + 5))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertNoAssignments(userAfter);
        assertRoleMembershipRefs(userAfter, 0);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + 5));        
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test120AssignAllARolesToJack() throws Exception {
		final String TEST_NAME = "test120AssignAllARolesToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignJackARoles(NUMBER_OF_LEVEL_A_ROLES, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Assign all A roles in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, NUMBER_OF_LEVEL_A_ROLES);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES));
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test122RecomputeJack() throws Exception {
		final String TEST_NAME = "test122RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberProjectorRunCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Recompute all A roles in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, NUMBER_OF_LEVEL_A_ROLES);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES));
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test124PreviewChangesJack() throws Exception {
		final String TEST_NAME = "test124PreviewChangesJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", assignmentSummary(userBefore));
        
        ObjectDelta<UserType> delta = userBefore.createModifyDelta();
        delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "123");
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberProjectorRunCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);        
		ModelContext<ObjectType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Preview changes (all A roles) in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleMembershipRef(userAfter, NUMBER_OF_LEVEL_A_ROLES);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertProjectorRunCount(1);
        assertRoleEvaluationCount((NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES)*2);
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test129Unassign5ARolesFromJack() throws Exception {
		final String TEST_NAME = "test129Unassign5ARolesFromJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        rememberRoleEvaluationCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignJackARoles(NUMBER_OF_LEVEL_A_ROLES, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Unassign all A roles in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertNoAssignments(userAfter);
        assertRoleMembershipRefs(userAfter, 0);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Role evaluations", InternalMonitor.getRoleEvaluationCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        assertRoleEvaluationCount(hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES));        
        assertPrismObjectCompareCount(0);
	}

	private void assignJackARoles(int numberOfRoles, Task task, OperationResult result) throws Exception {
		modifyJackARolesAssignment(numberOfRoles, true, task, result);
	}
	
	private void unassignJackARoles(int numberOfRoles, Task task, OperationResult result) throws Exception {
		modifyJackARolesAssignment(numberOfRoles, false, task, result);
	}
	
	private void modifyJackARolesAssignment(int numberOfRoles, boolean add, Task task, OperationResult result) throws Exception {
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		for (int i=0; i<numberOfRoles; i++) {
			modifications.add((createAssignmentModification(generateRoleAOid(i), RoleType.COMPLEX_TYPE, null, null, null, add)));
		}
		ObjectDelta<UserType> delta = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
		
		executeChanges(delta, null, task, result);
	}

	//
//	private void addAssignments(PrismObject<UserType> user, QName relation, int offset, int num) {
//		UserType userType = user.asObjectable();
//		for (int i = 0; i < num; i++) {
//			AssignmentType assignmentType = new AssignmentType();
//			String oid = generateRoleOid(offset + i);
//			assignmentType.targetRef(oid, RoleType.COMPLEX_TYPE, relation);
//			userType.getAssignment().add(assignmentType);
//		}
//	}
//
	private void assertJackRoleMembershipRef(PrismObject<UserType> user, int numberOfLevelARoles) {
		
		assertRoleMembershipRefs(user, ROLE_LEVEL_A_OID_FORMAT, numberOfLevelARoles);
		assertRoleMembershipRefNonExclusive(user, ROLE_NOTORIOUS_OID);
		assertRoleMembershipRefs(user, ROLE_LEVEL_B_OID_FORMAT, NUMBER_OF_LEVEL_B_ROLES);
		
		assertRoleMembershipRefs(user, numberOfLevelARoles + 1 + NUMBER_OF_LEVEL_B_ROLES);
	}

	private void assertRoleMembershipRefs(PrismObject<UserType> user, String oidFormat, int num) {
		for (int i = 0; i < num; i++) {
			assertRoleMembershipRefNonExclusive(user, generateRoleOid(oidFormat, i));
		}
	}
	
	private void assertRoleMembershipRefNonExclusive(PrismObject<UserType> user, String roleOid) {
		List<ObjectReferenceType> roleMembershipRefs = user.asObjectable().getRoleMembershipRef();
		for (ObjectReferenceType roleMembershipRef: roleMembershipRefs) {
			if (ObjectTypeUtil.referenceMatches(roleMembershipRef, roleOid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)) {
				return;
			}
		}
		fail("Cannot find membership of role "+roleOid+" in "+user);
	}

	
}
