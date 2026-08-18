/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.repo.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventRecordPayload;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPayloadType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

public class AuditEventRecordPayloadTest {

    @Test
    public void payloadsSurviveJavaToSchemaConversionInOrder() {
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("first", "application/json", "{\"id\":1}"));
        record.addPayload(new AuditEventRecordPayload("second", "application/xml", "<ok/>"));

        AuditEventRecordType bean = record.createAuditEventRecordType(false);

        assertThat(bean.getPayload()).hasSize(2);
        assertPayload(bean.getPayload().get(0), "first", "application/json", "{\"id\":1}");
        assertPayload(bean.getPayload().get(1), "second", "application/xml", "<ok/>");

        AuditEventRecordPayload importedPayload = AuditEventRecordPayload.fromXml(bean.getPayload().get(0));
        assertThat(importedPayload.getName()).isEqualTo("first");
        assertThat(importedPayload.getContentType()).isEqualTo("application/json");
        assertThat(importedPayload.getContent()).isEqualTo("{\"id\":1}");
    }

    private void assertPayload(AuditEventRecordPayloadType payload, String name, String contentType, String content) {
        assertThat(payload.getName()).isEqualTo(name);
        assertThat(payload.getContentType()).isEqualTo(contentType);
        assertThat(payload.getContent()).isEqualTo(content);
    }
}
