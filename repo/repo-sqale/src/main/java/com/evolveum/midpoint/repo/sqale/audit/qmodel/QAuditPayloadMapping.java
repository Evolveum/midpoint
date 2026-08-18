/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditPayload.TABLE_NAME;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbException;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPayloadType;

/**
 * Mapping between {@link QAuditPayload} and {@link AuditEventRecordPayloadType}.
 */
public class QAuditPayloadMapping
        extends SqaleTableMapping<AuditEventRecordPayloadType, QAuditPayload, MAuditPayload> {

    public static final String DEFAULT_ALIAS_NAME = "ap";

    private static QAuditPayloadMapping instance;

    public static QAuditPayloadMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QAuditPayloadMapping(repositoryContext);
        return instance;
    }

    public static QAuditPayloadMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditPayloadMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordPayloadType.class, QAuditPayload.class, repositoryContext);
    }

    @Override
    protected QAuditPayload newAliasInstance(String alias) {
        return new QAuditPayload(alias);
    }

    @Override
    public AuditEventRecordPayloadType toSchemaObject(MAuditPayload row) {
        return new AuditEventRecordPayloadType()
                .name(row.name)
                .contentType(row.contentType)
                .content(contentToString(row.content, row.contentType));
    }

    public Jsonb contentToJsonb(String content, String contentType) {
        if (content == null) {
            return null;
        }

        if (isJsonContentType(contentType)) {
            validateJsonContent(content);
            return new Jsonb(content);
        }

        return contentStringScalar(content);
    }

    private void validateJsonContent(String content) {
        try {
            if (Jsonb.MAPPER.readTree(content) == null) {
                throw new JsonbException("Audit payload content is not valid JSON", null);
            }
        } catch (JsonProcessingException e) {
            throw new JsonbException("Audit payload content is not valid JSON", e);
        }
    }

    private Jsonb contentStringScalar(String content) {
        try {
            return new Jsonb(Jsonb.MAPPER.writeValueAsString(content));
        } catch (JsonProcessingException ex) {
            throw new JsonbException("Unexpected error while writing audit payload content", ex);
        }
    }

    private String contentToString(Jsonb content, String contentType) {
        if (content == null) {
            return null;
        }

        if (isJsonContentType(contentType)) {
            return content.value;
        }

        try {
            return Jsonb.MAPPER.readValue(content.value, String.class);
        } catch (JsonProcessingException e) {
            throw new JsonbException("Unexpected error while reading audit payload content", e);
        }
    }

    private boolean isJsonContentType(String contentType) {
        if (contentType == null) {
            return false;
        }

        String mediaType = contentType;
        int parameterStart = mediaType.indexOf(';');
        if (parameterStart >= 0) {
            mediaType = mediaType.substring(0, parameterStart);
        }
        mediaType = mediaType.trim().toLowerCase(Locale.ROOT);

        return mediaType.equals("application/json")
                || mediaType.startsWith("application/") && mediaType.endsWith("+json");
    }

    @Override
    public AuditEventRecordPayloadType toSchemaObject(
            @NotNull Tuple row, @NotNull QAuditPayload entityPath, @NotNull JdbcSession jdbcSession,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        throw new UnsupportedOperationException(); // implemented through MAuditEventRecord batching
    }
}
