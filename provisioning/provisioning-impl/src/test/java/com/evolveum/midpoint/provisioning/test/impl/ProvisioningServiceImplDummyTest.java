/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.DeltaConvertor;
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
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.apache.commons.lang.StringUtils;
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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

/**
 * The test of Provisioning service on the API level. The test is using dummy resource for speed and flexibility.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-task.xml",
        "classpath:application-context-audit.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext
public class ProvisioningServiceImplDummyTest extends AbstractIntegrationTest {

	private static final String FILENAME_RESOURCE_DUMMY = "src/test/resources/object/resource-dummy.xml";
	private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	
	private static final String ACCOUNT_WILL_FILENAME = "src/test/resources/impl/account-dummy.xml";
	private static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
	private static final String ACCOUNT_WILL_ICF_UID = "will";
	
	private static final String ACCOUNT_DAEMON_USERNAME = "daemon";
	private static final String ACCOUNT_DAEMON_OID = "c0c010c0-dddd-dddd-dddd-dddddddae604";
	private static final String ACCOUNT_DAEMON_FILENAME = "src/test/resources/impl/account-dummy-daemon.xml";
	
	private static final String ACCOUNT_DAVIEJONES_USERNAME = "daviejones";
	
	private static final String ACCOUNT_MORGAN_FILENAME = "src/test/resources/impl/account-dummy-noname.xml";
	private static final String ACCOUNT_MORGAN_OID = "c0c010c0-d34d-b44f-f11d-444400008888";
	private static final String ACCOUNT_MORGAN_NAME = "morgan";
	
	private static final String FILENAME_ACCOUNT_SCRIPT = "src/test/resources/impl/account-dummy-script.xml";
	private static final String ACCOUNT_NEW_SCRIPT_OID = "c0c010c0-d34d-b44f-f11d-33322212abcd";
	private static final String FILENAME_ENABLE_ACCOUNT = "src/test/resources/impl/enable-account.xml";
	private static final String FILENAME_DISABLE_ACCOUNT = "src/test/resources/impl/disable-account.xml";
	private static final String FILENAME_SCRIPT_ADD = "src/test/resources/impl/script-add.xml";
	private static final String DUMMY_CONNECTOR_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";

	private static final String NOT_PRESENT_OID = "deaddead-dead-dead-dead-deaddeaddead";
	
	private static final String BLACKBEARD_USERNAME = "blackbeard";
	private static final String DRAKE_USERNAME = "drake";
	
	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImplDummyTest.class);

	private PrismObject<ResourceType> resource;
	private ResourceType resourceType;
	private static DummyResource dummyResource;
	private static Task syncTask;
	
	@Autowired(required=true)
	private ProvisioningService provisioningService;
	
	// Used to make sure that the connector is cached
	@Autowired(required=true)
	private ConnectorTypeManager connectorTypeManager;
	
	@Autowired(required=true)
	private SynchornizationServiceMock syncServiceMock; 
	

	/**
	 * @throws JAXBException
	 */
	public ProvisioningServiceImplDummyTest() throws JAXBException {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem()
	 */

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		resource = addResourceFromFile(FILENAME_RESOURCE_DUMMY, DUMMY_CONNECTOR_TYPE, initResult);
		resourceType = resource.asObjectable();
		
		dummyResource = DummyResource.getInstance();
		dummyResource.reset();
		dummyResource.populateWithDefaultSchema();
		
		DummyAccount dummyAccountDaemon = new DummyAccount(ACCOUNT_DAEMON_USERNAME);
		dummyAccountDaemon.setEnabled(true);
		dummyAccountDaemon.addAttributeValues("fullname", "Evil Daemon");
		dummyResource.addAccount(dummyAccountDaemon);
		
		addObjectFromFile(ACCOUNT_DAEMON_FILENAME, initResult);
	}

	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");

		display("Dummy resource instance", dummyResource.toString());
		
		assertNotNull("Resource is null", resource);
		assertNotNull("ResourceType is null", resourceType);

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				result).asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService
				.getObject(ConnectorType.class, connectorOid, result).asObjectable();
		assertNotNull(connector);
		display("Dummy Connector", connector);
		
		// Check connector schema
		ProvisioningTestUtil.assertConnectorSchemaSanity(connector, prismContext);
	}

	/**
	 * Check whether the connectors were discovered correctly and were added to
	 * the repository.
	 * 
	 * @throws SchemaException
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
		displayTestTile("test001Connectors");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test001Connectors");

		// WHEN
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, null, null, result);

		// THEN
		assertFalse("No connector found", connectors.isEmpty());
		for (PrismObject<ConnectorType> connPrism : connectors) {
			ConnectorType conn = connPrism.asObjectable();
			display("Found connector "+conn, conn);
			
			display("XML "+conn, PrismTestUtil.serializeObjectToString(connPrism));
					
			XmlSchemaType xmlSchemaType = conn.getSchema();
			assertNotNull("xmlSchemaType is null", xmlSchemaType);
			Element connectorXsdSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(conn);
			assertNotNull("No schema", connectorXsdSchemaElement);
			
			// Try to parse the schema
			PrismSchema schema = PrismSchema.parse(connectorXsdSchemaElement, "connector schema "+conn, prismContext);
			assertNotNull("Cannot parse schema", schema);
			assertFalse("Empty schema", schema.isEmpty());
			
			display("Parsed connector schema "+conn, schema);
			
			QName configurationElementQname = new QName(conn.getNamespace(), "configuration");
			PrismContainerDefinition configurationContainer = schema.findContainerDefinitionByElementName(configurationElementQname);
			assertNotNull("No "+configurationElementQname+" element in schema of "+conn, configurationContainer);
			PrismContainerDefinition definition = schema.findItemDefinition("configuration",
					PrismContainerDefinition.class);
			assertNotNull("Definition of <configuration> property container not found", definition);
			PrismContainerDefinition pcd = (PrismContainerDefinition) definition;
			assertFalse("Empty definition", pcd.isEmpty());
		}
	}

	/**
	 * Running discovery for a second time should return nothing - as nothing
	 * new was installed in the meantime.
	 */
	@Test
	public void test002ConnectorRediscovery() {
		displayTestTile("test002ConnectorRediscovery");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test002ConnectorRediscovery");

		// WHEN
		Set<ConnectorType> discoverLocalConnectors = connectorTypeManager.discoverLocalConnectors(result);

		// THEN
		result.computeStatus();
		assertSuccess("discoverLocalConnectors failed", result);
		assertTrue("Rediscovered something", discoverLocalConnectors.isEmpty());
	}

	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema
	 * should be generated from the resource on the first use. This is the test
	 * that executes testResource and checks whether the schema was generated.
	 */
	@Test
	public void test003Connection() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test003Connection");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test003Connection");
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				result).asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore
				.getConnectorRef().getOid(), result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);

		// THEN
		display("Test result", testResult);
		assertSuccess("Test resource failed (result)", testResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable(); 
		display("Resource after test", resourceTypeRepoAfter);

		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		String resourceXml = prismContext.getPrismDomProcessor().serializeObjectToString(resourceRepoAfter);
		display("Resource XML", resourceXml);

		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resourceBefore.toString(), prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		// schema will be checked in next test
	}
	
	@Test
	public void test004Configuration() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test004Configuration");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test004Configuration");

		// WHEN
		resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		resourceType = resource.asObjectable();

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		assertNotNull("No configuration container", configurationContainer);
		PrismContainerDefinition confContDef = configurationContainer.getDefinition();
		assertNotNull("No configuration container definition", confContDef);
		PrismContainer confingurationPropertiesContainer = 
			configurationContainer.findContainer(ConnectorFactoryIcfImpl.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);
		assertNotNull("No configuration properties container", confingurationPropertiesContainer);
		PrismContainerDefinition confPropsDef = confingurationPropertiesContainer.getDefinition();
		assertNotNull("No configuration properties container definition", confPropsDef);
		List<PrismProperty<?>> configurationProperties = confingurationPropertiesContainer.getValue().getItems();
		assertFalse("No configuration properties", configurationProperties.isEmpty());
		for (PrismProperty<?> confProp: configurationProperties) {
			PrismPropertyDefinition confPropDef = confProp.getDefinition();
			assertNotNull("No definition for configuration property "+confProp, confPropDef);
			assertFalse("Configuration property "+confProp+" is raw", confProp.isRaw());
		}
	}

	@Test
	public void test005ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		displayTestTile("test005ParsedSchema");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test005ParsedSchema");

		// THEN
		// The returned type should have the schema pre-parsed
		assertNotNull(RefinedResourceSchema.hasParsedSchema(resourceType));

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
		
		display("Parsed resource schema", returnedSchema);

		// Check whether it is reusing the existing schema and not parsing it all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching", returnedSchema == RefinedResourceSchema.getResourceSchema(resourceType, prismContext));

		ProvisioningTestUtil.assertDummyResourceSchemaSanity(returnedSchema, resourceType);

	}
	
	@Test
	public void test006RefinedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		displayTestTile("test006RefinedSchema");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test006RefinedSchema");

		// WHEN
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType, prismContext);
		display("Refined schema", refinedSchema);

		// Check whether it is reusing the existing schema and not parsing it all over again
		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue("Broken caching", refinedSchema == RefinedResourceSchema.getRefinedSchema(resourceType, prismContext));

		RefinedAccountDefinition accountDef = refinedSchema.getDefaultAccountDefinition();
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition uidDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(0, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canCreate());
		assertFalse("UID has update",uidDef.canUpdate());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canCreate());
		assertTrue("No NAME update",nameDef.canUpdate());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canCreate());
		assertTrue("No fullname update", fullnameDef.canUpdate());
		assertTrue("No fullname read", fullnameDef.canRead());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));

	}

	@Test
	public void test006Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException, JAXBException, ConfigurationException, SecurityViolationException {
		displayTestTile("test006Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test006Capabilities");

		// WHEN
		ResourceType resourceType = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result).asObjectable();

		// THEN

		// Check native capabilities
		CachedCapabilitiesType nativeCapabilities = resourceType.getNativeCapabilities();
		System.out.println("Native capabilities: " + PrismTestUtil.marshalWrap(nativeCapabilities));
		System.out.println("resource: " + resourceType.asPrismObject().dump());
		List<Object> nativeCapabilitiesList = nativeCapabilities.getCapabilities().getAny();
		assertFalse("Empty capabilities returned", nativeCapabilitiesList.isEmpty());
		CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				CredentialsCapabilityType.class);
		assertNotNull("password native capability not present", capCred.getPassword());
		ActivationCapabilityType capAct = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				ActivationCapabilityType.class);
		assertNotNull("native activation capability not present", capAct);
		assertNotNull("native activation/enabledisable capability not present", capAct.getEnableDisable());
		TestConnectionCapabilityType capTest = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = ResourceTypeUtil.getCapability(nativeCapabilitiesList,
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

		List<Object> effectiveCapabilities = ResourceTypeUtil.listEffectiveCapabilities(resourceType);
		for (Object capability : effectiveCapabilities) {
			System.out.println("Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : "
					+ capability);
		}
	}
	
	@Test
	public void test007ResourceAndConnectorCaching() throws Exception {
		displayTestTile("test007ResourceAndConnectorCaching");

		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test007ResourceAndConnectorCaching");
		ConnectorInstance configuredConnectorInstance = connectorTypeManager.getConfiguredConnectorInstance(resourceType, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);
		
		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class,RESOURCE_DUMMY_OID, null, result);
		
		//THEN
		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref",resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeAgain.getConnectorRef().getOid());

		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONFIGURATION);		
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
		
		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
		
		// Now we stick our nose deep inside the provisioning impl. But we need to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = connectorTypeManager.getConfiguredConnectorInstance(resourceTypeAgain, result);
		assertNotNull("No configuredConnectorInstance (again)", configuredConnectorInstance);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);
		
		// Check if the connector still works
		OperationResult testResult = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test007ResourceAndConnectorCaching.test");
		configuredConnectorInstanceAgain.test(testResult);
		testResult.computeStatus();
		assertSuccess("Connector test failed", testResult);
	}

	@Test
	public void test010AddAccount() throws Exception {
		displayTestTile("test010AddAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test010AddAccount");

		AccountShadowType account = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, AccountShadowType.class);

		display("Adding shadow", account.asPrismObject());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_WILL_OID,
				result).asObjectable();
		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_WILL_OID, null, result).asObjectable();
		display("account from provisioning",provisioningAccountType);
		assertEquals("will", provisioningAccountType.getName());
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow",
				ResourceObjectShadowUtil.getAttributeValues(provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());
		
		// Check if the shadow is in the repo
		PrismObject<AccountShadowType> shadowFromRepo = repositoryService.getObject(AccountShadowType.class, addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository",shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());
		
		checkRepoShadow(shadowFromRepo);
		
		checkConsistency();
	}
	
	@Test
	public void test011AddAccountWithoutName() throws Exception {
		displayTestTile("test011AddAccountWithoutName");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test010Atest011AddAccountWithoutNameddAccount");

		AccountShadowType account = parseObjectTypeFromFile(ACCOUNT_MORGAN_FILENAME, AccountShadowType.class);

		display("Adding shadow", account.asPrismObject());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_MORGAN_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_MORGAN_OID,
				result).asObjectable();
		assertEquals("Account name was not generated (repository)", ACCOUNT_MORGAN_NAME, accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_MORGAN_OID, null, result).asObjectable();
		display("account from provisioning",provisioningAccountType);
		assertEquals("Account name was not generated (provisioning)", ACCOUNT_MORGAN_NAME, provisioningAccountType.getName());
		
		assertNull("The _PASSSWORD_ attribute sneaked into shadow",
				ResourceObjectShadowUtil.getAttributeValues(provisioningAccountType, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));

		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_MORGAN_NAME);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Captain Morgan", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "sh1verM3T1mb3rs", dummyAccount.getPassword());
		
		// Check if the shadow is in the repo
		PrismObject<AccountShadowType> shadowFromRepo = repositoryService.getObject(AccountShadowType.class, addedObjectOid, result);
		assertNotNull("Shadow was not created in the repository",shadowFromRepo);
		display("Repository shadow", shadowFromRepo.dump());
		
		checkRepoShadow(shadowFromRepo);
		
		checkConsistency();
	}

	@Test
	public void test011GetAccount() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test011GetAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test011GetAccount");

		// WHEN
		AccountShadowType shadow = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_WILL_OID, null, result).asObjectable();

		// THEN
		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);
		
		checkShadow(shadow, result);
		
		checkConsistency();
	}
	
	@Test
	public void test012SeachIterative() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, SecurityViolationException {
		displayTestTile("test012SeachIterative");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test012SeachIterative");
		
		// Make sure there is an account on resource that the provisioning has never seen before, so there is no shadow
		// for it yet.
		DummyAccount newAccount = new DummyAccount("meathook");
		newAccount.addAttributeValues("fullname", "Meathook");
		newAccount.setEnabled(true);
		newAccount.setPassword("parrotMonster");
		dummyResource.addAccount(newAccount);
		
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		query.setFilter(QueryUtil.createAndFilter(doc,
				QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF, RESOURCE_DUMMY_OID),
				QueryUtil.createEqualFilter(doc, null, SchemaConstants.I_OBJECT_CLASS, 
						new QName(ResourceTypeUtil.getResourceNamespace(resourceType),ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME)),
				QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME, "will")));
		
		final List<AccountShadowType> foundObjects = new ArrayList<AccountShadowType>();
		ResultHandler<AccountShadowType> handler = new ResultHandler<AccountShadowType>() {
			
			@Override
			public boolean handle(PrismObject<AccountShadowType> object, OperationResult parentResult) {
				foundObjects.add(object.asObjectable());
		
				ObjectType objectType = object.asObjectable();
				assertTrue(objectType instanceof AccountShadowType);
                AccountShadowType shadow = (AccountShadowType) objectType;
                checkShadow(shadow, parentResult);                
                return true;
			}
		};
		
		// WHEN
		provisioningService.searchObjectsIterative(AccountShadowType.class, query, null, handler , result);
		
		// THEN
		
		assertEquals(3,foundObjects.size());
				
		checkConsistency();
		
		// And again ...
		
		foundObjects.clear();
		
		// WHEN
		provisioningService.searchObjectsIterative(AccountShadowType.class, query, null, handler , result);
		
		// THEN
		
		assertEquals(3,foundObjects.size());
				
		checkConsistency();
	}
	
	@Test
	public void test013SearchAllShadowsInRepository() throws Exception {
		displayTestTile("test013SearchAllShadowsInRepository");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test013SearchAllShadowsInRepository");
		QueryType query = IntegrationTestTools.createAllShadowsQuery(resourceType);
		display("All shadows query", query);
		
		List<PrismObject<AccountShadowType>> allShadows = repositoryService.searchObjects(AccountShadowType.class, query, null , result);
		display("Found "+allShadows.size()+" shadows");
		
		assertFalse("No shadows found", allShadows.isEmpty());
	}
	
	@Test
	public void test014SearchAllShadows() throws Exception {
		displayTestTile("test014SearchAllShadows");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test014SearchAllShadows");
		QueryType query = IntegrationTestTools.createAllShadowsQuery(resourceType, SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
		display("All shadows query", query);
		
		List<PrismObject<AccountShadowType>> allShadows = provisioningService.searchObjects(AccountShadowType.class, query, null , result);
		display("Found "+allShadows.size()+" shadows");
		
		assertFalse("No shadows found", allShadows.isEmpty());
		assertEquals("Wrong number of results", 3, allShadows.size());
	}
	
	@Test
	public void test015countAllShadows() throws Exception {
		displayTestTile("test015countAllShadows");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test015countAllShadows");
		QueryType query = IntegrationTestTools.createAllShadowsQuery(resourceType, SchemaTestConstants.ICF_ACCOUNT_OBJECT_CLASS_LOCAL_NAME);
		display("All shadows query", query);
		
		int count = provisioningService.countObjects(AccountShadowType.class, query, result);
		display("Found "+count+" shadows");
		
		assertEquals("Wrong number of results", 3, count);
	}
	
	@Test
	public void test016SearchNullQueryResource() throws Exception {
		displayTestTile("test016SearchNullQueryResource");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test016SearchNullQueryResource");
		
		List<PrismObject<ResourceType>> allResources = provisioningService.searchObjects(ResourceType.class, null, null , result);
		display("Found "+allResources.size()+" resources");
		
		assertFalse("No resources found", allResources.isEmpty());
		assertEquals("Wrong number of results", 1, allResources.size());
	}
	
	@Test
	public void test017CountNullQueryResource() throws Exception {
		displayTestTile("test017CountNullQueryResource");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test017CountNullQueryResource");
		
		int count = provisioningService.countObjects(ResourceType.class, null, result);
		display("Counted "+count+" resources");
		
		assertEquals("Wrong count", 1, count);
	}

		
	private void checkShadow(AccountShadowType shadow, OperationResult parentResult) {
		ObjectChecker<AccountShadowType> checker = createShadowChecker();
		IntegrationTestTools.checkShadow(shadow, resourceType, repositoryService, checker, prismContext, parentResult);
	}
	
	private void checkAllShadows() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {		
		ObjectChecker<AccountShadowType> checker = null;
		IntegrationTestTools.checkAllShadows(resourceType, repositoryService, checker, prismContext);		
	}

	private ObjectChecker<AccountShadowType> createShadowChecker() {
		return  new ObjectChecker<AccountShadowType>() {
			@Override
			public void check(AccountShadowType shadow) {
				String icfName = ResourceObjectShadowUtil.getSingleStringAttributeValue(shadow, SchemaTestConstants.ICFS_NAME);
		        assertNotNull("No ICF NAME", icfName);
		        assertEquals("Wrong shadow name", shadow.getName(), icfName);
		        assertNotNull("Missing fullname attribute", ResourceObjectShadowUtil.getSingleStringAttributeValue(shadow,
								new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "fullname")));
				assertNotNull("no activation",shadow.getActivation());
				assertNotNull("no activation/enabled",shadow.getActivation().isEnabled());
				assertTrue("not enabled",shadow.getActivation().isEnabled());
				
				assertProvisioningAccountShadow(shadow.asPrismObject(), resourceType, ResourceAttributeDefinition.class);				
			}

		};
	}

	@Test
	public void test021EnableAccount() throws FileNotFoundException, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		displayTestTile("test021EnableAccount");
		// GIVEN

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test021EnableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_WILL_OID, null, result).asObjectable();
		assertNotNull(accountType);

		// THEN
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertTrue(dummyAccount.isEnabled());

		ObjectModificationType objectModification = unmarshallJaxbFromFile(FILENAME_DISABLE_ACCOUNT,
				ObjectModificationType.class);
		ObjectDelta<AccountShadowType> delta = DeltaConvertor.createObjectDelta(objectModification, 
				AccountShadowType.class, PrismTestUtil.getPrismContext());		
		System.out.println("ObjectDelta:");
		System.out.println(delta.dump());

		provisioningService.modifyObject(AccountShadowType.class, objectModification.getOid(), delta.getModifications(),
				new ScriptsType(), result);

		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername("will");
		assertFalse(dummyAccount.isEnabled());
	}

	@Test
	public void test022DisableAccount() throws FileNotFoundException, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		displayTestTile("test022EnableAccount");
		// GIVEN

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test022EnableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_WILL_OID, null, result).asObjectable();
		assertNotNull(accountType);

		// THEN
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertFalse("Account is not disabled", dummyAccount.isEnabled());

		ObjectModificationType objectModification = unmarshallJaxbFromFile(FILENAME_ENABLE_ACCOUNT,
				ObjectModificationType.class);
		ObjectDelta<AccountShadowType> delta = DeltaConvertor.createObjectDelta(objectModification, 
				AccountShadowType.class, PrismTestUtil.getPrismContext());
		System.out.println(SchemaDebugUtil.prettyPrint(objectModification));
		System.out.println("ObjectDelta:");
		System.out.println(delta.dump());

		provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(), new ScriptsType(),
				result);

		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername("will");
		assertTrue(dummyAccount.isEnabled());
	}

	@Test
	public void test031AddScript() throws FileNotFoundException, JAXBException, ObjectAlreadyExistsException,
			SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException {
		displayTestTile("test031AddScript");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test031AddScript");

		AccountShadowType account = parseObjectTypeFromFile(FILENAME_ACCOUNT_SCRIPT, AccountShadowType.class);

		System.out.println(SchemaDebugUtil.prettyPrint(account));
		System.out.println(account.asPrismObject().dump());

		ScriptsType scriptsType = unmarshallJaxbFromFile(FILENAME_SCRIPT_ADD, ScriptsType.class);
		System.out.println(PrismTestUtil.marshalWrap(scriptsType));

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), scriptsType, result);

	
		
		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_NEW_SCRIPT_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, result).asObjectable();
		assertEquals("william", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, null, result).asObjectable();
		assertEquals("william", provisioningAccountType.getName());

		
		
		// Check if the account was created in the dummy resource

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("william");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "William Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled", dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th123", dummyAccount.getPassword());
		//TODO:add check if script was caled
		List<String> scriptsHistory = dummyResource.getScriptHistory();
		for (String script : scriptsHistory) {
			System.out.println("Script: " + script);
		
		}
	}

	@Test
	public void test032ModifyScript() {
		// TODO
	}

	@Test
	public void test033DeleteScript() {
		// TODO
	}
		
	
	@Test
	public void test500AddProtectedAccount() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {
		displayTestTile("test500AddProtectedAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test500AddProtectedAccount");
		
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultAccountDefinition();
		AccountShadowType shadowType = new AccountShadowType();
		PrismTestUtil.getPrismContext().adopt(shadowType);
		shadowType.setName(ACCOUNT_DAVIEJONES_USERNAME);
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		shadowType.setObjectClass(defaultAccountDefinition.getTypeName());
		PrismObject<AccountShadowType> shadow = shadowType.asPrismObject();
		PrismContainer<Containerable> attrsCont = shadow.findOrCreateContainer(AccountShadowType.F_ATTRIBUTES);
		PrismProperty<String> icfsNameProp = attrsCont.findOrCreateProperty(ConnectorFactoryIcfImpl.ICFS_NAME);
		icfsNameProp.setRealValue(ACCOUNT_DAVIEJONES_USERNAME);

		// WHEN
		try {
			provisioningService.addObject(shadow, null, result);
			AssertJUnit.fail("Expected security exception while adding 'daviejones' account");
		} catch (SecurityViolationException e) {
			// This is expected
		}
		
		checkConsistency();
	}
	
	@Test
	public void test501GetProtectedAccountShadow() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test501GetProtectedAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test501GetProtectedAccount");

		// WHEN
		try {
			provisioningService.getObject(AccountShadowType.class,
					ACCOUNT_DAEMON_OID, null, result);
			AssertJUnit.fail("Expected security exception while reading 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
		}
		
		checkConsistency();
	}
	
	@Test
	public void test502ModifyProtectedAccountShadow() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test502ModifyProtectedAccountShadow");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test502ModifyProtectedAccountShadow");

		Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition defaultAccountDefinition = resourceSchema.findDefaultAccountDefinition();
		ResourceAttributeDefinition fullnameAttrDef = defaultAccountDefinition.findAttributeDefinition("fullname");
		ResourceAttribute fullnameAttr = fullnameAttrDef.instantiate();
		PropertyDelta fullnameDelta = fullnameAttr.createDelta(new PropertyPath(ResourceObjectShadowType.F_ATTRIBUTES, fullnameAttrDef.getName()));
		fullnameDelta.setValueToReplace(new PrismPropertyValue<String>("Good Daemon"));
		((Collection)modifications).add(fullnameDelta);
		
		// WHEN
		try {
			provisioningService.modifyObject(AccountShadowType.class,
					ACCOUNT_DAEMON_OID, modifications, null, result);
			AssertJUnit.fail("Expected security exception while modifying 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
		}
		
		checkConsistency();
	}
	
	@Test
	public void test503DeleteProtectedAccountShadow() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test503DeleteProtectedAccountShadow");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test503DeleteProtectedAccountShadow");

		// WHEN
		try {
			provisioningService.deleteObject(AccountShadowType.class, ACCOUNT_DAEMON_OID, null, result);
			AssertJUnit.fail("Expected security exception while deleting 'daemon' account");
		} catch (SecurityViolationException e) {
			// This is expected
		}
		
		checkConsistency();
	}
	
	@Test
	public void test800LiveSyncInit() throws ObjectNotFoundException, CommunicationException, SchemaException, 
			com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {
		displayTestTile("test800LiveSyncInit");
		syncTask = taskManager.createTaskInstance(ProvisioningServiceImplDummyTest.class.getName() + ".syncTask");
		
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		syncServiceMock.reset();
		
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName() + ".test800LiveSyncInit");
		
		// Dry run to remember the current sync token in the task instance. Otherwise a last sync token whould be used and
		// no change would be detected
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTask, result);
		
		// THEN
		
		// No change, no fun
		assertFalse(syncServiceMock.wasCalled());
		
		checkAllShadows();
	}

	@Test
	public void test801LiveSyncAddBlackbeard() throws ObjectNotFoundException, CommunicationException, SchemaException, com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {
		displayTestTile("test801LiveSyncAddBlackbeard");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName() + ".test801LiveSyncAddBlackbeard");
		
		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount(BLACKBEARD_USERNAME);
		newAccount.addAttributeValues("fullname", "Edward Teach");
		newAccount.setEnabled(true);
		newAccount.setPassword("shiverMEtimbers");
		dummyResource.addAccount(newAccount);
		
		display("Resource before sync", dummyResource.dump());
		
		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTask, result);
		
		// THEN
		
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);
		
		assertTrue("Sync service was not called", syncServiceMock.wasCalled());
		
		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);
		
