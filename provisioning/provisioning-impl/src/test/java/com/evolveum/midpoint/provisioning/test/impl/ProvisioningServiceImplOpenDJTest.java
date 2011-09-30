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

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.testng.AssertJUnit;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
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
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.EnhancedResourceType;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.ldap.OpenDJController;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CachingMetadata;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;

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
		"classpath:application-context-task.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
public class ProvisioningServiceImplOpenDJTest extends AbstractIntegrationTest {

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
	private static final String FILENAME_ACCOUNT_MODIFY_PASSWORD = "src/test/resources/impl/account-modify-password.xml";
	private static final String ACCOUNT_MODIFY_PASSWORD_OID = "c0c010c0-d34d-b44f-f11d-333222444566";
	private static final String FILENAME_ACCOUNT_DELETE = "src/test/resources/impl/account-delete.xml";
	private static final String ACCOUNT_DELETE_OID = "c0c010c0-d34d-b44f-f11d-333222654321";
	private static final String FILENAME_ACCOUNT_SEARCH_ITERATIVE = "src/test/resources/impl/account-search-iterative.xml";
	private static final String ACCOUNT_SEARCH_ITERATIVE_OID = "c0c010c0-d34d-b44f-f11d-333222666666";
	private static final String FILENAME_ACCOUNT_SEARCH = "src/test/resources/impl/account-search.xml";
	private static final String ACCOUNT_SEARCH_OID = "c0c010c0-d34d-b44f-f11d-333222777777";
	private static final String FILENAME_ACCOUNT_NEW_WITH_PASSWORD = "src/test/resources/impl/account-new-with-password.xml";;
	private static final String ACCOUNT_NEW_WITH_PASSWORD_OID = "c0c010c0-d34d-b44f-f11d-333222124422";
	private static final String NON_EXISTENT_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
	private static final String RESOURCE_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	private static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_NS,"AccountObjectClass");
	private static final String LDAP_CONNECTOR_TYPE = "org.identityconnectors.ldap.LdapConnector";

	@Autowired
	private ProvisioningService provisioningService;
	@Autowired
	private ConnectorTypeManager connectorTypeManager;
	@Autowired(required = true)
	private ConnectorFactory connectorFactoryIcfImpl;

	private static Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImplOpenDJTest.class);

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
		provisioningService.postInit(initResult);
		ResourceType resource = addResourceFromFile(FILENAME_RESOURCE_OPENDJ, LDAP_CONNECTOR_TYPE, initResult);
//		addObjectFromFile(FILENAME_ACCOUNT1);
		addObjectFromFile(FILENAME_ACCOUNT_BAD,initResult);
	}
	
	@BeforeClass
	public static void startLdap() throws Exception {
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("START:  ProvisioningServiceImplOpenDJTest");
		LOGGER.info("------------------------------------------------------------------------------");
		try {
		openDJController.startCleanServer();
		} catch (IOException ex) {
			LOGGER.error("Couldn't start LDAP.", ex);
			throw ex;
		}
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		openDJController.stop();
		LOGGER.info("------------------------------------------------------------------------------");
		LOGGER.info("STOP:  ProvisioningServiceImplOpenDJTest");
		LOGGER.info("------------------------------------------------------------------------------");
	}
	
	@AfterMethod
	public void shutdownUcf() throws Exception {

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".shutdownUcf");
		try {
			repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
		} catch (Exception e) {
		}
