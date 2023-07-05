package com.evolveum.midpoint.repo.sqale.qmodel.common;

import java.sql.Types;

import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class QGlobalMetadata extends FlexibleRelationalPathBase<MGlobalMetadata> {

    private static final long serialVersionUID = -1519824042438215508L;

    public static final String TABLE_NAME = "m_global_metadata";

    public static final QGlobalMetadata DEFAULT = new QGlobalMetadata("gm");

    public static final ColumnMetadata NAME =
            ColumnMetadata.named("name").ofType(Types.VARCHAR).notNull();
    public static final ColumnMetadata VALUE =
            ColumnMetadata.named("value").ofType(Types.VARCHAR).notNull();

    public final StringPath name = createString("name", NAME);
    public final StringPath value = createString("value", VALUE);

    public QGlobalMetadata(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QGlobalMetadata(String variable, String schema, String table) {
        super(MGlobalMetadata.class, variable, schema, table);
    }
}
