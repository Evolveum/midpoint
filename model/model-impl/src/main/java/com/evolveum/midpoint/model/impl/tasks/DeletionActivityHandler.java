/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.fromModelExecutionOptionsType;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.run.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
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

    private static final Trace LOGGER = TraceManager.getTrace(DeletionActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return DeletionWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull QName getWorkDefinitionItemName() {
        return WorkDefinitionsType.F_DELETION;
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

        /**
         * Execute options that will be used. They differ from the configured option in that the raw mode
         * is applied:
         *
         * - in legacy case the value from `optionRaw` (default: `true`) is used,
         * - in modern case the default of `true` is used.
         *
         * (Note that similar structure for the search options is not stored here. The search options
         * are managed by the activity framework itself. See {@link #customizeSearchOptions(SearchSpecification, OperationResult)}.
         * But the principle of managing raw value in search is the same.)
         */
        private ModelExecuteOptions effectiveModelExecuteOptions;

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

        /** Checks the safety of parameters, and fills-in fields in this object. */
        @Override
        public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
            if (!super.beforeRun(result)) {
                return false;
            }

            executeSafetyChecks();
            effectiveModelExecuteOptions = getExecuteOptionsWithRawSet(getWorkDefinition().executionOptions);

            return true;
        }

        /**
         * Safety checking: Object deletion can be really dangerous. So let us minimize the chance of user error
         * by checking the settings.
         */
        private void executeSafetyChecks() {
            ObjectSetType objects = getWorkDefinition().getObjectSetSpecification();
            argCheck(objects.getType() != null, "Object type must be specified (this is a safety check)");
            argCheck(objects.getQuery() != null, "Object query must be specified (this is a safety check)");
            // We could later support also explicit object references here
            checkRawModeSettings();
        }

        /**
         * Before 4.4, the raw mode for both searching and execution was driven by an extension property value `optionRaw`.
         * (Now mapped to `legacyRawMode`, and always either true or false in the legacy mode.)
         *
         * In 4.4 this property is no longer available. Instead, explicit search and execution options can be set.
         * The default mode for both is raw. And, to minimize chances of user error, we require that either both
         * are specified, or both are left as default (true).
         */
        private void checkRawModeSettings() {
            Boolean rawInSearch =
                    getRaw(GetOperationOptionsUtil.optionsBeanToOptions(
                            getWorkDefinition().objects.getSearchOptions()));

            Boolean rawInExecution =
                    ModelExecuteOptions.getRaw(
                            getWorkDefinition().executionOptions);

            argCheck(rawInSearch != null && rawInExecution != null || rawInSearch == null && rawInExecution == null,
                    "Neither both search and execution raw mode should be defined, or none "
                            + "(this is a safety check)");
        }

        private @Nullable Boolean getRaw(@Nullable Collection<SelectorOptions<GetOperationOptions>> searchOptions) {
            return GetOperationOptions.getRaw(
                    SelectorOptions.findRootOptions(searchOptions));
        }

        @Override
        public void customizeSearchOptions(SearchSpecification<ObjectType> searchSpecification, OperationResult result) {
            searchSpecification.setSearchOptions(
                    getSearchOptionsWithRawSet(
                            searchSpecification.getSearchOptions()));
        }

        /** Returns search options that have `raw` option set (provided or default). */
        private Collection<SelectorOptions<GetOperationOptions>> getSearchOptionsWithRawSet(
                @Nullable Collection<SelectorOptions<GetOperationOptions>> configuredOptions) {
            if (getRaw(configuredOptions) != null) {
                return configuredOptions;
            } else {
                return GetOperationOptions.updateToRaw(configuredOptions, true);
            }
        }

        /** Returns execute options that have `raw` option set (provided or default). */
        private ModelExecuteOptions getExecuteOptionsWithRawSet(@NotNull ModelExecuteOptions executeOptions) {
            if (executeOptions.getRaw() != null) {
                return executeOptions;
            } else {
                return executeOptions.clone()
                        .raw(true);
            }
        }

        @Override
        public boolean processItem(
                @NotNull ObjectType object,
                @NotNull ItemProcessingRequest<ObjectType> request,
                RunningTask workerTask,
                OperationResult result)
                throws CommonException {

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

            if (!isRawExecution() && isProtectedShadow(object)) {
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
                    List.of(delta), effectiveModelExecuteOptions, workerTask, result);
        }

        private boolean isProtectedShadow(ObjectType object) {
            return object instanceof ShadowType &&
                    ShadowUtil.isProtected((ShadowType) object);
        }

        boolean isRawExecution() {
            // We assume that the raw flag is set (true or false) at this moment.
            return Objects.requireNonNull(
                    effectiveModelExecuteOptions.getRaw());
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;
        @NotNull private final ModelExecuteOptions executionOptions;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
            var typedDefinition = (DeletionWorkDefinitionType) info.getBean();
            objects = ObjectSetUtil.emptyIfNull(typedDefinition.getObjects()); // Can contain search options.
            executionOptions = Objects.requireNonNullElseGet(
                    fromModelExecutionOptionsType(typedDefinition.getExecutionOptions()),
                    ModelExecuteOptions::create);
            // Intentionally not setting default object type nor query. These must be defined.
            // Corresponding safety checks are done before real execution.
        }

        @Override
        public @NotNull ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects (default for raw not yet applied)", objects, indent+1);
            DebugUtil.debugDumpWithLabel(sb, "executionOptions (default for raw not yet applied)",
                    String.valueOf(executionOptions), indent+1);
        }
    }
}
