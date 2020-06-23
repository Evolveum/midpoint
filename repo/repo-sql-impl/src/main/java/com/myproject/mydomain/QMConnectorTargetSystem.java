package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMConnectorTargetSystem is a Querydsl query type for QMConnectorTargetSystem
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMConnectorTargetSystem extends com.querydsl.sql.RelationalPathBase<QMConnectorTargetSystem> {

    private static final long serialVersionUID = -189731519;

    public static final QMConnectorTargetSystem mConnectorTargetSystem = new QMConnectorTargetSystem("M_CONNECTOR_TARGET_SYSTEM");

    public final StringPath connectorOid = createString("connectorOid");

    public final StringPath targetsystemtype = createString("targetsystemtype");

    public final com.querydsl.sql.ForeignKey<QMConnector> connectorTargetSystemFk = createForeignKey(connectorOid, "OID");

    public QMConnectorTargetSystem(String variable) {
        super(QMConnectorTargetSystem.class, forVariable(variable), "PUBLIC", "M_CONNECTOR_TARGET_SYSTEM");
        addMetadata();
    }

    public QMConnectorTargetSystem(String variable, String schema, String table) {
        super(QMConnectorTargetSystem.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMConnectorTargetSystem(String variable, String schema) {
        super(QMConnectorTargetSystem.class, forVariable(variable), schema, "M_CONNECTOR_TARGET_SYSTEM");
        addMetadata();
    }

    public QMConnectorTargetSystem(Path<? extends QMConnectorTargetSystem> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_CONNECTOR_TARGET_SYSTEM");
        addMetadata();
    }

    public QMConnectorTargetSystem(PathMetadata metadata) {
        super(QMConnectorTargetSystem.class, metadata, "PUBLIC", "M_CONNECTOR_TARGET_SYSTEM");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(connectorOid, ColumnMetadata.named("CONNECTOR_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(targetsystemtype, ColumnMetadata.named("TARGETSYSTEMTYPE").withIndex(2).ofType(Types.VARCHAR).withSize(255));
    }

}

