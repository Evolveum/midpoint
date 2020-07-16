package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditRefValue;

/**
 * Querydsl "row bean" type related to {@link QAuditRefValue}.
 */
@SuppressWarnings("unused")
public class MAuditRefValue {

    public Long id;
    public Long recordId;
    public String name;
    public String oid;
    public String targetNameNorm;
    public String targetNameOrig;
    public String type;

    @Override
    public String toString() {
        return "MAuditRefValue{" +
                "id=" + id +
                ", recordId=" + recordId +
                ", name='" + name + '\'' +
                ", oid='" + oid + '\'' +
                ", targetNameNorm='" + targetNameNorm + '\'' +
                ", targetNameOrig='" + targetNameOrig + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
