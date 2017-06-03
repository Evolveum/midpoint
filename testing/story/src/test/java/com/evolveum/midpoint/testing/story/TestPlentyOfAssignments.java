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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPlentyOfAssignments extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "plenty-of-assignments");
	
	public static final File USER_CHEESE_FILE = new File(TEST_DIR, "user-cheese.xml");
	public static final String USER_CHEESE_OID = "9e796c76-45e0-11e7-9dfd-1792e56081d0";
	
	public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	public static final String ROLE_BASIC_OID = "6909ff20-45e4-11e7-b0a3-0fe76ff4380e";

	private static final int NUMBER_OF_ORDINARY_ROLES = 2; // including superuser role
	private static final int NUMBER_OF_GENERATED_ROLES = 1000;
	private static final String GENERATED_ROLE_OID_FORMAT = "00000000-0000-ffff-2000-00000000%04d"; 
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER = 600;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER = 400;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_ORDINARY = 1;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS = NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER + NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER + NUMBER_OF_CHEESE_ASSIGNMENTS_ORDINARY;
	
	private static final Trace LOGGER = TraceManager.getTrace(TestPlentyOfAssignments.class);
	
	private RepoReadInspector inspector;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		importObjectFromFile(ROLE_BASIC_FILE, initResult);
		
		generateRoles(NUMBER_OF_GENERATED_ROLES, "role%04d", GENERATED_ROLE_OID_FORMAT, null, initResult);
		
		inspector = new RepoReadInspector();
		InternalMonitor.setInspector(inspector);
	}

	private String generateRoleOid(int num) {
		return String.format(GENERATED_ROLE_OID_FORMAT, num);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTile(TEST_NAME);

        assertObjects(RoleType.class, NUMBER_OF_GENERATED_ROLES + NUMBER_OF_ORDINARY_ROLES);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());
	}
	
	@Test
    public void test100AddCheese() throws Exception {
		final String TEST_NAME = "test100AddCheese";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> cheeseBefore = prepareCheese();
        display("Cheese before", assignmentSummary(cheeseBefore));
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        addObject(cheeseBefore, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Added cheese in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_CHEESE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> cheeseAfter = getUser(USER_CHEESE_OID);
        display("Cheese after", assignmentSummary(cheeseAfter));
        assertCheeseRoleMembershipRef(cheeseAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
//        inspector.assertRead(RoleType.class, NUMBER_OF_CHEESE_ASSIGNMENTS);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
	}
	
	
	@Test
    public void test110RecomputeCheese() throws Exception {
		final String TEST_NAME = "test110RecomputeCheese";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> cheeseBefore = prepareCheese();
        display("Cheese before", assignmentSummary(cheeseBefore));
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_CHEESE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Recomputed cheese in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_CHEESE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> cheeseAfter = getUser(USER_CHEESE_OID);
        display("Cheese after", assignmentSummary(cheeseAfter));
        assertCheeseRoleMembershipRef(cheeseAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());
        
        display("Inspector", inspector);

        inspector.assertRead(RoleType.class, 1);
//        assertRepositoryReadCount(4); // may be influenced by tasks
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test120CheesePreviewChanges() throws Exception {
		final String TEST_NAME = "test120CheesePreviewChanges";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> cheeseBefore = prepareCheese();
        display("Cheese before", assignmentSummary(cheeseBefore));
        
        ObjectDelta<UserType> delta = cheeseBefore.createModifyDelta();
        delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "123");
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
		ModelContext<ObjectType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Preview cheese in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_CHEESE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> cheeseAfter = getUser(USER_CHEESE_OID);
        display("Cheese after", assignmentSummary(cheeseAfter));
        assertCheeseRoleMembershipRef(cheeseAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());
        
        display("Inspector", inspector);

        inspector.assertRead(RoleType.class, 1);
//        assertRepositoryReadCount(4); // may be influenced by tasks
        assertPrismObjectCompareCount(0);
	}

	private PrismObject<UserType> prepareCheese() throws Exception {
		PrismObject<UserType> cheese = PrismTestUtil.parseObject(USER_CHEESE_FILE);
		addAssignments(cheese, SchemaConstants.ORG_APPROVER, 0, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER);
		addAssignments(cheese, SchemaConstants.ORG_OWNER, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER, NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER);
		return cheese;
	}

	private void addAssignments(PrismObject<UserType> user, QName relation, int offset, int num) {
		UserType userType = user.asObjectable();
		for (int i = 0; i < num; i++) {
			AssignmentType assignmentType = new AssignmentType();
			String oid = generateRoleOid(offset + i);
			assignmentType.targetRef(oid, RoleType.COMPLEX_TYPE, relation);
			userType.getAssignment().add(assignmentType);
		}
	}

	private void assertCheeseRoleMembershipRef(PrismObject<UserType> cheese) {
		
		assertRoleMembershipRefs(cheese, SchemaConstants.ORG_APPROVER, 0, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER);
		assertRoleMembershipRefs(cheese, SchemaConstants.ORG_OWNER, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER, NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER);
		
		assertRoleMembershipRef(cheese, ROLE_BASIC_OID, SchemaConstants.ORG_DEFAULT);
		
		assertRoleMembershipRefs(cheese, NUMBER_OF_CHEESE_ASSIGNMENTS);
	}

	private void assertRoleMembershipRefs(PrismObject<UserType> user, QName relation, int offset, int num) {
		for (int i = 0; i < num; i++) {
			assertRoleMembershipRef(user, relation, offset + i);
		}
	}

	private void assertRoleMembershipRef(PrismObject<UserType> user, QName relation, int num) {
		assertRoleMembershipRef(user, generateRoleOid(num), relation);
	}
	
	private void assertRoleMembershipRef(PrismObject<UserType> user, String roleOid, QName relation) {
		List<ObjectReferenceType> roleMembershipRefs = user.asObjectable().getRoleMembershipRef();
		for (ObjectReferenceType roleMembershipRef: roleMembershipRefs) {
			if (ObjectTypeUtil.referenceMatches(roleMembershipRef, roleOid, RoleType.COMPLEX_TYPE, relation)) {
				return;
			}
		}
		fail("Cannot find membership of role "+roleOid+" ("+relation.getLocalPart()+") in "+user);
	}

}
