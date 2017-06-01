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
import java.util.Collection;
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
import com.evolveum.midpoint.prism.PrismObjectDefinition;
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
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER = 600;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER = 400;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_ORDINARY = 1;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS = NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER + NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER + NUMBER_OF_CHEESE_ASSIGNMENTS_ORDINARY;
	
	private static final Trace LOGGER = TraceManager.getTrace(TestPlentyOfAssignments.class);
	
	private Inspector inspector;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		importObjectFromFile(ROLE_BASIC_FILE, initResult);
		
		generateRoles(initResult);
		
		inspector = new Inspector();
		InternalMonitor.setInspector(inspector);
	}
	
	private void generateRoles(OperationResult result) throws Exception {
		long startMillis = System.currentTimeMillis();
		
		PrismObjectDefinition<RoleType> roleDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class);
		for(int i=0; i < NUMBER_OF_GENERATED_ROLES; i++) {
			PrismObject<RoleType> role = roleDefinition.instantiate();
			RoleType roleType = role.asObjectable();
			String name = String.format("role%04d", i);
			String oid = generateRoleOid(i);
			roleType.setName(createPolyStringType(name));
			roleType.setOid(oid);
			LOGGER.info("Adding {}", role);
			repositoryService.addObject(role, null, result);
			LOGGER.info("  added {}", role);
		}
		
		long endMillis = System.currentTimeMillis();
		long duration = (endMillis - startMillis);
		display("Roles import", "import of "+NUMBER_OF_GENERATED_ROLES+" roles took "+(duration/1000)+" seconds ("+(duration/NUMBER_OF_GENERATED_ROLES)+"ms per role)");
	}

	private String generateRoleOid(int num) {
		return String.format("00000000-0000-ffff-2000-00000000%04d", num);
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
	
	// TODO preview changes

	private String assignmentSummary(PrismObject<UserType> user) {
		Map<String,Integer> assignmentRelations = new HashMap<>();
		for (AssignmentType assignment: user.asObjectable().getAssignment()) {
			ObjectReferenceType targetRef = assignment.getTargetRef();
			relationToMap(assignmentRelations, assignment.getTargetRef());
		}
		Map<String,Integer> memRelations = new HashMap<>();
		for (ObjectReferenceType ref: user.asObjectable().getRoleMembershipRef()) {
			relationToMap(memRelations, ref);
		}
		return "User "+user
				+"\n  "+user.asObjectable().getAssignment().size()+" assignments\n    "+assignmentRelations
				+"\n  "+user.asObjectable().getRoleMembershipRef().size()+" roleMembershipRefs\n    "+memRelations;
	}

	private void relationToMap(Map<String, Integer> map, ObjectReferenceType ref) {
		if (ref != null) {
			Integer i = map.get(ref.getRelation().getLocalPart());
			if (i == null) {
				i = 0;
			}
			i++;
			map.put(ref.getRelation().getLocalPart(), i);
		}
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
	
	private class Inspector implements InternalInspector, DebugDumpable {

		@SuppressWarnings("rawtypes")
		Map<Class,Integer> readMap = new HashMap<>();
		
		@Override
		public <O extends ObjectType> void inspectRepositoryRead(Class<O> type, String oid) {
			Integer i = readMap.get(type);
			if (i == null) {
				i = 0;
			}
			i++;
			readMap.put(type, i);
		}
		
		public <O extends ObjectType> void assertRead(Class<O> type, int expectedCount) {
			assertEquals("Unexpected number of reads of "+type.getSimpleName(), (Integer)expectedCount, readMap.get(type));
		}

		private void reset() {
			readMap = new HashMap<>();
		}

		@Override
		public String debugDump(int indent) {
			StringBuilder sb = new StringBuilder();
			DebugUtil.indentDebugDump(sb, indent);
			DebugUtil.debugDumpWithLabel(sb, "read", readMap, indent + 1);
			return sb.toString();
		}
		
		
		
	}
}
