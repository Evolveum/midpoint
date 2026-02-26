/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.objectTypeStatisticsComputation;

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
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Activity handler for object type statistics computation.
 *
 * <p>Registers and manages execution of activities that compute statistics
 * for a specific object type on a resource.</p>
 */
@Component
public class ObjectTypeStatisticsComputationActivityHandler
        extends ModelActivityHandler<
        ObjectTypeStatisticsComputationWorkDefinition,
        ObjectTypeStatisticsComputationActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ObjectTypeStatisticsComputationWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_OBJECT_TYPE_STATISTICS_COMPUTATION,
                MyWorkDefinition.class,
                MyWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ObjectTypeStatisticsComputationWorkDefinitionType.COMPLEX_TYPE,
                MyWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ObjectTypeStatisticsComputationWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public String getIdentifierPrefix() {
        return "object-type-statistics-computation";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    @Override
    public AbstractActivityRun<ObjectTypeStatisticsComputationWorkDefinition, ObjectTypeStatisticsComputationActivityHandler, ?> createActivityRun(@NotNull ActivityRunInstantiationContext<ObjectTypeStatisticsComputationWorkDefinition, ObjectTypeStatisticsComputationActivityHandler> context, @NotNull OperationResult result) {
        return new ObjectTypeStatisticsComputationActivityRun(context, "Object type statistics computation");
    }

    public static class MyWorkDefinition extends ObjectTypeStatisticsComputationWorkDefinition {
        public MyWorkDefinition(WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }
}
