/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qbean.MObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QObject<T extends MObject> extends FlexibleRelationalPathBase<T> {

    /** Alias for {@link Types#OTHER} working in tandem with {@link UuidPath}. */
    public static final int UUID_TYPE = Types.OTHER;

    private static final long serialVersionUID = -4174420892574422778L;

    /** If {@code QObject.class} is not enough because of generics, try {@code QObject.CLASS}. */
    public static final Class<? extends QObject<MObject>> CLASS = QObjectReal.class;

    public static final String TABLE_NAME = "m_object";

    public static final ColumnMetadata OID =
            ColumnMetadata.named("oid").ofType(UUID_TYPE).notNull();
    public static final ColumnMetadata EVENT_TYPE =
            ColumnMetadata.named("objectTypeClass").ofType(Types.INTEGER).notNull();
    public static final ColumnMetadata NAME_NORM =
            ColumnMetadata.named("name_norm").ofType(Types.VARCHAR).withSize(255).notNull();
    public static final ColumnMetadata NAME_ORIG =
            ColumnMetadata.named("name_orig").ofType(Types.VARCHAR).withSize(255).notNull();
    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);
    public static final ColumnMetadata CREATOR_REF_RELATION_ID =
            ColumnMetadata.named("creatorRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata CREATOR_REF_TARGET_OID =
            ColumnMetadata.named("creatorRef_targetOid").ofType(UUID_TYPE);
    public static final ColumnMetadata CREATOR_REF_TARGET_TYPE =
            ColumnMetadata.named("creatorRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata CREATE_CHANNEL_ID =
            ColumnMetadata.named("createChannel_id").ofType(Types.INTEGER);
    public static final ColumnMetadata CREATE_TIMESTAMP =
            ColumnMetadata.named("createTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata MODIFIER_REF_RELATION_ID =
            ColumnMetadata.named("modifierRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata MODIFIER_REF_TARGET_OID =
            ColumnMetadata.named("modifierRef_targetOid").ofType(UUID_TYPE);
    public static final ColumnMetadata MODIFIER_REF_TARGET_TYPE =
            ColumnMetadata.named("modifierRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata MODIFY_CHANNEL_ID =
            ColumnMetadata.named("modifyChannel_id").ofType(Types.INTEGER);
    public static final ColumnMetadata MODIFY_TIMESTAMP =
            ColumnMetadata.named("modifyTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata TENANT_REF_RELATION_ID =
            ColumnMetadata.named("tenantRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata TENANT_REF_TARGET_OID =
            ColumnMetadata.named("tenantRef_targetOid").ofType(UUID_TYPE);
    public static final ColumnMetadata TENANT_REF_TARGET_TYPE =
            ColumnMetadata.named("tenantRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata LIFECYCLE_STATE =
            ColumnMetadata.named("lifecycleState").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata VERSION =
            ColumnMetadata.named("version").ofType(Types.INTEGER).notNull();
    public static final ColumnMetadata EXT =
            ColumnMetadata.named("ext").ofType(Types.BINARY);

    // columns and relations
    public final UuidPath oid = createUuid("oid", OID);
    public final StringPath nameNorm = createString("nameNorm", NAME_NORM);
    public final StringPath nameOrig = createString("nameOrig", NAME_ORIG);
    public final ArrayPath<byte[], Byte> fullObject = createByteArray("fullObject", FULL_OBJECT);
    public final NumberPath<Integer> creatorRefRelationId =
            createInteger("creatorRefRelationId", CREATOR_REF_RELATION_ID);
    public final UuidPath creatorRefTargetOid =
            createUuid("creatorRefTargetOid", CREATOR_REF_TARGET_OID);
    public final NumberPath<Integer> creatorRefTargetType =
            createInteger("creatorRefTargetType", CREATOR_REF_TARGET_TYPE);
    public final NumberPath<Integer> createChannelId =
            createInteger("createChannelId", CREATE_CHANNEL_ID);
    public final DateTimePath<Instant> createTimestamp =
            createInstant("createTimestamp", CREATE_TIMESTAMP);
    public final NumberPath<Integer> modifierRefRelationId =
            createInteger("modifierRefRelationId", MODIFIER_REF_RELATION_ID);
    public final UuidPath modifierRefTargetOid =
            createUuid("modifierRefTargetOid", MODIFIER_REF_TARGET_OID);
    public final NumberPath<Integer> modifierRefTargetType =
            createInteger("modifierRefTargetType", MODIFIER_REF_TARGET_TYPE);
    public final NumberPath<Integer> modifyChannelId =
            createInteger("modifyChannelId", MODIFY_CHANNEL_ID);
    public final DateTimePath<Instant> modifyTimestamp =
            createInstant("modifyTimestamp", MODIFY_TIMESTAMP);
    public final NumberPath<Integer> tenantRefRelationId =
            createInteger("tenantRefRelationId", TENANT_REF_RELATION_ID);
    public final UuidPath tenantRefTargetOid =
            createUuid("tenantRefTargetOid", TENANT_REF_TARGET_OID);
    public final NumberPath<Integer> tenantRefTargetType =
            createInteger("tenantRefTargetType", TENANT_REF_TARGET_TYPE);
    public final StringPath lifecycleState = createString("lifecycleState", LIFECYCLE_STATE);
    public final NumberPath<Integer> version = createInteger("version", VERSION);
    public final ArrayPath<byte[], Byte> ext = createByteArray("ext", EXT); // TODO is byte[] the right type?

    public final PrimaryKey<T> pk = createPrimaryKey(oid);
    public final ForeignKey<QQName> createChannelIdFk =
            createForeignKey(createChannelId, QQName.ID.getName());
    public final ForeignKey<QQName> modifyChannelIdFk =
            createForeignKey(modifyChannelId, QQName.ID.getName());
    public final ForeignKey<QQName> creatorRefRelationIdFk =
            createForeignKey(creatorRefRelationId, QQName.ID.getName());
    public final ForeignKey<QQName> modifierRefRelationIdFk =
            createForeignKey(modifierRefRelationId, QQName.ID.getName());
    public final ForeignKey<QQName> tenantRefRelationIdFk =
            createForeignKey(tenantRefRelationId, QQName.ID.getName());

    public QObject(Class<T> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QObject(Class<? extends T> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }

    /**
     * Class representing generic {@code QObject<MObject>.class} which is otherwise impossible.
     * There should be no need to instantiate this, so the class is private and final.
     */
    public static final class QObjectReal extends QObject<MObject> {
        public QObjectReal(String variable) {
            super(MObject.class, variable);
        }
    }
}
