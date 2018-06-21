/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpWfTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 *
 * Change aspect deals with a given (elementary) kind of primary-stage change. Examples of change aspects:
 *  - AddRoleAssignmentAspect
 *  - CreateUserAspect
 *  - ChangeAttributeXAspect (X is an attribute of a user)
 *  - ...
 *
 * Change aspect plays a role on these occasions:
 * 1) When a change arrives - change aspect tries to recognize whether the change
 *    contains relevant delta(s); if so, it prepares instruction(s) to start related workflow approval process(es).
 * 2) When a process instance finishes, change aspect:
 *     - modifies the delta(s) related to particular process instance and passes them along, to be executed,
 *     - provides a list of approvers that is to be stored in modified object's metadata.
 * 3) When a user wants to work on his task, the change aspect prepares a form to be presented to the user.
 * 4) When a user asks about the state of process instance(s), the change aspect prepares that part of the
 *    answer that is specific to individual process.
 *
 * @author mederly
 */
public interface PrimaryChangeAspect {

    /**
     * Examines the change and determines whether there are pieces that require (change type specific)
     * approval, for example, if there are roles added.
     *
     * If yes, it takes these deltas out of the original change and prepares instruction(s) to start wf process(es).
     *
     * @param objectTreeDeltas Change to be examined and modified by implementation of this method
     * @param ctx
     * @param result Operation result - the method should report any errors here (TODO what about creating subresults?)
     * @return list of start process instructions  @see WfTaskCreationInstruction
     */
    @NotNull
    <T extends ObjectType> List<PcpChildWfTaskCreationInstruction<?>> prepareTasks(@NotNull ObjectTreeDeltas<T> objectTreeDeltas,
            ModelInvocationContext<T> ctx, @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException;

    /**
     * On process instance end, prepares deltaOut based in deltaIn and information gathered during approval process.
     *
     * @param event Current ProcessEvent providing information on what happened within wf process instance.
     * @param job Reference to a job (pair of process instance and a task) in which the event happened.
     * @param result Operation result - the method should report any errors here.
     * @return List of resulting object deltas. Typically, when approved, resulting delta is the same as delta that had to be approved,
     * and when rejected, the resulting delta list is empty. However, approver might requested a change in the delta, so the processing
     * here may be more complex.
     * @throws SchemaException if there is any problem with the schema.
     */
    ObjectTreeDeltas prepareDeltaOut(ProcessEvent event, PcpWfTask job, OperationResult result) throws SchemaException;

    /**
     * Returns a list of users who have approved the particular request. This information is then stored in the task by the wf module,
     * and eventually fetched from there and put into metadata (createApproverRef/modifyApproverRef) by the model ChangeExecutor.
     *
     * However, information about the approvers is process-specific. Default implementation of this method in BasePrimaryChangeAspect corresponds
     * to behavior of general ItemApproval process.
     *
     * @param event Current ProcessEvent providing information on what happened within wf process instance.
     * @param job Reference to a job (pair of process instance and a task) in which the event happened.
     * @param result Operation result - the method should report any errors here.
     * @return List of references to approvers that approved this request.
     */
    List<ObjectReferenceType> prepareApprovedBy(ProcessEvent event, PcpWfTask job, OperationResult result);

    /**
     * Returns true if this aspect is enabled by default, i.e. even if not listed in primary change processor configuration.
     */
    boolean isEnabledByDefault();

    boolean isEnabled(PrimaryChangeProcessorConfigurationType processorConfigurationType);

    String getBeanName();
}
