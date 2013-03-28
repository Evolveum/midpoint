/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.processes.ProcessWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.FormType;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.*;
import org.activiti.engine.query.Query;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;

import javax.xml.namespace.QName;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * @author mederly
 */
public class WfDataAccessor {

    private static final transient Trace LOGGER = TraceManager.getTrace(WfDataAccessor.class);

    private static final char PROPERTY_TYPE_SEPARATOR_CHAR = '$';
    private static final char FLAG_SEPARATOR_CHAR = '#';

    private WorkflowManager workflowManager;
    private WfCore wfCore;
    private static final String DOT_CLASS = WfDataAccessor.class.getName() + ".";
    private static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_CLASS + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_CLASS + "deleteProcessInstance";
    private static final String OPERATION_COUNT_WORK_ITEMS_RELATED_TO_USER = DOT_CLASS + "countWorkItemsRelatedToUser";
    private static final String OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER = DOT_CLASS + "listWorkItemsRelatedToUser";
    private static final char FLAG_CLEAR_ON_ENTRY = 'C';
    private static final String OPERATION_COUNT_PROCESS_INSTANCES_RELATED_TO_USER = DOT_CLASS + "countProcessInstancesRelatedToUser";
    private static final String OPERATION_LIST_PROCESS_INSTANCES_RELATED_TO_USER = DOT_CLASS + "listProcessInstancesRelatedToUser";
    private static final String OPERATION_GET_WORK_ITEM_BY_TASK_ID = DOT_CLASS + "getWorkItemByTaskId";
    private static final String OPERATION_GET_PROCESS_INSTANCE_BY_TASK_ID = DOT_CLASS + "getProcessInstanceByTaskId";
    private static final String OPERATION_GET_PROCESS_INSTANCE_BY_INSTANCE_ID = DOT_CLASS + "getProcessInstanceByInstanceId";
    private static final String OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE = DOT_CLASS + "activitiToMidpointProcessInstance";
    private static final String OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE_HISTORY = DOT_CLASS + "activitiToMidpointProcessInstanceHistory";
    private static final String OPERATION_ACTIVITI_TASK_TO_WORK_ITEM = DOT_CLASS + "activitiTaskToWorkItem";
    private static final String OPERATION_APPROVE_OR_REJECT_WORK_ITEM = DOT_CLASS + "approveOrRejectWorkItem";

    public WfDataAccessor(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
        this.wfCore = workflowManager.getWfCore();
    }

    private ActivitiEngine getActivitiEngine() {
        return workflowManager.getActivitiEngine();
    }

    /*
    * Some externally-visible methods. These count* and list* methods are used by the GUI.
    */


    /*
     * Work items for user
     * ===================
     */

