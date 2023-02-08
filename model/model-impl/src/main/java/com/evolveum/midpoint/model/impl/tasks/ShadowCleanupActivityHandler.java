/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
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
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The original idea behind this activity was to treat shadows on (asynchronous) Kafka resources that did not support
 * "read" operation (or did that in a very limited way). So the only way how to know what shadows are really existing
 * was to send regular account update events that would keep "fullSynchronizationTimestamp" up to date. Shadows that
 * were not updated were considered to be dead.
 *
 * However, such an approach is a bit brittle. In particular, if used for a regular resource, it may be possible that
 * such a shadow really exists. Hence, in 4.7 the behavior was changed to call explicit provisioning "getObject"
 * operation instead of simply assuming the shadow is gone. This conflicts with the original use case, and if that
 * should be usable again, the code would need to be improved somehow.
 *
 * TODO Decide on the fate of this activity (MID-8350)
 *
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

    public final class MyRun extends
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

            ensureNoPreviewNorDryRun();

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
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(createXMLGregorianCalendar(deletingDate))
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
                @NotNull ItemProcessingRequest<ShadowType> request, RunningTask workerTask, OperationResult result)
                throws CommonException {
            var options = GetOperationOptionsBuilder.create()
                    .forceRefresh()
                    .forceRetry()
                    .allowNotFound()
                    .build();
            try {
                // TODO what if the resource does not support "read" capability?
                provisioningService.getObject(ShadowType.class, shadow.getOid(), options, workerTask, result);
                // The "shadow dead" or even "shadow deleted" event should be emitted by the provisioning service
            } catch (ObjectNotFoundException e) {
                LOGGER.trace("Shadow is no longer there - OK, that makes sense: {}", shadow);
            }
            return true;
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
            DebugUtil.debugDumpWithLabel(sb, "interval", String.valueOf(interval), indent + 1);
        }
    }
}
