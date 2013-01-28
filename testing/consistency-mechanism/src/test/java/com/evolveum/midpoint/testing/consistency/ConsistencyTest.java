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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.testing.consistency;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.util.EmbeddedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceAccountTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

/**
 * Consistency test suite. It tests consistency mechanisms. It works as end-to-end integration test accross all subsystems.
 * 
 * @author Katarina Valalikova
 */
@ContextConfiguration(locations = { "classpath:ctx-consistency-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ConsistencyTest extends AbstractModelIntegrationTest {
	
	private static final String REPO_DIR_NAME = "src/test/resources/repo/";
	private static final String REQUEST_DIR_NAME = "src/test/resources/request/";

	private static final String SYSTEM_CONFIGURATION_FILENAME = REPO_DIR_NAME + "system-configuration.xml";
	private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

	private static final String RESOURCE_OPENDJ_FILENAME = REPO_DIR_NAME + "resource-opendj.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String CONNECTOR_LDAP_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.forgerock.openicf.connectors.ldap-connector/org.identityconnectors.ldap.LdapConnector";

	private static final String TASK_OPENDJ_SYNC_FILENAME = REPO_DIR_NAME + "task-opendj-sync.xml";
	private static final String TASK_OPENDJ_SYNC_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = REPO_DIR_NAME + "sample-configuration-object.xml";
	private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";

	private static final String USER_TEMPLATE_FILENAME = REPO_DIR_NAME + "user-template.xml";
	private static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	private static final String USER_ADMINISTRATOR_FILENAME = REPO_DIR_NAME + "user-administrator.xml";
	private static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

	private static final String USER_JACK_FILENAME = REPO_DIR_NAME + "user-jack.xml";
	// private static final File USER_JACK_FILE = new File(USER_JACK_FILENAME);
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

	private static final String USER_DENIELS_FILENAME = REPO_DIR_NAME + "user-deniels.xml";
	// private static final File USER_JACK_FILE = new File(USER_JACK_FILENAME);
	private static final String USER_DENIELS_OID = "c0c010c0-d34d-b33f-f00d-222111111111";

	private static final String USER_JACK2_FILENAME = REPO_DIR_NAME + "user-jack2.xml";
	private static final String USER_JACK2_OID = "c0c010c0-d34d-b33f-f00d-111111114444";

	private static final String USER_WILL_FILENAME = REPO_DIR_NAME + "user-will.xml";
	private static final String USER_WILL_OID = "c0c010c0-d34d-b33f-f00d-111111115555";

	private static final String USER_JACK_LDAP_UID = "jackie";
	private static final String USER_JACK_LDAP_DN = "uid=" + USER_JACK_LDAP_UID + "," + OPENDJ_PEOPLE_SUFFIX;

	private static final String USER_GUYBRUSH_FILENAME = REPO_DIR_NAME + "user-guybrush.xml";
	private static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111222";

	private static final String USER_GUYBRUSH_NOT_FOUND_FILENAME = REPO_DIR_NAME + "user-guybrush-modify-not-found.xml";
	private static final String USER_GUYBRUSH_NOT_FOUND_OID = "c0c010c0-d34d-b33f-f00d-111111111333";
	
	private static final String USER_HECTOR_NOT_FOUND_FILENAME = REPO_DIR_NAME + "user-hector.xml";
	private static final String USER_HECTOR_NOT_FOUND_OID = "c0c010c0-d34d-b33f-f00d-111111222333";	

	private static final String USER_E_FILENAME = REPO_DIR_NAME + "user-e.xml";
	private static final String USER_E_OID = "c0c010c0-d34d-b33f-f00d-111111111100";
	
	private static final String USER_ELAINE_FILENAME = REPO_DIR_NAME + "user-elaine.xml";
	private static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-111111116666";
	
	private static final String USER_MORGAN_FILENAME = REQUEST_DIR_NAME + "user-morgan.xml";
	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	
	private static final String USER_ANGELIKA_FILENAME = REPO_DIR_NAME + "user-angelika.xml";
	private static final String USER_ANGELIKA_OID = "c0c010c0-d34d-b33f-f00d-111111111888";
	
	private static final String USER_ALICE_FILENAME = REPO_DIR_NAME + "user-alice.xml";
	private static final String USER_ALICE_OID = "c0c010c0-d34d-b33f-f00d-111111111999";
	
	private static final String USER_BOB_NO_FAMILY_NAME_FILENAME = REPO_DIR_NAME + "user-bob-no-family-name.xml";
	private static final String USER_BOB_NO_FAMILY_NAME_OID = "c0c010c0-d34d-b33f-f00d-222111222999";
	
	private static final String ACCOUNT_GUYBRUSH_FILENAME = REPO_DIR_NAME + "account-guybrush.xml";
	private static final String ACCOUNT_GUYBRUSH_OID = "a0c010c0-d34d-b33f-f00d-111111111222";
	
	private static final String ACCOUNT_HECTOR_FILENAME = REPO_DIR_NAME + "account-hector-not-found.xml";
	private static final String ACCOUNT_HECTOR_OID = "a0c010c0-d34d-b33f-f00d-111111222333";

	private static final String ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILENAME = REPO_DIR_NAME + "account-guybrush-not-found.xml";
	private static final String ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID = "a0c010c0-d34d-b33f-f00d-111111111333";

	private static final String ACCOUNT_DENIELS_FILENAME = REPO_DIR_NAME + "account-deniels.xml";
	private static final String ACCOUNT_DENIELS_OID = "a0c010c0-d34d-b33f-f00d-111111111555";

	private static final String ROLE_CAPTAIN_FILENAME = REPO_DIR_NAME + "role-captain.xml";
	private static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-987987cccccc";

	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_DENIELS_FILENAME = "src/test/resources/request/user-modify-add-account-deniels.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM = "src/test/resources/request/user-modify-add-account-communication-problem.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_LINKED_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account-already-exist-linked.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_UNLINKED_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account-already-exist-unlinked.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_COMMUNICATION_PROBLEM_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account-already-exist-communication-problem.xml";
	private static final String REQUEST_USER_MODIFY_DELETE_ACCOUNT = "src/test/resources/request/user-modify-delete-account.xml";
	private static final String REQUEST_USER_MODIFY_DELETE_ACCOUNT_COMMUNICATION_PROBLEM = "src/test/resources/request/user-modify-delete-account-communication-problem.xml";
	private static final String REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT = "src/test/resources/request/account-guybrush-modify-attributes.xml";
	private static final String REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM = "src/test/resources/request/account-modify-attrs-communication-problem.xml";
	private static final String REQUEST_ADD_ACCOUNT_JACKIE = "src/test/resources/request/add-account-jack.xml";

	private static final String TASK_OPENDJ_RECONCILIATION_FILENAME = "src/test/resources/repo/task-opendj-reconciliation.xml";
	private static final String TASK_OPENDJ_RECONCILIATION_OID = "91919191-76e0-59e2-86d6-3d4f02d30000";

	private static final String LDIF_WILL_FILENAME = "src/test/resources/request/will.ldif";
	private static final String LDIF_ELAINE_FILENAME = "src/test/resources/request/elaine.ldif";
	private static final String LDIF_MORGAN_FILENAME = "src/test/resources/request/morgan.ldif";

	private static final QName IMPORT_OBJECTCLASS = new QName(
			"http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff",
			"AccountObjectClass");

	private static final Trace LOGGER = TraceManager.getTrace(ConsistencyTest.class);

	private static final String NS_MY = "http://whatever.com/my";
	private static final QName MY_SHIP_STATE = new QName(NS_MY, "shipState");

	/**
	 * Unmarshalled resource definition to reach the embedded OpenDJ instance.
	 * Used for convenience - the tests method may find it handy.
	 */
	private static ResourceType resourceTypeOpenDjrepo;
	private static String accountShadowOidOpendj;
	private static String originalJacksPassword;

	// private int lastSyncToken;
	
	public ConsistencyTest() throws JAXBException {
		super();
	}

	@BeforeMethod
	public void beforeMethod() throws Exception {
		LOGGER.info("BEFORE METHOD");
		OperationResult result = new OperationResult("get administrator");
		PrismObject<UserType> object = modelService.getObject(UserType.class,
				SystemObjectsType.USER_ADMINISTRATOR.value(), null, null, result);

		assertNotNull("Administrator user is null", object.asObjectable());
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(object.asObjectable(), null));

		LOGGER.info("BEFORE METHOD END");
	}

	@AfterMethod
	public void afterMethod() {
		LOGGER.info("AFTER METHOD");
		SecurityContextHolder.getContext().setAuthentication(null);
		LOGGER.info("AFTER METHOD END");
	}

	// This will get called from the superclass to init the repository
	// It will be called only once
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		addObjectFromFile(USER_ADMINISTRATOR_FILENAME, UserType.class, initResult);

		// This should discover the connectors
		LOGGER.trace("initSystem: trying modelService.postInit()");
		modelService.postInit(initResult);
		LOGGER.trace("initSystem: modelService.postInit() done");

		// We need to add config after calling postInit() so it will not be
		// applied.
		// we want original logging configuration from the test logback config
		// file, not
		// the one from the system config.
		addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);

		// Add broken connector before importing resources
		// addObjectFromFile(CONNECTOR_BROKEN_FILENAME, initResult);

		// Need to import instead of add, so the (dynamic) connector reference
		// will be resolved
		// correctly
		importObjectFromFile(RESOURCE_OPENDJ_FILENAME, initResult);
		// importObjectFromFile(RESOURCE_DERBY_FILENAME, initResult);
		// importObjectFromFile(RESOURCE_BROKEN_FILENAME, initResult);

		addObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME, GenericObjectType.class, initResult);
		addObjectFromFile(USER_TEMPLATE_FILENAME, UserTemplateType.class, initResult);
		// addObjectFromFile(ROLE_SAILOR_FILENAME, initResult);
		// addObjectFromFile(ROLE_PIRATE_FILENAME, initResult);
		addObjectFromFile(ROLE_CAPTAIN_FILENAME, RoleType.class, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
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
	 * @throws CommunicationException
	 */
	@Test
	public void test000Integrity() throws ObjectNotFoundException, SchemaException, CommunicationException {
		displayTestTile(this, "test000Integrity");
		assertNotNull(modelWeb);
		assertNotNull(modelService);
		assertNotNull(repositoryService);
		assertTrue(isSystemInitialized());
		assertNotNull(taskManager);

		assertNotNull(prismContext);
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		assertNotNull(schemaRegistry);
		// This is defined in extra schema. So this effectively checks whether
		// the extra schema was loaded
		PrismPropertyDefinition shipStateDefinition = schemaRegistry
				.findPropertyDefinitionByElementName(MY_SHIP_STATE);
		assertNotNull("No my:shipState definition", shipStateDefinition);
		assertEquals("Wrong maxOccurs in my:shipState definition", 1, shipStateDefinition.getMaxOccurs());

		assertNoRepoCache();

		OperationResult result = new OperationResult(ConsistencyTest.class.getName() + ".test000Integrity");

		// Check if OpenDJ resource was imported correctly

		PrismObject<ResourceType> openDjResource = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, result);
		display("Imported OpenDJ resource (repository)", openDjResource);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());
		assertNoRepoCache();

		String ldapConnectorOid = openDjResource.asObjectable().getConnectorRef().getOid();
		PrismObject<ConnectorType> ldapConnector = repositoryService.getObject(ConnectorType.class,
				ldapConnectorOid, result);
		display("LDAP Connector: ", ldapConnector);

		// Check if Derby resource was imported correctly

		// PrismObject<ResourceType> derbyResource =
		// repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID,
		// null,
		// result);
		// AssertJUnit.assertEquals(RESOURCE_DERBY_OID, derbyResource.getOid());

		// assertCache();
		//
		// String dbConnectorOid =
		// derbyResource.asObjectable().getConnectorRef().getOid();
		// PrismObject<ConnectorType> dbConnector =
		// repositoryService.getObject(ConnectorType.class, dbConnectorOid,
		// null, result);
		// display("DB Connector: ", dbConnector);
		//
		// // Check if password was encrypted during import
		// Object configurationPropertiesElement =
		// JAXBUtil.findElement(derbyResource.asObjectable().getConfiguration().getAny(),
		// new QName(dbConnector.asObjectable().getNamespace(),
		// "configurationProperties"));
		// Object passwordElement =
		// JAXBUtil.findElement(JAXBUtil.listChildElements(configurationPropertiesElement),
		// new QName(dbConnector.asObjectable().getNamespace(), "password"));
		// System.out.println("Password element: " + passwordElement);

		// TODO: test if OpenDJ and Derby are running

		repositoryService.getObject(GenericObjectType.class, SAMPLE_CONFIGURATION_OBJECT_OID, result);
	}

	/**
	 * Test the testResource method. Expect a complete success for now.
	 */
	@Test
	public void test001TestConnectionOpenDJ() throws FaultMessage, JAXBException, ObjectNotFoundException,
			SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		displayTestTile("test001TestConnectionOpenDJ");

		// GIVEN

		assertNoRepoCache();

		// WHEN
		OperationResultType result = modelWeb.testResource(RESOURCE_OPENDJ_OID);

		// THEN

		assertNoRepoCache();

		displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

		assertSuccess("testResource has failed", result);

		OperationResult opResult = new OperationResult(ConsistencyTest.class.getName()
				+ ".test001TestConnectionOpenDJ");

		PrismObject<ResourceType> resourceOpenDjRepo = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, opResult);
		resourceTypeOpenDjrepo = resourceOpenDjRepo.asObjectable();

		assertNoRepoCache();
		assertEquals(RESOURCE_OPENDJ_OID, resourceTypeOpenDjrepo.getOid());
		display("Initialized OpenDJ resource (respository)", resourceTypeOpenDjrepo);
		assertNotNull("Resource schema was not generated", resourceTypeOpenDjrepo.getSchema());
		Element resourceOpenDjXsdSchemaElement = ResourceTypeUtil
				.getResourceXsdSchema(resourceTypeOpenDjrepo);
		assertNotNull("Resource schema was not generated", resourceOpenDjXsdSchemaElement);

		PrismObject<ResourceType> openDjResourceProvisioninig = provisioningService.getObject(
				ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult);
		display("Initialized OpenDJ resource resource (provisioning)", openDjResourceProvisioninig);

		PrismObject<ResourceType> openDjResourceModel = provisioningService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, null, opResult);
		display("Initialized OpenDJ resource OpenDJ resource (model)", openDjResourceModel);

		checkOpenDjResource(resourceTypeOpenDjrepo, "repository");

		System.out.println("------------------------------------------------------------------");
		display("OpenDJ resource schema (repo XML)",
				DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(resourceOpenDjRepo)));
		System.out.println("------------------------------------------------------------------");

		checkOpenDjResource(openDjResourceProvisioninig.asObjectable(), "provisioning");
		checkOpenDjResource(openDjResourceModel.asObjectable(), "model");
		// TODO: model web

	}

	private void checkRepoOpenDjResource() throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(ConsistencyTest.class.getName()
				+ ".checkRepoOpenDjResource");
		PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, result);
		checkOpenDjResource(resource.asObjectable(), "repository");
	}

	// private void checkRepoDerbyResource() throws ObjectNotFoundException,
	// SchemaException {
	// OperationResult result = new
	// OperationResult(ConsistencyTest.class.getName()+".checkRepoDerbyResource");
	// PrismObject<ResourceType> resource =
	// repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null,
	// result);
	// checkDerbyResource(resource, "repository");
	// }

	// private void checkDerbyResource(PrismObject<ResourceType> resource,
	// String source) {
	// checkDerbyConfiguration(resource, source);
	// }

	/**
	 * Checks if the resource is internally consistent, if it has everything it
	 * should have.
	 * 
	 * @throws SchemaException
	 */
	private void checkOpenDjResource(ResourceType resource, String source) throws SchemaException {
		assertNotNull("Resource from " + source + " is null", resource);
		assertNotNull("Resource from " + source + " has null configuration", resource.getConnectorConfiguration());
		assertNotNull("Resource from " + source + " has null schema", resource.getSchema());
		checkOpenDjSchema(resource, source);
		assertNotNull("Resource from " + source + " has null schemahandling", resource.getSchemaHandling());
		checkOpenDjSchemaHandling(resource, source);
		assertNotNull("Resource from " + source + " has null capabilities", resource.getCapabilities());
		if (!source.equals("repository")) {
			// This is generated on the fly in provisioning
			assertNotNull("Resource from " + source + " has null native capabilities",
					resource.getCapabilities().getNative());
			assertFalse("Resource from " + source + " has empty native capabilities", resource
					.getCapabilities().getNative().getAny().isEmpty());
		}
		assertNotNull("Resource from " + source + " has null configured capabilities", resource.getCapabilities().getConfigured());
		assertFalse("Resource from " + source + " has empty configured capabilities", resource.getCapabilities().getConfigured()
				.getAny().isEmpty());
		assertNotNull("Resource from " + source + " has null synchronization", resource.getSynchronization());
		checkOpenDjConfiguration(resource.asPrismObject(), source);
	}

	private void checkOpenDjSchema(ResourceType resource, String source) throws SchemaException {
		ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition accountDefinition = schema.findDefaultAccountDefinition();
		assertNotNull("Schema does not define any account (resource from " + source + ")", accountDefinition);
		Collection<ResourceAttributeDefinition> identifiers = accountDefinition.getIdentifiers();
		assertFalse("No account identifiers (resource from " + source + ")", identifiers == null
				|| identifiers.isEmpty());
		// TODO: check for naming attributes and display names, etc

		ActivationCapabilityType capActivation = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (capActivation != null && capActivation.getEnableDisable() != null
				&& capActivation.getEnableDisable().getAttribute() != null) {
			// There is simulated activation capability, check if the attribute
			// is in schema.
			QName enableAttrName = capActivation.getEnableDisable().getAttribute();
			ResourceAttributeDefinition enableAttrDef = accountDefinition
					.findAttributeDefinition(enableAttrName);
			display("Simulated activation attribute definition", enableAttrDef);
			assertNotNull("No definition for enable attribute " + enableAttrName
					+ " in account (resource from " + source + ")", enableAttrDef);
			assertTrue("Enable attribute " + enableAttrName + " is not ignored (resource from " + source
					+ ")", enableAttrDef.isIgnored());
		}
	}

	private void checkOpenDjSchemaHandling(ResourceType resource, String source) {
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		for (ResourceAccountTypeDefinitionType accountType : schemaHandling.getAccountType()) {
			String name = accountType.getName();
			assertNotNull("Resource " + resource + " from " + source
					+ " has an schemaHandlig account definition without a name", name);
			assertNotNull("Account type " + name + " in " + resource + " from " + source
					+ " does not have object class", accountType.getObjectClass());
		}
	}

	private void checkOpenDjConfiguration(PrismObject<ResourceType> resource, String source) {
		checkOpenResourceConfiguration(resource, CONNECTOR_LDAP_NAMESPACE, "credentials", 7, source);
	}

	// private void checkDerbyConfiguration(PrismObject<ResourceType> resource,
	// String source) {
	// checkOpenResourceConfiguration(resource, CONNECTOR_DBTABLE_NAMESPACE,
	// "password", 10, source);
	// }

	private void checkOpenResourceConfiguration(PrismObject<ResourceType> resource,
			String connectorNamespace, String credentialsPropertyName, int numConfigProps, String source) {
		PrismContainer<Containerable> configurationContainer = resource
				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container in " + resource + " from " + source, configurationContainer);
		PrismContainer<Containerable> configPropsContainer = configurationContainer
				.findContainer(SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No configuration properties container in " + resource + " from " + source,
				configPropsContainer);
		List<Item<?>> configProps = configPropsContainer.getValue().getItems();
		assertEquals("Wrong number of config properties in " + resource + " from " + source, numConfigProps,
				configProps.size());
		PrismProperty<Object> credentialsProp = configPropsContainer.findProperty(new QName(
				connectorNamespace, credentialsPropertyName));
		if (credentialsProp == null) {
			// The is the heisenbug we are looking for. Just dump the entire
			// damn thing.
			display("Configuration with the heisenbug", configurationContainer.dump());
		}
		assertNotNull("No credentials property in " + resource + " from " + source, credentialsProp);
		assertEquals("Wrong number of credentials property value in " + resource + " from " + source, 1,
				credentialsProp.getValues().size());
		PrismPropertyValue<Object> credentialsPropertyValue = credentialsProp.getValues().iterator().next();
		assertNotNull("No credentials property value in " + resource + " from " + source,
				credentialsPropertyValue);
		Object rawElement = credentialsPropertyValue.getRawElement();
		// assertTrue("Wrong element class " + rawElement.getClass() + " in " +
		// resource + " from " + source,
		// rawElement instanceof Element);
		// Element rawDomElement = (Element) rawElement;
		// // display("LDAP credentials raw element",
		// // DOMUtil.serializeDOMToString(rawDomElement));
		// assertEquals("Wrong credentials element namespace in " + resource +
		// " from " + source,
		// connectorNamespace, rawDomElement.getNamespaceURI());
		// assertEquals("Wrong credentials element local name in " + resource +
		// " from " + source,
		// credentialsPropertyName, rawDomElement.getLocalName());
		// Element encryptedDataElement = DOMUtil.getChildElement(rawDomElement,
		// new QName(DOMUtil.NS_XML_ENC,
		// "EncryptedData"));
		// assertNotNull("No EncryptedData element", encryptedDataElement);
		// assertEquals("Wrong EncryptedData element namespace in " + resource +
		// " from " + source,
		// DOMUtil.NS_XML_ENC, encryptedDataElement.getNamespaceURI());
		// assertEquals("Wrong EncryptedData element local name in " + resource
		// + " from " + source,
		// "EncryptedData", encryptedDataElement.getLocalName());
	}

	private UserType testAddUserToRepo(String displayMessage, String fileName, String userOid)
			throws FileNotFoundException, ObjectNotFoundException, SchemaException, EncryptionException,
			ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException,
			ConfigurationException, PolicyViolationException, SecurityViolationException {

		checkRepoOpenDjResource();
		assertNoRepoCache();

		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(fileName));
		UserType userType = user.asObjectable();
		PrismAsserts.assertParentConsistency(user);

		// Encrypt Jack's password
		protector.encrypt(userType.getCredentials().getPassword().getValue());
		PrismAsserts.assertParentConsistency(user);

		OperationResult result = new OperationResult("add user");
		// Holder<OperationResultType> resultHolder = new
		// Holder<OperationResultType>(result);
		// Holder<String> oidHolder = new Holder<String>();

		display("Adding user object", userType);

		Task task = taskManager.createTaskInstance();
		// WHEN
		ObjectDelta delta = ObjectDelta.createAddDelta(user);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(delta);
		modelService.executeChanges(deltas, null, task, result);
