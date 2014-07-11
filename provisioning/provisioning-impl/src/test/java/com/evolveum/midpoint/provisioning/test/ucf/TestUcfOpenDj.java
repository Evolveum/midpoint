/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.provisioning.test.ucf;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

/**
 * Test UCF implementation with OpenDJ and ICF LDAP connector.
 * <p/>
 * This test is using embedded OpenDJ as a resource and ICF LDAP connector. The
 * test is executed by direct calls to the UCF interface.
 * 
 * @author Radovan Semancik
 * @author Katka Valalikova
 *         <p/>
 *         This is an UCF test. It shold not need repository or other things
 *         from the midPoint spring context except from the provisioning beans.
 *         But due to a general issue with spring context initialization this is
 *         a lesser evil for now (MID-392)
 */
@ContextConfiguration(locations = { "classpath:ctx-provisioning-test-no-repo.xml" })
public class TestUcfOpenDj extends AbstractTestNGSpringContextTests {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/object/resource-opendj.xml";
	private static final String FILENAME_RESOURCE_OPENDJ_BAD = "src/test/resources/object/resource-opendj-bad.xml";
	private static final String FILENAME_CONNECTOR_LDAP = "src/test/resources/ucf/connector-ldap.xml";

	private ResourceType resourceType;
	private ResourceType badResourceType;
	private ConnectorType connectorType;
	private ConnectorFactory factory;
	private ConnectorInstance cc;
	private PrismSchema connectorSchema;
	private ResourceSchema resourceSchema;

	private static Trace LOGGER = TraceManager.getTrace(TestUcfOpenDj.class);

	@Autowired(required = true)
	ConnectorFactory connectorFactoryIcfImpl;
	@Autowired(required = true)
	Protector protector;
	@Autowired(required = true)
	PrismContext prismContext;

	protected static OpenDJController openDJController = new OpenDJController();

