/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.simple;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;

import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;

import com.evolveum.midpoint.repo.common.tasks.handlers.AbstractMockActivityHandler;
import com.evolveum.midpoint.repo.common.tasks.handlers.CommonMockActivityHelper;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;

@Component
public class SimpleMockActivityHandler extends AbstractMockActivityHandler<SimpleMockWorkDefinition, SimpleMockActivityHandler> {

    @Autowired private ActivityHandlerRegistry handlerRegistry;
    @Autowired private CommonMockActivityHelper mockHelper;
    @Autowired private MockRecorder recorder;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                SimpleMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, SimpleMockWorkDefinition.WORK_DEFINITION_ITEM_QNAME,
                SimpleMockWorkDefinition.class, SimpleMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                SimpleMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME,
                SimpleMockWorkDefinition.class);
    }

    @Override
    public @NotNull AbstractActivityRun<SimpleMockWorkDefinition, SimpleMockActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<SimpleMockWorkDefinition, SimpleMockActivityHandler> context,
            @NotNull OperationResult result) {
        return new SimpleMockActivityRun(context);
    }

    @NotNull CommonMockActivityHelper getMockHelper() {
        return mockHelper;
    }

    public @NotNull MockRecorder getRecorder() {
        return recorder;
    }

    @Override
    public String getIdentifierPrefix() {
        return "mock-simple";
    }
}
