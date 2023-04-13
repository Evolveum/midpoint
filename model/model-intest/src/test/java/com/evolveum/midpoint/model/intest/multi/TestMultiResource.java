/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.multi;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME_PATH;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;

/**
 * Tests with several inter-dependent resources.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiResource extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/multi-resource");

    // LAVENDER dummy resource has a STRICT dependency on default dummy resource
    private static final File RESOURCE_DUMMY_LAVENDER_FILE = new File(TEST_DIR, "resource-dummy-lavender.xml");
    private static final String RESOURCE_DUMMY_LAVENDER_OID = "10000000-0000-0000-0000-000000000504";
    private static final String RESOURCE_DUMMY_LAVENDER_NAME = "lavender";

    // IVORY dummy resource has a LAX dependency on default dummy resource
    private static final File RESOURCE_DUMMY_IVORY_FILE = new File(TEST_DIR, "resource-dummy-ivory.xml");
    private static final String RESOURCE_DUMMY_IVORY_OID = "10000000-0000-0000-0000-000000011504";
    private static final String RESOURCE_DUMMY_IVORY_NAME = "ivory";

    // BEIGE dummy resource has a RELAXED dependency on default dummy resource
    private static final File RESOURCE_DUMMY_BEIGE_FILE = new File(TEST_DIR, "resource-dummy-beige.xml");
    private static final String RESOURCE_DUMMY_BEIGE_OID = "10000000-0000-0000-0000-00000001b504";
    private static final String RESOURCE_DUMMY_BEIGE_NAME = "beige";

    // PERU dummy resource has a RELAXED dependency on YELLOW dummy resource
    private static final File RESOURCE_DUMMY_PERU_FILE = new File(TEST_DIR, "resource-dummy-peru.xml");
    private static final String RESOURCE_DUMMY_PERU_OID = "10000000-0000-0000-0000-00000001c504";
    private static final String RESOURCE_DUMMY_PERU_NAME = "peru";

    // PERU dummy resource has a RELAXED dependency on YELLOW dummy resource and disable instead of delete
    private static final File RESOURCE_DUMMY_DARK_PERU_FILE = new File(TEST_DIR, "resource-dummy-dark-peru.xml");
    private static final String RESOURCE_DUMMY_DARK_PERU_OID = "f5253596-333d-11e8-8894-37a2f88e7609";
    private static final String RESOURCE_DUMMY_DARK_PERU_NAME = "dark-peru";

    // Similar to YELLOW, but has disable instead of delete
    private static final File RESOURCE_DUMMY_DARK_YELLOW_FILE = new File(TEST_DIR, "resource-dummy-dark-yellow.xml");
    private static final String RESOURCE_DUMMY_DARK_YELLOW_OID = "33da1afe-3efb-11e8-a5e3-4fed83f61ae7";
    private static final String RESOURCE_DUMMY_DARK_YELLOW_NAME = "dark-yellow";

    private static final File RESOURCE_DUMMY_DAVID_FILE = new File(TEST_DIR, "resource-dummy-david.xml");
    private static final String RESOURCE_DUMMY_DAVID_OID = "10000000-0000-0000-0000-000000300001";
    private static final String RESOURCE_DUMMY_DAVID_NAME = "david";

    private static final File RESOURCE_DUMMY_GOLIATH_FILE = new File(TEST_DIR, "resource-dummy-goliath.xml");
    private static final String RESOURCE_DUMMY_GOLIATH_OID = "10000000-0000-0000-0000-000000300002";
    private static final String RESOURCE_DUMMY_GOLIATH_NAME = "goliath";

    // Assigns default dummy resource and red dummy resource
    private static final File ROLE_DUMMIES_FILE = new File(TEST_DIR, "role-dummies.xml");
    private static final String ROLE_DUMMIES_OID = "12345678-d34d-b33f-f00d-55555555dddd";

    private static final File ROLE_DUMMIES_IVORY_FILE = new File(TEST_DIR, "role-dummies-ivory.xml");
    private static final String ROLE_DUMMIES_IVORY_OID = "12345678-d34d-b33f-f00d-55555511dddd";

    private static final File ROLE_DUMMIES_BEIGE_FILE = new File(TEST_DIR, "role-dummies-beige.xml");
    private static final String ROLE_DUMMIES_BEIGE_OID = "12345678-d34d-b33f-f00d-5555551bdddd";

    private static final File ROLE_FIGHT_FILE = new File(TEST_DIR, "role-fight.xml");
    private static final String ROLE_FIGHT_OID = "12345678-d34d-b33f-f00d-5555550303dd";

    private static final File ROLE_DARK_YELLOW_PERU_FILE = new File(TEST_DIR, "role-dark-yellow-peru.xml");
    private static final String ROLE_DARK_YELLOW_PERU_OID = "95213bbc-3357-11e8-aeb8-439c6ddc0fa0";

    private static final String USER_WORLD_NAME = "world";
    private static final String USER_WORLD_FULL_NAME = "The World";

    private static final String USER_FIELD_NAME = "field";

    private static final String USER_PASSWORD_A_CLEAR = "A"; // too short

    private String deadShadowOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        setRecordEnhancedShadowChanges(true, initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_LAVENDER_NAME,
                RESOURCE_DUMMY_LAVENDER_FILE, RESOURCE_DUMMY_LAVENDER_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_IVORY_NAME,
                RESOURCE_DUMMY_IVORY_FILE, RESOURCE_DUMMY_IVORY_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_BEIGE_NAME,
                RESOURCE_DUMMY_BEIGE_FILE, RESOURCE_DUMMY_BEIGE_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_PERU_NAME,
                RESOURCE_DUMMY_PERU_FILE, RESOURCE_DUMMY_PERU_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_DARK_PERU_NAME,
                RESOURCE_DUMMY_DARK_PERU_FILE, RESOURCE_DUMMY_DARK_PERU_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_DARK_YELLOW_NAME,
                RESOURCE_DUMMY_DARK_YELLOW_FILE, RESOURCE_DUMMY_DARK_YELLOW_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_DAVID_NAME,
                RESOURCE_DUMMY_DAVID_FILE, RESOURCE_DUMMY_DAVID_OID, initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_GOLIATH_NAME,
                RESOURCE_DUMMY_GOLIATH_FILE, RESOURCE_DUMMY_GOLIATH_OID, initTask, initResult);

        repoAddObjectFromFile(ROLE_DUMMIES_FILE, initResult);
        repoAddObjectFromFile(ROLE_DUMMIES_IVORY_FILE, initResult);
        repoAddObjectFromFile(ROLE_DUMMIES_BEIGE_FILE, initResult);
        repoAddObjectFromFile(ROLE_FIGHT_FILE, initResult);
        repoAddObjectFromFile(ROLE_DARK_YELLOW_PERU_FILE, initResult);

        getDummyResource().resetBreakMode();
    }

    @Test
    public void test110JackAssignRoleDummiesFull() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        jackAssignRoleDummies();
    }

    @Test
    public void test113JackRenameFull() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        jackRename();
    }

    @Test
    public void test114JackUnAssignRoleDummiesFull() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        jackUnAssignRoleDummies();
    }

    // TODO: lavender resource with failure

    @Test
    public void test115JackAssignRoleDummiesFullErrorIvory() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        getDummyResource().setAddBreakMode(BreakMode.NETWORK);
        jackAssignRoleDummiesError(ROLE_DUMMIES_IVORY_OID, RESOURCE_DUMMY_IVORY_NAME, true);
    }

    @Test
    public void test116JackUnAssignRoleDummiesFullErrorIvory() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        getDummyResource().setAddBreakMode(BreakMode.NETWORK);
        jackUnassignRoleDummiesError(ROLE_DUMMIES_IVORY_OID, RESOURCE_DUMMY_IVORY_OID);
    }

    /**
     * Beige resource has relaxed dependency on default dummy resource.
     * There is an error provisioning default dummy resource. Therefore provisioning
     * of beige resource is skipped entirely.
     */
    @Test
    public void test117JackAssignRoleDummiesFullErrorBeige() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        getDummyResource().setAddBreakMode(BreakMode.NETWORK);
        jackAssignRoleDummiesError(ROLE_DUMMIES_BEIGE_OID, RESOURCE_DUMMY_BEIGE_NAME, false);
    }

    @Test
    public void test118JackUnAssignRoleDummiesFullErrorBeige() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
        getDummyResource().setAddBreakMode(BreakMode.NETWORK);
        jackUnassignRoleDummiesError(ROLE_DUMMIES_BEIGE_OID, null);
    }

    @Test
    public void test120JackAssignRoleDummiesRelative() throws Exception {
        getDummyResource().resetBreakMode();
        // Clean up user
        Task task = getTestTask();
        OperationResult result = task.getResult();
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        jackAssignRoleDummies();
    }

    /**
     * Try to delete Jack's default dummy account. As other provisioned accounts depends on it the
     * operation should fail.
     */
    @Test
    public void test121JackTryDeleteAccount() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        getDummyResource().resetBreakMode();
        // Clean up user
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        String accountJackDummyOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_OID);

        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createDeleteDelta(ShadowType.class, accountJackDummyOid);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(accountDelta);

        try {
            // WHEN
            modelService.executeChanges(deltas, null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }
    }

    @Test
    public void test123JackRenameRelative() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        jackRename();
    }

    @Test
    public void test129JackUnAssignRoleDummiesRelative() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        jackUnAssignRoleDummies();
    }

    /**
     * Ivory resource has a lax dependency. The provisioning should go OK.
     */
    @Test
    public void test200JackAssignDummyIvory() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Clean up user
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        // WHEN
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_IVORY_OID, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLiveLinks(userJack, 1);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
    }

    /**
     * Ivory resource has a lax dependency. The provisioning should go OK.
     */
    @Test
    public void test209JackUnAssignDummyIvory() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Clean up user
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        // WHEN
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_IVORY_OID, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLiveLinks(userJack, 0);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Beige resource has a relaxed dependency. The provisioning should go OK
     * even if there is no default dummy account.
     */
    @Test
    public void test210JackAssignDummyBeige() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Clean up user
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLiveLinks(userJack, 1);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
    }

    /**
     * Beige resource has a relaxed dependency. The deprovisioning should go OK
     * even if there is not default dummy account.
     */
    @Test
    public void test219JackUnAssignDummyBeige() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Clean up user
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLiveLinks(userJack, 0);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Beige resource has a relaxed dependency. Try provisioning of both
     * beige and default dummy accounts.
     */
    @Test
    public void test220JackAssignDummyBeigeAndDefault() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Clean up user
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, true);
        userDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, true));

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLiveLinks(userJack, 2);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Recompute to check that recompute will not ruin anything.
     */
    @Test
    public void test221JackRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLiveLinks(userJack, 2);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Delete account on default dummy resource (but keep it assigned and keep the shadow).
     * Then recompute the user. The account should be re-created.
     * MID-2134, MID-3093
     */
    @Test
    public void test223JackKillDefaultDummyAccountAndRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        displayDumpable("dummy resource before", getDummyResource());

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assertLiveLinks(2);

        displayDumpable("dummy resource after", getDummyResource());

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        // No value for ship ... no place to get it from
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Delete account on beige dummy resource (but keep it assigned and keep the shadow).
     * Then recompute the user. The account should be re-created.
     * MID-2134, MID-3093
     */
    @Test
    public void test224JackKillBeigeAccountAndRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_BEIGE_NAME).deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        displayDumpable("beige dummy resource before", getDummyResource(RESOURCE_DUMMY_BEIGE_NAME));

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, executeOptions().reconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .displayWithProjections()
            .links()
                .assertLiveLinks(2)
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_RELATED)
                    .dead(true)
                .find()
                    .end()
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .end()
                .by()
                    .resourceOid(RESOURCE_DUMMY_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .target()
                        .assertLive()
                        .end()
                    .end()
                .by()
                    .resourceOid(RESOURCE_DUMMY_OID)
                    .relation(SchemaConstants.ORG_RELATED)
                    .dead(true)
                .find()
                    .target()
                        .assertDead()
                        .end()
                    .end()
                .end();

        displayDumpable("beige dummy resource after", getDummyResource(RESOURCE_DUMMY_BEIGE_NAME));

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled();

        assertDummyAccountByUsername(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            // No value for ship ... no place to get it from
            .assertNoAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test225ForceDeleteDeadShadow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String deadBeigeShadowOid = assertUserBefore(USER_JACK_OID)
            .links()
                .assertLiveLinks(2)
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_RELATED)
                    .dead(true)
                .find()
                    .getOid();

        // WHEN
        when();
        forceDeleteObject(ShadowType.class, deadBeigeShadowOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoShadow(deadBeigeShadowOid);

        UserAsserter<Void> userAfterAsserter = assertUserAfter(USER_JACK_OID)
            .displayWithProjections();

        // Make sure that only the dead shadow was deleted and live shadow stays
        userAfterAsserter
            .links()
                .assertLiveLinks(2)
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                .find()
                    .resolveTarget()
                        .assertLive();

        // Make sure that only the dead shadow was deleted and the account stays
        assertDummyAccountByUsername(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            // No value for ship ... no place to get it from
            .assertNoAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);
    }

    /**
     * Delete both accounts on beige and default dummy resource (but keep it assigned and keep the shadows).
     * Then recompute the user. The accounts should be re-created.
     * MID-2134, MID-3093
     */
    @Test
    public void test226JackKillBothAccountsAndRecompute() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertUserBefore(USER_JACK_OID)
            .displayWithProjections()
            .assertLiveLinks(2);

        getDummyResource().deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        displayDumpable("dummy resource before", getDummyResource());

        getDummyResource(RESOURCE_DUMMY_BEIGE_NAME).deleteAccountByName(ACCOUNT_JACK_DUMMY_USERNAME);
        displayDumpable("beige dummy resource before", getDummyResource(RESOURCE_DUMMY_BEIGE_NAME));

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, executeOptions().reconcile(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .displayWithProjections()
            .links()
                .assertLiveLinks(2)
                .by()
                    .resourceOid(RESOURCE_DUMMY_OID)
                    .relation(SchemaConstants.ORG_RELATED)
                    .dead(true)
                    .assertCount(2)
                .by()
                    .resourceOid(RESOURCE_DUMMY_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .end()
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_RELATED)
                    .dead(true)
                .find()
                    .end()
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .end();

        displayDumpable("dummy resource after", getDummyResource());
        displayDumpable("beige dummy resource after", getDummyResource(RESOURCE_DUMMY_BEIGE_NAME));

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled();

        assertDummyAccountByUsername(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            // No value for ship ... no place to get it from
            .assertNoAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Cause schema violation on the account during a provisioning operation. This should fail
     * the operation, but other operations should proceed and the account should definitely NOT
     * be unlinked.
     * MID-2134
     */
    @Test
    public void test227ModifyUserJackDefaultDummyBrokenSchemaViolation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();

        getDummyResource().setModifyBreakMode(BreakMode.SCHEMA);

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                createPolyString("Cpt. Jack Sparrow"));

        // THEN
        then();
        assertPartialError(result);

        assertUserAfter(USER_JACK_OID)
            .links()
                .assertLiveLinks(2)
                .by()
                    .resourceOid(RESOURCE_DUMMY_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .end()
                .by()
                    // dead shadow from previous test. We leave it here
                    // by purpose. It causes some nuisance. But it should not
                    // ruin anything.
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_RELATED)
                    .dead(true)
                .find()
                    .end()
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .end();

        displayDumpable("dummy resource after", getDummyResource());
        displayDumpable("beige dummy resource after", getDummyResource(RESOURCE_DUMMY_BEIGE_NAME));

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
            // Full name is NOT modified here because of (simulated) schema exception.
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled();

        assertDummyAccountByUsername(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            // Full name is modified here. Operation done.
            .assertFullName("Cpt. Jack Sparrow")
            .assertEnabled()
            // No value for ship ... no place to get it from
            .assertNoAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        getDummyResource().resetBreakMode();
    }

    /**
     * Reset break mode, make sure that everything is back to normal.
     * MID-2134
     */
    @Test
    public void test228ModifyUserJackDefaultDummyNoError() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        dummyAuditService.clear();
        getDummyResource().resetBreakMode();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result,
                createPolyString(USER_JACK_FULL_NAME));

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .links()
                .assertLiveLinks(2)
                .by()
                    .resourceOid(RESOURCE_DUMMY_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .end()
                .by()
                    // dead shadow from previous test. We leave it here
                    // by purpose. It causes some nuisance. But it should not
                    // ruin anything.
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_RELATED)
                    .dead(true)
                .find()
                    .end()
                .by()
                    .resourceOid(RESOURCE_DUMMY_BEIGE_OID)
                    .relation(SchemaConstants.ORG_DEFAULT)
                    .dead(false)
                .find()
                    .end();

        displayDumpable("dummy resource after", getDummyResource());
        displayDumpable("beige dummy resource after", getDummyResource(RESOURCE_DUMMY_BEIGE_NAME));

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled();

        assertDummyAccountByUsername(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            // No value for ship ... no place to get it from
            .assertNoAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Beige resource has a relaxed dependency. Try provisioning of both
     * beige and default dummy accounts.
     */
    @Test
    public void test229JackUnassignDummyBeigeAndDefault() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        getDummyResource().resetBreakMode();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_OID, null, false);
        userDelta.addModification(createAssignmentModification(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null, false));

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        String deadShadowOid = assertUserAfter(USER_JACK_OID)
            .links()
                .by().resourceOid(RESOURCE_DUMMY_BEIGE_OID).find()
                    // dead shadow from previous test. We leave it here
                    // by purpose. It causes some nuisance. But it should not
                    // ruin anything.
                    .resolveTarget()
                        .assertDead()
                        .getOid();

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // CLEANUP - make sure we have clean slate for next tests
        displayCleanup();
        forceDeleteShadow(deadShadowOid);

        assertNoShadow(deadShadowOid);
        assertUserAfter(USER_JACK_OID)
            .assertLiveLinks(0);
    }


    /**
     * Lavender resource has a strict dependency. The provisioning should fail.
     */
    @Test
    public void test250JackAssignDummyLavender() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Clean up user
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);

        try {
            // WHEN
            assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_LAVENDER_OID, null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            // this is expected
        }

        // THEN
        result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertLiveLinks(userJack, 0);
        assertAssignments(userJack, 0);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * The "dummies" role assigns two dummy resources that are in a dependency. The value of
     * {@link DummyResourceContoller#DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME} is propagated from one
     * resource through the user to the other resource. If dependency does not work then no value is propagated.
     *
     * We do the simulation first, then the real execution.
     */
    private void jackAssignRoleDummies() throws Exception {
        Task task = createTask("jackAssignRoleDummies");
        OperationResult result = task.getResult();
        clearJackOrganizationalUnit(task, result);

        if (isNativeRepository()) {
            jackAssignRoleDummiesSimulated(task, result);
        }
        jackAssignRoleDummiesReal(task, result);
    }

    /**
     * For the explanation of attribute/property assertions, see {@link #jackAssignRoleDummiesReal(Task, OperationResult)}.
     */
    private void jackAssignRoleDummiesSimulated(Task task, OperationResult result) throws Exception {
        when();
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> assignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result));

        then();
        assertSuccess(result);
        // @formatter:off
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventMarks(MARK_FOCUS_ASSIGNMENT_CHANGED, MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED)
                    .delta()
                        .assertPolyStringModification(
                                UserType.F_ORGANIZATIONAL_UNIT,
                                null,
                                "The crew of The Lost Souls") // see the "real" assertions
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).resourceOid(RESOURCE_DUMMY_OID).find()
                    .assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .objectToAdd()
                            .asShadow()
                                .attributes()
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME, ACCOUNT_JACK_DUMMY_FULLNAME)
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_QNAME, "The Great Voodoo Master")
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, "The Lost Souls")
                                .end()
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).resourceOid(RESOURCE_DUMMY_LAVENDER_OID).find()
                    .assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .objectToAdd()
                            .asShadow()
                                .attributes()
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME, ACCOUNT_JACK_DUMMY_FULLNAME)
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, "The crew of The Lost Souls")
                                    .assertValue(
                                            DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME,
                                            "Jack Sparrow must be the best captain the Caribbean has ever seen")
                                .end()
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).resourceOid(RESOURCE_DUMMY_IVORY_OID).find()
                    .assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .objectToAdd()
                            .asShadow()
                                .attributes()
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME, ACCOUNT_JACK_DUMMY_FULLNAME)
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, "The crew of The Lost Souls")
                                .end()
                            .end()
                        .end()
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).resourceOid(RESOURCE_DUMMY_BEIGE_OID).find()
                    .assertEventMarks(MARK_PROJECTION_ACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .objectToAdd()
                            .asShadow()
                                .attributes()
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_QNAME, ACCOUNT_JACK_DUMMY_FULLNAME)
                                    .assertValue(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, "The crew of The Lost Souls")
                                .end()
                            .end()
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    private void jackAssignRoleDummiesReal(Task task, OperationResult result) throws Exception {
        when();
        assignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .displayWithProjections()
            .assignments()
                .assertRole(ROLE_DUMMIES_OID)
                .end()
            .links()
                .assertLiveLinks(4)
                .end()
            // This is set up by "feedback" using an inbound expression. It has nothing with dependencies yet.
            .assertOrganizationalUnit("The crew of The Lost Souls");

        assertDummyAccountByUsername(null, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "The Great Voodoo Master")
            .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The Lost Souls");

        assertDummyAccountByUsername(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
            // after user's organizationUnit is set and it will have the same value as above.
            .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls")
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "Jack Sparrow must be the best captain the Caribbean has ever seen");

        assertDummyAccountByUsername(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
            // after user's organizationUnit is set and it will have the same value as above.
            .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");

        assertDummyAccountByUsername(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME)
            .assertFullName(ACCOUNT_JACK_DUMMY_FULLNAME)
            .assertEnabled()
            // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
            // after user's organizationUnit is set and it will have the same value as above.
            .assertAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
    }

    private void jackRename() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        if (isNativeRepository()) {
            jackRenameSimulated("jackie", "Jackie Sparrow", task, result);
        }
        jackRenameReal("jackie", "Jackie Sparrow", task, result);
        jackRenameReal(USER_JACK_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, task, result);
    }

    @SuppressWarnings("SameParameterValue")
    private void jackRenameSimulated(String toName, String toFullName, Task task, OperationResult result)
            throws Exception {

        when("simulated change to " + toName + "/" + toFullName + " is executed");
        var simulationResult = executeWithSimulationResult(
                List.of(createRenameDelta(toName, toFullName)), task, result);

        then("result is OK");
        assertSuccess(result);
        // @formatter:off
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventMarks(MARK_FOCUS_RENAMED)
                    .delta()
                        .assertModifiedExclusive(
                                UserType.F_NAME,
                                UserType.F_FULL_NAME,
                                UserType.F_METADATA)
                        .assertPolyStringModification(UserType.F_NAME, "jack", toName)
                        .assertPolyStringModification(UserType.F_FULL_NAME, "Jack Sparrow", toFullName)
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).resourceOid(RESOURCE_DUMMY_OID).find()
                    .assertEventMarks(
                            MARK_PROJECTION_RENAMED,
                            MARK_PROJECTION_IDENTIFIER_CHANGED,
                            MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .assertModification(ICFS_NAME_PATH, "jack", toName)
                        .assertModification(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Jack Sparrow", toFullName)
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).resourceOid(RESOURCE_DUMMY_LAVENDER_OID).find()
                    .assertEventMarks(
                            MARK_PROJECTION_RENAMED,
                            MARK_PROJECTION_IDENTIFIER_CHANGED,
                            MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .assertModification(ICFS_NAME_PATH, "jack", toName)
                        .assertModification(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Jack Sparrow", toFullName)
                        .assertModification(
                                DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_PATH,
                                "Jack Sparrow must be the best captain the Caribbean has ever seen",
                                toFullName +" must be the best captain the Caribbean has ever seen")
                        // plus other changes not checked here
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).resourceOid(RESOURCE_DUMMY_IVORY_OID).find()
                    .assertEventMarks(
                            MARK_PROJECTION_RENAMED,
                            MARK_PROJECTION_IDENTIFIER_CHANGED,
                            MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .assertModification(ICFS_NAME_PATH, "jack", toName)
                        .assertModification(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Jack Sparrow", toFullName)
                    .end()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).resourceOid(RESOURCE_DUMMY_BEIGE_OID).find()
                    .assertEventMarks(
                            MARK_PROJECTION_RENAMED,
                            MARK_PROJECTION_IDENTIFIER_CHANGED,
                            MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                    .delta()
                        .assertModification(ICFS_NAME_PATH, "jack", toName)
                        .assertModification(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH, "Jack Sparrow", toFullName)
                    .end()
                .end();
        // @formatter:on
    }

    private void jackRenameReal(String toName, String toFullName, Task task, OperationResult result)
            throws Exception {

        when("real change to " + toName + "/" + toFullName + " is executed");
        modelService.executeChanges(
                List.of(createRenameDelta(toName, toFullName)), null, task, result);

        then("result is OK");
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(toName));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(toFullName));
        assertAssignedRole(USER_JACK_OID, ROLE_DUMMIES_OID, result);
        assertLiveLinks(userJack, 4);

        assertDefaultDummyAccount(toName, toFullName, true);
        assertDefaultDummyAccountAttribute(toName, "title", "The Great Voodoo Master");
        assertDefaultDummyAccountAttribute(toName, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The Lost Souls");

        // This is set up by "feedback" using an inbound expression. It has nothing with dependencies yet.
        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));

        assertDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, toName, toFullName, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_LAVENDER_NAME, toName,
                DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
        assertDummyAccountAttribute(RESOURCE_DUMMY_LAVENDER_NAME, toName,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, toFullName +" must be the best captain the Caribbean has ever seen");

        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, toName, toFullName, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_IVORY_NAME, toName, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");

        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, toName, toFullName, true);
        // This is set by red's outbound from user's organizationalUnit. If dependencies work this outbound is processed
        // after user's organizationUnit is set and it will have the same value as above.
        assertDummyAccountAttribute(RESOURCE_DUMMY_BEIGE_NAME, toName, DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
    }

    private ObjectDelta<UserType> createRenameDelta(String toName, String toFullName) throws SchemaException {
        return deltaFor(UserType.class)
                .item(UserType.F_NAME).replace(PolyString.fromOrig(toName))
                .item(UserType.F_FULL_NAME).replace(PolyString.fromOrig(toFullName))
                .asObjectDelta(USER_JACK_OID);
    }

    private void jackUnAssignRoleDummies() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        if (isNativeRepository()) {
            jackUnAssignRoleDummiesSimulated(task, result);
        }
        jackUnAssignRoleDummiesReal(task, result);
    }

    private void jackUnAssignRoleDummiesSimulated(Task task, OperationResult result) throws Exception {
        when("unassignment is executed in simulation mode");
        var simulationResult = executeWithSimulationResult(
                task, result,
                () -> unassignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result));

        then("result is OK");
        assertSuccess(result);
        // @formatter:off
        assertProcessedObjects(simulationResult, "after")
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventMarks(MARK_FOCUS_ASSIGNMENT_CHANGED, MARK_FOCUS_ROLE_MEMBERSHIP_CHANGED)
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).resourceOid(RESOURCE_DUMMY_OID).find()
                    .assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).resourceOid(RESOURCE_DUMMY_LAVENDER_OID).find()
                    .assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).resourceOid(RESOURCE_DUMMY_IVORY_OID).find()
                    .assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.DELETE).resourceOid(RESOURCE_DUMMY_BEIGE_OID).find()
                    .assertEventMarks(MARK_PROJECTION_DEACTIVATED, MARK_PROJECTION_RESOURCE_OBJECT_AFFECTED)
                .end();
        // @formatter:on
    }

    private void jackUnAssignRoleDummiesReal(Task task, OperationResult result) throws Exception {

        List<String> linkOidsBefore = assertUserBefore(USER_JACK_OID)
                .links()
                .getOids();

        when();
        unassignRole(USER_JACK_OID, ROLE_DUMMIES_OID, task, result);

        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertNoRole()
                .end()
            .assertLiveLinks(0)
            .assertOrganizationalUnits("The crew of The Lost Souls");

        assertNoShadows(linkOidsBefore);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * The "dummies" role assigns two dummy resources that are in a dependency. The value of
     * {@link DummyResourceContoller#DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME} is propagated from one
     * resource through the user to the other resource. If dependency does not work then no value is propagated.
     */
    private void jackAssignRoleDummiesError(
            String roleOid, String dummyResourceName, boolean expectAccount) throws Exception {

        Task task = getTestTask();
        OperationResult result = task.getResult();
        clearJackOrganizationalUnit(task, result);

        assertUserBefore(USER_JACK_OID)
                .assertAssignments(0)
                .assertLiveLinks(0);

        // WHEN
        when();
        assignRole(USER_JACK_OID, roleOid, task, result);

        // THEN
        then();
        result.computeStatus();
        display(result);
        TestUtil.assertResultStatus(result, OperationResultStatus.IN_PROGRESS);

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertRole(roleOid)
                .end()
            // One of the accountRefs is actually ref to an uncreated shadow
            .assertLiveLinks(expectAccount ? 2 : 1);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME);

        if (expectAccount) {
            assertDummyAccount(dummyResourceName, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
            // This is actually pulled from the uncreated shadow
            assertDummyAccountAttribute(
                    dummyResourceName, ACCOUNT_JACK_DUMMY_USERNAME,
                    DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "The crew of The Lost Souls");
        } else {
            assertNoDummyAccount(dummyResourceName, ACCOUNT_JACK_DUMMY_USERNAME);
        }
    }

    private void clearJackOrganizationalUnit(Task task, OperationResult result) throws CommonException {
        modifyUserReplace(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, task, result);
    }

    private void jackUnassignRoleDummiesError(
            String roleOid, String otherResourceOid) throws Exception {

        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserAsserter<Void> userBeforeAsserter = assertUserBefore(USER_JACK_OID);
        String dummyShadowOid = userBeforeAsserter
                .links()
                .by()
                .resourceOid(RESOURCE_DUMMY_OID)
                .find()
                    .resolveTarget()
                        .assertConception()
                        .hasUnfinishedPendingOperations()
                        .getOid();
        String dummyOtherShadowOid;
        if (otherResourceOid != null) {
            dummyOtherShadowOid = userBeforeAsserter
                .links()
                    .by()
                        .resourceOid(otherResourceOid)
                    .find()
                        .getOid();
        } else {
            dummyOtherShadowOid = null;
        }

        // WHEN
        when();
        unassignRole(USER_JACK_OID, roleOid, task, result);

        // THEN
        then();
        result.computeStatus();
        display(result);
        // there is a failure while reading dummy account - it was not created
        // because of unavailability of the resource..but it is OK..
        OperationResultStatus status = result.getStatus();
        if (status != OperationResultStatus.HANDLED_ERROR && status != OperationResultStatus.PARTIAL_ERROR) {
            AssertJUnit.fail("Expected result success or partial error status, but was "+status);
        }

        assertUserAfter(USER_JACK_OID)
            .assignments()
                .assertNoRole()
                .end()
            .assertLinks(0, 1);

        assertRepoShadow(dummyShadowOid)
            .assertTombstone()
            .pendingOperations()
                .by()
                    .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertAll();

        if (dummyOtherShadowOid != null) {
            assertNoShadow(dummyOtherShadowOid);
        }

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        assertUserProperty(USER_JACK_OID, UserType.F_ORGANIZATIONAL_UNIT, PrismTestUtil.createPolyString("The crew of The Lost Souls"));

        displayValue("FORCE DELETE", dummyShadowOid);
        // Force delete of dead shadow
        forceDeleteShadow(dummyShadowOid);

        assertNoShadow(dummyShadowOid);
        assertUserAfter(USER_JACK_OID)
            .assertLiveLinks(0);
    }

    @Test
    public void test300AddAndAssignRelative() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Add default dummy account to jack without assigning it.
        // In relative mode this account should shay untouched while we play with assignments and
        // unassignments of other accounts
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(ACCOUNT_JACK_DUMMY_FILE);
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(UserType.class, USER_JACK_OID);
        PrismReferenceValue accountRefVal = itemFactory().createReferenceValue();
        accountRefVal.setObject(account);
        ReferenceDelta accountDelta = prismContext.deltaFactory().reference()
                .createModificationAdd(UserType.F_LINK_REF, getUserDefinition(), accountRefVal);
        userDelta.addModification(accountDelta);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscUtil.createCollection(userDelta);
        modelService.executeChanges(deltas, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after default dummy account add", userJack);
        assertUserJack(userJack);
        assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        userJack = getUser(USER_JACK_OID);
        display("User after red dummy assignment", userJack);
        assertUserJack(userJack);
        assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertAccount(userJack, RESOURCE_DUMMY_BLUE_OID);
        assertLiveLinks(userJack, 2);
        String accountOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_BLUE_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, getDummyResourceType(RESOURCE_DUMMY_BLUE_NAME));

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    @Test
    public void test310AddedAccountAndUnassignRelative() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_BLUE_OID, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after red dummy unassignment", userJack);
        assertUserJack(userJack);
        assertAccount(userJack, RESOURCE_DUMMY_OID);
        assertLiveLinks(userJack, 1);
        String accountOid = getLiveLinkRefOid(userJack, RESOURCE_DUMMY_OID);

        // Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        assertDummyAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        assertDummyAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

    }

    /**
     * This is mostly a cleanup. But it also tests some cases.
     */
    @Test
    public void test319UnassignDummyRelative() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        // Originally here was unassign (delete assignment) operation. But such assignment does not exist.
        // As we updated the expression evaluation, such phantom changes are ignored.
        //
        // So assignment deletion is here replaced by simple unlink operation.
        ObjectDelta<UserType> delta = createModifyUserUnlinkAccount(USER_JACK_OID, RESOURCE_DUMMY_OID);
        executeChanges(delta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after dummy unassignment", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 0);

        assertDummyAccount(null, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BLUE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_OID, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_OID, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Attempt to add lavender account should fail. There is unsatisfied strict dependency on
     * default dummy resource.
     */
    @Test
    public void test350AddAccountLavender() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserAddDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_LAVENDER_NAME);

        // WHEN
        try {
            when();
            executeChanges(userDelta, null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertFailure(result);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

    }

    @Test
    public void test352AddAccountIvory() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserAddDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_IVORY_NAME);

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    @Test
    public void test354AddAccountBeige() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserAddDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_NAME);

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    /**
     * The dummy account exists, because in test319 it was only unlinked, not deleted. It will be re-linked now.
     */
    @Test
    public void test360AddAccountDummy() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserAddAccount(USER_JACK_OID, getDummyResourceObject());

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    /**
     * This should work now as the dependency is satisfied.
     */
    @Test
    public void test362AddAccountLavender() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserAddDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_LAVENDER_NAME);

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    /**
     * The account cannot be deleted because there is strict dependency on it (from lavender resource).
     */
    @Test
    public void test370DeleteAccountDummy() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, getDummyResourceObject());

        // WHEN
        try {
            when();
            executeChanges(userDelta, null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertFailure(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAccount(user, RESOURCE_DUMMY_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    /**
     * The account cannot be unlinked because there is strict dependency on it (from lavender resource).
     */
    @Test
    public void test372UnlinkAccountDummy() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserUnlinkAccount(USER_JACK_OID, getDummyResourceObject());

        // WHEN
        try {
            when();
            executeChanges(userDelta, null, task, result);

            AssertJUnit.fail("Unexpected success");
        } catch (PolicyViolationException e) {
            displayExpectedException(e);
        }

        // THEN
        then();
        assertFailure(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAccount(user, RESOURCE_DUMMY_OID);

        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    @Test
    public void test374DeleteAccountLavender() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserDeleteDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_LAVENDER_NAME);

        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAccount(user, RESOURCE_DUMMY_OID);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);

    }

    /**
     * This should go well now as the dependency is gone.
     */
    @Test
    public void test376DeleteAccountDummy() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserDeleteAccount(USER_JACK_OID, getDummyResourceObject());

        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }

    /**
     * Beige resource has relaxed dependency on default dummy. Even though the default dummy is no
     * longer there the delete should go smoothly.
     */
    @Test
    public void test378DeleteAccountBeige() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserDeleteDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_BEIGE_NAME);

        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }

    /**
     * Ivory resource has lax dependency on default dummy. Even though the default dummy is no
     * longer there the delete should go smoothly.
     */
    @Test
    public void test379DeleteAccountIvory() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserDeleteDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_IVORY_NAME);

        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }

    /**
     * Resource peru depends on resource yellow, but the dependency is relaxed.
     * The account should be created even if we do not have yellow account yet.
     */
    @Test
    public void test380AddAccountPeru() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        getDummyResource().resetBreakMode();

        // precondition
        assertEncryptedUserPassword(USER_JACK_OID, USER_JACK_PASSWORD);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserAddDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_PERU_NAME);

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);
    }

    @Test
    public void test382AddAccountYellow() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserAddDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_NAME);

        // WHEN
        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 2);

        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);

        assertDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);
    }

    /**
     * Yellow resource has minimum password length constraint. Change password to something shorter.
     * There is dependency yellow<-peru. Make sure that the peru is not affected (the dependency is relaxed)
     * MID-3033, MID-2134
     */
    @Test
    public void test385ModifyUserJackPasswordA() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        // WHEN
        modifyUserChangePassword(USER_JACK_OID, USER_PASSWORD_A_CLEAR, task, result);

        // THEN
        assertPartialError(result);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User after change execution", userJack);
        assertUserJack(userJack);
        assertLiveLinks(userJack, 2);

        // Check account in dummy resource (yellow): password is too short for this, original password should remain there
        assertDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_JACK_PASSWORD);

        // Check account in dummy resource (peru)
        assertDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyPassword(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, USER_PASSWORD_A_CLEAR);

        assertEncryptedUserPassword(userJack, USER_PASSWORD_A_CLEAR);
    }


    @Test
    public void test389DeleteAccountPeru() throws Exception {
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createModifyUserDeleteDummyAccount(USER_JACK_OID, RESOURCE_DUMMY_PERU_NAME);

        when();
        executeChanges(userDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoDummyAccount(RESOURCE_DUMMY_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME);

        // just to be sure
        assertNoDummyAccount(RESOURCE_DUMMY_LAVENDER_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_BEIGE_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_IVORY_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME);
    }

    @Test
    public void test400DavidAndGoliathAssignRole() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_WORLD_NAME, USER_WORLD_FULL_NAME, true);
        userBefore.asObjectable().getOrganizationalUnit().add(PrismTestUtil.createPolyStringType("stone"));
        addObject(userBefore);

        dummyAuditService.clear();

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "stone", USER_WORLD_NAME, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(7);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();

        dummyAuditService.assertHasDelta(0, AuditEventStage.RESOURCE, ChangeType.ADD, ShadowType.class);

        dummyAuditService.assertExecutionDeltas(1, 3);
        dummyAuditService.assertHasDelta(1, ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertHasDelta(1, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1, ChangeType.ADD, ShadowType.class);

        dummyAuditService.assertHasDelta(2, AuditEventStage.RESOURCE, ChangeType.ADD, ShadowType.class);

        dummyAuditService.assertExecutionDeltas(3, 3);
        dummyAuditService.assertHasDelta(3, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(3, ChangeType.ADD, ShadowType.class);

        dummyAuditService.assertHasDelta(4, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);

        dummyAuditService.assertExecutionDeltas(5, 2);
        dummyAuditService.assertHasDelta(5, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(5, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Have a closer look at the last shadow modify delta. Make sure there are no phantom changes.
        ObjectDeltaOperation<?> executionDeltaOp = dummyAuditService.getExecutionDelta(5, ChangeType.MODIFY, ShadowType.class);
        ObjectDelta<?> executionDelta = executionDeltaOp.getObjectDelta();
        displayDumpable("Last execution delta", executionDelta);
        PrismAsserts.assertModifications("Phantom changes in last delta:", executionDelta, 7);
    }

    private void assertDavidGoliath(String userOid, String ou, String name, boolean userEnabled, boolean davidEnabled, boolean goliathEnabled) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, SchemaViolationException, ConflictException, ExpressionEvaluationException, InterruptedException {
        PrismObject<UserType> userAfter = getUser(userOid);
        display("User after", userAfter);

        assertUser(userAfter, userOid, name, USER_WORLD_FULL_NAME, null, null);
        assertAccount(userAfter, RESOURCE_DUMMY_GOLIATH_OID);
        assertAccount(userAfter, RESOURCE_DUMMY_DAVID_OID);
        assertLiveLinks(userAfter, 2);
        if (userEnabled) {
            assertAdministrativeStatusEnabled(userAfter);
        } else {
            assertAdministrativeStatusDisabled(userAfter);
        }

        assertDummyAccount(RESOURCE_DUMMY_DAVID_NAME, name, USER_WORLD_FULL_NAME, davidEnabled);

        assertDummyAccountAttribute(RESOURCE_DUMMY_DAVID_NAME, name,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ou+ " ("+name+") take");

        assertDummyAccountAttribute(RESOURCE_DUMMY_DAVID_NAME, name,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, "the king");

        assertUserProperty(userAfter, UserType.F_LOCALITY, PrismTestUtil.createPolyString(ou+" ("+name+") take throw"));

        assertDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, name, USER_WORLD_FULL_NAME, goliathEnabled);

        assertDummyAccountAttribute(RESOURCE_DUMMY_GOLIATH_NAME, name,
                DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ou+" ("+name+") take throw ("+name+") hit");

        assertUserProperty(userAfter, UserType.F_TITLE, PrismTestUtil.createPolyString(ou+" ("+name+") take throw ("+name+") hit fall"));

        assertDummyAccountAttribute(RESOURCE_DUMMY_DAVID_NAME, name,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, ou+" ("+name+") take throw ("+name+") hit fall ("+name+") win");

    }

    @Test
    public void test401DavidAndGoliathModifyOu() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserReplace(
                userBefore.getOid(), UserType.F_ORGANIZATIONAL_UNIT, task, result, PrismTestUtil.createPolyString("rock"));

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(7);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertHasDelta(0, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertHasDelta(2, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(3,2);
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertHasDelta(4, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(5,2);
        dummyAuditService.assertHasDelta(5,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(5,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Have a closer look at the last shadow modify delta. Make sure there are no phantom changes.
        ObjectDeltaOperation<?> executionDeltaOp = dummyAuditService.getExecutionDelta(5, ChangeType.MODIFY, ShadowType.class);
        ObjectDelta<?> executionDelta = executionDeltaOp.getObjectDelta();
        displayDumpable("Last execution delta", executionDelta);
        PrismAsserts.assertModifications("Phantom changes in last delta:", executionDelta, 7);
    }

    @Test
    public void test403DavidAndGoliathDisableUser() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserReplace(userBefore.getOid(), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, false, false, false);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(5);            // last one is duplicate
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertHasDelta(0, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertHasDelta(2, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(3,2);            // user is again disabled here
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, UserType.class); // lastProvisioningTimestamp
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test404DavidAndGoliathEnableUser() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserReplace(userBefore.getOid(), ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(5);                        // last one is duplicate
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertHasDelta(0, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertHasDelta(2, AuditEventStage.RESOURCE, ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(3,2);            // user is again disabled here
        dummyAuditService.assertHasDelta(3, ChangeType.MODIFY, UserType.class); // lastProvisioningTimestamp
        dummyAuditService.assertHasDelta(3,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        setRecordEnhancedShadowChanges(false, task, result);
    }

    @Test
    public void test405DavidAndGoliathDisableAccountDavid() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        String accountDavidOid = getLiveLinkRefOid(userBefore, RESOURCE_DUMMY_DAVID_OID);
        dummyAuditService.clear();

        // WHEN
        when();
        modifyAccountShadowReplace(accountDavidOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, false, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // lastProvisioningTimestamp
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    /**
     * Try to recompute to make sure it does not destroy anything. The recompute should do nothing.
     */
    @Test
    public void test406DavidAndGoliathRecompute() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        dummyAuditService.clear();

        // WHEN
        when();
        recomputeUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, false, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertNoRecord();
    }

    @Test
    public void test408DavidAndGoliathEnableAccountDavid() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        assertAdministrativeStatusEnabled(userBefore);
        String accountDavidOid = getLiveLinkRefOid(userBefore, RESOURCE_DUMMY_DAVID_OID);
        dummyAuditService.clear();

        // WHEN
        when();
        modifyAccountShadowReplace(accountDavidOid, ACTIVATION_ADMINISTRATIVE_STATUS_PATH, task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_WORLD_NAME, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class); // lastProvisioningTimestamp
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();
    }

    @Test
    public void test410DavidAndGoliathRename() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_WORLD_NAME);
        dummyAuditService.clear();

        // WHEN
        when();
        modifyUserReplace(userBefore.getOid(), UserType.F_NAME, task, result, PrismTestUtil.createPolyString(USER_FIELD_NAME));

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_FIELD_NAME, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertExecutionSuccess();

        // Have a closer look at the last shadow modify delta. Make sure there are no phantom changes.
        ObjectDeltaOperation<?> executionDeltaOp = dummyAuditService.getExecutionDelta(2, ChangeType.MODIFY, ShadowType.class);
        ObjectDelta<?> executionDelta = executionDeltaOp.getObjectDelta();
        displayDumpable("Last execution delta", executionDelta);
        PrismAsserts.assertModifications("Phantom changes in last delta:", executionDelta, 7);
    }

    @Test
    public void test419DavidAndGoliathUnassignRole() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        dummyAuditService.clear();

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);

        // THEN
        then();
        assertSuccess(result, 2);

        assertUserAfter(userBefore.getOid())
            .assertName(USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .assertLiveLinks(0);

        assertNoDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,1);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.DELETE, ShadowType.class);
    }

    @Test
    public void test420DavidAndGoliathAssignRoleGoliathDown() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, userBefore.getOid(),
                UserType.F_LOCALITY);
        userDelta.addModificationReplaceProperty(UserType.F_TITLE);
        executeChanges(userDelta, null, task, result);
        assertSuccess(result);

        userBefore = assertUserBeforeByUsername(USER_FIELD_NAME)
                .assertLiveLinks(0)
                .getObject();

        assertNoDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);

        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).setBreakMode(BreakMode.NETWORK);
        dummyAuditService.clear();

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);

        // THEN
        then();
        result.computeStatus(); // explicitly recompute status here. It was computed before.
        // Inner errors are expected - but those should be pending on retry
        assertInProgress(result);

        assertUserAfter(userBefore.getOid())
            .assertName(USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .assertLocality("rock (field) take throw")
            .assertLiveLinks(2)
            .assertHasProjectionOnResource(RESOURCE_DUMMY_DAVID_OID)
            .projectionOnResource(RESOURCE_DUMMY_GOLIATH_OID) // This is unfinished shadow
                .display()
                .hasUnfinishedPendingOperations();

        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();

        assertDummyAccountByUsername(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .assertEnabled()
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "rock (field) take");

        // Goliath is down. No account.
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,3);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,1);
        dummyAuditService.assertHasDelta(2,ChangeType.MODIFY, UserType.class);
    }

    /**
     *  Recompute. Before retry interval.
     *  Even though resource is now up nothing should happen (yet).
     *  MID-1566
     */
    @Test
    public void test421DavidAndGoliathAssignRoleGoliathUpRecompute() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();
        dummyAuditService.clear();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);

        // WHEN
        recomputeUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBefore.getOid())
        .assertName(USER_FIELD_NAME)
        .assertFullName(USER_WORLD_FULL_NAME)
        .assertLocality("rock (field) take throw")
        .assertLiveLinks(2)
        .assertHasProjectionOnResource(RESOURCE_DUMMY_DAVID_OID)
        .projectionOnResource(RESOURCE_DUMMY_GOLIATH_OID) // This is unfinished shadow
            .display()
            .hasUnfinishedPendingOperations();

        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();

        assertDummyAccountByUsername(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .assertEnabled()
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "rock (field) take");

        // Goliath is up. But haven't retried yet. hence no account.
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
    }


    /**
     *  Wait for retry interval. Then recompute.
     *  Even though resource is now up nothing should happen (yet) as recompute
     *  is meant to be "lightweight" and it should not trigger refresh.
     *  MID-1566
     */
    @Test
    public void test422DavidAndGoliathAssignRoleGoliathUpRecompute() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();
        dummyAuditService.clear();

        clockForward("PT1H");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);

        // WHEN
        recomputeUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBefore.getOid())
        .assertName(USER_FIELD_NAME)
        .assertFullName(USER_WORLD_FULL_NAME)
        .assertLocality("rock (field) take throw")
        .assertLiveLinks(2)
        .assertHasProjectionOnResource(RESOURCE_DUMMY_DAVID_OID)
        .projectionOnResource(RESOURCE_DUMMY_GOLIATH_OID) // This is unfinished shadow
            .display()
            .hasUnfinishedPendingOperations();

        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();

        assertDummyAccountByUsername(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .assertEnabled()
            .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "rock (field) take");

        // Goliath is up. But haven't retried yet. hence no account.
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(0);
    }

    /**
     * Reconcile after retry interval. Now we are rocking ... and things will finally
     * get fixed.
     * MID-1566
     */
    @Test
    public void test423DavidAndGoliathAssignRoleGoliathUpReconcile() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();
        dummyAuditService.clear();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);

        // WHEN
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "rock", USER_FIELD_NAME, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
    }

    @Test
    public void test428DavidAndGoliathUnassignRole() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();
        dummyAuditService.clear();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        deadShadowOid = assertUserAfter(userBefore.getOid())
            .assertName(USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .singleLink()
                .resolveTarget()
                    .display()
                    .assertDead()
                    .getObject().getOid();

        assertNoDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(4);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,1);
        dummyAuditService.assertExecutionDeltas(1,2);
        dummyAuditService.assertHasDelta(0, ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(1, ChangeType.DELETE, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(2,2);
        dummyAuditService.assertHasDelta(1, ChangeType.DELETE, ShadowType.class);
    }

    /**
     * Let the dead shadow expire. Let's check that it is gone and that it is
     * also properly unlinked. It also cleans the slate for next tests.
     */
    @Test
    public void test429ExpireDeadShadow() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        getDummyResource(RESOURCE_DUMMY_DAVID_NAME).resetBreakMode();
        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();
        dummyAuditService.clear();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);

        clockForward("P10D");

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        // during this operation (more specifically get shadow) shadow is refreshed, pending operations deleted and
        // modifyTimestamp updated, meaning deadRetentionPeriod will not be exceeded
        reconcileUser(userBefore.getOid(), task, result);

        // this will remove shadow during shadow refresh, since it's dead and modifications didn't happend
        clockForward("P10D");
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(userBefore.getOid())
            .assertName(USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .assertLinks(0, 0);

        assertNoRepoShadow(deadShadowOid);

        assertNoDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME);
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);
    }

    @Test
    public void test430DavidAndGoliathAssignRoleDavidDown() throws Exception {
        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_FIELD_NAME);
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, userBefore.getOid(),
                UserType.F_LOCALITY);
        userDelta.addModificationReplaceProperty(UserType.F_TITLE);
        executeChanges(userDelta, null, task, result);
        assertSuccess(result);

        userBefore = findUserByUsername(USER_FIELD_NAME);
        display("User before", userBefore);

        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).setBreakMode(BreakMode.NONE);
        getDummyResource(RESOURCE_DUMMY_DAVID_NAME).setBreakMode(BreakMode.NETWORK);

        dummyAuditService.clear();

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_FIGHT_OID, task, result);

        // THEN
        then();
