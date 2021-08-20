/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.cluster;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.BaseSearchBasedExecutionSpecificsImpl;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

public class AutoScalingExecutionSpecifics extends
        BaseSearchBasedExecutionSpecificsImpl<TaskType, AutoScalingWorkDefinition, AutoScalingActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(AutoScalingActivityHandler.class);

    /**
     * Decides whether reconciliation is to start or not. Initialized before execution.
     */
    private ReconciliationLatch latch;

    AutoScalingExecutionSpecifics(
            @NotNull SearchBasedActivityExecution<TaskType, AutoScalingWorkDefinition, AutoScalingActivityHandler, ?> activityExecution) {
        super(activityExecution);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .skipWritingOperationExecutionRecords(false); // this is to be reconsidered
    }

    @Override
    public void beforeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        XMLGregorianCalendar now = getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar();
        latch = new ReconciliationLatch(getActivity(), getActivityState(), now);
        latch.determineSituation(opResult);
    }

    @Override
    public ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult result) throws CommonException {
        PrismContext prismContext = getBeans().prismContext;

        if (!latch.isShouldReconcileTasks()) {
            return prismContext.queryFor(TaskType.class)
                    .none()
                    .build();
        }

        // We select all running tasks that have some children.
        // TODO After autoscaling flag and scheduling state are indexed, we should use them in this filter.
        ObjectFilter reconcilableTasksFilter = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStateType.RUNNING)
                .buildFilter();

        ObjectQuery objectQuery = ObjectQueryUtil.addConjunctions(configuredQuery, prismContext, reconcilableTasksFilter);

        LOGGER.info("Going to reconcile workers for tasks using a query of {}", objectQuery);
        return objectQuery;
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(
            Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult result) throws CommonException {
        return GetOperationOptions.updateToNoFetch(configuredOptions);
    }

    @Override
    public boolean processObject(@NotNull PrismObject<TaskType> object,
            @NotNull ItemProcessingRequest<PrismObject<TaskType>> request, RunningTask workerTask, OperationResult result)
            throws CommonException {

        TaskType task = object.asObjectable();
        if (!shouldReconcileTask(task, result)) {
            return true;
        }

        LOGGER.debug("Going to reconcile workers for task {}", task);
        getActivityHandler().activityManager.reconcileWorkers(task.getOid(), result);
        return true;
    }

    private boolean shouldReconcileTask(TaskType task, OperationResult result) {
        if (task.getSchedulingState() != TaskSchedulingStateType.WAITING) {
            result.recordNotApplicable("Task is not waiting for children");
            return false;
        }

        if (TaskTypeUtil.isAutoScalingDisabled(task)) {
            result.recordNotApplicable("Auto-scaling for this task is disabled");
            return false;
        }

        return true;
    }

    @Override
    public void afterExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        latch.updateActivityState(result);
    }
}
