/*
 * Copyright (c) 2020-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.objectTypeSuggestion;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowObjectClassUtil;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.smart.impl.activities.Util;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Calls the remote service to provide object types suggestions for a given object class.
 *
 * Later, we can create a variant of this activity that computes statistics for objects already present in the repository.
 */
class ObjectTypesSuggestionObjectTypesActivityRun
        extends LocalActivityRun<
            ObjectTypesSuggestionWorkDefinition,
            ObjectTypesSuggestionActivityHandler,
            ObjectTypesSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypesSuggestionObjectTypesActivityRun.class);

    ObjectTypesSuggestionObjectTypesActivityRun(
            ActivityRunInstantiationContext<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws ActivityRunException, CommonException {
        var task = getRunningTask();
        var parentState = Util.getParentState(this, result);
        var resourceOid = getWorkDefinition().getResourceOid();
        var objectClassName = getWorkDefinition().getObjectClassName();

        boolean useAi = getWorkDefinition().getPermissions().contains(DataAccessPermissionType.STATISTICS_ACCESS);
        ObjectTypesSuggestionType suggestedTypes;

        if (useAi) {
            var statisticsOid = MiscUtil.stateNonNull(
                    Referencable.getOid(parentState.getWorkStateReferenceRealValue(
                            ObjectTypesSuggestionWorkStateType.F_STATISTICS_REF)),
                    "Statistics object reference is not set in the work state in %s", task);
            LOGGER.debug("Going to suggest object types for resource {} and object class {}; statistics in: {}",
                    resourceOid, objectClassName, statisticsOid);

            var statistics = ShadowObjectClassUtil.getStatisticsRequired(
                    getBeans().repositoryService.getObject(GenericObjectType.class, statisticsOid, null, result));
            suggestedTypes = SmartIntegrationBeans.get().smartIntegrationService.suggestObjectTypes(
                    resourceOid, objectClassName, statistics, task, result);
            LOGGER.debug("AI-based suggestions to be written to the work state:\n{}",
                    suggestedTypes.debugDump(1));
        } else {
            LOGGER.debug("Cannot suggest object types for resource {} and object class {}: {} permission not granted",
                    resourceOid, objectClassName, DataAccessPermissionType.STATISTICS_ACCESS);
            suggestedTypes = new ObjectTypesSuggestionType();
        }

        parentState.setWorkStateItemRealValues(ObjectTypesSuggestionWorkStateType.F_RESULT, suggestedTypes);
        parentState.flushPendingTaskModifications(result);

        return ActivityRunResult.success();
    }
}
