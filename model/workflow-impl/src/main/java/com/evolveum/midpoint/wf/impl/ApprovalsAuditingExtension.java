/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.AuditingExtension;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ApprovalsAuditingExtension implements AuditingExtension {

    // We'll generalize this if more change processors will be supported.
    @Autowired private PrimaryChangeProcessor primaryChangeProcessor;

    @Override
    public void enrichCaseRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        primaryChangeProcessor.enrichCaseAuditRecord(auditEventRecord, operation);
    }

    @Override
    public void enrichWorkItemCreatedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        primaryChangeProcessor.enrichWorkItemCreatedAuditRecord(auditEventRecord, operation);
    }

    @Override
    public void enrichWorkItemDeletedAuditRecord(
            @NotNull AuditEventRecord auditEventRecord,
            @NotNull CaseWorkItemType workItem,
            @NotNull CaseEngineOperation operation,
            @NotNull OperationResult result) {
        primaryChangeProcessor.enrichWorkItemDeletedAuditRecord(auditEventRecord, operation);
    }
}
