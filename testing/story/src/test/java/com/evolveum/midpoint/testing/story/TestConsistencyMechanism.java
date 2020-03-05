/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoRepoCache;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayJaxb;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.LinksAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.ShadowReferenceAsserter;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointAsserts;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Consistency test suite. It tests consistency mechanisms. It works as end-to-end integration test accross all subsystems.
 *
 * @author Katarina Valalikova
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConsistencyMechanism extends AbstractModelIntegrationTest {

    private static final String TEST_DIR = "src/test/resources/consistency/";

    private static final String SYSTEM_CONFIGURATION_FILENAME = TEST_DIR + "system-configuration.xml";

    private static final String ROLE_SUPERUSER_FILENAME = TEST_DIR + "role-superuser.xml";
    private static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    private static final String ROLE_LDAP_ADMINS_FILENAME = TEST_DIR + "role-admins.xml";
    private static final String ROLE_LDAP_ADMINS_OID = "88888888-8888-8888-8888-000000000009";
    private static final String ROLE_LDAP_ADMINS_DN = "cn=admins,ou=groups,dc=example,dc=com";

    private static final String SAMPLE_CONFIGURATION_OBJECT_FILENAME = TEST_DIR + "sample-configuration-object.xml";
    private static final String SAMPLE_CONFIGURATION_OBJECT_OID = "c0c010c0-d34d-b33f-f00d-999111111111";

    private static final String RESOURCE_OPENDJ_FILENAME = TEST_DIR + "resource-opendj.xml";
    private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
    private static final String RESOURCE_OPENDJ_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
    private static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_OPENDJ_NS, "inetOrgPerson");
    private static final QName RESOURCE_OPENDJ_GROUP_OBJECTCLASS = new QName(RESOURCE_OPENDJ_NS, "groupOfUniqueNames");
    private static final String RESOURCE_OPENDJ_PRIMARY_IDENTIFIER_LOCAL_NAME = "entryUUID";
    private static final String RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME = "dn";
    private static final QName RESOURCE_OPENDJ_SECONDARY_IDENTIFIER = new QName(RESOURCE_OPENDJ_NS, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME);

    private static final String CONNECTOR_LDAP_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-ldap/com.evolveum.polygon.connector.ldap.LdapConnector";

    private static final String USER_TEMPLATE_FILENAME = TEST_DIR + "user-template.xml";

    private static final String USER_ADMINISTRATOR_FILENAME = TEST_DIR + "user-administrator.xml";
    private static final String USER_ADMINISTRATOR_NAME = "administrator";

    private static final String USER_JACK_FILENAME = TEST_DIR + "user-jack.xml";
    private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

    private static final String USER_DENIELS_FILENAME = TEST_DIR + "user-deniels.xml";
    private static final String USER_DENIELS_OID = "c0c010c0-d34d-b33f-f00d-222111111111";

    private static final String USER_JACKIE_FILENAME = TEST_DIR + "user-jackie.xml";
    private static final File USER_JACKIE_FILE = new File(USER_JACKIE_FILENAME);
    private static final String USER_JACKIE_OID = "c0c010c0-d34d-b33f-f00d-111111114444";

    private static final String USER_WILL_FILENAME = TEST_DIR + "user-will.xml";
    private static final String USER_WILL_OID = "c0c010c0-d34d-b33f-f00d-111111115555";

    private static final String USER_GUYBRUSH_FILENAME = TEST_DIR + "user-guybrush.xml";
    private static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111222";

    private static final String USER_GUYBRUSH_NOT_FOUND_FILENAME = TEST_DIR + "user-guybrush-modify-not-found.xml";
    private static final String USER_GUYBRUSH_NOT_FOUND_OID = "c0c010c0-d34d-b33f-f00d-111111111333";

    private static final String USER_HECTOR_NOT_FOUND_FILENAME = TEST_DIR + "user-hector.xml";
    private static final String USER_HECTOR_NOT_FOUND_OID = "c0c010c0-d34d-b33f-f00d-111111222333";

    private static final String USER_E_FILENAME = TEST_DIR + "user-e.xml";
    private static final String USER_E_OID = "c0c010c0-d34d-b33f-f00d-111111111100";

    private static final String USER_ELAINE_FILENAME = TEST_DIR + "user-elaine.xml";
    private static final File USER_ELAINE_FILE = new File(USER_ELAINE_FILENAME);
    private static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-111111116666";

    private static final String USER_HERMAN_FILENAME = TEST_DIR + "user-herman.xml";
    private static final String USER_HERMAN_OID = "c0c010c0-d34d-b33f-f00d-111111119999";

    private static final String USER_MORGAN_FILENAME = TEST_DIR + "user-morgan.xml";
    private static final File USER_MORGAN_FILE = new File(USER_MORGAN_FILENAME);
    private static final String USER_MORGAN_OID = "c0c010c0-d34d-b33f-f00d-171171117777";

    private static final String USER_CHUCK_FILENAME = TEST_DIR + "user-chuck.xml";
    private static final File USER_CHUCK_FILE = new File(USER_CHUCK_FILENAME);
    private static final String USER_CHUCK_OID = "c0c010c0-d34d-b33f-f00d-171171118888";

    private static final String USER_ANGELIKA_FILENAME = TEST_DIR + "user-angelika.xml";
    private static final String USER_ANGELIKA_OID = "c0c010c0-d34d-b33f-f00d-111111111888";

    private static final String USER_ALICE_FILENAME = TEST_DIR + "user-alice.xml";
    private static final String USER_ALICE_OID = "c0c010c0-d34d-b33f-f00d-111111111999";

    private static final String USER_BOB_NO_GIVEN_NAME_FILENAME = TEST_DIR + "user-bob-no-given-name.xml";
    private static final String USER_BOB_NO_GIVEN_NAME_OID = "c0c010c0-d34d-b33f-f00d-222111222999";

    private static final String USER_JOHN_WEAK_FILENAME = TEST_DIR + "user-john.xml";
    private static final String USER_JOHN_WEAK_OID = "c0c010c0-d34d-b33f-f00d-999111111888";

    private static final String USER_DONALD_FILENAME = TEST_DIR + "user-donald.xml";
    private static final String USER_DONALD_OID = "c0c010c0-d34d-b33f-f00d-999111111777";
    private static final String ACCOUNT_DONALD_LDAP_UID = "donald";
    private static final String ACCOUNT_DONALD_LDAP_DN = "uid=" + ACCOUNT_DONALD_LDAP_UID + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final String USER_DISCOVERY_FILENAME = TEST_DIR + "user-discovery.xml";
    private static final String USER_DISCOVERY_OID = "c0c010c0-d34d-b33f-f00d-111112226666";

    private static final String USER_ABOMBA_FILENAME = TEST_DIR + "user-abomba.xml";
    private static final String USER_ABOMBA_OID = "c0c010c0-d34d-b33f-f00d-016016111111";

    private static final String USER_ABOM_FILENAME = TEST_DIR + "user-abom.xml";
    private static final String USER_ABOM_OID = "c0c010c0-d34d-b33f-f00d-111111016016";

    private static final File ACCOUNT_GUYBRUSH_FILE = new File(TEST_DIR, "account-guybrush.xml");
    private static final String ACCOUNT_GUYBRUSH_OID = "a0c010c0-d34d-b33f-f00d-111111111222";

    private static final File ACCOUNT_HECTOR_FILE = new File(TEST_DIR, "account-hector-not-found.xml");
    private static final String ACCOUNT_HECTOR_OID = "a0c010c0-d34d-b33f-f00d-111111222333";

    private static final File ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILE = new File(TEST_DIR, "account-guybrush-not-found.xml");
    private static final String ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID = "a0c010c0-d34d-b33f-f00d-111111111333";

    private static final String ACCOUNT_DENIELS_FILENAME = TEST_DIR + "account-deniels.xml";
    private static final File ACCOUNT_DENIELS_FILE = new File(ACCOUNT_DENIELS_FILENAME);
    private static final String ACCOUNT_DENIELS_OID = "a0c010c0-d34d-b33f-f00d-111111111555";
    private static final String ACCOUNT_DENIELS_LDAP_UID = "deniels";
    private static final String ACCOUNT_DENIELS_LDAP_DN = "uid=" + ACCOUNT_DENIELS_LDAP_UID + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final String ACCOUNT_CHUCK_FILENAME = TEST_DIR + "account-chuck.xml";

    private static final String ACCOUNT_HERMAN_FILENAME = TEST_DIR + "account-herman.xml";
    private static final String ACCOUNT_HERMAN_OID = "22220000-2200-0000-0000-333300003333";

    private static final File ACCOUNT_JACKIE_FILE = new File(TEST_DIR, "account-jack.xml");
    private static final String ACCOUNT_JACKIE_OID = "22220000-2222-5555-0000-333300003333";
    private static final String ACCOUNT_JACKIE_LDAP_UID = "jackie";
    private static final String ACCOUNT_JACKIE_LDAP_DN = "uid=" + ACCOUNT_JACKIE_LDAP_UID + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final File TASK_OPENDJ_RECONCILIATION_FILE = new File(TEST_DIR, "task-opendj-reconciliation.xml");
    private static final String TASK_OPENDJ_RECONCILIATION_OID = "91919191-76e0-59e2-86d6-3d4f02d30000";

    private static final File LDIF_WILL_FILE = new File(TEST_DIR, "will.ldif");
    private static final File LDIF_ELAINE_FILE = new File(TEST_DIR, "elaine.ldif");
    private static final File LDIF_MORGAN_FILE = new File(TEST_DIR, "morgan.ldif");
    private static final File LDIF_DISCOVERY_FILE = new File(TEST_DIR, "discovery.ldif");

    private static final File LDIF_CREATE_USERS_OU_FILE = new File(TEST_DIR, "usersOu.ldif");
    private static final File LDIF_CREATE_ADMINS_GROUP_FILE = new File(TEST_DIR, "adminsGroup.ldif");

    private static final File LDIF_MODIFY_RENAME_FILE = new File(TEST_DIR, "modify-rename.ldif");

    private static final String NS_MY = "http://whatever.com/my";
    private static final QName MY_SHIP_STATE = new QName(NS_MY, "shipState");

    private static final QName LDAP_ATTRIBUTE_DN = new QName(MidPointConstants.NS_RI, "dn");
    private static final QName LDAP_ATTRIBUTE_UID = new QName(MidPointConstants.NS_RI, "uid");
    private static final QName LDAP_ATTRIBUTE_GIVENNAME = new QName(MidPointConstants.NS_RI, "givenName");
    private static final QName LDAP_ATTRIBUTE_SN = new QName(MidPointConstants.NS_RI, "sn");
    private static final QName LDAP_ATTRIBUTE_CN = new QName(MidPointConstants.NS_RI, "cn");
    private static final QName LDAP_ATTRIBUTE_CAR_LICENCE = new QName(MidPointConstants.NS_RI, "carLicense");
    private static final QName LDAP_ATTRIBUTE_EMPLOYEE_NUMBER = new ItemName(MidPointConstants.NS_RI, "employeeNumber");
    private static final QName LDAP_ATTRIBUTE_EMPLOYEE_TYPE = new ItemName(MidPointConstants.NS_RI, "employeeType");

    private static ResourceType resourceTypeOpenDjrepo;
    private static String accountShadowOidOpendj;
    private String aliceAccountDn;

    // This will get called from the superclass to init the repository
    // It will be called only once
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_SUPERUSER_FILENAME, initResult);
        repoAddObjectFromFile(ROLE_LDAP_ADMINS_FILENAME, initResult);
        repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, initResult);

        // This should discover the connectors
        logger.trace("initSystem: trying modelService.postInit()");
        modelService.postInit(initResult);
        logger.trace("initSystem: modelService.postInit() done");

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

