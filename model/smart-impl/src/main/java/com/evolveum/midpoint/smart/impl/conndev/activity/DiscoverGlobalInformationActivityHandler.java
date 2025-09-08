package com.evolveum.midpoint.smart.impl.conndev.activity;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.AffectedObjectsInformation;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscoverGlobalInformationActivityHandler
        extends AbstractConnDevActivityHandler<DiscoverGlobalInformationActivityHandler.WorkDefinition, DiscoverGlobalInformationActivityHandler> {

    public DiscoverGlobalInformationActivityHandler() {
        super(
                ConnDevDiscoverGlobalInformationWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_DISCOVER_GLOBAL_INFORMATION,
                ConnDevDiscoverGlobalInformationWorkStateType.COMPLEX_TYPE,
                DiscoverGlobalInformationActivityHandler.WorkDefinition.class,
                DiscoverGlobalInformationActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<DiscoverGlobalInformationActivityHandler.WorkDefinition, DiscoverGlobalInformationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<DiscoverGlobalInformationActivityHandler.WorkDefinition, DiscoverGlobalInformationActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    public static class WorkDefinition extends AbstractWorkDefinition {

        public WorkDefinition(WorkDefinitionFactory.@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
        }
    }

    public static class MyActivityRun
            extends LocalActivityRun<
            DiscoverGlobalInformationActivityHandler.WorkDefinition,
            DiscoverGlobalInformationActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<DiscoverGlobalInformationActivityHandler.WorkDefinition, DiscoverGlobalInformationActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();
            var beans = ConnDevBeans.get();
            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);

            ConnDevApplicationInfoType type = backend.discoverBasicInformation(task, result);
            backend.populateBasicApplicationInformation(type);

            List<ConnDevAuthInfoType> authInfo = backend.discoverAuthorizationInformation(task, result);
            backend.populateApplicationAuthInfo(authInfo);

            var state = getActivityState();

            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,new ConnDevDiscoverGlobalInformationResultType());
            state.flushPendingTaskModifications(result);
            return ActivityRunResult.success();
        }
    }
}
