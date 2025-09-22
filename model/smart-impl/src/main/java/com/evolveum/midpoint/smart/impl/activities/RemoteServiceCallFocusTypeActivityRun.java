/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectFocusSpecificationType;

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

        for (var objectTypeBean : suggestedObjectTypesClone.getObjectType()) {
            LOGGER.debug("Going to suggest focus type for resource {} and object type:\n{}",
                    resourceOid, objectTypeBean.debugDumpLazily(1));
            try {
                var focusType =
                        SmartIntegrationBeans.get().smartIntegrationService.suggestFocusType(resourceOid, objectTypeBean, task, result);
                objectTypeBean.setFocus(
                        new ResourceObjectFocusSpecificationType()
                                .type(focusType.getFocusType()));
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't determine focus type for resource {} and object type {}",
                        e, resourceOid, ResourceObjectTypeIdentification.of(objectTypeBean));
                // TODO report the error somehow (probably after making this activity iterative)
            }
        }
        parentState.setWorkStateItemRealValues(ObjectTypesSuggestionWorkStateType.F_RESULT, suggestedObjectTypesClone);
        parentState.flushPendingTaskModifications(result);
        LOGGER.debug("Suggestions written to the work state:\n{}", suggestedObjectTypesClone.debugDump(1));

        return ActivityRunResult.success();
    }
}
