/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 *  MID-5353
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLiveSyncTaskInterruption extends AbstractInitializedModelIntegrationTest {

	private static final File TEST_DIR = new File("src/test/resources/sync");

	private static final File RESOURCE_DUMMY_SLOW_SYNC_FILE = new File(TEST_DIR, "resource-dummy-slow-sync.xml");
	private static final String RESOURCE_DUMMY_SLOW_SYNC_OID = "7a58233a-1cfb-46d1-a404-08cdf4626ebb";
	private static final String RESOURCE_DUMMY_SLOW_SYNC_NAME = "slowSync";

	private DummyResourceContoller slowSyncController;

	private static final File TASK_SLOW_SYNC_FILE = new File(TEST_DIR, "task-slow-sync.xml");
	private static final String TASK_SLOW_SYNC_OID = "ca51f209-1ef5-42b3-84e7-5f639ee8e300";

	private static final File TASK_SLOW_SYNC_2_FILE = new File(TEST_DIR, "task-slow-sync-2.xml");
	private static final String TASK_SLOW_SYNC_2_OID = "c37dda96-e547-41c2-b343-b890bc7fade9";

	private static final File TASK_SLOW_SYNC_BATCHED_FILE = new File(TEST_DIR, "task-slow-sync-batched.xml");
	private static final String TASK_SLOW_SYNC_BATCHED_OID = "ef22bf7b-5d28-4a57-b3a5-6fa58491eeb3";

	public static long delay = 1;           // referenced from resource-dummy-slow-sync.xml
	private static final int USERS = 1000;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		slowSyncController = initDummyResource(RESOURCE_DUMMY_SLOW_SYNC_NAME,
				RESOURCE_DUMMY_SLOW_SYNC_FILE, RESOURCE_DUMMY_SLOW_SYNC_OID, initTask, initResult);
		slowSyncController.setSyncStyle(DummySyncStyle.DUMB);

		// Initial run of these tasks must come before accounts are created.

		importObjectFromFile(TASK_SLOW_SYNC_FILE);
		waitForTaskFinish(TASK_SLOW_SYNC_OID, false);

		importObjectFromFile(TASK_SLOW_SYNC_2_FILE);
		waitForTaskFinish(TASK_SLOW_SYNC_2_OID, false);

		importObjectFromFile(TASK_SLOW_SYNC_BATCHED_FILE);
		waitForTaskFinish(TASK_SLOW_SYNC_BATCHED_OID, false);

		assertUsers(getNumberOfUsers());
		for (int i = 0; i < USERS; i++) {
			slowSyncController.addAccount(String.format("user-%06d", i));
		}
	}

	@Test
	public void test100SuspendWhileIcfSync() throws Exception {
		final String TEST_NAME = "test100SuspendWhileIcfSync";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// Resource gives out changes slowly now.
		slowSyncController.getDummyResource().setOperationDelayOffset(500);

		// WHEN
		displayWhen(TEST_NAME);

		waitForTaskNextStart(TASK_SLOW_SYNC_OID, false, 2000, true);  // starts the task
		boolean suspended = suspendTask(TASK_SLOW_SYNC_OID, 5000);

		// THEN
		displayThen(TEST_NAME);

		assertTrue("Task was not suspended", suspended);
		Task taskAfter = taskManager.getTask(TASK_SLOW_SYNC_OID, result);
		display("Task after", taskAfter);
		assertEquals("Wrong token value", (Integer) 0, taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN));
	}

	@Test
	public void test110SuspendWhileProcessing() throws Exception {
		final String TEST_NAME = "test110SuspendWhileProcessing";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// Resource gives out changes quickly. But they are processed slowly.
		slowSyncController.getDummyResource().setOperationDelayOffset(0);
		delay = 100;

		// WHEN
		displayWhen(TEST_NAME);

		waitForTaskNextStart(TASK_SLOW_SYNC_2_OID, false, 2000, true);  // starts the task
		Thread.sleep(4000);
		boolean suspended = suspendTask(TASK_SLOW_SYNC_2_OID, 5000);

		// THEN
		displayThen(TEST_NAME);

		assertTrue("Task was not suspended", suspended);
		Task taskAfter = taskManager.getTask(TASK_SLOW_SYNC_2_OID, result);
		display("Task after", taskAfter);
		Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
		assertTrue("Token value is zero (should be greater)", token != null && token > 0);

		ObjectQuery query = prismContext.queryFor(UserType.class)
				.item(UserType.F_NAME).startsWith("user-")
				.build();

		assertObjects(UserType.class, query, token);
	}

	@Test
	public void test120Batched() throws Exception {
		final String TEST_NAME = "test120Batched";
		displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// Changes are provided and processed normally. But we will take only first 10 of them.
		slowSyncController.getDummyResource().setOperationDelayOffset(0);
		delay = 0;

		// WHEN
		displayWhen(TEST_NAME);

		waitForTaskNextRun(TASK_SLOW_SYNC_BATCHED_OID, false, 10000, true);

		// THEN
		displayThen(TEST_NAME);

		Task taskAfter = taskManager.getTask(TASK_SLOW_SYNC_BATCHED_OID, result);
		display("Task after", taskAfter);
		Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
		assertEquals("Wrong token value", (Integer) 10, token);

		ObjectQuery query = prismContext.queryFor(UserType.class)
				.item(UserType.F_NAME).startsWith("user-")
				.build();

		assertObjects(UserType.class, query, 10);

		// WHEN
		displayWhen(TEST_NAME);

		waitForTaskNextRun(TASK_SLOW_SYNC_BATCHED_OID, false, 10000, true);

		// THEN
		displayThen(TEST_NAME);

		taskAfter = taskManager.getTask(TASK_SLOW_SYNC_BATCHED_OID, result);
		display("Task after", taskAfter);
		token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
		assertEquals("Wrong token value", (Integer) 20, token);

		assertObjects(UserType.class, query, 20);
	}
}
