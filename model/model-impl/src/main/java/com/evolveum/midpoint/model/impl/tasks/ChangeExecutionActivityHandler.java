/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.fromModelExecutionOptionsType;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Collections;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.task.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution.SearchBasedSpecificsSupplier;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ChangeExecutionWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Executes specified deltas on specified set of objects.
 */
@Component
public class ChangeExecutionActivityHandler
        extends SimpleActivityHandler<
            ObjectType,
            ChangeExecutionActivityHandler.MyWorkDefinition,
            ChangeExecutionActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.EXECUTE_CHANGES_TASK_HANDLER_URI;
    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutionActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return ChangeExecutionWorkDefinitionType.COMPLEX_TYPE;
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
    protected @NotNull SearchBasedSpecificsSupplier<ObjectType, MyWorkDefinition, ChangeExecutionActivityHandler> getSpecificSupplier() {
        return MyExecutionSpecifics::new;
    }

    @Override
    protected @NotNull String getLegacyHandlerUri() {
        return LEGACY_HANDLER_URI;
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    protected @NotNull String getShortName() {
        return "Change execution";
    }

    @Override
    public String getIdentifierPrefix() {
        return "change-execution";
    }

    static class MyExecutionSpecifics extends
            BaseSearchBasedExecutionSpecificsImpl<ObjectType, MyWorkDefinition, ChangeExecutionActivityHandler> {

        MyExecutionSpecifics(@NotNull SearchBasedActivityExecution<ObjectType, MyWorkDefinition, ChangeExecutionActivityHandler, ?> activityExecution) {
            super(activityExecution);
        }

        @Override
        public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
            return super.getDefaultReportingOptions()
                    .enableActionsExecutedStatistics(true);
        }

        @Override
        public boolean processObject(@NotNull PrismObject<ObjectType> object,
                @NotNull ItemProcessingRequest<PrismObject<ObjectType>> request, RunningTask workerTask, OperationResult result)
                throws CommonException, ActivityExecutionException {
            LOGGER.trace("Executing change on object {}", object);

            IterativeActivityExecution<PrismObject<ObjectType>, ?, ?, ?, ?, ?> activityExecution = request.getActivityExecution();
            MyWorkDefinition workDefinition =
                    (MyWorkDefinition) activityExecution.getActivity().getWorkDefinition();

            PrismContext prismContext = getActivityHandler().prismContext;
            ObjectDelta<ObjectType> delta = DeltaConvertor.createObjectDelta(workDefinition.getDelta(), prismContext);
            delta.setOid(object.getOid());
            if (object.getCompileTimeClass() != null) {
                delta.setObjectTypeClass(object.getCompileTimeClass());
            }
            prismContext.adopt(delta);

            getActivityHandler().modelController.executeChanges(
                    Collections.singletonList(delta),
                    workDefinition.getExecutionOptions(),
                    workerTask, result);
            LOGGER.trace("Execute changes {} for object {}: {}", delta, object, result.getStatus());
            return true;
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        private final ObjectSetType objects;
        private final ObjectDeltaType delta;
        private final ModelExecuteOptions executionOptions;

        MyWorkDefinition(WorkDefinitionSource source) {
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
                objects = ObjectSetUtil.fromLegacySource(legacy);
                delta = legacy.getExtensionItemRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_DELTA, ObjectDeltaType.class);
                executionOptions = ModelImplUtils.getModelExecuteOptions(legacy.getTaskExtension());
            } else {
                ChangeExecutionWorkDefinitionType typedDefinition = (ChangeExecutionWorkDefinitionType)
                        ((WorkDefinitionWrapper.TypedWorkDefinitionWrapper) source).getTypedDefinition();
                objects = typedDefinition.getObjects();
                delta = typedDefinition.getDelta();
                executionOptions = fromModelExecutionOptionsType(typedDefinition.getExecutionOptions());
            }

            argCheck(delta != null, "No delta specified");
        }

        @Override
        public ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        public ObjectDeltaType getDelta() {
            return delta;
        }

        public ModelExecuteOptions getExecutionOptions() {
            return executionOptions;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent+1);
            DebugUtil.debugDumpWithLabelLn(sb, "delta", String.valueOf(delta), indent+1);
            DebugUtil.debugDumpWithLabelLn(sb, "executionOptions", String.valueOf(executionOptions), indent+1);
        }
    }
}
