/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLifecycle extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/lifecycle");

    private static final File SYSTEM_CONFIGURATION_LIFECYCLE_FILE = new File(TEST_DIR, "system-configuration-lifecycle.xml");

    // subtype = dataProcessingBasis
    private static final File ROLE_HEADMASTER_FILE = new File(TEST_DIR, "role-headmaster.xml");
    private static final String ROLE_HEADMASTER_OID = "b9c885ba-034b-11e8-a708-13836b619045";

    // subtype = dataProcessingBasis
    private static final File ROLE_CARETAKER_FILE = new File(TEST_DIR, "role-caretaker.xml");
    private static final String ROLE_CARETAKER_OID = "9162a952-034b-11e8-afb7-138a763f2350";

    // no subtype, this is NOT a dataProcessingBasis
    private static final File ROLE_GAMBLER_FILE = new File(TEST_DIR, "role-gambler.xml");
    private static final String ROLE_GAMBLER_OID = "2bb2fb86-034e-11e8-9cf3-77abfc7aafec";

    //no subtype, forced in draft state
    private static final File ROLE_CROUPIER_FILE = new File(TEST_DIR, "role-croupier.xml");

    private static final File ROLE_PIT_BOSS_FILE = new File(TEST_DIR, "role-pit-boss.xml");

    private static final String SUBTYPE_EMPLOYEE = "employee";
    private static final Object USER_JACK_TELEPHONE_NUMBER = "12345654321";

    private static final TestObject<RoleType> ROLE_A =
            TestObject.file(TEST_DIR, "role-a.xml", "1d954756-6e95-11f0-8ff0-0050568cc7f8");

    private static final TestObject<RoleType> ROLE_B =
            TestObject.file(TEST_DIR, "role-b.xml", "650dbd16-7818-11f0-b840-0050568cc7f8");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "resource-dummy-10813.xml",
            "d9c71b78-6d25-11f0-bff2-0050568cc7f8",
            "resource-dummy-10813",
            DummyResourceContoller::populateWithDefaultSchema);

    private DummyResourceContoller dummyResourceCtl;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_HEADMASTER_FILE, initResult);
        repoAddObjectFromFile(ROLE_CARETAKER_FILE, initResult);
        repoAddObjectFromFile(ROLE_GAMBLER_FILE, initResult);
        repoAddObjectFromFile(ROLE_CROUPIER_FILE, initResult);
        repoAddObjectFromFile(ROLE_PIT_BOSS_FILE, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        addObject(ROLE_A, initTask, initResult);
        addObject(ROLE_B, initTask, initResult);

        dummyResourceCtl = initDummyResource(RESOURCE_DUMMY, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_LIFECYCLE_FILE;
    }

    /**
     * Setup jack. Setting subtype to employee will put him under lifecycle
     * control. But before that we want him to have at least one
     * processing basis role.
     * This starts from "draft" state.
     */
    @Test
    public void test050SetupJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignRole(USER_JACK_OID, ROLE_HEADMASTER_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_GAMBLER_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, SchemaConstants.LIFECYCLE_DRAFT);
        modifyUserReplace(USER_JACK_OID, UserType.F_SUBTYPE, task, result, SUBTYPE_EMPLOYEE);
        modifyUserReplace(USER_JACK_OID, UserType.F_TELEPHONE_NUMBER, task, result, USER_JACK_TELEPHONE_NUMBER);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
        assertLifecycleState(userAfter, SchemaConstants.LIFECYCLE_DRAFT);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
        assertEffectiveActivation(userAfter, ActivationStatusType.DISABLED);
        // User is in draft lifecycle. Assignments are not active. Therefore account does not exist yet.
        assertLiveLinks(userAfter, 0);
    }

    @Test
    public void test052PrincipalJackDraft() throws Exception {
        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        then();
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNotAuthorized(principal, AUTZ_GAMBLE_URL);
        assertNotAuthorized(principal, AUTZ_APPARATE_URL);
    }

    /**
     * Transition Jack to proposed lifecycle state (manual transition).
     * Proposed state should have effective status of "disabled" by default.
     * But that is overridden in the lifecycle model. So the user should be
     * enabled.
     */
    @Test
    public void test060TransitionJackToProposed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result, SchemaConstants.LIFECYCLE_PROPOSED);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
        assertLifecycleState(userAfter, SchemaConstants.LIFECYCLE_PROPOSED);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
        assertEffectiveActivation(userAfter, ActivationStatusType.ENABLED);
        // Although we are in the proposed lifecycle and assignments would not be active by default
        // the proposed lifecycle is forcing activation to enabled. Therefore also assignments are
        // considered active.
        getSingleLinkOid(userAfter);
    }

    @Test
    public void test062PrincipalJackProposed() throws Exception {
        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        then();
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        // Although we are in the proposed lifecycle and assignments would not be active by default
        // the proposed lifecycle is forcing activation to enabled. Therefore also assignments are
        // considered active. Their authorizations should be applied to principal.
        assertAuthorized(principal, AUTZ_GAMBLE_URL);
        assertAuthorized(principal, AUTZ_APPARATE_URL);
        // Forced assignment as specified in proposed lifecycle model
        assertAuthorized(principal, AUTZ_PIT_BOSS_URL);
        // and induced authz from pit boss
        assertAuthorized(principal, AUTZ_CROUPIER_URL);
    }

    /**
     * Transition Jack to default lifecycle (active) state (manual transition).
     * This prepares jack for next tests.
     */
    @Test
    public void test090TransitionJackToDefaultActive() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_LIFECYCLE_STATE, task, result /* no value */);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRefs(userAfter, 4);
        assertLifecycleState(userAfter, null);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
        assertEffectiveActivation(userAfter, ActivationStatusType.ENABLED);
        assertLiveLinks(userAfter, 1);
    }

    @Test
    public void test092PrincipalJackDefaultActive() throws Exception {
        // WHEN
        when();
        MidPointPrincipal principal = focusProfileService.getPrincipal(USER_JACK_USERNAME, UserType.class);

        // THEN
        then();
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertAuthorized(principal, AUTZ_GAMBLE_URL);
        assertAuthorized(principal, AUTZ_APPARATE_URL);
        // Forced assignment not specified for active lifecycle state
        assertNotAuthorized(principal, AUTZ_PIT_BOSS_URL);
        assertNotAuthorized(principal, AUTZ_CROUPIER_URL);
    }

    private void assertTelephoneNumber(PrismObject<UserType> user, Object expectedTelephoneNumber) {
        assertEquals("Wrong telephone number in " + user, expectedTelephoneNumber, user.asObjectable().getTelephoneNumber());
    }

    private <O extends ObjectType> void assertLifecycleState(PrismObject<O> object, String expectedLifecycleState) {
        assertEquals("Wrong lifecycle state in " + object, expectedLifecycleState, object.asObjectable().getLifecycleState());
    }

    @Test
    public void test100AssignJackCaretaker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignRole(USER_JACK_OID, ROLE_CARETAKER_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 4);
        assertRoleMembershipRefs(userAfter, 5);
        assertLifecycleState(userAfter, null);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
    }

    @Test
    public void test102UnassignJackHeadmaster() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignRole(USER_JACK_OID, ROLE_HEADMASTER_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRefs(userAfter, 4);
        assertLifecycleState(userAfter, null);
        assertTelephoneNumber(userAfter, USER_JACK_TELEPHONE_NUMBER);
    }

    /**
     * This is the real test. Now lifecycle transition should take
     * place because jack has no processing basis role.
     */
    @Test
    public void test110UnassignJackCaretaker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        unassignRole(USER_JACK_OID, ROLE_CARETAKER_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 2);
        assertLifecycleState(userAfter, SchemaConstants.LIFECYCLE_ARCHIVED);
        assertTelephoneNumber(userAfter, null);
    }

    /**
     * Jack is now archived. So, even if we assign a new processing basis
     * role the lifecycle should not change. Archival is a one-way process.
     */
    @Test
    public void test112UnassignJackCaretaker() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        assignRole(USER_JACK_OID, ROLE_HEADMASTER_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRefs(userAfter, 0);
        assertLifecycleState(userAfter, SchemaConstants.LIFECYCLE_ARCHIVED);
        assertTelephoneNumber(userAfter, null);
    }

    /**
     * todo not finished yet
     *
     * MID-10813
     */
    @Test(enabled = false)
    public void test200LifecycleArchived() throws Exception {
        UserType userA = new UserType();
        userA.setOid("64da8f2e-78b1-11f0-850f-0050568cc7f8");
        userA.setName(PolyStringType.fromOrig("user-a"));
        userA.setLifecycleState(SchemaConstants.LIFECYCLE_ACTIVE);
        userA.beginAssignment()
                .targetRef(ROLE_A.oid, RoleType.COMPLEX_TYPE);

        testUserLifecycleChange(userA);

        UserType userB = new UserType();
        userB.setOid("ba1a9fb8-78b3-11f0-b929-0050568cc7f8");
        userB.setName(PolyStringType.fromOrig("user-b"));
        userB.setLifecycleState(SchemaConstants.LIFECYCLE_ACTIVE);
        userA.beginAssignment()
                .targetRef(ROLE_B.oid, RoleType.COMPLEX_TYPE);

        testUserLifecycleChange(userB);
    }

    private void testUserLifecycleChange(UserType user) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given();

        Assertions.assertThat(dummyResourceCtl.getDummyResource().listAccounts()).isEmpty();

        addObject(user.asPrismObject(), task, result);

        Assertions.assertThat(dummyResourceCtl.getDummyResource().listAccounts()).hasSize(1);

        when();

        ObjectDelta<UserType> deltaA = PrismTestUtil.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ARCHIVED)
                .asObjectDelta(user.getOid());

        executeChanges(deltaA, ModelExecuteOptions.create(), task, result);

        then();

        Assertions.assertThat(dummyResourceCtl.getDummyResource().listAccounts()).isEmpty();
    }
}
