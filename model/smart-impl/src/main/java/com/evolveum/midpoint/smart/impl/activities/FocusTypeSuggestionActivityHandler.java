package com.evolveum.midpoint.smart.impl.activities;

import java.util.ArrayList;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;

import javax.xml.namespace.QName;

@Component
public class FocusTypeSuggestionActivityHandler
        extends ModelActivityHandler<FocusTypeSuggestionWorkDefinition, FocusTypeSuggestionActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusTypeSuggestionActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                FocusTypeSuggestionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_FOCUS_TYPE_SUGGESTION,
                FocusTypeSuggestionWorkDefinition.class,
                FocusTypeSuggestionWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                FocusTypeSuggestionWorkDefinitionType.COMPLEX_TYPE, FocusTypeSuggestionWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(FocusTypeSuggestionWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<FocusTypeSuggestionWorkDefinition, FocusTypeSuggestionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<FocusTypeSuggestionWorkDefinition, FocusTypeSuggestionActivityHandler> context,
            @NotNull OperationResult result) {
        return new FocusTypeSuggestionActivityHandler.MyActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "focus-type-suggestion";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    static class MyActivityRun
            extends LocalActivityRun<
            FocusTypeSuggestionWorkDefinition,
            FocusTypeSuggestionActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<FocusTypeSuggestionWorkDefinition, FocusTypeSuggestionActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var resourceOid = getWorkDefinition().getResourceOid();
            var typeIdentification = getWorkDefinition().getTypeIdentification();

            QName suggestedFocusType = SmartIntegrationBeans.get().smartIntegrationService.suggestFocusType(
                    resourceOid, typeIdentification, task, result
            );

            var state = getActivityState();
            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT, suggestedFocusType);
            state.flushPendingTaskModifications(result);

            return ActivityRunResult.success();
        }
    }
}
