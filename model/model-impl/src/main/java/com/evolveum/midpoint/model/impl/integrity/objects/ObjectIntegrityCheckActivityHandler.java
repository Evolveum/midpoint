/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.objects;

import java.util.Collection;
import java.util.Map;
import javax.xml.namespace.QName;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.simple.ExecutionContext;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityExecution;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectIntegrityCheckWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Task handler for "Object integrity check" task.
 *
 * The purpose of this task is to detect and optionally fix anomalies in repository objects.
 *
 * However, currently its only function is to display information about objects size.
 */
@Component
public class ObjectIntegrityCheckActivityHandler
        extends SimpleActivityHandler<
        ObjectType,
        ObjectIntegrityCheckActivityHandler.MyWorkDefinition,
        ObjectIntegrityCheckActivityHandler.MyExecutionContext> {

    public static final String LEGACY_HANDLER_URI = ModelPublicConstants.OBJECT_INTEGRITY_CHECK_TASK_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectIntegrityCheckActivityHandler.class);

    private static final int DEFAULT_HISTOGRAM_COLUMNS = 80;

    private static final String OP_PROCESS_ITEM = ObjectIntegrityCheckActivityHandler.class.getName() + ".processItem";

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return ObjectIntegrityCheckWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull Class<MyWorkDefinition> getWorkDefinitionClass() {
        return MyWorkDefinition.class;
    }

    @Override
    protected @NotNull WorkDefinitionFactory.WorkDefinitionSupplier getWorkDefinitionSupplier() {
        return MyWorkDefinition::new;
    }

    @Override
    protected @NotNull String getLegacyHandlerUri() {
        return LEGACY_HANDLER_URI;
    }

    @Override
    protected @NotNull String getShortName() {
        return "Object integrity check";
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return new ActivityReportingOptions()
                .logErrors(false) // we do log errors ourselves
                .skipWritingOperationExecutionRecords(true); // because of performance
    }

    @Override
    public boolean processItem(PrismObject<ObjectType> object, ItemProcessingRequest<PrismObject<ObjectType>> request,
            SimpleActivityExecution<ObjectType, MyWorkDefinition, MyExecutionContext> activityExecution,
            RunningTask workerTask, OperationResult parentResult) throws CommonException, ActivityExecutionException {

        OperationResult result = parentResult.createMinorSubresult(OP_PROCESS_ITEM);
        ObjectStatistics objectStatistics = activityExecution.getExecutionContext().objectStatistics;
        try {
            objectStatistics.record(object);
        } catch (RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error while checking object {} integrity", e, ObjectTypeUtil.toShortString(object));
            result.recordPartialError("Unexpected error while checking object integrity", e);
            objectStatistics.incrementObjectsWithErrors();
        } finally {
            workerTask.markObjectActionExecutedBoundary();
        }

        result.computeStatusIfUnknown();
        return true;
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            @NotNull SimpleActivityExecution<ObjectType, MyWorkDefinition, MyExecutionContext> activityExecution,
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult opResult)
            throws CommonException {
        return SelectorOptions.updateRootOptions(configuredOptions, opt -> opt.setAttachDiagData(true), GetOperationOptions::new);
    }

    @Override
    public boolean doesRequireDirectRepositoryAccess() {
        return true;
    }

    @Override
    public void beforeExecution(
            @NotNull SimpleActivityExecution<ObjectType, MyWorkDefinition, MyExecutionContext> activityExecution,
            OperationResult opResult) {

        activityExecution.ensureNoWorkerThreads();
    }

    @Override
    public void afterExecution(
            @NotNull SimpleActivityExecution<ObjectType, MyWorkDefinition, MyExecutionContext> activityExecution,
            OperationResult opResult) throws ActivityExecutionException, CommonException {
        dumpStatistics(
                activityExecution.getExecutionContext().objectStatistics,
                activityExecution.getWorkDefinition().histogramColumns);
    }

    private void dumpStatistics(ObjectStatistics objectStatistics, int histogramColumns) {
        Map<String, ObjectTypeStatistics> map = objectStatistics.getStatisticsMap();
        if (map.isEmpty()) {
            LOGGER.info("(no objects were found)");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, ObjectTypeStatistics> entry : map.entrySet()) {
                sb.append("\n\n**************************************** Statistics for ").append(entry.getKey()).append(" ****************************************\n\n");
                sb.append(entry.getValue().dump(histogramColumns));
            }
            LOGGER.info("{}", sb);
        }
        LOGGER.info("Objects processed with errors: {}", objectStatistics.getErrors());
    }

    static class MyExecutionContext extends ExecutionContext {

        final ObjectStatistics objectStatistics = new ObjectStatistics();

    }

    static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;
        private final int histogramColumns;

        MyWorkDefinition(WorkDefinitionSource source) {
            if (source instanceof LegacyWorkDefinitionSource) {
                LegacyWorkDefinitionSource legacySource = (LegacyWorkDefinitionSource) source;
                objects = ObjectSetUtil.fromLegacySource(legacySource);
                histogramColumns = DEFAULT_HISTOGRAM_COLUMNS;
            } else {
                ObjectIntegrityCheckWorkDefinitionType typedDefinition = (ObjectIntegrityCheckWorkDefinitionType)
                        ((WorkDefinitionWrapper.TypedWorkDefinitionWrapper) source).getTypedDefinition();
                objects = typedDefinition.getObjects() != null ?
                        typedDefinition.getObjects() : new ObjectSetType(PrismContext.get());
                histogramColumns = MoreObjects.firstNonNull(typedDefinition.getHistogramColumns(), DEFAULT_HISTOGRAM_COLUMNS);
            }
        }

        @Override
        public ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        public int getHistogramColumns() {
            return histogramColumns;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "histogramColumns", histogramColumns, indent + 1);
        }
    }
}
