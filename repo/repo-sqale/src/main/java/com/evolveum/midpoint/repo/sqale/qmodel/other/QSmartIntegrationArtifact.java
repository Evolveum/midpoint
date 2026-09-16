/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.other;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.querydsl.core.types.dsl.*;
import com.querydsl.sql.ColumnMetadata;

import java.io.Serial;
import java.sql.Types;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QSmartIntegrationArtifact extends QAssignmentHolder<MSmartIntegrationArtifact> {

    @Serial private static final long serialVersionUID = -9035871032121613158L;

    public static final String TABLE_NAME = "m_smart_integration_artifact";

    private static final ColumnMetadata RESOURCE_REF_TARGET_OID =
            ColumnMetadata.named("resourceRefTargetOid").ofType(UuidPath.UUID_TYPE);
    private static final ColumnMetadata RESOURCE_REF_TARGET_TYPE =
            ColumnMetadata.named("resourceRefTargetType").ofType(Types.OTHER);
    private static final ColumnMetadata RESOURCE_REF_RELATION_ID =
            ColumnMetadata.named("resourceRefRelationId").ofType(Types.INTEGER);

    private static final ColumnMetadata OBJECT_CLASS_ID =
            ColumnMetadata.named("objectClassId").ofType(Types.INTEGER);
    private static final ColumnMetadata KIND =
            ColumnMetadata.named("kind").ofType(Types.OTHER);
    private static final ColumnMetadata INTENT =
            ColumnMetadata.named("intent").ofType(Types.VARCHAR);
    private static final ColumnMetadata FOCUS_TYPE_ID =
            ColumnMetadata.named("focusTypeId").ofType(Types.INTEGER);

    public final UuidPath resourceRefTargetOid =
            createUuid("resourceRefTargetOid", RESOURCE_REF_TARGET_OID);
    public final EnumPath<MObjectType> resourceRefTargetType =
            createEnum("resourceRefTargetType", MObjectType.class, RESOURCE_REF_TARGET_TYPE);
    public final NumberPath<Integer> resourceRefRelationId =
            createInteger("resourceRefRelationId", RESOURCE_REF_RELATION_ID);

    public final NumberPath<Integer> objectClassId = createInteger("objectClassId", OBJECT_CLASS_ID);
    public final EnumPath<ShadowKindType> kind = createEnum("kind", ShadowKindType.class, KIND);
    public final StringPath intent = createString("intent", INTENT);
    final NumberPath<Integer> focusTypeId = createInteger("focusTypeId", FOCUS_TYPE_ID);

    QSmartIntegrationArtifact(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    private QSmartIntegrationArtifact(String variable, String schema, String table) {
        super(MSmartIntegrationArtifact.class, variable, schema, table);
    }
}
