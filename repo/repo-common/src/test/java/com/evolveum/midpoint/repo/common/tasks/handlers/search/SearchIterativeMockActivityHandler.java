/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.search;

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
public class SearchIterativeMockActivityHandler
        extends AbstractMockActivityHandler<SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler> {

    @Autowired private ActivityHandlerRegistry handlerRegistry;
    @Autowired private MockRecorder recorder;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                SearchIterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME,
                SearchIterativeMockWorkDefinition.WORK_DEFINITION_ITEM_QNAME,
                SearchIterativeMockWorkDefinition.class, SearchIterativeMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                SearchIterativeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME,
                SearchIterativeMockWorkDefinition.class);
    }

    @Override
    public @NotNull AbstractActivityRun<SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler> context,
            @NotNull OperationResult result) {
        return new SearchBasedMockActivityRun(context);
    }

    public @NotNull MockRecorder getRecorder() {
        return recorder;
    }

    @Override
    public String getIdentifierPrefix() {
        return "mock-search-iterative";
    }
}
