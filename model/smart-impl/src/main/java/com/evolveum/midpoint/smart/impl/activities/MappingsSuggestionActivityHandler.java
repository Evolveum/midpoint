package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class MappingsSuggestionActivityHandler
        extends ModelActivityHandler<ObjectTypeRelatedSuggestionWorkDefinition, MappingsSuggestionActivityHandler> {

    private static final String ID_MAPPING_STATISTICS_COMPUTATION = "mappingStatisticsComputation";
    private static final String ID_MAPPINGS_SUGGESTION = "mappingsSuggestion";

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                MappingsSuggestionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_MAPPINGS_SUGGESTION,
                ObjectTypeRelatedSuggestionWorkDefinition.class,
                ObjectTypeRelatedSuggestionWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                MappingsSuggestionWorkDefinitionType.COMPLEX_TYPE, ObjectTypeRelatedSuggestionWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(MappingsSuggestionWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<ObjectTypeRelatedSuggestionWorkDefinition, MappingsSuggestionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ObjectTypeRelatedSuggestionWorkDefinition, MappingsSuggestionActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<ObjectTypeRelatedSuggestionWorkDefinition, MappingsSuggestionActivityHandler> parentActivity) {
        var children = new ArrayList<Activity<?, ?>>();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new MappingsStatisticsComputationActivityRun(context, "Statistics computation"),
                null,
                (i) -> ID_MAPPING_STATISTICS_COMPUTATION,
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
