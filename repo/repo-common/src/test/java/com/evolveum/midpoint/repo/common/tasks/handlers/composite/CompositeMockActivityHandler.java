/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.task.execution.ActivityContext;
import com.evolveum.midpoint.repo.common.task.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.task.execution.ActivityExecution;

import com.evolveum.midpoint.repo.common.task.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.handlers.ActivityHandlerRegistry;

import com.evolveum.midpoint.repo.common.task.task.GenericTaskHandler;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO
 */
@Component
public class CompositeMockActivityHandler implements ActivityHandler<CompositeMockWorkDefinition> {

    private static final String LEGACY_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/task/composite-mock/handler-3";

    @Autowired ActivityHandlerRegistry registry;
    @Autowired GenericTaskHandler genericTaskHandler;
    @Autowired WorkDefinitionFactory workDefinitionFactory;
    @Autowired MockRecorder recorder;

    @PostConstruct
    public void register() {
        registry.register(CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, this);
        genericTaskHandler.registerLegacyHandlerUri(LEGACY_HANDLER_URI);
        workDefinitionFactory.registerSupplier(CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, LEGACY_HANDLER_URI,
                CompositeMockWorkDefinition.supplier());
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME);
        genericTaskHandler.unregisterLegacyHandlerUri(LEGACY_HANDLER_URI);
        workDefinitionFactory.unregisterSupplier(CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME,
                LEGACY_HANDLER_URI);
    }

    @Override
    public @NotNull ActivityExecution createExecution(@NotNull ActivityContext<CompositeMockWorkDefinition> context,
            @NotNull OperationResult result) {
        return new CompositeMockActivityExecution(context, this);
    }

    @NotNull
    public MockRecorder getRecorder() {
        return recorder;
    }
}
