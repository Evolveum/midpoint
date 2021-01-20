/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Types;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qbean.MObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Querydsl query type for M_OBJECT table.
 */
@SuppressWarnings("unused")
public abstract class QObject<T extends MObject> extends FlexibleRelationalPathBase<T> {

    private static final long serialVersionUID = -4174420892574422778L;

    /** If {@code QObject.class} is not enough because of generics, try {@code QObject.CLASS}. */
    public static final Class<? extends QObject<MObject>> CLASS = QObjectReal.class;

    public static final String TABLE_NAME = "m_object";

    public static final ColumnMetadata OID =
            // TODO how to represent UUID type better? There is no constant for it.
            ColumnMetadata.named("oid").ofType(Types.VARCHAR).withSize(36).notNull();
    public static final ColumnMetadata EVENT_TYPE =
            ColumnMetadata.named("objectTypeClass").ofType(Types.INTEGER).withSize(10).notNull();
    public static final ColumnMetadata NAME_NORM =
            ColumnMetadata.named("name_norm").ofType(Types.VARCHAR).withSize(255).notNull();
    public static final ColumnMetadata NAME_ORIG =
            ColumnMetadata.named("name_orig").ofType(Types.VARCHAR).withSize(255).notNull();
    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);
    public static final ColumnMetadata CREATOR_REF_RELATION =
            ColumnMetadata.named("creatorRef_relation").ofType(Types.VARCHAR).withSize(157);
    public static final ColumnMetadata CREATOR_REF_TARGET_OID =
            ColumnMetadata.named("creatorRef_targetOid").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata CREATOR_REF_TARGET_TYPE =
            ColumnMetadata.named("creatorRef_targetType").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata CREATE_CHANNEL_ID =
            ColumnMetadata.named("createChannel_id").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata CREATE_TIMESTAMP =
            ColumnMetadata.named("createTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE).notNull();
    public static final ColumnMetadata MODIFIER_REF_RELATION =
            ColumnMetadata.named("modifierRef_relation").ofType(Types.VARCHAR).withSize(157);
    public static final ColumnMetadata MODIFIER_REF_TARGET_OID =
            ColumnMetadata.named("modifierRef_targetOid").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata MODIFIER_REF_TARGET_TYPE =
            ColumnMetadata.named("modifierRef_targetType").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata MODIFY_CHANNEL_ID =
            ColumnMetadata.named("modifyChannel_id").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata MODIFY_TIMESTAMP =
            ColumnMetadata.named("modifyTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE).notNull();
    public static final ColumnMetadata TENANT_REF_RELATION =
            ColumnMetadata.named("tenantRef_relation").ofType(Types.VARCHAR).withSize(157);
    public static final ColumnMetadata TENANT_REF_TARGET_OID =
            ColumnMetadata.named("tenantRef_targetOid").ofType(Types.VARCHAR).withSize(36);
    public static final ColumnMetadata TENANT_REF_TARGET_TYPE =
            ColumnMetadata.named("tenantRef_targetType").ofType(Types.INTEGER).withSize(10);
    public static final ColumnMetadata LIFECYCLE_STATE =
            ColumnMetadata.named("lifecycleState").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata VERSION =
            ColumnMetadata.named("version").ofType(Types.INTEGER).withSize(10).notNull();
    public static final ColumnMetadata EXT =
            ColumnMetadata.named("ext").ofType(Types.BINARY);

    // columns and relations
    public final StringPath oid = createString("oid", OID);
    public final StringPath nameNorm = createString("nameNorm", NAME_NORM);
    public final StringPath nameOrig = createString("nameOrig", NAME_ORIG);
    public final ArrayPath<byte[], Byte> fullObject = createBlob("fullObject", FULL_OBJECT);
    public final NumberPath<Integer> createChannelId = createInteger("createChannelId", CREATE_CHANNEL_ID);
    // TODO other columns

    public final PrimaryKey<T> pk = createPrimaryKey(oid);
    public final ForeignKey<QQName> qNameFk = createForeignKey(createChannelId, QQName.ID.getName());

    public QObject(Class<? extends T> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QObject(Class<? extends T> type, String variable, String schema, String table) {
        super(type, forVariable(variable), schema, table);
    }

    /**
     * Class representing generic {@code QClass<MObject>.class} which is otherwise impossible.
     * There should be no need to instantiate this, so the class is private and final.
     */
    private static final class QObjectReal extends QObject<MObject> {
        private QObjectReal(String variable) {
            super(MObject.class, variable);
        }
    }
}
