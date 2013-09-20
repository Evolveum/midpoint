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

package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.jobs.JobCreationInstruction;
import com.evolveum.midpoint.wf.messages.ProcessEvent;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import java.util.List;
import java.util.Map;

/**
 *
 * PrimaryApprovalProcessWrapper is an interface to (wrapper of) a specific kind of workflow process related
 * to primary-stage change approval. Examples of process wrappers:
 *  - AddRoleProcessWrapper
 *  - CreateUserProcessWrapper
 *  - ChangeAttributeXProcessWrapper (X is an attribute of a user)
 *  - ...
 *
 * It plays a role on these occasions:
 *  1) when a change arrives - process wrapper tries to recognize whether the change contains relevant
 *     delta(s); if so, it prepares instruction(s) to start related workflow approval process(es)
 *  2) when a process instance finishes - process wrapper modifies the delta(s) related to particular
 *     process instance and passes them along, to be executed
 *  3) when a user wants to approve the item or asks about the state of process instance(s) -
 *     it prepares the data that is specific to individual process.
 *
 * @author mederly
 */
public interface PrimaryApprovalProcessWrapper {


    /**
     * Examines the change and determines whether there are pieces that require (change type specific)
     * approval, for example, if there are roles added.
     *
     * If yes, it takes these deltas out of the original change and prepares instruction(s) to start wf process(es).
     *
     * @param modelContext Original model context (e.g. to be able to get information about whole context of the operation)
     * @param change Change to be examined and modified (as a side effect!)
     * @param taskFromModel General context of the operation - the method should not modify the task.
     * @param result Operation result - the method should report any errors here (TODO what about creating subresults?)
     * @return list of start process instructions
     */
    List<JobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, ObjectDelta<? extends ObjectType> change, Task taskFromModel, OperationResult result) throws SchemaException;

    /**
     * Returns the name of process instance details GUI panel. (Currently not used.)
     * @return
     */
    String getProcessInstanceDetailsPanelName(ProcessInstance processInstance);

    // TODO (after this mark)
    //-------------------------------------------------------------------------------------

    List<ObjectDelta<Objectable>> prepareDeltaOut(ProcessEvent event, PrimaryChangeProcessorJob job, OperationResult result) throws SchemaException;

    PrimaryChangeProcessor getChangeProcessor();

    void setChangeProcessor(PrimaryChangeProcessor changeProcessor);

    PrismObject<? extends ObjectType> getRequestSpecificData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException;

    PrismObject<? extends ObjectType> getAdditionalData(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException;

    List<ObjectReferenceType> getApprovedBy(ProcessEvent event);
}
