/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.sql.Types;
import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerWithFullObject;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QOperationExecution<OR extends MObject> extends QContainerWithFullObject<MOperationExecution, OR> {

    private static final long serialVersionUID = -6856661540710930040L;

    /**
     * If `QOperationExecution.class` is not enough because of generics,
     * try `QOperationExecution.CLASS`.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QOperationExecution<MObject>> CLASS =
            (Class) QOperationExecution.class;

    public static final String TABLE_NAME = "m_operation_execution";

    public static final ColumnMetadata STATUS =
            ColumnMetadata.named("status").ofType(Types.OTHER);
    public static final ColumnMetadata RECORD_TYPE =
            ColumnMetadata.named("recordType").ofType(Types.OTHER);
    public static final ColumnMetadata INITIATOR_REF_TARGET_OID =
            ColumnMetadata.named("initiatorRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata INITIATOR_REF_TARGET_TYPE =
            ColumnMetadata.named("initiatorRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata INITIATOR_REF_RELATION_ID =
            ColumnMetadata.named("initiatorRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata TASK_REF_TARGET_OID =
            ColumnMetadata.named("taskRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TASK_REF_TARGET_TYPE =
            ColumnMetadata.named("taskRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata TASK_REF_RELATION_ID =
            ColumnMetadata.named("taskRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata TIMESTAMP =
            ColumnMetadata.named("timestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    // attributes

    public final EnumPath<OperationResultStatusType> status =
            createEnum("status", OperationResultStatusType.class, STATUS);
    public final EnumPath<OperationExecutionRecordTypeType> recordType =
            createEnum("recordType", OperationExecutionRecordTypeType.class, RECORD_TYPE);
    public final UuidPath initiatorRefTargetOid =
            createUuid("initiatorRefTargetOid", INITIATOR_REF_TARGET_OID);
    public final EnumPath<MObjectType> initiatorRefTargetType =
            createEnum("initiatorRefTargetType", MObjectType.class, INITIATOR_REF_TARGET_TYPE);
    public final NumberPath<Integer> initiatorRefRelationId =
            createInteger("initiatorRefRelationId", INITIATOR_REF_RELATION_ID);
    public final UuidPath taskRefTargetOid =
            createUuid("taskRefTargetOid", TASK_REF_TARGET_OID);
    public final EnumPath<MObjectType> taskRefTargetType =
            createEnum("taskRefTargetType", MObjectType.class, TASK_REF_TARGET_TYPE);
    public final NumberPath<Integer> taskRefRelationId =
            createInteger("taskRefRelationId", TASK_REF_RELATION_ID);
    public final DateTimePath<Instant> timestamp =
            createInstant("timestamp", TIMESTAMP);

    public QOperationExecution(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QOperationExecution(String variable, String schema, String table) {
        super(MOperationExecution.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
