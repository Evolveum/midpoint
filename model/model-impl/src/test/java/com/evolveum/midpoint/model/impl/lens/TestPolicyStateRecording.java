/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createAssignmentTo;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests recording of policy situations into objects and assignments.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyStateRecording extends AbstractLensTest {

    protected static final File TEST_DIR = new File(AbstractLensTest.TEST_DIR, "policy/state");

    private static final File USER_BOB_FILE = new File(TEST_DIR, "user-bob.xml");
    private static final String WRONG_URI = "http://test.org/wrong";
    private static String userBobOid;
    private static final File USER_EVE_FILE = new File(TEST_DIR, "user-eve.xml");
    private static String userEveOid;
    private static final File ROLE_A_TEST_2A_FILE = new File(TEST_DIR, "a-test-2a.xml");
    private static String roleATest2aOid;
    private static final File ROLE_A_TEST_2B_FILE = new File(TEST_DIR, "a-test-2b.xml");
    private static String roleATest2bOid;
    private static final File ROLE_A_TEST_2C_FILE = new File(TEST_DIR, "a-test-2c.xml");
    private static final File ROLE_A_TEST_3A_FILE = new File(TEST_DIR, "a-test-3a.xml");
    private static String roleATest3aOid;
    private static final File ROLE_A_TEST_3B_FILE = new File(TEST_DIR, "a-test-3b.xml");
    private static String roleATest3bOid;
    private static final File ROLE_A_TEST_3X_FILE = new File(TEST_DIR, "a-test-3x.xml");
    private static final File ROLE_A_TEST_3Y_FILE = new File(TEST_DIR, "a-test-3y.xml");
    private static final File ROLE_A_TEST_WRONG_FILE = new File(TEST_DIR, "a-test-wrong.xml");
    private static String roleATestWrongOid;
    private static final File METAROLE_COMMON_RULES_FILE = new File(TEST_DIR, "metarole-common-rules.xml");
    private static String metaroleCommonRulesOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        setDefaultUserTemplate(USER_TEMPLATE_OID);

        addObject(ROLE_PIRATE_RECORD_ONLY_FILE);
        addObject(ROLE_JUDGE_RECORD_ONLY_FILE);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        metaroleCommonRulesOid = addAndRecompute(METAROLE_COMMON_RULES_FILE, initTask, initResult);
        roleATest2aOid = addAndRecompute(ROLE_A_TEST_2A_FILE, initTask, initResult);
        roleATest2bOid = addAndRecompute(ROLE_A_TEST_2B_FILE, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_2C_FILE, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3X_FILE, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3Y_FILE, initTask, initResult);
        roleATest3aOid = addAndRecompute(ROLE_A_TEST_3A_FILE, initTask, initResult);
        roleATest3bOid = addAndRecompute(ROLE_A_TEST_3B_FILE, initTask, initResult);
        roleATestWrongOid = addAndRecompute(ROLE_A_TEST_WRONG_FILE, initTask, initResult);

        userBobOid = addAndRecompute(USER_BOB_FILE, initTask, initResult);
        userEveOid = addAndRecompute(USER_EVE_FILE, initTask, initResult);

        InternalMonitor.reset();

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Test
    public void test100JackAssignRoleJudge() throws Exception {
        given();
        Task task = createPlainTask();

        when();
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, task.getResult());

        then();
        UserType jack = getUser(USER_JACK_OID).asObjectable();
        display("jack", jack);
        assertSuccess(task.getResult());

        assertAssignedRole(jack.asPrismObject(), ROLE_JUDGE_OID);
        assertEquals("Wrong # of assignments", 1, jack.getAssignment().size());
        assertEquals("Wrong policy situations",
                Collections.emptyList(),
                jack.getAssignment().get(0).getPolicySituation());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1 + accessesMetadataAuditOverhead(1));
    }

    @Test
    public void test110JackAssignRolePirate() throws Exception {
        when();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, getTestTask(), getTestOperationResult());

        then();
        UserType jack = getUser(USER_JACK_OID).asObjectable();
        display("jack", jack);
        assertSuccess(getTestOperationResult());

        assertAssignedRole(jack.asPrismObject(), ROLE_PIRATE_OID);
        assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
        for (AssignmentType assignment : jack.getAssignment()) {
            assertExclusionViolationState(assignment, 2);
        }

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(isNativeRepository() ? 1 : 2); // rules without IDs, with IDs
    }

    // should keep the situation for both assignments
    @Test
    public void test120RecomputeJack() throws Exception {
        given();
        dummyAuditService.clear();

        when();
        executeChanges(prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, USER_JACK_OID),
                executeOptions().reconcile(), getTestTask(), getTestOperationResult());

        then();
        UserType jack = getUser(USER_JACK_OID).asObjectable();
        display("jack", jack);
        assertSuccess(getTestOperationResult());

        // TODO test that assignment IDs are filled in correctly (currently they are not)
        assertEquals("Wrong # of assignments", 2, jack.getAssignment().size());
        for (AssignmentType assignment : jack.getAssignment()) {
            assertEquals("Wrong policy situations",
                    Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
                    assignment.getPolicySituation());
        }

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1);
        dummyAuditService.assertExecutionDeltas(0);
    }

    @Test
    public void test130JackUnassignRolePirate() throws Exception {
        given();
        UserType jack = getUser(USER_JACK_OID).asObjectable();
        AssignmentType pirateAssignment = findAssignmentByTargetRequired(jack.asPrismObject(), ROLE_PIRATE_OID);

        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(pirateAssignment.clone())
                .asObjectDelta(USER_JACK_OID);
        executeChangesAssertSuccess(delta, null, getTestTask(), getTestOperationResult());

        then();
        jack = getUser(USER_JACK_OID).asObjectable();
        display("jack", jack);
        assertSuccess(getTestOperationResult());

        assertNotAssignedRole(jack.asPrismObject(), ROLE_PIRATE_OID);
        assertEquals("Wrong # of assignments", 1, jack.getAssignment().size());
        assertEquals("Wrong policy situations",
                Collections.emptyList(),
                jack.getAssignment().get(0).getPolicySituation());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1);            // executed in one shot
    }

    @Test
    public void test200BobAssign2a3a() throws Exception {
        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE),
                        createAssignmentTo(roleATest3aOid, ObjectTypes.ROLE))
                .asObjectDelta(userBobOid);
        executeChangesAssertSuccess(delta, null, getTestTask(), getTestOperationResult());

        then();
        UserType bob = getUser(userBobOid).asObjectable();
        display("bob", bob);
        assertSuccess(getTestOperationResult());

        assertAssignedRole(bob.asPrismObject(), roleATest2aOid);
        assertAssignedRole(bob.asPrismObject(), roleATest3aOid);
        assertEquals("Wrong # of assignments", 2, bob.getAssignment().size());
        assertEquals("Wrong policy situations for assignment 1",
                Collections.emptyList(),
                bob.getAssignment().get(0).getPolicySituation());
        assertEquals("Wrong policy situations for assignment 2",
                Collections.emptyList(),
                bob.getAssignment().get(1).getPolicySituation());

        displayDumpable("Audit", dummyAuditService);
        // no policy state update
        dummyAuditService.assertExecutionRecords(1 + accessesMetadataAuditOverhead(1));
    }

    @Test
    public void test210BobAssign2b3b() throws Exception {
        when();
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE),
                        createAssignmentTo(roleATest3bOid, ObjectTypes.ROLE))
                .asObjectDelta(userBobOid);
        executeChangesAssertSuccess(delta, null, getTestTask(), getTestOperationResult());

        then();
        UserType bob = getUser(userBobOid).asObjectable();
        display("bob", bob);
        assertSuccess(getTestOperationResult());

        assertAssignedRole(bob.asPrismObject(), roleATest2aOid);
        assertAssignedRole(bob.asPrismObject(), roleATest2bOid);
        assertAssignedRole(bob.asPrismObject(), roleATest3aOid);
        assertAssignedRole(bob.asPrismObject(), roleATest3bOid);
        assertEquals("Wrong # of assignments", 4, bob.getAssignment().size());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(isNativeRepository() ? 1 : 2); // rules without IDs, with IDs

        for (AssignmentType assignment : bob.getAssignment()) {
            assertExclusionViolationState(assignment, 1);
        }
    }

    private void assertExclusionViolationState(AssignmentType assignment, int expectedRules) {
        assertEquals("Wrong policy situations",
                Collections.singletonList(SchemaConstants.MODEL_POLICY_SITUATION_EXCLUSION_VIOLATION),
                assignment.getPolicySituation());
        List<EvaluatedPolicyRuleType> triggeredRules = assignment.getTriggeredPolicyRule();
        assertThat(triggeredRules)
                .as("triggered policy rules in assignment " + assignment)
                .hasSize(expectedRules);
        for (EvaluatedPolicyRuleType triggeredRule : triggeredRules) {
            List<EvaluatedPolicyRuleTriggerType> triggers = triggeredRule.getTrigger();
            assertThat(triggers)
                    .as("Triggers in triggered policy rule in assignment " + assignment)
                    .hasSize(1);
            assertThat(triggers.get(0).getConstraintKind())
                    .as("type of trigger in " + assignment)
                    .isEqualTo(PolicyConstraintKindType.EXCLUSION);
        }
    }

    // new user, new assignments (no IDs)
    @Test
    public void test220AliceAssign2a2b() throws Exception {
        given();
        UserType alice = prismContext.createObjectable(UserType.class)
                .name("alice")
                .assignment(createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE))
                .assignment(createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE));

        when();
        addObject(alice, getTestTask(), getTestOperationResult());

        then();
        alice = getUser(alice.getOid()).asObjectable();
        display("alice", alice);
        assertSuccess(getTestOperationResult());

        assertAssignedRole(alice.asPrismObject(), roleATest2aOid);
        assertAssignedRole(alice.asPrismObject(), roleATest2bOid);
        assertEquals("Wrong # of assignments", 2, alice.getAssignment().size());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1 + accessesMetadataAuditOverhead(1));

        for (AssignmentType assignment : alice.getAssignment()) {
            assertExclusionViolationState(assignment, 1);
        }
    }

    // new user, new assignments (explicit IDs)
    @Test
    public void test230ChuckAssign2a2b() throws Exception {
        given();
        AssignmentType assignment2a = createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE);
        assignment2a.setId(100L);
        AssignmentType assignment2b = createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE);
        assignment2b.setId(101L);
        UserType chuck = prismContext.createObjectable(UserType.class)
                .name("chuck")
                .assignment(assignment2a)
                .assignment(assignment2b);

        when();
        addObject(chuck, getTestTask(), getTestOperationResult());

        then();
        chuck = getUser(chuck.getOid()).asObjectable();
        display("chuck", chuck);
        assertSuccess(getTestOperationResult());

        assertAssignedRole(chuck.asPrismObject(), roleATest2aOid);
        assertAssignedRole(chuck.asPrismObject(), roleATest2bOid);
        assertEquals("Wrong # of assignments", 2, chuck.getAssignment().size());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1);

        for (AssignmentType assignment : chuck.getAssignment()) {
            assertExclusionViolationState(assignment, 1);
        }
    }

    // new user, new assignments (explicit IDs, explicit OID)
    @Test
    public void test240DanAssign2a2b() throws Exception {
        given();
        AssignmentType assignment2a = createAssignmentTo(roleATest2aOid, ObjectTypes.ROLE);
        assignment2a.setId(100L);
        AssignmentType assignment2b = createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE);
        assignment2b.setId(101L);
        UserType dan = prismContext.createObjectable(UserType.class)
                .oid("207752fa-9559-496c-b04d-42b5e9af2779")
                .name("dan")
                .assignment(assignment2a)
                .assignment(assignment2b);

        when();
        addObject(dan, getTestTask(), getTestOperationResult());

        then();
        dan = getUser(dan.getOid()).asObjectable();
        display("dan", dan);
        assertSuccess(getTestOperationResult());

        assertAssignedRole(dan.asPrismObject(), roleATest2aOid);
        assertAssignedRole(dan.asPrismObject(), roleATest2bOid);
        assertEquals("Wrong # of assignments", 2, dan.getAssignment().size());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1);

        for (AssignmentType assignment : dan.getAssignment()) {
            assertExclusionViolationState(assignment, 1);
        }
    }

    // modified user, new assignment (with ID)
    @Test
    public void test250EveAssign2b() throws Exception {
        when();
        AssignmentType assignment2b = createAssignmentTo(roleATest2bOid, ObjectTypes.ROLE);
        assignment2b.setId(200L);
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(assignment2b)
                .asObjectDelta(userEveOid);
        executeChangesAssertSuccess(delta, null, getTestTask(), getTestOperationResult());

        then();
        UserType eve = getUser(userEveOid).asObjectable();
        display("eve after", eve);
        assertSuccess(getTestOperationResult());

        assertAssignedRole(eve.asPrismObject(), roleATest2aOid);
        assertAssignedRole(eve.asPrismObject(), roleATest2bOid);
        assertEquals("Wrong # of assignments", 2, eve.getAssignment().size());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1);

        for (AssignmentType assignment : eve.getAssignment()) {
            assertExclusionViolationState(assignment, 1);
        }
    }

    @Test
    public void test300MakeRoleWrong() throws Exception {
        when();
        ObjectDelta<RoleType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_DESCRIPTION).replace("wrong")
                .asObjectDelta(roleATestWrongOid);
        executeChangesAssertSuccess(delta, null, getTestTask(), getTestOperationResult());

        then();
        RoleType wrong = getRole(roleATestWrongOid).asObjectable();
        display("role 'wrong'", wrong);
        assertSuccess(getTestOperationResult());

        assertEquals("Wrong policy situations for role", Collections.singletonList(WRONG_URI), wrong.getPolicySituation());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1);            // no extra policy state update
    }

    @Test
    public void test310CreateWrongRole() throws Exception {
        given();
        RoleType wrong2 = prismContext.createObjectable(RoleType.class)
                .name("wrong-2")
                .description("wrong")
                .assignment(createAssignmentTo(metaroleCommonRulesOid, ObjectTypes.ROLE));

        when();
        addObject(wrong2, getTestTask(), getTestOperationResult());

        then();
        wrong2 = getRole(wrong2.getOid()).asObjectable();
        display("role 'wrong-2'", wrong2);
        assertSuccess(getTestOperationResult());

        assertEquals("Wrong policy situations for role", Collections.singletonList(WRONG_URI), wrong2.getPolicySituation());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1 + accessesMetadataAuditOverhead(1));
    }

    @Test
    public void test320CreateWrongRoleKnownOid() throws Exception {
        given();
        AssignmentType assignmentCommon = createAssignmentTo(metaroleCommonRulesOid, ObjectTypes.ROLE);
        assignmentCommon.setId(300L);
        RoleType wrong3 = prismContext.createObjectable(RoleType.class)
                .name("wrong-3")
                .oid("df6c6bdc-f938-4afc-98f3-10d18ceda274")
                .description("wrong")
                .assignment(assignmentCommon);

        when();
        addObject(wrong3, getTestTask(), getTestOperationResult());

        then();
        wrong3 = getRole(wrong3.getOid()).asObjectable();
        display("role 'wrong-3'", wrong3);
        assertSuccess(getTestOperationResult());

        assertEquals("Wrong policy situations for role", Collections.singletonList(WRONG_URI), wrong3.getPolicySituation());

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertExecutionRecords(1);            // no extra policy state update
    }
}
