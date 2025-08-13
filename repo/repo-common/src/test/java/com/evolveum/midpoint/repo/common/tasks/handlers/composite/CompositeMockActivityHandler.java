/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.tasks.handlers.composite;

import java.util.ArrayList;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;

import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.tasks.handlers.AbstractMockActivityHandler;

import com.evolveum.midpoint.repo.common.tasks.handlers.CommonMockActivityHelper;

import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerRegistry;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Activity handler for the "composite mock" activity.
 */
@Component
public class CompositeMockActivityHandler
        extends AbstractMockActivityHandler<CompositeMockWorkDefinition, CompositeMockActivityHandler> {

    @Autowired ActivityHandlerRegistry registry;
    @Autowired WorkDefinitionFactory workDefinitionFactory;
    @Autowired private CommonMockActivityHelper mockHelper;
    @Autowired MockRecorder recorder;
    @Autowired CommonTaskBeans commonTaskBeans;

    @PostConstruct
    public void register() {
        registry.register(
                CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME, CompositeMockWorkDefinition.WORK_DEFINITION_ITEM_QNAME,
                CompositeMockWorkDefinition.class, CompositeMockWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        workDefinitionFactory.unregisterSupplier(CompositeMockWorkDefinition.WORK_DEFINITION_TYPE_QNAME);
        registry.unregisterHandler(CompositeMockWorkDefinition.class);
    }

    @NotNull
    @Override
    public AbstractActivityRun<CompositeMockWorkDefinition, CompositeMockActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<CompositeMockWorkDefinition, CompositeMockActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<CompositeMockWorkDefinition, CompositeMockActivityHandler> parentActivity) {
        CompositeMockWorkDefinition workDefinition = parentActivity.getWorkDefinition();
        ArrayList<Activity<?, ?>> children = new ArrayList<>();
        if (workDefinition.isOpeningEnabled()) {
            children.add(EmbeddedActivity.create(
                    parentActivity.getDefinition().cloneWithoutId(),
                    (context, result) -> new MockOpeningActivityRun(context),
                    this::runBeforeExecution,
                    (i) -> "opening",
                    ActivityStateDefinition.normal(),
                    parentActivity));
        }
        if (workDefinition.isClosingEnabled()) {
            children.add(EmbeddedActivity.create(
                    parentActivity.getDefinition().cloneWithoutId(),
                    (context, result) -> new MockClosingActivityRun(context),
                    this::runBeforeExecution,
                    (i) -> "closing",
                    ActivityStateDefinition.perpetual(),
                    parentActivity));
        }
        return children;
    }

    private void runBeforeExecution(
            EmbeddedActivity<CompositeMockWorkDefinition, CompositeMockActivityHandler> activity,
            RunningTask runningTask, OperationResult result) throws CommonException {
        ActivityState parentActivityState =
                ActivityState.getActivityStateUpwards(
                        activity.getPath().allExceptLast(),
                        runningTask,
                        AbstractActivityWorkStateType.COMPLEX_TYPE,
                        result);
        getMockHelper().setLastMessage(
                parentActivityState,
                createUpdatedMessage(
                        getMockHelper().getLastMessage(parentActivityState),
                        activity.getIdentifier()),
                result);
    }

    private String createUpdatedMessage(String lastMessage, String currentMessage) {
        if (lastMessage != null) {
            return lastMessage + " + " + currentMessage;
        } else {
            return currentMessage;
        }
    }

    public @NotNull MockRecorder getRecorder() {
        return recorder;
    }

    @NotNull CommonMockActivityHelper getMockHelper() {
        return mockHelper;
    }

    @Override
    public String getIdentifierPrefix() {
        return "mock-composite";
    }
}
