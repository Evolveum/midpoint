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
package com.evolveum.midpoint.testing.sanity;

import static com.evolveum.midpoint.prism.util.PrismAsserts.assertEqualsPolyString;
import static com.evolveum.midpoint.prism.util.PrismAsserts.assertParentConsistency;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttribute;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertAttributeNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoRepoCache;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNotEmpty;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayJaxb;
import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValue;
import static com.evolveum.midpoint.test.IntegrationTestTools.getAttributeValues;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.task.api.TaskManagerException;
import com.evolveum.midpoint.util.exception.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.*;
import org.opends.server.types.ModificationType;
import org.opends.server.util.ChangeRecordEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.ObjectAlreadyExistsFaultType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.EncryptedDataType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Sanity test suite.
 * <p/>
 * It tests the very basic representative test cases. It does not try to be
 * complete. It rather should be quick to execute and pass through the most
 * representative cases. It should test all the system components except for
 * GUI. Therefore the test cases are selected to pass through most of the
 * components.
 * <p/>
 * It is using embedded H2 repository and embedded OpenDJ instance as a testing
 * resource. The repository is instantiated from the Spring context in the
 * same way as all other components. OpenDJ instance is started explicitly using
 * BeforeClass method. Appropriate resource definition to reach the OpenDJ
 * instance is provided in the test data and is inserted in the repository as
 * part of test initialization.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-sanity-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSanity extends AbstractModelIntegrationTest {

	private static final String REPO_DIR_NAME = "src/test/resources/repo/";
	private static final String REQUEST_DIR_NAME = "src/test/resources/request/";
	private static final File REQUEST_DIR = new File(REQUEST_DIR_NAME);

    private static final String SYSTEM_CONFIGURATION_FILENAME = REPO_DIR_NAME + "system-configuration.xml";
    private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

    private static final String ROLE_SUPERUSER_FILENAME = REPO_DIR_NAME + "role-superuser.xml";
    private static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    private static final String RESOURCE_OPENDJ_FILENAME = REPO_DIR_NAME + "resource-opendj.xml";
    private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
    private static final String RESOURCE_OPENDJ_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
    protected static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_OPENDJ_NS,"inetOrgPerson");
	private static final String RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME = "entryUUID";
	private static final String RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME = "dn";


    private static final String RESOURCE_DERBY_FILENAME = REPO_DIR_NAME + "resource-derby.xml";
    private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";

    private static final String RESOURCE_BROKEN_FILENAME = REPO_DIR_NAME + "resource-broken.xml";
    private static final String RESOURCE_BROKEN_OID = "ef2bc95b-76e0-59e2-ffff-ffffffffffff";

    private static final String RESOURCE_DUMMY_FILENAME = REPO_DIR_NAME + "resource-dummy.xml";
    private static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";

    private static final String CONNECTOR_LDAP_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-ldap/com.evolveum.polygon.connector.ldap.LdapConnector";
    private static final String CONNECTOR_DBTABLE_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-databasetable/org.identityconnectors.databasetable.DatabaseTableConnector";

    private static final String CONNECTOR_BROKEN_FILENAME = REPO_DIR_NAME + "connector-broken.xml";
    private static final String CONNECTOR_BROKEN_OID = "cccccccc-76e0-59e2-ffff-ffffffffffff";

    private static final String TASK_OPENDJ_SYNC_FILENAME = REPO_DIR_NAME + "task-opendj-sync.xml";
    private static final String TASK_OPENDJ_SYNC_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";

    private static final String TASK_USER_RECOMPUTE_FILENAME = REPO_DIR_NAME + "task-user-recompute.xml";
    private static final String TASK_USER_RECOMPUTE_OID = "91919191-76e0-59e2-86d6-3d4f02d3aaaa";

    private static final String TASK_OPENDJ_RECON_FILENAME = REPO_DIR_NAME + "task-opendj-reconciliation.xml";
    private static final String TASK_OPENDJ_RECON_OID = "91919191-76e0-59e2-86d6-3d4f02d30000";

    private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = REPO_DIR_NAME + "sample-configuration-object.xml";
    private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";

    private static final String USER_TEMPLATE_FILENAME = REPO_DIR_NAME + "user-template.xml";
    private static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

    private static final String USER_ADMINISTRATOR_FILENAME = REPO_DIR_NAME + "user-administrator.xml";
    private static final String USER_ADMINISTRATOR_NAME = "administrator";
    private static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

    private static final String USER_JACK_FILENAME = REPO_DIR_NAME + "user-jack.xml";
    private static final File USER_JACK_FILE = new File(USER_JACK_FILENAME);
    private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    private static final String USER_JACK_LDAP_UID = "jack";
    private static final String USER_JACK_LDAP_DN = "uid=" + USER_JACK_LDAP_UID
            + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final String USER_GUYBRUSH_FILENAME = REPO_DIR_NAME + "user-guybrush.xml";
    private static final File USER_GUYBRUSH_FILE = new File(USER_GUYBRUSH_FILENAME);
    private static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111222";
    private static final String USER_GUYBRUSH_USERNAME = "guybrush";
    private static final String USER_GUYBRUSH_LDAP_UID = "guybrush";
    private static final String USER_GUYBRUSH_LDAP_DN = "uid=" + USER_GUYBRUSH_LDAP_UID
            + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final String USER_E_LINK_ACTION_FILENAME = REPO_DIR_NAME + "user-e.xml";
    private static final File USER_E_LINK_ACTION_FILE = new File(USER_E_LINK_ACTION_FILENAME);
    private static final String LDIF_E_FILENAME_LINK = "src/test/resources/request/e-create.ldif";

    private static final String ROLE_PIRATE_FILENAME = REPO_DIR_NAME + "role-pirate.xml";
    private static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-987987987988";

    private static final String ROLE_SAILOR_FILENAME = REPO_DIR_NAME + "role-sailor.xml";
    private static final String ROLE_SAILOR_OID = "12345678-d34d-b33f-f00d-987955553535";

    private static final String ROLE_CAPTAIN_FILENAME = REPO_DIR_NAME + "role-captain.xml";
    private static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-987987cccccc";

    private static final String ROLE_JUDGE_FILENAME = REPO_DIR_NAME + "role-judge.xml";
    private static final String ROLE_JUDGE_OID = "12345111-1111-2222-1111-121212111111";

    private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME = REQUEST_DIR_NAME + "user-modify-add-account.xml";

    private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_DERBY_FILENAME = REQUEST_DIR_NAME + "user-modify-add-account-derby.xml";
    private static final String USER_JACK_DERBY_LOGIN = "jsparrow";

    private static final String REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME = REQUEST_DIR_NAME + "user-modify-fullname-locality.xml";
    private static final String REQUEST_USER_MODIFY_GIVENNAME_FILENAME = REQUEST_DIR_NAME + "user-modify-givenname.xml";
    private static final String REQUEST_USER_MODIFY_PASSWORD_FILENAME = REQUEST_DIR_NAME + "user-modify-password.xml";
    private static final String REQUEST_USER_MODIFY_ACTIVATION_DISABLE_FILENAME = REQUEST_DIR_NAME + "user-modify-activation-disable.xml";
    private static final String REQUEST_USER_MODIFY_ACTIVATION_ENABLE_FILENAME = REQUEST_DIR_NAME + "user-modify-activation-enable.xml";
    private static final String REQUEST_USER_MODIFY_NAME_FILENAME = REQUEST_DIR_NAME + "user-modify-name.xml";

    private static final String REQUEST_USER_MODIFY_ADD_ROLE_PIRATE_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-pirate.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_1_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-captain-1.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_2_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-captain-2.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_JUDGE_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-judge.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_PIRATE_FILENAME = REQUEST_DIR_NAME + "user-modify-delete-role-pirate.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_1_FILENAME = REQUEST_DIR_NAME + "user-modify-delete-role-captain-1.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_2_FILENAME = REQUEST_DIR_NAME + "user-modify-delete-role-captain-2.xml";

    private static final File REQUEST_ACCOUNT_MODIFY_ATTRS_FILE = new File(REQUEST_DIR, "account-modify-attrs.xml");
    private static final File REQUEST_ACCOUNT_MODIFY_ROOM_NUMBER_FILE = new File(REQUEST_DIR, "account-modify-roomnumber.xml");
    private static final File REQUEST_ACCOUNT_MODIFY_ROOM_NUMBER_EXPLICIT_TYPE_FILE = new File(REQUEST_DIR, "account-modify-roomnumber-explicit-type.xml");
    private static final File REQUEST_ACCOUNT_MODIFY_BAD_PATH_FILE = new File(REQUEST_DIR, "account-modify-bad-path.xml");

    private static final String LDIF_WILL_FILENAME = REQUEST_DIR_NAME + "will.ldif";
    private static final File LDIF_WILL_MODIFY_FILE = new File (REQUEST_DIR_NAME, "will-modify.ldif");
    private static final String LDIF_WILL_WITHOUT_LOCATION_FILENAME = REQUEST_DIR_NAME + "will-without-location.ldif";
    private static final String WILL_NAME = "wturner";

    private static final String LDIF_ANGELIKA_FILENAME = REQUEST_DIR_NAME + "angelika.ldif";
    private static final String ANGELIKA_NAME = "angelika";

    private static final String ACCOUNT_ANGELIKA_FILENAME = REQUEST_DIR_NAME + "account-angelika.xml";


    private static final String LDIF_ELAINE_FILENAME = REQUEST_DIR_NAME + "elaine.ldif";
    private static final String ELAINE_NAME = "elaine";

    private static final File LDIF_GIBBS_MODIFY_FILE = new File (REQUEST_DIR_NAME, "gibbs-modify.ldif");

    private static final String  LDIF_HERMAN_FILENAME = REQUEST_DIR_NAME + "herman.ldif";

    private static final Trace LOGGER = TraceManager.getTrace(TestSanity.class);

	private static final String NS_MY = "http://whatever.com/my";
	private static final QName MY_SHIP_STATE = new QName(NS_MY, "shipState");
	private static final QName MY_DEAD = new QName(NS_MY, "dead");

	private static final long WAIT_FOR_LOOP_SLEEP_MILIS = 1000;

    /**
     * Unmarshalled resource definition to reach the embedded OpenDJ instance.
     * Used for convenience - the tests method may find it handy.
     */
    private static ResourceType resourceTypeOpenDjrepo;
    private static ResourceType resourceDerby;
    private static String accountShadowOidOpendj;
    private static String accountShadowOidDerby;
    private static String accountShadowOidGuybrushOpendj;
    private static String accountGuybrushOpendjEntryUuuid = null;
    private static String originalJacksLdapPassword;
    private static String lastJacksLdapPassword = null;

    private int lastSyncToken;

    @Autowired(required = true)
    private MatchingRuleRegistry matchingRuleRegistry;

    // This will get called from the superclass to init the repository
    // It will be called only once
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        LOGGER.trace("initSystem");
        try{
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILENAME, initResult);
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

        // Add broken connector before importing resources
        repoAddObjectFromFile(CONNECTOR_BROKEN_FILENAME, initResult);

        // Need to import instead of add, so the (dynamic) connector reference
        // will be resolved
        // correctly
        importObjectFromFile(RESOURCE_OPENDJ_FILENAME, initResult);
        importObjectFromFile(RESOURCE_BROKEN_FILENAME, initResult);

        repoAddObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_FILENAME, initResult);
        repoAddObjectFromFile(ROLE_SAILOR_FILENAME, initResult);
        repoAddObjectFromFile(ROLE_PIRATE_FILENAME, initResult);
        repoAddObjectFromFile(ROLE_CAPTAIN_FILENAME, initResult);
        repoAddObjectFromFile(ROLE_JUDGE_FILENAME, initResult);
        } catch (Exception ex){
        	LOGGER.error("erro: {}", ex);
        	throw ex;
        }
    }

    /**
     * Initialize embedded OpenDJ instance Note: this is not in the abstract
     * superclass so individual tests may avoid starting OpenDJ.
     */
    @Override
    public void startResources() throws Exception {
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
    public void test000Integrity() throws Exception {
    	final String TEST_NAME = "test000Integrity";
        TestUtil.displayTestTitle(this, TEST_NAME);
        assertNotNull(modelWeb);
        assertNotNull(modelService);
        assertNotNull(repositoryService);
        assertTrue(isSystemInitialized());
        assertNotNull(taskManager);

        assertNotNull(prismContext);
        SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
        assertNotNull(schemaRegistry);
        // This is defined in extra schema. So this effectively checks whether the extra schema was loaded
        PrismPropertyDefinition shipStateDefinition = schemaRegistry.findPropertyDefinitionByElementName(MY_SHIP_STATE);
        assertNotNull("No my:shipState definition", shipStateDefinition);
        assertEquals("Wrong maxOccurs in my:shipState definition", 1, shipStateDefinition.getMaxOccurs());

        assertNoRepoCache();

        Task task = taskManager.createTaskInstance(TestSanity.class.getName() + ".test000Integrity");
        OperationResult result = task.getResult();

        // Check if OpenDJ resource was imported correctly

        PrismObject<ResourceType> openDjResource = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result);
        display("Imported OpenDJ resource (repository)", openDjResource);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());

        assertNoRepoCache();

        String ldapConnectorOid = openDjResource.asObjectable().getConnectorRef().getOid();
        PrismObject<ConnectorType> ldapConnector = repositoryService.getObject(ConnectorType.class, ldapConnectorOid, null, result);
        display("LDAP Connector: ", ldapConnector);

        // TODO: test if OpenDJ and Derby are running

        repositoryService.getObject(GenericObjectType.class, SAMPLE_CONFIGURATION_OBJECT_OID, null, result);
    }

    /**
     * Repeat self-test when we have all the dependencies on the classpath.
     */
    @Test
    public void test001SelfTests() throws Exception {
    	final String TEST_NAME = "test001SelfTests";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSanity.class.getName()+"."+TEST_NAME);

        // WHEN
        OperationResult repositorySelfTestResult = modelDiagnosticService.repositorySelfTest(task);

        // THEN
        assertSuccess("Repository self test", repositorySelfTestResult);

        // WHEN
        OperationResult provisioningSelfTestResult = modelDiagnosticService.provisioningSelfTest(task);

        // THEN
        display("Repository self test result", provisioningSelfTestResult);
        // There may be warning about illegal key size on some platforms. As far as it is warning and not error we are OK
        // the system will fall back to a interoperable key size
		if (provisioningSelfTestResult.getStatus() != OperationResultStatus.SUCCESS && provisioningSelfTestResult.getStatus() != OperationResultStatus.WARNING) {
			AssertJUnit.fail("Provisioning self-test failed: "+provisioningSelfTestResult);
		}
}


    /**
     * Test the testResource method. Expect a complete success for now.
     */
    @Test
    public void test001TestConnectionOpenDJ() throws Exception {
    	final String TEST_NAME = "test001TestConnectionOpenDJ";
        displayTestTitle(TEST_NAME);

        // GIVEN
        try{
        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.testResource(RESOURCE_OPENDJ_OID);

        // THEN

        assertNoRepoCache();

        displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

        TestUtil.assertSuccess("testResource has failed", result);

        OperationResult opResult = new OperationResult(TestSanity.class.getName() + ".test001TestConnectionOpenDJ");

        PrismObject<ResourceType> resourceOpenDjRepo = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult);
        resourceTypeOpenDjrepo = resourceOpenDjRepo.asObjectable();

        assertNoRepoCache();
        assertEquals(RESOURCE_OPENDJ_OID, resourceTypeOpenDjrepo.getOid());
        display("Initialized OpenDJ resource (respository)", resourceTypeOpenDjrepo);
        assertNotNull("Resource schema was not generated", resourceTypeOpenDjrepo.getSchema());
        Element resourceOpenDjXsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(resourceTypeOpenDjrepo);
        assertNotNull("Resource schema was not generated", resourceOpenDjXsdSchemaElement);

        PrismObject<ResourceType> openDjResourceProvisioninig = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID,
        		null, null, opResult);
        display("Initialized OpenDJ resource resource (provisioning)", openDjResourceProvisioninig);

        PrismObject<ResourceType> openDjResourceModel = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, null, opResult);
        display("Initialized OpenDJ resource OpenDJ resource (model)", openDjResourceModel);

        checkOpenDjResource(resourceTypeOpenDjrepo, "repository");

        System.out.println("------------------------------------------------------------------");
        display("OpenDJ resource schema (repo XML)", DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(resourceOpenDjRepo)));
        System.out.println("------------------------------------------------------------------");

        checkOpenDjResource(openDjResourceProvisioninig.asObjectable(), "provisioning");
        checkOpenDjResource(openDjResourceModel.asObjectable(), "model");
        // TODO: model web
        } catch (Exception ex){
        	LOGGER.info("exception: " + ex);
        	throw ex;
        }

    }

    private void checkRepoOpenDjResource() throws ObjectNotFoundException, SchemaException {
    	OperationResult result = new OperationResult(TestSanity.class.getName()+".checkRepoOpenDjResource");
    	PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, result);
    	checkOpenDjResource(resource.asObjectable(), "repository");
    }

	/**
     * Checks if the resource is internally consistent, if it has everything it should have.
     *
     * @throws SchemaException
     */
    private void checkOpenDjResource(ResourceType resource, String source) throws SchemaException {
        assertNotNull("Resource from " + source + " is null", resource);
        ObjectReferenceType connectorRefType = resource.getConnectorRef();
        assertNotNull("Resource from " + source + " has null connectorRef", connectorRefType);
        assertFalse("Resource from " + source + " has no OID in connectorRef", StringUtils.isBlank(connectorRefType.getOid()));
        assertNotNull("Resource from " + source + " has null description in connectorRef", connectorRefType.getDescription());
        assertNotNull("Resource from " + source + " has null filter in connectorRef", connectorRefType.getFilter());
        assertNotNull("Resource from " + source + " has null filter element in connectorRef", connectorRefType.getFilter().getFilterClauseXNode());
        assertNotNull("Resource from " + source + " has null configuration", resource.getConnectorConfiguration());
        assertNotNull("Resource from " + source + " has null schema", resource.getSchema());
        checkOpenDjSchema(resource, source);
        assertNotNull("Resource from " + source + " has null schemahandling", resource.getSchemaHandling());
        checkOpenDjSchemaHandling(resource, source);
        if (!source.equals("repository")) {
            // This is generated on the fly in provisioning
            assertNotNull("Resource from " + source + " has null nativeCapabilities", resource.getCapabilities().getNative());
            assertFalse("Resource from " + source + " has empty nativeCapabilities",
            		resource.getCapabilities().getNative().getAny().isEmpty());
        }
        assertNotNull("Resource from " + source + " has null configured capabilities", resource.getCapabilities().getConfigured());
        assertFalse("Resource from " + source + " has empty capabilities", resource.getCapabilities().getConfigured().getAny().isEmpty());
        assertNotNull("Resource from " + source + " has null synchronization", resource.getSynchronization());
        checkOpenDjConfiguration(resource.asPrismObject(), source);
    }

    private void checkOpenDjSchema(ResourceType resource, String source) throws SchemaException {
        ResourceSchema schema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
        ObjectClassComplexTypeDefinition accountDefinition = schema.findObjectClassDefinition(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        assertNotNull("Schema does not define any account (resource from " + source + ")", accountDefinition);
        Collection<? extends ResourceAttributeDefinition> identifiers = accountDefinition.getPrimaryIdentifiers();
        assertFalse("No account identifiers (resource from " + source + ")", identifiers == null || identifiers.isEmpty());
        // TODO: check for naming attributes and display names, etc

        ActivationCapabilityType capActivation = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        if (capActivation != null && capActivation.getStatus() != null && capActivation.getStatus().getAttribute() != null) {
            // There is simulated activation capability, check if the attribute is in schema.
            QName enableAttrName = capActivation.getStatus().getAttribute();
            ResourceAttributeDefinition enableAttrDef = accountDefinition.findAttributeDefinition(enableAttrName);
            display("Simulated activation attribute definition", enableAttrDef);
            assertNotNull("No definition for enable attribute " + enableAttrName + " in account (resource from " + source + ")", enableAttrDef);
            assertTrue("Enable attribute " + enableAttrName + " is not ignored (resource from " + source + ")", enableAttrDef.isIgnored());
        }
    }

	private void checkOpenDjSchemaHandling(ResourceType resource, String source) {
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		for (ResourceObjectTypeDefinitionType resObjectTypeDef: schemaHandling.getObjectType()) {
			if (resObjectTypeDef.getKind() == ShadowKindType.ACCOUNT) {
				String name = resObjectTypeDef.getIntent();
				assertNotNull("Resource "+resource+" from "+source+" has an schemaHandlig account definition without intent", name);
				assertNotNull("Account type "+name+" in "+resource+" from "+source+" does not have object class", resObjectTypeDef.getObjectClass());
			}
			if (resObjectTypeDef.getKind() == ShadowKindType.ENTITLEMENT) {
				String name = resObjectTypeDef.getIntent();
				assertNotNull("Resource "+resource+" from "+source+" has an schemaHandlig entitlement definition without intent", name);
				assertNotNull("Entitlement type "+name+" in "+resource+" from "+source+" does not have object class", resObjectTypeDef.getObjectClass());
			}
		}
	}

	private void checkOpenDjConfiguration(PrismObject<ResourceType> resource, String source) {
		checkOpenResourceConfiguration(resource, CONNECTOR_LDAP_NAMESPACE, "bindPassword", 8, source);
	}

	private void checkOpenResourceConfiguration(PrismObject<ResourceType> resource, String connectorNamespace, String credentialsPropertyName,
			int numConfigProps, String source) {
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container in "+resource+" from "+source, configurationContainer);
		PrismContainer<Containerable> configPropsContainer = configurationContainer.findContainer(SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No configuration properties container in "+resource+" from "+source, configPropsContainer);
		List<? extends Item<?,?>> configProps = configPropsContainer.getValue().getItems();
		assertEquals("Wrong number of config properties in "+resource+" from "+source, numConfigProps, configProps.size());
		PrismProperty<Object> credentialsProp = configPropsContainer.findProperty(new QName(connectorNamespace,credentialsPropertyName));
		if (credentialsProp == null) {
			// The is the heisenbug we are looking for. Just dump the entire damn thing.
			display("Configuration with the heisenbug", configurationContainer.debugDump());
		}
		assertNotNull("No "+credentialsPropertyName+" property in "+resource+" from "+source, credentialsProp);
		assertEquals("Wrong number of "+credentialsPropertyName+" property value in "+resource+" from "+source, 1, credentialsProp.getValues().size());
		PrismPropertyValue<Object> credentialsPropertyValue = credentialsProp.getValues().iterator().next();
		assertNotNull("No "+credentialsPropertyName+" property value in "+resource+" from "+source, credentialsPropertyValue);
		if (credentialsPropertyValue.isRaw()) {
			Object rawElement = credentialsPropertyValue.getRawElement();
			assertTrue("Wrong element class "+rawElement.getClass()+" in "+resource+" from "+source, rawElement instanceof MapXNode);
//			Element rawDomElement = (Element)rawElement;
			MapXNode xmap = (MapXNode) rawElement;
			try{
			ProtectedStringType protectedType = new ProtectedStringType();
			XNodeProcessorUtil.parseProtectedType(protectedType, xmap, prismContext);
	//		display("LDAP credentials raw element", DOMUtil.serializeDOMToString(rawDomElement));
//			assertEquals("Wrong credentials element namespace in "+resource+" from "+source, connectorNamespace, rawDomElement.getNamespaceURI());
//			assertEquals("Wrong credentials element local name in "+resource+" from "+source, credentialsPropertyName, rawDomElement.getLocalName());
//			Element encryptedDataElement = DOMUtil.getChildElement(rawDomElement, new QName(DOMUtil.NS_XML_ENC, "EncryptedData"));
			EncryptedDataType encryptedDataType = protectedType.getEncryptedDataType();
			assertNotNull("No EncryptedData element", encryptedDataType);
			} catch (SchemaException ex){
				throw new IllegalArgumentException(ex);
			}
//			assertEquals("Wrong EncryptedData element namespace in "+resource+" from "+source, DOMUtil.NS_XML_ENC, encryptedDataType.getNamespaceURI());
//			assertEquals("Wrong EncryptedData element local name in "+resource+" from "+source, "EncryptedData", encryptedDataType.getLocalName());
		} else {
			Object credentials = credentialsPropertyValue.getValue();
			assertTrue("Wrong type of credentials configuration property in "+resource+" from "+source+": "+credentials.getClass(), credentials instanceof ProtectedStringType);
			ProtectedStringType credentialsPs = (ProtectedStringType)credentials;
			EncryptedDataType encryptedData = credentialsPs.getEncryptedDataType();
			assertNotNull("No EncryptedData element", encryptedData);
		}

	}

	@Test
    public void test002AddDerbyResource() throws Exception {
		final String TEST_NAME = "test002AddDerbyResource";
        displayTestTitle(TEST_NAME);

        // GIVEN
        OperationResult result = new OperationResult(TestSanity.class.getName() + "." + TEST_NAME);

        checkRepoOpenDjResource();
        assertNoRepoCache();

        PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(RESOURCE_DERBY_FILENAME));
        assertParentConsistency(resource);
        fillInConnectorRef(resource, IntegrationTestTools.DBTABLE_CONNECTOR_TYPE, result);

        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<String> oidHolder = new Holder<String>();

        display("Adding Derby Resource", resource);

        // WHEN
        addObjectViaModelWS(resource.asObjectable(), null, oidHolder, resultHolder);

        // THEN
        // Check if Derby resource was imported correctly
        PrismObject<ResourceType> derbyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result);
        AssertJUnit.assertEquals(RESOURCE_DERBY_OID, derbyResource.getOid());

        assertNoRepoCache();

        String dbConnectorOid = derbyResource.asObjectable().getConnectorRef().getOid();
        PrismObject<ConnectorType> dbConnector = repositoryService.getObject(ConnectorType.class, dbConnectorOid, null, result);
        display("DB Connector: ", dbConnector);

        // Check if password was encrypted during import
        // via JAXB
        Object configurationPropertiesElement = JAXBUtil.findElement(derbyResource.asObjectable().getConnectorConfiguration().getAny(),
                new QName(dbConnector.asObjectable().getNamespace(), "configurationProperties"));
        Object passwordElement = JAXBUtil.findElement(JAXBUtil.listChildElements(configurationPropertiesElement),
                new QName(dbConnector.asObjectable().getNamespace(), "password"));
        System.out.println("Password element: " + passwordElement);

        // via prisms
        PrismContainerValue configurationProperties = derbyResource.findContainer(
                new ItemPath(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        new QName("configurationProperties")))
                .getValue();
        PrismProperty password = configurationProperties.findProperty(new QName(dbConnector.asObjectable().getNamespace(), "password"));
        System.out.println("Password property: " + password);
	}

    private void addObjectViaModelWS(ObjectType objectType, ModelExecuteOptionsType options, Holder<String> oidHolder, Holder<OperationResultType> resultHolder) throws FaultMessage {
    	ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType objectDelta = new ObjectDeltaType();
    	objectDelta.setObjectToAdd(objectType);
    	QName type = objectType.asPrismObject().getDefinition().getTypeName();
    	objectDelta.setObjectType(type);
    	objectDelta.setChangeType(ChangeTypeType.ADD);
    	deltaList.getDelta().add(objectDelta);
    	ObjectDeltaOperationListType objectDeltaOperationListType = modelWeb.executeChanges(deltaList, options);
        ObjectDeltaOperationType objectDeltaOperationType = getOdoFromDeltaOperationList(objectDeltaOperationListType, objectDelta);
        resultHolder.value = objectDeltaOperationType.getExecutionResult();
        oidHolder.value = ((ObjectType) objectDeltaOperationType.getObjectDelta().getObjectToAdd()).getOid();
    }

    // ugly hack...
    private static ObjectDeltaOperationType getOdoFromDeltaOperationList(ObjectDeltaOperationListType operationListType, ObjectDeltaType originalDelta) {
        Validate.notNull(operationListType);
        Validate.notNull(originalDelta);
        for (ObjectDeltaOperationType operationType : operationListType.getDeltaOperation()) {
            ObjectDeltaType objectDeltaType = operationType.getObjectDelta();
            if (originalDelta.getChangeType() == ChangeTypeType.ADD) {
                if (objectDeltaType.getChangeType() == originalDelta.getChangeType() &&
                    objectDeltaType.getObjectToAdd() != null) {
                    ObjectType objectAdded = (ObjectType) objectDeltaType.getObjectToAdd();
                    if (objectAdded.getClass().equals(originalDelta.getObjectToAdd().getClass())) {
                        return operationType;
                    }
                }
            } else {
                if (objectDeltaType.getChangeType() == originalDelta.getChangeType() &&
                        originalDelta.getOid().equals(objectDeltaType.getOid())) {
                    return operationType;
                }
            }
        }
        throw new IllegalStateException("No suitable ObjectDeltaOperationType found");
    }

    private void checkRepoDerbyResource() throws ObjectNotFoundException, SchemaException {
    	OperationResult result = new OperationResult(TestSanity.class.getName()+".checkRepoDerbyResource");
    	PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, result);
    	checkDerbyResource(resource, "repository");
    }

	private void checkDerbyResource(PrismObject<ResourceType> resource, String source) {
		checkDerbyConfiguration(resource, source);
	}

	private void checkDerbyConfiguration(PrismObject<ResourceType> resource, String source) {
		checkOpenResourceConfiguration(resource, CONNECTOR_DBTABLE_NAMESPACE, "password", 10, source);
	}


    /**
     * Test the testResource method. Expect a complete success for now.
     */
    @Test
    public void test003TestConnectionDerby() throws Exception {
        TestUtil.displayTestTitle("test003TestConnectionDerby");

        // GIVEN

        checkRepoDerbyResource();
        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.testResource(RESOURCE_DERBY_OID);

        // THEN

        assertNoRepoCache();
        displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

        TestUtil.assertSuccess("testResource has failed", result.getPartialResults().get(0));

        OperationResult opResult = new OperationResult(TestSanity.class.getName() + ".test002TestConnectionDerby");

        PrismObject<ResourceType> rObject = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, opResult);
        resourceDerby = rObject.asObjectable();
        checkDerbyResource(rObject, "repository(after test)");

        assertNoRepoCache();
        assertEquals(RESOURCE_DERBY_OID, resourceDerby.getOid());
        display("Initialized Derby resource (respository)", resourceDerby);
        assertNotNull("Resource schema was not generated", resourceDerby.getSchema());
        Element resourceDerbyXsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(resourceDerby);
        assertNotNull("Resource schema was not generated", resourceDerbyXsdSchemaElement);

        PrismObject<ResourceType> derbyResourceProvisioninig = provisioningService.getObject(ResourceType.class, RESOURCE_DERBY_OID,
                null, null, opResult);
        display("Initialized Derby resource (provisioning)", derbyResourceProvisioninig);

        PrismObject<ResourceType> derbyResourceModel = provisioningService.getObject(ResourceType.class, RESOURCE_DERBY_OID,
                null, null, opResult);
        display("Initialized Derby resource (model)", derbyResourceModel);

        // TODO: check
