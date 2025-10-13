/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.task;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QAffectedObjects extends QContainer<MAffectedObjects, MTask> {

    public static final String TABLE_NAME = "m_task_affected_objects";

    public static final ColumnMetadata OBJECT_TYPE =
            ColumnMetadata.named("type").ofType(Types.OTHER).notNull();

    public static final ColumnMetadata ACTIVITY_ID =
            ColumnMetadata.named("activityId").ofType(Types.INTEGER);

    public static final ColumnMetadata ARCHETYPE_REF_TARGET_OID =
            ColumnMetadata.named("archetypeRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ARCHETYPE_REF_TARGET_TYPE =
            ColumnMetadata.named("archetypeRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata ARCHETYPE_REF_RELATION_ID =
            ColumnMetadata.named("archetypeRefRelationId").ofType(Types.INTEGER);

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
    public static final ColumnMetadata KIND =
            ColumnMetadata.named("kind").ofType(Types.OTHER);
    private static final ColumnMetadata EXECUTION_MODE = ColumnMetadata.named("executionMode").ofType(Types.OTHER);
    private static final ColumnMetadata PREDEFINED_CONFIGURATION_TO_USE = ColumnMetadata.named("predefinedConfigurationToUse").ofType(Types.OTHER);

    public final NumberPath<Integer> activityId =
            createInteger("activityId", ACTIVITY_ID);

    public final EnumPath<MObjectType> type = createEnum("type", MObjectType.class, OBJECT_TYPE);
    public final UuidPath archetypeRefTargetOid =
            createUuid("archetypeRefTargetOid", ARCHETYPE_REF_TARGET_OID);
    public final EnumPath<MObjectType> archetypeRefTargetType =
            createEnum("archetypeRefTargetType", MObjectType.class, ARCHETYPE_REF_TARGET_TYPE);
    public final NumberPath<Integer> archetypeRefRelationId =
            createInteger("archetypeRefRelationId", ARCHETYPE_REF_RELATION_ID);

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
    public EnumPath<ExecutionModeType> executionMode = createEnum("executionMode", ExecutionModeType.class, EXECUTION_MODE);
    public EnumPath<PredefinedConfigurationType> predefinedConfigurationToUse = createEnum("predefinedConfigurationToUse", PredefinedConfigurationType.class, PREDEFINED_CONFIGURATION_TO_USE);

    public QAffectedObjects(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAffectedObjects(String variable, String schema, String table) {
        super(MAffectedObjects.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MTask ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
