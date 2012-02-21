/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.test.ucf;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.*;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileInputStream;
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
@ContextConfiguration(locations = { "classpath:application-context-provisioning-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml" })
public class OpenDjUcfTest extends AbstractTestNGSpringContextTests {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String FILENAME_RESOURCE_OPENDJ_BAD = "src/test/resources/ucf/opendj-resource-bad.xml";
	private static final String FILENAME_CONNECTOR_LDAP = "src/test/resources/ucf/ldap-connector.xml";

	private JAXBContext jaxbctx;
	ResourceType resource;
	ResourceType badResource;
	ConnectorType connectorType;
	private ConnectorFactory factory;
	private ConnectorInstance cc;
	ResourceSchema schema;

	private static Trace LOGGER = TraceManager.getTrace(OpenDjUcfTest.class);

	@Autowired(required = true)
	ConnectorFactory connectorFactoryIcfImpl;
	@Autowired(required = true)
	Protector protector;
	@Autowired(required = true)
	PrismContext prismContext;

	protected static OpenDJController openDJController = new OpenDJController();

	public OpenDjUcfTest() throws JAXBException {
		System.setProperty("midpoint.home", "target/midPointHome/");
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
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

		File file = new File(FILENAME_RESOURCE_OPENDJ);
		FileInputStream fis = new FileInputStream(file);

		// Resource
		Unmarshaller u = jaxbctx.createUnmarshaller();
		Object object = u.unmarshal(fis);
		resource = (ResourceType) ((JAXBElement) object).getValue();

		// Resource: Second copy for negative test cases
		file = new File(FILENAME_RESOURCE_OPENDJ_BAD);
		fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		badResource = (ResourceType) ((JAXBElement) object).getValue();

		// Connector
		file = new File(FILENAME_CONNECTOR_LDAP);
		fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		connectorType = (ConnectorType) ((JAXBElement) object).getValue();

		factory = connectorFactoryIcfImpl;

		cc = factory.createConnectorInstance(connectorType, resource.getNamespace());
		AssertJUnit.assertNotNull(cc);
		OperationResult result = new OperationResult("initUcf");
		cc.configure(resource.getConfiguration().asPrismContainer(), result);
		cc.initialize(result);
		// TODO: assert something

		schema = cc.getResourceSchema(result);

		AssertJUnit.assertNotNull(schema);

	}

	@AfterMethod
	public void shutdownUcf() throws Exception {
	}

	private Set<ResourceAttribute> addSampleResourceObject(String name, String givenName, String familyName)
			throws CommunicationException, GenericFrameworkException, SchemaException,
			ObjectAlreadyExistsException {
		OperationResult result = new OperationResult(this.getClass().getName() + ".testAdd");

		ResourceAttributeContainerDefinition accountDefinition = (ResourceAttributeContainerDefinition) schema
				.findContainerDefinitionByType(new QName(resource.getNamespace(), "AccountObjectClass"));
		ResourceAttributeContainer resourceObject = accountDefinition.instantiate();

		PrismPropertyDefinition propertyDefinition = accountDefinition
				.findPropertyDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		PrismProperty property = propertyDefinition.instantiate(null);
		property.setValue(new PrismPropertyValue("uid=" + name + ",ou=people,dc=example,dc=com"));
		resourceObject.add(property);

		propertyDefinition = accountDefinition
				.findPropertyDefinition(new QName(resource.getNamespace(), "sn"));
		property = propertyDefinition.instantiate();
		property.setValue(new PrismPropertyValue(familyName));
		resourceObject.add(property);

		propertyDefinition = accountDefinition
				.findPropertyDefinition(new QName(resource.getNamespace(), "cn"));
		property = propertyDefinition.instantiate(null);
		property.setValue(new PrismPropertyValue(givenName + " " + familyName));
		resourceObject.add(property);

		propertyDefinition = accountDefinition.findPropertyDefinition(new QName(resource.getNamespace(),
				"givenName"));
		property = propertyDefinition.instantiate(null);
		property.setValue(new PrismPropertyValue(givenName));
		resourceObject.add(property);

		PrismObject<AccountShadowType> shadow = wrapInShadow(AccountShadowType.class, resourceObject);

		Set<Operation> operation = new HashSet<Operation>();
		Set<ResourceAttribute> resourceAttributes = cc.addObject(shadow, operation, result);
		return resourceAttributes;
	}

