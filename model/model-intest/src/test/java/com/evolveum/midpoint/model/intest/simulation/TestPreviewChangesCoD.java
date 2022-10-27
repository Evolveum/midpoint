/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.simulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPreviewChangesCoD extends AbstractConfiguredModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/simulation/cod");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_ORG =
            new TestResource<>(TEST_DIR, "object-template-org.xml", "80d8bdb4-7288-41fe-a8a3-e39f1c9d2de3");

    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            new TestResource<>(TEST_DIR, "object-template-user.xml", "fc5bfc7b-0612-450a-85d2-ab5cff7e4ed9");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(TEST_DIR, "resource-dummy.xml",
            "8dfeccc9-e144-4864-a692-e483f4b1873a", "resource-preview-changes-cod", TestPreviewChangesCoD::createAttributeDefinitions);

    private static final TestResource<OrgType> ORG_CHILD = new TestResource<>(TEST_DIR, "org-child.xml");

    private static final TestResource<UserType> USER_BOB = new TestResource<>(TEST_DIR, "user-bob.xml");

    private static final TestResource<RoleType> ROLE_META_ASSIGNMENT_SEARCH =
            new TestResource<>(TEST_DIR, "role-meta-assignment-search.xml", "1ac00214-ffd0-49db-a1b9-51b46a0e9ae1");

    private static final TestResource<RoleType> ROLE_META_ASSOCIATION_SEARCH =
            new TestResource<>(TEST_DIR, "role-meta-assignment-search.xml", "07edb2fc-5662-4886-aba7-54fbc58ce5ca");

    private static final Class<? extends ObjectType>[] COLLECT_COUNT_TYPES = new Class[] { FocusType.class, ShadowType.class };

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        TestResource.read(ORG_CHILD, USER_BOB);

        addObject(OBJECT_TEMPLATE_ORG, initTask, initResult);
        addObject(OBJECT_TEMPLATE_USER, initTask, initResult);
        addObject(ROLE_META_ASSIGNMENT_SEARCH, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY, initTask, initResult);
    }

    public static void createAttributeDefinitions(DummyResourceContoller controller)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                "fullName", String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                "description", String.class, false, false);
    }

    @Test
    public void test100OrgNotProvisioned() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        when();

        PrismObject<OrgType> orgChild = ORG_CHILD.getObject().clone();
        ObjectDelta<OrgType> delta = orgChild.createAddDelta();

        ModelContext<OrgType> context = modelInteractionService.previewChanges(Collections.singletonList(delta), ModelExecuteOptions.create(), task, result);

        then();

        AssertJUnit.assertNotNull(context);
        assertCollectedCounts(counts, task, result);
    }

    @Test
    public void test150OrgNotProvisionedWithMetarole() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        when();

        PrismObject<OrgType> orgChild = ORG_CHILD.getObject().clone();
        // we'll add assignment to meta role
        orgChild.asObjectable().getAssignment().add(new AssignmentType().targetRef(ROLE_META_ASSIGNMENT_SEARCH.oid, RoleType.COMPLEX_TYPE));
        ObjectDelta<OrgType> delta = orgChild.createAddDelta();

        ModelContext<OrgType> context = modelInteractionService.previewChanges(Collections.singletonList(delta), ModelExecuteOptions.create(), task, result);

        then();

        AssertJUnit.assertNotNull(context);
        assertCollectedCounts(counts, task, result);
    }

    @Test
    public void test200ComplexCase() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        when();

        PrismObject<UserType> orgChild = USER_BOB.getObject().clone();
        ObjectDelta<UserType> delta = orgChild.createAddDelta();

        ModelContext<UserType> context = modelInteractionService.previewChanges(Collections.singletonList(delta), ModelExecuteOptions.create(), task, result);

        then();

        AssertJUnit.assertNotNull(context);
        assertCollectedCounts(counts, task, result);
    }

    @Test
    public void test300ReferenceTargetSearch() throws Exception {
//        given();
//
//        Task task = getTestTask();
//        OperationResult result = task.getResult();
//
//        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);
//
//        when();
//
//        then();
//
//        AssertJUnit.assertNotNull(context);
//        assertCollectedCounts(counts, task, result);
    }

    /**
     * MID-7155
     */
    @Test
    public void test400MultiThreadSupportForCreateOnDemand() throws Exception {

    }

    private Map<Class<? extends ObjectType>, Integer> collectCounts(Task task, OperationResult result) throws Exception {
        Map<Class<? extends ObjectType>, Integer> map = new HashMap<>();

        for (ObjectTypes type : ObjectTypes.values()) {
            Class<? extends ObjectType> clazz = type.getClassDefinition();
            if (Arrays.stream(COLLECT_COUNT_TYPES).noneMatch(c -> c.isAssignableFrom(clazz))) {
                continue;
            }

            int count = modelService.countObjects(clazz, null, null, task, result);
            map.put(clazz, count);
        }

        return map;
    }

    private void assertCollectedCounts(Map<Class<? extends ObjectType>, Integer> counts, Task task, OperationResult result) throws Exception {
        for (Class<? extends ObjectType> clazz : counts.keySet()) {
            int expected = counts.get(clazz);

            int real = modelService.countObjects(clazz, null, null, task, result);
            AssertJUnit.assertEquals(clazz.getSimpleName() + " were created", expected, real);
        }
    }
}
