/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.evolveum.midpoint.ninja.action.RepositoryAction;
import com.evolveum.midpoint.ninja.action.worker.ImportProducerWorker;
import com.evolveum.midpoint.ninja.action.worker.ProgressReporterWorker;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Action for importing audit event records to the repository.
 */
public class ImportAuditRepositoryAction extends RepositoryAction<ImportAuditOptions, Void> {

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;
    private static final long CONSUMERS_WAIT_FOR_START = 2000L;

    public static final String OPERATION_SHORT_NAME = "importAudit";
    public static final String OPERATION_NAME = ImportAuditRepositoryAction.class.getName() + "." + OPERATION_SHORT_NAME;

    @Override
    public String getOperationName() {
        return "import audit";
    }

    @Override
    public Void execute() throws Exception {
        OperationResult result = new OperationResult(OPERATION_NAME);
        OperationStatus progress = new OperationStatus(context, result);

        BlockingQueue<AuditEventRecordType> queue =
                new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

        // "+ 2" will be used for producer and progress reporter
        ExecutorService executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

        ImportProducerWorker<AuditEventRecordType> producer;
        ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context, AuditEventRecordType.class);
        producer = importByFilter(filter, false, queue, progress);

        executor.execute(producer);

        Thread.sleep(CONSUMERS_WAIT_FOR_START);

        executor.execute(new ProgressReporterWorker<>(context, options, queue, progress));

        List<ImportAuditConsumerWorker> consumers = createConsumers(queue, progress);
        consumers.forEach(c -> executor.execute(c));

        executor.shutdown();
        boolean awaitResult = executor.awaitTermination(NinjaUtils.WAIT_FOR_EXECUTOR_FINISH, TimeUnit.DAYS);
        if (!awaitResult) {
            log.error("Executor did not finish before timeout");
        }

        handleResultOnFinish(progress, "Audit import finished");

        return null;
    }

    @Override
    public LogTarget getLogTarget() {
        if (options.getInput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private ImportProducerWorker<AuditEventRecordType> importByFilter(
            ObjectFilter filter, boolean stopAfterFound,
            BlockingQueue<AuditEventRecordType> queue, OperationStatus status) {
        ImportProducerWorker ret = new ImportProducerWorker<>(context, options, queue, status, filter, stopAfterFound, false);
        ret.setConvertMissingType(true);
        ret.setCompatMode(true);
        return ret;
    }

    private List<ImportAuditConsumerWorker> createConsumers(
            BlockingQueue<AuditEventRecordType> queue, OperationStatus operation) {
        List<ImportAuditConsumerWorker> consumers = new ArrayList<>();

        for (int i = 0; i < options.getMultiThread(); i++) {
            consumers.add(new ImportAuditConsumerWorker(context, options, queue, operation, consumers));
        }

        return consumers;
    }
}
