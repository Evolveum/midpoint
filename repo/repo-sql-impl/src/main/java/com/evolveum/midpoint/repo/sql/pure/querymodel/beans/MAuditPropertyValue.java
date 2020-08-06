/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditPropertyValue;

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