//		String oid = modelService.addObject(user, task, result);

		// THEN

		assertNoRepoCache();
		// displayJaxb("addObject result:", resultHolder.value,
		// SchemaConstants.C_RESULT);
		// assertSuccess("addObject has failed", result);

//		AssertJUnit.assertEquals(userOid, oid);

		return userType;
	}

	private Collection<ObjectDelta<? extends ObjectType>> createDeltaCollection(ObjectDelta delta) {
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(delta);
		return deltas;
	}

	/**
	 * Attempt to add new user. It is only added to the repository, so check if
	 * it is in the repository after the operation.
	 */
	@Test
	public void test010AddUser() throws Exception {

		UserType userType = testAddUserToRepo("test010AddUser", USER_JACK_FILENAME, USER_JACK_OID);

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		PrismObject<UserType> uObject = repositoryService
				.getObject(UserType.class, USER_JACK_OID, repoResult);
		UserType repoUser = uObject.asObjectable();

		repoResult.computeStatus();
		display("repository.getObject result", repoResult);
		assertSuccess("getObject has failed", repoResult);
		AssertJUnit.assertEquals(USER_JACK_OID, repoUser.getOid());
		PrismAsserts.assertEqualsPolyString("User full name not equals as expected.", userType.getFullName(),
				repoUser.getFullName());

		// TODO: better checks
	}

	private OperationResultType modifyUserAddAccount(String modifyUserRequest) throws FileNotFoundException,
			JAXBException, FaultMessage, ObjectNotFoundException, SchemaException, DirectoryException, ObjectAlreadyExistsException {
		checkRepoOpenDjResource();
		assertNoRepoCache();

		ObjectModificationType objectChange = unmarshallJaxbFromFile(modifyUserRequest,
				ObjectModificationType.class);

		// WHEN
		OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

		// THEN
		assertNoRepoCache();
		return result;
	}

	/**
	 * Add account to user. This should result in account provisioning. Check if
	 * that happens in repo and in LDAP.
	 */
	@Test
	public void test013prepareOpenDjWithAccounts() throws Exception {
		displayTestTile("test013prepareOpenDjWithAccounts");
		OperationResult parentResult = new OperationResult("test013prepareOpenDjWithAccounts");

		AccountShadowType jackeAccount = unmarshallJaxbFromFile(REQUEST_ADD_ACCOUNT_JACKIE,
				AccountShadowType.class);

		Task task = taskManager.createTaskInstance();
		String oid = provisioningService.addObject(jackeAccount.asPrismObject(), null, null, task, parentResult);
		PrismObject<AccountShadowType> jackFromRepo = repositoryService.getObject(AccountShadowType.class,
				oid, parentResult);
		LOGGER.debug("account jack after provisioning: {}", jackFromRepo.dump());

		PrismObject<UserType> jackUser = repositoryService.getObject(UserType.class, USER_JACK_OID,
				parentResult);
		ObjectReferenceType ort = new ObjectReferenceType();
		ort.setOid(oid);

		jackUser.asObjectable().getAccountRef().add(ort);

		PrismObject<UserType> jackUserRepo = repositoryService.getObject(UserType.class, USER_JACK_OID,
				parentResult);
		ObjectDelta delta = DiffUtil.diff(jackUserRepo, jackUser);

		repositoryService.modifyObject(UserType.class, USER_JACK_OID, delta.getModifications(), parentResult);

		// GIVEN
		// OperationResultType result =
		// modifyUserAddAccount(REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME);
		// displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
		// assertSuccess("modifyObject has failed", result);

		// Check if user object was modified in the repo

		OperationResult repoResult = new OperationResult("getObject");


		PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID,
				repoResult);
		UserType repoUserType = repoUser.asObjectable();

		repoResult.computeStatus();
		assertSuccess("getObject has failed", repoResult);
		display("User (repository)", repoUser);

		List<ObjectReferenceType> accountRefs = repoUserType.getAccountRef();
		assertEquals("No accountRefs", 1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		accountShadowOidOpendj = accountRef.getOid();
		assertFalse(accountShadowOidOpendj.isEmpty());

		// Check if shadow was created in the repo

		repoResult = new OperationResult("getObject");

		PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class,
				accountShadowOidOpendj, repoResult);
		AccountShadowType repoShadowType = repoShadow.asObjectable();
		repoResult.computeStatus();
		assertSuccess("getObject has failed", repoResult);
		display("Shadow (repository)", repoShadow);
		assertNotNull(repoShadowType);
		assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

		assertNotNull("Shadow stored in repository has no name", repoShadowType.getName());
		// Check the "name" property, it should be set to DN, not entryUUID
		assertEquals("Wrong name property", USER_JACK_LDAP_DN.toLowerCase(), repoShadowType.getName()
				.getOrig().toLowerCase());

		// check attributes in the shadow: should be only identifiers (ICF UID)
		String uid = checkRepoShadow(repoShadow);

		// check if account was created in LDAP

		SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);

		display("LDAP account", entry);

