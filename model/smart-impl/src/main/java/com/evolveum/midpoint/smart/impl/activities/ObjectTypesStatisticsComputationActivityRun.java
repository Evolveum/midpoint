/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.smart.impl.activities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionWorkStateType;

/**
 * Activity run responsible for providing statistics used for object type suggestions.
 *
 * <p>This activity works in two steps:
 * <ol>
 *   <li>If the latest statistics already exist in the system, they are reused.</li>
 *   <li>If no statistics are found, new ones are generated and stored.</li>
 * </ol>
 *
 * The resulting statistics are then used exclusively for suggestion purposes.</p>
 */
public class ObjectTypesStatisticsComputationActivityRun
        extends AbstractStatisticsComputationActivityRun<
        ObjectTypesSuggestionWorkDefinition,
        ObjectTypesSuggestionActivityHandler,
                ObjectTypesSuggestionWorkStateType> {

    ObjectTypesStatisticsComputationActivityRun(
            ActivityRunInstantiationContext<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> context,
            String shortNameCapitalized) {
        super(context, shortNameCapitalized, TraceManager.getTrace(ObjectTypesStatisticsComputationActivityRun.class));
    }

    @Override
    protected @Nullable String getPresetStatisticsObjectOid() {
        return getWorkDefinition().getStatisticsObjectOid();
    }

    @Override
    protected @NotNull String getResourceOid() {
        return getWorkDefinition().getResourceOid();
    }

    @Override
    protected @NotNull javax.xml.namespace.QName getObjectClassName() {
        return getWorkDefinition().getObjectClassName();
    }

    @Override
    protected void storeStatisticsObjectOid(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = Util.getParentState(this, result);
        parentState.setWorkStateItemRealValues(
                ObjectTypesSuggestionWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }
}
