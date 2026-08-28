/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.repo.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventRecordPayload;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPayloadType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Tests generic audit event payload handling in {@link AuditEventRecord}.
 *
 * Verifies schema conversion, cloning, payload ordering, and basic validation.
 */
public class AuditEventRecordTest {

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

    @Test
    public void clonedRecordPreservesPayloads() {
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload(
                "first", "application/json", "{\"id\":1}"));
        record.addPayload(new AuditEventRecordPayload(
                "second", "text/plain", "hello"));

        AuditEventRecord clone = record.clone();

        assertThat(clone.getPayloads()).hasSize(2);
        assertThat(clone.getPayloads())
                .extracting(AuditEventRecordPayload::getName)
                .containsExactly("first", "second");
        assertThat(clone.getPayloads())
                .extracting(AuditEventRecordPayload::getContent)
                .containsExactly("{\"id\":1}", "hello");
    }

    @Test
    public void nullPayloadIsRejected() {
        AuditEventRecord record = new AuditEventRecord();

        assertThatThrownBy(() -> record.addPayload(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Payload must not be null");
    }

    private void assertPayload(AuditEventRecordPayloadType payload, String name, String contentType, String content) {
        assertThat(payload.getName()).isEqualTo(name);
        assertThat(payload.getContentType()).isEqualTo(contentType);
        assertThat(payload.getContent()).isEqualTo(content);
    }
}