//		OpenDJController.assertAttribute(entry, "uid", "jackie");
		OpenDJController.assertAttribute(entry, "givenName", "Jack");
		OpenDJController.assertAttribute(entry, "sn", "Sparrow");
		OpenDJController.assertAttribute(entry, "cn", "Jack Sparrow");
		// The "l" attribute is assigned indirectly through schemaHandling and
		// config object
		// OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

		// originalJacksPassword = OpenDJController.getAttributeValue(entry,
		// "userPassword");
		// assertNotNull("Pasword was not set on create",
		// originalJacksPassword);
		// System.out.println("password after create: " +
		// originalJacksPassword);

		// Use getObject to test fetch of complete shadow

		assertNoRepoCache();

		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		Holder<ObjectType> objectHolder = new Holder<ObjectType>();

		// WHEN
		PropertyReferenceListType resolve = new PropertyReferenceListType();
//		List<ObjectOperationOptions> options = new ArrayList<ObjectOperationOptions>();
		OperationOptionsType oot = new OperationOptionsType();
		modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj, oot,
				objectHolder, resultHolder);

		// THEN
		assertNoRepoCache();
		displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
		assertSuccess("getObject has failed", resultHolder.value);

		AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
		display("Shadow (model)", modelShadow);

		AssertJUnit.assertNotNull(modelShadow);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

		assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
		assertAttribute(modelShadow, resourceTypeOpenDjrepo, "uid", "jackie");
		assertAttribute(modelShadow, resourceTypeOpenDjrepo, "givenName", "Jack");
		assertAttribute(modelShadow, resourceTypeOpenDjrepo, "sn", "Sparrow");
		assertAttribute(modelShadow, resourceTypeOpenDjrepo, "cn", "Jack Sparrow");
		// assertAttribute(modelShadow, resourceTypeOpenDjrepo, "l",
		// "middle of nowhere");
		assertNull("carLicense attribute sneaked to LDAP",
				OpenDJController.getAttributeValue(entry, "carLicense"));

		assertNotNull("Activation is null", modelShadow.getActivation());
		assertNotNull("No 'enabled' in the shadow", modelShadow.getActivation().isEnabled());
		assertTrue("The account is not enabled in the shadow", modelShadow.getActivation().isEnabled());

		displayTestTile("test013prepareOpenDjWithAccounts - add second account");

		OperationResult secondResult = new OperationResult(
				"test013prepareOpenDjWithAccounts - add second account");

		AccountShadowType shadow = unmarshallJaxbFromFile(ACCOUNT_DENIELS_FILENAME, AccountShadowType.class);

		provisioningService.addObject(shadow.asPrismObject(), null, null, task, secondResult);

		addObjectFromFile(USER_DENIELS_FILENAME, UserType.class, secondResult);

		// GIVEN
		// result =
		// modifyUserAddAccount(REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME);
		// displayJaxb("modifyObject result", parentResult,
		// SchemaConstants.C_RESULT);
		// assertSuccess("modifyObject has failed", parentResult);

		// Check if user object was modified in the repo

	}

	@Test
	public void test014addAccountAlreadyExistLinked() throws Exception {
		displayTestTile("test014addAccountAlreadyExistLinked");
		
		// GIVEN
		OperationResult parentResult = new OperationResult("Add account already exist linked");
		testAddUserToRepo("test014testAssAccountAlreadyExistLinked", USER_JACK2_FILENAME, USER_JACK2_OID);

		PrismObject<UserType> user = repositoryService
				.getObject(UserType.class, USER_JACK2_OID, parentResult);
		assertEquals(0, user.asObjectable().getAccountRef().size());

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_LINKED_OPENDJ_FILENAME,
				ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		//check if the jackie account already exists on the resource
		UserType jackUser = repositoryService.getObject(UserType.class, USER_JACK_OID, parentResult).asObjectable();
		assertNotNull(jackUser);
		assertEquals(1, jackUser.getAccountRef().size());
		PrismObject<AccountShadowType> jackUserAccount = repositoryService.getObject(AccountShadowType.class, jackUser.getAccountRef().get(0).getOid(), parentResult);
		display("Jack's account: ", jackUserAccount.dump());
		
		Task task = taskManager.createTaskInstance();
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(USER_JACK2_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, parentResult);

		// THEN
		user = modelService.getObject(UserType.class, USER_JACK2_OID, null, task, parentResult);
		assertEquals(1, user.asObjectable().getAccountRef().size());
		MidPointAsserts.assertAssignments(user, 1);

		PrismObject<AccountShadowType> newAccount = modelService.getObject(AccountShadowType.class, user
				.asObjectable().getAccountRef().get(0).getOid(), null, task, parentResult);
		AccountShadowType createdShadow = newAccount.asObjectable();
		display("Created account: ", createdShadow);

		AssertJUnit.assertNotNull(createdShadow);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, createdShadow.getResourceRef().getOid());
		assertAttributeNotNull(createdShadow, ConnectorFactoryIcfImpl.ICFS_UID);
