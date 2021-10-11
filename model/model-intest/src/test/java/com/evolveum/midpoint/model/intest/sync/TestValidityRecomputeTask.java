/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.TestActivation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author Radovan Semancik
 * @see TestActivation
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestValidityRecomputeTask extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    protected static final File ROLE_RED_JUDGE_FILE = new File(TEST_DIR, "role-red-judge.xml");
    protected static final String ROLE_RED_JUDGE_OID = "12345111-1111-2222-1111-121212111222";

    protected static final File ROLE_BIG_JUDGE_FILE = new File(TEST_DIR, "role-big-judge.xml");
    protected static final String ROLE_BIG_JUDGE_OID = "12345111-1111-2222-1111-121212111224";

    private static final XMLGregorianCalendar LONG_LONG_TIME_AGO = XmlTypeConverter.createXMLGregorianCalendar(1111, 1, 1, 12, 0, 0);

    private XMLGregorianCalendar drakeValidFrom;
    private XMLGregorianCalendar drakeValidTo;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(ROLE_RED_JUDGE_FILE, initResult);
        repoAddObjectFromFile(ROLE_BIG_JUDGE_FILE, initResult);

        DebugUtil.setDetailedDebugDump(true);
    }

    protected String getValidityScannerTaskFileName() {
        return TASK_VALIDITY_SCANNER_FILENAME;
    }

    @Test
    public void test100ImportValidityScannerTask() throws Exception {
        // GIVEN
        // Pretend that the user was added a long time ago
        clock.override(LONG_LONG_TIME_AGO);
        addObject(USER_HERMAN_FILE);
        // Make sure that it is effectively disabled
        PrismObject<UserType> userHermanBefore = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanBefore, ActivationStatusType.DISABLED);
        assertValidityStatus(userHermanBefore, TimeIntervalStatusType.BEFORE);
        clock.resetOverride();

        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        /// WHEN
        when();
        importObjectFromFile(getValidityScannerTaskFileName());

        waitForValidityTaskStart();
        waitForValidityTaskFinish();

        // THEN
        then();
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastScanTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);

        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.ENABLED);
        assertValidityStatus(userHermanAfter, TimeIntervalStatusType.IN);
    }

    @Test
    public void test110JackAssignJudgeDisabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.DISABLED);

        testJackAssignRoleJudgeInvalid(activationType, task, result);
    }

    @Test
    public void test111JackAssignJudgeNotYetValid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validFrom = clock.currentTimeXMLGregorianCalendar();
        validFrom.add(XmlTypeConverter.createDuration(60 * 60 * 1000)); // one hour ahead
        activationType.setValidFrom(validFrom);

        testJackAssignRoleJudgeInvalid(activationType, task, result);
    }

    @Test
    public void test112JackAssignJudgeAfterValidity() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(-60 * 60 * 1000)); // one hour ago
        activationType.setValidTo(validTo);

        testJackAssignRoleJudgeInvalid(activationType, task, result);
    }

    @Test
    public void test115JackAssignJudgeEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        testJackAssignRoleJudgeValid(activationType, task, result);
    }

    @Test
    public void test115JackAssignJudgeValid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validFrom = clock.currentTimeXMLGregorianCalendar();
        validFrom.add(XmlTypeConverter.createDuration(-60 * 60 * 1000)); // one hour ago
        activationType.setValidFrom(validFrom);
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(60 * 60 * 1000)); // one hour ahead
        activationType.setValidTo(validTo);

        testJackAssignRoleJudgeValid(activationType, task, result);
    }

    private void testJackAssignRoleJudgeValid(
            ActivationType activationType, Task task, OperationResult result) throws Exception {

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_BIG_JUDGE_OID, activationType, task, result);

        // THEN
        then();
        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(null, USER_JACK_USERNAME);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertLinks(user, 2);
        assert11xUserOk(user);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);

        // CLEANUP
        unassignAllRoles(USER_JACK_OID);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
    }

    private void testJackAssignRoleJudgeInvalid(
            ActivationType activationType, Task task, OperationResult result) throws Exception {

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_BIG_JUDGE_OID, activationType, task, result);

        // THEN
        then();
        assertNoDummyAccount(null, USER_JACK_USERNAME);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertNoDummyAccount(null, USER_JACK_USERNAME);
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoLinkedAccount(user);
        assert11xUserOk(user);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);

        // CLEANUP
        unassignAllRoles(USER_JACK_OID);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
    }

    private void assert11xUserOk(PrismObject<UserType> user) {
        assertAdministrativeStatusEnabled(user);
        assertEffectiveActivation(user, ActivationStatusType.ENABLED);
    }

    @Test
    public void test120JackDisableAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);
        assignRole(USER_JACK_OID, ROLE_BIG_JUDGE_OID, activationType, task, result);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
                ActivationStatusType.DISABLED, task, result);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoDummyAccount(null, USER_JACK_USERNAME);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_JACK_USERNAME, USER_JACK_FULL_NAME, false);

        assert11xUserOk(user);
    }

    @Test
    public void test122JackReplaceNullAdministrativeStatusAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
                null, task, result);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
    }

    @Test
    public void test123JackDisableAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
                ActivationStatusType.DISABLED, task, result);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
    }

    @Test
    public void test124JackEnableAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_JACK_OID, judgeAssignment.getId(),
                ActivationStatusType.ENABLED, task, result);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
    }

    @Test
    public void test125JackDeleteAdministrativeStatusAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        when();
        modifyObjectDeleteProperty(UserType.class, USER_JACK_OID,
                ItemPath.create(UserType.F_ASSIGNMENT, judgeAssignment.getId(), AssignmentType.F_ACTIVATION,
                        ActivationType.F_ADMINISTRATIVE_STATUS),
                task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
    }

    @Test
    public void test126JackAddAdministrativeStatusAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);

        // WHEN
        when();
        modifyObjectAddProperty(UserType.class, USER_JACK_OID,
                ItemPath.create(UserType.F_ASSIGNMENT, judgeAssignment.getId(),
                        AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
    }

    @Test
    public void test127JackDeleteActivationAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);
        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        when();
        modifyObjectDeleteContainer(UserType.class, USER_JACK_OID,
                ItemPath.create(UserType.F_ASSIGNMENT, judgeAssignment.getId(), AssignmentType.F_ACTIVATION),
                task, result, activationType);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
    }

    @Test
    public void test128JackAssignmentJudgeValidToSetInvalid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);
        ActivationType activationType = new ActivationType();
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(-60 * 60 * 1000)); // one hour ago
        activationType.setValidTo(validTo);

        // WHEN
        when();
        modifyObjectReplaceContainer(UserType.class, USER_JACK_OID,
                ItemPath.create(UserType.F_ASSIGNMENT, judgeAssignment.getId(), AssignmentType.F_ACTIVATION),
                task, result, activationType);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
        assert11xUserOk(user);
    }

    @Test
    public void test129JackAssignmentJudgeValidToSetValid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoDummyAccount(null, USER_JACK_USERNAME);
        AssignmentType judgeAssignment = getJudgeAssignment(USER_JACK_OID);
        XMLGregorianCalendar validTo = clock.currentTimeXMLGregorianCalendar();
        validTo.add(XmlTypeConverter.createDuration(60 * 60 * 1000)); // one hour ahead

        // WHEN
        when();
        modifyObjectReplaceProperty(UserType.class, USER_JACK_OID,
                ItemPath.create(UserType.F_ASSIGNMENT, judgeAssignment.getId(), AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO),
                task, result, validTo);

        // THEN
        then();
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("User after", user);
        assertDummyAccount(null, USER_JACK_USERNAME);

        assert11xUserOk(user);

        // CLEANUP
        unassignAllRoles(USER_JACK_OID);
        assertNoDummyAccount(null, USER_JACK_USERNAME);
    }

    private AssignmentType getJudgeAssignment(String userOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<UserType> user = getUser(userOid);
        List<AssignmentType> assignments = user.asObjectable().getAssignment();
        assertEquals("Wrong num ass", 1, assignments.size());
        return assignments.iterator().next();
    }

    /**
     * The test13x works with two roles for the same resource, enabling/disabling them.
     */
    @Test
    public void test130BarbossaAssignJudgeEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // preconditions
        assertNoAssignments(USER_BARBOSSA_OID);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        when();
        assignRole(USER_BARBOSSA_OID, ROLE_JUDGE_OID, activationType, task, result);

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test131BarbossaAssignSailorEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        when();
        assignRole(USER_BARBOSSA_OID, ROLE_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test132BarbossaDisableAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.DISABLED, task, result);

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertNoDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                RESOURCE_DUMMY_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test133BarbossaDisableAssignmentSailor() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.DISABLED, task, result);

        // THEN
        then();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test134BarbossaEnableAssignmentJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.ENABLED, task, result);

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test135BarbossaEnableAssignmentSailor() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.ENABLED, task, result);

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test136BarbossaDisableBothAssignments() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                        USER_BARBOSSA_OID,
                        ItemPath.create(UserType.F_ASSIGNMENT, judgeAssignment.getId(),
                                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                        ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(ItemPath.create(UserType.F_ASSIGNMENT, sailorAssignment.getId(),
                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.DISABLED);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test137BarbossaEnableBothAssignments() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                        USER_BARBOSSA_OID,
                        ItemPath.create(
                                UserType.F_ASSIGNMENT,
                                judgeAssignment.getId(),
                                AssignmentType.F_ACTIVATION,
                                ActivationType.F_ADMINISTRATIVE_STATUS),
                        ActivationStatusType.ENABLED);
        objectDelta.addModificationReplaceProperty(ItemPath.create(
                UserType.F_ASSIGNMENT,
                sailorAssignment.getId(),
                AssignmentType.F_ACTIVATION,
                ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(null, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(null, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                RESOURCE_DUMMY_DRINK, ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    /**
     * Unassign disabled assignments.
     */
    @Test
    public void test139BarbossaDisableBothAssignmentsUnassign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_JUDGE_OID);
        AssignmentType judgeAssignmentLight = new AssignmentType();
        judgeAssignmentLight.setId(judgeAssignment.getId());
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_SAILOR_OID);
        AssignmentType sailorAssignmentLight = new AssignmentType();
        sailorAssignmentLight.setId(sailorAssignment.getId());

        ObjectDelta<UserType> objectDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                        USER_BARBOSSA_OID,
                        ItemPath.create(
                                UserType.F_ASSIGNMENT,
                                judgeAssignment.getId(),
                                AssignmentType.F_ACTIVATION,
                                ActivationType.F_ADMINISTRATIVE_STATUS),
                        ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(ItemPath.create(
                UserType.F_ASSIGNMENT,
                sailorAssignment.getId(),
                AssignmentType.F_ACTIVATION,
                ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.DISABLED);

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        waitForValidityNextRunAssertSuccess();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);
        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);

        objectDelta =
                prismContext.deltaFactory().object().createModificationDeleteContainer(UserType.class,
                        USER_BARBOSSA_OID, UserType.F_ASSIGNMENT, judgeAssignmentLight);
        objectDelta.addModificationDeleteContainer(UserType.F_ASSIGNMENT, sailorAssignmentLight);

        displayDumpable("Unassign delta", objectDelta);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);

        user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 0);

        assertNoAssignments(user);

        principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    /**
     * The 14x tests are similar than test13x tests, they work with two roles for the same resource, enabling/disabling them.
     * The 14x work with the red dummy resource that does disable instead of account delete.
     */
    @Test
    public void test140BarbossaAssignRedJudgeEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // preconditions
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User before", user);
        assertLinks(user, 0);
        assertNoAssignments(user);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME); // just to be on the safe side
        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME);

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        when();
        assignRole(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID, activationType, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                ROLE_JUDGE_DRINK);

        user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test141BarbossaAssignRedSailorEnabled() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        activationType.setAdministrativeStatus(ActivationStatusType.ENABLED);

        // WHEN
        when();
        assignRole(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID, activationType, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test142BarbossaDisableAssignmentRedJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.DISABLED, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test143BarbossaDisableAssignmentRedSailor() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.DISABLED, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test144BarbossaEnableAssignmentRedJudge() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.ENABLED, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                ROLE_JUDGE_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test145BarbossaEnableAssignmentRedSailor() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType assignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        // WHEN
        when();
        modifyAssignmentAdministrativeStatus(USER_BARBOSSA_OID, assignment.getId(),
                ActivationStatusType.ENABLED, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test146BarbossaDisableBothRedAssignments() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                        USER_BARBOSSA_OID,
                        ItemPath.create(
                                UserType.F_ASSIGNMENT,
                                judgeAssignment.getId(),
                                AssignmentType.F_ACTIVATION,
                                ActivationType.F_ADMINISTRATIVE_STATUS),
                        ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(ItemPath.create(
                UserType.F_ASSIGNMENT,
                sailorAssignment.getId(),
                AssignmentType.F_ACTIVATION,
                ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.DISABLED);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test147BarbossaEnableBothRedAssignments() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);

        ObjectDelta<UserType> objectDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                        USER_BARBOSSA_OID,
                        ItemPath.create(
                                UserType.F_ASSIGNMENT,
                                judgeAssignment.getId(),
                                AssignmentType.F_ACTIVATION,
                                ActivationType.F_ADMINISTRATIVE_STATUS),
                        ActivationStatusType.ENABLED);
        objectDelta.addModificationReplaceProperty(ItemPath.create(
                UserType.F_ASSIGNMENT,
                sailorAssignment.getId(),
                AssignmentType.F_ACTIVATION,
                ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, true);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME,
                ROLE_JUDGE_TITLE);
        assertDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME,
                ROLE_JUDGE_DRINK, ROLE_SAILOR_DRINK);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertAuthorized(principal, AUTZ_PUNISH_URL);
    }

    /**
     * Unassign disabled assignments.
     */
    @Test
    public void test149BarbossaDisableBothRedAssignmentsUnassign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        AssignmentType judgeAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_JUDGE_OID);
        AssignmentType judgeAssignmentLight = new AssignmentType();
        judgeAssignmentLight.setId(judgeAssignment.getId());
        AssignmentType sailorAssignment = getUserAssignment(USER_BARBOSSA_OID, ROLE_RED_SAILOR_OID);
        AssignmentType sailorAssignmentLight = new AssignmentType();
        sailorAssignmentLight.setId(sailorAssignment.getId());

        ObjectDelta<UserType> objectDelta =
                prismContext.deltaFactory().object().createModificationReplaceProperty(UserType.class,
                        USER_BARBOSSA_OID,
                        ItemPath.create(UserType.F_ASSIGNMENT, judgeAssignment.getId(),
                                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                        ActivationStatusType.DISABLED);
        objectDelta.addModificationReplaceProperty(ItemPath.create(
                UserType.F_ASSIGNMENT,
                sailorAssignment.getId(),
                AssignmentType.F_ACTIVATION,
                ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.DISABLED);

        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        waitForValidityNextRunAssertSuccess();
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME);
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);
        MidPointPrincipal principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);

        objectDelta =
                prismContext.deltaFactory().object().createModificationDeleteContainer(UserType.class,
                        USER_BARBOSSA_OID, UserType.F_ASSIGNMENT, judgeAssignmentLight);
        objectDelta.addModificationDeleteContainer(UserType.F_ASSIGNMENT, sailorAssignmentLight);

        displayDumpable("Unassign delta", objectDelta);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME); // to be on the safe side

        // WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, false);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME);
        assertNoDummyAccountAttribute(RESOURCE_DUMMY_RED_NAME, USER_BARBOSSA_USERNAME,
                DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME);
        assertNoDummyAccount(null, USER_BARBOSSA_USERNAME); // to be on the safe side

        user = getUser(USER_BARBOSSA_OID);
        display("User after", user);
        assertLinks(user, 1);

        assertNoAssignments(user);

        principal = focusProfileService.getPrincipal(user);
        assertNotAuthorized(principal, AUTZ_PUNISH_URL);
    }

    @Test
    public void test190HermanGoesInvalid() throws Exception {
        // GIVEN
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userHermanBefore = getUser(USER_HERMAN_OID);
        XMLGregorianCalendar validTo = userHermanBefore.asObjectable().getActivation().getValidTo();
        assertEffectiveActivation(userHermanBefore, ActivationStatusType.ENABLED);
        assertValidityStatus(userHermanBefore, TimeIntervalStatusType.IN);

        // Let's move the clock tiny bit after herman's validTo
        validTo.add(XmlTypeConverter.createDuration(100));
        clock.override(validTo);

        /// WHEN
        when();
        waitForValidityNextRunAssertSuccess();

        // THEN
        then();

        // THEN
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();

        PrismObject<UserType> userHermanAfter = getUser(USER_HERMAN_OID);
        assertEffectiveActivation(userHermanAfter, ActivationStatusType.DISABLED);
        assertValidityStatus(userHermanAfter, TimeIntervalStatusType.AFTER);

        assertLastScanTimestamp(TASK_VALIDITY_SCANNER_OID, startCal, endCal);
    }

    @Test
    public void test200ImportTriggerScannerTask() throws Exception {
        // GIVEN
        XMLGregorianCalendar startCal = clock.currentTimeXMLGregorianCalendar();

        /// WHEN
        when();
        importObjectFromFile(TASK_TRIGGER_SCANNER_FILE);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID, false);
        waitForTaskFinish(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        then();
        XMLGregorianCalendar endCal = clock.currentTimeXMLGregorianCalendar();
        assertLastScanTimestamp(TASK_TRIGGER_SCANNER_OID, startCal, endCal);

    }

    /**
     * Explicitly disable Elaine's red account. Do this at the beginning of the test. We will
     * move time ahead in later tests. This account should remain here exactly like this
     * at the end of all tests.
     */
    @Test
    public void test205AccountRedElaineDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyAccountShadowReplace(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID, ACTIVATION_ADMINISTRATIVE_STATUS_PATH,
                task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> accountShadow = getShadowModel(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID);
        assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
    }

    /**
     * Note: red resource disables account on unassign, does NOT delete it.
     * Just the recompute trigger is set
     */
    @Test
    public void test210JackAssignAndUnassignAccountRed() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // assign
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_JACK_OID,
                RESOURCE_DUMMY_RED_OID, null, true);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        // unassign
        deltas = new ArrayList<>();
        userDelta = createAccountAssignmentUserDelta(USER_JACK_OID, RESOURCE_DUMMY_RED_OID, null, false);
        deltas.add(userDelta);

        // WHEN
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        // Let's wait for the task to give it a change to screw up
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        assertUserJack(userJack, "Jack Sparrow", "Jack", "Sparrow");

        String accountRedOid = getLinkRefOid(userJack, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);

        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        end.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
        assertTrigger(accountRed, RecomputeTriggerHandler.HANDLER_URI, start, end);
        assertAdministrativeStatusDisabled(accountRed);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_DEPROVISION);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", false);
    }

    /**
     * Move time a month ahead. The account that was disabled in a previous test should be
     * deleted now.
     */
    @Test
    public void test215JackDummyAccountDeleteAfterMonth() throws Exception {
        // GIVEN
        XMLGregorianCalendar time = clock.currentTimeXMLGregorianCalendar();
        // A month and a day, to make sure we move past the trigger
        time.add(XmlTypeConverter.createDuration(true, 0, 1, 1, 0, 0, 0));

        // WHEN
        when();
        clock.override(time);

        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        then();

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, ACCOUNT_JACK_DUMMY_USERNAME);
    }

    @Test
    public void test220AddDrake() throws Exception {
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        displayValue("Start", start);

        PrismObject<UserType> userDrake = PrismTestUtil.parseObject(USER_DRAKE_FILE);
        UserType userDrakeType = userDrake.asObjectable();

        // Activation
        ActivationType activationType = new ActivationType();
        userDrakeType.setActivation(activationType);
        drakeValidFrom = clock.currentTimeXMLGregorianCalendar();
        drakeValidFrom.add(XmlTypeConverter.createDuration(true, 0, 0, 10, 0, 0, 0));
        activationType.setValidFrom(drakeValidFrom);
        drakeValidTo = clock.currentTimeXMLGregorianCalendar();
        drakeValidTo.add(XmlTypeConverter.createDuration(true, 0, 0, 80, 0, 0, 0));
        activationType.setValidTo(drakeValidTo);

        // Assignment: dummy red
        AssignmentType assignmentType = new AssignmentType();
        userDrakeType.getAssignment().add(assignmentType);
        ConstructionType constructionType = new ConstructionType();
        assignmentType.setConstruction(constructionType);
        constructionType.setKind(ShadowKindType.ACCOUNT);
        ObjectReferenceType resourceRedRef = new ObjectReferenceType();
        resourceRedRef.setOid(RESOURCE_DUMMY_RED_OID);
        constructionType.setResourceRef(resourceRedRef);

        // the following assignments are used only to generate superfluous searches
        // in validity scanner task
        AssignmentType dummyAssignmentType1 = new AssignmentType();
        userDrakeType.getAssignment().add(dummyAssignmentType1);
        dummyAssignmentType1.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_SUPERUSER_OID, ObjectTypes.ROLE));
        dummyAssignmentType1.setActivation(activationType.clone());

        AssignmentType dummyAssignmentType2 = new AssignmentType();
        userDrakeType.getAssignment().add(dummyAssignmentType2);
        dummyAssignmentType2.setTargetRef(ObjectTypeUtil.createObjectRef(ROLE_SUPERUSER_OID, ObjectTypes.ROLE));
        dummyAssignmentType2.setActivation(activationType.clone());
        dummyAssignmentType2.setDescription("just to differentiate");

        display("Drake before", userDrake);

        // WHEN
        when();

        addObject(userDrake);

        // THEN
        // Give the tasks a chance to screw up
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // Make sure that it is effectivelly disabled
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        assertLinks(userDrakeAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake");
    }

    @Test
    public void test222Drake4DaysBeforeValidFrom() throws Exception {
        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(false, 0, 0, 4, 0, 0, 0));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        // Make sure that it is effectivelly disabled
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", false);
    }

    @Test
    public void test224Drake1DaysAfterValidFrom() throws Exception {
        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.ENABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", true);
    }

    @Test
    public void test226Drake1DayBeforeValidTo() throws Exception {
        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(false, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.ENABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", true);
    }

    @Test
    public void test228Drake1DayAfterValidTo() throws Exception {
        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 1, 0, 0, 0));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", false);
    }

    @Test
    public void test230Drake20DaysAfterValidTo() throws Exception {
        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 20, 0, 0, 0));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        String accountRedOid = getLinkRefOid(userDrakeAfter, RESOURCE_DUMMY_RED_OID);
        PrismObject<ShadowType> accountRed = getShadowModel(accountRedOid);
        display("Drake account RED after", accountRed);
        assertDisableReasonShadow(accountRed, SchemaConstants.MODEL_DISABLE_REASON_MAPPED);

        assertDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake", "Francis Drake", false);
    }

    @Test
    public void test232Drake40DaysAfterValidTo() throws Exception {
        XMLGregorianCalendar start = (XMLGregorianCalendar) drakeValidTo.clone();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 40, 0, 0, 0));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();
        waitForTaskNextRunAssertSuccess(TASK_TRIGGER_SCANNER_OID, true);

        // THEN
        PrismObject<UserType> userDrakeAfter = getUser(USER_DRAKE_OID);
        display("Drake after", userDrakeAfter);
        assertEffectiveActivation(userDrakeAfter, ActivationStatusType.DISABLED);

        assertLinks(userDrakeAfter, 0);

        assertNoDummyAccount(RESOURCE_DUMMY_RED_NAME, "drake");
    }

    /**
     * Elaine's red account was explicitly disabled. We have moved the time ahead in previous tests.
     * But this account should remain as it is.
     */
    @Test
    public void test250CheckAccountRedElaine() throws Exception {
        // GIVEN

        // WHEN
        // nothing to do

        // THEN

        PrismObject<ShadowType> accountShadow = getShadowModel(ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID);
        assertDisableReasonShadow(accountShadow, SchemaConstants.MODEL_DISABLE_REASON_EXPLICIT);
    }

    private XMLGregorianCalendar judgeAssignmentValidFrom;
    private XMLGregorianCalendar judgeAssignmentValidTo;

    @Test
    public void test300HermanAssignJudgeNotYetValid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ActivationType activationType = new ActivationType();
        judgeAssignmentValidFrom = clock.currentTimeXMLGregorianCalendar();
        judgeAssignmentValidFrom.add(XmlTypeConverter.createDuration(10 * 60 * 1000)); // 10 minutes ahead
        activationType.setValidFrom(judgeAssignmentValidFrom);
        judgeAssignmentValidTo = clock.currentTimeXMLGregorianCalendar();
        judgeAssignmentValidTo.add(XmlTypeConverter.createDuration(30 * 60 * 1000)); // 30 minutes ahead
        activationType.setValidTo(judgeAssignmentValidTo);
        displayValue("Assignment validFrom", judgeAssignmentValidFrom);
        displayValue("Assignment validTo", judgeAssignmentValidTo);

        // WHEN
        when();
        assignRole(USER_HERMAN_OID, ROLE_JUDGE_OID, activationType, task, result);

        // THEN
        then();
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
    }

    @Test
    public void test310HermanAssignJudgeBecomesValid() throws Exception {
        // GIVEN
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User before", user);
        XMLGregorianCalendar start = (XMLGregorianCalendar) judgeAssignmentValidFrom.clone();
        start.add(XmlTypeConverter.createDuration(60 * 1000));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();

        assertRoleJudgeValid();
    }

    @Test
    public void test315HermanAssignJudgeBecomesInValid() throws Exception {
        // GIVEN
        XMLGregorianCalendar start = (XMLGregorianCalendar) judgeAssignmentValidTo.clone();
        start.add(XmlTypeConverter.createDuration(Duration.ofMinutes(1).toMillis()));
        clock.override(start);
        displayValue("Start", start);

        // WHEN
        // just wait
        waitForValidityNextRunAssertSuccess();

        assertRoleJudgeInValid();
    }

    private void assertRoleJudgeValid() throws Exception {
        assertDummyAccount(null, USER_HERMAN_USERNAME);
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after", user);
        assertLinks(user, 1);
    }

    private void assertRoleJudgeInValid() throws Exception {
        assertNoDummyAccount(null, USER_HERMAN_USERNAME);
        PrismObject<UserType> user = getUser(USER_HERMAN_OID);
        display("User after", user);
        assertLinks(user, 0);
    }

    private void modifyAssignmentAdministrativeStatus(String userOid, long assignmentId, ActivationStatusType status, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        if (status == null) {
            modifyObjectReplaceProperty(UserType.class, userOid,
                    ItemPath.create(
                            UserType.F_ASSIGNMENT,
                            assignmentId,
                            AssignmentType.F_ACTIVATION,
                            ActivationType.F_ADMINISTRATIVE_STATUS),
                    task, result);
        } else {
            modifyObjectReplaceProperty(UserType.class, userOid,
                    ItemPath.create(
                            UserType.F_ASSIGNMENT,
                            assignmentId,
                            AssignmentType.F_ACTIVATION,
                            ActivationType.F_ADMINISTRATIVE_STATUS),
                    task, result, status);
        }
    }

    protected void waitForValidityTaskFinish() throws Exception {
        waitForTaskFinish(TASK_VALIDITY_SCANNER_OID, true);
    }

    protected void waitForValidityTaskStart() throws Exception {
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID, false);
    }

    protected void waitForValidityNextRunAssertSuccess() throws Exception {
        waitForTaskNextRunAssertSuccess(TASK_VALIDITY_SCANNER_OID, true);
    }
}
