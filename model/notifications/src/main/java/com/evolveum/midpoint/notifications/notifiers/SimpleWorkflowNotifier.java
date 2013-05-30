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

package com.evolveum.midpoint.notifications.notifiers;

import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.events.WorkItemEvent;
import com.evolveum.midpoint.notifications.events.WorkflowEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SimpleWorkflowNotifierType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @author mederly
 */
@Component
public class SimpleWorkflowNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleWorkflowNotifier.class);
    private static final Integer LEVEL_TECH_INFO = 10;

    @PostConstruct
    public void init() {
        register(SimpleWorkflowNotifierType.class);
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof WorkflowEvent)) {
            LOGGER.trace("SimpleWorkflowNotifier was called with incompatible notification event; class = " + event.getClass());
            return false;
        }
        return true;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) {

        if (event.isAdd()) {
            if (event instanceof WorkItemEvent) {
                return "A new work item has been created";
            } else {
                return "Workflow process instance has been started";
            }
        } else {
            if (event instanceof WorkItemEvent) {
                return "Work item has been completed";
            } else {
                return "Workflow process instance has finished";
            }
        }
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, OperationResult result) throws SchemaException {

        WorkflowEvent workflowEvent = (WorkflowEvent) event;

        boolean techInfo = generalNotifierType.getLevelOfDetail() != null && generalNotifierType.getLevelOfDetail() >= LEVEL_TECH_INFO;

        StringBuilder body = new StringBuilder();

        body.append(getSubject(event, generalNotifierType, transport, result));
        body.append("\n\n");

        body.append("Process instance name: " + workflowEvent.getProcessName());
        if (workflowEvent instanceof WorkItemEvent) {
            WorkItemEvent workItemEvent = (WorkItemEvent) workflowEvent;
            body.append("Work item: " + workItemEvent.getWorkItemName() + "\n");
            ObjectType assigneeType = notificationsUtil.getObjectType(workItemEvent.getAssignee(), result);
            if (assigneeType != null) {
                body.append("Assignee: " + assigneeType.getName() + "\n");
            }
        }
        body.append("\n");
        if (event.isDelete() && workflowEvent.isResultKnown()) {
            body.append("Result: " + (workflowEvent.isApproved() ? "APPROVED" : "REJECTED") + "\n\n");
        }
        body.append("Notification created on: " + new Date() + "\n\n");

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            for (String key : workflowEvent.getVariables().keySet()) {
                body.append(key + " = " + workflowEvent.getVariables().get(key) + "\n");
            }
        }

        return body.toString();
    }

}
