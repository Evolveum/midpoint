/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.sync.SynchronizationService;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskExecution;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;

/**
 * Task handler for search-iterative tasks in model (and upper) layers.
 *
 * It currently provides the model-level beans useful for individual task handlers and related code.
 *
 * @author semancik
 */
public abstract class AbstractSearchIterativeModelTaskHandler
        <TH extends AbstractSearchIterativeTaskHandler<TH, TE>,
                TE extends AbstractSearchIterativeTaskExecution<TH, TE>>
        extends AbstractSearchIterativeTaskHandler<TH, TE> {

    // WARNING! This task handler is efficiently singleton!
    // It is a spring bean and it is supposed to handle all search task instances
    // Therefore it must not have task-specific fields. It can only contain fields specific to
    // all tasks of a specified type
    // If you need to store fields specific to task instance or task run the ResultHandler is a good place to do that.

    @Autowired protected ModelObjectResolver modelObjectResolver;
    @Autowired protected SecurityEnforcer securityEnforcer;
    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected SystemObjectCache systemObjectCache;
    @Autowired protected ModelService model;
    @Autowired protected SynchronizationService synchronizationService;
    @Autowired protected Clock clock;
    @Autowired protected ProvisioningService provisioningService;
    @Autowired protected ContextFactory contextFactory;
    @Autowired protected Clockwork clockwork;

    protected AbstractSearchIterativeModelTaskHandler(String taskName, String taskOperationPrefix) {
        super(taskName, taskOperationPrefix);
    }

    public ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    public ProvisioningService getProvisioningService() {
        return provisioningService;
    }

    public SynchronizationService getSynchronizationService() {
        return synchronizationService;
    }

    public SystemObjectCache getSystemObjectsCache() {
        return systemObjectCache;
    }

    public Clock getClock() {
        return clock;
    }
}
