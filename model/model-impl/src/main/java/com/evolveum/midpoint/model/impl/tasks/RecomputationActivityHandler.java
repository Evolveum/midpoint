/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.fromModelExecutionOptionsType;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
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

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.RECOMPUTE_HANDLER_URI;
    private static final Trace LOGGER = TraceManager.getTrace(RecomputationActivityHandler.class);

    private static final QName DEFAULT_OBJECT_TYPE_FOR_LEGACY_SPEC = UserType.COMPLEX_TYPE;  // This is pre-4.4 behavior
    private static final QName DEFAULT_OBJECT_TYPE_FOR_NEW_SPEC = FocusType.COMPLEX_TYPE; // This is more reasonable

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return RecomputationWorkDefinitionType.COMPLEX_TYPE;
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
    protected @NotNull String getLegacyHandlerUri() {
        return LEGACY_HANDLER_URI;
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
        public boolean processItem(@NotNull ObjectType object,
                @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult result)
                throws CommonException, ActivityRunException {
            boolean simulate = isPreview();
            String action = simulate ? "Simulated recomputation" : "Recomputation";

            if (simulate) {
                LOGGER.trace("{} of object {}", action, object);

                LensContext<FocusType> syncContext = getActivityHandler().contextFactory.createRecomputeContext(
                        object.asPrismObject(), getWorkDefinition().getExecutionOptions(), workerTask, result);
                getActivityHandler().clockwork.previewChanges(syncContext, null, workerTask, result);

                LOGGER.trace("{} of object {}: context:\n{}", action, object, syncContext.debugDumpLazily());
            } else {
                getActivityHandler().modelController.executeRecompute(
                        object.asPrismObject(), getWorkDefinition().getExecutionOptions(), workerTask, result);
            }
            LOGGER.trace("{} of object {}: {}", action, object, result.getStatus());
            return true;
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;
        @NotNull private final ModelExecuteOptions executionOptions;

        MyWorkDefinition(WorkDefinitionSource source) {
            ModelExecuteOptions rawExecutionOptions;
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
                objects = ObjectSetUtil.fromLegacySource(legacy);
                rawExecutionOptions = ModelImplUtils.getModelExecuteOptions(legacy.getTaskExtension());
                ObjectSetUtil.applyDefaultObjectType(objects, DEFAULT_OBJECT_TYPE_FOR_LEGACY_SPEC);
            } else {
                RecomputationWorkDefinitionType typedDefinition = (RecomputationWorkDefinitionType)
                        ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
                objects = ObjectSetUtil.fromConfiguration(typedDefinition.getObjects());
                rawExecutionOptions = fromModelExecutionOptionsType(typedDefinition.getExecutionOptions());
                ObjectSetUtil.applyDefaultObjectType(objects, DEFAULT_OBJECT_TYPE_FOR_NEW_SPEC);
            }
            executionOptions = java.util.Objects.requireNonNullElseGet(
                    rawExecutionOptions,
                    () -> ModelExecuteOptions.create(PrismContext.get()).reconcile()); // Default for compatibility reasons
        }

        @Override
        public ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        public @NotNull ModelExecuteOptions getExecutionOptions() {
            return executionOptions;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
            DebugUtil.debugDumpWithLabelLn(sb, "executionOptions", String.valueOf(executionOptions), indent+1);
        }
    }
}
