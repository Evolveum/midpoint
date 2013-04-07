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

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.namespace.QName;

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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

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
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDJ extends AbstractOpenDJTest {
	
	private static Trace LOGGER = TraceManager.getTrace(TestOpenDJ.class);

	@Autowired
	TaskManager taskManager;
	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
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

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+".test003Connection");
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
		ResourceSchema parsedSchema = ResourceSchema.parse(xsdElement, resourceTypeRepoAfter.toString(), prismContext);
		assertNotNull("No schema after parsing",parsedSchema);
		
		ObjectClassComplexTypeDefinition accountDefinition = parsedSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDefinition.findAttributeDefinition(
				new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));
		assertNull("The userPassword attribute sneaked into schema", accountDefinition.findAttributeDefinition(
				new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeRepoAfter),"userPassword")));
		
	}
	
	@Test
	public void test004ResourceAndConnectorCaching() throws Exception {
		displayTestTile("test004ResourceAndConnectorCaching");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+".test004ResourceAndConnectorCaching");
		resource = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		ResourceType resourceType = resource.asObjectable();
		ConnectorInstance configuredConnectorInstance = connectorManager.getConfiguredConnectorInstance(
				resource.asObjectable(), false, result);
		assertNotNull("No configuredConnectorInstance", configuredConnectorInstance);
		ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		assertNotNull("No resource schema", resourceSchema);
		
		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		
		// THEN
		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref",resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeAgain.getConnectorRef().getOid());
		
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);		
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
		assertTrue("Configurations not equals", configurationContainer.equals(configurationContainerAgain));

		ResourceSchema resourceSchemaAgain = RefinedResourceSchema.getResourceSchema(resourceAgain, prismContext);
		assertNotNull("No resource schema (again)", resourceSchemaAgain);
		assertEquals("Schema serial number mismatch", resourceType.getSchema().getCachingMetadata().getSerialNumber(),
				resourceTypeAgain.getSchema().getCachingMetadata().getSerialNumber());
		assertTrue("Resource schema was not cached", resourceSchema == resourceSchemaAgain);
		
		// Now we stick our nose deep inside the provisioning impl. But we need to make sure that the
		// configured connector is properly cached
		ConnectorInstance configuredConnectorInstanceAgain = connectorManager.getConfiguredConnectorInstance(
				resourceTypeAgain, false, result);
		assertTrue("Connector instance was not cached", configuredConnectorInstance == configuredConnectorInstanceAgain);
	}
	
	@Test
	public void test005Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException {
		displayTestTile("test005Capabilities");

		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+".test005Capabilities");

		// WHEN
		ResourceType resource = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result).asObjectable();
		
		// THEN
		display("Resource from provisioninig", resource);
		display("Resource from provisioninig (XML)", PrismTestUtil.serializeObjectToString(resource.asPrismObject()));
		
		CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
		List<Object> nativeCapabilitiesList = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned",nativeCapabilitiesList.isEmpty());
        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(nativeCapabilitiesList, CredentialsCapabilityType.class);
        assertNotNull("credentials capability not found",capCred);
        assertNotNull("password capability not present",capCred.getPassword());
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilitiesList, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it",capAct);
        
        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
        	System.out.println("Capability: "+CapabilityUtil.getCapabilityDisplayName(capability)+" : "+capability);
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
	public void test006ListResourceObjects() throws Exception {
		displayTestTile("test006ListResourceObjects");
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+".test006ListResourceObjects");
		// WHEN
		List<PrismObject<? extends ShadowType>> objectList = provisioningService.listResourceObjects(
				RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, result);
		// THEN
		assertNotNull(objectList);
		assertFalse("Empty list returned",objectList.isEmpty());
		display("Resource object list "+RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS,objectList);
	}

	@Test
	public void test007GetObject() throws Exception {
		displayTestTile("test007GetObject");
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test007GetObject");
		try {

			ShadowType objectToAdd = parseObjectTypeFromFile(ACCOUNT1_FILENAME, ShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(objectToAdd));
			System.out.println(objectToAdd.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(objectToAdd.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT1_OID, addedObjectOid);
			PropertyReferenceListType resolve = new PropertyReferenceListType();

			ShadowType acct = provisioningService.getObject(ShadowType.class, ACCOUNT1_OID, null, result).asObjectable();

			assertNotNull(acct);

			System.out.println(SchemaDebugUtil.prettyPrint(acct));
			System.out.println(acct.asPrismObject().dump());
			
			PrismAsserts.assertEqualsPolyString("Name not equals.", "jbond", acct.getName());
//			assertEquals("jbond", acct.getName());
			
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
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
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test008GetObjectNotFoundRepo");

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, NON_EXISTENT_OID, null, result).asObjectable();
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
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
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
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test009GetObjectNotFoundResource");

		try {
			ObjectType object = provisioningService.getObject(ObjectType.class, ACCOUNT_BAD_OID, null, result).asObjectable();
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
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
		}

	}

	@Test
	public void test010AddObject() throws Exception {
		displayTestTile("test010AddObject");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test010AddObject");

		try {
			ShadowType object = parseObjectTypeFromFile(ACCOUNT_NEW_FILENAME, ShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT_NEW_OID, addedObjectOid);

			ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
					result).asObjectable();
			PrismAsserts.assertEqualsPolyString("Name not equal.", "will", accountType.getName());
//			assertEquals("will", accountType.getName());

			ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_OID,
					null, result).asObjectable();
//			assertEquals("will", provisioningAccountType.getName());
			PrismAsserts.assertEqualsPolyString("Name not equal.", "will", provisioningAccountType.getName());
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_NEW_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	
	@Test
	public void test011AddObjectNull() throws Exception {
		displayTestTile("test011AddObjectNull");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test011AddObjectNull");

		String addedObjectOid = null;
		
		try {
		
			addedObjectOid = provisioningService.addObject(null, null, null, taskManager.createTaskInstance(), result);
			Assert.fail("Expected IllegalArgumentException but haven't got one.");
		} catch(IllegalArgumentException ex){
			assertEquals("Object to add must not be null.", ex.getMessage());
			assertNull(addedObjectOid);
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	
	@Test
	public void test012DeleteObject() throws Exception {
		displayTestTile("test012DeleteObject");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test012DeleteObject");

		try {
			ShadowType object = parseObjectTypeFromFile(ACCOUNT_DELETE_FILENAME, ShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT_DELETE_OID, addedObjectOid);

			provisioningService.deleteObject(ShadowType.class, ACCOUNT_DELETE_OID, null, null, taskManager.createTaskInstance(), result);

			ShadowType objType = null;

			try {
				objType = provisioningService.getObject(ShadowType.class, ACCOUNT_DELETE_OID,
						null, result).asObjectable();
				Assert.fail("Expected exception ObjectNotFoundException, but haven't got one.");
			} catch (ObjectNotFoundException ex) {
				System.out.println("Catched ObjectNotFoundException.");
				assertNull(objType);
			}

			try {
				objType = repositoryService.getObject(ShadowType.class, ACCOUNT_DELETE_OID,
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
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
		}

	}

	@Test
	public void test013ModifyObject() throws Exception {
		displayTestTile("test013ModifyObject");
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test013ModifyObject");

		try {
			
			ShadowType object = unmarshallJaxbFromFile(ACCOUNT_MODIFY_FILENAME, ShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT_MODIFY_OID, addedObjectOid);

			ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(
					new File("src/test/resources/impl/account-change-description.xml"), ObjectModificationType.class);
			ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, ShadowType.class, PrismTestUtil.getPrismContext());
			
			ItemPath icfNamePath = new ItemPath(
					ShadowType.F_ATTRIBUTES, ConnectorFactoryIcfImpl.ICFS_NAME);
			PrismPropertyDefinition icfNameDef = object
					.asPrismObject().getDefinition().findPropertyDefinition(icfNamePath);
			ItemDelta renameDelta = PropertyDelta.createModificationReplaceProperty(icfNamePath, icfNameDef, "uid=rename,ou=People,dc=example,dc=com");
//			renameDelta.addValueToDelete(new PrismPropertyValue("uid=jack,ou=People,dc=example,dc=com"));
			((Collection)delta.getModifications()).add(renameDelta);
			
//			renameDelta = PropertyDelta.createModificationDeleteProperty(icfNamePath, icfNameDef, "uid=jack,ou=People,dc=example,dc=com");
//			((Collection)delta.getModifications()).add(renameDelta);
			
			
			display("Object change",delta);
			provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
					delta.getModifications(), null, null, taskManager.createTaskInstance(), result);
			
			ShadowType accountType = provisioningService.getObject(ShadowType.class,
					ACCOUNT_MODIFY_OID, null, result).asObjectable();
			
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
			
			String uid = ShadowUtil.getSingleStringAttributeValue(accountType, ConnectorFactoryIcfImpl.ICFS_UID);
			List<Object> snValues = ShadowUtil.getAttributeValues(accountType, new QName(RESOURCE_NS, "sn"));
			assertNotNull(snValues);
			assertFalse("Surname attributes must not be empty", snValues.isEmpty());
			assertEquals(1, snValues.size());
			
			String name = ShadowUtil.getSingleStringAttributeValue(accountType, ConnectorFactoryIcfImpl.ICFS_NAME);
			assertEquals("After rename, dn is not equal.", "uid=rename,ou=People,dc=example,dc=com", name);
			assertEquals("shadow name not changed after rename", "uid=rename,ou=People,dc=example,dc=com", accountType.getName().getOrig());
			
			String changedSn = (String) snValues.get(0);
			
			assertNotNull(uid);
			
			// Check if object was modified in LDAP
			
			SearchResultEntry response = openDJController.searchAndAssertByEntryUuid(uid);			
			display("LDAP account", response);
			
			OpenDJController.assertAttribute(response, "sn", "First");
			
			assertEquals("First", changedSn);
			
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_MODIFY_OID, result);
			} catch (Exception ex) {
			}
		}

	}

	@Test
	public void test014ChangePassword() throws Exception {
		displayTestTile("test014ChangePassword");
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test014ChangePassword");

		try {

			ShadowType object = parseObjectTypeFromFile(ACCOUNT_MODIFY_PASSWORD_FILENAME, ShadowType.class);

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);

			assertEquals(ACCOUNT_MODIFY_PASSWORD_OID, addedObjectOid);
			
			ShadowType accountType = provisioningService.getObject(ShadowType.class,
					ACCOUNT_MODIFY_PASSWORD_OID, null, result).asObjectable();
			
			display("Object before password change",accountType);
			
			String uid = null;
			uid = ShadowUtil.getSingleStringAttributeValue(accountType, ConnectorFactoryIcfImpl.ICFS_UID);
			assertNotNull(uid);
			
			SearchResultEntry entryBefore = openDJController.searchAndAssertByEntryUuid(uid);			
			display("LDAP account before", entryBefore);

			String passwordBefore = OpenDJController.getAttributeValue(entryBefore, "userPassword");
			assertNull("Unexpected password before change",passwordBefore);
			
			ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(
					new File("src/test/resources/impl/account-change-password.xml"), ObjectModificationType.class);
			ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, ShadowType.class, PrismTestUtil.getPrismContext());
			display("Object change",delta);

			// WHEN
			provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(), null, null, taskManager.createTaskInstance(), result);

			// THEN
			
			// Check if object was modified in LDAP
			
			SearchResultEntry entryAfter = openDJController.searchAndAssertByEntryUuid(uid);			
			display("LDAP account after", entryAfter);

			String passwordAfter = OpenDJController.getAttributeValue(entryAfter, "userPassword");
			assertNotNull("The password was not changed",passwordAfter);
			
			System.out.println("Changed password: "+passwordAfter);

		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_MODIFY_PASSWORD_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void test015AddObjectWithPassword() throws Exception {
		displayTestTile("test015AddObjectWithPassword");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test015AddObjectWithPassword");

		try {
			ShadowType object = parseObjectTypeFromFile(ACCOUNT_NEW_WITH_PASSWORD_FILENAME, ShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT_NEW_WITH_PASSWORD_OID, addedObjectOid);

			ShadowType accountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
					result).asObjectable();
//			assertEquals("lechuck", accountType.getName());
			PrismAsserts.assertEqualsPolyString("Name not equal.", "lechuck", accountType.getName());

			ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID,
					null, result).asObjectable();
			PrismAsserts.assertEqualsPolyString("Name not equal.", "lechuck", provisioningAccountType.getName());
