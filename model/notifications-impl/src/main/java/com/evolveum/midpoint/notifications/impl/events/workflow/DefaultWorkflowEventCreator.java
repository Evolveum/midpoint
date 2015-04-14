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
import com.evolveum.midpoint.notifications.impl.NotificationsUtil;
import com.evolveum.midpoint.notifications.impl.SimpleObjectRefImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.PrimaryChangeProcessorState;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class DefaultWorkflowEventCreator implements WorkflowEventCreator {

    @Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Autowired
    private NotificationsUtil notificationsUtil;

    @Autowired
    private NotificationManager notificationManager;

    @PostConstruct
    public void init() {
        notificationManager.registerWorkflowEventCreator(ProcessInstanceState.class, this);
    }

    @Override
    public WorkflowProcessEvent createWorkflowProcessStartEvent(PrismObject<? extends ProcessInstanceState> instanceState, OperationResult result) {
        return createWorkflowProcessEvent(instanceState, ChangeType.ADD, result);
    }

    @Override
    public WorkflowProcessEvent createWorkflowProcessEndEvent(PrismObject<? extends ProcessInstanceState> instanceState, OperationResult result) {
        return createWorkflowProcessEvent(instanceState, ChangeType.DELETE, result);
    }

    private WorkflowProcessEvent createWorkflowProcessEvent(PrismObject<? extends ProcessInstanceState> instanceState, ChangeType changeType, OperationResult result) {
        WorkflowProcessEvent event = new WorkflowProcessEvent(lightweightIdentifierGenerator, changeType);
        fillInEvent(event, instanceState.asObjectable().getProcessInstanceName(), instanceState, instanceState.asObjectable().getAnswer(), result);
        return event;
    }

    private void fillInEvent(WorkflowEvent event, String instanceName, PrismObject<? extends ProcessInstanceState> instanceState, String decision, OperationResult result) {
        event.setProcessInstanceName(instanceName);
        event.setOperationStatusCustom(decision);
        event.setProcessInstanceState(instanceState);
        event.setRequester(new SimpleObjectRefImpl(notificationsUtil, instanceState.asObjectable().getRequesterOid()));
        if (instanceState.asObjectable().getObjectOid() != null) {
            event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, instanceState.asObjectable().getObjectOid()));
        }

        // fill-in requestee (for primary approval process variables)

        if (event.getRequestee() == null && instanceState.asObjectable().getProcessorSpecificState() instanceof PrimaryChangeProcessorState) {
            PrimaryChangeProcessorState pcpState = (PrimaryChangeProcessorState) instanceState.asObjectable().getProcessorSpecificState();
            if (pcpState.getObjectToBeAdded() != null) {
                ObjectType objectToBeAdded = pcpState.getObjectToBeAdded();
                if (objectToBeAdded instanceof UserType) {
                    event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, objectToBeAdded));
                }
            }
        }
    }

    @Override
    public WorkItemEvent createWorkItemCreateEvent(String workItemName, String assigneeOid, PrismObject<? extends ProcessInstanceState> instanceState) {
        return createWorkItemEvent(workItemName, assigneeOid, instanceState, ChangeType.ADD, null);
    }

    @Override
    public WorkItemEvent createWorkItemCompleteEvent(String workItemName, String assigneeOid, PrismObject<? extends ProcessInstanceState> instanceState, String decision) {
        return createWorkItemEvent(workItemName, assigneeOid, instanceState, ChangeType.DELETE, decision);
    }

    private WorkItemEvent createWorkItemEvent(String workItemName, String assigneeOid, PrismObject<? extends ProcessInstanceState> instanceState, ChangeType changeType, String decision) {
        WorkItemEvent event = new WorkItemEvent(lightweightIdentifierGenerator, changeType);
        event.setWorkItemName(workItemName);
        event.setAssignee(new SimpleObjectRefImpl(notificationsUtil, assigneeOid));
        fillInEvent(event, instanceState.asObjectable().getProcessInstanceName(), instanceState, decision, new OperationResult("dummy"));
        return event;
    }
}
