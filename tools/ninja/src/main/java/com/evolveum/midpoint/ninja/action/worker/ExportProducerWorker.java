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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportProducerWorker extends BaseWorker<ExportOptions, PrismObject> {

    private ObjectTypes type;
    private ObjectQuery query;

    public ExportProducerWorker(NinjaContext context, ExportOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation, List<ExportProducerWorker> producers,
                                ObjectTypes type, ObjectQuery query) {
        super(context, options, queue, operation, producers);

        this.type = type;
        this.query = query;
    }

    @Override
    public void run() {
        Log log = context.getLog();

        try {
            Collection<SelectorOptions<GetOperationOptions>> opts = new ArrayList<>();
            if (options.isRaw()) {
                opts = GetOperationOptions.createRawCollection();
            }

            NinjaUtils.addIncludeOptionsForExport(opts, type.getClassDefinition());

            ResultHandler handler = (object, parentResult) -> {
                try {
                    queue.put(object);
                } catch (InterruptedException ex) {
                    log.error("Couldn't queue object {}, reason: {}", ex, object, ex.getMessage());
                }
                return true;
            };

            RepositoryService repository = context.getRepository();
            repository.searchObjectsIterative(type.getClassDefinition(), query, handler, opts, false, operation.getResult());
        } catch (SchemaException ex) {
            log.error("Unexpected exception, reason: {}", ex, ex.getMessage());
        } finally {
            markDone();

            if (isWorkersDone()) {
                operation.producerFinish();
            }
        }
    }
}
