/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.consistency;

import static com.evolveum.midpoint.test.IntegrationTestTools.toRiQName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoRepoThreadLocalCache;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.opends.server.types.DirectoryException;
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
import com.evolveum.midpoint.schema.CapabilityUtil;
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
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Consistency test suite. It tests consistency mechanisms. It works as end-to-end integration test across all subsystems.
 *
 * @author Katarina Valalikova
 */
@SuppressWarnings("SameParameterValue")
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConsistencyMechanism extends AbstractModelIntegrationTest {

    protected static final String TEST_DIR = "src/test/resources/consistency/";

    private static final String SYSTEM_CONFIGURATION_FILENAME = TEST_DIR + "system-configuration.xml";

    private static final String ROLE_SUPERUSER_FILENAME = TEST_DIR + "role-superuser.xml";

    private static final String ROLE_LDAP_ADMINS_FILENAME = TEST_DIR + "role-admins.xml";
    private static final String ROLE_LDAP_ADMINS_OID = "88888888-8888-8888-8888-000000000009";
    private static final String ROLE_LDAP_ADMINS_DN = "cn=admins,ou=groups,dc=example,dc=com";

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
    private static final String RESOURCE_OPENDJ_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance-3";
    private static final QName RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS = new QName(RESOURCE_OPENDJ_NS, "inetOrgPerson");
    private static final QName RESOURCE_OPENDJ_GROUP_OBJECTCLASS = new QName(RESOURCE_OPENDJ_NS, "groupOfUniqueNames");
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
    protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-111111116666";

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

    private static final String USER_TRAINEE_FILENAME = TEST_DIR + "user-trainee.xml";
    private static final String USER_TRAINEE_OID = "c0c010c0-0000-b33f-f00d-111112226666";

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
    protected static final String ACCOUNT_DENIELS_OID = "a0c010c0-d34d-b33f-f00d-111111111555";
    private static final String ACCOUNT_DENIELS_LDAP_UID = "deniels";
    //private static final String ACCOUNT_DENIELS_LDAP_DN = "uid=" + ACCOUNT_DENIELS_LDAP_UID + "," + OPENDJ_PEOPLE_SUFFIX;

    private static final String ACCOUNT_CHUCK_FILENAME = TEST_DIR + "account-chuck.xml";

    private static final String ACCOUNT_HERMAN_FILENAME = TEST_DIR + "account-herman.xml";
    private static final String ACCOUNT_HERMAN_OID = "22220000-2200-0000-0000-333300003333";

    private static final File ACCOUNT_JACKIE_FILE = new File(TEST_DIR, "account-jack.xml");
    private static final String ACCOUNT_JACKIE_OID = "22220000-2222-5555-0000-333300003333";
    private static final String ACCOUNT_JACKIE_LDAP_UID = "jackie";
    //private static final String ACCOUNT_JACKIE_LDAP_DN = "uid=" + ACCOUNT_JACKIE_LDAP_UID + "," + OPENDJ_PEOPLE_SUFFIX;

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

    private static final String INTENT_INTERNAL = "internal";

    private static ResourceType resourceTypeOpenDjrepo;
    private String aliceAccountDn;

    // This will get called from the superclass to init the repository
    // It will be called only once
    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        logger.trace("initSystem");
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, initResult);

        logger.trace("initSystem: trying modelService.postInit()");
        modelService.postInit(initResult);
        logger.trace("initSystem: modelService.postInit() done");

        repoAddObjectFromFile(ROLE_SUPERUSER_FILENAME, initResult);
        repoAddObjectFromFile(ROLE_LDAP_ADMINS_FILENAME, initResult);
        repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, initResult);

        login(USER_ADMINISTRATOR_NAME);

        // Need to import instead of add, so the (dynamic) connector reference
        // will be resolved correctly
        importObjectFromFile(getResourceFile(), initResult);

        repoAddObjectFromFile(USER_TEMPLATE_FILENAME, initResult);
    }

    protected File getResourceFile() {
        return RESOURCE_OPENDJ_FILE;
    }

    /**
     * Initialize embedded OpenDJ instance. Note: this is not in the abstract
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
        assertNotNull(modelService);
        assertNotNull(repositoryService);
        assertNotNull(taskManager);

        assertNotNull(prismContext);
        SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
        assertNotNull(schemaRegistry);

        // This is defined in extra schema. So this effectively checks whether the extra schema was loaded.
        PrismPropertyDefinition<?> shipStateDefinition = schemaRegistry
                .findPropertyDefinitionByElementName(MY_SHIP_STATE);
        assertNotNull("No my:shipState definition", shipStateDefinition);
        assertEquals("Wrong maxOccurs in my:shipState definition", 1, shipStateDefinition.getMaxOccurs());

        assertNoRepoThreadLocalCache();

        OperationResult result = createOperationResult();

        // Check if OpenDJ resource was imported correctly

        PrismObject<ResourceType> openDjResource = repositoryService.getObject(ResourceType.class,
                RESOURCE_OPENDJ_OID, null, result);
        display("Imported OpenDJ resource (repository)", openDjResource);
        AssertJUnit.assertEquals(RESOURCE_OPENDJ_OID, openDjResource.getOid());
        assertNoRepoThreadLocalCache();

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
        Task task = getTestTask();

        given();
        assertNoRepoThreadLocalCache();

        when();
        OperationResult result = modelService.testResource(RESOURCE_OPENDJ_OID, task, task.getResult());

        then();
        assertNoRepoThreadLocalCache();
        TestUtil.assertSuccess("testResource has failed", result);
        OperationResult opResult = createOperationResult();

        fetchResourceObject(opResult);

        assertNoRepoThreadLocalCache();
        assertEquals(RESOURCE_OPENDJ_OID, resourceTypeOpenDjrepo.getOid());
        display("Initialized OpenDJ resource (repository)", resourceTypeOpenDjrepo);
        assertNotNull("Resource schema was not generated", resourceTypeOpenDjrepo.getSchema());
        Element resourceOpenDjXsdSchemaElement = ResourceTypeUtil
                .getResourceXsdSchemaElement(resourceTypeOpenDjrepo);
        assertNotNull("Resource schema was not generated", resourceOpenDjXsdSchemaElement);

        PrismObject<ResourceType> openDjResourceProvisioning = provisioningService.getObject(
                ResourceType.class, RESOURCE_OPENDJ_OID, null, task, opResult);
        display("Initialized OpenDJ resource resource (provisioning)", openDjResourceProvisioning);

        PrismObject<ResourceType> openDjResourceModel = provisioningService.getObject(ResourceType.class,
                RESOURCE_OPENDJ_OID, null, task, opResult);
        display("Initialized OpenDJ resource OpenDJ resource (model)", openDjResourceModel);

        checkOpenDjResource(resourceTypeOpenDjrepo, "repository");

        System.out.println("------------------------------------------------------------------");
        displayValue("OpenDJ resource schema (repo XML)",
                DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchemaElement(resourceTypeOpenDjrepo)));
        System.out.println("------------------------------------------------------------------");

        checkOpenDjResource(openDjResourceProvisioning.asObjectable(), "provisioning");
        checkOpenDjResource(openDjResourceModel.asObjectable(), "model");
    }

    private void fetchResourceObject(OperationResult opResult) throws ObjectNotFoundException, SchemaException {
        resourceTypeOpenDjrepo = repositoryService
                .getObject(ResourceType.class, RESOURCE_OPENDJ_OID, null, opResult)
                .asObjectable();
    }

    /**
     * Attempt to add new user. It is only added to the repository, so check if
     * it is in the repository after the operation.
     */
    @Test
    public void test100AddUser() throws Exception {
        UserType userType = testAddUserToRepo(USER_JACK_FILENAME);

        OperationResult repoResult = new OperationResult("getObject");

        when();
        PrismObject<UserType> uObject = repositoryService.getObject(UserType.class, USER_JACK_OID, null, repoResult);

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

        // Adding jackie shadow directly and then linking this shadow to the user jack.
        // We need to do linking on repository level, to skip clockwork execution.
        PrismObject<ShadowType> jackieAccount = addObject(ACCOUNT_JACKIE_FILE, task, parentResult);
        String oid = jackieAccount.getOid();

        PrismObject<ShadowType> jackFromRepo = repositoryService.getObject(ShadowType.class, oid, null, parentResult);
        display("account jack after provisioning", jackFromRepo);

        ReferenceDelta linkRefDelta =
                prismContext.deltaFactory().reference().createModificationAdd(
                        UserType.class, UserType.F_LINK_REF,
                        ObjectTypeUtil.createObjectRef(jackieAccount, SchemaConstants.ORG_DEFAULT)
                                .asReferenceValue());
        repositoryService.modifyObject(UserType.class, USER_JACK_OID,
                Collections.singletonList(linkRefDelta), parentResult);

        // Check if user object was modified in the repo
        assertUserOneLinkRef(USER_JACK_OID);

        // check LDAP
        Entry entry = openDJController.searchByUid(ACCOUNT_JACKIE_LDAP_UID);
        assertNotNull("Entry uid " + ACCOUNT_JACKIE_LDAP_UID + " not found", entry);

        display("LDAP account", entry);
        OpenDJController.assertAttribute(entry, "uid", "jackie");
        OpenDJController.assertAttribute(entry, "givenName", "Jack");
        OpenDJController.assertAttribute(entry, "sn", "Sparrow");
        OpenDJController.assertAttribute(entry, "cn", "Jack Sparrow");

        assertNoRepoThreadLocalCache();

        // check full (model) shadow
        PrismObject<ShadowType> modelShadow = getShadowModel(ACCOUNT_JACKIE_OID);
        display("Shadow (model)", modelShadow);

        // @formatter:off
        ShadowAsserter.forShadow(modelShadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_INTERNAL)
                .attributes()
                    .assertHasPrimaryIdentifier()
                    .assertHasSecondaryIdentifier()
                    .assertValue(QNAME_UID, ACCOUNT_JACKIE_LDAP_UID)
                    .assertValue(QNAME_GIVEN_NAME, "Jack")
                    .assertValue(QNAME_CN, "Jack Sparrow")
                    .assertValue(QNAME_SN, "Sparrow")
                    .assertNoSimpleAttribute(QNAME_CAR_LICENSE)
                .end()
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on
    }

    /**
     * Creates a shadow of `deniels` (propagating to the LDAP resource).
     *
     * Then creates user `deniels` (having a link to that shadow) directly in repo.
     */
    @Test
    public void test111PrepareOpenDjWithDenielsAccounts() throws Exception {
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

        assertNoRepoThreadLocalCache();

        // check full (model) shadow
        PrismObject<ShadowType> modelShadow = getShadowModel(ACCOUNT_DENIELS_OID);
        display("Shadow (model)", modelShadow);

        // @formatter:off
        ShadowAsserter.forShadow(modelShadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_INTERNAL)
                .attributes()
                    .assertHasPrimaryIdentifier()
                    .assertHasSecondaryIdentifier()
                    .assertValue(QNAME_UID, ACCOUNT_DENIELS_LDAP_UID)
                    .assertValue(QNAME_GIVEN_NAME, "Jack")
                    .assertValue(QNAME_CN, "Jack Deniels")
                    .assertValue(QNAME_SN, "Deniels")
                    .assertNoSimpleAttribute(QNAME_CAR_LICENSE)
                .end()
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on

        repoAddObjectFromFile(USER_DENIELS_FILENAME, parentResult);
        //TODO some asserts?
    }

    /**
     * Checks resolution of account add conflict:
     *
     * 1. creates a new user `jackie`
     * 2. assigns an account to `jackie` (but `jackie` account already exists and is linked to `jack`)
     * 3. a different account (`jackie1`) should be created
     */
    @Test
    public void test120AddAccountAlreadyExistLinked() throws Exception {
        Task task = taskManager.createTaskInstance();

        OperationResult parentResult = new OperationResult("Add account already exist linked");

        given("User jackie added, previous user jack already having the account identifier jackie.");
        assertNoRepoThreadLocalCache();
        addObject(USER_JACKIE_FILE, task, parentResult);
        PrismObject<UserType> userJackieBefore = getUser(USER_JACKIE_OID);
        UserAsserter.forUser(userJackieBefore)
                .assertLiveLinks(0);

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        UserAsserter.forUser(userJackBefore)
                .assertLiveLinks(1)
                .links()
                .link(ACCOUNT_JACKIE_OID);
        //check if the jackie account already exists on the resource

        PrismObject<ShadowType> existingJackieAccount = getShadowRepoLegacy(ACCOUNT_JACKIE_OID);
        display("Jack's account: ", existingJackieAccount);

        when("Adding account on the resource to user jackie...");
        assignAccountToUser(USER_JACKIE_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL);

        then("The dn and ri:uid will be jackie1 because jackie already exists and is liked to another user");
        PrismObject<UserType> userJackieAfter = getUser(USER_JACKIE_OID);
        UserAsserter.forUser(userJackieAfter)
                .assertLiveLinks(1);

        ObjectReferenceType linkRef = userJackieAfter.asObjectable().getLinkRef().iterator().next();
        assertThat(linkRef.getOid()).withFailMessage("Wrong account linked")
                .isNotEqualTo(ACCOUNT_JACKIE_OID);

        // @formatter:off
        PrismObject<ShadowType> shadow = getShadowModel(linkRef.getOid());
        ShadowAsserter.forShadow(shadow)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_INTERNAL)
                .attributes()
                    .assertHasPrimaryIdentifier()
                    .assertHasSecondaryIdentifier()
                    .assertValue(QNAME_UID, "jackie1")
                    .assertValue(QNAME_GIVEN_NAME, "Jack")
                    .assertValue(QNAME_CN, "Jack Russel")
                    .assertValue(QNAME_SN, "Russel")
                    .assertNoSimpleAttribute(QNAME_CAR_LICENSE)
                .end()
                .assertResource(RESOURCE_OPENDJ_OID)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);
        // @formatter:on
    }

    /**
     * 1. creates unlinked LDAP account of `wturner` (no shadow)
     * 2. adds a user of `will`
     * 3. assigns `will` an LDAP account
     * 4. makes sure that the pre-existing `wturner` account is linked to user `will`
     */
    @Test
    public void test122AddAccountAlreadyExistUnlinked() throws Exception {
        given();
        OperationResult parentResult = new OperationResult("Add account already exist unlinked.");
        openDJController.addEntryFromLdifFile(LDIF_WILL_FILE);
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

        testAddUserToRepo(USER_WILL_FILENAME);
        assertUserNoAccountRef(USER_WILL_OID, parentResult);

        Task task = taskManager.createTaskInstance();

        when();
        assignAccount(UserType.class, USER_WILL_OID, RESOURCE_OPENDJ_OID, null, task, parentResult);

        then();
        String accountOid = checkUser(USER_WILL_OID, task, parentResult);

        PrismObject<ShadowType> account = provisioningService.getObject(ShadowType.class,
                accountOid, null, task, parentResult);

        ShadowAttributesContainer attributes = ShadowUtil.getAttributesContainer(account);

        assertEquals("shadow secondary identifier not equal with the account dn. ", dn, attributes
                .findSimpleAttribute(getOpenDjSecondaryIdentifierQName()).getRealValue(String.class));

        String identifier = attributes.getPrimaryIdentifier().getRealValue(String.class);

        openDJController.searchAndAssertByEntryUuid(identifier);
    }

    /**
     * 1. creates Alfred Bomba `abomba` user (no projections), then adds an account for `abomba`
     * - DN is `abomba`
     * 2. creates Andrej Bomba `abom` user (no projections), then adds an account for `abom`
     * - DN should be `abomba`, iterated to `abomba1`
     * 3. unlinks `abomba` account from `abomba`
     * 4. unlinks `abomba1` account from `abom`
     * 5. re-creates `abomba` account to `abomba`
     *
     * MID-1595, MID-1577
     */
    @Test
    public void test124AddAccountDirectAlreadyExists() throws Exception {
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        when("create abomba with an account");
        repoAddObjectFromFile(USER_ABOMBA_FILENAME, parentResult);

        ObjectDelta<UserType> abombaDelta = createModifyUserAddAccount(USER_ABOMBA_OID, resourceTypeOpenDjrepo.asPrismObject(), "contractor");
        executeChanges(abombaDelta, null, task, parentResult);

        then("create abomba with an account");
        assertUser(USER_ABOMBA_OID, "User before")
                .assertLiveLinks(1);

        String abombaShadowOid = getLiveLinkRefOid(USER_ABOMBA_OID, RESOURCE_OPENDJ_OID);

        ShadowType abombaShadow = repositoryService.getObject(ShadowType.class, abombaShadowOid, null, parentResult)
                .asObjectable();
        assertShadowName(abombaShadow, "uid=abomba,OU=people,DC=example,DC=com");

        when("create abom with an account");
        repoAddObjectFromFile(USER_ABOM_FILENAME, parentResult);
        ObjectDelta<UserType> abomDelta = createModifyUserAddAccount(USER_ABOM_OID, resourceTypeOpenDjrepo.asPrismObject(), "contractor");
        executeChanges(abomDelta, null, task, parentResult);

        then("create abom with an account");
        assertUser(USER_ABOM_OID, "User before")
                .assertLiveLinks(1);

        String abomShadowOid = getLiveLinkRefOid(USER_ABOM_OID, RESOURCE_OPENDJ_OID);

        ShadowType abomShadow = repositoryService.getObject(ShadowType.class, abomShadowOid, null, parentResult)
                .asObjectable();
        assertShadowName(abomShadow, "uid=abomba1,OU=people,DC=example,DC=com");

        when("unlink abomba account by reference");
        ReferenceDelta abombaDeleteAccDelta = prismContext.deltaFactory().reference()
                .createModificationDelete(ShadowType.class,
                        UserType.F_LINK_REF,
                        getPrismContext().itemFactory().createReferenceValue(abombaShadowOid));
        ObjectDelta<UserType> d = prismContext.deltaFactory().object()
                .createModifyDelta(USER_ABOMBA_OID, abombaDeleteAccDelta, UserType.class);
        modelService.executeChanges(MiscSchemaUtil.createCollection(d), null, task, parentResult);

        then("unlink abomba account by reference");
        assertUserNoAccountRef(USER_ABOMBA_OID, parentResult);

        repositoryService.getObject(ShadowType.class, abombaShadowOid, null, parentResult);

        when("unlink abom account by value");
        ReferenceDelta abomDeleteAccDelta = prismContext.deltaFactory().reference()
                .createModificationDelete(ShadowType.class,
                        UserType.F_LINK_REF,
                        abomShadow.asPrismObject());
        ObjectDelta<UserType> d2 = prismContext.deltaFactory().object()
                .createModifyDelta(USER_ABOM_OID, abomDeleteAccDelta, UserType.class);
        // This will delete the account, because we unlink it "by value"
        modelService.executeChanges(MiscSchemaUtil.createCollection(d2), null, task, parentResult);

        then("unlink abom account by value");
        assertUserNoAccountRef(USER_ABOM_OID, parentResult);
        try {
            repositoryService.getObject(ShadowType.class, abomShadowOid, null, parentResult);
            fail("Expected that shadow abom does not exist, but it is");
        } catch (ObjectNotFoundException ex) {
            displayExpectedException(ex);
        }

        when("re-add abomba account");
        OperationResult result = new OperationResult("Add account already exist result.");
        ObjectDelta<UserType> abombaDelta2 = createModifyUserAddAccount(USER_ABOMBA_OID, resourceTypeOpenDjrepo.asPrismObject(), "contractor");
        executeChanges(abombaDelta2, null, task, parentResult);

        then("re-add abomba account");
        String abombaOid2 = assertUserOneLinkRef(USER_ABOMBA_OID);
        ShadowType abombaShadow2 = repositoryService.getObject(ShadowType.class, abombaOid2, null, result).asObjectable();
        assertShadowName(abombaShadow2,
                "uid=abomba,OU=people,DC=example,DC=com");

        result.computeStatus();

        display("Second execution request for abomba", result);
    }

    /**
     * 1. creates an account and a user in repo only - nothing on resource, and these two are not linked
     * 2. deletes the account via model (does not exist on resource)
     * 3. checks that there's a dead account that gets deleted on refresh
     */
    @Test
    public void test130DeleteObjectNotFound() throws Exception {
        OperationResult parentResult = createOperationResult();

        repoAddShadowFromFile(ACCOUNT_GUYBRUSH_FILE, parentResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, parentResult);

        Task task = taskManager.createTaskInstance();
        ObjectDelta<ShadowType> deleteDelta =
                prismContext.deltaFactory().object().createDeleteDelta(ShadowType.class, ACCOUNT_GUYBRUSH_OID);

        executeChanges(deleteDelta, null, task, parentResult);
        parentResult.computeStatus();

        then();
        checkTest130DeadShadow(task, parentResult);

        assertNoRepoShadow(ACCOUNT_GUYBRUSH_OID);

        clock.resetOverride();

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        UserAsserter.forUser(userAfter)
                .assertLiveLinks(0);

        repositoryService.deleteObject(UserType.class, USER_GUYBRUSH_OID, parentResult);
    }

    protected void checkTest130DeadShadow(Task task, OperationResult parentResult) throws CommonException {
        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_GUYBRUSH_OID);
        ShadowAsserter.forShadow(shadowRepo)
                .assertTombstone()
                .assertDead()
                .assertIsNotExists();

        clockForward("PT20M");

        then();
        provisioningService.refreshShadow(shadowRepo, null, task, parentResult);
    }

    /**
     * Modify account not found => reaction: Delete account
     * <p>
     * no assignment - only linkRef to non existent account
     */
    @Test
    public void test140ModifyObjectNotFoundLinkedAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        repoAddShadowFromFile(ACCOUNT_GUYBRUSH_FILE, result);
        repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, result);

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        UserAsserter.forUser(userBefore)
                .assertLiveLinks(1)
                .links()
                .link(ACCOUNT_GUYBRUSH_OID);

        when();
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .property(attributePath("roomNumber")).replace("cabin")
                .asObjectDelta(ACCOUNT_GUYBRUSH_OID);

        try {
            executeChanges(delta, null, task, result);
            fail("Unexpected success while modifying non-existing object");
        } catch (ObjectNotFoundException e) {
            displayExpectedException(e);
        } catch (Throwable e) {
            fail("Unexpected exception during modifying object, " + e.getMessage());
        }

        then();
        checkTest140DeadShadow(task, result);

        assertNoRepoShadow(ACCOUNT_GUYBRUSH_OID);

        clock.resetOverride();
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        UserAsserter.forUser(userAfter)
                .assertLiveLinks(0);
    }

    protected void checkTest140DeadShadow(Task task, OperationResult result) throws CommonException {
        PrismObject<ShadowType> shadowAfter = getShadowRepoLegacy(ACCOUNT_GUYBRUSH_OID);
        ShadowAsserter.forShadow(shadowAfter)
                .assertTombstone()
                .assertDead()
                .assertIsNotExists();

        clockForward("PT20M");
        provisioningService.refreshShadow(shadowAfter, null, task, result);
    }

    /**
     * Modify account not found => reaction: Re-create account, apply changes.
     * <p>
     * assignment with non-existent account
     */
    @Test
    public void test142ModifyObjectNotFoundAssignedAccount() throws Exception {
        given();
        OperationResult parentResult = createOperationResult();

        repoAddShadowFromFile(ACCOUNT_GUYBRUSH_MODIFY_DELETE_FILE, parentResult);
        repoAddObjectFromFile(USER_GUYBRUSH_NOT_FOUND_FILENAME, parentResult);

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_NOT_FOUND_OID);
        UserAsserter.forUser(userBefore)
                .assertLiveLinks(1)
                .links()
                .link(ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID);

        Task task = taskManager.createTaskInstance();

        when();
        ObjectDelta<ShadowType> delta = prismContext.deltaFor(ShadowType.class)
                .property(attributePath("roomNumber")).replace("cabin")
                .property(attributePath("businessCategory")).add("capsize", "fighting")
                .asObjectDelta(ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID);

        try {
            executeChanges(delta, null, task, parentResult);
            fail("Unexpected existence of account");
        } catch (ObjectNotFoundException e) {
            //expected, because we requested the direct modification on non-existing account
            // if the modification is indirect (let's say on focus, the error should not occur)
        }

        then();
        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_NOT_FOUND_OID);
        UserAsserter.forUser(userAfter)
                .assertLiveLinks(1);

        String newShadowOid = getLiveLinkRefOid(USER_GUYBRUSH_NOT_FOUND_OID, RESOURCE_OPENDJ_OID);

        assertThat(newShadowOid)
                .withFailMessage("Unexpected that new and old shadows have the same oid")
                .isNotEqualTo(ACCOUNT_GUYBRUSH_MODIFY_DELETE_OID);

        PrismObject<ShadowType> accountAfter = getShadowModel(newShadowOid);
        assertNotNull(accountAfter);
        display("Modified shadow", accountAfter);

        // The account does not contain does not contain these modifications. It was re-created anew.

