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
package com.evolveum.midpoint.testing.sanity;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;

import org.apache.commons.lang.StringUtils;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.*;
import org.opends.server.types.ModificationType;
import org.opends.server.util.ChangeRecordEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.*;
import org.w3._2001._04.xmlenc.EncryptedDataType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.*;

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

    private static final String SYSTEM_CONFIGURATION_FILENAME = REPO_DIR_NAME + "system-configuration.xml";
    private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

    private static final String RESOURCE_OPENDJ_FILENAME = REPO_DIR_NAME + "resource-opendj.xml";
    private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

    private static final String RESOURCE_DERBY_FILENAME = REPO_DIR_NAME + "resource-derby.xml";
    private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";

    private static final String RESOURCE_BROKEN_FILENAME = REPO_DIR_NAME + "resource-broken.xml";
    private static final String RESOURCE_BROKEN_OID = "ef2bc95b-76e0-59e2-ffff-ffffffffffff";

    private static final String CONNECTOR_LDAP_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.forgerock.openicf.connectors.ldap-connector/org.identityconnectors.ldap.LdapConnector";
    private static final String CONNECTOR_DBTABLE_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/org.forgerock.openicf.connectors.databasetable-connector/org.identityconnectors.databasetable.DatabaseTableConnector";
    
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
    private static final String REQUEST_USER_MODIFY_PASSWORD_FILENAME = REQUEST_DIR_NAME + "user-modify-password.xml";
    private static final String REQUEST_USER_MODIFY_ACTIVATION_DISABLE_FILENAME = REQUEST_DIR_NAME + "user-modify-activation-disable.xml";
    private static final String REQUEST_USER_MODIFY_ACTIVATION_ENABLE_FILENAME = REQUEST_DIR_NAME + "user-modify-activation-enable.xml";

    private static final String REQUEST_USER_MODIFY_ADD_ROLE_PIRATE_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-pirate.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_1_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-captain-1.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_2_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-captain-2.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_JUDGE_FILENAME = REQUEST_DIR_NAME + "user-modify-add-role-judge.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_PIRATE_FILENAME = REQUEST_DIR_NAME + "user-modify-delete-role-pirate.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_1_FILENAME = REQUEST_DIR_NAME + "user-modify-delete-role-captain-1.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_2_FILENAME = REQUEST_DIR_NAME + "user-modify-delete-role-captain-2.xml";

    private static final String REQUEST_ACCOUNT_MODIFY_ATTRS_FILENAME = REQUEST_DIR_NAME + "account-modify-attrs.xml";

    private static final String LDIF_WILL_FILENAME = REQUEST_DIR_NAME + "will.ldif";
    private static final String LDIF_WILL_MODIFY_FILENAME = REQUEST_DIR_NAME + "will-modify.ldif";
    private static final String LDIF_WILL_WITHOUT_LOCATION_FILENAME = REQUEST_DIR_NAME + "will-without-location.ldif";
    private static final String WILL_NAME = "wturner";

    private static final String LDIF_ELAINE_FILENAME = REQUEST_DIR_NAME + "elaine.ldif";
    private static final String ELAINE_NAME = "elaine";
    
    private static final String LDIF_GIBBS_MODIFY_FILENAME = REQUEST_DIR_NAME + "gibbs-modify.ldif";
    
    private static final String  LDIF_HERMAN_FILENAME = REQUEST_DIR_NAME + "herman.ldif";

    private static final QName IMPORT_OBJECTCLASS = new QName(
            "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff",
            "AccountObjectClass");

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
    private static String originalJacksPassword;

    private int lastSyncToken;
    
    public TestSanity() throws JAXBException {
        super();
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        Task task = taskManager.createTaskInstance("get administrator");
        PrismObject<UserType> object = modelService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(),
                null, task, task.getResult());

        assertNotNull("Administrator user is null", object.asObjectable());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(object.asObjectable(), null));
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

        // We need to add config after calling postInit() so it will not be applied.
        // we want original logging configuration from the test logback config file, not
        // the one from the system config.
        addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);

        // Add broken connector before importing resources
        addObjectFromFile(CONNECTOR_BROKEN_FILENAME, ConnectorType.class, initResult);

        // Need to import instead of add, so the (dynamic) connector reference
        // will be resolved
        // correctly
        importObjectFromFile(RESOURCE_OPENDJ_FILENAME, initResult);
        importObjectFromFile(RESOURCE_DERBY_FILENAME, initResult);
        importObjectFromFile(RESOURCE_BROKEN_FILENAME, initResult);

        addObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME, GenericObjectType.class, initResult);
        addObjectFromFile(USER_TEMPLATE_FILENAME, UserTemplateType.class, initResult);
        addObjectFromFile(ROLE_SAILOR_FILENAME, RoleType.class, initResult);
        addObjectFromFile(ROLE_PIRATE_FILENAME, RoleType.class, initResult);
        addObjectFromFile(ROLE_CAPTAIN_FILENAME, RoleType.class, initResult);
        addObjectFromFile(ROLE_JUDGE_FILENAME, RoleType.class, initResult);
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
        // This is defined in extra schema. So this effectively checks whether the extra schema was loaded
        PrismPropertyDefinition shipStateDefinition = schemaRegistry.findPropertyDefinitionByElementName(MY_SHIP_STATE);
        assertNotNull("No my:shipState definition", shipStateDefinition);
        assertEquals("Wrong maxOccurs in my:shipState definition", 1, shipStateDefinition.getMaxOccurs());
        
        assertNoRepoCache();

        Task task = taskManager.createTaskInstance(TestSanity.class.getName() + ".test000Integrity");
        OperationResult result = task.getResult();

        // Check if OpenDJ resource was imported correctly

        PrismObject<ResourceType> openDjResource = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, result);
        display("Imported OpenDJ resource (repository)", openDjResource);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());

        assertNoRepoCache();

        String ldapConnectorOid = openDjResource.asObjectable().getConnectorRef().getOid();
        PrismObject<ConnectorType> ldapConnector = repositoryService.getObject(ConnectorType.class, ldapConnectorOid, result);
        display("LDAP Connector: ", ldapConnector);

        // Check if Derby resource was imported correctly

        PrismObject<ResourceType> derbyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, result);
        AssertJUnit.assertEquals(RESOURCE_DERBY_OID, derbyResource.getOid());

        assertNoRepoCache();

        String dbConnectorOid = derbyResource.asObjectable().getConnectorRef().getOid();
        PrismObject<ConnectorType> dbConnector = repositoryService.getObject(ConnectorType.class, dbConnectorOid, result);
        display("DB Connector: ", dbConnector);

        // Check if password was encrypted during import
        Object configurationPropertiesElement = JAXBUtil.findElement(derbyResource.asObjectable().getConnectorConfiguration().getAny(),
                new QName(dbConnector.asObjectable().getNamespace(), "configurationProperties"));
        Object passwordElement = JAXBUtil.findElement(JAXBUtil.listChildElements(configurationPropertiesElement),
                new QName(dbConnector.asObjectable().getNamespace(), "password"));
        System.out.println("Password element: " + passwordElement);


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

        OperationResult opResult = new OperationResult(TestSanity.class.getName() + ".test001TestConnectionOpenDJ");

        PrismObject<ResourceType> resourceOpenDjRepo = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, opResult);
        resourceTypeOpenDjrepo = resourceOpenDjRepo.asObjectable();

        assertNoRepoCache();
        assertEquals(RESOURCE_OPENDJ_OID, resourceTypeOpenDjrepo.getOid());
        display("Initialized OpenDJ resource (respository)", resourceTypeOpenDjrepo);
        assertNotNull("Resource schema was not generated", resourceTypeOpenDjrepo.getSchema());
        Element resourceOpenDjXsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(resourceTypeOpenDjrepo);
        assertNotNull("Resource schema was not generated", resourceOpenDjXsdSchemaElement);

        PrismObject<ResourceType> openDjResourceProvisioninig = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, 
        		null, opResult);
        display("Initialized OpenDJ resource resource (provisioning)", openDjResourceProvisioninig);

        PrismObject<ResourceType> openDjResourceModel = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult);
        display("Initialized OpenDJ resource OpenDJ resource (model)", openDjResourceModel);

        checkOpenDjResource(resourceTypeOpenDjrepo, "repository");
        
        System.out.println("------------------------------------------------------------------");
        display("OpenDJ resource schema (repo XML)", DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(resourceOpenDjRepo)));
        System.out.println("------------------------------------------------------------------");
        
        checkOpenDjResource(openDjResourceProvisioninig.asObjectable(), "provisioning");
        checkOpenDjResource(openDjResourceModel.asObjectable(), "model");
        // TODO: model web

    }

    
    private void checkRepoOpenDjResource() throws ObjectNotFoundException, SchemaException {
    	OperationResult result = new OperationResult(TestSanity.class.getName()+".checkRepoOpenDjResource");
    	PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, result);
    	checkOpenDjResource(resource.asObjectable(), "repository");
    }

    private void checkRepoDerbyResource() throws ObjectNotFoundException, SchemaException {
    	OperationResult result = new OperationResult(TestSanity.class.getName()+".checkRepoDerbyResource");
    	PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, result);
    	checkDerbyResource(resource, "repository");
    }

    
	private void checkDerbyResource(PrismObject<ResourceType> resource, String source) {
		checkDerbyConfiguration(resource, source);
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
        assertNotNull("Resource from " + source + " has null filter element in connectorRef", connectorRefType.getFilter().getFilter());
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
        ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        ObjectClassComplexTypeDefinition accountDefinition = schema.findDefaultAccountDefinition();
        assertNotNull("Schema does not define any account (resource from " + source + ")", accountDefinition);
        Collection<ResourceAttributeDefinition> identifiers = accountDefinition.getIdentifiers();
        assertFalse("No account identifiers (resource from " + source + ")", identifiers == null || identifiers.isEmpty());
        // TODO: check for naming attributes and display names, etc

        ActivationCapabilityType capActivation = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        if (capActivation != null && capActivation.getEnableDisable() != null && capActivation.getEnableDisable().getAttribute() != null) {
            // There is simulated activation capability, check if the attribute is in schema.
            QName enableAttrName = capActivation.getEnableDisable().getAttribute();
            ResourceAttributeDefinition enableAttrDef = accountDefinition.findAttributeDefinition(enableAttrName);
            display("Simulated activation attribute definition", enableAttrDef);
            assertNotNull("No definition for enable attribute " + enableAttrName + " in account (resource from " + source + ")", enableAttrDef);
            assertTrue("Enable attribute " + enableAttrName + " is not ignored (resource from " + source + ")", enableAttrDef.isIgnored());
        }
    }

	private void checkOpenDjSchemaHandling(ResourceType resource, String source) {
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		for (ResourceAccountTypeDefinitionType accountType: schemaHandling.getAccountType()) {
			String name = accountType.getName();
			assertNotNull("Resource "+resource+" from "+source+" has an schemaHandlig account definition without a name", name);
			assertNotNull("Account type "+name+" in "+resource+" from "+source+" does not have object class", accountType.getObjectClass());
		}
	}
	
	private void checkOpenDjConfiguration(PrismObject<ResourceType> resource, String source) {
		checkOpenResourceConfiguration(resource, CONNECTOR_LDAP_NAMESPACE, "credentials", 7, source);
	}
	
	private void checkDerbyConfiguration(PrismObject<ResourceType> resource, String source) {
		checkOpenResourceConfiguration(resource, CONNECTOR_DBTABLE_NAMESPACE, "password", 10, source);
	}
		
	private void checkOpenResourceConfiguration(PrismObject<ResourceType> resource, String connectorNamespace, String credentialsPropertyName,
			int numConfigProps, String source) {
		PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		assertNotNull("No configuration container in "+resource+" from "+source, configurationContainer);
		PrismContainer<Containerable> configPropsContainer = configurationContainer.findContainer(SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES);
		assertNotNull("No configuration properties container in "+resource+" from "+source, configPropsContainer);
		List<Item<?>> configProps = configPropsContainer.getValue().getItems();
		assertEquals("Wrong number of config properties in "+resource+" from "+source, numConfigProps, configProps.size());
		PrismProperty<Object> credentialsProp = configPropsContainer.findProperty(new QName(connectorNamespace,credentialsPropertyName));
		if (credentialsProp == null) {
			// The is the heisenbug we are looking for. Just dump the entire damn thing.
			display("Configuration with the heisenbug", configurationContainer.dump());
		}
		assertNotNull("No credentials property in "+resource+" from "+source, credentialsProp);
		assertEquals("Wrong number of credentials property value in "+resource+" from "+source, 1, credentialsProp.getValues().size());
		PrismPropertyValue<Object> credentialsPropertyValue = credentialsProp.getValues().iterator().next();
		assertNotNull("No credentials property value in "+resource+" from "+source, credentialsPropertyValue);
		if (credentialsPropertyValue.isRaw()) {
			Object rawElement = credentialsPropertyValue.getRawElement();
			assertTrue("Wrong element class "+rawElement.getClass()+" in "+resource+" from "+source, rawElement instanceof Element);
			Element rawDomElement = (Element)rawElement;
	//		display("LDAP credentials raw element", DOMUtil.serializeDOMToString(rawDomElement));
			assertEquals("Wrong credentials element namespace in "+resource+" from "+source, connectorNamespace, rawDomElement.getNamespaceURI());
			assertEquals("Wrong credentials element local name in "+resource+" from "+source, credentialsPropertyName, rawDomElement.getLocalName());
			Element encryptedDataElement = DOMUtil.getChildElement(rawDomElement, new QName(DOMUtil.NS_XML_ENC, "EncryptedData"));
			assertNotNull("No EncryptedData element", encryptedDataElement);
			assertEquals("Wrong EncryptedData element namespace in "+resource+" from "+source, DOMUtil.NS_XML_ENC, encryptedDataElement.getNamespaceURI());
			assertEquals("Wrong EncryptedData element local name in "+resource+" from "+source, "EncryptedData", encryptedDataElement.getLocalName());
		} else {			
			Object credentials = credentialsPropertyValue.getValue();
			assertTrue("Wrong type of credentials configuration property in "+resource+" from "+source+": "+credentials.getClass(), credentials instanceof ProtectedStringType);
			ProtectedStringType credentialsPs = (ProtectedStringType)credentials;
			EncryptedDataType encryptedData = credentialsPs.getEncryptedData();
			assertNotNull("No EncryptedData element", encryptedData);
		}

	}
    
    /**
     * Test the testResource method. Expect a complete success for now.
     */
    @Test
    public void test002TestConnectionDerby() throws FaultMessage, JAXBException, ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        displayTestTile("test002TestConnectionDerby");

        // GIVEN

        checkRepoDerbyResource();
        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.testResource(RESOURCE_DERBY_OID);

        // THEN

        assertNoRepoCache();
        displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

        assertSuccess("testResource has failed", result.getPartialResults().get(0));

        OperationResult opResult = new OperationResult(TestSanity.class.getName() + ".test002TestConnectionDerby");

        PrismObject<ResourceType> rObject = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, opResult);
        resourceDerby = rObject.asObjectable();
        checkDerbyResource(rObject, "repository(after test)");

        assertNoRepoCache();
        assertEquals(RESOURCE_DERBY_OID, resourceDerby.getOid());
        display("Initialized Derby resource (respository)", resourceDerby);
        assertNotNull("Resource schema was not generated", resourceDerby.getSchema());
        Element resourceDerbyXsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(resourceDerby);
        assertNotNull("Resource schema was not generated", resourceDerbyXsdSchemaElement);

        PrismObject<ResourceType> derbyResourceProvisioninig = provisioningService.getObject(ResourceType.class, RESOURCE_DERBY_OID,
                null, opResult);
        display("Initialized Derby resource (provisioning)", derbyResourceProvisioninig);

        PrismObject<ResourceType> derbyResourceModel = provisioningService.getObject(ResourceType.class, RESOURCE_DERBY_OID,
                null, opResult);
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
        displayTestTile("test004Capabilities");

        // GIVEN
        
        checkRepoOpenDjResource();

        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        OperationOptionsType options = new OperationOptionsType();
        
		// WHEN
        modelWeb.getObject(ObjectTypes.RESOURCE.getObjectTypeUri(), RESOURCE_OPENDJ_OID,
                options , objectHolder, resultHolder);

        ResourceType resource = (ResourceType) objectHolder.value;

        // THEN
        display("Resource", resource);

        assertNoRepoCache();

        CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
        List<Object> capabilities = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned", capabilities.isEmpty());

        for (Object capability : nativeCapabilities.getAny()) {
            System.out.println("Native Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        if (resource.getCapabilities() != null) {
            for (Object capability : resource.getCapabilities().getConfigured().getAny()) {
                System.out.println("Configured Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : " + capability);
            }
        }

        List<Object> effectiveCapabilities = ResourceTypeUtil.getEffectiveCapabilities(resource);
        for (Object capability : effectiveCapabilities) {
            System.out.println("Efective Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        CredentialsCapabilityType capCred = ResourceTypeUtil.getCapability(capabilities, CredentialsCapabilityType.class);
        assertNotNull("password capability not present", capCred.getPassword());
        // Connector cannot do activation, this should be null
        ActivationCapabilityType capAct = ResourceTypeUtil.getCapability(capabilities, ActivationCapabilityType.class);
        assertNull("Found activation capability while not expecting it", capAct);

        capCred = ResourceTypeUtil.getEffectiveCapability(resource, CredentialsCapabilityType.class);
        assertNotNull("password capability not found", capCred.getPassword());
        // Although connector does not support activation, the resource specifies a way how to simulate it.
        // Therefore the following should succeed
        capAct = ResourceTypeUtil.getEffectiveCapability(resource, ActivationCapabilityType.class);
        assertNotNull("activation capability not found", capAct);

    }

    /**
     * Attempt to add new user. It is only added to the repository, so check if
     * it is in the repository after the operation.
     */
    @Test
    public void test010AddUser() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException {
        displayTestTile("test010AddUser");

        // GIVEN
        checkRepoOpenDjResource();
        assertNoRepoCache();

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
        UserType userType = user.asObjectable();
        PrismAsserts.assertParentConsistency(user);
        
        // Encrypt Jack's password
        protector.encrypt(userType.getCredentials().getPassword().getValue());
        PrismAsserts.assertParentConsistency(user);

        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
        Holder<String> oidHolder = new Holder<String>();

        display("Adding user object", userType);

        // WHEN
        modelWeb.addObject(userType, oidHolder, resultHolder);

        // THEN

        assertNoRepoCache();
        displayJaxb("addObject result:", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("addObject has failed", resultHolder.value);

        AssertJUnit.assertEquals(USER_JACK_OID, oidHolder.value);

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, oidHolder.value, repoResult);
        UserType repoUser = uObject.asObjectable();

        repoResult.computeStatus();
        display("repository.getObject result", repoResult);
        assertSuccess("getObject has failed", repoResult);
        AssertJUnit.assertEquals(USER_JACK_OID, repoUser.getOid());
        PrismAsserts.assertEqualsPolyString("fullName", userType.getFullName(), repoUser.getFullName());

        // TODO: better checks
    }

    /**
     * Add account to user. This should result in account provisioning. Check if
     * that happens in repo and in LDAP.
     */
    @Test
    public void test013AddOpenDjAccountToUser() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException, ObjectAlreadyExistsException {
        displayTestTile("test013AddOpenDjAccountToUser");

        // GIVEN
        checkRepoOpenDjResource();
        assertNoRepoCache();

        // IMPORTANT! SWITCHING OFF ASSIGNMENT ENFORCEMENT HERE!
        setAssignmentEnforcement(AssignmentPolicyEnforcementType.NONE);
        // This is not redundant. It checks that the previous command set the policy correctly
        assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.NONE);

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
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

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj,
                repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        assertNotNull("Shadow stored in repository has no name", repoShadowType.getName());
        // Check the "name" property, it should be set to DN, not entryUUID
        assertEquals("Wrong name property", USER_JACK_LDAP_DN.toLowerCase(), repoShadowType.getName().getOrig().toLowerCase());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        OpenDJController.assertAttribute(entry, "cn", "Jack Sparrow");
        OpenDJController.assertAttribute(entry, "displayName", "Jack Sparrow");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

        originalJacksPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword was not set on create", originalJacksPassword);
        System.out.println("password after create: " + originalJacksPassword);

        // Use getObject to test fetch of complete shadow

        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        OperationOptionsType options = new OperationOptionsType();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj,
                options, objectHolder, resultHolder);

        // THEN
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "uid", "jack");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "givenName", "Jack");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "sn", "Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "cn", "Jack Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "displayName", "Jack Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "l", "middle of nowhere");
        assertNull("carLicense attribute sneaked to LDAP", OpenDJController.getAttributeValue(entry, "carLicense"));
        assertNull("postalAddress attribute sneaked to LDAP", OpenDJController.getAttributeValue(entry, "postalAddress"));

        assertNotNull("Activation is null", modelShadow.getActivation());
        assertNotNull("No 'enabled' in the shadow", modelShadow.getActivation().isEnabled());
        assertTrue("The account is not enabled in the shadow", modelShadow.getActivation().isEnabled());

    }

	/**
     * Add Derby account to user. This should result in account provisioning. Check if
     * that happens in repo and in Derby.
     */
    @Test
    public void test014AddDerbyAccountToUser() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException, SQLException {
        displayTestTile("test014AddDerbyAccountToUser");

        // GIVEN

        checkRepoDerbyResource();
        assertNoRepoCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ACCOUNT_DERBY_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
        UserType repoUser = uObject.asObjectable();

        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
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

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidDerby,
                repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        assertSuccess("addObject has failed", repoResult);
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
        OperationOptionsType options = new OperationOptionsType();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidDerby,
                options, objectHolder, resultHolder);

        // THEN
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_DERBY_OID, modelShadow.getResourceRef().getOid());

        assertAttribute(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID, USER_JACK_DERBY_LOGIN);
        assertAttribute(modelShadow, ConnectorFactoryIcfImpl.ICFS_NAME, USER_JACK_DERBY_LOGIN);
        assertAttribute(modelShadow, resourceDerby, "FULL_NAME", "Cpt. Jack Sparrow");

    }

    @Test
    public void test015AccountOwner() throws FaultMessage, ObjectNotFoundException, SchemaException, JAXBException {
        displayTestTile("test015AccountOwner");

        // GIVEN
        checkRepoOpenDjResource();
        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<UserType> userHolder = new Holder<UserType>();

        // WHEN

        modelWeb.listAccountShadowOwner(accountShadowOidOpendj, userHolder, resultHolder);

        // THEN

        display("listAccountShadowOwner result", resultHolder.value);
        assertSuccess("listAccountShadowOwner has failed (result)", resultHolder.value);
        UserType user = userHolder.value;
        assertNotNull("No owner", user);
        assertEquals(USER_JACK_OID, user.getOid());

        System.out.println("Account " + accountShadowOidOpendj + " has owner " + ObjectTypeUtil.toShortString(user));
    }

    @Test
    public void test016ProvisioningSearchAccountsIterative() throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        displayTestTile("test016ProvisioningSearchAccountsIterative");

        // GIVEN
        OperationResult result = new OperationResult(TestSanity.class.getName() + ".test016ProvisioningSearchAccountsIterative");

        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceTypeOpenDjrepo, prismContext);
        RefinedAccountDefinition refinedAccountDefinition = refinedSchema.getDefaultAccountDefinition();

        QName objectClass = refinedAccountDefinition.getObjectClassDefinition().getTypeName();
        ObjectQuery q = ObjectQueryUtil.createResourceAndAccountQuery(resourceTypeOpenDjrepo.getOid(), objectClass, prismContext);
