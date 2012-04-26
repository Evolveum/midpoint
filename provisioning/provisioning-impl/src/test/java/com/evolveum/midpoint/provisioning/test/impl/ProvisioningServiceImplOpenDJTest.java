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

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

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
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext
public class ProvisioningServiceImplOpenDJTest extends AbstractIntegrationTest {

	// Let's reuse the resource definition from UCF tests ... for now
	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/object/resource-opendj.xml";
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
	private static final String FILENAME_ACCOUNT_DISABLE_SIMULATED = "src/test/resources/impl/account-disable-simulated-opendj.xml";
	private static final String ACCOUNT_DISABLE_SIMULATED_OID = "dbb0c37d-9ee6-44a4-8d39-016dbce1aaaa";
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
	
	private PrismObject<ResourceType> resource;
	private PrismObject<ConnectorType> connector;

	private static Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImplOpenDJTest.class);

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		provisioningService.postInit(initResult);
		PrismObject<ResourceType> resource = addResourceFromFile(FILENAME_RESOURCE_OPENDJ, LDAP_CONNECTOR_TYPE, initResult);
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
		ResourceType resourceTypeBefore = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, result).asObjectable();
		assertNotNull("No connector ref",resourceTypeBefore.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeBefore.getConnectorRef().getOid());
		connector = repositoryService.getObject(ConnectorType.class, resourceTypeBefore.getConnectorRef().getOid(), result);
		ConnectorType connectorType = connector.asObjectable();
		assertNotNull(connectorType);
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", resourceXsdSchemaElementBefore);
		
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID);
		
		display("Test connection result",operationResult);
		assertSuccess("Test connection failed",operationResult);

		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, result);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		
		display("Resource after testResource (repository)",resourceTypeRepoAfter);
		
		display("Resource after testResource (repository, XML)", PrismTestUtil.serializeObjectToString(resourceTypeRepoAfter.asPrismObject()));
		
		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNotNull("No schema after test connection",xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);
		
		CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
		assertNotNull("No caching metadata",cachingMetadata);
		assertNotNull("No retrievalTimestamp",cachingMetadata.getRetrievalTimestamp());
		assertNotNull("No serialNumber",cachingMetadata.getSerialNumber());
		
		Element xsdElement = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, prismContext);
		assertNotNull("No schema after parsing",parsedSchema);
		
		ObjectClassComplexTypeDefinition accountDefinition = parsedSchema.findDefaultAccountDefinition();
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDefinition.findAttributeDefinition(
				new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));
		assertNull("The userPassword attribute sneaked into schema", accountDefinition.findAttributeDefinition(
				new QName(resourceTypeRepoAfter.getNamespace(),"userPassword")));
		
	}
	
	@Test
	public void test004ResourceAndConnectorCaching() throws Exception {
		displayTestTile("test004ResourceAndConnectorCaching");

		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test004ResourceAndConnectorCaching");
		resource = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, result);
		ResourceType resourceType = resource.asObjectable();
		ConnectorInstance configuredConnectorInstance = connectorTypeManager.getConfiguredConnectorInstance(resource.asObjectable(), result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);
		
		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, result);
		
		// THEN
		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref",resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeAgain.getConnectorRef().getOid());
		
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONFIGURATION);		
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
		assertTrue("Configurations not equals", configurationContainer.equals(configurationContainerAgain));

		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertEquals("Schema serial number mismatch", resourceType.getSchema().getCachingMetadata().getSerialNumber(),
				resourceTypeAgain.getSchema().getCachingMetadata().getSerialNumber());
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
		
		// Now we stick our nose deep inside the provisioning impl. But we need to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = connectorTypeManager.getConfiguredConnectorInstance(resourceTypeAgain, result);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);
	}
	
	@Test
	public void test005Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test005Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test005Capabilities");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, result).asObjectable();
		
		// THEN
		display("Resource from provisioninig", resource);
		display("Resource from provisioninig (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject()));
		
		CapabilitiesType nativeCapabilities = resource.getNativeCapabilities().getCapabilities();
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",nativeCapabilitiesList.isEmpty());
        CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(nativeCapabilitiesList, CredentialsCapabilityType.class);
        assertNotNull("credentials capability not found",capCred);
        assertNotNull("password capability not present",capCred.getPassword());
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = ResourceTypeUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it",capAct);
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.listEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+ResourceTypeUtil.getCapabilityDisplayName(capability)+" : "+capability);
        }
        
        capCred = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
        assertNotNull("credentials effective capability not found",capCred);
        assertNotNull("password effective capability not found",capCred.getPassword());
        // Although connector does not support activation, the resource specifies a way how to simulate it.
        // Therefore the following should succeed
        capAct = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        assertNotNull("activation capability not found",capAct);
        
	}
	
	
	@Test
	public void test006ListResourceObjects() throws SchemaException, ObjectNotFoundException, CommunicationException {
		displayTestTile("test006ListResourceObjects");
		// GIVEN
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+".test006ListResourceObjects");
		// WHEN
		List<PrismObject<? extends ResourceObjectShadowType>> objectList = provisioningService.listResourceObjects(
				RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, result);
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

			AccountShadowType objectToAdd = parseObjectTypeFromFile(FILENAME_ACCOUNT1, AccountShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(objectToAdd));
			System.out.println(objectToAdd.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(objectToAdd.asPrismObject(), null, result);
			assertEquals(ACCOUNT1_OID, addedObjectOid);
			PropertyReferenceListType resolve = new PropertyReferenceListType();

			AccountShadowType acct = provisioningService.getObject(AccountShadowType.class, ACCOUNT1_OID, result).asObjectable();

			assertNotNull(acct);

			System.out.println(SchemaDebugUtil.prettyPrint(acct));
			System.out.println(acct.asPrismObject().dump());
			
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

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, NON_EXISTENT_OID, result).asObjectable();
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

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, ACCOUNT_BAD_OID, result).asObjectable();
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
			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_NEW, AccountShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

			AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
					result).asObjectable();
			assertEquals("will", accountType.getName());

			AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
					result).asObjectable();
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
			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_DELETE, AccountShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_DELETE_OID, addedObjectOid);

			provisioningService.deleteObject(AccountShadowType.class, ACCOUNT_DELETE_OID, null, result);

			AccountShadowType objType = null;

			try {
				objType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_DELETE_OID,
						result).asObjectable();
				Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
			} catch (ObjectNotFoundException ex) {
				System.out.println("Catched ObjectNotFoundException.");
				assertNull(objType);
			}

			try {
				objType = repositoryService.getObject(AccountShadowType.class, ACCOUNT_DELETE_OID,
						result).asObjectable();
				// objType = container.getObject();
				Assert.fail("Expected exception, but haven't got one.");
			} catch (Exception ex) {
				assertNull(objType);
                assertEquals(ex.getClass(), ObjectNotFoundException.class);
                assertTrue(ex.getMessage().contains(ACCOUNT_DELETE_OID));
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

			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_MODIFY, AccountShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_MODIFY_OID, addedObjectOid);

			ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(
					new File("src/test/resources/impl/account-change-description.xml"), ObjectModificationType.class);
			ObjectDelta<AccountShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class, PrismTestUtil.getPrismContext());
			display("Object change",delta);

			provisioningService.modifyObject(AccountShadowType.class, objectChange.getOid(),
					delta.getModifications(), null, result);
			
			AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
					ACCOUNT_MODIFY_OID, result).asObjectable();
			
			display("Object after change",accountType);