//        DebugUtil.setDetailedDebugDump(true);
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
    public void stopResources() {
        openDJController.stop();
    }

    /**
     * Test integrity of the test setup.
     */
    @Test
    public void test000Integrity() throws Exception {
        assertNotNull(modelWeb);
        assertNotNull(modelService);
        assertNotNull(repositoryService);
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

        OperationResult result = createOperationResult();

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

        Task task = taskManager.createTaskInstance();
        // GIVEN

        assertNoRepoCache();

        // WHEN
        OperationResultType result = modelWeb.testResource(RESOURCE_OPENDJ_OID);

        // THEN

        assertNoRepoCache();

        displayJaxb("testResource result:", result, SchemaConstants.C_RESULT);

        TestUtil.assertSuccess("testResource has failed", result);

        OperationResult opResult = new OperationResult(TestConsistencyMechanism.class.getName()
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

        // WHEN
        when();
        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);

        // THEN
        then();
        assertSuccess(repoResult);

        assertUser(uObject, "repo")
                .assertOid(USER_JACK_OID)
                .assertFullName(userType.getFullName().getOrig());
    }

    /**
     * Add account to user. This should result in account provisioning. Check if
     * that happens in repo and in LDAP.
     */
    @Test
    public void test110PrepareOpenDjWithJackieAccounts() throws Exception {
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        // adding jackie shadow directly and then, linking this shadow to the user jack. we need to do linking on repository level, to skip clockwork execution
        PrismObject<ShadowType> jackieAccount = addObject(ACCOUNT_JACKIE_FILE, task, parentResult);
        String oid = jackieAccount.getOid();

        PrismObject<ShadowType> jackFromRepo = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        display("account jack after provisioning", jackFromRepo);

        ReferenceDelta linkRefDelta = prismContext.deltaFactory().reference().createModificationAdd(UserType.class, UserType.F_LINK_REF, ObjectTypeUtil.createObjectRef(jackieAccount, SchemaConstants.ORG_DEFAULT).asReferenceValue());
        repositoryService.modifyObject(UserType.class, USER_JACK_OID, Arrays.asList(linkRefDelta), parentResult);

        OperationResult repoResult = new OperationResult("getObject");

        // Check if user object was modified in the repo
        assertUserOneAccountRef(USER_JACK_OID);

        // check LDAP
        Entry entry = openDJController.searchByUid(ACCOUNT_JACKIE_LDAP_UID);
        assertNotNull("Entry uid " + ACCOUNT_JACKIE_LDAP_UID + " not found", entry);

        display("LDAP account", entry);
        OpenDJController.assertAttribute(entry, "uid", "jackie");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        OpenDJController.assertAttribute(entry, "cn", "Jack Sparrow");

        assertNoRepoCache();

        // check full (model) shadow
        PrismObject<ShadowType> modelShadow = getShadowModel(ACCOUNT_JACKIE_OID);
        display("Shadow (model)", modelShadow);

        ShadowAsserter.forShadow(modelShadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("internal")
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertValue(LDAP_ATTRIBUTE_UID, ACCOUNT_JACKIE_LDAP_UID)
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "Jack")
                .assertValue(LDAP_ATTRIBUTE_CN, "Jack Sparrow")
                .assertValue(LDAP_ATTRIBUTE_SN, "Sparrow")
                .assertNoAttribute(LDAP_ATTRIBUTE_CAR_LICENCE)
                .end()
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

    }

    @Test
    public void test111prepareOpenDjWithDenielsAccounts() throws Exception {
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        addObject(ACCOUNT_DENIELS_FILE, task, parentResult);

        // check LDAP
        Entry entry = openDJController.searchByUid(ACCOUNT_DENIELS_LDAP_UID);
        assertNotNull("Entry uid " + ACCOUNT_DENIELS_LDAP_UID + " not found", entry);

        display("LDAP account", entry);
        OpenDJController.assertAttribute(entry, "uid", "deniels");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Deniels");
        OpenDJController.assertAttribute(entry, "cn", "Jack Deniels");

        assertNoRepoCache();

        // check full (model) shadow
        PrismObject<ShadowType> modelShadow = getShadowModel(ACCOUNT_DENIELS_OID);
        display("Shadow (model)", modelShadow);

        ShadowAsserter.forShadow(modelShadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("internal")
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertValue(LDAP_ATTRIBUTE_UID, ACCOUNT_DENIELS_LDAP_UID)
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "Jack")
                .assertValue(LDAP_ATTRIBUTE_CN, "Jack Deniels")
                .assertValue(LDAP_ATTRIBUTE_SN, "Deniels")
                .assertNoAttribute(LDAP_ATTRIBUTE_CAR_LICENCE)
                .end()
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        repoAddObjectFromFile(USER_DENIELS_FILENAME, parentResult);
        //TODO some asserts?

    }

    @Test
    public void test120AddAccountAlreadyExistLinked() throws Exception {
        Task task = taskManager.createTaskInstance();

        OperationResult parentResult = new OperationResult("Add account already exist linked");

        // GIVEN
        //adding user jakie. we already have user jack with the account identifier jackie.
        assertNoRepoCache();
        addObject(USER_JACKIE_FILE, task, parentResult);
        PrismObject<UserType> userJackieBefore = getUser(USER_JACKIE_OID);
        UserAsserter.forUser(userJackieBefore).assertLinks(0);

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        UserAsserter.forUser(userJackBefore)
                .assertLinks(1)
                .links()
                .link(ACCOUNT_JACKIE_OID);
//        //check if the jackie account already exists on the resource

        PrismObject<ShadowType> existingJackieAccount = getShadowRepo(ACCOUNT_JACKIE_OID);
        display("Jack's account: ", existingJackieAccount);

        // WHEN
        assignAccountToUser(USER_JACKIE_OID, RESOURCE_OPENDJ_OID, "internal");

        // THEN
        //expected thet the dn and ri:uid will be jackie1 because jackie already exists and is liked to another user..
        PrismObject<UserType> userJackieAfter = getUser(USER_JACKIE_OID);
        UserAsserter.forUser(userJackieAfter)
                .assertLinks(1);

        ObjectReferenceType linkRef = userJackieAfter.asObjectable().getLinkRef().iterator().next();
        assertFalse("Wrong account linked", ACCOUNT_JACKIE_OID.equals(linkRef.getOid()));

        PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
        ShadowAsserter.forShadow(shadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent("internal")
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .assertValue(LDAP_ATTRIBUTE_UID, "jackie1")
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "Jack")
                .assertValue(LDAP_ATTRIBUTE_CN, "Jack Russel")
                .assertValue(LDAP_ATTRIBUTE_SN, "Russel")
                .assertNoAttribute(LDAP_ATTRIBUTE_CAR_LICENCE)
                .end()
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

//        checkAccount(accountOid, "jackie1", "Jack", "Russel", "Jack Russel", task, parentResult);
    }

    @Test
    public void test122AddAccountAlreadyExistUnlinked() throws Exception {
        // GIVEN
        OperationResult parentResult = new OperationResult("Add account already exist unlinked.");
        Entry entry = openDJController.addEntryFromLdifFile(LDIF_WILL_FILE);
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

        testAddUserToRepo("test122AddAccountAlreadyExistUnlinked", USER_WILL_FILENAME,
                USER_WILL_OID);
        assertUserNoAccountRef(USER_WILL_OID, parentResult);

        Task task = taskManager.createTaskInstance();

        //WHEN
        when();
        assignAccount(UserType.class, USER_WILL_OID, RESOURCE_OPENDJ_OID, null, task, parentResult);

        // THEN
        then();
        String accountOid = checkUser(USER_WILL_OID, task, parentResult);
//        MidPointAsserts.assertAssignments(user, 1);

        PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class,
                accountOid, null, task, parentResult);

        ResourceAttributeContainer attributes = ShadowUtil.getAttributesContainer(account);

        assertEquals("shadow secondary identifier not equal with the account dn. ", dn, attributes
                .findAttribute(getOpenDjSecondaryIdentifierQName()).getRealValue(String.class));

        String identifier = attributes.getPrimaryIdentifier().getRealValue(String.class);

        openDJController.searchAndAssertByEntryUuid(identifier);

    }

    //MID-1595, MID-1577
    @Test(enabled = false)
    public void test124AddAccountDirectAlreadyExists() throws Exception {
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        try {

            repoAddObjectFromFile(USER_ABOMBA_FILENAME, parentResult);

            ObjectDelta<UserType> abombaDelta = createModifyUserAddAccount(USER_ABOMBA_OID, resourceTypeOpenDjrepo.asPrismObject(), "contractor");
            executeChanges(abombaDelta, null, task, parentResult);

            assertUser(USER_ABOMBA_OID, "User before")
                    .assertLinks(1);

            String abombaOid = getLinkRefOid(USER_ABOMBA_OID, RESOURCE_OPENDJ_OID);

            ShadowType abombaShadow = repositoryService.getObject(ShadowType.class, abombaOid, null, parentResult).asObjectable();
            assertShadowName(abombaShadow, "uid=abomba,OU=people,DC=example,DC=com");

            repoAddObjectFromFile(USER_ABOM_FILENAME, parentResult);
            ObjectDelta<UserType> abomDelta = createModifyUserAddAccount(USER_ABOM_OID, resourceTypeOpenDjrepo.asPrismObject(), "contractor");
            executeChanges(abomDelta, null, task, parentResult);

            assertUser(USER_ABOM_OID, "User before")
                    .assertLinks(1);

            String abomOid = getLinkRefOid(USER_ABOMBA_OID, RESOURCE_OPENDJ_OID);

            ShadowType abomShadow = repositoryService.getObject(ShadowType.class, abomOid, null, parentResult).asObjectable();
            assertShadowName(abomShadow, "uid=abomba1,OU=people,DC=example,DC=com");

            ReferenceDelta abombaDeleteAccDelta = prismContext.deltaFactory().reference()
                    .createModificationDelete(ShadowType.class,
                            UserType.F_LINK_REF,
                            getPrismContext().itemFactory().createReferenceValue(abombaOid));
            ObjectDelta d = prismContext.deltaFactory().object().createModifyDelta(USER_ABOMBA_OID,
                    abombaDeleteAccDelta, UserType.class);
            modelService.executeChanges(MiscSchemaUtil.createCollection(d), null, task,
                    parentResult);

            assertUserNoAccountRef(USER_ABOMBA_OID, parentResult);

            repositoryService.getObject(ShadowType.class, abombaOid, null,
                    parentResult);

            ReferenceDelta abomDeleteAccDelta = prismContext.deltaFactory().reference()
                    .createModificationDelete(ShadowType.class,
                            UserType.F_LINK_REF,
                            abomShadow.asPrismObject());
            ObjectDelta d2 = prismContext.deltaFactory().object().createModifyDelta(USER_ABOM_OID,
                    abomDeleteAccDelta, UserType.class);
            modelService.executeChanges(MiscSchemaUtil.createCollection(d2), null, task,
                    parentResult);

            assertUserNoAccountRef(USER_ABOM_OID, parentResult);
            try {
                repositoryService.getObject(ShadowType.class, abomOid, null,
                        parentResult);
                fail("Expected that shadow abom does not exist, but it is");
            } catch (ObjectNotFoundException ex) {
                // this is expected
            }

            logger.info("starting second execution request for user abomba");
            OperationResult result = new OperationResult("Add account already exist result.");
            ObjectDelta<UserType> abombaDelta2 = createModifyUserAddAccount(USER_ABOMBA_OID, resourceTypeOpenDjrepo.asPrismObject(), "contractor");
            executeChanges(abombaDelta2, null, task, parentResult);

            String abombaOid2 = assertUserOneAccountRef(USER_ABOMBA_OID);
            ShadowType abombaShadow2 = repositoryService.getObject(ShadowType.class, abombaOid2, null, result).asObjectable();
            assertShadowName(abombaShadow2,
                    "uid=abomba,OU=people,DC=example,DC=com");

            result.computeStatus();

            logger.info("Displaying execute changes result");
            display(result);

        } catch (Exception ex) {
            logger.info("error: " + ex.getMessage(), ex);
            throw ex;
        }
    }

    @Test
    public void test130DeleteObjectNotFound() throws Exception {
        OperationResult parentResult = createOperationResult();

        repoAddShadowFromFile(ACCOUNT_GUYBRUSH_FILE, parentResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, parentResult);

        Task task = taskManager.createTaskInstance();
//        requestToExecuteChanges(REQUEST_USER_MODIFY_DELETE_ACCOUNT_FILE, USER_GUYBRUSH_OID, UserType.class, task, null, parentResult);

        // WHEN
        ObjectDelta deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(ShadowType.class, ACCOUNT_GUYBRUSH_OID);

        executeChanges(deleteDelta, null, task, parentResult);
        parentResult.computeStatus();

        PrismObject<ShadowType> shadowRepo = getShadowRepo(ACCOUNT_GUYBRUSH_OID);
        ShadowAsserter.forShadow(shadowRepo)
                .assertTombstone()
                .assertDead()
                .assertIsNotExists();

        clockForward("PT20M");

        then();
        provisioningService.refreshShadow(shadowRepo, null, task, parentResult);

        try {
            repositoryService.getObject(ShadowType.class, ACCOUNT_GUYBRUSH_OID, null, parentResult);
            fail("Unexpected object found");
        } catch (Exception ex) {
            if (!(ex instanceof ObjectNotFoundException)) {
                fail("Expected ObjectNotFoundException but got " + ex);
            }
        }

        clock.resetOverride();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        UserAsserter.forUser(userAfter)
                .assertLinks(0);

        repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, parentResult);
    }

    /**
     * Modify account not found => reaction: Delete account
     * <p>
     * no assignemnt - only linkRef to non existent account
     */
    @SuppressWarnings("unchecked")
    @Test
    public void test140ModifyObjectNotFoundLinkedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repoAddShadowFromFile(ACCOUNT_GUYBRUSH_FILE, result);
        repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, result);

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        UserAsserter.forUser(userBefore)
                .assertLinks(1)
                .links()
                .link(ACCOUNT_GUYBRUSH_OID);

        // WHEN
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .property(ItemPath.create(ShadowType.F_ATTRIBUTES, new ItemName(MidPointConstants.NS_RI, "roomNumber")))
                .replace("cabin")
                .asObjectDelta(ACCOUNT_GUYBRUSH_OID);

        try {
            executeChanges(delta, null, task, result);
            fail("Unexpected succes while modifying non-exsiting object");
        } catch (ObjectNotFoundException e) {
            //this is expected
        } catch (Throwable e) {
            fail("Unexpected exception furing modifying object, " + e.getMessage());
        }
