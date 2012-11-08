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

import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttributeNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayJaxb;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayWhen;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayThen;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
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
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountSynchronizationSettingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
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

/**
 * Sanity test suite.
 * <p/>
 * It tests the very basic representative test cases. It does not try to be
 * complete. It rather should be quick to execute and pass through the most
 * representative cases. It should test all the system components except for
 * GUI. Therefore the test cases are selected to pass through most of the
 * components.
 * <p/>
 * It is using mock BaseX repository and embedded OpenDJ instance as a testing
 * resource. The BaseX repository is instantiated from the Spring context in the
 * same way as all other components. OpenDJ instance is started explicitly using
 * BeforeClass method. Appropriate resource definition to reach the OpenDJ
 * instance is provided in the test data and is inserted in the repository as
 * part of test initialization.
 * 
 * @author Katarina Valalikova
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-consistency-test.xml", "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml", "classpath:application-context-repository.xml",
		"classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ConsistencyTest extends AbstractIntegrationTest {

	private static final String OPENDJ_PEOPLE_SUFFIX = "ou=people,dc=example,dc=com";

	private static final String SYSTEM_CONFIGURATION_FILENAME = "src/test/resources/repo/system-configuration.xml";
	private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

	private static final String RESOURCE_OPENDJ_FILENAME = "src/test/resources/repo/resource-opendj.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String CONNECTOR_LDAP_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.forgerock.openicf.connectors.ldap.ldap/org.identityconnectors.ldap.LdapConnector";
	private static final String CONNECTOR_DBTABLE_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.forgerock.openicf.connectors.db.databasetable/org.identityconnectors.databasetable.DatabaseTableConnector";

	private static final String TASK_OPENDJ_SYNC_FILENAME = "src/test/resources/repo/task-opendj-sync.xml";
	private static final String TASK_OPENDJ_SYNC_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";

	private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = "src/test/resources/repo/sample-configuration-object.xml";
	private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";

	private static final String USER_TEMPLATE_FILENAME = "src/test/resources/repo/user-template.xml";
	private static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	private static final String USER_ADMINISTRATOR_FILENAME = "src/test/resources/repo/user-administrator.xml";
	private static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

	private static final String USER_JACK_FILENAME = "src/test/resources/repo/user-jack.xml";
	// private static final File USER_JACK_FILE = new File(USER_JACK_FILENAME);
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

	private static final String USER_DENIELS_FILENAME = "src/test/resources/repo/user-deniels.xml";
	// private static final File USER_JACK_FILE = new File(USER_JACK_FILENAME);
	private static final String USER_DENIELS_OID = "c0c010c0-d34d-b33f-f00d-222111111111";

	private static final String USER_JACK2_FILENAME = "src/test/resources/repo/user-jack2.xml";
	private static final String USER_JACK2_OID = "c0c010c0-d34d-b33f-f00d-111111114444";

	private static final String USER_WILL_FILENAME = "src/test/resources/repo/user-will.xml";
	private static final String USER_WILL_OID = "c0c010c0-d34d-b33f-f00d-111111115555";

	private static final String USER_JACK_LDAP_UID = "jackie";
	private static final String USER_JACK_LDAP_DN = "uid=" + USER_JACK_LDAP_UID + "," + OPENDJ_PEOPLE_SUFFIX;

	private static final String USER_GUYBRUSH_FILENAME = "src/test/resources/repo/user-guybrush.xml";
	private static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111222";

	private static final String USER_GUYBRUSH_NOT_FOUND_FILENAME = "src/test/resources/repo/user-guybrush-modify-not-found.xml";
	private static final String USER_GUYBRUSH_NOT_FOUND_OID = "c0c010c0-d34d-b33f-f00d-111111111333";
	
	private static final String USER_HECTOR_NOT_FOUND_FILENAME = "src/test/resources/repo/user-hector.xml";
	private static final String USER_HECTOR_NOT_FOUND_OID = "c0c010c0-d34d-b33f-f00d-111111222333";	

	private static final String USER_E_FILENAME = "src/test/resources/repo/user-e.xml";
	private static final String USER_E_OID = "c0c010c0-d34d-b33f-f00d-111111111100";

	private static final String ACCOUNT_GUYBRUSH_FILENAME = "src/test/resources/repo/account-guybrush.xml";
	private static final String ACCOUNT_GUYBRUSH_OID = "a0c010c0-d34d-b33f-f00d-111111111222";
	
	private static final String ACCOUNT_HECTOR_FILENAME = "src/test/resources/repo/account-hector-not-found.xml";
	private static final String ACCOUNT_HECTOR_OID = "a0c010c0-d34d-b33f-f00d-111111222333";

	private static final String ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILENAME = "src/test/resources/repo/account-guybrush-not-found.xml";
	private static final String ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID = "a0c010c0-d34d-b33f-f00d-111111111333";

	private static final String ACCOUNT_DENIELS_FILENAME = "src/test/resources/repo/account-deniels.xml";
	private static final String ACCOUNT_DENIELS_OID = "a0c010c0-d34d-b33f-f00d-111111111555";

	private static final String ROLE_CAPTAIN_FILENAME = "src/test/resources/repo/role-captain.xml";
	private static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-987987cccccc";

	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_DENIELS_FILENAME = "src/test/resources/request/user-modify-add-account-deniels.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM = "src/test/resources/request/user-modify-add-account-communication-problem.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXIST_LINKED_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account-already-exist-linked.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXIST_UNLINKED_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account-already-exist-unlinked.xml";
	private static final String REQUEST_USER_MODIFY_DELETE_ACCOUNT = "src/test/resources/request/user-modify-delete-account.xml";
	private static final String REQUEST_USER_MODIFY_DELETE_ACCOUNT_COMMUNICATION_PROBLEM = "src/test/resources/request/user-modify-delete-account-communication-problem.xml";
	private static final String REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT = "src/test/resources/request/account-guybrush-modify-attributes.xml";
	private static final String REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM = "src/test/resources/request/account-modify-attrs-communication-problem.xml";
	private static final String REQUEST_ADD_ACCOUNT_JACKIE = "src/test/resources/request/add-account-jack.xml";

	private static final String TASK_OPENDJ_RECONCILIATION_FILENAME = "src/test/resources/repo/task-opendj-reconciliation.xml";
	private static final String TASK_OPENDJ_RECONCILIATION_OID = "91919191-76e0-59e2-86d6-3d4f02d30000";

	private static final String LDIF_WILL_FILENAME = "src/test/resources/request/will.ldif";

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

	/**
	 * The instance of ModelService. This is the interface that we will test.
	 */
	@Autowired(required = true)
	private ModelPortType modelWeb;
	@Autowired(required = true)
	private ModelService modelService;
	@Autowired(required = true)
	private ProvisioningService provisioningService;
	@Autowired(required = true)
	private PrismContext prismContext;
	@Autowired(required = true)
	private TaskManager taskManager;
	
	
	public ConsistencyTest() throws JAXBException {
		super();
		// TODO: fix this
		// IntegrationTestTools.checkResults = false;
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
	public void initSystem(OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
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

		assertCache();

		OperationResult result = new OperationResult(ConsistencyTest.class.getName() + ".test000Integrity");

		// Check if OpenDJ resource was imported correctly

		PrismObject<ResourceType> openDjResource = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, result);
		display("Imported OpenDJ resource (repository)", openDjResource);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());
		assertCache();

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

		assertCache();

		// WHEN
		OperationResultType result = modelWeb.testResource(RESOURCE_OPENDJ_OID);

		// THEN

		assertCache();

		displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

		assertSuccess("testResource has failed", result);

		OperationResult opResult = new OperationResult(ConsistencyTest.class.getName()
				+ ".test001TestConnectionOpenDJ");

		PrismObject<ResourceType> resourceOpenDjRepo = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, opResult);
		resourceTypeOpenDjrepo = resourceOpenDjRepo.asObjectable();

		assertCache();
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
		assertCache();

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

		assertCache();
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
		assertCache();

		// IMPORTANT! SWITCHING OFF ASSIGNMENT ENFORCEMENT HERE!
		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
		syncSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.FULL);
		applySyncSettings(syncSettings);

		assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.FULL);

		ObjectModificationType objectChange = unmarshallJaxbFromFile(modifyUserRequest,
				ObjectModificationType.class);

		// WHEN
		OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

		// THEN
		assertCache();
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

		String oid = provisioningService.addObject(jackeAccount.asPrismObject(), null, parentResult);
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

		assertCache();

		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		Holder<ObjectType> objectHolder = new Holder<ObjectType>();

		// WHEN
		PropertyReferenceListType resolve = new PropertyReferenceListType();
