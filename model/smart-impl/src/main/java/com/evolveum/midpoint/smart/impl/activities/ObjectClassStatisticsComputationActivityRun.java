/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.impl.activities;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassStatisticsComputationWorkStateType;

import javax.xml.namespace.QName;

/**
 * Activity run that always generates new statistics for a given object class on a resource.
 *
 * <p>Existing statistics objects are ignored and never reused.
 * A new statistics object is always computed and stored.</p>
 *
 * <p>This activity is responsible only for statistics computation and persistence.</p>
 */
public class ObjectClassStatisticsComputationActivityRun
        extends AbstractStatisticsComputationActivityRun<
        ObjectClassStatisticsComputationActivityHandler.MyWorkDefinition,
        ObjectClassStatisticsComputationActivityHandler,
        ObjectClassStatisticsComputationWorkStateType> {

    ObjectClassStatisticsComputationActivityRun(
            ActivityRunInstantiationContext<
                    ObjectClassStatisticsComputationActivityHandler.MyWorkDefinition,
                    ObjectClassStatisticsComputationActivityHandler> context,
            String shortNameCapitalized) {
        super(context, shortNameCapitalized, TraceManager.getTrace(ObjectClassStatisticsComputationActivityRun.class));
    }

    @Override
    protected @NotNull String getResourceOid() {
        return getWorkDefinition().getResourceOid();
    }

    @Override
    protected @NotNull QName getObjectClassName() {
        return getWorkDefinition().getObjectClassName();
    }

    @Override
    protected void storeStatisticsObjectOid(String oid, OperationResult result)
            throws SchemaException, ActivityRunException {
        var state = getActivityState();
        state.setWorkStateItemRealValues(
                ObjectClassStatisticsComputationWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        state.flushPendingTaskModificationsChecked(result);
    }

    @Override
    protected boolean reuseExistingStatisticsObject() {
        return false;
    }
}