//			String changedSn = null;
//			String uid = null;
//			for (Object e : accountType.getAttributes().getAny()) {
//				if ("sn".equals(JAXBUtil.getElementQName(e).getLocalPart())) {
//					changedSn = ((Element)e).getTextContent();
//				}
//				if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(e))) {
//					uid = ((Element)e).getTextContent();
//				}
//
//			}
			
			String uid = ResourceObjectShadowUtil.getSingleStringAttributeValue(accountType, ConnectorFactoryIcfImpl.ICFS_UID);
			List<Object> snValues = ResourceObjectShadowUtil.getAttributeValues(accountType, new QName(RESOURCE_NS, "sn"));
			
			assertNotNull(snValues);
			assertFalse("Surname attributes must not be empty", snValues.isEmpty());
			assertEquals(1, snValues.size());
			
			String changedSn = (String) snValues.get(0);
			
			assertNotNull(uid);
			
			// Check if object was modified in LDAP
			
			SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);			
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

			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_MODIFY_PASSWORD, AccountShadowType.class);

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);

			assertEquals(ACCOUNT_MODIFY_PASSWORD_OID, addedObjectOid);
			
			AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
					ACCOUNT_MODIFY_PASSWORD_OID, result).asObjectable();
			
			display("Object before password change",accountType);
			
			String uid = null;
			uid = ResourceObjectShadowUtil.getSingleStringAttributeValue(accountType, ConnectorFactoryIcfImpl.ICFS_UID);
			assertNotNull(uid);
			
			SearchResultEntry entryBefore = openDJController.searchAndAssertByEntryUuid(uid);			
			display("LDAP account before", entryBefore);

			String passwordBefore = OpenDJController.getAttributeValue(entryBefore, "userPassword");
			assertNull("Unexpected password before change",passwordBefore);
			
			ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(
					new File("src/test/resources/impl/account-change-password.xml"), ObjectModificationType.class);
			ObjectDelta<AccountShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class, PrismTestUtil.getPrismContext());
			display("Object change",delta);

			// WHEN
			provisioningService.modifyObject(AccountShadowType.class, delta.getOid(), delta.getModifications(), null, result);

			// THEN
			
			// Check if object was modified in LDAP
			
			SearchResultEntry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);			
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
			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_NEW_WITH_PASSWORD, AccountShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_NEW_WITH_PASSWORD_OID, addedObjectOid);

			AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
					result).asObjectable();
			assertEquals("lechuck", accountType.getName());

			AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
					result).asObjectable();
			assertEquals("lechuck", provisioningAccountType.getName());
			
			String uid = null;
			for (Object e : accountType.getAttributes().getAny()) {
				if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(e))) {
					uid = ((Element)e).getTextContent();
				}
			}
			assertNotNull(uid);
			
			// Check if object was created in LDAP and that there is a password
			
			SearchResultEntry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);			
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
    public void test016SearchAccountsIterative() throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        displayTestTile("test016SearchAccountsIterative");

        // GIVEN
        OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName() + ".test016SearchAccountsIterative");

        final String resourceNamespace = resource.asObjectable().getNamespace();
        QName objectClass = new QName(resourceNamespace, "AccountObjectClass");
        QueryType query = QueryUtil.createResourceAndAccountQuery(resource.asObjectable(), objectClass, null);

        final Collection<ObjectType> objects = new HashSet<ObjectType>();

        ResultHandler handler = new ResultHandler<ObjectType>() {

            @Override
            public boolean handle(PrismObject<ObjectType> prismObject, OperationResult parentResult) {
                ObjectType objectType = prismObject.asObjectable();
                objects.add(objectType);

                display("Found object", objectType);

                assertTrue(objectType instanceof AccountShadowType);
                AccountShadowType shadow = (AccountShadowType) objectType;
                assertNotNull(shadow.getOid());
                assertNotNull(shadow.getName());
                assertEquals(new QName(resourceNamespace, "AccountObjectClass"), shadow.getObjectClass());
                assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
                String icfUid = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "uid"));
                assertNotNull("No ICF UID", icfUid);
                String icfName = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "name"));
                assertNotNull("No ICF NAME", icfName);
                assertEquals("Wrong shadow name", shadow.getName(), icfName);
                assertNotNull("Missing LDAP uid", getAttributeValue(shadow, new QName(resourceNamespace, "uid")));
                assertNotNull("Missing LDAP cn", getAttributeValue(shadow, new QName(resourceNamespace, "cn")));
                assertNotNull("Missing LDAP sn", getAttributeValue(shadow, new QName(resourceNamespace, "sn")));
                assertNotNull("Missing activation", shadow.getActivation());
                assertNotNull("Missing activation/enabled", shadow.getActivation().isEnabled());
                assertTrue("Not enabled", shadow.getActivation().isEnabled());
                return true;
            }
        };

        // WHEN

        provisioningService.searchObjectsIterative(AccountShadowType.class, query, null, handler, result);

        // THEN

        display("Count", objects.size());
    }

	@Test
	public void test017DisableAccount() throws Exception{
		display("test017DisableAccount");
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()+"test017DisableAccount");
		try {

			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_DISABLE_SIMULATED, AccountShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_DISABLE_SIMULATED_OID, addedObjectOid);
			

			ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(
					new File("src/test/resources/impl/disable-account-simulated.xml"), ObjectModificationType.class);
			ObjectDelta<AccountShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class, PrismTestUtil.getPrismContext());
			display("Object change",delta);

			provisioningService.modifyObject(AccountShadowType.class, objectChange.getOid(),
					delta.getModifications(), null, result);
			
			AccountShadowType accountType = provisioningService.getObject(AccountShadowType.class,
					ACCOUNT_DISABLE_SIMULATED_OID, result).asObjectable();
			
			display("Object after change",accountType);
			
