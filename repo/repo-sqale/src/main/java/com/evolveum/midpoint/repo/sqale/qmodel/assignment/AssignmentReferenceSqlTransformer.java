/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.ReferenceSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;

public class AssignmentReferenceSqlTransformer
        extends ReferenceSqlTransformer<QAssignmentReference, MAssignmentReference> {

    public AssignmentReferenceSqlTransformer(
            SqlTransformerSupport transformerSupport, QAssignmentReferenceMapping mapping) {
        super(transformerSupport, mapping);
    }
}
