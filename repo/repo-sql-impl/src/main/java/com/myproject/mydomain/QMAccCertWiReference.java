package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import java.util.*;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAccCertWiReference is a Querydsl query type for QMAccCertWiReference
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAccCertWiReference extends com.querydsl.sql.RelationalPathBase<QMAccCertWiReference> {

    private static final long serialVersionUID = -471751584;

    public static final QMAccCertWiReference mAccCertWiReference = new QMAccCertWiReference("M_ACC_CERT_WI_REFERENCE");

    public final NumberPath<Integer> ownerId = createNumber("ownerId", Integer.class);

    public final NumberPath<Integer> ownerOwnerId = createNumber("ownerOwnerId", Integer.class);

    public final StringPath ownerOwnerOwnerOid = createString("ownerOwnerOwnerOid");

    public final StringPath relation = createString("relation");

    public final StringPath targetoid = createString("targetoid");

    public final NumberPath<Integer> targettype = createNumber("targettype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMAccCertWiReference> constraint4 = createPrimaryKey(ownerId, ownerOwnerId, ownerOwnerOwnerOid, relation, targetoid);

    public final com.querydsl.sql.ForeignKey<QMAccCertWi> accCertWiRefOwnerFk = createForeignKey(Arrays.asList(ownerId, ownerOwnerId, ownerOwnerOwnerOid), Arrays.asList("ID", "OWNER_ID", "OWNER_OWNER_OID"));

    public QMAccCertWiReference(String variable) {
        super(QMAccCertWiReference.class, forVariable(variable), "PUBLIC", "M_ACC_CERT_WI_REFERENCE");
        addMetadata();
    }

    public QMAccCertWiReference(String variable, String schema, String table) {
        super(QMAccCertWiReference.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAccCertWiReference(String variable, String schema) {
        super(QMAccCertWiReference.class, forVariable(variable), schema, "M_ACC_CERT_WI_REFERENCE");
        addMetadata();
    }

    public QMAccCertWiReference(Path<? extends QMAccCertWiReference> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ACC_CERT_WI_REFERENCE");
        addMetadata();
    }

    public QMAccCertWiReference(PathMetadata metadata) {
        super(QMAccCertWiReference.class, metadata, "PUBLIC", "M_ACC_CERT_WI_REFERENCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(ownerId, ColumnMetadata.named("OWNER_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOwnerId, ColumnMetadata.named("OWNER_OWNER_ID").withIndex(2).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOwnerOwnerOid, ColumnMetadata.named("OWNER_OWNER_OWNER_OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(relation, ColumnMetadata.named("RELATION").withIndex(4).ofType(Types.VARCHAR).withSize(157).notNull());
        addMetadata(targetoid, ColumnMetadata.named("TARGETOID").withIndex(5).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(targettype, ColumnMetadata.named("TARGETTYPE").withIndex(6).ofType(Types.INTEGER).withSize(10));
    }

}

