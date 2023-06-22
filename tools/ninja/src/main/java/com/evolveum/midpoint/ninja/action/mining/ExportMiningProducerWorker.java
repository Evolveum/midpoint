/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.worker.BaseWorker;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ExportMiningProducerWorker extends BaseWorker<ExportMiningOptions, FocusType> {

    private final ObjectQuery query;
    private final Class<?> type;

    public ExportMiningProducerWorker(
            NinjaContext context, ExportMiningOptions options, BlockingQueue<FocusType> queue,
            OperationStatus operation, List<ExportMiningProducerWorker> producers, ObjectQuery query, Class<?> type) {
        super(context, options, queue, operation, producers);

        this.query = query;
        this.type = type;
    }

    @Override
    public void run() {

        Log log = context.getLog();
        try {
            GetOperationOptionsBuilder optionsBuilder = context.getSchemaService().getOperationOptionsBuilder();

            if (type.equals(RoleType.class)) {
                optionsBuilder = NinjaUtils.addIncludeOptionsForExport(optionsBuilder, RoleType.class);
                context.getRepository().searchObjectsIterative(RoleType.class, query, getRoleTypeResultHandler(log),
                        optionsBuilder.build(), true, operation.getResult());
            } else if (type.equals(UserType.class)) {
                optionsBuilder = NinjaUtils.addIncludeOptionsForExport(optionsBuilder, UserType.class);
                context.getRepository().searchObjectsIterative(UserType.class, query, getUserTypeResultHandler(log),
                        optionsBuilder.build(), true, operation.getResult());
            } else if (type.equals(OrgType.class)) {

                optionsBuilder = NinjaUtils.addIncludeOptionsForExport(optionsBuilder, OrgType.class);
                context.getRepository().searchObjectsIterative(OrgType.class, query, getOrgTypeResultHandler(log),
                        optionsBuilder.build(), true, operation.getResult());
            }

        } catch (NinjaException ex) {
            log.error(ex.getMessage(), ex);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        } finally {
            markDone();

            if (isWorkersDone()) {
                if (!operation.isFinished()) {
                    operation.producerFinish();
                }
            }
        }
    }

    @NotNull
    private ResultHandler<OrgType> getOrgTypeResultHandler(Log log) {
        return (object, parentResult) -> {
            try {
                queue.put(object.asObjectable());
            } catch (InterruptedException ex) {
                log.error("Couldn't queue orgType object {}, reason: {}", ex, object, ex.getMessage());
            }
            return true;
        };
    }

    @NotNull
    private ResultHandler<UserType> getUserTypeResultHandler(Log log) {
        return (object, parentResult) -> {
            try {
                queue.put(object.asObjectable());
            } catch (InterruptedException ex) {
                log.error("Couldn't queue userType object {}, reason: {}", ex, object, ex.getMessage());
            }
            return true;
        };
    }

    @NotNull
    private ResultHandler<RoleType> getRoleTypeResultHandler(Log log) {
        return (object, parentResult) -> {
            try {
                queue.put(object.asObjectable());
            } catch (InterruptedException ex) {
                log.error("Couldn't queue roleType object {}, reason: {}", ex, object, ex.getMessage());
            }
            return true;
        };
    }

}
