/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Task part execution for model-level search-iterative tasks.
 *
 * Provides an implementation of functionality that is not present in repo-common
 * (where root search-iterative functionality resides), like calling model for search/count operations,
 * processing expressions in search queries, or authorizations.
 */
public abstract class AbstractIterativeModelTaskPartExecution<O extends ObjectType,
        TH extends AbstractModelTaskHandler<TH, TE>,
        TE extends AbstractTaskExecution<TH, TE>,
        PE extends AbstractIterativeModelTaskPartExecution<O, TH, TE, PE, RH>,
        RH extends AbstractSearchIterativeItemProcessor<O, TH, TE, PE, RH>>
        extends AbstractSearchIterativeTaskPartExecution<O, TH, TE, PE, RH> {

    public AbstractIterativeModelTaskPartExecution(TE ctx) {
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
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, null, null,
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
            taskHandler.modelObjectResolver.searchIterative(objectType, query, searchOptions, createSearchResultHandler(),
                    localCoordinatorTask, opResult);
        }
    }

    @Override
    protected boolean modelProcessingAvailable() {
        return true;
    }

    @Override
    protected @NotNull ObjectPreprocessor<O> createShadowFetchingPreprocessor() {
        argCheck(ShadowType.class.equals(objectType),
                "FETCH_FAILED_OBJECTS processing is available only for shadows, not for %s", objectType);
        //noinspection unchecked
        return (ObjectPreprocessor<O>) new ShadowFetchingPreprocessor(this);
    }

    @Override
    protected void checkRawAuthorization(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        taskHandler.securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), null, AuthorizationParameters.EMPTY, null, task, result);
    }
}