//			assertEquals("lechuck", provisioningAccountType.getName());
			
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
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_NEW_WITH_PASSWORD_OID, result);
			} catch (Exception ex) {
			}
		}
	}
	
	@Test
    public void test016SearchAccountsIterative() throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, Exception {
        displayTestTile("test016SearchAccountsIterative");

        // GIVEN
        try{
        OperationResult result = new OperationResult(TestOpenDJ.class.getName() + ".test016SearchAccountsIterative");

        final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
        QName objectClass = new QName(resourceNamespace, "AccountObjectClass");
//        QueryType query = QueryUtil.createResourceAndAccountQuery(resource.asObjectable(), objectClass, null);

        ObjectQuery query = ObjectQueryUtil.createResourceAndAccountQuery(resource.getOid(), objectClass, prismContext);
//        AndFilter and = AndFilter.createAnd(Ref.createReferenceEqual(ResourceObjectShadowType.class, ResourceObjectShadowType.F_RESOURCE_REF, prismContext, resource.getOid()),
//        		EqualsFilter.createEqual(ResourceObjectShadowType.class, prismContext, ResourceObjectShadowType.F_OBJECT_CLASS, objectClass));
//        ObjectQuery query = ObjectQuery.createObjectQuery(and);
        
        final Collection<ObjectType> objects = new HashSet<ObjectType>();

        ResultHandler handler = new ResultHandler<ObjectType>() {

            @Override
            public boolean handle(PrismObject<ObjectType> prismObject, OperationResult parentResult) {
                ObjectType objectType = prismObject.asObjectable();
                objects.add(objectType);

                display("Found object", objectType);

                assertTrue(objectType instanceof ShadowType);
                ShadowType shadow = (ShadowType) objectType;
                assertNotNull(shadow.getOid());
                assertNotNull(shadow.getName());
                assertEquals(new QName(resourceNamespace, "AccountObjectClass"), shadow.getObjectClass());
                assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
                String icfUid = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "uid"));
                assertNotNull("No ICF UID", icfUid);
                String icfName = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "name"));
                assertNotNull("No ICF NAME", icfName);
                PrismAsserts.assertEqualsPolyString("Wrong shadow name.", icfName, shadow.getName());