//		assertNull("Old shadow present when not expecting it", lastChange.getOldShadow());
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ResourceObjectShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: "+ currentShadowType.getClass().getName(), currentShadowType instanceof AccountShadowType);
		
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Edward Teach", fullnameAttribute.getRealValue());
		
		checkAllShadows();
	}
	
	@Test
	public void test802LiveSyncModifyBlackbeard() throws ObjectNotFoundException, CommunicationException, SchemaException, com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {
		displayTestTile("test802LiveSyncModifyBlackbeard");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName() + ".test802LiveSyncModifyBlackbeard");
		
		syncServiceMock.reset();
		
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(BLACKBEARD_USERNAME);
		dummyAccount.replaceAttributeValue("fullname", "Captain Blackbeard");
		
		display("Resource before sync", dummyResource.dump());
		
		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTask, result);
		
		// THEN
		
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);
		
		assertTrue("Sync service was not called", syncServiceMock.wasCalled());
		
		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);
		
//		assertNull("Old shadow present when not expecting it", lastChange.getOldShadow());
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ResourceObjectShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: "+ currentShadowType.getClass().getName(), currentShadowType instanceof AccountShadowType);
		
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Captain Blackbeard", fullnameAttribute.getRealValue());
		
		checkAllShadows();
	}
	
	@Test
	public void test803LiveSyncAddDrake() throws ObjectNotFoundException, CommunicationException, SchemaException, com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {
		displayTestTile("test803LiveSyncAddDrake");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName() + ".test803LiveSyncAddDrake");
		
		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount(DRAKE_USERNAME);
		newAccount.addAttributeValues("fullname", "Sir Francis Drake");
		newAccount.setEnabled(true);
		newAccount.setPassword("avast!");
		dummyResource.addAccount(newAccount);
		
		display("Resource before sync", dummyResource.dump());
		
		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTask, result);
		
		// THEN
		
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);
		
		assertTrue("Sync service was not called", syncServiceMock.wasCalled());
		
		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);
		
