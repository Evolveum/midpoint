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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowException;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngineDataHelper;
import com.evolveum.midpoint.wf.impl.jobs.WfUtil;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;

import java.util.*;

import static com.evolveum.midpoint.schema.util.ObjectQueryUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNewType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNewType.F_CANDIDATE_ROLES_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNewType.F_WORK_ITEM_ID;
import static org.apache.commons.collections.CollectionUtils.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Used to retrieve (and provide) data about work items.
 *
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

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_COUNT_WORK_ITEMS_RELATED_TO_USER = DOT_INTERFACE + "countWorkItemsRelatedToUser";
    private static final String OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER = DOT_INTERFACE  + "listWorkItemsRelatedToUser";
    private static final String OPERATION_SEARCH_WORK_ITEMS = DOT_INTERFACE  + "searchWorkItem";
    private static final String OPERATION_COUNT_WORK_ITEMS = DOT_INTERFACE  + "countWorkItem";
    private static final String OPERATION_ACTIVITI_TASK_TO_WORK_ITEM = DOT_CLASS + "activitiTaskToWorkItem";
    private static final String OPERATION_ACTIVITI_DELEGATE_TASK_TO_WORK_ITEM = DOT_CLASS + "activitiDelegateTaskToWorkItem";
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
    public int countWorkItemsRelatedToUser(String userOid, boolean assigned, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_COUNT_WORK_ITEMS_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("assigned", assigned);
        try {
            int count = (int) createQueryForTasksRelatedToUser(userOid, assigned, result).count();
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
    @Deprecated
    public List<WorkItemType> listWorkItemsRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("assigned", assigned);
        result.addParam("first", first);
        result.addParam("count", count);
        List<Task> tasks;
        try {
            tasks = createQueryForTasksRelatedToUser(userOid, assigned, result).listPage(first, count);
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't list work items assigned/assignable to " + userOid, e);
            throw new SystemException("Couldn't list work items assigned/assignable to " + userOid + " due to Activiti exception", e);
        }

        List<WorkItemType> retval = tasksToWorkItems(tasks, false, false, true, result);       // there's no need to fill-in assignee details nor data forms; but candidates are necessary to fill-in
        result.computeStatusIfUnknown();
        return retval;
    }

    @Deprecated
    public List<WorkItemNewType> listWorkItemsNewRelatedToUser(String userOid, boolean assigned, int first, int count, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OPERATION_LIST_WORK_ITEMS_RELATED_TO_USER);
        result.addParam("userOid", userOid);
        result.addParam("assigned", assigned);
        result.addParam("first", first);
        result.addParam("count", count);
        List<Task> tasks;
        try {
            tasks = createQueryForTasksRelatedToUser(userOid, assigned, result).listPage(first, count);
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't list work items assigned/assignable to " + userOid, e);
            throw new SystemException("Couldn't list work items assigned/assignable to " + userOid + " due to Activiti exception", e);
        }

        List<WorkItemNewType> retval = tasksToWorkItemsNew(tasks, null, false, true, result);       // there's no need to fill-in assignee details nor data forms; but candidates are necessary to fill-in
        result.computeStatusIfUnknown();
        return retval;
    }

    private TaskQuery createQueryForTasksRelatedToUser(String oid, boolean assigned, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (assigned) {
            return activitiEngine.getTaskService().createTaskQuery()
                    .taskAssignee(oid)
                    .orderByTaskCreateTime().desc()
                    .includeTaskLocalVariables()
                    .includeProcessVariables();

        } else {
            return activitiEngine.getTaskService().createTaskQuery()
                    .taskUnassigned()
                    .taskCandidateGroupIn(miscDataUtil.getGroupsForUser(oid, result))
                    .orderByTaskCreateTime().desc()
                    .includeTaskLocalVariables()
                    .includeProcessVariables();
        }
    }

    // primitive 'query interpreter'
    private TaskQuery createTaskQuery(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        FilterComponents components = factorOutQuery(query, F_ASSIGNEE_REF, F_CANDIDATE_ROLES_REF, F_WORK_ITEM_ID);
        if (components.hasRemainder()) {
            throw new SchemaException("Unsupported clause(s) in search filter: " + components.getRemainderClauses());
        }

        final ItemPath WORK_ITEM_ID_PATH = new ItemPath(F_WORK_ITEM_ID);
        final ItemPath ASSIGNEE_PATH = new ItemPath(F_ASSIGNEE_REF);
        final ItemPath CANDIDATE_ROLES_PATH = new ItemPath(F_CANDIDATE_ROLES_REF);
        final ItemPath CREATED_PATH = new ItemPath(WorkItemNewType.F_WORK_ITEM_CREATED_TIMESTAMP);

        final Map.Entry<ItemPath, Collection<? extends PrismValue>> workItemIdFilter = components.getKnownComponent(WORK_ITEM_ID_PATH);
        final Map.Entry<ItemPath, Collection<? extends PrismValue>> assigneeFilter = components.getKnownComponent(ASSIGNEE_PATH);
        final Map.Entry<ItemPath, Collection<? extends PrismValue>> candidateRolesFilter = components.getKnownComponent(CANDIDATE_ROLES_PATH);

        TaskQuery taskQuery = activitiEngine.getTaskService().createTaskQuery();

		if (workItemIdFilter != null) {
			taskQuery = taskQuery.taskId(((PrismPropertyValue<String>) workItemIdFilter.getValue().iterator().next()).getValue());
		}

		if (assigneeFilter != null) {
            if (isNotEmpty(assigneeFilter.getValue())) {
                if (assigneeFilter.getValue().size() > 1) {
                    throw new SchemaException("Filter with more than one assignee is not supported: " + assigneeFilter.getValue());
                }
                taskQuery = taskQuery.taskAssignee(((PrismReferenceValue) assigneeFilter.getValue().iterator().next()).getOid());
            } else {
                taskQuery = taskQuery.taskUnassigned();
            }
        }

        if (candidateRolesFilter != null) {
            taskQuery = taskQuery.taskCandidateGroupIn(ObjectTypeUtil.referenceValueListToOidList((Collection<PrismReferenceValue>) candidateRolesFilter.getValue()));
        }

        if (query != null && query.getPaging() != null) {
            ObjectPaging paging = query.getPaging();
            if (paging.getOrderingInstructions().size() > 1) {
                throw new UnsupportedOperationException("Ordering by more than one property is not supported: " + paging.getOrderingInstructions());
            } else if (paging.getOrderingInstructions().size() == 1) {
                ItemPath orderBy = paging.getOrderBy();
                if (CREATED_PATH.equivalent(orderBy)) {
                    taskQuery = taskQuery.orderByTaskCreateTime();
                } else {
                    throw new UnsupportedOperationException("Ordering by " + orderBy + " is not currently supported");
                }

                switch (paging.getDirection()) {
                    case DESCENDING: taskQuery = taskQuery.desc(); break;
                    case ASCENDING:
                        default: taskQuery = taskQuery.asc(); break;
                }
            }
        }

        return taskQuery
                .includeTaskLocalVariables()
                .includeProcessVariables();
    }

    @Deprecated
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
            retval = taskToWorkItem(task, true, true, true, result);
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
     * In similar way, getAssigneeDetails influences whether details about assignee are filled-in.
     * And getCandidateDetails influences whether details about candidate users and groups are filled-in.
     * This should be skipped if there's no need to display these (e.g. in the list of work items assigned to the current user).
     */

    List<WorkItemType> tasksToWorkItems(List<Task> tasks, boolean getTaskDetails, boolean getAssigneeDetails, boolean getCandidateDetails, OperationResult result) {
        List<WorkItemType> retval = new ArrayList<WorkItemType>();
        for (Task task : tasks) {
            try {
                retval.add(taskToWorkItem(task, getTaskDetails, getAssigneeDetails, getCandidateDetails, result));
            } catch (WorkflowException e) {
                LoggingUtils.logException(LOGGER, "Couldn't get information on activiti task {}", e, task.getId());
            }
        }
        return retval;
    }

    SearchResultList<WorkItemNewType> tasksToWorkItemsNew(List<Task> tasks, Map<String, Object> processVariables,
            boolean getAssigneeDetails, boolean getCandidateDetails, OperationResult result) {
        SearchResultList<WorkItemNewType> retval = new SearchResultList<>(new ArrayList<WorkItemNewType>());
        for (Task task : tasks) {
            try {
                retval.add(taskToWorkItemNew(task, processVariables, getAssigneeDetails, getCandidateDetails, result));
            } catch (WorkflowException e) {
                LoggingUtils.logException(LOGGER, "Couldn't get information on activiti task {}", e, task.getId());
            }
        }
        return retval;
    }

    public <T extends Containerable> Integer countWorkItems(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createSubresult(OPERATION_COUNT_WORK_ITEMS);
        result.addParam("query", query);
        result.addCollectionOfSerializablesAsParam("options", options);
        try {
            TaskQuery taskQuery = createTaskQuery(query, options, result);
            int count = (int) taskQuery.count();
            result.computeStatusIfUnknown();
            return count;
        } catch (ActivitiException e) {
            result.recordFatalError("Couldn't count work items", e);
            throw new SystemException("Couldn't count work items due to Activiti exception", e);
        }
    }

    public <T extends Containerable> SearchResultList<T> searchWorkItems(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        OperationResult result = parentResult.createSubresult(OPERATION_SEARCH_WORK_ITEMS);
        result.addParam("query", query);
        result.addCollectionOfSerializablesAsParam("options", options);
        List<Task> tasks;
        try {
            TaskQuery taskQuery = createTaskQuery(query, options, result);
            Integer offset = query != null ? query.getOffset() : null;
            Integer maxSize = query != null ? query.getMaxSize() : null;
            if (offset == null && maxSize == null) {
                tasks = taskQuery.list();
            } else {
                tasks = taskQuery.listPage(defaultIfNull(offset, 0), defaultIfNull(maxSize, Integer.MAX_VALUE));
            }
        } catch (RuntimeException e) {
            result.recordFatalError("Couldn't list work items: " + e.getMessage(), e);
            throw new SystemException("Couldn't list work items due to an exception", e);
        } catch (SchemaException e) {
            result.recordFatalError("Couldn't list work items: " + e.getMessage(), e);
            throw e;
        }

        // there's no need to fill-in assignee details ; but candidates are necessary to fill-in
        // TODO implement based on options (resolve names)
        SearchResultList<WorkItemNewType> retval = tasksToWorkItemsNew(tasks, null, false, true, result);
        result.computeStatusIfUnknown();
        return (SearchResultList<T>) retval;
    }

    // should not throw ActivitiException

    /**
     * Helper class to carry relevant data from both Task and DelegateTask (to avoid code duplication)
     */
    private class TaskExtract {

        private String id;
        private String assignee;
        private String name;
        private String processInstanceId;
        private Date createTime;
        private String owner;
        private String executionId;
        private Map<String,Object> variables;
        private List<String> candidateUsers;
        private List<String> candidateGroups;

        TaskExtract(Task task) {
            id = task.getId();
            assignee = task.getAssignee();
            name = task.getName();
            processInstanceId = task.getProcessInstanceId();
            createTime = task.getCreateTime();
            owner = task.getOwner();
            executionId = task.getExecutionId();
            variables = new HashMap<>();
            if (task.getProcessVariables() != null) {
                variables.putAll(task.getProcessVariables());
            }
            if (task.getTaskLocalVariables() != null) {
                variables.putAll(task.getTaskLocalVariables());
            }
            candidateUsers = new ArrayList<>();
            candidateGroups = new ArrayList<>();
            TaskService taskService = activitiEngine.getTaskService();
            for (IdentityLink link : taskService.getIdentityLinksForTask(task.getId())) {
                if (IdentityLinkType.CANDIDATE.equals(link.getType())) {
                    if (link.getUserId() != null) {
                        candidateUsers.add(link.getUserId());
                    } else if (link.getGroupId() != null) {
                        candidateGroups.add(link.getGroupId());
                    } else {
                        throw new IllegalStateException("A link is defined to neither a user nor a group for task " + task.getId());
                    }
                }
            }
        }

//        TaskExtract(DelegateTask task) {
//            id = task.getId();
//            assignee = task.getAssignee();
//            name = task.getName();
//            processInstanceId = task.getProcessInstanceId();
//            createTime = task.getCreateTime();
//            owner = task.getOwner();
//            executionId = task.getExecutionId();
//            variables = task.getVariables();
//        }

        TaskExtract(TaskEvent task) {
            id = task.getTaskId();
            assignee = task.getAssigneeOid();
            name = task.getTaskName();
            processInstanceId = task.getProcessInstanceId();
            createTime = task.getCreateTime();
            owner = task.getOwner();
            executionId = task.getExecutionId();
            variables = task.getVariables();
            candidateUsers = task.getCandidateUsers();
            candidateGroups = task.getCandidateGroups();
        }

        public TaskExtract(Task task, Map<String, Object> processVariables) {
            this(task);
            if (processVariables != null) {
                for (Map.Entry<String, Object> variable: processVariables.entrySet()) {
                    if (!variables.containsKey(variable.getKey())) {
                        variables.put(variable.getKey(), variable.getValue());
                    }
                }
            }
        }

        String getId() {
            return id;
        }

        String getAssignee() {
            return assignee;
        }

        String getName() {
            return name;
        }

        String getProcessInstanceId() {
            return processInstanceId;
        }

        Date getCreateTime() {
            return createTime;
        }

        String getOwner() {
            return owner;
        }

        String getExecutionId() {
            return executionId;
        }

        Map<String,Object> getVariables() {
            return variables;
        }

        public List<String> getCandidateUsers() {
            return candidateUsers;
        }

        public List<String> getCandidateGroups() {
            return candidateGroups;
        }

        @Override
        public String toString() {
            return "Task{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", processInstanceId='" + processInstanceId + '\'' +
                    '}';
        }
    }

    private WorkItemType taskToWorkItem(Task task, boolean getTaskDetails, boolean getAssigneeDetails, boolean getCandidateDetails, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
        result.addParam("task id", task.getId());
        result.addParam("getTaskDetails", getTaskDetails);
        result.addParam("getAssigneeDetails", getAssigneeDetails);

        TaskExtract taskExtract = new TaskExtract(task);
        WorkItemType wi = taskExtractToWorkItem(taskExtract, getAssigneeDetails, getCandidateDetails, result);

        // this could be moved to taskExtractToWorkType after changing ChangeProcessor interface to accept TaskExtract instead of Task
        if (getTaskDetails) {
            try {
                Map<String, Object> variables = activitiEngineDataHelper.getProcessVariables(task.getId(), result);
                ChangeProcessor cp = getChangeProcessor(taskExtract, variables);

                PrismObject<UserType> requester = miscDataUtil.getRequester(variables, result);
                wi.setRequester(requester.asObjectable());
                wi.setRequesterRef(MiscSchemaUtil.createObjectReference(requester.getOid(), SchemaConstants.C_USER_TYPE));

                wi.setContents(asObjectable(cp.externalizeWorkItemContents(task, variables, result)));

                wi.setTrackingData(asObjectable(getTrackingData(taskExtract, variables, result)));
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

    private WorkItemNewType taskToWorkItemNew(Task task, Map<String, Object> processVariables, boolean getAssigneeDetails,
            boolean getCandidateDetails, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
        result.addParam("task id", task.getId());
        result.addParam("getAssigneeDetails", getAssigneeDetails);

        try {
            TaskExtract taskExtract = new TaskExtract(task, processVariables);
            WorkItemNewType wi = taskExtractToWorkItemNew(taskExtract, getAssigneeDetails, getCandidateDetails, result);
            return wi;
        } catch (RuntimeException|WorkflowException e) {
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    // this method should reside outside activiti-related packages
    // we'll deal with it when we implement support for multiple wf providers
    public WorkItemType taskEventToWorkItem(TaskEvent taskEvent, boolean getAssigneeDetails, boolean getCandidateDetails, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_DELEGATE_TASK_TO_WORK_ITEM);
        result.addParam("task id", taskEvent.getTaskId());
        result.addParam("getAssigneeDetails", getAssigneeDetails);

        WorkItemType wi = taskExtractToWorkItem(new TaskExtract(taskEvent), getAssigneeDetails, getCandidateDetails, result);
        result.recordSuccessIfUnknown();
        return wi;
    }

    private WorkItemType taskExtractToWorkItem(TaskExtract task, boolean getAssigneeDetails, boolean getCandidateDetails, OperationResult result) throws WorkflowException {
        WorkItemType wi = prismContext.createObject(WorkItemType.class).asObjectable();
        try {
            wi.setWorkItemId(task.getId());
            if (task.getAssignee() != null) {
                wi.setAssigneeRef(MiscSchemaUtil.createObjectReference(task.getAssignee(), SchemaConstants.C_USER_TYPE));
            }
            for (String candidateUser : task.getCandidateUsers()) {
                wi.getCandidateUsersRef().add(MiscSchemaUtil.createObjectReference(candidateUser, SchemaConstants.C_USER_TYPE));
            }
            for (String candidateGroup : task.getCandidateGroups()) {
                wi.getCandidateRolesRef().add(miscDataUtil.groupIdToObjectReference(candidateGroup));
            }
            wi.setName(new PolyStringType(task.getName()));
            wi.setProcessInstanceId(task.getProcessInstanceId());
            wi.setChangeProcessor((String) task.getVariables().get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR));
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
        }
        if (getCandidateDetails) {
            for (ObjectReferenceType ort : wi.getCandidateUsersRef()) {
                PrismObject<UserType> obj = miscDataUtil.getUserByOid(ort.getOid(), result);
                if (obj != null) {
                    wi.getCandidateUsers().add(obj.asObjectable());
                }
            }
            for (ObjectReferenceType ort : wi.getCandidateRolesRef()) {
                PrismObject<AbstractRoleType> obj = miscDataUtil.resolveObjectReference(ort, result);
                if (obj != null) {
                    wi.getCandidateRoles().add(obj.asObjectable());
                }
            }
        }
        return wi;
    }

    private WorkItemNewType taskExtractToWorkItemNew(TaskExtract task, boolean getAssigneeDetails, boolean getCandidateDetails, OperationResult result) throws WorkflowException {
        WorkItemNewType wi = new WorkItemNewType(prismContext);
        try {
            final Map<String, Object> variables = task.getVariables();

            wi.setWorkItemId(task.getId());
            wi.setName(task.getName());
            wi.setWorkItemCreatedTimestamp(XmlTypeConverter.createXMLGregorianCalendar(task.getCreateTime()));
            wi.setProcessStartedTimestamp(XmlTypeConverter.createXMLGregorianCalendar((Date) variables.get(CommonProcessVariableNames.VARIABLE_START_TIME)));
            String taskOid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
            if (taskOid != null) {
                wi.setTaskRef(ObjectTypeUtil.createObjectRef(taskOid, ObjectTypes.TASK));
            }
            if (task.getAssignee() != null) {
                wi.setAssigneeRef(MiscSchemaUtil.createObjectReference(task.getAssignee(), SchemaConstants.C_USER_TYPE));
            }
            for (String candidateUser : task.getCandidateUsers()) {
                wi.getCandidateUsersRef().add(MiscSchemaUtil.createObjectReference(candidateUser, SchemaConstants.C_USER_TYPE));
            }
            for (String candidateGroup : task.getCandidateGroups()) {
                wi.getCandidateRolesRef().add(miscDataUtil.groupIdToObjectReference(candidateGroup));
            }
            wi.setObjectRef(WfUtil.toObjectReferenceType((LightweightObjectRef) variables.get(CommonProcessVariableNames.VARIABLE_OBJECT_REF)));
            wi.setTargetRef(WfUtil.toObjectReferenceType((LightweightObjectRef) variables.get(CommonProcessVariableNames.VARIABLE_TARGET_REF)));
        } catch (ActivitiException e) {     // not sure if any of the above methods can throw this exception, but for safety we catch it here
            throw new WorkflowException("Couldn't get information on activiti task " + task.getId(), e);
        }
        return wi;
    }

    private <T> T asObjectable(PrismObject<? extends T> prismObject) {
        return prismObject != null ? prismObject.asObjectable() : null;
    }

    private ChangeProcessor getChangeProcessor(TaskExtract task, Map<String, Object> variables) {
        String cpClassName = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR);
        if (cpClassName == null) {
            throw new IllegalStateException("Change processor is unknown for task: " + task);
        }

        return wfConfiguration.findChangeProcessor(cpClassName);
    }

    private ChangeProcessor getChangeProcessor(Map<String, Object> variables, String context) {
        String cpClassName = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR);
        if (cpClassName == null) {
            throw new IllegalStateException("No change processor in " + context);
        }
        return wfConfiguration.findChangeProcessor(cpClassName);
    }

    private PrismObject<? extends TrackingDataType> getTrackingData(TaskExtract task, Map<String, Object> variables, OperationResult result) throws ObjectNotFoundException, SchemaException {
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
        //form.setTaskCandidates(getCandidatesAsString(task));
        form.setExecutionId(task.getExecutionId());
        form.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        form.setShadowTaskOid((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + formPrism.debugDump());
        }
        return formPrism;
    }

//    private List<String> getCandidates(TaskExtract task) {
//
//        List<String> retval = new ArrayList<String>();
//
//        TaskService taskService = activitiEngine.getTaskService();
//
//        List<IdentityLink> ils = taskService.getIdentityLinksForTask(task.getId());     // dangerous (activiti bug)
//        for (IdentityLink il : ils) {
//            if ("candidate".equals(il.getType())) {
//                if (il.getGroupId() != null) {
//                    retval.add("G:" + il.getGroupId());
//                }
//                if (il.getUserId() != null) {
//                    retval.add("U:" + il.getUserId());
//                }
//            }
//        }
//
//        return retval;
//    }
//
//    private String getCandidatesAsString(TaskExtract task) {
//
//        StringBuilder retval = new StringBuilder();
//        boolean first = true;
//        for (String c : getCandidates(task)) {
//            if (first) {
//                first = false;
//            } else {
//                retval.append(", ");
//            }
//            retval.append(c);
//        }
//        return retval.toString();
//    }

}
