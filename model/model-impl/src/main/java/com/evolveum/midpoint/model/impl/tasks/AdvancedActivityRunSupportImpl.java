/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.tasks.sources.ModelAuditItemSource;
import com.evolveum.midpoint.model.impl.tasks.sources.ModelContainerableItemSource;
import com.evolveum.midpoint.model.impl.tasks.sources.ModelObjectSource;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationImpl;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemPreprocessor;
import com.evolveum.midpoint.repo.common.activity.run.sources.SearchableItemSource;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;

import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.ObjectProcessingListener;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;

@Component
public class AdvancedActivityRunSupportImpl implements AdvancedActivityRunSupport {

    @Autowired private SyncTaskHelper syncTaskHelper;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private ModelObjectResolver modelObjectResolver;
    @Autowired private ModelObjectSource modelObjectSource;
    @Autowired private ModelAuditItemSource modelAuditItemSource;
    @Autowired private ModelContainerableItemSource modelContainerableItemSource;
    @Autowired private SimulationResultManager simulationResultManager;

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public @NotNull SearchSpecification<?> createSearchSpecificationFromResourceObjectSetSpec(
            @NotNull ResourceObjectSetSpecificationImpl objectSetSpecification, @NotNull RunningTask task, OperationResult result)
            throws SchemaException, ActivityRunException {
        return syncTaskHelper.createSearchSpecification(
                objectSetSpecification.getResourceObjectSetBean(),
                task, result);
    }

    @Override
    public ObjectQuery evaluateQueryExpressions(@NotNull ObjectQuery query, ExpressionProfile expressionProfile,
            @NotNull RunningTask task, OperationResult result)
            throws CommonException {
        PrismObject<SystemConfigurationType> configuration = systemObjectCache.getSystemConfiguration(result);
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(null, null, null, asObjectable(configuration));
        try {
            ModelExpressionEnvironment<?,?,?> env = new ModelExpressionEnvironment<>(task, result);
            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(env);
            return ExpressionUtil.evaluateQueryExpressions(query, variables, expressionProfile,
                    expressionFactory, prismContext, "evaluate query expressions",
                    task, result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    @Override
    public void applyDefinitionsToQuery(@NotNull SearchSpecification<?> searchSpecification, @NotNull Task task,
            OperationResult result) throws CommonException {
        Class<? extends Containerable> itemType = searchSpecification.getType();
        if (!itemType.isAssignableFrom(ObjectType.class)) {
            return;
        }

        //noinspection unchecked
        Class<? extends ObjectType> objectType = (Class<? extends ObjectType>) itemType;
        if (searchSpecification.isUseRepository() || !ObjectTypes.isClassManagedByProvisioning(objectType)) {
            return;
        }

        provisioningService
                .applyDefinition(objectType, searchSpecification.getQuery(), task, result);
    }

    @Override
    public void checkRawAuthorization(Task task, OperationResult result) throws CommonException {
        securityEnforcer.authorize(ModelAuthorizationAction.RAW_OPERATION.getUrl(), null,
                AuthorizationParameters.EMPTY, null, task, result);
    }

    @Override
    public ItemPreprocessor<ShadowType> createShadowFetchingPreprocessor(
            @NotNull Producer<Collection<SelectorOptions<GetOperationOptions>>> producerOptions,
            @NotNull SchemaService schemaService) {
        return new ShadowFetchingPreprocessor(producerOptions, schemaService, modelObjectResolver);
    }

    @Override
    public <C extends Containerable> SearchableItemSource getItemSourceFor(Class<C> type) {
        if (MiscSchemaUtil.isObjectType(type)) {
            return modelObjectSource;
        } else if (MiscSchemaUtil.isAuditType(type)) {
            return modelAuditItemSource;
        } else {
            return modelContainerableItemSource;
        }
    }

    @Override
    public @NotNull ObjectReferenceType createSimulationResult(
            @Nullable SimulationDefinitionType definition, OperationResult result)
            throws ConfigurationException {
        return simulationResultManager
                .newSimulationResult(definition, result)
                .getResultRef();
    }

    @Override
    public @NotNull ObjectProcessingListener getObjectProcessingListener(
            @NotNull ObjectReferenceType simulationResultRef) {
        return simulationResultManager
                .newSimulationContext(Objects.requireNonNull(simulationResultRef.getOid(), "no result OID"))
                .objectProcessingListener();
    }

    @Override
    public void closeSimulationResult(@NotNull ObjectReferenceType simulationResultRef, OperationResult result)
            throws ObjectNotFoundException {
        simulationResultManager
                .closeSimulationResult(simulationResultRef, result);
    }
}
