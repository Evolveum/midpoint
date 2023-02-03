/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.sqale.qmodel.QOwnedBy;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.mapping.SqlOrderableExpression;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table that contains all persisted object references.
 * This actually points to super-table, concrete tables are partitioned by {@link MReferenceType}.
 *
 * @param <R> type of the reference row
 * @param <OR> type of the owner row
 */
public class QReference<R extends MReference, OR> extends FlexibleRelationalPathBase<R>
        implements QOwnedBy<OR>, SqlOrderableExpression {

    private static final long serialVersionUID = -466419569179455042L;

    /** If {@code QReference.class} is not enough because of generics, try {@code QReference.CLASS}. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QReference<MReference, Object>> CLASS = (Class) QReference.class;

    public static final String TABLE_NAME = "m_reference";

    public static final ColumnMetadata OWNER_OID =
            ColumnMetadata.named("ownerOid").ofType(UuidPath.UUID_TYPE).notNull();
    public static final ColumnMetadata OWNER_TYPE =
            ColumnMetadata.named("ownerType").ofType(Types.OTHER);
    public static final ColumnMetadata REFERENCE_TYPE =
            ColumnMetadata.named("referenceType").ofType(Types.OTHER).notNull();
    public static final ColumnMetadata TARGET_OID =
            ColumnMetadata.named("targetOid").ofType(UuidPath.UUID_TYPE).notNull();
    public static final ColumnMetadata TARGET_TYPE =
            ColumnMetadata.named("targetType").ofType(Types.OTHER).notNull();
    public static final ColumnMetadata RELATION_ID =
            ColumnMetadata.named("relationId").ofType(Types.INTEGER).notNull();

    public final UuidPath ownerOid = createUuid("ownerOid", OWNER_OID);
    public final EnumPath<MObjectType> ownerType =
            createEnum("ownerType", MObjectType.class, OWNER_TYPE);
    public final EnumPath<MReferenceType> referenceType =
            createEnum("referenceType", MReferenceType.class, REFERENCE_TYPE);
    public final UuidPath targetOid = createUuid("targetOid", TARGET_OID);
    public final EnumPath<MObjectType> targetType =
            createEnum("targetType", MObjectType.class, TARGET_TYPE);
    public final NumberPath<Integer> relationId = createInteger("relationId", RELATION_ID);

    public final PrimaryKey<R> pk = createPrimaryKey(ownerOid, relationId, targetOid);

    public QReference(Class<R> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QReference(Class<R> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) {
        throw new UnsupportedOperationException(
                "isOwnedBy not supported for abstract reference table");
    }

    /**
     * Special case ordering used to paginate iterative reference search strictly sequentially.
     */
    @Override
    public void orderBy(SQLQuery<?> sqlQuery, ObjectOrdering ordering) {
        if (ordering.getDirection() == OrderDirection.DESCENDING) {
            sqlQuery.orderBy(ownerOid.desc(), relationId.desc(), targetOid.desc());
        } else {
            sqlQuery.orderBy(ownerOid.asc(), relationId.asc(), targetOid.asc());
        }
    }
}
