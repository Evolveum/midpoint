package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMConnectorHost is a Querydsl query type for QMConnectorHost
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMConnectorHost extends com.querydsl.sql.RelationalPathBase<QMConnectorHost> {

    private static final long serialVersionUID = -344984375;

    public static final QMConnectorHost mConnectorHost = new QMConnectorHost("M_CONNECTOR_HOST");

    public final StringPath hostname = createString("hostname");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final StringPath port = createString("port");

    public final com.querydsl.sql.PrimaryKey<QMConnectorHost> constraint3 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> connectorHostFk = createForeignKey(oid, "OID");

    public QMConnectorHost(String variable) {
        super(QMConnectorHost.class, forVariable(variable), "PUBLIC", "M_CONNECTOR_HOST");
        addMetadata();
    }

    public QMConnectorHost(String variable, String schema, String table) {
        super(QMConnectorHost.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMConnectorHost(String variable, String schema) {
        super(QMConnectorHost.class, forVariable(variable), schema, "M_CONNECTOR_HOST");
        addMetadata();
    }

    public QMConnectorHost(Path<? extends QMConnectorHost> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_CONNECTOR_HOST");
        addMetadata();
    }

    public QMConnectorHost(PathMetadata metadata) {
        super(QMConnectorHost.class, metadata, "PUBLIC", "M_CONNECTOR_HOST");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(hostname, ColumnMetadata.named("HOSTNAME").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(5).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(port, ColumnMetadata.named("PORT").withIndex(4).ofType(Types.VARCHAR).withSize(255));
    }

}

