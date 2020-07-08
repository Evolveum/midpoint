/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import java.time.Instant;
import java.util.*;

/**
 * Querydsl "row bean" type related to {@code QAuditEventRecord}.
 * Usable only for built-in low-level queries without extension columns.
 */
@SuppressWarnings("unused")
public class MAuditEventRecord {

    public Long id;
    public String eventIdentifier;
    public Instant timestamp;
    public String channel;
    public Integer eventStage;
    public Integer eventType;
    public String attorneyName;
    public String attorneyOid;
    public String hostIdentifier;
    public String initiatorName;
    public String initiatorOid;
    public Integer initiatorType;
    public String message;
    public String nodeIdentifier;
    public Integer outcome;
    public String parameter;
    public String remoteHostAddress;
    public String requestIdentifier;
    public String result;
    public String sessionIdentifier;
    public String targetName;
    public String targetOid;
    public Integer targetType;
    public String targetOwnerName;
    public String targetOwnerOid;
    public Integer targetOwnerType;
    public String taskIdentifier;
    public String taskOid;

    // "transient" fields not used by Querydsl
    public List<MAuditDelta> deltas;
    public Map<String, List<String>> properties;

    public void addProperty(MAuditPropertyValue propertyValue) {
        if (properties == null) {
            properties = new TreeMap<>();
        }
        List<String> values = properties.computeIfAbsent(propertyValue.name, s -> new ArrayList<>());
        values.add(propertyValue.value);
    }

    @Override
    public String toString() {
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

    public void addDelta(MAuditDelta mAuditDelta) {
        if (deltas == null) {
            deltas = new ArrayList<>();
        }
        deltas.add(mAuditDelta);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        MAuditEventRecord that = (MAuditEventRecord) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
