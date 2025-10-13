/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.scripting;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.BulkActionExecutionOptions;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.BulkActionExecutionResult;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
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
    protected @NotNull QName getWorkDefinitionItemName() {
        return WorkDefinitionsType.F_ITERATIVE_SCRIPTING;
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
                throws CommonException {
            executeScriptOnObject(object, workerTask, result);
            return true;
        }

        private void executeScriptOnObject(ObjectType object, RunningTask workerTask, OperationResult result)
                throws CommonException {
            ExecuteScriptConfigItem requestCloned = getWorkDefinition().getScriptExecutionRequest().clone();
            requestCloned.value().setInput(new ValueListType().value(object));
            BulkActionExecutionResult executionResult =
                    getActivityHandler().bulkActionsService.executeBulkAction(
                            requestCloned,
                            VariablesMap.emptyMap(),
                            BulkActionExecutionOptions.create(),
                            workerTask,
                            result);
            LOGGER.debug("Execution output: {} item(s)", executionResult.getDataOutput().size());
            LOGGER.debug("Execution result:\n{}", executionResult.getConsoleOutput());
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;
        @NotNull private final ExecuteScriptType scriptExecutionRequest;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
            var typedDefinition = (IterativeScriptingWorkDefinitionType) info.getBean();
            objects = ObjectSetUtil.emptyIfNull(typedDefinition.getObjects());
            scriptExecutionRequest = typedDefinition.getScriptExecutionRequest();
            argCheck(scriptExecutionRequest != null, "No script execution request provided");
            argCheck(scriptExecutionRequest.getScriptingExpression() != null, "No scripting expression provided");
            if (scriptExecutionRequest.getInput() != null && !scriptExecutionRequest.getInput().getValue().isEmpty()) {
                LOGGER.warn("Ignoring input values in executeScript data: {}", scriptExecutionRequest);
            }
        }

        @Override
        public @NotNull ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        public @NotNull ExecuteScriptConfigItem getScriptExecutionRequest() {
            // note that the origin is usually only approximate here, so the "child" call is more or less useless for now
            return ExecuteScriptConfigItem.of(
                    scriptExecutionRequest,
                    getOrigin().child(IterativeScriptingWorkDefinitionType.F_SCRIPT_EXECUTION_REQUEST));
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
            DebugUtil.debugDumpWithLabel(sb, "scriptExecutionRequest", String.valueOf(scriptExecutionRequest), indent+1);
        }
    }
}
