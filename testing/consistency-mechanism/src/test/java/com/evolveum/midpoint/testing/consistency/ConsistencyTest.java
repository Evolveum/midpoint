/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.testing.consistency;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttributeNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoRepoCache;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayJaxb;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import org.apache.commons.lang.StringUtils;
import org.opends.server.types.Entry;
import org.opends.server.util.EmbeddedUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
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
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManagerException;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

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
	
	private static final String ROLE_SUPERUSER_FILENAME = REPO_DIR_NAME + "role-superuser.xml";
    private static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";
    
    private static final String ROLE_LDAP_ADMINS_FILENAME = REPO_DIR_NAME + "role-admins.xml";
    private static final String ROLE_LDAP_ADMINS_OID = "88888888-8888-8888-8888-000000000009";

	private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = REPO_DIR_NAME + "sample-configuration-object.xml";
    private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";
	
	private static final String RESOURCE_OPENDJ_FILENAME = REPO_DIR_NAME + "resource-opendj.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	private static final String RESOURCE_OPENDJ_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
	private static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_OPENDJ_NS,"inetOrgPerson");
	private static final QName RESOURCE_OPENDJ_GROUP_OBJECTCLASS = new QName(RESOURCE_OPENDJ_NS,"groupOfUniqueNames");
	private static final String RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME = "entryUUID";
	private static final String RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME = "dn";
	private static final QName RESOURCE_OPENDJ_SECONDARY_IDENTIFIER = new QName(RESOURCE_OPENDJ_NS, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME);

	private static final String CONNECTOR_LDAP_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-ldap/com.evolveum.polygon.connector.ldap.LdapConnector";

	private static final String USER_TEMPLATE_FILENAME = REPO_DIR_NAME + "user-template.xml";

	private static final String USER_ADMINISTRATOR_FILENAME = REPO_DIR_NAME + "user-administrator.xml";
	private static final String USER_ADMINISTRATOR_NAME = "administrator";

	private static final String USER_JACK_FILENAME = REPO_DIR_NAME + "user-jack.xml";
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

	private static final String USER_DENIELS_FILENAME = REPO_DIR_NAME + "user-deniels.xml";
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
	
	private static final String USER_HERMAN_FILENAME = REPO_DIR_NAME + "user-herman.xml";
	private static final String USER_HERMAN_OID = "c0c010c0-d34d-b33f-f00d-111111119999";
	
	private static final String USER_MORGAN_FILENAME = REQUEST_DIR_NAME + "user-morgan.xml";
	private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";
	
	private static final String USER_CHUCK_FILENAME = REQUEST_DIR_NAME + "user-chuck.xml";
	private static final String USER_CHUCK_OID = "c0c010c0-d34d-b33f-f00d-171171118888";
	
	private static final String USER_ANGELIKA_FILENAME = REPO_DIR_NAME + "user-angelika.xml";
	private static final String USER_ANGELIKA_OID = "c0c010c0-d34d-b33f-f00d-111111111888";
	
	private static final String USER_ALICE_FILENAME = REPO_DIR_NAME + "user-alice.xml";
	private static final String USER_ALICE_OID = "c0c010c0-d34d-b33f-f00d-111111111999";
	
	private static final String USER_BOB_NO_GIVEN_NAME_FILENAME = REPO_DIR_NAME + "user-bob-no-given-name.xml";
	private static final String USER_BOB_NO_GIVEN_NAME_OID = "c0c010c0-d34d-b33f-f00d-222111222999";
	
	private static final String USER_JOHN_WEAK_FILENAME = REPO_DIR_NAME + "user-john.xml";
	private static final String USER_JOHN_WEAK_OID = "c0c010c0-d34d-b33f-f00d-999111111888";
	
	private static final String USER_DONALD_FILENAME = REPO_DIR_NAME + "user-donald.xml";
	private static final String USER_DONALD_OID = "c0c010c0-d34d-b33f-f00d-999111111777";
	
	private static final String USER_DISCOVERY_FILENAME = REPO_DIR_NAME + "user-discovery.xml";
	private static final String USER_DISCOVERY_OID = "c0c010c0-d34d-b33f-f00d-111112226666";
	
	private static final String USER_ABOMBA_FILENAME = REPO_DIR_NAME + "user-abomba.xml";
	private static final String USER_ABOMBA_OID = "c0c010c0-d34d-b33f-f00d-016016111111";
	
	private static final String USER_ABOM_FILENAME = REPO_DIR_NAME + "user-abom.xml";
	private static final String USER_ABOM_OID = "c0c010c0-d34d-b33f-f00d-111111016016";
	
	private static final File ACCOUNT_GUYBRUSH_FILE = new File(REPO_DIR_NAME, "account-guybrush.xml");
	private static final String ACCOUNT_GUYBRUSH_OID = "a0c010c0-d34d-b33f-f00d-111111111222";
	
	private static final File ACCOUNT_HECTOR_FILE = new File(REPO_DIR_NAME, "account-hector-not-found.xml");
	private static final String ACCOUNT_HECTOR_OID = "a0c010c0-d34d-b33f-f00d-111111222333";

	private static final File ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILE = new File(REPO_DIR_NAME, "account-guybrush-not-found.xml");
	private static final String ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID = "a0c010c0-d34d-b33f-f00d-111111111333";

	private static final String ACCOUNT_DENIELS_FILENAME = REPO_DIR_NAME + "account-deniels.xml";
	private static final String ACCOUNT_DENIELS_OID = "a0c010c0-d34d-b33f-f00d-111111111555";
	
	private static final String ACCOUNT_CHUCK_FILENAME = REPO_DIR_NAME + "account-chuck.xml";
	
	private static final String ACCOUNT_HERMAN_FILENAME = REPO_DIR_NAME + "account-herman.xml";
	private static final String ACCOUNT_HERMAN_OID = "22220000-2200-0000-0000-333300003333";

	private static final String REQUEST_USER_MODIFY_ASSIGN_ACCOUNT = "src/test/resources/request/user-modify-assign-account.xml";
	private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_DIRECTLY = "src/test/resources/request/user-modify-add-account-directly.xml";
	private static final String REQUEST_USER_MODIFY_DELETE_ACCOUNT = "src/test/resources/request/user-modify-delete-account.xml";
	private static final String REQUEST_USER_MODIFY_DELETE_ACCOUNT_COMMUNICATION_PROBLEM = "src/test/resources/request/user-modify-delete-account-communication-problem.xml";
	private static final String REQUEST_USER_MODIFY_ASSIGN_ROLE_ADMINS = "src/test/resources/request/user-modify-assign-role-admin.xml";
	
	private static final String REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT = "src/test/resources/request/account-guybrush-modify-attributes.xml";
	private static final String REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM = "src/test/resources/request/account-modify-attrs-communication-problem.xml";
	private static final String REQUEST_ADD_ACCOUNT_JACKIE = "src/test/resources/request/add-account-jack.xml";
	private static final String REQUEST_USER_MODIFY_WEAK_MAPPING_COMMUNICATION_PROBLEM = "src/test/resources/request/user-modify-employeeType.xml";
	private static final String REQUEST_USER_MODIFY_WEAK_STRONG_MAPPING_COMMUNICATION_PROBLEM = "src/test/resources/request/user-modify-employeeType-givenName.xml";
	private static final String REQUEST_RESOURCE_MODIFY_RESOURCE_SCHEMA = "src/test/resources/request/resource-modify-resource-schema.xml";
	private static final String REQUEST_RESOURCE_MODIFY_SYNCHRONIZATION = "src/test/resources/request/resource-modify-synchronization.xml";
	private static final String REQUEST_USER_MODIFY_CHANGE_PASSWORD_1 = "src/test/resources/request/user-modify-change-password-1.xml";
	private static final String REQUEST_USER_MODIFY_CHANGE_PASSWORD_2 = "src/test/resources/request/user-modify-change-password-2.xml";

	private static final String TASK_OPENDJ_RECONCILIATION_FILENAME = "src/test/resources/repo/task-opendj-reconciliation.xml";
	private static final String TASK_OPENDJ_RECONCILIATION_OID = "91919191-76e0-59e2-86d6-3d4f02d30000";

	private static final String LDIF_WILL_FILENAME = "src/test/resources/request/will.ldif";
	private static final String LDIF_ELAINE_FILENAME = "src/test/resources/request/elaine.ldif";
	private static final String LDIF_MORGAN_FILENAME = "src/test/resources/request/morgan.ldif";
	private static final String LDIF_DISCOVERY_FILENAME = "src/test/resources/request/discovery.ldif";
	
	private static final String LDIF_CREATE_USERS_OU_FILENAME = "src/test/resources/request/usersOu.ldif";
	private static final String LDIF_CREATE_ADMINS_GROUP_FILENAME = "src/test/resources/request/adminsGroup.ldif";
	
	
	private static final String LDIF_MODIFY_RENAME_FILENAME = "src/test/resources/request/modify-rename.ldif";

	private static final Trace LOGGER = TraceManager.getTrace(ConsistencyTest.class);

	private static final String NS_MY = "http://whatever.com/my";
	private static final QName MY_SHIP_STATE = new QName(NS_MY, "shipState");

	private static ResourceType resourceTypeOpenDjrepo;
	private static String accountShadowOidOpendj;
	private String aliceAccountDn;

	// This will get called from the superclass to init the repository
	// It will be called only once
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(ROLE_SUPERUSER_FILENAME, initResult);
		repoAddObjectFromFile(ROLE_LDAP_ADMINS_FILENAME, initResult);
		repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, initResult);

		// This should discover the connectors
		LOGGER.trace("initSystem: trying modelService.postInit()");
		modelService.postInit(initResult);
		LOGGER.trace("initSystem: modelService.postInit() done");
		
		login(USER_ADMINISTRATOR_NAME);

		// We need to add config after calling postInit() so it will not be applied.
		// we want original logging configuration from the test logback config file, not
		// the one from the system config.
		repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, initResult);

		// Need to import instead of add, so the (dynamic) connector reference
		// will be resolved correctly
		importObjectFromFile(RESOURCE_OPENDJ_FILENAME, initResult);

		repoAddObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME, initResult);
		repoAddObjectFromFile(USER_TEMPLATE_FILENAME, initResult);
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.POSITIVE);
		
