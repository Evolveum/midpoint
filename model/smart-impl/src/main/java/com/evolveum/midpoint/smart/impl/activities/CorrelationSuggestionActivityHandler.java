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
public class CorrelationSuggestionActivityHandler
        extends ModelActivityHandler<CorrelationSuggestionWorkDefinition, CorrelationSuggestionActivityHandler> {

    private static final String ID_CORRELATION_SUGGESTION = "correlationSuggestion";
    private static final String ID_CORRELATION_SCHEMA_MATCHING = "schemaMatching";

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                CorrelationSuggestionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_CORRELATION_SUGGESTION,
                CorrelationSuggestionWorkDefinition.class,
                CorrelationSuggestionWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                CorrelationSuggestionWorkDefinitionType.COMPLEX_TYPE, CorrelationSuggestionWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(CorrelationSuggestionWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<CorrelationSuggestionWorkDefinition, CorrelationSuggestionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<CorrelationSuggestionWorkDefinition, CorrelationSuggestionActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<CorrelationSuggestionWorkDefinition, CorrelationSuggestionActivityHandler> parentActivity) {
        var children = new ArrayList<Activity<?, ?>>();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new CorrelationSchemaMatchingActivityRun(context),
                null,
                (i) -> ID_CORRELATION_SCHEMA_MATCHING,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new CorrelationSuggestionRemoteServiceCallActivityRun(context),
                null,
                (i) -> ID_CORRELATION_SUGGESTION,
                ActivityStateDefinition.normal(),
                parentActivity));
        return children;
    }

    @Override
    public String getIdentifierPrefix() {
        return "correlation-suggestion";
    }

}
