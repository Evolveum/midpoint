/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QPolicyRule<OR extends MObject> extends QContainer<MPolicyRule, OR> {

    public static final String TABLE_NAME = "m_policy_rule";

    public static final ColumnMetadata OWNER_TYPE =
            ColumnMetadata.named("ownerType").ofType(Types.OTHER);
    public static final ColumnMetadata POLICY_RULE_CONSTRAINTS =
            ColumnMetadata.named("policyRuleConstraints").ofType(Types.ARRAY);
    public static final ColumnMetadata POLICY_RULE_ACTIONS =
            ColumnMetadata.named("policyRuleActions").ofType(Types.ARRAY);

    public final EnumPath<MObjectType> ownerType = createEnum("ownerType", MObjectType.class, OWNER_TYPE);
    public final ArrayPath<Integer[], Integer> policyRuleConstraints =
            createArray("policyRuleConstraints", Integer[].class, POLICY_RULE_CONSTRAINTS);
    public final ArrayPath<Integer[], Integer> policyRuleActions =
            createArray("policyRuleActions", Integer[].class, POLICY_RULE_ACTIONS);

    public QPolicyRule(String variable) { this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME); }
    public QPolicyRule(String variable, String schema, String table) {
        super(MPolicyRule.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) { return ownerOid.eq(ownerRow.oid); }
}
