/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSearchSpecification;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.SearchSpecification;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Task part execution for model-level search-iterative tasks.
 *
 * Provides an implementation of functionality that is not present in repo-common
 * (where root search-iterative functionality resides), like calling model for search/count operations,
 * processing expressions in search queries, or authorizations.
 */
public abstract class AbstractModelSearchActivityExecution<O extends ObjectType,
        WD extends WorkDefinition,
        MAH extends ModelActivityHandler<WD, MAH>,
        AE extends AbstractModelSearchActivityExecution<O, WD, MAH, AE, BS>,
        BS extends AbstractActivityWorkStateType>
        extends AbstractSearchIterativeActivityExecution<O, WD, MAH, AE, BS> {

    public AbstractModelSearchActivityExecution(@NotNull ExecutionInstantiationContext<WD, MAH> context,
            @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    protected @NotNull SearchSpecification<O> createSearchSpecification(OperationResult opResult) throws SchemaException,
            ActivityExecutionException {
        ObjectSetSpecification objectSetSpecification = ObjectSetSpecification.fromWorkDefinition(activity.getWorkDefinition());
        if (objectSetSpecification instanceof ResourceObjectSetSpecificationImpl) {
            //noinspection unchecked
            return (SearchSpecification<O>) getModelBeans().syncTaskHelper.createSearchSpecification(
                    ((ResourceObjectSetSpecificationImpl) objectSetSpecification).getResourceObjectSetBean(),
                    getRunningTask(), opResult);
        } else {
            return super.createSearchSpecification(opResult);
        }
    }

    /**
     * Use only when the work definition produces {@link ResourceObjectSetSpecificationImpl}!
     * We perhaps should implement this in statically type-safe way, but we do not want to introduce yet another
     * class parameter. (Maybe it should be hacked into {@link ResourceObjectSetSpecificationProvider}?)
     */
    public @NotNull ResourceSearchSpecification getResourceSearchSpecification() {
        SearchSpecification<O> searchSpecification = getSearchSpecificationRequired();
        if (searchSpecification instanceof ResourceSearchSpecification) {
            return (ResourceSearchSpecification) searchSpecification;
        } else {
            throw new IllegalStateException("Expected ResourceSearchSpecification, got " + searchSpecification.getClass());
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
        if (isUseRepository()) {
            return countObjectsInRepository(opResult);
        } else {
            return getModelBeans().modelObjectResolver.countObjects(
                    getObjectType(), getQuery(), getSearchOptions(), getRunningTask(), opResult);
        }
    }

    @Override
    protected final void searchIterative(OperationResult opResult) throws CommonException {
        if (isUseRepository()) {
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
