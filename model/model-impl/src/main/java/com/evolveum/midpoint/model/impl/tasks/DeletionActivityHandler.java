/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.fromModelExecutionOptionsType;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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
 * Deletes specified objects.
 */
@Component
public class DeletionActivityHandler
        extends SimpleActivityHandler<
            ObjectType,
            DeletionActivityHandler.MyWorkDefinition,
        DeletionActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.DELETE_TASK_HANDLER_URI;
    private static final Trace LOGGER = TraceManager.getTrace(DeletionActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return DeletionWorkDefinitionType.COMPLEX_TYPE;
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
    protected @NotNull ExecutionSupplier<ObjectType, MyWorkDefinition, DeletionActivityHandler> getExecutionSupplier() {
        return MyRun::new;
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
        return "Deletion";
    }

    @Override
    public String getIdentifierPrefix() {
        return "deletion";
    }

    static final class MyRun extends
            SearchBasedActivityRun<ObjectType, MyWorkDefinition, DeletionActivityHandler, AbstractActivityWorkStateType> {

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, DeletionActivityHandler> context,
                String shortName) {
            super(context, shortName);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .skipWritingOperationExecutionRecords(true); // objects are going to be deleted anyway
        }

        @Override
        public void beforeRun(OperationResult opResult) throws CommonException, ActivityRunException {
            ObjectSetType objects = getWorkDefinition().getObjectSetSpecification();
            argCheck(objects.getType() != null, "Object type must be specified (this is a safety check)");
            argCheck(objects.getQuery() != null, "Object query must be specified (this is a safety check)");
        }

        @Override
        public boolean processItem(@NotNull ObjectType object,
                @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult result)
                throws CommonException, ActivityRunException {

            if (isFullExecution()) {
                deleteObject(object, workerTask, result);
            } else {
                // All objects will be skipped in non-full exec mode.
                result.recordNotApplicable("Deletion in a mode other than full execution is not supported");
            }
            return true;
        }

        private void deleteObject(ObjectType object, RunningTask workerTask, OperationResult result)
                throws CommonException {

            // The model would reject deletion of these objects anyway. But checking here may be better
            // (e.g. no audit log records, etc).
            if (ObjectTypeUtil.isIndestructible(object)) {
                LOGGER.debug("Skipping deletion of indestructible object {}", object);
                result.recordNotApplicable("This is an indestructible object");
                return;
            }

            if (!getWorkDefinition().isRawExecution() && isProtectedShadow(object)) {
                // Protected objects will be prevented from the deletion on the resource
                // (that is what would occur in non-raw mode), so it's logical to not attempt that at all.
                // We'll spare some error messages, and (potential) problems with shadows.
                LOGGER.debug("Skipping deletion of protected object in non-raw mode: {}", object);
                result.recordNotApplicable("This is a protected shadow");
                return;
            }

            ObjectDelta<? extends ObjectType> delta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(object.getClass(), object.getOid());

            getActivityHandler().modelService.executeChanges(
                    List.of(delta), getWorkDefinition().executionOptions, workerTask, result);
        }

        private boolean isProtectedShadow(ObjectType object) {
            return object instanceof ShadowType &&
                    ShadowUtil.isProtected((ShadowType) object);
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;
        @NotNull private final ModelExecuteOptions executionOptions;

        MyWorkDefinition(WorkDefinitionSource source) {
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
                objects = ObjectSetUtil.fromLegacySource(legacy);
                boolean raw = Objects.requireNonNullElse(
                        legacy.getExtensionItemRealValue(SchemaConstants.MODEL_EXTENSION_OPTION_RAW, Boolean.class),
                        true); // Default is intentionally true
                executionOptions = ModelExecuteOptions.create().raw(raw);
            } else {
                DeletionWorkDefinitionType typedDefinition = (DeletionWorkDefinitionType)
                        ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
                objects = ObjectSetUtil.fromConfiguration(typedDefinition.getObjects());
                executionOptions = Objects.requireNonNullElseGet(
                        fromModelExecutionOptionsType(typedDefinition.getExecutionOptions()),
                        () -> ModelExecuteOptions.create().raw()); // Here the default is raw=true as well.
            }
            // Intentionally not setting default object type nor query. These must be defined.
        }

        boolean isRawExecution() {
            return ModelExecuteOptions.isRaw(executionOptions);
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
