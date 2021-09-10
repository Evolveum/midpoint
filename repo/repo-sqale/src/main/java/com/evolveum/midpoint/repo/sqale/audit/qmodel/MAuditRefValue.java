/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

/**
 * Querydsl "row bean" type related to {@link QAuditRefValue}.
 */
@SuppressWarnings("unused")
public class MAuditRefValue {

    public Long id;
    public Long recordId;
    public Instant timestamp;
    public String name;
    public UUID targetOid;
    public MObjectType targetType;
    public String targetNameNorm;
    public String targetNameOrig;

    @Override
    public String toString() {
        return "MAuditRefValue{" +
                "id=" + id +
                ", recordId=" + recordId +
                ", timestamp=" + timestamp +
                ", name='" + name + '\'' +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", targetNameNorm='" + targetNameNorm + '\'' +
                ", targetNameOrig='" + targetNameOrig + '\'' +
                '}';
    }
}
