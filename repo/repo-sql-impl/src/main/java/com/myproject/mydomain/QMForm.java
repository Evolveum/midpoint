package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMForm is a Querydsl query type for QMForm
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMForm extends com.querydsl.sql.RelationalPathBase<QMForm> {

    private static final long serialVersionUID = 2118065424;

    public static final QMForm mForm = new QMForm("M_FORM");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMForm> constraint88c0 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> formFk = createForeignKey(oid, "OID");

    public QMForm(String variable) {
        super(QMForm.class, forVariable(variable), "PUBLIC", "M_FORM");
        addMetadata();
    }

    public QMForm(String variable, String schema, String table) {
        super(QMForm.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMForm(String variable, String schema) {
        super(QMForm.class, forVariable(variable), schema, "M_FORM");
        addMetadata();
    }

    public QMForm(Path<? extends QMForm> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_FORM");
        addMetadata();
    }

    public QMForm(PathMetadata metadata) {
        super(QMForm.class, metadata, "PUBLIC", "M_FORM");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

