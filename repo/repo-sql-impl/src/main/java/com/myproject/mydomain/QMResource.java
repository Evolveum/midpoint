package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMResource is a Querydsl query type for QMResource
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMResource extends com.querydsl.sql.RelationalPathBase<QMResource> {

    private static final long serialVersionUID = -1344156070;

    public static final QMResource mResource = new QMResource("M_RESOURCE");

    public final NumberPath<Integer> administrativestate = createNumber("administrativestate", Integer.class);

    public final StringPath connectorrefRelation = createString("connectorrefRelation");

    public final StringPath connectorrefTargetoid = createString("connectorrefTargetoid");

    public final NumberPath<Integer> connectorrefTargettype = createNumber("connectorrefTargettype", Integer.class);

    public final NumberPath<Integer> connectorrefType = createNumber("connectorrefType", Integer.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final NumberPath<Integer> o16Lastavailabilitystatus = createNumber("o16Lastavailabilitystatus", Integer.class);

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMResource> constraint99 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> resourceFk = createForeignKey(oid, "OID");

    public QMResource(String variable) {
        super(QMResource.class, forVariable(variable), "PUBLIC", "M_RESOURCE");
        addMetadata();
    }

    public QMResource(String variable, String schema, String table) {
        super(QMResource.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMResource(String variable, String schema) {
        super(QMResource.class, forVariable(variable), schema, "M_RESOURCE");
        addMetadata();
    }

    public QMResource(Path<? extends QMResource> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_RESOURCE");
        addMetadata();
    }

    public QMResource(PathMetadata metadata) {
        super(QMResource.class, metadata, "PUBLIC", "M_RESOURCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(administrativestate, ColumnMetadata.named("ADMINISTRATIVESTATE").withIndex(1).ofType(Types.INTEGER).withSize(10));
        addMetadata(connectorrefRelation, ColumnMetadata.named("CONNECTORREF_RELATION").withIndex(2).ofType(Types.VARCHAR).withSize(157));
        addMetadata(connectorrefTargetoid, ColumnMetadata.named("CONNECTORREF_TARGETOID").withIndex(3).ofType(Types.VARCHAR).withSize(36));
        addMetadata(connectorrefTargettype, ColumnMetadata.named("CONNECTORREF_TARGETTYPE").withIndex(9).ofType(Types.INTEGER).withSize(10));
        addMetadata(connectorrefType, ColumnMetadata.named("CONNECTORREF_TYPE").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(6).ofType(Types.VARCHAR).withSize(255));
        addMetadata(o16Lastavailabilitystatus, ColumnMetadata.named("O16_LASTAVAILABILITYSTATUS").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(8).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

