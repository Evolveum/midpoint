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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.EnhancedResourceType;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CachingMetadata;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.TestConnectionCapabilityType;

/**
 * The test of Provisioning service on the API level. The test is using dummy resource for speed and flexibility.
 * 
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml", "classpath:application-context-task.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext
public class ProvisioningServiceImplDummyTest extends AbstractIntegrationTest {

	private static final String FILENAME_RESOURCE_DUMMY = "src/test/resources/impl/resource-dummy.xml";
	private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	private static final String FILENAME_ACCOUNT = "src/test/resources/impl/account-dummy.xml";
	private static final String ACCOUNT_NEW_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
	private static final String ACCOUNT_NEW_ICF_UID = "will";
	private static final String FILENAME_ACCOUNT_SCRIPT = "src/test/resources/impl/account-dummy-script.xml";
	private static final String ACCOUNT_NEW_SCRIPT_OID = "c0c010c0-d34d-b44f-f11d-33322212abcd";
	private static final String FILENAME_ENABLE_ACCOUNT = "src/test/resources/impl/enable-account.xml";
	private static final String FILENAME_DISABLE_ACCOUNT = "src/test/resources/impl/disable-account.xml";
	private static final String FILENAME_SCRIPT_ADD = "src/test/resources/impl/script-add.xml";
	private static final String DUMMY_CONNECTOR_TYPE = "com.evolveum.icf.dummy.connector.DummyConnector";

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImplDummyTest.class);

	private ResourceType resource;
	private static DummyResource dummyResource;
	
	@Autowired(required=true)
	private ProvisioningService provisioningService;
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
				result).asObjectable();
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService
				.getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
		assertNotNull(connector);
		display("Dummy Connector", connector);
		
		// Check connector schema
		ProvisioningTestUtil.assertConnectorSchemaSanity(connector, prismContext);
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
		List<PrismObject<ConnectorType>> connectors = repositoryService.listObjects(ConnectorType.class, null, result);

		// THEN
		assertFalse("No connector found", connectors.isEmpty());
		for (PrismObject<ConnectorType> connPrism : connectors) {
			ConnectorType conn = connPrism.asObjectable();
			display("Found connector", conn);
			XmlSchemaType xmlSchemaType = conn.getSchema();
			assertNotNull("xmlSchemaType is null", xmlSchemaType);
			assertFalse("Empty schema", xmlSchemaType.getAny().isEmpty());
			// Try to parse the schema
			PrismSchema schema = PrismSchema.parse(xmlSchemaType.getAny().get(0), prismContext);
			assertNotNull("Cannot parse schema", schema);
			assertFalse("Empty schema", schema.isEmpty());
			display("Parsed connector schema", schema);
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
				null, result).asObjectable();
		assertNotNull("No connector ref", resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID", resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore
				.getConnectorRef().getOid(), null, result).asObjectable();
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		AssertJUnit.assertTrue("Found schema before test connection. Bad test setup?", xmlSchemaTypeBefore
				.getAny().isEmpty());

		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);

		// THEN
		display("Test result", testResult);
		assertSuccess("Test resource failed (result)", testResult);

		resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result).asObjectable();
		display("Resource after test", resource);

		XmlSchemaType xmlSchemaTypeAfter = resource.getSchema();
		assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
		assertFalse("No schema after test connection", xmlSchemaTypeAfter.getAny().isEmpty());

		CachingMetadata cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata", cachingMetadata);
		assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, prismContext);
		assertNotNull("No schema after parsing", parsedSchema);

		// schema will be checked in next test
	}

	@Test
	public void test004ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		displayTestTile("test004ParsedSchema");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test004ParsedSchema");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null,
				result).asObjectable();

		// THEN
		// The returned type should have the schema pre-parsed
		assertTrue(resource instanceof EnhancedResourceType);
		EnhancedResourceType enh = (EnhancedResourceType) resource;
		assertNotNull(enh.getParsedSchema());

		// Also test if the utility method returns the same thing
		ResourceSchema returnedSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);

		// Not equals() but == ... we want to really know if exactly the same
		// object instance is returned
		assertTrue(returnedSchema == enh.getParsedSchema());

		ObjectClassComplexTypeDefinition accountDef = returnedSchema.findDefaultAccountDefinition();
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		ResourceAttributeDefinition uidDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(1, uidDef.getMinOccurs());
		// assertFalse(StringUtils.isEmpty(uidDef.getDisplayName()));
		assertFalse(uidDef.canCreate());
		assertFalse(uidDef.canUpdate());
		assertTrue(uidDef.canRead());

		ResourceAttributeDefinition nameDef = accountDef
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		// assertFalse(StringUtils.isEmpty(nameDef.getDisplayName()));
		assertTrue(nameDef.canCreate());
		assertTrue(nameDef.canUpdate());
		assertTrue(nameDef.canRead());

		ResourceAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue(fullnameDef.canCreate());
		assertTrue(fullnameDef.canUpdate());
		assertTrue(fullnameDef.canRead());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));

	}

	@Test
	public void test005Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException, JAXBException, ConfigurationException {
		displayTestTile("test005Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test005Capabilities");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null,
				result).asObjectable();

		// THEN

		// Check native capabilities
		CapabilitiesType nativeCapabilities = resource.getNativeCapabilities();
		System.out.println("Native capabilities: " + PrismTestUtil.marshalWrap(nativeCapabilities));
		System.out.println("resource: " + resource.asPrismObject().dump());
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

		System.out.println(SchemaDebugUtil.prettyPrint(account));
		System.out.println(account.asPrismObject().dump());

		// WHEN
		String addedObjectOid = provisioningService.addObject(account.asPrismObject(), null, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		assertSuccess("addObject has failed (result)", result);
		assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

		AccountShadowType accountType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result).asObjectable();
		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, new PropertyReferenceListType(), result).asObjectable();
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
		
		checkConsistency();
	}

	@Test
	public void test011GetAccount() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		displayTestTile("test011GetAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test011GetAccount");

		// WHEN
		AccountShadowType shadow = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, null, result).asObjectable();

		// THEN
		display("Retrieved account shadow", shadow);

		assertNotNull("No dummy account", shadow);
		
		checkShadow(shadow);
		
		checkConsistency();
	}
	
	@Test
	public void test012SeachIterative() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		displayTestTile("test012SeachIterative");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test012SeachIterative");
		
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		query.setFilter(QueryUtil.createAndFilter(doc,
				QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF, RESOURCE_DUMMY_OID),
				QueryUtil.createEqualFilter(doc, null, SchemaConstants.I_OBJECT_CLASS, new QName(resource.getNamespace(),ConnectorFactoryIcfImpl.ACCOUNT_OBJECT_CLASS_LOCAL_NAME)),
				QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME, "will")));
		
		final List<AccountShadowType> foundObjects = new ArrayList<AccountShadowType>();
		ResultHandler<AccountShadowType> handler = new ResultHandler<AccountShadowType>() {
			
			@Override
			public boolean handle(PrismObject<AccountShadowType> object, OperationResult parentResult) {
				foundObjects.add(object.asObjectable());
				return true;
			}
		};
		
		// WHEN
		provisioningService.searchObjectsIterative(AccountShadowType.class, query, null, handler , result);
		
		// THEN
		
		assertEquals(1,foundObjects.size());
		
		AccountShadowType shadow = foundObjects.get(0);
		display("Found object",shadow);
		checkShadow(shadow);
		
		checkConsistency();
	}
		
	private void checkShadow(AccountShadowType shadow) {
		assertNotNull("no OID",shadow.getOid());
		assertNotNull("no name",shadow.getName());
		ResourceAttributeContainer attrs = ResourceObjectShadowUtil.getAttributesContainer(shadow);
		assertNotNull("no attributes",attrs);
		assertFalse("empty attributes",attrs.isEmpty());
		assertNotNull("no ICF UID",ResourceObjectShadowUtil.getSingleStringAttributeValue(shadow, ConnectorFactoryIcfImpl.ICFS_UID));
		assertNotNull("no ICF NAME",ResourceObjectShadowUtil.getSingleStringAttributeValue(shadow, ConnectorFactoryIcfImpl.ICFS_NAME));
		assertEquals("Will Turner",
				ResourceObjectShadowUtil.getSingleStringAttributeValue(shadow,
						new QName(resource.getNamespace(), "fullname")));
		assertNotNull("no activation",shadow.getActivation());
		assertNotNull("no activation/enabled",shadow.getActivation().isEnabled());
	}

	@Test
	public void test021EnableAccount() throws FileNotFoundException, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException {
		displayTestTile("test021EnableAccount");
		// GIVEN

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test021EnableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, null, result).asObjectable();
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
			SchemaException, CommunicationException, ConfigurationException {
		displayTestTile("test022EnableAccount");
		// GIVEN

		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test022EnableAccount");

		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_OID, null, result).asObjectable();
		assertNotNull(accountType);

		// THEN
		display("Retrieved account shadow", accountType);

		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertFalse(dummyAccount.isEnabled());

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
			SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException {
		displayTestTile("test031AddScript");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test031AddScript");

		AccountShadowType account = unmarshallJaxbFromFile(FILENAME_ACCOUNT_SCRIPT, AccountShadowType.class);

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
				ACCOUNT_NEW_SCRIPT_OID, new PropertyReferenceListType(), result).asObjectable();
		assertEquals("william", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class,
				ACCOUNT_NEW_SCRIPT_OID, new PropertyReferenceListType(), result).asObjectable();
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
	public void test100LiveSyncDryRun() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException {
		displayTestTile("test100LiveSyncDryRun");
		// GIVEN
		Task task = taskManager.createTaskInstance(ProvisioningServiceImplDummyTest.class.getName() + ".test100LiveSyncDryRun");
		OperationResult result = task.getResult();
		syncServiceMock.reset();
		
		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, task, result);
		
		// THEN
		
		// No change, no fun
		assertFalse(syncServiceMock.wasCalled());
		
	}

	@Test
	public void test101LiveSyncAdd() throws ObjectNotFoundException, CommunicationException, SchemaException, com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException, ConfigurationException {
		displayTestTile("test101LiveSyncAdd");
		// GIVEN
		Task task = taskManager.createTaskInstance(ProvisioningServiceImplDummyTest.class.getName() + ".test101LiveSyncAdd");
		OperationResult result = task.getResult();
		
		// Dry run to remember the current sync token in the task instance. Otherwise a last sync token whould be used and
		// no change would be detected
		provisioningService.synchronize(RESOURCE_DUMMY_OID, task, result);
		
		syncServiceMock.reset();
		dummyResource.setSyncStyle(DummySyncStyle.DUMB);
		DummyAccount newAccount = new DummyAccount("blackbeard");
		newAccount.addAttributeValues("fullnameame", "Edward Teach");
		newAccount.setEnabled(true);
		newAccount.setPassword("shiverMEtimbers");
		dummyResource.addAccount(newAccount);
		
		display("Resource before sync", dummyResource.dump());
		
		// WHEN
		provisioningService.synchronize(RESOURCE_DUMMY_OID, task, result);
		
		// THEN
		
		assertTrue("Sync service was not called", syncServiceMock.wasCalled());
		
		ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
//		assertNull("Old shadow present when not expecting it", lastChange.getOldShadow());
		assertNull("Delta present when not expecting it", lastChange.getObjectDelta());
		ResourceObjectShadowType currentShadowType = lastChange.getCurrentShadow().asObjectable();
		assertNotNull("Current shadow missing", lastChange.getCurrentShadow());
		assertTrue("Wrong type of current shadow: "+ currentShadowType.getClass().getName(), currentShadowType instanceof AccountShadowType);
		
	}

	
	private void checkConsistency() throws SchemaException {
		
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName() + ".checkConsistency");
		
		QueryType query = new QueryType();
		Document doc = DOMUtil.getDocument();
		XPathHolder xpath = new XPathHolder(SchemaConstants.I_ATTRIBUTES);
		query.setFilter(QueryUtil.createEqualFilter(doc, xpath, ConnectorFactoryIcfImpl.ICFS_UID, ACCOUNT_NEW_ICF_UID));
		
		ResultList<PrismObject<AccountShadowType>> objects = repositoryService.searchObjects(AccountShadowType.class, query , null, result);
		assertEquals("Wrong number of shadows for ICF UID " + ACCOUNT_NEW_ICF_UID, 1, objects.size());
		
	}

}
