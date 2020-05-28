/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

/**
 * Creates "single run" scripting task.
 */
abstract class AbstractSingleRunTaskCreator extends ScriptingTaskCreator {

    AbstractSingleRunTaskCreator(@NotNull ActionContext actx) {
        super(actx);
    }

    @NotNull
    TaskType createTaskForSingleRunScript(ExecuteScriptType executeScript, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        TaskType newTask = createArchetypedTask(result);
        setScriptInTask(newTask, executeScript);
        return newTask;
    }

    @NotNull
    private TaskType createArchetypedTask(OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        TaskType newTask = createEmptyTask(result);
        newTask.setHandlerUri(ModelPublicConstants.SCRIPT_EXECUTION_TASK_HANDLER_URI);

        // TODO task customizer
        // TODO task archetype

        return newTask;
    }

}
