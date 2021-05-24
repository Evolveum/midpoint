/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.cases.MCase;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QCaseWorkItem extends QContainer<MCaseWorkItem, MCase> {

    private static final long serialVersionUID = 341727922218837393L;

    public static final String TABLE_NAME = "m_case_wi";

    public static final ColumnMetadata CLOSE_TIMESTAMP =
            ColumnMetadata.named("closeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata CREATE_TIMESTAMP =
            ColumnMetadata.named("createTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DEADLINE =
            ColumnMetadata.named("deadline").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata ORIGINAL_ASSIGNEE_REF_TARGET_OID =
            ColumnMetadata.named("originalAssigneeRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ORIGINAL_ASSIGNEE_REF_TARGET_TYPE =
            ColumnMetadata.named("originalAssigneeRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata ORIGINAL_ASSIGNEE_REF_RELATION_ID =
            ColumnMetadata.named("originalAssigneeRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata OUTCOME =
            ColumnMetadata.named("outcome").ofType(Types.VARCHAR);
    public static final ColumnMetadata PERFORMER_REF_TARGET_OID =
            ColumnMetadata.named("performerRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata PERFORMER_REF_TARGET_TYPE =
            ColumnMetadata.named("performerRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata PERFORMER_REF_RELATION_ID =
            ColumnMetadata.named("performerRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata STAGE_NUMBER =
            ColumnMetadata.named("stageNumber").ofType(Types.INTEGER);

    // attributes

    public final DateTimePath<Instant> closeTimestamp =
            createInstant("closeTimestamp", CLOSE_TIMESTAMP);
    public final DateTimePath<Instant> createTimestamp =
            createInstant("createTimestamp", CREATE_TIMESTAMP);
    public final DateTimePath<Instant> deadline =
            createInstant("deadline", DEADLINE);
    public final UuidPath originalAssigneeRefTargetOid =
            createUuid("originalAssigneeRefTargetOid", ORIGINAL_ASSIGNEE_REF_TARGET_OID);
    public final EnumPath<MObjectType> originalAssigneeRefTargetType =
            createEnum("originalAssigneeRefTargetType", MObjectType.class, ORIGINAL_ASSIGNEE_REF_TARGET_TYPE);
    public final NumberPath<Integer> originalAssigneeRefRelationId =
            createInteger("originalAssigneeRefRelationId", ORIGINAL_ASSIGNEE_REF_RELATION_ID);
    public final StringPath outcome = createString("outcome", OUTCOME);
    public final UuidPath performerRefTargetOid =
            createUuid("performerRefTargetOid", PERFORMER_REF_TARGET_OID);
    public final EnumPath<MObjectType> performerRefTargetType =
            createEnum("performerRefTargetType", MObjectType.class, PERFORMER_REF_TARGET_TYPE);
    public final NumberPath<Integer> performerRefRelationId =
            createInteger("performerRefRelationId", PERFORMER_REF_RELATION_ID);
    public final NumberPath<Integer> stageNumber =
            createInteger("stageNumber", STAGE_NUMBER);

    public QCaseWorkItem(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QCaseWorkItem(String variable, String schema, String table) {
        super(MCaseWorkItem.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MCase ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
