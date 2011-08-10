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

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
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
import com.evolveum.midpoint.xml.schema.SchemaConstants;

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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-repository-test.xml" })
public class ProvisioningServiceImplOpenDJTest extends OpenDJUnitTestAdapter {

	// Let's reuse the resource definition from UCF tests ... for now
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
	private static final String RESOURCE_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	private ResourceType resource;
	@Autowired
	private ConnectorFactory manager;
	@Autowired
	private ProvisioningService provisioningService;
	private Unmarshaller unmarshaller;

	@Autowired(required = true)
	private RepositoryService repositoryService;

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

	@BeforeClass
	public static void startLdap() throws Exception {
		startACleanDJ();
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		stopDJ();

	}

	@Before
	public void initProvisioning() throws Exception {

		assertNotNull(manager);

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".initProvisioning");
		// The default repository content is using old format of resource
		// configuration
		// We need a sample data in the new format, so we need to set it up
		// manually.

		resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
//		addObjectFromFile(FILENAME_ACCOUNT1);
		addObjectFromFile(FILENAME_ACCOUNT_BAD);
		assertNotNull(provisioningService);

	}
	
	@After
	public void shutdownUcf() throws Exception {

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".shutdownUcf");
		try {
			repositoryService.deleteObject(ACCOUNT1_OID, result);
		} catch (Exception e) {
		}
		try {
			repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
		} catch (Exception e) {
		}
		try {
			repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
		} catch (Exception e) {
		}
	}
	
	/**
	 * This should be the very first executed test.
	 * 
	 * The original repository object does not have resource schema. The schema should be generated from
	 * the resource on the first use. This is the test that executes testResource and checks whether the
	 * schema was generated. Therefore it must be the very first test to be executed.
	 */
	@Test
	public void test000Connection() throws Exception {
		displayTestTile("test000Connection");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+"test000Connection");
		ObjectType object = repositoryService.getObject(RESOURCE_OPENDJ_OID, null, result);
		ResourceType resourceBefore = (ResourceType)object;
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		assertTrue("Found schema before test connection. Bad test setup?",xmlSchemaTypeBefore.getAny().isEmpty());
		
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID);
		
		display("Test connection result",operationResult);
		assertSuccess("Test connection failed",operationResult);

		object = repositoryService.getObject(RESOURCE_OPENDJ_OID, null, result);
		ResourceType resourceAfter = (ResourceType)object;
		XmlSchemaType xmlSchemaTypeAfter = resourceAfter.getSchema();
		assertNotNull("No schema after test connection",xmlSchemaTypeAfter);
		assertFalse("No schema after test connection",xmlSchemaTypeAfter.getAny().isEmpty());

		display("Generated schema",xmlSchemaTypeBefore.getAny());
	}

	@Test
	public void testGetObject() throws Exception {
		displayTestTile("testGetObject");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".getObjectTest");
		try {

			ObjectType objectToAdd = createObjectFromFile(FILENAME_ACCOUNT1);

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
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
			fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND REPO result:");
			System.out.println(result.dump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
			fail("Expected exception, but haven't got one");
		} catch (ObjectNotFoundException e) {
			// This is expected

			// Just to close the top-level result.
			result.recordFatalError("Error :-)");

			System.out.println("NOT FOUND RESOURCE result:");
			System.out.println(result.dump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
			ObjectType object = createObjectFromFile(FILENAME_ACCOUNT_NEW);

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
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_NEW_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
			fail("Expected IllegalArgumentException but haven't got one.");
		} catch(IllegalArgumentException ex){
			assertEquals("Object to add must not be null.", ex.getMessage());
			assertNull(addedObjectOid);
		} finally {
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
			ObjectType object = createObjectFromFile(FILENAME_ACCOUNT_DELETE);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_DELETE_OID, addedObjectOid);

			provisioningService.deleteObject(ACCOUNT_DELETE_OID, null, result);

			ObjectType objType = null;

			try {
				objType = provisioningService.getObject(ACCOUNT_DELETE_OID, new PropertyReferenceListType(),
						result);
				fail("Expected exception ObjectNotFoundException, but haven't got one.");
			} catch (ObjectNotFoundException ex) {
				System.out.println("Catched ObjectNotFoundException.");
				assertNull(objType);
			}

			try {
				objType = repositoryService.getObject(ACCOUNT_DELETE_OID, new PropertyReferenceListType(),
						result);
				// objType = container.getObject();
				fail("Expected exception, but haven't got one.");
			} catch (Exception ex) {
				assertNull(objType);
				assertEquals("Object not found. OID: " + ACCOUNT_DELETE_OID, ex.getMessage());

			}
		} finally {
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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

			ObjectType object = createObjectFromFile(FILENAME_ACCOUNT_MODIFY);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_MODIFY_OID, addedObjectOid);

			ObjectChangeModificationType objectChange = ((JAXBElement<ObjectChangeModificationType>) JAXBUtil
					.unmarshal(new File("src/test/resources/impl/account-change-description.xml")))
					.getValue();

			System.out.println("oid changed obj: " + objectChange.getObjectModification().getOid());

			provisioningService.modifyObject(objectChange.getObjectModification(), null, result);

			AccountShadowType accountType = (AccountShadowType) provisioningService.getObject(
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
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_MODIFY_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
				ObjectListType objListType = provisioningService.listObjects(AccountShadowType.class,
						new PagingType(), result);
				fail("Expected excetpion, but haven't got one");
			} catch (Exception ex) {
				assertEquals("NotImplementedException", ex.getClass().getSimpleName());
			}

		} finally {
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_MODIFY_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
			ObjectType object = createObjectFromFile(FILENAME_ACCOUNT_SEARCH_ITERATIVE);

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
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_SEARCH_ITERATIVE_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
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
			ObjectType object = createObjectFromFile(FILENAME_ACCOUNT_SEARCH);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_SEARCH_OID, addedObjectOid);

			QueryType query = ((JAXBElement<QueryType>) JAXBUtil.unmarshal(new File(
					"src/test/resources/impl/query-filter-all-accounts.xml"))).getValue();

			ObjectListType objListType = provisioningService.searchObjects(query, new PagingType(), result);
			for (ObjectType objType : objListType.getObject()) {
				if (objType == null) {
					System.out.println("Object not found in repository.");
				} else {
					System.out.println("found object: " + objType.getName());
				}
			}
		} finally {
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ACCOUNT_SEARCH_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void testListConnectors(){
		displayTestTile("testListConnectors");
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".listConnectorsTest");
		
		ObjectListType objListType = provisioningService.listObjects(ConnectorType.class, new PagingType(), result);
		assertNotNull(objListType);
		
		List<ObjectType> objects = objListType.getObject();
		for (ObjectType objType : objects){
			assertEquals(ConnectorType.class, objType.getClass());
			System.out.println("connector name: "+ ((ConnectorType) objType).getName());
			System.out.println("connector type: "+ ((ConnectorType) objType).getConnectorType());
		}
	}
	
	private ObjectType createObjectFromFile(String filePath) throws FileNotFoundException, JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		ObjectType objectType = ((JAXBElement<ObjectType>) object).getValue();
		return objectType;
	}

	private ObjectType addObjectFromFile(String filePath) throws Exception {
		ObjectType object = createObjectFromFile(filePath);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".addObjectFromFile");
		repositoryService.addObject(object, result);
		return object;
	}

}
