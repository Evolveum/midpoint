/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import java.sql.Types;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QGenericObject extends QFocus<MGenericObject> {

    private static final long serialVersionUID = -8447131528511969285L;

    public static final String TABLE_NAME = "m_generic_object";

    public static final ColumnMetadata GENERIC_OBJECT_TYPE_ID =
            ColumnMetadata.named("genericObjectType_id").ofType(Types.INTEGER);

    public final NumberPath<Integer> genericObjectTypeId =
            createInteger("genericObjectTypeId", GENERIC_OBJECT_TYPE_ID);

    public QGenericObject(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QGenericObject(String variable, String schema, String table) {
        super(MGenericObject.class, variable, schema, table);
    }
}
