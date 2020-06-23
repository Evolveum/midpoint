package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMService is a Querydsl query type for QMService
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMService extends com.querydsl.sql.RelationalPathBase<QMService> {

    private static final long serialVersionUID = 289227497;

    public static final QMService mService = new QMService("M_SERVICE");

    public final NumberPath<Integer> displayorder = createNumber("displayorder", Integer.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMService> constraintC6 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMAbstractRole> serviceFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMServiceType> _serviceTypeFk = createInvForeignKey(oid, "SERVICE_OID");

    public QMService(String variable) {
        super(QMService.class, forVariable(variable), "PUBLIC", "M_SERVICE");
        addMetadata();
    }

    public QMService(String variable, String schema, String table) {
        super(QMService.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMService(String variable, String schema) {
        super(QMService.class, forVariable(variable), schema, "M_SERVICE");
        addMetadata();
    }

    public QMService(Path<? extends QMService> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_SERVICE");
        addMetadata();
    }

    public QMService(PathMetadata metadata) {
        super(QMService.class, metadata, "PUBLIC", "M_SERVICE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(displayorder, ColumnMetadata.named("DISPLAYORDER").withIndex(1).ofType(Types.INTEGER).withSize(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(4).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

