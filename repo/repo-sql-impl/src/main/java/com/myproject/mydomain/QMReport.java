package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMReport is a Querydsl query type for QMReport
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMReport extends com.querydsl.sql.RelationalPathBase<QMReport> {

    private static final long serialVersionUID = -19365248;

    public static final QMReport mReport = new QMReport("M_REPORT");

    public final NumberPath<Integer> export = createNumber("export", Integer.class);

    public final NumberPath<Integer> exporttype = createNumber("exporttype", Integer.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final NumberPath<Integer> orientation = createNumber("orientation", Integer.class);

    public final BooleanPath parent = createBoolean("parent");

    public final BooleanPath usehibernatesession = createBoolean("usehibernatesession");

    public final com.querydsl.sql.PrimaryKey<QMReport> constraint70 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> reportFk = createForeignKey(oid, "OID");

    public QMReport(String variable) {
        super(QMReport.class, forVariable(variable), "PUBLIC", "M_REPORT");
        addMetadata();
    }

    public QMReport(String variable, String schema, String table) {
        super(QMReport.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMReport(String variable, String schema) {
        super(QMReport.class, forVariable(variable), schema, "M_REPORT");
        addMetadata();
    }

    public QMReport(Path<? extends QMReport> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_REPORT");
        addMetadata();
    }

    public QMReport(PathMetadata metadata) {
        super(QMReport.class, metadata, "PUBLIC", "M_REPORT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(export, ColumnMetadata.named("EXPORT").withIndex(1).ofType(Types.INTEGER).withSize(10));
        addMetadata(exporttype, ColumnMetadata.named("EXPORTTYPE").withIndex(8).ofType(Types.INTEGER).withSize(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(7).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(orientation, ColumnMetadata.named("ORIENTATION").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(parent, ColumnMetadata.named("PARENT").withIndex(5).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(usehibernatesession, ColumnMetadata.named("USEHIBERNATESESSION").withIndex(6).ofType(Types.BOOLEAN).withSize(1));
    }

}

