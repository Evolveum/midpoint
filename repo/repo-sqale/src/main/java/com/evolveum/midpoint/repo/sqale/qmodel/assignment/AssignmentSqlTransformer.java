/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ContainerSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

public class AssignmentSqlTransformer
        extends ContainerSqlTransformer<AssignmentType, QAssignment, MAssignment> {

    public AssignmentSqlTransformer(
            SqlTransformerSupport transformerSupport, QAssignmentMapping mapping) {
        super(transformerSupport, mapping);
    }

    public MAssignment toRowObject(AssignmentType schemaObject, MObject ownerRow) {
        MAssignment row = super.toRowObject(schemaObject);

        row.ownerOid = ownerRow.oid;
        row.ownerType = ownerRow.objectType;
        // TODO add other fields here

        return row;
    }
}
