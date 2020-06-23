package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMOperationExecution is a Querydsl query type for QMOperationExecution
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMOperationExecution extends com.querydsl.sql.RelationalPathBase<QMOperationExecution> {

    private static final long serialVersionUID = -1328343875;

    public static final QMOperationExecution mOperationExecution = new QMOperationExecution("M_OPERATION_EXECUTION");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath initiatorrefRelation = createString("initiatorrefRelation");

    public final StringPath initiatorrefTargetoid = createString("initiatorrefTargetoid");

    public final NumberPath<Integer> initiatorrefTargettype = createNumber("initiatorrefTargettype", Integer.class);

    public final NumberPath<Integer> initiatorrefType = createNumber("initiatorrefType", Integer.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final NumberPath<Integer> status = createNumber("status", Integer.class);

    public final StringPath taskrefRelation = createString("taskrefRelation");

    public final StringPath taskrefTargetoid = createString("taskrefTargetoid");

    public final NumberPath<Integer> taskrefTargettype = createNumber("taskrefTargettype", Integer.class);

    public final NumberPath<Integer> taskrefType = createNumber("taskrefType", Integer.class);

    public final DateTimePath<java.sql.Timestamp> timestampvalue = createDateTime("timestampvalue", java.sql.Timestamp.class);

    public final com.querydsl.sql.PrimaryKey<QMOperationExecution> constraint5d = createPrimaryKey(id, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMObject> opExecOwnerFk = createForeignKey(ownerOid, "OID");

    public QMOperationExecution(String variable) {
        super(QMOperationExecution.class, forVariable(variable), "PUBLIC", "M_OPERATION_EXECUTION");
        addMetadata();
    }

    public QMOperationExecution(String variable, String schema, String table) {
        super(QMOperationExecution.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMOperationExecution(String variable, String schema) {
        super(QMOperationExecution.class, forVariable(variable), schema, "M_OPERATION_EXECUTION");
        addMetadata();
    }

    public QMOperationExecution(Path<? extends QMOperationExecution> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OPERATION_EXECUTION");
        addMetadata();
    }

    public QMOperationExecution(PathMetadata metadata) {
        super(QMOperationExecution.class, metadata, "PUBLIC", "M_OPERATION_EXECUTION");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(initiatorrefRelation, ColumnMetadata.named("INITIATORREF_RELATION").withIndex(3).ofType(Types.VARCHAR).withSize(157));
        addMetadata(initiatorrefTargetoid, ColumnMetadata.named("INITIATORREF_TARGETOID").withIndex(4).ofType(Types.VARCHAR).withSize(36));
        addMetadata(initiatorrefTargettype, ColumnMetadata.named("INITIATORREF_TARGETTYPE").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(initiatorrefType, ColumnMetadata.named("INITIATORREF_TYPE").withIndex(5).ofType(Types.INTEGER).withSize(10));
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(status, ColumnMetadata.named("STATUS").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(taskrefRelation, ColumnMetadata.named("TASKREF_RELATION").withIndex(7).ofType(Types.VARCHAR).withSize(157));
        addMetadata(taskrefTargetoid, ColumnMetadata.named("TASKREF_TARGETOID").withIndex(8).ofType(Types.VARCHAR).withSize(36));
        addMetadata(taskrefTargettype, ColumnMetadata.named("TASKREF_TARGETTYPE").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(taskrefType, ColumnMetadata.named("TASKREF_TYPE").withIndex(9).ofType(Types.INTEGER).withSize(10));
        addMetadata(timestampvalue, ColumnMetadata.named("TIMESTAMPVALUE").withIndex(10).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
    }

}

