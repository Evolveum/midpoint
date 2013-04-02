/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

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
public class TestDummySchemaless extends AbstractIntegrationTest {

	private static final String TEST_DIR = "src/test/resources/impl/dummy-schemaless/";

	private static final String RESOURCE_DUMMY_NO_SCHEMA_FILENAME = TEST_DIR 
			+ "resource-dummy-schemaless-no-schema.xml";
	private static final String RESOURCE_DUMMY_NO_SCHEMA_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0000";
	private static final String RESOURCE_DUMMY_NO_SCHEMA_INSTANCE_ID = "schemaless";
	
	private static final String RESOURCE_DUMMY_STATIC_SCHEMA_FILENAME = TEST_DIR 
			+ "resource-dummy-schemaless-static-schema.xml";
	private static final String RESOURCE_DUMMY_STATIC_SCHEMA_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0505";
	private static final String RESOURCE_DUMMY_STATIC_SCHEMA_INSTANCE_ID = "staticSchema";
	
	private static final String ACCOUNT_WILL_FILENAME = TEST_DIR + "account-will.xml";
	private static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
	private static final String ACCOUNT_WILL_ICF_UID = "will";

	private static final Trace LOGGER = TraceManager.getTrace(TestDummySchemaless.class);

	private PrismObject<ResourceType> resourceSchemaless;
	private ResourceType resourceTypeSchemaless;
	private static DummyResource dummyResourceSchemaless;
	
	private PrismObject<ResourceType> resourceStaticSchema;
	private ResourceType resourceTypeStaticSchema;
	private static DummyResource dummyResourceStaticSchema;

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	
//	@Autowired
//	TaskManager taskManager;

	/**
	 * @throws JAXBException
	 */
	public TestDummySchemaless() throws JAXBException {
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

		resourceSchemaless = addResourceFromFile(RESOURCE_DUMMY_NO_SCHEMA_FILENAME, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceTypeSchemaless = resourceSchemaless.asObjectable();

		dummyResourceSchemaless = DummyResource.getInstance(RESOURCE_DUMMY_NO_SCHEMA_INSTANCE_ID);
		dummyResourceSchemaless.reset();
		dummyResourceSchemaless.populateWithDefaultSchema();
		
		resourceStaticSchema = addResourceFromFile(RESOURCE_DUMMY_STATIC_SCHEMA_FILENAME, IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
		resourceTypeStaticSchema = resourceStaticSchema.asObjectable();

		dummyResourceStaticSchema = DummyResource.getInstance(RESOURCE_DUMMY_STATIC_SCHEMA_INSTANCE_ID);
		dummyResourceStaticSchema.reset();
		dummyResourceStaticSchema.populateWithDefaultSchema();

	}

	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");

		display("Dummy resource instance", dummyResourceSchemaless.toString());

		assertNotNull("Resource is null", resourceSchemaless);
		assertNotNull("ResourceType is null", resourceTypeSchemaless);

		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_SCHEMA_OID, result)
				.asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, result).asObjectable();
		assertNotNull(connector);
		display("Dummy Connector", connector);

