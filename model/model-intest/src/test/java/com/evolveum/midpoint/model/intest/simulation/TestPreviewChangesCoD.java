/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.simulation;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Modifier;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.evolveum.midpoint.model.test.TestSimulationResult;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPreviewChangesCoD extends AbstractConfiguredModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestPreviewChangesCoD.class);

    private static final File TEST_DIR = new File("src/test/resources/simulation/cod");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_ORG =
            new TestResource<>(TEST_DIR, "object-template-org.xml", "80d8bdb4-7288-41fe-a8a3-e39f1c9d2de3");

    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            new TestResource<>(TEST_DIR, "object-template-user.xml", "fc5bfc7b-0612-450a-85d2-ab5cff7e4ed9");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(TEST_DIR, "resource-dummy.xml",
            "8dfeccc9-e144-4864-a692-e483f4b1873a", "resource-preview-changes-cod", TestPreviewChangesCoD::createAttributeDefinitions);

    private static final TestResource<RoleType> ROLE_ORG =
            new TestResource<>(TEST_DIR, "role-org.xml", "3d82a1af-0380-4368-b80a-b28a8c87b5bb");

    private static final TestResource<OrgType> ORG_CHILD = new TestResource<>(TEST_DIR, "org-child.xml");

    private static final TestResource<UserType> USER_BOB = new TestResource<>(TEST_DIR, "user-bob.xml");

    private static final TestResource<RoleType> ROLE_META_ASSIGNMENT_SEARCH =
            new TestResource<>(TEST_DIR, "role-meta-assignment-search.xml", "1ac00214-ffd0-49db-a1b9-51b46a0e9ae1");

    @SuppressWarnings("unchecked")
    private static final Class<? extends ObjectType>[] COLLECT_COUNT_TYPES = new Class[] { FocusType.class, ShadowType.class };

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY.initAndTest(this, initTask, initResult);

        TestResource.getAll(ORG_CHILD, USER_BOB);

        addObject(OBJECT_TEMPLATE_ORG, initTask, initResult);
        addObject(OBJECT_TEMPLATE_USER, initTask, initResult);
        addObject(ROLE_META_ASSIGNMENT_SEARCH, initTask, initResult);
        addObject(ROLE_ORG, initTask, initResult);
    }

    private static void createAttributeDefinitions(DummyResourceContoller controller)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                "fullName", String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                "description", String.class, false, false);
        controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                DummyGroup.ATTR_MEMBERS_NAME, String.class, false, true);
    }

    /**
     * The simplest "create on demand" scenario:
     *
     * - `ORG_CHILD` has `parentIdentifier` of "parentOfChild-1".
     * - There is no such org in repo, so normally we should create it on demand.
     * - But we are in a preview mode, so no org should be really created in the repository.
     */
    @Test
    public void test100SimpleCreateOnDemand() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        when("preview for create org, that should search/createOnDemand parent org");

        PrismObject<OrgType> orgChild = ORG_CHILD.get().clone();
        ObjectDelta<OrgType> delta = orgChild.createAddDelta();

        ModelContext<OrgType> context = modelInteractionService.previewChanges(List.of(delta), null, task, result);

        then("No extra object should be created");
        assertCollectedCounts(counts, task, result);

        and("there should be some secondary deltas");
        AssertJUnit.assertNotNull(context);
        displayDumpable("context", context); // TODO assert the deltas
    }

    /**
     * As {@link #test100SimpleCreateOnDemand()} but using `executeChanges` with "no persistent effects" execution mode.
     */
    @Test
    public void test110SimpleCreateOnDemandSimulation() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        given("simple orphan ADD delta");
        ObjectDelta<OrgType> delta = ORG_CHILD.get().clone().createAddDelta();

        when("executeChanges is called in simulation mode");
        TestSimulationResult simResult = executeInProductionSimulationMode(List.of(delta), task, result);

        then("No extra objects should be created");
        assertCollectedCounts(counts, task, result);

        and("there should be some secondary deltas in model context");
        ModelContext<?> context = simResult.getLastModelContext(); // TODO - which one is this? the original or the embedded one
        AssertJUnit.assertNotNull(context);
        displayDumpable("context", context); // TODO assert the deltas
    }

    /**
     * More advanced "create on demand" scenario for `ORG_CHILD`:
     *
     * - `parentIdentifier` is "parentOfChild-1" -> a parent organization with this name should be created (and assigned)
     * - metarole `role-meta-assignment-search` is present -> a role "Role: child org-Read" should be created (and assigned)
     */
    @Test
    public void test150CreateOnDemandForOrgAndRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        given("org ADD delta");
        ObjectDelta<OrgType> delta = createAddDeltaForOrgWithRoleAssignment();

        when("previewChanges is invoked");
        ModelContext<OrgType> context =
                modelInteractionService.previewChanges(
                        List.of(delta), ModelExecuteOptions.create(), task, result);

        then("no new objects are created");
        AssertJUnit.assertNotNull(context);
        assertCollectedCounts(counts, task, result);

        // TODO some asserts here
    }

    private ObjectDelta<OrgType> createAddDeltaForOrgWithRoleAssignment() {
        PrismObject<OrgType> orgChild = ORG_CHILD.get().clone();
        addMetaroleAssignment(orgChild);
        return orgChild.createAddDelta();
    }

    private void addMetaroleAssignment(PrismObject<OrgType> orgChild) {
        orgChild.asObjectable().getAssignment().add(
                new AssignmentType()
                        .targetRef(ROLE_META_ASSIGNMENT_SEARCH.oid, RoleType.COMPLEX_TYPE));
    }

    /**
     * As {@link #test150CreateOnDemandForOrgAndRole()} but using `executeChanges` with "no persistent effects" execution mode.
     */
    @Test
    public void test160CreateOnDemandForOrgAndRoleSimulated() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        given("org ADD delta");
        ObjectDelta<OrgType> delta = createAddDeltaForOrgWithRoleAssignment();

        when("executeChanges is called in simulation mode");
        TestSimulationResult simResult = executeInProductionSimulationMode(List.of(delta), task, result);

        then("No extra objects should be created");
        assertCollectedCounts(counts, task, result);

        and("there should be some secondary deltas in model context");
        ModelContext<?> context = simResult.getLastModelContext(); // TODO - which one is this? the original or the embedded one
        AssertJUnit.assertNotNull(context);
        displayDumpable("context", context); // TODO assert the deltas
    }

    /**
     * User `bob` has `organization` of "parent:child" which means that he should be in `child` org (CoD)
     * that should be in `parent` org (CoD again).
     *
     * The "no-provisioning" flag is not set, so (simulated) groups and group memberships should be created for the orgs
     * and for bob.
     */
    @Test
    public void test200CreateOnDemandWithProvisioning() throws Exception {
        skipIfNotNativeRepository();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        Map<Class<? extends ObjectType>, Integer> counts = collectCounts(task, result);

        given("user bob ADD delta");
        ObjectDelta<UserType> delta = USER_BOB.get().clone().createAddDelta();

        when("executeChanges is called in simulation mode");
        TestSimulationResult simResult = executeInProductionSimulationMode(List.of(delta), task, result);

        then("No extra objects should be created");
        assertCollectedCounts(counts, task, result);

        and("there should be some secondary deltas in model context");
        ModelContext<?> context = simResult.getLastModelContext(); // TODO - which one is this? the original or the embedded one
        AssertJUnit.assertNotNull(context);
        displayDumpable("context", context); // TODO assert the deltas
    }

    /**
     * MID-6166
     */
    @Test
    public void test400MultiThreadSupportForCreateOnDemand() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<PrismObject<OrgType>> orgs = repositoryService.searchObjects(OrgType.class, null, null, result);
        orgs.forEach(org -> {
            try {
                repositoryService.deleteObject(OrgType.class, org.getOid(), result);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        int count = repositoryService.countObjects(OrgType.class, null, null, result);
        AssertJUnit.assertEquals("There shouldn't be org units", 0, count);

        when();

        final int MAX_WORKERS = 10;
        ExecutorService pool = Executors.newFixedThreadPool(MAX_WORKERS);

        List<Callable<Exception>> tasks = new ArrayList<>();
        for (int i = 0; i < MAX_WORKERS; i++) {
            tasks.add(createMultithreadedTask(i, task));
        }

        List<Exception> exceptions = new ArrayList<>();

        List<Future<Exception>> futures = pool.invokeAll(tasks);
        for (Future<Exception> future : futures) {
            Exception ex = future.get();
            if (ex == null) {
                continue;
            }

            exceptions.add(ex);
        }

        result.computeStatusIfUnknown();

        then();

        exceptions.forEach(ex -> LOGGER.error("Error occurred ", ex));

        int orgCount = repositoryService.countObjects(OrgType.class, null, null, result);
        AssertJUnit.assertEquals("Two org should be present", 2, orgCount);

        int userCount = repositoryService.countObjects(UserType.class, null, null, result);
        // user is created in each thread + administrator
        AssertJUnit.assertEquals("Two users should be present", MAX_WORKERS + 1, userCount);

        AssertJUnit.assertEquals("Exception happened during processing", 0, exceptions.size());
    }

    private Callable<Exception> createMultithreadedTask(int id, Task task) {
        return () -> {

            OperationResult result = task.getResult().createSubresult("CoD runnable " + id);

            try {
                login(userAdministrator.clone());

                PrismObject<UserType> bob = USER_BOB.get().clone();
                UserType userBob = bob.asObjectable();
                userBob.setName(new PolyStringType("bob" + id));
                userBob.setDescription("no-provisioning");

                ObjectDelta<UserType> delta = bob.createAddDelta();

                modelService.executeChanges(List.of(delta), ModelExecuteOptions.create(), task, result);
                return null;
            } catch (Exception ex) {
                return ex;
            } finally {
                result.computeStatusIfUnknown();
            }
        };
    }

    private Map<Class<? extends ObjectType>, Integer> collectCounts(Task task, OperationResult result) throws Exception {
        Map<Class<? extends ObjectType>, Integer> map = new HashMap<>();

        for (ObjectTypes type : ObjectTypes.values()) {
            Class<? extends ObjectType> clazz = type.getClassDefinition();
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            if (Arrays.stream(COLLECT_COUNT_TYPES).noneMatch(c -> c.isAssignableFrom(clazz))) {
                continue;
            }

            int count = repositoryService.countObjects(clazz, null, null, result);
            map.put(clazz, count);
        }

        return map;
    }

    private void assertCollectedCounts(Map<Class<? extends ObjectType>, Integer> counts, Task task, OperationResult result) throws Exception {
        StringBuilder msg = new StringBuilder();

        boolean fail = false;
        for (Class<? extends ObjectType> clazz : counts.keySet()) {
            int expected = counts.get(clazz);

            int real = repositoryService.countObjects(clazz, null, null, result);
            if (expected != real) {
                fail = true;
                msg.append(
                        String.format("%s were created, expected: %d, real: %d\n", clazz.getSimpleName(), expected, real));

                if (ShadowType.class.equals(clazz)) {
                    ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_DUMMY.oid, SchemaConstants.RI_GROUP_OBJECT_CLASS);
                    modelService.searchObjects(ShadowType.class, query, null, task, result).forEach(o -> LOGGER.info(o.debugDump()));
                } else {
                    repositoryService.searchObjectsIterative(clazz, null, (o, r) -> {
                        LOGGER.info(o.debugDump());
                        return true;
                    }, null, true, result);
                }
            }
        }

        if (fail) {
            AssertJUnit.fail(msg.toString());
        }
    }
}
