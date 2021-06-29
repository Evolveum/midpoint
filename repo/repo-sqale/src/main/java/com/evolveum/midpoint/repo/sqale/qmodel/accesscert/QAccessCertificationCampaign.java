/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAccessCertificationCampaign extends QAssignmentHolder<MAccessCertificationCampaign> {

    private static final long serialVersionUID = -6699491294529689597L;

    public static final String TABLE_NAME = "m_access_cert_campaign";

    public static final ColumnMetadata DEFINITION_REF_TARGET_OID =
            ColumnMetadata.named("definitionRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata DEFINITION_REF_TARGET_TYPE =
            ColumnMetadata.named("definitionRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata DEFINITION_REF_RELATION_ID =
            ColumnMetadata.named("definitionRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata END_TIMESTAMP =
            ColumnMetadata.named("endTimestamp")
                    .ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata HANDLER_URI_ID =
            ColumnMetadata.named("handlerUriId").ofType(Types.INTEGER);
    public static final ColumnMetadata CAMPAIGN_ITERATION =
            ColumnMetadata.named("campaignIteration").ofType(Types.INTEGER);
    public static final ColumnMetadata OWNER_REF_TARGET_OID =
            ColumnMetadata.named("ownerRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata OWNER_REF_TARGET_TYPE =
            ColumnMetadata.named("ownerRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata OWNER_REF_RELATION_ID =
            ColumnMetadata.named("ownerRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata STAGE_NUMBER =
            ColumnMetadata.named("stageNumber").ofType(Types.INTEGER);
    public static final ColumnMetadata START_TIMESTAMP =
            ColumnMetadata.named("startTimestamp")
                    .ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata STATE =
            ColumnMetadata.named("state").ofType(Types.OTHER);

    public final UuidPath definitionRefTargetOid =
            createUuid("definitionRefTargetOid", DEFINITION_REF_TARGET_OID);
    public final EnumPath<MObjectType> definitionRefTargetType =
            createEnum("definitionRefTargetType", MObjectType.class, DEFINITION_REF_TARGET_TYPE);
    public final NumberPath<Integer> definitionRefRelationId =
            createInteger("definitionRefRelationId", DEFINITION_REF_RELATION_ID);
    public final DateTimePath<Instant> endTimestamp =
            createInstant("endTimestamp", END_TIMESTAMP);
    public final NumberPath<Integer> handlerUriId = createInteger("handlerUriId", HANDLER_URI_ID);
    public final NumberPath<Integer> campaignIteration = createInteger("campaignIteration", CAMPAIGN_ITERATION);
    public final UuidPath ownerRefTargetOid =
            createUuid("ownerRefTargetOid", OWNER_REF_TARGET_OID);
    public final EnumPath<MObjectType> ownerRefTargetType =
            createEnum("ownerRefTargetType", MObjectType.class, OWNER_REF_TARGET_TYPE);
    public final NumberPath<Integer> ownerRefRelationId =
            createInteger("ownerRefRelationId", OWNER_REF_RELATION_ID);
    public final NumberPath<Integer> stageNumber = createInteger("stageNumber", STAGE_NUMBER);
    public final DateTimePath<Instant> startTimestamp =
            createInstant("startTimestamp", START_TIMESTAMP);
    public final EnumPath<AccessCertificationCampaignStateType> state =
            createEnum("state", AccessCertificationCampaignStateType.class, STATE);

    public QAccessCertificationCampaign(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAccessCertificationCampaign(String variable, String schema, String table) {
        super(MAccessCertificationCampaign.class, variable, schema, table);
    }
}
