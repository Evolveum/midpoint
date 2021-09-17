/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.task.*;

import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AdvancedActivityExecutionSupportImpl implements AdvancedActivityExecutionSupport {

    @Autowired private SyncTaskHelper syncTaskHelper;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private ModelObjectResolver modelObjectResolver;

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public @NotNull SearchSpecification<?> createSearchSpecificationFromResourceObjectSetSpec(
            @NotNull ResourceObjectSetSpecificationImpl objectSetSpecification, @NotNull RunningTask task, OperationResult result)
            throws SchemaException, ActivityExecutionException {
        return syncTaskHelper.createSearchSpecification(
                objectSetSpecification.getResourceObjectSetBean(),
                task, result);
    }

    @Override
    public ObjectQuery evaluateQueryExpressions(@NotNull ObjectQuery query, ExpressionProfile expressionProfile,
            @NotNull RunningTask task, OperationResult result)
            throws CommonException {
        PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(result);
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, null, null,
                configuration != null ? configuration.asObjectable() : null, PrismContext.get());
        try {
            ExpressionEnvironment<?,?,?> env = new ExpressionEnvironment<>(task, result);
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
            return ExpressionUtil.evaluateQueryExpressions(query, variables, expressionProfile,
                    expressionFactory, prismContext, "evaluate query expressions",
                    task, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    @Override
    public void applyDefinitionsToQuery(@NotNull SearchSpecification<?> searchSpecification, @NotNull Task task,
            OperationResult result) throws CommonException {
        Class<? extends ObjectType> objectType = searchSpecification.getObjectType();
        if (!searchSpecification.isUseRepository() && ObjectTypes.isClassManagedByProvisioning(objectType)) {
            provisioningService
                    .applyDefinition(objectType, searchSpecification.getQuery(), task, result);
        }
    }

    @Override
    public void checkRawAuthorization(Task task, OperationResult result) throws CommonException {
        securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), null,
                AuthorizationParameters.EMPTY, null, task, result);
    }

    @Override
    public Integer countObjects(@NotNull SearchSpecification<?> searchSpecification, @NotNull RunningTask task,
            @NotNull OperationResult result) throws CommonException {
        return modelObjectResolver.countObjects(
                searchSpecification.getObjectType(),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                task, result);
    }

    @Override
    public <O extends ObjectType> void searchIterative(@NotNull SearchSpecification<O> searchSpecification,
            @NotNull ResultHandler<O> handler, @NotNull RunningTask task, @NotNull OperationResult result)
            throws CommonException {
        modelObjectResolver.searchIterative(
                searchSpecification.getObjectType(),
                searchSpecification.getQuery(),
                searchSpecification.getSearchOptions(),
                handler, task, result);
    }

    @Override
    public ObjectPreprocessor<ShadowType> createShadowFetchingPreprocessor(
            @NotNull SearchBasedActivityExecution<?, ?, ?, ?> activityExecution) {
        return new ShadowFetchingPreprocessor(activityExecution, modelObjectResolver);
    }
}
