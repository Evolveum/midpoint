/*
 * Copyright (c) 2017-2018 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CountObjectsSimulateType;

/**
 * Testing expressions in dummy resource configuration.
 * Also, this resource has no paging support and no object count simulation.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyExpression extends TestDummy {

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-expression");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}

	@Override
	protected File getResourceDummyFile() {
		return RESOURCE_DUMMY_FILE;
	}

	@Override
	protected <T> void assertConfigurationProperty(PrismProperty<T> confProp) {
		T val = confProp.getRealValue();
		switch (confProp.getElementName().getLocalPart()) {
			case "instanceId":
				assertEquals("Wrong value for "+confProp, "", val);
				break;

			case "uselessString":
				assertEquals("Wrong value for "+confProp, "Shiver me timbers!", val);
				assertExpression(confProp, "value");
				break;

			default:
				break;
		}
	}
	
	@Override
	protected CountObjectsSimulateType getCountSimulationMode() {
		return null;
	}
	
	@Override
	protected Integer getTest115ExpectedCount() {
		return null;
	}
	
	// No paging means no support for server-side sorting
	// Note: ordering may change here if dummy resource impl is changed
	@Override
	protected String[] getSortedUsernames18x() {
		// daemon, Will, morgan, carla, meathook 
		return new String[] { "daemon", transformNameFromResource("Will"), transformNameFromResource("morgan"), "carla", "meathook" };
	}
	
	// No paging
	@Override
	protected Integer getTest18xApproxNumberOfSearchResults() {
		return null;
	}
	
	@Test
	@Override
	public void test181SearchNullPagingOffset0Size3Desc() throws Exception {
		// Nothing to do. No sorting support. So desc sorting won't work at all.
	}
	
	@Test
	@Override
	public void test183SearchNullPagingOffset2Size3Desc() throws Exception {
		// Nothing to do. No sorting support. So desc sorting won't work at all.
	}

}
