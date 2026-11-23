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
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SqaleRepoPolicyRuleIndexTest extends SqaleRepoBaseTest {

    private static final long ROLE1_INDUCEMENT_WITH_RULE_ID = 10L;
    private static final long ROLE1_INDUCEMENT_WITHOUT_RULE_ID = 11L;

    private static final String ROLE_X_OID = "aaaaaaaa-1111-0000-0000-000000000001";
    private static final String ROLE_Y_OID = "aaaaaaaa-1111-0000-0000-000000000002";
    private static final String TARGET_MARK_OID = "aaaaaaaa-1111-0000-0000-000000000004";

    private String role1Oid; // inducement: exclusion of X (in "and") + enforcement, markRef; inducement without rule

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

}