//                assertEquals("Wrong shadow name", shadow.getName(), icfName);
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

      
        provisioningService.searchObjectsIterative(ShadowType.class, query, handler, result);
        
        display("Count", objects.size());
        } catch(Exception ex){
        	LOGGER.info("ERROR: {}", ex.getMessage(), ex);
        	throw ex;
        }
        // THEN

        
    }

	@Test
	public void test017DisableAccount() throws Exception{
		display("test017DisableAccount");
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()+"test017DisableAccount");
		try {

			ShadowType object = parseObjectTypeFromFile(ACCOUNT_DISABLE_SIMULATED_FILENAME, ShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT_DISABLE_SIMULATED_OID, addedObjectOid);
			

			ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(
					new File("src/test/resources/impl/disable-account-simulated.xml"), ObjectModificationType.class);
			ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, ShadowType.class, PrismTestUtil.getPrismContext());
			display("Object change",delta);

			provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
					delta.getModifications(), null, null, taskManager.createTaskInstance(), result);
			
			ShadowType accountType = provisioningService.getObject(ShadowType.class,
					ACCOUNT_DISABLE_SIMULATED_OID, null, result).asObjectable();
			
			display("Object after change",accountType);
			
//			assertFalse("Account was not disabled.", accountType.getActivation().isEnabled());
			
			String uid = ShadowUtil.getSingleStringAttributeValue(accountType, ConnectorFactoryIcfImpl.ICFS_UID);

			
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
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_DISABLE_SIMULATED_OID, result);
			} catch (Exception ex) {
			}
		}


	}
	
	@Test
	public void test200SearchObjectsIterative() throws Exception {
		displayTestTile("test200SearchObjectsIterative");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".searchObjectsIterativeTest");
		try {
			ShadowType object = parseObjectTypeFromFile(ACCOUNT_SEARCH_ITERATIVE_FILENAME, ShadowType.class);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT_SEARCH_ITERATIVE_OID, addedObjectOid);

			final List<ShadowType> objectTypeList = new ArrayList<ShadowType>();

			QueryType queryType = PrismTestUtil.unmarshalObject(new File(
					"src/test/resources/impl/query-filter-all-accounts.xml"), QueryType.class);
			ObjectQuery query = QueryConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);
			
			provisioningService.searchObjectsIterative(ShadowType.class, query, new ResultHandler<ShadowType>() {

				@Override
				public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {

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
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_SEARCH_ITERATIVE_OID, result);
			} catch (Exception ex) {
			}
		}
	}

	@Test
	public void test201SearchObjects() throws Exception {
		displayTestTile("test201SearchObjects");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test201SearchObjects");

		try {
			ShadowType object = parseObjectTypeFromFile(ACCOUNT_SEARCH_FILENAME, ShadowType.class); 
				//unmarshallJaxbFromFile(FILENAME_ACCOUNT_SEARCH);

			System.out.println(SchemaDebugUtil.prettyPrint(object));
			System.out.println(object.asPrismObject().dump());

			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			assertEquals(ACCOUNT_SEARCH_OID, addedObjectOid);

			QueryType queryType = PrismTestUtil.unmarshalObject(new File("src/test/resources/impl/query-filter-all-accounts.xml"), 
					QueryType.class);
			ObjectQuery query = QueryConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);

			List<PrismObject<ShadowType>> objListType = 
				provisioningService.searchObjects(ShadowType.class, query, result);
			
			for (PrismObject<ShadowType> objType : objListType) {
				if (objType == null) {
					System.out.println("Object not found in repository.");
				} else {
					System.out.println("found object: " + objType.asObjectable().getName());
				}
			}
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			//do not delete the account to search, it will be used in the next test
//			try {
//				repositoryService.deleteObject(ResourceObjectShadowType.class, ACCOUNT_SEARCH_OID, result);
//			} catch (Exception ex) {
//			}
		}
	}

	@Test
	public void test202SearchObjectsCompexFilter() throws Exception {
		displayTestTile("test202SearchObjectsCompexFilter");

		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test202SearchObjectsCompexFilter");

		try {

			QueryType queryType = PrismTestUtil.unmarshalObject(new File("src/test/resources/impl/query-complex-filter.xml"), 
					QueryType.class);
			ObjectQuery query = QueryConvertor.createObjectQuery(ShadowType.class, queryType, prismContext);

			List<PrismObject<ShadowType>> objListType = 
				provisioningService.searchObjects(ShadowType.class, query, result);
			
			for (PrismObject<ShadowType> objType : objListType) {
				if (objType == null) {
					System.out.println("Object not found in repository.");
				} else {
					System.out.println("found object: " + objType.asObjectable().getName());
				}
			}
		} finally {
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT1_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_BAD_OID, result);
			} catch (Exception ex) {
			}
			try {
				repositoryService.deleteObject(ShadowType.class, ACCOUNT_SEARCH_OID, result);
			} catch (Exception ex) {
			}
		}
	}
	
	/**
	 * The exception comes from the resource. There is no shadow for this object.
	 */
	@Test
	public void test300AddObjectObjectAlreadyExistResource() throws Exception{
		displayTestTile("test300AddObjectObjectAlreadyExistResource");
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test300AddObjectObjectAlreadyExist");
		
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_NEW_FILENAME));
		display("Account to add", account);
		
		try {
			// WHEN
			provisioningService.addObject(account, null, null, taskManager.createTaskInstance(), result);
			
			AssertJUnit.fail("Expected addObject operation to fail but it was successful");
			
		} catch (ObjectAlreadyExistsException e) {
			// This is expected
			display("Expected exception", e);
			
			// The exception should originate from the LDAP layers
			IntegrationTestTools.assertInMessageRecursive(e, "LDAP");
		}
		
		// TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

	}
	
	@Test
	public void test310AddObjectNoSn() throws Exception{
		displayTestTile("test310AddObjectNoSn");
		
		OperationResult result = new OperationResult(TestOpenDJ.class.getName()
				+ ".test300AddObjectObjectAlreadyExist");

		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_NO_SN_FILENAME));
		display("Account to add", account);
		
		try {
			// WHEN
			provisioningService.addObject(account, null, null, taskManager.createTaskInstance(), result);
			
			AssertJUnit.fail("Expected addObject operation to fail but it was successful");
			
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
			
			// This error should be detectable before it reaches a resource. Therefore we check that the
			// cause was not a LDAP exception
			
			// MID-1007
//			IntegrationTestTools.assertNotInMessageRecursive(e, "LDAP");
		}
		
		// TODO: search to check that the shadow with the same NAME exists (search for OID will not do)

	}
	
}
