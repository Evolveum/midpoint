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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.ActivitiEngineDataHelper;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TrackingDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */

@Component
public class WorkItemProvider {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemProvider.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    @Autowired
    private ActivitiEngineDataHelper activitiEngineDataHelper;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private MiscDataUtil miscDataUtil;

    @Autowired
    private PrismContext prismContext;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_COUNT_WORK_ITEMS_RELATED_TO_USER = DOT_INTERFACE + "countWorkItemsRelatedToUser";
    private static final String OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER = DOT_INTERFACE  + "listWorkItemsRelatedToUser";
    private static final String OPERATION_ACTIVITI_TASK_TO_WORK_ITEM = DOT_CLASS + "activitiTaskToWorkItem";
    private static final String OPERATION_GET_WORK_ITEM_DETAILS_BY_TASK_ID = DOT_CLASS + "getWorkItemDetailsById";

    /*
     * ========================= PART 1 - main operations =========================
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
    public int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_COUNT_WORK_ITEMS_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("assigned", assigned);
        try {
            int count = (int) createQueryForTasksRelatedToUser(userOid, assigned).count();
            result.recordSuccess();
            return count;
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't count work items assigned/assignable to " + userOid, e);
            throw new SystemException("Couldn't count work items assigned/assignable to " + userOid + " due to Activiti exception", e);
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
    public List<WorkItemType> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("assigned", assigned);
        result.addParam("first", first);
        result.addParam("count", count);
        List<Task> tasks;
        try {
            tasks = createQueryForTasksRelatedToUser(userOid, assigned).listPage(first, count);
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't list work items assigned/assignable to " + userOid, e);
            throw new SystemException("Couldn't list work items assigned/assignable to " + userOid + " due to Activiti exception", e);
        }

        List<WorkItemType> retval = tasksToWorkItems(tasks, false, false, result);       // there's no need to fill-in assignee details nor data forms
        result.computeStatusIfUnknown();
        return retval;
    }

    private TaskQuery createQueryForTasksRelatedToUser(String oid, boolean assigned) {
        if (assigned) {
            return activitiEngine.getTaskService().createTaskQuery().taskAssignee(oid).orderByTaskCreateTime().desc();
        } else {
            return activitiEngine.getTaskService().createTaskQuery().taskCandidateUser(oid).orderByTaskCreateTime().desc();
        }
    }

    public WorkItemType getWorkItemDetailsById(String taskId, OperationResult parentResult) throws ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_WORK_ITEM_DETAILS_BY_TASK_ID);
        result.addParam("taskId", taskId);
        Task task;
        try {
            task = activitiEngineDataHelper.getTaskById(taskId, result);
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't get work item with id " + taskId, e);
            throw new SystemException("Couldn't get work item with id " + taskId, e);
        }
        WorkItemType retval;
        try {
            retval = taskToWorkItem(task, true, true, result);
        } catch (WorkflowException e) {
            throw new SystemException(e);
        }
        result.computeStatusIfUnknown();
        return retval;
    }

    /*
     * ========================= PART 2 - activiti to midpoint converters =========================
     *
     * getTaskDetails parameter influences whether we want to get only basic or more detailed information.
     * The details contain the following information:
     *  - requester details,
     *  - object details,
     *  - all forms needed to display the work item,
     * so, obviously, it is more expensive to obtain.
     *
     * In similar way, getAssigneeDetails influences whether details about assignee and candidates are filled-in.
     * This should be skipped if there's no need to display these (e.g. in the list of work items assigned to the current user).
     */

    List<WorkItemType> tasksToWorkItems(List<Task> tasks, boolean getTaskDetails, boolean getAssigneeDetails, OperationResult result) {
        List<WorkItemType> retval = new ArrayList<WorkItemType>();
        for (Task task : tasks) {
            try {
                retval.add(taskToWorkItem(task, getTaskDetails, getAssigneeDetails, result));
            } catch (WorkflowException e) {
                LoggingUtils.logException(LOGGER, "Couldn't get information on activiti task {}", e, task.getId());
            }
        }
        return retval;
    }

    // should not throw ActivitiException
    // returns WorkItem or WorkItemDetailed, based on the 'getTaskDetails' parameter value
    private WorkItemType taskToWorkItem(Task task, boolean getTaskDetails, boolean getAssigneeDetails, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
        result.addParam("task id", task.getId());
        result.addParam("getTaskDetails", getTaskDetails);

        WorkItemType wi = new WorkItemType();
        try {
            wi.setWorkItemId(task.getId());
            wi.setAssigneeRef(MiscSchemaUtil.createObjectReference(task.getAssignee(), SchemaConstants.C_USER_TYPE));
            wi.setName(new PolyStringType(task.getName()));
            wi.setProcessInstanceId(task.getProcessInstanceId());

            MetadataType metadataType = new MetadataType();
            metadataType.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(task.getCreateTime()));
            wi.setMetadata(metadataType);
        } catch (ActivitiException e) {     // not sure if any of the above methods can throw this exception, but for safety we catch it here
            result.recordFatalError("Couldn't get information on activiti task " + task.getId(), e);
            throw new WorkflowException("Couldn't get information on activiti task " + task.getId(), e);
        }

