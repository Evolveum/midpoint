/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.func;

import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.MAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.assignment.QAssignmentMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.policy.MAssignmentPolicyRuleReference;
import com.evolveum.midpoint.repo.sqale.qmodel.policy.MPolicyReferenceType;
import com.evolveum.midpoint.repo.sqale.qmodel.policy.QAssignmentPolicyRuleReference;
import com.evolveum.midpoint.repo.sqale.qmodel.policy.QAssignmentPolicyRuleReferenceMapping;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SqaleRepoPolicyRuleIndexTest extends SqaleRepoBaseTest {

    private static final long ROLE1_INDUCEMENT_WITH_RULE_ID = 10L;
    private static final long ROLE1_INDUCEMENT_WITHOUT_RULE_ID = 11L;

    private static final String ROLE_X_OID = "aaaaaaaa-1111-0000-0000-000000000001";
    private static final String ROLE_Y_OID = "aaaaaaaa-1111-0000-0000-000000000002";
    private static final String TARGET_MARK_OID = "aaaaaaaa-1111-0000-0000-000000000004";
    private static final String ARCHETYPE_OID = "7b1c9e4a-3f2d-4a8b-9c61-5e0d2f8a1b34";

    private String role1Oid; // inducement: exclusion of X (in "and") + enforcement, markRef; inducement without rule
    private String role2Oid;
    private String role3Oid;
    private String policy1Oid;


    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        role1Oid = repositoryService.addObject(
                new RoleType()
                        .name("role-1")
                        .inducement(new AssignmentType()
                                .id(ROLE1_INDUCEMENT_WITH_RULE_ID)
                                .policyRule(new PolicyRuleType()
                                        .policyConstraints(new PolicyConstraintsType()
                                                .and(new PolicyConstraintsType()
                                                        .exclusion(new ExclusionPolicyConstraintType()
                                                                .targetRef(ROLE_X_OID, RoleType.COMPLEX_TYPE))))
                                        .markRef(TARGET_MARK_OID, MarkType.COMPLEX_TYPE)
                                        .policyActions(new PolicyActionsType()
                                                .enforcement(new EnforcementPolicyActionType()))))
                        .inducement(new AssignmentType()
                                .id(ROLE1_INDUCEMENT_WITHOUT_RULE_ID)
                                .targetRef(ROLE_Y_OID, RoleType.COMPLEX_TYPE))
                        .asPrismObject(),
                null, result);

        // rule without actions in an inducement + rule with requirement in an assignment
        role2Oid = repositoryService.addObject(
                new RoleType()
                        .name("role-2")
                        .inducement(new AssignmentType()
                                .policyRule(new PolicyRuleType()
                                        .policyConstraints(new PolicyConstraintsType()
                                                .modification(new ModificationPolicyConstraintType()))))
                        .assignment(new AssignmentType()
                                .policyRule(new PolicyRuleType()
                                        .policyConstraints(new PolicyConstraintsType()
                                                .requirement(new ExclusionPolicyConstraintType()
                                                        .targetRef(ROLE_X_OID, RoleType.COMPLEX_TYPE)))
                                        .policyActions(new PolicyActionsType()
                                                .notification(new NotificationPolicyActionType()))))
                        .asPrismObject(),
                null, result);

// no policy rules at all
        role3Oid = repositoryService.addObject(
                new RoleType()
                        .name("role-3")
                        .inducement(new AssignmentType()
                                .targetRef(ROLE_X_OID, RoleType.COMPLEX_TYPE))
                        .asPrismObject(),
                null, result);

// policy: two exclusions in one rule (role target and archetype target), two actions
        policy1Oid = repositoryService.addObject(
                new PolicyType()
                        .name("policy-1")
                        .inducement(new AssignmentType()
                                .policyRule(new PolicyRuleType()
                                        .policyConstraints(new PolicyConstraintsType()
                                                .exclusion(new ExclusionPolicyConstraintType()
                                                        .targetRef(ROLE_Y_OID, RoleType.COMPLEX_TYPE))
                                                .exclusion(new ExclusionPolicyConstraintType()
                                                        .targetArchetypeRef(ARCHETYPE_OID, ArchetypeType.COMPLEX_TYPE)))
                                        .policyActions(new PolicyActionsType()
                                                .enforcement(new EnforcementPolicyActionType())
                                                .notification(new NotificationPolicyActionType()))))
                        .asPrismObject(),
                null, result);


    }

    @Test
    public void test100InducementRuleIsIndexedOnAssignmentRow() {

        QAssignment<?> a = QAssignmentMapping.getInducementMapping().defaultAlias();

        MAssignment withRule = selectOne(a,
                a.ownerOid.eq(UUID.fromString(role1Oid)), a.cid.eq(ROLE1_INDUCEMENT_WITH_RULE_ID));
        // "and" is not listed, only the constraint nested in it
        assertThat(withRule.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_EXCLUSION));
        assertThat(withRule.policyRuleActions)
                .containsExactly(cachedUriId(PolicyActionsType.F_ENFORCEMENT));

        MAssignment withoutRule = selectOne(a,
                a.ownerOid.eq(UUID.fromString(role1Oid)), a.cid.eq(ROLE1_INDUCEMENT_WITHOUT_RULE_ID));
        assertThat(withoutRule.policyRuleConstraints).isNull();
        assertThat(withoutRule.policyRuleActions).isNull();
    }
    @Test
    public void test110RuleWithoutActionsHasEmptyActions() {
        QAssignment<?> a = QAssignmentMapping.getInducementMapping().defaultAlias();
        MAssignment row = selectOne(a,
                a.ownerOid.eq(UUID.fromString(role2Oid)),
                a.containerType.eq(MContainerType.INDUCEMENT));

        assertThat(row.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_MODIFICATION));
        assertThat(row.policyRuleActions).isNotNull().isEmpty();   // empty, not null: there is a rule
    }

    @Test
    public void test115RuleInAssignmentIsIndexed() {
        QAssignment<?> a = QAssignmentMapping.getAssignmentMapping().defaultAlias();
        MAssignment row = selectOne(a,
                a.ownerOid.eq(UUID.fromString(role2Oid)),
                a.containerType.eq(MContainerType.ASSIGNMENT));

        assertThat(row.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_REQUIREMENT));
        assertThat(row.policyRuleActions)
                .containsExactly(cachedUriId(PolicyActionsType.F_NOTIFICATION));
        assertThat(ruleRefs(role2Oid, row.cid))
                .singleElement()
                .matches(r -> r.policyReferenceType == MPolicyReferenceType.REQUIREMENT_TARGET
                        && r.targetOid.equals(UUID.fromString(ROLE_X_OID)));
    }

    @Test
    public void test125RoleAndArchetypeTargetsInOneRule() {
        QAssignment<?> a = QAssignmentMapping.getInducementMapping().defaultAlias();
        MAssignment row = selectOne(a, a.ownerOid.eq(UUID.fromString(policy1Oid)));

        // two exclusions, but the constraint type is listed once
        assertThat(row.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_EXCLUSION));
        assertThat(row.policyRuleActions).containsExactlyInAnyOrder(
                cachedUriId(PolicyActionsType.F_ENFORCEMENT), cachedUriId(PolicyActionsType.F_NOTIFICATION));
        assertThat(ruleRefs(policy1Oid, row.cid))
                .extracting(r -> r.policyReferenceType)
                .containsExactlyInAnyOrder(
                        MPolicyReferenceType.EXCLUSION_TARGET,
                        MPolicyReferenceType.EXCLUSION_TARGET_ARCHETYPE);
    }

    @Test
    public void test130ObjectWithoutRulesHasNoIndex() {
        QAssignment<?> a = QAssignmentMapping.getInducementMapping().defaultAlias();
        MAssignment row = selectOne(a, a.ownerOid.eq(UUID.fromString(role3Oid)));

        assertThat(row.policyRuleConstraints).isNull();
        assertThat(row.policyRuleActions).isNull();
        QAssignmentPolicyRuleReference r = QAssignmentPolicyRuleReferenceMapping.get().defaultAlias();
        assertThat(count(r, r.ownerOid.eq(UUID.fromString(role3Oid)))).isZero();
    }

    private List<MAssignmentPolicyRuleReference> ruleRefs(String ownerOid, long cid) {
        QAssignmentPolicyRuleReference r = QAssignmentPolicyRuleReferenceMapping.get().defaultAlias();
        return select(r,
                r.ownerOid.eq(UUID.fromString(ownerOid)),
                r.assignmentCid.eq(cid));
    }

}
