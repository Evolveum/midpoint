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
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1_wsdl.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_1.CredentialsCapabilityType;
import org.opends.server.core.ModifyOperation;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.*;
import org.opends.server.util.ChangeRecordEntry;
import org.opends.server.util.LDIFReader;
import org.opends.server.util.ModifyChangeRecordEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
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
 * It is using mock BaseX repository and embedded OpenDJ instance as a testing
 * resource. The BaseX repository is instantiated from the Spring context in the
 * same way as all other components. OpenDJ instance is started explicitly using
 * BeforeClass method. Appropriate resource definition to reach the OpenDJ
 * instance is provided in the test data and is inserted in the repository as
 * part of test initialization.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-provisioning.xml", "classpath:application-context-sanity-test.xml",
        "classpath:application-context-task.xml", "classpath:application-context-repository.xml",
        "classpath:application-context-configuration-test.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSanity extends AbstractIntegrationTest {

    private static final String OPENDJ_PEOPLE_SUFFIX = "ou=people,dc=example,dc=com";

    private static final String SYSTEM_CONFIGURATION_FILENAME = "src/test/resources/repo/system-configuration.xml";
    private static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";

    private static final String RESOURCE_OPENDJ_FILENAME = "src/test/resources/repo/resource-opendj.xml";
    private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

    private static final String RESOURCE_DERBY_FILENAME = "src/test/resources/repo/resource-derby.xml";
    private static final String RESOURCE_DERBY_OID = "ef2bc95b-76e0-59e2-86d6-999902d3abab";

    private static final String RESOURCE_BROKEN_FILENAME = "src/test/resources/repo/resource-broken.xml";
    private static final String RESOURCE_BROKEN_OID = "ef2bc95b-76e0-59e2-ffff-ffffffffffff";

    private static final String CONNECTOR_BROKEN_FILENAME = "src/test/resources/repo/connector-broken.xml";
    private static final String CONNECTOR_BROKEN_OID = "cccccccc-76e0-59e2-ffff-ffffffffffff";

    private static final String TASK_OPENDJ_SYNC_FILENAME = "src/test/resources/repo/opendj-sync-task.xml";
    private static final String TASK_OPENDJ_SYNC_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";

    private static final String TASK_USER_RECOMPUTE_FILENAME = "src/test/resources/repo/task-user-recompute.xml";
    private static final String TASK_USER_RECOMPUTE_OID = "91919191-76e0-59e2-86d6-3d4f02d3aaaa";

    private static final String TASK_OPENDJ_RECON_FILENAME = "src/test/resources/repo/task-opendj-reconciliation.xml";
    private static final String TASK_OPENDJ_RECON_OID = "91919191-76e0-59e2-86d6-3d4f02d30000";

    private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = "src/test/resources/repo/sample-configuration-object.xml";
    private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";

    private static final String USER_TEMPLATE_FILENAME = "src/test/resources/repo/user-template.xml";
    private static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

    private static final String USER_ADMINISTRATOR_FILENAME = "src/test/resources/repo/user-administrator.xml";
    private static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

    private static final String USER_JACK_FILENAME = "src/test/resources/repo/user-jack.xml";
    private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
    private static final String USER_JACK_LDAP_UID = "jack";
    private static final String USER_JACK_LDAP_DN = "uid=" + USER_JACK_LDAP_UID
            + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final String USER_GUYBRUSH_FILENAME = "src/test/resources/repo/user-guybrush.xml";
    private static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111222";
    private static final String USER_GUYBRUSH_USERNAME = "guybrush";
    private static final String USER_GUYBRUSH_LDAP_UID = "guybrush";
    private static final String USER_GUYBRUSH_LDAP_DN = "uid=" + USER_GUYBRUSH_LDAP_UID
            + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final String USER_E_LINK_ACTION = "src/test/resources/repo/user-e.xml";
    private static final String LDIF_E_FILENAME_LINK = "src/test/resources/request/e-create.ldif";

    private static final String ROLE_PIRATE_FILENAME = "src/test/resources/repo/role-pirate.xml";
    private static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-987987987988";

    private static final String ROLE_SAILOR_FILENAME = "src/test/resources/repo/role-sailor.xml";
    private static final String ROLE_SAILOR_OID = "12345678-d34d-b33f-f00d-987955553535";

    private static final String ROLE_CAPTAIN_FILENAME = "src/test/resources/repo/role-captain.xml";
    private static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-987987cccccc";

    private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME = "src/test/resources/request/user-modify-add-account.xml";

    private static final String REQUEST_USER_MODIFY_ADD_ACCOUNT_DERBY_FILENAME = "src/test/resources/request/user-modify-add-account-derby.xml";
    private static final String USER_JACK_DERBY_LOGIN = "jsparrow";

    private static final String REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME = "src/test/resources/request/user-modify-fullname-locality.xml";
    private static final String REQUEST_USER_MODIFY_PASSWORD_FILENAME = "src/test/resources/request/user-modify-password.xml";
    private static final String REQUEST_USER_MODIFY_ACTIVATION_DISABLE_FILENAME = "src/test/resources/request/user-modify-activation-disable.xml";
    private static final String REQUEST_USER_MODIFY_ACTIVATION_ENABLE_FILENAME = "src/test/resources/request/user-modify-activation-enable.xml";

    private static final String REQUEST_USER_MODIFY_ADD_ROLE_PIRATE_FILENAME = "src/test/resources/request/user-modify-add-role-pirate.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_1_FILENAME = "src/test/resources/request/user-modify-add-role-captain-1.xml";
    private static final String REQUEST_USER_MODIFY_ADD_ROLE_CAPTAIN_2_FILENAME = "src/test/resources/request/user-modify-add-role-captain-2.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_PIRATE_FILENAME = "src/test/resources/request/user-modify-delete-role-pirate.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_1_FILENAME = "src/test/resources/request/user-modify-delete-role-captain-1.xml";
    private static final String REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_2_FILENAME = "src/test/resources/request/user-modify-delete-role-captain-2.xml";

    private static final String REQUEST_ACCOUNT_MODIFY_ATTRS_FILENAME = "src/test/resources/request/account-modify-attrs.xml";

    private static final String LDIF_WILL_FILENAME = "src/test/resources/request/will.ldif";
    private static final String LDIF_WILL_MODIFY_FILENAME = "src/test/resources/request/will-modify.ldif";
    private static final String LDIF_WILL_WITHOUT_LOCATION_FILENAME = "src/test/resources/request/will-without-location.ldif";
    private static final String WILL_NAME = "wturner";

    private static final String LDIF_ELAINE_FILENAME = "src/test/resources/request/elaine.ldif";
    private static final String ELAINE_NAME = "elaine";
    
    private static final String LDIF_GIBBS_MODIFY_FILENAME = "src/test/resources/request/gibbs-modify.ldif";

    private static final QName IMPORT_OBJECTCLASS = new QName(
            "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff",
            "AccountObjectClass");

    private static final Trace LOGGER = TraceManager.getTrace(TestSanity.class);

    /**
     * Unmarshalled resource definition to reach the embedded OpenDJ instance.
     * Used for convenience - the tests method may find it handy.
     */
    private static ResourceType resourceOpenDj;
    private static ResourceType resourceDerby;
    private static String accountShadowOidOpendj;
    private static String accountShadowOidDerby;
    private static String accountShadowOidGuybrushOpendj;
    private static String accountGuybrushOpendjEntryUuuid = null;
    private static String originalJacksPassword;

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
    private SchemaRegistry schemaRegistry;

    public TestSanity() throws JAXBException {
        super();
        // TODO: fix this
        //IntegrationTestTools.checkResults = false;
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        LOGGER.info("BEFORE METHOD");
        OperationResult result = new OperationResult("get administrator");
        PrismObject<UserType> object = modelService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(),
                null, result);

        assertNotNull("Administrator user is null", object.asObjectable());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(object.asObjectable(), null));

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
        addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, initResult);
        addObjectFromFile(USER_ADMINISTRATOR_FILENAME, initResult);

        // This should discover the connectors
        LOGGER.trace("initSystem: trying modelService.postInit()");
        modelService.postInit(initResult);
        LOGGER.trace("initSystem: modelService.postInit() done");

        // Add broken connector before importing resources
        addObjectFromFile(CONNECTOR_BROKEN_FILENAME, initResult);

        // Need to import instead of add, so the (dynamic) connector reference
        // will be resolved
        // correctly
        importObjectFromFile(RESOURCE_OPENDJ_FILENAME, initResult);
        importObjectFromFile(RESOURCE_DERBY_FILENAME, initResult);
        importObjectFromFile(RESOURCE_BROKEN_FILENAME, initResult);

        addObjectFromFile(SAMPLE_CONFIGURATION_OBJECT_FILENAME, initResult);
        addObjectFromFile(USER_TEMPLATE_FILENAME, initResult);
        addObjectFromFile(ROLE_SAILOR_FILENAME, initResult);
        addObjectFromFile(ROLE_PIRATE_FILENAME, initResult);
        addObjectFromFile(ROLE_CAPTAIN_FILENAME, initResult);
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

        assertCache();

        OperationResult result = new OperationResult(TestSanity.class.getName() + ".test000Integrity");

        // Check if OpenDJ resource was imported correctly

        PrismObject<ResourceType> openDjResource = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null,
                result);
        display("Imported OpenDJ resource (repository)", openDjResource);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());

        assertCache();

        String ldapConnectorOid = openDjResource.asObjectable().getConnectorRef().getOid();
        PrismObject<ConnectorType> ldapConnector = repositoryService.getObject(ConnectorType.class, ldapConnectorOid, null, result);
        display("LDAP Connector: ", ldapConnector);

        // Check if Derby resource was imported correctly

        PrismObject<ResourceType> derbyResource = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null,
                result);
        AssertJUnit.assertEquals(RESOURCE_DERBY_OID, derbyResource.getOid());

        assertCache();

        String dbConnectorOid = derbyResource.asObjectable().getConnectorRef().getOid();
        PrismObject<ConnectorType> dbConnector = repositoryService.getObject(ConnectorType.class, dbConnectorOid, null, result);
        display("DB Connector: ", dbConnector);

        // Check if password was encrypted during import
        Object configurationPropertiesElement = JAXBUtil.findElement(derbyResource.asObjectable().getConfiguration().getAny(),
                new QName(dbConnector.asObjectable().getNamespace(), "configurationProperties"));
        Object passwordElement = JAXBUtil.findElement(JAXBUtil.listChildElements(configurationPropertiesElement),
                new QName(dbConnector.asObjectable().getNamespace(), "password"));
        System.out.println("Password element: " + passwordElement);


        // TODO: test if OpenDJ and Derby are running
    }

    /**
     * Test the testResource method. Expect a complete success for now.
     */
    @Test
    public void test001TestConnectionOpenDJ() throws FaultMessage, JAXBException, ObjectNotFoundException,
            SchemaException, CommunicationException {
        displayTestTile("test001TestConnectionOpenDJ");

        // GIVEN

        assertCache();

        // WHEN
        OperationResultType result = modelWeb.testResource(RESOURCE_OPENDJ_OID);

        // THEN

        assertCache();

        displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

        assertSuccess("testResource has failed", result);

        OperationResult opResult = new OperationResult(TestSanity.class.getName() + ".test001TestConnectionOpenDJ");

        PrismObject<ResourceType> rObject = repositoryService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult);
        resourceOpenDj = rObject.asObjectable();

        assertCache();
        assertEquals(RESOURCE_OPENDJ_OID, resourceOpenDj.getOid());
        display("Initialized OpenDJ resource (respository)", resourceOpenDj);
        assertNotNull("Resource schema was not generated", resourceOpenDj.getSchema());
        assertFalse("Resource schema was not generated", resourceOpenDj.getSchema().getAny().isEmpty());

        PrismObject<ResourceType> openDjResourceProvisioninig = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null,
                opResult);
        display("Initialized OpenDJ resource resource (provisioning)", openDjResourceProvisioninig);

        PrismObject<ResourceType> openDjResourceModel = provisioningService.getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null,
                opResult);
        display("Initialized OpenDJ resource OpenDJ resource (model)", openDjResourceModel);

        checkOpenDjResource(resourceOpenDj, "repository");
        checkOpenDjResource(openDjResourceProvisioninig.asObjectable(), "provisioning");
        checkOpenDjResource(openDjResourceModel.asObjectable(), "model");
        // TODO: model web

    }

    /**
     * Checks if the resource is internally consistent, if it has everything it should have.
     *
     * @throws SchemaException
     */
    private void checkOpenDjResource(ResourceType resource, String source) throws SchemaException {
        assertNotNull("Resource from " + source + " is null", resource);
        assertNotNull("Resource from " + source + " has null configuration", resource.getConfiguration());
        assertNotNull("Resource from " + source + " has null schema", resource.getSchema());
        checkOpenDjSchema(resource, source);
        assertNotNull("Resource from " + source + " has null schemahandling", resource.getSchemaHandling());
        if (!source.equals("repository")) {
            // This is generated on the fly in provisioning
            assertNotNull("Resource from " + source + " has null nativeCapabilities", resource.getNativeCapabilities());
            assertFalse("Resource from " + source + " has empty nativeCapabilities", resource.getNativeCapabilities().getAny().isEmpty());
        }
        assertNotNull("Resource from " + source + " has null capabilities", resource.getCapabilities());
        assertFalse("Resource from " + source + " has empty capabilities", resource.getCapabilities().getAny().isEmpty());
        assertNotNull("Resource from " + source + " has null synchronization", resource.getSynchronization());
    }

    /**
     * @param resource
     * @param source
     * @throws SchemaException
     */
    private void checkOpenDjSchema(ResourceType resource, String source) throws SchemaException {
        PrismSchema schema = RefinedResourceSchema.getResourceSchema(resource, schemaRegistry.getPrismContext());
        ResourceAttributeContainerDefinition accountDefinition = schema.findAccountDefinition();
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

    /**
     * Test the testResource method. Expect a complete success for now.
     */
    @Test
    public void test002TestConnectionDerby() throws FaultMessage, JAXBException, ObjectNotFoundException,
            SchemaException, CommunicationException {
        displayTestTile("test002TestConnectionDerby");

        // GIVEN

        assertCache();

        // WHEN
        OperationResultType result = modelWeb.testResource(RESOURCE_DERBY_OID);

        // THEN

        assertCache();
        displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

        assertSuccess("testResource has failed", result.getPartialResults().get(0));

        OperationResult opResult = new OperationResult(TestSanity.class.getName() + ".test002TestConnectionDerby");

        PrismObject<ResourceType> rObject = repositoryService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null, opResult);
        resourceDerby = rObject.asObjectable();

        assertCache();
        assertEquals(RESOURCE_DERBY_OID, resourceDerby.getOid());
        display("Initialized Derby resource (respository)", resourceDerby);
        assertNotNull("Resource schema was not generated", resourceDerby.getSchema());
        assertFalse("Resource schema was not generated", resourceDerby.getSchema().getAny().isEmpty());

        PrismObject<ResourceType> derbyResourceProvisioninig = provisioningService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null,
                opResult);
        display("Initialized Derby resource (provisioning)", derbyResourceProvisioninig);

        PrismObject<ResourceType> derbyResourceModel = provisioningService.getObject(ResourceType.class, RESOURCE_DERBY_OID, null,
                opResult);
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

        assertCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        // WHEN
        modelWeb.getObject(ObjectTypes.RESOURCE.getObjectTypeUri(), RESOURCE_OPENDJ_OID,
                resolve, objectHolder, resultHolder);

        ResourceType resource = (ResourceType) objectHolder.value;

        // THEN
        display("Resource", resource);

        assertCache();

        CapabilitiesType nativeCapabilities = resource.getNativeCapabilities();
        List<Object> capabilities = nativeCapabilities.getAny();
        assertFalse("Empty capabilities returned", capabilities.isEmpty());

        for (Object capability : nativeCapabilities.getAny()) {
            System.out.println("Native Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : " + capability);
        }

        if (resource.getCapabilities() != null) {
            for (Object capability : resource.getCapabilities().getAny()) {
                System.out.println("Configured Capability: " + ResourceTypeUtil.getCapabilityDisplayName(capability) + " : " + capability);
            }
        }

        List<Object> effectiveCapabilities = ResourceTypeUtil.listEffectiveCapabilities(resource);
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
        displayTestTile("test012AddUser");

        // GIVEN
        assertCache();

        UserType user = unmarshallJaxbFromFile(USER_JACK_FILENAME, UserType.class);

        // Encrypt Jack's password
        protector.encrypt(user.getCredentials().getPassword().getProtectedString());

        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
        Holder<String> oidHolder = new Holder<String>();

        display("Adding user object", user);

        // WHEN
        modelWeb.addObject(user, oidHolder, resultHolder);

        // THEN

        assertCache();
        displayJaxb("addObject result:", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("addObject has failed", resultHolder.value);

        AssertJUnit.assertEquals(USER_JACK_OID, oidHolder.value);

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, oidHolder.value, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();

        repoResult.computeStatus();
        display("repository.getObject result", repoResult);
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
    public void test013AddOpenDjAccountToUser() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, DirectoryException {
        displayTestTile("test013AddOpenDjAccountToUser");

        // GIVEN

        assertCache();

        // IMPORTANT! SWITCHING OFF ASSIGNMENT ENFORCEMENT HERE!
        AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
        syncSettings.setAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.NONE);
        applySyncSettings(syncSettings);

        assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType.NONE);

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ACCOUNT_OPENDJ_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();

        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals("No accountRefs", 1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidOpendj = accountRef.getOid();
        assertFalse(accountShadowOidOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("addObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // Check the "name" property, it should be set to DN, not entryUUID
        assertEquals("Wrong name property", USER_JACK_LDAP_DN.toLowerCase(), repoShadow.getName().toLowerCase());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = null;
        boolean hasOthers = false;
        List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
                if (uid != null) {
                    AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
                } else {
                    uid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        assertFalse(hasOthers);
        assertNotNull(uid);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        OpenDJController.assertAttribute(entry, "cn", "Jack Sparrow");
        // The "l" attribute is assigned indirectly through schemaHandling and
        // config object
        OpenDJController.assertAttribute(entry, "l", "middle of nowhere");

        originalJacksPassword = OpenDJController.getAttributeValue(entry, "userPassword");
        assertNotNull("Pasword was not set on create", originalJacksPassword);
        System.out.println("password after create: " + originalJacksPassword);

        // Use getObject to test fetch of complete shadow

        assertCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj,
                resolve, objectHolder, resultHolder);

        // THEN
        assertCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        displayJaxb("Shadow (model)", modelShadow, new QName("shadow"));

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
        assertAttribute(modelShadow, resourceOpenDj, "uid", "jack");
        assertAttribute(modelShadow, resourceOpenDj, "givenName", "Jack");
        assertAttribute(modelShadow, resourceOpenDj, "sn", "Sparrow");
        assertAttribute(modelShadow, resourceOpenDj, "cn", "Jack Sparrow");
        assertAttribute(modelShadow, resourceOpenDj, "l", "middle of nowhere");
        assertNull("carLicense attribute sneaked to LDAP", OpenDJController.getAttributeValue(entry, "carLicense"));

        assertNotNull("Activation is null", modelShadow.getActivation());
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

        assertCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ACCOUNT_DERBY_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();

        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

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

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidDerby,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("addObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_DERBY_OID, repoShadow.getResourceRef().getOid());

        // Check the "name" property, it should be set to DN, not entryUUID
        assertEquals("Wrong name property", USER_JACK_DERBY_LOGIN, repoShadow.getName());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        String uid = null;
        boolean hasOthers = false;
        List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
                if (uid != null) {
                    AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
                } else {
                    uid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        assertFalse(hasOthers);
        assertNotNull(uid);

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

        assertCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidDerby,
                resolve, objectHolder, resultHolder);

        // THEN
        assertCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        displayJaxb("Shadow (model)", modelShadow, new QName("shadow"));

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_DERBY_OID, modelShadow.getResourceRef().getOid());

        assertAttribute(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID, USER_JACK_DERBY_LOGIN);
        assertAttribute(modelShadow, ConnectorFactoryIcfImpl.ICFS_NAME, USER_JACK_DERBY_LOGIN);
        assertAttribute(modelShadow, resourceDerby, "FULL_NAME", "Cpt. Jack Sparrow");

    }

    @Test
    public void test015AccountOwner() throws FaultMessage {
        displayTestTile("test015AccountOwner");

        // GIVEN

        assertCache();

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<UserType> userHolder = new Holder<UserType>();

        // WHEN

        modelWeb.listAccountShadowOwner(accountShadowOidOpendj, userHolder, resultHolder);

        // THEN

        assertSuccess("listAccountShadowOwner has failed (result)", resultHolder.value);
        UserType user = userHolder.value;
        assertNotNull("No owner", user);
        assertEquals(USER_JACK_OID, user.getOid());

        System.out.println("Account " + accountShadowOidOpendj + " has owner " + ObjectTypeUtil.toShortString(user));
    }

    @Test
    public void test016ProvisioningSearchAccountsIterative() throws SchemaException, ObjectNotFoundException,
            CommunicationException {
        displayTestTile("test016ProvisioningSearchAccountsIterative");

        // GIVEN
        OperationResult result = new OperationResult(TestSanity.class.getName() + ".test016ProvisioningSearchAccountsIterative");

        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceOpenDj, schemaRegistry.getPrismContext());
        RefinedAccountDefinition refinedAccountDefinition = refinedSchema.getDefaultAccountDefinition();

        QName objectClass = refinedAccountDefinition.getObjectClassDefinition().getTypeName();
        QueryType query = QueryUtil.createResourceAndAccountQuery(resourceOpenDj, objectClass, null);

        final Collection<ObjectType> objects = new HashSet<ObjectType>();

        ResultHandler handler = new ResultHandler<ObjectType>() {

            @Override
            public boolean handle(PrismObject<ObjectType> prismObject, OperationResult parentResult) {
                ObjectType object = prismObject.asObjectable();
                objects.add(object);

                display("Found object", object);

                assertTrue(object instanceof AccountShadowType);
                AccountShadowType shadow = (AccountShadowType) object;
                assertNotNull(shadow.getOid());
                assertNotNull(shadow.getName());
                assertEquals(new QName(resourceOpenDj.getNamespace(), "AccountObjectClass"), shadow.getObjectClass());
                assertEquals(RESOURCE_OPENDJ_OID, shadow.getResourceRef().getOid());
                String icfUid = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "uid"));
                assertNotNull("No ICF UID", icfUid);
                String icfName = getAttributeValue(shadow, new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA, "name"));
                assertNotNull("No ICF NAME", icfName);
                assertEquals("Wrong shadow name", shadow.getName(), icfName);
                assertNotNull("Missing LDAP uid", getAttributeValue(shadow, new QName(resourceOpenDj.getNamespace(), "uid")));
                assertNotNull("Missing LDAP cn", getAttributeValue(shadow, new QName(resourceOpenDj.getNamespace(), "cn")));
                assertNotNull("Missing LDAP sn", getAttributeValue(shadow, new QName(resourceOpenDj.getNamespace(), "sn")));
                assertNotNull("Missing activation", shadow.getActivation());
                assertNotNull("Missing activation/enabled", shadow.getActivation().isEnabled());
                return true;
            }
        };

        // WHEN

        provisioningService.searchObjectsIterative(AccountShadowType.class, query, null, handler, result);

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

        assertCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_FULLNAME_LOCALITY_FILENAME, ObjectModificationType.class);

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();
        PrismObject<ObjectType> object = repositoryService.getObject(ObjectType.class, USER_JACK_OID, resolve, repoResult);
        ObjectType repoObject = object.asObjectable(); 
        UserType repoUser = (UserType) repoObject;
        displayJaxb("repository user", repoUser, new QName("user"));

        assertEquals("Cpt. Jack Sparrow", repoUser.getFullName());
        assertEquals("somewhere", repoUser.getLocality());

        // Check if appropriate accountRef is still there

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue(
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));

        }

        // Check if shadow is still in the repo and that it is untouched

        repoResult = new OperationResult("getObject");
        object = repositoryService.getObject(ObjectType.class, accountShadowOidOpendj, resolve, repoResult);
        repoObject = object.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        AccountShadowType repoShadow = (AccountShadowType) repoObject;
        displayJaxb("repository shadow", repoShadow, new QName("shadow"));
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
                    uid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        AssertJUnit.assertFalse(hasOthers);
        assertNotNull(uid);

        // Check if LDAP account was updated
        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);

        display(entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        // These two should be assigned from the User modification by
        // schemaHandling
        OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
        // This will get translated from "somewhere" to this (outbound expression in schemeHandling)
        OpenDJController.assertAttribute(entry, "l", "There there over the corner");

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

        System.out.println("In modification: " + objectChange.getPropertyModification().get(0).getValue().getAny().get(0));
        assertCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();
        PrismObject<ObjectType> object = repositoryService.getObject(ObjectType.class, USER_JACK_OID, resolve, repoResult);
        ObjectType repoObject = object.asObjectable();
        UserType repoUser = (UserType) repoObject;
        displayJaxb("repository user", repoUser, new QName("user"));

        // Check if nothing else was modified
        AssertJUnit.assertEquals("Cpt. Jack Sparrow", repoUser.getFullName());
        AssertJUnit.assertEquals("somewhere", repoUser.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue(
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));

        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");
        object = repositoryService.getObject(ObjectType.class, accountShadowOidOpendj, resolve, repoResult);
        repoObject = object.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        AccountShadowType repoShadow = (AccountShadowType) repoObject;
        displayJaxb("repository shadow", repoShadow, new QName("shadow"));
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
                    uid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        AssertJUnit.assertFalse(hasOthers);
        assertNotNull(uid);

        // Check if LDAP account was updated

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(uid);
        display(entry);

        OpenDJController.assertAttribute(entry, "uid", "jack");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        // These two should be assigned from the User modification by
        // schemaHandling
        OpenDJController.assertAttribute(entry, "cn", "Cpt. Jack Sparrow");
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
        OpenDJController.assertAttribute(entry, "l", "There there over the corner");

        String pwpAccountDisabled = OpenDJController.getAttributeValue(entry, "ds-pwp-account-disabled");
        System.out.println("ds-pwp-account-disabled before change: " + pwpAccountDisabled);
        System.out.println();
        assertNull(pwpAccountDisabled);
        assertCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();
        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        displayJaxb("repository user", repoUser, new QName("user"));

        // Check if nothing else was modified
        AssertJUnit.assertEquals("Cpt. Jack Sparrow", repoUser.getFullName());
        AssertJUnit.assertEquals("somewhere", repoUser.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue(
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));
        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj, resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();

        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        displayJaxb("repo shadow", repoShadow, new QName("shadow"));
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
                    uid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        AssertJUnit.assertFalse(hasOthers);
        assertNotNull(uid);

        // Use getObject to test fetch of complete shadow

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        assertCache();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj,
                resolve, objectHolder, resultHolder);

        // THEN
        assertCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        displayJaxb("Shadow (model)", modelShadow, new QName("shadow"));

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
        assertAttribute(modelShadow, resourceOpenDj, "uid", "jack");
        assertAttribute(modelShadow, resourceOpenDj, "givenName", "Jack");
        assertAttribute(modelShadow, resourceOpenDj, "sn", "Sparrow");
        assertAttribute(modelShadow, resourceOpenDj, "cn", "Cpt. Jack Sparrow");
        assertAttribute(modelShadow, resourceOpenDj, "l", "There there over the corner");

        assertNotNull("The account activation is null in the shadow", modelShadow.getActivation());
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
        assertCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result:", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();
        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        displayJaxb("repo user", repoUser, new QName("user"));

        // Check if nothing else was modified
        AssertJUnit.assertEquals("Cpt. Jack Sparrow", repoUser.getFullName());
        AssertJUnit.assertEquals("somewhere", repoUser.getLocality());

        // Check if appropriate accountRef is still there
        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(2, accountRefs.size());
        for (ObjectReferenceType accountRef : accountRefs) {
            assertTrue(
                    accountRef.getOid().equals(accountShadowOidOpendj) ||
                            accountRef.getOid().equals(accountShadowOidDerby));
        }

        // Check if shadow is still in the repo and that it is untouched
        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj, resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();

        repoResult.computeStatus();
        assertSuccess("getObject(repo) has failed", repoResult);
        displayJaxb("repo shadow", repoShadow, new QName("shadow"));
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
                    uid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        AssertJUnit.assertFalse(hasOthers);
        assertNotNull(uid);

        // Use getObject to test fetch of complete shadow

        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        assertCache();

        // WHEN
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidOpendj,
                resolve, objectHolder, resultHolder);

        // THEN
        assertCache();
        displayJaxb("getObject result", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("getObject has failed", resultHolder.value);

        AccountShadowType modelShadow = (AccountShadowType) objectHolder.value;
        displayJaxb("Shadow (model)", modelShadow, new QName("shadow"));

        AssertJUnit.assertNotNull(modelShadow);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, modelShadow.getResourceRef().getOid());

        assertAttributeNotNull(modelShadow, ConnectorFactoryIcfImpl.ICFS_UID);
        assertAttribute(modelShadow, resourceOpenDj, "uid", "jack");
        assertAttribute(modelShadow, resourceOpenDj, "givenName", "Jack");
        assertAttribute(modelShadow, resourceOpenDj, "sn", "Sparrow");
        assertAttribute(modelShadow, resourceOpenDj, "cn", "Cpt. Jack Sparrow");
        assertAttribute(modelShadow, resourceOpenDj, "l", "There there over the corner");

        assertNotNull("The account activation is null in the shadow", modelShadow.getActivation());
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
        PropertyModificationType modificationDeleteAccountRef = new PropertyModificationType();
        modificationDeleteAccountRef.setModificationType(PropertyModificationTypeType.delete);
        Value modificationValue = new Value();
        ObjectReferenceType accountRefToDelete = new ObjectReferenceType();
        accountRefToDelete.setOid(accountShadowOidDerby);
        JAXBElement<ObjectReferenceType> accountRefToDeleteElement = new JAXBElement<ObjectReferenceType>(SchemaConstants.I_ACCOUNT_REF, ObjectReferenceType.class, accountRefToDelete);
        modificationValue.getAny().add(accountRefToDeleteElement);
        modificationDeleteAccountRef.setValue(modificationValue);
        objectChange.getPropertyModification().add(modificationDeleteAccountRef);
        displayJaxb("modifyObject input", objectChange, new QName(SchemaConstants.NS_C, "change"));
        assertCache();

        // WHEN
        OperationResultType result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        // only OpenDJ account should be left now
        assertEquals(1, accountRefs.size());
        ObjectReferenceType ref = accountRefs.get(0);
        assertEquals(accountShadowOidOpendj, ref.getOid());

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

        assertCache();

        // WHEN
        OperationResultType result = modelWeb.deleteObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountShadowOidDerby);

        // THEN
        assertCache();
        displayJaxb("deleteObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("deleteObject has failed", result);

        // Check if shadow was deleted
        OperationResult repoResult = new OperationResult("getObject");

        try {
            repositoryService.getObject(AccountShadowType.class, accountShadowOidDerby,
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

        assertCache();

        // WHEN
        OperationResultType result = modelWeb.deleteObject(ObjectTypes.USER.getObjectTypeUri(), USER_JACK_OID);

        // THEN
        assertCache();
        displayJaxb("deleteObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("deleteObject has failed", result);

        // User should be gone from the repository
        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();
        try {
            repositoryService.getObject(UserType.class, USER_JACK_OID, resolve, repoResult);
            AssertJUnit.fail("User still exists in repo after delete");
        } catch (ObjectNotFoundException e) {
            // This is expected
        }

        // Account shadow should be gone from the repository
        repoResult = new OperationResult("getObject");
        try {
            repositoryService.getObject(AccountShadowType.class, accountShadowOidOpendj, resolve, repoResult);
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
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test050AssignRolePirate");

        // GIVEN

        // IMPORTANT! Assignment enforcement is back to default (FULL)
        AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
        applySyncSettings(syncSettings);

        UserType user = unmarshallJaxbFromFile(USER_GUYBRUSH_FILENAME, UserType.class);

        // Encrypt the password
        protector.encrypt(user.getCredentials().getPassword().getProtectedString());

        OperationResultType result = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(result);
        Holder<String> oidHolder = new Holder<String>();
        assertCache();

        modelWeb.addObject(user, oidHolder, resultHolder);

        assertCache();
        assertSuccess("addObject has failed", resultHolder.value);

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_ADD_ROLE_PIRATE_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        boolean hasOthers = false;
        List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
                if (accountGuybrushOpendjEntryUuuid != null) {
                    AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
                } else {
                    accountGuybrushOpendjEntryUuuid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        assertFalse(hasOthers);
        assertNotNull(accountGuybrushOpendjEntryUuuid);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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

        assertCache();

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
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // check if LDAP account was modified

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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


    @Test
    public void test057UnassignRolePirate() throws FileNotFoundException, JAXBException, FaultMessage,
            ObjectNotFoundException, SchemaException, EncryptionException, DirectoryException {
        displayTestTile("test057UnassignRolePirate");

        // GIVEN

        OperationResultType result = new OperationResultType();
        assertCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_PIRATE_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));


        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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
        assertCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_1_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));


        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        assertEquals(accountShadowOidGuybrushOpendj, accountRef.getOid());

        // Check if shadow is still in the repo

        repoResult = new OperationResult("getObject");

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check if account is still in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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
        assertCache();

        ObjectModificationType objectChange = unmarshallJaxbFromFile(
                REQUEST_USER_MODIFY_DELETE_ROLE_CAPTAIN_2_FILENAME, ObjectModificationType.class);

        // WHEN
        result = modelWeb.modifyObject(ObjectTypes.USER.getObjectTypeUri(), objectChange);

        // THEN
        assertCache();
        displayJaxb("modifyObject result", result, SchemaConstants.C_RESULT);
        assertSuccess("modifyObject has failed", result);

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
        UserType repoUser = uObject.asObjectable();
        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(0, accountRefs.size());

        // Check if shadow was deleted from the repo

        repoResult = new OperationResult("getObject");

        try {
            PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                    resolve, repoResult);
            AccountShadowType repoShadow = aObject.asObjectable();
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
    public void test060ListResourcesWithBrokenResource() {
        displayTestTile("test060ListResourcesWithBrokenResource");

        // GIVEN
        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test060ListResourcesWithBrokenResource");

        // WHEN
        ResultList<PrismObject<ResourceType>> resources = modelService.listObjects(ResourceType.class, null, result);

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
        display("getTask result", result);
        assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);

        PrismObject<ObjectType> o = repositoryService.getObject(ObjectType.class, TASK_OPENDJ_SYNC_OID, null, result);
        display("Task after pickup in the repository", o.asObjectable());

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
        PrismContainer taskExtension = task.getExtension();
        AssertJUnit.assertNotNull(taskExtension);
        display("Task extension", taskExtension);
        PrismProperty shipStateProp = taskExtension.findProperty(new QName("http://myself.me/schemas/whatever",
                "shipState"));
        AssertJUnit.assertEquals("capsized", shipStateProp.getValue(String.class).getValue());
        PrismProperty deadProp = taskExtension
                .findProperty(new QName("http://myself.me/schemas/whatever", "dead"));
        AssertJUnit.assertEquals(Integer.class, deadProp.getValues().iterator().next().getValue().getClass());
        AssertJUnit.assertEquals(Integer.valueOf(42), deadProp.getValue(Integer.class).getValue());

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
     * should be created in repo.
     *
     * @throws Exception
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

        // WHEN

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_WILL_FILENAME);
        display("Entry from LDIF", entry);

        // THEN

        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, result);

        // Search for the user that should be created now
        UserType user = searchUserByName(WILL_NAME);

        AssertJUnit.assertEquals(user.getName(), WILL_NAME);

        // TODO: more checks
    }

    @Test
    public void test102LiveSyncModify() throws Exception {
        displayTestTile("test102LiveSyncModify");

        LDIFImportConfig importConfig = new LDIFImportConfig(LDIF_WILL_MODIFY_FILENAME);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);
        display("Entry from LDIF", entry);

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test102LiveSyncModify");
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        final Object tokenBefore = findSyncToken(syncCycle);

        // WHEN
        ModifyOperation modifyOperation = openDJController.getInternalConnection()
                .processModify((ModifyChangeRecordEntry) entry);

        // THEN
        AssertJUnit.assertEquals("LDAP modify operation failed", ResultCode.SUCCESS,
                modifyOperation.getResultCode());

        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, result);
        // Search for the user that should be created now
        UserType user = searchUserByName (WILL_NAME);

        AssertJUnit.assertEquals(WILL_NAME, user.getName());
        AssertJUnit.assertEquals("asdf", user.getGivenName());
    }

    @Test
    public void test103LiveSyncLink() throws Exception {
        displayTestTile("test103LiveSyncLink");

        // GIVEN
        assertCache();
        UserType user = unmarshallJaxbFromFile(USER_E_LINK_ACTION, UserType.class);
        final String userOid = user.getOid();
        // Encrypt e's password
        protector.encrypt(user.getCredentials().getPassword().getProtectedString());
        // create user in repository
        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<String> oidHolder = new Holder<String>();
        display("Adding user object", user);
        modelWeb.addObject(user, oidHolder, resultHolder);
        //check results
        assertCache();
        displayJaxb("addObject result:", resultHolder.value, SchemaConstants.C_RESULT);
        assertSuccess("addObject has failed", resultHolder.value);
        AssertJUnit.assertEquals(userOid, oidHolder.value);

        //WHEN
        //create account for e which should be correlated
        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test103LiveSyncLink");
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        final Object tokenBefore = findSyncToken(syncCycle);

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_E_FILENAME_LINK);
        display("Entry from LDIF", entry);

        // THEN
        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, result);

        //check user and account ref
        user = searchUserByName("e");

        List<ObjectReferenceType> accountRefs = user.getAccountRef();
        assertEquals("Account ref not found, or found too many", 1, accountRefs.size());

        //check account defined by account ref
        String accountOid = accountRefs.get(0).getOid();
        AccountShadowType account = searchAccountByOid(accountOid);

        assertEquals("Name doesn't match", "uid=e,ou=People,dc=example,dc=com", account.getName());
    }

    /**
     * Create LDAP object. That should be picked up by liveSync and a user
     * should be created in repo.
     * Also location (ldap l) should be updated through outbound
     *
     * @throws Exception
     */
    @Test
    public void test104LiveSyncCreate() throws Exception {
        displayTestTile("test104LiveSyncCreate");
        // Sync task should be running (tested in previous test), so just create
        // new LDAP object.

        final OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test104LiveSyncCreate");
        final Task syncCycle = taskManager.getTask(TASK_OPENDJ_SYNC_OID, result);
        AssertJUnit.assertNotNull(syncCycle);

        final Object tokenBefore = findSyncToken(syncCycle);

        // WHEN
        Entry entry = openDJController.addEntryFromLdifFile(LDIF_WILL_WITHOUT_LOCATION_FILENAME);
        display("Entry from LDIF", entry);

        // THEN
        // Wait a bit to give the sync cycle time to detect the change
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, result, 60000);
        // Search for the user that should be created now
        final String USER_NAME = "wturner1";
        UserType user = searchUserByName(USER_NAME);

        List<ObjectReferenceType> accountRefs = user.getAccountRef();
        assertEquals("Account ref not found, or found too many", 1, accountRefs.size());

        //check account defined by account ref
        String accountOid = accountRefs.get(0).getOid();
        AccountShadowType account = searchAccountByOid(accountOid);

        assertEquals("Name doesn't match", "uid=" + USER_NAME + ",ou=People,dc=example,dc=com", account.getName());
        List<String> localities = getAttributeValues(account, new QName(IMPORT_OBJECTCLASS.getNamespaceURI(), "l"));
        assertNotNull(localities);
        assertEquals(1, localities.size());
        assertEquals("Locality doesn't match", "middle of nowhere", localities.get(0));
    }

    private Object findSyncToken(Task syncCycle) {
        Object token = null;
        PrismProperty tokenProperty = syncCycle.getExtension().findProperty(SchemaConstants.SYNC_TOKEN);
        if (tokenProperty != null) {
            token = tokenProperty.getValue();
        }

        return token;
    }

    private AccountShadowType searchAccountByOid(final String accountOid) throws Exception {
        OperationResultType resultType = new OperationResultType();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>(resultType);
        Holder<ObjectType> accountHolder = new Holder<ObjectType>();
        modelWeb.getObject(ObjectTypes.ACCOUNT.getObjectTypeUri(), accountOid, new PropertyReferenceListType(), accountHolder, resultHolder);
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

        modelWeb.searchObjects(ObjectTypes.USER.getObjectTypeUri(), query, null,
                listHolder, resultHolder);

        assertCache();
        ObjectListType objects = listHolder.value;
        assertSuccess("searchObjects has failed", resultHolder.value);
        AssertJUnit.assertEquals("User not found (or found too many)", 1, objects.getObject().size());
        UserType user = (UserType) objects.getObject().get(0);

        AssertJUnit.assertEquals(user.getName(), name);

        return user;
    }

    private void basicWaitForSyncChangeDetection(final Task syncCycle, final Object tokenBefore,
            final OperationResult result) throws Exception {
        basicWaitForSyncChangeDetection(syncCycle, tokenBefore, result, 40000);
    }

    private void basicWaitForSyncChangeDetection(final Task syncCycle, final Object tokenBefore,
            final OperationResult result, int timeout) throws Exception {

        waitFor("Waiting for sync cycle to detect change", new Checker() {
            @Override
            public boolean check() throws Exception {
                syncCycle.refresh(result);
                display("SyncCycle while waiting for sync cycle to detect change", syncCycle);
                Object tokenNow = findSyncToken(syncCycle);
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
        }, timeout);
    }

    /**
     * Not really a test. Just cleans up after live sync.
     *
     * @throws ObjectNotFoundException
     */
    @Test
    public void test199LiveSyncCleanup() throws ObjectNotFoundException {
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

        assertCache();

        OperationResult result = new OperationResult(TestSanity.class.getName()
                + ".test200ImportFromResource");
        
        // Make sure Mr. Gibbs has "l" attribute set to the same value as an outbound expression is setting
        LDIFImportConfig importConfig = new LDIFImportConfig(LDIF_GIBBS_MODIFY_FILENAME);
        LDIFReader ldifReader = new LDIFReader(importConfig);
        ChangeRecordEntry entry = ldifReader.readChangeRecord(false);
        display("Entry from LDIF", entry);
        ModifyOperation modifyOperation = openDJController.getInternalConnection()
        		.processModify((ModifyChangeRecordEntry) entry);
        AssertJUnit.assertEquals("LDAP modify operation failed", ResultCode.SUCCESS,
                modifyOperation.getResultCode());

        // WHEN
        TaskType taskType = modelWeb.importFromResource(RESOURCE_OPENDJ_OID, IMPORT_OBJECTCLASS);

        // THEN

        assertCache();
        displayJaxb("importFromResource result", taskType.getResult(), SchemaConstants.C_RESULT);
        AssertJUnit.assertEquals("importFromResource has failed", OperationResultStatusType.IN_PROGRESS, taskType.getResult().getStatus());
        // Convert the returned TaskType to a more usable Task
        Task task = taskManager.createTaskInstance(taskType.asPrismObject(), result);
        AssertJUnit.assertNotNull(task);
        assertNotNull(task.getOid());
        AssertJUnit.assertTrue(task.isAsynchronous());
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());
        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        display("Import task after launch", task);

        PrismObject<TaskType> tObject = repositoryService.getObject(TaskType.class, task.getOid(), null, result);
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
                assertCache();
                modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), taskOid,
                        new PropertyReferenceListType(), objectHolder, resultHolder);
                assertCache();
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

        
        //### Check task state after the task is finished ###
        
        Holder<ObjectType> objectHolder = new Holder<ObjectType>();
        Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
        assertCache();

        modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), task.getOid(),
                new PropertyReferenceListType(), objectHolder, resultHolder);

        assertCache();
        assertSuccess("getObject has failed", resultHolder.value);
        task = taskManager.createTaskInstance(objectHolder.value.asPrismObject(), result);

        display("Import task after finish (fetched from model)", task);

        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        long importDuration = task.getLastRunFinishTimestamp() - task.getLastRunStartTimestamp();
        double usersPerSec = (task.getProgress() * 1000) / importDuration;
        display("Imported " + task.getProgress() + " users in " + importDuration + " milliseconds (" + usersPerSec + " users/sec)");

        waitFor("Waiting for task to get released", new Checker() {
            @Override
            public boolean check() throws Exception {
                Holder<OperationResultType> resultHolder = new Holder<OperationResultType>();
                Holder<ObjectType> objectHolder = new Holder<ObjectType>();
                OperationResult opResult = new OperationResult("import check");
                assertCache();
                modelWeb.getObject(ObjectTypes.TASK.getObjectTypeUri(), taskOid,
                        new PropertyReferenceListType(), objectHolder, resultHolder);
                assertCache();
                //				display("getObject result (wait loop)",resultHolder.value);
                assertSuccess("getObject has failed", resultHolder.value);
                Task task = taskManager.createTaskInstance(objectHolder.value.asPrismObject(), opResult);
                System.out.println("Import task status: " + task.getExecutionStatus());
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
        }, 10000);

        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task has no result", taskResult);
        assertSuccess("Import task result is not success", taskResult);
        AssertJUnit.assertTrue("Task failed", taskResult.isSuccess());

        AssertJUnit.assertTrue("No progress", task.getProgress() > 0);

        //### Check if the import created users and shadows ###

        // Listing of shadows is not supported by the provisioning. So we need
        // to look directly into repository
        List<PrismObject<AccountShadowType>> sobjects = repositoryService.listObjects(AccountShadowType.class, null,
                result);
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
        assertCache();

        modelWeb.listObjects(ObjectTypes.USER.getObjectTypeUri(), null,
                listHolder, resultHolder);

        assertCache();
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
            assertNotEmpty("No fullName in user", user.getFullName());
            assertNotEmpty("No familyName in user", user.getFamilyName());
            // givenName is not mandatory in LDAP, therefore givenName may not
            // be present on user

            if (user.getName().equals(USER_GUYBRUSH_USERNAME)) {
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
            
            PrismObject<AccountShadowType> aObject = modelService.getObject(AccountShadowType.class, accountRef.getOid(), null, result);
            AccountShadowType account = aObject.asObjectable();
            
            display("Account after import ", account);
            
            String attributeValueL = ResourceObjectShadowUtil.getAttributeStringValue(account, new QName(resourceOpenDj.getNamespace(), "l"));
            assertEquals("Unexcpected value of l", "middle of nowhere", attributeValueL);
        }
        
        // This also includes "idm" user imported from LDAP. Later we need to ignore that one.
        assertEquals("Wrong number of users after import",9,uobjects.getObject().size());
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
        repositoryService.modifyObject(UserType.class, changeAddRoleCaptain, result);


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
        }, 20000);

        // Check task status

        Task task = taskManager.getTask(TASK_USER_RECOMPUTE_OID, result);
        result.computeStatus();
        display("getTask result", result);
        assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);
        assertFalse(task.getTaskIdentifier().isEmpty());

        PrismObject<ObjectType> o = repositoryService.getObject(ObjectType.class, TASK_USER_RECOMPUTE_OID, null, result);
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
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> object = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
        UserType repoUser = object.asObjectable();

        repoResult.computeStatus();
        displayJaxb("User (repository)", repoUser, new QName("user"));

        List<ObjectReferenceType> accountRefs = repoUser.getAccountRef();
        assertEquals(1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);
        accountShadowOidGuybrushOpendj = accountRef.getOid();
        assertFalse(accountShadowOidGuybrushOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

         PrismObject<AccountShadowType> accObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = accObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        boolean hasOthers = false;
        accountGuybrushOpendjEntryUuuid = null;
        List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
                if (accountGuybrushOpendjEntryUuuid != null) {
                    AssertJUnit.fail("Multiple values for ICF UID in shadow attributes");
                } else {
                    accountGuybrushOpendjEntryUuuid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        assertFalse(hasOthers);
        assertNotNull(accountGuybrushOpendjEntryUuuid);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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

        addObjectFromFile(TASK_OPENDJ_RECON_FILENAME, result);


        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task

        waitFor("Waiting for task to finish first run", new Checker() {
            public boolean check() throws ObjectNotFoundException, SchemaException {
                Task task = taskManager.getTask(TASK_OPENDJ_RECON_OID, result);
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
        }, 180000);

        // Check task status

        Task task = taskManager.getTask(TASK_OPENDJ_RECON_OID, result);
        result.computeStatus();
        display("getTask result", result);
        assertSuccess("getTask has failed", result);
        AssertJUnit.assertNotNull(task);
        display("Task after pickup", task);

        PrismObject<ObjectType> o = repositoryService.getObject(ObjectType.class, TASK_OPENDJ_RECON_OID, null, result);
        display("Task after pickup in the repository", o.asObjectable());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNING, task.getExecutionStatus());

        // .. and claimed
        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

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
        taskManager.deleteTask(TASK_OPENDJ_RECON_OID, result);

        // CHECK RESULT: account for user guybrush should be still there and unchanged

        // Check if user object was modified in the repo

        OperationResult repoResult = new OperationResult("getObject");
        PropertyReferenceListType resolve = new PropertyReferenceListType();

        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_GUYBRUSH_OID, resolve, repoResult);
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

        PrismObject<AccountShadowType> aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidGuybrushOpendj,
                resolve, repoResult);
        AccountShadowType repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        boolean hasOthers = false;
        accountGuybrushOpendjEntryUuuid = null;
        List<Object> xmlAttributes = repoShadow.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
                if (accountGuybrushOpendjEntryUuuid != null) {
                    AssertJUnit.fail("Multiple values for ICF UID in shadow attributes (Guybrush)");
                } else {
                    accountGuybrushOpendjEntryUuuid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        assertFalse(hasOthers);
        assertNotNull(accountGuybrushOpendjEntryUuuid);

        // check if account was created in LDAP

        SearchResultEntry entry = openDJController.searchAndAssertByEntryUuid(accountGuybrushOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", "guybrush");
        OpenDJController.assertAttribute(entry, "givenName", "Guybrush");
        OpenDJController.assertAttribute(entry, "sn", "Threepwood");
        OpenDJController.assertAttribute(entry, "cn", "Guybrush Threepwood");
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


        QueryType query = QueryUtil.createNameQuery(ELAINE_NAME);
        ResultList<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, repoResult);
        assertEquals("Wrong number of Elaines", 1, users.size());
        repoUser = users.get(0).asObjectable();

        repoResult.computeStatus();
        displayJaxb("User Elaine (repository)", repoUser, new QName("user"));

        assertNotNull(repoUser.getOid());
        assertEquals(ELAINE_NAME, repoUser.getName());
        assertEquals("Elaine", repoUser.getGivenName());
        assertEquals("Marley", repoUser.getFamilyName());
        assertEquals("Elaine Marley", repoUser.getFullName());

        accountRefs = repoUser.getAccountRef();
        assertEquals("Elaine has wrong number of accounts", 1, accountRefs.size());
        accountRef = accountRefs.get(0);
        String accountShadowOidElaineOpendj = accountRef.getOid();
        assertFalse(accountShadowOidElaineOpendj.isEmpty());

        // Check if shadow was created in the repo

        repoResult = new OperationResult("getObject");

        aObject = repositoryService.getObject(AccountShadowType.class, accountShadowOidElaineOpendj,
                resolve, repoResult);
        repoShadow = aObject.asObjectable();
        repoResult.computeStatus();
        assertSuccess("getObject has failed", repoResult);
        displayJaxb("Shadow (repository)", repoShadow, new QName("shadow"));
        assertNotNull(repoShadow);
        assertEquals(RESOURCE_OPENDJ_OID, repoShadow.getResourceRef().getOid());

        // check attributes in the shadow: should be only identifiers (ICF UID)
        hasOthers = false;
        String accountElainehOpendjEntryUuuid = null;
        xmlAttributes = repoShadow.getAttributes().getAny();
        for (Object element : xmlAttributes) {
            if (ConnectorFactoryIcfImpl.ICFS_UID.equals(JAXBUtil.getElementQName(element))) {
                if (accountElainehOpendjEntryUuuid != null) {
                    AssertJUnit.fail("Multiple values for ICF UID in shadow attributes (Elaine)");
                } else {
                    accountElainehOpendjEntryUuuid = ((Element) element).getTextContent();
                }
            } else {
                hasOthers = true;
            }
        }

        assertFalse("Elaine has unexpected attributes in shadow", hasOthers);
        assertNotNull("Elaine does not have an UID in shadow", accountElainehOpendjEntryUuuid);

        // check if account is still in LDAP

        entry = openDJController.searchAndAssertByEntryUuid(accountElainehOpendjEntryUuuid);

        display("LDAP account", entry);

        OpenDJController.assertAttribute(entry, "uid", ELAINE_NAME);
        OpenDJController.assertAttribute(entry, "givenName", "Elaine");
        OpenDJController.assertAttribute(entry, "sn", "Marley");
        OpenDJController.assertAttribute(entry, "cn", "Elaine Marley");
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
     * @param filename
     * @return
     * @throws FileNotFoundException
     */
    private void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
        LOGGER.trace("importObjectFromFile: {}", filename);
        Task task = taskManager.createTaskInstance();
        FileInputStream stream = new FileInputStream(filename);
        modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);
    }

    private void assertCache() {
        if (RepositoryCache.exists()) {
            AssertJUnit.fail("Cache exists! " + RepositoryCache.dump());
        }
    }

    private void applySyncSettings(AccountSynchronizationSettingsType syncSettings) throws ObjectNotFoundException,
            SchemaException {
        ObjectModificationType objectChange = new ObjectModificationType();
        objectChange.setOid(SystemObjectsType.SYSTEM_CONFIGURATION.value());
        PropertyModificationType propMod = new PropertyModificationType();
        propMod.setModificationType(PropertyModificationTypeType.replace);
        Value value = new Value();
        JAXBElement<AccountSynchronizationSettingsType> syncSettingsElement = new JAXBElement<AccountSynchronizationSettingsType>(
                SchemaConstants.C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS, AccountSynchronizationSettingsType.class,
                syncSettings);
        value.getAny().add(syncSettingsElement);
        propMod.setValue(value);
        objectChange.getPropertyModification().add(propMod);

        OperationResult result = new OperationResult("Aplying sync settings");
        repositoryService.modifyObject(SystemConfigurationType.class, objectChange, result);
        display("Aplying sync settings result", result);
        result.computeStatus();
        assertSuccess("Aplying sync settings failed (result)", result);
    }

    private void assertSyncSettingsAssignmentPolicyEnforcement(AssignmentPolicyEnforcementType assignmentPolicy) throws
            ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("Asserting sync settings");
        PrismObject<SystemConfigurationType> systemConfigurationType = repositoryService.getObject(SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);
        result.computeStatus();
        assertSuccess("Asserting sync settings failed (result)", result);
        AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfigurationType.asObjectable().getGlobalAccountSynchronizationSettings();
        assertNotNull("globalAccountSynchronizationSettings is null", globalAccountSynchronizationSettings);
        AssignmentPolicyEnforcementType assignmentPolicyEnforcement = globalAccountSynchronizationSettings.getAssignmentPolicyEnforcement();
        assertNotNull("assignmentPolicyEnforcement is null", assignmentPolicyEnforcement);
        assertEquals("Assignment policy mismatch", assignmentPolicy, assignmentPolicyEnforcement);
    }

}
