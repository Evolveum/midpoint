/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.framework.impl.api.ConnectorFacadeFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
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
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CachingMetadata;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

/**
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class ProvisioningServiceImplDummyTest extends AbstractIntegrationTest {
	
	private static final String FILENAME_RESOURCE_DUMMY = "src/test/resources/impl/resource-dummy.xml";
	private static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
	private static final String FILENAME_ACCOUNT = "src/test/resources/impl/account-dummy.xml";
	private static final String ACCOUNT_NEW_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
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

	/* (non-Javadoc)
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
		
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()+".test000Integrity");
		
		ResourceType resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		String connectorOid = resource.getConnectorRef().getOid();
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, result);
		assertNotNull(connector);
		display("Dummy Connector",connector);
	}
	
	/**
	 * Check whether the connectors were discovered correctly and were added to the repository.
	 * @throws SchemaProcessorException 
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
		displayTestTile("test001Connectors");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test001Connectors");
		
		// WHEN
		List<ConnectorType> connectors = repositoryService.listObjects(ConnectorType.class, null, result);
		
		// THEN
		assertFalse("No connector found",connectors.isEmpty());
		for (ConnectorType conn : connectors) {
			display("Found connector",conn);
			XmlSchemaType xmlSchemaType = conn.getSchema();
			assertNotNull("xmlSchemaType is null",xmlSchemaType);
			assertFalse("Empty schema",xmlSchemaType.getAny().isEmpty());
			// Try to parse the schema
			Schema schema = Schema.parse(xmlSchemaType.getAny().get(0));
			assertNotNull("Cannot parse schema",schema);
			assertFalse("Empty schema",schema.isEmpty());
			display("Parsed connector schema",schema);
			PropertyContainerDefinition definition = schema.findItemDefinition("configuration",PropertyContainerDefinition.class);
			assertNotNull("Definition of <configuration> property container not found",definition);
			PropertyContainerDefinition pcd = (PropertyContainerDefinition)definition;
			assertFalse("Empty definition",pcd.isEmpty());
		}
	}
	
	/**
	 * Running discovery for a second time should return nothing - as nothing new was installed in the
	 * meantime.
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
		assertTrue("Rediscovered something",discoverLocalConnectors.isEmpty());
	}
	
	
	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema should be generated from
	 * the resource on the first use. This is the test that executes testResource and checks whether the
	 * schema was generated.
	 */
	@Test
	public void test003Connection() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test003Connection");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test003Connection");
		// Check that there is no schema before test (pre-condition)
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class,RESOURCE_DUMMY_OID, null, result);
		assertNotNull("No connector ref",resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID",resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore.getConnectorRef().getOid(), null, result);
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		AssertJUnit.assertTrue("Found schema before test connection. Bad test setup?",xmlSchemaTypeBefore.getAny().isEmpty());
		
		// WHEN
		OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID);
		
		// THEN
		display("Test result",testResult);
		assertSuccess("Test resource failed (result)", testResult);

		resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		display("Resource after test",resource);
		
		XmlSchemaType xmlSchemaTypeAfter = resource.getSchema();
		assertNotNull("No schema after test connection",xmlSchemaTypeAfter);
		assertFalse("No schema after test connection",xmlSchemaTypeAfter.getAny().isEmpty());
		
		CachingMetadata cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata",cachingMetadata);
		assertNotNull("No retrievalTimestamp",cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber",cachingMetadata.getSerialNumber());
		
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		Schema parsedSchema = Schema.parse(xsdElement);
		assertNotNull("No schema after parsing",parsedSchema);
		
		// schema will be checked in next test
	}
	
	@Test
	public void test004ParsedSchema() throws ObjectNotFoundException, CommunicationException, SchemaException {
		displayTestTile("test004ParsedSchema");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test004ParsedSchema");
		
		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
		
		// THEN
		// The returned type should have the schema pre-parsed
		assertTrue(resource instanceof EnhancedResourceType);
		EnhancedResourceType enh = (EnhancedResourceType) resource;
		assertNotNull(enh.getParsedSchema());
		
		// Also test if the utility method returns the same thing		
		Schema returnedSchema = ResourceTypeUtil.getResourceSchema(resource);
		
		// Not equals() but == ... we want to really know if exactly the same object instance is returned
		assertTrue(returnedSchema == enh.getParsedSchema());

		ResourceObjectDefinition accountDef = returnedSchema.findAccountDefinition();
		assertNotNull("Account definition is missing",accountDef);
		assertNotNull("Null identifiers in account",accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account",accountDef.getIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));
		
		ResourceObjectAttributeDefinition uidDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1,uidDef.getMaxOccurs());
		assertEquals(1,uidDef.getMinOccurs());
//		assertFalse(StringUtils.isEmpty(uidDef.getDisplayName()));
		assertFalse(uidDef.canCreate());
		assertFalse(uidDef.canUpdate());
		assertTrue(uidDef.canRead());
		
		ResourceObjectAttributeDefinition nameDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1,nameDef.getMaxOccurs());
		assertEquals(1,nameDef.getMinOccurs());
//		assertFalse(StringUtils.isEmpty(nameDef.getDisplayName()));
		assertTrue(nameDef.canCreate());
		assertTrue(nameDef.canUpdate());
		assertTrue(nameDef.canRead());
		
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
		display("add object result",result);
		assertSuccess("addObject has failed (result)",result);
		assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

		AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result);
		assertEquals("will", accountType.getName());

		AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
				new PropertyReferenceListType(), result);
		assertEquals("will", provisioningAccountType.getName());
		
		// Check if the account was created in the dummy resource
		
		DummyAccount dummyAccount = dummyResource.getAccountByUsername("will");
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", "Will Turner", dummyAccount.getAttributeValue("fullname"));
		assertTrue("The account is not enabled",dummyAccount.isEnabled());
		assertEquals("Wrong password", "3lizab3th", dummyAccount.getPassword());
	}
	
	@Test
	public void test011GetAccount() throws ObjectNotFoundException, CommunicationException, SchemaException {
		displayTestTile("test011GetAccount");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplDummyTest.class.getName()
				+ ".test011GetAccount");
		
		// WHEN
		AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID, null, result);
		
		// THEN
		display("Retrieved account shadow",accountType);

		assertNotNull("No dummy account", accountType);
		assertEquals("Will Turner", ResourceObjectShadowUtil.getSingleAttributeValue(accountType, 
				new QName(resource.getNamespace(), "fullname")));

		// TODO: check
	}
}
