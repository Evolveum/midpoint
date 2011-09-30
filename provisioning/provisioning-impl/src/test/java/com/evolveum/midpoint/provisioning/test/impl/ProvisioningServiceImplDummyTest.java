/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.EnhancedResourceType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CachingMetadata;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.TestConnectionCapabilityType;

/**
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml", "classpath:application-context-task.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class ProvisioningServiceImplDummyTest extends AbstractIntegrationTest {

	private static final String FILENAME_RESOURCE_DUMMY = "src/test/resources/impl/resource-dummy.xml";
	private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	private static final String FILENAME_ACCOUNT = "src/test/resources/impl/account-dummy.xml";
	private static final String ACCOUNT_NEW_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
	private static final String FILENAME_ACCOUNT_SCRIPT = "src/test/resources/impl/account-dummy-script.xml";
	private static final String ACCOUNT_NEW_SCRIPT_OID = "c0c010c0-d34d-b44f-f11d-33322212abcd";
	private static final String FILENAME_ENABLE_ACCOUNT = "src/test/resources/impl/enable-account.xml";
	private static final String FILENAME_DISABLE_ACCOUNT = "src/test/resources/impl/disable-account.xml";
	private static final String FILENAME_SCRIPT_ADD = "src/test/resources/impl/script-add.xml";
	private static final String DUMMY_CONNECTOR_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImplDummyTest.class);

	private ResourceType resource;
	private static DummyResource dummyResource;

	@Autowired
	private ProvisioningService provisioningService;
	@Autowired
	private ConnectorTypeManager connectorTypeManager;

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
		addResourceFromFile(FILENAME_RESOURCE_DUMMY, DUMMY_CONNECTOR_TYPE, initResult);
	}

	@BeforeClass
	public static void initResource() throws Exception {
		dummyResource = DummyResource.getInstance();
		dummyResource.populateWithDefaultSchema();
	}

	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");

		display("Dummy resource instance", dummyResource.toString());

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test000Integrity");

		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null,
				result);
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result);
		assertNotNull(connector);
		display("Dummy Connector", connector);
	}

	/**
	 * Check whether the connectors were discovered correctly and were added to
	 * the repository.
	 * 
	 * @throws SchemaProcessorException
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
		displayTestTile("test001Connectors");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test001Connectors");

		// WHEN
		List<ConnectorType> connectors = repositoryService.listObjects(ConnectorType.class, null, result);

		// THEN
		assertFalse("No connector found", connectors.isEmpty());
		for (ConnectorType conn : connectors) {
			display("Found connector", conn);
			XmlSchemaType xmlSchemaType = conn.getSchema();
			assertNotNull("xmlSchemaType is null", xmlSchemaType);
			assertFalse("Empty schema", xmlSchemaType.getAny().isEmpty());
			// Try to parse the schema
			Schema schema = Schema.parse(xmlSchemaType.getAny().get(0));
			assertNotNull("Cannot parse schema", schema);
			assertFalse("Empty schema", schema.isEmpty());
			display("Parsed connector schema", schema);
			PropertyContainerDefinition definition = schema.findItemDefinition("configuration",
					PropertyContainerDefinition.class);
			assertNotNull("Definition of <configuration> property container not found", definition);
			PropertyContainerDefinition pcd = (PropertyContainerDefinition) definition;
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
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
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
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test003Connection");
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID,
				null, result);
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore
				.getConnectorRef().getOid(), null, result);
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		AssertJUnit.assertTrue("Found schema before test connection. Bad test setup?", xmlSchemaTypeBefore
				.getAny().isEmpty());

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);

		// THEN
		display("Test result", testResult);
		assertSuccess("Test resource failed (result)", testResult);

		resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Resource after test", resource);

		XmlSchemaType xmlSchemaTypeAfter = resource.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		assertFalse("No schema after test connection", xmlSchemaTypeAfter.getAny().isEmpty());

		CachingMetadata cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		Schema parsedSchema = Schema.parse(xsdElement);
		assertNotNull("No schema after parsing", parsedSchema);

		// schema will be checked in next test
	}

	@Test
	public void test004ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException {
		displayTestTile("test004ParsedSchema");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test004ParsedSchema");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null,
				result);

		// THEN
		// The returned type should have the schema pre-parsed
		assertTrue(resource instanceof EnhancedResourceType);
		EnhancedResourceType enh = (EnhancedResourceType) resource;
		assertNotNull(enh.getParsedSchema());

		// Also test if the utility method returns the same thing
		Schema returnedSchema = ResourceTypeUtil.getResourceSchema(resource);

		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue(returnedSchema == enh.getParsedSchema());

		ResourceObjectDefinition accountDef = returnedSchema.findAccountDefinition();
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		ResourceObjectAttributeDefinition uidDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(1, uidDef.getMinOccurs());
		// assertFalse(StringUtils.isEmpty(uidDef.getDisplayName()));
		assertFalse(uidDef.canCreate());
		assertFalse(uidDef.canUpdate());
		assertTrue(uidDef.canRead());

		ResourceObjectAttributeDefinition nameDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		// assertFalse(StringUtils.isEmpty(nameDef.getDisplayName()));
		assertTrue(nameDef.canCreate());
		assertTrue(nameDef.canUpdate());
		assertTrue(nameDef.canRead());

		ResourceObjectAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue(fullnameDef.canCreate());
		assertTrue(fullnameDef.canUpdate());
		assertTrue(fullnameDef.canRead());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));

	}

	@Test
	public void test005Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException {
		displayTestTile("test005Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test005Capabilities");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null,
				result);

		// THEN

		// Check native capabilities
		CapabilitiesType nativeCapabilities = resource.getNativeCapabilities();
		System.out.println("Native capabilities: " + JAXBUtil.silentMarshalWrap(nativeCapabilities));
		System.out.println("resource: " + JAXBUtil.silentMarshalWrap(resource));
		List<Object> capabilities = nativeCapabilities.getAny();
		assertFalse("Empty capabilities returned", capabilities.isEmpty());
		CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(capabilities,
				CredentialsCapabilityType.class);
		assertNotNull("password native capability not present", capCred.getPassword());
		ActivationCapabilityType capAct = ResourceTypeUtil.getCapability(capabilities,
				ActivationCapabilityType.class);
		assertNotNull("native activation capability not present", capAct);
		assertNotNull("native activation/enabledisable capability not present", capAct.getEnableDisable());
		TestConnectionCapabilityType capTest = ResourceTypeUtil.getCapability(capabilities,
				TestConnectionCapabilityType.class);
		assertNotNull("native test capability not present", capTest);
		ScriptCapabilityType capScript = ResourceTypeUtil.getCapability(capabilities,
				ScriptCapabilityType.class);
		assertNotNull("native script capability not present", capScript);
		assertNotNull("No host in native script capability", capScript.getHost());
		assertFalse("No host in native script capability", capScript.getHost().isEmpty());
		// TODO: better look inside

		// Check effective capabilites
		capCred = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
		assertNotNull("password capability not found", capCred.getPassword());
		// Although connector does not support activation, the resource
		// specifies a way how to simulate it.
		// Therefore the following should succeed
		capAct = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
		assertNotNull("activation capability not found", capCred.getPassword());

		List<Object> effectiveCapabilities = ResourceTypeUtil.listEffectiveCapabilities(resource);
		for (Object capability : effectiveCapabilities) {
			System.out.println("Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : "
					+ capability);
		}
	}

	@Test
	public void test010AddAccount() throws Exception {
		displayTestTile("test010AddAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test010AddAccount");

		AccountShadowType account = unmarshallJaxbFromFile(FILENAME_ACCOUNT, AccountShadowType.class);

		System.out.println(DebugUtil.prettyPrint(account));
		System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(account,
				SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

		// WHEN
		String addedObjectOid = provisioningService.addObject(account, null, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result);
		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, new PropertyReferenceListType(), result);
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
	}

	@Test
	public void test011GetAccount() throws ObjectNotFoundException, CommunicationException, SchemaException {
		displayTestTile("test011GetAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test011GetAccount");

		// WHEN
		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, null, result);

		// THEN
		display("Retrieved account shadow", accountType);

		assertNotNull("No dummy account", accountType);
		assertEquals(
				"Will Turner",
				ResourceObjectShadowUtil.getSingleStringAttributeValue(accountType,
						new QName(resource.getNamespace(), "fullname")));

		// TODO: check
	}

	@Test
	public void test012EnableAccount() throws FileNotFoundException, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException {
		displayTestTile("test012DisableAccount");
		// GIVEN

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test012DisableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, null, result);
		assertNotNull(accountType);

		// THEN
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertTrue(dummyAccount.isEnabled());

		ObjectModificationType objectModification = unmarshallJaxbFromFile(FILENAME_DISABLE_ACCOUNT,
				ObjectModificationType.class);
		System.out.println(DebugUtil.prettyPrint(objectModification));
		System.out.println("ObjectModification:");
		System.out.println(JAXBUtil.silentMarshalWrap(objectModification));

		provisioningService.modifyObject(AccountShadowType.class, objectModification, new ScriptsType(),
				result);

		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername("will");
		assertFalse(dummyAccount.isEnabled());
	}

	@Test
	public void test013DisableAccount() throws FileNotFoundException, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException {
		displayTestTile("test012EnableAccount");
		// GIVEN

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test012EnableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, null, result);
		assertNotNull(accountType);

		// THEN
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertFalse(dummyAccount.isEnabled());

		ObjectModificationType objectModification = unmarshallJaxbFromFile(FILENAME_ENABLE_ACCOUNT,
				ObjectModificationType.class);
		System.out.println(DebugUtil.prettyPrint(objectModification));
		System.out.println("ObjectModification:");
		System.out.println(JAXBUtil.silentMarshalWrap(objectModification));

		provisioningService.modifyObject(AccountShadowType.class, objectModification, new ScriptsType(),
				result);

		// check if activation was changed
		dummyAccount = dummyResource.getAccountByUsername("will");
		assertTrue(dummyAccount.isEnabled());
	}

	@Test
	public void test14AddScript() throws FileNotFoundException, JAXBException, ObjectAlreadyExistsException,
			SchemaException, CommunicationException, ObjectNotFoundException {
		displayTestTile("test14AddScript");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test14AddScript");

		AccountShadowType account = unmarshallJaxbFromFile(FILENAME_ACCOUNT_SCRIPT, AccountShadowType.class);

		System.out.println(DebugUtil.prettyPrint(account));
		System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(account,
				SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

		ScriptsType scriptsType = unmarshallJaxbFromFile(FILENAME_SCRIPT_ADD, ScriptsType.class);
		System.out.println(JAXBUtil.silentMarshalWrap(scriptsType));

		// WHEN
		String addedObjectOid = provisioningService.addObject(account, scriptsType, result);

	
		
		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_NEW_SCRIPT_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, new PropertyReferenceListType(), result);
		assertEquals("william", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, new PropertyReferenceListType(), result);
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
	public void test15ModifyScript() {

	}

	@Test
	public void test16DeleteScript() {

	}

}
