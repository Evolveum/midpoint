/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext;
import com.evolveum.midpoint.model.impl.tasks.simple.Processing;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleIterativeTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The task handler for object recompute.
 *
 *  This handler takes care of executing recompute "runs". The task will iterate over all objects of a given type
 *  and recompute their assignments and expressions. This is needed after the expressions are changed,
 *  e.g in resource outbound expressions or in a role definition.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class RecomputeTaskHandler
        extends SimpleIterativeTaskHandler
        <FocusType,
                RecomputeTaskHandler.MyExecutionContext,
                RecomputeTaskHandler.MyProcessing> {

    public static final String HANDLER_URI = ModelPublicConstants.RECOMPUTE_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(RecomputeTaskHandler.class);

    public RecomputeTaskHandler() {
        super(LOGGER, "Recompute", OperationConstants.RECOMPUTE);
        reportingOptions.setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected MyExecutionContext createExecutionContext() {
        return new MyExecutionContext();
    }

    @Override
    protected MyProcessing createProcessing(MyExecutionContext ctx) {
        return new MyProcessing(ctx);
    }

    public class MyExecutionContext extends ExecutionContext {

        private ModelExecuteOptions executeOptions;

        @Override
        protected void initialize(OperationResult opResult) {
            executeOptions = getExecuteOptions();
            LOGGER.trace("ModelExecuteOptions: {}", executeOptions);
        }

        private ModelExecuteOptions getExecuteOptions() {
            ModelExecuteOptions optionsFromTask = ModelImplUtils.getModelExecuteOptions(getLocalCoordinationTask());
            if (optionsFromTask != null) {
                return optionsFromTask;
            } else {
                // Make reconcile the default (for compatibility).
                return ModelExecuteOptions.create(getPrismContext()).reconcile();
            }
        }
    }

    public class MyProcessing extends Processing<FocusType, MyExecutionContext> {

        private MyProcessing(MyExecutionContext ctx) {
            super(ctx);
        }

        @Override
        protected Class<? extends FocusType> determineObjectType(Class<? extends FocusType> configuredType) {
            return defaultIfNull(configuredType, UserType.class);
        }

        @Override
        protected void handleObject(PrismObject<FocusType> object, RunningTask workerTask, OperationResult result)
                throws CommonException, PreconditionViolationException {
            LOGGER.trace("Recomputing object {}", object);

            LensContext<FocusType> syncContext = contextFactory.createRecomputeContext(object, ctx.executeOptions, workerTask, result);
            LOGGER.trace("Recomputing object {}: context:\n{}", object, syncContext.debugDumpLazily());

            TaskPartitionDefinitionType partDef = ctx.getPartDefinition();
            if (partDef != null && partDef.getStage() == ExecutionModeType.SIMULATE) {
                clockwork.previewChanges(syncContext, null, workerTask, result);
            } else {
                clockwork.run(syncContext, workerTask, result);
            }
            LOGGER.trace("Recomputation of object {}: {}", object, result.getStatus());
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.RECOMPUTATION;
    }

    @Override
    public String getDefaultChannel() {
        return Channel.RECOMPUTATION.getUri();
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value();
    }
}
