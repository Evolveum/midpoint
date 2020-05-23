/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.PredefinedTestMethodTracing;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Various tests related to navigation the links between objects.
 * See also https://wiki.evolveum.com/display/midPoint/Linked+objects.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLinkedObjects extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/linked");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<ObjectTemplateType> TEMPLATE_USER = new TestResource<>(TEST_DIR, "template-user.xml", "241afdcc-26eb-4417-a4df-a8c1add06e84");
    private static final TestResource<ObjectTemplateType> TEMPLATE_DEVICE = new TestResource<>(TEST_DIR, "template-device.xml", "e0d5d585-da74-4523-b4f5-78cb54c0dccd");

    private static final TestResource<ArchetypeType> ARCHETYPE_USER = new TestResource<>(TEST_DIR, "archetype-user.xml", "c46b1bcc-af43-44ee-a107-71f36e952cc5");
    private static final TestResource<ArchetypeType> ARCHETYPE_TOKEN = new TestResource<>(TEST_DIR, "archetype-token.xml", "e7bff8d1-cebd-4fbe-b935-64cfc2f22f52");
    private static final TestResource<ArchetypeType> ARCHETYPE_DEVICE = new TestResource<>(TEST_DIR, "archetype-device.xml", "d6d90e2c-ad25-4f7f-a0e1-2f5fac03b402");

    private static final TestResource<ServiceType> SERVICE_MEDALLION = new TestResource<>(TEST_DIR, "service-medallion.xml", "8734f795-f6b4-4cc5-843b-6307aaf88f9d");
    private static final TestResource<ServiceType> SERVICE_WHISTLE = new TestResource<>(TEST_DIR, "service-whistle.xml", "40c18026-ca88-4bda-ab0b-f1a2a9c94818");
    private static final TestResource<ServiceType> SERVICE_SWORD = new TestResource<>(TEST_DIR, "service-sword.xml", "c64ee819-6dcd-4ad2-a91a-303fb0aed29e");
    private static final TestResource<ServiceType> SERVICE_AXE = new TestResource<>(TEST_DIR, "service-axe.xml", "90a3a6a0-07ea-4b2d-b800-ccdf4e7dea78");

    private static final TestResource<UserType> USER_CAVIN = new TestResource<>(TEST_DIR, "user-cavin.xml", "04753be2-f0f1-4292-8f24-48b0eedfcce3");
    private static final TestResource<UserType> USER_ZUMMI = new TestResource<>(TEST_DIR, "user-zummi.xml", "3224fccd-27fa-45b5-8cf3-497a0d2dd892");
    private static final TestResource<UserType> USER_GRUFFY = new TestResource<>(TEST_DIR, "user-gruffy.xml", "30b59b40-2875-410d-8731-482743eb6de2");
    private static final TestResource<UserType> USER_GRAMMI = new TestResource<>(TEST_DIR, "user-grammi.xml", "041d0c03-c322-4e0d-89ba-a2d49b732674");
    private static final TestResource<UserType> USER_CUBBY = new TestResource<>(TEST_DIR, "user-cubby.xml", "7b8f2e00-a49e-40ff-a4bd-11b70bac89d3");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(TEMPLATE_USER, initTask, initResult);
        addObject(TEMPLATE_DEVICE, initTask, initResult);

        addObject(ARCHETYPE_USER, initTask, initResult);
        addObject(ARCHETYPE_TOKEN, initTask, initResult);
        addObject(ARCHETYPE_DEVICE, initTask, initResult);

        addObject(SERVICE_MEDALLION, initTask, initResult);
        addObject(SERVICE_WHISTLE, initTask, initResult);
        addObject(SERVICE_SWORD, initTask, initResult);
        addObject(SERVICE_AXE, initTask, initResult);

        addObject(USER_CAVIN, initTask, initResult);
        addObject(USER_ZUMMI, initTask, initResult);
        addObject(USER_GRUFFY, initTask, initResult);
        addObject(USER_GRAMMI, initTask, initResult);
        addObject(USER_CUBBY, initTask, initResult);

//        predefinedTestMethodTracing = PredefinedTestMethodTracing.MODEL_LOGGING;
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test000Sanity() throws Exception {
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
     * Cavin's grandfather gives medallion to him.
     * We should observe correct data on both the medallion and its holder.
     */
    @Test
    public void test100GiveMedallionToCavin() throws Exception {
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
    public void test110EntitleCavin() throws Exception {
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
    public void test120PassMedallionToZummiPhaseOne() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refresh(USER_CAVIN, task, result);

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
    public void test130PassMedallionToZummiPhaseTwo() throws Exception {
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
    public void test140RenameMedallion() throws Exception {
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
    public void test150GiveAxeToGruffy() throws Exception {
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
     * Weird case: In a user -> token link, the token is deactivated.
     * User should be no longer marked as token holder.
     */
    @Test
    public void test200DeactivateMedallion() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        ObjectDelta<ServiceType> delta = deltaFor(ServiceType.class)
                .item(ServiceType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
                .asObjectDelta(SERVICE_MEDALLION.oid);
        executeChanges(delta, null, task, result);

        then();
        assertSuccess(result);
        recomputeUser(USER_ZUMMI.oid); // temporary

        assertUserAfter(USER_ZUMMI.oid)
                .assertOrganizationalUnits();
    }
    /**
     * Assign disabled device.
     */
    @Test
    public void test210AssignDisabledSword() throws Exception {
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
    public void test220RenameDisabledSword() throws Exception {
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
    public void test230RenameCubbyUsingDisabledSword() throws Exception {
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
    public void test240UnassignSwordFromCubby() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refresh(USER_CUBBY, task, result);

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
     * Whistle is held by test user since creation - to check on the ordering of assignments.
     * (Whistle first, User second). TEMPORARY
     */
    @Test
    public void test300CreateNewWhistleHolder() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        UserType testUser = new UserType(prismContext)
                .name(getTestNameShort())
                .fullName("Test User")
                .organizationalUnit("castle holders")
                .beginAssignment()
                    .targetRef(SERVICE_WHISTLE.oid, ServiceType.COMPLEX_TYPE)
                .<UserType>end()
                .beginAssignment()
                    .targetRef(ARCHETYPE_USER.oid, ArchetypeType.COMPLEX_TYPE)
                .end();
        addObject(testUser.asPrismObject(), task, result);

        then();
        assertSuccess(result);

        assertUserAfter(testUser.getOid())
                .assertOrganizationalUnits("whistle holders");

        assertServiceAfter(SERVICE_WHISTLE.oid)
                .assertDescription("Held by " + testUser.getName().getOrig() + " (Test User)");

        recomputeUser(testUser.getOid(), task, result);

        assertUserAfter(testUser.getOid())
                .assertOrganizationalUnits("whistle holders");

        assertServiceAfter(SERVICE_WHISTLE.oid)
                .assertDescription("Held by " + testUser.getName().getOrig() + " (Test User)");

    }
}
