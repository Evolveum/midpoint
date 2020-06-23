package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMConnector is a Querydsl query type for QMConnector
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMConnector extends com.querydsl.sql.RelationalPathBase<QMConnector> {

    private static final long serialVersionUID = -1610271871;

    public static final QMConnector mConnector = new QMConnector("M_CONNECTOR");

    public final StringPath connectorbundle = createString("connectorbundle");

    public final StringPath connectorhostrefRelation = createString("connectorhostrefRelation");

    public final StringPath connectorhostrefTargetoid = createString("connectorhostrefTargetoid");

    public final NumberPath<Integer> connectorhostrefTargettype = createNumber("connectorhostrefTargettype", Integer.class);

    public final NumberPath<Integer> connectorhostrefType = createNumber("connectorhostrefType", Integer.class);

    public final StringPath connectortype = createString("connectortype");

    public final StringPath connectorversion = createString("connectorversion");

    public final StringPath framework = createString("framework");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMConnector> constraintF = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> connectorFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMConnectorTargetSystem> _connectorTargetSystemFk = createInvForeignKey(oid, "CONNECTOR_OID");

    public QMConnector(String variable) {
        super(QMConnector.class, forVariable(variable), "PUBLIC", "M_CONNECTOR");
        addMetadata();
    }

    public QMConnector(String variable, String schema, String table) {
        super(QMConnector.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMConnector(String variable, String schema) {
        super(QMConnector.class, forVariable(variable), schema, "M_CONNECTOR");
        addMetadata();
    }

    public QMConnector(Path<? extends QMConnector> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_CONNECTOR");
        addMetadata();
    }

    public QMConnector(PathMetadata metadata) {
        super(QMConnector.class, metadata, "PUBLIC", "M_CONNECTOR");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(connectorbundle, ColumnMetadata.named("CONNECTORBUNDLE").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(connectorhostrefRelation, ColumnMetadata.named("CONNECTORHOSTREF_RELATION").withIndex(2).ofType(Types.VARCHAR).withSize(157));
        addMetadata(connectorhostrefTargetoid, ColumnMetadata.named("CONNECTORHOSTREF_TARGETOID").withIndex(3).ofType(Types.VARCHAR).withSize(36));
        addMetadata(connectorhostrefTargettype, ColumnMetadata.named("CONNECTORHOSTREF_TARGETTYPE").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(connectorhostrefType, ColumnMetadata.named("CONNECTORHOSTREF_TYPE").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(connectortype, ColumnMetadata.named("CONNECTORTYPE").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(connectorversion, ColumnMetadata.named("CONNECTORVERSION").withIndex(6).ofType(Types.VARCHAR).withSize(255));
        addMetadata(framework, ColumnMetadata.named("FRAMEWORK").withIndex(7).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(8).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(9).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(10).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

