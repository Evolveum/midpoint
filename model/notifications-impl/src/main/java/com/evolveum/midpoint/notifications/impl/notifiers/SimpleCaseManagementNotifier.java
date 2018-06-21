/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.CaseWorkItemEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Default implementation of a notifier dealing with case management events; currently related to work items creation only.
 *
 * Derived from SimpleWorkflowNotifier. (But greatly simplified.) In the future we might try to somewhat unify these two.
 *
 * It is not complete yet. TODO add some useful information here, like the resource to act on, or the details of the account to create.
 *
 * @author mederly
 */
@Component
public class SimpleCaseManagementNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleCaseManagementNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleCaseManagementNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof CaseWorkItemEvent)) {
            LOGGER.trace("SimpleCaseManagementNotifier is not applicable for this kind of event, continuing in the handler chain; event class = {}", event.getClass());
            return false;
        }
        if (!event.isAdd()) {
	        LOGGER.trace("SimpleCaseManagementNotifier currently supports only ADD events");
	        return false;
        }
        return true;
    }

	@Override
	protected UserType getDefaultRecipient(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
		SimpleObjectRef recipientRef = ((CaseWorkItemEvent) event).getAssignee();
		ObjectType recipient = functions.getObjectType(recipientRef, false, result);
		if (recipient instanceof UserType) {
			return (UserType) recipient;
		} else {
			return null;
		}
	}

	@Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
		return "A new work item has been created";
	}

	@Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) throws SchemaException {

    	CaseWorkItemEvent workItemEvent = (CaseWorkItemEvent) event;

		boolean techInfo = isTrue(generalNotifierType.isShowTechnicalInformation());

		StringBuilder body = new StringBuilder();

        body.append(getSubject(event, generalNotifierType, transport, task, result));
        body.append("\n\n");

        appendGeneralInformation(body, workItemEvent);		// process instance name, work item name, stage, escalation level
		appendAssigneeInformation(body, workItemEvent, result);
		appendDeadlineInformation(body, workItemEvent);

		body.append("\nNotification created on: ").append(new Date()).append("\n\n");

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
			body.append("WorkItem:\n")
					.append(PrismUtil.serializeQuietly(prismContext, workItemEvent.getWorkItem()))
					.append("\n");
			body.append("Case:\n")
					.append(PrismUtil.serializeQuietly(prismContext, workItemEvent.getCase()));
        }
        return body.toString();
    }

	private void appendGeneralInformation(StringBuilder sb, CaseWorkItemEvent event) {
		sb.append("Work item: ").append(event.getWorkItemName()).append("\n");
	}

	private void appendDeadlineInformation(StringBuilder sb, CaseWorkItemEvent event) {
		CaseWorkItemType workItem = event.getWorkItem();
		if (workItem.getDeadline() != null) {
			SimpleWorkflowNotifier.appendDeadlineInformation(sb, workItem, textFormatter);
		}
	}

	private void appendAssigneeInformation(StringBuilder sb, CaseWorkItemEvent event, OperationResult result) {
		CaseWorkItemType workItem = event.getWorkItem();
		ObjectReferenceType originalAssignee = workItem.getOriginalAssigneeRef();
		List<ObjectReferenceType> currentAssignees = workItem.getAssigneeRef();
		boolean atLeastOne = false;
		if (currentAssignees.size() != 1 || !java.util.Objects.equals(originalAssignee.getOid(), currentAssignees.get(0).getOid())) {
			UserType originalAssigneeObject = (UserType) functions.getObjectType(originalAssignee, true, result);
			sb.append("Originally allocated to: ").append(
					textFormatter.formatUserName(originalAssigneeObject, originalAssignee.getOid())).append("\n");
			atLeastOne = true;
		}
		if (!workItem.getAssigneeRef().isEmpty()) {
			sb.append("Allocated to: ");
			sb.append(workItem.getAssigneeRef().stream()
					.map(ref -> textFormatter.formatUserName(ref, result))
					.collect(Collectors.joining(", ")));
			sb.append("\n");
			atLeastOne = true;
		}
		if (atLeastOne) {
			sb.append("\n");
		}
	}

	@Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