//		DebugUtil.setDetailedDebugDump(true);
	}

	/**
	 * Initialize embedded OpenDJ instance Note: this is not in the abstract
	 * superclass so individual tests may avoid starting OpenDJ.
	 */
	@Override
	public void startResources() throws Exception {
		openDJController.startCleanServer();
	}

	/**
	 * Shutdown embedded OpenDJ instance Note: this is not in the abstract
	 * superclass so individual tests may avoid starting OpenDJ.
	 */
	@AfterClass
	public static void stopResources() throws Exception {
		openDJController.stop();
	}

	/**
	 * Test integrity of the test setup.
	 */
	@Test
	public void test000Integrity() throws Exception {
		final String TEST_NAME = "test000Integrity";
		TestUtil.displayTestTile(this, TEST_NAME);
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

		OperationResult result = new OperationResult(ConsistencyTest.class.getName() + "." + TEST_NAME);

		// Check if OpenDJ resource was imported correctly

		PrismObject<ResourceType> openDjResource = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, null, result);
		display("Imported OpenDJ resource (repository)", openDjResource);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());
		assertNoRepoCache();

		String ldapConnectorOid = openDjResource.asObjectable().getConnectorRef().getOid();
		PrismObject<ConnectorType> ldapConnector = repositoryService.getObject(ConnectorType.class,
				ldapConnectorOid, null, result);
		display("LDAP Connector: ", ldapConnector);
	}

	/**
	 * Test the testResource method. Expect a complete success for now.
	 */
	@Test
	public void test001TestConnectionOpenDJ() throws Exception {
		final String TEST_NAME = "test001TestConnectionOpenDJ";
		TestUtil.displayTestTile(TEST_NAME);

		Task task = taskManager.createTaskInstance();
		// GIVEN

		assertNoRepoCache();

		// WHEN
		OperationResultType result = modelWeb.testResource(RESOURCE_OPENDJ_OID);

		// THEN

		assertNoRepoCache();

		displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

		TestUtil.assertSuccess("testResource has failed", result);

		OperationResult opResult = new OperationResult(ConsistencyTest.class.getName()
				+ "." + TEST_NAME);

		PrismObject<ResourceType> resourceOpenDjRepo = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, null, opResult);
		resourceTypeOpenDjrepo = resourceOpenDjRepo.asObjectable();

		assertNoRepoCache();
		assertEquals(RESOURCE_OPENDJ_OID, resourceTypeOpenDjrepo.getOid());
		display("Initialized OpenDJ resource (respository)", resourceTypeOpenDjrepo);
		assertNotNull("Resource schema was not generated", resourceTypeOpenDjrepo.getSchema());
		Element resourceOpenDjXsdSchemaElement = ResourceTypeUtil
				.getResourceXsdSchema(resourceTypeOpenDjrepo);
		assertNotNull("Resource schema was not generated", resourceOpenDjXsdSchemaElement);

		PrismObject<ResourceType> openDjResourceProvisioninig = provisioningService.getObject(
				ResourceType.class, RESOURCE_OPENDJ_OID, null, task, opResult);
		display("Initialized OpenDJ resource resource (provisioning)", openDjResourceProvisioninig);

		PrismObject<ResourceType> openDjResourceModel = provisioningService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, null, task, opResult);
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

	/**
	 * Attempt to add new user. It is only added to the repository, so check if
	 * it is in the repository after the operation.
	 */
	@Test
	public void test100AddUser() throws Exception {
		final String TEST_NAME = "test100AddUser";

		UserType userType = testAddUserToRepo(TEST_NAME, USER_JACK_FILENAME, USER_JACK_OID);

		OperationResult repoResult = new OperationResult("getObject");
		PropertyReferenceListType resolve = new PropertyReferenceListType();

		PrismObject<UserType> uObject = repositoryService
				.getObject(UserType.class, USER_JACK_OID, null, repoResult);
		UserType repoUser = uObject.asObjectable();

		repoResult.computeStatus();
		display("repository.getObject result", repoResult);
		TestUtil.assertSuccess("getObject has failed", repoResult);
		AssertJUnit.assertEquals(USER_JACK_OID, repoUser.getOid());
		PrismAsserts.assertEqualsPolyString("User full name not equals as expected.", userType.getFullName(),
				repoUser.getFullName());

		// TODO: better checks
	}
	

	/**
	 * Add account to user. This should result in account provisioning. Check if
	 * that happens in repo and in LDAP.
	 */
	@Test
	public void test110PrepareOpenDjWithAccounts() throws Exception {
		final String TEST_NAME = "test110PrepareOpenDjWithAccounts";
		TestUtil.displayTestTile(TEST_NAME);
		OperationResult parentResult = new OperationResult(TEST_NAME);

		ShadowType jackeAccount = unmarshallValueFromFile(REQUEST_ADD_ACCOUNT_JACKIE,
                ShadowType.class);

		Task task = taskManager.createTaskInstance();
		String oid = provisioningService.addObject(jackeAccount.asPrismObject(), null, null, task, parentResult);
		PrismObject<ShadowType> jackFromRepo = repositoryService.getObject(ShadowType.class,
				oid, null, parentResult);
		LOGGER.debug("account jack after provisioning: {}", jackFromRepo.debugDump());

		PrismObject<UserType> jackUser = repositoryService.getObject(UserType.class, USER_JACK_OID,
				null, parentResult);
		ObjectReferenceType ort = new ObjectReferenceType();
		ort.setOid(oid);
		ort.setType(ShadowType.COMPLEX_TYPE);

		jackUser.asObjectable().getLinkRef().add(ort);

		PrismObject<UserType> jackUserRepo = repositoryService.getObject(UserType.class, USER_JACK_OID,
				null, parentResult);
		ObjectDelta delta = DiffUtil.diff(jackUserRepo, jackUser);

		repositoryService.modifyObject(UserType.class, USER_JACK_OID, delta.getModifications(), parentResult);

		// GIVEN

		OperationResult repoResult = new OperationResult("getObject");

		
		// Check if user object was modified in the repo
		accountShadowOidOpendj = assertUserOneAccountRef(USER_JACK_OID);
		assertFalse(accountShadowOidOpendj.isEmpty());

		// Check if shadow was created in the repo

		repoResult = new OperationResult("getObject");

		PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class,
				accountShadowOidOpendj, null, repoResult);
		ShadowType repoShadowType = repoShadow.asObjectable();
		repoResult.computeStatus();
		TestUtil.assertSuccess("getObject has failed", repoResult);
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

		Entry entry = openDJController.searchAndAssertByEntryUuid(uid);

		display("LDAP account", entry);
	
		OpenDJController.assertAttribute(entry, "uid", "jackie");
		OpenDJController.assertAttribute(entry, "givenName", "Jack");
		OpenDJController.assertAttribute(entry, "sn", "Sparrow");
		OpenDJController.assertAttribute(entry, "cn", "Jack Sparrow");

		assertNoRepoCache();

		Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
		Holder<ObjectType> objectHolder = new Holder<ObjectType>();

		// WHEN
		PropertyReferenceListType resolve = new PropertyReferenceListType();
