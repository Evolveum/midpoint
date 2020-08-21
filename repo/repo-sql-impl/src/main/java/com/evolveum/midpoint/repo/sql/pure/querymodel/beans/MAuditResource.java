/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
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
