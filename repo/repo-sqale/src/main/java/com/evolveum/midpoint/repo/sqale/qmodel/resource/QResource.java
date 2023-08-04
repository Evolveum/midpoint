/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.resource;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdministrativeAvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAdministrativeStateType;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QResource extends QAssignmentHolder<MResource> {

    private static final long serialVersionUID = 4311838248823321876L;

    public static final String TABLE_NAME = "m_resource";

    public static final ColumnMetadata BUSINESS_ADMINISTRATIVE_STATE =
            ColumnMetadata.named("businessAdministrativeState").ofType(Types.OTHER);
    public static final ColumnMetadata ADMINISTRATIVE_OPERATIONAL_STATE_ADMINISTRATIVE_AVAILABILITY_STATUS =
            ColumnMetadata.named("administrativeOperationalStateAdministrativeAvailabilityStatus").ofType(Types.OTHER);
    public static final ColumnMetadata OPERATIONAL_STATE_LAST_AVAILABILITY_STATUS =
            ColumnMetadata.named("operationalStateLastAvailabilityStatus").ofType(Types.OTHER);
    public static final ColumnMetadata CONNECTOR_REF_TARGET_OID =
            ColumnMetadata.named("connectorRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata CONNECTOR_REF_TARGET_TYPE =
            ColumnMetadata.named("connectorRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata CONNECTOR_REF_RELATION_ID =
            ColumnMetadata.named("connectorRefRelationId").ofType(Types.INTEGER);
    public static final ColumnMetadata TEMPLATE = ColumnMetadata.named("template").ofType(Types.BOOLEAN);
    public static final ColumnMetadata ABSTRACT = ColumnMetadata.named("abstract").ofType(Types.BOOLEAN);

    public static final ColumnMetadata SUPER_REF_TARGET_OID =
            ColumnMetadata.named("superRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata SUPER_REF_TARGET_TYPE =
            ColumnMetadata.named("superRefTargetType").ofType(Types.OTHER);
    public static final ColumnMetadata SUPER_REF_RELATION_ID =
            ColumnMetadata.named("superRefRelationId").ofType(Types.INTEGER);

    public final EnumPath<ResourceAdministrativeStateType> businessAdministrativeState =
            createEnum("businessAdministrativeState", ResourceAdministrativeStateType.class,
                    BUSINESS_ADMINISTRATIVE_STATE);
    public final EnumPath<AdministrativeAvailabilityStatusType> administrativeOperationalStateAdministrativeAvailabilityStatus =
            createEnum("administrativeOperationalStateAdministrativeAvailabilityStatus",
                    AdministrativeAvailabilityStatusType.class,
                    ADMINISTRATIVE_OPERATIONAL_STATE_ADMINISTRATIVE_AVAILABILITY_STATUS);
    public final EnumPath<AvailabilityStatusType> operationalStateLastAvailabilityStatus =
            createEnum("operationalStateLastAvailabilityStatus", AvailabilityStatusType.class,
                    OPERATIONAL_STATE_LAST_AVAILABILITY_STATUS);
    public final UuidPath connectorRefTargetOid =
            createUuid("connectorRefTargetOid", CONNECTOR_REF_TARGET_OID);
    public final EnumPath<MObjectType> connectorRefTargetType =
            createEnum("connectorRefTargetType",
                    MObjectType.class, CONNECTOR_REF_TARGET_TYPE);
    public final NumberPath<Integer> connectorRefRelationId =
            createInteger("connectorRefRelationId", CONNECTOR_REF_RELATION_ID);
    public final BooleanPath template = createBoolean("template", TEMPLATE);
    public final BooleanPath abstractValue = createBoolean("abstractValue", ABSTRACT);

    public final UuidPath superRefTargetOid =
            createUuid("superRefTargetOid", SUPER_REF_TARGET_OID);
    public final EnumPath<MObjectType> superRefTargetType =
            createEnum("superRefTargetType",
                    MObjectType.class, SUPER_REF_TARGET_TYPE);
    public final NumberPath<Integer> superRefRelationId =
            createInteger("superRefRelationId", SUPER_REF_RELATION_ID);

    public QResource(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QResource(String variable, String schema, String table) {
        super(MResource.class, variable, schema, table);
    }
}
