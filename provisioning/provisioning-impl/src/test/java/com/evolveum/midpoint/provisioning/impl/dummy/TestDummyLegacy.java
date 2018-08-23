/*
 * Copyright (c) 2015-2018 Evolveum
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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

/**
 * Test with legacy "ICF" schema.
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyLegacy extends AbstractIntegrationTest {

	private static final File TEST_DIR = new File(AbstractDummyTest.TEST_DIR_DUMMY, "dummy-legacy");

	private static final File RESOURCE_DUMMY_NATIVE_FILE = new File(TEST_DIR, "resource-dummy-native.xml");
	private static final String RESOURCE_DUMMY_NATIVE_OID = "17e6e88c-4be6-11e5-8abd-001e8c717e5b";
	private static final String RESOURCE_DUMMY_NATIVE_INSTANCE_ID = "native";

	private static final File RESOURCE_DUMMY_LEGACY_FILE = new File(TEST_DIR, "resource-dummy-legacy.xml");
	private static final String RESOURCE_DUMMY_LEGACY_OID = "387a3400-4be6-11e5-b41a-001e8c717e5b";
	private static final String RESOURCE_DUMMY_LEGACY_INSTANCE_ID = "legacy";

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyLegacy.class);

	private static final String OBJECTCLASS_NATIVE_ACCOUNT = "account";
	private static final String OBJECTCLASS_NATIVE_GROUP = "group";
	private static final String OBJECTCLASS_NATIVE_PRIVILEGE = "privilege";

	private static final String OBJECTCLASS_LEGACY_ACCOUNT = "CustomaccountObjectClass";
	private static final String OBJECTCLASS_LEGACY_GROUP = "CustomgroupObjectClass";
	private static final String OBJECTCLASS_LEGACY_PRIVILEGE = "CustomprivilegeObjectClass";

	private PrismObject<ResourceType> resourceNative;
	private ResourceType resourceTypeNative;
	private static DummyResource dummyResourceNative;
	private DummyResourceContoller dummyResourceNativeCtl;

	private PrismObject<ResourceType> resourceLegacy;
	private ResourceType resourceTypeLegacy;
	private static DummyResource dummyResourceLegacy;
	private DummyResourceContoller dummyResourceLegacyCtl;

	@Autowired(required = true)
	private ProvisioningService provisioningService;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);

		InternalsConfig.encryptionChecks = false;

		resourceNative = addResourceFromFile(RESOURCE_DUMMY_NATIVE_FILE, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceTypeNative = resourceNative.asObjectable();
		dummyResourceNativeCtl = DummyResourceContoller.create(RESOURCE_DUMMY_NATIVE_INSTANCE_ID);
		dummyResourceNativeCtl.setResource(resourceNative);
		dummyResourceNative = dummyResourceNativeCtl.getDummyResource();

		resourceLegacy = addResourceFromFile(RESOURCE_DUMMY_LEGACY_FILE, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceTypeLegacy = resourceLegacy.asObjectable();
		dummyResourceLegacyCtl = DummyResourceContoller.create(RESOURCE_DUMMY_LEGACY_INSTANCE_ID);
		dummyResourceLegacyCtl.setResource(resourceLegacy);
		dummyResourceLegacy = dummyResourceLegacyCtl.getDummyResource();
}

	@Test
	public void test100NativeIntegrity() throws Exception {
		final String TEST_NAME = "test100NativeIntegrity";
		TestUtil.displayTestTitle(TEST_NAME);

		display("Dummy resource instance", dummyResourceNative.toString());

		assertNotNull("Resource is null", resourceNative);
		assertNotNull("ResourceType is null", resourceTypeNative);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_NATIVE_OID, null, result)
				.asObjectable();
	}

	@Test
	public void test103NativeTestResource() throws Exception {
		final String TEST_NAME = "test103NativeTestResource";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_NATIVE_OID, null, result)
				.asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class,
				resourceBefore.getConnectorRef().getOid(), null, result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_NATIVE_OID, task);

		// THEN
		display("Test result", testResult);
		OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONFIGURATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CAPABILITIES);
		assertSuccess(connectorResult);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.RESOURCE_SCHEMA);
		assertSuccess(testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_NATIVE_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
	}

	@Test
	public void test105NativeParsedSchema() throws Exception {
		final String TEST_NAME = "test105NativeParsedSchema";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyLegacy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		resourceNative = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_NATIVE_OID, null, task, result);

		// THEN
		result.computeStatus();
		assertSuccess(result);
		resourceTypeNative = resourceNative.asObjectable();

		ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceTypeNative, prismContext);
		display("Parsed resource schema", returnedSchema);
		assertNotNull("No parsed schema", returnedSchema);

		assertObjectClass(returnedSchema, OBJECTCLASS_NATIVE_ACCOUNT);
		assertObjectClass(returnedSchema, OBJECTCLASS_NATIVE_GROUP);
		assertObjectClass(returnedSchema, OBJECTCLASS_NATIVE_PRIVILEGE);
		assertNoObjectClass(returnedSchema, OBJECTCLASS_LEGACY_ACCOUNT);
		assertNoObjectClass(returnedSchema, OBJECTCLASS_LEGACY_GROUP);
		assertNoObjectClass(returnedSchema, OBJECTCLASS_LEGACY_PRIVILEGE);
	}

	@Test
	public void test200LegacyIntegrity() throws Exception {
		final String TEST_NAME = "test200LegacyIntegrity";
		TestUtil.displayTestTitle(TEST_NAME);

		display("Dummy resource instance", dummyResourceLegacy.toString());

		assertNotNull("Resource is null", resourceLegacy);
		assertNotNull("ResourceType is null", resourceTypeLegacy);

		Task task = taskManager.createTaskInstance(TestDummyLegacy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_LEGACY_OID, null, result)
				.asObjectable();
	}

	@Test
	public void test203LegacyTestResource() throws Exception {
		final String TEST_NAME = "test203LegacyTestResource";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_LEGACY_OID, null, result)
				.asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class,
				resourceBefore.getConnectorRef().getOid(), null, result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_LEGACY_OID, task);

		// THEN
		display("Test result", testResult);
		OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONFIGURATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CAPABILITIES);
		assertSuccess(connectorResult);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.RESOURCE_SCHEMA);
		assertSuccess(testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_LEGACY_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
	}

	@Test
	public void test205LegacyParsedSchema() throws Exception {
		final String TEST_NAME = "test205LegacyParsedSchema";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummyLegacy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		resourceLegacy = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_LEGACY_OID, null, task, result);

		// THEN
		result.computeStatus();
		assertSuccess(result);
		resourceTypeLegacy = resourceLegacy.asObjectable();

		ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceTypeLegacy, prismContext);
		display("Parsed resource schema", returnedSchema);
		assertNotNull("No parsed schema", returnedSchema);

		assertObjectClass(returnedSchema, OBJECTCLASS_LEGACY_ACCOUNT);
		assertObjectClass(returnedSchema, OBJECTCLASS_LEGACY_GROUP);
		assertObjectClass(returnedSchema, OBJECTCLASS_LEGACY_PRIVILEGE);
		assertNoObjectClass(returnedSchema, OBJECTCLASS_NATIVE_ACCOUNT);
		assertNoObjectClass(returnedSchema, OBJECTCLASS_NATIVE_GROUP);
		assertNoObjectClass(returnedSchema, OBJECTCLASS_NATIVE_PRIVILEGE);
	}

	private void assertObjectClass(ResourceSchema schema, String objectClassLocalName) {
		ObjectClassComplexTypeDefinition ocDef = schema.findObjectClassDefinition(objectClassLocalName);
		assertNotNull("No objectclass "+objectClassLocalName+" found in schema", ocDef);
	}

	private void assertNoObjectClass(ResourceSchema schema, String objectClassLocalName) {
		ObjectClassComplexTypeDefinition ocDef = schema.findObjectClassDefinition(objectClassLocalName);
		assertNull("Objectclass "+objectClassLocalName+" found in schema while not expecting it", ocDef);
	}

}
