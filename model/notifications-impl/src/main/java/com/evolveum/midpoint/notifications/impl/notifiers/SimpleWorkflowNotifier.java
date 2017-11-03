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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.*;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of a notifier dealing with workflow events (related to both work items and process instances).
 *
 * @author mederly
 */
@Component
public class SimpleWorkflowNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleWorkflowNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleWorkflowNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof WorkflowEvent)) {
            LOGGER.trace("SimpleWorkflowNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        }
        return true;
    }

	@Override
	protected UserType getDefaultRecipient(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
		@Nullable SimpleObjectRef recipientRef;
		if (event instanceof WorkflowProcessEvent) {
			recipientRef = event.getRequester();
		} else if (event instanceof WorkItemEvent) {
			recipientRef = ((WorkItemEvent) event).getAssignee();
		} else {
			return null;
		}
		ObjectType recipient = functions.getObjectType(recipientRef, false, result);
		if (recipient instanceof UserType) {
			return (UserType) recipient;
		} else {
			return null;
		}
	}

	@Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
		if (event instanceof WorkflowProcessEvent) {
			return event.isAdd() ? "Workflow process instance has been started" : "Workflow process instance has finished";
		} else if (event instanceof WorkItemEvent) {
			return getSubjectFromWorkItemEvent((WorkItemEvent) event, generalNotifierType, transport, task, result);
		} else {
			throw new UnsupportedOperationException("Unsupported event type for event=" + event);
		}
	}

	private String getSubjectFromWorkItemEvent(WorkItemEvent event, GeneralNotifierType generalNotifierType, String transport,
			Task task, OperationResult result) {
		if (event instanceof WorkItemLifecycleEvent) {
			if (event.isAdd()) {
				return "A new work item has been created";
			} else if (event.isDelete()) {
				if (event.getOperationKind() == WorkItemOperationKindType.COMPLETE) {
					return "Work item has been completed";
				} else {
					return "Work item has been cancelled";
				}
			} else {
				throw new UnsupportedOperationException("workItemLifecycle event with MODIFY operation is not supported");
			}
		} else if (event instanceof WorkItemAllocationEvent) {
			if (event.isAdd()) {
				return "Work item has been allocated to you";
			} else if (event.isModify()) {
				if (event.getOperationKind() == null) {
					throw new IllegalStateException("Missing operationKind in " + event);
				}
				String rv = "Work item will be automatically " + getOperationPastTenseVerb(event.getOperationKind());
				if (event.getTimeBefore() != null) {        // should always be
					rv += " in " + DurationFormatUtils.formatDurationWords(
							event.getTimeBefore().getTimeInMillis(new Date()), true, true);
				}
				return rv;
			} else {
				return "Work item has been " + getOperationPastTenseVerb(event.getOperationKind());
			}
		} else if (event instanceof WorkItemCustomEvent) {
			return "A notification about work item";
		} else {
			throw new UnsupportedOperationException("Unsupported event type for event=" + event);
		}
	}

	@Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) throws SchemaException {

        WorkflowEvent workflowEvent = (WorkflowEvent) event;

        boolean techInfo = Boolean.TRUE.equals(generalNotifierType.isShowTechnicalInformation());

        StringBuilder body = new StringBuilder();

        body.append(getSubject(event, generalNotifierType, transport, task, result));
        body.append("\n\n");

        appendGeneralInformation(body, workflowEvent);		// process instance name, work item name, stage, escalation level

		if (workflowEvent instanceof WorkItemEvent) {
			WorkItemEvent workItemEvent = (WorkItemEvent) workflowEvent;
			appendAssigneeInformation(body, workItemEvent, result);
			appendResultAndOriginInformation(body, workItemEvent, result);
			appendDeadlineInformation(body, workItemEvent);
        } else {
			appendResultInformation(body, workflowEvent, true);
		}
		body.append("\nNotification created on: ").append(new Date()).append("\n\n");

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            if (workflowEvent instanceof WorkItemEvent) {
				WorkItemEvent workItemEvent = (WorkItemEvent) workflowEvent;
				body.append("WorkItem:\n")
						.append(PrismUtil.serializeQuietly(prismContext, workItemEvent.getWorkItem()))
						.append("\n");
			}
			body.append("Workflow context:\n")
					.append(PrismUtil.serializeQuietly(prismContext, ((WorkflowEvent) event).getWorkflowContext()));
        }
        return body.toString();
    }

	private void appendGeneralInformation(StringBuilder sb, WorkflowEvent workflowEvent) {
		sb.append("Process instance name: ").append(workflowEvent.getProcessInstanceName()).append("\n");
		if (workflowEvent instanceof WorkItemEvent) {
			WorkItemEvent event = (WorkItemEvent) workflowEvent;
			sb.append("Work item: ").append(event.getWorkItemName()).append("\n");
			appendStageInformation(sb, event);
			appendEscalationInformation(sb, event);
		} else {
			appendStageInformation(sb, workflowEvent);
		}
		sb.append("\n");
	}

	private boolean appendResultInformation(StringBuilder body, WorkflowEvent workflowEvent, boolean emptyLineAfter) {
		if (workflowEvent.isDelete() && workflowEvent.isResultKnown()) {
			body.append("Result: ").append(workflowEvent.isApproved() ? "APPROVED" : "REJECTED").append("\n");
			if (emptyLineAfter) {
				body.append("\n");
			}
			return true;
		} else {
			return false;
		}
	}

	private void appendDeadlineInformation(StringBuilder sb, WorkItemEvent event) {
		WorkItemType workItem = event.getWorkItem();
		if (!isDone(event) && workItem.getDeadline() != null) {
			appendDeadlineInformation(sb, workItem, textFormatter);
		}
	}

	static void appendDeadlineInformation(StringBuilder sb, AbstractWorkItemType workItem, TextFormatter textFormatter) {
		XMLGregorianCalendar deadline = workItem.getDeadline();
		long before = XmlTypeConverter.toMillis(deadline) - System.currentTimeMillis();
		long beforeRounded = Math.round((double) before / 60000.0) * 60000L;
		String beforeWords = DurationFormatUtils.formatDurationWords(Math.abs(beforeRounded), true, true);
		String beforePhrase;
		if (beforeRounded > 0) {
			beforePhrase = " (in " + beforeWords + ")";
		} else if (beforeRounded < 0) {
			beforePhrase = " (" + beforeWords + " ago)";
		} else {
			beforePhrase = "";
		}
		sb.append("Deadline: ").append(textFormatter.formatDateTime(deadline)).append(beforePhrase).append("\n");
		sb.append("\n");
	}

	private void appendResultAndOriginInformation(StringBuilder sb, WorkItemEvent event, OperationResult result) {
		boolean atLeastOne = appendResultInformation(sb, event, false);
		WorkItemEventCauseInformationType cause = event.getCause();
		if (cause != null && cause.getType() == WorkItemEventCauseTypeType.TIMED_ACTION) {
			sb.append("Reason: ");
			if (cause.getDisplayName() != null) {
				sb.append(cause.getDisplayName()).append(" (timed action)");
			} else if (cause.getName() != null) {
				sb.append(cause.getName()).append(" (timed action)");
			} else {
				sb.append("Timed action");
			}
			sb.append("\n");
			atLeastOne = true;
		} else {
			SimpleObjectRef initiator = event.getInitiator();
			if (initiator != null && !isCancelled(event)) {
				UserType initiatorFull = (UserType) functions.getObjectType(initiator, true, result);
				sb.append("Carried out by: ").append(textFormatter.formatUserName(initiatorFull, initiator.getOid())).append("\n");
				atLeastOne = true;
			}
		}
		if (atLeastOne) {
			sb.append("\n");
		}
	}

	private void appendAssigneeInformation(StringBuilder sb, WorkItemEvent event, OperationResult result) {
		WorkItemType workItem = event.getWorkItem();
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
			sb.append("Allocated to");
			if (event.getOperationKind() == WorkItemOperationKindType.DELEGATE) {
				sb.append(event.isAdd() ? " (after delegation)" : " (before delegation)");
			} else if (event.getOperationKind() == WorkItemOperationKindType.ESCALATE) {
				sb.append(event.isAdd() ? " (after escalation)" : " (before escalation)");
			}
			sb.append(": ");
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

	// a bit of heuristics...
	private boolean isDone(WorkItemEvent event) {
		if (event instanceof WorkItemLifecycleEvent) {
			return event.isDelete();
		} else if (event instanceof WorkItemAllocationEvent) {
			return event.isDelete() &&
					(event.getOperationKind() == null || event.getOperationKind() == WorkItemOperationKindType.CANCEL
							|| event.getOperationKind() == WorkItemOperationKindType.COMPLETE);
		} else {
			return false;
		}
	}

	private boolean isCancelled(WorkItemEvent event) {
		return (event instanceof WorkItemLifecycleEvent || event instanceof WorkItemAllocationEvent)
				&& event.isDelete()
				&& (event.getOperationKind() == null || event.getOperationKind() == WorkItemOperationKindType.CANCEL);
	}

	private void appendEscalationInformation(StringBuilder sb, WorkItemEvent workItemEvent) {
		String info = WfContextUtil.getEscalationLevelInfo(workItemEvent.getWorkItem());
		if (info != null) {
			sb.append("Escalation level: ").append(info).append("\n");
		}
	}

	private void appendStageInformation(StringBuilder sb, WorkflowEvent workflowEvent) {
    	String info = WfContextUtil.getStageInfo(workflowEvent.getWorkflowContext());
    	if (info != null) {
    		sb.append("Stage: ").append(info).append("\n");
		}
	}

	@Override
    protected Trace getLogger() {
        return LOGGER;
    }

	private String getOperationPastTenseVerb(WorkItemOperationKindType operationKind) {
		if (operationKind == null) {
			return "cancelled";		// OK?
		}
		switch (operationKind) {
			case CLAIM: return "claimed";
			case RELEASE: return "released";
			case COMPLETE: return "completed";
			case DELEGATE: return "delegated";
			case ESCALATE: return "escalated";
			case CANCEL: return "cancelled";
			default: throw new IllegalArgumentException("operation kind: " + operationKind);
		}
	}

}
