/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMisc extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/misc");

    protected static final File ROLE_IMPORT_FILTERS_FILE = new File(TEST_DIR, "role-import-filters.xml");
    protected static final String ROLE_IMPORT_FILTERS_OID = "aad19b9a-d511-11e7-8bf7-cfecde275e59";

    protected static final File ROLE_SHIP_FILE = new File(TEST_DIR, "role-ship.xml");
    protected static final String ROLE_SHIP_OID = "bbd19b9a-d511-11e7-8bf7-cfecde275e59";

    protected static final File RESOURCE_SCRIPTY_FILE = new File(TEST_DIR, "resource-dummy-scripty.xml");
    protected static final String RESOURCE_SCRIPTY_OID = "399f5308-0447-11e8-91e9-a7f9c4100ffb";
    protected static final String RESOURCE_DUMMY_SCRIPTY_NAME = "scripty";

    public static final byte[] KEY = { 0x01, 0x02, 0x03, 0x04, 0x05 };

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
    }

    @Test
    public void test100GetRepositoryDiag() {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        RepositoryDiag diag = modelDiagnosticService.getRepositoryDiag(task, result);

        // THEN
        then();
        displayValue("Diag", diag);
        assertSuccess(result);

        assertEquals("Wrong implementationShortName", "SQL", diag.getImplementationShortName());
        assertNotNull("Missing implementationDescription", diag.getImplementationDescription());
        // TODO
    }

    @Test
    public void test110RepositorySelfTest() {
        // GIVEN
        Task task = getTestTask();

        // WHEN
        when();
        OperationResult testResult = modelDiagnosticService.repositorySelfTest(task);

        // THEN
        then();
        display("Repository self-test result", testResult);
        TestUtil.assertSuccess("Repository self-test result", testResult);

        // TODO: check the number of tests, etc.
    }

    @Test
    public void test200ExportUsers() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null,
                SelectorOptions.createCollection(GetOperationOptions.createRaw()), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEquals("Unexpected number of users", 6, users.size());
        for (PrismObject<UserType> user : users) {
            display("Exporting user", user);
            assertNotNull("Null definition in " + user, user.getDefinition());
            displayDumpable("Definition", user.getDefinition());
            String xmlString = prismContext.serializerFor(PrismContext.LANG_XML).serialize(user);
            displayValue("Exported user", xmlString);

            Document xmlDocument = DOMUtil.parseDocument(xmlString);
            Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
            Validator validator = javaxSchema.newValidator();
            validator.setResourceResolver(prismContext.getEntityResolver());
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_NAME)
                .eq("JacK")
                .matchingNorm()
                .build();

        // WHEN
        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_NAME)
                .eq("JacK")
                .matchingCaseIgnore()
                .build();

        try {
            // WHEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER)
                .eq("EMP1234") // Real value is "emp123"
                .matchingCaseIgnore()
                .build();

        // WHEN
        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        // THEN
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
    @Test
    public void test216SearchUsersMatchingRulesStringNorm() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = queryFor(UserType.class)
                .item(UserType.F_EMPLOYEE_NUMBER)
                .eq("EMP1234") // Real value is "emp123"
                .matchingNorm()
                .build();

        // WHEN
        when();
        List<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, query, null, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, getExtensionPath(PIRACY_KEY), task, result, KEY);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        PrismAsserts.assertPropertyValue(userAfter, getExtensionPath(PIRACY_KEY), KEY);
    }

    @Test
    public void test310AddUserClean() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_CLEAN_NAME, USER_CLEAN_GIVEN_NAME, USER_CLEAN_FAMILY_NAME, true);

        // WHEN
        when();
        addObject(userBefore, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(userCleanOid, getExtensionPath(PIRACY_BINARY_ID), task, result, KEY);

        // THEN
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
        // WHEN
        when();
        List<RelationDefinitionType> relations = modelInteractionService.getRelationDefinitions();

        // THEN
        then();
        display("Relations", relations);
        assertRelationDef(relations, SchemaConstants.ORG_MANAGER, "RelationTypes.manager");
        assertRelationDef(relations, SchemaConstants.ORG_OWNER, "RelationTypes.owner");
        assertEquals("Unexpected number of relation definitions", 7, relations.size());
    }

    /**
     * MID-3879
     */
    @Test
    public void test400ImportRoleWithFilters() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modelService.importObjectsFromFile(ROLE_IMPORT_FILTERS_FILE, null, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_SCRIPTY_OID, null, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);

        // WHEN
        when();
        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);

        // WHEN
        when();
        PrismObject<ShadowType> accountShadow = modelService.getObject(ShadowType.class, accountOid, null, task, result);

        // THEN
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 1);
        String accountOid = getSingleLinkOid(userBefore);
        PrismObject<ResourceType> resourceBefore = getObject(ResourceType.class, RESOURCE_SCRIPTY_OID);
        displayValue("Resource version before", resourceBefore.getVersion());

        // WHEN
        when();
        modifyObjectReplaceProperty(ResourceType.class, RESOURCE_SCRIPTY_OID,
                ResourceType.F_DESCRIPTION, null, task, result, "Whatever");

        // THEN
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
        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_SHIP_OID);

        //THEN
        then();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User before", userAfter);
        assertAssignments(userAfter, 2);
        assertLinks(userAfter, 1);

        PrismReference linkRef = userAfter.findReference(UserType.F_LINK_REF);
        assertFalse(linkRef.isEmpty());

//        PrismObject<ShadowType> shadowModel = getShadowModel(linkRef.getOid());

        assertDummyAccountAttribute(RESOURCE_DUMMY_SCRIPTY_NAME, USER_JACK_USERNAME, "ship", "ship");

    }

    @Test
    public void test601jackUnassignResourceAccount() throws Exception {
        // GIVEN
        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 2);

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_SCRIPTY_OID, null);

        //THEN
        then();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLinks(userAfter, 1);
    }

    /**
     * MID-4504
     * midpoint.getLinkedShadow fails recomputing without throwing exception during shadow delete
     * <p>
     * first assign role ship, the ship attribute in the role has mapping with calling midpoint.getLinkedShadow()
     */
    @Test
    public void test602jackUnssigndRoleShip() throws Exception {
        when();
        unassignRole(USER_JACK_OID, ROLE_SHIP_OID);

        then();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User before", userAfter);
        assertAssignments(userAfter, 0);
        assertLinks(userAfter, 0);
    }

}
