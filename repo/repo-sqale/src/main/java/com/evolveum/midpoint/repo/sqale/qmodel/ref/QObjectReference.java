/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

/**
 * Querydsl query type for object owned references.
 * This actually points to super-table, concrete tables are partitioned by {@link MReferenceType}.
 */
public class QObjectReference extends QReference<MReference> {

    private static final long serialVersionUID = -4850458578494140921L;

    public QObjectReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QObjectReference(String variable, String schema, String table) {
        super(MReference.class, variable, schema, table);
    }
}
