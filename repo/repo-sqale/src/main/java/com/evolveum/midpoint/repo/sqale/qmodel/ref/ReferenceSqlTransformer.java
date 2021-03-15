/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class ReferenceSqlTransformer<Q extends QReference<R>, R extends MReference>
        extends SqaleTransformerBase<ObjectReferenceType, Q, R> {

    public ReferenceSqlTransformer(
            SqlTransformerSupport transformerSupport, QReferenceMapping<Q, R> mapping) {
        super(transformerSupport, mapping);
    }

    /**
     * There is no need to override this as the {@link MReferenceOwner#createReference()} takes
     * care of the FK-columns initialization directly on the owning row object.
     * All the other columns are based on a single schema type, so there is no variation.
     */
    public void insert(ObjectReferenceType schemaObject,
            MReferenceOwner<R> ownerRow, JdbcSession jdbcSession) {
        R row = ownerRow.createReference();
        // row.referenceType is DB generated, must be kept NULL, but it will match referenceType
        row.relationId = processCacheableRelation(schemaObject.getRelation(), jdbcSession);
        row.targetOid = UUID.fromString(schemaObject.getOid());
        row.targetType = schemaTypeToObjectType(schemaObject.getType());

        insert(row, jdbcSession);
    }
}
