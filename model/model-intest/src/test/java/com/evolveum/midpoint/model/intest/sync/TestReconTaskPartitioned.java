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
package com.evolveum.midpoint.model.intest.sync;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.io.FileNotFoundException;

/**
 * The same as TestReconTask but this one uses partitioned reconciliation task handler.
 * I.e. each reconciliation task is divided into three subtasks (for stage 1, 2, 3).
 *
 * Cannot be run under H2 because of too much contention.
 * Also, it takes a little longer than standard TestReconTask because of the overhead.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconTaskPartitioned extends TestReconTask {

	protected static final String TASK_RECONCILE_DUMMY_PARTITIONED_FILENAME = COMMON_DIR + "/task-reconcile-dummy-partitioned.xml";
	protected static final String TASK_RECONCILE_DUMMY_PARTITIONED_OID = "10000000-0000-0000-565P-565600000004";

	protected static final String TASK_RECONCILE_DUMMY_BLUE_PARTITIONED_FILENAME = COMMON_DIR + "/task-reconcile-dummy-blue-partitioned.xml";
	protected static final String TASK_RECONCILE_DUMMY_BLUE_PARTITIONED_OID = "10000000-0000-0000-565P-565600000204";

	protected static final String TASK_RECONCILE_DUMMY_GREEN_PARTITIONED_FILENAME = COMMON_DIR + "/task-reconcile-dummy-green-partitioned.xml";
	protected static final String TASK_RECONCILE_DUMMY_GREEN_PARTITIONED_OID = "10000000-0000-0000-565P-565600000404";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		taskManager.setFreeBucketWaitInterval(100L);
	}

	@SuppressWarnings("Duplicates")
	@Override
	protected void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException {
		if (resource == resourceDummyGreen) {
			importObjectFromFile(TASK_RECONCILE_DUMMY_GREEN_PARTITIONED_FILENAME);
		} else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
			importObjectFromFile(TASK_RECONCILE_DUMMY_BLUE_PARTITIONED_FILENAME);
		} else if (resource == getDummyResourceObject()) {
			importObjectFromFile(TASK_RECONCILE_DUMMY_PARTITIONED_FILENAME);
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

	@SuppressWarnings("Duplicates")
	@Override
	protected String getSyncTaskOid(PrismObject<ResourceType> resource) {
		if (resource == resourceDummyGreen) {
			return TASK_RECONCILE_DUMMY_GREEN_PARTITIONED_OID;
		} else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
			return TASK_RECONCILE_DUMMY_BLUE_PARTITIONED_OID;
		} else if (resource == getDummyResourceObject()) {
			return TASK_RECONCILE_DUMMY_PARTITIONED_OID;
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

	@Override
	protected OperationResult waitForSyncTaskNextRunAssertSuccess(PrismObject<ResourceType> resource) throws Exception {
		OperationResult result = waitForTaskTreeNextFinishedRun(getSyncTaskOid(resource), getWaitTimeout());
		TestUtil.assertSuccess(result);
		return result;
	}

	@Override
	protected OperationResult waitForSyncTaskNextRun(PrismObject<ResourceType> resource) throws Exception {
		return waitForTaskTreeNextFinishedRun(getSyncTaskOid(resource), getWaitTimeout());
	}

}
