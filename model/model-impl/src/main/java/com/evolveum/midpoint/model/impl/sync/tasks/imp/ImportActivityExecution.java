/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import java.util.Collection;

import com.evolveum.midpoint.repo.common.task.ObjectSearchBasedActivityExecution;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.model.impl.sync.tasks.SynchronizationObjectsFilterImpl;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.work.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ImportActivityExecution
        extends ObjectSearchBasedActivityExecution<ShadowType, ImportWorkDefinition, ImportActivityHandler, AbstractActivityWorkStateType> {

    private ResourceObjectClassSpecification resourceObjectClassSpecification;
    private SynchronizationObjectsFilterImpl objectsFilter;
    private Synchronizer synchronizer;

    ImportActivityExecution(ExecutionInstantiationContext<ImportWorkDefinition, ImportActivityHandler> context) {
        super(context, "Import");
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(true);
    }

    @Override
    public void beforeExecution(OperationResult result) throws ActivityExecutionException, CommonException {
        ResourceObjectSetType resourceObjectSet = getWorkDefinition().getResourceObjectSetSpecification();

        resourceObjectClassSpecification = getModelBeans().syncTaskHelper
                .createObjectClassSpec(resourceObjectSet, getRunningTask(), result);
        objectsFilter = this.resourceObjectClassSpecification.getObjectFilter(resourceObjectSet);
        synchronizer = createSynchronizer();

        resourceObjectClassSpecification.checkNotInMaintenance();
    }

    private Synchronizer createSynchronizer() {
        return new Synchronizer(
                resourceObjectClassSpecification.getResource(),
                resourceObjectClassSpecification.getObjectClassDefinitionRequired(),
                objectsFilter,
                getModelBeans().eventDispatcher,
                SchemaConstants.CHANNEL_IMPORT,
                isPreview(),
                true);
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result) {
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = SchemaService.get().getOperationOptionsBuilder()
                .doNotDiscovery(false)
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();

        // It is questionable if "do not discovery" and "error reporting" can be overridden from the task
        // or not. Let us assume reasonable administrators and allow the overriding. Otherwise, we would swap the arguments below.
        return GetOperationOptions.merge(PrismContext.get(), defaultOptions, configuredOptions);
    }

    @Override
    public ItemDefinitionProvider createItemDefinitionProvider() {
        return ItemDefinitionProvider.forObjectClassAttributes(resourceObjectClassSpecification.getObjectClassDefinition());
    }

    @Override
    public boolean processObject(@NotNull PrismObject<ShadowType> object,
            @NotNull ItemProcessingRequest<PrismObject<ShadowType>> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        synchronizer.synchronize(object, request.getIdentifier(), workerTask, result);
        return true;
    }

    private @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }
}