//		checkOpenDjResource(resourceOpenDj,"repository");
//		checkOpenDjResource(openDjResourceProvisioninig,"provisioning");
//		checkOpenDjResource(openDjResourceModel,"model");
        // TODO: model web

    }

    @Test
    public void test004Capabilities() throws ObjectNotFoundException, CommunicationException, SchemaException,
            FaultMessage {
        TestUtil.displayTestTitle("test004Capabilities");

        // GIVEN

        checkRepoOpenDjResource();

        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();

		// WHEN
        modelWeb.getObject(ObjectTypes.RESOURCE.getTypeQName(), RESOURCE_OPENDJ_OID,
                options , objectHolder, resultHolder);

        ResourceType resource = (ResourceType) objectHolder.value;

        // THEN
        display("Resource", resource);

        assertNoRepoCache();

        CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
        List<Object> capabilities = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned", capabilities.isEmpty());

        for (Object capability : nativeCapabilities.getAny()) {
            System.out.println("Native Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        if (resource.getCapabilities() != null) {
            for (Object capability : resource.getCapabilities().getConfigured().getAny()) {
                System.out.println("Configured Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
            }
        }

        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
            System.out.println("Efective Capability: " + CapabilityUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        CredentialsCapabilityType capCred = CapabilityUtil.getCapability(capabilities, CredentialsCapabilityType.class);
        assertNotNull("password capability not present", capCred.getPassword());
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = CapabilityUtil.getCapability(capabilities, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it", capAct);

        capCred = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
        assertNotNull("password capability not found", capCred.getPassword());
        // Although connector does not support activation, the resource specifies a way how to simulate it.
        // Therefore the following should succeed
        capAct = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        assertNotNull("activation capability not found", capAct);

    }

    @Test
    public void test005resolveConnectorRef() throws Exception{

    	TestUtil.displayTestTitle("test005resolveConnectorRef");

    	PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(RESOURCE_DUMMY_FILENAME));

    	ModelExecuteOptionsType options = new ModelExecuteOptionsType();
    	options.setIsImport(Boolean.TRUE);
    	addObjectViaModelWS(resource.asObjectable(), options, new Holder<String>(), new Holder<OperationResultType>());

    	 OperationResult repoResult = new OperationResult("getObject");

         PrismObject<ResourceType> uObject = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, repoResult);
         assertNotNull(uObject);

         ResourceType resourceType = uObject.asObjectable();
         assertNotNull("Reference on the connector must not be null in resource.",resourceType.getConnectorRef());
         assertNotNull("Missing oid reference on the connector",resourceType.getConnectorRef().getOid());

    }

    @Test
    public void test006reimportResourceDummy() throws Exception{

    	TestUtil.displayTestTitle("test006reimportResourceDummy");

    	//get object from repo (with version set and try to add it - it should be re-added, without error)
    	 OperationResult repoResult = new OperationResult("getObject");

         PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, repoResult);
         assertNotNull(resource);


         ModelExecuteOptionsType options = new ModelExecuteOptionsType();
         options.setOverwrite(Boolean.TRUE);
         options.setIsImport(Boolean.TRUE);
     	addObjectViaModelWS(resource.asObjectable(), options, new Holder<String>(), new Holder<OperationResultType>());

     	//TODO: add some asserts


     	//parse object from file again and try to add it - this should fail, becasue the same object already exists)
     	resource = PrismTestUtil.parseObject(new File(RESOURCE_DUMMY_FILENAME));

		try {
     	Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
     	options = new ModelExecuteOptionsType();
        options.setIsImport(Boolean.TRUE);
			addObjectViaModelWS(resource.asObjectable(), options, new Holder<String>(),
                    resultHolder);

			OperationResultType result = resultHolder.value;
			TestUtil.assertFailure(result);

			fail("Expected object already exists exception, but haven't got one.");
		} catch (FaultMessage ex) {
			LOGGER.info("fault {}", ex.getFaultInfo());
			LOGGER.info("fault {}", ex.getCause());
			if (ex.getFaultInfo() instanceof ObjectAlreadyExistsFaultType){
			// this is OK, we expect this
			} else{
				fail("Expected object already exists exception, but haven't got one.");
			}

		}


//         ResourceType resourceType = uObject.asObjectable();
//         assertNotNull("Reference on the connector must not be null in resource.",resourceType.getConnectorRef());
//         assertNotNull("Missing oid reference on the connector",resourceType.getConnectorRef().getOid());

    }

    /**
     * Attempt to add new user. It is only added to the repository, so check if
     * it is in the repository after the operation.
     */
    @Test
    public void test010AddUser() throws Exception {
    	final String TEST_NAME = "test010AddUser";
        displayTestTitle(TEST_NAME);

        // GIVEN
        checkRepoOpenDjResource();
        assertNoRepoCache();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
        UserType userType = user.asObjectable();
        assertParentConsistency(user);

        // Encrypt Jack's password
        protector.encrypt(userType.getCredentials().getPassword().getValue());
        assertParentConsistency(user);

        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
        Holder<String> oidHolder = new Holder<String>();

        display("Adding user object", userType);

        // WHEN
        addObjectViaModelWS(userType, null, oidHolder, resultHolder);

        // THEN

        assertNoRepoCache();
        displayJaxb("addObject result:", resultHolder.value, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("addObject has failed", resultHolder.value);

//        AssertJUnit.assertEquals(USER_JACK_OID, oid);

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();

        repoResult.computeStatus();
        display("repository.getObject result", repoResult);
        TestUtil.assertSuccess("getObject has failed", repoResult);
        AssertJUnit.assertEquals(USER_JACK_OID, repoUser.getOid());
        assertEqualsPolyString("fullName", userType.getFullName(), repoUser.getFullName());

        // TODO: better checks
    }

    /**
     * Add account to user. This should result in account provisioning. Check if
     * that happens in repo and in LDAP.
     */
    @Test
    public void test013AddOpenDjAccountToUser() throws Exception {
    	final String TEST_NAME = "test013AddOpenDjAccountToUser";
        displayTestTitle(TEST_NAME);
        try{
        // GIVEN
        checkRepoOpenDjResource();
        assertNoRepoCache();

        // IMPORTANT! SWITCHING OFF ASSIGNMENT ENFORCEMENT HERE!
        setAssignmentEnforcement(AssignmentPolicyEnforcementType.NONE);
        // This is not redundant. It checks that the previous command set the policy correctly
        assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.NONE);

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME, ObjectDeltaType.class);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();

        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals("No accountRefs", 1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidOpendj = accountRef.getOid();
        assertFalse(accountShadowOidOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj,
        		null, repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        assertNotNull("Shadow stored in repository has no name", repoShadowType.getName());
        // Check the "name" property, it should be set to DN, not entryUUID
        assertEquals("Wrong name property", USER_JACK_LDAP_DN.toLowerCase(), repoShadowType.getName().getOrig().toLowerCase());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(uid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        OpenDJController.assertAttribute(entry, "cn", "Jack Sparrow");
        OpenDJController.assertAttribute(entry, "displayName", "Jack Sparrow");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Black Pearl");

        assertTrue("LDAP account is not enabled", openDJController.isAccountEnabled(entry));

        originalJacksLdapPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword was not set on create", originalJacksLdapPassword);
        System.out.println("password after create: " + originalJacksLdapPassword);

        // Use getObject to test fetch of complete shadow

        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();

        // WHEN
        modelWeb.getObject(ObjectTypes.SHADOW.getTypeQName(), accountShadowOidOpendj,
                options, objectHolder, resultHolder);

        // THEN
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("getObject has failed", resultHolder.value);

        ShadowType modelShadow = (ShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, getOpenDjPrimaryIdentifierQName());
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "uid", "jack");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "givenName", "Jack");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "sn", "Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "cn", "Jack Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "displayName", "Jack Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "l", "Black Pearl");
        assertNull("carLicense attribute sneaked to LDAP", OpenDJController.getAttributeValue(entry, "carLicense"));
        assertNull("postalAddress attribute sneaked to LDAP", OpenDJController.getAttributeValue(entry, "postalAddress"));

        assertNotNull("Activation is null (model)", modelShadow.getActivation());
        assertEquals("Wrong administrativeStatus in the shadow (model)", ActivationStatusType.ENABLED, modelShadow.getActivation().getAdministrativeStatus());
        } catch (Exception ex){
        	LOGGER.info("ERROR: {}", ex);
        	throw ex;
        }

    }

    private OperationResultType modifyObjectViaModelWS(ObjectDeltaType objectChange) throws FaultMessage {
    	ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	deltaList.getDelta().add(objectChange);
    	ObjectDeltaOperationListType list = modelWeb.executeChanges(deltaList, null);
        return getOdoFromDeltaOperationList(list, objectChange).getExecutionResult();
    }

    /**
     * Add Derby account to user. This should result in account provisioning. Check if
     * that happens in repo and in Derby.
     */
    @Test
    public void test014AddDerbyAccountToUser() throws IOException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException, SQLException {
        TestUtil.displayTestTitle("test014AddDerbyAccountToUser");

        // GIVEN

        checkRepoDerbyResource();
        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ADD_ACCOUNT_DERBY_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();

        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        // OpenDJ account was added in previous test, hence 2 accounts
        assertEquals(2, accountRefs.size());

        ObjectReferenceType accountRef = null;
        for (ObjectReferenceType ref : accountRefs) {
            if (!ref.getOid().equals(accountShadowOidOpendj)) {
                accountRef = ref;
            }
        }

        accountShadowOidDerby = accountRef.getOid();
        assertFalse(accountShadowOidDerby.isEmpty());

        // Check if shadow was created in the repo
        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidDerby,
        		null, repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("addObject has failed", repoResult);
        display("Shadow (repository)", repoShadowType);
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_DERBY_OID, repoShadowType.getResourceRef().getOid());

        // Check the "name" property, it should be set to DN, not entryUUID
        assertEquals("Wrong name property", PrismTestUtil.createPolyStringType(USER_JACK_DERBY_LOGIN),
        		repoShadowType.getName());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // check if account was created in DB Table

        Statement stmt = derbyController.getExecutedStatementWhereLoginName(uid);
        ResultSet rs = stmt.getResultSet();

        System.out.println("RS: " + rs);

        assertTrue("No records found for login name " + uid, rs.next());
        assertEquals(USER_JACK_DERBY_LOGIN, rs.getString(DerbyController.COLUMN_LOGIN));
        assertEquals("Cpt. Jack Sparrow", rs.getString(DerbyController.COLUMN_FULL_NAME));
        // TODO: check password
        //assertEquals("3lizab3th",rs.getString(DerbyController.COLUMN_PASSWORD));
        System.out.println("Password: " + rs.getString(DerbyController.COLUMN_PASSWORD));

        assertFalse("Too many records found for login name " + uid, rs.next());
        rs.close();
        stmt.close();

        // Use getObject to test fetch of complete shadow

        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType ();

        // WHEN
        modelWeb.getObject(ObjectTypes.SHADOW.getTypeQName(), accountShadowOidDerby,
                options, objectHolder, resultHolder);

        // THEN
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("getObject has failed", resultHolder.value);

        ShadowType modelShadow = (ShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_DERBY_OID, modelShadow.getResourceRef().getOid());

        assertAttribute(modelShadow, SchemaConstants.ICFS_UID, USER_JACK_DERBY_LOGIN);
        assertAttribute(modelShadow, SchemaConstants.ICFS_NAME, USER_JACK_DERBY_LOGIN);
        assertAttribute(resourceDerby, modelShadow, "FULL_NAME", "Cpt. Jack Sparrow");

    }

    @Test
    public void test015AccountOwner() throws FaultMessage, ObjectNotFoundException, SchemaException, JAXBException {
        TestUtil.displayTestTitle("test015AccountOwner");

        // GIVEN
        checkRepoOpenDjResource();
        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<UserType> userHolder = new Holder<UserType>();

        // WHEN

        modelWeb.findShadowOwner(accountShadowOidOpendj, userHolder, resultHolder);

        // THEN

        display("listAccountShadowOwner result", resultHolder.value);
        TestUtil.assertSuccess("listAccountShadowOwner has failed (result)", resultHolder.value);
        UserType user = userHolder.value;
        assertNotNull("No owner", user);
        assertEquals(USER_JACK_OID, user.getOid());

        System.out.println("Account " + accountShadowOidOpendj + " has owner " + ObjectTypeUtil.toShortString(user));
    }

    @Test
    public void test016ProvisioningSearchAccountsIterative() throws Exception {
        TestUtil.displayTestTitle("test016ProvisioningSearchAccountsIterative");

        // GIVEN
        OperationResult result = new OperationResult(TestSanity.class.getName() + ".test016ProvisioningSearchAccountsIterative");

        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceTypeOpenDjrepo, prismContext);
        final RefinedObjectClassDefinition refinedAccountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);

        QName objectClass = refinedAccountDefinition.getObjectClassDefinition().getTypeName();
        ObjectQuery q = ObjectQueryUtil.createResourceAndObjectClassQuery(resourceTypeOpenDjrepo.getOid(), objectClass, prismContext);
