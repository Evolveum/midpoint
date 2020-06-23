package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObjectTextInfo is a Querydsl query type for QMObjectTextInfo
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObjectTextInfo extends com.querydsl.sql.RelationalPathBase<QMObjectTextInfo> {

    private static final long serialVersionUID = -1863829210;

    public static final QMObjectTextInfo mObjectTextInfo = new QMObjectTextInfo("M_OBJECT_TEXT_INFO");

    public final StringPath ownerOid = createString("ownerOid");

    public final StringPath text = createString("text");

    public final com.querydsl.sql.PrimaryKey<QMObjectTextInfo> constraint58 = createPrimaryKey(ownerOid, text);

    public final com.querydsl.sql.ForeignKey<QMObject> objectTextInfoOwnerFk = createForeignKey(ownerOid, "OID");

    public QMObjectTextInfo(String variable) {
        super(QMObjectTextInfo.class, forVariable(variable), "PUBLIC", "M_OBJECT_TEXT_INFO");
        addMetadata();
    }

    public QMObjectTextInfo(String variable, String schema, String table) {
        super(QMObjectTextInfo.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObjectTextInfo(String variable, String schema) {
        super(QMObjectTextInfo.class, forVariable(variable), schema, "M_OBJECT_TEXT_INFO");
        addMetadata();
    }

    public QMObjectTextInfo(Path<? extends QMObjectTextInfo> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT_TEXT_INFO");
        addMetadata();
    }

    public QMObjectTextInfo(PathMetadata metadata) {
        super(QMObjectTextInfo.class, metadata, "PUBLIC", "M_OBJECT_TEXT_INFO");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(text, ColumnMetadata.named("TEXT").withIndex(2).ofType(Types.VARCHAR).withSize(255).notNull());
    }

}

