/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.BooleanExpression;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

/**
 * Querydsl query type for object owned references.
 * This actually points to super-table, concrete tables are partitioned by {@link MReferenceType}.
 *
 * @param <OR> type of the owner row
 */
public class QObjectReference<OR extends MObject> extends QReference<MReference, OR> {

    private static final long serialVersionUID = -4850458578494140921L;

    public QObjectReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QObjectReference(String variable, String schema, String table) {
        this(MReference.class, variable, schema, table);
    }

    public QObjectReference(Class<? extends MReference> clazz, String variable, String schema, String table) {
        super(clazz, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }

}
