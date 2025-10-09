/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
