/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.ActivityState;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandler;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.task.TaskExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;

/**
 * Base class for activity executions.
 *
 * @param <WD> Definition of the work that this activity has to do.
 */
public abstract class AbstractActivityExecution<
        WD extends WorkDefinition,
        AH extends ActivityHandler<WD, AH>,
        BS extends AbstractActivityWorkStateType> implements ActivityExecution {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractActivityExecution.class);

    /**
     * The task execution in context of which this activity execution takes place.
     */
    @NotNull protected final TaskExecution taskExecution;

    /**
     * Definition of the activity. Contains the definition of the work.
     */
    @NotNull protected final Activity<WD, AH> activity;

    /**
     * TODO
     */
    @NotNull private final ActivityState<BS> workState;

    @NotNull private final QName workStateTypeName;

    protected AbstractActivityExecution(@NotNull ExecutionInstantiationContext<WD, AH> context) {
        this.taskExecution = context.getTaskExecution();
        this.activity = context.getActivity();
        this.workStateTypeName = context.getActivity().getHandler().getWorkStateTypeName();
        this.workState = new ActivityState<>(this);
    }

    @NotNull
    @Override
    public TaskExecution getTaskExecution() {
        return taskExecution;
    }

    public @NotNull Activity<WD, AH> getActivity() {
        return activity;
    }

    public CommonTaskBeans getBeans() {
        return taskExecution.getBeans();
    }

    @Override
    public @NotNull ActivityExecutionResult execute(OperationResult result) throws ActivityExecutionException {

        workState.initialize(result);

        if (workState.isComplete()) {
            logComplete();
            return ActivityExecutionResult.finished();
        } else {
            logStart();

            ActivityExecutionResult executionResult = executeCatchingExceptions(result);
            logEnd(executionResult);

            if (executionResult.isFinished()) {
                workState.markComplete(result);
            }

            return executionResult;
        }
    }

    @NotNull
    private ActivityExecutionResult executeCatchingExceptions(OperationResult result) {
        ActivityExecutionResult executionResult;
        try {
            executionResult = executeInternal(result);
        } catch (ActivityExecutionException e) {
            executionResult = e.getActivityExecutionResult();
        } catch (Exception e) {
            LOGGER.warn("Unhandled exception in {}: {}", this, MiscUtil.getClassWithMessage(e));
            executionResult = ActivityExecutionResult.exception(PERMANENT_ERROR, e);
        }

        executionResult.markFinishedIfNoError();

        return executionResult;
    }

    private void logStart() {
        LOGGER.trace("Starting execution of activity with identifier '{}' and path '{}' (local: '{}') with work state "
                        + "prism item path: {}", activity.getIdentifier(), activity.getPath(), activity.getLocalPath(),
                workState.getItemPath());
    }

    private void logEnd(ActivityExecutionResult executionResult) {
        LOGGER.trace("Finished execution of activity with identifier '{}' and path '{}' (local: {}) with result: {}",
                activity.getIdentifier(), activity.getPath(), activity.getLocalPath(), executionResult);
    }

    private void logComplete() {
        LOGGER.trace("Skipped execution of activity with identifier '{}' and path '{}' (local: {}) as it was already executed",
                activity.getIdentifier(), activity.getPath(), activity.getLocalPath());
    }

    /**
     * Carries out the actual execution of this activity.
     */
    protected abstract @NotNull ActivityExecutionResult executeInternal(OperationResult result)
            throws ActivityExecutionException, CommonException;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "act=" + activity +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, getClass().getSimpleName(), indent);
        if (activity.isRoot()) {
            DebugUtil.debugDumpWithLabelLn(sb, "task execution", taskExecution.shortDump(), indent + 1);
        }
        return sb.toString();
    }

    public ActivityPath getLocalPath() {
        return activity.getLocalPath();
    }

    public ActivityPath getPath() {
        return activity.getPath();
    }

    public AbstractActivityExecution<?, ?, ?> getLocalParentExecution() {
        if (activity.isLocalRoot()) {
            return null;
        }

        Activity<?, ?> parentActivity = activity.getParent();
        if (parentActivity != null) {
            return parentActivity.getExecution();
        } else {
            return null;
        }
    }

    public @NotNull ActivityPath getActivityPath() {
        return activity.getPath();
    }

    public @NotNull AH getActivityHandler() {
        return activity.getHandler();
    }

    public @NotNull ActivityState<BS> getWorkState() {
        return workState;
    }

    public @NotNull RunningTask getRunningTask() {
        return taskExecution.getRunningTask();
    }

    public @NotNull QName getWorkStateTypeName() {
        return workStateTypeName;
    }
}
