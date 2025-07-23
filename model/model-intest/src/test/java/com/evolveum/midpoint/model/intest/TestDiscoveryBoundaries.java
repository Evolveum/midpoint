package com.evolveum.midpoint.model.intest;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * MID-9899 Improve the discovery mechanism to work across objects boundaries
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDiscoveryBoundaries extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("./src/test/resources/discovery");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "resource-9899.xml",
            "5e127000-6f41-4bc3-b5b4-065a6183a97e",
            "resource9899",
            TestDiscoveryBoundaries::populateWithSchema);

    private static final TestObject<RoleType> ROLE_METAROLE =
            TestObject.file(TEST_DIR, "metarole.xml", "e9f41864-5240-4d0f-b7ce-1f94fc9b4ad8");

    private static final TestObject<RoleType> ROLE_A =
            TestObject.file(TEST_DIR, "role-a.xml", "a728590a-be90-4ff4-80a0-608bbdb2d0e6");

    private static final TestObject<RoleType> ROLE_B =
            TestObject.file(TEST_DIR, "role-b.xml", "e4cc516c-2301-4fb9-b0fb-3a9a3e540561");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY, initTask, initResult);

        addObject(ROLE_METAROLE, initTask, initResult);
    }

    private static void populateWithSchema(DummyResourceContoller controller) throws Exception {
        controller.populateWithDefaultSchema();

        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                "l", String.class, false, false);

        controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                "description", String.class, false, false);
    }

    @Test
    public void testDiscoveryAcrossObjectBoundaries() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(ROLE_B, task, result);
        addObject(ROLE_A, task, result);

        assertTestRolesCount(3);
        assertResourceGroupsCount(2);

        DummyResource dummyResource = RESOURCE_DUMMY.controller.getDummyResource();
        dummyResource.deleteGroupByName("b");

        assertResourceGroupsCount(1);
        assertGroupExists("a");

        PrismObject<RoleType> roleA = getObject(RoleType.class, ROLE_A.oid);
        ObjectDelta<RoleType> delta = roleA.createModifyDelta();

        AssignmentType assignment = new AssignmentType();
        assignment.targetRef(ROLE_B.oid, RoleType.COMPLEX_TYPE);

        delta.addModificationAddContainer(RoleType.F_ASSIGNMENT, assignment.asPrismContainerValue());

        try {
            traced(() -> modelService.executeChanges(List.of(delta), null, task, result));
        } finally {
            // only role "a" and "metadata" should be here
            assertRoleExists(ROLE_METAROLE);
            assertRoleExists(ROLE_A);

            // todo this role should be deleted when MID-9899 improvement is fixed
            assertRoleExists(ROLE_B);

            // only group "a" should be here
            assertResourceGroupsCount(1);
            assertGroupExists("a");

            result.computeStatusIfUnknown();
            assertPartialError(result);
        }
    }

    private void assertGroupExists(String groupName) throws Exception {
        DummyResource dummyResource = RESOURCE_DUMMY.controller.getDummyResource();
        Collection<DummyGroup> groups = dummyResource.listGroups();
        Assertions.assertThat(groups).anySatisfy(g -> Assertions.assertThat(g.getName()).isEqualTo(groupName));
    }

    private void assertRoleNotExists(TestObject object) throws Exception {
        try {
            PrismObject<RoleType> role = getObject(RoleType.class, object.oid);
            Assertions.assertThat(role).isNotNull();
            Assertions.fail("Role '" + object.getNameOrig() + "' (" + object.oid + ") should not exist");
        } catch (ObjectNotFoundException e) {
        }
    }

    private void assertRoleExists(TestObject object) throws Exception {
        try {
            getObject(RoleType.class, object.oid);
        } catch (ObjectNotFoundException e) {
            Assertions.fail("Role '" + object.getNameOrig() + "' (" + object.oid + ") should exist");
        }
    }

    private void assertResourceGroupsCount(int expectedCount) throws Exception {
        DummyResource dummyResource = RESOURCE_DUMMY.controller.getDummyResource();
        Collection<DummyGroup> groups = dummyResource.listGroups();
        Assertions.assertThat(groups).hasSize(expectedCount);
    }

    private void assertTestRolesCount(int expectedCount) throws Exception {
        ObjectQuery query = PrismTestUtil.getPrismContext().queryFor(RoleType.class)
                .item(RoleType.F_IDENTIFIER).eq("test")
                .build();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        SearchResultList<PrismObject<RoleType>> roles =
                modelService.searchObjects(RoleType.class, query, null, task, result);

        Assertions.assertThat(roles).hasSize(expectedCount);
    }
}
