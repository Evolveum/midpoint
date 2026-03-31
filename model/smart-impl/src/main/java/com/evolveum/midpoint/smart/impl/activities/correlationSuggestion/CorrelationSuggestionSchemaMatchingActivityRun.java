/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.correlationSuggestion;

import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.smart.impl.activities.Util;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CorrelationSuggestionSchemaMatchingActivityRun extends LocalActivityRun<
        CorrelationSuggestionWorkDefinition,
        CorrelationSuggestionActivityHandler,
        CorrelationSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationSuggestionSchemaMatchingActivityRun.class);

    public CorrelationSuggestionSchemaMatchingActivityRun(
            @NotNull ActivityRunInstantiationContext<CorrelationSuggestionWorkDefinition, CorrelationSuggestionActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    private void setSchemaMatchObjectOidInWorkState(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = Util.getParentState(this, result);
        parentState.setWorkStateItemRealValues(
                CorrelationSuggestionWorkStateType.F_SCHEMA_MATCH_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }

    private @Nullable String findLatestSchemaMatchObjectOid(OperationResult result) throws SchemaException {
        var workDef = getWorkDefinition();
        var lastSchemaMatchObject = SmartIntegrationBeans.get().smartIntegrationService.getLatestObjectTypeSchemaMatch(
                workDef.getResourceOid(), workDef.getKind(), workDef.getIntent(), result);
        return lastSchemaMatchObject != null ? lastSchemaMatchObject.getOid() : null;
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result)
            throws ActivityRunException, CommonException, ActivityInterruptedException {
        var workDef = getWorkDefinition();
        var resourceOid = workDef.getResourceOid();
        var typeIdentification = workDef.getTypeIdentification();

        var foundOid = findLatestSchemaMatchObjectOid(result);
        if (foundOid != null) {
            LOGGER.debug("Found existing object type schema match object with OID {}, will skip the computation", foundOid);
            setSchemaMatchObjectOidInWorkState(foundOid, result);
            return ActivityRunResult.success();
        }

        boolean useAi = workDef.getPermissions().contains(DataAccessPermissionType.SCHEMA_ACCESS);
        var match = SmartIntegrationBeans.get().smartIntegrationService
                .computeSchemaMatch(resourceOid, typeIdentification, useAi, getRunningTask(), result);
        var schemaMatchOid = SmartIntegrationBeans.get().schemaMatchService
                .saveSchemaMatch(resourceOid, workDef.getKind(), workDef.getIntent(), match, result);

        setSchemaMatchObjectOidInWorkState(schemaMatchOid, result);

        return ActivityRunResult.success();
    }
}
