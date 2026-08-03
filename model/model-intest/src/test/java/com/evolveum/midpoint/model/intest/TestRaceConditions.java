/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.intest.util.DelayingProgressListener;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.test.util.AbstractMultithreadCycleRunner;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.util.CheckedRunnable;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRaceConditions extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/contract");

    private static final DummyTestResource RESOURCE_DUMMY_CONFLICT = new DummyTestResource(
            TEST_DIR, "resource-dummy-conflict.xml", "f6ff3f3f-290e-475c-b1aa-96bad5058322", "conflict",
            TestRaceConditions::initializeResource);

    private static final TestTask TASK_LIVE_SYNC_CONFLICT = TestTask.file(
            TEST_DIR, "task-live-sync-conflict.xml", "56b0caba-9682-409b-815b-878029b42ef0");

    private static final TestObject<ResourceType> RESOURCE_DUMMY_CONFLICT_ABSTRACT = TestObject.file(
            TEST_DIR, "resource-dummy-conflict-abstract.xml", "1770d7f2-45e7-4e7a-8598-b98e042c95df");
    private static final DummyTestResource RESOURCE_DUMMY_CONFLICT_1 = new DummyTestResource(
            TEST_DIR, "resource-dummy-conflict-1.xml", "600d9e19-c948-4fa3-b2ba-0b09bfbb200a", "conflict-1",
            TestRaceConditions::initializeResource);
    private static final DummyTestResource RESOURCE_DUMMY_CONFLICT_2 = new DummyTestResource(
            TEST_DIR, "resource-dummy-conflict-2.xml", "2b0e7322-0f93-498d-8096-5c739a1add39", "conflict-2",
            TestRaceConditions::initializeResource);
    private static final DummyTestResource RESOURCE_DUMMY_CONFLICT_3 = new DummyTestResource(
            TEST_DIR, "resource-dummy-conflict-3.xml", "f9208c71-6ef6-4cb9-a890-821cd3a06a51", "conflict-3",
            TestRaceConditions::initializeResource);
    private static final DummyTestResource RESOURCE_DUMMY_CONFLICT_4 = new DummyTestResource(
            TEST_DIR, "resource-dummy-conflict-4.xml", "f372a5bc-a197-44c7-a277-fef0878fd2bf", "conflict-4",
            TestRaceConditions::initializeResource);

    private static final TestTask TASK_LIVE_SYNC_CONFLICT_1 = TestTask.file(
            TEST_DIR, "task-live-sync-conflict-1.xml", "6c448591-6c03-4c48-a82d-060d6a614e2b");
    private static final TestTask TASK_LIVE_SYNC_CONFLICT_2 = TestTask.file(
            TEST_DIR, "task-live-sync-conflict-2.xml", "a36241bb-49b9-4f31-8fbd-c1706c5016b5");
    private static final TestTask TASK_LIVE_SYNC_CONFLICT_3 = TestTask.file(
            TEST_DIR, "task-live-sync-conflict-3.xml", "be22f4bb-70ec-4951-94bb-c9f41ffb0300");
    private static final TestTask TASK_LIVE_SYNC_CONFLICT_4 = TestTask.file(
            TEST_DIR, "task-live-sync-conflict-4.xml", "fe3da318-53d6-48e9-98b3-be21ef536fab");

    /** Roles that will be assigned based on 'privileges' account attribute in the live sync tests. */
    private List<RoleType> roles;

    /** Number of {@link #roles} to create. */
    private static final int ROLE_COUNT = 10;

    private static void initializeResource(DummyResourceContoller c) {
        c.populateWithDefaultSchema();
        c.getDummyResource().setSyncStyle(DummySyncStyle.SMART);
    }

    @Autowired private ModelService modelService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.ARCHETYPE_IMPORT_TASK.init(this, initTask, initResult);

        initAndTestDummyResource(RESOURCE_DUMMY_CONFLICT, initTask, initResult);

        initTestObjects(initTask, initResult, RESOURCE_DUMMY_CONFLICT_ABSTRACT);
        initAndTestDummyResource(RESOURCE_DUMMY_CONFLICT_1, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_CONFLICT_2, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_CONFLICT_3, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_CONFLICT_4, initTask, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        roles = modelObjectCreatorFor(RoleType.class)
                .withObjectCount(ROLE_COUNT)
                .withNamePattern("role-%d")
                .execute(initResult);
    }

    @Override
    protected ConflictResolutionActionType getDefaultConflictResolutionAction() {
        return ConflictResolutionActionType.RECOMPUTE;
    }

    @Test
    public void test100AssignRoles() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        @SuppressWarnings({ "raw" })
        ObjectDelta<UserType> objectDelta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(
                        ObjectTypeUtil.createAssignmentTo(ROLE_PIRATE_OID, ObjectTypes.ROLE),
                        ObjectTypeUtil.createAssignmentTo(ROLE_SAILOR_OID, ObjectTypes.ROLE))
                .asObjectDelta(USER_JACK_OID);
        executeChangesAssertSuccess(objectDelta, null, task, result);

        // THEN
        then();
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);

        String accountJackOid = getSingleLinkOid(userJack);

        // Check shadow
        var accountShadow = getShadowRepo(accountJackOid);
        assertDummyAccountShadowRepo(accountShadow, accountJackOid, "jack");

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountJackOid, "jack", "Jack Sparrow");

        // Check account in dummy resource
        assertDefaultDummyAccount("jack", "Jack Sparrow", true);
    }

    /**
     * Remove both roles at once, in different threads.
     */
    @Test
    public void test110UnassignRoles() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        List<AssignmentType> assignments = userJack.asObjectable().getAssignment();
        assertEquals("Wrong # of assignments", 2, assignments.size());

        OperationResult subresult1 = result.createSubresult("thread1");
        OperationResult subresult2 = result.createSubresult("thread1");

        // WHEN
        Thread t1 = new Thread(() -> deleteAssignment(userJack, 0, task, subresult1));
        Thread t2 = new Thread(() -> deleteAssignment(userJack, 1, task, subresult2));
        t1.start();
        t2.start();
        t1.join(30000L);
        t2.join(30000L);

        // THEN
        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User after change execution", userJackAfter);
        assertEquals("Unexpected # of projections of jack", 0, userJackAfter.asObjectable().getLinkRef().size());
    }

    private void deleteAssignment(PrismObject<UserType> user, int index, Task task, OperationResult result) {
        try {
            login(userAdministrator.copy()); // without cloning there are conflicts on login->getPrincipal->recompute
            @SuppressWarnings({ "raw" })
            ObjectDelta<UserType> objectDelta = deltaFor(UserType.class)
                    .item(FocusType.F_ASSIGNMENT).delete(user.asObjectable().getAssignment().get(index).clone())
                    .asObjectDelta(USER_JACK_OID);

            var options = ModelExecuteOptions.create()
                    .focusConflictResolution(new ConflictResolutionType()
                            .action(ConflictResolutionActionType.RESTART));

            modelService.executeChanges(Collections.singletonList(objectDelta), options, task,
                    Collections.singletonList(new DelayingProgressListener(0, 1000)), result);
        } catch (Throwable t) {
            throw new SystemException(t);
        }
    }

    /**
     * Assign the same role concurrently in different threads.
     * There should be a single assignment at the end.
     *
     * #10714
     */
    @Test
    public void test120AssignRoleConcurrently() throws Exception {
        testAssignRoleConcurrently(
                ConflictResolutionActionType.RECOMPUTE,
                null,
                ConflictResolutionActionType.RESTART,
                "the role is assigned just once"
        );
    }

    /**
     * Task execution environment carries RESTART conflict resolution while system
     * configuration has none. The task-level policy should take effect, resolving
     * all concurrent assignment conflicts so that the role ends up assigned exactly once.
     *
     * Priority chain: options > task > system config. This test covers the task level.
     */
    @Test
    public void test130ConflictResolutionFromTask() throws Exception {
        testAssignRoleConcurrently(
                null,
                ConflictResolutionActionType.RESTART,
                null,
                "the role is assigned exactly once, conflict resolved by task-level policy"
        );
    }

    /**
     * ModelExecuteOptions carry RESTART conflict resolution while the task execution
     * environment carries NONE (which would suppress retries). Options sit at the top
     * of the priority chain (options > task > system config), so RESTART wins and all
     * concurrent assignment conflicts are resolved.
     *
     * Priority chain: options > task > system config. This test covers the options level.
     */
    @Test
    public void test131OptionsOverrideTaskConflictResolution() throws Exception {
        testAssignRoleConcurrently(
                ConflictResolutionActionType.RECOMPUTE,
                ConflictResolutionActionType.NONE,
                ConflictResolutionActionType.RESTART,
                "the role is assigned exactly once, options RESTART took precedence over task NONE");
    }

    private void testAssignRoleConcurrently(
            ConflictResolutionActionType systemResolutionAction,
            ConflictResolutionActionType taskResolutionAction,
            ConflictResolutionActionType optionsResolutionAction,
            String finalThenMessage) throws Exception {

        skipIfNotNativeRepository();

        var task = getTestTask();
        var result = task.getResult();

        int THREADS = 4;
        long DURATION = 10_000L;

        given("system configuration has " + systemResolutionAction + " as default conflict resolution");
        assumeConflictResolutionAction(systemResolutionAction);

        given("a user without assignments");
        UserType user = new UserType().name(getTestName());
        String oid = addObject(user.asPrismObject(), task, result);

        var options = optionsResolutionAction != null ?
                ModelExecuteOptions.create()
                .focusConflictResolution(new ConflictResolutionType()
                                         .action(optionsResolutionAction)
                                         .maxAttempts(10))
                : null;

        when("assigning the same role concurrently");
        ParallelTestThread[] threads = multithread(
                new AbstractMultithreadCycleRunner(DURATION) {
                    @Override
                    public void init(int threadIndex) throws Exception {
                        super.init(threadIndex);
                        login(userAdministrator.clone());
                    }

                    @Override
                    public void run(int threadIndex, int cycleNumber) throws Exception {
                        Task localTask = createTask(getTestNameShort());
                        if (taskResolutionAction != null) {
                            localTask.setExecutionEnvironment(new TaskExecutionEnvironmentType()
                                    .conflictResolution(new ConflictResolutionType()
                                            .action(taskResolutionAction)
                                            .maxAttempts(10)));
                        }

                        modifyAssignmentHolderAssignment(
                                UserType.class,
                                oid,
                                SystemObjectsType.ROLE_SUPERUSER.value(),
                                RoleType.COMPLEX_TYPE,
                                SchemaConstants.ORG_DEFAULT,
                                localTask,
                                null,
                                null,
                                true,
                                options,
                                localTask.getResult());
                    }
                },
                THREADS, null);
        waitForThreads(threads, DURATION * 10);

        then(finalThenMessage);
        assertUserAfter(oid).assertAssignments(1);
    }

    /**
     * A realistic test for #10714: Creates multiple LiveSync tasks operating on a single user.
     * Gradually, each of the task is made to create an assignment to a given role. (Using an inbound
     * with `assignmentTargetSearch` evaluator, driven by an attribute containing role names to assign.)
     *
     * Without special conditioning, duplicate assignments are created, because the tasks operate in parallel.
     * (Moreover, the processing is artificially slowed down, so the conflicts are guaranteed to occur.)
     *
     * However, by setting conflict resolution action to `restart`, we make sure that conflicting updates are
     * detected and treated by restarting the particular change processing.
     */
    @Test
    public void test140TestLiveSyncConflictResolution() throws Exception {
        skipIfNotNativeRepository();

        given();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        assumeConflictResolutionAction(null);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        final String userName = "jdoe";
        final int liveSyncTaskCount = 10;

        DummyAccount account = createAndImportAccount(RESOURCE_DUMMY_CONFLICT, userName, result);

        and("array of live sync tasks to be run concurrently");
        List<PrismObject<TaskType>> tasks = new ArrayList<>();
        for (int i = 0; i < liveSyncTaskCount; i++) {
            PrismObject<TaskType> lsTask = TASK_LIVE_SYNC_CONFLICT.getFresh();
            lsTask.asObjectable()
                    .oid(UUID.randomUUID().toString())
                    .name("Live-sync task " + i);

            tasks.add(lsTask);

            addObject(lsTask, task, result);
        }

        // wait until LS tasks get latest token
        Thread.sleep(3000);

        List<String> taskOids = tasks.stream()
                .map(t -> t.getOid())
                .toList();

        for (int i = 0; i < ROLE_COUNT; i++) {
            logger.info("Adding value to privileges attribute, iteration {}", i);

            var role = roles.get(i);
            var roleName = role.getName().getOrig();

            when("adding the name of role " + roleName + " to privileges attribute");

            account.addAttributeValue(DummyAccount.ATTR_PRIVILEGES_NAME, roleName);

            int expectedAssignments = i + 1;

            for (String oid : taskOids) {
                // Progress for LS tasks is the number of changes processed. Here we just wait for all tasks to process
                // (at least) the currently introduced change.
                waitForTaskProgress(oid, expectedAssignments, 20000, result);
            }

            then("user is there, and it has no duplicate assignments (i.e. only " + expectedAssignments + " of them)");

            assertUserByUsername(userName, "")
                    .assertAssignments(expectedAssignments);
        }
    }

    private DummyAccount createAndImportAccount(DummyTestResource resource, String userName, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaViolationException, ConflictException, InterruptedException,
            ObjectDoesNotExistException, CommonException, IOException {

        given("account '%s' on resource '%s' that will be live-synced".formatted(userName, resource.name));
        DummyAccount account = resource.controller.addAccount(userName);

        and("account imported as a midPoint user");
        importAccountsRequest()
                .withResourceOid(resource.oid)
                .withNameValue(userName)
                .executeOnForeground(result);

        return account;
    }

    /**
     * As {@link #test140TestLiveSyncConflictResolution()} but each LiveSync task adds its own data - conflicting executions
     * are no longer "mutually equivalent". So, we now check that each LiveSync operation is actually executed.
     *
     * It is ensured by executing LiveSync tasks against different resources, where each one brings a specific value to
     * `organization` property.
     *
     * Unlike previous test, here the LiveSync tasks are running "synchronously": they have no schedule. Instead, they
     * are started on demand using {@link MyThreadPool}. It is probably neither better or worse -- just a different approach.
     */
    @Test
    public void test150TestLiveSyncConflictResolutionComplex() throws Exception {
        skipIfNotNativeRepository();

        given();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        assumeConflictResolutionAction(null);

        var task = getTestTask();
        var result = task.getResult();

        final String userName = getTestNameShort();

        var accounts = List.of(
                createAndImportAccount(RESOURCE_DUMMY_CONFLICT_1, userName, result),
                createAndImportAccount(RESOURCE_DUMMY_CONFLICT_2, userName, result),
                createAndImportAccount(RESOURCE_DUMMY_CONFLICT_3, userName, result),
                createAndImportAccount(RESOURCE_DUMMY_CONFLICT_4, userName, result));

        var tasks = List.of(
                TASK_LIVE_SYNC_CONFLICT_1,
                TASK_LIVE_SYNC_CONFLICT_2,
                TASK_LIVE_SYNC_CONFLICT_3,
                TASK_LIVE_SYNC_CONFLICT_4);

        initTestObjects(task, result,
                tasks.toArray(new TestObject<?>[0]));

        var expectedOrganizations = new HashSet<String>();

        var pool = new MyThreadPool();
        try {
            pool.setup();

            given("running tasks to obtain initial tokens");
            pool.runTasksOnce(tasks);

            for (int roleNumber = 0; roleNumber < ROLE_COUNT; roleNumber++) {
                var role = roles.get(roleNumber);
                var roleName = role.getName().getOrig();

                when("adding role " + roleName + " (with tags) to 'privileges' attribute");
                for (int accountNumber = 0; accountNumber < accounts.size(); accountNumber++) {
                    DummyAccount account = accounts.get(accountNumber);
                    String privilege = "%s#%d".formatted(roleName, accountNumber);
                    account.addAttributeValue(DummyAccount.ATTR_PRIVILEGES_NAME, privilege);
                    expectedOrganizations.add(privilege);
                }

                and("running LS tasks");
                pool.runTasksOnce(tasks); // most probably these two changes will be processed as two independent ones

                var expectedAssignments = roleNumber + 1;

                then("user is there, and it has correct data");
                assertUserByUsername(userName, "")
                        .assertAssignments(expectedAssignments)
                        .assertOrganizations(expectedOrganizations.toArray(new String[0]));
            }
        } finally {
            pool.shutdown();
        }
    }

    /** Used mainly to run related tasks quasi-synchronously. */
    class MyThreadPool {

        private ThreadPoolExecutor pool;

        public void setup() {

            // The goal is to have worker threads with administrator being logged in.
            ThreadFactory factory = new ThreadFactory() {

                private final AtomicInteger seq = new AtomicInteger();

                @Override
                public Thread newThread(@NotNull Runnable r) {
                    return new Thread(() -> {
                        try {
                            System.out.println("Logging in worker thread " + Thread.currentThread().getName());
                            login(userAdministrator.clone());
                            r.run();
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    }, "worker-" + seq.incrementAndGet());
                }
            };

            pool = new ThreadPoolExecutor(
                    4, 4,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    factory);
            pool.prestartAllCoreThreads();
        }

        Future<?> submit(CheckedRunnable runnable) {
            return pool.submit(() -> {
                try {
                    runnable.run();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            });
        }

        void shutdown() {
            if (pool != null) {
                pool.shutdown();
            }
        }

        void runTasksOnce(List<TestTask> tasks) {
            System.out.println("Running tasks once");
            var futures = tasks.stream()
                    .map(task -> submit(() -> {
                        System.out.println("Running task " + task + " in " + Thread.currentThread().getName());
                        task.rerun(new OperationResult("dummy"));
                        task.doAssert("")
                                .assertSuccess();
                        System.out.println("DONE Running task " + task + " in " + Thread.currentThread().getName());
                    }))
                    .toList();
            futures.forEach(future -> {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new AssertionError(e);
                }
            });
            System.out.println("DONE Running tasks once");
        }
    }
}
