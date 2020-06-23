package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAuditItem is a Querydsl query type for QMAuditItem
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAuditItem extends com.querydsl.sql.RelationalPathBase<QMAuditItem> {

    private static final long serialVersionUID = -838572862;

    public static final QMAuditItem mAuditItem = new QMAuditItem("M_AUDIT_ITEM");

    public final StringPath changeditempath = createString("changeditempath");

    public final NumberPath<Long> recordId = createNumber("recordId", Long.class);

    public final com.querydsl.sql.PrimaryKey<QMAuditItem> constraint1 = createPrimaryKey(changeditempath, recordId);

    public final com.querydsl.sql.ForeignKey<QMAuditEvent> auditItemFk = createForeignKey(recordId, "ID");

    public QMAuditItem(String variable) {
        super(QMAuditItem.class, forVariable(variable), "PUBLIC", "M_AUDIT_ITEM");
        addMetadata();
    }

    public QMAuditItem(String variable, String schema, String table) {
        super(QMAuditItem.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAuditItem(String variable, String schema) {
        super(QMAuditItem.class, forVariable(variable), schema, "M_AUDIT_ITEM");
        addMetadata();
    }

    public QMAuditItem(Path<? extends QMAuditItem> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_AUDIT_ITEM");
        addMetadata();
    }

    public QMAuditItem(PathMetadata metadata) {
        super(QMAuditItem.class, metadata, "PUBLIC", "M_AUDIT_ITEM");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(changeditempath, ColumnMetadata.named("CHANGEDITEMPATH").withIndex(1).ofType(Types.VARCHAR).withSize(255).notNull());
        addMetadata(recordId, ColumnMetadata.named("RECORD_ID").withIndex(2).ofType(Types.BIGINT).withSize(19).notNull());
    }

}

