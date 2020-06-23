package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMServiceType is a Querydsl query type for QMServiceType
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMServiceType extends com.querydsl.sql.RelationalPathBase<QMServiceType> {

    private static final long serialVersionUID = -641226301;

    public static final QMServiceType mServiceType = new QMServiceType("M_SERVICE_TYPE");

    public final StringPath serviceOid = createString("serviceOid");

    public final StringPath servicetype = createString("servicetype");

    public final com.querydsl.sql.ForeignKey<QMService> serviceTypeFk = createForeignKey(serviceOid, "OID");

    public QMServiceType(String variable) {
        super(QMServiceType.class, forVariable(variable), "PUBLIC", "M_SERVICE_TYPE");
        addMetadata();
    }

    public QMServiceType(String variable, String schema, String table) {
        super(QMServiceType.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMServiceType(String variable, String schema) {
        super(QMServiceType.class, forVariable(variable), schema, "M_SERVICE_TYPE");
        addMetadata();
    }

    public QMServiceType(Path<? extends QMServiceType> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_SERVICE_TYPE");
        addMetadata();
    }

    public QMServiceType(PathMetadata metadata) {
        super(QMServiceType.class, metadata, "PUBLIC", "M_SERVICE_TYPE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(serviceOid, ColumnMetadata.named("SERVICE_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(servicetype, ColumnMetadata.named("SERVICETYPE").withIndex(2).ofType(Types.VARCHAR).withSize(255));
    }

}

