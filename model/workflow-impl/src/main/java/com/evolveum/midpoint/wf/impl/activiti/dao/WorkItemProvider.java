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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.messages.TaskEvent;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.schema.constants.ObjectTypes.TASK;
import static com.evolveum.midpoint.schema.constants.ObjectTypes.USER;
import static com.evolveum.midpoint.schema.util.ObjectQueryUtil.FilterComponents;
import static com.evolveum.midpoint.schema.util.ObjectQueryUtil.factorOutQuery;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType.*;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
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
    private MiscDataUtil miscDataUtil;

    @Autowired
    private PrismContext prismContext;

	@Autowired
	private ProcessInterfaceFinder processInterfaceFinder;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";

    private static final String OPERATION_ACTIVITI_TASK_TO_WORK_ITEM = DOT_CLASS + "activitiTaskToWorkItem";

	public Integer countWorkItems(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
		TaskQuery taskQuery = createTaskQuery(query, false, options, result);
		return taskQuery != null ? (int) taskQuery.count() : 0;
	}

	public SearchResultList<WorkItemType> searchWorkItems(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
			throws SchemaException {
		TaskQuery taskQuery = createTaskQuery(query, true, options, result);
		if (taskQuery == null) {
			return new SearchResultList<>(Collections.emptyList());
		}
		Integer offset = query != null ? query.getOffset() : null;
		Integer maxSize = query != null ? query.getMaxSize() : null;
		List<Task> tasks;
		if (offset == null && maxSize == null) {
			tasks = taskQuery.list();
		} else {
			tasks = taskQuery.listPage(defaultIfNull(offset, 0), defaultIfNull(maxSize, Integer.MAX_VALUE));
		}
		boolean getAllVariables = true;				// TODO implement based on options
		// there's no need to fill-in assignee details ; but candidates are necessary to fill-in; TODO implement based on options (resolve)
		return tasksToWorkItems(tasks, null, false, false, true, getAllVariables, result);
	}

	// primitive 'query interpreter'
	// returns null if no results should be returned
    private TaskQuery createTaskQuery(ObjectQuery query, boolean includeVariables, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        FilterComponents components = factorOutQuery(query, F_ASSIGNEE_REF, F_CANDIDATE_REF, F_WORK_ITEM_ID);
		List<ObjectFilter> remainingClauses = components.getRemainderClauses();
		if (!remainingClauses.isEmpty()) {
            throw new SchemaException("Unsupported clause(s) in search filter: " + remainingClauses);
        }

        final ItemPath WORK_ITEM_ID_PATH = new ItemPath(F_WORK_ITEM_ID);
        final ItemPath ASSIGNEE_PATH = new ItemPath(F_ASSIGNEE_REF);
        final ItemPath CANDIDATE_PATH = new ItemPath(F_CANDIDATE_REF);
        final ItemPath CREATED_PATH = new ItemPath(WorkItemType.F_WORK_ITEM_CREATED_TIMESTAMP);

        final Map.Entry<ItemPath, Collection<? extends PrismValue>> workItemIdFilter = components.getKnownComponent(WORK_ITEM_ID_PATH);
        final Map.Entry<ItemPath, Collection<? extends PrismValue>> assigneeFilter = components.getKnownComponent(ASSIGNEE_PATH);
        final Map.Entry<ItemPath, Collection<? extends PrismValue>> candidateRolesFilter = components.getKnownComponent(CANDIDATE_PATH);

        TaskQuery taskQuery = activitiEngine.getTaskService().createTaskQuery();

		if (workItemIdFilter != null) {
			Collection<? extends PrismValue> filterValues = workItemIdFilter.getValue();
			if (filterValues.size() > 1) {
				throw new IllegalArgumentException("In a query there must be exactly one value for workItemId: " + filterValues);
			}
			taskQuery = taskQuery.taskId(((PrismPropertyValue<String>) filterValues.iterator().next()).getValue());
		}

		if (assigneeFilter != null) {
			taskQuery = addAssigneesToQuery(taskQuery, assigneeFilter);
		}

        if (candidateRolesFilter != null) {
			// TODO what about candidate users? (currently these are not supported)
			List<String> candidateGroups = MiscDataUtil.prismRefsToStrings((Collection<PrismReferenceValue>) candidateRolesFilter.getValue());
			if (!candidateGroups.isEmpty()) {
				taskQuery = taskQuery.taskCandidateGroupIn(candidateGroups);
			} else {
				return null;			// no groups -> no result
			}
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

		if (includeVariables) {
			return taskQuery
					.includeTaskLocalVariables()
					.includeProcessVariables();
		} else {
			return taskQuery;
		}
    }

	private TaskQuery addAssigneesToQuery(TaskQuery taskQuery, Map.Entry<ItemPath, Collection<? extends PrismValue>> assigneeFilter) {
		@SuppressWarnings("unchecked")
		Collection<PrismReferenceValue> assigneeRefs = (Collection<PrismReferenceValue>) assigneeFilter.getValue();
		if (isEmpty(assigneeRefs)) {
			return taskQuery.taskUnassigned();
		} else {
			List<String> values = MiscDataUtil.prismRefsToStrings(assigneeRefs);
			return taskQuery.taskInvolvedUser(StringUtils.join(values, ';'));
		}
	}

	// special interface for ProcessInstanceProvider - TODO align with other interfaces
    public SearchResultList<WorkItemType> getWorkItemsForProcessInstanceId(String processInstanceId, OperationResult result) {
        TaskService ts = activitiEngine.getTaskService();
        List<Task> tasks = ts.createTaskQuery()
                .processInstanceId(processInstanceId)
                .includeTaskLocalVariables()
                .includeProcessVariables()
                .list();
        return tasksToWorkItems(tasks, null, false, true, true, true, result);
    }

    public WorkItemType getWorkItem(String workItemId, OperationResult result) {
        TaskService ts = activitiEngine.getTaskService();
        Task task = ts.createTaskQuery()
                .taskId(workItemId)
                .includeTaskLocalVariables()
                .includeProcessVariables()
                .singleResult();
        return taskToWorkItem(task, null, false, false, false, false, result);
    }

    private SearchResultList<WorkItemType> tasksToWorkItems(List<Task> tasks, Map<String, Object> processVariables,
            boolean resolveTask, boolean resolveAssignee, boolean resolveCandidates, boolean fetchAllVariables, OperationResult result) {
        SearchResultList<WorkItemType> retval = new SearchResultList<>(new ArrayList<WorkItemType>());
        for (Task task : tasks) {
            try {
                retval.add(taskToWorkItem(task, processVariables, resolveTask, resolveAssignee, resolveCandidates, fetchAllVariables, result));
            } catch (RuntimeException e) {
				// operation result already contains corresponding error record
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get information on activiti task {}", e, task.getId());
            }
        }
        return retval;
    }

    @NotNull
	public List<ObjectReferenceType> getMidpointAssignees(TaskExtract taskExtract) {
		List<ObjectReferenceType> rv = new ArrayList<>();
		for (IdentityLink link : taskExtract.getTaskIdentityLinks()) {
			if (CommonProcessVariableNames.MIDPOINT_ASSIGNEE.equals(link.getType())) {
				if (link.getUserId() != null) {
					rv.add(MiscDataUtil.stringToRef(link.getUserId()));
				}
				if (link.getGroupId() != null) {		// just for completeness (currently we don't expect groups to be here)
					rv.add(MiscDataUtil.stringToRef(link.getGroupId()));
				}
			}
		}
		return rv;
	}

	/**
     * Helper class to carry relevant data from both Task and DelegateTask (to avoid code duplication)
     */
    private class TaskExtract {

        private String id;
        private String assignee;
        private String name;
        private String processInstanceId;
        private Date createTime;
        private Date dueDate;
        private String owner;
        private String executionId;
        private Map<String,Object> variables;
        private List<String> candidateUsers;
        private List<String> candidateGroups;
		private final List<IdentityLink> taskIdentityLinks;

        TaskExtract(TaskEvent task, Map<String, Object> processVariables, List<IdentityLink> taskIdentityLinks) {
            id = task.getTaskId();
            assignee = task.getAssigneeOid();
            name = task.getTaskName();
            processInstanceId = task.getProcessInstanceId();
            createTime = task.getCreateTime();
            dueDate = task.getDueDate();
            owner = task.getOwner();
            executionId = task.getExecutionId();
            variables = task.getVariables();
            candidateUsers = task.getCandidateUsers();
            candidateGroups = task.getCandidateGroups();
			addProcessVariables(processVariables);
			this.taskIdentityLinks = taskIdentityLinks;
        }

        TaskExtract(Task task, Map<String, Object> processVariables, List<IdentityLink> taskIdentityLinks) {
			id = task.getId();
			assignee = task.getAssignee();
			name = task.getName();
			processInstanceId = task.getProcessInstanceId();
			createTime = task.getCreateTime();
			dueDate = task.getDueDate();
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
			for (IdentityLink link : taskIdentityLinks) {
				if (IdentityLinkType.CANDIDATE.equals(link.getType())) {
					if (link.getUserId() != null) {
						candidateUsers.add(link.getUserId());
					} else if (link.getGroupId() != null) {
						candidateGroups.add(link.getGroupId());
					}
				}
			}
			addProcessVariables(processVariables);
			this.taskIdentityLinks = taskIdentityLinks;
		}

		private void addProcessVariables(Map<String, Object> processVariables) {
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

        ObjectReferenceType getAssignee() {
            return assignee != null ? MiscDataUtil.stringToRef(assignee) : null;
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

		Date getDueDate() {
			return dueDate;
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

		public List<IdentityLink> getTaskIdentityLinks() {
			return taskIdentityLinks;
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

    private WorkItemType taskToWorkItem(Task task, Map<String, Object> processVariables, boolean resolveTask, boolean resolveAssignee,
            boolean resolveCandidates, boolean fetchAllVariables, OperationResult result) {
    	if (task == null) {
    		return null;
		}
		TaskExtract taskExtract = new TaskExtract(task, processVariables, getTaskIdentityLinks(task.getId()));
		return taskExtractToWorkItem(taskExtract, resolveTask, resolveAssignee, resolveCandidates, fetchAllVariables, result);
    }

	private List<IdentityLink> getTaskIdentityLinks(String taskId) {
		return activitiEngine.getTaskService().getIdentityLinksForTask(taskId);
	}

	public WorkItemType taskEventToWorkItemNew(TaskEvent taskEvent, Map<String, Object> processVariables, boolean resolveTask,
			boolean resolveAssignee, boolean resolveCandidates, OperationResult result) {
		TaskExtract taskExtract = new TaskExtract(taskEvent, processVariables, getTaskIdentityLinks(taskEvent.getTaskId()));
		return taskExtractToWorkItem(taskExtract, resolveTask, resolveAssignee, resolveCandidates, false, result);
    }

    public WorkItemType taskExtractToWorkItem(TaskExtract task, boolean resolveTask, boolean resolveAssignee,
			boolean resolveCandidates, boolean fetchAllVariables, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(OPERATION_ACTIVITI_TASK_TO_WORK_ITEM);
		result.addParams(new String [] { "activitiTaskId", "resolveTask", "resolveAssignee", "resolveCandidates" },
				task.getId(), resolveTask, resolveAssignee, resolveCandidates);
		try {

			WorkItemType wi = new WorkItemType(prismContext);
			final Map<String, Object> variables = task.getVariables();

			wi.setWorkItemId(task.getId());
			wi.setProcessInstanceId(task.getProcessInstanceId());
			wi.setName(task.getName());
			wi.setWorkItemCreatedTimestamp(XmlTypeConverter.createXMLGregorianCalendar(task.getCreateTime()));
			wi.setProcessStartedTimestamp(XmlTypeConverter.createXMLGregorianCalendar(ActivitiUtil.getRequiredVariable(
					variables, CommonProcessVariableNames.VARIABLE_START_TIME, Date.class, prismContext)));
			wi.setDeadline(XmlTypeConverter.createXMLGregorianCalendar(task.getDueDate()));

			String taskOid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
			if (taskOid != null) {
				wi.setTaskRef(createObjectRef(taskOid, TASK));
				if (resolveTask) {
					miscDataUtil.resolveAndStoreObjectReference(wi.getTaskRef(), result);
				}
			}

			// assignees
			wi.getAssigneeRef().addAll(getMidpointAssignees(task));
			String originalAssigneeString = ActivitiUtil.getVariable(variables,
					CommonProcessVariableNames.VARIABLE_ORIGINAL_ASSIGNEE, String.class, prismContext);
			if (originalAssigneeString != null) {
				wi.setOriginalAssigneeRef(MiscDataUtil.stringToRef(originalAssigneeString));
			}
			if (resolveAssignee) {
				miscDataUtil.resolveAndStoreObjectReferences(wi.getAssigneeRef(), result);
				miscDataUtil.resolveAndStoreObjectReference(wi.getOriginalAssigneeRef(), result);
			}

			// candidates
			task.getCandidateUsers().forEach(s -> wi.getCandidateRef().add(createObjectRef(s, USER)));
			task.getCandidateGroups().forEach(s -> wi.getCandidateRef().add(MiscDataUtil.stringToRef(s)));
			if (resolveCandidates) {
				miscDataUtil.resolveAndStoreObjectReferences(wi.getCandidateRef(), result);
			}

			// other
			wi.setObjectRef(MiscDataUtil.toObjectReferenceType((LightweightObjectRef) variables.get(CommonProcessVariableNames.VARIABLE_OBJECT_REF)));
			wi.setTargetRef(MiscDataUtil.toObjectReferenceType((LightweightObjectRef) variables.get(CommonProcessVariableNames.VARIABLE_TARGET_REF)));

			ProcessMidPointInterface pmi = processInterfaceFinder.getProcessInterface(variables);
			wi.setResult(pmi.extractWorkItemResult(variables));
			String completedBy = ActivitiUtil.getVariable(variables, CommonProcessVariableNames.VARIABLE_WORK_ITEM_COMPLETED_BY, String.class, prismContext);
			if (completedBy != null) {
				wi.setCompletedByRef(ObjectTypeUtil.createObjectRef(completedBy, ObjectTypes.USER));
			}

			wi.setStageNumber(pmi.getStageNumber(variables));
			wi.setStageCount(pmi.getStageCount(variables));
			wi.setStageName(pmi.getStageName(variables));
			wi.setStageDisplayName(pmi.getStageDisplayName(variables));
			wi.setStageName(pmi.getStageName(variables));
			wi.setStageDisplayName(pmi.getStageDisplayName(variables));

			wi.setEscalationLevelNumber(pmi.getEscalationLevelNumber(variables));
			wi.setEscalationLevelName(pmi.getEscalationLevelName(variables));
			wi.setEscalationLevelDisplayName(pmi.getEscalationLevelDisplayName(variables));

			// This is just because 'variables' switches in task query DO NOT fetch all required variables...
			if (fetchAllVariables) {		// TODO can we do this e.g. in the task completion listener?
				Map<String, Object> allVariables = activitiEngine.getTaskService().getVariables(task.getId());
				wi.setProcessSpecificPart(pmi.extractProcessSpecificWorkItemPart(allVariables));
				wi.getAdditionalInformation().addAll(pmi.getAdditionalInformation(allVariables));
			}

			return wi;

		} catch (RuntimeException e) {
			result.recordFatalError("Couldn't convert activiti task " + task.getId() + " to midPoint WorkItem: " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}
}
