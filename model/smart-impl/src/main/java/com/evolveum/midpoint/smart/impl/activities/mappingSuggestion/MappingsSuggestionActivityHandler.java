/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.mappingSuggestion;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class MappingsSuggestionActivityHandler
        extends ModelActivityHandler<MappingsSuggestionWorkDefinition, MappingsSuggestionActivityHandler> {

    private static final String ID_STATISTICS_COMPUTATION = "statisticsComputation";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";
    private static final String ID_SCHEMA_MATCHING = "schemaMatching";

    @Autowired private ModelService modelService;
    @Autowired private RepositoryService repositoryService;
    @Autowired private SmartIntegrationService smartIntegrationService;
    @Autowired private Clock clock;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                MappingsSuggestionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_MAPPINGS_SUGGESTION,
                MappingsSuggestionWorkDefinition.class,
                MappingsSuggestionWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                MappingsSuggestionWorkDefinitionType.COMPLEX_TYPE, MappingsSuggestionWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(MappingsSuggestionWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<MappingsSuggestionWorkDefinition, MappingsSuggestionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MappingsSuggestionWorkDefinition, MappingsSuggestionActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<MappingsSuggestionWorkDefinition, MappingsSuggestionActivityHandler> parentActivity) {
        var children = new ArrayList<Activity<?, ?>>();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new MappingsSuggestionStatisticsComputationActivityRun(
                        context, modelService, repositoryService, smartIntegrationService, clock),
                null,
                (i) -> ID_STATISTICS_COMPUTATION,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new MappingsSuggestionSchemaMatchingActivityRun(context),
                null,
                (i) -> ID_SCHEMA_MATCHING,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new MappingsSuggestionRemoteServiceCallActivityRun(context),
                null,
                (i) -> ID_MAPPINGS_SUGGESTION,
                ActivityStateDefinition.normal(),
                parentActivity));
        return children;
    }

    @Override
    public String getIdentifierPrefix() {
        return "mappings-suggestion";
    }

}
