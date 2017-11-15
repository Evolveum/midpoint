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

import com.evolveum.midpoint.schema.result.OperationResult;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * A page that supports progress reporting, e.g. page for editing users, orgs, roles.
 * <p>
 * Main responsibility of such a page is to correctly finish processing an operation
 * that could have been executed asynchronously.
 *
 * @author mederly
 */
public interface ProgressReportingAwarePage {

    void startProcessing(AjaxRequestTarget target, OperationResult result);

    void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync);

    void continueEditing(AjaxRequestTarget target);
}