//		List<ObjectOperationOptions> options = new ArrayList<ObjectOperationOptions>();
		OperationOptionsType oot = new OperationOptionsType();
		modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj, oot,
				objectHolder, resultHolder);

		// THEN
		assertCache();
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

		provisioningService.addObject(shadow.asPrismObject(), null, secondResult);

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
				REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXIST_LINKED_OPENDJ_FILENAME,
				ObjectModificationType.class);

		ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, UserType.class,
				PrismTestUtil.getPrismContext());

		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
		syncSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.FULL);
		applySyncSettings(syncSettings);

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

		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
		syncSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.FULL);
		applySyncSettings(syncSettings);

		ObjectModificationType objectChange = unmarshallJaxbFromFile(
				REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXIST_UNLINKED_OPENDJ_FILENAME,
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
//		modelService.modifyObject(UserType.class, USER_GUYBRUSH_OID, delta.getModifications(), task,
//				parentResult);

		ObjectDelta deleteDelta = ObjectDelta.createDeleteDelta(AccountShadowType.class, ACCOUNT_GUYBRUSH_OID, prismContext);
		deltas = createDeltaCollection(deleteDelta);
		modelService.executeChanges(deltas, null, task, parentResult);
//		modelService.deleteObject(AccountShadowType.class, ACCOUNT_GUYBRUSH_OID, task, parentResult);

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

		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
		syncSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.FULL);
		applySyncSettings(syncSettings);

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

