/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Executes specified delta or deltas in background.
 *
 * Currently used to implement "Save in background" functionality, as well
 * as {@link MidpointFunctions#executeChangesAsynchronously(Collection, ModelExecuteOptions, String)} method.
 *
 * Not intended to be used in a recurring way.
 */
@Component
public class ExecuteDeltasTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteDeltasTaskHandler.class);

    private static final String DOT_CLASS = ExecuteDeltasTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;

    @Override
    public TaskRunResult run(@NotNull RunningTask task) {
        OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
        TaskRunResult runResult = new TaskRunResult();

        try {
            Collection<ObjectDelta<? extends ObjectType>> objectDeltas = getDeltas(task);
            ModelExecuteOptions options = getOptions(task);

            modelService.executeChanges(objectDeltas, options, task, result);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        } catch (Throwable t) {
            String message = "An exception occurred when executing changes, in task " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message, t);
            result.recordFatalError(message, t);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
        } finally {
            result.computeStatusIfUnknown();
        }

        task.getResult().recomputeStatus();
        runResult.setOperationResult(task.getResult());
        return runResult;
    }

    @NotNull
    private Collection<ObjectDelta<? extends ObjectType>> getDeltas(RunningTask task) throws SchemaException {
        Collection<ObjectDeltaType> deltas = getRawDeltas(task);

        Collection<ObjectDelta<? extends ObjectType>> objectDeltas = new ArrayList<>();
        for (ObjectDeltaType deltaBean : deltas) {
            objectDeltas.add(DeltaConvertor.createObjectDelta(deltaBean, prismContext));
        }
        return objectDeltas;
    }

    @NotNull
    private Collection<ObjectDeltaType> getRawDeltas(RunningTask task) {
        PrismProperty<ObjectDeltaType> deltasProperty =
                task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTAS);
        if (deltasProperty != null && !deltasProperty.isEmpty()) {
            return deltasProperty.getRealValues();
        }

        PrismProperty<ObjectDeltaType> deltaProperty =
                task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
        if (deltaProperty != null && !deltaProperty.isEmpty()) {
            return deltaProperty.getRealValues();
        }

        throw new IllegalArgumentException("No deltas to execute");
    }

    @Nullable
    private ModelExecuteOptions getOptions(RunningTask task) {
        ModelExecuteOptionsType optionsBean =
                task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
        return optionsBean != null ?
                ModelExecuteOptions.fromModelExecutionOptionsType(optionsBean) : null;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.EXECUTE_DELTAS_TASK_HANDLER_URI, this);
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(); // todo reconsider
    }

    @Override
    public String getDefaultChannel() {
        return null; // The channel URI should be provided by the task creator.
    }
}
