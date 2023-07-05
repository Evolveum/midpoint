/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.scripting;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionBean;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;

@Component
public class IterativeScriptingActivityHandler
        extends SimpleActivityHandler<
            ObjectType,
            IterativeScriptingActivityHandler.MyWorkDefinition,
            IterativeScriptingActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(IterativeScriptingActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return IterativeScriptingWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull Class<MyWorkDefinition> getWorkDefinitionClass() {
        return MyWorkDefinition.class;
    }

    @Override
    protected @NotNull WorkDefinitionSupplier getWorkDefinitionSupplier() {
        return MyWorkDefinition::new;
    }

    @Override
    protected @NotNull ExecutionSupplier<ObjectType, MyWorkDefinition, IterativeScriptingActivityHandler> getExecutionSupplier() {
        return MyRun::new;
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value();
    }

    @Override
    protected @NotNull String getShortName() {
        return "Iterative scripting";
    }

    @Override
    public String getIdentifierPrefix() {
        return "iterative-scripting";
    }

    static final class MyRun extends
            SearchBasedActivityRun<ObjectType, MyWorkDefinition, IterativeScriptingActivityHandler, AbstractActivityWorkStateType> {

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, IterativeScriptingActivityHandler> context, String shortName) {
            super(context, shortName);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .actionsExecutedStatisticsSupported(true);
        }

        // We allow dry run mode, as it is conceivable that the script
        // could check for it. (To be decided.)

        @Override
        public boolean processItem(@NotNull ObjectType object,
                @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult result)
                throws CommonException, ActivityRunException {
            executeScriptOnObject(object, workerTask, result);
            return true;
        }

        private void executeScriptOnObject(ObjectType object, RunningTask workerTask, OperationResult result)
                throws CommonException {
            ExecuteScriptType executeScriptRequest = getWorkDefinition().getScriptExecutionRequest().clone();
            executeScriptRequest.setInput(new ValueListType().value(object));
            ScriptExecutionResult executionResult = getActivityHandler().scriptingService.evaluateExpression(executeScriptRequest,
                    VariablesMap.emptyMap(), false, workerTask, result);
            LOGGER.debug("Execution output: {} item(s)", executionResult.getDataOutput().size());
            LOGGER.debug("Execution result:\n{}", executionResult.getConsoleOutput());
            result.computeStatus();
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        private final ObjectSetType objects;
        private final ExecuteScriptType scriptExecutionRequest;

        MyWorkDefinition(@NotNull WorkDefinitionBean source) {
            var typedDefinition = (IterativeScriptingWorkDefinitionType) source.getBean();
            objects = ObjectSetUtil.fromConfiguration(typedDefinition.getObjects());
            scriptExecutionRequest = typedDefinition.getScriptExecutionRequest();
            argCheck(scriptExecutionRequest != null, "No script execution request provided");
            argCheck(scriptExecutionRequest.getScriptingExpression() != null, "No scripting expression provided");
            if (scriptExecutionRequest.getInput() != null && !scriptExecutionRequest.getInput().getValue().isEmpty()) {
                LOGGER.warn("Ignoring input values in executeScript data: {}", scriptExecutionRequest);
            }
        }

        @Override
        public ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        public ExecuteScriptType getScriptExecutionRequest() {
            return scriptExecutionRequest;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
            DebugUtil.debugDumpWithLabel(sb, "scriptExecutionRequest", String.valueOf(scriptExecutionRequest), indent+1);
        }
    }
}
