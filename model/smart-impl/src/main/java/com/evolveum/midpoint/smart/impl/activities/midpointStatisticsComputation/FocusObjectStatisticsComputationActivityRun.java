/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.midpointStatisticsComputation;

import static com.evolveum.midpoint.schema.util.FocusObjectStatisticsTypeUtil.createFocusObjectStatisticsObject;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.smart.impl.activities.FocusObjectStatisticsComputer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

/**
 * Activity run for computing statistics for focus objects of a specific type.
 *
 * <p>The activity processes objects of the given type (e.g. {@link UserType}).
 * If resource/kind/intent filters are specified, only focuses that have shadows
 * matching these criteria will be processed.
 * Computed statistics are persisted as a {@link GenericObjectType}.</p>
 */
public class FocusObjectStatisticsComputationActivityRun
        extends SearchBasedActivityRun<
        ObjectType, FocusObjectStatisticsComputationWorkDefinition,
        FocusObjectStatisticsComputationActivityHandler, FocusObjectStatisticsComputationWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusObjectStatisticsComputationActivityRun.class);

    /** Computes the statistics for the objects found. Null if statistics are not being computed. */
    private FocusObjectStatisticsComputer computer;

    /** Resolved object type class. */
    private Class<? extends ObjectType> objectTypeClass;

    protected FocusObjectStatisticsComputationActivityRun(
            ActivityRunInstantiationContext<FocusObjectStatisticsComputationWorkDefinition,
                    FocusObjectStatisticsComputationActivityHandler> context,
            String shortNameCapitalized) {
        super(context, shortNameCapitalized);
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .skipWritingOperationExecutionRecords(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }

        ensureNoDryRun();
        ensureNoParallelism();

        QName objectTypeName = getWorkDefinition().getObjectType();
        objectTypeClass = ObjectTypes.getObjectTypeClass(objectTypeName);

        var presetOid = getWorkDefinition().getStatisticsObjectOid();
        if (presetOid != null) {
            LOGGER.debug("Statistics object OID is pre-set to {}, will skip the execution", presetOid);
            storeStatisticsObjectOid(presetOid, result);
            return false;
        }

        PrismObjectDefinition<?> objectDefinition = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(objectTypeName);
        if (objectDefinition == null) {
            throw new ActivityRunException(
                    "No prism object definition found for type " + objectTypeName,
                    OperationResultStatus.FATAL_ERROR,
                    ActivityRunResultStatus.PERMANENT_ERROR);
        }

        computer = new FocusObjectStatisticsComputer(objectTypeName, objectDefinition);

        return true;
    }

    @Override
    public @Nullable SearchSpecification<ObjectType> createCustomSearchSpecification(OperationResult result) {
        var queryBuilder = PrismContext.get().queryFor(objectTypeClass)
                .item(FocusType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE, ShadowType.F_RESOURCE_REF).ref(getWorkDefinition().getResourceOid())
                .and().item(FocusType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE, ShadowType.F_KIND).eq(getWorkDefinition().getKind())
                .and().item(FocusType.F_LINK_REF, PrismConstants.T_OBJECT_REFERENCE, ShadowType.F_INTENT).eq(getWorkDefinition().getIntent());

        return new SearchSpecification<>(
                (Class<ObjectType>) objectTypeClass,
                queryBuilder.build(),
                null,
                false);
    }

    @Override
    public boolean processItem(
            @NotNull ObjectType item,
            @NotNull ItemProcessingRequest<ObjectType> request,
            RunningTask workerTask,
            OperationResult result) {
        computer.process(item);
        return true;
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {
        super.afterRun(result);

        if (!getRunningTask().canRun()) {
            return;
        }

        computer.postProcessStatistics();

        var statistics = computer.getStatistics()
                .coverage(1.0f)
                .timestamp(beans.clock.currentTimeXMLGregorianCalendar());

        var objectTypeName = getWorkDefinition().getObjectType().getLocalPart();
        var resourceOid = getWorkDefinition().getResourceOid();
        var kind = getWorkDefinition().getKind().value();
        var intent = getWorkDefinition().getIntent();
        var statisticsObject = createFocusObjectStatisticsObject(
                objectTypeName, resourceOid, kind, intent, statistics);

        LOGGER.debug("Adding focus object statistics object:\n{}", statisticsObject.debugDump(1));

        var oid = getBeans().repositoryService.addObject(statisticsObject.asPrismObject(), null, result);
        storeStatisticsObjectOid(oid, result);
    }

    /** Stores the produced (or reused) statistics object OID into appropriate work state. */
    protected void storeStatisticsObjectOid(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = this.getActivityState();
        parentState.setWorkStateItemRealValues(
                FocusObjectStatisticsComputationWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }
}
