/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A page that supports progress reporting, e.g. page for editing users, orgs, roles.
 * <p>
 * Main responsibility of such a page is to correctly finish processing an operation
 * that could have been executed asynchronously.
 */
public interface ProgressReportingAwarePage {

    ProgressPanel startAndGetProgressPanel(AjaxRequestTarget target, OperationResult result);

    void finishProcessing(AjaxRequestTarget target, boolean returningFromAsync, OperationResult result);

    void continueEditing(AjaxRequestTarget target);

    TaskManager getTaskManager();

    ModelInteractionService getModelInteractionService();

    PrismContext getPrismContext();
}
