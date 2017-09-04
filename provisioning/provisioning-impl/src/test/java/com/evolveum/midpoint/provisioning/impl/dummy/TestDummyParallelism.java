/*
 * Copyright (c) 2010-2017 Evolveum
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

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.Counter;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The test of Provisioning service on the API level.
 *
 * This test is focused on parallelism and race conditions.
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

	private static final int DUMMY_OPERATION_DELAY_RANGE = 1000;

	private String accountMorganOid;

	protected int getConcurrentTestNumberOfThreads() {
		return 5;
	}

	protected int getConcurrentTestRandomStartDelayRange() {
		return 10;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		dummyResource.setOperationDelayRange(DUMMY_OPERATION_DELAY_RANGE);
//		InternalMonitor.setTraceConnectorOperation(true);
	}

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}

	// test000-test100 in the superclasses

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

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestRandomStartDelayRange());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, WAIT_TIMEOUT);

		successCounter.assertCount("Wrong number of successful operations", 1);

		PrismObject<ShadowType> shadowAfter = provisioningService.getObject(ShadowType.class, accountMorganOid, null, task, result);
		display("Shadow after", shadowAfter);

		assertDummyResourceWriteOperationCountIncrement(null, 1);

		assertSteadyResource();
	}

	@Test
	public void test229ParallelDelete() throws Exception {
		final String TEST_NAME = "test229ParallelDelete";
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

					try {
						display("Thread "+Thread.currentThread().getName()+" START");
						provisioningService.deleteObject(ShadowType.class, accountMorganOid, null, null, localTask, localResult);
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
					}

				}, getConcurrentTestNumberOfThreads(), getConcurrentTestRandomStartDelayRange());

		// THEN
		displayThen(TEST_NAME);
		waitForThreads(threads, WAIT_TIMEOUT);

		successCounter.assertCount("Wrong number of successful operations", 1);

		assertNoRepoObject(ShadowType.class, accountMorganOid);

		assertDummyResourceWriteOperationCountIncrement(null, 1);

		assertSteadyResource();
	}

}
