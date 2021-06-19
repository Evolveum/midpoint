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

import com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext;

import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityExecution;

import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.ModelConstants;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.task.AbstractIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
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
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Executes specified deltas on specified set of objects.
 */
@Component
public class ChangeExecutionActivityHandler
        extends SimpleActivityHandler<ObjectType, ChangeExecutionActivityHandler.ChangeExecutionWorkDefinition, ExecutionContext> {

    private static final String LEGACY_HANDLER_URI = ModelConstants.NS_SYNCHRONIZATION_TASK_PREFIX + "/execute/handler-3";
    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutionActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return ChangeExecutionWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull Class<ChangeExecutionWorkDefinition> getWorkDefinitionClass() {
        return ChangeExecutionWorkDefinition.class;
    }

    @Override
    protected @NotNull WorkDefinitionSupplier getWorkDefinitionSupplier() {
        return ChangeExecutionWorkDefinition::new;
    }

    @Override
    protected @NotNull String getLegacyHandlerUri() {
        return LEGACY_HANDLER_URI;
    }

    @Override
    protected @NotNull String getShortName() {
        return "Change execution";
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions();
    }

    @Override
    public boolean processItem(PrismObject<ObjectType> object, ItemProcessingRequest<PrismObject<ObjectType>> request,
            SimpleActivityExecution<ObjectType, ChangeExecutionWorkDefinition, ExecutionContext> ignored,
            RunningTask workerTask, OperationResult result) throws CommonException {
        LOGGER.trace("Executing change on object {}", object);

        AbstractIterativeActivityExecution<PrismObject<ObjectType>, ?, ?, ?> activityExecution = request.getActivityExecution();
        ChangeExecutionWorkDefinition workDefinition =
                (ChangeExecutionWorkDefinition) activityExecution.getActivity().getWorkDefinition();

        ObjectDelta<ObjectType> delta = DeltaConvertor.createObjectDelta(workDefinition.getDelta(), prismContext);
        delta.setOid(object.getOid());
        if (object.getCompileTimeClass() != null) {
            delta.setObjectTypeClass(object.getCompileTimeClass());
        }
        prismContext.adopt(delta);

        modelController.executeChanges(Collections.singletonList(delta), workDefinition.getExecutionOptions(), workerTask, result);
        LOGGER.trace("Execute changes {} for object {}: {}", delta, object, result.getStatus());
        return true;
    }

    public static class ChangeExecutionWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        private final ObjectSetType objects;
        private final ObjectDeltaType delta;
        private final ModelExecuteOptions executionOptions;

        ChangeExecutionWorkDefinition(WorkDefinitionSource source) {
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