//        ObjectQuery q = QueryConvertor.createObjectQuery(AccountShadowType.class, query, prismContext);

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
                assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "AccountObjectClass"), shadow.getObjectClass());
                assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
                String icfUid = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "uid"));
                assertNotNull("No ICF UID", icfUid);
                String icfName = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "name"));
                assertNotNull("No ICF NAME", icfName);
                assertEquals("Wrong shadow name", shadow.getName().getOrig(), icfName);
                assertNotNull("Missing LDAP uid", getAttributeValue(shadow, new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "uid")));
                assertNotNull("Missing LDAP cn", getAttributeValue(shadow, new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "cn")));
                assertNotNull("Missing LDAP sn", getAttributeValue(shadow, new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "sn")));
                assertNotNull("Missing activation", shadow.getActivation());
                assertNotNull("Missing activation/enabled", shadow.getActivation().isEnabled());
                return true;
            }
        };

        // WHEN

        provisioningService.searchObjectsIterative(AccountShadowType.class, q, handler, result);

        // THEN

        display("Count", objects.size());
    }

    /**
     * We are going to modify the user. As the user has an account, the user
     * changes should be also applied to the account (by schemaHandling).
     *
     * @throws DirectoryException
     */
    @Test
    public void test020ModifyUser() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException {
        displayTestTile("test020ModifyUser");
        // GIVEN

        assertNoRepoCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
        UserType repoUserType = repoUser.asObjectable(); 
        display("repository user", repoUser);

        PrismAsserts.assertEqualsPolyString("wrong value for fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong value for locality", "somewhere", repoUserType.getLocality());
        assertEquals("wrong value for employeeNumber", "1", repoUserType.getEmployeeNumber());

        // Check if appropriate accountRef is still there

        List<ObjectReferenceType> accountRefs = repoUserType.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue("No OID in "+accountRef+" in "+repoUserType,
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));

        }

        // Check if shadow is still in the repo and that it is untouched

        repoResult = new OperationResult("getObject");
        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj, repoResult);
        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        display("repository shadow", repoShadow);
        AssertJUnit.assertNotNull(repoShadow);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)

        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated
        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);

        display(entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        // These two should be assigned from the User modification by
        // schemaHandling
        OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "displayName", "Cpt. Jack Sparrow");
        // This will get translated from "somewhere" to this (outbound expression in schemeHandling)
        OpenDJController.assertAttribute(entry, "l", "There there over the corner");
        OpenDJController.assertAttribute(entry, "postalAddress", "Number 1");

    }

    /**
     * We are going to change user's password. As the user has an account, the password change
     * should be also applied to the account (by schemaHandling).
     */
    @Test
    public void test022ChangePassword() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException {
        displayTestTile("test022ChangePassword");
        // GIVEN

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_PASSWORD_FILENAME, ObjectModificationType.class);

        System.out.println("In modification: " + objectChange.getModification().get(0).getValue().getAny().get(0));
        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        display("repository user", repoUser);

        // Check if nothing else was modified
        PrismAsserts.assertEqualsPolyString("wrong repo fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong repo locality", "somewhere", repoUserType.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUserType.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue("No OID in "+accountRef+" in "+repoUserType,
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));

        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");
        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj, repoResult);
        display("repository shadow", repoShadow);
        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        AssertJUnit.assertNotNull(repoShadowType);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // Check if LDAP account was updated

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);
        display(entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        // These two should be assigned from the User modification by
        // schemaHandling
        OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "displayName", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "l", "There there over the corner");

        String passwordAfter = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull(passwordAfter);

        System.out.println("password after change: " + passwordAfter);

        assertFalse("No change in password", passwordAfter.equals(originalJacksPassword));
    }

    /**
     * Try to disable user. As the user has an account, the account should be disabled as well.
     */
    @Test
    public void test030Disable() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException {
        displayTestTile("test030Disable");
        // GIVEN

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ACTIVATION_DISABLE_FILENAME, ObjectModificationType.class);

        SearchResultEntry entry = openDJController.searchByUid("jack");
        display(entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        // These two should be assigned from the User modification by
        // schemaHandling
        OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "displayName", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "l", "There there over the corner");

        String pwpAccountDisabled = OpenDJController.getAttributeValue(entry, "ds-pwp-account-disabled");
        System.out.println("ds-pwp-account-disabled before change: " + pwpAccountDisabled);
        System.out.println();
        assertNull(pwpAccountDisabled);
        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
        display("repository user", repoUser);
        UserType repoUserType = repoUser.asObjectable();

        // Check if nothing else was modified
        PrismAsserts.assertEqualsPolyString("wrong repo fullName", "Cpt. Jack Sparrow", repoUserType.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong repo locality", "somewhere", repoUserType.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUserType.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue("No OID in "+accountRef+" in "+repoUserType,
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));
        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj, repoResult);
        display("repo shadow", repoShadow);
        AccountShadowType repoShadowType = repoShadow.asObjectable();

        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        AssertJUnit.assertNotNull(repoShadowType);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // Use getObject to test fetch of complete shadow

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        OperationOptionsType options = new OperationOptionsType();
        assertNoRepoCache();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj,
                options, objectHolder, resultHolder);

        // THEN
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "uid", "jack");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "givenName", "Jack");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "sn", "Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "cn", "Cpt. Jack Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "displayName", "Cpt. Jack Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "l", "There there over the corner");

        assertNotNull("The account activation is null in the shadow", modelShadow.getActivation());
        assertNotNull("The account 'enabled' status was not present in shadow", modelShadow.getActivation().isEnabled());
        assertFalse("The account was not disabled in the shadow", modelShadow.getActivation().isEnabled());

        // Check if LDAP account was updated

        entry = openDJController.searchAndAssertByEntryUuid(uid);
        display(entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        // These two should be assigned from the User modification by
        // schemaHandling
        OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "displayName", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "l", "There there over the corner");

        pwpAccountDisabled = OpenDJController.getAttributeValue(entry, "ds-pwp-account-disabled");
        assertNotNull(pwpAccountDisabled);

        System.out.println("ds-pwp-account-disabled after change: " + pwpAccountDisabled);

        assertEquals("ds-pwp-account-disabled not set to \"true\"", "true", pwpAccountDisabled);
    }

    /**
     * Try to enable user after it has been disabled. As the user has an account, the account should be enabled as well.
     */
    @Test
    public void test031Enable() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException {
        displayTestTile("test031Enable");
        // GIVEN

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ACTIVATION_ENABLE_FILENAME, ObjectModificationType.class);
        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
        UserType repoUser = uObject.asObjectable();
        display("repo user", repoUser);

        // Check if nothing else was modified
        PrismAsserts.assertEqualsPolyString("wrong repo fullName", "Cpt. Jack Sparrow", repoUser.getFullName());
        PrismAsserts.assertEqualsPolyString("wrong repo locality", "somewhere", repoUser.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue("No OID in "+accountRef+" in "+repoUser,
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));
        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj,
        		repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();

        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        display("repo shadow", repoShadowType);
        AssertJUnit.assertNotNull(repoShadowType);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = checkRepoShadow(repoShadow);

        // Use getObject to test fetch of complete shadow

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        OperationOptionsType options = new OperationOptionsType();
        assertNoRepoCache();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj,
                options, objectHolder, resultHolder);

        // THEN
        assertNoRepoCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        display("Shadow (model)", modelShadow);

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "uid", "jack");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "givenName", "Jack");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "sn", "Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "cn", "Cpt. Jack Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "displayName", "Cpt. Jack Sparrow");
        assertAttribute(modelShadow, resourceTypeOpenDjrepo, "l", "There there over the corner");

        assertNotNull("The account activation is null in the shadow", modelShadow.getActivation());
        assertNotNull("The account 'enabled' status was not present in shadow", modelShadow.getActivation().isEnabled());
        assertTrue("The account was not enabled in the shadow", modelShadow.getActivation().isEnabled());

        // Check if LDAP account was updated

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);
        display(entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        // These two should be assigned from the User modification by
        // schemaHandling
        OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "displayName", "Cpt. Jack Sparrow");
        OpenDJController.assertAttribute(entry, "l", "There there over the corner");

        // The value of ds-pwp-account-disabled should have been removed
        String pwpAccountDisabled = OpenDJController.getAttributeValue(entry, "ds-pwp-account-disabled");
        System.out.println("ds-pwp-account-disabled after change: " + pwpAccountDisabled);
        assertTrue("LDAP account was not enabled", (pwpAccountDisabled == null) || (pwpAccountDisabled.equals("false")));
    }

    /**
     * Unlink account by removing the accountRef from the user.
     * The account will not be deleted, just the association to user will be broken.
     */
    @Test
    public void test040UnlinkDerbyAccountFromUser() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException, SQLException {
        displayTestTile("test040UnlinkDerbyAccountFromUser");

        // GIVEN

        ObjectModificationType objectChange = new ObjectModificationType();
        objectChange.setOid(USER_JACK_OID);
        ItemDeltaType modificationDeleteAccountRef = new ItemDeltaType();
        modificationDeleteAccountRef.setModificationType(ModificationTypeType.DELETE);
        ItemDeltaType.Value modificationValue = new ItemDeltaType.Value();
        ObjectReferenceType accountRefToDelete = new ObjectReferenceType();
        accountRefToDelete.setOid(accountShadowOidDerby);
        JAXBElement<ObjectReferenceType> accountRefToDeleteElement = new JAXBElement<ObjectReferenceType>(SchemaConstants.I_ACCOUNT_REF, ObjectReferenceType.class, accountRefToDelete);
        modificationValue.getAny().add(accountRefToDeleteElement);
        modificationDeleteAccountRef.setValue(modificationValue);
        objectChange.getModification().add(modificationDeleteAccountRef);
        displayJaxb("modifyObject input", objectChange, new QName(SchemaConstants.NS_C, "change"));
        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
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
        displayTestTile("test041DeleteDerbyAccount");

        // GIVEN

        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.deleteObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidDerby);

        // THEN
        assertNoRepoCache();
        displayJaxb("deleteObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("deleteObject has failed", result);

        // Check if shadow was deleted
        OperationResult repoResult = new OperationResult("getObject");

        try {
            repositoryService.getObject(AccountShadowType.class, accountShadowOidDerby,
                    repoResult);
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

    /**
     * The user should have an account now. Let's try to delete the user. The
     * account should be gone as well.
     *
     * @throws JAXBException
     */
    @Test
    public void test049DeleteUser() throws SchemaException, FaultMessage, DirectoryException, JAXBException {
        displayTestTile("test049DeleteUser");
        // GIVEN

        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.deleteObject(ObjectTypes.USER.getObjectTypeUri(), USER_JACK_OID);

        // THEN
        assertNoRepoCache();
        displayJaxb("deleteObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("deleteObject has failed", result);

        // User should be gone from the repository
        OperationResult repoResult = new OperationResult("getObject");
        try {
            repositoryService.getObject(UserType.class, USER_JACK_OID, repoResult);
            AssertJUnit.fail("User still exists in repo after delete");
        } catch (ObjectNotFoundException e) {
            // This is expected
        }

        // Account shadow should be gone from the repository
        repoResult = new OperationResult("getObject");
        try {
            repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj, repoResult);
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
    public void test050AssignRolePirate() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException,
            ObjectAlreadyExistsException {
        displayTestTile("test050AssignRolePirate");

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

        modelWeb.addObject(userType, oidHolder, resultHolder);

        assertNoRepoCache();
        assertSuccess("addObject has failed", resultHolder.value);

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_PIRATE_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadowType);
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        accountGuybrushOpendjEntryUuuid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

        // Set by the role
        OpenDJController.assertAttribute(entry, "employeeType", "sailor");
        OpenDJController.assertAttribute(entry, "title", "Bloody Pirate");
        OpenDJController.assertAttribute(entry, "businessCategory", "loot", "murder");

        String guybrushPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword was not set on create", guybrushPassword);

        // TODO: Derby

    }

    @Test
    public void test051AccountOwnerAfterRole() throws FaultMessage {
        displayTestTile("test051AccountOwnerAfterRole");

        // GIVEN

        assertNoRepoCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<UserType> userHolder = new Holder<UserType>();

        // WHEN

        modelWeb.listAccountShadowOwner(accountShadowOidGuybrushOpendj, userHolder, resultHolder);

        // THEN

        assertSuccess("listAccountShadowOwner has failed (result)", resultHolder.value);
        UserType user = userHolder.value;
        assertNotNull("No owner", user);
        assertEquals(USER_GUYBRUSH_OID, user.getOid());

        System.out.println("Account " + accountShadowOidGuybrushOpendj + " has owner " + ObjectTypeUtil.toShortString(user));
    }


    @Test
    public void test052AssignRoleCaptain() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test052AssignRoleCaptain");

        // GIVEN

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_1_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

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
    public void test053AssignRoleCaptainAgain() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test053AssignRoleCaptain");

        // GIVEN

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_2_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

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
    public void test055ModifyAccount() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test055ModifyAccount");

        // GIVEN

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_ACCOUNT_MODIFY_ATTRS_FILENAME, ObjectModificationType.class);
        objectChange.setOid(accountShadowOidGuybrushOpendj);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // check if LDAP account was modified

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

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
    public void test054AssignRoleJudge() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test054AssignRoleJudge");

        // GIVEN

        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
        Holder<String> oidHolder = new Holder<String>();
        assertNoRepoCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_JUDGE_FILENAME, ObjectModificationType.class);
        try {
        	
            // WHEN
        	result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

            // THEN
        	AssertJUnit.fail("Expected a failure after assigning conflicting roles but nothing happened and life goes on");
        } catch (FaultMessage f) {
        	// This is expected
        	// TODO: check if the fault is the right one
        }

        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object remain unmodified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals("Unexpected number or accountRefs", 1, accountRefs.size());

    }


    @Test
    public void test057UnassignRolePirate() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test057UnassignRolePirate");

        // GIVEN

        OperationResultType result = new OperationResultType();
        assertNoRepoCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_PIRATE_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);


        List<ObjectReferenceType> accountRefs = repoUserType.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

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
    public void test058UnassignRoleCaptain() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test058UnassignRoleCaptain");

        // GIVEN

        OperationResultType result = new OperationResultType();
        assertNoRepoCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_1_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUser);


        List<ObjectReferenceType> accountRefs = repoUserType.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        display("Shadow (repository)", repoShadow);
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

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
    public void test059UnassignRoleCaptainAgain() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test059UnassignRoleCaptain");

        // GIVEN

        OperationResultType result = new OperationResultType();
        assertNoRepoCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_2_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertNoRepoCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
      
        //TODO TODO TODO TODO operation result from repostiory.getObject is unknown...find out why..
