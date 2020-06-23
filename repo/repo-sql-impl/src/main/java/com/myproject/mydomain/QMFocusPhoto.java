package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMFocusPhoto is a Querydsl query type for QMFocusPhoto
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMFocusPhoto extends com.querydsl.sql.RelationalPathBase<QMFocusPhoto> {

    private static final long serialVersionUID = 1341056902;

    public static final QMFocusPhoto mFocusPhoto = new QMFocusPhoto("M_FOCUS_PHOTO");

    public final StringPath ownerOid = createString("ownerOid");

    public final SimplePath<java.sql.Blob> photo = createSimple("photo", java.sql.Blob.class);

    public final com.querydsl.sql.PrimaryKey<QMFocusPhoto> constraintCa = createPrimaryKey(ownerOid);

    public final com.querydsl.sql.ForeignKey<QMFocus> focusPhotoFk = createForeignKey(ownerOid, "OID");

    public QMFocusPhoto(String variable) {
        super(QMFocusPhoto.class, forVariable(variable), "PUBLIC", "M_FOCUS_PHOTO");
        addMetadata();
    }

    public QMFocusPhoto(String variable, String schema, String table) {
        super(QMFocusPhoto.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMFocusPhoto(String variable, String schema) {
        super(QMFocusPhoto.class, forVariable(variable), schema, "M_FOCUS_PHOTO");
        addMetadata();
    }

    public QMFocusPhoto(Path<? extends QMFocusPhoto> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_FOCUS_PHOTO");
        addMetadata();
    }

    public QMFocusPhoto(PathMetadata metadata) {
        super(QMFocusPhoto.class, metadata, "PUBLIC", "M_FOCUS_PHOTO");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(photo, ColumnMetadata.named("PHOTO").withIndex(2).ofType(Types.BLOB).withSize(2147483647));
    }

}

