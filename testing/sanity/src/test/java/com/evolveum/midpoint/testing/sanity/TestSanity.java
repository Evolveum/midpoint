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
package com.evolveum.midpoint.testing.sanity;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.Attribute;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * Sanity test suite.
 * 
 * It tests the very basic representative test cases. It does not try to be
 * complete. It rather should be quick to execute and pass through the most
 * representative cases. It should test all the system components except for
 * GUI. Therefore the test cases are selected to pass through most of the
 * components.
 * 
 * It is using mock BaseX repository and embedded OpenDJ instance as a testing
 * resource. The BaseX repository is instantiated from the Spring context in the
 * same way as all other components. OpenDJ instance is started explicitly using
 * BeforeClass method. Appropriate resource definition to reach the OpenDJ
 * instance is provided in the test data and is inserted in the repository as
 * part of test initialization.
 * 
 * @author Radovan Semancik
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-provisioning.xml", "classpath:application-context-sanity-test.xml", "classpath:application-context-task.xml" })
public class TestSanity extends OpenDJUnitTestAdapter {

	private static final String SYSTEM_CONFIGURATION_FILENAME = "src/test/resources/repo/system-configuration.xml";
	private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

	private static final String RESOURCE_OPENDJ_FILENAME = "src/test/resources/repo/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	
	private static final String TASK_OPENDJ_SYNC_FILENAME = "src/test/resources/repo/opendj-sync-task.xml";
	private static final String TASK_OPENDJ_SYNC_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = "src/test/resources/repo/sample-configuration-object.xml";
	private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";
	
	private static final String USER_JACK_FILENAME = "src/test/resources/repo/user-jack.xml";
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	private static final String USER_JACK_LDAP_UID = "jack";

	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_FILENAME = "src/test/resources/request/user-modify-add-account.xml";
	private static final String REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME = "src/test/resources/request/user-modify-fullname-locality.xml";
	
	private static final QName IMPORT_OBJECTCLASS = new QName("http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff","AccountObjectClass");
	
	/**
	 * Utility to control embedded OpenDJ instance (start/stop)
	 */
	protected static OpenDJUtil djUtil;

	/**
	 * Unmarshalled resource definition to reach the embedded OpenDJ instance.
	 * Used for convenience - the tests method may find it handy.
	 */
	private static ResourceType resource;
	private static JAXBContext jaxbctx;
	private static Unmarshaller unmarshaller;
	private static String shadowOid;

	/**
	 * The instance of ModelService. This is the interface that we will test.
	 */
	@Autowired(required = true)
	private ModelPortType model;

	@Autowired(required = true)
	private RepositoryService repositoryService;
	private static boolean repoInitialized = false;
	
	@Autowired(required = true)
	private TaskManager taskManager;

	public TestSanity() throws JAXBException {
		djUtil = new OpenDJUtil();
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
	}

	/**
	 * Initialize embedded OpenDJ instance
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void startLdap() throws Exception {
		startACleanDJ();
	}

	/**
	 * Shutdown embedded OpenDJ instance
	 * 
	 * @throws Exception
	 */
	@AfterClass
	public static void stopLdap() throws Exception {
		stopDJ();
	}

