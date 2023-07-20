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
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AbstractExecutionActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

@Component
public class OperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(OperationsHelper.class);

    private static final String PARAM_RAW = "raw";
    private static final String PARAM_DRY_RUN = "dryRun";
    private static final String PARAM_SKIP_APPROVALS = "skipApprovals";
    private static final String PARAM_OPTIONS = "options";

    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionHelper expressionHelper;

    public boolean getDryRun(ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult result)
            throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return expressionHelper.getActionArgument(Boolean.class, action,
                AbstractExecutionActionExpressionType.F_DRY_RUN, PARAM_DRY_RUN,
                input, context, false, PARAM_DRY_RUN, result);
    }

    @NotNull
    public ModelExecuteOptions getOptions(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult result) throws ScriptExecutionException, SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        ModelExecuteOptions options = getRawOptions(action, input, context, result);

        // raw and skipApprovals are not part of static schema
        Boolean raw = expressionHelper.getArgumentAsBoolean(action.getParameter(), PARAM_RAW, input, context, null, PARAM_RAW, result);
        Boolean skipApprovals = expressionHelper.getArgumentAsBoolean(action.getParameter(), PARAM_SKIP_APPROVALS, input, context, null, PARAM_SKIP_APPROVALS, result);

        if (Boolean.TRUE.equals(raw)) {
            options.raw(true);
        }
        if (Boolean.TRUE.equals(skipApprovals)) {
            if (options.getPartialProcessing() != null) {
                options.getPartialProcessing().setApprovals(SKIP);
            } else {
                options.partialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
            }
        }
        return options;
    }

    @NotNull
    private ModelExecuteOptions getRawOptions(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult result) throws ScriptExecutionException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ModelExecuteOptionsType optionsBean = expressionHelper.getActionArgument(ModelExecuteOptionsType.class, action,
                AbstractExecutionActionExpressionType.F_EXECUTE_OPTIONS, PARAM_OPTIONS, input, context, null,
                "executeOptions", result);
        if (optionsBean != null) {
            return ModelExecuteOptions.fromModelExecutionOptionsType(optionsBean);
        } else {
            return ModelExecuteOptions.create();
        }
    }


    public Collection<ObjectDeltaOperation<? extends ObjectType>> applyDelta(ObjectDelta delta, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        return applyDelta(delta, null, context, result);
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> applyDelta(ObjectDelta delta, ModelExecuteOptions options, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        try {
            //noinspection unchecked
            return modelService.executeChanges(Collections.singleton(delta), options, context.getTask(), result);
        } catch (ObjectAlreadyExistsException|ObjectNotFoundException|SchemaException|ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException|SecurityViolationException e) {
            throw new ScriptExecutionException("Couldn't modify object: " + e.getMessage(), e);
        }
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> applyDelta(ObjectDelta<? extends ObjectType> delta, ModelExecuteOptions options, boolean dryRun, ExecutionContext context, OperationResult result) throws ScriptExecutionException {
        try {
            LOGGER.debug("Going to execute delta (raw={}):\n{}", dryRun, delta.debugDumpLazily());
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
        Collection<SelectorOptions<GetOperationOptions>> rv = GetOperationOptionsUtil.optionsBeanToOptions(optionsBean);
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
            return modelService.getObject(type, oid, createGetOptions(null, noFetch), context.getTask(), result); // TODO readOnly?
        } catch (ConfigurationException|ObjectNotFoundException|SchemaException|CommunicationException|SecurityViolationException e) {
            throw new ScriptExecutionException("Couldn't get object: " + e.getMessage(), e);
        }
    }

    public Operation recordStart(ExecutionContext context, ObjectType object) {
        if (context.isRecordProgressAndIterationStatistics()) {
            Task task = context.getTask();
            if (task != null && object != null) {
                return task.recordIterativeOperationStart(object.asPrismObject());
            } else {
                LOGGER.warn("Couldn't record operation start in script execution; task = {}, object = {}", task, object);
                return null;
            }
        }
        return null;
    }

    public void recordEnd(ExecutionContext context, Operation op, Throwable ex, OperationResult result) {
        Task task = context.getTask();
        if (task == null || !context.isRecordProgressAndIterationStatistics()) {
            return;
        }

        if (op != null) {
            if (ex != null) {
                op.failed(ex);
            } else {
                op.succeeded();
            }
        }
        if (task instanceof RunningTask) {
            try {
                ((RunningTask) task).incrementLegacyProgressAndStoreStatisticsIfTimePassed(result);
            } catch (SchemaException | ObjectNotFoundException e) {
                throw new SystemException("Unexpected exception when recording"
                        + " progress/statistics into the task: " + e.getMessage(), e);
            }
        } else {
            task.setLegacyProgress(task.getLegacyProgress() + 1);
        }
    }

    public OperationResult createActionResult(PipelineItem item, ActionExecutor executor, OperationResult globalResult) {
        OperationResult result = globalResult.createMinorSubresult(executor.getClass().getName() + "." + "execute");
        if (item != null) {
            result.addParam("value", String.valueOf(item.getValue()));
        }
        return result;
    }

    public void trimAndCloneResult(OperationResult result, OperationResult itemResultParent) {
        result.computeStatusIfUnknown();
        // TODO make this configurable
        result.getSubresults().forEach(OperationResult::setMinor);
        result.cleanup();
        if (itemResultParent != null) {
            itemResultParent.addSubresult(result.clone());
        }
    }
}
