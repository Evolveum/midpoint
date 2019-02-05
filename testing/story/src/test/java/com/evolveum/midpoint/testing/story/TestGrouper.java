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

package com.evolveum.midpoint.testing.story;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestGrouper extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "grouper");

	protected static final File RESOURCE_GROUPER_FILE = new File(TEST_DIR, "resource-grouper.xml");
	protected static final String RESOURCE_GROUPER_ID = "Grouper";
	protected static final String RESOURCE_GROUPER_OID = "bbb9900a-b53d-4453-b60b-908725e3950e";

	public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	public static final String ORG_TOP_OID = "8fe3acc3-c689-4f77-9512-3d06b5d00dc2";

	protected static DummyResource dummyResourceGrouper;
	protected static DummyResourceContoller dummyResourceCtlGrouper;
	protected ResourceType resourceDummyGrouperType;
	protected PrismObject<ResourceType> resourceDummyGrouper;

	@Override
	protected String getTopOrgOid() {
		return ORG_TOP_OID;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// Resources
		dummyResourceCtlGrouper = DummyResourceContoller.create(RESOURCE_GROUPER_ID, resourceDummyGrouper);
		dummyResourceGrouper = dummyResourceCtlGrouper.getDummyResource();
		resourceDummyGrouper = importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER_FILE, RESOURCE_GROUPER_OID, initTask, initResult);
		resourceDummyGrouperType = resourceDummyGrouper.asObjectable();
		dummyResourceCtlGrouper.setResource(resourceDummyGrouper);
		dummyResourceGrouper.setSyncStyle(DummySyncStyle.SMART);

		// Org
		importObjectFromFile(ORG_TOP_FILE, initResult);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestGrouper.class.getName() + "." + TEST_NAME);

        OperationResult testResultGrouper = modelService.testResource(RESOURCE_GROUPER_OID, task);
        TestUtil.assertSuccess(testResultGrouper);

        dumpOrgTree();
	}

}