//        requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT_FILE, ACCOUNT_GUYBRUSH_OID, ShadowType.class, task, null, result);

        // THEN
        then();
        PrismObject<ShadowType> shadowAfter = getShadowRepo(ACCOUNT_GUYBRUSH_OID);
        ShadowAsserter.forShadow(shadowAfter)
                .assertTombstone()
                .assertDead()
                .assertIsNotExists();

        clockForward("PT20M");
        provisioningService.refreshShadow(shadowAfter, null, task, result);

        try {
            repositoryService.getObject(ShadowType.class, ACCOUNT_GUYBRUSH_OID, null, result);
            fail("Expected ObjectNotFound but did not get one.");
        } catch (Exception ex) {
            if (!(ex instanceof ObjectNotFoundException)) {
                fail("Expected ObjectNotFoundException but got " + ex);
            }
        }

        clock.resetOverride();
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        UserAsserter.forUser(userAfter)
                .assertLinks(0);

//        repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, result);
    }

    /**
     * Modify account not found => reaction: Re-create account, apply changes.
     * <p>
     * assignemt with non-existent account
     */
    @Test
    public void test142ModifyObjectNotFoundAssignedAccount() throws Exception {
        // GIVEN
        OperationResult parentResult = createOperationResult();

        repoAddShadowFromFile(ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILE, parentResult);
        repoAddObjectFromFile(USER_GUYBRUSH_NOT_FOUND_FILENAME, parentResult);

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_NOT_FOUND_OID);
        UserAsserter.forUser(userBefore)
                .assertLinks(1)
                .links()
                .link(ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID);

        Task task = taskManager.createTaskInstance();

        //WHEN
        when();
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .property(ItemPath.create(ShadowType.F_ATTRIBUTES, new ItemName(MidPointConstants.NS_RI, "roomNumber")))
                .replace("cabin")
                .property(ItemPath.create(ShadowType.F_ATTRIBUTES, new ItemName(MidPointConstants.NS_RI, "businessCategory")))
                .add("capsize", "fighting")
                .asObjectDelta(ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID);

        try {
            executeChanges(delta, null, task, parentResult);
            fail("Unexpected existence of account");
        } catch (ObjectNotFoundException e) {
            //expected, because we requested the direct modification on non-existing account
            // if the modification is indirect (let's say on focus, the error should not occur)
        }
//        requestToExecuteChanges(REQUEST_ACCOUNT_MODIFY_NOT_FOUND_DELETE_ACCOUNT_FILE, ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID, ShadowType.class, task, null, parentResult);

        // THEN
        then();
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_NOT_FOUND_OID);
        UserAsserter.forUser(userAfter)
                .assertLinks(1);

        String newShadowOid = getLinkRefOid(USER_GUYBRUSH_NOT_FOUND_OID, RESOURCE_OPENDJ_OID);

        assertFalse("Unexpected that new and old shadows have the same oid", ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID.equals(newShadowOid));

        PrismObject<ShadowType> accountAfter = getShadowModel(newShadowOid);
        assertNotNull(accountAfter);
        display("Modified shadow", accountAfter);

