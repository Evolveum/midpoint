/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import java.util.Collection;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.sync.tasks.SynchronizationObjectsFilterImpl;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.model.impl.sync.tasks.TargetInfo;
import com.evolveum.midpoint.model.impl.tasks.AbstractModelSearchActivityExecution;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ImportActivityExecution
        extends AbstractModelSearchActivityExecution
        <ShadowType,
                        ImportWorkDefinition,
                        ImportActivityHandler,
                        ImportActivityExecution,
                        AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ImportActivityExecution.class);

    private static final String SHORT_NAME = "Import";

    private TargetInfo targetInfo;
    private SynchronizationObjectsFilterImpl objectsFilter;
    private Synchronizer synchronizer;

    ImportActivityExecution(
            @NotNull ExecutionInstantiationContext<ImportWorkDefinition, ImportActivityHandler> context) {
        super(context, SHORT_NAME);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .enableActionsExecutedStatistics(true)
                .enableSynchronizationStatistics(true);
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws ActivityExecutionException, CommonException {
        ResourceObjectSetType resourceObjectSet = getResourceObjectSet();

        targetInfo = getModelBeans().syncTaskHelper
                .createTargetInfo(resourceObjectSet, getRunningTask(), opResult);
        objectsFilter = this.targetInfo.getObjectFilter(resourceObjectSet);
        synchronizer = createSynchronizer();

        targetInfo.checkNotInMaintenance();
    }

    private Synchronizer createSynchronizer() {
        return new Synchronizer(
                targetInfo.getResource(),
                targetInfo.getObjectClassDefinitionRequired(),
                objectsFilter,
                getModelBeans().eventDispatcher,
                SchemaConstants.CHANNEL_IMPORT,
                isSimulate(),
                true);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = getSchemaService().getOperationOptionsBuilder()
                .doNotDiscovery(false)
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();

        // It is questionable if "do not discovery" and "error reporting" can be overridden from the task
        // or not. Let us assume reasonable administrators and allow the overriding. Otherwise we would swap the arguments below.
        return GetOperationOptions.merge(getPrismContext(), defaultOptions, configuredOptions);
    }

    @Override
    protected Function<ItemPath, ItemDefinition<?>> createItemDefinitionProvider() {
        return createItemDefinitionProviderForAttributes(targetInfo.getObjectClassDefinition());
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ShadowType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(
                (object, request, workerTask, result) -> {
                    synchronizer.synchronize(object, request.getIdentifier(), workerTask, result);
                    return true;
                }
        );
    }

    private @NotNull ResourceObjectSetType getResourceObjectSet() {
        return activity.getWorkDefinition().getResourceObjectSetSpecification();
    }
}