	// We need this complicated init as we want to initialize repo only once.
	// JUnit will
	// create new class instance for every test, so @Before and @PostInit will
	// not work
	// directly. We also need to init the repo after spring autowire is done, so
	// @BeforeClass won't work either.
	@Before
	public void initRepository() throws Exception {
		if (!repoInitialized) {
			resource = (ResourceType) addObjectFromFile(RESOURCE_OPENDJ_FILENAME);
			addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME);
			addObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME);
			repoInitialized = true;
		}
	}

	/**
	 * Test integrity of the test setup.
	 * 
	 * @throws SchemaException
	 * @throws ObjectNotFoundException
	 */
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		assertNotNull(resource);
		assertNotNull(model);
		assertNotNull(repositoryService);

		OperationResult result = new OperationResult(TestSanity.class.getName() + ".test000Integrity");
		ObjectType object = repositoryService.getObject(RESOURCE_OPENDJ_OID, null, result);
		assertTrue(object instanceof ResourceType);
		assertEquals(RESOURCE_OPENDJ_OID, object.getOid());

		// TODO: test if OpenDJ is running
	}

	/**
	 * Test the testResource method. Expect a complete success for now.
	 * 
	 * TODO: better check for the returned result. Look inside and check if all
	 * the expected tests were run.
	 * 
	 * @throws FaultMessage
	 * @throws JAXBException
	 */
	@Test
	public void test001TestConnection() throws FaultMessage, JAXBException {
		// GIVEN
		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.testResource(RESOURCE_OPENDJ_OID, holder);

		// THEN
		displayJaxb(result, new QName("result"));

		assertSuccess(result.getPartialResults().get(0));
	}

	private void assertSuccess(OperationResultType result) {
		assertEquals(OperationResultStatusType.SUCCESS, result.getStatus());
		List<OperationResultType> partialResults = result.getPartialResults();
		for (OperationResultType subResult : partialResults) {
			assertSuccess(subResult);
		}
	}

	/**
	 * Attempt to add new user. It is only added to the repository, so check if
	 * it is in the repository after the operation.
	 */
	@Ignore
	@Test
	public void test002AddUser() throws FileNotFoundException, JAXBException, FaultMessage, ObjectNotFoundException,
			SchemaException {
		// GIVEN
		UserType user = unmarshallJaxbFromFile(USER_JACK_FILENAME, UserType.class);

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		String oid = model.addObject(user, holder);

		// THEN

		assertEquals(USER_JACK_OID, oid);

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType repoObject = repositoryService.getObject(oid, resolve, repoResult);
		assertEquals(USER_JACK_OID, repoObject.getOid());
		UserType repoUser = (UserType) repoObject;
		assertEquals(user.getFullName(), repoUser.getFullName());

		// TODO: better checks
	}

	/**
	 * Add account to user. This should result in account provisioning. Check if that happens in repo and in LDAP.
	 */
	@Ignore
	@Test
	public void test003AddAccountToUser() throws FileNotFoundException, JAXBException, FaultMessage, ObjectNotFoundException,
			SchemaException, DirectoryException {
		// GIVEN

		ObjectModificationType objectChange = unmarshallJaxbFromFile(REQUEST_USER_MODIFY_ADD_ACCOUNT_FILENAME,
				ObjectModificationType.class);

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.modifyObject(objectChange, holder);

		// THEN

		// Check if user object was modified in the repo

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType repoObject = repositoryService.getObject(USER_JACK_OID, resolve, repoResult);
		UserType repoUser = (UserType) repoObject;
		displayJaxb(repoUser, new QName("user"));
		List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
		assertEquals(1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		shadowOid = accountRef.getOid();
		assertFalse(shadowOid.isEmpty());

		// Check if shadow was created in the repo

		repoResult = new OperationResult("getObject");
		repoObject = repositoryService.getObject(shadowOid, resolve, repoResult);
		AccountShadowType repoShadow = (AccountShadowType) repoObject;
		displayJaxb(repoShadow, new QName("shadow"));
		assertNotNull(repoShadow);
		assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

		// check attributes in the shadow: should be only identifiers (ICF UID)

		String uid = null;
		boolean hasOthers = false;
		List<Element> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Element element : xmlAttributes) {
			if (element.getNamespaceURI().equals(SchemaConstants.ICFS_UID.getNamespaceURI())
					&& element.getLocalName().equals(SchemaConstants.ICFS_UID.getLocalPart())) {
				if (uid != null) {
					fail("Multiple values for ICF UID in shadow attributes");
				} else {
					uid = element.getTextContent();
				}
			} else {
				hasOthers = true;
			}
		}

		assertFalse(hasOthers);
		assertNotNull(uid);

		// check if account was created in LDAP

		// Set<String> attributes = new HashSet<String>();
		// attributes.add(
		// "ds-pwp-account-disabled");
		// attributes.add(
		// "givenName");
		InternalSearchOperation op = controller.getInternalConnection().processSearch(
				"dc=example,dc=com",
				SearchScope.WHOLE_SUBTREE, 
				DereferencePolicy.NEVER_DEREF_ALIASES, 
				100, 
				100, 
				false, 
				"(entryUUID=" + uid + ")",
				null);

		assertEquals(1, op.getEntriesSent());
		SearchResultEntry response = op.getSearchEntries().get(0);

		display(response);
		
		assertAttribute(response, "uid", "jack");
		assertAttribute(response, "givenName", "Jack");
		assertAttribute(response, "sn", "Sparrow");
		assertAttribute(response, "cn", "Jack Sparrow");
		// The "l" attribute is assigned indirectly through schemaHandling and config object
		assertAttribute(response, "l", "middle of nowhere");
		
		// Use getObject to test fetch of complete shadow

		result = new OperationResultType();
		holder.value = result;

		ObjectType modelObject = model.getObject(shadowOid, resolve, holder);
		AccountShadowType modelShadow = (AccountShadowType) modelObject;
		displayJaxb(modelShadow, new QName("shadow"));
		assertNotNull(modelShadow);
		assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

		assertAttribute(repoShadow, resource, "uid", "jack");
		assertAttribute(repoShadow, resource, "givenName", "Jack");
		assertAttribute(repoShadow, resource, "sn", "Sparrow");
		assertAttribute(repoShadow, resource, "cn", "Jack Sparrow");
		assertAttribute(repoShadow, resource, "l", "middle of nowhere");
	}
	

	/**
	 * We are going to modify the user. As the user has an account, the user changes
	 * should be also applied to the account (by schemaHandling).
	 * @throws DirectoryException 
	 */
	@Ignore
	@Test
	public void test004modifyUser() throws FileNotFoundException, JAXBException, FaultMessage, ObjectNotFoundException, SchemaException, DirectoryException {
		// GIVEN

		ObjectModificationType objectChange = unmarshallJaxbFromFile(REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME,
				ObjectModificationType.class);

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.modifyObject(objectChange, holder);

		// THEN

		// Check if user object was modified in the repo

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType repoObject = repositoryService.getObject(USER_JACK_OID, resolve, repoResult);
		UserType repoUser = (UserType) repoObject;
		displayJaxb(repoUser, new QName("user"));
		
		assertEquals("Cpt. Jack Sparrow", repoUser.getFullName());
		assertEquals("somewhere", repoUser.getLocality());

		// Check if appropriate accountRef is still there
		
		List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
		assertEquals(1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		String newShadowOid = accountRef.getOid();
		assertEquals(shadowOid,newShadowOid);
				
		// Check if shadow is still in the repo and that it is untouched

		repoResult = new OperationResult("getObject");
		repoObject = repositoryService.getObject(shadowOid, resolve, repoResult);
		AccountShadowType repoShadow = (AccountShadowType) repoObject;
		displayJaxb(repoShadow, new QName("shadow"));
		assertNotNull(repoShadow);
		assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

		// check attributes in the shadow: should be only identifiers (ICF UID)

		String uid = null;
		boolean hasOthers = false;
		List<Element> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Element element : xmlAttributes) {
			if (element.getNamespaceURI().equals(SchemaConstants.ICFS_UID.getNamespaceURI())
					&& element.getLocalName().equals(SchemaConstants.ICFS_UID.getLocalPart())) {
				if (uid != null) {
					fail("Multiple values for ICF UID in shadow attributes");
				} else {
					uid = element.getTextContent();
				}
			} else {
				hasOthers = true;
			}
		}

		assertFalse(hasOthers);
		assertNotNull(uid);

		// Check if LDAP account was updated
		
		InternalSearchOperation op = controller.getInternalConnection().processSearch(
				"dc=example,dc=com",
				SearchScope.WHOLE_SUBTREE, 
				DereferencePolicy.NEVER_DEREF_ALIASES, 
				100, 
				100, 
				false, 
				"(entryUUID=" + uid + ")",
				null);

		assertEquals(1, op.getEntriesSent());
		SearchResultEntry response = op.getSearchEntries().get(0);

		display(response);
		
		assertAttribute(response, "uid", "jack");
		assertAttribute(response, "givenName", "Jack");
		assertAttribute(response, "sn", "Sparrow");
		// These two should be assigned from the User modification by schemaHandling
		assertAttribute(response, "cn", "Cpt. Jack Sparrow");
				
		assertAttribute(response, "l", "There there over the corner"); //IS THIS NOT RIGHT?
//		assertAttribute(response, "l", "somewhere");
	}
	
	/**
	 * The user should have an account now. Let's try to delete the user.
	 * The account should be gone as well.
	 */
	@Ignore
	@Test
	public void test005DeleteUser() throws SchemaException, FaultMessage, DirectoryException {
		// GIVEN

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.deleteObject(USER_JACK_OID,holder);

		// THEN
		
		// User should be gone from the repository
		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		try {
			repositoryService.getObject(USER_JACK_OID, resolve, repoResult);
			fail("User still exists in repo after delete");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		// Account shadow should be gone from the repository
		repoResult = new OperationResult("getObject");
		try {
			repositoryService.getObject(shadowOid, resolve, repoResult);
			fail("Shadow still exists in repo after delete");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
		
		// Account should be deleted from LDAP
		InternalSearchOperation op = controller.getInternalConnection().processSearch(
				"dc=example,dc=com",
				SearchScope.WHOLE_SUBTREE, 
				DereferencePolicy.NEVER_DEREF_ALIASES, 
				100, 
				100, 
				false, 
				"(uid=" + USER_JACK_LDAP_UID + ")",
				null);

		assertEquals(0, op.getEntriesSent());
		
	}
	
	
	// Synchronization tests
	
	/**
	 * Test initialization of synchronization.
	 * It will create a cycle task and check if the cycle executes
	 * No changes are synchronized yet.
	 */
	@Ignore
	@Test
	public void test100SynchronizationInit() throws Exception { 
		// Now it is the right time to add task definition to the repository
		// We don't want it there any sooner, as it may interfere with the
		// previous tests
		
		addObjectFromFile(TASK_OPENDJ_SYNC_FILENAME);
		
		// We need to wait for a sync interval, so the task scanner has a chance to pick up this
		// task
		
		System.out.println("Waining for task manager to pick up the task");
		Thread.sleep(10000);
		System.out.println("... done");
		
		// Check task status
		
		OperationResult result = new OperationResult(TestSanity.class.getName()+".test100Synchronization");
		Task task = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
		
		assertNotNull(task);
		System.out.println(task.dump());
		
		ObjectType o = repositoryService.getObject(TASK_OPENDJ_SYNC_OID,null, result);
		System.out.println(ObjectTypeUtil.dump(o));
		
		// .. it should be running
		assertEquals(TaskExecutionStatus.RUNNING,task.getExecutionStatus());
		
		// .. and claimed
		assertEquals(TaskExclusivityStatus.CLAIMED,task.getExclusivityStatus());
		
		// .. and last run should not be zero
		assertNotNull(task.getLastRunStartTimestamp());
		assertFalse(task.getLastRunStartTimestamp().longValue()==0);
		assertNotNull(task.getLastRunFinishTimestamp());
		assertFalse(task.getLastRunFinishTimestamp().longValue()==0);

		// Test for extension. This will also roughly test extension processor and schema processor
		PropertyContainer taskExtension = task.getExtension();
		assertNotNull(taskExtension);
		System.out.println(taskExtension.dump());
		Property shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever","shipState"));
		assertEquals("capsized",shipStateProp.getValue(String.class));
		Property deadProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever","dead"));
		assertEquals(Integer.class,deadProp.getValues().iterator().next().getClass());
		assertEquals(Integer.valueOf(42),deadProp.getValue(Integer.class));

		// The progress should be 0, as there were no changes yet
		assertEquals(0,task.getProgress());
		
		// Test for presence of a result. It should be there and it should indicate success
		OperationResult taskResult = task.getResult();
		assertNotNull(taskResult);
		
		// Failure is expected here ... for now
//		assertTrue(taskResult.isSuccess());
		
	}
	
	// TODO: insert changes in OpenDJ, let the cycle pick them up
	
	@Test
	public void test200ImportFromResource() throws Exception {
		// GIVEN

		OperationResult result = new OperationResult(TestSanity.class.getName()+".test200ImportFromResource");

		Task inTask = taskManager.createTaskInstance();
		TaskType taskType = inTask.getTaskTypeObject();
		OperationResultType resultType = new OperationResultType();
		resultType.setOperation(TestSanity.class.getName()+".test200ImportFromResource");
		taskType.setResult(resultType);
		Holder<TaskType> taskHolder = new Holder<TaskType>(taskType);
		
		// WHEN
		model.importFromResource(RESOURCE_OPENDJ_OID, IMPORT_OBJECTCLASS, taskHolder);
		
		// THEN
		
		// Convert the returned TaskType to a more useable Task
		Task task = taskManager.createTaskInstance(taskHolder.value);
		assertNotNull(task);
		assertNotNull(task.getOid());
		assertTrue(task.isAsynchronous());
		assertEquals(TaskExecutionStatus.RUNNING,task.getExecutionStatus());
		assertEquals(TaskExclusivityStatus.CLAIMED,task.getExclusivityStatus());
				
		System.out.println("Import task after launch:");
		System.out.println(task.dump());
		
		System.out.println("Import task in repo after launch:");
		ObjectType o = repositoryService.getObject(task.getOid(),null, result);
		System.out.println(ObjectTypeUtil.dump(o));
		
		System.out.println("Waining for import to complete");
		Thread.sleep(10000);
		System.out.println("... done");
		
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
		ObjectType obj = model.getObject(task.getOid(), new PropertyReferenceListType(), resultHolder);
		task = taskManager.createTaskInstance((TaskType)obj);

		System.out.println("Import task after finish:");
		System.out.println(task.dump());
		
		assertEquals(TaskExecutionStatus.CLOSED,task.getExecutionStatus());
		assertEquals(TaskExclusivityStatus.RELEASED,task.getExclusivityStatus());
		
		OperationResult taskResult = task.getResult();
		assertNotNull("Task has no result",taskResult);
		assertTrue("Task failed",taskResult.isSuccess());
		
		assertTrue(task.getProgress()>0);
		
	}
	
	// TODO: test for missing/corrupt system configuration
	// TODO: test for missing sample config (bad reference in expression arguments)
	
	// UTILITY METHODS
	
	// TODO: maybe we should move them to a common utility class
	
	private void assertAttribute(AccountShadowType repoShadow, ResourceType resource, String name, String value) {
		assertAttribute(repoShadow,new QName(resource.getNamespace(),name),value);
	}
		
	private void assertAttribute(AccountShadowType repoShadow, QName name, String value) {
		boolean found = false;
		List<Element> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Element element : xmlAttributes) {
			if (element.getNamespaceURI().equals(name.getNamespaceURI())
					&& element.getLocalName().equals(name.getLocalPart())) {
				if (found) {
					fail("Multiple values for "+name+" attribute in shadow attributes");
				} else {
					assertEquals(value,element.getTextContent());
					found = true;
				}
			}
		}
	}

	protected void assertAttribute(SearchResultEntry response, String name, String value) {
		Assert.assertNotNull(response.getAttribute(name.toLowerCase()));
		Assert.assertEquals(1, response.getAttribute(name.toLowerCase()).size());
		Attribute attribute = response.getAttribute(name.toLowerCase()).get(0);
		Assert.assertEquals(value, attribute.iterator().next().getValue().toString());
	}
	
	private <T> T unmarshallJaxbFromFile(String filePath, Class<T> clazz) throws FileNotFoundException, JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		T objectType = ((JAXBElement<T>) object).getValue();
		return objectType;
	}

	private ObjectType addObjectFromFile(String filePath) throws Exception {
		ObjectType object = unmarshallJaxbFromFile(filePath, ObjectType.class);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(TestSanity.class.getName() + ".addObjectFromFile");
		repositoryService.addObject(object, result);
		return object;
	}

	private void displayJaxb(Object o, QName qname) throws JAXBException {
		Document doc = DOMUtil.getDocument();
		Element element = JAXBUtil.jaxbToDom(o, qname, doc);
		System.out.println(DOMUtil.serializeDOMToString(element));
	}

	private void display(SearchResultEntry response) {
		// TODO Auto-generated method stub
		System.out.println(response.toLDIFString());
	}


}
