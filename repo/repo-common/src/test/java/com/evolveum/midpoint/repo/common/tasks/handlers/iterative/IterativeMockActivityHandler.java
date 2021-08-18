/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.task.PlainIterativeActivityExecution;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.tasks.handlers.AbstractMockActivityHandler;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
@Component
public class IterativeMockActivityHandler
        extends AbstractMockActivityHandler<IterativeMockWorkDefinition, IterativeMockActivityHandler> {

    @Autowired private ActivityHandlerRegistry handlerRegistry;
    @Autowired private MockRecorder recorder;

    @PostConstruct
    public void register() {
        handlerRegistry.register(IterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, null,
                IterativeMockWorkDefinition.class, IterativeMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(IterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, null,
                IterativeMockWorkDefinition.class);
    }

    @Override
    public @NotNull AbstractActivityExecution<IterativeMockWorkDefinition, IterativeMockActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<IterativeMockWorkDefinition, IterativeMockActivityHandler> context,
            @NotNull OperationResult result) {
        return new PlainIterativeActivityExecution<>(context, "Iterative Mock Activity", IterativeMockActivityExecutionSpecifics::new);
    }

    public @NotNull MockRecorder getRecorder() {
        return recorder;
    }

    @Override
    public String getIdentifierPrefix() {
        return "mock-iterative";
    }
}
