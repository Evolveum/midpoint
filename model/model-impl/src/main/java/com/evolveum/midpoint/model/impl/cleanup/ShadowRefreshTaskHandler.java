/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.cleanup;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;

import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScannerResultHandler;
import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScannerTaskExecution;
import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScannerTaskHandler;
import com.evolveum.midpoint.model.impl.tasks.scanner.AbstractScannerTaskPartExecution;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.PartExecutionClass;
import com.evolveum.midpoint.repo.common.task.ResultHandlerClass;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Scanner that looks for pending operations in the shadows and updates the status.
 *
 * TODO migrate to simple iterative task subclass
 *
 * @author Radovan Semancik
 */
@Component
@TaskExecutionClass(ShadowRefreshTaskHandler.TaskExecution.class)
@PartExecutionClass(ShadowRefreshTaskHandler.PartExecution.class)
public class ShadowRefreshTaskHandler
        extends AbstractScannerTaskHandler
        <ShadowRefreshTaskHandler, ShadowRefreshTaskHandler.TaskExecution> {

    public static final String HANDLER_URI = ModelPublicConstants.SHADOW_REFRESH_TASK_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowRefreshTaskHandler.class);

    public ShadowRefreshTaskHandler() {
        super("Shadow refresh", OperationConstants.SHADOW_REFRESH);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @ResultHandlerClass(PartExecution.Handler.class)
    @HandledObjectType(ShadowType.class)
    public class PartExecution
            extends AbstractScannerTaskPartExecution
            <ShadowType,
                    ShadowRefreshTaskHandler,
                    ShadowRefreshTaskHandler.TaskExecution,
                    PartExecution,
                    PartExecution.Handler> {

        public PartExecution(ShadowRefreshTaskHandler.TaskExecution ctx) {
            super(ctx);
        }

        @Override
        protected boolean requiresDirectRepositoryAccess(OperationResult opResult) {
            return true;
        }

        @Override
        protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException {
            ObjectQuery query = createQueryFromTask();

            if (query.getFilter() == null) {
                ObjectFilter filter = prismContext.queryFor(ShadowType.class)
                        .exists(ShadowType.F_PENDING_OPERATION)
                        .buildFilter();
                query.setFilter(filter);
            }

            return query;
        }

        public class Handler
                extends AbstractScannerResultHandler
                <ShadowType,
                        ShadowRefreshTaskHandler,
                        ShadowRefreshTaskHandler.TaskExecution,
                        PartExecution,
                        PartExecution.Handler> {

            public Handler(PartExecution taskExecution) {
                super(taskExecution);
            }

            @Override
            protected boolean handleObject(PrismObject<ShadowType> object, RunningTask workerTask, OperationResult result)
                    throws CommonException, PreconditionViolationException {
                LOGGER.trace("Refreshing {}", object);
                provisioningService.refreshShadow(object, null, workerTask, result);
                LOGGER.trace("Refreshed {}", object);
                return true;
            }
        }
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    /** Just to make Java compiler happy. */

    public static class TaskExecution
            extends AbstractScannerTaskExecution<ShadowRefreshTaskHandler, ShadowRefreshTaskHandler.TaskExecution> {

        public TaskExecution(ShadowRefreshTaskHandler taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
