/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;

/**
 * A page that supports progress reporting, e.g. page for editing users, orgs, roles.
 *
 * Main responsibility of such a page is to correctly finish processing an operation
 * that could have been executed asynchronously.
 *
 * The interface also contains some methods common to all midPoint pages that are needed in this package.
 * (Should be done seriously, some day...)
 *
 * @author mederly
 */
public interface ProgressReportingAwarePage {

	void startProcessing(AjaxRequestTarget target, OperationResult result);

    void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync);

    // things from PageBase (todo factor this out eventually)

    ModelService getModelService();

    ModelInteractionService getModelInteractionService();

    SecurityEnforcer getSecurityEnforcer();
    
    SecurityContextManager getSecurityContextManager();

    Task createSimpleTask(String name);

    WebApplicationConfiguration getWebApplicationConfiguration();

    WebMarkupContainer getFeedbackPanel();

	void continueEditing(AjaxRequestTarget target);
}