//		assertAttribute(createdShadow, resourceTypeOpenDjrepo, "uid", "jackie");
		assertAttribute(createdShadow, resourceTypeOpenDjrepo, "givenName", "Jack");
		assertAttribute(createdShadow, resourceTypeOpenDjrepo, "sn", "Russel");
		assertAttribute(createdShadow, resourceTypeOpenDjrepo, "cn", "Jack Russel");

	}

	@Test
	public void test015addAccountAlreadyExistUnlinked() throws Exception {
		final String TEST_NAME = "test015addAccountAlreadyExistUnlinked";
		displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult parentResult = new OperationResult("Add account already exist unlinked.");
		Entry entry = openDJController.addEntryFromLdifFile(LDIF_WILL_FILENAME);
		SearchResultEntry searchResult = openDJController.searchByUid("wturner");
		OpenDJController.assertAttribute(searchResult, "l", "Caribbean");
		OpenDJController.assertAttribute(searchResult, "givenName", "Will");
		OpenDJController.assertAttribute(searchResult, "sn", "Turner");
		OpenDJController.assertAttribute(searchResult, "cn", "Will Turner");
		OpenDJController.assertAttribute(searchResult, "mail", "will.turner@blackpearl.com");
		OpenDJController.assertAttribute(searchResult, "telephonenumber", "+1 408 555 1234");
		OpenDJController.assertAttribute(searchResult, "facsimiletelephonenumber", "+1 408 555 4321");
		String dn = searchResult.getDN().toString();
		assertEquals("DN attribute " + dn + " not equals", dn, "uid=wturner,ou=People,dc=example,dc=com");

		testAddUserToRepo("add user - test015 account already exist unlinked", USER_WILL_FILENAME,
				USER_WILL_OID);
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_WILL_OID, parentResult);
		assertNotNull(user);
		assertEquals(0, user.asObjectable().getAccountRef().size());

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_UNLINKED_OPENDJ_FILENAME,
				ObjectModificationType.class);

		ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta<UserType> modifyDelta = ObjectDelta.createModifyDelta(USER_WILL_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		
		// WHEN
		displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, parentResult);

		// THEN
		displayThen(TEST_NAME);
		user = repositoryService.getObject(UserType.class, USER_WILL_OID, parentResult);
		assertNotNull(user);
		List<ObjectReferenceType> accountRefs = user.asObjectable().getAccountRef();
		assertEquals(1, accountRefs.size());
		MidPointAsserts.assertAssignments(user, 1);

		PrismObject<AccountShadowType> account = provisioningService.getObject(AccountShadowType.class,
				accountRefs.get(0).getOid(),null,  parentResult);

		ResourceAttributeContainer attributes = ResourceObjectShadowUtil.getAttributesContainer(account);

		assertEquals("shadow secondary identifier not equal with the account dn. ", dn, attributes
				.getSecondaryIdentifier().getRealValue(String.class));

		String identifier = attributes.getIdentifier().getRealValue(String.class);

		openDJController.searchAndAssertByEntryUuid(identifier);

	}

	public void test016addObjectAlreadyExistLinkToAnother() {

	}

	@Test
	public void test017deleteObjectNotFound() throws Exception {
		displayTestTile("test017deleteObjectNotFound");
		OperationResult parentResult = new OperationResult("Delete object not found");

		addObjectFromFile(ACCOUNT_GUYBRUSH_FILENAME, AccountShadowType.class, parentResult);
		addObjectFromFile(USER_GUYBRUSH_FILENAME, UserType.class, parentResult);

		ObjectModificationType objectChange = unmarshallJaxbFromFile(REQUEST_USER_MODIFY_DELETE_ACCOUNT,
				ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(USER_GUYBRUSH_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, parentResult);

		// WHEN
		ObjectDelta deleteDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, ACCOUNT_GUYBRUSH_OID, prismContext);
		deltas = createDeltaCollection(deleteDelta);
		modelService.executeChanges(deltas, null, task, parentResult);

		try {
			repositoryService.getObject(AccountShadowType.class, ACCOUNT_GUYBRUSH_OID, parentResult);
		} catch (Exception ex) {
			if (!(ex instanceof ObjectNotFoundException)) {
				fail("Expected ObjectNotFoundException but got " + ex);
			}
		}

		PrismObject<UserType> modificatedUser = repositoryService.getObject(UserType.class,
				USER_GUYBRUSH_OID, parentResult);
		assertNotNull(modificatedUser);
		assertEquals("Expecting that user does not have account reference, but found "
				+ modificatedUser.asObjectable().getAccountRef().size() + " reference", 0, modificatedUser
				.asObjectable().getAccountRef().size());

		repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, parentResult);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test018AmodifyObjectNotFound() throws Exception {
		displayTestTile("test018AmodifyObjectNotFound");
		OperationResult parentResult = new OperationResult(
				"Modify account not found => reaction: Delete account");

		addObjectFromFile(ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILENAME, AccountShadowType.class, parentResult);
		addObjectFromFile(USER_GUYBRUSH_FILENAME, UserType.class, parentResult);

		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID,
				parentResult);
		assertNotNull(user);
		assertEquals("Expecting that user has one account reference, but found "
				+ user.asObjectable().getAccountRef().size() + " reference", 1, user.asObjectable()
				.getAccountRef().size());

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(ACCOUNT_GUYBRUSH_OID, delta.getModifications(), AccountShadowType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, parentResult);
//		modelService.modifyObject(AccountShadowType.class, ACCOUNT_GUYBRUSH_OID, delta.getModifications(),
//				task, parentResult);

		try {
			repositoryService.getObject(AccountShadowType.class, ACCOUNT_GUYBRUSH_OID, parentResult);
			fail("Expected ObjectNotFound but did not get one.");
		} catch (Exception ex) {
			if (!(ex instanceof ObjectNotFoundException)) {
				fail("Expected ObjectNotFoundException but got " + ex);
			}
		}

		PrismObject<UserType> modificatedUser = repositoryService.getObject(UserType.class,
				USER_GUYBRUSH_OID, parentResult);
		assertNotNull(modificatedUser);
		assertEquals("Expecting that user does not have account reference, but found "
				+ modificatedUser.asObjectable().getAccountRef().size() + " reference", 0, modificatedUser
				.asObjectable().getAccountRef().size());

		repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, parentResult);

	}

	@Test
	public void test018BmodifyObjectNotFoundAssignedAccount() throws Exception {
		displayTestTile("test018BmodifyObjectNotFoundAssignedAccount");
		
		// GIVEN
		OperationResult parentResult = new OperationResult(
				"Modify account not found => reaction: Re-create account, apply changes.");

		addObjectFromFile(ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILENAME, AccountShadowType.class, parentResult);
		addObjectFromFile(USER_GUYBRUSH_NOT_FOUND_FILENAME, UserType.class, parentResult);

		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID,
				parentResult);
		assertNotNull(user);
		assertEquals("Expecting that user has one account reference, but found "
				+ user.asObjectable().getAccountRef().size() + " reference", 1, user.asObjectable()
				.getAccountRef().size());

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(ACCOUNT_GUYBRUSH_OID, delta.getModifications(), AccountShadowType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		
		// WHEN
		modelService.executeChanges(deltas, null, task, parentResult);
//		modelService.modifyObject(AccountShadowType.class, ACCOUNT_GUYBRUSH_OID, delta.getModifications(),
//				task, parentResult);

		// THEN
		PrismObject<UserType> modificatedUser = repositoryService.getObject(UserType.class,
				USER_GUYBRUSH_OID, parentResult);
		assertNotNull(modificatedUser);
		List<ObjectReferenceType> referenceList = modificatedUser.asObjectable().getAccountRef();
		assertEquals("Expecting that user has one account reference, but found " + referenceList.size()
				+ " reference", 1, referenceList.size());

		assertFalse("Old shadow oid and new shadow oid should not be the same.", ACCOUNT_GUYBRUSH_OID.equals(referenceList.get(0).getOid()));
		
		PrismObject<AccountShadowType> modifiedAccount = provisioningService.getObject(
				AccountShadowType.class, referenceList.get(0).getOid(), null, parentResult);
		assertNotNull(modifiedAccount);
		PrismAsserts.assertEqualsPolyString("Wrong shadw name", "uid=guybrush123,ou=people,dc=example,dc=com", modifiedAccount.asObjectable().getName());
		ResourceAttributeContainer attributeContainer = ResourceObjectShadowUtil
				.getAttributesContainer(modifiedAccount);
		assertAttribute(modifiedAccount.asObjectable(),
				new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "roomNumber"),
				"cabin");
		// assertEquals(
		// attributeContainer.findProperty(
		// new QName(resourceTypeOpenDjrepo.getNamespace(),
		// "roomNumber")).getRealValue(
		// String.class), "cabin");
		assertNotNull(attributeContainer.findProperty(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceTypeOpenDjrepo), "businessCategory")));

	}

	@Test
	public void test018CgetObjectNotFoundAssignedAccount() throws Exception {
		displayTestTile("test018CgetObjectNotFoundAssignedAccount");
		
		// GIVEN
		OperationResult parentResult = new OperationResult(
				"Get account not found => reaction: Re-create account, return re-created.");

		addObjectFromFile(ACCOUNT_HECTOR_FILENAME, AccountShadowType.class, parentResult);
		addObjectFromFile(USER_HECTOR_NOT_FOUND_FILENAME, UserType.class, parentResult);

		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_HECTOR_NOT_FOUND_OID,
				parentResult);
		assertNotNull(user);
		assertEquals("Expecting that user has one account reference, but found "
				+ user.asObjectable().getAccountRef().size() + " reference", 1, user.asObjectable()
				.getAccountRef().size());

		Task task = taskManager.createTaskInstance();

		//WHEN
		PrismObject<UserType> modificatedUser = modelService.getObject(UserType.class, USER_HECTOR_NOT_FOUND_OID, null, task, parentResult);
		
		// THEN
		assertNotNull(modificatedUser);
		List<ObjectReferenceType> referenceList = modificatedUser.asObjectable().getAccountRef();
		assertEquals("Expecting that user has one account reference, but found " + referenceList.size()
				+ " reference", 1, referenceList.size());

		assertFalse("Old shadow oid and new shadow oid should not be the same.", ACCOUNT_GUYBRUSH_OID.equals(referenceList.get(0).getOid()));
		
		PrismObject<AccountShadowType> modifiedAccount = modelService.getObject(
				AccountShadowType.class, referenceList.get(0).getOid(), null, task, parentResult);
		assertNotNull(modifiedAccount);
		PrismAsserts.assertEqualsPolyString("Wrong shadw name", "uid=hector,ou=people,dc=example,dc=com", modifiedAccount.asObjectable().getName());

	}

	@Test
	public void test019StopOpenDj() throws Exception {
		displayTestTile("test019TestConnectionOpenDJ");
		openDJController.stop();

		assertEquals("Resource is running", false, EmbeddedUtils.isRunning());

	}

	@Test
	public void test020addObjectCommunicationProblem() throws Exception {
		displayTestTile("test020 add object - communication problem");
		OperationResult result = new OperationResult("add object communication error.");
		addObjectFromFile(USER_E_FILENAME, UserType.class, result);

		PrismObject<UserType> addedUser = repositoryService.getObject(UserType.class, USER_E_OID, result);
		assertNotNull(addedUser);
		List<ObjectReferenceType> accountRefs = addedUser.asObjectable().getAccountRef();
		assertEquals("Expected that user does not have account reference, but found " + accountRefs.size(),
				0, accountRefs.size());

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(USER_E_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, result);
//		modelService.modifyObject(UserType.class, USER_E_OID, delta.getModifications(), task, result);

		result.computeStatus();
		display("add object communication problem result: ", result);
		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
		
		PrismObject<UserType> userAferModifyOperation = repositoryService.getObject(UserType.class,
				USER_E_OID, result);
		assertNotNull(userAferModifyOperation);
		accountRefs = userAferModifyOperation.asObjectable().getAccountRef();
		assertEquals("Expected that user has one account reference, but found " + accountRefs.size(), 1,
				accountRefs.size());

		String accountOid = accountRefs.get(0).getOid();
		AccountShadowType faieldAccount = repositoryService.getObject(AccountShadowType.class, accountOid, result).asObjectable();
		assertNotNull(faieldAccount);
		displayJaxb("shadow from the repository: ", faieldAccount, AccountShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				FailedOperationTypeType.ADD, faieldAccount.getFailedOperationType());
		assertNotNull(faieldAccount.getResult());
		assertNotNull(faieldAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), faieldAccount.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "sn", "e");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "cn", "e");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "givenName", "e");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "uid", "e");
		
		

	}

	@SuppressWarnings("unchecked")
	@Test
	public void test021addModifyObjectCommunicationProblem() throws Exception {
		displayTestTile("test021 add modify object - communication problem");
		OperationResult result = new OperationResult("add object communication error.");

		PrismObject<UserType> userE = repositoryService.getObject(UserType.class, USER_E_OID, result);
		assertNotNull(userE);
		List<ObjectReferenceType> accountRefs = userE.asObjectable().getAccountRef();
		 assertEquals("Expected that user has 1 account reference, but found "
		 + accountRefs.size(),
		 1, accountRefs.size());

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(accountRefs.get(0).getOid(), delta.getModifications(), AccountShadowType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, result);
//		modelService.modifyObject(AccountShadowType.class, accountRefs.get(0).getOid(),
//				delta.getModifications(), task, result);

		String accountOid = accountRefs.get(0).getOid();
		AccountShadowType faieldAccount = repositoryService.getObject(AccountShadowType.class, accountOid,
				result).asObjectable();
		assertNotNull(faieldAccount);
		displayJaxb("shadow from the repository: ", faieldAccount, AccountShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				FailedOperationTypeType.ADD, faieldAccount.getFailedOperationType());
		assertNotNull(faieldAccount.getResult());
		assertNotNull(faieldAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), faieldAccount.getResourceRef().getOid());
		//
