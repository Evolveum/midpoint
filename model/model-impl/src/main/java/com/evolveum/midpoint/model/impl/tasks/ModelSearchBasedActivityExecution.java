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
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Activity execution for model-level search-based activities.
 *
 * Provides an implementation of functionality that is not present in repo-common
 * (where common search-based functionality resides), like:
 *
 * 1. calling model for search/count operations,
 * 2. processing expressions in search queries (although this could be solved in repo-common!),
 * 3. authorizations.
 */
public class ModelSearchBasedActivityExecution<O extends ObjectType,
        WD extends WorkDefinition,
        MAH extends ModelActivityHandler<WD, MAH>,
        WS extends AbstractActivityWorkStateType>
        extends SearchBasedActivityExecution<O, WD, MAH, WS> {

    public ModelSearchBasedActivityExecution(@NotNull ExecutionInstantiationContext<WD, MAH> context,
            @NotNull String shortNameCapitalized,
            @NotNull SearchBasedActivityExecution.SearchBasedSpecificsSupplier<O, WD, MAH> specificsSupplier) {
        super(context, shortNameCapitalized, specificsSupplier);
    }

    protected @NotNull SearchSpecification<O> createSearchSpecificationFromObjectSetSpec(
            @NotNull ObjectSetSpecification objectSetSpecification, OperationResult result)
            throws SchemaException, ActivityExecutionException {
        if (objectSetSpecification instanceof ResourceObjectSetSpecificationImpl) {
            //noinspection unchecked
            return (SearchSpecification<O>) getModelBeans().syncTaskHelper.createSearchSpecification(
                    ((ResourceObjectSetSpecificationImpl) objectSetSpecification).getResourceObjectSetBean(),
                    getRunningTask(), result);
        } else {
            return super.createSearchSpecificationFromObjectSetSpec(objectSetSpecification, result);
        }
    }

    @Override
    protected ObjectQuery evaluateQueryExpressions(ObjectQuery query, OperationResult opResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        // TODO consider which variables should go here (there's no focus, shadow, resource - only configuration)
        if (!ExpressionUtil.hasExpressions(query.getFilter())) {
            return query;
        }

        PrismObject<SystemConfigurationType> configuration = getModelBeans().systemObjectCache.getSystemConfiguration(opResult);
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, null, null,
                configuration != null ? configuration.asObjectable() : null, getPrismContext());
        try {
            ExpressionEnvironment<?,?,?> env = new ExpressionEnvironment<>(getRunningTask(), opResult);
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
            return ExpressionUtil.evaluateQueryExpressions(query, variables, getExpressionProfile(),
                    getModelBeans().expressionFactory, getPrismContext(), "evaluate query expressions",
                    getRunningTask(), opResult);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    @Override
    protected void applyDefinitionsToQuery(OperationResult opResult) throws CommonException {
        if (Boolean.TRUE.equals(searchSpecification.getUseRepository())) {
            return;
        }
        Class<O> objectType = searchSpecification.getObjectType();
        if (ObjectTypes.isClassManagedByProvisioning(objectType)) {
            getModelBeans().provisioningService
                    .applyDefinition(objectType, searchSpecification.getQuery(), getRunningTask(), opResult);
        }
    }

    @Override
    protected final Integer countObjects(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (searchSpecification.isUseRepository()) {
            return countObjectsInRepository(opResult);
        } else {
            return getModelBeans().modelObjectResolver.countObjects(
                    getObjectType(), getQuery(), getSearchOptions(), getRunningTask(), opResult);
        }
    }

    @Override
    protected final void searchIterative(OperationResult opResult) throws CommonException {
        if (searchSpecification.isUseRepository()) {
            searchIterativeInRepository(opResult);
        } else {
            getModelBeans().modelObjectResolver.searchIterative(
                    getObjectType(), getQuery(), getSearchOptions(), createSearchResultHandler(), getRunningTask(), opResult);
        }
    }

    @Override
    protected boolean modelProcessingAvailable() {
        return true;
    }

    @Override
    protected @NotNull ObjectPreprocessor<O> createShadowFetchingPreprocessor() {
        SearchSpecification<O> searchSpec = getSearchSpecificationRequired();
        argCheck(searchSpec.concernsShadows(),
                "FETCH_FAILED_OBJECTS processing is available only for shadows, not for %s", searchSpec.getObjectType());
        //noinspection unchecked
        return (ObjectPreprocessor<O>) new ShadowFetchingPreprocessor(this);
    }

    @Override
    protected void checkRawAuthorization(Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        getModelBeans().securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), null,
                AuthorizationParameters.EMPTY, null, task, result);
    }

    public @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
