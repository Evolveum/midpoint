/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.util.exception.NotLoggedInException;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.restartOnLoginPageException;

/**
 * Executes arbitrary code that needs a {@link Task} and an {@link OperationResult}.
 *
 * Creates a task, executes the code, and then handles any errors.
 * Displays the operation result.
 *
 * Created to avoid repeating these actions throughout GUI code.
 *
 * This class is NOT SERIALIZABLE (although it probably could be made so); neither the instances of {@link Executable}
 * and {@link ExecutableVoid} are. Please do not store these objects in Wicket components (directly or indirectly).
 *
 * TODO the treatment of "void" methods is not good now (two methods + ugly names); to be improved
 */
@Experimental
public class TaskAwareExecutor {

    @NotNull private final PageBase pageBase;
    @NotNull private final AjaxRequestTarget ajaxRequestTarget;
    @NotNull private final String operation;

    /** Options for displaying the operation result. */
    private OpResult.Options showResultOptions = OpResult.Options.create();

    /** The panel that has to be refreshed (after showing the result) if different from the default one. */
    private Component customFeedbackPanel;

    public TaskAwareExecutor(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget ajaxRequestTarget,
            @NotNull String operation) {
        this.pageBase = pageBase;
        this.ajaxRequestTarget = ajaxRequestTarget;
        this.operation = operation;
    }

    /** If true, only non-success status is shown (in feedback panel). */
    public TaskAwareExecutor hideSuccessfulStatus() {
        showResultOptions = showResultOptions.withHideSuccess(true);
        return this;
    }

    /** Options for rendering the operation result. */
    public TaskAwareExecutor withOpResultOptions(OpResult.Options options) {
        showResultOptions = options;
        return this;
    }

    /** If the feedback panel is different from the default one. */
    public TaskAwareExecutor withCustomFeedbackPanel(Component panel) {
        this.customFeedbackPanel = panel;
        return this;
    }

    /**
     * Returns `null` in case of error; even in cases where the original {@link Executable#execute(Task, OperationResult)}
     * _implementation_ is marked as `@NotNull`.
     */
    public <X> @Nullable X run(@NotNull Executable<X> executable) {
        var task = pageBase.createSimpleTask(operation);
        OperationResult result = task.getResult();
        X returnValue = null;
        try {
            returnValue = executable.execute(task, result);
        } catch (NotLoggedInException e) {
            throw restartOnLoginPageException();
        } catch (Throwable t) {
            result.recordException(t);
        } finally {
            result.close();
        }
        showResultIfNeeded(result);
        return returnValue;
    }

    private void showResultIfNeeded(OperationResult result) {
        if (result.isSuccess() && showResultOptions.hideSuccess()
            || result.isInProgress() && showResultOptions.hideInProgress()) {
            // This is checked also in pageBase.showResult, but we want to avoid the extra work if possible.
            return;
        }

        pageBase.showResult(result, null, showResultOptions);
        ajaxRequestTarget.add(
                Objects.requireNonNullElseGet(
                        customFeedbackPanel,
                        pageBase::getFeedbackPanel));
    }

    /** TODO better name */
    @SuppressWarnings("WeakerAccess")
    public void runVoid(@NotNull ExecutableVoid executable) {
        run((task, result) -> {
            executable.execute(task, result);
            return null; // ignored
        });
    }

    @FunctionalInterface
    public interface Executable<T> {
        T execute(@NotNull Task task, @NotNull OperationResult result) throws CommonException;
    }

    @FunctionalInterface
    public interface ExecutableVoid {
        void execute(@NotNull Task task, @NotNull OperationResult result) throws CommonException;
    }
}
