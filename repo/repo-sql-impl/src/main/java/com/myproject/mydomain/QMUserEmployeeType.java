package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMUserEmployeeType is a Querydsl query type for QMUserEmployeeType
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMUserEmployeeType extends com.querydsl.sql.RelationalPathBase<QMUserEmployeeType> {

    private static final long serialVersionUID = 1236846783;

    public static final QMUserEmployeeType mUserEmployeeType = new QMUserEmployeeType("M_USER_EMPLOYEE_TYPE");

    public final StringPath employeetype = createString("employeetype");

    public final StringPath userOid = createString("userOid");

    public final com.querydsl.sql.ForeignKey<QMUser> userEmployeeTypeFk = createForeignKey(userOid, "OID");

    public QMUserEmployeeType(String variable) {
        super(QMUserEmployeeType.class, forVariable(variable), "PUBLIC", "M_USER_EMPLOYEE_TYPE");
        addMetadata();
    }

    public QMUserEmployeeType(String variable, String schema, String table) {
        super(QMUserEmployeeType.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMUserEmployeeType(String variable, String schema) {
        super(QMUserEmployeeType.class, forVariable(variable), schema, "M_USER_EMPLOYEE_TYPE");
        addMetadata();
    }

    public QMUserEmployeeType(Path<? extends QMUserEmployeeType> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_USER_EMPLOYEE_TYPE");
        addMetadata();
    }

    public QMUserEmployeeType(PathMetadata metadata) {
        super(QMUserEmployeeType.class, metadata, "PUBLIC", "M_USER_EMPLOYEE_TYPE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(employeetype, ColumnMetadata.named("EMPLOYEETYPE").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(userOid, ColumnMetadata.named("USER_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

