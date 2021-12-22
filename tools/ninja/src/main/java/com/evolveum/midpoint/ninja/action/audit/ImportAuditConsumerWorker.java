/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.audit;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationContext;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Consumer importing audit events to the database.
 */
public class ImportAuditConsumerWorker extends BaseWorker<ImportAuditOptions, AuditEventRecordType> {

    public ImportAuditConsumerWorker(
            NinjaContext context, ImportAuditOptions options, BlockingQueue<AuditEventRecordType> queue,
            OperationStatus operation, List<ImportAuditConsumerWorker> consumers) {
        super(context, options, queue, operation, consumers);
    }

    @Override
    public void run() {
        ApplicationContext ctx = context.getApplicationContext();
        Protector protector = ctx.getBean(Protector.class);

        try {
            while (!shouldConsumerStop()) {
                AuditEventRecordType auditRecord = null;
                try {
                    auditRecord = queue.poll(CONSUMER_POLL_TIMEOUT, TimeUnit.SECONDS);
                    if (auditRecord == null) {
                        continue;
                    }

                    /* TODO relevant for audit deltas?
                    if (!opts.isAllowUnencryptedValues()) {
                        //noinspection unchecked
                        CryptoUtil.encryptValues(protector, object);
                    }
                    */

                    AuditService auditService = context.getAuditService();
                    auditService.audit(auditRecord, new OperationResult("Import audit"));

                    operation.incrementTotal();
                } catch (Exception ex) {
                    Throwable cause = ExceptionUtil.findRootCause(ex);
                    // Short message for existing audit event conflict, but only for new repo
                    if (cause.toString().contains("duplicate key value violates unique constraint \"ma_audit_event_")
                            && cause.toString().contains("Detail: Key (id, \"timestamp\")")) {
                        context.getLog().info("Audit already exists, skipping ({}, {})",
                                auditRecord.getRepoId(), auditRecord.getTimestamp());
                        operation.incrementSkipped(); // TODO: skipped or still error? also, info or error?
                    } else {
                        context.getLog().error("Couldn't add object {}, reason: {}", ex, auditRecord, ex.getMessage());
                        operation.incrementError();
                    }
                }
            }
        } finally {
            markDone();

            if (isWorkersDone()) {
                operation.finish();
            }
        }
    }
}
