/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.ActivityExecutionModeDefinition;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationProcessedObjectListener;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Helps with managing simulation aspects of the activity.
 *
 * Separate in order to document the handling of these aspects at single place.
 */
class SimulationSupport {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationSupport.class);

    @NotNull private final AbstractActivityRun<?, ?, ?> activityRun;
    @NotNull private final AdvancedActivityRunSupport advancedActivityRunSupport;

    SimulationSupport(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
        this.advancedActivityRunSupport = activityRun.getBeans().getAdvancedActivityRunSupport();
    }

    /** Creates the simulation result for the current activity. Assumed to be called once per realization. */
    void getOrCreateSimulationResult(OperationResult result) throws ActivityRunException {
        if (!activityRun.getExecutionModeDefinition().shouldCreateSimulationResult()) {
            return;
        }
        var activityState = activityRun.getActivityState();
        if (activityState.getSimulationResultRef() != null) {
            LOGGER.warn("Simulation result is already present for {} - even at the start of the realization", activityRun);
            return;
        }

        String simResultOid = null;
        for (ActivityState state : activityState.getActivityStatesUpwardsForParent(result)) {
            simResultOid = state.getSimulationResultOid();
            if (simResultOid != null) {
                LOGGER.trace("Simulation result present in an ancestor activity state {} -> reusing it: {}", state, simResultOid);
                break;
            }
        }
        if (simResultOid == null) {
            try {
                ActivityExecutionModeDefinition execModeDef = activityRun.getActivityDefinition().getExecutionModeDefinition();
                simResultOid =
                        advancedActivityRunSupport.openNewSimulationResult(
                                execModeDef.getSimulationDefinition(),
                                activityRun.getRunningTask().getRootTaskOid(),
                                execModeDef.getConfigurationSpecification(),
                                result);
            } catch (ConfigurationException e) {
                throw new ActivityRunException("Couldn't create simulation result", FATAL_ERROR, PERMANENT_ERROR, e);
            }
            activityState.setSimulationResultCreated();
            LOGGER.trace("Created a simulation result: {}", simResultOid);
        } else {
            // TODO issue a warning when re-defining the simulation in a sub-activity
        }

        // We put the simulation result into the current activity to allow fast retrieval when processing the objects,
        // see getSimulationObjectProcessingListener().
        activityState.setSimulationResultOid(simResultOid);
        activityState.flushPendingTaskModificationsChecked(result);
    }

    /**
     * Creates "object processing listener" that will record data into the simulation result object.
     */
    SimulationProcessedObjectListener getSimulationProcessedObjectListener() {
        ActivityExecutionModeDefinition modeDef = activityRun.getExecutionModeDefinition();
        if (!modeDef.shouldCreateSimulationResult()) {
            LOGGER.trace("Not creating object processing listener; mode definition = {}", modeDef);
            return null;
        }
        String simulationResultOid = activityRun.activityState.getSimulationResultOid();
        String transactionId = getSimulationResultTxId();
        LOGGER.trace("Existing simulation result OID: {}, transaction ID: {}", simulationResultOid, transactionId);
        stateCheck(simulationResultOid != null,
                "No simulation result reference in %s even if simulation was requested", this);

        return advancedActivityRunSupport.getSimulationProcessedObjectListener(simulationResultOid, transactionId);
    }

    private int getBucketSequentialNumber() {
        WorkBucketType bucket = activityRun instanceof IterativeActivityRun ?
                ((IterativeActivityRun<?, ?, ?, ?>) activityRun).getBucket() : null;
        return bucket != null ? bucket.getSequentialNumber() : 0;
    }

    void closeSimulationResultIfOpenedHere(OperationResult result) throws ActivityRunException {
        if (activityRun.activityState.isSimulationResultCreated()) {
            String simulationResultOid = activityRun.activityState.getSimulationResultOid();
            stateCheck(simulationResultOid != null,
                    "No simulation result reference in %s even it should be there (created=true)", this);
            try {
                advancedActivityRunSupport.closeSimulationResult(simulationResultOid, activityRun.getRunningTask(), result);
            } catch (ObjectNotFoundException e) {
                throw new ActivityRunException("Couldn't close simulation result", FATAL_ERROR, PERMANENT_ERROR, e);
            }
        }
    }

    private String getSimulationResultTxId() {
        return activityRun.getActivityPath() + "#" + getBucketSequentialNumber();
    }

    void openSimulationResultTransaction(OperationResult result) {
        String resultOid = activityRun.activityState.getSimulationResultOid();
        if (resultOid != null) {
            advancedActivityRunSupport.openSimulationResultTransaction(resultOid, getSimulationResultTxId(), result);
        }
    }

    void commitSimulationResultTransaction(OperationResult result) {
        String resultOid = activityRun.activityState.getSimulationResultOid();
        if (resultOid != null) {
            advancedActivityRunSupport.commitSimulationResultTransaction(resultOid, getSimulationResultTxId(), result);
        }
    }
}
