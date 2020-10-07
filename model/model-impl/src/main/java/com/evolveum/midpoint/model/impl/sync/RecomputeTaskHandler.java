/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
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
 */
@Component
public class RecomputeTaskHandler extends AbstractSearchIterativeModelTaskHandler<FocusType, AbstractSearchIterativeResultHandler<FocusType>> {

    public static final String HANDLER_URI = ModelPublicConstants.RECOMPUTE_HANDLER_URI;

    @Autowired private TaskManager taskManager;
    @Autowired private ContextFactory contextFactory;
    @Autowired private Clockwork clockwork;

    private static final Trace LOGGER = TraceManager.getTrace(RecomputeTaskHandler.class);

    public RecomputeTaskHandler() {
        super("Recompute", OperationConstants.RECOMPUTE);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    protected Class<? extends ObjectType> getType(Task task) {
        return getTypeFromTask(task, UserType.class);
    }

    @Override
    protected AbstractSearchIterativeResultHandler<FocusType> createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, RunningTask coordinatorTask,
            OperationResult opResult) throws SchemaException {

        ModelExecuteOptions options = getOptions(coordinatorTask);
        LOGGER.trace("ModelExecuteOptions: {}", options);

        AbstractSearchIterativeResultHandler<FocusType> handler = new AbstractSearchIterativeResultHandler<FocusType>(
                coordinatorTask, RecomputeTaskHandler.class.getName(), "recompute", "recompute task", partition, taskManager) {

            @Override
            protected boolean handleObject(PrismObject<FocusType> object, RunningTask workerTask, OperationResult result) throws CommonException, PreconditionViolationException {
                recompute(object, options, workerTask, partition, result);
                return true;
            }

        };
        handler.setStopOnError(false);
        return handler;
    }

    private ModelExecuteOptions getOptions(Task coordinatorTask) throws SchemaException {
        ModelExecuteOptions optionsFromTask = ModelImplUtils.getModelExecuteOptions(coordinatorTask);
        if (optionsFromTask != null) {
            return optionsFromTask;
        } else {
            // Make reconcile the default (for compatibility).
            return ModelExecuteOptions.create(prismContext).reconcile();
        }
    }

    private void recompute(PrismObject<FocusType> focalObject, ModelExecuteOptions options, Task task, TaskPartitionDefinitionType partition, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, PolicyViolationException, SecurityViolationException, PreconditionViolationException {
        LOGGER.trace("Recomputing object {}", focalObject);

        LensContext<FocusType> syncContext = contextFactory.createRecomputeContext(focalObject, options, task, result);
        LOGGER.trace("Recomputing object {}: context:\n{}", focalObject, syncContext.debugDumpLazily());

        if (partition != null && partition.getStage() == ExecutionModeType.SIMULATE) {
            clockwork.previewChanges(syncContext, null, task, result);
        } else {
            clockwork.run(syncContext, task, result);
        }
        LOGGER.trace("Recomputation of object {}: {}", focalObject, result.getStatus());
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
