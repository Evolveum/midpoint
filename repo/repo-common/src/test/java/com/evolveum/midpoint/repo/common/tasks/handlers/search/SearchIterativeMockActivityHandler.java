/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.search;

import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;
import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext;
import com.evolveum.midpoint.repo.common.task.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.repo.common.tasks.handlers.iterative.IterativeMockWorkDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * TODO
 */
@Component
public class SearchIterativeMockActivityHandler implements ActivityHandler<SearchIterativeMockWorkDefinition> {

    @Autowired private ActivityHandlerRegistry handlerRegistry;
    @Autowired private MockRecorder recorder;

    @PostConstruct
    public void register() {
        handlerRegistry.register(SearchIterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, null,
                SearchIterativeMockWorkDefinition.class, SearchIterativeMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(SearchIterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, null,
                SearchIterativeMockWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityExecution createExecution(
            @NotNull ActivityInstantiationContext<SearchIterativeMockWorkDefinition> context, @NotNull OperationResult result) {
        return new SearchIterativeMockActivityExecution(context, this);
    }

    public @NotNull MockRecorder getRecorder() {
        return recorder;
    }
}
