/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.smart.impl.activities.midpointStatisticsComputation;

import static com.evolveum.midpoint.repo.common.activity.run.ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
import static com.evolveum.midpoint.schema.util.FocusObjectStatisticsTypeUtil.createFocusObjectStatisticsObject;

import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ErrorHandlingStrategyExecutor;
import com.evolveum.midpoint.repo.common.activity.run.PlainIterativeActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.processing.ObjectProcessingRequest;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.smart.impl.activities.FocusObjectStatisticsComputer;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity run for computing statistics for focus objects of a specific type.
 *
 * The activity processes objects of the given type (e.g. {@link UserType}).
 * If resource/kind/intent filters are specified, only focuses that have shadows
 * matching these criteria will be processed.
 * Computed statistics are persisted as a {@link GenericObjectType}.
 */
public class FocusObjectStatisticsComputationActivityRun extends PlainIterativeActivityRun<
        FocusType,
        FocusObjectStatisticsComputationWorkDefinition,
        FocusObjectStatisticsComputationActivityHandler,
        FocusObjectStatisticsComputationWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusObjectStatisticsComputationActivityRun.class);

    /** Factory for creating iteration instances. */
    private final FocusObjectStatisticsIterationFactory iterationFactory;

    /**
     * Provides numbers for {@link ItemProcessingRequest} objects.
     */
    private final AtomicInteger objectsCounter = new AtomicInteger(0);

    /** Computes the statistics for the objects found. Null if statistics are not being computed. */
    private FocusObjectStatisticsComputer computer;

    protected FocusObjectStatisticsComputationActivityRun(
            ActivityRunInstantiationContext<FocusObjectStatisticsComputationWorkDefinition,
                    FocusObjectStatisticsComputationActivityHandler> context,
            String shortNameCapitalized,
            FocusObjectStatisticsIterationFactory iterationFactory) {
        super(context, shortNameCapitalized);
        this.iterationFactory = iterationFactory;
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
        ensureNotInWorkerTask(null);

        QName objectTypeName = getWorkDefinition().getObjectType();

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
    public void iterateOverItemsInBucket(OperationResult opResult) throws CommonException {
        // @formatter:off
        final ObjectQuery shadowQuery = PrismContext.get().queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(getWorkDefinition().getResourceOid())
                .and()
                    .item(ShadowType.F_KIND).eq(getWorkDefinition().getKind())
                .and()
                    .item(ShadowType.F_INTENT).eq(getWorkDefinition().getIntent())
                .build();
        // @formatter:on
        iterationFactory.iterationForType(getWorkDefinition().getObjectType())
                .iterate(shadowQuery)
                .consumeItemsWith(
                        (focus, lResult) -> {
                            final var processingRequest = ObjectProcessingRequest.create(this.objectsCounter.getAndIncrement(),
                                    focus.asObjectable(), this);
                            return this.coordinator.submit(processingRequest, lResult);
                        },
                        opResult);
    }

    @Override
    public boolean processItem(
            @NotNull ItemProcessingRequest<FocusType> request,
            @NotNull RunningTask workerTask,
            @NotNull OperationResult result) {
        ObjectType item = request.getItem();
        computer.process(item);
        return true;
    }

    @Override
    public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return CONTINUE;
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
            throws SchemaException, ActivityRunException {
        var parentState = this.getActivityState();
        parentState.setWorkStateItemRealValues(
                FocusObjectStatisticsComputationWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }
}