//        ShadowAsserter.forShadow(accountAfter)
//                .assertName("uid=guybrush123,ou=people,dc=example,dc=com")
//                .attributes()
//                    .assertValue(new ItemName(MidPointConstants.NS_RI, "roomNumber"),"cabin")
//                    .assertValue(new ItemName(MidPointConstants.NS_RI, "businessCategory"), "capsize", "fighting")
//                .end();

    }

    /**
     * Get account not found => reaction: Re-create account, return re-created.
     */
    @Test
    public void test144GetObjectNotFoundAssignedAccount() throws Exception {
        // GIVEN
        OperationResult parentResult = createOperationResult();

        repoAddShadowFromFile(ACCOUNT_HECTOR_FILE, parentResult);
        repoAddObjectFromFile(USER_HECTOR_NOT_FOUND_FILENAME, parentResult);

        PrismObject<UserType> userBefore = getUser(USER_HECTOR_NOT_FOUND_OID);
        UserAsserter.forUser(userBefore)
                .assertLinks(1)
                .links()
                .link(ACCOUNT_HECTOR_OID);

        Task task = taskManager.createTaskInstance();

        //WHEN
        PrismObject<UserType> modificatedUser = modelService.getObject(UserType.class, USER_HECTOR_NOT_FOUND_OID, null, task, parentResult);

        // THEN
        PrismObject<UserType> userAfter = getUser(USER_HECTOR_NOT_FOUND_OID);
        UserAsserter.forUser(userAfter)
                .assertLinks(1)
                .links()
                .link(ACCOUNT_HECTOR_OID);

        PrismObject<ShadowType> modifiedAccount = getShadowModel(ACCOUNT_HECTOR_OID);
        assertNotNull(modifiedAccount);

        ShadowAsserter.forShadow(modifiedAccount)
                .assertName("uid=hector,ou=people,dc=example,dc=com");

    }

    /**
     * Recompute user => account not found => reaction: Re-create account
     * MID-3093
     */
    @Test
    public void test150RecomputeUserAccountNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_OPENDJ_OID, null);

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        UserAsserter.forUser(userBefore)
                .assertLinks(1);

        ObjectReferenceType linkRef = userBefore.asObjectable().getLinkRef().iterator().next();
        PrismObject<ShadowType> shadowBefore = getShadowModel(linkRef.getOid());
        display("Model Shadow before", shadowBefore);

        String dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        openDJController.delete(dn);

        PrismObject<ShadowType> repoShadowBefore = getShadowRepo(linkRef.getOid());
        assertNotNull("Repo shadow is gone!", repoShadowBefore);
        display("Repository shadow before", repoShadowBefore);
        assertTrue("Oh my! Shadow is dead!", repoShadowBefore.asObjectable().isDead() != Boolean.TRUE);

        // WHEN
        when();
        recomputeUser(USER_GUYBRUSH_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        UserAsserter.forUser(userAfter)
                .assertLinks(1);

        ObjectReferenceType linkRefAfter = userAfter.asObjectable().getLinkRef().iterator().next();
        assertFalse("Old and new shadow with the same oid?", linkRef.getOid().equals(linkRefAfter.getOid()));

        PrismObject<ShadowType> shadowAfter = getShadowModel(linkRefAfter.getOid());
        display("Shadow after", shadowAfter);
        assertNotNull("No shadow found", shadowAfter);

        Entry entryAfter = openDJController.fetchEntry(dn);
        display("Entry after", entryAfter);
    }

    /**
     * Recompute user => account not found => reaction: Re-create account
     * MID-3093
     */
    @Test
    public void test152RecomputeUserAccountAndShadowNotFound() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
        UserAsserter.forUser(userBefore)
                .assertLinks(1);

        ObjectReferenceType linkRef = userBefore.asObjectable().getLinkRef().iterator().next();
        String shadowOidBefore = linkRef.getOid();
        PrismObject<ShadowType> shadowBefore = getShadowModel(shadowOidBefore);
        display("Shadow before", shadowBefore);

        String dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        openDJController.delete(dn);

        repositoryService.deleteObject(ShadowType.class, shadowOidBefore, result);

        // WHEN
        recomputeUser(USER_GUYBRUSH_OID, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        UserAsserter.forUser(userAfter)
                .assertLinks(1);

        ObjectReferenceType linkRefAfter = userAfter.asObjectable().getLinkRef().iterator().next();
        String shadowOidAfter = linkRefAfter.getOid();
        assertFalse("Old and new shadow with the same oid?", shadowOidBefore.equals(shadowOidAfter));

        PrismObject<ShadowType> shadowAfter = getShadowModel(shadowOidAfter);
        display("Shadow after", shadowAfter);
        assertNotNull("No shadow found", shadowAfter);

        Entry entryAfter = openDJController.fetchEntry(dn);
        display("Entry after", entryAfter);
        assertNotNull("No LDAP entry for " + dn + " found", entryAfter);
    }

    @Test
    public void test159DeleteUSerGuybrush() throws Exception {
        // GIVEN
        Task task = getTestTask();
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
        openDJController.stop();

        assertEquals("Resource is running", false, EmbeddedUtils.isRunning());
    }

    @Test
    public void test210AddObjectCommunicationProblem() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        repoAddObjectFromFile(USER_E_FILENAME, parentResult);

        assertUser(USER_E_OID, getTestNameShort()).assertLinks(0);

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        assignAccount(UserType.class, USER_E_OID, RESOURCE_OPENDJ_OID, "internal", task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        parentResult.computeStatus();
        display("add object communication problem result: ", parentResult);
        assertEquals("Expected handled error but got: " + parentResult.getStatus(), OperationResultStatus.IN_PROGRESS, parentResult.getStatus());

        assertUser(USER_E_OID, "after").assertLinks(1);

        String shadowEOid = getLinkRefOid(USER_E_OID, RESOURCE_OPENDJ_OID);

        assertRepoShadow(shadowEOid)
                .display(getTestNameShort() + " Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoPrimaryIdentifierValue()
                .assertNoLegacyConsistency()
                .attributes()
                .assertAttributes(LDAP_ATTRIBUTE_DN, LDAP_ATTRIBUTE_UID)
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertAdd();

//        checkPostponedAccountWithAttributes(accountOid, "e", "e", "e", "e", FailedOperationTypeType.ADD, false, task, parentResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test212AddModifyObjectCommunicationProblem() throws Exception {
        final String TEST_NAME = "test212AddModifyObjectCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult parentResult = task.getResult();

        assertUser(USER_E_OID, "User before").assertLinks(1);

        String accountOid = getLinkRefOid(USER_E_OID, RESOURCE_OPENDJ_OID);

        //WHEN
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory()
                .object()
                .createModificationAddProperty(ShadowType.class, accountOid, createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER), "emp4321");
        delta.addModificationReplaceProperty(createAttributePath(LDAP_ATTRIBUTE_GIVENNAME), "eeeee");

        when();
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(delta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        //THEN
        then();
        assertRepoShadow(accountOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoPrimaryIdentifierValue()
                .assertNoLegacyConsistency()
                .attributes()
                .assertAttributes(LDAP_ATTRIBUTE_DN, LDAP_ATTRIBUTE_UID)
                .end()
                .pendingOperations().assertOperations(2)
                .addOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertAttemptNumber(2)
                .delta()
                .display()
                .end()
                .end()
                .modifyOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .end()
                .end()
                .end()
                .display();

//        checkPostponedAccountWithAttributes(accountOid, "e", "Jackkk", "e", "e", "emp4321", FailedOperationTypeType.ADD, false, task, parentResult);
    }

    private ItemPath createAttributePath(QName itemName) {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, itemName);
    }

    @Test
    public void test214ModifyObjectCommunicationProblem() throws Exception {
        final String TEST_NAME = "test214ModifyObjectCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        String accountOid = assertUserOneAccountRef(USER_JACK_OID);

        Task task = taskManager.createTaskInstance();

        //WHEN
        when();
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory()
                .object()
                .createModificationAddProperty(ShadowType.class, accountOid, createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER), "emp4321");
        delta.addModificationReplaceProperty(createAttributePath(LDAP_ATTRIBUTE_GIVENNAME), "Jackkk");

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();

        executeChanges(delta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        //THEN
        then();
        assertModelShadowNoFetch(ACCOUNT_JACKIE_OID)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertModify();

        assertModelShadowFutureNoFetch(ACCOUNT_JACKIE_OID)
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "Jackkk")
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER, "emp4321")
                .end()
                .assertIsExists()
                .end();
    }

    @Test
    public void test220DeleteObjectCommunicationProblem() throws Exception {
        final String TEST_NAME = "test220DeleteObjectCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult parentResult = task.getResult();

        String accountOid = assertUserOneAccountRef(USER_DENIELS_OID);

        //WHEN
        ObjectDelta deleteDelta = prismContext.deltaFactory().object().createDeleteDelta(ShadowType.class, ACCOUNT_DENIELS_OID);

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(deleteDelta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        assertUser(USER_DENIELS_OID, TEST_NAME)
                .assertLinks(1);

        // THEN
        assertModelShadowNoFetch(ACCOUNT_DENIELS_OID)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertDelete();

//        clockForward("PT20M");
//        assertNoModelShadowFuture(ACCOUNT_DENIELS_OID);
//        clock.resetOverride();
    }

    @Test
    public void test230GetAccountCommunicationProblem() throws Exception {
        final String TEST_NAME = "test230GetAccountCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        OperationResult result = createOperationResult();

        ShadowType account = modelService.getObject(ShadowType.class, ACCOUNT_DENIELS_OID,
                null, null, result).asObjectable();
        assertNotNull("Get method returned null account.", account);
        assertNotNull("Fetch result was not set in the shadow.", account.getFetchResult());

        assertModelShadowNoFetch(ACCOUNT_DENIELS_OID)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .delta()
                .display()
                .assertDelete();

//        clockForward("PT20M");
//        assertNoModelShadowFuture(ACCOUNT_DENIELS_OID);
//        clock.resetOverride();
    }

    @Test
    public void test240AddObjectCommunicationProblemAlreadyExists() throws Exception {
        final String TEST_NAME = "test240AddObjectCommunicationProblemAlreadyExists";

        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_ELAINE_FILE);
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

        repoAddObjectFromFile(USER_ELAINE_FILE, parentResult);

        assertUser(USER_ELAINE_OID, "User before")
                .assertLinks(0);

        Task task = taskManager.createTaskInstance();

        //WHEN assign account on OpenDJ
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        assignAccount(UserType.class, USER_ELAINE_OID, RESOURCE_OPENDJ_OID, "internal", task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        parentResult.computeStatus();
        assertInProgress(parentResult);

        //THEN
        assertUser(USER_ELAINE_OID, "User after")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_ELAINE_OID, RESOURCE_OPENDJ_OID);

        assertRepoShadow(shadowOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoPrimaryIdentifierValue()
                .assertNoLegacyConsistency()
                .attributes()
                .assertAttributes(LDAP_ATTRIBUTE_DN, LDAP_ATTRIBUTE_UID)
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertAdd();

        //check futurized
//        checkPostponedAccountWithAttributes(shadowOid, "elaine", "Elaine", "Marley", "Elaine Marley", FailedOperationTypeType.ADD, false, task, parentResult);
    }

    @Test
    public void test250ModifyFocusCommunicationProblem() throws Exception {
        final String TEST_NAME = "test250ModifyFocusCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        assertUser(USER_JACK_OID, "User before")
                .assertLinks(1);

        Collection<PropertyDelta> modifications = new ArrayList<>();

        PropertyDelta fullNameDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_FULL_NAME, getUserDefinition(), new PolyString("jackNew2"));
        modifications.add(fullNameDelta);

        PropertyDelta<ActivationStatusType> enabledDelta = prismContext.deltaFactory()
                .property()
                .createModificationAddProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, getUserDefinition(), ActivationStatusType.ENABLED);
        modifications.add(enabledDelta);

        ObjectDelta objectDelta = prismContext.deltaFactory()
                .object()
                .createModifyDelta(USER_JACKIE_OID, modifications, UserType.class);
        Task task = taskManager.createTaskInstance();

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(objectDelta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        parentResult.computeStatus();
        assertInProgress(parentResult);

        assertUser(USER_JACKIE_OID, "User after first modify")
                .assertLinks(1)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertFullName("jackNew2");

        String shadowOid = getLinkRefOid(USER_JACKIE_OID, RESOURCE_OPENDJ_OID);

        assertModelShadowNoFetch(shadowOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertModify();

        assertModelShadowFutureNoFetch(shadowOid)
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_CN, "jackNew2")
                .end()
                .assertIsExists()
                .end();

    }

    @Test
    public void test251ModifyFocusCommunicationProblemSecondTime() throws Exception {
        final String TEST_NAME = "test251ModifyFocusCommunicationProblemSecondTime";

        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        assertUser(USER_JACKIE_OID, "User before")
                .assertLinks(1)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertFullName("jackNew2");

        Collection<PropertyDelta> modifications = new ArrayList<>();

        PropertyDelta fullNameDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_FULL_NAME, getUserDefinition(), new PolyString("jackNew2a"));
        modifications.add(fullNameDelta);

        PropertyDelta givenNameDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_GIVEN_NAME, getUserDefinition(), new PolyString("jackNew2a"));
        modifications.add(givenNameDelta);

        PropertyDelta<ActivationStatusType> enabledDelta = prismContext.deltaFactory()
                .property()
                .createModificationAddProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, getUserDefinition(), ActivationStatusType.ENABLED);
        modifications.add(enabledDelta);

        ObjectDelta objectDelta = prismContext.deltaFactory()
                .object()
                .createModifyDelta(USER_JACKIE_OID, modifications, UserType.class);
        Task task = taskManager.createTaskInstance();

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(objectDelta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        parentResult.computeStatus();
        assertInProgress(parentResult);

        assertUser(USER_JACKIE_OID, "User after first modify")
                .assertLinks(1)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertFullName("jackNew2a")
                .assertGivenName("jackNew2a");

        String shadowOid = getLinkRefOid(USER_JACKIE_OID, RESOURCE_OPENDJ_OID);

        assertModelShadowNoFetch(shadowOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .assertOperations(2)
                .by()
                .changeType(ChangeTypeType.MODIFY)
                .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertAll();
//                    .modifyOperation()
//                        .display()
//                        .assertType(PendingOperationTypeType.RETRY)
//                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
//                        .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
//                        .assertAttemptNumber(1)
//                        .delta()
//                            .display()
//                            .assertModify()
//                            .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_CN))
//                        .end()
//                    .end()
//                    .modifyOperation()
//                        .display()
//                        .assertType(PendingOperationTypeType.RETRY)
//                        .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
//                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
//                        .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
//                        .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
//                        .assertAttemptNumber(1)
//                        .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
//                        .delta()
//                            .display()
//                            .assertModify()
//                            .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_CN))
//                            .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_GIVENNAME))
//                        .end();

        assertModelShadowFutureNoFetch(shadowOid)
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_CN, "jackNew2a")
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "jackNew2a")
                .end()
                .assertIsExists()
                .end();

    }

    /**
     * this test simulates situation, when someone tries to add account while
     * resource is down and this account is created by next get call on this
     * account
     */
    @Test
    public void test260GetDiscoveryAddCommunicationProblem() throws Exception {
        final String TEST_NAME = "test260GetDiscoveryAddCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        display("OpenDJ stopped");
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_ANGELIKA_FILENAME, parentResult);

        assertUser(USER_ANGELIKA_OID, "User before")
                .assertLinks(0);

        Task task = taskManager.createTaskInstance();

        // WHEN
        when();

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        assignAccount(UserType.class, USER_ANGELIKA_OID, RESOURCE_OPENDJ_OID, "internal", task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        // THEN
        then();
        parentResult.computeStatus();
        assertInProgress(parentResult);

        assertUser(USER_ANGELIKA_OID, "User after")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_ANGELIKA_OID, RESOURCE_OPENDJ_OID);
        assertRepoShadow(shadowOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoPrimaryIdentifierValue()
                .assertNoLegacyConsistency()
                .attributes()
                .assertAttributes(LDAP_ATTRIBUTE_DN, LDAP_ATTRIBUTE_UID)
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertAdd();
        //start openDJ
        openDJController.start();
        //and set the resource availability status to UP
        TestUtil.info("OpenDJ started, resource UP");

        PrismObject<ShadowType> shadow = getShadowModel(shadowOid, GetOperationOptions.createForceRetry(), true);
        //TODO more asserts
    }

    @Test
    public void test262GetDiscoveryModifyCommunicationProblem() throws Exception {
        final String TEST_NAME = "test262GetDiscoveryModifyCommunicationProblem";

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult parentResult = task.getResult();

        //prepare user
        repoAddObjectFromFile(USER_ALICE_FILENAME, parentResult);
        assertUser(USER_ALICE_OID, "User before")
                .assertLinks(0);

        //and add account to the user while resource is UP
        assignAccountToUser(USER_ALICE_OID, RESOURCE_OPENDJ_OID, "internal");

        //then stop openDJ
        openDJController.stop();

        assertUser(USER_ALICE_OID, "User after")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_ALICE_OID, RESOURCE_OPENDJ_OID);

        //and make some modifications to the account while resource is DOWN
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        modifyAccountShadowReplace(shadowOid, createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER), task, parentResult, "44332");
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        assertModelShadowNoFetch(shadowOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertModify()
                .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER));

        //start openDJ
        openDJController.start();
        //and set the resource availability status to UP
        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);

        assertModelShadowFuture(shadowOid)
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER, "44332");

        //and then try to get account -> result is that the modifications will be applied to the account
        XMLGregorianCalendar lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<ShadowType> shadowAfter = getShadowModel(shadowOid, GetOperationOptions.createForceRetry(), true);
        XMLGregorianCalendar lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();

        ShadowAsserter.forShadow(shadowAfter)
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER, "44332")
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(2)
                .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                .delta()
                .display()
                .assertModify()
                .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER));
    }

    @Test
    public void test264GetDiscoveryModifyUserPasswordCommunicationProblem() throws Exception {
        final String TEST_NAME = "test264GetDiscoveryModifyUserPasswordCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult parentResult = task.getResult();

        assertUser(USER_ALICE_OID, "User before")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_ALICE_OID, RESOURCE_OPENDJ_OID);
        // WHEN (down)
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        modifyUserChangePassword(USER_ALICE_OID, "DEADmenTELLnoTALES", task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        // THEN
        //check the state after execution
        assertModelShadowNoFetch(shadowOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .assertOperations(2)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertModify()
                .assertHasModification(SchemaConstants.PATH_PASSWORD_VALUE)
                .end()
                .end()
                .by()
                .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .find()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertAttemptNumber(2)
                .delta()
                .display()
                .assertModify()
                .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER));
        ;

        //start openDJ
        openDJController.start();
        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);

        // WHEN (restore)
        //and then try to get account -> result is that the modifications will be applied to the account
        PrismObject<ShadowType> shadowAfter = getShadowModel(shadowOid, GetOperationOptions.createForceRetry(), true);
        ShadowAsserter.forShadow(shadowAfter)
                .assertNotDead()
                .display()
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER, "44332")
                .assertHasPrimaryIdentifier();

        assertUser(USER_ALICE_OID, "User after")
                .assertPassword("DEADmenTELLnoTALES");

        aliceAccountDn = ShadowUtil.getAttributeValue(shadowAfter.asObjectable(), getOpenDjSecondaryIdentifierQName());
        openDJController.assertPassword(aliceAccountDn, "DEADmenTELLnoTALES");
    }

    /**
     * Modify user password when resource is down. Bring resource up and run recon.
     * Check that the password change is applied.
     */
    @Test
    public void test265ModifyUserPasswordCommunicationProblemRecon() throws Exception {
        final String TEST_NAME = "test265ModifyUserPasswordCommunicationProblemRecon";

        // GIVEN
        openDJController.assumeStopped();
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult result = task.getResult();

        assertUser(USER_ALICE_OID, "User before")
                .assertLinks(1);
        String shadowOid = getLinkRefOid(USER_ALICE_OID, RESOURCE_OPENDJ_OID);

        // WHEN (down)
        when();
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        modifyUserChangePassword(USER_ALICE_OID, "UNDEADmenTELLscaryTALES", task, result);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        // THEN
        then();
        //check the state after execution
        assertModelShadowNoFetch(shadowOid)
                .display(TEST_NAME + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoLegacyConsistency()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .pendingOperations()
                .assertOperations(3)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                .assertAttemptNumber(1)
                .assertLastAttemptTimestamp(lastRequestStartTs, lastRequestEndTs)
                .delta()
                .display()
                .assertModify()
                .assertHasModification(SchemaConstants.PATH_PASSWORD_VALUE);

        //start openDJ
        openDJController.start();
        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, result);

        // WHEN (restore)
        when();
        reconcileUser(USER_ALICE_OID, task, result);

        // THEN
        then();
        openDJController.assertPassword(aliceAccountDn, "UNDEADmenTELLscaryTALES");

        assertUser(USER_ALICE_OID, "User after")
                .assertPassword("UNDEADmenTELLscaryTALES");

    }

    /**
     * this test simulates situation, when someone tries to add account while
     * resource is down and this account is created by next get call on this
     * account
     */
    @Test
    public void test270ModifyDiscoveryAddCommunicationProblem() throws Exception {
        final String TEST_NAME = "test270ModifyDiscoveryAddCommunicationProblem";

        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        // WHEN
        repoAddObjectFromFile(USER_BOB_NO_GIVEN_NAME_FILENAME, parentResult);

        assertUser(USER_BOB_NO_GIVEN_NAME_OID, "User bofore")
                .assertLinks(0);

        Task task = taskManager.createTaskInstance();

        //REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
        assignAccount(UserType.class, USER_BOB_NO_GIVEN_NAME_OID, RESOURCE_OPENDJ_OID, "internal", task, parentResult);

        parentResult.computeStatus();
        assertInProgress(parentResult);

        assertUser(USER_BOB_NO_GIVEN_NAME_OID, "User after")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_BOB_NO_GIVEN_NAME_OID, RESOURCE_OPENDJ_OID);

        //start openDJ
        openDJController.start();

        // WHEN
        // This should not throw exception
        getShadowModelNoFetch(shadowOid);

        OperationResult modifyGivenNameResult = new OperationResult("execute changes -> modify user's given name");
        logger.trace("execute changes -> modify user's given name");
        modifyObjectAddProperty(UserType.class, USER_BOB_NO_GIVEN_NAME_OID, UserType.F_GIVEN_NAME, task, modifyGivenNameResult, new PolyString("Bob"));

        modifyGivenNameResult.computeStatus();
        assertSuccess(modifyGivenNameResult);

