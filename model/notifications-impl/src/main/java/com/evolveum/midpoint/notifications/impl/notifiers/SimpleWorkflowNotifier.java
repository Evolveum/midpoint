/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.notifications.impl.events.*;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.*;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Default implementation of a notifier dealing with workflow events (related to both work items and process instances).
 */
@Component
public class SimpleWorkflowNotifier extends AbstractGeneralNotifier<WorkflowEvent, SimpleWorkflowNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleWorkflowNotifier.class);

    @Override
    public Class<WorkflowEvent> getEventType() {
        return WorkflowEvent.class;
    }

    @Override
    public Class<SimpleWorkflowNotifierType> getEventHandlerConfigurationType() {
        return SimpleWorkflowNotifierType.class;
    }

    @Override
    protected UserType getDefaultRecipient(WorkflowEvent event, SimpleWorkflowNotifierType configuration, OperationResult result) {
        @Nullable SimpleObjectRef recipientRef;
        if (event instanceof WorkflowProcessEventImpl) {
            recipientRef = event.getRequester();
        } else if (event instanceof WorkItemEventImpl) {
            recipientRef = ((WorkItemEventImpl) event).getAssignee();
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
    protected String getSubject(WorkflowEvent event, SimpleWorkflowNotifierType configuration, String transport, Task task, OperationResult result) {
        if (event instanceof WorkflowProcessEventImpl) {
            return event.isAdd() ? "Workflow process instance has been started" : "Workflow process instance has finished";
        } else if (event instanceof WorkItemEventImpl) {
            return getSubjectFromWorkItemEvent((WorkItemEventImpl) event);
        } else {
            throw new UnsupportedOperationException("Unsupported event type for event=" + event);
        }
    }

    private String getSubjectFromWorkItemEvent(WorkItemEventImpl event) {
        if (event instanceof WorkItemLifecycleEventImpl) {
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
        } else if (event instanceof WorkItemAllocationEventImpl) {
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
        } else if (event instanceof WorkItemCustomEventImpl) {
            return "A notification about work item";
        } else {
            throw new UnsupportedOperationException("Unsupported event type for event=" + event);
        }
    }

    @Override
    protected String getBody(WorkflowEvent event, SimpleWorkflowNotifierType configuration,
            String transport, Task task, OperationResult result) {

        boolean techInfo = Boolean.TRUE.equals(configuration.isShowTechnicalInformation());

        StringBuilder body = new StringBuilder();

        body.append(getSubject(event, configuration, transport, task, result));
        body.append("\n\n");

        appendGeneralInformation(body, event); // process instance name, work item name, stage, escalation level

        if (event instanceof WorkItemEventImpl) {
            WorkItemEventImpl workItemEvent = (WorkItemEventImpl) event;
            appendAssigneeInformation(body, workItemEvent, result);
            appendResultAndOriginInformation(body, workItemEvent, result);
            appendDeadlineInformation(body, workItemEvent);
        } else {
            appendResultInformation(body, event, true);
        }
        body.append("\nNotification created on: ").append(new Date()).append("\n\n");

        if (techInfo) {
            body.append("----------------------------------------\n");
            body.append("Technical information:\n\n");
            if (event instanceof WorkItemEventImpl) {
                WorkItemEventImpl workItemEvent = (WorkItemEventImpl) event;
                body.append("WorkItem:\n")
                        .append(PrismUtil.serializeQuietly(prismContext, workItemEvent.getWorkItem()))
                        .append("\n");
            }
            body.append("Workflow context:\n")
                    .append(PrismUtil.serializeQuietly(prismContext, event.getApprovalContext()));
        }
        return body.toString();
    }

    private void appendGeneralInformation(StringBuilder sb, WorkflowEvent workflowEvent) {
        sb.append("Process instance name: ").append(workflowEvent.getProcessInstanceName()).append("\n");
        if (workflowEvent instanceof WorkItemEventImpl) {
            WorkItemEventImpl event = (WorkItemEventImpl) workflowEvent;
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

    private void appendDeadlineInformation(StringBuilder sb, WorkItemEventImpl event) {
        CaseWorkItemType workItem = event.getWorkItem();
        if (!isDone(event) && workItem.getDeadline() != null) {
            appendDeadlineInformation(sb, workItem);
        }
    }

    private void appendDeadlineInformation(StringBuilder sb, AbstractWorkItemType workItem) {
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
        sb.append("Deadline: ").append(valueFormatter.formatDateTime(deadline)).append(beforePhrase).append("\n");
        sb.append("\n");
    }

    private void appendResultAndOriginInformation(StringBuilder sb, WorkItemEventImpl event, OperationResult result) {
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
                sb.append("Carried out by: ").append(valueFormatter.formatUserName(initiatorFull, initiator.getOid())).append("\n");
                atLeastOne = true;
            }
        }
        if (atLeastOne) {
            sb.append("\n");
        }
    }

    private void appendAssigneeInformation(StringBuilder sb, WorkItemEventImpl event, OperationResult result) {
        CaseWorkItemType workItem = event.getWorkItem();
        ObjectReferenceType originalAssignee = workItem.getOriginalAssigneeRef();
        List<ObjectReferenceType> currentAssignees = workItem.getAssigneeRef();
        boolean atLeastOne = false;
        if (currentAssignees.size() != 1 || !java.util.Objects.equals(originalAssignee.getOid(), currentAssignees.get(0).getOid())) {
            UserType originalAssigneeObject = (UserType) functions.getObjectType(originalAssignee, true, result);
            sb.append("Originally allocated to: ").append(
                    valueFormatter.formatUserName(originalAssigneeObject, originalAssignee.getOid())).append("\n");
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
                    .map(ref -> valueFormatter.formatUserName(ref, result))
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
            atLeastOne = true;
        }
        if (atLeastOne) {
            sb.append("\n");
        }
    }

    // a bit of heuristics...
    @SuppressWarnings("SimplifiableIfStatement")
    private boolean isDone(WorkItemEventImpl event) {
        if (event instanceof WorkItemLifecycleEventImpl) {
            return event.isDelete();
        } else if (event instanceof WorkItemAllocationEventImpl) {
            return event.isDelete() &&
                    (event.getOperationKind() == null || event.getOperationKind() == WorkItemOperationKindType.CANCEL
                            || event.getOperationKind() == WorkItemOperationKindType.COMPLETE);
        } else {
            return false;
        }
    }

    private boolean isCancelled(WorkItemEventImpl event) {
        return (event instanceof WorkItemLifecycleEventImpl || event instanceof WorkItemAllocationEventImpl)
                && event.isDelete()
                && (event.getOperationKind() == null || event.getOperationKind() == WorkItemOperationKindType.CANCEL);
    }

    private void appendEscalationInformation(StringBuilder sb, WorkItemEventImpl workItemEvent) {
        String info = ApprovalContextUtil.getEscalationLevelInfo(workItemEvent.getWorkItem());
        if (info != null) {
            sb.append("Escalation level: ").append(info).append("\n");
        }
    }

    private void appendStageInformation(StringBuilder sb, WorkflowEvent workflowEvent) {
        String info = ApprovalContextUtil.getStageInfo(workflowEvent.getCase());
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
            return "cancelled";        // OK?
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