//        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUserType = repoUser.asObjectable();
        repoResult.computeStatus();
        display("User (repository)", repoUserType);

        List<ObjectReferenceType> accountRefs = repoUserType.getAccountRef();
        assertEquals(0, accountRefs.size());

        // Check if shadow was deleted from the repo

        repoResult = new OperationResult("getObject");

        try {
            PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                    repoResult);
            AssertJUnit.fail("Account shadow was not deleted from repo");
        } catch (ObjectNotFoundException ex) {
            // This is expected
        }

        // check if account was deleted from LDAP

        SearchResultEntry entry = openDJController.searchByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        assertNull("LDAP account was not deleted", entry);

        // TODO: Derby

    }


    @Test
    public void test060ListResourcesWithBrokenResource() throws Exception {
        displayTestTile("test060ListResourcesWithBrokenResource");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSanity.class.getName() + ".test060ListResourcesWithBrokenResource");
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
                assertEquals("No error in fetchResult in " + ObjectTypeUtil.toShortString(resource), OperationResultStatusType.FATAL_ERROR, resource.getFetchResult().getStatus());
            } else {
                assertTrue("Unexpected error in fetchResult in " + ObjectTypeUtil.toShortString(resource),
                        resource.getFetchResult() == null || resource.getFetchResult().getStatus() == OperationResultStatusType.SUCCESS);
            }

        }

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
        
        checkAllShadows();
        
        // IMPORTANT! Assignment enforcement is POSITIVE now
        setAssignmentEnforcement(AssignmentPolicyEnforcementType.POSITIVE);
        // This is not redundant. It checks that the previous command set the policy correctly
        assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.POSITIVE);

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test100Synchronization");

        addObjectFromFile(TASK_OPENDJ_SYNC_FILENAME, TaskType.class, result);


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
        assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, TASK_OPENDJ_SYNC_OID, result);
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
    public void test101LiveSyncCreate() throws Exception {
        displayTestTile("test101LiveSyncCreate");
        // Sync task should be running (tested in previous test), so just create
        // new LDAP object.

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test101LiveSyncCreate");
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
        assertNotNull(user.getAccountRef());
        assertFalse(user.getAccountRef().isEmpty());
//        AssertJUnit.assertEquals(user.getName(), WILL_NAME);

        // TODO: more checks
        
        assertAndStoreSyncTokenIncrement(syncCycle, 4);
        checkAllShadows();
    }

    @Test
    public void test102LiveSyncModify() throws Exception {
        displayTestTile("test102LiveSyncModify");

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test102LiveSyncModify");
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        int tokenBefore = findSyncToken(syncCycle);
        display("Sync token before", tokenBefore);

        // WHEN
        display("Modifying LDAP entry");
        ChangeRecordEntry entry = openDJController.executeLdifChange(LDIF_WILL_MODIFY_FILENAME);

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
    public void test103LiveSyncLink() throws Exception {
        displayTestTile("test103LiveSyncLink");

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
        modelWeb.addObject(userType, oidHolder, resultHolder);
        //check results
        assertNoRepoCache();
        displayJaxb("addObject result:", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("addObject has failed", resultHolder.value);
        AssertJUnit.assertEquals(userOid, oidHolder.value);

        //WHEN
        //create account for e which should be correlated
        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test103LiveSyncLink");
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        int tokenBefore = findSyncToken(syncCycle);
        display("Sync token before", tokenBefore);

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_E_FILENAME_LINK);
        display("Entry from LDIF", entry);

        // THEN
        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, 3, result);

        //check user and account ref
        userType = searchUserByName("e");

        List<ObjectReferenceType> accountRefs = userType.getAccountRef();
        assertEquals("Account ref not found, or found too many", 1, accountRefs.size());

        //check account defined by account ref
        String accountOid = accountRefs.get(0).getOid();
        AccountShadowType account = searchAccountByOid(accountOid);

        PrismAsserts.assertEqualsPolyString("Name doesn't match",  "uid=e,ou=People,dc=example,dc=com", account.getName());
//        assertEquals("Name doesn't match", "uid=e,ou=People,dc=example,dc=com", account.getName());
        
        assertAndStoreSyncTokenIncrement(syncCycle, 3);
        checkAllShadows();
    }

    /**
     * Create LDAP object. That should be picked up by liveSync and a user
     * should be created in repo.
     * Also location (ldap l) should be updated through outbound
     */
    @Test
    public void test104LiveSyncCreateNoLocation() throws Exception {
        displayTestTile("test104LiveSyncCreateNoLocation");
        // Sync task should be running (tested in previous test), so just create
        // new LDAP object.

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test104LiveSyncCreateNoLocation");
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

        List<ObjectReferenceType> accountRefs = user.getAccountRef();
        assertEquals("Account ref not found, or found too many", 1, accountRefs.size());

        //check account defined by account ref
        String accountOid = accountRefs.get(0).getOid();
        AccountShadowType account = searchAccountByOid(accountOid);

        PrismAsserts.assertEqualsPolyString("Name doesn't match",  "uid=" + userName + ",ou=People,dc=example,dc=com", account.getName());
//        assertEquals("Name doesn't match", "uid=" + userName + ",ou=People,dc=example,dc=com", account.getName());
        Collection<String> localities = getAttributeValues(account, new QName(IMPORT_OBJECTCLASS.getNamespaceURI(), "l"));
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
    public void test199LiveSyncCleanup() throws Exception {
        displayTestTile("test199LiveSyncCleanup");
        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test199LiveSyncCleanup");

        taskManager.deleteTask(TASK_OPENDJ_SYNC_OID, result);

        // TODO: check if the task is really stopped
    }

    @Test
    public void test200ImportFromResource() throws Exception {
        displayTestTile("test200ImportFromResource");
        // GIVEN
        
        checkAllShadows();
        assertNoRepoCache();

        OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test200ImportFromResource");
        
        // Make sure Mr. Gibbs has "l" attribute set to the same value as an outbound expression is setting
        ChangeRecordEntry entry = openDJController.executeLdifChange(LDIF_GIBBS_MODIFY_FILENAME);
        display("Entry from LDIF", entry);
        
        // Let's add an entry with multiple uids.
        Entry addEntry = openDJController.addEntryFromLdifFile(LDIF_HERMAN_FILENAME);
        display("Entry from LDIF", addEntry);

        // WHEN
        TaskType taskType = modelWeb.importFromResource(RESOURCE_OPENDJ_OID, IMPORT_OBJECTCLASS);

        // THEN

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

        PrismObject<TaskType> tObject = repositoryService.getObject(TaskType.class, task.getOid(), result);
        TaskType taskAfter = tObject.asObjectable();
        display("Import task in repo after launch", taskAfter);

        result.computeStatus();
        assertSuccess("getObject has failed", result);

        final String taskOid = task.getOid();

        waitFor("Waiting for import to complete", new Checker() {
            @Override
            public boolean check() throws Exception {
                Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
                Holder<ObjectType> objectHolder = new Holder<ObjectType>();
                OperationResult opResult = new OperationResult("import check");
                assertNoRepoCache();
                OperationOptionsType options = new OperationOptionsType();
                modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), taskOid,
                        options, objectHolder, resultHolder);
                assertNoRepoCache();
                //				display("getObject result (wait loop)",resultHolder.value);
                assertSuccess("getObject has failed", resultHolder.value);
                Task task = taskManager.createTaskInstance(objectHolder.value.asPrismObject(), opResult);
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
        OperationOptionsType options = new OperationOptionsType();
        assertNoRepoCache();

        modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), task.getOid(),
                options, objectHolder, resultHolder);

        assertNoRepoCache();
        assertSuccess("getObject has failed", resultHolder.value);
        task = taskManager.createTaskInstance(objectHolder.value.asPrismObject(), result);

        display("Import task after finish (fetched from model)", task);

        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());
        
        assertNotNull("Null lastRunStartTimestamp in "+task, task.getLastRunStartTimestamp());
        assertNotNull("Null lastRunFinishTimestamp in "+task, task.getLastRunFinishTimestamp());

        long importDuration = task.getLastRunFinishTimestamp() - task.getLastRunStartTimestamp();
        double usersPerSec = (task.getProgress() * 1000) / importDuration;
        display("Imported " + task.getProgress() + " users in " + importDuration + " milliseconds (" + usersPerSec + " users/sec)");