//        ObjectQuery q = QueryConvertor.createObjectQuery(ResourceObjectShadowType.class, query, prismContext);

        final Collection<ObjectType> objects = new HashSet<ObjectType>();
        final MatchingRule caseIgnoreMatchingRule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, DOMUtil.XSD_STRING);
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
                assertEquals(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS, shadow.getObjectClass());
                assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
                String icfUid = getAttributeValue(shadow, getOpenDjPrimaryIdentifierQName());
                assertNotNull("No ICF UID", icfUid);
                String icfName = getNormalizedAttributeValue(shadow, refinedAccountDefinition, getOpenDjSecondaryIdentifierQName());
                assertNotNull("No ICF NAME", icfName);
                try {
					PrismAsserts.assertEquals("Wrong shadow name", caseIgnoreMatchingRule, shadow.getName().getOrig(), icfName);
				} catch (SchemaException e) {
					throw new IllegalArgumentException(e.getMessage(),e);
				}
                assertNotNull("Missing LDAP uid", getAttributeValue(shadow, new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "uid")));
                assertNotNull("Missing LDAP cn", getAttributeValue(shadow, new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "cn")));
                assertNotNull("Missing LDAP sn", getAttributeValue(shadow, new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "sn")));
                assertNotNull("Missing activation", shadow.getActivation());
                assertNotNull("Missing activation status", shadow.getActivation().getAdministrativeStatus());
                return true;
            }
        };

        // WHEN

        provisioningService.searchObjectsIterative(ShadowType.class, q, null, handler, null, result);

        // THEN

        display("Count", objects.size());
    }

    /**
     * We are going to modify the user. As the user has an account, the user
     * changes should be also applied to the account (by schemaHandling).
     */
    @Test
    public void test020ModifyUser() throws Exception {
    	final String TEST_NAME = "test020ModifyUser";
        displayTestTitle(TEST_NAME);
        // GIVEN

        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        display("repository user", repoUser);

        PrismAsserts.assertEqualsPolyString("wrong value for fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong value for locality", "somewhere", repoUserType.getLocality());
        assertEquals("wrong value for employeeNumber", "1", repoUserType.getEmployeeNumber());

        // Check if appropriate accountRef is still there

        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue("No OID in "+accountRef+" in "+repoUserType,
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));

        }

        // Check if shadow is still in the repo and that it is untouched

        repoResult = new OperationResult("getObject");
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        display("repository shadow", repoShadow);
        AssertJUnit.assertNotNull(repoShadow);
        ShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)

        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated
        assertOpenDJAccountJack(uid, "jack");
    }

    private Entry assertOpenDJAccountJack(String entryUuid, String uid) throws DirectoryException {
    	Entry entry = openDJController.searchAndAssertByEntryUuid(entryUuid);
    	return assertOpenDJAccountJack(entry, uid);
    }

    private Entry assertOpenDJAccountJack(Entry entry, String uid) throws DirectoryException {
    	return assertOpenDJAccountJack(entry, uid, "Jack");
    }

    private Entry assertOpenDJAccountJack(Entry entry, String uid, String givenName) throws DirectoryException {
        display(entry);

	    OpenDJController.assertDn(entry, "uid="+uid+",ou=people,dc=example,dc=com");
	    OpenDJController.assertAttribute(entry, "uid", uid);
	    if (givenName == null) {
	    	OpenDJController.assertAttribute(entry, "givenName");
	    } else {
	    	OpenDJController.assertAttribute(entry, "givenName", givenName);
	    }
	    OpenDJController.assertAttribute(entry, "sn", "Sparrow");
	    // These two should be assigned from the User modification by
	    // schemaHandling
	    OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
	    OpenDJController.assertAttribute(entry, "displayName", "Cpt. Jack Sparrow");
		// This will get translated from "somewhere" to this (outbound
		// expression in schemeHandling) -> this is not more supported...we
		// don't support complex run-time properties. the value will be
		// evaluated from outbound expression
	    OpenDJController.assertAttribute(entry, "l", "somewhere");
	    OpenDJController.assertAttribute(entry, "postalAddress", "Number 1");

	    return entry;
    }

    /**
     * We are going to change user's password. As the user has an account, the password change
     * should be also applied to the account (by schemaHandling).
     */
    @Test
    public void test022ChangeUserPassword() throws Exception {
    	final String TEST_NAME = "test022ChangeUserPassword";
        displayTestTitle(TEST_NAME);
        // GIVEN

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_PASSWORD_FILENAME, ObjectDeltaType.class);

        System.out.println("In modification: " + objectChange.getItemDelta().get(0).getValue().get(0));
        assertNoRepoCache();

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertUserPasswordChange("butUnd3dM4yT4lkAL0t", result);
    }

    /**
     * Similar to previous test just the request is constructed a bit differently.
     */
    @Test
    public void test023ChangeUserPasswordJAXB() throws Exception {
    	final String TEST_NAME = "test023ChangeUserPasswordJAXB";
        displayTestTitle(TEST_NAME);

        // GIVEN
        final String NEW_PASSWORD = "abandonSHIP";
        Document doc = ModelClientUtil.getDocumnent();

        ObjectDeltaType userDelta = new ObjectDeltaType();
        userDelta.setOid(USER_JACK_OID);
        userDelta.setChangeType(ChangeTypeType.MODIFY);
        userDelta.setObjectType(UserType.COMPLEX_TYPE);

        ItemDeltaType passwordDelta = new ItemDeltaType();
        passwordDelta.setModificationType(ModificationTypeType.REPLACE);
        passwordDelta.setPath(ModelClientUtil.createItemPathType("credentials/password/value"));
        ProtectedStringType pass = new ProtectedStringType();
        pass.setClearValue(NEW_PASSWORD);
        XNode passValue = ((PrismContextImpl) prismContext).getBeanMarshaller().marshall(pass);
        System.out.println("PASSWORD VALUE: " + passValue.debugDump());
        RawType passwordValue = new RawType(passValue, prismContext);
        passwordDelta.getValue().add(passwordValue);
        userDelta.getItemDelta().add(passwordDelta);

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(userDelta);

        // THEN
        assertUserPasswordChange(NEW_PASSWORD, result);
    }

    private void assertUserPasswordChange(String expectedUserPassword, OperationResultType result) throws JAXBException, ObjectNotFoundException, SchemaException, DirectoryException, EncryptionException {
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        display("repository user", repoUser);

        // Check if nothing else was modified
        PrismAsserts.assertEqualsPolyString("wrong repo fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong repo locality", "somewhere", repoUserType.getLocality());

        // Check if appropriate accountRef is still there
        assertLinks(repoUser, 2);
        assertLinked(repoUser, accountShadowOidOpendj);
        assertLinked(repoUser, accountShadowOidDerby);

        assertPassword(repoUser, expectedUserPassword);

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
        display("repository shadow", repoShadow);
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertNotNull(repoShadowType);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated
        Entry entry = assertOpenDJAccountJack(uid, "jack");

        String ldapPasswordAfter = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull(ldapPasswordAfter);

        display("LDAP password after change", ldapPasswordAfter);

        assertFalse("No change in password (original)", ldapPasswordAfter.equals(originalJacksLdapPassword));
        if (lastJacksLdapPassword != null) {
        	assertFalse("No change in password (last)", ldapPasswordAfter.equals(lastJacksLdapPassword));
        }
        lastJacksLdapPassword = ldapPasswordAfter;
    }

    @Test
    public void test027ModifyAccountDj() throws Exception {
    	final String TEST_NAME = "test027ModifyAccountDj";
        testModifyAccountDjRoomNumber(TEST_NAME, REQUEST_ACCOUNT_MODIFY_ROOM_NUMBER_FILE, "quarterdeck");
    }

    @Test
    public void test028ModifyAccountDjExplicitType() throws Exception {
    	final String TEST_NAME = "test028ModifyAccountDjExplicitType";
        testModifyAccountDjRoomNumber(TEST_NAME, REQUEST_ACCOUNT_MODIFY_ROOM_NUMBER_EXPLICIT_TYPE_FILE, "upperdeck");
    }

    public void testModifyAccountDjRoomNumber(final String TEST_NAME, File reqFile, String expectedVal) throws Exception {
        displayTestTitle(TEST_NAME);
        // GIVEN
        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(reqFile, ObjectDeltaType.class);
        objectChange.setOid(accountShadowOidOpendj);

        // WHEN
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        display("repository shadow", repoShadow);
        AssertJUnit.assertNotNull(repoShadow);
        ShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)

        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated
        Entry jackLdapEntry = assertOpenDJAccountJack(uid, "jack");
        OpenDJController.assertAttribute(jackLdapEntry, "roomNumber", expectedVal);
    }


    @Test
    public void test029ModifyAccountDjBadPath() throws Exception {
    	final String TEST_NAME = "test029ModifyAccountDjBadPath";
        displayTestTitle(TEST_NAME);
        // GIVEN
        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_ACCOUNT_MODIFY_BAD_PATH_FILE, ObjectDeltaType.class);
        objectChange.setOid(accountShadowOidOpendj);

        OperationResultType result;
        try {
	        // WHEN
	        result = modifyObjectViaModelWS(objectChange);

	        AssertJUnit.fail("Unexpected success");
        } catch (FaultMessage f) {
        	// this is expected
        	FaultType faultInfo = f.getFaultInfo();
        	result = faultInfo.getOperationResult();
        }

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertFailure(result);

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        display("repository shadow", repoShadow);
        AssertJUnit.assertNotNull(repoShadow);
        ShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)

        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated
        Entry jackLdapEntry = assertOpenDJAccountJack(uid, "jack");
        OpenDJController.assertAttribute(jackLdapEntry, "roomNumber", "upperdeck");
    }

    /**
     * Try to disable user. As the user has an account, the account should be disabled as well.
     */
    @Test
    public void test030DisableUser() throws Exception {
    	final String TEST_NAME = "test030DisableUser";
        displayTestTitle(TEST_NAME);
        // GIVEN

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ACTIVATION_DISABLE_FILENAME, ObjectDeltaType.class);

        Entry entry = openDJController.searchByUid("jack");
        assertOpenDJAccountJack(entry, "jack");

        String pwpAccountDisabled = OpenDJController.getAttributeValue(entry, "ds-pwp-account-disabled");
        display("ds-pwp-account-disabled before change", pwpAccountDisabled);
        assertTrue("LDAP account is not enabled (precondition)", openDJController.isAccountEnabled(entry));

        assertNoRepoCache();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        display("repository user", repoUser);
        UserType repoUserType = repoUser.asObjectable();

        // Check if nothing else was modified
        assertEqualsPolyString("wrong repo fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        assertEqualsPolyString("wrong repo locality", "somewhere", repoUserType.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue("No OID in "+accountRef+" in "+repoUserType,
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));
        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
        display("repo shadow", repoShadow);
        ShadowType repoShadowType = repoShadow.asObjectable();

        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        AssertJUnit.assertNotNull(repoShadowType);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated

        entry = openDJController.searchAndAssertByEntryUuid(uid);
        assertOpenDJAccountJack(entry, "jack");

        pwpAccountDisabled = OpenDJController.getAttributeValue(entry, "ds-pwp-account-disabled");
        display("ds-pwp-account-disabled after change", pwpAccountDisabled);
        assertFalse("LDAP account was not disabled", openDJController.isAccountEnabled(entry));

        // Use getObject to test fetch of complete shadow

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        assertNoRepoCache();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelWeb.getObject(ObjectTypes.SHADOW.getTypeQName(), accountShadowOidOpendj,
                options, objectHolder, resultHolder);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("getObject has failed", resultHolder.value);

        ShadowType modelShadow = (ShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, getOpenDjPrimaryIdentifierQName());
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "uid", "jack");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "givenName", "Jack");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "sn", "Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "cn", "Cpt. Jack Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "displayName", "Cpt. Jack Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "l", "somewhere");

        assertNotNull("The account activation is null in the shadow", modelShadow.getActivation());
        assertNotNull("The account activation status was not present in shadow", modelShadow.getActivation().getAdministrativeStatus());
        assertEquals("The account was not disabled in the shadow", ActivationStatusType.DISABLED, modelShadow.getActivation().getAdministrativeStatus());

    }

    /**
     * Try to enable user after it has been disabled. As the user has an account, the account should be enabled as well.
     */
    @Test
    public void test031EnableUser() throws Exception {
    	final String TEST_NAME = "test031EnableUser";
        displayTestTitle(TEST_NAME);
        // GIVEN

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ACTIVATION_ENABLE_FILENAME, ObjectDeltaType.class);
        assertNoRepoCache();

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();
        display("repo user", repoUser);

        // Check if nothing else was modified
        PrismAsserts.assertEqualsPolyString("wrong repo fullName", "Cpt. Jack Sparrow", repoUser.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong repo locality", "somewhere", repoUser.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue("No OID in "+accountRef+" in "+repoUser,
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));
        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj,
        		null, repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();

        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        display("repo shadow", repoShadowType);
        AssertJUnit.assertNotNull(repoShadowType);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // Use getObject to test fetch of complete shadow

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        assertNoRepoCache();

        // WHEN
        modelWeb.getObject(ObjectTypes.SHADOW.getTypeQName(), accountShadowOidOpendj,
                options, objectHolder, resultHolder);

        // THEN
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("getObject has failed", resultHolder.value);

        ShadowType modelShadow = (ShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, getOpenDjPrimaryIdentifierQName());
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "uid", "jack");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "givenName", "Jack");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "sn", "Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "cn", "Cpt. Jack Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "displayName", "Cpt. Jack Sparrow");
        assertAttribute(resourceTypeOpenDjrepo, modelShadow, "l", "somewhere");

        assertNotNull("The account activation is null in the shadow", modelShadow.getActivation());
        assertNotNull("The account activation status was not present in shadow", modelShadow.getActivation().getAdministrativeStatus());
        assertEquals("The account was not enabled in the shadow", ActivationStatusType.ENABLED, modelShadow.getActivation().getAdministrativeStatus());

        // Check if LDAP account was updated

        Entry entry = openDJController.searchAndAssertByEntryUuid(uid);
        assertOpenDJAccountJack(entry, "jack");

        // The value of ds-pwp-account-disabled should have been removed
        String pwpAccountDisabled = OpenDJController.getAttributeValue(entry, "ds-pwp-account-disabled");
        System.out.println("ds-pwp-account-disabled after change: " + pwpAccountDisabled);
        assertTrue("LDAP account was not enabled", openDJController.isAccountEnabled(entry));
    }

    /**
     * Unlink account by removing the accountRef from the user.
     * The account will not be deleted, just the association to user will be broken.
     */
    @Test
    public void test040UnlinkDerbyAccountFromUser() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException, SQLException {
        TestUtil.displayTestTitle("test040UnlinkDerbyAccountFromUser");

        // GIVEN

        ObjectDeltaType objectChange = new ObjectDeltaType();
        objectChange.setOid(USER_JACK_OID);
        ItemDeltaType modificationDeleteAccountRef = new ItemDeltaType();
        modificationDeleteAccountRef.setModificationType(ModificationTypeType.DELETE);
        ObjectReferenceType accountRefToDelete = new ObjectReferenceType();
        accountRefToDelete.setOid(accountShadowOidDerby);
        RawType modificationValue = new RawType(((PrismContextImpl) prismContext).getBeanMarshaller().marshall(accountRefToDelete), prismContext);
        modificationDeleteAccountRef.getValue().add(modificationValue);
        modificationDeleteAccountRef.setPath(new ItemPathType(new ItemPath(UserType.F_LINK_REF)));
        objectChange.getItemDelta().add(modificationDeleteAccountRef);
        objectChange.setChangeType(ChangeTypeType.MODIFY);
        objectChange.setObjectType(UserType.COMPLEX_TYPE);
        displayJaxb("modifyObject input", objectChange, new QName(SchemaConstants.NS_C, "change"));
        assertNoRepoCache();

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        // only OpenDJ account should be left now
        assertEquals(1, accountRefs.size());
        ObjectReferenceType ref = accountRefs.get(0);
        assertEquals("Wrong OID in accountRef in "+repoUser, accountShadowOidOpendj, ref.getOid());

    }

    /**
     * Delete the shadow which will cause deletion of associated account.
     * The account was unlinked in the previous test, therefore no operation with user is needed.
     */
    @Test
    public void test041DeleteDerbyAccount() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException, SQLException {
        TestUtil.displayTestTitle("test041DeleteDerbyAccount");

        // GIVEN

        assertNoRepoCache();

        // WHEN
        OperationResultType result = deleteObjectViaModelWS(ObjectTypes.SHADOW.getTypeQName(), accountShadowOidDerby);

        // THEN
        assertNoRepoCache();
        displayJaxb("deleteObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("deleteObject has failed", result);

        // Check if shadow was deleted
        OperationResult repoResult = new OperationResult("getObject");

        try {
            repositoryService.getObject(ShadowType.class, accountShadowOidDerby,
            		null, repoResult);
            AssertJUnit.fail("Shadow was not deleted");
        } catch (ObjectNotFoundException ex) {
            display("Caught expected exception from getObject(shadow): " + ex);
        }

        // check if account was deleted in DB Table

        Statement stmt = derbyController.getExecutedStatementWhereLoginName(USER_JACK_DERBY_LOGIN);
        ResultSet rs = stmt.getResultSet();

        System.out.println("RS: " + rs);

        assertFalse("Account was not deleted in database", rs.next());

    }

    private OperationResultType deleteObjectViaModelWS(QName typeQName, String oid) throws FaultMessage {
    	ObjectDeltaListType deltaList = new ObjectDeltaListType();
    	ObjectDeltaType objectDelta = new ObjectDeltaType();
    	objectDelta.setOid(oid);
    	objectDelta.setObjectType(typeQName);
    	objectDelta.setChangeType(ChangeTypeType.DELETE);
    	deltaList.getDelta().add(objectDelta);
    	ObjectDeltaOperationListType list = modelWeb.executeChanges(deltaList, null);
        return getOdoFromDeltaOperationList(list, objectDelta).getExecutionResult();
    }

    @Test
    public void test047RenameUser() throws Exception {
    	final String TEST_NAME = "test047RenameUser";
        displayTestTitle(TEST_NAME);
        // GIVEN

        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_NAME_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        display("repository user", repoUser);

        PrismAsserts.assertEqualsPolyString("wrong value for User name", "jsparrow", repoUserType.getName());
        PrismAsserts.assertEqualsPolyString("wrong value for User fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong value for User locality", "somewhere", repoUserType.getLocality());
        assertEquals("wrong value for employeeNumber", "1", repoUserType.getEmployeeNumber());

        // Check if appropriate accountRef is still there

        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.iterator().next();
        assertEquals("Wrong OID in "+accountRef+" in "+repoUserType,
        		accountShadowOidOpendj, accountRef.getOid());

        // Check if shadow is still in the repo and that it is untouched

        repoResult = new OperationResult("getObject");
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        display("repository shadow", repoShadow);
        AssertJUnit.assertNotNull(repoShadow);
        ShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)

        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated
        assertOpenDJAccountJack(uid, "jsparrow");
    }


    /**
     * We are going to modify the user. As the user has an account, the user
     * changes should be also applied to the account (by schemaHandling).
     *
     * @throws DirectoryException
     */
    @Test
    public void test048ModifyUserRemoveGivenName() throws Exception {
    	final String TEST_NAME = "test048ModifyUserRemoveGivenName";
        displayTestTitle(TEST_NAME);

        // GIVEN
        assertNoRepoCache();
        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_GIVENNAME_FILENAME, ObjectDeltaType.class);
        displayJaxb("objectChange:", objectChange, SchemaConstants.T_OBJECT_DELTA);

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        display("repository user", repoUser);

        PrismAsserts.assertEqualsPolyString("wrong value for fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        assertNull("Value for givenName still present", repoUserType.getGivenName());

        // Check if appropriate accountRef is still there

        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.iterator().next();
        accountRef.getOid().equals(accountShadowOidOpendj);

        // Check if shadow is still in the repo and that it is untouched

        repoResult = new OperationResult("getObject");
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject(repo) has failed", repoResult);
        display("repository shadow", repoShadow);
        AssertJUnit.assertNotNull(repoShadow);
        ShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)

        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated
        Entry entry = openDJController.searchAndAssertByEntryUuid(uid);
        assertOpenDJAccountJack(entry, "jsparrow", null);
    }

    /**
     * The user should have an account now. Let's try to delete the user. The
     * account should be gone as well.
     *
     * @throws JAXBException
     */
    @Test
    public void test049DeleteUser() throws SchemaException, FaultMessage, DirectoryException, JAXBException {
        TestUtil.displayTestTitle("test049DeleteUser");
        // GIVEN

        assertNoRepoCache();

        // WHEN
        OperationResultType result = deleteObjectViaModelWS(ObjectTypes.USER.getTypeQName(), USER_JACK_OID);

        // THEN
        assertNoRepoCache();
        displayJaxb("deleteObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("deleteObject has failed", result);

        // User should be gone from the repository
        OperationResult repoResult = new OperationResult("getObject");
        try {
            repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);
            AssertJUnit.fail("User still exists in repo after delete");
        } catch (ObjectNotFoundException e) {
            // This is expected
        }

        // Account shadow should be gone from the repository
        repoResult = new OperationResult("getObject");
        try {
            repositoryService.getObject(ShadowType.class, accountShadowOidOpendj, null, repoResult);
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

    @Test
    public void test100AssignRolePirate() throws Exception {
    	final String TEST_NAME = "test100AssignRolePirate";
        displayTestTitle(TEST_NAME);

        // GIVEN

        // IMPORTANT! Assignment enforcement is FULL now
        setAssignmentEnforcement(AssignmentPolicyEnforcementType.FULL);
        // This is not redundant. It checks that the previous command set the policy correctly
        assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_GUYBRUSH_FILE);
        UserType userType = user.asObjectable();

        // Encrypt the password
        protector.encrypt(userType.getCredentials().getPassword().getValue());

        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
        Holder<String> oidHolder = new Holder<String>();
        assertNoRepoCache();

        addObjectViaModelWS(userType, null, oidHolder, resultHolder);

        assertNoRepoCache();
        TestUtil.assertSuccess("addObject has failed", resultHolder.value);

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_PIRATE_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
        		null, repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadowType);
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        accountGuybrushOpendjEntryUuuid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Bloody Pirate");
        OpenDJController.assertAttribute(entry, "businessCategory", "loot", "murder");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword was not set on create", guybrushPassword);

        // TODO: Derby

    }

    @Test
    public void test101AccountOwnerAfterRole() throws Exception {
    	final String TEST_NAME = "test101AccountOwnerAfterRole";
        displayTestTitle(TEST_NAME);

        // GIVEN

        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<UserType> userHolder = new Holder<UserType>();

        // WHEN

        modelWeb.findShadowOwner(accountShadowOidGuybrushOpendj, userHolder, resultHolder);

        // THEN

        TestUtil.assertSuccess("listAccountShadowOwner has failed (result)", resultHolder.value);
        UserType user = userHolder.value;
        assertNotNull("No owner", user);
        assertEquals(USER_GUYBRUSH_OID, user.getOid());

        System.out.println("Account " + accountShadowOidGuybrushOpendj + " has owner " + ObjectTypeUtil.toShortString(user));
    }


    @Test
    public void test102AssignRoleCaptain() throws Exception {
    	final String TEST_NAME = "test102AssignRoleCaptain";
        displayTestTitle(TEST_NAME);

        // GIVEN

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_1_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> aObject = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
        		null, repoResult);
        ShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Bloody Pirate", "Honorable Captain");
        OpenDJController.assertAttribute(entry, "carLicense", "C4PT41N");
        OpenDJController.assertAttribute(entry, "businessCategory", "loot", "murder", "cruise");
        // Expression in the role taking that from the user
        OpenDJController.assertAttribute(entry, "destinationIndicator", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "departmentNumber", "Department of Guybrush");
        // Expression in the role taking that from the assignment
        OpenDJController.assertAttribute(entry, "physicalDeliveryOfficeName", "The Sea Monkey");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword disappeared", guybrushPassword);

        // TODO: Derby

    }


    /**
     * Assign the same "captain" role again, this time with a slightly different assignment parameters.
     */
    @Test
    public void test103AssignRoleCaptainAgain() throws Exception {
    	final String TEST_NAME = "test103AssignRoleCaptainAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_2_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> aObject = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
        		null, repoResult);
        ShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Bloody Pirate", "Honorable Captain");
        OpenDJController.assertAttribute(entry, "carLicense", "C4PT41N");
        OpenDJController.assertAttribute(entry, "businessCategory", "loot", "murder", "cruise");
        // Expression in the role taking that from the user
        OpenDJController.assertAttribute(entry, "destinationIndicator", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "departmentNumber", "Department of Guybrush");
        // Expression in the role taking that from the assignments (both of them)
        OpenDJController.assertAttribute(entry, "physicalDeliveryOfficeName", "The Sea Monkey", "The Dainty Lady");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword disappeared", guybrushPassword);

        // TODO: Derby

    }


    @Test
    public void test105ModifyAccount() throws Exception {
    	final String TEST_NAME = "test105ModifyAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_ACCOUNT_MODIFY_ATTRS_FILE, ObjectDeltaType.class);
        objectChange.setOid(accountShadowOidGuybrushOpendj);

        // WHEN ObjectTypes.SHADOW.getTypeQName(),
        OperationResultType result = modifyObjectViaModelWS(objectChange);

        Task task = taskManager.createTaskInstance();
        OperationResult parentResult = new OperationResult(TEST_NAME + "-get after first modify");
        PrismObject<ShadowType> shadow= modelService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj, null, task, parentResult);
        assertNotNull("shadow must not be null", shadow);

        ShadowType shadowType = shadow.asObjectable();
        QName employeeTypeQName = new QName(resourceTypeOpenDjrepo.getNamespace(), "employeeType");
        ItemPath employeeTypePath = new ItemPath(ShadowType.F_ATTRIBUTES, employeeTypeQName);
        PrismProperty item = shadow.findProperty(employeeTypePath);

        PropertyDelta deleteDelta = new PropertyDelta(new ItemPath(ShadowType.F_ATTRIBUTES), item.getDefinition().getName(), item.getDefinition(), prismContext);