//        ShadowAsserter.forShadow(accountAfter)
//                .assertName("uid=guybrush123,ou=people,dc=example,dc=com")
//                .attributes()
//                    .assertValue(new ItemName(MidPointConstants.NS_RI, "roomNumber"),"cabin")
//                    .assertValue(new ItemName(MidPointConstants.NS_RI, "businessCategory"), "capsize", "fighting")
//                .end();
    }

    /**
     * `hector` has an account assigned
     *
     * Get account not found => reaction: Re-create account, return re-created.
     */
    @Test
    public void test144GetObjectNotFoundAssignedAccount() throws Exception {
        given();
        OperationResult parentResult = createOperationResult();

        repoAddShadowFromFile(ACCOUNT_HECTOR_FILE, parentResult);
        repoAddObjectFromFile(USER_HECTOR_NOT_FOUND_FILENAME, parentResult);

        PrismObject<UserType> userBefore = getUser(USER_HECTOR_NOT_FOUND_OID);
        UserAsserter.forUser(userBefore)
                .assertLiveLinks(1)
                .links()
                .link(ACCOUNT_HECTOR_OID);

        Task task = taskManager.createTaskInstance();

        when();
        modelService.getObject(UserType.class, USER_HECTOR_NOT_FOUND_OID, null, task, parentResult);

        then();
        PrismObject<UserType> userAfter = getUser(USER_HECTOR_NOT_FOUND_OID);
        UserAsserter.forUser(userAfter)
                .assertLiveLinks(1)
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
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assignAccountToUser(USER_GUYBRUSH_OID, RESOURCE_OPENDJ_OID, null);

        String liveLinkOidBefore = assertUserBefore(USER_GUYBRUSH_OID)
                .links()
                .singleLive()
                .getOid();

        PrismObject<ShadowType> shadowBefore = getShadowModel(liveLinkOidBefore);
        display("Model Shadow before", shadowBefore);

        String dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        openDJController.delete(dn);

        PrismObject<ShadowType> repoShadowBefore = getShadowRepoLegacy(liveLinkOidBefore);
        assertNotNull("Repo shadow is gone!", repoShadowBefore);
        display("Repository shadow before", repoShadowBefore);
        assertThat(repoShadowBefore.asObjectable().isDead())
                .withFailMessage("Oh my! Shadow is dead!")
                .isNotEqualTo(Boolean.TRUE);

        invalidateShadowCacheIfNeeded(RESOURCE_OPENDJ_OID);

        when();
        recomputeUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);

        // @formatter:off
        String liveLinkOidAfter = assertUser(userAfter, "after")
                .links()
                    .assertLinks(1, isReaper() ? 0 : 1)
                    .singleLive()
                        .resolveTarget()
                        .display("live shadow after")
                        .getOid();
        // @formatter:on

        assertThat(liveLinkOidBefore).withFailMessage("Old and new shadow with the same oid?")
                .isNotEqualTo(liveLinkOidAfter);

        Entry entryAfter = openDJController.fetchEntry(dn);
        display("Entry after", entryAfter);
    }

    /**
     * Recompute user => account not found => reaction: Re-create account
     * MID-3093
     */
    @Test
    public void test152RecomputeUserAccountAndShadowNotFound() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_GUYBRUSH_OID);
        display("User before", userBefore);