//		assertTrue(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifiers().isEmpty());
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "sn", "e");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "cn", "e");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "givenName", "Jackkk");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "uid", "e");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

	}

	@Test
	public void test022modifyObjectCommunicationProblem() throws Exception {

		displayTestTile("test022 modify object - communication problem");
		OperationResult parentResult = new OperationResult("modify object - communication problem");
		UserType userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, parentResult)
				.asObjectable();
		assertNotNull(userJack);
		assertEquals(1, userJack.getAccountRef().size());
		String accountRefOid = userJack.getAccountRef().get(0).getOid();

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(accountRefOid, delta.getModifications(), AccountShadowType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, parentResult);
//		modelService.modifyObject(AccountShadowType.class, accountRefOid, delta.getModifications(), task,
//				parentResult);

		AccountShadowType faieldAccount = repositoryService.getObject(AccountShadowType.class, accountRefOid,
				parentResult).asObjectable();
		assertNotNull(faieldAccount);
		displayJaxb("shadow from the repository: ", faieldAccount, AccountShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				FailedOperationTypeType.MODIFY, faieldAccount.getFailedOperationType());
		assertNotNull(faieldAccount.getResult());
		assertNotNull(faieldAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), faieldAccount.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertNotNull(faieldAccount.getObjectChange());

	}

	@Test
	public void test023deleteObjectCommunicationProblem() throws Exception {
		displayTestTile("test023 delete object - communication problem");
		OperationResult parentResult = new OperationResult("modify object - communication problem");
		UserType userJack = repositoryService.getObject(UserType.class, USER_DENIELS_OID, parentResult)
				.asObjectable();
		assertNotNull(userJack);
		assertEquals(1, userJack.getAccountRef().size());
		String accountRefOid = userJack.getAccountRef().get(0).getOid();

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_DELETE_ACCOUNT_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(USER_DENIELS_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, parentResult);
//		
//		modelService.modifyObject(UserType.class, USER_DENIELS_OID, delta.getModifications(), task,
//				parentResult);

		UserType userJackAftermodify = repositoryService.getObject(UserType.class, USER_DENIELS_OID,
				parentResult).asObjectable();
		assertNotNull(userJack);
//		 assertEquals(0, userJack.getAccountRef().size());

		ObjectDelta deleteDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, ACCOUNT_DENIELS_OID, prismContext);
		deltas = createDeltaCollection(deleteDelta);
		modelService.executeChanges(deltas, null, task, parentResult);