//        PropertyDelta deleteDelta = PropertyDelta.createDelta(employeeTypePath, shadow.getDefinition());
//        PrismPropertyValue valToDelte = new PrismPropertyValue("A");
//        valToDelte.setParent(deleteDelta);
        Collection<PrismPropertyValue> values= item.getValues();
        for (PrismPropertyValue val : values){
        	if ("A".equals(val.getValue())){
        		deleteDelta.addValueToDelete(val.clone());
        	}
        }


        ObjectDelta delta = new ObjectDelta(ShadowType.class, ChangeType.MODIFY, prismContext);
        delta.addModification(deleteDelta);
        delta.setOid(accountShadowOidGuybrushOpendj);
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        deltas.add(delta);
        LOGGER.info("-------->>EXECUTE DELETE MODIFICATION<<------------");
        modelService.executeChanges(deltas, null, task, parentResult);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // check if LDAP account was modified

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        OpenDJController.assertAttribute(entry, "roomNumber", "captain's cabin");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Bloody Pirate", "Honorable Captain");
        OpenDJController.assertAttribute(entry, "carLicense", "C4PT41N");
        OpenDJController.assertAttribute(entry, "businessCategory", "loot", "murder", "cruise", "fighting", "capsize");
        // Expression in the role taking that from the user
        OpenDJController.assertAttribute(entry, "destinationIndicator", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "departmentNumber", "Department of Guybrush");
        // Expression in the role taking that from the assignments (both of them)
        OpenDJController.assertAttribute(entry, "physicalDeliveryOfficeName", "The Sea Monkey", "The Dainty Lady");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword disappeared", guybrushPassword);

    }

    /**
     * Judge role excludes pirate role. This assignment should fail.
     */
    @Test
    public void test104AssignRoleJudge() throws Exception {
    	final String TEST_NAME = "test104AssignRoleJudge";
        displayTestTitle(TEST_NAME);

        // GIVEN

        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
        Holder<String> oidHolder = new Holder<String>();
        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_JUDGE_FILENAME, ObjectDeltaType.class);
        try {

            // WHEN ObjectTypes.USER.getTypeQName(),
        	result = modifyObjectViaModelWS(objectChange);

            // THEN
        	AssertJUnit.fail("Expected a failure after assigning conflicting roles but nothing happened and life goes on");
        } catch (FaultMessage f) {
        	// This is expected
        	// TODO: check if the fault is the right one
        }

        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object remain unmodified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        assertEquals("Unexpected number or accountRefs", 1, accountRefs.size());

    }


    @Test
    public void test107UnassignRolePirate() throws Exception {
    	final String TEST_NAME = "test107UnassignRolePirate";
        displayTestTitle(TEST_NAME);

        // GIVEN

        OperationResultType result = new OperationResultType();
        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_PIRATE_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);


        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> aObject = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
        		null, repoResult);
        ShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Honorable Captain");
        OpenDJController.assertAttribute(entry, "carLicense", "C4PT41N");
        OpenDJController.assertAttribute(entry, "businessCategory", "cruise", "fighting", "capsize");
        // Expression in the role taking that from the user
        OpenDJController.assertAttribute(entry, "destinationIndicator", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "departmentNumber", "Department of Guybrush");
        // Expression in the role taking that from the assignments (both of them)
        OpenDJController.assertAttribute(entry, "physicalDeliveryOfficeName", "The Sea Monkey", "The Dainty Lady");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword disappeared", guybrushPassword);

        // TODO: Derby


    }

    @Test
    public void test108UnassignRoleCaptain() throws Exception {
    	final String TEST_NAME = "test108UnassignRoleCaptain";
        displayTestTitle(TEST_NAME);

        // GIVEN

        OperationResultType result = new OperationResultType();
        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_1_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);


        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
        		null, repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check if account is still in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Honorable Captain");
        OpenDJController.assertAttribute(entry, "carLicense", "C4PT41N");
        OpenDJController.assertAttribute(entry, "businessCategory", "cruise", "fighting", "capsize");
        // Expression in the role taking that from the user
        OpenDJController.assertAttribute(entry, "destinationIndicator", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "departmentNumber", "Department of Guybrush");
        // Expression in the role taking that from the assignments (both of them)
        OpenDJController.assertAttribute(entry, "physicalDeliveryOfficeName", "The Dainty Lady");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword disappeared", guybrushPassword);

        // TODO: Derby


    }

    /**
     * Captain role was assigned twice. It has to also be unassigned twice.
     */
    @Test
    public void test109UnassignRoleCaptainAgain() throws Exception {
    	final String TEST_NAME = "test109UnassignRoleCaptainAgain";
        displayTestTitle(TEST_NAME);

        // GIVEN

        OperationResultType result = new OperationResultType();
        assertNoRepoCache();

        ObjectDeltaType objectChange = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_2_FILENAME, ObjectDeltaType.class);

        // WHEN ObjectTypes.USER.getTypeQName(),
        result = modifyObjectViaModelWS(objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);

        //TODO TODO TODO TODO operation result from repostiory.getObject is unknown...find out why..
