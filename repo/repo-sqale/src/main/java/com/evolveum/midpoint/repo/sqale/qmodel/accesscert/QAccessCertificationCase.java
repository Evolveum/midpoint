/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAccessCertificationCase extends QContainer<MAccessCertificationCase, MAccessCertificationCampaign> {

    private static final long serialVersionUID = -7057010668203386506L;

    public static final String TABLE_NAME = "m_access_cert_case";

    // activation columns
    public static final ColumnMetadata ADMINISTRATIVE_STATUS =
            ColumnMetadata.named("administrativeStatus").ofType(Types.OTHER);
    public static final ColumnMetadata ARCHIVE_TIMESTAMP =
            ColumnMetadata.named("archiveTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DISABLE_REASON =
            ColumnMetadata.named("disableReason").ofType(Types.VARCHAR);
    public static final ColumnMetadata DISABLE_TIMESTAMP =
            ColumnMetadata.named("disableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata EFFECTIVE_STATUS =
            ColumnMetadata.named("effectiveStatus").ofType(Types.OTHER);
    public static final ColumnMetadata ENABLE_TIMESTAMP =
            ColumnMetadata.named("enableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALID_FROM =
            ColumnMetadata.named("validFrom").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALID_TO =
            ColumnMetadata.named("validTo").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALIDITY_CHANGE_TIMESTAMP =
            ColumnMetadata.named("validityChangeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALIDITY_STATUS =
            ColumnMetadata.named("validityStatus").ofType(Types.OTHER);
    public static final ColumnMetadata CURRENT_STAGE_OUTCOME =
            ColumnMetadata.named("currentStageOutcome").ofType(Types.VARCHAR);
    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);
    public static final ColumnMetadata CAMPAIGN_ITERATION =
            ColumnMetadata.named("campaignIteration").ofType(Types.INTEGER);
    public static final ColumnMetadata OBJECT_REF_TARGET_OID =
            ColumnMetadata.named("objectRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata OBJECT_REF_TARGET_TYPE =
            ColumnMetadata.named("objectRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata OBJECT_REF_RELATION_ID =
            ColumnMetadata.named("objectRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata ORG_REF_TARGET_OID =
            ColumnMetadata.named("orgRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ORG_REF_TARGET_TYPE =
            ColumnMetadata.named("orgRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata ORG_REF_RELATION_ID =
            ColumnMetadata.named("orgRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata OUTCOME =
            ColumnMetadata.named("outcome").ofType(Types.VARCHAR);
    public static final ColumnMetadata REMEDIED_TIMESTAMP =
            ColumnMetadata.named("remediedTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CURRENT_STAGE_DEADLINE =
            ColumnMetadata.named("currentStageDeadline").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CURRENT_STAGE_CREATE_TIMESTAMP =
            ColumnMetadata.named("currentStageCreateTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata STAGE_NUMBER =
            ColumnMetadata.named("stageNumber").ofType(Types.INTEGER);
    public static final ColumnMetadata TARGET_REF_TARGET_OID =
            ColumnMetadata.named("targetRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TARGET_REF_TARGET_TYPE =
            ColumnMetadata.named("targetRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata TARGET_REF_RELATION_ID =
            ColumnMetadata.named("targetRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata TENANT_REF_TARGET_OID =
            ColumnMetadata.named("tenantRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TENANT_REF_TARGET_TYPE =
            ColumnMetadata.named("tenantRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata TENANT_REF_RELATION_ID =
            ColumnMetadata.named("tenantRefRelationId").ofType(Types.INTEGER);

    // attributes

    // activation
    public final EnumPath<ActivationStatusType> administrativeStatus =
            createEnum("administrativeStatus", ActivationStatusType.class, ADMINISTRATIVE_STATUS);
    public final DateTimePath<Instant> archiveTimestamp =
            createInstant("archiveTimestamp", ARCHIVE_TIMESTAMP);
    public final StringPath disableReason = createString("disableReason", DISABLE_REASON);
    public final DateTimePath<Instant> disableTimestamp =
            createInstant("disableTimestamp", DISABLE_TIMESTAMP);
    public final EnumPath<ActivationStatusType> effectiveStatus =
            createEnum("effectiveStatus", ActivationStatusType.class, EFFECTIVE_STATUS);
    public final DateTimePath<Instant> enableTimestamp =
            createInstant("enableTimestamp", ENABLE_TIMESTAMP);
    public final DateTimePath<Instant> validFrom = createInstant("validFrom", VALID_FROM);
    public final DateTimePath<Instant> validTo = createInstant("validTo", VALID_TO);
    public final DateTimePath<Instant> validityChangeTimestamp =
            createInstant("validityChangeTimestamp", VALIDITY_CHANGE_TIMESTAMP);
    public final EnumPath<TimeIntervalStatusType> validityStatus =
            createEnum("validityStatus", TimeIntervalStatusType.class, VALIDITY_STATUS);

    public final StringPath currentStageOutcome = createString("currentStageOutcome", CURRENT_STAGE_OUTCOME);
    public final ArrayPath<byte[], Byte> fullObject = createByteArray("fullObject", FULL_OBJECT);
    public final NumberPath<Integer> campaignIteration = createInteger("campaignIteration", CAMPAIGN_ITERATION);
    public final UuidPath objectRefTargetOid =
            createUuid("objectRefTargetOid", OBJECT_REF_TARGET_OID);
    public final EnumPath<MObjectType> objectRefTargetType =
            createEnum("objectRefTargetType", MObjectType.class, OBJECT_REF_TARGET_TYPE);
    public final NumberPath<Integer> objectRefRelationId =
            createInteger("objectRefRelationId", OBJECT_REF_RELATION_ID);
    public final UuidPath orgRefTargetOid =
            createUuid("orgRefTargetOid", ORG_REF_TARGET_OID);
    public final EnumPath<MObjectType> orgRefTargetType =
            createEnum("orgRefTargetType", MObjectType.class, ORG_REF_TARGET_TYPE);
    public final NumberPath<Integer> orgRefRelationId =
            createInteger("orgRefRelationId", ORG_REF_RELATION_ID);
    public final StringPath outcome = createString("outcome", OUTCOME);
    public final DateTimePath<Instant> remediedTimestamp =
            createInstant("remediedTimestamp", REMEDIED_TIMESTAMP);
    public final DateTimePath<Instant> currentStageDeadline =
            createInstant("currentStageDeadline", CURRENT_STAGE_DEADLINE);
    public final DateTimePath<Instant> currentStageCreateTimestamp =
            createInstant("currentStageCreateTimestamp", CURRENT_STAGE_CREATE_TIMESTAMP);
    public final DateTimePath<Instant> reviewRequestedTimestamp =
            createInstant("reviewRequestedTimestamp", CURRENT_STAGE_CREATE_TIMESTAMP);
    public final NumberPath<Integer> stageNumber =
            createInteger("stageNumber", STAGE_NUMBER);
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

    public QAccessCertificationCase(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAccessCertificationCase(String variable, String schema, String table) {
        super(MAccessCertificationCase.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MAccessCertificationCampaign ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
