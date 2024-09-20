/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisc extends AbstractMiscTest {

    private static final File ROLE_IMPORT_FILTERS_FILE = new File(TEST_DIR, "role-import-filters.xml");
    private static final String ROLE_IMPORT_FILTERS_OID = "aad19b9a-d511-11e7-8bf7-cfecde275e59";

    private static final File ROLE_SHIP_FILE = new File(TEST_DIR, "role-ship.xml");
    private static final String ROLE_SHIP_OID = "bbd19b9a-d511-11e7-8bf7-cfecde275e59";

    private static final TestObject<ArchetypeType> ARCHETYPE_NODE_GROUP_GUI = TestObject.file(TEST_DIR, "archetype-node-group-gui.xml", "05b6933a-b7fc-4543-b8fa-fd8b278ff9ee");

    protected static final File RESOURCE_SCRIPTY_FILE = new File(TEST_DIR, "resource-dummy-scripty.xml");
    protected static final String RESOURCE_SCRIPTY_OID = "399f5308-0447-11e8-91e9-a7f9c4100ffb";
    protected static final String RESOURCE_DUMMY_SCRIPTY_NAME = "scripty";

    private static final byte[] KEY = { 0x01, 0x02, 0x03, 0x04, 0x05 };

    private static final String USER_CLEAN_NAME = "clean";
    private static final String USER_CLEAN_GIVEN_NAME = "John";
    private static final String USER_CLEAN_FAMILY_NAME = "Clean";

    private String userCleanOid;
    private Integer lastDummyConnectorNumber;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_SCRIPTY_NAME,
                RESOURCE_SCRIPTY_FILE, RESOURCE_SCRIPTY_OID, initTask, initResult);

        importObjectFromFile(ROLE_SHIP_FILE);
        repoAdd(ARCHETYPE_NODE_GROUP_GUI, initResult);
    }

    @Test
    public void test100GetRepositoryDiag() {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        RepositoryDiag diag = modelDiagnosticService.getRepositoryDiag(task, result);

        then();
        displayValue("Diag", diag);
        assertSuccess(result);

        assertThat(diag.getImplementationShortName()).isIn("SQL", "Native");
        assertNotNull("Missing implementationDescription", diag.getImplementationDescription());
    }

    @Test
    public void test110RepositorySelfTest() {
        given();
        Task task = getTestTask();

        when();
        OperationResult testResult = modelDiagnosticService.repositorySelfTest(task);

        then();
        display("Repository self-test result", testResult);
        TestUtil.assertSuccess("Repository self-test result", testResult);

        // TODO: check the number of tests, etc.
    }

    @Test
    public void test200ExportUsers() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), task, result);

        then();
        assertSuccess(result);

        assertEquals("Unexpected number of users", 6, users.size());
        for (PrismObject<UserType> user : users) {
            display("Exporting user", user);
            assertNotNull("Null definition in " + user, user.getDefinition());
            displayDumpable("Definition", user.getDefinition());
            String xmlString = prismContext.xmlSerializer().serialize(user);
            displayValue("Exported user", xmlString);

            // We cannot validate the objects, as new value metadata breaks the validation.
//            Document xmlDocument = DOMUtil.parseDocument(xmlString);
//            Validator validator = prismContext.getSchemaRegistry().getJavaxSchemaValidator();
//            validator.validate(new DOMSource(xmlDocument));

            PrismObject<Objectable> parsedUser = prismContext.parseObject(xmlString);
            assertEquals("Re-parsed user is not equal to original: " + user, parsedUser, user);
        }
    }

    /**
     * Polystring search, polystringNorm matching rule.
     * This should go well, no error, no warning.
     */
    @Test
    public void test210SearchUsersMatchingRulesPolystringNorm() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_NAME)
                .eq("JacK")
                .matchingNorm()
                .build();

        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        then();
        assertSuccess(result);

        assertEquals("Unexpected number of users", 1, users.size());
        PrismObject<UserType> user = users.get(0);
        assertUser(user, "found user")
                .assertName(USER_JACK_USERNAME);
    }

    /**
     * Polystring search, stringIgnoreCase matching rule.
     * This does not fit. It is not supported. There is a failure.
     * MID-5911
     */
    @Test
    public void test212SearchUsersMatchingRulesPolystringIgnoreCase() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_NAME)
                .eq("JacK")
                .matchingCaseIgnore()
                .build();

        try {
            when();
            modelService.searchObjects(UserType.class, query, null, task, result);
        } catch (SystemException e) {
            // this is expected
        }
    }

    /**
     * String search, stringIgnoreCase matching rule.
     * This should go well, no error, no warning.
     * MID-5911
     */
    @Test
    public void test214SearchUsersMatchingRulesStringIgnoreCase() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER)
                .eq("EMP1234") // Real value is "emp123"
                .matchingCaseIgnore()
                .build();

        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        then();
        assertSuccess(result);

        assertEquals("Unexpected number of users", 1, users.size());
        PrismObject<UserType> user = users.get(0);
        assertUser(user, "found user")
                .assertName(USER_JACK_USERNAME);
    }

    /**
     * String search, polystringNorm matching rule.
     * This does not really fit. It fill not fail. But it will find nothing and there is a warning.
     * MID-5911
     */
    @Test(enabled = false)
    public void test216SearchUsersMatchingRulesStringNorm() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER)
                .eq("EMP1234") // Real value is "emp123"
                .matchingCaseIgnore()
                .build();

        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        then();
        assertSuccess(result);

        assertEquals("Unexpected number of users", 0, users.size());
    }

    /**
     * Just to make sure Jack is clean and that the next text will
     * start from a clean state.
     */
    @Test
    public void test300RecomputeJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        recomputeUser(USER_JACK_OID, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
    }

    /**
     * Modify custom binary property.
     * MID-3999
     */
    @Test
    public void test302UpdateKeyJack() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        //noinspection PrimitiveArrayArgumentToVarargsMethod
        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_KEY), task, result, KEY);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        PrismAsserts.assertPropertyValue(userAfter, getExtensionPath(PIRACY_KEY), KEY);
    }

    @Test
    public void test310AddUserClean() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_CLEAN_NAME, USER_CLEAN_GIVEN_NAME, USER_CLEAN_FAMILY_NAME, true);

        when();
        addObject(userBefore, task, result);

        then();
        assertSuccess(result);

        userCleanOid = userBefore.getOid();

        PrismObject<UserType> userAfter = getUser(userCleanOid);
        display("User after", userAfter);
    }

    /**
     * Modify custom binary property.
     * MID-3999
     */
    @Test
    public void test312UpdateBinaryIdClean() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        //noinspection PrimitiveArrayArgumentToVarargsMethod
        modifyUserReplace(userCleanOid, getExtensionPath(PIRACY_BINARY_ID), task, result, KEY);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userCleanOid);
        display("User after", userAfter);
        PrismAsserts.assertPropertyValue(userAfter, getExtensionPath(PIRACY_BINARY_ID), KEY);
    }

    /**
     * MID-4660, MID-4491, MID-3581
     */
    @Test
    public void test320DefaultRelations() {
        when();
        List<RelationDefinitionType> relations = modelInteractionService.getRelationDefinitions();

        then();
        display("Relations", relations);
        assertRelationDef(relations, SchemaConstants.ORG_MANAGER, "manager");
        assertRelationDef(relations, SchemaConstants.ORG_OWNER, "owner");
        assertEquals("Unexpected number of relation definitions", 8, relations.size());
    }

    /**
     * Add indestructible user.
     * This is add. Nothing special should happen.
     * <p>
     * MID-1448
     */
    @Test
    public void test330IndestructibleSkellingtonAdd() throws Exception {
        when();
        addObject(USER_SKELLINGTON_FILE);

        then();
        assertUserAfter(USER_SKELLINGTON_OID)
                .assertName(USER_SKELLINGTON_NAME)
                .assertIndestructible();
    }

    /**
     * Attempt to delete indestructible user.
     * This should end up with an error.
     * <p>
     * MID-1448
     */
    @Test
    public void test332IndestructibleSkellingtonDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            when();
            deleteObject(UserType.class, USER_SKELLINGTON_OID, task, result);
            assertNotReached();
        } catch (PolicyViolationException e) {
            assertFailure(result);
        }

        then();
        assertUserAfter(USER_SKELLINGTON_OID)
                .assertName(USER_SKELLINGTON_NAME)
                .assertIndestructible();
    }

    /**
     * Attempt to RAW delete indestructible user.
     * This should end up with an error.
     * <p>
     * MID-1448
     */
    @Test
    public void test333IndestructibleSkellingtonDeleteRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        try {
            when();
            deleteObjectRaw(UserType.class, USER_SKELLINGTON_OID, task, result);
            assertNotReached();
        } catch (PolicyViolationException e) {
            assertFailure(result);
        }

        then();
        assertUserAfter(USER_SKELLINGTON_OID)
                .assertName(USER_SKELLINGTON_NAME)
                .assertIndestructible();
    }

    /**
     * Modify Skellington to be destructible. This should go well.
     * <p>
     * MID-1448
     */
    @Test
    public void test335IndestructibleSkellingtonModify() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modifyObjectReplaceProperty(UserType.class, USER_SKELLINGTON_OID, UserType.F_INDESTRUCTIBLE, task, result, false);

        then();
        assertSuccess(result);
        assertUserAfter(USER_SKELLINGTON_OID)
                .assertName(USER_SKELLINGTON_NAME)
                .assertDestructible();
    }

    /**
     * Attempt to delete indestructible user, that is made destructible now.
     * This should go well.
     * <p>
     * MID-1448
     */
    @Test
    public void test339DestructibleSkellingtonDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        deleteObject(UserType.class, USER_SKELLINGTON_OID, task, result);

        then();
        assertSuccess(result);
        assertNoObject(UserType.class, USER_SKELLINGTON_OID);
    }

    /**
     * MID-3879
     */
    @Test
    public void test400ImportRoleWithFilters() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modelService.importObjectsFromFile(ROLE_IMPORT_FILTERS_FILE, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getRole(ROLE_IMPORT_FILTERS_OID);
        display("Role after", roleAfter);

        assertInducedRole(roleAfter, ROLE_PIRATE_OID);
        assertInducedRole(roleAfter, ROLE_SAILOR_OID);
        assertInducements(roleAfter, 2);
    }

    @Test
    public void test500AddHocProvisioningScriptAssignJackResourceScripty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);

        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_SCRIPTY_OID, null, task, result);

        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_SCRIPTY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_SCRIPTY_NAME, ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                "Mr. POLY JACK SPARROW");
    }

    /**
     * MID-3044
     */
    @Test
    public void test502GetAccountJackResourceScripty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);

        when();
        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        then();
        assertSuccess(result);

        assertAttribute(accountShadow.asObjectable(),
                getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
                "Dummy Resource: Scripty");
        lastDummyConnectorNumber = ShadowUtil.getAttributeValue(accountShadow,
                getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME));
    }

    /**
     * Check that the same connector instance is used. The connector should be pooled and cached.
     * MID-3104
     */
    @Test
    public void test504GetAccountJackResourceScriptyAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);

        when();
        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        then();
        assertSuccess(result);

        assertAttribute(accountShadow.asObjectable(),
                getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME),
                "Dummy Resource: Scripty");
        Integer dummyConnectorNumber = ShadowUtil.getAttributeValue(accountShadow,
                getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME));
        assertEquals("Connector number has changed", lastDummyConnectorNumber, dummyConnectorNumber);
    }

    /**
     * Modify resource (but make sure that connector configuration is the same).
     * Make just small an unimportant change in the connector. That should increase the version
     * number which should purge resource caches. But the connector instance should still be the
     * same because connector configuration haven't changed. We do NOT want to purge connector cache
     * (and re-create connector) after every minor change to the resource. In that case change in
     * resource availability status can trigger quite a lot of connector re-initializations.
     */
    @Test
    public void test506ModifyResourceGetAccountJackResourceScripty() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);
        PrismObject<ResourceType> resourceBefore = getObject(ResourceType.class, RESOURCE_SCRIPTY_OID);
        displayValue("Resource version before", resourceBefore.getVersion());

        when();
        modifyObjectReplaceProperty(ResourceType.class, RESOURCE_SCRIPTY_OID,
                ResourceType.F_DESCRIPTION, null, task, result, "Whatever");

        then();
        assertSuccess(result);

        PrismObject<ResourceType> resourceAfter = getObject(ResourceType.class, RESOURCE_SCRIPTY_OID);
        displayValue("Resource version after", resourceAfter.getVersion());
        assertThat(resourceAfter.getVersion())
                .withFailMessage("Resource version is still the same")
                .isNotEqualTo(resourceBefore.getVersion());

        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        Integer dummyConnectorNumber = ShadowUtil.getAttributeValue(accountShadow,
                getDummyResourceController(RESOURCE_DUMMY_SCRIPTY_NAME).getAttributeQName(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME));
        assertEquals("Connector number hash changed: " + lastDummyConnectorNumber + " -> " + dummyConnectorNumber,
                dummyConnectorNumber, lastDummyConnectorNumber);
    }

    /**
     * MID-4504
     * midpoint.getLinkedShadow fails recomputing without throwing exception during shadow delete
     * <p>
     * the ship attribute in the role "Ship" has mapping with calling midpoint.getLinkedShadow() on the reosurce which doesn't exist
     */
    @Test
    public void test600jackAssignRoleShip() throws Exception {
        when();
        assignRole(USER_JACK_OID, ROLE_SHIP_OID);

        then();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User before", userAfter);
        assertAssignments(userAfter, 2);
        assertLiveLinks(userAfter, 1);

        PrismReference linkRef = userAfter.findReference(UserType.F_LINK_REF);
        assertFalse(linkRef.isEmpty());

        assertDummyAccountAttribute(RESOURCE_DUMMY_SCRIPTY_NAME, USER_JACK_USERNAME, "ship", "ship");
    }

    @Test
    public void test601jackUnassignResourceAccount() throws Exception {
        given();
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_SCRIPTY_OID, null);

        then();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 1);
    }

    /**
     * MID-4504
     * midpoint.getLinkedShadow fails recomputing without throwing exception during shadow delete
     * <p>
     * first assign role ship, the ship attribute in the role has mapping with calling midpoint.getLinkedShadow()
     */
    @Test
    public void test602jackUnassignRoleShip() throws Exception {
        when();
        unassignRole(USER_JACK_OID, ROLE_SHIP_OID);

        then();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User before", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 0);
    }

    @Test
    public void test700AddNodeArchetype() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        String localNodeOid = taskManager.getLocalNodeOid();
        ObjectDelta<NodeType> delta = deltaFor(NodeType.class)
                .item(NodeType.F_ASSIGNMENT)
                .add(ObjectTypeUtil.createAssignmentTo(ARCHETYPE_NODE_GROUP_GUI.oid, ObjectTypes.ARCHETYPE))
                .asObjectDelta(localNodeOid);

        executeChanges(delta, null, task, result);

        then();
        assertObject(NodeType.class, localNodeOid, "after")
                .display()
                .assertArchetypeRef(ARCHETYPE_NODE_GROUP_GUI.oid);

        Set<String> localNodeGroups = taskManager.getLocalNodeGroups().stream()
                .map(ObjectReferenceType::getOid)
                .collect(Collectors.toSet());
        assertThat(localNodeGroups).as("local node groups")
                .containsExactlyInAnyOrder(ARCHETYPE_NODE_GROUP_GUI.oid);
    }
}
