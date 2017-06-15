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
package com.evolveum.midpoint.testing.story.notorious;

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
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalInspector;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.internals.TestingPaths;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.testing.story.CountingInspector;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
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
 *            notorious role / org
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
public abstract class AbstractNotoriousTest extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "notorious");
		
	private static final int NUMBER_OF_ORDINARY_ROLES = 1; // including superuser role
	
	protected static final int NUMBER_OF_LEVEL_A_ROLES = 100;
	protected static final String ROLE_LEVEL_A_NAME_FORMAT = "Role A %06d";
	protected static final String ROLE_LEVEL_A_ROLETYPE = "levelA";
	protected static final String ROLE_LEVEL_A_OID_FORMAT = "00000000-0000-ffff-2a00-000000%06d";
	
	protected static final int NUMBER_OF_LEVEL_B_ROLES = 300;
	protected static final String ROLE_LEVEL_B_NAME_FORMAT = "Role B %06d";
	protected static final String ROLE_LEVEL_B_ROLETYPE = "levelB";
	protected static final String ROLE_LEVEL_B_OID_FORMAT = "00000000-0000-ffff-2b00-000000%06d";
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractNotoriousTest.class);
	
	protected CountingInspector inspector;
	
	protected abstract String getNotoriousOid();
	
	protected abstract File getNotoriousFile();
	
	protected abstract QName getNotoriousType();
	
	protected abstract int getNumberOfExtraRoles();
	
	protected abstract int getNumberOfExtraOrgs();
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		generateRoles(NUMBER_OF_LEVEL_A_ROLES, ROLE_LEVEL_A_NAME_FORMAT, ROLE_LEVEL_A_OID_FORMAT,
				(role,i) -> {
					role.roleType(ROLE_LEVEL_A_ROLETYPE);
					role.beginInducement().targetRef(getNotoriousOid(), getNotoriousType()).end();
				},
				initResult);

		addNotoriousRole(initResult);
		
		// Add these using model, so they have proper roleMembershipRef
		generateRoles(NUMBER_OF_LEVEL_B_ROLES, ROLE_LEVEL_B_NAME_FORMAT, ROLE_LEVEL_B_OID_FORMAT,
				this::fillLevelBRole,
				role -> addObject(role, initTask, initResult),
				initResult);
		
		inspector = new CountingInspector();
		InternalMonitor.setInspector(inspector);
		
		InternalMonitor.setTrace(InternalOperationClasses.ROLE_EVALUATIONS, true);
	}
	
	protected abstract void addNotoriousRole(OperationResult result) throws Exception;
	
	protected void fillLevelBRole(RoleType roleType, int i) {
		roleType
			.roleType(ROLE_LEVEL_B_ROLETYPE);
	}
	
	protected void fillNotorious(AbstractRoleType roleType) throws Exception {
		for(int i=0; i < NUMBER_OF_LEVEL_B_ROLES; i++) {
			roleType.beginInducement()
				.targetRef(generateRoleBOid(i), RoleType.COMPLEX_TYPE)
				.focusType(UserType.COMPLEX_TYPE)
			.end();
		}
	}

	private String generateRoleOid(String oidFormat, int num) {
		return String.format(oidFormat, num);
	}
	
	private String generateRoleAOid(int num) {
		return String.format(ROLE_LEVEL_A_OID_FORMAT, num);
	}
	
	private String generateRoleBOid(int num) {
		return String.format(ROLE_LEVEL_B_OID_FORMAT, num);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTile(TEST_NAME);

        assertObjects(RoleType.class, NUMBER_OF_LEVEL_A_ROLES + NUMBER_OF_LEVEL_B_ROLES + NUMBER_OF_ORDINARY_ROLES + getNumberOfExtraRoles());
        assertObjects(OrgType.class, getNumberOfExtraOrgs());
        
        display("Repo reads", InternalMonitor.getCount(InternalCounters.REPOSITORY_READ_COUNT));
        display("Object compares", InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_COMPARE_COUNT));
	}
	
	@Test
    public void test100AssignRa0ToJack() throws Exception {
		final String TEST_NAME = "test100AssignRa0ToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        
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
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(1, 0);
        
        assertCounterIncrement(InternalCounters.PROJECTOR_RUN_COUNT, hackify(1));
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}

	@Test
    public void test102RecomputeJack() throws Exception {
		final String TEST_NAME = "test102RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
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
        
        prepareTest();
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
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.PROJECTOR_RUN_COUNT, 1);
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, (NUMBER_OF_LEVEL_B_ROLES + 2)*2);
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test109UnassignRa0FromJack() throws Exception {
		final String TEST_NAME = "test109UnassignRa0FromJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertNoNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2));        
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test110Assign5ARolesToJack() throws Exception {
		final String TEST_NAME = "test110AssignAllARolesToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertJackRoleAMembershipRef(userAfter, 5);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + 5));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test112RecomputeJack() throws Exception {
		final String TEST_NAME = "test112RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertJackRoleAMembershipRef(userAfter, 5);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + 5));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test119Unassign5ARolesFromJack() throws Exception {
		final String TEST_NAME = "test119Unassign5ARolesFromJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertNoNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + 5));        
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test120AssignAllARolesToJack() throws Exception {
		final String TEST_NAME = "test120AssignAllARolesToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertJackRoleAMembershipRef(userAfter, NUMBER_OF_LEVEL_A_ROLES);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test122RecomputeJack() throws Exception {
		final String TEST_NAME = "test122RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertJackRoleAMembershipRef(userAfter, NUMBER_OF_LEVEL_A_ROLES);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
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
        
        prepareTest();
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
        assertJackRoleAMembershipRef(userAfter, NUMBER_OF_LEVEL_A_ROLES);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.PROJECTOR_RUN_COUNT, 1);
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, (NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES)*2);
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test129UnassignAllARolesFromJack() throws Exception {
		final String TEST_NAME = "test129UnassignAllARolesFromJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
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
        assertNoNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 1 + NUMBER_OF_LEVEL_A_ROLES));        
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test130AssignRb0ToJack() throws Exception {
		final String TEST_NAME = "test130AssignRb0ToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, generateRoleBOid(0), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Rb0 assign in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertRoleMembershipRef(userAfter, generateRoleBOid(0));
        assertNoNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(0, 1);
        
        assertCounterIncrement(InternalCounters.PROJECTOR_RUN_COUNT, hackify(1));
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(1));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	/**
	 * Now jack has RoleB0 assigned in two ways: directly and through RA0->notorious->RB0
	 * This may cause problems e.g. for supernotorious roles where the direct assignment
	 * may cause evaluation of notorious role as metarole. And then the second evaluation
	 * may be skipped. Which is wrong.
	 */
	@Test
    public void test132AssignRa0ToJack() throws Exception {
		final String TEST_NAME = "test132AssignRa0ToJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        
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
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(1, 1);
        
        assertCounterIncrement(InternalCounters.PROJECTOR_RUN_COUNT, hackify(1));
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2 + 1));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}

	@Test
    public void test134RecomputeJack() throws Exception {
		final String TEST_NAME = "test134RecomputeJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0+Rb0 recompute in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(1, 1);
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2 + 1));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test136UnassignRb0FromJack() throws Exception {
		final String TEST_NAME = "test136UnassignRb0FromJack";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, generateRoleBOid(0), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Rb0 unassign in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(1, 1);
        
        assertCounterIncrement(InternalCounters.PROJECTOR_RUN_COUNT, hackify(1));
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2 + 1));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test138AssignRb0ToJackAgain() throws Exception {
		final String TEST_NAME = "test138AssignRb0ToJackAgain";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, generateRoleBOid(0), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Rb0 assign in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(1, 1);
        
        assertCounterIncrement(InternalCounters.PROJECTOR_RUN_COUNT, hackify(1));
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2 + 1));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test140RecomputeJackAgain() throws Exception {
		final String TEST_NAME = "test140RecomputeJackAgain";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0+Rb0 recompute again in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(1, 1);
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2 + 1));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	@Test
    public void test142RecomputeJackAlt() throws Exception {
		final String TEST_NAME = "test142RecomputeJackAlt";
        displayTestTile(TEST_NAME);
        
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        prepareTest();
        InternalsConfig.setTestingPaths(TestingPaths.REVERSED);
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Ra0+Rb0 recompute again in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/(NUMBER_OF_LEVEL_B_ROLES + 2))+"ms per assigned role)");
        
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", assignmentSummary(userAfter));
        assertJackRoleAMembershipRef(userAfter, 1);
        assertNotoriousParentOrgRef(userAfter);
        
        displayCountersAndInspector();
        
        assertRoleEvaluationCount(1, 1);
        
        assertCounterIncrement(InternalCounters.ROLE_EVALUATION_COUNT, hackify(NUMBER_OF_LEVEL_B_ROLES + 2 + 1));
        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}
	
	// TODO: ...

	

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

	private void assertJackRoleAMembershipRef(PrismObject<UserType> user, int numberOfLevelARoles) {
		
		assertRoleMembershipRefs(user, ROLE_LEVEL_A_OID_FORMAT, numberOfLevelARoles);
		assertRoleMembershipRefNonExclusive(user, getNotoriousOid(), getNotoriousType());
		assertRoleMembershipRefs(user, ROLE_LEVEL_B_OID_FORMAT, NUMBER_OF_LEVEL_B_ROLES);
		
		assertRoleMembershipRefs(user, numberOfLevelARoles + 1 + NUMBER_OF_LEVEL_B_ROLES);
	}

	private void assertRoleMembershipRefs(PrismObject<UserType> user, String oidFormat, int num) {
		for (int i = 0; i < num; i++) {
			assertRoleMembershipRefNonExclusive(user, generateRoleOid(oidFormat, i), RoleType.COMPLEX_TYPE);
		}
	}
	
	private void assertRoleMembershipRefNonExclusive(PrismObject<UserType> user, String roleOid, QName roleType) {
		List<ObjectReferenceType> roleMembershipRefs = user.asObjectable().getRoleMembershipRef();
		for (ObjectReferenceType roleMembershipRef: roleMembershipRefs) {
			if (ObjectTypeUtil.referenceMatches(roleMembershipRef, roleOid, roleType, SchemaConstants.ORG_DEFAULT)) {
				return;
			}
		}
		fail("Cannot find membership of role "+roleOid+" in "+user);
	}
	
	protected void assertRoleEvaluationCount(int numberOfLevelAAssignments, int numberOfOtherAssignments) {
		// for subclasses
	}
	
	protected void assertNoNotoriousParentOrgRef(PrismObject<UserType> userAfter) {
		assertHasNoOrg(userAfter, getNotoriousOid());
	}
	
	protected void assertNotoriousParentOrgRef(PrismObject<UserType> userAfter) {
		// for subclasses
	}

	private void prepareTest() {
		InternalsConfig.resetTestingPaths();
		inspector.reset();
        rememberCounter(InternalCounters.PRISM_OBJECT_COMPARE_COUNT);
        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);
        rememberCounter(InternalCounters.PROJECTOR_RUN_COUNT);
        rememberCounter(InternalCounters.ROLE_EVALUATION_COUNT);
	}

	private void displayCountersAndInspector() {
		displayCounters(
        		InternalCounters.REPOSITORY_READ_COUNT, 
        		InternalCounters.PROJECTOR_RUN_COUNT,
        		InternalCounters.ROLE_EVALUATION_COUNT,
        		InternalCounters.ROLE_EVALUATION_SKIP_COUNT,
        		InternalCounters.PRISM_OBJECT_COMPARE_COUNT);
		display("Inspector", inspector);
	}


	protected int hackify(int i) {
		// TODO: projector now runs three times instead of one.
		return i*3;
	}
	
}
