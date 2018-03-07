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
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;

import java.io.File;
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
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfOpenDj extends AbstractTestNGSpringContextTests {

	private static final File RESOURCE_OPENDJ_FILE = new File(UcfTestUtil.TEST_DIR, "resource-opendj.xml");
	private static final File RESOURCE_OPENDJ_BAD_FILE = new File(UcfTestUtil.TEST_DIR, "resource-opendj-bad.xml");
	private static final File CONNECTOR_LDAP_FILE = new File(UcfTestUtil.TEST_DIR, "connector-ldap.xml");

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
		TestUtil.displayTestTitle("initUcf");

		// Resource
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(RESOURCE_OPENDJ_FILE);
		resourceType = resource.asObjectable();

		// Resource: Second copy for negative test cases
		PrismObject<ResourceType> badResource = PrismTestUtil.parseObject(RESOURCE_OPENDJ_BAD_FILE);
		badResourceType = badResource.asObjectable();

		// Connector
		PrismObject<ConnectorType> connector = PrismTestUtil.parseObject(CONNECTOR_LDAP_FILE);
		connectorType = connector.asObjectable();

		factory = connectorFactoryIcfImpl;

		connectorSchema = factory.generateConnectorConfigurationSchema(connectorType);
		AssertJUnit.assertNotNull("Cannot generate connector schema", connectorSchema);
		display("Connector schema", connectorSchema);

		cc = factory.createConnectorInstance(connectorType, ResourceTypeUtil.getResourceNamespace(resourceType), "ldap connector");

		OperationResult result = new OperationResult("initUcf");
		cc.configure(resourceType.getConnectorConfiguration().asPrismContainerValue(), result);
		cc.initialize(null, null, false, result);
		// TODO: assert something

		resourceSchema = cc.fetchResourceSchema(null, result);
		display("Resource schema", resourceSchema);

		AssertJUnit.assertNotNull(resourceSchema);

	}

	@AfterMethod
	public void shutdownUcf() throws Exception {
	}


	@Test
	public void test010ConnectorSchemaSanity() throws Exception {
		final String TEST_NAME = "test010ConnectorSchemaSanity";
		TestUtil.displayTestTitle(TEST_NAME);

		IntegrationTestTools.assertConnectorSchemaSanity(connectorSchema, "LDAP connector", true);

		PrismContainerDefinition configurationDefinition =
				connectorSchema.findItemDefinition(ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(), PrismContainerDefinition.class);
		PrismContainerDefinition configurationPropertiesDefinition =
			configurationDefinition.findContainerDefinition(SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_PROPERTIES_ELEMENT_QNAME);

		PrismPropertyDefinition<String> propHost = configurationPropertiesDefinition.findPropertyDefinition(new QName(UcfTestUtil.CONNECTOR_LDAP_NS,"host"));
		assertNotNull("No definition for configuration property 'host' in connector schema", propHost);
		PrismAsserts.assertDefinition(propHost, new QName(UcfTestUtil.CONNECTOR_LDAP_NS,"host"), DOMUtil.XSD_STRING, 1, 1);
		assertEquals("Wrong property 'host' display name", "Host", propHost.getDisplayName());
		assertEquals("Wrong property 'host' help", "The name or IP address of the LDAP server host.", propHost.getHelp());
		assertEquals("Wrong property 'host' display order", (Integer)1, propHost.getDisplayOrder()); // MID-2642

		PrismPropertyDefinition<String> propPort = configurationPropertiesDefinition.findPropertyDefinition(new QName(UcfTestUtil.CONNECTOR_LDAP_NS,"port"));
		assertNotNull("No definition for configuration property 'port' in connector schema", propPort);
		PrismAsserts.assertDefinition(propPort, new QName(UcfTestUtil.CONNECTOR_LDAP_NS,"port"), DOMUtil.XSD_INT, 0, 1);
		assertEquals("Wrong property 'port' display name", "Port number", propPort.getDisplayName());
		assertEquals("Wrong property 'port' help", "LDAP server port number.", propPort.getHelp());
		assertEquals("Wrong property 'port' display order", (Integer)2, propPort.getDisplayOrder()); // MID-2642
	}


	@Test
	public void test020ResourceSchemaSanity() throws Exception {
		final String TEST_NAME = "test020ResourceSchemaSanity";
		TestUtil.displayTestTitle(TEST_NAME);

		QName objectClassQname = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME);
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
		assertNotNull("No object class definition " + objectClassQname, accountDefinition);