//		List<ObjectOperationOptions> options = new ArrayList<ObjectOperationOptions>();
		modelWeb.getObject(ObjectTypes.SHADOW.getTypeQName(), accountShadowOidOpendj, null,
				objectHolder, resultHolder);

		// THEN
		assertNoRepoCache();
		displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
		TestUtil.assertSuccess("getObject has failed", resultHolder.value);

		ShadowType modelShadow = (ShadowType) objectHolder.value;
		display("Shadow (model)", modelShadow);

		AssertJUnit.assertNotNull(modelShadow);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

		assertAttributeNotNull(modelShadow, getOpenDjPrimaryIdentifierQName());
		assertAttributes(modelShadow, "jackie", "Jack", "Sparrow", "Jack Sparrow");
		// "middle of nowhere");
		assertNull("carLicense attribute sneaked to LDAP",
				OpenDJController.getAttributeValue(entry, "carLicense"));

		assertNotNull("Activation is null", modelShadow.getActivation());
		assertNotNull("No 'enabled' in the shadow", modelShadow.getActivation().getAdministrativeStatus());
		assertEquals("The account is not enabled in the shadow", ActivationStatusType.ENABLED, modelShadow.getActivation().getAdministrativeStatus());

		TestUtil.displayTestTile("test013prepareOpenDjWithAccounts - add second account");

		OperationResult secondResult = new OperationResult(
				"test013prepareOpenDjWithAccounts - add second account");

		ShadowType shadow = unmarshallValueFromFile(ACCOUNT_DENIELS_FILENAME, ShadowType.class);

		provisioningService.addObject(shadow.asPrismObject(), null, null, task, secondResult);

		repoAddObjectFromFile(USER_DENIELS_FILENAME, secondResult);

	}
	
	@Test
	public void test120AddAccountAlreadyExistLinked() throws Exception {
		final String TEST_NAME = "test120AddAccountAlreadyExistLinked";
		TestUtil.displayTestTile(TEST_NAME);
		Task task = taskManager.createTaskInstance();
		// GIVEN
		OperationResult parentResult = new OperationResult("Add account already exist linked");
		testAddUserToRepo("test014testAssAccountAlreadyExistLinked", USER_JACK2_FILENAME, USER_JACK2_OID);

		assertUserNoAccountRef(USER_JACK2_OID, parentResult);

//		//check if the jackie account already exists on the resource
		String accountRef = assertUserOneAccountRef(USER_JACK_OID);

		PrismObject<ShadowType> jackUserAccount = repositoryService.getObject(ShadowType.class, accountRef, null, parentResult);
		display("Jack's account: ", jackUserAccount.debugDump());
					
		// WHEN REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_LINKED_OPENDJ_FILENAME
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_JACK2_OID, UserType.class, task, null, parentResult);

		// THEN		
		//expected thet the dn and ri:uid will be jackie1 because jackie already exists and is liked to another user..
		String accountOid = checkUser(USER_JACK2_OID, task, parentResult);
		
		checkAccount(accountOid, "jackie1", "Jack", "Russel", "Jack Russel", task, parentResult);
	}
	
	
	@Test
	public void test122AddAccountAlreadyExistUnlinked() throws Exception {
		final String TEST_NAME = "test122AddAccountAlreadyExistUnlinked";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		OperationResult parentResult = new OperationResult("Add account already exist unlinked.");
		Entry entry = openDJController.addEntryFromLdifFile(LDIF_WILL_FILENAME);
		Entry searchResult = openDJController.searchByUid("wturner");
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
		assertUserNoAccountRef(USER_WILL_OID, parentResult);

		Task task = taskManager.createTaskInstance();
		
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_WILL_OID, UserType.class, task, null, parentResult);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		String accountOid = checkUser(USER_WILL_OID, task, parentResult);
//		MidPointAsserts.assertAssignments(user, 1);

		PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class,
				accountOid, null, task, parentResult);

		ResourceAttributeContainer attributes = ShadowUtil.getAttributesContainer(account);

		assertEquals("shadow secondary identifier not equal with the account dn. ", dn, attributes
				.findAttribute(getOpenDjSecondaryIdentifierQName()).getRealValue(String.class));

		String identifier = attributes.getPrimaryIdentifier().getRealValue(String.class);

		openDJController.searchAndAssertByEntryUuid(identifier);

	}
	
	//MID-1595, MID-1577
	@Test
	public void test124AddAccountDirectAlreadyExists() throws Exception {
		final String TEST_NAME = "test124AddAccountDirectAlreadyExists";
		TestUtil.displayTestTile(TEST_NAME);
		OperationResult parentResult = new OperationResult(TEST_NAME);
		Task task = taskManager.createTaskInstance();

		SchemaHandlingType oldSchemaHandling = resourceTypeOpenDjrepo
				.getSchemaHandling();
		SynchronizationType oldSynchronization = resourceTypeOpenDjrepo
				.getSynchronization();
		try {

			// we will reapply this schema handling after this test finish
			ItemDefinition syncDef = resourceTypeOpenDjrepo.asPrismObject().getDefinition().findItemDefinition(ResourceType.F_SYNCHRONIZATION);
			assertNotNull("null definition for sync delta", syncDef);

			ObjectDeltaType omt = unmarshallValueFromFile(REQUEST_RESOURCE_MODIFY_SYNCHRONIZATION, ObjectDeltaType.class);
			ObjectDelta objectDelta = DeltaConvertor.createObjectDelta(omt, prismContext);
			
			repositoryService.modifyObject(ResourceType.class, RESOURCE_OPENDJ_OID, objectDelta.getModifications(), parentResult);
			requestToExecuteChanges(REQUEST_RESOURCE_MODIFY_RESOURCE_SCHEMA,
					RESOURCE_OPENDJ_OID, ResourceType.class, task, null,
					parentResult);

			PrismObject<ResourceType> res = repositoryService
					.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null,
							parentResult);
			// LOGGER.trace("resource schema handling after modify: {}",
			// prismContext.silentMarshalObject(res.asObjectable(), LOGGER));

			repoAddObjectFromFile(USER_ABOMBA_FILENAME,
					parentResult);
			requestToExecuteChanges(REQUEST_USER_MODIFY_ADD_ACCOUNT_DIRECTLY,
					USER_ABOMBA_OID, UserType.class, task, null, parentResult);

			String abombaOid = assertUserOneAccountRef(USER_ABOMBA_OID);

			ShadowType abombaShadow = repositoryService.getObject(
					ShadowType.class, abombaOid, null, parentResult)
					.asObjectable();
			assertShadowName(abombaShadow,
					"uid=abomba,OU=people,DC=example,DC=com");

			repoAddObjectFromFile(USER_ABOM_FILENAME,
					parentResult);
			requestToExecuteChanges(REQUEST_USER_MODIFY_ADD_ACCOUNT_DIRECTLY,
					USER_ABOM_OID, UserType.class, task, null, parentResult);

			String abomOid = assertUserOneAccountRef(USER_ABOM_OID);

			ShadowType abomShadow = repositoryService.getObject(
					ShadowType.class, abomOid, null, parentResult)
					.asObjectable();
			assertShadowName(abomShadow,
					"uid=abomba1,OU=people,DC=example,DC=com");

			ReferenceDelta abombaDeleteAccDelta = ReferenceDelta
					.createModificationDelete(ShadowType.class,
							UserType.F_LINK_REF, prismContext,
							new PrismReferenceValue(abombaOid));
			ObjectDelta d = ObjectDelta.createModifyDelta(USER_ABOMBA_OID,
					abombaDeleteAccDelta, UserType.class, prismContext);
			modelService.executeChanges(MiscSchemaUtil.createCollection(d), null, task,
					parentResult);

			assertUserNoAccountRef(USER_ABOMBA_OID, parentResult);

			repositoryService.getObject(ShadowType.class, abombaOid, null,
					parentResult);

			ReferenceDelta abomDeleteAccDelta = ReferenceDelta
					.createModificationDelete(ShadowType.class,
							UserType.F_LINK_REF, prismContext,
							abomShadow.asPrismObject());
			ObjectDelta d2 = ObjectDelta.createModifyDelta(USER_ABOM_OID,
					abomDeleteAccDelta, UserType.class, prismContext);
			modelService.executeChanges(MiscSchemaUtil.createCollection(d2), null, task,
					parentResult);

			assertUserNoAccountRef(USER_ABOM_OID, parentResult);
			try {
				repositoryService.getObject(ShadowType.class, abomOid, null,
						parentResult);
				fail("Expected that shadow abom does not exist, but it is");
			} catch (ObjectNotFoundException ex) {
				// this is expected
			} catch (Exception ex) {
				fail("Expected object not found exception but got " + ex);
			}

			LOGGER.info("starting second execution request for user abomba");
			OperationResult result = new OperationResult("Add account already exist result.");
			requestToExecuteChanges(REQUEST_USER_MODIFY_ADD_ACCOUNT_DIRECTLY,
					USER_ABOMBA_OID, UserType.class, task, null, result);

			
			String abombaOid2 = assertUserOneAccountRef(USER_ABOMBA_OID);
			ShadowType abombaShadow2 = repositoryService.getObject(
					ShadowType.class, abombaOid2, null, result)
					.asObjectable();
			assertShadowName(abombaShadow2,
					"uid=abomba,OU=people,DC=example,DC=com");

			
			result.computeStatus();
			
			LOGGER.info("Displaying execute changes result");
			display(result);
			
			// return the previous changes of resource back
			Collection<? extends ItemDelta> schemaHandlingDelta = ContainerDelta
					.createModificationReplaceContainerCollection(
							ResourceType.F_SCHEMA_HANDLING,
							resourceTypeOpenDjrepo.asPrismObject()
									.getDefinition(), oldSchemaHandling.asPrismContainerValue().clone());
			PropertyDelta syncDelta = PropertyDelta
					.createModificationReplaceProperty(
							ResourceType.F_SYNCHRONIZATION,
							resourceTypeOpenDjrepo.asPrismObject()
									.getDefinition(), oldSynchronization);
			((Collection) schemaHandlingDelta).add(syncDelta);
			repositoryService.modifyObject(ResourceType.class,
					RESOURCE_OPENDJ_OID, schemaHandlingDelta, parentResult);
		} catch (Exception ex) {
			LOGGER.info("error: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	@Test
	public void test130DeleteObjectNotFound() throws Exception {
		final String TEST_NAME = "test130DeleteObjectNotFound";
		TestUtil.displayTestTile(TEST_NAME);
		OperationResult parentResult = new OperationResult(TEST_NAME);

		repoAddShadowFromFile(ACCOUNT_GUYBRUSH_FILE, parentResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, parentResult);
		
		Task task = taskManager.createTaskInstance();
		requestToExecuteChanges(REQUEST_USER_MODIFY_DELETE_ACCOUNT, USER_GUYBRUSH_OID, UserType.class, task, null, parentResult);

		// WHEN
		ObjectDelta deleteDelta = ObjectDelta.createDeleteDelta(ShadowType.class, ACCOUNT_GUYBRUSH_OID, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(deleteDelta);
		modelService.executeChanges(deltas, null, task, parentResult);

		try {
			repositoryService.getObject(ShadowType.class, ACCOUNT_GUYBRUSH_OID, null, parentResult);
		} catch (Exception ex) {
			if (!(ex instanceof ObjectNotFoundException)) {
				fail("Expected ObjectNotFoundException but got " + ex);
			}
		}
		
		assertUserNoAccountRef(USER_GUYBRUSH_OID, parentResult);

		repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, parentResult);
	}

	/**
	 * Modify account not found => reaction: Delete account
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void test140ModifyObjectNotFound() throws Exception {
		final String TEST_NAME = "test140ModifyObjectNotFound";
		TestUtil.displayTestTile(TEST_NAME);
		OperationResult result = new OperationResult(TEST_NAME);

		repoAddShadowFromFile(ACCOUNT_GUYBRUSH_FILE, result);
		repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, result);

		assertUserOneAccountRef(USER_GUYBRUSH_OID);
		
		Task task = taskManager.createTaskInstance();
		
		// WHEN
		requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT, ACCOUNT_GUYBRUSH_OID, ShadowType.class, task, null, result);

		// THEN
		try {
			repositoryService.getObject(ShadowType.class, ACCOUNT_GUYBRUSH_OID, null, result);
			fail("Expected ObjectNotFound but did not get one.");
		} catch (Exception ex) {
			if (!(ex instanceof ObjectNotFoundException)) {
				fail("Expected ObjectNotFoundException but got " + ex);
			}
		}

		assertUserNoAccountRef(USER_GUYBRUSH_OID, result);

		repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, result);
	}

	/**
	 * Modify account not found => reaction: Re-create account, apply changes.
	 */
	@Test
	public void test142ModifyObjectNotFoundAssignedAccount() throws Exception {
		final String TEST_NAME = "test142ModifyObjectNotFoundAssignedAccount";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult parentResult = new OperationResult(TEST_NAME);

		repoAddShadowFromFile(ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILE, parentResult);
		repoAddObjectFromFile(USER_GUYBRUSH_NOT_FOUND_FILENAME, parentResult);

		assertUserOneAccountRef(USER_GUYBRUSH_NOT_FOUND_OID);
		
		Task task = taskManager.createTaskInstance();
		
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT, ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID, ShadowType.class, task, null, parentResult);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		String accountOid = assertUserOneAccountRef(USER_GUYBRUSH_NOT_FOUND_OID);
		
		PrismObject<ShadowType> modifiedAccount = provisioningService.getObject(
				ShadowType.class, accountOid, null, task, parentResult);
		assertNotNull(modifiedAccount);
		display("Modified shadow", modifiedAccount);
		assertShadowName(modifiedAccount.asObjectable(), "uid=guybrush123,ou=people,dc=example,dc=com");
//		PrismAsserts.assertEqualsPolyString("Wrong shadow name", "uid=guybrush123,ou=people,dc=example,dc=com", modifiedAccount.asObjectable().getName());
		ResourceAttributeContainer attributeContainer = ShadowUtil
				.getAttributesContainer(modifiedAccount);
		assertAttribute(modifiedAccount.asObjectable(),
				new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "roomNumber"),
				"cabin");
		assertNotNull(attributeContainer.findProperty(new QName(ResourceTypeUtil
				.getResourceNamespace(resourceTypeOpenDjrepo), "businessCategory")));

	}

	/**
	 * Get account not found => reaction: Re-create account, return re-created.
	 */
	@Test
	public void test144GetObjectNotFoundAssignedAccount() throws Exception {
		final String TEST_NAME = "test144GetObjectNotFoundAssignedAccount";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		OperationResult parentResult = new OperationResult(TEST_NAME);

		repoAddShadowFromFile(ACCOUNT_HECTOR_FILE, parentResult);
		repoAddObjectFromFile(USER_HECTOR_NOT_FOUND_FILENAME, parentResult);

		assertUserOneAccountRef(USER_HECTOR_NOT_FOUND_OID);

		Task task = taskManager.createTaskInstance();

		//WHEN
		PrismObject<UserType> modificatedUser = modelService.getObject(UserType.class, USER_HECTOR_NOT_FOUND_OID, null, task, parentResult);
		
		// THEN
		String accountOid = assertOneAccountRef(modificatedUser);
		
		PrismObject<ShadowType> modifiedAccount = modelService.getObject(ShadowType.class, accountOid, null, task, parentResult);
		assertNotNull(modifiedAccount);
		assertShadowName(modifiedAccount.asObjectable(), "uid=hector,ou=people,dc=example,dc=com");
	}
	
	/**
	 * Recompute user => account not found => reaction: Re-create account
	 * MID-3093
	 */
	@Test
	public void test150RecomputeUserAccountNotFound() throws Exception {
		final String TEST_NAME = "test150RecomputeUserAccountNotFound";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		PrismObject<UserType> userGuybrushFromFile = PrismTestUtil.parseObject(new File(USER_GUYBRUSH_FILENAME));
		userGuybrushFromFile.asObjectable().getLinkRef().clear();
		repoAddObject(userGuybrushFromFile, result);
		assignAccount(USER_GUYBRUSH_OID, RESOURCE_OPENDJ_OID, null);
		
		PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
		String accountShadowOid = assertOneAccountRef(userBefore);
		PrismObject<ShadowType> shadowBefore = getShadowModel(accountShadowOid);
		display("Model Shadow before", shadowBefore);
		
		String dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
		openDJController.delete(dn);
		
		PrismObject<ShadowType> repoShadowBefore = repositoryService.getObject(ShadowType.class, accountShadowOid, null, result);
		assertNotNull("Repo shadow is gone!", repoShadowBefore);
		display("Repository shadow before", repoShadowBefore);
		assertTrue("Oh my! Shadow is dead!", repoShadowBefore.asObjectable().isDead() != Boolean.TRUE);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		recomputeUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		String accountShadowOidAfter = assertOneAccountRef(userAfter);
		PrismObject<ShadowType> shadowAfter = getShadowModel(accountShadowOidAfter);
		display("Shadow after", shadowAfter);

		Entry entryAfter = openDJController.fetchEntry(dn);
		display("Entry after", entryAfter);
	}
	
	/**
	 * Recompute user => account not found => reaction: Re-create account
	 * MID-3093
	 */
	@Test
	public void test152RecomputeUserAccountAndShadowNotFound() throws Exception {
		final String TEST_NAME = "test152RecomputeUserAccountAndShadowNotFound";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
		String accountShadowOid = assertOneAccountRef(userBefore);
		PrismObject<ShadowType> shadowBefore = getShadowModel(accountShadowOid);
		display("Shadow before", shadowBefore);
		
		String dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
		openDJController.delete(dn);
		
		repositoryService.deleteObject(ShadowType.class, accountShadowOid, result);
		
		// WHEN
		recomputeUser(USER_GUYBRUSH_OID, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
		display("User after", userAfter);
		String accountShadowOidAfter = assertOneAccountRef(userAfter);
		PrismObject<ShadowType> shadowAfter = getShadowModel(accountShadowOidAfter);
		display("Shadow after", shadowAfter);

		Entry entryAfter = openDJController.fetchEntry(dn);
		display("Entry after", entryAfter);
	}
	
	@Test
	public void test159DeleteUSerGuybrush() throws Exception {
		final String TEST_NAME = "test159DeleteUSerGuybrush";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();

		PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
		display("User before", userBefore);
		String accountShadowOid = assertOneAccountRef(userBefore);
		PrismObject<ShadowType> shadowBefore = getShadowModel(accountShadowOid);
		display("Shadow before", shadowBefore);
		
		String dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
		openDJController.delete(dn);
		
		// WHEN
		deleteObject(UserType.class, USER_GUYBRUSH_OID, task, result);

		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertNoObject(UserType.class, USER_GUYBRUSH_OID, task, result);
		
		// TODO: assert no shadow
		// TODO: assert no entry
	}
	
	@Test
	public void test200StopOpenDj() throws Exception {
		final String TEST_NAME = "test200StopOpenDj";
		TestUtil.displayTestTile(TEST_NAME);
		openDJController.stop();

		assertEquals("Resource is running", false, EmbeddedUtils.isRunning());
	}

	@Test
	public void test210AddObjectCommunicationProblem() throws Exception {
		final String TEST_NAME = "test210AddObjectCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		openDJController.assumeStopped();
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = task.getResult();
		
		repoAddObjectFromFile(USER_E_FILENAME, parentResult);

		assertUserNoAccountRef(USER_E_OID, parentResult);
		
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_E_OID, UserType.class, task, null, parentResult);

		parentResult.computeStatus();
		display("add object communication problem result: ", parentResult);
		assertEquals("Expected handled error but got: " + parentResult.getStatus(), OperationResultStatus.HANDLED_ERROR, parentResult.getStatus());
		
		String accountOid = checkRepoUser(USER_E_OID, parentResult); 

		checkPostponedAccountWithAttributes(accountOid, "e", "e", "e", "e", FailedOperationTypeType.ADD, false, task, parentResult);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void test212AddModifyObjectCommunicationProblem() throws Exception {
		final String TEST_NAME = "test212AddModifyObjectCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeStopped();
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = task.getResult();

		String accountOid = assertUserOneAccountRef(USER_E_OID);
		 
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM, accountOid, ShadowType.class, task, null, parentResult);
		
		//THEN
		TestUtil.displayThen(TEST_NAME);
		checkPostponedAccountWithAttributes(accountOid, "e", "Jackkk", "e", "e", "emp4321", FailedOperationTypeType.ADD, false, task, parentResult);
	}

	@Test
	public void test214ModifyObjectCommunicationProblem() throws Exception {
		final String TEST_NAME = "test214ModifyObjectCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeStopped();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		String accountOid = assertUserOneAccountRef(USER_JACK_OID);

		Task task = taskManager.createTaskInstance();
		
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM, accountOid, ShadowType.class, task, null, parentResult);
		
		//THEN
		TestUtil.displayThen(TEST_NAME);
		checkPostponedAccountBasic(accountOid, FailedOperationTypeType.MODIFY, true, parentResult);
	}

	@Test
	public void test220DeleteObjectCommunicationProblem() throws Exception {
		final String TEST_NAME = "test220DeleteObjectCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeStopped();
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = task.getResult();
		
		String accountOid = assertUserOneAccountRef(USER_DENIELS_OID);
		
		//WHEN
		requestToExecuteChanges(REQUEST_USER_MODIFY_DELETE_ACCOUNT_COMMUNICATION_PROBLEM, USER_DENIELS_OID, UserType.class, task, null, parentResult);
		
		assertUserNoAccountRef(USER_DENIELS_OID, parentResult);
		
		ObjectDelta deleteDelta = ObjectDelta.createDeleteDelta(ShadowType.class, ACCOUNT_DENIELS_OID, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(deleteDelta);
		modelService.executeChanges(deltas, null, task, parentResult);

		// THEN
		checkPostponedAccountBasic(accountOid, FailedOperationTypeType.DELETE, false, parentResult);
	}

	@Test
	public void test230GetAccountCommunicationProblem() throws Exception {
		final String TEST_NAME = "test230GetAccountCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		openDJController.assumeStopped();
		OperationResult result = new OperationResult(TEST_NAME);
		
		ShadowType account = modelService.getObject(ShadowType.class, ACCOUNT_DENIELS_OID,
				null, null, result).asObjectable();
		assertNotNull("Get method returned null account.", account);
		assertNotNull("Fetch result was not set in the shadow.", account.getFetchResult());
	}

	@Test
	public void test240AddObjectCommunicationProblemAlreadyExists() throws Exception{
		final String TEST_NAME = "test240AddObjectCommunicationProblemAlreadyExists";
		TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
		openDJController.assumeRunning();
		OperationResult parentResult = new OperationResult(TEST_NAME);
	
		Entry entry = openDJController.addEntryFromLdifFile(LDIF_ELAINE_FILENAME);
		Entry searchResult = openDJController.searchByUid("elaine");
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
		
		testAddUserToRepo(TEST_NAME, USER_ELAINE_FILENAME,
				USER_ELAINE_OID);
		assertUserNoAccountRef(USER_ELAINE_OID, parentResult);

		Task task = taskManager.createTaskInstance();
		
		//WHEN REQUEST_USER_MODIFY_ADD_ACCOUNT_ALERADY_EXISTS_COMMUNICATION_PROBLEM_OPENDJ_FILENAME
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_ELAINE_OID, UserType.class, task, null, parentResult);

		//THEN
		String accountOid = assertUserOneAccountRef(USER_ELAINE_OID);
		
		checkPostponedAccountWithAttributes(accountOid, "elaine", "Elaine", "Marley", "Elaine Marley", FailedOperationTypeType.ADD, false, task, parentResult);
	}
	
	@Test
	public void test250ModifyObjectTwoTimesCommunicationProblem() throws Exception {
		final String TEST_NAME = "test250ModifyObjectTwoTimesCommunicationProblem";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
		openDJController.assumeStopped();
        OperationResult parentResult = new OperationResult(TEST_NAME);
		
		assertUserOneAccountRef(USER_JACK2_OID);
		
		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
		
		PropertyDelta fullNameDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_FULL_NAME), getUserDefinition(), new PolyString("jackNew2"));
		modifications.add(fullNameDelta);
		
		PrismPropertyValue<ActivationStatusType> enabledUserAction = new PrismPropertyValue<ActivationStatusType>(ActivationStatusType.ENABLED, OriginType.USER_ACTION, null);
		PropertyDelta<ActivationStatusType> enabledDelta = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, getUserDefinition());
		enabledDelta.addValueToAdd(enabledUserAction);
		modifications.add(enabledDelta);
		
		ObjectDelta objectDelta = ObjectDelta.createModifyDelta(USER_JACK2_OID, modifications, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		
		
		Task task = taskManager.createTaskInstance();
		
		modelService.executeChanges(deltas, null, task, parentResult);
		parentResult.computeStatus();
		String accountOid = assertUserOneAccountRef(USER_JACK2_OID);

		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountOid, null, task, parentResult);
		assertNotNull(account);
		ShadowType shadow = account.asObjectable();
		assertNotNull(shadow.getObjectChange());
		display("shadow after communication problem", shadow);
		
		
		Collection<PropertyDelta> newModifications = new ArrayList<PropertyDelta>();
		PropertyDelta fullNameDeltaNew = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_FULL_NAME), getUserDefinition(), new PolyString("jackNew2a"));
		newModifications.add(fullNameDeltaNew);
		
		
		PropertyDelta givenNameDeltaNew = PropertyDelta.createModificationReplaceProperty(new ItemPath(UserType.F_GIVEN_NAME), getUserDefinition(), new PolyString("jackNew2a"));
		newModifications.add(givenNameDeltaNew);
		
		PrismPropertyValue<ActivationStatusType> enabledOutboundAction = new PrismPropertyValue<ActivationStatusType>(ActivationStatusType.ENABLED, OriginType.USER_ACTION, null);
		PropertyDelta<ActivationStatusType> enabledDeltaNew = PropertyDelta.createDelta(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, getUserDefinition());
		enabledDeltaNew.addValueToAdd(enabledOutboundAction);
		newModifications.add(enabledDeltaNew);
		
		ObjectDelta newObjectDelta = ObjectDelta.createModifyDelta(USER_JACK2_OID, newModifications, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> newDeltas = MiscSchemaUtil.createCollection(newObjectDelta);
		
		
		modelService.executeChanges(newDeltas, null, task, parentResult);
		
		account = modelService.getObject(ShadowType.class, accountOid, null, task, parentResult);
		assertNotNull(account);
		shadow = account.asObjectable();
		assertNotNull(shadow.getObjectChange());
		display("shadow after communication problem", shadow);
//		parentResult.computeStatus();
//		assertEquals("expected handled error in the result", OperationResultStatus.HANDLED_ERROR, parentResult.getStatus());
		
		
	}
	
	/**
	 * this test simulates situation, when someone tries to add account while
	 * resource is down and this account is created by next get call on this
	 * account
	 */
	@Test
	public void test260GetDiscoveryAddCommunicationProblem() throws Exception {
		final String TEST_NAME = "test260GetDiscoveryAddCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		openDJController.assumeStopped();
		display("OpenDJ stopped");
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		repoAddObjectFromFile(USER_ANGELIKA_FILENAME, parentResult);

		assertUserNoAccountRef(USER_ANGELIKA_OID, parentResult);
		
		Task task = taskManager.createTaskInstance();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_ANGELIKA_OID, UserType.class, task, null, parentResult);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		parentResult.computeStatus();
		display("add object communication problem result: ", parentResult);
		assertEquals("Expected handled error but got: " + parentResult.getStatus(), OperationResultStatus.HANDLED_ERROR, parentResult.getStatus());
		
		
		String accountOid = assertUserOneAccountRef(USER_ANGELIKA_OID);

		checkPostponedAccountWithAttributes(accountOid, "angelika", "angelika", "angelika", "angelika", FailedOperationTypeType.ADD, false, task, parentResult);
		
		//start openDJ
		openDJController.start();
		//and set the resource availability status to UP
		modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);
		TestUtil.info("OpenDJ started, resource UP");
		
		ShadowType shadowAfter = checkNormalizedShadowWithAttributes(accountOid, "angelika", "angelika", "angelika", "angelika", false, task, parentResult);
	}
		
	@Test
	public void test262GetDiscoveryModifyCommunicationProblem() throws Exception {
		final String TEST_NAME = "test262GetDiscoveryModifyCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeRunning();		
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = task.getResult();
		
		//prepare user 
		repoAddObjectFromFile(USER_ALICE_FILENAME, parentResult);
		
		assertUserNoAccountRef(USER_ALICE_OID, parentResult);

		//and add account to the user while resource is UP
		
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_ALICE_OID, UserType.class, task, null, parentResult);
		
		//then stop openDJ
		openDJController.stop();
		
		String accountOid = assertUserOneAccountRef(USER_ALICE_OID);

		//and make some modifications to the account while resource is DOWN
		requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM, accountOid, ShadowType.class, task, null, parentResult);

		//check the state after execution
		checkPostponedAccountBasic(accountOid, FailedOperationTypeType.MODIFY, true, parentResult);

		//start openDJ
		openDJController.start();
		//and set the resource availability status to UP
		modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);
		
		//and then try to get account -> result is that the modifications will be applied to the account
		ShadowType aliceAccount = checkNormalizedShadowWithAttributes(accountOid, "alice", "Jackkk", "alice", "alice", true, task, parentResult);
		assertAttribute(aliceAccount, "employeeNumber", "emp4321");
	}
	
	@Test
	public void test264GetDiscoveryModifyUserPasswordCommunicationProblem() throws Exception {
		final String TEST_NAME = "test264GetDiscoveryModifyUserPasswordCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeStopped();
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = task.getResult();
		
		String accountOid = assertUserOneAccountRef(USER_ALICE_OID);
		
		// WHEN (down)
		requestToExecuteChanges(REQUEST_USER_MODIFY_CHANGE_PASSWORD_1, USER_ALICE_OID, UserType.class, task, null, parentResult);

		// THEN
		//check the state after execution
		checkPostponedAccountBasic(accountOid, FailedOperationTypeType.MODIFY, true, parentResult);

		//start openDJ
		openDJController.start();
		modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);
		
		// WHEN (restore)
		//and then try to get account -> result is that the modifications will be applied to the account
		ShadowType aliceAccount = checkNormalizedShadowWithAttributes(accountOid, "alice", "Jackkk", "alice", "alice", true, task, parentResult);
		assertAttribute(aliceAccount, "employeeNumber", "emp4321");
		
		PrismObject<UserType> userAliceAfter = getUser(USER_ALICE_OID);
		assertPassword(userAliceAfter, "DEADmenTELLnoTALES");
		
		aliceAccountDn = ShadowUtil.getAttributeValue(aliceAccount, getOpenDjSecondaryIdentifierQName());
		openDJController.assertPassword(aliceAccountDn, "DEADmenTELLnoTALES");
	}
	
	/**
	 * Modify user password when resource is down. Bring resource up and run recon.
	 * Check that the password change is applied.
	 */
	@Test
	public void test265ModifyUserPasswordCommunicationProblemRecon() throws Exception {
		final String TEST_NAME = "test265ModifyUserPasswordCommunicationProblemRecon";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeStopped();
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult result = task.getResult();
		
		String accountOid = assertUserOneAccountRef(USER_ALICE_OID);
		
		// WHEN (down)
		TestUtil.displayWhen(TEST_NAME);
		requestToExecuteChanges(REQUEST_USER_MODIFY_CHANGE_PASSWORD_2, USER_ALICE_OID, UserType.class, task, null, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		//check the state after execution
		checkPostponedAccountBasic(accountOid, FailedOperationTypeType.MODIFY, true, result);

		//start openDJ
		openDJController.start();
		modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, result);
		
		// WHEN (restore)
		TestUtil.displayWhen(TEST_NAME);
		reconcileUser(USER_ALICE_OID, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		openDJController.assertPassword(aliceAccountDn, "UNDEADmenTELLscaryTALES");

		PrismObject<UserType> userAliceAfter = getUser(USER_ALICE_OID);
		assertPassword(userAliceAfter, "UNDEADmenTELLscaryTALES");
			
		// Do this as a last step so it will not interfere with the results
		ShadowType aliceAccount = checkNormalizedShadowWithAttributes(accountOid, "alice", "Jackkk", "alice", "alice", true, task, result);
		assertAttribute(aliceAccount, "employeeNumber", "emp4321");

	}
	
	/**
	 * this test simulates situation, when someone tries to add account while
	 * resource is down and this account is created by next get call on this
	 * account
	 */
	@Test
	public void test270ModifyDiscoveryAddCommunicationProblem() throws Exception {
		final String TEST_NAME = "test270ModifyDiscoveryAddCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeStopped();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		// WHEN
		repoAddObjectFromFile(USER_BOB_NO_GIVEN_NAME_FILENAME, parentResult);

		assertUserNoAccountRef(USER_BOB_NO_GIVEN_NAME_OID, parentResult);

		Task task = taskManager.createTaskInstance();
		
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_BOB_NO_GIVEN_NAME_OID, UserType.class, task, null, parentResult);
		
		parentResult.computeStatus();
		display("add object communication problem result: ", parentResult);
		assertEquals("Expected handled error but got: " + parentResult.getStatus(), OperationResultStatus.HANDLED_ERROR, parentResult.getStatus());
		
		String accountOid = assertUserOneAccountRef(USER_BOB_NO_GIVEN_NAME_OID);

		checkPostponedAccountWithAttributes(accountOid, "bob", null, "Dylan",  "Bob Dylan", FailedOperationTypeType.ADD, false, task, parentResult);
		
		//start openDJ
		openDJController.start();
		//and set the resource availability status to UP
		modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);
		
		// WHEN
		// This should not throw exception
		modelService.getObject(ShadowType.class, accountOid, null, task, parentResult);
		
		OperationResult modifyGivenNameResult = new OperationResult("execute changes -> modify user's given name");
		LOGGER.trace("execute changes -> modify user's given name");
		Collection<? extends ItemDelta> givenNameDelta = PropertyDelta.createModificationReplacePropertyCollection(UserType.F_GIVEN_NAME, getUserDefinition(), new PolyString("Bob"));
		ObjectDelta familyNameD = ObjectDelta.createModifyDelta(USER_BOB_NO_GIVEN_NAME_OID, givenNameDelta, UserType.class, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> modifyFamilyNameDelta = MiscSchemaUtil.createCollection(familyNameD);
		modelService.executeChanges(modifyFamilyNameDelta, null, task, modifyGivenNameResult);
		
		modifyGivenNameResult.computeStatus();
		display("add object communication problem result: ", modifyGivenNameResult);
		assertEquals("Expected handled error but got: " + modifyGivenNameResult.getStatus(), OperationResultStatus.SUCCESS, modifyGivenNameResult.getStatus());
		
		PrismObject<ShadowType> bobRepoAcc = repositoryService.getObject(ShadowType.class, accountOid, null, modifyGivenNameResult);
		assertNotNull(bobRepoAcc);
		ShadowType bobRepoAccount = bobRepoAcc.asObjectable();
		displayJaxb("Shadow after discovery: ", bobRepoAccount, ShadowType.COMPLEX_TYPE);
		assertNull("Bob's account after discovery must not have failed opertion.", bobRepoAccount.getFailedOperationType());
		assertNull("Bob's account after discovery must not have result.", bobRepoAccount.getResult());
		assertNotNull("Bob's account must contain reference on the resource", bobRepoAccount.getResourceRef());
		
		checkNormalizedShadowWithAttributes(accountOid, "bob", "Bob", "Dylan", "Bob Dylan", false, task, parentResult);
		
	}
	
	@Test
	public void test280ModifyObjectCommunicationProblemWeakMapping() throws Exception{
		final String TEST_NAME = "test280ModifyObjectCommunicationProblemWeakMapping";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		openDJController.assumeRunning();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		repoAddObjectFromFile(USER_JOHN_WEAK_FILENAME, parentResult);

		assertUserNoAccountRef(USER_JOHN_WEAK_OID, parentResult);
		
		Task task = taskManager.createTaskInstance();
		
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_JOHN_WEAK_OID, UserType.class, task, null, parentResult);
		
		parentResult.computeStatus();
		display("add object communication problem result: ", parentResult);
		assertEquals("Expected success but got: " + parentResult.getStatus(), OperationResultStatus.SUCCESS, parentResult.getStatus());
		
		String accountOid = assertUserOneAccountRef(USER_JOHN_WEAK_OID);
		
		checkNormalizedShadowWithAttributes(accountOid, "john", "john", "weak", "john weak", "manager", false, task, parentResult);
		
		//stop opendj and try to modify employeeType (weak mapping)
		openDJController.stop();
		
		LOGGER.info("start modifying user - account with weak mapping after stopping opendj.");
		
		requestToExecuteChanges(REQUEST_USER_MODIFY_WEAK_MAPPING_COMMUNICATION_PROBLEM, USER_JOHN_WEAK_OID, UserType.class, task, null, parentResult);

		// TODO: [RS] not 100% sure about this. But if you do not expect an error you should not set doNotDiscovery. Server is still not running.
		checkNormalizedShadowBasic(accountOid, "john", true, null, task, parentResult);