    /**
     * Counts Work Items related to a user.
     *
     * @param userOid OID of the user
     * @param assigned whether to count assigned (true) or assignable (false) work items
     * @param parentResult
     * @return number of relevant work items
     * @throws WorkflowException
     */
    public int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_COUNT_WORK_ITEMS_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("assigned", assigned);
        try {
            int count = (int) createQueryForTasksRelatedToUser(userOid, assigned).count();
            result.recordSuccess();
            return count;
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't count work items assigned/assignable to " + userOid, e);
            throw new WorkflowException("Couldn't count work items assigned/assignable to " + userOid + " due to Activiti exception", e);
        }
    }

    /**
     * Lists work items related to a user.
     *
     * @param userOid OID of the user
     * @param assigned whether to count assigned (true) or assignable (false) work items
     * @param first
     * @param count
     * @param parentResult
     * @return list of work items
     * @throws WorkflowException
     */
    public List<WorkItem> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("assigned", assigned);
        result.addParam("first", first);
        result.addParam("count", count);
        try {
            List<Task> tasks = createQueryForTasksRelatedToUser(userOid, assigned).listPage(first, count);
            result.recordSuccess();
            return tasksToWorkItems(tasks, false, result);
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't list work items assigned/assignable to " + userOid, e);
            throw new WorkflowException("Couldn't list work items assigned/assignable to " + userOid + " due to Activiti exception", e);
        }
    }

    private TaskQuery createQueryForTasksRelatedToUser(String oid, boolean assigned) {
        if (assigned) {
            return getActivitiEngine().getTaskService().createTaskQuery().taskAssignee(oid).orderByTaskCreateTime().desc();
        } else {
            return getActivitiEngine().getTaskService().createTaskQuery().taskCandidateUser(oid).orderByTaskCreateTime().desc();
        }
    }

    /*
    * Process instances for user
    * ==========================
    * (1) current
    * (2) historic (finished)
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
                    activitiToMidpointProcessInstancesHistory((List<HistoricProcessInstance>) instances, false, result) :
                    activitiToMidpointProcessInstances((List<org.activiti.engine.runtime.ProcessInstance>) instances, false, result);
            result.recomputeStatus();
            return mInstances;
        } catch (ActivitiException e) {
            String m = "Couldn't list process instances related to " + userOid + " due to Activiti exception";
            result.recordFatalError(m, e);
            throw new WorkflowException(m, e);
        }
    }

    private Query<?,?> createQueryForProcessInstancesRelatedToUser(String userOid, boolean requestedBy, boolean requestedFor, boolean finished) {
        if (finished) {
            HistoryService hs = getActivitiEngine().getHistoryService();

            HistoricProcessInstanceQuery hpiq = hs.createHistoricProcessInstanceQuery().processDefinitionKey("AddRoles").finished().orderByProcessInstanceEndTime().desc();
            if (requestedBy) {
                hpiq = hpiq.startedBy(userOid);
            }
            if (requestedFor) {
                hpiq = hpiq.processInstanceBusinessKey(userOid);
            }
            return hpiq;
        } else {
            ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery().processDefinitionKey("AddRoles").orderByProcessInstanceId().asc();
            if (requestedBy) {
                piq = piq.variableValueEquals(WfConstants.VARIABLE_MIDPOINT_REQUESTER_OID, userOid);
            }
            if (requestedFor) {
                piq = piq.processInstanceBusinessKey(userOid);
            }
            return piq;
        }
    }


    /*
     * Work Item and Process Instance by Activiti Task ID or Process Instance ID
     * =========================================================================
     */

    public WorkItemDetailed getWorkItemByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_WORK_ITEM_BY_TASK_ID);
        result.addParam("taskId", taskId);
        WorkItemDetailed retval = (WorkItemDetailed) taskToWorkItem(getTaskById(taskId, result), true, result);
        result.recordSuccessIfUnknown();
        return retval;
    }

    public ProcessInstance getProcessInstanceByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_PROCESS_INSTANCE_BY_TASK_ID);
        result.addParam("taskId", taskId);
        Task task = getTaskById(taskId, result);
        return getProcessInstanceByInstanceIdInternal(task.getProcessInstanceId(), false, false, result);
    }

    public ProcessInstance getProcessInstanceByInstanceId(String instanceId, boolean historic, boolean getProcessDetails, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_PROCESS_INSTANCE_BY_INSTANCE_ID);
        result.addParam("instanceId", instanceId);
        result.addParam("historic", historic);
        return getProcessInstanceByInstanceIdInternal(instanceId, historic, getProcessDetails, result);
    }

    private ProcessInstance getProcessInstanceByInstanceIdInternal(String instanceId, boolean historic, boolean getProcessDetails, OperationResult result) throws ObjectNotFoundException, WorkflowException {

        if (historic) {
            HistoricProcessInstanceQuery hpiq = getActivitiEngine().getHistoryService().createHistoricProcessInstanceQuery();
            hpiq.processInstanceId(instanceId);
            HistoricProcessInstance historicProcessInstance = hpiq.singleResult();
            if (historicProcessInstance != null) {
                return activitiToMidpointProcessInstanceHistory(historicProcessInstance, getProcessDetails, result);
            } else {
                result.recordFatalError("Process instance " + instanceId + " couldn't be found.");
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            }
        } else {
            ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery();
            piq.processInstanceId(instanceId);
            org.activiti.engine.runtime.ProcessInstance instance = piq.singleResult();

            if (instance != null) {
                return activitiToMidpointProcessInstance(instance, getProcessDetails, result);
            } else {
                result.recordFatalError("Process instance " + instanceId + " couldn't be found.");
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            }
        }
    }

    private Task getTaskById(String taskId, OperationResult result) throws ObjectNotFoundException {
        TaskService taskService = getActivitiEngine().getTaskService();
        TaskQuery tq = taskService.createTaskQuery();
        tq.taskId(taskId);
        Task task = tq.singleResult();
        if (task == null) {
            result.recordFatalError("Task with ID " + taskId + " does not exist.");
            throw new ObjectNotFoundException("Task with ID " + taskId + " does not exist.");
        } else {
            return task;
        }
    }

    private List<ProcessInstance> activitiToMidpointProcessInstances(List<org.activiti.engine.runtime.ProcessInstance> instances, boolean details, OperationResult result) {
        List<ProcessInstance> retval = new ArrayList<ProcessInstance>();
        int problems = 0;
        Exception lastException = null;
        for (org.activiti.engine.runtime.ProcessInstance instance : instances) {
            try {
                retval.add(activitiToMidpointProcessInstance(instance, details, result));
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

    private List<ProcessInstance> activitiToMidpointProcessInstancesHistory(List<HistoricProcessInstance> instances, boolean details, OperationResult result) throws WorkflowException {
        List<ProcessInstance> retval = new ArrayList<ProcessInstance>();
        int problems = 0;
        WorkflowException lastException = null;
        for (HistoricProcessInstance instance : instances) {
            try {
                retval.add(activitiToMidpointProcessInstanceHistory(instance, details, result));
            } catch(WorkflowException e) {
                problems++;
                lastException = e;
                // this is a design decision: when an error occurs when listing instances, the ones that are fine WILL BE displayed
                LoggingUtils.logException(LOGGER, "Couldn't get information on workflow process instance", e);
                // operation result already contains the exception information
            }
        }
        if (problems > 0) {
            result.recordWarning(problems + " finished instance(s) could not be shown; last exception: " + lastException.getMessage(), lastException);
        }
        return retval;
    }

    private ProcessInstance activitiToMidpointProcessInstance(org.activiti.engine.runtime.ProcessInstance instance, boolean getProcessDetails, OperationResult parentResult) throws WorkflowException {

        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE);
        result.addParam("instance id", instance.getProcessInstanceId());
        result.addParam("getProcessDetails", getProcessDetails);
        ProcessInstance pi = new ProcessInstance();
        pi.setProcessId(instance.getProcessInstanceId());

        RuntimeService rs = getActivitiEngine().getRuntimeService();

        Map<String,Object> vars = null;
        try {
            vars = rs.getVariables(instance.getProcessInstanceId());
            pi.setName((String) vars.get(WfConstants.VARIABLE_PROCESS_NAME));
            pi.setStartTime((Date) vars.get(WfConstants.VARIABLE_START_TIME));
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't get process instance variables for instance " + instance.getProcessInstanceId(), e);

            pi.setName("(unreadable process instance with id = " + instance.getId() + ")");
            pi.setStartTime(null);

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            pi.setDetails("Process instance details couldn't be found because of the following problem:\n" + sw.toString());

            return pi;

//            if (details) {
//                throw new WorkflowException("Couldn't get process instance variables for instance " + instance.getProcessInstanceId(), e);
//            }
        }

        if (getProcessDetails) {
            TaskService ts = getActivitiEngine().getTaskService();
            List<Task> tasks = ts.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).list();
            pi.setWorkItems(tasksToWorkItems(tasks, false, result));
            pi.setDetails(getProcessSpecificDetails(instance, vars, tasks, result));
        }

        result.recordSuccessIfUnknown();
        return pi;
    }

    private ProcessInstance activitiToMidpointProcessInstanceHistory(HistoricProcessInstance instance, boolean getProcessDetails, OperationResult parentResult) throws WorkflowException {

        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TO_MIDPOINT_PROCESS_INSTANCE_HISTORY);

        ProcessInstance pi = new ProcessInstance();
        pi.setProcessId(instance.getId());
        pi.setStartTime(instance.getStartTime());
        pi.setEndTime(instance.getEndTime());

        try {
            Map<String,Object> vars = getHistoricVariables(instance.getId(), result);
            pi.setName((String) vars.get(WfConstants.VARIABLE_PROCESS_NAME));
            if (getProcessDetails) {
                pi.setDetails(getProcessSpecificDetails(instance, vars, result));
            }
        } catch (Exception e) {     // todo: was: WorkflowException but there can be e.g. NPEs there
            result.recordFatalError("Couldn't get information about finished process instance " + instance.getId(), e);
            pi.setName("(unreadable process instance with id = " + instance.getId() + ")");

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            pi.setDetails("Process instance details couldn't be found because of the following problem:\n" + sw.toString());
        }

        return pi;
    }

    private Map<String, Object> getHistoricVariables(String pid, OperationResult result) throws WorkflowException {

        Map<String, Object> retval = new HashMap<String, Object>();

        // copied from ActivitiInterface!
        HistoryService hs = getActivitiEngine().getHistoryService();

        try {

            HistoricDetailQuery hdq = hs.createHistoricDetailQuery()
                .variableUpdates()
                .processInstanceId(pid)
                .orderByTime().desc();

            for (HistoricDetail hd : hdq.list())
            {
                HistoricVariableUpdate hvu = (HistoricVariableUpdate) hd;
                String name = hvu.getVariableName();
                Object value = hvu.getValue();
                if (!retval.containsKey(name)) {
                    retval.put(name, value);
                }
            }

            return retval;

        } catch (ActivitiException e) {
            String m = "Couldn't get variables for finished process instance " + pid;
            result.recordFatalError(m, e);
            throw new WorkflowException(m, e);
        }
    }

    private Map<String,Object> getProcessVariables(String taskId, OperationResult result) throws ObjectNotFoundException, WorkflowException {
        try {
            Task task = getTask(taskId);
            Map<String,Object> variables = getActivitiEngine().getProcessEngine().getRuntimeService().getVariables((task.getExecutionId()));
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Execution " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);
            }
            return variables;
        } catch (ActivitiException e) {
            String m = "Couldn't get variables for the process corresponding to task " + taskId;
            result.recordFatalError(m, e);
            throw new WorkflowException(m, e);
        }
    }

    private String getProcessSpecificDetails(org.activiti.engine.runtime.ProcessInstance instance, Map<String, Object> vars, List<Task> tasks, OperationResult result) throws WorkflowException {
        ProcessWrapper wrapper = wfCore.findProcessWrapper(vars, instance.getId(), result);
        String d = wrapper.getProcessSpecificDetails(instance, vars, tasks);
        result.recordSuccessIfUnknown();
        return d;
    }

    private String getProcessSpecificDetails(HistoricProcessInstance instance, Map<String, Object> vars, OperationResult result) throws WorkflowException {
        ProcessWrapper wrapper = wfCore.findProcessWrapper(vars, instance.getId(), result);
        String d = wrapper.getProcessSpecificDetails(instance, vars);
        result.recordSuccessIfUnknown();
        return d;
    }

    private List<WorkItem> tasksToWorkItems(List<Task> tasks, boolean getTaskDetails, OperationResult result) throws WorkflowException {
        List<WorkItem> retval = new ArrayList<WorkItem>();
        for (Task task : tasks) {
            retval.add(taskToWorkItem(task, getTaskDetails, result));
        }
        return retval;
    }

    // should not throw ActivitiException
    // returns WorkItem or WorkItemDetailed, based on the 'getTaskDetails' parameter value
    private WorkItem taskToWorkItem(Task task, boolean getTaskDetails, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
        result.addParam("task id", task.getId());
        result.addParam("getTaskDetails", getTaskDetails);
        WorkItem wi = getTaskDetails ? new WorkItemDetailed() : new WorkItem();
        wi.setTaskId(task.getId());
        wi.setAssignee(task.getAssignee());
        if (getTaskDetails) {
            wi.setAssigneeName(getUserNameByOid(task.getAssignee(), result));
        } else {
            wi.setAssigneeName(wi.getAssignee());
        }
        wi.setName(task.getName());
        wi.setProcessId(task.getProcessInstanceId());
        try {
            wi.setCandidates(getCandidatesAsString(task));
        } catch(ActivitiException e) {
            String m = "Couldn't get work item candidates for Activiti task " + task.getId();
            result.recordPartialError(m, e);
            LoggingUtils.logException(LOGGER, m, e);
        }
        wi.setCreateTime(task.getCreateTime());

        if (getTaskDetails) {
            try {
                Map<String,Object> variables = getProcessVariables(task.getId(), result);
                ((WorkItemDetailed) wi).setRequester((PrismObject<UserType>) variables.get(WfConstants.VARIABLE_MIDPOINT_REQUESTER));
                ((WorkItemDetailed) wi).setObjectOld((PrismObject<ObjectType>) variables.get(WfConstants.VARIABLE_MIDPOINT_OBJECT_BEFORE));
                ((WorkItemDetailed) wi).setObjectNew((PrismObject<ObjectType>) variables.get(WfConstants.VARIABLE_MIDPOINT_OBJECT_AFTER));
                ((WorkItemDetailed) wi).setRequestCommonData(getRequestCommon(task, result));
                ((WorkItemDetailed) wi).setRequestSpecificData(getRequestSpecific(task, variables, result));
                ((WorkItemDetailed) wi).setTrackingData(getTrackingData(task, variables, result));
                ((WorkItemDetailed) wi).setAdditionalData(getAdditionalData(task, variables, result));
            } catch (SchemaException e) {
                throw new SystemException("Got unexpected schema exception when preparing information on Work Item", e);
            } catch (ObjectNotFoundException e) {
                throw new SystemException("Got unexpected object-not-found exception when preparing information on Work Item; perhaps a task was deleted while we processed it.", e);
            }
        }

        result.recordSuccessIfUnknown();
        return wi;
    }

    // returns oid when user cannot be retrieved
    private String getUserNameByOid(String oid, OperationResult result) {
        try {
            PrismObject<UserType> user = workflowManager.getRepositoryService().getObject(UserType.class, oid, result);
            return user.asObjectable().getName().getOrig();
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details because it couldn't be found", e, oid);
            return oid;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details due to schema exception", e, oid);
            return oid;
        }
    }


