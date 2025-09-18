/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSuggestionWorkStateType;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationSuggestionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

@Component
public class AssociationsSuggestionActivityHandler
        extends ModelActivityHandler<AssociationsSuggestionActivityHandler.MyWorkDefinition, AssociationsSuggestionActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationsSuggestionActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                AssociationSuggestionWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_ASSOCIATIONS_SUGGESTION,
                MyWorkDefinition.class,
                MyWorkDefinition::new,
                this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                AssociationSuggestionWorkDefinitionType.COMPLEX_TYPE,
                MyWorkDefinition.class);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(AssociationSuggestionWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, AssociationsSuggestionActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, AssociationsSuggestionActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    @Override
    public String getIdentifierPrefix() {
        return "associations-suggestion";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    public static class MyWorkDefinition extends AssociationSuggestionWorkDefinition {
        MyWorkDefinition(WorkDefinitionInfo workDefinitionType) throws ConfigurationException {
            super(workDefinitionType);
        }
    }

    static class MyActivityRun
            extends LocalActivityRun<
            MyWorkDefinition,
            AssociationsSuggestionActivityHandler,
            AssociationSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<MyWorkDefinition, AssociationsSuggestionActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var resourceOid = getWorkDefinition().getResourceOid();
            var subjectTypes = getWorkDefinition().getSubjectTypeIdentifications();
            var objectTypes = getWorkDefinition().getObjectTypeIdentifications();

            var suggestedAssociations =
                    SmartIntegrationBeans.get().smartIntegrationService.suggestAssociations(
                            resourceOid, subjectTypes, objectTypes, task, result);

            var state = getActivityState();
            state.setWorkStateItemRealValues(
                    AssociationSuggestionWorkStateType.F_RESULT, suggestedAssociations);
            state.flushPendingTaskModifications(result);

            LOGGER.debug("Association suggestions written to the work state:\n{}",
                    suggestedAssociations.debugDump(1));

            return ActivityRunResult.success();
        }
    }
}
