/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.UUID;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;

public class ReferenceSqlTransformer<Q extends QReference<R>, R extends MReference, OR>
        extends SqaleTransformerBase<Referencable, Q, R> {

    private final QReferenceMapping<Q, R, ?, OR> mapping;

    public ReferenceSqlTransformer(
            SqlTransformerSupport transformerSupport, QReferenceMapping<Q, R, ?, OR> mapping) {
        super(transformerSupport, mapping);
        this.mapping = mapping;
    }

    /**
     * There is no need to override this, only reference creation is different and that is covered
     * by {@link QReferenceMapping#newRowObject(Object)} including setting FK columns.
     * All the other columns are based on a single schema type, so there is no variation.
     */
    public void insert(Referencable schemaObject, OR ownerRow, JdbcSession jdbcSession) {
        R row = mapping.newRowObject(ownerRow);
        // row.referenceType is DB generated, must be kept NULL, but it will match referenceType
        row.relationId = processCacheableRelation(schemaObject.getRelation());
        row.targetOid = UUID.fromString(schemaObject.getOid());
        row.targetType = schemaTypeToObjectType(schemaObject.getType());

        insert(row, jdbcSession);
    }
}
