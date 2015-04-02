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

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.jobs.Job;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;

import javax.xml.bind.JAXBException;

import java.util.Map;

/**
 * A change processor can be viewed as a kind of framework supporting customer-specific
 * workflow code. Individual change processors are specialized in their areas, allowing
 * customer code to focus on business logic with minimal effort.
 *
 * The name "change processor" is derived from the fact that primary purpose of this
 * framework is to process change requests coming from the model.
 *
 * TODO find a better name
 *
 * However, a change processor has many more duties, e.g.
 *
 * (1) recognizes the instance (instances) of given kind of change within model operation context,
 * (2) processes the result of the workflow process instances when they are finished,
 * (3) presents (externalizes) the content of process instances to outside world: to the GUI, auditing, and notifications.
 *
 * Currently, there are the following change processors implemented or planned:
 * - PrimaryChangeProcessor: manages approvals of changes of objects (in model's primary stage)
 * - GeneralChangeProcessor: manages any change, as configured by the system engineer/administrator
 *
 * @author mederly
 */
public interface ChangeProcessor {

    /**
     * Processes workflow-related aspect of a model operation. Namely, tries to find whether user interaction is necessary,
     * and arranges everything to carry out that interaction.
     *
     * @param context Model context of the operation.
     * @param wfConfigurationType
     *@param taskFromModel Task in context of which the operation is carried out.
     * @param result Where to put information on operation execution.   @return non-null value if it processed the request;
     *              BACKGROUND = the process was "caught" by the processor, and continues in background,
     *              FOREGROUND = nothing was left on background, the model operation should continue in foreground,
     *              ERROR = something wrong has happened, there's no point in continuing with this operation.
     *         null if the request is not relevant to this processor
     *
     * Actually, the FOREGROUND return value is quite unusual, because the change processor cannot
     * know in advance whether other processors would not want to process the invocation from the model.
     */
    HookOperationMode processModelInvocation(ModelContext context, WfConfigurationType wfConfigurationType, Task taskFromModel, OperationResult result) throws SchemaException;

    /**
     * Handles an event from WfMS that indicates finishing of the workflow process instance.
     * Usually, at this point we see what was approved (and what was not) and continue with model operation(s).
     *
     * Should leave the task in saved state (if finishing successfully).
     *
     * @param event
     * @param task
     * @param result Here should be stored information about whether the finalization was successful or not
     * @throws SchemaException
     */
    void onProcessEnd(ProcessEvent event, Job job, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException;

    /**
     * Externalizes internal state of the process instance. Typically, uninteresting (auxiliary) data elements
     * are thrown away, internal representation suitable for workflow processing is replaced by "clean" prism
     * object structure, and untyped Map[String,Object] is replaced by typed prism data.
     *
     * @param variables internal process state represented by a map
     * @return external representation in the form of PrismObject
     */
    PrismObject<? extends ProcessInstanceState> externalizeProcessInstanceState(Map<String, Object> variables) throws JAXBException, SchemaException;

    /**
     * Prepares a displayable work item contents. For example, in case of primary change processor,
     * it returns a GeneralChangeApprovalWorkItemContents containing original object state
     * (objectOld), to-be object state (objectNew), delta, additional object, and a situation-specific
     * question form.
     *
     * @param task activiti task corresponding to the work item for which the contents is to be prepared
     * @param processInstanceVariables variables of the process instance of which this task is a part
     * @param result here the method stores its result
     * @return
     * @throws JAXBException
     * @throws ObjectNotFoundException
     * @throws SchemaException
     */
    PrismObject<? extends WorkItemContents> externalizeWorkItemContents(org.activiti.engine.task.Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException;

    /**
     * Prepares a process instance-related audit record.
     *
     * @param variables
     * @param job
     * @param stage
     * @param result
     * @return
     */
    AuditEventRecord prepareProcessInstanceAuditRecord(Map<String, Object> variables, Job job, AuditEventStage stage, OperationResult result);

    /**
     * Prepares a work item-related audit record.
     *
     * @param taskEvent
     * @param stage
     * @param result
     * @return
     */
    AuditEventRecord prepareWorkItemAuditRecord(TaskEvent taskEvent, AuditEventStage stage, OperationResult result) throws WorkflowException;

    /**
     * Auxiliary method to access autowired Spring beans from within non-spring java objects.
     *
     * @return
     */
    WorkflowManagerImpl getWorkflowManager();
}

