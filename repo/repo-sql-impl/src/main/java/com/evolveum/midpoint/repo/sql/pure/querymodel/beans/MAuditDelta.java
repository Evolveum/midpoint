package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import java.sql.Blob;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditDelta;

/**
 * Querydsl "row bean" type related to {@link QAuditDelta}.
 */
@SuppressWarnings("unused")
public class MAuditDelta {

    // TODO why is checksum part of PK? why not (recordId, deltaNumber) or even totally unique single ID?
    public Long recordId;
    public String checksum;
    public Blob delta;
    public String deltaOid;
    public Integer deltaType;
    public Blob fullResult;
    public String objectNameNorm;
    public String objectNameOrig;
    public String resourceNameNorm;
    public String resourceNameOrig;
    public String resourceOid;
    public Integer status;

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
                ", checksum='" + checksum + '\'' +
                ", delta=" + delta +
                ", deltaOid='" + deltaOid + '\'' +
                ", deltaType=" + deltaType +
                ", fullResult=" + fullResult +
                ", objectNameNorm='" + objectNameNorm + '\'' +
                ", objectNameOrig='" + objectNameOrig + '\'' +
                ", resourceNameNorm='" + resourceNameNorm + '\'' +
                ", resourceNameOrig='" + resourceNameOrig + '\'' +
                ", resourceOid='" + resourceOid + '\'' +
                ", status=" + status +
                '}';
    }
}
