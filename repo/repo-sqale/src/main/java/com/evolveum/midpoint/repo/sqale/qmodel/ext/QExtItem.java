/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ext;

import java.sql.Types;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for {@value #TABLE_NAME} table with the catalog of indexed extension items.
 * This entity is not registered to any schema type so it doesn't have related mapping class.
 * Use {@link #DEFAULT} for default alias directly.
 */
public class QExtItem extends FlexibleRelationalPathBase<MExtItem> {

    private static final long serialVersionUID = -1519824042438215508L;

    public static final String TABLE_NAME = "m_ext_item";

    public static final QExtItem DEFAULT = new QExtItem("ei");

    public static final ColumnMetadata ID =
            ColumnMetadata.named("id").ofType(Types.INTEGER).notNull();
    public static final ColumnMetadata ITEM_NAME =
            ColumnMetadata.named("itemName").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata VALUE_TYPE =
            ColumnMetadata.named("valueType").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata HOLDER_TYPE =
            ColumnMetadata.named("holderType").ofType(Types.OTHER).notNull();
    public static final ColumnMetadata CARDINALITY =
            ColumnMetadata.named("cardinality").ofType(Types.OTHER).notNull();

    public final NumberPath<Integer> id = createInteger("id", ID);
    public final StringPath itemName = createString("itemName", ITEM_NAME);
    public final StringPath valueType = createString("valueType", VALUE_TYPE);
    public final EnumPath<MExtItemHolderType> holderType =
            createEnum("holderType", MExtItemHolderType.class, HOLDER_TYPE);
    public final EnumPath<MExtItemCardinality> cardinality =
            createEnum("cardinality", MExtItemCardinality.class, CARDINALITY);

    public final PrimaryKey<MExtItem> pk = createPrimaryKey(id);

    public QExtItem(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QExtItem(String variable, String schema, String table) {
        super(MExtItem.class, variable, schema, table);
    }
}
