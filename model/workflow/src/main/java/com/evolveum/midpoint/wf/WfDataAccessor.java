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
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.processes.ProcessWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.FormType;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.history.*;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;

import javax.xml.namespace.QName;
import java.util.*;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 28.9.2012
 * Time: 13:26
 * To change this template use File | Settings | File Templates.
 */
public class WfDataAccessor {

    private static final transient Trace LOGGER = TraceManager.getTrace(WfDataAccessor.class);
    private static final char PROPERTY_TYPE_SEPARATOR_CHAR = '$';
    private static final char FLAG_SEPARATOR_CHAR = '#';

    private WorkflowManager workflowManager;
    private static final String DOT_CLASS = WfDataAccessor.class.getName() + ".";
    private static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_CLASS + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_CLASS + "deleteProcessInstance";
    private static final char FLAG_CLEAR_ON_ENTRY = 'C';

    public WfDataAccessor(WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
    }

    private ActivitiEngine getActivitiEngine() {
        return workflowManager.getActivitiEngine();
    }

    /*
    * Some externally-visible methods. These count* and list* methods are used by the GUI.
    */

    public int countWorkItemsAssignedToUser(String user, OperationResult parentResult) {
        TaskService taskService = getActivitiEngine().getTaskService();
        TaskQuery tq = taskService.createTaskQuery();
        tq.taskAssignee(user);
        int retval = (int) tq.count();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("countWorkItemsAssignedToUser, user=" + user + ": " + retval);
        }
        return retval;
    }

    public List<WorkItem> listWorkItemsAssignedToUser(String user, int first, int count, OperationResult parentResult) {
        TaskService taskService = getActivitiEngine().getTaskService();
        TaskQuery tq = taskService.createTaskQuery();
        tq.taskAssignee(user);
        List<Task> tasks = tq.listPage(first, count);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("listWorkItemsAssignedToUser, user=" + user + ", first/count=" + first + "/" + count + ": " + tasks);
        }
        return tasksToWorkItems(tasks, false);
    }

    public int countWorkItemsAssignableToUser(String user, OperationResult parentResult) {
        TaskService taskService = getActivitiEngine().getTaskService();
        TaskQuery tq = taskService.createTaskQuery();
        tq.taskCandidateUser(user);
        int retval = (int) tq.count();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("countWorkItemsAssignableToUser, user=" + user + ": " + retval);
        }
        return retval;
    }

    public List<WorkItem> listWorkItemsAssignableToUser(String user, int first, int count, OperationResult parentResult) {
        TaskService taskService = getActivitiEngine().getTaskService();
        TaskQuery tq = taskService.createTaskQuery();
        tq.taskCandidateUser(user);
        List<Task> tasks = tq.listPage(first, count);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("listWorkItemsAssignableToUser, user=" + user + ", first/count=" + first + "/" + count + ": " + tasks);
        }
        return tasksToWorkItems(tasks, false);
    }

    public int countProcessInstancesStartedByUser(String userOid, OperationResult parentResult) {
        ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery();
        piq.variableValueEquals(WfConstants.VARIABLE_MIDPOINT_REQUESTER_OID, userOid);
        int retval = (int) piq.count();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("countProcessInstancesStartedByUser, user=" + userOid + ": " + retval);
        }
        return retval;
    }

    public List<ProcessInstance> listProcessInstancesStartedByUser(String userOid, int first, int count, OperationResult parentResult) {
        ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery();
        piq.variableValueEquals(WfConstants.VARIABLE_MIDPOINT_REQUESTER_OID, userOid);
        List<org.activiti.engine.runtime.ProcessInstance> instances = piq.listPage(first, count);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("listProcessInstancesStartedByUser, user=" + userOid + ", first/count=" + first + "/" + count + ": " + instances);
        }
        return activitiToMidpointProcessInstances(instances);
    }

    public List<ProcessInstance> listProcessInstances(boolean requestedBy, boolean finished, String userOid, int first, int count, OperationResult result) {
        if (finished) {
            return listFinishedProcessInstances(requestedBy, userOid, first, count, result);
        } else {
            return requestedBy ? listProcessInstancesStartedByUser(userOid, first, count, result)
                    : listProcessInstancesRelatedToUser(userOid, first, count, result);
        }
    }

    public int countProcessInstances(boolean requestedBy, boolean finished, String userOid, OperationResult result) {
        if (finished) {
            return countFinishedProcessInstances(requestedBy, userOid, result);
        } else {
            return requestedBy ? countProcessInstancesStartedByUser(userOid, result)
                    : countProcessInstancesRelatedToUser(userOid, result);
        }
    }

    HistoricProcessInstanceQuery queryForFinishedProcessInstances(boolean requestedBy, String userOid) {
        HistoryService hs = getActivitiEngine().getHistoryService();
        HistoricProcessInstanceQuery hpiq = hs.createHistoricProcessInstanceQuery().finished().orderByProcessInstanceEndTime().desc();
        if (requestedBy) {
            hpiq = hpiq.startedBy(userOid);
        } else {
            hpiq = hpiq.processInstanceBusinessKey(userOid);
        }
        return hpiq;
    }

    private int countFinishedProcessInstances(boolean requestedBy, String userOid, OperationResult result) {
        int instances = (int) queryForFinishedProcessInstances(requestedBy, userOid).count();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("countFinishedProcessInstances, user=" + userOid + ", requestedBy = " + requestedBy + ", result=" + instances);
        }
        return instances;
    }

    private List<ProcessInstance> listFinishedProcessInstances(boolean requestedBy, String userOid, int first, int count, OperationResult result) {
        HistoricProcessInstanceQuery hpiq = queryForFinishedProcessInstances(requestedBy, userOid);
        List<HistoricProcessInstance> instances = hpiq.listPage(first, count);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("listFinishedProcessInstances, user=" + userOid + ", requestedBy=" + requestedBy + ", first/count=" + first + "/" + count + ": " + instances);
        }
        return activitiToMidpointProcessInstancesHistory(instances);
    }

    public int countProcessInstancesRelatedToUser(String userOid, OperationResult parentResult) {
        ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery();
        piq.variableValueEquals(WfConstants.VARIABLE_MIDPOINT_OBJECT_OID, userOid);
        int retval = (int) piq.count();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("countProcessInstancesRelatedToUser, user=" + userOid + ": " + retval);
        }
        return retval;
    }

    public List<ProcessInstance> listProcessInstancesRelatedToUser(String userOid, int first, int count, OperationResult result) {
        ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery();
        piq.variableValueEquals(WfConstants.VARIABLE_MIDPOINT_OBJECT_OID, userOid);
        List<org.activiti.engine.runtime.ProcessInstance> instances = piq.listPage(first, count);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("listProcessInstancesRelatedToUser, user=" + userOid + ", first/count=" + first + "/" + count + ": " + instances);
        }
        return activitiToMidpointProcessInstances(instances);
    }

    public ProcessInstance getProcessInstanceByInstanceId(String instanceId, boolean historic) throws ObjectNotFoundException {

        if (historic) {
            HistoricProcessInstanceQuery hpiq = getActivitiEngine().getHistoryService().createHistoricProcessInstanceQuery();
            hpiq.processInstanceId(instanceId);
            HistoricProcessInstance historicProcessInstance = hpiq.singleResult();
            if (historicProcessInstance != null) {
                return activitiToMidpointProcessInstanceHistory(historicProcessInstance, true);
            } else {
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            }
        } else {
            ProcessInstanceQuery piq = getActivitiEngine().getRuntimeService().createProcessInstanceQuery();
            piq.processInstanceId(instanceId);
            org.activiti.engine.runtime.ProcessInstance instance = piq.singleResult();

            if (instance != null) {
                return activitiToMidpointProcessInstanceWithDetails(instance);
            } else {
                throw new ObjectNotFoundException("Process instance " + instanceId + " couldn't be found.");
            }
        }
    }

    private List<ProcessInstance> activitiToMidpointProcessInstancesHistory(List<HistoricProcessInstance> instances) {
        List<ProcessInstance> retval = new ArrayList<ProcessInstance>();
        for (HistoricProcessInstance instance : instances) {
            retval.add(activitiToMidpointProcessInstanceHistory(instance, false));
        }
        return retval;
    }

    private ProcessInstance activitiToMidpointProcessInstanceHistory(HistoricProcessInstance instance, boolean details) {
        ProcessInstance pi = new ProcessInstance();

        Map<String,Object> vars = getHistoricVariables(instance.getId());
        pi.setName((String) vars.get(WfConstants.VARIABLE_PROCESS_NAME));
        pi.setProcessId(instance.getId());
        pi.setStartTime((Date) vars.get(WfConstants.VARIABLE_START_TIME));
        pi.setEndTime(instance.getEndTime());

        pi.setDetails(getProcessSpecificDetails(instance, vars));

        return pi;
    }

    private Map<String, Object> getHistoricVariables(String pid) {

        Map<String, Object> retval = new HashMap<String, Object>();

        // copied from ActivitiInterface!
        HistoryService hs = getActivitiEngine().getHistoryService();

        HistoricDetailQuery hdq = hs.createHistoricDetailQuery()
                .variableUpdates()
                .processInstanceId(pid)
                .orderByTime().desc();

        for (HistoricDetail hd : hdq.list())
        {
            HistoricVariableUpdate hvu = (HistoricVariableUpdate) hd;
            String varname = hvu.getVariableName();
            Object value = hvu.getValue();
            if (!retval.containsKey(varname)) {
                retval.put(varname, value);
            }
        }
        return retval;
    }

    private List<ProcessInstance> activitiToMidpointProcessInstances(List<org.activiti.engine.runtime.ProcessInstance> instances) {
        List<ProcessInstance> retval = new ArrayList<ProcessInstance>();
        for (org.activiti.engine.runtime.ProcessInstance instance : instances) {
            retval.add(activitiToMidpointProcessInstance(instance));
        }
        return retval;
    }

    private ProcessInstance activitiToMidpointProcessInstanceWithDetails(org.activiti.engine.runtime.ProcessInstance instance) {

        ProcessInstance pi = new ProcessInstance();

        RuntimeService rs = getActivitiEngine().getRuntimeService();
        TaskService ts = getActivitiEngine().getTaskService();

        Map<String,Object> vars = rs.getVariables(instance.getProcessInstanceId());
        pi.setName((String) vars.get(WfConstants.VARIABLE_PROCESS_NAME));
        pi.setProcessId(instance.getProcessInstanceId());
        pi.setStartTime((Date) vars.get(WfConstants.VARIABLE_START_TIME));

        List<Task> tasks = ts.createTaskQuery().processInstanceId(instance.getProcessInstanceId()).list();
        pi.setWorkItems(tasksToWorkItems(tasks, true));

        pi.setDetails(getProcessSpecificDetails(instance, vars, tasks));

        return pi;
    }

    private String getProcessSpecificDetails(org.activiti.engine.runtime.ProcessInstance instance, Map<String, Object> vars, List<Task> tasks) {
        ProcessWrapper wrapper = findWrapper(vars, instance.getId());
        if (wrapper == null) {
            return "Error: process wrapper could not be found.";        // todo error reporting
        } else {
            return wrapper.getProcessSpecificDetails(instance, vars, tasks);
        }
    }

    // todo error reporting
    private ProcessWrapper findWrapper(Map<String, Object> vars, String id) {
        String wrapperName = (String) vars.get(WfConstants.VARIABLE_MIDPOINT_PROCESS_WRAPPER);
        if (wrapperName == null) {
            LOGGER.warn("No process wrapper found for wf process " + id);
            return null;
        }
        try {
            return (ProcessWrapper) Class.forName(wrapperName).newInstance();
        } catch (InstantiationException e) {
            LoggingUtils.logException(LOGGER, "Cannot instantiate workflow process wrapper {} due to instantiation exception", e, wrapperName);
            return null;
        } catch (IllegalAccessException e) {
            LoggingUtils.logException(LOGGER, "Cannot instantiate workflow process wrapper {} due to illegal access exception", e, wrapperName);
            return null;
        } catch (ClassNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot instantiate workflow process wrapper {} because the class cannot be found", e, wrapperName);
            return null;
        }
    }

    private String getProcessSpecificDetails(HistoricProcessInstance instance, Map<String, Object> vars) {
        ProcessWrapper wrapper = findWrapper(vars, instance.getId());
        if (wrapper == null) {
            return "Error: process wrapper could not be found.";        // todo error reporting
        } else {
            return wrapper.getProcessSpecificDetails(instance, vars);
        }
    }

    // todo refactor this
    private ProcessInstance activitiToMidpointProcessInstance(org.activiti.engine.runtime.ProcessInstance instance) {

        RuntimeService rs = getActivitiEngine().getRuntimeService();

        ProcessInstance pi = new ProcessInstance();
        Map<String,Object> vars = rs.getVariables(instance.getProcessInstanceId());
        pi.setName((String) vars.get(WfConstants.VARIABLE_PROCESS_NAME));
        pi.setProcessId(instance.getProcessInstanceId());
        pi.setStartTime((Date) vars.get(WfConstants.VARIABLE_START_TIME));
        return pi;
    }

    private List<WorkItem> tasksToWorkItems(List<Task> tasks, boolean extended) {
        List<WorkItem> retval = new ArrayList<WorkItem>();
        for (Task task : tasks) {
            retval.add(taskToWorkItem(task, extended));
        }
        return retval;
    }

    private WorkItem taskToWorkItem(Task task, boolean extended) {
        WorkItem wi = new WorkItem();
        wi.setTaskId(task.getId());
        wi.setAssignee(task.getAssignee());
        if (extended) {
            wi.setAssigneeName(getUserNameByOid(task.getAssignee()));
        } else {
            wi.setAssigneeName(wi.getAssignee());
        }
        wi.setName(task.getName());
        wi.setProcessId(task.getProcessInstanceId());
        wi.setCandidates(getCandidatesAsString(task));
        wi.setCreateTime(task.getCreateTime());
        return wi;
    }

    private String getUserNameByOid(String oid) {
        try {
//            LOGGER.info("getUserNameByOid called for " + oid);
            PrismObject<UserType> user = workflowManager.getRepositoryService().getObject(UserType.class, oid, new OperationResult("todo"));
            String name = user.asObjectable().getName();
//            LOGGER.info("getUserNameByOid returning " + name);
            return name;
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details because it couldn't be found", e, oid);
            return oid;
        } catch (SchemaException e) {
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

    public void claimWorkItem(WorkItem workItem, String userId, OperationResult result) {
        TaskService taskService = getActivitiEngine().getTaskService();
        taskService.claim(workItem.getTaskId(), userId);
        result.recordSuccess();
    }

    public void releaseWorkItem(WorkItem workItem, OperationResult result) {
        TaskService taskService = getActivitiEngine().getTaskService();
        taskService.claim(workItem.getTaskId(), null);
        result.recordSuccess();
    }





    private static final QName WORK_ITEM_NAME = new QName(SchemaConstants.NS_C, "WorkItem");
    private static final QName WORK_ITEM_TASK_ID = new QName(SchemaConstants.NS_C, "taskId");
    private static final QName WORK_ITEM_TASK_NAME = new QName(SchemaConstants.NS_C, "name");
    private static final QName WORK_ITEM_ASSIGNEE = new QName(SchemaConstants.NS_C, "assignee");
    private static final QName WORK_ITEM_CANDIDATES = new QName(SchemaConstants.NS_C, "candidates");
    private static final QName WORK_ITEM_CREATED = new QName(SchemaConstants.NS_C, "created");

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

        StringBuffer retval = new StringBuffer();
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

    public void saveWorkItemPrism(PrismObject specific, PrismObject common, OperationResult result) {

        LOGGER.info("WorkItem form object (process-specific) = " + specific.debugDump());
        LOGGER.info("WorkItem form object (process-independent) = " + common.debugDump());

        String taskId = (String) common.getPropertyRealValue(WORK_ITEM_TASK_ID, String.class);

        LOGGER.trace("Saving work item " + taskId);

        FormService formService = getActivitiEngine().getFormService();
        Map<String,String> propertiesToSubmit = new HashMap<String,String>();
        TaskFormData data = getActivitiEngine().getFormService().getTaskFormData(taskId);

        LOGGER.trace("# of form properties: " + data.getFormProperties().size());

        for (FormProperty formProperty : data.getFormProperties()) {

            LOGGER.trace("Processing property " + formProperty.getId() + ":" + formProperty.getName());

            if (formProperty.isWritable()) {
                QName propertyName = new QName(SchemaConstants.NS_C, formProperty.getName());

                Object value = specific.getPropertyRealValue(propertyName, Object.class);
                LOGGER.trace("Writable property " + formProperty.getId() + " has a value of " + value);
                propertiesToSubmit.put(formProperty.getId(), value == null ? "" : value.toString());
            }
        }

        LOGGER.trace("Submitting " + propertiesToSubmit.size() + " properties");

        formService.submitTaskFormData(taskId, propertiesToSubmit);
    }

    private Map<String,Object> getProcessVariables(String taskId) throws ObjectNotFoundException {

        Task task = getTask(taskId);
        Map<String,Object> variables = getActivitiEngine().getProcessEngine().getRuntimeService().getVariables((task.getExecutionId()));
        LOGGER.info("Execution " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);
        return variables;
    }

    public PrismObject<UserType> getRequester(String taskId) throws ObjectNotFoundException {
        Map<String,Object> variables = getProcessVariables(taskId);
        return (PrismObject<UserType>) variables.get(WfConstants.VARIABLE_MIDPOINT_REQUESTER);
    }

    public PrismObject<? extends ObjectType> getObjectOld(String taskId) throws ObjectNotFoundException {
        Map<String,Object> variables = getProcessVariables(taskId);
        return (PrismObject<? extends ObjectType>) variables.get(WfConstants.VARIABLE_MIDPOINT_OBJECT_OLD);
    }

    public PrismObject<? extends ObjectType> getObjectNew(String taskId) throws ObjectNotFoundException {
        Map<String,Object> variables = getProcessVariables(taskId);
        return (PrismObject<? extends ObjectType>) variables.get(WfConstants.VARIABLE_MIDPOINT_OBJECT_NEW);
    }

    public PrismObject<? extends ObjectType> getRequestSpecific(String taskId) throws SchemaException, ObjectNotFoundException {

        Task task = getTask(taskId);

        Map<String,Object> variables = getActivitiEngine().getProcessEngine().getRuntimeService().getVariables((task.getExecutionId()));
        LOGGER.info("getRequestSpecific starting: execution id " + task.getExecutionId() + ", pid " + task.getProcessInstanceId() + ", variables = " + variables);

        // todo - use NS other than NS_C (at least for form properties)

        ComplexTypeDefinition ctd = new ComplexTypeDefinition(WORK_ITEM_NAME, WORK_ITEM_NAME, workflowManager.getPrismContext());

        TaskFormData data = getActivitiEngine().getFormService().getTaskFormData(task.getId());

        for (FormProperty formProperty : data.getFormProperties()) {

            LOGGER.trace("- form property: name=" + formProperty.getName() + ", value=" + formProperty.getValue() + ", type=" + formProperty.getType());

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
                LOGGER.info("Complex type = " + ptype + ", its definition = " + childCtd);
                PrismContainerDefinition pcd = new PrismContainerDefinition(pname, childCtd, workflowManager.getPrismContext());
                ctd.add(pcd);
            }

        }

        ctd.setObjectMarker(true);

        PrismObjectDefinition<ObjectType> prismObjectDefinition = new PrismObjectDefinition<ObjectType>(WORK_ITEM_NAME, ctd, workflowManager.getPrismContext(), ObjectType.class);
        PrismObject<ObjectType> instance = prismObjectDefinition.instantiate();

        for (FormProperty formProperty : data.getFormProperties()) {

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

        LOGGER.info("Resulting prism object instance = " + instance.debugDump());
        return instance;
    }

    // TODO make more clean!
    private boolean containsFlag(FormProperty formProperty, char flag) {
        return formProperty.getId().contains("" + FLAG_SEPARATOR_CHAR + flag);
    }

    public PrismObject<? extends ObjectType> getRequestCommon(String taskId) throws SchemaException, ObjectNotFoundException {

        Task task = getTask(taskId);

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
        instance.findOrCreateProperty(WORK_ITEM_TASK_ID).setValue(new PrismPropertyValue<Object>(taskId));
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

        LOGGER.info("Resulting prism object instance = " + instance.debugDump());
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

    public PrismObject<? extends ObjectType> getAdditionalData(String taskId) throws ObjectNotFoundException {
        Map<String,Object> variables = getProcessVariables(taskId);
        Object d = variables.get(WfConstants.VARIABLE_MIDPOINT_ADDITIONAL_DATA);
        if (d instanceof PrismObject) {
            return (PrismObject<? extends ObjectType>) d;
        } else if (d instanceof String && ((String) d).startsWith("@")) {   // brutal hack - reference to another process variable in the form of @variableName
            d = variables.get(((String) d).substring(1));
            if (d instanceof PrismObject) {
                return (PrismObject<? extends ObjectType>) d;
            } else if (d instanceof ObjectType) {
                return ((ObjectType) d).asPrismObject();
            }
        }

        LOGGER.info("Variable " + WfConstants.VARIABLE_MIDPOINT_ADDITIONAL_DATA + " found to be " + d + " but that's nothing useful at this moment.");
        return null;
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
