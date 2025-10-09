/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
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

        // @formatter:off
        TaskType newTask = super.createEmptyTask(result)
                .beginAssignment()
                    .targetRef(SystemObjectsType.ARCHETYPE_SINGLE_BULK_ACTION_TASK.value(), ArchetypeType.COMPLEX_TYPE)
                .<TaskType>end()
                .beginActivity()
                    .beginWork()
                        .beginNonIterativeScripting()
                            .scriptExecutionRequest(executeScript)
                        .<WorkDefinitionsType>end()
                    .<ActivityDefinitionType>end()
                .end();
        // @formatter:on

        return customizeTask(newTask, result);
    }
}