//        waitFor("Waiting for task to get released", new Checker() {
//            @Override
//            public boolean check() throws Exception {
//                Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
//                Holder<ObjectType> objectHolder = new Holder<ObjectType>();
//                OperationResult opResult = new OperationResult("import check");
//                assertCache();
//                modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), taskOid,
//                        new PropertyReferenceListType(), objectHolder, resultHolder);
//                assertCache();
//                //				display("getObject result (wait loop)",resultHolder.value);
//                assertSuccess("getObject has failed", resultHolder.value);
//                Task task = taskManager.createTaskInstance(objectHolder.value.asPrismObject(), opResult);
//                System.out.println("Import task status: " + task.getExecutionStatus());
//                if (task.getExclusivityStatus() == TaskExclusivityStatus.RELEASED) {
//                    // Task closed and released, wait finished
//                    return true;
//                }
//                //				IntegrationTestTools.display("Task result while waiting: ", task.getResult());
//                return false;
//            }
//
//            public void timeout() {
//                Assert.fail("The task was not released after closing");
//            }
//        }, 10000);

        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task has no result", taskResult);
        assertSuccess("Import task result is not success", taskResult);
        AssertJUnit.assertTrue("Task failed", taskResult.isSuccess());

        AssertJUnit.assertTrue("No progress", task.getProgress() > 0);

        //### Check if the import created users and shadows ###

        // Listing of shadows is not supported by the provisioning. So we need
        // to look directly into repository
        List<PrismObject<AccountShadowType>> sobjects = repositoryService.searchObjects(AccountShadowType.class, null, result);
        result.computeStatus();
        assertSuccess("listObjects has failed", result);
        AssertJUnit.assertFalse("No shadows created", sobjects.isEmpty());

        for (PrismObject<AccountShadowType> aObject : sobjects) {
            AccountShadowType shadow = aObject.asObjectable();
            display("Shadow object after import (repo)", shadow);
            assertNotEmpty("No OID in shadow", shadow.getOid()); // This would be really strange ;-)
            assertNotEmpty("No name in shadow", shadow.getName());
            AssertJUnit.assertNotNull("No objectclass in shadow", shadow.getObjectClass());
            AssertJUnit.assertNotNull("Null attributes in shadow", shadow.getAttributes());
            assertAttributeNotNull("No UID in shadow", shadow, ConnectorFactoryIcfImpl.ICFS_UID);
        }

        Holder<ObjectListType> listHolder = new Holder<ObjectListType>();
        assertNoRepoCache();

        modelWeb.listObjects(ObjectTypes.USER.getObjectTypeUri(), null, null,
                listHolder, resultHolder);

        assertNoRepoCache();
        ObjectListType uobjects = listHolder.value;
        assertSuccess("listObjects has failed", resultHolder.value);
        AssertJUnit.assertFalse("No users created", uobjects.getObject().isEmpty());

        // TODO: use another account, not guybrush
