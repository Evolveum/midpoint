package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMObject is a Querydsl query type for QMObject
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMObject extends com.querydsl.sql.RelationalPathBase<QMObject> {

    private static final long serialVersionUID = -108212085;

    public static final QMObject mObject = new QMObject("M_OBJECT");

    public final StringPath createchannel = createString("createchannel");

    public final DateTimePath<java.sql.Timestamp> createtimestamp = createDateTime("createtimestamp", java.sql.Timestamp.class);

    public final StringPath creatorrefRelation = createString("creatorrefRelation");

    public final StringPath creatorrefTargetoid = createString("creatorrefTargetoid");

    public final NumberPath<Integer> creatorrefTargettype = createNumber("creatorrefTargettype", Integer.class);

    public final NumberPath<Integer> creatorrefType = createNumber("creatorrefType", Integer.class);

    public final SimplePath<java.sql.Blob> fullobject = createSimple("fullobject", java.sql.Blob.class);

    public final StringPath lifecyclestate = createString("lifecyclestate");

    public final StringPath modifierrefRelation = createString("modifierrefRelation");

    public final StringPath modifierrefTargetoid = createString("modifierrefTargetoid");

    public final NumberPath<Integer> modifierrefTargettype = createNumber("modifierrefTargettype", Integer.class);

    public final NumberPath<Integer> modifierrefType = createNumber("modifierrefType", Integer.class);

    public final StringPath modifychannel = createString("modifychannel");

    public final DateTimePath<java.sql.Timestamp> modifytimestamp = createDateTime("modifytimestamp", java.sql.Timestamp.class);

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final NumberPath<Integer> objecttypeclass = createNumber("objecttypeclass", Integer.class);

    public final StringPath oid = createString("oid");

    public final StringPath tenantrefRelation = createString("tenantrefRelation");

    public final StringPath tenantrefTargetoid = createString("tenantrefTargetoid");

    public final NumberPath<Integer> tenantrefTargettype = createNumber("tenantrefTargettype", Integer.class);

    public final NumberPath<Integer> tenantrefType = createNumber("tenantrefType", Integer.class);

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public final com.querydsl.sql.PrimaryKey<QMObject> constraint6a = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMOrgClosure> _ancestorFk = createInvForeignKey(oid, "ANCESTOR_OID");

    public final com.querydsl.sql.ForeignKey<QMAccCertDefinition> _accCertDefinitionFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMObjectExtBoolean> _oExtBooleanOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMObjectTextInfo> _objectTextInfoOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMAssignment> _assignmentOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMReference> _referenceOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMResource> _resourceFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMDashboard> _dashboardFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMSecurityPolicy> _securityPolicyFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMSystemConfiguration> _systemConfigurationFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMTrigger> _triggerOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMShadow> _shadowFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMObjectExtDate> _oExtDateOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMObjectExtLong> _objectExtLongFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMLookupTable> _lookupTableFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMObjectExtPoly> _oExtPolyOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMReport> _reportFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMConnector> _connectorFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMObjectExtReference> _oExtReferenceOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMObjectExtString> _objectExtStringFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMObjectSubtype> _objectSubtypeFk = createInvForeignKey(oid, "OBJECT_OID");

    public final com.querydsl.sql.ForeignKey<QMFocus> _focusFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMAccCertCampaign> _accCertCampaignFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMCase> _caseFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMReportOutput> _reportOutputFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMOperationExecution> _opExecOwnerFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMValuePolicy> _valuePolicyFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMConnectorHost> _connectorHostFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMObjectTemplate> _objectTemplateFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMOrgClosure> _descendantFk = createInvForeignKey(oid, "DESCENDANT_OID");

    public final com.querydsl.sql.ForeignKey<QMFunctionLibrary> _functionLibraryFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMTask> _taskFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMNode> _nodeFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMSequence> _sequenceFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMForm> _formFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMObjectCollection> _objectCollectionFk = createInvForeignKey(oid, "OID");

    public QMObject(String variable) {
        super(QMObject.class, forVariable(variable), "PUBLIC", "M_OBJECT");
        addMetadata();
    }

    public QMObject(String variable, String schema, String table) {
        super(QMObject.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMObject(String variable, String schema) {
        super(QMObject.class, forVariable(variable), schema, "M_OBJECT");
        addMetadata();
    }

    public QMObject(Path<? extends QMObject> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_OBJECT");
        addMetadata();
    }

    public QMObject(PathMetadata metadata) {
        super(QMObject.class, metadata, "PUBLIC", "M_OBJECT");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(createchannel, ColumnMetadata.named("CREATECHANNEL").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(createtimestamp, ColumnMetadata.named("CREATETIMESTAMP").withIndex(3).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(creatorrefRelation, ColumnMetadata.named("CREATORREF_RELATION").withIndex(4).ofType(Types.VARCHAR).withSize(157));
        addMetadata(creatorrefTargetoid, ColumnMetadata.named("CREATORREF_TARGETOID").withIndex(5).ofType(Types.VARCHAR).withSize(36));
        addMetadata(creatorrefTargettype, ColumnMetadata.named("CREATORREF_TARGETTYPE").withIndex(21).ofType(Types.INTEGER).withSize(10));
        addMetadata(creatorrefType, ColumnMetadata.named("CREATORREF_TYPE").withIndex(6).ofType(Types.INTEGER).withSize(10));
        addMetadata(fullobject, ColumnMetadata.named("FULLOBJECT").withIndex(7).ofType(Types.BLOB).withSize(2147483647));
        addMetadata(lifecyclestate, ColumnMetadata.named("LIFECYCLESTATE").withIndex(8).ofType(Types.VARCHAR).withSize(255));
        addMetadata(modifierrefRelation, ColumnMetadata.named("MODIFIERREF_RELATION").withIndex(9).ofType(Types.VARCHAR).withSize(157));
        addMetadata(modifierrefTargetoid, ColumnMetadata.named("MODIFIERREF_TARGETOID").withIndex(10).ofType(Types.VARCHAR).withSize(36));
        addMetadata(modifierrefTargettype, ColumnMetadata.named("MODIFIERREF_TARGETTYPE").withIndex(22).ofType(Types.INTEGER).withSize(10));
        addMetadata(modifierrefType, ColumnMetadata.named("MODIFIERREF_TYPE").withIndex(11).ofType(Types.INTEGER).withSize(10));
        addMetadata(modifychannel, ColumnMetadata.named("MODIFYCHANNEL").withIndex(12).ofType(Types.VARCHAR).withSize(255));
        addMetadata(modifytimestamp, ColumnMetadata.named("MODIFYTIMESTAMP").withIndex(13).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(14).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(15).ofType(Types.VARCHAR).withSize(255));
        addMetadata(objecttypeclass, ColumnMetadata.named("OBJECTTYPECLASS").withIndex(16).ofType(Types.INTEGER).withSize(10));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(tenantrefRelation, ColumnMetadata.named("TENANTREF_RELATION").withIndex(17).ofType(Types.VARCHAR).withSize(157));
        addMetadata(tenantrefTargetoid, ColumnMetadata.named("TENANTREF_TARGETOID").withIndex(18).ofType(Types.VARCHAR).withSize(36));
        addMetadata(tenantrefTargettype, ColumnMetadata.named("TENANTREF_TARGETTYPE").withIndex(23).ofType(Types.INTEGER).withSize(10));
        addMetadata(tenantrefType, ColumnMetadata.named("TENANTREF_TYPE").withIndex(19).ofType(Types.INTEGER).withSize(10));
        addMetadata(version, ColumnMetadata.named("VERSION").withIndex(20).ofType(Types.INTEGER).withSize(10).notNull());
    }

}

