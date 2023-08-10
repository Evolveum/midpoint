/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.evolveum.midpoint.ninja.action.BasicExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.commons.io.IOUtils;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class AbstractWriterConsumerWorker<O extends BasicExportOptions, T>
        extends BaseWorker<O, T> {

    public AbstractWriterConsumerWorker(NinjaContext context,
            O options, BlockingQueue<T> queue, OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    public void run() {
        Log log = context.getLog();

        init();

        Writer writer = null;
        try {
            writer = createWriter();

            while (!shouldConsumerStop()) {
                T object = null;
                try {
                    object = queue.poll(CONSUMER_POLL_TIMEOUT, TimeUnit.SECONDS);
                    if (object == null) {
                        continue;
                    }

                    write(writer, object);
                    writer.flush();

                    operation.incrementTotal();
                } catch (Exception ex) {
                    log.error("Couldn't store object {}, reason: {}", ex, object, ex.getMessage());
                    operation.incrementError();
                }
            }

            finalizeWriter(writer);
        } catch (IOException ex) {
            log.error("Unexpected exception, reason: {}", ex, ex.getMessage());
        } catch (NinjaException ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            if (options.getOutput() != null) {
                // we don't want to close stdout, e.g. only if we were writing to file
                IOUtils.closeQuietly(writer);
            }

            markDone();

            if (isWorkersDone()) {
                operation.finish();
            }

            destroy();
        }
    }

    protected void init() {
    }

    protected void destroy() {
    }

    protected abstract String getProlog();

    protected abstract void write(Writer writer, T object) throws SchemaException, IOException;

    protected abstract String getEpilog();

    private Writer createWriter() throws IOException {
        Writer writer = NinjaUtils.createWriter(
                options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite(), context.out);

        String prolog = getProlog();
        if (prolog != null) {
            writer.write(prolog);
        }

        return writer;
    }

    private void finalizeWriter(Writer writer) throws IOException {
        if (writer == null) {
            return;
        }

        String epilog = getEpilog();
        if (epilog != null) {
            writer.write(epilog);
        }
        writer.flush();
    }
}
