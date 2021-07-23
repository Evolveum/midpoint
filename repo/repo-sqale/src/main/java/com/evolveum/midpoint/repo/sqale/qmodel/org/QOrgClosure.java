/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.org;

import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.SqaleRepositoryService;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for org closure table.
 * Can also be used for common table expression (CTE) representing org hierarchy on the fly.
 * This does not have to be under {@link FlexibleRelationalPathBase}, but is for convenience.
 *
 * [IMPORTANT]
 * *Be aware that the materialized view is refreshed only on demand!*
 * This is executed when {@link com.evolveum.midpoint.prism.query.OrgFilter} is used in
 * {@link SqaleQueryContext#beforeQuery()} or when executing
 * {@link SqaleRepositoryService#isAnySubordinate(java.lang.String, java.util.Collection)}.
 * If any access via other paths is done, use statement `CALL m_refresh_org_closure()` before.
 */
@SuppressWarnings("unused")
public class QOrgClosure extends FlexibleRelationalPathBase<MOrgClosure> {

    private static final long serialVersionUID = 4406075586720866032L;

    public static final String TABLE_NAME = "m_org_closure";

    public static final String DEFAULT_ALIAS_NAME = "orgc";

    public static final ColumnMetadata ANCESTOR_OID =
            ColumnMetadata.named("ancestor_oid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata DESCENDANT_OID =
            ColumnMetadata.named("descendant_oid").ofType(UuidPath.UUID_TYPE);

    public final UuidPath ancestorOid = createUuid("ancestorOid", ANCESTOR_OID);
    public final UuidPath descendantOid = createUuid("descendantOid", DESCENDANT_OID);

    public QOrgClosure() {
        this(DEFAULT_ALIAS_NAME, DEFAULT_SCHEMA_NAME);
    }

    public QOrgClosure(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME);
    }

    public QOrgClosure(String variable, String schema) {
        super(MOrgClosure.class, variable, schema, TABLE_NAME);
    }
}