//        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUserType);

        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals(0, accountRefs.size());

        // Check if shadow was deleted from the repo

        repoResult = new OperationResult("getObject");

        try {
            PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
            		null, repoResult);
            AssertJUnit.fail("Account shadow was not deleted from repo");
        } catch (ObjectNotFoundException ex) {
            // This is expected
        }

        // check if account was deleted from LDAP

        Entry entry = openDJController.searchByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        assertNull("LDAP account was not deleted", entry);

        // TODO: Derby

    }


    // Synchronization tests

    /**
     * Test initialization of synchronization. It will create a cycle task and
     * check if the cycle executes No changes are synchronized yet.
     */
    @Test
    public void test300LiveSyncInit() throws Exception {
    	final String TEST_NAME = "test300LiveSyncInit";
        displayTestTitle(TEST_NAME);
        // Now it is the right time to add task definition to the repository
        // We don't want it there any sooner, as it may interfere with the
        // previous tests

        checkAllShadows();

        // IMPORTANT! Assignment enforcement is POSITIVE now
        setAssignmentEnforcement(AssignmentPolicyEnforcementType.POSITIVE);
        // This is not redundant. It checks that the previous command set the policy correctly
        assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.POSITIVE);

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);

        repoAddObjectFromFile(TASK_OPENDJ_SYNC_FILENAME, result);


        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task

        waitFor("Waiting for task manager to pick up the task", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
                display("Task while waiting for task manager to pick up the task", task);
                // wait until the task is picked up
                return task.getLastRunFinishTimestamp() != null;
//                if (TaskExclusivityStatus.CLAIMED == task.getExclusivityStatus()) {
//                    // wait until the first run is finished
//                    if (task.getLastRunFinishTimestamp() == null) {
//                        return false;
//                    }
//                    return true;
//                }
//                return false;
            }

            @Override
            public void timeout() {
                // No reaction, the test will fail right after return from this
            }
        }, 20000);

        // Check task status

        Task task = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        result.computeStatus();
        display("getTask result", result);
        TestUtil.assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, TASK_OPENDJ_SYNC_OID, null, result);
        display("Task after pickup in the repository", o.asObjectable());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and claimed
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        // .. and last run should not be zero
        assertNotNull("No lastRunStartTimestamp", task.getLastRunStartTimestamp());
        assertFalse("Zero lastRunStartTimestamp", task.getLastRunStartTimestamp().longValue() == 0);
        assertNotNull("No lastRunFinishedTimestamp", task.getLastRunFinishTimestamp());
        assertFalse("Zero lastRunFinishedTimestamp", task.getLastRunFinishTimestamp().longValue() == 0);

        // Test for extension. This will also roughly test extension processor
        // and schema processor
        PrismContainer<?> taskExtension = task.getExtension();
        AssertJUnit.assertNotNull(taskExtension);
        display("Task extension", taskExtension);
        PrismProperty<String> shipStateProp = taskExtension.findProperty(MY_SHIP_STATE);
        AssertJUnit.assertEquals("Wrong 'shipState' property value", "capsized", shipStateProp.getValue().getValue());
        PrismProperty<Integer> deadProp = taskExtension.findProperty(MY_DEAD);
        PrismPropertyValue<Integer> deadPVal = deadProp.getValues().iterator().next();
        AssertJUnit.assertEquals("Wrong 'dead' property class", Integer.class, deadPVal.getValue().getClass());
        AssertJUnit.assertEquals("Wrong 'dead' property value", Integer.valueOf(42), deadPVal.getValue());

        // The progress should be 0, as there were no changes yet
        AssertJUnit.assertEquals(0, task.getProgress());

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);

         assertTrue(taskResult.isSuccess());

         final Object tokenAfter = findSyncToken(task);
         display("Sync token after", tokenAfter.toString());
         lastSyncToken = (Integer)tokenAfter;

         checkAllShadows();
    }

    /**
     * Create LDAP object. That should be picked up by liveSync and a user
     * should be created in repo.
     */
    @Test
    public void test301LiveSyncCreate() throws Exception {
    	final String TEST_NAME = "test301LiveSyncCreate";
        displayTestTitle(TEST_NAME);
        // Sync task should be running (tested in previous test), so just create
        // new LDAP object.

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        final Object tokenBefore = findSyncToken(syncCycle);
        display("Sync token before", tokenBefore.toString());

        // WHEN

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_WILL_FILENAME);
        display("Entry from LDIF", entry);

        // THEN

        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, 4, result);

        // Search for the user that should be created now
        UserType user = searchUserByName(WILL_NAME);

        PrismAsserts.assertEqualsPolyString("Wrong name.",  WILL_NAME, user.getName());
        assertNotNull(user.getLinkRef());
        assertFalse(user.getLinkRef().isEmpty());