//    private List<String> groupsForUser(String user) {
//        IdentityService identityService = getActivitiEngine().getIdentityService();
//        GroupQuery gq = identityService.createGroupQuery();
//        gq.groupMember(user);
//        List<String> groupNames = new ArrayList<String>();
//        List<Group> groups = gq.list();
//        LOGGER.trace("Activiti groups for " + user + ":");
//        for (Group g : groups) {
//            LOGGER.trace(" - group: id = " + g.getId() + ", name = " + g.getName());
//            groupNames.add(g.getId());
//        }
//        return groupNames;
//    }

//    public void claimWorkItem(WorkItem workItem, String userId, OperationResult result) {
//        TaskService taskService = getActivitiEngine().getTaskService();
//        taskService.claim(workItem.getTaskId(), userId);
//        result.recordSuccess();
//    }
//
//    public void releaseWorkItem(WorkItem workItem, OperationResult result) {
//        TaskService taskService = getActivitiEngine().getTaskService();
//        taskService.claim(workItem.getTaskId(), null);
//        result.recordSuccess();
//    }





















    private static final QName WORK_ITEM_NAME = new QName(SchemaConstants.NS_C, "WorkItem");
    private static final QName WORK_ITEM_TASK_ID = new QName(SchemaConstants.NS_C, "01: taskId");
    private static final QName WORK_ITEM_PROCESS_INSTANCE_ID = new QName(SchemaConstants.NS_C, "02: processInstanceId");
    private static final QName WORK_ITEM_EXECUTION_ID = new QName(SchemaConstants.NS_C, "03: executionId");
    private static final QName WORK_ITEM_TASK_NAME = new QName(SchemaConstants.NS_C, "name");
    private static final QName WORK_ITEM_TASK_OWNER = new QName(SchemaConstants.NS_C, "10: taskOwner");
    private static final QName WORK_ITEM_TASK_ASSIGNEE = new QName(SchemaConstants.NS_C, "11: taskAssignee");
    private static final QName WORK_ITEM_TASK_CANDIDATES = new QName(SchemaConstants.NS_C, "12: candidates");
    private static final QName WORK_ITEM_CREATED = new QName(SchemaConstants.NS_C, "created");
    private static final QName WORK_ITEM_PROCESS_DEFINITION_ID = new QName(SchemaConstants.NS_C, "04: processDefinitionId");
    private static final QName WORK_ITEM_WATCHER_OID = new QName(SchemaConstants.NS_C, "99: watcherServerTaskOid");


    private String getPropertyName(FormProperty formProperty) {
//        String id = formProperty.getId();
//        int i = id.indexOf(PROPERTY_TYPE_SEPARATOR_CHAR);
//        return i < 0 ? id : id.substring(0, i);
        return formProperty.getName();
    }

    private String getPropertyType(FormProperty formProperty) {
        String id = formProperty.getId();
        int i = id.indexOf(PROPERTY_TYPE_SEPARATOR_CHAR);
        return i < 0 ? null : id.substring(i+1);
    }

    private List<String> getCandidates(Task task) {

        List<String> retval = new ArrayList<String>();

        TaskService taskService = getActivitiEngine().getTaskService();

        List<IdentityLink> ils = taskService.getIdentityLinksForTask(task.getId());
        for (IdentityLink il : ils) {
            if ("candidate".equals(il.getType())) {
                if (il.getGroupId() != null) {
                    retval.add("G:" + il.getGroupId());
                }
                if (il.getUserId() != null) {
                    retval.add("U:" + il.getUserId());
                }
            }
        }

        return retval;
    }

    private String getCandidatesAsString(Task task) {

        StringBuilder retval = new StringBuilder();
        boolean first = true;
        for (String c : getCandidates(task)) {
            if (first) {
                first = false;
            } else {
                retval.append(", ");
            }
            retval.append(c);
        }
        return retval.toString();
    }

    /**
     * Approves or rejects a work item (without supplying any further information).
     *
     * @param taskId identifier of activiti task backing the work item
     * @param decision true = approve, false = reject
     * @param parentResult
     */
    public void approveOrRejectWorkItem(String taskId, boolean decision, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OPERATION_APPROVE_OR_REJECT_WORK_ITEM);
        result.addParam("taskId", taskId);
        result.addParam("decision", decision);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Approving/rejecting work item " + taskId);
            LOGGER.trace("Decision: " + decision);
        }

        FormService formService = getActivitiEngine().getFormService();
        Map<String,String> propertiesToSubmit = new HashMap<String,String>();
        TaskFormData data = getActivitiEngine().getFormService().getTaskFormData(taskId);

        // we hope that the form has a property named "decision"
        propertiesToSubmit.put(WfConstants.FORM_FIELD_DECISION, Boolean.toString(decision));
        formService.submitTaskFormData(taskId, propertiesToSubmit);

        result.recordSuccessIfUnknown();
    }

    // todo error reporting
    public void saveWorkItemPrism(String taskId, PrismObject specific, boolean decision, OperationResult result) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving work item " + taskId);
            LOGGER.trace("Decision: " + decision);
            LOGGER.trace("WorkItem form object (process-specific) = " + specific.debugDump());
        }

        FormService formService = getActivitiEngine().getFormService();
        Map<String,String> propertiesToSubmit = new HashMap<String,String>();
        TaskFormData data = getActivitiEngine().getFormService().getTaskFormData(taskId);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("# of form properties: " + data.getFormProperties().size());
        }

        for (FormProperty formProperty : data.getFormProperties()) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing property " + formProperty.getId() + ":" + formProperty.getName());
            }

            if (formProperty.isWritable()) {

                Object value;

                if (WfConstants.FORM_FIELD_DECISION.equals(formProperty.getId())) {
                    value = decision;
                } else {
                    QName propertyName = new QName(SchemaConstants.NS_C, formProperty.getName());
                    value = specific.getPropertyRealValue(propertyName, Object.class);
                }

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Writable property " + formProperty.getId() + " has a value of " + value);
                }

                propertiesToSubmit.put(formProperty.getId(), value == null ? "" : value.toString());
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Submitting " + propertiesToSubmit.size() + " properties");
        }

        formService.submitTaskFormData(taskId, propertiesToSubmit);
    }


    private PrismObject<ObjectType> getRequestSpecific(Task task, Map<String,Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException, WorkflowException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getRequestSpecific starting: execution id " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);
        }

        // todo - use NS other than NS_C (at least for form properties)

        ComplexTypeDefinition ctd = new ComplexTypeDefinition(WORK_ITEM_NAME, WORK_ITEM_NAME, workflowManager.getPrismContext());

        TaskFormData data = getActivitiEngine().getFormService().getTaskFormData(task.getId());

        for (FormProperty formProperty : data.getFormProperties()) {

            LOGGER.trace("- form property: name=" + formProperty.getName() + ", value=" + formProperty.getValue() + ", type=" + formProperty.getType());

            if (WfConstants.FORM_FIELD_DECISION.equals(formProperty.getId())) {
                LOGGER.trace("   - it is a decision field, concealing it.");
                continue;
            }

            String propertyName = getPropertyName(formProperty);
            String propertyType = getPropertyType(formProperty);

            QName pname = new QName(SchemaConstants.NS_C, propertyName);
            QName ptype;

            if (propertyType == null) {
                FormType t = formProperty.getType();
                String ts = t == null ? "string" : t.getName();

                if ("string".equals(ts)) {
                    ptype = DOMUtil.XSD_STRING;
                } else if ("boolean".equals(ts)) {
                    ptype = DOMUtil.XSD_BOOLEAN;
                } else if ("long".equals(ts)) {
                    ptype = DOMUtil.XSD_LONG;
                } else if ("date".equals(ts)) {
                    ptype = new QName(W3C_XML_SCHEMA_NS_URI, "date",
                            DOMUtil.NS_W3C_XML_SCHEMA_PREFIX);
                } else if ("enum".equals(ts)) {
                    ptype = DOMUtil.XSD_INT;        // TODO: implement somehow ...
                } else {
                    LOGGER.warn("Unknown Activiti type: " + ts);
                    continue;
                }
                PrismPropertyDefinition ppd = ctd.createPropertyDefinifion(pname, ptype);
                if (!formProperty.isWritable()) {
                    ppd.setReadOnly();
                }
                ppd.setMinOccurs(0);
            } else {
                ptype = new QName(SchemaConstants.NS_C, propertyType);
                ComplexTypeDefinition childCtd = workflowManager.getPrismContext().getSchemaRegistry().findComplexTypeDefinition(ptype);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Complex type = " + ptype + ", its definition = " + childCtd);
                }
                PrismContainerDefinition pcd = new PrismContainerDefinition(pname, childCtd, workflowManager.getPrismContext());
                ctd.add(pcd);
            }

        }

        ctd.setObjectMarker(true);

        PrismObjectDefinition<ObjectType> prismObjectDefinition = new PrismObjectDefinition<ObjectType>(WORK_ITEM_NAME, ctd, workflowManager.getPrismContext(), ObjectType.class);
        PrismObject<ObjectType> instance = prismObjectDefinition.instantiate();

        for (FormProperty formProperty : data.getFormProperties()) {

            if (WfConstants.FORM_FIELD_DECISION.equals(formProperty.getId())) {
                continue;
            }

            FormType t = formProperty.getType();
            String ts = t == null ? "string" : t.getName();

            boolean isBoolean = "boolean".equals(ts);

            if (!isBoolean && containsFlag(formProperty, FLAG_CLEAR_ON_ENTRY)) {
                continue;
            }

            String propertyName = getPropertyName(formProperty);
            String propertyType = getPropertyType(formProperty);

            QName pname = new QName(SchemaConstants.NS_C, propertyName);
            if (propertyType == null) {
                Object value = formProperty.getValue();     // non-string values cannot be read here as for now...
                if (isBoolean && containsFlag(formProperty, FLAG_CLEAR_ON_ENTRY)) {
                    value = Boolean.FALSE;        // todo: brutal hack to avoid three-state check box
                }
                instance.findOrCreateProperty(pname).setValue(new PrismPropertyValue<Object>(value));
            } else {
                // this is defunct as for now
                Object value = variables.get(propertyName);
                if (value instanceof RoleType) {
                    RoleType roleType = (RoleType) value;
                    PrismContainer container = instance.findOrCreateContainer(pname);
                    container.add(roleType.asPrismContainerValue().clone());
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + instance.debugDump());
        }
        return instance;
    }

    // TODO make more clean!
    private boolean containsFlag(FormProperty formProperty, char flag) {
        return formProperty.getId().contains("" + FLAG_SEPARATOR_CHAR + flag);
    }

    public PrismObject<ObjectType> getRequestCommon(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        // todo - use NS other than NS_C (at least for form properties)

        ComplexTypeDefinition ctd = new ComplexTypeDefinition(WORK_ITEM_NAME, WORK_ITEM_NAME, workflowManager.getPrismContext());
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_ID, DOMUtil.XSD_STRING).setReadOnly();
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_NAME, DOMUtil.XSD_STRING).setReadOnly();
        //ctd.createPropertyDefinifion(WORK_ITEM_ASSIGNEE, DOMUtil.XSD_STRING);
        //ctd.createPropertyDefinifion(WORK_ITEM_CANDIDATES, DOMUtil.XSD_STRING);
        //ctd.createPropertyDefinifion(WORK_ITEM_CREATED, DOMUtil.XSD_DATETIME).setReadOnly();
        ctd.createPropertyDefinifion(WORK_ITEM_CREATED, DOMUtil.XSD_STRING).setReadOnly();

        ctd.setObjectMarker(true);

        PrismObjectDefinition<ObjectType> prismObjectDefinition = new PrismObjectDefinition<ObjectType>(WORK_ITEM_NAME, ctd, workflowManager.getPrismContext(), ObjectType.class);
        PrismObject<ObjectType> instance = prismObjectDefinition.instantiate();
        instance.findOrCreateProperty(WORK_ITEM_TASK_ID).setValue(new PrismPropertyValue<Object>(task.getId()));
        instance.findOrCreateProperty(WORK_ITEM_TASK_NAME).setValue(new PrismPropertyValue<Object>(task.getName()));