//			assertFalse("Account was not disabled.", accountType.getActivation().isEnabled());
			
			String uid = ResourceObjectShadowUtil.getSingleStringAttributeValue(accountType, ConnectorFactoryIcfImpl.ICFS_UID);

			
			assertNotNull(uid);
			
			// Check if object was modified in LDAP
			
			SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);
			display("LDAP account", response);
			
			String disabled = openDJController.getAttributeValue(response, "ds-pwp-account-disabled");
			assertNotNull(disabled);

	        System.out.println("ds-pwp-account-disabled after change: " + disabled);

	        assertEquals("ds-pwp-account-disabled not set to \"true\"", "true", disabled);
			
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
				repositoryService.deleteObject(AccountShadowType.class, ACCOUNT_DISABLE_SIMULATED_OID, result);
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
				List<PrismObject<AccountShadowType>> objListType = provisioningService.listObjects(AccountShadowType.class,
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
			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_SEARCH_ITERATIVE, AccountShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_SEARCH_ITERATIVE_OID, addedObjectOid);

			final List<AccountShadowType> objectTypeList = new ArrayList<AccountShadowType>();

			QueryType query = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/impl/query-filter-all-accounts.xml"), QueryType.class);
			provisioningService.searchObjectsIterative(AccountShadowType.class, query, new PagingType(), 
					new ResultHandler<AccountShadowType>() {

				@Override
				public boolean handle(PrismObject<AccountShadowType> object, OperationResult parentResult) {

					return objectTypeList.add(object.asObjectable());
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
			AccountShadowType object = parseObjectTypeFromFile(FILENAME_ACCOUNT_SEARCH, AccountShadowType.class); 
				//unmarshallJaxbFromFile(FILENAME_ACCOUNT_SEARCH);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_SEARCH_OID, addedObjectOid);

			QueryType query = PrismTestUtil.unmarshalObject(new File("src/test/resources/impl/query-filter-all-accounts.xml"), 
					QueryType.class);

			List<PrismObject<AccountShadowType>> objListType = 
				provisioningService.searchObjects(AccountShadowType.class, query, new PagingType(), result);
			
			for (PrismObject<AccountShadowType> objType : objListType) {
				if (objType == null) {
					System.out.println("Object not found in repository.");
				} else {
					System.out.println("found object: " + objType.asObjectable().getName());
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

	
	public void testAddObjectObjectAlreadyExist() throws Exception{
		OperationResult result = new OperationResult(ProvisioningServiceImplOpenDJTest.class.getName()
				+ ".test010AddObject");

		try {
			ObjectType object = unmarshallJaxbFromFile(FILENAME_ACCOUNT_NEW);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_NEW_OID, addedObjectOid);
			
			String addedObjectOid2 = provisioningService.addObject(object.asPrismObject(), null, result);
			assertEquals(ACCOUNT_NEW_OID, addedObjectOid2);

			AccountShadowType accountType =  repositoryService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
					result).asObjectable();
			assertEquals("will", accountType.getName());

			AccountShadowType provisioningAccountType = provisioningService.getObject(AccountShadowType.class, ACCOUNT_NEW_OID,
					result).asObjectable();
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
}