//		assertEquals("Object class " + objectClassQname + " is not account", ShadowKindType.ACCOUNT, accountDefinition.getKind());
//		assertTrue("Object class " + objectClassQname + " is not default account", accountDefinition.isDefaultInAKind());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isEmpty());
		assertFalse("Object class " + objectClassQname + " is empty", accountDefinition.isIgnored());

		Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getPrimaryIdentifiers();
		assertNotNull("Null identifiers for " + objectClassQname, identifiers);
		assertFalse("Empty identifiers for " + objectClassQname, identifiers.isEmpty());

		ResourceAttributeDefinition<String> idPrimaryDef = accountDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME));
		assertNotNull("No definition for attribute "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME, idPrimaryDef);
		assertTrue("Attribute "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" in not an identifier",idPrimaryDef.isIdentifier(accountDefinition));
		assertTrue("Attribute "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" in not in identifiers list",identifiers.contains(idPrimaryDef));
		assertEquals("Attribute "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" has wrong native name", OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME, idPrimaryDef.getNativeAttributeName());
		assertEquals("Attribute "+OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME+" has wrong framework name", Uid.NAME, idPrimaryDef.getFrameworkAttributeName());

		ResourceAttributeDefinition<String> idSecondaryDef = accountDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME));
		assertNotNull("No definition for attribute "+SchemaConstants.ICFS_NAME, idSecondaryDef);
		assertTrue("Attribute "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" in not secondary identifier",idSecondaryDef.isSecondaryIdentifier(accountDefinition));
		assertFalse("Attribute "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" in in identifiers list and it should NOT be",identifiers.contains(idSecondaryDef));
		assertTrue("Attribute "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" in not in secomdary identifiers list",accountDefinition.getSecondaryIdentifiers().contains(idSecondaryDef));
		assertEquals("Attribute "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" has wrong native name", OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME, idSecondaryDef.getNativeAttributeName());
		assertEquals("Attribute "+OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME+" has wrong framework name", Name.NAME, idSecondaryDef.getFrameworkAttributeName());

		assertEquals("Unexpected identifiers: "+identifiers, 1, identifiers.size());
		assertEquals("Unexpected secondary identifiers: "+accountDefinition.getSecondaryIdentifiers(), 1, accountDefinition.getSecondaryIdentifiers().size());
	}

	private Collection<ResourceAttribute<?>> addSampleResourceObject(String name, String givenName, String familyName)
			throws CommunicationException, GenericFrameworkException, SchemaException,
			ObjectAlreadyExistsException, ConfigurationException {
		OperationResult result = new OperationResult(this.getClass().getName() + ".testAdd");

		QName objectClassQname = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME);
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(objectClassQname);
		assertNotNull("No object class definition "+objectClassQname, accountDefinition);
		ResourceAttributeContainer resourceObject = accountDefinition.instantiate(ShadowType.F_ATTRIBUTES);

		ResourceAttributeDefinition<String> attributeDefinition = accountDefinition
				.findAttributeDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME));
		ResourceAttribute<String> attribute = attributeDefinition.instantiate();
		attribute.setValue(new PrismPropertyValue<>("uid=" + name + ",ou=people,dc=example,dc=com"));
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

		Set<Operation> operation = new HashSet<>();
		AsynchronousOperationReturnValue<Collection<ResourceAttribute<?>>> ret = cc.addObject(shadow, operation, null, result);
		Collection<ResourceAttribute<?>> resourceAttributes = ret.getReturnValue();
		return resourceAttributes;
	}

	private String getEntryUuid(Collection<ResourceAttribute<?>> identifiers) {
		for (ResourceAttribute<?> identifier : identifiers) {
			if (identifier.getElementName().equals(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME))) {
				return identifier.getValue(String.class).getValue();
			}
		}
		return null;
	}

	@Test
	public void test100AddDeleteObject() throws Exception {
		final String TEST_NAME = "test100AddDeleteObject";
		TestUtil.displayTestTitle(this, TEST_NAME);

		OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		Collection<ResourceAttribute<?>> identifiers = addSampleResourceObject("john", "John", "Smith");

		String uid = null;
		for (ResourceAttribute<?> resourceAttribute : identifiers) {
			if (SchemaConstants.ICFS_UID.equals(resourceAttribute.getElementName())) {
				uid = resourceAttribute.getValue(String.class).getValue();
				System.out.println("uuuuid:" + uid);
				assertNotNull(uid);
			}
		}

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME);

		cc.deleteObject(accountDefinition, null, identifiers, null, result);

		ResourceObjectIdentification identification = ResourceObjectIdentification.createFromAttributes(
				accountDefinition, identifiers);
		PrismObject<ShadowType> resObj = null;
		try {
			resObj = cc.fetchObject(identification, null, null,
					result);
			Assert.fail();
		} catch (ObjectNotFoundException ex) {
			AssertJUnit.assertNull(resObj);
		}

	}

	@Test
	public void test110ChangeModifyObject() throws Exception {
		final String TEST_NAME = "test110ChangeModifyObject";
		TestUtil.displayTestTitle(this, TEST_NAME);

		OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		Collection<ResourceAttribute<?>> identifiers = addSampleResourceObject("john", "John", "Smith");

		Set<Operation> changes = new HashSet<>();

		changes.add(createAddAttributeChange("employeeNumber", "123123123"));
		changes.add(createReplaceAttributeChange("sn", "Smith007"));
		changes.add(createAddAttributeChange("street", "Wall Street"));
		changes.add(createDeleteAttributeChange("givenName", "John"));

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME);

		cc.modifyObject(accountDefinition, identifiers, changes, null, result);

		ResourceObjectIdentification identification = ResourceObjectIdentification.createFromAttributes(
				accountDefinition, identifiers);
		PrismObject<ShadowType> shadow = cc.fetchObject(identification, null, null, result);
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
	public void test200FetchChanges() throws Exception {
		final String TEST_NAME = "test200FetchChanges";
		TestUtil.displayTestTitle(this, TEST_NAME);

		OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME);
		PrismProperty<Integer> lastToken = cc.fetchCurrentToken(accountDefinition, null, result);

		System.out.println("Property:");
		System.out.println(SchemaDebugUtil.prettyPrint(lastToken));
		System.out.println("token " + lastToken.toString());

		assertNotNull("No last token", lastToken);
		assertNotNull("No last token value", lastToken.getRealValue());

		List<Change> changes = cc.fetchChanges(accountDefinition, lastToken, null, null, result);
		display("Changes", changes);

		// Just one pseudo-change that updates the token
		AssertJUnit.assertEquals(1, changes.size());
		Change change = changes.get(0);
		assertNull(change.getCurrentShadow());
		assertNull(change.getIdentifiers());
		assertNull(change.getObjectDelta());
		assertNotNull(change.getToken());
	}

	private PrismProperty createProperty(String propertyName, String propertyValue) {
		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME));
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
		delta.setValueToReplace(new PrismPropertyValue<>(status));
		return new PropertyModificationOperation(delta);
	}

	/**
	 * Simple call to connector test() method.
	 *
	 * @throws Exception
	 */
	@Test
	public void test300TestConnection() throws Exception {
		final String TEST_NAME = "test300TestConnection";
		TestUtil.displayTestTitle(this, TEST_NAME);
		// GIVEN

		OperationResult result = new OperationResult(TEST_NAME);

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
	public void test310TestConnectionNegative() throws Exception {
		final String TEST_NAME = "test310TestConnectionNegative";
		TestUtil.displayTestTitle(this, TEST_NAME);
		// GIVEN

		OperationResult result = new OperationResult(TEST_NAME);

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
	public void test400FetchResourceSchema() throws Exception {
		final String TEST_NAME = "test400FetchResourceSchema";
		TestUtil.displayTestTitle(this, TEST_NAME);
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
				.findObjectClassDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME));
		AssertJUnit.assertNotNull(accountDefinition);

		AssertJUnit.assertFalse("No identifiers for account object class ", accountDefinition
				.getPrimaryIdentifiers().isEmpty());

		PrismPropertyDefinition uidDefinition = accountDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME));
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
	public void test410Capabilities() throws Exception {
		final String TEST_NAME = "test410Capabilities";
		TestUtil.displayTestTitle(this, TEST_NAME);
		// GIVEN

		OperationResult result = new OperationResult(TEST_NAME);

		// WHEN

		Collection<Object> capabilities = cc.fetchCapabilities(result);

		// THEN
		result.computeStatus("getCapabilities failed");
		TestUtil.assertSuccess("getCapabilities failed (result)", result);
		assertFalse("Empty capabilities returned", capabilities.isEmpty());
		CredentialsCapabilityType capCred = CapabilityUtil.getCapability(capabilities,
				CredentialsCapabilityType.class);
		assertNotNull("password capability not present", capCred.getPassword());

		PagedSearchCapabilityType capPage = CapabilityUtil.getCapability(capabilities, PagedSearchCapabilityType.class);
		assertNotNull("paged search capability not present", capPage);
	}

	@Test
	public void test500FetchObject() throws Exception {
		final String TEST_NAME = "test500FetchObject";
		TestUtil.displayTestTitle(this, TEST_NAME);

		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=Teell,ou=People,dc=example,dc=com", "Teell William", "Teell");

		OperationResult addResult = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
		// Add a testing object
		cc.addObject(shadow, null, null, addResult);

		ObjectClassComplexTypeDefinition accountDefinition = resourceObject.getDefinition().getComplexTypeDefinition();

		Collection<ResourceAttribute<?>> identifiers = resourceObject.getPrimaryIdentifiers();
		// Determine object class from the schema

		ResourceObjectIdentification identification = new ResourceObjectIdentification(accountDefinition, identifiers, null);
		OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		// WHEN
		PrismObject<ShadowType> ro = cc.fetchObject(identification, null, null, result);

		// THEN

		AssertJUnit.assertNotNull(ro);
		System.out.println("Fetched object " + ro);
		System.out.println("Result:");
		System.out.println(result.debugDump());

	}

	@Test
	public void test510Search() throws Exception {
		final String TEST_NAME = "test510Search";
		TestUtil.displayTestTitle(this, TEST_NAME);
		// GIVEN

		ObjectClassComplexTypeDefinition accountDefinition = resourceSchema.findObjectClassDefinition(OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME);
		// Determine object class from the schema

		ShadowResultHandler handler = new ShadowResultHandler() {
			@Override
			public boolean handle(PrismObject<ShadowType> object) {
				System.out.println("Search: found: " + object);
				return true;
			}
		};

		OperationResult result = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		// WHEN
		cc.search(accountDefinition, new ObjectQuery(), handler, null, null, null, null, result);

		// THEN

	}

	@Test
	public void test600CreateAccountWithPassword() throws Exception {
		final String TEST_NAME = "test600CreateAccountWithPassword";
		TestUtil.displayTestTitle(this, TEST_NAME);
		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=lechuck,ou=people,dc=example,dc=com", "Ghost Pirate LeChuck", "LeChuck");

		Set<Operation> additionalOperations = new HashSet<>();
		ProtectedStringType ps = protector.encryptString("t4k30v3rTh3W0rld");

//		PasswordChangeOperation passOp = new PasswordChangeOperation(ps);
//		additionalOperations.add(passOp);

		OperationResult addResult = new OperationResult(this.getClass().getName()
				+ "." + TEST_NAME);

		PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);
		CredentialsType credentials = new CredentialsType();
		PasswordType pass = new PasswordType();
		pass.setValue(ps);
		credentials.setPassword(pass);
		shadow.asObjectable().setCredentials(credentials);

		// WHEN
		cc.addObject(shadow, additionalOperations, null, addResult);

		// THEN

		String entryUuid = (String) resourceObject.getPrimaryIdentifier().getValue().getValue();
		Entry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
		display("Entry before change", entry);
		String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");

		assertNotNull(passwordAfter);

		System.out.println("Changed password: " + passwordAfter);

		// TODO
	}

	@Test
	public void test610ChangePassword() throws Exception {
		final String TEST_NAME = "test610ChangePassword";
		TestUtil.displayTestTitle(this, TEST_NAME);
		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=drake,ou=People,dc=example,dc=com", "Sir Francis Drake", "Drake");
		PrismObject<ShadowType> shadow = wrapInShadow(ShadowType.class, resourceObject);

		OperationResult addResult = new OperationResult(this.getClass().getName() + "." + TEST_NAME);

		// Add a testing object
		cc.addObject(shadow, null, null, addResult);

		String entryUuid = (String) resourceObject.getPrimaryIdentifier().getValue().getValue();
		Entry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
		display("Entry before change", entry);
		String passwordBefore = OpenDJController.getAttributeValue(entry, "userPassword");
		// We have set no password during create, therefore the password should
		// be empty
		assertNull(passwordBefore);

		ObjectClassComplexTypeDefinition accountDefinition = resourceObject.getDefinition().getComplexTypeDefinition();

		Collection<ResourceAttribute<?>> identifiers = resourceObject.getPrimaryIdentifiers();
		// Determine object class from the schema

		OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchObject");

		// WHEN

		Set<Operation> changes = new HashSet<>();
		ProtectedStringType passPs = protector.encryptString("salalala");

		ItemDeltaType propMod = new ItemDeltaType();
		//create modification path
		Document doc = DOMUtil.getDocument();
		ItemPathType path = new ItemPathType("credentials/password/value");
//		PropertyPath propPath = new PropertyPath(new PropertyPath(ResourceObjectShadowType.F_CREDENTIALS), CredentialsType.F_PASSWORD);
		propMod.setPath(path);

		//set the replace value
        MapXNode passPsXnode = ((PrismContextImpl) prismContext).getBeanMarshaller().marshalProtectedDataType(passPs, null);
		RawType value = new RawType(passPsXnode, prismContext);
		propMod.getValue().add(value);

		//set the modificaion type
		propMod.setModificationType(ModificationTypeType.REPLACE);

		PropertyDelta passDelta = (PropertyDelta)DeltaConvertor.createItemDelta(propMod, shadow.getDefinition());
		PropertyModificationOperation passwordModification = new PropertyModificationOperation(passDelta);
		changes.add(passwordModification);

//		PasswordChangeOperation passwordChange = new PasswordChangeOperation(passPs);
//		changes.add(passwordChange);
		cc.modifyObject(accountDefinition, identifiers, changes, null, result);

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
				.findObjectClassDefinition(new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.OBJECT_CLASS_INETORGPERSON_NAME));
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

		road = accountDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceType), OpenDJController.RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME));
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
