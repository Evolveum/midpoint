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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNewType;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.schema.constants.ObjectTypes.TASK;
import static com.evolveum.midpoint.schema.constants.ObjectTypes.USER;
import static com.evolveum.midpoint.schema.util.ObjectQueryUtil.FilterComponents;
import static com.evolveum.midpoint.schema.util.ObjectQueryUtil.factorOutQuery;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNewType.*;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
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
	private ProcessInterfaceFinder processInterfaceFinder;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_SEARCH_WORK_ITEMS = DOT_INTERFACE  + "searchWorkItem";
    private static final String OPERATION_COUNT_WORK_ITEMS = DOT_INTERFACE  + "countWorkItem";
    private static final String OPERATION_ACTIVITI_TASK_TO_WORK_ITEM = DOT_CLASS + "activitiTaskToWorkItem";

    /*
     * ========================= PART 1 - main operations =========================
     */

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

    SearchResultList<WorkItemNewType> tasksToWorkItemsNew(List<Task> tasks, Map<String, Object> processVariables,
            boolean resolveTask, boolean resolveAssignee, boolean resolveCandidates, OperationResult result) {
        SearchResultList<WorkItemNewType> retval = new SearchResultList<>(new ArrayList<WorkItemNewType>());
        for (Task task : tasks) {
            try {
                retval.add(taskToWorkItemNew(task, processVariables, resolveTask, resolveAssignee, resolveCandidates, result));
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
        SearchResultList<WorkItemNewType> retval = tasksToWorkItemsNew(tasks, null, false, false, true, result);
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

    private WorkItemNewType taskToWorkItemNew(Task task, Map<String, Object> processVariables, boolean resolveTask, boolean resolveAssignee,
            boolean resolveCandidates, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
        result.addParam("task id", task.getId());
        result.addParam("getAssigneeDetails", resolveAssignee);

        try {
            TaskExtract taskExtract = new TaskExtract(task, processVariables);
            WorkItemNewType wi = taskExtractToWorkItemNew(taskExtract, resolveTask, resolveAssignee, resolveCandidates, result);
            return wi;
        } catch (RuntimeException|WorkflowException e) {
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public WorkItemNewType taskEventToWorkItemNew(TaskEvent taskEvent, Map<String, Object> processVariables, boolean resolveTask,
			boolean resolveAssignee, boolean resolveCandidates, OperationResult parentResult) throws WorkflowException {
        OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
        result.addParam("task id", taskEvent.getTaskId());
        result.addParam("getAssigneeDetails", resolveAssignee);

        try {
            TaskExtract taskExtract = new TaskExtract(taskEvent);
            WorkItemNewType wi = taskExtractToWorkItemNew(taskExtract, resolveTask, resolveAssignee, resolveCandidates, result);
            return wi;
        } catch (RuntimeException|WorkflowException e) {
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public WorkItemNewType taskExtractToWorkItemNew(TaskExtract task, boolean resolveTask, boolean resolveAssignee, boolean resolveCandidates, OperationResult result) throws WorkflowException {
        WorkItemNewType wi = new WorkItemNewType(prismContext);
        try {
            final Map<String, Object> variables = task.getVariables();

            wi.setWorkItemId(task.getId());
            wi.setName(task.getName());
            wi.setWorkItemCreatedTimestamp(XmlTypeConverter.createXMLGregorianCalendar(task.getCreateTime()));
            wi.setProcessStartedTimestamp(XmlTypeConverter.createXMLGregorianCalendar((Date) variables.get(CommonProcessVariableNames.VARIABLE_START_TIME)));
            String taskOid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
            if (taskOid != null) {
                wi.setTaskRef(createObjectRef(taskOid, TASK));
				if (resolveTask) {
					miscDataUtil.resolveAndStoreObjectReference(wi.getTaskRef(), result);
				}
            }
            if (task.getAssignee() != null) {
                wi.setAssigneeRef(createObjectRef(task.getAssignee(), USER));
				if (resolveAssignee) {
					miscDataUtil.resolveAndStoreObjectReference(wi.getAssigneeRef(), result);
				}
            }
            for (String candidateUser : task.getCandidateUsers()) {
                wi.getCandidateUsersRef().add(createObjectRef(candidateUser, USER));
				if (resolveCandidates) {
					for (ObjectReferenceType ref : wi.getCandidateUsersRef()) {
						miscDataUtil.resolveAndStoreObjectReference(ref, result);
					}
				}
            }
            for (String candidateGroup : task.getCandidateGroups()) {
                wi.getCandidateRolesRef().add(miscDataUtil.groupIdToObjectReference(candidateGroup));
				if (resolveCandidates) {
					for (ObjectReferenceType ref : wi.getCandidateRolesRef()) {
						miscDataUtil.resolveAndStoreObjectReference(ref, result);
					}
				}
            }
            wi.setObjectRef(WfUtil.toObjectReferenceType((LightweightObjectRef) variables.get(CommonProcessVariableNames.VARIABLE_OBJECT_REF)));
            wi.setTargetRef(WfUtil.toObjectReferenceType((LightweightObjectRef) variables.get(CommonProcessVariableNames.VARIABLE_TARGET_REF)));

			ProcessMidPointInterface pmi = processInterfaceFinder.getProcessInterface(variables);
			wi.setDecision(pmi.extractDecision(variables));

        } catch (ActivitiException e) {     // not sure if any of the above methods can throw this exception, but for safety we catch it here
            throw new WorkflowException("Couldn't get information on activiti task " + task.getId(), e);
        }
        return wi;
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
