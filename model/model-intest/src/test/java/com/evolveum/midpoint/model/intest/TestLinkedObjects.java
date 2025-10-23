/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Various tests related to navigation the links between objects.
 * See also https://docs.evolveum.com/midpoint/reference/synchronization/linked-objects/.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLinkedObjects extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/linked");
    private static final File HW_TOKENS_DIR = new File("src/test/resources/linked/hw-tokens");
    private static final File GUMMI_DIR = new File("src/test/resources/linked/gummi");
    private static final File PROJECTS_DIR = new File("src/test/resources/linked/projects");
    private static final File ORGS_DIR = new File("src/test/resources/linked/orgs");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    //region Objects for HW tokens scenario
    private static final String ATTR_OWNER_NAME = "ownerName";
    private static final String ATTR_OWNER_EMAIL_ADDRESS = "ownerEmailAddress";

    private static final TestObject<ArchetypeType> ARCHETYPE_HW_TOKEN = TestObject.file(HW_TOKENS_DIR, "archetype-hw-token.xml", "21575364-d869-4b96-ac3f-b7b26e0e8540");
    private static final DummyTestResource RESOURCE_HW_TOKENS = new DummyTestResource(HW_TOKENS_DIR, "resource-hw-tokens.xml", "2730a64c-73fc-4c67-8bac-e40b4437931c", "hw-tokens",
            controller -> {
                controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                        ATTR_OWNER_NAME, String.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getGroupObjectClass(),
                        ATTR_OWNER_EMAIL_ADDRESS, String.class, false, false);
            });

    private static final TestObject<ServiceType> TOKEN_BLUE = TestObject.file(HW_TOKENS_DIR, "token-blue.xml", "0e5b7304-ea5c-438e-84d1-2b0ce40517ce");
    private static final TestObject<ServiceType> TOKEN_GREEN = TestObject.file(HW_TOKENS_DIR, "token-green.xml", "36c01826-c425-4da0-9e1e-023d807e1284");
    private static final TestObject<ServiceType> TOKEN_RED = TestObject.file(HW_TOKENS_DIR, "token-red.xml", "a449ad16-d13f-4a4d-99d4-2dd29ab18e65");

    private static final TestObject<UserType> USER_NIELS = TestObject.file(HW_TOKENS_DIR, "user-niels.xml", "139b2203-9675-422f-b5da-85001641c730");
    private static final TestObject<UserType> USER_PAUL = TestObject.file(HW_TOKENS_DIR, "user-paul.xml", "8dff246b-82f3-47b2-ad58-42d962c46c2c");
    private static final TestObject<UserType> USER_WERNER = TestObject.file(HW_TOKENS_DIR, "user-werner.xml", "98f31568-5a4d-413d-beed-394b90530033");
    //endregion

    //region Gummi scenario (devices and magic tokens)
    private static final TestObject<ArchetypeType> ARCHETYPE_USER = TestObject.file(GUMMI_DIR, "archetype-gummi-user.xml", "c46b1bcc-af43-44ee-a107-71f36e952cc5");
    private static final TestObject<ArchetypeType> ARCHETYPE_TOKEN = TestObject.file(GUMMI_DIR, "archetype-magic-token.xml", "e7bff8d1-cebd-4fbe-b935-64cfc2f22f52");
    private static final TestObject<ArchetypeType> ARCHETYPE_DEVICE = TestObject.file(GUMMI_DIR, "archetype-device.xml", "d6d90e2c-ad25-4f7f-a0e1-2f5fac03b402");
    private static final TestObject<ArchetypeType> ARCHETYPE_DUMMY = TestObject.file(GUMMI_DIR, "archetype-dummy.xml", "c1e5dc3d-dc84-4e71-b916-814b71bb264a");

    private static final TestObject<ServiceType> SERVICE_MEDALLION = TestObject.file(GUMMI_DIR, "service-medallion.xml", "8734f795-f6b4-4cc5-843b-6307aaf88f9d");
    private static final TestObject<ServiceType> SERVICE_WHISTLE = TestObject.file(GUMMI_DIR, "service-whistle.xml", "40c18026-ca88-4bda-ab0b-f1a2a9c94818");
    private static final TestObject<ServiceType> SERVICE_SWORD = TestObject.file(GUMMI_DIR, "service-sword.xml", "c64ee819-6dcd-4ad2-a91a-303fb0aed29e");
    private static final TestObject<ServiceType> SERVICE_AXE = TestObject.file(GUMMI_DIR, "service-axe.xml", "90a3a6a0-07ea-4b2d-b800-ccdf4e7dea78");

    private static final TestObject<UserType> USER_CAVIN = TestObject.file(GUMMI_DIR, "user-cavin.xml", "04753be2-f0f1-4292-8f24-48b0eedfcce3");
    private static final TestObject<UserType> USER_ZUMMI = TestObject.file(GUMMI_DIR, "user-zummi.xml", "3224fccd-27fa-45b5-8cf3-497a0d2dd892");
    private static final TestObject<UserType> USER_GRUFFY = TestObject.file(GUMMI_DIR, "user-gruffy.xml", "30b59b40-2875-410d-8731-482743eb6de2");
    private static final TestObject<UserType> USER_GRAMMI = TestObject.file(GUMMI_DIR, "user-grammi.xml", "041d0c03-c322-4e0d-89ba-a2d49b732674");
    private static final TestObject<UserType> USER_CUBBY = TestObject.file(GUMMI_DIR, "user-cubby.xml", "7b8f2e00-a49e-40ff-a4bd-11b70bac89d3");
    //endregion

    // region Projects scenario
    private static final TestObject<ArchetypeType> ARCHETYPE_PROJECT = TestObject.file(PROJECTS_DIR, "archetype-project.xml", "4d3280a1-6514-4984-ac2c-7e56c05af258");
    private static final TestObject<ArchetypeType> ARCHETYPE_PROJECT_USERS = TestObject.file(PROJECTS_DIR, "archetype-project-users.xml", "3af67ba4-183f-45e7-887e-4ae5ddff4cdf");
    private static final TestObject<ArchetypeType> ARCHETYPE_PROJECT_GROUPS = TestObject.file(PROJECTS_DIR, "archetype-project-groups.xml", "a85bddc9-4ff0-475f-8ccc-17f9038d4ce1");
    // endregion

    // region Orgs scenario
    private static final TestObject<ArchetypeType> ARCHETYPE_DELETION_SAFE_ORG = TestObject.file(
            ORGS_DIR, "archetype-deletion-safe-org.xml", "b8a973e0-f645-490b-a2ac-b69bd4103bf8");
    private static final TestObject<ArchetypeType> ARCHETYPE_DELETION_SAFE_ORG_ASYNC = TestObject.file(
            ORGS_DIR, "archetype-deletion-safe-org-async.xml", "08893534-3ab3-4209-8702-d21e5492813f");
    // endregion

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Initialization for HW tokens scenario

        addObject(ARCHETYPE_HW_TOKEN, initTask, initResult);
        initDummyResource(RESOURCE_HW_TOKENS, initTask, initResult);

        addObject(TOKEN_BLUE, initTask, initResult);
        addObject(TOKEN_GREEN, initTask, initResult);
        addObject(TOKEN_RED, initTask, initResult);

        addObject(USER_NIELS, initTask, initResult);
        addObject(USER_PAUL, initTask, initResult);

        // Initialization for Gummi scenario

        addObject(ARCHETYPE_USER, initTask, initResult);
        addObject(ARCHETYPE_TOKEN, initTask, initResult);
        addObject(ARCHETYPE_DEVICE, initTask, initResult);
        addObject(ARCHETYPE_DUMMY, initTask, initResult);

        addObject(SERVICE_MEDALLION, initTask, initResult);
        addObject(SERVICE_WHISTLE, initTask, initResult);
        addObject(SERVICE_SWORD, initTask, initResult);
        addObject(SERVICE_AXE, initTask, initResult);

        addObject(USER_CAVIN, initTask, initResult);
        addObject(USER_ZUMMI, initTask, initResult);
        addObject(USER_GRUFFY, initTask, initResult);
        addObject(USER_GRAMMI, initTask, initResult);
        addObject(USER_CUBBY, initTask, initResult);

        // Initialization for Projects scenario

        addObject(ARCHETYPE_PROJECT, initTask, initResult);
        addObject(ARCHETYPE_PROJECT_USERS, initTask, initResult);
        addObject(ARCHETYPE_PROJECT_GROUPS, initTask, initResult);

        // Initialization for Orgs scenario

        addObject(ARCHETYPE_DELETION_SAFE_ORG, initTask, initResult);
        addObject(ARCHETYPE_DELETION_SAFE_ORG_ASYNC, initTask, initResult);

        addObject(CommonInitialObjects.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK, initTask, initResult);

