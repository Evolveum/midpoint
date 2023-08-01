/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QAffectedObjects extends QContainer<MAffectedObjects, MTask> {

    public static final String TABLE_NAME = "m_task_affects_objects";

    public static final ColumnMetadata OBJECT_TYPE =
            ColumnMetadata.named("type").ofType(Types.OTHER).notNull();

    public static final ColumnMetadata ARCHETYPE_REF_TARGET_OID =
            ColumnMetadata.named("archetypeRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ARCHETYPE_REF_TARGET_TYPE =
            ColumnMetadata.named("archetypeRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata ARCHETYPE_REF_RELATION_ID =
            ColumnMetadata.named("archetypeRefRelationId").ofType(Types.INTEGER);

    public final EnumPath<MObjectType> type = createEnum("type", MObjectType.class, OBJECT_TYPE);
    public final UuidPath archetypeRefTargetOid =
            createUuid("archetypeRefTargetOid", ARCHETYPE_REF_TARGET_OID);
    public final EnumPath<MObjectType> archetypeRefTargetType =
            createEnum("archetypeRefTargetType", MObjectType.class, ARCHETYPE_REF_TARGET_TYPE);
    public final NumberPath<Integer> archetypeRefRelationId =
            createInteger("archetypeRefRelationId", ARCHETYPE_REF_RELATION_ID);
    public QAffectedObjects(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAffectedObjects(String variable, String schema, String table) {
        super(MAffectedObjects.class, variable, schema, table);
    }

}
