/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyAssignmentTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/modify/assignment");

    private static final File FILE_ROLE = new File(TEST_DIR, "role.xml");

    private static final String ROLE_OID = "00000000-8888-6666-0000-100000000005";

    private static final String OLD_ASSIGNMENT_OID = "12345678-d34d-b33f-f00d-987987987988";
    private static final String NEW_ASSIGNMENT_OID = "12345678-d34d-b33f-f00d-987987987989";
    private static final String NEW_INDUCEMENT_OID = "12345678-d34d-b33f-f00d-987987987987";

	private static final String ROLE_A1_OID = "aaaa00aa-aa00-aa00-a0a0-000000000001";
	private static final String ROLE_A2_OID = "aaaa00aa-aa00-aa00-a0a0-000000000002";

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        resetPrismContext(MidPointPrismContextFactory.FACTORY);
        //given
        //no role

        PrismObject<RoleType> role = prismContext.parseObject(FILE_ROLE);

        OperationResult result = new OperationResult("add role");
        
        // WHEN
        String oid = repositoryService.addObject(role, null, result);
        
        // THEN
        assertSuccess(result);
        assertEquals(ROLE_OID, oid);

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);
        
        PrismAsserts.assertEquals(FILE_ROLE, repoRole);
    }

    @Test
    public void test010AddAssignment() throws Exception {
        //given

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "modify-add-assignment.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);
        OperationResult result = new OperationResult("add assignment");
        
        // WHEN
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        // THEN
        assertSuccess(result);

        //check role and its assignments and inducements
        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);
        System.out.println("role after: "  + repoRole.debugDump());

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        assertNotNull(inducement);
        assertEquals(2, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value4 = assignment.getValue(4L);
        PrismReference targetRef = value4.findReference(AssignmentType.F_TARGET_REF);
        assertNotNull(targetRef);
        assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        assertEquals(OLD_ASSIGNMENT_OID, refValue.getOid());
        assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());
    }

    @Test
    public void test011AddInducement() throws Exception {
        //given
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "modify-add-inducement.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("add inducement");
        
        // WHEN
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        // THEN
        assertSuccess(result);

        //check role and its assignments and inducements
        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);
        System.out.println("role: " + repoRole.debugDump());

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value5 = inducement.getValue(5L);
        PrismContainer<ConstructionType> accConstruction = value5.findContainer(AssignmentType.F_CONSTRUCTION);
        assertNotNull(accConstruction);
        assertEquals(1, accConstruction.getValues().size());
    }

    @Test
    public void test020ModifyAssignment() throws Exception {
        //given

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "modify-assignment.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("modify assignment");
        
        // WHEN
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value4 = assignment.getValue(4L);
        PrismReference targetRef = value4.findReference(AssignmentType.F_TARGET_REF);
        assertNotNull(targetRef);
        assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        assertEquals(NEW_ASSIGNMENT_OID, refValue.getOid());
        assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());
    }

    @Test
    public void test021ModifyInducement() throws Exception {
        //given

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "modify-inducement.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("modify inducement");
        
        // WHEN
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value5 = inducement.getValue(5L);
        PrismReference targetRef = value5.findReference(AssignmentType.F_TARGET_REF);
        assertNotNull(targetRef);
        assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        assertEquals(NEW_INDUCEMENT_OID, refValue.getOid());
        assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());

        PrismProperty accConstruction = value5.findProperty(AssignmentType.F_CONSTRUCTION);
        AssertJUnit.assertNull(accConstruction);
    }

    @Test
    public void test030DeleteAssignment() throws Exception {
        //given

        AssignmentType a = new AssignmentType();
        a.setId(4L);
        ObjectDelta<RoleType> delta = ObjectDelta.createModificationDeleteContainer(RoleType.class,
                "00000000-8888-6666-0000-100000000005", RoleType.F_ASSIGNMENT, prismContext, a);

        OperationResult result = new OperationResult("delete assignment");

        // WHEN
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals(1, assignment.getValues().size());

        assertNotNull(assignment.getValue(1L));

        Session session = open();
        try {
            Query query = session.createSQLQuery("select count(*) from m_assignment where owner_oid=:oid and id=:id");
            query.setParameter("oid", delta.getOid());
            query.setParameter("id", 4);
            Number number = (Number) query.uniqueResult();
            assertEquals(0, number.intValue());
        } finally {
            close(session);
        }
    }

    @Test
    public void test031DeleteInducement() throws Exception {
        //given

        //when
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "modify-delete-inducement.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("delete inducement");

        // WHEN
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        assertNotNull(inducement);
        assertEquals(2, inducement.getValues().size());

        assertNotNull(inducement.getValue(2L));
        assertNotNull(inducement.getValue(5L));

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals(1, assignment.getValues().size());

        assertNotNull(assignment.getValue(1L));
    }

    /**
     * Test for MID-1374
     */
    @Test
    public void test040RenameAssignmentToInducement() throws Exception {
        //given

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(
                new File(TEST_DIR, "modify-delete-add-assignment.xml"), ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("delete add assignment");
        
        // WHEN
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        assertNotNull(inducement.getValue(1L));
        assertNotNull(inducement.getValue(2L));
        assertNotNull(inducement.getValue(5L));
    }
    
    @Test
    public void test100AssignmentAdd() throws Exception {
    	final String TEST_NAME = "test100AssignmentAdd";
        //given

        OperationResult result = createResult(TEST_NAME);
    	
    	PrismObject<RoleType> roleBefore = getObject(RoleType.class, ROLE_OID);
    	display("Role before", roleBefore);
    	
        AssignmentType assignmentToAdd = new AssignmentType();
        assignmentToAdd.targetRef(ROLE_A1_OID, RoleType.COMPLEX_TYPE);
        List<ItemDelta<?, ?>> deltas = deltaFor(RoleType.class)
        		.item(RoleType.F_ASSIGNMENT).add(assignmentToAdd)
        		.asItemDeltas();
		
        // WHEN
        repositoryService.modifyObject(RoleType.class, ROLE_OID, deltas, result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_OID);
        display("Role after", roleAfter);

        PrismContainer<AssignmentType> assignment = roleAfter.findContainer(new ItemPath(RoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals("Wrong number of assignments", 1, assignment.getValues().size());
        
        assertEquals("Wrong assignment id", (Long) 6L, assignment.getValues().iterator().next().getId());
    }

    
    /**
     * Add and delete assignment in one operation. Make sure that the container id is not reused.
     * 
     * MID-4463
     */
    @Test
    public void test110AssignmentAddDeleteIds() throws Exception {
    	final String TEST_NAME = "test110AssignmentAddDeleteIds";
        //given

        OperationResult result = createResult(TEST_NAME);
    	
    	PrismObject<RoleType> roleBefore = getObject(RoleType.class, ROLE_OID);
    	display("Role before", roleBefore);
    	
        AssignmentType assignmentToAdd = new AssignmentType();
        assignmentToAdd.targetRef(ROLE_A2_OID, RoleType.COMPLEX_TYPE);
		
		AssignmentType assignmentToDelete = new AssignmentType();
		Long origAssingmentId = roleBefore.asObjectable().getAssignment().iterator().next().getId();
		assertNotNull(origAssingmentId);
		assignmentToDelete.setId(origAssingmentId);
		
		List<ItemDelta<?, ?>> deltas = deltaFor(RoleType.class)
        		.item(RoleType.F_ASSIGNMENT)
        			.add(assignmentToAdd)
        			.delete(assignmentToDelete)
        		.asItemDeltas();
        
        // WHEN
        repositoryService.modifyObject(RoleType.class, ROLE_OID, deltas, result);

        // THEN
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_OID);
        display("Role after", roleAfter);

        PrismContainer<AssignmentType> assignment = roleAfter.findContainer(new ItemPath(RoleType.F_ASSIGNMENT));
        assertNotNull(assignment);
        assertEquals("Wrong number of assignments", 1, assignment.getValues().size());
        
        assertEquals("Wrong assignment id", (Long) (origAssingmentId + 1), assignment.getValues().iterator().next().getId());
    }

}
