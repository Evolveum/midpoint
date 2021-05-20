/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.task.handlers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext;
import com.evolveum.midpoint.repo.common.task.definition.CompositeWorkDefinition;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;

import com.evolveum.midpoint.repo.common.task.execution.PureCompositeActivityExecution;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
@Component
public class PureCompositeActivityHandler implements ActivityHandler<CompositeWorkDefinition> {

    @Autowired ActivityHandlerRegistry handlerRegistry;

    @PostConstruct
    public void register() {
        handlerRegistry.registerHandler(CompositeWorkDefinition.class, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregisterHandler(CompositeWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityExecution createExecution(@NotNull ActivityInstantiationContext<CompositeWorkDefinition> context,
            @NotNull OperationResult result) {
        return new PureCompositeActivityExecution(context, this);
    }
}
