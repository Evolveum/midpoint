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

/**
 *
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.TestConnectionCapabilityType;

/**
 * The test of Provisioning service on the API level. The test is using dummy
 * resource WITHOUT A SCHEMA. It checks if the system is still able to basically operate.
 * Even though the resource will not be usable until the schema is specified manually.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummySchemaless extends AbstractProvisioningIntegrationTest {

	private static final File TEST_DIR = new File(AbstractDummyTest.TEST_DIR_DUMMY, "dummy-schemaless");

	private static final File RESOURCE_DUMMY_NO_SCHEMA_FILE = new File(TEST_DIR, "resource-dummy-schemaless-no-schema.xml");
	private static final String RESOURCE_DUMMY_NO_SCHEMA_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0000";
	private static final String RESOURCE_DUMMY_NO_SCHEMA_INSTANCE_ID = "schemaless";

	private static final File RESOURCE_DUMMY_STATIC_SCHEMA_FILE = new File(TEST_DIR, "resource-dummy-schemaless-static-schema.xml");
	private static final String RESOURCE_DUMMY_STATIC_SCHEMA_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0505";
	private static final String RESOURCE_DUMMY_STATIC_SCHEMA_INSTANCE_ID = "staticSchema";

	private static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
	private static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
	private static final String ACCOUNT_WILL_ICF_UID = "will";

	private static final Trace LOGGER = TraceManager.getTrace(TestDummySchemaless.class);

	private PrismObject<ResourceType> resourceSchemaless;
	private ResourceType resourceTypeSchemaless;
	private static DummyResource dummyResourceSchemaless;

	private PrismObject<ResourceType> resourceStaticSchema;
	private static DummyResource dummyResourceStaticSchema;
	private static DummyResourceContoller dummyResourceSchemalessCtl;

	@Autowired(required = true)
	private ProvisioningService provisioningService;

	public TestDummySchemaless() {
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);

		InternalsConfig.encryptionChecks = false;

		resourceSchemaless = addResourceFromFile(RESOURCE_DUMMY_NO_SCHEMA_FILE, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceTypeSchemaless = resourceSchemaless.asObjectable();

		dummyResourceSchemalessCtl = DummyResourceContoller.create(RESOURCE_DUMMY_NO_SCHEMA_INSTANCE_ID);
		dummyResourceSchemalessCtl.setResource(resourceSchemaless);
		dummyResourceSchemaless = dummyResourceSchemalessCtl.getDummyResource();

		resourceStaticSchema = addResourceFromFile(RESOURCE_DUMMY_STATIC_SCHEMA_FILE, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);

		dummyResourceStaticSchema = DummyResource.getInstance(RESOURCE_DUMMY_STATIC_SCHEMA_INSTANCE_ID);
		dummyResourceStaticSchema.reset();
		dummyResourceStaticSchema.populateWithDefaultSchema();

	}
	
	@Override
	protected PrismObject<ResourceType> getResource() {
		return resourceStaticSchema;
	}

	@Test
	public void test000Integrity() throws Exception {
		final String TEST_NAME = "test000Integrity";
		TestUtil.displayTestTitle(TEST_NAME);

		display("Dummy resource instance", dummyResourceSchemaless.toString());

		assertNotNull("Resource is null", resourceSchemaless);
		assertNotNull("ResourceType is null", resourceTypeSchemaless);

		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_SCHEMA_OID, null, result)
				.asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();

		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(connector);
		display("Dummy Connector", connector);

		// Check connector schema
		IntegrationTestTools.assertConnectorSchemaSanity(connector, prismContext);
	}


	/**
	 * This should be the very first test that works with the resource.
	 *
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test003ConnectionSchemaless() throws Exception {
		final String TEST_NAME = "test003ConnectionSchemaless";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_SCHEMA_OID, null, result)
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
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_NO_SCHEMA_OID, task);

		// THEN
		display("Test result", testResult);
		OperationResult connectorResult = assertSingleConnectorTestResult(testResult);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONFIGURATION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertTestResourceSuccess(connectorResult, ConnectorTestOperation.CONNECTOR_CAPABILITIES);
		assertSuccess(connectorResult);
		assertTestResourceFailure(testResult, ConnectorTestOperation.RESOURCE_SCHEMA);
		assertFailure(testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_NO_SCHEMA_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
	}

	/**
	 * This basically checks if the methods do not die on NPE
	 */
	@Test
	public void test005ParsedSchemaSchemaless() throws Exception {
		final String TEST_NAME = "test005ParsedSchemaSchemaless";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName() + "." + TEST_NAME);

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchemaImpl.hasParsedSchema(resourceTypeSchemaless));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceTypeSchemaless, prismContext);

		display("Parsed resource schema", returnedSchema);

		assertNull("Unexpected schema after parsing", returnedSchema);
	}

	@Test
	public void test006GetObjectSchemaless() throws Exception {
		final String TEST_NAME = "test006GetObjectSchemaless";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName() + "." + TEST_NAME);

		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_SCHEMA_OID, null, null, result);
		assertNotNull("Resource is null", resource);
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("No connector ref", resourceType.getConnectorRef());
		assertNotNull("No connector ref OID", resourceType.getConnectorRef().getOid());
	}
	
	@Test
	public void test020ResourceStaticSchemaTest() throws Exception {
		resourceStaticSchemaTest("test020ResourceStaticSchemaTest");
	}
	
	public void resourceStaticSchemaTest(final String TEST_NAME) throws Exception {
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// Some connector initialization and other things might happen in previous tests.
		// The monitor is static, not part of spring context, it will not be cleared

		rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
		rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
		rememberResourceCacheStats();

		// Check that there is no schema before test (pre-condition)
		PrismObject<ResourceType> resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, result);
		ResourceType resourceTypeBefore = resourceBefore.asObjectable();
		rememberResourceVersion(resourceBefore.getVersion());
		assertNotNull("No connector ref", resourceTypeBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceTypeBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class,
				resourceTypeBefore.getConnectorRef().getOid(), null, result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceTypeBefore.getSchema();
		assertNotNull("No schema in static resource before", xmlSchemaTypeBefore);
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		assertNotNull("No schema XSD element in static resource before", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_STATIC_SCHEMA_OID, task);

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
				RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);

		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

		IntegrationTestTools.displayXml("Resource XML", resourceRepoAfter);

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchemaImpl.parse(xsdElement, resourceTypeBefore.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		// schema will be checked in next test

		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 1);
		assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 1);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 1);
		
		// Not sure why 2 instead 1, but this is a small difference, we can live with that
		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 2);
		
		// One increment for availablity status, the other for schema
		assertResourceVersionIncrement(resourceRepoAfter, 2);

	}

	/**
	 * MID-4472, MID-4174
	 */
	@Test
	public void test030ResourceStatciSchemaResourceAndConnectorCaching() throws Exception {
		resourceStatciSchemaResourceAndConnectorCaching("test030ResourceStatciSchemaResourceAndConnectorCaching");
	}
	
	public void resourceStatciSchemaResourceAndConnectorCaching(final String TEST_NAME) throws Exception {
		displayTestTitle(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDj.class.getName() + "." + TEST_NAME);
		
		// re-read the resource before tests so we have a clean slate, e.g. configuration properly parsed (no raw elements)
		resourceStaticSchema = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, null, result);
		ConnectorInstance currentConnectorInstance = resourceManager.getConfiguredConnectorInstance(
				resourceStaticSchema, ReadCapabilityType.class, false, result);
		
		IntegrationTestTools.displayXml("Initialized static schema resource", resourceStaticSchema);
		
		rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
		rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
		rememberSchemaMetadata(resourceStaticSchema);
		rememberConnectorInstance(currentConnectorInstance);
		rememberResourceCacheStats();

		ConnectorInstance configuredConnectorInstance = resourceManager.getConfiguredConnectorInstance(
				resourceStaticSchema, ReadCapabilityType.class, false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchemaBefore = RefinedResourceSchemaImpl.getResourceSchema(resourceStaticSchema, prismContext);
		assertNotNull("No resource schema", resourceSchemaBefore);
		assertStaticSchemaSanity(resourceSchemaBefore);
		
		// WHEN
		displayWhen(TEST_NAME);
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID,
				null, null, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref", resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID", resourceTypeAgain.getConnectorRef().getOid());

		PrismContainer<Containerable> configurationContainer = resourceStaticSchema.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain
				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));

		// Check resource schema caching
		ResourceSchema resourceSchemaAgain = RefinedResourceSchemaImpl.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertTrue("Resource schema was not cached", resourceSchemaBefore == resourceSchemaAgain);

		// Check capabilities caching

		CapabilitiesType capabilitiesType = resourceStaticSchema.asObjectable().getCapabilities();
		assertNotNull("No capabilities fetched from provisioning", capabilitiesType);
		CachingMetadataType capCachingMetadataType = capabilitiesType.getCachingMetadata();
		assertNotNull("No capabilities caching metadata fetched from provisioning", capCachingMetadataType);
		CachingMetadataType capCachingMetadataTypeAgain = resourceTypeAgain.getCapabilities().getCachingMetadata();
		assertEquals("Capabilities caching metadata serial number has changed", capCachingMetadataType.getSerialNumber(),
				capCachingMetadataTypeAgain.getSerialNumber());
		assertEquals("Capabilities caching metadata timestamp has changed", capCachingMetadataType.getRetrievalTimestamp(),
				capCachingMetadataTypeAgain.getRetrievalTimestamp());

		// Rough test if everything is fine
		resourceStaticSchema.asObjectable().setFetchResult(null);
		resourceAgain.asObjectable().setFetchResult(null);
		ObjectDelta<ResourceType> dummyResourceDiff = DiffUtil.diff(resourceStaticSchema, resourceAgain);
        display("Dummy resource diff", dummyResourceDiff);
        assertTrue("The resource read again is not the same as the original. diff:"+dummyResourceDiff, dummyResourceDiff.isEmpty());

		// Now we stick our nose deep inside the provisioning impl. But we need
		// to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = resourceManager.getConfiguredConnectorInstance(
				resourceAgain, ReadCapabilityType.class, false, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAgain);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);

		// Check if the connector still works.
		OperationResult testResult = new OperationResult(TestOpenDj.class.getName()
				+ ".test010ResourceAndConnectorCaching.test");
		configuredConnectorInstanceAgain.test(testResult);
		testResult.computeStatus();
		TestUtil.assertSuccess("Connector test failed", testResult);

		// Test connection should also refresh the connector by itself. So check if it has been refreshed

		ConnectorInstance configuredConnectorInstanceAfterTest = resourceManager.getConfiguredConnectorInstance(
				resourceAgain, ReadCapabilityType.class, false, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstanceAfterTest);
		assertTrue("Connector instance was not cached", configuredConnectorInstanceAgain == configuredConnectorInstanceAfterTest);

		assertSteadyResource();
	}
	
	@Test
	public void test040ReAddResourceStaticSchema() throws Exception {
		final String TEST_NAME = "test040ReAddResourceStaticSchema";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		PrismObject<ResourceType> resourceNew = prismContext.parseObject(RESOURCE_DUMMY_STATIC_SCHEMA_FILE);
		fillInConnectorRef(resourceNew, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, result);
		
		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.addObject(resourceNew, null, ProvisioningOperationOptions.createOverwrite(true), task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
		
		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, result);
		
		// One increment for availablity status, the other for schema
		assertResourceVersionIncrement(resourceRepoAfter, 1);
	}
	
	@Test
	public void test042ResourceStaticSchemaTestAgain() throws Exception {
		resourceStaticSchemaTest("test042ResourceStaticSchemaTestAgain");
	}
	
	/**
	 * MID-4472, MID-4174
	 */
	@Test
	public void test044ResourceStatciSchemaResourceAndConnectorCachingAgain() throws Exception {
		resourceStatciSchemaResourceAndConnectorCaching("test044ResourceStatciSchemaResourceAndConnectorCachingAgain");
	}

	
	/**
	 * This should be the very first test that works with the resource.
	 *
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test103ConnectionStaticSchema() throws Exception {
		final String TEST_NAME = "test103ConnectionStaticSchema";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		// Check that there a schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, null,
				result)
				.asObjectable();
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNotNull("No schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_STATIC_SCHEMA_OID, task);

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
				RESOURCE_DUMMY_NO_SCHEMA_OID, null, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);

		// TODO
	}

	/**
	 * This basically checks if the methods do not die on NPE
	 */
	@Test
	public void test105ParsedSchemaStaticSchema() throws Exception {
		final String TEST_NAME = "test105ParsedSchemaStaticSchema";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName() + "." + TEST_NAME);

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchemaImpl.hasParsedSchema(resourceStaticSchema.asObjectable()));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceStaticSchema.asObjectable(), prismContext);

		display("Parsed resource schema", returnedSchema);
		assertNotNull("Null resource schema", returnedSchema);

		assertStaticSchemaSanity(returnedSchema);
	}

	@Test
	public void test106GetObjectStaticSchema() throws Exception {
		final String TEST_NAME = "test106GetObjectStaticSchema";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName() + "." + TEST_NAME);

		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, null, result);
		assertNotNull("Resource is null", resource);
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("No connector ref", resourceType.getConnectorRef());
		assertNotNull("No connector ref OID", resourceType.getConnectorRef().getOid());

		ResourceSchema returnedSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);

		display("Parsed resource schema", returnedSchema);
		assertNotNull("Null resource schema", returnedSchema);

		assertStaticSchemaSanity(returnedSchema);
	}
	
	private void assertStaticSchemaSanity(ResourceSchema resorceSchema) {
		ResourceType resourceType = resourceStaticSchema.asObjectable();
		assertNotNull("No resource schema in "+resourceType, resorceSchema);
		QName objectClassQname = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
		ObjectClassComplexTypeDefinition accountDefinition = resorceSchema.findObjectClassDefinition(objectClassQname);
		assertNotNull("No object class definition for "+objectClassQname+" in resource schema", accountDefinition);
		ObjectClassComplexTypeDefinition accountDef1 = resorceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertTrue("Mismatched account definition: "+accountDefinition+" <-> "+accountDef1, accountDefinition == accountDef1);
		
		assertNotNull("No object class definition " + objectClassQname, accountDefinition);
		assertEquals("Object class " + objectClassQname + " is not account", ShadowKindType.ACCOUNT, accountDefinition.getKind());
		assertTrue("Object class " + objectClassQname + " is not default account", accountDefinition.isDefaultInAKind());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isEmpty());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isIgnored());
		
		Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getPrimaryIdentifiers();
		assertNotNull("Null identifiers for " + objectClassQname, identifiers);
		assertFalse("Empty identifiers for " + objectClassQname, identifiers.isEmpty());
		
		
		ResourceAttributeDefinition uidAttributeDefinition = accountDefinition.findAttributeDefinition(SchemaTestConstants.ICFS_UID);
		assertNotNull("No definition for attribute "+SchemaTestConstants.ICFS_UID, uidAttributeDefinition);
		assertTrue("Attribute "+SchemaTestConstants.ICFS_UID+" in not an identifier",uidAttributeDefinition.isIdentifier(accountDefinition));
		assertTrue("Attribute "+SchemaTestConstants.ICFS_UID+" in not in identifiers list",identifiers.contains(uidAttributeDefinition));
		assertEquals("Wrong displayName for attribute "+SchemaTestConstants.ICFS_UID, "Modified ConnId UID", uidAttributeDefinition.getDisplayName());
		assertEquals("Wrong displayOrder for attribute "+SchemaTestConstants.ICFS_UID, (Integer)100, uidAttributeDefinition.getDisplayOrder());
		
		Collection<? extends ResourceAttributeDefinition> secondaryIdentifiers = accountDefinition.getSecondaryIdentifiers();
		assertNotNull("Null secondary identifiers for " + objectClassQname, secondaryIdentifiers);
		assertFalse("Empty secondary identifiers for " + objectClassQname, secondaryIdentifiers.isEmpty());
		
		ResourceAttributeDefinition nameAttributeDefinition = accountDefinition.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		assertNotNull("No definition for attribute "+SchemaTestConstants.ICFS_NAME, nameAttributeDefinition);
		assertTrue("Attribute "+SchemaTestConstants.ICFS_NAME+" in not an identifier",nameAttributeDefinition.isSecondaryIdentifier(accountDefinition));
		assertTrue("Attribute "+SchemaTestConstants.ICFS_NAME+" in not in identifiers list",secondaryIdentifiers.contains(nameAttributeDefinition));
		assertEquals("Wrong displayName for attribute "+SchemaTestConstants.ICFS_NAME, "Modified ConnId Name", nameAttributeDefinition.getDisplayName());
		assertEquals("Wrong displayOrder for attribute "+SchemaTestConstants.ICFS_NAME, (Integer)110, nameAttributeDefinition.getDisplayOrder());
		
		assertNotNull("Null identifiers in account", accountDef1.getPrimaryIdentifiers());
		assertFalse("Empty identifiers in account", accountDef1.getPrimaryIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef1.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef1.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef1.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef1.getNativeObjectClass()));
		
		ResourceAttributeDefinition uidDef = accountDef1
				.findAttributeDefinition(SchemaTestConstants.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canAdd());
		assertFalse("UID has update",uidDef.canModify());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef1.getPrimaryIdentifiers().contains(uidDef));
		assertEquals("Wrong refined displayName for attribute "+SchemaTestConstants.ICFS_UID, "Modified ConnId UID", uidDef.getDisplayName());
		assertEquals("Wrong refined displayOrder for attribute "+SchemaTestConstants.ICFS_UID, (Integer)100, uidDef.getDisplayOrder());
		
		ResourceAttributeDefinition nameDef = accountDef1
				.findAttributeDefinition(SchemaTestConstants.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canAdd());
		assertTrue("No NAME update",nameDef.canModify());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef1.getSecondaryIdentifiers().contains(nameDef));
		assertEquals("Wrong refined displayName for attribute "+SchemaTestConstants.ICFS_NAME, "Modified ConnId Name", nameDef.getDisplayName());
		assertEquals("Wrong refined displayOrder for attribute "+SchemaTestConstants.ICFS_NAME, (Integer)110, nameDef.getDisplayOrder());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef1.findAttributeDefinition(new QName(SchemaTestConstants.NS_ICFS,"password")));
		
		// ACCOUNT
		ObjectClassComplexTypeDefinition accountDef = resorceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertNotNull("No ACCOUNT kind definition", accountDef);
		
		ResourceAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canAdd());
		assertTrue("No fullname update", fullnameDef.canModify());
		assertTrue("No fullname read", fullnameDef.canRead());
		assertTrue("Wrong displayOrder for attribute fullName: "+fullnameDef.getDisplayOrder(),
				fullnameDef.getDisplayOrder() == 200 || fullnameDef.getDisplayOrder() == 250 || fullnameDef.getDisplayOrder() == 260);
		
		// GROUP
		ObjectClassComplexTypeDefinition groupObjectClass = resorceSchema.findObjectClassDefinition(SchemaTestConstants.GROUP_OBJECT_CLASS_LOCAL_NAME);
		assertNotNull("No group objectClass", groupObjectClass);
		
		ResourceAttributeDefinition membersDef = groupObjectClass.findAttributeDefinition(DummyResourceContoller.DUMMY_GROUP_MEMBERS_ATTRIBUTE_NAME);
		assertNotNull("No definition for members", membersDef);
		assertEquals("Wrong maxOccurs", -1, membersDef.getMaxOccurs());
		assertEquals("Wrong minOccurs", 0, membersDef.getMinOccurs());
		assertTrue("No members create", membersDef.canAdd());
		assertTrue("No members update", membersDef.canModify());
		assertTrue("No members read", membersDef.canRead());
		
		assertEquals("Unexpected number of schema definitions in "+dummyResourceSchemalessCtl.getName()+" dummy resource", dummyResourceStaticSchema.getNumberOfObjectclasses(), resorceSchema.getDefinitions().size());
		
		for (Definition def: resorceSchema.getDefinitions()) {
			if (def instanceof RefinedObjectClassDefinition) {
				AssertJUnit.fail("Refined definition sneaked into resource schema of "+dummyResourceSchemalessCtl.getName()+" dummy resource: "+def);
			}
		}
	}

	@Test
	public void test107Capabilities() throws Exception {
		final String TEST_NAME = "test107Capabilities";
		TestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		ResourceType resourceType = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, null, result)
				.asObjectable();

		// THEN
		TestUtil.displayThen(TEST_NAME);
		assertSuccess(result);

		// Check native capabilities
		CapabilityCollectionType nativeCapabilities = resourceType.getCapabilities().getNative();
		System.out.println("Native capabilities: " + PrismTestUtil.serializeAnyDataWrapped(nativeCapabilities));
		System.out.println("resource: " + resourceType.asPrismObject().debugDump());
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
		TestConnectionCapabilityType capTest = CapabilityUtil.getCapability(nativeCapabilitiesList,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ScriptCapabilityType.class);
		assertNotNull("native script capability not present", capScript);
		assertNotNull("No host in native script capability", capScript.getHost());
		assertFalse("No host in native script capability", capScript.getHost().isEmpty());

		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList,
				CredentialsCapabilityType.class);
		assertNull("Unexpected native credentials capability", capCred);
		ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ActivationCapabilityType.class);
		assertNull("Unexpected native activation capability", capAct);

		// Check effective capabilites
		capCred = ResourceTypeUtil.getEffectiveCapability(resourceType, CredentialsCapabilityType.class);
		assertNotNull("password capability not found", capCred.getPassword());
		// Although connector does not support activation, the resource
		// specifies a way how to simulate it.
		// Therefore the following should succeed
		capAct = ResourceTypeUtil.getEffectiveCapability(resourceType, ActivationCapabilityType.class);
		assertNotNull("activation capability not found", capCred.getPassword());

		List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resourceType);
		for (Object capability : effectiveCapabilities) {
			display("Capability " + CapabilityUtil.getCapabilityDisplayName(capability), capability);
		}
	}

	/**
	 * Try a very basic operation and see if that works ...
	 */
	@Test
	public void test200AddAccount() throws Exception {
		final String TEST_NAME = "test200AddAccount";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName() + "." + TEST_NAME);

		ShadowType account = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
		account.asPrismObject().checkConsistence();

		display("Adding shadow", account.asPrismObject());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, taskManager.createTaskInstance(), result);

		// THEN
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		account.asPrismObject().checkConsistence();

		ShadowType accountType = repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, result)
				.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", "will", accountType.getName());
//		assertEquals("will", accountType.getName());

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, null, result).asObjectable();
		display("account from provisioning", provisioningAccountType);
		PrismAsserts.assertEqualsPolyString("Wrong name", "will", provisioningAccountType.getName());
//		assertEquals("will", provisioningAccountType.getName());

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ShadowUtil.getAttributeValues(
				provisioningAccountType, new QName(SchemaConstants.NS_ICF_SCHEMA, "password")));

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResourceStaticSchema.getAccountByUsername("will");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());

		// Check if the shadow is in the repo
		PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
				addedObjectOid, null, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.debugDump());

		ProvisioningTestUtil.checkRepoAccountShadow(shadowFromRepo);

	}

}