//        AssertJUnit.assertEquals(user.getName(), WILL_NAME);

        // TODO: more checks

        assertAndStoreSyncTokenIncrement(syncCycle, 4);
        checkAllShadows();
    }

    @Test
    public void test302LiveSyncModify() throws Exception {
    	final String TEST_NAME = "test302LiveSyncModify";
        displayTestTitle(TEST_NAME);

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        int tokenBefore = findSyncToken(syncCycle);
        display("Sync token before", tokenBefore);

        // WHEN
        display("Modifying LDAP entry");
        ChangeRecordEntry entry = openDJController.executeLdifChange(LDIF_WILL_MODIFY_FILE);

        // THEN
        display("Entry from LDIF", entry);

        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, 1, result);
        // Search for the user that should be created now
        UserType user = searchUserByName (WILL_NAME);

//        AssertJUnit.assertEquals(WILL_NAME, user.getName());
        PrismAsserts.assertEqualsPolyString("Wrong name.",  WILL_NAME, user.getName());
        PrismAsserts.assertEqualsPolyString("wrong givenName", "asdf", user.getGivenName());

        assertAndStoreSyncTokenIncrement(syncCycle, 1);
        checkAllShadows();
    }

    @Test
    public void test303LiveSyncLink() throws Exception {
    	final String TEST_NAME = "test303LiveSyncLink";
        displayTestTitle(TEST_NAME);

        // GIVEN
        assertNoRepoCache();
        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_E_LINK_ACTION_FILE);
        UserType userType = user.asObjectable();
        final String userOid = userType.getOid();
        // Encrypt e's password
        protector.encrypt(userType.getCredentials().getPassword().getValue());
        // create user in repository
        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<String> oidHolder = new Holder<String>();
        display("Adding user object", userType);
        addObjectViaModelWS(userType, null, oidHolder, resultHolder);
        //check results
        assertNoRepoCache();
        displayJaxb("addObject result:", resultHolder.value, SchemaConstants.C_RESULT);
        TestUtil.assertSuccess("addObject has failed", resultHolder.value);
//        AssertJUnit.assertEquals(userOid, oidHolder.value);

        //WHEN
        //create account for e which should be correlated
        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        int tokenBefore = findSyncToken(syncCycle);
        display("Sync token before", tokenBefore);

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_E_FILENAME_LINK);
        display("Entry from LDIF", entry);

        // THEN
        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, 4, result);

        //check user and account ref
        userType = searchUserByName("e");

        List<ObjectReferenceType> accountRefs = userType.getLinkRef();
        assertEquals("Account ref not found, or found too many", 1, accountRefs.size());

        //check account defined by account ref
        String accountOid = accountRefs.get(0).getOid();
        ShadowType account = searchAccountByOid(accountOid);

        assertEqualsPolyString("Name doesn't match",  "uid=e,ou=People,dc=example,dc=com", account.getName());

        assertAndStoreSyncTokenIncrement(syncCycle, 4);
        checkAllShadows();
    }

    /**
     * Create LDAP object. That should be picked up by liveSync and a user
     * should be created in repo.
     * Also location (ldap l) should be updated through outbound
     */
    @Test
    public void test304LiveSyncCreateNoLocation() throws Exception {
    	final String TEST_NAME = "test304LiveSyncCreateNoLocation";
        displayTestTitle(TEST_NAME);
        // Sync task should be running (tested in previous test), so just create
        // new LDAP object.

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        int tokenBefore = findSyncToken(syncCycle);
        display("Sync token before", tokenBefore);

        // WHEN
        Entry entry = openDJController.addEntryFromLdifFile(LDIF_WILL_WITHOUT_LOCATION_FILENAME);
        display("Entry from LDIF", entry);

        // THEN
        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, 3, result, 60000);
        // Search for the user that should be created now
        final String userName = "wturner1";
        UserType user = searchUserByName(userName);

        List<ObjectReferenceType> accountRefs = user.getLinkRef();
        assertEquals("Account ref not found, or found too many", 1, accountRefs.size());

        //check account defined by account ref
        String accountOid = accountRefs.get(0).getOid();
        ShadowType account = searchAccountByOid(accountOid);

        assertEqualsPolyString("Name doesn't match",  "uid=" + userName + ",ou=People,dc=example,dc=com", account.getName());
