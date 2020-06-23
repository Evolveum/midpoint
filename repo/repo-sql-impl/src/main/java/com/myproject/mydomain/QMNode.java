package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMNode is a Querydsl query type for QMNode
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMNode extends com.querydsl.sql.RelationalPathBase<QMNode> {

    private static final long serialVersionUID = 2118303310;

    public static final QMNode mNode = new QMNode("M_NODE");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath nodeidentifier = createString("nodeidentifier");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMNode> constraint88c4 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> nodeFk = createForeignKey(oid, "OID");

    public QMNode(String variable) {
        super(QMNode.class, forVariable(variable), "PUBLIC", "M_NODE");
        addMetadata();
    }

    public QMNode(String variable, String schema, String table) {
        super(QMNode.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMNode(String variable, String schema) {
        super(QMNode.class, forVariable(variable), schema, "M_NODE");
        addMetadata();
    }

    public QMNode(Path<? extends QMNode> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_NODE");
        addMetadata();
    }

    public QMNode(PathMetadata metadata) {
        super(QMNode.class, metadata, "PUBLIC", "M_NODE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nodeidentifier, ColumnMetadata.named("NODEIDENTIFIER").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(4).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

