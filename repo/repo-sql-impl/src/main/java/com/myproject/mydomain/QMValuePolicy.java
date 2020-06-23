package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMValuePolicy is a Querydsl query type for QMValuePolicy
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMValuePolicy extends com.querydsl.sql.RelationalPathBase<QMValuePolicy> {

    private static final long serialVersionUID = -228144073;

    public static final QMValuePolicy mValuePolicy = new QMValuePolicy("M_VALUE_POLICY");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMValuePolicy> constraintC4 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> valuePolicyFk = createForeignKey(oid, "OID");

    public QMValuePolicy(String variable) {
        super(QMValuePolicy.class, forVariable(variable), "PUBLIC", "M_VALUE_POLICY");
        addMetadata();
    }

    public QMValuePolicy(String variable, String schema, String table) {
        super(QMValuePolicy.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMValuePolicy(String variable, String schema) {
        super(QMValuePolicy.class, forVariable(variable), schema, "M_VALUE_POLICY");
        addMetadata();
    }

    public QMValuePolicy(Path<? extends QMValuePolicy> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_VALUE_POLICY");
        addMetadata();
    }

    public QMValuePolicy(PathMetadata metadata) {
        super(QMValuePolicy.class, metadata, "PUBLIC", "M_VALUE_POLICY");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