//        try {
//            AccountShadowType guybrushShadow = modelService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj, null, new OperationResult("get shadow"));
//            display("Guybrush shadow (" + accountShadowOidGuybrushOpendj + ")", guybrushShadow);
//        } catch (ObjectNotFoundException e) {
//            System.out.println("NO GUYBRUSH SHADOW");
//            // TODO: fail
//        }
        
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
            
            assertTrue("User "+user.getName()+" is disabled", user.getActivation() == null || user.getActivation().isEnabled() == null ||
            		user.getActivation().isEnabled());

            List<ObjectReferenceType> accountRefs = user.getAccountRef();
            AssertJUnit.assertEquals("Wrong accountRef for user " + user.getName(), 1, accountRefs.size());
            ObjectReferenceType accountRef = accountRefs.get(0);

            boolean found = false;
            for (PrismObject<AccountShadowType> aObject : sobjects) {
                AccountShadowType acc = aObject.asObjectable();
                if (accountRef.getOid().equals(acc.getOid())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                AssertJUnit.fail("accountRef does not point to existing account " + accountRef.getOid());
            }
            
            PrismObject<AccountShadowType> aObject = modelService.getObject(AccountShadowType.class, accountRef.getOid(), null, task, result);
            AccountShadowType account = aObject.asObjectable();
            
            display("Account after import ", account);
            
            String attributeValueL = ResourceObjectShadowUtil.getMultiStringAttributeValueAsSingle(account, 
            		new QName(ResourceTypeUtil.getResourceNamespace(resourceTypeOpenDjrepo), "l"));
            assertEquals("Unexcpected value of l", "middle of nowhere", attributeValueL);
        }
        
        // This also includes "idm" user imported from LDAP. Later we need to ignore that one.
        assertEquals("Wrong number of users after import", 10, uobjects.getObject().size());
        
        checkAllShadows();
    }

    @Test
    public void test300RecomputeUsers() throws Exception {
        displayTestTile("test300RecomputeUsers");
        // GIVEN

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test300RecomputeUsers");

        // Assign role to a user, but we do this using a repository instead of model.
        // The role assignment will not be executed and this created an inconsistent state.
        ObjectModificationType changeAddRoleCaptain = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_1_FILENAME, ObjectModificationType.class);
        Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(changeAddRoleCaptain,
        		UserType.class, prismContext);
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
        assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);
        AssertJUnit.assertNotNull(task.getTaskIdentifier());
        assertFalse(task.getTaskIdentifier().isEmpty());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, TASK_USER_RECOMPUTE_OID, result);
        display("Task after pickup in the repository", o.asObjectable());

        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());
        // The task may still be claimed if we are too fast
        //AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

        // .. and last run should not be zero
        assertNotNull(task.getLastRunStartTimestamp());
        AssertJUnit.assertFalse(task.getLastRunStartTimestamp().longValue() == 0);
        assertNotNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertFalse(task.getLastRunFinishTimestamp().longValue() == 0);

        // The progress should be 0, as there were no changes yet
        AssertJUnit.assertEquals(0, task.getProgress());

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);

        // STOP the task. We don't need it any more and we don't want to give it a chance to run more than once
        taskManager.deleteTask(TASK_USER_RECOMPUTE_OID, result);

        // CHECK RESULT: account created for user guybrush

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> object = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUser = object.asObjectable();

        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals("Wrong number of accountRefs after recompute for user "+repoUser.getName(), 1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

         PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadowType, new QName("shadow"));
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        accountGuybrushOpendjEntryUuuid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

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
    public void test310ReconcileResourceOpenDj() throws Exception {
        displayTestTile("test310ReconcileResourceOpenDj");
        // GIVEN

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test310ReconcileResourceOpenDj");

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

        addObjectFromFile(TASK_OPENDJ_RECON_FILENAME, TaskType.class, result);


        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task

        waitFor("Waiting for task to finish first run", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(TASK_OPENDJ_RECON_OID, result);
                display("Task while waiting for task manager to pick up the task", task);
                // wait until the task is finished
                return task.getLastRunFinishTimestamp() != null;
//                if (TaskExclusivityStatus.CLAIMED == task.getExclusivityStatus()) {			we cannot check exclusivity status for now
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
        }, 180000);

        // Check task status

        Task task = taskManager.getTask(TASK_OPENDJ_RECON_OID, result);
        result.computeStatus();
        display("getTask result", result);
        assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, TASK_OPENDJ_RECON_OID, result);
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
        AssertJUnit.assertEquals(0, task.getProgress());

        // Test for presence of a result. It should be there and it should
        // indicate success
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull(taskResult);

        // STOP the task. We don't need it any more and we don't want to give it a chance to run more than once
        taskManager.deleteTask(TASK_OPENDJ_RECON_OID, result);

        // CHECK RESULT: account for user guybrush should be still there and unchanged

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals("Guybrush has wrong number of accounts", 1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                repoResult);
        AccountShadowType repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadowType, new QName("shadow"));
        assertNotNull(repoShadowType);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadowType.getResourceRef().getOid());

        accountGuybrushOpendjEntryUuuid = checkRepoShadow(repoShadow);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
        OpenDJController.assertAttribute(entry, "displayName", "Guybrush Threepwood");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object. It is not tolerant, therefore the other value should be gone now
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

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
        ObjectQuery query = ObjectQuery.createObjectQuery(EqualsFilter.createEqual(UserType.class, prismContext, UserType.F_NAME, ELAINE_NAME));
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, repoResult);
        assertEquals("Wrong number of Elaines", 1, users.size());
        repoUser = users.get(0).asObjectable();

        repoResult.computeStatus();
        displayJaxb("User Elaine (repository)", repoUser, new QName("user"));

        assertNotNull(repoUser.getOid());
        assertEquals(PrismTestUtil.createPolyStringType(ELAINE_NAME), repoUser.getName());
        PrismAsserts.assertEqualsPolyString("wrong repo givenName", "Elaine", repoUser.getGivenName());
        PrismAsserts.assertEqualsPolyString("wrong repo familyName", "Marley", repoUser.getFamilyName());
        PrismAsserts.assertEqualsPolyString("wrong repo fullName", "Elaine Marley", repoUser.getFullName());

        accountRefs = repoUser.getAccountRef();
        assertEquals("Elaine has wrong number of accounts", 1, accountRefs.size());
        accountRef = accountRefs.get(0);
        String accountShadowOidElaineOpendj = accountRef.getOid();
        assertFalse(accountShadowOidElaineOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        repoShadow = repositoryService.getObject(AccountShadowType.class, accountShadowOidElaineOpendj,
                repoResult);
        repoShadowType = repoShadow.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
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

        assertFalse("Shadow "+repoShadow+" has unexpected elements", hasOthers);
        assertNotNull(uid);
        
        return uid;
	}
    
    private AccountShadowType searchAccountByOid(final String accountOid) throws Exception {
        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<ObjectType> accountHolder = new Holder<ObjectType>();
        OperationOptionsType options = new OperationOptionsType();
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountOid, options, accountHolder, resultHolder);
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
//        Document doc = DOMUtil.getDocument();
//        Element nameElement = doc.createElementNS(SchemaConstants.C_NAME.getNamespaceURI(),
//                SchemaConstants.C_NAME.getLocalPart());
//        nameElement.setTextContent(name);
//        Element filter = QueryUtil.createEqualFilter(doc, null, nameElement);
//
//        QueryType query = new QueryType();
//        query.setFilter(filter);
    	ObjectQuery q = ObjectQueryUtil.createNameQuery(UserType.class, prismContext, name);
    	QueryType query = QueryConvertor.createQueryType(q, prismContext);
        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<ObjectListType> listHolder = new Holder<ObjectListType>();
        assertNoRepoCache();

        modelWeb.searchObjects(ObjectTypes.USER.getObjectTypeUri(), query, null, listHolder, resultHolder);

        assertNoRepoCache();
        ObjectListType objects = listHolder.value;
        assertSuccess("searchObjects has failed", resultHolder.value);
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
            public boolean check() throws Exception {
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
		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
        syncSettings.setAssignmentPolicyEnforcement(enforcementType);
        applySyncSettings(syncSettings);
	}

    private void assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType assignmentPolicy) throws
            ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("Asserting sync settings");
        PrismObject<SystemConfigurationType> systemConfigurationType = repositoryService.getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);
        result.computeStatus();
        assertSuccess("Asserting sync settings failed (result)", result);
        AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfigurationType.asObjectable().getGlobalAccountSynchronizationSettings();
        assertNotNull("globalAccountSynchronizationSettings is null", globalAccountSynchronizationSettings);
        AssignmentPolicyEnforcementType assignmentPolicyEnforcement = globalAccountSynchronizationSettings.getAssignmentPolicyEnforcement();
        assertNotNull("assignmentPolicyEnforcement is null", assignmentPolicyEnforcement);
        assertEquals("Assignment policy mismatch", assignmentPolicy, assignmentPolicyEnforcement);
    }
    
    private void checkAllShadows() throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
    	LOGGER.trace("Checking all shadows");
    	System.out.println("Checking all shadows");
		ObjectChecker<AccountShadowType> checker = null;
		IntegrationTestTools.checkAllShadows(resourceTypeOpenDjrepo, repositoryService, checker, prismContext);		
	}	

}
