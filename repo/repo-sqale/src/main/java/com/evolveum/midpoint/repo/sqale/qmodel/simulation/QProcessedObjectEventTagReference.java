package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.sql.Types;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

public class QProcessedObjectEventTagReference extends QReference<MProcessedObjectEventTagReference, MProcessedObject> {

    private static final long serialVersionUID = -4323954643404516391L;

    public static final ColumnMetadata PROCESSED_OBJECT_CID =
            ColumnMetadata.named("processedObjectCid").ofType(Types.BIGINT).notNull();

    public final NumberPath<Long> processedObjectCid = createLong("processedObjectCid", PROCESSED_OBJECT_CID);

    public final PrimaryKey<MProcessedObjectEventTagReference> pk =
            createPrimaryKey(ownerOid, processedObjectCid, referenceType, relationId, targetOid);

    public QProcessedObjectEventTagReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QProcessedObjectEventTagReference(String variable, String schema, String table) {
        super(MProcessedObjectEventTagReference.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MProcessedObject ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
                .and(processedObjectCid.eq(ownerRow.cid));
    }

}
