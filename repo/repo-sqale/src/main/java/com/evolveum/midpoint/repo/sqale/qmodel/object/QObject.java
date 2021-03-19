/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath.JSONB_TYPE;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.ForeignKey;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QUri;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QObject<T extends MObject> extends FlexibleRelationalPathBase<T> {

    private static final long serialVersionUID = -4174420892574422778L;

    /** If {@code QObject.class} is not enough because of generics, try {@code QObject.CLASS}. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QObject<MObject>> CLASS = (Class) QObject.class;

    public static final String TABLE_NAME = "m_object";

    public static final ColumnMetadata OID =
            ColumnMetadata.named("oid").ofType(UuidPath.UUID_TYPE).notNull();
    public static final ColumnMetadata OBJECT_TYPE =
            ColumnMetadata.named("objectType").ofType(Types.OTHER).notNull();
    public static final ColumnMetadata NAME_ORIG =
            ColumnMetadata.named("name_orig").ofType(Types.VARCHAR).withSize(255).notNull();
    public static final ColumnMetadata NAME_NORM =
            ColumnMetadata.named("name_norm").ofType(Types.VARCHAR).withSize(255).notNull();
    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);
    public static final ColumnMetadata TENANT_REF_TARGET_OID =
            ColumnMetadata.named("tenantRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TENANT_REF_TARGET_TYPE =
            ColumnMetadata.named("tenantRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata TENANT_REF_RELATION_ID =
            ColumnMetadata.named("tenantRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata LIFECYCLE_STATE =
            ColumnMetadata.named("lifecycleState").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata CID_SEQ =
            ColumnMetadata.named("cid_seq").ofType(Types.BIGINT).notNull();
    public static final ColumnMetadata VERSION =
            ColumnMetadata.named("version").ofType(Types.INTEGER).notNull();
    public static final ColumnMetadata POLICY_SITUATIONS =
            ColumnMetadata.named("policySituations").ofType(Types.ARRAY);
    public static final ColumnMetadata SUBTYPES =
            ColumnMetadata.named("subtypes").ofType(Types.ARRAY);
    public static final ColumnMetadata EXT = ColumnMetadata.named("ext").ofType(JSONB_TYPE);
    // metadata columns
    public static final ColumnMetadata CREATOR_REF_TARGET_OID =
            ColumnMetadata.named("creatorRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata CREATOR_REF_TARGET_TYPE =
            ColumnMetadata.named("creatorRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata CREATOR_REF_RELATION_ID =
            ColumnMetadata.named("creatorRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata CREATE_CHANNEL_ID =
            ColumnMetadata.named("createChannel_id").ofType(Types.INTEGER);
    public static final ColumnMetadata CREATE_TIMESTAMP =
            ColumnMetadata.named("createTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata MODIFIER_REF_TARGET_OID =
            ColumnMetadata.named("modifierRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata MODIFIER_REF_TARGET_TYPE =
            ColumnMetadata.named("modifierRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata MODIFIER_REF_RELATION_ID =
            ColumnMetadata.named("modifierRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata MODIFY_CHANNEL_ID =
            ColumnMetadata.named("modifyChannel_id").ofType(Types.INTEGER);
    public static final ColumnMetadata MODIFY_TIMESTAMP =
            ColumnMetadata.named("modifyTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    // columns and relations
    public final UuidPath oid = createUuid("oid", OID);
    public final EnumPath<MObjectType> objectType =
            createEnum("objectType", MObjectType.class, OBJECT_TYPE);
    public final StringPath nameOrig = createString("nameOrig", NAME_ORIG);
    public final StringPath nameNorm = createString("nameNorm", NAME_NORM);
    public final ArrayPath<byte[], Byte> fullObject = createByteArray("fullObject", FULL_OBJECT);
    public final UuidPath tenantRefTargetOid =
            createUuid("tenantRefTargetOid", TENANT_REF_TARGET_OID);
    public final EnumPath<MObjectType> tenantRefTargetType =
            createEnum("tenantRefTargetType", MObjectType.class, TENANT_REF_TARGET_TYPE);
    public final NumberPath<Integer> tenantRefRelationId =
            createInteger("tenantRefRelationId", TENANT_REF_RELATION_ID);
    public final StringPath lifecycleState = createString("lifecycleState", LIFECYCLE_STATE);
    public final NumberPath<Long> containerIdSeq = createLong("containerIdSeq", CID_SEQ);
    public final NumberPath<Integer> version = createInteger("version", VERSION);
    public final ArrayPath<Integer[], Integer> policySituations =
            createArray("policySituations", Integer[].class, POLICY_SITUATIONS);
    public final ArrayPath<String[], String> subtypes =
            createArray("subtypes", String[].class, SUBTYPES);
    public final JsonbPath ext = createJsonb("ext", EXT);
    // metadata attributes
    public final UuidPath creatorRefTargetOid =
            createUuid("creatorRefTargetOid", CREATOR_REF_TARGET_OID);
    public final EnumPath<MObjectType> creatorRefTargetType =
            createEnum("creatorRefTargetType", MObjectType.class, CREATOR_REF_TARGET_TYPE);
    public final NumberPath<Integer> creatorRefRelationId =
            createInteger("creatorRefRelationId", CREATOR_REF_RELATION_ID);
    public final NumberPath<Integer> createChannelId =
            createInteger("createChannelId", CREATE_CHANNEL_ID);
    public final DateTimePath<Instant> createTimestamp =
            createInstant("createTimestamp", CREATE_TIMESTAMP);
    public final UuidPath modifierRefTargetOid =
            createUuid("modifierRefTargetOid", MODIFIER_REF_TARGET_OID);
    public final EnumPath<MObjectType> modifierRefTargetType =
            createEnum("modifierRefTargetType", MObjectType.class, MODIFIER_REF_TARGET_TYPE);
    public final NumberPath<Integer> modifierRefRelationId =
            createInteger("modifierRefRelationId", MODIFIER_REF_RELATION_ID);
    public final NumberPath<Integer> modifyChannelId =
            createInteger("modifyChannelId", MODIFY_CHANNEL_ID);
    public final DateTimePath<Instant> modifyTimestamp =
            createInstant("modifyTimestamp", MODIFY_TIMESTAMP);

    public final PrimaryKey<T> pk = createPrimaryKey(oid);
    public final ForeignKey<QUri> createChannelIdFk =
            createForeignKey(createChannelId, QUri.ID.getName());
    public final ForeignKey<QUri> modifyChannelIdFk =
            createForeignKey(modifyChannelId, QUri.ID.getName());
    public final ForeignKey<QUri> creatorRefRelationIdFk =
            createForeignKey(creatorRefRelationId, QUri.ID.getName());
    public final ForeignKey<QUri> modifierRefRelationIdFk =
            createForeignKey(modifierRefRelationId, QUri.ID.getName());
    public final ForeignKey<QUri> tenantRefRelationIdFk =
            createForeignKey(tenantRefRelationId, QUri.ID.getName());

    public QObject(Class<T> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QObject(Class<T> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }
}
