/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.beans;

import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem;

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