//        PrismObject<ShadowType> shadowAfter = getShadowModel(shadowOid, GetOperationOptions.createForceRefresh(), true);
//        ShadowAsserter.forShadow(shadowAfter)
        assertModelShadow(shadowOid)
                .assertIsExists()
                .assertNotDead()
                .assertResource(RESOURCE_OPENDJ_OID)
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "Bob")
                .assertValue(LDAP_ATTRIBUTE_UID, "bob")
                .assertValue(LDAP_ATTRIBUTE_CN, "Bob Dylan")
                .assertValue(LDAP_ATTRIBUTE_SN, "Dylan");

    }

    @Test
    public void test280ModifyObjectCommunicationProblemWeakMapping() throws Exception {
        final String TEST_NAME = "test280ModifyObjectCommunicationProblemWeakMapping";

        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_JOHN_WEAK_FILENAME, parentResult);
        assertUser(USER_JOHN_WEAK_OID, "User before")
                .assertLinks(0);

        Task task = taskManager.createTaskInstance();

        //REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
        assignAccountToUser(USER_JOHN_WEAK_OID, RESOURCE_OPENDJ_OID, "internal");

        assertUser(USER_JOHN_WEAK_OID, "User after")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_JOHN_WEAK_OID, RESOURCE_OPENDJ_OID);
        assertModelShadow(shadowOid)
                .assertIsExists()
                .assertNotDead()
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_UID, "john")
                .assertValue(LDAP_ATTRIBUTE_SN, "weak")
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "john")
                .assertValue(LDAP_ATTRIBUTE_CN, "john weak")
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_TYPE, "manager");

        //stop opendj and try to modify employeeType (weak mapping)
        openDJController.stop();

        logger.info("start modifying user - account with weak mapping after stopping opendj.");

        modifyUserReplace(USER_JOHN_WEAK_OID, UserType.F_EMPLOYEE_TYPE, task, parentResult, "boss");

        assertModelShadowFutureNoFetch(shadowOid)
                .pendingOperations()
                .assertOperations(0);

