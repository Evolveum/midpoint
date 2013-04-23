package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

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

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        //given
        //no role

        //when
        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        PrismObject role = domProcessor.parseObject(FILE_ROLE);

        OperationResult result = new OperationResult("add role");
        String oid = repositoryService.addObject(role, null, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        //then
        AssertJUnit.assertTrue(result.isSuccess());
        AssertJUnit.assertEquals(ROLE_OID, oid);

        result = new OperationResult("get role");
        PrismObject repoRole = repositoryService.getObject(RoleType.class, ROLE_OID, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());
        PrismAsserts.assertEquals(FILE_ROLE, repoRole);
    }

    @Test
    public void test10AddAssignment() throws Exception {
        //given

        //when
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-add-assignment.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("add assignment");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        //then
        AssertJUnit.assertTrue(result.isSuccess());

        //check role and its assignments and inducements
        result = new OperationResult("get role");
        PrismObject repoRole = repositoryService.getObject(RoleType.class, ROLE_OID, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        AssertJUnit.assertNotNull(inducement);
        AssertJUnit.assertEquals(2, inducement.getValues().size());

        PrismContainer assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        AssertJUnit.assertNotNull(assignment);
        AssertJUnit.assertEquals(2, assignment.getValues().size());

        PrismContainerValue value4 = assignment.getValue(4L);
        PrismReference targetRef = value4.findReference(AssignmentType.F_TARGET_REF);
        AssertJUnit.assertNotNull(targetRef);
        AssertJUnit.assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        AssertJUnit.assertEquals(OLD_ASSIGNMENT_OID, refValue.getOid());
        AssertJUnit.assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());
    }

    @Test
    public void test11AddInducement() throws Exception {
        //given

        //when
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-add-inducement.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("add inducement");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        //then
        AssertJUnit.assertTrue(result.isSuccess());

        //check role and its assignments and inducements
        result = new OperationResult("get role");
        PrismObject repoRole = repositoryService.getObject(RoleType.class, ROLE_OID, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        AssertJUnit.assertNotNull(inducement);
        AssertJUnit.assertEquals(3, inducement.getValues().size());

        PrismContainer assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        AssertJUnit.assertNotNull(assignment);
        AssertJUnit.assertEquals(2, assignment.getValues().size());

        PrismContainerValue value5 = inducement.getValue(5L);
        PrismProperty accConstruction = value5.findProperty(AssignmentType.F_ACCOUNT_CONSTRUCTION);
        AssertJUnit.assertNotNull(accConstruction);
        AssertJUnit.assertEquals(1, accConstruction.getValues().size());
    }

    @Test
    public void test20ModifyAssignment() throws Exception {
        //given

        //when
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-assignment.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("modify assignment");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        //then
        AssertJUnit.assertTrue(result.isSuccess());

        result = new OperationResult("get role");
        PrismObject repoRole = repositoryService.getObject(RoleType.class, ROLE_OID, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        AssertJUnit.assertNotNull(inducement);
        AssertJUnit.assertEquals(3, inducement.getValues().size());

        PrismContainer assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        AssertJUnit.assertNotNull(assignment);
        AssertJUnit.assertEquals(2, assignment.getValues().size());

        PrismContainerValue value4 = assignment.getValue(4L);
        PrismReference targetRef = value4.findReference(AssignmentType.F_TARGET_REF);
        AssertJUnit.assertNotNull(targetRef);
        AssertJUnit.assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        AssertJUnit.assertEquals(NEW_ASSIGNMENT_OID, refValue.getOid());
        AssertJUnit.assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());
    }

    @Test
    public void test21ModifyInducement() throws Exception {
        //given

        //when
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-inducement.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("modify inducement");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        //then
        AssertJUnit.assertTrue(result.isSuccess());

        result = new OperationResult("get role");
        PrismObject repoRole = repositoryService.getObject(RoleType.class, ROLE_OID, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        AssertJUnit.assertNotNull(inducement);
        AssertJUnit.assertEquals(3, inducement.getValues().size());

        PrismContainer assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        AssertJUnit.assertNotNull(assignment);
        AssertJUnit.assertEquals(2, assignment.getValues().size());

        PrismContainerValue value5 = inducement.getValue(5L);
        PrismReference targetRef = value5.findReference(AssignmentType.F_TARGET_REF);
        AssertJUnit.assertNotNull(targetRef);
        AssertJUnit.assertEquals(1, targetRef.getValues().size());
        PrismReferenceValue refValue = targetRef.getValue();
        AssertJUnit.assertEquals(NEW_INDUCEMENT_OID, refValue.getOid());
        AssertJUnit.assertEquals(RoleType.COMPLEX_TYPE, refValue.getTargetType());

        PrismProperty accConstruction = value5.findProperty(AssignmentType.F_ACCOUNT_CONSTRUCTION);
        AssertJUnit.assertNull(accConstruction);
    }

    @Test
    public void test30DeleteAssignment() throws Exception {
        //given

        //when
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-delete-assignment.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("delete assignment");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        //then
        AssertJUnit.assertTrue(result.isSuccess());

        result = new OperationResult("get role");
        PrismObject repoRole = repositoryService.getObject(RoleType.class, ROLE_OID, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        AssertJUnit.assertNotNull(inducement);
        AssertJUnit.assertEquals(3, inducement.getValues().size());

        PrismContainer assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        AssertJUnit.assertNotNull(assignment);
        AssertJUnit.assertEquals(1, assignment.getValues().size());

        AssertJUnit.assertNotNull(assignment.getValue(1L));
    }

    @Test
    public void test31DeleteInducement() throws Exception {
        //given

        //when
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-delete-inducement.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("delete inducement");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        //then
        AssertJUnit.assertTrue(result.isSuccess());

        result = new OperationResult("get role");
        PrismObject repoRole = repositoryService.getObject(RoleType.class, ROLE_OID, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());

        PrismContainer inducement = repoRole.findContainer(new ItemPath(RoleType.F_INDUCEMENT));
        AssertJUnit.assertNotNull(inducement);
        AssertJUnit.assertEquals(2, inducement.getValues().size());

        AssertJUnit.assertNotNull(inducement.getValue(2L));
        AssertJUnit.assertNotNull(inducement.getValue(5L));

        PrismContainer assignment = repoRole.findContainer(new ItemPath(AbstractRoleType.F_ASSIGNMENT));
        AssertJUnit.assertNotNull(assignment);
        AssertJUnit.assertEquals(1, assignment.getValues().size());

        AssertJUnit.assertNotNull(assignment.getValue(1L));
    }
}
