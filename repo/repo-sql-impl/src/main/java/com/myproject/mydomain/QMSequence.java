package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMSequence is a Querydsl query type for QMSequence
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMSequence extends com.querydsl.sql.RelationalPathBase<QMSequence> {

    private static final long serialVersionUID = 346456589;

    public static final QMSequence mSequence = new QMSequence("M_SEQUENCE");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMSequence> constraintFe = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> sequenceFk = createForeignKey(oid, "OID");

    public QMSequence(String variable) {
        super(QMSequence.class, forVariable(variable), "PUBLIC", "M_SEQUENCE");
        addMetadata();
    }

    public QMSequence(String variable, String schema, String table) {
        super(QMSequence.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMSequence(String variable, String schema) {
        super(QMSequence.class, forVariable(variable), schema, "M_SEQUENCE");
        addMetadata();
    }

    public QMSequence(Path<? extends QMSequence> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_SEQUENCE");
        addMetadata();
    }

    public QMSequence(PathMetadata metadata) {
        super(QMSequence.class, metadata, "PUBLIC", "M_SEQUENCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

