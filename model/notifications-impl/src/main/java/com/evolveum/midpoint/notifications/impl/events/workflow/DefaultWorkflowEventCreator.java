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

package com.evolveum.midpoint.notifications.impl.events.workflow;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.WorkItemEvent;
import com.evolveum.midpoint.notifications.api.events.WorkflowEvent;
import com.evolveum.midpoint.notifications.api.events.WorkflowEventCreator;
import com.evolveum.midpoint.notifications.api.events.WorkflowProcessEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFuctionsImpl;
import com.evolveum.midpoint.notifications.impl.SimpleObjectRefImpl;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class DefaultWorkflowEventCreator implements WorkflowEventCreator {

    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Autowired
    private NotificationFuctionsImpl notificationsUtil;

    @Autowired
    private NotificationManager notificationManager;

    @Override
    public WorkflowProcessEvent createWorkflowProcessStartEvent(Task wfTask, OperationResult result) {
        return createWorkflowProcessEvent(wfTask, ChangeType.ADD, result);
    }

    @Override
    public WorkflowProcessEvent createWorkflowProcessEndEvent(Task wfTask, OperationResult result) {
        return createWorkflowProcessEvent(wfTask, ChangeType.DELETE, result);
    }

    private WorkflowProcessEvent createWorkflowProcessEvent(Task wfTask, ChangeType changeType, OperationResult result) {
        WorkflowProcessEvent event = new WorkflowProcessEvent(lightweightIdentifierGenerator, changeType, wfTask.getWorkflowContext());
        fillInEvent(event, wfTask);
        return event;
    }

    private void fillInEvent(WorkflowEvent event, Task wfTask) {
		WfContextType wfc = wfTask.getWorkflowContext();
        event.setRequester(new SimpleObjectRefImpl(notificationsUtil, wfc.getRequesterRef()));
        if (wfc.getObjectRef() != null) {
            event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, wfc.getObjectRef()));
        }
		// TODO what if requestee is yet to be created?
    }

    @Override
    public WorkItemEvent createWorkItemCreateEvent(WorkItemType workItem, Task wfTask, OperationResult result) {
        return createWorkItemEvent(workItem, wfTask, ChangeType.ADD);
    }

    @Override
    public WorkItemEvent createWorkItemCompleteEvent(WorkItemType workItem, Task wfTask, OperationResult result) {
        return createWorkItemEvent(workItem, wfTask, ChangeType.DELETE);
    }

    private WorkItemEvent createWorkItemEvent(WorkItemType workItemType, Task wfTask, ChangeType changeType) {
		SimpleObjectRefImpl assignee = workItemType.getAssigneeRef() != null ?
				new SimpleObjectRefImpl(notificationsUtil, workItemType.getAssigneeRef()) : null;
        WorkItemEvent event = new WorkItemEvent(lightweightIdentifierGenerator, changeType, workItemType, assignee, wfTask.getWorkflowContext());
        fillInEvent(event, wfTask);
        return event;
    }
}
