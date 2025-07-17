/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Referencable;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionWorkStateType;

/**
 * Calls the remote service to provide object types suggestions for a given object class.
 *
 * Later, we can create a variant of this activity that computes statistics for objects already present in the repository.
 */
class RemoteServiceCallActivityRun
        extends LocalActivityRun<
            ObjectTypesSuggestionWorkDefinition,
            ObjectTypesSuggestionActivityHandler,
            ObjectTypesSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(RemoteServiceCallActivityRun.class);

    RemoteServiceCallActivityRun(
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
        var statisticsOid =
                MiscUtil.stateNonNull(
                        Referencable.getOid(parentState.getWorkStateReferenceRealValue(
                                ObjectTypesSuggestionWorkStateType.F_STATISTICS_REF)),
                        "Statistics object reference is not set in the work state in %s", task);

        LOGGER.debug("Going to suggest object types for resource {} and object class {}; statistics in: {}",
                resourceOid, objectClassName, statisticsOid);
        var statistics = ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(
                getBeans().repositoryService.getObject(GenericObjectType.class, statisticsOid, null, result));
        var suggestedTypes = SmartIntegrationBeans.get().smartIntegrationService.suggestObjectTypes(
                resourceOid, objectClassName, statistics, task, result);
        parentState.setWorkStateItemRealValues(ObjectTypesSuggestionWorkStateType.F_RESULT, suggestedTypes);
        parentState.flushPendingTaskModifications(result);
        LOGGER.debug("Suggestions written to the work state:\n{}",
                suggestedTypes.debugDump(1));

        return ActivityRunResult.success();
    }
}
