/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.executor.FocusChangeExecution;
import com.evolveum.midpoint.model.impl.lens.executor.ProjectionChangeExecution;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Executes precomputed changes.
 *
 * Does almost nothing by itself. Everything is delegated to other components:
 *
 * - {@link FocusChangeExecution}
 * - {@link ProjectionChangeExecution}
 *
 * TODO Move to `executor` package. But this is incompatible change regarding loggers and operation names.
 *
 * @author semancik
 */
@Component
public class ChangeExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    private static final String OPERATION_EXECUTE = ChangeExecutor.class.getName() + ".execute";
    public static final String OPERATION_EXECUTE_FOCUS = OPERATION_EXECUTE + ".focus";
    public static final String OPERATION_EXECUTE_PROJECTION = OPERATION_EXECUTE + ".projection";
    public static final String OPERATION_EXECUTE_DELTA = ChangeExecutor.class.getName() + ".executeDelta";

    @Autowired private ModelBeans modelBeans;

    /**
     * Returns true if current wave has to be restarted, see {@link ObjectAlreadyExistsException} handling.
     */
    public <O extends ObjectType> boolean executeChanges(LensContext<O> context, Task task,
            OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, PreconditionViolationException, PolicyViolationException {

        OperationResult result = parentResult.createSubresult(OPERATION_EXECUTE);

        try {

            executeFocusChanges(context, task, result);
            return executeProjectionsChanges(context, task, result);

        } catch (Throwable t) {
            result.recordThrowableIfNeeded(t); // last resort: to avoid UNKNOWN subresults
            throw t;
        } finally {
            // Result computation here needs to be slightly different (i.e. composite)
            result.computeStatusIfUnknownComposite();
        }
    }

    private <O extends ObjectType> void executeFocusChanges(LensContext<O> context, Task task, OperationResult result)
            throws SchemaException, PolicyViolationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PreconditionViolationException,
            ObjectAlreadyExistsException {
        context.checkAbortRequested();

        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return;
        }

        new FocusChangeExecution<>(context, focusContext, task, modelBeans)
                .execute(result);
    }

    private <O extends ObjectType> boolean executeProjectionsChanges(LensContext<O> context, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException,
            PreconditionViolationException {

        boolean restartRequested = false;

        for (LensProjectionContext projCtx : context.getProjectionContexts()) {

            context.checkAbortRequested();

            ProjectionChangeExecution<O> execution = new ProjectionChangeExecution<>(context, projCtx, task, modelBeans);
            execution.execute(result);

            restartRequested = restartRequested || execution.isRestartRequested();
        }

        LOGGER.trace("Restart requested = {}", restartRequested);
        LensFocusContext<O> focusContext = context.getFocusContext();
        if (restartRequested && focusContext != null) {
            focusContext.setFresh(false); // will run activation again (hopefully)
        }
        return restartRequested;
    }
}
