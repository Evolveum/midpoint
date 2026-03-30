/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.mappingSuggestion;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeUtil;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.smart.impl.activities.Util;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        final MappingsSuggestionWorkDefinition workDefinition = getWorkDefinition();
        var resourceOid = workDefinition.getResourceOid();
        var typeDef = workDefinition.getTypeIdentification();
        var targetPathsToIgnore = workDefinition.getTargetPathsToIgnore();
        var state = getActivityState();

        LOGGER.debug("Going to suggest mappings for resource {}, kind {} and intent {}",
                resourceOid, typeDef.getKind(), typeDef.getIntent());

        var schemaMatch = parentState.getWorkStateItemRealValueClone(
                MappingsSuggestionWorkStateType.F_SCHEMA_MATCH, SchemaMatchResultType.class);
        var isInbound = workDefinition.isInbound();

        boolean useAi = workDefinition.getPermissions().contains(DataAccessPermissionType.RAW_DATA_ACCESS);

        var objectTypeStatistics = loadObjectTypeStatistics(parentState, result);

        var suggestedMappings = SmartIntegrationBeans.get().smartIntegrationService.suggestMappings(
                resourceOid, typeDef, schemaMatch, isInbound, useAi, objectTypeStatistics, targetPathsToIgnore, state, task, result);

        parentState.setWorkStateItemRealValues(MappingsSuggestionWorkStateType.F_RESULT, suggestedMappings);
        parentState.flushPendingTaskModifications(result);
        LOGGER.debug("Suggestions written to the work state:\n{}", suggestedMappings.debugDump(1));

        return ActivityRunResult.success();
    }

    private ShadowObjectClassStatisticsType loadObjectTypeStatistics(
            ActivityState parentState, OperationResult result) {
        try {
            var statisticsRef = parentState.getWorkStateItemRealValueClone(
                    MappingsSuggestionWorkStateType.F_STATISTICS_REF, ObjectReferenceType.class);
            if (statisticsRef == null) {
                return null;
            }
            var statisticsOid = Referencable.getOid(statisticsRef);
            if (statisticsOid == null) {
                return null;
            }
            var statisticsObject = SmartIntegrationBeans.get().repositoryService
                    .getObject(GenericObjectType.class, statisticsOid, null, result)
                    .asObjectable();
            return ShadowObjectTypeUtil.getObjectTypeStatisticsRequired(statisticsObject);
        } catch (Exception e) {
            LOGGER.warn("Failed to load object type statistics from work state, proceeding without them: {}", e.getMessage());
            return null;
        }
    }
}