//		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
//		syncSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.FULL);
//		applySyncSettings(syncSettings);

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

		PrismObject<UserType> userAferModifyOperation = repositoryService.getObject(UserType.class,
				USER_E_OID, result);
		assertNotNull(userAferModifyOperation);
		accountRefs = userAferModifyOperation.asObjectable().getAccountRef();
		assertEquals("Expected that user has one account reference, but found " + accountRefs.size(), 1,
				accountRefs.size());

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

	@Test
	public void test025reconciliation() throws Exception {

		displayTestTile("test025 reconciliation");

		final OperationResult result = new OperationResult("reconciliation");

		// start openDJ first
		openDJController.start();
		assertTrue(EmbeddedUtils.isRunning());

		addObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILENAME, TaskType.class, result);

		// We need to wait for a sync interval, so the task scanner has a chance
		// to pick up this
		// task

		waitFor("Waiting for task to finish first run", new Checker() {
			public boolean check() throws ObjectNotFoundException, SchemaException {
				Task task = taskManager.getTask(TASK_OPENDJ_RECONCILIATION_OID, result);
				display("Task while waiting for task manager to pick up the task", task);
				// wait until the task is finished
				return task.getLastRunFinishTimestamp() != null;
				// if (TaskExclusivityStatus.CLAIMED ==
				// task.getExclusivityStatus()) { we cannot check exclusivity
				// status for now
				// // wait until the first run is finished
				// if (task.getLastRunFinishTimestamp() == null) {
				// return false;
				// }
				// return true;
				// }
				// return false;
			}

			@Override
			public void timeout() {
				// No reaction, the test will fail right after return from this
			}
		}, 180000);

		Task task = taskManager.getTask(TASK_OPENDJ_RECONCILIATION_OID, result);
		result.computeStatus();
		display("getTask result", result);
		assertSuccess("getTask has failed", result);
		AssertJUnit.assertNotNull(task);
		display("Task after pickup", task);

		PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, TASK_OPENDJ_RECONCILIATION_OID,
				result);
		display("Task after pickup in the repository", o.asObjectable());

		// .. it should be running