//		checkNormalizedShadowBasic(accountOid, "john", true, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), task, parentResult);
	}

	
	@Test
	public void test282ModifyObjectCommunicationProblemWeakAndStrongMapping() throws Exception {
		final String TEST_NAME = "test282ModifyObjectCommunicationProblemWeakAndStrongMapping";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeRunning();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		repoAddObjectFromFile(USER_DONALD_FILENAME, parentResult);

		assertUserNoAccountRef(USER_DONALD_OID, parentResult);
				
		Task task = taskManager.createTaskInstance();
		
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_DONALD_OID, UserType.class, task, null, parentResult);
		
		parentResult.computeStatus();
		display("add object communication problem result: ", parentResult);
		assertEquals("Expected success but got: " + parentResult.getStatus(), OperationResultStatus.SUCCESS, parentResult.getStatus());
		
		String accountOid = assertUserOneAccountRef(USER_DONALD_OID);
				
		ShadowType johnAccountType = checkNormalizedShadowWithAttributes(accountOid, "donald", "donald", "trump", "donald trump", false, task, parentResult);
		assertAttribute(johnAccountType, "employeeType", "manager");

		//stop opendj and try to modify employeeType (weak mapping)
		openDJController.stop();
		
		
		LOGGER.info("start modifying user - account with weak mapping after stopping opendj.");
		
		requestToExecuteChanges(REQUEST_USER_MODIFY_WEAK_STRONG_MAPPING_COMMUNICATION_PROBLEM, USER_DONALD_OID, UserType.class, task, null, parentResult);

		johnAccountType = checkPostponedAccountBasic(accountOid, FailedOperationTypeType.MODIFY, true, parentResult);
		ObjectDelta deltaInAccount = DeltaConvertor.createObjectDelta(johnAccountType.getObjectChange(), prismContext);
		assertTrue("Delta stored in account must contain given name modification", deltaInAccount.hasItemDelta(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceTypeOpenDjrepo.getNamespace(), "givenName"))));
		assertFalse("Delta stored in account must not contain employeeType modification", deltaInAccount.hasItemDelta(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceTypeOpenDjrepo.getNamespace(), "employeeType"))));
		assertNotNull("Donald's account must contain reference on the resource", johnAccountType.getResourceRef());
	
		//TODO: check on user if it was processed successfully (add this check also to previous (30) test..
	}
	
	@Test
	public void test283GetObjectNoFetchShadowAndRecompute() throws Exception {
		final String TEST_NAME = "test283GetObjectNoFetchShadowAndRecompute";
		TestUtil.displayTestTile(TEST_NAME);
		
		// GIVEN
		openDJController.assumeRunning();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		String accountOid = assertUserOneAccountRef(USER_DONALD_OID);
				
		Task task = taskManager.createTaskInstance();
		
		// Get user's account with noFetch option - changes shouldn't be applied, bud should be still saved in shadow
		PrismObject<ShadowType> johnAccount = modelService.getObject(ShadowType.class, accountOid, GetOperationOptions.createNoFetchCollection(), task, parentResult);
		
		ShadowType johnAccountType = checkPostponedAccountBasic(johnAccount, FailedOperationTypeType.MODIFY, true, parentResult);
		ObjectDelta deltaInAccount = DeltaConvertor.createObjectDelta(johnAccountType.getObjectChange(), prismContext);
		assertTrue("Delta stored in account must contain given name modification", deltaInAccount.hasItemDelta(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceTypeOpenDjrepo.getNamespace(), "givenName"))));
		assertFalse("Delta stored in account must not contain employeeType modification", deltaInAccount.hasItemDelta(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceTypeOpenDjrepo.getNamespace(), "employeeType"))));
		assertNotNull("Donald's account must contain reference on the resource", johnAccountType.getResourceRef());
		
		
		//THEN recompute the user - postponed changes should be applied
		LOGGER.info("recompute user - account with weak mapping after stopping opendj.");
		ObjectDelta<UserType> emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_DONALD_OID, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(emptyDelta);
		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);
		
		
		accountOid = assertUserOneAccountRef(USER_DONALD_OID);
		johnAccountType = checkNormalizedShadowWithAttributes(accountOid, "donald", "don", "trump", "donald trump", false, task, parentResult);
		
		//TODO: check on user if it was processed successfully (add this check also to previous (30) test..
	}
	
	@Test
	public void test284ModifyObjectAssignToGroupCommunicationProblem() throws Exception {
		final String TEST_NAME = "test284ModifyObjectAssignToGroupCommunicationProblem";
		TestUtil.displayTestTile(TEST_NAME);
		Task task = taskManager.createTaskInstance();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		// GIVEN
		openDJController.addEntriesFromLdifFile(LDIF_CREATE_ADMINS_GROUP_FILENAME);
		ObjectQuery filter = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_GROUP_OBJECTCLASS, prismContext);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, filter, null, task, parentResult);
		
		for (PrismObject<ShadowType> shadow : shadows) {
			LOGGER.info("SHADOW ===> {}", shadow.debugDump() );
		}
		
		// WHEN
		openDJController.assumeStopped();
		
		
		String accountOid = assertUserOneAccountRef(USER_DONALD_OID);
				
		
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ROLE_ADMINS, USER_DONALD_OID, UserType.class, task, null, parentResult);
		
	
		// Get user's account with noFetch option - changes shouldn't be applied, bud should be still saved in shadow
		PrismObject<ShadowType> johnAccount = modelService.getObject(ShadowType.class, accountOid, GetOperationOptions.createNoFetchCollection(), task, parentResult);
		
		ShadowType johnAccountType = checkPostponedAccountBasic(johnAccount, FailedOperationTypeType.MODIFY, true, parentResult);
		ObjectDelta deltaInAccount = DeltaConvertor.createObjectDelta(johnAccountType.getObjectChange(), prismContext);
		assertTrue("Delta stored in account must contain association modification", deltaInAccount.hasItemDelta(new ItemPath(ShadowType.F_ASSOCIATION)));
