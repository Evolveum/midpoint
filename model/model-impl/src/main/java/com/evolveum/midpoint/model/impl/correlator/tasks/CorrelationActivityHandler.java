/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.correlator.tasks;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity handler for correlation simulation activity.
 *
 * Correlation simulation activity is used, as name suggest, to simulate correlation with provided correlator on given
 * set of shadows.
 *
 * **Supports only simulation (preview) mode.**
 */
@Component
public class CorrelationActivityHandler
        implements ActivityHandler<CorrelationWorkDefinition, CorrelationActivityHandler> {

    @Autowired
    private final ActivityHandlerRegistry activityHandlerRegistry;

    public CorrelationActivityHandler(ActivityHandlerRegistry activityHandlerRegistry) {
        this.activityHandlerRegistry = activityHandlerRegistry;
    }

    @PostConstruct
    public void init() {
        this.activityHandlerRegistry.register(CorrelationWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_CORRELATION, CorrelationWorkDefinition.class, CorrelationWorkDefinition::new,
                this
        );
    }

    public CorrelationActivityRun createActivityRun(
            @NotNull ActivityRunInstantiationContext<CorrelationWorkDefinition, CorrelationActivityHandler> ctx,
            @NotNull OperationResult result) {
        return new CorrelationActivityRun(ctx);
    }
}
