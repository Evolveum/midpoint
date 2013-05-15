package com.evolveum.midpoint.wf.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WorkflowServiceImpl;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.ActivitiEngineDataHelper;
import com.evolveum.midpoint.wf.api.WorkItem;
import com.evolveum.midpoint.wf.api.WorkItemDetailed;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.TrackingDataFormType;
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

    private static final String DOT_CLASS = WorkflowServiceImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowService.class.getName() + ".";

    private static final String OPERATION_COUNT_WORK_ITEMS_RELATED_TO_USER = DOT_INTERFACE + "countWorkItemsRelatedToUser";
    private static final String OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER = DOT_INTERFACE  + "listWorkItemsRelatedToUser";
    private static final String OPERATION_ACTIVITI_TASK_TO_WORK_ITEM = DOT_CLASS + "activitiTaskToWorkItem";
    private static final String OPERATION_GET_WORK_ITEM_DETAILS_BY_TASK_ID = DOT_CLASS + "getWorkItemDetailsByTaskId";

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
            return tasksToWorkItems(tasks, false, false, result);       // there's no need to fill-in assignee details nor data forms
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't list work items assigned/assignable to " + userOid, e);
            throw new WorkflowException("Couldn't list work items assigned/assignable to " + userOid + " due to Activiti exception", e);
        }
    }

    public WorkItemDetailed getWorkItemDetailsByTaskId(String taskId, OperationResult parentResult) throws ObjectNotFoundException, WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_GET_WORK_ITEM_DETAILS_BY_TASK_ID);
        result.addParam("taskId", taskId);
        Task task = activitiEngineDataHelper.getTaskById(taskId, result);
        WorkItemDetailed retval = (WorkItemDetailed) taskToWorkItem(task, true, true, result);
        result.recordSuccessIfUnknown();
        return retval;
    }

    private TaskQuery createQueryForTasksRelatedToUser(String oid, boolean assigned) {
        if (assigned) {
            return activitiEngine.getTaskService().createTaskQuery().taskAssignee(oid).orderByTaskCreateTime().desc();
        } else {
            return activitiEngine.getTaskService().createTaskQuery().taskCandidateUser(oid).orderByTaskCreateTime().desc();
        }
    }

    /*
     * ========================= PART 2 - activiti to midpoint converters =========================
     *
     * getTaskDetails parameter influences whether we want to get only basic or more detailed information
     * (see WorkItem and WorkItemDetailed classes). The latter contains the following information:
     *  - requester details,
     *  - object details,
     *  - all forms needed to display the work item,
     * so, obviously, it is more expensive to obtain.
     *
     * In similar way, getAssigneeDetails influences whether details about assignee and candidates are filled-in.
     * This should be skipped if there's no need to display these (e.g. in the list of work items assigned to the current user).
     */

    List<WorkItem> tasksToWorkItems(List<Task> tasks, boolean getTaskDetails, boolean getAssigneeDetails, OperationResult result) throws WorkflowException {
        List<WorkItem> retval = new ArrayList<WorkItem>();
        for (Task task : tasks) {
            retval.add(taskToWorkItem(task, getTaskDetails, getAssigneeDetails, result));
        }
        return retval;
    }

    // should not throw ActivitiException
    // returns WorkItem or WorkItemDetailed, based on the 'getTaskDetails' parameter value
    private WorkItem taskToWorkItem(Task task, boolean getTaskDetails, boolean getAssigneeDetails, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
        result.addParam("task id", task.getId());
        result.addParam("getTaskDetails", getTaskDetails);

        WorkItem wi = getTaskDetails ? new WorkItemDetailed() : new WorkItem();
        wi.setTaskId(task.getId());
        wi.setAssignee(task.getAssignee());
        wi.setName(task.getName());
        wi.setProcessId(task.getProcessInstanceId());
        wi.setCreateTime(task.getCreateTime());

        if (getAssigneeDetails) {
            wi.setAssigneeName(miscDataUtil.getUserNameByOid(task.getAssignee(), result));
            try {
                wi.setCandidates(activitiEngineDataHelper.getCandidatesAsString(task));
            } catch (ActivitiException e) {
                String m = "Couldn't get work item candidates for Activiti task " + task.getId();
                result.recordPartialError(m, e);
                LoggingUtils.logException(LOGGER, m, e);
            }
        }

        if (getTaskDetails) {
            try {
                Map<String,Object> variables = activitiEngineDataHelper.getProcessVariables(task.getId(), result);
                ((WorkItemDetailed) wi).setRequester(miscDataUtil.getRequester(variables, result));
                PrismObject<? extends ObjectType> objectBefore = miscDataUtil.getObjectBefore(variables, prismContext, result);
                ((WorkItemDetailed) wi).setObjectOld(objectBefore);
                ((WorkItemDetailed) wi).setObjectDelta(miscDataUtil.getObjectDelta(variables, prismContext, result));
                ((WorkItemDetailed) wi).setObjectNew(miscDataUtil.getObjectAfter(variables, ((WorkItemDetailed) wi).getObjectDelta(), objectBefore, prismContext, result));
                ((WorkItemDetailed) wi).setRequestSpecificData(getRequestSpecificData(task, variables, result));
                ((WorkItemDetailed) wi).setTrackingData(getTrackingData(task, variables, result));
                ((WorkItemDetailed) wi).setAdditionalData(getAdditionalData(task, variables, result));
            } catch (SchemaException e) {
                throw new SystemException("Got unexpected schema exception when preparing information on Work Item", e);
            } catch (ObjectNotFoundException e) {
                throw new SystemException("Got unexpected object-not-found exception when preparing information on Work Item; perhaps the requester or a workflow task was deleted in the meantime.", e);
            } catch (JAXBException e) {
                throw new SystemException("Got unexpected JAXB exception when preparing information on Work Item", e);
            }
        }

        result.recordSuccessIfUnknown();
        return wi;
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

//    // TODO make more clean!
//    private boolean containsFlag(FormProperty formProperty, char flag) {
//        return formProperty.getId().contains("" + FLAG_SEPARATOR_CHAR + flag);
//    }

    private PrismObject<? extends ObjectType> getAdditionalData(Task task, Map<String,Object> variables, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return getChangeProcessor(task, variables).getAdditionalData(task, variables, result);
    }

    private PrismObject<? extends ObjectType> getTrackingData(Task task, Map<String,Object> variables, OperationResult result) throws ObjectNotFoundException, SchemaException {
        ProcessInstanceQuery piq = activitiEngine.getRuntimeService().createProcessInstanceQuery();
        piq.processInstanceId(task.getProcessInstanceId());
        org.activiti.engine.runtime.ProcessInstance processInstance = piq.singleResult();

        PrismObjectDefinition<TrackingDataFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(TrackingDataFormType.COMPLEX_TYPE);
        PrismObject<TrackingDataFormType> formPrism = formDefinition.instantiate();
        TrackingDataFormType form = formPrism.asObjectable();

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
