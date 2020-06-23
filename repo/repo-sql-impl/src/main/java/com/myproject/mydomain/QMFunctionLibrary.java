package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMFunctionLibrary is a Querydsl query type for QMFunctionLibrary
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMFunctionLibrary extends com.querydsl.sql.RelationalPathBase<QMFunctionLibrary> {

    private static final long serialVersionUID = 1455148535;

    public static final QMFunctionLibrary mFunctionLibrary = new QMFunctionLibrary("M_FUNCTION_LIBRARY");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMFunctionLibrary> constraint1cc = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> functionLibraryFk = createForeignKey(oid, "OID");

    public QMFunctionLibrary(String variable) {
        super(QMFunctionLibrary.class, forVariable(variable), "PUBLIC", "M_FUNCTION_LIBRARY");
        addMetadata();
    }

    public QMFunctionLibrary(String variable, String schema, String table) {
        super(QMFunctionLibrary.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMFunctionLibrary(String variable, String schema) {
        super(QMFunctionLibrary.class, forVariable(variable), schema, "M_FUNCTION_LIBRARY");
        addMetadata();
    }

    public QMFunctionLibrary(Path<? extends QMFunctionLibrary> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_FUNCTION_LIBRARY");
        addMetadata();
    }

    public QMFunctionLibrary(PathMetadata metadata) {
        super(QMFunctionLibrary.class, metadata, "PUBLIC", "M_FUNCTION_LIBRARY");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

