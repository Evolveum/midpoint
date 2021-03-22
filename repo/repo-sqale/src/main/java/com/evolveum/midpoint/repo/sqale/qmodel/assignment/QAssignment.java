/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import static com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath.JSONB_TYPE;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.JsonbPath;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 * TODO: split to supertype for m_assignment_type and add QInducement too
 */
@SuppressWarnings("unused")
public class QAssignment extends QContainer<MAssignment> {

    private static final long serialVersionUID = 7068031681581618788L;

    public static final String TABLE_NAME = "m_assignment";

    public static final ColumnMetadata OWNER_TYPE =
            ColumnMetadata.named("owner_type").ofType(Types.OTHER);
    public static final ColumnMetadata LIFECYCLE_STATE =
            ColumnMetadata.named("lifecycleState").ofType(Types.VARCHAR);
    public static final ColumnMetadata ORDER_VALUE =
            ColumnMetadata.named("orderValue").ofType(Types.INTEGER);
    public static final ColumnMetadata ORG_REF_TARGET_OID =
            ColumnMetadata.named("orgRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ORG_REF_TARGET_TYPE =
            ColumnMetadata.named("orgRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata ORG_REF_RELATION_ID =
            ColumnMetadata.named("orgRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata TARGET_REF_TARGET_OID =
            ColumnMetadata.named("targetRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TARGET_REF_TARGET_TYPE =
            ColumnMetadata.named("targetRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata TARGET_REF_RELATION_ID =
            ColumnMetadata.named("targetRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata TENANT_REF_TARGET_OID =
            ColumnMetadata.named("tenantRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TENANT_REF_TARGET_TYPE =
            ColumnMetadata.named("tenantRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata TENANT_REF_RELATION_ID =
            ColumnMetadata.named("tenantRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata EXT_ID =
            ColumnMetadata.named("extId").ofType(Types.INTEGER);
    // TODO UUID or not? our control or outside?
    public static final ColumnMetadata EXT_OID =
            ColumnMetadata.named("extOid").ofType(Types.VARCHAR);
    public static final ColumnMetadata POLICY_SITUATIONS =
            ColumnMetadata.named("policySituations").ofType(Types.ARRAY);
    public static final ColumnMetadata EXT = ColumnMetadata.named("ext").ofType(JSONB_TYPE);
    // construction columns
    public static final ColumnMetadata RESOURCE_REF_TARGET_OID =
            ColumnMetadata.named("resourceRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata RESOURCE_REF_TARGET_TYPE =
            ColumnMetadata.named("resourceRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata RESOURCE_REF_RELATION_ID =
            ColumnMetadata.named("resourceRef_relation_id").ofType(Types.INTEGER);
    // activation columns
    public static final ColumnMetadata ADMINISTRATIVE_STATUS =
            ColumnMetadata.named("administrativeStatus").ofType(Types.OTHER);
    public static final ColumnMetadata EFFECTIVE_STATUS =
            ColumnMetadata.named("effectiveStatus").ofType(Types.OTHER);
    public static final ColumnMetadata ENABLE_TIMESTAMP =
            ColumnMetadata.named("enableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DISABLE_TIMESTAMP =
            ColumnMetadata.named("disableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DISABLE_REASON =
            ColumnMetadata.named("disableReason").ofType(Types.VARCHAR);
    public static final ColumnMetadata VALIDITY_STATUS =
            ColumnMetadata.named("validityStatus").ofType(Types.OTHER);
    public static final ColumnMetadata VALID_FROM =
            ColumnMetadata.named("validFrom").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALID_TO =
            ColumnMetadata.named("validTo").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALIDITY_CHANGE_TIMESTAMP =
            ColumnMetadata.named("validityChangeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata ARCHIVE_TIMESTAMP =
            ColumnMetadata.named("archiveTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
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

    // attributes

    public final EnumPath<MObjectType> ownerType =
            createEnum("ownerType", MObjectType.class, OWNER_TYPE);
    public final StringPath lifecycleState = createString("lifecycleState", LIFECYCLE_STATE);
    public final NumberPath<Integer> orderValue = createInteger("orderValue", ORDER_VALUE);
    public final UuidPath orgRefTargetOid =
            createUuid("orgRefTargetOid", ORG_REF_TARGET_OID);
    public final EnumPath<MObjectType> orgRefTargetType =
            createEnum("orgRefTargetType", MObjectType.class, ORG_REF_TARGET_TYPE);
    public final NumberPath<Integer> orgRefRelationId =
            createInteger("orgRefRelationId", ORG_REF_RELATION_ID);
    public final UuidPath targetRefTargetOid =
            createUuid("targetRefTargetOid", TARGET_REF_TARGET_OID);
    public final EnumPath<MObjectType> targetRefTargetType =
            createEnum("targetRefTargetType", MObjectType.class, TARGET_REF_TARGET_TYPE);
    public final NumberPath<Integer> targetRefRelationId =
            createInteger("targetRefRelationId", TARGET_REF_RELATION_ID);
    public final UuidPath tenantRefTargetOid =
            createUuid("tenantRefTargetOid", TENANT_REF_TARGET_OID);
    public final EnumPath<MObjectType> tenantRefTargetType =
            createEnum("tenantRefTargetType", MObjectType.class, TENANT_REF_TARGET_TYPE);
    public final NumberPath<Integer> tenantRefRelationId =
            createInteger("tenantRefRelationId", TENANT_REF_RELATION_ID);
    public final NumberPath<Integer> extId = createInteger("extId", EXT_ID);
    public final StringPath extOid = createString("extOid", EXT_OID);
    public final ArrayPath<Integer[], Integer> policySituations =
            createArray("policySituations", Integer[].class, POLICY_SITUATIONS);
    public final JsonbPath ext = createJsonb("ext", EXT);
    // construction attributes
    public final UuidPath resourceRefTargetOid =
            createUuid("resourceRefTargetOid", RESOURCE_REF_TARGET_OID);
    public final EnumPath<MObjectType> resourceRefTargetType =
            createEnum("resourceRefTargetType", MObjectType.class, RESOURCE_REF_TARGET_TYPE);
    public final NumberPath<Integer> resourceRefRelationId =
            createInteger("resourceRefRelationId", RESOURCE_REF_RELATION_ID);
    // activation attributes
    public final EnumPath<ActivationStatusType> administrativeStatus =
            createEnum("administrativeStatus", ActivationStatusType.class, ADMINISTRATIVE_STATUS);
    public final EnumPath<ActivationStatusType> effectiveStatus =
            createEnum("effectiveStatus", ActivationStatusType.class, EFFECTIVE_STATUS);
    public final DateTimePath<Instant> enableTimestamp =
            createInstant("enableTimestamp", ENABLE_TIMESTAMP);
    public final DateTimePath<Instant> disableTimestamp =
            createInstant("disableTimestamp", DISABLE_TIMESTAMP);
    public final StringPath disableReason = createString("disableReason", DISABLE_REASON);
    public final EnumPath<TimeIntervalStatusType> validityStatus =
            createEnum("validityStatus", TimeIntervalStatusType.class, VALIDITY_STATUS);
    public final DateTimePath<Instant> validFrom = createInstant("validFrom", VALID_FROM);
    public final DateTimePath<Instant> validTo = createInstant("validTo", VALID_TO);
    public final DateTimePath<Instant> validityChangeTimestamp =
            createInstant("validityChangeTimestamp", VALIDITY_CHANGE_TIMESTAMP);
    public final DateTimePath<Instant> archiveTimestamp =
            createInstant("archiveTimestamp", ARCHIVE_TIMESTAMP);
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

    public QAssignment(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAssignment(String variable, String schema, String table) {
        super(MAssignment.class, variable, schema, table);
    }
}