//		assertFalse("Delta stored in account must not contain employeeType modification", deltaInAccount.hasItemDelta(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceTypeOpenDjrepo.getNamespace(), "employeeType"))));
		assertNotNull("Donald's account must contain reference on the resource", johnAccountType.getResourceRef());
		
		
		//THEN recompute the user - postponed changes should be applied
		openDJController.assumeRunning();
		LOGGER.info("recompute user - account with weak mapping after stopping opendj.");
		ObjectDelta<UserType> emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, USER_DONALD_OID, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(emptyDelta);
		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);
		
		
		accountOid = assertUserOneAccountRef(USER_DONALD_OID);
		johnAccountType = checkNormalizedShadowWithAttributes(accountOid, "donald", "donalld", "trump", "donald trump", false, task, parentResult);
	
		openDJController.assertUniqueMember("cn=admins,ou=groups,dc=example,dc=com", "uid=donald,ou=people,dc=example,dc=com");
		//TODO: check on user if it was processed successfully (add this check also to previous (30) test..
	}
	
	

	//TODO: enable after notify failure will be implemented..
	@Test(enabled = false)
	public void test400GetDiscoveryAddCommunicationProblemAlreadyExists() throws Exception{
		final String TEST_NAME = "test400GetDiscoveryAddCommunicationProblemAlreadyExists";
		TestUtil.displayTestTile(TEST_NAME);

		// GIVEN
		openDJController.assumeStopped();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		repoAddObjectFromFile(USER_DISCOVERY_FILENAME, parentResult);

		assertUserNoAccountRef(USER_DISCOVERY_OID, parentResult);
				
		Task task = taskManager.createTaskInstance();
		
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_DISCOVERY_OID, UserType.class, task, null, parentResult);
		
		parentResult.computeStatus();
		display("add object communication problem result: ", parentResult);
		assertEquals("Expected success but got: " + parentResult.getStatus(), OperationResultStatus.HANDLED_ERROR, parentResult.getStatus());
		
		String accountOid = assertUserOneAccountRef(USER_DISCOVERY_OID);
		
		openDJController.start();
		assertTrue(EmbeddedUtils.isRunning());
		
		Entry entry = openDJController.addEntryFromLdifFile(LDIF_DISCOVERY_FILENAME);
		Entry searchResult = openDJController.searchByUid("discovery");
		OpenDJController.assertAttribute(searchResult, "l", "Caribbean");
		OpenDJController.assertAttribute(searchResult, "givenName", "discovery");
		OpenDJController.assertAttribute(searchResult, "sn", "discovery");
		OpenDJController.assertAttribute(searchResult, "cn", "discovery");
		OpenDJController.assertAttribute(searchResult, "mail", "discovery@deep.in.the.caribbean.com");
		String dn = searchResult.getDN().toString();
		assertEquals("DN attribute " + dn + " not equals", dn, "uid=discovery,ou=people,dc=example,dc=com");
		
		modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);

		modelService.getObject(ShadowType.class, accountOid, null, task, parentResult);
		
	}
	
	/**
	 * Adding a user (morgan) that has an OpenDJ assignment. But the equivalent account already exists on
	 * OpenDJ. The account should be linked.
	 */	
	@Test
    public void test500AddUserMorganWithAssignment() throws Exception {
		final String TEST_NAME = "test500AddUserMorganWithAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        openDJController.assumeRunning();        
        Task task = taskManager.createTaskInstance(ConsistencyTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        Entry entry = openDJController.addEntryFromLdifFile(LDIF_MORGAN_FILENAME);
        display("Entry from LDIF", entry);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_MORGAN_FILENAME));
        display("Adding user", user);
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
//		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
        
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
		display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertShadowRepo(accountShadow, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        
        // TODO: check OpenDJ Account        
	}
	
	/**
	 * Adding a user (morgan) that has an OpenDJ assignment. But the equivalent account already exists on
	 * OpenDJ and there is also corresponding shadow in the repo. The account should be linked.
	 */	
	@Test
    public void test501AddUserChuckWithAssignment() throws Exception {
		final String TEST_NAME = "test501AddUserChuckWithAssignment";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN	
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(ConsistencyTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
//        Entry entry = openDJController.addEntryFromLdifFile(LDIF_MORGAN_FILENAME);
//        display("Entry from LDIF", entry);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_CHUCK_FILENAME));
        String accOid = provisioningService.addObject(account, null, null, task, result);
//        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_CHUCK_FILENAME));
        display("Adding user", user);
        ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
//		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
        
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_CHUCK_OID, null, task, result);
		display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        assertEquals("old oid not used..", accOid, accountOid);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertShadowRepo(accountShadow, accountOid, "uid=chuck,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "uid=chuck,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        ShadowType accountTypeModel = accountModel.asObjectable();
        
        assertAttribute(accountTypeModel, "uid", "chuck");
		assertAttribute(accountTypeModel, "givenName", "Chuck");
		assertAttribute(accountTypeModel, "sn", "Norris");
		assertAttribute(accountTypeModel, "cn", "Chuck Norris");
		
        // TODO: check OpenDJ Account        
	}

	
	/**
	 * Assigning accoun to user, account with the same identifier exist on the resource and there is also shadow in the repository. The account should be linked.
	 */	
	@Test
    public void test502AssignAccountToHerman() throws Exception {
		final String TEST_NAME = "test502AssignAccountToHerman";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN	
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(ConsistencyTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
//        Entry entry = openDJController.addEntryFromLdifFile(LDIF_MORGAN_FILENAME);
//        display("Entry from LDIF", entry);
        
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_HERMAN_FILENAME));
        String accOid = provisioningService.addObject(account, null, null, task, result);
//        
        repoAddObjectFromFile(USER_HERMAN_FILENAME, result);
        
        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_HERMAN_FILENAME));
        display("Adding user", user);
        
        //REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
        requestToExecuteChanges(REQUEST_USER_MODIFY_ASSIGN_ACCOUNT, USER_HERMAN_OID, UserType.class, task, null, result);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
