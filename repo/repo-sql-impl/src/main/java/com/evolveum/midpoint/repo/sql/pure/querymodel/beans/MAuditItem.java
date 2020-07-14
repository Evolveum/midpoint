package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditItem;

/**
 * Querydsl "row bean" type related to {@link QAuditItem}.
 */
@SuppressWarnings("unused")
public class MAuditItem {

    private Long recordId;
    private String changedItemPath;

    @Override
    public String toString() {
        return "MAuditItem{" +
                "recordId=" + recordId +
                ", changedItemPath='" + changedItemPath + '\'' +
                '}';
    }
}
