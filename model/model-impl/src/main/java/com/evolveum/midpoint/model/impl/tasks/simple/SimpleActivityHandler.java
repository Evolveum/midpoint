/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks.simple;

import java.util.Collection;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.sync.SynchronizationService;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TODO
 */
@Component
public abstract class SimpleActivityHandler<O extends ObjectType, WD extends WorkDefinition, EC extends ExecutionContext>
        extends ModelActivityHandler<WD, SimpleActivityHandler<O, WD, EC>> {

    @Autowired protected WorkDefinitionFactory workDefinitionFactory;
    @Autowired protected ProvisioningService provisioningService;
    @Autowired protected PrismContext prismContext;
    @Autowired protected ModelController modelController;
    @Autowired protected SynchronizationService synchronizationService;
    @Autowired protected SyncTaskHelper syncTaskHelper;
    @Autowired protected SecurityEnforcer securityEnforcer;
    @Autowired @Qualifier("cacheRepositoryService") protected RepositoryService repositoryService;
    @Autowired protected Clock clock;
    @Autowired protected Clockwork clockwork;
    @Autowired protected ContextFactory contextFactory;
    @Autowired protected ScriptingService scriptingService;

    @PostConstruct
    public void register() {
        handlerRegistry.register(getWorkDefinitionTypeName(), getLegacyHandlerUri(), getWorkDefinitionClass(),
                getWorkDefinitionSupplier(), this, getArchetypeOid());
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(getWorkDefinitionTypeName(), getLegacyHandlerUri(), getWorkDefinitionClass());
    }

    @Override
    public @NotNull SimpleActivityExecution<O, WD, EC> createExecution(
            @NotNull ExecutionInstantiationContext<WD, SimpleActivityHandler<O, WD, EC>> context,
            @NotNull OperationResult result) {
        return new SimpleActivityExecution<>(context, getShortName());
    }

    @Override
    public String getIdentifierPrefix() {
        return "propagation";
    }

    protected abstract @NotNull QName getWorkDefinitionTypeName();

    protected abstract @NotNull Class<WD> getWorkDefinitionClass();

    protected abstract @NotNull WorkDefinitionSupplier getWorkDefinitionSupplier();

    protected abstract @NotNull String getLegacyHandlerUri();

    protected abstract @Nullable String getArchetypeOid();

    protected abstract @NotNull String getShortName();

    public abstract @NotNull ActivityReportingOptions getDefaultReportingOptions();

    public EC createExecutionContext(SimpleActivityExecution<O, WD, EC> activityExecution,
            OperationResult result) throws ActivityExecutionException, CommonException {
        return null;
    }

    public void beforeExecution(@NotNull SimpleActivityExecution<O, WD, EC> activityExecution, OperationResult opResult)
            throws ActivityExecutionException, CommonException {
    }

    public void afterExecution(@NotNull SimpleActivityExecution<O, WD, EC> activityExecution, OperationResult opResult)
            throws ActivityExecutionException, CommonException {
    }

    public ObjectQuery customizeQuery(@NotNull SimpleActivityExecution<O, WD, EC> activityExecution,
            ObjectQuery configuredQuery, OperationResult opResult) {
        return configuredQuery;
    }

    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            @NotNull SimpleActivityExecution<O, WD, EC> activityExecution,
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult opResult) throws CommonException {
        return configuredOptions;
    }

    public boolean doesRequireDirectRepositoryAccess() {
        return false;
    }

    public abstract boolean processItem(PrismObject<O> object, ItemProcessingRequest<PrismObject<O>> request,
            SimpleActivityExecution<O, WD, EC> activityExecution, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException;
}
