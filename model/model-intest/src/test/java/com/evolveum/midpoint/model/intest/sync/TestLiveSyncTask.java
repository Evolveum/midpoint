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
package com.evolveum.midpoint.model.intest.sync;

import java.io.FileNotFoundException;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLiveSyncTask extends AbstractSynchronizationStoryTest {

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		dummyResourceGreen.setSyncStyle(DummySyncStyle.SMART);
		getDummyResource().setSyncStyle(DummySyncStyle.DUMB);
		getDummyResource(RESOURCE_DUMMY_BLUE_NAME).setSyncStyle(DummySyncStyle.SMART);
	}

	@Override
	protected String getExpectedChannel() {
		return SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI;
	}

	@Override
	protected void importSyncTask(PrismObject<ResourceType> resource) throws FileNotFoundException {
		if (resource == resourceDummyGreen) {
			importObjectFromFile(TASK_LIVE_SYNC_DUMMY_GREEN_FILENAME);
		} else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
			importObjectFromFile(TASK_LIVE_SYNC_DUMMY_BLUE_FILENAME);
		} else if (resource == getDummyResourceObject()) {
			importObjectFromFile(TASK_LIVE_SYNC_DUMMY_FILENAME);
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

	@Override
	protected String getSyncTaskOid(PrismObject<ResourceType> resource) {
		if (resource == resourceDummyGreen) {
			return TASK_LIVE_SYNC_DUMMY_GREEN_OID;
		} else if (resource == getDummyResourceObject(RESOURCE_DUMMY_BLUE_NAME)) {
			return TASK_LIVE_SYNC_DUMMY_BLUE_OID;
		} else if (resource == getDummyResourceObject()) {
			return TASK_LIVE_SYNC_DUMMY_OID;
		} else {
			throw new IllegalArgumentException("Unknown resource "+resource);
		}
	}

}
