/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.fromModelExecutionOptionsType;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Recomputes specified objects.
 */
@Component
public class RecomputationActivityHandler
        extends SimpleActivityHandler<
            ObjectType,
            RecomputationActivityHandler.MyWorkDefinition,
            RecomputationActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(RecomputationActivityHandler.class);

    private static final QName DEFAULT_OBJECT_TYPE_FOR_NEW_SPEC = FocusType.COMPLEX_TYPE; // This is more reasonable

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return RecomputationWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull QName getWorkDefinitionItemName() {
        return WorkDefinitionsType.F_RECOMPUTATION;
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
    protected @NotNull ExecutionSupplier<ObjectType, MyWorkDefinition, RecomputationActivityHandler> getExecutionSupplier() {
        return MyRun::new;
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value();
    }

    @Override
    protected @NotNull String getShortName() {
        return "Recomputation";
    }

    @Override
    public String getIdentifierPrefix() {
        return "recomputation";
    }

    static final class MyRun extends
            SearchBasedActivityRun<ObjectType, MyWorkDefinition, RecomputationActivityHandler, AbstractActivityWorkStateType> {

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, RecomputationActivityHandler> context,
                String shortName) {
            super(context, shortName);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .actionsExecutedStatisticsSupported(true);
        }

        @Override
        public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
            if (!super.beforeRun(result)) {
                return false;
            }
            ensureNoDryRun();
            return true;
        }

        @Override
        public boolean processItem(@NotNull ObjectType object,
                @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult result)
                throws CommonException {
            getActivityHandler().modelController.executeRecompute(
                    object.asPrismObject(), getWorkDefinition().getExecutionOptions(), workerTask, result);
            LOGGER.trace("Recomputation of object {}: {}", object, result.getStatus());
            return true;
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;
        @NotNull private final ModelExecuteOptions executionOptions;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
            var typedDefinition = (RecomputationWorkDefinitionType) info.getBean();
            objects = ObjectSetUtil.emptyIfNull(typedDefinition.getObjects());
            ObjectSetUtil.applyDefaultObjectType(objects, DEFAULT_OBJECT_TYPE_FOR_NEW_SPEC);
            executionOptions = java.util.Objects.requireNonNullElseGet(
                    fromModelExecutionOptionsType(typedDefinition.getExecutionOptions()),
                    () -> ModelExecuteOptions.create().reconcile()); // Default for compatibility reasons
        }

        @Override
        public @NotNull ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        @NotNull ModelExecuteOptions getExecutionOptions() {
            return executionOptions;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
            DebugUtil.debugDumpWithLabel(sb, "executionOptions", String.valueOf(executionOptions), indent+1);
        }
    }
}