//        assertInProgress(result);

        assertUserAfter(userBefore.getOid())
            .assertName(USER_FIELD_NAME)
            .assertFullName(USER_WORLD_FULL_NAME)
            .assertLiveLinks(1)
            .projectionOnResource(RESOURCE_DUMMY_DAVID_OID)
                .hasUnfinishedPendingOperations(); // This is unfinished shadow

        getDummyResource(RESOURCE_DUMMY_DAVID_NAME).resetBreakMode();

        // David is down. No account.
        assertNoDummyAccount(RESOURCE_DUMMY_DAVID_NAME, USER_FIELD_NAME);

        // Goliath depends on David, no account.
        assertNoDummyAccount(RESOURCE_DUMMY_GOLIATH_NAME, USER_FIELD_NAME);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(0,3);
        dummyAuditService.assertHasDelta(0,ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(0,ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertExecutionDeltas(1,1);
        dummyAuditService.assertHasDelta(1,ChangeType.MODIFY, UserType.class);

    }

    @Test
    public void test440DavidAndGoliathAssignRoleAndCreateUserInOneStep() throws Exception {
        getDummyResource(RESOURCE_DUMMY_GOLIATH_NAME).resetBreakMode();
        getDummyResource(RESOURCE_DUMMY_DAVID_NAME).resetBreakMode();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        // delete user and his roles which were added before

        PrismObject<UserType> userWorld = findUserByUsername(USER_FIELD_NAME);

        AssertJUnit.assertNotNull("User must not be null.", userWorld);

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object()
                .createDeleteDelta(UserType.class, userWorld.getOid());
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        deltas.add(delta);
        modelService.executeChanges(deltas, null, task, result);

        OperationResult deleteResult = createOperationResult("Check if user was deleted properly.");
        try {
            repositoryService.getObject(UserType.class, userWorld.getOid(), null, deleteResult);
        } catch (ObjectNotFoundException ex) {
            //this is OK, we deleted user before
        }

        // GIVEN
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);



        PrismObject<UserType> userBefore = createUser(USER_WORLD_NAME, USER_WORLD_FULL_NAME, true);
        userBefore.asObjectable().getOrganizationalUnit().add(PrismTestUtil.createPolyStringType("stone"));

        PrismContainerValue<AssignmentType> cval = prismContext.itemFactory().createContainerValue();
        PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
        targetRef.getValue().setOid(ROLE_FIGHT_OID);
        targetRef.getValue().setTargetType(RoleType.COMPLEX_TYPE);
        //noinspection unchecked,rawtypes
        userBefore.findOrCreateContainer(UserType.F_ASSIGNMENT).add((PrismContainerValue) cval);

        // this should add user and at the sate time assign the role fight..->
        // the result of the operation have to be the same as in test 400
        addObject(userBefore);

        dummyAuditService.clear();

        expect();
        assertSuccess(result);

        assertDavidGoliath(userBefore.getOid(), "stone", USER_WORLD_NAME, true, true, true);

        // Check audit
        displayDumpable("Audit", dummyAuditService);
    }

    @Test
    public void test500PrepareJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // ... to satisfy yellow password policy
        modifyUserChangePassword(USER_JACK_OID, "d3adM3Nt3llN0tal3s", task, result);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);
        assertLiveLinks(userBefore, 1);

        // WHEN
        when();
        deleteUserAccount(USER_JACK_OID, RESOURCE_DUMMY_YELLOW_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test501JackAssignDummyDarkYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);
        assertLiveLinks(userBefore, 0);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_DARK_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 1);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
    }

    @Test
    public void test502JackAssignDummyDarkPeru() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_DUMMY_DARK_PERU_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertLiveLinks(userAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
    }

    /**
     * MID-4554
     */
    @Test
    public void test504JackUnassignDummyDarkPeru() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_DARK_PERU_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
    }

    @Test
    public void test507JackUnassignDummyDarkYellow() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_DARK_YELLOW_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
    }

    @Test
    public void test508JackDeleteDummyDarkYellowAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        // WHEN
        when();
        deleteUserAccount(USER_JACK_OID, RESOURCE_DUMMY_DARK_YELLOW_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 1);

        assertNoDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
    }

    @Test
    public void test509JackDeleteDummyDarkPeruAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        // WHEN
        when();
        deleteUserAccount(USER_JACK_OID, RESOURCE_DUMMY_DARK_PERU_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test510JackAssignRoleDarkYellowPeru() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Old password too short for yellow resource
        modifyUserChangePassword(USER_JACK_OID, "123abc456QWE", task, result);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_DARK_YELLOW_PERU_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
    }

    /**
     * MID-4554
     */
    @Test
    public void test519JackUnassignDarkRoleYellowPeru() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_DARK_YELLOW_PERU_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
    }

    @Test
    public void test520JackAssignRoleDarkYellowPeru() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        // Old password too short for yellow resource
        modifyUserChangePassword(USER_JACK_OID, "123abc456QWE", task, result);

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        assertAssignments(userBefore, 0);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_DARK_YELLOW_PERU_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
    }

    /**
     * MID-4554
     */
    @Test
    public void test529JackUnassignRoleDarkYellowPeru() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User before", userBefore);
        AssignmentType currentAssignment = findAssignmentByTargetRequired(userBefore, ROLE_DARK_YELLOW_PERU_OID);

        // WHEN
        when();
        unassign(UserType.class, USER_JACK_OID, currentAssignment.getId(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 0);
        assertLiveLinks(userAfter, 2);

        assertDummyAccount(RESOURCE_DUMMY_DARK_YELLOW_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
        assertDummyAccount(RESOURCE_DUMMY_DARK_PERU_NAME, ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, false);
    }
}
