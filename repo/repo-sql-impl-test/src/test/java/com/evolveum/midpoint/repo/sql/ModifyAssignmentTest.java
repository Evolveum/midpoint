/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.List;

import com.evolveum.midpoint.repo.sql.util.RUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ModifyAssignmentTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/modify/assignment");

    private static final File FILE_ROLE = new File(TEST_DIR, "role.xml");

    private static final String ROLE_OID = "00000000-8888-6666-0000-100000000005";

    private static final String ORIGINAL_ASSIGNMENT_4_TARGET_OID = "12345678-d34d-b33f-f00d-987987987988";
    private static final String NEW_ASSIGNMENT_4_TARGET_OID = "12345678-d34d-b33f-f00d-987987987989";
    private static final String ORIGINAL_INDUCEMENT_5_TARGET_OID = "00000000-76e0-48e2-86d6-3d4f02d3e1a2";
    private static final String NEW_INDUCEMENT_5_TARGET_OID = "12345678-d34d-b33f-f00d-987987987987";
    private static final String MOVED_ASSIGNMENT_TARGET_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3e333";

    private static final String ROLE_A1_OID = "aaaa00aa-aa00-aa00-a0a0-000000000001";
    private static final String ROLE_A2_OID = "aaaa00aa-aa00-aa00-a0a0-000000000002";

    @Override
    public void initSystem() throws Exception {
        PrismObject<RoleType> role = prismContext.parseObject(FILE_ROLE);

        OperationResult result = new OperationResult("add role");

        String oid = repositoryService.addObject(role, null, result);

        assertThatOperationResult(result).isSuccess();
        assertEquals(ROLE_OID, oid);

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismAsserts.assertEquals(FILE_ROLE, repoRole);
    }

    @Test
    public void test010AddAssignment() throws Exception {
        given();
        AssignmentType assignment1 = new AssignmentType()
                .id(4L)
                .targetRef(ORIGINAL_ASSIGNMENT_4_TARGET_OID, RoleType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT).add(assignment1)
                .asObjectDelta(ROLE_OID);

        OperationResult result = new OperationResult("add assignment");

        when();
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        then();
        assertThatOperationResult(result).isSuccess();

        //check role and its assignments and inducements
        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);
        System.out.println("role after: " + repoRole.debugDump());

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull(inducement);
        assertEquals(2, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(AbstractRoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value4 = assignment.getValue(4L);
        PrismReference targetRef = value4.findReference(AssignmentType.F_TARGET_REF);
        assertNotNull(targetRef);
        assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        assertEquals(ORIGINAL_ASSIGNMENT_4_TARGET_OID, refValue.getOid());
        assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());
    }

    @Test
    public void test011AddInducement() throws Exception {
        given();
        AssignmentType inducement1 = new AssignmentType()
                .id(5L)
                .beginConstruction()
                .resourceRef(ORIGINAL_INDUCEMENT_5_TARGET_OID, ResourceType.COMPLEX_TYPE)
                .end();

        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT).add(inducement1)
                .asObjectDelta(ROLE_OID);

        OperationResult result = new OperationResult("add inducement");

        when();
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        then();
        assertThatOperationResult(result).isSuccess();

        //check role and its assignments and inducements
        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);
        System.out.println("role: " + repoRole.debugDump());

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(AbstractRoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value5 = inducement.getValue(5L);
        PrismContainer<ConstructionType> accConstruction = value5.findContainer(AssignmentType.F_CONSTRUCTION);
        assertNotNull(accConstruction);
        assertEquals(1, accConstruction.getValues().size());
    }

    @Test
    public void test020ModifyAssignment() throws Exception {
        given();
        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT, 4L, AssignmentType.F_TARGET_REF)
                .replace(new ObjectReferenceType().oid(NEW_ASSIGNMENT_4_TARGET_OID).type(RoleType.COMPLEX_TYPE))
                .asObjectDelta(ROLE_OID);

        OperationResult result = new OperationResult("modify assignment");

        when();
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        then();
        assertThatOperationResult(result).isSuccess();

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(AbstractRoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value4 = assignment.getValue(4L);
        PrismReference targetRef = value4.findReference(AssignmentType.F_TARGET_REF);
        assertNotNull(targetRef);
        assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        assertEquals(NEW_ASSIGNMENT_4_TARGET_OID, refValue.getOid());
        assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());
    }

    @Test
    public void test021ModifyInducement() throws Exception {
        given();
        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT, 5L, AssignmentType.F_TARGET_REF)
                .replace(new ObjectReferenceType().oid(NEW_INDUCEMENT_5_TARGET_OID).type(RoleType.COMPLEX_TYPE))
                .asObjectDelta(ROLE_OID);

        OperationResult result = new OperationResult("modify inducement");

        when();
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        then();
        assertThatOperationResult(result).isSuccess();

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(AbstractRoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals(2, assignment.getValues().size());

        PrismContainerValue<AssignmentType> value5 = inducement.getValue(5L);
        PrismReference targetRef = value5.findReference(AssignmentType.F_TARGET_REF);
        assertNotNull(targetRef);
        assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        assertEquals(NEW_INDUCEMENT_5_TARGET_OID, refValue.getOid());
        assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());

        PrismProperty<Void> accConstruction = value5.findProperty(AssignmentType.F_CONSTRUCTION);
        AssertJUnit.assertNull(accConstruction);
    }

    @Test
    public void test030DeleteAssignment() throws Exception {
        given();
        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT).delete(new AssignmentType().id(4L))
                .asObjectDelta(ROLE_OID);

        OperationResult result = new OperationResult("delete assignment");

        when();
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        then();
        assertThatOperationResult(result).isSuccess();

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(AbstractRoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals(1, assignment.getValues().size());

        assertNotNull(assignment.getValue(1L));

        EntityManager em = open();
        try {
            Query query = em.createNativeQuery(
                    "select count(*) from m_assignment where owner_oid=:oid and id=:id", Integer.class);
            query.setParameter("oid", delta.getOid());
            query.setParameter("id", 4);
            Integer number = RUtil.getSingleResultOrNull(query);
            assertEquals(0, number.intValue());
        } finally {
            close(em);
        }
    }

    @Test
    public void test031DeleteInducement() throws Exception {
        given();
        AssignmentType i = new AssignmentType()
                .id(3L)
                .targetRef(ORIGINAL_INDUCEMENT_5_TARGET_OID, OrgType.COMPLEX_TYPE);

        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_INDUCEMENT).delete(i)
                .asObjectDelta(ROLE_OID);

        OperationResult result = new OperationResult("delete inducement");

        when();
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        then();
        assertThatOperationResult(result).isSuccess();

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull(inducement);
        assertEquals(2, inducement.getValues().size());

        assertNotNull(inducement.getValue(2L));
        assertNotNull(inducement.getValue(5L));

        PrismContainer<AssignmentType> assignment = repoRole.findContainer(AbstractRoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals(1, assignment.getValues().size());

        assertNotNull(assignment.getValue(1L));
    }

    /**
     * Test for MID-1374
     */
    @Test
    public void test040RenameAssignmentToInducement() throws Exception {
        given();
        AssignmentType a = new AssignmentType()
                .id(1L)
                .beginConstruction()
                .resourceRef(MOVED_ASSIGNMENT_TARGET_OID, ResourceType.COMPLEX_TYPE)
                .end();

        AssignmentType in = a.clone();
        in.setId(null);

        ObjectDelta<RoleType> delta = deltaFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT).delete(a.clone())
                .item(RoleType.F_INDUCEMENT).add(in)
                .asObjectDelta(ROLE_OID);

        OperationResult result = new OperationResult("delete add assignment");

        when();
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);

        then();
        assertThatOperationResult(result).isSuccess();

        PrismObject<RoleType> repoRole = getObject(RoleType.class, ROLE_OID);

        PrismContainer<AssignmentType> inducement = repoRole.findContainer(RoleType.F_INDUCEMENT);
        assertNotNull(inducement);
        assertEquals(3, inducement.getValues().size());

        assertNotNull(inducement.getValue(6L));
        assertNotNull(inducement.getValue(2L));
        assertNotNull(inducement.getValue(5L));
    }

    @Test
    public void test100AssignmentAdd() throws Exception {
        given();
        OperationResult result = createOperationResult();

        PrismObject<RoleType> roleBefore = getObject(RoleType.class, ROLE_OID);
        displayValue("Role before", roleBefore);

        AssignmentType assignmentToAdd = new AssignmentType();
        assignmentToAdd.targetRef(ROLE_A1_OID, RoleType.COMPLEX_TYPE);
        List<ItemDelta<?, ?>> deltas = deltaFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT).add(assignmentToAdd)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(RoleType.class, ROLE_OID, deltas, result);

        then();
        assertThatOperationResult(result).isSuccess();

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_OID);
        displayValue("Role after", roleAfter);

        PrismContainer<AssignmentType> assignment = roleAfter.findContainer(RoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals("Wrong number of assignments", 1, assignment.getValues().size());

        assertEquals("Wrong assignment id", (Long) 7L, assignment.getValues().iterator().next().getId());
    }

    /**
     * Add and delete assignment in one operation. Make sure that the container id is not reused.
     * <p>
     * MID-4463
     */
    @Test
    public void test110AssignmentAddDeleteIds() throws Exception {
        given();
        OperationResult result = createOperationResult();

        PrismObject<RoleType> roleBefore = getObject(RoleType.class, ROLE_OID);
        displayValue("Role before", roleBefore);

        AssignmentType assignmentToAdd = new AssignmentType();
        assignmentToAdd.targetRef(ROLE_A2_OID, RoleType.COMPLEX_TYPE);

        AssignmentType assignmentToDelete = new AssignmentType();
        Long origAssignmentId = roleBefore.asObjectable().getAssignment().iterator().next().getId();
        assertNotNull(origAssignmentId);
        assignmentToDelete.setId(origAssignmentId);

        List<ItemDelta<?, ?>> deltas = deltaFor(RoleType.class)
                .item(RoleType.F_ASSIGNMENT)
                .delete(assignmentToDelete)
                .add(assignmentToAdd)
                .asItemDeltas();

        when();
        repositoryService.modifyObject(RoleType.class, ROLE_OID, deltas, result);

        then();
        assertThatOperationResult(result).isSuccess();

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_OID);
        displayValue("Role after", roleAfter);

        PrismContainer<AssignmentType> assignment = roleAfter.findContainer(RoleType.F_ASSIGNMENT);
        assertNotNull(assignment);
        assertEquals("Wrong number of assignments", 1, assignment.getValues().size());

        assertEquals("Wrong assignment id", (Long) (origAssignmentId + 1), assignment.getValues().iterator().next().getId());
    }
}
