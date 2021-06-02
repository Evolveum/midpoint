/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
@Component
public class CompositeMockActivityHandler implements ActivityHandler<CompositeMockWorkDefinition, CompositeMockActivityHandler> {

    private static final String LEGACY_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/composite-mock/handler-3";

    @Autowired ActivityHandlerRegistry registry;
    @Autowired WorkDefinitionFactory workDefinitionFactory;
    @Autowired MockRecorder recorder;

    @PostConstruct
    public void register() {
        registry.register(CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, LEGACY_HANDLER_URI,
                CompositeMockWorkDefinition.class, CompositeMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        workDefinitionFactory.unregisterSupplier(CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME,
                LEGACY_HANDLER_URI);
        registry.unregisterHandler(CompositeMockWorkDefinition.class);
    }

    @NotNull
    @Override
    public AbstractActivityExecution<CompositeMockWorkDefinition, CompositeMockActivityHandler> createExecution(
            @NotNull ExecutionInstantiationContext<CompositeMockWorkDefinition, CompositeMockActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeMockActivityExecution(context);
    }

    @Override
    public List<Activity<?, ?>> createChildActivities(
            Activity<CompositeMockWorkDefinition, CompositeMockActivityHandler> parentActivity) {
        CompositeMockWorkDefinition workDefinition = parentActivity.getWorkDefinition();
        List<Activity<?, ?>> children = new ArrayList<>();
        if (workDefinition.isOpeningEnabled()) {
            children.add(EmbeddedActivity.create(parentActivity.getDefinition(),
                    (context, result) -> new MockOpeningActivityExecution(context),
                    (i) -> "opening",
                    parentActivity));
        }
        if (workDefinition.isClosingEnabled()) {
            children.add(EmbeddedActivity.create(parentActivity.getDefinition(),
                    (context, result) -> new MockClosingActivityExecution(context),
                    (i) -> "closing",
                    parentActivity));
        }
        return children;
    }

    @NotNull
    public MockRecorder getRecorder() {
        return recorder;
    }

    @Override
    public String getIdentifierPrefix() {
        return "mock-composite";
    }
}
