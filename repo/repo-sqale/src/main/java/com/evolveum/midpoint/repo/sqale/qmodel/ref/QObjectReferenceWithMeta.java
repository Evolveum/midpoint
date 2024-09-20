package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QObjectReferenceWithMeta<OR extends MObject> extends QObjectReference<OR> {

    public static final ColumnMetadata FULL_OBJECT =
            ColumnMetadata.named("fullObject").ofType(Types.BINARY);

    public final ArrayPath<byte[], Byte> fullObject = createByteArray("fullObject", FULL_OBJECT);

    public QObjectReferenceWithMeta(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QObjectReferenceWithMeta(String variable, String schema, String table) {
        super(MObjectReferenceWithMeta.class, variable, schema, table);
    }

}
