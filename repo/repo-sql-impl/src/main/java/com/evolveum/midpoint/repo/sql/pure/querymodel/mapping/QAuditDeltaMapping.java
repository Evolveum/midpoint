package com.evolveum.midpoint.repo.sql.pure.querymodel.mapping;

import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditItem.*;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.pure.SqlTransformer;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditRefValue;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;

/**
 * Mapping between {@link QAuditDelta} and {@link ObjectDeltaOperationType}.
 */
public class QAuditDeltaMapping
        extends QueryModelMapping<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> {

    public static final String DEFAULT_ALIAS_NAME = "ad";

    public static final QAuditDeltaMapping INSTANCE = new QAuditDeltaMapping();

    private QAuditDeltaMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectDeltaOperationType.class, QAuditDelta.class,
                RECORD_ID, CHANGED_ITEM_PATH);
    }

    @Override
    public SqlTransformer<ObjectDeltaOperationType, MAuditDelta> createTransformer(
            PrismContext prismContext) {
        throw new UnsupportedOperationException("handled by AuditEventRecordSqlTransformer");
    }
}
