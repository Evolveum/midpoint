/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class AbstractWriterConsumerWorker<OP extends ExportOptions> extends BaseWorker<OP, PrismObject> {

    public AbstractWriterConsumerWorker(NinjaContext context, OP options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    public void run() {
        Log log = context.getLog();

        // todo handle split option

        init();

        try (Writer writer = createWriter()) {
            while (!shouldConsumerStop()) {
                PrismObject object = null;
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
            markDone();

            if (isWorkersDone()) {
                operation.finish();
            }
        }
    }

    protected abstract void init();

    protected abstract String getProlog();

    protected abstract <O extends ObjectType> void write(Writer writer, PrismObject<O> object) throws SchemaException, IOException;

    protected abstract String getEpilog();

    private Writer createWriter() throws IOException {
        Writer writer = NinjaUtils.createWriter(options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite());
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
