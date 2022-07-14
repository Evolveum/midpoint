/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.APPEND;

import java.util.Collection;
import java.util.Date;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class ShadowCleanupActivityHandler
        extends SimpleActivityHandler<
        ShadowType,
        ShadowCleanupActivityHandler.MyWorkDefinition,
        ShadowCleanupActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.DELETE_NOT_UPDATE_SHADOW_TASK_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowCleanupActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return ShadowCleanupWorkDefinitionType.COMPLEX_TYPE;
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
    protected @NotNull ExecutionSupplier<ShadowType, MyWorkDefinition, ShadowCleanupActivityHandler> getExecutionSupplier() {
        return MyRun::new;
    }

    @Override
    protected @NotNull String getLegacyHandlerUri() {
        return LEGACY_HANDLER_URI;
    }

    @Override // TODO or should the archetype be "cleanup"?
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    protected @NotNull String getShortName() {
        return "Shadow cleanup";
    }

    @Override
    public String getIdentifierPrefix() {
        return "shadow-cleanup";
    }

    public static final class MyRun extends
            SearchBasedActivityRun<ShadowType, MyWorkDefinition, ShadowCleanupActivityHandler, AbstractActivityWorkStateType> {

        private ProcessingScope processingScope;

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, ShadowCleanupActivityHandler> context,
                String shortName) {
            super(context, shortName);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .actionsExecutedStatisticsSupported(true)
                    .skipWritingOperationExecutionRecords(true); // because the shadows are deleted anyway
        }

        @Override
        public void beforeRun(OperationResult result) throws ActivityRunException, CommonException {

            ResourceObjectSetType resourceObjectSet = getWorkDefinition().getResourceObjectSetSpecification();
            RunningTask runningTask = getRunningTask();

            processingScope = getActivityHandler().syncTaskHelper
                    .getProcessingScopeCheckingMaintenance(resourceObjectSet, runningTask, result);
            processingScope.checkResourceUp();
        }

        @Override
        protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
            return processingScope.getResourceRef();
        }

        @Override
        public ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult result) {

            Duration notUpdatedDuration = getWorkDefinition().getInterval();
            Date deletingDate = new Date(getActivityHandler().clock.currentTimeMillis());
            notUpdatedDuration.addTo(deletingDate);

            PrismContext prismContext = getActivityHandler().prismContext;

            ObjectFilter syncTimestampFilter = prismContext.queryFor(ShadowType.class)
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(XmlTypeConverter.createXMLGregorianCalendar(deletingDate))
                    .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                    .buildFilter();

            ObjectQuery fullQuery = ObjectQueryUtil.addConjunctions(configuredQuery, syncTimestampFilter);

            LOGGER.trace("Query with sync timestamp filter:\n{}", fullQuery.debugDumpLazily());
            return fullQuery;
        }

        @Override
        public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
                Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result) {
            return GetOperationOptions.updateToNoFetch(configuredOptions);
        }

        @Override
        public boolean processItem(@NotNull ShadowType shadow,
                @NotNull ItemProcessingRequest<ShadowType> request, RunningTask workerTask, OperationResult result) {
            deleteShadow(shadow.asPrismObject(), workerTask, result);
            return true;
        }

        private void deleteShadow(PrismObject<ShadowType> shadow, Task workerTask, OperationResult result) {
            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setObjectDelta(shadow.createDeleteDelta());
            change.setResource(processingScope.getResource().asPrismObject());
            change.setShadowedResourceObject(shadow);
            change.setSourceChannel(SchemaConstants.CHANNEL_CLEANUP_URI);
            getActivityHandler().synchronizationService.notifyChange(change, workerTask, result);
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ResourceObjectSetSpecificationProvider {

        private final ResourceObjectSetType shadows;
        @NotNull private final Duration interval;

        MyWorkDefinition(WorkDefinitionSource source) {
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
                shadows = ResourceObjectSetUtil.fromLegacySource(legacy);
                interval = legacy.getExtensionItemRealValue(
                        SchemaConstants.LEGACY_NOT_UPDATED_DURATION_PROPERTY_NAME, Duration.class);
            } else {
                ShadowCleanupWorkDefinitionType typedDefinition = (ShadowCleanupWorkDefinitionType)
                        ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
                shadows = ResourceObjectSetUtil.fromConfiguration(typedDefinition.getShadows());
                interval = typedDefinition.getInterval();
            }
            ResourceObjectSetUtil.setDefaultQueryApplicationMode(shadows, APPEND); // "replace" would be very dangerous

            argCheck(interval != null, "No freshness interval specified");
            if (interval.getSign() == 1) {
                interval.negate();
            }
        }

        @Override
        public ResourceObjectSetType getResourceObjectSetSpecification() {
            return shadows;
        }

        public @NotNull Duration getInterval() {
            return interval;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "shadows", shadows, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "interval", String.valueOf(interval), indent + 1);
        }
    }
}
