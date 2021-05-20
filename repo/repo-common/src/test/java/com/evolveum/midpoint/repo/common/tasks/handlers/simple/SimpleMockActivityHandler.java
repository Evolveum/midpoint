/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.simple;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.task.execution.ActivityInstantiationContext;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;

import com.evolveum.midpoint.repo.common.task.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.handlers.ActivityHandlerRegistry;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
@Component
public class SimpleMockActivityHandler implements ActivityHandler<SimpleMockWorkDefinition> {

    private static final String LEGACY_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/simple-mock/handler-3";

    @Autowired private ActivityHandlerRegistry handlerRegistry;
    @Autowired private MockRecorder recorder;

    @PostConstruct
    public void register() {
        handlerRegistry.register(SimpleMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, LEGACY_HANDLER_URI,
                SimpleMockWorkDefinition.class, SimpleMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(SimpleMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, LEGACY_HANDLER_URI,
                SimpleMockWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityExecution createExecution(@NotNull ActivityInstantiationContext<SimpleMockWorkDefinition> context,
            @NotNull OperationResult result) {
        return new SimpleMockActivityExecution(context, this);
    }

    public @NotNull MockRecorder getRecorder() {
        return recorder;
    }
}
