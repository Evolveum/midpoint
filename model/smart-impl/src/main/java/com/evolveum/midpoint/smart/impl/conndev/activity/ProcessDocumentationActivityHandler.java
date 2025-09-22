package com.evolveum.midpoint.smart.impl.conndev.activity;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.conndev.ConnectorDevelopmentBackend;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ProcessDocumentationActivityHandler
        extends AbstractConnDevActivityHandler<ProcessDocumentationActivityHandler.WorkDefinition, ProcessDocumentationActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ProcessDocumentationActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();

    public ProcessDocumentationActivityHandler() {
        super(
                ConnDevProcessDocumentationWorkDefinitionType.COMPLEX_TYPE,
                WorkDefinitionsType.F_PROCESS_DOCUMENTATION,
                ConnDevProcessDocumentationWorkStateType.COMPLEX_TYPE,
                ProcessDocumentationActivityHandler.WorkDefinition.class,
                ProcessDocumentationActivityHandler.WorkDefinition::new);
    }

    @Override
    public AbstractActivityRun<ProcessDocumentationActivityHandler.WorkDefinition, ProcessDocumentationActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<ProcessDocumentationActivityHandler.WorkDefinition, ProcessDocumentationActivityHandler> context,
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
            ProcessDocumentationActivityHandler.WorkDefinition,
            ProcessDocumentationActivityHandler,
            FocusTypeSuggestionWorkStateType> {

        MyActivityRun(
                ActivityRunInstantiationContext<ProcessDocumentationActivityHandler.WorkDefinition, ProcessDocumentationActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
            var task = getRunningTask();

            var backend = ConnectorDevelopmentBackend.backendFor(getWorkDefinition().connectorDevelopmentOid, task, result);
            backend.processDocumentation();

            var ret = new ConnDevProcessDocumentationResultType();

            var state = getActivityState();
            state.setWorkStateItemRealValues(FocusTypeSuggestionWorkStateType.F_RESULT,ret);
            state.flushPendingTaskModifications(result);

            return ActivityRunResult.success();
        }
    }
}
