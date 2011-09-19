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

import static org.testng.AssertJUnit.assertFalse;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.testng.AssertJUnit;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttributeNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNotEmpty;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayJaxb;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.opends.server.core.AddOperation;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;
import org.opends.server.util.LDIFReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.MiscUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;

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
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-provisioning.xml", "classpath:application-context-sanity-test.xml",
		"classpath:application-context-task.xml", "classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class TestSanity extends AbstractIntegrationTest {

	private static final String SYSTEM_CONFIGURATION_FILENAME = "src/test/resources/repo/system-configuration.xml";
	private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

	private static final String RESOURCE_OPENDJ_FILENAME = "src/test/resources/repo/resource-opendj.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String RESOURCE_DERBY_FILENAME = "src/test/resources/repo/resource-derby.xml";
	private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";
	
	private static final String TASK_OPENDJ_SYNC_FILENAME = "src/test/resources/repo/opendj-sync-task.xml";
	private static final String TASK_OPENDJ_SYNC_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = "src/test/resources/repo/sample-configuration-object.xml";
	private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";

	private static final String USER_TEMPLATE_FILENAME = "src/test/resources/repo/user-template.xml";
	private static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	private static final String USER_JACK_FILENAME = "src/test/resources/repo/user-jack.xml";
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	private static final String USER_JACK_LDAP_UID = "jack";
	private static final String USER_JACK_LDAP_DN = "uid=" + USER_JACK_LDAP_UID
			+ ",ou=people,dc=example,dc=com";

	private static final String LDIF_WILL_FILENAME = "src/test/resources/request/will.ldif";
	private static final String WILL_NAME = "wturner";

	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_FILENAME = "src/test/resources/request/user-modify-add-account.xml";
	private static final String REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME = "src/test/resources/request/user-modify-fullname-locality.xml";
	private static final String REQUEST_USER_MODIFY_PASSWORD_FILENAME = "src/test/resources/request/user-modify-password.xml";

	private static final QName IMPORT_OBJECTCLASS = new QName(
			"http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff",
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
	private ModelPortType modelWeb;
	@Autowired(required = true)
	private ModelService modelService;

	public TestSanity() throws JAXBException {
		super();
		// TODO: fix this
		//IntegrationTestTools.checkResults = false;
	}

	// This will get called from the superclass to init the repository
	// It will be called only once
	public void initSystem(OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, initResult);

		// This should discover the connectors
		LOGGER.trace("initSystem: trying modelService.postInit()");
		modelService.postInit(initResult);
		LOGGER.trace("initSystem: modelService.postInit() done");

		// Need to import instead of add, so the (dynamic) connector reference
		// will be resolved
		// correctly
		importObjectFromFile(RESOURCE_OPENDJ_FILENAME, initResult);
		importObjectFromFile(RESOURCE_DERBY_FILENAME, initResult);

		addObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME, initResult);
		addObjectFromFile(USER_TEMPLATE_FILENAME, initResult);
	}

	/**
	 * Initialize embedded OpenDJ instance Note: this is not in the abstract
	 * superclass so individual tests may avoid starting OpenDJ.
	 */
	@BeforeClass
	public static void startResources() throws Exception {
		openDJController.startCleanServer();
		derbyController.startCleanServer();
	}

	/**
	 * Shutdown embedded OpenDJ instance Note: this is not in the abstract
	 * superclass so individual tests may avoid starting OpenDJ.
	 */
	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
		derbyController.stop();
	}

	/**
	 * Test integrity of the test setup.
	 * 
	 * @throws SchemaException
	 * @throws ObjectNotFoundException
	 */
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException {
		displayTestTile(this, "test000Integrity");
		AssertJUnit.assertNotNull(modelWeb);
		AssertJUnit.assertNotNull(modelService);
		AssertJUnit.assertNotNull(repositoryService);
		AssertJUnit.assertTrue(isSystemInitialized());
		AssertJUnit.assertNotNull(taskManager);

		OperationResult result = new OperationResult(TestSanity.class.getName() + ".test000Integrity");
		
		// Check if OpenDJ resource was imported correctly
		
		ResourceType openDjResource = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null,
				result);
		display("Imported resource",openDjResource);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());
		
		String ldapConnectorOid = openDjResource.getConnectorRef().getOid();
		ConnectorType ldapConnector = repositoryService.getObject(ConnectorType.class, ldapConnectorOid, null, result);
		display("LDAP Connector: ", ldapConnector);
		
		// Check if Derby resource was imported correctly
		
		ResourceType derbyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null,
				result);
		AssertJUnit.assertEquals(RESOURCE_DERBY_OID, derbyResource.getOid());
		
		String dbConnectorOid = derbyResource.getConnectorRef().getOid();
		ConnectorType dbConnector = repositoryService.getObject(ConnectorType.class, dbConnectorOid, null, result);
		display("DB Connector: ", dbConnector);
		
		// Check if password was encrypted during import
		Object configurationPropertiesElement = JAXBUtil.findElement(derbyResource.getConfiguration().getAny(),new QName(dbConnector.getNamespace(),"configurationProperties"));
		Object passwordElement = JAXBUtil.findElement(JAXBUtil.listChildElements(configurationPropertiesElement),new QName(dbConnector.getNamespace(),"password"));
		System.out.println("Password element: "+passwordElement);

		// TODO: test if OpenDJ and Derby are running
	}

	/**
	 * Test the testResource method. Expect a complete success for now.
	 * 
	 * TODO: better check for the returned result. Look inside and check if all
	 * the expected tests were run.
	 * 
	 * @throws FaultMessage
	 * @throws JAXBException
	 * @throws SchemaException
	 * @throws ObjectNotFoundException
	 */
	@Test
	public void test001TestConnection() throws FaultMessage, JAXBException, ObjectNotFoundException,
			SchemaException {
		displayTestTile("test001TestConnection");

		// GIVEN

		// WHEN
		OperationResultType result = modelWeb.testResource(RESOURCE_OPENDJ_OID);

		// THEN

		System.out.println("testResource result:");
		displayJaxb(result, SchemaConstants.C_RESULT);

		assertSuccess("testResource has failed", result.getPartialResults().get(0));

		OperationResult opResult = new OperationResult(TestSanity.class.getName() + ".test001TestConnection");
		resource = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, resource.getOid());
		display("Initialized resource", resource);
		AssertJUnit.assertNotNull("Resource schema was not generated", resource.getSchema());
		AssertJUnit.assertFalse("Resource schema was not generated", resource.getSchema().getAny().isEmpty());
	}

	/**
	 * Attempt to add new user. It is only added to the repository, so check if
	 * it is in the repository after the operation.
	 */
	@Test
	public void test002AddUser() throws FileNotFoundException, JAXBException, FaultMessage,
			ObjectNotFoundException, SchemaException, EncryptionException {
		displayTestTile("test002AddUser");

		// GIVEN
		UserType user = unmarshallJaxbFromFile(USER_JACK_FILENAME, UserType.class);
		
		// Encrypt Jack's password
		protector.encrypt(user.getCredentials().getPassword().getProtectedString());

		OperationResultType result = new OperationResultType();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
		Holder<String> oidHolder = new Holder<String>();
		
		display("Adding user object",user);

		// WHEN
		modelWeb.addObject(user, oidHolder, resultHolder);

		// THEN

		System.out.println("addObject result:");
		displayJaxb(resultHolder.value, SchemaConstants.C_RESULT);
		assertSuccess("addObject has failed", resultHolder.value);

		AssertJUnit.assertEquals(USER_JACK_OID, oidHolder.value);

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		UserType repoUser = repositoryService.getObject(UserType.class, oidHolder.value, resolve, repoResult);

		repoResult.computeStatus();
		display("repository.getObject result",repoResult);
		assertSuccess("getObject has failed", repoResult);
		AssertJUnit.assertEquals(USER_JACK_OID, repoUser.getOid());
		AssertJUnit.assertEquals(user.getFullName(), repoUser.getFullName());

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

		// WHEN
		OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

		// THEN
		displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
		assertSuccess("modifyObject has failed", result);

		// Check if user object was modified in the repo

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		UserType repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, resolve, repoResult);

		repoResult.computeStatus();
		displayJaxb("User (repository)", repoUser, new QName("user"));

		List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
		assertEquals(1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		shadowOid = accountRef.getOid();
		assertFalse(shadowOid.isEmpty());

		// Check if shadow was created in the repo

		repoResult = new OperationResult("getObject");

		AccountShadowType repoShadow = repositoryService.getObject(AccountShadowType.class, shadowOid,
				resolve, repoResult);
		repoResult.computeStatus();
		assertSuccess("addObject has failed", repoResult);
		displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
		assertNotNull(repoShadow);
		assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

		// Check the "name" property, it should be set to DN, not entryUUID
		assertEquals("Wrong name property", repoShadow.getName().toLowerCase(), USER_JACK_LDAP_DN.toLowerCase());

		// check attributes in the shadow: should be only identifiers (ICF UID)
		String uid = null;
		boolean hasOthers = false;
		List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Object element : xmlAttributes) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
				if (uid != null) {
					AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
				} else {
					uid = ((Element)element).getTextContent();
				}
			} else {
				hasOthers = true;
			}
		}

		assertFalse(hasOthers);
		assertNotNull(uid);

		// check if account was created in LDAP

		SearchResultEntry response = openDJController.searchByEntryUuid(uid);
		
		display("LDAP account", response);

		OpenDJController.assertAttribute(response, "uid", "jack");
		OpenDJController.assertAttribute(response, "givenName", "Jack");
		OpenDJController.assertAttribute(response, "sn", "Sparrow");
		OpenDJController.assertAttribute(response, "cn", "Jack Sparrow");
		// The "l" attribute is assigned indirectly through schemaHandling and
		// config object
		OpenDJController.assertAttribute(response, "l", "middle of nowhere");

		// Use getObject to test fetch of complete shadow

		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		Holder<ObjectType> objectHolder = new Holder<ObjectType>();

		// WHEN
		modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), shadowOid,
				resolve, objectHolder, resultHolder);

		// THEN
		displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
		assertSuccess("getObject has failed", resultHolder.value);

		AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
		displayJaxb("Shadow (model)", modelShadow, new QName("shadow"));

		AssertJUnit.assertNotNull(modelShadow);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

		assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
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
	public void test004ModifyUser() throws FileNotFoundException, JAXBException, FaultMessage,
			ObjectNotFoundException, SchemaException, DirectoryException {
		displayTestTile("test004ModifyUser");
		// GIVEN

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME, ObjectModificationType.class);

		// WHEN
		OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

		// THEN
		System.out.println("modifyObject result:");
		displayJaxb(result, SchemaConstants.C_RESULT);
		assertSuccess("modifyObject has failed", result);

		// Check if user object was modified in the repo

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType repoObject = repositoryService.getObject(ObjectType.class, USER_JACK_OID, resolve, repoResult);
		UserType repoUser = (UserType) repoObject;
		displayJaxb(repoUser, new QName("user"));

		AssertJUnit.assertEquals("Cpt. Jack Sparrow", repoUser.getFullName());
		AssertJUnit.assertEquals("somewhere", repoUser.getLocality());

		// Check if appropriate accountRef is still there

		List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
		AssertJUnit.assertEquals(1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		String newShadowOid = accountRef.getOid();
		AssertJUnit.assertEquals(shadowOid, newShadowOid);

		// Check if shadow is still in the repo and that it is untouched

		repoResult = new OperationResult("getObject");
		repoObject = repositoryService.getObject(ObjectType.class, shadowOid, resolve, repoResult);
		repoResult.computeStatus();
		assertSuccess("getObject(repo) has failed", repoResult);
		AccountShadowType repoShadow = (AccountShadowType) repoObject;
		displayJaxb(repoShadow, new QName("shadow"));
		AssertJUnit.assertNotNull(repoShadow);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

		// check attributes in the shadow: should be only identifiers (ICF UID)

		String uid = null;
		boolean hasOthers = false;
		List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Object element : xmlAttributes) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
				if (uid != null) {
					AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
				} else {
					uid = ((Element)element).getTextContent();
				}
			} else {
				hasOthers = true;
			}
		}

		AssertJUnit.assertFalse(hasOthers);
		assertNotNull(uid);

		// Check if LDAP account was updated
		SearchResultEntry response = openDJController.searchByEntryUuid(uid);

		display(response);

		OpenDJController.assertAttribute(response, "uid", "jack");
		OpenDJController.assertAttribute(response, "givenName", "Jack");
		OpenDJController.assertAttribute(response, "sn", "Sparrow");
		// These two should be assigned from the User modification by
		// schemaHandling
		OpenDJController.assertAttribute(response, "cn", "Cpt. Jack Sparrow");
		// This will get translated from "somewhere" to this (outbound expression in schemeHandling)
		OpenDJController.assertAttribute(response, "l", "There there over the corner");
	}

	/**
	 * We are going to change user's password. As the user has an account, the password change
	 * should be also applied to the account (by schemaHandling).
	 */
	@Test
	public void test005ChangePassword() throws FileNotFoundException, JAXBException, FaultMessage,
			ObjectNotFoundException, SchemaException, DirectoryException {
		displayTestTile("test005ChangePassword");
		// GIVEN

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_PASSWORD_FILENAME, ObjectModificationType.class);
		
		System.out.println("In modification: "+objectChange.getPropertyModification().get(0).getValue().getAny().get(0));

		// WHEN
		OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

		// THEN
		System.out.println("modifyObject result:");
		displayJaxb(result, SchemaConstants.C_RESULT);
		assertSuccess("modifyObject has failed", result);

		// Check if user object was modified in the repo

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		ObjectType repoObject = repositoryService.getObject(ObjectType.class, USER_JACK_OID, resolve, repoResult);
		UserType repoUser = (UserType) repoObject;
		displayJaxb(repoUser, new QName("user"));

		// Check if nothing else was modified
		AssertJUnit.assertEquals("Cpt. Jack Sparrow", repoUser.getFullName());
		AssertJUnit.assertEquals("somewhere", repoUser.getLocality());

		// Check if appropriate accountRef is still there
		List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
		AssertJUnit.assertEquals(1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		String newShadowOid = accountRef.getOid();
		AssertJUnit.assertEquals(shadowOid, newShadowOid);

		// Check if shadow is still in the repo and that it is untouched
		repoResult = new OperationResult("getObject");
		repoObject = repositoryService.getObject(ObjectType.class, shadowOid, resolve, repoResult);
		repoResult.computeStatus();
		assertSuccess("getObject(repo) has failed", repoResult);
		AccountShadowType repoShadow = (AccountShadowType) repoObject;
		displayJaxb(repoShadow, new QName("shadow"));
		AssertJUnit.assertNotNull(repoShadow);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

		// check attributes in the shadow: should be only identifiers (ICF UID)
		String uid = null;
		boolean hasOthers = false;
		List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
		for (Object element : xmlAttributes) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
				if (uid != null) {
					AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
				} else {
					uid = ((Element)element).getTextContent();
				}
			} else {
				hasOthers = true;
			}
		}

		AssertJUnit.assertFalse(hasOthers);
		assertNotNull(uid);

		// Check if LDAP account was updated

		SearchResultEntry response = openDJController.searchByEntryUuid(uid);
		display(response);

		OpenDJController.assertAttribute(response, "uid", "jack");
		OpenDJController.assertAttribute(response, "givenName", "Jack");
		OpenDJController.assertAttribute(response, "sn", "Sparrow");
		// These two should be assigned from the User modification by
		// schemaHandling
		OpenDJController.assertAttribute(response, "cn", "Cpt. Jack Sparrow");
		OpenDJController.assertAttribute(response, "l", "There there over the corner");
	}
	
	/**
	 * The user should have an account now. Let's try to delete the user. The
	 * account should be gone as well.
	 * 
	 * @throws JAXBException
	 */
	@Test
	public void test006DeleteUser() throws SchemaException, FaultMessage, DirectoryException, JAXBException {
		displayTestTile("test005DeleteUser");
		// GIVEN

		// WHEN
		OperationResultType result = modelWeb.deleteObject(ObjectTypes.USER.getObjectTypeUri(), USER_JACK_OID);

		// THEN
		System.out.println("deleteObject result:");
		displayJaxb(result, SchemaConstants.C_RESULT);
		assertSuccess("deleteObject has failed", result);

		// User should be gone from the repository
		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();
		try {
			repositoryService.getObject(ObjectType.class, USER_JACK_OID, resolve, repoResult);
			AssertJUnit.fail("User still exists in repo after delete");
		} catch (ObjectNotFoundException e) {
			// This is expected
		}

		// Account shadow should be gone from the repository
		repoResult = new OperationResult("getObject");
		try {
			repositoryService.getObject(ObjectType.class, shadowOid, resolve, repoResult);
			AssertJUnit.fail("Shadow still exists in repo after delete");
		} catch (ObjectNotFoundException e) {
			// This is expected, but check also the result
			AssertJUnit.assertFalse("getObject failed as expected, but the result indicates success",
					repoResult.isSuccess());
		}

		// Account should be deleted from LDAP
		InternalSearchOperation op = openDJController.getInternalConnection().processSearch(
				"dc=example,dc=com", SearchScope.WHOLE_SUBTREE, DereferencePolicy.NEVER_DEREF_ALIASES, 100,
				100, false, "(uid=" + USER_JACK_LDAP_UID + ")", null);

		AssertJUnit.assertEquals(0, op.getEntriesSent());

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

		final OperationResult result = new OperationResult(TestSanity.class.getName()
				+ ".test100Synchronization");

		addObjectFromFile(TASK_OPENDJ_SYNC_FILENAME, result);

		
		// We need to wait for a sync interval, so the task scanner has a chance
		// to pick up this
		// task

		waitFor("Waiting for task manager to pick up the task", new Checker() {
			public boolean check() throws ObjectNotFoundException, SchemaException {
				Task task = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
				display("Task while waiting for task manager to pick up the task", task);
				// wait until the task is picked up
				if (TaskExclusivityStatus.CLAIMED == task.getExclusivityStatus()) {
					// wait until the first run is finished
					if (task.getLastRunFinishTimestamp() == null) {
						return false;
					}
					return true;
				}
				return false;
			}
			@Override
			public void timeout() {
				// No reaction, the test will fail right after return from this
			}
		}, 20000);

		// Check task status

		Task task = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
		result.computeStatus();
		display("getTask result",result);
		assertSuccess("getTask has failed", result);
		AssertJUnit.assertNotNull(task);
		display("Task after pickup", task);

		ObjectType o = repositoryService.getObject(ObjectType.class, TASK_OPENDJ_SYNC_OID, null, result);
		display("Task after pickup in the repository", o);

		// .. it should be running
		AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());

		// .. and claimed
		AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

		// .. and last run should not be zero
		assertNotNull(task.getLastRunStartTimestamp());
		AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
		assertNotNull(task.getLastRunFinishTimestamp());
		AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

		// Test for extension. This will also roughly test extension processor
		// and schema processor
		PropertyContainer taskExtension = task.getExtension();
		AssertJUnit.assertNotNull(taskExtension);
		display("Task extension", taskExtension);
		Property shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever",
				"shipState"));
		AssertJUnit.assertEquals("capsized", shipStateProp.getValue(String.class));
		Property deadProp = taskExtension
				.findProperty(new QName("http://myself.me/schemas/whatever", "dead"));
		AssertJUnit.assertEquals(Integer.class, deadProp.getValues().iterator().next().getClass());
		AssertJUnit.assertEquals(Integer.valueOf(42), deadProp.getValue(Integer.class));

		// The progress should be 0, as there were no changes yet
		AssertJUnit.assertEquals(0, task.getProgress());

		// Test for presence of a result. It should be there and it should
		// indicate success
		OperationResult taskResult = task.getResult();
		AssertJUnit.assertNotNull(taskResult);

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

		final OperationResult result = new OperationResult(TestSanity.class.getName()
				+ ".test101LiveSyncCreate");
		final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
		AssertJUnit.assertNotNull(syncCycle);

		final Object tokenBefore;
		Property tokenProperty = syncCycle.getExtension().findProperty(SchemaConstants.SYNC_TOKEN);
		if (tokenProperty == null) {
			tokenBefore = null;
		} else {
			tokenBefore = tokenProperty.getValue();
		}

		// WHEN

		AddOperation addOperation = openDJController.getInternalConnection().processAdd(entry);

		// THEN

		AssertJUnit.assertEquals("LDAP add operation failed", ResultCode.SUCCESS,
				addOperation.getResultCode());

		// Wait a bit to give the sync cycle time to detect the change

		waitFor("Waiting for sync cycle to detect change", new Checker() {
			@Override
			public boolean check() throws Exception {
				syncCycle.refresh(result);
				display("SyncCycle while waiting for sync cycle to detect change", syncCycle);
				Object tokenNow = null;
				Property propertyNow = syncCycle.getExtension().findProperty(SchemaConstants.SYNC_TOKEN);
				if (propertyNow == null) {
					tokenNow = null;
				} else {
					tokenNow = propertyNow.getValue();
				}
				if (tokenBefore == null) {
					return (tokenNow != null);
				} else {
					return (!tokenBefore.equals(tokenNow));
				}
			}
			@Override
			public void timeout() {
				// No reaction, the test will fail right after return from this
			}
		}, 30000);

		// Search for the user that should be created now

		Document doc = DOMUtil.getDocument();
		Element nameElement = doc.createElementNS(SchemaConstants.C_NAME.getNamespaceURI(),
				SchemaConstants.C_NAME.getLocalPart());
		nameElement.setTextContent(WILL_NAME);
		Element filter = QueryUtil.createEqualFilter(doc, null, nameElement);

		QueryType query = new QueryType();
		query.setFilter(filter);
		OperationResultType resultType = new OperationResultType();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
		Holder<ObjectListType> listHolder = new Holder<ObjectListType>();

		modelWeb.searchObjects(ObjectTypes.USER.getObjectTypeUri(), query, null,
				listHolder, resultHolder);
		
		ObjectListType objects = listHolder.value;
		assertSuccess("searchObjects has failed", resultHolder.value);
		AssertJUnit.assertEquals("User not found (or found too many)", 1, objects.getObject().size());
		UserType user = (UserType) objects.getObject().get(0);

		AssertJUnit.assertEquals(user.getName(), WILL_NAME);

		// TODO: more checks
	}

	// TODO: insert changes in OpenDJ, let the cycle pick them up

	@Test
	public void test200ImportFromResource() throws Exception {
		displayTestTile("test200ImportFromResource");
		// GIVEN

		OperationResult result = new OperationResult(TestSanity.class.getName()
				+ ".test200ImportFromResource");

		// WHEN
		TaskType taskType = modelWeb.importFromResource(RESOURCE_OPENDJ_OID, IMPORT_OBJECTCLASS);

		// THEN

		System.out.println("importFromResource result:");
		displayJaxb(taskType.getResult(), SchemaConstants.C_RESULT);
		AssertJUnit.assertEquals("importFromResource has failed", OperationResultStatusType.IN_PROGRESS, taskType.getResult().getStatus());
		// Convert the returned TaskType to a more usable Task
		Task task = taskManager.createTaskInstance(taskType);
		AssertJUnit.assertNotNull(task);
		assertNotNull(task.getOid());
		AssertJUnit.assertTrue(task.isAsynchronous());
		AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
		AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

		display("Import task after launch", task);

		TaskType taskAfter = repositoryService.getObject(TaskType.class, task.getOid(), null, result);
		display("Import task in repo after launch", taskAfter);

		result.computeStatus();
		assertSuccess("getObject has failed", result);

		final String taskOid = task.getOid();

		waitFor("Waiting for import to complete", new Checker() {
			@Override
			public boolean check() throws Exception {
				Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
				Holder<ObjectType> objectHolder = new Holder<ObjectType>();
				modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), taskOid,
						new PropertyReferenceListType(), objectHolder, resultHolder);
//				display("getObject result (wait loop)",resultHolder.value);
				assertSuccess("getObject has failed", resultHolder.value);
				Task task = taskManager.createTaskInstance((TaskType) objectHolder.value);
				System.out.println("Import task status: "+task.getExecutionStatus());
				if (task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
					// Task closed, wait finished
					return true;
				}
//				IntegrationTestTools.display("Task result while waiting: ", task.getResult());
				return false;
			}
			@Override
			public void timeout() {
				// No reaction, the test will fail right after return from this
			}
		}, 45000);

		Holder<ObjectType> objectHolder = new Holder<ObjectType>();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		
		modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), task.getOid(),
				new PropertyReferenceListType(), objectHolder, resultHolder);
		
		assertSuccess("getObject has failed", resultHolder.value);
		task = taskManager.createTaskInstance((TaskType) objectHolder.value);

		display("Import task after finish (fetched from model)", task);

		AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());
		
		waitFor("Waiting for task to get released", new Checker() {
			@Override
			public boolean check() throws Exception {
				Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
				Holder<ObjectType> objectHolder = new Holder<ObjectType>();
				modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), taskOid,
						new PropertyReferenceListType(), objectHolder, resultHolder);
//				display("getObject result (wait loop)",resultHolder.value);
				assertSuccess("getObject has failed", resultHolder.value);
				Task task = taskManager.createTaskInstance((TaskType) objectHolder.value);
				System.out.println("Import task status: "+task.getExecutionStatus());
				if (task.getExclusivityStatus() == TaskExclusivityStatus.RELEASED) {
					// Task closed and released, wait finished
					return true;
				}
//				IntegrationTestTools.display("Task result while waiting: ", task.getResult());
				return false;
			}
			public void timeout() {
				Assert.fail("The task was not released after closing");
			}
		}, 1000);

		OperationResult taskResult = task.getResult();
		AssertJUnit.assertNotNull("Task has no result", taskResult);
		AssertJUnit.assertTrue("Task failed", taskResult.isSuccess());

		AssertJUnit.assertTrue("No progress", task.getProgress() > 0);

		// Check if the import created users and shadows

		// Listing of shadows is not supported by the provisioning. So we need
		// to look directly into repository
		List<AccountShadowType> sobjects = repositoryService.listObjects(AccountShadowType.class, null,
				result);
		result.computeStatus();
		assertSuccess("listObjects has failed", result);
		AssertJUnit.assertFalse("No shadows created", sobjects.isEmpty());

		for (AccountShadowType shadow : sobjects) {
			display("Shadow object after import (repo)", shadow);
			assertNotEmpty("No OID in shadow", shadow.getOid()); // This would
																	// be really
																	// strange
																	// ;-)
			assertNotEmpty("No name in shadow", shadow.getName());
			AssertJUnit.assertNotNull("No objectclass in shadow", shadow.getObjectClass());
			AssertJUnit.assertNotNull("Null attributes in shadow", shadow.getAttributes());
			assertAttributeNotNull("No UID in shadow", shadow, ConnectorFactoryIcfImpl.ICFS_UID);
		}
		
		Holder<ObjectListType> listHolder = new Holder<ObjectListType>();
		
		modelWeb.listObjects(ObjectTypes.USER.getObjectTypeUri(), null,
				listHolder, resultHolder);
		
		ObjectListType uobjects = listHolder.value;
		assertSuccess("listObjects has failed", resultHolder.value);
		AssertJUnit.assertFalse("No users created", uobjects.getObject().isEmpty());

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
			AssertJUnit.assertEquals("Wrong accountRef", 1, accountRefs.size());
			ObjectReferenceType accountRef = accountRefs.get(0);
			// here was ref to resource oid, not account oid

			// XXX: HACK: I don't know how to match accounts here
			boolean found = false;
			for (AccountShadowType account : sobjects) {
				if (accountRef.getOid().equals(account.getOid())) {
					found = true;
					break;
				}
			}
			if (!found) {
				AssertJUnit.fail("accountRef does not point to existing account " + accountRef.getOid());
			}
		}
	}

	@Test
	public void test999Shutdown() throws Exception {
		taskManager.shutdown();
		waitFor("waiting for task manager shutdown", new Checker() {
			@Override
			public boolean check() throws Exception {
				return taskManager.getRunningTasks().isEmpty();
			}
			@Override
			public void timeout() {
				// No reaction, the test will fail right after return from this
			}
		}, 10000);
		AssertJUnit.assertEquals("Some tasks left running after shutdown", new HashSet<Task>(),
				taskManager.getRunningTasks());
	}

	// TODO: test for missing/corrupt system configuration
	// TODO: test for missing sample config (bad reference in expression
	// arguments)

	/**
	 * @param resourceOpendjFilename
	 * @return
	 * @throws FileNotFoundException
	 */
	private void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
		LOGGER.trace("importObjectFromFile: {}", filename);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(filename);
		modelService.importObjectsFromStream(stream, MiscUtil.getDefaultImportOptions(), task, result);
	}

}
