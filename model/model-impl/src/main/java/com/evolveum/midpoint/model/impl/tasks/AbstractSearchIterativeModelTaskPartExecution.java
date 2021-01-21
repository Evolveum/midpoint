/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskExecution;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskPartExecution;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Task part execution for model-level search-iterative tasks.
 *
 * Provides an implementation of functionality that is not present in repo-common
 * (where root search-iterative functionality resides), like calling model for search/count operations,
 * processing expressions in search queries, or authorizations.
 */
public abstract class AbstractSearchIterativeModelTaskPartExecution<O extends ObjectType,
        TH extends AbstractSearchIterativeModelTaskHandler<TH, TE>,
        TE extends AbstractSearchIterativeTaskExecution<TH, TE>,
        PE extends AbstractSearchIterativeModelTaskPartExecution<O, TH, TE, PE, RH>,
        RH extends AbstractSearchIterativeResultHandler<O, TH, TE, PE, RH>>
        extends AbstractSearchIterativeTaskPartExecution<O, TH, TE, PE, RH> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchIterativeModelTaskPartExecution.class);

    public AbstractSearchIterativeModelTaskPartExecution(TE ctx) {
        super(ctx);
    }

    @Override
    protected ObjectQuery preProcessQuery(ObjectQuery query, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        // TODO consider which variables should go here (there's no focus, shadow, resource - only configuration)
        if (!ExpressionUtil.hasExpressions(query.getFilter())) {
            return query;
        }

        PrismObject<SystemConfigurationType> configuration = taskHandler.systemObjectCache.getSystemConfiguration(opResult);
        ExpressionVariables variables = ModelImplUtils.getDefaultExpressionVariables(null, null, null,
                configuration != null ? configuration.asObjectable() : null, getPrismContext());
        try {
            ExpressionEnvironment<?,?,?> env = new ExpressionEnvironment<>(localCoordinatorTask, opResult);
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
            return ExpressionUtil.evaluateQueryExpressions(query, variables, getExpressionProfile(),
                    taskHandler.expressionFactory, getPrismContext(), "evaluate query expressions",
                    localCoordinatorTask, opResult);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    @Override
    protected final Integer countObjects(OperationResult opResult) throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (useRepository) {
            return countObjectsInRepository(opResult);
        } else {
            return taskHandler.modelObjectResolver.countObjects(objectType, query, searchOptions, localCoordinatorTask, opResult);
        }
    }

    @Override
    protected final void searchIterative(OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (useRepository) {
            searchIterativeInRepository(opResult);
        } else {
            taskHandler.modelObjectResolver.searchIterative(objectType, query, searchOptions, resultHandler, localCoordinatorTask, opResult);
        }
    }

    // TODO eliminate "task" parameter
    protected final ModelExecuteOptions getExecuteOptionsFromTask(Task task) {
        ModelExecuteOptionsType options = task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
        return options != null ? ModelExecuteOptions.fromModelExecutionOptionsType(options) : null;
    }

    @Override
    protected void checkRawAuthorization(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        taskHandler.securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), null, AuthorizationParameters.EMPTY, null, task, result);
    }
}
