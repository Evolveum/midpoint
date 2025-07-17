/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSetTaskWorkDefinition;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.buckets.ItemDefinitionProvider;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Abstract class for runner of tasks based on import  (import and shadow reclassification).
 */
public abstract class AbstractImportActivityRun<WD extends ResourceSetTaskWorkDefinition, AH extends ModelActivityHandler<WD, AH>>
        extends SearchBasedActivityRun<ShadowType, WD, AH, AbstractActivityWorkStateType> {

    /** Objects to be processed (resource, OC, kind, intent). */
    private ProcessingScope processingScope;

    /** Executor that process the resource objects. */
    private Synchronizer synchronizer;

    protected AbstractImportActivityRun(ActivityRunInstantiationContext<WD, AH> context, @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .synchronizationStatisticsSupported(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }
        ResourceObjectSetType resourceObjectSet = getWorkDefinition().getResourceObjectSetSpecification();

        processingScope = getModelBeans().syncTaskHelper
                .getProcessingScopeCheckingMaintenance(resourceObjectSet, getRunningTask(), result);
        synchronizer = createSynchronizer();
        return true;
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
                getSourceChannel(),
                true);
    }

    protected abstract QName getSourceChannel();

    @Override
    public void customizeSearchOptions(SearchSpecification<ShadowType> searchSpecification, OperationResult result) {
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = SchemaService.get().getOperationOptionsBuilder()
                .doNotDiscovery(false)
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();

        searchSpecification.setSearchOptions(mergeSearchOptions(defaultOptions, searchSpecification.getSearchOptions()));
    }

    protected Collection<SelectorOptions<GetOperationOptions>> mergeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> defaultOptions,
            Collection<SelectorOptions<GetOperationOptions>> searchOptions) {

        // It is questionable if "do not discovery" and "error reporting" can be overridden from the task
        // or not. Let us assume reasonable administrators and allow the overriding. Otherwise, we would swap the arguments below.
        return GetOperationOptions.merge(defaultOptions, searchOptions);
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
}