	public TestUcfOpenDj() throws JAXBException {
		System.setProperty("midpoint.home", "target/midPointHome/");
	}

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@BeforeClass
	public static void startLdap() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  OpenDjUcfTest");
		LOGGER.info("------------------------------------------------------------------------------");
		openDJController.startCleanServer();
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		openDJController.stop();
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  OpenDjUcfTest");
		LOGGER.info("------------------------------------------------------------------------------");
	}

	@BeforeMethod
	public void initUcf() throws Exception {
		TestUtil.displayTestTile("initUcf");

		File file = new File(FILENAME_RESOURCE_OPENDJ);
		FileInputStream fis = new FileInputStream(file);

		// Resource
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(FILENAME_RESOURCE_OPENDJ));
		resourceType = resource.asObjectable();

		// Resource: Second copy for negative test cases
		PrismObject<ResourceType> badResource = PrismTestUtil.parseObject(new File(FILENAME_RESOURCE_OPENDJ_BAD));
		badResourceType = badResource.asObjectable();

		// Connector
		PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(new File(FILENAME_CONNECTOR_LDAP));
		connectorType = connector.asObjectable();

		
		factory = connectorFactoryIcfImpl;

		cc = factory.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType),
				"test connector");
		AssertJUnit.assertNotNull("Cannot create connector instance", cc);
		
		connectorSchema = cc.generateConnectorSchema();
		AssertJUnit.assertNotNull("Cannot generate connector schema", cc);
		display("Connector schema", connectorSchema);
		
		OperationResult result = new OperationResult("initUcf");
		cc.configure(resourceType.getConnectorConfiguration().asPrismContainerValue(), result);
		cc.initialize(null, null, result);
		// TODO: assert something

		resourceSchema = cc.fetchResourceSchema(null, result);
		display("Resource schema", resourceSchema);

		AssertJUnit.assertNotNull(resourceSchema);

	}

	@AfterMethod
	public void shutdownUcf() throws Exception {
	}
	
	
	@Test
	public void testConnectorSchemaSanity() throws Exception {
		TestUtil.displayTestTile("testConnectorSchemaSanity");
	
		ProvisioningTestUtil.assertConnectorSchemaSanity(connectorSchema, "LDAP connector");		
	}

	
	@Test
	public void testResourceSchemaSanity() throws Exception {
		TestUtil.displayTestTile("testResourceSchemaSanity");
		
		QName objectClassQname = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
		assertNotNull("No object class definition " + objectClassQname, accountDefinition);
		assertEquals("Object class " + objectClassQname + " is not account", ShadowKindType.ACCOUNT, accountDefinition.getKind());
		assertTrue("Object class " + objectClassQname + " is not default account", accountDefinition.isDefaultInAKind());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isEmpty());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isIgnored());
		
		Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getIdentifiers();
		assertNotNull("Null identifiers for " + objectClassQname, identifiers);
		assertFalse("Empty identifiers for " + objectClassQname, identifiers.isEmpty());
		// TODO

		ResourceAttributeDefinition attributeDefinition = accountDefinition
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertNotNull("No definition for attribute "+ConnectorFactoryIcfImpl.ICFS_UID, attributeDefinition);
		assertTrue("Attribute "+ConnectorFactoryIcfImpl.ICFS_UID+" in not an identifier",attributeDefinition.isIdentifier(accountDefinition));
		assertTrue("Attribute "+ConnectorFactoryIcfImpl.ICFS_UID+" in not in identifiers list",identifiers.contains(attributeDefinition));
		
	}

	private Collection<ResourceAttribute<?>> addSampleResourceObject(String name, String givenName, String familyName)
			throws CommunicationException, GenericFrameworkException, SchemaException,
			ObjectAlreadyExistsException, ConfigurationException {
		OperationResult result = new OperationResult(this.getClass().getName() + ".testAdd");

		QName objectClassQname = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
		assertNotNull("No object class definition "+objectClassQname, accountDefinition);
		ResourceAttributeContainer resourceObject = accountDefinition.instantiate(ShadowType.F_ATTRIBUTES);

		ResourceAttributeDefinition attributeDefinition = accountDefinition
				.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		ResourceAttribute<String> attribute = attributeDefinition.instantiate();
		attribute.setValue(new PrismPropertyValue<String>("uid=" + name + ",ou=people,dc=example,dc=com"));
		resourceObject.add(attribute);

		attributeDefinition = accountDefinition
				.findAttributeDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "sn"));
		attribute = attributeDefinition.instantiate();
		attribute.setValue(new PrismPropertyValue(familyName));
		resourceObject.add(attribute);

		attributeDefinition = accountDefinition
				.findAttributeDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "cn"));
		attribute = attributeDefinition.instantiate();
		attribute.setValue(new PrismPropertyValue(givenName + " " + familyName));
		resourceObject.add(attribute);

		attributeDefinition = accountDefinition.findAttributeDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType),
				"givenName"));
		attribute = attributeDefinition.instantiate();
		attribute.setValue(new PrismPropertyValue(givenName));
		resourceObject.add(attribute);

		PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);

		Set<Operation> operation = new HashSet<Operation>();
		Collection<ResourceAttribute<?>> resourceAttributes = cc.addObject(shadow, operation, result);
		return resourceAttributes;
	}

	private String getEntryUuid(Collection<ResourceAttribute<?>> identifiers) {
		for (ResourceAttribute<?> identifier : identifiers) {
			if (identifier.getElementName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return identifier.getValue(String.class).getValue();
			}
		}
		return null;
	}

	@Test
	public void testAddDeleteObject() throws Exception {
		TestUtil.displayTestTile(this, "testDeleteObject");

		OperationResult result = new OperationResult(this.getClass().getName() + ".testDelete");

		Collection<ResourceAttribute<?>> identifiers = addSampleResourceObject("john", "John", "Smith");

		String uid = null;
		for (ResourceAttribute<?> resourceAttribute : identifiers) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(resourceAttribute.getElementName())) {
				uid = resourceAttribute.getValue(String.class).getValue();
				System.out.println("uuuuid:" + uid);
				assertNotNull(uid);
			}
		}

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);

		cc.deleteObject(accountDefinition, null, identifiers, result);

		PrismObject<ShadowType> resObj = null;
		try {
			resObj = cc.fetchObject(ShadowType.class, accountDefinition, identifiers, null,
					result);
			Assert.fail();
		} catch (ObjectNotFoundException ex) {
			AssertJUnit.assertNull(resObj);
		}

	}

	@Test
	public void testChangeModifyObject() throws Exception {
		TestUtil.displayTestTile(this, "testChangeModifyObject");

		OperationResult result = new OperationResult(this.getClass().getName() + ".testModify");

		Collection<ResourceAttribute<?>> identifiers = addSampleResourceObject("john", "John", "Smith");

		Set<Operation> changes = new HashSet<Operation>();

		changes.add(createAddAttributeChange("employeeNumber", "123123123"));
		changes.add(createReplaceAttributeChange("sn", "Smith007"));
		changes.add(createAddAttributeChange("street", "Wall Street"));
		changes.add(createDeleteAttributeChange("givenName", "John"));

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);

		cc.modifyObject(accountDefinition, identifiers, changes, result);

		PrismObject<ShadowType> shadow = cc.fetchObject(ShadowType.class, accountDefinition,
				identifiers, null, result);
		ResourceAttributeContainer resObj = ShadowUtil.getAttributesContainer(shadow);

		AssertJUnit.assertNull(resObj.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "givenName")));

		String addedEmployeeNumber = resObj
				.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "employeeNumber")).getValue(String.class)
				.getValue();
		String changedSn = resObj.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "sn"))
				.getValues(String.class).iterator().next().getValue();
		String addedStreet = resObj.findAttribute(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "street"))
				.getValues(String.class).iterator().next().getValue();

		System.out.println("changed employee number: " + addedEmployeeNumber);
		System.out.println("changed sn: " + changedSn);
		System.out.println("added street: " + addedStreet);

		AssertJUnit.assertEquals("123123123", addedEmployeeNumber);
		AssertJUnit.assertEquals("Smith007", changedSn);
		AssertJUnit.assertEquals("Wall Street", addedStreet);

	}

	@Test
	public void testFetchChanges() throws Exception {
		TestUtil.displayTestTile(this, "testFetchChanges");

		OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchChanges");
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		PrismProperty lastToken = cc.fetchCurrentToken(accountDefinition, result);

		System.out.println("Property:");
		System.out.println(SchemaDebugUtil.prettyPrint(lastToken));

		System.out.println("token " + lastToken.toString());
		List<Change<ShadowType>> changes = cc.fetchChanges(accountDefinition, lastToken, null, result);
		AssertJUnit.assertEquals(0, changes.size());
	}

	// This obviously does not work with LDAP connector
	@Test(enabled = false)
	public void testDisableAccount() throws Exception {
		TestUtil.displayTestTile(this, "testDisableAccount");

		// GIVEN
		OperationResult result = new OperationResult(this.getClass().getName() + ".testDisableAccount");

		Collection<ResourceAttribute<?>> identifiers = addSampleResourceObject("blackbeard", "Edward", "Teach");

		// Check precondition
		String entryUuid = getEntryUuid(identifiers);
		SearchResultEntry ldapEntryBefore = openDJController.searchAndAssertByEntryUuid(entryUuid);
		assertTrue("The account is not enabled", openDJController.isAccountEnabled(ldapEntryBefore));

		// WHEN

		Set<Operation> changes = new HashSet<Operation>();
		changes.add(createActivationChange(ActivationStatusType.DISABLED));

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);

		cc.modifyObject(accountDefinition, identifiers, changes, result);

		// THEN

		SearchResultEntry ldapEntryAfter = openDJController.searchAndAssertByEntryUuid(entryUuid);
		assertFalse("The account was not disabled", openDJController.isAccountEnabled(ldapEntryAfter));

	}

	private PrismProperty createProperty(String propertyName, String propertyValue) {
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass"));
		ResourceAttributeDefinition propertyDef = accountDefinition.findAttributeDefinition(new QName(
				ResourceTypeUtil.getResourceNamespace(resourceType), propertyName));
		ResourceAttribute property = propertyDef.instantiate();
		property.setValue(new PrismPropertyValue(propertyValue));
		return property;
	}

	private PropertyModificationOperation createReplaceAttributeChange(String propertyName, String propertyValue) {
		PrismProperty property = createProperty(propertyName, propertyValue);
		ItemPath propertyPath = new ItemPath(ShadowType.F_ATTRIBUTES, 
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), propertyName));
		PropertyDelta delta = new PropertyDelta(propertyPath, property.getDefinition(), prismContext);
		delta.setValueToReplace(new PrismPropertyValue(propertyValue));
		PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
		return attributeModification;
	}

	private PropertyModificationOperation createAddAttributeChange(String propertyName, String propertyValue) {
		PrismProperty property = createProperty(propertyName, propertyValue);
		ItemPath propertyPath = new ItemPath(ShadowType.F_ATTRIBUTES, 
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), propertyName));
		PropertyDelta delta = new PropertyDelta(propertyPath, property.getDefinition(), prismContext);
		delta.addValueToAdd(new PrismPropertyValue(propertyValue));
		PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
		return attributeModification;
	}

	private PropertyModificationOperation createDeleteAttributeChange(String propertyName, String propertyValue) {
		PrismProperty property = createProperty(propertyName, propertyValue);
		ItemPath propertyPath = new ItemPath(ShadowType.F_ATTRIBUTES, 
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), propertyName));
		PropertyDelta delta = new PropertyDelta(propertyPath, property.getDefinition(), prismContext);
		delta.addValueToDelete(new PrismPropertyValue(propertyValue));
		PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
		return attributeModification;
	}

	private PropertyModificationOperation createActivationChange(ActivationStatusType status) {
		PrismObjectDefinition<ShadowType> shadowDefinition = getShadowDefinition(ShadowType.class);
		PropertyDelta<ActivationStatusType> delta = PropertyDelta.createDelta(
				new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
				shadowDefinition);
		delta.setValueToReplace(new PrismPropertyValue<ActivationStatusType>(status));
		return new PropertyModificationOperation(delta);
	}

	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTestConnection() throws Exception {
		TestUtil.displayTestTile("testTestConnection");
		// GIVEN

		OperationResult result = new OperationResult("testTestConnection");

		// WHEN

		cc.test(result);

		// THEN
		result.computeStatus("test failed");
		AssertJUnit.assertNotNull(result);
		OperationResult connectorConnectionResult = result.getSubresults().get(0);
		AssertJUnit.assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: " + connectorConnectionResult);
		AssertJUnit.assertTrue(connectorConnectionResult.isSuccess());
		AssertJUnit.assertTrue(result.isSuccess());
	}

	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTestConnectionNegative() throws Exception {
		TestUtil.displayTestTile("testTestConnectionNegative");
		// GIVEN

		OperationResult result = new OperationResult("testTestConnectionNegative");

		ConnectorInstance badConnector = factory.createConnectorInstance(connectorType,
				ResourceTypeUtil.getResourceNamespace(badResourceType), "test connector");
		badConnector.configure(badResourceType.getConnectorConfiguration().asPrismContainerValue(), result);

		// WHEN

		badConnector.test(result);

		// THEN
		result.computeStatus("test failed");
		display("Test result (FAILURE EXPECTED)", result);
		AssertJUnit.assertNotNull(result);
		OperationResult connectorConnectionResult = result.getSubresults().get(1);
		AssertJUnit.assertNotNull(connectorConnectionResult);
		System.out.println("Test \"connector connection\" result: " + connectorConnectionResult
				+ " (FAILURE EXPECTED)");
		AssertJUnit.assertTrue("Unexpected success of bad connector test",
				!connectorConnectionResult.isSuccess());
		AssertJUnit.assertTrue(!result.isSuccess());
	}

	/**
	 * Test fetching and translating resource schema.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFetchResourceSchema() throws CommunicationException, SchemaException {
		TestUtil.displayTestTile("testFetchResourceSchema");
		// GIVEN

		// WHEN

		// The schema was fetched during test init. Now just check if it was OK.

		// THEN

		AssertJUnit.assertNotNull(resourceSchema);

		System.out.println(resourceSchema.debugDump());

		Document xsdSchema = resourceSchema.serializeToXsd();

		System.out
				.println("-------------------------------------------------------------------------------------");
		System.out.println(DOMUtil.printDom(xsdSchema));
		System.out
				.println("-------------------------------------------------------------------------------------");

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema
				.findObjectClassDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass"));
		AssertJUnit.assertNotNull(accountDefinition);

		AssertJUnit.assertFalse("No identifiers for account object class ", accountDefinition
				.getIdentifiers().isEmpty());

		PrismPropertyDefinition uidDefinition = accountDefinition
				.findPropertyDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		AssertJUnit.assertNotNull(uidDefinition);

		for (Definition def : resourceSchema.getDefinitions()) {
			if (def instanceof ResourceAttributeContainerDefinition) {
				ResourceAttributeContainerDefinition rdef = (ResourceAttributeContainerDefinition) def;
				assertNotEmpty("No type name in object class", rdef.getTypeName());
				assertNotEmpty("No native object class for " + rdef.getTypeName(),
						rdef.getNativeObjectClass());

				// This is maybe not that important, but just for a sake of
				// completeness
				assertNotEmpty("No name for " + rdef.getTypeName(), rdef.getName());
			}
		}

	}

	@Test
	public void testCapabilities() throws Exception {
		TestUtil.displayTestTile("testCapabilities");
		// GIVEN

		OperationResult result = new OperationResult("testCapabilities");

		// WHEN

		Collection<Object> capabilities = cc.fetchCapabilities(result);

		// THEN
		result.computeStatus("getCapabilities failed");
		TestUtil.assertSuccess("getCapabilities failed (result)", result);
		assertFalse("Empty capabilities returned", capabilities.isEmpty());
		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(capabilities,
				CredentialsCapabilityType.class);
		assertNotNull("password capability not present", capCred.getPassword());

	}

	@Test
	public void testFetchObject() throws Exception {
		TestUtil.displayTestTile("testFetchObject");

		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=Teell,ou=People,dc=example,dc=com", "Teell William", "Teell");

		OperationResult addResult = new OperationResult(this.getClass().getName() + ".testFetchObject");

		PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
		// Add a testing object
		cc.addObject(shadow, null, addResult);

		ObjectClassComplexTypeDefinition accountDefinition = resourceObject.getDefinition().getComplexTypeDefinition();

		Collection<ResourceAttribute<?>> identifiers = resourceObject.getIdentifiers();
		// Determine object class from the schema

		OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchObject");

		// WHEN
		PrismObject<ShadowType> ro = cc.fetchObject(ShadowType.class, accountDefinition,
				identifiers, null, result);

		// THEN

		AssertJUnit.assertNotNull(ro);
		System.out.println("Fetched object " + ro);
		System.out.println("Result:");
		System.out.println(result.debugDump());

	}

	@Test
	public void testSearch() throws UcfException, SchemaException, CommunicationException {
		TestUtil.displayTestTile("testSearch");
		// GIVEN

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		// Determine object class from the schema

		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {

			@Override
			public boolean handle(PrismObject<ShadowType> object) {
				System.out.println("Search: found: " + object);
				return true;
			}
		};

		OperationResult result = new OperationResult(this.getClass().getName() + ".testSearch");

		// WHEN
		cc.search(accountDefinition, new ObjectQuery(), handler, null, result);

		// THEN

	}

	@Test
	public void testCreateAccountWithPassword() throws Exception {
		TestUtil.displayTestTile("testCreateAccountWithPassword");
		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=lechuck,ou=people,dc=example,dc=com", "Ghost Pirate LeChuck", "LeChuck");

		Set<Operation> additionalOperations = new HashSet<Operation>();
		ProtectedStringType ps = protector.encryptString("t4k30v3rTh3W0rld");
		
//		PasswordChangeOperation passOp = new PasswordChangeOperation(ps);
//		additionalOperations.add(passOp);

		OperationResult addResult = new OperationResult(this.getClass().getName()
				+ ".testCreateAccountWithPassword");

		PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
		CredentialsType credentials = new CredentialsType();
		PasswordType pass = new PasswordType();
		pass.setValue(ps);
		credentials.setPassword(pass);
		shadow.asObjectable().setCredentials(credentials);
		
		// WHEN
		cc.addObject(shadow, additionalOperations, addResult);

		// THEN

		String entryUuid = (String) resourceObject.getIdentifier().getValue().getValue();
		SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
		display("Entry before change", entry);
		String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");

		assertNotNull(passwordAfter);

		System.out.println("Changed password: " + passwordAfter);

		// TODO
	}

	@Test
	public void testChangePassword() throws Exception {
		TestUtil.displayTestTile("testChangePassword");
		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=drake,ou=People,dc=example,dc=com", "Sir Francis Drake", "Drake");
		PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);

		OperationResult addResult = new OperationResult(this.getClass().getName() + ".testChangePassword");
		
		// Add a testing object
		cc.addObject(shadow, null, addResult);

		String entryUuid = (String) resourceObject.getIdentifier().getValue().getValue();
		SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
		display("Entry before change", entry);
		String passwordBefore = OpenDJController.getAttributeValue(entry, "userPassword");
		// We have set no password during create, therefore the password should
		// be empty
		assertNull(passwordBefore);

		ObjectClassComplexTypeDefinition accountDefinition = resourceObject.getDefinition().getComplexTypeDefinition();

		Collection<ResourceAttribute<?>> identifiers = resourceObject.getIdentifiers();
		// Determine object class from the schema

		OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchObject");

		// WHEN

		Set<Operation> changes = new HashSet<Operation>();
		ProtectedStringType passPs = protector.encryptString("salalala");
		
		ItemDeltaType propMod = new ItemDeltaType();
		//create modification path
		Document doc = DOMUtil.getDocument();
		ItemPathType path = new ItemPathType("credentials/password/value");
//		PropertyPath propPath = new PropertyPath(new PropertyPath(ResourceObjectShadowType.F_CREDENTIALS), CredentialsType.F_PASSWORD);
		propMod.setPath(path);

		//set the replace value
        MapXNode passPsXnode = prismContext.getBeanConverter().marshalProtectedDataType(passPs);
		RawType value = new RawType(passPsXnode, prismContext);
		propMod.getValue().add(value);
		
		//set the modificaion type
		propMod.setModificationType(ModificationTypeType.REPLACE);
		
		PropertyDelta passDelta = (PropertyDelta)DeltaConvertor.createItemDelta(propMod, shadow.getDefinition());
		PropertyModificationOperation passwordModification = new PropertyModificationOperation(passDelta);
		changes.add(passwordModification);
		
//		PasswordChangeOperation passwordChange = new PasswordChangeOperation(passPs);
//		changes.add(passwordChange);
		cc.modifyObject(accountDefinition, identifiers, changes, result);

		// THEN

		entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
		display("Entry after change", entry);

		String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");
		assertNotNull(passwordAfter);

		System.out.println("Account password: " + passwordAfter);
	}
	
	private ResourceAttributeContainer createResourceObject(String dn, String sn, String cn) throws SchemaException {
		// Account type is hardcoded now
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema
				.findObjectClassDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass"));
		// Determine identifier from the schema
		ResourceAttributeContainer resourceObject = accountDefinition.instantiate(ShadowType.F_ATTRIBUTES);

		ResourceAttributeDefinition road = accountDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "sn"));
		ResourceAttribute roa = road.instantiate();
		roa.setValue(new PrismPropertyValue(sn));
		resourceObject.add(roa);

		road = accountDefinition.findAttributeDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "cn"));
		roa = road.instantiate();
		roa.setValue(new PrismPropertyValue(cn));
		resourceObject.add(roa);

		road = accountDefinition.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		roa = road.instantiate();
		roa.setValue(new PrismPropertyValue(dn));
		resourceObject.add(roa);

		return resourceObject;
	}
	
	private <T extends ShadowType> PrismObject<T> wrapInShadow(Class<T> type, ResourceAttributeContainer resourceObject) throws SchemaException {
		PrismObjectDefinition<T> shadowDefinition = getShadowDefinition(type);
		PrismObject<T> shadow = shadowDefinition.instantiate();
		resourceObject.setElementName(ShadowType.F_ATTRIBUTES);
		shadow.getValue().add(resourceObject);
		return shadow;
	}
	
	private <T extends ShadowType> PrismObjectDefinition<T> getShadowDefinition(Class<T> type) { 
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
	}
}
