/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.rbac;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPolicyRules extends AbstractInitializedModelIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/rbac/policy-rules");

    static final TestObject<PolicyType> POLICY_SKIPPER_LICENSE = TestObject.file(
            TEST_DIR, "policy-skipper-license.xml", "25f26c46-427f-11ef-9666-f352a031a80e");

    static final TestObject<PolicyType> ROLE_SKIPPER = TestObject.file(
            TEST_DIR, "role-skipper.xml", "bbfe9846-427e-11ef-a31c-53388393ba50");

    // Business roles that includes Skipper
    static final TestObject<PolicyType> ROLE_NAVY_CAPTAIN = TestObject.file(
            TEST_DIR, "role-navy-captain.xml", "b577476c-438b-11ef-b695-030d5a076b98");

    // Role that includes Skipper license
    static final TestObject<PolicyType> ROLE_NAVAL_ACADEMY_GRADUATE = TestObject.file(
            TEST_DIR, "role-naval-academy-graduate.xml", "31cfa124-438c-11ef-865e-0bfb88c1246d");

    // Information security role, it has information security responsibility classification
    static final TestObject<PolicyType> ROLE_BRIG_GUARD = TestObject.file(
            TEST_DIR, "role-brig-guard.xml", "3d6b0864-499c-11ef-bce7-d376c0ac2e64");

    // minAssignees and maxAssignees policy rules
    static final TestObject<PolicyType> ROLE_FRIENDLY_INTROVERT = TestObject.file(
            TEST_DIR, "role-friendly-introvert.xml", "111eddca-49ac-11ef-8749-cf1eec22a477");

    static final TestObject<PolicyType> POLICY_INFORMATION_SECURITY_RESPONSIBILITY = TestObject.file(
            TEST_DIR, "333-classification-information-security-responsibility.xml", "00000000-0000-0000-0000-000000000333");


    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        if (isNativeRepository()) {
            initTestObjects(
                    initTask, initResult,
                    CommonInitialObjects.ARCHETYPE_CLASSIFICATION,
                    CommonInitialObjects.ARCHETYPE_BUSINESS_ROLE,
//                    CommonInitialObjects.POLICY_INFORMATION_SECURITY_RESPONSIBILITY,
                    POLICY_INFORMATION_SECURITY_RESPONSIBILITY,
                    POLICY_SKIPPER_LICENSE,
                    ROLE_SKIPPER,
                    ROLE_NAVY_CAPTAIN,
                    ROLE_NAVAL_ACADEMY_GRADUATE,
                    ROLE_BRIG_GUARD,
                    ROLE_FRIENDLY_INTROVERT);
        }
    }

    @Test
    public void test110DirectRequirementSkipperFail() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        try {
            when();
            // Jack jas no skipper license, it should fail
            assignRole(USER_JACK_OID, ROLE_SKIPPER.oid, task, result);

            fail("Expected policy violation after adding skipper role, but it went well");
        } catch (PolicyViolationException e) {
            then();
            System.out.println("Got expected exception: " + e + ": " + e.getMessage());
            assertMessage(e, "Policy requirement not met: role \"Skipper\" requires policy \"Skipper license\"");
            result.computeStatus();
            assertFailure(result);
        }

        display("User after", getUser(USER_JACK_OID));

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    @Test
    public void test120DirectRequirementSkipperSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE.oid, task, result);

        when();
        // Jack has skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_SKIPPER.oid, task, result);

        then();
        unassignRole(USER_JACK_OID, ROLE_SKIPPER.oid, task, result);

        unassignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE.oid, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  indirect, role skipper in business role navy captain
    @Test
    public void test130IndirectRequirementNavyCaptainFail() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        try {
            when();
            // Jack jas no skipper license, it should fail
            assignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN.oid, task, result);

            fail("Expected policy violation after adding navy captain role, but it went well");
        } catch (PolicyViolationException e) {
            then();
            System.out.println("Got expected exception: " + e + ": " + e.getMessage());
            assertMessage(e, "Policy requirement not met: role \"Skipper\" (Navy captain -> Skipper) requires policy \"Skipper license\"");
            result.computeStatus();
            assertFailure(result);
        }

        display("User after", getUser(USER_JACK_OID));

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  indirect, role skipper in business role navy captain
    @Test
    public void test140IndirectRequirementNavyCaptainSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        assignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE.oid, task, result);

        when();
        // Jack has skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN.oid, task, result);

        then();
        unassignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN.oid, task, result);

        unassignPolicy(USER_JACK_OID, POLICY_SKIPPER_LICENSE.oid, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  indirect, skipper license in graduate role
    @Test
    public void test150IndirectRequirementGraduateSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        assignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE.oid, task, result);

        when();
        // Jack has (indirect) skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_SKIPPER.oid, task, result);

        then();
        unassignRole(USER_JACK_OID, ROLE_SKIPPER.oid, task, result);

        unassignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE.oid, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }

    //  double indirect, role skipper in business role navy captain, skipper license in graduate role
    @Test
    public void test160IndirectRequirementNavyCaptainGraduateSuccess() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);

        assignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE.oid, task, result);

        when();
        // Jack has (indirect) skipper license now, it should go well
        assignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN.oid, task, result);

        then();
        unassignRole(USER_JACK_OID, ROLE_NAVY_CAPTAIN.oid, task, result);

        unassignRole(USER_JACK_OID, ROLE_NAVAL_ACADEMY_GRADUATE.oid, task, result);

        assertAssignedNoRole(USER_JACK_OID, result);
        assertAssignedNoPolicy(USER_JACK_OID, result);
    }


    // TODO: requirement: assign both license and skipper in one operation

    // TODO: requirement: unassign both license and skipper in one operation

    // TODO: requirement: business roles that contains both license and role skipper


    /**
     * Role "friendly introvert" has minAssignees and maxAssignees policy rules, 1 > members >= 2.
     * Therefore, as it is not assigned, it should be in state of violation.
     */
    @Test(enabled = false) // #9869
    public void test200FriendlyIntrovertRecompute() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modelService.recompute(RoleType.class, ROLE_FRIENDLY_INTROVERT.oid, null, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_FRIENDLY_INTROVERT.oid, "Role after")
                .display()
                .assertEffectiveMarks(SystemObjectsType.MARK_UNDERASSIGNED.value())
                .assertPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED); // legacy
    }

    /**
     * Assign "friendly introvert", this should make the rule happy.
     */
    @Test(enabled = false) // #9869
    public void test202FriendlyIntrovertAssignOnce() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(USER_JACK_OID, ROLE_FRIENDLY_INTROVERT.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_FRIENDLY_INTROVERT.oid, "Role after")
                .display()
                .assertNoEffectiveMarks()
                .assertNoPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED);
        assertUserAfter(USER_JACK_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
    }

    /**
     * Assign "friendly introvert" again. We are still happy.
     */
    @Test(enabled = false) // #9869
    public void test204FriendlyIntrovertAssignAgain() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(USER_GUYBRUSH_OID, ROLE_FRIENDLY_INTROVERT.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_FRIENDLY_INTROVERT.oid, "Role after")
                .assertNoEffectiveMarks()
                .assertNoPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED);
        assertUserAfter(USER_JACK_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_GUYBRUSH_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
    }


    /**
     * Assign "friendly introvert" two more times, this should trigger maxAssignees rule.
     */
    @Test(enabled = false) // #9869
    public void test206FriendlyIntrovertAssignTooMuch() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(USER_BARBOSSA_OID, ROLE_FRIENDLY_INTROVERT.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_FRIENDLY_INTROVERT.oid, "Role after")
                .display()
                .assertEffectiveMarks(SystemObjectsType.MARK_OVERASSIGNED.value())
                .assertPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_OVERASSIGNED); // legacy
        assertUserAfter(USER_JACK_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_GUYBRUSH_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_BARBOSSA_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
    }

    /**
     * Unassign one user. This should make the rules happy again.
     */
    @Test(enabled = false) // #9869
    public void test207UnassignOnce() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_JACK_OID, ROLE_FRIENDLY_INTROVERT.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_FRIENDLY_INTROVERT.oid, "Role after")
                .display()
                .assertNoEffectiveMarks()
                .assertNoPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED);
        assertUserAfter(USER_JACK_OID).assignments().assertNoRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_GUYBRUSH_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_BARBOSSA_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
    }

    /**
     * Unassign one more user. Still happy.
     */
    @Test(enabled = false) // #9869
    public void test208UnassignAgain() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_BARBOSSA_OID, ROLE_FRIENDLY_INTROVERT.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_FRIENDLY_INTROVERT.oid, "Role after")
                .display()
                .assertNoEffectiveMarks()
                .assertNoPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED);
        assertUserAfter(USER_JACK_OID).assignments().assertNoRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_GUYBRUSH_OID).assignments().assertRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_BARBOSSA_OID).assignments().assertNoRole(ROLE_FRIENDLY_INTROVERT.oid);
    }

    /**
     * Unassign last user, minAssignees should trigger again.
     */
    @Test(enabled = false) // #9869
    public void test209UnassignLastUser() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_GUYBRUSH_OID, ROLE_FRIENDLY_INTROVERT.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_FRIENDLY_INTROVERT.oid, "Role after")
                .display()
                .assertEffectiveMarks(SystemObjectsType.MARK_UNDERASSIGNED.value())
                .assertPolicySituation(SchemaConstants.MODEL_POLICY_SITUATION_UNDERASSIGNED); // legacy
        assertUserAfter(USER_JACK_OID).assignments().assertNoRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_GUYBRUSH_OID).assignments().assertNoRole(ROLE_FRIENDLY_INTROVERT.oid);
        assertUserAfter(USER_BARBOSSA_OID).assignments().assertNoRole(ROLE_FRIENDLY_INTROVERT.oid);
    }

    /**
     * Brig guard is a security role, it has information security responsibility classification.
     * The classification has inducements with policy rules, including minAssignees rule.
     * As this role is not assigned to anyone, minAssignees policy rule in the classification should indicate underassignment.
     */
    @Test(enabled = false) // #9869
    public void test210BrigGuardRecompute() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        modelService.recompute(RoleType.class, ROLE_BRIG_GUARD.oid, null, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_BRIG_GUARD.oid, "Role after").assertEffectiveMarks(
//                SystemObjectsType.MARK_UNDERASSIGNED.value(), SystemObjectsType.MARK_UNDERSTAFFED_SECURITY.value()
                SystemObjectsType.MARK_UNDERASSIGNED.value()
        );
    }

    /**
     * We assign Brig guard role to user.
     * This should satisfy minAssignees policy rule in the classification.
     * The marks should be gone.
     */
    @Test(enabled = false) // #9869
    public void test212BrigGuardAssign() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        assignRole(USER_JACK_OID, ROLE_BRIG_GUARD.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_BRIG_GUARD.oid, "Role after").assertNoEffectiveMarks();
        assertUserAfter(USER_JACK_OID).assignments().assertRole(ROLE_BRIG_GUARD.oid);
    }

    /**
     * Unassign Brig guard.
     * The minAssignees policy rule in the classification should trigger again.
     */
    @Test(enabled = false) // #9869
    public void test214BrigGuardUnassign() throws Exception {
        skipIfNotNativeRepository();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        unassignRole(USER_JACK_OID, ROLE_BRIG_GUARD.oid, task, result);

        then();
        assertSuccess(result);

        assertRole(ROLE_BRIG_GUARD.oid, "Role after").assertEffectiveMarks(
//                SystemObjectsType.MARK_UNDERASSIGNED.value(), SystemObjectsType.MARK_UNDERSTAFFED_SECURITY.value()
                SystemObjectsType.MARK_UNDERASSIGNED.value()
        );
        assertUserAfter(USER_JACK_OID).assignments().assertNoRole(ROLE_BRIG_GUARD.oid);
    }

}