	private String getEntryUuid(Set<ResourceAttribute> identifiers) {
		for (ResourceAttribute<?> identifier : identifiers) {
			if (identifier.getName().equals(ConnectorFactoryIcfImpl.ICFS_UID)) {
				return identifier.getValue(String.class).getValue();
			}
		}
		return null;
	}

	@Test
	public void testAddDeleteObject() throws Exception {
		displayTestTile(this, "testDeleteObject");

		OperationResult result = new OperationResult(this.getClass().getName() + ".testDelete");

		Set<ResourceAttribute> identifiers = addSampleResourceObject("john", "John", "Smith");

		String uid = null;
		for (ResourceAttribute<?> resourceAttribute : identifiers) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(resourceAttribute.getName())) {
				uid = resourceAttribute.getValue(String.class).getValue();
				System.out.println("uuuuid:" + uid);
				assertNotNull(uid);
			}
		}

		ResourceAttributeContainerDefinition accountDefinition = schema.findDefaultAccountDefinition();

		cc.deleteObject(accountDefinition, null, identifiers, result);

		PrismObject<AccountShadowType> resObj = null;
		try {
			resObj = cc.fetchObject(AccountShadowType.class, accountDefinition, identifiers, true, null,
					result);
			Assert.fail();
		} catch (ObjectNotFoundException ex) {
			AssertJUnit.assertNull(resObj);
		}

	}

	@Test
	public void testChangeModifyObject() throws Exception {
		displayTestTile(this, "testChangeModifyObject");

		OperationResult result = new OperationResult(this.getClass().getName() + ".testModify");

		Set<ResourceAttribute> identifiers = addSampleResourceObject("john", "John", "Smith");

		Set<Operation> changes = new HashSet<Operation>();

		changes.add(createAddChange("employeeNumber", "123123123"));
		changes.add(createReplaceChange("sn", "Smith007"));
		changes.add(createAddChange("street", "Wall Street"));
		changes.add(createDeleteChange("givenName", "John"));

		ResourceAttributeContainerDefinition accountDefinition = schema.findDefaultAccountDefinition();

		cc.modifyObject(accountDefinition, identifiers, changes, result);

		PrismObject<AccountShadowType> shadow = cc.fetchObject(AccountShadowType.class, accountDefinition,
				identifiers, true, null, result);
		ResourceAttributeContainer resObj = ResourceObjectShadowUtil.getAttributesContainer(shadow);

		AssertJUnit.assertNull(resObj.findAttribute(new QName(resource.getNamespace(), "givenName")));

		String addedEmployeeNumber = resObj
				.findAttribute(new QName(resource.getNamespace(), "employeeNumber")).getValue(String.class)
				.getValue();
		String changedSn = resObj.findAttribute(new QName(resource.getNamespace(), "sn"))
				.getValue(String.class).getValue();
		String addedStreet = resObj.findAttribute(new QName(resource.getNamespace(), "street"))
				.getValue(String.class).getValue();

		System.out.println("changed employee number: " + addedEmployeeNumber);
		System.out.println("changed sn: " + changedSn);
		System.out.println("added street: " + addedStreet);

		AssertJUnit.assertEquals("123123123", addedEmployeeNumber);
		AssertJUnit.assertEquals("Smith007", changedSn);
		AssertJUnit.assertEquals("Wall Street", addedStreet);

	}

	@Test
	public void testFetchChanges() throws Exception {
		displayTestTile(this, "testFetchChanges");

		OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchChanges");
		ResourceAttributeContainerDefinition accountDefinition = schema.findDefaultAccountDefinition();
		PrismProperty lastToken = cc.fetchCurrentToken(accountDefinition, result);

		System.out.println("Property:");
		System.out.println(SchemaDebugUtil.prettyPrint(lastToken));

		System.out.println("token " + lastToken.toString());
		List<Change> changes = cc.fetchChanges(accountDefinition, lastToken, result);
		AssertJUnit.assertEquals(0, changes.size());
	}

	// This obviously does not work with LDAP connector
	@Test(enabled = false)
	public void testDisableAccount() throws Exception {
		displayTestTile(this, "testDisableAccount");

		// GIVEN
		OperationResult result = new OperationResult(this.getClass().getName() + ".testDisableAccount");

		Set<ResourceAttribute> identifiers = addSampleResourceObject("blackbeard", "Edward", "Teach");

		// Check precondition
		String entryUuid = getEntryUuid(identifiers);
		SearchResultEntry ldapEntryBefore = openDJController.searchAndAssertByEntryUuid(entryUuid);
		assertTrue("The account is not enabled", openDJController.isAccountEnabled(ldapEntryBefore));

		// WHEN

		Set<Operation> changes = new HashSet<Operation>();
		changes.add(createActivationChange(false));

		ResourceAttributeContainerDefinition accountDefinition = schema.findDefaultAccountDefinition();

		cc.modifyObject(accountDefinition, identifiers, changes, result);

		// THEN

		SearchResultEntry ldapEntryAfter = openDJController.searchAndAssertByEntryUuid(entryUuid);
		assertFalse("The account was not disabled", openDJController.isAccountEnabled(ldapEntryAfter));

	}

	private PrismProperty createProperty(String propertyName, String propertyValue) {
		ResourceAttributeContainerDefinition accountDefinition = (ResourceAttributeContainerDefinition) schema
				.findContainerDefinitionByType(new QName(resource.getNamespace(), "AccountObjectClass"));
		ResourceAttributeDefinition propertyDef = accountDefinition.findAttributeDefinition(new QName(
				resource.getNamespace(), propertyName));
		ResourceAttribute property = propertyDef.instantiate(null);
		property.setValue(new PrismPropertyValue(propertyValue));
		return property;
	}

	private PropertyModificationOperation createReplaceChange(String propertyName, String propertyValue) {
		PrismProperty property = createProperty(propertyName, propertyValue);
		PropertyDelta delta = new PropertyDelta(property.getDefinition());
		delta.setValueToReplace(new PrismPropertyValue(propertyValue));
		PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
		return attributeModification;
	}

	private PropertyModificationOperation createAddChange(String propertyName, String propertyValue) {
		PrismProperty property = createProperty(propertyName, propertyValue);
		PropertyDelta delta = new PropertyDelta(property.getDefinition());
		delta.addValueToAdd(new PrismPropertyValue(propertyValue));
		PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
		return attributeModification;
	}

	private PropertyModificationOperation createDeleteChange(String propertyName, String propertyValue) {
		PrismProperty property = createProperty(propertyName, propertyValue);
		PropertyDelta delta = new PropertyDelta(property.getDefinition());
		delta.addValueToDelete(new PrismPropertyValue(propertyValue));
		PropertyModificationOperation attributeModification = new PropertyModificationOperation(delta);
		return attributeModification;
	}

	private PropertyModificationOperation createActivationChange(boolean b) {
		PrismObjectDefinition<ResourceObjectShadowType> shadowDefinition = getShadowDefinition(ResourceObjectShadowType.class);
		PropertyDelta delta = PropertyDelta.createDelta(
				new PropertyPath(ResourceObjectShadowType.F_ACTIVATION, ActivationType.F_ENABLED),
				shadowDefinition);
		delta.setValueToReplace(new PrismPropertyValue(b));
		return new PropertyModificationOperation(delta);
	}

	/**
	 * Simple call to connector test() method.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTestConnection() throws Exception {
		displayTestTile("testTestConnection");
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
		displayTestTile("testTestConnectionNegative");
		// GIVEN

		OperationResult result = new OperationResult("testTestConnectionNegative");

		ConnectorInstance badConnector = factory.createConnectorInstance(connectorType,
				badResource.getNamespace());
		badConnector.configure(badResource.getConfiguration().asPrismContainer(), result);

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
		displayTestTile("testFetchResourceSchema");
		// GIVEN

		// WHEN

		// The schema was fetched during test init. Now just check if it was OK.

		// THEN

		AssertJUnit.assertNotNull(schema);

		System.out.println(schema.dump());

		Document xsdSchema = PrismSchema.serializeToXsd(schema);

		System.out
				.println("-------------------------------------------------------------------------------------");
		System.out.println(DOMUtil.printDom(xsdSchema));
		System.out
				.println("-------------------------------------------------------------------------------------");

		ResourceAttributeContainerDefinition accountDefinition = (ResourceAttributeContainerDefinition) schema
				.findContainerDefinitionByType(new QName(resource.getNamespace(), "AccountObjectClass"));
		AssertJUnit.assertNotNull(accountDefinition);

		AssertJUnit.assertFalse("No identifiers for account object class ", accountDefinition
				.getIdentifiers().isEmpty());

		PrismPropertyDefinition uidDefinition = accountDefinition
				.findPropertyDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		AssertJUnit.assertNotNull(uidDefinition);

		for (Definition def : schema.getDefinitions()) {
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
		displayTestTile("testCapabilities");
		// GIVEN

		OperationResult result = new OperationResult("testCapabilities");

		// WHEN

		Set<Object> capabilities = cc.getCapabilities(result);

		// THEN
		result.computeStatus("getCapabilities failed");
		assertSuccess("getCapabilities failed (result)", result);
		assertFalse("Empty capabilities returned", capabilities.isEmpty());
		CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(capabilities,
				CredentialsCapabilityType.class);
		assertNotNull("password capability not present", capCred.getPassword());

	}

	@Test
	public void testFetchObject() throws Exception {
		displayTestTile("testFetchObject");

		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=Teell,ou=People,dc=example,dc=com", "Teell William", "Teell");

		OperationResult addResult = new OperationResult(this.getClass().getName() + ".testFetchObject");

		PrismObject<AccountShadowType> shadow = wrapInShadow(AccountShadowType.class, resourceObject);
		// Add a testing object
		cc.addObject(shadow, null, addResult);

		ResourceAttributeContainerDefinition accountDefinition = resourceObject.getDefinition();

		Set<ResourceAttribute> identifiers = resourceObject.getIdentifiers();
		// Determine object class from the schema

		OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchObject");

		// WHEN
		PrismObject<AccountShadowType> ro = cc.fetchObject(AccountShadowType.class, accountDefinition,
				identifiers, true, null, result);

		// THEN

		AssertJUnit.assertNotNull(ro);
		System.out.println("Fetched object " + ro);
		System.out.println("Result:");
		System.out.println(result.dump());

	}

	@Test
	public void testSearch() throws UcfException, SchemaException {
		displayTestTile("testSearch");
		// GIVEN

		ResourceAttributeContainerDefinition accountDefinition = schema.findDefaultAccountDefinition();
		// Determine object class from the schema

		ResultHandler<AccountShadowType> handler = new ResultHandler<AccountShadowType>() {

			@Override
			public boolean handle(PrismObject<AccountShadowType> object) {
				System.out.println("Search: found: " + object);
				return true;
			}
		};

		OperationResult result = new OperationResult(this.getClass().getName() + ".testSearch");

		// WHEN
		cc.search(AccountShadowType.class, accountDefinition, handler, result);

		// THEN

	}

	@Test
	public void testCreateAccountWithPassword() throws CommunicationException, GenericFrameworkException,
			SchemaException, ObjectAlreadyExistsException, EncryptionException, DirectoryException {
		displayTestTile("testCreateAccountWithPassword");
		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=lechuck,ou=people,dc=example,dc=com", "Ghost Pirate LeChuck", "LeChuck");

		Set<Operation> additionalOperations = new HashSet<Operation>();
		ProtectedStringType ps = protector.encryptString("t4k30v3rTh3W0rld");
		PasswordChangeOperation passOp = new PasswordChangeOperation(ps);
		additionalOperations.add(passOp);

		OperationResult addResult = new OperationResult(this.getClass().getName()
				+ ".testCreateAccountWithPassword");

		PrismObject<AccountShadowType> shadow = wrapInShadow(AccountShadowType.class, resourceObject);
		
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
	public void testChangePassword() throws DirectoryException, CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException,
			ObjectNotFoundException, EncryptionException {
		displayTestTile("testChangePassword");
		// GIVEN
		ResourceAttributeContainer resourceObject = createResourceObject(
				"uid=drake,ou=People,dc=example,dc=com", "Sir Francis Drake", "Drake");
		PrismObject<AccountShadowType> shadow = wrapInShadow(AccountShadowType.class, resourceObject);

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

		ResourceAttributeContainerDefinition accountDefinition = resourceObject.getDefinition();

		Set<ResourceAttribute> identifiers = resourceObject.getIdentifiers();
		// Determine object class from the schema

		OperationResult result = new OperationResult(this.getClass().getName() + ".testFetchObject");

		// WHEN

		Set<Operation> changes = new HashSet<Operation>();
		ProtectedStringType passPs = protector.encryptString("x-m4rx-da-sp0t");
		PasswordChangeOperation passwordChange = new PasswordChangeOperation(passPs);
		changes.add(passwordChange);
		cc.modifyObject(accountDefinition, identifiers, changes, result);

		// THEN

		entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
		display("Entry after change", entry);

		String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");
		assertNotNull(passwordAfter);

		System.out.println("Account password: " + passwordAfter);
	}
	
	private ResourceAttributeContainer createResourceObject(String dn, String sn, String cn) {
		// Account type is hardcoded now
		ResourceAttributeContainerDefinition accountDefinition = (ResourceAttributeContainerDefinition) schema
				.findContainerDefinitionByType(new QName(resource.getNamespace(), "AccountObjectClass"));
		// Determine identifier from the schema
		ResourceAttributeContainer resourceObject = accountDefinition.instantiate();

		ResourceAttributeDefinition road = accountDefinition.findAttributeDefinition(new QName(resource
				.getNamespace(), "sn"));
		ResourceAttribute roa = road.instantiate(null);
		roa.setValue(new PrismPropertyValue(sn));
		resourceObject.add(roa);

		road = accountDefinition.findAttributeDefinition(new QName(resource.getNamespace(), "cn"));
		roa = road.instantiate(null);
		roa.setValue(new PrismPropertyValue(cn));
		resourceObject.add(roa);

		road = accountDefinition.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		roa = road.instantiate(null);
		roa.setValue(new PrismPropertyValue(dn));
		resourceObject.add(roa);

		return resourceObject;
	}
	
	private <T extends ResourceObjectShadowType> PrismObject<T> wrapInShadow(Class<T> type, ResourceAttributeContainer resourceObject) {
		PrismObjectDefinition<T> shadowDefinition = getShadowDefinition(type);
		PrismObject<T> shadow = shadowDefinition.instantiate();
		shadow.getValue().add(resourceObject);
		return shadow;
	}

	private <T extends ResourceObjectShadowType> PrismObjectDefinition<T> getShadowDefinition(Class<T> type) { 
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
	}
}
