package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMAbstractRole is a Querydsl query type for QMAbstractRole
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAbstractRole extends com.querydsl.sql.RelationalPathBase<QMAbstractRole> {

    private static final long serialVersionUID = 489162756;

    public static final QMAbstractRole mAbstractRole = new QMAbstractRole("M_ABSTRACT_ROLE");

    public final StringPath approvalprocess = createString("approvalprocess");

    public final BooleanPath autoassignEnabled = createBoolean("autoassignEnabled");

    public final StringPath displaynameNorm = createString("displaynameNorm");

    public final StringPath displaynameOrig = createString("displaynameOrig");

    public final StringPath identifier = createString("identifier");

    public final StringPath oid = createString("oid");

    public final StringPath ownerrefRelation = createString("ownerrefRelation");

    public final StringPath ownerrefTargetoid = createString("ownerrefTargetoid");

    public final NumberPath<Integer> ownerrefTargettype = createNumber("ownerrefTargettype", Integer.class);

    public final NumberPath<Integer> ownerrefType = createNumber("ownerrefType", Integer.class);

    public final BooleanPath requestable = createBoolean("requestable");

    public final StringPath risklevel = createString("risklevel");

    public final com.querydsl.sql.PrimaryKey<QMAbstractRole> constraint1c = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMFocus> abstractRoleFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMRole> _roleFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMArchetype> _archetypeFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMOrg> _orgFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMService> _serviceFk = createInvForeignKey(oid, "OID");

    public QMAbstractRole(String variable) {
        super(QMAbstractRole.class, forVariable(variable), "PUBLIC", "M_ABSTRACT_ROLE");
        addMetadata();
    }

    public QMAbstractRole(String variable, String schema, String table) {
        super(QMAbstractRole.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAbstractRole(String variable, String schema) {
        super(QMAbstractRole.class, forVariable(variable), schema, "M_ABSTRACT_ROLE");
        addMetadata();
    }

    public QMAbstractRole(Path<? extends QMAbstractRole> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ABSTRACT_ROLE");
        addMetadata();
    }

    public QMAbstractRole(PathMetadata metadata) {
        super(QMAbstractRole.class, metadata, "PUBLIC", "M_ABSTRACT_ROLE");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(approvalprocess, ColumnMetadata.named("APPROVALPROCESS").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(autoassignEnabled, ColumnMetadata.named("AUTOASSIGN_ENABLED").withIndex(2).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(displaynameNorm, ColumnMetadata.named("DISPLAYNAME_NORM").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(displaynameOrig, ColumnMetadata.named("DISPLAYNAME_ORIG").withIndex(4).ofType(Types.VARCHAR).withSize(255));
        addMetadata(identifier, ColumnMetadata.named("IDENTIFIER").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(11).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(ownerrefRelation, ColumnMetadata.named("OWNERREF_RELATION").withIndex(6).ofType(Types.VARCHAR).withSize(157));
        addMetadata(ownerrefTargetoid, ColumnMetadata.named("OWNERREF_TARGETOID").withIndex(7).ofType(Types.VARCHAR).withSize(36));
        addMetadata(ownerrefTargettype, ColumnMetadata.named("OWNERREF_TARGETTYPE").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(ownerrefType, ColumnMetadata.named("OWNERREF_TYPE").withIndex(8).ofType(Types.INTEGER).withSize(10));
        addMetadata(requestable, ColumnMetadata.named("REQUESTABLE").withIndex(9).ofType(Types.BOOLEAN).withSize(1));
        addMetadata(risklevel, ColumnMetadata.named("RISKLEVEL").withIndex(10).ofType(Types.VARCHAR).withSize(255));
    }

}

