/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * The task handler for object recompute.
 *
 *  This handler takes care of executing recompute "runs". The task will iterate over all objects of a given type
 *  and recompute their assignments and expressions. This is needed after the expressions are changed,
 *  e.g in resource outbound expressions or in a role definition.
 *
 * @author Radovan Semancik
 *
 * TODO REMOVE
 *
 */
@Component
@TaskExecutionClass(RecomputeTaskHandlerOld.TaskExecution.class)
@PartExecutionClass(RecomputeTaskHandlerOld.PartExecution.class)
public class RecomputeTaskHandlerOld
        extends AbstractSearchIterativeModelTaskHandler
        <RecomputeTaskHandlerOld, RecomputeTaskHandlerOld.TaskExecution> {

    public static final String HANDLER_URI = ModelPublicConstants.RECOMPUTE_HANDLER_URI + ".OLD";

    @Autowired private TaskManager taskManager;
    @Autowired private ContextFactory contextFactory;
    @Autowired private Clockwork clockwork;

    private static final Trace LOGGER = TraceManager.getTrace(RecomputeTaskHandlerOld.class);

    public RecomputeTaskHandlerOld() {
        super("Recompute", OperationConstants.RECOMPUTE);
        reportingOptions.setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @ResultHandlerClass(PartExecution.Handler.class)
    @DefaultHandledObjectType(UserType.class) // TODO change to FocusType eventually
    protected class PartExecution
            extends AbstractSearchIterativeModelTaskPartExecution
            <FocusType,
                    RecomputeTaskHandlerOld,
                    RecomputeTaskHandlerOld.TaskExecution,
                    PartExecution, PartExecution.Handler> {

        private ModelExecuteOptions options;

        public PartExecution(RecomputeTaskHandlerOld.TaskExecution taskExecution) {
            super(taskExecution);
        }

        @Override
        protected void initialize(OperationResult opResult) {
            options = getOptions();
            LOGGER.trace("ModelExecuteOptions: {}", options);
        }

        private ModelExecuteOptions getOptions() {
            ModelExecuteOptions optionsFromTask = ModelImplUtils.getModelExecuteOptions(localCoordinatorTask);
            if (optionsFromTask != null) {
                return optionsFromTask;
            } else {
                // Make reconcile the default (for compatibility).
                return ModelExecuteOptions.create(getPrismContext()).reconcile();
            }
        }

        protected class Handler
                extends AbstractSearchIterativeResultHandler
                <FocusType,
                        RecomputeTaskHandlerOld,
                        RecomputeTaskHandlerOld.TaskExecution,
                        PartExecution, Handler> {

            // Accessed from the outside
            public Handler(PartExecution taskExecution) {
                super(taskExecution);
            }

            @Override
            protected boolean handleObject(PrismObject<FocusType> object, RunningTask workerTask, OperationResult result)
                    throws CommonException, PreconditionViolationException {
                recompute(object, workerTask, result);
                return true;
            }

            private void recompute(PrismObject<FocusType> focalObject, Task task, OperationResult result) throws SchemaException,
                    ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
                    ConfigurationException, PolicyViolationException, SecurityViolationException, PreconditionViolationException {
                LOGGER.trace("Recomputing object {}", focalObject);

                LensContext<FocusType> syncContext = contextFactory.createRecomputeContext(focalObject, options, task, result);
                LOGGER.trace("Recomputing object {}: context:\n{}", focalObject, syncContext.debugDumpLazily());

                TaskPartitionDefinitionType partDef = partExecution.partDefinition;
                if (partDef != null && partDef.getStage() == ExecutionModeType.SIMULATE) {
                    clockwork.previewChanges(syncContext, null, task, result);
                } else {
                    clockwork.run(syncContext, task, result);
                }
                LOGGER.trace("Recomputation of object {}: {}", focalObject, result.getStatus());
            }
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

    /** Just to make Java compiler happy. */
    protected static class TaskExecution
            extends AbstractSearchIterativeTaskExecution<RecomputeTaskHandlerOld, RecomputeTaskHandlerOld.TaskExecution> {

        public TaskExecution(RecomputeTaskHandlerOld taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
