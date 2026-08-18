/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;

/** Querydsl "row bean" type related to {@link QAuditPayload}. */
@SuppressWarnings("unused")
public class MAuditPayload {

    public Long recordId;
    public Instant timestamp;
    public Integer ordinal;
    public String name;
    public String contentType;
    public Jsonb content;
    public String searchableText;

    @Override
    public String toString() {
        return "MAuditPayload{" +
                "recordId=" + recordId +
                ", timestamp=" + timestamp +
                ", ordinal=" + ordinal +
                ", name='" + name + '\'' +
                ", contentType='" + contentType + '\'' +
                ", content=" + content +
                '}';
    }
}
