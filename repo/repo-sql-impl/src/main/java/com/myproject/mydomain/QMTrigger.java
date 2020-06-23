package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMTrigger is a Querydsl query type for QMTrigger
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMTrigger extends com.querydsl.sql.RelationalPathBase<QMTrigger> {

    private static final long serialVersionUID = 1540149740;

    public static final QMTrigger mTrigger = new QMTrigger("M_TRIGGER");

    public final StringPath handleruri = createString("handleruri");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final DateTimePath<java.sql.Timestamp> timestampvalue = createDateTime("timestampvalue", java.sql.Timestamp.class);

    public final com.querydsl.sql.PrimaryKey<QMTrigger> constraint10 = createPrimaryKey(id, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMObject> triggerOwnerFk = createForeignKey(ownerOid, "OID");

    public QMTrigger(String variable) {
        super(QMTrigger.class, forVariable(variable), "PUBLIC", "M_TRIGGER");
        addMetadata();
    }

    public QMTrigger(String variable, String schema, String table) {
        super(QMTrigger.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMTrigger(String variable, String schema) {
        super(QMTrigger.class, forVariable(variable), schema, "M_TRIGGER");
        addMetadata();
    }

    public QMTrigger(Path<? extends QMTrigger> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_TRIGGER");
        addMetadata();
    }

    public QMTrigger(PathMetadata metadata) {
        super(QMTrigger.class, metadata, "PUBLIC", "M_TRIGGER");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(handleruri, ColumnMetadata.named("HANDLERURI").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(timestampvalue, ColumnMetadata.named("TIMESTAMPVALUE").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
    }

}

