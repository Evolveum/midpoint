/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.activity.handlers;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.StandaloneActivity;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.definition.CompositeWorkDefinition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomCompositeWorkStateType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Handles custom composite activities, see
 * https://docs.evolveum.com/midpoint/reference/tasks/activities/#configuring-custom-composite-activities.
 */
@Component
public class CustomCompositeActivityHandler implements ActivityHandler<CompositeWorkDefinition, CustomCompositeActivityHandler> {

    @Autowired ActivityHandlerRegistry handlerRegistry;
    @Autowired WorkDefinitionFactory workDefinitionFactory;

    @PostConstruct
    public void register() {
        handlerRegistry.registerHandler(CompositeWorkDefinition.class, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregisterHandler(CompositeWorkDefinition.class);
    }

    @Override
    public @NotNull AbstractActivityRun<CompositeWorkDefinition, CustomCompositeActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<CompositeWorkDefinition, CustomCompositeActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(Activity<CompositeWorkDefinition, CustomCompositeActivityHandler> parent) {
        return parent.getWorkDefinition().getComposition().getActivity().stream()
                .sorted(Comparator.comparing(
                        ActivityDefinitionType::getOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(definitionBean -> ActivityDefinition.createChild(definitionBean, workDefinitionFactory))
                .map(definition -> createChildActivity(definition, parent))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private <WD extends WorkDefinition, AH extends ActivityHandler<WD, AH>> Activity<?, ?> createChildActivity(
            ActivityDefinition<WD> definition, Activity<CompositeWorkDefinition, ?> parent) {
        AH handler = handlerRegistry.getHandler(definition);
        return StandaloneActivity.createNonRoot(definition, handler, parent);
    }

    @Override
    public String getIdentifierPrefix() {
        return "composition";
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(CustomCompositeWorkStateType.COMPLEX_TYPE);
    }
}
