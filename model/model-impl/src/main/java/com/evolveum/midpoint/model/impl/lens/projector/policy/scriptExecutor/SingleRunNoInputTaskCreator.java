/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.scriptExecutor;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

import org.jetbrains.annotations.NotNull;

/**
 * Creates task with script execution request with no specific input added.
 */
class SingleRunNoInputTaskCreator extends AbstractSingleRunTaskCreator {

    SingleRunNoInputTaskCreator(@NotNull ActionContext actx) {
        super(actx);
    }

    @Override
    TaskType createTask(ExecuteScriptType executeScript, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        // Nothing special here. Creates task for the script "as is".
        return createTaskForSingleRunScript(executeScript, result);
    }
}
