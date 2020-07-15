package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditItem;

/**
 * Querydsl "row bean" type related to {@link QAuditItem}.
 */
@SuppressWarnings("unused")
public class MAuditItem {

    public Long recordId;
    public String changedItemPath;

    @Override
    public String toString() {
        return "MAuditItem{" +
                "recordId=" + recordId +
                ", changedItemPath='" + changedItemPath + '\'' +
                '}';
    }
}
