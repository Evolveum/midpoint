/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.impl.activities;

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
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassStatisticsComputationWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassStatisticsComputationWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

/**
 * Activity handler for object class statistics computation.
 *
 * <p>Registers and manages execution of activities that compute statistics
 * for a specific object class on a resource.</p>
 */
@Component
public class ObjectClassStatisticsComputationActivityHandler
        extends ModelActivityHandler<
        ObjectClassStatisticsComputationActivityHandler.MyWorkDefinition,
        ObjectClassStatisticsComputationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ObjectClassStatisticsComputationWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_OBJECT_CLASS_STATISTICS_COMPUTATION,
                MyWorkDefinition.class,
                MyWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ObjectClassStatisticsComputationWorkDefinitionType.COMPLEX_TYPE,
                MyWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ObjectClassStatisticsComputationWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, ObjectClassStatisticsComputationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, ObjectClassStatisticsComputationActivityHandler> context,
            @NotNull OperationResult result) {
        return new ObjectClassStatisticsComputationActivityRun(context, "Statistics computation");
    }

    @Override
    public String getIdentifierPrefix() {
        return "object-class-statistics-computation";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    public static class MyWorkDefinition extends ObjectClassStatisticsComputationWorkDefinition {
        public MyWorkDefinition(WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }
}
