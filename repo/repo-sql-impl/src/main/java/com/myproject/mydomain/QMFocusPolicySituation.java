package com.myproject.mydomain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.Generated;
import com.querydsl.core.types.Path;

import com.querydsl.sql.ColumnMetadata;
import java.sql.Types;




/**
 * QMFocusPolicySituation is a Querydsl query type for QMFocusPolicySituation
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMFocusPolicySituation extends com.querydsl.sql.RelationalPathBase<QMFocusPolicySituation> {

    private static final long serialVersionUID = 124099904;

    public static final QMFocusPolicySituation mFocusPolicySituation = new QMFocusPolicySituation("M_FOCUS_POLICY_SITUATION");

    public final StringPath focusOid = createString("focusOid");

    public final StringPath policysituation = createString("policysituation");

    public final com.querydsl.sql.ForeignKey<QMFocus> focusPolicySituationFk = createForeignKey(focusOid, "OID");

    public QMFocusPolicySituation(String variable) {
        super(QMFocusPolicySituation.class, forVariable(variable), "PUBLIC", "M_FOCUS_POLICY_SITUATION");
        addMetadata();
    }

    public QMFocusPolicySituation(String variable, String schema, String table) {
        super(QMFocusPolicySituation.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMFocusPolicySituation(String variable, String schema) {
        super(QMFocusPolicySituation.class, forVariable(variable), schema, "M_FOCUS_POLICY_SITUATION");
        addMetadata();
    }

    public QMFocusPolicySituation(Path<? extends QMFocusPolicySituation> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_FOCUS_POLICY_SITUATION");
        addMetadata();
    }

    public QMFocusPolicySituation(PathMetadata metadata) {
        super(QMFocusPolicySituation.class, metadata, "PUBLIC", "M_FOCUS_POLICY_SITUATION");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(focusOid, ColumnMetadata.named("FOCUS_OID").withIndex(1).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(policysituation, ColumnMetadata.named("POLICYSITUATION").withIndex(2).ofType(Types.VARCHAR).withSize(255));
    }

}