//        UserAsserter.forUser(userBefore)
//                .links()
//                .assertLinks(1, isReaper() ? 0 : 1);

        String dn = null;
        String liveOidBefore = null;
        for (ObjectReferenceType linkRef : userBefore.asObjectable().getLinkRef()) {
            String shadowOidBefore = linkRef.getOid();
            PrismObject<ShadowType> shadowBefore = getShadowModel(shadowOidBefore);
            display("Shadow before", shadowBefore);

            if (dn == null) {
                dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
                if (dn != null) {
                    openDJController.delete(dn);
                }
            }

            if (ShadowUtil.isNotDead(linkRef)) {
                liveOidBefore = linkRef.getOid();
            }

            repositoryService.deleteObject(ShadowType.class, shadowOidBefore, result);
        }

        assertThat(liveOidBefore).isNotNull();
        assertThat(dn).isNotNull();

        when();
        recomputeUser(USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_GUYBRUSH_OID);
        display("User after", userAfter);
        String liveOidAfter = assertUser(userAfter, "after")
                .links()
                .singleLive()
                .resolveTarget()
                .display()
                .getOid();

        assertThat(liveOidBefore)
                .withFailMessage("Old and new shadow with the same oid?")
                .isNotEqualTo(liveOidAfter);

        Entry entryAfter = openDJController.fetchEntry(dn);
        display("Entry after", entryAfter);
        assertNotNull("No LDAP entry for " + dn + " found", entryAfter);
    }

    /**
     * 1. `guybrush` has 1 account.
     * 2. we delete the LDAP account manually
     * 3. then we delete the user via model
     */
    @Test
    public void test159DeleteUserGuybrush() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserAsserter<Void> asserter = assertUserBefore(USER_GUYBRUSH_OID);
        asserter.setObjectResolver(createSimpleModelObjectResolver());

        // @formatter:off
        PrismObject<ShadowType> shadowBefore = asserter
                .links()
                    .assertLiveLinks(1)
                    .singleLive()
                        .resolveTarget()
                            .display()
                            .getObject();
        // @formatter:on

        String dn = ShadowUtil.getAttributeValue(shadowBefore, RESOURCE_OPENDJ_SECONDARY_IDENTIFIER);
        openDJController.delete(dn);

        when();
        deleteObject(UserType.class, USER_GUYBRUSH_OID, task, result);

        then();
        assertSuccess(result);

        assertNoObject(UserType.class, USER_GUYBRUSH_OID, task, result);

        // TODO: assert no shadow
        // TODO: assert no entry
    }

    @Test
    public void test200StopOpenDj() {
        openDJController.stop();

        assertFalse("Resource is running", EmbeddedUtils.isRunning());
    }

    /**
     * Creates an assignment while the resource is down.
     * Shadow with pending ADD operation should be created.
     */
    @Test
    public void test210AddObjectCommunicationProblem() throws Exception {
        given();
        openDJController.assumeStopped();
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        repoAddObjectFromFile(USER_E_FILENAME, parentResult);

        assertUser(USER_E_OID, getTestNameShort())
                .assertLiveLinks(0);

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        assignAccount(UserType.class, USER_E_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        parentResult.computeStatus();
        display("add object communication problem result: ", parentResult);
        assertEquals("Expected handled error but got: " + parentResult.getStatus(), OperationResultStatus.IN_PROGRESS, parentResult.getStatus());

        assertUser(USER_E_OID, "after")
                .assertLiveLinks(1);

        String shadowEOid = getLiveLinkRefOid(USER_E_OID, RESOURCE_OPENDJ_OID);

        // @formatter:off
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
                    .assertAttributesCachingAware(QNAME_DN, QNAME_UID)
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
        // @formatter:on
    }

    /**
     * Modifies the shadow while resource is down. A pending MODIFY should be added.
     */
    @Test
    public void test212AddModifyObjectCommunicationProblem() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        assertUser(USER_E_OID, "User before")
                .assertLiveLinks(1);

        String accountOid = getLiveLinkRefOid(USER_E_OID, RESOURCE_OPENDJ_OID);

        //WHEN
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory()
                .object()
                .createModificationAddProperty(ShadowType.class, accountOid, attributePath(QNAME_EMPLOYEE_NUMBER), "emp4321");
        delta.addModificationReplaceProperty(attributePath(QNAME_GIVEN_NAME), "eeeee");

        when();
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(delta, ModelExecuteOptions.create().reconcile(), task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        then();

        // @formatter:off
        assertRepoShadow(accountOid)
                .display(getTestNameShort() + "Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoPrimaryIdentifierValue()
                .assertNoLegacyConsistency()
                .attributes()
                    .assertAttributesCachingAware(QNAME_DN, QNAME_UID)
                    .end()
                .pendingOperations()
                    .assertOperations(2)
                    .addOperation()
                        .display()
                        .assertType(PendingOperationTypeType.RETRY)
                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                        .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                        .assertAttemptNumber(3) // +1 during initial loading, then +1 when executing the modification
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
        // @formatter:on
    }

    /**
     * Modifies the `jackie` shadow (resource is still down). A single pending MODIFY should be there.
     */
    @Test
    public void test214ModifyObjectCommunicationProblem() throws Exception {
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        String accountOid = assertUserOneLinkRef(USER_JACK_OID);

        Task task = taskManager.createTaskInstance();

        when();
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory()
                .object()
                .createModificationAddProperty(ShadowType.class, accountOid, attributePath(QNAME_EMPLOYEE_NUMBER), "emp4321");
        delta.addModificationReplaceProperty(attributePath(QNAME_GIVEN_NAME), "Jackkk");

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(delta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        then();
        // @formatter:off
        assertModelShadowNoFetch(ACCOUNT_JACKIE_OID)
                .display(getTestNameShort() + ".Shadow after")
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
                    .assertValue(QNAME_GIVEN_NAME, "Jackkk")
                    .assertValue(QNAME_EMPLOYEE_NUMBER, "emp4321")
                .end()
                .assertIsExists();
        // @formatter:on
    }

    /**
     * Deletes `deniels` account (user + shadow exists in repo) while the resource is down.
     */
    @Test
    public void test220DeleteObjectCommunicationProblem() throws Exception {
        final String testName = getTestNameShort();

        // GIVEN
        openDJController.assumeStopped();
        Task task = taskManager.createTaskInstance(testName);
        OperationResult parentResult = task.getResult();

        assertUserOneLinkRef(USER_DENIELS_OID);

        when();
        ObjectDelta<ShadowType> deleteDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, ACCOUNT_DENIELS_OID);

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(deleteDelta, null, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        assertUser(USER_DENIELS_OID, testName)
                .assertLiveLinks(1);

        then();
        // @formatter:off
        assertModelShadowNoFetch(ACCOUNT_DENIELS_OID)
                .display(testName + "Shadow after")
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
        // @formatter:off

//        clockForward("PT20M");
//        assertNoModelShadowFuture(ACCOUNT_DENIELS_OID);
//        clock.resetOverride();
    }

    /**
     * Tries to get freshly deleted `deniels` account via model
     */
    @Test
    public void test230GetAccountCommunicationProblem() throws Exception {
        final String testName = getTestNameShort();
        Task task = getTestTask();

        // GIVEN
        openDJController.assumeStopped();
        OperationResult result = createOperationResult();

        ShadowType account = modelService.getObject(ShadowType.class, ACCOUNT_DENIELS_OID,
                null, task, result).asObjectable();
        assertNotNull("Get method returned null account.", account);
        assertNotNull("Fetch result was not set in the shadow.", account.getFetchResult());

        // @formatter:off
        assertModelShadowNoFetch(ACCOUNT_DENIELS_OID)
                .display(testName + "Shadow after")
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
        // @formatter:on

//        clockForward("PT20M");
//        assertNoModelShadowFuture(ACCOUNT_DENIELS_OID);
//        clock.resetOverride();
    }

    /**
     * 1. creates LDAP account directly (no shadow)
     * 2. creates a user assignment when LDAP resource is down
     * 3. a shadow with pending ADD operation should be created
     *
     * The resolution of this situation is during the final reconciliation in {@link #test800Reconciliation()}.
     */
    @Test
    public void test240AddObjectCommunicationProblemAlreadyExists() throws Exception {
        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        openDJController.addEntryFromLdifFile(LDIF_ELAINE_FILE);
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
                .assertLiveLinks(0);

        Task task = taskManager.createTaskInstance();

        when("assign account on OpenDJ");
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        assignAccount(UserType.class, USER_ELAINE_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        parentResult.computeStatus();
        assertInProgress(parentResult);
        assertMessageContains(parentResult, "Connection failed");

        then("assign account on OpenDJ");
        assertUser(USER_ELAINE_OID, "User after")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_ELAINE_OID, RESOURCE_OPENDJ_OID);

        // @formatter:off
        assertRepoShadow(shadowOid)
                .display(getTestNameShort() + ".Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoPrimaryIdentifierValue()
                .assertNoLegacyConsistency()
                .attributes()
                    .assertAttributesCachingAware(QNAME_DN, QNAME_UID)
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
        // @formatter:on

        //check futurized
    }

    /**
     * 1. Modifies user `jackie` while resource is down.
     * 2. Checks pending MODIFY + futurized shadow.
     */
    @Test
    public void test250ModifyFocusCommunicationProblem() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        assertUser(USER_JACK_OID, "User before")
                .assertLiveLinks(1);

        Collection<PropertyDelta<?>> modifications = new ArrayList<>();

        PropertyDelta<?> fullNameDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_FULL_NAME, getUserDefinition(), new PolyString("jackNew2"));
        modifications.add(fullNameDelta);

        PropertyDelta<ActivationStatusType> enabledDelta = prismContext.deltaFactory()
                .property()
                .createModificationAddProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, getUserDefinition(), ActivationStatusType.ENABLED);
        modifications.add(enabledDelta);

        ObjectDelta<UserType> objectDelta = prismContext.deltaFactory()
                .object()
                .createModifyDelta(USER_JACKIE_OID, modifications, UserType.class);
        Task task = taskManager.createTaskInstance();

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        executeChanges(objectDelta, ModelExecuteOptions.create().pushChanges(), task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        parentResult.computeStatus();
        assertInProgress(parentResult);
        assertMessageContains(parentResult, "Connection failed");

        assertUser(USER_JACKIE_OID, "User after first modify")
                .assertLiveLinks(1)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertFullName("jackNew2");

        String shadowOid = getLiveLinkRefOid(USER_JACKIE_OID, RESOURCE_OPENDJ_OID);

        // @formatter:off
        assertModelShadowNoFetch(shadowOid)
                .display(getTestNameShort() + "Shadow after")
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
        // @formatter:on

        assertModelShadowFutureNoFetch(shadowOid)
                .display()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED) // MID-6420
                .attributes()
                .assertValue(QNAME_CN, "jackNew2")
                .end()
                .assertIsExists()
                .end();
    }

    /**
     * 1. Modifies user `jackie` again - while resource is down.
     * 2. Checks pending MODIFY + futurized shadow.
     */
    @Test
    public void test251ModifyFocusCommunicationProblemSecondTime() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        assertUser(USER_JACKIE_OID, "User before")
                .assertLiveLinks(1)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertFullName("jackNew2");

        Collection<PropertyDelta<?>> modifications = new ArrayList<>();

        PropertyDelta<?> fullNameDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_FULL_NAME, getUserDefinition(), new PolyString("jackNew2a"));
        modifications.add(fullNameDelta);

        PropertyDelta<?> givenNameDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_GIVEN_NAME, getUserDefinition(), new PolyString("jackNew2a"));
        modifications.add(givenNameDelta);

        PropertyDelta<ActivationStatusType> enabledDelta = prismContext.deltaFactory()
                .property()
                .createModificationAddProperty(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, getUserDefinition(), ActivationStatusType.ENABLED);
        modifications.add(enabledDelta);

        ObjectDelta<UserType> objectDelta = prismContext.deltaFactory()
                .object()
                .createModifyDelta(USER_JACKIE_OID, modifications, UserType.class);
        Task task = taskManager.createTaskInstance();

        executeChanges(objectDelta, ModelExecuteOptions.create().pushChanges(), task, parentResult);

        parentResult.computeStatus();
        assertInProgress(parentResult);
        assertSubresultMessageContains(parentResult, "Connection failed");

        assertUser(USER_JACKIE_OID, "User after first modify")
                .assertLiveLinks(1)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .assertFullName("jackNew2a")
                .assertGivenName("jackNew2a");

        String shadowOid = getLiveLinkRefOid(USER_JACKIE_OID, RESOURCE_OPENDJ_OID);

        // @formatter:off
        assertModelShadowNoFetch(shadowOid)
                .display(getTestNameShort() + "Shadow after")
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
                .assertAdministrativeStatus(ActivationStatusType.ENABLED) // MID-6420
                .attributes()
                    .assertValue(QNAME_CN, "jackNew2a")
                    .assertValue(QNAME_GIVEN_NAME, "jackNew2a")
                .end()
                .assertIsExists();
        // @formatter:on
    }

    /**
     * this test simulates situation, when someone tries to add account while
     * resource is down and this account is created by next get call on this
     * account
     */
    @Test
    public void test260GetDiscoveryAddCommunicationProblem() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        display("OpenDJ stopped");
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_ANGELIKA_FILENAME, parentResult);

        assertUser(USER_ANGELIKA_OID, "User before")
                .assertLiveLinks(0);

        Task task = taskManager.createTaskInstance();

        when();

        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        assignAccount(UserType.class, USER_ANGELIKA_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL, task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        then();
        parentResult.computeStatus();
        assertInProgress(parentResult);
        assertMessageContains(parentResult, "Connection failed");

        assertUser(USER_ANGELIKA_OID, "User after")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_ANGELIKA_OID, RESOURCE_OPENDJ_OID);
        // @formatter:off
        assertRepoShadow(shadowOid)
                .display(getTestNameShort() + ".Shadow after")
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIsNotExists()
                .assertNotDead()
                // We have name (secondary identifier), but we do not have primary identifier yet.
                // We will get that only when create operation is successful.
                .assertNoPrimaryIdentifierValue()
                .assertNoLegacyConsistency()
                .attributes()
                    .assertAttributesCachingAware(QNAME_DN, QNAME_UID)
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
        // @formatter:on
        PrismObject<ShadowType> repoShadow = getShadowRepoLegacy(shadowOid);
        assertNotNull("Shadow doesn't have metadata",
                ValueMetadataTypeUtil.getCreateTimestamp(repoShadow.asObjectable()));

        //start openDJ
        openDJController.start();
        //and set the resource availability status to UP
        TestUtil.info("OpenDJ started, resource UP");

        getShadowModel(shadowOid, GetOperationOptions.createForceRetry(), true);
        //TODO more asserts
    }

    /**
     * 1. modifies an account (while resource is down)
     * - checks pending MODIFY operation + futurized shadow
     * 2. gets an account while resource is up (force retry)
     * - checks that the pending operation is complete
     */
    @Test
    public void test262GetDiscoveryModifyCommunicationProblem() throws Exception {
        // GIVEN
        openDJController.assumeRunning();
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        //prepare user
        repoAddObjectFromFile(USER_ALICE_FILENAME, parentResult);
        assertUser(USER_ALICE_OID, "User before")
                .assertLiveLinks(0);

        //and add account to the user while resource is UP
        assignAccountToUser(USER_ALICE_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL);

        //then stop openDJ
        openDJController.stop();

        assertUser(USER_ALICE_OID, "User after")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_ALICE_OID, RESOURCE_OPENDJ_OID);

        //and make some modifications to the account while resource is DOWN
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        modifyAccountShadowReplace(shadowOid, attributePath(QNAME_EMPLOYEE_NUMBER), task, parentResult, "44332");
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        // @formatter:off
        assertModelShadowNoFetch(shadowOid)
                .display(getTestNameShort() + "Shadow after")
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
                            .assertHasModification(attributePath(QNAME_EMPLOYEE_NUMBER));
        // @formatter:on

        //start openDJ
        openDJController.start();
        //and set the resource availability status to UP
        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);

        assertModelShadowFuture(shadowOid)
                .attributes()
                .assertValue(QNAME_EMPLOYEE_NUMBER, "44332");

        //and then try to get account -> result is that the modifications will be applied to the account
        XMLGregorianCalendar lastAttemptStartTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<ShadowType> shadowAfter = getShadowModel(shadowOid, GetOperationOptions.createForceRetry(), true);
        XMLGregorianCalendar lastAttemptEndTs = clock.currentTimeXMLGregorianCalendar();

        // @formatter:off
        ShadowAsserter.forShadow(shadowAfter)
                .attributes()
                    .assertValue(QNAME_EMPLOYEE_NUMBER, "44332")
                .end()
                .pendingOperations()
                    .singleOperation()
                        .display()
                        .assertType(PendingOperationTypeType.RETRY)
                        .assertRequestTimestamp(lastRequestStartTs, lastRequestEndTs)
                        .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED) // The operation was completed above!
                        .assertResultStatus(OperationResultStatusType.SUCCESS)
                        .assertOperationStartTimestamp(lastRequestStartTs, lastRequestEndTs)
                        .assertAttemptNumber(2)
                        .assertLastAttemptTimestamp(lastAttemptStartTs, lastAttemptEndTs)
                        .delta()
                            .display()
                            .assertModify()
                            .assertHasModification(attributePath(QNAME_EMPLOYEE_NUMBER));
        // @formatter:on
    }

    /**
     * Similar to {@link #test262GetDiscoveryModifyCommunicationProblem()} but this time modifying the password on the user
     * (instead of modifying a regular attribute on shadow).
     */
    @Test
    public void test264GetDiscoveryModifyUserPasswordCommunicationProblem() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        assertUser(USER_ALICE_OID, "User before")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_ALICE_OID, RESOURCE_OPENDJ_OID);
        // WHEN (down)
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        modifyUserChangePassword(USER_ALICE_OID, "DEADmenTELLnoTALES", task, parentResult);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        // THEN
        //check the state after execution

        // @formatter:off
        assertModelShadowNoFetch(shadowOid)
                .display(getTestNameShort() + ".Shadow after")
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
                    .by().executionStatus(PendingOperationExecutionStatusType.EXECUTING).find()
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
                    .by().executionStatus(PendingOperationExecutionStatusType.COMPLETED).find()
                        .display()
                        .assertType(PendingOperationTypeType.RETRY)
                        .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                        .assertResultStatus(OperationResultStatusType.SUCCESS)
                        .assertAttemptNumber(2)
                        .delta()
                            .display()
                            .assertModify()
                            .assertHasModification(attributePath(QNAME_EMPLOYEE_NUMBER));
        // @formatter:on

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
                .assertValue(QNAME_EMPLOYEE_NUMBER, "44332")
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
        // GIVEN
        openDJController.assumeStopped();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertUser(USER_ALICE_OID, "User before")
                .assertLiveLinks(1);
        String shadowOid = getLiveLinkRefOid(USER_ALICE_OID, RESOURCE_OPENDJ_OID);

        // WHEN (down)
        when();
        XMLGregorianCalendar lastRequestStartTs = clock.currentTimeXMLGregorianCalendar();
        modifyUserChangePassword(USER_ALICE_OID, "UNDEADmenTELLscaryTALES", task, result);
        XMLGregorianCalendar lastRequestEndTs = clock.currentTimeXMLGregorianCalendar();

        // THEN
        then();
        //check the state after execution

        // @formatter:off
        assertModelShadowNoFetch(shadowOid)
                .display(getTestNameShort() + ".Shadow after")
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
                    .by().executionStatus(PendingOperationExecutionStatusType.EXECUTING).find()
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
        // @formatter:on

        //start openDJ
        openDJController.start();
        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, result);

        // WHEN (restore)
        when();
        invalidateShadowCacheIfNeeded(RESOURCE_OPENDJ_OID); // this is needed to trigger shadow fetch operation
        reconcileUser(USER_ALICE_OID, task, result);

        // THEN
        then();
        openDJController.assertPassword(aliceAccountDn, "UNDEADmenTELLscaryTALES");

        assertUser(USER_ALICE_OID, "User after")
                .assertPassword("UNDEADmenTELLscaryTALES");
    }

    /**
     * This test simulates situation when someone tries to add account while
     * resource is down and this account is created by next modify call on the owning user.
     */
    @Test
    public void test270ModifyDiscoveryAddCommunicationProblem() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        // WHEN
        repoAddObjectFromFile(USER_BOB_NO_GIVEN_NAME_FILENAME, parentResult);

        assertUser(USER_BOB_NO_GIVEN_NAME_OID, "User bofore")
                .assertLiveLinks(0);

        Task task = taskManager.createTaskInstance();

        assignAccount(UserType.class, USER_BOB_NO_GIVEN_NAME_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL, task, parentResult);

        parentResult.computeStatus();
        assertInProgress(parentResult);
        assertMessageContains(parentResult, "Connection failed");

        assertUser(USER_BOB_NO_GIVEN_NAME_OID, "User after")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_BOB_NO_GIVEN_NAME_OID, RESOURCE_OPENDJ_OID);

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

        // @formatter:off
        assertModelShadow(shadowOid)
                .assertIsExists()
                .assertNotDead()
                .assertResource(RESOURCE_OPENDJ_OID)
                .attributes()
                    .assertValue(QNAME_GIVEN_NAME, "Bob")
                    .assertValue(QNAME_UID, "bob")
                    .assertValue(QNAME_CN, "Bob Dylan")
                    .assertValue(QNAME_SN, "Dylan");
        // @formatter:on
    }

    /**
     * Modifies the `subtype` property that has a weak mapping onto `ri:employeeType` (while the resource is down).
     * There should be no pending changes because the employeeType already has a value - but we don't know this.
     */
    @Test
    public void test280ModifyObjectCommunicationProblemWeakMapping() throws Exception {
        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_JOHN_WEAK_FILENAME, parentResult);
        assertUser(USER_JOHN_WEAK_OID, "User before")
                .assertLiveLinks(0);

        Task task = taskManager.createTaskInstance();

        assignAccountToUser(USER_JOHN_WEAK_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL);

        assertUser(USER_JOHN_WEAK_OID, "User after")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_JOHN_WEAK_OID, RESOURCE_OPENDJ_OID);
        // @formatter:off
        assertModelShadow(shadowOid)
                .assertIsExists()
                .assertNotDead()
                .attributes()
                    .assertValue(QNAME_UID, "john")
                    .assertValue(QNAME_SN, "weak")
                    .assertValue(QNAME_GIVEN_NAME, "john")
                    .assertValue(QNAME_CN, "john weak")
                    .assertValue(QNAME_EMPLOYEE_TYPE, "manager");
        // @formatter:on

        //stop opendj and try to modify subtype (weak mapping)
        openDJController.stop();

        logger.info("start modifying user - account with weak mapping after stopping opendj.");

        modifyUserReplace(USER_JOHN_WEAK_OID, UserType.F_SUBTYPE, task, parentResult, "boss");

        assertModelShadowFutureNoFetch(shadowOid)
                .pendingOperations()
                .assertOperations(0);

