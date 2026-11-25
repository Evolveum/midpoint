/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPolicyRuleIndexExtractor extends AbstractSchemaTest {

    private static final String ROLE_A_OID = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String ROLE_B_OID = "aaaaaaaa-0000-0000-0000-000000000002";
    private static final String ARCHETYPE_OID = "aaaaaaaa-0000-0000-0000-000000000003";
    private static final String MARK_OID = "aaaaaaaa-0000-0000-0000-000000000004";

    @Test
    public void test100SimpleExclusionWithEnforcement() {

        PolicyRuleType rule = new PolicyRuleType().policyConstraints(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef(ROLE_A_OID, RoleType.COMPLEX_TYPE)))
                .policyActions(new PolicyActionsType().enforcement(new EnforcementPolicyActionType()));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(data.constraints()).containsExactly(PolicyConstraintsType.F_EXCLUSION);
        assertThat(data.actions()).containsExactly(PolicyActionsType.F_ENFORCEMENT);
        assertThat(data.refs()).hasSize(1);
        IndexedRef ref = data.refs().iterator().next();
        assertThat(ref.holder()).isEqualTo(PolicyConstraintsType.F_EXCLUSION);
        assertThat(ref.refItem()).isEqualTo(AssignmentType.F_TARGET_REF);
        assertThat(ref.targetOid()).isEqualTo(ROLE_A_OID);
        assertThat(ref.targetType()).isEqualTo(RoleType.COMPLEX_TYPE);
        assertThat(ref.relation()).isEqualTo(PrismContext.get().getDefaultRelation());
    }

    @Test
    public void test110NestedConstraintsAreFlattened() {
        PolicyRuleType rule = new PolicyRuleType().policyConstraints(new PolicyConstraintsType().and(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()).not(new PolicyConstraintsType()
                                .hasAssignment(new HasAssignmentPolicyConstraintType().targetRef(ROLE_B_OID, RoleType.COMPLEX_TYPE))))
                .or(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef(ROLE_A_OID, RoleType.COMPLEX_TYPE)))

        );
        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(data.constraints()).containsExactlyInAnyOrder(
                PolicyConstraintsType.F_MODIFICATION,
                PolicyConstraintsType.F_HAS_ASSIGNMENT,
                PolicyConstraintsType.F_EXCLUSION);
        assertThat(paths(data)).containsExactlyInAnyOrder(
                "hasAssignment/targetRef", "exclusion/targetRef");
    }

    @Test
    public void test120RuleWithoutActions() {
        PolicyRuleType rule = new PolicyRuleType().policyConstraints(new PolicyConstraintsType()
                .modification(new ModificationPolicyConstraintType()));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);
        assertThat(data.actions()).hasSize(0);
        assertThat(data.refs()).hasSize(0);
    }

    @Test
    public void test130MoreActions() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()))
                .policyActions(new PolicyActionsType()
                        .record(new RecordPolicyActionType())
                        .notification(new NotificationPolicyActionType()));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(data.actions()).containsExactlyInAnyOrder(
                PolicyActionsType.F_RECORD, PolicyActionsType.F_NOTIFICATION);
    }

    @Test
    public void test140TargetPathsForAllConstraintKinds() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef(ROLE_A_OID, RoleType.COMPLEX_TYPE))
                        .requirement(new ExclusionPolicyConstraintType()
                                .targetArchetypeRef(ARCHETYPE_OID, ArchetypeType.COMPLEX_TYPE))
                        .hasAssignment(new HasAssignmentPolicyConstraintType()
                                .targetRef(ROLE_B_OID, RoleType.COMPLEX_TYPE))
                        .hasNoAssignment(new HasAssignmentPolicyConstraintType()
                                .targetArchetypeRef(ARCHETYPE_OID, ArchetypeType.COMPLEX_TYPE)));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(paths(data)).containsExactlyInAnyOrder(
                "exclusion/targetRef",
                "requirement/targetArchetypeRef",
                "hasAssignment/targetRef",
                "hasNoAssignment/targetArchetypeRef");
    }

    @Test
    public void test145ArchetypeIsIgnoredWhenTargetRefIsPresent() {
        // The model evaluates targetArchetypeRef only when there is no targetRef (ConstraintReferenceMatcher).
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef(ROLE_A_OID, RoleType.COMPLEX_TYPE)
                                .targetArchetypeRef(ARCHETYPE_OID, ArchetypeType.COMPLEX_TYPE))
                        .hasAssignment(new HasAssignmentPolicyConstraintType()
                                .targetRef(ROLE_B_OID, RoleType.COMPLEX_TYPE)
                                .targetArchetypeRef(ARCHETYPE_OID, ArchetypeType.COMPLEX_TYPE)));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(paths(data)).containsExactlyInAnyOrder(
                "exclusion/targetRef",
                "hasAssignment/targetRef");
    }

    @Test
    public void test150MarkRef() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType()))
                .markRef(MARK_OID, MarkType.COMPLEX_TYPE);

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);
        assertThat(data.refs()).hasSize(1);
        IndexedRef ref = data.refs().iterator().next();
        assertThat(ref.refItem()).isEqualTo(PolicyRuleType.F_MARK_REF);
        assertThat(ref.holder()).isEqualTo(AssignmentType.F_POLICY_RULE);
        assertThat(ref.targetOid()).isEqualTo(MARK_OID);
    }

    @Test
    public void test160ReferenceWithoutOidIsIgnored() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef(new ObjectReferenceType().type(RoleType.COMPLEX_TYPE))));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(data.constraints()).containsExactly(PolicyConstraintsType.F_EXCLUSION);
        assertThat(data.refs()).isEmpty();
    }

    @Test
    public void test170SameTargetTwiceIsIndexedOnce() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef(ROLE_A_OID, RoleType.COMPLEX_TYPE))
                        .exclusion(new ExclusionPolicyConstraintType()
                                // no type and explicit default relation: still the same database key
                                .targetRef(new ObjectReferenceType()
                                        .oid(ROLE_A_OID)
                                        .relation(PrismContext.get().getDefaultRelation())))
                        .exclusion(new ExclusionPolicyConstraintType()
                                .targetRef(ROLE_B_OID, RoleType.COMPLEX_TYPE)));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(data.refs()).hasSize(2);
        assertThat(data.refs().stream().map(IndexedRef::targetOid).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(ROLE_A_OID, ROLE_B_OID);
    }

    @Test
    public void test180TransitionAndItsContentAreListed() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .transition(new TransitionPolicyConstraintType()
                                .constraints(new PolicyConstraintsType()
                                        .hasAssignment(new HasAssignmentPolicyConstraintType()
                                                .targetRef(ROLE_A_OID, RoleType.COMPLEX_TYPE)))));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(data.constraints()).containsExactlyInAnyOrder(
                PolicyConstraintsType.F_TRANSITION, PolicyConstraintsType.F_HAS_ASSIGNMENT);
        assertThat(paths(data)).containsExactly("hasAssignment/targetRef");
    }

    @Test
    public void test190NamedConstraintReferenceIsIgnored() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .ref(new PolicyConstraintReferenceType().name("some-constraint"))
                        .modification(new ModificationPolicyConstraintType()));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        assertThat(data.constraints()).containsExactly(PolicyConstraintsType.F_MODIFICATION);
    }

    @Test
    public void test200ConstraintNamesAreElementNamesNotKinds() {
        PolicyRuleType rule = new PolicyRuleType()
                .policyConstraints(new PolicyConstraintsType()
                        .modification(new ModificationPolicyConstraintType())
                        .assignment(new AssignmentModificationPolicyConstraintType()));

        PolicyRuleIndexData data = PolicyRuleIndexExtractor.extract(rule);

        // element names as written in the rule, not PolicyConstraintKindType values
        assertThat(data.constraints())
                .extracting(QName::getLocalPart)
                .containsExactlyInAnyOrder("modification", "assignment")
                .doesNotContain("objectModification", "assignmentModification");
    }

    @Test
    public void test400ActivityPoliciesCollector() {
        ActivityPoliciesType top = new ActivityPoliciesType()
                .policyRef("11111111-0000-0000-0000-000000000001", PolicyType.COMPLEX_TYPE);
        ActivityPoliciesType nested = new ActivityPoliciesType()
                .policy(new PolicyRuleType().name("nested"));
        ActivityPoliciesDefinitionTailoringType changed = new ActivityPoliciesDefinitionTailoringType()
                .policy(new PolicyRuleType().name("changed"));

        ActivityDefinitionType activity = new ActivityDefinitionType()
                .policies(top)
                .composition(new ActivityCompositionType()
                        .activity(new ActivityDefinitionType()
                                .composition(new ActivityCompositionType()
                                        .activity(new ActivityDefinitionType()
                                                .policies(nested)))))
                .tailoring(new ActivitiesTailoringType()
                        .change(new ActivityTailoringType()
                                .policies(changed)));

        List<ActivityPoliciesType> collected = ActivityPoliciesCollector.collect(activity);

        assertThat(collected).hasSize(3);
        assertThat(collected.stream()
                .flatMap(p -> p.getPolicy().stream())
                .map(PolicyRuleType::getName)
                .collect(Collectors.toList()))
                .containsExactlyInAnyOrder("nested", "changed");
        assertThat(ActivityPoliciesCollector.collect(null)).isEmpty();
    }

    private List<String> paths(PolicyRuleIndexData data) {
        return data.refs().stream()
                .map(r -> r.holder().getLocalPart() + "/" + r.refItem().getLocalPart())
                .collect(Collectors.toList());
    }
}
