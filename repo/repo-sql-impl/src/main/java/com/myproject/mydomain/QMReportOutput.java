package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMReportOutput is a Querydsl query type for QMReportOutput
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMReportOutput extends com.querydsl.sql.RelationalPathBase<QMReportOutput> {

    private static final long serialVersionUID = 1736615841;

    public static final QMReportOutput mReportOutput = new QMReportOutput("M_REPORT_OUTPUT");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final StringPath reportrefRelation = createString("reportrefRelation");

    public final StringPath reportrefTargetoid = createString("reportrefTargetoid");

    public final NumberPath<Integer> reportrefTargettype = createNumber("reportrefTargettype", Integer.class);

    public final NumberPath<Integer> reportrefType = createNumber("reportrefType", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMReportOutput> constraintFc = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> reportOutputFk = createForeignKey(oid, "OID");

    public QMReportOutput(String variable) {
        super(QMReportOutput.class, forVariable(variable), "PUBLIC", "M_REPORT_OUTPUT");
        addMetadata();
    }

    public QMReportOutput(String variable, String schema, String table) {
        super(QMReportOutput.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMReportOutput(String variable, String schema) {
        super(QMReportOutput.class, forVariable(variable), schema, "M_REPORT_OUTPUT");
        addMetadata();
    }

    public QMReportOutput(Path<? extends QMReportOutput> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_REPORT_OUTPUT");
        addMetadata();
    }

    public QMReportOutput(PathMetadata metadata) {
        super(QMReportOutput.class, metadata, "PUBLIC", "M_REPORT_OUTPUT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(6).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(reportrefRelation, ColumnMetadata.named("REPORTREF_RELATION").withIndex(3).ofType(Types.VARCHAR).withSize(157));
        addMetadata(reportrefTargetoid, ColumnMetadata.named("REPORTREF_TARGETOID").withIndex(4).ofType(Types.VARCHAR).withSize(36));
        addMetadata(reportrefTargettype, ColumnMetadata.named("REPORTREF_TARGETTYPE").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(reportrefType, ColumnMetadata.named("REPORTREF_TYPE").withIndex(5).ofType(Types.INTEGER).withSize(10));
    }

}

