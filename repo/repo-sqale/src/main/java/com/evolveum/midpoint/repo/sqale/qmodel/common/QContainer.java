/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QContainer<T extends MContainer> extends FlexibleRelationalPathBase<T> {

    private static final long serialVersionUID = 1033484385814003347L;

    /** If {@code QContainer.class} is not enough because of generics, try {@code QContainer.CLASS}. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QContainer<MContainer>> CLASS = (Class) QContainer.class;

    public static final String TABLE_NAME = "m_container";

    public static final ColumnMetadata OWNER_OID =
            ColumnMetadata.named("owner_oid").ofType(UuidPath.UUID_TYPE).notNull();
    public static final ColumnMetadata CID =
            ColumnMetadata.named("cid").ofType(Types.INTEGER).notNull();

    // columns and relations
    public final UuidPath ownerOid = createUuid("ownerOid", OWNER_OID);
    public final NumberPath<Integer> cid = createInteger("cid", CID);

    public final PrimaryKey<T> pk = createPrimaryKey(ownerOid, cid);

    public final ForeignKey<QObject<?>> objectOidIdFk =
            createForeignKey(ownerOid, QObject.OID.getName());

    public QContainer(Class<T> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QContainer(Class<? extends T> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }
}
