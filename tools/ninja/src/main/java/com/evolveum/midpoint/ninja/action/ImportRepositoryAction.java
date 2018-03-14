package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.worker.ImportConsumerWorker;
import com.evolveum.midpoint.ninja.action.worker.ImportProducerWorker;
import com.evolveum.midpoint.ninja.action.worker.ProgressReporterWorker;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.opts.ImportOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.concurrent.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportRepositoryAction extends RepositoryAction<ImportOptions> {

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;
    private static final long CONSUMERS_WAIT_FOR_START = 2000L;

    private BlockingQueue<PrismObject> queue;

    private ExecutorService executor;

    @Override
    public void execute() throws Exception {
        OperationStatus progress = new OperationStatus();

        queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

        // + 2 means producer and progress reporter
        executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

        ImportProducerWorker producer;
        if (options.getOid() != null) {
            InOidFilter filter = InOidFilter.createInOid(options.getOid());
            producer = importByFilter(filter, true, progress);
        } else {
            ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context);
            producer = importByFilter(filter, false, progress);
        }

        executor.execute(producer);

        Thread.sleep(CONSUMERS_WAIT_FOR_START);

        executor.execute(new ProgressReporterWorker(queue, progress, log));

        for (int i = 0; i < options.getMultiThread(); i++) {
            ImportConsumerWorker worker = new ImportConsumerWorker(context, options, queue, progress);
            executor.execute(worker);
        }

        executor.shutdown();
        executor.awaitTermination(365, TimeUnit.DAYS);

        OperationResult result = producer.getResult();

        handleResultOnFinish(result, progress, "Import finished");
    }

    @Override
    protected LogTarget getInfoLogTarget() {
        if (options.getInput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private ImportProducerWorker importByFilter(ObjectFilter filter, boolean stopAfterFound, OperationStatus status) {
        ImportProducerWorker producer = new ImportProducerWorker(context, options, queue, filter, stopAfterFound);
        producer.setOperation(status);

        return producer;
    }
}
