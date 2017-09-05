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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertTestResourceFailure;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertTestResourceSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;

import javax.xml.bind.JAXBException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.rbac.TestRbac;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Test if a resource without a schema can pass basic operations such as getObject and testResource.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSchemalessResource extends AbstractInitializedModelIntegrationTest {

	private static String accountOid;

	public TestSchemalessResource() throws JAXBException {
		super();
	}

	/**
	 * Just test if this does not die on an exception.
	 */
	@Test
    public void test001GetObject() throws Exception {
		final String TEST_NAME = "test001GetObject";
        TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_SCHEMALESS_OID, null, task, result);

        // THEN
        assertNotNull("Null resource returned", resource);
	}

	@Test
    public void test002TestConnection() throws Exception {
		final String TEST_NAME = "test002TestConnection";
		TestUtil.displayTestTitle(this, TEST_NAME);

        Task task = createTask(TEST_NAME);

        // WHEN
        OperationResult testResult = modelService.testResource(RESOURCE_DUMMY_SCHEMALESS_OID, task);

        // THEN
		display("Test result", testResult);
		OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONFIGURATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertSuccess(connectorResult);
		assertTestResourceFailure(testResult, ConnectorTestOperation.RESOURCE_SCHEMA);
		assertFailure(testResult);

	}

}
