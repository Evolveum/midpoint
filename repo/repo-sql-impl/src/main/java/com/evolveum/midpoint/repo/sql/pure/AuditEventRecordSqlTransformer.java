package com.evolveum.midpoint.repo.sql.pure;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Simple class with static methods for audit event transformation between repo and Prism world.
 */
public class AuditEventRecordSqlTransformer {

    public static AuditEventRecordType toAuditEventRecordType(MAuditEventRecord row) {
        AuditEventRecordType rv = new AuditEventRecordType();
        // todo transformation from whatever input
//        rv.set where is repo ID? is it not necessary?

        return rv;
    }
}
