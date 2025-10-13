/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.iterative;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
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
        handlerRegistry.register(
                IterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, IterativeMockWorkDefinition.WORK_DEFINITION_ITEM_QNAME,
                IterativeMockWorkDefinition.class, IterativeMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(IterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME,
                IterativeMockWorkDefinition.class);
    }

    @Override
    public @NotNull AbstractActivityRun<IterativeMockWorkDefinition, IterativeMockActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<IterativeMockWorkDefinition, IterativeMockActivityHandler> context,
            @NotNull OperationResult result) {
        return new IterativeMockActivityRun(context);
    }

    public @NotNull MockRecorder getRecorder() {
        return recorder;
    }

    @Override
    public String getIdentifierPrefix() {
        return "mock-iterative";
    }
}
