package com.evolveum.midpoint.smart.impl.activities;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.ActivityInterruptedException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeStatisticsTypeUtil;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaMatchResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiMatchSchemaResponseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.evolveum.midpoint.prism.PrismContext.get;

/**
 * Computes schema match and stores it into the parent work state as XML string for reuse.
 */
public class MappingsSchemaMatchingActivityRun extends LocalActivityRun<
        MappingsSuggestionWorkDefinition,
        MappingsSuggestionActivityHandler,
        MappingsSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSchemaMatchingActivityRun.class);

    public MappingsSchemaMatchingActivityRun(
            @NotNull ActivityRunInstantiationContext<MappingsSuggestionWorkDefinition, MappingsSuggestionActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    private void setSchemaMatchObjectOidInWorkState(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = Util.getParentState(this, result);
        parentState.setWorkStateItemRealValues(
                MappingsSuggestionWorkStateType.F_SCHEMA_MATCH_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }

    private @Nullable String findLatestSchemaMatchObjectOid(OperationResult result) throws SchemaException {
        var workDef = getWorkDefinition();
        var lastSchemaMatchObject = SmartIntegrationBeans.get().smartIntegrationService.getLatestObjectTypeSchemaMatch(
                workDef.getResourceOid(), workDef.getKind(), workDef.getIntent(), getRunningTask(), result);
        return lastSchemaMatchObject != null ? lastSchemaMatchObject.getOid() : null;
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result)
            throws ActivityRunException, CommonException, ActivityInterruptedException {
        var workDef = getWorkDefinition();
        var resourceOid = workDef.getResourceOid();
        var typeIdentification = workDef.getTypeIdentification();

        SchemaMatchResultType match = SmartIntegrationBeans.get().smartIntegrationService
                .computeSchemaMatch(resourceOid, typeIdentification, getRunningTask(), result);

        var parentState = Util.getParentState(this, result);
        parentState.setWorkStateItemRealValues(MappingsSuggestionWorkStateType.F_SCHEMA_MATCH, match);
        parentState.flushPendingTaskModificationsChecked(result);

        return ActivityRunResult.success();
    }
}
