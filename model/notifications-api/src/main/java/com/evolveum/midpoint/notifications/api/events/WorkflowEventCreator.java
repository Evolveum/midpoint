/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;

/**
 * Used to create (fill-in) a workflow event based on information passed from workflow module.
 *
 * Although, generally, the mapping from that information to an event is quite straightforward,
 * filling-in some fields (e.g. the requestee information) is a bit dependent on particular workflow
 * process. Therefore, the author of wf process can supply his own implementation of workflow
 * event creator to deal with that situation.
 *
 * (If there are special requirements on how the notification should look like, one could
 * provide a custom notifier as well - see SimpleWorkflowNotifier for an inspiration.)
 *
 * @author mederly
 */
public interface WorkflowEventCreator {

    WorkflowProcessEvent createWorkflowProcessStartEvent(Task wfTask, OperationResult result);

    WorkflowProcessEvent createWorkflowProcessEndEvent(Task wfTask, OperationResult result);

    WorkItemEvent createWorkItemCreateEvent(WorkItemType workItem, Task wfTask, OperationResult result);

    WorkItemEvent createWorkItemCompleteEvent(WorkItemType workItem, Task wfTask, OperationResult result);
}
