/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.role;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocus;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QAbstractRole<R extends MAbstractRole> extends QFocus<R> {

    private static final long serialVersionUID = 8559628642680237808L;

    /**
     * If {@code QAbstractRole.class} is not enough because of generics,
     * try {@code QAbstractRole.CLASS}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QAbstractRole<MAbstractRole>> CLASS = (Class) QAbstractRole.class;

    public static final String TABLE_NAME = "m_abstract_role";

    public static final ColumnMetadata AUTO_ASSIGN_ENABLED =
            ColumnMetadata.named("autoAssignEnabled").ofType(Types.BOOLEAN);
    public static final ColumnMetadata DISPLAY_NAME_ORIG =
            ColumnMetadata.named("displayNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata DISPLAY_NAME_NORM =
            ColumnMetadata.named("displayNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata IDENTIFIER =
            ColumnMetadata.named("identifier").ofType(Types.VARCHAR);
    public static final ColumnMetadata REQUESTABLE =
            ColumnMetadata.named("requestable").ofType(Types.BOOLEAN);
    public static final ColumnMetadata RISK_LEVEL =
            ColumnMetadata.named("riskLevel").ofType(Types.VARCHAR);

    public final BooleanPath autoAssignEnabled =
            createBoolean("autoAssignEnabled", AUTO_ASSIGN_ENABLED);
    public final StringPath displayNameNorm = createString("displayNameNorm", DISPLAY_NAME_NORM);
    public final StringPath displayNameOrig = createString("displayNameOrig", DISPLAY_NAME_ORIG);
    public final StringPath identifier = createString("identifier", IDENTIFIER);
    public final BooleanPath requestable = createBoolean("requestable", REQUESTABLE);
    public final StringPath riskLevel = createString("riskLevel", RISK_LEVEL);

    public QAbstractRole(Class<R> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QAbstractRole(Class<R> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }
}
