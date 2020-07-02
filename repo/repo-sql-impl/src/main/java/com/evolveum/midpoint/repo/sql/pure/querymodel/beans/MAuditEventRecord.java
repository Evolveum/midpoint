/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel.beans;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MAuditEvent is a Querydsl bean type related to
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
    public String hostidentifier;
    public String initiatorname;
    public String initiatoroid;
    public Integer initiatortype;
    public String message;
    public String nodeidentifier;
    public Integer outcome;
    public String parameter;
    public String remotehostaddress;
    public String requestidentifier;
    public String result;
    public String sessionidentifier;
    public String targetname;
    public String targetoid;
    public String targetownername;
    public String targetowneroid;
    public Integer targetownertype;
    public Integer targettype;
    public String taskidentifier;
    public String taskoid;

    // "transient" fields not used by Querydsl
    public List<MAuditDelta> deltas;

    @Override
    public String toString() {
        return "MAuditEventRecord{" +
                "id=" + id +
                ", eventIdentifier='" + eventIdentifier + '\'' +
                ", timestamp=" + timestamp +
                ", channel='" + channel + '\'' +
                ", eventType=" + eventType +
                ", eventStage=" + eventStage +
//                ", attorneyName='" + attorneyName + '\'' +
//                ", attorneyOid='" + attorneyOid + '\'' +
//                ", hostidentifier='" + hostidentifier + '\'' +
//                ", initiatorname='" + initiatorname + '\'' +
//                ", initiatoroid='" + initiatoroid + '\'' +
//                ", initiatortype=" + initiatortype +
//                ", message='" + message + '\'' +
//                ", nodeidentifier='" + nodeidentifier + '\'' +
//                ", outcome=" + outcome +
//                ", parameter='" + parameter + '\'' +
//                ", remotehostaddress='" + remotehostaddress + '\'' +
//                ", requestidentifier='" + requestidentifier + '\'' +
//                ", result='" + result + '\'' +
//                ", sessionidentifier='" + sessionidentifier + '\'' +
//                ", targetname='" + targetname + '\'' +
//                ", targetoid='" + targetoid + '\'' +
//                ", targetownername='" + targetownername + '\'' +
//                ", targetowneroid='" + targetowneroid + '\'' +
//                ", targetownertype=" + targetownertype +
//                ", targettype=" + targettype +
//                ", taskidentifier='" + taskidentifier + '\'' +
//                ", taskoid='" + taskoid + '\'' +
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