//        assertEquals("Name doesn't match", "uid=" + userName + ",ou=People,dc=example,dc=com", account.getName());
        Collection<String> localities = getAttributeValues(account, new QName(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS.getNamespaceURI(), "l"));
        assertNotNull("null value list for attribute 'l'", localities);
        assertEquals("unexpected number of values of attribute 'l'", 1, localities.size());
        assertEquals("Locality doesn't match", "middle of nowhere", localities.iterator().next());

        assertAndStoreSyncTokenIncrement(syncCycle, 3);
        checkAllShadows();
    }

	private void assertAndStoreSyncTokenIncrement(Task syncCycle, int increment) {
        final Object tokenAfter = findSyncToken(syncCycle);
        display("Sync token after", tokenAfter.toString());
        int tokenAfterInt = (Integer)tokenAfter;
        int expectedToken = lastSyncToken + increment;
        lastSyncToken = tokenAfterInt;
        assertEquals("Unexpected sync toke value", expectedToken, tokenAfterInt);
	}

    private int findSyncToken(Task syncCycle) {
    	return (Integer)findSyncTokenObject(syncCycle);
    }

    private Object findSyncTokenObject(Task syncCycle) {
        Object token = null;
        PrismProperty<?> tokenProperty = syncCycle.getExtension().findProperty(SchemaConstants.SYNC_TOKEN);
        if (tokenProperty != null) {
        	Collection<?> values = tokenProperty.getRealValues();
        	if (values.size() > 1) {
        		throw new IllegalStateException("Too must values in token "+tokenProperty);
        	}
            token = values.iterator().next();
        }

        return token;
    }

    /**
     * Not really a test. Just cleans up after live sync.
     */
    @Test
    public void test399LiveSyncCleanup() throws Exception {
    	final String TEST_NAME = "test399LiveSyncCleanup";
        displayTestTitle(TEST_NAME);
        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);

        taskManager.deleteTask(TASK_OPENDJ_SYNC_OID, result);

        // TODO: check if the task is really stopped
    }

    @Test
    public void test400ImportFromResource() throws Exception {
    	final String TEST_NAME = "test400ImportFromResource";
        displayTestTitle(TEST_NAME);
        // GIVEN
        checkAllShadows();
        assertNoRepoCache();

        OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);

        // Make sure Mr. Gibbs has "l" attribute set to the same value as an outbound expression is setting
        ChangeRecordEntry entry = openDJController.executeLdifChange(LDIF_GIBBS_MODIFY_FILE);
        display("Entry from LDIF", entry);

        // Let's add an entry with multiple uids.
        Entry addEntry = openDJController.addEntryFromLdifFile(LDIF_HERMAN_FILENAME);
        display("Entry from LDIF", addEntry);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        TaskType taskType = modelWeb.importFromResource(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        assertNoRepoCache();
        displayJaxb("importFromResource result", taskType.getResult(), SchemaConstants.C_RESULT);
        AssertJUnit.assertEquals("importFromResource has failed", OperationResultStatusType.IN_PROGRESS, taskType.getResult().getStatus());
        // Convert the returned TaskType to a more usable Task
        Task task = taskManager.createTaskInstance(taskType.asPrismObject(), result);
        AssertJUnit.assertNotNull(task);
        assertNotNull(task.getOid());
        AssertJUnit.assertTrue(task.isAsynchronous());
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        display("Import task after launch", task);

        PrismObject<TaskType> tObject = repositoryService.getObject(TaskType.class, task.getOid(), null, result);
        TaskType taskAfter = tObject.asObjectable();
        display("Import task in repo after launch", taskAfter);

        result.computeStatus();
        TestUtil.assertSuccess("getObject has failed", result);

        final String taskOid = task.getOid();

        waitFor("Waiting for import to complete", new Checker() {
            @Override
            public boolean check() throws CommonException {
                Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
                Holder<ObjectType> objectHolder = new Holder<ObjectType>();
                OperationResult opResult = new OperationResult("import check");
                assertNoRepoCache();
                SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
				try {
					modelWeb.getObject(ObjectTypes.TASK.getTypeQName(), taskOid,
							options, objectHolder, resultHolder);
				} catch (FaultMessage faultMessage) {
					throw new SystemException(faultMessage);
				}
				assertNoRepoCache();
                //				display("getObject result (wait loop)",resultHolder.value);
                TestUtil.assertSuccess("getObject has failed", resultHolder.value);
                Task task = taskManager.createTaskInstance((PrismObject<TaskType>) objectHolder.value.asPrismObject(), opResult);
                System.out.println(new Date() + ": Import task status: " + task.getExecutionStatus() + ", progress: " + task.getProgress());
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
        }, 180000);

        // wait a second until the task will be definitely saved
        Thread.sleep(1000);

        //### Check task state after the task is finished ###

        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        assertNoRepoCache();

        modelWeb.getObject(ObjectTypes.TASK.getTypeQName(), task.getOid(),
                options, objectHolder, resultHolder);

        assertNoRepoCache();
        TestUtil.assertSuccess("getObject has failed", resultHolder.value);
        task = taskManager.createTaskInstance((PrismObject<TaskType>) objectHolder.value.asPrismObject(), result);

        display("Import task after finish (fetched from model)", task);

        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        assertNotNull("Null lastRunStartTimestamp in "+task, task.getLastRunStartTimestamp());
        assertNotNull("Null lastRunFinishTimestamp in "+task, task.getLastRunFinishTimestamp());

        long importDuration = task.getLastRunFinishTimestamp() - task.getLastRunStartTimestamp();
        double usersPerSec = (task.getProgress() * 1000) / importDuration;
        display("Imported " + task.getProgress() + " users in " + importDuration + " milliseconds (" + usersPerSec + " users/sec)");

        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task has no result", taskResult);
        TestUtil.assertSuccess("Import task result is not success", taskResult);
        AssertJUnit.assertTrue("Task failed", taskResult.isSuccess());

        AssertJUnit.assertTrue("No progress", task.getProgress() > 0);

        //### Check if the import created users and shadows ###

        // Listing of shadows is not supported by the provisioning. So we need
        // to look directly into repository
        List<PrismObject<ShadowType>> sobjects = repositoryService.searchObjects(ShadowType.class, null, null, result);
        result.computeStatus();
        TestUtil.assertSuccess("listObjects has failed", result);
        AssertJUnit.assertFalse("No shadows created", sobjects.isEmpty());

        for (PrismObject<ShadowType> aObject : sobjects) {
            ShadowType shadow = aObject.asObjectable();
            display("Shadow object after import (repo)", shadow);
            assertNotEmpty("No OID in shadow", shadow.getOid()); // This would be really strange ;-)
            assertNotEmpty("No name in shadow", shadow.getName());
            AssertJUnit.assertNotNull("No objectclass in shadow", shadow.getObjectClass());
            AssertJUnit.assertNotNull("Null attributes in shadow", shadow.getAttributes());
            String resourceOid = shadow.getResourceRef().getOid();
            if (resourceOid.equals(RESOURCE_OPENDJ_OID)) {
            	assertAttributeNotNull("No identifier in shadow", shadow, getOpenDjPrimaryIdentifierQName());
            } else {
            	assertAttributeNotNull("No UID in shadow", shadow, SchemaConstants.ICFS_UID);
            }
        }

        Holder<ObjectListType> listHolder = new Holder<>();
        assertNoRepoCache();

        modelWeb.searchObjects(ObjectTypes.USER.getTypeQName(), null, null,
                listHolder, resultHolder);

        assertNoRepoCache();
        ObjectListType uobjects = listHolder.value;
        TestUtil.assertSuccess("listObjects has failed", resultHolder.value);
        AssertJUnit.assertFalse("No users created", uobjects.getObject().isEmpty());

        // TODO: use another account, not guybrush

        display("Users after import "+uobjects.getObject().size());

        for (ObjectType oo : uobjects.getObject()) {
            UserType user = (UserType) oo;
            if (SystemObjectsType.USER_ADMINISTRATOR.value().equals(user.getOid())) {
                //skip administrator check
                continue;
            }
            display("User after import (repo)", user);
            assertNotEmpty("No OID in user", user.getOid()); // This would be
            // really
            // strange ;-)
            assertNotEmpty("No name in user", user.getName());
            assertNotNull("No fullName in user", user.getFullName());
            assertNotEmpty("No fullName in user", user.getFullName().getOrig());
            assertNotEmpty("No familyName in user", user.getFamilyName().getOrig());
            // givenName is not mandatory in LDAP, therefore givenName may not
            // be present on user

            if (user.getName().getOrig().equals(USER_GUYBRUSH_USERNAME)) {
            	// skip the rest of checks for guybrush, he does not have LDAP account now
            	continue;
            }

            assertTrue("User "+user.getName()+" is disabled ("+user.getActivation().getAdministrativeStatus()+")", user.getActivation() == null ||
            		user.getActivation().getAdministrativeStatus() == ActivationStatusType.ENABLED);

            List<ObjectReferenceType> accountRefs = user.getLinkRef();
            AssertJUnit.assertEquals("Wrong accountRef for user " + user.getName(), 1, accountRefs.size());
            ObjectReferenceType accountRef = accountRefs.get(0);

            boolean found = false;
            for (PrismObject<ShadowType> aObject : sobjects) {
                ShadowType acc = aObject.asObjectable();
                if (accountRef.getOid().equals(acc.getOid())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                AssertJUnit.fail("accountRef does not point to existing account " + accountRef.getOid());
            }

            PrismObject<ShadowType> aObject = modelService.getObject(ShadowType.class, accountRef.getOid(), null, task, result);
            ShadowType account = aObject.asObjectable();

            display("Account after import ", account);

            String attributeValueL = ShadowUtil.getMultiStringAttributeValueAsSingle(account,
            		new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "l"));
//            assertEquals("Unexcpected value of l", "middle of nowhere", attributeValueL);
            assertEquals("Unexcpected value of l", getUserLocality(user), attributeValueL);
        }

        // This also includes "idm" user imported from LDAP. Later we need to ignore that one.
        assertEquals("Wrong number of users after import", 10, uobjects.getObject().size());

        checkAllShadows();

    }

    private String getUserLocality(UserType user){
    	return user.getLocality() != null ? user.getLocality().getOrig() :"middle of nowhere";
    }

    @Test
    public void test420RecomputeUsers() throws Exception {
    	final String TEST_NAME = "test420RecomputeUsers";
        displayTestTitle(TEST_NAME);
        // GIVEN

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);

        // Assign role to a user, but we do this using a repository instead of model.
        // The role assignment will not be executed and this created an inconsistent state.
        ObjectDeltaType changeAddRoleCaptain = unmarshallValueFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_1_FILENAME, ObjectDeltaType.class);
        Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(changeAddRoleCaptain.getItemDelta(),
        		getUserDefinition());
        repositoryService.modifyObject(UserType.class, changeAddRoleCaptain.getOid(), modifications, result);


        // TODO: setup more "inconsistent" state


        // Add reconciliation task. This will trigger reconciliation

        importObjectFromFile(TASK_USER_RECOMPUTE_FILENAME, result);

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task

        waitFor("Waiting for task to finish", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(TASK_USER_RECOMPUTE_OID, result);
                //display("Task while waiting for task manager to pick up the task", task);
                // wait until the task is finished
                if (TaskExecutionStatus.CLOSED == task.getExecutionStatus()) {
                    return true;
                }
                return false;
            }

            @Override
            public void timeout() {
                // No reaction, the test will fail right after return from this
            }
        }, 40000);

        // wait a second until the task will be definitely saved
        Thread.sleep(1000);

        // Check task status

        Task task = taskManager.getTask(TASK_USER_RECOMPUTE_OID, result);
        result.computeStatus();
        display("getTask result", result);
        TestUtil.assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after finish", task);
        AssertJUnit.assertNotNull(task.getTaskIdentifier());
        assertFalse(task.getTaskIdentifier().isEmpty());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, TASK_USER_RECOMPUTE_OID, null, result);
        display("Task after pickup in the repository", o.asObjectable());

        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and last run should not be zero
        assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        AssertJUnit.assertEquals(10, task.getProgress());

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        display("Recompute task result", taskResult);
        AssertJUnit.assertNotNull(taskResult);
        TestUtil.assertSuccess("Recompute task result", taskResult);

        // STOP the task. We don't need it any more and we don't want to give it a chance to run more than once
        taskManager.deleteTask(TASK_USER_RECOMPUTE_OID, result);

        // CHECK RESULT: account created for user guybrush

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> object = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUser = object.asObjectable();

        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        assertEquals("Wrong number of accountRefs after recompute for user "+repoUser.getName(), 1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

         PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
        		 null, repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadowType, new QName("shadow"));
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        accountGuybrushOpendjEntryUuuid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Honorable Captain");
        OpenDJController.assertAttribute(entry, "carLicense", "C4PT41N");
        OpenDJController.assertAttribute(entry, "businessCategory", "cruise");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword was not set on create", guybrushPassword);

        checkAllShadows();
    }

    @Test
    public void test440ReconcileResourceOpenDj() throws Exception {
    	final String TEST_NAME = "test440ReconcileResourceOpenDj";
        displayTestTitle(TEST_NAME);
        // GIVEN

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + "." + TEST_NAME);

        // Create LDAP account without an owner. The liveSync is off, so it will not be picked up

        Entry ldifEntry = openDJController.addEntryFromLdifFile(LDIF_ELAINE_FILENAME);
        display("Entry from LDIF", ldifEntry);

        // Guybrush's attributes were set up by a role in the previous test. Let's mess the up a bit. Recon should sort it out.

        List<RawModification> modifications = new ArrayList<RawModification>();
        // Expect that a correct title will be added to this one
        RawModification titleMod = RawModification.create(ModificationType.REPLACE, "title", "Scurvy earthworm");
        modifications.add(titleMod);
        // Expect that the correct location will replace this one
        RawModification lMod = RawModification.create(ModificationType.REPLACE, "l", "Davie Jones' locker");
        modifications.add(lMod);
        // Expect that this will be untouched
        RawModification poMod = RawModification.create(ModificationType.REPLACE, "postOfficeBox", "X marks the spot");
        modifications.add(poMod);
        ModifyOperation modifyOperation = openDJController.getInternalConnection().processModify(USER_GUYBRUSH_LDAP_DN, modifications);
        if (ResultCode.SUCCESS != modifyOperation.getResultCode()) {
            AssertJUnit.fail("LDAP operation failed: " + modifyOperation.getErrorMessage());
        }

        // TODO: setup more "inconsistent" state

        // Add reconciliation task. This will trigger reconciliation

        repoAddObjectFromFile(TASK_OPENDJ_RECON_FILENAME, result);


        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task

        waitFor("Waiting for task to finish first run", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(TASK_OPENDJ_RECON_OID, result);
                display("Task while waiting for task manager to pick up the task", task);
                // wait until the task is finished
                return task.getLastRunFinishTimestamp() != null;
            }

            @Override
            public void timeout() {
                // No reaction, the test will fail right after return from this
            }
        }, 180000);

        // Check task status

        Task task = taskManager.getTask(TASK_OPENDJ_RECON_OID, result);
        result.computeStatus();
        display("getTask result", result);
        TestUtil.assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, TASK_OPENDJ_RECON_OID, null, result);
        display("Task after pickup in the repository", o.asObjectable());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and claimed
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        // .. and last run should not be zero
        assertNotNull("Null last run start in recon task", task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse("Zero last run start in recon task", task.getLastRunStartTimestamp().longValue() == 0);
        assertNotNull("Null last run finish in recon task", task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse("Zero last run finish in recon task", task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be 0, as there were no changes yet
        // [pm] commented out, as progress in recon task is now determined not only using # of changes
        //AssertJUnit.assertEquals(0, task.getProgress());

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);

        // STOP the task. We don't need it any more and we don't want to give it a chance to run more than once
        taskManager.deleteTask(TASK_OPENDJ_RECON_OID, result);

        // CHECK RESULT: account for user guybrush should be still there and unchanged

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, null, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getLinkRef();
        assertEquals("Guybrush has wrong number of accounts", 1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidGuybrushOpendj,
        		null, repoResult);
        ShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadowType, new QName("shadow"));
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        accountGuybrushOpendjEntryUuuid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        Entry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object. It is not tolerant, therefore the other value should be gone now
        OpenDJController.assertAttribute(entry, "l", "Deep in the Caribbean");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");

        // "title" is tolerant, so it will retain the original value as well as the one provided by the role
        OpenDJController.assertAttribute(entry, "title", "Scurvy earthworm", "Honorable Captain");

        OpenDJController.assertAttribute(entry, "carLicense", "C4PT41N");
        OpenDJController.assertAttribute(entry, "businessCategory", "cruise");

        // No setting for "postOfficeBox", so the value should be unchanged
        OpenDJController.assertAttribute(entry, "postOfficeBox", "X marks the spot");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword was not set on create", guybrushPassword);


//        QueryType query = QueryUtil.createNameQuery(ELAINE_NAME);
//        ObjectQuery query = ObjectQuery.createObjectQuery(EqualsFilter.createEqual(UserType.class, prismContext, UserType.F_NAME, ELAINE_NAME));
        ObjectQuery query = ObjectQueryUtil.createNameQuery(ELAINE_NAME, prismContext);
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, repoResult);
        assertEquals("Wrong number of Elaines", 1, users.size());
        repoUser = users.get(0).asObjectable();

        repoResult.computeStatus();
        displayJaxb("User Elaine (repository)", repoUser, new QName("user"));

        assertNotNull(repoUser.getOid());
        assertEquals(PrismTestUtil.createPolyStringType(ELAINE_NAME), repoUser.getName());
        PrismAsserts.assertEqualsPolyString("wrong repo givenName", "Elaine", repoUser.getGivenName());
        PrismAsserts.assertEqualsPolyString("wrong repo familyName", "Marley", repoUser.getFamilyName());
        PrismAsserts.assertEqualsPolyString("wrong repo fullName", "Elaine Marley", repoUser.getFullName());

        accountRefs = repoUser.getLinkRef();
        assertEquals("Elaine has wrong number of accounts", 1, accountRefs.size());
        accountRef = accountRefs.get(0);
        String accountShadowOidElaineOpendj = accountRef.getOid();
        assertFalse(accountShadowOidElaineOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        repoShadow = repositoryService.getObject(ShadowType.class, accountShadowOidElaineOpendj,
        		null, repoResult);
        repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadowType, new QName("shadow"));
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        String accountElainehOpendjEntryUuuid = checkRepoShadow(repoShadow);

        // check if account is still in LDAP

        entry = openDJController.searchAndAssertByEntryUuid(accountElainehOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", ELAINE_NAME);
        OpenDJController.assertAttribute(entry, "givenName", "Elaine");
        OpenDJController.assertAttribute(entry, "sn", "Marley");
        OpenDJController.assertAttribute(entry, "cn", "Elaine Marley");
        OpenDJController.assertAttribute(entry, "displayName", "Elaine Marley");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        // FIXME
        //OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "governor");
        OpenDJController.assertAttribute(entry, "title", "Governor");
        OpenDJController.assertAttribute(entry, "businessCategory", "state");

        String elainePassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Password of Elaine has disappeared", elainePassword);

        checkAllShadows();
    }

    @Test
    public void test480ListResources() throws Exception {
    	final String TEST_NAME = "test480ListResources";
        displayTestTitle(TEST_NAME);
        // GIVEN
        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);

        Holder<ObjectListType> objectListHolder = new Holder<ObjectListType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();

		// WHEN
        modelWeb.searchObjects(ObjectTypes.RESOURCE.getTypeQName(), null, options, objectListHolder, resultHolder);

        // THEN

        display("Resources", objectListHolder.value);
        assertEquals("Unexpected number of resources", 4, objectListHolder.value.getObject().size());
        // TODO

        for(ObjectType object: objectListHolder.value.getObject()) {
        	// Marshalling may fail even though the Java object is OK so test for it
        	String xml = prismContext.serializeObjectToString(object.asPrismObject(), PrismContext.LANG_XML);
        }

    }

    @Test
    public void test485ListResourcesWithBrokenResource() throws Exception {
        TestUtil.displayTestTitle("test485ListResourcesWithBrokenResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSanity.class.getName() + ".test410ListResourcesWithBrokenResource");
        final OperationResult result = task.getResult();

        // WHEN
        List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, new ObjectQuery(), null, task, result);

        // THEN
        assertNotNull("listObjects returned null list", resources);

        for (PrismObject<ResourceType> object : resources) {
            ResourceType resource = object.asObjectable();
            //display("Resource found",resource);
            display("Found " + ObjectTypeUtil.toShortString(resource) + ", result " + (resource.getFetchResult() == null ? "null" : resource.getFetchResult().getStatus()));

            assertNotNull(resource.getOid());
            assertNotNull(resource.getName());

            if (resource.getOid().equals(RESOURCE_BROKEN_OID)) {
                assertTrue("No error in fetchResult in " + ObjectTypeUtil.toShortString(resource),
                        resource.getFetchResult() != null &&
                                (resource.getFetchResult().getStatus() == OperationResultStatusType.PARTIAL_ERROR ||
                                        resource.getFetchResult().getStatus() == OperationResultStatusType.FATAL_ERROR));
            } else {
                assertTrue("Unexpected error in fetchResult in " + ObjectTypeUtil.toShortString(resource),
                        resource.getFetchResult() == null || resource.getFetchResult().getStatus() == OperationResultStatusType.SUCCESS);
            }
        }
    }

    @Test
    public void test500NotifyChangeCreateAccount() throws Exception{
    	final String TEST_NAME = "test500NotifyChangeCreateAccount";
		displayTestTitle(TEST_NAME);

    	Entry ldifEntry = openDJController.addEntryFromLdifFile(LDIF_ANGELIKA_FILENAME);
        display("Entry from LDIF", ldifEntry);

        List<Attribute> attributes = ldifEntry.getAttributes();
        List<Attribute> attrs = ldifEntry.getAttribute("entryUUID");

        AttributeValue val = null;
        if (attrs == null){
        	for (Attribute a : attributes){
        		if (a.getName().equals("entryUUID")){
        			val = a.iterator().next();
        		}
        	}
        } else{
        	val = attrs.get(0).iterator().next();
        }

        String entryUuid = val.toString();

        ShadowType anglicaAccount = parseObjectType(new File(ACCOUNT_ANGELIKA_FILENAME), ShadowType.class);
        PrismProperty<String> prop = anglicaAccount.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES).getValue().createProperty(
        		new PrismPropertyDefinitionImpl<>(getOpenDjPrimaryIdentifierQName(), DOMUtil.XSD_STRING, prismContext));
    	prop.setValue(new PrismPropertyValue<>(entryUuid));
    	anglicaAccount.setResourceRef(ObjectTypeUtil.createObjectRef(RESOURCE_OPENDJ_OID, ObjectTypes.RESOURCE));

    	display("Angelica shadow: ", anglicaAccount.asPrismObject().debugDump());

    	ResourceObjectShadowChangeDescriptionType changeDescription = new ResourceObjectShadowChangeDescriptionType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setChangeType(ChangeTypeType.ADD);
    	delta.setObjectToAdd(anglicaAccount);
    	delta.setObjectType(ShadowType.COMPLEX_TYPE);
    	changeDescription.setObjectDelta(delta);
    	changeDescription.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);

    	// WHEN
    	TaskType task = modelWeb.notifyChange(changeDescription);

    	// THEN
    	OperationResult result = OperationResult.createOperationResult(task.getResult());
    	display(result);
    	assertSuccess(result);

    	PrismObject<UserType> userAngelika = findUserByUsername(ANGELIKA_NAME);
    	assertNotNull("User with the name angelika must exist.", userAngelika);

    	UserType user = userAngelika.asObjectable();
    	assertNotNull("User with the name angelika must have one link ref.", user.getLinkRef());

    	assertEquals("Expected one account ref in user", 1, user.getLinkRef().size());
    	String oid = user.getLinkRef().get(0).getOid();

    	PrismObject<ShadowType> modelShadow = modelService.getObject(ShadowType.class, oid, null, taskManager.createTaskInstance(), result);

    	assertAttributeNotNull(modelShadow, getOpenDjPrimaryIdentifierQName());
        assertAttribute(modelShadow, "uid", "angelika");
        assertAttribute(modelShadow, "givenName", "Angelika");
        assertAttribute(modelShadow, "sn", "Marley");
        assertAttribute(modelShadow, "cn", "Angelika Marley");

    }

    @Test
    public void test501NotifyChangeModifyAccount() throws Exception{
    	final String TEST_NAME = "test501NotifyChangeModifyAccount";
    	displayTestTitle(TEST_NAME);

		OperationResult parentResult = new OperationResult(TEST_NAME);
		PrismObject<UserType> userAngelika = findUserByUsername(ANGELIKA_NAME);
		assertNotNull("User with the name angelika must exist.", userAngelika);

		UserType user = userAngelika.asObjectable();
		assertNotNull("User with the name angelika must have one link ref.", user.getLinkRef());

		assertEquals("Expected one account ref in user", 1, user.getLinkRef().size());
		String oid = user.getLinkRef().get(0).getOid();

    	ResourceObjectShadowChangeDescriptionType changeDescription = new ResourceObjectShadowChangeDescriptionType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setChangeType(ChangeTypeType.MODIFY);
    	delta.setObjectType(ShadowType.COMPLEX_TYPE);

    	ItemDeltaType mod1 = new ItemDeltaType();
    	mod1.setModificationType(ModificationTypeType.REPLACE);
    	ItemPathType path = new ItemPathType(new ItemPath(ShadowType.F_ATTRIBUTES, new QName(resourceTypeOpenDjrepo.getNamespace(), "givenName")));
    	mod1.setPath(path);

    	RawType value = new RawType(new PrimitiveXNode<String>("newAngelika"), prismContext);
        mod1.getValue().add(value);

    	delta.getItemDelta().add(mod1);
    	delta.setOid(oid);

    	LOGGER.info("item delta: {}", SchemaDebugUtil.prettyPrint(mod1));

    	LOGGER.info("delta: {}", DebugUtil.dump(mod1));

    	changeDescription.setObjectDelta(delta);

    	changeDescription.setOldShadowOid(oid);
    	changeDescription.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);

    	// WHEN
    	TaskType task = modelWeb.notifyChange(changeDescription);

    	// THEN
    	OperationResult result = OperationResult.createOperationResult(task.getResult());
    	display(result);
    	assertSuccess(result);

    	PrismObject<UserType> userAngelikaAfterSync = findUserByUsername(ANGELIKA_NAME);
    	assertNotNull("User with the name angelika must exist.", userAngelikaAfterSync);

    	UserType userAfterSync = userAngelikaAfterSync.asObjectable();

    	PrismAsserts.assertEqualsPolyString("wrong given name in user angelika", PrismTestUtil.createPolyStringType("newAngelika"), userAfterSync.getGivenName());

    }

    @Test
    public void test502NotifyChangeModifyAccountPassword() throws Exception{
    	final String TEST_NAME = "test502NotifyChangeModifyAccountPassword";
    	displayTestTitle(TEST_NAME);

		PrismObject<UserType> userAngelika = findUserByUsername(ANGELIKA_NAME);
		assertNotNull("User with the name angelika must exist.", userAngelika);

		UserType user = userAngelika.asObjectable();
		assertNotNull("User with the name angelika must have one link ref.", user.getLinkRef());

		assertEquals("Expected one account ref in user", 1, user.getLinkRef().size());
		String oid = user.getLinkRef().get(0).getOid();

		String newPassword = "newPassword";

		openDJController.modifyReplace("uid="+ANGELIKA_NAME+","+openDJController.getSuffixPeople(), "userPassword", newPassword);

    	ResourceObjectShadowChangeDescriptionType changeDescription = new ResourceObjectShadowChangeDescriptionType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setChangeType(ChangeTypeType.MODIFY);
    	delta.setObjectType(ShadowType.COMPLEX_TYPE);

        ItemDeltaType passwordDelta = new ItemDeltaType();
        passwordDelta.setModificationType(ModificationTypeType.REPLACE);
        passwordDelta.setPath(ModelClientUtil.createItemPathType("credentials/password/value"));
        RawType passwordValue = new RawType(((PrismContextImpl) prismContext).getBeanMarshaller().marshall(ModelClientUtil.createProtectedString(newPassword)), prismContext);
        passwordDelta.getValue().add(passwordValue);

        delta.getItemDelta().add(passwordDelta);
    	delta.setOid(oid);

    	LOGGER.info("item delta: {}", SchemaDebugUtil.prettyPrint(passwordDelta));

    	LOGGER.info("delta: {}", DebugUtil.dump(passwordDelta));

    	changeDescription.setObjectDelta(delta);

    	changeDescription.setOldShadowOid(oid);
//    	changeDescription.setCurrentShadow(angelicaShadowType);
    	changeDescription.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);

    	// WHEN
    	TaskType task = modelWeb.notifyChange(changeDescription);

    	// THEN
    	OperationResult result = OperationResult.createOperationResult(task.getResult());
    	display(result);
    	assertSuccess(result);

    	PrismObject<UserType> userAngelikaAfterSync = findUserByUsername(ANGELIKA_NAME);
    	assertNotNull("User with the name angelika must exist.", userAngelikaAfterSync);

    	assertUserLdapPassword(userAngelikaAfterSync, newPassword);
    	
    }

    @Test
    public void test503NotifyChangeDeleteAccount() throws Exception{
    	final String TEST_NAME = "test503NotifyChangeDeleteAccount";
    	displayTestTitle(TEST_NAME);

		PrismObject<UserType> userAngelika = findUserByUsername(ANGELIKA_NAME);
		assertNotNull("User with the name angelika must exist.", userAngelika);

		UserType user = userAngelika.asObjectable();
		assertNotNull("User with the name angelika must have one link ref.", user.getLinkRef());

		assertEquals("Expected one account ref in user", 1, user.getLinkRef().size());
		String oid = user.getLinkRef().get(0).getOid();

    	ResourceObjectShadowChangeDescriptionType changeDescription = new ResourceObjectShadowChangeDescriptionType();
    	ObjectDeltaType delta = new ObjectDeltaType();
    	delta.setChangeType(ChangeTypeType.DELETE);
    	delta.setObjectType(ShadowType.COMPLEX_TYPE);
        delta.setOid(oid);
    	changeDescription.setObjectDelta(delta);
    	changeDescription.setOldShadowOid(oid);
    	changeDescription.setChannel(SchemaConstants.CHANNEL_WEB_SERVICE_URI);

    	// WHEN
    	TaskType task = modelWeb.notifyChange(changeDescription);

    	// THEN
    	OperationResult result = OperationResult.createOperationResult(task.getResult());
    	display(result);
    	assertTrue(result.isAcceptable());

    	PrismObject<UserType> userAngelikaAfterSync = findUserByUsername(ANGELIKA_NAME);
    	display("User after", userAngelikaAfterSync);
    	assertNotNull("User with the name angelika must exist.", userAngelikaAfterSync);

    	UserType userType = userAngelikaAfterSync.asObjectable();
    	assertNotNull("User with the name angelika must have one link ref.", userType.getLinkRef());

    	assertEquals("Expected no account ref in user", 0, userType.getLinkRef().size());

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

    // TODO: test for missing/corrupt system configuration
    // TODO: test for missing sample config (bad reference in expression
    // arguments)

	private String checkRepoShadow(PrismObject<ShadowType> repoShadow) {
		ShadowType repoShadowType = repoShadow.asObjectable();
		String uid = null;
        boolean hasOthers = false;
        List<Object> xmlAttributes = repoShadowType.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (SchemaConstants.ICFS_UID.equals(JAXBUtil.getElementQName(element)) || getOpenDjPrimaryIdentifierQName().equals(JAXBUtil.getElementQName(element))) {
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

	private ShadowType searchAccountByOid(final String accountOid) throws Exception {
        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<ObjectType> accountHolder = new Holder<ObjectType>();
        SelectorQualifiedGetOptionsType options = new SelectorQualifiedGetOptionsType();
        modelWeb.getObject(ObjectTypes.SHADOW.getTypeQName(), accountOid, options, accountHolder, resultHolder);
        ObjectType object = accountHolder.value;
        TestUtil.assertSuccess("searchObjects has failed", resultHolder.value);
        assertNotNull("Account is null", object);

        if (!(object instanceof ShadowType)) {
            fail("Object is not account.");
        }
        ShadowType account = (ShadowType) object;
        assertEquals(accountOid, account.getOid());

        return account;
    }

    private UserType searchUserByName(String name) throws Exception {
//        Document doc = DOMUtil.getDocument();
//        Element nameElement = doc.createElementNS(SchemaConstants.C_NAME.getNamespaceURI(),
//                SchemaConstants.C_NAME.getLocalPart());
//        nameElement.setTextContent(name);
//        Element filter = QueryUtil.createEqualFilter(doc, null, nameElement);
//
//        QueryType query = new QueryType();
//        query.setFilter(filter);
    	ObjectQuery q = ObjectQueryUtil.createNameQuery(UserType.class, prismContext, name);
    	QueryType query = QueryJaxbConvertor.createQueryType(q, prismContext);
        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<ObjectListType> listHolder = new Holder<ObjectListType>();
        assertNoRepoCache();

        modelWeb.searchObjects(ObjectTypes.USER.getTypeQName(), query, null, listHolder, resultHolder);

        assertNoRepoCache();
        ObjectListType objects = listHolder.value;
        TestUtil.assertSuccess("searchObjects has failed", resultHolder.value);
        AssertJUnit.assertEquals("User not found (or found too many)", 1, objects.getObject().size());
        UserType user = (UserType) objects.getObject().get(0);

        AssertJUnit.assertEquals(user.getName(), PrismTestUtil.createPolyStringType(name));

        return user;
    }

    private void basicWaitForSyncChangeDetection(Task syncCycle, Object tokenBefore, int increment,
            final OperationResult result) throws Exception {
    	basicWaitForSyncChangeDetection(syncCycle, (int)((Integer)tokenBefore), increment, result);
    }

    private void basicWaitForSyncChangeDetection(Task syncCycle, int tokenBefore, int increment,
            final OperationResult result) throws Exception {
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, increment, result, 40000);
    }

    private void basicWaitForSyncChangeDetection(final Task syncCycle, final int tokenBefore, final int increment,
            final OperationResult result, int timeout) throws Exception {

        waitFor("Waiting for sync cycle to detect change", new Checker() {
            @Override
            public boolean check() throws CommonException {
                syncCycle.refresh(result);
                display("SyncCycle while waiting for sync cycle to detect change", syncCycle);
                if (syncCycle.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                	throw new IllegalStateException("Task not runnable: "+syncCycle.getExecutionStatus()+"; "+syncCycle);
                }
                int tokenNow = findSyncToken(syncCycle);
                display("tokenNow = " + tokenNow);
                if (tokenNow >= tokenBefore + increment) {
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void timeout() {
                // No reaction, the test will fail right after return from this
            }
        }, timeout, WAIT_FOR_LOOP_SLEEP_MILIS);
    }

    private void setAssignmentEnforcement(AssignmentPolicyEnforcementType enforcementType) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		assumeAssignmentPolicy(enforcementType);
//    	AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
//        syncSettings.setAssignmentPolicyEnforcement(enforcementType);
//        applySyncSettings(SystemConfigurationType.class, syncSettings);
	}

    private void assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType assignmentPolicy) throws
            ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("Asserting sync settings");
        PrismObject<SystemConfigurationType> systemConfigurationType = repositoryService.getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);
        result.computeStatus();
        TestUtil.assertSuccess("Asserting sync settings failed (result)", result);
        ProjectionPolicyType globalAccountSynchronizationSettings = systemConfigurationType.asObjectable().getGlobalAccountSynchronizationSettings();
        assertNotNull("globalAccountSynchronizationSettings is null", globalAccountSynchronizationSettings);
        AssignmentPolicyEnforcementType assignmentPolicyEnforcement = globalAccountSynchronizationSettings.getAssignmentPolicyEnforcement();
        assertNotNull("assignmentPolicyEnforcement is null", assignmentPolicyEnforcement);
        assertEquals("Assignment policy mismatch", assignmentPolicy, assignmentPolicyEnforcement);
    }

    private void checkAllShadows() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
    	LOGGER.trace("Checking all shadows");
    	System.out.println("Checking all shadows");
		ObjectChecker<ShadowType> checker = null;
		IntegrationTestTools.checkAllShadows(resourceTypeOpenDjrepo, repositoryService, checker, prismContext);
	}

    public static String getNormalizedAttributeValue(ShadowType repoShadow, RefinedObjectClassDefinition objClassDef, QName name) {

		String value = getAttributeValue(repoShadow, name);

		RefinedAttributeDefinition idDef = objClassDef.getPrimaryIdentifiers().iterator().next();
		if (idDef.getMatchingRuleQName() != null && idDef.getMatchingRuleQName().equals(StringIgnoreCaseMatchingRule.NAME)){
			return value.toLowerCase();
		}

		return value;
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