//        // TODO: [RS] not 100% sure about this. But if you do not expect an error you should not set doNotDiscovery. Server is still not running.
//        checkNormalizedShadowBasic(accountOid, "john", true, null, task, parentResult);
//        checkNormalizedShadowBasic(accountOid, "john", true, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), task, parentResult);
    }

    /**
     * Changes employeeType (weak mapping) and givenName (normal mapping) while the resource is down.
     */
    @Test
    public void test282ModifyObjectCommunicationProblemWeakAndStrongMapping() throws Exception {
        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_DONALD_FILENAME, parentResult);

        assertUser(USER_DONALD_OID, "User before")
                .assertLiveLinks(0);

        Task task = taskManager.createTaskInstance();

        assignAccount(UserType.class, USER_DONALD_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL);

        assertUser(USER_DONALD_OID, "User after")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_DONALD_OID, RESOURCE_OPENDJ_OID);

        // @formatter:off
        assertModelShadow(shadowOid)
                .assertIsExists()
                .assertNotDead()
                .attributes()
                    .assertValue(QNAME_UID, "donald")
                    .assertValue(QNAME_SN, "trump")
                    .assertValue(QNAME_GIVEN_NAME, "donald")
                    .assertValue(QNAME_CN, "donald trump")
                    .assertValue(QNAME_EMPLOYEE_TYPE, "manager");
        // @formatter:on

        //stop opendj and try to modify employeeType (weak mapping)
        openDJController.stop();

        logger.info("start modifying user - account with weak mapping after stopping opendj.");

        PropertyDelta<String> employeeTypeDelta = prismContext.deltaFactory()
                .property()
                .createModificationReplaceProperty(UserType.F_SUBTYPE, getUserDefinition(), "boss");

        ObjectDelta<UserType> userDelta = prismContext.deltaFactory()
                .object()
                .createModificationReplaceProperty(UserType.class, USER_DONALD_OID, UserType.F_GIVEN_NAME, new PolyString("don"));
        userDelta.addModification(employeeTypeDelta);

        executeChanges(userDelta, null, task, parentResult);

        // @formatter:off
        assertModelShadowNoFetch(shadowOid)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .pendingOperations()
                    .singleOperation()
                    .display()
                    .assertAttemptNumber(1)
                    .delta()
                        .assertModify()
                        .assertHasModification(attributePath(QNAME_GIVEN_NAME))
                        .assertNoModification(attributePath(QNAME_EMPLOYEE_TYPE));
        // @formatter:on
    }

    @Test
    public void test283GetObjectNoFetchShadowAndRecompute() throws Exception {
        // GIVEN
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        assertUser(USER_DONALD_OID, "User before")
                .assertLiveLinks(1);
        String shadowOid = getLiveLinkRefOid(USER_DONALD_OID, RESOURCE_OPENDJ_OID);

        Task task = taskManager.createTaskInstance();

        // @formatter:off
        assertModelShadowNoFetch(shadowOid)
                .assertNotDead()
                .assertNoLegacyConsistency()
                .pendingOperations()
                    .singleOperation()
                    .display()
                    .assertAttemptNumber(1)
                    .delta()
                        .assertModify()
                        .assertHasModification(attributePath(QNAME_GIVEN_NAME))
                        .assertNoModification(attributePath(QNAME_EMPLOYEE_TYPE));
        // @formatter:on

        when();
        invalidateShadowCacheIfNeeded(RESOURCE_OPENDJ_OID); // this is needed to trigger shadow fetch operation
        recomputeUser(USER_DONALD_OID, executeOptions().reconcile(), task, parentResult);

        then();
        assertModelShadow(shadowOid)
                .attributes()
                .assertValue(QNAME_GIVEN_NAME, "don")
                .assertValue(QNAME_EMPLOYEE_TYPE, "manager");
    }

    @Test
    public void test284ModifyObjectAssignToGroupCommunicationProblem() throws Exception {
        Task task = taskManager.createTaskInstance();
        OperationResult parentResult = createOperationResult();
        // GIVEN
        openDJController.addEntriesFromLdifFile(LDIF_CREATE_ADMINS_GROUP_FILE);

        ObjectQuery filter = ObjectQueryUtil.createResourceAndObjectClassQuery(RESOURCE_OPENDJ_OID, RESOURCE_OPENDJ_GROUP_OBJECTCLASS);
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, filter, null, task, parentResult);

        for (PrismObject<ShadowType> shadow : shadows) {
            logger.info("SHADOW ===> {}", shadow.debugDump());
        }

        // WHEN
        openDJController.assumeStopped();

        assertUser(USER_DONALD_OID, "User before")
                .assertLiveLinks(1);

        String shadowOid = getLiveLinkRefOid(USER_DONALD_OID, RESOURCE_OPENDJ_OID);

        // WHEN
        when();

        AssignmentType adminRoleAssignment = new AssignmentType();
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
                .assertIntent(INTENT_INTERNAL)
                .assertKind()
                .pendingOperations()
                .by()
                .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .find()
                .display()
                .assertType(PendingOperationTypeType.RETRY)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .delta()
                .assertNoModification(attributePath(QNAME_EMPLOYEE_TYPE))
                .assertHasModification(ShadowType.F_ASSOCIATIONS.append(toRiQName("group")));

        //THEN
        openDJController.assumeRunning();

        // MID-8327
        recomputeUser(USER_DONALD_OID, null, task, parentResult);

        // To have the real effects (checked below)
        invalidateShadowCacheIfNeeded(RESOURCE_OPENDJ_OID); // this is needed to trigger shadow fetch operation
        recomputeUser(USER_DONALD_OID, executeOptions().reconcile(), task, parentResult);

        assertModelShadow(shadowOid)
                .attributes()
                .assertValue(QNAME_GIVEN_NAME, "donalld")
                .assertValue(QNAME_EMPLOYEE_TYPE, "manager");

        openDJController.assertUniqueMember(ROLE_LDAP_ADMINS_DN, ACCOUNT_DONALD_LDAP_DN);
        //TODO: check on user if it was processed successfully (add this check also to previous (30) test..
    }

    @Test
    public void test300AddAccountTrainee() throws Exception {
        given();
        openDJController.assumeRunning();

        when();
        addObject(new File(USER_TRAINEE_FILENAME));

        then();
        PrismObject<UserType> userAfter = getUser(USER_TRAINEE_OID);
        assertUser(userAfter, "After")
                .assertName("trainee")
                .assertLiveLinks(1);
    }

    //MID-6742
    @Test
    public void test310ModifyAccountTraineeCommunicationProblem() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.assumeStopped();

        when();
        modifyObjectReplaceProperty(UserType.class, USER_TRAINEE_OID, UserType.F_NAME,
                ModelExecuteOptions.create().reconcile(), task, result, PolyString.fromOrig("trainee01"));

        then();
