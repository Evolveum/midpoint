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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Producer worker for all search-based operations, such as export and verify.
 *
 * Created by Viliam Repan (lazyman).
 */
public class SearchProducerWorker extends BaseWorker<ExportOptions, PrismObject> {

    private ObjectTypes type;
    private ObjectQuery query;

    public SearchProducerWorker(NinjaContext context, ExportOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation, List<SearchProducerWorker> producers,
                                ObjectTypes type, ObjectQuery query) {
        super(context, options, queue, operation, producers);

        this.type = type;
        this.query = query;
    }

    @Override
    public void run() {
        Log log = context.getLog();

        try {
            GetOperationOptionsBuilder optionsBuilder = context.getSchemaService().getOperationOptionsBuilder();
            if (options.isRaw()) {
                optionsBuilder = optionsBuilder.raw();
            }

            optionsBuilder = NinjaUtils.addIncludeOptionsForExport(optionsBuilder, type.getClassDefinition());

            ResultHandler handler = (object, parentResult) -> {
                try {
                    queue.put(object);
                } catch (InterruptedException ex) {
                    log.error("Couldn't queue object {}, reason: {}", ex, object, ex.getMessage());
                }
                return true;
            };

            RepositoryService repository = context.getRepository();
            repository.searchObjectsIterative(type.getClassDefinition(), query, handler, optionsBuilder.build(), true, operation.getResult());
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
