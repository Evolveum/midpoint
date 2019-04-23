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

/**
 *
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.Counter;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.util.FailableProducer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The test of Provisioning service on the API level.
 *
 * This test is focused on parallelism and race conditions.
 * The resource is configured to use proposed shadows and to record all
 * operations.
 *
 * The test is using dummy resource for speed and flexibility.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestDummyParallelism extends AbstractBasicDummyTest {

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyParallelism.class);

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-parallelism");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	private static final long WAIT_TIMEOUT = 60000L;

	private static final int DUMMY_OPERATION_DELAY_RANGE = 1500;

	private static final int MESS_RESOURCE_ITERATIONS = 200;
	
	private final Random RND = new Random();

	private String accountMorganOid;
	private String accountElizabethOid;
	private String accountWallyOid;

	protected int getConcurrentTestNumberOfThreads() {
		return 5;
	}

	protected int getConcurrentTestFastRandomStartDelayRange() {
		return 10;
	}
	
	protected int getConcurrentTestSlowRandomStartDelayRange() {
		return 150;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		dummyResource.setOperationDelayRange(DUMMY_OPERATION_DELAY_RANGE);
//		InternalMonitor.setTraceConnectorOperation(true);
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
		final String TEST_NAME = "test120ModifyWillReplaceFullname";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = ObjectDelta.createModificationReplaceProperty(ShadowType.class,
				ACCOUNT_WILL_OID, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Pirate Will Turner");
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

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

		syncServiceMock.assertNotifySuccessOnly();

		assertSteadyResource();
	}
	
	@Test
	public void test190DeleteWill() throws Exception {
		final String TEST_NAME = "test190DeleteWill";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		syncServiceMock.assertNotifySuccessOnly();

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

	@Test
	public void test200ParallelCreate() throws Exception {
		final String TEST_NAME = "test200ParallelCreate";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		final Counter successCounter = new Counter();
		rememberDummyResourceWriteOperationCount(null);

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();

					ShadowType account = parseObjectType(ACCOUNT_MORGAN_FILE, ShadowType.class);

					try {
						accountMorganOid = provisioningService.addObject(account.asPrismObject(), null, null, localTask, localResult);
						successCounter.click();
					} catch (ObjectAlreadyExistsException e) {
						// this is expected ... sometimes
						LOGGER.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
					}

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestFastRandomStartDelayRange());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, WAIT_TIMEOUT);

		successCounter.assertCount("Wrong number of successful operations", 1);

		PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, accountMorganOid, null, task, result);
		display("Shadow after", shadowAfter);

		assertDummyResourceWriteOperationCountIncrement(null, 1);

		assertSteadyResource();
	}

	/**
	 * Create a lot parallel modifications for the same property and the same value.
	 * These should all be eliminated - except for one of them.
	 * 
	 * There is a slight chance that one of the thread starts after the first operation
	 * is finished. But the threads are fast and the operations are slow. So this is
	 * a very slim chance.
	 */
	@Test
	public void test202ParallelModifyCaptainMorgan() throws Exception {
		final String TEST_NAME = "test202ParallelModifyCaptainMorgan";
		
		PrismObject<ShadowType> shadowAfter = parallelModifyTest(TEST_NAME,
				() -> ObjectDelta.createModificationReplaceProperty(ShadowType.class,
						accountMorganOid, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Captain Morgan"));
		
		assertAttribute(shadowAfter, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Captain Morgan");
	}
	
	/**
	 * Create a lot parallel modifications for the same property and the same value.
	 * These should all be eliminated - except for one of them.
	 * 
	 * There is a slight chance that one of the thread starts after the first operation
	 * is finished. But the threads are fast and the operations are slow. So this is
	 * a very slim chance.
	 */
	@Test
	public void test204ParallelModifyDisable() throws Exception {
		final String TEST_NAME = "test204ParallelModifyDisable";
		
		PrismObject<ShadowType> shadowAfter = parallelModifyTest(TEST_NAME,
				() -> ObjectDelta.createModificationReplaceProperty(ShadowType.class,
						accountMorganOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext, ActivationStatusType.DISABLED));

		assertActivationAdministrativeStatus(shadowAfter, ActivationStatusType.DISABLED);
	}
		
	private PrismObject<ShadowType> parallelModifyTest(final String TEST_NAME, FailableProducer<ObjectDelta<ShadowType>> deltaProducer) throws Exception {
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		final Counter successCounter = new Counter();
		rememberDummyResourceWriteOperationCount(null);

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();
					
					RepositoryCache.enter();
					// Playing with cache, trying to make a worst case
					PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, accountMorganOid, null, localResult);

					randomDelay(getConcurrentTestSlowRandomStartDelayRange());
					LOGGER.info("{} starting to do some work", Thread.currentThread().getName());
					
					ObjectDelta<ShadowType> delta = deltaProducer.run();
					display("ObjectDelta", delta);
					
					provisioningService.modifyObject(ShadowType.class, accountMorganOid, delta.getModifications(), null, null, localTask, localResult);
					
					localResult.computeStatus();
					display("Thread "+Thread.currentThread().getName()+" DONE, result", localResult);
					if (localResult.isSuccess()) {
						successCounter.click();
					} else if (localResult.isInProgress()) {
						// expected
					} else {
						fail("Unexpected thread result status " + localResult.getStatus());
					}
					
					RepositoryCache.exit();

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestFastRandomStartDelayRange());

		// THEN
		displayThen(TEST_NAME);
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
		final String TEST_NAME = "test209ParallelDelete";
		displayTestTitle(TEST_NAME);

		// GIVEN
		final Counter successCounter = new Counter();
		rememberDummyResourceWriteOperationCount(null);

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();
					
					RepositoryCache.enter();
					
					try {
						display("Thread "+Thread.currentThread().getName()+" START");
						provisioningService.deleteObject(ShadowType.class, accountMorganOid, null, null, localTask, localResult);
						localResult.computeStatus();
						display("Thread "+Thread.currentThread().getName()+" DONE, result", localResult);
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
						LOGGER.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
					} finally {
						RepositoryCache.exit();
					}

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestFastRandomStartDelayRange());

		// THEN
		displayThen(TEST_NAME);
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
		final String TEST_NAME = "test210ParallelCreateSlow";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		final Counter successCounter = new Counter();
		rememberDummyResourceWriteOperationCount(null);

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();
					
					RepositoryCache.enter();
					
					randomDelay(getConcurrentTestSlowRandomStartDelayRange());
					LOGGER.info("{} starting to do some work", Thread.currentThread().getName());

					ShadowType account = parseObjectType(ACCOUNT_ELIZABETH_FILE, ShadowType.class);

					try {
						accountElizabethOid = provisioningService.addObject(account.asPrismObject(), null, null, localTask, localResult);
						successCounter.click();
					} catch (ObjectAlreadyExistsException e) {
						// this is expected ... sometimes
						LOGGER.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
					} finally {
						RepositoryCache.exit();
					}

				}, getConcurrentTestNumberOfThreads(), null);

		// THEN
		displayThen(TEST_NAME);
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
	 * 
	 * There is a slight chance that one of the thread starts after the first operation
	 * is finished. But the threads are fast and the operations are slow. So this is
	 * a very slim chance.
	 */
	@Test
	public void test212ParallelModifyElizabethSlow() throws Exception {
		final String TEST_NAME = "test212ParallelModifyElizabethSlow";
		
		PrismObject<ShadowType> shadowAfter = parallelModifyTestSlow(TEST_NAME,
				() -> ObjectDelta.createModificationReplaceProperty(ShadowType.class,
						accountElizabethOid, dummyResourceCtl.getAttributeFullnamePath(), prismContext, "Miss Swan"));
		
		assertAttribute(shadowAfter, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Miss Swan");
	}
	
	/**
	 * Create a lot parallel modifications for the same property and the same value.
	 * These should all be eliminated - except for one of them.
	 * 
	 * There is a slight chance that one of the thread starts after the first operation
	 * is finished. But the threads are fast and the operations are slow. So this is
	 * a very slim chance.
	 */
	@Test
	public void test214ParallelModifyDisableSlow() throws Exception {
		final String TEST_NAME = "test214ParallelModifyDisableSlow";
		
		PrismObject<ShadowType> shadowAfter = parallelModifyTestSlow(TEST_NAME,
				() -> ObjectDelta.createModificationReplaceProperty(ShadowType.class,
						accountElizabethOid, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, prismContext, ActivationStatusType.DISABLED));

		assertActivationAdministrativeStatus(shadowAfter, ActivationStatusType.DISABLED);
	}
		
	private PrismObject<ShadowType> parallelModifyTestSlow(final String TEST_NAME, FailableProducer<ObjectDelta<ShadowType>> deltaProducer) throws Exception {
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		final Counter successCounter = new Counter();
		rememberDummyResourceWriteOperationCount(null);

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();

					RepositoryCache.enter();
					// Playing with cache, trying to make a worst case
					PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, accountElizabethOid, null, localResult);

					randomDelay(getConcurrentTestSlowRandomStartDelayRange());
					LOGGER.info("{} starting to do some work", Thread.currentThread().getName());
					
					ObjectDelta<ShadowType> delta = deltaProducer.run();
					display("ObjectDelta", delta);
					
					provisioningService.modifyObject(ShadowType.class, accountElizabethOid, delta.getModifications(), null, null, localTask, localResult);
					
					localResult.computeStatus();
					display("Thread "+Thread.currentThread().getName()+" DONE, result", localResult);
					if (localResult.isSuccess()) {
						successCounter.click();
					} else if (localResult.isInProgress()) {
						// expected
					} else {
						fail("Unexpected thread result status " + localResult.getStatus());
					}
					
					RepositoryCache.exit();

				}, getConcurrentTestNumberOfThreads(), null);

		// THEN
		displayThen(TEST_NAME);
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
		final String TEST_NAME = "test229ParallelDeleteSlow";
		displayTestTitle(TEST_NAME);

		// GIVEN
		final Counter successCounter = new Counter();
		rememberDummyResourceWriteOperationCount(null);

		// WHEN
		displayWhen(TEST_NAME);

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(i) -> {
					Task localTask = createTask(TEST_NAME + ".local");
					OperationResult localResult = localTask.getResult();
					
					RepositoryCache.enter();
					// Playing with cache, trying to make a worst case
					PrismObject<ShadowType> shadowBefore = repositoryService.getObject(ShadowType.class, accountElizabethOid, null, localResult);

					randomDelay(getConcurrentTestSlowRandomStartDelayRange());
					LOGGER.info("{} starting to do some work", Thread.currentThread().getName());
					
					try {
						display("Thread "+Thread.currentThread().getName()+" START");
						provisioningService.deleteObject(ShadowType.class, accountElizabethOid, null, null, localTask, localResult);
						localResult.computeStatus();
						display("Thread "+Thread.currentThread().getName()+" DONE, result", localResult);
						if (localResult.isSuccess()) {
							successCounter.click();
						} else if (localResult.isInProgress()) {
							// expected
						} else {
							fail("Unexpected thread result status " + localResult.getStatus());
						}
					} catch (ObjectNotFoundException e) {
						// this is expected ... sometimes
						LOGGER.info("Exception (maybe expected): {}: {}", e.getClass().getSimpleName(), e.getMessage());
					} finally {
						RepositoryCache.exit();
					}

				}, getConcurrentTestNumberOfThreads(), null);

		// THEN
		displayThen(TEST_NAME);
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
	 * Several threads reading from resource. Couple other threads try to get into the way
	 * by modifying the resource, hence forcing connector re-initialization.
	 * The goal is to detect connector initialization race conditions.
	 * 
	 * MID-5068
	 */
	@Test
	public void test800ParallelReadAndModifyResource() throws Exception {
		final String TEST_NAME = "test800ParallelReadAndModifyResource";
		displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		dummyResource.assertConnections(1);
		assertDummyConnectorInstances(1);
		
		dummyResource.setOperationDelayRange(0);
		
		PrismObject<ShadowType> accountBefore = parseObject(ACCOUNT_WALLY_FILE);
		display("Account before", accountBefore);
		accountWallyOid = provisioningService.addObject(accountBefore, null, null, task, result);
		PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, accountWallyOid, null, task, result);
		result.computeStatus();
		if (result.getStatus() != OperationResultStatus.SUCCESS) {
			display("Failed read result (precondition)", result);
			fail("Unexpected read status (precondition): "+result.getStatus());
		}
		
		// WHEN
		displayWhen(TEST_NAME);
		
		long t0 = System.currentTimeMillis();
		MutableBoolean readFinished = new MutableBoolean();

		ParallelTestThread[] threads = multithread(TEST_NAME,
				(threadIndex) -> {
				
					if (threadIndex <= 6) {
						
						for (int i = 0; /* neverending */ ; i++) {
							
							messResource(TEST_NAME, threadIndex, i);
							
							display("T +"+(System.currentTimeMillis() - t0));
							
							if (readFinished.booleanValue()) {
								break;
							}
							
						}	
						
					} else if (threadIndex == 7) {
						
						for (int i = 0; /* neverending */ ; i++) {
							
							Task localTask = createTask(TEST_NAME + ".test."+i);
							
							LOGGER.debug("PAR: TESTing "+threadIndex+"."+i);
							
							OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, localTask);
							
							display("PAR: TESTed "+threadIndex+"."+i+": "+testResult.getStatus());
							
							if (testResult.getStatus() != OperationResultStatus.SUCCESS) {
								display("Failed test resource result", testResult);
								readFinished.setValue(true);
								fail("Unexpected test resource result status: "+testResult.getStatus());
							}
							
							if (readFinished.booleanValue()) {
								break;
							}
							
						}	
						
					} else {

						try {
							for (int i = 0; i < MESS_RESOURCE_ITERATIONS; i++) {
								Task localTask = createTask(TEST_NAME + ".op."+i);
								OperationResult localResult = localTask.getResult();

								LOGGER.debug("PAR: OPing "+threadIndex+"."+i);
								
								Object out = doResourceOperation(threadIndex, i, localTask, localResult);
								
								localResult.computeStatus();
								display("PAR: OPed "+threadIndex+"."+i+": " + out.toString() + ": "+localResult.getStatus());
								
								if (localResult.getStatus() != OperationResultStatus.SUCCESS) {
									display("Failed read result", localResult);
									readFinished.setValue(true);
									fail("Unexpected read status: "+localResult.getStatus());
								}
								
								if (readFinished.booleanValue()) {
									break;
								}
							}
						} finally {
							readFinished.setValue(true);
						}
					}
					
					display("mischief managed ("+threadIndex+")");

				}, 15, getConcurrentTestFastRandomStartDelayRange());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, WAIT_TIMEOUT);
		
		PrismObject<ResourceType> resourceAfter = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
		display("resource after", resourceAfter);
		
		List<ConnectorOperationalStatus> stats = provisioningService.getConnectorOperationalStatus(RESOURCE_DUMMY_OID, task, result);
		display("Dummy connector stats after", stats);
		
		display("Dummy resource connections", dummyResource.getConnectionCount());
		
		assertDummyConnectorInstances(dummyResource.getConnectionCount());
	}

	private Object doResourceOperation(int threadIndex, int i, Task task, OperationResult result) throws Exception {
		int op = RND.nextInt(3);
		
		if (op == 0) {
			return provisioningService.getObject(ShadowType.class, accountWallyOid, null, task, result);
			
		} else if (op == 1) {
			ObjectQuery query = ObjectQueryUtil.createResourceAndKind(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, prismContext);
			List<PrismObject<ShadowType>> list = new ArrayList<>();
			ResultHandler<ShadowType> handler = (o,or) -> { list.add(o); return true; };
			provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, result);
			return list;
			
		} else if (op == 2) {
			ObjectQuery query = ObjectQueryUtil.createResourceAndKind(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, prismContext);
			return provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		}
		return null;
	}

	private void messResource(final String TEST_NAME, int threadIndex, int i) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
		Task task = createTask(TEST_NAME+".mess."+threadIndex+"."+i);
		OperationResult result = task.getResult();
		List<ItemDelta<?,?>> deltas = deltaFor(ResourceType.class)
			.item(ResourceType.F_DESCRIPTION).replace("Iter "+threadIndex+"."+i)
			.asItemDeltas();
		
		LOGGER.debug("PAR: MESSing "+threadIndex+"."+i);
		
		provisioningService.modifyObject(ResourceType.class, RESOURCE_DUMMY_OID, deltas, null, null, task, result);
		result.computeStatus();
		
		display("PAR: MESSed "+threadIndex+"."+i+": "+result.getStatus());
		
		if (result.getStatus() != OperationResultStatus.SUCCESS) {
			display("Failed mess resource result", result);
			fail("Unexpected mess resource result status: "+result.getStatus());
		}
	}
}
