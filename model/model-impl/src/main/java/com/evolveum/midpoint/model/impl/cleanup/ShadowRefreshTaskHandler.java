/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.cleanup;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.task.api.RunningTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.util.AbstractScannerResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractScannerTaskHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * Scanner that looks for pending operations in the shadows and updates the status.
 *
 * @author Radovan Semancik
 */
@Component
public class ShadowRefreshTaskHandler extends AbstractScannerTaskHandler<ShadowType, AbstractScannerResultHandler<ShadowType>> {

    // WARNING! This task handler is efficiently singleton!
    // It is a spring bean and it is supposed to handle all search task instances
    // Therefore it must not have task-specific fields. It can only contain fields specific to
    // all tasks of a specified type

    public static final String HANDLER_URI = ModelPublicConstants.SHADOW_REFRESH_TASK_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowRefreshTaskHandler.class);

    @Autowired
    protected ProvisioningService provisioningService;

    public ShadowRefreshTaskHandler() {
        super(ShadowType.class, "Shadow refresh", OperationConstants.SHADOW_REFRESH);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected Class<ShadowType> getType(Task task) {
        return ShadowType.class;
    }

    @Override
    protected boolean requiresDirectRepositoryAccess(AbstractScannerResultHandler<ShadowType> resultHandler,
            TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) {
        return true;
    }

    @Override
    protected ObjectQuery createQuery(AbstractScannerResultHandler<ShadowType> handler, TaskRunResult runResult,
            Task coordinatorTask, OperationResult opResult) throws SchemaException {
        ObjectQuery query = createQueryFromTask(handler, runResult, coordinatorTask, opResult);

        if (query.getFilter() == null) {
            ObjectFilter filter = prismContext.queryFor(ShadowType.class)
                    .exists(ShadowType.F_PENDING_OPERATION)
                .buildFilter();
            query.setFilter(filter);
        }

        return query;
    }

    @Override
    protected AbstractScannerResultHandler<ShadowType> createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, final RunningTask coordinatorTask,
            OperationResult opResult) {

        AbstractScannerResultHandler<ShadowType> handler = new AbstractScannerResultHandler<ShadowType>(
                coordinatorTask, ShadowRefreshTaskHandler.class.getName(), "shadowRefresh", "shadow refresh task", taskManager) {
            @Override
            protected boolean handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result) throws CommonException {
                LOGGER.trace("Refreshing {}", object);

                provisioningService.refreshShadow(object, null, workerTask, result);

                LOGGER.trace("Refreshed {}", object);
                return true;
            }
        };
        handler.setStopOnError(false);
        return handler;
    }
}
