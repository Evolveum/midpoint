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

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.XPathUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import java.io.FileNotFoundException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.io.File;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.provisioning.impl.RepositoryWrapper;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.test.repository.BaseXDatabaseFactory;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorManagerIcfImpl;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import javax.xml.bind.JAXBException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Element;

import static org.junit.Assert.*;

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
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml" })
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
	private static final String NON_EXISTENT_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	private static final String RESOURCE_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	private ResourceType resource;
	private ConnectorManager manager;
	private ShadowCache shadowCache;
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

		ConnectorManagerIcfImpl managerImpl = new ConnectorManagerIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;
		assertNotNull(manager);

		// The default repository content is using old format of resource
		// configuration
		// We need a sample data in the new format, so we need to set it up
		// manually.

		try {
			resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
			addObjectFromFile(FILENAME_ACCOUNT1);
			addObjectFromFile(FILENAME_ACCOUNT_BAD);
		} catch (Exception ex) {
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, null);
			} catch (Exception e) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, null);
			} catch (Exception e) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, null);
			} catch (Exception e) {}
		}

		shadowCache = new ShadowCache();
		shadowCache.setConnectorManager(manager);

		shadowCache.setRepositoryService(repositoryService);

		ProvisioningServiceImpl provisioningServiceImpl = new ProvisioningServiceImpl();
		provisioningServiceImpl.setShadowCache(shadowCache);
		provisioningServiceImpl.setRepositoryService(repositoryService);
		provisioningService = provisioningServiceImpl;

		assertNotNull(provisioningService);
	}

	private ObjectType createObjectFromFile(String filePath) throws FileNotFoundException, JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		ObjectType objectType = (ObjectType) ((JAXBElement) object).getValue();
		// ObjectContainerType container = new ObjectContainerType();
		// container.setObject(objectType);
		return objectType;
	}

	private ObjectType addObjectFromFile(String filePath) throws Exception {
		// File file = new File(filePath);
		// FileInputStream fis = new FileInputStream(file);
		// Object object = unmarshaller.unmarshal(fis);
		// ObjectType objectType = (ObjectType) ((JAXBElement)
		// object).getValue();
		// ObjectContainerType container = new ObjectContainerType();
		// container.setObject(objectType);
		ObjectType object = createObjectFromFile(filePath);
		System.out.println("obj: " + object.getName());
		// repositoryPort.addObject(container);
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".getObjectTest");
		repositoryService.addObject(object, result);
		return object;// container.getObject();
	}

	@After
	public void shutdownUcf() throws Exception {
		BaseXDatabaseFactory.XMLServerStop();

	}

	@Test
	public void testGetObject() throws Exception {
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".getObjectTest");
		try {

			PropertyReferenceListType resolve = new PropertyReferenceListType();

			ObjectType object = provisioningService.getObject(ACCOUNT1_OID, resolve, result);

			assertNotNull(object);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));
		} finally {
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {}
		}
		// TODO: check values
	}

	/**
	 * Let's try to fetch object that does not exist in the repository.
	 */
	@Test
	public void testGetObjectNotFoundRepo() throws Exception {
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
			System.out.println(result.debugDump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {}
		}

	}

	/**
	 * Let's try to fetch object that does exit in the repository but does not
	 * exist in the resource.
	 */
	@Test
	public void testGetObjectNotFoundResource() throws Exception {
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
			System.out.println(result.debugDump());

			assertFalse(result.hasUnknownStatus());
			// TODO: check result
		} catch (CommunicationException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} catch (SchemaException e) {
			fail("Expected ObjectNotFoundException, but got" + e);
		} finally {
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {}
		}

	}

	@Test
	public void testAddObject() throws Exception {

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
				repositoryService.deleteObject(ACCOUNT_NEW_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {}
		}
	}

	@Test
	public void testDeleteObject() throws Exception {

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
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {}
		}

	}

	@Test
	public void testModifyObject() throws Exception {

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
				repositoryService.deleteObject(ACCOUNT_MODIFY_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {}
		}

	}

	@Test
	public void testListObjects() throws Exception {
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
				repositoryService.deleteObject(ACCOUNT_MODIFY_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT1_OID, result);
			} catch (Exception ex) {}
			try {
				repositoryService.deleteObject(ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {}
		}

	}

}
