
package com.evolveum.midpoint.model.impl.integrity.objects;

import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.util.exception.CommonException;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    private static final Trace LOGGER = TraceManager.getTrace(ObjectIntegrityCheckActivityHandler.class);

    private static final int DEFAULT_HISTOGRAM_COLUMNS = 80;

    private static final String OP_PROCESS_ITEM = ObjectIntegrityCheckActivityHandler.class.getName() + ".processItem";

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return ObjectIntegrityCheckWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull QName getWorkDefinitionItemName() {
        return WorkDefinitionsType.F_OBJECT_INTEGRITY_CHECK;
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
    protected @NotNull ExecutionSupplier<ObjectType, MyWorkDefinition, ObjectIntegrityCheckActivityHandler> getExecutionSupplier() {
        return MyRun::new;
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

    static final class MyRun extends
            SearchBasedActivityRun<ObjectType, MyWorkDefinition, ObjectIntegrityCheckActivityHandler, AbstractActivityWorkStateType> {

        final ObjectStatistics objectStatistics = new ObjectStatistics();

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, ObjectIntegrityCheckActivityHandler> context,
                String shortName) {
            super(context, shortName);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .logErrors(false) // we do log errors ourselves
                    .skipWritingOperationExecutionRecords(true); // because of performance
        }

        @Override
        public void customizeSearchOptions(
                SearchSpecification<ObjectType> searchSpecification, OperationResult result) {
            searchSpecification.setSearchOptions(
                    SelectorOptions.updateRootOptions(
                            searchSpecification.getSearchOptions(),
                            opt -> opt.setAttachDiagData(true),
                            GetOperationOptions::new));
        }

        @Override
        public boolean doesRequireDirectRepositoryAccess() {
            return true;
        }

        @Override
        public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
            if (!super.beforeRun(result)) {
                return false;
            }
            ensureNoWorkerThreads();
            ensureNoPreviewNorDryRun();
            return true;
        }

        @Override
        public void afterRun(OperationResult result) {
            getActivityHandler().dumpStatistics(
                    objectStatistics,
                    getWorkDefinition().histogramColumns);
        }

        @Override
        public boolean processItem(@NotNull ObjectType object,
                @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult parentResult) {
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

    protected static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectSetType objects;
        private final int histogramColumns;

        MyWorkDefinition(@NotNull WorkDefinitionFactory.WorkDefinitionInfo info) {
            super(info);
            var typedDefinition = (ObjectIntegrityCheckWorkDefinitionType) info.getBean();
            objects = ObjectSetUtil.emptyIfNull(typedDefinition.getObjects());
            histogramColumns = MoreObjects.firstNonNull(typedDefinition.getHistogramColumns(), DEFAULT_HISTOGRAM_COLUMNS);
        }

        @Override
        public @NotNull ObjectSetType getObjectSetSpecification() {
            return objects;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "objects", objects, indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "histogramColumns", histogramColumns, indent + 1);
        }
    }
}
