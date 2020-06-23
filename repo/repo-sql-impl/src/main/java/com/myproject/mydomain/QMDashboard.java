package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMDashboard is a Querydsl query type for QMDashboard
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMDashboard extends com.querydsl.sql.RelationalPathBase<QMDashboard> {

    private static final long serialVersionUID = -2078922296;

    public static final QMDashboard mDashboard = new QMDashboard("M_DASHBOARD");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMDashboard> constraintD7 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> dashboardFk = createForeignKey(oid, "OID");

    public QMDashboard(String variable) {
        super(QMDashboard.class, forVariable(variable), "PUBLIC", "M_DASHBOARD");
        addMetadata();
    }

    public QMDashboard(String variable, String schema, String table) {
        super(QMDashboard.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMDashboard(String variable, String schema) {
        super(QMDashboard.class, forVariable(variable), schema, "M_DASHBOARD");
        addMetadata();
    }

    public QMDashboard(Path<? extends QMDashboard> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_DASHBOARD");
        addMetadata();
    }

    public QMDashboard(PathMetadata metadata) {
        super(QMDashboard.class, metadata, "PUBLIC", "M_DASHBOARD");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

