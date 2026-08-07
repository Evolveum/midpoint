package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshSchemaWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshSchemaWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusTypeSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class RefreshSchemaActivityHandler
        extends AbstractConnDevActivityHandler<RefreshSchemaActivityHandler.WorkDefinition, RefreshSchemaActivityHandler> {

    public RefreshSchemaActivityHandler() {
        super(
                ConnDevRefreshSchemaWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_REFRESH_SCHEMA,
                ConnDevRefreshSchemaWorkStateType.COMPLEX_TYPE,
                RefreshSchemaActivityHandler.WorkDefinition.class,
                RefreshSchemaActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<WorkDefinition, RefreshSchemaActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<WorkDefinition, RefreshSchemaActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevRefreshSchemaWorkDefinitionType> {
        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<WorkDefinition, RefreshSchemaActivityHandler, FocusTypeSuggestionWorkStateType> {

        MyActivityRun(ActivityRunInstantiationContext<WorkDefinition, RefreshSchemaActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            backend.refreshConnDevDocumentation();
            return ActivityRunResult.success();
        }
    }
}