		// Check connector schema
		ProvisioningTestUtil.assertConnectorSchemaSanity(connector, prismContext);
	}
	

	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test003ConnectionSchemaless() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test003ConnectionSchemaless");
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test003ConnectionSchemaless");
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_SCHEMA_OID, result)
				.asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class,
				resourceBefore.getConnectorRef().getOid(), result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_NO_SCHEMA_OID);

		// THEN
		display("Test result", testResult);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONFIGURATION_VALIDATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertTestResourceFailure(testResult, ConnectorTestOperation.CONNECTOR_SCHEMA);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_NO_SCHEMA_OID, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
	}

	/**
	 * This basically checks if the methods do not die on NPE
	 */
	@Test
	public void test005ParsedSchemaSchemaless() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException {
		displayTestTile("test005ParsedSchemaSchemaless");
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test005ParsedSchemaSchemaless");

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceTypeSchemaless));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceTypeSchemaless, prismContext);

		display("Parsed resource schema", returnedSchema);
		
		assertNull("Unexpected schema after parsing", returnedSchema);
	}

	@Test
	public void test006GetObjectSchemaless() throws Exception {
		displayTestTile("test006GetObjectSchemaless");
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test006GetObjectSchemaless");

		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_SCHEMA_OID, null, result);
		assertNotNull("Resource is null", resource);
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("No connector ref", resourceType.getConnectorRef());
		assertNotNull("No connector ref OID", resourceType.getConnectorRef().getOid());		
	}

	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test103ConnectionStaticSchema() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test103ConnectionStaticSchema");
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test003ConnectionSchemaless");

		// Check that there a schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, result)
				.asObjectable();
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNotNull("No schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_STATIC_SCHEMA_OID);

		// THEN
		display("Test result", testResult);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_INITIALIZATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONFIGURATION_VALIDATION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_CONNECTION);
		assertTestResourceSuccess(testResult, ConnectorTestOperation.CONNECTOR_SCHEMA);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,
				RESOURCE_DUMMY_NO_SCHEMA_OID, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after test", resourceTypeRepoAfter);
		
		// TODO
	}

	/**
	 * This basically checks if the methods do not die on NPE
	 */
	@Test
	public void test105ParsedSchemaStaticSchema() throws ObjectNotFoundException, CommunicationException, SchemaException,
			ConfigurationException {
		displayTestTile("test105ParsedSchemaStaticSchema");
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test105ParsedSchemaStaticSchema");

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceTypeStaticSchema));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceTypeStaticSchema, prismContext);

		display("Parsed resource schema", returnedSchema);
		assertNotNull("Null resource schema", returnedSchema);
		
		ProvisioningTestUtil.assertDummyResourceSchemaSanity(returnedSchema, resourceTypeStaticSchema);
	}

	@Test
	public void test106GetObjectStaticSchema() throws Exception {
		displayTestTile("test106GetObjectStaticSchema");
		// GIVEN
		OperationResult result = new OperationResult(TestDummySchemaless.class.getName()
				+ ".test106GetObjectStaticSchema");

		PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, result);
		assertNotNull("Resource is null", resource);
		ResourceType resourceType = resource.asObjectable();
		assertNotNull("No connector ref", resourceType.getConnectorRef());
		assertNotNull("No connector ref OID", resourceType.getConnectorRef().getOid());
		
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);

		display("Parsed resource schema", returnedSchema);
		assertNotNull("Null resource schema", returnedSchema);
		
		ProvisioningTestUtil.assertDummyResourceSchemaSanity(returnedSchema, resource.asObjectable());
	}
	
	@Test
	public void test107Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException,
			JAXBException, ConfigurationException, SecurityViolationException {
		displayTestTile("test107Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test107Capabilities");

		// WHEN
		ResourceType resourceType = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_STATIC_SCHEMA_OID, null, result)
				.asObjectable();

		// THEN

		// Check native capabilities
		CapabilityCollectionType nativeCapabilities = resourceType.getCapabilities().getNative();
		System.out.println("Native capabilities: " + PrismTestUtil.marshalWrap(nativeCapabilities));
		System.out.println("resource: " + resourceType.asPrismObject().dump());
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList,
				CredentialsCapabilityType.class);
		assertNotNull("password native capability not present", capCred.getPassword());
		ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ActivationCapabilityType.class);
		assertNotNull("native activation capability not present", capAct);
		assertNotNull("native activation/enabledisable capability not present", capAct.getEnableDisable());
		TestConnectionCapabilityType capTest = CapabilityUtil.getCapability(nativeCapabilitiesList,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = CapabilityUtil.getCapability(nativeCapabilitiesList,
				ScriptCapabilityType.class);
		assertNotNull("native script capability not present", capScript);
		assertNotNull("No host in native script capability", capScript.getHost());
		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
		// TODO: better look inside

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
			System.out.println("Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : "
					+ capability);
		}
	}

	/**
	 * Try a very basic operation and see if that works ...
	 */
	@Test
	public void test200AddAccount() throws Exception {
		displayTestTile("test110AddAccount");
		// GIVEN
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".test110AddAccount");

		AccountShadowType account = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, AccountShadowType.class);
		account.asPrismObject().checkConsistence();

		display("Adding shadow", account.asPrismObject());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, null, taskManager.createTaskInstance(), result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		account.asPrismObject().checkConsistence();

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID, result)
				.asObjectable();
		PrismAsserts.assertEqualsPolyString("Wrong name", "will", accountType.getName());
//		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_WILL_OID, null, result).asObjectable();
		display("account from provisioning", provisioningAccountType);
		PrismAsserts.assertEqualsPolyString("Wrong name", "will", provisioningAccountType.getName());
//		assertEquals("will", provisioningAccountType.getName());

		assertNull("The _PASSSWORD_ attribute sneaked into shadow", ResourceObjectShadowUtil.getAttributeValues(
				provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "password")));

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResourceStaticSchema.getAccountByUsername("will");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());

		// Check if the shadow is in the repo
		PrismObject<AccountShadowType> shadowFromRepo = repositoryService.getObject(AccountShadowType.class,
				addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());

		ProvisioningTestUtil.checkRepoShadow(shadowFromRepo);

	}

}
