package com.evolveum.midpoint.repo.sqale.qmodel.common;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QContainerWithFullObject<R extends MContainerWithFullObject, OR>  extends QContainer<R, OR>  {

    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);

    public final ArrayPath<byte[], Byte> fullObject = createByteArray("fullObject", FULL_OBJECT);

    public QContainerWithFullObject(Class<? extends R> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }
}
