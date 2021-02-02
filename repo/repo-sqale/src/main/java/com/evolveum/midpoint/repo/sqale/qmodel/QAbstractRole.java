/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qbean.MAbstractRole;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAbstractRole<T extends MAbstractRole> extends QObject<T> {

    private static final long serialVersionUID = 8559628642680237808L;

    public static final String TABLE_NAME = "m_abstract_role";

    public static final ColumnMetadata APPROVAL_PROCESS =
            ColumnMetadata.named("approvalProcess").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata AUTOASSIGN_ENABLED =
            ColumnMetadata.named("autoassign_enabled").ofType(Types.BOOLEAN);
    public static final ColumnMetadata DISPLAY_NAME_NORM =
            ColumnMetadata.named("displayName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata DISPLAY_NAME_ORIG =
            ColumnMetadata.named("displayName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata IDENTIFIER =
            ColumnMetadata.named("identifier").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata OWNER_REF_TARGET_OID =
            ColumnMetadata.named("ownerRef_targetOid").ofType(UUID_TYPE);
    public static final ColumnMetadata OWNER_REF_TARGET_TYPE =
            ColumnMetadata.named("ownerRef_targetType").ofType(Types.INTEGER);
    public static final ColumnMetadata OWNER_REF_RELATION_ID =
            ColumnMetadata.named("ownerRef_relation_id").ofType(Types.INTEGER);
    public static final ColumnMetadata REQUESTABLE =
            ColumnMetadata.named("requestable").ofType(Types.BOOLEAN);
    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR).withSize(255);

    public final StringPath approvalProcess = createString("approvalProcess", APPROVAL_PROCESS);
    public final BooleanPath effectiveStatus = createBoolean("autoassignEnabled", AUTOASSIGN_ENABLED);
    public final StringPath displayNameNorm = createString("displayNameNorm", DISPLAY_NAME_NORM);
    public final StringPath displayNameOrig = createString("displayNameOrig", DISPLAY_NAME_ORIG);
    public final StringPath identifier = createString("identifier", IDENTIFIER);
    public final UuidPath ownerRefTargetOid = createUuid("ownerRefTargetOid", OWNER_REF_TARGET_OID);
    public final NumberPath<Integer> ownerRefTargetType =
            createInteger("ownerRefTargetType", OWNER_REF_TARGET_TYPE);
    public final NumberPath<Integer> ownerRefRelationId =
            createInteger("ownerRefRelationId", OWNER_REF_RELATION_ID);
    public final BooleanPath requestable = createBoolean("requestable", REQUESTABLE);
    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);

    public QAbstractRole(Class<? extends T> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAbstractRole(Class<? extends T> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }

    /**
     * Class representing generic {@code QAbstractRole<MAbstractRole>.class} which is otherwise impossible.
     * There should be no need to instantiate this, so the class is private and final.
     */
    public static final class QAbstractRoleReal extends QAbstractRole<MAbstractRole> {
        public QAbstractRoleReal(String variable) {
            super(MAbstractRole.class, variable);
        }
    }
}
