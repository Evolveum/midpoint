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
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.policy.*;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.exception.SchemaException;
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
    private String mark1Oid;

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

        mark1Oid = repositoryService.addObject(
                new MarkType()
                        .name("mark-1")
                        .policyRule(new GlobalPolicyRuleType()
                                .id(1L)
                                .markRef(TARGET_MARK_OID, MarkType.COMPLEX_TYPE)
                                .policyConstraints(new PolicyConstraintsType()
                                        .exclusion(new ExclusionPolicyConstraintType()
                                                .targetRef(ROLE_X_OID, RoleType.COMPLEX_TYPE)))
                                .policyActions(new PolicyActionsType()
                                        .enforcement(new EnforcementPolicyActionType())))
                        .policyRule(new GlobalPolicyRuleType()
                                .id(2L)
                                .policyConstraints(new PolicyConstraintsType()
                                        .modification(new ModificationPolicyConstraintType())))
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

    @Test
    public void test200MarkRulesAreStoredInPolicyRuleTable() {
        QPolicyRule<?> p = QPolicyRuleMapping.get().defaultAlias();
        assertThat(count(p, p.ownerOid.eq(UUID.fromString(mark1Oid)))).isEqualTo(2);

        MPolicyRule rule1 = policyRuleRow(mark1Oid, 1);
        assertThat(rule1.ownerType).isEqualTo(MObjectType.MARK);
        assertThat(rule1.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_EXCLUSION));
        assertThat(rule1.policyRuleActions)
                .containsExactly(cachedUriId(PolicyActionsType.F_ENFORCEMENT));

        MPolicyRule rule2 = policyRuleRow(mark1Oid, 2);
        assertThat(rule2.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_MODIFICATION));
        assertThat(rule2.policyRuleActions).isNotNull().isEmpty();
    }

    @Test
    public void test210MarkRuleReferencesAreStored() {
        assertThat(policyRuleRefs(mark1Oid, 1))
                .extracting(r -> r.policyReferenceType)
                .containsExactlyInAnyOrder(
                        MPolicyReferenceType.EXCLUSION_TARGET,
                        MPolicyReferenceType.MARK);
        assertThat(policyRuleRefs(mark1Oid, 2)).isEmpty();
    }

    private MAssignment inducementRow(String ownerOid, long cid) {
        QAssignment<?> a = QAssignmentMapping.getInducementMapping().defaultAlias();
        return selectOne(a, a.ownerOid.eq(UUID.fromString(ownerOid)), a.cid.eq(cid));
    }

    @Test
    public void test400ModificationInsideRuleReindexesIt() throws Exception {
        OperationResult result = createOperationResult();

        when("an action is added and the exclusion target is changed");
        repositoryService.modifyObject(RoleType.class, role1Oid,
                prismContext.deltaFor(RoleType.class)
                        .item(RoleType.F_INDUCEMENT, ROLE1_INDUCEMENT_WITH_RULE_ID,
                                AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_ACTIONS, PolicyActionsType.F_RECORD)
                        .replace(new RecordPolicyActionType())
                        .item(RoleType.F_INDUCEMENT, ROLE1_INDUCEMENT_WITH_RULE_ID,
                                AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS)
                        .replace(new PolicyConstraintsType()
                                .exclusion(new ExclusionPolicyConstraintType()
                                        .targetRef(ROLE_Y_OID, RoleType.COMPLEX_TYPE)))
                        .asItemDeltas(),
                result);

        then("columns and references follow the rule");
        MAssignment row = inducementRow(role1Oid, ROLE1_INDUCEMENT_WITH_RULE_ID);
        assertThat(row.policyRuleActions).containsExactlyInAnyOrder(
                cachedUriId(PolicyActionsType.F_ENFORCEMENT), cachedUriId(PolicyActionsType.F_RECORD));
        assertThat(ruleRefs(role1Oid, ROLE1_INDUCEMENT_WITH_RULE_ID))
                .anyMatch(r -> r.policyReferenceType == MPolicyReferenceType.EXCLUSION_TARGET
                        && r.targetOid.equals(UUID.fromString(ROLE_Y_OID)))
                .noneMatch(r -> r.targetOid.equals(UUID.fromString(ROLE_X_OID)))
                .anyMatch(r -> r.policyReferenceType == MPolicyReferenceType.MARK);   // markRef was not touched
    }

    @Test
    public void test410AddingRuleToInducementIndexesIt() throws Exception {
        OperationResult result = createOperationResult();

        given("inducement 11 of role-1 without policy rule");
        assertThat(inducementRow(role1Oid, 11).policyRuleConstraints).isNull();
        assertThat(ruleRefs(role1Oid, 11)).isEmpty();

        when("policy rule is added to the inducement");
        repositoryService.modifyObject(RoleType.class, role1Oid,
                prismContext.deltaFor(RoleType.class)
                        .item(RoleType.F_INDUCEMENT, 11L, AssignmentType.F_POLICY_RULE)
                        .replace(new PolicyRuleType()
                                .policyConstraints(new PolicyConstraintsType()
                                        .exclusion(new ExclusionPolicyConstraintType()
                                                .targetRef(ROLE_Y_OID, RoleType.COMPLEX_TYPE)))
                                .policyActions(new PolicyActionsType()
                                        .notification(new NotificationPolicyActionType())))
                        .asItemDeltas(),
                result);

        then("index columns and references are filled");
        assertThatOperationResult(result).isSuccess();

        MAssignment row = inducementRow(role1Oid, 11);
        assertThat(row.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_EXCLUSION));
        assertThat(row.policyRuleActions)
                .containsExactly(cachedUriId(PolicyActionsType.F_NOTIFICATION));
        assertThat(ruleRefs(role1Oid, 11))
                .singleElement()
                .matches(r -> r.policyReferenceType == MPolicyReferenceType.EXCLUSION_TARGET
                        && r.targetOid.equals(UUID.fromString(ROLE_Y_OID)));

        and("the other inducement is untouched");
        assertThat(ruleRefs(role1Oid, 10)).hasSize(2);   // EXCLUSION_TARGET X + MARK
    }

    @Test
    public void test420RemovingRuleClearsTheIndex() throws Exception {
        OperationResult result = createOperationResult();

        given("inducement 10 of role-1 with policy rule");
        assertThat(inducementRow(role1Oid, 10).policyRuleConstraints).isNotNull();
        assertThat(ruleRefs(role1Oid, 10)).isNotEmpty();

        when("policy rule is removed from the inducement");
        repositoryService.modifyObject(RoleType.class, role1Oid,
                prismContext.deltaFor(RoleType.class)
                        .item(RoleType.F_INDUCEMENT, 10L, AssignmentType.F_POLICY_RULE)
                        .replace()
                        .asItemDeltas(),
                result);

        then("index columns are null and references are gone");
        assertThatOperationResult(result).isSuccess();

        MAssignment row = inducementRow(role1Oid, 10);
        assertThat(row.policyRuleConstraints).isNull();
        assertThat(row.policyRuleActions).isNull();
        assertThat(ruleRefs(role1Oid, 10)).isEmpty();

        and("the inducement itself still exists");
        assertThat(row).isNotNull();
    }

    @Test
    public void test430AddingInducementWithRuleIndexesIt() throws Exception {
        OperationResult result = createOperationResult();

        when("new inducement with policy rule is added to role-3");
        repositoryService.modifyObject(RoleType.class, role3Oid,
                prismContext.deltaFor(RoleType.class)
                        .item(RoleType.F_INDUCEMENT)
                        .add(new AssignmentType()
                                .id(5L)
                                .policyRule(new PolicyRuleType()
                                        .markRef(TARGET_MARK_OID, MarkType.COMPLEX_TYPE)
                                        .policyConstraints(new PolicyConstraintsType()
                                                .hasAssignment(new HasAssignmentPolicyConstraintType()
                                                        .targetRef(ROLE_X_OID, RoleType.COMPLEX_TYPE)))
                                        .policyActions(new PolicyActionsType()
                                                .enforcement(new EnforcementPolicyActionType()))))
                        .asItemDeltas(),
                result);

        then("new inducement is indexed");
        assertThatOperationResult(result).isSuccess();

        MAssignment row = inducementRow(role3Oid, 5);
        assertThat(row.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_HAS_ASSIGNMENT));
        assertThat(row.policyRuleActions)
                .containsExactly(cachedUriId(PolicyActionsType.F_ENFORCEMENT));
        assertThat(ruleRefs(role3Oid, 5))
                .extracting(r -> r.policyReferenceType)
                .containsExactlyInAnyOrder(
                        MPolicyReferenceType.HAS_ASSIGNMENT_TARGET,
                        MPolicyReferenceType.MARK);

        and("the original inducement without rule stays unindexed");
        MAssignment original = inducementRow(role3Oid, 1);
        assertThat(original.policyRuleConstraints).isNull();
        assertThat(ruleRefs(role3Oid, 1)).isEmpty();
    }

    @Test
    public void test440ChangeInsideMarkRuleReindexesIt() throws Exception {
        OperationResult result = createOperationResult();

        when("actions of mark rule 2 are replaced");
        repositoryService.modifyObject(MarkType.class, mark1Oid,
                prismContext.deltaFor(MarkType.class)
                        .item(MarkType.F_POLICY_RULE, 2L, PolicyRuleType.F_POLICY_ACTIONS)
                        .replace(new PolicyActionsType()
                                .notification(new NotificationPolicyActionType()))
                        .asItemDeltas(),
                result);

        then("rule 2 has the new action, rule 1 is untouched");
        assertThatOperationResult(result).isSuccess();
        assertThat(policyRuleRow(mark1Oid, 2).policyRuleActions)
                .containsExactly(cachedUriId(PolicyActionsType.F_NOTIFICATION));
        assertThat(policyRuleRefs(mark1Oid, 1)).hasSize(2);
    }

    @Test
    public void test450AddingMarkRuleIndexesIt() throws Exception {
        OperationResult result = createOperationResult();

        when("new rule is added to the mark");
        repositoryService.modifyObject(MarkType.class, mark1Oid,
                prismContext.deltaFor(MarkType.class)
                        .item(MarkType.F_POLICY_RULE)
                        .add(new GlobalPolicyRuleType()
                                .id(5L)
                                .policyConstraints(new PolicyConstraintsType()
                                        .hasAssignment(new HasAssignmentPolicyConstraintType()
                                                .targetRef(ROLE_Y_OID, RoleType.COMPLEX_TYPE))))
                        .asItemDeltas(),
                result);

        then("new row and its reference exist");
        assertThatOperationResult(result).isSuccess();
        MPolicyRule row = policyRuleRow(mark1Oid, 5);
        assertThat(row).isNotNull();
        assertThat(row.policyRuleConstraints)
                .containsExactly(cachedUriId(PolicyConstraintsType.F_HAS_ASSIGNMENT));
        assertThat(policyRuleRefs(mark1Oid, 5))
                .singleElement()
                .matches(r -> r.policyReferenceType == MPolicyReferenceType.HAS_ASSIGNMENT_TARGET
                        && r.targetOid.equals(UUID.fromString(ROLE_Y_OID)));
    }

    @Test
    public void test460RemovingMarkRuleDeletesItsIndex() throws Exception {
        OperationResult result = createOperationResult();

        when("rule 1 is removed from the mark");
        repositoryService.modifyObject(MarkType.class, mark1Oid,
                prismContext.deltaFor(MarkType.class)
                        .item(MarkType.F_POLICY_RULE)
                        .delete(new GlobalPolicyRuleType().id(1L))
                        .asItemDeltas(),
                result);

        then("row and references of rule 1 are gone, others stay");
        assertThatOperationResult(result).isSuccess();
        assertThat(policyRuleRow(mark1Oid, 1)).isNull();
        assertThat(policyRuleRefs(mark1Oid, 1)).isEmpty();
        assertThat(policyRuleRow(mark1Oid, 2)).isNotNull();
    }

    private MPolicyRule policyRuleRow(String ownerOid, long cid) {
        QPolicyRule<?> p = QPolicyRuleMapping.get().defaultAlias();
        return selectOneNullable(p,
                p.ownerOid.eq(UUID.fromString(ownerOid)),
                p.cid.eq(cid));
    }

    private List<MPolicyRuleReference> policyRuleRefs(String ownerOid, long cid) {
        QPolicyRuleReference r = QPolicyRuleReferenceMapping.get().defaultAlias();
        return select(r,
                r.ownerOid.eq(UUID.fromString(ownerOid)),
                r.policyRuleCid.eq(cid));
    }

}
