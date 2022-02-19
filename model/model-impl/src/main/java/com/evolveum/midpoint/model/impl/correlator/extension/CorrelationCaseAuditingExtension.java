/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.extension;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.AuditingExtension;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.jetbrains.annotations.NotNull;

public class CorrelationCaseAuditingExtension implements AuditingExtension {

    @Override
    public void enrichCaseRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // TODO
    }

    @Override
    public void enrichWorkItemCreatedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // TODO
    }

    @Override
    public void enrichWorkItemDeletedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        // TODO
    }
}
