/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.api.LiveSyncTokenStorage;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LiveSyncWorkStateType;

public class ActivityTokenStorageImpl implements LiveSyncTokenStorage {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityTokenStorageImpl.class);

    @NotNull private final AbstractActivityRun<?, ?, ?> activityRun;

    ActivityTokenStorageImpl(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    @Override
    public LiveSyncToken getToken() {
        Object fromActivity = getValueFromActivity();
        if (fromActivity != null) {
            return LiveSyncToken.of(fromActivity);
        }

        Object fromTaskExtension = getValueFromTaskExtension();
        if (fromTaskExtension != null) {
            return LiveSyncToken.of(fromTaskExtension);
        }

        return null;
    }

    private Object getValueFromActivity() {
        return activityRun.getActivityState().getWorkStatePropertyRealValue(LiveSyncWorkStateType.F_TOKEN, Object.class);
    }

    private Object getValueFromTaskExtension() {
        Object tokenValue = activityRun.getRunningTask().getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        LOGGER.trace("Initial token from the task: {}", SchemaDebugUtil.prettyPrintLazily(tokenValue));
        return tokenValue;
    }

    @Override
    public void setToken(LiveSyncToken token, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        CurrentActivityState<?> activityState = activityRun.getActivityState();
        if (token != null) {
            Object tokenValue = token.getValue();
            PrismPropertyDefinition<?> tokenDefinition = createDefinition(tokenValue);
            activityState.setWorkStateItemRealValues(LiveSyncWorkStateType.F_TOKEN, tokenDefinition, tokenValue);
        } else {
            activityState.setWorkStateItemRealValues(LiveSyncWorkStateType.F_TOKEN);
        }
        activityState.flushPendingTaskModifications(result);
        // TODO remove token from task if exists
    }

    private static <T> @NotNull PrismPropertyDefinition<T> createDefinition(@NotNull T realValue) {
        QName type = XsdTypeMapper.toXsdType(realValue.getClass());

        PrismPropertyDefinition<T> propDef =
                PrismContext.get().definitionFactory().newPropertyDefinition(
                        LiveSyncWorkStateType.F_TOKEN, type, 0, 1);
        propDef.mutator().setDynamic(true);
        return propDef;
    }
}
