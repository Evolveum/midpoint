/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.UpdateCapabilityType;

/**
 * Test for resources with limited capabilities.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLimitedResources extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "limited-resources");

    protected static final File RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_FILE = new File(TEST_DIR, "resource-dummy-no-attribute-add-delete.xml");
    protected static final String RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID = "5098fa64-8386-11e8-b2e2-83ada4418787";
    protected static final String RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME = "no-attribute-add-delete";
    private static final String RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_SHIP_PREFIX = "Spirit of ";

    protected static final File RESOURCE_DUMMY_NO_CREATE_FILE = new File(TEST_DIR, "resource-dummy-no-create.xml");
    protected static final String RESOURCE_DUMMY_NO_CREATE_OID = "dbaf45c6-40fb-11e9-85b4-5f58a76d12e0";
    protected static final String RESOURCE_DUMMY_NO_CREATE_NAME = "no-create";

    private static final String RESOURCE_DUMMY_NS = MidPointConstants.NS_RI;

    private static final String OU_TREASURE_HUNT = "Treasure Hunt";
    private static final String OU_LOOTING = "Looting";
    private static final String OU_SWASHBACKLING = "Swashbuckling";
    private static final String OU_SAILING = "Sailing";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_FILE, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_NO_CREATE_NAME, RESOURCE_DUMMY_NO_CREATE_FILE, RESOURCE_DUMMY_NO_CREATE_OID, initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        final String TEST_NAME = "test000Sanity";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        PrismObject<ResourceType> resourceNoAAD = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        List<Object> configuredCapabilities = resourceNoAAD.asObjectable().getCapabilities().getConfigured().getAny();
        UpdateCapabilityType capUpdate = CapabilityUtil.getCapability(configuredCapabilities,
                UpdateCapabilityType.class);
        assertNotNull("No configured UpdateCapabilityType", capUpdate);
        assertTrue("Configured addRemoveAttributeValues is not disabled", Boolean.FALSE.equals(capUpdate.isAddRemoveAttributeValues()));
    }

    @Test
    public void test100AssignJackAccountNoAttributeAddDelete() throws Exception {
        final String TEST_NAME = "test100AssignJackAccountNoAttributeAddDelete";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .links()
                .single();

        assertAADWeapon(USER_JACK_USERNAME /* no value */);
    }

    @Test
    public void test102AddJackOrganizationalUnitTreasureHunt() throws Exception {
        final String TEST_NAME = "test102AddJackOrganizationalUnitTreasureHunt";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                createPolyString(OU_TREASURE_HUNT));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        getSingleLinkOid(userAfter);

        assertAADWeapon(USER_JACK_USERNAME, OU_TREASURE_HUNT);
    }

    @Test
    public void test104AddJackOrganizationalUnitLootingSailing() throws Exception {
        final String TEST_NAME = "test104AddJackOrganizationalUnitLootingSailing";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                createPolyString(OU_LOOTING), createPolyString(OU_SAILING));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        getSingleLinkOid(userAfter);

        assertAADWeapon(USER_JACK_USERNAME, OU_TREASURE_HUNT, OU_LOOTING, OU_SAILING);
    }

    @Test
    public void test106DeleteJackOrganizationalUnitLooting() throws Exception {
        final String TEST_NAME = "test106DeleteJackOrganizationalUnitLooting";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserDelete(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                createPolyString(OU_LOOTING));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        getSingleLinkOid(userAfter);

        assertAADWeapon(USER_JACK_USERNAME, OU_TREASURE_HUNT, OU_SAILING);
    }

    /**
     *  MID-4607
     */
    @Test
    public void test108DeleteJackOrganizationalUnitTreasureHuntSailing() throws Exception {
        final String TEST_NAME = "test108DeleteJackOrganizationalUnitTreasureHuntSailing";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserDelete(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result,
                createPolyString(OU_TREASURE_HUNT), createPolyString(OU_SAILING));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        getSingleLinkOid(userAfter);

        assertAADWeapon(USER_JACK_USERNAME /* no values */);
    }

    @Test
    public void test109UnassignJackAccountNoAttributeAddDelete() throws Exception {
        final String TEST_NAME = "test109UnassignJackAccountNoAttributeAddDelete";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertNone()
                .end()
            .links()
                .assertNone();
    }

    /**
     * MID-5129
     */
    @Test
    public void test110AssignJackAccountNoCreate() throws Exception {
        final String TEST_NAME = "test110AssignJackAccountNoCreate";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_NO_CREATE_OID, null, task, result);

        // THEN
        then();
        display("Result", result);
        assertSuccess(result, 2);

        assertUserAfter(USER_JACK_OID)
            .links()
                .assertNone();

        assertNoDummyAccount(RESOURCE_DUMMY_NO_CREATE_NAME, USER_JACK_USERNAME);
    }

    @Test
    public void test119UnassignJackAccountNoCreate() throws Exception {
        final String TEST_NAME = "test119UnassignJackAccountNoCreate";

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_NO_CREATE_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertNone()
                .end()
            .links()
                .assertNone();

        assertNoDummyAccount(RESOURCE_DUMMY_NO_CREATE_NAME, USER_JACK_USERNAME);
    }

    private void assertAADWeapon(String username, String... ous) throws SchemaViolationException, ConflictException, InterruptedException {
        assertDummyAccount(RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME, username);
        Object[] weapons = new String[ous.length];
        for (int i=0; i<ous.length; i++) {
            weapons[i] = RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_SHIP_PREFIX + ous[i];
        }
        assertDummyAccountAttribute(RESOURCE_DUMMY_NO_ATTRIBUTE_ADD_DELETE_NAME, username,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, weapons);
    }
}
