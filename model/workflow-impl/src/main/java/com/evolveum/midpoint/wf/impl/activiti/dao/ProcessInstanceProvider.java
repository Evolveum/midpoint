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
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.F_WORK_ITEM;

/**
 * @author mederly
 */
@Component
public class ProcessInstanceProvider {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProcessInstanceProvider.class);

    @Autowired
    private WorkItemProvider workItemProvider;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";
    private static final String OPERATION_AUGMENT_TASK_OBJECT = DOT_INTERFACE + "augmentTaskObject";

	// doesn't throw any exceptions - these are logged and stored into the operation result
    public <T extends ObjectType> void augmentTaskObject(PrismObject<T> object,
            Collection<SelectorOptions<GetOperationOptions>> options, Task opTask, OperationResult parentResult) {

        final OperationResult result = parentResult.createSubresult(OPERATION_AUGMENT_TASK_OBJECT);
        result.addParam("object", ObjectTypeUtil.toShortString(object));
		result.addArbitraryObjectCollectionAsParam("options", options);

        if (!(object.asObjectable() instanceof TaskType)) {
            result.recordNotApplicableIfUnknown();
            return;
        }
        final TaskType taskType = (TaskType) object.asObjectable();

        try {
            if (taskType.getWorkflowContext() == null) {
                return;
            }
            final String instanceId = taskType.getWorkflowContext().getProcessInstanceId();
            if (instanceId == null) {
                return;
            }
            final boolean retrieveWorkItems = SelectorOptions.hasToLoadPath(new ItemPath(F_WORKFLOW_CONTEXT, F_WORK_ITEM), options);
            if (!retrieveWorkItems) {
                // We assume that everything (except work items) is already stored in repo.
                return;
            }
			final List<WorkItemType> workItems = workItemProvider.getWorkItemsForProcessInstanceId(instanceId, result);
			taskType.getWorkflowContext().getWorkItem().addAll(CloneUtil.cloneCollectionMembers(workItems));
        } catch (RuntimeException e) {
            result.recordFatalError(e.getMessage(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't prepare wf-related information for {}", e, ObjectTypeUtil.toShortString(object));
        } finally {
            result.computeStatusIfUnknown();
        }

		if (!result.isSuccess()) {
			taskType.setFetchResult(result.createOperationResultType());
		}
	}

	// doesn't throw any exceptions - these are logged and stored into the operation result
	public <T extends ObjectType> void augmentTaskObjectList(SearchResultList<PrismObject<T>> list,
            Collection<SelectorOptions<GetOperationOptions>> options, com.evolveum.midpoint.task.api.Task task, OperationResult result) {
        for (PrismObject<T> object : list) {
            augmentTaskObject(object, options, task, result);
        }
    }
}
