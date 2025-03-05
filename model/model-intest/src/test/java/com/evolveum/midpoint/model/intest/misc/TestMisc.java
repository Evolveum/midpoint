/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnectorInstanceConnIdImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy;
import com.evolveum.midpoint.schema.statistics.BasicComponentStructure;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.DummyObjectsCreator;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.prism.Referencable;

import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.w3c.dom.Document;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisc extends AbstractMiscTest {

    private static final File ROLE_IMPORT_FILTERS_FILE = new File(TEST_DIR, "role-import-filters.xml");
    private static final String ROLE_IMPORT_FILTERS_OID = "aad19b9a-d511-11e7-8bf7-cfecde275e59";

    private static final File ROLE_SHIP_FILE = new File(TEST_DIR, "role-ship.xml");
    private static final String ROLE_SHIP_OID = "bbd19b9a-d511-11e7-8bf7-cfecde275e59";

    private static final TestObject<?> ORG_ROOT = TestObject.file(
            TEST_DIR, "org-root.xml", "0fec8f6d-4034-4202-b6e7-317cf87545ea");

    private static final TestObject<?> TEMPLATE_SCRIPTED = TestObject.file(
            TEST_DIR, "template-scripted.xml", "de691036-ecb0-4fce-9823-2393bcff3cbd");
    private static final TestObject<?> ARCHETYPE_SCRIPTED = TestObject.file(
            TEST_DIR, "archetype-scripted.xml", "357afd27-8462-43eb-8d1b-908bf623e0a0");
    private static final TestObject<?> USER_SCRIPTED = TestObject.file(
            TEST_DIR, "user-scripted.xml", "167cb7a4-f3be-4a8a-9d90-bb049e82da04");

    private static final TestObject<ArchetypeType> ARCHETYPE_NODE_GROUP_GUI = TestObject.file(TEST_DIR, "archetype-node-group-gui.xml", "05b6933a-b7fc-4543-b8fa-fd8b278ff9ee");

    private static final File RESOURCE_SCRIPTY_FILE = new File(TEST_DIR, "resource-dummy-scripty.xml");
    private static final String RESOURCE_SCRIPTY_OID = "399f5308-0447-11e8-91e9-a7f9c4100ffb";
    private static final String RESOURCE_DUMMY_SCRIPTY_NAME = "scripty";

    private static final DummyTestResource RESOURCE_DUMMY_PERF = new DummyTestResource(
            TEST_DIR, "resource-dummy-perf.xml", "5d9cdc84-09cd-45f0-a60c-522ccc8fca88", "perf");

    private static final byte[] KEY = { 0x01, 0x02, 0x03, 0x04, 0x05 };

    private static final String USER_CLEAN_NAME = "clean";
    private static final String USER_CLEAN_GIVEN_NAME = "John";
    private static final String USER_CLEAN_FAMILY_NAME = "Clean";

    private static final int OBJECT_PROCESSING_DELAY = 100;
    private static final double SAFETY_MARGIN = 1.5;

    @Autowired private CacheConfigurationManager cacheConfigurationManager;

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

        initTestObjects(initTask, initResult,
                ORG_ROOT, TEMPLATE_SCRIPTED, ARCHETYPE_SCRIPTED);

        initAndTestDummyResource(RESOURCE_DUMMY_PERF, initTask, initResult);
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

            Document xmlDocument = DOMUtil.parseDocument(xmlString);
            Validator validator = prismContext.getSchemaRegistry().getJavaxSchemaValidator();
            validator.validate(new DOMSource(xmlDocument));

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
                .matchingNorm()
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
     * Iterative search (from repo and repo cache) should provide correct operation names and execution times for the whole chain:
     * going down from model to repo cache to repo impl, and then stepping up the result handlers.
     *
     * MID-10446
     */
    @Test
    public void test520OperationResultInHandlersForRepoIterativeSearch() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        int numGeneratedRoles = 30;

        // Please adapt if the internals of the iterative search change

        // model level
        var modelSearchIterativeOpName = ModelController.SEARCH_OBJECTS;
        var modelHandleObjectFoundOpName = ModelController.OP_HANDLE_OBJECT_FOUND;

        // repo cache level
        var repoCacheSearchIterativeOpName = RepositoryCache.OP_SEARCH_OBJECTS_ITERATIVE_IMPL;
        var repoCacheIterateOverQueryResultOpName = RepositoryCache.OP_ITERATE_OVER_QUERY_RESULT;
        var repoCacheHandleObjectFoundOpName = RepositoryCache.OP_HANDLE_OBJECT_FOUND_IMPL;

        // repo impl level
        var repoSearchObjectsIterativeOpName = isNativeRepository() ?
                "SqaleRepositoryService.searchObjectsIterative" : RepositoryService.SEARCH_OBJECTS_ITERATIVE;
        var repoHandleObjectFoundOpName = isNativeRepository() ?
                "SqaleRepositoryService.handleObjectFound" : RepositoryService.HANDLE_OBJECT_FOUND;

        long expectedTotalLow = numGeneratedRoles * OBJECT_PROCESSING_DELAY * 1000L;
        long expectedTotalHigh = (long) (expectedTotalLow * SAFETY_MARGIN) * 1000L;
        long expectedNoOpOwnHigh = 100 * 1000L;
        long expectedOwnLow = numGeneratedRoles * OBJECT_PROCESSING_DELAY * 1000L;
        long expectedOwnHigh = (long) (expectedOwnLow * SAFETY_MARGIN) * 1000L;

        // Needed for returning objects from the local cache (roles are not cached globally by default)
        RepositoryCache.enterLocalCaches(cacheConfigurationManager);

        try {

            given("%s generated roles".formatted(numGeneratedRoles));
            generateRoles(numGeneratedRoles, "generated-role-%04d", null, null, result);

            when("searching for users iteratively");
            OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
            var result1 = result.createSubresult(TestMisc.class.getName() + ".first");
            searchGeneratedRolesIteratively(task, result1);

            then("result is OK");
            var previous = DebugUtil.setDetailedDebugDump(true);
            displayDumpable("result", result1);
            DebugUtil.setDetailedDebugDump(previous);
            // To be adapted if the internals of the iterative search change
            var handlingResults1 = assertThatOperationResult(result1)
                    .firstSubResultMatching(sr -> sr.getOperation().equals(modelSearchIterativeOpName))
                    .firstSubResultMatching(sr -> sr.getOperation().equals(repoCacheSearchIterativeOpName))
                    .firstSubResultMatching(sr -> sr.getOperation().equals(repoSearchObjectsIterativeOpName))
                    .getAllSubResultsMatching(sr -> sr.getOperation().equals(repoHandleObjectFoundOpName));
            // The following two numbers should include summarized (hidden) subresults
            var totalHandlingTime1 = handlingResults1.stream()
                    .mapToLong(sr -> sr.getMicroseconds())
                    .sum();
            var ownHandlingTime1 = handlingResults1.stream()
                    .mapToLong(sr -> sr.getOwnMicroseconds())
                    .sum();

            and("performance info is OK");
            var info1 = assertGlobalOperationsPerformance()
                    .display(SortBy.NAME)
                    .assertInvocationCount(modelSearchIterativeOpName, 1)
                    .assertTotalTimeBetween(modelSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(modelSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(repoCacheSearchIterativeOpName, 1)
                    .assertTotalTimeBetween(repoCacheSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(repoCacheSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(repoSearchObjectsIterativeOpName, 1)
                    .assertTotalTimeBetween(repoSearchObjectsIterativeOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(repoSearchObjectsIterativeOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(repoHandleObjectFoundOpName, numGeneratedRoles)
                    .assertTotalTimeBetween(repoHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(repoHandleObjectFoundOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(modelHandleObjectFoundOpName, numGeneratedRoles)
                    .assertTotalTimeBetween(modelHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(modelHandleObjectFoundOpName, expectedOwnLow, expectedOwnHigh)
                    .get();
            // Checking that summarized (hidden) subresults are correctly included in the total time.
            assertThat(info1.getTotalTime(repoHandleObjectFoundOpName))
                    .as("Total handling time (repo level)")
                    .isEqualTo(totalHandlingTime1);
            assertThat(info1.getOwnTime(repoHandleObjectFoundOpName))
                    .as("Own handling time (repo level)")
                    .isEqualTo(ownHandlingTime1);

            and("components performance info is OK");
            assertGlobalOperationsPerformanceByStandardComponents()
                    .display()
                    .assertTotalTimeBetween(BasicComponentStructure.MODEL.getComponentName(), expectedOwnLow, expectedOwnHigh)
                    .assertTotalTimeBetween(BasicComponentStructure.REPOSITORY_CACHE.getComponentName(), 0, expectedNoOpOwnHigh)
                    .assertTotalTimeBetween(BasicComponentStructure.REPOSITORY.getComponentName(), 0, expectedNoOpOwnHigh);

            given("putting the search result into the repo cache");
            // Iterative search contains the delay that makes the result too old to be cached, so we must do normal search
            searchGeneratedRoles(task, result);

            when("searching for users iteratively again (from the cache)");
            OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
            var result2 = result.createSubresult(TestMisc.class.getName() + ".second");
            searchGeneratedRolesIteratively(task, result2);

            then("result is OK");
            DebugUtil.setDetailedDebugDump(true);
            displayDumpable("result", result2);
            DebugUtil.setDetailedDebugDump(previous);
            // To be adapted if the internals of the iterative search change
            var handlingResults2 = assertThatOperationResult(result2)
                    .firstSubResultMatching(sr -> sr.getOperation().equals(modelSearchIterativeOpName))
                    .firstSubResultMatching(sr -> sr.getOperation().equals(repoCacheSearchIterativeOpName))
                    .firstSubResultMatching(sr -> sr.getOperation().equals(repoCacheIterateOverQueryResultOpName))
                    .getAllSubResultsMatching(sr -> sr.getOperation().equals(repoCacheHandleObjectFoundOpName));
            // The following two numbers should include summarized (hidden) subresults
            var totalHandlingTime2 = handlingResults2.stream()
                    .mapToLong(sr -> sr.getMicroseconds())
                    .sum();
            var ownHandlingTime2 = handlingResults2.stream()
                    .mapToLong(sr -> sr.getOwnMicroseconds())
                    .sum();

            and("performance info is OK");
            var info2 = assertGlobalOperationsPerformance()
                    .display(SortBy.NAME)
                    .assertInvocationCount(modelSearchIterativeOpName, 1)
                    .assertTotalTimeBetween(modelSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(modelSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(repoCacheSearchIterativeOpName, 1)
                    .assertTotalTimeBetween(repoCacheSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(repoCacheSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(repoCacheIterateOverQueryResultOpName, 1)
                    .assertTotalTimeBetween(repoCacheIterateOverQueryResultOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(repoCacheIterateOverQueryResultOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(repoCacheHandleObjectFoundOpName, numGeneratedRoles)
                    .assertTotalTimeBetween(repoCacheHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(repoCacheHandleObjectFoundOpName, 0, expectedNoOpOwnHigh)
                    .assertInvocationCount(modelHandleObjectFoundOpName, numGeneratedRoles)
                    .assertTotalTimeBetween(modelHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                    .assertOwnTimeBetween(modelHandleObjectFoundOpName, expectedOwnLow, expectedOwnHigh)
                    .get();
            // Checking that summarized (hidden) subresults are correctly included in the total time.
            assertThat(info2.getTotalTime(repoCacheHandleObjectFoundOpName))
                    .as("Total handling time (repo cache level)")
                    .isEqualTo(totalHandlingTime2);
            assertThat(info2.getOwnTime(repoCacheHandleObjectFoundOpName))
                    .as("Own handling time (repo cache level)")
                    .isEqualTo(ownHandlingTime2);

            and("components performance info is OK");
            assertGlobalOperationsPerformanceByStandardComponents()
                    .display()
                    .assertTotalTimeBetween(BasicComponentStructure.MODEL.getComponentName(), expectedOwnLow, expectedOwnHigh)
                    .assertTotalTimeBetween(BasicComponentStructure.REPOSITORY_CACHE.getComponentName(), 0, expectedNoOpOwnHigh)
                    .assertTotalTime(BasicComponentStructure.REPOSITORY.getComponentName(), null);

        } finally {
            RepositoryCache.exitLocalCaches();
        }
    }

    private void searchGeneratedRolesIteratively(Task task, OperationResult result) throws CommonException {
        modelService.searchObjectsIterative(
                RoleType.class, generatedRolesQuery(),
                (object, lResult) -> {
                    lResult.setMessage("Hello");
                    MiscUtil.sleepCatchingInterruptedException(OBJECT_PROCESSING_DELAY);
                    return true;
                },
                null, task, result);
    }

    private void searchGeneratedRoles(Task task, OperationResult result) throws CommonException {
        modelService.searchObjects(RoleType.class, generatedRolesQuery(), null, task, result);
    }

    private ObjectQuery generatedRolesQuery() {
        return prismContext.queryFor(RoleType.class)
                .item(RoleType.F_NAME).startsWith("generated-role-").matchingOrig()
                .build();
    }

    /**
     * Iterative search (from provisioning: both fetch and no-fetch) should provide correct operation names and execution times
     * for the whole chain: going down from model to provisioning to UCF to ConnId, and then stepping up the result handlers.
     *
     * MID-10446
     */
    @Test
    public void test530OperationResultInHandlersForProvisioningIterativeSearch() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        int numCreatedAccounts = 30;

        // Please adapt if the internals of the iterative search change

        // model level
        var modelSearchIterativeOpName = ModelService.SEARCH_OBJECTS;
        var modelHandleObjectFoundOpName = ModelController.OP_HANDLE_OBJECT_FOUND;

        // provisioning - upper (shadows) level
        var provisioningSearchIterativeOpName = ProvisioningService.OP_SEARCH_OBJECTS_ITERATIVE;
        var shadowsHandleObjectFoundOpName = ShadowsFacade.OP_HANDLE_RESOURCE_OBJECT_FOUND;
        var shadowManagerHandleObjectFoundOpName = ShadowFinder.OP_HANDLE_OBJECT_FOUND;

        // provisioning - lower (resource objects) level
        var resourceObjectsSearchOpName = ResourceObjectConverter.OP_SEARCH_RESOURCE_OBJECTS;
        var resourceObjectsHandleObjectFound = ResourceObjectConverter.OP_HANDLE_OBJECT_FOUND;

        // UCF level
        var ucfSearchOpName = ConnectorInstance.OP_SEARCH;

        // ConnId level
        var connIdSearchOpName = ConnectorInstanceConnIdImpl.FACADE_OP_SEARCH;
        var connIdHandleObjectFoundOpName = ConnectorInstanceConnIdImpl.OP_HANDLE_OBJECT_FOUND;

        // repo cache level
        var repoCacheSearchIterativeOpName = RepositoryCache.OP_SEARCH_OBJECTS_ITERATIVE_IMPL;

        // repo impl level
        var repoSearchObjectsIterativeOpName = isNativeRepository() ?
                "SqaleRepositoryService.searchObjectsIterative" : RepositoryService.SEARCH_OBJECTS_ITERATIVE;
        var repoHandleObjectFoundOpName = isNativeRepository() ?
                "SqaleRepositoryService.handleObjectFound" : RepositoryService.HANDLE_OBJECT_FOUND;

        long expectedTotalLow = numCreatedAccounts * OBJECT_PROCESSING_DELAY * 1000L;
        long expectedTotalHigh = (long) (expectedTotalLow * SAFETY_MARGIN) * 1000L;
        long expectedNoOpOwnHigh = 100 * 1000L;
        long expectedOwnLow = numCreatedAccounts * OBJECT_PROCESSING_DELAY * 1000L;
        long expectedOwnHigh = (long) (expectedOwnLow * SAFETY_MARGIN) * 1000L;

        given("%s created accounts".formatted(numCreatedAccounts));
        DummyObjectsCreator.accounts()
                .withObjectCount(numCreatedAccounts)
                .withNamePattern("generated-account-%04d")
                .withController(RESOURCE_DUMMY_PERF.controller)
                .execute();

        when("searching for accounts iteratively (initially: creating shadows)");
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
        var result0 = result.createSubresult(TestMisc.class.getName() + ".initial");
        searchCreatedAccounts(false, task, result0);
        assertGlobalOperationsPerformance()
                .display(SortBy.NAME);
        assertGlobalOperationsPerformanceByStandardComponents()
                .display();

        when("searching for accounts iteratively (first time with shadows)");
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
        var result1 = result.createSubresult(TestMisc.class.getName() + ".first");
        searchCreatedAccounts(false, task, result1);

        then("result is OK");
        var previous = DebugUtil.setDetailedDebugDump(true);
        displayDumpable("result", result1);
        DebugUtil.setDetailedDebugDump(previous);
        // To be adapted if the internals of the iterative search change
        var handlingResults1 = assertThatOperationResult(result1)
                .firstSubResultMatching(sr -> sr.getOperation().equals(modelSearchIterativeOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(provisioningSearchIterativeOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(resourceObjectsSearchOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(ucfSearchOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(connIdSearchOpName))
                .getAllSubResultsMatching(sr -> sr.getOperation().equals(connIdHandleObjectFoundOpName));
        // The following two numbers should include summarized (hidden) subresults
        var totalHandlingTime1 = handlingResults1.stream()
                .mapToLong(sr -> sr.getMicroseconds())
                .sum();
        var ownHandlingTime1 = handlingResults1.stream()
                .mapToLong(sr -> sr.getOwnMicroseconds())
                .sum();

        and("performance info is OK");
        var info1 = assertGlobalOperationsPerformance()
                .display(SortBy.NAME)
                .assertInvocationCount(modelSearchIterativeOpName, 1)
                .assertTotalTimeBetween(modelSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(modelSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(provisioningSearchIterativeOpName, 1)
                .assertTotalTimeBetween(provisioningSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(provisioningSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(resourceObjectsSearchOpName, 1)
                .assertTotalTimeBetween(resourceObjectsSearchOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(resourceObjectsSearchOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(ucfSearchOpName, 1)
                .assertTotalTimeBetween(ucfSearchOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(ucfSearchOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(connIdSearchOpName, 1)
                .assertTotalTimeBetween(connIdSearchOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(connIdSearchOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(connIdHandleObjectFoundOpName, numCreatedAccounts)
                .assertTotalTimeBetween(connIdHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(connIdHandleObjectFoundOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(resourceObjectsHandleObjectFound, numCreatedAccounts)
                .assertTotalTimeBetween(resourceObjectsHandleObjectFound, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(resourceObjectsHandleObjectFound, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(shadowsHandleObjectFoundOpName, numCreatedAccounts)
                .assertTotalTimeBetween(shadowsHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(shadowsHandleObjectFoundOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(modelHandleObjectFoundOpName, numCreatedAccounts)
                .assertTotalTimeBetween(modelHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(modelHandleObjectFoundOpName, expectedOwnLow, expectedOwnHigh)
                .get();
        // Checking that summarized (hidden) subresults are correctly included in the total time.
        assertThat(info1.getTotalTime(connIdHandleObjectFoundOpName))
                .as("Total handling time (repo level)")
                .isEqualTo(totalHandlingTime1);
        assertThat(info1.getOwnTime(connIdHandleObjectFoundOpName))
                .as("Own handling time (repo level)")
                .isEqualTo(ownHandlingTime1);

        and("components performance info is OK");
        assertGlobalOperationsPerformanceByStandardComponents()
                .display()
                .assertTotalTimeBetween(BasicComponentStructure.MODEL.getComponentName(), expectedOwnLow, expectedOwnHigh)
                .assertTotalTimeBetween(BasicComponentStructure.PROVISIONING.getComponentName(), 0, expectedNoOpOwnHigh)
                .assertTotalTimeBetween(BasicComponentStructure.UCF.getComponentName(), 0, expectedNoOpOwnHigh)
                .assertTotalTimeBetween(BasicComponentStructure.CONN_ID.getComponentName(), 0, expectedNoOpOwnHigh);

        when("searching for accounts iteratively again (noFetch)");
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
        var result2 = result.createSubresult(TestMisc.class.getName() + ".second");
        searchCreatedAccounts(true, task, result2);

        then("result is OK");
        DebugUtil.setDetailedDebugDump(true);
        displayDumpable("result", result2);
        DebugUtil.setDetailedDebugDump(previous);
        // To be adapted if the internals of the iterative search change
        var handlingResults2 = assertThatOperationResult(result2)
                .firstSubResultMatching(sr -> sr.getOperation().equals(modelSearchIterativeOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(provisioningSearchIterativeOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(repoCacheSearchIterativeOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(repoSearchObjectsIterativeOpName))
                .getAllSubResultsMatching(sr -> sr.getOperation().equals(repoHandleObjectFoundOpName));
        // The following two numbers should include summarized (hidden) subresults
        var totalHandlingTime2 = handlingResults2.stream()
                .mapToLong(sr -> sr.getMicroseconds())
                .sum();
        var ownHandlingTime2 = handlingResults2.stream()
                .mapToLong(sr -> sr.getOwnMicroseconds())
                .sum();

        and("performance info is OK");
        var info2 = assertGlobalOperationsPerformance()
                .display(SortBy.NAME)
                .assertInvocationCount(modelSearchIterativeOpName, 1)
                .assertTotalTimeBetween(modelSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(modelSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(provisioningSearchIterativeOpName, 1)
                .assertTotalTimeBetween(provisioningSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(provisioningSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(repoCacheSearchIterativeOpName, 1)
                .assertTotalTimeBetween(repoCacheSearchIterativeOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(repoCacheSearchIterativeOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(repoSearchObjectsIterativeOpName, 1)
                .assertTotalTimeBetween(repoSearchObjectsIterativeOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(repoSearchObjectsIterativeOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(repoHandleObjectFoundOpName, numCreatedAccounts)
                .assertTotalTimeBetween(repoHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(repoHandleObjectFoundOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(shadowManagerHandleObjectFoundOpName, numCreatedAccounts)
                .assertTotalTimeBetween(shadowManagerHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(shadowManagerHandleObjectFoundOpName, 0, expectedNoOpOwnHigh)
                .assertInvocationCount(modelHandleObjectFoundOpName, numCreatedAccounts)
                .assertTotalTimeBetween(modelHandleObjectFoundOpName, expectedTotalLow, expectedTotalHigh)
                .assertOwnTimeBetween(modelHandleObjectFoundOpName, expectedOwnLow, expectedOwnHigh)
                .get();
        // Checking that summarized (hidden) subresults are correctly included in the total time.
        assertThat(info2.getTotalTime(repoHandleObjectFoundOpName))
                .as("Total handling time (repo level)")
                .isEqualTo(totalHandlingTime2);
        assertThat(info2.getOwnTime(repoHandleObjectFoundOpName))
                .as("Own handling time (repo level)")
                .isEqualTo(ownHandlingTime2);

        and("components performance info is OK");
        assertGlobalOperationsPerformanceByStandardComponents()
                .display()
                .assertTotalTimeBetween(BasicComponentStructure.MODEL.getComponentName(), expectedOwnLow, expectedOwnHigh)
                .assertTotalTimeBetween(BasicComponentStructure.PROVISIONING.getComponentName(), 0, expectedNoOpOwnHigh)
                .assertTotalTimeBetween(BasicComponentStructure.REPOSITORY_CACHE.getComponentName(), 0, expectedNoOpOwnHigh)
                .assertTotalTimeBetween(BasicComponentStructure.REPOSITORY.getComponentName(), 0, expectedNoOpOwnHigh)
                .assertTotalTime(BasicComponentStructure.UCF.getComponentName(), null)
                .assertTotalTime(BasicComponentStructure.CONN_ID.getComponentName(), null);
    }

    private void searchCreatedAccounts(boolean noFetch, Task task, OperationResult result) throws CommonException {
        modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_PERF.get())
                        .queryFor(ACCOUNT, INTENT_DEFAULT)
                        .and()
                        .item(ShadowType.F_ATTRIBUTES, ICFS_NAME)
                        .startsWith("generated-account-")
                        .build(),
                (object, lResult) -> {
                    lResult.setMessage("Hello");
                    MiscUtil.sleepCatchingInterruptedException(OBJECT_PROCESSING_DELAY);
                    return true;
                },
                noFetch ? createNoFetchCollection() : null,
                task, result);
    }

    /**
     * "Get object" (from provisioning: both fetch and no-fetch) should provide correct operation names and execution times
     * for the whole chain.
     *
     * MID-10446
     */
    @Test
    public void test540OperationResultInHandlersForProvisioningGetObject() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var modelGetObjectOpName = ModelService.GET_OBJECT;
        var provisioningGetObjectOpName = ProvisioningService.OP_GET_OBJECT;
        var shadowsGetObjectOpName = "com.evolveum.midpoint.provisioning.impl.shadows.ShadowGetOperation.getResourceObject";
        var ucfFetchObjectOpName = ConnectorInstance.OP_FETCH_OBJECT;
        var connIdGetObjectOpName = ConnectorInstanceConnIdImpl.FACADE_OP_GET_OBJECT;
        var repoCacheGetObjectOpName = RepositoryCache.OP_GET_OBJECT_IMPL;
        var repoGetObjectOpName = isNativeRepository() ? "SqaleRepositoryService.getObject" : RepositoryService.GET_OBJECT;
        var expectedTimeMax = 500_000; // microseconds

        given("an account");
        var accountName = getTestNameShort();
        DummyObjectsCreator.accounts()
                .withObjectCount(1)
                .withNamePattern(accountName)
                .withController(RESOURCE_DUMMY_PERF.controller)
                .execute();

        var shadows = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(RESOURCE_DUMMY_PERF.get())
                        .queryFor(ACCOUNT, INTENT_DEFAULT)
                        .and()
                        .item(ShadowType.F_ATTRIBUTES, ICFS_NAME)
                        .eq(accountName)
                        .build(),
                null, task, result);
        var shadowOid = MiscUtil.extractSingletonRequired(shadows).getOid();

        when("getting the shadow from the resource");
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
        var result1 = result.createSubresult(TestMisc.class.getName() + ".first");
        modelService.getObject(ShadowType.class, shadowOid, null, task, result1);

        then("result is OK");
        var previous = DebugUtil.setDetailedDebugDump(true);
        displayDumpable("result", result1);
        DebugUtil.setDetailedDebugDump(previous);
        // To be adapted if the internals of the operation change
        assertThatOperationResult(result1)
                .firstSubResultMatching(sr -> sr.getOperation().equals(modelGetObjectOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(provisioningGetObjectOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(shadowsGetObjectOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(ucfFetchObjectOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(connIdGetObjectOpName));

        and("performance info is OK");
        assertGlobalOperationsPerformance()
                .display(SortBy.NAME);

        and("components performance info is OK");
        assertGlobalOperationsPerformanceByStandardComponents()
                .display()
                .assertTotalTimeBetween(BasicComponentStructure.MODEL.getComponentName(), 0, expectedTimeMax)
                .assertTotalTimeBetween(BasicComponentStructure.PROVISIONING.getComponentName(), 0, expectedTimeMax)
                .assertTotalTimeBetween(BasicComponentStructure.UCF.getComponentName(), 0, expectedTimeMax)
                .assertTotalTimeBetween(BasicComponentStructure.CONN_ID.getComponentName(), 0, expectedTimeMax);

        when("getting the shadow with 'noFetch' option");
        OperationsPerformanceMonitor.INSTANCE.clearGlobalPerformanceInformation();
        var result2 = result.createSubresult(TestMisc.class.getName() + ".second");
        modelService.getObject(ShadowType.class, shadowOid, createNoFetchCollection(), task, result2);

        then("result is OK");
        DebugUtil.setDetailedDebugDump(true);
        displayDumpable("result", result2);
        DebugUtil.setDetailedDebugDump(previous);
        // To be adapted if the internals of the operation change
        assertThatOperationResult(result2)
                .firstSubResultMatching(sr -> sr.getOperation().equals(modelGetObjectOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(provisioningGetObjectOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(repoCacheGetObjectOpName))
                .firstSubResultMatching(sr -> sr.getOperation().equals(repoGetObjectOpName));

        and("performance info is OK");
        assertGlobalOperationsPerformance()
                .display(SortBy.NAME);

        and("components performance info is OK");
        assertGlobalOperationsPerformanceByStandardComponents()
                .display()
                .assertTotalTimeBetween(BasicComponentStructure.MODEL.getComponentName(), 0, expectedTimeMax)
                .assertTotalTimeBetween(BasicComponentStructure.PROVISIONING.getComponentName(), 0, expectedTimeMax)
                .assertTotalTimeBetween(BasicComponentStructure.REPOSITORY_CACHE.getComponentName(), 0, expectedTimeMax)
                .assertTotalTimeBetween(BasicComponentStructure.REPOSITORY.getComponentName(), 0, expectedTimeMax)
                .assertTotalTime(BasicComponentStructure.UCF.getComponentName(), null)
                .assertTotalTime(BasicComponentStructure.CONN_ID.getComponentName(), null);
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

    /**
     * This checks that only proper {@link Referencable} instances (like {@link ObjectReferenceType}) are passed to scripts.
     *
     * See MID-10130.
     */
    @Test
    public void test710ReferenceVariableOnUserAdd() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user");
        addObject(USER_SCRIPTED, task, result);

        when("user is assigned to an org");
        assignOrg(USER_SCRIPTED.oid, ORG_ROOT.oid, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_SCRIPTED.oid)
                .assertOrganizationalUnits("add:null", "modify:ObjectReferenceType");
    }
}
