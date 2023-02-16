/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import com.evolveum.midpoint.model.intest.rbac.TestSegregationOfDuties;

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
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
 *
 * NOTE: At the model level, see also TestPolicyRules, TestPolicyRules2, TestAssignmentEvaluator
 *
 * @see TestRbac
 * @see TestSegregationOfDuties
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentsProcessing extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/assignments");

    private static final TestResource<RoleType> ROLE_IDEMPOTENT =
            new TestResource<>(TEST_DIR, "role-idempotent.xml", "c7697df9-b873-4ae3-9854-7d01dd6f0c27");
    private static final TestResource<RoleType> ROLE_INDUCING_IDEMPOTENT =
            new TestResource<>(TEST_DIR, "role-inducing-idempotent.xml", "7e2e721c-6f9e-4013-be4d-bbe32ea64238");

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ROLE_IDEMPOTENT, initTask, initResult);
        addObject(ROLE_INDUCING_IDEMPOTENT, initTask, initResult);
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
}