//		try {
//			repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
//		} catch (Exception e) {
//		}
//		try {
//			repositoryService.deleteObject(AccountShadowType.class, RESOURCE_OPENDJ_OID, result);
//		} catch (Exception e) {
//		}
	}
	
	/**
	 * Check whether the connectors were discovered correctly and were added to the repository.
	 * @throws SchemaProcessorException 
	 * 
	 */
	@Test
	public void test001Connectors() throws SchemaException {
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
			display("Parsed connector schema",schema);
			PropertyContainerDefinition definition = schema.findItemDefinition("configuration",PropertyContainerDefinition.class);
			assertNotNull("Definition of <configuration> property container not found",definition);
			PropertyContainerDefinition pcd = (PropertyContainerDefinition)definition;
			assertFalse("Empty definition",pcd.isEmpty());
		}
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
		assertNotNull("No connector ref",resourceBefore.getConnectorRef());
		assertNotNull("No connector ref OID",resourceBefore.getConnectorRef().getOid());
		ConnectorType connector = repositoryService.getObject(ConnectorType.class, resourceBefore.getConnectorRef().getOid(), null, result);
		assertNotNull(connector);
		XmlSchemaType xmlSchemaTypeBefore = resourceBefore.getSchema();
		AssertJUnit.assertTrue("Found schema before test connection. Bad test setup?",xmlSchemaTypeBefore.getAny().isEmpty());
		
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID);
		
		display("Test connection result",operationResult);
		assertSuccess("Test connection failed",operationResult);

		ResourceType resourceAfter = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		
		display("Resource after testResource",resourceAfter);
		
		XmlSchemaType xmlSchemaTypeAfter = resourceAfter.getSchema();
		assertNotNull("No schema after test connection",xmlSchemaTypeAfter);
		assertFalse("No schema after test connection",xmlSchemaTypeAfter.getAny().isEmpty());
		
		CachingMetadata cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata",cachingMetadata);
		assertNotNull("No retrievalTimestamp",cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber",cachingMetadata.getSerialNumber());
		
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
		Schema parsedSchema = Schema.parse(xsdElement);
		assertNotNull("No schema after parsing",parsedSchema);
		
		ResourceObjectDefinition accountDefinition = parsedSchema.findAccountDefinition();
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDefinition.findAttributeDefinition(
				new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));
		
	}
	
	@Test
	public void test005Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException {
		displayTestTile("test005Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test005Capabilities");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result);
		
		// THEN
		CapabilitiesType nativeCapabilities = resource.getNativeCapabilities();
		List<Object> capabilities = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",capabilities.isEmpty());
        CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(capabilities, CredentialsCapabilityType.class);
        assertNotNull("password capability not present",capCred.getPassword());
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = ResourceTypeUtil.getCapability(capabilities, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it",capAct);
        
        capCred = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
        assertNotNull("password capability not found",capCred.getPassword());
        // Although connector does not support activation, the resource specifies a way how to simulate it.
        // Therefore the following should succeed
        capAct = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        assertNotNull("activation capability not found",capAct);
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.listEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+ResourceTypeUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }
	}
	
	
	@Test
	public void test006ListResourceObjects() throws SchemaException, ObjectNotFoundException, CommunicationException {
		displayTestTile("test006ListResourceObjects");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test006ListResourceObjects");
		// WHEN
		ResultList<? extends ResourceObjectShadowType> objectList = provisioningService.listResourceObjects(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, result);
		// THEN
		assertNotNull(objectList);
		assertFalse("Empty list returned",objectList.isEmpty());
		display("Resource object list "+RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS,objectList);
	}

	@Test
	public void test007GetObject() throws Exception {
		displayTestTile("test007GetObject");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test007GetObject");
		try {

			ObjectType objectToAdd = unmarshallJaxbFromFile(FILENAME_ACCOUNT1);

			System.out.println(DebugUtil.prettyPrint(objectToAdd));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(objectToAdd,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(objectToAdd, null, result);
			assertEquals(ACCOUNT1_OID, addedObjectOid);
			PropertyReferenceListType resolve = new PropertyReferenceListType();

			AccountShadowType acct = provisioningService.getObject(AccountShadowType.class, ACCOUNT1_OID, resolve, result);

			assertNotNull(acct);

			System.out.println(DebugUtil.prettyPrint(acct));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(acct,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));
			
			assertEquals("jbond", acct.getName());
			
		} finally {
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
		}
		// TODO: check values
	}

	/**
	 * Let's try to fetch object that does not exist in the repository.
	 */
	@Test
	public void test008GetObjectNotFoundRepo() throws Exception {
		displayTestTile("test008GetObjectNotFoundRepo");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test008GetObjectNotFoundRepo");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, NON_EXISTENT_OID, resolve, result);
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
		}

	}

	/**
	 * Let's try to fetch object that does exit in the repository but does not
	 * exist in the resource.
	 */
	@Test
	public void test009GetObjectNotFoundResource() throws Exception {
		displayTestTile("test009GetObjectNotFoundResource");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test009GetObjectNotFoundResource");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, ACCOUNT_BAD_OID, resolve, result);
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
		}

	}

	@Test
	public void test010AddObject() throws Exception {
		displayTestTile("test010AddObject");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test010AddObject");

		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_NEW);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

			AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
					new PropertyReferenceListType(), result);
			assertEquals("will", accountType.getName());

			AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
					new PropertyReferenceListType(), result);
			assertEquals("will", provisioningAccountType.getName());
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
		}
	}

	
	@Test
	public void test011AddObjectNull() throws Exception {
		displayTestTile("test011AddObjectNull");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test011AddObjectNull");

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
		}
	}

	
	@Test
	public void test012DeleteObject() throws Exception {
		displayTestTile("test012DeleteObject");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test012DeleteObject");

		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_DELETE);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_DELETE_OID, addedObjectOid);

			provisioningService.deleteObject(AccountShadowType.class, ACCOUNT_DELETE_OID, null, result);

			AccountShadowType objType = null;

			try {
				objType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_DELETE_OID, new PropertyReferenceListType(),
						result);
				Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
			} catch (ObjectNotFoundException ex) {
				System.out.println("Catched ObjectNotFoundException.");
				assertNull(objType);
			}

			try {
				objType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_DELETE_OID, new PropertyReferenceListType(),
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
		}

	}

	@Test
	public void test013ModifyObject() throws Exception {
		displayTestTile("test013ModifyObject");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test013ModifyObject");

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
			display("Object change",DebugUtil.prettyPrint(objectChange));
			System.out.println("new sn value: " + ((Element)objectChange.getObjectModification().getPropertyModification().get(0).getValue().getAny().get(0)).getTextContent());
			System.out.println("oid changed obj: " + objectChange.getObjectModification().getOid());

			provisioningService.modifyObject(AccountShadowType.class,objectChange.getObjectModification(), null, result);
			
			AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
					ACCOUNT_MODIFY_OID, new PropertyReferenceListType(), result);
			
			display("Object after change",accountType);

			String changedSn = null;
			String uid = null;
			for (Object e : accountType.getAttributes().getAny()) {
				if ("sn".equals(JAXBUtil.getElementQName(e).getLocalPart())) {
					changedSn = ((Element)e).getTextContent();
				}
				if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(e))) {
					uid = ((Element)e).getTextContent();
				}

			}
			assertNotNull(uid);
			
			// Check if object was modified in LDAP
			
			SearchResultEntry response = openDJController.searchByEntryUuid(uid);			
			display("LDAP account", response);
			
			OpenDJController.assertAttribute(response, "sn", "First");
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
		}

	}

	@Test
	public void test014ChangePassword() throws Exception {
		displayTestTile("test014ChangePassword");
		
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test014ChangePassword");

		try {

			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_MODIFY_PASSWORD);

			String addedObjectOid = provisioningService.addObject(object, null, result);

			assertEquals(ACCOUNT_MODIFY_PASSWORD_OID, addedObjectOid);
			
			AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
					ACCOUNT_MODIFY_PASSWORD_OID, new PropertyReferenceListType(), result);
			
			display("Object after password change",accountType);
			
			String uid = null;
			for (Object e : accountType.getAttributes().getAny()) {
				if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(e))) {
					uid = ((Element)e).getTextContent();
				}
			}
			assertNotNull(uid);
			
			SearchResultEntry entryBefore = openDJController.searchByEntryUuid(uid);			
			display("LDAP account before", entryBefore);

			String passwordBefore = OpenDJController.getAttributeValue(entryBefore, "userPassword");
			assertNull("Unexpected password before change",passwordBefore);

			ObjectChangeModificationType objectChange = ((JAXBElement<ObjectChangeModificationType>) JAXBUtil
					.unmarshal(new File("src/test/resources/impl/account-change-password.xml")))
					.getValue();
			display("Object change",DebugUtil.prettyPrint(objectChange));

			// WHEN
			provisioningService.modifyObject(AccountShadowType.class,objectChange.getObjectModification(), null, result);

			// THEN
			
			// Check if object was modified in LDAP
			
			SearchResultEntry entryAfter = openDJController.searchByEntryUuid(uid);			
			display("LDAP account after", entryAfter);

			String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
			assertNotNull("The password was not changed",passwordAfter);
			
			System.out.println("Changed password: "+passwordAfter);

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
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_MODIFY_PASSWORD_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void test015AddObjectWithPassword() throws Exception {
		displayTestTile("test015AddObjectWithPassword");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test015AddObjectWithPassword");

		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_NEW_WITH_PASSWORD);

			System.out.println(DebugUtil.prettyPrint(object));
			System.out.println(DOMUtil.serializeDOMToString(JAXBUtil.jaxbToDom(object,
					SchemaConstants.I_ACCOUNT, DOMUtil.getDocument())));

			String addedObjectOid = provisioningService.addObject(object, null, result);
			assertEquals(ACCOUNT_NEW_WITH_PASSWORD_OID, addedObjectOid);

			AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
					new PropertyReferenceListType(), result);
			assertEquals("lechuck", accountType.getName());

			AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
					new PropertyReferenceListType(), result);
			assertEquals("lechuck", provisioningAccountType.getName());
			
			String uid = null;
			for (Object e : accountType.getAttributes().getAny()) {
				if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(e))) {
					uid = ((Element)e).getTextContent();
				}
			}
			assertNotNull(uid);
			
			// Check if object was created in LDAP and that there is a password
			
			SearchResultEntry entryAfter = openDJController.searchByEntryUuid(uid);			
			display("LDAP account after", entryAfter);

			String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
			assertNotNull("The password was not changed",passwordAfter);
			
			System.out.println("Account password: "+passwordAfter);
			
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
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID, result);
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
