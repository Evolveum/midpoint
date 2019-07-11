/*
 * Copyright (c) 2018-2019 Evolveum
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
package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Test for resources with configured capabilities (MID-5400)
 * 
 * @author Gustav Palos
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConfiguredCapabilitiesActivation extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "configured-capabilities-activation");
	
	protected static final File RESOURCE_DUMMY_ACTIVATION_FILE = new File(TEST_DIR, "resource-dummy-activation.xml");
	protected static final String RESOURCE_DUMMY_ACTIVATION_OID = "4bac305c-ed1f-4919-9670-e11863156811";
	protected static final String RESOURCE_DUMMY_ACTIVATION_NAME = "activation";
	
	protected static final File SHADOW_FILE = new File(TEST_DIR, "shadow-sample.xml");
	protected static final String SHADOW_OID = "6925f5a7-2acb-409b-b2e1-94a6534a9745";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(RESOURCE_DUMMY_ACTIVATION_NAME, RESOURCE_DUMMY_ACTIVATION_FILE, RESOURCE_DUMMY_ACTIVATION_OID, initTask, initResult);
		addObject(SHADOW_FILE);
	}
	
	@Test
	public void test000ImportAccount() throws Exception {
		final String TEST_NAME = "test000ImportAccount";
		displayTestTitle(TEST_NAME);
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
        displayWhen(TEST_NAME);

		modelService.importFromResource(SHADOW_OID, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
/*		List<Object> configuredCapabilities = resourceNoAAD.asObjectable().getCapabilities().getConfigured().getAny();
		UpdateCapabilityType capUpdate = CapabilityUtil.getCapability(configuredCapabilities,
				UpdateCapabilityType.class);
		assertNotNull("No configured UpdateCapabilityType", capUpdate);
		assertTrue("Configured addRemoveAttributeValues is not disabled", Boolean.FALSE.equals(capUpdate.isAddRemoveAttributeValues()));
*/	
	}
	
}