//		modelService.deleteObject(AccountShadowType.class, ACCOUNT_DENIELS_OID, task, parentResult);

		AccountShadowType faieldAccount = repositoryService.getObject(AccountShadowType.class,
				ACCOUNT_DENIELS_OID, parentResult).asObjectable();
		assertNotNull(faieldAccount);
		displayJaxb("shadow from the repository: ", faieldAccount, AccountShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				FailedOperationTypeType.DELETE, faieldAccount.getFailedOperationType());
		assertNotNull(faieldAccount.getResult());
		assertNotNull(faieldAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), faieldAccount.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		// assertNotNull(faieldAccount.getObjectChange());
	}

	@Test
	public void test024getAccountCommunicationProblem() throws Exception {
		displayTestTile("test024getAccountCommunicationProblem");
		OperationResult result = new OperationResult("test024 get account communication problem");
		AccountShadowType account = modelService.getObject(AccountShadowType.class, ACCOUNT_DENIELS_OID,
				null, null, result).asObjectable();
		assertNotNull("Get method returned null account.", account);
		assertNotNull("Fetch result was not set in the shadow.", account.getFetchResult());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void test025addObjectCommunicationProblemAlreadyExists() throws Exception{
		
		OperationResult parentResult = new OperationResult("Add account already exist unlinked.");
		
		openDJController.start();
	
		Entry entry = openDJController.addEntryFromLdifFile(LDIF_ELAINE_FILENAME);
		SearchResultEntry searchResult = openDJController.searchByUid("elaine");
		OpenDJController.assertAttribute(searchResult, "l", "Caribbean");
		OpenDJController.assertAttribute(searchResult, "givenName", "Elaine");
		OpenDJController.assertAttribute(searchResult, "sn", "Marley");
		OpenDJController.assertAttribute(searchResult, "cn", "Elaine Marley");
		OpenDJController.assertAttribute(searchResult, "mail", "governor.marley@deep.in.the.caribbean.com");
		OpenDJController.assertAttribute(searchResult, "employeeType", "governor");
		OpenDJController.assertAttribute(searchResult, "title", "Governor");
		String dn = searchResult.getDN().toString();
		assertEquals("DN attribute " + dn + " not equals", dn, "uid=elaine,ou=people,dc=example,dc=com");

		openDJController.stop();
		
		
		testAddUserToRepo("add user - test025 account already exists communication problem", USER_ELAINE_FILENAME,
				USER_ELAINE_OID);
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_ELAINE_OID, parentResult);
		assertNotNull(user);
		assertEquals(0, user.asObjectable().getAccountRef().size());

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_COMMUNICATION_PROBLEM_OPENDJ_FILENAME,
				ObjectModificationType.class);

		ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta<UserType> modifyDelta = ObjectDelta.createModifyDelta(USER_ELAINE_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		
		// WHEN
//		displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, parentResult);
		PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_ELAINE_OID, parentResult);
		assertNotNull(repoUser);
		assertEquals("user does not contain reference to shadow", 1, repoUser.asObjectable().getAccountRef().size());
		String shadowOid = repoUser.asObjectable().getAccountRef().get(0).getOid();
		PrismObject<AccountShadowType> shadow = modelService.getObject(AccountShadowType.class, shadowOid, null, task, parentResult);
		assertNotNull("Shadow must not be null", shadow);
		AccountShadowType shadowType = shadow.asObjectable();
		assertEquals("Failed operation type must not be null.", FailedOperationTypeType.ADD, shadowType.getFailedOperationType());
		assertNotNull("Result in the shadow must not be null", shadowType.getResult());
		assertNotNull("Resource ref in the shadow must not be null", shadowType.getResourceRef());
		assertEquals("Resource in the shadow not same as actual resource.", resourceTypeOpenDjrepo.getOid(), shadowType.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertAttribute(shadowType, resourceTypeOpenDjrepo, "sn", "Marley");
		assertAttribute(shadowType, resourceTypeOpenDjrepo, "cn", "Elaine Marley");
		assertAttribute(shadowType, resourceTypeOpenDjrepo, "givenName", "Elaine");
		assertAttribute(shadowType, resourceTypeOpenDjrepo, "uid", "elaine");
	
	}
	
	@Test
	public void test026modifyObjectTwoTimesCommunicationProblem() throws Exception{
		final String TEST_NAME = "test026modifyObjectTwoTimesCommunicationProblem";
        displayTestTile(this, TEST_NAME);
        
		OperationResult parentResult = new OperationResult(TEST_NAME);
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_JACK2_OID, parentResult);
		assertNotNull(user);
		UserType userType = user.asObjectable();
		assertNotNull(userType.getAccountRef());
		
		
//		PrismObjectDefinition accountDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
//		assertNotNull("Account definition must not be null.", accountDef);
		
//		PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(SchemaConstants.PATH_ACTIVATION, accountDef, true);
		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
//		modifications.add(delta);
		
		PropertyDelta fullNameDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_FULL_NAME), user.getDefinition(), new PolyString("jackNew2"));
		modifications.add(fullNameDelta);
		
		PrismPropertyValue enabledUserAction = new PrismPropertyValue(true, OriginType.USER_ACTION, null);
		PropertyDelta enabledDelta = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ENABLE, user.getDefinition());
		enabledDelta.addValueToAdd(enabledUserAction);
		modifications.add(enabledDelta);
//		delta = PropertyDelta.createModificationReplaceProperty(SchemaConstants.PATH_ACTIVATION_ENABLE, user.getDefinition(), true);
//		modifications.add(delta);
		
		ObjectDelta objectDelta = ObjectDelta.createModifyDelta(USER_JACK2_OID, modifications, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(objectDelta);
		
		
		Task task = taskManager.createTaskInstance();
		
		modelService.executeChanges(deltas, null, task, parentResult);
		parentResult.computeStatus();
		assertEquals("Expected that user has one account reference, but got " + userType.getAccountRef().size(), 1, userType.getAccountRef().size());	
		String shadowOid = userType.getAccountRef().get(0).getOid();
		PrismObject<AccountShadowType> account = modelService.getObject(AccountShadowType.class, shadowOid, null, task, parentResult);
		assertNotNull(account);
		AccountShadowType shadow = account.asObjectable();
		assertNotNull(shadow.getObjectChange());
		display("shadow after communication problem", shadow);
		
		
		Collection<PropertyDelta> newModifications = new ArrayList<PropertyDelta>();
		PropertyDelta fullNameDeltaNew = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_FULL_NAME), user.getDefinition(), new PolyString("jackNew2a"));
		newModifications.add(fullNameDeltaNew);
		
		
		PropertyDelta givenNameDeltaNew = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_GIVEN_NAME), user.getDefinition(), new PolyString("jackNew2a"));
		newModifications.add(givenNameDeltaNew);
		
		PrismPropertyValue enabledOutboundAction = new PrismPropertyValue(true, OriginType.USER_ACTION, null);
		PropertyDelta enabledDeltaNew = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ENABLE, user.getDefinition());
		enabledDeltaNew.addValueToAdd(enabledOutboundAction);
		newModifications.add(enabledDeltaNew);
		
		ObjectDelta newObjectDelta = ObjectDelta.createModifyDelta(USER_JACK2_OID, newModifications, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> newDeltas = createDeltaCollection(newObjectDelta);
		
		
		modelService.executeChanges(newDeltas, null, task, parentResult);
		
//		parentResult.computeStatus();
//		assertEquals("expected handled error in the result", OperationResultStatus.HANDLED_ERROR, parentResult.getStatus());
		
		
	}
	
	/**
	 * this test simulates situation, when someone tries to add account while
	 * resource is down and this account is created by next get call on this
	 * account
	 * 
	 * @throws Exception
	 */
	@Test
	public void test027getDiscoveryAddCommunicationProblem() throws Exception {
		displayTestTile("test027getDiscoveryAddCommunicationProblem");
		OperationResult result = new OperationResult("test027getDiscoveryAddCommunicationProblem");
		addObjectFromFile(USER_ANGELIKA_FILENAME, UserType.class, result);

		PrismObject<UserType> addedUser = repositoryService.getObject(UserType.class, USER_ANGELIKA_OID, result);
		assertNotNull(addedUser);
		List<ObjectReferenceType> accountRefs = addedUser.asObjectable().getAccountRef();
		assertEquals("Expected that user does not have account reference, but found " + accountRefs.size(),
				0, accountRefs.size());

		
		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(USER_ANGELIKA_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, result);
//		modelService.modifyObject(UserType.class, USER_E_OID, delta.getModifications(), task, result);

		result.computeStatus();
		display("add object communication problem result: ", result);
		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
		
		PrismObject<UserType> userAferModifyOperation = repositoryService.getObject(UserType.class,
				USER_ANGELIKA_OID, result);
		assertNotNull(userAferModifyOperation);
		accountRefs = userAferModifyOperation.asObjectable().getAccountRef();
		assertEquals("Expected that user has one account reference, but found " + accountRefs.size(), 1,
				accountRefs.size());

		String accountOid = accountRefs.get(0).getOid();
		AccountShadowType faieldAccount = repositoryService.getObject(AccountShadowType.class, accountOid, result).asObjectable();
		assertNotNull(faieldAccount);
		displayJaxb("shadow from the repository: ", faieldAccount, AccountShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				FailedOperationTypeType.ADD, faieldAccount.getFailedOperationType());
		assertNotNull("Failed angelica's account must have result.", faieldAccount.getResult());
		assertNotNull("Failed angelica's account must contain reference on the resource", faieldAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), faieldAccount.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "sn", "angelika");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "cn", "angelika");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "givenName", "angelika");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "uid", "angelika");
		
		//start openDJ
		openDJController.start();
		//and set the resource availability status to UP
		PropertyDelta resourceStatusDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
				ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
				resourceTypeOpenDjrepo.asPrismObject().getDefinition(), AvailabilityStatusType.UP);
		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
		modifications.add(resourceStatusDelta);
		repositoryService.modifyObject(ResourceType.class, resourceTypeOpenDjrepo.getOid(), modifications, result);
		//and then try to get account -> result is that the account will be created while getting it
		
		PrismObject<AccountShadowType> angelicaAcc = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
		assertNotNull(angelicaAcc);
		AccountShadowType angelicaAccount = angelicaAcc.asObjectable();
		displayJaxb("Shadow after discovery: ", angelicaAccount, AccountShadowType.COMPLEX_TYPE);
		assertNull("Angelica's account after discovery must not have failed opertion.", angelicaAccount.getFailedOperationType());
		assertNull("Angelica's account after discovery must not have result.", angelicaAccount.getResult());
		assertNotNull("Angelica's account must contain reference on the resource", angelicaAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), angelicaAccount.getResourceRef().getOid());
