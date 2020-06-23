package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMFocus is a Querydsl query type for QMFocus
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMFocus extends com.querydsl.sql.RelationalPathBase<QMFocus> {

    private static final long serialVersionUID = 1235504652;

    public static final QMFocus mFocus = new QMFocus("M_FOCUS");

    public final NumberPath<Integer> administrativestatus = createNumber("administrativestatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> archivetimestamp = createDateTime("archivetimestamp", java.sql.Timestamp.class);

    public final StringPath costcenter = createString("costcenter");

    public final StringPath disablereason = createString("disablereason");

    public final DateTimePath<java.sql.Timestamp> disabletimestamp = createDateTime("disabletimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> effectivestatus = createNumber("effectivestatus", Integer.class);

    public final StringPath emailaddress = createString("emailaddress");

    public final DateTimePath<java.sql.Timestamp> enabletimestamp = createDateTime("enabletimestamp", java.sql.Timestamp.class);

    public final BooleanPath hasphoto = createBoolean("hasphoto");

    public final StringPath locale = createString("locale");

    public final StringPath localityNorm = createString("localityNorm");

    public final StringPath localityOrig = createString("localityOrig");

    public final StringPath oid = createString("oid");

    public final StringPath preferredlanguage = createString("preferredlanguage");

    public final StringPath telephonenumber = createString("telephonenumber");

    public final StringPath timezone = createString("timezone");

    public final DateTimePath<java.sql.Timestamp> validfrom = createDateTime("validfrom", java.sql.Timestamp.class);

    public final DateTimePath<java.sql.Timestamp> validitychangetimestamp = createDateTime("validitychangetimestamp", java.sql.Timestamp.class);

    public final NumberPath<Integer> validitystatus = createNumber("validitystatus", Integer.class);

    public final DateTimePath<java.sql.Timestamp> validto = createDateTime("validto", java.sql.Timestamp.class);

    public final com.querydsl.sql.PrimaryKey<QMFocus> constraint8f = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMObject> focusFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMFocusPolicySituation> _focusPolicySituationFk = createInvForeignKey(oid, "FOCUS_OID");

    public final com.querydsl.sql.ForeignKey<QMAbstractRole> _abstractRoleFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMUser> _userFk = createInvForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMFocusPhoto> _focusPhotoFk = createInvForeignKey(oid, "OWNER_OID");

    public final com.querydsl.sql.ForeignKey<QMGenericObject> _genericObjectFk = createInvForeignKey(oid, "OID");

    public QMFocus(String variable) {
        super(QMFocus.class, forVariable(variable), "PUBLIC", "M_FOCUS");
        addMetadata();
    }

    public QMFocus(String variable, String schema, String table) {
        super(QMFocus.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMFocus(String variable, String schema) {
        super(QMFocus.class, forVariable(variable), schema, "M_FOCUS");
        addMetadata();
    }

    public QMFocus(Path<? extends QMFocus> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_FOCUS");
        addMetadata();
    }

    public QMFocus(PathMetadata metadata) {
        super(QMFocus.class, metadata, "PUBLIC", "M_FOCUS");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(administrativestatus, ColumnMetadata.named("ADMINISTRATIVESTATUS").withIndex(1).ofType(Types.INTEGER).withSize(10));
        addMetadata(archivetimestamp, ColumnMetadata.named("ARCHIVETIMESTAMP").withIndex(2).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(costcenter, ColumnMetadata.named("COSTCENTER").withIndex(11).ofType(Types.VARCHAR).withSize(255));
        addMetadata(disablereason, ColumnMetadata.named("DISABLEREASON").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(disabletimestamp, ColumnMetadata.named("DISABLETIMESTAMP").withIndex(4).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(effectivestatus, ColumnMetadata.named("EFFECTIVESTATUS").withIndex(5).ofType(Types.INTEGER).withSize(10));
        addMetadata(emailaddress, ColumnMetadata.named("EMAILADDRESS").withIndex(12).ofType(Types.VARCHAR).withSize(255));
        addMetadata(enabletimestamp, ColumnMetadata.named("ENABLETIMESTAMP").withIndex(6).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(hasphoto, ColumnMetadata.named("HASPHOTO").withIndex(13).ofType(Types.BOOLEAN).withSize(1).notNull());
        addMetadata(locale, ColumnMetadata.named("LOCALE").withIndex(14).ofType(Types.VARCHAR).withSize(255));
        addMetadata(localityNorm, ColumnMetadata.named("LOCALITY_NORM").withIndex(15).ofType(Types.VARCHAR).withSize(255));
        addMetadata(localityOrig, ColumnMetadata.named("LOCALITY_ORIG").withIndex(16).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(20).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(preferredlanguage, ColumnMetadata.named("PREFERREDLANGUAGE").withIndex(17).ofType(Types.VARCHAR).withSize(255));
        addMetadata(telephonenumber, ColumnMetadata.named("TELEPHONENUMBER").withIndex(18).ofType(Types.VARCHAR).withSize(255));
        addMetadata(timezone, ColumnMetadata.named("TIMEZONE").withIndex(19).ofType(Types.VARCHAR).withSize(255));
        addMetadata(validfrom, ColumnMetadata.named("VALIDFROM").withIndex(7).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(validitychangetimestamp, ColumnMetadata.named("VALIDITYCHANGETIMESTAMP").withIndex(9).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
        addMetadata(validitystatus, ColumnMetadata.named("VALIDITYSTATUS").withIndex(10).ofType(Types.INTEGER).withSize(10));
        addMetadata(validto, ColumnMetadata.named("VALIDTO").withIndex(8).ofType(Types.TIMESTAMP).withSize(23).withDigits(10));
    }

}