//        // TODO: [RS] not 100% sure about this. But if you do not expect an error you should not set doNotDiscovery. Server is still not running.
//        checkNormalizedShadowBasic(accountOid, "john", true, null, task, parentResult);
//        checkNormalizedShadowBasic(accountOid, "john", true, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), task, parentResult);
    }

    @Test
    public void test282ModifyObjectCommunicationProblemWeakAndStrongMapping() throws Exception {
        final String TEST_NAME = "test282ModifyObjectCommunicationProblemWeakAndStrongMapping";

        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_DONALD_FILENAME, parentResult);

        assertUser(USER_DONALD_OID, "User before")
                .assertLinks(0);

        Task task = taskManager.createTaskInstance();

        //REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
        assignAccount(UserType.class, USER_DONALD_OID, RESOURCE_OPENDJ_OID, "internal");

        assertUser(USER_DONALD_OID, "User after")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_DONALD_OID, RESOURCE_OPENDJ_OID);

        assertModelShadow(shadowOid)
                .assertIsExists()
                .assertNotDead()
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_UID, "donald")
                .assertValue(LDAP_ATTRIBUTE_SN, "trump")
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "donald")
                .assertValue(LDAP_ATTRIBUTE_CN, "donald trump")
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_TYPE, "manager");

        //stop opendj and try to modify employeeType (weak mapping)
        openDJController.stop();

        logger.info("start modifying user - account with weak mapping after stopping opendj.");

        PropertyDelta<String> employeeTypeDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_EMPLOYEE_TYPE, getUserDefinition(), "boss");

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory()
                .object()
                .createModificationReplaceProperty(UserType.class, USER_DONALD_OID, UserType.F_GIVEN_NAME, new PolyString("don"));
        userDelta.addModification(employeeTypeDelta);

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(userDelta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        assertModelShadowNoFetch(shadowOid)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertAttemptNumber(1)
                .delta()
                .assertModify()
                .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_GIVENNAME))
                .assertNoModification(createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_TYPE));

    }

    @Test
    public void test283GetObjectNoFetchShadowAndRecompute() throws Exception {
        final String TEST_NAME = "test283GetObjectNoFetchShadowAndRecompute";

        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        assertUser(USER_DONALD_OID, "User before")
                .assertLinks(1);
        String shadowOid = getLinkRefOid(USER_DONALD_OID, RESOURCE_OPENDJ_OID);

        Task task = taskManager.createTaskInstance();

        assertModelShadowNoFetch(shadowOid)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .pendingOperations()
                .singleOperation()
                .display()
                .assertAttemptNumber(1)
                .delta()
                .assertModify()
                .assertHasModification(createAttributePath(LDAP_ATTRIBUTE_GIVENNAME))
                .assertNoModification(createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_TYPE));

        //WHEN
        when();
        recomputeUser(USER_DONALD_OID, ModelExecuteOptions.createReconcile(), task, parentResult);

        //THEN
        then();
        assertModelShadow(shadowOid)
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "don")
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_TYPE, "manager");

    }

    @Test
    public void test284ModifyObjectAssignToGroupCommunicationProblem() throws Exception {
        final String TEST_NAME = "test284ModifyObjectAssignToGroupCommunicationProblem";
        Task task = taskManager.createTaskInstance();
        OperationResult parentResult = createOperationResult();
        // GIVEN
        openDJController.addEntriesFromLdifFile(LDIF_CREATE_ADMINS_GROUP_FILE);

        ObjectQuery filter = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_GROUP_OBJECTCLASS, prismContext);
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, filter, null, task, parentResult);

        for (PrismObject<ShadowType> shadow : shadows) {
            logger.info("SHADOW ===> {}", shadow.debugDump());
        }

        // WHEN
        openDJController.assumeStopped();

        assertUser(USER_DONALD_OID, "User before")
                .assertLinks(1);

        String shadowOid = getLinkRefOid(USER_DONALD_OID, RESOURCE_OPENDJ_OID);

        // WHEN
        when();

        AssignmentType adminRoleAssignment = new AssignmentType(prismContext);
        adminRoleAssignment.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_LDAP_ADMINS_OID, ObjectTypes.ROLE));
        ObjectDelta<UserType> delta = prismContext.deltaFactory()
                .object()
                .createModificationAddContainer(UserType.class, USER_DONALD_OID, UserType.F_ASSIGNMENT, adminRoleAssignment);
        delta.addModification(prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_GIVEN_NAME, getUserDefinition(), new PolyString("donalld")));

        executeChanges(delta, null, task, parentResult);

        // Get user's account with noFetch option - changes shouldn't be applied, bud should be still saved in shadow
        assertModelShadowNoFetch(shadowOid)
                .assertIsExists()
                .assertNotDead()
                .assertIntent("internal")
                .assertKind()
                .pendingOperations()
                .by()
                .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .delta()
                .assertNoModification(createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_TYPE))
                .assertHasModification(ShadowType.F_ASSOCIATION);

        //THEN
        openDJController.assumeRunning();

        recomputeUser(USER_DONALD_OID, ModelExecuteOptions.createReconcile(), task, parentResult);

        assertModelShadow(shadowOid)
                .attributes()
                .assertValue(LDAP_ATTRIBUTE_GIVENNAME, "donalld")
                .assertValue(LDAP_ATTRIBUTE_EMPLOYEE_TYPE, "manager");

        openDJController.assertUniqueMember(ROLE_LDAP_ADMINS_DN, ACCOUNT_DONALD_LDAP_DN);
        //TODO: check on user if it was processed successfully (add this check also to previous (30) test..
    }

    //TODO: enable after notify failure will be implemented..
    @Test(enabled = false)
    public void test400GetDiscoveryAddCommunicationProblemAlreadyExists() throws Exception {
        final String TEST_NAME = "test400GetDiscoveryAddCommunicationProblemAlreadyExists";

        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_DISCOVERY_FILENAME, parentResult);

        assertUserNoAccountRef(USER_DISCOVERY_OID, parentResult);

        Task task = taskManager.createTaskInstance();

        assignAccount(UserType.class, USER_DISCOVERY_OID, RESOURCE_OPENDJ_OID, "internal", task, parentResult);

        parentResult.computeStatus();
        display("add object communication problem result: ", parentResult);
        assertEquals("Expected success but got: " + parentResult.getStatus(), OperationResultStatus.HANDLED_ERROR, parentResult.getStatus());

        String accountOid = assertUserOneAccountRef(USER_DISCOVERY_OID);

        openDJController.start();
        assertTrue(EmbeddedUtils.isRunning());

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_DISCOVERY_FILE);
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

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(TestConsistencyMechanism.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        Entry entry = openDJController.addEntryFromLdifFile(LDIF_MORGAN_FILE);
        display("Entry from LDIF", entry);

        // WHEN
        when();
        addObject(USER_MORGAN_FILE, task, result);

        // THEN
        then();

        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String accountOid = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        provisioningService.applyDefinition(accountShadow, task, result);
        display("account shadow (repo)", accountShadow);
        assertShadowRepo(accountShadow, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("account shadow (model)", accountModel);
        assertShadowModel(accountModel, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
    }

    /**
     * Adding a user (morgan) that has an OpenDJ assignment. But the equivalent account already exists on
     * OpenDJ and there is also corresponding shadow in the repo. The account should be linked.
     */
    @Test
    public void test501AddUserChuckWithAssignment() throws Exception {
        final String TEST_NAME = "test501AddUserChuckWithAssignment";

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(TestConsistencyMechanism.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

//        Entry entry = openDJController.addEntryFromLdifFile(LDIF_MORGAN_FILENAME);
//        display("Entry from LDIF", entry);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_CHUCK_FILENAME));
        String accOid = provisioningService.addObject(account, null, null, task, result);
//

        // WHEN
        when();
        addObject(USER_CHUCK_FILE);

        // THEN
        then();
        result.computeStatus();
//        assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());

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
        provisioningService.applyDefinition(accountShadow, task, result);
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

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(TestConsistencyMechanism.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

//        Entry entry = openDJController.addEntryFromLdifFile(LDIF_MORGAN_FILENAME);
//        display("Entry from LDIF", entry);

        PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(ACCOUNT_HERMAN_FILENAME));
        String shadowOidBefore = provisioningService.addObject(account, null, null, task, result);
//
        repoAddObjectFromFile(USER_HERMAN_FILENAME, result);

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(USER_HERMAN_FILENAME));
        display("Adding user", user);

        //REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
        //WHEN
        when();
        assignAccount(UserType.class, USER_HERMAN_OID, RESOURCE_OPENDJ_OID, "internal", task, result);

        // THEN
        then();
        result.computeStatus();
//        assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());

        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_HERMAN_OID, null, task, result);
        display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        ObjectReferenceType accountRefType = userMorganType.getLinkRef().get(0);
        String shadowOidAfter = accountRefType.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(shadowOidAfter));
        assertEquals("old oid not used..", shadowOidBefore, shadowOidAfter);
        assertEquals("old oid not used..", ACCOUNT_HERMAN_OID, shadowOidAfter);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, shadowOidAfter, null, result);
        provisioningService.applyDefinition(accountShadow, task, result);
        assertShadowRepo(accountShadow, shadowOidAfter, "uid=ht,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, shadowOidAfter, null, task, result);
        assertShadowModel(accountModel, shadowOidAfter, "uid=ht,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        ShadowType accountTypeModel = accountModel.asObjectable();

        // Check account's attributes
        assertAttributes(accountTypeModel, "ht", "Herman", "Toothrot", "Herman Toothrot");

        // TODO: check OpenDJ Account
    }

    /**
     * Unlink account morgan, delete shadow and remove assignmnet from user morgan - preparation for the next test
     */
    @Test
    public void test510UnlinkAndUnassignAccountMorgan() throws Exception {
        final String TEST_NAME = "test510UnlinkAndUnassignAccountMorgan";

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(TestConsistencyMechanism.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_MORGAN_OID, null, result);
        display("User Morgan: ", user);
        List<PrismReferenceValue> linkRefs = user.findReference(UserType.F_LINK_REF).getValues();
        assertEquals("Unexpected number of link refs", 1, linkRefs.size());
        PrismReferenceValue linkRef = linkRefs.iterator().next();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object().createModificationDeleteReference(UserType.class,
                USER_MORGAN_OID, UserType.F_LINK_REF, linkRef.clone());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        ///----user's link is removed, now, remove assignment
        userDelta = prismContext.deltaFactory().object().createModificationDeleteContainer(UserType.class,
                USER_MORGAN_OID, UserType.F_ASSIGNMENT,
                user.findContainer(UserType.F_ASSIGNMENT).getValue().clone());
        deltas = MiscSchemaUtil.createCollection(userDelta);
        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        repositoryService.deleteObject(ShadowType.class, linkRef.getOid(), result);

        // THEN
        then();
        result.computeStatus();
//        assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());

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
        } catch (ObjectNotFoundException ex) {
            //this is expected..shadow must not exist in repo
        }
        // Check account
        try {
            PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
            assertAccountShadowModel(accountModel, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo);
            fail("Unexpected shadow in repo. Shadow mut not exist");
        } catch (ObjectNotFoundException ex) {
            //this is expected..shadow must not exist in repo
        }
        // TODO: check OpenDJ Account
    }

    /**
     * assign account to the user morgan. Account with the same 'uid' (not dn, nut other secondary identifier already exists)
     * account should be linked to the user.
     *
     * @throws Exception
     */
    @Test
    public void test511AssignAccountMorgan() throws Exception {
        final String TEST_NAME = "test511AssignAccountMorgan";

        // GIVEN
        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(TestConsistencyMechanism.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        //prepare new OU in opendj
        Entry entry = openDJController.addEntryFromLdifFile(LDIF_CREATE_USERS_OU_FILE);

        PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_MORGAN_OID, null, result);
        display("User Morgan: ", user);
        PrismReference linkRef = user.findReference(UserType.F_LINK_REF);

        ExpressionType expression = new ExpressionType();
        ObjectFactory of = new ObjectFactory();
        RawType raw = new RawType(prismContext.xnodeFactory().primitive("uid=morgan,ou=users,dc=example,dc=com"), prismContext);

        JAXBElement val = of.createValue(raw);
        expression.getExpressionEvaluator().add(val);

        MappingType mapping = new MappingType();
        mapping.setExpression(expression);

        ResourceAttributeDefinitionType attrDefType = new ResourceAttributeDefinitionType();
        attrDefType.setRef(new ItemPathType(ItemPath.create(getOpenDjSecondaryIdentifierQName())));
        attrDefType.setOutbound(mapping);

        ConstructionType construction = new ConstructionType();
        construction.getAttribute().add(attrDefType);
        construction.setResourceRef(ObjectTypeUtil.createObjectRef(resourceTypeOpenDjrepo, prismContext));

        AssignmentType assignment = new AssignmentType();
        assignment.setConstruction(construction);

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationAddContainer(UserType.class, USER_MORGAN_OID, UserType.F_ASSIGNMENT,
                        assignment.asPrismContainerValue());
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
        result.computeStatus();
//        assertEquals("Expected handled error but got: " + result.getStatus(), OperationResultStatus.HANDLED_ERROR, result.getStatus());

        PrismObject<UserType> userMorgan = modelService.getObject(UserType.class, USER_MORGAN_OID, null, task, result);
        display("User morgan after", userMorgan);
        UserType userMorganType = userMorgan.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userMorganType.getLinkRef().size());
        String accountOid = userMorganType.getLinkRef().iterator().next().getOid();

        // Check shadow

        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        provisioningService.applyDefinition(accountShadow, task, result);
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

        openDJController.assumeRunning();
        Task task = taskManager.createTaskInstance(TEST_NAME);
        OperationResult parentResult = task.getResult();

        ObjectDelta<UserType> deleteAliceDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(UserType.class, USER_ALICE_OID);

        modelService.executeChanges(MiscSchemaUtil.createCollection(deleteAliceDelta), null, task, parentResult);

        try {
            modelService.getObject(UserType.class, USER_ALICE_OID, null, task, parentResult);
            fail("Expected object not found error, but haven't got one. Something went wrong while deleting user alice");
        } catch (ObjectNotFoundException ex) {
            //this is expected

        }

    }

    @Test
    public void test601GetDiscoveryModifyCommunicationProblemDirectAccount() throws Exception {
        String TEST_NAME = "test601GetDiscoveryModifyCommunicationProblemDirectAccount";

        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        //prepare user
        repoAddObjectFromFile(USER_ALICE_FILENAME, parentResult);

        assertUserNoAccountRef(USER_ALICE_OID, parentResult);

        Task task = taskManager.createTaskInstance();
        //and add account to the user while resource is UP

        //REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
        ObjectDelta<UserType> addDelta = createModifyUserAddAccount(USER_ALICE_OID, resourceTypeOpenDjrepo.asPrismObject());
        executeChanges(addDelta, null, task, parentResult);

        //then stop openDJ
        openDJController.stop();

        String accountOid = assertUserOneAccountRef(USER_ALICE_OID);

        //and make some modifications to the account while resource is DOWN
        //WHEN
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory()
                .object()
                .createModificationAddProperty(ShadowType.class, accountOid, createAttributePath(LDAP_ATTRIBUTE_EMPLOYEE_NUMBER), "emp4321");
        delta.addModificationReplaceProperty(createAttributePath(LDAP_ATTRIBUTE_GIVENNAME), "Aliceeee");

        executeChanges(delta, null, task, parentResult);

        //check the state after execution
        checkPostponedAccountBasic(accountOid, FailedOperationTypeType.MODIFY, true, parentResult);

        //start openDJ
        openDJController.start();
        //and set the resource availability status to UP
//        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);
        ObjectDelta<UserType> emptyAliceDelta = prismContext.deltaFactory().object()
                .createEmptyDelta(UserType.class, USER_ALICE_OID, ChangeType.MODIFY);
        modelService.executeChanges(MiscSchemaUtil.createCollection(emptyAliceDelta), ModelExecuteOptions.createReconcile(), task, parentResult);
        accountOid = assertUserOneAccountRef(USER_ALICE_OID);

        //and then try to get account -> result is that the modifications will be applied to the account
//        ShadowType aliceAccount = checkNormalizedShadowWithAttributes(accountOid, "alice", "Jackkk", "alice", "alice", true, task, parentResult);
//        assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

        //and finally stop openDJ
//        openDJController.stop();
    }

    // This should run last. It starts a task that may interfere with other tests
    //MID-5844
    @Test//(enabled = false)
    public void test800Reconciliation() throws Exception {
        final String TEST_NAME = "test800Reconciliation";

        openDJController.assumeRunning();

        final OperationResult result = new OperationResult(TestConsistencyMechanism.class.getName() + "." + TEST_NAME);

        // TODO: remove this if the previous test is enabled
//        openDJController.start();

        // rename eobject dirrectly on resource before the recon start ..it tests the rename + recon situation (MID-1594)

        // precondition
        assertTrue(EmbeddedUtils.isRunning());
        UserType userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result).asObjectable();
        display("Jack before", userJack);

        logger.info("start running task");
        // WHEN
        repoAddObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILE, result);
        verbose = true;
        long started = System.currentTimeMillis();
        waitForTaskNextRunAssertSuccess(TASK_OPENDJ_RECONCILIATION_OID, false, 120000);
        logger.info("Reconciliation task run took {} seconds", (System.currentTimeMillis() - started) / 1000L);

        // THEN

        // STOP the task. We don't need it any more. Even if it's non-recurrent its safer to delete it
        taskManager.deleteTask(TASK_OPENDJ_RECONCILIATION_OID, result);

        // check if the account was added after reconciliation
        UserType userE = repositoryService.getObject(UserType.class, USER_E_OID, null, result).asObjectable();
        String accountOid = assertUserOneAccountRef(USER_E_OID);

        ShadowType eAccount = checkNormalizedShadowWithAttributes(accountOid, "e", "eeeee", "e", "e", true, null, result);
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

        // check if the account was marked as dead during the reconciliation process
        assertRepoShadow(ACCOUNT_DENIELS_OID)
                .assertDead();

        LinksAsserter linksAsserter = assertUser(USER_ELAINE_OID, "User after recon")
                .assertLinks(2)
                .links();

        ShadowReferenceAsserter notDeadShadow = linksAsserter.by()
                .dead(false)
                .find();

        assertModelShadow(notDeadShadow.getOid())
                .display()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .end();

        ShadowReferenceAsserter deadShadow = linksAsserter.by()
                .dead(true)
                .find();

        assertModelShadowNoFetch(deadShadow.getOid())
                .display()
                .attributes()
                .assertNoPrimaryIdentifier()
                .end()
                .pendingOperations()
                .singleOperation()
                .display()
                .delta()
                .assertAdd();

        accountOid = assertUserOneAccountRef(USER_JACKIE_OID);
        ShadowType jack2Shadow = checkNormalizedShadowBasic(accountOid, "jack2", true, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null, result);
        assertAttribute(jack2Shadow, "givenName", "jackNew2a");
        assertAttribute(jack2Shadow, "cn", "jackNew2a");

    }

    //MID-5844
    @Test//(enabled = false)
    public void test801TestReconciliationRename() throws Exception {
        final String TEST_NAME = "test801TestReconciliationRename";

        openDJController.assumeRunning();
        Task task = getTestTask();
        final OperationResult result = task.getResult();

        logger.info("starting rename");

        openDJController.executeRenameChange(LDIF_MODIFY_RENAME_FILE);
        logger.info("rename ended");
//        Entry res = openDJController.searchByUid("e");
//        LOGGER.info("E OBJECT AFTER RENAME " + res.toString());

        logger.info("start running task");
        // WHEN
        long started = System.currentTimeMillis();
        repoAddObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILE, result);
        waitForTaskFinish(TASK_OPENDJ_RECONCILIATION_OID, false, 120000);
        logger.info("Reconciliation task run took {} seconds", (System.currentTimeMillis() - started) / 1000L);

        // THEN

        // STOP the task. We don't need it any more. Even if it's non-recurrent its safer to delete it
        taskManager.deleteTask(TASK_OPENDJ_RECONCILIATION_OID, result);

        // check if the account was added after reconciliation
        UserType userE = repositoryService.getObject(UserType.class, USER_E_OID, null, result).asObjectable();
        String accountOid = assertUserOneAccountRef(USER_E_OID);

        ShadowType eAccount = checkNormalizedShadowWithAttributes(accountOid, "e123", "eeeee", "e", "e", true, null, result);
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

        provisioningService.applyDefinition(repoShadow, task, result);

        ResourceAttributeContainer repoAttributeContainer = ShadowUtil.getAttributesContainer(repoShadow);
        ResourceAttribute repoIcfNameAttr = repoAttributeContainer.findAttribute(getOpenDjSecondaryIdentifierQName());
        assertEquals("Wrong secondary indetifier.", "uid=e123,ou=people,dc=example,dc=com", repoIcfNameAttr.getRealValue());

        assertEquals("Wrong shadow name. ", "uid=e123,ou=people,dc=example,dc=com", repoShadow.asObjectable().getName().getOrig());

    }

    private void checkRepoOpenDjResource() throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult(TestConsistencyMechanism.class.getName()
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
        Collection<? extends Item<?, ?>> configProps = configPropsContainer.getValue().getItems();
        assertEquals("Wrong number of config properties in " + resource + " from " + source, numConfigProps,
                configProps.size());
        PrismProperty<Object> credentialsProp = configPropsContainer.findProperty(new ItemName(
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
        ObjectDelta delta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
        modelService.executeChanges(deltas, null, task, result);
        // THEN

        assertNoRepoCache();

        return userType;
    }

    private String assertUserOneAccountRef(String userOid) throws Exception {
        OperationResult parentResult = new OperationResult("getObject from repo");

        PrismObject<UserType> repoUser = repositoryService.getObject(UserType.class, userOid,
                null, parentResult);
        UserType repoUserType = repoUser.asObjectable();

        parentResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", parentResult);

        return assertOneAccountRef(repoUser);
    }

    private String assertOneAccountRef(PrismObject<UserType> user) throws Exception {

        UserType repoUserType = user.asObjectable();
        display("User (repository)", user);

        List<ObjectReferenceType> accountRefs = repoUserType.getLinkRef();
        assertEquals("No accountRefs", 1, accountRefs.size());
        ObjectReferenceType accountRef = accountRefs.get(0);

        return accountRef.getOid();
    }

    private void assertUserNoAccountRef(String userOid, OperationResult parentResult) throws Exception {
        PrismObject<UserType> user = repositoryService
                .getObject(UserType.class, userOid, null, parentResult);
        assertEquals(0, user.asObjectable().getLinkRef().size());
    }

    private QName getOpenDjSecondaryIdentifierQName() {
        return new QName(RESOURCE_OPENDJ_NS, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER_LOCAL_NAME);
    }

    private String checkUser(String userOid, Task task, OperationResult parentResult) throws Exception {
        PrismObject<UserType> user = modelService.getObject(UserType.class, userOid, null, task, parentResult);
        return checkUser(user);
    }

    private String checkUser(PrismObject<UserType> user) {
        assertNotNull("User must not be null", user);
        UserType userType = user.asObjectable();
        assertEquals("User must have one link ref, ", 1, userType.getLinkRef().size());
        MidPointAsserts.assertAssignments(user, 1);

        String accountOid = userType.getLinkRef().get(0).getOid();

        return accountOid;
    }

    private void assertAttributes(ShadowType shadow, String uid, String givenName, String sn, String cn) {
        assertAttribute(shadow, "uid", uid);
        if (givenName != null) {
            assertAttribute(shadow, "givenName", givenName);
        }
        if (sn != null) {
            assertAttribute(shadow, "sn", sn);
        }
        assertAttribute(shadow, "cn", cn);
    }

    private ShadowType checkPostponedAccountBasic(PrismObject<ShadowType> failedAccount, FailedOperationTypeType failedOperation, boolean modify, OperationResult parentResult) throws Exception {
        display("Repository shadow (postponed operation expected)", failedAccount);
        assertNotNull("Shadow must not be null", failedAccount);
        ShadowType failedAccountType = failedAccount.asObjectable();
        assertNotNull(failedAccountType);
        // Too much noise
//        displayJaxb("shadow from the repository: ", failedAccountType, ShadowType.COMPLEX_TYPE);
        // TODO FIX THIS!!!
//        assertEquals("Failed operation saved with account differt from  the expected value.",
//                failedOperation, failedAccountType.getFailedOperationType());
//        assertNotNull("Result of failed shadow must not be null.", failedAccountType.getResult());
//        assertNotNull("Shadow does not contain resource ref.", failedAccountType.getResourceRef());
//        assertEquals("Wrong resource ref in shadow", resourceTypeOpenDjrepo.getOid(), failedAccountType.getResourceRef().getOid());
//        if (modify){
//            assertNotNull("Null object change in shadow", failedAccountType.getObjectChange());
//        }
        return failedAccountType;
    }

    private ShadowType checkPostponedAccountBasic(String accountOid, FailedOperationTypeType failedOperation, boolean modify, OperationResult parentResult) throws Exception {
        PrismObject<ShadowType> faieldAccount = repositoryService.getObject(ShadowType.class, accountOid, null, parentResult);
        return checkPostponedAccountBasic(faieldAccount, failedOperation, modify, parentResult);
    }

    private Collection<ObjectDelta<? extends ObjectType>> createDeltas(Class type, File requestFile, String objectOid) throws IOException, SchemaException, JAXBException {

        try {
            ObjectDeltaType objectChange = unmarshallValueFromFile(requestFile, ObjectDeltaType.class);
            objectChange.setOid(objectOid);

            ObjectDelta delta = DeltaConvertor.createObjectDelta(objectChange, prismContext);

            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);

            return deltas;
        } catch (Exception ex) {
            logger.error("ERROR while unmarshalling: {}", ex);
            throw ex;
        }

    }

    private void modifyResourceAvailabilityStatus(AvailabilityStatusType status, OperationResult parentResult) throws Exception {
        PropertyDelta resourceStatusDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(ItemPath.create(
                ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
                resourceTypeOpenDjrepo.asPrismObject().getDefinition(), status);
        Collection<PropertyDelta> modifications = new ArrayList<>();
        modifications.add(resourceStatusDelta);
        repositoryService.modifyObject(ResourceType.class, resourceTypeOpenDjrepo.getOid(), modifications, parentResult);
    }

    private ShadowType checkNormalizedShadowWithAttributes(String accountOid, String uid, String givenName, String sn, String cn, boolean modify, Task task, OperationResult parentResult) throws Exception {
        ShadowType resourceAccount = checkNormalizedShadowBasic(accountOid, uid, modify, null, task, parentResult);
        assertAttributes(resourceAccount, uid, givenName, sn, cn);
        return resourceAccount;
    }

    private ShadowType checkNormalizedShadowBasic(String accountOid, String name, boolean modify, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws Exception {
        PrismObject<ShadowType> account = modelService.getObject(ShadowType.class, accountOid, options, task, parentResult);
        assertNotNull(account);
        ShadowType accountType = account.asObjectable();
        display("Shadow after discovery", account);
        // TODO FIX THIS!!!
//        assertNull(name + "'s account after discovery must not have failed opertion.", accountType.getFailedOperationType());
//        assertNull(name + "'s account after discovery must not have result.", accountType.getResult());
//        assertNotNull(name + "'s account must contain reference on the resource", accountType.getResourceRef());
//        assertEquals(resourceTypeOpenDjrepo.getOid(), accountType.getResourceRef().getOid());
//
//        if (modify){
//            assertNull(name + "'s account must not have object change", accountType.getObjectChange());
//        }

        return accountType;
//        assertNotNull("Identifier in the angelica's account after discovery must not be null.",ResourceObjectShadowUtil.getAttributesContainer(faieldAccount).getIdentifier().getRealValue());

    }

    protected <T> void assertAttribute(ShadowType shadowType, String attrName, T... expectedValues) {
        assertAttribute(resourceTypeOpenDjrepo, shadowType, attrName, expectedValues);
    }

    protected <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
        assertAttribute(resourceTypeOpenDjrepo, shadow.asObjectable(), attrName, expectedValues);
    }
}
