/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import java.io.Serial;
import java.sql.Types;

/**
 * Querydsl query type for affected object reference tables.
 */
public class QAffectedObjectReference extends QReference<MAffectedObjectReference, MAffectedObjects> {

    @Serial private static final long serialVersionUID = 3046837007769017218L;

    public static final ColumnMetadata AFFECTED_OBJECT_CID =
            ColumnMetadata.named("affectedObjectCid").ofType(Types.BIGINT).notNull();

    public final NumberPath<Long> affectedObjectCid = createLong("affectedObjectCid", AFFECTED_OBJECT_CID);


    public final PrimaryKey<MAffectedObjectReference> pk =
            createPrimaryKey(ownerOid, referenceType, relationId, targetOid);

    public QAffectedObjectReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QAffectedObjectReference(String variable, String schema, String table) {
        super(MAffectedObjectReference.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MAffectedObjects ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
                .and(affectedObjectCid.eq(ownerRow.cid));
    }
}
