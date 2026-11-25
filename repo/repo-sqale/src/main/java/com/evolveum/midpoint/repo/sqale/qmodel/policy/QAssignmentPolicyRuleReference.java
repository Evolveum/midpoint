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

import java.io.Serial;
import java.sql.Types;

import static com.querydsl.sql.ColumnMetadata.named;

/**
 * Querydsl query type for the `m_ref_assignment_policy_rule` table: references (constraint targets, marks)
 * of the policy rules embedded in assignments and inducements.
 */

public class QAssignmentPolicyRuleReference extends QReference<MAssignmentPolicyRuleReference, MAssignment> {

    @Serial private static final long serialVersionUID = -1729936414530158037L;

    public static final String TABLE_NAME = "m_ref_assignment_policy_rule";

    public static final ColumnMetadata ASSIGNMENT_CID =
            named("assignmentCid").ofType(Types.BIGINT).notNull();

    public static final ColumnMetadata POLICY_REFERENCE_TYPE = named("policyReferenceType").ofType(Types.OTHER).notNull();

    EnumPath<MPolicyReferenceType> policyReferenceType = createEnum("policyReferenceType", MPolicyReferenceType.class, POLICY_REFERENCE_TYPE);

    //  PK (ownerOid, assignmentCid, policyReferenceType, relationId, targetOid)

    public final NumberPath<Long> assignmentCid = createLong("assignmentCid", ASSIGNMENT_CID);

    public final PrimaryKey<MAssignmentPolicyRuleReference> pk =
            createPrimaryKey(ownerOid, assignmentCid, policyReferenceType, relationId, targetOid);

    public QAssignmentPolicyRuleReference(String variable, String schema, String table) {
        super(MAssignmentPolicyRuleReference.class, variable, schema, table);
    }

    public QAssignmentPolicyRuleReference(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    @Override
    public BooleanExpression isOwnedBy(MAssignment ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
                .and(assignmentCid.eq(ownerRow.cid));
    }

}
