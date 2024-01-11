package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.metadata.QValueMetadata;
import com.evolveum.midpoint.repo.sqale.qmodel.metadata.QValueMetadataMapping;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;

import java.sql.Types;

public class QAssignmentMetadata extends QValueMetadata<MAssignmentMetadata, MAssignment> {

    public static final String TABLE_NAME = "m_assignment_metadata";
    public static final String ALIAS = "am";

    public static final ColumnMetadata ASSIGNMENT_CID =
            ColumnMetadata.named("assignmentCid").ofType(Types.BIGINT).notNull();

    public final NumberPath<Long> assignmentCid = createLong("assignmentCid", ASSIGNMENT_CID);

    public QAssignmentMetadata(String variable) {
        super(MAssignmentMetadata.class, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    @Override
    public BooleanExpression isOwnedBy(MAssignment ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
                .and(assignmentCid.eq(ownerRow.cid));
    }
}
