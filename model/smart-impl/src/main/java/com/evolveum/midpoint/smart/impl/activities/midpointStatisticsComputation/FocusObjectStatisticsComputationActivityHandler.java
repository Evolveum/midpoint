/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */
package com.evolveum.midpoint.smart.impl.activities.midpointStatisticsComputation;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Activity handler for focus object statistics computation.
 *
 * Registers and manages execution of activities that compute statistics
 * for focus objects of a specific type (e.g. UserType, RoleType).
 */
@Component
public class FocusObjectStatisticsComputationActivityHandler
        extends ModelActivityHandler<
        FocusObjectStatisticsComputationWorkDefinition,
        FocusObjectStatisticsComputationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                FocusObjectStatisticsComputationWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_FOCUS_OBJECT_STATISTICS_COMPUTATION,
                FocusObjectStatisticsComputationWorkDefinition.class,
                FocusObjectStatisticsComputationWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                FocusObjectStatisticsComputationWorkDefinitionType.COMPLEX_TYPE,
                FocusObjectStatisticsComputationWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(FocusObjectStatisticsComputationWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public String getIdentifierPrefix() {
        return "focus-object-statistics-computation";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    @Override
    public AbstractActivityRun<FocusObjectStatisticsComputationWorkDefinition, FocusObjectStatisticsComputationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<FocusObjectStatisticsComputationWorkDefinition, FocusObjectStatisticsComputationActivityHandler> context,
            @NotNull OperationResult result) {
        return new FocusObjectStatisticsComputationActivityRun(context, "Focus object statistics computation");
    }
}
