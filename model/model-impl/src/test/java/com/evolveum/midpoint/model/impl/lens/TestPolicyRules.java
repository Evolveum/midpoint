/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.util.RecordingProgressListener;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_PATH;
import static com.evolveum.midpoint.test.util.MidPointAsserts.assertSerializable;

import static org.testng.AssertJUnit.*;

/**
 * Tests triggering of exclusion, assignment and situation rules.
 * Also rule exceptions.
 *
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules extends AbstractLensTest {

    private static final String ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME = "criminal exclusion";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        setDefaultUserTemplate(USER_TEMPLATE_OID);

        repoAddObjectFromFile(USER_DRAKE_FILE, initResult);

        addObject(ROLE_PIRATE_FILE);
        addObject(ROLE_MUTINEER_FILE);
        addObject(ROLE_JUDGE_FILE);
        addObject(ROLE_CONSTABLE_FILE);
        addObject(ROLE_THIEF_FILE);

        addObjects(ROLE_CORP_FILES);

        addObject(USER_LOCALIZED, initTask, initResult);
        addObject(ROLE_LOCALIZED, initTask, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        InternalMonitor.reset();
    }

    @Test
    public void test005JackAttemptAssignRoleJudge() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_JUDGE_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);

        assertEvaluatedTargetPolicyRules(context, 7);
        assertTargetTriggers(context, PolicyConstraintKindType.OBJECT_STATE, 2);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 4);
        assertTargetTriggers(context, null, 6);

        assertSerializable(context);
    }

    @Test(enabled = false)
    public void test007JackAttemptAssignRoleJudgeAsOwner() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID,
                assignment -> assignment.getTargetRef().setRelation(SchemaConstants.ORG_OWNER)
        );

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);

        assertEvaluatedTargetPolicyRules(context, 4);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 0);

        assertSerializable(context);
    }

    /**
     * Mostly preparation for other tests. But good check for no exclusion conflict.
     */
    @Test
    public void test010JackAssignRoleJudge() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID);
    }

    /**
     * We expect 2 assignments rules to be triggered: unassign + all operations.
     */
    @Test
    public void test020JackUnassignRoleJudge() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextUnassignRole(context, USER_JACK_OID, ROLE_JUDGE_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, 7);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 2);
        assertTargetTriggers(context, null, 2);

        assertSerializable(context);
    }

    /**
     * No exclusion here. The assignment should go smoothly.
     */
    @Test
    public void test100AssignRoleMutineerToJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_MUTINEER_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignAccountToJack(context);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        assertEvaluatedTargetPolicyRules(context, 7);
        assertTargetTriggers(context,  null, 0);

        assertSerializable(context);
    }

    @Test(enabled = false)          // after MID-4797 the projector.project now raises PolicyViolationException on conflicting roles
    public void test110AssignRolePirateToJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignAccountToJack(context);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        assertEvaluatedTargetPolicyRules(context, 7);
        EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, true);
        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_PIRATE_OID, trigger.getConflictingAssignment().getTarget().getOid());

        assertSerializable(context);
    }

    /**
     * Assignment with an exception from the exclusion rule.
     */
    @Test
    public void test112AssignRolePirateWithExceptionToJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_PIRATE_OID,
                assignment -> {
                    PolicyExceptionType policyException = new PolicyExceptionType();
                    policyException.setRuleName(ROLE_JUDGE_POLICY_RULE_EXCLUSION_NAME);
                    assignment.getPolicyException().add(policyException);
                });

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertAssignAccountToJack(context);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        assertEvaluatedTargetPolicyRules(context, 7);
        assertTargetTriggers(context,  null, 0);

        assertSerializable(context);
    }

    @Test
    public void test120AssignRoleConstableToJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CONSTABLE_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("Output context", context);

        dumpPolicyRules(context);
        dumpPolicySituations(context);
        assertEvaluatedTargetPolicyRules(context, 8);

        ObjectDelta<UserType> focusSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        PrismAsserts.assertIsModify(focusSecondaryDelta);
        PrismAsserts.assertModifications(focusSecondaryDelta, 2);
        ContainerDelta<AssignmentType> assignmentDelta = focusSecondaryDelta.findContainerDelta(FocusType.F_ASSIGNMENT);
        assertEquals("Unexpected assignment secondary delta", 1, assignmentDelta.getValuesToDelete().size());
        PrismContainerValue<AssignmentType> deletedAssignment = assignmentDelta.getValuesToDelete().iterator().next();
        assertEquals("Wrong OID in deleted assignment", ROLE_JUDGE_OID, deletedAssignment.asContainerable().getTargetRef().getOid());

        ObjectDelta<ShadowType> accountSecondaryDelta = assertAssignAccountToJack(context);
        PrismAsserts.assertPropertyAdd(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_PATH, "Constable");
        PrismAsserts.assertPropertyDelete(
                accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_TITLE_PATH, "Honorable Justice");

        assertSerializable(context);
    }

    /**
     * Thief has got separated constraints and actions.
     */
    @Test
    public void test150AssignRoleThiefToJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_THIEF_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        DeltaSetTriple<EvaluatedAssignmentImpl<?>> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        displayDumpable("Output evaluatedAssignmentTriple", evaluatedAssignmentTriple);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        assertEvaluatedTargetPolicyRules(context, 9);
        assertTargetTriggers(context, PolicyConstraintKindType.EXCLUSION, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.SITUATION, 1);

        EvaluatedExclusionTrigger triggerExclusion = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, null, PolicyConstraintKindType.EXCLUSION, 1, false);
        assertNotNull("No conflicting assignment in trigger", triggerExclusion.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_JUDGE_OID, triggerExclusion.getConflictingAssignment().getTarget().getOid());

        EvaluatedSituationTrigger triggerSituation = (EvaluatedSituationTrigger) assertTriggeredTargetPolicyRule(context, null, PolicyConstraintKindType.SITUATION, 1, false);
        assertEquals("Wrong # of source rules", 1, triggerSituation.getSourceRules().size());
        EvaluatedPolicyRule sourceRule = triggerSituation.getSourceRules().iterator().next();
        EvaluatedExclusionTrigger sourceTrigger = (EvaluatedExclusionTrigger) sourceRule.getTriggers().iterator().next();
        assertNotNull("No conflicting assignment in source trigger", sourceTrigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in source trigger", ROLE_JUDGE_OID, sourceTrigger.getConflictingAssignment().getTarget().getOid());

        assertSerializable(context);
    }

    /**
     * Mostly preparation for other tests. But good check for no exclusion conflict.
     */
    @Test
    public void test200JackAssignRoleContractor() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        // WHEN
        when();
        assignRole(USER_JACK_OID, ROLE_CORP_CONTRACTOR_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_CORP_CONTRACTOR_OID);
    }

    /**
     *  Employee conflicts with Contractor.
     */
    @Test
    public void test210AssignRoleEmployeeToJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CORP_EMPLOYEE_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        // Judge: criminal-exclusion, unassignment, all-assignment-operations, all-assignment-operations-on-jack, all-assignment-operations-on-elaine, all-assignment-operations-on-jack-via-script, global-assignment-notification-for-judge
        // Employee: approve-any-corp-role, notify-exclusion-violations, employee-excludes-contractor
        // Contractor: approve-any-corp-role, notify-exclusion-violations, contractor-excludes-employee
        assertEvaluatedTargetPolicyRules(context, 13);

        EvaluatedExclusionTrigger trigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, ROLE_CORP_EMPLOYEE_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
        assertNotNull("No conflicting assignment in trigger", trigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_CORP_CONTRACTOR_OID, trigger.getConflictingAssignment().getTarget().getOid());

        assertSerializable(context);
    }

    /**
     *  Engineer->Employee conflicts with Contractor.
     */
    @Test
    public void test220AssignRoleEngineerToJack() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContextAssignRole(context, USER_JACK_OID, ROLE_CORP_ENGINEER_OID);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);
        rememberCounter(InternalCounters.SHADOW_FETCH_OPERATION_COUNT);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);
        dumpPolicySituations(context);

        // Judge: L:criminal-exclusion, L:unassignment, L:all-assignment-operations
        // Contractor: L:approve-any-corp-role, L:notify-exclusion-violations, L:contractor-excludes-employee
        // Engineer: approve-any-corp-role, notify-exclusion-violations, employee-excludes-contractor, L:approve-any-corp-role, L:notify-exclusion-violations
        assertEvaluatedTargetPolicyRules(context, 15);
        EvaluatedExclusionTrigger engineerTrigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
        assertNotNull("No conflicting assignment in trigger", engineerTrigger.getConflictingAssignment());
        assertEquals("Wrong conflicting assignment in trigger", ROLE_CORP_CONTRACTOR_OID, engineerTrigger.getConflictingAssignment().getTarget().getOid());
        assertTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1, false);
        assertTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.SITUATION, 1, false);

        EvaluatedPolicyRule engineerRule = getTriggeredTargetPolicyRule(context, ROLE_CORP_ENGINEER_OID, PolicyConstraintKindType.EXCLUSION);
        displayDumpable("exclusion rule for Engineer", engineerRule);
        displayDumpable("exclusion trigger for Engineer", engineerTrigger);
        displayDumpable("Engineer: assignmentPath", engineerRule.getAssignmentPath());
        displayDumpable("Engineer: conflictingPath", engineerTrigger.getConflictingPath());
        assertAssignmentPath(engineerRule.getAssignmentPath(), ROLE_CORP_ENGINEER_OID, ROLE_CORP_EMPLOYEE_OID, null);
        assertAssignmentPath(engineerTrigger.getConflictingPath(), ROLE_CORP_CONTRACTOR_OID);

        EvaluatedPolicyRule contractorRule = getTriggeredTargetPolicyRule(context, ROLE_CORP_CONTRACTOR_OID, PolicyConstraintKindType.EXCLUSION);
        EvaluatedExclusionTrigger contractorTrigger = (EvaluatedExclusionTrigger) assertTriggeredTargetPolicyRule(context, ROLE_CORP_CONTRACTOR_OID, PolicyConstraintKindType.EXCLUSION, 1, false);
        displayDumpable("exclusion rule for Contractor", contractorRule);
        displayDumpable("exclusion trigger for Contractor", contractorTrigger);
        displayDumpable("Contractor: assignmentPath", contractorRule.getAssignmentPath());
        displayDumpable("Contractor: conflictingPath", contractorTrigger.getConflictingPath());
        assertAssignmentPath(contractorRule.getAssignmentPath(), ROLE_CORP_CONTRACTOR_OID, null);
        assertAssignmentPath(contractorTrigger.getConflictingPath(), ROLE_CORP_ENGINEER_OID, ROLE_CORP_EMPLOYEE_OID);

        assertSerializable(context);
    }

    /**
     * MID-4132
     *
     * Drake changes employeeType null to T. There's a global notification policy rule applicable to users with employeeType != T.
     * Should we get the notification?
     *
     * Yes and no. The condition is checked on objectCurrent. So, in primary state the rule is applied (employeeType is null
     * at that moment). But in final state (when notification actions are evaluated) the condition should be already false.
     * So we should not get the notification.
     */
    @Test
    public void test300DrakeChangeEmployeeType() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        // WHEN
        when();
        ObjectDelta<? extends ObjectType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(ObjectTypeUtil.createAssignmentTo(ROLE_JUDGE_OID, ObjectTypes.ROLE, prismContext))
                .item(UserType.F_EMPLOYEE_NUMBER).replace("T")
                .asObjectDelta(USER_DRAKE_OID);

        RecordingProgressListener recordingListener = new RecordingProgressListener();
        modelService.executeChanges(List.of(delta), null, task, List.of(recordingListener), result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_DRAKE_OID);
        display("User after", userAfter);
        assertAssignedRole(userAfter, ROLE_JUDGE_OID);

        LensFocusContext<?> focusContext = ((LensContext<?>) recordingListener.getModelContext()).getFocusContext();
        displayDumpable("focusContext", focusContext);
        assertEquals("Wrong # of focus policy rules", 0, focusContext.getObjectPolicyRules().size());
    }

    /**
     * Tests policy rule messages for objects with localized names.
     * See also MID-5916.
     */
    @Test
    public void test400AssignRoleLocalized() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        LensContext<UserType> context = createUserLensContext();
        fillContextWithUser(context, USER_LOCALIZED.oid, result);
        addModificationToContextAssignRole(context, USER_LOCALIZED.oid, ROLE_LOCALIZED.oid);

        displayDumpable("Input context", context);

        assertFocusModificationSanity(context);

        // WHEN
        when();
        projector.project(context, "test", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        dumpPolicyRules(context);

        Locale SLOVAK = Locale.forLanguageTag("sk-SK");
        assertNotNull("No Slovak locale", SLOVAK);

        // sanity check
        PrismObject<RoleType> roleLocalized = getRole(ROLE_LOCALIZED.oid);
        String roleNameUs = localizationService.translate(roleLocalized.getName(), Locale.US, false);
        String roleNameSk = localizationService.translate(roleLocalized.getName(), SLOVAK, false);
        System.out.println("Role name translated (US): " + roleNameUs);
        System.out.println("Role name translated (SK): " + roleNameSk);
        assertEquals("Wrong US role name", roleNameUs, "Localized role");
        assertEquals("Wrong SK role name", roleNameSk, "Lokalizovana rola");

        // checking the rules
        assertEvaluatedTargetPolicyRules(context, 1);
        assertTargetTriggers(context, PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1);
        EvaluatedPolicyRuleTrigger<?> trigger = assertTriggeredTargetPolicyRule(context, ROLE_LOCALIZED.oid,
                PolicyConstraintKindType.ASSIGNMENT_MODIFICATION, 1, true);
        LocalizableMessage message = trigger.getMessage();

        System.out.println("Trigger message: " + message);
        String messageUs = localizationService.translate(message, Locale.US);
        String messageSk = localizationService.translate(message, SLOVAK);
        System.out.println("Trigger message translated (US): " + messageUs);
        System.out.println("Trigger message translated (SK): " + messageSk);
        assertEquals("Wrong US message", "Assignment of role \"Localized role\" (relation member) is to be added", messageUs);
        assertEquals("Wrong SK message", "Priradenie pre rola \"Lokalizovana rola\" (vztah predvolené) ma byt pridane", messageSk);

        assertSerializable(context);
    }

    private ObjectDelta<ShadowType> assertAssignAccountToJack(LensContext<UserType> context) {
        displayDumpable("Output context", context);

        assertSame(context.getFocusContext().getPrimaryDelta().getChangeType(), ChangeType.MODIFY);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());
        Collection<LensProjectionContext> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();

        assertEquals("Wrong decision", SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());

        return accountSecondaryDelta;

    }
}
