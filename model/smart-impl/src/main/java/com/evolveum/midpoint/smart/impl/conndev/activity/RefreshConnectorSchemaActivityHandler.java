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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshConnectorSchemaResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshConnectorSchemaWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRefreshConnectorSchemaWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkDefinitionsType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class RefreshConnectorSchemaActivityHandler
        extends AbstractConnDevActivityHandler<RefreshConnectorSchemaActivityHandler.WorkDefinition, RefreshConnectorSchemaActivityHandler> {

    public RefreshConnectorSchemaActivityHandler() {
        super(
                ConnDevRefreshConnectorSchemaWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_REFRESH_CONNECTOR_SCHEMA,
                ConnDevRefreshConnectorSchemaWorkStateType.COMPLEX_TYPE,
                RefreshConnectorSchemaActivityHandler.WorkDefinition.class,
                RefreshConnectorSchemaActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<WorkDefinition, RefreshConnectorSchemaActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<WorkDefinition, RefreshConnectorSchemaActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition<ConnDevRefreshConnectorSchemaWorkDefinitionType> {
        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<WorkDefinition, RefreshConnectorSchemaActivityHandler, ConnDevRefreshConnectorSchemaWorkStateType> {

        MyActivityRun(ActivityRunInstantiationContext<WorkDefinition, RefreshConnectorSchemaActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            backend.refreshConnectorSchema(result);

            var state = getActivityState();
            state.setWorkStateItemRealValues(ConnDevRefreshConnectorSchemaWorkStateType.F_RESULT, new ConnDevRefreshConnectorSchemaResultType());
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
