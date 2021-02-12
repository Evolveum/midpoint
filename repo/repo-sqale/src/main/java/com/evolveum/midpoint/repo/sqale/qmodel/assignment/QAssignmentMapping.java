/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * Mapping between {@link QAssignment} and {@link AssignmentType}.
 */
public class QAssignmentMapping
        extends SqaleModelMapping<AssignmentType, QAssignment, MAssignment> {

    public static final String DEFAULT_ALIAS_NAME = "u";

    public static final QAssignmentMapping INSTANCE = new QAssignmentMapping();

    private QAssignmentMapping() {
        super(QAssignment.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AssignmentType.class, QAssignment.class);

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
