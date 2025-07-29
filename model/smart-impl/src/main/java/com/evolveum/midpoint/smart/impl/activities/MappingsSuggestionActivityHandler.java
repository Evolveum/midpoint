/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@Component
public class MappingsSuggestionActivityHandler
        extends ModelActivityHandler<MappingsSuggestionActivityHandler.MyWorkDefinition, MappingsSuggestionActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSuggestionActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                MappingsSuggestionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_MAPPINGS_SUGGESTION,
                MyWorkDefinition.class,
                MyWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                MappingsSuggestionWorkDefinitionType.COMPLEX_TYPE, MyWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(MappingsSuggestionWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, MappingsSuggestionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, MappingsSuggestionActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "mappings-suggestion";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    public static class MyWorkDefinition extends ObjectTypeRelatedSuggestionWorkDefinition {
        MyWorkDefinition(WorkDefinitionFactory.WorkDefinitionInfo workDefinitionType) throws ConfigurationException {
            super(workDefinitionType);
        }
    }

    static class MyActivityRun
            extends LocalActivityRun<
            MyWorkDefinition,
            MappingsSuggestionActivityHandler,
            MappingsSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<MyWorkDefinition, MappingsSuggestionActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var resourceOid = getWorkDefinition().getResourceOid();
            var typeIdentification = getWorkDefinition().getTypeIdentification();

            var suggestedMappings = SmartIntegrationBeans.get().smartIntegrationService.suggestMappings(
                    resourceOid, typeIdentification, null, null, task, result);

            var state = getActivityState();
            state.setWorkStateItemRealValues(MappingsSuggestionWorkStateType.F_RESULT, suggestedMappings);
            state.flushPendingTaskModifications(result);
            LOGGER.debug("Suggestions written to the work state:\n{}", suggestedMappings.debugDump(1));

            return ActivityRunResult.success();
        }
    }
}
