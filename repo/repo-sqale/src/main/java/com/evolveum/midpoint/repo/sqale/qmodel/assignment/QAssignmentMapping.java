/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.F_TARGET_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.F_TENANT_REF;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * Mapping between {@link QAssignment} and {@link AssignmentType}.
 */
public class QAssignmentMapping
        extends SqaleModelMapping<AssignmentType, QAssignment, MAssignment> {

    public static final String DEFAULT_ALIAS_NAME = "a";

    public static final QAssignmentMapping INSTANCE = new QAssignmentMapping();

    private QAssignmentMapping() {
        super(QAssignment.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AssignmentType.class, QAssignment.class);

        addItemMapping(F_TARGET_REF, RefItemFilterProcessor.mapper(
                path(q -> q.targetRefTargetOid),
                path(q -> q.targetRefTargetType),
                path(q -> q.targetRefRelationId)));
        addItemMapping(F_TENANT_REF, RefItemFilterProcessor.mapper(
                path(q -> q.tenantRefTargetOid),
                path(q -> q.tenantRefTargetType),
                path(q -> q.tenantRefRelationId)));

        // TODO mapping
    }

    @Override
    protected QAssignment newAliasInstance(String alias) {
        return new QAssignment(alias);
    }

    @Override
    public AssignmentSqlTransformer createTransformer(
            SqlTransformerContext transformerContext) {
        return new AssignmentSqlTransformer(transformerContext, this);
    }

    @Override
    public MAssignment newRowObject() {
        return new MAssignment();
    }
}
