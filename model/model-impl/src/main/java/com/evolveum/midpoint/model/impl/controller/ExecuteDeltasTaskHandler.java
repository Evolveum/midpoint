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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Temporary/experimental implementation.
 *
 * @author mederly
 */

@Component
public class ExecuteDeltasTaskHandler implements TaskHandler {

    private static final Trace LOGGER = TraceManager.getTrace(ExecuteDeltasTaskHandler.class);

    private static final String DOT_CLASS = ExecuteDeltasTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        OperationResult result = task.getResult().createSubresult(DOT_CLASS + "run");
        TaskRunResult runResult = new TaskRunResult();

        Collection<ObjectDeltaType> deltas;
        PrismProperty<ObjectDeltaType> deltasProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTAS);
        if (deltasProperty == null || deltasProperty.isEmpty()) {
            PrismProperty<ObjectDeltaType> deltaProperty = task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA);
            if (deltaProperty == null || deltaProperty.isEmpty()) {
                throw new IllegalArgumentException("No deltas to execute");
            } else {
                deltas = deltaProperty.getRealValues();
            }
        } else {
            deltas = deltasProperty.getRealValues();
        }
        ModelExecuteOptionsType optionsBean = task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_EXECUTE_OPTIONS);
        ModelExecuteOptions options = optionsBean != null ?
                ModelExecuteOptions.fromModelExecutionOptionsType(optionsBean) : null;

        try {
            Collection<ObjectDelta<?>> objectDeltas = new ArrayList<>();
            for (ObjectDeltaType deltaBean : deltas) {
                objectDeltas.add(DeltaConvertor.createObjectDelta(deltaBean, prismContext));
            }
            //noinspection unchecked
            modelService.executeChanges((Collection) objectDeltas, options, task, result);
            result.computeStatusIfUnknown();
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.FINISHED);
        } catch (CommonException | RuntimeException e) {
            String message = "An exception occurred when executing changes, in task " + task;
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            result.recordFatalError(message, e);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
        }
        task.getResult().recomputeStatus();
        runResult.setOperationResult(task.getResult());
        return runResult;
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
}
