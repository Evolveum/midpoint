/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import java.sql.Types;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QShadowPartitionRef extends FlexibleRelationalPathBase<MShadowPartitionDef> {

    private static final long serialVersionUID = -8704333735247282997L;

    public static final String TABLE_NAME = "m_shadow_partition_def";

    public static final ColumnMetadata OBJECT_CLASS_ID =
            ColumnMetadata.named("objectClassId").ofType(Types.INTEGER);
    public static final ColumnMetadata RESOURCE_OID =
            ColumnMetadata.named("resourceOid").ofType(UuidPath.UUID_TYPE);

    public static final ColumnMetadata PARTITION = ColumnMetadata.named("partition").ofType(Types.BOOLEAN);
    public static final ColumnMetadata ATTACHED = ColumnMetadata.named("attached").ofType(Types.BOOLEAN);

    public static final ColumnMetadata TABLE = ColumnMetadata.named("table").ofType(Types.VARCHAR);

    // columns and relations

    public QShadowPartitionRef(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QShadowPartitionRef(String variable, String schema, String table) {
        super(MShadowPartitionDef.class, variable, schema, table);
    }

    public final NumberPath<Integer> objectClassId =
            createInteger("objectClassId", OBJECT_CLASS_ID);
    public final UuidPath resourceOid =
            createUuid("resourceOid", RESOURCE_OID);
    public final BooleanPath partition = createBoolean("partition", PARTITION);
    public final BooleanPath attached = createBoolean("attached", ATTACHED);
    public final StringPath table = createString("table", TABLE);

}
