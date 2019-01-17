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

package com.evolveum.midpoint.wf.impl.engine.dao;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WorkflowManagerImpl;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.wf.impl.engine.WorkflowInterface;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * Used to retrieve (and provide) data about work items.
 *
 * @author mederly
 */

@Component
public class WorkItemProvider {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemProvider.class);

    @Autowired private PrismContext prismContext;
	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private RepositoryService repositoryService;
	@Autowired private SchemaHelper schemaHelper;

    private static final String DOT_CLASS = WorkflowManagerImpl.class.getName() + ".";

	public Integer countWorkItems(ObjectQuery query, OperationResult result) throws SchemaException {
		return workflowEngine.countWorkItems(query, result);
	}

	public SearchResultList<WorkItemType> searchWorkItems(ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
		List<CaseWorkItemType> workItems = workflowEngine.searchWorkItems(query, options, result);
		return toFullWorkItems(workItems, result);
	}

	// special interface for ProcessInstanceProvider - TODO align with other interfaces
	SearchResultList<WorkItemType> getWorkItemsForCase(String caseOid, OperationResult result) throws SchemaException {
	    Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
			    .items(CaseWorkItemType.F_ASSIGNEE_REF, CaseWorkItemType.F_CANDIDATE_REF).resolve()
			    .build();
	    SearchResultList<CaseWorkItemType> workItems = workflowEngine.getWorkItemsForCase(caseOid, options, result);
	    return toFullWorkItems(workItems, result);
    }

    public WorkItemType getWorkItem(String workItemId, OperationResult result) throws SchemaException, ObjectNotFoundException {
		return workflowEngine.getFullWorkItem(workItemId, result);
    }

    private SearchResultList<WorkItemType> toFullWorkItems(List<CaseWorkItemType> workItems, OperationResult result) {
        SearchResultList<WorkItemType> retval = new SearchResultList<>(new ArrayList<>());
        Map<String, TaskType> ownerTasks = new HashMap<>();
        for (CaseWorkItemType workItem : workItems) {
            try {
                retval.add(toFullWorkItem(workItem, ownerTasks, result));
            } catch (RuntimeException|SchemaException e) {
				// operation result already contains corresponding error record
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get information on work item {}", e, workItem.getId());   // todo
            }
        }
        return retval;
    }

    // use version with the owner task, if known
    public WorkItemType toFullWorkItem(CaseWorkItemType workItem, OperationResult result) throws SchemaException {
		Map<String, TaskType> ownerTasks = new HashMap<>();
		return toFullWorkItem(workItem, ownerTasks, result);
    }

    public WorkItemType toFullWorkItem(CaseWorkItemType workItem, TaskType ownerTask, OperationResult result) throws SchemaException {
		Map<String, TaskType> ownerTasks = new HashMap<>();
		ownerTasks.put(ownerTask.getOid(), ownerTask);
		return toFullWorkItem(workItem, ownerTasks, result);
    }

    private WorkItemType toFullWorkItem(CaseWorkItemType workItem, Map<String, TaskType> ownerTasks,
		    OperationResult result) throws SchemaException {
    	if (workItem == null) {
    		return null;
		}
    	WorkItemType fullWorkItem = new WorkItemType(prismContext);
	    //noinspection unchecked
	    for (Item<?, ?> item : new ArrayList<>(emptyIfNull((List<Item<?, ?>>) workItem.asPrismContainerValue().getItems()))) {
//		    workItem.asPrismContainerValue().remove(item);
		    fullWorkItem.asPrismContainerValue().add(item.clone());
	    }
	    fullWorkItem.setId(workItem.getId());
	    CaseType aCase = CaseWorkItemUtil.getCase(workItem);
	    if (aCase != null) {
	    	if (aCase.getOid() != null) {
			    fullWorkItem.setExternalId(WorkflowInterface.createWorkItemId(aCase.getOid(), workItem.getId()));
		    }
		    String taskOid = WorkflowEngine.getTaskOidFromCaseName(aCase.getName().getOrig());
		    if (taskOid != null) {
			    TaskType ownerTask = resolveOwnerTask(taskOid, ownerTasks, result);
			    if (ownerTask.getWorkflowContext() == null) {
				    ownerTask.setWorkflowContext(new WfContextType(prismContext));
			    }
			    ownerTask.getWorkflowContext().getWorkItem().add(fullWorkItem);
		    }
	    }
	    return fullWorkItem;
    }

	private TaskType resolveOwnerTask(@NotNull String taskOid, Map<String, TaskType> ownerTasks, OperationResult result)
			throws SchemaException {
		if (ownerTasks.containsKey(taskOid)) {
			return ownerTasks.get(taskOid);     // might be null if the task is not resolvable
		} else {
			PrismObject<TaskType> task;
			try {
				task = repositoryService.getObject(TaskType.class, taskOid,
						schemaHelper.getOperationOptionsBuilder().allowNotFound().build(),
						result);
			} catch (ObjectNotFoundException e) {
				throw new IllegalStateException("Unexpected exception", e);
			}
			TaskType rv = task != null ? task.asObjectable() : null;
			ownerTasks.put(taskOid, rv);
			return rv;
		}
	}

//	public WorkItemType taskEventToWorkItemNew(WorkItemEvent workItemEvent, Map<String, Object> processVariables, boolean resolveTask,
//			boolean resolveAssignee, boolean resolveCandidates, OperationResult result) {
//		TaskExtract taskExtract = new TaskExtract(workItemEvent, processVariables, getTaskIdentityLinks(workItemEvent.getTaskId()));
//		return taskExtractToWorkItem(taskExtract, resolveTask, resolveAssignee, resolveCandidates, false, result);
//    }
}
