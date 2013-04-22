package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
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

    private static final Trace LOGGER = TraceManager.getTrace(ModifyAssignmentTest.class);
    private static final File TEST_DIR = new File("src/test/resources/modify/assignment");

    @BeforeClass
    public void beforeClass() throws Exception {
        super.beforeClass();

        PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
        PrismObject role = domProcessor.parseObject(new File(TEST_DIR, "role.xml"));

        OperationResult result = new OperationResult("add role");
        repositoryService.addObject(role, null, result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void test10AddAssignment() throws Exception {
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-add-assignment.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("add assignment");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void test11AddInducement() throws Exception {
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(
                new File(TEST_DIR, "modify-add-inducement.xml"), ObjectModificationType.class);

        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, RoleType.class, prismContext);

        OperationResult result = new OperationResult("add inducement");
        repositoryService.modifyObject(RoleType.class, delta.getOid(), delta.getModifications(), result);
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void test20ModifyAssignment() {

    }

    @Test
    public void test21ModifyInducement() {

    }

    @Test
    public void test30DeleteAssignment() {

    }

    @Test
    public void test31DeleteInducement() {

    }
}
