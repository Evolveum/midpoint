package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.worker.ImportConsumerWorker;
import com.evolveum.midpoint.ninja.action.worker.ImportProducerWorker;
import com.evolveum.midpoint.ninja.action.worker.ProgressReporterWorker;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ImportOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        try (Reader reader = createReader()) {

            OperationStatus progress = new OperationStatus();

            queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

            // + 2 means producer and progress reporter
            executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

            String oid = options.getOid();

            ImportProducerWorker producer;
            if (oid != null) {
                producer = importByOid(reader);
            } else {
                producer = importByFilter(reader);
            }
            producer.setOperation(progress);

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
    }

    @Override
    protected LogTarget getInfoLogTarget() {
        if (options.getInput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private Reader createReader() throws IOException {
        Charset charset = context.getCharset();

        File input = options.getInput();

        InputStream is;
        if (input != null) {
            if (!input.exists()) {
                throw new NinjaException("Import file '" + input.getPath() + "' doesn't exist");
            }

            is = new FileInputStream(input);
        } else {
            is = System.in;
        }

        if (options.isZip()) {
            ZipInputStream zis = new ZipInputStream(is);

            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                break;
            }
            is = zis;
        }

        return new InputStreamReader(is, charset);
    }

    private ImportProducerWorker importByOid(Reader reader) {
        InOidFilter filter = InOidFilter.createInOid(options.getOid());

        return importByFilter(filter, true, reader);
    }

    private ImportProducerWorker importByFilter(Reader reader) throws SchemaException, IOException {
        ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context);

        return importByFilter(filter, false, reader);
    }

    private ImportProducerWorker importByFilter(ObjectFilter filter, boolean stopAfterFound, Reader reader) {
        return new ImportProducerWorker(context, options, queue, filter, stopAfterFound, reader);
    }
}