//		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
        
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_HERMAN_OID, null, task, result);
		display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        assertEquals("old oid not used..", accOid, accountOid);
        assertEquals("old oid not used..", ACCOUNT_HERMAN_OID, accountOid);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertShadowRepo(accountShadow, accountOid, "uid=ht,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "uid=ht,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        ShadowType accountTypeModel = accountModel.asObjectable();
        
        // Check account's attributes
        assertAttributes(accountTypeModel, "ht", "Herman", "Toothrot", "Herman Toothrot");
		
        // TODO: check OpenDJ Account        
	}
	
	/**
	 *  Unlink account morgan, delete shadow and remove assignmnet from user morgan - preparation for the next test
	 */	
	@Test
    public void test510UnlinkAndUnassignAccountMorgan() throws Exception {
		final String TEST_NAME = "test510UnlinkAndUnassignAccountMorgan";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(ConsistencyTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_MORGAN_OID, null, result);
        display("User Morgan: ", user);
        List<PrismReferenceValue> linkRefs = user.findReference(UserType.F_LINK_REF).getValues();
        assertEquals("Unexpected number of link refs", 1, linkRefs.size());
        PrismReferenceValue linkRef = linkRefs.iterator().next();
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteReference(UserType.class,
				USER_MORGAN_OID, UserType.F_LINK_REF, prismContext, linkRef.clone());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		///----user's link is removed, now, remove assignment
		userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class,
				USER_MORGAN_OID, UserType.F_ASSIGNMENT, prismContext, user.findContainer(UserType.F_ASSIGNMENT).getValue().clone());
        deltas = MiscSchemaUtil.createCollection(userDelta);
     // WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		repositoryService.deleteObject(ShadowType.class, linkRef.getOid(), result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
