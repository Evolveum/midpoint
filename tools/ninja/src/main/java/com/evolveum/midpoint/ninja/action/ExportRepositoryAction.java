package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.worker.ExportConsumerWorker;
import com.evolveum.midpoint.ninja.action.worker.ExportProducerWorker;
import com.evolveum.midpoint.ninja.action.worker.ProgressReporterWorker;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportRepositoryAction extends RepositoryAction<ExportOptions> {

    private static final String DOT_CLASS = ExportRepositoryAction.class.getName() + ".";

    private static final String OPERATION_EXPORT = DOT_CLASS + "export";

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;
    private static final long CONSUMERS_WAIT_FOR_START = 2000L;

    @Override
    public void execute() throws Exception {
        OperationResult result = new OperationResult(OPERATION_EXPORT);
        OperationStatus operation = new OperationStatus(result);

        // "+ 2" will be used for consumer and progress reporter
        ExecutorService executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

        BlockingQueue<PrismObject> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

        List<ExportProducerWorker> producers = createProducers(queue, operation);

        log.info("Starting export");
        operation.start();

        // execute as many producers as there are threads for them
        for (int i = 0; i < producers.size() && i < options.getMultiThread(); i++) {
            executor.execute(producers.get(i));
        }

        Thread.sleep(CONSUMERS_WAIT_FOR_START);

        executor.execute(new ProgressReporterWorker(context, options, queue, operation));

        ExportConsumerWorker consumer = new ExportConsumerWorker(context, options, queue, operation);
        executor.execute(consumer);

        // execute rest of the producers
        for (int i = options.getMultiThread(); i < producers.size(); i++) {
            executor.execute(producers.get(i));
        }

        executor.shutdown();
        executor.awaitTermination(NinjaUtils.WAIT_FOR_EXECUTOR_FINISH, TimeUnit.DAYS);

        handleResultOnFinish(operation, "Export finished");
    }

    @Override
    protected LogTarget getInfoLogTarget() {
        if (options.getOutput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private List<ExportProducerWorker> createProducers(BlockingQueue<PrismObject> queue, OperationStatus operation)
            throws SchemaException, IOException {

        List<ExportProducerWorker> producers = new ArrayList<>();

        if (options.getOid() != null) {
            ObjectTypes type = options.getType();
            if (type == null) {
                type = ObjectTypes.OBJECT;
            }

            InOidFilter filter = InOidFilter.createInOid(options.getOid());
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            producers.add(new ExportProducerWorker(context, options, queue, operation, type, query));
            return producers;
        }

        ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);

        List<ObjectTypes> types = NinjaUtils.getTypes(options.getType());
        for (ObjectTypes type : types) {
            producers.add(new ExportProducerWorker(context, options, queue, operation, type, query));
        }

        return producers;
    }
}