//        instance.findOrCreateProperty(WORK_ITEM_CREATED).setValue(new PrismPropertyValue<Object>(
//                XmlTypeConverter.createXMLGregorianCalendar(task.getCreateTime().getTime())));

        // todo i18n (it's an ugly hack now); default PrismObject viewer does not show the time
        instance.findOrCreateProperty(WORK_ITEM_CREATED).setValue(new PrismPropertyValue<Object>(
                task.getCreateTime().toString()));

//        if (task.getAssignee() != null) {
//            instance.findOrCreateProperty(WORK_ITEM_ASSIGNEE).setValue(new PrismPropertyValue<Object>(task.getAssignee()));
//        }
//        String candidates = getCandidatesAsString(task);
//        if (candidates != null) {
//            instance.findOrCreateProperty(WORK_ITEM_CANDIDATES).setValue(new PrismPropertyValue<Object>(candidates));
//        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + instance.debugDump());
        }
        return instance;
    }

    // todo: ObjectNotFoundException used in unusual way (not in connection with midPoint repository)
    private Task getTask(String taskId) throws ObjectNotFoundException {
        Task task = getActivitiEngine().getTaskService().createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new ObjectNotFoundException("Task " + taskId + " could not be found.");
        }
        return task;
    }

    public PrismObject<ObjectType> getAdditionalData(Task task, Map<String,Object> variables, OperationResult result) throws ObjectNotFoundException {
        Object d = variables.get(WfConstants.VARIABLE_MIDPOINT_ADDITIONAL_DATA);
        if (d instanceof PrismObject) {
            return (PrismObject<ObjectType>) d;
        } else if (d instanceof String && ((String) d).startsWith("@")) {   // brutal hack - reference to another process variable in the form of @variableName
            d = variables.get(((String) d).substring(1));
            if (d instanceof PrismObject) {
                return (PrismObject<ObjectType>) d;
            } else if (d instanceof ObjectType) {
                return ((ObjectType) d).asPrismObject();
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Variable " + WfConstants.VARIABLE_MIDPOINT_ADDITIONAL_DATA + " found to be " + d + " but that's nothing useful at this moment.");
        }
        return null;
    }

    public PrismObject<ObjectType> getTrackingData(Task task, Map<String,Object> variables, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery();
        piq.processInstanceId(task.getProcessInstanceId());
        org.activiti.engine.runtime.ProcessInstance processInstance = piq.singleResult();

        // todo - use NS other than NS_C (at least for form properties)

        ComplexTypeDefinition ctd = new ComplexTypeDefinition(WORK_ITEM_NAME, WORK_ITEM_NAME, workflowManager.getPrismContext());
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_ID, DOMUtil.XSD_STRING);//.setReadOnly();
        ctd.createPropertyDefinifion(WORK_ITEM_PROCESS_INSTANCE_ID, DOMUtil.XSD_STRING);//.setReadOnly();
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_ASSIGNEE, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_OWNER, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_CANDIDATES, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_EXECUTION_ID, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_PROCESS_DEFINITION_ID, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_WATCHER_OID, DOMUtil.XSD_STRING);

        ctd.setObjectMarker(true);

        PrismObjectDefinition<ObjectType> prismObjectDefinition = new PrismObjectDefinition<ObjectType>(WORK_ITEM_NAME, ctd, workflowManager.getPrismContext(), ObjectType.class);
        PrismObject<ObjectType> instance = prismObjectDefinition.instantiate();
        instance.findOrCreateProperty(WORK_ITEM_TASK_ID).setValue(new PrismPropertyValue<Object>(task.getId()));
        instance.findOrCreateProperty(WORK_ITEM_PROCESS_INSTANCE_ID).setValue(new PrismPropertyValue<Object>(task.getProcessInstanceId()));
        instance.findOrCreateProperty(WORK_ITEM_TASK_ASSIGNEE).setValue(new PrismPropertyValue<Object>(task.getAssignee()));
        instance.findOrCreateProperty(WORK_ITEM_TASK_OWNER).setValue(new PrismPropertyValue<Object>(task.getOwner()));
        instance.findOrCreateProperty(WORK_ITEM_TASK_CANDIDATES).setValue(new PrismPropertyValue<Object>(getCandidatesAsString(task)));
        instance.findOrCreateProperty(WORK_ITEM_EXECUTION_ID).setValue(new PrismPropertyValue<Object>(task.getExecutionId()));
        instance.findOrCreateProperty(WORK_ITEM_PROCESS_DEFINITION_ID).setValue(new PrismPropertyValue<Object>(processInstance.getProcessDefinitionId()));
        instance.findOrCreateProperty(WORK_ITEM_WATCHER_OID).setValue(new PrismPropertyValue<Object>(variables.get(WfConstants.VARIABLE_MIDPOINT_TASK_OID)));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + instance.debugDump());
        }
        return instance;
    }


    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_STOP_PROCESS_INSTANCE);

        RuntimeService rs = getActivitiEngine().getRuntimeService();
        try {
            rs.deleteProcessInstance(instanceId, "Process instance stopped on the request of " + username);
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Process instance couldn't be stopped", e);
            LoggingUtils.logException(LOGGER, "Process instance {} couldn't be stopped", e);
        }
    }

    public void deleteProcessInstance(String instanceId, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_PROCESS_INSTANCE);

        HistoryService hs = getActivitiEngine().getHistoryService();
        try {
            hs.deleteHistoricProcessInstance(instanceId);
            result.recordSuccess();
        } catch (ActivitiException e) {
            result.recordFatalError("Process instance couldn't be deleted", e);
            LoggingUtils.logException(LOGGER, "Process instance {} couldn't be deleted", e);
        }
    }

}
