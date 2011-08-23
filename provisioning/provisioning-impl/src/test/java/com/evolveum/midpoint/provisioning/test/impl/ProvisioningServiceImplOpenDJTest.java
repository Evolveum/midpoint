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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.testng.AssertJUnit;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

/**
 * Test for provisioning service implementation.
 * 
 * This test will initialize mock repository and fill-in some test data. The
 * "default" repository objects cannot be used, as the new provisioning service
 * implementation assumes a slightly different connector configuration that was
 * used in the OpenIDM.
 * 
 * This test will initialize embedded OpenDJ as a target resource.
 * 
 * The test calls the new Provisioning Service Interface (java). No WSDL mess.
 * 
 * @author Radovan Semancik
 * @author Katka Valalikova
 */

@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class ProvisioningServiceImplOpenDJTest extends AbstractIntegrationTest {

	// Let's reuse the resource definition from UCF tests ... for now
	private static final String FILENAME_CONNECTOR_LDAP = "src/test/resources/ucf/ldap-connector.xml";
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	private static final String FILENAME_ACCOUNT1 = "src/test/resources/impl/account1.xml";
	private static final String ACCOUNT1_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1cccc";
	private static final String FILENAME_ACCOUNT_NEW = "src/test/resources/impl/account-new.xml";
	private static final String ACCOUNT_NEW_OID = "c0c010c0-d34d-b44f-f11d-333222123456";
	private static final String FILENAME_ACCOUNT_BAD = "src/test/resources/impl/account-bad.xml";
	private static final String ACCOUNT_BAD_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1ffff";
	private static final String FILENAME_ACCOUNT_MODIFY = "src/test/resources/impl/account-modify.xml";
	private static final String ACCOUNT_MODIFY_OID = "c0c010c0-d34d-b44f-f11d-333222444555";
	private static final String FILENAME_ACCOUNT_DELETE = "src/test/resources/impl/account-delete.xml";
	private static final String ACCOUNT_DELETE_OID = "c0c010c0-d34d-b44f-f11d-333222654321";
	private static final String FILENAME_ACCOUNT_SEARCH_ITERATIVE = "src/test/resources/impl/account-search-iterative.xml";
	private static final String ACCOUNT_SEARCH_ITERATIVE_OID = "c0c010c0-d34d-b44f-f11d-333222666666";
	private static final String FILENAME_ACCOUNT_SEARCH = "src/test/resources/impl/account-search.xml";
	private static final String ACCOUNT_SEARCH_OID = "c0c010c0-d34d-b44f-f11d-333222777777";
	private static final String NON_EXISTENT_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	private static final String RESOURCE_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	private static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_NS,"AccountObjectClass");

	private JAXBContext jaxbctx;
	private ResourceType resource;
	@Autowired
	private ConnectorFactory manager;
	@Autowired
	private ProvisioningService provisioningService;
	@Autowired
	private ConnectorTypeManager connectorTypeManager;
	
	private Unmarshaller unmarshaller;
	private static boolean provisioningInitialized = false;

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public ProvisioningServiceImplOpenDJTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
	}

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		// Nothing to do		
	}
	
	@BeforeClass
	public static void startLdap() throws Exception {
		openDJController.startCleanServer();
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		openDJController.stop();
	}

	@BeforeMethod
	public void initProvisioning() throws Exception {

		assertNotNull(manager);

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".initProvisioning");
		
		if (!provisioningInitialized) {
			// Adding this before "postInit" will give us fixed OID of the ConnectorType object
			addObjectFromFile(FILENAME_CONNECTOR_LDAP);
			
			provisioningService.postInit(result);
			result.computeStatus("Test failed");
			display("Provisioning initialization",result);
			assertSuccess("Provisioning initialization failed", result);
			provisioningInitialized=true;
		}
		
		resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
