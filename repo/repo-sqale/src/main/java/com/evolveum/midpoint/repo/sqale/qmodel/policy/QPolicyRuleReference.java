/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import com.evolveum.midpoint.repo.sqale.qmodel.assignment.MAssignment;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import java.sql.Types;

import static com.querydsl.sql.ColumnMetadata.named;

/**
 * Querydsl query type for the `m_ref_policy_rule` table.
 */

public class QPolicyRuleReference extends QReference<MPolicyRuleReference, MPolicyRule> {

    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "m_ref_policy_rule";

    public static final ColumnMetadata POLICY_RULE_CID =
            ColumnMetadata.named("policyRuleCid").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata POLICY_REFERENCE_TYPE =
            ColumnMetadata.named("policyReferenceType").ofType(Types.OTHER).notNull();

    public final NumberPath<Long> policyRuleCid = createLong("policyRuleCid", POLICY_RULE_CID);
    public final EnumPath<MPolicyReferenceType> policyReferenceType =
            createEnum("policyReferenceType", MPolicyReferenceType.class, POLICY_REFERENCE_TYPE);

    public final PrimaryKey<MPolicyRuleReference> pk =
            createPrimaryKey(ownerOid, policyRuleCid, policyReferenceType, relationId, targetOid);

    public QPolicyRuleReference(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QPolicyRuleReference(String variable, String schema, String table) {
        super(MPolicyRuleReference.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MPolicyRule ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
                .and(policyRuleCid.eq(ownerRow.cid));
    }

}
