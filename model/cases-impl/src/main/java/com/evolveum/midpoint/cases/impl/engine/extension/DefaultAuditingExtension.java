/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.AuditingExtension;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.jetbrains.annotations.NotNull;

public class DefaultAuditingExtension implements AuditingExtension {

    @Override
    public void enrichCaseRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // nothing to do here
    }

    @Override
    public void enrichWorkItemCreatedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // nothing to do here
    }

    @Override
    public void enrichWorkItemDeletedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // nothing to do here
    }

}
