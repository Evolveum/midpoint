/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import static com.evolveum.midpoint.schema.util.ExecuteScriptUtil.createInput;
import static com.evolveum.midpoint.schema.util.ExecuteScriptUtil.implantInput;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;

/**
 * Creates task with script execution request having input consisting of object references derived
 * from <object> specification.
 */
class SingleRunTaskCreator extends AbstractSingleRunTaskCreator {

    SingleRunTaskCreator(@NotNull ActionContext actx) {
        super(actx);
    }

    @Override
    @NotNull
    TaskType createTask(ExecuteScriptType executeScript, OperationResult result) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ExecuteScriptType executeScriptWithInput = addInputToScript(executeScript, result);
        return createTaskForSingleRunScript(executeScriptWithInput, result);
    }

    private ExecuteScriptType addInputToScript(ExecuteScriptType executeScript, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (executeScript.getInput() != null) {
            throw new UnsupportedOperationException("Explicit input with SINGLE_RUN task execution is not supported.");
        }
        ReferenceBasedObjectSet objectSet = new ReferenceBasedObjectSet(actx, result);
        objectSet.collect();
        ValueListType input = createInput(objectSet.asReferenceValues());
        return implantInput(executeScript, input);
    }
}
