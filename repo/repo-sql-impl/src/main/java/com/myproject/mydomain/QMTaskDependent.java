package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMTaskDependent is a Querydsl query type for QMTaskDependent
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMTaskDependent extends com.querydsl.sql.RelationalPathBase<QMTaskDependent> {

    private static final long serialVersionUID = 807559214;

    public static final QMTaskDependent mTaskDependent = new QMTaskDependent("M_TASK_DEPENDENT");

    public final StringPath dependent = createString("dependent");

    public final StringPath taskOid = createString("taskOid");

    public final com.querydsl.sql.ForeignKey<QMTask> taskDependentFk = createForeignKey(taskOid, "OID");

    public QMTaskDependent(String variable) {
        super(QMTaskDependent.class, forVariable(variable), "PUBLIC", "M_TASK_DEPENDENT");
        addMetadata();
    }

    public QMTaskDependent(String variable, String schema, String table) {
        super(QMTaskDependent.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMTaskDependent(String variable, String schema) {
        super(QMTaskDependent.class, forVariable(variable), schema, "M_TASK_DEPENDENT");
        addMetadata();
    }

    public QMTaskDependent(Path<? extends QMTaskDependent> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_TASK_DEPENDENT");
        addMetadata();
    }

    public QMTaskDependent(PathMetadata metadata) {
        super(QMTaskDependent.class, metadata, "PUBLIC", "M_TASK_DEPENDENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(dependent, ColumnMetadata.named("DEPENDENT").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(taskOid, ColumnMetadata.named("TASK_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

