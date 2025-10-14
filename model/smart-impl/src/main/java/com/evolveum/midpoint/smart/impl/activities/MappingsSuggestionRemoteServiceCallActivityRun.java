package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionWorkStateType;

import org.jetbrains.annotations.NotNull;

public class MappingsSuggestionRemoteServiceCallActivityRun extends LocalActivityRun<
        ObjectTypeRelatedSuggestionWorkDefinition,
        MappingsSuggestionActivityHandler,
        MappingsSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSuggestionRemoteServiceCallActivityRun.class);

    protected MappingsSuggestionRemoteServiceCallActivityRun(@NotNull ActivityRunInstantiationContext<ObjectTypeRelatedSuggestionWorkDefinition, MappingsSuggestionActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws ActivityRunException, CommonException, ActivityInterruptedException {
        var task = getRunningTask();
        var parentState = Util.getParentState(this, result);
        var resourceOid = getWorkDefinition().getResourceOid();
        var kind = getWorkDefinition().getKind();
        var intent = getWorkDefinition().getIntent();
        var state = getActivityState();
        var statisticsOid =
                MiscUtil.stateNonNull(
                        Referencable.getOid(parentState.getWorkStateReferenceRealValue(
                                MappingsSuggestionWorkStateType.F_STATISTICS_REF)),
                        "Statistics object reference is not set in the work state in %s", task);


        LOGGER.debug("Going to suggest mappings for resource {}, kind {} and intent {}; statistics in: {}",
                resourceOid, kind, intent, statisticsOid);
        var statistics = ShadowObjectClassStatisticsTypeUtil.getObjectTypeStatisticsRequired(
                getBeans().repositoryService.getObject(GenericObjectType.class, statisticsOid, null, result));

        var suggestedMappings = SmartIntegrationBeans.get().smartIntegrationService.suggestMappings(
                resourceOid, getWorkDefinition().getTypeIdentification(), statistics, null, null, state, task, result);

        parentState.setWorkStateItemRealValues(MappingsSuggestionWorkStateType.F_RESULT, suggestedMappings);
        parentState.flushPendingTaskModifications(result);
        LOGGER.debug("Suggestions written to the work state:\n{}", suggestedMappings.debugDump(1));

        return ActivityRunResult.success();
    }
}
