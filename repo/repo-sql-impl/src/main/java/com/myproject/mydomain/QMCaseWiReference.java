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
 * QMCaseWiReference is a Querydsl query type for QMCaseWiReference
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMCaseWiReference extends com.querydsl.sql.RelationalPathBase<QMCaseWiReference> {

    private static final long serialVersionUID = 1688155005;

    public static final QMCaseWiReference mCaseWiReference = new QMCaseWiReference("M_CASE_WI_REFERENCE");

    public final NumberPath<Integer> ownerId = createNumber("ownerId", Integer.class);

    public final StringPath ownerOwnerOid = createString("ownerOwnerOid");

    public final NumberPath<Integer> referenceType = createNumber("referenceType", Integer.class);

    public final StringPath relation = createString("relation");

    public final StringPath targetoid = createString("targetoid");

    public final NumberPath<Integer> targettype = createNumber("targettype", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMCaseWiReference> constraintDf = createPrimaryKey(ownerId, ownerOwnerOid, referenceType, relation, targetoid);

    public final com.querydsl.sql.ForeignKey<QMCaseWi> caseWiReferenceOwnerFk = createForeignKey(Arrays.asList(ownerId, ownerOwnerOid), Arrays.asList("ID", "OWNER_OID"));

    public QMCaseWiReference(String variable) {
        super(QMCaseWiReference.class, forVariable(variable), "PUBLIC", "M_CASE_WI_REFERENCE");
        addMetadata();
    }

    public QMCaseWiReference(String variable, String schema, String table) {
        super(QMCaseWiReference.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMCaseWiReference(String variable, String schema) {
        super(QMCaseWiReference.class, forVariable(variable), schema, "M_CASE_WI_REFERENCE");
        addMetadata();
    }

    public QMCaseWiReference(Path<? extends QMCaseWiReference> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_CASE_WI_REFERENCE");
        addMetadata();
    }

    public QMCaseWiReference(PathMetadata metadata) {
        super(QMCaseWiReference.class, metadata, "PUBLIC", "M_CASE_WI_REFERENCE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(ownerId, ColumnMetadata.named("OWNER_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(ownerOwnerOid, ColumnMetadata.named("OWNER_OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(referenceType, ColumnMetadata.named("REFERENCE_TYPE").withIndex(3).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(relation, ColumnMetadata.named("RELATION").withIndex(4).ofType(Types.VARCHAR).withSize(157).notNull());
        addMetadata(targetoid, ColumnMetadata.named("TARGETOID").withIndex(5).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(targettype, ColumnMetadata.named("TARGETTYPE").withIndex(6).ofType(Types.INTEGER).withSize(10));
    }

}

