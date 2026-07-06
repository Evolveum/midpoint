package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ScimBackend;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshScimSchemaWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshScimSchemaWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusTypeSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class RefreshScimSchemaActivityHandler
        extends AbstractConnDevActivityHandler<RefreshScimSchemaActivityHandler.WorkDefinition, RefreshScimSchemaActivityHandler> {

    public RefreshScimSchemaActivityHandler() {
        super(
                ConnDevRefreshScimSchemaWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_REFRESH_SCIM_SCHEMA,
                ConnDevRefreshScimSchemaWorkStateType.COMPLEX_TYPE,
                RefreshScimSchemaActivityHandler.WorkDefinition.class,
                RefreshScimSchemaActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<WorkDefinition, RefreshScimSchemaActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<WorkDefinition, RefreshScimSchemaActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevRefreshScimSchemaWorkDefinitionType> {
        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<WorkDefinition, RefreshScimSchemaActivityHandler, FocusTypeSuggestionWorkStateType> {

        MyActivityRun(ActivityRunInstantiationContext<WorkDefinition, RefreshScimSchemaActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            if (backend instanceof ScimBackend scimBackend) {
                scimBackend.refreshScimDocumentation();
            }
            return ActivityRunResult.success();
        }
    }
}
