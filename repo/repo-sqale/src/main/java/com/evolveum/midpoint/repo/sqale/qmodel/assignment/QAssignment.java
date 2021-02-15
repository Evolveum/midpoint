/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAssignment extends FlexibleRelationalPathBase<MAssignment> {

    private static final long serialVersionUID = 7068031681581618788L;

    public static final String TABLE_NAME = "m_assignment";

    public static final ColumnMetadata OWNER_OID =
            ColumnMetadata.named("owner_oid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ID =
            ColumnMetadata.named("cid").ofType(Types.INTEGER);

    public static final ColumnMetadata CREATOR_REF_TARGET_OID =
            ColumnMetadata.named("creatorRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata CREATOR_REF_TARGET_TYPE =
            ColumnMetadata.named("creatorRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata CREATOR_REF_RELATION_ID =
            ColumnMetadata.named("creatorRef_relation_id").ofType(Types.INTEGER);

    public static final ColumnMetadata TARGET_REF_TARGET_OID =
            ColumnMetadata.named("targetRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TARGET_REF_TARGET_TYPE =
            ColumnMetadata.named("targetRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata TARGET_REF_RELATION_ID =
            ColumnMetadata.named("targetRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata TENANT_REF_TARGET_OID =
            ColumnMetadata.named("tenantRef_targetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata TENANT_REF_TARGET_TYPE =
            ColumnMetadata.named("tenantRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata TENANT_REF_RELATION_ID =
            ColumnMetadata.named("tenantRef_relation_id").ofType(Types.INTEGER);

    /*
    owner_type INTEGER NOT NULL,
    administrativeStatus INTEGER,
    archiveTimestamp TIMESTAMPTZ,
    disableReason VARCHAR(255),
    disableTimestamp TIMESTAMPTZ,
    effectiveStatus INTEGER,
    enableTimestamp TIMESTAMPTZ,
    validFrom TIMESTAMPTZ,
    validTo TIMESTAMPTZ,
    validityChangeTimestamp TIMESTAMPTZ,
    validityStatus INTEGER,
    assignmentOwner INTEGER,
    createChannel VARCHAR(255),
    createTimestamp TIMESTAMPTZ,
    creatorRef_targetOid UUID,
    creatorRef_targetType INTEGER, -- soft-references m_objtype
    creatorRef_relation_id INTEGER, -- soft-references m_uri
    lifecycleState VARCHAR(255),
    modifierRef_targetOid UUID,
    modifierRef_targetType INTEGER, -- soft-references m_objtype
    modifierRef_relation_id INTEGER, -- soft-references m_uri
    modifyChannel VARCHAR(255),
    modifyTimestamp TIMESTAMPTZ,
    orderValue INTEGER,
    orgRef_targetOid UUID,
    orgRef_targetType INTEGER, -- soft-references m_objtype
    orgRef_relation_id INTEGER, -- soft-references m_uri
    resourceRef_targetOid UUID,
    resourceRef_targetType INTEGER, -- soft-references m_objtype
    resourceRef_relation_id INTEGER, -- soft-references m_uri
    targetRef_targetOid UUID,
    targetRef_targetType INTEGER, -- soft-references m_objtype
    targetRef_relation_id INTEGER, -- soft-references m_uri
    tenantRef_targetOid UUID,
    tenantRef_targetType INTEGER, -- soft-references m_objtype
    tenantRef_relation_id INTEGER, -- soft-references m_uri
    extId INTEGER, -- TODO what is this?
    extOid VARCHAR(36), -- is this UUID too?
    ext JSONB,
     */
    // TODO the rest

    public UuidPath ownerOid = createUuid("ownerOid", OWNER_OID);
    public NumberPath<Integer> cid = createInteger("cid", ID);
    public final UuidPath targetRefTargetOid =
            createUuid("targetRefTargetOid", TARGET_REF_TARGET_OID);
    public final NumberPath<Integer> targetRefTargetType =
            createInteger("targetRefTargetType", TARGET_REF_TARGET_TYPE);
    public final NumberPath<Integer> targetRefRelationId =
            createInteger("targetRefRelationId", TARGET_REF_RELATION_ID);
    public final UuidPath tenantRefTargetOid =
            createUuid("tenantRefTargetOid", TENANT_REF_TARGET_OID);
    public final NumberPath<Integer> tenantRefTargetType =
            createInteger("tenantRefTargetType", TENANT_REF_TARGET_TYPE);
    public final NumberPath<Integer> tenantRefRelationId =
            createInteger("tenantRefRelationId", TENANT_REF_RELATION_ID);

    public QAssignment(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAssignment(String variable, String schema, String table) {
        super(MAssignment.class, variable, schema, table);
    }
}
