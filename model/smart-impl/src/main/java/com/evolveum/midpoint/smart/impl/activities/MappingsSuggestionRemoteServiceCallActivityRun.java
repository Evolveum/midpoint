package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaMatchResultType;

import org.jetbrains.annotations.NotNull;

public class MappingsSuggestionRemoteServiceCallActivityRun extends LocalActivityRun<
        MappingsSuggestionWorkDefinition,
        MappingsSuggestionActivityHandler,
        MappingsSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSuggestionRemoteServiceCallActivityRun.class);

    protected MappingsSuggestionRemoteServiceCallActivityRun(@NotNull ActivityRunInstantiationContext<MappingsSuggestionWorkDefinition, MappingsSuggestionActivityHandler> context) {
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
        var typeDef = getWorkDefinition().getTypeIdentification();
        var targetPathsToIgnore = getWorkDefinition().getTargetPathsToIgnore();
        var state = getActivityState();

        LOGGER.debug("Going to suggest mappings for resource {}, kind {} and intent {}",
                resourceOid, kind, intent);

        var schemaMatch = parentState.getWorkStateItemRealValueClone(
                MappingsSuggestionWorkStateType.F_SCHEMA_MATCH, SchemaMatchResultType.class);
        var isInbound = getWorkDefinition().isInbound();

        var suggestedMappings = SmartIntegrationBeans.get().smartIntegrationService.suggestMappings(
                resourceOid, typeDef, schemaMatch, isInbound, targetPathsToIgnore, state, task, result);

        parentState.setWorkStateItemRealValues(MappingsSuggestionWorkStateType.F_RESULT, suggestedMappings);
        parentState.flushPendingTaskModifications(result);
        LOGGER.debug("Suggestions written to the work state:\n{}", suggestedMappings.debugDump(1));

        return ActivityRunResult.success();
    }
}
