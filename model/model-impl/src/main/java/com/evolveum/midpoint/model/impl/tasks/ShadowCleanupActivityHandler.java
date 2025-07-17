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

import java.util.Date;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.sync.tasks.ProcessingScope;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
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

    private static final Trace LOGGER = TraceManager.getTrace(ShadowCleanupActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return ShadowCleanupWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull QName getWorkDefinitionItemName() {
        return WorkDefinitionsType.F_SHADOW_CLEANUP;
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
        public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
            if (!super.beforeRun(result)) {
                return false;
            }

            ensureNoPreviewNorDryRun();

            ResourceObjectSetType resourceObjectSet = getWorkDefinition().getResourceObjectSetSpecification();
            RunningTask runningTask = getRunningTask();

            processingScope = getActivityHandler().syncTaskHelper
                    .getProcessingScopeCheckingMaintenance(resourceObjectSet, runningTask, result);
            processingScope.checkResourceUp();

            return true;
        }

        @Override
        protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
            return processingScope.getResourceRef();
        }

        @Override
        public void customizeQuery(SearchSpecification<ShadowType> searchSpecification, OperationResult result) {

            Duration notUpdatedDuration = getWorkDefinition().getInterval();
            Date deletingDate = new Date(getActivityHandler().clock.currentTimeMillis());
            notUpdatedDuration.addTo(deletingDate);

            PrismContext prismContext = getActivityHandler().prismContext;

            ObjectFilter syncTimestampFilter = prismContext.queryFor(ShadowType.class)
                    .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(createXMLGregorianCalendar(deletingDate))
                    .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                    .buildFilter();

            LOGGER.trace("Using sync timestamp filter:\n{}", syncTimestampFilter.debugDumpLazily());
            searchSpecification.addFilter(syncTimestampFilter);
        }

        @Override
        public void customizeSearchOptions(SearchSpecification<ShadowType> searchSpecification, OperationResult result) {
            searchSpecification.setSearchOptions(
                    GetOperationOptions.updateToNoFetch(
                            searchSpecification.getSearchOptions()));
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

        /** Mutable, disconnected from the source. */
        @NotNull private final ResourceObjectSetType shadows;
        @NotNull private final Duration interval;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
            var typedDefinition = (ShadowCleanupWorkDefinitionType) info.getBean();

            shadows = ResourceObjectSetUtil.fromConfiguration(typedDefinition.getShadows());
            ResourceObjectSetUtil.setDefaultQueryApplicationMode(shadows, APPEND); // "replace" would be very dangerous

            interval = typedDefinition.getInterval();
            argCheck(interval != null, "No freshness interval specified");
            if (interval.getSign() == 1) {
                interval.negate();
            }
        }

        @Override
        public @NotNull ResourceObjectSetType getResourceObjectSetSpecification() {
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
