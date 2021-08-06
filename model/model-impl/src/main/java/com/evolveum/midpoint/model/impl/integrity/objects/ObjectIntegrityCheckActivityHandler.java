
package com.evolveum.midpoint.model.impl.integrity.objects;

import java.util.Collection;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.task.BaseSearchBasedExecutionSpecificsImpl;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution.SearchBasedSpecificsSupplier;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

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
            ObjectIntegrityCheckActivityHandler> {

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
    protected @NotNull SearchBasedSpecificsSupplier<ObjectType, MyWorkDefinition, ObjectIntegrityCheckActivityHandler> getSpecificSupplier() {
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
        return "Object integrity check";
    }

    @Override
    public String getIdentifierPrefix() {
        return "object-integrity-check";
    }

    static class MyExecutionSpecifics extends
            BaseSearchBasedExecutionSpecificsImpl<ObjectType, MyWorkDefinition, ObjectIntegrityCheckActivityHandler> {

        final ObjectStatistics objectStatistics = new ObjectStatistics();

        MyExecutionSpecifics(
                @NotNull SearchBasedActivityExecution<ObjectType, MyWorkDefinition, ObjectIntegrityCheckActivityHandler, ?> activityExecution) {
            super(activityExecution);
        }

        @Override
        public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
            return super.getDefaultReportingOptions()
                    .logErrors(false) // we do log errors ourselves
                    .skipWritingOperationExecutionRecords(true); // because of performance
        }

        @Override
        public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
                Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result)
                throws CommonException {
            return SelectorOptions.updateRootOptions(configuredOptions, opt -> opt.setAttachDiagData(true), GetOperationOptions::new);
        }

        @Override
        public boolean doesRequireDirectRepositoryAccess() {
            return true;
        }

        @Override
        public void beforeExecution(OperationResult result) {
            activityExecution.ensureNoWorkerThreads();
        }

        @Override
        public void afterExecution(OperationResult result) throws ActivityExecutionException, CommonException {
            getActivityHandler().dumpStatistics(
                    objectStatistics,
                    getWorkDefinition().histogramColumns);
        }

        @Override
        public boolean processObject(@NotNull PrismObject<ObjectType> object,
                @NotNull ItemProcessingRequest<PrismObject<ObjectType>> request, RunningTask workerTask, OperationResult parentResult)
                throws CommonException, ActivityExecutionException {
            OperationResult result = parentResult.createMinorSubresult(OP_PROCESS_ITEM);
            try {
                objectStatistics.record(object);
            } catch (RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Unexpected error while checking object {} integrity", e, ObjectTypeUtil.toShortString(object));
                result.recordPartialError("Unexpected error while checking object integrity", e);
                objectStatistics.incrementObjectsWithErrors();
            }

            result.computeStatusIfUnknown();
            return true;
        }
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
