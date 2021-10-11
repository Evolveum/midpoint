/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.helpers;

import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.impl.scripting.ActionExecutor;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * @author mederly
 */
@Component
public class OperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(OperationsHelper.class);

    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private PrismContext prismContext;

    public Collection<ObjectDeltaOperation<? extends ObjectType>> applyDelta(ObjectDelta delta, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        return applyDelta(delta, null, context, result);
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> applyDelta(ObjectDelta delta, ModelExecuteOptions options, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        try {
            return modelService.executeChanges(Collections.singleton(delta), options, context.getTask(), result);
        } catch (ObjectAlreadyExistsException|ObjectNotFoundException|SchemaException|ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException|SecurityViolationException e) {
            throw new ScriptExecutionException("Couldn't modify object: " + e.getMessage(), e);
        }
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> applyDelta(ObjectDelta<? extends ObjectType> delta, ModelExecuteOptions options, boolean dryRun, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        try {
            if (dryRun) {
                modelInteractionService.previewChanges(Collections.singleton(delta), options, context.getTask(), result);
                return null;
            } else {
                return modelService.executeChanges(Collections.singleton(delta), options, context.getTask(), result);
            }
        } catch (ObjectAlreadyExistsException|ObjectNotFoundException|SchemaException|ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException|SecurityViolationException e) {
            throw new ScriptExecutionException("Couldn't modify object: " + e.getMessage(), e);
        }
    }

    public Collection<SelectorOptions<GetOperationOptions>> createGetOptions(SelectorQualifiedGetOptionsType optionsBean, boolean noFetch) {
        LOGGER.trace("optionsBean = {}, noFetch = {}", optionsBean, noFetch);
        Collection<SelectorOptions<GetOperationOptions>> rv = MiscSchemaUtil.optionsTypeToOptions(optionsBean, prismContext);
        if (noFetch) {
            if (rv == null) {
                return SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
            GetOperationOptions root = SelectorOptions.findRootOptions(rv);
            if (root != null) {
                root.setNoFetch(true);
            } else {
                rv.add(SelectorOptions.create(GetOperationOptions.createNoFetch()));
            }
        }
        return rv;
    }

    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, boolean noFetch, ExecutionContext context, OperationResult result) throws ScriptExecutionException, ExpressionEvaluationException {
        try {
            return modelService.getObject(type, oid, createGetOptions(null, noFetch), context.getTask(), result);
        } catch (ConfigurationException|ObjectNotFoundException|SchemaException|CommunicationException|SecurityViolationException e) {
            throw new ScriptExecutionException("Couldn't get object: " + e.getMessage(), e);
        }
    }

    public ModelExecuteOptions createExecutionOptions(boolean raw) {
        ModelExecuteOptions options = new ModelExecuteOptions();
        options.setRaw(raw);
        return options;
    }

    public long recordStart(ExecutionContext context, ObjectType objectType) {
        long started = System.currentTimeMillis();
        if (context.isRecordProgressAndIterationStatistics()) {
            if (context.getTask() != null && objectType != null) {
                context.getTask().recordIterativeOperationStart(PolyString.getOrig(objectType.getName()),
                        StatisticsUtil.getDisplayName(objectType.asPrismObject()),
                        StatisticsUtil.getObjectType(objectType, prismContext),
                        objectType.getOid());
            } else {
                LOGGER.warn("Couldn't record operation start in script execution; task = {}, objectType = {}",
                        context.getTask(), objectType);
            }
        }
        return started;
    }

    public void recordEnd(ExecutionContext context, ObjectType objectType, long started, Throwable ex) {
        if (context.isRecordProgressAndIterationStatistics()) {
            Task task = context.getTask();
            if (task != null && objectType != null) {
                task.recordIterativeOperationEnd(
                        PolyString.getOrig(objectType.getName()),
                        StatisticsUtil.getDisplayName(objectType.asPrismObject()),
                        StatisticsUtil.getObjectType(objectType, prismContext),
                        objectType.getOid(),
                        started, ex);
            } else {
                LOGGER.warn("Couldn't record operation end in script execution; task = {}, objectType = {}",
                        task, objectType);
            }
            if (task != null) {
                if (task instanceof RunningTask) {
                    ((RunningTask) task).incrementProgressAndStoreStatsIfNeeded();
                } else {
                    task.setProgress(task.getProgress() + 1);
                }
            }
        }
    }

    public OperationResult createActionResult(PipelineItem item, ActionExecutor executor, ExecutionContext context,
            OperationResult globalResult) {
        OperationResult result = new OperationResult(executor.getClass().getName() + "." + "execute");
        if (item != null) {
            result.addParam("value", String.valueOf(item.getValue()));
            item.getResult().addSubresult(result);
        }
        return result;
    }

    public void trimAndCloneResult(OperationResult result, OperationResult globalResult,
            ExecutionContext context) {
        result.computeStatusIfUnknown();
        // TODO make this configurable
        result.getSubresults().forEach(s -> s.setMinor());
        result.cleanupResult();
        globalResult.addSubresult(result.clone());
    }
}
