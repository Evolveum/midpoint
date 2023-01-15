/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityExecutionModeDefinition;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Helps with managing simulation aspects of the activity.
 *
 * Separate in order to document the handling of these aspects at single place.
 */
class SimulationSupport {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationSupport.class);

    @NotNull private final AbstractActivityRun<?, ?, ?> activityRun;

    SimulationSupport(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    /** Creates the simulation result for the current activity. Assumed to be called once per realization. */
    void createSimulationResult(OperationResult result) throws ActivityRunException {
        if (!activityRun.getExecutionModeDefinition().shouldCreateSimulationResult()) {
            return;
        }
        var activityState = activityRun.getActivityState();
        if (activityState.getSimulationResultRef() != null) {
            LOGGER.warn("Simulation result is already present for {} - even at the start of the realization", activityRun);
            return;
        }

        ObjectReferenceType simResultRef = null;
        for (ActivityState state : activityState.getActivityStatesUpwardsForParent(result)) {
            simResultRef = state.getSimulationResultRef();
            if (simResultRef != null) {
                LOGGER.trace("Simulation result present in an ancestor activity state {} -> reusing it: {}", state, simResultRef);
                simResultRef = simResultRef.clone(); // because we need want to store it in activity state below
                break;
            }
        }
        if (simResultRef == null) {
            simResultRef = activityRun.getBeans().getAdvancedActivityRunSupport().createSimulationResult(result);
            LOGGER.trace("Created a simulation result: {}", simResultRef);
        }

        // We put the simulation result into the current activity to allow fast retrieval when processing the objects,
        // see getObjectProcessingListener().
        activityState.setSimulationResultRef(simResultRef);
        activityState.flushPendingTaskModificationsChecked(result);
    }

    /**
     * Creates "object processing listener" that will record data into the simulation result object.
     */
    AggregatedObjectProcessingListener getObjectProcessingListener() {
        ActivityExecutionModeDefinition modeDef = activityRun.getExecutionModeDefinition();
        if (modeDef.getMode() != ExecutionModeType.PREVIEW || !modeDef.shouldCreateSimulationResult()) {
            LOGGER.trace("Not creating object processing listener; mode definition = {}", modeDef);
            return null;
        }
        ObjectReferenceType simulationResultRef = activityRun.activityState.getSimulationResultRef();
        LOGGER.trace("Existing simulation result ref: {}", simulationResultRef);
        stateCheck(simulationResultRef != null,
                "No simulation result reference in %s even if simulation was requested", this);
        return activityRun.getBeans().getAdvancedActivityRunSupport().getObjectProcessingListener(simulationResultRef);
    }
}
