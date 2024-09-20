/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;

/**
 * Querydsl query type for assignment reference tables (for assignment metadata references).
 */
public class QAssignmentReference<O extends MAssignmentReference.Owner> extends QReference<MAssignmentReference, O> {

    private static final long serialVersionUID = 3046837007769017219L;

    public static final ColumnMetadata ASSIGNMENT_CID =
            ColumnMetadata.named("assignmentCid").ofType(Types.BIGINT).notNull();

    public static final ColumnMetadata METADATA_CID =
            ColumnMetadata.named("metadataCid").ofType(Types.BIGINT);

    public final NumberPath<Long> assignmentCid = createLong("assignmentCid", ASSIGNMENT_CID);
    public final NumberPath<Long> metadataCid = createLong("metadataCid", METADATA_CID);


    public final PrimaryKey<MAssignmentReference> pk =
            createPrimaryKey(ownerOid, assignmentCid, referenceType, relationId, targetOid);

    public QAssignmentReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QAssignmentReference(String variable, String schema, String table) {
        super(MAssignmentReference.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MAssignmentReference.Owner ownerRow) {
        return ownerRow.owns(this);
    }
}
