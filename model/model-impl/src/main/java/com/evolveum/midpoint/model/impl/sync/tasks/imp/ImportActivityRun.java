/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import java.util.Collection;

import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;

public final class ImportActivityRun
        extends SearchBasedActivityRun<ShadowType, ImportWorkDefinition, ImportActivityHandler, AbstractActivityWorkStateType> {

    /** Objects to be imported (resource, OC, kind, intent). */
    private ProcessingScope processingScope;

    /** Executor that imports the resource objects. */
    private Synchronizer synchronizer;

    ImportActivityRun(ActivityRunInstantiationContext<ImportWorkDefinition, ImportActivityHandler> context) {
        super(context, "Import");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .synchronizationStatisticsSupported(true);
    }

    @Override
    public void beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        ResourceObjectSetType resourceObjectSet = getWorkDefinition().getResourceObjectSetSpecification();

        processingScope = getModelBeans().syncTaskHelper
                .getProcessingScopeCheckingMaintenance(resourceObjectSet, getRunningTask(), result);
        synchronizer = createSynchronizer();
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return processingScope.getResourceRef();
    }

    private Synchronizer createSynchronizer() {
        return new Synchronizer(
                processingScope.getResource(),
                processingScope.getPostSearchFilter(),
                getModelBeans().eventDispatcher,
                SchemaConstants.CHANNEL_IMPORT,
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
        return processingScope.createItemDefinitionProvider();
    }

    @Override
    public boolean processItem(@NotNull ShadowType object,
            @NotNull ItemProcessingRequest<ShadowType> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityRunException {
        synchronizer.synchronize(object.asPrismObject(), request.getIdentifier(), workerTask, result);
        return true;
    }

    private @NotNull ModelBeans getModelBeans() {
        return getActivityHandler().getModelBeans();
    }

    @Override
    protected String getChannelOverride() {
        return SchemaConstants.CHANNEL_IMPORT_URI;
    }
}
