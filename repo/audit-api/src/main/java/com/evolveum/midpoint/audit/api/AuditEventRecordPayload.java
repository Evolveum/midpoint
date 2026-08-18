/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.audit.api;

import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPayloadType;

public class AuditEventRecordPayload implements Serializable {

    private final String name;
    private final String contentType;
    private final String content;

    public AuditEventRecordPayload(String name, String contentType, String content) {
        this.name = name;
        this.contentType = contentType;
        this.content = content;
    }

    public static AuditEventRecordPayload fromXml(AuditEventRecordPayloadType payload) {
        return new AuditEventRecordPayload(
                payload.getName(),
                payload.getContentType(),
                payload.getContent());
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContent() {
        return content;
    }

    public AuditEventRecordPayloadType toXml() {
        return new AuditEventRecordPayloadType()
                .name(name)
                .contentType(contentType)
                .content(content);
    }

    @Override
    public String toString() {
        return "AuditEventRecordPayload{" +
                "name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
