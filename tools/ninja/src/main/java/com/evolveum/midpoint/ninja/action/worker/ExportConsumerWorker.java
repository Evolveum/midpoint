/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportConsumerWorker extends BaseWorker<ExportOptions, PrismObject> {

    public ExportConsumerWorker(NinjaContext context, ExportOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation) {
        super(context, options, queue, operation);
    }

    @Override
    public void run() {
        Log log = context.getLog();

        // todo handle split option

        PrismSerializer<String> serializer = context.getPrismContext().xmlSerializer();

        try (Writer writer = createWriter()) {
            while (!shouldConsumerStop()) {
                PrismObject object = null;
                try {
                    object = queue.poll(CONSUMER_POLL_TIMEOUT, TimeUnit.SECONDS);
                    if (object == null) {
                        continue;
                    }

                    String xml = serializer.serialize(object);
                    writer.write(xml);
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
        } finally {
            markDone();

            if (isWorkersDone()) {
                operation.finish();
            }
        }
    }

    private Writer createWriter() throws IOException {
        Writer writer = NinjaUtils.createWriter(options.getOutput(), context.getCharset(), options.isZip());
        writer.write(NinjaUtils.XML_OBJECTS_PREFIX);

        return writer;
    }

    private void finalizeWriter(Writer writer) throws IOException {
        if (writer == null) {
            return;
        }

        writer.write(NinjaUtils.XML_OBJECTS_SUFFIX);
        writer.flush();
    }
}
