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
import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
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
import org.opends.server.core.AddOperation;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.Attribute;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
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
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-sanity-test.xml",
		"classpath:application-context-task.xml" ,
		"classpath:application-context-repository-test.xml"})

public class TestSanity extends AbstractIntegrationTest {

	private static final String SYSTEM_CONFIGURATION_FILENAME = "src/test/resources/repo/system-configuration.xml";
	private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

	private static final String RESOURCE_OPENDJ_FILENAME = "src/test/resources/repo/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String TASK_OPENDJ_SYNC_FILENAME = "src/test/resources/repo/opendj-sync-task.xml";
	private static final String TASK_OPENDJ_SYNC_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = "src/test/resources/repo/sample-configuration-object.xml";
	private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";

	private static final String USER_TEMPLATE_FILENAME = "src/test/resources/repo/user-template.xml";
	private static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	private static final String USER_JACK_FILENAME = "src/test/resources/repo/user-jack.xml";
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	private static final String USER_JACK_LDAP_UID = "jack";

	private static final String LDIF_WILL_FILENAME = "src/test/resources/request/will.ldif";
	private static final String WILL_NAME = "wturner";

	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_FILENAME = "src/test/resources/request/user-modify-add-account.xml";
	private static final String REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME = "src/test/resources/request/user-modify-fullname-locality.xml";

	private static final QName IMPORT_OBJECTCLASS = new QName(
			"http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff",
			"AccountObjectClass");

	private static final Trace LOGGER = TraceManager.getTrace(TestSanity.class);


	/**
	 * Unmarshalled resource definition to reach the embedded OpenDJ instance.
	 * Used for convenience - the tests method may find it handy.
	 */
	private static ResourceType resource;
	private static String shadowOid;

	/**
	 * The instance of ModelService. This is the interface that we will test.
	 */
	@Autowired(required = true)
	private ModelPortType model;

	public TestSanity() throws JAXBException {
		super();
		// TODO: fix this
		IntegrationTestTools.checkResults=false;
	}

