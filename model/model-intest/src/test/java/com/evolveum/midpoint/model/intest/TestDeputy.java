/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Test for deputy (delegation) mechanism.
 * <p>
 * MID-3472
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDeputy extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/deputy");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User Jack", userJack);
        assertNoAssignments(userJack);
        assertLiveLinks(userJack, 0);
        assertNoAuthorizations(userJack);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        display("User Barbossa", userBarbossa);
        assertNoAssignments(userBarbossa);
        assertLiveLinks(userBarbossa, 0);
        assertNoAuthorizations(userBarbossa);
    }

    /**
     * Jack and Barbossa does not have any accounts or roles.
     * Assign Barbossa as Jack's deputy. Not much should happen.
     */
    @Test
    public void test100AssignDeputyNoBigDeal() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLiveLinks(userJackAfter, 0);
        assertNoAuthorizations(userJackAfter);

    }

    /**
     * Jack and Barbossa does not have any accounts or roles.
     * Unassign Barbossa as Jack's deputy. Not much should happen.
     */
    @Test
    public void test109UnassignDeputyNoBigDeal() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLiveLinks(userJackAfter, 0);
        assertNoAuthorizations(userJackAfter);

    }

    /**
     * Still not much here. Just preparing Jack.
     * Make sure that Barbossa is not affected though.
     */
    @Test
    public void test110AssignJackPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

    }

    /**
     * Assign Barbossa as Jack's deputy. Barbossa should get equivalent
     * accounts and authorizations as Jack.
     */
    @Test
    public void test112AssignDeputyPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);

    }

    // TODO: recompute barbossa, recompute jack

    /**
     * Unassign Barbossa as Jack's deputy. Barbossa should get
     * back to emptiness.
     */
    @Test
    public void test119UnassignDeputyPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);

    }

    /**
     * Guybrush and Barbossa does not have any accounts or roles. Yet.
     * Assign Barbossa as Guybrush's deputy. Not much should happen.
     */
    @Test
    public void test120AssignbarbossaDeputyOfGuybrush() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush before", userGuybrushBefore);
        assertLiveLinks(userGuybrushBefore, 1);

        // WHEN
        when();

        assignDeputy(USER_BARBOSSA_OID, USER_GUYBRUSH_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLiveLinks(userGuybrushAfter, 1);
        assertNoAuthorizations(userGuybrushAfter);

    }

    /**
     * Assign Guybrush pirate role. Barbossa is Guybrushe's deputy,
     * but Barbossa should be only partially affected yet.
     * Barbossa should not have the accounts, but he should have the
     * authorization. Barbossa will be completely affected after recompute.
     */
    @Test
    public void test122AssignGuybrushPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignedRole(userGuybrushAfter, ROLE_PIRATE_OID);
        assertAssignments(userGuybrushAfter, 1);
        assertAccount(userGuybrushAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userGuybrushAfter, 1);
        assertAuthorizations(userGuybrushAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLiveLinks(userBarbossaAfter, 0);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

    }

    /**
     * Recompute Barbossa. Barbossa should get the deputy rights
     * from Guybrush after recompute.
     */
    @Test
    public void test124RecomputeBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        recomputeUser(USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignedRole(userGuybrushAfter, ROLE_PIRATE_OID);
        assertAssignments(userGuybrushAfter, 1);
        assertAccount(userGuybrushAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userGuybrushAfter, 1);
        assertAuthorizations(userGuybrushAfter, AUTZ_LOOT_URL);

    }

    /**
     * Unassign Guybrush pirate role. Barbossa is Guybrushe's deputy,
     * but Barbossa should be only partially affected yet.
     */
    @Test
    public void test126UnassignGuybrushPirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignRole(USER_GUYBRUSH_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);

    }

    /**
     * Recompute Barbossa. Barbossa should get the deputy rights
     * from Guybrush after recompute.
     */
    @Test
    public void test128RecomputeBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        when();

        recomputeUser(USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

    }

    @Test
    public void test129UnassignBarbossaDeputyOfGuybrush() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputy(USER_BARBOSSA_OID, USER_GUYBRUSH_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

    }

    /**
     * Assign more roles and orgs to Jack. We will use these for
     * selective delegation in subsequent tests.
     */
    @Test
    public void test150AssignJackMoreRoles() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(
                USER_JACK_OID, ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null, null, null, true);
        // Captain is NOT delegable
        userDelta.addModification((createAssignmentModification(ROLE_CAPTAIN_OID,
                RoleType.COMPLEX_TYPE, null, null, null, true)));
        userDelta.addModification((createAssignmentModification(ROLE_RED_SAILOR_OID,
                RoleType.COMPLEX_TYPE, null, null, null, true)));
        userDelta.addModification((createAssignmentModification(ROLE_CYAN_SAILOR_OID,
                RoleType.COMPLEX_TYPE, null, null, null, true)));
        userDelta.addModification((createAssignmentModification(ORG_SWASHBUCKLER_SECTION_OID,
                OrgType.COMPLEX_TYPE, null, null, null, true)));
        userDelta.addModification((createAssignmentModification(ORG_MINISTRY_OF_RUM_OID,
                OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER, null, null, true)));
        userDelta.addModification((createAssignmentModification(ROLE_EMPTY_OID,
                RoleType.COMPLEX_TYPE, null, null, null, true)));

        // WHEN
        when();

        modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignedRole(userJackAfter, ROLE_EMPTY_OID);
        assertAssignedRole(userJackAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userJackAfter, 7);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertAccount(userJackAfter, RESOURCE_DUMMY_RED_OID);
        assertAccount(userJackAfter, RESOURCE_DUMMY_CYAN_OID);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

    }

    @Test
    public void test152AssignbarbossaDeputyLimitedDeputy() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_PIRATE_OID),
                createOrgReference(ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test154UnassignbarbossaDeputyLimitedDeputy() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_PIRATE_OID),
                createOrgReference(ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLiveLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test156AssignbarbossaDeputyLimitedDeputyRed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_RED_SAILOR_OID),
                createOrgReference(ORG_MINISTRY_OF_RUM_OID) // There is no assignment like this in Jack
        );

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_SAIL_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test159UnassignbarbossaDeputyLimitedDeputyRed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_RED_SAILOR_OID),
                createOrgReference(ORG_MINISTRY_OF_RUM_OID)); // There is no assignment like this in Jack

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);
    }

    @Test
    public void test160AssignbarbossaDeputyLimitedDeputyEmpty() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_EMPTY_OID));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test162UnassignbarbossaDeputyLimitedDeputyEmpty() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_EMPTY_OID));

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test170AddRoleDrinker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_DRINKER_FILE);
        display("Adding role", role);

        // WHEN
        when();
        addObject(role, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_DRINKER_OID);
        display("Role after", roleAfter);
        assertAssignedOrg(roleAfter, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(roleAfter, ORG_MINISTRY_OF_RUM_OID);
    }

    @Test
    public void test172AssignJackRoleDrinker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_DRINKER_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignedRole(userJackAfter, ROLE_EMPTY_OID);
        assertAssignedRole(userJackAfter, ROLE_DRINKER_OID);
        assertAssignedRole(userJackAfter, ROLE_CAPTAIN_OID);
        assertAssignments(userJackAfter, 8);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertAccount(userJackAfter, RESOURCE_DUMMY_RED_OID);
        assertAccount(userJackAfter, RESOURCE_DUMMY_CYAN_OID);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);
    }

    @Test
    public void test174AssignBarbossaDeputyLimitedDeputyDrinker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_DRINKER_OID));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_DRINKER_OID);
        assertAssignments(userJackAfter, 8);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertNotAssignedRole(userBarbossaAfter, ROLE_DRINKER_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_DRINK_URL);
    }

    /**
     * Deputy of a deputy. Limited, allow transitive.
     * MID-4176
     */
    @Test
    public void test176AssignbarGuybrushLimitedDeputyOfBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushBefore);
        assertAssignments(userGuybrushBefore, 0);
        assertNotAssignedRole(userGuybrushBefore, ROLE_DRINKER_OID);
        assertLiveLinks(userGuybrushBefore, 0);
        assertNoAuthorizations(userGuybrushBefore);

        // WHEN
        when();

        assignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID,
                assignment -> assignment.beginLimitTargetContent().allowTransitive(true),
                task, result,
                createRoleReference(ROLE_DRINKER_OID));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertNotAssignedRole(userGuybrushAfter, ROLE_DRINKER_OID);
        assertLiveLinks(userGuybrushAfter, 0);
        assertAuthorizations(userGuybrushAfter, AUTZ_DRINK_URL);
    }

    @Test
    public void test178UnassignBarbossaDeputyLimitedDeputyDrinker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_DRINKER_OID));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }

    @Test
    public void test179UnassignbarGuybrushLimitedDeputyOfBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID,
                assignment -> assignment.beginLimitTargetContent().allowTransitive(true),
                task, result,
                createRoleReference(ROLE_DRINKER_OID));

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 0);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

    }

    @Test
    public void test180AssignBarbossaDeputyOfJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertNotAssignedRole(userBarbossaAfter, ROLE_DRINKER_OID);
        assertLiveLinks(userBarbossaAfter, 3);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);
    }

    /**
     * Deputy of a deputy. Limited, do NOT allow transitive.
     */
    @Test
    public void test182AssignGuybrushLimitedDeputyOfBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushBefore);
        assertAssignments(userGuybrushBefore, 0);
        assertNotAssignedRole(userGuybrushBefore, ROLE_DRINKER_OID);
        assertLiveLinks(userGuybrushBefore, 0);
        assertNoAuthorizations(userGuybrushBefore);

        // WHEN
        when();

        assignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result,
                createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertNotAssignedRole(userGuybrushAfter, ROLE_DRINKER_OID);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }

    @Test
    public void test184UnassignGuybrushLimitedDeputyOfBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result,
                createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 0);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }

    /**
     * Deputy of a deputy. Unlimited, do NOT allow transitive.
     */
    @Test
    public void test186AssignGuybrushDeputyOfBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushBefore);
        assertAssignments(userGuybrushBefore, 0);
        assertNotAssignedRole(userGuybrushBefore, ROLE_DRINKER_OID);
        assertLiveLinks(userGuybrushBefore, 0);
        assertNoAuthorizations(userGuybrushBefore);

        // WHEN
        when();

        assignDeputy(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertNotAssignedRole(userGuybrushAfter, ROLE_DRINKER_OID);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }

    @Test
    public void test188UnassignGuybrushDeputyOfBarbossa() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputy(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 0);
        assertLiveLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }

    @Test
    public void test189UnassignBarbossaDeputyOfJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 0);
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
    }

    @Test
    public void test190AssignBarbossaDeputyLimitedDeputyEmptyDrinker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_EMPTY_OID),
                createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertLiveLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_DRINK_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }

    @Test
    public void test192UnassignbarbossaDeputyLimitedDeputyEmptyDrinker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
                createRoleReference(ROLE_EMPTY_OID),
                createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }

    @Test
    public void test800ImportValidityScannerTask() throws Exception {
        // GIVEN
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        /// WHEN
        when();
        importObjectFromFile(TASK_VALIDITY_SCANNER_FILENAME);

        waitForTaskStart(TASK_VALIDITY_SCANNER_OID);
        waitForTaskFinish(TASK_VALIDITY_SCANNER_OID);

        // THEN
        then();
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastValidityFullScanTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);
    }

    /**
     * Assign Barbossa as Jack's deputy. Barbossa should have all the privileges now.
     * But they will expire soon ...
     */
    @Test
    public void test802AssignBarbossaDeputyOfJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("User Jack before", userJackBefore);
        assertAssignments(userJackBefore, 8);
        assertLiveLinks(userJackBefore, 3);
        assertAuthorizations(userJackBefore, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        ActivationType activationType = new ActivationType();
        activationType.setValidTo(XmlTypeConverter.addDuration(startTs, "PT2H"));

        // WHEN
        when();

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, assignment -> assignment.setActivation(activationType),
                task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_CYAN_OID);
        assertLiveLinks(userBarbossaAfter, 3);
        // Command autz should NOT be here, it is not delegable MID-3550
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_DRINK_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }

    /**
     * Assign Barbossa as Jack's deputy. Barbossa privileges are about to expire.
     */
    @Test
    public void test804BarbosaThreeHoursLater() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clock.overrideDuration("PT3H");

        // WHEN
        when();

        waitForTaskNextRunAssertSuccess(TASK_VALIDITY_SCANNER_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLiveLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLiveLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }

}
