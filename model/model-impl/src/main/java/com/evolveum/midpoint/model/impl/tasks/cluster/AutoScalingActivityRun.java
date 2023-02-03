/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.cluster;

import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public final class AutoScalingActivityRun extends
        SearchBasedActivityRun<TaskType, AutoScalingWorkDefinition, AutoScalingActivityHandler, ActivityAutoScalingWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AutoScalingActivityHandler.class);

    /**
     * Decides whether reconciliation is to start or not. Initialized before execution.
     */
    private ReconciliationLatch latch;

    AutoScalingActivityRun(
            @NotNull ActivityRunInstantiationContext<AutoScalingWorkDefinition, AutoScalingActivityHandler> activityRun) {
        super(activityRun, "Auto-scaling");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .skipWritingOperationExecutionRecords(false); // this is to be reconsidered
    }

    @Override
    public void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        ensureNoPreviewNorDryRun();

        XMLGregorianCalendar now = getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar();
        latch = new ReconciliationLatch(getActivity(), getActivityState(), now);
        latch.determineSituation(result);
    }

    @Override
    public ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult result) {
        PrismContext prismContext = getBeans().prismContext;

        if (!latch.isShouldReconcileTasks()) {
            return prismContext.queryFor(TaskType.class)
                    .none()
                    .build();
        }

        // We select all autoscaling-enabled running tasks that have some children.
        // (The last condition is approximated by being in WAITING state.)
        ObjectFilter reconcilableTasksFilter = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.RUNNING)
                .and().item(TaskType.F_SCHEDULING_STATE).eq(TaskSchedulingStateType.WAITING)
                .and().not().item(TaskType.F_AUTO_SCALING, TaskAutoScalingType.F_MODE).eq(TaskAutoScalingModeType.DISABLED)
                .buildFilter();

        ObjectQuery objectQuery = ObjectQueryUtil.addConjunctions(configuredQuery, reconcilableTasksFilter);

        LOGGER.info("Going to reconcile workers for tasks using a query of {}", objectQuery);
        return objectQuery;
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result) throws CommonException {
        return GetOperationOptions.updateToNoFetch(configuredOptions);
    }

    @Override
    public boolean processItem(@NotNull TaskType task,
            @NotNull ItemProcessingRequest<TaskType> request, RunningTask workerTask, OperationResult result)
            throws CommonException {
        LOGGER.debug("Going to reconcile workers for task {}", task);
        getActivityHandler().activityManager.reconcileWorkers(task.getOid(), result);
        return true;
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {
        latch.updateActivityState(result);
    }
}