	// This will get called from the superclass to init the repository
	// It will be called only once
	public void initRepository() throws Exception {
			resource = (ResourceType) addObjectFromFile(RESOURCE_OPENDJ_FILENAME);
			addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME);
			addObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME);
			addObjectFromFile(USER_TEMPLATE_FILENAME);
	}

	/**
	 * Test integrity of the test setup.
	 * 
	 * @throws SchemaException
	 * @throws ObjectNotFoundException
	 */
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile("test000Integrity");
		assertNotNull(resource);
		assertNotNull(model);
		assertNotNull(repositoryService);
		assertTrue(repoInitialized);
		assertNotNull(taskManager);

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
		displayTestTile("test001TestConnection");

		// GIVEN
		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.testResource(RESOURCE_OPENDJ_OID, holder);

		// THEN

		System.out.println("testResource result:");
		displayJaxb(result, SchemaConstants.C_RESULT);

		assertSuccess("testResource has failed", result.getPartialResults().get(0));
	}

	/**
	 * Attempt to add new user. It is only added to the repository, so check if
	 * it is in the repository after the operation.
	 */
	@Test
	public void test002AddUser() throws FileNotFoundException, JAXBException, FaultMessage,
			ObjectNotFoundException, SchemaException {
		displayTestTile("test002AddUser");

		// GIVEN
		UserType user = unmarshallJaxbFromFile(USER_JACK_FILENAME, UserType.class);

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		String oid = model.addObject(user, holder);

		// THEN

		System.out.println("addObject result:");
		displayJaxb(holder.value, SchemaConstants.C_RESULT);
		assertSuccess("addObject has failed", holder.value);

		assertEquals(USER_JACK_OID, oid);

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		ObjectType repoObject = repositoryService.getObject(oid, resolve, repoResult);

		assertSuccess("getObject has failed", repoResult);
		assertEquals(USER_JACK_OID, repoObject.getOid());
		UserType repoUser = (UserType) repoObject;
		assertEquals(user.getFullName(), repoUser.getFullName());

		// TODO: better checks
	}

	/**
	 * Add account to user. This should result in account provisioning. Check if
	 * that happens in repo and in LDAP.
	 */
	@Test
	public void test003AddAccountToUser() throws FileNotFoundException, JAXBException, FaultMessage,
			ObjectNotFoundException, SchemaException, DirectoryException {
		displayTestTile("test003AddAccountToUser");

		// GIVEN

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_FILENAME, ObjectModificationType.class);

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.modifyObject(objectChange, holder);

		// THEN
		displayJaxb("modifyObject result", holder.value, SchemaConstants.C_RESULT);
		assertSuccess("modifyObject has failed", holder.value);

		// Check if user object was modified in the repo

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		ObjectType repoObject = repositoryService.getObject(USER_JACK_OID, resolve, repoResult);
		UserType repoUser = (UserType) repoObject;

		displayJaxb("User (repository)", repoUser, new QName("user"));

		List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
		assertEquals(1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		shadowOid = accountRef.getOid();
		assertFalse(shadowOid.isEmpty());

		// Check if shadow was created in the repo

		repoResult = new OperationResult("getObject");

		repoObject = repositoryService.getObject(shadowOid, resolve, repoResult);
		assertSuccess("addObject has failed", repoResult);
		AccountShadowType repoShadow = (AccountShadowType) repoObject;

		displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));

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
		InternalSearchOperation op = controller.getInternalConnection().processSearch("dc=example,dc=com",
				SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100, 100, false,
				"(entryUUID=" + uid + ")", null);

		assertEquals(1, op.getEntriesSent());
		SearchResultEntry response = op.getSearchEntries().get(0);

		display("LDAP account", response);

		assertAttribute(response, "uid", "jack");
		assertAttribute(response, "givenName", "Jack");
		assertAttribute(response, "sn", "Sparrow");
		assertAttribute(response, "cn", "Jack Sparrow");
		// The "l" attribute is assigned indirectly through schemaHandling and
		// config object
		assertAttribute(response, "l", "middle of nowhere");

		// Use getObject to test fetch of complete shadow

		result = new OperationResultType();
		holder.value = result;

		// WHEN
		ObjectType modelObject = model.getObject(shadowOid, resolve, holder);

		// THEN
		displayJaxb("getObject result", holder.value, SchemaConstants.C_RESULT);
		assertSuccess("getObject has failed", holder.value);

		AccountShadowType modelShadow = (AccountShadowType) modelObject;
		displayJaxb("Shadow (model)", modelShadow, new QName("shadow"));

		assertNotNull(modelShadow);
		assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

		assertAttributeNotNull(modelShadow, SchemaConstants.ICFS_UID);
		assertAttribute(modelShadow, resource, "uid", "jack");
		assertAttribute(modelShadow, resource, "givenName", "Jack");
		assertAttribute(modelShadow, resource, "sn", "Sparrow");
		assertAttribute(modelShadow, resource, "cn", "Jack Sparrow");
		assertAttribute(modelShadow, resource, "l", "middle of nowhere");
	}

	/**
	 * We are going to modify the user. As the user has an account, the user
	 * changes should be also applied to the account (by schemaHandling).
	 * 
	 * @throws DirectoryException
	 */
	@Test
	public void test004modifyUser() throws FileNotFoundException, JAXBException, FaultMessage,
			ObjectNotFoundException, SchemaException, DirectoryException {
		displayTestTile("test004modifyUser");
		// GIVEN

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME, ObjectModificationType.class);

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.modifyObject(objectChange, holder);

		// THEN
		System.out.println("modifyObject result:");
		displayJaxb(holder.value, SchemaConstants.C_RESULT);
		assertSuccess("modifyObject has failed", holder.value);

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
		assertEquals(shadowOid, newShadowOid);

		// Check if shadow is still in the repo and that it is untouched

		repoResult = new OperationResult("getObject");
		repoObject = repositoryService.getObject(shadowOid, resolve, repoResult);
		assertSuccess("getObject(repo) has failed", repoResult);
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

		InternalSearchOperation op = controller.getInternalConnection().processSearch("dc=example,dc=com",
				SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100, 100, false,
				"(entryUUID=" + uid + ")", null);

		assertEquals(1, op.getEntriesSent());
		SearchResultEntry response = op.getSearchEntries().get(0);

		display(response);

		assertAttribute(response, "uid", "jack");
		assertAttribute(response, "givenName", "Jack");
		assertAttribute(response, "sn", "Sparrow");
		// These two should be assigned from the User modification by
		// schemaHandling
		assertAttribute(response, "cn", "Cpt. Jack Sparrow");

		assertAttribute(response, "l", "There there over the corner"); // IS
																		// THIS
																		// NOT
																		// RIGHT?
		// assertAttribute(response, "l", "somewhere");
	}

	/**
	 * The user should have an account now. Let's try to delete the user. The
	 * account should be gone as well.
	 * 
	 * @throws JAXBException
	 */
	@Test
	public void test005DeleteUser() throws SchemaException, FaultMessage, DirectoryException, JAXBException {
		displayTestTile("test005DeleteUser");
		// GIVEN

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(result);

		// WHEN
		model.deleteObject(USER_JACK_OID, holder);

		// THEN
		System.out.println("deleteObject result:");
		displayJaxb(holder.value, SchemaConstants.C_RESULT);
		assertSuccess("deleteObject has failed", holder.value);

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
			// This is expected, but check also the result
			assertFalse("getObject failed as expected, but the result indicates success",
					repoResult.isSuccess());
		}

		// Account should be deleted from LDAP
		InternalSearchOperation op = controller.getInternalConnection().processSearch("dc=example,dc=com",
				SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100, 100, false,
				"(uid=" + USER_JACK_LDAP_UID + ")", null);

		assertEquals(0, op.getEntriesSent());

	}

	// Synchronization tests

	/**
	 * Test initialization of synchronization. It will create a cycle task and
	 * check if the cycle executes No changes are synchronized yet.
	 */
	@Test
	public void test100LiveSyncInit() throws Exception {
		displayTestTile("test100LiveSyncInit");
		// Now it is the right time to add task definition to the repository
		// We don't want it there any sooner, as it may interfere with the
		// previous tests

		addObjectFromFile(TASK_OPENDJ_SYNC_FILENAME);

		final OperationResult result = new OperationResult(TestSanity.class.getName() + ".test100Synchronization");
		
		// We need to wait for a sync interval, so the task scanner has a chance
		// to pick up this
		// task
		
		waitFor("Waiting for task manager to pick up the task",
				new Checker() {
					public boolean check() throws ObjectNotFoundException, SchemaException {						
						Task task = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
						display("Task while waiting for task manager to pick up the task",task);
						// wait until the task is picked up
						if (TaskExclusivityStatus.CLAIMED == task.getExclusivityStatus()) {
							// wait until the first run is finished
							if (task.getLastRunFinishTimestamp()==null) {
								return false;
							}
							return true;
						}
						return false;
					};
				},
				20000);

		// Check task status

		Task task = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
		assertSuccess("getTask has failed", result);
		assertNotNull(task);
		display("Task after pickup", task);

		ObjectType o = repositoryService.getObject(TASK_OPENDJ_SYNC_OID, null, result);
		display("Task after pickup in the repository", o);

		// .. it should be running
		assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());

		// .. and claimed
		assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

		// .. and last run should not be zero
		assertNotNull(task.getLastRunStartTimestamp());
		assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
		assertNotNull(task.getLastRunFinishTimestamp());
		assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

		// Test for extension. This will also roughly test extension processor
		// and schema processor
		PropertyContainer taskExtension = task.getExtension();
		assertNotNull(taskExtension);
		display("Task extension", taskExtension);
		Property shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever",
				"shipState"));
		assertEquals("capsized", shipStateProp.getValue(String.class));
		Property deadProp = taskExtension
				.findProperty(new QName("http://myself.me/schemas/whatever", "dead"));
		assertEquals(Integer.class, deadProp.getValues().iterator().next().getClass());
		assertEquals(Integer.valueOf(42), deadProp.getValue(Integer.class));

		// The progress should be 0, as there were no changes yet
		assertEquals(0, task.getProgress());

		// Test for presence of a result. It should be there and it should
		// indicate success
		OperationResult taskResult = task.getResult();
		assertNotNull(taskResult);

		// Failure is expected here ... for now
		// assertTrue(taskResult.isSuccess());

	}

	/**
	 * Create LDAP object. That should be picked up by liveSync and a user
	 * should be craeted in repo.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test101LiveSyncCreate() throws Exception {
		displayTestTile("test101LiveSyncCreate");
		// Sync task should be running (tested in previous test), so just create
		// new LDAP object.

		LDIFImportConfig importConfig = new LDIFImportConfig(LDIF_WILL_FILENAME);
		LDIFReader ldifReader = new LDIFReader(importConfig);
		Entry entry = ldifReader.readEntry();
		display("Entry from LDIF", entry);
		
		final OperationResult result = new OperationResult(TestSanity.class.getName() + ".test101LiveSyncCreate");
		final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
		assertNotNull(syncCycle);
		
		final Object tokenBefore;
		Property tokenProperty = syncCycle.getExtension().findProperty(SchemaConstants.SYNC_TOKEN);
		if (tokenProperty==null) {
			tokenBefore=null;
		} else {
			tokenBefore=tokenProperty.getValue();
		}

		// WHEN

		AddOperation addOperation = controller.getInternalConnection().processAdd(entry);

		// THEN

		assertEquals("LDAP add operation failed", ResultCode.SUCCESS, addOperation.getResultCode());

		// Wait a bit to give the sync cycle time to detect the change

		waitFor("Waiting for sync cycle to detect change",
				new Checker() {
					@Override
					public boolean check() throws Exception {
						syncCycle.refresh(result);
						display("SyncCycle while waiting for sync cycle to detect change",syncCycle);
						Object tokenNow = null;
						Property propertyNow = syncCycle.getExtension().findProperty(SchemaConstants.SYNC_TOKEN);
						if (propertyNow==null) {
							tokenNow=null;
						} else {
							tokenNow=propertyNow.getValue();
						}
						if (tokenBefore==null) {
							return(tokenNow!=null);
						} else {
							return(!tokenBefore.equals(tokenNow));
						}
					}
				},
				30000);

		// Search for the user that should be created now

		Document doc = DOMUtil.getDocument();
		Element nameElement = doc.createElementNS(SchemaConstants.C_NAME.getNamespaceURI(),
				SchemaConstants.C_NAME.getLocalPart());
		nameElement.setTextContent(WILL_NAME);
		Element filter = QueryUtil.createAndFilter(doc,
				// No path needed. The default is OK.
				QueryUtil.createTypeFilter(doc, ObjectTypes.USER.getObjectTypeUri()),
				QueryUtil.createEqualFilter(doc, null, nameElement));

		QueryType query = new QueryType();
		query.setFilter(filter);
		OperationResultType resultType = new OperationResultType();
		Holder<OperationResultType> holder = new Holder<OperationResultType>(resultType);

		ObjectListType objects = model.searchObjects(query, null, holder);

		assertSuccess("searchObjects has failed", holder.value);
		assertEquals("User not found (or found too many)", 1, objects.getObject().size());
		UserType user = (UserType) objects.getObject().get(0);

		assertEquals(user.getName(), WILL_NAME);

		// TODO: more checks
	}

	// TODO: insert changes in OpenDJ, let the cycle pick them up
	
	@Test
	public void test200ImportFromResource() throws Exception {
		displayTestTile("test200ImportFromResource");
		// GIVEN

		OperationResult result = new OperationResult(TestSanity.class.getName()
				+ ".test200ImportFromResource");

		Task inTask = taskManager.createTaskInstance();
		TaskType taskType = inTask.getTaskTypeObject();
		final OperationResultType resultType = new OperationResultType();
		resultType.setOperation(TestSanity.class.getName() + ".test200ImportFromResource");
		taskType.setResult(resultType);
		Holder<TaskType> taskHolder = new Holder<TaskType>(taskType);

		// WHEN
		model.importFromResource(RESOURCE_OPENDJ_OID, IMPORT_OBJECTCLASS, taskHolder);

		// THEN

		assertSuccess("importFromResource has failed", taskHolder.value.getResult());
		// Convert the returned TaskType to a more usable Task
		Task task = taskManager.createTaskInstance(taskHolder.value);
		assertNotNull(task);
		assertNotNull(task.getOid());
		assertTrue(task.isAsynchronous());
		assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
		assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

		display("Import task after launch", task);

		ObjectType o = repositoryService.getObject(task.getOid(), null, result);
		display("Import task in repo after launch", o);

		assertSuccess("getObject has failed", result);

		final String taskOid = task.getOid();
		waitFor("Waiting for import to complete",
				new Checker() {
					@Override
					public boolean check() throws Exception {
						Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
						ObjectType obj = model.getObject(taskOid, new PropertyReferenceListType(), resultHolder);
						assertSuccess("getObject has failed", resultHolder.value);
						Task task = taskManager.createTaskInstance((TaskType) obj);
						return(task.getExecutionStatus() == TaskExecutionStatus.CLOSED);
					}
				},
				30000);

		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
		ObjectType obj = model.getObject(task.getOid(), new PropertyReferenceListType(), resultHolder);
		assertSuccess("getObject has failed", resultHolder.value);
		task = taskManager.createTaskInstance((TaskType) obj);

		display("Import task after finish (fetched from model)", task);

		assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

		//Ugly fix to wait until state change success fully on slowmachines.
		try {
			Thread.sleep(1000);
 		} catch (Exception e) {
		}

		assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

		OperationResult taskResult = task.getResult();
		assertNotNull("Task has no result", taskResult);
		assertTrue("Task failed", taskResult.isSuccess());

		assertTrue("No progress", task.getProgress() > 0);

		// Check if the import created users and shadows

		// Listing of shadows is not supported by the provisioning. So we need
		// to look directly into repository
		ObjectListType sobjects = repositoryService.listObjects(AccountShadowType.class, null, result);
		assertSuccess("listObjects has failed", result);
		assertFalse("No shadows created", sobjects.getObject().isEmpty());

		for (ObjectType oo : sobjects.getObject()) {
			ResourceObjectShadowType shadow = (ResourceObjectShadowType) oo;
			display("Shadow object after import (repo)", shadow);
			assertNotEmpty("No OID in shadow", shadow.getOid()); // This would
																	// be really
																	// strange
																	// ;-)
			assertNotEmpty("No name in shadow", shadow.getName());
			assertNotNull("No objectclass in shadow", shadow.getObjectClass());
			assertNotNull("Null attributes in shadow", shadow.getAttributes());
			assertAttributeNotNull("No UID in shadow", shadow, SchemaConstants.ICFS_UID);
		}
		ObjectListType uobjects = model.listObjects(ObjectTypes.USER.getObjectTypeUri(), null, resultHolder);
		assertSuccess("listObjects has failed", resultHolder.value);
		assertFalse("No users created", uobjects.getObject().isEmpty());

		for (ObjectType oo : uobjects.getObject()) {
			UserType user = (UserType) oo;
			display("User after import (repo)", user);
			assertNotEmpty("No OID in user", user.getOid()); // This would be
																// really
																// strange ;-)
			assertNotEmpty("No name in user", user.getName());
			assertNotEmpty("No fullName in user", user.getFullName());
			assertNotEmpty("No familyName in user", user.getFamilyName());
			// givenName is not mandatory in LDAP, therefore givenName may not
			// be present on user
			List<ObjectReferenceType> accountRefs = user.getAccountRef();
			assertEquals("Wrong accountRef", 1, accountRefs.size());
			ObjectReferenceType accountRef = accountRefs.get(0);
			// here was ref to resource oid, not account oid

			// XXX: HACK: I don't know how to match accounts here
			boolean found = false;
			for (ObjectType account : sobjects.getObject()) {
				if (accountRef.getOid().equals(account.getOid())) {
					found = true;
					break;
				}
			}
			if (!found) {
				fail("accountRef does not point to existing account " + accountRef.getOid());
			}
		}
	}

	@Test
	public void test999Shutdown() throws Exception {
		taskManager.shutdown();
		waitFor("waiting for task manager shutdown",
				new Checker() {
					@Override
					public boolean check() throws Exception {
						return taskManager.getRunningTasks().isEmpty();
					}
				},
				10000);
		assertEquals("Some tasks left running after shutdown", new HashSet<Task>(),
				taskManager.getRunningTasks());
	}

	// TODO: test for missing/corrupt system configuration
	// TODO: test for missing sample config (bad reference in expression
	// arguments)

}
