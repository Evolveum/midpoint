/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.rbac;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentValidity extends AbstractRbacTest {

    private XMLGregorianCalendar jackPirateValidTo;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);
    }

    /**
     * MID-4110
     */
    @Test
    public void test100JackAssignRolePirateValidTo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * Assignment expires.
     * MID-4110, MID-4114
     */
    @Test
    public void test102Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test104JackAssignRolePirateAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * Unassign valid assignment. Only invalid assignment remains.
     * MID-4110
     */
    @Test
    public void test106JackUnassignRolePirateValid() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-4110
     */
    @Test
    public void test109JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * Raw modification of assignment. The assignment is not effective immediately,
     * as this is raw operation. So, nothing much happens. Yet.
     * MID-4110
     */
    @Test
    public void test110JackAssignRolePirateValidToRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        ModelExecuteOptions options = executeOptions().raw();

        // WHEN
        when();
        modifyUserAssignment(USER_JACK_OID, ROLE_PIRATE_OID, RoleType.COMPLEX_TYPE, null,
                task, null, activationType, true, options, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-4110, MID-4114
     */
    @Test
    public void test111RecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * Assignment expires.
     * MID-4110, MID-4114
     */
    @Test
    public void test112Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test114JackAssignRolePirateAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test119JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * Sailor is an idempotent(conservative) role.
     * MID-4110
     */
    @Test
    public void test120JackAssignRoleSailorValidTo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummySailorAccount();
    }

    /**
     * Assignment expires.
     * MID-4110, MID-4114
     */
    @Test
    public void test122Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test124JackAssignRoleSailorAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummySailorAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test129JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * Raw modification of assignment. The assignment is not effective immediately,
     * as this is raw operation. So, nothing much happens. Yet.
     * MID-4110
     */
    @Test
    public void test130JackAssignRoleSailorValidToRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        ModelExecuteOptions options = executeOptions().raw();

        // WHEN
        when();
        modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null,
                task, null, activationType, true, options, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-4110, MID-4114
     */
    @Test
    public void test131RecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummySailorAccount();
    }

    /**
     * Assignment expires.
     * MID-4110, MID-4114
     */
    @Test
    public void test132Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test134JackAssignRoleSailorAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummySailorAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test139JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * This time do not recompute. Just set everything up, let the assignment expire
     * and assign the role again.
     * MID-4110
     */
    @Test
    public void test140JackAssignRoleSailorValidToRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        ModelExecuteOptions options = executeOptions().raw();

        // WHEN
        when();
        modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null,
                task, null, activationType, true, options, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * Assignment expires. BUt do NOT recompute.
     * MID-4110
     */
    @Test
    public void test142Forward15min() throws Exception {
        // WHEN
        when();
        clockForward("PT15M");
        // do NOT recompute

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null); // Not recomputed
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test144JackAssignRoleSailorAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummySailorAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test149JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * MID-4110
     */
    @Test
    public void test150JackAssignRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignments(userAfter, 1);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * Sailor is an idempotent(conservative) role.
     * MID-4110
     */
    @Test
    public void test151JackAssignRoleSailorValidTo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignments(userAfter, 2);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateSailorAccount();
    }

    /**
     * Assignment expires.
     * MID-4110, MID-4114
     */
    @Test
    public void test153Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        assertJackDummyPirateAccount();
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test154JackAssignRoleSailorAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateSailorAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test159JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * MID-4110
     */
    @Test
    public void test160JackAssignRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignments(userAfter, 1);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test161JackAssignRoleSailorValidToRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        ModelExecuteOptions options = executeOptions().raw();

        // WHEN
        when();
        modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null,
                task, null, activationType, true, options, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignments(userAfter, 2);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, null);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID); // SAILOR is not here, we are raw
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * Recompute should fix it all.
     * MID-4110
     */
    @Test
    public void test162RecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignments(userAfter, 2);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateSailorAccount();
    }

    /**
     * Assignment expires.
     * MID-4110, MID-4114
     */
    @Test
    public void test163Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        assertJackDummyPirateAccount();
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test164JackAssignRoleSailorAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateSailorAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test169JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * MID-4110
     */
    @Test
    public void test170JackAssignRolePirate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignments(userAfter, 1);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test171JackAssignRoleWeakSingerValidTo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);

        assertAssignments(userAfter, 2);
        AssignmentType assignmentSingerTypeAfter = assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        assertEffectiveActivation(assignmentSingerTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);

        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_WEAK_SINGER_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateSingerAccount();
    }

    /**
     * Assignment expires.
     * MID-4110, MID-4114
     */
    @Test
    public void test173Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentPirateTypeAfter = assertAssignedRole(userAfter, ROLE_PIRATE_OID);
        assertEffectiveActivation(assignmentPirateTypeAfter, ActivationStatusType.ENABLED);
        AssignmentType assignmentSailorTypeAfter = assertAssignedRole(userAfter, ROLE_WEAK_SINGER_OID);
        assertEffectiveActivation(assignmentSailorTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        // Dummy attribute "title" is tolerant, so the singer value remains
        assertJackDummyPirateSingerAccount();
    }

    /**
     * New assignment. No time validity.
     * MID-4110
     */
    @Test
    public void test174JackAssignRoleSingerAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_WEAK_SINGER_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 3);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_WEAK_SINGER_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateSingerAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test179JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * This time do both assigns as raw. And do NOT recompute until everything is set up.
     * MID-4110
     */
    @Test
    public void test180JackAssignRoleSailorValidToRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        // WHEN
        when();
        modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null,
                task, null, activationType, true, executeOptions().raw(), result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, null);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-4110
     */
    @Test
    public void test182Forward15minAndAssignRaw() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        modifyUserAssignment(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, RoleType.COMPLEX_TYPE, null,
                task, null, null, true, executeOptions().raw(), result);

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    /**
     * MID-4110, MID-4114
     */
    @Test
    public void test184RecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_STRONG_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummySailorAccount();
    }

    /**
     * MID-4110
     */
    @Test
    public void test189JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * Preparation for MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test200JackAssignCurrentPirateFutureSailor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        activationType.setValidFrom(getTimestamp("P1M"));

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_STRONG_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();     // no sailor!
    }

    /**
     * MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test202RecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();     // still there should be no sailor
    }

    /**
     * MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test204ReconcileJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();     // still there should be no sailor
    }

    @Test
    public void test209JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * The same as test200-204 but with ROLE_STRONG_RICH_SAILOR
     * <p>
     * Preparation for MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test210JackAssignCurrentPirateFutureRichSailor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        activationType.setValidFrom(getTimestamp("P1M"));

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_STRONG_RICH_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_RICH_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();     // no sailor!
    }

    /**
     * MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test212RecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();     // still there should be no sailor
    }

    /**
     * MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test214ReconcileJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateAccount();     // still there should be no sailor
    }

    @Test
    public void test219JackUnassignAll() throws Exception {
        unassignAll();
    }

    /**
     * Just verifying that account presence honors assignment validity (checked as part of MID-4199 evaluation)
     */
    @Test
    public void test220JackAssignFutureRichSailor() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        unassignAll();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        activationType.setValidFrom(getTimestamp("P1M"));

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_STRONG_RICH_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_RICH_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter);
        assertDelegatedRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test229JackUnassignAll() throws Exception {
        unassignAll();
        assertNoDummyAccount(USER_JACK_USERNAME);
    }

    /**
     * Attribute-granting assignment that loses its validity. Resource account is granted by another (enabled) assignment.
     */
    @Test
    public void test230JackAssignRoleStrongRichSailorValidTo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        // beware of the order: weapon is weak and is set differently by role (cutlass) and resource (from user extension: pistol, mouth)
        // the assert expects cutlass, so the pirate role assignment must go first
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_STRONG_RICH_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_RICH_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_STRONG_RICH_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateRichSailorAccount();
    }

    /**
     * Sailor assignment expires.
     */
    @Test
    public void test232Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_STRONG_RICH_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        assertJackDummyPirateAccount();
    }

    @Test
    public void test239JackUnassignAll() throws Exception {
        unassignAll();
        assertNoDummyAccount(USER_JACK_USERNAME);
    }

    /**
     * Attribute-granting assignment that loses its validity. Resource account is granted by another (enabled) assignment.
     * Using non-strong (normal i.e. relative) mappings.
     */
    @Test
    public void test240JackAssignRoleRichSailorValidTo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activationType = new ActivationType();
        jackPirateValidTo = getTimestamp("PT10M");
        activationType.setValidTo(jackPirateValidTo);

        XMLGregorianCalendar startTs = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        // beware of the order: weapon is weak and is set differently by role (cutlass) and resource (from user extension: pistol, mouth)
        // the assert expects cutlass, so the pirate role assignment must go first
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        assignRole(USER_JACK_OID, ROLE_RICH_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertSuccess(result);

        XMLGregorianCalendar endTs = clock.currentTimeXMLGregorianCalendar();
        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertModifyMetadata(userAfter, startTs, endTs);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_RICH_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.ENABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID, ROLE_RICH_SAILOR_OID);
        assertDelegatedRef(userAfter);

        assertJackDummyPirateRichSailorAccount();
    }

    /**
     * Sailor assignment expires.
     */

    @Test
    public void test242Forward15min() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT15M");

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 2);
        AssignmentType assignmentTypeAfter = assertAssignedRole(userAfter, ROLE_RICH_SAILOR_OID);
        assertEffectiveActivation(assignmentTypeAfter, ActivationStatusType.DISABLED);
        assertRoleMembershipRef(userAfter, ROLE_PIRATE_OID);

        assertJackDummyPirateAccount();
    }

    @Test
    public void test249JackUnassignAll() throws Exception {
        unassignAll();
        assertNoDummyAccount(USER_JACK_USERNAME);
    }

    /**
     * MID-4198
     */
    @Test
    public void test250JackAssignFocusExistsResource() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        ActivationType activation = new ActivationType();
        activation.setValidFrom(getTimestamp("PT10M"));
        AssignmentType assignment = ObjectTypeUtil.createAssignmentTo(resourceDummyFocusExists, prismContext).activation(activation);

        // WHEN
        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).add(assignment)
                .asObjectDelta(USER_JACK_OID);
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_FOCUS_EXISTS_NAME, USER_JACK_USERNAME);
    }

    /**
     * MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test252RecomputeJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        recomputeUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_FOCUS_EXISTS_NAME, USER_JACK_USERNAME);
    }

    /**
     * MID-4198 "Disabled assignments project value in certain cases"
     */
    @Test
    public void test254ReconcileJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        reconcileUser(USER_JACK_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 1);
        assertLiveLinks(userAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_FOCUS_EXISTS_NAME, USER_JACK_USERNAME);
    }

    private void assertJackDummyPirateAccount() throws Exception {
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Caribbean has ever seen");
        assertNoDummyAccountAttribute(null, ACCOUNT_JACK_DUMMY_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME);
    }

    private void assertJackDummySailorAccount() throws Exception {
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
    }

    private void assertJackDummyPirateSailorAccount() throws Exception {
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Caribbean has ever seen");
    }

    private void assertJackDummyPirateRichSailorAccount() throws Exception {
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Caribbean has ever seen");
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEALTH_NAME,
                1000000);
    }

    private void assertJackDummyPirateSingerAccount() throws Exception {
        assertDefaultDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME, ACCOUNT_JACK_DUMMY_FULLNAME, true);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ROLE_PIRATE_TITLE, ROLE_WEAK_SINGER_TITLE);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, RESOURCE_DUMMY_DRINK);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, USER_JACK_LOCALITY);
        // Outbound mapping for weapon is weak, therefore the mapping in role should override it
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, ROLE_PIRATE_WEAPON);
        assertDefaultDummyAccountAttribute(ACCOUNT_JACK_DUMMY_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME,
                "Jack Sparrow is the best pirate Caribbean has ever seen");
    }

    private void unassignAll() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_JACK_OID);
        display("User jack before", userBefore);

        // WHEN
        when();
        unassignAll(userBefore, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User jack after", userAfter);
        assertAssignments(userAfter, 0);
        assertRoleMembershipRef(userAfter);

        assertNoDummyAccount(ACCOUNT_JACK_DUMMY_USERNAME);
    }
}
