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
import com.evolveum.midpoint.model.impl.sync.tasks.ResourceObjectClassSpecification;
import com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityExecution;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowCleanupWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 */
@Component
public class ShadowCleanupActivityHandler
        extends SimpleActivityHandler<
        ShadowType,
        ShadowCleanupActivityHandler.MyWorkDefinition,
        ShadowCleanupActivityHandler.MyExecutionContext> {

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
    protected @NotNull String getLegacyHandlerUri() {
        return LEGACY_HANDLER_URI;
    }

    @Override
    protected @NotNull String getShortName() {
        return "Shadow cleanup";
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .enableActionsExecutedStatistics(true)
                .skipWritingOperationExecutionRecords(true); // because the shadows are deleted anyway
    }

    @Override
    public MyExecutionContext createExecutionContext(
            SimpleActivityExecution<ShadowType, MyWorkDefinition, MyExecutionContext> execution,
            OperationResult opResult) throws ActivityExecutionException, CommonException {

        ResourceObjectSetType resourceObjectSet = execution.getWorkDefinition().getResourceObjectSetSpecification();
        RunningTask runningTask = execution.getRunningTask();

        ResourceObjectClassSpecification objectClassSpec = getModelBeans().syncTaskHelper
                .createObjectClassSpec(resourceObjectSet, runningTask, opResult);

        objectClassSpec.checkNotInMaintenance();
        objectClassSpec.checkResourceUp();
        return new MyExecutionContext(objectClassSpec);
    }

    @Override
    public ObjectQuery customizeQuery(
            @NotNull SimpleActivityExecution<ShadowType, MyWorkDefinition, MyExecutionContext> activityExecution,
            ObjectQuery configuredQuery, OperationResult opResult) {

        Duration notUpdatedDuration = activityExecution.getWorkDefinition().getInterval();
        Date deletingDate = new Date(clock.currentTimeMillis());
        notUpdatedDuration.addTo(deletingDate);

        ObjectFilter syncTimestampFilter = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).le(XmlTypeConverter.createXMLGregorianCalendar(deletingDate))
                .or().item(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP).isNull()
                .buildFilter();

        ObjectQuery fullQuery = ObjectQueryUtil.addConjunctions(configuredQuery, prismContext, syncTimestampFilter);

        LOGGER.trace("Query with sync timestamp filter:\n{}", fullQuery.debugDumpLazily());
        return fullQuery;
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            @NotNull SimpleActivityExecution<ShadowType, MyWorkDefinition, MyExecutionContext> activityExecution,
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult opResult) throws CommonException {

        return GetOperationOptions.updateToNoFetch(configuredOptions);
    }

    @Override
    public boolean processItem(PrismObject<ShadowType> shadow, ItemProcessingRequest<PrismObject<ShadowType>> request,
            SimpleActivityExecution<ShadowType, MyWorkDefinition, MyExecutionContext> activityExecution,
            RunningTask workerTask, OperationResult result) throws CommonException {
        deleteShadow(shadow, activityExecution.getExecutionContext(), workerTask, result);
        return true;
    }

    private void deleteShadow(PrismObject<ShadowType> shadow, MyExecutionContext executionContext,
            Task workerTask, OperationResult result) {
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setObjectDelta(shadow.createDeleteDelta());
        change.setResource(executionContext.getObjectClassSpec().getResource().asPrismObject());
        change.setShadowedResourceObject(shadow);
        change.setSourceChannel(SchemaConstants.CHANNEL_CLEANUP_URI);
        synchronizationService.notifyChange(change, workerTask, result);
    }

    public static class MyExecutionContext extends ExecutionContext {

        @NotNull private final ResourceObjectClassSpecification resourceObjectClassSpecification;

        MyExecutionContext(@NotNull ResourceObjectClassSpecification resourceObjectClassSpecification) {
            this.resourceObjectClassSpecification = resourceObjectClassSpecification;
        }

        public @NotNull ResourceObjectClassSpecification getObjectClassSpec() {
            return resourceObjectClassSpecification;
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ResourceObjectSetSpecificationProvider {

        private final ResourceObjectSetType shadows;
        @NotNull private final Duration interval;

        MyWorkDefinition(WorkDefinitionSource source) {
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacy = (LegacyWorkDefinitionSource) source;
                shadows = ResourceObjectSetUtil.fromLegacySource(legacy);
                interval = legacy.getExtensionItemRealValue(SchemaConstants.LEGACY_NOT_UPDATED_DURATION_PROPERTY_NAME, Duration.class);
            } else {
                ShadowCleanupWorkDefinitionType typedDefinition = (ShadowCleanupWorkDefinitionType)
                        ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
                shadows = typedDefinition.getShadows();
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
            DebugUtil.debugDumpWithLabelLn(sb, "shadows", shadows, indent+1);
            DebugUtil.debugDumpWithLabelLn(sb, "interval", String.valueOf(interval), indent+1);
        }
    }
}
