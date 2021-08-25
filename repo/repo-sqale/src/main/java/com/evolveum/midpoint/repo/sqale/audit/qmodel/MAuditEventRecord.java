/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import java.time.Instant;
import java.util.*;

import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/** Querydsl "row bean" type related to {@link QAuditEventRecord}. */
@SuppressWarnings("unused")
public class MAuditEventRecord {

    public Long id;
    public Instant timestamp;
    public String eventIdentifier;
    public AuditEventTypeType eventType;
    public AuditEventStageType eventStage;
    public String sessionIdentifier;
    public String requestIdentifier;
    public String taskIdentifier;
    public UUID taskOid;
    public String hostIdentifier;
    public String nodeIdentifier;
    public String remoteHostAddress;
    public UUID initiatorOid;
    public MObjectType initiatorType;
    public String initiatorName;
    public UUID attorneyOid;
    public String attorneyName;
    public UUID targetOid;
    public MObjectType targetType;
    public String targetName;
    public UUID targetOwnerOid;
    public MObjectType targetOwnerType;
    public String targetOwnerName;
    public String channel;
    public OperationResultStatusType outcome;
    public String parameter;
    public String result;
    public String message;
    public String[] changedItemPaths;
    public UUID[] resourceOids;
    public Jsonb properties;
    public Jsonb customColumnProperties;

    // "transient" fields not used by Querydsl
    public List<MAuditDelta> deltas;
    public Map<String, List<MAuditRefValue>> refValues;

    public void addDelta(MAuditDelta mAuditDelta) {
        if (deltas == null) {
            deltas = new ArrayList<>();
        }
        deltas.add(mAuditDelta);
    }

    public void addRefValue(MAuditRefValue refValue) {
        if (refValues == null) {
            refValues = new TreeMap<>();
        }
        List<MAuditRefValue> values = refValues.computeIfAbsent(refValue.name, s -> new ArrayList<>());
        values.add(refValue);
    }

    /* TODO props stored as JSONB
    public void addProperty(MAuditPropertyValue propertyValue) {
        if (properties == null) {
            properties = new TreeMap<>();
        }
        List<String> values = properties.computeIfAbsent(propertyValue.name, s -> new ArrayList<>());
        values.add(propertyValue.value);
    }
    */

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        MAuditEventRecord that = (MAuditEventRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        // TODO check/fix
        return "MAuditEventRecord{" +
                "id=" + id +
                ", eventIdentifier='" + eventIdentifier + '\'' +
                ", timestamp=" + timestamp +
                ", channel='" + channel + '\'' +
                ", eventStage=" + eventStage +
                ", eventType=" + eventType +
                ", attorneyName='" + attorneyName + '\'' +
                ", attorneyOid='" + attorneyOid + '\'' +
                ", hostIdentifier='" + hostIdentifier + '\'' +
                ", initiatorName='" + initiatorName + '\'' +
                ", initiatorOid='" + initiatorOid + '\'' +
                ", initiatorType=" + initiatorType +
                ", message='" + message + '\'' +
                ", nodeIdentifier='" + nodeIdentifier + '\'' +
                ", outcome=" + outcome +
                ", parameter='" + parameter + '\'' +
                ", remoteHostAddress='" + remoteHostAddress + '\'' +
                ", requestIdentifier='" + requestIdentifier + '\'' +
                ", result='" + result + '\'' +
                ", sessionIdentifier='" + sessionIdentifier + '\'' +
                ", targetName='" + targetName + '\'' +
                ", targetOid='" + targetOid + '\'' +
                ", targetOwnerName='" + targetOwnerName + '\'' +
                ", targetOwnerOid='" + targetOwnerOid + '\'' +
                ", targetOwnerType=" + targetOwnerType +
                ", targetType=" + targetType +
                ", taskIdentifier='" + taskIdentifier + '\'' +
                ", taskOid='" + taskOid + '\'' +
                '}';
    }
}
