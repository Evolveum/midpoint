/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath.JSONB_TYPE;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QShadow extends QObject<MShadow> {

    private static final long serialVersionUID = -8704333735247282997L;

    public static final String TABLE_NAME = "m_shadow";

    public static final ColumnMetadata OBJECT_CLASS_ID =
            ColumnMetadata.named("objectClassId").ofType(Types.INTEGER);
    public static final ColumnMetadata RESOURCE_REF_TARGET_OID =
            ColumnMetadata.named("resourceRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata RESOURCE_REF_TARGET_TYPE =
            ColumnMetadata.named("resourceRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata RESOURCE_REF_RELATION_ID =
            ColumnMetadata.named("resourceRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata INTENT =
            ColumnMetadata.named("intent").ofType(Types.VARCHAR);
    public static final ColumnMetadata TAG =
            ColumnMetadata.named("tag").ofType(Types.VARCHAR);
    public static final ColumnMetadata KIND =
            ColumnMetadata.named("kind").ofType(Types.OTHER);
    public static final ColumnMetadata DEAD = ColumnMetadata.named("dead").ofType(Types.BOOLEAN);
    public static final ColumnMetadata EXIST = ColumnMetadata.named("exist").ofType(Types.BOOLEAN);
    public static final ColumnMetadata FULL_SYNCHRONIZATION_TIMESTAMP =
            ColumnMetadata.named("fullSynchronizationTimestamp")
                    .ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata PENDING_OPERATION_COUNT =
            ColumnMetadata.named("pendingOperationCount").ofType(Types.INTEGER);
    public static final ColumnMetadata PRIMARY_IDENTIFIER_VALUE =
            ColumnMetadata.named("primaryIdentifierValue").ofType(Types.VARCHAR);
    public static final ColumnMetadata SYNCHRONIZATION_SITUATION =
            ColumnMetadata.named("synchronizationSituation").ofType(Types.OTHER);
    public static final ColumnMetadata SYNCHRONIZATION_TIMESTAMP =
            ColumnMetadata.named("synchronizationTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata ATTRIBUTES =
            ColumnMetadata.named("attributes").ofType(JSONB_TYPE);
    // correlation
    public static final ColumnMetadata CORRELATION_START_TIMESTAMP =
            ColumnMetadata.named("correlationStartTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CORRELATION_END_TIMESTAMP =
            ColumnMetadata.named("correlationEndTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CORRELATION_CASE_OPEN_TIMESTAMP =
            ColumnMetadata.named("correlationCaseOpenTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CORRELATION_CASE_CLOSE_TIMESTAMP =
            ColumnMetadata.named("correlationCaseCloseTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CORRELATION_SITUATION =
            ColumnMetadata.named("correlationSituation").ofType(Types.OTHER);


    public static final ColumnMetadata ACTIVATION_DISABLE_REASON_ID =
            ColumnMetadata.named("disableReasonId").ofType(Types.INTEGER);

    public static final ColumnMetadata ACTIVATION_ENABLE_TIMESTAMP =
            ColumnMetadata.named("enableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata ACTIVATION_DISABLE_TIMESTAMP =
            ColumnMetadata.named("disableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    public static final ColumnMetadata LAST_LOGIN_TIMESTAMP =
            ColumnMetadata.named("lastLoginTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    // columns and relations

    public final NumberPath<Integer> objectClassId =
            createInteger("objectClassId", OBJECT_CLASS_ID);
    public final UuidPath resourceRefTargetOid =
            createUuid("resourceRefTargetOid", RESOURCE_REF_TARGET_OID);
    public final EnumPath<MObjectType> resourceRefTargetType =
            createEnum("resourceRefTargetType", MObjectType.class, RESOURCE_REF_TARGET_TYPE);
    public final NumberPath<Integer> resourceRefRelationId =
            createInteger("resourceRefRelationId", RESOURCE_REF_RELATION_ID);
    public final StringPath intent = createString("intent", INTENT);
    public final StringPath tag = createString("tag", TAG);
    public final EnumPath<ShadowKindType> kind = createEnum("kind", ShadowKindType.class, KIND);
    public final BooleanPath dead = createBoolean("dead", DEAD);
    public final BooleanPath exist = createBoolean("exist", EXIST);
    public final DateTimePath<Instant> fullSynchronizationTimestamp =
            createInstant("fullSynchronizationTimestamp", FULL_SYNCHRONIZATION_TIMESTAMP);
    public final NumberPath<Integer> pendingOperationCount =
            createInteger("pendingOperationCount", PENDING_OPERATION_COUNT);
    public final StringPath primaryIdentifierValue =
            createString("primaryIdentifierValue", PRIMARY_IDENTIFIER_VALUE);
    public final EnumPath<SynchronizationSituationType> synchronizationSituation =
            createEnum("synchronizationSituation",
                    SynchronizationSituationType.class, SYNCHRONIZATION_SITUATION);
    public final DateTimePath<Instant> synchronizationTimestamp =
            createInstant("synchronizationTimestamp", SYNCHRONIZATION_TIMESTAMP);
    public final JsonbPath attributes =
            addMetadata(add(new JsonbPath(forProperty("attributes"))), ATTRIBUTES);
    // correlation
    public final DateTimePath<Instant> correlationStartTimestamp =
            createInstant("correlationStartTimestamp", CORRELATION_START_TIMESTAMP);
    public final DateTimePath<Instant> correlationEndTimestamp =
            createInstant("correlationEndTimestamp", CORRELATION_END_TIMESTAMP);
    public final DateTimePath<Instant> correlationCaseOpenTimestamp =
            createInstant("correlationCaseOpenTimestamp", CORRELATION_CASE_OPEN_TIMESTAMP);
    public final DateTimePath<Instant> correlationCaseCloseTimestamp =
            createInstant("correlationCaseCloseTimestamp", CORRELATION_CASE_CLOSE_TIMESTAMP);
    public final EnumPath<CorrelationSituationType> correlationSituation =
            createEnum("correlationSituation", CorrelationSituationType.class, CORRELATION_SITUATION);

    public final NumberPath<Integer> disableReasonId =
            createInteger("disableReasonId", ACTIVATION_DISABLE_REASON_ID);
    public final DateTimePath<Instant> enableTimestamp =
            createInstant("enableTimestamp", ACTIVATION_ENABLE_TIMESTAMP);

    public final DateTimePath<Instant> disableTimestamp =
            createInstant("disableTimestamp", ACTIVATION_DISABLE_TIMESTAMP);
    public final DateTimePath<Instant> lastLoginTimestamp =
            createInstant("lastLoginTimestamp", LAST_LOGIN_TIMESTAMP);


    public QShadow(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QShadow(String variable, String schema, String table) {
        super(MShadow.class, variable, schema, table);
    }

}
