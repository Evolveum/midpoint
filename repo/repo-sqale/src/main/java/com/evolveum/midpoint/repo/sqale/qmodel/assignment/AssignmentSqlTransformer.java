/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

public class AssignmentSqlTransformer
        extends SqaleTransformerBase<AssignmentType, QAssignment, MAssignment> {

    public AssignmentSqlTransformer(
            SqlTransformerContext transformerContext, QAssignmentMapping mapping) {
        super(transformerContext, mapping);
    }

    @Override
    public AssignmentType toSchemaObject(MAssignment row) {
        return null;
    }

    // TODO to row? and back...
}
