/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Iterator;

import static com.evolveum.midpoint.model.impl.lens.LensContext.*;

/**
 * Handles a "ModelOperation task" - executes a given model operation in a context
 * of the task (i.e., in most cases, asynchronously).
 *
 * The context of the model operation (i.e., model context) is stored in task property
 * called "modelContext". When this handler is executed, the context is retrieved, unwrapped from
 * its XML representation, and the model operation is (re)started.
 *
 * This was to be used for workflow execution. Currently this responsibility is moved to CaseOperationExecutionTaskHandler
 * and this class is unused.
 *
 * CURRENTLY UNUSED.
 */
@Component
public class ModelOperationTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ModelOperationTaskHandler.class);

    private static final String DOT_CLASS = ModelOperationTaskHandler.class.getName() + ".";

    private static final String MODEL_OPERATION_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/model/operation/handler-3";

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private Clockwork clockwork;

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
        TaskRunResult runResult = new TaskRunResult();

        LensContextType contextType = task.getModelOperationContext();
        if (contextType == null) {
            LOGGER.trace("No model context found, skipping the model operation execution.");
            if (result.isUnknown()) {
                result.computeStatus();
            }
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        } else {
            LensContext<?> context;
            //noinspection TryWithIdenticalCatches
            try {
                context = fromLensContextType(contextType, prismContext, provisioningService, task, result);
            } catch (SchemaException e) {
                throw new SystemException("Cannot recover model context from task " + task + " due to schema exception", e);
            } catch (ObjectNotFoundException | ConfigurationException | ExpressionEvaluationException e) {
                throw new SystemException("Cannot recover model context from task " + task, e);
            } catch (CommunicationException e) {
                throw new SystemException("Cannot recover model context from task " + task, e);     // todo wait and retry
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Context to be executed = {}", context.debugDump());
            }

            try {
                // here we brutally remove all the projection contexts -- because if we are continuing after rejection of a role/resource assignment
                // that resulted in such projection contexts, we DO NOT want them to appear in the context any more
                context.rot("assignment rejection");
                Iterator<LensProjectionContext> projectionIterator = context.getProjectionContextsIterator();
                while (projectionIterator.hasNext()) {
                    LensProjectionContext projectionContext = projectionIterator.next();
                    if (!ObjectDelta.isEmpty(projectionContext.getPrimaryDelta()) || !ObjectDelta.isEmpty(projectionContext.getSyncDelta())) {
                        continue;       // don't remove client requested or externally triggered actions!
                    }
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Removing projection context {}", projectionContext.getHumanReadableName());
                    }
                    projectionIterator.remove();
                }
                if (task.getChannel() == null) {
                    task.setChannel(context.getChannel());
                }
                clockwork.run(context, task, result);

                task.setModelOperationContext(context.toLensContextType(context.getState() == ModelState.FINAL ? ExportType.REDUCED : ExportType.OPERATIONAL));
                task.flushPendingModifications(result);

                if (result.isUnknown()) {
                    result.computeStatus();
                }
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
            } catch (CommonException | PreconditionViolationException | RuntimeException | Error e) {
                String message = "An exception occurred within model operation, in task " + task;
                LoggingUtils.logUnexpectedException(LOGGER, message, e);
                result.recordPartialError(message, e);
                // TODO: here we do not know whether the error is temporary or permanent (in the future we could discriminate on the basis of particular exception caught)
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
            }
        }

        task.getResult().recomputeStatus();
        runResult.setOperationResult(task.getResult());
        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return null; // null - as *not* to record progress
    }

    @Override
    public void refreshStatus(Task task) {
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(MODEL_OPERATION_TASK_URI, this);
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
