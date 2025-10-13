/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import java.io.File;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.rbac.TestSegregationOfDuties;

import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.lens.assignments.PathSegmentEvaluation;
import com.evolveum.midpoint.model.impl.lens.assignments.TargetEvaluation;
import com.evolveum.midpoint.model.impl.lens.projector.focus.AssignmentProcessor;
import com.evolveum.midpoint.model.intest.rbac.TestRbac;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

/**
 * Tests various aspects of assignments processing.
 *
 * Somewhat similar to {@link TestRbac} but this class is simpler and standalone (e.g. no subclasses).
 *
 * Note that assignments processing is one of the basic parts of the Projector. So it's in fact tested in many
 * test classes. However, here we specifically test the assignment processing itself.
 *
 * The class is named after assignment _processing_, because we plan that it will cover not only the functionality of
 * {@link AssignmentProcessor} but the related functionality as well.
 *
 * Content:
 *
 * - tests 100-130 check working with aggressively idempotent roles (MID-7382)
 * - test 200 deals with "projection legalization" feature (MID-8562)
 *
 * NOTE: In `model-impl`, see also `TestPolicyRules`, `TestPolicyRules2`, `TestAssignmentEvaluator`.
 *
 * @see TestRbac
 * @see TestSegregationOfDuties
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentsProcessing extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/assignments");

    private static final String ATTR_TYPE = "type";
    private static final String TYPE_EMPLOYEE = "employee";
    private static final String TYPE_CONTRACTOR = "contractor";
    private static final String TYPE_GUEST = "guest";

    private static final DummyTestResource RESOURCE_DUMMY_LEGALIZING =
            new DummyTestResource(TEST_DIR, "resource-dummy-legalizing.xml", "d3995d45-2d51-4f1a-b9c0-4bb74d4e8231",
                    "legalizing",
                    c -> c.addAttrDef(c.getAccountObjectClass(), ATTR_TYPE, String.class, false, false));

    private static final TestObject<RoleType> ROLE_IDEMPOTENT =
            TestObject.file(TEST_DIR, "role-idempotent.xml", "c7697df9-b873-4ae3-9854-7d01dd6f0c27");
    private static final TestObject<RoleType> ROLE_INDUCING_IDEMPOTENT =
            TestObject.file(TEST_DIR, "role-inducing-idempotent.xml", "7e2e721c-6f9e-4013-be4d-bbe32ea64238");

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_DUMMY_LEGALIZING.initAndTest(this, initTask, initResult);
        initTestObjects(initTask, initResult,
                ROLE_IDEMPOTENT,
                ROLE_INDUCING_IDEMPOTENT);
    }

    /**
     * A user has two assignments of an aggressively idempotent role.
     * After removing one of them we check that the role OID is still in roleMembershipRef values.
     *
     *   user --a1-> idempotent
     *        --a2-> idempotent
     *
     * MID-7382
     */
    @Test
    public void test100TwoAssignmentsOfIdempotentRoleOneRemoved() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // @formatter:off
        UserType user = new UserType()
                .name("test100")
                .beginAssignment()
                    .description("a1")
                    .targetRef(ROLE_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                .<UserType>end()
                .beginAssignment()
                    .description("a2")
                    .targetRef(ROLE_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                .end();
        // @formatter:on
        addObject(user.asPrismObject(), null, task, result);

        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(user.getAssignment().get(1).clone())
                        .asObjectDelta(user.getOid()),
                null, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(user.getOid())
                .roleMembershipRefs()
                    .assertRole(ROLE_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRoleMemberhipRefs(1);
        // @formatter:on
    }

    /**
     * A user has two assignments of a role inducing an aggressively idempotent role.
     * After removing one of them we check that the role OIDs are still in roleMembershipRef values.
     *
     *   user --a1-> inducing-idempotent --i-> idempotent
     *        --a2-> inducing-idempotent --i-> idempotent
     *
     * MID-7382
     */
    @Test
    public void test110TwoIndirectAssignmentsOfIdempotentRoleOneRemoved() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // @formatter:off
        UserType user = new UserType()
                .name("test110")
                .beginAssignment()
                    .description("a1")
                    .targetRef(ROLE_INDUCING_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                .<UserType>end()
                .beginAssignment()
                    .description("a2")
                    .targetRef(ROLE_INDUCING_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                .end();
        // @formatter:on
        addObject(user.asPrismObject(), null, task, result);

        // @formatter:off
        assertUserBefore(user.getOid())
                .roleMembershipRefs()
                    .assertRole(ROLE_INDUCING_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRole(ROLE_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRoleMemberhipRefs(2);
        // @formatter:on

        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .delete(user.getAssignment().get(1).clone())
                        .asObjectDelta(user.getOid()),
                null, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(user.getOid())
                .roleMembershipRefs()
                    .assertRole(ROLE_INDUCING_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRole(ROLE_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRoleMemberhipRefs(2);
        // @formatter:on
    }

    /**
     * Situation:
     *
     *   user --a-(enabled)-> idempotent
     *        --a-(disabled)-> idempotent
     *
     * Checking that roleMembershipRef is correct.
     *
     * Note that this works even without explicit fixing because non-active targets are not cached.
     * See {@link TargetEvaluation#evaluate()}.
     *
     * MID-7382
     */
    @Test
    public void test120TwoAssignmentsOfIdempotentRoleOneDisabled() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        // @formatter:off
        UserType user = new UserType()
                .name("test120")
                .beginAssignment()
                    .description("disabled")
                    .targetRef(ROLE_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                    .activation(disabled())
                .<UserType>end()
                .beginAssignment()
                    .description("enabled")
                    .targetRef(ROLE_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                .end();
        // @formatter:on

        addObject(user.asPrismObject(), null, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(user.getOid())
                .roleMembershipRefs()
                    .assertRole(ROLE_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRoleMemberhipRefs(1);
        // @formatter:on
    }

    /**
     * Situation:
     *
     *   user --a-(enabled)-> inducing-idempotent --i-> idempotent
     *        --a-(disabled)-> inducing-idempotent --i-> idempotent
     *
     * Checking that roleMembershipRef is correct even with idempotent roles caching.
     *
     * Note that this works even without explicit fixing because (1) non-active targets are not cached
     * (see {@link TargetEvaluation#evaluate()}), and (2) non-active non-direct targets are not even evaluated
     * (see {@link PathSegmentEvaluation#evaluateSegmentPayloadAndTargets()}).
     *
     * MID-7382
     */
    @Test
    public void test130TwoIndirectAssignmentsOfIdempotentRoleOneDisabled() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        // @formatter:off
        UserType user = new UserType()
                .name("test130")
                .beginAssignment()
                    .targetRef(ROLE_INDUCING_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                    .activation(disabled())
                .<UserType>end()
                .beginAssignment()
                    .description("enabled")
                    .targetRef(ROLE_INDUCING_IDEMPOTENT.oid, RoleType.COMPLEX_TYPE)
                .end();
        // @formatter:on

        addObject(user.asPrismObject(), null, task, result);

        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(user.getOid())
                .roleMembershipRefs()
                    .assertRole(ROLE_INDUCING_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRole(ROLE_IDEMPOTENT.oid, SchemaConstants.ORG_DEFAULT)
                    .assertRoleMemberhipRefs(2);
        // @formatter:on
    }

    private ActivationType disabled() {
        return new ActivationType()
                .administrativeStatus(ActivationStatusType.DISABLED);
    }

    /** Checks legalization of three accounts of different intents. MID-8562. */
    @Test
    public void test200LegalizeAccounts() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("three accounts");
        DummyAccount ethan = new DummyAccount("ethan");
        ethan.addAttributeValue(ATTR_TYPE, TYPE_EMPLOYEE);
        DummyAccount charlie = new DummyAccount("charlie");
        charlie.addAttributeValue(ATTR_TYPE, TYPE_CONTRACTOR);
        DummyAccount gustav = new DummyAccount("gustav");
        gustav.addAttributeValue(ATTR_TYPE, TYPE_GUEST);

        RESOURCE_DUMMY_LEGALIZING.getDummyResource().addAccount(ethan);
        RESOURCE_DUMMY_LEGALIZING.getDummyResource().addAccount(charlie);
        RESOURCE_DUMMY_LEGALIZING.getDummyResource().addAccount(gustav);

        when("accounts are imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_LEGALIZING.oid)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .withProcessingAllAccounts()
                .execute(result);

        then("the employee has account legalized");
        assertUserAfterByUsername("ethan")
                .assignments()
                .single()
                .assertResource(RESOURCE_DUMMY_LEGALIZING.oid)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(TYPE_EMPLOYEE);

        and("the contractor has account legalized");
        assertUserAfterByUsername("charlie")
                .assignments()
                .single()
                .assertResource(RESOURCE_DUMMY_LEGALIZING.oid)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(TYPE_CONTRACTOR);

        and("the guest has account not legalized");
        assertUserAfterByUsername("gustav")
                .assertAssignments(0);
    }
}
