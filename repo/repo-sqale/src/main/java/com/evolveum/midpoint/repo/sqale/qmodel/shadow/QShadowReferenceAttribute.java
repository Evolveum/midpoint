package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QShadowReferenceAttribute extends QReference<MShadowReferenceAttribute, MShadow> {

    public static final String TABLE_NAME = "m_shadow_ref_attribute";

    public static final ColumnMetadata PATH_ID =
            ColumnMetadata.named("pathId").ofType(Types.INTEGER);

    public static final ColumnMetadata OWNER_OBJECT_CLASS_ID =
            ColumnMetadata.named("ownerObjectClassId").ofType(Types.INTEGER);
    public static final ColumnMetadata RESOURCE_OID =
            ColumnMetadata.named("resourceOid").ofType(UuidPath.UUID_TYPE);


    public QShadowReferenceAttribute(String variable) {
        super(MShadowReferenceAttribute.class, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public final NumberPath<Integer> pathId =
            createInteger("pathId", PATH_ID);

    public final NumberPath<Integer> ownerObjectClassId =
            createInteger("ownerObjectClassId", OWNER_OBJECT_CLASS_ID);
    public final UuidPath resourceOid =
            createUuid("resourceOid", RESOURCE_OID);

    @Override
    public BooleanExpression isOwnedBy(MShadow ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
