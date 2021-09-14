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

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAccessCertificationWorkItem extends QContainer<MAccessCertificationWorkItem, MAccessCertificationCase> {

    private static final long serialVersionUID = -672265595179912120L;

    public static final String TABLE_NAME = "m_access_cert_wi";

    public static final ColumnMetadata ACCESS_CERT_CASE_CID =
            ColumnMetadata.named("accessCertCaseCid").ofType(Types.BIGINT).notNull();

    public static final ColumnMetadata CLOSE_TIMESTAMP =
            ColumnMetadata.named("closeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CAMPAIGN_ITERATION =
            ColumnMetadata.named("campaignIteration").ofType(Types.INTEGER);
    public static final ColumnMetadata OUTCOME =
            ColumnMetadata.named("outcome").ofType(Types.VARCHAR);
    public static final ColumnMetadata OUTPUT_CHANGE_TIMESTAMP =
            ColumnMetadata.named("outputChangeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata PERFORMER_REF_TARGET_OID =
            ColumnMetadata.named("performerRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata PERFORMER_REF_TARGET_TYPE =
            ColumnMetadata.named("performerRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata PERFORMER_REF_RELATION_ID =
            ColumnMetadata.named("performerRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata STAGE_NUMBER =
            ColumnMetadata.named("stageNumber").ofType(Types.INTEGER);

    // attributes

    public final NumberPath<Long> accessCertCaseCid =
            createLong("accessCertCaseCid", ACCESS_CERT_CASE_CID);

    public final DateTimePath<Instant> closeTimestamp =
            createInstant("closeTimestamp", CLOSE_TIMESTAMP);
    public final NumberPath<Integer> campaignIteration =
            createInteger("campaignIteration", CAMPAIGN_ITERATION);
    public final StringPath outcome = createString("outcome", OUTCOME);
    public final DateTimePath<Instant> outputChangeTimestamp =
            createInstant("outputChangeTimestamp", OUTPUT_CHANGE_TIMESTAMP);
    public final UuidPath performerRefTargetOid =
            createUuid("performerRefTargetOid", PERFORMER_REF_TARGET_OID);
    public final EnumPath<MObjectType> performerRefTargetType =
            createEnum("performerRefTargetType", MObjectType.class, PERFORMER_REF_TARGET_TYPE);
    public final NumberPath<Integer> performerRefRelationId =
            createInteger("performerRefRelationId", PERFORMER_REF_RELATION_ID);
    public final NumberPath<Integer> stageNumber =
            createInteger("stageNumber", STAGE_NUMBER);

    public QAccessCertificationWorkItem(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAccessCertificationWorkItem(String variable, String schema, String table) {
        super(MAccessCertificationWorkItem.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MAccessCertificationCase caseRow) {
        return ownerOid.eq(caseRow.ownerOid)
                .and(accessCertCaseCid.eq(caseRow.cid));
    }
}
