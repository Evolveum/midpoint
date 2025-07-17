/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.tasks.handlers.search;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;

import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.repo.common.tasks.handlers.MockRecorder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Execution of a search-based mock activity.
 */
final class SearchBasedMockActivityRun
        extends SearchBasedActivityRun
        <ObjectType, SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler, AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBasedMockActivityRun.class);

    @NotNull private final Lazy<ObjectFilter> failOnFilter = Lazy.from(this::parseFailOnFilter);

    SearchBasedMockActivityRun(
            @NotNull ActivityRunInstantiationContext<SearchIterativeMockWorkDefinition, SearchIterativeMockActivityHandler> context) {
        super(context, "Search-iterative mock activity");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .synchronizationStatisticsSupported(true)
                .actionsExecutedStatisticsSupported(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }
        getRecorder().recordRealizationStartTimestamp(
                activityState.getRealizationStartTimestamp());
        return true;
    }

    @Override
    public boolean processItem(@NotNull ObjectType object,
            @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult result)
            throws SchemaException, ThresholdPolicyViolationException {

        String message = emptyIfNull(getWorkDefinition().getMessage()) + object.getName().getOrig();
        LOGGER.info("Message: {}", message);
        getRecorder().recordExecution(message);

        checkFailOn(object);
        checkFreezeIfScavenger();

        provideSomeMockStatistics(request, workerTask);
        return true;
    }

    private void checkFreezeIfScavenger() {
        if (!getWorkDefinition().isFreezeIfScavenger()) {
            return;
        }

        boolean scavenger = isWorker() && !isNonScavengingWorker();
        if (scavenger) {
            LOGGER.warn("Freezing because we are a scavenger");
            MiscUtil.sleepWatchfully(Long.MAX_VALUE, 100, () -> getRunningTask().canRun());
        }
    }

    private void checkFailOn(ObjectType object) throws SchemaException, ThresholdPolicyViolationException {
        if (getWorkDefinition().getFailOn() == null) {
            return;
        }

        boolean matches = failOnFilter.get().match(
                object.asPrismContainerValue(),
                SchemaService.get().matchingRuleRegistry());
        if (matches) {
            // To stop the processing immediately.
            throw new ThresholdPolicyViolationException("Object matches a filter: " + object);
        }
    }

    private ObjectFilter parseFailOnFilter() {
        SearchFilterType failOnBean = getWorkDefinition().getFailOn();
        if (failOnBean != null) {
            try {
                return PrismContext.get().getQueryConverter()
                        .parseFilter(failOnBean, getSearchSpecificationRequired().getType());
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        } else {
            return null;
        }
    }

    private void provideSomeMockStatistics(ItemProcessingRequest<ObjectType> request, RunningTask workerTask) {
        ObjectType object = request.getItem();
        workerTask.onSynchronizationStart(request.getIdentifier(), object.getOid(), SynchronizationSituationType.UNLINKED);
        workerTask.onSynchronizationSituationChange(request.getIdentifier(), object.getOid(), SynchronizationSituationType.LINKED);
        workerTask.recordObjectActionExecuted(object.asPrismObject(), ChangeType.MODIFY, null);
        workerTask.recordObjectActionExecuted(object.asPrismObject(), ChangeType.MODIFY, null);
    }

    @NotNull
    private MockRecorder getRecorder() {
        return getActivityHandler().getRecorder();
    }
}
