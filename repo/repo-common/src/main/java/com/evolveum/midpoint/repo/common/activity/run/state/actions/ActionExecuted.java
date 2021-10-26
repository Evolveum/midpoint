/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state.actions;

import com.evolveum.midpoint.prism.delta.ChangeType;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

class ActionExecuted {

    final String objectName;
    final String objectDisplayName;
    final QName objectType;
    String objectOid;
    final ChangeType changeType;
    final String channel;
    final Throwable exception;
    final XMLGregorianCalendar timestamp;

    ActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
            ChangeType changeType, String channel, Throwable exception, XMLGregorianCalendar timestamp) {
        this.objectName = objectName;
        this.objectDisplayName = objectDisplayName;
        this.objectType = objectType;
        this.objectOid = objectOid;
        this.changeType = changeType;
        this.channel = channel;
        this.exception = exception;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ActionExecuted{" +
                "objectName='" + objectName + '\'' +
                ", objectDisplayName='" + objectDisplayName + '\'' +
                ", objectType=" + objectType +
                ", objectOid='" + objectOid + '\'' +
                ", changeType=" + changeType +
                ", channel='" + channel + '\'' +
                ", exception=" + exception +
                ", timestamp=" + timestamp +
                '}';
    }
}
