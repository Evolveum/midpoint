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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.icf.dummy.connector.DummyConnectorLegacyUpdate;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

/**
 * Almost the same as TestDummy but this is using connector with legacy update methods.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyLegacyUpdate extends TestDummy {

	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy-legacy-update.xml");

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}
	
	@Override
	protected File getResourceDummyFile() {
		return RESOURCE_DUMMY_FILE;
	}

	@Override
	protected String getDummyConnectorType() {
		return IntegrationTestTools.DUMMY_CONNECTOR_LEGACY_UPDATE_TYPE;
	}
	
	@Override
	protected Class<?> getDummyConnectorClass() {
		return  DummyConnectorLegacyUpdate.class;
	}

	@Override
	protected void assertUpdateCapability(UpdateCapabilityType capUpdate) {
		assertNotNull("native update capability not present", capUpdate);
		assertNull("native update capability is manual", capUpdate.isManual());
		assertNull("native update capability is delta", capUpdate.isDelta());
	}
	
}
