/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.audit;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Producer worker for audit export operation.
 */
public class ExportAuditProducerWorker extends BaseWorker<BaseAuditImportExportOptions, AuditEventRecordType> {

    private final ObjectQuery query;

    public ExportAuditProducerWorker(
            NinjaContext context, BaseAuditImportExportOptions options, BlockingQueue<AuditEventRecordType> queue,
            OperationStatus operation, List<ExportAuditProducerWorker> producers, ObjectQuery query) {
        super(context, options, queue, operation, producers);

        this.query = query;
    }

    @Override
    public void run() {
        Log log = context.getLog();

        try {
            GetOperationOptionsBuilder optionsBuilder = context.getSchemaService().getOperationOptionsBuilder();

            optionsBuilder = NinjaUtils.addIncludeOptionsForExport(optionsBuilder, AuditEventRecordType.class);

            AuditResultHandler handler = (object, parentResult) -> {
                try {
                    queue.put(object);
                } catch (InterruptedException ex) {
                    log.error("Couldn't queue object {}, reason: {}", ex, object, ex.getMessage());
                }
                return true;
            };

            AuditService auditService = context.getAuditService();
            auditService.searchObjectsIterative(query, handler, optionsBuilder.build(), operation.getResult());
        } catch (SchemaException ex) {
            log.error("Unexpected exception, reason: {}", ex, ex.getMessage());
        } catch (NinjaException ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            markDone();

            if (isWorkersDone()) {
                if (!operation.isFinished()) {
                    operation.producerFinish();
                }
            }
        }
    }
}
