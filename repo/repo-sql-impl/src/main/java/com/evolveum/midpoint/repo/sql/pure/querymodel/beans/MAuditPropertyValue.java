package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

/**
 * Querydsl "row bean" type related to {@code QAuditPropertyValue}.
 */
public class MAuditPropertyValue {

    public Long id;
    public Long recordId;
    public String name;
    public String value;

    @Override
    public String toString() {
        return "MAuditPropertyValue{" +
                "id=" + id +
                ", recordId=" + recordId +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
