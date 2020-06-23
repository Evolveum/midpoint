package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMGlobalMetadata is a Querydsl query type for QMGlobalMetadata
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMGlobalMetadata extends com.querydsl.sql.RelationalPathBase<QMGlobalMetadata> {

    private static final long serialVersionUID = -785338050;

    public static final QMGlobalMetadata mGlobalMetadata = new QMGlobalMetadata("M_GLOBAL_METADATA");

    public final StringPath name = createString("name");

    public final StringPath value = createString("value");

    public final com.querydsl.sql.PrimaryKey<QMGlobalMetadata> constraintF5 = createPrimaryKey(name);

    public QMGlobalMetadata(String variable) {
        super(QMGlobalMetadata.class, forVariable(variable), "PUBLIC", "M_GLOBAL_METADATA");
        addMetadata();
    }

    public QMGlobalMetadata(String variable, String schema, String table) {
        super(QMGlobalMetadata.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMGlobalMetadata(String variable, String schema) {
        super(QMGlobalMetadata.class, forVariable(variable), schema, "M_GLOBAL_METADATA");
        addMetadata();
    }

    public QMGlobalMetadata(Path<? extends QMGlobalMetadata> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_GLOBAL_METADATA");
        addMetadata();
    }

    public QMGlobalMetadata(PathMetadata metadata) {
        super(QMGlobalMetadata.class, metadata, "PUBLIC", "M_GLOBAL_METADATA");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(name, ColumnMetadata.named("NAME").withIndex(1).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(value, ColumnMetadata.named("VALUE").withIndex(2).ofType(Types.VARCHAR).withSize(255));
    }

}

