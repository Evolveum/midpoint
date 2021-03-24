/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QShadow extends QObject<MShadow> {

    private static final long serialVersionUID = -8704333735247282997L;

    public static final String TABLE_NAME = "m_shadow";

    // TODO
    public static final ColumnMetadata OBJECT_CLASS_ID =
            ColumnMetadata.named("objectClass_id").ofType(Types.INTEGER);
    public static final ColumnMetadata RESOURCE_REF_TARGET_OID =
            ColumnMetadata.named("resourceRef_targetOid").ofType(Types.OTHER);
    public static final ColumnMetadata RESOURCE_REF_TARGET_TYPE =
            ColumnMetadata.named("resourceRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata RESOURCE_REF_RELATION_ID =
            ColumnMetadata.named("resourceRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata INTENT =
            ColumnMetadata.named("intent").ofType(Types.VARCHAR);
    public static final ColumnMetadata KIND =
            ColumnMetadata.named("kind").ofType(Types.OTHER);
    public static final ColumnMetadata ATTEMPT_NUMBER =
            ColumnMetadata.named("attemptNumber").ofType(Types.INTEGER);
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
    public final EnumPath<ShadowKindType> kind = createEnum("kind", ShadowKindType.class, KIND);
    public final NumberPath<Integer> attemptNumber = createInteger("attemptNumber", ATTEMPT_NUMBER);
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

    public QShadow(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QShadow(String variable, String schema, String table) {
        super(MShadow.class, variable, schema, table);
    }
}
