package com.evolveum.midpoint.repo.sql.pure;

import com.evolveum.midpoint.audit.api.AuditEventRecord;

/**
 * Simple class with static methods for audit event transformation between repo and Prism world.
 */
public class AuditEventRecordSqlTransformer {

    public static AuditEventRecord toAuditEventRecord(/* not sure yet */) {
        AuditEventRecord rv = new AuditEventRecord();
        // todo transformation from whatever input
        return rv;
    }
}