//		assertNull("Old shadow present when not expecting it", lastChange.getOldShadow());
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ResourceObjectShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: "+ currentShadowType.getClass().getName(), currentShadowType instanceof AccountShadowType);
		
		ResourceAttributeContainer attributesContainer = ResourceObjectShadowUtil.getAttributesContainer(currentShadowType);
		assertNotNull("No attributes container in current shadow", attributesContainer);
		Collection<ResourceAttribute<?>> attributes = attributesContainer.getAttributes();
		assertFalse("Attributes container is empty", attributes.isEmpty());
		assertEquals("Unexpected number of attributes", 3, attributes.size());
		ResourceAttribute<?> fullnameAttribute = attributesContainer.findAttribute(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "fullname"));
		assertNotNull("No fullname attribute in current shadow", fullnameAttribute);
		assertEquals("Wrong value of fullname attribute in current shadow", "Sir Francis Drake", fullnameAttribute.getRealValue());
		
		checkAllShadows();
	}

	@Test
	public void test810LiveSyncModifyProtectedAccount() throws ObjectNotFoundException, CommunicationException, SchemaException, com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {
		displayTestTile("test810LiveSyncModifyProtectedAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName() + ".test810LiveSyncModifyProtectedAccount");
		
		syncServiceMock.reset();
		
		DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_DAEMON_USERNAME);
		dummyAccount.replaceAttributeValue("fullname", "Maxwell deamon");
		
		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, syncTask, result);
		
		// THEN
		
		result.computeStatus();
		display("Synchronization result", result);
		assertSuccess("Synchronization result is not OK", result);
		
		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
		display("The change", lastChange);
		
		assertFalse("Sync service was called (and should not be)", syncServiceMock.wasCalled());
		
		checkAllShadows();
	}
	
	@Test
	public void test901FailResourceNotFound() throws FileNotFoundException, JAXBException, ObjectAlreadyExistsException,
			SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException {
		displayTestTile("test901FailResourceNotFound");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test901FailResourceNotFound");
	
		// WHEN
		try {
			PrismObject<ResourceType> object = provisioningService.getObject(ResourceType.class, NOT_PRESENT_OID, null, result);
			AssertJUnit.fail("Expected ObjectNotFoundException to be thrown, but getObject returned "+object+" instead");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		// TODO: check result
	}

	private void checkRepoShadow(PrismObject<AccountShadowType> repoShadow) {
		AccountShadowType repoShadowType = repoShadow.asObjectable();
		assertNotNull("No OID in repo shadow", repoShadowType.getOid());
		assertNotNull("No name in repo shadow", repoShadowType.getName());
		assertNotNull("No objectClass in repo shadow", repoShadowType.getObjectClass());
		PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(AccountShadowType.F_ATTRIBUTES);
		assertNotNull("No attributes in repo shadow", attributesContainer);
		List<Item<?>> attributes = attributesContainer.getValue().getItems();
		assertFalse("Empty attributes in repo shadow", attributes.isEmpty());
		assertEquals("Unexpected number of attributes in repo shadow", 2, attributes.size());
		
	}
	
	private void checkConsistency() throws SchemaException {
		
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName() + ".checkConsistency");
		
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		XPathHolder xpath = new XPathHolder(SchemaConstants.I_ATTRIBUTES);
		query.setFilter(QueryUtil.createEqualFilter(doc, xpath, ConnectorFactoryIcfImpl.ICFS_UID, ACCOUNT_WILL_ICF_UID));
		System.out.println("Looking for shadows of \""+ACCOUNT_WILL_ICF_UID+"\" with filter "+DOMUtil.serializeDOMToString(query.getFilter()));
		display("Looking for shadows of \""+ACCOUNT_WILL_ICF_UID+"\" with filter "+DOMUtil.serializeDOMToString(query.getFilter()));
		List<PrismObject<AccountShadowType>> objects = repositoryService.searchObjects(AccountShadowType.class, query , null, result);
		
		assertEquals("Wrong number of shadows for ICF UID \"" + ACCOUNT_WILL_ICF_UID + "\"", 1, objects.size());
		
	}

}
