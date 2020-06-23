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
 * QMAssignmentPolicySituation is a Querydsl query type for QMAssignmentPolicySituation
 */
@Generated("com.querydsl.sql.codegen.MetaDataSerializer")
public class QMAssignmentPolicySituation extends com.querydsl.sql.RelationalPathBase<QMAssignmentPolicySituation> {

    private static final long serialVersionUID = 690880915;

    public static final QMAssignmentPolicySituation mAssignmentPolicySituation = new QMAssignmentPolicySituation("M_ASSIGNMENT_POLICY_SITUATION");

    public final NumberPath<Integer> assignmentId = createNumber("assignmentId", Integer.class);

    public final StringPath assignmentOid = createString("assignmentOid");

    public final StringPath policysituation = createString("policysituation");

    public final com.querydsl.sql.ForeignKey<QMAssignment> assignmentPolicySituationFk = createForeignKey(Arrays.asList(assignmentId, assignmentOid), Arrays.asList("ID", "OWNER_OID"));

    public QMAssignmentPolicySituation(String variable) {
        super(QMAssignmentPolicySituation.class, forVariable(variable), "PUBLIC", "M_ASSIGNMENT_POLICY_SITUATION");
        addMetadata();
    }

    public QMAssignmentPolicySituation(String variable, String schema, String table) {
        super(QMAssignmentPolicySituation.class, forVariable(variable), schema, table);
        addMetadata();
    }

    public QMAssignmentPolicySituation(String variable, String schema) {
        super(QMAssignmentPolicySituation.class, forVariable(variable), schema, "M_ASSIGNMENT_POLICY_SITUATION");
        addMetadata();
    }

    public QMAssignmentPolicySituation(Path<? extends QMAssignmentPolicySituation> path) {
        super(path.getType(), path.getMetadata(), "PUBLIC", "M_ASSIGNMENT_POLICY_SITUATION");
        addMetadata();
    }

    public QMAssignmentPolicySituation(PathMetadata metadata) {
        super(QMAssignmentPolicySituation.class, metadata, "PUBLIC", "M_ASSIGNMENT_POLICY_SITUATION");
        addMetadata();
    }

    public void addMetadata() {
        addMetadata(assignmentId, ColumnMetadata.named("ASSIGNMENT_ID").withIndex(1).ofType(Types.INTEGER).withSize(10).notNull());
        addMetadata(assignmentOid, ColumnMetadata.named("ASSIGNMENT_OID").withIndex(2).ofType(Types.VARCHAR).withSize(36).notNull());
        addMetadata(policysituation, ColumnMetadata.named("POLICYSITUATION").withIndex(3).ofType(Types.VARCHAR).withSize(255));
    }

}

