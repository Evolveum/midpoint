/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.sync.tasks.Synchronizer;
import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeItemProcessor;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessorClass;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Searches for resource objects and imports them. (By delegating to Synchronizer class.)
 */
@ItemProcessorClass(ImportFromResourceTaskPartExecution.ItemProcessor.class)
@HandledObjectType(ShadowType.class)
public class ImportFromResourceTaskPartExecution
        extends AbstractIterativeModelTaskPartExecution
        <ShadowType,
                ImportFromResourceTaskHandler,
                ImportFromResourceTaskExecution,
                ImportFromResourceTaskPartExecution,
                ImportFromResourceTaskPartExecution.ItemProcessor> {

    private final Synchronizer synchronizer;

    public ImportFromResourceTaskPartExecution(ImportFromResourceTaskExecution taskExecution) {
        super(taskExecution);
        setContextDescription("from " + taskExecution.getTargetInfo().getContextDescription());
        synchronizer = createSynchronizer();
    }

    private Synchronizer createSynchronizer() {
        SyncTaskHelper.TargetInfo targetInfo = taskExecution.getTargetInfo();
        return new Synchronizer(
                targetInfo.getResource(),
                targetInfo.getObjectClassDefinition(),
                taskExecution.getObjectsFilter(),
                taskHandler.getObjectChangeListener(),
                taskHandler.getTaskTypeName(),
                SchemaConstants.CHANNEL_IMPORT,
                taskExecution.partDefinition,
                true);
    }

//    private SynchronizeAccountResultHandler createHandler(@NotNull ResourceType resource,
//            @Nullable PrismObject<ShadowType> shadowToImport, RunningTask coordinatorTask, TaskPartitionDefinitionType partition,
//            TaskRunResult runResult, OperationResult opResult) {
//        SynchronizeAccountResultHandler handler = new SynchronizeAccountResultHandler(resource, objectClass, objectsFilter, "import",
//                coordinatorTask, changeNotificationDispatcher, partition, taskManager);
//        handler.setSourceChannel(SchemaConstants.CHANNEL_IMPORT);
//        handler.setForceAdd(true);
//        handler.setStopOnError(false);
//        handler.setContextDesc("from "+resource);
//        handler.setLogObjectProgress(true);
//
//        return handler;
//    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> createSearchOptions(
            OperationResult opResult) {
        Collection<SelectorOptions<GetOperationOptions>> defaultOptions = getSchemaHelper().getOperationOptionsBuilder()
                .doNotDiscovery(false)
                .errorReportingMethod(FetchErrorReportingMethodType.FETCH_RESULT)
                .build();
        Collection<SelectorOptions<GetOperationOptions>> configuredOptions =
                super.createSearchOptions(opResult);

        // It is questionable if "do not discovery" and "error reporting" can be overridden from the task
        // or not. Let us assume reasonable administrators and allow the overriding. Otherwise we would swap the arguments below.
        return GetOperationOptions.merge(getPrismContext(), defaultOptions, configuredOptions);
    }

    @Override
    protected ObjectQuery createQuery(OperationResult opResult) throws SchemaException {
        ObjectQuery query = createQueryFromTaskIfExists();
        if (query != null) {
            return query;
        } else {
            return ObjectQueryUtil.createResourceAndObjectClassQuery(taskExecution.getResourceOid(),
                    taskExecution.getObjectClassName(), getPrismContext());
        }
    }

    @Override
    protected Function<ItemPath, ItemDefinition<?>> createItemDefinitionProvider() {
        return createItemDefinitionProviderForAttributes(taskExecution.getTargetInfo().getObjectClassDefinition());
    }

    public class ItemProcessor
            extends AbstractSearchIterativeItemProcessor
            <ShadowType,
                    ImportFromResourceTaskHandler,
                    ImportFromResourceTaskExecution,
                    ImportFromResourceTaskPartExecution,
                    ItemProcessor> {

        public ItemProcessor(ImportFromResourceTaskPartExecution taskExecution) {
            super(taskExecution);
        }

        @Override
        protected boolean processObject(PrismObject<ShadowType> object, RunningTask workerTask,
                OperationResult result) throws CommonException, PreconditionViolationException {
            synchronizer.handleObject(object, workerTask, result);
            return true;
        }
    }
}
