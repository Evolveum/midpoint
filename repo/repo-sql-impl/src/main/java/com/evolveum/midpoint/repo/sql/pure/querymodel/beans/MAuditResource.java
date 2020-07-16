package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditResource;

/**
 * Querydsl "row bean" type related to {@link QAuditResource}.
 */
@SuppressWarnings("unused")
public class MAuditResource {

    public Long recordId;
    public String resourceOid;

    @Override
    public String toString() {
        return "MAuditItem{" +
                "recordId=" + recordId +
                ", resourceOid='" + resourceOid + '\'' +
                '}';
    }
}
