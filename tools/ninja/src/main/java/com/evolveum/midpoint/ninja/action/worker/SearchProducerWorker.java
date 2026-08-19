/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action.worker;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Producer worker for all search-based operations, such as export and verify.
 *
 * Created by Viliam Repan (lazyman).
 */
public class SearchProducerWorker extends BaseWorker<ExportOptions, ObjectType> {

    private static final String OPERATION_SEARCH = SearchProducerWorker.class.getName() + ".search";

    private final ObjectTypes type;
    private final ObjectQuery query;

    public SearchProducerWorker(
            NinjaContext context, ExportOptions options, BlockingQueue<ObjectType> queue,
            OperationStatus operation, List<SearchProducerWorker> producers, ObjectTypes type, ObjectQuery query) {
        super(context, options, queue, operation, producers);

        this.type = type;
        this.query = query;
    }

    @Override
    public void run() {
        Log log = context.getLog();
        OperationResult result = new OperationResult(OPERATION_SEARCH + "." + type.getValue());

        try {
            GetOperationOptionsBuilder optionsBuilder = context.getSchemaService().getOperationOptionsBuilder();
            if (options.isRaw()) {
                optionsBuilder = optionsBuilder.raw();
            }

            optionsBuilder = NinjaUtils.addIncludeOptionsForExport(optionsBuilder, type.getClassDefinition());

            ResultHandler<?> handler = (object, parentResult) -> {
                try {
                    if (operation.isFinished()) {
                        return false;
                    }

                    queue.put(object.asObjectable());
                } catch (InterruptedException ex) {
                    log.error("Couldn't queue object {}, reason: {}", ex, object, ex.getMessage());
                }
                return true;
            };

            RepositoryService repository = context.getRepository();
            if (repository.supports(type.getClassDefinition())) {
                repository.searchObjectsIterative(type.getClassDefinition(), query, handler, optionsBuilder.build(), true, result);
            } else {
                log.debug("Type {} is not supported on current repository", type);
            }
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