//		addObjectFromFile(FILENAME_ACCOUNT1);
		addObjectFromFile(FILENAME_ACCOUNT_BAD);
	}
	
	@AfterMethod
	public void shutdownUcf() throws Exception {

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".shutdownUcf");
		try {
			repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
		} catch (Exception e) {
		}
		try {
			repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
		} catch (Exception e) {
		}
		try {
			repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
		} catch (Exception e) {
		}
	}
	
	/**
	 * Check whether the connectors were discovered correctly and were added to the repository.
	 * @throws SchemaProcessorException 
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaProcessorException {
		displayTestTile("test001Connectors");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test001Connectors");
		
		List<ConnectorType> connectors = repositoryService.listObjects(ConnectorType.class, null, result);
		
		assertFalse("No connector found",connectors.isEmpty());
		
		for (ConnectorType conn : connectors) {
			display("Found connector",conn);
			if (conn.getConnectorType().equals("org.identityconnectors.ldap.LdapConnector")) {
				// This connector is loaded manually, it has no schema
				continue;
			}
			XmlSchemaType xmlSchemaType = conn.getSchema();
			assertNotNull("xmlSchemaType is null",xmlSchemaType);
			assertFalse("Empty schema",xmlSchemaType.getAny().isEmpty());
			// Try to parse the schema
			Schema schema = Schema.parse(xmlSchemaType.getAny().get(0));
			assertNotNull("Cannot parse schema",schema);
			assertFalse("Empty schema",schema.isEmpty());
			Definition definition = schema.getDefinitions().iterator().next();
			assertNotNull(definition);
			AssertJUnit.assertTrue("Unexpected definition",definition instanceof PropertyContainerDefinition);
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
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test002ConnectorRediscovery");
		
		Set<ConnectorType> discoverLocalConnectors = connectorTypeManager.discoverLocalConnectors(result);
		result.computeStatus("test failed");
		assertSuccess("discoverLocalConnectors failed", result);
		AssertJUnit.assertTrue("Rediscovered something",discoverLocalConnectors.isEmpty());
	}
	
	/**
	 * This should be the very first test that works with the resource.
	 * 
	 * The original repository object does not have resource schema. The schema should be generated from
	 * the resource on the first use. This is the test that executes testResource and checks whether the
	 * schema was generated.
	 */
	@Test
	public void test003Connection() throws Exception {
		displayTestTile("test003Connection");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test003Connection");
		ResourceType resourceBefore = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		AssertJUnit.assertTrue("Found schema before test connection. Bad test setup?",xmlSchemaTypeBefore.getAny().isEmpty());
		
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID);
		
		display("Test connection result",operationResult);
		assertSuccess("Test connection failed",operationResult);

		ResourceType resourceAfter = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		XmlSchemaType xmlSchemaTypeAfter = resourceAfter.getSchema();
		assertNotNull("No schema after test connection",xmlSchemaTypeAfter);
		assertFalse("No schema after test connection",xmlSchemaTypeAfter.getAny().isEmpty());

		display("Generated schema",xmlSchemaTypeBefore.getAny());
		
		// TODO: try to parse the schema
	}
	
	@Test
	public void test004ListResourceObjects() throws SchemaException, ObjectNotFoundException, CommunicationException {
		displayTestTile("test004ListResourceObjects");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test004ListResourceObjects");
		// WHEN
		ObjectListType objectList = provisioningService.listResourceObjects(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, result);
		// THEN
		assertNotNull(objectList);
		assertFalse("Empty list returned",objectList.getObject().isEmpty());
		display("Resource object list "+RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS,objectList.getObject());
	}

	@Test
	public void testGetObject() throws Exception {
		displayTestTile("testGetObject");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".getObjectTest");
		try {

			ObjectType objectToAdd = unmarshallJaxbFromFile(FILENAME_ACCOUNT1);

			System.out.println(DebugUtil.prettyPrint(objectToAdd));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(objectToAdd,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(objectToAdd, null, result);
			assertEquals(ACCOUNT1_OID, addedObjectOid);
			PropertyReferenceListType resolve = new PropertyReferenceListType();

			ObjectType object = provisioningService.getObject(ACCOUNT1_OID, resolve, result);

			assertNotNull(object);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));
			
			assertEquals("jbond", object.getName());
			
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}
		// TODO: check values
	}

	/**
	 * Let's try to fetch object that does not exist in the repository.
	 */
	@Test
	public void testGetObjectNotFoundRepo() throws Exception {
		displayTestTile("testGetObjectNotFoundRepo");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".getObjectTest");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		try {
			ObjectType object = provisioningService.getObject(NON_EXISTENT_OID, resolve, result);
			Assert.fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND REPO result:");
			System.out.println(result.dump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}

	}

	/**
	 * Let's try to fetch object that does exit in the repository but does not
	 * exist in the resource.
	 */
	@Test
	public void testGetObjectNotFoundResource() throws Exception {
		displayTestTile("testGetObjectNotFoundResource");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".getObjectTest");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		try {
			ObjectType object = provisioningService.getObject(ACCOUNT_BAD_OID, resolve, result);
			Assert.fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND RESOURCE result:");
			System.out.println(result.dump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			Assert.fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}

	}

	@Test
	public void testAddObject() throws Exception {
		displayTestTile("testAddObject");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".addObjectTest");

		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_NEW);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

			ObjectType container = repositoryService.getObject(ACCOUNT_NEW_OID,
					new PropertyReferenceListType(), result);
			AccountShadowType accountType = (AccountShadowType) container;
			assertEquals("will", accountType.getName());

			ObjectType objType = provisioningService.getObject(ACCOUNT_NEW_OID,
					new PropertyReferenceListType(), result);
			assertEquals("will", objType.getName());
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_NEW_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}
	}

	
	@Test
	public void testAddObjectNull() throws Exception {
		displayTestTile("testAddObjectNull");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".addObjectTest");

		String addedObjectOid = null;
		
		try {
		
			addedObjectOid = provisioningService.addObject(null, null, result);
			Assert.fail("Expected IllegalArgumentException but haven't got one.");
		} catch(IllegalArgumentException ex){
			assertEquals("Object to add must not be null.", ex.getMessage());
			assertNull(addedObjectOid);
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}
	}

	
	@Test
	public void testDeleteObject() throws Exception {
		displayTestTile("testDeleteObject");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".addObjectTest");

		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_DELETE);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_DELETE_OID, addedObjectOid);

			provisioningService.deleteObject(AccountShadowType.class, ACCOUNT_DELETE_OID, null, result);

			ObjectType objType = null;

			try {
				objType = provisioningService.getObject(ACCOUNT_DELETE_OID, new PropertyReferenceListType(),
						result);
				Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
			} catch (ObjectNotFoundException ex) {
				System.out.println("Catched ObjectNotFoundException.");
				assertNull(objType);
			}

			try {
				objType = repositoryService.getObject(ACCOUNT_DELETE_OID, new PropertyReferenceListType(),
						result);
				// objType = container.getObject();
				Assert.fail("Expected exception, but haven't got one.");
			} catch (Exception ex) {
				assertNull(objType);
				assertEquals("Object not found. OID: " + ACCOUNT_DELETE_OID, ex.getMessage());

			}
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}

	}

	@Test
	public void testModifyObject() throws Exception {
		displayTestTile("testModifyObject");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".addObjectTest");

		try {

			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_MODIFY);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_MODIFY_OID, addedObjectOid);

			ObjectChangeModificationType objectChange = ((JAXBElement<ObjectChangeModificationType>) JAXBUtil
					.unmarshal(new File("src/test/resources/impl/account-change-description.xml")))
					.getValue();

			System.out.println("oid changed obj: " + objectChange.getObjectModification().getOid());

			provisioningService.modifyObject(AccountShadowType.class,objectChange.getObjectModification(), null, result);

			AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
					ACCOUNT_MODIFY_OID, new PropertyReferenceListType(), result);
			String changedSn = null;
			for (Element e : accountType.getAttributes().getAny()) {
				if (QNameUtil.compareQName(new QName(RESOURCE_NS, "sn"), e)) {
					changedSn = e.getTextContent();
				}
			}

			assertEquals("First", changedSn);
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_MODIFY_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}

	}

	@Test
	public void testListObjects() throws Exception {
		displayTestTile("testListObjects");
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".addObjectTest");

		try {

			try {
				List<AccountShadowType> objListType = provisioningService.listObjects(AccountShadowType.class,
						new PagingType(), result);
				Assert.fail("Expected excetpion, but haven't got one");
			} catch (Exception ex) {
				assertEquals("NotImplementedException", ex.getClass().getSimpleName());
			}

		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_MODIFY_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}

		}

	}

	@Test
	public void test200SearchObjectsIterative() throws Exception {
		displayTestTile("test200SearchObjectsIterative");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".searchObjectsIterativeTest");
		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_SEARCH_ITERATIVE);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_SEARCH_ITERATIVE_OID, addedObjectOid);

			final List<ObjectType> objectTypeList = new ArrayList<ObjectType>();

			QueryType query = ((JAXBElement<QueryType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/impl/query-filter-all-accounts.xml"))).getValue();
			provisioningService.searchObjectsIterative(query, new PagingType(), new ResultHandler() {

				@Override
				public boolean handle(ObjectType object, OperationResult parentResult) {

					return objectTypeList.add(object);
				}
			}, result);

			// TODO: check result
			System.out.println("ObjectType list size: " + objectTypeList.size());

			for (ObjectType objType : objectTypeList) {
				if (objType == null) {
					System.out.println("Object not found in repo");
				} else {
					System.out.println("obj name: " + objType.getName());
				}
			}
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_SEARCH_ITERATIVE_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testSearchObjects() throws Exception {
		displayTestTile("testSearchObjects");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".searchObjectsTest");

		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_SEARCH);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_SEARCH_OID, addedObjectOid);

			QueryType query = ((JAXBElement<QueryType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/impl/query-filter-all-accounts.xml"))).getValue();

			List<AccountShadowType> objListType = provisioningService.searchObjects(AccountShadowType.class, query, new PagingType(), result);
			for (AccountShadowType objType : objListType) {
				if (objType == null) {
					System.out.println("Object not found in repository.");
				} else {
					System.out.println("found object: " + objType.getName());
				}
			}
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_SEARCH_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testListConnectors(){
		displayTestTile("testListConnectors");
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".listConnectorsTest");
		
		List<ConnectorType> connectors = provisioningService.listObjects(ConnectorType.class, new PagingType(), result);
		assertNotNull(connectors);
		
		for (ConnectorType conn : connectors){
			System.out.println("connector name: "+ conn.getName());
			System.out.println("connector type: "+ conn.getConnectorType());
		}
		
		// TODO: assert something
	}
	
	
}
