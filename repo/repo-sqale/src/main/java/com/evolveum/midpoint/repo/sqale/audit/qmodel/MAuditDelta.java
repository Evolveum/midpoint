/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

/**
 * Querydsl "row bean" type related to {@link QAuditDelta}.
 */
@SuppressWarnings("unused")
public class MAuditDelta {

    public Long recordId;
    public Instant timestamp;
    public String checksum;
    public byte[] delta;
    public UUID deltaOid;
    public ChangeTypeType deltaType;
    public byte[] fullResult; // not just full, can be cleaned-up, top or even none
    public String objectNameNorm;
    public String objectNameOrig;
    public UUID resourceOid;
    public String resourceNameNorm;
    public String resourceNameOrig;

    public ShadowKindType shadowKind;
    public String shadowIntent;

    public OperationResultStatusType status;

    // "transient" fields not used by Querydsl
    public String serializedDelta;

    public PolyString getObjectName() {
        return new PolyString(objectNameOrig, objectNameNorm);
    }

    public PolyString getResourceName() {
        return new PolyString(resourceNameOrig, resourceNameNorm);
    }

    @Override
    public String toString() {
        return "MAuditDelta{" +
                "recordId=" + recordId +
                ", timestamp=" + timestamp +
                ", checksum='" + checksum + '\'' +
                ", delta=" + MiscUtil.bytesToHexPreview(delta) +
                ", deltaOid=" + deltaOid +
                ", deltaType=" + deltaType +
                ", fullResult=" + MiscUtil.bytesToHexPreview(fullResult) +
                ", objectNameNorm='" + objectNameNorm + '\'' +
                ", objectNameOrig='" + objectNameOrig + '\'' +
                ", resourceOid=" + resourceOid +
                ", resourceNameNorm='" + resourceNameNorm + '\'' +
                ", resourceNameOrig='" + resourceNameOrig + '\'' +
                ", status=" + status +
                '}';
    }
}
