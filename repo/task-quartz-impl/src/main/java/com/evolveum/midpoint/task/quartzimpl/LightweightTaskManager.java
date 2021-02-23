package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.LightweightTaskHandler;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Manages lightweight asynchronous tasks.
 *
 * TODO finish revision of this class
 */
@Component
public class LightweightTaskManager {

    private static final Trace LOGGER = TraceManager.getTrace(LightweightTaskManager.class);

    @Autowired
    @Qualifier("securityContextManager")
    private SecurityContextManager securityContextManager;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private TaskStateManager taskStateManager;
    @Autowired private LocalNodeState localNodeState;

    private final ExecutorService lightweightHandlersExecutor = Executors.newCachedThreadPool();

    void startLightweightTask(RunningTaskQuartzImpl task) {
        stateCheck(task.isTransient(),
                "An attempt to start LightweightTaskHandler in a persistent task %s", task);
        stateCheck(!task.lightweightHandlerStartRequested(),
                "Handler for the lightweight task %s has already been started", task);
        stateCheck(task.isRunnable(),
                "Handler for lightweight task %s couldn't be started because the task's state is %s",
                task, task.getExecutionState());

        final LightweightTaskHandler lightweightTaskHandler = task.getLightweightTaskHandler();
        if (lightweightTaskHandler == null) {
            LOGGER.trace("No lightweight task handler present in {}", task);
            return;
        }

        synchronized (task) {

            Runnable r = () -> {
                LOGGER.debug("Lightweight task handler shell starting execution; task = {}", task);
                setupSecurityContext(task, task.getResult());
                try {
                    task.setLightweightHandlerExecuting(true);
                    task.setExecutingThread(Thread.currentThread());
                    task.startCollectingLowLevelStatistics();
                    cacheConfigurationManager.setThreadLocalProfiles(task.getCachingProfiles());
                    OperationResult.setThreadLocalHandlingStrategy(task.getOperationResultHandlingStrategyName());
                    task.setExecutionState(TaskExecutionStateType.RUNNING);
                    lightweightTaskHandler.run(task);
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Lightweight task handler has thrown an exception; task = {}", t, task);
                }
                cacheConfigurationManager.unsetThreadLocalProfiles();
                task.setExecutingThread(null);
                task.setLightweightHandlerExecuting(false);
                try {
                    taskStateManager.closeTask(task, task.getResult());
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't correctly close task {}", t, task);
                }
                LOGGER.debug("Lightweight task handler shell finishing; task = {}", task);
            };

            Future<?> future = lightweightHandlersExecutor.submit(r);
            task.setLightweightHandlerFuture(future);
            LOGGER.debug("Lightweight task handler submitted to start; task = {}", task);
        }
    }

    private void setupSecurityContext(RunningTaskQuartzImpl task, OperationResult result) {
        try {
            // Task owner is cloned because otherwise we get CME's when recomputing the owner user during login process
            securityContextManager.setupPreAuthenticatedSecurityContext(CloneUtil.clone(task.getOwner(result)));
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't set up task security context {}", e, task);
            throw new SystemException(e.getMessage(), e);
        }
    }

    public void waitForTransientChildren(RunningTask task, OperationResult result) {
        for (RunningTaskQuartzImpl subtask : ((RunningTaskQuartzImpl) task).getRunningLightweightAsynchronousSubtasks()) {
            Future<?> future = subtask.getLightweightHandlerFuture();
            if (future != null) { // should always be
                LOGGER.debug("Waiting for subtask {} to complete.", subtask);
                try {
                    future.get();
                } catch (CancellationException e) {
                    // the Future was cancelled; however, the run() method may be still executing
                    // we want to be sure it is already done
                    while (subtask.isLightweightHandlerExecuting()) {
                        LOGGER.debug("Subtask {} was cancelled, waiting for its real completion.", subtask);
                        try {
                            //noinspection BusyWait
                            Thread.sleep(500);
                        } catch (InterruptedException e1) {
                            LOGGER.warn("Waiting for subtask {} completion interrupted.", subtask);
                            break;
                        }
                    }
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Exception while waiting for subtask {} to complete.", t, subtask);
                    result.recordWarning("Got exception while waiting for subtask " + subtask + " to complete: " + t.getMessage(), t);
                }
                LOGGER.debug("Waiting for subtask {} done.", subtask);
            }
        }
    }

    public Collection<TaskQuartzImpl> getTransientSubtasks(String identifier) {
        RunningTaskQuartzImpl runningInstance = localNodeState.getLocallyRunningTaskByIdentifier(identifier);
        if (runningInstance != null) {
            List<TaskQuartzImpl> subtasks = new ArrayList<>();
            for (RunningTaskQuartzImpl subtask : runningInstance.getLightweightAsynchronousSubtasks()) {
                subtasks.add(subtask.cloneAsStaticTask());
            }
            return subtasks;
        } else {
            return Collections.emptyList();
        }
    }
}
