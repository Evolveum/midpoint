/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import java.sql.Types;

/**
 * Querydsl query type for assignment reference tables (for assignment metadata references).
 */
public class QAssignmentMarkReference extends QReference<MAssignmentMark, MAssignment> {

    private static final long serialVersionUID = 3046837007769017219L;

    public static final ColumnMetadata ASSIGNMENT_CID =
            ColumnMetadata.named("assignmentCid").ofType(Types.BIGINT).notNull();

    public final NumberPath<Long> assignmentCid = createLong("assignmentCid", ASSIGNMENT_CID);

    public final PrimaryKey<MAssignmentMark> pk =
            createPrimaryKey(ownerOid, assignmentCid, referenceType, relationId, targetOid);

    public QAssignmentMarkReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QAssignmentMarkReference(String variable, String schema, String table) {
        super(MAssignmentMark.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MAssignment ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
//                .and(ownerType.eq(ownerRow.ownerType)) //TODO later?
                .and(assignmentCid.eq(ownerRow.cid));
    }
}