//		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
        
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
		display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 0, userMorganType.getLinkRef().size());
        
		// Check shadow
        String accountOid = linkRef.getOid();
        try {
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo);
        fail("Unexpected shadow in repo. Shadow mut not exist");
        } catch (ObjectNotFoundException ex){
        	//this is expected..shadow must not exist in repo
        }
        // Check account
        try {
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo);
        fail("Unexpected shadow in repo. Shadow mut not exist");
        } catch (ObjectNotFoundException ex){
        	//this is expected..shadow must not exist in repo
        }
        // TODO: check OpenDJ Account        
	}
	
	
	/**
	 * assign account to the user morgan. Account with the same 'uid' (not dn, nut other secondary identifier already exists)
	 * account should be linked to the user.
	 * @throws Exception
	 */
	@Test
    public void test511AssignAccountMorgan() throws Exception {
		final String TEST_NAME = "test511AssignAccountMorgan";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(ConsistencyTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        
        //prepare new OU in opendj
        Entry entry = openDJController.addEntryFromLdifFile(LDIF_CREATE_USERS_OU_FILENAME);
        
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_MORGAN_OID, null, result);
        display("User Morgan: ", user);
        PrismReference linkRef = user.findReference(UserType.F_LINK_REF);
        
        ExpressionType expression = new ExpressionType();
        ObjectFactory of = new ObjectFactory();
        RawType raw = new RawType(new PrimitiveXNode("uid=morgan,ou=users,dc=example,dc=com"), prismContext);       
       
        JAXBElement val = of.createValue(raw);
        expression.getExpressionEvaluator().add(val);
        
        MappingType mapping = new MappingType();
        mapping.setExpression(expression);
        
        ResourceAttributeDefinitionType attrDefType = new ResourceAttributeDefinitionType();
        attrDefType.setRef(new ItemPathType(new ItemPath(getOpenDjSecondaryIdentifierQName())));
        attrDefType.setOutbound(mapping);
        
        ConstructionType construction = new ConstructionType();
        construction.getAttribute().add(attrDefType);
        construction.setResourceRef(ObjectTypeUtil.createObjectRef(resourceTypeOpenDjrepo));
        
        AssignmentType assignment = new AssignmentType();
        assignment.setConstruction(construction);
        
        ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddContainer(UserType.class, USER_MORGAN_OID, UserType.F_ASSIGNMENT, prismContext, assignment.asPrismContainerValue());
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
//		assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());
        
		PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
		display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        String accountOid = userMorganType.getLinkRef().iterator().next().getOid();
        
		// Check shadow
        
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertShadowRepo(accountShadow, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        ResourceAttribute attributes = ShadowUtil.getAttribute(accountModel, new QName(resourceTypeOpenDjrepo.getNamespace(), "uid"));
        assertEquals("morgan", attributes.getAnyRealValue());
        // TODO: check OpenDJ Account        
	}

	
	@Test
	public void test600DeleteUserAlice() throws Exception {
		String TEST_NAME = "test600DeleteUserAlice";
		TestUtil.displayTestTile(TEST_NAME);
		
		openDJController.assumeRunning();
		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = task.getResult();
		
		ObjectDelta<UserType> deleteAliceDelta = ObjectDelta.createDeleteDelta(UserType.class, USER_ALICE_OID, prismContext);
		
		modelService.executeChanges(MiscSchemaUtil.createCollection(deleteAliceDelta), null, task, parentResult);
		
		try {
			modelService.getObject(UserType.class, USER_ALICE_OID, null, task, parentResult);
			fail("Expected object not found error, but haven't got one. Something went wrong while deleting user alice");
		} catch (ObjectNotFoundException ex){
			//this is expected
			
		}
		
	}
	
	@Test
	public void test601GetDiscoveryModifyCommunicationProblemDirectAccount() throws Exception {
		String TEST_NAME = "test601GetDiscoveryModifyCommunicationProblemDirectAccount";
		TestUtil.displayTestTile(TEST_NAME);
		
		openDJController.assumeRunning();
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		//prepare user 
		repoAddObjectFromFile(USER_ALICE_FILENAME, parentResult);
		
		assertUserNoAccountRef(USER_ALICE_OID, parentResult);

		Task task = taskManager.createTaskInstance();
		//and add account to the user while resource is UP
		
		//REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
		requestToExecuteChanges(REQUEST_USER_MODIFY_ADD_ACCOUNT_DIRECTLY, USER_ALICE_OID, UserType.class, task, null, parentResult);
		
		//then stop openDJ
		openDJController.stop();
		
		String accountOid = assertUserOneAccountRef(USER_ALICE_OID);

		//and make some modifications to the account while resource is DOWN
		requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_COMMUNICATION_PROBLEM, accountOid, ShadowType.class, task, null, parentResult);

		//check the state after execution
		checkPostponedAccountBasic(accountOid, FailedOperationTypeType.MODIFY, true, parentResult);

		//start openDJ
		openDJController.start();
		//and set the resource availability status to UP
//		modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);
		ObjectDelta<UserType> emptyAliceDelta = ObjectDelta.createEmptyDelta(UserType.class, USER_ALICE_OID, prismContext, ChangeType.MODIFY);
		modelService.executeChanges(MiscSchemaUtil.createCollection(emptyAliceDelta), ModelExecuteOptions.createReconcile(), task, parentResult);
		accountOid = assertUserOneAccountRef(USER_ALICE_OID);
		
		//and then try to get account -> result is that the modifications will be applied to the account
//		ShadowType aliceAccount = checkNormalizedShadowWithAttributes(accountOid, "alice", "Jackkk", "alice", "alice", true, task, parentResult);
//		assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

		//and finally stop openDJ
//		openDJController.stop();
	}	
	
	// This should run last. It starts a task that may interfere with other tests
	@Test
	public void test800Reconciliation() throws Exception {
		final String TEST_NAME = "test800Reconciliation";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        openDJController.assumeRunning();

		final OperationResult result = new OperationResult(ConsistencyTest.class.getName() + "." + TEST_NAME);

		// TODO: remove this if the previous test is enabled
//		openDJController.start();
		
		// rename eobject dirrectly on resource before the recon start ..it tests the rename + recon situation (MID-1594)
		
		// precondition
		assertTrue(EmbeddedUtils.isRunning());
		UserType userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result).asObjectable();
		display("Jack before", userJack);
		
		
		LOGGER.info("start running task");
		// WHEN
		repoAddObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILENAME, result);
		verbose = true;
		long started = System.currentTimeMillis();
		waitForTaskNextRunAssertSuccess(TASK_OPENDJ_RECONCILIATION_OID, false, 120000);
		LOGGER.info("Reconciliation task run took {} seconds", (System.currentTimeMillis()-started)/1000L);

		// THEN
		
		// STOP the task. We don't need it any more. Even if it's non-recurrent its safer to delete it
		taskManager.deleteTask(TASK_OPENDJ_RECONCILIATION_OID, result);

		// check if the account was added after reconciliation
		UserType userE = repositoryService.getObject(UserType.class, USER_E_OID, null, result).asObjectable();
		String accountOid = assertUserOneAccountRef(USER_E_OID);
		
		ShadowType eAccount = checkNormalizedShadowWithAttributes(accountOid, "e", "Jackkk", "e", "e", true, null, result);
		assertAttribute(eAccount, "employeeNumber", "emp4321");
		ResourceAttributeContainer attributeContainer = ShadowUtil
				.getAttributesContainer(eAccount);
		Collection<ResourceAttribute<?>> identifiers = attributeContainer.getPrimaryIdentifiers();
		assertNotNull(identifiers);
		assertFalse(identifiers.isEmpty());
		assertEquals(1, identifiers.size());
		
		
		// check if the account was modified during reconciliation process
		String jackAccountOid = assertUserOneAccountRef(USER_JACK_OID);
		ShadowType modifiedAccount = checkNormalizedShadowBasic(jackAccountOid, "jack", true, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null, result);
		assertAttribute(modifiedAccount, "givenName", "Jackkk");
		assertAttribute(modifiedAccount, "employeeNumber", "emp4321");

		
		// check if the account was deleted during the reconciliation process
		try {
			modelService.getObject(ShadowType.class, ACCOUNT_DENIELS_OID, null, null, result);
			fail("Expected ObjectNotFoundException but haven't got one.");
		} catch (Exception ex) {
			if (!(ex instanceof ObjectNotFoundException)) {
				fail("Expected ObjectNotFoundException but got " + ex);
			}

		}
		
		String elaineAccountOid = assertUserOneAccountRef(USER_ELAINE_OID);
		modifiedAccount = checkNormalizedShadowBasic(elaineAccountOid, "elaine", true, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null, result);
		assertAttribute(modifiedAccount, getOpenDjSecondaryIdentifierQName(), "uid=elaine,ou=people,dc=example,dc=com");

		accountOid = assertUserOneAccountRef(USER_JACK2_OID);
		ShadowType jack2Shadow = checkNormalizedShadowBasic(accountOid, "jack2", true, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null, result);
		assertAttribute(jack2Shadow, "givenName", "jackNew2a");
		assertAttribute(jack2Shadow, "cn", "jackNew2a");

	}
	
	@Test
	public void test801TestReconciliationRename() throws Exception{
		final String TEST_NAME = "test801TestReconciliationRename";
        TestUtil.displayTestTile(this, TEST_NAME);

        openDJController.assumeRunning();
		final OperationResult result = new OperationResult(ConsistencyTest.class.getName() + "." + TEST_NAME);

		LOGGER.info("starting rename");
		
		openDJController.executeRenameChange(LDIF_MODIFY_RENAME_FILENAME);
		LOGGER.info("rename ended");
//		Entry res = openDJController.searchByUid("e");
//		LOGGER.info("E OBJECT AFTER RENAME " + res.toString());
		
		LOGGER.info("start running task");
		// WHEN
		long started = System.currentTimeMillis();
		repoAddObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILENAME, result);
		waitForTaskFinish(TASK_OPENDJ_RECONCILIATION_OID, false, 120000);
		LOGGER.info("Reconciliation task run took {} seconds", (System.currentTimeMillis()-started)/1000L);

		// THEN

		// STOP the task. We don't need it any more. Even if it's non-recurrent its safer to delete it
		taskManager.deleteTask(TASK_OPENDJ_RECONCILIATION_OID, result);

		// check if the account was added after reconciliation
		UserType userE = repositoryService.getObject(UserType.class, USER_E_OID, null, result).asObjectable();
		String accountOid = assertUserOneAccountRef(USER_E_OID);
		
		ShadowType eAccount = checkNormalizedShadowWithAttributes(accountOid, "e123", "Jackkk", "e", "e", true, null, result);
		assertAttribute(eAccount, "employeeNumber", "emp4321");
		ResourceAttributeContainer attributeContainer = ShadowUtil
				.getAttributesContainer(eAccount);
		Collection<ResourceAttribute<?>> identifiers = attributeContainer.getPrimaryIdentifiers();
		assertNotNull(identifiers);
		assertFalse(identifiers.isEmpty());
		assertEquals(1, identifiers.size());

		
		ResourceAttribute icfNameAttr = attributeContainer.findAttribute(getOpenDjSecondaryIdentifierQName());
		assertEquals("Wrong secondary indetifier.", "uid=e123,ou=people,dc=example,dc=com", icfNameAttr.getRealValue());
		
		assertEquals("Wrong shadow name. ", "uid=e123,ou=people,dc=example,dc=com", eAccount.getName().getOrig());
		
		PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
		
		provisioningService.applyDefinition(repoShadow, result);
		
		ResourceAttributeContainer repoAttributeContainer = ShadowUtil.getAttributesContainer(repoShadow);
		ResourceAttribute repoIcfNameAttr = repoAttributeContainer.findAttribute(getOpenDjSecondaryIdentifierQName());
		assertEquals("Wrong secondary indetifier.", "uid=e123,ou=people,dc=example,dc=com", repoIcfNameAttr.getRealValue());
		
		assertEquals("Wrong shadow name. ", "uid=e123,ou=people,dc=example,dc=com", repoShadow.asObjectable().getName().getOrig());
		
	}
	
	@Test
	public void test999Shutdown() throws Exception {
		taskManager.shutdown();
		waitFor("waiting for task manager shutdown", new Checker() {
			@Override
			public boolean check() throws CommonException {
				try {
					return taskManager.getLocallyRunningTasks(new OperationResult("dummy")).isEmpty();
				} catch (TaskManagerException e) {
					throw new SystemException(e);
				}
			}

			@Override
			public void timeout() {
				// No reaction, the test will fail right after return from this
			}
		}, 10000);
		AssertJUnit.assertEquals("Some tasks left running after shutdown", new HashSet<Task>(),
				taskManager.getLocallyRunningTasks(new OperationResult("dummy")));
	}
	
	
	private void checkRepoOpenDjResource() throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(ConsistencyTest.class.getName()
				+ ".checkRepoOpenDjResource");
		PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class,
				RESOURCE_OPENDJ_OID, null, result);
		checkOpenDjResource(resource.asObjectable(), "repository");
	}
	
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
		ResourceSchema schema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		ObjectClassComplexTypeDefinition accountDefinition = schema.findObjectClassDefinition(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
		assertNotNull("Schema does not define any account (resource from " + source + ")", accountDefinition);
		Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getPrimaryIdentifiers();
		assertFalse("No account identifiers (resource from " + source + ")", identifiers == null
				|| identifiers.isEmpty());
		// TODO: check for naming attributes and display names, etc

		ActivationCapabilityType capActivation = ResourceTypeUtil.getEffectiveCapability(resource,
				ActivationCapabilityType.class);
		if (capActivation != null && capActivation.getStatus() != null && capActivation.getStatus().getAttribute() != null) {
            // There is simulated activation capability, check if the attribute is in schema.
            QName enableAttrName = capActivation.getStatus().getAttribute();
            ResourceAttributeDefinition enableAttrDef = accountDefinition.findAttributeDefinition(enableAttrName);
            display("Simulated activation attribute definition", enableAttrDef);
            assertNotNull("No definition for enable attribute " + enableAttrName + " in account (resource from " + source + ")", enableAttrDef);
            assertTrue("Enable attribute " + enableAttrName + " is not ignored (resource from " + source + ")", enableAttrDef.isIgnored());
        }
	}

	private void checkOpenDjConfiguration(PrismObject<ResourceType> resource, String source) {
		checkOpenResourceConfiguration(resource, CONNECTOR_LDAP_NAMESPACE, "bindPassword", 8, source);
	}

	private void checkOpenResourceConfiguration(PrismObject<ResourceType> resource,
			String connectorNamespace, String credentialsPropertyName, int numConfigProps, String source) {
		PrismContainer<Containerable> configurationContainer = resource
				.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container in " + resource + " from " + source, configurationContainer);
		PrismContainer<Containerable> configPropsContainer = configurationContainer
				.findContainer(SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No configuration properties container in " + resource + " from " + source,
				configPropsContainer);
		List<? extends Item<?,?>> configProps = configPropsContainer.getValue().getItems();
		assertEquals("Wrong number of config properties in " + resource + " from " + source, numConfigProps,
				configProps.size());
		PrismProperty<Object> credentialsProp = configPropsContainer.findProperty(new QName(
				connectorNamespace, credentialsPropertyName));
		if (credentialsProp == null) {
			// The is the heisenbug we are looking for. Just dump the entire
			// damn thing.
			display("Configuration with the heisenbug", configurationContainer.debugDump());
		}
		assertNotNull("No credentials property in " + resource + " from " + source, credentialsProp);
		assertEquals("Wrong number of credentials property value in " + resource + " from " + source, 1,
				credentialsProp.getValues().size());
		PrismPropertyValue<Object> credentialsPropertyValue = credentialsProp.getValues().iterator().next();
		assertNotNull("No credentials property value in " + resource + " from " + source,
				credentialsPropertyValue);
		Object rawElement = credentialsPropertyValue.getRawElement();
		}

	private UserType testAddUserToRepo(String displayMessage, String fileName, String userOid)
            throws IOException, ObjectNotFoundException, SchemaException, EncryptionException,
            ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException, SecurityViolationException {

		checkRepoOpenDjResource();
		assertNoRepoCache();

		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(fileName));
		UserType userType = user.asObjectable();
		PrismAsserts.assertParentConsistency(user);

		protector.encrypt(userType.getCredentials().getPassword().getValue());
		PrismAsserts.assertParentConsistency(user);

		OperationResult result = new OperationResult("add user");
	
		display("Adding user object", userType);

		Task task = taskManager.createTaskInstance();
		// WHEN
		ObjectDelta delta = ObjectDelta.createAddDelta(user);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
		modelService.executeChanges(deltas, null, task, result);
		// THEN

		assertNoRepoCache();
	
		return userType;
	}


	private String assertUserOneAccountRef(String userOid) throws Exception{
		OperationResult parentResult = new OperationResult("getObject from repo");
		
		PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, userOid,
				null, parentResult);
		UserType repoUserType = repoUser.asObjectable();

		parentResult.computeStatus();
		TestUtil.assertSuccess("getObject has failed", parentResult);
		
		return assertOneAccountRef(repoUser);
	}

	private String assertOneAccountRef(PrismObject<UserType> user) throws Exception{

		UserType repoUserType = user.asObjectable();
		display("User (repository)", user);

		List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
		assertEquals("No accountRefs", 1, accountRefs.size());
		ObjectReferenceType accountRef = accountRefs.get(0);
		
		return accountRef.getOid();
	}

	private void assertUserNoAccountRef(String userOid, OperationResult parentResult) throws Exception{
		PrismObject<UserType> user = repositoryService
				.getObject(UserType.class, userOid, null, parentResult);
		assertEquals(0, user.asObjectable().getLinkRef().size());
	}
	
	private String checkRepoShadow(PrismObject<ShadowType> repoShadow) {
		ShadowType repoShadowType = repoShadow.asObjectable();
		String uid = null;
        boolean hasOthers = false;
        List<Object> xmlAttributes = repoShadowType.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (getOpenDjPrimaryIdentifierQName().equals(JAXBUtil.getElementQName(element)) || getOpenDjPrimaryIdentifierQName().equals(JAXBUtil.getElementQName(element))) {
                if (uid != null) {
                    AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
                } else {
                    uid = ((Element) element).getTextContent();
                }
            } else if (SchemaConstants.ICFS_NAME.equals(JAXBUtil.getElementQName(element)) || getOpenDjSecondaryIdentifierQName().equals(JAXBUtil.getElementQName(element))) {
            	// This is OK
        	} else {
                hasOthers = true;
            }
        }

        assertFalse("Shadow "+repoShadow+" has unexpected elements", hasOthers);
        assertNotNull(uid);
        
        return uid;
	}
    
    private QName getOpenDjPrimaryIdentifierQName() {
    	return new QName(RESOURCE_OPENDJ_NS, RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME);
	}

	private QName getOpenDjSecondaryIdentifierQName() {
		return new QName(RESOURCE_OPENDJ_NS, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME);
	}
	
	private String checkUser(String userOid, Task task, OperationResult parentResult) throws Exception{
		PrismObject<UserType> user = modelService.getObject(UserType.class, userOid, null, task, parentResult);
		return checkUser(user);
	}
	
	private String checkRepoUser(String userOid, OperationResult parentResult) throws Exception{
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, parentResult);
		return checkUser(user);
	}
	
	private String checkUser(PrismObject<UserType> user){
		assertNotNull("User must not be null", user);
		UserType userType = user.asObjectable();
		assertEquals("User must have one link ref, ", 1, userType.getLinkRef().size());
		MidPointAsserts.assertAssignments(user, 1);
		
		String accountOid = userType.getLinkRef().get(0).getOid();
		
		return accountOid;
	}
	
	private void checkAccount(String accountOid, String uid, String givenName, String sn, String cn, Task task, OperationResult parentResult) throws Exception{
		PrismObject<ShadowType> newAccount = modelService.getObject(ShadowType.class, accountOid, null, task, parentResult);
		assertNotNull("Shadow must not be null", newAccount);
		ShadowType createdShadow = newAccount.asObjectable();
		display("Created account: ", createdShadow);

		AssertJUnit.assertNotNull(createdShadow);
		AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, createdShadow.getResourceRef().getOid());
		assertAttributeNotNull(createdShadow, getOpenDjPrimaryIdentifierQName());
		
		assertAttributes(createdShadow, uid, givenName, sn, cn);
		
	}
		
	private void assertAttributes(ShadowType shadow, String uid, String givenName, String sn, String cn){
		assertAttribute(shadow, "uid", uid);
		if (givenName != null) {
			assertAttribute(shadow, "givenName", givenName);
		}
		if (sn != null) {
			assertAttribute(shadow, "sn", sn);
		}
		assertAttribute(shadow, "cn", cn);
	}
	
	
	private void checkPostponedAccountWithAttributes(String accountOid, String uid, String givenName, String sn, String cn, String employeeNumber, FailedOperationTypeType failedOperation, boolean modify, Task task, OperationResult parentResult) throws Exception{
		ShadowType account = checkPostponedAccountWithAttributes(accountOid, uid, givenName, sn, cn, failedOperation, modify, task, parentResult);
		display("account shadow (postponed operation)", account);
		assertAttribute(account, "employeeNumber", employeeNumber);
	}
	
	private ShadowType checkPostponedAccountWithAttributes(String accountOid, String uid, String givenName, String sn, String cn, FailedOperationTypeType failedOperation, boolean modify, Task task, OperationResult parentResult) throws Exception{
		ShadowType failedAccountType = checkPostponedAccountBasic(accountOid, failedOperation, modify, parentResult);
		
		// assertNull(ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		assertAttributes(failedAccountType, uid, givenName, sn, cn);
		return failedAccountType;
	}
	
	private ShadowType checkPostponedAccountBasic(PrismObject<ShadowType> failedAccount, FailedOperationTypeType failedOperation, boolean modify, OperationResult parentResult) throws Exception{
		display("Repository shadow (postponed operation expected)", failedAccount);
		assertNotNull("Shadow must not be null", failedAccount);
		ShadowType failedAccountType = failedAccount.asObjectable();
		assertNotNull(failedAccountType);
		// Too much noise
//		displayJaxb("shadow from the repository: ", failedAccountType, ShadowType.COMPLEX_TYPE);
		assertEquals("Failed operation saved with account differt from  the expected value.",
				failedOperation, failedAccountType.getFailedOperationType());
		assertNotNull("Result of failed shadow must not be null.", failedAccountType.getResult());
		assertNotNull("Shadow does not contain resource ref.", failedAccountType.getResourceRef());
		assertEquals("Wrong resource ref in shadow", resourceTypeOpenDjrepo.getOid(), failedAccountType.getResourceRef().getOid());
		if (modify){
			assertNotNull("Null object change in shadow", failedAccountType.getObjectChange());
		}
		return failedAccountType;
	}
	
	private ShadowType checkPostponedAccountBasic(String accountOid, FailedOperationTypeType failedOperation, boolean modify, OperationResult parentResult) throws Exception{
		PrismObject<ShadowType> faieldAccount = repositoryService.getObject(ShadowType.class, accountOid, null, parentResult);
		return checkPostponedAccountBasic(faieldAccount, failedOperation, modify, parentResult);
	}
	
	private void requestToExecuteChanges(String requestFilename, String objectOid,
			Class type, Task task, ModelExecuteOptions options, OperationResult parentResult)
			throws Exception {
		Collection<ObjectDelta<? extends ObjectType>> deltas = createDeltas(type, requestFilename, objectOid);
		display("Executing deltas", deltas);
		
		// WHEN
		modelService.executeChanges(deltas, options, task, parentResult);
	}
	
	private Collection<ObjectDelta<? extends ObjectType>> createDeltas(Class type, String requestFilename, String objectOid) throws IOException, SchemaException, JAXBException{
		
		try{
			ObjectDeltaType objectChange = unmarshallValueFromFile(
	                requestFilename, ObjectDeltaType.class);
			objectChange.setOid(objectOid);
	
			ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, prismContext);
	
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
	
			return deltas;
		} catch (Exception ex){
			LOGGER.error("ERROR while unmarshalling: {}", ex);
			throw ex;
		}
		
	}

	private void modifyResourceAvailabilityStatus(AvailabilityStatusType status, OperationResult parentResult) throws Exception {
		PropertyDelta resourceStatusDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(
				ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
				resourceTypeOpenDjrepo.asPrismObject().getDefinition(), status);
		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>();
		modifications.add(resourceStatusDelta);
		repositoryService.modifyObject(ResourceType.class, resourceTypeOpenDjrepo.getOid(), modifications, parentResult);
	}

	private void checkNormalizedShadowWithAttributes(String accountOid, String uid, String givenName, String sn, String cn, String employeeType, boolean modify, Task task, OperationResult parentResult) throws Exception{
		ShadowType resourceAccount = checkNormalizedShadowBasic(accountOid, uid, modify, null, task, parentResult);
		assertAttributes(resourceAccount, uid, givenName, sn, cn);
		assertAttribute(resourceAccount, "employeeType", employeeType);
	}
	
	private ShadowType checkNormalizedShadowWithAttributes(String accountOid, String uid, String givenName, String sn, String cn, boolean modify, Task task, OperationResult parentResult) throws Exception{
		ShadowType resourceAccount = checkNormalizedShadowBasic(accountOid, uid, modify, null, task, parentResult);
		assertAttributes(resourceAccount, uid, givenName, sn, cn);
		return resourceAccount;
	}
	
	private ShadowType checkNormalizedShadowBasic(String accountOid, String name, boolean modify, Collection<SelectorOptions<GetOperationOptions>> options,Task task, OperationResult parentResult) throws Exception{
		PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountOid, options, task, parentResult);
		assertNotNull(account);
		ShadowType accountType = account.asObjectable();
		display("Shadow after discovery", account);
		assertNull(name + "'s account after discovery must not have failed opertion.", accountType.getFailedOperationType());
		assertNull(name + "'s account after discovery must not have result.", accountType.getResult());
		assertNotNull(name + "'s account must contain reference on the resource", accountType.getResourceRef());
		assertEquals(resourceTypeOpenDjrepo.getOid(), accountType.getResourceRef().getOid());
		
		if (modify){
			assertNull(name + "'s account must not have object change", accountType.getObjectChange());
		}
		
		return accountType;
//		assertNotNull("Identifier in the angelica's account after discovery must not be null.",ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());
		
	}

	protected <T> void assertAttribute(ShadowType shadowType, String attrName,  T... expectedValues) {
		assertAttribute(resourceTypeOpenDjrepo, shadowType, attrName, expectedValues);
	}
    
    protected <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName,  T... expectedValues) {
		assertAttribute(resourceTypeOpenDjrepo, shadow.asObjectable(), attrName, expectedValues);
	}
    
    protected <T> void assertAttribute(ShadowType shadowType, QName attrName,  T... expectedValues) {
		assertAttribute(resourceTypeOpenDjrepo, shadowType, attrName, expectedValues);
	}

}
