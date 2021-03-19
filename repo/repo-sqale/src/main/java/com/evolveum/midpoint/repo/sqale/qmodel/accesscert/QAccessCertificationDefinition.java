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
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAccessCertificationDefinition extends QObject<MAccessCertificationDefinition> {

    private static final long serialVersionUID = 6073628996722018176L;

    public static final String TABLE_NAME = "m_access_cert_definition";

    public static final ColumnMetadata HANDLER_URI_ID =
            ColumnMetadata.named("handlerUri_id").ofType(Types.INTEGER);
    public static final ColumnMetadata LAST_CAMPAIGN_STARTED_TIMESTAMP =
            ColumnMetadata.named("lastCampaignStartedTimestamp")
                    .ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata LAST_CAMPAIGN_CLOSED_TIMESTAMP =
            ColumnMetadata.named("lastCampaignClosedTimestamp")
                    .ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata OWNER_REF_TARGET_OID =
            ColumnMetadata.named("ownerRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata OWNER_REF_TARGET_TYPE =
            ColumnMetadata.named("ownerRef_targetType").ofType(Types.OTHER);
    public static final ColumnMetadata OWNER_REF_RELATION_ID =
            ColumnMetadata.named("ownerRef_relation_id").ofType(Types.INTEGER);

    public final NumberPath<Integer> handlerUriId = createInteger("handlerUriId", HANDLER_URI_ID);
    public final DateTimePath<Instant> lastCampaignStartedTimestamp =
            createInstant("lastCampaignStartedTimestamp", LAST_CAMPAIGN_STARTED_TIMESTAMP);
    public final DateTimePath<Instant> lastCampaignClosedTimestamp =
            createInstant("lastCampaignClosedTimestamp", LAST_CAMPAIGN_CLOSED_TIMESTAMP);
    public final UuidPath ownerRefTargetOid =
            createUuid("ownerRefTargetOid", OWNER_REF_TARGET_OID);
    public final EnumPath<MObjectType> ownerRefTargetType =
            createEnum("ownerRefTargetType", MObjectType.class, OWNER_REF_TARGET_TYPE);
    public final NumberPath<Integer> ownerRefRelationId =
            createInteger("ownerRefRelationId", OWNER_REF_RELATION_ID);

    public QAccessCertificationDefinition(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAccessCertificationDefinition(String variable, String schema, String table) {
        super(MAccessCertificationDefinition.class, variable, schema, table);
    }
}
