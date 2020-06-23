package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMTask is a Querydsl query type for QMTask
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMTask extends com.querydsl.sql.RelationalPathBase<QMTask> {

    private static final long serialVersionUID = 2118469073;

    public static final QMTask mTask = new QMTask("M_TASK");

    public final NumberPath<Integer> binding = createNumber("binding", Integer.class);

    public final StringPath category = createString("category");

    public final DateTimePath<java.sql.Timestamp> completiontimestamp = createDateTime("completiontimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> executionstatus = createNumber("executionstatus", Integer.class);

    public final SimplePath<java.sql.Blob> fullresult = createSimple("fullresult", java.sql.Blob.class);

    public final StringPath handleruri = createString("handleruri");

    public final DateTimePath<java.sql.Timestamp> lastrunfinishtimestamp = createDateTime("lastrunfinishtimestamp", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> lastrunstarttimestamp = createDateTime("lastrunstarttimestamp", java.sql.Timestamp.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath node = createString("node");

    public final StringPath objectrefRelation = createString("objectrefRelation");

    public final StringPath objectrefTargetoid = createString("objectrefTargetoid");

    public final NumberPath<Integer> objectrefTargettype = createNumber("objectrefTargettype", Integer.class);

    public final NumberPath<Integer> objectrefType = createNumber("objectrefType", Integer.class);

    public final StringPath oid = createString("oid");

    public final StringPath ownerrefRelation = createString("ownerrefRelation");

    public final StringPath ownerrefTargetoid = createString("ownerrefTargetoid");

    public final NumberPath<Integer> ownerreftaskTargettype = createNumber("ownerreftaskTargettype", Integer.class);

    public final NumberPath<Integer> ownerrefType = createNumber("ownerrefType", Integer.class);

    public final StringPath parent = createString("parent");

    public final NumberPath<Integer> recurrence = createNumber("recurrence", Integer.class);

    public final NumberPath<Integer> status = createNumber("status", Integer.class);

    public final StringPath taskidentifier = createString("taskidentifier");

    public final NumberPath<Integer> threadstopaction = createNumber("threadstopaction", Integer.class);

    public final NumberPath<Integer> waitingreason = createNumber("waitingreason", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMTask> constraint88c = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> taskFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMTaskDependent> _taskDependentFk = createInvForeignKey(oid, "TASK_OID");

    public QMTask(String variable) {
        super(QMTask.class, forVariable(variable), "PUBLIC", "M_TASK");
        addMetadata();
    }

    public QMTask(String variable, String schema, String table) {
        super(QMTask.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMTask(String variable, String schema) {
        super(QMTask.class, forVariable(variable), schema, "M_TASK");
        addMetadata();
    }

    public QMTask(Path<? extends QMTask> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_TASK");
        addMetadata();
    }

    public QMTask(PathMetadata metadata) {
        super(QMTask.class, metadata, "PUBLIC", "M_TASK");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(binding, ColumnMetadata.named("BINDING").withIndex(1).ofType(Types.INTEGER).withSize(10));
        addMetadata(category, ColumnMetadata.named("CATEGORY").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(completiontimestamp, ColumnMetadata.named("COMPLETIONTIMESTAMP").withIndex(3).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(executionstatus, ColumnMetadata.named("EXECUTIONSTATUS").withIndex(4).ofType(Types.INTEGER).withSize(10));
        addMetadata(fullresult, ColumnMetadata.named("FULLRESULT").withIndex(5).ofType(Types.BLOB).withSize(2147483647));
        addMetadata(handleruri, ColumnMetadata.named("HANDLERURI").withIndex(6).ofType(Types.VARCHAR).withSize(255));
        addMetadata(lastrunfinishtimestamp, ColumnMetadata.named("LASTRUNFINISHTIMESTAMP").withIndex(7).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(lastrunstarttimestamp, ColumnMetadata.named("LASTRUNSTARTTIMESTAMP").withIndex(8).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(9).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(10).ofType(Types.VARCHAR).withSize(255));
        addMetadata(node, ColumnMetadata.named("NODE").withIndex(11).ofType(Types.VARCHAR).withSize(255));
        addMetadata(objectrefRelation, ColumnMetadata.named("OBJECTREF_RELATION").withIndex(12).ofType(Types.VARCHAR).withSize(157));
        addMetadata(objectrefTargetoid, ColumnMetadata.named("OBJECTREF_TARGETOID").withIndex(13).ofType(Types.VARCHAR).withSize(36));
        addMetadata(objectrefTargettype, ColumnMetadata.named("OBJECTREF_TARGETTYPE").withIndex(25).ofType(Types.INTEGER).withSize(10));
        addMetadata(objectrefType, ColumnMetadata.named("OBJECTREF_TYPE").withIndex(14).ofType(Types.INTEGER).withSize(10));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(24).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownerrefRelation, ColumnMetadata.named("OWNERREF_RELATION").withIndex(15).ofType(Types.VARCHAR).withSize(157));
        addMetadata(ownerrefTargetoid, ColumnMetadata.named("OWNERREF_TARGETOID").withIndex(16).ofType(Types.VARCHAR).withSize(36));
        addMetadata(ownerreftaskTargettype, ColumnMetadata.named("OWNERREFTASK_TARGETTYPE").withIndex(26).ofType(Types.INTEGER).withSize(10));
        addMetadata(ownerrefType, ColumnMetadata.named("OWNERREF_TYPE").withIndex(17).ofType(Types.INTEGER).withSize(10));
        addMetadata(parent, ColumnMetadata.named("PARENT").withIndex(18).ofType(Types.VARCHAR).withSize(255));
        addMetadata(recurrence, ColumnMetadata.named("RECURRENCE").withIndex(19).ofType(Types.INTEGER).withSize(10));
        addMetadata(status, ColumnMetadata.named("STATUS").withIndex(20).ofType(Types.INTEGER).withSize(10));
        addMetadata(taskidentifier, ColumnMetadata.named("TASKIDENTIFIER").withIndex(21).ofType(Types.VARCHAR).withSize(255));
        addMetadata(threadstopaction, ColumnMetadata.named("THREADSTOPACTION").withIndex(22).ofType(Types.INTEGER).withSize(10));
        addMetadata(waitingreason, ColumnMetadata.named("WAITINGREASON").withIndex(23).ofType(Types.INTEGER).withSize(10));
    }

}

