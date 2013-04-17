/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processors;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import java.util.Map;

/**
 * Manages workflow-related aspects of a change.
 *
 * (1) recognizes the instance (instances) of given kind of change within model context
 * (2) processes the result of the workflow(s) started
 *
 * Currently, there are the following change processors implemented or planned:
 * - PrimaryUserChangeProcessor: manages approvals of changes of user objects (in model's primary stage)
 * - ResourceModificationProcessor: manages approvals of changes related to individual resources (in model's secondary stage) - planned
 *
 * TODO find a better name (ChangeHandler, ChangeCategory, ...)
 *
 * @author mederly
 */
public interface ChangeProcessor {

    /**
     * Processes workflow-related aspect of a model operation. Namely, tries to find whether user interaction is necessary,
     * and arranges everything to carry out that interaction.
     *
     * @param context
     * @param task
     * @param result
     * @return non-null value if it processed the request;
     *              BACKGROUND = the process continues in background,
     *              FOREGROUND = nothing was left background, the model operation should continue in foreground,
     *              ERROR = something wrong has happened, there's no point in continuing with this operation.
     *         null if the request is not relevant to this processor
     */
    HookOperationMode startProcessesIfNeeded(ModelContext context, Task task, OperationResult result) throws SchemaException;

    /**
     * Handles an event from WfMS that indicates finishing of the workflow process instance.
     * Usually, at this point we see what was approved (and what was not) and continue with model operation(s).
     *
     * Should leave the task in saved state (if finishing successfully).
     *
     * @param event
     * @param task
     * @param result
     * @throws SchemaException
     */
    void finishProcess(ProcessEvent event, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;

    PrismObject<? extends ObjectType> getRequestSpecificData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result);
}
