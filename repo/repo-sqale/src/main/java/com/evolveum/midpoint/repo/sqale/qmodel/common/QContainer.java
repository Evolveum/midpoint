/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qmodel.QOwnedBy;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 *
 * @param <R> type of this container row
 * @param <OR> type of the immediate owner row (can be object or container)
 */
@SuppressWarnings("unused")
public class QContainer<R extends MContainer, OR> extends FlexibleRelationalPathBase<R>
        implements QOwnedBy<OR> {

    private static final long serialVersionUID = 1033484385814003347L;

    /** If {@code QContainer.class} is not enough because of generics, try {@code QContainer.CLASS}. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QContainer<MContainer, Object>> CLASS = (Class) QContainer.class;

    public static final String TABLE_NAME = "m_container";

    public static final ColumnMetadata OWNER_OID =
            ColumnMetadata.named("ownerOid").ofType(UuidPath.UUID_TYPE).notNull();
    public static final ColumnMetadata CID =
            ColumnMetadata.named("cid").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata CONTAINER_TYPE =
            ColumnMetadata.named("containerType").ofType(Types.OTHER);

    // columns and relations
    public final UuidPath ownerOid = createUuid("ownerOid", OWNER_OID);
    public final NumberPath<Long> cid = createLong("cid", CID);
    public final EnumPath<MContainerType> containerType =
            createEnum("containerType", MContainerType.class, CONTAINER_TYPE);

    public final PrimaryKey<R> pk = createPrimaryKey(ownerOid, cid);

    public final ForeignKey<QObject<?>> objectOidIdFk =
            createForeignKey(ownerOid, QObject.OID.getName());

    public QContainer(Class<R> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QContainer(Class<? extends R> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) {
        throw new UnsupportedOperationException(
                "isOwnedBy not supported for abstract container table");
    }
}
