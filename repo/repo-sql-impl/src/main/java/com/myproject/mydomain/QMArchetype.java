package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMArchetype is a Querydsl query type for QMArchetype
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMArchetype extends com.querydsl.sql.RelationalPathBase<QMArchetype> {

    private static final long serialVersionUID = 1594231229;

    public static final QMArchetype mArchetype = new QMArchetype("M_ARCHETYPE");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath oid = createString("oid");

    public final com.querydsl.sql.PrimaryKey<QMArchetype> constraintB = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMAbstractRole> archetypeFk = createForeignKey(oid, "OID");

    public QMArchetype(String variable) {
        super(QMArchetype.class, forVariable(variable), "PUBLIC", "M_ARCHETYPE");
        addMetadata();
    }

    public QMArchetype(String variable, String schema, String table) {
        super(QMArchetype.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMArchetype(String variable, String schema) {
        super(QMArchetype.class, forVariable(variable), schema, "M_ARCHETYPE");
        addMetadata();
    }

    public QMArchetype(Path<? extends QMArchetype> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ARCHETYPE");
        addMetadata();
    }

    public QMArchetype(PathMetadata metadata) {
        super(QMArchetype.class, metadata, "PUBLIC", "M_ARCHETYPE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(3).ofType(Types.VARCHAR).withSize(36).notNull());
    }

}

