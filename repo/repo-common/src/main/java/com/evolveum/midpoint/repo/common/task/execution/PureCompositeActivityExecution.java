/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.execution;

import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext.ComponentActivityInstantiationContext;

import com.evolveum.midpoint.repo.common.task.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.task.definition.CompositeWorkDefinition;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.task.handlers.PureCompositeActivityHandler;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.List;
import java.util.stream.Collectors;

public class PureCompositeActivityExecution
        extends AbstractCompositeActivityExecution<CompositeWorkDefinition, PureCompositeActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(PureCompositeActivityExecution.class);

    public PureCompositeActivityExecution(ActivityInstantiationContext<CompositeWorkDefinition> context,
            PureCompositeActivityHandler activityHandler) {
        super(context, activityHandler);
    }

    @NotNull
    @Override
    protected List<ActivityExecution> createChildren(OperationResult result) throws SchemaException {
        return activityDefinition.getWorkDefinition().createChildDefinitions().stream()
                .map(childDefinition -> createChildExecution(childDefinition, result))
                .collect(Collectors.toList());
    }

    @NotNull
    private <WD extends WorkDefinition> ActivityExecution createChildExecution(ActivityDefinition<WD> childDefinition,
            OperationResult result) {
        ActivityExecution childExecution = getBeans().activityHandlerRegistry
                .getHandler(childDefinition)
                .createExecution(new ComponentActivityInstantiationContext<>(childDefinition, this), result);
        LOGGER.trace("Created execution {} for child definition {} to {}", childExecution, childDefinition, this);
        return childExecution;
    }
}
