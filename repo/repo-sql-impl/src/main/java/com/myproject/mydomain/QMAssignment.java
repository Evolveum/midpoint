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
 * QMAssignment is a Querydsl query type for QMAssignment
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignment extends com.querydsl.sql.RelationalPathBase<QMAssignment> {

    private static final long serialVersionUID = -871879143;

    public static final QMAssignment mAssignment = new QMAssignment("M_ASSIGNMENT");

    public final NumberPath<Integer> administrativestatus = createNumber("administrativestatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> archivetimestamp = createDateTime("archivetimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> assignmentowner = createNumber("assignmentowner", Integer.class);

    public final StringPath createchannel = createString("createchannel");

    public final DateTimePath<java.sql.Timestamp> createtimestamp = createDateTime("createtimestamp", java.sql.Timestamp.class);

    public final StringPath creatorrefRelation = createString("creatorrefRelation");

    public final StringPath creatorrefTargetoid = createString("creatorrefTargetoid");

    public final NumberPath<Integer> creatorrefTargettype = createNumber("creatorrefTargettype", Integer.class);

    public final NumberPath<Integer> creatorrefType = createNumber("creatorrefType", Integer.class);

    public final StringPath disablereason = createString("disablereason");

    public final DateTimePath<java.sql.Timestamp> disabletimestamp = createDateTime("disabletimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> effectivestatus = createNumber("effectivestatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> enabletimestamp = createDateTime("enabletimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> extid = createNumber("extid", Integer.class);

    public final StringPath extoid = createString("extoid");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath lifecyclestate = createString("lifecyclestate");

    public final StringPath modifierrefRelation = createString("modifierrefRelation");

    public final StringPath modifierrefTargetoid = createString("modifierrefTargetoid");

    public final NumberPath<Integer> modifierrefTargettype = createNumber("modifierrefTargettype", Integer.class);

    public final NumberPath<Integer> modifierrefType = createNumber("modifierrefType", Integer.class);

    public final StringPath modifychannel = createString("modifychannel");

    public final DateTimePath<java.sql.Timestamp> modifytimestamp = createDateTime("modifytimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> ordervalue = createNumber("ordervalue", Integer.class);

    public final StringPath orgrefRelation = createString("orgrefRelation");

    public final StringPath orgrefTargetoid = createString("orgrefTargetoid");

    public final NumberPath<Integer> orgrefTargettype = createNumber("orgrefTargettype", Integer.class);

    public final NumberPath<Integer> orgrefType = createNumber("orgrefType", Integer.class);

    public final StringPath ownerOid = createString("ownerOid");

    public final StringPath resourcerefRelation = createString("resourcerefRelation");

    public final StringPath resourcerefTargetoid = createString("resourcerefTargetoid");

    public final NumberPath<Integer> resourcerefTargettype = createNumber("resourcerefTargettype", Integer.class);

    public final NumberPath<Integer> resourcerefType = createNumber("resourcerefType", Integer.class);

    public final StringPath targetrefRelation = createString("targetrefRelation");

    public final StringPath targetrefTargetoid = createString("targetrefTargetoid");

    public final NumberPath<Integer> targetrefTargettype = createNumber("targetrefTargettype", Integer.class);

    public final NumberPath<Integer> targetrefType = createNumber("targetrefType", Integer.class);

    public final StringPath tenantrefRelation = createString("tenantrefRelation");

    public final StringPath tenantrefTargetoid = createString("tenantrefTargetoid");

    public final NumberPath<Integer> tenantrefTargettype = createNumber("tenantrefTargettype", Integer.class);

    public final NumberPath<Integer> tenantrefType = createNumber("tenantrefType", Integer.class);

    public final DateTimePath<java.sql.Timestamp> validfrom = createDateTime("validfrom", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> validitychangetimestamp = createDateTime("validitychangetimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> validitystatus = createNumber("validitystatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> validto = createDateTime("validto", java.sql.Timestamp.class);

    public final com.querydsl.sql.PrimaryKey<QMAssignment> constraintE = createPrimaryKey(id, ownerOid);

    public final com.querydsl.sql.ForeignKey<QMObject> assignmentOwnerFk = createForeignKey(ownerOid, "OID");

    public final com.querydsl.sql.ForeignKey<QMAssignmentReference> _assignmentReferenceFk = createInvForeignKey(Arrays.asList(id, ownerOid), Arrays.asList("OWNER_ID", "OWNER_OWNER_OID"));

    public final com.querydsl.sql.ForeignKey<QMAssignmentPolicySituation> _assignmentPolicySituationFk = createInvForeignKey(Arrays.asList(id, ownerOid), Arrays.asList("ASSIGNMENT_ID", "ASSIGNMENT_OID"));

    public QMAssignment(String variable) {
        super(QMAssignment.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT");
        addMetadata();
    }

    public QMAssignment(String variable, String schema, String table) {
        super(QMAssignment.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignment(String variable, String schema) {
        super(QMAssignment.class, forVariable(variable), schema, "M_ASSIGNMENT");
        addMetadata();
    }

    public QMAssignment(Path<? extends QMAssignment> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT");
        addMetadata();
    }

    public QMAssignment(PathMetadata metadata) {
        super(QMAssignment.class, metadata, "PUBLIC", "M_ASSIGNMENT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(administrativestatus, ColumnMetadata.named("ADMINISTRATIVESTATUS").withIndex(3).ofType(Types.INTEGER).withSize(10));
        addMetadata(archivetimestamp, ColumnMetadata.named("ARCHIVETIMESTAMP").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(assignmentowner, ColumnMetadata.named("ASSIGNMENTOWNER").withIndex(13).ofType(Types.INTEGER).withSize(10));
        addMetadata(createchannel, ColumnMetadata.named("CREATECHANNEL").withIndex(14).ofType(Types.VARCHAR).withSize(255));
        addMetadata(createtimestamp, ColumnMetadata.named("CREATETIMESTAMP").withIndex(15).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(creatorrefRelation, ColumnMetadata.named("CREATORREF_RELATION").withIndex(16).ofType(Types.VARCHAR).withSize(157));
        addMetadata(creatorrefTargetoid, ColumnMetadata.named("CREATORREF_TARGETOID").withIndex(17).ofType(Types.VARCHAR).withSize(36));
        addMetadata(creatorrefTargettype, ColumnMetadata.named("CREATORREF_TARGETTYPE").withIndex(40).ofType(Types.INTEGER).withSize(10));
        addMetadata(creatorrefType, ColumnMetadata.named("CREATORREF_TYPE").withIndex(18).ofType(Types.INTEGER).withSize(10));
        addMetadata(disablereason, ColumnMetadata.named("DISABLEREASON").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(disabletimestamp, ColumnMetadata.named("DISABLETIMESTAMP").withIndex(6).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(effectivestatus, ColumnMetadata.named("EFFECTIVESTATUS").withIndex(7).ofType(Types.INTEGER).withSize(10));
        addMetadata(enabletimestamp, ColumnMetadata.named("ENABLETIMESTAMP").withIndex(8).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(extid, ColumnMetadata.named("EXTID").withIndex(38).ofType(Types.INTEGER).withSize(10));
        addMetadata(extoid, ColumnMetadata.named("EXTOID").withIndex(39).ofType(Types.VARCHAR).withSize(36));
        addMetadata(id, ColumnMetadata.named("ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(lifecyclestate, ColumnMetadata.named("LIFECYCLESTATE").withIndex(19).ofType(Types.VARCHAR).withSize(255));
        addMetadata(modifierrefRelation, ColumnMetadata.named("MODIFIERREF_RELATION").withIndex(20).ofType(Types.VARCHAR).withSize(157));
        addMetadata(modifierrefTargetoid, ColumnMetadata.named("MODIFIERREF_TARGETOID").withIndex(21).ofType(Types.VARCHAR).withSize(36));
        addMetadata(modifierrefTargettype, ColumnMetadata.named("MODIFIERREF_TARGETTYPE").withIndex(41).ofType(Types.INTEGER).withSize(10));
        addMetadata(modifierrefType, ColumnMetadata.named("MODIFIERREF_TYPE").withIndex(22).ofType(Types.INTEGER).withSize(10));
        addMetadata(modifychannel, ColumnMetadata.named("MODIFYCHANNEL").withIndex(23).ofType(Types.VARCHAR).withSize(255));
        addMetadata(modifytimestamp, ColumnMetadata.named("MODIFYTIMESTAMP").withIndex(24).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(ordervalue, ColumnMetadata.named("ORDERVALUE").withIndex(25).ofType(Types.INTEGER).withSize(10));
        addMetadata(orgrefRelation, ColumnMetadata.named("ORGREF_RELATION").withIndex(26).ofType(Types.VARCHAR).withSize(157));
        addMetadata(orgrefTargetoid, ColumnMetadata.named("ORGREF_TARGETOID").withIndex(27).ofType(Types.VARCHAR).withSize(36));
        addMetadata(orgrefTargettype, ColumnMetadata.named("ORGREF_TARGETTYPE").withIndex(42).ofType(Types.INTEGER).withSize(10));
        addMetadata(orgrefType, ColumnMetadata.named("ORGREF_TYPE").withIndex(28).ofType(Types.INTEGER).withSize(10));
        addMetadata(ownerOid, ColumnMetadata.named("OWNER_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(resourcerefRelation, ColumnMetadata.named("RESOURCEREF_RELATION").withIndex(29).ofType(Types.VARCHAR).withSize(157));
        addMetadata(resourcerefTargetoid, ColumnMetadata.named("RESOURCEREF_TARGETOID").withIndex(30).ofType(Types.VARCHAR).withSize(36));
        addMetadata(resourcerefTargettype, ColumnMetadata.named("RESOURCEREF_TARGETTYPE").withIndex(43).ofType(Types.INTEGER).withSize(10));
        addMetadata(resourcerefType, ColumnMetadata.named("RESOURCEREF_TYPE").withIndex(31).ofType(Types.INTEGER).withSize(10));
        addMetadata(targetrefRelation, ColumnMetadata.named("TARGETREF_RELATION").withIndex(32).ofType(Types.VARCHAR).withSize(157));
        addMetadata(targetrefTargetoid, ColumnMetadata.named("TARGETREF_TARGETOID").withIndex(33).ofType(Types.VARCHAR).withSize(36));
        addMetadata(targetrefTargettype, ColumnMetadata.named("TARGETREF_TARGETTYPE").withIndex(44).ofType(Types.INTEGER).withSize(10));
        addMetadata(targetrefType, ColumnMetadata.named("TARGETREF_TYPE").withIndex(34).ofType(Types.INTEGER).withSize(10));
        addMetadata(tenantrefRelation, ColumnMetadata.named("TENANTREF_RELATION").withIndex(35).ofType(Types.VARCHAR).withSize(157));
        addMetadata(tenantrefTargetoid, ColumnMetadata.named("TENANTREF_TARGETOID").withIndex(36).ofType(Types.VARCHAR).withSize(36));
        addMetadata(tenantrefTargettype, ColumnMetadata.named("TENANTREF_TARGETTYPE").withIndex(45).ofType(Types.INTEGER).withSize(10));
        addMetadata(tenantrefType, ColumnMetadata.named("TENANTREF_TYPE").withIndex(37).ofType(Types.INTEGER).withSize(10));
        addMetadata(validfrom, ColumnMetadata.named("VALIDFROM").withIndex(9).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(validitychangetimestamp, ColumnMetadata.named("VALIDITYCHANGETIMESTAMP").withIndex(11).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(validitystatus, ColumnMetadata.named("VALIDITYSTATUS").withIndex(12).ofType(Types.INTEGER).withSize(10));
        addMetadata(validto, ColumnMetadata.named("VALIDTO").withIndex(10).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
    }

}

