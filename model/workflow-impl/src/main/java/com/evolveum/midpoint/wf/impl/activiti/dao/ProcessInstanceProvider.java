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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngineDataHelper;

import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessInstanceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.query.Query;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.util.*;

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

    private static final String OPERATION_COUNT_PROCESS_INSTANCES_RELATED_TO_USER = DOT_INTERFACE + "countProcessInstancesRelatedToUser";
    private static final String OPERATION_LIST_PROCESS_INSTANCES_RELATED_TO_USER = DOT_INTERFACE + "listProcessInstancesRelatedToUser";
    private static final String OPERATION_GET_PROCESS_INSTANCE_BY_TASK_ID = DOT_INTERFACE + "getProcessInstanceByWorkItemId";
    private static final String OPERATION_GET_PROCESS_INSTANCE_BY_INSTANCE_ID = DOT_INTERFACE + "getProcessInstanceById";
    private static final String OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE = DOT_CLASS + "activitiToMidpointRunningProcessInstance";
    private static final String OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE_HISTORY = DOT_CLASS + "activitiToMidpointProcessInstanceHistory";
    private static final String OPERATION_AUGMENT_TASK_OBJECT = DOT_INTERFACE + "augmentTaskObject";


    //region ========================= PART 1 - main operations =========================

    public int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_COUNT_PROCESS_INSTANCES_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("requestedBy", requestedBy);
        result.addParam("requestedFor", requestedFor);
        result.addParam("finished", finished);
        try {
            int instances = (int) createQueryForProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished).count();
            result.recordSuccessIfUnknown();
            return instances;
        } catch (ActivitiException e) {
            String m = "Couldn't count process instances related to " + userOid + " due to Activiti exception";
            result.recordFatalError(m, e);
            throw new SystemException(m, e);
        }
    }

    public List<WfProcessInstanceType> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_LIST_PROCESS_INSTANCES_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("requestedBy", requestedBy);
        result.addParam("requestedFor", requestedFor);
        result.addParam("finished", finished);
        result.addParam("first", first);
        result.addParam("count", count);
        try {
            List<?> instances = createQueryForProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished).listPage(first, count);
            List<WfProcessInstanceType> mInstances = finished ?
                    activitiToMidpointHistoricProcessInstanceList((List<HistoricProcessInstance>) instances, result) :
                    activitiToMidpointRunningProcessInstanceList((List<org.activiti.engine.runtime.ProcessInstance>) instances, false, result); // false = no work items
            result.recordSuccessIfUnknown();
            return mInstances;
        } catch (ActivitiException e) {
            String m = "Couldn't list process instances related to " + userOid + " due to Activiti exception";
            result.recordFatalError(m, e);
            throw new SystemException(m, e);
        }
    }

    private Query<?,?> createQueryForProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished) {
        if (finished) {
            HistoryService hs = activitiEngine.getHistoryService();

            HistoricProcessInstanceQuery hpiq = hs.createHistoricProcessInstanceQuery().finished().orderByProcessInstanceEndTime().desc();
            if (requestedBy) {
                hpiq = hpiq.startedBy(userOid);
            }
            if (requestedFor) {
                hpiq = hpiq.variableValueEquals(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID, userOid);
            }
            return hpiq;
        } else {
            ProcessInstanceQuery piq = activitiEngine.getRuntimeService().createProcessInstanceQuery().orderByProcessInstanceId().asc();
            if (requestedBy) {
                piq = piq.variableValueEquals(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID, userOid);
            }
            if (requestedFor) {
                piq = piq.variableValueEquals(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID, userOid);
            }
            return piq;
        }
    }

    @Deprecated
    public WfProcessInstanceType getProcessInstanceByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_PROCESS_INSTANCE_BY_TASK_ID);
        result.addParam("taskId", taskId);
        Task task = activitiEngineDataHelper.getTaskById(taskId, result);
        return getProcessInstanceByInstanceIdInternal(task.getProcessInstanceId(), false, true, result);       // true = load also work items
    }

    @Deprecated
    public WfProcessInstanceType getProcessInstanceByInstanceId(String instanceId, Boolean historic, boolean getWorkItems, OperationResult parentResult) throws ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_PROCESS_INSTANCE_BY_INSTANCE_ID);
        result.addParam("instanceId", instanceId);
        result.addParam("historic", historic);
        try {
            return getProcessInstanceByInstanceIdInternal(instanceId, historic, getWorkItems, result);
        } catch (ObjectNotFoundException|RuntimeException e) {
            result.recordFatalError(e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Deprecated
    private WfProcessInstanceType getProcessInstanceByInstanceIdInternal(String instanceId, Boolean historic, boolean getWorkItems, OperationResult result) throws ObjectNotFoundException {
        if (historic == null) {
            org.activiti.engine.runtime.ProcessInstance instance = getProcessInstance(instanceId);
            if (instance == null) {
                HistoricProcessInstance historicProcessInstance = getHistoricProcessInstance(instanceId);
                if (historicProcessInstance == null) {
                    throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found (current nor historic).");
                } else {
                    return activitiToMidpointProcessInstanceHistory(historicProcessInstance, result);
                }
            } else {
                return activitiToMidpointRunningProcessInstance(instance, getWorkItems, result);
            }
        } else if (historic) {
            HistoricProcessInstance historicProcessInstance = getHistoricProcessInstance(instanceId);
            if (historicProcessInstance == null) {
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            } else {
                return activitiToMidpointProcessInstanceHistory(historicProcessInstance, result);
            }
        } else {
            org.activiti.engine.runtime.ProcessInstance instance = getProcessInstance(instanceId);
            if (instance == null) {
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            } else {
                return activitiToMidpointRunningProcessInstance(instance, getWorkItems, result);
            }
        }
    }

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

    private ProcessInstance getProcessInstance(String instanceId) {
        ProcessInstanceQuery piq = activitiEngine.getRuntimeService().createProcessInstanceQuery();
        piq.processInstanceId(instanceId);
        return piq.singleResult();
    }

    //endregion

    //region ========================= PART 2 - activiti to midpoint converters =========================
    //
    // getWorkItems parameter influences whether we want to get also work items for the particular process instance
    // (may be quite slow to execute).

    private List<WfProcessInstanceType> activitiToMidpointRunningProcessInstanceList(List<org.activiti.engine.runtime.ProcessInstance> instances, boolean getWorkItems, OperationResult result) {
        List<WfProcessInstanceType> retval = new ArrayList<WfProcessInstanceType>();
        int problems = 0;
        Exception lastException = null;
        for (org.activiti.engine.runtime.ProcessInstance instance : instances) {
            try {
                retval.add(activitiToMidpointRunningProcessInstance(instance, getWorkItems, result));
            } catch(Exception e) {      // todo: was WorkflowException
                problems++;
                lastException = e;
                // this is a design decision: when an error occurs when listing instances, the ones that are fine WILL BE displayed
                LoggingUtils.logException(LOGGER, "Couldn't get information on workflow process instance", e);
                // operation result already contains the exception information
            }
        }
        if (problems > 0) {
            result.recordWarning(problems + " active instance(s) could not be shown; last exception: " + lastException.getMessage(), lastException);
        }
        return retval;
    }

    private List<WfProcessInstanceType> activitiToMidpointHistoricProcessInstanceList(List<HistoricProcessInstance> instances, OperationResult result) {
        List<WfProcessInstanceType> retval = new ArrayList<WfProcessInstanceType>();
        for (HistoricProcessInstance instance : instances) {
            retval.add(activitiToMidpointProcessInstanceHistory(instance, result));
            if (!result.getLastSubresult().isSuccess()) {
                // this is a design decision: when an error occurs when listing instances, the ones that are fine WILL BE displayed
                LOGGER.error("Couldn't get information on workflow process instance", result.getLastSubresult().getMessage());
                // operation result already contains the exception information
            }
        }
        result.computeStatusIfUnknown();
        return retval;
    }

    private WfProcessInstanceType activitiToMidpointRunningProcessInstance(org.activiti.engine.runtime.ProcessInstance instance, boolean getWorkItems, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE);
        result.addParam("instance id", instance.getProcessInstanceId());
        result.addParam("getWorkItems", getWorkItems);

        WfProcessInstanceType pi = new WfProcessInstanceType();
        pi.setFinished(false);
        pi.setProcessInstanceId(instance.getProcessInstanceId());

        RuntimeService rs = activitiEngine.getRuntimeService();

        Map<String,Object> vars = null;
        try {
            vars = rs.getVariables(instance.getProcessInstanceId());
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't get process instance variables for instance " + instance.getProcessInstanceId(), e);
            LoggingUtils.logException(LOGGER, "Couldn't get process instance variables for instance {}", e, instance.getProcessInstanceId());
            pi.setName(new PolyStringType("(unreadable process instance with id = " + instance.getId() + ")"));
            pi.setStartTimestamp(null);
            return pi;
        }

        ChangeProcessor cp = wfConfiguration.findChangeProcessor((String) vars.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR));

        try {
            pi.setState(cp.externalizeProcessInstanceState(vars).asObjectable());
        } catch (JAXBException|SchemaException e) {
            result.recordFatalError("Couldn't externalize process instance state for instance " + instance.getProcessInstanceId(), e);
            LoggingUtils.logException(LOGGER, "Couldn't externalize process instance state for instance {}", e, instance.getProcessInstanceId());
        }

        pi.setName(new PolyStringType((String) vars.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME)));
        pi.setStartTimestamp(XmlTypeConverter.createXMLGregorianCalendar((Date) vars.get(CommonProcessVariableNames.VARIABLE_START_TIME)));

        if (getWorkItems) {
            TaskService ts = activitiEngine.getTaskService();
            List<Task> tasks = ts.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).list();
            pi.getWorkItems().addAll(workItemProvider.tasksToWorkItems(tasks, false, true, true, result));     // "no" to task forms, "yes" to assignee and candidate details
        }

        result.recordSuccessIfUnknown();
        return pi;
    }

    public WfProcessInstanceType activitiToMidpointProcessInstanceHistory(HistoricProcessInstance instance, OperationResult parentResult)  {

        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE_HISTORY);

        WfProcessInstanceType pi = new WfProcessInstanceType();
        pi.setFinished(true);
        pi.setProcessInstanceId(instance.getId());
        pi.setStartTimestamp(XmlTypeConverter.createXMLGregorianCalendar(instance.getStartTime()));
        pi.setEndTimestamp(XmlTypeConverter.createXMLGregorianCalendar(instance.getEndTime()));

        try {
            Map<String,Object> vars = activitiEngineDataHelper.getHistoricVariables(instance.getId(), result);
            pi.setName(new PolyStringType((String) vars.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME)));
            ChangeProcessor cp = wfConfiguration.findChangeProcessor((String) vars.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR));
            pi.setState(cp.externalizeProcessInstanceState(vars).asObjectable());
            result.recordSuccessIfUnknown();
            return pi;
        } catch (RuntimeException|WorkflowException|JAXBException|SchemaException e) {
            result.recordFatalError("Couldn't get information about finished process instance " + instance.getId(), e);
            pi.setName(new PolyStringType("(unreadable process instance with id = " + instance.getId() + ")"));
            return pi;
        }
    }

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

        ChangeProcessor cp = wfConfiguration.findChangeProcessor((String) vars.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR));
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
                throw new SchemaException("No process instance ID in workflow context");
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