//        assertResultStatus(result, OperationResultStatus.IN_PROGRESS);
        PrismObject<UserType> userAfter = getUser(USER_TRAINEE_OID);
        assertUser(userAfter, " After ").assertName("trainee01");

        String accountOid = assertOneLinkRef(userAfter);

        assertRepoShadow(accountOid)
                .hasUnfinishedPendingOperations()
                .pendingOperations()
                .assertOperations(1)
                .modifyOperation()
                .display()
                .assertAttemptNumber(1)
                .delta()
                .assertModify()
                .assertHasModification(ItemPath.create(ShadowType.F_ATTRIBUTES, QNAME_DN));
    }

    //TODO: enable after notify failure will be implemented..
    @Test(enabled = false)
    public void test400GetDiscoveryAddCommunicationProblemAlreadyExists() throws Exception {
        // GIVEN
        openDJController.assumeStopped();
        OperationResult parentResult = createOperationResult();

        repoAddObjectFromFile(USER_DISCOVERY_FILENAME, parentResult);

        assertUserNoAccountRef(USER_DISCOVERY_OID, parentResult);

        Task task = taskManager.createTaskInstance();

        assignAccount(UserType.class, USER_DISCOVERY_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL, task, parentResult);

        parentResult.computeStatus();
        display("add object communication problem result: ", parentResult);
        assertEquals("Expected success but got: " + parentResult.getStatus(), OperationResultStatus.SUCCESS, parentResult.getStatus());

        String accountOid = assertUserOneLinkRef(USER_DISCOVERY_OID);

        openDJController.start();
        assertTrue(EmbeddedUtils.isRunning());

        openDJController.addEntryFromLdifFile(LDIF_DISCOVERY_FILE);
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
        // GIVEN
        openDJController.assumeRunning();
        Task task = getTestTask();
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
        var accountShadow = getShadowRepo(accountOid);
        display("account shadow (repo)", accountShadow);
        assertShadowRepo(accountShadow, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("account shadow (model)", accountModel);
        assertShadowModel(accountModel, accountOid, "uid=morgan,ou=people,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
    }

    /**
     * Adding a user (chuck) that has an OpenDJ assignment. But the equivalent account already exists on
     * OpenDJ and there is also corresponding shadow in the repo. The account should be linked.
     */
    @Test
    public void test501AddUserChuckWithAssignment() throws Exception {
        // GIVEN
        openDJController.assumeRunning();
        Task task = getTestTask();
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

        PrismObject<UserType> userChuck = modelService.getObject(UserType.class, USER_CHUCK_OID, null, task, result);
        display("User chuck after", userChuck);
        UserType userChuckBean = userChuck.asObjectable();
        assertEquals("Unexpected number of accountRefs", 1, userChuckBean.getLinkRef().size());
        ObjectReferenceType accountRef = userChuckBean.getLinkRef().get(0);
        String accountOid = accountRef.getOid();
        assertFalse("No accountRef oid", StringUtils.isBlank(accountOid));
        assertEquals("old oid not used..", accOid, accountOid);

        // Check shadow
        var accountShadow = getShadowRepo(accountOid);
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
        // GIVEN
        openDJController.assumeRunning();
        Task task = getTestTask();
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
        assignAccount(UserType.class, USER_HERMAN_OID, RESOURCE_OPENDJ_OID, INTENT_INTERNAL, task, result);

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
        var accountShadow = getShadowRepo(shadowOidAfter);
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
        // GIVEN
        openDJController.assumeRunning();
        Task task = getTestTask();
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
        //noinspection unchecked
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
            var accountShadow = getShadowRepo(accountOid);
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
     */
    @Test
    public void test511AssignAccountMorgan() throws Exception {
        // GIVEN
        openDJController.assumeRunning();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        //prepare new OU in opendj
        openDJController.addEntryFromLdifFile(LDIF_CREATE_USERS_OU_FILE);

        PrismObject<UserType> user = repositoryService.getObject(UserType.class, USER_MORGAN_OID, null, result);
        display("User Morgan: ", user);

        ExpressionType expression = new ExpressionType();
        ObjectFactory of = new ObjectFactory();
        RawType raw = new RawType(prismContext.xnodeFactory().primitive("uid=morgan,ou=users,dc=example,dc=com").frozen());

        JAXBElement<?> val = of.createValue(raw);
        expression.getExpressionEvaluator().add(val);

        MappingType mapping = new MappingType();
        mapping.setExpression(expression);

        ResourceAttributeDefinitionType attrDefType = new ResourceAttributeDefinitionType();
        attrDefType.setRef(new ItemPathType(ItemPath.create(getOpenDjSecondaryIdentifierQName())));
        attrDefType.setOutbound(mapping);

        ConstructionType construction = new ConstructionType();
        construction.getAttribute().add(attrDefType);
        construction.setResourceRef(ObjectTypeUtil.createObjectRef(resourceTypeOpenDjrepo));

        AssignmentType assignment = new AssignmentType();
        assignment.setConstruction(construction);

        //noinspection unchecked
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

        var accountShadow = getShadowRepo(accountOid);
        assertShadowRepo(accountShadow, accountOid, "uid=morgan,ou=users,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertShadowModel(accountModel, accountOid, "uid=morgan,ou=users,dc=example,dc=com", resourceTypeOpenDjrepo, RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        ShadowSimpleAttribute<?> attributes = ShadowUtil.getSimpleAttribute(accountModel, QNAME_UID);
        assertEquals("morgan", attributes.getAnyRealValue());
        // TODO: check OpenDJ Account
    }

    @Test
    public void test600DeleteUserAlice() throws Exception {
        openDJController.assumeRunning();
        Task task = getTestTask();
        OperationResult parentResult = task.getResult();

        ObjectDelta<UserType> deleteAliceDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(UserType.class, USER_ALICE_OID);

        modelService.executeChanges(MiscSchemaUtil.createCollection(deleteAliceDelta), null, task, parentResult);

        try {
            modelService.getObject(UserType.class, USER_ALICE_OID, null, task, parentResult);
            fail("Expected object not found error, but haven't got one. Something went wrong while deleting user alice");
        } catch (ObjectNotFoundException ex) {
            displayExpectedException(ex);
        }
    }

    @Test
    public void test601GetDiscoveryModifyCommunicationProblemDirectAccount() throws Exception {
        openDJController.assumeRunning();
        OperationResult parentResult = createOperationResult();

        //prepare user
        repoAddObjectFromFile(USER_ALICE_FILENAME, parentResult);

        assertUserNoAccountRef(USER_ALICE_OID, parentResult);

        Task task = taskManager.createTaskInstance();
        //and add account to the user while resource is UP

        //REQUEST_USER_MODIFY_ADD_ACCOUNT_COMMUNICATION_PROBLEM
        ObjectDelta<UserType> addDelta = createModifyUserAddAccount(
                USER_ALICE_OID, resourceTypeOpenDjrepo.asPrismObject(), INTENT_INTERNAL);
        executeChanges(addDelta, null, task, parentResult);

        //then stop openDJ
        openDJController.stop();

        String accountOid = assertUserOneLinkRef(USER_ALICE_OID);

        //and make some modifications to the account while resource is DOWN
        //WHEN
        ObjectDelta<ShadowType> delta = prismContext.deltaFactory()
                .object()
                .createModificationAddProperty(ShadowType.class, accountOid, attributePath(QNAME_EMPLOYEE_NUMBER), "emp4321");
        delta.addModificationReplaceProperty(attributePath(QNAME_GIVEN_NAME), "Aliceeee");

        executeChanges(delta, null, task, parentResult);

        //check the state after execution

        // @formatter:off
        assertRepoShadow(accountOid, "failed shadow")
                .display()
                .pendingOperations()
                    .singleOperation()
                        .display()
                        .assertType(PendingOperationTypeType.RETRY)
                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                        .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                        .assertAttemptNumber(1)
                        .delta()
                            .display()
                            .assertModify();
        // @formatter:on

        //start openDJ
        openDJController.start();
        //and set the resource availability status to UP
//        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, parentResult);
        ObjectDelta<UserType> emptyAliceDelta = prismContext.deltaFactory().object()
                .createEmptyDelta(UserType.class, USER_ALICE_OID, ChangeType.MODIFY);
        modelService.executeChanges(MiscSchemaUtil.createCollection(emptyAliceDelta), executeOptions().reconcile(), task, parentResult);
        assertUserOneLinkRef(USER_ALICE_OID);

        //and then try to get account -> result is that the modifications will be applied to the account
//        ShadowType aliceAccount = checkNormalizedShadowWithAttributes(accountOid, "alice", "Jackkk", "alice", "alice", true, task, parentResult);
//        assertAttribute(aliceAccount, resourceTypeOpenDjrepo, "employeeNumber", "emp4321");

        //and finally stop openDJ
//        openDJController.stop();
    }

    /**
     * Given:
     *
     * - there is a user with an assigned account on OpenDJ
     *
     * The following process is executed:
     *
     * 1. A construction is unassigned, resulting in account being deleted.
     * The delete operation is pending because of the resource unavailability.
     * 2. Resource goes up. The pending operation is NOT applied (no shadow refresh is explicitly run).
     * 3. The construction is assigned again.
     * 4. Shadow refresh is executed.
     *
     * We should ensure that either the pending delete operation is cancelled during point 3, or
     * - at least - the account is deleted and then immediately re-created. In any case, the account has to
     * exist after the whole process.
     *
     * MID-7386
     */
    @Test
    public void test610DeleteAndRecreateShadow() throws Exception {
        given("preparing user with assigned account");
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.assumeRunning();

        // Enable the following line if the test is running standalone
        prepareOpenDjResource(task, result);

        UserType userBefore = createUserWithAssignedAccount(task, result);

        String userOid = userBefore.getOid();
        String userName = userBefore.getName().getOrig();
        AssignmentType assignment = userBefore.getAssignment().get(0).clone();
        AssignmentType assignmentNoIdNoMetadata = userBefore.getAssignment().get(0).cloneWithoutIdAndMetadata();

        openDJController.stop();

        // --- deleting the assignment ---
        when("deleting account by unassigning the construction");
        deleteAssignment(userOid, assignment, task, result);

        then("deleting account by unassigning the construction");
        PrismObject<ShadowType> originalShadow = assertDeletedAssignmentAndAccount(userOid);

        // --- re-creating the assignment ---
        when("re-creating the account by re-assigning the construction");
        openDJController.start();
        modifyResourceAvailabilityStatus(AvailabilityStatusType.UP, result);

        invalidateShadowCacheIfNeeded(RESOURCE_OPENDJ_OID); // this is needed to trigger shadow fetch operation
        recreateAssignment(userOid, assignmentNoIdNoMetadata, task, result);

        then("re-creating the account by re-assigning the construction");
        assertStateAfterRecreatingTheAssignment(userOid, userName, originalShadow, task, result);
    }

    /**
     * The same as {@link #test610DeleteAndRecreateShadow()} but this time the resource is in maintenance mode
     * (not really down). Please have a look at the description for that test.
     *
     * MID-7386
     */
    @Test
    public void test620DeleteAndRecreateShadowInMaintenanceMode() throws Exception {
        given("preparing user with assigned account");
        Task task = getTestTask();
        OperationResult result = task.getResult();

        openDJController.assumeRunning();

        // Enable the following line if the test is running standalone
        //prepareOpenDjResource(task, result);

        UserType userBefore = createUserWithAssignedAccount(task, result);

        String userOid = userBefore.getOid();
        String userName = userBefore.getName().getOrig();
        AssignmentType assignment = userBefore.getAssignment().get(0).clone();
        AssignmentType assignmentNoIdNoMetadata = userBefore.getAssignment().get(0).cloneWithoutIdAndMetadata();

        turnMaintenanceModeOn(RESOURCE_OPENDJ_OID, result);

        // --- deleting the assignment ---
        when("deleting account by unassigning the construction");
        deleteAssignment(userOid, assignment, task, result);

        then("deleting account by unassigning the construction");
        PrismObject<ShadowType> originalShadow = assertDeletedAssignmentAndAccount(userOid);

        // --- re-creating the assignment ---
        when("re-creating the account by re-assigning the construction");
        turnMaintenanceModeOff(RESOURCE_OPENDJ_OID, result);

        invalidateShadowCacheIfNeeded(RESOURCE_OPENDJ_OID); // this is needed to trigger shadow fetch operation
        recreateAssignment(userOid, assignmentNoIdNoMetadata, task, result);

        then("re-creating the account by re-assigning the construction");
        assertStateAfterRecreatingTheAssignment(userOid, userName, originalShadow, task, result);
    }

    /** Prepares OpenDJ resource for tests that we want to run standalone. (Contains the required core of test001.) */
    @SuppressWarnings("unused") // enabled on demand
    private void prepareOpenDjResource(Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException,
            SecurityViolationException, CommunicationException {
        assertSuccess(
                modelService.testResource(RESOURCE_OPENDJ_OID, task, result));
        fetchResourceObject(result);
    }

    /** Creates a user with a single assignment of an account construction. */
    private UserType createUserWithAssignedAccount(Task task, OperationResult result) throws CommonException {
        // @formatter:off
        UserType user = new UserType()
                .name(getTestNameShort())
                .fullName("test")
                .familyName("test")
                .beginAssignment()
                    .beginConstruction()
                        .resourceRef(RESOURCE_OPENDJ_OID, ResourceType.COMPLEX_TYPE)
                    .<AssignmentType>end()
                .end();
        // @formatter:on

        addObject(user.asPrismObject(), null, task, result);

        // @formatter:off
        return assertUser(user.getOid(), "before")
                .display()
                .links()
                    .singleLive()
                        .resolveTarget()
                            .display()
                        .end()
                    .end()
                .end()
                .getObjectable();
        // @formatter:on
    }

    private void deleteAssignment(String userOid, AssignmentType assignment, Task task, OperationResult result)
            throws CommonException {
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).delete(assignment.clone())
                        .asObjectDelta(userOid),
                null, task, result);
    }

    private PrismObject<ShadowType> assertDeletedAssignmentAndAccount(String userOid) throws CommonException {
        // @formatter:off
        String shadowOid = assertUser(userOid, "after unassignment")
                .display()
                .assertAssignments(0)
                .links()
                    .singleLive() // the link is live because the shadow is not dead (it's "reaping" i.e. pending delete)
                    .getOid();

        return assertRepoShadow(shadowOid)
                .pendingOperations()
                    .singleOperation()
                        .delta()
                            .assertDelete()
                        .end()
                        .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .end()
                .end()
                .getObject();
        // @formatter:on
    }

    private void recreateAssignment(String userOid, AssignmentType assignmentNoId, Task task, OperationResult result)
            throws CommonException {
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(assignmentNoId.clone())
                        .asObjectDelta(userOid),
                ModelExecuteOptions.create().reconcile(),
                task, result);
    }

    /**
     * In the current implementation, the pending deletion was carried out, so the original account
     * was deleted. A new one was created in its place. In the future we may be able to implement
     * a smarter approach where the pending retryable operation would be cancelled, so no account
     * would be deleted and re-created.
     */
    private void assertStateAfterRecreatingTheAssignment(String userOid, String userName, PrismObject<ShadowType> originalShadow,
            Task task, OperationResult result) throws CommonException, DirectoryException {

        // @formatter:off
        assertUser(userOid, "after re-assignment")
                .display()
                .assertAssignments(1)
                .links()
                    .assertLinks(1, isReaper() ? 0 : 1);
        // @formatter:on

        Entry accountBeforeRefresh = openDJController.searchByUid(userName);
        assertThat(accountBeforeRefresh).withFailMessage("no account before refresh").isNotNull();
        display("account before refresh", accountBeforeRefresh);

        if (!isReaper()) {

            when("refreshing deleted shadow");

            assertRepoShadow(originalShadow.getOid(), "repo shadow before refresh")
                    .display();

            try {
                clock.overrideDuration("PT1H"); // to be after retry period
                provisioningService.refreshShadow(originalShadow, null, task, result);
            } finally {
                clock.resetOverride();
            }

            then("refreshing deleted shadow");

            assertRepoShadow(originalShadow.getOid(), "repo shadow after refresh")
                    .display();
        }

        Entry accountAfterRefresh = openDJController.searchByUid(userName);
        assertThat(accountAfterRefresh).withFailMessage("no account after refresh").isNotNull();
        display("account after refresh", accountBeforeRefresh);
    }

    // This should run last. It starts a task that may interfere with other tests
    //MID-5844
    @Test
    public void test800Reconciliation() throws Exception {
        openDJController.assumeRunning();

        Task task = getTestTask();
        final OperationResult result = createOperationResult();

        // rename object directly on resource before the recon start ..it tests the rename + recon situation (MID-1594)

        // precondition
        assertTrue(EmbeddedUtils.isRunning());
        UserType userJack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result).asObjectable();
        display("Jack before", userJack);

        logger.info("start running task");
        // WHEN
        repoAddObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILE, result);
        verbose = true;
        long started = System.currentTimeMillis();
        waitForTaskNextRunAssertSuccess(TASK_OPENDJ_RECONCILIATION_OID, 120000);
        logger.info("Reconciliation task run took {} seconds", (System.currentTimeMillis() - started) / 1000L);

        // THEN

        // STOP the task. We don't need it any more. Even if it's non-recurring its safer to delete it
        taskManager.deleteTask(TASK_OPENDJ_RECONCILIATION_OID, result);

        // check if the account was added after reconciliation
        String accountOid = assertUserOneLinkRef(USER_E_OID);

        ShadowType eAccount = checkNormalizedShadowWithAttributes(accountOid, "e", "eeeee", "e", "e", task, result);
        assertAttribute(eAccount, "employeeNumber", "emp4321");
        ShadowAttributesContainer attributeContainer = ShadowUtil
                .getAttributesContainer(eAccount);
        Collection<ShadowSimpleAttribute<?>> identifiers = attributeContainer.getPrimaryIdentifiers();
        assertNotNull(identifiers);
        assertFalse(identifiers.isEmpty());
        assertEquals(1, identifiers.size());

        // check if the account was modified during reconciliation process
        String jackAccountOid = assertUserOneLinkRef(USER_JACK_OID);
        ShadowType modifiedAccount = checkNormalizedShadowBasic(jackAccountOid, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), task, result);
        assertAttribute(modifiedAccount, "givenName", "Jackkk");
        assertAttribute(modifiedAccount, "employeeNumber", "emp4321");

        // check if the account was marked as dead (or deleted) during the reconciliation process
        assert800DeadShadows();

        accountOid = assertUserOneLinkRef(USER_JACKIE_OID);
        ShadowType jack2Shadow = checkNormalizedShadowBasic(accountOid, SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), task, result);
        assertAttribute(jack2Shadow, "givenName", "jackNew2a");
        assertAttribute(jack2Shadow, "cn", "jackNew2a");
    }

    protected void assert800DeadShadows() throws CommonException {
        assertRepoShadow(ACCOUNT_DENIELS_OID)
                .assertDead();

        // @formatter:off
        LinksAsserter<?, ?, ?> linksAsserter = assertUserAfter(USER_ELAINE_OID)
                .assertLinks(1, 1)
                .links();

        ShadowReferenceAsserter<?> notDeadShadow = linksAsserter.by()
                .dead(false)
                .find();

        assertModelShadow(notDeadShadow.getOid())
                .display()
                .attributes()
                    .assertHasPrimaryIdentifier()
                    .assertHasSecondaryIdentifier()
                    .end()
                .end();

        ShadowReferenceAsserter<?> deadShadow = linksAsserter.by()
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
        // @formatter:on
    }

    //MID-5844
    @Test//(enabled = false)
    public void test801TestReconciliationRename() throws Exception {
        openDJController.assumeRunning();
        Task task = getTestTask();
        final OperationResult result = task.getResult();

        logger.info("starting rename");

        openDJController.executeRenameChange(LDIF_MODIFY_RENAME_FILE);
        logger.info("rename ended");

        logger.info("start running task");
        // WHEN
        long started = System.currentTimeMillis();
        repoAddObjectFromFile(TASK_OPENDJ_RECONCILIATION_FILE, result);
        waitForTaskFinish(TASK_OPENDJ_RECONCILIATION_OID, 120000);
        logger.info("Reconciliation task run took {} seconds", (System.currentTimeMillis() - started) / 1000L);

        // THEN

        // STOP the task. We don't need it any more. Even if it's non-recurring its safer to delete it
        taskManager.deleteTask(TASK_OPENDJ_RECONCILIATION_OID, result);

        // check if the account was added after reconciliation
        repositoryService.getObject(UserType.class, USER_E_OID, null, result).asObjectable();
        String accountOid = assertUserOneLinkRef(USER_E_OID);

        ShadowType eAccount = checkNormalizedShadowWithAttributes(accountOid, "e123", "eeeee", "e", "e", task, result);
        assertAttribute(eAccount, "employeeNumber", "emp4321");
        ShadowAttributesContainer attributeContainer = ShadowUtil
                .getAttributesContainer(eAccount);
        Collection<ShadowSimpleAttribute<?>> identifiers = attributeContainer.getPrimaryIdentifiers();
        assertNotNull(identifiers);
        assertFalse(identifiers.isEmpty());
        assertEquals(1, identifiers.size());

        ShadowSimpleAttribute<?> icfNameAttr = attributeContainer.findSimpleAttribute(getOpenDjSecondaryIdentifierQName());
        assertEquals("Wrong secondary indetifier.", "uid=e123,ou=people,dc=example,dc=com", icfNameAttr.getRealValue());

        assertEquals("Wrong shadow name. ", "uid=e123,ou=people,dc=example,dc=com", eAccount.getName().getOrig());

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);

        provisioningService.applyDefinition(repoShadow, task, result);

        ShadowAttributesContainer repoAttributeContainer = ShadowUtil.getAttributesContainer(repoShadow);
        ShadowSimpleAttribute<?> repoIcfNameAttr = repoAttributeContainer.findSimpleAttribute(getOpenDjSecondaryIdentifierQName());
        assertEquals("Wrong secondary indetifier.", "uid=e123,ou=people,dc=example,dc=com", repoIcfNameAttr.getRealValue());

        assertEquals("Wrong shadow name. ", "uid=e123,ou=people,dc=example,dc=com", repoShadow.asObjectable().getName().getOrig());

    }

    private void checkRepoOpenDjResource() throws ObjectNotFoundException, SchemaException, ConfigurationException {
        OperationResult result = new OperationResult(TestConsistencyMechanism.class.getName()
                + ".checkRepoOpenDjResource");
        PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class,
                RESOURCE_OPENDJ_OID, null, result);
        checkOpenDjResource(resource.asObjectable(), "repository");
    }

    /**
     * Checks if the resource is internally consistent, if it has everything it
     * should have.
     */
    private void checkOpenDjResource(ResourceType resource, String source) throws SchemaException, ConfigurationException {
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
            assertFalse("Resource from " + source + " has empty native capabilities",
                    CapabilityUtil.isEmpty(resource.getCapabilities().getNative()));
        }
        CapabilityCollectionType configured = resource.getCapabilities().getConfigured();
        assertNotNull("Resource from " + source + " has null configured capabilities", configured);
        assertFalse("Resource from " + source + " has empty configured capabilities", CapabilityUtil.isEmpty(configured));
        assertNotNull("Resource from " + source + " has null synchronization", resource.getSynchronization());
        checkOpenDjConfiguration(resource.asPrismObject(), source);
    }

    private void checkOpenDjSchema(ResourceType resource, String source) throws SchemaException, ConfigurationException {
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchemaRequired(resource);
        ResourceObjectClassDefinition accountDefinition = schema.findObjectClassDefinition(RESOURCE_OPENDJ_ACCOUNT_OBJECTCLASS);
        assertNotNull("Schema does not define any account (resource from " + source + ")", accountDefinition);
        Collection<? extends ShadowSimpleAttributeDefinition<?>> identifiers = accountDefinition.getPrimaryIdentifiers();
        assertFalse("No account identifiers (resource from " + source + ")", identifiers.isEmpty());
        // TODO: check for naming attributes and display names, etc

        ActivationCapabilityType capActivation = ResourceTypeUtil.getEnabledCapability(resource,
                ActivationCapabilityType.class);
        if (capActivation != null && capActivation.getStatus() != null && capActivation.getStatus().getAttribute() != null) {
            // There is simulated activation capability, check if the attribute is in schema.
            QName enableAttrName = capActivation.getStatus().getAttribute();
            ShadowSimpleAttributeDefinition<?> enableAttrDef = accountDefinition.findSimpleAttributeDefinition(enableAttrName);
            displayDumpable("Simulated activation attribute definition", enableAttrDef);
            assertNotNull("No definition for enable attribute " + enableAttrName + " in account (resource from " + source + ")", enableAttrDef);
            assertSame("Enable attribute " + enableAttrName + " is not ignored (resource from " + source + ")",
                    enableAttrDef.getProcessing(), ItemProcessing.IGNORE);
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
            // This is the heisenbug we are looking for. Just dump the entire damn thing.
            displayValue("Configuration with the heisenbug", configurationContainer.debugDump());
        }
        assertNotNull("No credentials property in " + resource + " from " + source, credentialsProp);
        assertEquals("Wrong number of credentials property value in " + resource + " from " + source, 1,
                credentialsProp.getValues().size());
        PrismPropertyValue<Object> credentialsPropertyValue = credentialsProp.getValues().iterator().next();
        assertNotNull("No credentials property value in " + resource + " from " + source,
                credentialsPropertyValue);
    }

    private UserType testAddUserToRepo(String fileName)
            throws IOException, ObjectNotFoundException, SchemaException, EncryptionException,
            ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, PolicyViolationException, SecurityViolationException {

        checkRepoOpenDjResource();
        assertNoRepoThreadLocalCache();

        PrismObject<UserType> user = PrismTestUtil.parseObject(new File(fileName));
        UserType userType = user.asObjectable();
        PrismAsserts.assertParentConsistency(user);

        protector.encrypt(userType.getCredentials().getPassword().getValue());
        PrismAsserts.assertParentConsistency(user);

        OperationResult result = new OperationResult("add user");

        display("Adding user object", userType);

        Task task = taskManager.createTaskInstance();
        // WHEN
        ObjectDelta<?> delta = DeltaFactory.Object.createAddDelta(user);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
        modelService.executeChanges(deltas, null, task, result);
        // THEN

        assertNoRepoThreadLocalCache();

        return userType;
    }

    private String assertUserOneLinkRef(String userOid) throws Exception {
        PrismObject<UserType> repoUser = getRepoUser(userOid);
        return assertOneLinkRef(repoUser);
    }

    @NotNull
    private PrismObject<UserType> getRepoUser(String userOid) throws ObjectNotFoundException, SchemaException {
        OperationResult parentResult = new OperationResult("getObject from repo");

        PrismObject<UserType> repoUser = repositoryService.getObject(
                UserType.class, userOid, null, parentResult);

        parentResult.computeStatus();
        TestUtil.assertSuccess("getObject has failed", parentResult);
        return repoUser;
    }

    private String assertOneLinkRef(PrismObject<UserType> user) {

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
        assertAssignments(user, 1);

        return userType.getLinkRef().get(0).getOid();
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

    private void modifyResourceAvailabilityStatus(AvailabilityStatusType status, OperationResult parentResult) throws Exception {
        PropertyDelta<AvailabilityStatusType> resourceStatusDelta = prismContext.deltaFactory().property()
                .createModificationReplaceProperty(
                        ItemPath.create(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
                        resourceTypeOpenDjrepo.asPrismObject().getDefinition(),
                        status);
        Collection<PropertyDelta<AvailabilityStatusType>> modifications = new ArrayList<>();
        modifications.add(resourceStatusDelta);
        repositoryService.modifyObject(ResourceType.class, resourceTypeOpenDjrepo.getOid(), modifications, parentResult);
    }

    private ShadowType checkNormalizedShadowWithAttributes(String accountOid, String uid, String givenName, String sn,
            String cn, Task task, OperationResult parentResult) throws Exception {
        ShadowType resourceAccount = checkNormalizedShadowBasic(accountOid, null, task, parentResult);
        assertAttributes(resourceAccount, uid, givenName, sn, cn);
        return resourceAccount;
    }

    private ShadowType checkNormalizedShadowBasic(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult parentResult) throws Exception {
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, shadowOid, options, task, parentResult);
        assertNotNull(shadow);
        ShadowType shadowBean = shadow.asObjectable();
        // @formatter:off
        assertShadow(shadow, "after discovery")
                .display()
                .assertResource(RESOURCE_OPENDJ_OID)
                .pendingOperations()
                    .assertNoUnfinishedOperations()
                .end();
        // @formatter:on
        return shadowBean;
    }

    @SafeVarargs
    protected final <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
        assertAttribute(shadow.asObjectable(), attrName, expectedValues);
    }

    protected boolean isReaper() {
        return false;
    }

    private ItemPath attributePath(QName itemName) {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, itemName);
    }

    private @NotNull ItemPath attributePath(String name) {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, new ItemName(MidPointConstants.NS_RI, name));
    }
}
