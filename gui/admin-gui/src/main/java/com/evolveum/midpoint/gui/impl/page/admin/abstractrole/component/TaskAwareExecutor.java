/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.util.exception.NotLoggedInException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.restartOnLoginPageException;

/**
 * Executes arbitrary code that needs a {@link Task} and an {@link OperationResult}.
 *
 * Creates a task, executes the code, and then handles any errors.
 * Displays the operation result.
 *
 * Created to avoid repeating these actions throughout GUI code.
 *
 * NOT SERIALIZABLE, so please do not store directly or indirectly in Wicket components.
 */
@Experimental
public class TaskAwareExecutor {

    @NotNull private final PageBase pageBase;
    @NotNull private final AjaxRequestTarget ajaxRequestTarget;
    @NotNull private final String operation;

    /** If true, only non-success and non-in-progress status are shown (in feedback panel). */
    private boolean hideSuccessfulStatus;

    public TaskAwareExecutor(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget ajaxRequestTarget,
            @NotNull String operation) {
        this.pageBase = pageBase;
        this.ajaxRequestTarget = ajaxRequestTarget;
        this.operation = operation;
    }

    public TaskAwareExecutor hideSuccessfulStatus() {
        hideSuccessfulStatus = true;
        return this;
    }

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
        if ((!result.isSuccess() && !result.isInProgress()) || !hideSuccessfulStatus) {
            pageBase.showResult(result);
            ajaxRequestTarget.add(pageBase.getFeedbackPanel());
        }
        return returnValue;
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
