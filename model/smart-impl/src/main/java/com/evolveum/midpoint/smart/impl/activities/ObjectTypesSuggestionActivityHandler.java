/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.activities;

import java.util.ArrayList;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

@Component
public class ObjectTypesSuggestionActivityHandler
        extends ModelActivityHandler<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    private static final String ID_STATISTICS_COMPUTATION = "statisticsComputation";
    private static final String ID_OBJECT_TYPES_SUGGESTION = "objectTypesSuggestion";
    private static final String ID_FOCUS_TYPE_SUGGESTION = "focusTypeSuggestion";

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ObjectTypesSuggestionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_OBJECT_TYPES_SUGGESTION,
                ObjectTypesSuggestionWorkDefinition.class,
                ObjectTypesSuggestionWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                ObjectTypesSuggestionWorkDefinitionType.COMPLEX_TYPE, ObjectTypesSuggestionWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ObjectTypesSuggestionWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> parentActivity) {
        var children = new ArrayList<Activity<?, ?>>();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new StatisticsComputationActivityRun(context, "Statistics computation"),
                null,
                (i) -> ID_STATISTICS_COMPUTATION,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new RemoteServiceCallActivityRun(context),
                null,
                (i) -> ID_OBJECT_TYPES_SUGGESTION,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new RemoteServiceCallFocusTypeActivityRun(context),
                null,
                (i) -> ID_FOCUS_TYPE_SUGGESTION,
                ActivityStateDefinition.normal(),
                parentActivity));
        return children;
    }

    @Override
    public String getIdentifierPrefix() {
        return "object-types-suggestion";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
