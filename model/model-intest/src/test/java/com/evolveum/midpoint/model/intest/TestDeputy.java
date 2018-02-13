/*
 * Copyright (c) 2016-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.sync.TestValidityRecomputeTask;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test for deputy (delegation) mechanism.
 *
 * MID-3472
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDeputy extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/deputy");

	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("User Jack", userJack);
        assertNoAssignments(userJack);
        assertLinks(userJack, 0);
        assertNoAuthorizations(userJack);

        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        display("User Barbossa", userBarbossa);
        assertNoAssignments(userBarbossa);
        assertLinks(userBarbossa, 0);
        assertNoAuthorizations(userBarbossa);
	}

	/**
	 * Jack and Barbossa does not have any accounts or roles.
	 * Assign Barbossa as Jack's deputy. Not much should happen.
	 */
    @Test
    public void test100AssignDeputyNoBigDeal() throws Exception {
		final String TEST_NAME = "test100AssignDeputyNoBigDeal";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLinks(userJackAfter, 0);
        assertNoAuthorizations(userJackAfter);

    }

    /**
	 * Jack and Barbossa does not have any accounts or roles.
	 * Unassign Barbossa as Jack's deputy. Not much should happen.
	 */
    @Test
    public void test109UnassignDeputyNoBigDeal() throws Exception {
		final String TEST_NAME = "test109UnassignDeputyNoBigDeal";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertNoAssignments(userJackAfter);
        assertLinks(userJackAfter, 0);
        assertNoAuthorizations(userJackAfter);

    }

    /**
	 * Still not much here. Just preparing Jack.
	 * Make sure that Barbossa is not affected though.
	 */
    @Test
    public void test110AssignJackPirate() throws Exception {
		final String TEST_NAME = "test110AssignJackPirate";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

    }

    /**
	 * Assign Barbossa as Jack's deputy. Barbossa should get equivalent
	 * accounts and authorizations as Jack.
	 */
    @Test
    public void test112AssignDeputyPirate() throws Exception {
		final String TEST_NAME = "test112AssignDeputyPirate";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);

    }

    // TODO: recompute barbossa, recompute jack

    /**
	 * Unassign Barbossa as Jack's deputy. Barbossa should get
	 * back to emptiness.
	 */
    @Test
    public void test119UnassignDeputyPirate() throws Exception {
		final String TEST_NAME = "test119UnassignDeputyPirate";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_PIRATE_OID);
        assertAssignments(userJackAfter, 1);
        assertAccount(userJackAfter, RESOURCE_DUMMY_OID);
        assertLinks(userJackAfter, 1);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL);

    }

    /**
	 * Guybrush and Barbossa does not have any accounts or roles. Yet.
	 * Assign Barbossa as Guybrush's deputy. Not much should happen.
	 */
    @Test
    public void test120AssignbarbossaDeputyOfGuybrush() throws Exception {
		final String TEST_NAME = "test120AssignbarbossaDeputyOfGuybrush";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush before", userGuybrushBefore);
        assertLinks(userGuybrushBefore, 1);

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputy(USER_BARBOSSA_OID, USER_GUYBRUSH_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLinks(userGuybrushAfter, 1);
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
		final String TEST_NAME = "test122AssignGuybrushPirate";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignedRole(userGuybrushAfter, ROLE_PIRATE_OID);
        assertAssignments(userGuybrushAfter, 1);
        assertAccount(userGuybrushAfter, RESOURCE_DUMMY_OID);
        assertLinks(userGuybrushAfter, 1);
        assertAuthorizations(userGuybrushAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLinks(userBarbossaAfter, 0);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

    }

    /**
	 * Recompute Barbossa. Barbossa should get the deputy rights
	 * from Guybrush after recompute.
	 */
    @Test
    public void test124RecomputeBarbossa() throws Exception {
		final String TEST_NAME = "test124RecomputeBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        recomputeUser(USER_BARBOSSA_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignedRole(userGuybrushAfter, ROLE_PIRATE_OID);
        assertAssignments(userGuybrushAfter, 1);
        assertAccount(userGuybrushAfter, RESOURCE_DUMMY_OID);
        assertLinks(userGuybrushAfter, 1);
        assertAuthorizations(userGuybrushAfter, AUTZ_LOOT_URL);

    }

    /**
	 * Unassign Guybrush pirate role. Barbossa is Guybrushe's deputy,
	 * but Barbossa should be only partially affected yet.
	 */
    @Test
    public void test126UnassignGuybrushPirate() throws Exception {
		final String TEST_NAME = "test126UnassignGuybrushPirate";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignRole(USER_GUYBRUSH_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);

    }

    /**
	 * Recompute Barbossa. Barbossa should get the deputy rights
	 * from Guybrush after recompute.
	 */
    @Test
    public void test128RecomputeBarbossa() throws Exception {
		final String TEST_NAME = "test128RecomputeBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        // WHEN
        displayWhen(TEST_NAME);

        recomputeUser(USER_BARBOSSA_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_GUYBRUSH_OID);
        assertAssignments(userBarbossaAfter, 1);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

    }

    @Test
    public void test129UnassignBarbossaDeputyOfGuybrush() throws Exception {
		final String TEST_NAME = "test129UnassignBarbossaDeputyOfGuybrush";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputy(USER_BARBOSSA_OID, USER_GUYBRUSH_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertNoAssignments(userGuybrushAfter);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

    }

    /**
	 * Assign more roles and orgs to Jack. We will use these for
	 * selective delegation in subsequent tests.
	 */
    @Test
    public void test150AssignJackMoreRoles() throws Exception {
		final String TEST_NAME = "test150AssignJackMoreRoles";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(USER_JACK_OID, ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE,
        		null, null, (ActivationType) null, true);
        // Captain is NOT delegable
        userDelta.addModification((createAssignmentModification(ROLE_CAPTAIN_OID, RoleType.COMPLEX_TYPE,
        		null, null, (ActivationType) null, true)));
        userDelta.addModification((createAssignmentModification(ROLE_RED_SAILOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, (ActivationType) null, true)));
        userDelta.addModification((createAssignmentModification(ROLE_CYAN_SAILOR_OID, RoleType.COMPLEX_TYPE,
        		null, null, (ActivationType) null, true)));
        userDelta.addModification((createAssignmentModification(ORG_SWASHBUCKLER_SECTION_OID, OrgType.COMPLEX_TYPE,
        		null, null, (ActivationType) null, true)));
        userDelta.addModification((createAssignmentModification(ORG_MINISTRY_OF_RUM_OID, OrgType.COMPLEX_TYPE,
        		SchemaConstants.ORG_MANAGER, null, (ActivationType) null, true)));
        userDelta.addModification((createAssignmentModification(ROLE_EMPTY_OID, RoleType.COMPLEX_TYPE,
        		null, null, (ActivationType) null, true)));

        // WHEN
        displayWhen(TEST_NAME);

		modelService.executeChanges(MiscSchemaUtil.createCollection(userDelta), null, task, result);

        // THEN
        displayThen(TEST_NAME);
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
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

    }

    @Test
    public void test152AssignbarbossaDeputyLimitedDeputy() throws Exception {
		final String TEST_NAME = "test152AssignbarbossaDeputyLimitedDeputy";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_PIRATE_OID),
        		createOrgReference(ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER));

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test154UnassignbarbossaDeputyLimitedDeputy() throws Exception {
		final String TEST_NAME = "test154UnassignbarbossaDeputyLimitedDeputy";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_PIRATE_OID),
        		createOrgReference(ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER));

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertLinks(userBarbossaAfter, 0);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test156AssignbarbossaDeputyLimitedDeputyRed() throws Exception {
		final String TEST_NAME = "test156AssignbarbossaDeputyLimitedDeputyRed";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_RED_SAILOR_OID),
        		createOrgReference(ORG_MINISTRY_OF_RUM_OID) // There is no assignment like this in Jack
        );

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_SAIL_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test159UnassignbarbossaDeputyLimitedDeputyRed() throws Exception {
		final String TEST_NAME = "test159UnassignbarbossaDeputyLimitedDeputyRed";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_RED_SAILOR_OID),
        		createOrgReference(ORG_MINISTRY_OF_RUM_OID) // There is no assignment like this in Jack
        );

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test160AssignbarbossaDeputyLimitedDeputyEmpty() throws Exception {
		final String TEST_NAME = "test160AssignbarbossaDeputyLimitedDeputyEmpty";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_EMPTY_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test162UnassignbarbossaDeputyLimitedDeputyEmpty() throws Exception {
		final String TEST_NAME = "test162UnassignbarbossaDeputyLimitedDeputyEmpty";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_EMPTY_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 7);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL);

    }

    @Test
    public void test170AddRoleDrinker() throws Exception {
		final String TEST_NAME = "test170AssignJackRoleDrinker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<RoleType> role = PrismTestUtil.parseObject(ROLE_DRINKER_FILE);
        display("Adding role", role);

        // WHEN
        displayWhen(TEST_NAME);
        addObject(role, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, ROLE_DRINKER_OID);
        display("Role after", roleAfter);
        assertAssignedOrg(roleAfter, ORG_MINISTRY_OF_RUM_OID);
        assertHasOrg(roleAfter, ORG_MINISTRY_OF_RUM_OID);
    }

    @Test
    public void test172AssignJackRoleDrinker() throws Exception {
		final String TEST_NAME = "test170AssignJackRoleDrinker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_DRINKER_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
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
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);
    }

    @Test
    public void test174AssignBarbossaDeputyLimitedDeputyDrinker() throws Exception {
		final String TEST_NAME = "test174AssignBarbossaDeputyLimitedDeputyDrinker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignedRole(userJackAfter, ROLE_DRINKER_OID);
        assertAssignments(userJackAfter, 8);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);
        
        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertNotAssignedRole(userBarbossaAfter, ROLE_DRINKER_OID);
        assertLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_DRINK_URL);
    }
    
    /**
     * Deputy of a deputy. Limited, allow transitive.
     * MID-4176
     */
    @Test
    public void test176AssignbarGuybrushLimitedDeputyOfBarbossa() throws Exception {
		final String TEST_NAME = "test176AssignbarGuybrushLimitedDeputyOfBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushBefore);
        assertAssignments(userGuybrushBefore, 0);
        assertNotAssignedRole(userGuybrushBefore, ROLE_DRINKER_OID);
        assertLinks(userGuybrushBefore, 0);
        assertNoAuthorizations(userGuybrushBefore);


        // WHEN
        displayWhen(TEST_NAME);

        assignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, 
        		assignment -> assignment.beginLimitTargetContent().allowTransitive(true),
        		task, result,
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertNotAssignedRole(userGuybrushAfter, ROLE_DRINKER_OID);
        assertLinks(userGuybrushAfter, 0);
        assertAuthorizations(userGuybrushAfter, AUTZ_DRINK_URL);
    }

    @Test
    public void test178UnassignBarbossaDeputyLimitedDeputyDrinker() throws Exception {
		final String TEST_NAME = "test178UnassignBsarbossaDeputyLimitedDeputyDrinker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }
    
    @Test
    public void test179UnassignbarGuybrushLimitedDeputyOfBarbossa() throws Exception {
		final String TEST_NAME = "test179UnassignbarGuybrushLimitedDeputyOfBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID,
        		assignment -> assignment.beginLimitTargetContent().allowTransitive(true),
        		task, result,
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 0);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);

    }
    
    @Test
    public void test180AssignBarbossaDeputyOfJack() throws Exception {
		final String TEST_NAME = "test180AssignBarbossaDeputyOfJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertNotAssignedRole(userBarbossaAfter, ROLE_DRINKER_OID);
        assertLinks(userBarbossaAfter, 3);
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);
    }
    
    /**
     * Deputy of a deputy. Limited, do NOT allow transitive.
     */
    @Test
    public void test182AssignGuybrushLimitedDeputyOfBarbossa() throws Exception {
		final String TEST_NAME = "test182AssignGuybrushLimitedDeputyOfBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushBefore);
        assertAssignments(userGuybrushBefore, 0);
        assertNotAssignedRole(userGuybrushBefore, ROLE_DRINKER_OID);
        assertLinks(userGuybrushBefore, 0);
        assertNoAuthorizations(userGuybrushBefore);


        // WHEN
        displayWhen(TEST_NAME);

        assignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result,
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertNotAssignedRole(userGuybrushAfter, ROLE_DRINKER_OID);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }
    
    @Test
    public void test184UnassignGuybrushLimitedDeputyOfBarbossa() throws Exception {
		final String TEST_NAME = "test182AssignGuybrushLimitedDeputyOfBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputyLimits(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result,
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 0);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }

    /**
     * Deputy of a deputy. Unlimited, do NOT allow transitive.
     */
    @Test
    public void test186AssignGuybrushDeputyOfBarbossa() throws Exception {
		final String TEST_NAME = "test182AssignGuybrushLimitedDeputyOfBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userGuybrushBefore = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushBefore);
        assertAssignments(userGuybrushBefore, 0);
        assertNotAssignedRole(userGuybrushBefore, ROLE_DRINKER_OID);
        assertLinks(userGuybrushBefore, 0);
        assertNoAuthorizations(userGuybrushBefore);


        // WHEN
        displayWhen(TEST_NAME);

        assignDeputy(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 1);
        assertAssignedDeputy(userGuybrushAfter, USER_BARBOSSA_OID);
        assertNotAssignedRole(userGuybrushAfter, ROLE_DRINKER_OID);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }
    
    @Test
    public void test188UnassignGuybrushDeputyOfBarbossa() throws Exception {
		final String TEST_NAME = "test188UnassignGuybrushDeputyOfBarbossa";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputy(USER_GUYBRUSH_OID, USER_BARBOSSA_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userGuybrushAfter = getUser(USER_GUYBRUSH_OID);
        display("User Guybrush after", userGuybrushAfter);
        assertAssignments(userGuybrushAfter, 0);
        assertLinks(userGuybrushAfter, 0);
        assertNoAuthorizations(userGuybrushAfter);
    }

    @Test
    public void test189UnassignBarbossaDeputyOfJack() throws Exception {
		final String TEST_NAME = "test189UnassignBarbossaDeputyOfJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 0);
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
    }

    @Test
    public void test190AssignBarbossaDeputyLimitedDeputyEmptyDrinker() throws Exception {
		final String TEST_NAME = "test190AssignBarbossaDeputyLimitedDeputyEmptyDrinker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_EMPTY_OID),
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertLinks(userBarbossaAfter, 1);
        assertAuthorizations(userBarbossaAfter, AUTZ_DRINK_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }

    @Test
    public void test192UnassignbarbossaDeputyLimitedDeputyEmptyDrinker() throws Exception {
		final String TEST_NAME = "test192UnassignbarbossaDeputyLimitedDeputyEmptyDrinker";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);

        unassignDeputyLimits(USER_BARBOSSA_OID, USER_JACK_OID, task, result,
        		createRoleReference(ROLE_EMPTY_OID),
        		createRoleReference(ROLE_DRINKER_OID)
        );

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertNoAssignments(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);
        TestUtil.assertModifyTimestamp(userBarbossaAfter, startTs, endTs);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }


    @Test
    public void test800ImportValidityScannerTask() throws Exception {
		final String TEST_NAME = "test800ImportValidityScannerTask";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TestValidityRecomputeTask.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

		/// WHEN
        displayWhen(TEST_NAME);
        importObjectFromFile(TASK_VALIDITY_SCANNER_FILENAME);

        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, false);
        waitForTaskFinish(TASK_VALIDITY_SCANNER_OID, true);

        // THEN
        displayThen(TEST_NAME);
		XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastRecomputeTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);
	}

    /**
	 * Assign Barbossa as Jack's deputy. Barbossa should have all the privileges now.
	 * But they will expire soon ...
	 */
    @Test
    public void test802AssignBarbossaDeputyOfJack() throws Exception {
		final String TEST_NAME = "test802AssignBarbossaDeputyOfJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userJackBefore = getUser(USER_JACK_OID);
        display("User Jack before", userJackBefore);
        assertAssignments(userJackBefore, 8);
        assertLinks(userJackBefore, 3);
        assertAuthorizations(userJackBefore, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        ActivationType activationType = new ActivationType();
        activationType.setValidTo(XmlTypeConverter.addDuration(startTs, "PT2H"));

        // WHEN
        displayWhen(TEST_NAME);

        assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, assignment -> assignment.setActivation(activationType),
        		task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAssignments(userBarbossaAfter, 1);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_OID);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_CYAN_OID);
        assertLinks(userBarbossaAfter, 3);
        // Command autz should NOT be here, it is not delegable MID-3550
        assertAuthorizations(userBarbossaAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_DRINK_URL);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }

    /**
	 * Assign Barbossa as Jack's deputy. Barbossa privileges are about to expire.
	 */
    @Test
    public void test804BarbosaThreeHoursLater() throws Exception {
		final String TEST_NAME = "test804BarbosaThreeHoursLater";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        clock.overrideDuration("PT3H");

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextRunAssertSuccess(TASK_VALIDITY_SCANNER_OID, true);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userBarbossaAfter = getUser(USER_BARBOSSA_OID);
        display("User Barbossa after", userBarbossaAfter);
        assertAssignedDeputy(userBarbossaAfter, USER_JACK_OID);
        assertAssignedNoRole(userBarbossaAfter);
        assertAccount(userBarbossaAfter, RESOURCE_DUMMY_RED_OID); // Resource red has delayed delete
        assertLinks(userBarbossaAfter, 1);
        assertNoAuthorizations(userBarbossaAfter);

        PrismObject<UserType> userJackAfter = getUser(USER_JACK_OID);
        display("User Jack after", userJackAfter);
        assertAssignments(userJackAfter, 8);
        assertLinks(userJackAfter, 3);
        assertAuthorizations(userJackAfter, AUTZ_LOOT_URL, AUTZ_SAIL_URL, AUTZ_SAIL_URL, AUTZ_COMMAND_URL, AUTZ_DRINK_URL);

    }

}
