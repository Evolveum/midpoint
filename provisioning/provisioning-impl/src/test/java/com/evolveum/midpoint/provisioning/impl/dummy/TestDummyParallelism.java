/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_GROUP_OBJECT_CLASS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.Counter;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The test of Provisioning service on the API level.
 * <p>
 * This test is focused on parallelism and race conditions.
 * The resource is configured to use proposed shadows and to record all
 * operations.
 * <p>
 * The test is using dummy resource for speed and flexibility.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyParallelism extends AbstractBasicDummyTest {

    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-parallelism");
    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    protected static final String GROUP_SCUM_NAME = "scum";

    private static final long WAIT_TIMEOUT = 60000L;

    private static final int DUMMY_OPERATION_DELAY_RANGE = 1500;
    private static final int MESS_RESOURCE_ITERATIONS = 100;

    private static final Random RND = new Random();

    /**
     * Typical base thread count for non-heavy tasks.
     */
    protected static final int CONCURRENT_TEST_THREAD_COUNT = 4;
    protected static final int CONCURRENT_TEST_THREAD_COUNT_MAX = 15;
    protected static final int CONCURRENT_TEST_THREAD_COUNT_HIGH = Math.min(
            CONCURRENT_TEST_THREAD_COUNT + Runtime.getRuntime().availableProcessors() / 2,
            CONCURRENT_TEST_THREAD_COUNT_MAX);

    protected static final int CONCURRENT_TEST_MAX_START_DELAY_FAST = 10;
    protected static final int CONCURRENT_TEST_MAX_START_DELAY_SLOW = 150;

    private String accountMorganOid;
    private String accountElizabethOid;
    private String accountWallyOid;

    private String groupScumOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        dummyResource.setOperationDelayRange(DUMMY_OPERATION_DELAY_RANGE);
    }

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    // test000-test106 in the superclasses

    protected void assertWillRepoShadowAfterCreate(PrismObject<ShadowType> repoShadow) {
        ShadowAsserter.forShadow(repoShadow, "repo")
                .assertActiveLifecycleState()
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .delta()
                .assertAdd();
    }

    @Test
    public void test120ModifyWillReplaceFullname() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), "Pirate Will Turner");
        displayDumpable("ObjectDelta", delta);
        delta.checkConsistence();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                new OperationProvisioningScriptsType(), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        //noinspection unchecked
        assertDummyAccount(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Pirate Will Turner");

        assertRepoShadow(ACCOUNT_WILL_OID)
                .assertActiveLifecycleState()
                .pendingOperations()
                .assertOperations(2)
                .modifyOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .delta()
                .assertModify();

        syncServiceMock.assertSingleNotifySuccessOnly();

        assertSteadyResource();
    }

    @Test
    public void test190DeleteWill() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        // WHEN
        when();
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, task, result);

        // THEN
        then();
        assertSuccess(result);
        syncServiceMock.assertSingleNotifySuccessOnly();

        assertNoDummyAccount(transformNameFromResource(ACCOUNT_WILL_USERNAME), willIcfUid);

        assertRepoShadow(ACCOUNT_WILL_OID)
                .assertDead()
                .assertIsNotExists()
                .pendingOperations()
                .assertOperations(3)
                .deleteOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .delta()
                .assertDelete();

        assertShadowProvisioning(ACCOUNT_WILL_OID)
                .assertTombstone();

        assertSteadyResource();
    }

    /**
     * Maybe some chance of reproducing MID-5237. But not much,
     * as the dummy resource is uniqueness arbiter here.
     */
    @Test
    public void test200ParallelCreate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        final Counter successCounter = new Counter();
        rememberDummyResourceWriteOperationCount(null);

        // WHEN
        when();

        accountMorganOid = null;

        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    ShadowType account = parseObjectType(ACCOUNT_MORGAN_FILE, ShadowType.class);

                    try {
                        String thisAccountMorganOid = provisioningService.addObject(
                                account.asPrismObject(), null, null, localTask, localResult);
                        successCounter.click();

                        synchronized (dummyResource) {
                            if (accountMorganOid == null) {
                                accountMorganOid = thisAccountMorganOid;
                            } else {
                                assertEquals("Whoops! Create shadow OID mismatch", accountMorganOid, thisAccountMorganOid);
                            }
                        }
                    } catch (ObjectAlreadyExistsException e) {
                        // this is expected ... sometimes
                        logger.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    }

                }, CONCURRENT_TEST_THREAD_COUNT_HIGH, CONCURRENT_TEST_MAX_START_DELAY_FAST);

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        successCounter.assertCount("Wrong number of successful operations", 1);

        PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, accountMorganOid, null, task, result);
        display("Shadow after", shadowAfter);

        assertDummyResourceWriteOperationCountIncrement(null, 1);

        checkUniqueness(shadowAfter);

        assertSteadyResource();
    }

    /**
     * Create a lot parallel modifications for the same property and the same value.
     * These should all be eliminated - except for one of them.
     * <p>
     * There is a slight chance that one of the thread starts after the first operation
     * is finished. But the threads are fast and the operations are slow. So this is
     * a very slim chance.
     */
    @Test
    public void test202ParallelModifyCaptainMorgan() throws Exception {
        PrismObject<ShadowType> shadowAfter = parallelModifyTest(
                () -> prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                        accountMorganOid, dummyResourceCtl.getAttributeFullnamePath(), "Captain Morgan"));

        assertAttribute(shadowAfter, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Captain Morgan");
    }

    /**
     * Create a lot parallel modifications for the same property and the same value.
     * These should all be eliminated - except for one of them.
     * <p>
     * There is a slight chance that one of the thread starts after the first operation
     * is finished. But the threads are fast and the operations are slow. So this is
     * a very slim chance.
     */
    @Test
    public void test204ParallelModifyDisable() throws Exception {
        PrismObject<ShadowType> shadowAfter = parallelModifyTest(
                () -> prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                        accountMorganOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                        ActivationStatusType.DISABLED));

        assertActivationAdministrativeStatus(shadowAfter, ActivationStatusType.DISABLED);
    }

    private PrismObject<ShadowType> parallelModifyTest(
            Callable<ObjectDelta<ShadowType>> deltaProducer) throws Exception {

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        final Counter successCounter = new Counter();
        rememberDummyResourceWriteOperationCount(null);

        // WHEN
        when();

        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    RepositoryCache.enterLocalCaches(cacheConfigurationManager);
                    // Playing with cache, trying to make a worst case
                    repositoryService.getObject(ShadowType.class, accountMorganOid, null, localResult);

                    randomDelay(CONCURRENT_TEST_MAX_START_DELAY_SLOW);
                    logger.info("{} starting to do some work", Thread.currentThread().getName());

                    ObjectDelta<ShadowType> delta = deltaProducer.call();
                    displayDumpable("ObjectDelta", delta);

                    provisioningService.modifyObject(ShadowType.class, accountMorganOid, delta.getModifications(), null, null, localTask, localResult);

                    localResult.computeStatus();
                    display("Thread " + Thread.currentThread().getName() + " DONE, result", localResult);
                    if (localResult.isSuccess()) {
                        successCounter.click();
                    } else if (localResult.isInProgress()) {
                        // expected
                    } else {
                        fail("Unexpected thread result status " + localResult.getStatus());
                    }

                    RepositoryCache.exitLocalCaches();

                }, CONCURRENT_TEST_THREAD_COUNT, CONCURRENT_TEST_MAX_START_DELAY_FAST);

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, accountMorganOid, null, task, result);
        display("Shadow after", shadowAfter);

        successCounter.assertCount("Wrong number of successful operations", 1);

        assertDummyResourceWriteOperationCountIncrement(null, 1);

        assertSteadyResource();

        return shadowAfter;
    }

    @Test
    public void test209ParallelDelete() throws Exception {
        // GIVEN
        final Counter successCounter = new Counter();
        rememberDummyResourceWriteOperationCount(null);

        // WHEN
        when();

        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    RepositoryCache.enterLocalCaches(cacheConfigurationManager);

                    try {
                        display("Thread " + Thread.currentThread().getName() + " START");
                        provisioningService.deleteObject(ShadowType.class, accountMorganOid, null, null, localTask, localResult);
                        localResult.computeStatus();
                        display("Thread " + Thread.currentThread().getName() + " DONE, result", localResult);
                        if (localResult.isSuccess()) {
                            successCounter.click();
                        } else if (localResult.isInProgress()) {
                            // expected
                        } else if (localResult.isHandledError()) {
                            // harmless. The account was just deleted in another thread.
                        } else {
                            fail("Unexpected thread result status " + localResult.getStatus());
                        }
                    } catch (ObjectNotFoundException e) {
                        // this is expected ... sometimes
                        logger.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    } finally {
                        RepositoryCache.exitLocalCaches();
                    }

                }, CONCURRENT_TEST_THREAD_COUNT, CONCURRENT_TEST_MAX_START_DELAY_FAST);

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        successCounter.assertCount("Wrong number of successful operations", 1);

        assertRepoShadow(accountMorganOid)
                .assertTombstone();

        assertShadowProvisioning(accountMorganOid)
                .assertTombstone();

        assertDummyResourceWriteOperationCountIncrement(null, 1);

        assertSteadyResource();
    }

    @Test
    public void test210ParallelCreateSlow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        final Counter successCounter = new Counter();
        rememberDummyResourceWriteOperationCount(null);

        // WHEN
        when();

        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    randomDelay(CONCURRENT_TEST_MAX_START_DELAY_SLOW);
                    logger.info("{} starting to do some work", Thread.currentThread().getName());

                    ShadowType account = parseObjectType(ACCOUNT_ELIZABETH_FILE, ShadowType.class);

                    try {
                        accountElizabethOid = provisioningService.addObject(account.asPrismObject(), null, null, localTask, localResult);
                        successCounter.click();
                    } catch (ObjectAlreadyExistsException e) {
                        // this is expected ... sometimes
                        logger.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    } finally {
                        RepositoryCache.exitLocalCaches();
                    }

                }, CONCURRENT_TEST_THREAD_COUNT, null);

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        successCounter.assertCount("Wrong number of successful operations", 1);

        PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, accountElizabethOid, null, task, result);
        display("Shadow after", shadowAfter);

        assertDummyResourceWriteOperationCountIncrement(null, 1);

        assertSteadyResource();
    }

    /**
     * Create a lot parallel modifications for the same property and the same value.
     * These should all be eliminated - except for one of them.
     * <p>
     * There is a slight chance that one of the thread starts after the first operation
     * is finished. But the threads are fast and the operations are slow. So this is
     * a very slim chance.
     */
    @Test
    public void test212ParallelModifyElizabethSlow() throws Exception {
        PrismObject<ShadowType> shadowAfter = parallelModifyTestSlow(
                () -> prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                        accountElizabethOid, dummyResourceCtl.getAttributeFullnamePath(), "Miss Swan"));

        assertAttribute(shadowAfter, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Miss Swan");
    }

    /**
     * Create a lot parallel modifications for the same property and the same value.
     * These should all be eliminated - except for one of them.
     * <p>
     * There is a slight chance that one of the thread starts after the first operation
     * is finished. But the threads are fast and the operations are slow. So this is
     * a very slim chance.
     */
    @Test
    public void test214ParallelModifyDisableSlow() throws Exception {
        PrismObject<ShadowType> shadowAfter = parallelModifyTestSlow(
                () -> prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                        accountElizabethOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                        ActivationStatusType.DISABLED));

        assertActivationAdministrativeStatus(shadowAfter, ActivationStatusType.DISABLED);
    }

    private PrismObject<ShadowType> parallelModifyTestSlow(
            Callable<ObjectDelta<ShadowType>> deltaProducer) throws Exception {

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        final Counter successCounter = new Counter();
        rememberDummyResourceWriteOperationCount(null);

        when();
        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    RepositoryCache.enterLocalCaches(cacheConfigurationManager);
                    // Playing with cache, trying to make a worst case
                    repositoryService.getObject(
                            ShadowType.class, accountElizabethOid, null, localResult);

                    randomDelay(CONCURRENT_TEST_MAX_START_DELAY_SLOW);
                    logger.info("{} starting to do some work", Thread.currentThread().getName());

                    ObjectDelta<ShadowType> delta = deltaProducer.call();
                    displayDumpable("ObjectDelta", delta);

                    provisioningService.modifyObject(ShadowType.class, accountElizabethOid,
                            delta.getModifications(), null, null, localTask, localResult);

                    localResult.computeStatus();
                    display("Thread " + Thread.currentThread().getName() + " DONE, result", localResult);
                    if (localResult.isSuccess()) {
                        successCounter.click();
                    } else if (localResult.isInProgress()) {
                        // expected
                    } else {
                        fail("Unexpected thread result status " + localResult.getStatus());
                    }

                    RepositoryCache.exitLocalCaches();

                }, CONCURRENT_TEST_THREAD_COUNT, null);

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, accountElizabethOid, null, task, result);
        display("Shadow after", shadowAfter);

        successCounter.assertCount("Wrong number of successful operations", 1);

        assertDummyResourceWriteOperationCountIncrement(null, 1);

        assertSteadyResource();

        return shadowAfter;
    }

    @Test
    public void test229ParallelDeleteSlow() throws Exception {
        // GIVEN
        final Counter successCounter = new Counter();
        rememberDummyResourceWriteOperationCount(null);

        // WHEN
        when();

        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    RepositoryCache.enterLocalCaches(cacheConfigurationManager);
                    // Playing with cache, trying to make a worst case
                    repositoryService.getObject(ShadowType.class, accountElizabethOid, null, localResult);

                    randomDelay(CONCURRENT_TEST_MAX_START_DELAY_SLOW);
                    logger.info("{} starting to do some work", Thread.currentThread().getName());

                    try {
                        display("Thread " + Thread.currentThread().getName() + " START");
                        provisioningService.deleteObject(ShadowType.class, accountElizabethOid, null, null, localTask, localResult);
                        localResult.computeStatus();
                        display("Thread " + Thread.currentThread().getName() + " DONE, result", localResult);
                        if (localResult.isSuccess()) {
                            successCounter.click();
                        } else if (localResult.isInProgress()) {
                            // expected
                        } else {
                            fail("Unexpected thread result status " + localResult.getStatus());
                        }
                    } catch (ObjectNotFoundException e) {
                        // this is expected ... sometimes
                        logger.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
                    } finally {
                        RepositoryCache.exitLocalCaches();
                    }

                }, CONCURRENT_TEST_THREAD_COUNT, null);

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        successCounter.assertCount("Wrong number of successful operations", 1);

        assertRepoShadow(accountElizabethOid)
                .assertTombstone();

        assertShadowProvisioning(accountElizabethOid)
                .assertTombstone();

        assertDummyResourceWriteOperationCountIncrement(null, 1);

        assertSteadyResource();
    }

    /**
     * Search for group that we do not have any shadow for yet.
     * Do that in several threads at once. The group will be "discovered"
     * by the threads at the same time, each thread trying to create shadow.
     * There is a chance that the shadows get duplicated.
     * <p>
     * MID-5237
     */
    @Test
    public void test230ParallelGroupSearch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyGroup groupScum = new DummyGroup(GROUP_SCUM_NAME);
        dummyResource.addGroup(groupScum);

        final Counter successCounter = new Counter();
        rememberDummyResourceWriteOperationCount(null);

        groupScumOid = null;
        dummyResource.setOperationDelayOffset(0);
        dummyResource.setOperationDelayRange(0);
        dummyResource.setSyncSearchHandlerStart(true);

        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    ObjectQuery query = createGroupNameQuery(GROUP_SCUM_NAME);

                    SearchResultList<PrismObject<ShadowType>> foundObjects =
                            provisioningService.searchObjects(
                                    ShadowType.class, query, null, localTask, localResult);
                    assertEquals("Unexpected number of shadows found: " + foundObjects, 1, foundObjects.size());
                    successCounter.click();

                    PrismObject<ShadowType> groupShadow = foundObjects.get(0);

                    synchronized (dummyResource) {
                        if (groupScumOid == null) {
                            groupScumOid = groupShadow.getOid();
                        } else {
                            assertEquals("Whoops! Create shadow OID mismatch", groupScumOid, groupShadow.getOid());
                        }
                    }

                }, 10, null); // No need to user more than 10 threads, we are going to max out connector pool anyway.

        // Give some time for all the threads to start
        // The threads should be blocked just before search handler invocation
        Thread.sleep(100);

        // WHEN
        when();

        // Unblock the handlers. And here we go!
        dummyResource.unblockAll();

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        dummyResource.setSyncSearchHandlerStart(false);
        successCounter.assertCount("Wrong number of successful operations", 10);

        PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, groupScumOid, null, task, result);
        display("Shadow after", shadowAfter);

        checkUniqueness(shadowAfter);

        assertSteadyResource();
    }

    private ObjectQuery createGroupNameQuery(String groupName) throws SchemaException, ConfigurationException {

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(
                RESOURCE_DUMMY_OID,
                new QName(MidPointConstants.NS_RI, OBJECTCLASS_GROUP_LOCAL_NAME));

        ResourceAttributeDefinition<?> attrDef =
                ResourceSchemaFactory.getRawSchemaRequired(resource.asObjectable())
                        .findObjectClassDefinitionRequired(RI_GROUP_OBJECT_CLASS)
                        .findAttributeDefinitionRequired(SchemaConstants.ICFS_NAME);
        ObjectFilter nameFilter = prismContext.queryFor(ShadowType.class)
                .itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getItemName()).eq(groupName)
                .buildFilter();

        query.setFilter(ObjectQueryUtil.filterAnd(query.getFilter(), nameFilter));
        return query;
    }

    /**
     * Several threads reading from resource. Couple other threads try to get into the way
     * by modifying the resource, hence forcing connector re-initialization.
     * The goal is to detect connector initialization race conditions.
     * <p>
     * MID-5068
     */
    @Test
    public void test800ParallelReadAndModifyResource() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Previous test will max out the connector pool
        dummyResource.assertConnections(13);
        assertDummyConnectorInstances(10);

        dummyResource.setOperationDelayOffset(0);
        dummyResource.setOperationDelayRange(0);

        PrismObject<ShadowType> accountBefore = parseObject(ACCOUNT_WALLY_FILE);
        display("Account before", accountBefore);
        accountWallyOid = provisioningService.addObject(accountBefore, null, null, task, result);
        provisioningService.getObject(ShadowType.class, accountWallyOid, null, task, result);
        result.computeStatus();
        if (result.getStatus() != OperationResultStatus.SUCCESS) {
            display("Failed read result (precondition)", result);
            fail("Unexpected read status (precondition): " + result.getStatus());
        }

        // WHEN
        when();

        long t0 = System.currentTimeMillis();
        MutableBoolean readFinished = new MutableBoolean();

        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (threadIndex) -> {

                    // roughly half the threads will try to mess with the resource
                    if (threadIndex < CONCURRENT_TEST_THREAD_COUNT_HIGH / 2) {
                        for (int i = 0; /* neverending */ ; i++) {
                            messResource(threadIndex, i);

                            display("T +" + (System.currentTimeMillis() - t0));

                            if (readFinished.booleanValue()) {
                                break;
                            }
                        }

                    } else if (threadIndex == CONCURRENT_TEST_THREAD_COUNT_HIGH / 2) {
                        for (int i = 0; /* neverending */ ; i++) {
                            Task localTask = createPlainTask(testName + "-test-thread-" + i);
                            OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, localTask, result);

                            logger.debug("PAR: TESTing " + threadIndex + "." + i);

                            display("PAR: TESTed " + threadIndex + "." + i + ": " + testResult.getStatus());

                            if (testResult.getStatus() != OperationResultStatus.SUCCESS) {
                                display("Failed test resource result", testResult);
                                readFinished.setValue(true);
                                fail("Unexpected test resource result status: " + testResult.getStatus());
                            }

                            if (readFinished.booleanValue()) {
                                break;
                            }
                        }

                    } else {
                        // the rest nearly half of the threads will try to do operations
                        try {
                            // TODO: why is the constant MESS_RESOURCE_ITERATIONS used for operations?
                            for (int i = 0; i < MESS_RESOURCE_ITERATIONS; i++) {
                                Task localTask = createPlainTask(testName + "-op-thread-" + i);
                                OperationResult localResult = localTask.getResult();

                                logger.debug("PAR: OPing " + threadIndex + "." + i);

                                Object out = doResourceOperation(task, localResult);

                                localResult.computeStatus();
                                display("PAR: OPed " + threadIndex + "." + i + ": " + out + ": " + localResult.getStatus());

                                if (localResult.getStatus() != OperationResultStatus.SUCCESS) {
                                    display("Failed read result", localResult);
                                    readFinished.setValue(true);
                                    fail("Unexpected read status: " + localResult.getStatus());
                                }

                                if (readFinished.booleanValue()) {
                                    break;
                                }
                            }
                        } finally {
                            readFinished.setValue(true);
                        }
                    }

                    display("mischief managed (" + threadIndex + ")");

                }, CONCURRENT_TEST_THREAD_COUNT_HIGH, CONCURRENT_TEST_MAX_START_DELAY_FAST);

        // THEN
        then();
        waitForThreads(threads, WAIT_TIMEOUT);

        PrismObject<ResourceType> resourceAfter = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        display("resource after", resourceAfter);

        List<ConnectorOperationalStatus> stats = provisioningService.getConnectorOperationalStatus(RESOURCE_DUMMY_OID, task, result);
        display("Dummy connector stats after", stats);

        displayValue("Dummy resource connections", dummyResource.getConnectionCount());

        //-3 because of connection for tests that use resource without oid (2 partial configuration, 1 discover configuration)
        assertDummyConnectorInstances(dummyResource.getConnectionCount() - 3);
    }

    private Object doResourceOperation(Task task, OperationResult result) throws Exception {
        int op = RND.nextInt(3);

        if (op == 0) {
            return provisioningService.getObject(ShadowType.class, accountWallyOid, null, task, result);

        } else if (op == 1) {
            ObjectQuery query = ObjectQueryUtil.createResourceAndKind(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT);
            List<PrismObject<ShadowType>> list = new ArrayList<>();
            ResultHandler<ShadowType> handler = (o, or) -> {
                list.add(o);
                return true;
            };
            provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);
            return list;

        } else if (op == 2) {
            ObjectQuery query = ObjectQueryUtil.createResourceAndKind(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT);
            return provisioningService.searchObjects(ShadowType.class, query, null, task, result);
        }
        return null;
    }

    private void messResource(int threadIndex, int i)
            throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, PolicyViolationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException {
        Task task = createPlainTask("mess-" + threadIndex + "-" + i);
        OperationResult result = task.getResult();
        List<ItemDelta<?, ?>> deltas = deltaFor(ResourceType.class)
                .item(ResourceType.F_DESCRIPTION).replace("Iter " + threadIndex + "." + i)
                .asItemDeltas();

        logger.debug("PAR: MESSing " + threadIndex + "." + i);

        provisioningService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, deltas, null, null, task, result);
        result.computeStatus();

        display("PAR: MESSed " + threadIndex + "." + i + ": " + result.getStatus());

        if (result.getStatus() != OperationResultStatus.SUCCESS) {
            display("Failed mess resource result", result);
            fail("Unexpected mess resource result status: " + result.getStatus());
        }
    }
}