        if (getAssigneeDetails) {
            PrismObject<UserType> assignee = miscDataUtil.getUserByOid(task.getAssignee(), result);
            if (assignee != null) {
                wi.setAssignee(assignee.asObjectable());
            }
            // todo fill-in candidate orgs/users
//            try {
//                wi.setCandidates(activitiEngineDataHelper.getCandidatesAsString(task));
//            } catch (ActivitiException e) {
//                String m = "Couldn't get work item candidates for Activiti task " + task.getId();
//                result.recordPartialError(m, e);
//                LoggingUtils.logException(LOGGER, m, e);
//            }
        }

        if (getTaskDetails) {
            try {
                Map<String,Object> variables = activitiEngineDataHelper.getProcessVariables(task.getId(), result);

                PrismObject<UserType> requester = miscDataUtil.getRequester(variables, result);
                wi.setRequester(requester.asObjectable());
                wi.setRequesterRef(MiscSchemaUtil.createObjectReference(requester.getOid(), SchemaConstants.C_USER_TYPE));

                PrismObject<? extends ObjectType> objectBefore = miscDataUtil.getObjectBefore(variables, prismContext, result);
                if (objectBefore != null) {
                    wi.setObjectOld(objectBefore.asObjectable());
                    if (objectBefore.getOid() != null) {
                        wi.setObjectOldRef(MiscSchemaUtil.createObjectReference(objectBefore.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
                    }
                }

                wi.setObjectDelta(miscDataUtil.getObjectDeltaType(variables, true));

                PrismObject<? extends ObjectType> objectAfter = miscDataUtil.getObjectAfter(variables, wi.getObjectDelta(), objectBefore, prismContext, result);
                if (objectAfter != null) {
                    wi.setObjectNew(objectAfter.asObjectable());
                    if (objectAfter.getOid() != null) {
                        wi.setObjectNewRef(MiscSchemaUtil.createObjectReference(objectAfter.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
                    }
                }

                PrismObject<? extends ObjectType> relatedObject = getRelatedObject(task, variables, result);
                if (relatedObject != null) {
                    wi.setRelatedObject(relatedObject.asObjectable());
                    if (relatedObject.getOid() != null) {
                        wi.setRelatedObjectRef(MiscSchemaUtil.createObjectReference(relatedObject.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
                    }
                }

                wi.setRequestSpecificData(asObjectable(getRequestSpecificData(task, variables, result)));
                wi.setTrackingData(asObjectable(getTrackingData(task, variables, result)));
            } catch (SchemaException e) {
                throw new SystemException("Got unexpected schema exception when preparing information on Work Item", e);
            } catch (ObjectNotFoundException e) {
                throw new SystemException("Got unexpected object-not-found exception when preparing information on Work Item; perhaps the requester or a workflow task was deleted in the meantime.", e);
            } catch (JAXBException e) {
                throw new SystemException("Got unexpected JAXB exception when preparing information on Work Item", e);
            } catch (WorkflowException e) {
                result.recordFatalError("Couldn't set work item details for activiti task " + task.getId(), e);
                throw e;
            }
        }

        result.recordSuccessIfUnknown();
        return wi;
    }

    private <T> T asObjectable(PrismObject<? extends T> prismObject) {
        return prismObject != null ? prismObject.asObjectable() : null;
    }

    private PrismObject<? extends ObjectType> getRequestSpecificData(Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException, WorkflowException {
        return getChangeProcessor(task, variables).getRequestSpecificData(task, variables, result);
    }

    private ChangeProcessor getChangeProcessor(Task task, Map<String, Object> variables) {
        String cpClassName = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR);
        if (cpClassName == null) {
            throw new IllegalStateException("Change processor is unknown for task: " + task);
        }

        return wfConfiguration.findChangeProcessor(cpClassName);
    }

    private PrismObject<? extends ObjectType> getRelatedObject(Task task, Map<String, Object> variables, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return getChangeProcessor(task, variables).getRelatedObject(task, variables, result);
    }

    private PrismObject<? extends TrackingDataType> getTrackingData(Task task, Map<String,Object> variables, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ProcessInstanceQuery piq = activitiEngine.getRuntimeService().createProcessInstanceQuery();
        piq.processInstanceId(task.getProcessInstanceId());
        org.activiti.engine.runtime.ProcessInstance processInstance = piq.singleResult();

        PrismObjectDefinition<TrackingDataType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(TrackingDataType.COMPLEX_TYPE);
        PrismObject<TrackingDataType> formPrism = formDefinition.instantiate();
        TrackingDataType form = formPrism.asObjectable();

        form.setTaskId(task.getId());
        form.setProcessInstanceId(task.getProcessInstanceId());
        form.setTaskAssignee(task.getAssignee());
        form.setTaskOwner(task.getOwner());
        form.setTaskCandidates(activitiEngineDataHelper.getCandidatesAsString(task));
        form.setExecutionId(task.getExecutionId());
        form.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        form.setShadowTaskOid((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + formPrism.debugDump());
        }
        return formPrism;
    }

}
