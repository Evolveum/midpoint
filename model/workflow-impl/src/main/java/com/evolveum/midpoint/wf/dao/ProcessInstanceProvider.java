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

package com.evolveum.midpoint.wf.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WorkflowServiceImpl;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.ActivitiEngineDataHelper;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.query.Query;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

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

    private static final String DOT_CLASS = WorkflowServiceImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowService.class.getName() + ".";

    private static final String OPERATION_COUNT_PROCESS_INSTANCES_RELATED_TO_USER = DOT_INTERFACE + "countProcessInstancesRelatedToUser";
    private static final String OPERATION_LIST_PROCESS_INSTANCES_RELATED_TO_USER = DOT_INTERFACE + "listProcessInstancesRelatedToUser";
    private static final String OPERATION_GET_PROCESS_INSTANCE_BY_TASK_ID = DOT_INTERFACE + "getProcessInstanceByTaskId";
    private static final String OPERATION_GET_PROCESS_INSTANCE_BY_INSTANCE_ID = DOT_INTERFACE + "getProcessInstanceByInstanceId";
    private static final String OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE = DOT_CLASS + "activitiToMidpointRunningProcessInstance";
    private static final String OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE_HISTORY = DOT_CLASS + "activitiToMidpointProcessInstanceHistory";

    /*
     * ========================= PART 1 - main operations =========================
     */

    public int countProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, OperationResult parentResult) throws WorkflowException {
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
            throw new WorkflowException(m, e);
        }
    }

    public List<ProcessInstance> listProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished, int first, int count, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_LIST_PROCESS_INSTANCES_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("requestedBy", requestedBy);
        result.addParam("requestedFor", requestedFor);
        result.addParam("finished", finished);
        result.addParam("first", first);
        result.addParam("count", count);
        try {
            List<?> instances = createQueryForProcessInstancesRelatedToUser(userOid, requestedBy, requestedFor, finished).listPage(first, count);
            List<ProcessInstance> mInstances = finished ?
                    activitiToMidpointHistoricProcessInstanceList((List<HistoricProcessInstance>) instances, result) :
                    activitiToMidpointRunningProcessInstanceList((List<org.activiti.engine.runtime.ProcessInstance>) instances, false, result); // false = no work items
            result.recordSuccessIfUnknown();
            return mInstances;
        } catch (ActivitiException e) {
            String m = "Couldn't list process instances related to " + userOid + " due to Activiti exception";
            result.recordFatalError(m, e);
            throw new WorkflowException(m, e);
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

    public ProcessInstance getProcessInstanceByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_PROCESS_INSTANCE_BY_TASK_ID);
        result.addParam("taskId", taskId);
        Task task = activitiEngineDataHelper.getTaskById(taskId, result);
        return getProcessInstanceByInstanceIdInternal(task.getProcessInstanceId(), false, true, result);       // true = load also work items
    }

    public ProcessInstance getProcessInstanceByInstanceId(String instanceId, boolean historic, boolean getWorkItems, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_PROCESS_INSTANCE_BY_INSTANCE_ID);
        result.addParam("instanceId", instanceId);
        result.addParam("historic", historic);
        return getProcessInstanceByInstanceIdInternal(instanceId, historic, getWorkItems, result);
    }

    private ProcessInstance getProcessInstanceByInstanceIdInternal(String instanceId, boolean historic, boolean getWorkItems, OperationResult result) throws ObjectNotFoundException, WorkflowException {

        if (historic) {
            HistoricProcessInstanceQuery hpiq = activitiEngine.getHistoryService().createHistoricProcessInstanceQuery();
            hpiq.processInstanceId(instanceId);
            HistoricProcessInstance historicProcessInstance = hpiq.singleResult();
            if (historicProcessInstance != null) {
                ProcessInstance retval = activitiToMidpointProcessInstanceHistory(historicProcessInstance, result);
                result.computeStatus();
                return retval;
            } else {
                result.recordFatalError("Process instance " + instanceId + " couldn't be found.");
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            }
        } else {
            ProcessInstanceQuery piq = activitiEngine.getRuntimeService().createProcessInstanceQuery();
            piq.processInstanceId(instanceId);
            org.activiti.engine.runtime.ProcessInstance instance = piq.singleResult();

            if (instance != null) {
                ProcessInstance retval = activitiToMidpointRunningProcessInstance(instance, getWorkItems, result);
                result.computeStatus();
                return retval;
            } else {
                result.recordFatalError("Process instance " + instanceId + " couldn't be found.");
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            }
        }
    }

    /*
     * ========================= PART 2 - activiti to midpoint converters =========================
     *
     * getWorkItems parameter influences whether we want to get also work items for the particular process instance
     * (may be quite slow to execute).
     *
     */

    private List<ProcessInstance> activitiToMidpointRunningProcessInstanceList(List<org.activiti.engine.runtime.ProcessInstance> instances, boolean getWorkItems, OperationResult result) {
        List<ProcessInstance> retval = new ArrayList<ProcessInstance>();
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

    private List<ProcessInstance> activitiToMidpointHistoricProcessInstanceList(List<HistoricProcessInstance> instances, OperationResult result) throws WorkflowException {
        List<ProcessInstance> retval = new ArrayList<ProcessInstance>();
        for (HistoricProcessInstance instance : instances) {
            retval.add(activitiToMidpointProcessInstanceHistory(instance, result));
            if (!result.getLastSubresult().isSuccess()) {
                // this is a design decision: when an error occurs when listing instances, the ones that are fine WILL BE displayed
                LOGGER.error("Couldn't get information on workflow process instance", result.getLastSubresult().getMessage());
                // operation result already contains the exception information
            }
        }
        if (result.isUnknown()) {
            result.computeStatus();
        }
        return retval;
    }

    private ProcessInstance activitiToMidpointRunningProcessInstance(org.activiti.engine.runtime.ProcessInstance instance, boolean getWorkItems, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE);
        result.addParam("instance id", instance.getProcessInstanceId());
        result.addParam("getWorkItems", getWorkItems);
        ProcessInstance pi = new ProcessInstance();
        pi.setFinished(false);
        pi.setProcessId(instance.getProcessInstanceId());

        RuntimeService rs = activitiEngine.getRuntimeService();

        Map<String,Object> vars = null;
        try {
            vars = rs.getVariables(instance.getProcessInstanceId());
            pi.setVariables(vars);
            pi.setName((String) vars.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME));
            pi.setStartTime((Date) vars.get(CommonProcessVariableNames.VARIABLE_START_TIME));
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't get process instance variables for instance " + instance.getProcessInstanceId(), e);

            pi.setName("(unreadable process instance with id = " + instance.getId() + ")");
            pi.setStartTime(null);
            pi.setVariables(new HashMap<String,Object>());      // not to get NPEs

            return pi;
        }

        if (getWorkItems) {
            TaskService ts = activitiEngine.getTaskService();
            List<Task> tasks = ts.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).list();
            pi.setWorkItems(workItemProvider.tasksToWorkItems(tasks, false, true, result));     // "no" to task forms, "yes" to assignee details
        }

        result.recordSuccessIfUnknown();
        return pi;
    }

    public ProcessInstance activitiToMidpointProcessInstanceHistory(HistoricProcessInstance instance, OperationResult parentResult)  {

        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE_HISTORY);

        ProcessInstance pi = new ProcessInstance();
        pi.setFinished(true);
        pi.setProcessId(instance.getId());
        pi.setStartTime(instance.getStartTime());
        pi.setEndTime(instance.getEndTime());

        Exception exception;

        try {
            Map<String,Object> vars = activitiEngineDataHelper.getHistoricVariables(instance.getId(), result);
            pi.setVariables(vars);
            pi.setName((String) vars.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME));
            result.recordSuccessIfUnknown();
            return pi;
        } catch (RuntimeException e) {
            exception = e;
        } catch (WorkflowException e) {
            exception = e;
        }

        result.recordFatalError("Couldn't get information about finished process instance " + instance.getId(), exception);
        pi.setName("(unreadable process instance with id = " + instance.getId() + ")");
        pi.setVariables(new HashMap<String,Object>());      // not to get NPEs
        return pi;
    }
}
