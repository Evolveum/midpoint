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
package com.evolveum.midpoint.provisioning.impl.opendj;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

/**
 * Test for provisioning service implementation. Using OpenDJ. But NOT STARTING IT.
 * Checking if appropriate errors are provided.
 */

@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestOpenDjNegative extends AbstractOpenDjTest {
	
	private static Trace LOGGER = TraceManager.getTrace(TestOpenDjNegative.class);

	@Autowired
	TaskManager taskManager;
	
//	@Autowired
//	private ResourceObjectChangeListener syncServiceMock;

		
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddShadowFromFile(ACCOUNT1_REPO_FILE, initResult);
		repoAddShadowFromFile(ACCOUNT_SPARROW_REPO_FILE, initResult);
		repoAddShadowFromFile(ACCOUNT_JACK_REPO_FILE, initResult);
	}
	
	@BeforeClass
	public static void stoptLdap() throws Exception {
		// Make sure that OpenDJ is stopped. We want to see the blood .. err ... errors
		try {
			openDJController.stop();
		} catch (Exception ex) {
			LOGGER.trace("Exeception during stopping already stopped LDAP (probably harmless)", ex);
		}
	}
	
	@Test
	public void test003Connection() throws Exception {
		final String TEST_NAME = "test003Connection";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		ResourceType resourceTypeBefore = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result).asObjectable();
		display("Resource before testResource (repository)", resourceTypeBefore);
		assertNotNull("No connector ref",resourceTypeBefore.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeBefore.getConnectorRef().getOid());
		connector = repositoryService.getObject(ConnectorType.class, resourceTypeBefore.getConnectorRef().getOid(), null, result);
		ConnectorType connectorType = connector.asObjectable();
		assertNotNull(connectorType);
		XmlSchemaType xmlSchemaTypeBefore = resourceTypeBefore.getSchema();
		AssertJUnit.assertNull("Found schema before test connection. Bad test setup?", xmlSchemaTypeBefore);
		Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchema(resourceTypeBefore);
		AssertJUnit.assertNull("Found schema element before test connection. Bad test setup?", resourceXsdSchemaElementBefore);
		
		// WHEN
		OperationResult	operationResult = provisioningService.testResource(RESOURCE_OPENDJ_OID, task);
		
		display("Test connection result (expected failure)",operationResult);
		TestUtil.assertFailure(operationResult);
		
		PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, result);
		display("Resource after testResource (repository)", resourceRepoAfter);
		ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
		display("Resource after testResource (repository, XML)", PrismTestUtil.serializeObjectToString(resourceTypeRepoAfter.asPrismObject(), PrismContext.LANG_XML));
		
		XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
		assertNull("The schema was generated after test connection but it should not be",xmlSchemaTypeAfter);
		Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchema(resourceTypeRepoAfter);
		assertNull("Schema after test connection (and should not be)", resourceXsdSchemaElementAfter);		
	}
	
	@Test
	public void test004ResourceAndConnectorCaching() throws Exception {
		TestUtil.displayTestTile("test004ResourceAndConnectorCaching");

		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()+".test004ResourceAndConnectorCaching");

		Task task = taskManager.createTaskInstance();
		// WHEN
		// This should NOT throw an exception. It should just indicate the failure in results
		resource = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, task, result);
		ResourceType resourceType = resource.asObjectable();

		// THEN
		result.computeStatus();
		display("getObject(resource) result", result);
		TestUtil.assertFailure(result);
		TestUtil.assertFailure(resource.asObjectable().getFetchResult());
		
		ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		assertNull("Resource schema found", resourceSchema);
		
		// WHEN
		PrismObject<ResourceType> resourceAgain = provisioningService.getObject(ResourceType.class,RESOURCE_OPENDJ_OID, null, task, result);
		
		// THEN
		result.computeStatus();
		display("getObject(resourceAgain) result", result);
		TestUtil.assertFailure(result);
		TestUtil.assertFailure(resourceAgain.asObjectable().getFetchResult());
		
		ResourceType resourceTypeAgain = resourceAgain.asObjectable();
		assertNotNull("No connector ref",resourceTypeAgain.getConnectorRef());
		assertNotNull("No connector ref OID",resourceTypeAgain.getConnectorRef().getOid());
		
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		PrismContainer<Containerable> configurationContainerAgain = resourceAgain.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);		
		assertTrue("Configurations not equivalent", configurationContainer.equivalent(configurationContainerAgain));
		assertTrue("Configurations not equals", configurationContainer.equals(configurationContainerAgain));

		ResourceSchema resourceSchemaAgain = RefinedResourceSchemaImpl.getResourceSchema(resourceAgain, prismContext);
		assertNull("Resource schema (again)", resourceSchemaAgain);
	}
	
	/**
	 * This goes to local repo, therefore the expected result is ObjectNotFound.
	 * We know that the shadow does not exist. 
	 */
	@Test
	public void test110GetObjectNoShadow() throws Exception {
		final String TEST_NAME = "test110GetObjectNoShadow";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		try {
			ShadowType acct = provisioningService.getObject(ShadowType.class, NON_EXISTENT_OID, null, taskManager.createTaskInstance(), result).asObjectable();
			
			AssertJUnit.fail("getObject succeeded unexpectedly");
		} catch (ObjectNotFoundException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}

	/**
	 * This is using the shadow to go to the resource. But it cannot as OpenDJ is down.
	 * It even cannot fetch schema. If there is no schema it does not even know how to process
	 * identifiers in the shadow. Therefore the expected result is ConfigurationException (CommunicationException).
	 * It must not be ObjectNotFound as we do NOT know that the shadow does not exist. 
	 */
	@Test
	public void test111GetObjectShadow() throws Exception {
		final String TEST_NAME = "test111GetObjectShadow";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);
				
		try {

			ShadowType acct = provisioningService.getObject(ShadowType.class, ACCOUNT1_OID, null, taskManager.createTaskInstance(), result).asObjectable();

			AssertJUnit.fail("getObject succeeded unexpectedly");
//		} catch (CommunicationException e) {
		} catch (ConfigurationException e){
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}
	
	@Test
	public void test120ListResourceObjects() throws Exception {
		final String TEST_NAME = "test120ListResourceObjects";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);
		
		try {
			// WHEN
			List<PrismObject<? extends ShadowType>> objectList = provisioningService.listResourceObjects(
					RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, null, result);
			
			AssertJUnit.fail("listResourceObjects succeeded unexpectedly");
		} catch (ConfigurationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}
	
	@Test
	public void test121SearchAccounts() throws SchemaException, ObjectNotFoundException,
          CommunicationException, ConfigurationException, SecurityViolationException, Exception {
		final String TEST_NAME = "test121SearchAccounts";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

      final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
      QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

      ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);
      
      try {
    	  
	      // WHEN
	      provisioningService.searchObjects(ShadowType.class, query, null, null, result);
	      
	      AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
		} catch (ConfigurationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display(result);
		TestUtil.assertFailure(result);
  	}
	
	@Test
	public void test122SearchAccountsIterative() throws SchemaException, ObjectNotFoundException,
          CommunicationException, ConfigurationException, SecurityViolationException, Exception {
		final String TEST_NAME = "test122SearchAccountsIterative";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

      final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
      QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

      ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);
      
      ResultHandler handler = new ResultHandler<ObjectType>() {
          @Override
          public boolean handle(PrismObject<ObjectType> prismObject, OperationResult parentResult) {
        	  AssertJUnit.fail("handler called unexpectedly");
        	  return false;
          }
      };

      try {
    	  
	      // WHEN
	      provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, null, result);
	      
	      AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
		} catch (ConfigurationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
  	}
	
	@Test
	public void test130AddAccountWill() throws Exception {
		final String TEST_NAME = "test130AddAccountWill";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		ShadowType object = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);

		display("Account to add", object);

		try {
			// WHEN
			String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, taskManager.createTaskInstance(), result);
			
			AssertJUnit.fail("addObject succeeded unexpectedly");
		} catch (ConfigurationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}

	
	@Test
	public void test140AddDeleteAccountSparrow() throws Exception {
		final String TEST_NAME = "test140AddDeleteAccountSparrow";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		try {

			provisioningService.deleteObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, null, taskManager.createTaskInstance(), result);

			AssertJUnit.fail("addObject succeeded unexpectedly");
		} catch (ConfigurationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);

	}
	
	@Test
	public void test150ModifyObject() throws Exception {
		final String TEST_NAME = "test150ModifyObject";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(ACCOUNT_JACK_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, ShadowType.class, PrismTestUtil.getPrismContext());
		display("Object change",delta);
		
		try {

			provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
					delta.getModifications(), null, null, taskManager.createTaskInstance(), result);
			
			AssertJUnit.fail("addObject succeeded unexpectedly");
		} catch (ConfigurationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}
	
	@Test
	public void test190Synchronize() throws Exception {
		final String TEST_NAME = "test190Synhronize";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_OPENDJ_OID, 
				new QName(RESOURCE_NS, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
		
		try {

			provisioningService.synchronize(coords, task, result);
			
			AssertJUnit.fail("addObject succeeded unexpectedly");
		} catch (CommunicationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}

	
	// =========================================================================================================
	// Now lets replace the resource with one that has schema and capabilities. And re-run some of the tests.
	// OpenDJ is still down so the results should be the same. But the code may take a different path if
	// schema is present.
	// =========================================================================================================
	
	@Test
	public void test500ReplaceResource() throws Exception {
		final String TEST_NAME = "test500ReplaceResource";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		// Delete should work fine even though OpenDJ is down
		provisioningService.deleteObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, null, taskManager.createTaskInstance(), result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		resource = addResourceFromFile(RESOURCE_OPENDJ_INITIALIZED_FILE, IntegrationTestTools.CONNECTOR_LDAP_TYPE, result);

		result.computeStatus();
		TestUtil.assertSuccess(result);

	}
	
	/**
	 * This goes to local repo, therefore the expected result is ObjectNotFound.
	 * We know that the shadow does not exist. 
	 */
	@Test
	public void test510GetObjectNoShadow() throws Exception {
		final String TEST_NAME = "test510GetObjectNoShadow";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		try {
			ShadowType acct = provisioningService.getObject(ShadowType.class, NON_EXISTENT_OID, null, taskManager.createTaskInstance(), result).asObjectable();
			
			AssertJUnit.fail("getObject succeeded unexpectedly");
		} catch (ObjectNotFoundException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}

	/**
	 * This is using the shadow to go to the resource. But it cannot as OpenDJ is down. 
	 * Therefore the expected result is CommunicationException. It must not be ObjectNotFound as 
	 * we do NOT know that the shadow does not exist.
	 * Provisioning should return a repo shadow and indicate the result both in operation result and
	 * in fetchResult in the returned shadow.
	 */
	@Test
	public void test511GetObjectShadow() throws Exception {
		final String TEST_NAME = "test511GetObjectShadow";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);
				
		PrismObject<ShadowType> acct = provisioningService.getObject(ShadowType.class, ACCOUNT1_OID, null, taskManager.createTaskInstance(), result);

		display("Account", acct);
		
		result.computeStatus();
		display("getObject result", result);
		assertEquals("Expected result partial error but was "+result.getStatus(), 
				OperationResultStatus.PARTIAL_ERROR, result.getStatus());
		
		OperationResultType fetchResult = acct.asObjectable().getFetchResult();
		display("getObject fetchResult", fetchResult);
		assertEquals("Expected fetchResult partial error but was "+result.getStatus(), 
				OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
	}

	/**
	 * This is using the shadow to go to the resource. But it cannot as OpenDJ is down. 
	 * Therefore the expected result is CommunicationException. It must not be ObjectNotFound as 
	 * we do NOT know that the shadow does not exist. 
	 */
	@Test
	public void test520ListResourceObjects() throws Exception {
		final String TEST_NAME = "test520ListResourceObjects";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);
		
		try {
			// WHEN
			List<PrismObject<? extends ShadowType>> objectList = provisioningService.listResourceObjects(
					RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, null, null, result);
			
			AssertJUnit.fail("listResourceObjects succeeded unexpectedly");
		} catch (CommunicationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}
	
	@Test
	public void test521SearchAccounts() throws SchemaException, ObjectNotFoundException,
          CommunicationException, ConfigurationException, SecurityViolationException, Exception {
		final String TEST_NAME = "test521SearchAccounts";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

      final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
      QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

      ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);
      
      try {
    	  
	      // WHEN
	      provisioningService.searchObjects(ShadowType.class, query, null, null, result);
	      
	      AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
		} catch (CommunicationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
  	}
	
	@Test
	public void test522SearchAccountsIterative() throws SchemaException, ObjectNotFoundException,
          CommunicationException, ConfigurationException, SecurityViolationException, Exception {
		final String TEST_NAME = "test522SearchAccountsIterative";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

      final String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
      QName objectClass = new QName(resourceNamespace, OBJECT_CLASS_INETORGPERSON_NAME);

      ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(resource.getOid(), objectClass, prismContext);
      
      ResultHandler handler = new ResultHandler<ObjectType>() {
          @Override
          public boolean handle(PrismObject<ObjectType> prismObject, OperationResult parentResult) {
        	  AssertJUnit.fail("handler called unexpectedly");
        	  return false;
          }
      };

      try {
    	  
	      // WHEN
	      provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, null, result);
	      
	      AssertJUnit.fail("searchObjectsIterative succeeded unexpectedly");
		} catch (CommunicationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		display(result);
		TestUtil.assertFailure(result);
  	}
	
	@Test
	public void test530AddAccountWill() throws Exception {
		final String TEST_NAME = "test530AddAccountWill";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		ShadowType object = parseObjectType(ACCOUNT_WILL_FILE, ShadowType.class);
		display("Account to add", object);

		// WHEN
		String addedObjectOid = provisioningService.addObject(object.asPrismObject(), null, null, task, result);
		
		// THEN
		result.computeStatus();
		display("addObject result", result);
		assertEquals("Wrong result", OperationResultStatus.HANDLED_ERROR, result.getStatus());
		
		assertEquals(ACCOUNT_WILL_OID, addedObjectOid);

		ShadowType repoAccountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, result).asObjectable();
		display("repo shadow", repoAccountType);
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_WILL_DN, repoAccountType.getName());
		assertEquals("Wrong failedOperationType in repo", FailedOperationTypeType.ADD, repoAccountType.getFailedOperationType());
		OperationResultType repoResult = repoAccountType.getResult();
		assertNotNull("No result in shadow (repo)", repoResult);
		TestUtil.assertFailure("Result in shadow (repo)", repoResult);

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID,
				null, task, result).asObjectable();
		display("provisioning shadow", provisioningAccountType);
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_WILL_DN, provisioningAccountType.getName());
		assertEquals("Wrong failedOperationType in repo", FailedOperationTypeType.ADD, provisioningAccountType.getFailedOperationType());
		OperationResultType provisioningResult = provisioningAccountType.getResult();
		assertNotNull("No result in shadow (repo)", provisioningResult);
		TestUtil.assertFailure("Result in shadow (repo)", provisioningResult);

	}
	
	@Test
	public void test540DeleteObject() throws Exception {
		final String TEST_NAME = "test540DeleteObject";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		Task task = taskManager.createTaskInstance();
		// WHEN
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_SPARROW_OID, null, null, task, result);

		// THEN
		result.computeStatus();
		display("deleteObject result", result);
		assertEquals("Wrong result", OperationResultStatus.HANDLED_ERROR, result.getStatus());
		
		ShadowType repoAccountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_SPARROW_OID,
				null, result).asObjectable();
		display("repo shadow", repoAccountType);
		assertEquals("Wrong failedOperationType in repo", FailedOperationTypeType.DELETE, repoAccountType.getFailedOperationType());
		OperationResultType repoResult = repoAccountType.getResult();
		assertNotNull("No result in shadow (repo)", repoResult);
		display("repoResult in shadow", repoResult);
		TestUtil.assertFailure("Result in shadow (repo)", repoResult);

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_SPARROW_OID,
				null, task, result).asObjectable();
		display("provisioning shadow", provisioningAccountType);
		assertEquals("Wrong failedOperationType in repo", FailedOperationTypeType.DELETE, provisioningAccountType.getFailedOperationType());
		OperationResultType provisioningResult = provisioningAccountType.getResult();
		assertNotNull("No result in shadow (repo)", provisioningResult);
		TestUtil.assertFailure("Result in shadow (repo)", provisioningResult);		
	}

	@Test
	public void test550ModifyObject() throws Exception {
		final String TEST_NAME = "test150ModifyObject";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		OperationResult result = new OperationResult(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);

		ObjectModificationType objectChange = PrismTestUtil.parseAtomicValue(ACCOUNT_JACK_CHANGE_FILE, ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<ShadowType> delta = DeltaConvertor.createObjectDelta(objectChange, ShadowType.class, PrismTestUtil.getPrismContext());
		display("Object change",delta);
		
		Task task = taskManager.createTaskInstance();
		provisioningService.modifyObject(ShadowType.class, objectChange.getOid(),
				delta.getModifications(), null, null, task, result);
		
	
		// THEN
		result.computeStatus();
		display("deleteObject result", result);
		assertEquals("Wrong result", OperationResultStatus.HANDLED_ERROR, result.getStatus());
		
		ShadowType repoAccountType =  repositoryService.getObject(ShadowType.class, ACCOUNT_JACK_OID,
				null, result).asObjectable();
		display("repo shadow", repoAccountType);
		assertEquals("Wrong failedOperationType in repo", FailedOperationTypeType.MODIFY, repoAccountType.getFailedOperationType());
		OperationResultType repoResult = repoAccountType.getResult();
		assertNotNull("No result in shadow (repo)", repoResult);
		TestUtil.assertFailure("Result in shadow (repo)", repoResult);

		ShadowType provisioningAccountType = provisioningService.getObject(ShadowType.class, ACCOUNT_JACK_OID,
				null, task, result).asObjectable();
		display("provisioning shadow", provisioningAccountType);
		assertEquals("Wrong failedOperationType in repo", FailedOperationTypeType.MODIFY, provisioningAccountType.getFailedOperationType());
		OperationResultType provisioningResult = provisioningAccountType.getResult();
		assertNotNull("No result in shadow (repo)", provisioningResult);
		TestUtil.assertFailure("Result in shadow (repo)", provisioningResult);
		
	}
	
	@Test
	public void test590Synchronize() throws Exception {
		final String TEST_NAME = "test590Synhronize";
		TestUtil.displayTestTile(TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance(TestOpenDjNegative.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();
		
		ResourceShadowDiscriminator coords = new ResourceShadowDiscriminator(RESOURCE_OPENDJ_OID, 
				new QName(RESOURCE_NS, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME));
		
		try {

			provisioningService.synchronize(coords,
					task, result);
			
			AssertJUnit.fail("addObject succeeded unexpectedly");
		} catch (CommunicationException e) {
			// This is expected
			display("Expected exception", e);
		}
		
		result.computeStatus();
		TestUtil.assertFailure(result);
	}


}
