/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAssignment extends FlexibleRelationalPathBase<MAssignment> {

    private static final long serialVersionUID = 7068031681581618788L;

    public static final String TABLE_NAME = "m_assignment";

    public static final ColumnMetadata OWNER_OID =
            ColumnMetadata.named("owner_oid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ID =
            ColumnMetadata.named("cid").ofType(Types.INTEGER);

    // TODO the rest

    public UuidPath ownerOid = createUuid("ownerOid", OWNER_OID);
    public NumberPath<Integer> cid = createInteger("cid", ID);

    public QAssignment(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAssignment(String variable, String schema, String table) {
        super(MAssignment.class, variable, schema, table);
    }
}
