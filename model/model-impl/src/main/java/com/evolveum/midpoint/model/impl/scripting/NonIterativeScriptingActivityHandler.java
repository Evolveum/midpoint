/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NonIterativeScriptingWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;

/**
 * This is a handler for "old", non-iterative (single) bulk actions.
 */
@Component
public class NonIterativeScriptingActivityHandler
        extends ModelActivityHandler<
        NonIterativeScriptingActivityHandler.MyWorkDefinition,
        NonIterativeScriptingActivityHandler> {

    @Autowired private ScriptingService scriptingService;

    private static final Trace LOGGER = TraceManager.getTrace(NonIterativeScriptingActivityHandler.class);

    private static final String OP_EXECUTE = NonIterativeScriptingActivityHandler.class.getName() + ".execute";

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                NonIterativeScriptingWorkDefinitionType.COMPLEX_TYPE,
                MyWorkDefinition.class, MyWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                NonIterativeScriptingWorkDefinitionType.COMPLEX_TYPE, MyWorkDefinition.class);
    }

    @Override
    public String getIdentifierPrefix() {
        return "non-iterative-scripting";
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SINGLE_BULK_ACTION_TASK.value();
    }

    @Override
    public AbstractActivityRun<MyWorkDefinition, NonIterativeScriptingActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<MyWorkDefinition, NonIterativeScriptingActivityHandler> context,
            @NotNull OperationResult result) {
        return new MyActivityRun(context);
    }

    final static class MyActivityRun
            extends LocalActivityRun<MyWorkDefinition, NonIterativeScriptingActivityHandler, AbstractActivityWorkStateType> {

        MyActivityRun(
                @NotNull ActivityRunInstantiationContext<MyWorkDefinition, NonIterativeScriptingActivityHandler> context) {
            super(context);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .statisticsSupported(true)
                    .progressSupported(true)
                    .progressCommitPointsSupported(false);
        }

        @Override
        protected @NotNull ActivityRunResult runLocally(OperationResult parentResult) throws CommonException {
            RunningTask runningTask = getRunningTask();
            ExecuteScriptType executeScriptRequest = getWorkDefinition().getScriptExecutionRequest().clone();
            runningTask.setExecutionSupport(this);

            // We need to create a subresult in order to be able to determine its status - we have to close it to get the status.
            OperationResult result = parentResult.createSubresult(OP_EXECUTE);
            try {
                ScriptExecutionResult executionResult =
                        getActivityHandler().scriptingService
                                .evaluateExpression(
                                        executeScriptRequest,
                                        VariablesMap.emptyMap(),
                                        true,
                                        runningTask,
                                        result);
                LOGGER.debug("Execution output: {} item(s)", executionResult.getDataOutput().size());
                LOGGER.debug("Execution result:\n{}", executionResult.getConsoleOutput());
            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                runningTask.setExecutionSupport(null);
                result.close();
            }
            return standardRunResult(result.getStatus());
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition {

        private final ExecuteScriptType scriptExecutionRequest;

        MyWorkDefinition(@NotNull WorkDefinitionBean source) {
            var typedDefinition = (NonIterativeScriptingWorkDefinitionType) source.getBean();
            scriptExecutionRequest = typedDefinition.getScriptExecutionRequest();
            argCheck(scriptExecutionRequest != null, "No script execution request provided");
            argCheck(scriptExecutionRequest.getScriptingExpression() != null, "No scripting expression provided");
        }

        public ExecuteScriptType getScriptExecutionRequest() {
            return scriptExecutionRequest;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabel(sb, "scriptExecutionRequest", String.valueOf(scriptExecutionRequest), indent+1);
        }
    }
}