//		assertNotNull("Identifier in the angelica's account after discovery must not be null.",ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertAttribute(angelicaAccount, resourceTypeOpenDjrepo, "sn", "angelika");
		assertAttribute(angelicaAccount, resourceTypeOpenDjrepo, "cn", "angelika");
		assertAttribute(angelicaAccount, resourceTypeOpenDjrepo, "givenName", "angelika");
		assertAttribute(angelicaAccount, resourceTypeOpenDjrepo, "uid", "angelika");
		
	}
	
	@Test
	public void test028getDiscoveryModifyCommunicationProblem() throws Exception{
		displayTestTile("test028getDiscoveryModifyCommunicationProblem");
		OperationResult parentResult = new OperationResult("test028getDiscoveryModifyCommunicationProblem");
		
		//prepare user 
		addObjectFromFile(USER_ALICE_FILENAME, UserType.class, parentResult);

		PrismObject<UserType> addedUser = repositoryService.getObject(UserType.class, USER_ALICE_OID, parentResult);
		assertNotNull(addedUser);
		List<ObjectReferenceType> accountRefs = addedUser.asObjectable().getAccountRef();
		assertEquals("Expected that user does not have account reference, but found " + accountRefs.size(),
				0, accountRefs.size());

		//and add account to the user while resource is UP
		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(USER_ALICE_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, parentResult);
		
		//then stop openDJ
		openDJController.stop();
		UserType userJack = repositoryService.getObject(UserType.class, USER_ALICE_OID, parentResult)
				.asObjectable();
		assertNotNull(userJack);
		assertEquals(1, userJack.getAccountRef().size());
		String accountRefOid = userJack.getAccountRef().get(0).getOid();

		//and make some modifications to the account while resource is DOWN
		objectChange = unmarshallJaxbFromFile(
				REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		delta = DeltaConvertor.createObjectDelta(objectChange, AccountShadowType.class, PrismTestUtil.getPrismContext());

		modifyDelta = ObjectDelta.createModifyDelta(accountRefOid, delta.getModifications(), AccountShadowType.class, prismContext);
		deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, parentResult);
//		modelService.modifyObject(AccountShadowType.class, accountRefOid, delta.getModifications(), task,
//				parentResult);

		//check the state after execution
		AccountShadowType faieldAccount = repositoryService.getObject(AccountShadowType.class, accountRefOid,
				parentResult).asObjectable();
		assertNotNull(faieldAccount);
		displayJaxb("shadow from the repository: ", faieldAccount, AccountShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				FailedOperationTypeType.MODIFY, faieldAccount.getFailedOperationType());
		assertNotNull(faieldAccount.getResult());
		assertNotNull(faieldAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), faieldAccount.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertNotNull(faieldAccount.getObjectChange());

		//start openDJ
		openDJController.start();
		//and set the resource availability status to UP
		PropertyDelta resourceStatusDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
				ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
				resourceTypeOpenDjrepo.asPrismObject().getDefinition(), AvailabilityStatusType.UP);
		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
		modifications.add(resourceStatusDelta);
		repositoryService.modifyObject(ResourceType.class, resourceTypeOpenDjrepo.getOid(), modifications, parentResult);
		
		//and then try to get account -> result is that the modifications will be applied to the account
		PrismObject<AccountShadowType> aliceAcc = modelService.getObject(AccountShadowType.class, accountRefOid, null, task, parentResult);
		assertNotNull(aliceAcc);
		AccountShadowType aliceAccount = aliceAcc.asObjectable();
		displayJaxb("Shadow after discovery: ", aliceAccount, AccountShadowType.COMPLEX_TYPE);
		assertNull("Alice's account after discovery must not have failed opertion.", aliceAccount.getFailedOperationType());
		assertNull("Alice's account after discovery must not have result.", aliceAccount.getResult());
		assertNull("Object changes in alices's account after discovery must be null", aliceAccount.getObjectChange());
		assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "sn", "alice");
		assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "cn", "alice");
		assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "givenName", "Jackkk");
		assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "uid", "alice");
		assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

		//and finally stop openDJ
		openDJController.stop();
	}
	
	/**
	 * this test simulates situation, when someone tries to add account while
	 * resource is down and this account is created by next get call on this
	 * account
	 * 
	 * @throws Exception
	 */
	@Test
	public void test029modifyDiscoveryAddCommunicationProblem() throws Exception {
		displayTestTile("test029modifyDiscoveryAddCommunicationProblem");
		OperationResult result = new OperationResult("test029modifyDiscoveryAddCommunicationProblem");
		addObjectFromFile(USER_BOB_NO_FAMILY_NAME_FILENAME, UserType.class, result);

		PrismObject<UserType> addedUser = repositoryService.getObject(UserType.class, USER_BOB_NO_FAMILY_NAME_OID, result);
		assertNotNull(addedUser);
		List<ObjectReferenceType> accountRefs = addedUser.asObjectable().getAccountRef();
		assertEquals("Expected that user does not have account reference, but found " + accountRefs.size(),
				0, accountRefs.size());

		
		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM, ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		Task task = taskManager.createTaskInstance();
		
		ObjectDelta modifyDelta = ObjectDelta.createModifyDelta(USER_BOB_NO_FAMILY_NAME_OID, delta.getModifications(), UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltaCollection(modifyDelta);
		modelService.executeChanges(deltas, null, task, result);
//		modelService.modifyObject(UserType.class, USER_E_OID, delta.getModifications(), task, result);

		result.computeStatus();
		display("add object communication problem result: ", result);
		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
		
		PrismObject<UserType> userAferModifyOperation = repositoryService.getObject(UserType.class,
				USER_BOB_NO_FAMILY_NAME_OID, result);
		assertNotNull(userAferModifyOperation);
		accountRefs = userAferModifyOperation.asObjectable().getAccountRef();
		assertEquals("Expected that user has one account reference, but found " + accountRefs.size(), 1,
				accountRefs.size());

		String accountOid = accountRefs.get(0).getOid();
		AccountShadowType faieldAccount = repositoryService.getObject(AccountShadowType.class, accountOid, result).asObjectable();
		assertNotNull(faieldAccount);
		displayJaxb("shadow from the repository: ", faieldAccount, AccountShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				FailedOperationTypeType.ADD, faieldAccount.getFailedOperationType());
		assertNotNull("Failed bob's account must have result.", faieldAccount.getResult());
		assertNotNull("Failed bob's account must contain reference on the resource", faieldAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), faieldAccount.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
//		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "sn", "angelika");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "cn", "Bob Dylan");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "givenName", "Bob");
		assertAttribute(faieldAccount, resourceTypeOpenDjrepo, "uid", "bob");
		
		//start openDJ
		openDJController.start();
		//and set the resource availability status to UP
		PropertyDelta resourceStatusDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
				ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
				resourceTypeOpenDjrepo.asPrismObject().getDefinition(), AvailabilityStatusType.UP);
		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
		modifications.add(resourceStatusDelta);
		repositoryService.modifyObject(ResourceType.class, resourceTypeOpenDjrepo.getOid(), modifications, result);
		//and then try to get account -> result is that the account will be created while getting it
		
		try{
		modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
		fail("expected schema exception was not thrown");
//		} catch (SchemaException ex){
//			LOGGER.info("schema exeption while trying to re-add account after communication problem without family name..this is expected.");
//			result.muteLastSubresultError();
//			result.recordSuccess();
		
		
		//TODO: is this really expected? shouldn't it be a schema exception???
		} catch (SystemException ex){
			LOGGER.info("system exeption while trying to re-add account after communication problem without family name..this is expected.");
			result.muteLastSubresultError();
			result.recordSuccess();
//			LOGGER.info("expected schema exeption while got: {}", ex);
		}
		
		OperationResult modifyFamilyNameResult = new OperationResult("execute changes -> modify user's family name");
		Collection<? extends ItemDelta> familyNameDelta = PropertyDelta.createModificationReplacePropertyCollection(UserType.F_FAMILY_NAME, addedUser.getDefinition(), new PolyString("Dylan"));
		ObjectDelta familyNameD = ObjectDelta.createModifyDelta(USER_BOB_NO_FAMILY_NAME_OID, familyNameDelta, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> modifyFamilyNameDelta = createDeltaCollection(familyNameD);
		modelService.executeChanges(modifyFamilyNameDelta, null, task, modifyFamilyNameResult);
		
		modifyFamilyNameResult.computeStatus();
		display("add object communication problem result: ", modifyFamilyNameResult);
		assertEquals("Expected handled error but got: " + modifyFamilyNameResult.getStatus(), OperationResultStatus.SUCCESS, modifyFamilyNameResult.getStatus());
		
		PrismObject<AccountShadowType> bobRepoAcc = repositoryService.getObject(AccountShadowType.class, accountOid, modifyFamilyNameResult);
		assertNotNull(bobRepoAcc);
		AccountShadowType bobRepoAccount = bobRepoAcc.asObjectable();
		displayJaxb("Shadow after discovery: ", bobRepoAccount, AccountShadowType.COMPLEX_TYPE);
		assertNull("Bob's account after discovery must not have failed opertion.", bobRepoAccount.getFailedOperationType());
		assertNull("Bob's account after discovery must not have result.", bobRepoAccount.getResult());
		assertNotNull("Bob's account must contain reference on the resource", bobRepoAccount.getResourceRef());
		
		PrismObject<AccountShadowType> bobResourceAcc = modelService.getObject(AccountShadowType.class, accountOid, null, task, modifyFamilyNameResult);
		assertNotNull(bobResourceAcc);
		AccountShadowType bobResourceAccount = bobResourceAcc.asObjectable();
//		displayJaxb("Shadow after discovery: ", angelicaAccount, AccountShadowType.COMPLEX_TYPE);
//		assertNull("Angelica's account after discovery must not have failed opertion.", angelicaAccount.getFailedOperationType());
//		assertNull("Angelica's account after discovery must not have result.", angelicaAccount.getResult());
//		assertNotNull("Angelica's account must contain reference on the resource", angelicaAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), bobResourceAccount.getResourceRef().getOid());
//		assertNotNull("Identifier in the angelica's account after discovery must not be null.",ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertAttribute(bobResourceAccount, resourceTypeOpenDjrepo, "sn", "Dylan");
		assertAttribute(bobResourceAccount, resourceTypeOpenDjrepo, "cn", "Bob Dylan");
		assertAttribute(bobResourceAccount, resourceTypeOpenDjrepo, "givenName", "Bob");
		assertAttribute(bobResourceAccount, resourceTypeOpenDjrepo, "uid", "bob");
		
		openDJController.stop();
	}

	/**
	 * Adding a user (morgan) that has an OpenDJ assignment. But the equivalent account already exists on
	 * OpenDJ. The account should be linked.
	 */
	// DISABLED because MID-1056
	@Test(enabled=false)
    public void test100AddUserMorganWithAssignment() throws Exception {
		final String TEST_NAME = "test100AddUserMorganWithAssignment";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        
        openDJController.start();
		assertTrue(EmbeddedUtils.isRunning());
		
        Task task = taskManager.createTaskInstance(ConsistencyTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Entry entry = openDJController.addEntryFromLdifFile(LDIF_MORGAN_FILENAME);
        display("Entry from LDIF", entry);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_MORGAN_FILENAME));
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
        IntegrationTestTools.assertSuccess("executeChanges result", result);
        
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getAccountRef().size());
        ObjectReferenceType accountRefType = userMorganType.getAccountRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<AccountShadowType> accountShadow = repositoryService.getObject(AccountShadowType.class, accountOid, result);
        assertShadowRepo(accountShadow, accountOid, "morgan", resourceTypeOpenDjrepo);
        
        // Check account
        PrismObject<AccountShadowType> accountModel = modelService.getObject(AccountShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "morgan", resourceTypeOpenDjrepo);
        
        // TODO: check OpenDJ Account        
	}
	
	
	// This should run last. It starts a task that may interfere with other tests
	@Test
	public void test800Reconciliation() throws Exception {
		final String TEST_NAME = "test800Reconciliation";
        displayTestTile(this, TEST_NAME);

		final OperationResult result = new OperationResult(ConsistencyTest.class.getName() + "." + TEST_NAME);

		// TODO: remove this if the previous test is enabled
		openDJController.start();
		
		// precondition
		assertTrue(EmbeddedUtils.isRunning());
		UserType userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, result).asObjectable();
		display("Jack before", userJack);
		
		// WHEN
		addObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILENAME, TaskType.class, result);
		waitForTaskNextRun(TASK_OPENDJ_RECONCILIATION_OID, false, 30000);

		// THEN
		
		// STOP the task. We don't need it any more and we don't want to give it
		// a chance to run more than once
		taskManager.deleteTask(TASK_OPENDJ_RECONCILIATION_OID, result);

		// check if the account was added after reconciliation
		UserType userE = repositoryService.getObject(UserType.class, USER_E_OID, result).asObjectable();
		assertNotNull(userE);
		List<ObjectReferenceType> accountRefs = userE.getAccountRef();
		assertFalse(accountRefs.isEmpty());
		assertEquals(1, accountRefs.size());

		AccountShadowType addedAccount = modelService.getObject(AccountShadowType.class,
				accountRefs.get(0).getOid(), null, null, result).asObjectable();
		assertNotNull(addedAccount);
		displayJaxb("shadow from the repository: ", addedAccount, AccountShadowType.COMPLEX_TYPE);
		assertNull("Expected that FailedOperationType in e's account is null, but it has value "+ addedAccount.getFailedOperationType(),
				addedAccount.getFailedOperationType());
		assertNull(addedAccount.getResult());
		assertNotNull(addedAccount.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), addedAccount.getResourceRef().getOid());
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		ResourceAttributeContainer attributeContainer = ResourceObjectShadowUtil
				.getAttributesContainer(addedAccount);
		Collection<ResourceAttribute<?>> identifiers = attributeContainer.getIdentifiers();
		assertNotNull(identifiers);
		assertFalse(identifiers.isEmpty());
		assertEquals(1, identifiers.size());
		assertAttribute(addedAccount, resourceTypeOpenDjrepo, "sn", "e");
		assertAttribute(addedAccount, resourceTypeOpenDjrepo, "cn", "e");
		assertAttribute(addedAccount, resourceTypeOpenDjrepo, "givenName", "Jackkk");
		assertAttribute(addedAccount, resourceTypeOpenDjrepo, "uid", "e");
		assertAttribute(addedAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

		// check if the account was modified during reconciliation process
		userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, result).asObjectable();
		display("Jack after", userJack);
		assertNotNull(userJack);
		assertEquals("Wrong number of jack's accounts", 1, userJack.getAccountRef().size());
		String accountRefOid = userJack.getAccountRef().get(0).getOid();
		AccountShadowType modifiedAccount = modelService.getObject(AccountShadowType.class, accountRefOid,
				null, null, result).asObjectable();
		assertNotNull(modifiedAccount);
		displayJaxb("shadow from the repository: ", modifiedAccount, AccountShadowType.COMPLEX_TYPE);
		assertNull("Expected that FailedOperationType is null, but isn't",
				modifiedAccount.getFailedOperationType());
		assertNull("result in the modified account after reconciliation must be null.", modifiedAccount.getResult());
		assertNull("Expected that modification in shadow are null, but got: "+ modifiedAccount.getObjectChange(), modifiedAccount.getObjectChange());
		
		assertAttribute(modifiedAccount, resourceTypeOpenDjrepo, "givenName", "Jackkk");
		assertAttribute(modifiedAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

		
		// check if the account was deleted during the reconciliation process
		try {
			modelService.getObject(AccountShadowType.class, ACCOUNT_DENIELS_OID, null, null, result);
			fail("Expected ObjectNotFoundException but haven't got one.");
		} catch (Exception ex) {
			if (!(ex instanceof ObjectNotFoundException)) {
				fail("Expected ObjectNotFoundException but got " + ex);
			}

		}
		
		UserType userElaine = repositoryService.getObject(UserType.class, USER_ELAINE_OID, result).asObjectable();
		assertNotNull(userElaine);
		assertEquals("elaine must have only one reference ont he account", 1, userElaine.getAccountRef().size());
		String accountElaineRefOid = userElaine.getAccountRef().get(0).getOid();
		AccountShadowType elaineAccount = modelService.getObject(AccountShadowType.class, accountElaineRefOid,
				null, null, result).asObjectable();
		assertNotNull(elaineAccount);
		displayJaxb("shadow from the repository: ", elaineAccount, AccountShadowType.COMPLEX_TYPE);
		assertNull("Expected that FailedOperationType in elaine's account is null, but isn't",
				elaineAccount.getFailedOperationType());
		assertNull("result in the modified account after reconciliation must be null.", elaineAccount.getResult());
		assertIcfsNameAttribute(elaineAccount, "uid=elaine,ou=people,dc=example,dc=com");
		assertAttribute(modifiedAccount, resourceTypeOpenDjrepo, "givenName", "Jackkk");
		assertAttribute(modifiedAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

		UserType userJack2 = repositoryService.getObject(UserType.class, USER_JACK2_OID, result).asObjectable();
		assertNotNull(userJack2);
		assertEquals(1, userJack2.getAccountRef().size());
		String jack2Oid = userJack2.getAccountRef().get(0).getOid();
		AccountShadowType jack2Shadow = provisioningService.getObject(AccountShadowType.class, jack2Oid, null, result).asObjectable();
		assertNotNull(jack2Shadow);
		assertNull("ObjectChnage in jack2 account must be null", jack2Shadow.getObjectChange());
		assertNull("result in the jack2 shadow must be null", jack2Shadow.getResult());
		assertNull("failed operation in jack2 shadow must be null", jack2Shadow.getFailedOperationType());
		assertAttribute(jack2Shadow, resourceTypeOpenDjrepo, "givenName", "jackNew2a");
		assertAttribute(jack2Shadow, resourceTypeOpenDjrepo, "cn", "jackNew2a");

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

	private String checkRepoShadow(PrismObject<AccountShadowType> repoShadow) {
		AccountShadowType repoShadowType = repoShadow.asObjectable();
		String uid = null;
		boolean hasOthers = false;
		List<Object> xmlAttributes = repoShadowType.getAttributes().getAny();
		for (Object element : xmlAttributes) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
				if (uid != null) {
					AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
				} else {
					uid = ((Element) element).getTextContent();
				}
			} else if (ConnectorFactoryIcfImpl.ICFS_NAME.equals(JAXBUtil.getElementQName(element))) {
				// This is OK
			} else {
				hasOthers = true;
			}
		}

		assertFalse("Shadow " + repoShadow + " has unexpected elements", hasOthers);
		assertNotNull(uid);

		return uid;
	}

}
