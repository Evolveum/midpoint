/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MAuditEvent is a Querydsl bean type
 */
@SuppressWarnings("unused")
public class MAuditEventRecord {

    public String attorneyName;
    public String attorneyOid;
    public String channel;
    public String eventidentifier;
    public Integer eventstage;
    public Integer eventtype;
    public String hostidentifier;
    public Long id;
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
    public java.sql.Timestamp timestampvalue;

    // "transient" fields not used by Querydsl
    public List<MAuditDelta> deltas;

    @Override
    public String toString() {
        return "MAuditEventRecord{" +
                "id=" + id +
//                ", attorneyName='" + attorneyName + '\'' +
//                ", attorneyOid='" + attorneyOid + '\'' +
//                ", channel='" + channel + '\'' +
//                ", eventidentifier='" + eventidentifier + '\'' +
//                ", eventstage=" + eventstage +
//                ", eventtype=" + eventtype +
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
                ", timestampvalue=" + timestampvalue +
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

