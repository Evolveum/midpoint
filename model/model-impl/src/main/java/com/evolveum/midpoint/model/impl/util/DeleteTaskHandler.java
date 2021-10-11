/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author semancik
 *
 */
@Component
public class DeleteTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = ModelPublicConstants.DELETE_TASK_HANDLER_URI;

    @Autowired protected TaskManager taskManager;
    @Autowired protected ModelService modelService;
    @Autowired protected PrismContext prismContext;

    private static final Trace LOGGER = TraceManager.getTrace(DeleteTaskHandler.class);

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromZero()
                .maintainIterationStatistics()
                .maintainActionsExecutedStatistics();
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        return runInternal(task);
    }

    private <O extends ObjectType> TaskRunResult runInternal(RunningTask task) {
        LOGGER.trace("Delete task run starting ({})", task);
        long startTimestamp = System.currentTimeMillis();

        OperationResult opResult = new OperationResult("DeleteTask.run");
        opResult.setStatus(OperationResultStatus.IN_PROGRESS);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        opResult.setSummarizeErrors(true);
        opResult.setSummarizePartialErrors(true);
        opResult.setSummarizeSuccesses(true);

        QueryType queryType;
        PrismProperty<QueryType> objectQueryPrismProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        if (objectQueryPrismProperty != null && objectQueryPrismProperty.getRealValue() != null) {
            queryType = objectQueryPrismProperty.getRealValue();
        } else {
            // For "foolproofness" reasons we really require a query. Even if it is "ALL" query.
            LOGGER.error("No query parameter in {}", task);
            opResult.recordFatalError("No query parameter in " + task);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        Class<O> objectType;
        QName objectTypeName;
        PrismProperty<QName> objectTypePrismProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        if (objectTypePrismProperty != null && objectTypePrismProperty.getRealValue() != null) {
            objectTypeName = objectTypePrismProperty.getRealValue();
            //noinspection unchecked
            objectType = (Class<O>) ObjectTypes.getObjectTypeFromTypeQName(objectTypeName).getClassDefinition();
        } else {
            LOGGER.error("No object type parameter in {}", task);
            opResult.recordFatalError("No object type parameter in " + task);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        ObjectQuery query;
        try {
             query = prismContext.getQueryConverter().createObjectQuery(objectType, queryType);
             if (LOGGER.isTraceEnabled()) {
                 LOGGER.trace("Using object query from the task: {}", query.debugDump());
             }
        } catch (SchemaException ex) {
            LOGGER.error("Schema error while creating a search filter: {}-{}", new Object[]{ex.getMessage(), ex});
            opResult.recordFatalError("Schema error while creating a search filter: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        PrismProperty<Boolean> optionRawPrismProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OPTION_RAW);
        boolean optionRaw = optionRawPrismProperty == null || optionRawPrismProperty.getRealValue() == null || optionRawPrismProperty.getRealValue();

        LOGGER.trace("Deleting {}, raw={} using query:\n{}", objectType.getSimpleName(), optionRaw, query.debugDumpLazily());

        boolean countObjectsOnStart = true; // TODO

        Integer maxSize = 100;
        ObjectPaging paging = prismContext.queryFactory().createPaging(0, maxSize);
        query.setPaging(paging);
        query.setAllowPartialResults(true);

        Collection<SelectorOptions<GetOperationOptions>> searchOptions;
        ModelExecuteOptions execOptions;
        if (optionRaw) {
            searchOptions = SelectorOptions.createCollection(GetOperationOptions.createRaw());
            execOptions = ModelExecuteOptions.createRaw();
        } else {
            searchOptions = null;
            execOptions = null;
        }

        try {

            // counting objects can be within try-catch block, because the handling is similar to handling errors within searchIterative

            //noinspection ConstantConditions
            if (countObjectsOnStart) {
                Integer expectedTotal = modelService.countObjects(objectType, query, searchOptions, task, opResult);
                LOGGER.trace("Expecting {} objects to be deleted", expectedTotal);
                if (expectedTotal != null) {
                    task.setExpectedTotalImmediate((long) expectedTotal, opResult);
                }
            }

            while (task.canRun()) {
                SearchResultList<PrismObject<O>> objects = modelService.searchObjects(objectType, query, searchOptions, task, opResult);
                if (objects.isEmpty()) {
                    break;
                }

                int skipped = 0;
                for (PrismObject<O> object: objects) {
                    if (!task.canRun()) {
                        break;
                    }
                    if (!optionRaw && ShadowType.class.isAssignableFrom(objectType)
                            && isTrue(((ShadowType)(object.asObjectable())).isProtectedObject())) {
                        LOGGER.debug("Skipping delete of protected object {}", object);
                        skipped++;
                        continue;
                    }

                    ObjectDelta<?> delta = prismContext.deltaFactory().object().createDeleteDelta(objectType, object.getOid());

                    String objectName = PolyString.getOrig(object.getName());
                    String objectDisplayName = StatisticsUtil.getDisplayName(object);
                    String objectOid = object.getOid();

                    task.recordIterativeOperationStart(objectName, objectDisplayName, objectTypeName, objectOid);
                    long objectDeletionStarted = System.currentTimeMillis();
                    try {
                        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), execOptions, task, opResult);
                        task.recordIterativeOperationEnd(objectName, objectDisplayName, objectTypeName, objectOid, objectDeletionStarted, null);
                    } catch (Throwable t) {
                        task.recordIterativeOperationEnd(objectName, objectDisplayName, objectTypeName, objectOid, objectDeletionStarted, t);
                        throw t;        // TODO we don't want to continue processing if an error occurs?
                    }
                    task.incrementProgressAndStoreStatsIfNeeded();
                }

                opResult.summarize();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Search returned {} objects, {} skipped, progress: {} (interrupted: {}), result:\n{}",
                            objects.size(), skipped, task.getProgress(), !task.canRun(), opResult.debugDump());
                }

                if (objects.size() == skipped) {
                    break;
                }
            }

        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
                | ExpressionEvaluationException | ConfigurationException
                | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("{}", e.getMessage(), e);
            opResult.recordFatalError("Object not found " + e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        } catch (CommunicationException e) {
            LOGGER.error("{}-{}", e.getMessage(), e);
            opResult.recordFatalError("Object not found " + e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResultStatus.TEMPORARY_ERROR);
            return runResult;
        }

        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        opResult.summarize();
        opResult.recordSuccess();

        long wallTime = System.currentTimeMillis() - startTimestamp;

        String finishMessage = "Finished delete (" + task + "). ";
        String statistics = "Processed " + task.getProgress() + " objects in " + wallTime/1000 + " seconds.";
        if (task.getProgress() > 0) {
            statistics += " Wall clock time average: " + ((float) wallTime / (float) task.getProgress()) + " milliseconds";
        }
        if (!task.canRun()) {
            statistics += " (task run was interrupted)";
        }

        opResult.createSubresult(DeleteTaskHandler.class.getName() + ".statistics").recordStatus(OperationResultStatus.SUCCESS, statistics);

        LOGGER.info(finishMessage + statistics);
        LOGGER.trace("Run finished (task {}, run result {}); interrupted = {}", task, runResult, !task.canRun());

        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return task.getProgress();
    }

    @Override
    public void refreshStatus(Task task) {
        // Local task. No refresh needed. The Task instance has always fresh data.
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
