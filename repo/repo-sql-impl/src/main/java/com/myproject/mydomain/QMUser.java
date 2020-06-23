package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMUser is a Querydsl query type for QMUser
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMUser extends com.querydsl.sql.RelationalPathBase<QMUser> {

    private static final long serialVersionUID = 2118515735;

    public static final QMUser mUser = new QMUser("M_USER");

    public final StringPath additionalnameNorm = createString("additionalnameNorm");

    public final StringPath additionalnameOrig = createString("additionalnameOrig");

    public final StringPath employeenumber = createString("employeenumber");

    public final StringPath familynameNorm = createString("familynameNorm");

    public final StringPath familynameOrig = createString("familynameOrig");

    public final StringPath fullnameNorm = createString("fullnameNorm");

    public final StringPath fullnameOrig = createString("fullnameOrig");

    public final StringPath givennameNorm = createString("givennameNorm");

    public final StringPath givennameOrig = createString("givennameOrig");

    public final StringPath honorificprefixNorm = createString("honorificprefixNorm");

    public final StringPath honorificprefixOrig = createString("honorificprefixOrig");

    public final StringPath honorificsuffixNorm = createString("honorificsuffixNorm");

    public final StringPath honorificsuffixOrig = createString("honorificsuffixOrig");

    public final StringPath nameNorm = createString("nameNorm");

    public final StringPath nameOrig = createString("nameOrig");

    public final StringPath nicknameNorm = createString("nicknameNorm");

    public final StringPath nicknameOrig = createString("nicknameOrig");

    public final StringPath oid = createString("oid");

    public final StringPath titleNorm = createString("titleNorm");

    public final StringPath titleOrig = createString("titleOrig");

    public final com.querydsl.sql.PrimaryKey<QMUser> constraint88c7 = createPrimaryKey(oid);

    public final com.querydsl.sql.ForeignKey<QMFocus> userFk = createForeignKey(oid, "OID");

    public final com.querydsl.sql.ForeignKey<QMUserOrganizationalUnit> _userOrgUnitFk = createInvForeignKey(oid, "USER_OID");

    public final com.querydsl.sql.ForeignKey<QMUserEmployeeType> _userEmployeeTypeFk = createInvForeignKey(oid, "USER_OID");

    public final com.querydsl.sql.ForeignKey<QMUserOrganization> _userOrganizationFk = createInvForeignKey(oid, "USER_OID");

    public QMUser(String variable) {
        super(QMUser.class, forVariable(variable), "PUBLIC", "M_USER");
        addMetadata();
    }

    public QMUser(String variable, String schema, String table) {
        super(QMUser.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMUser(String variable, String schema) {
        super(QMUser.class, forVariable(variable), schema, "M_USER");
        addMetadata();
    }

    public QMUser(Path<? extends QMUser> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_USER");
        addMetadata();
    }

    public QMUser(PathMetadata metadata) {
        super(QMUser.class, metadata, "PUBLIC", "M_USER");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(additionalnameNorm, ColumnMetadata.named("ADDITIONALNAME_NORM").withIndex(1).ofType(Types.VARCHAR).withSize(255));
        addMetadata(additionalnameOrig, ColumnMetadata.named("ADDITIONALNAME_ORIG").withIndex(2).ofType(Types.VARCHAR).withSize(255));
        addMetadata(employeenumber, ColumnMetadata.named("EMPLOYEENUMBER").withIndex(3).ofType(Types.VARCHAR).withSize(255));
        addMetadata(familynameNorm, ColumnMetadata.named("FAMILYNAME_NORM").withIndex(4).ofType(Types.VARCHAR).withSize(255));
        addMetadata(familynameOrig, ColumnMetadata.named("FAMILYNAME_ORIG").withIndex(5).ofType(Types.VARCHAR).withSize(255));
        addMetadata(fullnameNorm, ColumnMetadata.named("FULLNAME_NORM").withIndex(6).ofType(Types.VARCHAR).withSize(255));
        addMetadata(fullnameOrig, ColumnMetadata.named("FULLNAME_ORIG").withIndex(7).ofType(Types.VARCHAR).withSize(255));
        addMetadata(givennameNorm, ColumnMetadata.named("GIVENNAME_NORM").withIndex(8).ofType(Types.VARCHAR).withSize(255));
        addMetadata(givennameOrig, ColumnMetadata.named("GIVENNAME_ORIG").withIndex(9).ofType(Types.VARCHAR).withSize(255));
        addMetadata(honorificprefixNorm, ColumnMetadata.named("HONORIFICPREFIX_NORM").withIndex(10).ofType(Types.VARCHAR).withSize(255));
        addMetadata(honorificprefixOrig, ColumnMetadata.named("HONORIFICPREFIX_ORIG").withIndex(11).ofType(Types.VARCHAR).withSize(255));
        addMetadata(honorificsuffixNorm, ColumnMetadata.named("HONORIFICSUFFIX_NORM").withIndex(12).ofType(Types.VARCHAR).withSize(255));
        addMetadata(honorificsuffixOrig, ColumnMetadata.named("HONORIFICSUFFIX_ORIG").withIndex(13).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameNorm, ColumnMetadata.named("NAME_NORM").withIndex(14).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nameOrig, ColumnMetadata.named("NAME_ORIG").withIndex(15).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nicknameNorm, ColumnMetadata.named("NICKNAME_NORM").withIndex(16).ofType(Types.VARCHAR).withSize(255));
        addMetadata(nicknameOrig, ColumnMetadata.named("NICKNAME_ORIG").withIndex(17).ofType(Types.VARCHAR).withSize(255));
        addMetadata(oid, ColumnMetadata.named("OID").withIndex(20).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(titleNorm, ColumnMetadata.named("TITLE_NORM").withIndex(18).ofType(Types.VARCHAR).withSize(255));
        addMetadata(titleOrig, ColumnMetadata.named("TITLE_ORIG").withIndex(19).ofType(Types.VARCHAR).withSize(255));
    }

}

