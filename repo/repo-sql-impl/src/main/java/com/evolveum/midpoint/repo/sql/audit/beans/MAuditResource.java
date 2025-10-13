/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.beans;

import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditResource;

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
