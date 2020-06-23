package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectTemplate is a Querydsl query type for QMObjectTemplate
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectTemplate extends com.querydsl.sql.RelationalPathBase<QMObjectTemplate> {

    private static final long serialVersionUID = 2113553957;

    public static final QMObjectTemplate mObjectTemplate = new QMObjectTemplate("M_OBJECT_TEMPLATE");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMObjectTemplate> constraintF8 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> objectTemplateFk = createForeignKey(oid, "OID");

    public QMObjectTemplate(String variable) {
        super(QMObjectTemplate.class, forVariable(variable), "PUBLIC", "M_OBJECT_TEMPLATE");
        addMetadata();
    }

    public QMObjectTemplate(String variable, String schema, String table) {
        super(QMObjectTemplate.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectTemplate(String variable, String schema) {
        super(QMObjectTemplate.class, forVariable(variable), schema, "M_OBJECT_TEMPLATE");
        addMetadata();
    }

    public QMObjectTemplate(Path<? extends QMObjectTemplate> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_TEMPLATE");
        addMetadata();
    }

    public QMObjectTemplate(PathMetadata metadata) {
        super(QMObjectTemplate.class, metadata, "PUBLIC", "M_OBJECT_TEMPLATE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

