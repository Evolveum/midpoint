/*
 * Copyright (C) 2010-2023 Evolveum and contributors
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
import com.evolveum.midpoint.xml.ns._public.common.audit_3.EffectivePrivilegesModificationType;
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
    public UUID effectivePrincipalOid;
    public MObjectType effectivePrincipalType;
    public String effectivePrincipalName;
    public EffectivePrivilegesModificationType effectivePrivilegesModification;
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

    // changedItemPaths are normalized with PrismContext.createCanonicalItemPath()
    public String[] changedItemPaths;
    public String[] resourceOids;
    public Jsonb properties;
    public Jsonb customColumnProperties;

    // "transient" fields not used by Querydsl
    public Collection<MAuditDelta> deltas;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MAuditEventRecord that = (MAuditEventRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "MAuditEventRecord{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", eventIdentifier='" + eventIdentifier + '\'' +
                ", eventType=" + eventType +
                ", eventStage=" + eventStage +
                ", sessionIdentifier='" + sessionIdentifier + '\'' +
                ", requestIdentifier='" + requestIdentifier + '\'' +
                ", taskIdentifier='" + taskIdentifier + '\'' +
                ", taskOid=" + taskOid +
                ", hostIdentifier='" + hostIdentifier + '\'' +
                ", nodeIdentifier='" + nodeIdentifier + '\'' +
                ", remoteHostAddress='" + remoteHostAddress + '\'' +
                ", initiatorOid=" + initiatorOid +
                ", initiatorType=" + initiatorType +
                ", initiatorName='" + initiatorName + '\'' +
                ", attorneyOid=" + attorneyOid +
                ", attorneyName='" + attorneyName + '\'' +
                ", effectivePrincipalOid=" + effectivePrincipalOid +
                ", effectivePrincipalType=" + effectivePrincipalType +
                ", effectivePrincipalName='" + effectivePrincipalName + '\'' +
                ", effectivePrivilegesModification=" + effectivePrivilegesModification +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", targetName='" + targetName + '\'' +
                ", targetOwnerOid=" + targetOwnerOid +
                ", targetOwnerType=" + targetOwnerType +
                ", targetOwnerName='" + targetOwnerName + '\'' +
                ", channel='" + channel + '\'' +
                ", outcome=" + outcome +
                ", parameter='" + parameter + '\'' +
                ", result='" + result + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
