/*
 * Copyright (C) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_TRIGGER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType.F_TIMESTAMP;

import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;

import com.evolveum.midpoint.repo.common.activity.run.SearchSpecification;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.tasks.scanner.ScanActivityRun;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Single execution of a trigger scanner task part.
 */
final class TriggerScanActivityRun
        extends ScanActivityRun<ObjectType, TriggerScanWorkDefinition, TriggerScanActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(TriggerScanActivityRun.class);

    private TriggerScanItemProcessor itemProcessor;

    TriggerScanActivityRun(
            @NotNull ActivityRunInstantiationContext<TriggerScanWorkDefinition, TriggerScanActivityHandler> context) {
        super(context, "Trigger scan");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }
        ensureNoDryRun();
        itemProcessor = new TriggerScanItemProcessor(this);
        return true;
    }

    @Override
    public void customizeQuery(SearchSpecification<ObjectType> searchSpecification, OperationResult result) {
        LOGGER.debug("Looking for triggers with timestamps up to {}", thisScanTimestamp);
        searchSpecification.addFilter(
                PrismContext.get().queryFor(ObjectType.class)
                        .item(F_TRIGGER, F_TIMESTAMP).le(thisScanTimestamp)
                        .buildFilter());
    }

    @Override
    public boolean processItem(@NotNull ObjectType object,
            @NotNull ItemProcessingRequest<ObjectType> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityRunException {
        return itemProcessor.processObject(object, workerTask, result);
    }
}
