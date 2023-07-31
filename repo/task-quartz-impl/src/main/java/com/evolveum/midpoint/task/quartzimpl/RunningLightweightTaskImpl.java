/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.RunningLightweightTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Running lightweight asynchronous task (LAT).
 */
public class RunningLightweightTaskImpl extends RunningTaskQuartzImpl implements RunningLightweightTask {

    private static final Trace LOGGER = TraceManager.getTrace(RunningLightweightTaskImpl.class);

    /**
     * Parent task. This must be a regular persistent task, not a LAT.
     */
    @NotNull private final RunningTaskQuartzImpl parent;

    /**
     * The code that should be run for asynchronous transient tasks.
     * (As opposed to asynchronous persistent tasks, where the handler is specified
     * via Handler URI in task prism object.)
     */
    @NotNull private final LightweightTaskHandler lightweightTaskHandler;

    /**
     * Future representing executing (or submitted-to-execution) lightweight task handler.
     *
     * Once set, remains set.
     *
     * Guarded by `this`.
     */
    private Future<?> lightweightHandlerFuture;

    /**
     * An indication whether lightweight handler is currently executing or not.
     * Used for waiting upon its completion (because java.util.concurrent facilities are not able
     * to show this for cancelled/interrupted tasks).
     */
    private volatile boolean lightweightHandlerExecuting;

    public RunningLightweightTaskImpl(@NotNull TaskManagerQuartzImpl taskManager, @NotNull PrismObject<TaskType> taskPrismObject,
            @NotNull Task rootTask, @NotNull RunningTaskQuartzImpl parent, @NotNull LightweightTaskHandler handler) {
        super(taskManager, taskPrismObject, rootTask, parent);
        this.parent = parent;
        lightweightTaskHandler = handler;
    }

    @Override
    public RunningTaskQuartzImpl getLightweightTaskParent() {
        return parent;
    }

    @Override
    public @NotNull LightweightTaskHandler getLightweightTaskHandler() {
        return lightweightTaskHandler;
    }

    private synchronized Future<?> getLightweightHandlerFuture() {
        return lightweightHandlerFuture;
    }

    @Override
    public synchronized boolean lightweightHandlerStartRequested() {
        return lightweightHandlerFuture != null;
    }

    @Override
    public synchronized void startLightweightHandler() {
        stateCheck(isTransient(),
                "An attempt to start LightweightTaskHandler in a persistent task %s", this);
        stateCheck(lightweightHandlerFuture == null,
                "Handler for the lightweight task %s has already been started", this);
        stateCheck(isRunnable(),
                "Handler for lightweight task %s couldn't be started because the task's state is %s",
                this, getExecutionState());

        Runnable r = () -> {
            LOGGER.debug("Lightweight task handler shell starting execution; task = {}", this);
            setupSecurityContext(taskResult);
            try {
                lightweightHandlerExecuting = true;
                setExecutingThread(Thread.currentThread());
                statistics.startOrRestartCollectingThreadLocalStatistics(beans.sqlPerformanceMonitorsCollection);
                beans.cacheConfigurationManager.setThreadLocalProfiles(getCachingProfiles());
                OperationResult.setThreadLocalHandlingStrategy(getOperationResultHandlingStrategyName());
                setExecutionState(TaskExecutionStateType.RUNNING);
                setNode(taskManager.getNodeId());
                lightweightTaskHandler.run(this);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Lightweight task handler has thrown an exception; task = {}", t, this);
            }
            beans.cacheConfigurationManager.unsetThreadLocalProfiles();
            setExecutingThread(null);
            setNode(null); // execution state is changed in .closeTask() below
            lightweightHandlerExecuting = false;
            try {
                beans.taskStateManager.closeTask(this, taskResult);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't correctly close task {}", t, this);
                setExecutionState(TaskExecutionStateType.CLOSED);
            }
            LOGGER.debug("Lightweight task handler shell finishing; task = {}", this);
        };

        lightweightHandlerFuture = beans.lightweightTaskManager.submit(r);
        LOGGER.debug("Lightweight task handler submitted to start; task = {}", this);
    }

    private void setupSecurityContext(OperationResult result) {
        try {
            // Task owner is cloned because otherwise we get CMEs when recomputing the owner user during login process
            beans.securityContextManager.setupPreAuthenticatedSecurityContext(
                    CloneUtil.clone(getOwner(result)),
                    result);
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't set up task security context {}", e, this);
            throw new SystemException(e.getMessage(), e);
        }
    }

    void waitForCompletion(OperationResult result) {
        Future<?> future = getLightweightHandlerFuture();
        if (future != null) {
            LOGGER.debug("Waiting for {} to complete.", this);
            try {
                future.get();
            } catch (CancellationException e) {
                // the Future was cancelled; however, the run() method may be still executing
                // we want to be sure it is already done
                while (lightweightHandlerExecuting) {
                    LOGGER.debug("{} was cancelled, waiting for its real completion.", this);
                    try {
                        //noinspection BusyWait
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        LOGGER.warn("Waiting for {} completion interrupted.", this);
                        break;
                    }
                }
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Exception while waiting for {} to complete.", t, this);
                result.recordWarning("Got exception while waiting for " + this + " to complete: " + t.getMessage(), t);
            }
            LOGGER.debug("Waiting for {} done.", this);
        } else {
            if (isRunnable()) {
                LOGGER.trace("Lightweight task handler for {} has not started yet; closing the task.", this);
            } else {
                LOGGER.warn("Lightweight task {} has not started yet and is in wrong state: {}/{}. Closing it.", this,
                        getExecutingThread(), getSchedulingState());
            }
            try {
                beans.taskStateManager.closeTask(this, result);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close transient task {}", e, this);
            }
            LOGGER.debug("Waiting for (not yet running) {} done.", this);
        }
    }

    public void cancel(boolean mayInterruptIfRunning) {
        Future<?> future = getLightweightHandlerFuture();
        if (future != null) {
            future.cancel(mayInterruptIfRunning);
        }
    }
}