//        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_LOGGING;
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test000SanityForHwTokens() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertService(TOKEN_BLUE.oid, "after init")
                .display()
                .assertLiveLinks(1)
                .getObjectable();
        refresh(TOKEN_BLUE, result);
        assertHwToken(TOKEN_BLUE, "blue", null, null);

        assertService(TOKEN_GREEN.oid, "after init")
                .display()
                .assertLiveLinks(1);
        refresh(TOKEN_GREEN, result);
        assertHwToken(TOKEN_GREEN, "green", null, null);

        assertService(TOKEN_RED.oid, "after init")
                .display()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .assertLiveLinks(1);
        refresh(TOKEN_RED, result);
        assertHwToken(TOKEN_RED, "red", null, null);
    }

    private void assertHwToken(TestObject<ServiceType> token, String desc, String expectedOwnerName, String expectedOwnerEmailAddress)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyGroup dummyGroup = RESOURCE_HW_TOKENS.controller.getDummyResource().getGroupByName(token.getObjectable().getName().getOrig());
        displayDumpable("hw token dummy group", dummyGroup);
        assertThat(dummyGroup).as(desc + " group").isNotNull();
        assertThat(dummyGroup.getAttributeValue(ATTR_OWNER_NAME)).as(desc + " owner name").isEqualTo(expectedOwnerName);
        assertThat(dummyGroup.getAttributeValue(ATTR_OWNER_EMAIL_ADDRESS)).as(desc + " owner email").isEqualTo(expectedOwnerEmailAddress);
    }

    @Test
    public void test000SanityForGummi() throws Exception {
        assertUser(USER_CAVIN.oid, "after init")
                .assertOrganizationalUnits()
                .display();
        assertService(SERVICE_MEDALLION.oid, "after init")
                .assertDescription("Not held")
                .display();
        assertService(SERVICE_WHISTLE.oid, "after init")
                .assertDescription("Not held")
                .display();
        assertService(SERVICE_SWORD.oid, "after init")
                .assertDescription("Not used")
                .display();
        assertService(SERVICE_AXE.oid, "after init")
                .assertDescription("Not used")
                .display();
    }

    /**
     * Let's give blue HW token to Niels Bohr.
     */
    @Test
    public void test100GiveBlueTokenToNiels() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assign(USER_NIELS, TOKEN_BLUE, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertHwToken(TOKEN_BLUE, "blue", "niels", "niels.bohr@mail.net");
    }

    /**
     * Let's take blue HW token from Niels Bohr and give him to Paul Dirac.
     */
    @Test
    public void test110GiveBlueTokenToPaul() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        refresh(USER_NIELS, result);
        unassignIfSingle(USER_NIELS, TOKEN_BLUE, SchemaConstants.ORG_DEFAULT, null, task, result);

        assertHwToken(TOKEN_BLUE, "blue", null, null);

        assign(USER_PAUL, TOKEN_BLUE, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertHwToken(TOKEN_BLUE, "blue", "paul", "pdi@m.org");
    }

    /**
     * Let's give red HW token (disabled) to Paul Dirac.
     */
    @Test
    public void test120GiveRedTokenToPaul() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assign(USER_PAUL, TOKEN_RED, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertHwToken(TOKEN_BLUE, "blue", "paul", "pdi@m.org");
        assertHwToken(TOKEN_RED, "red", "paul", "pdi@m.org");
    }

    /**
     * Move Paul's mailbox. Both blue (enabled) and red (disabled) tokens should be updated.
     */
    @Test
    public void test130MovePaulMailbox() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_EMAIL_ADDRESS).replace("paul.dirac@mail.net")
                .asObjectDelta(USER_PAUL.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertHwToken(TOKEN_BLUE, "blue", "paul", "paul.dirac@mail.net");
        assertHwToken(TOKEN_RED, "red", "paul", "paul.dirac@mail.net");
    }

    /**
     * Let's give red token (disabled) from Dirac to Bohr.
     */
    @Test
    public void test140GiveRedTokenToNiels() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        refresh(USER_PAUL, result);
        unassignIfSingle(USER_PAUL, TOKEN_RED, SchemaConstants.ORG_DEFAULT, null, task, result);

        assertHwToken(TOKEN_RED, "red", null, null);

        assign(USER_NIELS, TOKEN_RED, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertHwToken(TOKEN_BLUE, "blue", "paul", "paul.dirac@mail.net");
        assertHwToken(TOKEN_RED, "red", "niels", "niels.bohr@mail.net");
    }

    /**
     * Now for something more harsh: let's create Werner with the token already assigned.
     */
    @Test
    public void test150CreateWernerWithGreenToken() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addObject(USER_WERNER, task, result);

        then();
        assertSuccess(result);

        assertHwToken(TOKEN_GREEN, "green", "werner", "anything@somewhere.maybe");
    }

    /**
     * And the final twist: Werner Heisenberg disappears with green token being assigned.
     */
    @Test
    public void test160DeleteWernerWithGreenToken() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        deleteObject(UserType.class, USER_WERNER.oid, task, result);

        then();
        assertSuccess(result);

        assertHwToken(TOKEN_GREEN, "green", null, null);
    }

    /**
     * Cavin's grandfather gives medallion to him.
     * We should observe correct data on both the medallion and its holder.
     */
    @Test
    public void test200GiveMedallionToCavin() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assign(USER_CAVIN, SERVICE_MEDALLION, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CAVIN.oid)
                .assertOrganizationalUnits("medallion holders");

        assertServiceAfter(SERVICE_MEDALLION.oid)
                .assertDescription("Held by cavin (Cavin)");
    }

    /**
     * Cavin is being entitled.
     * We should observe update on the medallion.
     */
    @Test
    public void test210EntitleCavin() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("Sir Cavin"))
                .asObjectDelta(USER_CAVIN.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CAVIN.oid)
                .assertFullName("Sir Cavin")
                .assertOrganizationalUnits("medallion holders");

        assertServiceAfter(SERVICE_MEDALLION.oid)
                .assertDescription("Held by cavin (Sir Cavin)");
    }

    /**
     * Cavin passes the medallion to Zummi.
     * Phase one is that it is unassigned from Cavin.
     */
    @Test
    public void test220PassMedallionToZummiPhaseOne() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refresh(USER_CAVIN, result);

        when();
        unassignIfSingle(USER_CAVIN, SERVICE_MEDALLION, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CAVIN.oid)
                .assertOrganizationalUnits();

        assertServiceAfter(SERVICE_MEDALLION.oid)
                .assertDescription("Not held");
    }

    /**
     * Cavin passes the medallion to Zummi.
     * Phase two is that it is assigned to Zummi.
     */
    @Test
    public void test230PassMedallionToZummiPhaseTwo() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assign(USER_ZUMMI, SERVICE_MEDALLION, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_ZUMMI.oid)
                .assertOrganizationalUnits("medallion holders");

        assertServiceAfter(SERVICE_MEDALLION.oid)
                .assertDescription("Held by zummi (Zummi Gummi)");
    }

    /**
     * Medallion is renamed. So its holder should be updated.
     */
    @Test
    public void test240RenameMedallion() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectDelta<ServiceType> delta = deltaFor(ServiceType.class)
                .item(ServiceType.F_NAME).replace(PolyString.fromOrig("gummi-medallion"))
                .asObjectDelta(SERVICE_MEDALLION.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_ZUMMI.oid)
                .assertOrganizationalUnits("gummi-medallion holders");
    }

    /**
     * Give axe to Gruffy.
     */
    @Test
    public void test250GiveAxeToGruffy() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assign(USER_GRUFFY, SERVICE_AXE, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_GRUFFY.oid)
                .assertOrganizations("axe users");

        assertServiceAfter(SERVICE_AXE.oid)
                .assertDescription("Used by gruffy (Gruffy Gummi)");
    }

    /**
     * Assign disabled device.
     */
    @Test
    public void test310AssignDisabledSword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assign(USER_CUBBY, SERVICE_SWORD, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CUBBY.oid)
                .assertOrganizations("sword users");

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertDescription("Used by cubby (Cubby Gummi)");
    }

    /**
     * Sword is renamed. So its holder should be updated, even if the sword is disabled.
     */
    @Test
    public void test320RenameDisabledSword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectDelta<ServiceType> delta = deltaFor(ServiceType.class)
                .item(ServiceType.F_NAME).replace(PolyString.fromOrig("wooden-sword"))
                .asObjectDelta(SERVICE_SWORD.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CUBBY.oid)
                .assertOrganizations("wooden-sword users");

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertDescription("Used by cubby (Cubby Gummi)");
    }

    /**
     * Sword user is renamed. So it should be updated, even if it is disabled.
     */
    @Test
    public void test330RenameCubbyUsingDisabledSword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig("Little Cubby Gummi"))
                .asObjectDelta(USER_CUBBY.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CUBBY.oid)
                .assertFullName("Little Cubby Gummi")
                .assertOrganizations("wooden-sword users");

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertDescription("Used by cubby (Little Cubby Gummi)");
    }

    /**
     * Unassign sword from cubby.
     */
    @Test
    public void test340UnassignSwordFromCubby() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refresh(USER_CUBBY, result);

        when();
        unassignIfSingle(USER_CUBBY, SERVICE_SWORD, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CUBBY.oid)
                .assertFullName("Little Cubby Gummi")
                .assertOrganizations();

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertDescription("Not used");
    }

    /**
     * Assign sword to cubby again.
     */
    @Test
    public void test350AssignSwordToCubbyAgain() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assign(USER_CUBBY, SERVICE_SWORD, SchemaConstants.ORG_DEFAULT, null, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_CUBBY.oid)
                .assertFullName("Little Cubby Gummi")
                .assertOrganizations("wooden-sword users");

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertDescription("Used by cubby (Little Cubby Gummi)");
    }

    /**
     * Delete cubby (sigh).
     */
    @Test
    public void test360DeleteCubby() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        deleteObject(UserType.class, USER_CUBBY.oid, task, result);

        then();
        assertSuccess(result);

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertDescription("Not used");
    }

    /**
     * Add cubby but with sword.
     */
    @Test
    public void test370AddCubbyWithSword() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addObject(USER_CUBBY, task, result, cubby ->
                cubby.asObjectable()
                        .beginAssignment().targetRef(SERVICE_SWORD.oid, ServiceType.COMPLEX_TYPE)
        );

        then();
        assertSuccess(result);

        assertUserAfter(USER_CUBBY.oid)
                .assertFullName("Cubby Gummi")
                .assertOrganizations("wooden-sword users");

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertDescription("Used by cubby (Cubby Gummi)");
    }

    /**
     * Change archetype of the sword from "device" to "dummy" (that does nothing).
     *
     * MID-8060
     */
    @Test
    public void test380MakeSwordDummy() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("sword is switched to dummy archetype");
        executeChanges(
                prismContext.deltaFor(ServiceType.class)
                        .item(ServiceType.F_ASSIGNMENT)
                        .delete(new AssignmentType()
                                .targetRef(ARCHETYPE_DEVICE.oid, ArchetypeType.COMPLEX_TYPE))
                        .add(new AssignmentType()
                                .targetRef(ARCHETYPE_DUMMY.oid, ArchetypeType.COMPLEX_TYPE))
                        .asObjectDelta(SERVICE_SWORD.oid),
                null, task, result);

        then();
        assertSuccess(result);

        assertServiceAfter(SERVICE_SWORD.oid)
                .assertArchetypeRef(ARCHETYPE_DUMMY.oid)
                .assertDescription(null); // The mapping disappeared, so its value went to minus set.

        assertUserAfter(USER_CUBBY.oid)
                .assertOrganizations();
    }

    /**
     * Change archetype of a new service from "dummy" to "device".
     *
     * MID-8060
     */
    @Test
    public void test390SwitchServiceArchetype() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a spoon with dummy archetype");
        ServiceType spoon390 = new ServiceType()
                .name("spoon390")
                .assignment(new AssignmentType()
                        .targetRef(ARCHETYPE_DUMMY.oid, ArchetypeType.COMPLEX_TYPE));
        addObject(spoon390, task, result);

        and("a user with the spoon");
        UserType user390 = new UserType()
                .name("user390")
                .fullName("User 390")
                .assignment(new AssignmentType()
                        .targetRef(ARCHETYPE_USER.oid, ArchetypeType.COMPLEX_TYPE))
                .assignment(new AssignmentType()
                        .targetRef(spoon390.getOid(), ServiceType.COMPLEX_TYPE));
        addObject(user390, task, result);

        when("spoon is switched to 'device' archetype");
        executeChanges(
                prismContext.deltaFor(ServiceType.class)
                        .item(ServiceType.F_ASSIGNMENT)
                        .delete(new AssignmentType()
                                .targetRef(ARCHETYPE_DUMMY.oid, ArchetypeType.COMPLEX_TYPE))
                        .add(new AssignmentType()
                                .targetRef(ARCHETYPE_DEVICE.oid, ArchetypeType.COMPLEX_TYPE))
                        .asObjectDelta(spoon390.getOid()),
                null, task, result);

        then();
        assertSuccess(result);

        assertServiceAfter(spoon390.getOid())
                .assertArchetypeRef(ARCHETYPE_DEVICE.oid)
                .assertDescription("Used by user390 (User 390)");

        assertUserAfter(user390.getOid())
                .assertOrganizations("spoon390 users");
    }

    /**
     * Creating archetyped device "from scratch".
     *
     * MID-8059
     */
    @Test
    public void test400CreateArchetypedDevice() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("archetyped device is created");
        ServiceType hammer400 = new ServiceType()
                .name("hammer400")
                .assignment(new AssignmentType()
                        .targetRef(ARCHETYPE_DEVICE.oid, ArchetypeType.COMPLEX_TYPE));
        addObject(hammer400, task, result);

        then("it is there");
        assertSuccess(result);

        assertServiceAfter(hammer400.getOid())
                .assertArchetypeRef(ARCHETYPE_DEVICE.oid)
                .assertDescription("Not used");
    }

    /**
     * Creates a project. Two children should be created automatically.
     */
    @Test
    public void test700CreateProject() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        OrgType ariane = new OrgType()
                .name("ariane")
                .beginAssignment()
                .targetRef(ARCHETYPE_PROJECT.oid, ArchetypeType.COMPLEX_TYPE)
                .end();
        addObject(ariane, task, result);

        then();
        assertSuccess(result);

        assertOrgAfter(ariane.getOid())
                .assertName("ariane");

        assertOrgByName("ariane_users", "after")
                .display()
                .assertParentOrgRefs(ariane.getOid());

        assertOrgByName("ariane_groups", "after")
                .display()
                .assertParentOrgRefs(ariane.getOid());
    }

    /**
     * Renames a project. Children should be renamed automatically.
     */
    @Test
    public void test710RenameProject() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        PrismObject<OrgType> ariane = assertSingleObjectByName(OrgType.class, "ariane", task, result);
        ObjectDelta<OrgType> delta = deltaFor(OrgType.class)
                .item(OrgType.F_NAME).replace(PolyString.fromOrig("ariane5"))
                .asObjectDelta(ariane.getOid());

        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);

        assertOrgAfter(ariane.getOid())
                .assertName("ariane5");

        assertOrgByName("ariane5_users", "after")
                .display()
                .assertParentOrgRefs(ariane.getOid());

        assertOrgByName("ariane5_groups", "after")
                .display()
                .assertParentOrgRefs(ariane.getOid());
    }

    /**
     * Deletes a project. Children should be deleted automatically.
     */
    @Test
    public void test720DeleteProject() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        PrismObject<OrgType> ariane5 = assertSingleObjectByName(OrgType.class, "ariane5", task, result);
        deleteObject(OrgType.class, ariane5.getOid(), task, result);

        then();
        assertSuccess(result);

        assertNoObjectByName(OrgType.class, "ariane5", task, result);
        assertNoObjectByName(OrgType.class, "ariane5_users", task, result);
        assertNoObjectByName(OrgType.class, "ariane5_groups", task, result);
    }

    /**
     * Deletes org and checks if it's unassigned from its former members.
     * MID-8366
     */
    @Test
    public void test800DeleteOrg() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        OrgType root = new OrgType()
                .name("root800")
                .beginAssignment()
                .targetRef(ARCHETYPE_DELETION_SAFE_ORG.oid, ArchetypeType.COMPLEX_TYPE)
                .end();
        addObject(root, task, result);

        OrgType child = new OrgType()
                .name("child800")
                .beginAssignment()
                .targetRef(root.getOid(), OrgType.COMPLEX_TYPE)
                .end();
        addObject(child, task, result);

        UserType user = new UserType()
                .name("user800")
                .beginAssignment()
                .targetRef(root.getOid(), OrgType.COMPLEX_TYPE)
                .end();
        addObject(user, task, result);

        when();
        deleteObject(OrgType.class, root.getOid(), task, result);

        then();
        assertSuccess(result); // in fact this is HANDLED_ERROR because of missing parent org

        assertOrgAfter(child.getOid())
                .assertAssignments(0);

        assertUserAfter(user.getOid())
                .assertAssignments(0);
    }

    /**
     * Deletes org and checks if it's (asynchronously) unassigned from its former members.
     * MID-8366
     */
    @Test
    public void test810DeleteOrgAsync() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        OrgType root = new OrgType()
                .name("root810")
                .beginAssignment()
                .targetRef(ARCHETYPE_DELETION_SAFE_ORG_ASYNC.oid, ArchetypeType.COMPLEX_TYPE)
                .end();
        addObject(root, task, result);

        OrgType child = new OrgType()
                .name("child810")
                .beginAssignment()
                .targetRef(root.getOid(), OrgType.COMPLEX_TYPE)
                .end();
        addObject(child, task, result);

        UserType user = new UserType()
                .name("user810")
                .beginAssignment()
                .targetRef(root.getOid(), OrgType.COMPLEX_TYPE)
                .end();
        addObject(user, task, result);

        when();
        deleteObject(OrgType.class, root.getOid(), task, result);

        then();
        assertSuccess(result);

        String taskOid = result.findTaskOid();
        assertThat(taskOid).as("background task OID").isNotNull();

        waitForTaskFinish(taskOid);
        assertTask(taskOid, "after")
                .display()
                .assertClosed()
                .assertSuccess();

        assertOrgAfter(child.getOid())
                .assertAssignments(0);

        assertUserAfter(user.getOid())
                .assertAssignments(0);
    }
}
