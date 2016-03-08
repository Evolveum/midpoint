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

package com.evolveum.midpoint.wf.impl.activiti.dao;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngineDataHelper;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_WORK_ITEM;

/**
 * @author mederly
 */
@Component
public class ProcessInstanceProvider {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProcessInstanceProvider.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private ActivitiEngineDataHelper activitiEngineDataHelper;

    @Autowired
    private WorkItemProvider workItemProvider;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private ProcessInterfaceFinder processInterfaceFinder;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_AUGMENT_TASK_OBJECT = DOT_INTERFACE + "augmentTaskObject";


    //region ========================= PART 1 - main operations =========================

    private WfContextType getWfContextType(String instanceId, boolean getWorkItems, OperationResult result)
            throws ObjectNotFoundException, WorkflowException, SchemaException {
        HistoricProcessInstance historicProcessInstance = getHistoricProcessInstance(instanceId);
        if (historicProcessInstance == null) {
            throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
        } else {
            return activitiToMidpointWfContextHistory(historicProcessInstance, getWorkItems, result);
        }
    }

    private HistoricProcessInstance getHistoricProcessInstance(String instanceId) {
        HistoricProcessInstanceQuery hpiq = activitiEngine.getHistoryService().createHistoricProcessInstanceQuery();
        hpiq.processInstanceId(instanceId);
        return hpiq.singleResult();
    }

    //endregion

    //region ========================= PART 2 - activiti to midpoint converters =========================
    //
    // getWorkItems parameter influences whether we want to get also work items for the particular process instance
    // (may be quite slow to execute).

    private WfContextType activitiToMidpointWfContextHistory(HistoricProcessInstance instance, boolean getWorkItems, OperationResult result)
            throws WorkflowException, SchemaException {

        WfContextType wfc = new WfContextType();
        wfc.setProcessInstanceId(instance.getId());
        wfc.setProcessInstanceName(instance.getName());
        wfc.setStartTimestamp(XmlTypeConverter.createXMLGregorianCalendar(instance.getStartTime()));
        wfc.setEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(instance.getEndTime()));

        final Map<String,Object> vars;
        try {
            vars = activitiEngineDataHelper.getHistoricVariables(instance.getId(), result);
        } catch (RuntimeException|WorkflowException e) {
            throw new WorkflowException("Couldn't get variables of process instance " + instance.getId(), e);
        }

        ProcessMidPointInterface pmi = processInterfaceFinder.getProcessInterface(vars);
        wfc.setAnswer(pmi.getAnswer(vars));
        wfc.setApproved(ApprovalUtils.approvalBooleanValue(wfc.getAnswer()));
        wfc.setState(pmi.getState(vars));

        LightweightObjectRef requesterRef = (LightweightObjectRef) vars.get(VARIABLE_REQUESTER_REF);
        if (requesterRef != null) {
            wfc.setRequesterRef(requesterRef.toObjectReferenceType());
        }

        LightweightObjectRef objectRef = (LightweightObjectRef) vars.get(VARIABLE_OBJECT_REF);
        if (objectRef != null) {
            wfc.setObjectRef(objectRef.toObjectReferenceType());
        }

        LightweightObjectRef targetRef = (LightweightObjectRef) vars.get(VARIABLE_TARGET_REF);
        if (targetRef != null) {
            wfc.setTargetRef(targetRef.toObjectReferenceType());
        }

        ChangeProcessor cp = wfConfiguration.findChangeProcessor((String) vars.get(CommonProcessVariableNames.VARIABLE_CHANGE_PROCESSOR));
        if (cp == null) {
            throw new SchemaException("No change processor information in process instance " + instance.getId());
        }
        wfc.setProcessorSpecificState(cp.externalizeProcessorSpecificState(vars));
        wfc.setProcessSpecificState(pmi.externalizeProcessSpecificState(vars));

        if (getWorkItems) {
            TaskService ts = activitiEngine.getTaskService();
            List<Task> tasks = ts.createTaskQuery()
                    .processInstanceId(instance.getId())
                    .list();
            wfc.getWorkItem().addAll(workItemProvider.tasksToWorkItemsNew(tasks, vars, true, true, result));     // "no" to task forms, "yes" to assignee and candidate details
        }

        return wfc;
    }

    //endregion
    //region ========================= PART 3 - task interface =========================

    public <T extends ObjectType> void augmentTaskObject(PrismObject<T> object,
            Collection<SelectorOptions<GetOperationOptions>> options, com.evolveum.midpoint.task.api.Task task, OperationResult parentResult) {
        final OperationResult result = parentResult.createSubresult(OPERATION_AUGMENT_TASK_OBJECT);
        result.addParam("taskOid", object.getOid());
        result.addCollectionOfSerializablesAsParam("options", options);

        if (!(object.asObjectable() instanceof TaskType)) {
            result.recordNotApplicableIfUnknown();
            return;
        }
        final TaskType taskType = (TaskType) object.asObjectable();

        try {
            if (taskType.getWorkflowContext() == null) {
                return;
            }
            String instanceId = taskType.getWorkflowContext().getProcessInstanceId();
            if (instanceId == null) {
                return;
            }
            boolean retrieveWorkItems = SelectorOptions.hasToLoadPath(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM), options);
            WfContextType wfContextType = getWfContextType(instanceId, retrieveWorkItems, result);
            taskType.setWorkflowContext(wfContextType);
        } catch (RuntimeException|SchemaException|ObjectNotFoundException|WorkflowException e) {
            result.recordFatalError(e.getMessage(), e);
            taskType.setFetchResult(result.createOperationResultType());
            LoggingUtils.logException(LOGGER, "Couldn't prepare wf-related information for {}", e, ObjectTypeUtil.toShortString(object));
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public <T extends ObjectType> void augmentTaskObjectList(SearchResultList<PrismObject<T>> list,
            Collection<SelectorOptions<GetOperationOptions>> options, com.evolveum.midpoint.task.api.Task task, OperationResult result) {
        for (PrismObject<T> object : list) {
            augmentTaskObject(object, options, task, result);
        }
    }

    //endregion
}
