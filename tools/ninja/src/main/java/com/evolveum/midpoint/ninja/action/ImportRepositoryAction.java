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

    private static final String DOT_CLASS = ImportProducerWorker.class.getName() + ".";

    private static final String OPERATION_IMPORT = DOT_CLASS + "import";

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;
    private static final long CONSUMERS_WAIT_FOR_START = 2000L;

    @Override
    public void execute() throws Exception {
        OperationResult result = new OperationResult(OPERATION_IMPORT);
        OperationStatus progress = new OperationStatus(result);

        BlockingQueue<PrismObject> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

        // "+ 2" will be used for producer and progress reporter
        ExecutorService executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

        ImportProducerWorker producer;
        if (options.getOid() != null) {
            InOidFilter filter = InOidFilter.createInOid(options.getOid());
            producer = importByFilter(filter, true, queue, progress);
        } else {
            ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context);
            producer = importByFilter(filter, false, queue, progress);
        }

        executor.execute(producer);

        Thread.sleep(CONSUMERS_WAIT_FOR_START);

        executor.execute(new ProgressReporterWorker(context, options, queue, progress));

        for (int i = 0; i < options.getMultiThread(); i++) {
            ImportConsumerWorker worker = new ImportConsumerWorker(context, options, queue, progress);
            executor.execute(worker);
        }

        executor.shutdown();
        executor.awaitTermination(NinjaUtils.WAIT_FOR_EXECUTOR_FINISH, TimeUnit.DAYS);

        handleResultOnFinish(progress, "Import finished");
    }

    @Override
    protected LogTarget getInfoLogTarget() {
        if (options.getInput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private ImportProducerWorker importByFilter(ObjectFilter filter, boolean stopAfterFound,
                                                BlockingQueue<PrismObject> queue, OperationStatus status) {
        return new ImportProducerWorker(context, options, queue, status, filter, stopAfterFound);
    }
}
