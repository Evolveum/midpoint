/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

class RemoteServiceCallFocusTypeActivityRun
        extends LocalActivityRun<
            ObjectTypesSuggestionWorkDefinition,
            ObjectTypesSuggestionActivityHandler,
            ObjectTypesSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(RemoteServiceCallFocusTypeActivityRun.class);

    RemoteServiceCallFocusTypeActivityRun(
            ActivityRunInstantiationContext<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws CommonException {
        var task = getRunningTask();
        var parentState = Util.getParentState(this, result);
        var resourceOid = getWorkDefinition().getResourceOid();

        var suggestedObjectTypesClone = parentState.getWorkStateItemRealValueClone(
                ObjectTypesSuggestionWorkStateType.F_RESULT, ObjectTypesSuggestionType.class);

        for (var objectType : suggestedObjectTypesClone.getObjectType()) {
            LOGGER.debug("Going to suggest focus type for resource {} and object type:\n{}",
                    resourceOid, objectType.debugDumpLazily(1));
            var focusType =
                    SmartIntegrationBeans.get().smartIntegrationService.suggestFocusType(resourceOid, objectType, task, result);
            objectType.setFocus(
                    new ResourceObjectFocusSpecificationType()
                            .type(focusType.getFocusType())
            );
        }
        parentState.setWorkStateItemRealValues(ObjectTypesSuggestionWorkStateType.F_RESULT, suggestedObjectTypesClone);
        parentState.flushPendingTaskModifications(result);
        LOGGER.debug("Suggestions written to the work state:\n{}", suggestedObjectTypesClone.debugDump(1));

        return ActivityRunResult.success();
    }
}
