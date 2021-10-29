/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.cluster;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterStateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.HashSet;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAutoScalingWorkStateType.*;

/**
 * Describes the current situation regarding the cluster state and last reconciliation execution.
 *
 * We could have put these properties to {@link AutoScalingActivityRun} directly but these are
 * filled-in and used only in one specific moment, so we do not mix them with the rest of the class.
 */
class ReconciliationLatch {

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationLatch.class);

    @NotNull private final Activity<AutoScalingWorkDefinition, AutoScalingActivityHandler> activity;
    @NotNull private final CurrentActivityState<?> activityState;
    @NotNull private final AutoScalingWorkDefinition workDef;
    @NotNull private final XMLGregorianCalendar now;

    /** Cluster state determined from the activity state. */
    private final ClusterStateType lastClusterState;

    /** Current cluster state (nodes from repository). */
    private ClusterStateType currentClusterState;

    /**
     * Has cluster state changed? This is set to true also if the last state was not known.
     * (Although the standard initial case is recognized by {@link #lastReconciliationTimestamp} being null.)
     */
    private boolean clusterStateChanged;

    /** Last timestamp from the activity state. */
    @Nullable private final XMLGregorianCalendar lastReconciliationTimestamp;

    /** Are we running the first time? We know that from the {@link #lastReconciliationTimestamp}. */
    private final boolean initialRun;

    /** Has reconciliation been requested but is pending because of minimal interval? (From activity state.) */
    private final boolean reconciliationPending;

    /** Should we set "reconciliation pending" flag in the activity state? */
    private boolean setReconciliationPending;

    /** The verdict: should we reconcile tasks? */
    private boolean shouldReconcileTasks;

    ReconciliationLatch(@NotNull Activity<AutoScalingWorkDefinition, AutoScalingActivityHandler> activity,
            @NotNull CurrentActivityState<?> activityState, @NotNull XMLGregorianCalendar now) {
        this.activity = activity;
        this.activityState = activityState;
        this.workDef = activity.getWorkDefinition();
        this.now = now;
        this.lastClusterState = activityState.getWorkStateItemRealValueClone(F_LAST_CLUSTER_STATE, ClusterStateType.class);
        this.lastReconciliationTimestamp =
                activityState.getWorkStateItemRealValueClone(F_LAST_RECONCILIATION_TIMESTAMP, XMLGregorianCalendar.class);
        this.initialRun = lastReconciliationTimestamp == null;
        this.reconciliationPending = Boolean.TRUE.equals(
                activityState.getWorkStateItemRealValueClone(F_RECONCILIATION_PENDING, Boolean.class));
    }

    void determineSituation(OperationResult result) throws CommonException {

        currentClusterState = activity.getHandler().getModelBeans().taskManager.determineClusterState(result);
        clusterStateChanged = lastClusterState == null ||
                !new HashSet<>(lastClusterState.getNodeUp()).equals(new HashSet<>(currentClusterState.getNodeUp()));

        if (initialRun) {
            if (workDef.isSkipInitialReconciliation()) {
                LOGGER.info("Skipping initial workers reconciliation.");
            } else {
                LOGGER.info("Going to execute initial workers reconciliation.");
                shouldReconcileTasks = true;
            }
        } else if (maxIntervalReached()) {
            LOGGER.info("Maximum reconciliation interval has elapsed, starting workers reconciliation.");
            shouldReconcileTasks = true;
        } else if (reconciliationPending && minIntervalReached()) {
            LOGGER.info("Reconciliation was pending and required minimum interval was reached -> starting workers reconciliation.");
            shouldReconcileTasks = true;
        } else if (!clusterStateChanged) {
            LOGGER.debug("Cluster state has not changed, no need to reconcile workers now.");
        } else if (minIntervalReached()) {
            LOGGER.debug("Cluster state has changed, will reconcile workers.");
            shouldReconcileTasks = true;
        } else {
            LOGGER.info("Cluster state has changed but minimum reconciliation interval has not elapsed. "
                    + "Marking reconciliation as pending.");
            setReconciliationPending = true;
        }
    }

    private boolean maxIntervalReached() {
        return intervalReached(workDef.getMaxReconciliationInterval(), false);
    }

    private boolean intervalReached(Duration interval, boolean defaultValue) {
        if (interval == null) {
            return defaultValue;
        } else {
            return XmlTypeConverter.isAfterInterval(lastReconciliationTimestamp, interval, now);
        }
    }

    private boolean minIntervalReached() {
        return intervalReached(activity.getWorkDefinition().getMinReconciliationInterval(), true);
    }

    boolean isShouldReconcileTasks() {
        return shouldReconcileTasks;
    }

    /**
     * Note that we defer the state update to the moment after workers reconciliation is done.
     * This is to protect against missing updates due to fatal failures (like a node crash)
     * occurring during the reconciliation.
     */
    void updateActivityState(OperationResult result) throws CommonException {

        if (clusterStateChanged) {
            activityState.setWorkStateItemRealValues(F_LAST_CLUSTER_STATE, currentClusterState);
            activityState.setWorkStateItemRealValues(F_LAST_CLUSTER_STATE_CHANGE_TIMESTAMP, now);
        }

        if (shouldReconcileTasks) {
            activityState.setWorkStateItemRealValues(F_RECONCILIATION_PENDING, false);
            activityState.setWorkStateItemRealValues(F_LAST_RECONCILIATION_TIMESTAMP, now);
        } else {
            if (setReconciliationPending) {
                activityState.setWorkStateItemRealValues(F_RECONCILIATION_PENDING, true);
            }
            if (initialRun) {
                // We set the reconciliation timestamp even if we skipped initial reconciliation.
                // This is to allow further reconciliations e.g. after max interval was reached.
                activityState.setWorkStateItemRealValues(F_LAST_RECONCILIATION_TIMESTAMP, now);
            }
        }

        activityState.flushPendingTaskModifications(result);

        LOGGER.trace("Updating activity state was updated.");
    }
}
