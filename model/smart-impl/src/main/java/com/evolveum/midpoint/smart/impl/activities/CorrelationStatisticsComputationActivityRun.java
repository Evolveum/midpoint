package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeStatisticsTypeUtil;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CorrelationStatisticsComputationActivityRun extends SearchBasedActivityRun<
        ShadowType,
        CorrelationSuggestionWorkDefinition,
        CorrelationSuggestionActivityHandler,
        CorrelationSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationStatisticsComputationActivityRun.class);

    private ResourceType resource;

    private ObjectTypeRelatedStatisticsComputer computer;

    public CorrelationStatisticsComputationActivityRun(@NotNull ActivityRunInstantiationContext<CorrelationSuggestionWorkDefinition, CorrelationSuggestionActivityHandler> context, @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
        setInstanceReady();
    }

    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .skipWritingOperationExecutionRecords(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }

        ensureNoDryRun();
        ensureNoParallelism();

        var workDef = getWorkDefinition();

        var presetOid = workDef.getStatisticsObjectOid();
        if (presetOid != null) {
            LOGGER.debug("Statistics object OID is pre-set to {}, will skip the execution", presetOid);
            setStatisticsObjectOidInWorkState(presetOid, result);
            return false;
        }

        var foundOid = findLatestStatisticsObjectOid(result);
        if (foundOid != null) {
            LOGGER.debug("Found existing statistics object with OID {}, will skip the execution", foundOid);
            setStatisticsObjectOidInWorkState(foundOid, result);
            return false;
        }

        resource = getActivityHandler().getModelBeans().modelService
                .getObject(ResourceType.class, workDef.getResourceOid(), null, getRunningTask(), result)
                .asObjectable();
        var resourceSchema = Resource.of(resource).getCompleteSchemaRequired();
        var typeIdentification = getWorkDefinition().getTypeIdentification();
        var typeDefinition = resourceSchema.getObjectTypeDefinitionRequired(typeIdentification);

        computer = new ObjectTypeRelatedStatisticsComputer(typeDefinition);

        return true;
    }

    private @Nullable String findLatestStatisticsObjectOid(OperationResult result) throws SchemaException {
        var workDef = getWorkDefinition();
        var lastStatisticsObject = SmartIntegrationBeans.get().smartIntegrationService.getLatestObjectTypeStatistics(
                workDef.getResourceOid(), workDef.getKind(), workDef.getIntent(), getRunningTask(), result);
        return lastStatisticsObject != null ? lastStatisticsObject.getOid() : null;
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {
        super.afterRun(result);
        if (getRunningTask().canRun()) {
            computer.postProcessStatistics();
            var statistics = computer.getStatistics()
                    .coverage(1.0f) // TODO: compute coverage properly
                    .timestamp(beans.clock.currentTimeXMLGregorianCalendar());

            var statisticsObject = ShadowObjectTypeStatisticsTypeUtil.createObjectTypeStatisticsObject(
                    resource.getOid(),
                    resource.getName().getOrig(),
                    getWorkDefinition().getKind(),
                    getWorkDefinition().getIntent(),
                    statistics);

            LOGGER.debug("Adding statistics object:\n{}", statisticsObject.debugDump(1));

            var statisticsObjectOid =
                    getBeans().repositoryService.addObject(statisticsObject.asPrismObject(), null, result);

            setStatisticsObjectOidInWorkState(statisticsObjectOid, result);
        }
    }

    private void setStatisticsObjectOidInWorkState(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = Util.getParentState(this, result);
        parentState.setWorkStateItemRealValues(
                CorrelationSuggestionWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }

    @Override
    public boolean processItem(
            @NotNull ShadowType item,
            @NotNull ItemProcessingRequest<ShadowType> request,
            RunningTask workerTask,
            OperationResult result) throws CommonException, ActivityRunException {
        computer.process(item);
        return true;
    }
}
