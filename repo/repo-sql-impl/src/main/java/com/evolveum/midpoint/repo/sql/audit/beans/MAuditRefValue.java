/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.beans;

import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditRefValue;

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
