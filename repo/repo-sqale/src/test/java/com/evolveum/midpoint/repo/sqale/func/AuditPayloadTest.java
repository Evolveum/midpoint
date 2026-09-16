/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventRecordPayload;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.MAuditPayload;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecordMapping;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditPayload;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditPayloadMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.test.NullTaskImpl;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPayloadType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Tests persistence and lifecycle behavior of audit event payloads in the Sqale audit repository.
 *
 * Verifies payload storage and loading, byte representation, ordering, generic non-JSON content,
 * cleanup behavior, and both {@link AuditEventRecord} and {@link AuditEventRecordType} insertion paths.
 */
public class AuditPayloadTest extends SqaleRepoBaseTest {

    @Test
    public void test100AuditRecordWithoutPayloadStillWorks() {
        given("audit is empty");
        clearAudit();

        and("audit event record without payload");
        AuditEventRecord record = new AuditEventRecord();
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("operation is success and no payload rows are stored");
        assertThatOperationResult(result).isSuccess();
        assertThat(record.getRepoId()).isNotNull();
        assertCount(QAuditEventRecordMapping.get().defaultAlias(), 1);
        assertCount(QAuditPayloadMapping.get().defaultAlias(), 0);
    }

    @Test
    public void test110JsonObjectPayloadRoundTripsExactly() throws Exception {
        given("audit is empty");
        clearAudit();

        and("audit event record with JSON object payload");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("input", "application/json", "{\"id\":1}"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload row is stored");
        QAuditPayload payload = QAuditPayloadMapping.get().defaultAlias();
        List<MAuditPayload> payloadRows = selectPayloads(payload);
        assertThat(payloadRows).hasSize(1);
        assertThat(payloadRows.get(0).ordinal).isEqualTo(0);
        assertThat(payloadRows.get(0).name).isEqualTo("input");
        assertThat(payloadRows.get(0).contentType).isEqualTo("application/json");
        assertThat(payloadRows.get(0).searchableText).isEqualTo(" id1 ");
        assertStoredContent(payloadRows.get(0), "{\"id\":1}");

        and("payload is loaded back with the audit record");
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertThat(loaded.getPayload()).hasSize(1);
        assertPayload(loaded.getPayload().get(0), "input", "application/json", "{\"id\":1}");
    }

    @Test
    public void test115JsonArrayPayloadRoundTripsExactly() throws Exception {
        given("audit is empty");
        clearAudit();

        and("audit event record with JSON array payload");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("array", "application/json", "[{\"id\":1},{\"id\":2}]"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload row stores the original JSON text");
        QAuditPayload payload = QAuditPayloadMapping.get().defaultAlias();
        MAuditPayload payloadRow = selectPayloads(payload).get(0);
        assertThat(payloadRow.searchableText).isEqualTo(" id1id2 ");
        assertStoredContent(payloadRow, "[{\"id\":1},{\"id\":2}]");

        and("payload is loaded back as JSON content");
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertPayload(loaded.getPayload().get(0), "array", "application/json", "[{\"id\":1},{\"id\":2}]");
    }

    @Test
    public void test117JsonStringPayloadKeepsJsonStringSyntax() throws Exception {
        given("audit is empty");
        clearAudit();

        and("audit event record with top-level JSON string payload");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("string", "application/json", "\"hello\""));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload row stores the original JSON string syntax");
        QAuditPayload payload = QAuditPayloadMapping.get().defaultAlias();
        MAuditPayload payloadRow = selectPayloads(payload).get(0);
        assertStoredContent(payloadRow, "\"hello\"");
        assertThat(payloadRow.searchableText).isEqualTo(" hello ");

        and("payload is loaded back with JSON quotes");
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertPayload(loaded.getPayload().get(0), "string", "application/json", "\"hello\"");
    }

    @Test
    public void test118JsonSubtypePayloadRoundTripsExactly() throws Exception {
        given("audit is empty");
        clearAudit();

        and("audit event record with +json content type");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("problem", "application/problem+json", "{\"status\":400}"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload row stores the original JSON text");
        MAuditPayload payloadRow = selectPayloads(QAuditPayloadMapping.get().defaultAlias()).get(0);
        assertStoredContent(payloadRow, "{\"status\":400}");

        and("payload is loaded back as JSON content");
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertPayload(loaded.getPayload().get(0), "problem", "application/problem+json", "{\"status\":400}");
    }

    @Test
    public void test119JsonContentTypeParametersAreIgnored() throws Exception {
        given("audit is empty");
        clearAudit();

        and("audit event record with JSON content type parameters");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("json", "application/json; charset=UTF-8", "{\"ok\":true}"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload row stores the original JSON text");
        MAuditPayload payloadRow = selectPayloads(QAuditPayloadMapping.get().defaultAlias()).get(0);
        assertStoredContent(payloadRow, "{\"ok\":true}");

        and("payload is loaded back as JSON content");
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertPayload(loaded.getPayload().get(0), "json", "application/json; charset=UTF-8", "{\"ok\":true}");
    }

    @Test
    public void test120MultiplePayloadsPreserveOrder() throws SchemaException {
        given("audit is empty");
        clearAudit();

        and("audit event record with multiple payloads");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("first", "text/plain", "alpha"));
        record.addPayload(new AuditEventRecordPayload("second", "text/plain", "beta"));
        record.addPayload(new AuditEventRecordPayload("third", "text/plain", "gamma"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload rows store ordinals");
        QAuditPayload payload = QAuditPayloadMapping.get().defaultAlias();
        assertThat(selectPayloads(payload))
                .extracting(row -> row.ordinal)
                .containsExactly(0, 1, 2);

        and("payloads are loaded back in order");
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertThat(loaded.getPayload())
                .extracting(AuditEventRecordPayloadType::getName)
                .containsExactly("first", "second", "third");
        assertThat(loaded.getPayload())
                .extracting(AuditEventRecordPayloadType::getContent)
                .containsExactly("alpha", "beta", "gamma");
    }

    @Test
    public void test125MalformedJsonPayloadRoundTripsAsText() throws Exception {
        given("audit is empty");
        clearAudit();

        and("audit event record with malformed JSON payload text");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("malformed", "application/json", "{"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload text is stored and loaded unchanged");
        MAuditPayload payloadRow = selectPayloads(QAuditPayloadMapping.get().defaultAlias()).get(0);
        assertStoredContent(payloadRow, "{");

        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertPayload(loaded.getPayload().get(0), "malformed", "application/json", "{");
    }

    @Test
    public void test130NonJsonPayloadContentRoundTripsAsGenericString() throws Exception {
        given("audit is empty");
        clearAudit();

        and("audit event record with non-JSON payload content");
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("plain", "text/plain", "not JSON: {"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, NullTaskImpl.INSTANCE, result);

        then("payload is stored without requiring JSON input");
        QAuditPayload payload = QAuditPayloadMapping.get().defaultAlias();
        MAuditPayload payloadRow = selectPayloads(payload).get(0);
        assertPayload(payloadRow, 0, "plain", "text/plain", "not JSON: {");
        assertThat(payloadRow.searchableText).isEqualTo(" not json ");
        assertStoredContent(payloadRow, "not JSON: {");

        and("payload is loaded back unchanged");
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertPayload(loaded.getPayload().get(0), "plain", "text/plain", "not JSON: {");
    }

    @Test
    public void test140PayloadsAreDeletedWithParentAuditEvent() {
        given("audit event record with payload");
        clearAudit();
        AuditEventRecord record = new AuditEventRecord();
        record.addPayload(new AuditEventRecordPayload("input", "text/plain", "content"));
        OperationResult result = createOperationResult();
        auditService.audit(record, NullTaskImpl.INSTANCE, result);
        QAuditPayload payload = QAuditPayloadMapping.get().defaultAlias();
        assertCount(payload, 1);

        when("deleting the parent audit event");
        QAuditEventRecord event = QAuditEventRecordMapping.get().defaultAlias();
        try (JdbcSession jdbcSession = startTransaction()) {
            jdbcSession.newDelete(event)
                    .where(event.id.eq(record.getRepoId()))
                    .execute();
            jdbcSession.commit();
        }

        then("payload rows are deleted by cascade");
        assertCount(QAuditEventRecordMapping.get().defaultAlias(), 0);
        assertCount(payload, 0);
    }

    @Test
    public void test150RawAuditRecordTypePayloadRoundTrips() throws SchemaException {
        given("audit is empty");
        clearAudit();

        and("raw audit event record type with payload");
        AuditEventRecordType record = new AuditEventRecordType()
                .timestamp(MiscUtil.asXMLGregorianCalendar(System.currentTimeMillis()))
                .payload(new AuditEventRecordPayloadType()
                        .name("schema")
                        .contentType("application/xml")
                        .content("<payload/>"));
        OperationResult result = createOperationResult();

        when("saving the event record");
        auditService.audit(record, result);

        then("operation is success and payload is loaded back");
        assertThatOperationResult(result).isSuccess();
        AuditEventRecordType loaded = searchByRepoId(record.getRepoId());
        assertPayload(loaded.getPayload().get(0), "schema", "application/xml", "<payload/>");
    }

    private List<MAuditPayload> selectPayloads(QAuditPayload payload) {
        try (JdbcSession jdbcSession = startReadOnlyTransaction()) {
            return jdbcSession.newQuery()
                    .select(payload)
                    .from(payload)
                    .orderBy(payload.ordinal.asc())
                    .fetch();
        }
    }

    private @NotNull AuditEventRecordType searchByRepoId(Long repoId) throws SchemaException {
        SearchResultList<AuditEventRecordType> records = auditService.searchObjects(
                prismContext.queryFor(AuditEventRecordType.class)
                        .item(AuditEventRecordType.F_REPO_ID).eq(repoId)
                        .build(),
                null,
                createOperationResult());
        assertThat(records).hasSize(1);
        return records.get(0);
    }

    private void assertPayload(MAuditPayload payload, int ordinal, String name, String contentType, String content) {
        assertThat(payload.ordinal).isEqualTo(ordinal);
        assertThat(payload.name).isEqualTo(name);
        assertThat(payload.contentType).isEqualTo(contentType);
        assertThat(QAuditPayloadMapping.get().toSchemaObject(payload).getContent()).isEqualTo(content);
    }

    private void assertPayload(AuditEventRecordPayloadType payload, String name, String contentType, String content) {
        assertThat(payload.getName()).isEqualTo(name);
        assertThat(payload.getContentType()).isEqualTo(contentType);
        assertThat(payload.getContent()).isEqualTo(content);
    }

    private void assertStoredContent(MAuditPayload payload, String content) {
        assertThat(new String(payload.content, StandardCharsets.UTF_8)).isEqualTo(content);
    }
}
