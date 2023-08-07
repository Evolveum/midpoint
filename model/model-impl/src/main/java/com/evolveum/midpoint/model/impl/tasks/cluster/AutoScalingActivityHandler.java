/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks.cluster;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Activity handler for "auto-scaling" activity.
 */
@Component
public class AutoScalingActivityHandler
        extends ModelActivityHandler<
            AutoScalingWorkDefinition,
            AutoScalingActivityHandler> {

    @Autowired TaskActivityManager activityManager;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ActivityAutoScalingWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_ACTIVITY_AUTO_SCALING,
                AutoScalingWorkDefinition.class, AutoScalingWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ActivityAutoScalingWorkDefinitionType.COMPLEX_TYPE, AutoScalingWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<AutoScalingWorkDefinition, AutoScalingActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<AutoScalingWorkDefinition, AutoScalingActivityHandler> context,
            @NotNull OperationResult result) {
        return new AutoScalingActivityRun(context);
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();
    }

    @Override
    public String getIdentifierPrefix() {
        return "auto-scaling";
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.perpetual(ActivityAutoScalingWorkStateType.COMPLEX_TYPE);
    }
}
