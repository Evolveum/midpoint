/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.beans;

import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditPropertyValue;

/**
 * Querydsl "row bean" type related to {@link QAuditPropertyValue}.
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
