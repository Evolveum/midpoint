/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.org;

import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for common table expression (CTE) representing org hierarchy on the fly.
 * Does not use any backing bean, it is never retrieved directly, only used in the query.
 * This does not have to be under {@link FlexibleRelationalPathBase}, but is for convenience.
 */
@SuppressWarnings("unused")
public class QOrgClosure extends FlexibleRelationalPathBase<QOrgClosure> {

    private static final long serialVersionUID = 4406075586720866032L;

    public static final String DEFAULT_ALIAS_NAME = "orgc";

    public static final ColumnMetadata PARENT =
            ColumnMetadata.named("parent").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata CHILD =
            ColumnMetadata.named("child").ofType(UuidPath.UUID_TYPE);

    public final UuidPath parent = createUuid("parent", PARENT);
    public final UuidPath child = createUuid("child", CHILD);

    public QOrgClosure() {
        this(DEFAULT_ALIAS_NAME, DEFAULT_SCHEMA_NAME);
    }

    public QOrgClosure(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME);
    }

    public QOrgClosure(String variable, String schema) {
        super(QOrgClosure.class, variable, schema, "orgc"); // not a real table
    }
}
