/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers;

import com.evolveum.midpoint.repo.common.task.TaskPartExecutionFactoryRegistry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.task.TaskPartExecutionFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static com.evolveum.midpoint.repo.common.task.TaskPartExecutionFactoryRegistry.partMatcher;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * TODO
 */
@Component
public class MockPartExecutionFactory {

    private static final TaskPartExecutionFactory MOCK_PART_EXECUTION_FACTORY =
            (partDef, taskExecution, result) -> new MockTaskPartExecution(taskExecution);

    private static final TaskPartExecutionFactory OPENING_PART_EXECUTION_FACTORY =
            (partDef, taskExecution, result) -> new MockOpeningTaskPartExecution(taskExecution);

    private static final TaskPartExecutionFactory CLOSING_PART_EXECUTION_FACTORY =
            (partDef, taskExecution, result) -> new MockClosingTaskPartExecution(taskExecution);

    @Autowired TaskPartExecutionFactoryRegistry registry;

    @PostConstruct
    public void register() {
        registry.register(
                partMatcher(
                        singleton(AbstractMockTaskPartExecution.COMMON_HANDLER_URI),
                        singleton(AbstractMockTaskPartExecution.MOCK_PARAMETERS_TYPE_QNAME)),
                MOCK_PART_EXECUTION_FACTORY);

        registry.register(
                partMatcher(
                        singleton(AbstractMockTaskPartExecution.OPENING_PART_HANDLER_URI),
                        emptySet()),
                OPENING_PART_EXECUTION_FACTORY);

        registry.register(
                partMatcher(
                        singleton(AbstractMockTaskPartExecution.CLOSING_PART_HANDLER_URI),
                        emptySet()),
                CLOSING_PART_EXECUTION_FACTORY);
    }

    @PreDestroy
    public void unregister() {
        registry.unregister(MOCK_PART_EXECUTION_FACTORY);
        registry.unregister(OPENING_PART_EXECUTION_FACTORY);
        registry.unregister(CLOSING_PART_EXECUTION_FACTORY);
    }
}