//		AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

		// .. and claimed
		// AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED,
		// task.getExclusivityStatus());

		// .. and last run should not be zero
		assertNotNull("Null last run start in recon task", task.getLastRunStartTimestamp());
		AssertJUnit.assertFalse("Zero last run start in recon task", task.getLastRunStartTimestamp()
				.longValue() == 0);
		assertNotNull("Null last run finish in recon task", task.getLastRunFinishTimestamp());
		AssertJUnit.assertFalse("Zero last run finish in recon task", task.getLastRunFinishTimestamp()
				.longValue() == 0);

		// The progress should be 0, as there were no changes yet
		AssertJUnit.assertEquals(0, task.getProgress());

		// Test for presence of a result. It should be there and it should
		// indicate success
		OperationResult taskResult = task.getResult();
		AssertJUnit.assertNotNull(taskResult);

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
		assertNull("Expected that FailedOperationType is null, but it has value "+ addedAccount.getFailedOperationType(),
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
		UserType userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, result).asObjectable();
		assertNotNull(userJack);
		assertEquals(1, userJack.getAccountRef().size());
		String accountRefOid = userJack.getAccountRef().get(0).getOid();
		AccountShadowType modifiedAccount = modelService.getObject(AccountShadowType.class, accountRefOid,
				null, null, result).asObjectable();
		assertNotNull(modifiedAccount);
		displayJaxb("shadow from the repository: ", modifiedAccount, AccountShadowType.COMPLEX_TYPE);
		assertNull("Expected that FailedOperationType is null, but isn't",
				modifiedAccount.getFailedOperationType());
		assertNull(addedAccount.getResult());

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

	private AccountShadowType searchAccountByOid(final String accountOid) throws Exception {
		OperationResultType resultType = new OperationResultType();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
		Holder<ObjectType> accountHolder = new Holder<ObjectType>();
		modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountOid,
				null, accountHolder, resultHolder);
		ObjectType object = accountHolder.value;
		assertSuccess("searchObjects has failed", resultHolder.value);
		assertNotNull("Account is null", object);

		if (!(object instanceof AccountShadowType)) {
			fail("Object is not account.");
		}
		AccountShadowType account = (AccountShadowType) object;
		assertEquals(accountOid, account.getOid());

		return account;
	}

	private UserType searchUserByName(String name) throws Exception {
		Document doc = DOMUtil.getDocument();
		Element nameElement = doc.createElementNS(SchemaConstants.C_NAME.getNamespaceURI(),
				SchemaConstants.C_NAME.getLocalPart());
		nameElement.setTextContent(name);
		Element filter = QueryUtil.createEqualFilter(doc, null, nameElement);

		QueryType query = new QueryType();
		query.setFilter(filter);
		OperationResultType resultType = new OperationResultType();
		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
		Holder<ObjectListType> listHolder = new Holder<ObjectListType>();
		assertCache();

		modelWeb.searchObjects(ObjectTypes.USER.getObjectTypeUri(), query, null, listHolder, resultHolder);

		assertCache();
		ObjectListType objects = listHolder.value;
		assertSuccess("searchObjects has failed", resultHolder.value);
		AssertJUnit.assertEquals("User not found (or found too many)", 1, objects.getObject().size());
		UserType user = (UserType) objects.getObject().get(0);

		AssertJUnit.assertEquals(user.getName(), name);

		return user;
	}

	private void importObjectFromFile(String filename, OperationResult parentResult)
			throws FileNotFoundException {
		OperationResult result = parentResult.createSubresult(ConsistencyTest.class.getName()
				+ ".importObjectFromFile");
		result.addParam("file", filename);
		LOGGER.trace("importObjectFromFile: {}", filename);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(filename);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);
		result.computeStatus();
	}

	private void assertCache() {
		if (RepositoryCache.exists()) {
			AssertJUnit.fail("Cache exists! " + RepositoryCache.dump());
		}
	}

	private void applySyncSettings(AccountSynchronizationSettingsType syncSettings)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObjectDefinition<SystemConfigurationType> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class);

		Collection<? extends ItemDelta> modifications = PropertyDelta
				.createModificationReplacePropertyCollection(
						SchemaConstants.C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS,
						objectDefinition, syncSettings);

		OperationResult result = new OperationResult("Aplying sync settings");

		repositoryService.modifyObject(SystemConfigurationType.class,
				SystemObjectsType.SYSTEM_CONFIGURATION.value(), modifications, result);
		display("Aplying sync settings result", result);
		result.computeStatus();
		assertSuccess("Aplying sync settings failed (result)", result);
	}

	private void assertSyncSettingsAssignmentPolicyEnforcement(
			AssignmentPolicyEnforcementType assignmentPolicy) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("Asserting sync settings");
		PrismObject<SystemConfigurationType> systemConfigurationType = repositoryService.getObject(
				SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);
		result.computeStatus();
		assertSuccess("Asserting sync settings failed (result)", result);
		AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfigurationType
				.asObjectable().getGlobalAccountSynchronizationSettings();
		assertNotNull("globalAccountSynchronizationSettings is null", globalAccountSynchronizationSettings);
		AssignmentPolicyEnforcementType assignmentPolicyEnforcement = globalAccountSynchronizationSettings
				.getAssignmentPolicyEnforcement();
		assertNotNull("assignmentPolicyEnforcement is null", assignmentPolicyEnforcement);
		assertEquals("Assignment policy mismatch", assignmentPolicy, assignmentPolicyEnforcement);
	}

}
