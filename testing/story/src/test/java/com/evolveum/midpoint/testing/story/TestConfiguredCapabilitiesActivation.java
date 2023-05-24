/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Test for resources with configured capabilities (MID-5400, MID-5883)
 *
 * @author Gustav Palos
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConfiguredCapabilitiesActivation extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "configured-capabilities-activation");

    /**
     * Native activation capability: no
     * Simulated activation configured capability: yes
     */
    protected static final File RESOURCE_DUMMY_ACTIVATION_SIMULATED_FILE = new File(TEST_DIR, "resource-dummy-activation-simulated.xml");
    protected static final String RESOURCE_DUMMY_ACTIVATION_SIMULATED_OID = "4bac305c-ed1f-4919-9670-e11863156811";
    protected static final String RESOURCE_DUMMY_ACTIVATION_SIMULATED_NAME = "activation-simulated";

    /**
     * Native activation capability: yes
     * Simulated activation configured capability: yes
     */
    private static final File RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_FILE = new File(TEST_DIR, "resource-dummy-activation-native-simulated.xml");
    private static final String RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_OID = "4bac305c-ed1f-4919-aaaa-e11863156811";
    private static final String RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_NAME = "activation-native-simulated";

    /**
     * Native activation capability: yes
     * Simulated activation configured capability: no
     */
    private static final File RESOURCE_DUMMY_ACTIVATION_NATIVE_FILE = new File(TEST_DIR, "resource-dummy-activation-native.xml");
    private static final String RESOURCE_DUMMY_ACTIVATION_NATIVE_OID = "3df33d1c-ff1c-11e9-a546-93b539ed664a";
    private static final String RESOURCE_DUMMY_ACTIVATION_NATIVE_NAME = "activation-native";

    protected static final File SHADOW_SAMPLE_ACTIVATION_SIMULATED_FILE = new File(TEST_DIR, "shadow-sample-activation-simulated.xml");
    protected static final String SHADOW_SAMPLE_ACTIVATION_SIMULATED_OID = "6925f5a7-2acb-409b-b2e1-94a6534a9745";

    private static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");
    private static final String ROLE_PIRATE_OID = "34713dae-8d56-4717-b184-86d02c9a2361";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_ACTIVATION_SIMULATED_NAME, RESOURCE_DUMMY_ACTIVATION_SIMULATED_FILE, RESOURCE_DUMMY_ACTIVATION_SIMULATED_OID, initTask, initResult);
        addObject(SHADOW_SAMPLE_ACTIVATION_SIMULATED_FILE);

        initDummyResource(RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_NAME, RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_FILE, RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_OID, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_ACTIVATION_NATIVE_NAME, RESOURCE_DUMMY_ACTIVATION_NATIVE_FILE, RESOURCE_DUMMY_ACTIVATION_NATIVE_OID, initTask, initResult);

        importObjectFromFile(ROLE_PIRATE_FILE, initResult);
    }

    @Test
    public void test100ImportAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        modelService.importFromResource(SHADOW_SAMPLE_ACTIVATION_SIMULATED_OID, task, result);

        // THEN
        then();
        assertSuccess(result);
    }

    @Test
    public void test110AssignJackPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        //GIVEN
        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        UserAsserter.forUser(userJackBefore).activation().assertAdministrativeStatus(ActivationStatusType.ENABLED);
        UserAsserter.forUser(userJackBefore).links().assertLiveLinks(0);

        //WHEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        display("Result:\n", result);
        assertSuccess(result);


        //THEN
        then();
        UserAsserter<Void> userAfterAsserter = assertUserAfter(USER_JACK_OID);
        userAfterAsserter
            .activation()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .end()
            .links()
                .assertLiveLinks(2);

        String shadowActivationNativeSimulatedOid = userAfterAsserter
                .links()
                .by()
                    .resourceOid(RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_OID)
                .find()
                    .getOid();
        assertModelShadow(shadowActivationNativeSimulatedOid)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        String shadowActivationNativeOid = userAfterAsserter
            .links()
                .by()
                    .resourceOid(RESOURCE_DUMMY_ACTIVATION_NATIVE_OID)
                .find()
                    .getOid();
        assertModelShadow(shadowActivationNativeOid)
            // MID-5883
            .assertAdministrativeStatus(ActivationStatusType.ENABLED);

        DummyAccount jackNativeSimulatedAccount = getDummyResource(RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_NAME).getAccountByUsername(USER_JACK_USERNAME);
        displayDumpable("Jack Dummy native-simulated account", jackNativeSimulatedAccount);
        String privilegesNativeSimulatedValue = jackNativeSimulatedAccount.getAttributeValue("privileges");
        AssertJUnit.assertEquals("Unexpected 'native-simulated' privileges attribute value: " + privilegesNativeSimulatedValue, "false", privilegesNativeSimulatedValue);

        DummyAccount jackNativeAccount = getDummyResource(RESOURCE_DUMMY_ACTIVATION_NATIVE_NAME).getAccountByUsername(USER_JACK_USERNAME);
        displayDumpable("Jack Dummy native account", jackNativeAccount);
        AssertJUnit.assertTrue("Unexpected 'native' activation status value: " + jackNativeAccount.isEnabled(), jackNativeAccount.isEnabled());
        String privilegesNativeValue = jackNativeAccount.getAttributeValue("privileges");
        AssertJUnit.assertNull("Unexpected 'native' privileges attribute: " + privilegesNativeValue, privilegesNativeValue);
    }

    @Test
    public void test112ModifyActivationJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        UserAsserter.forUser(userJack).activation().assertAdministrativeStatus(ActivationStatusType.ENABLED);

        //WHEN
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.DISABLED);

        //THEN
        then();

        UserAsserter<Void> userAfterAsserter = assertUserAfter(USER_JACK_OID);
        userAfterAsserter
            .activation()
                .assertAdministrativeStatus(ActivationStatusType.DISABLED)
                .end()
            .links()
                .assertLiveLinks(2);

        String shadowActivationNativeSimulatedOid = userAfterAsserter
                .links()
                .by()
                    .resourceOid(RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_OID)
                .find()
                    .getOid();
        assertModelShadow(shadowActivationNativeSimulatedOid)
                .assertAdministrativeStatus(ActivationStatusType.DISABLED);

        String shadowActivationNativeOid = userAfterAsserter
            .links()
                .by()
                    .resourceOid(RESOURCE_DUMMY_ACTIVATION_NATIVE_OID)
                .find()
                    .getOid();
        assertModelShadow(shadowActivationNativeOid)
            // MID-5883
            .assertAdministrativeStatus(ActivationStatusType.DISABLED);

        DummyAccount jackNativeSimulatedAccount = getDummyResource(RESOURCE_DUMMY_ACTIVATION_NATIVE_SIMULATED_NAME).getAccountByUsername(USER_JACK_USERNAME);
        displayDumpable("Jack Dummy native-simulated account", jackNativeSimulatedAccount);
        String privilegesNativeSimulatedValue = jackNativeSimulatedAccount.getAttributeValue("privileges");
        AssertJUnit.assertEquals("Unexpected 'native-simulated' privileges attribute value: " + privilegesNativeSimulatedValue, "true", privilegesNativeSimulatedValue);

        DummyAccount jackNativeAccount = getDummyResource(RESOURCE_DUMMY_ACTIVATION_NATIVE_NAME).getAccountByUsername(USER_JACK_USERNAME);
        displayDumpable("Jack Dummy native account", jackNativeAccount);
        AssertJUnit.assertFalse("Unexpected 'native' activation status value: " + jackNativeAccount.isEnabled(), jackNativeAccount.isEnabled());
        String privilegesNativeValue = jackNativeAccount.getAttributeValue("privileges");
        AssertJUnit.assertNull("Unexpected 'native' privileges attribute: " + privilegesNativeValue, privilegesNativeValue);
    }

}
