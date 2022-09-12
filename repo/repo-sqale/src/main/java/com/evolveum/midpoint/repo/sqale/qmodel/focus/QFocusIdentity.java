/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import java.sql.Types;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QFocusIdentity<OR extends MFocus> extends QContainer<MFocusIdentity, OR> {

    private static final long serialVersionUID = -6856661540710930040L;

    /**
     * If `QFocusIdentity.class` is not enough because of generics, try `QFocusIdentity.CLASS`.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QFocusIdentity<MFocus>> CLASS =
            (Class) QFocusIdentity.class;

    public static final String TABLE_NAME = "m_focus_identity";

    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);
    public static final ColumnMetadata SOURCE_RESOURCE_REF_TARGET_OID =
            ColumnMetadata.named("sourceResourceRefTargetOid").ofType(UuidPath.UUID_TYPE);

    // attributes

    public final ArrayPath<byte[], Byte> fullObject = createByteArray("fullObject", FULL_OBJECT);
    public final UuidPath sourceResourceRefTargetOid =
            createUuid("sourceResourceRefTargetOid", SOURCE_RESOURCE_REF_TARGET_OID);

    public QFocusIdentity(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QFocusIdentity(String variable, String schema, String table) {
        super(MFocusIdentity.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
