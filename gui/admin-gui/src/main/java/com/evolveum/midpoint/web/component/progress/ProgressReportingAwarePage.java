/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.gui.impl.page.admin.component.ProgressPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.Collection;

/**
 * A page that supports progress reporting, e.g. page for editing users, orgs, roles.
 * <p>
 * Main responsibility of such a page is to correctly finish processing an operation
 * that could have been executed asynchronously.
 */
public interface ProgressReportingAwarePage {

    void startProcessing(AjaxRequestTarget target, OperationResult result);

    void finishProcessing(AjaxRequestTarget target, Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas, boolean returningFromAsync, OperationResult result);

    void continueEditing(AjaxRequestTarget target);

}
